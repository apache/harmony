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

package javax.swing.filechooser;

import java.io.File;

public final class FileNameExtensionFilter extends FileFilter {

    private String description;
    private String[] extensions;

    public FileNameExtensionFilter(String description, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException("Extensions array cannot be null or zero in size");
        }

        for (int i=0; i<extensions.length; i++) {
            if (extensions[i] == null || extensions[i].length() == 0) {
                throw new IllegalArgumentException("Extensions cannot contain a null or zero length string");
            }
        }

        this.description = description;
        this.extensions = extensions;
    }

    public boolean accept(File f) {
        if (f.isDirectory()) return true;

        for (int i=0; i<extensions.length; i++) {
            if (f.getName().endsWith("." + extensions[i])) {
                return true;
            }
        }

        return false;
    }

    public String getDescription() {
        return description;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String toString() {
        String resultString = super.toString() + "[description="+description+" extensions=[";
        for (int i=0; i<extensions.length; i++) {
            resultString += extensions[i];
            if (i != extensions.length - 1) {
                resultString += ", ";
            }
        }
        resultString += "]]";
        return resultString;
    }
}
