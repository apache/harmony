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
#include "vmi.h"

#include "java_util_prefs_RegistryPreferencesImpl.h"

// max buffer size in bytes
#define MAX_KEY_LENGTH 255
#define MAX_VALUE_NAME 255
#define MAX_VALUE_DATA 16383

//map windows error code to customized error code
int
checkErrorCode (DWORD err)
{
  switch (err)
    {
    case ERROR_ACCESS_DENIED:
      return java_util_prefs_RegistryPreferencesImpl_RETURN_ACCESS_DENIED;
    case ERROR_SUCCESS:
      return java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS;
    case ERROR_FILE_NOT_FOUND:
      return java_util_prefs_RegistryPreferencesImpl_RETURN_FILE_NOT_FOUND;
    default:
      return java_util_prefs_RegistryPreferencesImpl_RETURN_UNKNOWN_ERROR;
    }
}

// convert java byte array to c string
jbyte *
byte2str (JNIEnv * env, const jbyteArray jkey)
{
  PORT_ACCESS_FROM_ENV (env);
  jboolean isCopy = JNI_TRUE;
  jbyte *keyByte = (*env)->GetPrimitiveArrayCritical (env, jkey, &isCopy);
  const jsize keyLen = (*env)->GetArrayLength (env, jkey);
  jbyte *keyStr = hymem_allocate_memory ((keyLen + 1) * sizeof (jbyte));
  jsize i = 0;

  if (keyByte == NULL)
    {
      return NULL;
    }
  while (i < keyLen)
    keyStr[i] = keyByte[i++];
  keyStr[i] = '\0';

  (*env)->ReleasePrimitiveArrayCritical (env, jkey, keyByte, JNI_ABORT);
  return keyStr;
}

