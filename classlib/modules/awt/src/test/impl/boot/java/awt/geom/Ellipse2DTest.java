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
package java.awt.geom;

public class Ellipse2DTest extends ShapeTestCase {

    public Ellipse2DTest(String name) {
        super(name);
//        filterImage = createFilter("^(ellipse).*([.]ico)$", "(.*)((affine)|(flat)|(bounds))(.*)");
        filterShape = createFilter("^(ellipse).*([.]shape)$", null);
    }

    public void testGetPathIteratorEmpty() {
        // Regression test HARMONY-1585
        Ellipse2D e = new Ellipse2D.Double();
        PathIterator p = e.getPathIterator(null);
        checkPathMove(p, false, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathClose(p, true);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(Ellipse2DTest.class);
    }

}
