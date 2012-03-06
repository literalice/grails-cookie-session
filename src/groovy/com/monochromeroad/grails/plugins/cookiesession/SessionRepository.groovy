package com.monochromeroad.grails.plugins.cookiesession

import javax.crypto.SecretKey
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

/**
 * Session Cookie Repository
 *
 * @author Masatoshi Hayashi
 */
class SessionRepository {

    /** HMAC SecretKey for detecting interpolate */
    SecretKey hmacSecretKey

    String key

    String hmacKey

    HttpServletRequest request

    HttpServletResponse response

    SessionSerializer serializer

    SessionRepository(String key,
            HttpServletRequest request, HttpServletResponse response,
            SessionSerializer serializer, Map hmacOption) {
        this.key = key

        this.request = request
        this.response = response
        this.serializer = serializer

        hmacKey = hmacOption["id"]
        String hmacAlgorithm = hmacOption["algorithm"]
        String hmacSecret = hmacOption["secret"]
        hmacSecretKey = createSecretKey(hmacAlgorithm, hmacSecret)
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    CookieSession find(int maxInterval) {
        Cookie sessionCookie = findCookie(key)
        Cookie hmacCookie = findCookie(hmacKey)
        if (sessionCookie && hmacCookie) {
            if (!isValidHmac(sessionCookie.value, hmacCookie.value)) {
                return null
            }
            CookieSession session = new CookieSession(this)
            CookieSession beforeSession = serializer.deserialize(sessionCookie.value) as CookieSession
            beforeSession.repository = this
            if (beforeSession && !beforeSession.isSessionTimeout(maxInterval)) {
                session.creationTime = beforeSession.creationTime
                session.lastAccessedTime = System.currentTimeMillis()
                session.attributes = beforeSession.attributes
                session.newSession = false
                return session
            } else {
                return null
            }
        } else {
            return null
        }
    }

    void save(CookieSession session) {
        if (session) {
            String serialized = serializer.serialize(session)
            Cookie cookie = createCookie(key)
            cookie.setValue(serialized)
            response.addCookie(cookie)
            Cookie hmacCookie = createCookie(hmacKey)
            hmacCookie.setValue(calculateHmac(serialized).encodeBase64().toString())
            response.addCookie(hmacCookie)
        }
    }

    void delete() {
        Cookie sessionCookie = createCookie(key)
        sessionCookie.setMaxAge(0) // Delete
        response.addCookie(sessionCookie)
        Cookie hmacCookie = createCookie(hmacKey)
        hmacCookie.setMaxAge(0)// Delete
        response.addCookie(hmacCookie)
    }

    private Cookie createCookie(String key) {
        Cookie result = new Cookie(key, "")
        result.setPath("/")
        return result
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
        Mac mac = Mac.getInstance(hmacSecretKey.getAlgorithm())
        mac.init(hmacSecretKey)
        return mac.doFinal(value.bytes)
    }

    private boolean isValidHmac(final String value, final String hmac) {
        if (value && hmac) {
            byte[] result = calculateHmac(value)
            byte[] input = hmac.decodeBase64()
            Arrays.equals(result, input)
        } else {
            false
        }
    }

    protected SecretKey createSecretKey(final String algorithm, final String base64encodedKey) {
        assert base64encodedKey

        byte[] keyBytes = base64encodedKey.decodeBase64()
        return new SecretKeySpec(keyBytes, algorithm)
    }

}
