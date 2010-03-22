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

import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public class ProgressMonitorInputStreamTest extends BasicSwingTestCase {
    private JFrame window;

    private ProgressMonitorInputStream in;

    private ByteArrayInputStream realIn;

    private byte[] bytes;

    private static class ErrorStream extends FilterInputStream {
        protected ErrorStream(final InputStream in) {
            super(in);
        }

        @Override
        public int available() throws IOException {
            throw new IOException();
        }
    }

    @Override
    public void setUp() {
        window = new JFrame();
        int l = 300;
        bytes = new byte[l];
        for (int i = 0; i < l; i++) {
            bytes[i] = (byte) i;
        }
        realIn = new ByteArrayInputStream(bytes);
    }

    @Override
    public void tearDown() {
        in = null;
        realIn = null;
        bytes = null;
        window.dispose();
    }

    public void testProgressMonitorInputStream() throws Exception {
        in = new ProgressMonitorInputStream(window, "Here we go...", realIn);
        assertNotNull(in.getProgressMonitor());
        in.read();
        Thread.sleep(600);
        in.skip(30);
        assertEquals(1, window.getOwnedWindows().length);
        in.close();
    }

    public void testMaximum() throws Exception {
        in = new ProgressMonitorInputStream(window, "Here we go...", new ErrorStream(realIn));
        assertEquals(0, in.getProgressMonitor().getMaximum());
        in.read();
        Thread.sleep(600);
        in.skip(30);
        Thread.sleep(600);
        assertEquals(0, window.getOwnedWindows().length);
        in.close();
    }

    public void testReset() throws Exception {
        in = new ProgressMonitorInputStream(window, "Here we go...", realIn);
        ProgressMonitor pm = in.getProgressMonitor();
        in.read();
        Thread.sleep(600);
        in.skip(30);
        in.reset();
        assertSame(pm, in.getProgressMonitor());
        in.close();
    }

    public void testInterrupted() throws Exception {
        in = new ProgressMonitorInputStream(window, "Here we go...", realIn);
        in.read();
        Thread.sleep(600);
        in.skip(30);
        JDialog dialog = (JDialog) window.getOwnedWindows()[0];
        dialog.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        in.reset();
        in.skip(30);
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                in.read();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class expectedExceptionClass() {
                return InterruptedIOException.class;
            }
        });
        Thread.sleep(600);
        in.close();
    }
}
