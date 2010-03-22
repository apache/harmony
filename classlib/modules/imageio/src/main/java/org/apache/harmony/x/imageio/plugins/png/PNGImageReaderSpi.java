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

package org.apache.harmony.x.imageio.plugins.png;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

import org.apache.harmony.x.imageio.plugins.ImageSignature;
import org.apache.harmony.x.imageio.plugins.ImageType;
import org.apache.harmony.x.imageio.plugins.PluginUtils;

public class PNGImageReaderSpi extends ImageReaderSpi {

    public PNGImageReaderSpi() {
        super(PluginUtils.VENDOR_NAME, PluginUtils.DEFAULT_VERSION,
                        ImageType.PNG.getNames(), ImageType.PNG.getSuffixes(),
                        ImageType.PNG.getMimeTypes(),
                        PNGImageReader.class.getName(), STANDARD_INPUT_TYPE,
                        new String[] { PNGImageWriterSpi.class.getName() },
                        false, null, null, null, null, false, null, null, null,
                        null);
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        return ImageSignature.PNG.verify(source);
    }

    @Override
    public ImageReader createReaderInstance(Object extension)
                    throws IOException {
        return new PNGImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "PNG image decoder"; //$NON-NLS-1$
    }
}
