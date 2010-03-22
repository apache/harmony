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
 * Created on 31.03.2005
 */
package org.apache.harmony.jpda.tests.framework.jdwp.exceptions;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;


/**
 * This exception is thrown if reply packet with error code is received.
 */
public class ReplyErrorCodeException extends TestErrorException {

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = -5114602978946240494L;

    private int errorCode;

    /**
     * Creates new instance of this exception.
     *
     * @param errorCode error code of received reply packet
     */
    public ReplyErrorCodeException(int errorCode) {
        super("Error " + errorCode + ": " + JDWPConstants.Error.getName(errorCode));
        this.errorCode = errorCode;
    }

    /**
     * Returns error code provided for this exception.
     *
     * @return error code
     */
    public int getErrorCode() {
        return errorCode;
    }

}
