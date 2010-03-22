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
package org.apache.harmony.x.imageio.plugins.gif;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

import org.apache.harmony.x.imageio.plugins.ImageSignature;
import org.apache.harmony.x.imageio.plugins.ImageType;
import org.apache.harmony.x.imageio.plugins.PluginUtils;

public class GIFImageReaderSpi extends ImageReaderSpi {

    public GIFImageReaderSpi() {
        super(PluginUtils.VENDOR_NAME, PluginUtils.DEFAULT_VERSION,
                        ImageType.GIF.getNames(), ImageType.GIF.getSuffixes(),
                        ImageType.GIF.getMimeTypes(),
                        GIFImageReader.class.getName(), STANDARD_INPUT_TYPE,
                        null, false, null, null, null, null, false, null, null,
                        null, null);
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        final byte[] sig = ImageSignature.readSignature(source, 6);
        return ImageSignature.GIF87a.verify(sig)
            || ImageSignature.GIF89a.verify(sig);
    }

    @Override
    public ImageReader createReaderInstance(Object extension)
                    throws IOException {
        return new GIFImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "GIF image decoder"; //$NON-NLS-1$
    }

}
