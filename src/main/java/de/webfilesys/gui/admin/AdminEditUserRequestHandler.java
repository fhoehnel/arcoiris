package de.webfilesys.gui.admin;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.LanguageManager;
import de.webfilesys.gui.CSSManager;
import de.webfilesys.user.TransientUser;

/**
 * Administrator edits the account of an user.
 * 
 * @author Frank Hoehnel
 */
public class AdminEditUserRequestHandler extends AdminRequestHandler {
    ArrayList<String> errorMsgs = null;

    public AdminEditUserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid, ArrayList<String> errorMsgs) {
        super(req, resp, session, output, uid);

        if (errorMsgs == null) {
            this.errorMsgs = new ArrayList<String>();
        } else {
            this.errorMsgs = errorMsgs;
        }
    }

    protected void process() {
        String login = getParameter("username");

        TransientUser user = userMgr.getUser(login);
        if (user == null) {
            Logger.getLogger(getClass()).error("user not found: " + login);
            return;
        }

        output.print("<html>");
        output.print("<head>");

        output.print("<title> arcoiris Blog Administration: Edit User </title>");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/common.css\">");
        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/admin.css\">");

        output.println("<script src=\"" + req.getContextPath() + "/javascript/admin.js\" type=\"text/javascript\"></script>");
        output.println("<script src=\"" + req.getContextPath() + "/javascript/util.js\" type=\"text/javascript\"></script>");

        output.print("</head>");
        output.print("<body>");

        headLine("arcoiris Blog Administration: Edit User " + login);

        output.println("<form id=\"userForm\" accept-charset=\"utf-8\" method=\"post\" action=\"" + req.getContextPath() + "/servlet\">");
        output.println("<input type=\"hidden\" name=\"command\" value=\"admin\">");
        output.println("<input type=\"hidden\" name=\"cmd\" value=\"changeUser\">");
        output.println("<input type=\"hidden\" name=\"username\" value=\"" + login + "\">");

        output.println("<div id=\"validationErrorCont\">");
        output.println("<ul id=\"validationErrorList\"></ul>");
        output.println("</div>");

        output.println("<table class=\"dataForm adminForm\" width=\"100%\">");

        output.println("<tr>");
        output.println("<td class=\"formParm1\"><b>userid / login</b></td>");
        output.println("<td class=\"formParm2\">" + login + "</td>");
        output.println("</tr>");

        output.println("<tr>");
        output.println("<td class=\"formParm1\">new password</td>");
        output.println("<td class=\"formParm2\"><input type=\"password\" id=\"password\" name=\"password\" maxlength=\"30\" value=\"\"></td>");
        output.println("</tr>");

        output.println("<tr>");
        output.println("<td class=\"formParm1\">password confirmation</td>");
        output.println("<td class=\"formParm2\"><input type=\"password\" id=\"pwconfirm\" name=\"pwconfirm\" maxlength=\"30\" value=\"\"></td>");
        output.println("</tr>");

        String firstName = null;

        if (errorMsgs.size() > 0) {
            firstName = req.getParameter("firstName");
        } else {
            firstName = user.getFirstName();
        }

        if (firstName == null) {
            firstName = "";
        }

        output.println("<tr>");
        output.println("<td class=\"formParm1\">first name</td>");
        output.println("<td class=\"formParm2\"><input type=\"text\" id=\"firstName\" name=\"firstName\" maxlength=\"120\" value=\"" + firstName + "\"></td>");
        output.println("</tr>");

        String lastName = null;

        if (errorMsgs.size() > 0) {
            lastName = req.getParameter("lastName");
        } else {
            lastName = user.getLastName();
        }

        if (lastName == null) {
            lastName = "";
        }

        output.println("<tr>");
        output.println("<td class=\"formParm1\">last name</td>");
        output.println("<td class=\"formParm2\"><input type=\"text\" id=\"lastName\" name=\"lastName\" maxlength=\"120\" value=\"" + lastName + "\"></td>");
        output.println("</tr>");

        String val = null;

        if (errorMsgs.size() > 0) {
            val = req.getParameter("email");
        } else {
            val = user.getEmail();
        }

        if (val == null) {
            val = "";
        }
        output.println("<tr>");
        output.println("<td class=\"formParm1\"><b>e-mail address</b></td>");
        output.println("<td class=\"formParm2\"><input type=\"text\" id=\"email\" name=\"email\" maxlength=\"120\" value=\"" + val + "\"></td>");
        output.println("</tr>");

        if (errorMsgs.size() > 0) {
            val = req.getParameter("phone");
        } else {
            val = user.getPhone();
        }

        if (val == null) {
            val = "";
        }
        output.println("<tr>");
        output.println("<td class=\"formParm1\">phone</td>");
        output.println("<td class=\"formParm2\"><input type=\"text\" id=\"phone\" name=\"phone\" maxlength=\"30\" value=\"" + val + "\"></td>");
        output.println("</tr>");

        long diskQuota = user.getDiskQuota();

        boolean checkDiskQuota = false;

        if (errorMsgs.size() > 0) {
            checkDiskQuota = (req.getParameter("checkDiskQuota") != null);
        } else {
            checkDiskQuota = (diskQuota > 0l);
        }

        output.println("<tr>");
        output.println("<td class=\"formParm1\">");
        output.println("check disk quota");
        output.println("</td>");
        output.println("<td class=\"formParm2\">");
        output.print("<input type=\"checkbox\" id=\"checkDiskQuota\" name=\"checkDiskQuota\" class=\"cb3\" onclick=\"switchDiskQuota(this)\"");
        if (checkDiskQuota) {
            output.print(" checked");
        }
        output.println(">");
        output.println("</td>");
        output.println("</tr>");

        val = null;

        if (errorMsgs.size() > 0) {
            val = getParameter("diskQuota");
        } else {
            if (diskQuota > 0l) {
                val = "" + (diskQuota / (1024l * 1024l));
            }
        }

        if (val == null) {
            val = "";
        }

        output.println("<tr>");
        output.println("<td class=\"formParm1\">disk quota (MBytes)</td>");
        output.println("<td class=\"formParm2\"><input type=\"text\" id=\"diskQuota\" name=\"diskQuota\" maxlength=\"12\" value=\"" + val + "\"");
        if (!checkDiskQuota) {
            output.print(" disabled=\"disabled\"");
        }
        output.println(">");
        output.println("</td>");
        output.println("</tr>");

        boolean activated = false;

        if (errorMsgs.size() > 0) {
            activated = (req.getParameter("activated") != null);
        } else {
            activated = user.isActivated();
        }

        output.println("<tr>");
        output.println("<td class=\"formParm1\">");
        output.println("activated");
        output.println("</td>");
        output.println("<td class=\"formParm2\">");
        output.print("<input type=\"checkbox\" id=\"activated\" name=\"activated\" class=\"cb3\"");
        if (activated) {
            output.print(" checked");
        }
        output.println(">");
        output.println("</td>");
        output.println("</tr>");

        output.println("<tr>");
        output.println("<td class=\"formParm1\"><b>role</b></td>");
        output.println("<td class=\"formParm2\"><select id=\"role\" name=\"role\" size=\"1\">");

        String role = null;

        if (errorMsgs.size() > 0) {
            role = getParameter("role");
        } else {
            role = user.getRole();
        }

        if ((role != null) && role.equals("blog")) {
            output.println("<option selected>blog</option>");
        } else {
            output.println("<option>blog</option>");
        }

        if ((role != null) && role.equals("admin")) {
            output.println("<option selected>admin</option>");
        } else {
            output.println("<option>admin</option>");
        }

        output.println("</select></td>");
        output.println("</tr>");

        String userLanguage = null;

        if (errorMsgs.size() > 0) {
            userLanguage = getParameter("language");
        } else {
            userLanguage = user.getLanguage();
        }

        if (userLanguage == null) {
            userLanguage = LanguageManager.DEFAULT_LANGUAGE;
        }

        ArrayList<String> languages = LanguageManager.getInstance().getAvailableLanguages();

        output.println("<tr>");
        output.println("<td class=\"formParm1\"><b>language</b></td>");
        output.println("<td class=\"formParm2\"><select id=\"language\" name=\"language\" size=\"1\">");

        for (String lang : languages) {
            output.print("<option");

            if (lang.equals(userLanguage)) {
                output.print(" selected=\"selected\"");
            }

            output.println(">" + lang + "</option>");
        }
        output.println("</select></td>");
        output.println("</tr>");

        String userCss = null;

        if (errorMsgs.size() > 0) {
            userCss = getParameter("css");
        } else {
            userCss = user.getCss();
        }

        if (userCss == null) {
            userCss = CSSManager.DEFAULT_LAYOUT;
        }

        ArrayList<String> cssList = CSSManager.getInstance().getAvailableCss();

        output.println("<tr>");
        output.println("<td class=\"formParm1\"><b>layout (CSS file)</b></td>");
        output.println("<td class=\"formParm2\"><select id=\"css\" name=\"css\" size=\"1\">");

        for (String css : cssList) {
            output.print("<option");

            if (css.equals(userCss)) {
                output.print(" selected=\"selected\"");
            }

            output.println(">" + css + "</option>");
        }
        output.println("</select></td>");
        output.println("</tr>");

        output.println("<tr><td colspan=\"2\">&nbsp;</td></tr>");
        output.println("<tr><td class=\"formButton\">");
        output.println("<input type=\"button\" name=\"changebutton\" value=\"&nbsp;Save&nbsp;\" onclick=\"validateUser(true);\">");
        output.println("</td><td class=\"formButton\" align=\"right\">");
        output.println("<input type=\"button\" value=\"Cancel\" onclick=\"javascript:window.location.href='" + req.getContextPath() + "/servlet?command=admin&cmd=userList'\">");
        output.println("</td></tr>");

        output.println("</table>");

        output.println("</form>");

        output.println("</body>");

        if (errorMsgs.size() > 0) {
            output.println("<script language=\"javascript\">");

            for (String errorMsg : errorMsgs) {
                output.println("addValidationError(null, '" + errorMsg + "');");
            }
            output.println("</script>");
        }

        output.println("</html>");
        output.flush();
    }

}
