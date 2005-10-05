
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
 * $Id: deps.c,v 1.1 2004/07/06 00:33:34 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * Generate a list of all other classes directly referenced by this classfile.
 * We do this by parsing CONSTANT_Class constant pool entries, and
 * field and method signatures.
 *
 * This list tells us which other classfiles we need to acquire to be
 * able to generate the C code for this class. Note that in the process
 * of acquiring any classfile, we automatically acquire the classfiles for
 * that class' superclasses and superinterfaces, so we don't need to
 * explicitly add them to the list.
 *
 * Similarly, this list tells us which other classes must remain loaded
 * as long as this class is loaded (i.e., this class has implicit references
 * to the dependent classes).
 *
 * Returns the number of entries, or -1 if there was an error.
 * If unsuccessful an exception is stored.
 *
 * NOTE: The caller is responsible for freeing the returned array
 * of _jc_class_ref structures.
 *
 * NOTE: The names in the array point into the 'cfile' data structure
 * and therefore become invalid when it does.
 */
int
_jc_gen_deplist(_jc_env *env, _jc_classfile *cfile, _jc_class_ref **arrayp)
{
	const size_t name_len = strlen(cfile->name);
	_jc_class_ref *array = NULL;
	_jc_class_ref dummy;
	u_char ptype;
	int num;
	int i;

pass2:
	/* Reset counter */
	num = 0;

	/* Parse out referenced class names */
	for (i = 0; i < cfile->num_constants; i++) {
		_jc_cf_constant *const cp = &cfile->constants[i];
		_jc_class_ref *const cref = array != NULL ?
		    &array[num] : &dummy;

		if (cp->type != CONSTANT_Class)
			continue;
		_jc_parse_class_ref(cp->u.Class, cref, JNI_TRUE, &ptype);
		if (ptype == _JC_TYPE_REFERENCE)
			num++;
	}
	for (i = 0; i < cfile->num_fields; i++) {
		_jc_cf_field *const field = &cfile->fields[i];
		_jc_class_ref *const cref = array != NULL ?
		    &array[num] : &dummy;

		_jc_parse_class_ref(field->descriptor, cref, JNI_FALSE, &ptype);
		if (ptype == _JC_TYPE_REFERENCE)
			num++;
	}
	for (i = 0; i < cfile->num_methods; i++) {
		_jc_cf_method *const method = &cfile->methods[i];
		const char *s;

		for (s = method->descriptor; *s != '\0'; ) {
			_jc_class_ref *const cref = array != NULL ?
			    &array[num] : &dummy;

			s =_jc_parse_class_ref(s, cref, JNI_FALSE, &ptype);
			if (ptype == _JC_TYPE_REFERENCE)
				num++;
		}
	}

	/* After first pass, allocate array and re-parse */
	if (array == NULL) {
		if ((array = _jc_vm_alloc(env,
		    (num + 1) * sizeof(*array))) == NULL)
			return -1;
		goto pass2;
	}

	/* Sort and uniqify the list, and remove self-references */
	qsort(array, num, sizeof(*array), _jc_class_ref_compare);
	for (i = 0; i < num; i++) {
		_jc_class_ref *const first = &array[i];
		int repeat;

		/* Count how many copies of the same entry */
		for (repeat = 0; i + repeat < num - 1; repeat++) {
			_jc_class_ref *const next = &array[i + 1 + repeat];

			if (_jc_class_ref_compare(first, next) != 0)
				break;
		}

		/* Compress out repeats */
		if (repeat > 0) {
			memmove(&array[i + 1], &array[i + 1 + repeat],
			    ((num -= repeat) - (i + 1)) * sizeof(*array));
		}

		/* Remove self-references */
		if (first->len == name_len
		    && strncmp(first->name, cfile->name, name_len) == 0) {
			memmove(&array[i], &array[i + 1],
			    (--num - i) * sizeof(*array));
		}
	}

	/* Done */
	*arrayp = array;
	return num;
}

