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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.compiler;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * This interface contains command line options and other textual constants
 * for RMI Compiler.
 *
 * @author  Vasily Zakharov
 */
interface RmicStrings extends RmicConstants {

    /**
     * Options prefix character.
     */
    String optionPrefix = "-"; //$NON-NLS-1$

    /**
     * <code>-v1.1</code> option.
     */
    String optionV11 = optionPrefix + "v1.1"; //$NON-NLS-1$

    /**
     * <code>-v1.2</code> option.
     */
    String optionV12 = optionPrefix + "v1.2"; //$NON-NLS-1$

    /**
     * <code>-vcompat</code> option.
     */
    String optionVCompat = optionPrefix + "vcompat"; //$NON-NLS-1$

    /**
     * <code>-idl</code> option.
     */
    String optionIDL = optionPrefix + "idl"; //$NON-NLS-1$

    /**
     * <code>-iiop</code> option.
     */
    String optionIIOP = optionPrefix + "iiop"; //$NON-NLS-1$

    /**
     * <code>-source</code> option for Java Compiler.
     */
    String optionSource = optionPrefix + "source"; //$NON-NLS-1$

    /**
     * <code>-target</code> option.
     */
    String optionTarget = optionPrefix + "target"; //$NON-NLS-1$

    /**
     * <code>-keep</code> option.
     */
    String optionKeep = optionPrefix + "keep"; //$NON-NLS-1$

    /**
     * <code>-keepgenerated</code> option.
     */
    String optionKeepGenerated = optionPrefix + "keepgenerated"; //$NON-NLS-1$

    /**
     * <code>-always</code> option.
     */
    String optionAlways = optionPrefix + "always"; //$NON-NLS-1$

    /**
     * <code>-alwaysgenerate</code> option.
     */
    String optionAlwaysGenerate = optionPrefix + "alwaysgenerate"; //$NON-NLS-1$

    /**
     * <code>-factory</code> option.
     */
    String optionFactory = optionPrefix + "factory"; //$NON-NLS-1$

    /**
     * <code>-noValueMethods</code> option.
     */
    String optionNoValueMethods = optionPrefix + "noValueMethods"; //$NON-NLS-1$

    /**
     * <code>-nolocalstubs</code> option.
     */
    String optionNoLocalStubs = optionPrefix + "nolocalstubs"; //$NON-NLS-1$

    /**
     * <code>-poa</code> option.
     */
    String optionPOA = optionPrefix + "poa"; //$NON-NLS-1$

    /**
     * <code>-g</code> option.
     */
    String optionDebug = optionPrefix + 'g';

    /**
     * <code>-g:</code> option.
     */
    String optionDebugDetails = optionDebug + ':';

    /**
     * <code>-nowarn</code> option.
     */
    String optionNoWarnings = optionPrefix + "nowarn"; //$NON-NLS-1$

    /**
     * <code>-nowrite</code> option.
     */
    String optionNoWrite = optionPrefix + "nowrite"; //$NON-NLS-1$

    /**
     * <code>-verbose</code> option.
     */
    String optionVerbose = optionPrefix + "verbose"; //$NON-NLS-1$

    /**
     * <code>-depend</code> option.
     */
    String optionDepend = optionPrefix + "depend"; //$NON-NLS-1$

    /**
     * <code>-idlModule</code> option.
     */
    String optionIdlModule = optionPrefix + "idlModule"; //$NON-NLS-1$

    /**
     * <code>-idlFile</code> option.
     */
    String optionIdlFile = optionPrefix + "idlFile"; //$NON-NLS-1$

    /**
     * <code>-d</code> option.
     */
    String optionDestinationDir = optionPrefix + 'd';

    /**
     * <code>-classpath</code> option.
     */
    String optionClassPath = optionPrefix + "classpath"; //$NON-NLS-1$

    /**
     * <code>-cp</code> option.
     */
    String optionCP = optionPrefix + "cp"; //$NON-NLS-1$

    /**
     * <code>-bootclasspath</code> option.
     */
    String optionBootClassPath = optionPrefix + "bootclasspath"; //$NON-NLS-1$

    /**
     * <code>-extdirs</code> option.
     */
    String optionExtDirs = optionPrefix + "extdirs"; //$NON-NLS-1$

    /**
     * <code>-J</code> option.
     */
    String optionJava = optionPrefix + 'J';

    /**
     * <code>-X</code> option.
     */
    String optionX = optionPrefix + 'X';

    /**
     * RMI Compiler usage text.
     */
    
