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
 * @author Dennis Ushakov
 */
package javax.swing;

import java.awt.Component;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public class ProgressMonitorInputStream extends FilterInputStream {
    private ProgressMonitor progressMonitor;
    private int progress;
    private int max;

    public ProgressMonitorInputStream(final Component parentComponent, final Object message,
                                      final InputStream in)  {
        super(in);
        max = getAvailable();
        progressMonitor = new ProgressMonitor(parentComponent, message, null, 0, max);
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    public int read() throws IOException {
        checkCancel();
        int result = super.read();
        updateMonitor(1);
        return result;
    }

    public int read(final byte[] b) throws IOException {
        checkCancel();
        int result = super.read(b);
        updateMonitor(result);
        return result;
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        checkCancel();
        int result = super.read(b, off, len);
        updateMonitor(result);
        return result;
    }

    public long skip(final long n) throws IOException {
        long result = super.skip(n);
        updateMonitor(result);
        return result;
    }

    public void close() throws IOException {
        progressMonitor.close();
        super.close();
    }

    public void reset() throws IOException {
        progressMonitor.setProgress(0);
        super.reset();
    }

    private void checkCancel() throws InterruptedIOException {
        if (progressMonitor.isCanceled()) {
            throw new InterruptedIOException();
        }
    }

    private void updateMonitor(final long bytesRead) throws IOException {
        progress += bytesRead;
        max = progress + getAvailable();
        progressMonitor.setMaximum(max);
        progressMonitor.setProgress(progress);
    }

    private int getAvailable(){
        try {
            return available();
        } catch (IOException e) {
            return 0;
        }
    }
}
