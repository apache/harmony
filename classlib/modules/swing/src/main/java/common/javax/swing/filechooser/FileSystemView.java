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
 * @author Anton Avtamonov, Sergey Burlak
 */

package javax.swing.filechooser;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.UIManager;

import org.apache.harmony.misc.SystemUtils;
import org.apache.harmony.x.swing.filechooser.PlatformFile;
import org.apache.harmony.x.swing.filechooser.PlatformFileManager;

import org.apache.harmony.x.swing.internal.nls.Messages;

public abstract class FileSystemView {
    private static FileSystemView instance;

    private static abstract class AbstractFileSystemView extends FileSystemView {
        public File createNewFolder(final File containingDir) throws IOException {
            String fileName = getFirstFolderName();
            int i = 1;
            while (exists(containingDir, fileName)) {
                fileName = getSubsequentFolderName(i++);
            }

            File result = createFileObject(containingDir, fileName);
            result.mkdir();

            return result;
        }

        private boolean exists(final File dir, final String fileName) {
            File[] files = getFiles(dir, false);
            if (files == null) {
                return false;
            }
            for (int i = 0; i < files.length; i++) {
                if (fileName.equals(files[i].getName())) {
                    return true;
                }
            }

            return false;
        }

        private String getSubsequentFolderName(final int sequencialNumber) {
            return MessageFormat.format(getSubsequentFolderNamePattern(), new Object[] {new Integer(sequencialNumber)});
        }

        protected abstract String getFirstFolderName();
        protected abstract String getSubsequentFolderNamePattern();
    }

    private static class WindowsFileSystemView extends AbstractFileSystemView {
        private static final String NEW_FOLDER_NAME = UIManager.getString("FileChooser.win32.newFolder");
        private static final String NEW_FOLDER_SUBSEQUENT_NAME = UIManager.getString("FileChooser.win32.newFolder.subsequent");

        private static PlatformFileManager fileManager = createManager();

        protected String getFirstFolderName() {
            return NEW_FOLDER_NAME;
        }

        protected String getSubsequentFolderNamePattern() {
            return NEW_FOLDER_SUBSEQUENT_NAME;
        }

        public boolean isRoot(final File f) {
            File[] roots = getRoots();
            for (int i = 0; i < roots.length; i++) {
                if (roots[i].equals(f)) {
                    return true;
                }
            }
            return false;
        }

        public String getSystemDisplayName(final File f) {
            if (f == null) {
                return null;
            }

            PlatformFile ef = getPlatformFile(f);
            return ef != null ? ef.getDisplayName() : super.getSystemDisplayName(f);
        }

        public String getSystemTypeDescription(final File f) {
            if (f == null) {
                return null;
            }

            PlatformFile ef = getPlatformFile(f);
            return ef != null ? ef.getTypeName() : super.getSystemTypeDescription(f);
        }

        public Icon getSystemIcon(final File f) {
            if (f == null) {
                return null;
            }

            PlatformFile ef = getPlatformFile(f);
            return ef != null ? ef.getIcon() : super.getSystemIcon(f);
        }

        public boolean isFileSystem(final File f) {
            PlatformFile ef = getPlatformFile(f);
            return ef != null ? ef.isFileSystem() : true;
        }

        public boolean isFileSystemRoot(final File dir) {
            return isDrive(dir);
        }

        public boolean isDrive(final File dir) {
            PlatformFile ef = getPlatformFile(dir);
            return ef != null ? ef.isDrive() : super.isFileSystemRoot(dir);
        }

        public boolean isFloppyDrive(final File dir) {
            PlatformFile ef = getPlatformFile(dir);
            return ef != null ? ef.isFloppyDrive() : false;
        }

        public boolean isHiddenFile(final File f) {
            PlatformFile ef = getPlatformFile(f);
            return ef != null ? ef.isHidden() : false;
        }


        public boolean isComputerNode(final File dir) {
            PlatformFile ef = getPlatformFile(dir);
            return ef != null ? ef.isComputerNode() : false;
        }

        public File[] getRoots() {
            return new File[] {fileManager.getHomeFolder()};
        }

        public File getDefaultDirectory() {
            return fileManager.getDefaultFolder();
        }

        public File getHomeDirectory() {
            return fileManager.getHomeFolder();
        }

        public File[] getFiles(final File dir, final boolean useFileHiding) {
            File ef = getPlatformFile(dir);
            File root = ef != null ? ef : dir;
            File[] files = root.listFiles();
            if (useFileHiding) {
                List result = new LinkedList();
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isHidden()) {
                        result.add(files[i]);
                    }
                }

