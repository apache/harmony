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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;

public abstract class ImageWriter implements ImageTranscoder {

    protected Locale[] availableLocales;
    protected Locale locale;
    protected ImageWriterSpi originatingProvider;
    protected Object output;
    protected List<IIOWriteProgressListener> progressListeners;
    protected List<IIOWriteWarningListener> warningListeners;
    protected List<Locale> warningLocales;

    // Indicates that abort operation is requested
    // Abort mechanism should be thread-safe
    private boolean aborted;

    protected ImageWriter(ImageWriterSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    public abstract IIOMetadata convertStreamMetadata(IIOMetadata iioMetadata,
                                             ImageWriteParam imageWriteParam);

    public abstract IIOMetadata convertImageMetadata(IIOMetadata iioMetadata,
                                            ImageTypeSpecifier imageTypeSpecifier,
                                            ImageWriteParam imageWriteParam);

    public ImageWriterSpi getOriginatingProvider() {
        return originatingProvider;
    }

    protected void processImageStarted(int imageIndex) {
        if (null != progressListeners) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.imageStarted(this, imageIndex);
            }
        }
    }

    protected void processImageProgress(float percentageDone) {
        if (null != progressListeners) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.imageProgress(this, percentageDone);
            }
        }
    }

    protected void processImageComplete() {
        if (null != progressListeners) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.imageComplete(this);
            }
        }
    }

    protected void processWarningOccurred(int imageIndex, String warning) {
        if (null == warning) {
            throw new NullPointerException("warning message should not be NULL");
        }
        if (null != warningListeners) {
            for (IIOWriteWarningListener listener : warningListeners) {
                listener.warningOccurred(this, imageIndex, warning);
            }
        }
    }

    protected void processWarningOccurred(int imageIndex, String bundle, String key) {
        if (warningListeners != null) { // Don't check the parameters
            return;
        }

        if (bundle == null) {
            throw new IllegalArgumentException("baseName == null!");
        }
        if (key == null) {
            throw new IllegalArgumentException("keyword == null!");
        }

        // Get the context class loader and try to locate the bundle with it first
        ClassLoader contextClassloader = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
        });

        // Iterate through both listeners and locales
        int n = warningListeners.size();
        for (int i=0; i < n; i++) {
            IIOWriteWarningListener listener = warningListeners.get(i);
            Locale locale = warningLocales.get(i);

            // Now try to get the resource bundle
            ResourceBundle rb;
            try {
                rb = ResourceBundle.getBundle(bundle, locale, contextClassloader);
            } catch (MissingResourceException e) {
                try {
                    rb = ResourceBundle.getBundle(bundle, locale);
                } catch (MissingResourceException e1) {
                    throw new IllegalArgumentException("Bundle not found!");
                }
            }

            try {
                String warning = rb.getString(key);
                listener.warningOccurred(this, imageIndex, warning);
            } catch (MissingResourceException e) {
                throw new IllegalArgumentException("Resource is missing!");
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Resource is not a String!");
            }
        }
    }

    public void setOutput(Object output) {
        if (output != null) {
            ImageWriterSpi spi = getOriginatingProvider();
            if (null != spi) {
                Class[] outTypes = spi.getOutputTypes();
                boolean supported = false;
                for (Class<?> element : outTypes) {
                    if (element.isInstance(output)) {
                        supported = true;
                        break;
                    }
                }
                if (!supported) {
                    throw new IllegalArgumentException("output " + output + " is not supported");
                }
            }
        }
        this.output = output;
    }

    public void write(IIOImage image) throws IOException {
        write(null, image, null);
    }

    public void write(RenderedImage image) throws IOException {
        write(null, new IIOImage(image, null, null), null);
    }

    /**
     * @throws  IllegalStateException - if the output has not been set.
     * @throws UnsupportedOperationException - if image contains a Raster and canWriteRasters returns false.
     * @throws IllegalArgumentException - if image is null.
     * @throws IOException - if an error occurs during writing.
     *
     * if !canWriteRaster() then Image must contain only RenderedImage

     * @param streamMetadata <code>null</code> for default stream metadata
     * @param image
     * @param param <code>null</code> for default params
     */
    public abstract void write(IIOMetadata streamMetadata,
                               IIOImage image, ImageWriteParam param) throws IOException;

    public void dispose() {
        // def impl. does nothing according to the spec.
    }

    public synchronized void abort() {
        aborted = true;
    }

    protected synchronized boolean abortRequested() {
        return aborted;
    }

    protected synchronized void clearAbortRequest() {
        aborted = false;
    }

    public void addIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null) {
            return;
        }

        if (progressListeners == null) {
            progressListeners = new ArrayList<IIOWriteProgressListener>();
        }

        progressListeners.add(listener);
    }

    public void addIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null) {
            return;
        }

        if (warningListeners == null) {
            warningListeners = new ArrayList<IIOWriteWarningListener>();
            warningLocales = new ArrayList<Locale>();
        }

        warningListeners.add(listener);
        warningLocales.add(getLocale());
    }

    public Object getOutput() {
        return output;
    }

    private final boolean checkOutputReturnFalse() {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    private final void unsupportedOperation() {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        throw new UnsupportedOperationException("Unsupported write variant!");
    }


    public boolean canInsertEmpty(int imageIndex) throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canInsertImage(int imageIndex) throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canRemoveImage(int imageIndex) throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canReplaceImageMetadata(int imageIndex) throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canReplacePixels(int imageIndex) throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canReplaceStreamMetadata() throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canWriteEmpty() throws IOException {
        return checkOutputReturnFalse();
    }

    public boolean canWriteRasters() {
        return false;
    }

    public boolean canWriteSequence() {
        return false;
    }

    public void endInsertEmpty() throws IOException {
        unsupportedOperation();
    }

    public void endReplacePixels() throws IOException {
        unsupportedOperation();
    }

    public void endWriteEmpty() throws IOException {
        unsupportedOperation();
    }

    public void endWriteSequence() throws IOException {
        unsupportedOperation();
    }

    public Locale[] getAvailableLocales() {
        if (availableLocales == null) {
            return null;
        }

        return availableLocales.clone();
    }

    public abstract IIOMetadata getDefaultImageMetadata(
            ImageTypeSpecifier imageType,
            ImageWriteParam param
    );

    public abstract IIOMetadata getDefaultStreamMetadata(ImageWriteParam param);

    public Locale getLocale() {
        return locale;
    }

    public ImageWriteParam getDefaultWriteParam() {
        return new ImageWriteParam(getLocale());
    }

    public int getNumThumbnailsSupported(
            ImageTypeSpecifier imageType,
            ImageWriteParam param,
            IIOMetadata streamMetadata,
            IIOMetadata imageMetadata
    ) {
        return 0;
    }

    public Dimension[] getPreferredThumbnailSizes(
            ImageTypeSpecifier imageType,
            ImageWriteParam param,
            IIOMetadata streamMetadata,
            IIOMetadata imageMetadata
    ) {
        return null;
    }

    public void prepareInsertEmpty(
            int imageIndex, ImageTypeSpecifier imageType,
            int width, int height,
            IIOMetadata imageMetadata, List<? extends BufferedImage> thumbnails,
            ImageWriteParam param
    ) throws IOException {
        unsupportedOperation();
    }

    public void prepareReplacePixels(int imageIndex, Rectangle region) throws IOException {
        unsupportedOperation();
    }

    public void prepareWriteEmpty(
            IIOMetadata streamMetadata, ImageTypeSpecifier imageType,
            int width, int height,
            IIOMetadata imageMetadata, List<? extends BufferedImage> thumbnails,
            ImageWriteParam param
    ) throws IOException {
        unsupportedOperation();
    }

    public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
        unsupportedOperation();
    }

    protected void processThumbnailComplete() {
        if (progressListeners != null) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.thumbnailComplete(this);
            }
        }
    }

    protected void processThumbnailProgress(float percentageDone) {
        if (progressListeners != null) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.thumbnailProgress(this, percentageDone);
            }
        }
    }

    protected void processThumbnailStarted(int imageIndex, int thumbnailIndex) {
        if (progressListeners != null) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.thumbnailStarted(this, imageIndex, thumbnailIndex);
            }
        }
    }

    protected void processWriteAborted() {
        if (progressListeners != null) {
            for (IIOWriteProgressListener listener : progressListeners) {
                listener.writeAborted(this);
            }
        }
    }

    public void removeAllIIOWriteProgressListeners() {
        progressListeners = null;
    }

    public void removeAllIIOWriteWarningListeners() {
        warningListeners = null;
        warningLocales = null;
    }

    public void removeIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (progressListeners != null && listener != null) {
            if (progressListeners.remove(listener) && progressListeners.isEmpty()) {
                progressListeners = null;
            }
        }
    }

    public void removeIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (warningListeners == null || listener == null) {
            return;
        }

        int idx = warningListeners.indexOf(listener);
        if (idx > -1) {
            warningListeners.remove(idx);
            warningLocales.remove(idx);

            if (warningListeners.isEmpty()) {
                warningListeners = null;
                warningLocales = null;
            }
        }
    }

    public void removeImage(int imageIndex) throws IOException {
        unsupportedOperation();
    }

    public void replaceImageMetadata(int imageIndex, IIOMetadata imageMetadata) throws IOException {
        unsupportedOperation();
    }

    public void replacePixels(RenderedImage image, ImageWriteParam param) throws IOException {
        unsupportedOperation();
    }

    public void replacePixels(Raster raster, ImageWriteParam param) throws IOException {
        unsupportedOperation();
    }

    public void replaceStreamMetadata(IIOMetadata streamMetadata) throws IOException {
        unsupportedOperation();
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            this.locale = null;
            return;
        }

        Locale[] locales = getAvailableLocales();
        boolean validLocale = false;
        if (locales != null) {
            for (int i = 0; i < locales.length; i++) {
                if (locale.equals(locales[i])) {
                    validLocale = true;
                    break;
                }
            }
        }

        if (validLocale) {
            this.locale = locale;
        } else {
            throw new IllegalArgumentException("Invalid locale!");
        }
    }

    public void reset() {
        setOutput(null);
        setLocale(null);
        removeAllIIOWriteWarningListeners();
        removeAllIIOWriteProgressListeners();
        clearAbortRequest();
    }

    public void writeInsert(int imageIndex, IIOImage image, ImageWriteParam param) throws IOException {
        unsupportedOperation();
    }

    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
        unsupportedOperation();
    }
}
