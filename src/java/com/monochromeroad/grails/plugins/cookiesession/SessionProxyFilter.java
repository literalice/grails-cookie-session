package com.monochromeroad.grails.plugins.cookiesession;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;

import org.codehaus.groovy.grails.commons.GrailsApplication;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Registers a request wrapper that intercepts getSession() calls and returns a cookie-backed implementation.
 *
 * @author Masatoshi Hayashi
 */
public class SessionProxyFilter extends OncePerRequestFilter {

    private GrailsApplication grailsApplication;

    protected String applicationName = "cookie_session";

    @SuppressWarnings("UnusedDeclaration") // For Spring
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        CookieSessionRequestWrapper wrapper = new CookieSessionRequestWrapper(request, response);
        chain.doFilter(wrapper, response);
    }

    private class CookieSessionRequestWrapper extends HttpServletRequestWrapper {

        private HttpServletResponse response;

        public CookieSessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.response = response;
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        @Override
        public HttpSession getSession(final boolean create) {
            CookieSession cookieSession = new CookieSession(
                    applicationName, getServletContext(),(HttpServletRequest)getRequest(), response, grailsApplication.getClassLoader());
            if (cookieSession.isNew() && !create) {
                cookieSession.invalidate();
                return null;
            }
            return cookieSession;
        }

    }
}
