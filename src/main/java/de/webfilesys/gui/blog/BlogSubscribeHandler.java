package de.webfilesys.gui.blog;

import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * Set the last modified time of the file to the current time (touch command in
 * UNIX).
 * 
 * @author Frank Hoehnel
 */
public class BlogSubscribeHandler extends XmlRequestHandlerBase {
    public BlogSubscribeHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String subscriberEmail = getParameter("subscriberEmail");

        if (CommonUtils.isEmpty(subscriberEmail)) {
            LogManager.getLogger(getClass()).error("missing parameter subscriberEmail");
            return;
        }

        boolean success = InvitationManager.getInstance().addSubscriber(uid, subscriberEmail);

        if (success) {
            LogManager.getLogger(getClass()).info("blog subscriber added, virtualUser=" + uid + " email=" + subscriberEmail);
        } else {
            LogManager.getLogger(getClass()).warn("failed to add blog subscriber, virtualUser=" + uid + " email=" + subscriberEmail);
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));

        doc.appendChild(resultElement);

        this.processResponse();
    }
}
