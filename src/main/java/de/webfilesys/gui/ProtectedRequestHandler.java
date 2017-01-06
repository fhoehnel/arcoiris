package de.webfilesys.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.user.UserManager;
import de.webfilesys.util.HTTPUtils;

/**
 * @author Frank Hoehnel
 */
public class ProtectedRequestHandler extends RequestHandler {
    private static final Logger LOG = Logger.getLogger(ProtectedRequestHandler.class);

    public String uid = null;

    protected long treeFileSize = 0L;

    protected UserManager userMgr = null;

    public ProtectedRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output);

        this.uid = uid;

        userMgr = ArcoirisBlog.getInstance().getUserMgr();
    }

    public void handleRequest() {
        process();
    }

    protected void process() {
    }

    protected boolean isAdminUser(boolean sendErrorPage) {
        String role = userMgr.getRole(uid);

        if ((role != null) && role.equals("admin")) {
            return (true);
        }

        if (!sendErrorPage) {
            return (false);
        }

        output.print(HTTPUtils.createHTMLHeader());

        output.println("<HTML>");
        output.println("<HEAD>");
        output.println("<TITLE> arcoiris Administration </TITLE>");
        output.println("</HEAD>");
        output.println("<BODY>");
        output.println("You are not an admin user!");
        output.println("</html></body>");
        output.flush();
        return (false);
    }

    protected boolean accessAllowed(String fileName) {
        if (fileName.indexOf("..") >= 0) {
            return (false);
        }

        if (File.separatorChar == '\\') {
            String lowerCaseDocRoot = userMgr.getLowerCaseDocRoot(uid);

            String formattedDocName = fileName.toLowerCase().replace('\\', '/');

            return (formattedDocName.startsWith(lowerCaseDocRoot) && ((formattedDocName.length() == lowerCaseDocRoot.length()) || (formattedDocName.charAt(lowerCaseDocRoot
                            .length()) == '/')));
        }

        String docRoot = userMgr.getDocumentRoot(uid);

        if (docRoot.equals("/")) {
            return (true);
        }

        return (fileName.startsWith(docRoot) && ((fileName.length() == docRoot.length()) || (fileName.charAt(docRoot.length()) == '/')));
    }

    protected boolean checkAccess(String fileName) {
        if (accessAllowed(fileName)) {
            return (true);
        }

        LOG.warn("user " + uid + " tried to access file outside of the document root: " + fileName);

        return (false);
    }

    public boolean copyFile(String sourceFilePath, String destFilePath) {
        if ((sourceFilePath == null) || (destFilePath == null)) {
            throw new IllegalArgumentException("source or target file for copy opertaion is null");
        }

        if (sourceFilePath.equals(destFilePath)) {
            LOG.warn("copy source equals destination: " + sourceFilePath);
            return (false);
        }

        File sourceFile = new File(sourceFilePath);
        long lastChangeDate = sourceFile.lastModified();

        boolean copyFailed = false;

        BufferedInputStream fin = null;
        BufferedOutputStream fout = null;

        try {
            fin = new BufferedInputStream(new FileInputStream(sourceFilePath));
            fout = new BufferedOutputStream(new FileOutputStream(destFilePath));

            byte[] buff = new byte[4096];
            int count;

            while ((count = fin.read(buff)) >= 0) {
                fout.write(buff, 0, count);
            }
        } catch (Exception e) {
            LOG.error("failed to copy file " + sourceFilePath + " to " + destFilePath, e);
            copyFailed = true;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception ex) {
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex) {
                }
            }
        }

        if (!copyFailed) {
            File destFile = new File(destFilePath);
            destFile.setLastModified(lastChangeDate);
        }

        return (!copyFailed);
    }

}
