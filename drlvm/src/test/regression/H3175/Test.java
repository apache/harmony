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

package org.apache.harmony.drlvm.tests.regression.h3175;

import junit.framework.TestCase;

public class Test extends TestCase {

    static int i1,i2;
    static byte[] b1, b2;


    public void testSwitch1() throws Exception {
        blit();
    }
    static void blit() {
        int dp=0;
        switch (i1) {
            case 1: dp=0; break;
            case 7: break;
            case 6: break;
            case 2: break;
        }              
    }






    public void testSwitch2() throws Exception {
        blit2();
    }

     static void blit2() {
        byte[] destData=b2;
        byte[] alphaData=b1;
        int dtype=i1;
        int destAlphaShift = i2;
        int alpha = 0;
        int dy = 0;
        for (; dy > 0; --dy){
            switch (dy) {
                case 3: dy = destData[0]; break;
                case 8: dy = 0; break;
                case 2: dy = 0; break;
                case 5:
                    int i = 0;
                    while (i < alpha) {
                        if ((i == alphaData[0]) && (i == alphaData[2])) {}
                    }
                    break;
            }
            final int data = 0  << destAlphaShift;
            switch (dtype) {
                case 1: alpha = data;  break;
                case 4: destData[0] = 0; 
                case 5: destData[1] = (byte)data; break;
            }
        }
    }
}

