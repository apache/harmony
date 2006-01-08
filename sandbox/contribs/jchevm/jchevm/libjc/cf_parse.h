
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

#ifndef _CF_PARSE_H_
#define _CF_PARSE_H_

/************************************************************************
 *			Class file structures				*
 ************************************************************************/

/*
 * Forward structure declarations and typedef's
 */
typedef struct _jc_cf_anewarray _jc_cf_anewarray;
typedef struct _jc_cf_attr _jc_cf_attr;
typedef struct _jc_cf_branch _jc_cf_branch;
typedef struct _jc_cf_bytecode _jc_cf_bytecode;
typedef struct _jc_cf_code _jc_cf_code;
typedef struct _jc_cf_constant _jc_cf_constant;
typedef struct _jc_cf_exceptions _jc_cf_exceptions;
typedef struct _jc_cf_field _jc_cf_field;
typedef struct _jc_cf_fieldref _jc_cf_fieldref;
typedef struct _jc_cf_iinc _jc_cf_iinc;
typedef struct _jc_cf_immediate _jc_cf_immediate;
typedef struct _jc_cf_inner_class _jc_cf_inner_class;
typedef struct _jc_cf_inner_classes _jc_cf_inner_classes;
typedef struct _jc_cf_insn _jc_cf_insn;
typedef struct _jc_cf_invoke _jc_cf_invoke;
typedef struct _jc_cf_linemap _jc_cf_linemap;
typedef struct _jc_cf_linenum _jc_cf_linenum;
typedef struct _jc_cf_linenums _jc_cf_linenums;
typedef struct _jc_cf_local _jc_cf_local;
typedef struct _jc_cf_lookup _jc_cf_lookup;
typedef struct _jc_cf_lookupswitch _jc_cf_lookupswitch;
typedef struct _jc_cf_method _jc_cf_method;
typedef struct _jc_cf_multianewarray _jc_cf_multianewarray;
typedef struct _jc_cf_name_type _jc_cf_name_type;
typedef struct _jc_cf_new _jc_cf_new;
typedef struct _jc_cf_newarray _jc_cf_newarray;
typedef struct _jc_cf_parse_state _jc_cf_parse_state;
typedef struct _jc_cf_ref _jc_cf_ref;
typedef struct _jc_cf_switch _jc_cf_switch;
typedef struct _jc_cf_tableswitch _jc_cf_tableswitch;
typedef struct _jc_cf_trap _jc_cf_trap;
typedef struct _jc_cf_type _jc_cf_type;

/************************************************************************
 *			Bytecode information				*
 ************************************************************************/

/*
 * Bytecode definitions
 */
