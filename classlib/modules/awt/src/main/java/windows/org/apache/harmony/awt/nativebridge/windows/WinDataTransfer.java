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
package org.apache.harmony.awt.nativebridge.windows;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;

import org.apache.harmony.awt.datatransfer.DataProvider;
import org.apache.harmony.awt.datatransfer.DataSnapshot;
import org.apache.harmony.awt.datatransfer.RawBitmap;
import org.apache.harmony.awt.datatransfer.windows.WinDragSource;
import org.apache.harmony.awt.datatransfer.windows.WinDropTarget;
import org.apache.harmony.awt.internal.nls.Messages;

/**
 * Native support for data transfer on Windows
 */
public final class WinDataTransfer {
    
    static {
        org.apache.harmony.awt.Utils.loadLibrary("Win32Wrapper"); //$NON-NLS-1$
    }

    /**
     * Wrapper for OLE interface IDataObject
     */
    public static class IDataObject implements DataProvider {
        
        /**
         * pointer to IDataObject interface
         */
        public final long pointer;
        
        public IDataObject(long p) {
            if (p == 0) {
                // awt.1D=Cannot get data from OLE clipboard
                throw new RuntimeException(Messages.getString("awt.1D")); //$NON-NLS-1$
            }
            pointer = p;
        }
        
        public String getText() {
            return getDataObjectText(pointer);
        }
        
        public String[] getFileList() {
            return getDataObjectFileList(pointer);
        }
        
        public String getURL() {
            return getDataObjectURL(pointer);
        }

        public String getHTML() {
            return getDataObjectHTML(pointer);
        }
        
        public RawBitmap getRawBitmap() {
            int header[] = new int[7];
            Object buffer = getDataObjectImage(pointer, header);
            if (buffer == null) {
                return null;
            }
            return new RawBitmap(header, buffer);
        }
        
        public String[] getNativeFormats() {
            return getDataObjectFormats(pointer);
        }

        public boolean isNativeFormatAvailable(String nativeFormat) {
            return isDataObjectFormatAvailable(pointer, nativeFormat);
        }
        
        public byte[] getSerializedObject(Class<?> clazz) {
            String nativeFormat = SystemFlavorMap.encodeDataFlavor(
                    new DataFlavor(clazz, null));
            return getDataObjectSerialized(pointer, nativeFormat);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IDataObject) {
                return pointer == ((IDataObject)obj).pointer;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return (int)pointer;
        }

        public void release() {
        }
    }

    public static native void init();

    private static native String getDataObjectText(long pointer);

    private static native String[] getDataObjectFileList(long pointer);

    private static native String getDataObjectURL(long pointer);

    private static native String getDataObjectHTML(long pointer);

    /**
     * Get bitmap bits and dimension from data object 
     * @param pointer - pointer to IDataObject interface
     * @param header - array of output values, representing bitmap 
     * parameters in the format:
     *  { width, height, stride, bitCount, redMask, greenMask, blueMask }
     *  
     * @return bitmap bits in form of int[], short[] or byte[],
     * or null in case of failure
     */
    private static native Object getDataObjectImage(long pointer, int[] header);

    private static native byte[] getDataObjectSerialized(
                                                long pointer,
                                                String nativeFormat);

    public static native String getSystemDefaultCharset();

    private static native String[] getDataObjectFormats(long pointer);

    private static native boolean isDataObjectFormatAvailable(
                                                long pointer,
                                                String nativeFormat);

    private static native long getOleClipboardDataObject();

    private static native void releaseDataObject(long pointer);

    public static IDataObject getClipboardContents() {
        long pointer = getOleClipboardDataObject();
        return pointer != 0 ? new IDataObject(pointer) : null;
    }

    public static native void setClipboardContents(DataSnapshot snapshot);

    /**
     * Perform OLE drag-and-drop, wait until the operation is complete.
     */
    public static native void startDrag(DataSnapshot snapshot,
                                       WinDragSource dragSource,
                                       int sourceActions);
    
    public static native long registerDropTarget(long hwnd, 
                                                 WinDropTarget target);
    
    public static native void revokeDropTarget(long hwnd, long target);
}
