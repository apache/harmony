/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
package org.apache.harmony.rmi.test;

import java.io.Serializable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class MyInvocationHandler implements InvocationHandler, Serializable {

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if (method.getName().equals("hashCode")) {
            return new Integer(0);
        }

        System.out.print("MyInvocationHandler: method = " + method + ", args = ");

        if (args == null) {
            System.out.println("null");
        } else if (args.length == 0) {
            System.out.println("<>");
        } else {
            for (int i = 0; i < args.length - 1; ++i) {
                System.out.print(args[i].toString() + ", ");
            }
            System.out.println(args[args.length - 1].toString());
        }
        return null;
    }
}
