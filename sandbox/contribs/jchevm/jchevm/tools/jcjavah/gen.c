
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

#include "javah.h"

struct flag_name {
	_jc_uint16	flag;
	const char	*name;
};

struct jni_name {
	const char	*name;
	const char	*ctype;
};

static const struct flag_name flag_names[] = {
	{ _JC_ACC_PUBLIC,	"public" },
	{ _JC_ACC_PROTECTED,	"protected" },
	{ _JC_ACC_PRIVATE,	"private" },
	{ _JC_ACC_ABSTRACT,	"abstract" },
	{ _JC_ACC_STATIC,	"static" },
	{ _JC_ACC_FINAL,	"final" },
	{ _JC_ACC_TRANSIENT,	"transient" },
	{ _JC_ACC_VOLATILE,	"volatile" },
	{ _JC_ACC_SYNCHRONIZED,	"synchronized" },
	{ _JC_ACC_NATIVE,	"native" },
	{ _JC_ACC_STRICT,	"strictfp" },
	{ 0, NULL }
};

static const struct jni_name jni_names[] = {
	{ "Ljava/lang/Class;",			"jclass" },
	{ "Ljava/lang/String;",			"jstring" },
	{ "Ljava/lang/Throwable;",		"jthrowable" },
	{ "Ljava/lang/ref/WeakReference;",	"jweak" },
	{ NULL, NULL }
};

static const char hexdig[] = "0123456789abcdef";

static const	char *flagstr(int flags, jboolean with_class);
static const	char *jtypestr(const char **sp);
static const	char *ctypestr(const char **sp, jboolean jni, int pnum);
static void	print_comment(FILE *fp, _jc_cf_method *method);
static void	print_export(FILE *fp, _jc_cf_method *method,
			jboolean jni, jboolean hdr);
static void	print_name(FILE *fp, _jc_classfile *cfile,
			_jc_cf_method *method, jboolean jni);
static void	print_decl(FILE *fp, _jc_cf_method *method,
			jboolean jni, jboolean with_names);
static void	encode(const char *s, char *buf, size_t bmax);

