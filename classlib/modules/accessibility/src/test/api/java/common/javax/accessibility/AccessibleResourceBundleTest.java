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

package javax.accessibility;

import junit.framework.TestCase;

public class AccessibleResourceBundleTest extends TestCase {

    @SuppressWarnings("deprecation")
    public void testGetContents() {
        AccessibleResourceBundle resBundle = new AccessibleResourceBundle();
        Object[][] contents = resBundle.getContents();
        assertNotNull(contents);
        assertTrue(contents.length > 0);
        
        assertEquals(2, contents[10].length);

        contents[10] = new Object[] {"a", "b"};
        Object[][] contents2 = resBundle.getContents();
        assertSame(contents[10], contents2[10]);
    }
}

