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
package org.apache.harmony.x.imageio.plugins;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.stream.ImageInputStream;

public class PluginUtils {

    public static final String VENDOR_NAME     = "Apache Harmony"; //$NON-NLS-1$
    public static final String DEFAULT_VERSION = "1.0";           //$NON-NLS-1$

    /**
     * Wrap the specified ImageInputStream object in an InputStream.
     */
    public static InputStream wrapIIS(final ImageInputStream iis) {
        return new IisWrapper(iis);
    }

    private static class IisWrapper extends InputStream {

        private final ImageInputStream input;

        IisWrapper(final ImageInputStream input) {
            this.input = input;
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return input.read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len)
                        throws IOException {
            return input.read(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            return input.skipBytes(n);
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(final int readlimit) {
            input.mark();
        }

        @Override
        public void reset() throws IOException {
            input.reset();
        }

        @Override
        public void close() throws IOException {
            input.close();
        }
    }
}
