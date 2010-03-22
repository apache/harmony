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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.theme.windows;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.Int16Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.VoidPointer;
import org.apache.harmony.awt.nativebridge.windows.Callback;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.Callback.Handler;


public class WinFileDialog extends WinStyle {
    
    // windows defs from CommDlg.h
    private static final int CDN_FIRST = -601;
    private static final int CDN_INCLUDEITEM = CDN_FIRST - 0x0007;
    
    private static class OFNHookHandler implements Handler {
        
        public long windowProc(long hwnd, int msg, long wParam, long lParam) {
            FileDialog fd = thread2fd.get(Thread.currentThread());
            if (fd == null) {
                return 0l;
            }
            FilenameFilter ff = fd.getFilenameFilter();
            
            if (msg == WM_INITDIALOG) {
                WinFileDialog.getInstance(fd).hwnd = win32.GetParent(hwnd);
            }
            if (msg != WM_NOTIFY) {
                return 0l;
            }            
            if (win32.createNMHDR(lParam).get_code() != CDN_INCLUDEITEM) {
                return 0l;
            }
            
            if (ff == null) {
                return 1l;
            }
            Win32.OFNOTIFYEXW ofnotify = win32.createOFNOTIFYEXW(lParam);
            Win32.IShellFolder psf = getIShellFolder(ofnotify.get_psf());
            
            int flags = (WindowsConsts.SHGDN_FORPARSING);            
            Win32.ITEMIDLIST item = win32.createITEMIDLIST(ofnotify.get_pidl());
            String fullName = getDisplayNameOf(item, psf, flags);
            File file = new File(fullName);
            String fName = file.getName();
            boolean res = true;
            if (!"".equals(fName)) { //$NON-NLS-1$
                res = ff.accept(file.getParentFile(), fName);
            }            
            return (res ? 1 : 0);
        }
        
        private Win32.IShellFolder getIShellFolder(VoidPointer ptr) {
            PointerPointer ptrPtr = nb.createPointerPointer(ptr, false);
            return win32.createIShellFolder(ptrPtr.getAddress(0));
        }
        
        private String getDisplayNameOf(Win32.ITEMIDLIST item, 
                                        Win32.IShellFolder parent,
                                        int flags) {
            
            Win32.STRRET strret = win32.createSTRRET(false);
            parent.GetDisplayNameOf(item, flags, strret);
            int bufLen = 2048;            
            Int16Pointer bufPtr = nb.createInt16Pointer(bufLen, false);
            win32.StrRetToBufW(strret, null, bufPtr, bufLen);
            return bufPtr.getString();
        }
        
    }
    
    private static final Win32 win32 = Win32.getInstance();
    private static final NativeBridge nb = NativeBridge.getInstance();
    private static final ComponentInternals ci = ComponentInternals.
    getComponentInternals();
    private static final Map<Thread, FileDialog> thread2fd = new HashMap<Thread, FileDialog>();
    private static final Map<FileDialog, WinFileDialog> fd2win = new HashMap<FileDialog, WinFileDialog>();
    private static final OFNHookHandler handler = new OFNHookHandler();
    private static final long ofnHookPtr = Callback.registerCallbackOFN(handler);
    private final FileDialog fileDialog;
    private final boolean modal;
    private final Win32.OPENFILENAMEW ofn;
    private long hwnd;
    
    public static WinFileDialog getInstance(FileDialog fd) {
        return fd2win.get(fd);
    }
    
    public WinFileDialog(FileDialog fd) {
        fileDialog = fd;
        fd2win.put(fd, this);
        modal = fd.isModal();
        ofn = win32.createOPENFILENAMEW(false);
        ofn.set_lStructSize(ofn.size());        
    }
    
    private void show(int mode) {
        synchronized (handler) {
            if (!fileDialog.isDisplayable()) {
                // make displayable together with owner
                fileDialog.addNotify();
            }
            ci.setVisibleFlag(fileDialog, true);
            postEvent(new ComponentEvent(fileDialog, 
                                         ComponentEvent.COMPONENT_SHOWN));
            initOFN();
            boolean ok = false;
            Thread thread = Thread.currentThread();
            thread2fd.put(thread, fileDialog);

            switch (mode) {
            case FileDialog.LOAD:
                ok = (win32.GetOpenFileNameW(ofn) != 0);
                break;
            case FileDialog.SAVE:
                ok = (win32.GetSaveFileNameW(ofn) != 0);
                break;
            default:
                return;

            }
            setValues(ok);
            thread2fd.remove(thread);
            fd2win.remove(fileDialog);
            ci.setVisibleFlag(fileDialog, false);
            postEvent(new ComponentEvent(fileDialog,
                    ComponentEvent.COMPONENT_HIDDEN));
        }
    }

