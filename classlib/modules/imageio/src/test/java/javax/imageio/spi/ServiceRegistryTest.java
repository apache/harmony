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

package javax.imageio.spi;

import java.util.Arrays;
import java.util.Locale;

import junit.framework.TestCase;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

public class ServiceRegistryTest extends TestCase {
    
    public void testRegistryServiceProvider() throws Exception {
        Class[] CATEGORIES = new Class[] {
                ImageWriterSpi.class, ImageReaderSpi.class
        };
        
        ServiceRegistry registry = new ServiceRegistry(Arrays.<Class<?>> asList(CATEGORIES).iterator());
        
        SampleImageReaderSpi spiA = new SampleImageReaderSpi();  
        SampleImageReaderSpi spiB = new SampleImageReaderSpi();
        
        assertTrue("registerServiceProvider() returns incorrect value for the empty registry",
            registry.registerServiceProvider(spiA, CATEGORIES[1]));
        assertFalse("registerServiceProvider() returns incorrect value if a provider of the same class was previously registered",
            registry.registerServiceProvider(spiB, CATEGORIES[1]));
    }
}

class SampleImageReaderSpi extends ImageReaderSpi {
    SampleImageReaderSpi() {
        super("sample vendor", "1.0", new String[] { "sample" },
            null, null, SampleImageReaderSpi.class.getName(),
            STANDARD_INPUT_TYPE, null, false, null, null,
            null, null, false, null, null, null, null);
    }

    public boolean canDecodeInput(Object source) {
        return false;
    }

    public ImageReader createReaderInstance(Object extension) {
        return null;
    }

    public String getDescription(Locale locale) {
        return null;
    }
}

