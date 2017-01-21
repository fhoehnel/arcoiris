package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.Comment;
import de.webfilesys.InvitationManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogAddCommentHandler extends XmlRequestHandlerBase {

    public BlogAddCommentHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String fileName = getParameter("fileName");

        String normalizedPath = getCwd().replace('/', File.separatorChar);

        String filePath;
        
        if (normalizedPath.endsWith(File.separator)) {
            filePath = normalizedPath + fileName;
        } else {
            filePath = normalizedPath + File.separator + fileName;
        }

        String commentAuthor = uid;

        boolean modifyPermission = true;

        if (userMgr.getUserType(uid).equals("virtual")) {
            modifyPermission = InvitationManager.getInstance().commentsAllowed(uid);

            String author = req.getParameter("author");

            if ((author != null) && (author.trim().length() > 0)) {
                commentAuthor = author;
            }
        }

        if (!modifyPermission) {
            Logger.getLogger(getClass()).warn("attempt to add comments for blog entry " + filePath + " from virtual user " + uid + " without permission");
            return;
        }

        String newComment = getParameter("newComment");

        boolean commentCreated = false;

        int newCommentCount = 0;

        if (!CommonUtils.isEmpty(newComment)) {

            newComment = CommonUtils.filterForbiddenChars(newComment);

            MetaInfManager.getInstance().addComment(filePath, new Comment(commentAuthor, new Date(), newComment));

            newCommentCount = MetaInfManager.getInstance().countComments(filePath);

            if (readonly) {
                MetaInfManager.getInstance().setCommentsSeenByOwner(filePath, false);
                MetaInfManager.getInstance().setUnnotifiedComments(normalizedPath, true);
            } else {
                MetaInfManager.getInstance().setCommentsSeenByOwner(filePath, true);
            }

            commentCreated = true;
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(commentCreated));

        XmlUtil.setChildText(resultElement, "newCommentCount", Integer.toString(newCommentCount));

        doc.appendChild(resultElement);

        processResponse();
    }

}
