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

package org.apache.harmony.vm.test.lazyresolution.data;

import java.util.*;

public class LazyObject1 {

    public static int intStaticField=10; 

    public static int intStaticField2=11; 


    public int intField=20; 

    public int intField2=21; 

    public static int getIntStaticField() {return intStaticField;}
    public static int getIntStaticField2() {return intStaticField2;}

    public int getIntField() {return intField;}
    public int getIntField2() {return intField2;}

    public LazyObject1(){}
    public LazyObject1(int v){intField=v;}
    public LazyObject1(int v1, int v2){intField=v1; intField2=v2;}

    public void virtualCall() {
        intField++;
    }


    public static Map staticMapField = new HashMap();
    
    public Map mapField = null;

    static {
        staticMapField.put("a", "b");
    }


}