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
 * Created on January 5, 2005
 *
 * This RuntimeTest class is used to test the Core API Runtime class
 * 
 */


/**
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
 **/

public class RuntimeTest extends TestCase {

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }    

    /**
     *  
     */
    public void test_availableProcessors() {
        /**/System.out.println("test_availableProcessors");
        int fR = Runtime.getRuntime().availableProcessors();
        int sR = Runtime.getRuntime().availableProcessors();
        assertEquals("Runtime.availableProcessors method should return the " +
                         "same value during this test running as a rule!",
                     fR, sR);
        assertTrue("Runtime.availableProcessors method should return " +
                        "a value greater than 0!(" + fR + "|" + 
                        Runtime.getRuntime().availableProcessors()+ ")",
                    fR > 0);
        //XXX: the next case may be to compare with the bringing via Runtime.exec(...).getOutputStream()...
    }

    /**
     *  
     */
    public void test_freeMemory() {
        Runtime.getRuntime().gc();
        long r1 = Runtime.getRuntime().freeMemory();
        assertTrue("Runtime.freeMemory() returned negative value: " + r1,
                r1 >= 0);
        if (r1 < 500000) {
            // low memory condition, 
            // avoid false alarm if indicator is too coarse-grained 
            return;
        }
        int probe = 0;
        try {
            String stmp = "";
            for (int ind = 0; ind < 300; ind++) {
                stmp += "0123456789";
            }
            String inc = stmp;
            probe = stmp.length();
            while (r1 <= Runtime.getRuntime().freeMemory()) {
                stmp += inc;
                probe = stmp.length();
            }
        } catch (OutOfMemoryError e) {
            fail("Runtime.freeMemory() failed to detect " + probe
                    + " memory reduction from " + r1);
        }
    }

    /**
     *  
     */
    public void test_gc() {
        /**/System.out.println("test_gc");
        long r1=Runtime.getRuntime().freeMemory();
        long r2;
        long r4;
        String[] sa = new String[(int)r1/50000];
        int ind1=0;
            try {
                String stmp = "";
                for (int ind2=0; ind2<100/* 1000 */; ind2++) {
                    stmp += "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
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
                for (ind1=0; ind1 <(int)r1/50000; ind1++) {
                    sa[ind1]=""+stmp;
                }
        
                r2=Runtime.getRuntime().freeMemory();
                for (ind1=0; ind1 <(int)r1/50000; ind1++) {
                    sa[ind1]=null;
                    Runtime.getRuntime().gc();
                    try {Thread.sleep(20);} catch (Exception e) {}
                }
                sa=null;
                try {Thread.sleep(1000);} catch (Exception e) {}
                Runtime.getRuntime().gc();
                try {Thread.sleep(1000);} catch (Exception e) {}
                r4=Runtime.getRuntime().freeMemory();
                
                //assertTrue( "FAILED: gc.check001: Runtime.gc method should initiate garbage collecting!("+r4+">"+r2+"?)", r4>r2 /*r4-r2>49999*/);
                if( r4 <= r2 ) {
                    System.out.println("WARNNING: RuntimeTest.test_gc " +
                            "check001: It would be better if Runtime.gc method" +
                            " could initiate garbage collecting! " +
                            "(Here we have " + r4 + " !> " + r2 + " .)");
                }
            } catch (OutOfMemoryError e) {
                System.out.println("WARNNING: test_gc did not check " +
                        "Runtime.gc method due to the technical reason !");
            }
    }

    /**
     *  
     */
    public void test_getLocalizedInputStream() {
        /**/System.out.println("test_getLocalizedInputStream");
        byte[] bt = new byte[9];
        int res = 0;
        java.io.InputStream is = Runtime.getRuntime().getLocalizedInputStream(
                new java.io.StringBufferInputStream(
                        "\u005a\u0061\u0070\u0072\u0065\u0079\u0065\u0076"));
        try {
            res = is.read(bt);
        }
        catch(Exception e) {
            fail("Runtime.getLocalizedInputStream method should return " +
                    "correct input stream!");
        }
        assertEquals("Incorrect number of bytes returned by InputStream!",
                     8, res);
        //assertTrue("FAILED: getLocalizedInputStream.check003: Runtime.getLocalizedInputStream method should return correct input stream ("+new String(bt)+")!", new String(bt).indexOf("Zapreyev")==0);
    }

    /**
     *  
     */
    public void test_getLocalizedOutputStream() {
        /**/System.out.println("test_getLocalizedOutputStream");
        byte[] bt1 = {0x5a, 0x61, 0x70, 0x72, 0x65, 0x79, 0x65, 0x76};
        byte[] bt2 = new byte[9];
        java.io.PipedInputStream pis = new java.io.PipedInputStream();
        java.io.OutputStream os = null;
        try {
            os = Runtime.getRuntime().getLocalizedOutputStream(
                    new java.io.PipedOutputStream(pis));
        } catch (Exception e) {
            fail("check001: unexpected exception " + e);
        }
        try {
            os.write(bt1);
        } catch (Exception e) {
            fail("check002: unexpected exception " + e);
        }
        try {
            pis.read(bt2);
        } catch (Exception e) {
            fail("check003: unexpected exception " + e);
        }
        assertTrue("Incorrect bytes written by outputStream: " + new String(bt2)
                       + ")!", 
                   new String(bt2).indexOf("Zapreyev") == 0);
    }

    /**
     *  
     */
    public void test_getRuntime() {
        /**/System.out.println("test_getRuntime");
        Runtime r1 = Runtime.getRuntime();
        assertNotNull("Runtime.getRuntime method must not return null!", r1);
        for (int ind2 = 0; ind2 < 1000; ind2++) {
            assertSame(
                    "Runtime.getRuntime() should always return the same value!",
                    r1, Runtime.getRuntime());
        }
    }

    /**
     *  
     */
    public void test_load() {
        /**/System.out.println("test_load");
        String jLP = null;
        String jlp = System.getProperty("java.library.path");
        String vblp = System.getProperty("vm.boot.library.path");
        jLP = (jlp != null && jlp.length() != 0 ? jlp : "")
                + (vblp != null && vblp.length() != 0 ? File.pathSeparator
                        + vblp : "");
        if (jLP.length() == 0) {
            fail("empty java.library.path!");
        }
        String[] paths = jLP.split(File.pathSeparator);
        String ext = (System.getProperty("os.name").indexOf("indows") != -1) 
                ? ".dll"
                : ".so";
        int ind1;
        int ind2;
        File[] asf = null;
        for (ind1 = 0; ind1 < paths.length; ind1++) {
            asf = new java.io.File(paths[ind1]).listFiles();
            if (asf != null) {
                for (ind2 = 0; ind2 < asf.length; ind2++) {
                    if (asf[ind2].getName().indexOf(ext) != -1) {
                        try {
                            Runtime.getRuntime().load(
                                    asf[ind2].getCanonicalPath());
                            return;
                        } catch (UnsatisfiedLinkError e) {
                            continue;
                        } catch (Throwable e) {
                            continue;
                        }
                    }
                }
            }
        }
        fail("Runtime.loadLibrary method has not loaded a dynamic library!");
    }

    /**
     *  
     */
    public void test_loadLibrary() {
        /**/System.out.println("test_loadLibrary");
        String jLP = null;
        String jlp = System.getProperty("java.library.path");
        String vblp = System.getProperty("vm.boot.library.path");
        jLP = (jlp != null && jlp.length() != 0 ? jlp : "")
                + (vblp != null && vblp.length() != 0 
                        ? File.pathSeparator + vblp 
                        : "");
        if (jLP.length() == 0) {
            fail("empty java.library.path!");
        }
        String[] paths = jLP.split(File.pathSeparator);
        String ext = (System.getProperty("os.name").indexOf("indows") != -1
                      ? ".dll"
                      :".so");
        int ind1;
        int ind2;
        File[] asf = null;
        for (ind1 = 0; ind1 < paths.length; ind1++) {
            if (paths[ind1] == null) {
                continue;
            }
            asf = new java.io.File(paths[ind1]).listFiles();
            if (asf != null) {
                for (ind2 = 0; ind2 < asf.length; ind2++) {
                    if (asf[ind2].getName().indexOf(ext) != -1) {
                        String libName = asf[ind2].getName();
                        if (ext.equals(".dll")) {
                            libName = libName.substring(0, libName.length() - 4);
                        } else {
                            libName = libName.substring(3, libName.length() - 3);
                        }
                        try {
                            Runtime.getRuntime().loadLibrary(libName);
                            return;
                        } catch (UnsatisfiedLinkError e) {
                            continue;
                        } catch (Throwable e) {
                            continue;
                        }                    
                    
                    }
                }
            }
        }
        fail("Runtime.loadLibrary method has not loaded a dynamic library!");
    }

    /**
     *  
     */
    public void test_maxMemory() {
        /**/System.out.println("test_maxMemory");
        long r1 = Runtime.getRuntime().freeMemory();
        long r2 = Runtime.getRuntime().maxMemory();
        assertTrue("Runtime.maxMemory method must not return negative value!",
                r2 >= 0);
        assertTrue(
                "Runtime.maxMemory must be greater than Runtime.freeMemory!",
                r2 >= r1);
        for (int ind2 = 0; ind2 < 1000; ind2++) {
            //assertSame("Runtime.maxMemory() must always return the same value!",
            //        r2, Runtime.getRuntime().maxMemory());
            assertTrue( "FAILED: test_maxMemory: Runtime.maxMemory method should return same value each time!", r2==Runtime.getRuntime().maxMemory());
        }
    }

    /**
     * 
     */
    public void test_totalMemory() {
        /**/System.out.println("test_totalMemory");
        long r1 = Runtime.getRuntime().freeMemory();
        long r2 = Runtime.getRuntime().maxMemory();
        long r3 = Runtime.getRuntime().totalMemory();
        assertTrue("Runtime.totalMemory() should not return negative value!",
                r3 >= 0);
        assertTrue(
                "Runtime.totalMemory() should be greater than "
                    + "Runtime.freeMemory()!",
                r3 >= r1);
        assertTrue(
                "Runtime.totalMemory() should be smaller than "
                    + "Runtime.maxMemory()!",
                r2 >= r3);
        for (int ind2 = 0; ind2 < 1000; ind2++) {
            assertTrue("Runtime.totalMemory, Runtime.freeMemory, " +
                    "Runtime.maxMemory should correlate correctly!",
                    Runtime.getRuntime().freeMemory() <= Runtime.getRuntime()
                            .totalMemory()
                            && Runtime.getRuntime().maxMemory() >= Runtime
                                    .getRuntime().totalMemory());
        }
    }

    /**
     *  
     */
    public void test_traceInstructions() {
        /**/System.out.println("test_traceInstructions");
        java.util.Random r = new java.util.Random();
        try {
            for (int ind2 = 0; ind2 < 1000; ind2++) {
                Runtime.getRuntime()
                        .traceInstructions((r.nextInt(10) % 2) == 0);
                Integer.toString(ind2);
            }
        } catch (Throwable e) {
            fail("Unexpected exception: " + e);
        }
    }

    /**
     *  
     */
    public void test_traceMethodCalls() {
        /**/System.out.println("test_traceMethodCalls");
        java.util.Random r = new java.util.Random();
        try {
            for (int ind2 = 0; ind2 < 1000; ind2++) {
                Runtime.getRuntime().traceMethodCalls((r.nextInt(10) % 2) == 0);
                Math.pow((long) ind2, (long) ind2);
                Math.IEEEremainder((double) ind2, (double) ind2);
            }
        } catch (Throwable e) {
            fail("Unexpected exception: " + e);
        }
    }

    /**
     *  
     */
    public void test_halt() {
        //System.out.println("test_halt");
        //Runtime.getRuntime().halt(777);
        //fail("what's wrong  ;) ?");
    }
    
    /**
     *  
     */
    public void test_exit() {
        //System.out.println("test_exit");
        //Runtime.getRuntime().exit(777);
        //fail("what's wrong  ;) ?");
    }

    /**
     * Regression test for HARMONY-690 
     */
    public void test_addShutdownHook() {
        // Test for method long java.lang.Runtime.addShutdownHook()
        boolean exception = false; 
        try {
            Runtime.getRuntime().addShutdownHook(null);
        } catch (NullPointerException npe) {
            exception = true;
        }
        assertTrue("NullPointerException expected!", exception);
    }

    /**
     * Regression test for HARMONY-690 
     */
    public void test_removeShutdownHook() {
        // Test for method long java.lang.Runtime.removeShutdownHook()
        boolean exception = false; 
        try {
            Runtime.getRuntime().removeShutdownHook(null);
        } catch (NullPointerException npe) {
            exception = true;
        }
        assertTrue("NullPointerException expected!", exception);
    }

    /**
     * Regression test for HARMONY-920 
     */
    public void test_execStrStr() throws Exception {
        try {
            String[] cmd = new String [] {null, "gcc"}; 
            String[] env = new String[] {"aaa", "bbb"}; 
            Runtime.getRuntime().exec(cmd, env); 
            fail("1: exception expcected"); 
        } catch (NullPointerException npe) {
            //expected
        }
        try { 
            String[] cmd = new String [] {"gcc", "-m"}; 
            String[] env = new String[] {"aaa", null}; 
            Runtime.getRuntime().exec(cmd, env); 
            fail("2: exception expcected"); 
        } catch (NullPointerException npe) {
            //expected
        } 
    }
}
