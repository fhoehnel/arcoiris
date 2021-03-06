package de.webfilesys.gui.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.ViewHandlerConfig;
import de.webfilesys.ViewHandlerManager;
import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.FileEncodingMap;
import de.webfilesys.util.MimeTypeMap;
import de.webfilesys.viewhandler.ViewHandler;

/**
 * @author Frank Hoehnel
 */
public class GetFileRequestHandler extends UserRequestHandler {
    public GetFileRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String filePath = getParameter("filePath");

        if (filePath == null) {
            String fileName = getParameter("fileName");
            if (fileName != null) {
                String cwdPath = getCwd();
                if (cwdPath != null) {
                    String thumbParam = getParameter("thumb");
                    if ((thumbParam != null) && thumbParam.equalsIgnoreCase("true")) {
                        filePath = CommonUtils.joinFilesysPath(cwdPath, CommonUtils.joinFilesysPath(BlogThumbnailHandler.BLOG_THUMB_PATH, fileName));
                    } else {
                        filePath = CommonUtils.joinFilesysPath(cwdPath, fileName);
                    }
                }
            }
        }

        if (!this.checkAccess(filePath)) {
            Logger.getLogger(getClass()).warn("unauthorized access to " + filePath);
            return;
        }

        boolean error = false;

        File fileToSend = new File(filePath);

        if (!fileToSend.exists()) {
            Logger.getLogger(getClass()).warn("requested file does not exist: " + filePath);

            error = true;
        } else if ((!fileToSend.isFile()) || (!fileToSend.canRead())) {
            Logger.getLogger(getClass()).warn("requested file is not a readable file: " + filePath);

            error = true;
        }

        if (error) {
            resp.setStatus(404);

            try {
                PrintWriter output = new PrintWriter(resp.getWriter());

                output.println("File not found or not readable: " + filePath);

                output.flush();

                return;
            } catch (IOException ioEx) {
                Logger.getLogger(getClass()).warn(ioEx);
            }
        }

        String disposition = getParameter("disposition");

        String mimeType = MimeTypeMap.getInstance().getMimeType(filePath);

        resp.setContentType(mimeType);
        
        String cached = getParameter("cached");

        if ((cached != null) && (cached.equals("true"))) {
            // overwrite the no chache headers already set in WebFileSysServlet
            // resp.setHeader("Cache-Control", null);
            resp.setHeader("Cache-Control", "public, max-age=3600, s-maxage=3600");
            resp.setDateHeader("expires", System.currentTimeMillis() + (60 * 60 * 1000)); // now + 10 hours
        }

        if (!CommonUtils.isEmpty(disposition)) {
            if (disposition.equals("download")) {
                resp.setHeader("Content-Disposition", "attachment; filename=" + fileToSend.getName());
            } else if (disposition.equals("inline")) {
                resp.setHeader("Content-Disposition", "inline; filename=" + fileToSend.getName());
            }
        }
        
        if (disposition == null) {
            ViewHandlerConfig viewHandlerConfig = ViewHandlerManager.getInstance().getViewHandlerConfig(fileToSend.getName());

            if (viewHandlerConfig != null) {
                String viewHandlerClassName = viewHandlerConfig.getHandlerClass();
              
                if (viewHandlerClassName != null) {
                    if (delegateToViewHandler(viewHandlerConfig, filePath, null)) {
                        return;
                    }
                }
            }
        }        

        String encoding = FileEncodingMap.getInstance().getFileEncoding(filePath);

        if (encoding != null) {
            resp.setCharacterEncoding(encoding);
        }

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

            /*
             * if (WebFileSys.getInstance().isDownloadStatistics()) {
             * MetaInfManager.getInstance().incrementDownloads(filePath); }
             */
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

    protected boolean delegateToViewHandler(ViewHandlerConfig viewHandlerConfig, String filePath, InputStream zipIn) {
        String viewHandlerClassName = viewHandlerConfig.getHandlerClass();

        try {
            ViewHandler viewHandler = (ViewHandler) (Class.forName(viewHandlerClassName).newInstance());

            Logger.getLogger(getClass()).debug("ViewHandler instantiated: " + viewHandler.getClass().getName());

            if (zipIn == null) {
                viewHandler.process(filePath, viewHandlerConfig, req, resp);
                return true;
            }

            if (!viewHandler.supportsZipContent()) {
                return false;
            }

            viewHandler.processZipContent(filePath, zipIn, viewHandlerConfig, req, resp);

            return (true);
        } catch (ClassNotFoundException cnfex) {
            Logger.getLogger(getClass()).error("Viewhandler class " + viewHandlerClassName + " cannot be found: " + cnfex);
        } catch (InstantiationException instEx) {
            Logger.getLogger(getClass()).error("Viewhandler class " + viewHandlerClassName + " cannot be instantiated: " + instEx);
        } catch (IllegalAccessException iaEx) {
            Logger.getLogger(getClass()).error("Viewhandler class " + viewHandlerClassName + " cannot be instantiated: " + iaEx);
        } catch (ClassCastException cex) {
            Logger.getLogger(getClass()).error("Viewhandler class " + viewHandlerClassName + " does not implement the ViewHandler interface: " + cex);
        }

        return (false);
    }
    
}
