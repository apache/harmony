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
 * Created on 29.01.2005
 */
package org.apache.harmony.jpda.tests.framework;

/**
 * This class defines minimal set of methods logging test execution.
 */
public abstract class LogWriter {

    protected String prefix;

    /**
     * Creates instance of the class with given prefix for log messages.
     *   
     * @param prefix - specifies a prefix string
     */
    public LogWriter(String prefix) {
        super();
        setPrefix(prefix);
    }

    /**
     * Returns prefix for messages.
     * 
     * @return prefix for messages
     */
    public synchronized String getPrefix() {
        return prefix;
    }

    /**
     * Sets prefix for messages.
     * 
     * @param prefix to be set
     */
    public synchronized void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Prints message to this log.
     * 
     * @param message message to be printed
     */
    public abstract void printError(String message);

    /**
     * Prints exception info to this log with explaining message.
     * 
     * @param message message to be printed
     * @param throwable exception to be printed
     */
    public abstract void printError(String message, Throwable throwable);

    /**
     * Prints exception info to this log with explaining message.
     * 
     * @param throwable exception to be printed
     */
    public abstract void printError(Throwable throwable);

    /**
     * Prints string to this log w/o line feed.
     * 
     * @param message message to be printed
     */
    public abstract void print(String message);

    /**
     * Prints a string to this log with line feed.
     * 
     * @param message message to be printed
     */
    public abstract void println(String message);
}
