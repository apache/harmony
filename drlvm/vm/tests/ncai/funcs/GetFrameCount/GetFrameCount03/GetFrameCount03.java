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

package ncai.funcs;

/**
 * @author Petr Ivanov
 *
 */
public class GetFrameCount03 extends Thread{
    public native void TestFunction();
    public native void TestFunction1();
    public static native boolean stopsignal();

    static boolean NoLibrary = false;
    static {
        try{
            System.loadLibrary("GetFrameCount03");
        }
        catch(Throwable e){
            NoLibrary = true;
        }
    }

    GetFrameCount03(String name)
    {
        super(name);
    }

    static public void main(String args[]) {
        if(NoLibrary) return;
        new GetFrameCount03("java_thread").start();
        special_method();
        return;
    }

    static public void special_method() {
        /*
         * Transfer control to native part.
         */
        try {
            throw new InterruptedException();
        } catch (Throwable tex) { }
        return;
    }

    public void run() {
        System.out.println("thread - java run\n");
        TestFunction();
    }
}


