package com.monochromeroad.grails.plugins.cookiesession;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    private transient Logger log = LoggerFactory.getLogger(getClass());

    private transient String name;

    private long creationTime;

    private long lastAccessedTime;

    private boolean valid;

    /** the time for Session Timeout */
    private transient int maxInactiveInterval = 30 * 60;

    private transient ServletContext servletContext;

    private transient HttpServletRequest servletRequest;

    private transient HttpServletResponse servletResponse;

    private transient ClassLoader classLoader;

    private transient boolean newSession;

    private Map<String, Serializable> attributes;

    public CookieSession() {
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.attributes = new HashMap<String, Serializable>();
        this.newSession = true;
        this.valid = true;
    }

    public CookieSession(
            String name, ServletContext context,
            HttpServletRequest request, HttpServletResponse response,  ClassLoader classLoader) {
        this();
        this.name = name;
        this.servletContext = context;
        this.servletRequest = request;
        this.servletResponse = response;
        this.classLoader = classLoader;
        load();
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * You don't need a session id.
     * it returns a dummy id just in case.
     *
     * @return a dummy id
     */
    @Override
    public String getId() {
        return name;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @SuppressWarnings("deprecation") // For Servlet API Spec
    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return SESSION_CONTEXT;
    }

    @Override
    public Object getAttribute(String s) {
        if (!isValidSession()) {
            return null;
        }
        this.lastAccessedTime = System.currentTimeMillis();
        return this.attributes.get(s);
    }

    @Override
    public Object getValue(String s) {
        return getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames() {
        Map<String, Serializable> currentAttributes = new HashMap<String, Serializable>();
        if (isValidSession()) {
            currentAttributes = attributes;
        }
        final Iterator<String> names = currentAttributes.keySet().iterator();
        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {return names.hasNext();}
            @Override
            public String nextElement() {return names.next();}
        };
    }

    @Override
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
    @Override
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
        save();
    }

    @Override
    public void putValue(String s, Object o) {
        setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        if (attributes.containsKey(s)) {
            attributes.remove(s);
            save();
        }
    }

    @Override
    public void removeValue(String s) {
        removeAttribute(s);
    }

    @Override
    public void invalidate() {
        this.valid = false;
        this.attributes.clear();
        delete();
    }

    @Override
    public boolean isNew() {
        return newSession;
    }

    private boolean isValidSession() {
        return valid && !isSessionTimeout();
    }

    void delete() {
        Cookie sessionCookie = createCookie();
        sessionCookie.setMaxAge(0); // Delete
        servletResponse.addCookie(sessionCookie);
    }

    private void save() {
        String sessionString = convertToString();
        Cookie sessionCookie = createCookie();
        sessionCookie.setValue(sessionString);
        servletResponse.addCookie(sessionCookie);
    }

    private String convertToString() {
        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            OutputStream compressOut = new GZIPOutputStream(byteArrayOut);
            ObjectOutputStream objectOut = new ObjectOutputStream(compressOut);
            try {
                objectOut.writeObject(this);
                objectOut.flush();
            } finally {
                objectOut.close();
            }
            return encode(byteArrayOut.toByteArray());
        } catch (final IOException e) {
            log.error("Cannot serialize a session.", e);
            return "-";
        }
    }

    private String encode(final byte[] serializedBytes) {
        return new Base64(-1, null, true).encodeToString(serializedBytes);
    }

    private void load() {
        Cookie cookie = findCookie();
        if (cookie == null) {
            return;
        }

        CookieSession cookieSession = readCookieSession(cookie);
        this.creationTime = cookieSession.creationTime;
        this.lastAccessedTime = cookieSession.lastAccessedTime;
        this.attributes = cookieSession.attributes;
        this.newSession = false;
    }

    private CookieSession readCookieSession(Cookie cookie) {
        ObjectInputStream objectIn = getCookieSessionInputStream(cookie);
        CookieSession result = null;
        try {
            result = (CookieSession) objectIn.readObject();
        } catch (IOException e) {
            log.warn("exception on reading a cookie session", e);
        } catch (ClassNotFoundException e) {
            log.warn("exception on reading a cookie session", e);
        }
        if (result != null && result.isSessionTimeout()) {
            return result;
        } else {
            return null;
        }
    }

    private ObjectInputStream getCookieSessionInputStream(Cookie sessionCookie) {
        String encoded = sessionCookie.getValue();
        byte[] decoded = decode(encoded);
        try {
            InputStream loadingStream = new GZIPInputStream(new ByteArrayInputStream(decoded));
            return new ObjectInputStream(loadingStream) {
                @Override
                public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    //noinspection GroovyUnusedCatchParameter
                    try {
                        return classLoader.loadClass(desc.getName());
                    } catch (ClassNotFoundException ex) {
                        return Class.forName(desc.getName());
                    }
                }
            };
        } catch (final ClassCastException e) {
            log.warn("exception on loading a cookie session", e);
        } catch (final IOException e) {
            log.warn("exception on loading a cookie session", e);
        }
        return null;
    }

    private Cookie findCookie() {
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies == null) return null;
        for (final Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie;
        }
        return null;
    }

    private byte[] decode(final String sessionData) {
        return Base64.decodeBase64(sessionData);
    }

    private boolean isSessionTimeout() {
        if (maxInactiveInterval < 0) {
            return false;
        }

        long timeout = System.currentTimeMillis() - getLastAccessedTime();
        return timeout - maxInactiveInterval * 1000 > 0;
    }

    private Cookie createCookie() {
        final Cookie result = new Cookie(name, "-");
        result.setDomain(servletRequest.getServerName()); // TODO needs config option
        result.setPath("/");
        return result;
    }
}