enum {
	_JC_aaload		=0x32,
	_JC_aastore		=0x53,
	_JC_aconst_null		=0x01,
	_JC_aload		=0x19,
	_JC_aload_0		=0x2a,
	_JC_aload_1		=0x2b,
	_JC_aload_2		=0x2c,
	_JC_aload_3		=0x2d,
	_JC_anewarray		=0xbd,
	_JC_areturn		=0xb0,
	_JC_arraylength		=0xbe,
	_JC_astore		=0x3a,
	_JC_astore_0		=0x4b,
	_JC_astore_1		=0x4c,
	_JC_astore_2		=0x4d,
	_JC_astore_3		=0x4e,
	_JC_athrow		=0xbf,
	_JC_baload		=0x33,
	_JC_bastore		=0x54,
	_JC_bipush		=0x10,
	_JC_caload		=0x34,
	_JC_castore		=0x55,
	_JC_checkcast		=0xc0,
	_JC_d2f			=0x90,
	_JC_d2i			=0x8e,
	_JC_d2l			=0x8f,
	_JC_dadd		=0x63,
	_JC_daload		=0x31,
	_JC_dastore		=0x52,
	_JC_dcmpg		=0x98,
	_JC_dcmpl		=0x97,
	_JC_dconst_0		=0x0e,
	_JC_dconst_1		=0x0f,
	_JC_ddiv		=0x6f,
	_JC_dload		=0x18,
	_JC_dload_0		=0x26,
	_JC_dload_1		=0x27,
	_JC_dload_2		=0x28,
	_JC_dload_3		=0x29,
	_JC_dmul		=0x6b,
	_JC_dneg		=0x77,
	_JC_drem		=0x73,
	_JC_dreturn		=0xaf,
	_JC_dstore		=0x39,
	_JC_dstore_0		=0x47,
	_JC_dstore_1		=0x48,
	_JC_dstore_2		=0x49,
	_JC_dstore_3		=0x4a,
	_JC_dsub		=0x67,
	_JC_dup			=0x59,
	_JC_dup_x1		=0x5a,
	_JC_dup_x2		=0x5b,
	_JC_dup2		=0x5c,
	_JC_dup2_x1		=0x5d,
	_JC_dup2_x2		=0x5e,
	_JC_f2d			=0x8d,
	_JC_f2i			=0x8b,
	_JC_f2l			=0x8c,
	_JC_fadd		=0x62,
	_JC_faload		=0x30,
	_JC_fastore		=0x51,
	_JC_fcmpg		=0x96,
	_JC_fcmpl		=0x95,
	_JC_fconst_0		=0x0b,
	_JC_fconst_1		=0x0c,
	_JC_fconst_2		=0x0d,
	_JC_fdiv		=0x6e,
	_JC_fload		=0x17,
	_JC_fload_0		=0x22,
	_JC_fload_1		=0x23,
	_JC_fload_2		=0x24,
	_JC_fload_3		=0x25,
	_JC_fmul		=0x6a,
	_JC_fneg		=0x76,
	_JC_frem		=0x72,
	_JC_freturn		=0xae,
	_JC_fstore		=0x38,
	_JC_fstore_0		=0x43,
	_JC_fstore_1		=0x44,
	_JC_fstore_2		=0x45,
	_JC_fstore_3		=0x46,
	_JC_fsub		=0x66,
	_JC_getfield		=0xb4,
	_JC_getstatic		=0xb2,
	_JC_goto		=0xa7,
	_JC_goto_w		=0xc8,
	_JC_i2b			=0x91,
	_JC_i2c			=0x92,
	_JC_i2d			=0x87,
	_JC_i2f			=0x86,
	_JC_i2l			=0x85,
	_JC_i2s			=0x93,
	_JC_iadd		=0x60,
	_JC_iaload		=0x2e,
	_JC_iand		=0x7e,
	_JC_iastore		=0x4f,
	_JC_iconst_m1		=0x02,
	_JC_iconst_0		=0x03,
	_JC_iconst_1		=0x04,
	_JC_iconst_2		=0x05,
	_JC_iconst_3		=0x06,
	_JC_iconst_4		=0x07,
	_JC_iconst_5		=0x08,
	_JC_idiv		=0x6c,
	_JC_if_acmpeq		=0xa5,
	_JC_if_acmpne		=0xa6,
	_JC_if_icmpeq		=0x9f,
	_JC_if_icmpne		=0xa0,
	_JC_if_icmplt		=0xa1,
	_JC_if_icmpge		=0xa2,
	_JC_if_icmpgt		=0xa3,
	_JC_if_icmple		=0xa4,
	_JC_ifeq		=0x99,
	_JC_ifne		=0x9a,
	_JC_iflt		=0x9b,
	_JC_ifge		=0x9c,
	_JC_ifgt		=0x9d,
	_JC_ifle		=0x9e,
	_JC_ifnonnull		=0xc7,
	_JC_ifnull		=0xc6,
	_JC_iinc		=0x84,
	_JC_iload		=0x15,
	_JC_iload_0		=0x1a,
	_JC_iload_1		=0x1b,
	_JC_iload_2		=0x1c,
	_JC_iload_3		=0x1d,
	_JC_imul		=0x68,
	_JC_ineg		=0x74,
	_JC_instanceof		=0xc1,
	_JC_invokeinterface	=0xb9,
	_JC_invokespecial	=0xb7,
	_JC_invokestatic	=0xb8,
	_JC_invokevirtual	=0xb6,
	_JC_ior			=0x80,
	_JC_irem		=0x70,
	_JC_ireturn		=0xac,
	_JC_ishl		=0x78,
	_JC_ishr		=0x7a,
	_JC_istore		=0x36,
	_JC_istore_0		=0x3b,
	_JC_istore_1		=0x3c,
	_JC_istore_2		=0x3d,
	_JC_istore_3		=0x3e,
	_JC_isub		=0x64,
	_JC_iushr		=0x7c,
	_JC_ixor		=0x82,
	_JC_jsr			=0xa8,
	_JC_jsr_w		=0xc9,
	_JC_l2d			=0x8a,
	_JC_l2f			=0x89,
	_JC_l2i			=0x88,
	_JC_ladd		=0x61,
	_JC_laload		=0x2f,
	_JC_land		=0x7f,
	_JC_lastore		=0x50,
	_JC_lcmp		=0x94,
	_JC_lconst_0		=0x09,
	_JC_lconst_1		=0x0a,
	_JC_ldc			=0x12,
	_JC_ldc_w		=0x13,
	_JC_ldc2_w		=0x14,
	_JC_ldiv		=0x6d,
	_JC_lload		=0x16,
	_JC_lload_0		=0x1e,
	_JC_lload_1		=0x1f,
	_JC_lload_2		=0x20,
	_JC_lload_3		=0x21,
	_JC_lmul		=0x69,
	_JC_lneg		=0x75,
	_JC_lookupswitch	=0xab,
	_JC_lor			=0x81,
	_JC_lrem		=0x71,
	_JC_lreturn		=0xad,
	_JC_lshl		=0x79,
	_JC_lshr		=0x7b,
	_JC_lstore		=0x37,
	_JC_lstore_0		=0x3f,
	_JC_lstore_1		=0x40,
	_JC_lstore_2		=0x41,
	_JC_lstore_3		=0x42,
	_JC_lsub		=0x65,
	_JC_lushr		=0x7d,
	_JC_lxor		=0x83,
	_JC_monitorenter	=0xc2,
	_JC_monitorexit		=0xc3,
	_JC_multianewarray	=0xc5,
	_JC_new			=0xbb,
	_JC_newarray		=0xbc,
	_JC_nop			=0x00,
	_JC_pop			=0x57,
	_JC_pop2		=0x58,
	_JC_putfield		=0xb5,
	_JC_putstatic		=0xb3,
	_JC_ret			=0xa9,
	_JC_return		=0xb1,
	_JC_saload		=0x35,
	_JC_sastore		=0x56,
	_JC_sipush		=0x11,
	_JC_swap		=0x5f,
	_JC_tableswitch		=0xaa,
	_JC_wide		=0xc4,
};

