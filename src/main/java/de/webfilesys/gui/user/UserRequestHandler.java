package de.webfilesys.gui.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.LanguageManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ProtectedRequestHandler;
import de.webfilesys.util.FileEncodingMap;
import de.webfilesys.util.HTTPUtils;

/**
 * Base handler class for non-anonymous and non-admin requests.
 * 
 * @author Frank Hoehnel
 */
public class UserRequestHandler extends ProtectedRequestHandler {
    protected String language = null;

    protected boolean readonly = true;

    public UserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);

        if (uid != null) {
            language = userMgr.getLanguage(uid);
        } else {
            language = LanguageManager.getInstance().getDefaultLanguage();
        }

        if (uid != null) {
            String sessionReadonly = (String) session.getAttribute("readonly");

            readonly = (sessionReadonly != null) || userMgr.isReadonly(uid);
        }
    }

    public void handleRequest() {
        process();
    }

    public String getResource(String key, String defaultValue) {
        return (LanguageManager.getInstance().getResource(language, key, defaultValue));
    }

    public boolean isReadonlySession() {
        boolean sessionReadonly = false;

        String sessRO = (String) session.getAttribute("readonly");
        if (sessRO != null) {
            sessionReadonly = Boolean.valueOf(sessRO);
        }

        return sessionReadonly || userMgr.isReadonly(uid);
    }
    
    public boolean checkWriteAccess() {
        boolean sessionReadonly = false;

        String sessRO = (String) session.getAttribute("readonly");
        if (sessRO != null) {
            sessionReadonly = Boolean.valueOf(sessRO);
        }

        boolean readonly = sessionReadonly || userMgr.isReadonly(uid);

        if (!readonly) {
            return (true);
        }

        Logger.getLogger(getClass()).warn("read-only user " + uid + " tried write access");

        output.print(HTTPUtils.createHTMLHeader());

        output.println("<HTML>");
        output.println("<HEAD>");
        output.println("<TITLE> Unauthorized access </TITLE>");
        output.println("<script language=\"javascript\">");
        output.println("alert('Write access is required to perform this operation!');");
        output.println("history.back();");
        output.println("</script>");

        output.println("</html>");
        output.flush();

        return (false);
    }

    public static boolean dirIsLink(File f) {
        if (File.separatorChar != '/') {
            // there is no way to detect NTFS symbolic links / junctions with
            // Java functions
            // see
            // http://stackoverflow.com/questions/3249117/cross-platform-way-to-detect-a-symbolic-link-junction-point
            // possible workaround: if the directory is not empty, files in the
            // linked directory
            // should have a canonical path that does not start with the path of
            // the parent dir

            return (false);
        }

        try {
            return (!(f.getCanonicalPath().equals(f.getAbsolutePath())));
        } catch (IOException ioex) {
            Logger.getLogger(UserRequestHandler.class).warn(ioex);
            return (false);
        }
    }

    protected boolean delDirTree(String path) {
        boolean deleteError = false;

        File dirToBeDeleted = new File(path);
        String fileList[] = dirToBeDeleted.list();

        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                File tempFile = new File(path + File.separator + fileList[i]);
                if (tempFile.isDirectory()) {
                    if (!delDirTree(path + File.separator + fileList[i]))
                        deleteError = true;
                } else {
                    String absolutePath = tempFile.getAbsolutePath();

                    if (!tempFile.delete()) {
                        deleteError = true;
                        Logger.getLogger(getClass()).warn("cannot delete " + tempFile);
                    } else {
                        MetaInfManager.getInstance().removeMetaInf(absolutePath);
                    }
                }
            }
        }

        if (!dirToBeDeleted.delete()) {
            deleteError = true;
        } else {
            MetaInfManager.getInstance().releaseMetaInf(path);
        }

        return (!(deleteError));
    }

    protected boolean isMobile() {
        return (session.getAttribute("mobile") != null);
    }

    /**
     * Guess the character encoing of the file.
     * 
     * @param filePath
     *            path and filename
     * @return encoding or null, if unknown
     */
    protected String guessFileEncoding(String filePath) {
        try {
            FileInputStream fin = new FileInputStream(filePath);

            int byte1 = fin.read();
            if (byte1 != (-1)) {
                int byte2 = fin.read();
                if (byte2 != (-1)) {
                    int byte3 = fin.read();
                    if ((byte1 == 0xef) && (byte2 == 0xbb) && (byte3 == 0xbf)) {
                        // BOM found - UTF-8
                        return "UTF-8-BOM";
                    }
                }
            }
            fin.close();

        } catch (IOException ioex) {
            Logger.getLogger(getClass()).warn("cannot determine file encoding for " + filePath);
        }

        return FileEncodingMap.getInstance().getFileEncoding(filePath);
    }

    protected String formatBlogDate(Date day) {
        StringBuffer buff = new StringBuffer();

        int weekday = day.getDay();

        switch (weekday) {
        case 0:
            buff.append(getResource("calendar.sunday", "sunday"));
            break;
        case 1:
            buff.append(getResource("calendar.monday", "monday"));
            break;
        case 2:
            buff.append(getResource("calendar.tuesday", "tuesday"));
            break;
        case 3:
            buff.append(getResource("calendar.wednesday", "wednesday"));
            break;
        case 4:
            buff.append(getResource("calendar.thursday", "thursday"));
            break;
        case 5:
            buff.append(getResource("calendar.friday", "friday"));
            break;
        case 6:
            buff.append(getResource("calendar.saturday", "saturday"));
        }

        buff.append(", ");

        buff.append(day.getDate());

        buff.append(' ');

        int month = day.getMonth();

        switch (month) {
        case 0:
            buff.append(getResource("calendar.january", "january"));
            break;
        case 1:
            buff.append(getResource("calendar.february", "february"));
            break;
        case 2:
            buff.append(getResource("calendar.march", "march"));
            break;
        case 3:
            buff.append(getResource("calendar.april", "april"));
            break;
        case 4:
            buff.append(getResource("calendar.may", "may"));
            break;
        case 5:
            buff.append(getResource("calendar.june", "june"));
            break;
        case 6:
            buff.append(getResource("calendar.july", "july"));
            break;
        case 7:
            buff.append(getResource("calendar.august", "august"));
            break;
        case 8:
            buff.append(getResource("calendar.september", "september"));
            break;
        case 9:
            buff.append(getResource("calendar.october", "october"));
            break;
        case 10:
            buff.append(getResource("calendar.november", "november"));
            break;
        case 11:
            buff.append(getResource("calendar.december", "december"));
        }

        buff.append(' ');

        buff.append(day.getYear() + 1900);

        return buff.toString();
    }
}
