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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Convertor from {@link java.awt.datatransfer.Transferable} to 
 * {@link org.apache.harmony.awt.datatransfer.DataProvider}
 */
public class DataSource implements DataProvider {

    // Cached data from transferable object
    private DataFlavor[] flavors;
    private List<String> nativeFormats;
    
    protected final Transferable contents;
    
    public DataSource(Transferable contents) {
        this.contents = contents;
    }

    private boolean isHtmlFlavor(DataFlavor f) {
        return "html".equalsIgnoreCase(f.getSubType()); //$NON-NLS-1$
    }
    
    protected DataFlavor[] getDataFlavors() {
        if (flavors == null) {
            flavors = contents.getTransferDataFlavors();
        }
        return flavors;
    }
    
    public String[] getNativeFormats() {
        return getNativeFormatsList().toArray(new String[0]);
    }
    
    public List<String> getNativeFormatsList() {
        if (nativeFormats == null) {
            DataFlavor[] flavors = getDataFlavors();
            nativeFormats = getNativesForFlavors(flavors);
        }

        return nativeFormats;
    }
    
    private static List<String> getNativesForFlavors(DataFlavor[] flavors) {
        ArrayList<String> natives = new ArrayList<String>();
        
        SystemFlavorMap flavorMap = 
            (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();
        
        for (int i = 0; i < flavors.length; i++) {
            List<String> list = flavorMap.getNativesForFlavor(flavors[i]);
            for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
                String nativeFormat = it.next();
                if (!natives.contains(nativeFormat)) {
                    natives.add(nativeFormat);
                }
            }
        }
        return natives;
    }
    
    private String getTextFromReader(Reader r) throws IOException {
        StringBuilder buffer = new StringBuilder();
        char chunk[] = new char[1024];
        int len;
        while ((len = r.read(chunk)) > 0) {
            buffer.append(chunk, 0, len);
        }
        return buffer.toString();
    }
    
    private String getText(boolean htmlOnly) {
        DataFlavor[] flavors = contents.getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            DataFlavor f = flavors[i];
            if (!f.isFlavorTextType()) {
                continue;
            }
            if (htmlOnly && !isHtmlFlavor(f)) {
                continue;
            }
            try {
                if (String.class.isAssignableFrom(
                        f.getRepresentationClass())) {
                    return (String)contents.getTransferData(f);
                }
                Reader r = f.getReaderForText(contents);
                return getTextFromReader(r);
            } catch (Exception e) {}
        }
        return null;
    }

    public String getText() {
        return getText(false);
    }

    public String[] getFileList() {
        try {
            List<?> list = (List<?>) contents.getTransferData(DataFlavor.javaFileListFlavor);
            return list.toArray(new String[list.size()]);
        } catch (Exception e) {
            return null;
        }
    }

    public String getURL() {
        try {
            URL url = (URL)contents.getTransferData(urlFlavor);
            return url.toString();
        } catch (Exception e) {}
        try {
            URL url = (URL)contents.getTransferData(uriFlavor);
            return url.toString();
        } catch (Exception e) {}
        try {
            URL url = new URL(getText());
            return url.toString();
        } catch (Exception e) {}
        return null;
    }

    public String getHTML() {
        return getText(true);
    }
    
    public RawBitmap getRawBitmap() {
        DataFlavor[] flavors = contents.getTransferDataFlavors();

        for (int i = 0; i < flavors.length; i++) {
            DataFlavor f = flavors[i];
            Class<?> c = f.getRepresentationClass();
            if (c != null && Image.class.isAssignableFrom(c) && 
                    (f.isMimeTypeEqual(DataFlavor.imageFlavor) || 
                            f.isFlavorSerializedObjectType())) {
                try {
                    Image im = (Image)contents.getTransferData(f);
                    return getImageBitmap(im);
                } catch (Throwable ex) {
                    continue;
                }
            }
        }
        return null;
    }
    
    private RawBitmap getImageBitmap(Image im) {
        if (im instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage)im;
            if (bi.getType() == BufferedImage.TYPE_INT_RGB) {
                return getImageBitmap32(bi);
            }
        }
        int width = im.getWidth(null);
        int height = im.getHeight(null);
        if (width <= 0 || height <= 0) {
            return null;
        }
        BufferedImage bi = 
            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics gr = bi.getGraphics();
        gr.drawImage(im, 0, 0, null);
        gr.dispose();
        return getImageBitmap32(bi);
    }

    private RawBitmap getImageBitmap32(BufferedImage bi) {
        int buffer[] = new int[bi.getWidth() * bi.getHeight()];
        DataBufferInt data = (DataBufferInt)bi.getRaster().getDataBuffer();
        int bufferPos = 0;
        int bankCount = data.getNumBanks();
        int offsets[] = data.getOffsets();
        for (int i = 0; i < bankCount; i++) {
            int[] fragment = data.getData(i);
            System.arraycopy(fragment, offsets[i], buffer, bufferPos, 
                    fragment.length - offsets[i]);
            bufferPos += fragment.length - offsets[i];
        }
        return new RawBitmap(bi.getWidth(), bi.getHeight(), bi.getWidth(), 
                32, 0xFF0000, 0xFF00, 0xFF, buffer);
    }

    public byte[] getSerializedObject(Class<?> clazz) {
        try {
            DataFlavor f = new DataFlavor(clazz, null);
            Serializable s = (Serializable)contents.getTransferData(f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new ObjectOutputStream(bytes).writeObject(s);
            return bytes.toByteArray();
        } catch (Throwable e) {
            return null;
        }
    }

    public boolean isNativeFormatAvailable(String nativeFormat) {
        return getNativeFormatsList().contains(nativeFormat);
    }
}
