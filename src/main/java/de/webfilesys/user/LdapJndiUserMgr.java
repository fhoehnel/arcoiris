package de.webfilesys.user;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;

public class LdapJndiUserMgr extends UserManagerBase {

    private static Logger LOG = Logger.getLogger(LdapJndiUserMgr.class);

    // TODO: use the WEB-INF dir of the wepapp
    private static final String CONFIG_PATH = "C:/Projekte/webfilesys/maven-project/webfilesys/src/main/webapp/WEB-INF";

    private static final String CONFIG_FILE = "ldapConfig.properties";

    private static final String ENCRYPTION_METHOD = "MD5";

    private static final String PROP_ATTR_MAP_PREFIX = "attribute.map.";

    private static final String PROP_LDAP_SERVER_URL = "ldapServerUrl";
    private static final String PROP_LDAP_AUTH_TYPE = "ldapAuthType";
    private static final String PROP_LDAP_BIND_USER = "ldapBindUser";
    private static final String PROP_LDAP_BIND_PASSWORD = "ldapBindPassword";

    private static final String LDAP_ORGANIZATION_WEBFILESYS = "ou=webfilesys,dc=maxcrc,dc=com";

    private static final String LDAP_ATTR_UID = "uid";
    private static final String LDAP_ATTR_COMMON_NAME = "cn";
    // private static final String LDAP_ATTR_LAST_LOGIN = "lastLogonTimestamp";
    private static final String LDAP_ATTR_LAST_LOGIN = "description"; // TODO:
                                                                      // change
    private static final String LDAP_ATTR_FIRST_NAME = "givenName";
    private static final String LDAP_ATTR_LAST_NAME = "sn";
    private static final String LDAP_ATTR_PASSWORD = "userPassword";
    private static final String LDAP_ATTR_LANGUAGE = "preferredLanguage";
    private static final String LDAP_ATTR_MAIL = "mail";
    private static final String LDAP_ATTR_PHONE = "telephoneNumber";
    // private static final String LDAP_ATTR_DISK_QUOTA = "quota";
    private static final String LDAP_ATTR_DISK_QUOTA = "title"; // TODO: change
    private static final String LDAP_ATTR_HOME_DIR = "homeDirectory";
    private static final String LDAP_ATTR_CSS = "description";
    private static final String LDAP_ATTR_USER_TYPE = "employeeType";
    private static final String LDAP_ATTR_READONLY = "readonly";
    private static final String LDAP_ATTR_PAGE_SIZE = "pageSize";

    private static final String LDAP_ATTR_GROUP_MEMBER = "memberUid";

    /** mapped LDAP attribute names for LDAP search */
    private String[] userAttribSet;

    /** unmapped LDAP attribute names for LDAP search */
    private static final String[] USER_ATTRIB_SET_KEYS = { LDAP_ATTR_UID, LDAP_ATTR_COMMON_NAME, LDAP_ATTR_FIRST_NAME, LDAP_ATTR_LAST_NAME, LDAP_ATTR_MAIL, LDAP_ATTR_PASSWORD,
            LDAP_ATTR_LANGUAGE, LDAP_ATTR_HOME_DIR, LDAP_ATTR_PHONE, LDAP_ATTR_DISK_QUOTA, LDAP_ATTR_LAST_LOGIN, LDAP_ATTR_CSS, LDAP_ATTR_USER_TYPE, LDAP_ATTR_READONLY,
            LDAP_ATTR_PAGE_SIZE };

    private static final String LDAP_GROUP_ADMIN = "admin";
    private static final String LDAP_GROUP_USER = "user";
    private static final String LDAP_GROUP_WEBSPACE = "webspace";
    private static final String LDAP_GROUP_BLOG = "blog";

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";
    private static final String ROLE_WEBSPACE = "webspace";
    private static final String ROLE_BLOG = "blog";

    private DirContext ctx = null;

    private Properties configProps;

