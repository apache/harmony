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

package org.apache.harmony.drlvm.tests.regression.h5127;

import junit.framework.TestCase;
import java.util.*;

class IntConversionTest {

    IntConversionTest(int i, float f, double d) { intValue = i; floatValue = f; doubleValue = d;}

    int     intValue;
    float   floatValue;
    double  doubleValue;
}

class LongConversionTest {

    LongConversionTest(long l, float f, double d) { longValue = l; floatValue = f; doubleValue = d;}

    long    longValue;
    float   floatValue;
    double  doubleValue;
}



public class Test extends TestCase {

    List<IntConversionTest> intTests = new ArrayList<IntConversionTest>();
    List<LongConversionTest> longTests = new ArrayList<LongConversionTest>();

    public void testMe() {
        prepareTests();
        runTests();
    }


    void prepareTests() {
        intTests.add(new IntConversionTest(0, (float)0.0, 0.0));
        intTests.add(new IntConversionTest(1, (float)1.0, 1.0));
        intTests.add(new IntConversionTest(-1, (float)-1.0, -1.0));
        intTests.add(new IntConversionTest(Integer.MAX_VALUE, 2.14748365E9F, 2.147483647E9));
        intTests.add(new IntConversionTest(Integer.MIN_VALUE, -2.14748365E9F, -2.147483648E9));



        longTests.add(new LongConversionTest(0L, (float)0.0, 0.0));
        longTests.add(new LongConversionTest(1L, (float)1.0, 1.0));
        longTests.add(new LongConversionTest(-1L, (float)-1.0, -1.0));
        longTests.add(new LongConversionTest(Long.MAX_VALUE, 9.223372E18F, 9.223372036854776E18));
        longTests.add(new LongConversionTest(Long.MIN_VALUE, -9.223372E18F, -9.223372036854776E18));


        longTests.add(new LongConversionTest((long)Integer.MAX_VALUE, 2.14748365E9F, 2.147483647E9));
        longTests.add(new LongConversionTest((long)Integer.MIN_VALUE, -2.14748365E9F, -2.147483648E9));

        longTests.add(new LongConversionTest((long)Integer.MAX_VALUE+1, 2.14748365E9F, 2.147483648E9));
        longTests.add(new LongConversionTest((long)Integer.MIN_VALUE+1, -2.14748365E9F, -2.147483647E9));

        longTests.add(new LongConversionTest((long)Integer.MAX_VALUE-1, 2.14748365E9F, 2.147483646E9));
        longTests.add(new LongConversionTest((long)Integer.MIN_VALUE-1, -2.14748365E9F, -2.147483649E9));


    }


    void runTests() {
        for(IntConversionTest t: intTests) {
            check(t.floatValue, t.intValue);
            check(t.doubleValue, t.intValue);
        }

        for(LongConversionTest t: longTests) {
            check(t.floatValue, t.longValue);
            check(t.doubleValue, t.longValue);
        }
    }


    void check(float f, int i) {
        assertEquals(f, (float)i);
    }

    void check(float f, long l) {
        assertEquals(f, (float)l);
    }

    void check(double d, int i) {
        assertEquals(d,(double)i);
    }

    void check(double d, long l) {
        assertEquals(d,(double)l);
    }


}
