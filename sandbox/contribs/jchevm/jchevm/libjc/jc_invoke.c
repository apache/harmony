
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

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <ctype.h>
#include <errno.h>
#include <err.h>

#include <popt.h>
#include "config.h"
#include "jni.h"
#include "jc_invoke.h"

/* Name of config files */
#define JC_USER_CONFIG		".jc"
#define JC_GLOBAL_CONFIG	_AC_SYSCONFDIR "/jc.conf"

/* Property names */
#define JAVA_CLASS_PATH		"java.class.path"
#define JAVA_LIBRARY_PATH	"java.library.path"
#define JAVA_BOOT_CLASS_PATH	"java.boot.class.path"

/* JAR loader class */
#define JC_JAR_LOADER_CLASS	"org.dellroad.jc.vm.JarLoader"

enum cmdline_option {
	OPT_CLASSPATH = 1,
	OPT_LIBRARYPATH,
	OPT_BOOTCLASSPATH,
	OPT_JAR,
	OPT_PROPERTY,
	OPT_VERBOSE,
	OPT_VERSION,
	OPT_COMPATOPTS,
};

struct flag_subst {
	const char	*jdk;
	const char	*jc;
	int		arg;
	const char	*prop;
};

/* Subversion revision from version.c */
extern const unsigned long _jc_svn_revision;

/* Verbosity options */
static const	char *const jc_verbose[] = {
	"class",
	"jni",
	"gc",
	"Xexceptions",
	"Xresolution",
	"Xjni-invoke",
	"Xinit",
	NULL
};

/* Map command line option to VM args property */
static const	char *const jc_opt2prop[] = {
	[OPT_CLASSPATH]=	JAVA_CLASS_PATH,
	[OPT_LIBRARYPATH]=	JAVA_LIBRARY_PATH,
	[OPT_BOOTCLASSPATH]=	JAVA_BOOT_CLASS_PATH
};

/* Command line options */
static const	struct poptOption jc_popt_options[] = {
    {	"classpath",		'c', POPT_ARG_STRING, NULL, OPT_CLASSPATH,
	"Set application class loader search path", "path" },
    {	"bootclasspath",	'b', POPT_ARG_STRING, NULL, OPT_BOOTCLASSPATH,
	"Set bootstrap class loader search path", "path" },
    {	"librarypath",		'l', POPT_ARG_STRING, NULL, OPT_LIBRARYPATH,
	"Set native library search path", "path" },
    {	"property",		'p', POPT_ARG_STRING, NULL, OPT_PROPERTY,
	"Set a system property", "name=value" },
    {	"verbose",		'v', POPT_ARG_STRING, NULL, OPT_VERBOSE,
	"Enable verbose output: "
		" class=Class loading"
		", jni=Native libraries"
		", gc=Garbage collection"
		", exceptions=Exceptions"
		", resolution=Class resolution"
		", jni-invoke=Native method calls"
		", init=Class initialization"
		".",
		"opt1,opt2,..." },
    {	"jar",			'j', POPT_ARG_NONE, NULL, OPT_JAR,
	"Execute main class of JAR file", NULL },
    {	"show-options",		'X', POPT_ARG_NONE,   NULL, OPT_COMPATOPTS,
	"Show additional options", NULL },
    {	"version",		'V', POPT_ARG_NONE,   NULL, OPT_VERSION,
	"Display version and exit", NULL },
	POPT_AUTOHELP
	POPT_TABLEEND
};
static const	int jc_num_options
			= sizeof(jc_popt_options) / sizeof(*jc_popt_options);

