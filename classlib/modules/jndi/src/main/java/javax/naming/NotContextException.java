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

package javax.naming;

/**
 * A <code>NotContextException</code> is the exception thrown by the naming
 * classes when an operation on a context object cannot proceed because the
 * resolved object is not a context type. If the operation requires a particular
 * subclass of a context type, such as an <code>LdapContext</code>, and the
 * resolved object is a context object, but not of the required subclass, then a
 * <code>NotContextException</code> is thrown.
 * <p>
 * Multithreaded access to a <code>NotContextException</code> instance is only
 * safe when client code locks the object first.
 * </p>
 */
public class NotContextException extends NamingException {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = 849752551644540417L;

    /**
     * Constructs a <code>NotContextException</code> instance with all data
     * initialized to null.
     */
    public NotContextException() {
        super();

    }

    /**
     * Constructs a <code>NotContextException</code> instance with a specified
     * error message.
     * 
     * @param msg
     *            The detail message for this exception. It may be null.
     */
    public NotContextException(String msg) {
        super(msg);
    }

}
