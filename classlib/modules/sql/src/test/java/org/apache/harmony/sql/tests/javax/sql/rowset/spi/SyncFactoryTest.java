/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.harmony.sql.tests.javax.sql.rowset.spi;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncFactoryException;
import javax.sql.rowset.spi.SyncProvider;

import junit.framework.TestCase;

public class SyncFactoryTest extends TestCase {

    /**
     * Test method for
     * {@link javax.sql.rowset.spi.SyncFactory#registerProvider(java.lang.String)}
     * and
     * {@link javax.sql.rowset.spi.SyncFactory#unregisterProvider(java.lang.String)}
     * 
     * @throws SyncFactoryException
     */
    public void test_registerProvider_Ljava_lang_String()
            throws SyncFactoryException {
        final String proID = "org.apache.harmony.sql.tests.TestProvider";
        SyncFactory.registerProvider(proID);
        Enumeration<SyncProvider> providers = SyncFactory
                .getRegisteredProviders();
        boolean ifContains = false;
        while (providers.hasMoreElements()) {
            if (providers.nextElement().getProviderID().equals(proID)) {
                ifContains = true;
                break;
            }
        }
        assertTrue("register provider error", ifContains);
        SyncFactory.unregisterProvider(proID);
        providers = SyncFactory.getRegisteredProviders();
        ifContains = false;
        while (providers.hasMoreElements()) {
            if (providers.nextElement().getProviderID().equals(proID)) {
                ifContains = true;
                break;
            }
        }
        assertFalse("unregister provider error", ifContains);
        try {
            SyncFactory.registerProvider(null);
            fail("should throw SyncFactoryException");
        } catch (SyncFactoryException e) {
            // expected
        }
        try {
            SyncFactory.registerProvider("");
            fail("should throw SyncFactoryException");
        } catch (SyncFactoryException e) {
            // expected
        }
        try {
            SyncFactory.unregisterProvider("test");
            fail("should throw SyncFactoryException");
        } catch (SyncFactoryException e) {
            // expected
        }
        try {
            SyncFactory.unregisterProvider(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

    }

    /**
     * Test method for {@link javax.sql.rowset.spi.SyncFactory#getSyncFactory()}
     */
    public void test_getSyncFactory() {
        SyncFactory fac1 = SyncFactory.getSyncFactory();
        SyncFactory fac2 = SyncFactory.getSyncFactory();
        assertSame("SyncFactory instance should be Singleton", fac1, fac2);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.spi.SyncFactory#getInstance(java.lang.String)}.
     * 
     * @throws SyncFactoryException
     */
    public void test_getInstance_Ljava_lang_String()
            throws SyncFactoryException {
        Enumeration<SyncProvider> providers = SyncFactory
                .getRegisteredProviders();
        SyncProvider expected = providers.nextElement();
        String id = expected.getProviderID();
        SyncProvider provider = SyncFactory.getInstance(id);
        assertEquals("getInstance error", expected.getVersion(), provider
                .getVersion());
        provider = SyncFactory.getInstance("nonExist");// should return default
        assertNotNull(provider);
        try {
            provider = SyncFactory.getInstance(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.spi.SyncFactory#setLogger(java.util.logging.Logger)}
     * {@link javax.sql.rowset.spi.SyncFactory#getLogger()} .
     * 
     * @throws SyncFactoryException
     */
    public void test_setLogger_Ljava_util_logging_Logger()
            throws SyncFactoryException {
        Logger logger = Logger.getAnonymousLogger();
        SyncFactory.setLogger(logger);
        assertEquals(logger, SyncFactory.getLogger());
        SyncFactory.setLogger(null);
        try {
            SyncFactory.getLogger();
            fail("should throw SyncFactoryException");
        } catch (SyncFactoryException e) {
            // expected
        }
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.spi.SyncFactory#setLogger(java.util.logging.Logger, java.util.logging.Level)}
     *
     * @throws SyncFactoryException
     */
    public void test_setLogger_Ljava_util_logging_LoggerLjava_util_logging_Level()
            throws SyncFactoryException {
        Logger logger = Logger.getAnonymousLogger();
        Level level = Level.parse("WARNING");
        SyncFactory.setLogger(logger, level);
        Logger actual = SyncFactory.getLogger();
        assertEquals("set logger or get logger error", logger, actual);
        assertEquals("set logger error in set level", level, logger.getLevel());
        try {
            SyncFactory.setLogger(null, level);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        SyncFactory.setLogger(logger, null);
        assertNull(SyncFactory.getLogger().getLevel());
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.spi.SyncFactory#setJNDIContext(javax.naming.Context)}
     * 
     * @throws NamingException
     * @throws SyncFactoryException
     */
    public void test_setJNDIContext_Ljavax_naming_Context()
            throws NamingException, SyncFactoryException {
        try {
            SyncFactory.setJNDIContext(null);
            fail("Should throw SyncFactoryException");
        } catch (SyncFactoryException e) {
            // expected
        }
        SyncFactory.setJNDIContext(new InitialContext());
    }
}
