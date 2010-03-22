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

package org.apache.harmony.drlvm.tests.regression.h2874;

import junit.framework.TestCase;



class aeo0 {
    private long num;
    public aeo0() { num = 0; }
    public void inc(aeo1 i) { num++; }
    public void inc1() { num++; }
    public long getNum() { return num; }
    public void reset() { num = 0; }
}
class aeo1 {
}

public class Test extends TestCase {
    static final long limit = 100000000;
    static aeo0 obj = new aeo0();

    public void test() {
        long before = 0, after = 0;
        for (int i = 0; i < 5; i++) {    
            obj.reset();
            before = System.currentTimeMillis();
            for (long k = 0; k < limit; k++ ) {
                dofc(k);
            }
            after = System.currentTimeMillis();
            System.out.println("Calls per millisecond: " + (obj.getNum() / (after - before)));
        }
    }
    static void dofc(long i) {
        aeo1 i1 = new aeo1();
        obj.inc1();
        if (i<0) {
            obj.inc(i1);
        }
    }
}
