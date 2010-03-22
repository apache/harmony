/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Common natives supporting the memory system interface.
 */

#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include "OSMemory.h"
#include "IMemorySystem.h"


#ifdef _WIN64
	#define __ptr64 __int64 *
#endif

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_luni_platform_OSMemory_isLittleEndianImpl
  (JNIEnv * env, jclass clazz)
{
  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_luni_platform_OSMemory_getPointerSizeImpl
  (JNIEnv * env, jclass clazz)
{
#if defined(_WIN64)
  return 8;
#else
  return 4;
#endif

}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_luni_platform_OSMemory_getAddress
  (JNIEnv * env, jobject thiz, jlong address)
{
#if defined(_WIN64)
  return *((POINTER_64) address);
#else
  return (jlong) * (long *) address;
#endif

}

JNIEXPORT void JNICALL Java_org_apache_harmony_luni_platform_OSMemory_setAddress
  (JNIEnv * env, jobject thiz, jlong address, jlong value)
{
#if defined(_WIN64)
  *((POINTER_64) address) = value;
#else
  *(long *) address = (long) value;
#endif

}


JNIEXPORT jint JNICALL Java_org_apache_harmony_luni_platform_OSMemory_loadImpl
  (JNIEnv * env, jobject thiz, jlong addr, jlong size){
    /* FIXME:
     * lock the memory make the pages be loaded into physical memory
     * and unlock then to make them swappable
     * maybe can be improved
     * */
    if(0 != VirtualLock((void *)addr, (SIZE_T)size)){
        VirtualUnlock((void *)addr, (SIZE_T)size);
        return 0;
    }else{
        printf("lock error: %d\n", GetLastError());
        return -1;
    }
  }

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_luni_platform_OSMemory_isLoadedImpl
  (JNIEnv * env, jobject thiz, jlong addr, jlong size){
    /* FIXME:
     * Windows only provides readiness status of memory pages
     * by VirtualQuery(EX), seems not enough to support this 
     * function
     * */
    return JNI_FALSE;
  }

JNIEXPORT jint JNICALL Java_org_apache_harmony_luni_platform_OSMemory_flushImpl
  (JNIEnv * env, jobject thiz, jlong addr, jlong size){
    return (jint)FlushViewOfFile((void *)addr, (SIZE_T)size);
  }

/*
 * Class:     org_apache_harmony_luni_platform_OSMemory
 * Method:    unmapImpl
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_luni_platform_OSMemory_unmapImpl
  (JNIEnv * env, jobject thiz, jlong addr, jlong size)
{
  UnmapViewOfFile ((HANDLE)addr);
}

/*
 * Class:     org_apache_harmony_luni_platform_OSMemory
 * Method:    mmapImpl
 * Signature: (JJJI)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_luni_platform_OSMemory_mmapImpl
  (JNIEnv * env, jobject thiz, jlong fd, jlong alignment, jlong size, jint mmode)
{
  void *mapAddress = NULL;
  int mapmode = 0;
  int protect = 0;
  HANDLE mapping;

  switch (mmode)
  {
    case org_apache_harmony_luni_platform_IMemorySystem_MMAP_READ_ONLY:
      mapmode = FILE_MAP_READ;
      protect = PAGE_READONLY;
      break;
    case org_apache_harmony_luni_platform_IMemorySystem_MMAP_READ_WRITE:
      mapmode = FILE_MAP_WRITE;
      protect = PAGE_READWRITE;
      break;
    case org_apache_harmony_luni_platform_IMemorySystem_MMAP_WRITE_COPY:
      mapmode = FILE_MAP_COPY;
      protect = PAGE_WRITECOPY;
      break;
    default:
      return -1;
  }

  mapping = CreateFileMapping ((HANDLE)fd, NULL, protect, 0, 0, NULL);
  if (mapping == NULL)
    {
      return -1;
    }
  mapAddress = MapViewOfFile (mapping, mapmode, (DWORD)((alignment>>0x20)&0x7fffffff), (DWORD)(alignment&0xffffffff), (SIZE_T)(size&0xffffffff));
  CloseHandle (mapping);

  if (mapAddress == NULL)
    {
      return -1;
    }
  
  return (jlong) mapAddress;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_luni_platform_OSMemory_getCharArray
  (JNIEnv * env, jobject thiz, jlong address, jcharArray jdest, jint offset, jint length)
{
  jboolean isCopy;
  jchar *dest = (*env)->GetCharArrayElements (env, jdest, &isCopy);
  memcpy (dest + offset, (const void *) address, (size_t) length);
  (*env)->ReleaseCharArrayElements (env, jdest, dest, 0);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_luni_platform_OSMemory_setCharArray
  (JNIEnv * env, jobject thiz, jlong address, jcharArray jsrc, jint offset, jint length)
{
  jboolean isCopy;
  jchar *src = (*env)->GetCharArrayElements (env, jsrc, &isCopy);
  memcpy ((void *) address, (const jchar *) src + offset, (size_t) length);
  (*env)->ReleaseCharArrayElements (env, jsrc, src, JNI_ABORT);
}
