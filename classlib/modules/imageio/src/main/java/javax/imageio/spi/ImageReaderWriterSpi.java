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

import org.apache.harmony.x.imageio.metadata.IIOMetadataUtils;

import javax.imageio.metadata.IIOMetadataFormat;

public abstract class ImageReaderWriterSpi extends IIOServiceProvider
        implements RegisterableService {

    protected String[] names;
    protected String[] suffixes;
    protected String[] MIMETypes;
    protected String pluginClassName;
    protected boolean supportsStandardStreamMetadataFormat;
    protected String nativeStreamMetadataFormatName;
    protected String nativeStreamMetadataFormatClassName;
    protected String[] extraStreamMetadataFormatNames;
    protected String[] extraStreamMetadataFormatClassNames;
    protected boolean supportsStandardImageMetadataFormat;
    protected String nativeImageMetadataFormatName;
    protected String nativeImageMetadataFormatClassName;
    protected String[] extraImageMetadataFormatNames;
    protected String[] extraImageMetadataFormatClassNames;

    public ImageReaderWriterSpi(String vendorName, String version, String[] names,
                                String[] suffixes, String[] MIMETypes,
                                String pluginClassName,
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
        super(vendorName, version);

        if (names == null || names.length == 0) {
            throw new NullPointerException("format names array cannot be NULL or empty");
        }

        if (pluginClassName == null) {
            throw new NullPointerException("Plugin class name cannot be NULL");
        }

        // We clone all the arrays to be consistent with the fact that
        // some methods of this class must return clones of the arrays
        // as it is stated in the spec.
        this.names = names.clone();
        this.suffixes = suffixes == null ? null : suffixes.clone();
        this.MIMETypes = MIMETypes == null ? null : MIMETypes.clone();
        this.pluginClassName = pluginClassName;
        this.supportsStandardStreamMetadataFormat = supportsStandardStreamMetadataFormat;
        this.nativeStreamMetadataFormatName = nativeStreamMetadataFormatName;
        this.nativeStreamMetadataFormatClassName = nativeStreamMetadataFormatClassName;

        this.extraStreamMetadataFormatNames =
                extraStreamMetadataFormatNames == null ?
                null : extraStreamMetadataFormatNames.clone();

        this.extraStreamMetadataFormatClassNames =
                extraStreamMetadataFormatClassNames == null ?
                null : extraStreamMetadataFormatClassNames.clone();

        this.supportsStandardImageMetadataFormat = supportsStandardImageMetadataFormat;
        this.nativeImageMetadataFormatName = nativeImageMetadataFormatName;
        this.nativeImageMetadataFormatClassName = nativeImageMetadataFormatClassName;

        this.extraImageMetadataFormatNames =
                extraImageMetadataFormatNames == null ?
                null : extraImageMetadataFormatNames.clone();

        this.extraImageMetadataFormatClassNames =
                extraImageMetadataFormatClassNames == null ?
                null : extraImageMetadataFormatClassNames.clone();
    }

    public ImageReaderWriterSpi() {}

    public String[] getFormatNames() {
        return names.clone();
    }

    public String[] getFileSuffixes() {
        return suffixes == null ? null : suffixes.clone();
    }

    public String[] getExtraImageMetadataFormatNames() {
        return extraImageMetadataFormatNames == null ? null : extraImageMetadataFormatNames.clone();
    }

    public String[] getExtraStreamMetadataFormatNames() {
        return extraStreamMetadataFormatNames == null ? null : extraStreamMetadataFormatNames.clone();
    }

    public IIOMetadataFormat getImageMetadataFormat(String formatName) {
        return IIOMetadataUtils.instantiateMetadataFormat(
                formatName, supportsStandardImageMetadataFormat,
                nativeImageMetadataFormatName, nativeImageMetadataFormatClassName,
                extraImageMetadataFormatNames, extraImageMetadataFormatClassNames
        );
    }

    public IIOMetadataFormat getStreamMetadataFormat(String formatName) {
        return IIOMetadataUtils.instantiateMetadataFormat(
                formatName, supportsStandardStreamMetadataFormat,
                nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName,
                extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames
        );
    }

    public String[] getMIMETypes() {
        return MIMETypes == null ? null : MIMETypes.clone();
    }

    public String getNativeImageMetadataFormatName() {
        return nativeImageMetadataFormatName;
    }

    public String getNativeStreamMetadataFormatName() {
        return nativeStreamMetadataFormatName;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public boolean isStandardImageMetadataFormatSupported() {
        return supportsStandardImageMetadataFormat;
    }

    public boolean isStandardStreamMetadataFormatSupported() {
        return supportsStandardStreamMetadataFormat;
    }
}
