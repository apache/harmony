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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "hyport.h"
#include "libhlp.h"

I_32
main_appendToClassPath (HyPortLibrary * portLib, U_16 sep,
                        HyStringBuffer ** classPath, char *toAppend)
{

  /* append a separator, first */
  if (*classPath && (*classPath)->data[strlen ((char *)((*classPath)->data))] != sep)
    {
      char separator[2];
      separator[0] = (char) sep;
      separator[1] = '\0';
      *classPath = strBufferCat (portLib, *classPath, separator);
      if (*classPath == NULL)
        return -1;
    }

  *classPath = strBufferCat (portLib, *classPath, toAppend);
  if (*classPath == NULL)
    return -1;

  return 0;
}

/* Allocates and retrieves initial value of the classPath. */

I_32
main_initializeClassPath (HyPortLibrary * portLib,
                          HyStringBuffer ** classPath)
{
  PORT_ACCESS_FROM_PORT (portLib);
  I_32 rc;
  char *envvars = "CLASSPATH\0classpath\0";
  char *envvar;

  for (envvar = envvars; *envvar; envvar += strlen (envvar) + 1)
    {
      rc = hysysinfo_get_env (envvar, NULL, 0);
      if (rc > 0)
        {
          *classPath = strBufferEnsure (portLib, *classPath, rc);
          if (*classPath == NULL)
            return -1;
          hysysinfo_get_env (envvar,
                             (char *)((*classPath)->data) + strlen ((char *)((*classPath)->data)),
                             (U_32)rc);
          (*classPath)->remaining -= rc;
          break;
        }
    }

  return 0;
}

IDATA
main_initializeJavaHome (HyPortLibrary * portLib,
                         HyStringBuffer ** finalJavaHome, int argc,
                         char **argv)
{
  char *javaHome = NULL;
  char *javaHomeModifiablePart = NULL;
  char *p;
  IDATA retval = -1;

  PORT_ACCESS_FROM_PORT (portLib);

  if ((argc < 1) || !argv)
    return -1;

  retval = hysysinfo_get_executable_name (argv[0], &javaHome);
  if (retval)
    {
      /* guess.  How about ".."? */
      *finalJavaHome = strBufferCat (portLib, *finalJavaHome, "..");
      return 0;
    }

  javaHomeModifiablePart = javaHome;
#if defined(WIN32)
  /* Make sure we don't modify a drive specifier in a pathname. */
  if ((strlen (javaHome) > 2) && (javaHome[1] == ':'))
    {
      javaHomeModifiablePart = javaHome + 2;
      if (javaHome[2] == DIR_SEPARATOR)
        javaHomeModifiablePart++;
    }
#endif

#if defined(WIN32)
  /* Make sure we don't modify the root of a UNC pathname. */
  if ((strlen (javaHome) > 2) && (javaHome[0] == DIR_SEPARATOR)
      && (javaHome[1] == DIR_SEPARATOR))
    {
      javaHomeModifiablePart = javaHome + 2;
      /* skip over the machine name */
      while (*javaHomeModifiablePart
             && (*javaHomeModifiablePart != DIR_SEPARATOR))
        {
          javaHomeModifiablePart++;
        }
      if (*javaHomeModifiablePart)
        javaHomeModifiablePart++;
      /* skip over the share name */
      while (*javaHomeModifiablePart
             && (*javaHomeModifiablePart != DIR_SEPARATOR))
        {
          javaHomeModifiablePart++;
        }
    }
#endif

  if ((javaHomeModifiablePart == javaHome) && javaHome[0] == DIR_SEPARATOR)
    {
      /* make sure we don't modify a root slash. */
      javaHomeModifiablePart++;
    }

  /* Note: if sysinfo_get_executable_name claims we were invoked from a root directory, */
  /* then this code will return that root directory for java.home also. */
  p = strrchr (javaHomeModifiablePart, DIR_SEPARATOR);
  if (!p)
    {
      javaHomeModifiablePart[0] = '\0'; /* chop off whole thing! */
    }
  else
    {
      p[0] = '\0';              /* chop off trailing slash and executable name. */
      p = strrchr (javaHomeModifiablePart, DIR_SEPARATOR);
      if (!p)
        {
          javaHomeModifiablePart[0] = '\0';     /* chop off the rest */
        }
      else
        {
          p[0] = '\0';          /* chop off trailing slash and deepest subdirectory. */
        }
    }

  *finalJavaHome = strBufferCat (portLib, *finalJavaHome, javaHome);

  hymem_free_memory (javaHome);

  return 0;
}

