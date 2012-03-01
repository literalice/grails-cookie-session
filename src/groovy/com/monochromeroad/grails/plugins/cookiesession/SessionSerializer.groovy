package com.monochromeroad.grails.plugins.cookiesession

import java.util.zip.GZIPOutputStream

import org.apache.commons.codec.binary.Base64
import java.util.zip.GZIPInputStream
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Created with IntelliJ IDEA.
 * User: masatoshi
 * Date: 12/03/02
 * Time: 2:20
 * To change this template use File | Settings | File Templates.
 */
class SessionSerializer {

    GrailsApplication grailsApplication

    private Logger log = LoggerFactory.getLogger(getClass());

    SessionSerializer(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    String serialize(Serializable serializable) {
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        //noinspection GroovyMissingReturnStatement
        new GZIPOutputStream(byteArrayOut).withObjectOutputStream {
            it.writeObject(serializable)
        }
        encode(byteArrayOut.toByteArray())
    }

    Object deserialize(String source) {
        byte[] decoded = decode(source);

        try {
            ObjectInputStream stream = getSessionInputStream(decoded)
            return stream.readObject();
        } catch (Exception e) {
            log.warn("exception on reading a cookie session", e);
            return null
        }
    }

    private ObjectInputStream getSessionInputStream(byte[] source) {
        InputStream loadingStream = new GZIPInputStream(new ByteArrayInputStream(source));
        return new ObjectInputStream(loadingStream) {
            @Override
            public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                //noinspection GroovyUnusedCatchParameter
                try {
                    return grailsApplication.classLoader.loadClass(desc.getName());
                } catch (ClassNotFoundException ex) {
                    return Class.forName(desc.getName());
                }
            }
        };
    }

    private byte[] decode(final String sessionData) {
        return Base64.decodeBase64(sessionData);
    }

    private String encode(final byte[] serializedBytes) {
        new Base64(-1, null, true).encodeToString(serializedBytes);
    }

}
