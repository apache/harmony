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
 * @author Pavel Pervov
 */  
package classloader;
/**
 * Test on class loader. It should throw ExceptionInInitializerError
 * and propagate it up the class loading chaing. Instead of it
 * on interpreter it throws NoClassDefFoundError.
 */
public class ExceptionInInitializerTest {
    public static void main(String[] args) {
        try {
            new Test3();
        } catch (Throwable e) {
            System.out.println("caught " + e);
            if (e.getClass() == ExceptionInInitializerError.class)
                System.out.println("PASS");
            else {
                System.out.println("FAILED");
                System.out.println("Wrong exception class");
                e.printStackTrace();
            }
        }
    }
}

class Test1 {
    static String z = null;
    static char c;

    static {
        c = z.charAt(0);
    }
}

class Test2 extends Test1 {
    public Test2() {
        super();
    }
}

class Test3 {
    static Test2 t = new Test2();

    public Test3() {
    }
}

