
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

#include "cfdump.h"

#define DEFAULT_CLASSPATH	".:" _JC_BOOT_CLASS_PATH

static void	dump_attr(_jc_env *env, int indent, _jc_classfile *cf,
			_jc_cf_attr *attrs, int num);
static void	dump_const(_jc_cf_constant *cp);
static void	dump_string(const char *s);
static void	dump_code(_jc_env *env, _jc_classfile *cfile,
			_jc_cf_bytecode *bytecode);
static void	usage(void) __attribute__ ((noreturn));

int
main(int ac, char **av)
{
	const char *classpath = DEFAULT_CLASSPATH;
	char *classname;
	_jc_classbytes *cb;
	_jc_classfile *cf;
	jboolean deps = JNI_FALSE;
	int flags = 0;
	_jc_env *env;
	_jc_jvm *vm;
	int i;

	/* Initialize */
	env = _jc_support_init();
	vm = env->vm;

	/* Parse command line */
	for (av++, ac--; ac > 0 && **av == '-'; av++, ac--) {
		if (strcmp(av[0], "-d") == 0)
			deps = JNI_TRUE;
		else if (strcmp(*av, "-e") == 0)
			flags |= DUMP_ENCODE_NAMES;
		else if (strcmp(*av, "-c") == 0)
			flags |= DUMP_TRANS_CLOSURE;
		else if (strcmp(*av, "-cp") == 0
		    || strcmp(*av, "-classpath") == 0) {
			av++, ac--;
			if (ac == 0)
				usage();
			classpath = *av;
		} else
			usage();
	}
	if (ac != 1)
		usage();

	/* Parse classpath */
	if (_jc_parse_classpath(env, classpath,
	    &vm->boot.class_path, &vm->boot.class_path_len) != JNI_OK)
		errx(1, "%s: %s", _jc_vmex_names[env->ex.num], env->ex.msg);

	/* Get class name */
	classname = av[0];
	for (i = 0; classname[i] != '\0'; i++) {
		if (classname[i] == '.')
			classname[i] = '/';
	}

	/* Read in classfile */
	if ((cb = _jc_bootcl_find_classbytes(env, classname, NULL)) == NULL) {
		errx(1, "can't load class `%s': %s: %s", classname,
		    _jc_vmex_names[env->ex.num], env->ex.msg);
	}

	/* Parse classfile */
	if ((cf = _jc_parse_classfile(env, cb, 2)) == NULL) {
		errx(1, "can't parse class `%s': %s: %s", classname,
		    _jc_vmex_names[env->ex.num], env->ex.msg);
	}
	_jc_free_classbytes(&cb);

	/* Just dump dependencies? */
	if (deps) {
		do_deps(env, cf, flags);
		goto done;
	}

	/* Dump parsed classfile */
	printf("version=\t%d.%d\n", cf->major_version, cf->minor_version);
	printf("num_constants=\t%d\n", cf->num_constants);
	for (i = 1; i < cf->num_constants; i++) {
		_jc_cf_constant *const cp = &cf->constants[i - 1];

		if (cp->type == 0)
			continue;
		printf("[%2d]\t", i);
		dump_const(cp);
		printf("\n");
	}
	printf("access_flags=\t0x%04x\n", cf->access_flags);
	printf("name=\t\t%s\n", cf->name);
	printf("superclass=\t%s\n", cf->superclass ? cf->superclass : "<NONE>");
	printf("num_interfaces=\t%d\n", cf->num_interfaces);
	for (i = 0; i < cf->num_interfaces; i++)
		printf("[%2d]\t%s\n", i, cf->interfaces[i]);
	printf("num_fields=\t%d\n", cf->num_fields);
	for (i = 0; i < cf->num_fields; i++) {
		_jc_cf_field *const field = &cf->fields[i];

		printf("[%2d]\tname=\t\t%s\n", i, field->name);
		printf("\tdescriptor=\t%s\n", field->descriptor);
		printf("\taccess_flags=\t0x%04x\n", field->access_flags);
		printf("\tnum_attributes=\t%d\n", field->num_attributes);
		dump_attr(env, 1, cf, field->attributes, field->num_attributes);
	}
	printf("num_methods=\t%d\n", cf->num_methods);
	for (i = 0; i < cf->num_methods; i++) {
		_jc_cf_method *const method = &cf->methods[i];

		printf("[%2d]\tname=\t\t%s\n", i, method->name);
		printf("\tdescriptor=\t%s\n", method->descriptor);
		printf("\taccess_flags=\t0x%04x\n", method->access_flags);
		printf("\tnum_attributes=\t%d\n", method->num_attributes);
		dump_attr(env, 1, cf, method->attributes,
		    method->num_attributes);
	}
	printf("num_attributes=\t%d\n", cf->num_attributes);
	dump_attr(env, 0, cf, cf->attributes, cf->num_attributes);

done:
	/* Clean up and exit */
	_jc_destroy_classfile(&cf);
	return 0;
}

