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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable copy of transferable data, 
 * safe for use across the threads
 */
public class DataSnapshot implements DataProvider {
    
    private final String text;
    private final String[] fileList;
    private final String url;
    private final String html;
    private final RawBitmap rawBitmap;
    
    private final String[] nativeFormats;
    /** Class -> byte[] */
    private final Map<Class<?>, byte[]> serializedObjects;
    
    /**
     * @param dataObject
     */
    public DataSnapshot(DataProvider data) {
        nativeFormats = data.getNativeFormats();
        text = data.getText();
        fileList = data.getFileList();
        url = data.getURL();
        html = data.getHTML();
        rawBitmap = data.getRawBitmap();
        
        serializedObjects = Collections.synchronizedMap(new HashMap<Class<?>, byte[]>());
        
        for (int i = 0; i < nativeFormats.length; i++) {
            DataFlavor df = null;
            try {
                df = SystemFlavorMap.decodeDataFlavor(nativeFormats[i]);
            } catch (ClassNotFoundException e) {}
            if (df != null) {
                Class<?> clazz = df.getRepresentationClass();
                byte[] bytes = data.getSerializedObject(clazz);
                if (bytes != null) {
                    serializedObjects.put(clazz, bytes);
                }
            }
        }
        // TODO: refine the list of native formats
    }

    public boolean isNativeFormatAvailable(String nativeFormat) {
        if (nativeFormat == null) {
            return false;
        }
        if (nativeFormat.equals(FORMAT_TEXT)) {
            return (text != null);
        }
        if (nativeFormat.equals(FORMAT_FILE_LIST)) {
            return (fileList != null);
        }
        if (nativeFormat.equals(FORMAT_URL)) {
            return (url != null);
        }
        if (nativeFormat.equals(FORMAT_HTML)) {
            return (html != null);
        }
        if (nativeFormat.equals(FORMAT_IMAGE)) {
            return (rawBitmap != null);
        }
        try {
            DataFlavor df = SystemFlavorMap.decodeDataFlavor(nativeFormat);
            return serializedObjects.containsKey(df.getRepresentationClass());
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getText() {
        return text;
    }
    
    public String[] getFileList() {
        return fileList;
    }
    
    public String getURL() {
        return url;
    }
    
    public String getHTML() {
        return html;
    }
    
    public RawBitmap getRawBitmap() {
        return rawBitmap;
    }
    
    public int[] getRawBitmapHeader() {
        return (rawBitmap != null) ? rawBitmap.getHeader() : null;
    }
    
    public byte[] getRawBitmapBuffer8() {
        return (rawBitmap != null) && (rawBitmap.buffer instanceof byte[]) ?
                (byte[])rawBitmap.buffer : null;
    }
    
    public short[] getRawBitmapBuffer16() {
        return (rawBitmap != null) && (rawBitmap.buffer instanceof short[]) ?
                (short[])rawBitmap.buffer : null;
    }
    
    public int[] getRawBitmapBuffer32() {
        return (rawBitmap != null) && (rawBitmap.buffer instanceof int[]) ?
                (int[])rawBitmap.buffer : null;
    }
    
    public byte[] getSerializedObject(Class<?> clazz) {
        return serializedObjects.get(clazz);
    }

    public byte[] getSerializedObject(String nativeFormat) {
        try {
            DataFlavor df = SystemFlavorMap.decodeDataFlavor(nativeFormat);
            return getSerializedObject(df.getRepresentationClass());
        } catch (Exception e) {
            return null;
        }
    }

    public String[] getNativeFormats() {
        return nativeFormats;
    }
}
