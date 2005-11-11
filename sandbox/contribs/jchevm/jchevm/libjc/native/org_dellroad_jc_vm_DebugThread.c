
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
 * $Id: org_dellroad_jc_vm_DebugThread.c,v 1.8 2005/11/09 18:14:22 archiecobbs Exp $
 */

#include "libjc.h"
#include "org_dellroad_jc_vm_DebugThread.h"

/* Internal functions */
static void	_jc_dump_thread(_jc_env *env, _jc_env *thread);
static void	_jc_dump_loader(_jc_env *env, _jc_class_loader *loader);
static void	_jc_thread_headline(_jc_env *env, _jc_env *thread);
static void	_jc_loader_headline(_jc_env *env, _jc_class_loader *loader);

/*
 * static native void dumpDebugInfo()
 */
void _JC_JCNI_ATTR
JCNI_org_dellroad_jc_vm_DebugThread_dumpDebugInfo(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *loader;
	_jc_env *thread;

	/* Stop the world */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_stop_the_world(env);
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Update my status */
	snprintf(env->text_status, sizeof(env->text_status),
	    "creating this debug printout");

	/* Dump threads */
	_jc_eprintf(vm, "\n[ Live threads ]\n\n");
	LIST_FOREACH(thread, &vm->threads.alive_list, link)
		_jc_dump_thread(env, thread);

	/* Dump class loaders */
	_jc_eprintf(vm, "\n[ Live class loaders ]\n\n");
	LIST_FOREACH(loader, &vm->class_loaders, link)
		_jc_dump_loader(env, loader);
	_jc_eprintf(vm, "\n");

	/* Resume the world */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_resume_the_world(env);
	_JC_MUTEX_UNLOCK(env, vm->mutex);
}

/*
 * Print out info about one thread.
 */
static void
_jc_dump_thread(_jc_env *env, _jc_env *thread)
{
	_jc_jvm *const vm = env->vm;
	_jc_saved_frame *frames = NULL;
	int num_frames;

	/* Print headline and status */
	_jc_thread_headline(env, thread);
	_jc_eprintf(vm, "    Status: %s\n", thread->text_status);

	/* Grab thread's stack trace */
	num_frames = _jc_save_stack_frames(env, thread, 0, NULL);
	if ((frames = _JC_STACK_ALLOC(env,
	    num_frames * sizeof(*frames))) == NULL)
		num_frames = 0;
	_jc_save_stack_frames(env, thread, num_frames, frames);

	/* Print stack trace */
	if (num_frames > 0) {
		_jc_eprintf(vm, "    Java stack trace:\n");
		_jc_print_stack_frames(env, stderr, num_frames, frames);
	}

	/* Print threads blocked waiting for me to release an object lock */
	if (!SLIST_EMPTY(&thread->lock.owner.waiters)) {
		_jc_env *waiter;

		/* Lock waiter list */
		_JC_MUTEX_LOCK(env, thread->lock.owner.mutex);

		_jc_eprintf(vm, "    Other threads waiting for me:\n");
		SLIST_FOREACH(waiter, &thread->lock.owner.waiters,
		    lock.waiter.link) {
			_jc_object *const obj = waiter->lock.waiter.object;
			_jc_word lockword;

			_jc_eprintf(vm, "\t");
			_jc_thread_headline(env, waiter);
			_jc_eprintf(vm, "\t  %s@%p\n", obj->type->name, obj);
			lockword = obj->lockword;
			_jc_eprintf(vm, "\t  lockword=0x%08x ", lockword);
			if (_JC_LW_TEST(lockword, FAT)) {
				const int lock_id
				    = _JC_LW_EXTRACT(lockword, FAT_ID);
				_jc_fat_lock *const lock
				    = vm->fat_locks.by_id[lock_id];

				_jc_eprintf(vm, "fat lock id=%d count=%d\n",
				    lock_id, lock->recursion_count);
			} else {
				_JC_ASSERT(_JC_LW_EXTRACT(lockword,
				    THIN_TID) == thread->thread_id);
				_jc_eprintf(vm, "thin lock count=%d\n",
				    _JC_LW_EXTRACT(lockword, THIN_COUNT) + 1);
			}
		}

		/* Unlock waiter list */
		_JC_MUTEX_UNLOCK(env, thread->lock.owner.mutex);
	}
}

