
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
#include "java_lang_Thread.h"

/* Internal functions */
static int	_jc_parse_vm_options(_jc_env *env, const JavaVMInitArgs *args);

/*
 * Create a new Java VM and attach the currently running thread
 * to it as its first and only thread.
 */
jint
_jc_create_vm(void *args, _jc_jvm **vmp, _jc_env **envp)
{
	_jc_initialization initialization;
	_jc_env temp_env;
	_jc_env *env = &temp_env;
	_jc_jvm temp_vm;
	_jc_jvm *vm = &temp_vm;
	_jc_c_stack cstack;
	jobject sref = NULL;
	int i;

	/* Static checks */
	_JC_ASSERT(_JC_VMEXCEPTION_MAX <= 64);		/* must fit in jlong */
	_JC_ASSERT(_JC_VERBOSE_MAX <= 32);		/* must fit in jint */
	_JC_ASSERT(_JC_FULL_ALIGNMENT <= sizeof(_jc_word));
	_JC_ASSERT(sizeof(void *) <= sizeof(jlong));
	_JC_ASSERT(sizeof(_jc_word) == sizeof(void *));

	/* Initialize initialization structure */
	memset(&initialization, 0, sizeof(initialization));
	initialization.ex.num = -1;

	/* Initialize temporary dummy thread and VM structures */
	memset(&temp_env, 0, sizeof(temp_env));
	memset(&temp_vm, 0, sizeof(temp_vm));
	temp_env.ex.num = -1;
	temp_env.vm = &temp_vm;
	temp_vm.initialization = &initialization;
	temp_vm.vfprintf = vfprintf;
	temp_vm.abort = abort;
	temp_vm.exit = exit;

	/* Allocate vm struct */
	if ((vm = _jc_vm_zalloc(env, sizeof(*vm))) == NULL) {
		vm = &temp_vm;
		goto fail_info;
	}
	env->vm = vm;
	vm->initialization = &initialization;

	/* Initialize the easy stuff */
	vm->jni_interface = &_jc_invoke_interface;
	vm->vfprintf = vfprintf;
	vm->abort = abort;
	vm->exit = exit;
	SLIST_INIT(&vm->native_globals);
	LIST_INIT(&vm->class_loaders);
	LIST_INIT(&vm->threads.alive_list);
	LIST_INIT(&vm->threads.free_list);
	SLIST_INIT(&vm->fat_locks.free_list);

	/* Initialize mutexes and condition variables */
	if (_jc_mutex_init(env, &vm->mutex) != JNI_OK)
		goto pfail1;
	if (_jc_cond_init(env, &vm->all_halted) != JNI_OK)
		goto pfail2;
	if (_jc_cond_init(env, &vm->world_restarted) != JNI_OK) {
		_jc_cond_destroy(&vm->all_halted);
pfail2:		_jc_mutex_destroy(&vm->mutex);
pfail1:		_jc_vm_free(&vm);
		vm = &temp_vm;
		goto fail_info;
	}

	/* Allocate threads.by_id array */
	if ((vm->threads.by_id = _jc_vm_alloc(env,
	    _JC_MAX_THREADS * sizeof(*vm->threads.by_id))) == NULL)
		goto fail_info;

	/* Allocate fat_locks.by_id array */
	if ((vm->fat_locks.by_id = _jc_vm_alloc(env,
	    _JC_MAX_FATLOCKS * sizeof(*vm->fat_locks.by_id))) == NULL)
		goto fail_info;

	/* Initialize list of free thread ID's (we never use ID zero) */
	vm->threads.next_free_id = 1;
	for (i = 1; i < _JC_MAX_THREADS - 1; i++)
		vm->threads.by_id[i] = (_jc_env *)(i + 1);
	vm->threads.by_id[i] = (_jc_env *)0;

	/* Create and attach a new thread structure to the current thread */
	_JC_MUTEX_LOCK(NULL, vm->mutex);
	if ((env = _jc_attach_thread(vm, &temp_env.ex, &cstack)) == NULL) {
		_JC_MUTEX_UNLOCK(NULL, vm->mutex);
		env = &temp_env;
		goto fail_info;
	}
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Initialize default system properties */
	if (_jc_set_system_properties(env) != JNI_OK)
		goto fail_info;

	/* Parse options */
	if (_jc_parse_vm_options(env, args) != JNI_OK)
		goto fail_info;

	/* Digest properties */
	if (_jc_digest_properties(env) != JNI_OK)
		goto fail_info;

	/* Initialize the heap */
	if (_jc_heap_init(env, vm) != JNI_OK)
		goto fail_info;

	/* Get interpreter targets */
	_jc_interp_get_targets(env);

	/* Create the bootstrap class loader */
	_JC_MUTEX_LOCK(env, vm->mutex);
	if ((vm->boot.loader = _jc_create_loader(env)) == NULL) {
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		goto fail_info;
	}
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Initialize boot loader's loading types tree */
	_jc_splay_init(&vm->boot.loading_types,
	    _jc_node_cmp, _JC_OFFSETOF(_jc_type_node, node));

	/* Open myself as a native library */
	if (_jc_load_native_library(env,
	    vm->boot.loader, _JC_INTERNAL_NATIVE_LIBRARY) != JNI_OK)
		goto fail_info;

	/* Load bootstrap Java classes, methods, etc. */
	if (_jc_bootstrap_classes(env) != JNI_OK)
		goto fail;

	/* Get min and max scheduler scheduling priorities */
	if ((vm->threads.prio_min = sched_get_priority_min(SCHED_RR)) == -1) {
		_JC_EX_STORE(env, InternalError,
		    "%s: %s", "sched_get_priority_min", strerror(errno));
		goto fail;
	}
	if ((vm->threads.prio_max = sched_get_priority_max(SCHED_RR)) == -1) {
		_JC_EX_STORE(env, InternalError,
		    "%s: %s", "sched_get_priority_max", strerror(errno));
		goto fail;
	}
	_JC_ASSERT(vm->threads.prio_min <= vm->threads.prio_max);

	/* Get minimum, maximum, and default Java priority values */
	vm->threads.java_prio_min = java_lang_Thread_MIN_PRIORITY;
	vm->threads.java_prio_max = java_lang_Thread_MAX_PRIORITY;
	vm->threads.java_prio_norm = java_lang_Thread_NORM_PRIORITY;

	/* Create system ThreadGroup */
	if ((sref = _jc_new_local_native_ref(env,
	    _jc_new_string(env, _JC_SYSTEM_THREADGROUP_NAME,
	      sizeof(_JC_SYSTEM_THREADGROUP_NAME) - 1))) == NULL)
		goto fail;
	if ((vm->boot.objects.systemThreadGroup = _jc_new_object(env,
	    vm->boot.types.ThreadGroup)) == NULL)
		goto fail;
	if (_jc_invoke_nonvirtual(env, vm->boot.methods.ThreadGroup.init,
	    vm->boot.objects.systemThreadGroup, *_JC_VMSTATICFIELD(vm,
	      ThreadGroup, root, _jc_object *), *sref) != JNI_OK)
		goto fail;

	/* Wrap it in a global native reference */
	if (_jc_new_global_native_ref(env,
	    vm->boot.objects.systemThreadGroup) == NULL)
		goto fail;

	/* Create java.lang.Thread instance for this thread */
	if (_jc_thread_create_instance(env, vm->boot.objects.systemThreadGroup,
	    "main", vm->threads.java_prio_norm, JNI_FALSE) != JNI_OK)
		goto fail;

	/* Start debug thread */
	if ((vm->debug_thread = _jc_internal_thread(env,
	    "org/dellroad/jc/vm/DebugThread")) == NULL)
		goto fail;

	/* Start finalizer thread */
	if ((vm->finalizer_thread = _jc_internal_thread(env,
	    "org/dellroad/jc/vm/FinalizerThread")) == NULL)
		goto fail;

	/* Initialization complete */
	_JC_ASSERT(vm->initialization->ex.num == -1);
	_JC_ASSERT(vm->initialization->frames == NULL);
	vm->initialization = NULL;

	/* Return to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	*vmp = vm;
	*envp = env;
	return JNI_OK;

fail_info:
	_jc_post_exception_info(env);

fail:
	/* Sanity check */
	_JC_ASSERT(vm->initialization != NULL);

	/*
	 * Exception may have been "posted" during bootstrap but
	 * with no actual object created. If so, print out the info
	 * saved in the initialization structure.
	 */
	if (env->pending == NULL && vm->initialization->ex.num != -1) {
		_jc_initialization *const init = vm->initialization;

		/* Print out the exception */
		_jc_eprintf(vm, "jc: exception during initialization\n");
		_jc_eprintf(vm, "%s", _jc_vmex_names[init->ex.num]);
		if (*init->ex.msg != '\0')
			_jc_eprintf(vm, ": %s", init->ex.msg);
		_jc_eprintf(vm, "\n");

		/* Print stack trace (if any) */
		_jc_print_stack_frames(env, stderr,
		    init->num_frames, init->frames);
		goto done;
	}

	/* Otherwise, exception must have been posted normally */
	_JC_ASSERT(env->pending != NULL);
	_jc_print_stack_trace(env, stderr);

done:
	/* Cleanup and return failure */
	_jc_free_local_native_ref(&sref);
	if (env != &temp_env)
		_jc_thread_shutdown(&env);
	if (vm != &temp_vm)
		_jc_free_vm(&vm);
	return JNI_ERR;
}

