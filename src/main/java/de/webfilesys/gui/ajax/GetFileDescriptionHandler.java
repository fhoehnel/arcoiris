package de.webfilesys.gui.ajax;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class GetFileDescriptionHandler extends XmlRequestHandlerBase {
    public GetFileDescriptionHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String fileName = getParameter("fileName");

        if (CommonUtils.isEmpty(fileName)) {
            Logger.getLogger(getClass()).warn("missing parameter fileName in GetFileDescriptionHandler");
            return;
        }

        String description = MetaInfManager.getInstance().getDescription(getCwd(), fileName);

        Element resultElement = doc.createElement("result");

        XmlUtil.setElementText(resultElement, (description == null ? "" : description));

        doc.appendChild(resultElement);

        processResponse();
    }
}
