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

package org.apache.harmony.tools.javac;

import java.io.File;
import java.io.PrintWriter;

import org.apache.harmony.tools.toolutils.Util;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

/**
 * This is the entry point for the javac tool.
 */
public final class Main {

    // javac tool return codes
    public static final int RC_SUCCESS = 0;
    public static final int RC_COMPILE_ERROR = 1;
    public static final int RC_USAGE_ERROR = 2;

    /*
     * Command-line tool invokes this method.
     */
    public static void main(String[] args) {
        int rc = new Main().compile(args);
        System.exit(rc);
    }

    /**
     * Default constructor.
     */
    public Main() {
        super();
    }

    /**
     * Invokes the ECJ compiler with the given arguments.
     * 
     * @param args
     *            the arguments passed through to the compiler
     * @return a return code as defined by this class
     */
    public int compile(String[] args) {
        return compile(args,
                Util.getDefaultWriter(System.out),
                Util.getDefaultWriter(System.err));
    }

    /**
     * Invokes the ECJ compiler with the given arguments.
     * 
     * @param args
     *            the arguments passed through to the compiler
     * @param out
     *            get the output from System.out
     * @param err
     *            get the output from System.err
     * @return a return code as defined by this class
     */
    public int compile(String[] args, PrintWriter out, PrintWriter err) {

        /* Give me something to do, or print usage message */
        if (args == null || args.length == 0) {
            BatchCompiler.compile("-help", out, err, null); //$NON-NLS-1$
            return RC_USAGE_ERROR;
        }

        /* Add in the base class library code to compile against */
        String[] newArgs = addLocalArgs(args);

        /* Invoke the compiler */
        boolean success = BatchCompiler.compile(newArgs, out, err, null);
        return success ? RC_SUCCESS : RC_COMPILE_ERROR;
    }

    /*
     * Set up the compiler option to compile against the running JRE class
     * libraries.
     */
    private String[] addLocalArgs(String[] args) {
        StringBuilder sb = new StringBuilder();
        String[] result = new String[args.length + 3];

        System.arraycopy(args, 0, result, 0, args.length);
        result[args.length] = "-classpath"; //$NON-NLS-1$
        sb.append(System.getProperty(
                        "org.apache.harmony.boot.class.path", ".")); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append(File.pathSeparator);
        sb.append(System.getProperty("sun.boot.class.path", ".")); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append(File.pathSeparator);
        sb.append("."); //$NON-NLS-1$
        result[args.length + 1] = sb.toString();
        result[args.length + 2] = "-1.5"; //$NON-NLS-1$
        return result;
    }
}
