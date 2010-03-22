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

package javax.imageio.plugins.jpeg;

import org.apache.harmony.x.imageio.plugins.jpeg.JPEGConsts;

import javax.imageio.ImageWriteParam;
import java.util.Locale;

public class JPEGImageWriteParam extends ImageWriteParam {
    private static final float[] COMP_QUALITY_VALUES = {0.05f, 0.75f, 0.95f};
    private static final String[] COMP_QUALITY_DESCRIPTIONS = {
            "Minimum useful",
            "Visually lossless",
            "Maximum useful"
    };

    private JPEGQTable[] qTables;
    private JPEGHuffmanTable[] dcHuffmanTables;
    private JPEGHuffmanTable[] acHuffmanTables;

    private boolean optimizeHuffmanTables;

    public JPEGImageWriteParam(Locale locale) {
        super(locale);

        canWriteProgressive = true;
        progressiveMode = ImageWriteParam.MODE_DISABLED;

        canWriteCompressed = true;
        compressionTypes = new String[]{"JPEG"};
        compressionType = compressionTypes[0]; 
        compressionQuality = JPEGConsts.DEFAULT_JPEG_COMPRESSION_QUALITY;
    }

    public boolean areTablesSet() {
        return qTables != null;
    }

    public void setEncodeTables(
            JPEGQTable[] qTables,
            JPEGHuffmanTable[] DCHuffmanTables,
            JPEGHuffmanTable[] ACHuffmanTables
    ) {
        if (qTables == null || DCHuffmanTables == null || ACHuffmanTables == null) {
            throw new IllegalArgumentException("Invalid JPEG table arrays");
        }
        if(DCHuffmanTables.length != ACHuffmanTables.length) {
            throw new IllegalArgumentException("Invalid JPEG table arrays");
        }
        if (qTables.length > 4 || DCHuffmanTables.length > 4) {
            throw new IllegalArgumentException("Invalid JPEG table arrays");
        }

        // Do the shallow copy, it should be enough
        this.qTables = qTables.clone();
        dcHuffmanTables = DCHuffmanTables.clone();
        acHuffmanTables = ACHuffmanTables.clone();
    }

    public void unsetEncodeTables() {
        qTables = null;
        dcHuffmanTables = null;
        acHuffmanTables = null;
    }

    public JPEGHuffmanTable[] getDCHuffmanTables() {
        return dcHuffmanTables == null ? null : dcHuffmanTables.clone();
    }

    public JPEGHuffmanTable[] getACHuffmanTables() {
        return acHuffmanTables == null ? null : acHuffmanTables.clone();
    }

    public JPEGQTable[] getQTables() {
        return qTables == null ? null : qTables.clone();
    }

    @Override
    public String[] getCompressionQualityDescriptions() {
        super.getCompressionQualityDescriptions();
        return COMP_QUALITY_DESCRIPTIONS.clone();
    }

    @Override
    public float[] getCompressionQualityValues() {
        super.getCompressionQualityValues();
        return COMP_QUALITY_VALUES.clone();
    }

    public void setOptimizeHuffmanTables(boolean optimize) {
        optimizeHuffmanTables = optimize;
    }

    public boolean getOptimizeHuffmanTables() {
        return optimizeHuffmanTables;
    }

    @Override
    public boolean isCompressionLossless() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return false;
    }

    @Override
    public void unsetCompression() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        compressionQuality = JPEGConsts.DEFAULT_JPEG_COMPRESSION_QUALITY;
    }
}