/* JDK-compatible equivalent command line arguments */
static const struct flag_subst jdk_flags[] = {
    {	"-client",	NULL,		0,	NULL },
    {	"-server",	NULL,		0,	NULL },
    {	"-hotspot",	NULL,		0,	NULL },
    {	"-cp",		"-c",		0,	NULL },
    {	"-classpath",	"-c",		0,	NULL },
    {	"-version",	"-V",		0,	NULL },
    {	"-help",	"-?",		0,	NULL },
    {	"-jar",		"-j",		0,	NULL },
    {	"-mx",		NULL,		0,	"jc.heap.size" },
    {	"-Xmx",		NULL,		0,	"jc.heap.size" },
    {	"-ms",		NULL,		0,	"jc.heap.initial" },
    {	"-Xms",		NULL,		0,	"jc.heap.initial" },
    {	"-ss",		NULL,		0,	"jc.stack.default" },
    {	"-Xss",		NULL,		0,	"jc.stack.default" },
    {	NULL,		NULL,		0,	NULL }
};

/* Internal functions */
static int	jc_run(JNIEnv *env, const char *main_class,
			int ac, const char **av);
static int	jc_read_options(_jc_printer *printer, poptContext pctx,
			const char *path);
static int	jc_process_verbose(JavaVMInitArgs *args,
			_jc_printer *printer, const char *value);
static int	jc_add_vm_property(JavaVMInitArgs *args, _jc_printer *printer,
			const char *name, size_t name_len, const char *value);
static int	jc_add_vm_arg(JavaVMInitArgs *args, _jc_printer *printer,
			const char *option, void *extraInfo);
static int	jc_extend_array(_jc_printer printer,
			void *array, size_t size, int length);
static int	jc_print(int (*printer)(FILE *, const char *, va_list),
			FILE *stream, const char *fmt, ...);
static int	jc_insert_opt(_jc_printer printer, int *acp,
			char ***avp, int pos, const char *opt);

/*
 * Invoke the JC VM with the supplied command line
 * and JNI invocation aguments.
 */
