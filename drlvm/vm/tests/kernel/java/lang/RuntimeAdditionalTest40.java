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

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */

public class RuntimeAdditionalTest40 extends TestCase {
    /**
     * create thread with two cat-process multi_byte-interconnected create two
     * thread waiting these processes correspondingly interrupt the last two
     * process to provide InterruptedException rising
     */
    class thread1 extends Thread {
        public void run() {
            String cmnd1 = null;
            String cmnd2 = null;
            if (RuntimeAdditionalTest0.os.equals("Win")) {
                //cmnd1 = RuntimeAdditionalTest0.cm + " /C tree \"C:\\Documents and Settings\"";
                //cmnd1 = "C:\\WINNT\\system32\\tree.com \"C:\\Documents and Settings\"";
                /**/cmnd1 = RuntimeAdditionalTest0.treeStarter+" \"C:\\Documents and Settings\"";
                //cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
                //cmnd2 = "C:\\CygWin\\bin\\cat.exe";
                /**/cmnd2 = RuntimeAdditionalTest0.catStarter;
            } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
                //cmnd1 = "/usr/bin/tree /";
                /**/cmnd1 = RuntimeAdditionalTest0.treeStarter+" /";
                //cmnd1 = "/usr/bin/tree /lib";
                //cmnd1 = "/usr/bin/tree /bin";
                //cmnd2 = "/bin/sh -c  cat";
                cmnd2 = RuntimeAdditionalTest0.catStarter;
            } else {
                fail("WARNING (test_1): unknown operating system.");
            }
            try {
                //System.out.println("thread1: 1");
  				//**/RuntimeAdditionalTest0.doMessage("thread1: 1\n");
                ppp3 = Runtime.getRuntime().exec(cmnd1);
                ppp4 = Runtime.getRuntime().exec(cmnd2);
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
                ppp3.getOutputStream();
                ppp3.getErrorStream();
                java.io.InputStream is = ppp3.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String procValue = null;
  		//**/RuntimeAdditionalTest0.doMessage(cmnd1);
                java.io.OutputStream os4 = ppp4.getOutputStream();
                ppp4.getErrorStream();
                java.io.InputStream is4 = ppp4.getInputStream();
                //**/System.out.println("thread1: 2");
  		//**/RuntimeAdditionalTest0.doMessage("thread1: 2\n");

                int ia;
                int ii = 0;
                while (true) {
                    while ((ia = is.available()) != 0) {
  		//**/RuntimeAdditionalTest0.doMessage("XXXXXXXXXXXXXX1!\n");
                        byte[] bbb = new byte[ia];
                        is.read(bbb);
                        os4.write(bbb);
  		//**/RuntimeAdditionalTest0.doMessage("XXXXXXXXXXXXXX2!\n");
                        while (ia != is4.available()) {
                            os4.flush();
                        }
   		//**/RuntimeAdditionalTest0.doMessage("XXXXXXXXXXXXXX3!\n");
                       while ((ia = is4.available()) != 0) {
                            byte[] bbb4 = new byte[ia];
                            is4.read(bbb4);
                            //\\//\\System.out.println(new String(bbb4));
                        }
  		//**/RuntimeAdditionalTest0.doMessage("XXXXXXXXXXXXXX4!\n");
                        ii++;
                        //if (ii % 500 == 0) System.out.println("thread1: 3");
                        flg = true; //####################################################
                        try {
                            Thread.sleep(20);
                        } catch (Exception e) {
                        }
                    }
                    try {
                        //**/System.out.println("thread1: 4");
  		//**/RuntimeAdditionalTest0.doMessage("thread1: 4\n");
                        ppp3.exitValue();
                        while ((ia = is.available()) != 0) {
                            byte[] bbb = new byte[ia];
                            is.read(bbb);
                            os4.write(bbb);
                            while ((ia = is4.available()) != 0) {
                                byte[] bbb4 = new byte[ia];
                                is4.read(bbb4);
                                //\\//\\System.out.println(new String(bbb4));
                            }
                        }
                        break;
                    } catch (IllegalThreadStateException e) {
                        continue;
                    }
                }

            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_38, thread1): unexpected exception.");
            }
        }

        protected void finalize() throws Throwable {
            super.finalize();
            //System.out.println("thread1: finalize");
  		//**/RuntimeAdditionalTest0.doMessage("thread1: finalize\n");
            System.out.flush();
        }
    }

    class thread2 extends Thread {
        public void run() {
            //System.out.println("thread2: 1");
  		//**/RuntimeAdditionalTest0.doMessage("thread2: 1\n");
            while (ppp3 == null) {
                try {
                    Thread.sleep(10);
                } catch (Exception _) {
                }
            }
            //System.out.println("thread2: 2");
  		//**/RuntimeAdditionalTest0.doMessage("thread2: 2\n");
            try {
                ppp3.waitFor();
            } catch (InterruptedException _) {
                flg2 = true;
                //System.out.println("InterruptedException 2");
  		//**/RuntimeAdditionalTest0.doMessage("InterruptedException 2\n");
            }
            //System.out.println("thread2: 3");
  		//**/RuntimeAdditionalTest0.doMessage("thread2: 3\n");
        }

        protected void finalize() throws Throwable {
            super.finalize();
            //**/System.out.println("thread2: finalize");
  		//**/RuntimeAdditionalTest0.doMessage("thread2: finalize\n");
            System.out.flush();
        }
    }

    class thread3 extends Thread {
        public void run() {
            //System.out.println("thread3: 1");
  		//**/RuntimeAdditionalTest0.doMessage("thread3: 1\n");
            while (ppp4 == null) {
                try {
                    Thread.sleep(10);
                } catch (Exception _) {
                }
            }
            //System.out.println("thread3: 2");
  		//**/RuntimeAdditionalTest0.doMessage("thread3: 2\n");
            try {
                ppp4.waitFor();
            } catch (InterruptedException _) {
                flg3 = true;
                //System.out.println("InterruptedException 3");
            }
            //System.out.println("thread3: 3");
  		//**/RuntimeAdditionalTest0.doMessage("thread3: 3\n");
        }

        protected void finalize() throws Throwable {
            super.finalize();
            //System.out.println("thread2: finalize");
  		//**/RuntimeAdditionalTest0.doMessage("thread2: finalize\n");
            System.out.flush();
        }
    }

    static Process ppp3 = null;

    static Process ppp4 = null;

    static boolean flg = false;

    static boolean flg2 = false;

    static boolean flg3 = false;

    public void test_38() {
		System.out.println("==test_38===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_2): unknown operating system.");
        }
        Runtime.runFinalizersOnExit(true);
        Thread t1 = null;
        Thread t2 = null;
        Thread t3 = null;
        try {
            t1 = new thread1();
            t1.start();
            while (!flg) {
                try {
                    Thread.sleep(10);
                } catch (Exception _) {
                }
            }
            t2 = new thread2();
            t2.start();
            try {
                Thread.sleep(100);
            } catch (Exception _) {
            }
            t3 = new thread3();
            t3.start();
            try {
                Thread.sleep(100);
            } catch (Exception _) {
            }
            t2.interrupt();
            //System.out.println("== interrupt t2 ===");
  		//**/RuntimeAdditionalTest0.doMessage("== interrupt t2 ===\n");
            //t2.join();
            t3.interrupt();
            //System.out.println("== interrupt t3 ===");
  		//**/RuntimeAdditionalTest0.doMessage("== interrupt t3 ===\n");
            //t3.join();
            ppp3.destroy();
            ppp4.destroy();
            t1.interrupt();
            t3.join();
            t2.join();
            t1.join();
            //try{Thread.sleep(5000);}catch(Exception _){}
            //System.out.println("== joined ===");
  		//**/RuntimeAdditionalTest0.doMessage("== joined ===\n");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ppp3.waitFor();
            } catch (InterruptedException ee) {
                ee.printStackTrace();
                try {
                    ppp4.waitFor();
                } catch (InterruptedException eee) {
                    eee.printStackTrace();
                    //System.out.println(flg2 + "." + flg3);
  		//**/RuntimeAdditionalTest0.doMessage(Boolean.valueOf(flg2) + "." + Boolean.valueOf(flg3));
                    return;
                }
            }
        }
        if (!(flg2 && flg3)) {
            ppp3.destroy();
            ppp4.destroy();
            t1.interrupt();
            //t1.destroy();
            t2.interrupt();
            //t2.destroy();
            t3.interrupt();
            //t3.destroy();
            fail("ERROR (test_38): InterruptedException should be risen.");
        } else {
            System.out.println("############################");
            System.out.println("## test_38 PASSED anyway. ##");
            System.out.println("############################");
        }
			RuntimeAdditionalTest0.killCat();
    }
}