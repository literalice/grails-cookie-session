package com.monochromeroad.grails.plugins.cookiesession

import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream
import org.slf4j.LoggerFactory
import org.slf4j.Logger

/**
 * Session Serializer
 *
 * @author Masatoshi Hayashi
 */
class SessionSerializer {

    def grailsApplication

    private Logger log = LoggerFactory.getLogger(getClass());

    SessionSerializer(grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    String serialize(Serializable serializable) {
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream()
        //noinspection GroovyMissingReturnStatement
        new GZIPOutputStream(byteArrayOut).withObjectOutputStream {
            it.writeObject(serializable)
        }
        byteArrayOut.toByteArray().encodeBase64()
    }

    Object deserialize(String source) {
        byte[] decoded = source.decodeBase64()

        try {
            ObjectInputStream stream = getSessionInputStream(decoded)
            return stream.readObject()
        } catch (Exception e) {
            log.warn("exception on reading a cookie session", e)
            return null
        }
    }

    private ObjectInputStream getSessionInputStream(byte[] source) {
        InputStream loadingStream = new GZIPInputStream(new ByteArrayInputStream(source))
        return new ObjectInputStream(loadingStream) {
            @Override
            public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                //noinspection GroovyUnusedCatchParameter
                try {
                    return grailsApplication.classLoader.loadClass(desc.getName())
                } catch (ClassNotFoundException ex) {
                    return Class.forName(desc.getName())
                }
            }
        }
    }
}
