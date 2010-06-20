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
 * @author Serguei S.Zapreyev
 */ 
// java.lang.Runtime class' native support.

#define LOG_DOMAIN "kernel.process"
#include "cxxlog.h"

#include "jni.h"
#include "exceptions.h"
#include "java_lang_Runtime_SubProcess.h"
#include "java_lang_Runtime_SubProcess_SubInputStream.h"
#include "java_lang_Runtime_SubProcess_SubOutputStream.h"
#include <wtypes.h>
#include <winbase.h>
#include <string.h>
#include <stdlib.h>

static void Error (LPTSTR lpszMess, JNIEnv *env, jlongArray la) 
{ 
   jlong *lp = (jlong*)env->GetLongArrayElements(la, 0);
   lp[0] = 0;
   env->ReleaseLongArrayElements(la, lp, 0);

   INFO(lpszMess);
} 

static void ErrorMsg() {
    int error = GetLastError();
    char *buf;

    FormatMessage(
            FORMAT_MESSAGE_ALLOCATE_BUFFER | 
            FORMAT_MESSAGE_FROM_SYSTEM,
            NULL,
            error,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            (LPTSTR) &buf,
            0,
            0);

    INFO(buf);
    LocalFree(buf);
}

static void ThrowError(JNIEnv *env, char *message = 0) {
    jclass cl = env->FindClass("java/io/IOException");

    if (message) {
        env->ThrowNew(cl, message);
        return;
    }

    int error = GetLastError();
    char *buf;

    FormatMessage(
            FORMAT_MESSAGE_ALLOCATE_BUFFER | 
            FORMAT_MESSAGE_FROM_SYSTEM,
            NULL,
            error,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            (LPTSTR) &buf,
            0,
            0);
    env->ThrowNew(cl, buf);
    LocalFree(buf);
}


