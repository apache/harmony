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
import java.util.Iterator;
import java.util.Locale;

import junit.framework.TestCase;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

public class ServiceRegistryTest extends TestCase {

    public void testLookupProviders() {
        // lookup from a correct provider-configuration file
        Iterator it = ServiceRegistry.lookupProviders(CorrectProviderConfiguration.class); 
        assertEquals("Failed to find provider and instantiate it",
                "class javax.imageio.spi.CorrectProviderConfiguration", 
                it.next().getClass().toString());
       
        // lookup from incorrect provider-configuration file
        try {
            it = ServiceRegistry.lookupProviders(IncorrectProviderConfiguration.class);
            fail("ServiceConfigurationError expected");
        } catch (Error e) {
            // Ok
        }
    }
    
    public void testDeregisterAll() {
        Class[] CATEGORIES = new Class[] {
                ImageReaderSpi.class };
        
        ServiceRegistry registry = new ServiceRegistry(Arrays.<Class<?>> asList(CATEGORIES).iterator());
        
        ImageReaderSpi reader1 = new Reader1Spi();
        ImageReaderSpi reader2 = new Reader2Spi();
        
        // Register two providers
        registry.registerServiceProvider(reader1, CATEGORIES[0]);
        registry.registerServiceProvider(reader2, CATEGORIES[0]);
        
        registry.deregisterAll(ImageReaderSpi.class);
        assertFalse("Reader1 is still regitered, deregisterAll(Class) failed",registry.contains(reader1));
        assertFalse("Reader2 is still regitered, deregisterAll(Class) failed",registry.contains(reader2));
        
        // Re-register two providers
        registry.registerServiceProvider(reader1, CATEGORIES[0]);
        registry.registerServiceProvider(reader2, CATEGORIES[0]);
        
        registry.deregisterAll();
        assertFalse("Reader1 is still regitered, deregisterAll() failed",registry.contains(reader1));
        assertFalse("Reader2 is still regitered, deregisterAll() failed",registry.contains(reader2));
    }
    
	public void testContains() {
		Class[] CATEGORIES = new Class[] {
                ImageReaderSpi.class };
        
        ServiceRegistry registry = new ServiceRegistry(Arrays.<Class<?>> asList(CATEGORIES).iterator());
        
        ImageReaderSpi reader1 = new SampleImageReaderSpi();
        ImageReaderSpi reader2 = new SampleImageReaderSpi();
        
        registry.registerServiceProvider(reader1, CATEGORIES[0]);
        
        assertTrue("Failed to check if reader1 registered", registry.contains(reader1));
        assertFalse("Failed to check if reader2 registered", registry.contains(reader2));        
	}
	
    public void testGetServiceProviders() {
        Class[] CATEGORIES = new Class[] {
                ImageReaderSpi.class };
        
        ServiceRegistry registry = new ServiceRegistry(Arrays.<Class<?>> asList(CATEGORIES).iterator());
        
        ImageReaderSpi reader = new SampleImageReaderSpi();
        ImageReaderSpi reader1 = new Reader1Spi();
        ImageReaderSpi reader2 = new Reader2Spi();
        
        // Add 3 different providers to the registry
        registry.registerServiceProvider(reader, CATEGORIES[0]);
        registry.registerServiceProvider(reader1, CATEGORIES[0]);
        registry.registerServiceProvider(reader2, CATEGORIES[0]);
        
        // Add a different type of provider to the category
        ImageWriterSpi writer = new SampleImageWriterSpi();
        try {
            registry.registerServiceProvider(writer, CATEGORIES[0]);
            fail("ClassCastException expected");
        }
        catch (ClassCastException expected) {
            // Ok
        }
        
        // Set ordering between these providers
        // reader2 > reader1 >  reader
        assertTrue("Failed to set ordering: reader2 > reader1",
            registry.setOrdering(CATEGORIES[0], reader2, reader1));
        assertTrue("Failed to set ordering: reader1 > reader",
            registry.setOrdering(CATEGORIES[0], reader1, reader));
        
        // Return false if this ordering has already been set
        assertFalse("Failed to check if the ordering reader1 > reader has been set",
            registry.setOrdering(CATEGORIES[0], reader1, reader));
        
        // If two providers are the same
        try {
        	registry.setOrdering(CATEGORIES[0], reader, reader);
        	fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException expected) {
            // Ok
        }
        
        // If either provider is null
        try {
        	registry.setOrdering(CATEGORIES[0], null, reader);
        	fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException expected) {
            // Ok
        }
        
        // Get the iterator of sorted providers
        Iterator it = registry.getServiceProviders(CATEGORIES[0], true);
        
        // Verify the order
        assertEquals("Failed to return reader2", it.next(), reader2);
        assertEquals("Failed to return reader1", it.next(), reader1);
        assertEquals("Failed to return reader", it.next(), reader);
        
        // The iterator should be able to run more than once
        it = registry.getServiceProviders(CATEGORIES[0], true);
        
        // Verify the order
        assertEquals("Failed to return reader2", it.next(), reader2);
        assertEquals("Failed to return reader1", it.next(), reader1);
        assertEquals("Failed to return reader", it.next(), reader);

        // Unset orderings
        assertTrue("Failed to unset ordering: reader2 > reader1", registry.unsetOrdering(CATEGORIES[0], reader2, reader1));
        assertTrue("Failed to unset ordering: reader1 > reader", registry.unsetOrdering(CATEGORIES[0], reader1, reader));
        
        // Return false if this ordering is not set
        assertFalse("Failed to check if the ordering is not set", registry.unsetOrdering(CATEGORIES[0], reader2, reader));
    }
    
