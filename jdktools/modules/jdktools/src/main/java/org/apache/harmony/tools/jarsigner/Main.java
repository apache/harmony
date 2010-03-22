/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.jarsigner;

import java.io.OutputStream;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;


/**
 * The main class that bundles command line parsing, interaction with the user,
 * exception handling and JAR signing and verification work.
 */
public class Main {
    /**
     * The main method to run from another program.
     * Parses the arguments, and performs the actual work 
     * on JAR signing and verification. 
     * If something goes wrong an exception is thrown.
     * 
     * @param args -
     *            command line with options.
     */
    public static void run(String[] args, OutputStream out) throws Exception {
        // set up logging
        Logger logger = Logger.getLogger(JSParameters.loggerName);
        logger.setUseParentHandlers(false);
        Handler handler = new StreamHandler(out, new JSLogFormatter());
        logger.addHandler(handler);
        
        // parse command line arguments
        JSParameters param = ArgParser.parseArgs(args, null);
        // print help if incorrect or no arguments
        if (param == null) {
            JSHelper.printHelp();
            return;
        }
        // do the actual work
        if (param.isVerify()) {
            JSVerifier.verifyJar(param);
        } else {
            JSSigner.signJar(param);
        }
    }

    
    /**
     * The main method to run from command line.
     * 
     * @param args -
     *            command line with options.
     */
    public static void main(String[] args) {
        try {
            run(args, System.out);
        } catch (Exception e) {
            System.out.print("JarSigner error: "
                    + e
                    + ((e.getCause() != null) ? ", caused by " + e.getCause()
                            : ""));
            //e.printStackTrace();
        }
    }

}

