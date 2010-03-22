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
 * @author Viskov Nikolay
 */
package org.apache.harmony.x.imageio.plugins.png;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

import org.apache.harmony.x.imageio.plugins.ImageType;
import org.apache.harmony.x.imageio.plugins.PluginUtils;

public class PNGImageWriterSpi extends ImageWriterSpi {

    public PNGImageWriterSpi() {
        super(PluginUtils.VENDOR_NAME, PluginUtils.DEFAULT_VERSION,
                        ImageType.PNG.getNames(), ImageType.PNG.getSuffixes(),
                        ImageType.PNG.getMimeTypes(),
                        PNGImageWriter.class.getName(), STANDARD_OUTPUT_TYPE,
                        new String[] { PNGImageReaderSpi.class.getName() },
                        false,// supportsStandardStreamMetadataFormat
                        null,// nativeStreamMetadataFormatName
                        null,// nativeStreamMetadataFormatClassName
                        null,// extraStreamMetadataFormatNames
                        null,// extraStreamMetadataFormatClassNames
                        false,// supportsStandardImageMetadataFormat
                        null,// nativeImageMetadataFormatName
                        null,// nativeImageMetadataFormatClassName
                        null,// extraImageMetadataFormatNames
                        null// extraImageMetadataFormatClassNames
        );
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        boolean canEncode = true;

        int numBands = type.getSampleModel().getNumBands();

        ColorModel colorModel = type.getColorModel();

        int bitDepth = colorModel.getPixelSize() / numBands;

        if (colorModel instanceof IndexColorModel) {
            if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4
                && bitDepth != 8) {
                canEncode = false;
            }
            if (numBands != 1) {
                canEncode = false;
            }
        } else if (numBands == 1) {
            if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4
                && bitDepth != 8 && bitDepth != 16) {
                canEncode = false;
            }
        } else if (numBands == 2) {
            if (bitDepth != 8 && bitDepth != 16) {
                canEncode = false;
            }
        } else if (numBands == 3) {
            if (bitDepth != 8 && bitDepth != 16) {
                canEncode = false;
            }
        } else if (numBands == 4) {
            if (bitDepth != 8 && bitDepth != 16) {
                canEncode = false;
            }
        }

        return canEncode;
    }

    @Override
    public ImageWriter createWriterInstance(Object arg0) throws IOException {
        return new PNGImageWriter(this);
    }

    @Override
    public String getDescription(Locale arg0) {
        return "PNG image encoder"; //$NON-NLS-1$
    }

}
