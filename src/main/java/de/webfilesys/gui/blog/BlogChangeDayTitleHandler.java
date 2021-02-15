package de.webfilesys.gui.blog;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogChangeDayTitleHandler extends XmlRequestHandlerBase {

    private static final Logger LOG = Logger.getLogger(BlogChangeDayTitleHandler.class);
    
    public BlogChangeDayTitleHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String day = getParameter("day");

        String titleText = getParameter("titleText");
        
        boolean titleChanged = false;
        
        if (!CommonUtils.isEmpty(titleText) && !CommonUtils.isEmpty(day)) {
            titleText = CommonUtils.filterForbiddenChars(titleText);
            MetaInfManager.getInstance().setDayTitle(getCwd(), day, titleText);
            titleChanged = true;
        } else {
            MetaInfManager.getInstance().removeDayTitle(getCwd(), day);
            titleChanged = true;
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(titleChanged));
        XmlUtil.setChildText(resultElement, "day", day);
        XmlUtil.setChildText(resultElement, "titleText", titleText);
        
        doc.appendChild(resultElement);

        processResponse();
    }

}
