package de.webfilesys.gui.ajax;

import java.io.File;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import de.webfilesys.config.BlogConfigManager;
import de.webfilesys.metainf.BlogMetaInfManager;
import org.w3c.dom.Element;

import de.webfilesys.GeoTag;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

public class AjaxCheckForGeoDataHandler extends XmlRequestHandlerBase {

    public AjaxCheckForGeoDataHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {
        boolean geoDataExist = false;

        String path = getCwd();

        boolean stagedPublication = BlogConfigManager.getInstance().isStagedPublication(path);

        File dirFile = new File(path);

        File[] fileList = dirFile.listFiles();

        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile() && file.canRead()) {
                    if (CommonUtils.isPictureFile(file)) {
                        if ((!stagedPublication) || (BlogMetaInfManager.getInstance().getStatus(file.getAbsolutePath()) != BlogMetaInfManager.STATUS_BLOG_EDIT)) {
                            if (hasGeoData(file.getAbsolutePath())) {
                                geoDataExist = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setElementText(resultElement, Boolean.toString(geoDataExist));

        doc.appendChild(resultElement);

        processResponse();
    }

    private boolean hasGeoData(String imgPath) {
        GeoTag geoTag = BlogMetaInfManager.getInstance().getGeoTag(imgPath);
        if (geoTag != null) {
            return true;
        }

        String fileExt = CommonUtils.getFileExtension(imgPath);

        if (fileExt.equals(".jpg") || fileExt.equals(".jpeg")) {
            CameraExifData exifData = new CameraExifData(imgPath);

            if (exifData.hasExifData()) {
                float gpsLatitude = exifData.getGpsLatitude();
                float gpsLongitude = exifData.getGpsLongitude();

                if ((gpsLatitude >= 0.0f) && (gpsLongitude >= 0.0f)) {
                    return true;
                }
            }
        }

        return false;
    }
}
