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
 * @author Alexey A. Ivanov, Anton Avtamonov
 */
package javax.swing;

import java.awt.EventQueue;

/**
 * A wrapper for Swing package unit tests. It wraps the real test code so that
 * it is executed in the event dispatch thread.
 *
 */
public abstract class SwingTestCase extends BasicSwingTestCase {
    public SwingTestCase() {
    }

    public SwingTestCase(final String name) {
        super(name);
    }

    @Override
    protected Throwable runBareImpl() throws Throwable {
        final Throwable[] exception = new Throwable[1];
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    runBareSuper();
                } catch (Throwable e) {
                    exception[0] = e;
                }
            }
        });
        
        return exception[0]; 
    }
}
