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
package org.apache.harmony.x.imageio.plugins;

public enum ImageType {
        @SuppressWarnings("nls")
        JPEG(new String[] { "jpeg", "jpg", "JPEG", "JPG" }, new String[] {
                        "jpeg", "jpg" }, new String[] { "image/jpeg" }),
        @SuppressWarnings("nls")
        BMP(new String[] { "bmp", "BMP" }, new String[] { "bmp" },
                        new String[] { "image/bmp" }),
        @SuppressWarnings("nls")
        GIF(new String[] { "gif", "GIF" }, new String[] { "gif" },
                        new String[] { "image/gif" }),
        @SuppressWarnings("nls")
        PNG(new String[] { "png", "PNG" }, new String[] { "png" },
                        new String[] { "image/png" });

    private final String names[];
    private final String suffixes[];
    private final String mimeTypes[];

    ImageType(final String names[], final String suffixes[],
                    final String mimeTypes[]) {
        this.names = names;
        this.suffixes = suffixes;
        this.mimeTypes = mimeTypes;
    }

    public String[] getNames() {
        return names.clone();
    }

    public String[] getSuffixes() {
        return suffixes.clone();
    }

    public String[] getMimeTypes() {
        return mimeTypes.clone();
    }
}
