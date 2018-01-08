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
        
        boolean moveDown = false;
        
        int currentPos = getCurrentPos(filesOfDay, fileToMove);

        int moveTarget = 0;
        
        if (targetPos.equals("top")) {
            moveTarget = 0;
        } else if (targetPos.equals("bottom")) {
            moveTarget = filesOfDay.size() -1;
            moveDown = true;
        } else {
            try {
                moveTarget = Integer.parseInt(targetPos) - 1;
                if (moveTarget > currentPos) {
                    moveDown = true;
                }
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
        
        boolean success = moveFileToTarget(getCwd(), currentFileAtTargetPos, fileToMove, moveDown);        

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));

        doc.appendChild(resultElement);

        processResponse();
    }
    
    private int getCurrentPos(ArrayList<File> filesOfDay, String fileToMove) {
        int posCounter = 0;
        
        for (File file : filesOfDay) {
            if (file.getName().equals(fileToMove)) {
                return posCounter;
            }
            posCounter++;
        }
        
        return filesOfDay.size() - 1;
    }
    
    protected boolean moveFileToTarget(String currentPath, String currentFileAtTargetPos, String fileToMove, boolean moveDown) {

        int firstDotIdx = currentFileAtTargetPos.indexOf('.');
        String targetNameBase = currentFileAtTargetPos.substring(0, firstDotIdx);
        
        int lastDotIdx = currentFileAtTargetPos.lastIndexOf('.');
        String targetNameExt = currentFileAtTargetPos.substring(lastDotIdx + 1);

        String suffix1;
        String suffix2;
        if (moveDown) {
            suffix1 = "-2.";
            suffix2 = "-1.";
        } else {
            suffix1 = "-1.";
            suffix2 = "-2.";
        }
        
        String insertTargetName = targetNameBase  + suffix1 + targetNameExt;

        String moveTargetName = targetNameBase  + suffix2 + targetNameExt;

        if (renameInclMetaInf(currentPath, currentFileAtTargetPos, moveTargetName)) {
            if (renameInclMetaInf(currentPath, fileToMove, insertTargetName)) {
                return true;
            }
        }
        
        return false;
    }
    
}