/*
 * Primitive type definitions used by 'newarray'
 */
enum {
	_JC_boolean		=4,
	_JC_char		=5,
	_JC_float		=6,
	_JC_double		=7,
	_JC_byte		=8,
	_JC_short		=9,
	_JC_int			=10,
	_JC_long		=11,
};

/*
 * Class file constant pool tags.
 */
enum {
	CONSTANT_Utf8			=1,
	CONSTANT_Integer		=3,
	CONSTANT_Float			=4,
	CONSTANT_Long			=5,
	CONSTANT_Double			=6,
	CONSTANT_Class			=7,
	CONSTANT_String			=8,
	CONSTANT_Fieldref		=9,
	CONSTANT_Methodref		=10,
	CONSTANT_InterfaceMethodref	=11,
	CONSTANT_NameAndType		=12,
};

/*
 * Extra information associated with certain bytecodes. The "target"
 * fields are indexes into the _jc_cf_insn array, not bytecode offsets.
 */
struct _jc_cf_fieldref {
	_jc_cf_ref	*field;
};
struct _jc_cf_iinc {
	_jc_uint16	index;
	jshort		value;
};
struct _jc_cf_invoke {
	_jc_cf_ref	*method;
};
struct _jc_cf_lookup {
	jint		match;
	_jc_uint16	target;
};
struct _jc_cf_lookupswitch {
	_jc_uint16	default_target;
	jint		num_pairs;
	_jc_cf_lookup	pairs[0];
};
struct _jc_cf_multianewarray {
	const char	*type;
	u_char		dims;
};
struct _jc_cf_newarray {
	u_char		type;		/* _JC_TYPE_XXX */
};
struct _jc_cf_tableswitch {
	jint		low;
	jint		high;
	_jc_uint16	default_target;
	_jc_uint16	targets[0];
};
struct _jc_cf_branch {
	_jc_uint16	target;
};
struct _jc_cf_local {
	_jc_uint16	index;
};
struct _jc_cf_immediate {
	jint		value;
};
struct _jc_cf_type {
	const char	*name;
};

/* Parsed instruction information */
struct _jc_cf_insn {
	union {
		_jc_cf_fieldref		fieldref;
		_jc_cf_iinc		iinc;
		_jc_cf_invoke		invoke;
		_jc_cf_multianewarray	multianewarray;
		_jc_cf_newarray		newarray;
		_jc_cf_lookupswitch	*lookupswitch;
		_jc_cf_tableswitch	*tableswitch;
		_jc_cf_branch		branch;
		_jc_cf_local		local;
		_jc_cf_immediate	immediate;
		_jc_cf_type		type;
		_jc_cf_constant		*constant;
	}		u;
	u_char		opcode;
};

/* LineNumberTable attribute */
struct _jc_cf_linenums {
	_jc_uint16	length;
	_jc_cf_linenum	*linenums;
};

struct _jc_cf_linenum {
	_jc_uint16	offset;			/* bytecode offset */
	_jc_uint16	line;
};

