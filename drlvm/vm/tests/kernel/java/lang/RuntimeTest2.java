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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

import junit.framework.TestCase;

/**
 * This RuntimeTest class is used to test the Core API Runtime class
 * Created on January 5, 2005
 * 
 * ###############################################################################
 * ###############################################################################
 * REMINDER("XXX") LIST:
 * 1. [Jun 11, 2005] test_availableProcessors, test_freeMemory, test_gc, test_runFinalization, 
 *    test_runFinalizersOnExit fail on "ORP+our Runtime+CLASSPATH API" platform
 *    because the availableProcessors, freeMemory, runFinalization (runFinalizersOnExit?)
 *    methods aren't correctly supported yet in orp/drl_natives/src
 * 2. [Jun 11, 2005] test_maxMemory, test_totalMemory fail on "ORP+CLASSPATH API" platform
 *    because the maxMemory
 *    method isn't correctly supported yet in orp/drl_natives/src:
 *    (Exception: java.lang.UnsatisfiedLinkError: Error compiling method java/lang/Runtime.maxMemory()J)
 * 3. [Jun 11, 2005] test_availableProcessors fails on "ORP+CLASSPATH API" platform
 *    because the availableProcessors
 *    method isn't correctly supported yet in orp/drl_natives/src:
 *    (Exception: java.lang.UnsatisfiedLinkError: Error compiling method java/lang/Runtime.availableProcessors()I)
 * ###############################################################################
 * ###############################################################################
 */
public class RuntimeTest2 extends TestCase {

    static class forInternalUseOnly {
        String stmp;

        forInternalUseOnly () {
            this.stmp = "";
            for (int ind2 = 0; ind2 < 100; ind2++) {
                this.stmp += "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
            }
        }
        protected void finalize() throws Throwable {
            runFinalizationFlag = true;
            super.finalize();
        }
    }

    static boolean runFinalizationFlag = false;

    public void test_runFinalization() throws InterruptedException {
        runFinalizationFlag = false;

        for (int ind2 = 0; ind2 < 10; ind2++) {
            forInternalUseOnly ins = new forInternalUseOnly();
            ins.stmp += "";
            ins = null;
            Thread.sleep(10);
            Runtime.getRuntime().gc();
            Thread.sleep(10);
            Runtime.getRuntime().runFinalization();
        }

        assertTrue("finalization has not been run", runFinalizationFlag);
    }

    @SuppressWarnings("deprecation")
    public void test_runFinalizersOnExit() throws InterruptedException {
        runFinalizationFlag = false;
        for (int ind2 = 0; ind2 < 5; ind2++) {
            Runtime.runFinalizersOnExit(false);
            forInternalUseOnly ins = new forInternalUseOnly();
            ins.stmp += "";
            ins = null;
            Thread.sleep(10);
            Runtime.getRuntime().gc();
            Thread.sleep(10);
        }

        assertTrue("check001: finalizers were not run", runFinalizationFlag);

        runFinalizationFlag = false;
        for (int ind2 = 0; ind2 < 5; ind2++) {
            Runtime.runFinalizersOnExit(true);
            forInternalUseOnly ins = new forInternalUseOnly();
            ins.stmp += "";
            ins = null;
            Thread.sleep(10);
            Runtime.getRuntime().gc();
            Thread.sleep(10);
        }
        assertTrue("check002: finalizers were not run", runFinalizationFlag);
    }

