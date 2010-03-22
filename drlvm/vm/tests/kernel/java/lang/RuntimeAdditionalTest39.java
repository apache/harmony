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
 */

package java.lang;

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
 * TODO: 1. The following issue should be investigated:
 * if we have uncommented "BUG: DRL is hanging here"-marked lines then the test fails on DRLVM on LINUX.
 * So, it looks like the bug in DRLVM ("writing into the input stream of unwaiting process spoils the receiving 
 * its output").
 * ###############################################################################
 * ###############################################################################
 */

public class RuntimeAdditionalTest39 extends TestCase {
    /**
     * creat two cat-process, multi_byte-read from one multi_byte-write (using
     * flushing) to another and multi_byte-read there again disturb this process
     * by writing some unwaited info
     */
    public void test_37() { 
        System.out.println("==test_37===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm + " /C tree \"C:\\Documents and Settings\"";
            //cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
            cmnd1 = RuntimeAdditionalTest0.treeStarter+" \"C:\\Documents and Settings\"";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd1 = "sh -c \"tree /lib\"";
            //cmnd1 = "/usr/bin/tree /lib";
            //cmnd1 = "/usr/bin/tree /bin";
            //cmnd2 = "sh -c  cat";
            cmnd1 = RuntimeAdditionalTest0.treeStarter+" /bin";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }
        try {

            Process pi3 = Runtime.getRuntime().exec(cmnd1);
            //Process pi4 = Runtime.getRuntime().exec(cmnd2);//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            java.io.OutputStream os = pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
			while ((is.available()) == 0) {RuntimeAdditionalTest0.doMessage("1\n");}//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
			//RuntimeAdditionalTest0.doMessage("2\n");
            Process pi4 = Runtime.getRuntime().exec(cmnd2);//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            java.io.OutputStream os4 = pi4.getOutputStream();
            pi4.getErrorStream();
            java.io.InputStream is4 = pi4.getInputStream();
            int ia;
            int ia3=0;
            while (true) {
               while ((ia = is.available()) != 0) {
                   byte[] bbb = new byte[ia];
                    is.read(bbb);
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                    os.write(bbb); //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< !!!!!!!!!!! BUG: DRL is hanging here !!!!!!!
//                                   // write into the unwaiting
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    os4.write(bbb);
                    int ia2 = 0;
                    //while (ia != is4.available()) {
                    //    os4.flush();
                    //    ia2++;
                    //    if (ia2 > 100) {
                    //        System.out.println("Warning (test_37): something wrong? We are waiting about the same number of bytes as were written.");
					//		break;
                    //    }
                    //}
                   while ((ia = is4.available()) != 0) {
                        byte[] bbb4 = new byte[ia];
                        is4.read(bbb4);
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                        os.write(bbb4); //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< !!!!!!!!!!! BUG: DRL is hanging here !!!!!!!
//                                       // write into the unwaiting
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        //\\//\\System.out.println(new String(bbb4));
                    }
               }
               try {
                    pi3.exitValue();
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
			        if(ia3++>1000) break;
                    continue;
                }
            }
			RuntimeAdditionalTest0.killTree();
			RuntimeAdditionalTest0.killCat();
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_37): unexpected exception.");
        }
    }
}