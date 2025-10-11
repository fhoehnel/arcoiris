package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.log4j.Logger;

import de.webfilesys.GeoTag;
import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.gui.user.UserRequestHandler;
import de.webfilesys.util.CommonUtils;

/**
 * @author Frank Hoehnel
 */
public class BlogChangeEntryHandler extends UserRequestHandler {
    protected HttpServletRequest req = null;

    protected HttpServletResponse resp = null;

    public BlogChangeEntryHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
        this.req = req;
        this.resp = resp;
    }

    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        String currentPath = getCwd();

        if ((currentPath == null) || (currentPath.trim().length() == 0)) {
            currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);
        }

        String fileName = req.getParameter("fileName");

        if (CommonUtils.isEmpty(fileName)) {
            Logger.getLogger(getClass()).error("missing parameter fileName");
            return;
        }

        File oldFile = new File(currentPath, fileName);
        if ((!oldFile.exists()) || (!oldFile.isFile()) || (!oldFile.canWrite())) {
            Logger.getLogger(getClass()).error("blog entry file not found: " + fileName);
            return;
        }

        String oldFilePath = oldFile.getAbsolutePath();

        String newFileName = fileName;

        String fileNamePrefixFromDate = getFileNamePrefixFromDate();

        if (!fileNamePrefixFromDate.equals(fileName.substring(0, 10))) {
            Logger.getLogger(getClass()).debug("date has changed");

            BlogMetaInfManager blogMetaInfMgr = BlogMetaInfManager.getInstance();

            // newFileName = fileNamePrefixFromDate + fileName.substring(10);
            newFileName = fileNamePrefixFromDate + "-" + System.currentTimeMillis() + CommonUtils.getFileExtension(fileName);

            File newFile = new File(currentPath, newFileName);

            if (!oldFile.renameTo(newFile)) {
                Logger.getLogger(getClass()).error("failed to rename blog file " + fileName + " to " + newFile.getName());
                return;
            } else {
                BlogThumbnailHandler.getInstance().renameThumbnail(oldFilePath, newFileName);
                blogMetaInfMgr.moveMetaInf(currentPath, fileName, newFileName);
                AttachmentManager.getInstance().moveAttachments(currentPath, fileName, newFileName);

                String titlePic = BlogConfigManager.getInstance().getTitlePic(currentPath);
                if ((titlePic != null) && titlePic.equals(fileName)) {
                    BlogConfigManager.getInstance().setTitlePic(currentPath, newFileName);
                }
            }
        }

        String blogText = req.getParameter("blogText");
        if (!CommonUtils.isEmpty(blogText)) {
            blogText = CommonUtils.filterForbiddenChars(blogText);
            BlogMetaInfManager.getInstance().setDescription(currentPath, newFileName, blogText);
        } else {
            BlogMetaInfManager.getInstance().setDescription(currentPath, newFileName, "");
        }

        setParameter("positionToFile", newFileName);
        
        String geoDataSwitcher = req.getParameter("geoDataSwitcher");

        if (geoDataSwitcher != null) {
            boolean geoDataExist = false;

            float latitude = 0f;

            String latitudeParm = req.getParameter("latitude");

            if ((latitudeParm != null) && (latitudeParm.trim().length() > 0)) {
                try {
                    latitude = Float.parseFloat(latitudeParm);

                    if ((latitude >= -90.0f) && (latitude <= 90.0f)) {
                        geoDataExist = true;
                    }
                } catch (NumberFormatException nfex) {
                }
            }

            float longitude = 0f;

            String longitudeParm = req.getParameter("longitude");

            if ((longitudeParm != null) && (longitudeParm.trim().length() > 0)) {
                try {
                    longitude = Float.parseFloat(longitudeParm);

                    if ((longitude >= -180.0f) && (longitude <= 180.0f)) {
                        geoDataExist = true;
                    }
                } catch (NumberFormatException nfex) {
                }
            }

            int zoomFactor = 10;

            String zoomFactorParm = req.getParameter("zoomFactor");

            if ((zoomFactorParm != null) && (zoomFactorParm.trim().length() > 0)) {
                try {
                    zoomFactor = Integer.parseInt(zoomFactorParm);
                } catch (NumberFormatException nfex) {
                }
            }

            if (geoDataExist) {
                GeoTag geoTag = new GeoTag(latitude, longitude, zoomFactor);

                String infoText = req.getParameter("infoText");

                if (infoText != null) {
                    infoText = CommonUtils.filterForbiddenChars(infoText);
                    geoTag.setInfotext(infoText);
                }
                BlogMetaInfManager.getInstance().setGeoTag(currentPath, newFileName, geoTag);
            }
        } else {
            if (newFileName.equals(fileName)) {
                if (BlogMetaInfManager.getInstance().getGeoTag(currentPath, newFileName) != null) {
                    BlogMetaInfManager.getInstance().removeGeoTag(currentPath, newFileName);
                }
            }
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date beforeDate = dateFormat.parse(fileNamePrefixFromDate);
            beforeDate.setTime(beforeDate.getTime() + (25l * 60l * 60l * 1000l));   // 25 hours because of change summer to winter time
            setParameter("beforeDay", dateFormat.format(beforeDate));
        } catch (Exception ex) {
            Logger.getLogger(getClass()).warn("invalid date format", ex);
        }
        
        (new BlogListHandler(req, resp, session, output, uid)).handleRequest();
    }

    private String getFileNamePrefixFromDate() {
        String dateYear = req.getParameter("dateYear");
        String dateMonth = req.getParameter("dateMonth");
        String dateDay = req.getParameter("dateDay");

        return dateYear + "-" + dateMonth + "-" + dateDay;
    }

}
