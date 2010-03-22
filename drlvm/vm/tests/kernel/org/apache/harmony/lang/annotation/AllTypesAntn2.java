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
package org.apache.harmony.lang.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The annotation containing all possible types of elements.
 * 
 * @author Alexey V. Varlamov
 */
@Retention(RetentionPolicy.RUNTIME)
@AllTypesAntn2(intValue=1,intArrayValue=2, 
        byteValue=3, byteArrayValue=4,
        shortValue=5, shortArrayValue=6,
        longValue=7, longArrayValue=8,
        charArrayValue='s', charValue='e',
        floatArrayValue=234, floatValue=456.456745f,
        doubleArrayValue=34, doubleValue=345,
        booleanArrayValue=true, booleanValue=false,
        classArrayValue=AllTypesAntn2.class, classValue=AllTypesAntn2.class,
        enumArrayValue=AllTypesAntn2.TheEnum.B, enumValue=AllTypesAntn2.TheEnum.C,
        antnArrayValue=@AllTypesAntn2.TheAntn, antnValue=@AllTypesAntn2.TheAntn)
public @interface AllTypesAntn2 {
    public enum TheEnum {A, B, C, }
    public @interface TheAntn {}
    
    int intValue();
    byte byteValue();
    short shortValue();
    long longValue() ;
    char charValue() ;
    float floatValue();
    double doubleValue() ;
    boolean booleanValue();
    Class classValue() ;
    TheEnum enumValue();
    TheAntn antnValue();
    
    int[] intArrayValue() ;
    byte[] byteArrayValue();
    short[] shortArrayValue() ;
    long[] longArrayValue() ;
    char[] charArrayValue() ;
    float[] floatArrayValue() ;
    double[] doubleArrayValue();
    boolean[] booleanArrayValue() ;
    Class[] classArrayValue() ;
    TheEnum[] enumArrayValue();
    TheAntn[] antnArrayValue();
}
