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
 
 #include <windows.h>
 #include "exceptions.h"
 #include "consoleimpl.h"
 
 HANDLE hStdIn;
 HANDLE hStdOut;
 DWORD saved_ConsoleMode;
 
 
 
/*
 * Whether the standard input is attached to a console.
 */
 
JNIEXPORT jboolean JNICALL Java_java_io_Console_hasStdInImpl  
    (JNIEnv *env, jclass thiz){       
    hStdIn = GetStdHandle(STD_INPUT_HANDLE);	 
    if(INVALID_HANDLE_VALUE == hStdIn || GetConsoleMode(hStdIn, &saved_ConsoleMode) == 0){
      return 0;
    }
    return 1;    
}    

/*
 * Whether the standard output is attached to a console.
 */
JNIEXPORT jboolean JNICALL Java_java_io_Console_hasStdOutImpl(JNIEnv *env, jclass thiz){
    hStdOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if(INVALID_HANDLE_VALUE == hStdOut || GetConsoleMode(hStdOut, &saved_ConsoleMode) == 0){
      return 0;
    }
    return 1;
} 
    
/*
 * Sets the standard input echoing off.
 */
  JNIEXPORT void JNICALL Java_java_io_Console_setEchoOffImpl(JNIEnv *env, jclass thiz){
    //save the original console mode.
    DWORD new_ConsoleMode;
    if(GetConsoleMode(hStdIn, &saved_ConsoleMode)==0){
       throwJavaIoIOException(env, "fails to get console mode when echo off.");
    }    
    
    //set echo option off.
    new_ConsoleMode = saved_ConsoleMode & ~ENABLE_ECHO_INPUT;
    
    //set the echo-offed console mode.
    if(SetConsoleMode(hStdIn, new_ConsoleMode)==0)
    {
	    throwJavaIoIOException(env, "fails to set console mode when echo off.");
    }
  }  
    
/*
 * Sets the standard output echoing on.
 */
  JNIEXPORT void JNICALL Java_java_io_Console_setEchoOnImpl(JNIEnv *env, jclass thiz){
    
    //restore the saved console mode.
    if(SetConsoleMode(hStdIn, saved_ConsoleMode)==0){
       throwJavaIoIOException(env, "fails to set console mode when echo on.");
    }
  }   
    