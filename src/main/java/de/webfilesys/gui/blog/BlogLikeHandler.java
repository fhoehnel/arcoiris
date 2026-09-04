package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.Hashtable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.servlet.VisitorServlet;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogLikeHandler extends XmlRequestHandlerBase {

    public BlogLikeHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        String imgName = getParameter("imgName");
        if (CommonUtils.isEmpty(imgName)) {
            LogManager.getLogger(getClass()).error("missing parameter imgName");
            return;
        }

        File imgFile = new File(currentPath, imgName);

        if ((!imgFile.exists()) || (!imgFile.isFile())) {
            LogManager.getLogger(getClass()).error("img file is not a readable file: " + imgFile.getAbsolutePath());
            return;
        }

        Hashtable<String, Boolean> ratedPictures = (Hashtable<String, Boolean>) session.getAttribute("ratedPictures");

        if (ratedPictures == null) {
            ratedPictures = new Hashtable<String, Boolean>(5);
            session.setAttribute("ratedPictures", ratedPictures);
        }

        if (ratedPictures.get(imgFile.getAbsolutePath()) == null) {
            String visitorId = (String) req.getSession().getAttribute(VisitorServlet.SESSION_ATTRIB_VISITOR_ID);

            if (visitorId != null) {
                BlogMetaInfManager.getInstance().addLiker(imgFile.getAbsolutePath(), visitorId);
            } else {
                // MetaInfManager.getInstance().addVisitorRating(currentPath, imgName, 5);
            }

            ratedPictures.put(imgFile.getAbsolutePath(), new Boolean(true));
        }

        boolean success = true;

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));

        int newVoteCount = BlogMetaInfManager.getInstance().getLikerCount(imgFile.getAbsolutePath());

        XmlUtil.setChildText(resultElement, "newVoteCount", Integer.toString(newVoteCount));

        doc.appendChild(resultElement);

        processResponse();
    }
}
