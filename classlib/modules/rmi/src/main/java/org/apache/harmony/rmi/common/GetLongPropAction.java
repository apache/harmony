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

import java.security.PrivilegedAction;


/**
 * Action for obtaining properties holding long values.
 *
 * @author  Mikhail A. Markov
 */
public class GetLongPropAction implements PrivilegedAction {

    // the name of the property to be obtained
    private String propName;

    // default value for the property
    private long defaultVal;

    /**
     * Constructs GetLongPropAction to read property with the given name.
     *
     * @param propName the name of the property to be read
     */
    public GetLongPropAction(String propName) {
        this(propName, 0);
    }

    /**
     * Constructs GetLongPropAction to read property with the given name.
     * and specified default value.
     *
     * @param propName the name of the property to be read
     * @param defaultVal default value for the property
     */
    public GetLongPropAction(String propName, long defaultVal) {
        this.propName = propName;
        this.defaultVal = defaultVal;
    }

    /**
     * Reads the property with the name specified in constructor and returns it
     * as a result; if value read is null, then default value will be returned.
     *
     * @return property read or defaultValue if read property is null
     */
    public Object run() {
        return Long.getLong(propName, defaultVal);
    }
}
