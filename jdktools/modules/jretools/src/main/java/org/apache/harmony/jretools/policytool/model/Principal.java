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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jretools.policytool.model;

/**
 * Represents a principal for the grant entries.
 */
public class Principal implements Cloneable {

    /** Type of the principal. */
    private String type;
    /** Name of the principal. */
    private String name;

    /**
     * Returns the type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the principal.
     * @param type type of the principal to be set
     */
    public void setType( final String type ) {
        this.type = type;
    }

    /**
     * Returns the name of the principal.
     * @return the name of the principal
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the principal.
     * @param name name of the principal to be set
     */
    public void setName( final String name ) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "principal " + type + " \"" + name + '"';
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch ( final CloneNotSupportedException cnse ) {
            // This is never going to happen.
            cnse.printStackTrace();
            return null;
        }
    }

}
