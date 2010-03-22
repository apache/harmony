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
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt.datatransfer;

import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.awt.internal.nls.Messages;

/**
 * Wrapper for native data
 */
public final class DataProxy implements Transferable {
    
    public static final Class<?>[] unicodeTextClasses = 
            { String.class, Reader.class, CharBuffer.class, char[].class }; 
    public static final Class<?>[] charsetTextClasses = 
              { byte[].class, ByteBuffer.class, InputStream.class };
    
    private final DataProvider data;
    private final SystemFlavorMap flavorMap;
    
    public DataProxy(DataProvider data) {
        this.data = data;
        this.flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();
    }
    
    public DataProvider getDataProvider() {
        return data;
    }
    
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        
        String mimeType = flavor.getPrimaryType() + "/" + flavor.getSubType(); //$NON-NLS-1$
        if (flavor.isFlavorTextType()) {
            if (mimeType.equalsIgnoreCase(DataProvider.TYPE_HTML)) {
                return getHTML(flavor);
            }
            if (mimeType.equalsIgnoreCase(DataProvider.TYPE_URILIST)) {
                return getURL(flavor);
            }
            return getPlainText(flavor);
        }
        if (flavor.isFlavorJavaFileListType()) {
            return getFileList(flavor);
        }
        if (flavor.isFlavorSerializedObjectType()) {
            return getSerializedObject(flavor);
        }
        if (flavor.equals(DataProvider.urlFlavor)) {
            return getURL(flavor);
        }
        if (mimeType.equalsIgnoreCase(DataProvider.TYPE_IMAGE) && 
                Image.class.isAssignableFrom(flavor.getRepresentationClass())) {
            return getImage(flavor);
        }
        
        throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        ArrayList<DataFlavor> result = new ArrayList<DataFlavor>();
        String[] natives = data.getNativeFormats();
        
