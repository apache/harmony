
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
 * Create a subdirectory under 'root' if it doesn't already exist.
 * The pathname is the name of any file in the subdirectory.
 */
jint
_jc_create_subdir(_jc_env *env, const char *root, const char *pathname)
{
	const size_t rlen = strlen(root);
	const char *error_path;
	struct stat info;
	char *next;
	char *buf;
	char *s;

	/* The root directory must exist */
	error_path = root;
	if (stat(root, &info) == -1)
		goto io_err;
	if ((info.st_mode & S_IFMT) != S_IFDIR) {
		errno = ENOTDIR;
		goto io_err;
	}

	/* Check successive subdirectories */
	if ((buf = _JC_FORMAT_STRING(env, "%s%s%s",
	    root, _JC_FILE_SEPARATOR, pathname)) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}
	error_path = buf;
	for (s = buf + rlen + 1; ; s = next) {

		/* Another directory in path? If not, we're done */
		if ((next = strstr(s, _JC_FILE_SEPARATOR)) == NULL)
			return JNI_OK;

		/* Check directory status; create if not found */
		*next = '\0';
		if (stat(buf, &info) == -1) {
			if (errno != ENOENT)
				goto io_err;
			if (mkdir(buf, 0755) == -1)
				goto io_err;
		} else if ((info.st_mode & S_IFMT) != S_IFDIR) {
			errno = ENOTDIR;
			goto io_err;
		}
		strcpy(next, _JC_FILE_SEPARATOR);
		next += strlen(_JC_FILE_SEPARATOR);
	}

io_err:
	/* Failed; throw an error */
	_jc_post_exception_msg(env, _JC_InternalError,
	    "%s: %s", error_path, strerror(errno));
	return JNI_ERR;
}

/*
 * Compare two class references.
 */
int
_jc_class_ref_compare(const void *v1, const void *v2)
{
	const _jc_class_ref *const cref1 = v1;
	const _jc_class_ref *const cref2 = v2;
	int diff;

	if (cref1->len < cref2->len)
		diff = strncmp(cref1->name, cref2->name, cref1->len);
	else
		diff = strncmp(cref1->name, cref2->name, cref2->len);
	if (diff != 0)
		return diff;
	return (cref1->len > cref2->len) - (cref1->len < cref2->len);
}

/*
 * JC-encode a Java name into the supplied buffer. Returns then length
 * of the encoded name. The 'buf' may be NULL to just compute the length.
 *
 * If 'pass_slash' is true, then pass '/' characters through unencoded.
 */
size_t
_jc_name_encode(const char *name, char *buf, jboolean pass_slash)
{
	static const char hexdigs[] = "0123456789abcdef";
	size_t len = 0;
	const char *s;

	for (s = name; *s != '\0'; s++) {
		const char ch = *s;

		if (isalnum(ch) || (ch == '/' && pass_slash)) {
			if (buf != NULL)
				buf[len] = ch;
			len++;
		} else if (ch == '/' || ch == '.') {
			if (buf != NULL)
				buf[len] = '_';
			len++;
		} else {
			if (buf != NULL) {
				buf[len + 0] = '_';
				buf[len + 1] = '_';
				buf[len + 2] = hexdigs[(ch >> 4) & 0x0f];
				buf[len + 3] = hexdigs[ch & 0x0f];
			}
			len += 4;
		}
	}
	if (buf != NULL)
		buf[len] = '\0';
	return len;
}

/*
 * Decode a JC-encoded Java name, stopping at the first '$' or end of string.
 * The supplied buffer is assumed to be big enough; it need not be bigger
 * than strlen(name) + 1 in all cases.
 *
 * Returns NULL if the string was not validly encoded, otherwise a pointer
 * to the terminating '\0' byte in the decoded buffer.
 */
char *
_jc_name_decode(const char *name, char *buf)
{
	const char *t;
	char *p;
	int i;

	for (p = buf, t = name; *t != '\0' && *t != '$'; ) {
		switch (*t) {
		case '_':

			// Try to decode an escaped character
			if (*++t == '_' && isxdigit(t[1]) && isxdigit(t[2])) {
				for (*p = '\0', i = 1; i <= 2; i++)
					*p = (*p << 4) | _JC_HEXVAL(t[i]);
				t += 3;
				p++;
				break;
			}

			// It's just a slash
			*p++ = '/';
			break;
		default:
			*p++ = *t++;
			break;
		}
	}
	*p = '\0';

	/* Done */
	return p;
}

