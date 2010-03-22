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
 * @author Aleksey V. Yantsen
 */

/**
 * Created on 12.03.2004
 */
package org.apache.harmony.jpda.tests.framework;

/**
 * This class provides info about breakpoint.
 */
public class Breakpoint {
    public String className;
    public String methodName;
    public long   index;

    /**
     * Creates Breakpoint instance with default values.
     */
    Breakpoint() {
        className = new String();
        methodName = new String();
        index = 0;
    }

    /**
     * Creates Breakpoint instance with given data.
     * 
     * @param clazz Class in which breakpoint is created
     * @param method Method in which breakpoint is created
     * @param location Location within the method
     */
    public Breakpoint(String clazz, String method, int location) {
        className = clazz;
        methodName = method;
        index = location;
    }
}