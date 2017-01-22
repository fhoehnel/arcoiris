package de.webfilesys.gui.admin;

import java.io.PrintWriter;
import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.FileSysStat;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class UserDiskQuotaHandler extends XmlRequestHandlerBase {
    
    Logger LOG = Logger.getLogger(UserDiskQuotaHandler.class);
    
    public UserDiskQuotaHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!isAdminUser(true)) {
            LOG.warn("admin command requested by non-admin user: " + uid);
            return;
        }
        
        String userName = getParameter("userName");
        
        if (CommonUtils.isEmpty(userName)) {
            LOG.warn("missing parameter username");
            return;
        }
        
        Element diskQuotaElement = doc.createElement("userDiskQuota");

        doc.appendChild(diskQuotaElement);
        
        long userDiskQuota = userMgr.getDiskQuota(userName);

        if (userDiskQuota <= 0) {
            LOG.warn("disk quota not defined for user " + userName);
            return;
        }

        DecimalFormat numFormat = new DecimalFormat("#,###,###,###,###");

        String docRoot = userMgr.getDocumentRoot(userName);

        FileSysStat fileSysStat = new FileSysStat(docRoot);

        fileSysStat.getStatistics();

        XmlUtil.setChildText(diskQuotaElement, "userid", userName, false);
        
        XmlUtil.setChildText(diskQuotaElement, "diskQuota", Long.toString(userDiskQuota), false);
        XmlUtil.setChildText(diskQuotaElement, "diskQuotaFormatted", numFormat.format(userDiskQuota / 1024), false);
        
        XmlUtil.setChildText(diskQuotaElement, "usedSpace", Long.toString(fileSysStat.getTotalSizeSum()), false);
        XmlUtil.setChildText(diskQuotaElement, "usedSpaceFormatted", numFormat.format(fileSysStat.getTotalSizeSum() / 1024), false);
        
        long usagePercent = (fileSysStat.getTotalSizeSum() * 100L / userDiskQuota);
        
        XmlUtil.setChildText(diskQuotaElement, "usagePercent", Long.toString(usagePercent), false);
        
        long progressBarWidth;
        
        if (fileSysStat.getTotalSizeSum() <= userDiskQuota) {
            progressBarWidth = fileSysStat.getTotalSizeSum() * 300l / userDiskQuota;
        } else {
            progressBarWidth = userDiskQuota * 300l / fileSysStat.getTotalSizeSum();
            XmlUtil.setChildText(diskQuotaElement, "overLimit", "true");
        }

        XmlUtil.setChildText(diskQuotaElement, "progressBarWidth", Long.toString(progressBarWidth), false);
        
        processResponse();
    }
}