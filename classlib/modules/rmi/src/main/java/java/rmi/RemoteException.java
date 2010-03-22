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

package java.rmi;

import java.io.IOException;

import org.apache.harmony.rmi.internal.nls.Messages;

public class RemoteException extends IOException {
    private static final long serialVersionUID = -5148567311918794206L;

    public Throwable detail;

    public RemoteException(String msg, Throwable cause) {
        super(msg);
        detail = cause;
        // prevent subsequent initCause calls
        initCause(null);
    }

    public RemoteException(String msg) {
        this(msg, null);
    }

    public RemoteException() {
        this(null, null);
    }

    @Override
    public Throwable getCause() {
        return detail;
    }

    @Override
    public String getMessage() {
        if (detail == null) {
            return super.getMessage();
        }
        return Messages.getString("rmi.08", super.getMessage(),detail); //$NON-NLS-1$
    }
}
