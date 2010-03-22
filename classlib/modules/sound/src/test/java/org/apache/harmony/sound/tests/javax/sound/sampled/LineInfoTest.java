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

package org.apache.harmony.sound.tests.javax.sound.sampled;

import javax.sound.sampled.Line;

import junit.framework.TestCase;

public class LineInfoTest extends TestCase {

    public void testMatches() {

        Line.Info info1 = new Line.Info("aaaa".getClass());
        Line.Info info2 = new Line.Info(new Object().getClass());

        assertTrue(info1.matches(info1));
        assertFalse(info1.matches(info2));
        assertTrue(info2.matches(info1));
    }

}
