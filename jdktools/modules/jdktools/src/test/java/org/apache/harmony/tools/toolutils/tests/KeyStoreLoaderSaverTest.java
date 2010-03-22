/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.toolutils.tests;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;

import junit.framework.TestCase;

import org.apache.harmony.tools.toolutils.KeyStoreLoaderSaver;

public class KeyStoreLoaderSaverTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(KeyStoreLoaderSaverTest.class);
    }

    /**
     * @tests 'KeyStoreLoaderSaver.loadStore(String, String, char[], String)'
     */
    public void testLoadStore() throws Exception {
        
        // Regression for HARMONY-1794
        try {
            KeyStoreLoaderSaver.loadStore("there_is_no_such_file", "BKS", "pwd"
                    .toCharArray(), null);

            // IOException must be thrown, because file does not exist
            fail("No expected IOException");
        } catch (IOException ok) {
        }
    }
    
    /**
     * @tests 'KeyStoreLoaderSaver.saveStore(KeyStore, String, char[], boolean)'
     */
    public void testSaveStore() throws Exception {

        // Regression for HARMONY-1927
        // create a path to save the store to
        String tempDir = System.getProperty("java.io.tmpdir")
                + File.separatorChar;
        String keyStorePath = tempDir + "SaveStoreTestTemporaryFile";
        try {
            KeyStore keyStore = KeyStoreLoaderSaver.loadStore(null, "BKS",
                    "pwd".toCharArray(), null);

            KeyStoreLoaderSaver.saveStore(keyStore, keyStorePath, "pwd"
                    .toCharArray(), false);
        } finally {
            new File(keyStorePath).delete();
        }
    }
}
