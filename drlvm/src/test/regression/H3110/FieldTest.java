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

package org.apache.harmony.drlvm.tests.regression.h3110;

import java.lang.reflect.*;
import junit.framework.TestCase;

public class FieldTest extends TestCase {

    public float amount = .555f;

    public void test_getFloat() throws Exception {
            Field floatField = FieldTest.class.getField("amount");
            FieldTest ft = new FieldTest();
        
            float fv = floatField.getFloat(ft);
            assertEquals(amount, fv);
    }

    public void test_get() throws Exception {
            Field floatField = FieldTest.class.getField("amount");
            FieldTest ft = new FieldTest();
        
            Float fo = (Float) floatField.get(ft);
            assertEquals(amount, fo.floatValue());
    }
}