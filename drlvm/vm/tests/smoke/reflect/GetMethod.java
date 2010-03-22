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
 * @author Alexei Fedotov, Pavel Afremov
 */  
package reflect;

import java.lang.reflect.Method;

/**
 * This fails on the external java 1.4.2.
 * 
 * @keyword 
 */
public class GetMethod {
    public Object f(String a) {
        return null;
    }

    public static void main(String[] s) {
        Class cl = GetMethod.class;
        Class args[] = new Class[2];
        args[0] = String.class;
        args[1] = null;

        Method m;
        try {
            m = cl.getMethod("f", args);
        } catch (NoSuchMethodException e) {
            System.out.println("Passed");
            return;
        }

        System.out.println("method = " + m);
    }
}
