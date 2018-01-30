/*  
 * WebFileSys
 * Copyright (C) 2011 Frank Hoehnel

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package de.webfilesys.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.ResourceBundleHandler;
import de.webfilesys.ArcoirisBlog;
import de.webfilesys.gui.admin.AdminAddUserRequestHandler;
import de.webfilesys.gui.admin.AdminChangeUserRequestHandler;
import de.webfilesys.gui.admin.AdminEditUserRequestHandler;
import de.webfilesys.gui.admin.AdminMenuRequestHandler;
import de.webfilesys.gui.admin.AdminRegisterUserRequestHandler;
import de.webfilesys.gui.admin.AdminSendEmailRequestHandler;
import de.webfilesys.gui.admin.BroadcastRequestHandler;
import de.webfilesys.gui.admin.DeleteUserRequestHandler;
import de.webfilesys.gui.admin.LoginLogoutHistoryHandler;
import de.webfilesys.gui.admin.SessionListHandler;
import de.webfilesys.gui.admin.UserDiskQuotaHandler;
import de.webfilesys.gui.admin.UserListRequestHandler;
import de.webfilesys.gui.admin.ViewLogRequestHandler;
import de.webfilesys.gui.ajax.AjaxCheckFileExistHandler;
import de.webfilesys.gui.ajax.AjaxCheckForGeoDataHandler;
import de.webfilesys.gui.ajax.GetFileDescriptionHandler;
import de.webfilesys.gui.ajax.XmlEmojiListHandler;
import de.webfilesys.gui.anonymous.VersionInfoRequestHandler;
import de.webfilesys.gui.blog.BlogAddCommentHandler;
import de.webfilesys.gui.blog.BlogAltPositionsHandler;
import de.webfilesys.gui.blog.BlogChangeEntryHandler;
import de.webfilesys.gui.blog.BlogDeleteCommentsHandler;
import de.webfilesys.gui.blog.BlogDeleteEntryHandler;
import de.webfilesys.gui.blog.BlogDetachHandler;
import de.webfilesys.gui.blog.BlogEditEntryHandler;
import de.webfilesys.gui.blog.BlogGetDatesWithEntriesHandler;
import de.webfilesys.gui.blog.BlogGetPublicUrlHandler;
import de.webfilesys.gui.blog.BlogLikeHandler;
import de.webfilesys.gui.blog.BlogListCommentsHandler;
import de.webfilesys.gui.blog.BlogListHandler;
import de.webfilesys.gui.blog.BlogListSubscribersHandler;
import de.webfilesys.gui.blog.BlogMoveEntryHandler;
import de.webfilesys.gui.blog.BlogMoveToPosHandler;
import de.webfilesys.gui.blog.BlogPostHandler;
import de.webfilesys.gui.blog.BlogPublishFormHandler;
import de.webfilesys.gui.blog.BlogPublishHandler;
import de.webfilesys.gui.blog.BlogPublishNewEntriesHandler;
import de.webfilesys.gui.blog.BlogRotateImgHandler;
import de.webfilesys.gui.blog.BlogSaveSettingsHandler;
import de.webfilesys.gui.blog.BlogSearchHandler;
import de.webfilesys.gui.blog.BlogSetDescrHandler;
import de.webfilesys.gui.blog.BlogSetTitlePicHandler;
import de.webfilesys.gui.blog.BlogShowSettingsHandler;
import de.webfilesys.gui.blog.BlogStatisticsHandler;
import de.webfilesys.gui.blog.BlogSubscribeHandler;
import de.webfilesys.gui.blog.BlogSwitchLowBandwidthHandler;
import de.webfilesys.gui.blog.BlogUnpublishHandler;
import de.webfilesys.gui.blog.BlogUnsetTitlePicHandler;
import de.webfilesys.gui.blog.BlogUnsubscribeHandler;
import de.webfilesys.gui.google.GoogleEarthDirPlacemarkHandler;
import de.webfilesys.gui.google.GoogleEarthSinglePlacemarkHandler;
import de.webfilesys.gui.user.ActivateUserRequestHandler;
import de.webfilesys.gui.user.GPXTrackHandler;
import de.webfilesys.gui.user.GetAttachmentRequestHandler;
import de.webfilesys.gui.user.GetFileRequestHandler;
import de.webfilesys.gui.user.OpenStreetMapPOIHandler;
import de.webfilesys.gui.xsl.GPXViewHandler;
import de.webfilesys.gui.xsl.XslGoogleMapHandler;
import de.webfilesys.gui.xsl.XslGoogleMapMultiHandler;
import de.webfilesys.gui.xsl.XslLogonHandler;
import de.webfilesys.gui.xsl.XslOpenStreetMapHandler;
import de.webfilesys.gui.xsl.XslSelfRegistrationHandler;
import de.webfilesys.mail.SmtpEmail;
import de.webfilesys.user.UserManager;
import de.webfilesys.util.UTF8URLDecoder;

/**
 * The main servlet class. Command dispatcher that delegates the work to request
 * handlers.
 * 
 * @author Frank Hoehnel
 */
