
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
 * $Id: class_file.c,v 1.3 2005/03/18 04:17:48 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * The VM class file table contains a node for each class file we've
 * seen. Each node has a reference count. Every loaded class counts
 * and each type dependency counts for one reference.
 */

/*
 * Search for a class file node in the VM class file tree. If not found,
 * actively retrieve it by trying to load it with the supplied class loader.
 *
 * Because user class loaders control finding and loading their classes,
 * the only way for us to force the acquisition of a classfile is to try to
 * load the type. In general this may cause ClassCircularityErrors, but we
 * don't care about them because by the time one is thrown, the class file
 * has already been seen by us and its node stored in the class file tree.
 *
 * If successful, the node is returned with an extra reference.
 *
 * This function is used only if vm->without_classfiles is false.
 */
_jc_class_node *
_jc_get_class_node(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_save class_save;
	_jc_class_node *node;
	_jc_class_node key;
	_jc_type *type;

	/* Sanity check */
	_JC_ASSERT(name[0] != '[');
	_JC_ASSERT(!vm->without_classfiles);

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Search for existing class file node */
	key.name = name;
	if ((node = _jc_splay_find(&vm->classfiles, &key)) != NULL
	    && node->bytes != NULL) {
		node->refs++;
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		return node;
	}
	node = NULL;

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* If loader is the boot loader, we can get the class file directly */
	if (loader == vm->boot.loader) {
		_jc_classbytes *cbytes;

		/* Find the class file bytes in the filesystem */
		if ((cbytes = _jc_bootcl_find_classbytes(env,
		    name, NULL)) == NULL) {
			_jc_post_exception_info(env);
			goto done;
		}

		/* Try to add class file to class file tree */
		_JC_MUTEX_LOCK(env, vm->mutex);
		node = _jc_ref_class_node(env, name, cbytes->hash, cbytes);
		_JC_MUTEX_UNLOCK(env, vm->mutex);

		/* Free class file bytes */
		_jc_free_classbytes(&cbytes);

		/* Post stored exception if failed */
		if (node == NULL)
			_jc_post_exception_info(env);

		/* Done */
		goto done;
	}

	/* Initialize our class file save structure */
	class_save.name = name;
	class_save.bytes = NULL;
	class_save.next = env->class_save;
	env->class_save = &class_save;

	/*
	 * Attempt to load the type in order to acquire the class file.
	 * If we succeeded, then the class file node must have been added.
	 */
	if ((type = _jc_load_type(env, loader, name)) != NULL) {

		/* Lock VM */
		_JC_MUTEX_LOCK(env, vm->mutex);

		/* Find node and add reference */
		node = _jc_splay_find(&vm->classfiles, &key);
		_JC_ASSERT(node != NULL);
		node->refs++;

		/* Unlock VM */
		_JC_MUTEX_UNLOCK(env, vm->mutex);

		/* Done */
		goto done;
	}

	/* Ignore LinkageError's but bail out if any other exception */
	if (!_jc_unpost_exception(env, _JC_LinkageError))
		goto done;

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/*
	 * Look for class file in our class file save structure; if found,
	 * copy its info to VM tree and grab a reference to the new node.
	 */
	if (class_save.bytes != NULL) {

		/* Add node */
		node = _jc_ref_class_node(env, name,
		    class_save.bytes->hash, class_save.bytes);
		_JC_MUTEX_UNLOCK(env, vm->mutex);

		/* Post exception if failed */
		if (node == NULL)
			_jc_post_exception_info(env);

		/* Done */
		goto done;
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

done:
	/* Destroy class save structure if we added one */
	if (env->class_save == &class_save) {
		_jc_free_classbytes(&class_save.bytes);
		env->class_save = class_save.next;
	}

	/* Done */
	return node;
}

/*
 * Find/create a class file node in the VM's class file table.
 *
 * If the class already exists, the hash must be the same, otherwise
 * a LinkageError is stored. Bump the reference count on the existing node.
 *
 * If no class by this name exists, add a new node with reference count 1.
 *
 * 'cbytes' should be NULL unless object generation is enabled and the
 * classfile was loaded by a user-defined class loader. If successful,
 * it will be copied to any newly created node.
 *
 * Stores an exception if unsuccessful.
 *
 * NOTE: This assumes the VM global mutex is locked.
 */
_jc_class_node *
_jc_ref_class_node(_jc_env *env, const char *name,
	jlong hash, _jc_classbytes *cbytes)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_node *node;
	_jc_class_node key;
	size_t nlen;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);
	_JC_ASSERT(cbytes == NULL || cbytes->hash == hash);

	/* If code generation is disabled, don't save the class file bytes */
	if (!vm->generation_enabled)
		cbytes = NULL;

	/* Search for existing node */
	key.name = name;
	if ((node = _jc_splay_find(&vm->classfiles, &key)) != NULL) {

		/* Hash values must be the same */
		if (hash != node->hash) {
			_JC_EX_STORE(env, LinkageError, "class file for `%s'"
			    " has an unexpected hash value 0x%" _JC_JLONG_FMT
			    " != 0x%" _JC_JLONG_FMT, name, hash, node->hash);
			return NULL;
		}

		/* Save class file if we don't have it yet */
		if (cbytes != NULL && node->bytes == NULL)
			node->bytes = _jc_dup_classbytes(cbytes);

		/* Increment node reference count */
		node->refs++;

		/* Return node */
		return node;
	}

	/* Create a new class file node */
	nlen = strlen(name);
	if ((node = _jc_vm_alloc(env, sizeof(*node) + nlen + 1)) == NULL)
		return NULL;
	memcpy(node + 1, name, nlen + 1);
	node->name = (char *)(node + 1);
	node->hash = hash;
	node->refs = 1;
	node->bytes = cbytes != NULL ? _jc_dup_classbytes(cbytes) : NULL;

	/* Add node to our classfile tree */
