package de.webfilesys.gui.admin;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.LanguageManager;
import de.webfilesys.Paging;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserComparator;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;

/**
 * Show a pageable list of users.
 * 
 * @author Frank Hoehnel
 */
public class UserListRequestHandler extends AdminRequestHandler {
    public static final String SESSION_KEY_USER_LIST_START_IDX = "userListStartIdx";
    public static final String SESSION_KEY_USER_LIST_PAGE_SIZE = "userListPageSize";
    public static final String SESSION_KEY_USER_LIST_SORT_FIELD = "userListSortField";
    public static final String SESSION_KEY_USER_LIST_FILTER = "userListFilter";

    public UserListRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        output.print("<html>");
        output.print("<head>");

        output.print("<title> arcoiris User Administration </title>");

        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/common.css\">");
        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/styles/admin.css\">");

        output.println("<script src=\"" + req.getContextPath() + "/javascript/util.js\" type=\"text/javascript\"></script>");
        output.println("<script src=\"" + req.getContextPath() + "/javascript/admin.js\" type=\"text/javascript\"></script>");

        output.println("</head>");
        output.println("<body>");

        headLine("arcoiris User Administration");

        output.println("<br/>");

        HttpSession session = req.getSession(true);

        String initial = req.getParameter("initial");

        int pageSize = Paging.DEFAULT_PAGE_SIZE;

        if (initial == null) {
            String pageSizeParm = getParameter(Paging.PARAM_PAGE_SIZE);

            if (pageSizeParm != null) {
                try {
                    pageSize = Integer.parseInt(pageSizeParm);
                    session.setAttribute(SESSION_KEY_USER_LIST_PAGE_SIZE, new Integer(pageSize));
                } catch (NumberFormatException nfex) {
                }
            } else {
                Integer userListPageSize = (Integer) session.getAttribute(SESSION_KEY_USER_LIST_PAGE_SIZE);

                if (userListPageSize != null) {
                    pageSize = userListPageSize.intValue();
                }
            }
        } else {
            session.removeAttribute(SESSION_KEY_USER_LIST_PAGE_SIZE);
        }

        int startIdx = 0;

        if (initial == null) {
            String startIdxParm = getParameter(Paging.PARAM_START_INDEX);

            if (startIdxParm != null) {
                try {
                    startIdx = Integer.parseInt(startIdxParm);
                    session.setAttribute(SESSION_KEY_USER_LIST_START_IDX, new Integer(startIdx));
                } catch (NumberFormatException nfex) {
                }
            } else {
                Integer userListStartIdx = (Integer) session.getAttribute(SESSION_KEY_USER_LIST_START_IDX);
                if (userListStartIdx != null) {
                    startIdx = userListStartIdx.intValue();
                }
            }
        } else {
            session.removeAttribute(SESSION_KEY_USER_LIST_START_IDX);
        }

        String searchMask = "";

        if (initial == null) {
            String filter = getParameter("searchMask");

            if (filter != null) {
                searchMask = filter;
                session.setAttribute(SESSION_KEY_USER_LIST_FILTER, filter);
            } else {
                String userListFilter = (String) session.getAttribute(SESSION_KEY_USER_LIST_FILTER);
                if (userListFilter != null) {
                    searchMask = userListFilter;
                }
            }
        } else {
            session.removeAttribute(SESSION_KEY_USER_LIST_FILTER);
        }

        int sortBy = UserComparator.SORT_BY_USERID;

        if (initial == null) {
            String sortParm = getParameter("sortField");

            if (sortParm != null) {
                try {
                    sortBy = Integer.parseInt(sortParm);
                    session.setAttribute(SESSION_KEY_USER_LIST_SORT_FIELD, new Integer(sortBy));
                } catch (NumberFormatException nfex) {
                }
            } else {
                Integer userListSortField = (Integer) session.getAttribute(SESSION_KEY_USER_LIST_SORT_FIELD);
                if (userListSortField != null) {
                    sortBy = userListSortField.intValue();
                }
            }
        } else {
            session.removeAttribute(SESSION_KEY_USER_LIST_SORT_FIELD);
        }

        output.println("<form accept-charset=\"utf-8\" method=\"get\" action=\"" + req.getContextPath() + "/servlet\">");

        output.println("<input type=\"hidden\" name=\"command\" value=\"admin\">");
        output.println("<input type=\"hidden\" name=\"cmd\" value=\"userList\">");
        output.println("<input type=\"hidden\" name=\"startIndex\" value=\"0\">");

        output.println("<table width=\"100%\" border=\"0\">");
        output.println("<tr>");
        output.println("<td width=\"70%\">&nbsp;</td>");

        output.println("<td class=\"plaintext\" align=\"right\" nowrap>");
        output.println("sort by");
        output.println("<select name=\"sortField\" size=\"1\" onchange=\"document.forms[0].submit()\">");
        output.print("<option value=\"" + UserComparator.SORT_BY_USERID + "\"");
        if (sortBy == UserComparator.SORT_BY_USERID) {
            output.print(" selected");
        }
        output.println(">userid</option>");

        output.print("<option value=\"" + UserComparator.SORT_BY_LAST_NAME + "\"");
        if (sortBy == UserComparator.SORT_BY_LAST_NAME) {
            output.print(" selected");
        }
        output.println(">last name</option>");

        output.print("<option value=\"" + UserComparator.SORT_BY_FIRST_NAME + "\"");
        if (sortBy == UserComparator.SORT_BY_FIRST_NAME) {
            output.print(" selected");
        }
        output.println(">first name</option>");

        output.println("<option value=\"" + UserComparator.SORT_BY_ROLE + "\"");
        if (sortBy == UserComparator.SORT_BY_ROLE) {
            output.print(" selected");
        }
        output.println(">role</option>");

        output.println("<option value=\"" + UserComparator.SORT_BY_EMAIL + "\"");
        if (sortBy == UserComparator.SORT_BY_EMAIL) {
            output.print(" selected");
        }
        output.println(">e-mail</option>");

        output.println("<option value=\"" + UserComparator.SORT_BY_LAST_LOGIN + "\"");
        if (sortBy == UserComparator.SORT_BY_LAST_LOGIN) {
            output.print(" selected");
        }
        output.println(">last login</option>");

        output.println("<option value=\"" + UserComparator.SORT_BY_LAYOUT + "\"");
        if (sortBy == UserComparator.SORT_BY_LAYOUT) {
            output.print(" selected");
        }
        output.println(">layout</option>");

        output.println("<option value=\"" + UserComparator.SORT_BY_ACTIVATED + "\"");
        if (sortBy == UserComparator.SORT_BY_ACTIVATED) {
            output.print(" selected");
        }
        output.println(">activated</option>");

        output.println(">last login</option>");
        output.println("</select>");
        output.println("</td>");

        output.println("<td>&nbsp;&nbsp;</td>");

        output.println("<td class=\"plaintext\" align=\"right\" nowrap>");
        output.println("filter: ");
        output.println("<input type=\"text\" name=\"searchMask\" size=\"10\" maxlength=\"32\" value=\"" + searchMask + "\" style=\"width:80px\">");
        output.println("</td>");

        output.println("<td>&nbsp;&nbsp;</td>");

        output.println("<td class=\"plaintext\" align=\"right\" nowrap>");
        output.println("elements per page: ");
        output.println("<input type=\"text\" name=\"" + Paging.PARAM_PAGE_SIZE + "\" maxlength=\"3\" maxlength=\"3\" value=\"" + pageSize + "\" style=\"width:40px\">");
        output.println("<input type=\"submit\" value=\"Refresh\">");
        output.println("</td></tr></table>");
        output.println("</form>");

        output.println("<table width=\"100%\" border=\"1\" cellspacing=\"0\">");
        output.println("<tr bgcolor=lavender>");
        output.println("<th class=\"datahead\">&nbsp;</th>");
        output.println("<th class=\"datahead\">userid/login</th>");
        output.println("<th class=\"datahead\">activated</th>");
        output.println("<th class=\"datahead\">role</th>");
        output.println("<th class=\"datahead\">language</th>");
        output.println("<th class=\"datahead\">last name, first name</th>");
        output.println("<th class=\"datahead\">e-mail</th>");
        output.println("<th class=\"datahead\">layout</th>");
        output.println("<th class=\"datahead\">disk quota</th>");
        output.println("<th class=\"datahead\">last login</th>");
        output.println("</tr>");

        SimpleDateFormat dateFormat = LanguageManager.getInstance().getDateFormat("admin");

        ArrayList<TransientUser> allUsers = userMgr.getRealUsers();

        if (allUsers.size() > 1) {
            Collections.sort(allUsers, new UserComparator(sortBy));
        }

        if ((searchMask != null) && (searchMask.trim().length() > 0)) {
            for (int i = allUsers.size() - 1; i >= 0; i--) {
                TransientUser user = (TransientUser) allUsers.get(i);

                if ((!CommonUtils.containsString(user.getUserid(), searchMask)) && (!CommonUtils.containsString(user.getLastName(), searchMask))
                                && (!CommonUtils.containsString(user.getFirstName(), searchMask)) && (!CommonUtils.containsString(user.getEmail(), searchMask))) {
                    allUsers.remove(i);
                }
            }

            // allUserNames=filterUsers(allUserNames,searchMask);
        }

        Paging paging = new Paging(allUsers, pageSize, startIdx);

        ArrayList<Object> usersOnPage = paging.getElementList();

        for (int i = 0; i < usersOnPage.size(); i++) {
            TransientUser actUser = (TransientUser) usersOnPage.get(i);

            output.println("<tr>");

            output.print("<td class=\"tableData\" align=\"left\"  nowrap>");

            output.print("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=editUser&username=" + UTF8URLEncoder.encode(actUser.getUserid())
                            + "\"><img src=\"images/edit2.gif\" alt=\"Edit User\" border=0></a>");

            if (!actUser.getRole().equals("admin")) {
                output.print("<a href=\"javascript:confirmDelete('" + actUser.getUserid() + "', '" + actUser.getDocumentRoot()
                                + "')\"><img src=\"images/trash.gif\" alt=\"Delete User\" border=\"0\"></a>");

                if (actUser.getDiskQuota() > 0) {
                    output.print("<a href=\"javascript:diskQuota('" + actUser.getUserid() + "')\"><img src=\"images/barGraph.gif\" alt=\"Disk Quota Usage\" border=0></a>");
                }
            }

            output.println("</td>");

            output.println("<td class=\"tableData\" align=\"left\" >" + actUser.getUserid() + " </td>");

            output.println("<td class=\"tableData\" align=\"left\" >" + actUser.isActivated() + " </td>");

            String role = actUser.getRole();

            if ((role == null) || (role.trim().length() == 0)) {
                role = "&nbsp;";
            }

            output.println("<td class=\"tableData\" > " + role + "</td>");

            String userLanguage = actUser.getLanguage();

            if (userLanguage == null) {
                userLanguage = LanguageManager.DEFAULT_LANGUAGE;
            }

            output.println("<td class=\"tableData\" > " + userLanguage + "</td>");

            String lastName = actUser.getLastName();
            String firstName = actUser.getFirstName();

            StringBuffer fullName = new StringBuffer();
            if ((lastName != null) && (lastName.trim().length() > 0)) {
                fullName.append(lastName);
                if ((firstName != null) && (firstName.trim().length() > 0)) {
                    fullName.append(", ");
                }
            }

            if ((firstName != null) && (firstName.trim().length() > 0)) {
                fullName.append(firstName);
            }

            if (fullName.length() == 0) {
                fullName.append("&nbsp;");
            }

            output.println("<td class=\"tableData\" > " + fullName.toString() + "</td>");

            String email = actUser.getEmail();

            if ((email == null) || (email.trim().length() == 0)) {
                output.println("<td class=\"tableData\">&nbsp;</td>");
            } else {
                int atSignIdx = email.indexOf('@');
                String formattedEmail = email.substring(0, atSignIdx + 1) + " " + email.substring(atSignIdx + 1);
                output.println("<td class=\"tableData\"><a class=\"fn\" href=\"mailto:" + email + "\">" + formattedEmail + "</a></td>");
            }

            String layout = actUser.getCss();

            if (CommonUtils.isEmpty(layout)) {
                layout = "&nbsp;";
            }

            output.println("<td class=\"tableData\" > " + layout + "</td>");

            long diskQuota = actUser.getDiskQuota();
            String diskQuotaText = "&nbsp;";
            if (diskQuota > 0) {
                diskQuotaText = (diskQuota / 1024l / 1024l) + " MB";
            }

            output.println("<td class=\"tableData\" style=\"text-align:right\" > " + diskQuotaText + "</td>");

            Date lastLogin = actUser.getLastLogin();

            output.println("<td class=\"tableData\">");

            if (lastLogin == null) {
                output.println("&nbsp;");
            } else {
                output.println(dateFormat.format(lastLogin));
            }
            output.println("</td>");

            output.println("</tr>");
        }

        output.println("</table><br>");

        if (paging.getElementNumber() > 0) {
            output.println("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");

            output.println("<tr>");
            output.println("<td class=\"dir\" align=\"left\" valign=\"middle\" nowrap=\"true\">");
            output.println("&nbsp;");

            if (paging.isFirstPage()) {
                output.println("<img src=\"images/firstDisabled.gif\" border=\"0\">");
                output.println("<img src=\"images/previousDisabled.gif\" border=\"0\">");
            } else {
                output.print("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=userList&startIndex=0\">");
                output.println("<img src=\"images/first.gif\" border=\"0\"></a>");

                output.println("&nbsp;");

                output.print("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=userList&startIndex=" + paging.getPrevStartIndex() + "\">");
                output.println("<img src=\"images/previous.gif\" border=\"0\"></a>");
            }

            output.println("</td>");

            output.println("<td class=\"plaintext\" align=\"center\" valign=\"middle\">");
            output.println("elements ");
            output.println(paging.getStartIndex() + " - " + paging.getEndIndex() + " of " + paging.getElementNumber());

            output.println("&nbsp;&nbsp;&nbsp;page&nbsp;");

            // if there are more than 30 pages we show not all pages in the list
            int pageStep = paging.getElementNumber() / pageSize / 30 + 1;

            ArrayList<Integer> startIndices = paging.getStartIndices();

            int pageCounter = 1;

            for (int k = 0; k < startIndices.size(); k++) {
                int idx = startIndices.get(k).intValue();

                if (idx != paging.getStartIndex() - 1) {
                    if (((pageCounter - 1) % pageStep == 0) || (k == startIndices.size() - 1)) {
                        output.print("<a class=\"fn\" href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=userList&startIndex=" + idx + "\">");
                        output.print(pageCounter);
                        output.print("</a>");
                    }
                } else {
                    output.print(pageCounter);
                }

                output.println("&nbsp;");

                pageCounter++;
            }

            output.println("</td>");

            output.println("<td class=\"dir\" align=\"right\" valign=\"middle\" nowrap=\"true\">");

            if (!paging.isLastPage()) {
                output.print("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=userList&startIndex=" + paging.getNextStartIndex() + "\">");
                output.println("<img src=\"images/next.gif\" border=\"0\"></a>");

                output.println("&nbsp;");

                output.print("<a href=\"" + req.getContextPath() + "/servlet?command=admin&cmd=userList&startIndex=" + paging.getLastPageStartIndex() + "\">");
                output.println("<img src=\"images/last.gif\" border=\"0\"></a>");
            } else {
                output.println("<img src=\"images/nextDisabled.gif\" border=\"0\">");
                output.println("<img src=\"images/lastDisabled.gif\" border=\"0\">");
            }

            output.println("&nbsp;");
            output.println("</td>");
            output.println("</tr>");
            output.println("</table>");
        } else {
            output.println("no entries");
        }

        output.println("<form accept-charset=\"utf-8\" style=\"margin-top:20px;\">");

        output.println("<input type=\"button\" value=\"Add new user\" onclick=\"window.location.href='" + req.getContextPath() + "/servlet?command=admin&cmd=registerUser'\">");

        output.println("&nbsp;&nbsp;&nbsp;");

        output.println("<input type=\"button\" value=\"Return\" onclick=\"window.location.href='" + req.getContextPath() + "/servlet?command=admin&cmd=menu'\">");

        output.println("</form>");

        output.println("</body></html>");
        output.flush();
    }

}
