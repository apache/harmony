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
 * @author Michael Danilov, Pavel Dolgov
 */
package org.apache.harmony.awt.datatransfer.windows;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.datatransfer.DataProxy;
import org.apache.harmony.awt.datatransfer.DataSnapshot;
import org.apache.harmony.awt.datatransfer.DataSource;
import org.apache.harmony.awt.datatransfer.NativeClipboard;
import org.apache.harmony.awt.nativebridge.windows.WinDataTransfer;
import org.apache.harmony.awt.wtk.windows.WinEventQueue;

public final class WinClipboard extends NativeClipboard
        implements WinEventQueue.Preprocessor {

    private final WinEventQueue winEventQueue;
    
    public WinClipboard() {
        super("System"); //$NON-NLS-1$
        winEventQueue = ((WinEventQueue) ContextStorage.getNativeEventQueue());
        winEventQueue.addPreprocessor(this);
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onRestart() {
    }

    @Override
    public void setContents(Transferable contents, ClipboardOwner owner) {
        DataSource dc = new DataSource(contents);
        final DataSnapshot snapshot = new DataSnapshot(dc);

        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                WinDataTransfer.setClipboardContents(snapshot);
            }
        };
        winEventQueue.performTask(task);
        // TODO: fire flavor change events
    }

    @Override
    public Object getData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        return getContents(this).getTransferData(flavor);
    }

    @Override
    public Transferable getContents(Object requestor) {
        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                WinDataTransfer.IDataObject dataObject = 
                        WinDataTransfer.getClipboardContents();
                DataSnapshot snapshot = new DataSnapshot(dataObject);
                dataObject.release();
                returnValue = new DataProxy(snapshot);
            }
        };
        winEventQueue.performTask(task);
        return (DataProxy)task.returnValue;
    }
    
    @Override
    public DataFlavor[] getAvailableDataFlavors() {
        Transferable t = getContents(this);
        return (t != null) ? 
                t.getTransferDataFlavors() : new DataFlavor[0];
    }

    public boolean preprocess(long hwnd, int msg, 
                              long wParam, long lParam, 
                              long[] result) {
        // TODO: track changes in Windows clipboard
        return false;
    }

}