void
javah_header(_jc_classfile *cfile, const char *dir, jboolean jni)
{
	char pathname[MAXPATHLEN];
	char namebuf[MAXPATHLEN];
	char *filename;
	int ivs = 0;
	FILE *fp;
	int i;

	/* Open file */
	encode(cfile->name, namebuf, sizeof(namebuf));
	snprintf(pathname, sizeof(pathname), "%s/%s.h", dir, namebuf);
	filename = pathname + strlen(dir) + 1;
	if ((fp = fopen(pathname, "w")) == NULL)
		err(1, "%s", pathname);

	/* Do header */
	fprintf(fp, "// generated file -- do not edit\n");
	fprintf(fp, "// %s %s\n",
	    flagstr(cfile->access_flags, JNI_TRUE), cfile->name);
	fprintf(fp, "\n");
	filename[strlen(filename) - 2] = '_';
	fprintf(fp, "#ifndef _Included_%s\n", filename);
	fprintf(fp, "#define _Included_%s\n", filename);
	fprintf(fp, "\n");
	fprintf(fp, "#include <%s.h>\n", jni ? "jni" : "jc_defs");
	fprintf(fp, "\n");
	fprintf(fp, "#ifdef __cplusplus\n");
	fprintf(fp, "extern \"C\" {\n");
	fprintf(fp, "#endif\n");
	fprintf(fp, "\n");

	/* Spit out static final fields */
	for (i = 0; i < cfile->num_fields; i++) {
		_jc_cf_field *const field = &cfile->fields[i];
		const u_char ptype = _jc_sig_types[(u_char)*field->descriptor];
		_jc_cf_constant *value = field->initial_value;
		char buf[256];

		if (!_JC_ACC_TEST(field, STATIC)
		    || ptype == _JC_TYPE_REFERENCE
		    || value == NULL)
			continue;
		encode(field->name, buf, sizeof(buf));
		fprintf(fp, "#define %s_%s ", namebuf, buf);
		switch (ptype) {
		case _JC_TYPE_BOOLEAN:
		case _JC_TYPE_BYTE:
		case _JC_TYPE_CHAR:
		case _JC_TYPE_SHORT:
		case _JC_TYPE_INT:
			fprintf(fp, "%d", value->u.Integer);
			break;
		case _JC_TYPE_LONG:
			fprintf(fp, "%ld", (long)value->u.Long);
			break;
		case _JC_TYPE_FLOAT:
			fprintf(fp, "%g", (double)value->u.Float);
			break;
		case _JC_TYPE_DOUBLE:
			fprintf(fp, "%g", (double)value->u.Double);
			break;
		default:
			assert(0);
		}
		fprintf(fp, "\n");
		ivs = 1;
	}
	if (ivs)
		printf("\n");

	/* Spit out native method declarations */
	for (i = 0; i < cfile->num_methods; i++) {
		_jc_cf_method *const method = &cfile->methods[i];

		if ((method->access_flags & _JC_ACC_NATIVE) == 0)
			continue;
		print_comment(fp, method);
		print_export(fp, method, jni, JNI_TRUE);
		fprintf(fp, " ");
		print_name(fp, cfile, method, jni);
		fprintf(fp, "\n  ");
		print_decl(fp, method, jni, JNI_FALSE);
		if (!jni)
			fprintf(fp, " _JC_JCNI_ATTR");
		fprintf(fp, ";\n\n");
	}
	fprintf(fp, "#ifdef __cplusplus\n");
	fprintf(fp, "}\n");
	fprintf(fp, "#endif\n");
	fprintf(fp, "\n");
	fprintf(fp, "#endif\t/* _Included_%s */\n", filename);
	fprintf(fp, "\n");
}

void
javah_source(_jc_classfile *cfile, const char *dir, jboolean jni)
{
	char pathname[MAXPATHLEN];
	char *filename;
	char *s;
	FILE *fp;
	int i;

	/* Open file */
	snprintf(pathname, sizeof(pathname), "%s/%s.c", dir, cfile->name);
	filename = pathname + strlen(dir) + 1;
	for (s = filename; *s != '\0'; s++) {
		if (*s == '/')
			*s = '_';
	}
	if ((fp = fopen(pathname, "w")) == NULL)
		err(1, "%s", pathname);

	/* Do header */
	fprintf(fp, "// generated file -- please edit\n");
	fprintf(fp, "// %s %s\n",
	    flagstr(cfile->access_flags, JNI_TRUE), cfile->name);
	fprintf(fp, "\n");
	filename[strlen(filename) - 1] = 'h';
	fprintf(fp, "#include %s\n", jni ? "<jni.h>" : "\"libjc.h\"");
	fprintf(fp, "#include \"%s\"\n", filename);
	fprintf(fp, "\n");

	/* Spit out native method declarations */
	for (i = 0; i < cfile->num_methods; i++) {
		_jc_cf_method *const method = &cfile->methods[i];

		if ((method->access_flags & _JC_ACC_NATIVE) == 0)
			continue;
		print_comment(fp, method);
		print_export(fp, method, jni, JNI_FALSE);
		fprintf(fp, "\n");
		print_name(fp, cfile, method, jni);
		print_decl(fp, method, jni, JNI_TRUE);
		fprintf(fp, "\n{\n}\n\n");
	}
}