static void
dump_attr(_jc_env *env, int indent, _jc_classfile *cf,
	_jc_cf_attr *attrs, int num)
{
	int i;

	for (i = 0; i < num; i++) {
		_jc_cf_attr *const attr = &attrs[i];

		if (indent)
			printf("\t");
		printf("[%2d]\t\%s: ", i, attr->name);
		if (strcmp(attr->name, "ConstantValue") == 0) {
			dump_const(attr->u.ConstantValue);
			printf("\n");
		} else if (strcmp(attr->name, "SourceFile") == 0)
			printf("\"%s\"\n", attr->u.SourceFile);
		else if (strcmp(attr->name, "Exceptions") == 0) {
			const int num = attr->u.Exceptions.num_exceptions;
			int j;

			for (j = 0; j < num; j++) {
				printf("%s%s", attr->u.Exceptions.exceptions[j],
				    j == num - 1 ? "\n" : ", ");
			}
		} else if (strcmp(attr->name, "InnerClasses") == 0) {
			const int num = attr->u.InnerClasses.num_classes;
			int j;

			printf("%d classes\n", num);
			for (j = 0; j < num; j++) {
				_jc_cf_inner_class *const inner
				    = &attr->u.InnerClasses.classes[j];

				printf("\t[%2d]\tInner: %s\n", j,
				    inner->inner != NULL ?
				      inner->inner : "none");
				printf("\t\tOuter: %s\n",
				    inner->outer != NULL ?
				      inner->outer : "none");
				printf("\t\t Name: %s\n",
				    inner->name != NULL ? inner->name : "none");
				printf("\t\tFlags: 0x%04x\n",
				    inner->access_flags);
			}
		} else if (strcmp(attr->name, "Code") == 0)
			dump_code(env, cf, &attr->u.Code);
		else
			printf("length %d\n", attr->length);
	}
}

static void
dump_const(_jc_cf_constant *cp)
{
	switch (cp->type) {
	case CONSTANT_Long:
		printf("0x%016llx", cp->u.Long);
		break;
	case CONSTANT_Float:
		printf("%g (0x%08x)", cp->u.Float, cp->u.Integer);
		break;
	case CONSTANT_Double:
		printf("%g (0x%016llx)", cp->u.Double, cp->u.Long);
		break;
	case CONSTANT_Integer:
		printf("0x%08x", cp->u.Integer);
		break;
	case CONSTANT_String:
		dump_string(cp->u.String);
		break;
	case CONSTANT_Class:
		printf("class \"%s\"", cp->u.Class);
		break;
	case CONSTANT_Fieldref:
		printf("class \"%s\" field \"%s\" descriptor \"%s\"",
		    cp->u.Ref.class, cp->u.Ref.name, cp->u.Ref.descriptor);
		break;
	case CONSTANT_Methodref:
		printf("class \"%s\" method \"%s\" descriptor \"%s\"",
		    cp->u.Ref.class, cp->u.Ref.name, cp->u.Ref.descriptor);
		break;
	case CONSTANT_InterfaceMethodref:
		printf("interface \"%s\" method \"%s\" descriptor \"%s\"",
		    cp->u.Ref.class, cp->u.Ref.name, cp->u.Ref.descriptor);
		break;
	case CONSTANT_NameAndType:
		printf("name \"%s\" descriptor \"%s\"",
		    cp->u.NameAndType.name, cp->u.NameAndType.descriptor);
		break;
	case CONSTANT_Utf8:
		dump_string(cp->u.Utf8);
		break;
	default:
		assert(0);
		break;
	}
}

static void
dump_string(const char *s)
{
	printf("\"");
	while (*s != '\0') {
		switch (*s) {
		case '"':
		case '\\':
			printf("\\%c", *s);
			break;
		case '\v':
			printf("\\v");
			break;
		case '\f':
			printf("\\f");
			break;
		case '\t':
			printf("\\t");
			break;
		case '\r':
			printf("\\r");
			break;
		case '\n':
			printf("\\n");
			break;
		default:
			if (isprint(*s)) {
				putchar(*s);
				break;
			}
			printf("\\x%02x", *s & 0xff);
			break;
		}
		s++;
	}
	printf("\"");
}

