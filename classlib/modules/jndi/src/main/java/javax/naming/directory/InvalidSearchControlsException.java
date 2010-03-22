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
 * Thrown when the <code>SearchControls</code> for a given search are invalid.
 * <p>
 * For example, the search controls would be invalid if the scope is not one of
 * the defined class constants.
 * </p>
 */
public class InvalidSearchControlsException extends NamingException {

    private static final long serialVersionUID = 0xb8e38210910fe94fL;

    /**
     * Default constructor.
     * <p>
     * All fields are initialized to null.
     * </p>
     */
    public InvalidSearchControlsException() {
        super();
    }

    /**
     * Constructs an <code>InvalidSearchControlsException</code> instance
     * using the supplied text of the message.
     * <p>
     * All fields are initialized to null.
     * </p>
     * 
     * @param s
     *            message about the problem
     */
    public InvalidSearchControlsException(String s) {
        super(s);
    }

}
