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
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.common;


/**
 * General exception for Java Compiler classes.
 *
 * @author  Vasily Zakharov
 */
public class JavaCompilerException extends Exception {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3632651458880391763L;

    /**
     * Creates this exception with <code>null</code> message and cause.
     */
    public JavaCompilerException() {
        super();
    }

    /**
     * Creates this exception with the specified message
     * and <code>null</code> cause.
     *
     * @param   message
     *          Detail message.
     */
    public JavaCompilerException(String message) {
        super(message);
    }

    /**
     * Creates this exception with the specified message and cause.
     *
     * @param   message
     *          Detail message.
     *
     * @param   cause
     *          Cause.
     */
    public JavaCompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates this exception with the specified cause
     * and message equal to the cause's message.
     *
     * @param   cause
     *          Cause.
     */
    public JavaCompilerException(Throwable cause) {
        super(cause);
    }
}