        for (int i = 0; i < natives.length; i++) {
            List<DataFlavor> flavors = flavorMap.getFlavorsForNative(natives[i]);
            for (Iterator<DataFlavor> it = flavors.iterator(); it.hasNext(); ) {
                DataFlavor f = it.next();
                if (!result.contains(f)) {
                    result.add(f);
                }
            }
        }
        return result.toArray(new DataFlavor[result.size()]);
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        DataFlavor[] flavors = getTransferDataFlavors();
        for (int i=0; i<flavors.length; i++) {
            if (flavors[i].equals(flavor)) {
                return true;
            }
        }
        return false;
    }
    
    private Object getPlainText(DataFlavor f)
            throws IOException, UnsupportedFlavorException {
        if (!data.isNativeFormatAvailable(DataProvider.FORMAT_TEXT)) {
            throw new UnsupportedFlavorException(f);
        }
        String str = data.getText();
        if (str == null) {
            // awt.4F=Data is not available
            throw new IOException(Messages.getString("awt.4F")); //$NON-NLS-1$
        }
        return getTextRepresentation(str, f);
    }

    private Object getFileList(DataFlavor f) 
            throws IOException, UnsupportedFlavorException {
        if (!data.isNativeFormatAvailable(DataProvider.FORMAT_FILE_LIST)) {
            throw new UnsupportedFlavorException(f);
        }
        String[] files = data.getFileList();
        if (files == null) {
            // awt.4F=Data is not available
            throw new IOException(Messages.getString("awt.4F")); //$NON-NLS-1$
        }
        return Arrays.asList(files);
    }

    private Object getHTML(DataFlavor f)
            throws IOException, UnsupportedFlavorException {
        if (!data.isNativeFormatAvailable(DataProvider.FORMAT_HTML)) {
            throw new UnsupportedFlavorException(f);
        }
        String str = data.getHTML();
        if (str == null) {
            // awt.4F=Data is not available
            throw new IOException(Messages.getString("awt.4F")); //$NON-NLS-1$
        }
        return getTextRepresentation(str, f);
    }

    private Object getURL(DataFlavor f)
            throws IOException, UnsupportedFlavorException {
        if (!data.isNativeFormatAvailable(DataProvider.FORMAT_URL)) {
            throw new UnsupportedFlavorException(f);
        }
        String str = data.getURL();
        if (str == null) {
            // awt.4F=Data is not available
            throw new IOException(Messages.getString("awt.4F")); //$NON-NLS-1$
        }
        URL url = new URL(str);
        if (f.getRepresentationClass().isAssignableFrom(URL.class)) {
            return url;
        }
        if (f.isFlavorTextType()) {
            return getTextRepresentation(url.toString(), f);
        }
        throw new UnsupportedFlavorException(f);
    }
    
    private Object getSerializedObject(DataFlavor f)
            throws IOException, UnsupportedFlavorException {
        String nativeFormat = SystemFlavorMap.encodeDataFlavor(f);
        if ((nativeFormat == null) || 
                !data.isNativeFormatAvailable(nativeFormat)) {
            throw new UnsupportedFlavorException(f);
        }
        byte bytes[] = data.getSerializedObject(f.getRepresentationClass());
        if (bytes == null) {
            // awt.4F=Data is not available
            throw new IOException(Messages.getString("awt.4F")); //$NON-NLS-1$
        }
        ByteArrayInputStream str = new ByteArrayInputStream(bytes);
        try {
            return new ObjectInputStream(str).readObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage());
        }
    }
    
    private String getCharset(DataFlavor f) {
        return f.getParameter("charset"); //$NON-NLS-1$
    }

    private Object getTextRepresentation(String text, DataFlavor f)
            throws UnsupportedFlavorException, IOException {
        if (f.getRepresentationClass() == String.class) {
            return text;
        }
        if (f.isRepresentationClassReader()) {
            return new StringReader(text);
        }
        if (f.isRepresentationClassCharBuffer()) {
            return CharBuffer.wrap(text);
        }
        if (f.getRepresentationClass() == char[].class) {
            char[] chars = new char[text.length()];
            text.getChars(0, text.length(), chars, 0);
            return chars;
        }
        String charset = getCharset(f);
        if (f.getRepresentationClass() == byte[].class) {
            byte[] bytes = text.getBytes(charset);
            return bytes;
        }
        if (f.isRepresentationClassByteBuffer()) {
            byte[] bytes = text.getBytes(charset);
            return ByteBuffer.wrap(bytes);
        }
        if (f.isRepresentationClassInputStream()) {
            byte[] bytes = text.getBytes(charset);
            return new ByteArrayInputStream(bytes);
        }
        throw new UnsupportedFlavorException(f);
    }

    private Image getImage(DataFlavor f) 
            throws IOException, UnsupportedFlavorException {
        if (!data.isNativeFormatAvailable(DataProvider.FORMAT_IMAGE)) {
            throw new UnsupportedFlavorException(f);
        }
        RawBitmap bitmap = data.getRawBitmap();
        if (bitmap == null) {
            // awt.4F=Data is not available
            throw new IOException(Messages.getString("awt.4F")); //$NON-NLS-1$
        }
        return createBufferedImage(bitmap);
    }
    
    private boolean isRGB(RawBitmap b) {
        return b.rMask == 0xFF0000 && b.gMask == 0xFF00 && b.bMask == 0xFF;
    }
    
    private boolean isBGR(RawBitmap b) {
        return b.rMask == 0xFF && b.gMask == 0xFF00 && b.bMask == 0xFF0000;
    }
    
    private BufferedImage createBufferedImage(RawBitmap b) {
        if (b == null || b.buffer == null
                || b.width <= 0 || b.height <= 0) {
            return null;
        }
        
        ColorModel cm = null;
        WritableRaster wr = null;

        if (b.bits == 32 && b.buffer instanceof int[]) {
            if (!isRGB(b) && !isBGR(b)) {
                return null;
            }
            int masks[] = { b.rMask, b.gMask, b.bMask };
            int buffer[] = (int [])b.buffer;
            cm = new DirectColorModel(24, b.rMask, b.gMask, b.bMask);
            wr = Raster.createPackedRaster(
                    new DataBufferInt(buffer, buffer.length), 
                    b.width, b.height, b.stride,
                    masks, null);

        } else  if (b.bits == 24 && b.buffer instanceof byte[]) {
            int bits[] = { 8, 8, 8 };
            int offsets[];
            if (isRGB(b)) {
                offsets = new int[] { 0, 1, 2 };
            } else if (isBGR(b)) {
                offsets = new int[] { 2, 1, 0 };
            } else {
                return null;
            }
            byte buffer[] = (byte [])b.buffer;
            cm = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    bits, false, false, 
                    Transparency.OPAQUE, 
                    DataBuffer.TYPE_BYTE);

            wr = Raster.createInterleavedRaster(
                    new DataBufferByte(buffer, buffer.length),
                    b.width, b.height, b.stride, 3, offsets, null);

        } else if ((b.bits == 16 || b.bits == 15)
                && b.buffer instanceof short[]) {
            int masks[] = { b.rMask, b.gMask, b.bMask };
            short buffer[] = (short [])b.buffer;
            cm = new DirectColorModel(b.bits, b.rMask, b.gMask, b.bMask);
            wr = Raster.createPackedRaster(
                    new DataBufferUShort(buffer, buffer.length), 
                    b.width, b.height, b.stride,
                    masks, null);
        }
        
        if (cm == null || wr == null) {
            return null;
        }
        return new BufferedImage(cm, wr, false, null);
    }
}

