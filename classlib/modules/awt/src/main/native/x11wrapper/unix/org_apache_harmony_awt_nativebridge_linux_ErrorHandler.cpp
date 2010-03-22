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
 * @author Rustem Rafikov
 */

#include "org_apache_harmony_awt_nativebridge_linux_ErrorHandler.h"
#include <string.h>

static jboolean isError = JNI_FALSE;
static XErrorEvent info;

int errorHandler(Display* d, XErrorEvent* env){
    isError = JNI_TRUE;
    memcpy(&info, env, sizeof(XErrorEvent));
    return 1;
}


JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_nativebridge_linux_ErrorHandler_isError (JNIEnv * env, jclass self) 
{
    return isError;    
}

JNIEXPORT jstring JNICALL Java_org_apache_harmony_awt_nativebridge_linux_ErrorHandler_getInfo (JNIEnv * env, jclass self) 
{
    char buffer[1024];
    char message[200];
    char request[200];
    char request_def[200];
    char code[30];
    if (isError) {
        isError = JNI_FALSE;
        XGetErrorText(info.display, info.error_code, message, 200);

        sprintf(code, "%d", info.request_code);
        sprintf(request_def, "Major opcode: %d", info.request_code);
        XGetErrorDatabaseText(info.display, "XRequest", code, request_def, request, 200);

        sprintf(buffer, "X Server Error: %s, %s, Minor opcode: %d", message, request, info.minor_code);
        return env->NewStringUTF((const char *) buffer);
    }
    return NULL;
}
