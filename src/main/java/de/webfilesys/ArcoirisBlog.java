/*  
 * arcoiris blog
 * Copyright (C) 2016 Frank Hoehnel

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package de.webfilesys;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Session;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;

import de.webfilesys.user.UserManager;
import de.webfilesys.user.XmlUserManager;
import de.webfilesys.util.CommonUtils;

/**
 * Container for configuration settings.
 */
public class ArcoirisBlog {
    private static ArcoirisBlog instance = null;

    public static final String VERSION = "Version 2.21.0-beta12 (07 Feb 2021)";

    public static final String DEFAULT_MAIL_SENDER_ADDRESS = "arcoirisblog@nowhere.com";

    public static final String DEFAULT_MAIL_SENDER_NAME = "arcoiris blog";

    public static final String LOG_CONFIG_FILE = "LogConfig.xml";

    private static final String LOOPBACK_ADDRESS = "127.0.0.1";

    // default upload limit: 32 MBytes
    private static final long DEFAULT_UPLOAD_LIMIT = (32l * 1024l * 1024l);

    // default maximum attachment size: 8 MBytes
    private static final long DEFAULT_ATTACHMENT_MAX_SIZE = (8l * 1024l * 1024l);
    
    // default disk quota: 16 MB
    private static long DEFAULT_DISK_QUOTA = 16l * 1024l * 1024l;

    private long defaultDiskQuota = DEFAULT_DISK_QUOTA;

    private String webAppRootDir = null;

    private String configBaseDir = null;

    private String localHostName;
    private String localIPAddress = null;

    private String primaryLanguage = null;

    private UserManager userMgr = null;

    private boolean openRegistration = false;

    private boolean debugMail = false;

    private Session mailSession = null;

    private boolean mailNotifyLogin = false;

    private boolean mailNotifyRegister = false;

    private boolean mailNotifyWelcome = false;

    private String mailHost = null;

    private boolean smtpAuth = false;

    private boolean smtpSecure = false;

    private String smtpUser = null;

    private String smtpPassword = null;

    private String mailSenderAddress = null;

    private String mailSenderName = null;

    private String clientUrl = null;

    private String contextRoot = null;

    /** the fully qualified server DNS name, if different from localhost DNS */
    private String serverDNS = null;

    private String userDocRoot = null;

    private String logoutURL = null;

    private String loginErrorPage = null;

    private String userMgrClass = null;

    private DiskQuotaInspector quotaInspector = null;

    private boolean mailNotifyQuotaAdmin = false;
    private boolean mailNotifyQuotaUser = false;

    private boolean enableDiskQuota = false;

    private int diskQuotaCheckHour = 3;

    private long uploadLimit = DEFAULT_UPLOAD_LIMIT;

    private long attachmentMaxSize = DEFAULT_ATTACHMENT_MAX_SIZE;
    
    private String googleMapsAPIKeyHTTP;
    private String googleMapsAPIKeyHTTPS;

    private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private DocumentBuilderFactory docFactory = null;

    public static ArcoirisBlog getInstance() {
        return (instance);
    }

    public static ArcoirisBlog createInstance(Properties configProps, String configBaseDir) {
        if (instance != null) {
            return (instance);
        }

        instance = new ArcoirisBlog(configProps, configBaseDir);

        return (instance);
    }

