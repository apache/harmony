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

package org.apache.harmony.jretools.policytool.control;

/**
 * Represents that operation with invalid policy text was attempted.
 */
public class InvalidPolicyTextException extends Exception {

    /** Line number where syntax error was found. */
    private int lineNumber;

    /**
     * Creates a new InvalidPolicyTextException.
     * @param message error message
     */
    public InvalidPolicyTextException( final String message ) {
        super( message );
    }

    /**
     * Creates a new InvalidPolicyTextException.
     * @param message error message
     * @param lineNumber line number where syntax error was found
     */
    public InvalidPolicyTextException( final String message, final int lineNumber ) {
        super( message );
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the line number where syntax error was found.
     * @return the line number where syntax error was found
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the line number where syntax error was found.
     * @param lineNumber the line number where syntax error was found
     */
    public void setLineNumber( final int lineNumber ) {
        this.lineNumber = lineNumber;
    }

}
