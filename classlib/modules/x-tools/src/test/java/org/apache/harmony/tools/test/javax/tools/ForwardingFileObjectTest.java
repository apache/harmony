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

package org.apache.harmony.tools.test.javax.tools;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.tools.FileObject;
import javax.tools.ForwardingFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.junit.Test;

public class ForwardingFileObjectTest {

    class MockForwardingFileObject extends ForwardingFileObject {
        public MockForwardingFileObject(URI uri, Kind kind) {
            super(new MockSimpleJavaFileObject(uri, kind));
        }
    }

    MockForwardingFileObject mock = new MockForwardingFileObject(null, Kind.OTHER);
    
    @Test
    public void testDelete() {
        mock.delete();
    }

    @Test
    public void testGetCharContent() {
        mock.getCharContent(false);
    }

    @Test
    public void testGetLastModified() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetName() {
        fail("Not yet implemented");
    }

    @Test
    public void testOpenInputStream() {
        fail("Not yet implemented");
    }

    @Test
    public void testOpenOutputStream() {
        fail("Not yet implemented");
    }

    @Test
    public void testOpenReader() {
        fail("Not yet implemented");
    }

    @Test
    public void testOpenWriter() {
        fail("Not yet implemented");
    }

    @Test
    public void testToUri() {
        fail("Not yet implemented");
    }

}
