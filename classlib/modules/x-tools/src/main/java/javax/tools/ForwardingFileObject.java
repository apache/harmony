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

package javax.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

public class ForwardingFileObject<F extends FileObject> implements FileObject {

    protected final F fileObject;

    protected ForwardingFileObject(F fileObject) {
        this.fileObject = fileObject;
    }

    public boolean delete() {
        // do nothing
        return fileObject.delete();
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors)
            throws IOException {
        // do nothing here
        return fileObject.getCharContent(ignoreEncodingErrors);
    }

    public long getLastModified() {
        return fileObject.getLastModified();
    }

    public InputStream openInputStream() throws IOException {
        return fileObject.openInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return fileObject.openOutputStream();
    }

    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return fileObject.openReader(ignoreEncodingErrors);
    }

    public Writer openWriter() throws IOException {
        return fileObject.openWriter();
    }

    public URI toUri() {
        return fileObject.toUri();
    }

    public String getName() {
        return fileObject.getName();
    }
}
