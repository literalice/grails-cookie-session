package com.monochromeroad.grails.plugins.cookiesession;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers a request wrapper that intercepts getSession() calls and returns a cookie-backed implementation.
 *
 * @author Masatoshi Hayashi
 */
class SessionProxyFilter extends OncePerRequestFilter {

    private static final String DEFAULT_SESSION_ID = "gssession";

    private static final int DEFAULT_SESSION_TIMEOUT = 30;

    private static final String DEFAULT_HMAC_ID = "gsesshmac";

    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";

    private GrailsApplication grailsApplication;

    private String sessionId;

    private int sessionTimeoutSecond;

    private Map<String, String> hmapOption;

    @SuppressWarnings("unused") // For Spring
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    protected void initFilterBean() {
        hmapOption = loadHmapOption();
        sessionId = loadSessionId();
        sessionTimeoutSecond = loadSessionTimeout();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        CookieSessionRequestWrapper wrapper = new CookieSessionRequestWrapper(request, response);
        chain.doFilter(wrapper, response);
    }

    private int loadSessionTimeout() {
        Map<String, Object> config = grailsApplication.getFlatConfig();
        Integer timeout = (Integer)config.get("grails.plugin.cookiesession.timeout");
        if (timeout == null) {
            return DEFAULT_SESSION_TIMEOUT * 60;
        } else {
            return timeout;
        }
    }

    private String loadSessionId() {
        Map<String, Object> config = grailsApplication.getFlatConfig();
        String id = (String)config.get("grails.plugin.cookiesession.id");
        if (StringUtils.isBlank(id)) {
            return DEFAULT_SESSION_ID;
        } else {
            return id;
        }
    }

    private Map<String, String> loadHmapOption() {
        Map<String, Object> config = grailsApplication.getFlatConfig();
        String hmacConfigKey = "grails.plugin.cookiesession.hmac.";

        String hmacSecret = (String)config.get(hmacConfigKey + "secret");
        if (StringUtils.isBlank(hmacSecret)) {
            throw new IllegalStateException("HMAC secret key not defined.");
        }

        String hmacId = (String)config.get(hmacConfigKey + "id");
        if (StringUtils.isBlank(hmacId)) {
            hmacId = DEFAULT_HMAC_ID;
        }
        String hmacAlgorithm = (String)config.get(hmacConfigKey + "algorithm");
        if (StringUtils.isBlank(hmacAlgorithm)) {
            hmacAlgorithm = DEFAULT_HMAC_ALGORITHM;
        }

        Map<String, String> option = new HashMap<String, String>();
        option.put("id", hmacId);
        option.put("algorithm", hmacAlgorithm);
        option.put("secret", hmacSecret);
        return option;
    }

    public class CookieSessionRequestWrapper extends HttpServletRequestWrapper {

        private SessionRepository repository;

        private CookieSession cookieSession;

        private CookieSessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            GrailsApplication app = SessionProxyFilter.this.grailsApplication;
            String sessionId = SessionProxyFilter.this.sessionId;
            Map<String, String> hmapOption = SessionProxyFilter.this.hmapOption;
            SessionSerializer serializer = new SessionSerializer(app);
            repository = new SessionRepository(sessionId, request, response, serializer, hmapOption);

            cookieSession = repository.find(SessionProxyFilter.this.sessionTimeoutSecond);
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        @Override
        public HttpSession getSession(final boolean create) {
            if (cookieSession == null && create) {
                cookieSession = new CookieSession(repository);
            }

            if (cookieSession != null) {
                cookieSession.maxInactiveInterval = SessionProxyFilter.this.sessionTimeoutSecond;
            }
            return cookieSession;
        }

    }
}
