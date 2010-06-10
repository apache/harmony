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