#ifndef NDEBUG
	memset(&node->node, 0, sizeof(node->node));
#endif
	_jc_splay_insert(&vm->classfiles, node);

	/* Done */
	return node;
}

/*
 * Unreference several class file nodes from a dependency list.
 *
 * NOTE: This assumes the VM global mutex is locked.
 */
void
_jc_unref_class_deps(_jc_jvm *vm, _jc_class_depend *deps, int num_deps)
{
	int i;

	/* Sanity check */
	_JC_MUTEX_ASSERT(_jc_get_current_env(), vm->mutex);

	/* Iterate through list */
	for (i = 0; i < num_deps; i++) {
		_jc_class_depend *const dep = &deps[i];
		_jc_class_node *node;
		_jc_class_node key;

		/* Find node; it must be there */
		key.name = dep->name;
		node = _jc_splay_find(&vm->classfiles, &key);
		_JC_ASSERT(node != NULL);
		_JC_ASSERT(node->refs > 0);

		/* Decrement reference count */
		if (--node->refs == 0) {
			_jc_splay_remove(&vm->classfiles, node);
			_jc_free_classbytes(&node->bytes);
			_jc_vm_free(&node);
		}
	}
}

/*
 * Unreference a class file node.
 *
 * NOTE: This assumes the VM global mutex is locked.
 */
void
_jc_unref_class_node(_jc_jvm *vm, _jc_class_node **nodep)
{
	_jc_class_node *node = *nodep;

	/* Sanity check */
	_JC_MUTEX_ASSERT(_jc_get_current_env(), vm->mutex);
	if (node == NULL)
		return;
	*nodep = NULL;

	/* Decrement reference count */
	_JC_ASSERT(node->refs > 0);
	if (--node->refs == 0) {
		_jc_splay_remove(&vm->classfiles, node);
		_jc_free_classbytes(&node->bytes);
		_jc_vm_free(&node);
	}
}

