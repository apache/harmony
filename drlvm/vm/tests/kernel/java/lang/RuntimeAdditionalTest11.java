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

public class RuntimeAdditionalTest11 extends TestCase {

    /**
     * read, write info using streams of two process (cat jpg/so <->cat) using
     * the "exitValue - IllegalThreadStateException" loop to try to read p1-out
     * to write p2-in and to read p2-out again
     */
    public void test_11_1() {
        System.out.println("==test_11_1===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\WINNT\\system32\\cmd.exe\"";
            //\\//\\cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\Documents and Settings\\All
            // Users\\Documents\\My Pictures\\Sample Pictures\\Winter.jpg\"";
            //cmnd1 = RuntimeAdditionalTest0.cm + " /C cat \"" + RuntimeAdditionalTest0.libFile + "\"";
            //cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
            cmnd1 = RuntimeAdditionalTest0.catStarter + " \"" + RuntimeAdditionalTest0.libFile + "\"";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd = "sh -c \"cat -v /lib/ld.so\"";
            //\\//\\cmnd1 = "cat -v /lib/ld.so";
            //cmnd1 = "cat -v \"" + RuntimeAdditionalTest0.libFile + "\"";
            //cmnd2 = "sh -c  cat";
            //cmnd1 = RuntimeAdditionalTest0.catStarter + " -v \"" + RuntimeAdditionalTest0.textFile + "\"";
            cmnd1 = "/bin/sh -c \"cat " + RuntimeAdditionalTest0.textFile + "\"";
            cmnd1 = "/bin/cat \"" + RuntimeAdditionalTest0.textFile + "\"";
            cmnd1 = "/bin/cat " + RuntimeAdditionalTest0.textFile;
            //cmnd1 = "/usr/bin/pr " + RuntimeAdditionalTest0.textFile;
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }

        try {
            Process pi3 = Runtime.getRuntime().exec(cmnd1);RuntimeAdditionalTest0.doMessage(cmnd1+"\n");//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
			        //if(System.getProperty("java.vm.name").indexOf("DRL")!=-1){RuntimeAdditionalTest0.killCat();int i=0,j=0; i=i/j;}
            //Process pi4 = Runtime.getRuntime().exec(cmnd2);//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            //try {
            //    Thread.sleep(2000);
            //} catch (Exception e) {
            //}
            //pi3.getOutputStream();
            //pi3.getErrorStream();
				
			java.io.InputStream is = pi3.getInputStream();
			///**/RuntimeAdditionalTest0.doMessage("1:"+cmnd1+"\n");
			while ((is.available()) == 0) {RuntimeAdditionalTest0.doMessage("1\n");}//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            Process pi4 = Runtime.getRuntime().exec(cmnd2);//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            java.io.OutputStream os4 = pi4.getOutputStream();
            pi4.getErrorStream();
            java.io.InputStream is4 = pi4.getInputStream();

            int ia;
            int ia3 = 0;
            while (true) {RuntimeAdditionalTest0.doMessage(Integer.toString(is.available(), 10)+"1\n");
                while ((ia = is.available()) != 0) {
					byte[] bbb = new byte[ia];
                    is.read(bbb);
//System.out.println(new String(bbb));
//RuntimeAdditionalTest0.doMessage("2:"+new String(bbb)+"\n");
					os4.write(bbb);
                    int ia2 = 0;
                    while (ia != is4.available()) {
///////////////////////////////////////////////////////////////////////////////////////
/**/                        os4.flush(); //NU, JRK POGODI!
///////////////////////////////////////////////////////////////////////////////////////
                        ia2++;			
                        if (ia2 > 5) {
							System.out.println("Warning (test_11): something wrong in the test - to investigate.");
							//RuntimeAdditionalTest0.killCat();
							break;//return; 
                        }
                    }
                    while ((ia = is4.available()) != 0) {
                        byte[] bbb4 = new byte[ia];
                        is4.read(bbb4);
System.out.println("zz|"+new String(bbb4)+"|zz");
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
					if(ia3++>100){RuntimeAdditionalTest0.killCat();return;}
                    continue;
                }
            }
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_11): unexpected exception.");
        }
		RuntimeAdditionalTest0.killCat();
    }
}