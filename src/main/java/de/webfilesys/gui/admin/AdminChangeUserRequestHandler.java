package de.webfilesys.gui.admin;

import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.mail.EmailUtils;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserMgmtException;
import de.webfilesys.util.CommonUtils;

/**
 * @author Frank Hoehnel
 */
public class AdminChangeUserRequestHandler extends AdminRequestHandler {
    public AdminChangeUserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        ArrayList<String> errorMsgs = new ArrayList<String>();

        String login = getParameter("username");

        String password = getParameter("password");
        String pwconfirm = getParameter("pwconfirm");

        if ((password != null) && (password.trim().length() > 0) || (pwconfirm != null) && (pwconfirm.trim().length() > 0)) {
            if ((password == null) || (password.trim().length() < 5)) {
                errorMsgs.add("the minimum password length is 5 characters");
            } else {
                if (password.indexOf(' ') > 0) {
                    errorMsgs.add("the password must not contain spaces");
                } else {
                    if ((pwconfirm == null) || (!pwconfirm.equals(password))) {
                        errorMsgs.add("the password and the password confirmation are not equal");
                    }
                }
            }
        }

        String email = getParameter("email");

        if (email == null) {
            errorMsgs.add("e-mail address is required");
        } else {
            email = email.trim();
            if (!EmailUtils.emailSyntaxOk(email)) {
                errorMsgs.add("the e-mail address is invalid");
            }
        }

        String activatedParam = getParameter("activated");
        boolean activated = (activatedParam != null);

        int diskQuotaMB = (-1);

        String checkDiskQuota = getParameter("checkDiskQuota");

        String diskQuotaString = getParameter("diskQuota");

        if (checkDiskQuota != null) {
            if (CommonUtils.isEmpty(diskQuotaString)) {
                errorMsgs.add("enter a disk quota value or uncheck the checkbox");
            } else {
                try {
                    diskQuotaMB = Integer.parseInt(diskQuotaString);
                } catch (NumberFormatException nfex) {
                    errorMsgs.add("the disk quota value is invalid");
                }
            }
        } else {
            if ((diskQuotaString != null) && (diskQuotaString.trim().length() > 0)) {
                errorMsgs.add("check the checkbox to set a disk quota");
            }
        }

        if (errorMsgs.size() > 0) {
            (new AdminEditUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();

            return;
        }

        TransientUser changedUser = userMgr.getUser(login);

        if (changedUser == null) {
            Logger.getLogger(getClass()).error("user for update not found: " + login);
            errorMsgs.add("user for update not found: " + login);
            (new AdminEditUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();
            return;
        }

        if (!CommonUtils.isEmpty(password)) {
            changedUser.setPassword(password);
        }

        changedUser.setEmail(email);

        String role = getParameter("role");

        if (!CommonUtils.isEmpty(role)) {
            changedUser.setRole(role);
        }

        String firstName = getParameter("firstName");

        if (!CommonUtils.isEmpty(firstName)) {
            changedUser.setFirstName(firstName);
        }

        String lastName = getParameter("lastName");

        if (!CommonUtils.isEmpty(lastName)) {
            changedUser.setLastName(lastName);
        }

        String phone = getParameter("phone");

        if (!CommonUtils.isEmpty(phone)) {
            changedUser.setPhone(phone);
        }

        String css = getParameter("css");

        if (!CommonUtils.isEmpty(css)) {
            changedUser.setCss(css);
        }

        changedUser.setActivated(activated);

        if (checkDiskQuota != null) {
            long diskQuota = ((long) diskQuotaMB) * (1024l) * (1024l);
            changedUser.setDiskQuota(diskQuota);
        } else {
            changedUser.setDiskQuota(-1);
        }

        String userLanguage = getParameter("language");

        if (userLanguage != null) {
            changedUser.setLanguage(userLanguage);
        }

        try {
            userMgr.updateUser(changedUser);
        } catch (UserMgmtException ex) {
            Logger.getLogger(getClass()).error("failed to update user " + login, ex);
            errorMsgs.add("failed to update user " + login);
            (new AdminEditUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();
            return;
        }

        (new UserListRequestHandler(req, resp, session, output, uid)).handleRequest();
    }

}
