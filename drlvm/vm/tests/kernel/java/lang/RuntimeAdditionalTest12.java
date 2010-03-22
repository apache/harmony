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

public class RuntimeAdditionalTest12 extends TestCase {
    /**
     * read, write info using streams of two process (cat txt/so <->cat) using
     * the "exitValue - IllegalThreadStateException" loop to try to read p1-out
     * to write p2-in and to read p2-out again
     */
    public void test_12() {
        System.out.println("==test_12===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\WINNT\\system32\\cmd.exe\"";
            //\\//\\cmnd1 = RuntimeAdditionalTest0.cm+" /C cat
            // \"C:\\IJE\\orp\\ZSS\\___LOG2.DO_VM_CLEAN no\"";
            cmnd1 = RuntimeAdditionalTest0.cm + " /C cat \"" + RuntimeAdditionalTest0.textFile + "\"";
            cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd = "sh -c \"cat -v /lib/ld.so\"";
            //\\//\\cmnd1 = "cat -v /lib/ld.so";
            cmnd1 = "/bin/cat -v \"" + RuntimeAdditionalTest0.libFile + "\"";
            cmnd2 = "/bin/sh -c  cat";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }

        Process pi3 = null;
        Process pi4 = null;
        java.io.OutputStream os = null;
        java.io.InputStream es = null;
        java.io.InputStream is = null;
        java.io.OutputStream os4 = null;
        java.io.InputStream es4 = null;
        java.io.InputStream is4 = null;
        try {
            //Process pi3 = Runtime.getRuntime().exec(cmnd+" /C cat
            // \"C:\\Documents and Settings\\All Users\\Documents\\My
            // Pictures\\Sample Pictures\\Winter.jpg\"");
            pi3 = Runtime.getRuntime().exec(cmnd1);
            pi4 = Runtime.getRuntime().exec(cmnd2);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            os = pi3.getOutputStream();
            es = pi3.getErrorStream();
            is = pi3.getInputStream();
            os4 = pi4.getOutputStream();
            es4 = pi4.getErrorStream();
            is4 = pi4.getInputStream();

            int ia;
            while (true) {
                while ((ia = is.available()) != 0) {
                    ///System.out.println(111);
                    byte[] bbb = new byte[ia];
                    is.read(bbb);
                    ///System.out.println(111.1);
                    os4.write(bbb);
                    int ia2 = 0;
                    while (ia != is4.available()) {
                        os4.flush();
                        //System.out.println(111.3);
                        ia2++;
                        if (ia2 > 10000) {
                            fail("ERROR (test_12): something wrong in the test - to investigate.");
                        }
                    }
                    ///System.out.println(111.2);
                    while ((ia = is4.available()) != 0) {
                        ///System.out.println(222);
                        byte[] bbb4 = new byte[ia];
                        is4.read(bbb4);
                        //\\//\\System.out.println(new String(bbb4));
                    }
                }
                try {
                    pi3.exitValue();
                    while ((ia = is.available()) != 0) {
                        ///System.out.println(333);
                        byte[] bbb = new byte[ia];
                        is.read(bbb);
                        os4.write(bbb);
                        while ((ia = is4.available()) != 0) {
                            //System.out.println(444);
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
            fail("ERROR (test_12): unexpected exception.");
        }
        if (!RuntimeAdditionalSupport1.openingFlag) { // to remember about the issue in Process.destroy() implementation
			try{
				os.close();
				es.close();
				is.close();
				os4.close();
				es4.close();
				is4.close();
				pi3.destroy();
				pi4.destroy();
				pi3.exitValue();
				pi4.exitValue();
				cmnd1 = RuntimeAdditionalTest0.cm + " /C ps -ef";
				Process pi5 = Runtime.getRuntime().exec(cmnd1);
				BufferedReader br = new BufferedReader(new InputStreamReader(pi5
                    .getInputStream()));
				boolean flg = true;
				String procValue = null;
				while ((procValue = br.readLine()) != null) {
					if (procValue.indexOf("cat") != -1) {
						System.out.println(111);
						flg = false;
						break;
					}
				}
				if (flg) {
					return;
				}
				String as[] = procValue.split(" ");
				for (int i = 0; i < as.length; i++) {
					if(as[i].matches("\\d+")){
						System.out.println(222);
						cmnd1 = RuntimeAdditionalTest0.cm + " /C kill "+as[i];
						Runtime.getRuntime().exec(cmnd1);
						Thread.sleep(3000);
					}
				}
			}catch(Exception e){
				fail("ERROR (test_14): unexpected exception: "+e.toString());
			}
		}
    }
}