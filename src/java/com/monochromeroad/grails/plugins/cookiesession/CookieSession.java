package com.monochromeroad.grails.plugins.cookiesession;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cookie Session
 */
public class CookieSession  implements HttpSession, Serializable {

    private static final long serialVersionUID = 6663229517649651010L;

    @SuppressWarnings("deprecation") /* ServletAPI */
    private static final javax.servlet.http.HttpSessionContext SESSION_CONTEXT = new javax.servlet.http.HttpSessionContext() {
        public HttpSession getSession(String sessionId) {
            return null;
        }
        public Enumeration<String> getIds() {
            return SESSION_CONTEXT_ID_ENUM;
        }
    };

    private static final Enumeration<String> SESSION_CONTEXT_ID_ENUM = new Enumeration<String>() {
        public String nextElement() {
            return null;
        }
        public boolean hasMoreElements() {
            return false;
        }
    };

    transient SessionRepository repository;

    long creationTime;

    long lastAccessedTime;

    boolean valid;

    /** the time for Session Timeout */
    transient int maxInactiveInterval;

    transient ServletContext servletContext;

    transient boolean newSession;

    Map<String, Serializable> attributes;

    public CookieSession() {
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.attributes = new HashMap<String, Serializable>();
        this.newSession = true;
        this.valid = true;
    }

    public CookieSession(SessionRepository repository) {
        this();
        this.repository = repository;
    }

    public long getCreationTime() {
        return creationTime;
    }

    /**
     * You don't need a session id.
     * it returns a dummy id just in case.
     *
     * @return a dummy id
     */
    public String getId() {
        return "-";
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @SuppressWarnings("deprecation") // For Servlet API Spec
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return SESSION_CONTEXT;
    }

    public Object getAttribute(String s) {
        if (!isValidSession()) {
            return null;
        }
        this.lastAccessedTime = System.currentTimeMillis();
        return this.attributes.get(s);
    }

    public Object getValue(String s) {
        return getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        Map<String, Serializable> currentAttributes = new HashMap<String, Serializable>();
        if (isValidSession()) {
            currentAttributes = attributes;
        }
        final Iterator<String> names = currentAttributes.keySet().iterator();
        return new Enumeration<String>() {
            public boolean hasMoreElements() {return names.hasNext();}
            public String nextElement() {return names.next();}
        };
    }

    public String[] getValueNames() {
        Map<String, Serializable> currentAttributes = new HashMap<String, Serializable>();
        if (isValidSession()) {
            currentAttributes = attributes;
        }
        return currentAttributes.keySet().toArray(new String[attributes.size()]);
    }

    /**
     * Sets attribute.
     * @param s a value name
     * @param o It should be serializable.
     */
    public void setAttribute(String s, Object o) {
        if (s == null) {
            throw new IllegalArgumentException("Session's key should not be null.");
        }
        if(o == null) {
            removeAttribute(s);
        }

        Serializable validValue;
        if (o instanceof Serializable) {
            validValue = (Serializable)o;
        } else {
            throw new IllegalArgumentException(("Session Object should be serializable."));
        }

        attributes.put(s, validValue);
        repository.save(this);
    }

    public void putValue(String s, Object o) {
        setAttribute(s, o);
    }

    public void removeAttribute(String s) {
        if (attributes.containsKey(s)) {
            attributes.remove(s);
            repository.save(this);
        }
    }

    public void removeValue(String s) {
        removeAttribute(s);
    }

    public void invalidate() {
        this.valid = false;
        this.attributes.clear();
        repository.delete();
    }

    public boolean isNew() {
        return newSession;
    }

    private boolean isValidSession() {
        return valid && !isSessionTimeout();
    }

    private boolean isSessionTimeout() {
        if (maxInactiveInterval < 0) {
            return false;
        }

        long meantime = System.currentTimeMillis() - getLastAccessedTime();
        boolean isTimeout = meantime - maxInactiveInterval * 1000 > 0;
        if(isTimeout) {
            invalidate();
        }
        return isTimeout;
    }

}

