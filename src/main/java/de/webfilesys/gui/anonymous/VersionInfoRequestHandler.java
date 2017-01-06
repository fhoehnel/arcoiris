package de.webfilesys.gui.anonymous;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import de.webfilesys.ArcoirisBlog;

/**
 * @author Frank Hoehnel
 */
public class VersionInfoRequestHandler {
    private PrintWriter output = null;

    private String contextRoot = null;

    public VersionInfoRequestHandler(HttpServletRequest req, PrintWriter output) {
        this.output = output;

        contextRoot = req.getContextPath();
    }

    public void handleRequest() {
        output.println("<html>");
        output.println("<head>");
        output.println("<title> arcoiris Blog Version Info </title>");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + contextRoot + "/styles/common.css\">");

        output.println("</head>");
        output.println("<body style=\"background-color:#e0e0e0\">");
        output.println("<table border=\"0\" width=\"100%\" cellpadding=\"14\">");
        output.println("<tr><td class=\"story\" style=\"text-align:center\"> arcoiris Blog</td></tr>");
        output.println("<tr><td class=\"value\" style=\"text-align:center\">" + ArcoirisBlog.VERSION + "</td></tr>");
        output.println("<tr><td style=\"text-align:center\"><a class=\"fn\" href=\"http://www.webfilesys.de/blog\" target=\"_blank\">www.webfilesys.de/blog</a></td></tr>");
        output.println("<tr><td style=\"text-align:center\"><form><input type=\"button\" value=\"Close\" onClick=\"self.close()\" /></form></td></tr>");
        output.println("</table>");
        output.println("</body></html>");
        output.flush();
    }
}
