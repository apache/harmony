/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.sun.image.codec.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

class JPEGImageEncoderImpl implements JPEGImageEncoder {
    private final OutputStream out;

    JPEGImageEncoderImpl(OutputStream out) {
        super();
        this.out = out;
    }

    public void encode(BufferedImage bi) throws IOException, ImageFormatException {
        ImageIO.write(bi, "jpg", out);
    }

    public void encode(BufferedImage bi, JPEGEncodeParam jep) throws IOException,
            ImageFormatException {
    }

    public void encode(Raster r) throws IOException, ImageFormatException {

    }

    public void encode(Raster r, JPEGEncodeParam jep) throws IOException, ImageFormatException {

    }
}
