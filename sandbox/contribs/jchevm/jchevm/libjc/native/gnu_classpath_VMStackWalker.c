
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
 * $Id$
 */

#include "libjc.h"
#include "gnu_classpath_VMStackWalker.h"
#include "java_lang_VMClass.h"

/* Internal functions */
static jboolean	_jc_poppable_method(_jc_jvm *vm, _jc_method *method);

static jboolean
_jc_poppable_method(_jc_jvm *vm, _jc_method *method)
{
	/* Check for VMStackWalker methods */
	if (method->class == vm->boot.types.VMStackWalker
	    && strcmp(method->name, "getClassContext") == 0)
		return JNI_TRUE;

	/* Check for Method.invoke() */
	if (method == vm->boot.methods.Method.invoke)
		return JNI_TRUE;

	/* Not poppable */
	return JNI_FALSE;
}

#if 0
/*
 * public static final native Class getCallingClass()
 *
 * NOTE: this implementation is not used with "stock" Classpath.
 */
_jc_object * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getCallingClass(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *result = NULL;
	_jc_java_stack *jstack;
	jboolean top;

	/* Crawl up the stack */
	for (top = JNI_TRUE, jstack = env->java_stack;
	    jstack != NULL; jstack = jstack->next) {

		/* Ignore internal methods on top of stack */
		if (top && _jc_poppable_method(vm, jstack->method))
			continue;
		top = JNI_FALSE;

		/* Pop one additional frame */
		jstack = jstack->next;

		/* Done */
		result = jstack->method->class->instance;
		break;
	}

	/* Done */
	return result;
}

/*
 * public static final native ClassLoader getCallingClassLoader()
 *
 * NOTE: this implementation is not used with "stock" Classpath.
 */
_jc_object * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getCallingClassLoader(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *result = NULL;
	_jc_java_stack *jstack;
	jboolean top;

	/* Crawl up the stack */
	for (top = JNI_TRUE, jstack = env->java_stack;
	    jstack != NULL; jstack = jstack->next) {

		/* Ignore internal methods on top of stack */
		if (top && _jc_poppable_method(vm, jstack->method))
			continue;
		top = JNI_FALSE;

		/* Pop one additional frame */
		jstack = jstack->next;

		/* Done */
		result = jstack->method->class->loader->instance;
		break;
	}

	/* Done */
	return result;
}
#endif

/*
 * public static final native Class[] getClassContext()
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getClassContext(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array = NULL;
	_jc_java_stack *jstack;
	jboolean top;
	int num;

again:
	/* Crawl up the stack */
	for (num = 0, top = JNI_TRUE, jstack = env->java_stack;
	    jstack != NULL; jstack = jstack->next) {

		/* Ignore internal methods on top of stack */
		if (top && _jc_poppable_method(vm, jstack->method))
			continue;
		top = JNI_FALSE;

		/* Add method's class */
		if (array != NULL)
			array->elems[~num] = jstack->method->class->instance;
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

	/* Done */
	return array;
}

_jc_object * _JC_JCNI_ATTR
JCNI_gnu_classpath_VMStackWalker_getClassLoader(_jc_env *env, _jc_object *clobj)
{
	return JCNI_java_lang_VMClass_getClassLoader(env, clobj);
}

