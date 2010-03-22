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
import java.util.Arrays;

import javax.imageio.stream.ImageInputStream;

import org.apache.harmony.x.imageio.internal.nls.Messages;

public enum ImageSignature {
        JPEG(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
        BMP(new byte[] { 'B', 'M' }),
        GIF87a(new byte[] { 'G', 'I', 'F', '8', '7', 'a' }),
        GIF89a(new byte[] { 'G', 'I', 'F', '8', '9', 'a' }),
        PNG(new byte[] { (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                        (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A });

    private final byte[] sig;

    ImageSignature(final byte[] sig) {
        this.sig = sig;
    }

    public static byte[] readSignature(final Object source, final int len)
                    throws IOException {
        if (source == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.2", //$NON-NLS-1$
                "source")); //$NON-NLS-1$
        }

        if (!(source instanceof ImageInputStream)) {
            return null;
        }

        final ImageInputStream iis = (ImageInputStream) source;
        final byte[] sig = new byte[len];

        iis.mark();
        iis.readFully(sig);
        iis.reset();

        return sig;
    }

    public byte[] getBytes() {
        return sig.clone();
    }

    public boolean verify(final byte[] sig) {
        return Arrays.equals(this.sig, sig);
    }

    public boolean verify(final Object source) throws IOException {
        return verify(readSignature(source, sig.length));
    }
}
