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

#include "iohelp.h"
#include "jclglob.h"

void
ioh_convertToPlatform (char *path)
{
  char *pathIndex;
  int length = strlen (path);

  /* Convert all separators to the same type */
  pathIndex = path;
  while (*pathIndex != '\0')
    {
      if ((*pathIndex == '\\' || *pathIndex == '/')
          && (*pathIndex != jclSeparator))
        *pathIndex = jclSeparator;
      pathIndex++;
    }

  /* Remove duplicate separators */
  if (jclSeparator == '/')
    return;                     /* Do not do POSIX platforms */

  /* Remove duplicate initial separators */
  pathIndex = path;
  while ((*pathIndex != '\0') && (*pathIndex == jclSeparator))
    {
      pathIndex++;
    }

  if ((pathIndex > path) && (length > (pathIndex - path))
      && (*(pathIndex + 1) == ':'))
    {
      /* For Example '////c:/*' */
      int newlen = length - (pathIndex - path);
      memmove (path, pathIndex, newlen);
      path[newlen] = '\0';
    }
  else
    {
      if ((pathIndex - path > 3) && (length > (pathIndex - path)))
        {
          /* For Example '////serverName/*' */
          int newlen = length - (pathIndex - path) + 2;
          memmove (path, pathIndex - 2, newlen);
          path[newlen] = '\0';
        }
    }
  /* This will have to handle extra \'s but currently doesn't */
}

/**
  * Throw java.lang.OutOfMemoryError
  */
void
throwNewOutOfMemoryError (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}
