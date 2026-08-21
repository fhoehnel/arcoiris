package de.webfilesys.gui.blog;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.gui.user.UserRequestHandler;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.util.CommonUtils;

public class BlogDeleteEntryHandler extends UserRequestHandler {
    public BlogDeleteEntryHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        String fileName = getParameter("fileName");

        if (CommonUtils.isEmpty(fileName)) {
            LogManager.getLogger(getClass()).error("missing parameter fileName");
            return;
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        boolean success = false;

        File fileToBeDeleted = new File(currentPath, fileName);

        String deletedFilePath = fileToBeDeleted.getAbsolutePath();

        if ((!fileToBeDeleted.exists()) || (!fileToBeDeleted.isFile()) || (!fileToBeDeleted.canWrite())) {
            LogManager.getLogger(getClass()).error("blog entry file to be deleted is not a writable file: " + fileToBeDeleted.getAbsolutePath());
        } else {
            if (fileToBeDeleted.delete()) {
                BlogMetaInfManager.getInstance().removeMetaInf(currentPath, fileName);
                AttachmentManager.getInstance().removeAttachments(currentPath, fileName);
                BlogThumbnailHandler.getInstance().deleteThumbnail(deletedFilePath);
                String titlePic = BlogConfigManager.getInstance().getTitlePic(currentPath);
                if (titlePic != null && titlePic.equals(fileName)) {
                    BlogConfigManager.getInstance().unsetTitlePic(currentPath);
                }
                success = true;
            } else {
                LogManager.getLogger(getClass()).error("failed to delete blog entry file " + fileToBeDeleted.getAbsolutePath());
            }
        }
        if (!success) {
            try {
                resp.sendError(HttpServletResponse.SC_CONFLICT);
            } catch (IOException ex) {
            }
        }
    }

}
