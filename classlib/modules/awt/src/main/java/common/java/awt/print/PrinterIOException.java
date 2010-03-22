/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Igor A. Pyankov
 */
package java.awt.print;

import java.io.IOException;

public class PrinterIOException extends PrinterException {

    private static final long serialVersionUID = 5850870712125932846L;

    private IOException mException;

    public PrinterIOException(IOException exception) {
        initCause(null);
        mException = exception;
    }

    @Override
    public Throwable getCause() {
        return mException;
    }

    public IOException getIOException() {
        return mException;
    }
}
