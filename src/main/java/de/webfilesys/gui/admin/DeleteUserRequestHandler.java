package de.webfilesys.gui.admin;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.webfilesys.util.CommonUtils;

/**
 * @author Frank Hoehnel
 */
public class DeleteUserRequestHandler extends AdminRequestHandler {
    public DeleteUserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String userToBeDeleted = getParameter("userToBeDeleted");

        if (!CommonUtils.isEmpty(userToBeDeleted)) {
            String userHomeDir = userMgr.getDocumentRoot(userToBeDeleted);
            if (!delDirTree(userHomeDir)) {
                LogManager.getLogger(getClass()).error("failed to delete home directory of user " + userToBeDeleted + ": " + userMgr.getDocumentRoot(userToBeDeleted));
            } else {
                String path = userHomeDir.replace('/', File.separatorChar);
                BlogMetaInfManager.getInstance().removeAllMetaInfOfUserFromCache(path);
                AttachmentManager.getInstance().removeAllAttachmentsOfUserFromCache(path);
            }

            userMgr.removeUser(userToBeDeleted);
        }

        (new UserListRequestHandler(req, resp, session, output, uid)).handleRequest();
    }

    protected boolean delDirTree(String path) {
        boolean deleteError = false;

        File dirToBeDeleted = new File(path);

        File[] fileList = dirToBeDeleted.listFiles();

        if (fileList != null) {
            for (File file : fileList) {

                String absolutePath = file.getAbsolutePath();

                if (file.isDirectory()) {
                    if (!delDirTree(absolutePath)) {
                        deleteError = true;
                    }
                } else {

                    if (!file.delete()) {
                        deleteError = true;
                        LogManager.getLogger(getClass()).error("cannot delete " + file.getAbsolutePath());
                    }
                }
            }
        }

        if (!dirToBeDeleted.delete()) {
            deleteError = true;
        } else {
        }

        return !deleteError;
    }

}
