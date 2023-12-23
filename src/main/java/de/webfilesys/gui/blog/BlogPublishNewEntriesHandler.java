package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.MetaInfManager;
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

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        boolean anyNewPublished = false;

        if (metaInfMgr.isStagedPublication(currentPath)) {

            File blogDir = new File(currentPath);

            File[] filesInDir = blogDir.listFiles();

            for (int i = 0; i < filesInDir.length; i++) {
                if (filesInDir[i].isFile() && filesInDir[i].canRead()) {
                    if (metaInfMgr.getStatus(filesInDir[i].getAbsolutePath()) == MetaInfManager.STATUS_BLOG_EDIT) {
                        metaInfMgr.setStatus(filesInDir[i].getAbsolutePath(), MetaInfManager.STATUS_BLOG_PUBLISHED);
                        anyNewPublished = true;
                    }
                }
            }

            if (anyNewPublished) {
                String accessCode = InvitationManager.getInstance().getInvitationCode(uid, currentPath);

                if (accessCode != null) {
                    InvitationManager.getInstance().notifySubscribers(accessCode);
                } else {
                    Logger.getLogger(getClass()).warn("could not determine invitation code for subscription notification, uid=" + uid + " docRoot=" + currentPath);
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
