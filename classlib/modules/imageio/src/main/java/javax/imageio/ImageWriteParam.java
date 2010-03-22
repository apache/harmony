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
package javax.imageio;

import java.util.Locale;
import java.awt.*;

public class ImageWriteParam extends IIOParam {

    public static final int MODE_DISABLED = 0;
    public static final int MODE_DEFAULT = 1;
    public static final int MODE_EXPLICIT = 2;
    public static final int MODE_COPY_FROM_METADATA = 3;
    protected boolean canWriteTiles = false;
    protected int tilingMode = MODE_COPY_FROM_METADATA;
    protected Dimension[] preferredTileSizes = null;
    protected boolean tilingSet = false;
    protected int tileWidth = 0;
    protected int tileHeight = 0;
    protected boolean canOffsetTiles = false;
    protected int tileGridXOffset = 0;
    protected int tileGridYOffset = 0;
    protected boolean canWriteProgressive = false;
    protected int progressiveMode = MODE_COPY_FROM_METADATA;
    protected boolean canWriteCompressed = false;
    protected int compressionMode = MODE_COPY_FROM_METADATA;
    protected String[] compressionTypes = null;
    protected String compressionType = null;
    protected float compressionQuality = 1.0f;
    protected Locale locale = null;

    protected ImageWriteParam() {}

    public ImageWriteParam(Locale locale) {
        this.locale = locale;

    }

    public int getProgressiveMode() {
        if (canWriteProgressive()) {
            return progressiveMode;
        }
        throw new UnsupportedOperationException("progressive mode is not supported");
    }

    public boolean canWriteProgressive() {
        return canWriteProgressive;
    }

