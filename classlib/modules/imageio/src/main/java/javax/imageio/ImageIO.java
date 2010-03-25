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

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.spi.*;

import org.apache.harmony.luni.util.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URL;
import org.apache.harmony.x.imageio.internal.nls.Messages;

public final class ImageIO {

    private static final IIORegistry registry = IIORegistry.getDefaultInstance();

    private ImageIO() {}
    

    public static void scanForPlugins() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static void setUseCache(boolean useCache) throws NotImplementedException {
        // TODO: implement
    }

    public static boolean getUseCache()  throws NotImplementedException {
        // TODO: implement
        return false;
    }

    public static void setCacheDirectory(File cacheDirectory) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static File getCacheDirectory() throws NotImplementedException {
        // TODO: implement
        //-- null indicates system-dep default temporary directory
        return null;
    }

    public static ImageInputStream createImageInputStream(Object input)
            throws IOException {

        if (input == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.4C"));
        }

        Iterator<ImageInputStreamSpi> it = registry.getServiceProviders(ImageInputStreamSpi.class, true);

        while (it.hasNext()) {
            ImageInputStreamSpi spi = it.next();
            if (spi.getInputClass().isInstance(input)) {
                return spi.createInputStreamInstance(input);
            }
        }
        return null;
    }

    public static ImageOutputStream createImageOutputStream(Object output)
            throws IOException {
        if (output == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.4D"));
        }

        Iterator<ImageOutputStreamSpi> it = registry.getServiceProviders(ImageOutputStreamSpi.class, true);

        while (it.hasNext()) {
            ImageOutputStreamSpi spi = it.next();
            if (spi.getOutputClass().isInstance(output)) {
                // todo - use getUseCache and getCacheDir here
                return spi.createOutputStreamInstance(output);
            }
        }
        return null;
    }

    public static String[] getReaderFormatNames() {
        ArrayList<String> FormatNames = new ArrayList<String>();

        Iterator<ImageReaderSpi> it = registry.getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            FormatNames.addAll(Arrays.asList(spi.getFormatNames()));
        }

