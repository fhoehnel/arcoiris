package de.webfilesys.gui.xsl;

import de.webfilesys.util.XmlUtil;
import org.w3c.dom.Element;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;

/**
 * @author Frank Hoehnel
 */
public class OSMMultiLocationMapHandler extends XslRequestHandlerBase {

    public OSMMultiLocationMapHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        Element geoDataElement = doc.createElement("geoData");
        doc.appendChild(geoDataElement);
        XmlUtil.setChildText(geoDataElement, "language", language, false);
        XmlUtil.setChildText(geoDataElement, "skin", userMgr.getCSS(uid), false);

        processResponse("osmMapMulti.xsl", req);
    }


}