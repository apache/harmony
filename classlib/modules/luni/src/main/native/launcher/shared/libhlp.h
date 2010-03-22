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

#if !defined(LIBHLP_H)
#define LIBHLP_H
#if defined(__cplusplus)
extern "C"
{
#endif
#include "hycomp.h"
#include "hyport.h"
#include "jni.h"

  typedef struct HyStringBuffer
  {
    UDATA remaining;
    U_8 data[4];
  } HyStringBuffer;
  struct haCmdlineOptions
  {
    int argc;
    char **argv;
    char **envp;
    HyPortLibrary *portLibrary;
  };
  struct HyPortLibrary;
  extern HY_CFUNC HyStringBuffer *strBufferCat
    PROTOTYPE ((struct HyPortLibrary * portLibrary, HyStringBuffer * buffer,
                const char *string));
  extern HY_CFUNC char *strBufferData PROTOTYPE ((HyStringBuffer * buffer));
  struct HyPortLibrary;
  extern HY_CFUNC HyStringBuffer *strBufferPrepend
    PROTOTYPE ((struct HyPortLibrary * portLibrary, HyStringBuffer * buffer,
                char *string));
  struct HyPortLibrary;
  extern HY_CFUNC HyStringBuffer *strBufferEnsure
    PROTOTYPE ((struct HyPortLibrary * portLibrary, HyStringBuffer * buffer,
                UDATA len));
  extern HY_CFUNC void dumpVersionInfo PROTOTYPE ((HyPortLibrary * portLib));
  extern HY_CFUNC int main_runJavaMain
    PROTOTYPE ((JNIEnv * env, char *mainClassName, int nameIsUTF,
                int java_argc, char **java_argv,
                HyPortLibrary * hyportLibrary));
  extern HY_CFUNC IDATA main_initializeJavaLibraryPath
    PROTOTYPE ((HyPortLibrary * portLib,
                HyStringBuffer ** finalJavaLibraryPath, char *argv0));
  extern HY_CFUNC I_32 main_initializeClassPath
    PROTOTYPE ((HyPortLibrary * portLib, HyStringBuffer ** classPath));
  extern HY_CFUNC IDATA main_initializeJavaHome
    PROTOTYPE ((HyPortLibrary * portLib, HyStringBuffer ** finalJavaHome,
                int argc, char **argv));
#if defined(__cplusplus)
}
#endif
#endif                          /* LIBHLP_H */
