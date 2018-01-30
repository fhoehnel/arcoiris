package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

public class StatisticManager extends Thread {
    
    private static final String STATISTIC_FILE_NAME = "statistics.xml";
    
    private static final Logger LOG = Logger.getLogger(StatisticManager.class);
    
    public final static long DAY_MILLIS = 1000L * 60L * 60L * 24L;
    public final static long TWO_DAY_MILLIS = 2L * DAY_MILLIS;
    public final static long WEEK_MILLIS = 7L * DAY_MILLIS;
    public final static long MONTH_MILLIS = 30L * DAY_MILLIS;
    public final static long YEAR_MILLIS = 365L * DAY_MILLIS;
    
    private static final long EXPIRATION_MILLIS = 366 * DAY_MILLIS;
    
    private HashMap<String, Element> userCache = null;

    boolean cacheDirty = false;

    private Document doc;
    
    private DocumentBuilder builder = null;

    private Element statisticRoot = null;

    boolean shutdownFlag = false;

    private static StatisticManager statisticManager = null;

    private String statisticFilePath = null;
    
    private StatisticManager() {
        shutdownFlag = false;
        
        statisticFilePath = ArcoirisBlog.getInstance().getConfigBaseDir() + "/" + STATISTIC_FILE_NAME;

        builder = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pcex) {
            LOG.error(pcex);
        }

        userCache = new HashMap<String, Element>();

        loadFromFile();
        
