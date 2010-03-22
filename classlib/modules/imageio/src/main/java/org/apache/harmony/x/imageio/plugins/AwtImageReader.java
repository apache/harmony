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
package org.apache.harmony.x.imageio.plugins;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.apache.harmony.awt.gl.image.DecodingImageSource;
import org.apache.harmony.awt.gl.image.OffscreenImage;
import org.apache.harmony.x.imageio.internal.nls.Messages;

/**
 * This implementation is based on the image loader from the AWT module. It
 * supports all the image types supported by the loader.
 */
public class AwtImageReader extends ImageReader {

    private ImageInputStream iis;
    private OffscreenImage   image;

    public AwtImageReader(final ImageReaderSpi imageReaderSpi) {
        super(imageReaderSpi);
    }

    @Override
    public int getHeight(final int i) throws IOException {
        return getImage(i).getHeight(new ImageObserver() {
            public boolean imageUpdate(final Image img, final int infoflags,
                            final int x, final int y, final int width,
                            final int height) {
                return (infoflags & HEIGHT) == 0;
            }
        });
    }

    @Override
    public int getWidth(final int i) throws IOException {
        return getImage(i).getWidth(new ImageObserver() {
            public boolean imageUpdate(final Image img, final int infoflags,
                            final int x, final int y, final int width,
                            final int height) {
                return (infoflags & WIDTH) == 0;
            }
        });
    }

    @Override
    public int getNumImages(final boolean b) throws IOException {
        return 1;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int i)
                    throws IOException {
        final ColorModel model = getImage(i).getColorModel();
        final ImageTypeSpecifier[] spec = { new ImageTypeSpecifier(model,
                        model.createCompatibleSampleModel(1, 1)) };
        return Arrays.asList(spec).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(final int i) throws IOException {
        return null;
    }

    @Override
    public BufferedImage read(final int i, final ImageReadParam imageReadParam)
                    throws IOException {
        return getImage(i).getBufferedImage();
    }

    @Override
    public void setInput(final Object input, final boolean seekForwardOnly,
                    final boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        iis = (ImageInputStream) input;
        image = null;
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    private OffscreenImage getImage(final int index) throws IOException {
        if (index >= getNumImages(false)) {
            throw new IndexOutOfBoundsException("index >= getNumImages()"); //$NON-NLS-1$
        }

        if (image == null) {
            if (iis == null) {
                throw new IllegalArgumentException(Messages.getString(
                    "imageio.2", //$NON-NLS-1$
                    "input")); //$NON-NLS-1$
            }

            final DecodingImageSource source = new IISDecodingImageSource(iis);
            image = new OffscreenImage(source);
            source.addConsumer(image);
            source.load();
        }

        return image;
    }

    private static class IISDecodingImageSource extends DecodingImageSource {

        private final InputStream is;

        IISDecodingImageSource(final ImageInputStream iis) {
            is = PluginUtils.wrapIIS(iis);
        }

        @Override
        protected boolean checkConnection() {
            return true;
        }

        @Override
        protected InputStream getInputStream() {
            return is;
        }
    }
}