    private void setValues(boolean ok) {
        String error = null;
        if (ok) {
            String fullName = ofn.get_lpstrFile().getString();
            File file = new File(fullName);
            fileDialog.setFile(ofn.get_lpstrFileTitle().getString());
            fileDialog.setDirectory(file.getParent() + File.separator);

        } else {
            fileDialog.setFile(null);
            fileDialog.setDirectory(null);
            int code = win32.CommDlgExtendedError();
            if (code != 0) {
                error = getExtendedError(code);
            }
        }
        if (error != null) {
            // awt.err.00=file dialog {0} error!
            System.err.println(Messages.getString("awt.err.00", error )); //$NON-NLS-1$
        }
    }

    private void initOFN() {        
        setOwner(fileDialog.getOwner());
        
        setFile(fileDialog.getFile());
        setDir(fileDialog.getDirectory());        
        setTitle(fileDialog.getTitle());
        setFilter("All Files (*.*)"); //$NON-NLS-1$
        
        ofn.set_Flags(OFN_ENABLEHOOK | OFN_ENABLEINCLUDENOTIFY | OFN_EXPLORER |
                      OFN_ENABLESIZING);
        ofn.set_lpfnHook(ofnHookPtr);
    }

    private void setOwner(Window w) {
        if ((w == null) || !w.isDisplayable()) {
            return;
        }
        // this also makes file dialog modal:
        ofn.set_hwndOwner(ci.getNativeWindow(w).getId());
    }

    private void setFilter(String filter) {
        if (filter == null) {
            return;
        }
        ofn.set_lpstrFilter(nb.createInt16Pointer(filter, false));
    }

    private void setTitle(String title) {
        if (title == null) {
            return;
        }
        ofn.set_lpstrTitle(nb.createInt16Pointer(title, false));
    }

    private void setDir(String dirName) {
        if (dirName == null) {
            dirName = org.apache.harmony.awt.Utils.getSystemProperty("user.dir"); //$NON-NLS-1$
        }
        ofn.set_lpstrInitialDir(nb.createInt16Pointer(dirName, false));
    }

    private void setFile(String fileName) {
        int bufSize = 255;
        Int16Pointer bufferPtr = nb.createInt16Pointer(bufSize, false);
        if (fileName != null) {
            bufferPtr.setString(fileName);
        }
        ofn.set_nMaxFileTitle(bufSize);
        ofn.set_lpstrFileTitle(nb.createInt16Pointer(bufSize, false));
        ofn.set_lpstrFile(bufferPtr);
        ofn.set_nMaxFile(bufSize); // mandatory!
    }
    
    public boolean show() {        
        if (modal) {
            if (EventQueue.isDispatchThread()) {
                // need to continue dispatching events
                // so start inner modal loop:
                new Thread() {
                    @Override
                    public void run() {
                        show(fileDialog.getMode());
                        ci.endModalLoop(fileDialog);
                    }
                }.start();
                ci.runModalLoop(fileDialog);
            } else {
                // just block the calling thread:
                show(fileDialog.getMode());
            }
        } else {
            // start new thread here and
            // return immediately(return value is useless)
            new Thread() {
                @Override
                public void run() {
                    show(fileDialog.getMode());
                }
            }.start();
        }
        
        return false; // don't call super(Dialog).show()
    }

    private String getExtendedError(int code) {
        switch (code) {
        case CDERR_GENERALCODES:
            return "general"; //$NON-NLS-1$
        case CDERR_STRUCTSIZE:
            return "structure size"; //$NON-NLS-1$
        case CDERR_INITIALIZATION:
            return "init"; //$NON-NLS-1$
        case CDERR_NOTEMPLATE:
            return "no template"; //$NON-NLS-1$
        case CDERR_NOHINSTANCE:
            return "no hInstance"; //$NON-NLS-1$
        case CDERR_LOADSTRFAILURE:
            return "load string failure"; //$NON-NLS-1$
        case CDERR_FINDRESFAILURE:
            return "find resource failure"; //$NON-NLS-1$
        case CDERR_LOADRESFAILURE:
            return "load resource failure"; //$NON-NLS-1$
        case CDERR_LOCKRESFAILURE:
            return "lock resource failure"; //$NON-NLS-1$
        case CDERR_MEMALLOCFAILURE:
            return "mem alloc failure"; //$NON-NLS-1$
        case CDERR_MEMLOCKFAILURE:
            return "mem lock failure"; //$NON-NLS-1$
        case CDERR_NOHOOK:
            return "no hook"; //$NON-NLS-1$
        }
        return "unknown"; //$NON-NLS-1$
    }
    
    private void postEvent(AWTEvent e) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
    }
    
    public long close() {
        ci.setVisibleFlag(fileDialog, false);
        fd2win.remove(fileDialog);
        // should post IDABORT, but it doesn't work for some reason
        // so use IDCANCEL as a workaround
        long res = win32.PostMessageW(hwnd, WM_COMMAND, IDCANCEL, 0);
        
        if (res == 0) {
            // awt.err.01=error: {0}
            System.err.println(Messages.getString("awt.err.01", win32.GetLastError())); //$NON-NLS-1$
        }
        return res;
    }
}
