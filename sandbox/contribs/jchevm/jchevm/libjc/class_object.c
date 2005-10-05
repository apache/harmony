
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
 * $Id: class_object.c,v 1.12 2005/05/18 22:04:45 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static _jc_elf	*_jc_load_object_dir(_jc_env *env, _jc_class_loader *loader,
			const char *dir, const char *name);

/*
 * Find the ELF object that defines the named class, then load the ELF object
 * and all types defined within it. All class file hash values in the ELF
 * object must match any already stored in the VM class file tree.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_load_object(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const key = (_jc_type *)((char *)&name
	    - _JC_OFFSETOF(_jc_type, name));
	int i;

	/* Sanity check */
	_JC_ASSERT(vm->loader_enabled);

	/* Try each entry in the object path */
	for (i = 0; i < vm->object_path_len; i++) {
		_jc_objpath_entry *const ent = &vm->object_path[i];
		_jc_elf *elf;

try_again:
		switch (ent->type) {
		case _JC_OBJPATH_DIRECTORY:

			/* Look for object file in this directory */
			if ((elf = _jc_load_object_dir(env,
			    loader, ent->pathname, name)) != NULL)
				break;

			/* If something unexpected happened, bail out */
			if (env->ex.num != _JC_LinkageError)
				return JNI_ERR;

			/* Try next entry */
			continue;
		case _JC_OBJPATH_ELFFILE:

			/* Has this loader already loaded this object? */
			if (loader->objects_loaded[i])
				continue;

			/* Try to load specified ELF object file */
			if ((elf = _jc_elf_load(env,
			    loader, ent->pathname)) != NULL)
				break;

			/* If something unexpected happened, bail out */
			if (env->ex.num != _JC_LinkageError)
				return JNI_ERR;

			/* Mark this entry as erroneous */
			VERBOSE(OBJ, vm, "`%s' is invalid: %s",
			    ent->pathname, env->ex.msg);
			ent->type = _JC_OBJPATH_ERROR;

			/* Try next entry */
			continue;
		case _JC_OBJPATH_UNKNOWN:
		    {
			struct stat info;

			/* Examine file; if invalid, skip it from now on */
			if (stat(ent->pathname, &info) == -1) {
				VERBOSE(OBJ, vm, "`%s' is invalid: %s",
				    ent->pathname, strerror(errno));
				continue;
			}

			/* If it's a directory, change type and try again */
			if ((info.st_mode & S_IFMT) == S_IFDIR) {
				_JC_MUTEX_LOCK(env, vm->mutex);
				if (ent->type == _JC_OBJPATH_UNKNOWN)
					ent->type = _JC_OBJPATH_DIRECTORY;
				_JC_MUTEX_UNLOCK(env, vm->mutex);
				goto try_again;
			}

			/* Assume it's an ELF file and try again */
			_JC_MUTEX_LOCK(env, vm->mutex);
			if (ent->type == _JC_OBJPATH_UNKNOWN)
				ent->type = _JC_OBJPATH_ELFFILE;
			_JC_MUTEX_UNLOCK(env, vm->mutex);
			goto try_again;
		    }
		case _JC_OBJPATH_ERROR:
			continue;
		default:
			_JC_ASSERT(JNI_FALSE);
		}

		/* We should have a loaded ELF object at this point */
		_JC_ASSERT(elf != NULL);

		/* Does the ELF file define the type we're looking for? */
		if (_jc_splay_find(&elf->types, key) == NULL) {
			VERBOSE(OBJ, vm, "failed: `%s' does not define `%s'",
			    elf->pathname, name);
			_jc_elf_unref(&elf);
			continue;
		}

		/* Load all types defined in the object */
		if (_jc_derive_types_from_object(env, elf) != JNI_OK) {
			if (env->ex.num != _JC_LinkageError)
				return JNI_ERR;
			VERBOSE(OBJ, vm, "failed: %s%s%s",
			    _jc_vmex_names[env->ex.num],
			    *env->ex.msg != '\0' ? ": " : "", env->ex.msg);
			_jc_elf_unref(&elf);
			continue;
		}

		/* Output object file in object file list if desired */
		if (vm->object_list != NULL) {
			fprintf(vm->object_list, "%s\n", elf->pathname);
			fflush(vm->object_list);
		}

		/* If object path entry was an ELF file, mark it as loaded */
		if (ent->type == _JC_OBJPATH_ELFFILE) {
			_JC_ASSERT(!loader->objects_loaded[i]);
			loader->objects_loaded[i] = JNI_TRUE;
		}

		/* Release our reference on ELF file */
		_jc_elf_unref(&elf);

		/* Done */
		return JNI_OK;
	}

	/* Not found */
	_JC_EX_STORE(env, LinkageError,
	    "no valid ELF object found containing `%s'", name);
	return JNI_ERR;
}

