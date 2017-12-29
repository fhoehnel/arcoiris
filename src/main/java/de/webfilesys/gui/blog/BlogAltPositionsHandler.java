package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.util.XmlUtil;

public class BlogAltPositionsHandler extends BlogMoveHandlerBase {

    public BlogAltPositionsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String fileToMove = getParameter("fileName");
        
        Element blogElem = doc.createElement("blog");
        doc.appendChild(blogElem);

        XmlUtil.setChildText(blogElem, "fileName", fileToMove);
        
        Element positionsElem = doc.createElement("positions");
        blogElem.appendChild(positionsElem);

        int posCounter = 0;
        
        ArrayList<File> filesOfDay = getAllFilesOfDaySorted(getCwd(), fileToMove);
        
        for (File fileOfDay : filesOfDay) {
            posCounter++;

            Element posElem = doc.createElement("pos");
            XmlUtil.setElementText(posElem, Integer.toString(posCounter));
            positionsElem.appendChild(posElem);

            if (fileOfDay.getName().equals(fileToMove)) {
                posElem.setAttribute("disabled", "true");
                
                if (posCounter == 1) {
                    XmlUtil.setChildText(blogElem, "isTop", "true");
                } else if (posCounter == filesOfDay.size()) {
                    XmlUtil.setChildText(blogElem, "isBottom", "true");
                }
            }
        }

        processResponse();
    }

}
