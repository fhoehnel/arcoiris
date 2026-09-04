package de.webfilesys;

import java.util.Enumeration;
import java.util.Hashtable;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.webfilesys.user.UserManager;
import de.webfilesys.user.UserManagerBase;

public class SessionHandler implements HttpSessionListener, ServletContextListener {
    private static int activeSessions = 0;

    private static Hashtable sessionList = new Hashtable();

    /**
     * @see jakarta.servlet.http.HttpSessionListener#sessionCreated(HttpSessionEvent)
     */
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        HttpSession session = sessionEvent.getSession();

        String sessionId = session.getId();

        sessionList.put(sessionId, session);

        activeSessions++;

        if (activeSessions >= 0) // this value can be negative because of
                                 // sessions that survived tomcat restart
        {
            // LogManager.getLogger(getClass()).debug("active sessions: " +
            // activeSessions);
        }
    }

    /**
     * @see jakarta.servlet.http.HttpSessionListener#sessionDestroyed(HttpSessionEvent)
     */
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        HttpSession session = sessionEvent.getSession();

        String sessionId = null;

        try {
            sessionId = session.getId();

            sessionList.remove(sessionId);

            String userid = (String) session.getAttribute("userid");

            if (userid == null) {
                LogManager.getLogger(getClass()).info("session expired/destroyed with id " + sessionId);
            } else {
                LogManager.getLogger(getClass()).info("session expired/destroyed for user: " + userid + " sessionId: " + sessionId);
            }
        } catch (IllegalStateException iex) {
            LogManager.getLogger(getClass()).info("session expired/destroyed with id " + sessionId);

            // In tomcat version 4 the session has already been invalidated when
            // sessionDestroyed()
            // is called. So we get an IllegalStateException when we try to read
            // the userid attribute.
            // In tomcat version 5 sessionDestroyed() is called before the
            // session is being invalidated.

            LogManager.getLogger(getClass()).debug(iex);
        }

        activeSessions--;

        LogManager.getLogger(getClass()).debug("active sessions: " + activeSessions);
    }

    public static Enumeration getSessions() {
        return (sessionList.elements());
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        // ServletContext servletContext = servletContextEvent.getServletContext
        // ();
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // ServletContext servletContext = servletContextEvent.getServletContext
        // ();

        LogManager.getLogger(getClass()).info("saving and cleaning up on context shutdown");

        UserManager userMgr = ArcoirisBlog.getInstance().getUserMgr();

        ((UserManagerBase) userMgr).interrupt();

        BlogMetaInfManager.getInstance().interrupt();

        InvitationManager.getInstance().interrupt();
        
        StatisticManager.getInstance().interrupt();
        
        if (ArcoirisBlog.getInstance().getDiskQuotaInspector() != null) {
            ArcoirisBlog.getInstance().getDiskQuotaInspector().interrupt();
        }

        do {
            try {
                Thread.currentThread().sleep(3000);
            } catch (InterruptedException iex) {
                System.out.println(iex);
            }
        } while (!userMgr.isReadyForShutdown());

        LogManager.getLogger(getClass()).info("arcoiris blog server ready for shutdown");
    }

}
