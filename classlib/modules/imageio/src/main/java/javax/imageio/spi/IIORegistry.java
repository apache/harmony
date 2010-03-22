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
/**
 * @author Rustem V. Rafikov
 */
package javax.imageio.spi;

import java.util.Arrays;

import org.apache.harmony.x.imageio.plugins.gif.GIFImageReaderSpi;
import org.apache.harmony.x.imageio.plugins.jpeg.JPEGImageReaderSpi;
import org.apache.harmony.x.imageio.plugins.jpeg.JPEGImageWriterSpi;
import org.apache.harmony.x.imageio.plugins.png.PNGImageReaderSpi;
import org.apache.harmony.x.imageio.plugins.png.PNGImageWriterSpi;
import org.apache.harmony.x.imageio.spi.FileIISSpi;
import org.apache.harmony.x.imageio.spi.FileIOSSpi;
import org.apache.harmony.x.imageio.spi.InputStreamIISSpi;
import org.apache.harmony.x.imageio.spi.OutputStreamIOSSpi;
import org.apache.harmony.x.imageio.spi.RAFIISSpi;
import org.apache.harmony.x.imageio.spi.RAFIOSSpi;

public final class IIORegistry extends ServiceRegistry {

    private static IIORegistry   instance;

    private static final Class[] CATEGORIES = new Class[] {
                    javax.imageio.spi.ImageWriterSpi.class,
                    javax.imageio.spi.ImageReaderSpi.class,
                    javax.imageio.spi.ImageInputStreamSpi.class,
                    // javax.imageio.spi.ImageTranscoderSpi.class,
                    javax.imageio.spi.ImageOutputStreamSpi.class };

    private IIORegistry() {
        super(Arrays.<Class<?>> asList(CATEGORIES).iterator());
        registerBuiltinSpis();
        registerApplicationClasspathSpis();
    }

    private void registerBuiltinSpis() {
        registerServiceProvider(new JPEGImageWriterSpi());
        registerServiceProvider(new JPEGImageReaderSpi());
        registerServiceProvider(new PNGImageReaderSpi());
        registerServiceProvider(new PNGImageWriterSpi());
        registerServiceProvider(new GIFImageReaderSpi());
        registerServiceProvider(new FileIOSSpi());
        registerServiceProvider(new FileIISSpi());
        registerServiceProvider(new RAFIOSSpi());
        registerServiceProvider(new RAFIISSpi());
        registerServiceProvider(new OutputStreamIOSSpi());
        registerServiceProvider(new InputStreamIISSpi());
    }

    public static IIORegistry getDefaultInstance() {
        // TODO implement own instance for each ThreadGroup (see also
        // ThreadLocal)
        synchronized (IIORegistry.class) {
            if (instance == null) {
                instance = new IIORegistry();
            }
            return instance;
        }
    }

    public void registerApplicationClasspathSpis() {
        // -- TODO implement for non-builtin plugins
    }
}