void JNICALL Java_java_lang_Runtime_00024SubProcess_createProcess0 (
    JNIEnv *env, jobject obj, jobjectArray cmdarray, 
    jobjectArray envp, jstring dir, jlongArray la)
{
    TRACE("Creating child process ...");

    HANDLE hChildStdinRd, hChildStdinWr, hChildStdinWrDup, hChildStdoutRd, hChildStdoutWr, hChildStdoutRdDup, 
           hChildStderrorRd, hChildStderrorWr, hChildStderrorRdDup; 
    PROCESS_INFORMATION piProcInfo; 
    STARTUPINFO siStartInfo;    
    SECURITY_ATTRIBUTES saAttr; 

    const char *strDir = NULL;
    // Get the working directory of the subprocess:
    if ( dir != NULL ) {
        strDir = env->GetStringUTFChars(dir, 0);
    }

    // Get the the command to call and its arguments:
    int btl = 1024;
    int l = btl;
    char *strCmnd = (char*)malloc(btl);
    *strCmnd = '\0';
    jsize len = env->GetArrayLength(cmdarray);
    int cur_pos = 0;
    int i;
    for ( i = 0; i < len; i++ ) {
        jstring jo = (jstring)env->GetObjectArrayElement(cmdarray, (jsize) i);
        const char *strChain = env->GetStringUTFChars(jo, 0);
        bool need_esc = (*strChain != '\"' && strchr(strChain, ' ') != NULL);
        cur_pos += (int)strlen(strChain) + (i == 0 ? 0 : 1) + (need_esc ? 0 : 2);
        while (l <= cur_pos) {
            char *strtmp = (char*)malloc(l + btl);
            memcpy(strtmp, strCmnd, l);
            l += btl;
            free((void *)strCmnd);
            strCmnd = strtmp;
        }
        if ( i != 0 ) {
            strcat(strCmnd, " ");
        }
        if (need_esc) strcat(strCmnd, "\"");
        strcat(strCmnd, strChain);
        if (need_esc) strcat(strCmnd, "\"");

        env->ReleaseStringUTFChars(jo, strChain);
    }

    TRACE("Child process command-line : " << strCmnd);

    char *strEnvp = NULL;
    // Get the array, each element of which has environment variable settings:
    if (envp != NULL) {
        int l = btl;
        strEnvp = (char*)malloc(btl);
        *strEnvp = '\0';
        len = env->GetArrayLength(envp);
        cur_pos = 0;
        for ( i = 0; i < len; i++ ) {
            jstring jo = (jstring)env->GetObjectArrayElement(envp, (jsize) i);
            const char* strChain = env->GetStringUTFChars(jo, 0);
            int tmp = (int)strlen(strChain) + 1;
            while (l <= (cur_pos + tmp)) {
                char *strtmp = (char*)malloc(l + btl);
                memcpy(strtmp, strEnvp, l);
                l += btl;
                free(strEnvp);
                strEnvp = strtmp;
            }
            strcpy(strEnvp + cur_pos, strChain);
            cur_pos += tmp;
            env->ReleaseStringUTFChars(jo, strChain);
        }
        strEnvp[cur_pos++] = '\0';
    }

    // Set the bInheritHandle flag so pipe handles are inherited. 
    saAttr.nLength = sizeof(SECURITY_ATTRIBUTES); 
    saAttr.bInheritHandle = TRUE; 
    saAttr.lpSecurityDescriptor = NULL; 

       // Preparation of the child process's STDOUT: 

       // 1. Create a pipe for the child process's STDOUT. 
       if (! CreatePipe(&hChildStdoutRd, &hChildStdoutWr, &saAttr, 0)) {
               Error("Stdout pipe creation failed\n", env, la); 
               return; 
       }

       // 2. Create noninheritable read handle and close the inheritable read handle. 
       if( !DuplicateHandle(GetCurrentProcess(), hChildStdoutRd, GetCurrentProcess(), &hChildStdoutRdDup, 0, FALSE, DUPLICATE_SAME_ACCESS) ) {
               CloseHandle(hChildStdoutRd);
               CloseHandle(hChildStdoutWr);
               Error("DuplicateHandle failed", env, la);
               return; 
       }
       CloseHandle(hChildStdoutRd);

       // Preparation of the child process's STDERROR: 

       // 1. Create anonymous pipe to be STDERROR for child process. 
       if (! CreatePipe(&hChildStderrorRd, &hChildStderrorWr, &saAttr, 0)) {
               CloseHandle(hChildStdoutRdDup);
               CloseHandle(hChildStdoutWr);
               Error("Stderror pipe creation failed\n", env, la); 
               return; 
       }

       // 2. Create a noninheritable duplicate of the read handle, and close the inheritable read handle. 
       if( !DuplicateHandle(GetCurrentProcess(), hChildStderrorRd, GetCurrentProcess(), &hChildStderrorRdDup, 0, FALSE, DUPLICATE_SAME_ACCESS) ) {
               CloseHandle(hChildStdoutRdDup);
               CloseHandle(hChildStdoutWr);
               CloseHandle(hChildStderrorRd);
               CloseHandle(hChildStderrorWr);
               Error("DuplicateHandle failed", env, la);
               return; 
       }
       CloseHandle(hChildStderrorRd);

       // Preparation of the child process's STDIN: 

       // 1. Create anonymous pipe to be STDIN for child process. 
       if (! CreatePipe(&hChildStdinRd, &hChildStdinWr, &saAttr, 0)) {
               CloseHandle(hChildStdoutRdDup);
               CloseHandle(hChildStdoutWr);
               CloseHandle(hChildStderrorRdDup);
               CloseHandle(hChildStderrorWr);
               Error("Stdin pipe creation failed\n", env, la); 
               return; 
       }

       // 2. Create a noninheritable duplicate of the write handle, and close the inheritable write handle. 
       if ( !DuplicateHandle(GetCurrentProcess(), hChildStdinWr, GetCurrentProcess(), &hChildStdinWrDup, 0, FALSE, DUPLICATE_SAME_ACCESS)) {
               CloseHandle(hChildStdoutRdDup);
               CloseHandle(hChildStdoutWr);
               CloseHandle(hChildStderrorRdDup);
               CloseHandle(hChildStderrorWr);
               CloseHandle(hChildStdinRd);
               CloseHandle(hChildStdinWr);
               Error("DuplicateHandle failed", env, la); 
               return; 
       }
       CloseHandle(hChildStdinWr); 

       // Now create the child process:    
    
    ZeroMemory(&siStartInfo, sizeof(STARTUPINFO));
    siStartInfo.cb = sizeof(STARTUPINFO); 
    siStartInfo.hStdError = hChildStderrorWr;
    siStartInfo.hStdOutput = hChildStdoutWr;
    siStartInfo.hStdInput = hChildStdinRd;
    siStartInfo.dwFlags |= STARTF_USESTDHANDLES;
   
    BOOL created = CreateProcess(NULL /* name of executable module */, 
        strCmnd /* command line */, 
        NULL /* process security attributes */, 
        NULL /* primary thread security attributes */, 
        TRUE /* handles are inherited */, 
        0 /* creation flags */, 
        strEnvp /* either use parent's environment or use the own one */, 
        strDir /* either use parent's current directory or use new one */, 
        &siStartInfo /* STARTUPINFO pointer */, 
        &piProcInfo /* receives PROCESS_INFORMATION */ );

    if (!created) {
        ErrorMsg();
        Error("Process creation failed\n", env, la);
    }
 
    // Memory deallocation.
    free(strCmnd);
    if (strEnvp) {
        free(strEnvp);
    }
    if (dir) {
        env->ReleaseStringUTFChars(dir, strDir);
    }

    CloseHandle(hChildStdoutWr);
    CloseHandle(hChildStderrorWr);
    CloseHandle(hChildStdinRd);
    
    if (created) {
        CloseHandle(piProcInfo.hThread); 
        jlong *lp = (jlong*)env->GetLongArrayElements(la, 0);
        lp[0] = (jlong) piProcInfo.hProcess;
        lp[1] = (jlong) hChildStdinWrDup; 
        lp[2] = (jlong) hChildStdoutRdDup; 
        lp[3] = (jlong) hChildStderrorRdDup;
        env->ReleaseLongArrayElements(la, lp, 0);
    } else {
        CloseHandle(hChildStdoutRdDup);
        CloseHandle(hChildStderrorRdDup);
        CloseHandle(hChildStdinWrDup);
    }
}

