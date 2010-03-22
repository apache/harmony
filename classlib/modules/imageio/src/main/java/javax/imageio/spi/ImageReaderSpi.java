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
import javax.imageio.ImageReader;

import org.apache.harmony.luni.util.NotImplementedException;

import java.io.IOException;

public abstract class ImageReaderSpi extends ImageReaderWriterSpi {

    public static final Class[] STANDARD_INPUT_TYPE = new Class[] {ImageInputStream.class};

    protected Class[] inputTypes;
    protected String[] writerSpiNames;

    protected ImageReaderSpi() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public ImageReaderSpi(String vendorName, String version, String[] names, String[] suffixes,
                             String[] MIMETypes, String pluginClassName,
                             Class[] inputTypes, String[] writerSpiNames,
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

        if (inputTypes == null || inputTypes.length == 0) {
            throw new NullPointerException("input types array cannot be NULL or empty");
        }
        this.inputTypes = inputTypes;
        this.writerSpiNames = writerSpiNames;
    }

    public Class[] getInputTypes() {
        return inputTypes;
    }

    public abstract boolean canDecodeInput(Object source) throws IOException;

    public ImageReader createReaderInstance() throws IOException {
        return createReaderInstance(null);
    }

    public abstract ImageReader createReaderInstance(Object extension) throws IOException;

    public boolean isOwnReader(ImageReader reader) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public String[] getImageWriterSpiNames() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }
}