    public void setProgressiveMode(int mode) {
        if (canWriteProgressive()) {
            if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA || mode == MODE_EXPLICIT) {
                throw new IllegalArgumentException("mode is not supported");
            }
            this.progressiveMode = mode;
        }
        throw new UnsupportedOperationException("progressive mode is not supported");
    }

    public boolean canOffsetTiles() {
        return canOffsetTiles;
    }

    public boolean canWriteCompressed() {
        return canWriteCompressed;
    }

    public boolean canWriteTiles() {
        return canWriteTiles;
    }

    private final void checkWriteCompressed() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
    }

    private final void checkCompressionMode() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
    }

    private final void checkCompressionType() {
        if (getCompressionTypes() != null && getCompressionType() == null) {
            throw new IllegalStateException("No compression type set!");
        }
    }

    public int getCompressionMode() {
        checkWriteCompressed();
        return compressionMode;
    }

    public String[] getCompressionTypes() {
        checkWriteCompressed();
        if (compressionTypes != null) {
            return compressionTypes.clone();
        }
        return null;
    }

    public String getCompressionType() {
        checkWriteCompressed();
        checkCompressionMode();
        return compressionType;
    }

    public float getBitRate(float quality) {
        checkWriteCompressed();
        checkCompressionMode();
        checkCompressionType();
        if (quality < 0 || quality > 1) {
            throw new IllegalArgumentException("Quality out-of-bounds!");
        }
        return -1.0f;
    }

    public float getCompressionQuality() {
        checkWriteCompressed();
        checkCompressionMode();
        checkCompressionType();
        return compressionQuality;
    }

    public String[] getCompressionQualityDescriptions() {
        checkWriteCompressed();
        checkCompressionMode();
        checkCompressionType();
        return null;
    }

    public float[] getCompressionQualityValues() {
        checkWriteCompressed();
        checkCompressionMode();
        checkCompressionType();
        return null;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLocalizedCompressionTypeName() {
        checkWriteCompressed();
        checkCompressionMode();

        String compressionType = getCompressionType();
        if (compressionType == null) {
            throw new IllegalStateException("No compression type set!");
        }
        return compressionType;

    }

    private final void checkTiling() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
    }

    private final void checkTilingMode() {
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
    }

    private final void checkTilingParams() {
        if (!tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
    }

    public int getTilingMode() {
        checkTiling();
        return tilingMode;
    }

    public Dimension[] getPreferredTileSizes() {
        checkTiling();
        if (preferredTileSizes == null) {
            return null;
        }

        Dimension[] retval = new Dimension[preferredTileSizes.length];
        for (int i = 0; i < preferredTileSizes.length; i++) {
            retval[i] = new Dimension(retval[i]);
        }
        return retval;
    }

    public int getTileGridXOffset() {
        checkTiling();
        checkTilingMode();
        checkTilingParams();
        return tileGridXOffset;
    }

    public int getTileGridYOffset() {
        checkTiling();
        checkTilingMode();
        checkTilingParams();
        return tileGridYOffset;
    }

    public int getTileHeight() {
        checkTiling();
        checkTilingMode();
        checkTilingParams();
        return tileHeight;
    }

    public int getTileWidth() {
        checkTiling();
        checkTilingMode();
        checkTilingParams();
        return tileWidth;
    }

    public boolean isCompressionLossless() {
        checkWriteCompressed();
        checkCompressionMode();
        checkCompressionType();
        return true;
    }

    public void unsetCompression() {
        checkWriteCompressed();
        checkCompressionMode();
        compressionType = null;
        compressionQuality = 1;
    }

    public void setCompressionMode(int mode) {
        checkWriteCompressed();
        switch (mode) {
            case MODE_EXPLICIT: {
                compressionMode = mode;
                unsetCompression();
                break;
            }
            case MODE_COPY_FROM_METADATA:
            case MODE_DISABLED:
            case MODE_DEFAULT: {
                compressionMode = mode;
                break;
            }
            default: {
                throw new IllegalArgumentException("Illegal value for mode!");
            }
        }
    }

    public void setCompressionQuality(float quality) {
        checkWriteCompressed();
        checkCompressionMode();
        checkCompressionType();
        if (quality < 0 || quality > 1) {
            throw new IllegalArgumentException("Quality out-of-bounds!");
        }
        compressionQuality = quality;
    }

    public void setCompressionType(String compressionType) {
        checkWriteCompressed();
        checkCompressionMode();

        if (compressionType == null) { // Don't check anything
            this.compressionType = null;
        } else {
            String[] compressionTypes = getCompressionTypes();
            if (compressionTypes == null) {
                throw new UnsupportedOperationException("No settable compression types");
            }

            for (int i = 0; i < compressionTypes.length; i++) {
                if (compressionTypes[i].equals(compressionType)) {
                    this.compressionType = compressionType;
                    return;
                }
            }

            // Compression type is not in the list.
            throw new IllegalArgumentException("Unknown compression type!");
        }
    }

    public void setTiling(int tileWidth, int tileHeight, int tileGridXOffset, int tileGridYOffset) {
        checkTiling();
        checkTilingMode();

        if (!canOffsetTiles() && (tileGridXOffset != 0 || tileGridYOffset != 0)) {
            throw new UnsupportedOperationException("Can't offset tiles!");
        }

        if (tileWidth <=0 || tileHeight <= 0) {
            throw new IllegalArgumentException("tile dimensions are non-positive!");
        }

        Dimension preferredTileSizes[] = getPreferredTileSizes();
        if (preferredTileSizes != null) {
            for (int i = 0; i < preferredTileSizes.length; i+=2) {
                Dimension minSize = preferredTileSizes[i];
                Dimension maxSize = preferredTileSizes[i+1];
                if (
                        tileWidth < minSize.width || tileWidth > maxSize.width ||
                        tileHeight < minSize.height || tileHeight > maxSize.height
                ) {
                    throw new IllegalArgumentException("Illegal tile size!");
                }
            }
        }

        tilingSet = true;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileGridXOffset = tileGridXOffset;
        this.tileGridYOffset = tileGridYOffset;
    }

    public void unsetTiling() {
        checkTiling();
        checkTilingMode();

        tilingSet = false;
        tileWidth = 0;
        tileHeight = 0;
        tileGridXOffset = 0;
        tileGridYOffset = 0;
    }

    public void setTilingMode(int mode) {
        checkTiling();

        switch (mode) {
            case MODE_EXPLICIT: {
                tilingMode = mode;
                unsetTiling();
                break;
            }
            case MODE_COPY_FROM_METADATA:
            case MODE_DISABLED:
            case MODE_DEFAULT: {
                tilingMode = mode;
                break;
            }
            default: {
                throw new IllegalArgumentException("Illegal value for mode!");
            }
        }
    }
}

