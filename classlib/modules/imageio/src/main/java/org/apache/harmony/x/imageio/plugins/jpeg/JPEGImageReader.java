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
package org.apache.harmony.x.imageio.plugins.jpeg;

import javax.imageio.ImageReadParam;
import javax.imageio.plugins.jpeg.JPEGImageReadParam;
import javax.imageio.spi.ImageReaderSpi;

import org.apache.harmony.x.imageio.plugins.AwtImageReader;

public class JPEGImageReader extends AwtImageReader {

    public JPEGImageReader(final ImageReaderSpi imageReaderSpi) {
        super(imageReaderSpi);
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new JPEGImageReadParam();
    }
}
