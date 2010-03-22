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
package java.awt;

import java.io.File;
import java.io.FilenameFilter;

import junit.framework.TestCase;

/**
 * FileDialogTest
 */
public class FileDialogTest extends TestCase {
    Frame frame;
    Dialog dialog;
    FileDialog fd;

    public static void main(String[] args) {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new Frame();
        dialog = new Dialog(frame);
        fd = new FileDialog(frame);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame != null) {
            frame.dispose();
        }
    }

    public final void testAddNotify() {
        assertNull(fd.getGraphics());
        assertNull(fd.getLayout());
        fd.addNotify();
        assertTrue(fd.isDisplayable());
        assertNull(fd.getLayout());
        assertNotNull(fd.getGraphics());
    }

    public final void testParamString() {
        String str = fd.paramString();
        assertEquals("name is correct", 0, str.indexOf("filedlg"));
        assertTrue(str.indexOf("modal") > 0);
        assertTrue(str.indexOf("dir=") > 0);
        assertTrue(str.indexOf("file=") > 0);
        assertTrue(str.indexOf("load") > 0);
    }

    private final void constructorTest() {
        assertSame(frame, fd.getParent());
        assertTrue(fd.isModal());
        assertNull(fd.getFile());
        assertNull(fd.getDirectory());
    }
    
    private final void constructorTestDialog() {        
        assertSame(dialog, fd.getParent());
        assertTrue(fd.isModal());
        assertNull(fd.getFile());
        assertNull(fd.getDirectory());
    }
    
    /*
     * Class under test for void FileDialog(java.awt.Frame)
     */
    public final void testFileDialogDialog() {
        fd = new FileDialog(dialog);
        constructorTestDialog();
        assertEquals("", fd.getTitle());
        assertEquals(FileDialog.LOAD, fd.getMode());

    }

    /*
     * Class under test for void FileDialog(java.awt.Frame, java.lang.String)
     */
    public final void testFileDialogDialogString() {
        String title = "Open";
        fd = new FileDialog(dialog, title);
        constructorTestDialog();
        assertEquals(title, fd.getTitle());
        assertEquals(FileDialog.LOAD, fd.getMode());
    }

    /*
     * Class under test for void FileDialog(java.awt.Frame, java.lang.String, int)
     */
    public final void testFileDialogDialogStringint() {
        String title = "Save";
        int mode = FileDialog.SAVE;
        fd = new FileDialog(dialog, title, mode);
        constructorTestDialog();
        assertEquals(title, fd.getTitle());
        assertEquals(mode, fd.getMode());
        boolean iae = false;
        try {
            fd = new FileDialog(dialog, title, mode = -666);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        assertEquals(FileDialog.SAVE, fd.getMode());
    }
    /*
     * Class under test for void FileDialog(java.awt.Frame)
     */
    public final void testFileDialogFrame() {
        constructorTest();
        assertEquals("", fd.getTitle());
        assertEquals(FileDialog.LOAD, fd.getMode());

    }

    /*
     * Class under test for void FileDialog(java.awt.Frame, java.lang.String)
     */
    public final void testFileDialogFrameString() {
        String title = "Open";
        fd = new FileDialog(frame, title);
        constructorTest();
        assertEquals(title, fd.getTitle());
        assertEquals(FileDialog.LOAD, fd.getMode());
    }

    /*
     * Class under test for void FileDialog(java.awt.Frame, java.lang.String, int)
     */
    public final void testFileDialogFrameStringint() {
        String title = "Save";
        int mode = FileDialog.SAVE;
        fd = new FileDialog(frame, title, mode);
        constructorTest();
        assertEquals(title, fd.getTitle());
        assertEquals(mode, fd.getMode());
        boolean iae = false;
        try {
            fd = new FileDialog(frame, title, mode = -666);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        assertEquals(FileDialog.SAVE, fd.getMode());
    }

    public final void testGetFile() {
        assertNull(fd.getFile());
    }

    public final void testGetDirectory() {
        assertNull(fd.getDirectory());
    }

    public final void testGetFilenameFilter() {
        assertNull(fd.getFilenameFilter());
    }

    public final void testGetMode() {
        assertEquals(FileDialog.LOAD, fd.getMode());
    }

    public final void testSetDirectory() {
        String dir = "dir";
        fd.setDirectory(dir);
        assertEquals(dir, fd.getDirectory());
        fd.setDirectory(null);
        assertNull(fd.getDirectory());
        fd.setFile(dir="");
        assertNull(fd.getDirectory());
    }

    public final void testSetFile() {
        String file = "file";
        fd.setFile(file);
        assertEquals(file, fd.getFile());
        fd.setFile(null);
        assertNull(fd.getFile());
        fd.setFile(file="");
        assertNull(fd.getFile());
    }

    public final void testSetFilenameFilter() {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return false;
            }
        };
        fd.setFilenameFilter(filter);
        assertSame(filter, fd.getFilenameFilter());
        fd.setFilenameFilter(null);
        assertNull(fd.getFilenameFilter());
    }

    public final void testSetMode() {
        int mode = 1000;
        boolean iae = false;
        try {
            fd.setMode(mode);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        assertEquals(FileDialog.LOAD, fd.getMode());
        fd.setMode(mode = FileDialog.SAVE);
        assertEquals(mode, fd.getMode());
        fd.setMode(mode = FileDialog.LOAD);
        assertEquals(mode, fd.getMode());
    }

}
