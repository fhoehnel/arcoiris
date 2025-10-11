package de.webfilesys.daytitle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class DayTitleManager {

    private static final Logger LOG = Logger.getLogger(DayTitleManager.class);
    public static final String DAY_TITLES_JSON_FILE_NAME = "dayTitles.json";
    public static final String DAY_TITLES_JSON_NEW_FILE_NAME = "dayTitles-new.json";

    private static DayTitleManager instance;

    public static synchronized DayTitleManager getInstance() {
        if (instance == null) {
            instance = new DayTitleManager();
        }
        return instance;
    }

    // key: path (unique per user), value: map of title texts for blog days
    private HashMap<String, HashMap<String, DayTitle>> dayTitleMap = new HashMap<>();

    public void setDayTitle(String path, String day, String titleText) {
        HashMap<String, DayTitle> userDayTitles = dayTitleMap.get(path);
        if (userDayTitles == null) {
            userDayTitles = new HashMap<>();
            dayTitleMap.put(path, userDayTitles);
        }
        DayTitle newDayTitle = new DayTitle();
        newDayTitle.setDay(day);
        newDayTitle.setTitle(titleText);
        userDayTitles.put(day, newDayTitle);
        saveDayTitles(path, userDayTitles);
    }

    public void deleteDayTitle(String path, String day) {
        HashMap<String, DayTitle> userDayTitles = dayTitleMap.get(path);
        if (userDayTitles == null) {
            return;
        }
        userDayTitles.remove(day);
        saveDayTitles(path, userDayTitles);
    }

    public String getDayTitle(String path, String day) {
        HashMap<String, DayTitle> userDayTitles = dayTitleMap.get(path);
        if (userDayTitles == null) {
            userDayTitles = loadDayTitles(path);
            dayTitleMap.put(path, userDayTitles);
        }
        DayTitle dayTitle = userDayTitles.get(day);
        return dayTitle != null ? dayTitle.getTitle() : null;
    }

    private void saveDayTitles(String path, HashMap<String, DayTitle> userDayTitles) {
        String dayTitleNewFilePath = path + File.separator + DAY_TITLES_JSON_NEW_FILE_NAME;
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writeValueAsString(userDayTitles);
            Files.write(Paths.get(dayTitleNewFilePath), jsonResult.getBytes(StandardCharsets.UTF_8));
            File newFile = new File(dayTitleNewFilePath);
            if (newFile.exists() && newFile.canWrite()) {
                boolean renameSuccess = true;
                File dayTitleFile = new File(path + File.separator + DAY_TITLES_JSON_FILE_NAME);
                if (dayTitleFile.exists()) {
                    boolean deleted = dayTitleFile.delete();
                    if (!deleted) {
                        LOG.error("failed to remove old day title file");
                    } else {
                        renameSuccess = newFile.renameTo(dayTitleFile);
                    }
                } else {
                    renameSuccess = newFile.renameTo(dayTitleFile);
                }
                if (!renameSuccess) {
                    LOG.error("failed to rename day title file " + dayTitleNewFilePath);
                }
            }
        } catch (IOException ex) {
            LOG.error("failed to write day titles to path " + path, ex);
        }
    }

    private HashMap<String, DayTitle> loadDayTitles(String path) {
        String dayTitleFilePath = path + File.separator + DAY_TITLES_JSON_FILE_NAME;
        File dayTitleFile = new File(dayTitleFilePath);
        if (!dayTitleFile.exists()) {
            return new HashMap<>();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = Files.readString(Paths.get(dayTitleFilePath), StandardCharsets.UTF_8);
            TypeReference<HashMap<String, DayTitle>> typeRef = new TypeReference<>() {};
            return mapper.readValue(json, typeRef);
        } catch (IOException ex) {
            LOG.error("failed to read day titles for path " + path, ex);
            return new HashMap<>();
        }
    }
}
