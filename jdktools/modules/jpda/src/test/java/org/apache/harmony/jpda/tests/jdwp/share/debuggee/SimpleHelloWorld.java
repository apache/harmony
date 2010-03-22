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
 * Created on 31.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.share.debuggee;

import org.apache.harmony.jpda.tests.share.Debuggee;

/**
 * This class provides simple HelloWorld debuggee class.
 */
public class SimpleHelloWorld extends Debuggee {

    public void run() {
        logWriter.println("Hello World");
    }

    public static void main(String [] args) {
        runDebuggee(SimpleHelloWorld.class);
    }
}
