package de.webfilesys.gui.blog;

import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.XmlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import java.io.PrintWriter;

/**
 * @author Frank Hoehnel
 */
public class BlogSaveTitlePicPositionHandler extends XmlRequestHandlerBase {

    public BlogSaveTitlePicPositionHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String position = getParameter("position");

        boolean success = false;
        try {
            BlogConfigManager.getInstance().setTitlePicPosition(getCwd(), Integer.parseInt(position));
            success = true;
        } catch (NumberFormatException e) {
            LogManager.getLogger(getClass()).warn("invalid title pic position: " + position);
        }

        Element resultElement = doc.createElement("result");
        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));
        doc.appendChild(resultElement);
        processResponse();
    }

}
