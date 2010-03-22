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
 * Microbenchmark on scalar replacement optimization (virtual method test).
 * To see the effect, tests need to be run with the following switches:
 * -Xem:server -XX:jit.SD2_OPT.arg.optimizer.escape=on
 * -Xem:server -XX:jit.SD2_OPT.arg.optimizer.escape=off
 */

class Cls1 { 
    private long num;
    public Cls1() { num = 0; }
    public void inc(Integer i) { num= i+1; }
    public long getNum() { return num; }
    public void reset() { num = 0; }
}

public class test1 {
    static final long limit = 100000000;
    static Cls1 obj = new Cls1();

    public static void main(String[] args) {
        long before = 0, after = 0;
        for (int i = 0; i < 5; i++) {    
            obj.reset();
            before = System.currentTimeMillis();
            for (long k = 0; k < limit; k++ ) {
                dovc(k);
            }
            after = System.currentTimeMillis();
            System.out.println("Calls per millisecond: " + (obj.getNum() / (after - before)));
        }
    }  
    static void dovc(long i) {
        Integer i1 = new Integer((int)i);
        obj.inc(i1);
    }
}