public class BlogWebServlet extends ServletBase {
    private static final long serialVersionUID = 1L;

    private Properties configProperties = null;

    static boolean initialized = false;

    public void init(ServletConfig config) throws ServletException {
        if (initialized) {
            return;
        }

        ServletContext context = config.getServletContext();

        String realLogDirPath = context.getRealPath("/WEB-INF/log");

        // this system property is used in log4j.xml to specify an absolute path
        // for the log files
        System.setProperty("webfilesys.log.path", realLogDirPath);

        String configFileName = config.getInitParameter("config");

        if ((configFileName == null) || (configFileName.trim().length() == 0)) {
            Logger.getLogger(getClass()).fatal("config file not specified in web.xml");
            throw new ServletException("config file not specified in web.xml");
        }

        String configPath = context.getRealPath(configFileName);

        if ((configPath == null) || (configPath.length() == 0)) {
            Logger.getLogger(getClass()).fatal("cannot determine real path of config file " + configFileName);
            throw new ServletException("cannot determine real path of config file " + configFileName);
        }

        File configFile = new File(configPath);

        if (!configFile.exists()) {
            throw new ServletException("config file does not exist: " + configPath);
        }

        if ((!configFile.isFile()) || (!configFile.canRead())) {
            Logger.getLogger(getClass()).fatal(configPath + " is not a readable file");
            throw new ServletException(configPath + " is not a readable file");
        }

        configProperties = new Properties();

        FileInputStream propFile = null;

        try {
            propFile = new FileInputStream(configFile);

            configProperties.load(propFile);

            Logger.getLogger(getClass()).info("properties loaded from " + configFile);
        } catch (IOException ioEx) {
            Logger.getLogger(getClass()).fatal("error reading config file: " + ioEx);
            throw new ServletException("error reading config file: " + ioEx);
        } finally {
            if (propFile != null) {
                try {
                    propFile.close();
                } catch (IOException ex) {
                }
            }
        }

        String webAppRootDir = context.getRealPath("/");

        if ((!webAppRootDir.endsWith(File.separator)) && (!webAppRootDir.endsWith("/"))) {
            webAppRootDir = webAppRootDir + File.separator;
        }

        String contextRoot = null;
        StringTokenizer pathParser = new StringTokenizer(webAppRootDir, "/\\");
        while (pathParser.hasMoreTokens()) {
            contextRoot = pathParser.nextToken();
        }

        configProperties.setProperty("contextRoot", contextRoot);

        ArcoirisBlog webFileSys = ArcoirisBlog.createInstance(configProperties, webAppRootDir);

        webFileSys.initialize(configProperties);

        initialized = true;
    }

    public void destroy() {
        super.destroy();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {

        PrintWriter output = null;

        String command = null;

        int servletPathLength = (req.getContextPath() + "/servlet").length();

        String requestPath = req.getRequestURI();

        if (requestPath.length() > servletPathLength) {
            command = "getFile";

            if (File.separatorChar == '\\') {
                if (requestPath.length() > servletPathLength + 1) {
                    req.setAttribute("filePath", UTF8URLDecoder.decode(requestPath.substring(servletPathLength + 1)));
                } else {
                    Logger.getLogger(getClass()).warn("invalid request path: " + requestPath);
                }
            } else {
                req.setAttribute("filePath", UTF8URLDecoder.decode(requestPath.substring(servletPathLength)));
            }
        } else {
            command = req.getParameter("command");
        }

        if ((command == null) || ((!command.equals("getFile")) && (!command.equals("getAttachment")))) {
            // resp.setCharacterEncoding("ISO-8859-1");
            resp.setCharacterEncoding("UTF-8");

            output = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));
        }

        String clientIP = req.getRemoteAddr();

