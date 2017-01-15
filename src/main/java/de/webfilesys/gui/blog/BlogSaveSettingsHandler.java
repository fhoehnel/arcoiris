package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserMgmtException;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * Delete a folder tree. Mobile version.
 */
public class BlogSaveSettingsHandler extends XmlRequestHandlerBase {
    public BlogSaveSettingsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        boolean blogTitleChanged = false;

        String newBlogTitle = req.getParameter("blogTitle");

        if (!CommonUtils.isEmpty(newBlogTitle)) {
            String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

            String oldBlogTitle = metaInfMgr.getDescription(currentPath, ".");

            if (!newBlogTitle.equals(oldBlogTitle)) {
                metaInfMgr.setDescription(currentPath, ".", newBlogTitle);
                blogTitleChanged = true;
            }
        }

        TransientUser virtualUser = null;

        boolean pageSizeChanged = false;

        String daysPerPage = req.getParameter("daysPerPage");

        if (!CommonUtils.isEmpty(daysPerPage)) {
            int pageSize = 0;
            try {
                pageSize = Integer.parseInt(daysPerPage);

                if (userMgr.getPageSize(uid) != pageSize) {
                    pageSizeChanged = true;
                    userMgr.setPageSize(uid, pageSize);

                    virtualUser = getVirtualUser();
                    if (virtualUser != null) {
                        virtualUser.setPageSize(pageSize);
                    }
                }

            } catch (NumberFormatException numEx) {
                Logger.getLogger(getClass()).error("invalid blog page size: " + daysPerPage);
            }
        } else {
            Logger.getLogger(getClass()).warn("missing parameter blog page size");
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        boolean stagingChanged = false;

        String stagedPublication = req.getParameter("stagedPublication");

        if (stagedPublication == null) {
            if (metaInfMgr.isStagedPublication(currentPath)) {

                File blogDir = new File(currentPath);

                File[] filesInDir = blogDir.listFiles();

                for (int i = 0; i < filesInDir.length; i++) {
                    if (filesInDir[i].isFile() && filesInDir[i].canRead()) {
                        if (metaInfMgr.getStatus(filesInDir[i].getAbsolutePath()) == MetaInfManager.STATUS_BLOG_EDIT) {
                            metaInfMgr.setStatus(filesInDir[i].getAbsolutePath(), MetaInfManager.STATUS_BLOG_PUBLISHED);
                        }
                    }
                }
                stagingChanged = true;
            }

            metaInfMgr.setStagedPublication(currentPath, false);
        } else {
            if (!metaInfMgr.isStagedPublication(currentPath)) {
                metaInfMgr.setStagedPublication(currentPath, true);
                stagingChanged = true;
            }
        }

        boolean skinChanged = false;

        String skin = getParameter("skin");

        if (!CommonUtils.isEmpty(skin)) {
            if (!skin.equals(userMgr.getCSS(uid))) {
                TransientUser changedUser = userMgr.getUser(uid);
                changedUser.setCss(skin);
                try {
                    userMgr.updateUser(changedUser);
                    skinChanged = true;

                    if (virtualUser == null) {
                        virtualUser = getVirtualUser();
                    }
                    if (virtualUser != null) {
                        virtualUser.setCss(skin);
                    }
                } catch (UserMgmtException ex) {
                    Logger.getLogger(getClass()).error("failed to update skin for user " + uid, ex);
                }
            }
        }

        boolean languageChanged = false;
        
        String newLanguage = getParameter("newLanguage");
        
        if (!CommonUtils.isEmpty(newLanguage)) {
            if (!newLanguage.equals(language)) {
                TransientUser changedUser = userMgr.getUser(uid);
                changedUser.setLanguage(newLanguage);
                try {
                    userMgr.updateUser(changedUser);
                    languageChanged = true;

                    if (virtualUser == null) {
                        virtualUser = getVirtualUser();
                    }
                    if (virtualUser != null) {
                        virtualUser.setLanguage(newLanguage);
                    }
                } catch (UserMgmtException ex) {
                    Logger.getLogger(getClass()).error("failed to update language for user " + uid, ex);
                }
            }
        }
        
        String newPassword = req.getParameter("newPassword");
        String newPasswdConfirm = req.getParameter("newPasswdConfirm");

        boolean passwordMismatch = false;

        if (CommonUtils.isEmpty(newPassword)) {
            if (!CommonUtils.isEmpty(newPasswdConfirm)) {
                passwordMismatch = true;
            }
        } else {
            if (CommonUtils.isEmpty(newPasswdConfirm)) {
                passwordMismatch = true;
            } else {
                if (!newPassword.equals(newPasswdConfirm)) {
                    passwordMismatch = true;
                } else {
                    userMgr.setPassword(uid, newPassword);
                }
            }
        }

        if (virtualUser != null) {
            try {
                userMgr.updateUser(virtualUser);
            } catch (UserMgmtException ex) {
                Logger.getLogger(getClass()).error("failed to update virtual user " + virtualUser.getUserid(), ex);
            }
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(!passwordMismatch));
        XmlUtil.setChildText(resultElement, "pageSizeChanged", Boolean.toString(pageSizeChanged));
        XmlUtil.setChildText(resultElement, "blogTitleChanged", Boolean.toString(blogTitleChanged));
        XmlUtil.setChildText(resultElement, "stagingChanged", Boolean.toString(stagingChanged));
        XmlUtil.setChildText(resultElement, "skinChanged", Boolean.toString(skinChanged));
        XmlUtil.setChildText(resultElement, "languageChanged", Boolean.toString(languageChanged));

        doc.appendChild(resultElement);

        processResponse();
    }

    private TransientUser getVirtualUser() {
        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        ArrayList<String> publishCodes = InvitationManager.getInstance().getInvitationsByOwner(uid);

        if (publishCodes != null) {
            for (int i = 0; i < publishCodes.size(); i++) {
                String accessCode = (String) publishCodes.get(i);

                String path = InvitationManager.getInstance().getInvitationPath(accessCode);

                if (path != null) { // not expired
                    if (path.equals(currentPath)) {
                        String virtualUserId = InvitationManager.getInstance().getVirtualUser(accessCode);

                        if (virtualUserId != null) {
                            TransientUser virtualUser = userMgr.getUser(virtualUserId);
                            return virtualUser;
                        }
                    }
                }
            }
        }

        Logger.getLogger(getClass()).error("virtual user for blog visitors not found for user " + uid);

        return null;
    }
}
