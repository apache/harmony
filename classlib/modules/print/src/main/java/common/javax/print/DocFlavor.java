/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.print;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.harmony.x.print.MimeType;

/** 
 * Encapsulates an object that specifies the format to a DocPrintJob print 
 * data.
 * @author Irina A. Arkhipets
 */
public class DocFlavor implements Serializable, Cloneable {
    private static final long serialVersionUID = -4512080796965449721L;

    public static final String hostEncoding;
    static {
        /*
         * Host encoding. Need to change it to "hostEncoding =
         * Charset.defaultCharset().toString"
         */
        hostEncoding = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("file.encoding");
            }
        });
    }

    // Document media type (as defined in RFC 2045,2046, 822)
    private transient MimeType aType;

    private final String aClass; // Representation class name

    public DocFlavor(String mimeType, String className) {
        if ((mimeType == null) || (className == null)) {
            throw new NullPointerException();
        }
        aType = new MimeType(mimeType);
        aClass = className;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof DocFlavor)
                && (getCanonicalForm().equals(((DocFlavor) obj).getCanonicalForm()));
    }

    public String getMediaSubtype() {
        return aType.getSubtype();
    }

    public String getMediaType() {
        return aType.getType();
    }

    public String getMimeType() {
        return aType.toString();
    }

    public String getParameter(String paramName) {
        if (paramName == null) {
            throw new NullPointerException();
        }
        return aType.getParameter(paramName);
    }

    public String getRepresentationClassName() {
        return aClass;
    }

    @Override
    public int hashCode() {
        return getCanonicalForm().hashCode();
    }

    @Override
    public String toString() {
        return getCanonicalForm();
    }

    // --------------------------------------------------------------
    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        aType = new MimeType((String) (s.readObject()));
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(aType.getCanonicalForm());
    }

    private String getCanonicalForm() {
        return aType.toString() + "; class=\"" + aClass + "\"";
    }

    // --------------------------------------------------------------
    public static class BYTE_ARRAY extends DocFlavor {
        private static final long serialVersionUID = -9065578006593857475L;

        public BYTE_ARRAY(String mimeType) {
            super(mimeType, "[B");
        }

        public static final BYTE_ARRAY AUTOSENSE = new BYTE_ARRAY("application/octet-stream");

        public static final BYTE_ARRAY GIF = new BYTE_ARRAY("image/gif");

        public static final BYTE_ARRAY JPEG = new BYTE_ARRAY("image/jpeg");

        public static final BYTE_ARRAY PCL = new BYTE_ARRAY("application/vnd.hp-pcl");

        public static final BYTE_ARRAY PDF = new BYTE_ARRAY("application/pdf");

        public static final BYTE_ARRAY PNG = new BYTE_ARRAY("image/png");

        public static final BYTE_ARRAY POSTSCRIPT = new BYTE_ARRAY("application/postscript");

        public static final BYTE_ARRAY TEXT_HTML_HOST = new BYTE_ARRAY("text/html;charset="
                + hostEncoding);

        public static final BYTE_ARRAY TEXT_HTML_US_ASCII = new BYTE_ARRAY(
                "text/html;charset=us-ascii");

        public static final BYTE_ARRAY TEXT_HTML_UTF_16 = new BYTE_ARRAY(
                "text/html;charset=utf-16");

        public static final BYTE_ARRAY TEXT_HTML_UTF_16BE = new BYTE_ARRAY(
                "text/html;charset=utf-16be");

        public static final BYTE_ARRAY TEXT_HTML_UTF_16LE = new BYTE_ARRAY(
                "text/html;charset=utf-16le");

        public static final BYTE_ARRAY TEXT_HTML_UTF_8 = new BYTE_ARRAY(
                "text/html;charset=utf-8");

        public static final BYTE_ARRAY TEXT_PLAIN_HOST = new BYTE_ARRAY("text/plain;charset="
                + hostEncoding);

        public static final BYTE_ARRAY TEXT_PLAIN_US_ASCII = new BYTE_ARRAY(
                "text/plain;charset=us-ascii");

        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16 = new BYTE_ARRAY(
                "text/plain;charset=utf-16");

        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16BE = new BYTE_ARRAY(
                "text/plain;charset=utf-16be");

        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16LE = new BYTE_ARRAY(
                "text/plain;charset=utf-16le");

        public static final BYTE_ARRAY TEXT_PLAIN_UTF_8 = new BYTE_ARRAY(
                "text/plain;charset=utf-8");
    }

    public static class CHAR_ARRAY extends DocFlavor {
        private static final long serialVersionUID = -8720590903724405128L;

        public CHAR_ARRAY(String mimeType) {
            super(mimeType, "[C");
        }

        public static final CHAR_ARRAY TEXT_HTML = new CHAR_ARRAY("text/html;charset=utf-16");

        public static final CHAR_ARRAY TEXT_PLAIN = new CHAR_ARRAY("text/plain;charset=utf-16");
    }

    public static class INPUT_STREAM extends DocFlavor {
        private static final long serialVersionUID = -7045842700749194127L;

        public INPUT_STREAM(String mimeType) {
            super(mimeType, "java.io.InputStream");
        }

        public static final INPUT_STREAM AUTOSENSE = new INPUT_STREAM(
                "application/octet-stream");

        public static final INPUT_STREAM GIF = new INPUT_STREAM("image/gif");

        public static final INPUT_STREAM JPEG = new INPUT_STREAM("image/jpeg");

        public static final INPUT_STREAM PCL = new INPUT_STREAM("application/vnd.hp-pcl");

        public static final INPUT_STREAM PDF = new INPUT_STREAM("application/pdf");

        public static final INPUT_STREAM PNG = new INPUT_STREAM("image/png");

        public static final INPUT_STREAM POSTSCRIPT = new INPUT_STREAM("application/postscript");

        public static final INPUT_STREAM TEXT_HTML_HOST = new INPUT_STREAM("text/html;charset="
                + hostEncoding);

        public static final INPUT_STREAM TEXT_HTML_US_ASCII = new INPUT_STREAM(
                "text/html;charset=us-ascii");

        public static final INPUT_STREAM TEXT_HTML_UTF_16 = new INPUT_STREAM(
                "text/html;charset=utf-16");

        public static final INPUT_STREAM TEXT_HTML_UTF_16BE = new INPUT_STREAM(
                "text/html;charset=utf-16be");

        public static final INPUT_STREAM TEXT_HTML_UTF_16LE = new INPUT_STREAM(
                "text/html;charset=utf-16le");

        public static final INPUT_STREAM TEXT_HTML_UTF_8 = new INPUT_STREAM(
                "text/html;charset=utf-8");

        public static final INPUT_STREAM TEXT_PLAIN_HOST = new INPUT_STREAM(
                "text/plain;charset=" + hostEncoding);

        public static final INPUT_STREAM TEXT_PLAIN_US_ASCII = new INPUT_STREAM(
                "text/plain;charset=us-ascii");

        public static final INPUT_STREAM TEXT_PLAIN_UTF_16 = new INPUT_STREAM(
                "text/plain;charset=utf-16");

        public static final INPUT_STREAM TEXT_PLAIN_UTF_16BE = new INPUT_STREAM(
                "text/plain;charset=utf-16be");

        public static final INPUT_STREAM TEXT_PLAIN_UTF_16LE = new INPUT_STREAM(
                "text/plain;charset=utf-16le");

        public static final INPUT_STREAM TEXT_PLAIN_UTF_8 = new INPUT_STREAM(
                "text/plain;charset=utf-8");
    }

    public static class READER extends DocFlavor {
        private static final long serialVersionUID = 7100295812579351567L;

        public READER(String mimeType) {
            super(mimeType, "java.io.Reader");
        }

        public static final READER TEXT_HTML = new READER("text/html;charset=utf-16");

        public static final READER TEXT_PLAIN = new READER("text/plain;charset=utf-16");
    }

    public static class SERVICE_FORMATTED extends DocFlavor {
        private static final long serialVersionUID = 6181337766266637256L;

        public SERVICE_FORMATTED(String className) {
            super("application/x-java-jvm-local-objectref", className);
        }

        public static final SERVICE_FORMATTED PAGEABLE = new SERVICE_FORMATTED(
                "java.awt.print.Pageable");

        public static final SERVICE_FORMATTED PRINTABLE = new SERVICE_FORMATTED(
                "java.awt.print.Printable");

        public static final SERVICE_FORMATTED RENDERABLE_IMAGE = new SERVICE_FORMATTED(
                "java.awt.image.renderable.RenderableImage");
    }

    public static class STRING extends DocFlavor {
        private static final long serialVersionUID = 4414407504887034035L;

        public STRING(String mimeType) {
            super(mimeType, "java.lang.String");
        }

        public static final STRING TEXT_HTML = new STRING("text/html;charset=utf-16");

        public static final STRING TEXT_PLAIN = new STRING("text/plain;charset=utf-16");
    }

    public static class URL extends DocFlavor {
        private static final long serialVersionUID = 2936725788144902062L;

        public URL(String mimeType) {
            super(mimeType, "java.net.URL");
        }

        public static final URL AUTOSENSE = new URL("application/octet-stream");

        public static final URL GIF = new URL("image/gif");

        public static final URL JPEG = new URL("image/jpeg");

        public static final URL PCL = new URL("application/vnd.hp-pcl");

        public static final URL PDF = new URL("application/pdf");

        public static final URL PNG = new URL("image/png");

        public static final URL POSTSCRIPT = new URL("application/postscript");

        public static final URL TEXT_HTML_HOST = new URL("text/html;charset=" + hostEncoding);

        public static final URL TEXT_HTML_US_ASCII = new URL("text/html;charset=us-ascii");

        public static final URL TEXT_HTML_UTF_16 = new URL("text/html;charset=utf-16");

        public static final URL TEXT_HTML_UTF_16BE = new URL("text/html;charset=utf-16be");

        public static final URL TEXT_HTML_UTF_16LE = new URL("text/html;charset=utf-16le");

        public static final URL TEXT_HTML_UTF_8 = new URL("text/html;charset=utf-8");

        public static final URL TEXT_PLAIN_HOST = new URL("text/plain;charset=" + hostEncoding);

        public static final URL TEXT_PLAIN_US_ASCII = new URL("text/plain;charset=us-ascii");

        public static final URL TEXT_PLAIN_UTF_16 = new URL("text/plain;charset=utf-16");

        public static final URL TEXT_PLAIN_UTF_16BE = new URL("text/plain;charset=utf-16be");

        public static final URL TEXT_PLAIN_UTF_16LE = new URL("text/plain;charset=utf-16le");

        public static final URL TEXT_PLAIN_UTF_8 = new URL("text/plain;charset=utf-8");
    }
} /* End of DocFlavor class */
