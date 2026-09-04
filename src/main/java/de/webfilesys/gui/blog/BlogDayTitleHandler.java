package de.webfilesys.gui.blog;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.daytitle.DayTitleManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogDayTitleHandler extends XmlRequestHandlerBase {
    public BlogDayTitleHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String day = getParameter("day");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date dayDate = null;
        try {
            dayDate = dateFormat.parse(day);
        } catch (Exception ex) {
            LogManager.getLogger(BlogDayTitleHandler.class).error("invalid date: " + day, ex);
            return;
        }
        
        Element dayTitleElement = doc.createElement("dayTitle");

        doc.appendChild(dayTitleElement);

        XmlUtil.setChildText(dayTitleElement, "css", userMgr.getCSS(uid), false);
        XmlUtil.setChildText(dayTitleElement, "language", language, false);

        XmlUtil.setChildText(dayTitleElement, "day", day, false);

        XmlUtil.setChildText(dayTitleElement, "displayDate", formatBlogDate(dayDate), false);

        String dayTitle = DayTitleManager.getInstance().getDayTitle(getCwd(), day);
        if (dayTitle == null) {
            dayTitle = "";
        }
        XmlUtil.setChildText(dayTitleElement, "titleText", dayTitle);
        
        processResponse();
    }

}