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

package javax.print;

import java.io.IOException;
import java.io.OutputStream;

public abstract class StreamPrintService implements PrintService {
    private final OutputStream outputStream;

    private boolean disposed;

    protected StreamPrintService(OutputStream out) {
        super();
        outputStream = out;
        disposed = false;
    }

    public abstract String getOutputFormat();

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void dispose() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
