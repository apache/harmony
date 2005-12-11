
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

/* Internal functions */
static void		_jc_munmap_freer(_jc_classbytes *bytes);
static void		_jc_free_freer(_jc_classbytes *bytes);

/*
 * Search for a class file in the filesystem using the bootstrap
 * class loader directory search path, and load it into memory.
 * This is used for the bootstrap loader to find classfiles.
 *
 * If not found, NULL is returned and a NoClassDefFoundError is stored.
 */
_jc_classbytes *
_jc_bootcl_find_classbytes(_jc_env *env, const char *name, int *indexp)
{
	_jc_jvm *const vm = env->vm;
	int i;

	/* Search for file in each class path component */
	for (i = 0; i < vm->boot.class_path_len; i++) {
		_jc_cpath_entry *const ent = &vm->boot.class_path[i];
		_jc_classbytes *bytes = NULL;

try_again:
		/* Look for class in this class path component */
		switch (ent->type) {
		case _JC_CPATH_DIRECTORY:
			_JC_ASSERT(ent->zip == NULL);
			if (_jc_read_classbytes_dir(env,
			    ent, name, &bytes) != JNI_OK)
				return NULL;
			break;
		case _JC_CPATH_ZIPFILE:
			_JC_ASSERT(ent->zip != NULL);
			if (_jc_read_classbytes_zip(env,
			    ent, name, &bytes) != JNI_OK) {
				if (env->ex.num == _JC_IOException)
					ent->type = _JC_CPATH_ERROR;
				return NULL;
			}
			break;
		case _JC_CPATH_ERROR:
			break;
		case _JC_CPATH_UNKNOWN:
		    {
			struct stat info;
			_jc_zip *zip;

			/* Examine file */
			_JC_ASSERT(ent->zip == NULL);
			if (stat(ent->pathname, &info) == -1) {
				_JC_MUTEX_LOCK(env, vm->mutex);
				if (ent->type == _JC_CPATH_UNKNOWN)
					ent->type = _JC_CPATH_ERROR;
				_JC_MUTEX_UNLOCK(env, vm->mutex);
				goto try_again;
			}

			/* If it's a directory, change type and try again */
			if ((info.st_mode & S_IFMT) == S_IFDIR) {
				_JC_MUTEX_LOCK(env, vm->mutex);
				if (ent->type == _JC_CPATH_UNKNOWN)
					ent->type = _JC_CPATH_DIRECTORY;
				_JC_MUTEX_UNLOCK(env, vm->mutex);
				goto try_again;
			}

			/* Otherwise, try to open it as a ZIP file */
			if ((zip = _jc_zip_open(env, ent->pathname)) == NULL) {
				if (env->ex.num != _JC_IOException)
					return NULL;
				_JC_MUTEX_LOCK(env, vm->mutex);
				if (ent->type == _JC_CPATH_UNKNOWN)
					ent->type = _JC_CPATH_ERROR;
				_JC_MUTEX_UNLOCK(env, vm->mutex);
				goto try_again;
			}

			/* That worked, so change type and try again */
			_JC_MUTEX_LOCK(env, vm->mutex);
			if (ent->type == _JC_CPATH_UNKNOWN) {
				_JC_ASSERT(ent->zip == NULL);
				ent->type = _JC_CPATH_ZIPFILE;
				ent->zip = zip;
				zip = NULL;
			}
			_JC_MUTEX_UNLOCK(env, vm->mutex);
			_jc_zip_close(&zip);
			goto try_again;
		    }
		default:
			_JC_ASSERT(JNI_FALSE);
		}

		/* If found, return them */
		if (bytes != NULL) {
			if (indexp != NULL)
				*indexp = i;
			return bytes;
		}
	}

	/* Not found */
	_JC_EX_STORE(env, NoClassDefFoundError, "%s", name);
	for (i = 0; env->ex.msg[i] != '\0'; i++) {
		if (env->ex.msg[i] == '/')
			env->ex.msg[i] = '.';
	}
	return NULL;
}

/*
 * Read a class file from the given directory hierarchy.
 *
 * Sets *bytesp to the class file bytes, or NULL if not found.
 * An exception is stored only if something else bad happens.
 */
