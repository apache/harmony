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
 * Thrown when attempting to add an attribute that is already defined for an
 * object.
 */
public class AttributeInUseException extends NamingException {

    private static final long serialVersionUID = 0x3d95ea02c92a5c44L;

    /**
     * Default constructor.
     * <p>
     * All fields are initialized to null.
     * </p>
     */
    public AttributeInUseException() {
        super();
    }

    /**
     * Constructs an <code>AttributeInUseException</code> instance using the
     * supplied text of the message.
     * <p>
     * All fields are initialized to null.
     * </p>
     * 
     * @param s
     *            message about the problem
     */
    public AttributeInUseException(String s) {
        super(s);
    }

}
