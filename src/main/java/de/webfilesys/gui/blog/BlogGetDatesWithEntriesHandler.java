package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogGetDatesWithEntriesHandler extends XmlRequestHandlerBase {

    public BlogGetDatesWithEntriesHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);
        
        Element dateListElem = doc.createElement("datesWithEntries");

        doc.appendChild(dateListElem);
        
        MetaInfManager metaInfMgr = MetaInfManager.getInstance();
        
        boolean stagedPublication = metaInfMgr.isStagedPublication(currentPath);
        
        File blogDir = new File(currentPath);
        
        if (!blogDir.exists() || (!blogDir.isDirectory()) || (!blogDir.canRead())) {
            Logger.getLogger(getClass()).error("home directory of user " + uid + " is not a readable directory: " + currentPath);
            return;
        }

        HashMap<String, Boolean> dateMap = new HashMap<String, Boolean>();
        
        File[] filesInDir = blogDir.listFiles();

        for (int i = 0; i < filesInDir.length; i++) {
            if (filesInDir[i].isFile() && filesInDir[i].canRead()) {

                if (CommonUtils.isPictureFile(filesInDir[i])) {

                    if ((!readonly) || (!stagedPublication) || (metaInfMgr.getStatus(filesInDir[i].getAbsolutePath()) != MetaInfManager.STATUS_BLOG_EDIT)) {

                        String fileName = filesInDir[i].getName();
                        if (fileName.length() >= 10) {
                            String blogDate = fileName.substring(0, 10);

                            dateMap.put(blogDate, Boolean.TRUE);
                        }
                    }
                }
            }
        }
        
        for (String blogDate : dateMap.keySet()) {
            Element dateElem = doc.createElement("date");
            XmlUtil.setElementText(dateElem,  blogDate);
            dateListElem.appendChild(dateElem);
        }

        processResponse();
    }
}