    private ArcoirisBlog(Properties config, String webAppRootDir) {
        Logger.getLogger(getClass()).info("starting arcoiris blog server " + VERSION);

        this.webAppRootDir = webAppRootDir;

        if (webAppRootDir.endsWith("\\") || webAppRootDir.endsWith("/")) {
            configBaseDir = webAppRootDir + "WEB-INF";
        } else {
            configBaseDir = webAppRootDir + "/WEB-INF";
        }

        Logger.getLogger(getClass()).info("java version : " + System.getProperty("java.version"));

        String opSysName = System.getProperty("os.name");

        Logger.getLogger(getClass()).info("operating system : " + opSysName);

        docFactory = DocumentBuilderFactory.newInstance();

        openRegistration = false;
        String temp = config.getProperty("RegistrationType", "closed");
        if (temp.equalsIgnoreCase("open")) {
            openRegistration = true;
            Logger.getLogger(getClass()).info("registration: open");
        } else {
            Logger.getLogger(getClass()).info("registration: closed");
        }

        userMgrClass = config.getProperty("UserManagerClass");

        userDocRoot = config.getProperty("UserDocumentRoot");

        if (openRegistration) {
            if (userDocRoot != null) {
                File docRootFile = new File(userDocRoot);

                if ((!docRootFile.exists()) || (!docRootFile.isDirectory()) || (!docRootFile.canWrite())) {
                    Logger.getLogger(getClass()).error("UserDocumentRoot is not a writable directory: " + userDocRoot);
                    userDocRoot = null;
                } else {
                    if ((File.separatorChar == '\\') && (userDocRoot.length() > 2)) {
                        try {
                            String canonicalRoot = docRootFile.getCanonicalPath().substring(2);
                            String absoluteRoot = docRootFile.getAbsolutePath().substring(2);

                            if (!canonicalRoot.equals(absoluteRoot)) {
                                Logger.getLogger(getClass()).error("UserDocumentRoot is not a writable directory (check uppercase/lowercase!): " + userDocRoot);
                                userDocRoot = null;
                            }
                        } catch (IOException ioex) {
                        }

                    }
                }

                if (userDocRoot != null) {
                    Logger.getLogger(getClass()).info("User Document Root: " + userDocRoot);
                }
            } else {
                userDocRoot = configBaseDir + File.separator + "userhome";

                Logger.getLogger(getClass()).info("using default UserDocumentRoot for open registration: " + userDocRoot);
            }
        }

        temp = config.getProperty("UploadLimit");

        if (temp != null) {
            try {
                uploadLimit = Long.parseLong(temp);
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).warn("invalid upload limit ignored: " + temp);
            }
        }

        temp = config.getProperty("AttachmentMaxSize");