/*
 * Free a Java VM structure.
 *
 * This assumes there are no threads remaining alive.
 */
void
_jc_free_vm(_jc_jvm **vmp)
{
	_jc_jvm *vm = *vmp;
	int i;

	/* Sanity check */
	if (vm == NULL)
		return;
	*vmp = NULL;

	/* There should be no alive threads! */
	_JC_ASSERT(LIST_EMPTY(&vm->threads.alive_list));

	/* Free system properties */
	_jc_destroy_properties(vm);

	/* Free other stuff */
	if (vm->boot.class_path != NULL) {
		for (i = 0; i < vm->boot.class_path_len; i++) {
			_jc_cpath_entry *const ent = &vm->boot.class_path[i];

			_jc_vm_free(&ent->pathname);
			_jc_zip_close(&ent->zip);
		}
		_jc_vm_free(&vm->boot.class_path);
	}

	/* Free fat locks XXX need to recover fat locks alive in the heap */
	while (!SLIST_EMPTY(&vm->fat_locks.free_list)) {
		_jc_fat_lock *lock = SLIST_FIRST(&vm->fat_locks.free_list);

		SLIST_REMOVE_HEAD(&vm->fat_locks.free_list, u.link);
		_jc_destroy_lock(&lock);
	}
	_jc_vm_free(&vm->fat_locks.by_id);

	/* Destroy threads on the free list */
	_jc_free_thread_stacks(vm);
	while (!LIST_EMPTY(&vm->threads.free_list)) {
		_jc_env *env = LIST_FIRST(&vm->threads.free_list);

		LIST_REMOVE(env, link);
		vm->threads.num_free--;
		_jc_destroy_thread(&env);
	}
	_jc_vm_free(&vm->threads.by_id);

	/* Free class loaders */
	while (!LIST_EMPTY(&vm->class_loaders)) {
		_jc_class_loader *loader = LIST_FIRST(&vm->class_loaders);

		_jc_destroy_loader(vm, &loader);
	}
	_JC_ASSERT(vm->avail_loader_pages == vm->max_loader_pages);

	/* Free native globals */
	_jc_free_all_native_global_refs(vm);

	/* Destroy mutexes and condition vars */
	_jc_mutex_destroy(&vm->mutex);
	_jc_cond_destroy(&vm->all_halted);
	_jc_cond_destroy(&vm->world_restarted);

	/* Free the heap */
	_jc_heap_destroy(vm);

	/* Free VM struct */
	memset(vm, 0, sizeof(*vm));		/* for good measure */
	_jc_vm_free(&vm);
}

