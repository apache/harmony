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
#include <vmi.h>
#include "hyenv.h"


JNIEXPORT jbyteArray JNICALL Java_org_apache_harmony_luni_platform_Environment_getEnvBytes

  (JNIEnv *env, jclass obj){ 
  jsize len =0;
  jbyte *buffer;
  LPTSTR lpszVars;
  LPVOID lpvEnv;
  jbyteArray byteArray;
  lpvEnv = GetEnvironmentStrings();
  if (NULL == lpvEnv){
    return NULL;
  }
  lpszVars = (LPTSTR)lpvEnv;
  buffer = lpszVars;
  while(*lpszVars){
    len += strlen(lpszVars)+1;
	lpszVars += strlen(lpszVars)+1;
  }
  byteArray = (*env)->NewByteArray(env,len);
  (*env)->SetByteArrayRegion(env,byteArray, 0, len, buffer);
  FreeEnvironmentStrings((LPTCH)lpvEnv);
  return byteArray;
}

JNIEXPORT jbyteArray JNICALL Java_org_apache_harmony_luni_platform_Environment_getEnvByName
  (JNIEnv *env, jclass obj, jbyteArray name){
  DWORD dwRet,dwErr;
  LPTSTR envvalue;
  jsize len = 0;
  const DWORD BUFSIZE = 1024;
  jbyteArray byteArray = NULL;
  char *envname;
  PORT_ACCESS_FROM_ENV(env);

  len = (*env)->GetArrayLength(env, name);
  envname = (char *)hymem_allocate_memory(len+1);
  (*env)->GetByteArrayRegion(env, name, 0, len,(jbyte *)envname);
  envname[len] = 0;

  envvalue = (LPTSTR)hymem_allocate_memory(BUFSIZE*sizeof(TCHAR));
  dwRet = GetEnvironmentVariable(envname, envvalue, BUFSIZE);
  
  if(0 == dwRet)
  {
    dwErr = GetLastError();
    if( ERROR_ENVVAR_NOT_FOUND == dwErr ){
      goto free_resource;
    }
  }
  else if(BUFSIZE < dwRet)
  {
    envvalue = (LPTSTR)hymem_reallocate_memory(envvalue, dwRet*sizeof(TCHAR));   
    if(NULL == envvalue){
      goto free_resource;
    }
    dwRet = GetEnvironmentVariable((LPCSTR)envname, envvalue, dwRet);
    if(!dwRet){
      goto free_resource;
    }
  }
  
  byteArray = (*env)->NewByteArray(env,dwRet);
  (*env)->SetByteArrayRegion(env,byteArray, 0, dwRet, (jbyte *)envvalue);
  free_resource:
  hymem_free_memory(envname);
  hymem_free_memory(envvalue);
  return byteArray;
}


