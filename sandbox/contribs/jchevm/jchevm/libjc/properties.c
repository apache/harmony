
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

/* List of fixed default properties */
static const _jc_property _jc_fixed_properties[] = {
{ "java.version",			"1.4.2" },
{ "java.vendor",			"JC virtual machine project" },
{ "java.vendor.url",			"http://jcvm.sourceforge.net/" },
{ "java.home",				_AC_DATADIR "/jc" },
{ "java.vm.name",			"JC virtual machine" },
{ "java.vm.vendor",			"JC virtual machine project" },
{ "java.vm.version",			PACKAGE_VERSION },
{ "java.vm.specification.name",		"Java Virtual Machine Specification" },
{ "java.vm.specification.vendor",	"Sun Microsystems Inc." },
{ "java.vm.specification.version",	"1.0" },
{ "java.specification.name",		"Java Platform API Specification" },
{ "java.specification.vendor",		"Sun Microsystems Inc." },
{ "java.specification.version",		"1.4" },
{ "java.class.version",			"46.0" },
{ "java.io.tmpdir",			_JC_TEMP_DIR },
{ "file.separator",			_JC_FILE_SEPARATOR },
{ "line.separator",			_JC_LINE_SEPARATOR },
{ "path.separator",			_JC_PATH_SEPARATOR },
{ "java.library.path",			_JC_LIBRARY_PATH },
{ "java.boot.class.path",		_JC_BOOT_CLASS_PATH },
#if _JC_BIG_ENDIAN
{ "gnu.cpu.endian",			"big" },
#else
{ "gnu.cpu.endian",			"little" },
#endif
#if _JC_THREAD_LOCAL_SUPPORT
{ "jc.thread.local",			"true" },
#else
{ "jc.thread.local",			"false" },
#endif
{ "jc.stack.minimum",			_JC_STACK_MINIMUM },
{ "jc.stack.maximum",			_JC_STACK_MAXIMUM },
{ "jc.stack.default",			_JC_STACK_DEFAULT },
{ "jc.java.stack.size",			_JC_JAVA_STACK_DEFAULT },
{ "jc.heap.size",			_JC_DEFAULT_HEAP_SIZE },
{ "jc.loader.size",			_JC_DEFAULT_LOADER_SIZE },
{ "jc.heap.granularity",		_JC_DEFAULT_HEAP_GRANULARITY },
{ NULL,					NULL }
};

/* Internal functions */
static jint		_jc_digest_size(_jc_env *env, size_t *ptr,
				_jc_property *prop, size_t max);
static _jc_property	*_jc_property_get(_jc_jvm *vm, const char *name);
static int		_jc_property_cmp(const void *item1, const void *item2);

/*
 * Set the default system properties.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_set_system_properties(_jc_env *env)
{
	const char *home_dir;
	struct utsname uts;
	struct passwd *pw;
	time_t now;
	char *s;
	int i;

	/* Set fixed properties */
	for (i = 0; _jc_fixed_properties[i].name != NULL; i++) {
		const _jc_property *const prop = &_jc_fixed_properties[i];

		if (_jc_set_property(env, prop->name, prop->value) != JNI_OK)
			return JNI_ERR;
	}

	/* Set current working directory */
	for (s = NULL, i = 64; JNI_TRUE; i *= 2) {
		if (i < 0)
			return JNI_ERR;
		_jc_vm_free(&s);
		if ((s = _jc_vm_zalloc(env, i)) == NULL)
			return JNI_ERR;
		if (getcwd(s, i) != NULL)
			break;
		if (errno != ERANGE) {
			_JC_EX_STORE(env, InternalError, "%s: %s",
			    "getcwd", strerror(errno));
			return JNI_ERR;
		}
	}
	if (_jc_set_property(env, "user.dir", s) != JNI_OK) {
		_jc_vm_free(&s);
		return JNI_ERR;
	}
	_jc_vm_free(&s);

	/* Set user name and home directory */
	if ((pw = getpwuid(getuid())) == NULL) {
		_JC_EX_STORE(env, InternalError, "%s: %s",
		    "getpwuid", strerror(errno));
		return JNI_ERR;
	}
	if (_jc_set_property(env, "user.name", pw->pw_name) != JNI_OK)
		return JNI_ERR;
	if ((home_dir = getenv("HOME")) == NULL)
		home_dir = pw->pw_dir;
	if (_jc_set_property(env, "user.home", home_dir) != JNI_OK)
		return JNI_ERR;

	/* Set user timezone */
	now = time(NULL);
	if (_jc_set_property(env,
	    "user.timezone", localtime(&now)->tm_zone) != JNI_OK)
		return JNI_ERR;

	/* Set operating system info */
	if (uname(&uts) == -1) {
		_JC_EX_STORE(env, InternalError, "%s: %s",
		    "uname", strerror(errno));
		return JNI_ERR;
	}
	if (strcmp(uts.machine, "i486") == 0
	    || strcmp(uts.machine, "i586") == 0
	    || strcmp(uts.machine, "i686") == 0)
		strcpy(uts.machine, "i386");
	if (_jc_set_property(env, "os.arch", uts.machine) != JNI_OK)
		return JNI_ERR;
	if (_jc_set_property(env, "os.name", uts.sysname) != JNI_OK)
		return JNI_ERR;
	if (_jc_set_property(env, "os.version", uts.release) != JNI_OK)
		return JNI_ERR;

	/* Done */
	endpwent();
	return JNI_OK;
}

