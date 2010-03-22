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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vitaly A. Provodin
 */

/**
 * Created on 02.02.2005
 */
package org.apache.harmony.jpda.tests.framework;

/**
 * Unchecked exception to be thrown in JPDA tests and framework if any error occurs.
 */
public class TestErrorException extends RuntimeException {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = -3946549945049751489L;

    /**
     * Hide the default constructor but make it visible from 
     * derived classes.   
     */
    protected TestErrorException() {
    }
    
    /**
     * Creates new exception instance with explaining message.
     * 
     * @param message cause explaining message
     */
    public TestErrorException(String message) {
        super(message);
    }

    /**
     * Creates new exception instance to enwrap other exception with explaining message.
     * 
     * @param message explaining message
     * @param throwable exception to enwrap
     */
    public TestErrorException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates new exception instance to enwrap other exception w/o explaining message.
     * 
     * @param throwable exception to enwrap
     */
    public TestErrorException(Throwable throwable) {
        super(throwable);
    }
}
