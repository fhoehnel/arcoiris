package de.webfilesys.gui.admin;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.ArcoirisBlog;

/**
 * @author Frank Hoehnel
 */
public class AdminMenuRequestHandler extends AdminRequestHandler {
    public AdminMenuRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        output.println("<html>");
        output.println("<head>");
        output.println("<title>arcoiris Blog Administration</title>");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/common.css\">");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/admin.css\">");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/icons.css\">");

        output.println("</head>");
        output.println("<body>");

        headLine("arcoiris Blog Administration");

        output.println("<br>");
        
        output.println("<div class=\"adminMenuEntry\">");
        output.println("<span class=\"icon-font icon-user iconAdminMenu\"></span>");
        output.println("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=userList&initial=true\">User Management</a>");
        output.println("</div>");
        

        output.println("<div class=\"adminMenuEntry\">");
        output.println("<span class=\"icon-font icon-watch iconAdminMenu\"></span>");
        output.println("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=sessionList\">Active Sessions</a>");
        output.println("</div>");

        if (ArcoirisBlog.getInstance().getMailHost() != null) {
            output.println("<div class=\"adminMenuEntry\">");
            output.println("<span class=\"icon-font icon-mail iconAdminMenu\"></span>");
            output.println("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=broadcast\">Broadcast e-mail</a>");
            output.println("</div>");
        }
        
        output.println("<div class=\"adminMenuEntry\">");
        output.println("<span class=\"icon-font icon-hddrive iconAdminMenu\"></span>");
        output.println("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=viewLog\">View Event Log</a>");
        output.println("</div>");
        
        output.println("<div class=\"adminMenuEntry\">");
        output.println("<span class=\"icon-font icon-exit iconAdminMenu\"></span>");
        output.println("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=loginHistory\">Login/Logout Events</a>");
        output.println("</div>");

        output.println("<br/>");

        output.println("<form>");

        output.println("<input type=\"button\" value=\"Logout\" onclick=\"window.location.href='" + req.getContextPath() + "/servlet?command=logout'\">");

        output.println("</form>");

        output.println("</body>");
        output.println("</html>");
        output.flush();
    }
}