IDATA
main_initializeJavaLibraryPath (HyPortLibrary * portLib,
                                HyStringBuffer ** finalJavaLibraryPath,
                                char *argv0)
{
  HyStringBuffer *javaLibraryPath = NULL;
  char *exeName = NULL;
  IDATA rc = -1;
  char *p;
  char *envResult;
  int envSize;
#define ENV_BUFFER_SIZE 80
  char envBuffer[ENV_BUFFER_SIZE];
  char sep[2];
  PORT_ACCESS_FROM_PORT (portLib);

  sep[0] = (char) hysysinfo_get_classpathSeparator ();
  sep[1] = '\0';

  if (hysysinfo_get_executable_name (argv0, &exeName))
    {
      goto done;
    }
  p = strrchr (exeName, DIR_SEPARATOR);
  if (p)
    {
      p[1] = '\0';
    }
  else
    {
      hymem_free_memory (exeName);
      exeName = NULL;
    }

  envSize = hysysinfo_get_env (LIBPATH_ENV_VAR, NULL, 0);
  if (envSize > 0)
    {
      if (envSize >= ENV_BUFFER_SIZE)
        {
          envResult = hymem_allocate_memory (envSize + 1);
          if (!envResult)
            goto done;
          hysysinfo_get_env (LIBPATH_ENV_VAR, envResult, envSize);
        }
      else
        {
          envSize = -1;         /* make it -1 so we don't free the buffer */
          hysysinfo_get_env (LIBPATH_ENV_VAR, envBuffer, ENV_BUFFER_SIZE);
          envResult = envBuffer;
        }
    }
  else
    {
      envResult = NULL;
    }

  /* Add one to each length to account for the separator character.  Add 2 at the end for the "." and NULL terminator */

  if (exeName)
    {
      javaLibraryPath = strBufferCat (portLib, javaLibraryPath, exeName);
      javaLibraryPath = strBufferCat (portLib, javaLibraryPath, sep);
    }
  javaLibraryPath = strBufferCat (portLib, javaLibraryPath, ".");
  if (envResult)
    {
      javaLibraryPath = strBufferCat (portLib, javaLibraryPath, sep);
      javaLibraryPath = strBufferCat (portLib, javaLibraryPath, envResult);
      if (envSize != -1)
        {
          hymem_free_memory (envResult);
        }
    }

  rc = 0;

done:
  if (exeName)
    {
      hymem_free_memory (exeName);
    }
  *finalJavaLibraryPath = javaLibraryPath;
  return rc;

}

IDATA
convertString (JNIEnv * env, HyPortLibrary * hyportLibrary,
               jclass stringClass, jmethodID stringMid, char *chars,
               jstring * str)
{
  UDATA strLength;
  jarray bytearray;
  jstring string;

  strLength = strlen (chars);
  bytearray = (*env)->NewByteArray (env, strLength);
  if (((*env)->ExceptionCheck (env)))
    return 1;

  (*env)->SetByteArrayRegion (env, bytearray, (UDATA) 0, strLength, (jbyte*)chars);

  string =
    (*env)->NewObject (env, stringClass, stringMid, bytearray, (UDATA) 0,
                       strLength);
  if (!string)
    return 2;

  (*env)->DeleteLocalRef (env, bytearray);

  *str = string;
  return 0;
}