/*
 * Find a class' ELF object file in a directory hierarchy and read
 * it in if found. Here we also look for "_package.o".
 *
 * If unsuccessful an exception is stored.
 */
static _jc_elf *
_jc_load_object_dir(_jc_env *env, _jc_class_loader *loader,
	const char *dir, const char *name)
{
	_jc_elf *elf;
	char *ename;
	char *path;
	char *s;

	/* Generate encoded name for class */
	if ((ename = _JC_STACK_ALLOC(env,
	    _jc_name_encode(name, NULL, JNI_TRUE) + 1)) == NULL)
		return NULL;
	_jc_name_encode(name, ename, JNI_TRUE);

	/* Get buffer big enough for both pathnames we'll try */
	if ((path = _JC_STACK_ALLOC(env, strlen(dir)
	    + sizeof(_JC_FILE_SEPARATOR) + strlen(ename)
	    + sizeof(_JC_PACKAGE_OBJECT_NAME))) == NULL)
		return NULL;

	/* Generate class-specific object file name */
	sprintf(path, "%s%s%s.o", dir, _JC_FILE_SEPARATOR, ename);

	/* Try to load class-specific ELF object file */
	if ((elf = _jc_elf_load(env, loader, path)) != NULL)
		return elf;

	/* Generate package object file name */
	for (s = path + strlen(path); s > path && s[-1] != '/'; s--);
	strcpy(s, _JC_PACKAGE_OBJECT_NAME);

	/* Try to load combo ELF object file */
	if ((elf = _jc_elf_load(env, loader, path)) != NULL)
		return elf;

	/* Not found */
	return NULL;
}

/*
 * Generate the ELF object file for the named class.
 *
 * If unsuccessful an exception is posted.
 */
jint
_jc_generate_object(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;
	jobject nameString = NULL;
	jobject genObj = NULL;
	jint status = JNI_ERR;

	/* Sanity check */
	_JC_ASSERT(vm->loader_enabled);
	_JC_ASSERT(vm->generation_enabled);

	/* Is the "compiler" disabled? */
	if (vm->compiler_disabled) {
		_jc_post_exception_msg(env, _JC_LinkageError,
		    "code generated is disabled");
		return JNI_ERR;
	}

	/* Don't generate code during VM initialization */
	if (vm->initialization != NULL) {
		_jc_post_exception_msg(env, _JC_LinkageError,
		    "can't generate code during VM initialization");
		return JNI_ERR;
	}

	/*
	 * Are we already in the middle of generating something?
	 * If so, don't recurse because Soot is probably not reentrant.
	 * Hopefully we're just trying to acquire the class file.
	 */
	if (env->generating != NULL) {
		_jc_post_exception_msg(env, _JC_LinkageError,
		    "recursive attempt to generate `%s' while generating `%s'",
		    name, env->generating);
		return JNI_ERR;
	}

	/* Verbosity */
	VERBOSE(GEN, vm, "generating ELF object for `%s'", name);

	/* Note that we're generating this object */
	env->generating = name;

	/* Get the Generate singleton object */
	if (_jc_invoke_static(env, vm->boot.methods.Generate.v) != JNI_OK)
		goto fail;
	if ((genObj = _jc_new_local_native_ref(env, env->retval.l)) == NULL)
		goto fail;

	/* Put class name in a String object */
	if ((nameString = _jc_new_local_native_ref(env,
	    _jc_new_string(env, name, strlen(name)))) == NULL)
		goto fail;

	/* Invoke Generate.v().generateObject() */
	if (_jc_invoke_virtual(env, vm->boot.methods.Generate.generateObject,
	    *genObj, *nameString, loader->instance) != JNI_OK)
		goto fail;

	/* Done */
	status = JNI_OK;

fail:
	/* Turn off generating flag */
	_JC_ASSERT(env->generating == name);
	env->generating = NULL;

	/* Clean up native refs */
	_jc_free_local_native_ref(&genObj);
	_jc_free_local_native_ref(&nameString);

	/* Report any error */
	if (status != JNI_OK) {
		if ((env->vm->verbose_flags & (1 << _JC_VERBOSE_GEN)) != 0) {
			_jc_printf(vm,
			    "[verbose %s: object generation for `%s' failed: ",
			    _jc_verbose_names[_JC_VERBOSE_GEN], name);
			_jc_fprint_exception_headline(env,
			    stdout, env->head.pending);
			_jc_printf(vm, "]\n");
		}
	}

	/* Done */
	return status;
}

