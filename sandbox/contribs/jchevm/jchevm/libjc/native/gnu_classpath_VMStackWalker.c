
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
 * $Id: gnu_classpath_VMStackWalker.c,v 1.4 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "gnu_classpath_VMStackWalker.h"

/* Internal functions */
static jboolean	_jc_poppable_method(_jc_jvm *vm, _jc_method *method);

static jboolean
_jc_poppable_method(_jc_jvm *vm, _jc_method *method)
{
	/* Check for VMStackWalker methods */
	if (method->class == vm->boot.types.VMStackWalker)
		return JNI_TRUE;

	/* Check for Method.invoke() */
	if (method == vm->boot.methods.Method.invoke)
		return JNI_TRUE;

	/* Not poppable */
	return JNI_FALSE;
}

/*
 * public static final native Class getCallingClass()
 */
_jc_object * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getCallingClass(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *result = NULL;
	_jc_stack_crawl crawl;
	jboolean top;

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Crawl up the stack */
	for (top = JNI_TRUE, _jc_stack_crawl_first(env, &crawl);
	    crawl.method != NULL; _jc_stack_crawl_next(vm, &crawl)) {

		/* Ignore non-Java stack frames */
		if (crawl.method->class == NULL)
			continue;

		/* Ignore internal methods on top of stack */
		if (top && _jc_poppable_method(vm, crawl.method))
			continue;
		top = JNI_FALSE;

		/* Pop one additional frame */
		do
		    _jc_stack_crawl_next(vm, &crawl);
		while (crawl.method != NULL && crawl.method->class == NULL);

		/* Done */
		if (crawl.method != NULL && crawl.method->class != NULL)
			result = crawl.method->class->instance;
		break;
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return result;
}

/*
 * public static final native ClassLoader getCallingClassLoader()
 */
_jc_object * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getCallingClassLoader(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *result = NULL;
	_jc_stack_crawl crawl;
	jboolean top;

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Crawl up the stack */
	for (top = JNI_TRUE, _jc_stack_crawl_first(env, &crawl);
	    crawl.method != NULL; _jc_stack_crawl_next(vm, &crawl)) {

		/* Ignore non-Java stack frames */
		if (crawl.method->class == NULL)
			continue;

		/* Ignore internal methods on top of stack */
		if (top && _jc_poppable_method(vm, crawl.method))
			continue;
		top = JNI_FALSE;

		/* Pop one additional frame */
		do
		    _jc_stack_crawl_next(vm, &crawl);
		while (crawl.method != NULL && crawl.method->class == NULL);

		/* Done */
		if (crawl.method != NULL && crawl.method->class != NULL)
			result = crawl.method->class->loader->instance;
		break;
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return result;
}

/*
 * public static final native Class[] getClassContext()
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getClassContext(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array = NULL;
	_jc_stack_crawl crawl;
	jboolean top;
	int num;

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

again:
	/* Crawl the Java stack and add Class objects for each method */
	for (num = 0, top = JNI_TRUE, _jc_stack_crawl_first(env, &crawl);
	    crawl.method != NULL; _jc_stack_crawl_next(vm, &crawl)) {

		/* Ignore non-Java stack frames */
		if (crawl.method->class == NULL)
			continue;

		/* Ignore internal methods on top of stack */
		if (top && _jc_poppable_method(vm, crawl.method))
			continue;
		top = JNI_FALSE;

		/* Add method's class */
		if (array != NULL)
			array->elems[~num] = crawl.method->class->instance;
		num++;
	}

	/* Create array and loop back */
	if (array == NULL) {
		if ((array = (_jc_object_array *)_jc_new_array(env,
		    vm->boot.types.Class_array, num)) == NULL) {
			_JC_MUTEX_UNLOCK(env, vm->mutex);
			_jc_throw_exception(env);
		}
		goto again;
	}
	_JC_ASSERT(num == array->length);

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return array;
}