        if (temp != null) {
            try {
                attachmentMaxSize = Long.parseLong(temp);
                
                if (attachmentMaxSize > uploadLimit) {
                    attachmentMaxSize = uploadLimit;
                    Logger.getLogger(getClass()).warn("max attachment size may not be larger than the upload limit of " + uploadLimit);
                }
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).warn("invalid value for max attachment size ignored: " + temp);
            }
        }

        if (openRegistration) {
            temp = config.getProperty("DiskQuotaDefaultMB", "1");

            try {
                int diskQuotaMB = Integer.parseInt(temp);

                defaultDiskQuota = ((long) diskQuotaMB) * 1024l * 1024l;
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).error("invalid default disk quota value: " + temp + " - using default value " + DEFAULT_DISK_QUOTA);
            }
        }

        logoutURL = config.getProperty("LogoutPageURL");

        loginErrorPage = config.getProperty("LoginErrorURL");

        mailHost = config.getProperty("SmtpMailHost");

        if ((mailHost != null) && (mailHost.trim().length() > 0)) {
            Logger.getLogger(getClass()).info("SMTP mail host: " + mailHost);

            temp = config.getProperty("SmtpAuth");

            smtpAuth = (temp != null) && temp.equalsIgnoreCase("true");

            if (smtpAuth) {
                smtpUser = config.getProperty("SmtpUser");
                if (CommonUtils.isEmpty(smtpUser)) {
                    Logger.getLogger(getClass()).error("SmtpUser property is required if SmtpAuth=true");
                }

                smtpPassword = config.getProperty("SmtpPassword");

                if (CommonUtils.isEmpty(smtpPassword)) {
                    Logger.getLogger(getClass()).error("smtpPassword property is required if SmtpAuth=true");
                }
            }

            temp = config.getProperty("SmtpSecure");

            smtpSecure = (temp != null) && temp.equalsIgnoreCase("true");

            mailSenderAddress = config.getProperty("MailSenderAddress", DEFAULT_MAIL_SENDER_ADDRESS);

            mailSenderName = config.getProperty("MailSenderName", DEFAULT_MAIL_SENDER_NAME);

            mailNotifyLogin = false;
            temp = config.getProperty("MailNotification.login", "false");
            if (temp.equalsIgnoreCase("true")) {
                mailNotifyLogin = true;
            }

            mailNotifyRegister = false;
            temp = config.getProperty("MailNotification.registration", "false");
            if (temp.equalsIgnoreCase("true")) {
                mailNotifyRegister = true;
            }

            mailNotifyWelcome = false;
            temp = config.getProperty("MailNotification.welcome", "false");
            if (temp.equalsIgnoreCase("true")) {
                mailNotifyWelcome = true;
            }

            clientUrl = config.getProperty("ClientURL");

        }

        serverDNS = config.getProperty("serverDNS");

        temp = config.getProperty("EnableDiskQuota", "false");

        if (temp.equalsIgnoreCase("true")) {
            enableDiskQuota = true;

            Logger.getLogger(getClass()).info("disk quota enabled");

            diskQuotaCheckHour = 3;

            temp = config.getProperty("DiskQuotaCheckHour", "3");

            try {
                diskQuotaCheckHour = Integer.parseInt(temp);
            } catch (NumberFormatException nfex) {
                Logger.getLogger(getClass()).error("invalid DiskQuotaCheckHour: " + temp);
            }

            mailNotifyQuotaAdmin = false;

            temp = config.getProperty("DiskQuotaNotifyAdmin", "false");

            if (temp.equalsIgnoreCase("true")) {
                mailNotifyQuotaAdmin = true;
            }

            mailNotifyQuotaUser = false;

            temp = config.getProperty("DiskQuotaNotifyUser", "false");

            if (temp.equalsIgnoreCase("true")) {
                mailNotifyQuotaUser = true;
            }
        } else {
            Logger.getLogger(getClass()).info("disk quota disabled");
        }

        temp = config.getProperty("DebugMail", "false");
        if (temp.equalsIgnoreCase("true")) {
            debugMail = true;
        }

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            localIPAddress = localHost.getHostAddress();
            localHostName = localHost.getHostName();
            Logger.getLogger(getClass()).info("local hostname: " + localHostName + "; local ip address : " + localIPAddress);
        } catch (Exception e) {
            Logger.getLogger(getClass()).error(e);
            try {
                localHostName = InetAddress.getLocalHost().toString();
            } catch (Exception o) {
                Logger.getLogger(getClass()).error(o);
                localHostName = "cannot query host name";
            }
        }

        googleMapsAPIKeyHTTP = config.getProperty("GoogleMapsAPIKeyHTTP");
        if (CommonUtils.isEmpty(googleMapsAPIKeyHTTP)) {
            Logger.getLogger(getClass()).warn("no google maps API key configured for HTTP (missing config property GoogleMapsAPIKeyHTTP)");
        }
        
        googleMapsAPIKeyHTTPS = config.getProperty("GoogleMapsAPIKeyHTTPS");
        if (CommonUtils.isEmpty(googleMapsAPIKeyHTTPS)) {
            Logger.getLogger(getClass()).warn("no google maps API key configured for HTTPS (missing config property GoogleMapsAPIKeyHTTPS)");
        }
        
        primaryLanguage = config.getProperty("primaryLanguage", LanguageManager.DEFAULT_LANGUAGE);

        contextRoot = config.getProperty("contextRoot", "/blog");

        Logger.getLogger(getClass()).info("primary language: " + primaryLanguage);
    }

    public void initialize(Properties config) {
        if ((userMgrClass == null) || (userMgrClass.trim().length() == 0)) {
            userMgr = new XmlUserManager();
        } else {
            try {
                userMgr = (UserManager) Class.forName(this.userMgrClass).newInstance();

                Logger.getLogger(getClass()).info("User Manager class: " + this.userMgrClass);
            } catch (ClassNotFoundException cnfex) {
                Logger.getLogger(getClass()).error("the user manager class " + userMgrClass + " cannot be found: " + cnfex);
            } catch (InstantiationException instEx) {
                Logger.getLogger(getClass()).error("the user manager cannot be instantiated: " + instEx);
            } catch (IllegalAccessException iaEx) {
                Logger.getLogger(getClass()).error("the user manager cannot be instantiated: " + iaEx);
            } catch (ClassCastException cex) {
                Logger.getLogger(getClass()).error("the class " + userMgrClass + " does not implement the UserManager interface: " + cex);
            }
        }

        LanguageManager.getInstance(primaryLanguage).listAvailableLanguages();

        readDateFormats(config);

        if (mailHost != null) {
            InvitationManager.getInstance();
        }

        if ((mailHost != null) && (mailHost.trim().length() > 0)) {
            initMailSession();
        }

        if (enableDiskQuota) {
            quotaInspector = new DiskQuotaInspector();
            quotaInspector.start();
        }
    }

    private void initMailSession() {
        Properties mailProps = new Properties();

        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.host", getMailHost());

        mailProps.put("mail.smtp.starttls.enable", isSmtpSecure());

        mailProps.put("mail.smtp.auth", isSmtpAuth());

        if (getSmtpUser() != null) {
            mailProps.put("mail.smtp.user", getSmtpUser());
        }

        mailSession = Session.getInstance(mailProps, null);

        if (isDebugMail()) {
            mailSession.setDebug(true);
        }
    }

    public Session getMailSession() {
        return mailSession;
    }

    protected void readDateFormats(Properties config) {
        Enumeration propertyNames = config.propertyNames();

        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();

            if (propertyName.startsWith("date.format.")) {
                try {
                    String lang = propertyName.substring(propertyName.lastIndexOf('.') + 1);

                    String dateFormatString = config.getProperty(propertyName);

                    if (dateFormatString.trim().length() == 0) {
                        dateFormatString = "yyyy/MM/dd HH:mm";
                    }

                    LanguageManager.getInstance().addDateFormat(lang, dateFormatString);
                } catch (IndexOutOfBoundsException iex) {
                    Logger.getLogger(getClass()).warn("invalid date format: " + iex);
                }
            }
        }
    }

    public int getDiskQuotaCheckHour() {
        return (diskQuotaCheckHour);
    }

    public String getMailHost() {
        return (mailHost);
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public boolean isSmtpSecure() {
        return smtpSecure;
    }

    public boolean isMailNotifyQuotaAdmin() {
        return mailNotifyQuotaAdmin;
    }

    public boolean isMailNotifyQuotaUser() {
        return mailNotifyQuotaUser;
    }

    public boolean isMailNotifyLogin() {
        return mailNotifyLogin;
    }

    public SimpleDateFormat getLogDateFormat() {
        return logDateFormat;
    }

    public String getConfigBaseDir() {
        return configBaseDir;
    }

    public UserManager getUserMgr() {
        return userMgr;
    }

    public boolean isDebugMail() {
        return debugMail;
    }

    public String getMailSenderAddress() {
        return mailSenderAddress;
    }

    public String getMailSenderName() {
        return mailSenderName;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public DocumentBuilderFactory getDocFactory() {
        return docFactory;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public boolean isOpenRegistration() {
        return openRegistration;
    }

    public String getWebAppRootDir() {
        return webAppRootDir;
    }

    public String getLoginErrorPage() {
        return loginErrorPage;
    }

    public String getLocalIPAddress() {
        return localIPAddress;
    }

    public long getUploadLimit() {
        return uploadLimit;
    }

    public long getAttachmentMaxSize() {
        return attachmentMaxSize;
    }
    
    public String getLoopbackAddress() {
        return LOOPBACK_ADDRESS;
    }

    public String getServerDNS() {
        return serverDNS;
    }

    public String getLogoutURL() {
        return logoutURL;
    }

    public String getUserDocRoot() {
        return userDocRoot;
    }

    public long getDefaultDiskQuota() {
        return defaultDiskQuota;
    }

    public boolean isMailNotifyRegister() {
        return mailNotifyRegister;
    }

    public boolean isMailNotifyWelcome() {
        return mailNotifyWelcome;
    }

    public DiskQuotaInspector getDiskQuotaInspector() {
        return quotaInspector;
    }

    public String getContextRoot() {
        return contextRoot;
    }
    
    public String getGoogleMapsAPIKeyHTTP() {
        return googleMapsAPIKeyHTTP;
    }

    public String getGoogleMapsAPIKeyHTTPS() {
        return googleMapsAPIKeyHTTPS;
    }
    
}
