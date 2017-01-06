package de.webfilesys.gui.google;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
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
            Logger.getLogger(getClass()).warn("fileName parameter missing");
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
            Logger.getLogger(getClass()).error("file not found: " + filePath);
            return placemarkElementList;
        }

        placemarkElementList.add(createPlacemark(filePath));

        return placemarkElementList;
    }
}