/*
 * Parse VM options.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_parse_vm_options(_jc_env *env, const JavaVMInitArgs *args)
{
	_jc_jvm *const vm = env->vm;
	int i;

	/* We only communicate using the 1.2 interface version */
	if (args->version != JNI_VERSION_1_2)
		return JNI_EVERSION;

	/* Parse options */
	for (i = 0; i < args->nOptions; i++) {
		JavaVMOption *const opt = &args->options[i];

		/* Function hooks */
		if (strcmp(opt->optionString, "vfprintf") == 0) {
			vm->vfprintf = opt->extraInfo;
			continue;
		}
		if (strcmp(opt->optionString, "exit") == 0) {
			vm->exit = opt->extraInfo;
			continue;
		}
		if (strcmp(opt->optionString, "abort") == 0) {
			vm->abort = opt->extraInfo;
			continue;
		}

		/* Properties */
		if (strncmp(opt->optionString, "-D", 2) == 0) {
			const char *const pname = opt->optionString + 2;
			const char *eq = strchr(pname, '=');
			const char *pvalue;
			size_t pname_len;

			/* Parse propery name and value */
			if (eq == NULL)
				goto option_fail;
			pname_len = eq - pname;
			pvalue = eq + 1;
			if (pname_len == 0)
				goto option_fail;

			/* Add property to list */
			if (_jc_set_property2(env,
			    pname, pname_len, pvalue) != JNI_OK)
				goto fail;
			continue;
		}

		/* Verbosity */
		if (strncmp(opt->optionString, "-verbose", 8) == 0) {
			const char *s = opt->optionString + 8;

			/* Verbose all standard stuff? */
			if (*s == '\0') {
				vm->verbose_flags |= (1 << _JC_VERBOSE_CLASS);
				vm->verbose_flags |= (1 << _JC_VERBOSE_GC);
				vm->verbose_flags |= (1 << _JC_VERBOSE_JNI);
				continue;
			}

			/* An explicitly specified list? */
			if (*s++ != ':')
				goto option_fail;

			/* Parse the list */
			while (*s != '\0') {
				size_t n = 0;
				int j;

				/* Match verbose options; 'X' is optional */
				for (j = 0; j < _JC_VERBOSE_MAX; j++) {
					const char *const vn
					    = _jc_verbose_names[j];

					n = strlen(vn);
					if ((strncmp(s, vn, n) == 0
					     && (s[n] == ',' || s[n] == '\0'))
					   || (*s == 'X'
					     && strncmp(s + 1, vn, n) == 0
					     && (s[n + 1] == ','
					      || s[n + 1] == '\0')
					     && ++n /* ugh */)) {
						vm->verbose_flags |= (1 << j);
						break;
					}
				}
				if (j == _JC_VERBOSE_MAX) {
					if (*s != 'X'
					    || !args->ignoreUnrecognized)
						goto option_fail;
				}
				s += n;
				if (*s != '\0' && *s++ != ',')
					goto option_fail;
			}
			continue;
		}

		/* Ignore unrecognized but ignorable options */
		if (args->ignoreUnrecognized
		    && (*opt->optionString == '_'
		      || strncmp(opt->optionString, "-X", 2) == 0))
			continue;

option_fail:
		/* Unrecognized option */
		_JC_EX_STORE(env, InternalError,
		    "invalid option `%s'\n", opt->optionString);
		goto fail;
	}

	/* Done */
	return JNI_OK;

fail:
	/* Failed */
	return JNI_ERR;
}

