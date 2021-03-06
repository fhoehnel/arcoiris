package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.Comment;
import de.webfilesys.InvitationManager;
import de.webfilesys.LanguageManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogListCommentsHandler extends XmlRequestHandlerBase {
    public BlogListCommentsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
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

        Element fileCommentsElement = doc.createElement("fileComments");

        doc.appendChild(fileCommentsElement);

        XmlUtil.setChildText(fileCommentsElement, "css", userMgr.getCSS(uid), false);
        XmlUtil.setChildText(fileCommentsElement, "language", language, false);

        XmlUtil.setChildText(fileCommentsElement, "fileName", fileName, false);

        boolean modifyPermission = true;

        if (userMgr.getUserType(uid).equals("virtual")) {
            modifyPermission = InvitationManager.getInstance().commentsAllowed(uid);
        }

        if (modifyPermission) {
            XmlUtil.setChildText(fileCommentsElement, "modifyPermission", "true", false);
        }

        if (userMgr.getUserType(uid).equals("virtual")) {
            XmlUtil.setChildText(fileCommentsElement, "virtualUser", "true", false);
        }

        if (readonly) {
            XmlUtil.setChildText(fileCommentsElement, "readonly", "true", false);
        }

        String mobile = (String) session.getAttribute("mobile");

        if (mobile != null) {
            XmlUtil.setChildText(fileCommentsElement, "mobile", "true", false);
        }

        Element commentListElement = doc.createElement("comments");

        fileCommentsElement.appendChild(commentListElement);

        Vector listOfComments = MetaInfManager.getInstance().getListOfComments(filePath);

        if ((listOfComments != null) && (listOfComments.size() > 0)) {
            SimpleDateFormat dateFormat = LanguageManager.getInstance().getDateFormat(language);

            for (int i = 0; i < listOfComments.size(); i++) {
                Comment comment = (Comment) listOfComments.elementAt(i);

                String login = comment.getUser();

                StringBuffer userString = new StringBuffer();

                if (!userMgr.userExists(login)) {
                    // anonymous guest who entered his name
                    userString.append(login);
                } else if (userMgr.getUserType(login).equals("virtual")) {
                    userString.append(getResource("label.guestuser", "Guest"));
                } else {
                    String firstName = userMgr.getFirstName(login);
                    String lastName = userMgr.getLastName(login);

                    if ((lastName != null) && (lastName.trim().length() > 0)) {
                        if (firstName != null) {
                            userString.append(firstName);
                            userString.append(" ");
                        }

                        userString.append(lastName);
                    } else {
                        userString.append(login);
                    }
                }

                Element commentElement = doc.createElement("comment");

                commentListElement.appendChild(commentElement);

                XmlUtil.setChildText(commentElement, "user", userString.toString(), false);

                XmlUtil.setChildText(commentElement, "date", dateFormat.format(comment.getCreationDate()), false);

                Element msgElem = doc.createElement("msg");
                commentElement.appendChild(msgElem);
                appendCommentFragments(comment.getMessage(), msgElem);
            }
        }

        if (!userMgr.getUserType(uid).equals("virtual")) {
            MetaInfManager.getInstance().setCommentsSeenByOwner(filePath, true);
            MetaInfManager.getInstance().setUnnotifiedComments(normalizedPath, false);
        }

        processResponse();
    }

    private void appendCommentFragments(String description, Element descrElem) {
        StringTokenizer descrParser = new StringTokenizer(description, "{}", true);

        boolean emojiStarted = false;
        String textFragment = null;
        String emojiName = null;

        while (descrParser.hasMoreTokens()) {
            String token = descrParser.nextToken();
            if (token.equals("{")) {
                if (!emojiStarted) {
                    if (textFragment != null) {
                        Element fragmentElem = doc.createElement("fragment");
                        XmlUtil.setElementText(fragmentElem, textFragment);
                        descrElem.appendChild(fragmentElem);
                        textFragment = null;
                    }
                    emojiStarted = true;
                }
            } else if (token.equals("}")) {
                if (emojiStarted) {
                    if (emojiName != null) {
                        Element emojiElem = doc.createElement("emoji");
                        XmlUtil.setElementText(emojiElem, emojiName);
                        descrElem.appendChild(emojiElem);
                        emojiName = null;
                    }
                    emojiStarted = false;
                }
            } else {
                if (emojiStarted) {
                    emojiName = token;
                } else {
                    textFragment = token;
                }
            }
        }
        if (textFragment != null) {
            Element fragmentElem = doc.createElement("fragment");
            XmlUtil.setElementText(fragmentElem, textFragment);
            descrElem.appendChild(fragmentElem);
        }
    }

}