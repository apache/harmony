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
/**
 * @author Denis M. Kishenko
 */
package org.apache.harmony.awt.gl;

import org.apache.harmony.awt.gl.MultiRectArea;

public class MultiRectAreaRectCashTest extends MultiRectAreaTestCase {

    MultiRectArea.RectCash area;

    static { 
        SERIALIZATION_TEST = false;
    }

    public MultiRectAreaRectCashTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        area = new MultiRectArea.RectCash();
    }

    @Override
    protected void tearDown() throws Exception {
        area = null;
        super.tearDown();
    }

    //     1234567
    // 1
    // 2   22233
    // 3   22244
    // 4   222
    // 5   555 666
    // 6   555 666
    // 7
    public void testCash() {
        checkArea("RectCash1", area, new int[]{});

        area.addRectCashed(1, 2, 3, 4);
        checkArea("RectCash2", area, new int[]{1, 2, 3, 4});

        area.addRectCashed(4, 2, 5, 2);
        checkArea("RectCash3", area,
            new int[]{
                1, 2, 3, 4,
                4, 2, 5, 2});

        area.addRectCashed(4, 3, 5, 3);
        checkArea("RectCash4", area,
            new int[]{
                1, 2, 3, 4,
                4, 2, 5, 2,
                4, 3, 5, 3});

        area.addRectCashed(1, 5, 3, 6);
        checkArea("RectCash5", area,
            new int[]{
                1, 2, 3, 4,
                4, 2, 5, 2,
                4, 3, 5, 3,
                1, 5, 3, 6});

        area.addRectCashed(5, 5, 7, 6);
        checkArea("RectCash6", area,
            new int[]{
                1, 2, 3, 4,
                4, 2, 5, 2,
                4, 3, 5, 3,
                1, 5, 3, 6,
                5, 5, 7, 6});
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MultiRectAreaRectCashTest.class);
    }

}
