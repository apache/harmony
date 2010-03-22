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

import java.awt.*;

public class ConvolveOpTest extends TestCase {
    class RR extends Raster {
        public RR (SampleModel sampleModel, Point origin) {
            super(sampleModel, origin);
        }
    }

    // Regression test
    public void testFilter() {
        float[] array0 = new float[71];
        Kernel localKernel = new Kernel(8, 7, array0);
        ConvolveOp localConvolveOp = new ConvolveOp(localKernel);
        int[] array1 = new int[704];
        ComponentSampleModel localComponentSampleModel = new ComponentSampleModel(2,43970,127,4,7,array1);
        Point localPoint = new Point(5, 9);
        RR localRaster = new RR(localComponentSampleModel, localPoint);
        try {
            localConvolveOp.filter(localRaster, (WritableRaster) null);
            fail("ImagingOpException expected, but not thrown");
        } catch (NegativeArraySizeException unexpectedException) {
            fail(unexpectedException + " was thrown");
        } catch (ImagingOpException expectedException) {
            // expected
        }
    }
}
