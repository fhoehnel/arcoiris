package de.webfilesys.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.StatisticManager;
import de.webfilesys.gui.blog.BlogListHandler;
import de.webfilesys.mail.SmtpEmail;
import de.webfilesys.user.UserManager;
import de.webfilesys.util.UTF8URLDecoder;

/**
 * Visitor access to published picture albums. The parameters are provided in
 * the URI path (no URL parameters) to make it search engine friendly.
 * 
 * request path syntax:
 * 
 * /<context-root>/visitor/<userid>/<password>/<viewType>
 * 
 * parameter viewType is optional, default is picture album
 */
public class VisitorServlet extends BlogWebServlet {
    private static final long serialVersionUID = 1L;

    public static final String VISITOR_COOKIE_NAME = "blog-visitor";

    public static final String SESSION_ATTRIB_VISITOR_ID = "visitorId";

    public static final String SESSION_ATTRIB_VISITOR_URL = "visitorURL";
    
    private static final int VISITOR_COOKIE_MAX_AGE = 365 * 24 * 60 * 60; // expires after one year

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {

        // prevent caching
        resp.setDateHeader("expires", 0l);
        resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");

        // done in XslRequestHandlerBase
        // resp.setContentType("text/xml");

        resp.setCharacterEncoding("UTF-8");

        String servletPath = req.getContextPath() + "/visitor";

        int servletPathLength = servletPath.length();

        String requestPath = req.getRequestURI();

        if (requestPath.length() <= servletPathLength + 1) {
            Logger.getLogger(getClass()).error("missing parameters");
            sendErrorPage(resp, "missing parameters");
            return;
        }

        String params = requestPath.substring(servletPathLength + 1);

        String visitorUserId = null;

        String password = null;

        StringTokenizer paramParser = new StringTokenizer(params, "/");
        if (paramParser.hasMoreTokens()) {
            visitorUserId = UTF8URLDecoder.decode(paramParser.nextToken());

            if (paramParser.hasMoreTokens()) {
                password = paramParser.nextToken();
            } else {
                Logger.getLogger(getClass()).error("missing parameter password");
                sendErrorPage(resp, "missing parameter");
                return;
            }
        } else {
            Logger.getLogger(getClass()).error("missing parameter userid");
            sendErrorPage(resp, "missing parameter");
            return;
        }

        UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();

        String clientIP = req.getRemoteAddr();

        String logEntry = null;

        HttpSession session = null;

        // if (userMgr.checkReadonlyPassword(visitorUserId, password)) {
        if (userMgr.checkPassword(visitorUserId, password)) {
            session = req.getSession(true);

            setSessionInfo(req, session);

            session.setAttribute("userid", visitorUserId);

            session.setAttribute("loginEvent", "true");

            session.setAttribute("readonly", "true");

            session.setAttribute("cwd", userMgr.getDocumentRoot(visitorUserId));

            session.removeAttribute("startIdx");

            logEntry = clientIP + ": visitor login user " + visitorUserId;

            String userAgent = req.getHeader("User-Agent");

            if (userAgent != null) {
                logEntry = logEntry + " [" + userAgent + "]";
            }

            Logger.getLogger(getClass()).info(logEntry);

            if ((ArcoirisBlog.getInstance().getMailHost() != null) && ArcoirisBlog.getInstance().isMailNotifyLogin()) {

                ArrayList<String> adminUserEmailList = userMgr.getAdminUserEmails();

                (new SmtpEmail(adminUserEmailList, "visitor login successful", ArcoirisBlog.getInstance().getLogDateFormat().format(new Date()) + " " + logEntry)).send();
            }

            String visitorId = getVisitorIdFromCookie(req);

            if (visitorId == null) {
                visitorId = UUID.randomUUID().toString();
                resp.addCookie(createVisitorCookie(visitorId, req.getContextPath()));
            }

            req.getSession(true).setAttribute(SESSION_ATTRIB_VISITOR_ID, visitorId);

            req.getSession(true).setAttribute(SESSION_ATTRIB_VISITOR_URL, req.getRequestURI());
            
            PrintWriter output = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));

            if (userMgr.getRole(visitorUserId).equals("blog")) {
                (new BlogListHandler(req, resp, session, output, visitorUserId)).handleRequest();
            }
            
            StatisticManager.getInstance().addVisit(visitorUserId, visitorId);
        } else {
            sendNotAuthorizedPage(resp);
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {

        doGet(req, resp);
    }

    private String getVisitorIdFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(VISITOR_COOKIE_NAME)) {
                    return cookies[i].getValue();
                }
            }
        }
        return null;
    }

    private Cookie createVisitorCookie(String visitorId, String contextRoot) {
        Cookie cookie = new Cookie(VISITOR_COOKIE_NAME, visitorId);

        String cookiePath = contextRoot + "/visitor";
        cookie.setPath(cookiePath);
        cookie.setMaxAge(VISITOR_COOKIE_MAX_AGE);
        return cookie;
    }

    private void sendNotAuthorizedPage(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter out = resp.getWriter();
        out.println("authorization failed");
        out.flush();
    }

    private void sendErrorPage(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

        PrintWriter out = resp.getWriter();
        out.println(msg);
        out.flush();
    }
}
