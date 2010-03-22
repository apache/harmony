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

public class JPEGSpiConsts {
    private JPEGSpiConsts() {
    }

    static final String[] writerSpiNames                       = { JPEGImageWriterSpi.class.getName() };
    static final String[] readerSpiNames                       = { JPEGImageReaderSpi.class.getName() };

    // -- TODO fill this stuff with correct data
    static final boolean  supportsStandardStreamMetadataFormat = false;
    static final String   nativeStreamMetadataFormatName       = null;
    static final String   nativeStreamMetadataFormatClassName  = null;
    static final String[] extraStreamMetadataFormatNames       = null;
    static final String[] extraStreamMetadataFormatClassNames  = null;
    static final boolean  supportsStandardImageMetadataFormat  = false;
    static final String   nativeImageMetadataFormatName        = null;
    static final String   nativeImageMetadataFormatClassName   = null;
    static final String[] extraImageMetadataFormatNames        = null;
    static final String[] extraImageMetadataFormatClassNames   = null;
}