/*
 * Set a system property.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_set_property(_jc_env *env, const char *name, const char *value)
{
	return _jc_set_property2(env, name, strlen(name), value);
}

/*
 * Set a system property, where the name does not have to be
 * nul-terminated.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_set_property2(_jc_env *env, const char *name,
	size_t name_len, const char *value)
{
	_jc_jvm *const vm = env->vm;
	_jc_properties *const props = &vm->system_properties;
	_jc_property *prop;
	char *new_value;
	char *new_name;
	int i;

	/* Replace property if it already exists */
	for (i = 0; i < props->length; i++) {
		prop = &props->elems[i];
		if (strncmp(prop->name, name, name_len) == 0
		    && prop->name[name_len] == '\0') {
			if ((new_value = _jc_vm_strdup(env, value)) == NULL)
				return JNI_ERR;
			_jc_vm_free(&prop->value);
			prop->value = new_value;
			return JNI_OK;
		}
	}

	/* Extend properties list if necessary */
	if (props->length == props->allocated) {
		u_int new_alloc = props->allocated + 128;
		_jc_property *new_elems;

		if ((new_elems = _jc_vm_realloc(env, props->elems,
		    new_alloc * sizeof(*new_elems))) == NULL)
			return JNI_ERR;
		props->elems = new_elems;
		props->allocated = new_alloc;
	}

	/* Add property to list */
	prop = &props->elems[props->length];
	memset(prop, 0, sizeof(*prop));
	if ((new_name = _jc_vm_strndup(env, name, name_len)) == NULL)
		return JNI_ERR;
	if ((new_value = _jc_vm_strdup(env, value)) == NULL) {
		_jc_vm_free(&new_name);
		return JNI_ERR;
	}
	prop->name = new_name;
	prop->value = new_value;

	/* Done */
	props->length++;
	return JNI_OK;
}

/*
 * Process system properties that have special meanings.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_digest_properties(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_property *prop;
	size_t size;
	int i;

	/* Sort properties for faster searching */
	qsort(vm->system_properties.elems, vm->system_properties.length,
	    sizeof(*vm->system_properties.elems), _jc_property_cmp);

	/* Sanity check */
	_JC_ASSERT(vm->boot.class_path == NULL);
	_JC_ASSERT(vm->boot.class_path_len == 0);

	/* Get boot loader class search path prepends */
	prop = _jc_property_get(vm, "java.boot.class.path.prepend");
	if (prop != NULL && _jc_parse_classpath(env, prop->value,
	    &vm->boot.class_path, &vm->boot.class_path_len) == JNI_ERR)
		return JNI_ERR;

	/* Get boot loader class search path */
	prop = _jc_property_get(vm, "java.boot.class.path");
	_JC_ASSERT(prop != NULL);
	if (_jc_parse_classpath(env, prop->value,
	    &vm->boot.class_path, &vm->boot.class_path_len) == JNI_ERR)
		return JNI_ERR;

	/* Get boot loader class search path appends */
	prop = _jc_property_get(vm, "java.boot.class.path.append");
	if (prop != NULL && _jc_parse_classpath(env, prop->value,
	    &vm->boot.class_path, &vm->boot.class_path_len) == JNI_ERR)
		return JNI_ERR;

	/* Get thread stack minimum, maximum, and default */
	prop = _jc_property_get(vm, "jc.stack.minimum");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &vm->threads.stack_minimum, prop, 0) != JNI_OK)
		return JNI_ERR;
	prop = _jc_property_get(vm, "jc.stack.maximum");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &vm->threads.stack_maximum, prop, 0) != JNI_OK)
		return JNI_ERR;
	prop = _jc_property_get(vm, "jc.stack.default");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &vm->threads.stack_default, prop, 0) != JNI_OK)
		return JNI_ERR;
	prop = _jc_property_get(vm, "jc.java.stack.size");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &size, prop, 0) != JNI_OK)
		return JNI_ERR;
	vm->java_stack_size = size;

	/* Get heap size and granularity factor */
	prop = _jc_property_get(vm, "jc.heap.size");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &vm->heap.size, prop, 0) != JNI_OK)
		return JNI_ERR;
	prop = _jc_property_get(vm, "jc.heap.granularity");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &size, prop, 99) != JNI_OK)
		return JNI_ERR;
	vm->heap.granularity = size;
	prop = _jc_property_get(vm, "jc.loader.size");
	_JC_ASSERT(prop != NULL);
	if (_jc_digest_size(env, &size, prop, 0) != JNI_OK)
		return JNI_ERR;
	vm->max_loader_pages = _JC_HOWMANY(size, _JC_PAGE_SIZE);
	vm->avail_loader_pages = vm->max_loader_pages;

	/* Check stack parameters */
	if (vm->threads.stack_minimum > vm->threads.stack_default
	    || (vm->threads.stack_maximum > 0
	      && (vm->threads.stack_default > vm->threads.stack_maximum
	       || vm->threads.stack_maximum < PTHREAD_STACK_MIN))) {
		_JC_EX_STORE(env, InternalError,
		    "invalid/incompatible values for properties"
		    " `%s' (%d), `%s' (%d), and/or `%s' (%d)",
		    "jc.stack.minimum", vm->threads.stack_minimum,
		    "jc.stack.maximum", vm->threads.stack_maximum,
		    "jc.stack.default", vm->threads.stack_default);
		return JNI_ERR;
	}
	if (vm->java_stack_size < _JC_JAVA_STACK_MARGIN) {
		_JC_EX_STORE(env, InternalError,
		    "too small value %d < %d for `%s'",
		    vm->java_stack_size, _JC_JAVA_STACK_MARGIN,
		    "jc.java.stack.size");
		return JNI_ERR;
	}

	/* Set verbose properties from verbose flags */
	for (i = 0; i < _JC_VERBOSE_MAX; i++) {
		const char *const flag = _jc_verbose_names[i];
		char name[128];

		snprintf(name, sizeof(name), "jc.verbose.%s", flag);
		if (_jc_set_property(env, name, ((vm->verbose_flags
		    & (1 << i)) != 0) ? "true" : "false") != JNI_OK)
			return JNI_ERR;
	}

	/* Done */
	return JNI_OK;
}

