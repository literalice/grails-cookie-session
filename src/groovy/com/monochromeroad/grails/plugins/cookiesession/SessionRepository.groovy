package com.monochromeroad.grails.plugins.cookiesession

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

/**
 * Created with IntelliJ IDEA.
 * User: masatoshi
 * Date: 12/03/02
 * Time: 3:30
 * To change this template use File | Settings | File Templates.
 */
class SessionRepository {

    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";

    /** HMAC SecretKey for detecting interpolate */
    private SecretKey hmacSecretKey;

    String key = "session-cookie"

    HttpServletRequest request

    HttpServletResponse response

    SessionSerializer serializer

    SessionRepository(HttpServletRequest request, HttpServletResponse response, SessionSerializer serializer) {
        this.request = request
        this.response = response
        this.serializer = serializer

        final String base64EncodedHmacSecretKey = ""
        hmacSecretKey = createSecretKey(DEFAULT_HMAC_ALGORITHM, base64EncodedHmacSecretKey)
    }

    CookieSession find() {
        Cookie sessionCookie = findCookie(key)
        Cookie hmacCookie = findCookie(key + "_hmac")
        if (sessionCookie && hmacCookie) {
            if (!isValidHmac(sessionCookie.value, hmacCookie.value)) {
                delete()
                return null
            }
            CookieSession session = new CookieSession(this)
            CookieSession beforeSession = serializer.deserialize(sessionCookie.value) as CookieSession
            session.creationTime = beforeSession.creationTime
            session.lastAccessedTime = beforeSession.lastAccessedTime
            session.attributes = beforeSession.attributes
            session.newSession = false
            return session
        } else {
            return null
        }
    }

    void save(CookieSession session) {
        String serialized = serializer.serialize(session)
        Cookie cookie = createCookie(key)
        cookie.setValue(serialized)
        response.addCookie(cookie)
        Cookie hmacCookie = createCookie(key + "_hmac")
        hmacCookie.setValue(encode(calculateHmac(serialized)))
        response.addCookie(hmacCookie)
    }

    void delete() {
        Cookie sessionCookie = createCookie(key);
        sessionCookie.setMaxAge(0); // Delete
        response.addCookie(sessionCookie);
        Cookie hmacCookie = createCookie(key + "_hmac");
        hmacCookie.setMaxAge(0); // Delete
        response.addCookie(hmacCookie);
    }

    private Cookie createCookie(String key) {
        final Cookie result = new Cookie(key, "-");
        result.setDomain(request.getServerName()); // TODO needs config option
        result.setPath("/");
        return result;
    }

    private Cookie findCookie(key) {
        Cookie[] cookies = request.getCookies()
        if (cookies == null) {
            return null
        }
        for (Cookie cookie in cookies) {
            if (cookie.getName().equals(key)) {
                return cookie
            }
        }
        return null
    }

    private byte[] calculateHmac(final String value) {
        final Mac mac = Mac.getInstance(hmacSecretKey.getAlgorithm());
        mac.init(hmacSecretKey);
        return mac.doFinal(value.getBytes());
    }

    private boolean isValidHmac(final String value, final String hmac) {
        final byte[] result = calculateHmac(value);
        final byte[] input = decode(hmac);
        return Arrays.equals(result, input);
    }

    protected SecretKey createSecretKey(final String algorithm, final String base64encodedKey) {
        final byte[] keyBytes = decode(base64encodedKey);
        return new SecretKeySpec(keyBytes, algorithm);
    }

    private byte[] decode(final String data) {
        return Base64.decodeBase64(data);
    }

    private String encode(final byte[] data) {
        new Base64(-1, null, true).encodeToString(data);
    }
}
