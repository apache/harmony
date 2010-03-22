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

public class Test {
    public static void main(String [] argv) {
        // check multianewarray
        try { 
            Class cl = Class.forName("TestArray");
            cl.newInstance();
            System.out.println("TestArray:     Fails");
        } catch (LinkageError e) {
            System.out.println("TestArray:     Passes: " + e);
        } catch (Throwable e) {
            System.out.println("Test Failed, caught unexpected exception");
            e.printStackTrace(System.out);
        }

        // check invokespecial
        try { 
            Class cl = Class.forName("TestSpecial");
            cl.newInstance();
            System.out.println("TestSpecial:   Fails");
        } catch (LinkageError e) {
            System.out.println("TestSpecial:   Passes: " + e);
        } catch (Throwable e) {
            System.out.println("Test Failed, caught unexpected exception");
            e.printStackTrace(System.out);
        }

        // check invokevirtual
        try { 
            Class cl = Class.forName("TestVirtual");
            cl.newInstance();
            System.out.println("TestVirtual:   Fails");
        } catch (LinkageError e) {
            System.out.println("TestVirtual:   Passes: " + e);
        } catch (Throwable e) {
            System.out.println("Test Failed, caught unexpected exception");
            e.printStackTrace(System.out);
        }
        
        // check invokeinterface
        try { 
            Class cl = Class.forName("TestInterface");
            cl.newInstance();
            System.out.println("TestInterface: Fails");
        } catch (LinkageError e) {
            System.out.println("TestInterface: Passes: " + e);
        } catch (Throwable e) {
            System.out.println("Test Failed, caught unexpected exception");
            e.printStackTrace(System.out);
        }

        // check invokestatic
        try { 
            Class cl = Class.forName("TestStatic");
            cl.newInstance();
            System.out.println("TestStatic:    Fails");
        } catch (LinkageError e) {
            System.out.println("TestStatic:    Passes: " + e);
        } catch (Throwable e) {
            System.out.println("Test Failed, caught unexpected exception");
            e.printStackTrace(System.out);
        }
    }
}