        StringBuffer logEntry = new StringBuffer();

        logEntry.append(clientIP);
        logEntry.append(' ');
        logEntry.append(req.getMethod());
        logEntry.append(' ');

        logEntry.append(req.getRequestURI());

        String queryString = req.getQueryString();
        if (queryString != null) {
            if (queryString.indexOf("silentLogin") < 0) {
                logEntry.append('?');
                logEntry.append(queryString);
            }
        }

        logEntry.append(" (");
        logEntry.append(req.getProtocol());
        logEntry.append(')');

        Logger.getLogger(getClass()).info(logEntry.toString());

        String localIP = ArcoirisBlog.getInstance().getLocalIPAddress();

        boolean requestIsLocal = clientIP.equals(localIP) || clientIP.equals(ArcoirisBlog.getInstance().getLoopbackAddress());

        // prevent caching
        // will be overwritten in GetFileRequestHandler with Parameter
        // cache=true
        // and in ResourceBundleHandler
        resp.setDateHeader("expires", 0l);
        resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");

        // inserted 2008/10/08
        // content type will be overwritten in some request handlers
        // last call to setContentType() wins
        resp.setContentType("text/html");

        String userid = null;

        HttpSession session = req.getSession(false);

        if (session != null) {
            userid = (String) session.getAttribute("userid");

            if (userid != null) {
                if (handleCommand(command, userid, req, resp, session, output, requestIsLocal)) {
                    return;
                }

                if (anonymousCommand(command, req, resp, output, requestIsLocal)) {
                    return;
                }
            } else {
                if (anonymousCommand(command, req, resp, output, requestIsLocal)) {
                    return;
                }

                if ((command != null) && command.equals("loginForm")) {
                    (new XslLogonHandler(req, resp, session, output, false)).handleRequest();

                    return;
                }

                if (output == null) {
                    output = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));
                }
                redirectToLogin(req, resp);
            }
        } else {
            session = req.getSession(true);

            setSessionInfo(req, session);

            if (anonymousCommand(command, req, resp, output, requestIsLocal)) {
                return;
            }

            if ((command != null) && command.equals("loginForm")) {
                (new XslLogonHandler(req, resp, session, output, false)).handleRequest();

                return;
            }

            if (output == null) {
                output = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));
            }
            redirectToLogin(req, resp);
        }

        if (output != null) {
            output.flush();
        }

        return;
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        doGet(req, resp);
    }

    private boolean anonymousCommand(String command, HttpServletRequest req, HttpServletResponse resp, PrintWriter output, boolean requestIsLocal) {
        if (command == null) {
            return (false);
        }

        if (command.equals("getResourceBundle")) {
            (new ResourceBundleHandler(req, resp, output)).handleRequest();

            return true;
        }

        if (command.equals("login")) {
            verifyLogin(req, resp, output, requestIsLocal);

            return true;
        }

        if (command.equals("registerSelf")) {
            (new XslSelfRegistrationHandler(req, resp, req.getSession(true), output)).handleRequest();

            return true;
        }

        if (command.equals("activateUser")) {
            (new ActivateUserRequestHandler(req, resp)).handleRequest();

            return true;
        }

        if (command.equals("versionInfo")) {
            (new VersionInfoRequestHandler(req, output)).handleRequest();

            return true;
        }

        if (command.equals("blog")) {
            String cmd = req.getParameter("cmd");
            if ((cmd != null) && cmd.equals("unsubscribe")) {
                (new BlogUnsubscribeHandler(req, resp, req.getSession(true), output, null)).handleRequest();
                return true;
            }
        }

        return (false);
    }

    private boolean handleCommand(String command, String userid, HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, boolean requestIsLocal) {
        if (command == null) {
            UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();

            String role = userMgr.getRole(userid);

            if (role != null) {
                if (role.equals("blog")) {
                    (new BlogListHandler(req, resp, session, output, userid)).handleRequest();
                }

                if (role.equals("admin")) {
                    (new AdminMenuRequestHandler(req, resp, session, output, userid)).handleRequest();
                }
            }

            return true;
        }

        if (command.equals("admin")) {
            String cmd = req.getParameter("cmd");

            if (cmd == null) {
                cmd = "menu";
            }

            if (cmd.equals("menu")) {
                (new AdminMenuRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("userList")) {
                (new UserListRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("editUser")) {
                (new AdminEditUserRequestHandler(req, resp, session, output, userid, null)).handleRequest();
                return true;
            } else if (cmd.equals("changeUser")) {
                (new AdminChangeUserRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("registerUser")) {
                (new AdminRegisterUserRequestHandler(req, resp, session, output, userid, null)).handleRequest();
                return true;
            } else if (cmd.equals("addUser")) {
                (new AdminAddUserRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("deleteUser")) {
                (new DeleteUserRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("viewLog")) {
                (new ViewLogRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("broadcast")) {
                (new BroadcastRequestHandler(req, resp, session, output, userid, null)).handleRequest();
                return true;
            } else if (cmd.equals("sendEmail")) {
                (new AdminSendEmailRequestHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("sessionList")) {
                (new SessionListHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("loginHistory")) {
                (new LoginLogoutHistoryHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("userDiskQuota")) {
                (new UserDiskQuotaHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            }
            
            Logger.getLogger(getClass()).info("unknown admin comamnd: " + cmd);
            return true;
        }

        if (command.equals("getFile")) {
            (new GetFileRequestHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("getAttachment")) {
            (new GetAttachmentRequestHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("getResourceBundle")) {
            (new ResourceBundleHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("blog")) {
            String cmd = req.getParameter("cmd");

            if (cmd == null) {
                cmd = "list";
            }

            if (cmd.equals("list")) {
                (new BlogListHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("post")) {
                (new BlogPostHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("editEntry")) {
                (new BlogEditEntryHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("changeEntry")) {
                (new BlogChangeEntryHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("setDescr")) {
                (new BlogSetDescrHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("deleteEntry")) {
                (new BlogDeleteEntryHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("moveEntry")) {
                (new BlogMoveEntryHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("publishForm")) {
                (new BlogPublishFormHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("publish")) {
                (new BlogPublishHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("getPublicURL")) {
                (new BlogGetPublicUrlHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("unpublish")) {
                (new BlogUnpublishHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("listComments")) {
                (new BlogListCommentsHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("addComment")) {
                (new BlogAddCommentHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("delComments")) {
                (new BlogDeleteCommentsHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("rotate")) {
                (new BlogRotateImgHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("showSettings")) {
                (new BlogShowSettingsHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("saveSettings")) {
                (new BlogSaveSettingsHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("subscribe")) {
                (new BlogSubscribeHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("unsubscribe")) {
                (new BlogUnsubscribeHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("listSubscribers")) {
                (new BlogListSubscribersHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("altPositions")) {
                (new BlogAltPositionsHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("moveToPos")) {
                (new BlogMoveToPosHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("emojiList")) {
                (new XmlEmojiListHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("setTitlePic")) {
                (new BlogSetTitlePicHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("unsetTitlePic")) {
                (new BlogUnsetTitlePicHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("like")) {
                (new BlogLikeHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("search")) {
                (new BlogSearchHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("publishNewEntries")) {
                (new BlogPublishNewEntriesHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("detach")) {
                (new BlogDetachHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("datesWithEntries")) {
                (new BlogGetDatesWithEntriesHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("switchLowBandwidthMode")) {
                (new BlogSwitchLowBandwidthHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else if (cmd.equals("statistics")) {
                (new BlogStatisticsHandler(req, resp, session, output, userid)).handleRequest();
                return true;
            } else {
                Logger.getLogger(getClass()).info("unknown blog comamnd: " + cmd);
                return true;
            }
        }

        if (command.equals("ajaxRPC")) {
            String method = req.getParameter("method");

            if (method.equals("checkForGeoData")) {
                (new AjaxCheckForGeoDataHandler(req, resp, session, output, userid)).handleRequest();
            } else if (method.equals("existFile")) {
                (new AjaxCheckFileExistHandler(req, resp, session, output, userid)).handleRequest();
            }

            return true;
        }

        if (command.equals("getFileDesc")) {
            (new GetFileDescriptionHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("googleMap")) {
            (new XslGoogleMapHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("googleMapMulti")) {
            (new XslGoogleMapMultiHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("osMap")) {
            (new XslOpenStreetMapHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("osmPOIList")) {
            (new OpenStreetMapPOIHandler(req, resp, session, output, userid)).handleRequest();

            return true;
        }

        if (command.equals("googleEarthPlacemark")) {
            (new GoogleEarthSinglePlacemarkHandler(req, resp, session, output, userid)).handleRequest();
            return true;
        }

        if (command.equals("googleEarthDirPlacemarks")) {
            (new GoogleEarthDirPlacemarkHandler(req, resp, session, output, userid)).handleRequest();
            return true;
        }
        
        if (command.equals("viewGPX")) {
            (new GPXViewHandler(req, resp, session, output, userid)).handleRequest();
            return(true);
        }

        if (command.equals("gpxTrack")) {
            (new GPXTrackHandler(req, resp, session, output, userid)).handleRequest();
            return(true);
        }
        
        if (command.equals("logout")) {
            logout(req, resp, session, userid);

            return true;
        }

        return (false);
    }

    protected void logout(HttpServletRequest req, HttpServletResponse resp, HttpSession session, String userid) {
        UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();

        session.removeAttribute("userid");

        session.invalidate();

        String logoutPage = req.getContextPath() + "/servlet";

        if (ArcoirisBlog.getInstance().getLogoutURL() != null) {
            logoutPage = ArcoirisBlog.getInstance().getLogoutURL();
        }

        Logger.getLogger(getClass()).info(req.getRemoteAddr() + ": logout user " + userid);

        try {
            resp.sendRedirect(logoutPage);
        } catch (IOException ioex) {
            Logger.getLogger(getClass()).warn(ioex);
        }
    }

    public void verifyLogin(HttpServletRequest req, HttpServletResponse resp, PrintWriter output, boolean requestIsLocal) {
        String userid = req.getParameter("userid");
        String password = req.getParameter("password");

        String clientIP = req.getRemoteAddr();

        UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();

        String logEntry = null;

        HttpSession session = null;

        if ((userid != null) && (password != null)) {
            if (userMgr.checkPassword(userid, password)) {
                session = req.getSession(false);
                if (session != null) {
                    Logger.getLogger(getClass()).debug("destroying existing session");
                    session.invalidate();
                }

                session = req.getSession(true);

                setSessionInfo(req, session);

                session.setAttribute("userid", userid);

                session.setAttribute("loginEvent", "true");

                String browserType = req.getHeader("User-Agent");

                ArcoirisBlog.getInstance().getUserMgr().setLastLoginTime(userid, new Date());

                logEntry = clientIP + ": login user " + userid;

                if (browserType != null) {
                    logEntry = logEntry + " [" + browserType + "]";
                }

                Logger.getLogger(getClass()).info(logEntry);

                if ((ArcoirisBlog.getInstance().getMailHost() != null) && ArcoirisBlog.getInstance().isMailNotifyLogin()) {
                    ArrayList<String> adminUserEmailList = userMgr.getAdminUserEmails();

                    (new SmtpEmail(adminUserEmailList, "login successful", ArcoirisBlog.getInstance().getLogDateFormat().format(new Date()) + " " + logEntry)).send();
                }

                String role = userMgr.getRole(userid);

                if (role != null) {
                    if (role.equals("admin")) {
                        (new AdminMenuRequestHandler(req, resp, session, output, userid)).handleRequest();
                        return;
                    }

                    if (role.equals("blog")) {
                        try {
                            resp.sendRedirect(req.getContextPath() + "/servlet?command=blog");
                            return;
                        } catch (IOException ex) {
                            Logger.getLogger(getClass()).warn("failed to redirect to blog handler", ex);
                        }
                    }
                }
            }
        }

        logEntry = clientIP + ": login failed for user " + userid;
        Logger.getLogger(getClass()).warn(logEntry);

        if ((ArcoirisBlog.getInstance().getMailHost() != null) && ArcoirisBlog.getInstance().isMailNotifyLogin()) {
            ArrayList<String> adminUserEmailList = userMgr.getAdminUserEmails();

            (new SmtpEmail(adminUserEmailList, "login failed", ArcoirisBlog.getInstance().getLogDateFormat().format(new Date()) + " " + logEntry)).send();
        }

        if (ArcoirisBlog.getInstance().getLoginErrorPage() != null) {
            try {
                resp.sendRedirect(ArcoirisBlog.getInstance().getLoginErrorPage());
            } catch (IOException ioex) {
                Logger.getLogger(getClass()).warn(ioex);
            }

            return;
        }

        (new XslLogonHandler(req, resp, session, output, true)).handleRequest();
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) {
        String redirectUrl = req.getContextPath() + "/servlet?command=loginForm";

        try {
            resp.sendRedirect(redirectUrl);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).warn("redirect failed", ex);
        }
    }
}
