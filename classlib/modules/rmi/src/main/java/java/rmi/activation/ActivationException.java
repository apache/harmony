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

package java.rmi.activation;

public class ActivationException extends Exception {
    private static final long serialVersionUID = -4320118837291406071l;

    public Throwable detail;

    public ActivationException(String message, Throwable detail) {
        // pass null for cause to ensure initCause can't be used
        super(message, null);
        this.detail = detail;
    }

    public ActivationException(String message) {
        // pass null for cause to ensure initCause can't be used
        super(message, null);
        this.detail = null;
    }

    public ActivationException() {
        // pass null for cause to ensure initCause can't be used
        super(null, null);
        this.detail = null;
    }

    @Override
    public Throwable getCause() {
        return detail;
    }

    @Override
    public String getMessage() {
        if (detail != null) {
            return super.getMessage() + "[detail throwable = " + detail + "] "; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return super.getMessage();
    }
}
