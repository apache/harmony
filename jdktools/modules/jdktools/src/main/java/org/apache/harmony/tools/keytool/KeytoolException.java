/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.keytool;

/**
 * Class representing the exceptions specific for keytool.
 */
public class KeytoolException extends Exception {

    /**
     * serial version UID.
     */
    private static final long serialVersionUID = -2930629442696213997L;

    /**
     * Default constructor.
     */
    public KeytoolException() {
        super();
    }

    /**
     * @param msg - exception message to print
     */
    public KeytoolException(String msg) {
        super(msg);
    }

    /**
     * @param msg - exception message to print
     * @param cause - throwable that caused this exception to be thrown
     */
    public KeytoolException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * @param cause - throwable that caused this exception to be thrown
     */
    public KeytoolException(Throwable cause) {
        super(cause);
    }
}
