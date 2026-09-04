package de.webfilesys.gui.admin;

import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;

/**
 * @author Frank Hoehnel
 */
public abstract class LogRequestHandlerBase extends AdminRequestHandler {
    /** name of the WebFileSys logger defined in log4j.xml */
    public static final String WEBFILESYS_LOGGER_NAME = "de.webfilesys";

    /**
     * name of the DailyRollingFileAppender defined in log4j.xml for the
     * de.webfilesys logger
     */
    public static final String APPENDER_NAME = "WebFileSysLogAppender";

    public LogRequestHandlerBase(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected String getSystemLogFilePath() {
        org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(WEBFILESYS_LOGGER_NAME);

        for (Appender appender : logger.getContext().getConfiguration()
                .getLoggerConfig(WEBFILESYS_LOGGER_NAME)
                .getAppenders()
                .values()) {
            if (APPENDER_NAME.equals(appender.getName())) {
                if (appender instanceof FileAppender) {
                    return ((FileAppender) appender).getFileName();
                }
            }
        }

        return null;
    }
}