static jboolean waitFor(JNIEnv *env, jobject obj, jint handle) {
    if (handle == -1) return true;

    DWORD wait = WaitForSingleObject((HANDLE) handle, 0);

    if (wait != WAIT_OBJECT_0) {
        return false;
    }

    DWORD exitCode;
    BOOL res = GetExitCodeProcess(
            (HANDLE) handle /* handle to the process */,
            &exitCode /* address to receive termination status */ );

    if (!res) {
        return false;
    }

    // store exit code
    jclass clss = env->GetObjectClass(obj);
    jfieldID exitCodeField = env->GetFieldID(clss, "processExitCode", "I");
    jfieldID handleField = env->GetFieldID(clss, "processHandle", "I");
    env->SetIntField(obj, exitCodeField, exitCode);
    env->SetIntField(obj, handleField, -1);
    CloseHandle((HANDLE) handle);
    return true;
}

jboolean JNICALL Java_java_lang_Runtime_00024SubProcess_getState0 (JNIEnv *env, jobject obj, jint handle) { 
    return waitFor(env, obj, handle);
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_destroy0 (JNIEnv *env, jobject obj, jint handle) {
    if (waitFor(env, obj, handle)) return;

    TerminateProcess(
            (HANDLE) handle /* handle to the process */,
            ERROR_PROCESS_ABORTED /* exit code for the process */ );
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_close0 (JNIEnv *env, jobject obj, jint handle) {
    CloseHandle((HANDLE)handle /* handle to process */ );
}

//###############################################################################################

jint JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubInputStream_readInputByte0(
    JNIEnv *env, jobject obj, jlong inputHandle) 
{
    int ia[128];
    LPVOID lpBuffer = (void*)ia;
    DWORD numberOfBytesRead;
    DWORD numberOfBytesLeftThisMessage;
    
    ia[0] = 0x0;
    numberOfBytesRead = 0;
    numberOfBytesLeftThisMessage = 0;

    if (ReadFile((HANDLE)(POINTER_SIZE_INT)inputHandle /* handle of file to read */, 
                lpBuffer /* pointer to buffer that receives data */, 
                1 /* number of bytes to read */, 
                &numberOfBytesRead /* pointer to number of bytes read */, 
                NULL /* pointer to structure for data */ )) 
    {
        TRACE("Subprocess input stream has read " << numberOfBytesRead << " bytes"); 
        if (numberOfBytesRead == 0) {
            return -1;
        }   
        return ia[0]&0xFF; 
    }

    DWORD error = GetLastError();
    INFO("Subprocess input stream read access failed; error code = " << error); 
    switch (error) {
            case ERROR_HANDLE_EOF:  //Reached the end of the file.
            case ERROR_NO_DATA:     //The pipe is being closed.
            case ERROR_BROKEN_PIPE: //The pipe has been ended.
                return -1;
    }

    ThrowError(env);
    return 0;
}

jint JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubInputStream_available0 (
    JNIEnv *env, jobject obj, jlong inputHandle) 
{
    DWORD totalBytesAvail;
    
    if (!PeekNamedPipe((HANDLE)(POINTER_SIZE_INT)inputHandle /* handle to pipe to copy from */, 
        NULL /* pointer to data buffer */, 
        NULL /* size, in bytes, of data buffer */, 
        NULL /* pointer to number of bytes read */, 
        &totalBytesAvail /* pointer to total number of bytes available */, 
        NULL /* pointer to unread bytes in this message */ ))
    {
        DWORD error = GetLastError();
        INFO("Subprocess input stream access failed; error code = " << error); 
        switch (error) {
            case ERROR_HANDLE_EOF:  //Reached the end of the file.
            case ERROR_NO_DATA:     //The pipe is being closed.
            case ERROR_BROKEN_PIPE: //The pipe has been ended.
                break;
            default:
                exn_raise_by_name("java/io/IOException");
        }
        
        return 0;
    }

    TRACE("Subprocess input stream " << inputHandle << " has " 
        << totalBytesAvail << " bytes available");

    return totalBytesAvail;
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubInputStream_close0 (
    JNIEnv *env, jobject obj, jlong inputHandle) 
{
    DWORD res = CloseHandle((HANDLE)(POINTER_SIZE_INT)inputHandle);
    if (res == 0 && GetLastError() != ERROR_INVALID_HANDLE) {
        ThrowError(env);
    }
}

//###############################################################################################

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_writeOutputByte0 (JNIEnv *env, jobject obj, jlong outputHandle, jint byte) {   
    DWORD dwWritten; 
    CHAR chBuf[1]; 
    
    if (outputHandle == -1) {
        ThrowError(env, "file already closed");
        return;
    }
    
    chBuf[0] = byte & 0xFF;
    
    if (WriteFile((HANDLE)(POINTER_SIZE_INT)outputHandle /* handle to file to write to */,
                  chBuf /* pointer to data to write to file */, 1 /* number of bytes to write */,
                  &dwWritten /* pointer to number of bytes written */,
                  NULL /* pointer to structure for overlapped I/O */) == 0) {
        ThrowError(env);
    }
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_writeOutputBytes0 (JNIEnv *env, jobject obj,
                                                                                            jlong outputHandle,
                                                                                            jbyteArray byte,
                                                                                            jint off,
                                                                                            jint len) {
    if (outputHandle == -1) {
        ThrowError(env, "file already closed");
        return;
    }

    DWORD dwWritten; 
    jboolean jb;
    char *ip = (char*) env->GetByteArrayElements((jbyteArray)byte, &jb);

    DWORD res = WriteFile(
                (HANDLE)(POINTER_SIZE_INT)outputHandle /* handle to file to write to */,
                ip + off /* pointer to data to write to file */,
                len /* number of bytes to write */,
                &dwWritten /* pointer to number of bytes written */,
                NULL /* pointer to structure for overlapped I/O */);

    env->ReleaseByteArrayElements((jbyteArray)byte, (signed char *)ip, 0);
    if (res == 0) ThrowError(env);
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_flush0 (JNIEnv *env, jobject obj, jlong outputHandle) {
    DWORD res = FlushFileBuffers((HANDLE)(POINTER_SIZE_INT)outputHandle /* open handle to file whose buffers are to be flushed */);
    if (res == 0) ThrowError(env);
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_close0 (JNIEnv *env, jobject obj, jlong outputHandle) {   
    DWORD res = CloseHandle((HANDLE)(POINTER_SIZE_INT)outputHandle /* handle to pipe */ );
    if (res == 0 && GetLastError() != ERROR_INVALID_HANDLE) ThrowError(env);
}
