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
import java.util.Iterator;
import java.util.Set;

import javax.tools.JavaFileObject.Kind;

public class ForwardingJavaFileManager<M extends JavaFileManager> implements
        JavaFileManager {

    protected JavaFileManager fileManager;

    protected ForwardingJavaFileManager(M fileManager) {
        this.fileManager = fileManager;
    }

    public void close() throws IOException {
        fileManager.close();
    }

    public void flush() throws IOException {
        fileManager.flush();
    }

    public ClassLoader getClassLoader(Location location) {
        return fileManager.getClassLoader(location);
    }

    public FileObject getFileForInput(Location location, String packageName,
            String relativeName) {
        return fileManager.getFileForInput(location, packageName, relativeName);
    }

    public FileObject getFileForOutput(Location location, String packageName,
            String relativeName, FileObject sibling) {
        return fileManager.getFileForInput(location, packageName, relativeName);
    }

    public JavaFileObject getJavaFileForInput(Location location,
            String className, Kind kind) {
        return fileManager.getJavaFileForInput(location, className, kind);
    }

    public JavaFileObject getJavaFileForOutput(Location location,
            String className, Kind kind, FileObject sibling) {
        return fileManager.getJavaFileForInput(location, className, kind);
    }

    public boolean handleOption(String current, Iterator<String> remaining) {
        return fileManager.handleOption(current, remaining);
    }

    public boolean hasLocation(Location location) {
        return fileManager.hasLocation(location);
    }

    public String inferBinaryName(Location location, JavaFileObject file) {
        return fileManager.inferBinaryName(location, file);
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return fileManager.isSameFile(a, b);
    }

    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<Kind> kinds, boolean recurse) {
        return fileManager.list(location, packageName, kinds, recurse);
    }

    public int isSupportedOption(String option) {
        return fileManager.isSupportedOption(option);
    }

}
