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
/** 
 * @author Pavel Dolgov
 */
package org.apache.harmony.applet;

import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import org.apache.harmony.applet.internal.nls.Messages;


/**
 * Thread that initializes applet and performs commands from the host application
 */
final class AppletThread extends Thread {
    private final Object monitor = new Object();
    private final List<Command> commandQueue = Collections.synchronizedList(new LinkedList<Command>());
    
    private boolean doExit;
    
    AppletThread(Proxy proxy) {
        super(proxy.docSlice.codeBase.threadGroup, "Applet-" + proxy.params.className);
        setContextClassLoader(proxy.docSlice.codeBase.classLoader);
    }
    
    @Override
    public void run() {

        while (true) {
            
            while( !commandQueue.isEmpty()) {
                Command command = commandQueue.remove(0);
                command.run();
            }
            
            synchronized(monitor) {
                if (doExit) {
                    return;
                }
                try {
                    monitor.wait();
                    if (doExit) {
                        return;
                    }
                } catch (InterruptedException e) {
                    // set the interrupt state
                    interrupt();
                    // the thread was interrupted, so we end it
                    return;
                }
            }
        }
    }

    void postCommand(Command command) {
        commandQueue.add(command);

        if (Thread.currentThread() != this) {
            synchronized(monitor) {
                monitor.notifyAll();
            }
        }
    }
    
    void exit() {
        if (Thread.currentThread() != this) {
            throw new InternalError(Messages.getString("applet.01"));
        }
        synchronized(monitor) {
            doExit = true;
            monitor.notifyAll();
        }
    }
    
}
