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

public class RuntimeAdditionalTest33 extends TestCase {
    /**
     * create two cat-process, multi_byte-read from one multi_byte-write (using
     * flushing) to another and multi_byte-read there again
     */
    public void test_32() {
        System.out.println("==test_32===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\WINNT\\system32\\cmd.exe\"";
            //\\//\\cmnd1 = RuntimeAdditionalTest0.cm+" /C cat \"C:\\Documents and Settings\\All
            // Users\\Documents\\My Pictures\\Sample Pictures\\Winter.jpg\"";
            cmnd1 = RuntimeAdditionalTest0.cm + " /C cat \"" + RuntimeAdditionalTest0.libFile + "\"";
            cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd1 = "sh -c sh -c \"cat -v /lib/ld.so\"";
            //cmnd1 = "cat -v /lib/ld.so";
            //\\//\\cmnd1 = "cat -v /bin/echo";
            cmnd1 = "cat -v \"" + RuntimeAdditionalTest0.libFile + "\"";
            cmnd2 = "sh -c  cat";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }

        try {

            Process pi3 = Runtime.getRuntime().exec(cmnd1);
            //Process pi3 = Runtime.getRuntime().exec(cmnd+" /C cat
            // \"C:\\IJE\\orp\\ZSS\\___LOG2.DO_VM_CLEAN no\"");
            //Process pi3 = Runtime.getRuntime().exec(cmnd+" /C tree");
            Process pi4 = Runtime.getRuntime().exec(cmnd2);
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

            int ia;
            while (true) {
                while ((ia = is.available()) != 0) {
                    byte[] bbb = new byte[ia];
                    is.read(bbb);
                    os4.write(bbb);
                    while (ia != is4.available()) {
                        os4.flush();
                    }
                    while ((ia = is4.available()) != 0) {
                        byte[] bbb4 = new byte[ia];
                        is4.read(bbb4);
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
                    continue;
                }
            }

        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_32): unexpected exception.");
        }
		try{
            //cmnd1 = RuntimeAdditionalTest0.cm + " /C ps -ef";
			cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?RuntimeAdditionalTest0.cm + " /C ps -Ws":RuntimeAdditionalTest0.cm + " -c \"ps -ef\"";
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
					cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?RuntimeAdditionalTest0.cm + " /C kill "+as[i]:RuntimeAdditionalTest0.cm + " -c \"kill "+as[i]+"\"";
					Runtime.getRuntime().exec(cmnd1);
					Thread.sleep(3000);
				}
			}
		}catch(Exception e){
			fail("ERROR (test_32): unexpected exception: "+e.toString());
		}
    }
}