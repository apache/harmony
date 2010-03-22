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
 * @author  Mikhail A. Markov
 */
package java.rmi.server;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public class ServerCloneException extends CloneNotSupportedException {

    private static final long serialVersionUID = 6617456357664815945L;

    /**
     * @com.intel.drl.spec_ref
     */
    public Exception detail;

    /**
     * @com.intel.drl.spec_ref
     */
    public ServerCloneException(String msg, Exception cause) {
        super(msg);
        detail = cause;
        initCause(null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public ServerCloneException(String msg) {
        this(msg, null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Throwable getCause() {
        return detail;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public String getMessage() {
        if (detail == null) {
            return super.getMessage();
        } else {
            // rmi.1E={0} Caused by: {1}
            return Messages.getString("rmi.1E", super.getMessage(), detail.getMessage()); //$NON-NLS-1$
        }
    }
}
