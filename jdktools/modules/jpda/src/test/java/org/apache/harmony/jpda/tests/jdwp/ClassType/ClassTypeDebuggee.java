/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Anton V. Karnachuk
 */

/**
 * Created on 10.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Common debuggee of some JDWP unit tests for JDWP ClassType command set.
 */
public class ClassTypeDebuggee extends SyncDebuggee {

    static boolean f_def_bool                   = false;
    private static boolean f_pri_bool           = false;
    protected static boolean f_pro_bool         = false;
    public static boolean f_pub_bool            = false;

    static final boolean ff_def_bool            = false;
    private static final boolean ff_pri_bool    = false;
    protected static final boolean ff_pro_bool  = false;
    public static final boolean ff_pub_bool     = false;
    
    public static byte f_pub_byte;
    public static char f_pub_char;
    public static float f_pub_float = 254.5f;
    public static double f_pub_double;
    public static int f_pub_int;
    public static long f_pub_long;
    public static short f_pub_short;
    public static boolean f_pub_boolean;

    public void run() {
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("ClassTypeDebuggee started");
        logWriter.println("DUMP{" + f_pri_bool + ff_pri_bool + "}");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("ClassTypeDebuggee done");
    }

    /**
     * Starts ClassTypeDebuggee with help of runDebuggee() method 
     * from <A HREF="../../share/Debuggee.html">Debuggee</A> super class.
     *  
     */
    public static void main(String [] args) {
        runDebuggee(ClassTypeDebuggee.class);
    }

}