/*
 * Print thread headline
 */
static void
_jc_thread_headline(_jc_env *env, _jc_env *thread)
{
	_jc_jvm *const vm = env->vm;

	_jc_eprintf(vm, "Thread %p [%d]: ", thread, thread->thread_id);
	if (thread->instance != NULL) {
		_jc_object *nameString;
		char namebuf[128];

		nameString = *_JC_VMFIELD(vm, thread->instance,
		    Thread, name, _jc_object *);
		if (nameString != NULL) {
			char *name;
			size_t len;

			/* Convert name to UTF-8 */
			len = _jc_decode_string_utf8(env, nameString, NULL);
			if ((name = _JC_STACK_ALLOC(env, len + 1)) == NULL) {
				snprintf(namebuf, sizeof(namebuf),
				    "[error decoding name: %s%s%s]",
				    _jc_vmex_names[env->ex.num],
				    env->ex.msg != '\0' ? ": " : "",
				    env->ex.msg);
			} else {
				_jc_decode_string_utf8(env, nameString, name);
				snprintf(namebuf, sizeof(namebuf),
				    "\"%s\"", name);
			}
		} else
			snprintf(namebuf, sizeof(namebuf), "[no name]");
		_jc_eprintf(vm, "%s (%s@%p)\n", namebuf,
		    thread->instance->type->name, thread->instance);
		return;
	} else
		_jc_eprintf(vm, "[thread with no Thread instance]");
}

/*
 * Print info about a class loader
 */
static void
_jc_dump_loader(_jc_env *env, _jc_class_loader *loader)
{
	_jc_jvm *const vm = env->vm;
	_jc_uni_page_list *const mem = &loader->uni.pages;
	_jc_class_loader *parent = NULL;
	_jc_uni_pages *pages;
	int unused_bytes;
	int num_pages;

	/* Print loader headline */
	_jc_eprintf(vm, "Loader %p: ", loader);
	_jc_loader_headline(env, loader);

	/* Display number of loaded classes */
	_jc_eprintf(vm, "%32s: %d\n",
	    "Number of loaded classes", loader->defined_types.size);

	/* Display number of pages of memory used */
	num_pages = 0;
	unused_bytes = 0;
	TAILQ_FOREACH(pages, mem, link) {
		num_pages += pages->num_pages;
		unused_bytes += (pages->num_pages * _JC_PAGE_SIZE)
		    - pages->offset;
	}
	_jc_eprintf(vm, "%32s: %d pages (%dK), %dK (%d%%) of that unused\n",
	    "Total memory allocated", num_pages,
	    (num_pages * _JC_PAGE_SIZE) / 1024, (unused_bytes + 513) / 1024,
	    (unused_bytes * 100) / (num_pages * _JC_PAGE_SIZE));

	/* Display parent class loader */
	_jc_eprintf(vm, "%32s: ", "Parent class loader");
	if (loader->instance != NULL) {
		_jc_object *pinstance;

		pinstance = *_JC_VMFIELD(vm, loader->instance,
		    ClassLoader, parent, _jc_object *);
		if (pinstance != NULL) {
			parent = _jc_get_vm_pointer(vm,
			    pinstance, vm->boot.fields.ClassLoader.vmdata);
		} else
			parent = vm->boot.loader;
	}
	_jc_loader_headline(env, parent);
}

/*
 * Print class loader headline
 */
static void
_jc_loader_headline(_jc_env *env, _jc_class_loader *loader)
{
	_jc_jvm *const vm = env->vm;

	if (loader == NULL)
		_jc_eprintf(vm, "None\n");
	else if (loader->instance == NULL)
		_jc_eprintf(vm, "Bootstrap loader\n");
	else {
		_jc_object *const instance = loader->instance;

		_jc_eprintf(vm, "%s@%p\n", instance->type->name, instance);
	}
}