    public void testGetServiceProviderByClass() {
    	Class[] CATEGORIES = new Class[] {
                ImageReaderSpi.class };
        
        ServiceRegistry registry = new ServiceRegistry(Arrays.<Class<?>> asList(CATEGORIES).iterator());
        ImageReaderSpi reader = new SampleImageReaderSpi();
        registry.registerServiceProvider(reader, CATEGORIES[0]);
        
        ImageReaderSpi provider = registry.getServiceProviderByClass(SampleImageReaderSpi.class);
        assertEquals(reader, provider);
    }

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
    
    @SuppressWarnings("unchecked")
    public void testDeregisterServiceProvider() throws Exception {
        Class[] CATEGORIES = new Class[] {
                javax.imageio.spi.ImageReaderSpi.class,
                javax.imageio.spi.SampleImageReaderSpi.class};

        ServiceRegistry registry = new ServiceRegistry(Arrays.<Class<?>> asList(CATEGORIES).iterator());

        SampleImageReaderSpi spi = new SampleImageReaderSpi();
        
        // Test deregisterServiceProvider(Object, Class)
        registry.registerServiceProvider(spi, CATEGORIES[0]);
        
        assertTrue("deregisterServiceProvider(Object, Class) returns incorrect value for a registered provider ",
                registry.deregisterServiceProvider(spi, CATEGORIES[0]));
        
        assertFalse("deregisterServiceProvider(Object, Class) returns incorrect value for a unregistered provider",
                registry.deregisterServiceProvider(spi, CATEGORIES[0]));
        
        // Test deregisterServiceProvider(Object)
        registry.registerServiceProvider(spi, CATEGORIES[0]);
        registry.registerServiceProvider(spi, CATEGORIES[1]);

        registry.deregisterServiceProvider(spi);
        
        assertFalse("deregisterServiceProvider(Object) failed to remove all providers",
                registry.deregisterServiceProvider(spi, CATEGORIES[0]));
        assertFalse("deregisterServiceProvider(Object) failed to remove all providers",
                registry.deregisterServiceProvider(spi, CATEGORIES[1]));
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

class Reader1Spi extends SampleImageReaderSpi {
}

class Reader2Spi extends SampleImageReaderSpi {
}

class SampleImageWriterSpi extends ImageWriterSpi {
    SampleImageWriterSpi() {
        super("sample vendor", "1.0", new String[] { "sample" },
            null, null, SampleImageReaderSpi.class.getName(),
            STANDARD_OUTPUT_TYPE, null, false, null, null,
            null, null, false, null, null, null, null);
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return false;
    }

    public ImageWriter createWriterInstance(Object extension) {
        return null;
    }

    public String getDescription(Locale locale) {
        return null;
    }
}

