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
 * @author Rustem V. Rafikov
 */
package org.apache.harmony.x.imageio.plugins.jpeg;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

import org.apache.harmony.x.imageio.plugins.ImageType;
import org.apache.harmony.x.imageio.plugins.PluginUtils;

public class JPEGImageWriterSpi extends ImageWriterSpi {

    public JPEGImageWriterSpi() {
        /* TODO: support st. metadata format */
        super(PluginUtils.VENDOR_NAME, PluginUtils.DEFAULT_VERSION,
                        ImageType.JPEG.getNames(),
                        ImageType.JPEG.getSuffixes(),
                        ImageType.JPEG.getMimeTypes(),
                        JPEGImageWriter.class.getName(), STANDARD_OUTPUT_TYPE,
                        JPEGSpiConsts.readerSpiNames,
                        JPEGSpiConsts.supportsStandardStreamMetadataFormat,
                        JPEGSpiConsts.nativeStreamMetadataFormatName,
                        JPEGSpiConsts.nativeStreamMetadataFormatClassName,
                        JPEGSpiConsts.extraStreamMetadataFormatNames,
                        JPEGSpiConsts.extraStreamMetadataFormatClassNames,
                        JPEGSpiConsts.supportsStandardImageMetadataFormat,
                        JPEGSpiConsts.nativeImageMetadataFormatName,
                        JPEGSpiConsts.nativeImageMetadataFormatClassName,
                        JPEGSpiConsts.extraImageMetadataFormatNames,
                        JPEGSpiConsts.extraImageMetadataFormatClassNames);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier imageTypeSpecifier) {
        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object o) throws IOException {
        return new JPEGImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "JPEG image Encoder"; //$NON-NLS-1$
    }
}
