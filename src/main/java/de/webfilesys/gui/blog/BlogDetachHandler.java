package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.servlet.UploadServlet;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogDetachHandler extends XmlRequestHandlerBase {

    public BlogDetachHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        String imgName = getParameter("imgName");
        if (CommonUtils.isEmpty(imgName)) {
            Logger.getLogger(getClass()).error("missing parameter imgName");
            return;
        }

        File imgFile = new File(currentPath, imgName);

        if ((!imgFile.exists()) || (!imgFile.isFile())) {
            Logger.getLogger(getClass()).error("img file is not a readable file: " + imgFile.getAbsolutePath());
            return;
        }
        
        ArrayList<String> attachmentNames = MetaInfManager.getInstance().getListOfAttachments(currentPath, imgName);
        
        if (attachmentNames != null) {
            
            for (String attachmentName : attachmentNames) {
                
                StringBuffer filePath = new StringBuffer(currentPath);
                
                if (!currentPath.endsWith(File.separator)) {
                    filePath.append(File.separatorChar);
                }

                filePath.append(UploadServlet.SUBDIR_ATTACHMENT);
                filePath.append(File.separator);
                filePath.append(attachmentName);
                
                boolean success = false;
                File attachmentFile = new File(filePath.toString());
                if (attachmentFile.exists() && attachmentFile.isFile() && attachmentFile.canWrite()) {
                    if (attachmentFile.delete()) {
                        success = true;
                    }
                }
                if (!success) {
                    Logger.getLogger(getClass()).error("failed to delete attachment file " + filePath);
                }
            }
        }
        
        MetaInfManager.getInstance().removeAttachments(currentPath, imgName);
        
        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.TRUE.toString());

        doc.appendChild(resultElement);

        processResponse();
    }
}
