package de.webfilesys.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.webfilesys.GeoTag;
import de.webfilesys.InvitationManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.graphics.ImageTransformUtil;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLDecoder;

/**
 * Handles requests from the Android blog app.
 */
public class BlogAppServlet extends BlogWebServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(BlogAppServlet.class);

    private static final String BASIC_HTTP_AUTH_HEADER = "Authorization";

    private static final String BASIC_HTTP_AUTH_PROMPT = "WWW-Authenticate";
    
    // private static int requestCounter = 0;

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("GET request");
        }

        resp.setDateHeader("expires", 0l);
        resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");

        String userid = authenticateUser(req, resp);

        if (userid == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader(BASIC_HTTP_AUTH_PROMPT, "Basic realm=\"webfilesys\"");
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("successful authentication by blog client");
            }
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        // prevent caching
        resp.setDateHeader("expires", 0l);
        resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");

        String userid = authenticateUser(req, resp);

        if (userid == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader(BASIC_HTTP_AUTH_PROMPT, "Basic realm=\"webfilesys\"");
            return;
        }

        String requestPath = req.getRequestURI();

        int lastPathDelimiterIdx = requestPath.lastIndexOf('/');

        int delIdx = requestPath.substring(0, lastPathDelimiterIdx).lastIndexOf('/');

        if (delIdx < 0) {
            LOG.error("invalid parameters for BlogPostServlet: " + requestPath);
            throw new ServletException("invalid parameters for BlogPostServlet");
        }

        String command = requestPath.substring(delIdx + 1, lastPathDelimiterIdx);

        LOG.debug("command posted by Android app: " + command);
        
        /*
         * if (command.equals("authenticate")) { return; }
         */

        String currentPath = ArcoirisBlog.getInstance().getUserMgr().getDocumentRoot(userid);

        if (currentPath == null) {
            LOG.error("current working directory unknown");
            throw new ServletException("current working directory unknown");
        }

        String fileName = UTF8URLDecoder.decode(requestPath.substring(lastPathDelimiterIdx + 1));

        fileName = replaceIllegalChars(fileName);

        if (command.equals("picture")) {
            try {
                reveicePicture(req, resp, userid, currentPath, fileName);
            } catch (Exception ex) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else if (command.equals("description")) {
            
            // testing code for network delay and network error             
            /*
            if (++requestCounter % 2 == 0) {
                if (System.currentTimeMillis() > 1l) {
                    throw new ServletException("test exception");
                }
                
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException iex) {
                }
            }
            */
            
            receiveDescription(req, resp, currentPath, fileName);

            String path = currentPath.replace('/', File.separatorChar);

            if (!MetaInfManager.getInstance().isStagedPublication(path)) {

                String accessCode = InvitationManager.getInstance().getInvitationCode(userid, path);
                if (accessCode != null) {
                    InvitationManager.getInstance().notifySubscribers(accessCode);
                } else {
                    Logger.getLogger(getClass()).warn("could not determine invitation code for subscription notification, uid=" + userid + " docRoot=" + path);
                }
            }
        } else if (command.equals("publish")) {
            receiveAndIgnoreParams(req, resp);
            String path = currentPath.replace('/', File.separatorChar);
            if (MetaInfManager.getInstance().isStagedPublication(path)) {
                MetaInfManager.getInstance().setStatus(path, fileName, MetaInfManager.STATUS_BLOG_PUBLISHED);

                String accessCode = InvitationManager.getInstance().getInvitationCode(userid, path);
                if (accessCode != null) {
                    InvitationManager.getInstance().notifySubscribers(accessCode);
                } else {
                    Logger.getLogger(getClass()).warn("could not determine invitation code for subscription notification, uid=" + userid + " docRoot=" + path);
                }
            }
        } else if (command.equals("cancel")) {
            receiveAndIgnoreParams(req, resp);
            
            String osDepPath = currentPath.replace('/', File.separatorChar);
            
            File fileToCancel = new File(osDepPath, fileName);
            
            if (fileToCancel.exists()) {
                MetaInfManager.getInstance().removeDescription(fileToCancel.getAbsolutePath());

                if (fileToCancel.delete()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("entry canceled by app: " + fileName);
                    }
                    if (!BlogThumbnailHandler.getInstance().deleteThumbnail(fileToCancel.getAbsolutePath())) {
                        LOG.warn("failed to delete thumbnail for canceled entry " + fileName);
                    }
                } else {
                    LOG.error("failed to delete picture file for entry canceled by app: " + fileName);
                }
            }
        } else {
            LOG.warn("invalid command in call to BlogPostServlet: " + command + ", request: " + requestPath);
        }
    }

    private void receiveDescription(HttpServletRequest req, HttpServletResponse resp, String currentPath, String fileName) {
        File picFile = new File(currentPath, fileName);

        if ((!picFile.exists()) || (!picFile.isFile()) || (!picFile.canRead())) {
            LOG.error("picture file for posted description does not exists: " + currentPath + " - " + fileName);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("receiving description for blog file: " + fileName);
        }

        InputStreamReader isr = null;
        BufferedReader bufferedIn = null;

        String latitudeParam = null;
        String longitudeParam = null;

        try {
            isr = new InputStreamReader(req.getInputStream(), "UTF-8");

            bufferedIn = new BufferedReader(isr);

            String description = bufferedIn.readLine();
            if (description == null) {
                description = "";
            } else {
                latitudeParam = bufferedIn.readLine();
                if (latitudeParam != null) {
                    longitudeParam = bufferedIn.readLine();
                }
            }

            String osDepPath = currentPath.replace('/', File.separatorChar);

            if (LOG.isDebugEnabled()) {
                LOG.debug("setting description for posted blog file: osDepPath=" + osDepPath + " fileName=" + fileName + " latitude: " + latitudeParam + " longitude: "
                                + longitudeParam + " desc=" + description);
            }

            MetaInfManager.getInstance().setDescription(osDepPath, fileName, description.toString());

            if ((latitudeParam != null) && (longitudeParam != null)) {
                latitudeParam = latitudeParam.replace(',', '.');
                longitudeParam = longitudeParam.replace(',', '.');
                try {
                    float latitude = Float.parseFloat(latitudeParam);
                    float longitude = Float.parseFloat(longitudeParam);

                    GeoTag geoTag = new GeoTag(latitude, longitude, 10);

                    MetaInfManager.getInstance().setGeoTag(osDepPath, fileName, geoTag);

                } catch (Exception ex) {
                    LOG.warn("invalid geo coordinate values: " + latitudeParam + " - " + longitudeParam);
                }
            }
        } catch (IOException ex) {
            LOG.error("failed to read description", ex);
        } finally {
            if (bufferedIn != null) {
                try {
                    bufferedIn.close();
                } catch (Exception closeEx) {
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception closeEx) {
                }
            }
        }
    }

    private void reveicePicture(HttpServletRequest req, HttpServletResponse resp, String userid, String currentPath, String fileName) throws IOException {
        long uploadLimit = ArcoirisBlog.getInstance().getUploadLimit();

        File outFile = new File(currentPath, fileName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("binary file upload to: " + outFile.getAbsolutePath());
        }

        long uploadSize = 0l;

        byte[] buff = new byte[4096];

        FileOutputStream uploadOut = null;

        boolean error = false;
        
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
                    resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                }

                uploadOut.write(buff, 0, bytesRead);
            }

            uploadOut.flush();

        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("error in ajax binary upload", ex);
            error = true;
        } finally {
            if (uploadOut != null) {
                try {
                    uploadOut.close();
                } catch (Exception closeEx) {
                }
            }
        }

        if (error) {
            if (outFile.exists()) {
                if (outFile.delete()) {
                    Logger.getLogger(getClass()).debug("deleted incompletely uploaded picture file " + fileName);
                } else {
                    Logger.getLogger(getClass()).warn("failed to delete incompletely uploaded picture file " + fileName);
                }
            }
            throw new IOException("Failed to receive blog picture " + fileName);
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

        String path = currentPath.replace('/', File.separatorChar);

        if (MetaInfManager.getInstance().isStagedPublication(path)) {
            MetaInfManager.getInstance().setStatus(origImgPath, MetaInfManager.STATUS_BLOG_EDIT);
        }

        BlogThumbnailHandler.getInstance().createBlogThumbnail(origImgPath);
    }

    private void receiveAndIgnoreParams(HttpServletRequest req, HttpServletResponse resp) {
        InputStreamReader isr = null;
        BufferedReader bufferedIn = null;

        try {
            isr = new InputStreamReader(req.getInputStream(), "UTF-8");
            bufferedIn = new BufferedReader(isr);

            // currently there are no parameters processed
            bufferedIn.readLine();
        } catch (IOException ex) {
            LOG.error("failed to read params", ex);
        } finally {
            if (bufferedIn != null) {
                try {
                    bufferedIn.close();
                } catch (Exception closeEx) {
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception closeEx) {
                }
            }
        }
    }

    private String authenticateUser(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        sun.misc.BASE64Decoder base64Decoder = new sun.misc.BASE64Decoder();

        String userid = null;

        String password = null;

        String basicHttpAuthParm = req.getHeader(BASIC_HTTP_AUTH_HEADER);

        if (!CommonUtils.isEmpty(basicHttpAuthParm)) {
            String paramParts[] = basicHttpAuthParm.split(" ");
            if (paramParts.length > 1) {
                String encodedAuthToken = paramParts[1];
                String decodedAuthToken = new String(base64Decoder.decodeBuffer(encodedAuthToken));
                String useridPassword[] = decodedAuthToken.split(":");
                if (useridPassword.length == 2) {
                    userid = useridPassword[0];
                    password = useridPassword[1];
                }
            }
        }

        if ((userid == null) || (password == null)) {
            LOG.warn("missing auth header data");
            return null;
        }

        if (!ArcoirisBlog.getInstance().getUserMgr().checkPassword(userid, password)) {
            LOG.warn("invalid credentials in BlogPostServlet");
            return null;
        }

        if (ArcoirisBlog.getInstance().getUserMgr().isReadonly(userid)) {
            LOG.warn("invalid credentials (readonly)");
            return null;
        }

        return userid;
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

    private boolean isPictureFile(String fileExt) {
        if (CommonUtils.isEmpty(fileExt)) {
            return false;
        }
        return (fileExt.equalsIgnoreCase(".jpg") || fileExt.equalsIgnoreCase(".gif") || fileExt.equalsIgnoreCase(".png") || fileExt.equalsIgnoreCase(".bmp") || fileExt
                        .equalsIgnoreCase(".jpeg"));
    }

}
