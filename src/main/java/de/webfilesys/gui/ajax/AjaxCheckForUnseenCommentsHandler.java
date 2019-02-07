package de.webfilesys.gui.ajax;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 * 
 */
public class AjaxCheckForUnseenCommentsHandler extends XmlRequestHandlerBase {

    public AjaxCheckForUnseenCommentsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String path = getCwd();

        int unseenCommentCount = MetaInfManager.getInstance().getUnseenCommentCount(path);
        
        Element resultElement = doc.createElement("result");

        XmlUtil.setElementText(resultElement, Integer.toString(unseenCommentCount));

        doc.appendChild(resultElement);

        processResponse();
    }
}
