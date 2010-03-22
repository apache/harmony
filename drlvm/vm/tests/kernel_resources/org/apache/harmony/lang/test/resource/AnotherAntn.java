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
package org.apache.harmony.lang.test.resource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The annotation containing all possible types of elements with default values.
 * 
 * @author Alexey V. Varlamov
 */
@Retention(RetentionPolicy.RUNTIME)
@AnotherAntn
public @interface AnotherAntn {
    public enum TheEnum {A, B, C, }
    public @interface TheAntn {}
    
    int intValue() default 345;
    byte byteValue() default (byte)28;
    short shortValue() default (short)3247;
    long longValue() default 234956955L;
    char charValue() default 'q';
    float floatValue() default 213235.34546546f;
    double doubleValue() default 7E-34;
    boolean booleanValue() default true;
    Class classValue() default AnotherAntn.class;
    TheEnum enumValue() default TheEnum.A;
    TheAntn antnValue() default @TheAntn;
    
    int[] intArrayValue() default 345;
    byte[] byteArrayValue() default (byte)28;
    short[] shortArrayValue() default (short)3247;
    long[] longArrayValue() default 234956955L;
    char[] charArrayValue() default 'q';
    float[] floatArrayValue() default 213235.34546546f;
    double[] doubleArrayValue() default 7E-34;
    boolean[] booleanArrayValue() default true;
    Class[] classArrayValue() default AnotherAntn.class;
    TheEnum[] enumArrayValue() default TheEnum.A;
    TheAntn[] antnArrayValue() default @TheAntn;
}
