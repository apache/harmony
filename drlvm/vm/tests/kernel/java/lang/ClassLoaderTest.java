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
 * @author Serguei S.Zapreyev
 */

package java.lang;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import org.apache.harmony.test.TestResources;

import junit.framework.TestCase;

/*
 * Created on 02.04.2006
 * 
 * This ClassLoaderTest class is used to test the Core API
 * java.lang.ClassLoader class
 *  
 */

@SuppressWarnings(value={"all"}) public class ClassLoaderTest extends TestCase {

    static String vendor = System.getProperty("java.vm.vendor");

    class TestClassLoader extends ClassLoader {
        // Use only for top level classes

        public Class<?> findClass(String name) {
            String nm = name.substring(1, name.length());
            byte[] b = loadClassData(nm);
            return defineClass(nm, b, 0, b.length);
        }

        private byte[] loadClassData(String name) {
            //return new byte[0];
            //System.out.println("loadClassData: "+ name);
            String nm = name.replace('.', '/');
            java.io.InputStream is = this.getClass().getResourceAsStream(
                    "/" + nm + ".class");
            //System.out.println("loadClassData: "+ nm);
            try {
                int len = is.available();
                byte ab[] = new byte[len];
                is.read(ab);
                return ab;
            } catch (java.io.IOException _) {
            }
            return null;
        }

        public TestClassLoader() {
            super();
        }

        public TestClassLoader(ClassLoader cl) {
            super(cl);
        }
    }

    /**
     *  
     */
    public void test_ClassLoader_V() {
        assertTrue("Error1",
                new TestClassLoader().getParent() == TestClassLoader
                        .getSystemClassLoader());
    }

    /**
     *  
     */
    public void test_ClassLoader_Cla() {
        TestClassLoader tcl = new TestClassLoader();
        assertTrue("Error1",
                new TestClassLoader((ClassLoader) tcl).getParent() == tcl);
    }

    /**
     *  
     */
    public void test_clearAssertionStatus_V() {
        TestClassLoader tcl = new TestClassLoader();
        ((ClassLoader) tcl).clearAssertionStatus();

        ClassLoader cl = Class.class.getClassLoader();
        if (cl == null)
            cl = ClassLoader.getSystemClassLoader();
        cl.clearAssertionStatus();
        try {
            Class c = cl.loadClass("javax.xml.transform.stream.StreamResult");
            try {
                c.newInstance(); // to be initialized
            } catch (Exception _) {
                //System.out.println("(test_clearAssertionStatus_V)Exception
                // 1");
            }
            assertTrue("Error1:", c.desiredAssertionStatus() == c
                    .desiredAssertionStatus()); // "this method is not
                                                // guaranteed to return the
                                                // actual assertion status"
        } catch (ClassNotFoundException _) {
            System.out.println("ClassNotFoundException 1");
        }
    }

