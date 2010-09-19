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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public interface JavaFileManager extends Closeable, Flushable, OptionChecker {
    static interface Location {
        String getName();

        boolean isOutputLocation();
    }

    void close() throws IOException;

    void flush() throws IOException;

    ClassLoader getClassLoader(JavaFileManager.Location location);

    FileObject getFileForInput(JavaFileManager.Location location,
            String packageName, String relativeName);

    FileObject getFileForOutput(JavaFileManager.Location location,
            String packageName, String relativeName, FileObject sibling);

    JavaFileObject getJavaFileForInput(JavaFileManager.Location location,
            String className, JavaFileObject.Kind kind);

    JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
            String className, JavaFileObject.Kind kind, FileObject sibling);

    boolean handleOption(String current, Iterator<String> remaining);

    boolean hasLocation(JavaFileManager.Location location);

    String inferBinaryName(JavaFileManager.Location location,
            JavaFileObject file);

    boolean isSameFile(FileObject a, FileObject b);

    Iterable<JavaFileObject> list(JavaFileManager.Location location,
            String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse);
}