/*
 * Parse a base class name out of a signature, stripping off array dimensions.
 *
 * Returns a pointer to the character after the parsed class name.
 * If 'cc' is false then non-array class names are expected to be encoded
 * "Llike/This;", otherwise "like/This" (the latter is the case for
 * CONSTANT_Class constant pool entries).
 *
 * Upon return '*ptype' will be set to the appropriate _JC_TYPE_XXX value
 * for the base type (possibly _JC_TYPE_INVALID or _JC_TYPE_VOID).
 */
const char *
_jc_parse_class_ref(const char *s, _jc_class_ref *rc, int cc, u_char *ptype)
{
	int dims;

	for (dims = 0; s[dims] == '['; dims++);
	s += dims;
	if (dims == 0 && cc) {			/* can't be primitive */
		rc->name = s;
		rc->len = strlen(s);
		*ptype = _JC_TYPE_REFERENCE;
		return s + rc->len;
	}
	if (*s != 'L') {
		rc->name = s;
		rc->len = 1;
		*ptype = _jc_sig_types[(u_char)*s];
		return s + 1;
	}
	rc->name = ++s;
	while (*s != '\0' && *s != ';')
		s++;
	rc->len = s - rc->name;
	*ptype = _JC_TYPE_REFERENCE;
	return *s == ';' ? s + 1 : s;
}

/*
 * Parse a directory search path into _jc_cpath_entry structures.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_parse_classpath(_jc_env *env, const char *path,
	_jc_cpath_entry **listp, int *lenp)
{
	_jc_cpath_entry *list;
	char **pathnames;
	int len;
	int i;

	/* Chop up classpath into components and count them */
	if ((pathnames = _jc_parse_searchpath(env, path)) == NULL)
		return JNI_ERR;
	for (len = 0; pathnames[len] != NULL; len++);

	/* Allocate class path entry array */
	if ((list = _jc_vm_realloc(env, *listp,
	    (*lenp + len) * sizeof(*list))) == NULL) {
		while (len > 0)
			_jc_vm_free(&pathnames[--len]);
		_jc_vm_free(&pathnames);
		return JNI_ERR;
	}
	memset(list + *lenp, 0, len * sizeof(*list));

	/* Initialize and append entries */
	for (i = 0; i < len; i++) {
		_jc_cpath_entry *const ent = &list[*lenp + i];

		ent->type = _JC_CPATH_UNKNOWN;
		ent->pathname = pathnames[i];
	}

	/* Return updated list */
	*listp = list;
	*lenp += len;

	/* Done */
	_jc_vm_free(&pathnames);
	return JNI_OK;
}

/*
 * Parse a directory search path into component directories.
 *
 * If unsuccessful an exception is stored.
 */
char **
_jc_parse_searchpath(_jc_env *env, const char *path)
{
	const size_t ps_len = strlen(_JC_PATH_SEPARATOR);
	char **array = NULL;
	const char *s;
	int num;

again:
	for (s = path, num = 0; JNI_TRUE; num++) {
		const char *t;
		int len;

		while (strncmp(s, _JC_PATH_SEPARATOR, ps_len) == 0)
			s += ps_len;
		if (*s == '\0')
			break;
		if ((t = strstr(s, _JC_PATH_SEPARATOR)) == NULL)
			t = s + strlen(s);
		len = t - s;
		if (array != NULL
		    && (array[num] = _jc_vm_strndup(env, s, len)) == NULL) {
			while (num > 0)
				_jc_vm_free(&array[--num]);
			_jc_vm_free(&array);
			return NULL;
		}
		s += len;
		if (array != NULL) {
			while (len > 0
			    && array[num][len - 1] == _JC_FILE_SEPARATOR[0])
				array[num][--len] = '\0';
		}
	}
	if (array == NULL) {
		if ((array = _jc_vm_zalloc(env,
		    (num + 1) * sizeof(*array))) == NULL)
			return NULL;
		goto again;
	}
	array[num] = NULL;
	return array;
}

/*
 * Compare two nodes in a loader's defining loader type tree.
 */
int
_jc_type_cmp(const void *item1, const void *item2)
{
	const _jc_type *const type1 = item1;
	const _jc_type *const type2 = item2;

	return strcmp(type1->name, type2->name);
}

/*
 * Compare two nodes in a loader's initiated type tree
 * or partially derived type tree.
 */
