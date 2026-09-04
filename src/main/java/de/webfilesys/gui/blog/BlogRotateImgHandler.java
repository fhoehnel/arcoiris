package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.attachment.AttachmentManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.graphics.BlogThumbnailHandler;
import de.webfilesys.graphics.ImageTransform;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class BlogRotateImgHandler extends XmlRequestHandlerBase {

    private static final String DIRECTION_LEFT = "left";
    private static final String DIRECTION_RIGHT = "right";

    public BlogRotateImgHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        if (!checkWriteAccess()) {
            return;
        }

        String currentPath = userMgr.getDocumentRoot(uid).replace('/', File.separatorChar);

        String imgName = getParameter("imgName");
        if (CommonUtils.isEmpty(imgName)) {
            LogManager.getLogger(getClass()).error("missing parameter imgName");
            return;
        }

        File imgFile = new File(currentPath, imgName);

        if ((!imgFile.exists()) || (!imgFile.isFile()) || (!imgFile.canWrite())) {
            LogManager.getLogger(getClass()).error("img file is not a writable file: " + imgFile.getAbsolutePath());
        }

        String direction = getParameter("direction");
        if (CommonUtils.isEmpty(direction)) {
            LogManager.getLogger(getClass()).error("missing parameter direction");
            return;
        }

        if ((!direction.equals(DIRECTION_LEFT)) && (!direction.equals(DIRECTION_RIGHT))) {
            LogManager.getLogger(getClass()).error("invalid parameter value for direction: " + direction);
            return;
        }

        String degrees = "90";
        if (direction.equals(DIRECTION_LEFT)) {
            degrees = "270";
        }

        ImageTransform imgTrans = new ImageTransform(imgFile.getAbsolutePath(), "rotate", degrees);

        String resultImageName = imgTrans.execute(false);

        boolean success = false;

        if (resultImageName != null) {
            BlogMetaInfManager.getInstance().moveMetaInf(currentPath, imgName, resultImageName);
            AttachmentManager.getInstance().moveAttachments(currentPath, imgName, resultImageName);

            // TODO: test which method results in better image quality:
            // - rotate (lossy) the existing thumbnail image
            // - create a new thumbnail from the (losslessly) rotated big image

            // BlogThumbnailHandler.getInstance().rotateThumbnail(imgFile.getAbsolutePath(),
            // degrees);

            BlogThumbnailHandler.getInstance().deleteThumbnail(imgFile.getAbsolutePath());

            File resultImgFile = new File(currentPath, resultImageName);

            BlogThumbnailHandler.getInstance().createBlogThumbnail(resultImgFile.getAbsolutePath());

            success = true;

            if (imgFile.exists()) {
                success = imgFile.delete();
            }
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));

        // XmlUtil.setChildText(resultElement, "resultFileName",
        // resultImageName);

        doc.appendChild(resultElement);

        processResponse();
    }
}
