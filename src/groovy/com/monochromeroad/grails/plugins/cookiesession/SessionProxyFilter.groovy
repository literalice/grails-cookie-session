package com.monochromeroad.grails.plugins.cookiesession

import javax.servlet.FilterChain
import javax.servlet.http.*

import org.springframework.web.filter.OncePerRequestFilter

/**
 * Registers a request wrapper that intercepts getSession() calls and returns a cookie-backed implementation.
 *
 * @author Masatoshi Hayashi
 */
class SessionProxyFilter extends OncePerRequestFilter {

    private static final String DEFAULT_SESSION_ID = "gssession"

    private static final String DEFAULT_HMAC_ID = "gsesshmac"

    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1"

    def grailsApplication

    def hmapOption

    def sessionId

    @Override
    protected void initFilterBean() {
        hmapOption = loadHmapOption()
        sessionId = loadSessionId()
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        CookieSessionRequestWrapper wrapper = new CookieSessionRequestWrapper(request, response)
        chain.doFilter(wrapper, response)
    }

    private loadSessionId() {
        def config = grailsApplication.config.grails.plugin.cookiesession
        (config.id) ?: DEFAULT_SESSION_ID
    }

    private loadHmapOption() {
        def config = grailsApplication.config.grails.plugin.cookiesession.hmac
        if (config.secret) {
            def hmacSecret = config.secret
            def hmacId = (config.id) ?: DEFAULT_HMAC_ID
            def hmacAlgorithm = (config.algorithm) ?: DEFAULT_HMAC_ALGORITHM
            [id: hmacId, algorithm: hmacAlgorithm, secret: hmacSecret]
        } else {
            throw new IllegalStateException("HMAC secret key not defined.")
        }
    }

    private class CookieSessionRequestWrapper extends HttpServletRequestWrapper {

        private SessionRepository repository

        private CookieSession cookieSession

        CookieSessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request)
            def app = SessionProxyFilter.this.grailsApplication
            def sessionId = SessionProxyFilter.this.sessionId
            def hmapOption = SessionProxyFilter.this.hmapOption
            SessionSerializer serializer = new SessionSerializer(app)
            repository = new SessionRepository(sessionId, request, response, serializer, hmapOption)
        }

        @Override
        HttpSession getSession() {
            getSession(true)
        }

        @Override
        HttpSession getSession(final boolean create) {
            if (cookieSession) {
                return cookieSession
            }

            cookieSession = repository.find()
            if (cookieSession) {
                return cookieSession
            }

            if (create) {
                return new CookieSession(repository)
            } else {
                return null
            }
        }

    }
}
