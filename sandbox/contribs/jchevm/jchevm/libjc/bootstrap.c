
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

/*
 * Macros for code simplification.
 */

#define BOOTSTRAP_TYPE(name, field)					\
    do {								\
	if ((vm->boot.types.field = _jc_load_type(env,			\
	    vm->boot.loader, name)) == NULL)				\
		goto fail;						\
    } while (0)

#define RESOLVE_METHOD1(class, cname, jname, signature, static)		\
    do {								\
	if ((vm->boot.methods.class.cname				\
	    = _jc_get_declared_method(env, vm->boot.types.class,	\
	      jname, signature, _JC_ACC_STATIC, static)) == NULL) {	\
		_jc_post_exception_info(env);				\
		goto fail;						\
	}								\
    } while (0)

#define RESOLVE_METHOD(class, name, signature, static)			\
	RESOLVE_METHOD1(class, name, #name, signature, static)

#define RESOLVE_CONSTRUCTOR(class, signature)				\
	RESOLVE_METHOD1(class, init, "<init>", signature, 0)

#define RESOLVE_FIELD(class, fname, signature, is_static)		\
    do {								\
	if ((vm->boot.fields.class.fname				\
	    = _jc_get_declared_field(env, vm->boot.types.class,		\
	      #fname, signature, is_static)) == NULL) {			\
		_jc_post_exception_info(env);				\
		goto fail;						\
	}								\
    } while (0)

/*
 * Bootstrap Java classes
 */
jint
_jc_bootstrap_classes(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	jboolean long_ptr = JNI_FALSE;
	_jc_type **types;
	int num_types;
	int i;

	/* Get pointer size */
	switch (sizeof(void *)) {
	case 4:
		long_ptr = JNI_FALSE;
		break;
	case 8:
		long_ptr = JNI_TRUE;
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
		break;
	}

	/*
	 * Load some special types and locate various fields,
	 * methods, and constructors. We are loading types only,
	 * which simply means creating the _jc_type structures.
	 *
	 * Since java.lang.Class is not loaded until later, during
	 * this initial loading we defer creating Class instances.
	 * We also disable class initialization until later.
	 */
	vm->initialization->may_execute = JNI_FALSE;

	/* Load primitive types */
	for (i = _JC_TYPE_BOOLEAN; i <= _JC_TYPE_VOID; i++) {
		if ((vm->boot.types.prim[i]
		    = _jc_load_primitive_type(env, i)) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}
	}

	/* Load types required for creating arrays */
	BOOTSTRAP_TYPE("java/lang/Object", Object);
	BOOTSTRAP_TYPE("java/lang/Cloneable", Cloneable);
	BOOTSTRAP_TYPE("java/io/Serializable", Serializable);

	/* Initialize array type info */
	if (_jc_setup_array_types(env) != JNI_OK) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Load primitive array types */
	for (i = _JC_TYPE_BOOLEAN; i < _JC_TYPE_VOID; i++) {
		char jname[] = { '[', _jc_prim_chars[i], '\0' };

		BOOTSTRAP_TYPE(jname, prim_array[i]);
	}

	/* Load some classes related to exceptions */
	BOOTSTRAP_TYPE("java/lang/String", String);
	BOOTSTRAP_TYPE("java/lang/Throwable", Throwable);
	BOOTSTRAP_TYPE("java/lang/VMThrowable", VMThrowable);
	RESOLVE_FIELD(Throwable, cause, "Ljava/lang/Throwable;", 0);
	RESOLVE_FIELD(Throwable, detailMessage, "Ljava/lang/String;", 0);
	RESOLVE_FIELD(Throwable, vmState, "Ljava/lang/VMThrowable;", 0);
	RESOLVE_FIELD(VMThrowable, vmdata, "Ljava/lang/Object;", 0);
	for (i = 0; i < _JC_VMEXCEPTION_MAX; i++)
		BOOTSTRAP_TYPE(_jc_vmex_names[i], vmex[i]);

	/* Load more special classes */
	BOOTSTRAP_TYPE(long_ptr ?
	    "gnu/classpath/Pointer64" : "gnu/classpath/Pointer32", Pointer);
	BOOTSTRAP_TYPE("gnu/classpath/VMStackWalker", VMStackWalker);
	BOOTSTRAP_TYPE("java/lang/Error", Error);
	BOOTSTRAP_TYPE("java/lang/ClassLoader", ClassLoader);
	BOOTSTRAP_TYPE("java/lang/StackTraceElement", StackTraceElement);
	BOOTSTRAP_TYPE("java/lang/System", System);
	BOOTSTRAP_TYPE("java/lang/Thread", Thread);
	BOOTSTRAP_TYPE("java/lang/ThreadGroup", ThreadGroup);
	BOOTSTRAP_TYPE("java/lang/VMThread", VMThread);
	BOOTSTRAP_TYPE("java/lang/ref/Reference", Reference);
	BOOTSTRAP_TYPE("java/lang/ref/SoftReference", SoftReference);
	BOOTSTRAP_TYPE("java/lang/ref/WeakReference", WeakReference);
	BOOTSTRAP_TYPE("java/lang/ref/PhantomReference", PhantomReference);
	BOOTSTRAP_TYPE("java/lang/reflect/AccessibleObject", AccessibleObject);
	BOOTSTRAP_TYPE("java/lang/reflect/Constructor", Constructor);
	BOOTSTRAP_TYPE("java/lang/reflect/Field", Field);
	BOOTSTRAP_TYPE("java/lang/reflect/Method", Method);
	BOOTSTRAP_TYPE("java/nio/Buffer", Buffer);
	BOOTSTRAP_TYPE("java/nio/DirectByteBufferImpl$ReadWrite", ReadWrite);
	BOOTSTRAP_TYPE("[Ljava/lang/Class;", Class_array);
	BOOTSTRAP_TYPE("[Ljava/lang/StackTraceElement;",
	    StackTraceElement_array);
	BOOTSTRAP_TYPE("[Ljava/lang/reflect/Constructor;", Constructor_array);
	BOOTSTRAP_TYPE("[Ljava/lang/reflect/Field;", Field_array);
	BOOTSTRAP_TYPE("[Ljava/lang/reflect/Method;", Method_array);
	for (i = _JC_TYPE_BOOLEAN; i <= _JC_TYPE_VOID; i++)
		BOOTSTRAP_TYPE(_jc_prim_wrapper_class[i], prim_wrapper[i]);

	/* Find special constructors */
	RESOLVE_CONSTRUCTOR(StackTraceElement,
	    "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Z)V");
	RESOLVE_CONSTRUCTOR(Constructor, "(Ljava/lang/Class;I)V");
	RESOLVE_CONSTRUCTOR(ReadWrite,
	    "(Ljava/lang/Object;Lgnu/classpath/Pointer;III)V");
	RESOLVE_CONSTRUCTOR(Field, "(Ljava/lang/Class;Ljava/lang/String;I)V");
	RESOLVE_CONSTRUCTOR(Method, "(Ljava/lang/Class;Ljava/lang/String;I)V");
	RESOLVE_CONSTRUCTOR(String, "([C)V");
	RESOLVE_CONSTRUCTOR(Thread,
	    "(Ljava/lang/VMThread;Ljava/lang/String;IZ)V");
	RESOLVE_CONSTRUCTOR(ThreadGroup,
	    "(Ljava/lang/ThreadGroup;Ljava/lang/String;)V");
	RESOLVE_CONSTRUCTOR(VMThread, "(Ljava/lang/Thread;)V");
	for (i = _JC_TYPE_BOOLEAN; i <= _JC_TYPE_DOUBLE; i++) {
		char signature[] = { '(', _jc_prim_chars[i], ')', 'V', '\0' };

		RESOLVE_CONSTRUCTOR(prim_wrapper[i], signature);
	}
	for (i = 0; i < _JC_VMEXCEPTION_MAX; i++) {
		switch (i) {
		case _JC_ClassNotFoundException:
			RESOLVE_CONSTRUCTOR(vmex[i],
			    "(Ljava/lang/String;Ljava/lang/Throwable;)V");
			break;
		case _JC_ExceptionInInitializerError:
		case _JC_InvocationTargetException:
			RESOLVE_CONSTRUCTOR(vmex[i],
			    "(Ljava/lang/Throwable;)V");
			break;
		case _JC_ThreadDeath:
			RESOLVE_CONSTRUCTOR(vmex[i], "()V");
			break;
		default:
			RESOLVE_CONSTRUCTOR(vmex[i], "(Ljava/lang/String;)V");
			break;
		}
	}

	/* Find special methods */
	RESOLVE_METHOD(AccessibleObject, isAccessible, "()Z", 0);
	RESOLVE_METHOD(ClassLoader, getSystemClassLoader,
	    "()Ljava/lang/ClassLoader;", _JC_ACC_STATIC);
	RESOLVE_METHOD(ClassLoader, loadClass,
	    "(Ljava/lang/String;)Ljava/lang/Class;", 0);
	RESOLVE_METHOD(Method, invoke,
	    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", 0);
	RESOLVE_METHOD(Object, finalize, "()V", 0);
	RESOLVE_METHOD(Object, notifyAll, "()V", 0);
	RESOLVE_METHOD(Object, toString, "()Ljava/lang/String;", 0);
	RESOLVE_METHOD(Object, wait, "()V", 0);
	RESOLVE_METHOD(Reference, enqueue, "()Z", 0);
	RESOLVE_METHOD(String, intern, "()Ljava/lang/String;", 0);
	RESOLVE_METHOD(Thread, stop, "()V", 0);
	RESOLVE_METHOD(ThreadGroup, addThread, "(Ljava/lang/Thread;)V", 0);
	RESOLVE_METHOD(ThreadGroup, uncaughtException,
	    "(Ljava/lang/Thread;Ljava/lang/Throwable;)V", 0);
	RESOLVE_METHOD(VMThread, run, "()V", 0);
	for (i = _JC_TYPE_BOOLEAN; i <= _JC_TYPE_DOUBLE; i++) {
		char signature[] = { '(', ')', _jc_prim_chars[i], '\0' };
		char mname[32];

		snprintf(mname, sizeof(mname), "%sValue", _jc_prim_names[i]);
		RESOLVE_METHOD1(prim_wrapper[i], value, mname, signature, 0);
	}

	/* Find special fields */
	RESOLVE_FIELD(Buffer, address, "Lgnu/classpath/Pointer;", 0);
	RESOLVE_FIELD(Buffer, cap, "I", 0);
	RESOLVE_FIELD(ClassLoader, parent, "Ljava/lang/ClassLoader;", 0);
	RESOLVE_FIELD(ClassLoader, vmdata, "Ljava/lang/Object;", 0);
	RESOLVE_FIELD(Constructor, clazz, "Ljava/lang/Class;", 0);
	RESOLVE_FIELD(Constructor, slot, "I", 0);
	RESOLVE_FIELD(Field, declaringClass, "Ljava/lang/Class;", 0);
	RESOLVE_FIELD(Field, slot, "I", 0);
	RESOLVE_FIELD(Method, declaringClass, "Ljava/lang/Class;", 0);
	RESOLVE_FIELD(Method, slot, "I", 0);
	RESOLVE_FIELD(Pointer, data, long_ptr ? "J" : "I", 0);
	RESOLVE_FIELD(Reference, referent, "Ljava/lang/Object;", 0);
	RESOLVE_FIELD(Reference, queue, "Ljava/lang/ref/ReferenceQueue;", 0);
	RESOLVE_FIELD(String, value, "[C", 0);
	RESOLVE_FIELD(String, offset, "I", 0);
	RESOLVE_FIELD(String, count, "I", 0);
	RESOLVE_FIELD(System, in, "Ljava/io/InputStream;", 1);
	RESOLVE_FIELD(System, out, "Ljava/io/PrintStream;", 1);
	RESOLVE_FIELD(System, err, "Ljava/io/PrintStream;", 1);
	RESOLVE_FIELD(Thread, daemon, "Z", 0);
	RESOLVE_FIELD(Thread, group, "Ljava/lang/ThreadGroup;", 0);
	RESOLVE_FIELD(Thread, name, "Ljava/lang/String;", 0);
	RESOLVE_FIELD(Thread, priority, "I", 0);
	RESOLVE_FIELD(Thread, vmThread, "Ljava/lang/VMThread;", 0);
	RESOLVE_FIELD(ThreadGroup, root, "Ljava/lang/ThreadGroup;", 1);
	RESOLVE_FIELD(VMThread, thread, "Ljava/lang/Thread;", 0);
	RESOLVE_FIELD(VMThread, vmdata, "Ljava/lang/Object;", 0);

	/* Load java.lang.Class */
	BOOTSTRAP_TYPE("java/lang/Class", Class);
	RESOLVE_FIELD(Class, vmdata, "Ljava/lang/Object;", 0);
	RESOLVE_FIELD(Class, pd, "Ljava/security/ProtectionDomain;", 0);

	/* Set initial lockwords for Object, Class, and Pointer */
	_jc_initialize_lockword(env, vm->boot.types.Object, NULL);
	_jc_initialize_lockword(env, vm->boot.types.Class,
	    vm->boot.types.Object);
	_jc_initialize_lockword(env, vm->boot.types.Pointer,
	    vm->boot.types.Object);

	/*
	 * We're now able to create "java/lang/Class" instances.
	 * Belatedly create them for all of the types we just loaded.
	 * Class initialization is disabled though so no Java code runs.
	 * Note: we never invoke constructors for Class instances.
	 */
	vm->initialization->create_class = JNI_TRUE;
	num_types = vm->boot.loader->defined_types.size;
	if ((types = _JC_STACK_ALLOC(env,
	    num_types * sizeof(*types))) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}
	_jc_splay_list(&vm->boot.loader->defined_types, (void **)types);
	_JC_MUTEX_LOCK(env, vm->boot.loader->mutex);
	for (i = 0; i < num_types; i++) {
		if (_jc_create_class_instance(env, types[i]) != JNI_OK) {
			_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);
			_jc_post_exception_info(env);
			goto fail;
		}
	}
	for (i = _JC_TYPE_BOOLEAN; i <= _JC_TYPE_VOID; i++) {
		if (_jc_create_class_instance(env,
		    vm->boot.types.prim[i]) != JNI_OK) {
			_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);
			_jc_post_exception_info(env);
			goto fail;
		}
	}
	_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);

	/*
	 * Now resolve all loaded types. This will cause a bunch of
	 * other classes to be loaded, and their Class instances will
	 * be created normally, but still no class initialization will
	 * take place so no Java code runs yet.
	 */
	if (_jc_resolve_type(env, vm->boot.types.Object) != JNI_OK)
		goto fail;
	if (_jc_resolve_type(env, vm->boot.types.Class) != JNI_OK)
		goto fail;
	for (i = 0; i < num_types; i++) {
		if (_jc_resolve_type(env, types[i]) != JNI_OK)
			goto fail;
	}

	/*
	 * Initialize java.lang.Class (and therefore java.lang.Object).
	 * This causes bunch of Java code to run for the first time,
	 * due to static initializers in Class and Object.
	 */
	vm->initialization->may_execute = JNI_TRUE;
	if (_jc_initialize_type(env, vm->boot.types.Class) != JNI_OK)
		goto fail;

	/* Create a "fallback" instance for each exception class */
	for (i = 0; i < _JC_VMEXCEPTION_MAX; i++) {
		_jc_method *cons;
		jobject ref;

		/* Get no-arg constructor */
		if ((cons = _jc_get_declared_method(env, vm->boot.types.vmex[i],
		    "<init>", "()V", _JC_ACC_STATIC, 0)) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}

		/* Create object */
		if ((vm->boot.objects.vmex[i] = _jc_new_object(env,
		    vm->boot.types.vmex[i])) == NULL)
			goto fail;

		/* Wrap it in a global native reference */
		if ((ref = _jc_new_global_native_ref(env,
		    vm->boot.objects.vmex[i])) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}

		/* Invoke the no-arg constructor */
		if (_jc_invoke_nonvirtual(env, cons, *ref) != JNI_OK) {
			_jc_free_global_native_ref(&ref);
			goto fail;
		}
	}

	/* Done */
	return JNI_OK;

fail:
	/* Handle failure case */
	return JNI_ERR;
}

