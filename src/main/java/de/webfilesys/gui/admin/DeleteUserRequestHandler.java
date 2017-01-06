package de.webfilesys.gui.admin;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.MetaInfManager;
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
            if (!delDirTree(userMgr.getDocumentRoot(userToBeDeleted))) {
                Logger.getLogger(getClass()).error("failed to delete home directory of user " + userToBeDeleted + ": " + userMgr.getDocumentRoot(userToBeDeleted));
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
                        Logger.getLogger(getClass()).error("cannot delete " + file.getAbsolutePath());
                    } else {
                        MetaInfManager.getInstance().removeMetaInf(absolutePath);
                    }
                }
            }
        }

        if (!dirToBeDeleted.delete()) {
            deleteError = true;
        } else {
            MetaInfManager.getInstance().releaseMetaInf(path);
        }

        return !deleteError;
    }

}
