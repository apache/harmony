/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.harmony.archive.tests.java.util.jar;

import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.jar.Pack200.Unpacker;

import junit.framework.TestCase;

public class Pack200Test extends TestCase {

    public void testPacker() {
        Packer packer = Pack200.newPacker();
        assertEquals("org.apache.harmony.pack200.Pack200PackerAdapter", packer.getClass().getName());
    }

    public void testUnpacker() {
        Unpacker unpacker = Pack200.newUnpacker();
        assertEquals("org.apache.harmony.unpack200.Pack200UnpackerAdapter", unpacker.getClass().getName());
    }

}
