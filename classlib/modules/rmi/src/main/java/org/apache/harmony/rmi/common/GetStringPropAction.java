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

package org.apache.harmony.rmi.common;

import java.security.PrivilegedAction;

/**
 * Action for obtaining properties holding string values.
 */
public class GetStringPropAction implements PrivilegedAction<String> {
    // the name of the property to be obtained
    private final String propName;

    // default value for the property
    private final String defaultVal;

    /**
     * Constructs GetStringPropAction to read property with the given name.
     *
     * @param propName the name of the property to be read
     */
    public GetStringPropAction(String propName) {
        this(propName, null);
    }

    /**
     * Constructs GetStringPropAction to read property with the given name
     * and the specified default value.
     *
     * @param propName the name of the property to be read
     * @param defaultVal default value for the property
     */
    public GetStringPropAction(String propName, String defaultVal) {
        this.propName = propName;
        this.defaultVal = defaultVal;
    }

    /**
     * Reads the property with the name specified in constructor and returns it
     * as a result; if value read is null, then default value (possibly
     * null) will be returned.
     *
     * @return property read or defaultValue if property read is null
     */
    public String run() {
        return System.getProperty(propName, defaultVal);
    }
}
