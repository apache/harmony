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

package org.apache.harmony.luni.tests.java.io;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class UnixFileTest extends TestCase {

    public void test_getCanonicalPath() throws IOException,
                                               InterruptedException {
        File tmpFolder1 = new File("folder1");
        tmpFolder1.mkdirs();
        tmpFolder1.deleteOnExit();

        File tmpFolder2 = new File(tmpFolder1.toString() + "/folder2");
        tmpFolder2.mkdirs();
        tmpFolder2.deleteOnExit();

        File tmpFolder3 = new File(tmpFolder2.toString() + "/folder3");
        tmpFolder3.mkdirs();
        tmpFolder3.deleteOnExit();

        File tmpFolder4 = new File(tmpFolder3.toString() + "/folder4");
        tmpFolder4.mkdirs();
        tmpFolder4.deleteOnExit();

        // make a link to folder1/folder2
        Process ln = Runtime.getRuntime().exec("ln -s folder1/folder2 folder2");
        ln.waitFor();
        File linkFile = new File("folder2");
        linkFile.deleteOnExit();

        File file = new File("folder2");
        assertEquals(tmpFolder2.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder1/folder2");
        assertEquals(tmpFolder2.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder2/folder3");
        assertEquals(tmpFolder3.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder2/folder3/folder4");
        assertEquals(tmpFolder4.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder1/folder2/folder3");
        assertEquals(tmpFolder3.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder1/folder2/folder3/folder4");
        assertEquals(tmpFolder4.getCanonicalPath(), file.getCanonicalPath());
    }
    
}