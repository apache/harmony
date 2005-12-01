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

#if !defined(helpers_h)
#define helpers_h
#include "jcl.h"
int platformReadLink (char *link);
jbyteArray getPlatformPath (JNIEnv * env, jbyteArray path);
void setDefaultServerSocketOptions (JNIEnv * env, hysocket_t socketP);
I_32 getPlatformRoots (char *rootStrings);
char *getCommports (JNIEnv * env);
jint getPlatformDatagramNominalSize (JNIEnv * env, hysocket_t socketP);
I_32 getPlatformIsHidden (JNIEnv * env, char *path);
jstring getCustomTimeZoneInfo (JNIEnv * env, jintArray tzinfo,
                               jbooleanArray isCustomTimeZone);
jint getPlatformDatagramMaxSize (JNIEnv * env, hysocket_t socketP);
I_32 getPlatformIsWriteOnly (JNIEnv * env, char *path);
I_32 setPlatformFileLength (JNIEnv * env, IDATA descriptor, jlong newLength);
I_32 getPlatformIsReadOnly (JNIEnv * env, char *path);
void setPlatformBindOptions (JNIEnv * env, hysocket_t socketP);
I_32 setPlatformLastModified (JNIEnv * env, char *path, I_64 time);
I_32 setPlatformReadOnly (JNIEnv * env, char *path);
int portCmp (const void **a, const void **b);
#endif /* helpers_h */
