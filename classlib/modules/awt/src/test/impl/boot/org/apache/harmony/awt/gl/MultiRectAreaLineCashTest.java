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

public class MultiRectAreaLineCashTest extends MultiRectAreaTestCase {

    static int CASH_SIZE = 100;

    MultiRectArea.LineCash area;

    static { 
        SERIALIZATION_TEST = false;
    }
    
    public MultiRectAreaLineCashTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        area = new MultiRectArea.LineCash(CASH_SIZE);
    }

    @Override
    protected void tearDown() throws Exception {
        area = null;
        super.tearDown();
    }

    //      1234567
    // 10   ##
    // 11   ##
    // 12   ##
    // 13   ## ##
    // 14   ## ##
    // 15      ####
    // 16   ## ##
    public void testCash() {
        checkArea("LineCash1", area, new int[]{});

        area.setLine(10);
        area.addLine(new int[]{1, 2}, 2);
        checkArea("LineCash2", area, new int[]{1, 10, 2, 10});

        area.addLine(new int[]{1, 2}, 2);
        checkArea("LineCash3", area, new int[]{1, 10, 2, 11});

        area.addLine(new int[]{1, 2}, 2);
        checkArea("LineCash4", area, new int[]{1, 10, 2, 12});

        area.addLine(new int[]{1, 2, 4, 5}, 4);
        checkArea("LineCash5", area, new int[]{
                1, 10, 2, 13,
                4, 13, 5, 13});

        area.addLine(new int[]{1, 2, 4, 5}, 4);
        checkArea("LineCash6", area, new int[]{
                1, 10, 2, 14,
                4, 13, 5, 14});

        area.addLine(new int[]{4, 5, 6, 7}, 4);
        checkArea("LineCash7", area, new int[]{
                1, 10, 2, 14,
                4, 13, 5, 15,
                6, 15, 7, 15});

        /// ????????
        area.addLine(new int[]{1, 2, 4, 5}, 4);
        checkArea("LineCash8", area, new int[]{
                1, 10, 2, 14,
                4, 13, 5, 16,
                6, 15, 7, 15,
                1, 16, 2, 16});

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MultiRectAreaLineCashTest.class);
    }

}
