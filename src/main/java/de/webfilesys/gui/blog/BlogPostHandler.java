package de.webfilesys.gui.blog;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.gui.xsl.XslRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

public class BlogPostHandler extends XslRequestHandlerBase {

    public BlogPostHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        Element blogElement = doc.createElement("blog");

        doc.appendChild(blogElement);

        ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"" + req.getContextPath() + "/xsl/blog/blogPost.xsl\"");

        doc.insertBefore(xslRef, blogElement);

        XmlUtil.setChildText(blogElement, "language", language, false);
        XmlUtil.setChildText(blogElement, "skin", userMgr.getCSS(uid), false);

        Element geoTagElement = doc.createElement("geoTag");

        blogElement.appendChild(geoTagElement);

        Element zoomLevelElement = doc.createElement("zoomLevel");

        geoTagElement.appendChild(zoomLevelElement);

        for (int i = 0; i < 16; i++) {
            Element zoomFactorElement = doc.createElement("zoomFactor");

            XmlUtil.setElementText(zoomFactorElement, Integer.toString(i));

            if (i == 10) {
                zoomFactorElement.setAttribute("current", "true");
            }

            zoomLevelElement.appendChild(zoomFactorElement);
        }

        String googleMapsAPIKey = null;
        if (req.getScheme().equalsIgnoreCase("https")) {
            googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTPS();
        } else {
            googleMapsAPIKey = ArcoirisBlog.getInstance().getGoogleMapsAPIKeyHTTP();
        }
        
        if (!CommonUtils.isEmpty(googleMapsAPIKey)) {
            XmlUtil.setChildText(geoTagElement, "googleMapsAPIKey", googleMapsAPIKey, false);
        }
        
        processResponse("blog/blogPost.xsl", req, true);
    }

}
