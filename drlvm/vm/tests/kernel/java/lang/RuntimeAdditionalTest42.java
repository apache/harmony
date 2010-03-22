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

public class RuntimeAdditionalTest42 extends TestCase {
    /**
     *  create tree-process and cat-process, organize their multi_byte-interconnection using flushing
     */
    public void test_40() { 
        System.out.println("==test_40===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm + " /C tree \"C:\\Documents and Settings\"";
            //cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
            cmnd1 = RuntimeAdditionalTest0.treeStarter+" \"C:\\Documents and Settings\"";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd1 = "sh -c  \"tree /lib\"";
            //cmnd1 = "/usr/bin/tree /lib";
            //cmnd1 = "/usr/bin/tree /bin";
            //cmnd2 = "sh -c  cat";
            cmnd1 = RuntimeAdditionalTest0.treeStarter+" /bin";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else {
            fail("WARNING (test_40): unknown operating system.");
        }
        try {

            Process pi3 = Runtime.getRuntime().exec(cmnd1);
            //Process pi4 = Runtime.getRuntime().exec(cmnd2);//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
			while ((is.available()) == 0) {RuntimeAdditionalTest0.doMessage("1\n");}//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
			//RuntimeAdditionalTest0.doMessage("2\n");
            Process pi4 = Runtime.getRuntime().exec(cmnd2);//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            java.io.OutputStream os4 = pi4.getOutputStream();
            pi4.getErrorStream();
            java.io.InputStream is4 = pi4.getInputStream();

            int ia;
            int ii = 0;
            int ia3=0;
            int ia4=0;
            while (true) {
                while ((ia = is.available()) != 0) {
                    if (RuntimeAdditionalTest0.os.equals("Win")) {
                        //System.out.println(1);
                        byte[] bb = new byte[5];
                        is.read(bb, 1, 2);
                        bb[3] = 10;
                        os4.write(bb, 1, 3);
                        os4.flush();
                        while ((ia = is4.available()) != 3) {/*System.out.println(3+"|"+ia);*/
							if(ia4++>10000){
								ia4 = 0;
								System.out.println("Warning (test_40): something wrong? We are waiting 3 bytes here as were written.");
								break;
							}
                        }
                        if (ia > 3) {
                            byte[] bbb = new byte[ia];
                            is4.read(bbb);
                            //fail("ERROR (test_40): flush() problem.");
							System.out.println("Warning (test_40): flush() problem has been detected! We are waiting 3 bytes here as were written.");
                        }
                        if(is4.available()!=0)/*System.out.print(Character
                                .toString((char) */is4.read()/*))*/;
                        if(is4.available()!=0)/*System.out.print(Character
                                .toString((char) */is4.read()/*))*/;
                        if(is4.available()!=0) is4.read();
                    } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
                        //System.out.println(1);
                        byte[] bb = new byte[5];
                        is.read(bb, 1, 2);
                        bb[3] = 10;
                        os4.write(bb, 1, 3);
                        os4.flush();
                        while ((ia = is4.available()) == 0) {/*System.out.println(3+"|"+ia);*/
                        } //due to our available() impl
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }
                        //	System.out.println(ia);
                        //	System.out.println(is4.available());
                        /*System.out.println(*/is4.read(bb, 1, 3)/*)*/;
                        //	System.out.println(new String(bb, 1, 3));
                        ///**/System.out.print(Character.toString((char)/**/is4.read()/**/))/**/;
                        //while((ia = is4.available()) == 0) {System.out.println(3+"|"+ia);} //due to our available() impl
                        ///**/System.out.print(Character.toString((char)/**/is4.read()/**/))/**/;
                        //while((ia = is4.available()) == 0) {System.out.println(3+"|"+ia);} //due to our available() impl
                        //is4.read();
                        //	System.out.println(is4.available());
                        if (is4.available() > 0) {
                            byte[] bbb = new byte[ia];
                            is4.read(bbb);
                            fail("ERROR (test_40): flush() problem.|"
                                    + new String(bbb) + "|");
                        }

                        try {
                            Thread.sleep(10);
                        } catch (Exception e) {
                        }
                        ii++;
                        if (ii > 1500) {
                            return;
                        }
                    }
                }
                try {
                    pi3.exitValue();
                    while ((ia = is.available()) != 0) {
                        os4.write(is.read());
                        if ((ia = is4.available()) == 0) {
                            os4.flush();
                            if (is4.available() != 1) {
                                fail("ERROR (test_40): 3.");
                            }
                        } else if (ia > 1) {
                            fail("ERROR (test_40): 4.");
                        }
                        is4.read();
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
            fail("ERROR (test_40): unexpected exception.");
        }
    }
}