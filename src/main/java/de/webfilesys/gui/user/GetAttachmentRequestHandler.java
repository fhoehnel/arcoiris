package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.servlet.UploadServlet;

/**
 * @author Frank Hoehnel
 */
public class GetAttachmentRequestHandler extends UserRequestHandler {
    public GetAttachmentRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String attachmentName = getParameter("attachmentName");

        String cwd = getCwd();
        
        StringBuffer filePath = new StringBuffer(cwd);
        
        if (!cwd.endsWith(File.separator)) {
            filePath.append(File.separatorChar);
        }

        filePath.append(UploadServlet.SUBDIR_ATTACHMENT);
        filePath.append(File.separator);
        filePath.append(attachmentName);
        
        setParameter("filePath", filePath.toString());
        
        setParameter("disposition", "inline");
        
        (new GetFileRequestHandler(req, resp, session, output, uid)).handleRequest();        
    }

}
