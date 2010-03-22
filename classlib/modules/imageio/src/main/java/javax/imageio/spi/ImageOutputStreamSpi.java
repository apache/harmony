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

import javax.imageio.stream.ImageOutputStream;

import org.apache.harmony.luni.util.NotImplementedException;

import java.io.IOException;
import java.io.File;

public abstract class ImageOutputStreamSpi extends IIOServiceProvider implements
        RegisterableService {
    protected Class<?> outputClass;

    protected ImageOutputStreamSpi() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public ImageOutputStreamSpi(String vendorName, String version, Class<?> outputClass) {
        super(vendorName, version);
        this.outputClass = outputClass;
    }

    public Class<?> getOutputClass() {
        return outputClass;
    }

    public boolean canUseCacheFile() {
        return false; // def
    }

    public boolean needsCacheFile() {
        return false; // def
    }

    public ImageOutputStream createOutputStreamInstance(Object output) throws IOException {
        return createOutputStreamInstance(output, true, null);
    }

    public abstract ImageOutputStream createOutputStreamInstance(Object output,
            boolean useCache, File cacheDir) throws IOException;
}
