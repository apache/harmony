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

#if !defined(_JVMRAS_H_)
#define _JVMRAS_H_

/*
 * ======================================================================
 * Allow for inclusion in C++
 * ======================================================================
 */
#if defined(__cplusplus)
extern "C"
{
#endif
#include "jni.h"
#include "stdarg.h"
/*
 * ======================================================================
 * Forward declarations
 * ======================================================================
 */
  typedef void (JNICALL * TraceListener) (JNIEnv * env, void **thrLocal,
                                          int traceId, const char *format,
                                          va_list varargs);
  typedef void (*DgRasOutOfMemoryHook) (void);
/*
 * ======================================================================
 * RasInfo structures
 * ======================================================================
 */
  typedef struct RasInfo
  {
    int type;
    union
    {
      struct
      {
        int number;
        char **names;
      } query;
      struct
      {
        int number;
        char **names;
      } trace_components;
      struct
      {
        char *name;
        int first;
        int last;
        unsigned char *bitMap;
      } trace_component;
    } info;
  } RasInfo;

#define RASINFO_TYPES                 0
#define RASINFO_TRACE_COMPONENTS      1
#define RASINFO_TRACE_COMPONENT       2
#define RASINFO_MAX_TYPES             2

/*
 * ======================================================================
 * External access facade
 * ======================================================================
 */
#define JVMRAS_VERSION_1_1      0x7F000001
#define JVMRAS_VERSION_1_3      0x7F000003

  typedef struct DgRasInterface
  {
    char eyecatcher[4];
    int length;
    int version;
    int modification;
    /* Interface level 1_1 */
    int (JNICALL * TraceRegister) (JNIEnv * env, TraceListener func);
    int (JNICALL * TraceDeregister) (JNIEnv * env, TraceListener func);
    int (JNICALL * TraceSet) (JNIEnv * env, const char *);
    void (JNICALL * TraceSnap) (JNIEnv * env, char *);
    void (JNICALL * TraceSuspend) (JNIEnv * env);
    void (JNICALL * TraceResume) (JNIEnv * env);
    int (JNICALL * GetRasInfo) (JNIEnv * env, RasInfo * info_ptr);
    int (JNICALL * ReleaseRasInfo) (JNIEnv * env, RasInfo * info_ptr);
    int (JNICALL * DumpRegister) (JNIEnv * env,
                                  int (JNICALL * func) (JNIEnv * env2,
                                                        void **threadLocal,
                                                        int reason));
    int (JNICALL * DumpDeregister) (JNIEnv * env,
                                    int (JNICALL * func) (JNIEnv * env2,
                                                          void **threadLocal,
                                                          int reason));
    void (JNICALL * NotifySignal) (JNIEnv * env, int signal);
    int (JNICALL * CreateThread) (JNIEnv * env,
                                  void (JNICALL * startFunc) (void *),
                                  void *args, int GCSuspend);
    int (JNICALL * GenerateJavacore) (JNIEnv * env);
    int (JNICALL * RunDumpRoutine) (JNIEnv * env, int componentID, int level,
                                    void (*printrtn) (void *env,
                                                      const char *tagName,
                                                      const char *fmt, ...));
    int (JNICALL * InjectSigsegv) (JNIEnv * env);
    int (JNICALL * InjectOutOfMemory) (JNIEnv * env);
    int (JNICALL * SetOutOfMemoryHook) (JNIEnv * env,
                                        void (*OutOfMemoryFunc) (void));
    int (JNICALL * GetComponentDataArea) (JNIEnv * env, char *componentName,
                                          void **dataArea, int *dataSize);
    int (JNICALL * InitiateSystemDump) (JNIEnv * env);
    /* Interface level 1_3 follows */
    void (JNICALL * DynamicVerbosegc) (JNIEnv * env, int vgc_switch,
                                       int vgccon, char *file_path,
                                       int number_of_files,
                                       int number_of_cycles);
    void (JNICALL * TraceSuspendThis) (JNIEnv * env);
    void (JNICALL * TraceResumeThis) (JNIEnv * env);
    int (JNICALL * GenerateHeapdump) (JNIEnv * env);
  } DgRasInterface;

/*
 * ======================================================================
 *    Dump exit return codes
 * ======================================================================
 */
#define RAS_DUMP_CONTINUE       0       /* Continue with diagnostic collection */
#define RAS_DUMP_ABORT          1       /* No more diagnostics should be taken */
/*
 * ======================================================================
 *    Thread Creation types
 * ======================================================================
 */
#define NO_GC_THREAD_SUSPEND    0       /* Do not suspend thread during CG. */
#define GC_THREAD_SUSPEND       1       /* Suspend thread during CG. */
#define RAS_THREAD_NAME_SIZE    50      /* Size of Ras Thread Name. */
/*
 * ======================================================================
 *    Dump Handler types
 * ======================================================================
 */
  enum dumpType
  {
    NODUMPS = 0,
    JAVADUMP = 0x01,
    SYSDUMP = 0x02,
    CEEDUMP = 0x04,
    HEAPDUMP = 0x08,
    MAXDUMPTYPES = 6,
    /* ensure 4-byte enum */
    dumpTypeEnsureWideEnum = 0x1000000
  };
#define ALLDUMPS (JAVADUMP | SYSDUMP | CEEDUMP | HEAPDUMP)
#define OSDUMP     (ALLDUMPS + 1)
#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /* !_JVMRAS_H_ */
