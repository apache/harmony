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
package org.apache.harmony.sql.tests.internal.rowset;

import java.util.Enumeration;

import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncFactoryException;
import javax.sql.rowset.spi.SyncProvider;

import junit.framework.TestCase;

public class SyncFactoryTest extends TestCase {

    /**
     * Test for SyncFactory.getRegisteredProviders(). Regression for
     * HARMONY-6345.
     * 
     * @throws SyncFactoryException
     */
    public void testGetRegisteredProviders() throws SyncFactoryException {
        Enumeration<SyncProvider> providers = SyncFactory
                .getRegisteredProviders();
        while (providers.hasMoreElements()) {
            SyncFactory.getInstance(providers.nextElement().getProviderID());
        }
    }
}