int
_jc_invoke(int orig_ac, const char **orig_av,
	int ignoreUnrecognized, _jc_printer *printer)
{
	JavaVMInitArgs vm_args;
	int rtn = _JC_RETURN_ERROR;
	poptContext pctx = NULL;
	jboolean jar = JNI_FALSE;
	const char *classpath = ".";
	const char *main_class;
	const char *home_dir;
	JavaVM *vm = NULL;
	JNIEnv *env = NULL;
	const char **params;
	char **av = NULL;
	int num_params;
	char *temp;
	void *envp;
	int ac = 0;
	int op;
	int i;

	/* Copy arguments so we can modify them */
	if ((av = malloc((orig_ac + 1) * sizeof(*av))) == NULL) {
		jc_print(printer, stderr, "jc: %s: %s\n",
		    "malloc", strerror(errno));
		goto done;
	}
	for (ac = 0; ac < orig_ac; ac++) {
		if ((av[ac] = strdup(orig_av[ac])) == NULL)
			goto done;
	}

	/* Initialize VM arguments */
	memset(&vm_args, 0, sizeof(vm_args));
	vm_args.version = JNI_VERSION_1_2;
	vm_args.ignoreUnrecognized = ignoreUnrecognized;
	if (jc_add_vm_arg(&vm_args, printer, "vfprintf", printer) != JNI_OK)
		goto done;
	if (jc_add_vm_arg(&vm_args, printer, "abort", abort) != JNI_OK)
		goto done;
	if (jc_add_vm_arg(&vm_args, printer, "exit", exit) != JNI_OK)
		goto done;

	/* Allow JDK-compatible command line flags */
	for (i = 1; i < ac && *av[i] == '-'; i++) {
		const char *const flag = av[i];
		int j;

		/* Skip normal options */
		for (j = 0; j < jc_num_options; j++) {
			const struct poptOption *const op = &jc_popt_options[j];

			if ((op->longName != NULL
			      && strcmp(flag, op->longName) == 0)
			    || (flag[1] == op->shortName && flag[2] == '\0')) {
				if (op->argInfo != POPT_ARG_NONE)
					i++;
				break;
			}
		}
		if (j < jc_num_options)
			continue;

		/* Handle -D flag (we have to separate & insert) */
		if (strncmp(flag, "-D", 2) == 0) {
			if (jc_insert_opt(printer, &ac,
			    &av, i++, "-p") != JNI_OK)
				goto done;
			memmove(av[i], av[i] + 2, strlen(av[i] + 2) + 1);
			continue;
		}

		/* Handle other substitutions */
		for (j = 0; jdk_flags[j].jdk != NULL; j++) {
			const struct flag_subst *const sub = &jdk_flags[j];

			/* Check if flag (or prefix) matches */
			if (strcmp(flag, sub->jdk) != 0
			    && !(sub->prop != NULL
			      && sub->jc == NULL
			      && !sub->arg
			      && strncmp(flag,
			       sub->jdk, strlen(sub->jdk)) == 0))
				continue;

			/* A property substitution? */
			if (sub->prop != NULL) {
				const char *val;
				char *buf;
				int skip;

				/* Get desired property and value */
				if (sub->jc != NULL)
					val = sub->jc;
				else if (!sub->arg)
					val = flag + strlen(sub->jdk);
				else {
					if (i + 1 == ac)
						goto usage;
					val = av[i + 1];
				}

				/* Insert command line flags to set it */
				if (jc_insert_opt(printer, &ac,
				    &av, i++, "-p") != JNI_OK)
					goto done;
				if ((buf = malloc(strlen(sub->prop)
				    + 1 + strlen(val) + 1)) == NULL)
					goto done;
				strcpy(buf, sub->prop);
				strcat(buf, "=");
				strcat(buf, val);
				if (jc_insert_opt(printer, &ac,
				    &av, i++, buf) != JNI_OK) {
					free(buf);
					goto done;
				}
				free(buf);

				/* Delete old command line flag */
				skip = 1 + sub->arg;
				memmove(av + i, av + i + skip,
				    ((ac -= skip) - i + 1) * sizeof(*av));
				i--;
				break;
			}

			/* A direct subsitution? */
			if (sub->jc != NULL) {

				/* Delete old flag */
				memmove(av + i, av + i + 1,
				    (ac-- - i) * sizeof(*av));

				/* Insert new flag */
				if (jc_insert_opt(printer, &ac,
				    &av, i++, sub->jc) != JNI_OK)
					goto done;
				if (sub->arg)
					i++;
				break;
			}

			/* Just ignore it */
			memmove(av + i, av + i + 1, (ac-- - i) * sizeof(*av));
			i--;
			break;
		}
	}

	/* Create option parsing context */
	if ((pctx = poptGetContext(NULL, ac, (const char **)av,
	    jc_popt_options, POPT_CONTEXT_POSIXMEHARDER)) == NULL) {
		jc_print(printer, stderr, "jc: %s: %s\n",
		    "poptGetContext", strerror(errno));
		goto done;
	}
	poptSetOtherOptionHelp(pctx, "[flag ...] <classname> [param ...]");

	/* Apply system-wide options */
	if (jc_read_options(printer, pctx, JC_GLOBAL_CONFIG) != JNI_OK)
		goto done;

	/* Apply user options */
	if ((home_dir = getenv("HOME")) != NULL) {
		const size_t hd_len = strlen(home_dir);
		const size_t cf_len = sizeof(JC_USER_CONFIG) - 1;
		char *path;

		/* Construct pathname to ~/.jc file */
		if ((path = malloc(hd_len + 1 + cf_len + 1)) == NULL) {
			jc_print(printer, stderr, "jc: %s: %s\n",
			    "malloc", strerror(errno));
			goto done;
		}
		memcpy(path, home_dir, hd_len);
		path[hd_len] = '/';
		memcpy(path + hd_len + 1, JC_USER_CONFIG, cf_len + 1);

		/* Apply user-specific options */
		if (jc_read_options(printer, pctx, path) != JNI_OK) {
			free(path);
			goto done;
		}
		free(path);
	}

	/* Parse command line options */
	while ((op = poptGetNextOpt(pctx)) > 0) {
		const char *const value = poptGetOptArg(pctx);

		switch (op) {
		case OPT_CLASSPATH:
			classpath = value;
			break;
		case OPT_LIBRARYPATH:
		case OPT_BOOTCLASSPATH:
		    {
			const char *const name = jc_opt2prop[op];

			if (jc_add_vm_property(&vm_args, printer,
			    name, strlen(name), value) != JNI_OK)
				goto done;
			break;
		    }
		case OPT_JAR:
			jar = JNI_TRUE;
			break;
		case OPT_PROPERTY:
		    {
			const char *eq;

			if ((eq = strchr(value, '=')) == NULL || eq == value) {
				jc_print(printer, stderr,
				   "jc: invalid property option \"%s\"\n",
				   value);
				goto usage;
			}
			if (jc_add_vm_property(&vm_args, printer,
			    value, eq - value, eq + 1) != JNI_OK)
				goto done;
			break;
		    }
		case OPT_VERBOSE:
		    {
			int status;

			if ((status = jc_process_verbose(&vm_args,
			    printer, value)) == JNI_EVERSION)
				goto usage;
			if (status != JNI_OK)
				goto done;
			break;
		    }
		case OPT_VERSION:
			jc_print(printer, stdout,
			    "JC virtual machine version %s (r%lu)\n"
			    "Copyright (C) 2003-2006 Archie L. Cobbs\n"
			    "All rights reserved.\n", VERSION,
			    _jc_svn_revision);
			rtn = _JC_RETURN_NORMAL;
			goto done;
		case OPT_COMPATOPTS:
			jc_print(printer, stdout, "Additional options:\n");
			jc_print(printer, stdout, "  %-16s", "-Dfoo=bar");
			jc_print(printer, stdout, "Same as -p foo=bar\n");
			for (i = 0; jdk_flags[i].jdk != NULL; i++) {
				const struct flag_subst *const sub
				    = &jdk_flags[i];

				jc_print(printer, stdout, "  %-16s", sub->jdk);
				if (sub->prop == NULL) {
					const char *jop = sub->jc;
					int j;

					if (sub->jc == NULL) {
						jc_print(printer, stdout,
						    "Ignored\n");
						continue;
					}
					for (j = 0; jc_popt_options[j].longName
					    != NULL; j++) {
					    	const struct poptOption *const
						  pop = &jc_popt_options[j];

						if (sub->jc[1]
						    == pop->shortName) {
						    	jop = pop->longName;
							break;
						}
					}
					if (strcmp(jop, "-?") == 0)
						jop = "help";
					jc_print(printer, stdout,
					    "Same as --%s\n", jop);
					continue;
				}
				jc_print(printer, stdout,
				    "Same as -p %s=%s\n", sub->prop,
				    sub->jc != NULL ? sub->jc : "");
			}
			rtn = _JC_RETURN_NORMAL;
			goto done;
		}
	}
	if (op != -1) {
		jc_print(printer, stderr, "jc: unknown option \"%s\": %s\n",
		    poptBadOption(pctx, 0), poptStrerror(op));
		goto usage;
	}

	/* Get class name and parameters */
	params = poptGetArgs(pctx);
	for (num_params = 0; params != NULL
	    && params[num_params] != NULL; num_params++);
	if (num_params == 0) {
		poptPrintHelp(pctx, stderr, 0);
		goto done;
	}

	/* For -jar, prepend JAR file to classpath */
	if (jar) {
		if ((temp = alloca(strlen(params[0]) + 1
		    + strlen(classpath) + 1)) == NULL) {
			jc_print(printer, stderr,
			    "jc: %s: %s\n", "alloca", strerror(errno));
			goto done;
		}
		sprintf(temp, "%s:%s", params[0], classpath);
		classpath = temp;
		main_class = JC_JAR_LOADER_CLASS;
	} else {
		main_class = *params++;
		num_params--;
	}

	/* Set classpath property */
	if (jc_add_vm_property(&vm_args, printer, JAVA_CLASS_PATH,
	    sizeof(JAVA_CLASS_PATH) - 1, classpath) != JNI_OK)
		goto done;

	/* Create new VM */
	if (JNI_CreateJavaVM(&vm, &envp, &vm_args) != JNI_OK) {
		jc_print(printer, stderr, "jc: failed to create VM\n");
		goto done;
	}
	env = (JNIEnv *)envp;

	/* Free options */
	while (vm_args.nOptions > 0)
		free(vm_args.options[--vm_args.nOptions].optionString);
	free(vm_args.options);
	vm_args.options = NULL;

	/* Run program */
	if ((rtn = jc_run(env, main_class,
	    num_params, params)) == _JC_RETURN_EXCEPTION)
		(*env)->ExceptionDescribe(env);

	/* Free parsing context */
	poptFreeContext(pctx);
	pctx = NULL;

done:
	/* Clean up and return */
	if (vm != NULL && (*vm)->DestroyJavaVM(vm) != 0)
		warnx("DestroyJavaVM failed");
	if (pctx != NULL)
		poptFreeContext(pctx);
	while (vm_args.nOptions > 0)
		free(vm_args.options[--vm_args.nOptions].optionString);
	free(vm_args.options);
	while (ac > 0)
		free(av[--ac]);
	return rtn;

usage:
	/* Bad command line argument */
	jc_print(printer, stderr, "jc: try \"jc --help\" for help\n");
	goto done;
}

