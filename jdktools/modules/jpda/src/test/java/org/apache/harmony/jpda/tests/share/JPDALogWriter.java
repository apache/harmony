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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 28.03.2005
 */
package org.apache.harmony.jpda.tests.share;

import java.io.PrintStream;

import org.apache.harmony.jpda.tests.framework.LogWriter;

/**
 * This class provides logging messages to underlying output stream. There are
 * can be several JPDALogWriter objects writing to the same underlying stream
 * with different prefixes.
 */
public class JPDALogWriter extends LogWriter {

    protected String printPrefix;

    protected String errorMessage;

    protected LogStream logStream;

    public boolean enablePrint = true;

    /**
     * Constructs an instance of the class for given output stream.
     * 
     * @param outputStream
     *            stream for output
     * @param prefix
     *            prefix for messages or null
     * @param enablePrint
     *            flag for enabling print to log
     */
    public JPDALogWriter(PrintStream outputStream, String prefix,
            boolean enablePrint) {
        super(prefix);
        this.enablePrint = enablePrint;
        logStream = new LogStream(outputStream);
    }

    /**
     * Constructs an instance of the class for given output stream.
     * 
     * @param outputStream
     *            stream for output
     * @param prefix
     *            prefix for messages or null
     */
    public JPDALogWriter(PrintStream outputStream, String prefix) {
        this(outputStream, prefix, true);
    }

    /**
     * Constructs an instance of the class to log to the same output stream.
     * 
     * @param logWriter
     *            log writer containing stream for output
     * @param prefix
     *            prefix for messages or null
     */
    public JPDALogWriter(JPDALogWriter logWriter, String prefix) {
        super(prefix);
        logStream = logWriter.getLogStream();
    }

    /**
     * Sets prefix for messages.
     * 
     * @param prefix
     *            to be set
     */
    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
        if (prefix == null || prefix.length() <= 0) {
            printPrefix = "";
        } else {
            printPrefix = prefix + "> ";
        }
    }

    /**
     * Prints error message.
     * 
     * @param message
     *            error message to be printed
     */
    public void printError(String message) {
        if (null == errorMessage) {
            errorMessage = message;
        }
        logStream.println(getErrorPrefix() + message);
    }

    /**
     * Prints exception information with explaining message.
     * 
     * @param message
     *            explaining message to be printed
     * @param throwable
     *            exception to be printed
     */
    public void printError(String message, Throwable throwable) {
        if (null == errorMessage) {
            errorMessage = message;
        }
        logStream.printStackTrace(getErrorPrefix() + message, throwable);
    }

    /**
     * Prints exception information w/o explaining message.
     * 
     * @param throwable
     *            exception to be printed
     */
    public void printError(Throwable throwable) {
        logStream.printStackTrace(null, throwable);
    }

    /**
     * Prints message to the output stream w/o line feed.
     * 
     * @param message
     *            to be printed
     */
    public void print(String message) {
        if (enablePrint) {
            logStream.print(printPrefix + message);
        }
    }

    /**
     * Prints message to the output stream with line feed.
     * 
     * @param message
     *            to be printed
     */
    public void println(String message) {
        if (enablePrint) {
            logStream.println(printPrefix + message);
        }
    }

    /**
     * Returns saved error message.
     * 
     * @return message string
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    // /////////////////////////////////////////////////////////////////////////////

    /**
     * Get prefix for error messages.
     */
    protected String getErrorPrefix() {
        return "# ERROR: " + printPrefix;
    }

    /**
     * Get underlying LogStream object.
     */
    protected LogStream getLogStream() {
        return logStream;
    }

    /**
     * Underlying stream with synchronous access.
     */
    protected static class LogStream {
        protected PrintStream outputStream;

        /**
         * A constructor.
         * 
         * @param outputStream
         */
        public LogStream(PrintStream outputStream) {
            setOutputStream(outputStream);
        }

        /**
         * @return The associated output stream.
         */
        public synchronized PrintStream getOutputStream() {
            return outputStream;
        }

        /**
         * Sets new output stream.
         * 
         * @param outputStream
         *            new output stream
         */
        public synchronized void setOutputStream(PrintStream outputStream) {
            this.outputStream = outputStream;
        }

        /**
         * Prints the given message with the newline to the output stream.
         * 
         * @param message
         *            log message
         */
        public synchronized void println(String message) {
            outputStream.println(message);
        }

        /**
         * Prints the given message to the output stream.
         * 
         * @param message
         *            log message
         */
        public synchronized void print(String message) {
            outputStream.print(message);
            outputStream.flush();
        }

        /**
         * Prints the given message and the call stack of the exception to the
         * output stream.
         * 
         * @param message
         *            log message
         * @param throwable
         *            exception
         */
        public synchronized void printStackTrace(String message,
                Throwable throwable) {
            if (message != null) {
                println(message);
            }
            throwable.printStackTrace(outputStream);
        }
    }
}
