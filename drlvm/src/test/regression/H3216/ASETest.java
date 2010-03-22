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

package org.apache.harmony.drlvm.tests.regression.h3216;

import junit.framework.TestCase;

public class ASETest extends TestCase { 
    Object[] oo1 = new String[3];

    public void testASE() { 
        
        Integer[] oo2 = new Integer[oo1.length]; 
        for (int i=0; i<oo2.length; i++) { 
            oo2[i] = new Integer(i); 
        } 
        try { 
            System.arraycopy(oo2, 0, oo1, 0, oo1.length); 
            fail("ArrayStoreException should be thrown");
        } catch (ArrayStoreException ok) {} 
    } 
} 
