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
 * 
 * This java_lang_Runtime.cpp source ("Software") is furnished under license and may
 * only be used or copied in accordance with the terms of that license.
 * 
 **/ 
// java_lang_Runtime.cpp : java.lang.Runtime class' native support.
//

#define LOG_DOMAIN "kernel.process"
#include "cxxlog.h"

#include "open/types.h"
#include "jni.h"
#include "exceptions.h"
#include "java_lang_Runtime_SubProcess.h"
#include "java_lang_Runtime_SubProcess_SubInputStream.h"
#include "java_lang_Runtime_SubProcess_SubOutputStream.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <signal.h>
#include <unistd.h>

#include <sys/stat.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <sys/poll.h>

#include <errno.h>

static void Error (const char *lpcszMess, JNIEnv *env, jlongArray la) 
{ 
   jboolean jb = true;
   jlong *lp = (jlong*)env->GetLongArrayElements(la, &jb);
   lp[0] = 0;
   env->ReleaseLongArrayElements(la, lp, 0);
   if (lpcszMess != NULL) {
        INFO(lpcszMess); 
   }
}

static void ThrowError(JNIEnv *env, const char *message = 0) {
    jclass jc = env->FindClass((const char *)"java/io/IOException");
    env->ThrowNew(jc, message ? message : strerror(errno));
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_createProcess0 (JNIEnv *env, jobject obj, jobjectArray cmdarray, jobjectArray envp, jstring dir, jlongArray la){
    jobject jo; 

     const char *strChain;
     int i;

     char *cmdDir = NULL;
     char *strCmd = NULL; 

     // Get the working directory of the subprocess:
     if ( dir != NULL ) {
         char* str = (char *)env->GetStringUTFChars(dir, 0);
         cmdDir = (char *)malloc(1+strlen(str)); // + NUL symbol
         *cmdDir = '\0';
         strcat(cmdDir, str);
         env->ReleaseStringUTFChars(dir, str);
     }  

     // Get the the command to call and its arguments (it must be non-null):
     int lenargv = 0;
     lenargv = env->GetArrayLength(cmdarray);
     char *argv[lenargv+1];
     for ( i = 0; i < lenargv; i++ ) {
         jo = env->GetObjectArrayElement((jobjectArray)((jobject)cmdarray), (jsize) i);
         strChain = env->GetStringUTFChars((jstring) jo, 0);
         strCmd = (char *)malloc(1+strlen(strChain)); // + NUL symbol
         *strCmd = '\0';
         strcat(strCmd, strChain);
         argv[i] = strCmd;
         env->ReleaseStringUTFChars((jstring) jo, strChain);
     }
     argv[lenargv] = (char *) 0; // NULL pointer

     // Get the array, each element of which has environment variable settings:
     int lenEnvp = 0;
     if (envp != NULL) {
         lenEnvp += env->GetArrayLength(envp);
     }
     char *strEnvpBeginAA[lenEnvp + 1];
     if (envp != NULL) {
         for ( i = 0; i < lenEnvp; i++ ) {
             jo = env->GetObjectArrayElement((jobjectArray)((jobject)envp), (jsize) i);
             strChain = env->GetStringUTFChars((jstring) jo, 0);
             strCmd = (char *)malloc(1+strlen(strChain)); // + NUL symbol
             *strCmd = '\0';
             strcat(strCmd, strChain);
             strEnvpBeginAA[i] = strCmd;
             env->ReleaseStringUTFChars((jstring) jo, strChain);
         }
     }

     strEnvpBeginAA[lenEnvp] = (char *) 0; // NULL pointer

     //define stdI/O/E for future process:
     int fildesO[2] = {-1,-1};
     int fildesE[2] = {-1,-1};
     int fildesI[2] = {-1,-1};
     // Controlling pipe.
     // Child process if successfully executed will close the handle (by system)
     // If execv failed the write(..) call will write 4 bytes in this stream.
     // Thus we can distinguish executed proccesses and failed ones.
     int fildesInfo[2] = {-1,-1};

     if (pipe(fildesO) == -1
             || pipe(fildesI) == -1
             || pipe(fildesE) == -1
             || pipe(fildesInfo) == -1) {
         if (fildesO[0] != -1) close(fildesO[0]);
         if (fildesO[1] != -1) close(fildesO[1]);
         if (fildesE[0] != -1) close(fildesE[0]);
         if (fildesE[1] != -1) close(fildesE[1]);
         if (fildesI[0] != -1) close(fildesI[0]);
         if (fildesI[1] != -1) close(fildesI[1]);
         if (fildesInfo[0] != -1) close(fildesInfo[0]);
         if (fildesInfo[1] != -1) close(fildesInfo[1]);
         Error("Stdin/stdout pipes creation failed:", env, la); 
         Error(strerror(errno), env, la); 
         return;
     }
     int spid = fork();
 
     if (spid == -1) {
         close(fildesI[0]);
         close(fildesI[1]);
         close(fildesO[0]);
         close(fildesO[1]);
         close(fildesE[0]);
         close(fildesE[1]);
         close(fildesInfo[0]);
         close(fildesInfo[1]);
         Error("Fork failed\n", env, la); 
     }

     if (spid==0) {
         ///// Child process code ///////////////
         dup2(fildesI[0], 0);
         dup2(fildesO[1], 1);
         dup2(fildesE[1], 2);
         close(fildesI[0]);
         close(fildesI[1]);
         close(fildesO[0]);
         close(fildesO[1]);
         close(fildesE[0]);
         close(fildesE[1]);
         close(fildesInfo[0]);
         long close_on_exec = FD_CLOEXEC; // set close on exec bit
         fcntl(fildesInfo[1], F_SETFD, close_on_exec);

         // Get the working directory of the subprocess:
         if ( cmdDir != NULL ) {
             int res = chdir(cmdDir);
             if (res == -1) {
                 write(fildesInfo[1], &errno, sizeof(int));
                 INFO("chdir failed: " << strerror(errno));
                 kill(getpid(), 9);
             }
             free(cmdDir);
         }
         
         if (lenEnvp == 0) {
             execvp(argv[0], argv);
         } else {
             execve(argv[0], argv, strEnvpBeginAA);
             if(strchr(argv[0], '/') == NULL) {
                 char* curDir = NULL;
                 char* cmdPath = NULL;
                 char* dirs = NULL;
                 if ((dirs = getenv("PATH")) != NULL) {
                     int len = 0;
                     curDir = strtok(dirs, ":");
                     while(curDir != NULL) {
                         if((len = strlen(curDir)) != 0) {
                             cmdPath = (char *)malloc(len+1+strlen(argv[0])+1);
                             *cmdPath = '\0';
                             strcat(strcat(strcat(cmdPath, curDir), "/"), argv[0]);
                             if (fopen(cmdPath, "r") != NULL) {
                                 execve(cmdPath, argv, strEnvpBeginAA);
                                 //XXX: should we inform only of a last error among all possible execve atempts?
                             }
                             free(cmdPath);
                         }
                         curDir = strtok(NULL, ":");
                     }
                 }
             }
         }
         write(fildesInfo[1], &errno, sizeof(int));
         INFO("Process initiation failed: " << strerror(errno));
         // kill self
         kill(getpid(), 9);
     }
     ///// End of child process code ////////////

     close(fildesO[1]);
     close(fildesI[0]);
     close(fildesE[1]);
     close(fildesInfo[1]);
     free(cmdDir);
     free(strCmd);

     // get execution status from child
     int errno_child;
     int res = read(fildesInfo[0], &errno_child, sizeof(int));
     if (res == 4) {
         Error("Process initiation failed", env, la);
         Error(strerror(errno_child), env, la);
         close(fildesO[0]);
         close(fildesI[1]);
         close(fildesE[0]);
         close(fildesInfo[0]);
         return;
     }

   jboolean jb = true;
   jlong *lp = (jlong*)env->GetLongArrayElements(la, &jb);
   lp[0] = (jlong) spid; // new process number 
   lp[1] = (jlong) fildesI[1];
   lp[2] = (jlong) fildesO[0];
   lp[3] = (jlong) fildesE[0];
   fcntl(fildesO[0], F_SETFL, 0); //XXX:to set !O_NONBLOCK because it is sometimes set by default and it should be investigated
   fcntl(fildesE[0], F_SETFL, 0); //XXX:to set !O_NONBLOCK because it is sometimes set by default and it should be investigated
   env->ReleaseLongArrayElements(la, lp, 0);

   close(fildesInfo[0]);
}

static jboolean waitFor(JNIEnv *env, jobject obj, jint handle) {
    if (handle == -1) return true;

    int status = 0;
    pid_t pid = (pid_t) handle;
    pid_t res = waitpid(pid, &status, WNOHANG);

    if (res != pid) {
        return false;
    }

    int exitCode = WEXITSTATUS(status);
    if (WIFSIGNALED(status)) exitCode = 129;

    // got the process event
    // store exit code
    jclass clss = env->GetObjectClass(obj);
    jfieldID exitCodeField = env->GetFieldID(clss, "processExitCode", "I");
    jfieldID handleField = env->GetFieldID(clss, "processHandle", "I");
    env->SetIntField(obj, exitCodeField, exitCode);
    env->SetIntField(obj, handleField, -1);
    return true;
}

jboolean JNICALL Java_java_lang_Runtime_00024SubProcess_getState0 (JNIEnv *env, jobject obj, jint handle) { 
    return waitFor(env, obj, handle);
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_destroy0 (JNIEnv *env, jobject obj, jint handle) {
    if (waitFor(env, obj, handle)) return;
    kill(handle, SIGKILL);
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_close0 (JNIEnv *env, jobject obj, jint handle) {
}

//###############################################################################################

jint JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubInputStream_readInputByte0 (JNIEnv *env, jobject obj, jlong inputHandle) {
    char ca[1];
    int res = read((int)inputHandle, ca, (unsigned) 1);
    if (res == 1) {
        return ((unsigned char) ca[0])&0xFF;
    }

    if (res == 0) {
        return -1;
    }
    
    jclass jc = env->FindClass((const char *)"java/io/IOException");
    env->ThrowNew(jc, strerror(errno));
    return -1;
    
}

jint JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubInputStream_available0 (JNIEnv *env, jobject obj, jlong inputHandle) {
    int res;
    if (ioctl((int) inputHandle, FIONREAD, &res) == -1) {
        if (errno == EINVAL) {
            struct pollfd r[1];

            r[0].fd = (int) inputHandle;
            r[0].events = POLLRDNORM;
            r[0].revents = 0;

            do {
                res = poll(r, 1, 0);
            } while (res == -1 && errno == EINTR);

            if (res == 1) {
                if(r[0].revents & r[0].events) {
                    return 1; // So, in that case we can define one byte is available at least
                } else if(r[0].revents & (POLLERR | POLLNVAL)) {
                    char mess[100];
                    mess[0] = '\0';
                    sprintf(mess, "%s", (r[0].revents & POLLERR ? "Some error condition has raised." : "Invalid request: handle closed."));
                    jclass jc = env->FindClass((const char *)"java/io/IOException");
                    env->ThrowNew(jc, (const char *) mess);
                }
                return 0;
            } else if (res < 0) {
                char mess[100];
                mess[0] = '\0';
                sprintf(mess, "It's impossible to identify if there are available bytes in the input stream! ERRNO=%d. %s", errno, strerror(errno));
                jclass jc = env->FindClass((const char *)"java/io/IOException");
                env->ThrowNew(jc, (const char *) mess);
            }
            return 0;
        } else {
            char mess[100];
            mess[0] = '\0';
            sprintf(mess, "%s", "Some error condition has raised.");
            jclass jc = env->FindClass((const char *)"java/io/IOException");
            env->ThrowNew(jc, (const char *) mess);
        }
    }
    return res;
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubInputStream_close0 (JNIEnv *env, jobject obj, jlong inputHandle) {
    int res = close((int) inputHandle);

    if (res == -1 && errno != EBADF) {
        ThrowError(env);
    }
}
//###############################################################################################

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_writeOutputByte0 (JNIEnv *env, jobject obj, jlong outputHandle, jint byte) {
    if (outputHandle == -1) {
        ThrowError(env, "file already closed");
        return;
    }

    char b = (char) byte;
    int res = write((int)outputHandle, &b, 1);

    if (res != 1) {
        ThrowError(env);
    }
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_writeOutputBytes0(JNIEnv *env,
        jobject obj, jlong outputHandle, jbyteArray byte, jint off, jint len) {

    if (outputHandle == -1) {
        ThrowError(env, "file already closed");
        return;
    }

    jboolean jb = true;
    char *cp = (char*)env->GetByteArrayElements((jbyteArray)byte, &jb);

    while (true) {
        int res = write((int)outputHandle, cp + off, len);

        if (res == len) break;
        if (res <= 0) {
            ThrowError(env);
            break;
        }

        len -= res;
        off += res;
    }

    env->ReleaseByteArrayElements((jbyteArray)byte, (signed char*)cp, 0);
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_flush0 (JNIEnv *env, jobject obj, jlong outputHandle) {
    return;
}

void JNICALL Java_java_lang_Runtime_00024SubProcess_00024SubOutputStream_close0 (JNIEnv *env, jobject obj, jlong outputHandle) {
    int res = close((int) outputHandle);

    if (res == -1 && errno != EBADF) {
        ThrowError(env);
    }
}
