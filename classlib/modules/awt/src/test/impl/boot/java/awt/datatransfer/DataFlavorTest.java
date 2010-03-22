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
/**
 * @author Michael Danilov
 */

package java.awt.datatransfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import junit.framework.TestCase;

public class DataFlavorTest extends TestCase {

    public final void testHashCode() {
        assertEquals(new DataFlavor("x/y; class=java.util.LinkedList", "").hashCode(),
                new DataFlavor("x/y; class=java.util.LinkedList", "").hashCode());
        assertTrue(new DataFlavor("z/y; class=java.util.LinkedList", "").hashCode() !=
                new DataFlavor("x/y; class=java.util.LinkedList", "").hashCode());
        assertTrue(new DataFlavor("x/z; class=java.util.LinkedList", "").hashCode() !=
                new DataFlavor("x/y; class=java.util.LinkedList", "").hashCode());
        assertTrue(new DataFlavor("x/y; class=java.util.LinkedList", "").hashCode() !=
                new DataFlavor("x/y; class=java.util.List", "").hashCode());
        assertTrue(new DataFlavor("x/y; class=java.lang.String; charset=c1", "").hashCode() ==
                new DataFlavor("x/y; class=java.lang.String; charset=c2", "").hashCode());
        assertTrue(new DataFlavor("x/y; class=java.util.LinkedList; charset=c1", "").hashCode() !=
                new DataFlavor("x/y; class=java.util.List; charset=c2", "").hashCode());
    }

