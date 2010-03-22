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
 * Thrown when attempting to make a modification that contravenes the directory
 * schema.
 * <p>
 * 
 * For example, this exception is thrown if an attempt is made to modify the set
 * of attributes that is defined on an entry to a state that is invalid by the
 * object attributes schema. Another example is if the naming schema is
 * contravened by attempting to move the entry to a new part of the directory.
 * <p>
 * 
 * The directory service provider throws these exceptions.
 * <p>
 * 
 * The specification for serialization and thread-safety of
 * <code>NamingException</code> applies equally to this class.
 * <p>
 */
public class SchemaViolationException extends NamingException {

    private static final long serialVersionUID = 0xd5c97d2fb107bec1L;

    /**
     * This is the default constructor. All fields are initialized to null.
     */
    public SchemaViolationException() {
        super();
    }

    /**
     * Construct a <code>SchemaViolationException</code> with given message.
     * 
     * @param s
     *            a message about exception detail
     */
    public SchemaViolationException(String s) {
        super(s);
    }

}
