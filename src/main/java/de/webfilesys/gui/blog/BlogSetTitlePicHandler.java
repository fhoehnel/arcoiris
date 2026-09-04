package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.config.BlogConfigManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogSetTitlePicHandler extends XmlRequestHandlerBase {

    public BlogSetTitlePicHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        if (!checkWriteAccess()) {
            return;
        }

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

        BlogConfigManager.getInstance().setTitlePic(currentPath, imgName);

        boolean success = true;

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));

        doc.appendChild(resultElement);

        processResponse();
    }
}
