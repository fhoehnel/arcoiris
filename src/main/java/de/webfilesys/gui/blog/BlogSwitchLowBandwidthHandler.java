package de.webfilesys.gui.blog;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogSwitchLowBandwidthHandler extends XmlRequestHandlerBase {

    public static final String SESSION_KEY_LOW_BANDWIDTH = "lowBandwidth";
    
    public BlogSwitchLowBandwidthHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        Boolean lowBandwidth = (Boolean) session.getAttribute(SESSION_KEY_LOW_BANDWIDTH);
        
        String newMode;
        
        if (lowBandwidth != null) {
            session.removeAttribute(SESSION_KEY_LOW_BANDWIDTH);
            newMode = "high";
        } else {
            session.setAttribute(SESSION_KEY_LOW_BANDWIDTH, Boolean.TRUE);
            newMode = "low";
        }
        
        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "newBandwidthMode", newMode);

        doc.appendChild(resultElement);

        processResponse();
    }
}