/*
 * Free all system properties.
 */
void
_jc_destroy_properties(_jc_jvm *vm)
{
	int i;

	for (i = 0; i < vm->system_properties.length; i++) {
		_jc_property *const prop = &vm->system_properties.elems[i];

		_jc_vm_free(&prop->name);
		_jc_vm_free(&prop->value);
	}
	_jc_vm_free(&vm->system_properties.elems);
	vm->system_properties.allocated = 0;
	vm->system_properties.length = 0;
}

/*
 * Digest an unsigned numerical property. Recognize suffixes for
 * kilobytes, megabytes, etc. and convert the property if found.
 */
static jint
_jc_digest_size(_jc_env *env, size_t *ptr, _jc_property *prop, size_t max)
{
	const char *value = prop->value;
	size_t vlen = strlen(value);
	unsigned long long size;
	unsigned int units = 1;
	char buf[64];
	char *eptr;

	/* Get units, if any */
	if (vlen > 0) {
		switch (value[vlen - 1]) {
		case 'K':
		case 'k':
			units = 1024;
			break;
		case 'M':
		case 'm':
			units = 1024 * 1024;
			break;
		case 'G':
		case 'g':
			units = 1024 * 1024 * 1024;
			break;
		default:
			break;
		}
		if (units != 1) {
			if (vlen > sizeof(buf))
				vlen = sizeof(buf);
			strncpy(buf, value, vlen - 1);
			buf[vlen - 1] = '\0';
			value = buf;
		}
	}

	/* Parse units */
	if (((size = strtoull(value, &eptr, 0)) == ULLONG_MAX
	      && errno == ERANGE)
	    || (eptr == value || *eptr != '\0')) {
		_JC_EX_STORE(env, InternalError,
		    "invalid value `%s' for property `%s'",
		    prop->value, prop->name);
		return JNI_ERR;
	}

	/* Multiply by units and update property */
	if (units > 1) {
		size *= units;
		snprintf(buf, sizeof(buf), "%llu", size);
		if ((value = _jc_vm_strdup(env, buf)) == NULL)
			return JNI_ERR;
		_jc_vm_free(&prop->value);
		prop->value = value;
	}

	/* Check upper bound */
	if (size > SIZE_T_MAX || (max != 0 && size > max)) {
		_JC_EX_STORE(env, InternalError,
		    "value `%s' for property `%s' is too large",
		    prop->value, prop->name);
		return JNI_ERR;
	}

	/* Done */
	*ptr = (size_t)size;
	return JNI_OK;
}

/*
 * Find a property in the sorted list.
 */
static _jc_property *
_jc_property_get(_jc_jvm *vm, const char *name)
{
	_jc_property key;

	key.name = name;
	return bsearch(&key, vm->system_properties.elems,
	    vm->system_properties.length, sizeof(*vm->system_properties.elems),
	    _jc_property_cmp);
}

/*
 * Compare two properties for sorting by name.
 */
static int
_jc_property_cmp(const void *item1, const void *item2)
{
	const _jc_property *const prop1 = item1;
	const _jc_property *const prop2 = item2;

	return strcmp(prop1->name, prop2->name);
}

