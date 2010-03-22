/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.luni.tests.java.io;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;

import tests.support.resource.Support_Resources;

import junit.framework.TestCase;

/**
 * Please note that this case can only be passed on Windows due to some file
 * system dependent reason.
 * 
 */

public class WinFileTest extends TestCase {
	private File testFile;

	private File testDir;	

    /**
     * @tests java.io.File#mkdir() 
     * @throws IOException
     */
    public void test_mkdir() throws IOException {
        // Test for method boolean java.io.File.mkdir() in Windows Platform

        String base = System.getProperty("user.dir");
        // Old test left behind "garbage files" so this time it creates a
        // directory
        // that is guaranteed not to already exist (and deletes it afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }
        assertTrue("mkdir failed", dir.mkdir() && dir.exists());
        dir.deleteOnExit();        
        String newbase = new String(dir + File.separator);
        
        dir = new File(newbase, ".abcd");
        assertTrue("mkdir " + dir.getCanonicalPath() + " failed",
                dir.mkdir() && dir.exists() && !(new File(newbase,"abcd")).exists());
        dir.deleteOnExit();        

        String []ss1 = {
                ".abcd" + File.separator + "." + File.separator + "dir1",
                ".abcd" + File.separator + ".." + File.separator + "dir2",
                ".abcd" + File.separator + "." + File.separator + "." + File.separator + "dir3",
                "12" + File.separator + "34" + File.separator + ".." + File.separator + ".." + File.separator + "dir4",
                "12" + File.separator + ".." + File.separator + "34" + File.separator + ".." + File.separator + "dir5",
                ".abcd." + File.separator + ".." + File.separator + "dir6.",
                ".abcd.." + File.separator + ".." + File.separator + "dir8"
        };
        String []ss2 = {
                ".abcd" + File.separator + "dir1",
                "dir2",
                ".abcd" + File.separator + "dir3",
                "dir4",
                "dir5",
                "dir6",
                "dir8"                
        };
        for (int i=0; i<ss1.length; i++)
        {
            dir = new File(newbase, ss1[i]);
            assertTrue("mkdir " + dir.getCanonicalPath() + " failed",
                    dir.mkdir() && dir.exists());
            dir = new File(newbase, ss2[i]);
            assertTrue("mkdir " + dir.getCanonicalPath() + " failed",
                    dir.exists());
            dir.deleteOnExit();
        }
    }


    /**
     * Regression test for HARMONY-4794
     */
    public void testNonASCIIFileName_4794() throws IOException {
        final String FILENAME="\u30d5\u30a1\u30a4\u30eb1.txt";
        final String CONTENT = "A pretty predicament";
        final String CNTNT_CHARSET = "ISO-8859-1"; 

        File folder = Support_Resources.createTempFolder();
        File f = new File(folder, FILENAME);
        FileOutputStream fos = new FileOutputStream(f);
        FileInputStream fis;
 
        f.createNewFile();
        f.deleteOnExit();
        fos.write(CONTENT.getBytes(CNTNT_CHARSET));
        fos.close();

        f = new File(folder, FILENAME);        
        assertEquals("Invalid file name", FILENAME, f.getName()); 
        assertTrue("File does not exist", f.exists());
        byte tmp[] = new byte[256];
        String wasRed;
        int n;

        fis = new FileInputStream(f);
        n = fis.read(tmp);
        fis.close();
        wasRed = new String(tmp, 0, n, CNTNT_CHARSET);
        assertEquals("Invalid content was red", CONTENT, wasRed);
    }


	/**
	 * @tests java.io.File#canExecute()
	 * 
	 * @since 1.6
	 */
	public void test_canExecute() {
		// files and directories are always executable on Windows.
		assertTrue(testFile.canExecute());
		assertFalse(testFile.setExecutable(false, false));
		assertTrue(testFile.canExecute());
		assertTrue(testFile.setExecutable(true));
		assertTrue(testFile.canExecute());

		assertFalse(testDir.setExecutable(false, false));
		assertTrue(testDir.canExecute());
	}
	
