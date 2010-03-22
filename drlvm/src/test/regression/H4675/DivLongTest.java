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

package org.apache.harmony.drlvm.tests.regression.h4675;

import junit.framework.TestCase;

public class DivLongTest extends TestCase {

    public void testLDIV_lowzero() {
        long test_long = 0x000000ff00000000L; 
        long res = test_long / test_long; 

        assertEquals(1, res);
    }

    /** HARMONY-4898 */
    public void testLDIV_min() {
        long l_min = Long.MIN_VALUE;
        long l_1 = -1;
        long res = l_min / l_1;

        assertEquals(Long.MIN_VALUE, res);
    }

    public void testLREM_min() {
        long l_min = Long.MIN_VALUE;
        long l_1 = -1;
        long res = l_min % l_1;

        assertEquals(0, res);
    }
}