/*
 * Run the Java program.
 */
static int
jc_run(JNIEnv *env, const char *main_class, int ac, const char **av)
{
	jclass string_class;
	jarray param_array;
	jmethodID method;
	jclass class;
	char *buf;
	int i;

	/* Build String[] array of parameters */
	if ((string_class = (*env)->FindClass(env, "java/lang/String")) == NULL)
		return _JC_RETURN_EXCEPTION;
	if ((param_array = (*env)->NewObjectArray(env,
	      ac, string_class, NULL)) == NULL)
		return _JC_RETURN_EXCEPTION;
	(*env)->DeleteLocalRef(env, string_class);
	for (i = 0; i < ac; i++) {
		jstring string;

		if ((string = (*env)->NewStringUTF(env, av[i])) == NULL)
			return _JC_RETURN_EXCEPTION;
		(*env)->SetObjectArrayElement(env, param_array, i, string);
		(*env)->DeleteLocalRef(env, string);
	}

	/* Convert application main class name to internal form */
	if ((buf = alloca(strlen(main_class) + 1)) == NULL)
		return _JC_RETURN_ERROR;
	strcpy(buf, main_class);
	for (i = 0; buf[i] != '\0'; i++) {
		if (buf[i] == '.')
			buf[i] = '/';
	}

	/* Find the application's main class */
	class = (*env)->FindClass(env, buf);
	if ((*env)->ExceptionCheck(env))
		return _JC_RETURN_EXCEPTION;

	/* Invoke main() */
	if ((method = (*env)->GetStaticMethodID(env, class,
	    "main", "([Ljava/lang/String;)V")) == NULL)
		return _JC_RETURN_EXCEPTION;
	(*env)->CallStaticVoidMethod(env, class, method, param_array);
	(*env)->DeleteLocalRef(env, param_array);
	(*env)->DeleteLocalRef(env, class);
	if ((*env)->ExceptionCheck(env))
		return _JC_RETURN_EXCEPTION;

	/* Done */
	return _JC_RETURN_NORMAL;
}