static void
print_decl(FILE *fp, _jc_cf_method *method, jboolean jni, jboolean with_names)
{
	const char *parm = strchr(method->descriptor, '(') + 1;
	const char *s;
	int i;

	if (jni)
		fprintf(fp, "(JNIEnv *%s", with_names ? "jenv" : "");	/*)*/
	else
		fprintf(fp, "(_jc_env *%s", with_names ? "env" : "");	/*)*/
	if ((method->access_flags & _JC_ACC_STATIC) != 0) {
		if (jni) {
			fprintf(fp, ", jclass");
			if (with_names)
				fprintf(fp, " clazz");
		}
	} else {
		fprintf(fp, ", %s", jni ? "jobject" : "_jc_object *");
		if (with_names)
			fprintf(fp, " this");
	}
	for (i = 0, s = parm; *s != /*(*/ ')'; i++)
		fprintf(fp, ", %s", ctypestr(&s, jni, with_names ? i + 1 : -1));
	fprintf(fp, /*(*/ ")");
}

static void
print_name(FILE *fp, _jc_classfile *cfile, _jc_cf_method *method, jboolean jni)
{
	char *buf;
	char *s;

	fprintf(fp, "%s_", jni ? "Java" : "JCNI");
	if ((buf = alloca(_jc_jni_encode_length(cfile->name) + 1
	    + _jc_jni_encode_length(method->name) + 1)) == NULL)
		err(1, "alloca");
	s = buf;
	_jc_jni_encode(&s, cfile->name);
	_jc_jni_encode(&s, "/");
	_jc_jni_encode(&s, method->name);
	*s = '\0';
	fprintf(fp, "%s", buf);
}

static void
print_export(FILE *fp, _jc_cf_method *method, jboolean jni, jboolean hdr)
{
	const char *ret = strchr(method->descriptor, /*(*/ ')') + 1;

	if (jni)
		fprintf(fp, "JNIEXPORT ");
	else if (hdr)
		fprintf(fp, "extern ");
	fprintf(fp, "%s", ctypestr(&ret, jni, -1));
	if (jni)
		fprintf(fp, " JNICALL");
}

static void
print_comment(FILE *fp, _jc_cf_method *method)
{
	const char *parm = strchr(method->descriptor, '(') + 1;
	const char *ret = strchr(method->descriptor, ')') + 1;
	_jc_cf_attr *eattr = NULL;
	const char *s;
	int i;

	fprintf(fp, "/*\n");
	fprintf(fp, " * %s %s %s(", /*)*/
	    flagstr(method->access_flags, JNI_FALSE),
	    jtypestr(&ret), method->name);
	for (s = parm; *s != ')'; ) {
		fprintf(fp, "%s", jtypestr(&s));
		if (*s != /*(*/ ')')
			fprintf(fp, ", ");
	}
	fprintf(fp, ")\n");
	for (i = 0; i < method->num_attributes; i++) {
		_jc_cf_attr *const attr = &method->attributes[i];

		if (strcmp(attr->name, "Exceptions") == 0) {
			eattr = attr;
			break;
		}
	}
	if (eattr != NULL) {
		const int num = eattr->u.Exceptions.num_exceptions;
		int j;

		fprintf(fp, " *\tthrows ");
		for (j = 0; j < num; j++) {
			fprintf(fp, "%s%s", eattr->u.Exceptions.exceptions[j],
			    j == num - 1 ? "\n" : ", ");
		}
	}
	fprintf(fp, " */\n");
}

