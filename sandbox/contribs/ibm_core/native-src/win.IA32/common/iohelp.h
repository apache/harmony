/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#if !defined(iohelp_h)
#define iohelp_h

#include <string.h>
#include "jcl.h"

/* DIR_SEPARATOR is defined in hycomp.h */
#define jclSeparator DIR_SEPARATOR

void *getJavaIoFileDescriptorContentsAsPointer (JNIEnv * env, jobject fd);
void throwNewOutOfMemoryError (JNIEnv * env, char *message);
jint ioh_readcharImpl (JNIEnv * env, jobject recv, IDATA descriptor);
void throwJavaIoIOException (JNIEnv * env, char *message);
void throwJavaIoIOExceptionClosed (JNIEnv * env);
void ioh_convertToPlatform (char *path);
jint new_ioh_available (JNIEnv * env, jobject recv, jfieldID fdFID);
void throwNPException (JNIEnv * env, char *message);
void setJavaIoFileDescriptorContentsAsPointer (JNIEnv * env, jobject fd,
                                               void *value);
void ioh_writebytesImpl (JNIEnv * env, jobject recv, jbyteArray buffer,
                         jint offset, jint count, IDATA descriptor);
char *ioLookupErrorString (JNIEnv * env, I_32 anErrorNum);
void ioh_writecharImpl (JNIEnv * env, jobject recv, jint c, IDATA descriptor);
jint ioh_readbytesImpl (JNIEnv * env, jobject recv, jbyteArray buffer,
                        jint offset, jint count, IDATA descriptor);
void new_ioh_close (JNIEnv * env, jobject recv, jfieldID fdFID);
void throwIndexOutOfBoundsException (JNIEnv * env);

#endif /* iohelp_h */