    class threadForInternalUseOnly1 extends Thread {
        public void run() {
            int I = threadForInternalUseOnly2.getI();
            int counter = 0;
            while ((I < 50 || number < I) && counter < 24000) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // ignore interruption request
                    // so reset interrupt indicator
                    this.interrupt();
                }
                I = threadForInternalUseOnly2.getI();
                counter += 1;
            }
        }

        protected void finalize() throws Throwable {
            if (runFinalizationFlag2 == 1 || runFinalizationFlag2 == 11
                    || runFinalizationFlag2 == 21) {
                // :) // assertTrue( "FAILED: addShutdownHook.check001", false);
            }
            super.finalize();
        }
    }

    static class threadForInternalUseOnly2 extends Thread {
        static int I = 0;
        long NM;
        int ORD;

        synchronized static void incrI() {
            I++;
        }

        synchronized static int getI() {
            return I;
        }

        threadForInternalUseOnly2(int ind) {
            super();
            NM = System.currentTimeMillis();
            ORD = ind;
        }

        public void run() {
            if (ORD == 1 || ORD == 11 || ORD == 21) {
                synchronized (threadForInternalUseOnly2.class) {
                    runFinalizationFlag2 = ORD;
                }
            }
            incrI();
            for (int j = 0; j < 30/* 100 */; j++) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // ignore request, set indicator
                    this.interrupt();
                }
            }
        }

        protected void finalize() throws Throwable {
            if (runFinalizationFlag2 == 1 || runFinalizationFlag2 == 11
                    || runFinalizationFlag2 == 21) {
                // :) // assertTrue( "FAILED: addShutdownHook.check002", false);
            }
            super.finalize();
        }
    }

    static class threadForInternalUseOnly3 extends Thread {
        static int I = 0;

        synchronized static void incrI() {
            I++;
        }

        synchronized static int getI() {
            return I;
        }

        public void run() {
            incrI();
        }
    }

    static int runFinalizationFlag2 = -1;
    static int number = 4; //100;
    static int nthr   = 2; //21;

    @SuppressWarnings("deprecation")
    public void test_addShutdownHook() throws InterruptedException {
        Thread[] thr = new Thread[number];
        for (int i = 0; i < number / 2; i++) {
            Runtime.getRuntime().addShutdownHook(
                    thr[2 * i + 0] = new threadForInternalUseOnly3());
            Thread.sleep(5);
            Runtime.getRuntime().addShutdownHook(
                    thr[2 * i + 1] = new threadForInternalUseOnly2(2 * i + 1));
            Thread.sleep(5);
        }
        Runtime.runFinalizersOnExit(true);
        new threadForInternalUseOnly1().start();
        try {
            Runtime.getRuntime().addShutdownHook(thr[nthr]);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test_removeShutdownHook() throws InterruptedException {
        Thread[] thr = new Thread[number];
        for (int i = 0; i < number / 2; i++) {
            Runtime.getRuntime().addShutdownHook(
                    thr[2 * i + 0] = new threadForInternalUseOnly3());
            Thread.sleep(5);
            Runtime.getRuntime().addShutdownHook(
                    thr[2 * i + 1] = new threadForInternalUseOnly2(2 * i + 1));
            Thread.sleep(5);
        }
        // Runtime.getRuntime().removeShutdownHook(thr[1]);
        // Runtime.getRuntime().removeShutdownHook(thr[11]);
        Runtime.getRuntime().removeShutdownHook(thr[nthr]);
        new threadForInternalUseOnly1().start();

        // Runtime.getRuntime().addShutdownHook(thr[1]);
        // Runtime.getRuntime().addShutdownHook(thr[11]);
        Runtime.getRuntime().addShutdownHook(thr[nthr]);
        // Runtime.getRuntime().removeShutdownHook(thr[1]);
        // Runtime.getRuntime().removeShutdownHook(thr[11]);
        Runtime.getRuntime().removeShutdownHook(thr[nthr]);
        // Runtime.getRuntime().removeShutdownHook(thr[1]);
        // Runtime.getRuntime().removeShutdownHook(thr[11]);
        Runtime.getRuntime().removeShutdownHook(thr[nthr]);
    }

    private static boolean isOSWindows(){
        String osName = System.getProperty("os.name");
        osName = osName.toLowerCase(Locale.US);
        return osName.contains("windows");
    }

    private static boolean isOSLinux(){
        String osName = System.getProperty("os.name");
        osName = osName.toLowerCase(Locale.US);
        return osName.contains("linux");
    }

    private void exec_StrForWindows() throws Exception {
        String cmnd = "cmd /C date";
        Process pi3 = Runtime.getRuntime().exec(cmnd);
        OutputStream os = pi3.getOutputStream();
        pi3.getErrorStream();
        InputStream is = pi3.getInputStream();
        // wait for is.available != 0
        int count = 100;
        while (is.available() < 60 && count-- > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        if (count < 0) {
            fail("check001: the date's reply has not been received");
        }

        int ia = is.available();
        byte[] bb = new byte[ia];
        is.read(bb);
        String r1 = new String(bb);
        if (r1.indexOf("The current date is") == -1
                || r1.indexOf("Enter the new date") == -1) {
            fail("check002: " + r1);
        }
        for (int ii = 0; ii < ia; ii++) {
            bb[ii] = (byte) 0;
        }

        os.write('x');
        os.write('x');
        os.write('-');
        os.write('x');
        os.write('x');
        os.write('-');
        os.write('x');
        os.write('x');
        os.write('\n');
        os.flush();

        // wait for is.available > 9 which means that 'is' contains
        // both the above written value and the consequent 
        // 'date' command's reply
        count = 300;
        while (is.available() < 11 && count-- > 0) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
        }
        if (count < 0) {
            fail("check003: the date's reply has not been received");
        }
        ia = is.available();
        byte[] bbb = new byte[ia];
        is.read(bbb);
        r1 = new String(bbb);
        if (r1.indexOf("The system cannot accept the date entered") == -1
                && r1.indexOf("Enter the new date") == -1) {
            fail("check004: unexpected output: " + r1);
        }
        os.write('\n');
        try {
            pi3.exitValue();
        } catch (IllegalThreadStateException e) {
            os.flush();
            try {
                pi3.waitFor();
            } catch (InterruptedException ee) {
            }
        }
        // System.out.println("5test_exec_Str");
        // os.write('\n');
        // os.write('\n');
        // os.flush();
        pi3.destroy();
        // pi3.waitFor();
    }

    public void test_exec_Str() throws Exception {
        if (isOSWindows()) {
            exec_StrForWindows();
        } else if (isOSLinux()) {
            // TODO
        } else {
            //UNKNOWN
        }
    }

    public void test_exec_StrArr() throws Exception {
        String[] command = null;
        if (isOSWindows()) {
            command = new String[]{"cmd", "/C", "echo S_O_M_E_T_H_I_N_G"};
        } else {
            command = new String[]{"/bin/sh", "-c", "echo S_O_M_E_T_H_I_N_G"};
        }
        String procValue = null;
        Process proc = Runtime.getRuntime().exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(
                proc.getInputStream()));
        procValue = br.readLine();
        assertTrue("echo command has not been run",
                procValue.indexOf("S_O_M_E_T_H_I_N_G") != -1);
    }

    public void test_exec_StrArr_StrArr() throws Exception {
        String[] command = null;
        if (isOSWindows()) {
            command = new String[] {"cmd", "/C", "echo %Z_S_S%"};
        } else {
            command = new String[] {"/bin/sh", "-c", "echo $Z_S_S"};
        }
        String procValue = null;
        Process proc = Runtime.getRuntime().exec(command,
                new String[] {"Z_S_S=S_O_M_E_T_H_I_N_G"});
        BufferedReader br = new BufferedReader(new InputStreamReader(proc
                .getInputStream()));
        procValue = br.readLine();
        assertTrue("echo command has not been run",
                procValue.indexOf("S_O_M_E_T_H_I_N_G") != -1);
    }

    public void test_exec_StrArr_StrArr_Fil() throws Exception {
        String[] command = null;
        if (isOSWindows()) {
            command = new String[] {"cmd", "/C", "set"};
        } else {
            command = new String[] {"/bin/sh", "-c", "env"};
        }
        String as[];
        int len = 0;

        Process proc = Runtime.getRuntime().exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(proc
                .getInputStream()));
        while (br.readLine() != null) {
            len++;
        }

        as = new String[len];
        proc = Runtime.getRuntime().exec(command);
        br = new BufferedReader(new InputStreamReader(proc
                .getInputStream()));
        for (int i = 0; i < len; i++) {
            as[i] = br.readLine();
        }

        if (isOSWindows()) {
            as = new String[]{"to_avoid=#s1s2f1t1"}; // <<<<<<<<<<< !!! to remember
            command = new String[]{"cmd", "/C", "dir"};
        } else {
            command = new String[]{"sh", "-c", "pwd"};
        }

        proc = Runtime.getRuntime().exec(command, as, new File(System.getProperty("java.io.tmpdir")));
        br = new BufferedReader(new InputStreamReader(
                proc.getInputStream()));
        //for (int i = 0; i < len; i++) {
        String ln;
        while ( (ln = br.readLine()) != null) {
            if(ln.indexOf(System.getProperty("java.io.tmpdir").substring(0,System.getProperty("java.io.tmpdir").length() -1 ))!=-1) {
                return;
            }
        }
        fail("Error3");
    }

    public void test_exec_Str_StrArr() throws Exception {
        String command = null;
        if (isOSWindows()) {
            command = "cmd /C \"echo %Z_S_S_2%\"";
        } else {
            //command = "/bin/sh -c \"echo $Z_S_S_2\"";
            //command = "/bin/echo $Z_S_S_2";
            command = "/usr/bin/env";
        }
        String procValue = null;
        block1: {
            Process proc = Runtime.getRuntime().exec(command,
                    new String[] {"Z_S_S_2=S_O_M_E_T_H_I_N_G"});
            BufferedReader br = new BufferedReader(new InputStreamReader(proc
                    .getInputStream()));
            while ((procValue = br.readLine()) != null) {
                if (procValue.indexOf("S_O_M_E_T_H_I_N_G") != -1) {
                    break block1;
                }
                fail("It should be the only singl environment variable here (" + procValue + ")");
            }
            fail("Z_S_S_2 var should be present and assingned correctly.");
        }
        
        Process proc = Runtime.getRuntime().exec(command, new String[] {
                "Z_S_S_2=S_O_M_E_T_H_I_N_G_s1s2f1t1", //<<<<<<<<<<< !!! to remember
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
                "Z_S_S_3=S_O_M_E_T_H_I_N_G L_O_N_GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
        });
        BufferedReader br = new BufferedReader(new InputStreamReader(
                proc.getInputStream()));
        procValue = br.readLine();

        assertTrue("Error4",procValue.indexOf("S_O_M_E_T_H_I_N_G")!=-1);
    }

    public void test_exec_Str_StrArr_Fil() throws Exception {
        String[] command = null;
        if (isOSWindows()) {
            command = new String[] {"cmd", "/C", "env"};
        } else {
            command = new String[] {"/bin/sh", "-c", "env"};
        }
        String as[];
        int len = 0;
        Process proc = Runtime.getRuntime().exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(proc
                .getInputStream()));
        while (br.readLine() != null) {
            len++;
        }
        
        as = new String[len];
        proc = Runtime.getRuntime().exec(command);
        br = new BufferedReader(new InputStreamReader(proc
                .getInputStream()));
        for (int i = 0; i < len; i++) {
            as[i] = br.readLine();
        }

        String command2;
        if (isOSWindows()) {
            as = new String[]{"to_avoid=#s1s2f1t1"};//<<<<<<<<<<< !!! to remember
            command2 = "cmd /C dir";
        } else {
            command2 = "sh -c pwd";
        }

        proc = Runtime.getRuntime().exec(command2, as, new File(System.getProperty("java.io.tmpdir")));
        br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String ln;
        while ( (ln = br.readLine()) != null) {
            if(ln.indexOf(System.getProperty("java.io.tmpdir").substring(0,System.getProperty("java.io.tmpdir").length() -1 ))!=-1) {
                return;
            }
        }
        fail("Error5");
    }

    public void test_exec_Str_F2T1S2Z() throws Exception {
        String line;
        if (isOSWindows()) {
            File f = new File(System.getProperty("java.io.tmpdir"));
            Process p = Runtime.getRuntime().exec(
                    new String[] { "cmd", "/C", "echo Hello World" }, new String[] {}, f);
            p.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(p
                    .getErrorStream()));
            while ((line = input.readLine()) != null) {
                fail("The ErrorStream should be empty!");
            }
            input.close();
            
            input = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            
            StringBuilder builder = new StringBuilder();
            while ((line = input.readLine()) != null) {
                builder.append(line);
                System.out.println(line);
            }
            input.close();
            
            assertEquals("Hello World", builder.toString().trim());
        } else if (isOSLinux()) {
            String strarr[] = {"6",System.getProperty("java.io.tmpdir")
                    + File.separator 
                    + "vasja", "Hello", "HELL", "\"Hello\" \"world\"",
                    "World hello", "vas\"a d:?*/\\" };
            File fff = null;
            PrintStream ps = null;
            try {
                fff = new File(System.getProperty("java.io.tmpdir")
                        + File.separator + "vasja");
                fff.createNewFile();
                ps = new PrintStream(new FileOutputStream(fff));
                ps.println("{ echo $#; echo $0; echo $1;  " +
                "echo $2; echo $3; echo $4; echo $5; }");
            } catch (Throwable e) {
                System.err.println(e);
                System.err.println(System.getProperty("user.home")
                        + File.separator + "vasja");
                new Throwable().printStackTrace();
                fail("Preparing fails!");
            }
            try {
                String pathList = System.getProperty("java.library.path");
                String[] paths = pathList.split(File.pathSeparator);
                String cmnd = null;
                int ind1;
                for (ind1 = 0; ind1 < paths.length; ind1++) {
                    if (paths[ind1] == null) {
                        continue;
                    }
                    File asf = new File(paths[ind1] + File.separator
                            + "sh");
                    if (asf.exists()) {
                        cmnd = paths[ind1] + File.separator + "sh";
                        break;
                    }
                }
                if (cmnd == null) {
                    cmnd = "/bin/sh";
                }
                File f = new File("/bin");
                Process p;
                if (f.exists()) {
                    p = Runtime.getRuntime().exec(new String[] {
                            cmnd, System.getProperty("java.io.tmpdir")
                            + File.separator + "vasja", "Hello", "HELL", 
                            "\"Hello\" \"world\"", "World hello", 
                            "vas\"a d:?*/\\", "\"World hello\"" },
                            new String[] {}, f);
                    p.waitFor();
                } else {
                    p = Runtime.getRuntime().exec(new String[] {
                            cmnd, System.getProperty("java.io.tmpdir")
                            + File.separator + "vasja", "Hello", "HELL",
                            "\"Hello\" \"world\"", "World hello",
                            "vas\"a d:?*/\\", "\"World hello\"" });
                    if (p.waitFor() != 0) {
                        fail("check005: sh.exe seems to have not been found" +
                                " by default! Please, set the path to sh.exe" +
                        " via java.library.path property.");
                    }
                }
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(p.getErrorStream()));
                boolean flg = false;
                while ((line = input.readLine()) != null) {
                    flg = true;
                    System.err.println("ErrorStream: " + line);
                }
                input.close();
                if (flg) {
                    fail("check006: ErrorStream should be empty!");
                }

                input = new BufferedReader(new InputStreamReader(p
                        .getInputStream()));
                int i = 0;
                while ((line = input.readLine()) != null) {
                    if (!line.equals(strarr[i])) {
                        flg = true;
                        System.out.println(line + " != " + strarr[i]);
                    }
                    i++;
                }
                input.close();
                if (flg) {
                    fail("check007: An uncoincidence was found (see above)!");
                }
            } catch (Exception eeee) {
                fail("check008: Unexpected exception on exec(String[], String[], File)");
            }
            try {
                fff.delete();
            } catch (Throwable _) {
            }
        } else {
            //UNKNOWN
        }
    }
}
