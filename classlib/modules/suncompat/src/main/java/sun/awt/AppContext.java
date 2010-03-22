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

package sun.awt;


/**
 * Stub class, added to make applications like Netbeans which address
 * this class by name compilable on Harmony.
 */
public class AppContext {

    /**
     * Instance to be returned by {@link #getAppContext()}.
     */
    private static final AppContext instance = new AppContext(null);

    /**
     * Stub method, returns the instance of this class;
     */
    public static final AppContext getAppContext() {
        return instance;
    }

    /**
     * Constructor, stub, does nothing.
     * Doesn't throw {@link UnsupportedOperationException}
     * for tests to be able to access the class instance methods.
     */
    AppContext(ThreadGroup group) {
    }

    /**
     * Stub method, throws {@link UnsupportedOperationException}.
     */
    public Object get(Object key) {
        throw new UnsupportedOperationException(
                "sun.awt.AppContext class is a stub and is not implemented");
    }

    /**
     * Stub method, throws {@link UnsupportedOperationException}.
     */
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException(
                "sun.awt.AppContext class is a stub and is not implemented");
    }

    /**
     * Stub method, throws {@link UnsupportedOperationException}.
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException(
                "sun.awt.AppContext class is a stub and is not implemented");
    }
}