static const char *
ctypestr(const char **sp, jboolean jni, int pnum)
{
	static char buf[1024];
	const char *s = *sp;
	jboolean star = JNI_FALSE;
	int dims = 0;
	u_char type;

	if ((type = _jc_sig_types[(u_char)*s]) != _JC_TYPE_REFERENCE) {
		snprintf(buf, sizeof(buf), "%s%s",
		    (type == _JC_TYPE_VOID) ? "" : "j", _jc_prim_names[type]);
		s++;
		goto done;
	}
	while (s[dims] == '[')
		dims++;
	s += dims;
	if ((type = _jc_sig_types[(u_char)*s]) == _JC_TYPE_INVALID)
		errx(1, "bogus type \"%s\"", *sp);
	if (type != _JC_TYPE_REFERENCE && dims == 1) {
		if (jni) {
			snprintf(buf, sizeof(buf),
			    "j%sArray", _jc_prim_names[type]);
		} else {
			snprintf(buf, sizeof(buf),
			    "_jc_%s_array *", _jc_prim_names[type]);
			star = JNI_TRUE;
		}
		s++;
	} else if (dims > 0) {
		if (jni)
			snprintf(buf, sizeof(buf), "jobjectArray");
		else {
			snprintf(buf, sizeof(buf), "_jc_object_array *");
			star = JNI_TRUE;
		}
		while (*s != ';')
			s++;
		s++;
	} else {
		const char *semi = strchr(s, ';');
		size_t len;

		if (semi == NULL)
			errx(1, "bogus type \"%s\"", *sp);
		len = (semi + 1) - s;
		if (jni) {
			int j;

			/* Check for specially named classes */
			for (j = 0; jni_names[j].name != NULL; j++) {
				if (strncmp(s, jni_names[j].name, len) == 0) {
					snprintf(buf, sizeof(buf),
					    "%s", jni_names[j].ctype);
					goto objdone;
				}
			}
		}
		snprintf(buf, sizeof(buf), jni ? "jobject" : "_jc_object *");
		star = !jni;
	objdone:
		s += len;
	}
done:
	if (pnum != -1) {
		snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf),
		    "%sparam%d", star ? "" : " ", pnum);
	}
	*sp = s;
	return buf;
}

static const char *
jtypestr(const char **sp)
{
	static char buf[1024];
	const char *s = *sp;
	int dims = 0;
	u_char type;

again:
	type = _jc_sig_types[(u_char)*s];
	if (type == _JC_TYPE_INVALID)
		goto bogus;
	if (type != _JC_TYPE_REFERENCE) {
		snprintf(buf, sizeof(buf), "%s", _jc_prim_names[type]);
		s++;
	} else if (*s == 'L') {
		const char *t = strchr(s, ';');
		size_t len;

		if (t == NULL)
			goto bogus;
		for (len = 0; t > s && t[-1] != '/'; t--, len++);
		if (len == 0)
			goto bogus;
		memcpy(buf, t, len);
		buf[len] = '\0';
		s = t + len + 1;
	} else if (*s == '[') {
		while (s[dims] == '[')
			dims++;
		s += dims;
		goto again;
	} else
bogus:		errx(1, "invalid type \"%s\"", *sp);
	while (dims-- > 0)
		snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf), "[]");
	*sp = s;
	return buf;
}

static const char *
flagstr(int flags, jboolean with_class)
{
	static char buf[128];
	int i;

	*buf = '\0';
	for (i = 0; flag_names[i].flag != 0; i++) {
		if ((flags & flag_names[i].flag) != 0) {
			if (with_class && flag_names[i].flag == _JC_ACC_SUPER)
				continue;
			if (*buf != '\0') {
				snprintf(buf + strlen(buf),
				    sizeof(buf) - strlen(buf), " ");
			}
			snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf),
			    "%s", flag_names[i].name);
		}
	}
	if (with_class) {
		if (*buf != '\0') {
			snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf),
			    " ");
		}
		snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf),
		    (flags & _JC_ACC_INTERFACE) != 0 ? "interface" : "class");
	}
	return buf;
}

static void
encode(const char *s, char *buf, size_t bmax)
{
	char ch;
	int i;

	for (i = 0; i < bmax - 3 && (ch = s[i]) != '\0'; i++) {
		if (ch == '/' || ch == '_') {
			*buf++ = '_';
			continue;
		}
		if (isalnum(ch)) {
			*buf++ = ch;
			continue;
		}
		*buf++ = '_';
		*buf++ = '_';
		*buf++ = hexdig[(ch >> 4) & 0x0f];
		*buf++ = hexdig[ch & 0x0f];
	}
	*buf++ = '\0';
}

