package com.monochromeroad.grails.plugins.cookiesession

import javax.crypto.SecretKey
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

import org.apache.commons.codec.binary.Base64

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

    CookieSession find() {
        Cookie sessionCookie = findCookie(key)
        Cookie hmacCookie = findCookie(hmacKey)
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
        if (session) {
            String serialized = serializer.serialize(session)
            Cookie cookie = createCookie(key)
            cookie.setValue(serialized)
            response.addCookie(cookie)
            Cookie hmacCookie = createCookie(hmacKey)
            hmacCookie.setValue(encode(calculateHmac(serialized)))
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
            byte[] input = decode(hmac)
            Arrays.equals(result, input)
        } else {
            false
        }
    }

    protected SecretKey createSecretKey(final String algorithm, final String base64encodedKey) {
        assert base64encodedKey

        byte[] keyBytes = decode(base64encodedKey)
        return new SecretKeySpec(keyBytes, algorithm)
    }

    private byte[] decode(final String data) {
        return Base64.decodeBase64(data)
    }

    private String encode(final byte[] data) {
        new Base64(-1, null, true).encodeToString(data)
    }
}