    /*
     * Class under test for boolean equals(Object)
     */
    public final void testEqualsObject() {
        assertTrue(new DataFlavor("x/y; class=java.util.LinkedList", "").equals(
                (Object) new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("z/y; class=java.util.LinkedList", "").equals(
                (Object) new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("x/z; class=java.util.LinkedList", "").equals(
                (Object) new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("x/y; class=java.util.LinkedList", "").equals(
                (Object) new DataFlavor("x/y; class=java.util.List", "")));
        assertTrue(new DataFlavor("x/y; class=java.lang.String; charset=c1", "").equals(
                (Object) new DataFlavor("x/y; class=java.lang.String; charset=c2", "")));
        assertTrue(!new DataFlavor("x/y; class=java.util.LinkedList; charset=c1", "").equals(
                (Object) new DataFlavor("x/y; class=java.util.List; charset=c2", "")));
        assertTrue(!new DataFlavor("z/y; class=java.util.LinkedList", "").equals(
                new RuntimeException()));

        // Regression for HARMONY-2033
        assertFalse(new DataFlavor().equals("")); //$NON-NLS-1$
    }

    /*
     * Class under test for boolean equals(DataFlavor)
     */
    public final void testEqualsDataFlavor() {
        assertTrue(new DataFlavor("x/y; class=java.util.LinkedList", "").equals(
                new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("z/y; class=java.util.LinkedList", "").equals(
                new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("x/z; class=java.util.LinkedList", "").equals(
            new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("x/y; class=java.util.LinkedList", "").equals(
            new DataFlavor("x/y; class=java.util.List", "")));
        assertTrue(new DataFlavor("x/y; class=java.lang.String; charset=c1", "").equals(
            new DataFlavor("x/y; class=java.lang.String; charset=c2", "")));
        assertTrue(!new DataFlavor("x/y; class=java.util.LinkedList; charset=c1", "").equals(
            new DataFlavor("x/y; class=java.util.List; charset=c2", "")));
    }

    public final void testMatch() {
        assertTrue(new DataFlavor("x/y; class=java.util.LinkedList", "").match(
                new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("z/y; class=java.util.LinkedList", "").match(
                new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("x/z; class=java.util.LinkedList", "").match(
            new DataFlavor("x/y; class=java.util.LinkedList", "")));
        assertTrue(!new DataFlavor("x/y; class=java.util.LinkedList", "").match(
            new DataFlavor("x/y; class=java.util.List", "")));
        assertTrue(new DataFlavor("x/y; class=java.lang.String; charset=c1", "").match(
            new DataFlavor("x/y; class=java.lang.String; charset=c2", "")));
        assertTrue(!new DataFlavor("x/y; class=java.util.LinkedList; charset=c1", "").match(
            new DataFlavor("x/y; class=java.util.List; charset=c2", "")));
    }

    public final void testTryToLoadClass() {
        try {
            DataFlavor.tryToLoadClass("java.lang.Thread", null);
            assertTrue(true);
        } catch (ClassNotFoundException e) {
            fail();
        }
    }

    public final void testGetTextPlainUnicodeFlavor() {
        String charset = null;
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.startsWith("linux")) {
            charset = "iso-10646-ucs-2";
        }
        if (osName.startsWith("windows")) {
            charset = "utf-16le";
        }

        assertEquals(DataFlavor.getTextPlainUnicodeFlavor(),
                new DataFlavor("text/plain; charset=" + charset, ""));
    }

    /*
     * Class under test for void DataFlavor(Class, String)
     */
    public final void testDataFlavorClassString() {
        assertEquals(new DataFlavor(String.class, "z"),
                new DataFlavor("application/x-java-serialized-object; class=java.lang.String", "z"));
        assertEquals(new DataFlavor(String.class, null),
                new DataFlavor("application/x-java-serialized-object; class=java.lang.String",
                        "application/x-java-serialized-object"));
    }

    /*
     * Class under test for void DataFlavor(String, String)
     */
    public final void testDataFlavorStringString() {
        assertEquals(new DataFlavor("x/y; param=value", "z").getParameter("param"), "value");
        assertEquals(new DataFlavor("x/y; param=value", "z").getRepresentationClass(),
                java.io.InputStream.class);
        assertEquals(new DataFlavor("application/x-java-serialized-object; class=java.lang.String", null),
                new DataFlavor("application/x-java-serialized-object; class=java.lang.String",
                        "application/x-java-serialized-object"));
        assertEquals(new DataFlavor("x/y; class=java.lang.String", "z").getRepresentationClass(),
                String.class);

        try {
            new DataFlavor("x/y class=java.lang.String", "z");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /*
     * Class under test for void DataFlavor(String)
     */
    public final void testDataFlavorString() {
        try {
            assertEquals(new DataFlavor("x/y; param=value").getParameter("param"), "value");
            assertEquals(new DataFlavor("x/y; param=value").getRepresentationClass(),
                    java.io.InputStream.class);
            assertEquals(new DataFlavor("application/x-java-serialized-object; class=java.lang.String"),
                    new DataFlavor("application/x-java-serialized-object; class=java.lang.String",
                            "application/x-java-serialized-object"));
            assertEquals(new DataFlavor("x/y; class=java.lang.String").getRepresentationClass(),
                    String.class);
        } catch (ClassNotFoundException e1) {
            fail();
        }

        try {
            new DataFlavor("x/y class=java.lang.String", "z");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new DataFlavor("x/y; class=java.lang.Strng");
            fail();
        } catch (ClassNotFoundException e) {
            assertTrue(true);
        }
    }

    /*
     * Class under test for void DataFlavor(String, String, ClassLoader)
     */
    public final void testDataFlavorStringStringClassLoader() {
        try {
            assertEquals(new DataFlavor("x/y; param=value", "z",
                    Thread.currentThread().getContextClassLoader()).getParameter("param"), "value");
            assertEquals(new DataFlavor("x/y; param=value", "z",
                    Thread.currentThread().getContextClassLoader()).getRepresentationClass(),
                    java.io.InputStream.class);
            assertEquals(new DataFlavor("application/x-java-serialized-object; class=java.lang.String", null,
                    Thread.currentThread().getContextClassLoader()),
                    new DataFlavor("application/x-java-serialized-object; class=java.lang.String",
                            "application/x-java-serialized-object"));
            assertEquals(new DataFlavor("x/y; class=java.lang.String", "z",
                    Thread.currentThread().getContextClassLoader()).getRepresentationClass(),
                    String.class);
        } catch (ClassNotFoundException e1) {
            fail();
        }

        try {
            new DataFlavor("x/y class=java.lang.String", "z");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public final void testGetPrimaryType() {
        assertEquals(new DataFlavor("x/y; class=java.lang.String", "z").getPrimaryType(), "x");
    }

    public final void testGetSubType() {
        assertEquals(new DataFlavor("x/y; class=java.lang.String", "z").getSubType(), "y");
    }

    public final void testGetMimeType() {
        assertEquals(new DataFlavor("x/y; param=value", "z").getMimeType(),
                "x/y; class=\"java.io.InputStream\"; param=\"value\"");
    }

    public final void testGetParameter() {
        assertEquals(new DataFlavor("x/y; param=value", "z").getParameter("param"), "value");
        assertEquals(new DataFlavor("x/y; param=value", "z").getParameter("humanPresentableName"), "z");
        
        // Regression for HARMONY-2033
        assertNull(new DataFlavor().getParameter("")); //$NON-NLS-1$
    }

    public final void testGetHumanPresentableName() {
        DataFlavor flavor = new DataFlavor("x/y; param=value", "z");

        assertEquals(flavor.getHumanPresentableName(), "z");
        flavor.setHumanPresentableName("zz");
        assertEquals(flavor.getHumanPresentableName(), "zz");
    }

    public final void testSetHumanPresentableName() {
        testGetHumanPresentableName();
    }

    public final void testGetRepresentationClass() {
        assertEquals(new DataFlavor("x/y; class=java.io.Reader", "z").getRepresentationClass(),
                java.io.Reader.class);
    }

    public final void testGetDefaultRepresentationClass() {
        assertEquals(new DataFlavor("x/y; param=value", "z").getDefaultRepresentationClass(),
                java.io.InputStream.class);
    }

    public final void testGetDefaultRepresentationClassAsString() {
        assertEquals(new DataFlavor("x/y; param=value", "z").getDefaultRepresentationClassAsString(),
                "java.io.InputStream");
    }

    public final void testIsRepresentationClassSerializable() {
        assertTrue(new DataFlavor(Serializable.class, "").
                isRepresentationClassSerializable());
        assertFalse(new DataFlavor(Object.class, "").
                isRepresentationClassSerializable());
    }

//    Enable this test when RMI is supported
//    public final void testIsRepresentationClassRemote() {
//        assertTrue(new DataFlavor(Remote.class, "").
//                isRepresentationClassRemote());
//        assertFalse(new DataFlavor(Object.class, "").
//                isRepresentationClassRemote());
//    }

    public final void testIsRepresentationClassReader() {
        assertTrue(new DataFlavor(new Reader() {
            @Override
            public void close() throws IOException {
            }
            @Override
            public int read(char[] arg0, int arg1, int arg2) throws IOException {
                return 0;
            }
        }.getClass(), "").isRepresentationClassReader());
        assertFalse(new DataFlavor(Object.class, "").
                isRepresentationClassReader());
    }

    public final void testIsRepresentationClassInputStream() {
        assertTrue(new DataFlavor(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        }.getClass(), "").isRepresentationClassInputStream());
        assertFalse(new DataFlavor(Object.class, "").
                isRepresentationClassInputStream());
    }

    public final void testIsRepresentationClassCharBuffer() {
        assertTrue(new DataFlavor(CharBuffer.class, "").
                isRepresentationClassCharBuffer());
        assertFalse(new DataFlavor(Object.class, "").
                isRepresentationClassCharBuffer());
    }

    public final void testIsRepresentationClassByteBuffer() {
        assertTrue(new DataFlavor(ByteBuffer.class, "").
                isRepresentationClassByteBuffer());
        assertFalse(new DataFlavor(Object.class, "").
                isRepresentationClassByteBuffer());
    }

    public final void testIsMimeTypeSerializedObject() {
        assertTrue(new DataFlavor(Serializable.class, "").
                isMimeTypeSerializedObject());
        assertFalse(new DataFlavor("x/y", "").
                isMimeTypeSerializedObject());
    }

//    Enable this test when RMI is supported
//    public final void testIsFlavorSerializedObjectType() {
//        assertTrue(new DataFlavor(Serializable.class, "").
//                isFlavorSerializedObjectType());
//        assertFalse(new DataFlavor("x/y; class=java.io.Serializable", "").
//                isFlavorSerializedObjectType());
//
//        assertFalse(new DataFlavor(Remote.class, "").
//                isFlavorSerializedObjectType());
//    }

//    Enable this test when RMI is supported
//    public final void testIsFlavorRemoteObjectType() {
//        assertTrue(new DataFlavor(DataFlavor.javaRemoteObjectMimeType + "; class=java.rmi.Remote", "").
//                isFlavorRemoteObjectType());
//        assertFalse(new DataFlavor(Serializable.class, "").
//                isFlavorRemoteObjectType());
//        assertFalse(new DataFlavor("x/y; class=java.rmi.Remote", "").
//                isFlavorRemoteObjectType());
//    }

    public final void testIsFlavorJavaFileListType() {
        assertTrue(new DataFlavor("application/x-java-file-list; class=java.util.LinkedList", "").
                isFlavorJavaFileListType());
        assertFalse(new DataFlavor("application/x-java-file; class=java.util.LinkedList", "").
                isFlavorJavaFileListType());
        assertFalse(new DataFlavor("application/x-java-file-list", "").
                isFlavorJavaFileListType());
    }

    public final void testIsFlavorTextType() {
        assertTrue(DataFlavor.stringFlavor.
                isFlavorTextType());
        assertTrue(new DataFlavor("text/z", "").
                isFlavorTextType());
        assertFalse(new DataFlavor("text/z; charset=dummy", "").
                isFlavorTextType());
    }



    /*
     * Class under test for boolean isMimeTypeEqual(DataFlavor)
     */
    public final void testIsMimeTypeEqualDataFlavor() {
        assertTrue(new DataFlavor("x/y; class=java.io.Serializable", "1").
                isMimeTypeEqual(new DataFlavor("x/y; param=value", "2")));
        assertFalse(new DataFlavor("x/y; class=java.io.Serializable", "1").
                isMimeTypeEqual(new DataFlavor("z/y; param=value", "2")));
        assertFalse(new DataFlavor("x/y; class=java.io.Serializable", "1").
                isMimeTypeEqual(new DataFlavor("x/z; param=value", "2")));
        
        // Regression for HARMONY-2033
        assertTrue(new DataFlavor().isMimeTypeEqual(new DataFlavor()));
    }

    /*
     * Class under test for boolean isMimeTypeEqual(String)
     */
    public final void testIsMimeTypeEqualString() {
        assertTrue(new DataFlavor("x/y; class=java.io.Serializable", "1").
                isMimeTypeEqual("x/y; param=value"));
        assertFalse(new DataFlavor("x/y; class=java.io.Serializable", "1").
                isMimeTypeEqual("z/y; param=value"));
        assertFalse(new DataFlavor("x/y; class=java.io.Serializable", "1").
                isMimeTypeEqual("x/z; param=value"));
        
        // Regression for HARMONY-2033
        assertFalse(new DataFlavor().isMimeTypeEqual("")); //$NON-NLS-1$
    }

    /*
     * Class under test for Object clone()
     */
    public final void testClone() throws CloneNotSupportedException {
        DataFlavor flavor = new DataFlavor("x/y; class=java.io.Serializable; param=value", "z");
        DataFlavor clone = (DataFlavor) flavor.clone();

        assertEquals(flavor, clone);
        assertEquals(flavor.getParameter("param"), clone.getParameter("param"));
        assertEquals(flavor.getHumanPresentableName(), clone.getHumanPresentableName());
    }

    public final void testSelectBestTextFlavor() {
        assertEquals(DataFlavor.selectBestTextFlavor(new DataFlavor[] {
                new DataFlavor("x/y", ""),
                DataFlavor.stringFlavor
        }), DataFlavor.stringFlavor);

        assertEquals(DataFlavor.selectBestTextFlavor(new DataFlavor[] {
                new DataFlavor("text/xml", ""),
                new DataFlavor("text/plain", "")
        }), new DataFlavor("text/xml", ""));

        assertEquals(DataFlavor.selectBestTextFlavor(new DataFlavor[] {
                new DataFlavor("text/xml; class=java.lang.String", ""),
                new DataFlavor("text/xml; class=java.nio.CharBuffer", "")
        }), new DataFlavor("text/xml; class=java.lang.String", ""));

        assertEquals(DataFlavor.selectBestTextFlavor(new DataFlavor[] {
                new DataFlavor("text/xml; class=java.lang.String; charset=US-ASCII", ""),
                new DataFlavor("text/xml; class=java.lang.String; charset=UTF-16", "")
        }), new DataFlavor("text/xml; class=java.lang.String; charset=UTF-16", ""));
        
        
        // Regression for HARMONY-2033
        assertNull(DataFlavor
                .selectBestTextFlavor(new DataFlavor[] { new DataFlavor() }));
    }

    @SuppressWarnings("deprecation")
    public void testHarmony1477Regression() {
        // Regression for HARMONY-1477
        DataFlavor df = new DataFlavor();
        try {
            assertFalse(df.equals(""));
            assertFalse(df.isMimeTypeEqual(""));
        } catch (IllegalArgumentException iae) {
            fail("Regression test failed");
        }
    }
    
    public void testCloneNullMimeInfo()
            throws CloneNotSupportedException,NullPointerException {
        // Regression for HARMONY-2069
        DataFlavor df = new DataFlavor();
        df.clone(); 
    }
}
