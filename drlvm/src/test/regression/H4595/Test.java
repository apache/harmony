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

package org.apache.harmony.drlvm.tests.regression.h4595;

import junit.framework.*;

public class Test extends TestCase {


/// test 1
    class A {
        void in(long l1, long l2, long l3){
            assertEquals(10000002, l1);
        }
    }

    XGraphics2D g2d = new XGraphics2D();
    A a = new A();

    public void test1() {
        long l = get();
        before(l);
        a.in(g2d.display, g2d.drawable, g2d.imageGC);
    }

    long get(){return 4;}
    void before(long l){ /*do nothing*/}




/// test 2
    public void test2() {
        long[] x = new long[] { g2d.drawable};
        //check that no exception is thrown
    }

/// test 3
    
    double d = 30d;
    static Test t = new Test();
    static long [] arr = new long [] {6, 25, 50};
    
    public void test3() {
        double v = t3();
        assertEquals(v, 5d);
    }

    double t3() {
        double d1 = t.d / arr[0];
        return d1;
    }

}

class XGraphics2D {
    long drawable = 10000001;
    long display  = 10000002;
    long imageGC  = 10000003;
}
