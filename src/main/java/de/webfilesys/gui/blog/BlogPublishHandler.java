package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogPublishHandler extends XmlRequestHandlerBase {

    private boolean ssl = false;

    private int serverPort = 80;

    public BlogPublishHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);

        String protocol = req.getScheme();

        if (protocol.toLowerCase().startsWith("https")) {
            ssl = true;
        }

        serverPort = req.getServerPort();
    }

    protected void process() {

        if (!checkWriteAccess()) {
            return;
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        String expiration = getParameter("expirationDays");

        int expDays = InvitationManager.DEFAULT_EXPIRATION_DAYS;

        if (!expiration.trim().isEmpty()) {
            try {
                expDays = Integer.parseInt(expiration);
            } catch (NumberFormatException nfex) {
                LogManager.getLogger(getClass()).error("invalid parameter value for expirationDays", nfex);
            }
        }

        int pageSize = userMgr.getPageSize(uid);
        String pageSizeParm = getParameter("daysPerPage");
        if (!CommonUtils.isEmpty(pageSizeParm)) {
            try {
                pageSize = Integer.parseInt(pageSizeParm);
            } catch (NumberFormatException nfex) {
                LogManager.getLogger(getClass()).error("invalid parameter value for daysPerPage", nfex);
            }
        }

        boolean allowComments = (getParameter("allowComments") != null);

        String virtualUser = null;

        String invitationType = "blog";

        virtualUser = userMgr.createVirtualUser(uid, currentPath, "blog", expDays, getParameter("language"));

        userMgr.setPageSize(virtualUser, pageSize);

        String accessCode = InvitationManager.getInstance().addInvitation(uid, currentPath, expDays, invitationType, allowComments, virtualUser);

        userMgr.setPassword(virtualUser, accessCode);

        StringBuffer publicURL = new StringBuffer();

        if (ssl) {
            publicURL.append("https://");
        } else {
            publicURL.append("http://");
        }

        if (ArcoirisBlog.getInstance().getServerDNS() != null) {
            publicURL.append(ArcoirisBlog.getInstance().getServerDNS());
        } else {
            publicURL.append(ArcoirisBlog.getInstance().getLocalIPAddress());
        }

        publicURL.append(":");

        publicURL.append(serverPort);

        publicURL.append(req.getContextPath() + "/visitor/");
        publicURL.append(UTF8URLEncoder.encode(virtualUser));
        publicURL.append('/');
        publicURL.append(accessCode);

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", "true");

        XmlUtil.setChildText(resultElement, "publicUrl", publicURL.toString());

        doc.appendChild(resultElement);

        processResponse();
    }
}
