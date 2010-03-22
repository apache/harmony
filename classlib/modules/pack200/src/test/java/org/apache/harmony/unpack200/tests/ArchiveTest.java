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
package org.apache.harmony.unpack200.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;

import org.apache.harmony.unpack200.Archive;

/**
 * Tests for org.apache.harmony.unpack200.Archive, which is the main class for
 * unpack200.
 */
public class ArchiveTest extends TestCase {

    InputStream in;
    JarOutputStream out;
    File file;

    public void testJustResourcesGZip() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/JustResources.pack.gz");
        file = File.createTempFile("Just", "ResourcesGz.jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's SQL module, packed with -E1
    public void testWithSqlE1() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/sql-e1.pack.gz");
        file = File.createTempFile("sql-e1", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's SQL module
    public void testWithSql() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/sql.pack.gz");
        file = File.createTempFile("sql", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
        JarFile jarFile = new JarFile(file);
        file.deleteOnExit();

        File compareFile = new File(Archive.class.getResource(
                "/org/apache/harmony/pack200/tests/sqlUnpacked.jar").toURI());

        JarFile jarFile2 = new JarFile(compareFile);

        long differenceInJarSizes = Math.abs(compareFile.length()
                - file.length());

        assertTrue("Expected jar files to be a similar size, difference was "
                + differenceInJarSizes + " bytes", differenceInJarSizes < 100);

        Enumeration entries = jarFile.entries();
        Enumeration entries2 = jarFile2.entries();
        while(entries.hasMoreElements() && entries2.hasMoreElements()) {

            JarEntry entry = (JarEntry) entries.nextElement();
            assertNotNull(entry);
            String name = entry.getName();

            JarEntry entry2 = (JarEntry) entries2.nextElement();
            assertNotNull(entry2);
            String name2 = entry2.getName();

            assertEquals(name, name2);

            InputStream ours = jarFile.getInputStream(entry);
            InputStream expected = jarFile2.getInputStream(entry2);

            BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(
                    expected));
            String line1 = reader1.readLine();
            String line2 = reader2.readLine();
            int i = 1;
            while (line1 != null || line2 != null) {
                assertEquals("Unpacked class files differ for " + name, line2, line1);
                line1 = reader1.readLine();
                line2 = reader2.readLine();
                i++;
            }
            reader1.close();
            reader2.close();
        }
    }

    // Test with an archive containing Harmony's Pack200 module, packed with -E1
    public void testWithPack200E1() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/pack200-e1.pack.gz");
        file = File.createTempFile("p200-e1", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's Pack200 module
    public void testWithPack200() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/pack200.pack.gz");
        file = File.createTempFile("p200", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's JNDI module
    public void testWithJNDIE1() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/jndi-e1.pack.gz");
        file = File.createTempFile("jndi-e1", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Annotations
    public void testWithAnnotations() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/annotations.pack.gz");
        file = File.createTempFile("annotations", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive packed with the -E0 option
    public void testWithE0() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/simple-E0.pack.gz");
        file = File.createTempFile("simple-e0", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with a class containing lots of local variables (regression test for
    // HARMONY-5470)
    public void testWithLargeClass() throws Exception {
        in = Archive.class
                .getResourceAsStream("/org/apache/harmony/pack200/tests/LargeClass.pack.gz");
        file = File.createTempFile("largeClass", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.unpack();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.delete();
    }

}