    /**
     *  
     */
//Commented because of the drlvm issue
//    public void te_st_defineClass_BArr_I_I() {
    public void test_defineClass_BArr_I_I() {
        class LCL extends ClassLoader {
            // only for special case

            public Class findClass(String name) {
                String nm = name.substring(1, name.length());
                byte[] b = loadClassData(nm);
                if (nm.endsWith("SAXTransformerFactory")) {
                    return defineClass(b, 0, b.length); // ALL RIGHT
                } else if (nm.endsWith("SAXSource")) {
                    return defineClass(b, 10, b.length - 20); // ClassFormatError
                } else if (nm.endsWith("SAXResult")) {
                    return defineClass(b, 0, b.length + 20); // IndexOutOfBoundsException
                }
                return null;
            }

            private byte[] loadClassData(String name) {
                //System.out.println("loadClassData: "+ name);
                String nm = name.replace('.', '/');
                java.io.InputStream is = this.getClass().getResourceAsStream(
                        "/" + nm + ".class");
                //System.out.println("loadClassData: "+ nm);
                try {
                    int len = is.available();
                    byte ab[] = new byte[len];
                    is.read(ab);
                    return ab;
                } catch (java.io.IOException _) {
                }
                return null;
            }

            public LCL() {
                super();
            }

            public LCL(ClassLoader cl) {
                super(cl);
            }
        }

        LCL tcl = new LCL();

        // TEST CASE #1:
        try {
            Class c = tcl
                    .loadClass("#javax.xml.transform.sax.SAXTransformerFactory");
            assertTrue("Error1", c != null);
        } catch (ClassNotFoundException _) {
        }

        // TEST CASE #2:
        try {
            Class c = tcl.loadClass("#javax.xml.transform.sax.SAXSource");
            fail("Error2: ");
        } catch (ClassFormatError _) {
        } catch (ClassNotFoundException e) {
            fail("Error3: " + e.toString());
        }

        // TEST CASE #3:
        try {
            Class c = tcl.loadClass("#javax.xml.transform.sax.SAXResult");
            fail("Error4: ");
        } catch (IndexOutOfBoundsException _) {
        } catch (ClassNotFoundException e) {
            fail("Error5: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_defineClass_Str_BArr_I_I() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                String nm = name.substring(1, name.length());
                byte[] b = loadClassData(nm);
                if (nm.endsWith("SAXTransformerFactory")) {
                    return defineClass(nm, b, 0, b.length); // ALL RIGHT
                } else if (nm.endsWith("SAXSource")) {
                    return defineClass(nm, b, 10, b.length - 20); // ClassFormatError
                } else if (nm.endsWith("SAXResult")) {
                    return defineClass(nm, b, 0, b.length + 20); // IndexOutOfBoundsException
                } else if (nm.endsWith("DOMResult")) {
                    return defineClass(nm + "XXX", b, 0, b.length); // NoClassDefFoundError
                } else if (nm.endsWith("Calendar")) {
                    return defineClass(nm, b, 0, b.length); // SecurityException
                }
                return null;
            }

            private byte[] loadClassData(String name) {
                String nm = name.replace('.', '/');
                java.io.InputStream is = this.getClass().getResourceAsStream(
                        "/" + nm + ".class");
                try {
                    int len = is.available();
                    byte ab[] = new byte[len];
                    is.read(ab);
                    return ab;
                } catch (java.io.IOException _) {
                }
                return null;
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();

        // TEST CASE #1:
        try {
            Class c = tcl
                    .loadClass("#javax.xml.transform.sax.SAXTransformerFactory");
            assertTrue("Error1", c != null);
        } catch (ClassNotFoundException _) {
        }

        // TEST CASE #2:
        try {
            tcl.loadClass("#javax.xml.transform.sax.SAXSource");
            fail("Error2: ");
        } catch (ClassFormatError _) {
        } catch (ClassNotFoundException e) {
            fail("Error3: " + e.toString());
        }

        // TEST CASE #3:
        try {
            tcl.loadClass("#javax.xml.transform.sax.SAXResult");
            fail("Error4: ");
        } catch (IndexOutOfBoundsException _) {
        } catch (ClassNotFoundException e) {
            fail("Error5: " + e.toString());
        }

        // TEST CASE #4:
        try {
            tcl.loadClass("#javax.xml.transform.dom.DOMResult");
            fail("Error6: ");
        } catch (NoClassDefFoundError _) {
        } catch (ClassNotFoundException e) {
            fail("Error7: " + e.toString());
        }

        // TEST CASE #5:
        try {
            tcl.loadClass("#java.util.Calendar");
            fail("Error8: ");
        } catch (SecurityException _) {
        } catch (ClassNotFoundException e) {
            fail("Error9: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_defineClass_Str_BArr_I_I_Pro() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                String nm = name.substring(1, name.length());
                //java.security.ProtectionDomain pd = new
                // java.security.ProtectionDomain(new CodeSource(new
                // java.net.URL(""), new java.security.cert.Certificate[]{} ),
                // new PermissionCollection());
                java.security.ProtectionDomain pd = Void.class
                        .getProtectionDomain();
                byte[] b = loadClassData(nm);
                if (nm.endsWith("SAXTransformerFactory")) {
                    return defineClass(nm, b, 0, b.length, pd); // ALL RIGHT
                } else if (nm.endsWith("SAXSource")) {
                    return defineClass(nm, b, 10, b.length - 20, pd); // ClassFormatError
                } else if (nm.endsWith("SAXResult")) {
                    return defineClass(nm, b, 0, b.length + 20, pd); // IndexOutOfBoundsException
                } else if (nm.endsWith("DOMResult")) {
                    return defineClass(nm + "XXX", b, 0, b.length, pd); // NoClassDefFoundError
                } else if (nm.endsWith("Calendar")) {
                    return defineClass(nm, b, 0, b.length, pd); // SecurityException
                } else if (nm.endsWith("TimeZone")) {
                    return defineClass(nm, b, 0, b.length,
                            (java.security.ProtectionDomain) null); // SecurityException
                }
                return null;
            }

            private byte[] loadClassData(String name) {
                String nm = name.replace('.', '/');
                java.io.InputStream is = this.getClass().getResourceAsStream(
                        "/" + nm + ".class");
                try {
                    int len = is.available();
                    byte ab[] = new byte[len];
                    is.read(ab);
                    return ab;
                } catch (java.io.IOException _) {
                }
                return null;
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();

        // TEST CASE #1:
        try {
            Class c = tcl
                    .loadClass("#javax.xml.transform.sax.SAXTransformerFactory");
            assertTrue("Error1", c.getProtectionDomain().equals(
                    Void.class.getProtectionDomain()));
        } catch (ClassNotFoundException _) {
        }

        // TEST CASE #2:
        try {
            tcl.loadClass("#javax.xml.transform.sax.SAXSource");
            fail("Error2: ");
        } catch (ClassFormatError _) {
        } catch (ClassNotFoundException e) {
            fail("Error3: " + e.toString());
        }

        // TEST CASE #3:
        try {
            tcl.loadClass("#javax.xml.transform.sax.SAXResult");
            fail("Error4: ");
        } catch (IndexOutOfBoundsException _) {
        } catch (ClassNotFoundException e) {
            fail("Error5: " + e.toString());
        }

        // TEST CASE #4:
        try {
            tcl.loadClass("#javax.xml.transform.dom.DOMResult");
            fail("Error6: ");
        } catch (NoClassDefFoundError _) {
        } catch (ClassNotFoundException e) {
            fail("Error7: " + e.toString());
        }

        // TEST CASE #5:
        try {
            tcl.loadClass("#java.util.Calendar");
            fail("Error8: ");
        } catch (SecurityException _) {
        } catch (ClassNotFoundException e) {
            fail("Error9: " + e.toString());
        }

        // TEST CASE #5:
        try {
            tcl.loadClass("#java.util.TimeZona");
            fail("Error10: ");
        } catch (NullPointerException _) {
        } catch (ClassNotFoundException e) {
            fail("Error11: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_definePackage_Str_Str_Str_Str_Str_Str_Str_Str_URL() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                String nm = name.substring(1, name.length());
                byte[] b = loadClassData(nm);
                if (nm.endsWith("AudioFileReader")
                        || nm.endsWith("AudioFileWriter")) {
                    return defineClass(b, 0, b.length); // ALL RIGHT
                }
                return null;
            }

            private byte[] loadClassData(String name) {
                String nm = name.replace('.', '/');
                java.io.InputStream is = this.getClass().getResourceAsStream(
                        "/" + nm + ".class");
                try {
                    int len = is.available();
                    byte ab[] = new byte[len];
                    is.read(ab);
                    return ab;
                } catch (java.io.IOException _) {
                }
                return null;
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();

        // TEST CASE #1:
        try {
            ClassLoader cl = Class.class.getClassLoader();
            if (cl == null)
                cl = ClassLoader.getSystemClassLoader();
            cl.definePackage("javax.swing.filechooser", "ZSSspecTitle",
                    "ZSSspecVersion", "ZSSspecVendor", "ZSSimplTitle",
                    "ZSSimplVersion", "ZSSimplVendor", new java.net.URL(
                            "http://intel.com/"));
            try {
                Class c1 = cl.loadClass("javax.swing.filechooser.FileFilter");
                Class c2 = cl.loadClass("javax.swing.filechooser.FileView");
                try {
                    c1.newInstance(); // to be initialized
                    c2.newInstance(); // to be initialized
                } catch (Exception _) {
                }
                //assertTrue("Error1", c1.getPackage().equals(c2.getPackage())
                // );
                assertTrue("Error1", cl.getPackage("javax.swing.filechooser")
                        .getName().equals("javax.swing.filechooser"));
                assertTrue("Error2 "
                        + cl.getPackage("javax.swing.filechooser")
                                .getSpecificationTitle(), cl.getPackage(
                        "javax.swing.filechooser").getSpecificationTitle()
                        .equals("ZSSspecTitle"));
                assertTrue("Error3", cl.getPackage("javax.swing.filechooser")
                        .getSpecificationVersion().equals("ZSSspecVersion"));
                assertTrue("Error4", cl.getPackage("javax.swing.filechooser")
                        .getSpecificationVendor().equals("ZSSspecVendor"));
                assertTrue("Error5", cl.getPackage("javax.swing.filechooser")
                        .getImplementationTitle().equals("ZSSimplTitle"));
                assertTrue("Error6", cl.getPackage("javax.swing.filechooser")
                        .getImplementationVersion().equals("ZSSimplVersion"));
                assertTrue("Error7", cl.getPackage("javax.swing.filechooser")
                        .getImplementationVendor().equals("ZSSimplVendor"));
            } catch (ClassNotFoundException _) {
                System.out.println("ClassNotFoundException 1");
            }

        } catch (java.net.MalformedURLException _) {
        }

        // TEST CASE #2:
        try {
            tcl.definePackage("javax.swing.filechooser", "ZSSspecTitle",
                    "ZSSspecVersion", "ZSSspecVendor", "ZSSimplTitle",
                    "ZSSimplVersion", "ZSSimplVendor", new java.net.URL(
                            "http://intel.com/"));
            fail("Error8");
        } catch (java.lang.IllegalArgumentException _) {
            // CORRECT !
        } catch (java.net.MalformedURLException _) {
            fail("Error9");
        }

        // TEST CASE #3:
        try {
            tcl.definePackage("javax.swing.plaf.basic", "ZSSspecTitle",
                    "ZSSspecVersion", "ZSSspecVendor", "ZSSimplTitle",
                    "ZSSimplVersion", "ZSSimplVendor", new java.net.URL(
                            "http://intel.com/"));

            tcl.loadClass("#javax.swing.plaf.basic.BasicBorders");
            tcl.loadClass("#javax.swing.plaf.basic.BasicArrowButton");
            assertTrue("Error10", tcl.getPackage("javax.swing.plaf.basic")
                    .getName().equals("javax.swing.plaf.basic"));
        } catch (ClassNotFoundException _) {
        } catch (java.net.MalformedURLException _) {
        }
    }

    /**
     *  
     */
    public void test_findClass_Str() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) throws ClassNotFoundException {
                return super.findClass(name);
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();

        // TEST CASE #1:
        try {
            tcl.loadClass("#javax.swing.plaf.basic.BasicBorders");
            fail("Error1");
        } catch (ClassNotFoundException _) {
        } catch (Throwable _) {
            fail("Error2");
        }

        // TEST CASE #2:
        try {
            tcl.loadClass(null);
            fail("Error3");
        } catch (NullPointerException _) {
        } catch (Throwable _) {
            fail("Error4");
        }

        // TEST CASE #3:
        try {
            tcl.loadClass("java.lang.Object");
        } catch (ClassNotFoundException _) {
        } catch (Throwable e) {
            fail("Error6 " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_findLibrary_Str() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) throws ClassNotFoundException {
                return super.findClass(name);
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();

        // TEST CASE #1:
        if (vendor.equals("Intel DRL")) {
            tcl.findLibrary(System.mapLibraryName("java"));
        }

        // TEST CASE #2:
        ClassLoader cl = Class.class.getClassLoader();
        if (cl == null)
            cl = ClassLoader.getSystemClassLoader();
        if (vendor.equals("Intel DRL")) {
            cl.findLibrary(System.mapLibraryName("lang"));
        }

        // TEST CASE #3:
        tcl.findLibrary(null);
    }

    /**
     *  
     */
    public void test_findLoadedClass_Str() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                String nm = name.substring(1, name.length());
                if (nm.endsWith("JPEGImageReadParamXXX")) {
                    byte[] b = loadClassData("j"
                            + (nm.replaceAll("JPEGImageReadParamXXX",
                                    "JPEGImageReadParam")));
                    return defineClass(b, 0, b.length); // ALL RIGHT
                }
                byte[] b = loadClassData(nm);
                if (nm.endsWith("IIOImage")) {
                    return defineClass(b, 0, b.length); // ALL RIGHT
                }
                if (nm.endsWith("TemplateSet")) {
                    return defineClass(b, 0, b.length); // ALL RIGHT
                }
                return null;
            }

            private byte[] loadClassData(String name) {
                String nm = name.replace('.', '/');
                java.io.InputStream is = this.getClass().getResourceAsStream(
                        "/" + nm + ".class");
                try {
                    int len = is.available();
                    byte ab[] = new byte[len];
                    is.read(ab);
                    return ab;
                } catch (java.io.IOException _) {
                }
                return null;
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();
        ClassLoader cl = Class.class.getClassLoader();
        if (cl == null)
            cl = ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            tcl.loadClass("#org.apache.harmony.lang.generics.TemplateSet");
            assertTrue("Error1", tcl.findLoadedClass("org.apache.harmony.lang.generics.TemplateSet")
                    .getName().equals("org.apache.harmony.lang.generics.TemplateSet"));
        } catch (Throwable e) {
            fail("Error2: " + e.toString());
        }

        // TEST CASE #2:
        try {
            assertTrue("Error3", cl.findLoadedClass(null) == null);
        } catch (Throwable e) {
            fail("Error4: " + e.toString());
        }
        try {
            assertTrue("Error33", tcl.findLoadedClass(null) == null);
        } catch (Throwable e) {
            fail("Error44: " + e.toString());
        }

        // TEST CASE #3:
        try {
        	if (cl.findLoadedClass("java.lang.Object") != null) // XXX: Here we
                                                                // differ of others. Is it
                                                                // absolutely
                                                                // acceptible
                                                                // and
                                                                // correct???
                assertTrue("Error5", cl.findLoadedClass("java.lang.Object")
                        .getName().equals("java.lang.Object"));
        } catch (Throwable e) {
            fail("Error6: " + e.toString());
        }

        // TEST CASE #5:
        try {
            cl.findLoadedClass("void");
        } catch (Throwable e) {
            fail("Error10: " + e.toString());
        }

        //      // TEST CASE #8:
        //      try {
        //          //Class c =
        // Class.forName("[[[Ljavax.imageio.plugins.jpeg.JPEGImageReadParam;",
        // true, tcl);
        //          Class c =
        // Class.forName("javax.imageio.plugins.jpeg.JPEGImageReadParamXXX",
        // true, tcl);
        //          ///Class c =
        // tcl.loadClass("#javax.imageio.plugins.jpeg.JPEGImageReadParam");
        //          System.out.println(c);
        //          tcl.findLoadedClass("javax.imageio.plugins.jpeg.JPEGImageReadParam");
        //          assertTrue("Error7",
        // tcl.findLoadedClass("javax.imageio.plugins.jpeg.JPEGImageReadParam").getName().equals("javax.imageio.plugins.jpeg.JPEGImageReadParam"));
        //          ///fail("Error11");
        //      } catch (Throwable e) {
        //          fail("Error12: " + e.toString());
        //      }
        

    }

    /**
     * FIXME invalid test: only VM can initiate loading class 
     */
    public void te_st_findLoadedClass_Str_2() {
        // TEST CASE #4:
        try {
            Class c = Class.forName("java.lang.ClassLoaderTest$7LCL", true,
                    ClassLoader.getSystemClassLoader());
            assertTrue("Error7", ClassLoader.getSystemClassLoader()
                    .findLoadedClass("java.lang.ClassLoaderTest$7LCL")
                    .getName().equals("java.lang.ClassLoaderTest$7LCL"));
        } catch (Throwable e) {
            fail("Error8: " + e.toString());
        }

        // TEST CASE #6:
        try {
            Class c = Class.forName("[B", true, ClassLoader
                    .getSystemClassLoader());
            assertTrue("Error7", ClassLoader.getSystemClassLoader()
                    .findLoadedClass("[B") == null); // if the element type is a primitive type, then the array class has no class loader
        } catch (Throwable e) {
            fail("Error12: " + e.toString());
        }

        // TEST CASE #6_1:
        try {
            ClassLoader ld = TestResources.getLoader();
            Class c = Class.forName("[Lorg.apache.harmony.lang.generics.TemplateSet;", true, ld);
            assertTrue("Error7_1", ld
                    .findLoadedClass("[Lorg.apache.harmony.test.TestResources;") == null); // according the delegate loading model
        } catch (Throwable e) {
            fail("Error12_1: " + e.toString());
        }

        // TEST CASE #6_2: 
        try {
            try {
                Class c = Class.forName("I", true, ClassLoader
                    .getSystemClassLoader()); // If name denotes a primitive type or void, an attempt will be made to 
                                              // locate a user-defined class in the unnamed package whose name is name.
                                              // Therefore, this method cannot be used to obtain any of the Class 
                                              // objects representing primitive types or void.
                assertTrue("Error7_2", ClassLoader.getSystemClassLoader()
                    .findLoadedClass("I") == null);
            } catch (ClassNotFoundException e) {
                //System.out.println("I1: "+e.toString());
            }
            try {
                Class c = Class.forName("int", true, ClassLoader
                    .getSystemClassLoader()); // If name denotes a primitive type or void, an attempt will be made to 
                                              // locate a user-defined class in the unnamed package whose name is name.
                                              // Therefore, this method cannot be used to obtain any of the Class 
                                              // objects representing primitive types or void.
                assertTrue("Error7_2", ClassLoader.getSystemClassLoader()
                    .findLoadedClass("int") == null);
            } catch (ClassNotFoundException e) {
                //System.out.println("I2: "+e.toString());
            }
            try {
                ClassLoader ld = TestResources.getLoader();
                //ClassLoader ld = ClassLoader.getSystemClassLoader();
                //System.out.println("I3: "+int.class.getName()+"|"+int.class.getClassLoader());
                Class c = ld.loadClass("int");
                assertTrue("Error7_2", ld.findLoadedClass("int") == null);
            } catch (ClassNotFoundException e) {
                //System.out.println("I4: "+e.toString());
            }
        } catch (Throwable e) {
            fail("Error12_2: " + e.toString());
        }

        // TEST CASE #7: 
        try {
            Class c = Class.forName("[[[Ljava.lang.Object;", false, ClassLoader
                    .getSystemClassLoader());
            ClassLoader.getSystemClassLoader().findLoadedClass(
                    "[[[Ljava.lang.Object;");
            assertTrue("Error7", ClassLoader.getSystemClassLoader()
                    .findLoadedClass("[[[Ljava.lang.Object;") == null);
        } catch (Throwable e) {
            fail("Error12: " + e.toString());
        }

        try{
            // "public class bbb{} // (javac 1.5)":
                byte[] clazz = {-54,-2,-70,-66,0,0,0,49,0,13,10,0,3,0,10,7,0,11,7,0,12,1,0,6,60,105,110,105,116,
                            62,1,0,3,40,41,86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,78,117,109,98,101,
                    114,84,97,98,108,101,1,0,10,83,111,117,114,99,101,70,105,108,101,1,0,8,98,98,98,
                    46,106,97,118,97,12,0,4,0,5,1,0,3,98,98,98,1,0,16,106,97,118,97,47,108,97,110,103,
                            47,79,98,106,101,99,116,0,33,0,2,0,3,0,0,0,0,0,1,0,1,0,4,0,5,0,1,0,6,0,0,0,29,0,
                            1,0,1,0,0,0,5,42,-73,0,1,-79,0,0,0,1,0,7,0,0,0,6,0,1,0,0,0,1,0,1,0,8,0,0,0,2,0,9};
                FileOutputStream fos;
                fos = new FileOutputStream(new File(System.getProperty("java.ext.dirs")+File.separator+"classes"+File.separator+"bbb.class"));
                for (int i = 0; i < clazz.length; i++) {
                    fos.write((byte)clazz[i]);
                }
                fos.flush();
                fos.close();
            } catch (IOException e) {
                System.out.println("TEST test_findLoadedClass_Str, CASE #10,  DIDN'T RUN"+e.toString());
            }

            // TEST CASE #9
            try{
                ClassLoader chain = ClassLoader.getSystemClassLoader();
                while (chain.getParent()!=null) {
                    chain = chain.getParent();
                }
                Class c = Class.forName("bbb", false, chain); // loading via the Capital ClassLoader
                assertTrue(chain.findLoadedClass("bbb").getName().equals("bbb"));
            } catch (ClassNotFoundException e) {
                System.out.println("TEST test_findLoadedClass_Str, CASE #10,  DIDN'T RUN"+e.toString());
            } catch (Exception e) {
                fail("TEST test_findLoadedClass_Str, CASE #9, FAILED"+e.toString());
            }

            // TEST CASE #10
            class LCL2 extends ClassLoader {
                    public Class findClass(String name) throws ClassNotFoundException {
                        return super.findClass(name);
                    }

                    public LCL2() {
                    super();
                    }
            }

            LCL2 tcl2 = new LCL2();

            try{
                Class c = Class.forName("bbb", false, tcl2);
                ClassLoader chain = tcl2;
                while (chain.getParent() != null) {
                    if (chain.findLoadedClass("bbb")!=null && chain.findLoadedClass("bbb").getName().equals("bbb")) {
                        //System.out.println("TEST PASSED");
                        return;
                    }
                    chain = chain.getParent();
                }
                assertTrue(chain.findLoadedClass("bbb")!=null && chain.findLoadedClass("bbb").getName().equals("bbb"));
            } catch (ClassNotFoundException e) {
                System.out.println("TEST test_findLoadedClass_Str, CASE #10,  DIDN'T RUN");
            } catch (Exception e) {
                fail("TEST test_findLoadedClass_Str, CASE #10,  FAILED(2)");
            }
    }

    /**
     * 
     */
    public void test_findLoadedClass_Str_3() {

        class testCL extends ClassLoader {
            public int res = 0;

            public testCL() {
                super();
                res = 0;
            }

            public Class loadClass(String nm) throws ClassNotFoundException {
                if ("java.lang.ClassLoaderTest$1a3".equals(nm)) {
                    res += 1;
                }
                return super.loadClass(nm);
            }

            public Class fndLoadedClass(String nm) {
                return super.findLoadedClass(nm);
            }
        }
        class a3 extends ClassLoader {
            public a3() {
                super();
            }

            public void main(String[] args) {
                try {
                    new a3().test(args);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            public int test(String[] args) throws Exception {
                testCL cl = new testCL();
                for (int i = 0; i < 10; i++) {
                    int t = (this.callClass(cl)).intValue();
                    if (t != 104) {
                        // System.out.println("Test failed step "+i+": method result = "+t);
                        return 105;
                    }
                }
                assertTrue(cl.fndLoadedClass("java.lang.ClassLoaderTest$1a3")
                        .getName().equals("java.lang.ClassLoaderTest$1a3"));
                return 104;
            }

            private Integer callClass(testCL cLoader) throws Exception {
                Class cls = Class.forName("java.lang.ClassLoaderTest$1a3",
                        true, cLoader);
                Method mm = cls.getMethod("test", (Class[])null);
                return (Integer) mm.invoke(this, new Object[0]);
            }

            public int test() {
                return 104;
            }
        }
        // FIXME invalid test: only VM can initiate loading class
        //new a3().main(new String[] { "" });
    }

    /**
     *  
     */
    public void test_findResource_Str() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                return null;
            }

            public java.net.URL findResource(String name) {
                String nm = name.replace('.', '/');
                return this.getClass().getResource("/" + nm + ".class");
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();
        // TEST CASE #2:
        assertTrue("Error1", tcl.findResource("java.lang.Class") != null);

        // TEST CASE #2:
        try {
            assertTrue("Error2", ClassLoader.getSystemClassLoader()
                    .findResources("java.lang.Class") != null);
        } catch (java.io.IOException _) {
            fail("Error3");
        }
    }
            
    /**
     *  
     */
    public void test_findResources_Str() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                return null;
            }

            public java.util.Enumeration<URL> findResources(String name)
                    throws java.io.IOException {
                return super.findResources(name);
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();
        // TEST CASE #1:
        try {
            assertTrue("Error1", tcl.findResources("java.lang.Class") != null);
        } catch (java.io.IOException _) {
            fail("Error2");
        }

        // TEST CASE #2:
        try {
            ClassLoader.getSystemClassLoader().findResource("java.lang.Class");
        } catch (Exception _) {
            fail("Error3");
        }
        
        try {
            tcl.findResources(null);
        } catch (NullPointerException _) {
            fail("Error5");
        } catch (java.io.IOException _) {
        }
    }

    /**
     *  
     */
    public void test_findSystemClass_Str() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) {
                return null;
            }

            public LCL() {
                super();
            }
        }

        LCL tcl = new LCL();
        // TEST CASE #1:
        try {
            assertTrue("Error1", tcl.findSystemClass("java.lang.Class") != null);
        } catch (ClassNotFoundException _) {
            fail("Error2");
        }

        // TEST CASE #2:
        try {
            ClassLoader.getSystemClassLoader().findSystemClass(
                    "java.lang.Class");
        } catch (ClassNotFoundException _) {
            fail("Error3");
        }

        // TEST CASE #3:
        try {
            ClassLoader.getSystemClassLoader().getSystemClassLoader()
                    .getSystemClassLoader().findSystemClass("java.lang.Class");
        } catch (ClassNotFoundException _) {
            fail("Error4");
        }

        // TEST CASE #4:
        try {
            ClassLoader.getSystemClassLoader().findSystemClass(null);
            fail("Error5");
        } catch (ClassNotFoundException _) {
            fail("Error6");
        } catch (NullPointerException _) {
        }
    }

    /**
     *  
     */
    public void test_getPackage_Str() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            cl.getPackage(null);
            fail("Error1");
        } catch (NullPointerException _) {
        }

        // TEST CASE #2:
        assertTrue("Error1", cl.getPackage("UNEXISTED.Unknown.Package") == null);

        // TEST CASE #3:
        try {
            cl.definePackage("javax", "ZSSspecTitle1", "ZSSspecVersion1",
                    "ZSSspecVendor1", "ZSSimplTitle1", "ZSSimplVersion1",
                    "ZSSimplVendor1", new java.net.URL(
                            "http://intel.com/"));
            cl.definePackage("javax.imageio", "ZSSspecTitle2",
                    "ZSSspecVersion2", "ZSSspecVendor2", "ZSSimplTitle2",
                    "ZSSimplVersion2", "ZSSimplVendor2", new java.net.URL(
                            "http://intel.com/"));
            try {
                Class c1 = cl.loadClass("javax.imageio.ImageIO");
                try {
                    c1.newInstance(); // to be initialized
                } catch (Exception _) {
                }
                assertTrue("Error1", cl.getPackage("javax.imageio").getName()
                        .equals("javax.imageio"));
                assertTrue("Error2", cl.getPackage("javax.imageio")
                        .getSpecificationTitle().equals("ZSSspecTitle2"));
                assertTrue("Error3", cl.getPackage("javax.imageio")
                        .getSpecificationVersion().equals("ZSSspecVersion2"));
                assertTrue("Error4", cl.getPackage("javax.imageio")
                        .getSpecificationVendor().equals("ZSSspecVendor2"));
                assertTrue("Error5", cl.getPackage("javax.imageio")
                        .getImplementationTitle().equals("ZSSimplTitle2"));
                assertTrue("Error6", cl.getPackage("javax.imageio")
                        .getImplementationVersion().equals("ZSSimplVersion2"));
                assertTrue("Error7", cl.getPackage("javax.imageio")
                        .getImplementationVendor().equals("ZSSimplVendor2"));
                assertTrue("Error8", cl.getPackage("javax").getName().equals(
                        "javax"));
                assertTrue("Error9", cl.getPackage("javax")
                        .getSpecificationTitle().equals("ZSSspecTitle1"));
                assertTrue("Error10", cl.getPackage("javax")
                        .getSpecificationVersion().equals("ZSSspecVersion1"));
                assertTrue("Error11", cl.getPackage("javax")
                        .getSpecificationVendor().equals("ZSSspecVendor1"));
                assertTrue("Error12", cl.getPackage("javax")
                        .getImplementationTitle().equals("ZSSimplTitle1"));
                assertTrue("Error13", cl.getPackage("javax")
                        .getImplementationVersion().equals("ZSSimplVersion1"));
                assertTrue("Error14", cl.getPackage("javax")
                        .getImplementationVendor().equals("ZSSimplVendor1"));
            } catch (ClassNotFoundException _) {
                System.out.println("ClassNotFoundException 1");
            }
        } catch (java.net.MalformedURLException _) {
        }
    }