        this.start();
    }

    public static StatisticManager getInstance() {
        if (statisticManager == null) {
            statisticManager = new StatisticManager();
        }

        return statisticManager;
    }

    
    public void loadFromFile() {
        
        File statisticFile = new File(statisticFilePath);

        if (!statisticFile.exists()) {
            LOG.info("creating new statistic data file " + statisticFilePath);

            doc = builder.newDocument();
            statisticRoot = doc.createElement("statistics");
        } else {
            if ((!statisticFile.isFile()) || (!statisticFile.canRead())) {
                LOG.error("statistic file " + statisticFilePath + " is not a readable file");
                return;
            }
            
            LOG.info("reading statistic data from file " + statisticFile.getAbsolutePath());
            
            doc = null;

            FileInputStream fis = null;

            try {
                fis = new FileInputStream(statisticFile);

                InputSource inputSource = new InputSource(fis);

                inputSource.setEncoding("UTF-8");

                doc = builder.parse(inputSource);
            } catch (SAXException saxex) {
                LOG.error("failed to load statistic data from file " + statisticFile.getAbsolutePath(), saxex);
            } catch (IOException ioex) {
                LOG.error("failed to load statistic data  from file : " + statisticFile.getAbsolutePath(), ioex);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception ex) {
                    }
                }
            }

            if (doc == null) {
                return;
            }

            statisticRoot = doc.getDocumentElement();
        
            if (statisticRoot != null) {
                // put user elements into cache
                NodeList userList = statisticRoot.getElementsByTagName("user");

                if (userList != null) {
                    int listLength = userList.getLength();

                    for (int i = 0; i < listLength; i++) {
                        Element userElement = (Element) userList.item(i);

                        String id = userElement.getAttribute("id");

                        if (!CommonUtils.isEmpty(id)) {
                            userCache.put(id, userElement);
                        }
                    }
                }
            }
        }
    }
    
    public synchronized void saveToFile() {
        if (statisticRoot == null) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("saving statisic data file: " + statisticFilePath);
        }

        synchronized (statisticRoot) {
            OutputStreamWriter xmlOutFile = null;

            try {
                FileOutputStream fos = new FileOutputStream(statisticFilePath);

                xmlOutFile = new OutputStreamWriter(fos, "UTF-8");

                XmlUtil.writeToStream(statisticRoot, xmlOutFile);

                xmlOutFile.flush();

                cacheDirty = false;
            } catch (IOException io1) {
                LOG.error("error saving statistics data file " + statisticFilePath, io1);
            } finally {
                if (xmlOutFile != null) {
                    try {
                        xmlOutFile.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }
    
    public synchronized void run() {
        boolean exitFlag = false;

        int loopCounter = 0;
        
        while (!exitFlag) {
            try {
                this.wait(60000);

                if (cacheDirty) {
                    saveToFile();
                }
                
                loopCounter++;
                
                if (loopCounter == 60 * 24) {
                    removeExpiredVisits();
                    loopCounter = 0;

                    if (cacheDirty) {
                        saveToFile();
                    }
                }
            } catch (InterruptedException e) {
                if (cacheDirty) {
                    saveToFile();
                }

                exitFlag = true;
                
                if (LOG.isDebugEnabled()) {
                    LOG.debug("StatisticManager ready for shutdown");
                }
            }
        }
    }

    private Element getUserElem(String userid) {
        Element userElem = userCache.get(userid);
        
        if (userElem == null) {
            userElem = doc.createElement("user");
            userElem.setAttribute("id", userid);
            statisticRoot.appendChild(userElem);
            userCache.put(userid, userElem);
            cacheDirty = true;
        }
        
        return userElem;
    }
    
    private Element getVisitListElem(String userid) {
        Element userElem = getUserElem(userid);

        Element visitListElem = XmlUtil.getChildByTagName(userElem, "visits");
        
        if (visitListElem == null) {
            visitListElem = doc.createElement("visits");
            userElem.appendChild(visitListElem);
            cacheDirty = true;
        }
        
        return visitListElem;
    }
    
    public void addVisit(String userid, String visitorId) {
        
        synchronized(statisticRoot) {
            Element visitListElem = getVisitListElem(userid);
            
            Element visitElem = doc.createElement("v");
            XmlUtil.setChildText(visitElem, "t", Long.toString(System.currentTimeMillis()));
            XmlUtil.setChildText(visitElem, "vId", visitorId);
            
            visitListElem.appendChild(visitElem);
            cacheDirty = true;
        }        
    }
    
    public BlogStatistic getVisitsByAge(String userid) {

        HashMap<String, Boolean> distinctVisitorsLastDay = new HashMap<String, Boolean>();
        HashMap<String, Boolean> distinctVisitorsLast2Days = new HashMap<String, Boolean>();
        HashMap<String, Boolean> distinctVisitorsLastWeek = new HashMap<String, Boolean>();
        HashMap<String, Boolean> distinctVisitorsLastMonth = new HashMap<String, Boolean>();
        HashMap<String, Boolean> distinctVisitorsLastYear = new HashMap<String, Boolean>();
        
        BlogStatistic visitStats = new BlogStatistic();
        
        Element visitListElem = getVisitListElem(userid);
        
        NodeList visitList = visitListElem.getElementsByTagName("v");

        if (visitList != null) {

            long now = System.currentTimeMillis();
            
            int listLength = visitList.getLength();

            for (int i = 0; i < listLength; i++) {
                Element visitElem = (Element) visitList.item(i);

                String timestamp = XmlUtil.getChildText(visitElem, "t");
                
                try {
                    long visitTime = Long.parseLong(timestamp);
                    
                    String visitorId = XmlUtil.getChildText(visitElem, "vId");

                    if (now - visitTime < YEAR_MILLIS) {
                        visitStats.setVisitsLastYear(visitStats.getVisitsLastYear() + 1);

                        distinctVisitorsLastYear.put(visitorId, Boolean.TRUE);

                        if (now - visitTime < MONTH_MILLIS) {
                            visitStats.setVisitsLastMonth(visitStats.getVisitsLastMonth() + 1);

                            distinctVisitorsLastMonth.put(visitorId, Boolean.TRUE);

                            if (now - visitTime < WEEK_MILLIS) {
                                visitStats.setVisitsLastWeek(visitStats.getVisitsLastWeek() + 1);

                                distinctVisitorsLastWeek.put(visitorId, Boolean.TRUE);
                                
                                if (now - visitTime < TWO_DAY_MILLIS) {
                                    visitStats.setVisitsLast2Days(visitStats.getVisitsLast2Days() + 1);

                                    distinctVisitorsLast2Days.put(visitorId, Boolean.TRUE);

                                    if (now - visitTime < DAY_MILLIS) {
                                        visitStats.setVisitsLastDay(visitStats.getVisitsLastDay() + 1);

                                        distinctVisitorsLastDay.put(visitorId, Boolean.TRUE);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOG.error("invalid timestamp value: " + timestamp);
                }
            }
        }
        
        visitStats.setDistinctVisitorsLastDay(distinctVisitorsLastDay.size());
        visitStats.setDistinctVisitorsLast2Days(distinctVisitorsLast2Days.size());
        visitStats.setDistinctVisitorsLastWeek(distinctVisitorsLastWeek.size());
        visitStats.setDistinctVisitorsLastMonth(distinctVisitorsLastMonth.size());
        visitStats.setDistinctVisitorsLastYear(distinctVisitorsLastYear.size());
        
        return visitStats;
    }
    
    private void removeExpiredVisits() {
       
        synchronized(statisticRoot) {

            int expirationCounter = 0;
            
            NodeList userList = statisticRoot.getElementsByTagName("user");

            if (userList != null) {

                long now = System.currentTimeMillis();
                
                int userListLength = userList.getLength();

                for (int i = 0; i < userListLength; i++) {
                    Element userElem = (Element) userList.item(i);
                    
                    Element visitListElem = XmlUtil.getChildByTagName(userElem, "visits");

                    if (visitListElem != null) {
                        NodeList visitList = visitListElem.getElementsByTagName("v");

                        if (visitList != null) {

                            int listLength = visitList.getLength();

                            for (int k = listLength - 1; k >= 0; k--) {
                                Element visitElem = (Element) visitList.item(k);

                                String timestamp = XmlUtil.getChildText(visitElem, "t");
                                
                                try {
                                    long visitTime = Long.parseLong(timestamp);
                                    
                                    if (now - visitTime > EXPIRATION_MILLIS) {
                                        visitListElem.removeChild(visitElem);
                                        expirationCounter++;
                                        cacheDirty = true;
                                    }
                                } catch (Exception ex) {
                                    LOG.error("invalid timestamp value: " + timestamp);
                                }
                            }
                        }
                    }
                }
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug(expirationCounter + " expired visits removed from statistcis");
            }
        }
    }
}
