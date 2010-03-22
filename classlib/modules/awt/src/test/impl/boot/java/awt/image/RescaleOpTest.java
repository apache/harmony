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


package java.awt.image;

import junit.framework.TestCase;

public class RescaleOpTest extends TestCase {
    // Regression test for HARMONY-2691
    public void testRescaleOp() throws IllegalArgumentException {
        new RescaleOp(new float[] {}, new float[] {0.75F}, null);
    }
    // Regression test for HARMONY-2691
    public void testRescaleOp2() throws IllegalArgumentException {
        new RescaleOp(new float[] {0.75F}, new float[] {}, null);
    }

    // A regression test for harmony-2689
    public void test_getOffsetsNullLength()
    {
        RescaleOp res = new RescaleOp(new float[1],new float[2],null);
        assertEquals(res.getOffsets(new float[] {}).length, 0);
    }
    
    // A regression test for harmony-2689
    public void test_getScaleFactorsNullLength()
    {
        RescaleOp res = new RescaleOp(new float[1],new float[2],null);
        assertEquals(res.getScaleFactors(new float[] {}).length, 0);
    }
}
