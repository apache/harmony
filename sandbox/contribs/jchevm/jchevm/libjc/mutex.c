
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id: mutex.c,v 1.2 2004/07/05 21:03:27 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * Initialize a mutex
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_mutex_init(_jc_env *env, pthread_mutex_t *mutex)
{
	pthread_mutexattr_t attr;
	int error;

	/* Initialize mutex attributes */
	if ((error = pthread_mutexattr_init(&attr)) != 0) {
		_JC_EX_STORE(env, InternalError,
		    "%s: %s", "pthread_mutexattr_init", strerror(error));
		return JNI_ERR;
	}

#if NDEBUG
	/* Enable debug checks */
	if ((error = pthread_mutexattr_settype(&attr,
	    PTHREAD_MUTEX_ERRORCHECK)) != 0) {
		_JC_EX_STORE(env, InternalError,
		    "%s: %s", "pthread_mutexattr_settype", strerror(error));
		pthread_mutexattr_destroy(&attr);
		return JNI_ERR;
	}
#endif

	/* Initialize mutex */
	if ((error = pthread_mutex_init(mutex, &attr)) != 0) {
		_JC_EX_STORE(env, InternalError,
		    "%s: %s", "pthread_mutex_init", strerror(error));
		pthread_mutexattr_destroy(&attr);
		return JNI_ERR;
	}

	/* Clean up */
	pthread_mutexattr_destroy(&attr);
	return JNI_OK;
}

/*
 * Destroy a mutex
 */
void
_jc_mutex_destroy(pthread_mutex_t *mutex)
{
	int r;

	r = pthread_mutex_destroy(mutex);
	_JC_ASSERT(r == 0);
}

/*
 * Initialize a condition variable.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_cond_init(_jc_env *env, pthread_cond_t *cond)
{
	int error;

	/* Initialize mutex attributes */
	if ((error = pthread_cond_init(cond, NULL)) != 0) {
		_JC_EX_STORE(env, InternalError,
		    "%s: %s", "pthread_cond_init", strerror(error));
		return JNI_ERR;
	}

	/* Done */
	return JNI_OK;
}

/*
 * Destroy a condition variable.
 */
void
_jc_cond_destroy(pthread_cond_t *cond)
{
	int r;

	r = pthread_cond_destroy(cond);
	_JC_ASSERT(r == 0);
}

