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
package javax.imageio;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.apache.harmony.x.imageio.internal.nls.Messages;

public abstract class ImageReader {

    protected ImageReaderSpi                originatingProvider;

    protected Object                        input;

    protected boolean                       seekForwardOnly;

    protected boolean                       ignoreMetadata;

    protected int                           minIndex;

    protected Locale[]                      availableLocales;

    protected Locale                        locale;

    protected List<IIOReadWarningListener>  warningListeners;

    protected List<Locale>                  warningLocales;

    protected List<IIOReadProgressListener> progressListeners;

    protected List<IIOReadUpdateListener>   updateListeners;

    private boolean                         isAborted;

    protected ImageReader(final ImageReaderSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    public String getFormatName() throws IOException {
        return originatingProvider.getFormatNames()[0];
    }

    public ImageReaderSpi getOriginatingProvider() {
        return originatingProvider;
    }

    public void setInput(final Object input, final boolean seekForwardOnly,
                    final boolean ignoreMetadata) {
        if (input != null) {
            if (!isSupported(input) && !(input instanceof ImageInputStream)) {
                throw new IllegalArgumentException(Messages.getString(
                    "imageio.2", //$NON-NLS-1$
                    input));
            }
        }
        this.minIndex = 0;
        this.seekForwardOnly = seekForwardOnly;
        this.ignoreMetadata = ignoreMetadata;
        this.input = input;
    }

    private boolean isSupported(final Object input) {
        ImageReaderSpi spi = getOriginatingProvider();
        if (null != spi) {
            Class<?>[] outTypes = spi.getInputTypes();

            for (Class<?> element : outTypes) {
                if (element.isInstance(input)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setInput(final Object input, final boolean seekForwardOnly) {
        setInput(input, seekForwardOnly, false);
    }

    public void setInput(final Object input) {
        setInput(input, false, false);
    }

    public Object getInput() {
        return input;
    }

    public boolean isSeekForwardOnly() {
        return seekForwardOnly;
    }

    public boolean isIgnoringMetadata() {
        return ignoreMetadata;
    }

    public int getMinIndex() {
        return minIndex;
    }

    public Locale[] getAvailableLocales() {
        return availableLocales;
    }

    public void setLocale(final Locale locale) {
        if (locale != null) {
            final Locale[] locales = getAvailableLocales();

            if ((locales == null) || !arrayContains(locales, locale)) {
                throw new IllegalArgumentException(Messages.getString(
                    "imageio.3", //$NON-NLS-1$
                    "Locale " + locale)); //$NON-NLS-1$
            }
        }

        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public abstract int getNumImages(final boolean allowSearch)
                    throws IOException;

    public abstract int getWidth(final int imageIndex) throws IOException;

    public abstract int getHeight(final int imageIndex) throws IOException;

    public boolean isRandomAccessEasy(final int imageIndex) throws IOException {
        return false;
    }

    public float getAspectRatio(final int imageIndex) throws IOException {
        return (float) getWidth(imageIndex) / getHeight(imageIndex);
    }

    public ImageTypeSpecifier getRawImageType(final int imageIndex)
                    throws IOException {
        return getImageTypes(imageIndex).next();
    }

    public abstract Iterator<ImageTypeSpecifier> getImageTypes(
                    final int imageIndex) throws IOException;

    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    public abstract IIOMetadata getStreamMetadata() throws IOException;

    public IIOMetadata getStreamMetadata(final String formatName,
                    final Set<String> nodeNames) throws IOException {
        iaeIfNull("formatName", formatName); //$NON-NLS-1$
        iaeIfNull("nodeNames", nodeNames); //$NON-NLS-1$

        final IIOMetadata data = getStreamMetadata();
        return isSupportedFormat(formatName, data) ? data : null;
    }

    public abstract IIOMetadata getImageMetadata(final int imageIndex)
                    throws IOException;

    public IIOMetadata getImageMetadata(final int imageIndex,
                    final String formatName, final Set<String> nodeNames)
                    throws IOException {
        iaeIfNull("formatName", formatName); //$NON-NLS-1$
        iaeIfNull("nodeNames", nodeNames); //$NON-NLS-1$

        final IIOMetadata data = getImageMetadata(imageIndex);
        return isSupportedFormat(formatName, data) ? data : null;
    }

    public BufferedImage read(final int imageIndex) throws IOException {
        return read(imageIndex, null);
    }

    public abstract BufferedImage read(final int imageIndex,
                    final ImageReadParam param) throws IOException;

    public IIOImage readAll(final int imageIndex, final ImageReadParam param)
                    throws IOException {
        List<BufferedImage> th = null;
        final BufferedImage img = read(imageIndex, param);
        final int num = getNumThumbnails(imageIndex);

        if (num > 0) {
            th = new ArrayList<BufferedImage>(num);

            for (int i = 0; i < num; i++) {
                th.add(readThumbnail(imageIndex, i));
            }
        }

        return new IIOImage(img, th, getImageMetadata(imageIndex));
    }

    public Iterator<IIOImage> readAll(
                    final Iterator<? extends ImageReadParam> params)
                    throws IOException {
        final int index = getMinIndex();
        final List<IIOImage> list = new LinkedList<IIOImage>();

        processSequenceStarted(index);

        while (params.hasNext()) {
            try {
                list.add(readAll(index, params.next()));
            } catch (final ClassCastException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        processSequenceComplete();
        return list.iterator();
    }

    public boolean canReadRaster() {
        return false; // def
    }

    public Raster readRaster(final int imageIndex, final ImageReadParam param)
                    throws IOException {
        throw new UnsupportedOperationException(Messages.getString("imageio.7", //$NON-NLS-1$
            "readRaster()")); //$NON-NLS-1$
    }

    public boolean isImageTiled(final int imageIndex) throws IOException {
        return false; // def
    }

    public int getTileWidth(final int imageIndex) throws IOException {
        return getWidth(imageIndex); // def
    }

    public int getTileHeight(final int imageIndex) throws IOException {
        return getHeight(imageIndex); // def
    }

    public int getTileGridXOffset(final int imageIndex) throws IOException {
        return 0; // def
    }

    public int getTileGridYOffset(final int imageIndex) throws IOException {
        return 0; // def
    }

    public BufferedImage readTile(final int imageIndex, final int tileX,
                    final int tileY) throws IOException {
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException(Messages.getString("imageio.5", //$NON-NLS-1$
                "0", "tileX & tileY")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return read(imageIndex);
    }

    public Raster readTileRaster(final int imageIndex, final int tileX,
                    final int tileY) throws IOException {
        if (canReadRaster()) {
            if ((tileX != 0) || (tileY != 0)) {
                throw new IllegalArgumentException(Messages.getString(
                    "imageio.5", //$NON-NLS-1$
                    "0", "tileX & tileY")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return readRaster(imageIndex, null);
        }

        throw new UnsupportedOperationException(Messages.getString("imageio.7", //$NON-NLS-1$
            "readTileRaster()")); //$NON-NLS-1$
    }

    public RenderedImage readAsRenderedImage(final int imageIndex,
                    final ImageReadParam param) throws IOException {
        return read(imageIndex, param);
    }

    public boolean readerSupportsThumbnails() {
        return false; // def
    }

    public boolean hasThumbnails(final int imageIndex) throws IOException {
        return getNumThumbnails(imageIndex) > 0; // def
    }

    public int getNumThumbnails(final int imageIndex) throws IOException {
        return 0; // def
    }

    public int getThumbnailWidth(final int imageIndex, final int thumbnailIndex)
                    throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getWidth(); // def
    }

    public int getThumbnailHeight(final int imageIndex, final int thumbnailIndex)
                    throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getHeight(); // def
    }

    public BufferedImage readThumbnail(final int imageIndex,
                    final int thumbnailIndex) throws IOException {
        throw new UnsupportedOperationException(Messages.getString("imageio.7", //$NON-NLS-1$
            "readThumbnail()")); //$NON-NLS-1$
    }

    public void abort() {
        isAborted = true;
    }

    protected boolean abortRequested() {
        return isAborted;
    }

    protected void clearAbortRequest() {
        isAborted = false;
    }

    public void addIIOReadWarningListener(final IIOReadWarningListener listener) {
        if (listener != null) {
            warningListeners = addToList(warningListeners, listener);
            warningLocales = addToList(warningLocales, getLocale());
        }
    }

    public void removeIIOReadWarningListener(
                    final IIOReadWarningListener listener) {
        final int ind;

        if ((warningListeners != null) && (listener != null)
            && ((ind = warningListeners.indexOf(listener)) != -1)) {
            warningListeners.remove(ind);
            warningLocales.remove(ind);
        }
    }

    public void removeAllIIOReadWarningListeners() {
        warningListeners = null;
        warningLocales = null;
    }

    public void addIIOReadProgressListener(
                    final IIOReadProgressListener listener) {
        if (listener != null) {
            progressListeners = addToList(progressListeners, listener);
        }
    }

    public void removeIIOReadProgressListener(
                    final IIOReadProgressListener listener) {
        if ((progressListeners != null) && (listener != null)) {
            progressListeners.remove(listener);
        }
    }

    public void removeAllIIOReadProgressListeners() {
        progressListeners = null;
    }

    public void addIIOReadUpdateListener(final IIOReadUpdateListener listener) {
        if (listener != null) {
            updateListeners = addToList(updateListeners, listener);
        }
    }

    public void removeIIOReadUpdateListener(final IIOReadUpdateListener listener) {
        if ((updateListeners != null) && (listener != null)) {
            updateListeners.remove(listener);
        }
    }

    public void removeAllIIOReadUpdateListeners() {
        updateListeners = null;
    }

    protected void processSequenceStarted(final int minIndex) {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.sequenceStarted(this, minIndex);
            }
        }
    }

    protected void processSequenceComplete() {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.sequenceComplete(this);
            }
        }
    }

    protected void processImageStarted(final int imageIndex) {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.imageStarted(this, imageIndex);
            }
        }
    }

    protected void processImageProgress(final float percentageDone) {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.imageProgress(this, percentageDone);
            }
        }
    }

    protected void processImageComplete() {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.imageComplete(this);
            }
        }
    }

    protected void processThumbnailStarted(final int imageIndex,
                    final int thumbnailIndex) {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.thumbnailStarted(this, imageIndex, thumbnailIndex);
            }
        }
    }

    protected void processThumbnailProgress(final float percentageDone) {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.thumbnailProgress(this, percentageDone);
            }
        }
    }

    protected void processThumbnailComplete() {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.thumbnailComplete(this);
            }
        }
    }

    protected void processReadAborted() {
        if (progressListeners != null) {
            for (final IIOReadProgressListener listener : progressListeners) {
                listener.readAborted(this);
            }
        }
    }

    protected void processPassStarted(final BufferedImage theImage,
                    final int pass, final int minPass, final int maxPass,
                    final int minX, final int minY, final int periodX,
                    final int periodY, final int[] bands) {
        if (updateListeners != null) {
            for (final IIOReadUpdateListener listener : updateListeners) {
                listener.passStarted(this, theImage, pass, minPass, maxPass,
                    minX, minY, periodX, periodY, bands);
            }
        }
    }

    protected void processImageUpdate(final BufferedImage theImage,
                    final int minX, final int minY, final int width,
                    final int height, final int periodX, final int periodY,
                    final int[] bands) {
        if (updateListeners != null) {
            for (final IIOReadUpdateListener listener : updateListeners) {
                listener.imageUpdate(this, theImage, minX, minY, width, height,
                    periodX, periodY, bands);
            }
        }
    }

    protected void processPassComplete(final BufferedImage theImage) {
        if (updateListeners != null) {
            for (IIOReadUpdateListener listener : updateListeners) {
                listener.passComplete(this, theImage);
            }
        }
    }

    protected void processThumbnailPassStarted(
                    final BufferedImage theThumbnail, final int pass,
                    final int minPass, final int maxPass, final int minX,
                    final int minY, final int periodX, final int periodY,
                    final int[] bands) {
        if (updateListeners != null) {
            for (final IIOReadUpdateListener listener : updateListeners) {
                listener.thumbnailPassStarted(this, theThumbnail, pass,
                    minPass, maxPass, minX, minY, periodX, periodY, bands);
            }
        }
    }

    protected void processThumbnailUpdate(final BufferedImage theThumbnail,
                    final int minX, final int minY, final int width,
                    final int height, final int periodX, final int periodY,
                    final int[] bands) {
        if (updateListeners != null) {
            for (final IIOReadUpdateListener listener : updateListeners) {
                listener.thumbnailUpdate(this, theThumbnail, minX, minY, width,
                    height, periodX, periodY, bands);
            }
        }
    }

    protected void processThumbnailPassComplete(final BufferedImage theThumbnail) {
        if (updateListeners != null) {
            for (final IIOReadUpdateListener listener : updateListeners) {
                listener.thumbnailPassComplete(this, theThumbnail);
            }
        }
    }

    protected void processWarningOccurred(final String warning) {
        if (warningListeners != null) {
            iaeIfNull("warning", warning); //$NON-NLS-1$
            for (final IIOReadWarningListener listener : warningListeners) {
                listener.warningOccurred(this, warning);
            }
        }
    }

    protected void processWarningOccurred(final String baseName,
                    final String keyword) {
        if (warningListeners != null) {
            int i = 0;

            iaeIfNull("keyword", keyword); //$NON-NLS-1$
            iaeIfNull("baseName", baseName); //$NON-NLS-1$

            for (final IIOReadWarningListener listener : warningListeners) {
                try {
                    final Locale locale = warningLocales.get(i);
                    final ResourceBundle bundle = (locale != null)
                        ? ResourceBundle.getBundle(baseName, locale)
                        : ResourceBundle.getBundle(baseName);
                    listener.warningOccurred(this, bundle.getString(keyword));
                } catch (final RuntimeException ex) {
                    throw new IllegalArgumentException(ex.getMessage());
                }

                i++;
            }
        }
    }

    public void reset() {
        setInput(null, false);
        setLocale(null);
        removeAllIIOReadUpdateListeners();
        removeAllIIOReadWarningListeners();
        removeAllIIOReadProgressListeners();
        clearAbortRequest();
    }

    public void dispose() {
        // do nothing by def
    }

    protected static Rectangle getSourceRegion(final ImageReadParam param,
                    final int srcWidth, final int srcHeight) {
        final Rectangle r = new Rectangle(0, 0, srcWidth, srcHeight);

        if (param != null) {
            final int x;
            final int y;
            final Rectangle sr = param.getSourceRegion();

            if (sr != null) {
                r.setBounds(r.intersection(sr));
            }

            x = param.getSubsamplingXOffset();
            y = param.getSubsamplingYOffset();
            r.x += x;
            r.y += y;
            r.width -= x;
            r.height -= y;
        }

        return r;
    }

    protected static void computeRegions(final ImageReadParam param,
                    final int srcWidth, final int srcHeight,
                    final BufferedImage image, final Rectangle srcRegion,
                    final Rectangle destRegion) {
        int xCols = 1;
        int yCols = 1;

        iaeIfNull("srcRegion", srcRegion); //$NON-NLS-1$
        iaeIfNull("destRegion", destRegion); //$NON-NLS-1$
        iaeIfEmpty("srcRegion", srcRegion.isEmpty()); //$NON-NLS-1$
        iaeIfEmpty("destRegion", destRegion.isEmpty()); //$NON-NLS-1$

        srcRegion.setBounds(getSourceRegion(param, srcWidth, srcHeight));

        if (param != null) {
            destRegion.setLocation(param.getDestinationOffset());
            xCols = param.getSourceXSubsampling();
            yCols = param.getSourceYSubsampling();
        }

        if (destRegion.x < 0) {
            final int shift = -destRegion.x * xCols;
            srcRegion.x += shift;
            srcRegion.width -= shift;
            destRegion.x = 0;
        }

        if (destRegion.y < 0) {
            final int shift = -destRegion.y * yCols;
            srcRegion.y += shift;
            srcRegion.height -= shift;
            destRegion.y = 0;
        }

        destRegion.width = srcRegion.width / xCols;
        destRegion.height = srcRegion.height / yCols;

        if (image != null) {
            destRegion.setBounds(destRegion.intersection(new Rectangle(0, 0,
                            image.getWidth(), image.getHeight())));
        }
    }

    protected static void checkReadParamBandSettings(
                    final ImageReadParam param, final int numSrcBands,
                    final int numDstBands) {
        final int[] src = (param != null) ? param.getSourceBands() : null;
        final int[] dst = (param != null) ? param.getDestinationBands() : null;
        final int srcLen = (src != null) ? src.length : numSrcBands;
        final int dstLen = (dst != null) ? dst.length : numDstBands;

        if (srcLen != dstLen) {
            throw new IllegalArgumentException("srcLen != dstLen"); //$NON-NLS-1$
        }

        if (src != null) {
            for (int i = 0; i < srcLen; i++) {
                if (src[i] >= numSrcBands) {
                    throw new IllegalArgumentException("src[" + i //$NON-NLS-1$
                        + "] >= numSrcBands"); //$NON-NLS-1$
                }
            }
        }

        if (dst != null) {
            for (int i = 0; i < dstLen; i++) {
                if (dst[i] >= numDstBands) {
                    throw new IllegalArgumentException("dst[" + i //$NON-NLS-1$
                        + "] >= numDstBands"); //$NON-NLS-1$
                }
            }
        }
    }

    protected static BufferedImage getDestination(final ImageReadParam param,
                    final Iterator<ImageTypeSpecifier> imageTypes,
                    final int width, final int height) throws IIOException {
        iaeIfNull("imageTypes", imageTypes); //$NON-NLS-1$
        iaeIfEmpty("imageTypes", !imageTypes.hasNext()); //$NON-NLS-1$

        if ((long) (width * height) > (long) Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                            "width * height > Integer.MAX_VALUE!"); //$NON-NLS-1$
        }

        final Rectangle dst;
        ImageTypeSpecifier its = null;

        if (param != null) {
            final BufferedImage img = param.getDestination();

            if (img != null) {
                return img;
            }

            its = param.getDestinationType();
        }

        try {
            isValid: if (its != null) {
                while (imageTypes.hasNext()) {
                    if (its.equals((ImageTypeSpecifier) imageTypes.next())) {
                        break isValid;
                    }
                }
                throw new IIOException(Messages.getString("imageio.3", its)); //$NON-NLS-1$
            } else {
                its = imageTypes.next();
            }
        } catch (final ClassCastException ex) {
            throw new IllegalArgumentException(ex);
        }

        dst = new Rectangle(0, 0, 0, 0);
        computeRegions(param, width, height, null, new Rectangle(0, 0, 0, 0),
            dst);
        return its.createBufferedImage(dst.width, dst.height);
    }

    private static <T> List<T> addToList(List<T> list, final T value) {
        if (list == null) {
            list = new LinkedList<T>();
        }

        list.add(value);
        return list;
    }

    private static void iaeIfNull(final String name, final Object value) {
        if (value == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.2", //$NON-NLS-1$
                name));
        }
    }

    private static void iaeIfEmpty(final String name, final boolean isEmpty) {
        if (isEmpty) {
            throw new IllegalArgumentException(Messages.getString("imageio.6", //$NON-NLS-1$
                name));
        }
    }

    private static <T> boolean arrayContains(final T[] array, final Object value) {
        for (T t : array) {
            if ((t == value) || ((t != null) && t.equals(value))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSupportedFormat(final String formatName,
                    final IIOMetadata data) {
        final String[] names;
        return ((data != null) && ((names = data.getMetadataFormatNames()) != null))
            ? arrayContains(names, formatName) : false;
    }
}
