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

import javax.sql.rowset.spi.SyncProviderException;
import javax.sql.rowset.spi.SyncResolver;

import junit.framework.TestCase;

public class SyncProviderExceptionTest extends TestCase {

    /**
     * @add tests
     *      {@link javax.sql.rowset.spi.SyncProviderException#SyncProviderException()}
     * @add tests
     *      {@link javax.sql.rowset.spi.SyncProviderException#SyncProviderException(String)}
     */
    public void test_constructor() {
        SyncProviderException ex = new SyncProviderException();
        assertNull(ex.getMessage());
        ex = new SyncProviderException("test");
        assertEquals("test", ex.getMessage());
        ex = new SyncProviderException((String) null);
        assertNull(ex.getMessage());
    }

    /**
     * @add tests
     *      {@link javax.sql.rowset.spi.SyncProviderException#SyncProviderException(javax.sql.rowset.spi.SyncResolver)}
     */
    public void test_constructor_Ljavax_sql_rowset_spi_SyncResolver() {
        try {
            new SyncProviderException((SyncResolver) null);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @add tests
     *      {@link javax.sql.rowset.spi.SyncProviderException#setSyncResolver(SyncResolver)}
     * @add tests
     *      {@link javax.sql.rowset.spi.SyncProviderException#getSyncResolver()}
     */
    public void test_setSyncResolver_Ljavax_sql_rowset_spi_SyncResolver() {
        SyncProviderException ex = new SyncProviderException();
        try {
            ex.setSyncResolver(null);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertNull(ex.getSyncResolver());
    }

}
