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

package org.apache.harmony.swing.tests.javax.swing.filechooser;

import junit.framework.TestCase;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Random;

public class FileNameExtensionFilterTest extends TestCase {

    private final String[] extensions = {"zip", "jar"};
    private final String description = "A description";

    public void test_constructor() {
        // Should be able to create fnfe with a null descriptor without exception
        FileNameExtensionFilter fnfe = new FileNameExtensionFilter(null, extensions);
        assertNull(fnfe.getDescription()); // Just check getDescription() returns null

        // Should throw IllegalArgumentException for a null extensions array
        String[] extensionsArray = null;
        try {
            fnfe = new FileNameExtensionFilter(description, extensionsArray);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Should throw IllegalArgumentException for 0 length extensions array
        try {
            fnfe = new FileNameExtensionFilter(description);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Should throw IllegalArgumentException for a null extension
        try {
            fnfe = new FileNameExtensionFilter(description, "zip", null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Should throw IllegalArgumentException for a 0 length extension
        try {
            fnfe = new FileNameExtensionFilter(description, "zip", "");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void test_getDescription() {
        FileNameExtensionFilter fnfe = new FileNameExtensionFilter(description, extensions);

        assertEquals(description, fnfe.getDescription());
    }

    public void test_getExtensions() {
        FileNameExtensionFilter fnfe = new FileNameExtensionFilter(description, extensions);

        String[] returnedExt = fnfe.getExtensions();
        for (int i=0; i<extensions.length; i++) {
            assertEquals(extensions[i], returnedExt[i]);
        }        
    }

    public void test_accept() throws Exception {
        FileNameExtensionFilter fnfe = new FileNameExtensionFilter(description, extensions);

        File file = new File("my_test.jpg");
        assertFalse(fnfe.accept(file));

        file = new File("my_test.zipp");
        assertFalse(fnfe.accept(file));

        file = new File("my_testzip");
        assertFalse(fnfe.accept(file));

        file = new File("my_test.zzip");
        assertFalse(fnfe.accept(file));

        file = new File("my_test.zip");
        assertTrue(fnfe.accept(file));

        file = new File("my_test.jar");
        assertTrue(fnfe.accept(file));

        /* accept() should always return true for directories, whatever the extension */
        file = createTempDir("FileNameExtensionFilterTest", ".zip");
        assertTrue(fnfe.accept(file));

        file = createTempDir("FileNameExtensionFilterTest", ".dir");
        assertTrue(fnfe.accept(file));
    }

    public void test_toString() {
        FileNameExtensionFilter fnfe = new FileNameExtensionFilter(description, extensions);

        assertTrue(fnfe.toString().endsWith("[description=A description extensions=[zip, jar]]"));
    }

    /* Creates an empty temporary dir using the logic from java.io.File.createTempFile() */
    private File createTempDir(String prefix, String suffix) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File tempDir;
        int counter = 0;
        do {
            if (counter == 0) {
                int newInt = new Random().nextInt();
                counter = ((newInt / 65535) & 0xFFFF) + 0x2710;
            } else{
                counter++;
            }
                        
            tempDir = new File(tmpDir, prefix + counter + suffix);
        } while (tempDir.exists());
        tempDir.mkdir();
        tempDir.deleteOnExit();
        return tempDir;
    }
}
