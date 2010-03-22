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
package org.apache.harmony.rmi.common;

import java.io.OutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;


/**
 * Class containing all RMI logging functionality.
 *
 * @author  Mikhail A. Markov
 */
public class RMILog implements RMIProperties {

    /**
     * RMI logging level corresponding to Level.OFF value.
     */
    public static final Level SILENT = Level.OFF;

    /**
     * RMI logging level corresponding to Level.FINE value.
     */
    public static final Level BRIEF = Level.FINE;

    /**
     * RMI logging level corresponding to Level.FINER value.
     */
    public static final Level VERBOSE = Level.FINER;

    // handler for copying rmi logging messages to System.err
    private static Handler consoleHandler =
        (Handler) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                Handler h = new RMIStreamHandler(System.err);
                h.setLevel(Level.ALL);
                return h;
            }
        });

    // Logger wrapped in this RMI log.
    private Logger logger;

    // Handler set by setOutputStream() method.
    private RMIStreamHandler rmiLogHandler;

    /**
     * Helper method.
     * Returns RMILog for logging remote calls on server side.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging remote calls on server side
     */
    public static RMILog getServerCallsLog() {
        return getLog("harmony.rmi.server.call", //$NON-NLS-1$
                getBoolean(LOGSERVER_PROP) ? VERBOSE : SILENT);
    }

    /**
     * Helper method.
     * Returns RMILog for logging remote calls on client side.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging remote calls on client side
     */
    public static RMILog getClientCallsLog() {
        return getLog("harmony.rmi.client.call", //$NON-NLS-1$
                getBoolean(LOGCLIENT_PROP) ? VERBOSE : SILENT);
    }

    /**
     * Helper method.
     * Returns RMILog for logging remote reference activity on server side.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging remote reference activity on server side
     */
    public static RMILog getServerRefLog() {
        return getLog("harmony.rmi.server.ref", getString(SERVERLOGLEVEL_PROP)); //$NON-NLS-1$
    }

    /**
     * Helper method.
     * Returns RMILog for logging remote reference activity on client side.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging remote reference activity on client side
     */
    public static RMILog getClientRefLog() {
        return getLog("harmony.rmi.client.ref", getString(CLIENTLOGLEVEL_PROP)); //$NON-NLS-1$
    }

    /**
     * Helper method.
     * Returns RMILog for logging DGC activity.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging DGC activity
     */
    public static RMILog getDGCLog() {
        return getLog("harmony.rmi.dgc", getString(DGCLOGLEVEL_PROP)); //$NON-NLS-1$
    }

    /**
     * Helper method.
     * Returns RMILog for logging activity of default RMIClassLoader provider.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging activity of default RMIClassLoader provider
     */
    public static RMILog getLoaderLog() {
        return getLog("harmony.rmi.loader", getString(LOADERLOGLEVEL_PROP)); //$NON-NLS-1$
    }

    /**
     * Helper method.
     * Returns RMILog for logging transport-layer activity.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging transport-layer activity
     */
    public static RMILog getTransportLog() {
        return getLog("harmony.rmi.transport.misc", //$NON-NLS-1$
                getString(TRANSPORTLOGLEVEL_PROP));
    }

    /**
     * Helper method.
     * Returns RMILog for logging TCP binding/connection activity.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging TCP binding/connection activity
     */
    public static RMILog getTcpTransportLog() {
        return getLog("harmony.rmi.transport.tcp", //$NON-NLS-1$
                getString(TRANSPORTTCPLOGLEVEL_PROP));
    }

    /**
     * Helper method.
     * Returns RMILog for logging HTTP connections activity.
     * If such a log does not exist, creates it.
     *
     * @return RMILog for logging HTTP connections activity
     */
    public static RMILog getProxyTransportLog() {
        return getLog("harmony.rmi.transport.proxy", //$NON-NLS-1$
                getString(TRANSPORTPROXYLOGLEVEL_PROP));
    }

    /**
     * Helper method. Returns RMILog for logging Activation/ActivationGroup/Rmid
     * events. If such a log does not exist, creates it.
     *
     * @return RMILog for logging remote calls on server side
     */
    public static RMILog getActivationLog() {
        return getLog("harmony.rmi.activation", getString(ACTIVATIONLOGLEVEL_PROP)); //$NON-NLS-1$
    }

    /**
     * Creates RMILog. Underlying logger will have the name 'loggerName'. The
     * level for created RMILog will be equal to 'logLevel' value.
     *
     * @param loggerName
     *        the name of the logger to be obtained
     *
     * @param logLevel
     *        the level for RMILog: it should be one of RMI logging levels
     *        (SILENT, BRIEF, VERBOSE or one of levels from
     *        java.util.logging.Level class
     */
    public static RMILog getLog(String loggerName, String logLevel) {
        return getLog(loggerName, parseLevelString(logLevel));
    }

    /**
     * Creates RMILog. Underlying logger will have the name 'loggerName'. The
     * level for created RMILog will be equal to 'logLevel' value.
     *
     * @param loggerName the name of the logger to be obtained
     * @param logLevel the level for RMILog
     */
    public static RMILog getLog(String loggerName, final Level logLevel) {
        final Logger logger = Logger.getLogger(loggerName);

        // add handler for publishing records to System.err
        RMILog log = (RMILog) AccessController.doPrivileged(
                new PrivilegedAction() {
                    public Object run() {

                        if (logger.getLevel() == null
                                || !logger.isLoggable(logLevel)) {
                            logger.setLevel(logLevel);
                        }

                        // remove System.err stream handler to avoid
                        // duplications
                        logger.removeHandler(consoleHandler);

                        // add System.err stream handler again
                        logger.addHandler(consoleHandler);
                        return new RMILog(logger);
                    }
                });
        return log;
    }

    /**
     * Parses the given string and returns the corresponding Level object.
     * Possible values for the incoming string are one of RMI logging
     * levels (SILENT, BRIEF, VERBOSE or one of levels from
     * java.util.logging.Level class. If the given string is null or it could
     * not be parsed then Level.OFF value will be returned.
     *
     * @param levelStr String to be parsed
     *
     * @return parsed Level or Level.OFF if the given string is null or an
     *         error occurred while it's parsing
     */
    public static Level parseLevelString(String levelStr) {
        if (levelStr == null) {
            return Level.OFF;
        }
        levelStr = levelStr.trim().toUpperCase();

        if (levelStr.equals("SILENT")) { //$NON-NLS-1$
            return SILENT;
        } else if (levelStr.equals("BRIEF")) { //$NON-NLS-1$
            return BRIEF;
        } else if (levelStr.equals("VERBOSE")) { //$NON-NLS-1$
            return VERBOSE;
        }
        Level logLevel = Level.OFF;

        try {
            logLevel = Level.parse(levelStr);
        } catch (IllegalArgumentException iae) {
        }
        return logLevel;
    }

    /*
     * Constructs RMILog containing specified Logger.
     *
     * @param logger Logger for RMILog
     */
    private RMILog(Logger logger) {
        this.logger = logger;
    }

    /**
     * Checks if underlying logger would log a message with the specified
     * level.
     *
     * @param l Logging level to be checked
     *
     * @return true if underlying logger would log a message with
     *         the specified level and false otherwise
     */
    public boolean isLoggable(Level l) {
        return logger.isLoggable(l);
    }

    /**
     * Logs specified message prepended by the current Thread's name
     * with the given level to the underlying logger.
     *
     * @param l logging level of the message
     * @param msg message to be logged
     */
    public void log(Level l, String msg) {
        if (isLoggable(l)) {
            String[] logSrc = getLogSource();
            logger.logp(l, logSrc[0], logSrc[1],
                    Thread.currentThread().getName() + ": " + msg); //$NON-NLS-1$
        }
    }

    /**
     * Logs specified message prepended by the current Thread's name
     * and Throwable object with the given level to the underlying logger.
     *
     * @param l logging level of the message and Throwable
     * @param msg message to be logged
     * @param t Throwable to be logged
     */
    public void log(Level l, String msg, Throwable t) {
        if (isLoggable(l)) {
            String[] logSrc = getLogSource();
            logger.logp(l, logSrc[0], logSrc[1],
                    Thread.currentThread().getName() + ": " + msg, t); //$NON-NLS-1$
        }
    }

    /**
     * Adds additional handler to the underlying logger from the given
     * OutputStream. If this method with non-null parameter was already called
     * and thus additional handler already exists, this handler will be replaced
     * by newly created handler.
     * This method is intended to be used by RemoteServer.setLog() method.
     *
     * @param out OutputStream for additional handler. If it's null then
     *        messages will not be logged to any additional handlers.
     *
     * @see RemoteServer.setLog(OutputStream)
     */
    public synchronized void setOutputStream(OutputStream out) {
        if (rmiLogHandler != null) {
            logger.removeHandler(rmiLogHandler);
        }

        if (out == null) {
            rmiLogHandler = null;
            return;
        }

        if (!logger.isLoggable(VERBOSE)) {
            logger.setLevel(VERBOSE);
        }

        rmiLogHandler = new RMIStreamHandler(out);
        rmiLogHandler.setLevel(VERBOSE);
        logger.addHandler(rmiLogHandler);
    }

    /**
     * Returns PrintStream where RMI logs messages.
     * This method is intended to be used by RemoteServer.getLog() method
     *
     * @return PrintStream where RMI logs messages (possibly null)
     *
     * @see RemoteServer.getLog()
     */
    public synchronized PrintStream getPrintStream() {
        return (rmiLogHandler == null) ? null : rmiLogHandler.ps;
    }

    // Reads boolean value from the given property name.
    private static boolean getBoolean(String propName) {
        return ((Boolean) AccessController.doPrivileged(
                new GetBooleanPropAction(propName))).booleanValue();
    }

    // Reads string value from the given property name.
    private static String getString(String propName) {
        return (String) AccessController.doPrivileged(
                new GetStringPropAction(propName));
    }

    /*
     * Returns string containing from 2 elements: the name of the class
     * and the name of the method from which log() method was called.
     * It's needed for logging the name of the method from which log() method
     * was called.
     */
    private String[] getLogSource() {
        StackTraceElement[] curST = (new Exception()).getStackTrace();

        // this method is called from appropriate log() method, so required
        // source will be at 3-rd cell
        return new String[] {
                curST[2].getClassName(), curST[2].getMethodName() };
    }


    /*
     * Handler similar to ConsoleHandler but working with arbitrary
     * OutputStreams.
     */
    private static class RMIStreamHandler extends StreamHandler {

        // PrintStream build from OutputStream provided to constructor
        PrintStream ps;

        /*
         * Constructs RMIStreamHandler from the given OutputStream.
         *
         * @param out underlying OutputStream for this handler
         */
        RMIStreamHandler(OutputStream out) {
            super(out, new SimpleFormatter());
            ps = new PrintStream(out);
        }

        /**
         * Publish specified LogRecord.
         *
         * @param rec LogRecord to be published
         */
        public void publish(LogRecord rec) {
            super.publish(rec);
            flush();
        }

        /**
         * Flushes the underlying OutputStream.
         */
        public void close() {
            flush();
        }
    }
}
