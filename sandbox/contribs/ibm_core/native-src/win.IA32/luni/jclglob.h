/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
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

#if !defined(jclglob_h)
#define jclglob_h
#include "jcl.h"
extern void *LUNI_JCL_ID_CACHE;
#define JCL_ID_CACHE LUNI_JCL_ID_CACHE
typedef struct LUNIJniIDCache
{

  jfieldID FID_java_io_RandomAccessFile_fd;
  jfieldID FID_java_io_FileDescriptor_descriptor;
  jfieldID FID_java_io_FileInputStream_fd;
  jfieldID FID_java_io_FileOutputStream_fd;
  jfieldID FID_com_ibm_oti_lang_ProcessInputStream_handle;
  jfieldID FID_com_ibm_oti_lang_ProcessInputStream_fd;
  jfieldID FID_com_ibm_oti_lang_ProcessOutputStream_handle;
  jfieldID FID_com_ibm_oti_lang_ProcessOutputStream_fd;
  jfieldID FID_com_ibm_oti_lang_SystemProcess_handle;
  jfieldID FID_java_net_SocketImpl_address;
  jfieldID FID_java_net_SocketImpl_port;
  jfieldID FID_java_net_DatagramPacket_length;
  jfieldID FID_java_net_DatagramPacket_address;
  jfieldID FID_java_net_DatagramPacket_port;
  jfieldID FID_java_lang_ClassLoader_vmRef;
  jfieldID FID_com_ibm_oti_vm_MemorySpace;
  /* Used by ObjectStreamClass */
  jmethodID MID_java_lang_reflect_Field_getSignature;
  jmethodID MID_java_lang_reflect_Method_getSignature;
  jmethodID MID_java_lang_reflect_Constructor_getSignature;
  /* Used by shared net helpers */
  jmethodID MID_java_lang_Boolean_init;
  jmethodID MID_java_lang_Byte_init;
  jmethodID MID_java_lang_Integer_init;
  jmethodID MID_java_net_InetAddress_init_byteArray;
  jmethodID MID_java_net_InetAddress_init_byteArrayLjava_lang_String;
  jmethodID MID_java_lang_Thread_yield;
  jmethodID MID_java_net_InetAddress_getByAddress_Ljava_lang_String_byteArray;
  jmethodID MID_java_net_InetAddress_getByAddress_byteArray;
  jmethodID MID_java_net_InetAddress_preferIPv6Addresses;
  jmethodID MID_java_net_Socket_preferIPv4Stack;
  jclass CLS_java_lang_Boolean;
  jclass CLS_java_lang_Byte;
  jclass CLS_java_lang_Integer;
  jclass CLS_java_lang_Thread;
  jclass CLS_java_net_InetAddress;
  jclass CLS_java_net_Socket;
  jfieldID FID_java_lang_Boolean_value;
  jfieldID FID_java_lang_Byte_value;
  jfieldID FID_java_lang_Integer_value;
  jfieldID FID_java_net_InetAddress_address;
  jclass CLS_array_of_byte;
  char **realPortArray;
  char **synthPortArray;
  int portListLen;

  jboolean jcl_supports_ipv6;
} LUNIJniIDCache;
#define JniIDCache LUNIJniIDCache
/* Now that the module-specific defines are in place, include the shared file */
#include "libglob.h"
/* all UNIX platforms */
#if defined(LINUX)
#include "hysignal.h"
#endif /* UNIX */
#endif /* jclglob_h */
