package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogGetPublicUrlHandler extends XmlRequestHandlerBase {

    private boolean ssl = false;

    private int serverPort = 80;

    public BlogGetPublicUrlHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
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

        String publicAccessCode = null;

        ArrayList<String> publishCodes = InvitationManager.getInstance().getInvitationsByOwner(uid);

        if (publishCodes != null) {
            for (int i = 0; (i < publishCodes.size()) && (publicAccessCode == null); i++) {
                String accessCode = (String) publishCodes.get(i);

                String path = InvitationManager.getInstance().getInvitationPath(accessCode);

                if (path != null) { // not expired
                    if (path.equals(currentPath)) {
                        publicAccessCode = accessCode;
                    }
                }
            }
        }

        StringBuffer publicURL = null;

        if (publicAccessCode != null) {
            publicURL = new StringBuffer();

            String virtualUserId = InvitationManager.getInstance().getVirtualUser(publicAccessCode);
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
            publicURL.append(UTF8URLEncoder.encode(virtualUserId));
            publicURL.append('/');
            publicURL.append(publicAccessCode);
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(publicURL != null));

        if (publicURL != null) {
            XmlUtil.setChildText(resultElement, "publicUrl", publicURL.toString());
        }

        doc.appendChild(resultElement);

        processResponse();
    }
}
