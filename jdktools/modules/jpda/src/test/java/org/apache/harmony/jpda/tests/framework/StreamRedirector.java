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

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>This class provides redirection of debuggee output and error streams to logWriter.
 */
public class StreamRedirector extends Thread {

    String name;
    LogWriter logWriter;
    BufferedReader br;
    boolean doExit;

    /**
     * Creates new redirector for the specified stream.
     * 
     * @param is stream to be redirected
     * @param logWriter logWriter to redirect stream to
     * @param name stream name used as prefix for redirected output
     */
    public StreamRedirector(InputStream is, LogWriter logWriter, String name) {
        super("Redirector for " + name);
        this.name = name;
        this.logWriter = logWriter;
        InputStreamReader isr = new InputStreamReader(is);
        br = new BufferedReader(isr, 1024);
        doExit = false;
    }

    /**
     * Reads all lines from stream and puts them to logWriter.
     */
    public void run() {
        logWriter.println("Redirector started: " + name);
        try {
            String line = "";
            while (!doExit) {
                try {
                    line = br.readLine();
                    if (line == null)
                        break;
                    
                    logWriter.println(name + "> " + line);
                } catch (IllegalStateException e) {
                     //logWriter.printError("Illegal state exception! " + e);
                    //ignore
                }
                
            }
            logWriter.println("Redirector completed: " + name);
        } catch (IOException e) {
            logWriter.printError(e);
        }
    }

    /**
     * Notifies redirector to stop stream redirection.
     */
    public void exit() {
        doExit = true;
    }
}
