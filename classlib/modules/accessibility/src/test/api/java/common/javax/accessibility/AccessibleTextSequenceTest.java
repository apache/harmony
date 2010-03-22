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

public class AccessibleTextSequenceTest extends TestCase {
    /**
     * add tests {@link javax.accessibility.AccessibleTextSequence}
     */
    public void test_constructor() {
        class MockAccessibleTextSequence extends AccessibleTextSequence {
            public MockAccessibleTextSequence() {
                super();
            }
        }
        MockAccessibleTextSequence mock = new MockAccessibleTextSequence();
        assertEquals("init error", 0, mock.endIndex);
        assertEquals("init error", 0, mock.startIndex);
        assertNull("init error", mock.text);
    }
}