static void
dump_code(_jc_env *env, _jc_classfile *cf, _jc_cf_bytecode *bytecode)
{ 
	_jc_cf_code codemem;
	_jc_cf_code *const code = &codemem;
	int i;

	/* Parse bytecode */
	if (_jc_parse_code(env, cf, bytecode, code) != JNI_OK) {
		errx(1, "can't parse bytecode: %s: %s",
		    _jc_vmex_names[env->ex.num], env->ex.msg);
	}

	/* Dump parsed code */
	printf("\t\tmax_stack=%d max_locals=%d #instructions=%d\n",
	    code->max_stack, code->max_locals, code->num_insns);
	for (i = 0; i < code->num_insns; i++) {
		_jc_cf_insn *const insn = &code->insns[i];
		char nbuf[32];

		snprintf(nbuf, sizeof(nbuf), "[%d]", i);
		printf("\t%6s: %s", nbuf, _jc_bytecode_names[insn->opcode]);
		switch (insn->opcode) {
		case _JC_aload:
		case _JC_astore:
		case _JC_dload:
		case _JC_dstore:
		case _JC_fload:
		case _JC_fstore:
		case _JC_iload:
		case _JC_istore:
		case _JC_lload:
		case _JC_lstore:
		case _JC_ret:
			printf(" local%d", insn->u.local.index);
			break;
		case _JC_anewarray:
		case _JC_checkcast:
		case _JC_instanceof:
		case _JC_new:
			printf(" %s", insn->u.type.name);
			break;
		case _JC_bipush:
			printf(" %d", insn->u.immediate.value);
			break;
		case _JC_getfield:
		case _JC_getstatic:
		case _JC_putfield:
		case _JC_putstatic:
			printf(" %s.%s (%s)", insn->u.fieldref.field->class,
			    insn->u.fieldref.field->name,
			    insn->u.fieldref.field->descriptor);
			break;
		case _JC_goto:
		case _JC_if_acmpeq:
		case _JC_if_acmpne:
		case _JC_if_icmpeq:
		case _JC_if_icmpne:
		case _JC_if_icmplt:
		case _JC_if_icmpge:
		case _JC_if_icmpgt:
		case _JC_if_icmple:
		case _JC_ifeq:
		case _JC_ifne:
		case _JC_iflt:
		case _JC_ifge:
		case _JC_ifgt:
		case _JC_ifle:
		case _JC_ifnonnull:
		case _JC_ifnull:
		case _JC_jsr:
			printf(" [%d]", insn->u.branch.target);
			break;
		case _JC_iinc:
			printf(" local%d %d", insn->u.iinc.index,
			    insn->u.iinc.value);
			break;
		case _JC_invokeinterface:
		case _JC_invokespecial:
		case _JC_invokestatic:
		case _JC_invokevirtual:
			printf(" %s.%s%s", insn->u.invoke.method->class,
			    insn->u.invoke.method->name,
			    insn->u.invoke.method->descriptor);
			break;
		case _JC_ldc:
			printf(" ");
			dump_const(insn->u.constant);
			break;
		case _JC_ldc2_w:
			printf(" ");
			dump_const(insn->u.constant);
			break;
		case _JC_lookupswitch:
		    {
			_jc_cf_lookupswitch *const lsw = insn->u.lookupswitch;
			int j;

			printf(" default [%d]", lsw->default_target);
			for (j = 0; j < lsw->num_pairs; j++) {
				_jc_cf_lookup *const lookup = &lsw->pairs[j];

				printf("\n\t\t%11d [%d]",
				    lookup->match, lookup->target);
			}
			break;
		    }
		case _JC_tableswitch:
		    {
			_jc_cf_tableswitch *const tsw = insn->u.tableswitch;
			int j;

			printf(" default [%d]", tsw->default_target);
			for (j = 0; j < tsw->high - tsw->low + 1; j++) {
				printf("\n\t\t%11d [%d]",
				    tsw->low + j, tsw->targets[j]);
			}
			break;
		    }
		case _JC_multianewarray:
			printf(" %d dims %s", insn->u.multianewarray.dims,
			    insn->u.multianewarray.type);
			break;
		case _JC_newarray:
			printf(" %s[]", _jc_prim_names[insn->u.newarray.type]);
			break;
		case _JC_sipush:
			printf(" %d", insn->u.immediate.value);
			break;
		default:
			_JC_ASSERT(_jc_bytecode_names[insn->opcode] != NULL);
			break;
		}
		printf("\n");
	}

	/* Dump traps */
	if (code->num_traps > 0) {
		printf("\t%d traps:\n", code->num_traps);
		for (i = 0; i < code->num_traps; i++) {
			_jc_cf_trap *const trap = &code->traps[i];
			char bufs[3][32];

			snprintf(bufs[0], sizeof(bufs[0]),
			    "[%d]", trap->start);
			snprintf(bufs[1], sizeof(bufs[1]),
			    "[%d]", trap->end);
			snprintf(bufs[2], sizeof(bufs[2]),
			    "[%d]", trap->target);
			printf("\t\t%-6s.. %-6s -> %-6s %s\n",
			    bufs[0], bufs[1], bufs[2],
			    trap->type != NULL ? trap->type : "");
		}
	}

	/* Dump line number table */
	if (code->num_linemaps > 0) {
		printf("\t%d line number entries:\n", code->num_linemaps);
		for (i = 0; i < code->num_linemaps; i++) {
			_jc_cf_linemap *const linemap = &code->linemaps[i];
			char buf[32];

			snprintf(buf, sizeof(buf), "[%d]", linemap->index);
			printf("\t\t%-6s line %d\n", buf, linemap->line);
		}
	}

	/* Free parsed code */
	_jc_destroy_code(code);
}

static void
usage(void)
{
	fprintf(stderr, "Usage: cfdump [-d] [-e] [-c]"
	    " [-classpath path] class-name\n");
	fprintf(stderr, "Options:\n"
	    "    -classpath\tSpecify search path for class files\n"
	    "    -cp\t\tSame as -classpath\n"
	    "    -d\t\tDump class, superclass & superinterface dependencies\n"
	    "    -c\t\tDo transitive closure of ``-d'' dependencies\n"
	    "    -e\t\tEncode class names suitable for filenames\n");
	exit(1);
}

