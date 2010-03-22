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

public class RuntimeAdditionalTest43 extends TestCase {
    /**
     *  create thread to write and read its streams
     *  slightly investigate is the buffering implemented for out
     *  (and what about in?)
     */
    class thread11 extends Thread {
        public void run() {
            String cmnd2 = null;
            if (RuntimeAdditionalTest0.os.equals("Win") || RuntimeAdditionalTest0.os.equals("Lin")) {
                //cmnd2 = "java -cp \""+System.getProperty("java.class.path")+"\" RuntimeAdditionalSupport2";
                //cmnd2 = "java "+(System.getProperty("java.class.path").length() > 0 ? "-cp \""+System.getProperty("java.class.path")+"\"":"")+" RuntimeAdditionalSupport2";
//                cmnd2 = "java "
//                        + (System.getProperty("java.class.path").length() > 0 ? "-cp "
//                                + System.getProperty("java.class.path")
//                                : "") + " RuntimeAdditionalSupport2";
                String ttt = System.getProperty("vm.boot.class.path") +(System.getProperty("java.class.path").length() > 0 ? 
                        File.pathSeparator + System.getProperty("java.class.path") : "");
                cmnd2 = RuntimeAdditionalTest0.javaStarter+" -Xbootclasspath/a:" + ttt + " -cp " + ttt + " java.lang.RuntimeAdditionalSupport2";
                /**/System.out.println("|" + cmnd2 + "|");
            } else {
                fail("WARNING (test_41): unknown operating system.");
            }
            try {
                /**/System.out.println("thread11: 1");
                ppp44 = Runtime.getRuntime().exec(cmnd2);
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
                try {
                    ppp44.exitValue();
                    System.out
                            .println("WARNING (test_41): java-pipe does not work.");
                } catch (IllegalThreadStateException e) {
                }
                java.io.OutputStream os4 = ppp44.getOutputStream();
                ppp44.getErrorStream();
                java.io.InputStream is4 = ppp44.getInputStream();
                /**/System.out.println("thread11: 2");

                int ia;
                while (true && !flg33) {
                    if (flg22) {
                        flg22 = false;
                        if (stage2 == 0) {
                            /**/System.out.println("thread11: stage1 - 1");
                            os4.write('Z');
                            os4.write(10);
                            stage1 = 1;
                            flg0 = true;
                        } else if (stage2 == 1) {
                            /**/System.out.println("thread11: stage1 - 2");
                            os4.flush();
                            stage1 = 2;
                            flg0 = true;
                        } else if (stage2 == 2) {
                            /**/System.out.println("thread11: stage1 - 3");
                            os4.write('Z');
                            os4.write('S');
                            os4.write('S');
                            os4.write(10);
                            os4.flush();
                            stage1 = 3;
                            flg0 = true;
                        } else if (stage2 == 3) {
                            /**/System.out.println("thread11: stage1 - 4");
                            byte[] bbb = new byte[] { 'S', 'e', 'r', 'g', 'u',
                                    'e', 'i', ' ', 'S', 't', 'e', 'p', 'a',
                                    'n', 'o', 'v', 'i', 'c', 'h' };
                            stage1 = 4;
                            flg0 = true;
                            int iii = 0;
                            while (count < 5 && !flg33) {
                                while (iii < 1000/*0*/) {
                                    ///**/System.out.println(count+"|"+flg3);
                                    os4.write(bbb);
                                    os4.write(10);
                                    iii++;
                                    try {
                                        Thread.sleep(2);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            stage1 = 5;
                            flg0 = true;
                            os4.write(3);
                            while ((ia = is4.available()) != 0) {
                                bbb = new byte[ia];
                                is4.read(bbb);
                                count++;
                                try {
                                    Thread.sleep(2);
                                } catch (Exception e) {
                                }
                            }
                            /**/System.out.println("------------------------------------");
                            ppp44.destroy();
                            ppp44.waitFor();
                            /**/System.out.println("------------------------------------");
                            break;
                        }
                    }
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }

            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_41, thread11): unexpected exception.");
            }
        }

        protected void finalize() throws Throwable {
            super.finalize();
            /**/System.out.println("thread11: finalize");
            System.out.flush();
        }
    }

    class thread22 extends Thread {
        public void run() {
            if (RuntimeAdditionalTest0.os.equals("Win") || RuntimeAdditionalTest0.os.equals("Lin")) {
            } else {
                fail("WARNING (test_41): unknown operating system.");
            }
            try {
                /**/System.out.println("thread22: 1");
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                }
                java.io.OutputStream os4 = ppp44.getOutputStream();
                java.io.InputStream es4 = ppp44.getErrorStream();
                java.io.InputStream is4 = ppp44.getInputStream();
                /**/System.out.println("thread22: 2");
                ///\\\while (es4.available() == 0) {}
                byte[] bbb5 = new byte[is4.available()];
                is4.read(bbb5);
                /**/System.out.println(new String(bbb5));
                ///\\\while (is4.available() == 0) {}
                /**/System.out.println("thread22: 21");
                Thread.sleep(100);
                bbb5 = new byte[is4.available()];
                is4.read(bbb5);
                /**/System.out.println(new String(bbb5));
                /**/System.out.println("thread22: 22");

                //special test case (zero array):
                byte[] bbb6 = new byte[0];
                is4.read(bbb6);
                os4.write(bbb6);
                /**/System.out.println("thread22: 23");

                flg22 = true;
                int ia;
                while (true) {
                    if (flg0) {
                        int ii = 0;
                        flg0 = false;
                        if (stage1 == 1) {
                            /**/System.out.println("thread22: stage2 - 1");
                            while ((ia = es4.available()) == 0 && ii < 500) {
                                ii++;
                                try {
                                    Thread.sleep(1);
                                } catch (Exception e) {
                                }
                            }
                            if ((ia = is4.available()) != 0) {
                                System.out
                                        .println("ERROR (test_41, thread22): Nothing should be readable before flush.");
                                /**/System.out.println(ia);
                                while (is4.available() != 0) {
                                    os4.flush();
                                    /*System.out.println("=" + */is4.available()/*)*/;
                                    /*System.out.println("|"
                                            + Character.toString((char) */is4
                                                    .read()/*) + "|")*/;
                                }
                                System.out
                                        .println("######################################################");
                                System.out
                                        .println("## test_41 WARNING: unbuffered stream is diagnosed. ##");
                                System.out
                                        .println("######################################################");
                                flg33 = true;
                                return;
                            }
                            stage2 = 1;
                            flg22 = true;
                        } else if (stage1 == 2) {
                            /**/System.out.println("thread22: stage2 - 2");
                            if ((ia = is4.available()) != 2
                                    && is4.read() != 'Z' && is4.read() != 10) {
                                System.out
                                        .println("ERROR (test_41, thread22): Z'10' should be to read.");
                                flg33 = true;
                                return;
                            }
                            stage2 = 2;
                            flg22 = true;
                        } else if (stage1 == 3) {
                            /**/System.out.println("thread22: stage2 - 3");
                            if ((ia = is4.available()) != 4
                                    && is4.read() != 'Z' && is4.read() != 'S'
                                    && is4.read() != 'S' && is4.read() != 10) {
                                System.out
                                        .println("ERROR (test_41, thread22): ZSS'10' should be to read.");
                                //ppp44.destroy();
                                flg33 = true;
                                return;
                            }
                            stage2 = 3;
                            flg22 = true;
                        } else if (stage1 == 4) {
                            /**/System.out.println("thread22: stage2 - 4");
                            while ((ia = is4.available()) != 0 || stage1 != 5) {
                                /**/System.out.println("thread22 length of reading: "+ia);
                                byte[] bbb = new byte[ia];
                                is4.read(bbb);
                                count++;
                                try {
                                    Thread.sleep(2);
                                } catch (Exception e) {
                                }
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                            }
                            /**/System.out.println("thread22: " + count);
                            System.out.println("#####################");
                            System.out.println("## test_41 PASSED. ##");
                            System.out.println("#####################");
                            flg33 = true;
                            return;
                        }
                    }
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                    }
                }

            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_41, thread22): unexpected exception.");
            }
        }

        protected void finalize() throws Throwable {
            super.finalize();
            /**/System.out.println("thread22: finalize");
            System.out.flush();
        }
    }

    static Process ppp44 = null;

    static int count = 0;

    static int stage1 = 0;

    static int stage2 = 0;

    static boolean flg0 = false;

    static boolean flg22 = false;

    static boolean flg33 = false;

    public void test_41() {
        System.out.println("==test_41===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_41): unknown operating system.");
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            return;
        }

        Runtime.runFinalizersOnExit(true);
        Thread t1 = null;
        Thread t2 = null;
        try {
            t1 = new thread11();
            t1.start();
            t2 = new thread22();
            t2.start();
            //t2.join();
            //t1.join();
            while (!flg33) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                }
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            ppp44.destroy();
            ppp44.waitFor();
            /**/System.out.println("== joined ===");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ppp44.destroy();
            } catch (Exception eee) {
                eee.printStackTrace();
                return;
            }
        }
    }
}