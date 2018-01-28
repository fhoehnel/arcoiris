package de.webfilesys.gui.blog;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.InvitationManager;
import de.webfilesys.StatisticManager;
import de.webfilesys.VisitStatistic;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.XmlUtil;

public class BlogStatisticsHandler extends XmlRequestHandlerBase {

    public BlogStatisticsHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected void process() {

        Element statisticsElement = doc.createElement("statistics");

        doc.appendChild(statisticsElement);

        String virtualUser = InvitationManager.getInstance().getFirstVirtualUser(uid);
        
        VisitStatistic visitStats = StatisticManager.getInstance().getVisitsByAge(virtualUser);
        
        addStatElem(statisticsElement, getResource("visitsLast24Hours", "visits last 24 hours"), visitStats.getVisitsLastDay());
        addStatElem(statisticsElement, getResource("visitorsLast24Hours", "distinct visitors last 24 hours"), visitStats.getDistinctVisitorsLastDay());
        addStatElem(statisticsElement, getResource("visitsLast48Hours", "visits last 48 hours"), visitStats.getVisitsLast2Days());
        addStatElem(statisticsElement, getResource("visitorsLast48Hours", "distinct visitors last 48 hours"), visitStats.getDistinctVisitorsLast2Days());
        addStatElem(statisticsElement, getResource("visitsLastWeek", "visits last week"), visitStats.getVisitsLastWeek());
        addStatElem(statisticsElement, getResource("visitorsLastWeek", "distinct visitors last week"), visitStats.getDistinctVisitorsLastWeek());
        addStatElem(statisticsElement, getResource("visitsLastMonth", "visits last month"), visitStats.getVisitsLastMonth());
        addStatElem(statisticsElement, getResource("visitorsLastMonth", "distinct visitors last month"), visitStats.getDistinctVisitorsLastMonth());
        addStatElem(statisticsElement, getResource("visitsLastYear", "visits last year"), visitStats.getVisitsLastYear());
        addStatElem(statisticsElement, getResource("visitorsLastYear", "distinct visitors last year"), visitStats.getDistinctVisitorsLastYear());
        
        processResponse();
    }

    private void  addStatElem(Element statisticsElement, String dateRange, int visitCount) {
        Element statisticEntryElem = doc.createElement("statisticEntry");

        XmlUtil.setChildText(statisticEntryElem, "dateRange", dateRange);
        XmlUtil.setChildText(statisticEntryElem, "visitCount", Integer.toString(visitCount));

        statisticsElement.appendChild(statisticEntryElem);
    }
    
}
