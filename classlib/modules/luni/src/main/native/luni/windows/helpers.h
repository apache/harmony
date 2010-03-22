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

#if !defined(helpers_h)
#define helpers_h

/* structure for returning interface address information */
typedef struct interfaceAddress_struct
{
  U_32 prefixLength;
  struct hyipAddress_struct *address;
  //struct hyipAddress_struct *mask;
  //struct hyipAddress_struct *broadcast;
} interfaceAddress_struct;

/* array of interface address structures */
typedef struct interfaceAddressArray_struct
{
  U_32 length;
  struct interfaceAddress_struct *elements;
} interfaceAddressArray_struct;

int platformReadLink (char *link);
void setDefaultServerSocketOptions (JNIEnv * env, hysocket_t socketP);
jint getPlatformDatagramNominalSize (JNIEnv * env, hysocket_t socketP);
I_32 getPlatformRoots (char *rootStrings);
jstring getCustomTimeZoneInfo (JNIEnv * env, jintArray tzinfo,
             jbooleanArray isCustomTimeZone);
I_32 getPlatformIsHidden (JNIEnv * env, char *path);
jint getPlatformDatagramMaxSize (JNIEnv * env, hysocket_t socketP);
char *getCommports (JNIEnv * env);
I_32 getPlatformIsWriteOnly (JNIEnv * env, char *path);
I_32 setPlatformFileLength (JNIEnv * env, IDATA descriptor, jlong newLength);
void platformCanonicalPath (char *pathCopy);
I_32 getPlatformIsReadOnly (JNIEnv * env, char *path);
void setPlatformBindOptions (JNIEnv * env, hysocket_t socketP);
I_32 setPlatformLastModified (JNIEnv * env, char *path, I_64 time);
I_32 setPlatformReadOnly (JNIEnv * env, char *path);
I_32 setPlatformWritable (JNIEnv * env, char *path, jboolean writable, jboolean ownerOnly);
jlong getPlatformTotal (JNIEnv * env, char *path);
jlong getPlatformUsableTotal (JNIEnv * env, char *path);
jlong getPlatformFreeTotal (JNIEnv * env, char *path);
char* convertInterfaceName(JNIEnv * env, jstring ifname);
jboolean getPlatformIsUp(JNIEnv * env, jstring ifname, jint index);
jboolean getPlatformIsLoopback(JNIEnv * env, jstring ifname, jint index);
jboolean getPlatformIsPoint2Point(JNIEnv * env, jstring ifname, jint index);
jboolean getPlatformSupportMulticast(JNIEnv * env, jstring ifname, jint index);
jint getPlatformMTU(JNIEnv * env, jstring ifname, jint index);
jbyteArray getPlatformHardwareAddress(JNIEnv * env, jstring ifname, jint index);
I_32 getPlatformInterfaceAddresses(JNIEnv * env, jstring ifname, jint index, interfaceAddressArray_struct* interfaceAddressArray);
I_32 freeInterfaceAddressArray(JNIEnv * env, struct interfaceAddressArray_struct * array);
void getOSCharset(char *locale, const size_t size);
#endif /* helpers_h */
