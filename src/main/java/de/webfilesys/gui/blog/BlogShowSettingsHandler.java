package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.LanguageManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.CSSManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogShowSettingsHandler extends XmlRequestHandlerBase {
    public BlogShowSettingsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!this.checkWriteAccess()) {
            return;
        }

        Element settingsElement = doc.createElement("settings");

        doc.appendChild(settingsElement);

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        String blogTitle = metaInfMgr.getDescription(currentPath, ".");

        XmlUtil.setChildText(settingsElement, "blogTitleText", blogTitle, true);

        int daysPerPage = BlogListHandler.DEFAULT_PAGE_SIZE;

        int pageSize = userMgr.getPageSize(uid);

        if (pageSize >= 1) {
            daysPerPage = pageSize;
        }

        XmlUtil.setChildText(settingsElement, "daysPerPage", Integer.toString(daysPerPage), false);

        boolean stagedPublication = metaInfMgr.isStagedPublication(currentPath);

        if (stagedPublication) {
            XmlUtil.setChildText(settingsElement, "stagedPublication", "true", false);
        }

        boolean notifyOnNewComment = metaInfMgr.isNotifyOnNewComment(currentPath);

        if (notifyOnNewComment) {
            XmlUtil.setChildText(settingsElement, "notifyOnNewComment", "true", false);
        }
        
        int sortOrder = metaInfMgr.getSortOrder(currentPath);
        if (sortOrder == 0) {
            sortOrder = BlogDateComparator.SORT_ORDER_BLOG;
        }
        XmlUtil.setChildText(settingsElement, "sortOrder", Integer.toString(sortOrder), false);
        
        Element skinsElement = doc.createElement("skins");
        settingsElement.appendChild(skinsElement);

        ArrayList<String> skins = CSSManager.getInstance().getAvailableCss();

        for (String skin : skins) {
            Element skinElem = doc.createElement("skin");
            XmlUtil.setElementText(skinElem, skin);
            skinsElement.appendChild(skinElem);
        }

        XmlUtil.setChildText(settingsElement, "activeSkin", userMgr.getCSS(uid), false);

        LanguageManager langMgr = LanguageManager.getInstance();

        Element languagesElement = doc.createElement("languages");

        settingsElement.appendChild(languagesElement);
        
        ArrayList<String> languageList = langMgr.getAvailableLanguages();

        for (String availableLanguage : languageList) {
        
            Element languageElement = doc.createElement("language");

            XmlUtil.setElementText(languageElement, availableLanguage);

            if (availableLanguage.equals(language)) {
                languageElement.setAttribute("selected", "true");
            }

            languagesElement.appendChild(languageElement);
        }
        
        processResponse();
    }
}