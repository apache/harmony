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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.harmony.rmi.common.JavaCompiler;
import org.apache.harmony.rmi.common.JavaCompilerException;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Core class of RMI Compiler.
 *
 * @author  Vasily Zakharov
 *
 * @todo    Implement IDL/IIOP support.
 */
public final class RMICompiler implements RmicConstants, RmicStrings {

    /**
     * Version of stubs to generate.
     */
    private final int version;

    /**
     * Option: do not delete generated source files
     * (<code>-keep</code>).
     */
    private final boolean keepSources;

    /**
     * Option: always regenerate IDL/IIOP stubs
     * (<code>-always</code>).
     */
    private final boolean always;

    /**
     * Option: use factory keyword in generated IDL stubs
     * (<code>-factory</code>).
     */
    private final boolean factory;

    /**
     * Option: valuetype methods and initializers in IDL stubs
     * (<code>-noValueMethods</code>).
     */
    private final boolean valueMethods;

    /**
     * Option: create stubs optimized for the same process
     * (<code>-nolocalstubs</code>).
     */
    private final boolean localStubs;

    /**
     * Option: change POA inheritance
     * (<code>-poa</code>).
     */
    private final boolean poa;

    /**
     * Option: generate debug information
     * (<code>-debug</code>).
     */
    private final boolean debug;

    /**
     * Option: notify about compiler warnings
     * (<code>-nowarn</code>).
     */
    private final boolean warnings;

    /**
     * Option: do not write compiled classes
     * (<code>-nowrite</code>).
     */
    private final boolean writeClasses;

    /**
     * Option: print detailed compilation log
     * (<code>-verbose</code>).
     */
    private final boolean verbose;

    /**
     * Option: recompile dependent classes.
     * (<code>-depend</code>).
     */
    private final boolean depend;

    /**
     * Option: destination directory for generated source and class files.
     * (<code>-d</code>).
     */
    private final String destinationDir;

    /**
     * If any options were specified.
     */
    private final boolean optionsPresent;

    /**
     * Java compiler options.
     */
    private final String[] javacOptions;

    /**
     * Warning flags.
     */
    private HashMap warningTags = new HashMap();

    /**
     * Classes to generate stubs for.
     *
     * We use {@link Object} array to store either {@link String}
     * or {@link Class} objects because we don't want to resolve all the class
     * names in the beginning - to avoid one incorrectly spelled class name
     * preventing generation of stubs for other specified classes.
     */
    private Object[] classes;