	/**
	 * @tests java.io.File#setExecutable(boolean, boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setExecutableZZ() {
		// setExecutable(false, true/false) fails on Windows.
		assertTrue(testFile.canExecute());
		assertFalse(testFile.setExecutable(false, false));
		assertTrue(testFile.canExecute());
		assertFalse(testFile.setExecutable(false, true));
		// setExecutable(true, true/false)
		assertTrue(testFile.setExecutable(true, false));
		assertTrue(testFile.canExecute());
		assertTrue(testFile.setExecutable(true, true));

		// tests directory
		assertTrue(testDir.canExecute());
		assertFalse(testDir.setExecutable(false, true));
		assertTrue(testDir.canExecute());
		assertFalse(testDir.setExecutable(false, false));

		assertTrue(testDir.setExecutable(true, true));
		assertTrue(testDir.setExecutable(true, false));
		assertTrue(testDir.canExecute());
	}

	/**
	 * @tests java.io.File#setExecutable(boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setExecutableZ() {
		// So far this method only deals with the situation that the user is the
		// owner of the file
		assertTrue(testFile.setExecutable(true));
		assertFalse(testFile.setExecutable(false));
		// tests directory
		assertTrue(testDir.setExecutable(true));
		assertFalse(testDir.setExecutable(false));
		assertTrue(testDir.canExecute());
	}

	/**
	 * @tests java.io.File#setReadable(boolean, boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setReadableZZ() throws Exception {
		// setReadable(false, false/true) fails on Windows. Files on windows are
		// always readable.
		assertTrue(testFile.canRead());
		assertFalse(testFile.setReadable(false, false));
		assertTrue(testFile.canRead());
		assertFalse(testFile.setReadable(false, true));
		assertTrue(testFile.canRead());

		assertFalse(testDir.setReadable(false, true));
		assertFalse(testDir.setReadable(false, false));
		assertTrue(testDir.canRead());

		// setReadable(true, false/true) and set them in turn
		assertTrue(testFile.setReadable(true, false));
		assertTrue(testFile.canRead());
		// setReadable(false) does not take effects on Windows.
		assertFalse(testFile.setReadable(false, true));
		assertTrue(testFile.canRead());
		assertTrue(testFile.setReadable(true, true));
		assertTrue(testFile.canRead());
		assertFalse(testFile.setReadable(false, false));
		assertTrue(testFile.canRead());

		assertTrue(testDir.setReadable(true, false));
		assertTrue(testDir.canRead());
		assertTrue(testDir.setReadable(true, true));
		assertTrue(testDir.canRead());
	}

	/**
	 * @tests java.io.File#setReadable(boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setReadableZ() {
		// So far this method only deals with the situation that the user is the
		// owner of the file
		// setReadable(false) fails on Windows. canRead() always returns true.
		assertFalse(testFile.setReadable(false));
		assertTrue(testFile.canRead());
		assertFalse(testDir.setReadable(false));
		assertTrue(testDir.canRead());

		// setReadable(true)
		assertTrue(testFile.setReadable(true));
		assertTrue(testFile.canRead());
		assertTrue(testDir.setReadable(true));
		assertTrue(testDir.canRead());
	}

    /**
     * @test java.io.File#toURI()
     */
    public void test_toURI_UNC() throws Exception {
        File f = new File("\\\\unchost\\[dir]\\file.txt");
        assertNotNull(f.toURI());
        assertEquals("incorrect URI for UNC path: " + f.toURI(), f.toURI(),
                new URI("file:////unchost/%5Bdir%5D/file.txt"));
    }

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testFile = File.createTempFile("testfile", null);
		testDir = new File(System.getProperty("java.io.tmpdir"));
	}

	@Override
	protected void tearDown() throws Exception {
		testFile.deleteOnExit();
		testDir.deleteOnExit();
		testFile = null;
		testDir = null;
		super.tearDown();
	}

}