    /* rmi.console.1C=Usage: rmic <options> <class names>
     * rmi.console.1D=Options:
     * rmi.console.1E=Create stubs/skeletons for 1.1 stub protocol version only
     * rmi.console.1F=(default) Create stubs for 1.2 stub protocol version only
     * rmi.console.20=Create stubs/skeletons compatible with both v1.1 and v1.2
     * rmi.console.21=Generate class files for the specified VM version
     * rmi.console.22=Do not delete generated source files
     * rmi.console.23=\ \ \ \ \ (the same as "
     * rmi.console.24=\ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ Generate debug information
     * rmi.console.25=\ \ \ \ \ \ \ \ \ \ \ \ Do not notify about warnings
     * rmi.console.26=Check run: do not write compiled classes
     * rmi.console.27=\ \ \ \ \ \ \ \ \ \ \ Print detailed compilation log
     * rmi.console.28=\ <directory>         Target directory for generated files
     * rmi.console.29=\ <path>      Input class files location
     * rmi.console.2A=\ <path>             (the same as "
     * rmi.console.2B=\ <path>  Override location of bootstrap class files
     * rmi.console.2C=\ <dirs>        Override location of installed extensions
     * rmi.console.2D=<JVM option>         Pass option to JVM
     * rmi.console.2E=<extended option>    Pass -X option to JVM
     */
    String usageText = Messages.getString("rmi.console.1C") + EOLN //$NON-NLS-1$
            + EOLN + Messages.getString("rmi.console.1D") + EOLN + "  " + optionV11 + "              " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + Messages.getString("rmi.console.1E") //$NON-NLS-1$
            + EOLN + "  " + optionV12 + "              " //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.getString("rmi.console.1F") //$NON-NLS-1$
            + EOLN + "  " + optionVCompat + "           " //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.getString("rmi.console.20") //$NON-NLS-1$
            + EOLN + EOLN + "  " + optionTarget + " <version>  " //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.getString("rmi.console.21") //$NON-NLS-1$
            + EOLN + EOLN + "  " + optionKeep + "              " //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.getString("rmi.console.22") + EOLN + "  " //$NON-NLS-1$ //$NON-NLS-2$
            + optionKeepGenerated + Messages.getString("rmi.console.23") + optionKeep + "\")" //$NON-NLS-1$ //$NON-NLS-2$
            + EOLN + EOLN + "  " + optionDebug //$NON-NLS-1$
            + Messages.getString("rmi.console.24") + EOLN + "  " //$NON-NLS-1$ //$NON-NLS-2$
            + optionNoWarnings + Messages.getString("rmi.console.25") //$NON-NLS-1$
            + EOLN + "  " + optionNoWrite + "           " //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.getString("rmi.console.26") + EOLN + "  " //$NON-NLS-1$ //$NON-NLS-2$
            + optionVerbose + Messages.getString("rmi.console.27") //$NON-NLS-1$
            + EOLN + EOLN + "  " + optionDestinationDir //$NON-NLS-1$
            + Messages.getString("rmi.console.28") //$NON-NLS-1$
            + EOLN + "  " + optionClassPath //$NON-NLS-1$
            + Messages.getString("rmi.console.29") //$NON-NLS-1$
            + EOLN + "  " + optionCP + Messages.getString("rmi.console.2A") //$NON-NLS-1$ //$NON-NLS-2$
            + optionClassPath + "\")" + EOLN + "  " + optionBootClassPath //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.getString("rmi.console.2B") //$NON-NLS-1$
            + EOLN + "  " + optionExtDirs //$NON-NLS-1$
            + Messages.getString("rmi.console.2C") //$NON-NLS-1$
            + EOLN + EOLN + "  " + optionJava //$NON-NLS-1$
            + Messages.getString("rmi.console.2D") //$NON-NLS-1$
            + EOLN + "  " + optionX //$NON-NLS-1$
            + Messages.getString("rmi.console.2E"); //$NON-NLS-1$

    /**
     * Version error text.
     */ 
    /* rmi.console.2F=You should specify at most one of "
     * rmi.console.31=" (default), "
     */
    String errorVersionText = Messages.getString("rmi.console.2F") //$NON-NLS-1$
            + optionV11 + "\", \"" + optionV12 + Messages.getString("rmi.console.31") //$NON-NLS-1$ //$NON-NLS-2$
            + optionVCompat + "\", \"" + optionIDL + "\", \"" + optionIIOP; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * No option parameter error text.
     */ 
    /*
     rmi.console.32=Option %s requires a parameter
     */
    String errorNeedParameterText = Messages.getString("rmi.console.32"); //$NON-NLS-1$

    /**
     * No JVM option parameter error text.
     */
    /*
     * rmi.console.33=Option %s must be immediately (without a space) followed by a JVM option
     */
    String errorNeedJVMParameterText = Messages.getString("rmi.console.33"); //$NON-NLS-1$

    /**
     * Need two parameters error text.
     */ 
     /*
      * rmi.console.35=Option %s requires two parameters 
      */ 
    String errorNeedTwoParametersText = Messages.getString("rmi.console.35"); //$NON-NLS-1$

    /**
     * Unknown option error text.
     */
    /*
     * rmi.console.36=Unknown option: %s
     */
    String errorUnknownOptionText = Messages.getString("rmi.console.36"); //$NON-NLS-1$

    /**
     * No classes to compile error text.
     */
    /*
     * rmi.console.37=No classes to compile specified
     */
    String errorNoClassesText = Messages.getString("rmi.console.37"); //$NON-NLS-1$

    /**
     * Unusable IDL/IIOP option error text.
     */
    /*
     * rmi.console.38=Option %s must only be used with {0} or {1}
     */
    String errorUnusableExceptIDL_IIOP = Messages.getString("rmi.console.38", //$NON-NLS-1$
                                         optionIDL, optionIIOP);

    /**
     * Unusable IDL option error text.
     */
    /*
     * rmi.console.39=Option %s must only be used with {0}
     */
    String errorUnusableExceptIDL = Messages.getString("rmi.console.39", //$NON-NLS-1$
                                    optionIDL);

    /**
     * Unusable IIOP option error text.
     */
    /*
     * rmi.console.3A=Option %s must only be used with {0}
     */
    String errorUnusableExceptIIOP = Messages.getString("rmi.console.3A", //$NON-NLS-1$
                                     optionIIOP);

    /**
     * Warning about classpath.
     */
    /*
     * rmi.console.3B=%s is specified. For proper operation the same %s + 
     *                should be specified in VM arguments. This is a + 
     *                limitation of current RMIC implementation.
     */
    String warningClassPathText = Messages.getString("rmi.console.3B"); //$NON-NLS-1$
}
