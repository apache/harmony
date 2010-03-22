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
 * @author Anton Avtamonov, Rustem Rafikov
 */
package org.apache.harmony.x.swing.filechooser.windows;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.harmony.awt.nativebridge.Int16Pointer;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.x.swing.filechooser.PlatformFile;
import org.apache.harmony.x.swing.filechooser.PlatformFileManager;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class WinFileManager implements PlatformFileManager {

    private static WinFileManager instance;

    private static final Win32 win32 = Win32.getInstance();
    private static final NativeBridge nb = NativeBridge.getInstance();

    //  REFIID for IShellFolder interface, see ComDefs.h
    public static final Win32.GUID SHELL_FOLDER_GUID = getGUID("{000214E6-0000-0000-C000-000000000046}");

    private final Win32.IShellFolder DESKTOP_SHELL_FOLDER = createDesktopShellFolder();
    private final WinFile DESKTOP_FOLDER = getWinFile(getAbsoluteItemID(WindowsDefs.CSIDL_DESKTOP));

    public static WinFileManager getManager() {
        return instance;
    }

    public WinFileManager() {
        if (instance != null) {
            throw new RuntimeException(Messages.getString("swing.6C")); //$NON-NLS-1$
        }
        instance = this;
    }

    public File getDefaultFolder() {
        return getMyDocumentsFolder();
    }

    public File getHomeFolder() {
        return getDesktopFolder();
    }

    public PlatformFile getPlatformFile(final File file) {
        if (file instanceof WinFile) {
            return (WinFile)file;
        }

        Win32.ITEMIDLIST absoluteItemId = getAbsoluteItemID(file
                .getAbsolutePath());
        if (absoluteItemId == null) {
            return null;
        }

        return getWinFile(absoluteItemId);
    }

    public WinFile getDesktopFolder() {
        return DESKTOP_FOLDER;
    }

    public Win32.IShellFolder getDesktopShellFolder() {
        return DESKTOP_SHELL_FOLDER;
    }

    public WinFile getMyDocumentsFolder() {
        return getWinFileForSystemFolder(WindowsDefs.CSIDL_PERSONAL);
    }

    public WinFile[] getDrives() {
        WinFile myComputer = getWinFileForSystemFolder(WindowsDefs.CSIDL_DRIVES);
        if (myComputer != null) {
            List drives = myComputer.getContent(WindowsConsts.SHCONTF_STORAGE);
            return (WinFile[]) drives.toArray(new WinFile[drives.size()]);
        }

        return new WinFile[0];
    }

    Win32.ITEMIDLIST[] getShellContent(final Win32.ITEMIDLIST absoluteParentItemId, final int flags) {
        Win32.IShellFolder parentFolder = getShellFolder(absoluteParentItemId);
        List result = new LinkedList();
        PointerPointer enumIDListPtrPtr = nb.createPointerPointer(1, false);
        if (parentFolder.EnumObjects(0, flags, enumIDListPtrPtr) == WindowsDefs.NOERROR) {
            Win32.IEnumIDList idList = win32.createIEnumIDList(enumIDListPtrPtr.getAddress(0));

            PointerPointer itemListPtr = nb.createPointerPointer(1, false);

            while (true) {
                if (idList.Next(1, itemListPtr, null) == WindowsDefs.NOERROR) {
                    Win32.ITEMIDLIST itemId = win32.createITEMIDLIST(itemListPtr.getAddress(0));
                    result.add(ItemIdProcessor.getAbsoluteItemID(absoluteParentItemId, itemId));
                } else {
                    break;
                }
            }
            idList.Release();
        }
        release(parentFolder);

        return (Win32.ITEMIDLIST[])result.toArray(new Win32.ITEMIDLIST[result.size()]);
    }

    WinFile getWinFile(final Win32.ITEMIDLIST absoluteItemId) {
        Object[] parentInfo = getParentShellInfo(absoluteItemId);
        Win32.IShellFolder parentFolder = (Win32.IShellFolder)parentInfo[0];
        String path = getPath(parentFolder, (Win32.ITEMIDLIST)parentInfo[1]);
        release(parentFolder);

        return new WinFile(path, absoluteItemId);
    }

    String getChildFolderDisplayName(final Win32.IShellFolder parent, final Win32.ITEMIDLIST itemId, final int flags) {
        int bufLen = 2048;

        Win32.STRRET strret = win32.createSTRRET(false);

        if (parent.GetDisplayNameOf(itemId, flags, strret) != WindowsDefs.NOERROR) {
            return null;
        }

        Int16Pointer bufPtr = nb.createInt16Pointer(bufLen, false);
        win32.StrRetToBufW(strret, null, bufPtr, bufLen);
        return bufPtr.getString();
    }

    void release(final Win32.IShellFolder folder) {
        if (folder == null) {
            return;
        }
        folder.Release();
    }

    int getAttribute(final Win32.IShellFolder parent, final Win32.ITEMIDLIST itemId, final int flags) {
        PointerPointer itemIdPtrPtr = nb.createPointerPointer(1, false);
        itemIdPtrPtr.set(0, itemId);

        Int32Pointer resultPtr = nb.createInt32Pointer(1, false);
        resultPtr.set(0, flags);
        if (parent.GetAttributesOf(1, itemIdPtrPtr, resultPtr) != WindowsDefs.NOERROR) {
            return -1;
        }
        return resultPtr.get(0);
    }

    Win32.SHFILEINFOW getFileInfo(final Win32.ITEMIDLIST itemId, final int flags) {
        Win32.SHFILEINFOW fileInfo = win32.createSHFILEINFOW(false);

        Win32.SHFILEINFOW result = null;
        if (win32.SHGetFileInfoW(nb.createInt16Pointer(itemId), 0, fileInfo,
                fileInfo.size(), WindowsDefs.SHGFI_PIDL | flags) != 0) {
            result = fileInfo;
        }
        return result;
    }

    int getDescription(final Win32.IShellFolder parent, final Win32.ITEMIDLIST itemId) {
        Win32.SHDESCRIPTIONID res = win32.createSHDESCRIPTIONID(false);

        if (win32.SHGetDataFromIDListW(parent, itemId, WindowsDefs.SHGDFIL_DESCRIPTIONID, res, res.size()) == WindowsDefs.NOERROR) {
            return res.get_dwDescriptionId();
        }
        return 0 ;
    }

    Win32.ITEMIDLIST getParentItemId(final Win32.ITEMIDLIST absoluteItemId) {
        return ItemIdProcessor.getAbsoluteParentItemID(absoluteItemId);
    }

    Object[] getParentShellInfo(final Win32.ITEMIDLIST absoluteItemId) {

        PointerPointer relativeItemIdPrtPrt = nb.createPointerPointer(1, false);
        PointerPointer shellFolderPtrPtr = nb.createPointerPointer(1, false);
        if (win32.SHBindToParent(absoluteItemId, WinFileManager.SHELL_FOLDER_GUID, shellFolderPtrPtr,
                relativeItemIdPrtPrt) != WindowsDefs.NOERROR) {
            return new Object[2];
        }
        Win32.IShellFolder parentFolder = win32.createIShellFolder(shellFolderPtrPtr.getAddress(0));
        Win32.ITEMIDLIST relativeItemId = win32.createITEMIDLIST(relativeItemIdPrtPrt.getAddress(0));
        return new Object[] { parentFolder, relativeItemId };
    }

    private String getPath(final Win32.IShellFolder parent, final Win32.ITEMIDLIST itemId) {
        return getChildFolderDisplayName(parent, itemId, WindowsConsts.SHGDN_FORPARSING);
    }

    private static Win32.GUID getGUID(final String name) {
        Int16Pointer namePrt = nb.createInt16Pointer(name, false);
        Win32.GUID result = win32.createGUID(false);
        if (win32.CLSIDFromString(namePrt, result) != WindowsDefs.NOERROR) {
            return null;
        }
        return result;
    }

    private Win32.IShellFolder getShellFolder(final Win32.ITEMIDLIST absoluteItemId) {
        if (ItemIdProcessor.isRoot(absoluteItemId)) {
            return getDesktopShellFolder();
        }

        Object[] parentInfo = getParentShellInfo(absoluteItemId);
        Win32.IShellFolder parentFolder = (Win32.IShellFolder)parentInfo[0];
        Win32.IShellFolder result = getShellFolder(parentFolder, (Win32.ITEMIDLIST)parentInfo[1]);
        release(parentFolder);

        return result;
    }

    private Win32.IShellFolder getShellFolder(final Win32.IShellFolder parent, final Win32.ITEMIDLIST itemId) {
        if (parent == null) {
            return null;
        }

        PointerPointer childPtrPtr = nb.createPointerPointer(1, false);
        if (parent.BindToObject(itemId, null, WinFileManager.SHELL_FOLDER_GUID, childPtrPtr) != WindowsDefs.NOERROR) {
            return null;
        }
        return win32.createIShellFolder(childPtrPtr.getAddress(0));
    }

    private WinFile getWinFileForSystemFolder(final int folderID) {
        Win32.ITEMIDLIST itemId = getAbsoluteItemID(folderID);
        if (itemId != null) {
            return getWinFile(itemId);
        }
        return null;
    }

    private Win32.IShellFolder createDesktopShellFolder() {
        PointerPointer shellDesktopPtrPtr = nb.createPointerPointer(1, false);
        if (win32.SHGetDesktopFolder(shellDesktopPtrPtr) != WindowsDefs.NOERROR) {
            return null;
        }
        return win32.createIShellFolder(shellDesktopPtrPtr.getAddress(0));
    }

    private Win32.ITEMIDLIST getAbsoluteItemID(final int folderId) {
        PointerPointer itemIdPtrPtr = nb.createPointerPointer(1, false);
        if (win32.SHGetFolderLocation(0, folderId, null, 0, itemIdPtrPtr) != WindowsDefs.NOERROR) {
            return null;
        }
        return win32.createITEMIDLIST(itemIdPtrPtr.getAddress(0));
    }

    private Win32.ITEMIDLIST getAbsoluteItemID(final String path) {
        Win32.IShellFolder desktop = getDesktopShellFolder();

        Int16Pointer displayNamePtr = nb.createInt16Pointer(path, false);
        PointerPointer itemIdPtrPtr = nb.createPointerPointer(1, false);
        if (desktop.ParseDisplayName(0, null, displayNamePtr, null, itemIdPtrPtr, null) != WindowsDefs.NOERROR) {
            return null;
        }
        return win32.createITEMIDLIST(itemIdPtrPtr.getAddress(0));
    }

    private static class ItemIdProcessor {
        public static Win32.ITEMIDLIST getAbsoluteItemID(final Win32.ITEMIDLIST parentAbsoluteItemId, final Win32.ITEMIDLIST itemId) {
            final int parentSize = getItemIDListSize(parentAbsoluteItemId) - 2;
            if (parentSize <= 0) {
                return itemId;
            }
            final int childSize = getItemIDListSize(itemId);
            Int8Pointer resPtr = nb.createInt8Pointer(parentSize + childSize, true); //!!! should be in native mem

            resPtr.copy(parentAbsoluteItemId, 0, parentSize);
            resPtr.copy(itemId, parentSize, childSize);

            return win32.createITEMIDLIST(resPtr);
        }

        public static Win32.ITEMIDLIST getAbsoluteParentItemID(final Win32.ITEMIDLIST absoluteItemId) {
            Win32.ITEMIDLIST lastItemId = getLastItemIDList(absoluteItemId);
            if (lastItemId == null) {
                return null;
            }

            final int parentSize = (int)(lastItemId.shortLockPointer() - absoluteItemId.shortLockPointer());
            lastItemId.unlock();
            absoluteItemId.unlock();

            Int8Pointer resPtr = nb.createInt8Pointer(parentSize + 2, true); // !!! should be in native mem
            resPtr.copy(absoluteItemId, 0, parentSize);
            resPtr.copy(lastItemId.getElementPointer(getItemIDListSize(lastItemId) - 2), parentSize, 2);
            return win32.createITEMIDLIST(resPtr);
        }

        public static boolean isRoot(final Win32.ITEMIDLIST itemId) {
            return getFirstItemSize(itemId) == 0;
        }

        private static int getFirstItemSize(final Win32.ITEMIDLIST itemId) {
            return itemId.get_mkid().get_cb();
        }

        private static Win32.ITEMIDLIST getNextItemID(final Win32.ITEMIDLIST itemId) {
            if (itemId == null) {
                return null;
            }
            final int size = getFirstItemSize(itemId);
            if (size == 0) {
                return null;
            }

            Win32.ITEMIDLIST result = win32.createITEMIDLIST(itemId.getElementPointer(size));

            final int nextSize = getFirstItemSize(result);
            if (nextSize == 0) {
                return null;
            }
            return result;
        }

        private static int getItemIDListSize(final Win32.ITEMIDLIST itemId) {
            int size = getFirstItemSize(itemId);
            if (size == 0) {
                return 0;
            }

            Win32.ITEMIDLIST child = getNextItemID(itemId);
            while (child != null) {
                size += getFirstItemSize(child);
                child = getNextItemID(child);
            }
            return size + 2;
        }

        private static Win32.ITEMIDLIST getLastItemIDList(final Win32.ITEMIDLIST itemId) {
            if (getFirstItemSize(itemId) == 0) {
                return null;
            }

            Win32.ITEMIDLIST prevChild = itemId;
            Win32.ITEMIDLIST child = getNextItemID(prevChild);
            while (child != null) {
                prevChild = child;
                child = getNextItemID(child);
            }

            return prevChild;
        }
    }
}
