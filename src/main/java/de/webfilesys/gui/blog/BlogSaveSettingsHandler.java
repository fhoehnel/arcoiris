package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.config.BlogConfig;
import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserMgmtException;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

public class BlogSaveSettingsHandler extends XmlRequestHandlerBase {
    public BlogSaveSettingsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        boolean configChanged = false;

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        BlogConfig blogConfig = BlogConfigManager.getInstance().getConfig(currentPath);

        String newBlogTitle = req.getParameter("blogTitle");

        if (!CommonUtils.isEmpty(newBlogTitle)) {
            if (!newBlogTitle.equals(blogConfig.getTitleText())) {
                blogConfig.setTitleText(newBlogTitle);
                configChanged = true;
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

        String stagedPublication = req.getParameter("stagedPublication");

        if (stagedPublication == null) {
            if (blogConfig.isStagedPublication()) {

                File blogDir = new File(currentPath);

                File[] filesInDir = blogDir.listFiles();

                for (int i = 0; i < filesInDir.length; i++) {
                    if (filesInDir[i].isFile() && filesInDir[i].canRead()) {
                        if (BlogMetaInfManager.getInstance().getStatus(filesInDir[i].getAbsolutePath()) == BlogMetaInfManager.STATUS_BLOG_EDIT) {
                            BlogMetaInfManager.getInstance().setStatus(filesInDir[i].getAbsolutePath(), BlogMetaInfManager.STATUS_BLOG_PUBLISHED);
                        }
                    }
                }
                configChanged = true;
            }
            blogConfig.setStagedPublication(false);
        } else {
            if (!blogConfig.isStagedPublication()) {
                blogConfig.setStagedPublication(true);
                configChanged = true;
            }
        }

        boolean notifyOnNewComment = (getParameter("notifyOnNewComment") != null);

        if (blogConfig.isNotifyOnNewComment() != notifyOnNewComment) {
            blogConfig.setNotifyOnNewComment(notifyOnNewComment);
            configChanged = true;
        }

        String sortOrderParam = getParameter("sortOrder");
        if (sortOrderParam != null) {
            try {
                int sortOrder = Integer.parseInt(sortOrderParam);

                if (sortOrder == BlogDateComparator.SORT_ORDER_BLOG) {
                    if (blogConfig.getSortOrder() == BlogConfig.SortOrder.DIARY) {
                        blogConfig.setSortOrder(BlogConfig.SortOrder.BLOG);
                        configChanged = true;
                    }
                } else {
                    if (blogConfig.getSortOrder() == BlogConfig.SortOrder.BLOG) {
                        blogConfig.setSortOrder(BlogConfig.SortOrder.DIARY);
                        configChanged = true;
                    }
                }
            } catch (NumberFormatException ex) {
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

        if (configChanged) {
            BlogConfigManager.getInstance().setConfig(currentPath, blogConfig);
        }

        boolean anythingChanged = configChanged || pageSizeChanged || skinChanged || languageChanged;

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(!passwordMismatch));
        XmlUtil.setChildText(resultElement, "configChanged", Boolean.toString(anythingChanged));

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
