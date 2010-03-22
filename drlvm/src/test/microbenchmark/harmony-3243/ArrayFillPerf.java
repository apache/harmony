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

import java.util.*;
import java.io.*;

class ArrayFillPerf
{

    int     rows;
    int     cols;
    char    screenBuf[][];
    
    public tp() {
        int x, y;
        rows = 24;
        cols = 80;
        screenBuf = new char[rows][];
        for (y = 0; y < rows; y++) {
            screenBuf[y] = new char[cols];
            for (x = 0; x < cols; x++) {
                screenBuf[y][x] = 'r';
            }
            ;
        }
        ;
    }
    
    public void foo() {
        int x, y;
        for (y = 0; y < rows; y++) {
            for (x = 0; x < cols; x++) {
                screenBuf[y][x] = ' ';
            }
            ;
        }
        ;
    }



	public static void main(String args[])
	{
        tp t = new tp();
        for (int i = 0; i < 500000; i++) {
    	    t.foo();
        }
        
        try {
            Thread.sleep(1000);
        } catch (Exception e) {};
        
        long t0 = System.currentTimeMillis();
        
        t = new tp();
        for (int i = 0; i < 900000; i++) {
    	    t.foo();
        }
        
        System.out.println("time:"+(System.currentTimeMillis() - t0));

        System.out.println("PASSED!");
    }
}

