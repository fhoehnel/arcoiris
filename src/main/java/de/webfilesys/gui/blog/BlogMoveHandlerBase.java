package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.FileComparator;
import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;

/**
 * Base class for handlers that move blog entries.
 */
public abstract class BlogMoveHandlerBase extends XmlRequestHandlerBase {
    
    public BlogMoveHandlerBase(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }
    
    protected ArrayList<File> getAllFilesOfDaySorted(String currentPath, String fileToMove) {

        ArrayList<File> entriesOfDay = new ArrayList<File>();

        String currentFileDate = fileToMove.substring(0, 10);

        File blogDir = new File(currentPath);

        File[] filesInDir = blogDir.listFiles();

        for (int i = 0; i < filesInDir.length; i++) {
            if (filesInDir[i].isFile() && filesInDir[i].canRead()) {

                if (CommonUtils.isPictureFile(filesInDir[i])) {

                    String fileName = filesInDir[i].getName();
                    if (fileName.length() >= 10) {
                        String blogDate = fileName.substring(0, 10);

                        if (blogDate.equals(currentFileDate)) {
                            entriesOfDay.add(filesInDir[i]);
                        }
                    }
                }
            }
        }

        if (entriesOfDay.size() > 1) {
            Collections.sort(entriesOfDay, new FileComparator());
        }

        return entriesOfDay;
    }
    
    protected boolean swapFileNamesAndRandomize(String currentPath, String fileName1, String fileName2) {

        boolean moveSuccess = false;

        int firstDotIdx = fileName1.indexOf('.');
        String fileName1Base = fileName1.substring(0, firstDotIdx);

        int lastDotIdx = fileName1.lastIndexOf('.');
        String fileName1Ext = fileName1.substring(lastDotIdx + 1);

        // file 1 -> temp file name
        String tempFileName = System.currentTimeMillis() + "." + fileName1Ext;
        if (renameInclMetaInf(currentPath, fileName1, tempFileName)) {

            // file 2 -> file 1 name + rand
            String file2NewName = fileName1Base + "." + System.currentTimeMillis() + "." + fileName1Ext;
            if (renameInclMetaInf(currentPath, fileName2, file2NewName)) {

                // temp filename -> file 2
                firstDotIdx = fileName2.indexOf('.');
                String fileName2Base = fileName2.substring(0, firstDotIdx);

                lastDotIdx = fileName2.lastIndexOf('.');
                String fileName2Ext = fileName2.substring(lastDotIdx + 1);

                String file1NewName = fileName2Base + "." + System.currentTimeMillis() + "." + fileName2Ext;

                if (renameInclMetaInf(currentPath, tempFileName, file1NewName)) {
                    moveSuccess = true;
                }
            }
        }

        return moveSuccess;
    }
    
    protected boolean renameInclMetaInf(String currentPath, String fileToMove, String newFileName) {
        File sourceFile = new File(currentPath, fileToMove);
        File destFile = new File(currentPath, newFileName);

        if (!sourceFile.renameTo(destFile)) {
            Logger.getLogger(getClass()).error("failed to rename file " + fileToMove + " to : " + newFileName);
            return false;
        }

        MetaInfManager.getInstance().moveMetaInf(currentPath, fileToMove, newFileName);

        String titlePic = MetaInfManager.getInstance().getTitlePic(currentPath);
        if ((titlePic != null) && titlePic.equals(fileToMove)) {
            MetaInfManager.getInstance().setTitlePic(currentPath, newFileName);
        }

        BlogThumbnailHandler.getInstance().renameThumbnail(sourceFile.getAbsolutePath(), newFileName);

        return true;
    }
}
