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
 * Created on 26.01.2005
 */
package org.apache.harmony.jpda.tests.framework;

/**
 * This class represents debuggee VM on debugger side.
 * <p>
 * This abstract class defines a set of commands to control debuggee VM.
 * To provide the safely start up and shoot down of debuggee, an instance of
 * <code>DebuggeeWrapper</code> should be registered in <code>DebuggeeRegister</code>.
 * 
 * @see DebuggeeRegister
 */
public abstract class DebuggeeWrapper {

    protected TestOptions settings;
    protected LogWriter logWriter;

    /**
     * Creates an instance of <code>DebuggeeWrapper</code>. 
     * 
     * @param settings specifies parameters for debuggee start
     * @param logWriter provides unified facilities for logging 
     */
    public DebuggeeWrapper(TestOptions settings, LogWriter logWriter) {
        super();
        this.settings = settings;
        this.logWriter = logWriter;
    }

    /**
     * An implementation of this method must initiate the debuggee to start.
     */
    public abstract void start();

    /**
     * An implementation of this method must cause the debuggee to stop.
     */
    public abstract void stop();

    /**
     * An implementation of this method must cause the debuggee to exit
     * with specified <code>exitStatus</code> .
     * 
     * @param exitStatus
     */
    public abstract void exit(int exitStatus);

    /**
     * An implementation of this method must cause the debuggee to resume.
     */
    public abstract void resume();

    /**
     * An implementation of this method must cause the debuggee to dispose.
     */
    public abstract void dispose();

    /**
     * Return associated logWriter object. 
     * 
     * @return associated logWriter
     */
    public LogWriter getLogWriter() {
        return logWriter;
    }
}