/* 
 * Read command line arguments from a file and prepend them
 * to the supplied parsing context.
 */
static int
jc_read_options(_jc_printer *printer, poptContext pctx, const char *path)
{
	char **options = NULL;
	int num_options = 0;
	int status = JNI_ERR;
	char line[1024];
	int line_num;
	FILE *fp;
	int r;

	/* Open file */
	if ((fp = fopen(path, "r")) == NULL) {
		if (errno == ENOENT)		/* if none found, nevermind */
			return JNI_OK;
		jc_print(printer, stderr,
		    "jc: %s: %s\n", path, strerror(errno));
		goto done;
	}

	/* Read options */
	for (line_num = 1; fgets(line, sizeof(line), fp) != NULL; line_num++) {
		size_t len = strlen(line);
		char *opt;
		char *s;

		/* Check for line too long */
		if (line[len - 1] != '\n') {
			jc_print(printer, stderr,
			    "jc: %s: line too long (line %d)\n",
			    path, line_num);
			goto done;
		}

		/* Trim leading whitespace */
		for (s = line; isspace(*s); s++, len--);

		/* Trim trailing whitespace */
		while (len > 0 && isspace(s[len - 1]))
			len--;
		s[len] = '\0';

		/* Ignore blank lines and # comment lines */
		if (*s == '#' || *s == '\0')
			continue;

		/* Extend option list */
		if (jc_extend_array(printer, &options,
		    sizeof(*options), num_options) != JNI_OK)
			goto done;

		/* Prepend '--' to the option */
		if ((opt = malloc(2 + len + 1)) == NULL) {
			jc_print(printer, stderr,
			    "jc: %s: %s\n", "malloc", strerror(errno));
			goto done;
		}
		opt[0] = '-';
		opt[1] = '-';
		memcpy(opt + 2, s, len);
		opt[2 + len] = '\0';

		/* Add option to the list */
		options[num_options++] = opt;
	}

	/* Terminate option list with a NULL */
	if (jc_extend_array(printer, &options,
	    sizeof(*options), num_options) != JNI_OK)
		goto done;
	options[num_options] = NULL;

	/* Insert options into parsing context */
	if (num_options > 0
	    && (r = poptStuffArgs(pctx, (const char **)options)) < 0) {
		jc_print(printer, stderr, "jc: %s: %s\n",
		    "poptStuffArgs", poptStrerror(r));
		goto done;
	}

	/* OK */
	status = JNI_OK;

done:
	/* Clean up and return */
	if (fp != NULL)
		fclose(fp);
	while (num_options > 0)
		free(options[--num_options]);
	free(options);
	return status;
}

