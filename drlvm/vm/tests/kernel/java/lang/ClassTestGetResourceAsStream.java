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
 * 
 * This ClassTestGetResourceAsStream class ("Software") is furnished under license and
 * may only be used or copied in accordance with the terms of that license.
 *  
 */

package java.lang;

import java.io.File;

import junit.framework.TestCase;

/*
 * Created on 03.02.2006
 * 
 * This ClassTestGetResourceAsStream class is used to test the Core API
 * java.lang.Class.getResourceAsStream method
 *  
 */
public class ClassTestGetResourceAsStream extends TestCase {

    static String vendor = System.getProperty("java.vm.vendor");

    /**
     * The method throws NullPointerException if argument is null
     */
    public void test1() {
        try {
            Void.class.getResourceAsStream(null);
        	fail("Error1: NullPointerException is not thrown for null argument");
        } catch (NullPointerException _) {           
        }
    }

    /**
     * prepending by package name.
     */
    public void test2() {
        byte magic[] = new byte[4];
        try {
            Void.class.getResourceAsStream("Class.class").read(magic);
            //System.out.println(Integer.toString(0xff&magic[0], 16));
            assertTrue("Error1", new String(magic).equals(new String(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})));
        } catch (java.io.IOException e) {
        	fail("Unexpected Exception " + e);
        }
    }

    /**
     * unchanging.
     */
    public void test3() {
        byte magic[] = new byte[4];
        try {
            Void.class.getResourceAsStream("/"
                    + Class.class.getPackage().getName().replace(
                            '.', '/') + "/Class.class").read(magic);
            assertTrue("Error1", new String(magic).equals(new String(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})));
        } catch (java.io.IOException e) {
        	fail("Unexpected Exception " + e);
        }
   }

    /**
     * in java.ext.dirs.
     */
    public void test4() {
            String as[] = System.getProperty("java.ext.dirs").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File dir = new File(as[i]);
                if (dir.exists() && dir.isDirectory()) {
                    String afn[] = dir.list();
                    File aff[] = dir.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile() && aff[j].getName().endsWith(".jar")) {
                            try {
                                java.util.jar.JarFile jf = new java.util.jar.JarFile(aff[j]);
                                for (java.util.Enumeration e = jf.entries(); e
                                        .hasMoreElements();) {
                                    String s = e.nextElement().toString();
                                    if (s.endsWith(".class")) {
                                        assertTrue("Error1",
                                            Void.class.getResourceAsStream("/" + s)
                                                .available() >= 0);
                                        return;
                                    }
                                }
                            } catch (java.io.IOException e) {
                            	fail("Unexpected Exception " + e);
                            }
                        }
                    }
                }
            }
    }

    /**
     * in java.class.path.
     */
    public void test5() {
            String as[] = System.getProperty("java.class.path").split(
                    System.getProperty("path.separator"));
            for (int i = 0; i < as.length; i++) {
                File f = new File(as[i]);
                if (f.exists() && f.isDirectory()) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            try {
                                assertTrue("Error1",
                                    Void.class.getResourceAsStream("/" + afn[j])
                                    .available() >= 0);
                            } catch(java.io.IOException e1) {
                            	fail("Unexpected Exception 1 " + e1);
                            }
                            return;
                        }
                    }
                } else if (f.exists() && f.isFile()
                        && f.getName().endsWith(".jar")) {
                    try {
                        java.util.jar.JarFile jf = new java.util.jar.JarFile(f);	
                        for (java.util.Enumeration e = jf.entries(); e
                                .hasMoreElements();) {
                            String s = e.nextElement().toString();
                            if (s.endsWith(".class")) {
                                byte magic[] = new byte[4];
                                try {
                                    Void.class.getResourceAsStream("/" + s).read(magic);
                                    assertTrue("Error2", new String(magic).equals(new String(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})));
                                } catch (java.io.IOException e2) {
                                	fail("Unexpected Exception 2 " + e2);
                                }
                                return;
                            }
                        }
                    } catch (java.io.IOException e) {
                    	fail("Unexpected Exception 3 " + e);
                    }
                }
            }
    }

    /**
     * via -Xbootclasspath (vm.boot.class.path).
     */
    public void test6() {
            String as[] = System.getProperty(
                    (vendor.equals("Apache Software Foundation") ? "vm" : "sun")
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
                                byte magic[] = new byte[4];
                                try {
                                    Void.class.getResourceAsStream("/" + s).read(magic);
                                    assertTrue("Error1", new String(magic).equals(new String(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})));
                                } catch (java.io.IOException e1) {
                                	fail("Unexpected Exception 1 " + e1);
                                }
                                return;
                            }
                        }
                    } catch (java.io.IOException e2) {
                    	fail("Unexpected Exception 2 " + e2);
                    }
                } else if (f.exists() && f.isDirectory()) {
                    String afn[] = f.list();
                    File aff[] = f.listFiles();
                    for (int j = 0; j < aff.length; j++) {
                        if (aff[j].isFile()) {
                            try {
                                assertTrue("Error2",
                                    Void.class.getResourceAsStream("/" + afn[j])
                                    .available() >= 0);
                            } catch(java.io.IOException e){
                            	fail("Unexpected Exception 3 " + e);
                            }
                            return;
                        }
                    }
                }
            }
    }
}