        return (String[])FormatNames.toArray(new String[FormatNames.size()]);
    }

    public static String[] getReaderMIMETypes() {
        ArrayList<String> MIMETypes = new ArrayList<String>();

        Iterator<ImageReaderSpi> it = registry.getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            MIMETypes.addAll(Arrays.asList(spi.getMIMETypes()));
        }

        return (String[])MIMETypes.toArray(new String[MIMETypes.size()]);
    }

    public static Iterator<ImageReader> getImageReaders(Object input) {
        if (input == null) {
            throw new NullPointerException(Messages.getString("imageio.4E"));
        }

        Iterator<ImageReaderSpi> it = registry.getServiceProviders(ImageReaderSpi.class,
                new CanReadFilter(input), true);

        return new SpiIteratorToReadersIteratorWrapper(it);
    }

    public static Iterator<ImageReader> getImageReadersByFormatName(String formatName) {
        if (formatName == null) {
            throw new NullPointerException(Messages.getString("imageio.4F"));
        }

        Iterator<ImageReaderSpi> it = registry.getServiceProviders(ImageReaderSpi.class,
                new FormatFilter(formatName), true);

        return new SpiIteratorToReadersIteratorWrapper(it);
    }

    public static Iterator<ImageReader> getImageReadersBySuffix(String fileSuffix) {
        if (fileSuffix == null) {
            throw new NullPointerException(Messages.getString("imageio.50"));
        }
        Iterator<ImageReaderSpi> it = registry.getServiceProviders(ImageReaderSpi.class,
                new SuffixFilter(fileSuffix), true);

        return new SpiIteratorToReadersIteratorWrapper(it);
    }

    public static Iterator<ImageReader> getImageReadersByMIMEType(
                    final String MIMEType) {
        return new SpiIteratorToReadersIteratorWrapper(
                registry.getServiceProviders(ImageReaderSpi.class,
                new MIMETypeFilter(MIMEType), true));
    }

    public static String[] getWriterFormatNames() {
        ArrayList<String> FormatNames = new ArrayList<String>();

        Iterator<ImageWriterSpi> it = registry.getServiceProviders(ImageWriterSpi.class, true);
        while (it.hasNext()) {
            ImageWriterSpi spi = it.next();
            FormatNames.addAll(Arrays.asList(spi.getFormatNames()));
        }

        return (String[])FormatNames.toArray(new String[FormatNames.size()]);
    }

    public static String[] getWriterMIMETypes() {
        ArrayList<String> MIMETypes = new ArrayList<String>();

        Iterator<ImageWriterSpi> it = registry.getServiceProviders(ImageWriterSpi.class, true);
        while (it.hasNext()) {
            ImageWriterSpi spi = it.next();
            MIMETypes.addAll(Arrays.asList(spi.getMIMETypes()));
        }

        return (String[])MIMETypes.toArray(new String[MIMETypes.size()]);
    }

    public static Iterator<ImageWriter> getImageWritersByFormatName(String formatName) {
        if (formatName == null) {
            throw new NullPointerException(Messages.getString("imageio.4F"));
        }

        Iterator<ImageWriterSpi> it = registry.getServiceProviders(ImageWriterSpi.class,
                new FormatFilter(formatName), true);

        return new SpiIteratorToWritersIteratorWrapper(it);
    }

    public static Iterator<ImageWriter> getImageWritersBySuffix(String fileSuffix) {
        if (fileSuffix == null) {
            throw new NullPointerException(Messages.getString("imageio.50"));
        }
        Iterator<ImageWriterSpi> it = registry.getServiceProviders(ImageWriterSpi.class,
                new SuffixFilter(fileSuffix), true);
        return new SpiIteratorToWritersIteratorWrapper(it);
    }

    public static Iterator<ImageWriter> getImageWritersByMIMEType(
                    final String MIMEType) {
        return new SpiIteratorToWritersIteratorWrapper(
                registry.getServiceProviders(ImageWriterSpi.class,
                new MIMETypeFilter(MIMEType), true));
    }

    public static ImageWriter getImageWriter(ImageReader reader) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static ImageReader getImageReader(ImageWriter writer) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static Iterator<ImageWriter> getImageWriters(ImageTypeSpecifier type,
                                           String formatName) {
        if (type == null) {
            throw new NullPointerException(Messages.getString("imageio.51"));
        }

        if (formatName == null) {
            throw new NullPointerException(Messages.getString("imageio.4F"));
        }

        Iterator<ImageWriterSpi> it = registry.getServiceProviders(ImageWriterSpi.class,
                new FormatAndEncodeFilter(type, formatName), true);

        return new SpiIteratorToWritersIteratorWrapper(it);
    }

    public static Iterator<ImageTranscoder> getImageTranscoders(ImageReader reader,
                                               ImageWriter writer) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static BufferedImage read(File input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.52"));
        }

        ImageInputStream stream = createImageInputStream(input);
        return read(stream);
    }

    public static BufferedImage read(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.52"));
        }

        ImageInputStream stream = createImageInputStream(input);
        return read(stream);
    }

    public static BufferedImage read(URL input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.52"));
        }

        InputStream stream = input.openStream();
        BufferedImage res = read(stream);
        stream.close();
        
        return res;
    }

    public static BufferedImage read(ImageInputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.0A"));
        }

        Iterator<ImageReader> imageReaders = getImageReaders(stream);
        if (!imageReaders.hasNext()) {
            return null;
        }

        ImageReader reader = imageReaders.next();
        reader.setInput(stream, false, true);
        BufferedImage res = reader.read(0);
        reader.dispose();

        try {
            stream.close();
        } catch (IOException e) {
            // Stream could be already closed, proceed silently in this case
        }
        
        return res;
    }

    public static boolean write(RenderedImage im,
                                String formatName,
                                ImageOutputStream output)
            throws IOException {

        if (im == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.53"));
        }
        if (formatName == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.4F"));
        }
        if (output == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.54"));
        }

        Iterator<ImageWriter> it = getImageWriters(ImageTypeSpecifier.createFromRenderedImage(im), formatName);
        if (it.hasNext()) {
            ImageWriter writer = it.next();
            writer.setOutput(output);
            writer.write(im);
            output.flush();
            writer.dispose();
            return true;
        }
        return false;
    }

    public static boolean write(RenderedImage im,
                                String formatName,
                                File output)
            throws IOException {

        if (output == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.54"));
        }

        if (output.exists()) {
            output.delete();
        }

        ImageOutputStream ios = createImageOutputStream(output);
        boolean rt = write(im, formatName, ios);
        ios.close();
        return rt;
    }

    public static boolean write(RenderedImage im,
                                String formatName,
                                OutputStream output)
            throws IOException {

        if (output == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.54"));
        }

        ImageOutputStream ios = createImageOutputStream(output);
        boolean rt = write(im, formatName, ios);
        ios.close();
        return rt;
    }


    /**
     * Filter to match spi by format name
     */
    static class FormatFilter implements ServiceRegistry.Filter {
        private String name;

        public FormatFilter(String name) {
            this.name = name;
        }

        public boolean filter(Object provider) {
            ImageReaderWriterSpi spi = (ImageReaderWriterSpi) provider;
            return Arrays.asList(spi.getFormatNames()).contains(name);
        }
    }

    /**
     * Filter to match spi by format name and encoding possibility
     */
    static class FormatAndEncodeFilter extends FormatFilter {

        private ImageTypeSpecifier type;

        public FormatAndEncodeFilter(ImageTypeSpecifier type, String name) {
            super(name);
            this.type = type;
        }

        @Override
        public boolean filter(Object provider) {
            ImageWriterSpi spi = (ImageWriterSpi) provider;
            return super.filter(provider) && spi.canEncodeImage(type);
        }
    }

    /**
     * Filter to match spi by suffix
     */
    static class SuffixFilter implements ServiceRegistry.Filter {
        private String suf;

        public SuffixFilter(String suf) {
            this.suf = suf;
        }

        public boolean filter(Object provider) {
            ImageReaderWriterSpi spi = (ImageReaderWriterSpi) provider;
            return Arrays.asList(spi.getFileSuffixes()).contains(suf);
        }
    }

    /**
     * Filter to match spi by MIMEType
     */
    static class MIMETypeFilter implements ServiceRegistry.Filter {
        private final String mimeType;

        public MIMETypeFilter(final String mimeType) {
            if (mimeType == null) {
                throw new NullPointerException(Messages.getString("imageio.55"));
            }
            
            this.mimeType = mimeType;
        }

        public boolean filter(final Object provider) {
            final String[] types = ((ImageReaderWriterSpi) provider).getMIMETypes();
            return (types != null) && Arrays.asList(types).contains(mimeType);
        }
    }

    /**
     * Filter to match spi by decoding possibility
     */
    static class CanReadFilter implements ServiceRegistry.Filter {
        private Object input;

        public CanReadFilter(Object input) {
            this.input = input;
        }

        public boolean filter(Object provider) {
            ImageReaderSpi spi = (ImageReaderSpi) provider;
            try {
                return spi.canDecodeInput(input);
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * Wraps Spi's iterator to ImageWriter iterator
     */
    static class SpiIteratorToWritersIteratorWrapper implements Iterator<ImageWriter> {

        private Iterator<ImageWriterSpi> backend;

        public SpiIteratorToWritersIteratorWrapper(Iterator<ImageWriterSpi> backend) {
            this.backend = backend;
        }

        public ImageWriter next() {
            try {
                return backend.next().createWriterInstance();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean hasNext() {
            return backend.hasNext();
        }

        public void remove() {
            throw new UnsupportedOperationException(Messages.getString("imageio.56"));
        }
    }

    /**
     * Wraps spi's iterator to ImageReader iterator
     */
    static class SpiIteratorToReadersIteratorWrapper implements Iterator<ImageReader> {
        private Iterator<ImageReaderSpi> backend;

        public SpiIteratorToReadersIteratorWrapper(Iterator<ImageReaderSpi> backend) {
            this.backend = backend;
        }

        public ImageReader next() {
            try {
                return backend.next().createReaderInstance();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean hasNext() {
            return backend.hasNext();
        }

        public void remove() {
            throw new UnsupportedOperationException(Messages.getString("imageio.56"));
        }
    }
}
