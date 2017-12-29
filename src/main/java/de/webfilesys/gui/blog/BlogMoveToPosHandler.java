package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogMoveToPosHandler extends BlogMoveHandlerBase {

    public BlogMoveToPosHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        String fileToMove = getParameter("fileName");

        String targetPos = getParameter("newPos");
        
        ArrayList<File> filesOfDay = getAllFilesOfDaySorted(getCwd(), fileToMove);
        
        int moveTarget = 0;
        
        if (targetPos.equals("top")) {
            moveTarget = 0;
        } else if (targetPos.equals("bottom")) {
            moveTarget = filesOfDay.size() -1;
        } else {
            try {
                moveTarget = Integer.parseInt(targetPos) - 1;
            } catch (Exception ex) {
                Logger.getLogger(getClass()).error("invalid target pos for move: " + targetPos, ex);
            }
        }
        
        String currentFileAtTargetPos = filesOfDay.get(moveTarget).getName();
        
        if (currentFileAtTargetPos.equals("fileToMove")) {
            // should never happen
            Logger.getLogger(getClass()).error("invalid target pos for move: " + targetPos);
            return;
        }
        
        boolean success = swapFileNamesAndRandomize(getCwd(), currentFileAtTargetPos, fileToMove);        

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));

        doc.appendChild(resultElement);

        processResponse();
    }
}