    /**
     *  
     */
    public void test_getPackages_V() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) throws ClassNotFoundException {
                return super.findClass(name);
            }

            public LCL() {
                super();
            }

            public LCL(ClassLoader cl) {
                super(cl);
            }
        }
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        int loadersNumber = 11; //20;
        int pkgsPerLoader = 21; //30;
        ClassLoader acl[] = new ClassLoader[loadersNumber];
        ClassLoader prevcl = new LCL(cl);

        // TEST CASE #1:
        for (int i = 0; i < loadersNumber; i++) {
            acl[i] = new LCL(prevcl);
            prevcl = acl[i];
        }
        for (int i = 0; i < loadersNumber; i++) {
            for (int j = 0; j < pkgsPerLoader; j++) {
                try {
                    String curLable = Integer.toString(i) + "_"
                            + Integer.toString(j);
                    acl[i].definePackage(curLable, "ZSSspecTitle" + curLable,
                            "ZSSspecVersion" + curLable, "ZSSspecVendor"
                                    + curLable, "ZSSimplTitle" + curLable,
                            "ZSSimplVersion" + curLable, "ZSSimplVendor"
                                    + curLable, new java.net.URL(
                                    "http://intel.com/"));
                } catch (java.net.MalformedURLException _) {
                }
            }
        }
        int res = 0;
        int prevLen = 0;
        int tmp = 0;
        for (int i = 0; i < loadersNumber; i++) {
            assertTrue("Error1", (tmp = acl[i].getPackages().length) > prevLen);
            prevLen = tmp;
            for (int j = 0; j < acl[i].getPackages().length; j++) {
                if (acl[i].getPackages()[j].getName().equals(
                        Integer.toString(i) + "_"
                                + Integer.toString(pkgsPerLoader - 1))) {
                    res++;
                    assertTrue(
                            "Error2",
                            acl[i].getPackages()[j]
                                    .getSpecificationTitle()
                                    .equals(
                                            "ZSSspecTitle"
                                                    + Integer.toString(i)
                                                    + "_"
                                                    + Integer
                                                            .toString(pkgsPerLoader - 1)));
                    assertTrue(
                            "Error3",
                            acl[i].getPackages()[j]
                                    .getSpecificationVersion()
                                    .equals(
                                            "ZSSspecVersion"
                                                    + Integer.toString(i)
                                                    + "_"
                                                    + Integer
                                                            .toString(pkgsPerLoader - 1)));
                    assertTrue(
                            "Error4",
                            acl[i].getPackages()[j]
                                    .getSpecificationVendor()
                                    .equals(
                                            "ZSSspecVendor"
                                                    + Integer.toString(i)
                                                    + "_"
                                                    + Integer
                                                            .toString(pkgsPerLoader - 1)));
                    assertTrue(
                            "Error5",
                            acl[i].getPackages()[j]
                                    .getImplementationTitle()
                                    .equals(
                                            "ZSSimplTitle"
                                                    + Integer.toString(i)
                                                    + "_"
                                                    + Integer
                                                            .toString(pkgsPerLoader - 1)));
                    assertTrue(
                            "Error6",
                            acl[i].getPackages()[j]
                                    .getImplementationVersion()
                                    .equals(
                                            "ZSSimplVersion"
                                                    + Integer.toString(i)
                                                    + "_"
                                                    + Integer
                                                            .toString(pkgsPerLoader - 1)));
                    assertTrue(
                            "Error7",
                            acl[i].getPackages()[j]
                                    .getImplementationVendor()
                                    .equals(
                                            "ZSSimplVendor"
                                                    + Integer.toString(i)
                                                    + "_"
                                                    + Integer
                                                            .toString(pkgsPerLoader - 1)));
                    break;
                }
            }
        }
        assertTrue("Error8", res == loadersNumber);
    }

    /**
     *  
     */
    public void test_getParent_V() {
        class LCL extends ClassLoader {
            // only for special case

            public Class<?> findClass(String name) throws ClassNotFoundException {
                return super.findClass(name);
            }

            public LCL() {
                super();
            }

            public LCL(ClassLoader cl) {
                super(cl);
            }
        }
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        int loadersNumber = 1000;
        ClassLoader acl[] = new ClassLoader[loadersNumber];
        ClassLoader prevcl = cl;

        // TEST CASE #1:
        for (int i = 0; i < loadersNumber; i++) {
            acl[i] = new LCL(prevcl);
            prevcl = acl[i];
        }
        for (int i = loadersNumber - 1; i > -1; i--) {
            if (i != 0) {
                assertTrue("Error1", acl[i].getParent().equals(acl[i - 1]));
            } else {
                assertTrue("Error2", acl[i].getParent().equals(cl));
            }
        }
    }

    /**
     *  
     */
    public void test_getResource_Str() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            cl.getResource(null);
            fail("Error1: NullPointerException is not thrown for null argument");
        } catch (NullPointerException _) {
        }

        // TEST CASE #2:
        assertTrue("Error1: unexpected:"
                + cl.getResource("java/lang/Class.class").toString(), cl
                .getResource("java/lang/Class.class").toString().indexOf(
                        "java/lang/Class.class") != -1);

        // TEST CASE #3:
        if (vendor.equals("Intel DRL")) {
            // -Xbootclasspath[/a /p]:<some non-empty path list> or
            // -D{vm/sun}.boot.class.path=<some non-empty path list> arguments
            // should be passed for ij.exe for real check
            String as[] = System.getProperty(
                    (vendor.equals("Intel DRL") ? "vm" : "sun")
                            + ".boot.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                assertTrue("Error1", cl.getResource(s)
                                        .toString().indexOf(s) != -1);
                                return;
                            }
                        }
                    } catch (java.io.IOException _) {
                    }
                } else if (f.exists() && f.isDirectory() && false) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            assertTrue("Error1", cl.getResource(afn[j])
                                    .toString().indexOf(afn[j]) != -1);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public void test_getResourceAsStream_Str() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            cl.getResourceAsStream(null);
            fail("Error1: NullPointerException is not thrown for null argument");
        } catch (NullPointerException _) {
        }

        // TEST CASE #2:
        byte magic[] = new byte[4];
        try {
            cl.getResourceAsStream("java/lang/Class.class").read(magic);
            assertTrue("Error1", new String(magic).equals(new String(
                    new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA,
                            (byte) 0xBE })));
        } catch (java.io.IOException _) {
        }

        // TEST CASE #3:
        if (vendor.equals("Intel DRL")) {
            // -Xbootclasspath[/a /p]:<some non-empty path list> or
            // -D{vm/sun}.boot.class.path=<some non-empty path list> arguments
            // should be passed for ij.exe for real check
            String as[] = System.getProperty(
                    (vendor.equals("Intel DRL") ? "vm" : "sun")
                            + ".boot.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                magic = new byte[4];
                                try {
                                    cl.getResourceAsStream(s).read(magic);
                                    assertTrue(
                                            "Error1",
                                            new String(magic)
                                                    .equals(new String(
                                                            new byte[] {
                                                                    (byte) 0xCA,
                                                                    (byte) 0xFE,
                                                                    (byte) 0xBA,
                                                                    (byte) 0xBE })));
                                } catch (java.io.IOException _) {
                                }
                                return;
                            }
                        }
                    } catch (java.io.IOException _) {
                    }
                } else if (f.exists() && f.isDirectory() && false) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            try {
                                assertTrue("Error1", cl.getResourceAsStream(
                                        afn[j]).available() >= 0);
                            } catch (java.io.IOException _) {
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public void test_getResources_Str() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            cl.getResources(null);
            assertTrue("Error1",
                    cl.getResources(null).hasMoreElements() == false);
        } catch (NullPointerException _) {
        } catch (java.io.IOException _) {
        }

        // TEST CASE #2:
        try {
            assertTrue("Error2: unexpected:"
                    + cl.getResources("java/lang/ClassLoader.class")
                            .nextElement(), ((java.net.URL) cl.getResources(
                    "java/lang/ClassLoader.class").nextElement()).toString()
                    .indexOf("java/lang/ClassLoader.class") != -1);
            //java.util.Enumeration e =
            // cl.getResources("java/lang/ClassLoader.class");
            //assertTrue("Error2: unexpected:"
            //        + cl.getResources("/meta-inf/Manifest.mf").nextElement(),
            //        ((java.net.URL)cl.getResources("/meta-inf/Manifest.mf").nextElement()).toString().indexOf(
            //                "/meta-inf/Manifest.mf") != -1);
            //e = cl.getResources("/meta-inf/Manifest.mf");
            //System.out.println(e.nextElement());
            //System.out.println(e.nextElement());
            //System.out.println(e.nextElement());
        } catch (java.io.IOException _) {
        }

        // TEST CASE #3:
        if (vendor.equals("Intel DRL")) {
            // -Xbootclasspath[/a /p]:<some non-empty path list> or
            // -D{vm/sun}.boot.class.path=<some non-empty path list> arguments
            // should be passed for ij.exe for real check
            String as[] = System.getProperty(
                    (vendor.equals("Intel DRL") ? "vm" : "sun")
                            + ".boot.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                assertTrue("Error3", ((java.net.URL) cl
                                        .getResources(s).nextElement())
                                        .toString().indexOf(s) != -1);
                                return;
                            }
                        }
                    } catch (java.io.IOException _) {
                    }
                } else if (f.exists() && f.isDirectory() && false) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            try {
                                assertTrue("Error4", ((java.net.URL) cl
                                        .getResources(afn[j]).nextElement())
                                        .toString().indexOf(afn[j]) != -1);
                                return;
                            } catch (java.io.IOException _) {
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public void test_getSystemClassLoader_V() {
        // TEST CASE #1:
        try {
            String jscl = System.getProperty("java.system.class.loader");
            if (jscl != null && jscl != "") {
                assertTrue("Error1", ClassLoader.getSystemClassLoader()
                        .getClass().getName().equals(jscl));
            }
            java.util.Properties pl = System.getProperties();
            pl.setProperty("java.system.class.loader",
                    "java.lang.ClassLoaderTest$TestClassLoader");
            System.setProperties(pl);
            jscl = System.getProperty("java.system.class.loader");
            if (jscl != null && jscl != "") {
                assertTrue("Error1 "
                        + ClassLoader.getSystemClassLoader().getClass()
                                .getName(), !ClassLoader.getSystemClassLoader()
                        .getClass().getName().equals(jscl));
            }
        } catch (NullPointerException _) {
        }
    }

    /**
     *  
     */
    public void test_getSystemResource_Str() {
        ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            ClassLoader.getSystemResource(null);
            fail("Error1: NullPointerException is not thrown for null argument");
        } catch (NullPointerException _) {
        }

        // TEST CASE #2:
        assertTrue("Error1: unexpected:"
                + ClassLoader.getSystemResource("java/lang/Void.class")
                        .toString(), ClassLoader.getSystemResource(
                "java/lang/Void.class").toString().indexOf(
                "java/lang/Void.class") != -1);

        // TEST CASE #3:
        if (vendor.equals("Intel DRL")) {
            // -Xbootclasspath[/a /p]:<some non-empty path list> or
            // -D{vm/sun}.boot.class.path=<some non-empty path list> arguments
            // should be passed for ij.exe for real check
            String as[] = System.getProperty(
                    (vendor.equals("Intel DRL") ? "vm" : "sun")
                            + ".boot.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                assertTrue("Error1", ClassLoader
                                        .getSystemResource(s).toString()
                                        .indexOf(s) != -1);
                                return;
                            }
                        }
                    } catch (java.io.IOException _) {
                    }
                } else if (f.exists() && f.isDirectory() && false) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            assertTrue("Error1", ClassLoader.getSystemResource(
                                    afn[j]).toString().indexOf(afn[j]) != -1);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public void test_getSystemResourceAsStream_Str() {
        ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            ClassLoader.getSystemResourceAsStream(null);
            fail("Error1: NullPointerException is not thrown for null argument");
        } catch (NullPointerException _) {
        }

        // TEST CASE #2:
        byte magic[] = new byte[4];
        try {
            ClassLoader.getSystemResourceAsStream(
                    "java/lang/reflect/Method.class").read(magic);
            assertTrue("Error1", new String(magic).equals(new String(
                    new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA,
                            (byte) 0xBE })));
        } catch (java.io.IOException _) {
        }

        // TEST CASE #3:
        if (vendor.equals("Intel DRL")) {
            // -Xbootclasspath[/a /p]:<some non-empty path list> or
            // -D{vm/sun}.boot.class.path=<some non-empty path list> arguments
            // should be passed for ij.exe for real check
            String as[] = System.getProperty(
                    (vendor.equals("Intel DRL") ? "vm" : "sun")
                            + ".boot.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                magic = new byte[4];
                                try {
                                    ClassLoader.getSystemResourceAsStream(s)
                                            .read(magic);
                                    assertTrue(
                                            "Error1",
                                            new String(magic)
                                                    .equals(new String(
                                                            new byte[] {
                                                                    (byte) 0xCA,
                                                                    (byte) 0xFE,
                                                                    (byte) 0xBA,
                                                                    (byte) 0xBE })));
                                } catch (java.io.IOException _) {
                                }
                                return;
                            }
                        }
                    } catch (java.io.IOException _) {
                    }
                } else if (f.exists() && f.isDirectory() && false) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            try {
                                assertTrue("Error1", ClassLoader
                                        .getSystemResourceAsStream(afn[j])
                                        .available() >= 0);
                            } catch (java.io.IOException _) {
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public void test_getSystemResources_Str() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        // TEST CASE #1:
        try {
            ClassLoader.getSystemResources(null);
            assertTrue("Error1",
                    cl.getResources(null).hasMoreElements() == false);
        } catch (NullPointerException _) {
        } catch (java.io.IOException _) {
        }

        // TEST CASE #2:
        try {
            assertTrue(
                    "Error2: unexpected:"
                            + ClassLoader.getSystemResources(
                                    "java/lang/ClassLoader.class")
                                    .nextElement(),
                    ((java.net.URL) ClassLoader.getSystemResources(
                            "java/lang/ClassLoader.class").nextElement())
                            .toString().indexOf("java/lang/ClassLoader.class") != -1);
        } catch (java.io.IOException _) {
        }

        // TEST CASE #3:
        if (vendor.equals("Intel DRL")) {
            // -Xbootclasspath[/a /p]:<some non-empty path list> or
            // -D{vm/sun}.boot.class.path=<some non-empty path list> arguments
            // should be passed for ij.exe for real check
            String as[] = System.getProperty(
                    (vendor.equals("Intel DRL") ? "vm" : "sun")
                            + ".boot.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                assertTrue("Error3",
                                        ((java.net.URL) ClassLoader
                                                .getSystemResources(s)
                                                .nextElement()).toString()
                                                .indexOf(s) != -1);
                                return;
                            }
                        }
                    } catch (java.io.IOException _) {
                    }
                } else if (f.exists() && f.isDirectory() && false) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            try {
                                assertTrue("Error4",
                                        ((java.net.URL) ClassLoader
                                                .getSystemResources(afn[j])
                                                .nextElement()).toString()
                                                .indexOf(afn[j]) != -1);
                                return;
                            } catch (java.io.IOException _) {
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public void test_loadClass_Str() {
        class LC1 {
            class LC2 {
                class LC3 {
                    class LC4 {
                        class LC5 {
                            class LC6 {
                                class LC7 {
                                    class LC8 {
                                        class LC9 {
                                            class LC10 {
                                                class LC11 {
                                                    class LC12 {
                                                        class LC13 {
                                                            class LC14 {
                                                                class LC15 {
                                                                    class LC16 {
                                                                        class LC17 {
                                                                            class LC18 {
                                                                                class LC19 {
                                                                                    class LC20
                                                                                            extends
                                                                                            LC19 {
                                                                                        class LC21 {
                                                                                            public LC21() {
                                                                                                System.out
                                                                                                        .println("1 LC21");
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //TestClassLoader tcl = new TestClassLoader();
        ClassLoader tcl = ClassLoader.getSystemClassLoader();
        try {
            Class c = ((ClassLoader) tcl)
                    .loadClass("java.lang.ClassLoaderTest$1LC1$LC2$LC3$LC4$LC5$LC6$LC7$LC8$LC9$LC10$LC11$LC12$LC13$LC14$LC15$LC16$LC17$LC18$LC19$LC20$LC21");
            c.newInstance();
            fail("Error1 ");
        } catch (ClassNotFoundException _) {
            fail("Error2");
        } catch (InstantiationException e) {
        } catch (Exception e) {
            fail("Error3");
        }
    }

    /**
     *  
     */
    public void test_loadClass_Str_Z() {
        class LC1 {
            class LC2 {
                class LC3 {
                    class LC4 {
                        class LC5 {
                            class LC6 {
                                class LC7 {
                                    class LC8 {
                                        class LC9 {
                                            class LC10 {
                                                class LC11 {
                                                    class LC12 {
                                                        class LC13 {
                                                            class LC14 {
                                                                class LC15 {
                                                                    class LC16 {
                                                                        class LC17 {
                                                                            class LC18 {
                                                                                class LC19 {
                                                                                    class LC20
                                                                                            extends
                                                                                            LC19 {
                                                                                        class LC21 {
                                                                                            public LC21() {
                                                                                                System.out
                                                                                                        .println("2 LC21");
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            public LC1() {
                System.out.println("1");
            }
        }
        String longName = "java.lang.ClassLoaderTest$2LC1$LC2$LC3$LC4$LC5$LC6$LC7$LC8$LC9$LC10$LC11$LC12$LC13$LC14$LC15$LC16$LC17$LC18$LC19$LC20$LC21";
        TestClassLoader tcl = new TestClassLoader();
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        String pices[] = longName.split("\\$");
        String curClNm = pices[0] + "$" + pices[1];
        Class c;
        for (int i = 2; i < pices.length; i++) {
            curClNm = curClNm + "$" + pices[i];
            try {
                if (i % 2 == 0) {
                    c = ((ClassLoader) tcl).loadClass(curClNm, true);
                } else {
                    c = cl.loadClass(curClNm, true);
                }
                try {
                    c.newInstance();
                } catch (Exception e) {
                }
            } catch (ClassNotFoundException _) {
                fail("Error1: " + curClNm);
            } catch (Exception e) {
                fail("Error2 " + e.toString());
            }
        }
    }

    /**
     *  
     */
    Class tmpCl;

    int tmpI;

    public void test_loadClass_Str_Z_1() {
        class LC1 {
            class LC2 {
                class LC3 {
                    class LC4 {
                        class LC5 {
                            class LC6 {
                                class LC7 {
                                    class LC8 {
                                        class LC9 {
                                            class LC10 {
                                                class LC11 {
                                                    class LC12 {
                                                        class LC13 {
                                                            class LC14 {
                                                                class LC15 {
                                                                    class LC16 {
                                                                        class LC17 {
                                                                            class LC18 {
                                                                                class LC19 {
                                                                                    class LC20
                                                                                            extends
                                                                                            LC19 {
                                                                                        class LC21 {
                                                                                            public LC21() {
                                                                                                System.out
                                                                                                        .println("3 LC21");
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                public LC2() {
                    System.out.println("1");
                }
            }

            public LC1() {
                System.out.println("1");
            }
        }
        class ConcurentLoader extends Thread {
            public String clN;

            public ClassLoader l;

            public void run() {
                try {
                    if (clN.length() == tmpI) {
                        tmpCl = ((ClassLoader) l).loadClass(clN, false);
                    } else {
                        ((ClassLoader) l).loadClass(clN, false);
                    }
                } catch (ClassNotFoundException _) {
                    fail("Error1");
                }
            }
        }
        String longName = "java.lang.ClassLoaderTest$3LC1$LC2$LC3$LC4$LC5$LC6$LC7$LC8$LC9$LC10$LC11$LC12$LC13$LC14$LC15$LC16$LC17$LC18$LC19$LC20$LC21";
        tmpI = longName.length();
        TestClassLoader tcl = new TestClassLoader();
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        String pices[] = longName.split("\\$");
        String curClNm = pices[0] + "$" + pices[1];
        ConcurentLoader[] thr = new ConcurentLoader[pices.length];
        for (int i = 2; i < pices.length; i++) {
            curClNm = curClNm + "$" + pices[i];
            try {
                thr[i] = new ConcurentLoader();
                thr[i].clN = curClNm;
                if (i % 2 == 0) {
                    thr[i].l = ((ClassLoader) tcl);
                } else {
                    thr[i].l = ((ClassLoader) cl);
                }
            } catch (Exception e) {
                fail("Error2 " + e.toString());
            }
        }
        for (int i = 2; i < pices.length; i++) {
            thr[i].start();
        }
        for (int i = 2; i < pices.length; i++) {
            try {
                thr[i].join();
            } catch (InterruptedException e) {
                fail("Error2 ");
            }
        }
        try {
            tmpCl.newInstance();
        } catch (InstantiationException e) {
            //System.out.println("Warning: "+e.toString());
            //try {
            //    Class.forName(longName).newInstance();
            //} catch (Exception ee) {
            //    System.out.println("Warning: "+ee.toString());
            //  try {
            //      Class.forName("java.lang.ClassLoaderTest$3$LC1").newInstance();
            //    } catch (Exception eee) {
            //        System.out.println("Warning: "+eee.toString());
            //  }
            //}
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_resolveClass_Cla() {

        // TEST CASE #1:
        TestClassLoader tcl = new TestClassLoader();
        ((ClassLoader) tcl).resolveClass(int.class);

        // TEST CASE #2:
        try {
            ClassLoader.getSystemClassLoader().resolveClass(null);
        } catch (NullPointerException _) {
        }

        // TEST CASE #2:
        try {
            Number i[][][] = new Number[1][2][3];
            ClassLoader.getSystemClassLoader().resolveClass(i.getClass());
        } catch (NullPointerException _) {
        }
    }

    /**
     *  
     */
    public void test_setClassAssertionStatus_Str_Z() {
        TestClassLoader tcl = new TestClassLoader();
        ((ClassLoader) tcl).setClassAssertionStatus(";^)", true);

        ClassLoader cl = Class.class.getClassLoader();
        if (cl == null)
            cl = ClassLoader.getSystemClassLoader();
        cl.setClassAssertionStatus("javax.xml.transform.sax.SAXSource", true);
        try {
            Class c = cl.loadClass("javax.xml.transform.sax.SAXSource");
            try {
                c.newInstance(); // to be initialized
            } catch (Exception _) {
            }
            assertTrue("Error1:", c.desiredAssertionStatus() == c
                    .desiredAssertionStatus()); // "this method is not
                                                // guaranteed to return the
                                                // actual assertion status"
        } catch (ClassNotFoundException _) {
            System.out.println("ClassNotFoundException 1");
        }

        ((ClassLoader) tcl).setClassAssertionStatus(
                "javax.xml.transform.sax.SAXTransformerFactory", true);
        try {
            Class c = tcl
                    .loadClass("#javax.xml.transform.sax.SAXTransformerFactory");
            assertTrue("Error2", c.desiredAssertionStatus() == c
                    .desiredAssertionStatus());
        } catch (ClassNotFoundException _) {
        }
        ((ClassLoader) tcl).setClassAssertionStatus(
                "javax.xml.transform.sax.SAXTransformerFactory", false);
        try {
            Class c = tcl
                    .loadClass("javax.xml.transform.sax.SAXTransformerFactory");
            assertTrue("Error3", c.desiredAssertionStatus() == c
                    .desiredAssertionStatus());
        } catch (ClassNotFoundException _) {
            System.out.println("ClassNotFoundException");
        }
        ((ClassLoader) tcl).setClassAssertionStatus(
                "javax.xml.transform.sax.SAXTransformerFactory", true);
        ((ClassLoader) tcl).setClassAssertionStatus(
                "javax.xml.transform.sax.SAXTransformerFactory", false);

        if (cl == null)
            cl = ClassLoader.getSystemClassLoader();
        cl.setClassAssertionStatus(
                "[Ljavax.xml.transform.sax.SAXTransformerFactory;", true);
        try {
            //Class c =
            // Class.forName("[Ljavax.xml.transform.sax.SAXTransformerFactory;",
            // false, cl);
            //  //try {
            //  // Object o = c.newInstance(); // to be initialized
            //  //} catch(Exception e) {System.out.println("Exception 2:"+e);}
            //assertTrue("Error4:", c.desiredAssertionStatus() ==
            // c.desiredAssertionStatus() ); // "this method is not guaranteed
            // to return the actual assertion status"
            Class c = Class.forName(
                    "[Ljavax.xml.transform.sax.SAXTransformerFactory;", true,
                    cl);
            assertTrue("Error5:", c.desiredAssertionStatus() == c
                    .desiredAssertionStatus()); // "this method is not
                                                // guaranteed to return the
                                                // actual assertion status"
        } catch (ClassNotFoundException _) {
            System.out.println("ClassNotFoundException 2");
        }
    }

    /**
     *  
     */
    public void test_setDefaultAssertionStatus_Z() {
        TestClassLoader tcl = new TestClassLoader();
        ((ClassLoader) tcl).setDefaultAssertionStatus(true);
        ((ClassLoader) tcl).setDefaultAssertionStatus(false);
        ((ClassLoader) tcl).setDefaultAssertionStatus(true);
        ((ClassLoader) tcl).setDefaultAssertionStatus(false);
    }

    /**
     *  
     */
    public void test_setPackageAssertionStatus_Str_Z() {
        TestClassLoader tcl = new TestClassLoader();
        ((ClassLoader) tcl).setPackageAssertionStatus(":^(", true);

        ClassLoader cl = Class.class.getClassLoader();
        if (cl == null)
            cl = ClassLoader.getSystemClassLoader();
        cl.setPackageAssertionStatus("javax.xml.transform.stream", true);
        try {
            Class c = cl.loadClass("javax.xml.transform.stream.StreamSource");
            try {
                c.newInstance(); // to be initialized
            } catch (Exception _) {
                //System.out.println("(test_setPackageAssertionStatus_Str_Z)Exception
                // 1");
            }
            assertTrue("Error1:", c.desiredAssertionStatus() == c
                    .desiredAssertionStatus()); // "this method is not
                                                // guaranteed to return the
                                                // actual assertion status"
        } catch (ClassNotFoundException _) {
            System.out.println("ClassNotFoundException 1");
        }

        ((ClassLoader) tcl).setPackageAssertionStatus(
                "javax.xml.transform.sax", true);
        ((ClassLoader) tcl).setPackageAssertionStatus(
                "javax.xml.transform.sax", false);
        ((ClassLoader) tcl).setPackageAssertionStatus("", true);
        ((ClassLoader) tcl).setPackageAssertionStatus("", false);
    }

    /**
     *  
     */
    public void test_setSigners_Cla_ObjArr() {

        // TEST CASE #1:
        TestClassLoader tcl = new TestClassLoader();
        ((ClassLoader) tcl).setSigners(int.class, (Object[]) null);

        // TEST CASE #2:
        ((ClassLoader) tcl).setSigners(int.class, new Object[] { null });

        // TEST CASE #3:
        try {
            ((ClassLoader) tcl).setSigners(null, new Object[] { "" });
            fail("Error1");
        } catch (NullPointerException _) {
        }

        // TEST CASE #4:
        ClassLoader.getSystemClassLoader().setSigners(int.class,
                (Object[]) null);

        // TEST CASE #5:
        ClassLoader.getSystemClassLoader().setSigners(int.class,
                new Object[] { null });

        // TEST CASE #6:
        try {
            ClassLoader.getSystemClassLoader().setSigners(null,
                    new Object[] { "" });
            fail("Error1");
        } catch (NullPointerException _) {
        }

        // TEST CASE #7:
        ClassLoader.getSystemClassLoader().setSigners(int.class,
                (Object[]) null);

        // TEST CASE #8:
        Number i[][][] = new Number[1][2][3];
        ClassLoader.getSystemClassLoader().setSigners(i.getClass(),
                (Object[]) null);
    }

    /*
     * Regression test for HARMONY-877
     * [classlib][lang] compatibility: Harmony method 
     * ClassLoader.getResource(null) returns null while RI throws NPE 
     *
     */
    public void test_HARMONY_877() {
        boolean et = false;
        try { 
            ClassLoader.getSystemClassLoader().getResource(null); 
        } catch (NullPointerException e) {
            et = true;
        }
        assertTrue("NullPointerException expected (case 1)", et);
        et = false;

        try {
            ClassLoader.getSystemClassLoader().getResourceAsStream(null); 
        } catch (NullPointerException e) { 
            et = true;
        }
        assertTrue("NullPointerException expected (case 2)", et);
    }
    /*
     * Regression test for HARMONY-885
     * [classlib][core][drlvm] unexpected LinkageError for ClassLoader.defineClass
     *
     */
    public void test_HARMONY_885() {
        try { 
            byte[] array0 = new byte[] { 2, 2, 2, 2, 2, 2}; 
            new CL().defineKlass(array0, 0, 3); 
            fail("exception expected"); 
        } catch (ClassFormatError e) { 
            //expected 
        } 
    }
    
    private class CL extends ClassLoader { 
        public Class defineKlass(byte[] a, int i1, int i2) throws ClassFormatError { 
            return super.defineClass(a, i1, i2); 
        } 
    } 
}
