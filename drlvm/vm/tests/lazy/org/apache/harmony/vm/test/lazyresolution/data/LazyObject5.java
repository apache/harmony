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

public class LazyObject5 {


    public static LazyObject1 staticObjectField = new LazyObject1();
    public static LazyObject1 staticArrayField[]= new LazyObject1[10];
    public static LazyObject1 staticMultiArrayField[][] = new LazyObject1[10][20];

    public LazyObject1     objectField = staticObjectField;
    public LazyObject1[]   arrayField = staticArrayField;
    public LazyObject1[][] multiArrayField = staticMultiArrayField;


    public static LazyObject1 getStaticObjectField() {return staticObjectField;}
    public LazyObject1 getObjectField() {return objectField;}

    public static LazyObject1[] getStaticArrayField() {return staticArrayField;}
    public LazyObject1[] getArrayField() {return arrayField;}

    public static LazyObject1[][] getStaticMultiArrayField() {return staticMultiArrayField;}
    public LazyObject1[][] getMultiArrayField() {return multiArrayField;}

}