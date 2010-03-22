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

import junit.framework.TestCase;

/*
 * Created on March 29, 2006
 *
 * This RuntimeAdditionalTest class is used to test the Core API Runtime class
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1. should be adopted for linux
 * ###############################################################################
 * ###############################################################################
 */

public class RuntimeAdditionalTest10 extends TestCase {
    /**
     * read, write info using streams of two process (cat <->java) using the
     * "exitValue - IllegalThreadStateException" loop to try to read p1-out to
     * write p2-in and to read p2-out again to calculate bytes to check their
     * equality
     */
    public void test_11() {
        System.out.println("==test_11===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\WINNT\\system32\\cmd.exe\"";
            //\\//\\cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\IJE\\orp\\ZSS\\___LOG.1\"";
            cmnd1 = RuntimeAdditionalTest0.cm + " /C cat \"" + RuntimeAdditionalTest0.textFile + "\"";
            //cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\Documents and Settings\\All
            // Users\\Documents\\My Pictures\\Sample Pictures\\Winter.jpg\"";
            //cmnd2 = RuntimeAdditionalTest0.cm+" /C cat";
            //\\//\\cmnd2 = "java -cp
            // \""+System.getProperty("java.class.path")+"\" RuntimeAdditionalSupport2";
//            cmnd2 = "java "
//                    + (System.getProperty("java.class.path").length() > 0 ? "-cp "
//                            + System.getProperty("java.class.path")
//                            : "") + " klazz2";
            String ttt = System.getProperty("vm.boot.class.path") +(System.getProperty("java.class.path").length() > 0 ? 
                    File.pathSeparator + System.getProperty("java.class.path") : "");
            cmnd2 = RuntimeAdditionalTest0.javaStarter+" -Xbootclasspath/a:" + ttt + " -cp " + ttt + " java.lang.RuntimeAdditionalSupport2";
           // System.out.println(cmnd2);
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd = "sh -c \"cat -v /lib/ld.so\"";
            //\\//\\cmnd1 = "cat -v /lib/ld.so";
            cmnd1 = "/bin/cat -v \"" + RuntimeAdditionalTest0.libFile + "\"";
            //cmnd2 = "sh -c cat";
            //\\//\\cmnd2 = "java -cp
            // \""+System.getProperty("java.class.path")+"\" RuntimeAdditionalSupport2";
//            cmnd2 = "java "
//                    + (System.getProperty("java.class.path").length() > 0 ? "-cp "
//                            + System.getProperty("java.class.path")
//                            : "") + " RuntimeAdditionalSupport2";
            String ttt = System.getProperty("vm.boot.class.path") +(System.getProperty("java.class.path").length() > 0 ? 
                    File.pathSeparator + System.getProperty("java.class.path") : "");
            cmnd2 = RuntimeAdditionalTest0.javaStarter+" -Xbootclasspath/a:" + ttt + " -cp " + ttt + " java.lang.RuntimeAdditionalSupport2";
            //System.out.println(cmnd2);
			return;
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }

        Process pi3 = null;
        Process pi4 = null;
        try {
            pi3 = Runtime.getRuntime().exec(cmnd1);
            pi4 = Runtime.getRuntime().exec(cmnd2);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
            java.io.OutputStream os4 = pi4.getOutputStream();
            pi4.getErrorStream();
            java.io.InputStream is4 = pi4.getInputStream();
            while (is4.available() == 0) {
            }
            Thread.sleep(1000);
            byte[] bbb5 = new byte[is4.available()];
            is4.read(bbb5);
            //System.out.println(new String(bbb5));
            int c1 = 0;
            int c2 = 0;
            //int s1 = 0;
            //int s2 = 0;
            while (true) {
                try {
                    while ((is.available()) != 0) {
                        os4.write(/*s1 = */is.read());
                        c1++;
                        os4.write(10);
                        c1++;
                        try {
                            os4.flush();
                        } catch (java.io.IOException e) {
                        }
                        /* System.out.println(Character.toString((char)( s2 =*/ is4
                                .read()/* ))) */;
                        /* System.out.println(Character.toString((char)( s2 =*/ is4
                                .read()/* ))) */;
                        c2 += 2;
                        /*
                         * while ((is4.available()) != 0) { c2++;
                         * System.out.println(Character.toString((char)(s2=is4.read())));
                         * System.out.println(s1+"|"+s2); try {
                         * Thread.sleep(1000); } catch (Exception e) { } }
                         */
                    }
                    //System.out.println(7777777);
                    pi3.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }
            while (true) {
                try {
                    while ((is4.available()) != 0) {
                        c2++;
                        /* System.out.println(Character.toString((char) */is4
                                .read()/* )) */;
                    }
                    //System.out.println(888888);
                    if (c2 == c1) {
                        if ((is4.available()) == 0) {
                            //System.out.println("test_11 PASSED.");
                        } else {
                            fail("test_11 FAILED.");
                        }
                        try {
                            pi4.exitValue();
                        } catch (IllegalThreadStateException e) {
                            pi4.destroy();
                            return;
                        }
                    } else if (is4.available() == 0) {
                        int i = 0;
                        for (; i < 500; i++) {
                            if (is4.available() != 0) {
                                break;
                            }
                            try {
                                Thread.sleep(10);
                            } catch (Exception e) {
                            }
                        }
                        //System.out.println(i);
                        if (i == 500 && is4.available() == 0) {
                            //System.out.println(c1+"|"+c2);
                            try {
                                pi4.exitValue();
                            } catch (IllegalThreadStateException e) {
                                pi4.destroy();
                            }
                            fail("test_11 FAILED.");
                        }
                    }
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }

        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_11): unexpected exception.");
        }
        try {
            pi3.exitValue();
        } catch (IllegalThreadStateException e) {
            pi3.destroy();
        }
        try {
            pi4.exitValue();
        } catch (IllegalThreadStateException e) {
            pi4.destroy();
        }
    }
}