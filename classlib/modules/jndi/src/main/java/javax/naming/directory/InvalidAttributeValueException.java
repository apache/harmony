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
 * Thrown when the value part of an attribute is invalid.
 * <p>
 * Directory service providers may restrict the characteristics of the attribute
 * value. If an attempt is made to set the attribute with an invalid attribute
 * value the provider will throw an <code>InvalidAttributeValueException</code>.
 * </p>
 * <p>
 * Examples include attempting to set a value on an attribute that doesn't take
 * a value, attempting to set multiple values on an attribute that only takes a
 * single value, attempting to clear a value on an attribute that must have a
 * value, and so on.
 * </p>
 * <p>
 * The serialization and synchronization specification for
 * <code>NamingException</code> applies equally to this class.
 * </p>
 * 
 * @see NamingException
 */
public class InvalidAttributeValueException extends NamingException {

    private static final long serialVersionUID = 0x7903d78afec63b03L;

    /**
     * Default constructor.
     * <p>
     * All fields are initialized to null.
     * </p>
     */
    public InvalidAttributeValueException() {
        super();
    }

    /**
     * Constructs an <code>InvalidAttributeValueException</code> instance
     * using the supplied text of the message.
     * <p>
     * All fields are initialized to null.
     * </p>
     * 
     * @param s
     *            message about the problem
     */
    public InvalidAttributeValueException(String s) {
        super(s);
    }

}
