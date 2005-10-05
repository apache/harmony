
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
 * $Id: main.c,v 1.3 2005/03/28 15:13:09 archiecobbs Exp $
 */

#include "javah.h"

int
main(int ac, char **av)
{
	jboolean jni = JNI_FALSE;
	jboolean c_files = JNI_FALSE;
	const char *output_dir = ".";
	const char *classpath = ".";
	_jc_classbytes *cb;
	_jc_classfile *cf;
	_jc_env *env;
	_jc_jvm *vm;

	/* Initialize */
	env = _jc_support_init();
	vm = env->vm;

	/* Parse command line */
	for (av++, ac--; ac > 0 && **av == '-'; av++, ac--) {
		if (strcmp(av[0], "-jni") == 0)
			jni = JNI_TRUE;
		else if (strcmp(*av, "-c") == 0)
			c_files = JNI_TRUE;
		else if (strcmp(*av, "-classpath") == 0
		    || strcmp(*av, "-cp") == 0) {
			av++, ac--;
			if (ac == 0)
				goto usage;
			classpath = *av;
		} else if (strcmp(*av, "-d") == 0) {
			av++, ac--;
			if (ac == 0)
				goto usage;
			output_dir = *av;
		} else
			goto usage;
	}
	if (ac == 0) {
usage:		fprintf(stderr, "Usage: jcjavah [-classpath path]"
		    " [-d output-dir] [-c] [-jni] class ...\n");
		fprintf(stderr, "Options:\n"
		    "    -classpath\tSpecify search path for class files\n"
		    "    -cp\t\tAlias for -classpath\n"
		    "    -d dir\tOutput directory for generated files\n"
		    "    -c\t\tAlso generate C source file stubs\n"
		    "    -jni\tGenerate JNI sources instead of JCNI\n");
		exit(1);
	}

	/* Parse classpath */
	if (_jc_parse_classpath(env, classpath,
	    &vm->boot.class_path, &vm->boot.class_path_len) != JNI_OK)
		errx(1, "%s: %s", _jc_vmex_names[env->ex.num], env->ex.msg);

	/* Process files */
	while (ac-- > 0) {
		char *const classname = *av++;
		int i;

		/* Get class name */
		for (i = 0; classname[i] != '\0'; i++) {
			if (classname[i] == '.')
				classname[i] = '/';
		}

		/* Read in classfile */
		if ((cb = _jc_bootcl_find_classbytes(env,
		   classname, NULL)) == NULL) {
			errx(1, "can't load class `%s': %s: %s", classname,
			    _jc_vmex_names[env->ex.num], env->ex.msg);
		}

		/* Parse classfile */
		if ((cf = _jc_parse_classfile(env, cb, 2)) == NULL) {
			errx(1, "can't parse class `%s': %s: %s", classname,
			    _jc_vmex_names[env->ex.num], env->ex.msg);
		}
		_jc_free_classbytes(&cb);

		/* Output files */
		javah_header(cf, output_dir, jni);
		if (c_files)
			javah_source(cf, output_dir, jni);
		_jc_destroy_classfile(&cf);
	}

	/* Done */
	return 0;
}

