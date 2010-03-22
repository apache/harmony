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
 * This exception is used if {@link RMIHash} fails to compute
 * RMI hash for some class or method.
 *
 * @author  Vasily Zakharov
 */
public class RMIHashException extends Exception {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7107015868027717508L;

    /**
     * Constructs a new exception with <code>null</code> message.
     */
    public RMIHashException() {
        super();
    }

    /**
     * Constructs a new exception with the specified message.
     *
     * @param   message
     *          Message of this exception.
     */
    public RMIHashException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause and message
     * equal to the specified cause.
     *
     * @param   cause
     *          Cause of this exception.
     */
    public RMIHashException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param   message
     *          Message of this exception.
     *
     * @param   cause
     *          Cause of this exception.
     */
    public RMIHashException(String message, Throwable cause) {
        super(message, cause);
    }
}