/*
 * Process verbose command line options.
 */
static int
jc_process_verbose(JavaVMInitArgs *args,
	_jc_printer *printer, const char *value)
{
	int verbose_bits = 0;
	const char *s;
	const char *next;
	size_t len;
	char *buf;
	int first;
	int i;

	/* Scan verbose options */
	for (s = value; *s != '\0'; s = next) {
		const char *vmopt;
		const char *c;
		size_t len;

		/* Parse out next item, but skip empties */
		if ((c = strchr(s, ',')) == NULL)
			len = strlen(s);
		else
			len = c - s;
		for (next = s + len; *next == ','; next++);
		if (len == 0)
			continue;

		/* Find the named verbosity flag */
		for (i = 0; jc_verbose[i] != NULL; i++) {

			/* Check for match, possibly without 'X' prefix */
			vmopt = jc_verbose[i];
			if ((strncmp(s, vmopt, len) == 0 && vmopt[len] == '\0')
			    || (*vmopt == 'X' && strncmp(s, vmopt + 1, len) == 0
			      && vmopt[len + 1] == '\0'))
				break;
		}
		if (jc_verbose[i] == NULL) {
			jc_print(printer, stderr,
			    "jc: unknown verbose flag \"%.*s\"\n", len, s);
			return JNI_EVERSION;		/* kludge */
		}

		/* Add the corresponding VM flag */
		verbose_bits |= (1 << i);
	}

	/* None specified? */
	if (verbose_bits == 0)
		return JNI_OK;

	/* Create VM argument with the supplied verbose flags */
	for (len = i = 0; jc_verbose[i] != NULL; i++) {
		if ((verbose_bits & (1 << i)) != 0)
			len += 1 + strlen(jc_verbose[i]);
	}
	len += strlen("-verbose") + 1;
	if ((buf = alloca(len)) == NULL) {
		jc_print(printer, stderr,
		    "jc: %s: %s\n", "alloca", strerror(errno));
		return JNI_ERR;
	}
	sprintf(buf, "-verbose");
	for (first = JNI_TRUE, i = 0; jc_verbose[i] != NULL; i++) {
		if ((verbose_bits & (1 << i)) != 0) {
			sprintf(buf + strlen(buf), "%c%s",
			    first ? ':' : ',', jc_verbose[i]);
			first = JNI_FALSE;
		}
	}

	/* Add VM option flag */
	if (jc_add_vm_arg(args, printer, buf, NULL) != JNI_OK)
		return JNI_ERR;

	/* Done */
	return JNI_OK;
}

