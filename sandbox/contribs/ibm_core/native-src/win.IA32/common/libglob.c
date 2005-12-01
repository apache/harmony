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

/**
 * @file
 * @ingroup HarmonyNatives
 * @brief Common helpers initialization API.
 */

#include "jcl.h"
#include "jclglob.h"

static UDATA keyInitCount = 0;

void *JCL_ID_CACHE = NULL;

static void freeReferences (JNIEnv * env);

/**
 * A DLL is attaching to the common code, do any initialization required.
 * This may be called more than once.
 */
jint JNICALL
ClearLibAttach (JNIEnv * env)
{
  void *keyInitCountPtr = GLOBAL_DATA (keyInitCount);
  void **jclIdCache = GLOBAL_DATA (JCL_ID_CACHE);
  JniIDCache *idCache;

  PORT_ACCESS_FROM_ENV (env);

  if (HY_VMLS_FNTBL (env)->
      HYVMLSAllocKeys (env, keyInitCountPtr, jclIdCache, NULL))
    {
      goto fail;
    }

  idCache = (JniIDCache *) HY_VMLS_GET (env, *jclIdCache);
  if (idCache == NULL)
    {
      idCache = (JniIDCache *) hymem_allocate_memory (sizeof (JniIDCache));
      if (!idCache)
        goto fail2;

      memset (idCache, 0, sizeof (JniIDCache));
      HY_VMLS_SET (env, *jclIdCache, idCache);
    }

  /* Increment the reference count. */
  idCache->attachCount++;

  return JNI_OK;

fail2:
  HY_VMLS_FNTBL (env)->HYVMLSFreeKeys (env, keyInitCountPtr, jclIdCache, NULL);
fail:
  return JNI_ERR;
}

/**
 * A DLL is detaching from the common code, do any clean up required.
 * This may be called more than once!!
 */
void JNICALL
ClearLibDetach (JNIEnv * env)
{
  void *keyInitCountPtr = GLOBAL_DATA (keyInitCount);
  void **jclIdCache = GLOBAL_DATA (JCL_ID_CACHE);
  JniIDCache *idCache;

  PORT_ACCESS_FROM_ENV (env);

  idCache = (JniIDCache *) HY_VMLS_GET (env, *jclIdCache);
  if (idCache)
    {
      /* Decrement the reference count and free if necessary. */
      if (--idCache->attachCount < 1)
        {
          freeReferences (env);

          /* Free VMLS keys */
          idCache = (JniIDCache *) HY_VMLS_GET (env, *jclIdCache);
          HY_VMLS_FNTBL (env)->HYVMLSFreeKeys (env, keyInitCountPtr,
                                              jclIdCache, NULL);
          hymem_free_memory (idCache);
        }
    }
}

/**
 * @internal
 */
static void
freeReferences (JNIEnv * env)
{
  /* empty */
}