    /**
     * Number of classes in {@link #classes} array.
     */
    private int numClasses;

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classes
     *              Classes to process.
     *              Call {@link #run()} to process them.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String[] args, Class[] classes)
            throws RMICompilerException {
        this(args, (Object[]) classes);
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classNames
     *              Names of classes to process.
     *              Call {@link #run()} to process them.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String[] args, String[] classNames)
            throws RMICompilerException {
        this(args, (Object[]) classNames);
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classNames
     *              Names of classes to process (space separated).
     *              Call {@link #run()} to process them.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String[] args, String classNames)
            throws RMICompilerException {
        this(args, (Object[]) stringToArray(classNames));
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *              Call {@link #run(Class[])} or {@link #run(String[])}
     *              to specify classes to process and process them immediately.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String[] args) throws RMICompilerException {
        this(args, (Object[]) null);
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options, space separated (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classes
     *              Classes to process.
     *              Call {@link #run()} to process them.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String args, Class[] classes)
            throws RMICompilerException {
        this(stringToArray(args), (Object[]) classes);
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options, space separated (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classNames
     *              Names of classes to process.
     *              Call {@link #run()} to process them.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String args, String[] classNames)
            throws RMICompilerException {
        this(stringToArray(args), (Object[]) classNames);
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options, space separated (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classNames
     *              Names of classes to process (space separated).
     *              Call {@link #run()} to process them.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String args, String classNames)
            throws RMICompilerException {
        this(stringToArray(args), (Object[]) stringToArray(classNames));
    }

    /**
     * Constructs instance of RMI Compiler.
     *
     * @param       args
     *              RMI Compiler options, space separated (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *              Call {@link #run(Class[])} or {@link #run(String[])}
     *              to specify classes to process and process them immediately.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public RMICompiler(String args) throws RMICompilerException {
        this(stringToArray(args), (Object[]) null);
    }

    /**
     * Private constructor, called from all other constructors.
     *
     * @param       args
     *              RMI Compiler options (see
     *              <a href="package-summary.html">package description</a>
     *              for details).
     *
     * @param       classes
     *              Classes to process ({@link Class} or {@link String}
     *              objects).
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    private RMICompiler(String[] args, Object[] classes)
            throws RMICompilerException {
        int numArgs = args.length;

        int version = VERSION_NOT_SET;
        boolean keepSources = false;
        boolean always = false;
        boolean factory = false;
        boolean valueMethods = true;
        boolean localStubs = true;
        boolean poa = false;
        boolean debug = false;
        boolean warnings = true;
        boolean writeClasses = true;
        boolean verbose = false;
        boolean depend = false;
        boolean optionsPresent = (numArgs > 0);
        String destinationDir = "."; //$NON-NLS-1$

        ArrayList javacOptionsList = new ArrayList();

        // User doesn't need any warnings on compiling stubs, as all possible
        // issues that may appear concern RMI specification, not user classes.
        javacOptionsList.add(optionNoWarnings);

        // Parse arguments, adjust values of option fields,
        // add necessary options to javacOptionsList.
        for (int i = 0; i < numArgs; i++) {
            String arg = args[i].intern();

            if (arg == optionV11) {
                if (version == VERSION_NOT_SET) {
                    version = VERSION_V11;
                } else {
                    error(errorVersionText);
                }
            } else if (arg == optionV12) {
                if (version == VERSION_NOT_SET) {
                    version = VERSION_V12;
                } else {
                    error(errorVersionText);
                }
            } else if (arg == optionVCompat) {
                if (version == VERSION_NOT_SET) {
                    version = VERSION_VCOMPAT;
                } else {
                    error(errorVersionText);
                }
            } else if (arg == optionIDL) {
                if (version == VERSION_NOT_SET) {
                    version = VERSION_IDL;
                } else {
                    error(errorVersionText);
                }
            } else if (arg == optionIIOP) {
                if (version == VERSION_NOT_SET) {
                    version = VERSION_IIOP;
                } else {
                    error(errorVersionText);
                }
            } else if (arg == optionTarget) {
                if (i < (numArgs - 1)) {
                    // If parameter is available,
                    // add options to javacOptionsList.
                    String target = args[++i].intern();
                    String source = (((target == "1.1") || (target == "1.2")) //$NON-NLS-1$ //$NON-NLS-2$
                            ? "1.3" : target); //$NON-NLS-1$

                    javacOptionsList.add(optionSource);
                    javacOptionsList.add(source);
                    javacOptionsList.add(optionTarget);
                    javacOptionsList.add(target);
                } else {
                    error(errorNeedParameterText, arg);
                }
            } else if ((arg == optionKeep) || (arg == optionKeepGenerated)) {
                keepSources = true;
            } else if ((arg == optionAlways) || (arg == optionAlwaysGenerate)) {
                always = true;
            } else if (arg == optionFactory) {
                factory = true;
            } else if (arg == optionNoValueMethods) {
                valueMethods = false;
            } else if (arg == optionNoLocalStubs) {
                localStubs = false;
            } else if (arg == optionPOA) {
                poa = true;
            } else if ((arg == optionDebug)
                    || arg.startsWith(optionDebugDetails)) {
                javacOptionsList.add(arg);
                debug = true;
            } else if (arg == optionNoWarnings) {
                warnings = false;
            } else if (arg == optionNoWrite) {
                writeClasses = false;
            } else if (arg == optionVerbose) {
                javacOptionsList.add(arg);
                verbose = true;
            } else if (arg == optionDepend) {
                depend = true;
            } else if ((arg == optionIdlModule) || (arg == optionIdlFile)) {
                if (i < (numArgs - 2)) {
                    // @ToDo: implement for IDL support.
                    i += 2;
                } else {
                    error(errorNeedTwoParametersText, arg);
                }
            } else if ((arg == optionClassPath) || (arg == optionCP)
                    || (arg == optionBootClassPath) || (arg == optionExtDirs)
                    || (arg == optionDestinationDir)) {
                if (i < (numArgs - 1)) {
                    // If parameter is available,
                    // add option to javacOptionsList.
                    String option = ((arg == optionCP) ? optionClassPath : arg);
                    javacOptionsList.add(option);
                    String param = args[++i];
                    javacOptionsList.add(param);

                    if (arg == optionDestinationDir) {
                        destinationDir = param;
                    } else {
                        addWarning(option.substring(1), warningClassPathText);
                    }
                } else {
                    error(errorNeedParameterText, arg);
                }
            } else if (arg.startsWith(optionJava) || arg.startsWith(optionX)) {
                if (arg.length() > 2) {
                    // If parameter is available,
                    // add option to javacOptionsList.
                    javacOptionsList.add(arg);
                } else {
                    error(errorNeedJVMParameterText, arg);
                }
            } else if (arg.startsWith(optionPrefix)) {
                // What starts with dash is probably the non-specified option.
                error(errorUnknownOptionText, arg);
            } else {
                // First arg that is not an option.
                if (classes != null) {
                    // If classes are specified explicitly,
                    // then this is just an incorrect option.
                    error(errorUnknownOptionText, arg);
                } else {
                    // If classes are not specified explicitly,
                    // extract them from args.
                    int numClasses = (numArgs - i);
                    classes = new Object[numClasses];
                    System.arraycopy(args, i, classes, 0, numClasses);

                    if (i == 0) {
                        // Mark that no options were really specified.
                        optionsPresent = false;
                    }
                }
                break;
            }
        }

        // Print warnings.
        if (warnings) {
            for (Iterator i = warningTags.values().iterator(); i.hasNext(); ) {
                // rmi.console.1A=WARNING: {0}
                System.err.println(Messages.getString("rmi.console.1A", i.next())); //$NON-NLS-1$
            }
        }

        // Check options compatibility.
        if (always && (version != VERSION_IDL) && (version != VERSION_IIOP)) {
            error(errorUnusableExceptIDL_IIOP, optionAlways);
        }

        if (factory && (version != VERSION_IDL)) {
            error(errorUnusableExceptIDL, optionFactory);
        }

        if (!valueMethods && (version != VERSION_IDL)) {
            error(errorUnusableExceptIDL, optionNoValueMethods);
        }

        if (!localStubs && (version != VERSION_IIOP)) {
            error(errorUnusableExceptIIOP, optionNoLocalStubs);
        }

        if (poa && (version != VERSION_IIOP)) {
            error(errorUnusableExceptIIOP, optionPOA);
        }

        if (version == VERSION_NOT_SET) {
            version = VERSION_V12;
        }

        // Save configuration.
        this.classes = ((classes != null) ? classes : new Object[0]);
        this.numClasses = this.classes.length;

        this.version = version;
        this.keepSources = keepSources;
        this.always = always;
        this.factory = factory;
        this.valueMethods = valueMethods;
        this.localStubs = localStubs;
        this.poa = poa;
        this.debug = debug;
        this.warnings = warnings;
        this.writeClasses = writeClasses;
        this.verbose = verbose;
        this.depend = depend;
        this.optionsPresent = optionsPresent;
        this.destinationDir = destinationDir;

        // "Export" Javac options.
        javacOptions = (String[]) javacOptionsList.toArray(
                new String[javacOptionsList.size()]);
    }

    /**
     * Runs the compilation process.
     *
     * Note that to call this method classes to compile must be specified
     * in constructor or in previous call to {@link #run(Class[])}
     * or {@link #run(String[])}.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public void run() throws RMICompilerException {
        if (numClasses < 1) {
            if (optionsPresent) {
                error(errorNoClassesText);
            } else {
                usage();
            }
        }

        // Create files array for stub and skeleton files.
        File[] stubFiles = new File[numClasses];
        File[] skelFiles = new File[numClasses];
        File[] skelClassFiles = new File[numClasses];
        int filesNum = 0;

        try {
            // Walk through the specified classes.
            for (int i = 0; i < numClasses; i++) {

                // Find out the class to process.
                Object obj = classes[i];
                Class cls;

                if (obj instanceof Class) {
                    cls = (Class) obj;
                } else {    // (obj instanceof String)
                    String className = (String) obj;

                    try {
                        cls = Class.forName(className);
                        classes[i] = cls;
                    } catch (ClassNotFoundException e) {
                        // rmi.55=Class not found: {0}
                        throw new RMICompilerException(
                                Messages.getString("rmi.55", e.getMessage()), e); //$NON-NLS-1$
                    } catch (LinkageError e) {
                        // rmi.57=Class loading error: {0}
                        throw new RMICompilerException(
                                Messages.getString("rmi.57", e.getMessage()), e); //$NON-NLS-1$
                    }
                }

                // Create class stub.
                ClassStub stub = new ClassStub(version, cls);

                String packageName = RMIUtil.getPackageName(cls);

                if (packageName != null) {
                    packageName = packageName.replace('.', File.separatorChar);
                }

                String stubClassName = stub.getStubClassName();
                String skelClassName = stub.getSkeletonClassName();

                File dir = RmicUtil.getPackageDir(destinationDir, packageName);

                // Generate stub source file name.
                File stubFile = RmicUtil.getPackageFile(dir,
                        stubClassName + javaSuffix);
                stubFiles[filesNum] = stubFile;

                // Generate skeleton source file name.
                File skelFile = RmicUtil.getPackageFile(dir,
                        skelClassName + javaSuffix);
                skelFiles[filesNum] = skelFile;

                // Generate skeleton class file name.
                skelClassFiles[filesNum] = RmicUtil.getPackageFile(dir,
                        skelClassName + classSuffix);

                filesNum++;

                try {
                    // Write generated stub source to the file.
                    FileWriter writer = new FileWriter(stubFile);
                    writer.write(stub.getStubSource());
                    writer.close();
                } catch (IOException e) {
                    // rmi.58=Can't write file {0}
                    throw new RMICompilerException(
                            Messages.getString("rmi.58", stubFile.getName()), e); //$NON-NLS-1$
                }

                if (version != VERSION_V12) {
                    try {
                        // Write generated skeleton source to the file.
                        FileWriter writer = new FileWriter(skelFile);
                        writer.write(stub.getSkeletonSource());
                        writer.close();
                    } catch (IOException e) {
                        // rmi.58=Can't write file {0}
                        throw new RMICompilerException(
                                Messages.getString("rmi.58", skelFile.getName()), e); //$NON-NLS-1$
                    }
                }
            }

            // Prepare files array for compilation.
            File[] files;

            if (version == VERSION_V12) {
                files = stubFiles;
            } else {
                files = new File[2 * numClasses];

                int j = 0;
                for (int i = 0; i < numClasses; i++) {
                    files[j++] = stubFiles[i];
                    files[j++] = skelFiles[i];
                }
            }

            try {
                // Invoke Java compiler for generated files.
                int ret = JavaCompiler.locateJavaCompiler(verbose)
                        .compile(javacOptions, files);

                if (ret != 0) {
                    // rmi.59=Javac failed, code {0}
                    throw new RMICompilerException(Messages.getString("rmi.59", ret)); //$NON-NLS-1$
                }
            } catch (JavaCompilerException e) {
                // rmi.5A=Can't run Javac: {0}
                throw new RMICompilerException(Messages.getString("rmi.5A", e), e); //$NON-NLS-1$
            }
        } finally {

            // Remove generated stub and skeleton files even if exception arose
            if (!keepSources) {
                for (int i = 0; i < filesNum; i++) {
                    stubFiles[i].delete();
                }
            }

            // Remove skeleton files if version is set to 1.2.
            if (!keepSources || (version == VERSION_V12)) {
                for (int i = 0; i < filesNum; i++) {
                    skelFiles[i].delete();
                }
            }

            // Remove skeleton class files if version is set to 1.2.
            if (version == VERSION_V12) {
                for (int i = 0; i < filesNum; i++) {
                    skelClassFiles[i].delete();
                }
            }
        }
    }

    /**
     * Runs the compilation process.
     *
     * @param       classes
     *              Classes to compile. This classes list replace any previous
     *              specifications of classes to compile, made in constructor
     *              or in other calls to <code>run()</code> methods.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public void run(Class[] classes) throws RMICompilerException {
        this.classes = (Object[]) classes;
        run();
    }

    /**
     * Runs the compilation process.
     *
     * @param       classNames
     *              Names of classes to compile. This classes list replace any
     *              previous specifications of classes to compile, made in
     *              constructor or in other calls to <code>run()</code> methods.
     *
     * @throws      RMICompilerException
     *              If some error occurs.
     */
    public void run(String[] classNames) throws RMICompilerException {
        this.classes = (Object[]) classNames;
        run();
    }

    /**
     * Produces usage information.
     *
     * @throws      RMICompilerException
     *              Always. Exception message contains usage information.
     */
    private static void usage() throws RMICompilerException {
        throw new RMICompilerException(EOLN + usageText);
    }

    /**
     * Produces error message.
     *
     * @param       message
     *              Error message.
     *
     * @throws      RMICompilerException
     *              Always. Exception message contains the specified message
     *              and usage information.
     */
    private static void error(String message) throws RMICompilerException {
        throw new RMICompilerException(message + EOLN);
    }

    /**
     * Produces error message.
     *
     * @param       message
     *              Error message, can contain references to a single argument,
     *              specified by <code>arg</code>. References are represented
     *              by <code>%s</code> substrings.
     *
     * @param       arg
     *              Argument. Each occurrence of <code>%s</code>
     *              in <code>message</code> is replaced with this string.
     *
     * @throws      RMICompilerException
     *              Always. Exception message contains the specified message
     *              and usage information.
     */
    private static void error(String message, String arg)
            throws RMICompilerException {
        error(message.replaceAll("%s", arg)); //$NON-NLS-1$
    }

    /**
     * Produces warning message.
     *
     * @param       tag
     *              Warning tag. Used to track warnings. Also, each occurrence
     *              of <code>%s</code> in <code>message</code> is replaced
     *              with this string.
     *
     * @param       message
     *              Warning message, can contain references to a single argument,
     *              specified by <code>arg</code>. References are represented
     *              by <code>%s</code> substrings.
     */
    private void addWarning(String tag, String message) {
        warningTags.put(tag, message.replaceAll("%s", tag)); //$NON-NLS-1$
    }

    /**
     * Parses string to a number of tokens, using spaces as separators.
     * Indeed, {@link StringTokenizer} with no parameters is used.
     *
     * @param       string
     *              String to process.
     *
     * @return      String array containing all tokens of the specified string.
     */
    private static String[] stringToArray(String string) {
        StringTokenizer tokenizer = new StringTokenizer(string);
        int numTokens = tokenizer.countTokens();
        String[] array = new String[numTokens];

        for (int i = 0; i < numTokens; i++) {
            array[i] = tokenizer.nextToken();
        }

        return array;
    }
}
