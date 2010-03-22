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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jretools.policytool;

import org.apache.harmony.jretools.policytool.view.MainFrame;

/**
 * The main class that parses command line parameters, and starts the GUI if everything everything is ok.
 *
 */
public class Main {

    /** Name of policy file to be loaded initially. */
    private static String policyFileName;

    /**
     * Entry point of the program.
     *
     * @param arguments used to take arguments from the running environment
     */
    public static void main( final String[] arguments ) {

        if ( processArguments( arguments ) ) {
            if ( policyFileName == null )
                new MainFrame().setVisible( true );
            else
                new MainFrame( policyFileName ).setVisible( true );
        }

    }

    /**
     * Processes the command line arguments.<br>
     * Currently only one option is supported:
     * <pre><code>    [-file file]</code></pre>
     * for specifying the name of a policy file to be loaded initially.
     *
     * @param arguments arguments taken from the running environment
     * @return true if arguments were processed successfully and launching the GUI is allowed;<br>
     *         false if there were missing or invalid arguments, or no GUI launch is needed
     */
    private static boolean processArguments( final String[] arguments ) {
        if ( arguments.length == 0 )
            return true;

        else {
            if ( arguments[ 0 ].startsWith( "-" ) ) // If it is a "real" option
                if ( arguments[ 0 ].equalsIgnoreCase( "-file" ) ) {
                    if ( arguments.length < 2 ) {   // policy file name must be provided
                        printErrorMessageAndUsage( "Missing policy file name!" );
                        return false;
                    } else {
                        policyFileName = arguments[ 1 ];
                        return true;
                    }
                } else {
                    printErrorMessageAndUsage( "Illegal option: " + arguments[ 0 ] );
                    return false;
                } else
                    return true;  // else the arguments are ignored
        }
    }

    /**
     * Prints an error message to the standard output followed by the program ussage.
     * @param errorMessage error message to be printed
     */
    private static void printErrorMessageAndUsage( final String errorMessage ) {
        System.out.println( errorMessage );
        printUsage();
    }

    /**
     * Prints the program usage to the standard output.
     */
    private static void printUsage() {
        System.out.println( "Usage: policytool [options]" );
        System.out.println();
        System.out.println( "  [-file <file>]    name of policy file to be loaded initially" );
    }

}
