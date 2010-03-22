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
 * @author Michael Danilov
 */
package org.apache.harmony.awt.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.dnd.peer.DropTargetContextPeer;
import java.nio.charset.Charset;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.misc.SystemUtils;

/**
 * Data transfer ToolKit.
 * Unites context-dependent and platform-dependent information of data transferring subsystem.
 */
public abstract class DTK {

    private NativeClipboard nativeClipboard = null;
    private NativeClipboard nativeSelection = null;

    protected SystemFlavorMap systemFlavorMap;

    protected final DataTransferThread dataTransferThread;

    protected DTK() {
        dataTransferThread = new DataTransferThread(this);
        dataTransferThread.start();
    }
    /**
     * Returns data transfer toolkit for current application context.
     */
    public static DTK getDTK() {
        synchronized(ContextStorage.getContextLock()) {
            if (ContextStorage.shutdownPending()) {
                return null;
            }

            DTK instance = ContextStorage.getDTK();

            if (instance == null) {
                instance = createDTK();
                ContextStorage.setDTK(instance);
            }

            return instance;
        }
    }

    /**
     * Returns system flavor map for current application context.
     * For use from SystemFlavorMap.getDefaultFlavorMap() only
     */
    public synchronized SystemFlavorMap getSystemFlavorMap() {
        return systemFlavorMap;
    }

    /**
     * Sets system flavor map for current application context.
     * For use from SystemFlavorMap.getDefaultFlavorMap() only.
     */
    public synchronized void setSystemFlavorMap(SystemFlavorMap newFlavorMap) {
        this.systemFlavorMap = newFlavorMap;
    }

    /**
     * Returns native clipboard for current application context.
     */
    public NativeClipboard getNativeClipboard() {
        if (nativeClipboard == null) {
            nativeClipboard = newNativeClipboard();
        }

        return nativeClipboard;
    }

    /**
     * Returns native selection for current application context.
     */
    public NativeClipboard getNativeSelection() {
        if (nativeSelection == null) {
            nativeSelection = newNativeSelection();
        }

        return nativeSelection;
    }

    /**
     * Creates native clipboard for current native platform.
     */
    protected abstract NativeClipboard newNativeClipboard();

    /**
     * Creates native selection for current native platform.
     */
    protected abstract NativeClipboard newNativeSelection();
    
    public abstract void initDragAndDrop();
    
    public abstract void runEventLoop();

    public abstract DropTargetContextPeer createDropTargetContextPeer(
            DropTargetContext context);

    public abstract DragSourceContextPeer createDragSourceContextPeer(
            DragGestureEvent dge);
    
    private static DTK createDTK() {
        String name;
        switch (SystemUtils.getOS()) {
        case SystemUtils.OS_WINDOWS:
            name = "org.apache.harmony.awt.datatransfer.windows.WinDTK"; //$NON-NLS-1$
            break;
        case SystemUtils.OS_LINUX:
        case SystemUtils.OS_FREEBSD:
            name = "org.apache.harmony.awt.datatransfer.linux.LinuxDTK"; //$NON-NLS-1$
            break;
        default:
            // awt.4E=Unknown native platform.
            throw new RuntimeException(Messages.getString("awt.4E")); //$NON-NLS-1$
        }
        try {
            DTK dtk = (DTK) Class.forName(name).newInstance();
            return dtk;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public String getDefaultCharset() {
        return "unicode"; //$NON-NLS-1$
    }

    protected String[] getCharsets() {
        return new String[] { 
                "UTF-16", "UTF-8", "unicode", "ISO-8859-1", "US-ASCII" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    public void initSystemFlavorMap(SystemFlavorMap fm) {
        String[] charsets = getCharsets();
        
        appendSystemFlavorMap(fm,
                DataFlavor.stringFlavor, 
                DataProvider.FORMAT_TEXT);
        appendSystemFlavorMap(fm,
                charsets, "plain", //$NON-NLS-1$
                DataProvider.FORMAT_TEXT);

        appendSystemFlavorMap(fm,
                charsets, "html", //$NON-NLS-1$
                DataProvider.FORMAT_HTML);
        
        appendSystemFlavorMap(fm,
                DataProvider.urlFlavor, 
                DataProvider.FORMAT_URL);
        appendSystemFlavorMap(fm,
                charsets, "uri-list", //$NON-NLS-1$
                DataProvider.FORMAT_URL);
        
        appendSystemFlavorMap(fm,
                DataFlavor.javaFileListFlavor, 
                DataProvider.FORMAT_FILE_LIST);

        appendSystemFlavorMap(fm,
                DataFlavor.imageFlavor, 
                DataProvider.FORMAT_IMAGE);
    }

    protected void appendSystemFlavorMap(SystemFlavorMap fm, 
                                         DataFlavor flav,
                                         String nat) {
        fm.addFlavorForUnencodedNative(nat, flav);
        fm.addUnencodedNativeForFlavor(flav, nat);
    }

    protected void appendSystemFlavorMap(SystemFlavorMap fm,
                                         String[] charsets,
                                         String subType,
                                         String nat) {
        TextFlavor.addUnicodeClasses(fm, nat, subType);
        for (int i = 0; i < charsets.length; i++) {
            if (charsets[i] != null && Charset.isSupported(charsets[i])) {
                TextFlavor.addCharsetClasses(fm, nat, subType, charsets[i]);
            }
        }
    }
}