                return (File[])result.toArray(new File[result.size()]);
            }
            return files;
        }

        public File getParentDirectory(final File dir) {
            if (dir == null) {
                return null;
            }

            File ef = getPlatformFile(dir);
            File root = ef != null ? ef : dir;
            return root.getParentFile();
        }

        private PlatformFile getPlatformFile(final File file) {
            return fileManager.getPlatformFile(file);
        }

        private static PlatformFileManager createManager() {
            try {
                return (PlatformFileManager)Class.forName("org.apache.harmony.x.swing.filechooser.windows.WinFileManager").newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class LinuxFileSystemView extends OtherFileSystemView {
        public File[] getRoots() {
            return new File[] { new File("/") };
        }
    }

    private static class OtherFileSystemView extends AbstractFileSystemView {
        private static final String NEW_FOLDER_NAME = UIManager.getString("FileChooser.other.newFolder");
        private static final String NEW_FOLDER_SUBSEQUENT_NAME = UIManager.getString("FileChooser.other.newFolder.subsequent");

        protected String getFirstFolderName() {
            return NEW_FOLDER_NAME;
        }

        protected String getSubsequentFolderNamePattern() {
            return NEW_FOLDER_SUBSEQUENT_NAME;
        }
    }

    private Comparator CASE_INSENSITIVE_COMPARATOR = new Comparator() {
        public int compare(final Object o1, final Object o2) {
            return o1.toString().compareToIgnoreCase(o2.toString());
        }
    };


    public static FileSystemView getFileSystemView() {
        if (instance == null) {
            if (SystemUtils.getOS() == SystemUtils.OS_WINDOWS) {
                instance = new WindowsFileSystemView();
            } else if (SystemUtils.getOS() == SystemUtils.OS_LINUX) {
                instance = new LinuxFileSystemView();
            } else {
                instance = new OtherFileSystemView();
            }
        }

        return instance;
    }

    public abstract File createNewFolder(final File containingDir) throws IOException;

    public File createFileObject(final File dir, final String fileName) {
        return new File(dir, fileName);
    }

    public File createFileObject(final String path) {
        return new File(path);
    }

    public File getChild(final File parent, final String fileName) {
        if (parent == null || !parent.exists()) {
            return createFileObject(parent, fileName);
        }
        File[] files = parent.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(fileName)) {
                return files[i];
            }
        }

        return createFileObject(parent, fileName);
    }

    public File getDefaultDirectory() {
        return new File(System.getProperty("user.home"));
    }

    public File[] getFiles(final File dir, final boolean useFileHiding) {
        File[] result = dir.listFiles();
        if (useFileHiding) {
            List filtered = new LinkedList();
            for (int i = 0; i < result.length; i++) {
                if (!result[i].isHidden()) {
                    filtered.add(result[i]);
                }
            }

            result = (File[])filtered.toArray(new File[filtered.size()]);
        }

        if (result != null) {
            Arrays.sort(result, CASE_INSENSITIVE_COMPARATOR);
        } else {
            result = new File[0];
        }

        return result;
    }

    public File getHomeDirectory() {
        return new File(System.getProperty("user.home"));
    }

    public File getParentDirectory(final File dir) {
        return dir != null && !isRoot(dir) ? dir.getParentFile() : null;
    }

    public File[] getRoots() {
        return File.listRoots();
    }

    public String getSystemDisplayName(final File f) {
        if (f == null) {
            return null;
        }

        return f.getName();
    }

    public Icon getSystemIcon(final File f) {
        return null;
    }

    public String getSystemTypeDescription(final File f) {
        if (f.isDirectory()) {
            return "Folder";
        } else if (f.isFile()) {
            return "File";
        }
        return null;
    }

    public boolean isComputerNode(final File dir) {
        return false;
    }

    public boolean isDrive(final File dir) {
        return false;
    }

    public boolean isFileSystem(final File f) {
        return true;
    }

    public boolean isFileSystemRoot(final File dir) {
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].equals(dir)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFloppyDrive(final File dir) {
        return false;
    }

    public boolean isHiddenFile(final File f) {
        return f.isHidden();
    }

    public boolean isParent(final File folder, final File file) {
        return getParentDirectory(file).equals(folder);
    }

    public boolean isRoot(final File f) {
        File[] roots = getRoots();
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].equals(f)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isTraversable(final File f) {
        return Boolean.valueOf(isDrive(f) || f.isDirectory() && f.canRead());
    }


    protected File createFileSystemRoot(final File f) {
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }
}