    public LdapJndiUserMgr() {

        File ldapConfigFile = new File(CONFIG_PATH, CONFIG_FILE);

        if (ldapConfigFile.exists() && ldapConfigFile.isFile() && ldapConfigFile.canRead()) {

            FileReader fin = null;
            try {
                fin = new FileReader(ldapConfigFile);
                configProps = new Properties();
                configProps.load(fin);

                Hashtable<String, String> env = new Hashtable<String, String>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(Context.PROVIDER_URL, configProps.getProperty(PROP_LDAP_SERVER_URL));
                env.put(Context.SECURITY_AUTHENTICATION, configProps.getProperty(PROP_LDAP_AUTH_TYPE));
                env.put(Context.SECURITY_PRINCIPAL, configProps.getProperty(PROP_LDAP_BIND_USER));
                env.put(Context.SECURITY_CREDENTIALS, configProps.getProperty(PROP_LDAP_BIND_PASSWORD));

                fillUserAttribSet();

                try {
                    ctx = new InitialDirContext(env);
                } catch (NamingException ex) {
                    Logger.getLogger(getClass()).warn("failed to get initial context for LDAP access", ex);
                    ex.printStackTrace();
                }

            } catch (IOException ioex) {
                LOG.error("failed to load LADP configuration from " + ldapConfigFile.getAbsolutePath());
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                    }
                }
            }
        } else {
            LOG.error("LDAP config file is not a readable file: " + ldapConfigFile.getAbsolutePath());
        }

        /*
         * LdapJndiUser newUser = new LdapJndiUser("BieneMaja", "BieneMaja",
         * "Biene", "Maja", "ou=webfilesys", "BieneMaja@nowhere.com", "123456",
         * "234567"); try {
         * ctx.bind("uid=BieneMaja,ou=webfilesys,dc=maxcrc,dc=com", newUser); }
         * catch (NamingException ex) {
         * Logger.getLogger(getClass()).warn("failed to create new LDAP object",
         * ex); ex.printStackTrace(); }
         */

        standaloneTest();
    }

    private void fillUserAttribSet() {
        userAttribSet = new String[USER_ATTRIB_SET_KEYS.length];

        for (int i = 0; i < USER_ATTRIB_SET_KEYS.length; i++) {
            userAttribSet[i] = getLdapAttrName(USER_ATTRIB_SET_KEYS[i]);
        }
    }

    private String getLdapAttrName(String attrKey) {
        String mappedAttrName = configProps.getProperty(PROP_ATTR_MAP_PREFIX + attrKey);
        if (mappedAttrName != null) {
            return mappedAttrName;
        }
        return attrKey;
    }

    /**
     * List of userids of all users.
     */
    public ArrayList<String> getListOfUsers() {
        ArrayList<String> useridList = new ArrayList<String>();

        ArrayList<TransientUser> allUsers = getUserList();

        for (TransientUser user : allUsers) {
            useridList.add(user.getUserid());
        }

        return useridList;
    }

    public ArrayList<TransientUser> getUserList() {
        ArrayList<TransientUser> userList = new ArrayList<TransientUser>();

        /*
         * Attributes matchAttrs = new BasicAttributes(true); matchAttrs.put(new
         * BasicAttribute("uid", "BieneMaja"));
         */

        try {
            NamingEnumeration ldapList = ctx.list(LDAP_ORGANIZATION_WEBFILESYS);

            while (ldapList.hasMore()) {
                NameClassPair nc = (NameClassPair) ldapList.next();

                if (nc.getName().startsWith(LDAP_ATTR_UID)) {
                    Attributes answer = ctx.getAttributes(nc.getName() + "," + LDAP_ORGANIZATION_WEBFILESYS, userAttribSet);

                    NamingEnumeration attribs = answer.getAll();

                    TransientUser user = new TransientUser();

                    fillUserAttribs(user, attribs);

                    userList.add(user);
                }
            }
        } catch (NamingException ex) {
            LOG.error("LDAP search error", ex);
        }

        return userList;
    }

    private void fillUserAttribs(TransientUser user, NamingEnumeration attribs) {
        try {
            while (attribs.hasMoreElements()) {
                Attribute attr = (Attribute) attribs.next();

                String attribName = attr.getID();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("attrib name: " + attribName);
                }

                String attribValue = null;

                NamingEnumeration values = attr.getAll();
                if (values.hasMore()) {
                    Object o = values.next();
                    if (o instanceof String) {
                        attribValue = (String) o;

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("attrib value: " + attribValue);
                        }
                    }
                }

                if (attribValue != null) {
                    if (attribName.equals(getLdapAttrName(LDAP_ATTR_UID))) {
                        user.setUserid(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_FIRST_NAME))) {
                        user.setFirstName(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_LAST_NAME))) {
                        user.setLastName(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_MAIL))) {
                        user.setEmail(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_LANGUAGE))) {
                        user.setLanguage(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_HOME_DIR))) {
                        user.setDocumentRoot(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_PHONE))) {
                        user.setPhone(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_CSS))) {
                        user.setCss(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_READONLY))) {
                        user.setReadonly(attribValue.equalsIgnoreCase("true"));
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_USER_TYPE))) {
                        user.setUserType(attribValue);
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_DISK_QUOTA))) {
                        try {
                            user.setDiskQuota(Long.parseLong(attribValue));
                        } catch (Exception ex) {
                            LOG.error("invalid LDAP attribute value for disk quota: " + attribValue, ex);
                        }
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_PAGE_SIZE))) {
                        try {
                            user.setPageSize(Integer.parseInt(attribValue));
                        } catch (Exception ex) {
                            LOG.error("invalid LDAP attribute value for page size: " + attribValue, ex);
                        }
                    } else if (attribName.equals(getLdapAttrName(LDAP_ATTR_LAST_LOGIN))) {
                        try {
                            user.setLastLogin(new Date(Long.parseLong(attribValue)));
                        } catch (Exception ex) {
                            LOG.warn("invalid last login time value: " + attribValue);
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            LOG.error("failed to fill user attribs from LDAP", ex);
        }
    }

    /**
     * Update an existing user.
     * 
     * @param changedUser
     *            the data of the changed user
     * @exception UserMgmtException
     *                user could not be updated
     */
    public void updateUser(TransientUser changedUser) throws UserMgmtException {
        if (changedUser == null) {
            throw new IllegalArgumentException("parameter changedUser may not be null");
        }

        ModificationItem[] mods = new ModificationItem[12];

        BasicAttribute attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_LAST_NAME), changedUser.getLastName());
        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_FIRST_NAME), (changedUser.getFirstName() != null ? changedUser.getFirstName() : ""));
        mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_COMMON_NAME), (changedUser.getFirstName() != null ? changedUser.getFirstName() : "") + changedUser.getLastName());
        mods[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_HOME_DIR), changedUser.getDocumentRoot());
        mods[3] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_MAIL), changedUser.getEmail());
        mods[4] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_CSS), changedUser.getCss());
        mods[5] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_DISK_QUOTA), Long.toString(changedUser.getDiskQuota()));
        mods[6] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_LANGUAGE), changedUser.getLanguage());
        mods[7] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_PHONE), changedUser.getPhone());
        mods[8] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_LAST_LOGIN), (changedUser.getLastLogin() == null ? 0 : Long.toString(changedUser.getLastLogin().getTime())));
        mods[9] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_READONLY), Boolean.toString(changedUser.isReadonly()));
        mods[10] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        attrib = new BasicAttribute(getLdapAttrName(LDAP_ATTR_PAGE_SIZE), Integer.toString(changedUser.getPageSize()));
        mods[11] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        try {
            ctx.modifyAttributes("uid=" + changedUser.getUserid() + "," + LDAP_ORGANIZATION_WEBFILESYS, mods);
        } catch (NamingException ex) {
            LOG.error("failed to update LDAP user " + changedUser.getUserid(), ex);
        }
    }

    /**
     * When a user publishes a complete folder tree, a virtual user is created
     * that has read-only access and can be used for login by guests. The path
     * of the base folder is used as document root for the virtual user. The
     * virtual user exists only for a limited time. The generated userid of the
     * virtual user must be unique. It is the responsibility of the
     * InvitationManager to remove expired virtual users.
     * 
     * @param realUser
     *            the userid of the user that owns this folder
     * @param docRoot
     *            the path of the published folder, used as document root
     * @param expDays
     *            the time to live of the user in days
     * @param language
     *            language to use
     * @return the generated unique userid of the virtual user created
     */
    public String createVirtualUser(String realUser, String docRoot, String role, int expDays, String language) {
        return null;
    }

    public ArrayList<String> getAdminUserEmails() {
        // TODO Auto-generated method stub
        return null;
    }

    public ArrayList<String> getMailAddressesByRole(String receiverRole) {
        // TODO Auto-generated method stub
        return null;
    }

    public ArrayList<String> getAllMailAddresses() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean addUser(String userId) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean removeUser(String userId) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean userExists(String userId) {
        // TODO Auto-generated method stub
        return false;
    }

    public String createVirtualUser(String realUser, String docRoot, String role, int expDays) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setUserType(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_USER_TYPE), newValue);
    }

    /**
     * Get the type of the user.
     * 
     * @param userId
     *            the userid of the user
     * @return null if the user does not exist, "default" if the user type has
     *         not ben set
     */
    public String getUserType(String userId) {
        String userType = (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_USER_TYPE));
        if ((userType == null) || userType.isEmpty()) {
            return "default";
        }
        return userType;
    }

    public String getFirstName(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_FIRST_NAME));
    }

    public void setFirstName(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_FIRST_NAME), newValue);
    }

    public String getLastName(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_LAST_NAME));
    }

    public void setLastName(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_LAST_NAME), newValue);
    }

    public String getEmail(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_MAIL));
    }

    public void setEmail(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_MAIL), newValue);
    }

    public long getDiskQuota(String userId) {
        String diskQuota = (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_DISK_QUOTA));
        if (diskQuota == null) {
            return 0;
        }

        try {
            return Long.parseLong(diskQuota);
        } catch (NumberFormatException numEx) {
            LOG.error("invalid value for disk quota: " + diskQuota, numEx);
        }
        return 0;
    }

    public void setDiskQuota(String userId, long newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_DISK_QUOTA), Long.toString(newValue));
    }

    public int getPageSize(String userId) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setPageSize(String userId, int newValue) {
        // TODO Auto-generated method stub

    }

    public Date getLastLoginTime(String userId) {
        String lastLogin = (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_LAST_LOGIN));
        if (lastLogin == null) {
            return null;
        }
        try {
            return new Date(Long.parseLong(lastLogin));
        } catch (Exception ex) {
            LOG.error("invalid last login value: " + lastLogin);
        }
        return null;
    }

    public void setLastLoginTime(String userId, Date newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_LAST_LOGIN), Long.toString(newValue.getTime()));
    }

    public String getPhone(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_PHONE));
    }

    public void setPhone(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_PHONE), newValue);
    }

    public String getLanguage(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_LANGUAGE));
    }

    public void setLanguage(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_LANGUAGE), newValue);
    }

    public void setRole(String userId, String newRole) {
        // TODO Auto-generated method stub
    }

    public boolean isReadonly(String userId) {
        // TODO Auto-generated method stub
        return false;
    }

    public void setReadonly(String userId, boolean readonly) {
        // TODO Auto-generated method stub

    }

    public String getDocumentRoot(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_HOME_DIR));
    }

    public void setDocumentRoot(String userId, String newValue) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_HOME_DIR), newValue);
    }

    /*
     * private String encryptPassword(String password) { try { MessageDigest md
     * = MessageDigest.getInstance(ENCRYPTION_METHOD);
     * 
     * byte[] encryptedPassword = md.digest(password.getBytes());
     * 
     * sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
     * 
     * String encodedPassword = encoder.encodeBuffer(encryptedPassword).trim();
     * 
     * return encodedPassword; } catch (java.security.NoSuchAlgorithmException
     * nsaEx) { Logger.getLogger(getClass()).error(nsaEx); }
     * 
     * return ""; }
     */

    private byte[] encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ENCRYPTION_METHOD);

            return md.digest(password.getBytes());
        } catch (java.security.NoSuchAlgorithmException nsaEx) {
            Logger.getLogger(getClass()).error(nsaEx);
        }

        return null;
    }

    public void setPassword(String userId, String newPassword) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_PASSWORD), encryptPassword(newPassword));
    }

    public boolean checkPassword(String userId, String password) {

        byte[] storedPassword = (byte[]) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_PASSWORD));

        byte[] encryptedPassword = encryptPassword(password);

        int i = 0;

        for (; i < storedPassword.length; i++) {
            if (i >= encryptedPassword.length) {
                return false;
            }
            if (storedPassword[i] != encryptedPassword[i]) {
                return false;
            }
        }

        if (i < encryptedPassword.length) {
            return false;
        }
        return true;
    }

    public boolean checkReadonlyPassword(String userId, String password) {
        // TODO Auto-generated method stub
        return false;
    }

    public void setReadonlyPassword(String userId, String newPassword) {
        // TODO Auto-generated method stub

    }

    public String getCSS(String userId) {
        return (String) getSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_CSS));
    }

    public void setCSS(String userId, String newCSS) {
        setSingleValueUserAttribute(userId, getLdapAttrName(LDAP_ATTR_CSS), newCSS);
    }

    public ArrayList<TransientUser> getRealUsers() {
        // TODO Auto-generated method stub
        return null;
    }

    public void createUser(TransientUser newUser) throws UserMgmtException {
        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("uid", newUser.getUserid()));
        try {
            NamingEnumeration answer = ctx.search("ou=webfilesys,dc=maxcrc,dc=com", matchAttrs);

            if (answer.hasMoreElements()) {
                throw new UserMgmtException("LDAP User with userid " + newUser.getUserid() + " already exists");
            }

            System.out.println("user does not exist, will be created: " + newUser.getUserid());

            Attributes attrs = new BasicAttributes(true);
            Attribute objclass = new BasicAttribute("objectclass");
            objclass.add("top");
            objclass.add("inetOrgPerson");
            objclass.add("posixAccount");
            attrs.put(objclass);

            Attribute lastNameAttr = new BasicAttribute("sn");
            lastNameAttr.add(newUser.getLastName());
            attrs.put(lastNameAttr);

            Attribute cnAttr = new BasicAttribute("cn");
            cnAttr.add((newUser.getFirstName() != null ? (newUser.getFirstName() + " ") : "") + newUser.getLastName());
            attrs.put(cnAttr);

            Attribute uidNumberAttr = new BasicAttribute("uidNumber");
            uidNumberAttr.add("1");
            attrs.put(uidNumberAttr);

            Attribute gidNumberAttr = new BasicAttribute("gidNumber");
            gidNumberAttr.add("1");
            attrs.put(gidNumberAttr);

            Attribute homeDirAttr = new BasicAttribute("homeDirectory");
            homeDirAttr.add(newUser.getDocumentRoot());
            attrs.put(homeDirAttr);

            // Create the context
            Context result = ctx.createSubcontext("uid=" + newUser.getUserid() + "," + LDAP_ORGANIZATION_WEBFILESYS, attrs);
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
    }

    private void setSingleValueUserAttribute(String userid, String attribName, Object newVal) {
        ModificationItem[] mods = new ModificationItem[1];

        BasicAttribute attrib = new BasicAttribute(attribName, newVal);

        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrib);

        try {
            ctx.modifyAttributes("uid=" + userid + "," + LDAP_ORGANIZATION_WEBFILESYS, mods);
        } catch (NamingException ex) {
            ex.printStackTrace();

            System.out.println("creating new attribute " + attribName);

            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, attrib);
            try {
                ctx.modifyAttributes("uid=" + userid + "," + LDAP_ORGANIZATION_WEBFILESYS, mods);

                System.out.println("new attribute created: " + attribName);
            } catch (NamingException ex2) {
                ex2.printStackTrace();
            }
        }
    }

    private Object getSingleValueUserAttribute(String userid, String attribName) {
        try {
            Attributes attribs = getLdapUserAttributes(userid);

            if (attribs == null) {
                return null;
            }

            Attribute attrib = attribs.get(attribName);
            if (attrib != null) {
                NamingEnumeration values = attrib.getAll();
                if (values.hasMore()) {
                    return values.next();
                    /*
                     * if (o instanceof String) { return (String) o; } else {
                     * System.out.println("class of value: " +
                     * o.getClass().getName()); }
                     */
                }
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private Attributes getLdapUserAttributes(String userid) {
        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("uid", userid));
        try {
            NamingEnumeration answer = ctx.search(LDAP_ORGANIZATION_WEBFILESYS, matchAttrs);

            while (answer.hasMoreElements()) {
                SearchResult searchResult = (SearchResult) answer.nextElement();
                return searchResult.getAttributes();
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        new LdapJndiUserMgr();
    }

    /**
     * Get the user with the given userId.
     * 
     * @param userId
     *            the userid of the user
     * @return user object or null if not found
     */
    public TransientUser getUser(String userId) {
        TransientUser user = null;

        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("uid", userId));
        try {
            NamingEnumeration answer = ctx.search("ou=webfilesys,dc=maxcrc,dc=com", matchAttrs);

            if (answer.hasMoreElements()) {
                System.out.println("user found");

                user = new TransientUser();

                user.setUserid(userId);

                SearchResult searchResult = (SearchResult) answer.nextElement();
                Attributes attribs = searchResult.getAttributes();

                NamingEnumeration allAttribs = attribs.getAll();

                fillUserAttribs(user, allAttribs);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("LDAP search answer is empty");
                }
            }
        } catch (NamingException ex) {
            LOG.error("LDAP search error");
        }

        return user;
    }

    public String getRole(String userId) {
        if (isMemberOfGroup(userId, LDAP_GROUP_ADMIN)) {
            return ROLE_ADMIN;
        }
        if (isMemberOfGroup(userId, LDAP_GROUP_USER)) {
            return ROLE_USER;
        }
        if (isMemberOfGroup(userId, LDAP_GROUP_WEBSPACE)) {
            return ROLE_WEBSPACE;
        }
        if (isMemberOfGroup(userId, LDAP_GROUP_BLOG)) {
            return ROLE_BLOG;
        }
        return null;
    }

    private boolean isMemberOfGroup(String userId, String groupId) {
        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("cn", groupId));
        try {
            NamingEnumeration answer = ctx.search(LDAP_ORGANIZATION_WEBFILESYS, matchAttrs);

            if (answer.hasMoreElements()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("group " + groupId + " found");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("group " + groupId + " NOT found");
                }
            }

            while (answer.hasMoreElements()) {
                SearchResult searchResult = (SearchResult) answer.nextElement();
                Attributes attribs = searchResult.getAttributes();
                Attribute memberUidAttr = attribs.get(LDAP_ATTR_GROUP_MEMBER);
                if (memberUidAttr != null) {
                    NamingEnumeration values = memberUidAttr.getAll();
                    while (values.hasMore()) {
                        String value = (String) values.next();
                        if (value.equals(userId)) {
                            return true;
                        }
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("Attribute " + LDAP_ATTR_GROUP_MEMBER + " not found for group " + groupId);
                    }
                }
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public void activateUser(String activationCode) throws UserMgmtException {
        // TODO: implement this
    }

    private void testGetGroupMembers(String groupId) {
        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("cn", groupId));
        try {
            NamingEnumeration answer = ctx.search(LDAP_ORGANIZATION_WEBFILESYS, matchAttrs);

            if (answer.hasMoreElements()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("group " + groupId + " found");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("group " + groupId + " NOT found");
                }
            }

            while (answer.hasMoreElements()) {
                SearchResult searchResult = (SearchResult) answer.nextElement();
                Attributes attribs = searchResult.getAttributes();
                Attribute memberUidAttr = attribs.get(LDAP_ATTR_GROUP_MEMBER);
                if (memberUidAttr != null) {
                    NamingEnumeration values = memberUidAttr.getAll();
                    while (values.hasMore()) {
                        String value = (String) values.next();
                        System.out.println("member of group " + groupId + ": " + value);
                    }
                } else {
                    System.out.println("Attribute " + LDAP_ATTR_GROUP_MEMBER + " not found for group " + groupId);
                }
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
    }

    private void testModifyAttribute() {
        ModificationItem[] mods = new ModificationItem[1];

        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(getLdapAttrName(LDAP_ATTR_MAIL), "Biene.Maja@nowhere.com"));

        try {
            ctx.modifyAttributes("uid=BieneMaja" + "," + LDAP_ORGANIZATION_WEBFILESYS, mods);
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
    }

    private void testAddGroupMember(String groupId, String newUserId) {
        ModificationItem[] mods = new ModificationItem[1];

        mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(LDAP_ATTR_GROUP_MEMBER, newUserId));

        try {
            ctx.modifyAttributes("cn=" + groupId + "," + LDAP_ORGANIZATION_WEBFILESYS, mods);
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
    }

    private void testRemoveGroupMember(String groupId, String userId) {
        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("cn", groupId));
        try {
            NamingEnumeration answer = ctx.search(LDAP_ORGANIZATION_WEBFILESYS, matchAttrs);

            while (answer.hasMoreElements()) {
                SearchResult searchResult = (SearchResult) answer.nextElement();
                Attributes attribs = searchResult.getAttributes();
                Attribute memberUidAttr = attribs.get(LDAP_ATTR_GROUP_MEMBER);
                if (memberUidAttr != null) {
                    NamingEnumeration values = memberUidAttr.getAll();
                    while (values.hasMore()) {
                        String value = (String) values.next();
                        if (value.equals(userId)) {
                            memberUidAttr.remove(value);
                            ctx.modifyAttributes("cn=" + groupId + "," + LDAP_ORGANIZATION_WEBFILESYS, DirContext.REPLACE_ATTRIBUTE, attribs);
                            System.out.println("removed memberUid value " + userId);
                            return;
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }

        System.out.println("userId " + userId + " not found as member of group " + groupId);
    }

    private void testSearchGroupMembers(String groupName) {
        SearchControls ctls = new SearchControls();

        // String filter = "&(objectCategory=user)(memberOf=CN=" + groupName +
        // "," + LDAP_ORGANIZATION_WEBFILESYS + ")";
        String filter = "(memberof=cn=" + groupName + "," + LDAP_ORGANIZATION_WEBFILESYS + ")";

        System.out.println("filter: " + filter);

        try {
            NamingEnumeration answer = ctx.search(LDAP_ORGANIZATION_WEBFILESYS, filter, ctls);

            if (answer.hasMoreElements()) {
                System.out.println("anything found");
            } else {
                System.out.println("answer is empty");
            }

            while (answer.hasMoreElements()) {
                SearchResult searchResult = (SearchResult) answer.nextElement();
                Attributes attribs = searchResult.getAttributes();

                System.out.println("group " + groupName + " member: " + attribs.get("givenName") + " " + attribs.get("sn"));
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
    }

    private void debugUser(TransientUser user) {
        System.out.println("userid: " + user.getUserid());
        System.out.println("user: " + user.getFirstName() + " " + user.getLastName());
        System.out.println("documentRoot: " + user.getDocumentRoot());
        System.out.println("email: " + user.getEmail());
        System.out.println("css: " + user.getCss());
        System.out.println("last login: " + user.getLastLogin());
        System.out.println("readonly: " + user.isReadonly());
        System.out.println("page size: " + user.getPageSize());
    }

    private void standaloneTest() {
        // testSearchUser("BieneMaja");

        // testGetGroupMembers("admin");
        // testGetGroupMembers("user");
        // testGetGroupMembers("webspace");

        // testModifyAttribute();

        // testAddGroupMember("user", "BieneMaja");
        // testAddGroupMember("user", "admin");

        // testRemoveGroupMember("user", "BieneMaja");
        // testRemoveGroupMember("user", "admin");

        /*
         * setLastLoginTime("BieneMaja", new Date());
         * 
         * setCSS("BieneMaja", "sunset");
         */

        /*
         * System.out.println("list of users: "); for (TransientUser user :
         * getUserList()) { debugUser(user); }
         */

        /*
         * System.out.println("first name of user BieneMaja: " +
         * getSingleValueUserAttribute("BieneMaja", "givenName"));
         * 
         * setSingleValueUserAttribute("BieneMaja", "givenName",
         * "Bienchenlein");
         * 
         * System.out.println("new first name of user BieneMaja: " +
         * getSingleValueUserAttribute("BieneMaja", "givenName"));
         */

        /*
         * setPassword("BieneMaja", "geheim");
         * 
         * System.out.println("ckeckPassword wrong: " +
         * checkPassword("BieneMaja", "schnulli"));
         * System.out.println("ckeckPassword ok: " + checkPassword("BieneMaja",
         * "geheim"));
         */

        /*
         * TransientUser copySource = testSearchUser("BieneMaja");
         * 
         * copySource.setUserid("DagobertDuck");
         * copySource.setFirstName("Dagobert"); copySource.setLastName("Duck");
         * 
         * try { addUser(copySource); } catch (DuplicateUserException ex) {
         * ex.printStackTrace(); }
         */

        /*
         * Vector webfilesysUsers = getListOfUsers();
         * 
         * System.out.println("list of users: "); for (int i = 0; i <
         * webfilesysUsers.size(); i++) { TransientUser user = (TransientUser)
         * webfilesysUsers.get(i); System.out.println("user: " +
         * user.getFirstName() + " " + user.getLastName());
         * System.out.println("css: " + user.getCss());
         * System.out.println("last login: " + user.getLastLogin()); }
         */

        /*
         * System.out.println("role of user BieneMaja: " +
         * getRole("BieneMaja")); System.out.println("role of user admin: " +
         * getRole("admin"));
         */

        /*
         * ArrayList<String> useridList = this.getListOfUsers(); for (String
         * userid : useridList) { System.out.println("user: " + userid); }
         */

        TransientUser changedUser = getUser("BieneMaja");

        changedUser.setFirstName("Biene-4");
        changedUser.setLastName("Maja-4");
        changedUser.setEmail("Biene.Maja-4@nowhere.com");
        changedUser.setCss("fmweb");
        changedUser.setDocumentRoot("/home/BieneMaja");
        changedUser.setLanguage("espanol");
        changedUser.setLastLogin(new Date());
        changedUser.setPhone("12121212");
        changedUser.setRole("webspace");
        changedUser.setDiskQuota(100000l);
        changedUser.setReadonly(true);
        changedUser.setPageSize(24);

        try {
            updateUser(changedUser);
        } catch (UserMgmtException ex) {
            LOG.error(ex);
        }

        TransientUser user = getUser("BieneMaja");
        debugUser(user);
    }

}
