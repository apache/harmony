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
package org.apache.harmony.jretools.unpack200;

import org.apache.harmony.unpack200.Archive;

/**
 * Main class for the unpack200 command line tool
 */
public class Main {

    public static void main(String args[]) throws Exception {

        String inputFileName = null;
        String outputFileName = null;
        boolean removePackFile = false;
        boolean verbose = false;
        boolean quiet = false;
        boolean overrideDeflateHint = false;
        boolean deflateHint = false;
        String logFileName = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help") || args[i].equals("-help")
                    || args[i].equals("-h") || args[i].equals("-?")) {
                printHelp();
                return;
            } else if(args[i].equals("-Htrue") || args[i].equals("--deflate-hint=true")) {
                overrideDeflateHint = true;
                deflateHint = true;
            } else if(args[i].equals("-Hfalse") || args[i].equals("--deflate-hint=false")) {
                overrideDeflateHint = true;
                deflateHint = false;
            } else if(args[i].equals("-Hkeep") || args[i].equals("--deflate-hint=keep")) {
                overrideDeflateHint = false;
            } else if(args[i].equals("-r") || args[i].equals("--remove-pack-file")) {
                removePackFile = true;
            } else if(args[i].equals("-v") || args[i].equals("--verbose")) {
                verbose = true;
                quiet = false;
            } else if(args[i].equals("-q") || args[i].equals("--quiet")) {
                quiet = true;
                verbose = false;
            } else if(args[i].startsWith("-l")) {
                logFileName = args[i].substring(2);
            } else if(args[i].equals("-V") || args[i].equals("--version")) {
                printVersion();
                return;
            } else {
                inputFileName = args[i];
                if(args.length > i + 1) {
                    outputFileName = args[i+1];
                }
                break;
            }
        }
        if(inputFileName == null || outputFileName == null) {
            printUsage();
            return;
        }
        Archive archive = new Archive(inputFileName, outputFileName);
        archive.setRemovePackFile(removePackFile);
        archive.setVerbose(verbose);
        archive.setQuiet(quiet);
        if(overrideDeflateHint) {
            archive.setDeflateHint(deflateHint);
        }
        if(logFileName != null) {
            archive.setLogFile(logFileName);
        }
        archive.unpack();
    }

    private static void printUsage() {
        System.out.println("Usage:  unpack200 [-opt... | --option=value]... x.pack[.gz] y.jar");
        System.out.println("(For more information, run unpack200 --help)");
    }

    private static void printHelp() {
        System.out.println("Usage:  unpack200 [-opt... | --option=value]... x.pack[.gz] y.jar");
        System.out.println();
        System.out.println("Options:");
        System.out.println("-H{h}, --deflate-hint={h}  Set the deflate hint for the output file to {h}, either true, false or keep");
        System.out.println("-r, --remove-pack-file     Delete the input file after unpacking");
        System.out.println("-v, --verbose              Print verbose output");
        System.out.println("-q, --quiet                Print no output");
        System.out.println("-l{F}, --log-file={F}      Print output to the log file {F}");
        System.out.println("-?, -h, --help             Show the help message");
        System.out.println("-V, --version              Show the program version number");
    }

    private static void printVersion() {
        System.out.println("Apache Harmony unpack200 version 0.0");  // TODO - version number
    }

}
