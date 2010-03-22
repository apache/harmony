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
 * Created on 16.03.2005
 */
package org.apache.harmony.jpda.tests.framework.jdwp.exceptions;

import java.io.IOException;

/**
 * This exception is thrown if reading packet form JDWP connection is timed out.
 */
public class TimeoutException extends IOException {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = -6288034406488650881L;

    private boolean wasConnectionClosed = false;
    
    /**
     * Create new exception instance.
     * @param connectionClosed is true if connection was normally closed
     *        before timeout exceeded 
     */
    public TimeoutException(boolean connectionClosed) {
        super(connectionClosed ? "Connection was closed" : "Timeout was exceeded");
        this.wasConnectionClosed = connectionClosed;
    }

    /**
     * Returns true if connection was normally closed before timeout exceeded.
     * @return true or false
     */
    public boolean isConnectionClosed() {
        return wasConnectionClosed;
    }
}
