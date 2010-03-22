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

package org.apache.harmony.drlvm.tests.regression.h5480;

import java.util.Random;

import junit.framework.TestCase;

public class LooseMathTest extends TestCase {
    static {
        // Let lazy resolution (if any) ramp up
        Math.class.hashCode();
    }
    static Random rnd = new Random();

    public void testABS() throws Exception {
        int i = rnd.nextInt();
        assertEquals("int " + i, StrictMath.abs(i), Math.abs(i));
        long j = rnd.nextLong();
        assertEquals("long " + j, StrictMath.abs(j), Math.abs(j));
        float f = rnd.nextInt() * rnd.nextFloat();
        assertEquals("float " + f, StrictMath.abs(f), Math.abs(f));
        double d = rnd.nextInt() * rnd.nextDouble();
        assertEquals("d=" + d, StrictMath.abs(d), Math.abs(d));
    }

    public void testSQRT() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        d *= Math.signum(d);
        double ulp = Math.ulp(StrictMath.sqrt(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.sqrt(d), Math.sqrt(d), ulp);
    }

    public void testSIN() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        //x87 trigonometry is less accurate...
        double ulp = 1.e-9; //Math.ulp(StrictMath.sin(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.sin(d), Math.sin(d), ulp);
    }
    
    public void testASIN() throws Exception {
        double d = rnd.nextDouble();
        double ulp = Math.ulp(StrictMath.asin(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.asin(d), Math.asin(d), ulp);
    }

    public void testCOS() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        //x87 trigonometry is less accurate...
        double ulp = 1.e-9; //Math.ulp(StrictMath.cos(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.cos(d), Math.cos(d), ulp);
    }

    public void testACOS() throws Exception {
        double d = rnd.nextDouble();
        double ulp = Math.ulp(StrictMath.acos(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.acos(d), Math.acos(d), ulp);
    }

    public void testTAN() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        //x87 trigonometry is less accurate...
        double ulp = 1.e-9; //Math.ulp(StrictMath.tan(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.tan(d), Math.tan(d), ulp);
    }

    public void testATAN() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        double ulp = Math.ulp(StrictMath.atan(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.atan(d), Math.atan(d), ulp);
    }
    
    public void testEXP() throws Exception {
        double d = rnd.nextDouble();
        double ulp = Math.ulp(StrictMath.exp(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.exp(d), Math.exp(d), ulp);
    }
    
    public void testLOG() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        d *= Math.signum(d);
        double ulp = Math.ulp(StrictMath.log(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.log(d), Math.log(d), ulp);
    }

    public void testLOG10() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        d *= Math.signum(d);
        double ulp = Math.ulp(StrictMath.log10(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.log10(d), Math.log10(d), ulp);
    }

    public void testLOG1P() throws Exception {
        double d = rnd.nextInt() * rnd.nextDouble();
        d *= Math.signum(d);
        double ulp = Math.ulp(StrictMath.log1p(d));
        assertEquals("d=" + d + " ulp="+ulp, StrictMath.log1p(d), Math.log1p(d), ulp);

        d = rnd.nextDouble();
        //TODO current impl is less accurate for small values
        ulp = 1.e-9; //Math.ulp(StrictMath.tan(d));
        
        ulp = Math.ulp(StrictMath.log1p(d));
        assertEquals("small d=" + d + " ulp="+ulp, StrictMath.log1p(d), Math.log1p(d), ulp);
    }

    public void testATAN2() throws Exception {
        double d1 = rnd.nextInt() * rnd.nextDouble();
        double d2 = rnd.nextInt() * rnd.nextDouble();
        assertEquals("d1=" + d1 + " d2=" + d2, 
                StrictMath.atan2(d1,d2), Math.atan2(d1,d2), Math.ulp(StrictMath.atan2(d1,d2)));
        double q1 = Math.atan2(1, 1);
        double q2 = Math.atan2(1, -1);
        double q3 = Math.atan2(-1, -1);
        double q4 = Math.atan2(-1, 1);
        //System.out.println(q1+"\n"+q2+"\n"+q3+"\n"+q4+"\n");
        
        assertTrue("q1="+q1, q1 > 0 && q1 < Math.PI/2);
        assertTrue("q2="+q2, q2 > Math.PI/2 && q1 < Math.PI);
        assertTrue("q3="+q3, q3 < -Math.PI/2 && q1 > -Math.PI);
        assertTrue("q4="+q4, q4 < 0 && q1 > -Math.PI/2);
    }
}