jint
_jc_read_classbytes_dir(_jc_env *env, _jc_cpath_entry *ent,
	const char *name, _jc_classbytes **bytesp)
{
	_jc_classbytes *bytes;
	struct stat info;
	char *path;
	void *addr;
	int fd = -1;
	char *s;

	/* Sanity check */
	_JC_ASSERT(ent->type == _JC_CPATH_DIRECTORY);

	/* Sanity check name to avoid reading arbitrary files */
	if (*name == '/')
		goto not_found;

	/* Concatenate directory and class name to get filename */
	if ((path = _JC_FORMAT_STRING(env,
	    "%s/%s.class", ent->pathname, name)) == NULL)
		return JNI_ERR;

	/* Convert Java "/" separator to directory path separator */
	for (s = path; *s != '\0'; s++) {
		if (*s == '/')
			*s = _JC_FILE_SEPARATOR[0];
	}

	/* Open file */
	if ((fd = open(path, O_RDONLY)) == -1) {
		if (errno == ENOENT)
			goto not_found;
		_JC_EX_STORE(env, IOException, "can't read `%s': %s",
		    name, strerror(errno));
		goto fail;
	}
	(void)fcntl(fd, F_SETFD, 1);
	if (fstat(fd, &info) == -1) {
		_JC_EX_STORE(env, IOException, "can't stat `%s': %s",
		    name, strerror(errno));
		goto fail;
	}

	/* Memory map in the file */
	if ((addr = mmap(NULL, info.st_size, PROT_READ,
	    MAP_PRIVATE, fd, 0)) == MAP_FAILED) {
		_JC_EX_STORE(env, IOException, "can't map `%s': %s",
		    name, strerror(errno));
		goto fail;
	}

	/* Create _jc_classbytes object */
	if ((bytes = _jc_vm_zalloc(env, sizeof(*bytes))) == NULL) {
		munmap(addr, info.st_size);
		goto fail;
	}
	bytes->bytes = addr;
	bytes->length = info.st_size;
	bytes->freer = _jc_munmap_freer;

	/* Set return value to the bytes found */
	*bytesp = bytes;

not_found:
	/* No error, just not found */
	if (fd != -1)
		close(fd);
	return JNI_OK;

fail:
	/* Clean up after error */
	if (fd != -1)
		close(fd);
	return JNI_ERR;
}

/*
 * Read a class file from the given ZIP file.
 *
 * Sets *bytesp to the class file bytes, or NULL if not found.
 * An exception is stored only if something else bad happens.
 */
jint
_jc_read_classbytes_zip(_jc_env *env, _jc_cpath_entry *ent,
	const char *name, _jc_classbytes **bytesp)
{
	_jc_classbytes *bytes = NULL;
	_jc_zip *const zip = ent->zip;
	_jc_zip_entry *zent;
	char *zent_name;
	int indx;

	/* Sanity check */
	_JC_ASSERT(ent->type == _JC_CPATH_ZIPFILE);
	_JC_ASSERT(ent->zip != NULL);

	/* Append '.class' suffix to class name */
	if ((zent_name = _JC_FORMAT_STRING(env, "%s.class", name)) == NULL)
		return JNI_ERR;

	/* Get file index */
	if ((indx = _jc_zip_search(ent->zip, zent_name)) == -1)
		return JNI_OK;				/* not found */
	zent = &zip->entries[indx];

	/* Create _jc_classbytes object */
	if ((bytes = _jc_vm_alloc(env,
	    sizeof(*bytes) + zent->uncomp_len)) == NULL)
		return JNI_ERR;
	memset(bytes, 0, sizeof(*bytes));
	bytes->bytes = (u_char *)bytes + sizeof(*bytes);
	bytes->length = zent->uncomp_len;
	bytes->freer = _jc_free_freer;

	/* Extract file contents from ZIP file */
	if (_jc_zip_read(env, zip, indx, bytes->bytes) != JNI_OK) {
		_jc_free_classbytes(&bytes);
		return JNI_ERR;
	}

	/* Set return value to the bytes found */
	*bytesp = bytes;
	return JNI_OK;
}

/*
 * Create a _jc_classbytes structure from an array of bytes.
 *
 * If there is an error an exception is stored and NULL returned.
 */
_jc_classbytes *
_jc_copy_classbytes(_jc_env *env, const void *data, size_t len)
{
	_jc_classbytes *bytes;

	/* Create classbytes object */
	if ((bytes = _jc_vm_alloc(env, sizeof(*bytes) + len)) == NULL)
		return NULL;
	memset(bytes, 0, sizeof(*bytes));
	bytes->bytes = (u_char *)bytes + sizeof(*bytes);
	bytes->length = len;
	bytes->freer = _jc_free_freer;

	/* Copy bytes */
	memcpy(bytes->bytes, data, len);

	/* Done */
	return bytes;
}

/*
 * Unreference a _jc_classbytes structure.
 */
void
_jc_free_classbytes(_jc_classbytes **bytesp)
{
	_jc_classbytes *bytes = *bytesp;

	/* Sanity check */
	if (bytes == NULL)
		return;
	*bytesp = NULL;

	/* Free structure */
	(*bytes->freer)(bytes);
}

/*
 * Free a class file that was memory mapped.
 */
static void
_jc_munmap_freer(_jc_classbytes *bytes)
{
	munmap(bytes->bytes, bytes->length);
	_jc_vm_free(&bytes);
}

/*
 * Free a class file that was allocated from the system heap.
 */
static void
_jc_free_freer(_jc_classbytes *bytes)
{
	_jc_vm_free(&bytes);
}

