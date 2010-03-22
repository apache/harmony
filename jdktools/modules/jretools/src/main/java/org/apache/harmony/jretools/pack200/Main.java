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
package org.apache.harmony.jretools.pack200;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.harmony.pack200.PackingOptions;

/**
 * Main class for the pack200 command line tool.
 */
public class Main {

    public static void main(String args[]) throws Exception {
        String inputFileName = null;
        String outputFileName = null;
        PackingOptions options = new PackingOptions();
        String value = null;

        for (int i = 0; i < args.length; i++) {
            if ("--help".equals(args[i]) || "-help".equals(args[i])
                    || "-h".equals(args[i]) || "-?".equals(args[i])) {
                printHelp();
                return;
            } else if ("-V".equals(args[i]) || "--version".equals(args[i])) {
                printVersion();
                return;
            } else if ("-g".equals(args[i]) || "--no-gzip".equals(args[i])) {
                options.setGzip(false);
            } else if ("--gzip".equals(args[i])) {
                options.setGzip(true);
            } else if ("-G".equals(args[i]) || "--strip-debug".equals(args[i])) {
                options.setStripDebug(true);
            } else if ("-O".equals(args[i])
                    || "--no-keep-file-order".equals(args[i])) {
                options.setKeepFileOrder(false);
            } else if ("--keep-file-order".equals(args[i])) {
                options.setKeepFileOrder(true);
            } else if (args[i].startsWith("-S")) {
                value = args[i].substring(2);
                if(value.length() == 0) {
                    if(i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -S ?");
                        printUsage();
                        return;
                    }
                }
                options.setSegmentLimit(Long.parseLong(value));
            } else if (args[i].startsWith("--segment-limit=")) {
                options.setSegmentLimit(Long.parseLong(args[i].substring(16)));
            } else if (args[i].startsWith("-E")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -E ?");
                        printUsage();
                        return;
                    }
                }
                options.setEffort(Integer.parseInt(value));
            } else if (args[i].startsWith("--effort=")) {
                options.setEffort(Integer.parseInt(args[i].substring(9)));
            } else if (args[i].startsWith("-H")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -H ?");
                        printUsage();
                        return;
                    }
                }
                options.setDeflateHint(value);
            } else if (args[i].startsWith("--deflate-hint=")) {
                options.setDeflateHint(args[i].substring(15));
            } else if (args[i].startsWith("-m")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -m ?");
                        printUsage();
                        return;
                    }
                }
                options.setModificationTime(value);
            } else if (args[i].startsWith("--modification-time=")) {
                options.setModificationTime(args[i].substring(20));
            } else if (args[i].startsWith("-P")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -P ?");
                        printUsage();
                        return;
                    }
                }
                options.addPassFile(value);
            } else if (args[i].startsWith("--pass-file=")) {
                options.addPassFile(args[i].substring(12));
            } else if (args[i].startsWith("-U")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -U ?");
                        printUsage();
                        return;
                    }
                }
                options.setUnknownAttributeAction(value);
            } else if (args[i].startsWith("--unknown-attribute=")) {
                options.setUnknownAttributeAction(args[i].substring(20));
            } else if (args[i].startsWith("-C")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -C ?");
                        printUsage();
                        return;
                    }
                }
                String[] nameEqualsAction = value.split("=");
                options.addClassAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("--class-attribute=")) {
                String[] nameEqualsAction = args[i].substring(18).split("=");
                options.addClassAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("-F")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -F ?");
                        printUsage();
                        return;
                    }
                }
                String[] nameEqualsAction = value.split("=");
                options.addFieldAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("--field-attribute=")) {
                String[] nameEqualsAction = args[i].substring(18).split("=");
                options.addFieldAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("-M")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -M ?");
                        printUsage();
                        return;
                    }
                }
                String[] nameEqualsAction = value.split("=");
                options.addMethodAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("--method-attribute=")) {
                String[] nameEqualsAction = args[i].substring(19).split("=");
                options.addMethodAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("-D")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -D ?");
                        printUsage();
                        return;
                    }
                }
                String[] nameEqualsAction = value.split("=");
                options.addCodeAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if (args[i].startsWith("--code-attribute=")) {
                String[] nameEqualsAction = args[i].substring(17).split("=");
                options.addCodeAttributeAction(nameEqualsAction[0],
                        nameEqualsAction[1]);
            } else if ("-v".equals(args[i]) || "--verbose".equals(args[i])) {
                options.setVerbose(true);
                options.setQuiet(false);
            } else if ("-q".equals(args[i]) || "--quiet".equals(args[i])) {
                options.setQuiet(true);
                options.setVerbose(false);
            } else if (args[i].startsWith("-l")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -l ?");
                        printUsage();
                        return;
                    }
                }
                options.setLogFile(value);
            } else if (args[i].startsWith("--log-file=")) {
                options.setLogFile(args[i].substring(11));
            } else if ("-r".equals(args[i]) || "--repack".equals(args[i])) {
                options.setRepack(true);
            } else if (args[i].startsWith("-f")) {
                value = args[i].substring(2);
                if (value.length() == 0) {
                    if (i + 1 < args.length) {
                        value = args[++i];
                    } else {
                        printErrorMessage("Bad argument: -f ?");
                        printUsage();
                        return;
                    }
                }
                loadPackProperties(value, options);
            } else if (args[i].startsWith("--config-file=")) {
                loadPackProperties(args[i].substring(14), options);
            } else {
                outputFileName = args[i];
                if (args.length > i + 1) {
                    if (args.length == i + 2) {
                        inputFileName = args[++i];
                    } else {
                        printUsage();
                        return;
                    }
                }
            }
        }

        if (options.isRepack()) {
            repack(inputFileName, outputFileName, options);
        } else {
            pack(inputFileName, outputFileName, options);
        }
    }

    /*
     * Load properties for packing
     */
    private static void loadPackProperties(String packPropertyFileName,
            PackingOptions options) throws IOException {
        Properties packProperties = new Properties();
        packProperties.load(new FileInputStream(packPropertyFileName));
        Enumeration propertyNames = packProperties.propertyNames();
        String propretyName, propretyValue;
        while (propertyNames.hasMoreElements()) {
            propretyName = (String) propertyNames.nextElement();
            propretyValue = packProperties.getProperty(propretyName);

            if ("deflate.hint".equals(propretyName)) {
                options.setDeflateHint(propretyValue);
            } else if ("effort".equals(propretyName)) {
                options.setEffort(Integer.parseInt(propretyValue));
            } else if ("keep.file.order".equals(propretyName)) {
                options.setKeepFileOrder(Boolean.getBoolean(propretyValue));
            } else if ("modification.time".equals(propretyName)) {
                options.setModificationTime(propretyName);
            } else if ("segment.limit".equals(propretyName)) {
                options.setSegmentLimit(Integer.parseInt(propretyValue));
            } else if ("unknown.attribute".equals(propretyName)) {
                options.setUnknownAttributeAction(propretyValue);
            }
        }
    }

    /*
     * Pack input stream of jar file into output stream
     */
    private static void pack(String inputFileName, String outputFileName,
            PackingOptions options) throws Exception {
        if (inputFileName == null || outputFileName == null) {
            printUsage();
            return;
        }

        if (options.isGzip() && !outputFileName.endsWith(".gz")) {
            printErrorMessage("To write a *.pack file, specify --no-gzip: "
                    + outputFileName);
            printUsage();
            return;
        }

        JarFile jarFile = new JarFile(inputFileName);
        OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputFileName));
        org.apache.harmony.pack200.Archive archive = new org.apache.harmony.pack200.Archive(
                jarFile, outputStream, options);
        archive.pack();
    }

    /*
     * Repack input stream of jar file into output stream of jar file
     */
    private static void repack(String inputFileName, String outputFileName,
            PackingOptions options) throws Exception {
        if (outputFileName == null) {
            printUsage();
            return;
        }

        if (inputFileName == null) {
            inputFileName = outputFileName;
        }

        // packing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        org.apache.harmony.pack200.Archive packer;
        // this is a workround for compatibility with RI
        if (0 == options.getEffort()) {
            packer = new org.apache.harmony.pack200.Archive(new JarInputStream(
                    new FileInputStream(inputFileName)), outputStream, options);
        } else {
            packer = new org.apache.harmony.pack200.Archive(new JarFile(
                    inputFileName), outputStream, options);
        }
        packer.pack();

        // unpacking
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                outputStream.toByteArray());
        JarOutputStream jarOutputStream = new JarOutputStream(
                new FileOutputStream(outputFileName));
        org.apache.harmony.unpack200.Archive unpacker = new org.apache.harmony.unpack200.Archive(
                inputStream, jarOutputStream);
        unpacker.setVerbose(options.isVerbose());
        unpacker.setQuiet(options.isQuiet());
        // set deflate_hint option
        if (!options.isKeepDeflateHint()) {
            unpacker.setDeflateHint("true".equals(options.getDeflateHint()));
        }
        // set log file
        String logFile = options.getLogFile();
        if(logFile != null) {
            unpacker.setLogFile(logFile, true);
        }
        unpacker.unpack();
    }

    private static void printErrorMessage(String mesg) {
        System.out.println("Error: " + mesg);
    }

    private static void printUsage() {
        System.out.println("Usage:  pack200 [-opt... | --option=value]... x.pack[.gz] y.jar");
        System.out.println("(For more information, run pack200 --help)");
    }

    private static void printHelp() {
        System.out.println("Usage:  pack200 [-opt... | --option=value]... x.pack[.gz] y.jar");
        System.out.println();
        System.out.println("Packing Options");
        System.out.println("  -g, --no-gzip                   output a plain *.pack file with no zipping");
        System.out.println("  --gzip                          (default) post-process the pack output with gzip");
        System.out.println("  -G, --strip-debug               remove debugging attributes while packing");
        System.out.println("  -O, --no-keep-file-order        do not transmit file ordering information");
        System.out.println("  --keep-file-order               (default) preserve input file ordering");
        System.out.println("  -S{N}, --segment-limit={N}      output segment limit (default N=1Mb)");
        System.out.println("  -E{N}, --effort={N}             packing effort (default N=5)");
        System.out.println("  -H{h}, --deflate-hint={h}       transmit deflate hint: true, false, or keep (default)");
        System.out.println("  -m{V}, --modification-time={V}  transmit modtimes: latest or keep (default)");
        System.out.println("  -P{F}, --pass-file={F}          transmit the given input element(s) uncompressed");
        System.out.println("  -U{a}, --unknown-attribute={a}  unknown attribute action: error, strip, or pass (default)");
        System.out.println("  -C{N}={L}, --class-attribute={N}={L}  (user-defined attribute)");
        System.out.println("  -F{N}={L}, --field-attribute={N}={L}  (user-defined attribute)");
        System.out.println("  -M{N}={L}, --method-attribute={N}={L} (user-defined attribute)");
        System.out.println("  -D{N}={L}, --code-attribute={N}={L}   (user-defined attribute)");
        System.out.println("  -f{F}, --config-file={F}        read file F for Pack200.Packer properties");
        System.out.println("  -v, --verbose                   increase program verbosity");
        System.out.println("  -q, --quiet                     set verbosity to lowest level");
        System.out.println("  -l{F}, --log-file={F}           output to the given log file, or '-' for System.out");
        System.out.println("  -?, -h, --help                  print this message");
        System.out.println("  -V, --version                   print program version");
        System.out.println("  -J{X}                           pass option X to underlying Java VM");
        System.out.println("");
        System.out.println("Notes:");
        System.out.println("  The -P, -C, -F, -M, and -D options accumulate.");
        System.out.println("  Example attribute definition:  -C SourceFile=RUH .");
        System.out.println("  Config. file properties are defined by the Pack200 API.");
        System.out.println("  For meaning of -S, -E, -H-, -m, -U values, see Pack200 API.");
        System.out.println("  Layout definitions (like RUH) are defined by JSR 200.");
        System.out.println("");
        System.out.println("Repacking mode updates the JAR file with a pack/unpack cycle:");
        System.out.println("    pack200 [-r|--repack] [-opt | --option=value]... [repackedy.jar] y.jar");
    }

    private static void printVersion() {
        System.out.println("Apache Harmony pack200 version 0.0");  // TODO - version number
    }

}
