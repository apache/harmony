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
package javax.imageio.spi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;

import org.apache.harmony.luni.util.NotImplementedException;

import java.awt.image.RenderedImage;
import java.io.IOException;

public abstract class ImageWriterSpi extends ImageReaderWriterSpi {

    public static final Class[] STANDARD_OUTPUT_TYPE = new Class[] {ImageInputStream.class};

    protected Class[] outputTypes;
    protected String[] readerSpiNames;

    protected ImageWriterSpi() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public ImageWriterSpi(String vendorName, String version, String[] names,
                             String[] suffixes, String[] MIMETypes,
                             String pluginClassName,
                             Class[] outputTypes, String[] readerSpiNames,
                             boolean supportsStandardStreamMetadataFormat,
                             String nativeStreamMetadataFormatName,
                             String nativeStreamMetadataFormatClassName,
                             String[] extraStreamMetadataFormatNames,
                             String[] extraStreamMetadataFormatClassNames,
                             boolean supportsStandardImageMetadataFormat,
                             String nativeImageMetadataFormatName,
                             String nativeImageMetadataFormatClassName,
                             String[] extraImageMetadataFormatNames,
                             String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, pluginClassName,
                supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
                nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
                extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
                nativeImageMetadataFormatName, nativeImageMetadataFormatClassName,
                extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);

        if (outputTypes == null || outputTypes.length == 0) {
            throw new NullPointerException("output types array cannot be NULL or empty");
        }

        this.outputTypes = outputTypes;
        this.readerSpiNames = readerSpiNames;
    }

    public boolean isFormatLossless() {
        return true;
    }

    public Class[] getOutputTypes() {
        return outputTypes;
    }

    public abstract boolean canEncodeImage(ImageTypeSpecifier type);

    public boolean canEncodeImage(RenderedImage im) {
        return canEncodeImage(ImageTypeSpecifier.createFromRenderedImage(im));
    }

    public ImageWriter createWriterInstance() throws IOException {
        return createWriterInstance(null);
    }

    public abstract ImageWriter createWriterInstance(Object extension) throws IOException;

    public boolean isOwnWriter(ImageWriter writer) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public String[] getImageReaderSpiNames() {
        return readerSpiNames;
    }
}
