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
 * Action for obtaining boolean value from properties.
 *
 * @author  Mikhail A. Markov
 */
public class GetBooleanPropAction implements PrivilegedAction {

    // the name of the property to be obtained
    private String propName;

    // default value for the property
    private Boolean defaultVal;

    /**
     * Constructs GetBooleanPropAction to read property with the given name.
     *
     * @param propName the name of the property to be read
     */
    public GetBooleanPropAction(String propName) {
        this.propName = propName;
        defaultVal = new Boolean(false);
    }

    /**
     * Constructs GetBooleanPropAction to read property with the given name
     * and the specified default value.
     *
     * @param propName the name of the property to be read
     * @param defaultVal default value for the property
     */
    public GetBooleanPropAction(String propName, boolean defaultVal) {
        this.propName = propName;
        this.defaultVal = new Boolean(defaultVal);
    }

    /**
     * Obtains boolean value from the property with the name specified in
     * constructor and returns it as a result (if this property does not
     * exist - the default value will be returned).
     *
     * @return value read or the default value if property to be read is not set
     */
    public Object run() {
        String propVal = System.getProperty(propName);
        return (propVal == null) ? defaultVal : Boolean.valueOf(propVal);
    }
}
