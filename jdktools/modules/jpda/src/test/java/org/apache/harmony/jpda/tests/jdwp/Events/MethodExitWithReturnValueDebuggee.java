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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.jpda.tests.jdwp.Events;

import java.io.IOException;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class MethodExitWithReturnValueDebuggee extends SyncDebuggee {
    
    public static final String BOOLEAN_TYPE = "BOOLEAN";
    
    public static final String SHORT_TYPE = "SHORT";
    
    public static final String CHAR_TYPE = "CHAR";
    
    public static final String INT_TYPE = "INT";
    
    public static final String LONG_TYPE = "LONG";
    
    public static final String DOUBLE_TYPE = "DOUBLE";
    
    public static final String EXCEPTION_TYPE = "EXCEPTION";
    
    public static final boolean EXPECTED_BOOLEAN = true;
    
    public static final short EXPECTED_SHORT = 2;
    
    public static final char EXPECTED_CHAR = 'B';
    
    public static final int EXPECTED_INT = 230;
    
    public static final long EXPECTED_LONG = 0523l;
    
    public static final double EXPECTED_DOUBLE = 5.23d;
    
    public static final Object EXPECTED_OBJECT = new MethodExitWithReturnValueDebuggee();

    public static void main(String[] args) {
        runDebuggee(MethodExitWithReturnValueDebuggee.class);
    }
    
    /*
     * tested methods with different return values 
     */
    
    public boolean booleanMethod() {
        logWriter.println("--> calling booleanMethod()");
        return EXPECTED_BOOLEAN;
    }
    
    public short shortMethod() {
        logWriter.println("--> calling shortMethod()");
        return EXPECTED_SHORT;
    }
    
    public char charMethod() {
        logWriter.println("--> calling charMethod()");
        return EXPECTED_CHAR;
    }
    
    public int intMethod() {
        logWriter.println("--> calling intMethod()");
        return EXPECTED_INT;
    }

    public long longMethod() {
        logWriter.println("--> calling longMethod()");
        return EXPECTED_LONG;
    }
    
    public double doubleMethod() {
        logWriter.println("--> calling doubleMethod()");
        return EXPECTED_DOUBLE;
    }
    
    public void run() {
        logWriter.println("--> MethodExitWithReturnValueDebuggee started");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Receive required method type
        String type = synchronizer.receiveMessage();
        logWriter.println("--> invoke method's return type is:" + type);
        
        // Invoke desired method
        if(type.equals(BOOLEAN_TYPE)){
            boolean b = booleanMethod();
            logWriter.println("--> booleanMethod() is invoked, return value:" + b);
        }else if(type.equals(SHORT_TYPE)){
            short s = shortMethod();
            logWriter.println("--> shortMethod() is invoked, return value:" + s);
        }else if(type.equals(CHAR_TYPE)){
            char c = charMethod();
            logWriter.println("--> charMethod() is invoked, return value:" + c);
        }else if(type.equals(INT_TYPE)){
            int i = intMethod();
            logWriter.println("--> intMethod() is invoked, return value:" + i);
        }else if(type.equals(LONG_TYPE)){
            long l = longMethod();
            logWriter.println("--> longMethod() is invoked, return value:" + l);
        }else if(type.equals(DOUBLE_TYPE)){
            double d = doubleMethod();
            logWriter.println("--> doubleMethod() is invoked, return value:" + d);
        }else if(type.equals(EXCEPTION_TYPE)){
            try {
                MockExceptionMethodClass.exceptionMethod();
            } catch (IOException e) {
                // expected
            }
            logWriter.println("--> exceptionMethod() is invoked.");
        }
        
        logWriter.println("--> MethodExitWithReturnValueDebuggee finished");
    }

}

class MockExceptionMethodClass{
    
    static public int exceptionMethod() throws IOException {
        throw new IOException();
    }
}