struct _jc_cf_linemap {
	_jc_uint16	index;			/* instruction index */
	_jc_uint16	line;
};

/* Parsed method trap */
struct _jc_cf_trap {
	_jc_uint16	start;			/* instruction index */
	_jc_uint16	end;			/* instruction index */
	_jc_uint16	target;			/* instruction index */
	const char	*type;
};

/* Unparsed method bytecode */
struct _jc_cf_bytecode {
	u_char		*bytecode;
	size_t		length;
	_jc_cf_linenums	*linenums;
};

/* Parsed method bytecode */
struct _jc_cf_code {
	_jc_uint16	max_stack;
	_jc_uint16	max_locals;
	_jc_uint16	num_traps;
	_jc_uint16	num_linemaps;
	_jc_uint16	num_insns;
	_jc_cf_insn	*insns;
	_jc_cf_trap	*traps;
	_jc_cf_linemap	*linemaps;
};

/************************************************************************
 *			Non-bytecode information			*
 ************************************************************************/

/* Fieldref, Methodref, or InterfaceMethodref constant */
struct _jc_cf_ref {
	const char	*class;
	const char	*name;
	const char	*descriptor;
};

/* NameAndType constant */
struct _jc_cf_name_type {
	const char	*name;
	const char	*descriptor;
};

/* Constant pool entry */
struct _jc_cf_constant {
	u_char		type;
	union {
	    const char		*Class;
	    _jc_cf_ref		Ref;
	    _jc_cf_name_type	NameAndType;
	    const char		*String;
	    jint		Integer;
	    jlong		Long;
	    jfloat		Float;
	    jdouble		Double;
	    const char		*Utf8;
	}		u;
};

/* Field info */
struct _jc_cf_field {
	_jc_uint16	access_flags;
	const char	*name;
	const char	*descriptor;
	_jc_uint16	num_attributes;
	_jc_cf_attr	*attributes;
	_jc_cf_constant	*initial_value;
};

/* Method info */
struct _jc_cf_method {
	_jc_uint16		access_flags;
	const char		*name;
	const char		*descriptor;
	_jc_uint16		num_attributes;
	_jc_cf_attr		*attributes;
	_jc_cf_exceptions	*exceptions;
	_jc_cf_bytecode		*code;
};

/* Exceptions attribute */
struct _jc_cf_exceptions {
	_jc_uint16	num_exceptions;
	const char	**exceptions;
};

/* InnerClasses attribute */
struct _jc_cf_inner_class {
	const char	*inner;
	const char	*outer;
	const char	*name;
	_jc_uint16	access_flags;
};

struct _jc_cf_inner_classes {
	_jc_uint16		num_classes;
	_jc_cf_inner_class	*classes;
};

/* Attribute (only some are explicitly supported) */
struct _jc_cf_attr {
	const char	*name;
	_jc_uint32	length;
	const u_char	*bytes;
	union {
	    _jc_cf_constant		*ConstantValue;
	    const char			*SourceFile;
	    _jc_cf_exceptions		Exceptions;
	    _jc_cf_inner_classes	InnerClasses;
	    _jc_cf_bytecode		Code;
	    _jc_cf_linenums		LineNumberTable;
	}		u;
};

/* Parsed classfile */
struct _jc_classfile {
	_jc_uint16		minor_version;
	_jc_uint16		major_version;
	_jc_uint16		access_flags;
	_jc_uint16		num_constants;
	_jc_cf_constant		*constants;
	const char		*name;
	const char		*superclass;
	_jc_uint16		num_interfaces;
	const char		**interfaces;
	_jc_uint16		num_fields;
	_jc_cf_field		*fields;
	_jc_uint16		num_methods;
	_jc_cf_method		*methods;
	_jc_uint16		num_attributes;
	_jc_cf_attr		*attributes;
	_jc_cf_inner_classes	*inner_classes;
	const char		*source_file;
	char			*string_mem;	/* nul-terminated utf strings */
};

/* Internal parsing state */
struct _jc_cf_parse_state {
	_jc_env		*env;
	_jc_classfile	*cfile;
	const u_char	*bytes;
	size_t		length;
	size_t		pos;
};

/* cf_parse.c */
extern _jc_classfile	*_jc_parse_classfile(_jc_env *env,
				_jc_classbytes *bytes, int howmuch);
extern void		_jc_destroy_classfile(_jc_classfile **cfilep);
extern int		_jc_parse_code(_jc_env *env, _jc_classfile *cfile,
				_jc_cf_bytecode *bytecode, _jc_cf_code *code);
extern void		_jc_destroy_code(_jc_cf_code *code);

#endif	/* _CF_PARSE_H_ */
