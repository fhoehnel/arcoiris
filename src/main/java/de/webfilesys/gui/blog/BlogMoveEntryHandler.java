package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * Move a blog entry up or down within a day.
 */
public class BlogMoveEntryHandler extends BlogMoveHandlerBase {
    public BlogMoveEntryHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        String fileToMove = getParameter("fileName");

        if (CommonUtils.isEmpty(fileToMove)) {
            Logger.getLogger(getClass()).error("missing parameter fileName");
            return;
        }

        String direction = getParameter("direction");
        if (CommonUtils.isEmpty(direction)) {
            Logger.getLogger(getClass()).error("missing parameter direction");
            return;
        }

        if ((!direction.equals("up")) && (!direction.equals("down"))) {
            Logger.getLogger(getClass()).error("invalid parameter direction");
            return;
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        boolean moveSuccess = false;

        ArrayList<File> filesOfDay = getAllFilesOfDaySorted(currentPath, fileToMove);

        if (direction.equals("up")) {
            File prevFile = null;

            Iterator<File> iter = filesOfDay.iterator();

            boolean found = false;

            while ((!found) && iter.hasNext()) {
                File fileOfDay = iter.next();
                if (fileOfDay.getName().equals(fileToMove)) {
                    found = true;
                } else {
                    prevFile = fileOfDay;
                }
            }

            if (found && (prevFile != null)) {
                moveSuccess = swapFileNamesAndRandomize(currentPath, prevFile.getName(), fileToMove);
            }
        } else if (direction.equals("down")) {
            File nextFile = null;

            Iterator<File> iter = filesOfDay.iterator();

            boolean found = false;

            while ((!found) && iter.hasNext()) {
                File fileOfDay = iter.next();
                if (fileOfDay.getName().equals(fileToMove)) {
                    found = true;
                    if (iter.hasNext()) {
                        nextFile = iter.next();
                    }
                }
            }

            if (found && (nextFile != null)) {
                moveSuccess = swapFileNamesAndRandomize(currentPath, nextFile.getName(), fileToMove);
            }
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(moveSuccess));

        doc.appendChild(resultElement);

        processResponse();
    }

}
