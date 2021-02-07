package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogShareSinglePicHandler extends XmlRequestHandlerBase {

    private boolean ssl = false;

    private int serverPort = 80;

    public BlogShareSinglePicHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
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

        String fileName = getParameter("fileName");
        
        String expiration = getParameter("expirationDays");

        int expDays = InvitationManager.EXPIRATION;

        if ((expiration != null) && expiration.trim().length() > 0) {
            try {
                expDays = Integer.parseInt(expiration.trim());
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).error("invalid parameter value for expirationDays", nfex);
            }
        }

        String accessCode = InvitationManager.getInstance().addSinglePictureInvitation(CommonUtils.joinFilesysPath(currentPath,  fileName), expDays);

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

        publicURL.append(req.getContextPath() + "/sharedFile/");
        publicURL.append(accessCode);

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", "true");

        XmlUtil.setChildText(resultElement, "publicUrl", publicURL.toString());

        doc.appendChild(resultElement);

        processResponse();
    }
}