DWORD
openRegKey (JNIEnv * env, jbyteArray jpath, jboolean jUserNode, PHKEY phKey,
	    REGSAM samDesired)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte *path = byte2str (env, jpath);
  const HKEY root = jUserNode ? HKEY_CURRENT_USER : HKEY_LOCAL_MACHINE;
  DWORD err;

  err = RegOpenKeyEx (root, path, 0, samDesired, phKey);
  if (err == ERROR_FILE_NOT_FOUND)
    {
      err =
	RegCreateKeyEx (root, path, 0, NULL, REG_OPTION_NON_VOLATILE,
			samDesired, NULL, phKey, NULL);
    }
  hymem_free_memory (path);
  return err;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    getValue
 * Signature: ([B[BZ[I)[B
 */
JNIEXPORT jbyteArray JNICALL
  Java_java_util_prefs_RegistryPreferencesImpl_getValue
  (JNIEnv * env, jobject obj, jbyteArray jpath, jbyteArray jkey,
   jboolean jUserNode, jintArray jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  HKEY hKey;
  DWORD type = REG_SZ, errorCode;
  jbyteArray result;
  jboolean isCopy = JNI_TRUE;
  jint *err = NULL;
  jbyte *keyStr = NULL;
  TCHAR *value;
  DWORD dwBufLen = MAX_VALUE_DATA + 1;
  int localErrorCode = 0;

  err = (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);
  keyStr = byte2str (env, jkey);
  errorCode = openRegKey (env, jpath, jUserNode, &hKey, KEY_QUERY_VALUE);
  localErrorCode = checkErrorCode (errorCode);
  err[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] = localErrorCode;
  if (localErrorCode !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, err, 0);
      RegCloseKey (hKey);
      hymem_free_memory (keyStr);
      return NULL;
    }
  value = hymem_allocate_memory ((MAX_VALUE_DATA + 1) * sizeof (TCHAR));
  errorCode = RegQueryValueEx (hKey, keyStr, 0, &type, value, &dwBufLen);
  localErrorCode = checkErrorCode (errorCode);
  RegCloseKey (hKey);
  hymem_free_memory (keyStr);
  err[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] = localErrorCode;
  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, err, 0);
  if (localErrorCode !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
      hymem_free_memory (value);
      return NULL;
    }
  if(dwBufLen > 0)
    {
      result = (*env)->NewByteArray (env, dwBufLen-1);
      (*env)->SetByteArrayRegion (env, result, 0, dwBufLen-1, value);
    }
      else
    {
      result = (*env)->NewByteArray (env, 0);
    }
  hymem_free_memory (value);
  return result;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    putValue
 * Signature: ([B[B[BZ[I)V
 */
JNIEXPORT void JNICALL Java_java_util_prefs_RegistryPreferencesImpl_putValue
  (JNIEnv * env, jobject obj, jbyteArray jpath, jbyteArray jkey,
   jbyteArray jvalue, jboolean jUserNode, jintArray jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  DWORD err;
  HKEY hKey;
  jbyte *keyStr;
  jbyte *valueByte;
  int localErrorCode = 0;
  const jint valueLen = (*env)->GetArrayLength (env, jvalue);
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_SET_VALUE);
  localErrorCode = checkErrorCode (err);
  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
    localErrorCode;
  if (localErrorCode !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return;
    }

  keyStr = byte2str (env, jkey);
  valueByte = byte2str (env, jvalue);
  err = RegSetValueEx (hKey, keyStr, 0, REG_SZ, valueByte, valueLen);
  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
    checkErrorCode (err);
      (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);
  hymem_free_memory (keyStr);
  hymem_free_memory (valueByte);
  RegCloseKey (hKey);
  return;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    removeKey
 * Signature: ([B[BZ[I)V
 */
JNIEXPORT void JNICALL Java_java_util_prefs_RegistryPreferencesImpl_removeKey
  (JNIEnv * env, jobject obj, jbyteArray jpath, jbyteArray jkey,
   jboolean jUserNode, jintArray jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  DWORD err;
  HKEY hKey;
  jbyte *keyStr;
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_SET_VALUE);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return;
    }

  keyStr = byte2str (env, jkey);
  err = RegDeleteValue (hKey, keyStr);
  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
    checkErrorCode (err);
      (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);
  hymem_free_memory (keyStr);
  RegCloseKey (hKey);
  return;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    keys
 * Signature: ([BZ[I)[[B
 */
JNIEXPORT jobjectArray JNICALL
  Java_java_util_prefs_RegistryPreferencesImpl_keys
  (JNIEnv * env, jobject obj, jbyteArray jpath, jboolean jUserNode,
   jintArray jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  DWORD number, size;
  jclass objectClazz;
  jobjectArray result;
  TCHAR *value;
  DWORD i = 0, j, err;
  HKEY hKey;
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_QUERY_VALUE);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return NULL;
    }

  err =
    RegQueryInfoKey (hKey, NULL, NULL, NULL, NULL, NULL, NULL, &number, NULL,
		     NULL, NULL, NULL);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      RegCloseKey (hKey);
      return NULL;
    }
  objectClazz = (*env)->FindClass (env, "java/lang/Object");
  result = (*env)->NewObjectArray (env, number, objectClazz, NULL);

  value = hymem_allocate_memory ((MAX_VALUE_NAME + 1) * sizeof (TCHAR));
  for (j = 0; j < number; j++)
    {
      value[0] = '\0';
      size = MAX_VALUE_NAME + 1;
      err = RegEnumValue (hKey, j, value, &size, NULL, NULL, NULL, NULL);
      if (err == ERROR_SUCCESS)
	{
	  jbyteArray key = (*env)->NewByteArray (env, strlen (value));
	  (*env)->SetByteArrayRegion (env, key, 0, strlen (value), value);
	  (*env)->SetObjectArrayElement (env, result, i++, key);
	}
      else
	{
	  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
	    checkErrorCode (err);
	}
    }
  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);
  hymem_free_memory (value);
  RegCloseKey (hKey);
  return result;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    removeNode
 * Signature: ([B[BZ[I)V
 */
JNIEXPORT void JNICALL Java_java_util_prefs_RegistryPreferencesImpl_removeNode
  (JNIEnv * env, jobject obj, jbyteArray jpath, jbyteArray jname,
   jboolean jUserNode, jintArray jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte *nameStr;
  HKEY hKey;
  DWORD err;
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_SET_VALUE);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return;
    }

  nameStr = byte2str (env, jname);
  err = RegDeleteKey (hKey, nameStr);
  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
    checkErrorCode (err);
  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);
  hymem_free_memory (nameStr);
  RegCloseKey (hKey);
  return;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    getNode
 * Signature: ([B[BZ[I)Z
 */
JNIEXPORT jboolean JNICALL
  Java_java_util_prefs_RegistryPreferencesImpl_getNode
  (JNIEnv * env, jobject obj, jbyteArray jpath, jbyteArray jname,
   jboolean jUserNode, jintArray jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte *name;
  HKEY hKey, childKey;
  DWORD dwDisposition, err;
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_QUERY_VALUE);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return JNI_FALSE;
    }

  name = byte2str (env, jname);
  err =
    RegCreateKeyEx (hKey, name, 0, NULL, REG_OPTION_NON_VOLATILE,
		    KEY_ALL_ACCESS, NULL, &childKey, &dwDisposition);
  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
    checkErrorCode (err);

  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);

  hymem_free_memory (name);
  RegCloseKey (hKey);
  RegCloseKey (childKey);

  return dwDisposition == REG_CREATED_NEW_KEY;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    getChildNames
 * Signature: ([BZ[I)[[B
 */
JNIEXPORT jobjectArray JNICALL
Java_java_util_prefs_RegistryPreferencesImpl_getChildNames (JNIEnv * env,
							    jobject obj,
							    jbyteArray jpath,
							    jboolean
							    jUserNode,
							    jintArray
							    jErrorCode)
{
  PORT_ACCESS_FROM_ENV (env);
  DWORD number, size;
  jclass objectClazz = NULL;
  jobjectArray result = NULL;
  TCHAR *value;
  DWORD i = 0, j;
  HKEY hKey;
  DWORD err;
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_READ);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return NULL;
    }

  err =
    RegQueryInfoKey (hKey, NULL, NULL, NULL, &number, NULL, NULL, NULL, NULL,
		     NULL, NULL, NULL);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      RegCloseKey (hKey);
      return NULL;
    }

  objectClazz = (*env)->FindClass (env, "java/lang/Object");
  result = (*env)->NewObjectArray (env, number, objectClazz, NULL);
  value = hymem_allocate_memory ((MAX_VALUE_DATA + 1) * sizeof (TCHAR));
  for (j = 0; j < number; j++)
    {
      value[0] = '\0';
      size = MAX_VALUE_DATA + 1;
      err = RegEnumKeyEx (hKey, j, value, &size, NULL, NULL, NULL, NULL);
      if (err == ERROR_SUCCESS)
	{
	  jbyteArray name = (*env)->NewByteArray (env, strlen (value));
	  (*env)->SetByteArrayRegion (env, name, 0, strlen (value), value);
	  (*env)->SetObjectArrayElement (env, result, i++, name);
	}
      else
	{
	  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
	    checkErrorCode (err);
	}
    }

  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);
  hymem_free_memory (value);
  RegCloseKey (hKey);
  return result;
}

/*
 * Class:     java_util_prefs_RegistryPreferencesImpl
 * Method:    flushPrefs
 * Signature: ([BZ[I)V
 */
JNIEXPORT void JNICALL Java_java_util_prefs_RegistryPreferencesImpl_flushPrefs
  (JNIEnv * env, jobject obj, jbyteArray jpath, jboolean jUserNode,
   jintArray jErrorCode)
{
  HKEY hKey;
  DWORD err;
  jboolean isCopy = JNI_TRUE;
  jint *errArray =
    (*env)->GetPrimitiveArrayCritical (env, jErrorCode, &isCopy);

  err = openRegKey (env, jpath, jUserNode, &hKey, KEY_ALL_ACCESS);
  if ((errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
       checkErrorCode (err)) !=
      java_util_prefs_RegistryPreferencesImpl_RETURN_SUCCESS)
    {
	  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray,
						 0);
      return;
    }

  errArray[java_util_prefs_RegistryPreferencesImpl_ERROR_CODE] =
    checkErrorCode (RegFlushKey (hKey));

  (*env)->ReleasePrimitiveArrayCritical (env, jErrorCode, errArray, 0);
  RegCloseKey (hKey);
  return;
}
