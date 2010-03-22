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
package javax.swing.filechooser;

import java.io.File;
import javax.swing.SwingTestCase;
import org.apache.harmony.misc.SystemUtils;

public class FileSystemViewTest extends SwingTestCase {
    private FileSystemView view;

    private File file;

    public FileSystemViewTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        timeoutDelay = 5 * DEFAULT_TIMEOUT_DELAY;
        view = FileSystemView.getFileSystemView();
        file = new File(new File(System.getProperty("user.home")), new Integer((int) (Math
                .random() * 1000)).toString());
        file.deleteOnExit();
    }

    @Override
    protected void tearDown() throws Exception {
        view = null;
        file.delete();
        file = null;
    }

    public void testGetFileSystemView() throws Exception {
        assertNotNull(FileSystemView.getFileSystemView());
        assertEquals(FileSystemView.getFileSystemView(), FileSystemView.getFileSystemView());
    }

    public void testIsRoot() throws Exception {
        assertFalse(view.isRoot(file));
        if (SystemUtils.getOS() == SystemUtils.OS_WINDOWS) {
            assertEquals(1, view.getRoots().length);
            assertNotEmpty(view.getRoots()[0].getName());
        } else if (SystemUtils.getOS() == SystemUtils.OS_LINUX) {
            assertEquals(1, view.getRoots().length);
            assertEquals("/", view.getRoots()[0].getPath());
        }
    }

    public void testIsTraversable() throws Exception {
        assertFalse(view.isTraversable(file).booleanValue());
        file.createNewFile();
        assertTrue(file.isFile());
        assertFalse(view.isTraversable(file).booleanValue());
        file.delete();
        file.mkdir();
        assertTrue(view.isTraversable(file).booleanValue());
        file.delete();
    }

    public void testGetSystemDisplayName() throws Exception {
        assertEquals(file.getName(), view.getSystemDisplayName(file));
        file = new File("a.b");
        assertEquals(file.getName(), view.getSystemDisplayName(file));
        file = new File("/a/b");
        assertEquals(file.getName(), view.getSystemDisplayName(file));
        File f = File.listRoots()[0];
        assertNotSame(f.getName(), view.getSystemDisplayName(f));
    }

    public void testGetSystemTypeDescription() throws Exception {
        file.createNewFile();
        assertNotEmpty(view.getSystemTypeDescription(file));
        file.delete();
        file.mkdir();
        assertNotEmpty(view.getSystemTypeDescription(file));
    }

    public void testGetRoot() throws Exception {
        assertNotNull(File.listRoots());
        assertTrue(File.listRoots().length > 0);
        assertNotSame(File.listRoots(), File.listRoots());
    }

    public void testGetSystemIcon() throws Exception {
    }

    public void testCreateFileObject() throws Exception {
        File dir = new File(System.getProperty("user.home"));
        assertEquals(new File(dir, "!!!!!!!"), view.createFileObject(dir, "!!!!!!!"));
        assertEquals(new File(dir, "***"), view.createFileObject(dir, "***"));
        assertEquals(new File(dir, "/normal"), view.createFileObject(dir, "/normal"));
        assertEquals(new File(new File("any"), "/normal"), view.createFileObject(
                new File("any"), "/normal"));
        assertEquals(new File(new File("/"), "/normal"), view.createFileObject(new File("/"),
                "/normal"));
    }

    public void testGetChild() throws Exception {
        File parent = new File("a");
        assertEquals(new File("a", "b"), view.getChild(parent, "b"));
        assertEquals(new File("b"), view.getChild(null, "b"));
    }

    public void testGetDefaultDirectory() throws Exception {
        assertNotNull(view.getDefaultDirectory());
    }

    public void testGetHomeDirectory() throws Exception {
        assertNotNull(view.getHomeDirectory());
    }

    public void testGetFiles() throws Exception {
        File dir = new File("a");
        File f = new File(dir, "a");
        try {
            dir.mkdir();
            assertEquals(0, view.getFiles(dir, false).length);
            f.createNewFile();
            assertEquals(1, view.getFiles(dir, false).length);
            f.delete();
            assertEquals(0, view.getFiles(dir, false).length);
        } finally {
            f.delete();
            dir.delete();
        }
    }
    
    public void testHarmony5211() {
        File []roots = File.listRoots();
        for (int i = 0; i < roots.length; i++)
            assertTrue(view.isFileSystemRoot(roots[i]));
    }

    private static void assertNotEmpty(final String name) {
        assertNotNull(name);
        assertTrue("name is empty", name.length() > 0);
    }
}
