/*!
 * @file sampleJNImain.c
 *
 * @brief Sample program to call JNI functions from a shared object.
 *
 * This code will generate external references to JNI 'C' function
 * entry point versions of Java JNI native methods.  These, in turn,
 * are designed to be stored into a shared object.  They reference
 * JNI 'C' function entry point versions of Java JNI @e local native
 * methods (because of the fact that they are @c @b java.lang.*
 * classes).  For anything besides @c @b java.lang.*, these
 * references would be to regular JNI 'C' or 'C++' or Fortran or
 * assembly or COBOL (gulp) or whatever kind of library code is
 * appropriate.
 *
 * The point with this sample @c @b main() program is to
 * create a sample program fragment that shows where the JNI
 * shared object fits into the overall scheme of things.  The
 * functions they reference demonstrate how to reference JNI
 * @e local native methods in the core JVM code.
 *
 *
 * @section Control
 *
 * \$URL$
 *
 * \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(sampleJNImain, c,
"$URL$",
"$Id$");

#include <stdlib.h>
#include <jni.h>

#include "java_lang_Object.h"
#include "java_lang_Class.h"
#include "java_lang_String.h"
#include "java_lang_Thread.h"

JNIEnv env;

jclass  clsidx_null    = 0;
jobject objhash_null   = 0;
jlong   long_int       = 0;
jint    regular_int    = 0;
jboolean boolean_value = JNI_FALSE;

int main(int argc, char **argv, char **envp)
{
    ARCH_FUNCTION_NAME(sampleJNImain /* or simply: main */ );

    JNIEnv *penv = &env;

    /* Register natives for all appropriate classes */
    Java_java_lang_Object_registerNatives(penv, clsidx_null);
    Java_java_lang_Class_registerNatives(penv, clsidx_null);
    Java_java_lang_String_registerNatives(penv, clsidx_null);
    Java_java_lang_Thread_registerNatives(penv, clsidx_null);

    /* Access the various methods */
    Java_java_lang_Object_hashCode(penv, objhash_null);
    Java_java_lang_Object_wait(penv, objhash_null);
    Java_java_lang_Object_wait__J(penv, objhash_null, long_int);

    Java_java_lang_Class_isArray(penv, objhash_null);
    Java_java_lang_Class_isPrimitive(penv, objhash_null);

    Java_java_lang_String_intern(penv, objhash_null);

    Java_java_lang_Thread_yield(penv, clsidx_null);
    Java_java_lang_Thread_interrupt(penv, objhash_null);
    Java_java_lang_Thread_interrupted(penv, clsidx_null);
    Java_java_lang_Thread_isInterrupted(penv, objhash_null);
    Java_java_lang_Thread_sleep__J(penv, clsidx_null, long_int);
    Java_java_lang_Thread_sleep__JI(penv,
                                    clsidx_null,
                                    long_int,
                                    regular_int);
    Java_java_lang_Thread_join(penv, objhash_null);
    Java_java_lang_Thread_join__J(penv, objhash_null, long_int);
    Java_java_lang_Thread_join__JI(penv,
                                   objhash_null,
                                   long_int,
                                   regular_int);
    Java_java_lang_Thread_isAlive(penv, objhash_null);
    Java_java_lang_Thread_start(penv, objhash_null);
    Java_java_lang_Thread_countStackFrames(penv, objhash_null);
    Java_java_lang_Thread_holdsLock(penv, clsidx_null, objhash_null);
    Java_java_lang_Thread_setPriority(penv, objhash_null, regular_int);
    Java_java_lang_Thread_getPriority(penv, objhash_null);
    Java_java_lang_Thread_destroy(penv, objhash_null);
    Java_java_lang_Thread_checkAccess(penv, objhash_null);
    Java_java_lang_Thread_setDaemon(penv, objhash_null, boolean_value);
    Java_java_lang_Thread_isDaemon(penv, objhash_null);
    Java_java_lang_Thread_stop(penv, objhash_null);
    Java_java_lang_Thread_resume(penv, objhash_null);
    Java_java_lang_Thread_suspend(penv, objhash_null);
    Java_java_lang_Thread_currentThread(penv, clsidx_null);


    /* Unregister natives for library unload, where implemented */
    Java_java_lang_Object_unregisterNatives(penv, clsidx_null);
    Java_java_lang_Class_unregisterNatives(penv, clsidx_null);
    Java_java_lang_String_unregisterNatives(penv, clsidx_null);
    Java_java_lang_Thread_unregisterNatives(penv, clsidx_null);

    exit(0);

} /* END of main() */


/* EOF */
