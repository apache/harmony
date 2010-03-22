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
 * @author Anton Avtamonov
 */
package org.apache.harmony.x.swing.filechooser.windows;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;

import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.x.swing.filechooser.PlatformFile;

import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

public class WinFile extends PlatformFile {
    private final Win32.ITEMIDLIST absoluteItemId;

    private int description = -1;
    private String displayName;


    public String getDisplayName() {
        Object[] parentInfo = getParentInfo();
        Win32.IShellFolder parentFolder = (Win32.IShellFolder)parentInfo[0];
        String result = WinFileManager.getManager().getChildFolderDisplayName(parentFolder, (Win32.ITEMIDLIST)parentInfo[1], WindowsConsts.SHGDN_FORADDRESSBAR);
        WinFileManager.getManager().release(parentFolder);

        if (result != null) {
            displayName = result;
        } else {
            result = displayName;
        }

        return result;
    }

    public Win32.ITEMIDLIST getAbsoluteItemID() {
        return absoluteItemId;
    }

    public Object[] getParentInfo() {
        return WinFileManager.getManager().getParentShellInfo(absoluteItemId);
    }

    public File getParentFile() {
        Win32.ITEMIDLIST parentItemId = WinFileManager.getManager().getParentItemId(absoluteItemId);
        return parentItemId != null ? WinFileManager.getManager().getWinFile(parentItemId) : null;
    }

    public boolean isDirectory() {
        return getAttribute(WindowsDefs.SFGAO_FOLDER) && !getAttribute(WindowsDefs.SFGAO_STREAM);
    }

    public boolean isHidden() {
        return getAttribute(WindowsDefs.SFGAO_HIDDEN);
    }

    public boolean exists() {
        return isDirectory() ? true : super.exists();
    }

    public File[] listFiles() {
        List files = getContent();
        return (File[])files.toArray(new File[files.size()]);
    }

    public File[] listFiles(final FileFilter filter) {
        List result = getContent();
        for (Iterator it = result.iterator(); it.hasNext(); ) {
            if (!filter.accept((File)it.next())) {
                it.remove();
            }
        }

        return (File[])result.toArray(new File[result.size()]);
    }

    public File[] listFiles(final FilenameFilter filter) {
        List result = getContent();
        for (Iterator it = result.iterator(); it.hasNext(); ) {
            File file = (File)it.next();
            if (!filter.accept(file.getParentFile(), file.getName())) {
                it.remove();
            }
        }

        return (File[])result.toArray(new File[result.size()]);
    }

    public boolean isFileSystem() {
        return getAttribute(WindowsDefs.SFGAO_FILESYSTEM);
    }

    public boolean isRoot() {
        Object[] parentInfo = getParentInfo();
        Win32.IShellFolder parentFolder = (Win32.IShellFolder)parentInfo[0];
        boolean result = parentFolder != null;
        WinFileManager.getManager().release(parentFolder);

        return result;
    }

    public boolean isLink() {
        return getAttribute(WindowsDefs.SFGAO_LINK);
    }


    public boolean isDrive() {
        return isCDROM()
               || isFixedDrive()
               || isNetDrive()
               || isRAM()
               || isRemovable()
               || isFloppyDrive();
    }

    public boolean isCDROM() {
        return getDescription() == WindowsDefs.SHDID_COMPUTER_CDROM;
    }

    public String toString() {
        return isFileSystem() ? super.toString() : getDisplayName();
    }
    
    public boolean isFixedDrive() {
        return getDescription() == WindowsDefs.SHDID_COMPUTER_FIXED;
    }

    public boolean isNetDrive() {
        return getDescription() == WindowsDefs.SHDID_COMPUTER_NETDRIVE;
    }

    public boolean isRAM() {
        return getDescription() == WindowsDefs.SHDID_COMPUTER_RAMDISK;
    }

    public boolean isFloppyDrive() {
        return getDescription() == WindowsDefs.SHDID_COMPUTER_DRIVE35
               || getDescription() == WindowsDefs.SHDID_COMPUTER_DRIVE525;
    }

    public boolean isRemovable() {
        return getDescription() == WindowsDefs.SHDID_COMPUTER_REMOVABLE;
    }

    public boolean isComputerNode() {
        return getDescription() == WindowsDefs.SHDID_NET_SERVER;
    }

    public String getTypeName() {
        if (isDrive()) {
            return getDisplayName();
        } else {
            Win32.SHFILEINFOW fileInfo = getFileInfo();
            return fileInfo.get_szTypeName().getString();
        }
    }


    public Icon getIcon() {
        return null;
    }


    WinFile(final String path, final Win32.ITEMIDLIST absoluteItemId) {
        super(path);
        this.absoluteItemId = absoluteItemId;
    }


    private boolean getAttribute(final int flags) {
        Object[] parentInfo = getParentInfo();
        Win32.IShellFolder parentFolder = (Win32.IShellFolder)parentInfo[0];
        boolean result = (WinFileManager.getManager().getAttribute(parentFolder, (Win32.ITEMIDLIST)parentInfo[1], flags) & flags) != 0;
        WinFileManager.getManager().release(parentFolder);

        return result;
    }

    private int getDescription() {
        if (description == -1) {
            Object[] parentInfo = getParentInfo();
            Win32.IShellFolder parentFolder = (Win32.IShellFolder)parentInfo[0];
            description = WinFileManager.getManager().getDescription(parentFolder, (Win32.ITEMIDLIST)parentInfo[1]);
            WinFileManager.getManager().release(parentFolder);
        }
        return description;
    }

    List getContent() {
        return getContent(WindowsConsts.SHCONTF_STORAGE | WindowsConsts.SHCONTF_FOLDERS | WindowsConsts.SHCONTF_INCLUDEHIDDEN);
    }

    List getContent(final int contentType) {
        if (!isDirectory()) {
            return new ArrayList();
        }

        List result = new LinkedList();
        Win32.ITEMIDLIST[] children = WinFileManager.getManager().getShellContent(absoluteItemId, contentType);
        for (int i = 0; i < children.length; i++) {
            WinFile file = WinFileManager.getManager().getWinFile(children[i]);
            if (!result.contains(file) && (file.isFileSystem() || file.isFileSystemAncestor())) {
                result.add(file);
            }
        }

        return result;
    }

    private Win32.SHFILEINFOW getFileInfo() {
        Object[] parentInfo = getParentInfo();
        WinFileManager.getManager().release((Win32.IShellFolder)parentInfo[0]);
        return WinFileManager.getManager().getFileInfo((Win32.ITEMIDLIST)parentInfo[1],
                WindowsDefs.SHGFI_TYPENAME | WindowsDefs.SHGFI_DISPLAYNAME |
                WindowsDefs.SHGFI_ICON | WindowsDefs.SHGFI_ICONLOCATION);
    }

    private boolean isFileSystemAncestor() {
        return getAttribute(WindowsDefs.SFGAO_FILESYSANCESTOR);
    }
}
