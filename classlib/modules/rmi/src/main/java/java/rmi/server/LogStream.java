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
 * @author  Mikhail A. Markov
 */
package java.rmi.server;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Hashtable;


/**
 * @com.intel.drl.spec_ref
 *
 * Note: this class is not used by RMI runtime.
 *
 * @author  Mikhail A. Markov
 * @deprecated No replacement. This class is deprecated since Java v1.2. 
 */
@Deprecated
public class LogStream extends PrintStream {

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static final int SILENT = 0;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static final int BRIEF = 10;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static final int VERBOSE = 20;

    // The default print stream.
    private static PrintStream defaultStream = System.err;

    // The list of created LogStreams. Their names are keys in the table.
    private static Hashtable logStreams = new Hashtable();

    // The name of this LogStream
    private String name;

    /*
     * True if write method was never called or '\n' (new-line symbol was
     * the last symbol written to the underlying stream.
     */
    private boolean isFirstByte = true;

    /*
     * Constructs LogStream having the given name and writing to the given
     * OutputStream.
     */
    private LogStream(String name, OutputStream out) {
        super(out);
        this.name = name;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public String toString() {
        return "LogStream[" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public void write(byte[] b, int off, int len) {
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("len < 0: " + len); //$NON-NLS-1$
        }

        for (int i = 0; i < len; ++i) {
            write(b[off + i]);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public void write(int b) {
        synchronized (this) {
            if (b == '\n') {
                super.write(b);
                isFirstByte = true;
            } else {
                if (isFirstByte) {
                    isFirstByte = false;
                    print(toString() + ":"); //$NON-NLS-1$
                }
                super.write(b);
            }
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public synchronized void setOutputStream(OutputStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public synchronized OutputStream getOutputStream() {
        return out;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static int parseLevel(String levelStr) {
        if (levelStr == null) {
            return -1;
        }
        levelStr = levelStr.trim().toUpperCase();

        if (levelStr.equals("SILENT")) { //$NON-NLS-1$
            return SILENT;
        } else if (levelStr.equals("BRIEF")) { //$NON-NLS-1$
            return BRIEF;
        } else if (levelStr.equals("VERBOSE")) { //$NON-NLS-1$
            return VERBOSE;
        } else {
            try {
                return Integer.parseInt(levelStr);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static synchronized void setDefaultStream(PrintStream stream) {
        defaultStream = stream;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static synchronized PrintStream getDefaultStream() {
        return defaultStream;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public static LogStream log(String name) {
        synchronized (logStreams) {
            LogStream stream = (LogStream) logStreams.get(name);

            if (stream != null) {
                return stream;
            } else {
                stream = new LogStream(name, defaultStream);
                logStreams.put(name, stream);
                return stream;
            }
        }
    }
}
