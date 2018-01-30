package de.webfilesys.gui.blog;

import java.io.File;
import java.io.PrintWriter;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.BlogStatistic;
import de.webfilesys.Comment;
import de.webfilesys.InvitationManager;
import de.webfilesys.MetaInfManager;
import de.webfilesys.StatisticManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

public class BlogStatisticsHandler extends XmlRequestHandlerBase {

    public BlogStatisticsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        Element statisticsElement = doc.createElement("statistics");

        doc.appendChild(statisticsElement);

        String virtualUser = InvitationManager.getInstance().getFirstVirtualUser(uid);
        
        BlogStatistic blogStats = StatisticManager.getInstance().getVisitsByAge(virtualUser);
        
        getCommentStatistics(blogStats);
        
        Element periodElem = doc.createElement("period");
        XmlUtil.setChildText(periodElem,  "dateRange", getResource("statLast24Hours", "last 24 hours"));
        statisticsElement.appendChild(periodElem);
        
        addStatElem(periodElem, getResource("statisticVisits", "visits"), blogStats.getVisitsLastDay());
        addStatElem(periodElem, getResource("statisticVisitors", "distinct visitors"), blogStats.getDistinctVisitorsLastDay());
        addStatElem(periodElem, getResource("statisticComments", "visitor comments"), blogStats.getCommentsLastDay());

        periodElem = doc.createElement("period");
        XmlUtil.setChildText(periodElem,  "dateRange", getResource("statLast48Hours", "last 48 hours"));
        statisticsElement.appendChild(periodElem);
        
        addStatElem(periodElem, getResource("statisticVisits", "visits"), blogStats.getVisitsLast2Days());
        addStatElem(periodElem, getResource("statisticVisitors", "distinct visitors"), blogStats.getDistinctVisitorsLast2Days());
        addStatElem(periodElem, getResource("statisticComments", "visitor comments"), blogStats.getCommentsLast2Days());

        periodElem = doc.createElement("period");
        XmlUtil.setChildText(periodElem,  "dateRange", getResource("statLastWeek", "last week"));
        statisticsElement.appendChild(periodElem);
        
        addStatElem(periodElem, getResource("statisticVisits", "visits"), blogStats.getVisitsLastWeek());
        addStatElem(periodElem, getResource("statisticVisitors", "distinct visitors"), blogStats.getDistinctVisitorsLastWeek());
        addStatElem(periodElem, getResource("statisticComments", "visitor comments"), blogStats.getCommentsLastWeek());

        periodElem = doc.createElement("period");
        XmlUtil.setChildText(periodElem,  "dateRange", getResource("statLastMonth", "last month"));
        statisticsElement.appendChild(periodElem);
        
        addStatElem(periodElem, getResource("statisticVisits", "visits"), blogStats.getVisitsLastMonth());
        addStatElem(periodElem, getResource("statisticVisitors", "distinct visitors"), blogStats.getDistinctVisitorsLastMonth());
        addStatElem(periodElem, getResource("statisticComments", "visitor comments"), blogStats.getCommentsLastMonth());

        periodElem = doc.createElement("period");
        XmlUtil.setChildText(periodElem,  "dateRange", getResource("statLastYear", "last year"));
        statisticsElement.appendChild(periodElem);
        
        addStatElem(periodElem, getResource("statisticVisits", "visits"), blogStats.getVisitsLastYear());
        addStatElem(periodElem, getResource("statisticVisitors", "distinct visitors"), blogStats.getDistinctVisitorsLastYear());
        addStatElem(periodElem, getResource("statisticComments", "visitor comments"), blogStats.getCommentsLastYear());
        
        processResponse();
    }

    private void  addStatElem(Element statPeriodElem, String dateRange, int visitCount) {
        Element statisticEntryElem = doc.createElement("statisticEntry");

        XmlUtil.setChildText(statisticEntryElem, "category", dateRange);
        XmlUtil.setChildText(statisticEntryElem, "count", Integer.toString(visitCount));

        statPeriodElem.appendChild(statisticEntryElem);
    }
    
    private void getCommentStatistics(BlogStatistic blogStats) {
        
        File blogDir = new File(getCwd());
        
        if (!blogDir.exists() || (!blogDir.isDirectory()) || (!blogDir.canRead())) {
            return;
        }
        
        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        long now = System.currentTimeMillis();

        File[] filesInDir = blogDir.listFiles();

        for (int i = 0; i < filesInDir.length; i++) {
            if (filesInDir[i].isFile() && filesInDir[i].canRead()) {

                if (CommonUtils.isPictureFile(filesInDir[i])) {

                    Vector<Comment> commentList = metaInfMgr.getListOfComments(filesInDir[i].getAbsolutePath());

                    if (commentList != null) {
                        for (Comment comment : commentList) {
                            if (!comment.getUser().equals(uid)) {
                                if (now - comment.getCreationTime() < StatisticManager.YEAR_MILLIS) {
                                    blogStats.setCommentsLastYear(blogStats.getCommentsLastYear() + 1);

                                    if (now - comment.getCreationTime() < StatisticManager.MONTH_MILLIS) {
                                        blogStats.setCommentsLastMonth(blogStats.getCommentsLastMonth() + 1);

                                        if (now - comment.getCreationTime() < StatisticManager.WEEK_MILLIS) {
                                            blogStats.setCommentsLastWeek(blogStats.getCommentsLastWeek() + 1);

                                            if (now - comment.getCreationTime() < StatisticManager.TWO_DAY_MILLIS) {
                                                blogStats.setCommentsLast2Days(blogStats.getCommentsLast2Days() + 1);

                                                if (now - comment.getCreationTime() < StatisticManager.DAY_MILLIS) {
                                                    blogStats.setCommentsLastDay(blogStats.getCommentsLastDay() + 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
