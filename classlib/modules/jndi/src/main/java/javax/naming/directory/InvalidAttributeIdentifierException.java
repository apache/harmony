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

package javax.naming.directory;

import javax.naming.NamingException;

/**
 * Thrown when the identifier part of an attribute is invalid.
 * <p>
 * Directory service providers may restrict the characteristics of the attribute
 * identifier. If an attempt is made to set the attribute with an invalid
 * attribute the provider will throw an
 * <code>InvalidAttributeIdentifierException</code>.
 * </p>
 */
public class InvalidAttributeIdentifierException extends NamingException {

    private static final long serialVersionUID = 0x829668e5be4a058dL;

    /**
     * Default constructor.
     * <p>
     * All fields are initialized to null.
     * </p>
     */
    public InvalidAttributeIdentifierException() {
        super();
    }

    /**
     * Constructs an <code>InvalidAttributeIdentifierException</code> instance
     * using the supplied text of the message.
     * <p>
     * All fields are initialized to null.
     * </p>
     * 
     * @param s
     *            message about the problem
     */
    public InvalidAttributeIdentifierException(String s) {
        super(s);
    }

}
