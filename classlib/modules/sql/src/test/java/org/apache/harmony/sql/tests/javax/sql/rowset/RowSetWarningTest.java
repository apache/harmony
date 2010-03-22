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
package org.apache.harmony.sql.tests.javax.sql.rowset;

import javax.sql.rowset.RowSetWarning;

import junit.framework.TestCase;

public class RowSetWarningTest extends TestCase {

    /**
     * Test method for {@link javax.sql.rowset.RowSetWarning#RowSetWarning()}.
     */

    public void test_constructor() {
        RowSetWarning warn = new RowSetWarning();
        assertNull(warn.getMessage());
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.RowSetWarning#RowSetWarning(java.lang.String)}.
     */

    public void test_constructor_Ljava_lang_String() {
        RowSetWarning war = new RowSetWarning("test");
        assertEquals("test", war.getMessage());
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.RowSetWarning#RowSetWarning(java.lang.String, java.lang.String)}
     */

    public void test_constructor_Ljava_lang_StringLjava_lang_String() {
        RowSetWarning warn = new RowSetWarning("test", "testState");
        assertEquals("test", warn.getMessage());
        assertEquals("testState", warn.getSQLState());
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.RowSetWarning#RowSetWarning(java.lang.String, java.lang.String, int)}
     */

    public void test_constructor_Ljava_lang_StringLjava_lang_StringI() {
        RowSetWarning warn = new RowSetWarning("test", "testState", 0);
        assertEquals("test", warn.getMessage());
        assertEquals("testState", warn.getSQLState());
        assertEquals(0, warn.getErrorCode());
    }

    /**
     * Test method for {@link javax.sql.rowset.RowSetWarning#getNextWarning()}.
     * {@link javax.sql.rowset.RowSetWarning#setNextWarning(javax.sql.rowset.RowSetWarning)}
     */

    public void test_set_getNextWarning() {
        RowSetWarning warn = new RowSetWarning();
        warn.setNextWarning(new RowSetWarning("test"));
        assertEquals("test", warn.getNextWarning().getMessage());
    }

}