int
_jc_node_cmp(const void *item1, const void *item2)
{
	const _jc_type_node *const node1 = item1;
	const _jc_type_node *const node2 = item2;

	return strcmp(node1->type->name, node2->type->name);
}

/*
 * Compare two _jc_field **'s, sorting in the same way as
 * org.dellroad.jc.cgen.Util.fieldComparator.
 */
int
_jc_field_compare(const void *v1, const void *v2)
{
	_jc_field *const field1 = *((_jc_field **)v1);
	_jc_field *const field2 = *((_jc_field **)v2);
	const u_char ptype1 = _jc_sig_types[(u_char)*field1->signature];
	const u_char ptype2 = _jc_sig_types[(u_char)*field2->signature];
	int diff;

	if ((diff = !_JC_ACC_TEST(field1, STATIC)
	    - !_JC_ACC_TEST(field2, STATIC)) != 0)
		return diff;
	if ((diff = _jc_field_type_sort[ptype1]
	    - _jc_field_type_sort[ptype2]) != 0)
		return diff;
	if ((diff = strcmp(field1->name, field2->name)) != 0)
		return diff;
	return strcmp(field1->signature, field2->signature);
}

/*
 * Compare two _jc_method **'s, sorting in the same way as
 * org.dellroad.jc.cgen.Util.methodComparator.
 */
int
_jc_method_compare(const void *v1, const void *v2)
{
	_jc_method *const method1 = *((_jc_method **)v1);
	_jc_method *const method2 = *((_jc_method **)v2);
	int diff;

	if ((diff = strcmp(method1->name, method2->name)) != 0)
		return diff;
	return strcmp(method1->signature, method2->signature);
}

/*
 * Determine the length of JNI encoding a method name or signature.
 */
size_t
_jc_jni_encode_length(const char *s)
{
	size_t len;

	for (len = 0; *s != '\0'; s++) {
		const u_char ch = *s;

		if (isalnum(ch) || ch == '/')
			len++;
		else if (ch == '_' || ch == ';' || ch == '[')
			len += 2;
		else if (ch == '(')
			/* skip */;
		else if (ch == ')')
			break;
		else if ((ch & 0x80) == 0x00)
			len += 6;
		else if ((ch & 0xe0) == 0xc0) {
			len += 6;
			s++;
		} else {
			_JC_ASSERT((ch & 0xf0) == 0xe0);
			len += 6;
			s += 2;
		}
	}
	return len;
}

/*
 * JNI encode a string.
 */
void
_jc_jni_encode(char **bufp, const char *s)
{
	while (*s != '\0') {
		u_char ch = *s++;

		if (isalnum(ch))
			*(*bufp)++ = ch;
		else if (ch == '/')
			*(*bufp)++ = '_';
		else if (ch == '_') {
			*(*bufp)++ = '_';
			*(*bufp)++ = '1';
		} else if (ch == ';') {
			*(*bufp)++ = '_';
			*(*bufp)++ = '2';
		} else if (ch == '[') {
			*(*bufp)++ = '_';
			*(*bufp)++ = '3';
		} else if (ch == '(')
			/* skip */;
		else if (ch == ')')
			break;
		else {
			static const char hexdigits[] = "0123456789abcdef";
			jchar jc;

			*(*bufp)++ = '_';
			*(*bufp)++ = '0';
			if ((ch & 0x80) == 0x00)
				jc = ch & 0x7f;
			else if ((ch & 0xe0) == 0xc0) {
				jc = (ch & 0x1f) << 6;
				ch = *s++;
				_JC_ASSERT((ch & 0xc0) == 0x80);
				jc |= (ch & 0x3f);
			} else {
				_JC_ASSERT(((ch >> 4) & 0x0f) == 0x0e);
				jc = (ch & 0x0f) << 12;
				ch = *s++;
				_JC_ASSERT((ch & 0xc0) == 0x80);
				jc |= (ch & 0x3f) << 6;
				ch = *s++;
				_JC_ASSERT((ch & 0xc0) == 0x80);
				jc |= (ch & 0x3f);
			}
			*(*bufp)++ = hexdigits[(jc >> 12) & 0x0f];
			*(*bufp)++ = hexdigits[(jc >>  8) & 0x0f];
			*(*bufp)++ = hexdigits[(jc >>  4) & 0x0f];
			*(*bufp)++ = hexdigits[(jc      ) & 0x0f];
		}
	}
}

