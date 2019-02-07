package de.webfilesys.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.GeoTag;
import de.webfilesys.MetaInfManager;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.graphics.ImageTransformUtil;
import de.webfilesys.gui.xsl.XslLogonHandler;
import de.webfilesys.util.UTF8URLDecoder;

public class UploadServlet extends BlogWebServlet {
    private static final long serialVersionUID = 1L;
    
    public static final String SUBDIR_ATTACHMENT = "attachments";

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        // prevent caching
        resp.setDateHeader("expires", 0l);
        resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");

        String userid = null;

        HttpSession session = req.getSession(false);

        if (session != null) {
            userid = (String) session.getAttribute("userid");

            if (userid == null) {
                (new XslLogonHandler(req, resp, session, resp.getWriter(), false)).handleRequest();

                return;
            }
        } else {
            session = req.getSession(true);

            (new XslLogonHandler(req, resp, session, resp.getWriter(), false)).handleRequest();

            return;
        }

        if (!checkWriteAccess(userid, session)) {
            throw new ServletException("write access forbidden");
        }

        String requestPath = req.getRequestURI();

        if (requestPath.indexOf("/attachment/") >= 0) {
            handleAttachmentUpload(req, resp);
            return;
        }
        
        handleSingleBinaryUpload(req, resp);
    }

    public void handleSingleBinaryUpload(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        HttpSession session = req.getSession(true);

        String currentPath = (String) session.getAttribute("cwd");

        if (currentPath == null) {
            Logger.getLogger(getClass()).error("current working directory unknown");
            return;
        }

        long uploadLimit = ArcoirisBlog.getInstance().getUploadLimit();

        String requestPath = req.getRequestURI();

        int lastPathDelimiterIdx = requestPath.lastIndexOf('/');

        String fileName = UTF8URLDecoder.decode(requestPath.substring(lastPathDelimiterIdx + 1));

        fileName = replaceIllegalChars(fileName);

        File outFile = new File(currentPath, fileName);

        if (Logger.getLogger(getClass()).isDebugEnabled()) {
            Logger.getLogger(getClass()).debug("ajax binary file upload: " + outFile.getAbsolutePath());
        }

        long uploadSize = 0l;

        byte[] buff = new byte[4096];

        FileOutputStream uploadOut = null;

        try {
            uploadOut = new FileOutputStream(outFile);

            InputStream input = req.getInputStream();

            int bytesRead;

            while ((bytesRead = input.read(buff)) > 0) {
                uploadSize += bytesRead;
                if (uploadSize > uploadLimit) {
                    Logger.getLogger(getClass()).warn("upload limit of " + uploadLimit + " bytes exceeded for file " + outFile.getAbsolutePath());
                    uploadOut.flush();
                    uploadOut.close();
                    outFile.delete();
                    throw new ServletException("upload limit of " + uploadLimit + " bytes exceeded");
                }

                uploadOut.write(buff, 0, bytesRead);
            }

            uploadOut.flush();

        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("error in ajax binary upload", ex);
            throw ex;
        } finally {
            if (uploadOut != null) {
                try {
                    uploadOut.close();
                } catch (Exception closeEx) {
                }
            }
        }

        String origImgPath = outFile.getAbsolutePath();

        GeoTag geoTag = null;

        CameraExifData exifData = new CameraExifData(origImgPath);

        if (exifData.hasExifData()) {
            float gpsLatitude = exifData.getGpsLatitude();
            float gpsLongitude = exifData.getGpsLongitude();

            if ((gpsLatitude >= 0.0f) && (gpsLongitude >= 0.0f)) {

                String latitudeRef = exifData.getGpsLatitudeRef();

                if ((latitudeRef != null) && latitudeRef.equalsIgnoreCase("S")) {
                    gpsLatitude = (-gpsLatitude);
                }

                String longitudeRef = exifData.getGpsLongitudeRef();

                if ((longitudeRef != null) && longitudeRef.equalsIgnoreCase("W")) {
                    gpsLongitude = (-gpsLongitude);
                }

                geoTag = new GeoTag();
                geoTag.setLatitude(gpsLatitude);
                geoTag.setLongitude(gpsLongitude);
            }
        }

        BlogThumbnailHandler.getInstance().createBlogThumbnail(origImgPath);

        int lastSepIdx = origImgPath.lastIndexOf(File.separatorChar);

        String scaledImgPath = origImgPath.substring(0, lastSepIdx + 1) + "scaled-" + origImgPath.substring(lastSepIdx + 1);

        // TODO: image size from blog settings
        if (ImageTransformUtil.createScaledImage(origImgPath, scaledImgPath, 1280, 1280)) {

            File origImgFile = new File(origImgPath);
            if (!origImgFile.delete()) {
                Logger.getLogger(getClass()).error("failed to delete original image after scaling: " + origImgPath);
            } else {
                File scaledImgFile = new File(scaledImgPath);
                if (!scaledImgFile.renameTo(origImgFile)) {
                    Logger.getLogger(getClass()).error("failed to rename scaled image file " + scaledImgPath + " to " + origImgPath);
                }
            }
        }

        if (geoTag != null) {
            MetaInfManager.getInstance().setGeoTag(origImgPath, geoTag);
        }

        if (MetaInfManager.getInstance().isStagedPublication(currentPath)) {
            MetaInfManager.getInstance().setStatus(origImgPath, MetaInfManager.STATUS_BLOG_EDIT);
        }
    }

    public void handleAttachmentUpload(HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, java.io.IOException {
        HttpSession session = req.getSession(true);

        String currentPath = (String) session.getAttribute("cwd");

        if (currentPath == null) {
            Logger.getLogger(getClass()).error("current working directory unknown");
            return;
        }

        long uploadLimit = ArcoirisBlog.getInstance().getUploadLimit();

        String requestPath = req.getRequestURI();

        String[] partsOfUri = requestPath.split("/");

        String blogFileName = UTF8URLDecoder.decode(partsOfUri[partsOfUri.length - 1]);
        
        String attachmentFileName = UTF8URLDecoder.decode(partsOfUri[partsOfUri.length - 2]);

        attachmentFileName = replaceIllegalChars(attachmentFileName);

        File attachmentDir = new File(currentPath, SUBDIR_ATTACHMENT);
        
        if (!attachmentDir.exists()) {
            if (!attachmentDir.mkdir()) {
                Logger.getLogger(getClass()).error("failed to create attachment dir");
                throw new ServletException("failed to create attachment dir");
            }
        }
        
        File outFile = new File(attachmentDir.getAbsolutePath(), attachmentFileName);

        if (Logger.getLogger(getClass()).isDebugEnabled()) {
            Logger.getLogger(getClass()).debug("attachment file upload: " + outFile.getAbsolutePath());
        }

        long uploadSize = 0l;

        byte[] buff = new byte[4096];

        FileOutputStream uploadOut = null;

        try {
            uploadOut = new FileOutputStream(outFile);

            InputStream input = req.getInputStream();

            int bytesRead;

            while ((bytesRead = input.read(buff)) > 0) {
                uploadSize += bytesRead;
                if (uploadSize > uploadLimit) {
                    Logger.getLogger(getClass()).warn("upload limit of " + uploadLimit + " bytes exceeded for file " + outFile.getAbsolutePath());
                    uploadOut.flush();
                    uploadOut.close();
                    outFile.delete();
                    throw new ServletException("upload limit of " + uploadLimit + " bytes exceeded");
                }

                uploadOut.write(buff, 0, bytesRead);
            }

            uploadOut.flush();

        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("error in attachment upload", ex);
            throw ex;
        } finally {
            if (uploadOut != null) {
                try {
                    uploadOut.close();
                } catch (Exception closeEx) {
                }
            }
        }
        
        MetaInfManager.getInstance().addAttachment(currentPath, blogFileName, attachmentFileName);
    }
    
    private String replaceIllegalChars(String fileName) {
        StringBuffer buff = new StringBuffer(fileName.length());

        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);

            if ((c == '\'') || (c == '#') || (c == '`') || (c == '%') || (c == '!') || (c == '§') || (c == '&') || (c == '[') || (c == ']') || (c == '\"')) {
                c = '_';
            }

            buff.append(c);
        }

        return (buff.toString());
    }

    public boolean checkWriteAccess(String userid, HttpSession session) {
        boolean sessionReadonly = false;

        Boolean sessRO = (Boolean) session.getAttribute("readonly");

        if (sessRO != null) {
            sessionReadonly = sessRO.booleanValue();
        }

        boolean readonly = sessionReadonly || ArcoirisBlog.getInstance().getUserMgr().isReadonly(userid);

        if (!readonly) {
            return (true);
        }

        Logger.getLogger(getClass()).warn("read-only user " + userid + " tried write access");

        return (false);
    }

}
