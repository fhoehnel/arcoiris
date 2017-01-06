package de.webfilesys.gui.admin;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.ArcoirisBlog;
import de.webfilesys.mail.EmailUtils;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserMgmtException;
import de.webfilesys.util.CommonUtils;

/**
 * @author Frank Hoehnel
 */
public class AdminAddUserRequestHandler extends AdminRequestHandler {
    public AdminAddUserRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        ArrayList<String> errorMsgs = new ArrayList<String>();

        String login = getParameter("username");

        if ((login == null) || (login.trim().length() < 3)) {
            errorMsgs.add("the minimum length of the login name is 3 characters");
        } else {
            login = login.trim();
            if (!CommonUtils.validateUserid(login)) {
                errorMsgs.add("The login name contains illegal characters. Allowed characters are: a-z, A-Z, 0-9, dash and dot.");
            }
        }

        String password = getParameter("password");
        String pwconfirm = getParameter("pwconfirm");

        if ((password == null) || (pwconfirm == null)) {
            errorMsgs.add("Password and password confirmation are required fields.");
        } else {
            password = password.trim();
            pwconfirm = pwconfirm.trim();

            if (password.trim().length() < 5) {
                errorMsgs.add("The minimum password length is 5 characters.");
            } else {
                if (password.indexOf(' ') > 0) {
                    errorMsgs.add("The password must not contain spaces.");
                } else {
                    if (!pwconfirm.equals(password)) {
                        errorMsgs.add("The password and the password confirmation do not match.");
                    }
                }
            }
        }

        String email = getParameter("email");

        if (email == null) {
            errorMsgs.add("e-mail is a required field.");
        } else {
            email = email.trim();
            if (!EmailUtils.emailSyntaxOk(email)) {
                errorMsgs.add("A valid e-mail address is required.");
            }
        }

        String userLanguage = getParameter("language");

        if ((userLanguage == null) || userLanguage.length() == 0) {
            errorMsgs.add("Language is mandatory.");
        }

        int diskQuotaMB = (-1);

        String checkDiskQuota = getParameter("checkDiskQuota");

        String diskQuotaString = getParameter("diskQuota");

        if (checkDiskQuota != null) {
            if (diskQuotaString == null) {
                errorMsgs.add("Enter a disk quota value or uncheck the checkbox.");
            } else {
                try {
                    diskQuotaMB = Integer.parseInt(diskQuotaString);
                } catch (NumberFormatException nfex) {
                    errorMsgs.add("The disk quota value is invalid.");
                }
            }
        } else {
            if (!CommonUtils.isEmpty(diskQuotaString)) {
                errorMsgs.add("Check the checkbox to set a disk quota.");
            }
        }

        if (errorMsgs.size() > 0) {
            (new AdminRegisterUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();
            return;
        }

        if (userMgr.userExists(login)) {
            errorMsgs.add("A user with userid " + login + " already exists.");

            (new AdminRegisterUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();

            return;
        }

        String homeDir = ArcoirisBlog.getInstance().getUserDocRoot();
        if (homeDir.endsWith(File.separator)) {
            homeDir = homeDir + login;
        } else {
            homeDir = homeDir + File.separator + login;
        }

        File docRootDir = new File(homeDir);
        if (!docRootDir.mkdirs()) {
            Logger.getLogger(getClass()).error("Failed to create home directory " + homeDir + " for new user " + login);

            errorMsgs.add("Failed to create home directory " + homeDir + ". ");
            (new AdminRegisterUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();
            return;
        }

        TransientUser newUser = new TransientUser();

        newUser.setUserid(login);
        newUser.setPassword(password);
        newUser.setDocumentRoot(homeDir);
        newUser.setEmail(email);
        newUser.setRole(getParameter("role"));
        newUser.setFirstName(getParameter("firstName"));
        newUser.setLastName(getParameter("lastName"));
        newUser.setPhone(getParameter("phone"));
        newUser.setCss(getParameter("css"));
        newUser.setLanguage(userLanguage);

        if (checkDiskQuota != null) {
            long diskQuota = ((long) diskQuotaMB) * (1024l) * (1024l);
            newUser.setDiskQuota(diskQuota);
        }

        try {
            userMgr.createUser(newUser);
        } catch (UserMgmtException ex) {
            Logger.getLogger(getClass()).warn("failed to create new user " + newUser.getUserid(), ex);
            errorMsgs.add("Failed to create new user " + newUser.getUserid() + ". ");
            (new AdminRegisterUserRequestHandler(req, resp, session, output, uid, errorMsgs)).handleRequest();
            return;
        }

        String sendWelcomeMail = getParameter("sendWelcomeMail");

        if ((ArcoirisBlog.getInstance().getMailHost() != null) && (sendWelcomeMail != null)) {
            EmailUtils.sendWelcomeMail(email, newUser.getFirstName(), newUser.getLastName(), login, password, null, userLanguage);
        }

        (new UserListRequestHandler(req, resp, session, output, uid)).handleRequest();
    }

}