/*
 * Add a new property setting to the VM args.
 */
static int
jc_add_vm_property(JavaVMInitArgs *args, _jc_printer *printer,
        const char *name, size_t name_len, const char *value)
{
	char *option;

	if ((option = alloca(2 + name_len + 1 + strlen(value) + 1)) == NULL) {
		jc_print(printer, stderr,
		    "jc: %s: %s\n", "alloca", strerror(errno));
		return JNI_ERR;
	}
	strcpy(option, "-D");
	memcpy(option + 2, name, name_len);
	option[2 + name_len] = '=';
	strcpy(option + 2 + name_len + 1, value);
	return jc_add_vm_arg(args, printer, option, NULL);
}

/*
 * Add a new JavaVM argument.
 */
static int
jc_add_vm_arg(JavaVMInitArgs *args, _jc_printer *printer,
	const char *option, void *extraInfo)
{
	/* Extend array */
	if (jc_extend_array(printer, &args->options,
	    sizeof(*args->options), args->nOptions) != JNI_OK)
		return JNI_ERR;

	/* Fill in new option */
	memset(&args->options[args->nOptions],
	    0, sizeof(args->options[args->nOptions]));
	if ((args->options[args->nOptions].optionString
	    = strdup(option)) == NULL) {
		jc_print(printer, stderr,
		    "jc: %s: %s\n", "strdup", strerror(errno));
		return JNI_ERR;
	}
	args->options[args->nOptions].extraInfo = extraInfo;

	/* Done */
	args->nOptions++;
	return JNI_OK;
}

/*
 * Extend an array of somethings by one element.
 */
static int
jc_extend_array(_jc_printer printer, void *arrayp, size_t size, int length)
{
	void *array = *(void **)arrayp;

	/* Extend array */
	if ((array = realloc(array, (length + 1) * size)) == NULL) {
		jc_print(printer, stderr,
		    "jc: %s: %s\n", "realloc", strerror(errno));
		return JNI_ERR;
	}

	/* Done */
	*(void **)arrayp = array;
	return JNI_OK;
}

/*
 * Print something using the supplied printer and stream.
 */
static int
jc_print(_jc_printer *printer, FILE *stream, const char *fmt, ...)
{
	va_list args;
	int r;

	va_start(args, fmt);
	r = (*printer)(stream, fmt, args);
	va_end(args);
	return r;
}

/*
 * Insert a command line flag.
 */
static int
jc_insert_opt(_jc_printer printer, int *acp,
	char ***avp, int pos, const char *opt)
{
	/* Resize array */
	if (jc_extend_array(printer, avp, sizeof(**avp), *acp + 1) != JNI_OK)
		return JNI_ERR;

	/* Shift elements to make room */
	memmove(*avp + pos + 1, *avp + pos, (++(*acp) - pos) * sizeof(**avp));

	/* Initialize new element */
	(*avp)[pos] = strdup(opt);
	return JNI_OK;
}

