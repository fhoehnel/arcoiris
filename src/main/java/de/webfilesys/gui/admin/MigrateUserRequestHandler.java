package de.webfilesys.gui.admin;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.Comment;
import de.webfilesys.GeoTag;
import de.webfilesys.MetaInfManager;
import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.config.BlogConfig;
import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.daytitle.DayTitleManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import de.webfilesys.state.BlogStateManager;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserManager;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UserMigrator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class MigrateUserRequestHandler extends AdminRequestHandler {

    private static final Logger LOG = LogManager.getLogger(MigrateUserRequestHandler.class);

    public MigrateUserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        String userid = getParameter("userid");
        boolean simulate = getParameter("simulate") != null;
        boolean allUsers = getParameter("allUsers") != null;

        output.print("<html>");
        output.print("<head>");

        output.print("<title> arcoiris Blog Administration: Migrate User </title>");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/common.css\">");
        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/admin.css\">");

        output.print("</head>");
        output.print("<body>");

        if (simulate) {
            headLine("arcoiris Blog Administration: Simulate User Migration: " + (userid != null ? userid : ""));
        } else {
            headLine("arcoiris Blog Administration: Migrate User " + (userid != null ? userid : ""));
        }
        output.flush();

        if (allUsers) {
            UserMigrator.getInstance().migrateAllUsers(simulate, output);
        } else {
            TransientUser user = userMgr.getUser(userid);
            if (user == null) {
                LOG.error("user not found: " + userid);
                return;
            }
            UserMigrator.getInstance().migrateSingleUser(user, simulate, output);
        }

        output.println("<hr/>");
        if (simulate) {
            output.println("<p>migration simulation completed</p>");
        } else {
            output.println("<p>migration completed</p>");
        }

        output.println("<br/>");
        output.println("<input type=\"button\" value=\"Return to user list\" onclick=\"window.location.href='" + req.getContextPath() + "/servlet?command=admin&cmd=userList'\">");

        output.println("<script type=\"text/javascript\">setTimeout(\"window.scrollTo(0,1000000)\",1000);</script>");

        output.println("</body>");

        output.println("</html>");
        output.flush();
    }

}
