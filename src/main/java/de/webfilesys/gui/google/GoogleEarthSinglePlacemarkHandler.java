package de.webfilesys.gui.google;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;

import de.webfilesys.util.CommonUtils;

/**
 * @author Frank Hoehnel
 */
public class GoogleEarthSinglePlacemarkHandler extends GoogleEarthHandlerBase {
    public GoogleEarthSinglePlacemarkHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected ArrayList<Element> createPlacemarkXml() {
        ArrayList<Element> placemarkElementList = new ArrayList<Element>();

        String fileName = getParameter("fileName");

        if (CommonUtils.isEmpty(fileName)) {
            LogManager.getLogger(getClass()).warn("fileName parameter missing");
            return placemarkElementList;
        }

        String filePath = getCwd();
        if (filePath.endsWith(File.separator)) {
            filePath = filePath + fileName;
        } else {
            filePath = filePath + File.separator + fileName;
        }

        File file = new File(filePath);

        if (!file.exists()) {
            LogManager.getLogger(getClass()).error("file not found: " + filePath);
            return placemarkElementList;
        }

        placemarkElementList.add(createPlacemark(filePath));

        return placemarkElementList;
    }
}
