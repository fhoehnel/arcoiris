package de.webfilesys.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.webfilesys.InvitationManager;
import de.webfilesys.util.MimeTypeMap;

public class SharedFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {

        // prevent caching
        resp.setDateHeader("expires", 0l);
        resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");

        String servletPath = req.getContextPath() + "/sharedFile";

        int servletPathLength = servletPath.length();

        String requestPath = req.getRequestURI();

        if (requestPath.length() <= servletPathLength + 1) {
            Logger.getLogger(getClass()).warn("missing parameters");
            sendNotAuthorizedPage(resp);
            return;
        }

        String accessCode = requestPath.substring(servletPathLength + 1);

        String filePath = InvitationManager.getInstance().getFilePathByAccessCode(accessCode);
        
        if (filePath == null) {
            Logger.getLogger(getClass()).warn("invalid access code: " + accessCode);
            sendNotAuthorizedPage(resp);
            return;
        }
        
        File fileToSend = new File(filePath);

        if (!fileToSend.exists()) {
            Logger.getLogger(getClass()).warn("requested file does not exist: " + filePath);
            sendNotFoundPage(resp);
            return;
        }
        if ((!fileToSend.isFile()) || (!fileToSend.canRead())) {
            Logger.getLogger(getClass()).warn("requested file is not a readable file: " + filePath);
            sendNotFoundPage(resp);
            return;
        }
        
        String mimeType = MimeTypeMap.getInstance().getMimeType(filePath);

        resp.setContentType(mimeType);
        
        long fileSize = fileToSend.length();

        resp.setContentLength((int) fileSize);

        byte buffer[] = null;

        if (fileSize < 16192) {
            buffer = new byte[16192];
        } else {
            buffer = new byte[65536];
        }

        FileInputStream fileInput = null;

        try {
            OutputStream byteOut = resp.getOutputStream();

            fileInput = new FileInputStream(fileToSend);

            int count = 0;
            long bytesWritten = 0;

            while ((count = fileInput.read(buffer)) >= 0) {
                byteOut.write(buffer, 0, count);

                bytesWritten += count;
            }

            if (bytesWritten != fileSize) {
                Logger.getLogger(getClass()).warn("only " + bytesWritten + " bytes of " + fileSize + " have been written to output");
            }

            byteOut.flush();

            buffer = null;
        } catch (IOException ioEx) {
            Logger.getLogger(getClass()).warn(ioEx);
        } finally {
            if (fileInput != null) {
                try {
                    fileInput.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        doGet(req, resp);
    }

    private void sendNotAuthorizedPage(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter out = resp.getWriter();
        out.println("authorization failed");
        out.flush();
    }

    private void sendNotFoundPage(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

        PrintWriter out = resp.getWriter();
        out.println("file not found");
        out.flush();
    }
}
