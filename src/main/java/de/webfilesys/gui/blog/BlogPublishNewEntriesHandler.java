package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogPublishNewEntriesHandler extends XmlRequestHandlerBase {

    public BlogPublishNewEntriesHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        String currentPath = getCwd();

        if ((currentPath == null) || (currentPath.trim().length() == 0)) {
            currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);
        }

        boolean anyNewPublished = false;

        if (BlogConfigManager.getInstance().isStagedPublication(currentPath)) {

            File blogDir = new File(currentPath);

            File[] filesInDir = blogDir.listFiles();

            for (int i = 0; i < filesInDir.length; i++) {
                if (filesInDir[i].isFile() && filesInDir[i].canRead()) {
                    if (BlogMetaInfManager.getInstance().getStatus(filesInDir[i].getAbsolutePath()) == BlogMetaInfManager.STATUS_BLOG_EDIT) {
                        BlogMetaInfManager.getInstance().setStatus(filesInDir[i].getAbsolutePath(), BlogMetaInfManager.STATUS_BLOG_PUBLISHED);
                        anyNewPublished = true;
                    }
                }
            }

            if (anyNewPublished) {
                String accessCode = InvitationManager.getInstance().getInvitationCode(uid, currentPath);

                if (accessCode != null) {
                    InvitationManager.getInstance().notifySubscribers(accessCode);
                } else {
                    LogManager.getLogger(getClass()).warn("could not determine invitation code for subscription notification, uid=" + uid + " docRoot=" + currentPath);
                }
            }
        }

        Element resultElement = doc.createElement("result");
        if (anyNewPublished) {
            XmlUtil.setChildText(resultElement, "success", "published");
        }
        doc.appendChild(resultElement);

        processResponse();
    }

}
