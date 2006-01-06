
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

#ifndef _JC_DEFS_H_
#define _JC_DEFS_H_

#ifndef __GNUC__		/* XXX check gcc version here too */
#error "GCC is required"
#endif

#include "jc_machdep.h"
#include "jc_arraydefs.h"

/************************************************************************
 *			      Definitions				*
 ************************************************************************/

/* C stuff */
#define _JC_NULL			((void *)0)
#define _JC_OFFSETOF(s, f)		((_jc_word)&((s *)0)->f)

/* Access flags */
#define _JC_ACC_PUBLIC			0x0001
#define _JC_ACC_PRIVATE			0x0002
#define _JC_ACC_PROTECTED		0x0004
#define _JC_ACC_STATIC			0x0008
#define _JC_ACC_FINAL			0x0010
#define _JC_ACC_SUPER			0x0020
#define _JC_ACC_SYNCHRONIZED		0x0020
#define _JC_ACC_VOLATILE		0x0040
#define _JC_ACC_TRANSIENT		0x0080
#define _JC_ACC_NATIVE			0x0100
#define _JC_ACC_INTERFACE		0x0200
#define _JC_ACC_ABSTRACT		0x0400
#define _JC_ACC_STRICT			0x0800
#define _JC_ACC_MASK			0x0fff

/* Additional flags stored with access flags */
#define _JC_ACC_JCNI			0x1000	/* JCNI native method (!JNI) */

/*
 * Flags for 'flags' field of the '_jc_type' structure,
 * and unique indicies for the various primitive Java types.
 */
#define _JC_TYPE_INVALID		0x0000
#define _JC_TYPE_BOOLEAN		0x0001
#define _JC_TYPE_BYTE			0x0002
#define _JC_TYPE_CHAR			0x0003
#define _JC_TYPE_SHORT			0x0004
#define _JC_TYPE_INT			0x0005
#define _JC_TYPE_LONG			0x0006
#define _JC_TYPE_FLOAT			0x0007
#define _JC_TYPE_DOUBLE			0x0008
#define _JC_TYPE_VOID			0x0009
#define _JC_TYPE_REFERENCE		0x000a
#define _JC_TYPE_MAX			0x000b
#define _JC_TYPE_MASK			0x000f
#define _JC_TYPE_ARRAY			0x0010

#define _JC_TYPE_SKIPWORD		0x0020
#define _JC_TYPE_LOADED			0x0040
#define _JC_TYPE_INIT_ERROR		0x0080		/* class init error */

#define _JC_TYPE_VERIFIED		0x0100
#define _JC_TYPE_PREPARED		0x0200
#define _JC_TYPE_RESOLVED		0x0400
#define _JC_TYPE_INITIALIZED		0x8000

/* Fixed parameters for hash tables */
#define _JC_IMETHOD_HASHSIZE		128		/* must be power of 2 */
#define _JC_INSTANCEOF_HASHSIZE		128		/* must be power of 2 */

/************************************************************************
 *				Typedefs				*
 ************************************************************************/

typedef struct _jc_object		_jc_object;

typedef struct _jc_env			_jc_env;
typedef struct _jc_splay_node		_jc_splay_node;
typedef struct _jc_class_loader		_jc_class_loader;
typedef struct _jc_array_type		_jc_array_type;
typedef struct _jc_nonarray_type	_jc_nonarray_type;
typedef struct _jc_classfile		_jc_classfile;
typedef struct _jc_super_info		_jc_super_info;

typedef struct _jc_type			_jc_type;
typedef struct _jc_method		_jc_method;
typedef struct _jc_method_code		_jc_method_code;
typedef struct _jc_array		_jc_array;
typedef struct _jc_object_array		_jc_object_array;
typedef union _jc_value			_jc_value;
typedef union _jc_rvalue		_jc_rvalue;
typedef struct _jc_catch_frame		_jc_catch_frame;

typedef struct _jc_field		_jc_field;
typedef struct _jc_trap_info		_jc_trap_info;
typedef struct _jc_inner_class		_jc_inner_class;

typedef struct _jc_lookup		_jc_lookup;
typedef struct _jc_iinc			_jc_iinc;
typedef struct _jc_invoke		_jc_invoke;
typedef struct _jc_linemap		_jc_linemap;
typedef struct _jc_lookupswitch		_jc_lookupswitch;
typedef struct _jc_insn			_jc_insn;
typedef union _jc_insn_info		_jc_insn_info;
typedef struct _jc_field_info		_jc_field_info;
typedef struct _jc_string_info		_jc_string_info;
typedef struct _jc_multianewarray	_jc_multianewarray;
typedef struct _jc_tableswitch		_jc_tableswitch;
typedef struct _jc_interp_trap		_jc_interp_trap;

/************************************************************************
 *				Structures				*
 ************************************************************************/

/*
 * Java value. This differs from a JNI 'jvalue' in that if the value is a
 * Java object, then the 'l' field points directly to the object, whereas
 * in a 'jvalue' the 'l' field is a native reference to the object.
 */
union _jc_value {
	jboolean	z;
	jbyte		b;
	jchar		c;
	jshort		s;
	jint		i;
	jlong		j;
	jfloat		f;
	jdouble		d;
	_jc_object	*l;
	_jc_word	_dummy;			/* ensure size/alignment */
};

/*
 * Java return value. Like _jc_value but has z, b, c, and s folded
 * into i, because Java methods return all those types as ints.
 */
union _jc_rvalue {
	jint		i;
	jlong		j;
	jfloat		f;
	jdouble		d;
	_jc_object	*l;
	_jc_word	_dummy;			/* ensure size/alignment */
};

/*
 * One splay search tree node.
 */
struct _jc_splay_node {
	_jc_splay_node	*left;
	_jc_splay_node	*right;
	jboolean	inserted;
};

/* Object head */
struct _jc_object {
	_jc_word	lockword;
	_jc_type	*type;
};

/* Array head */
struct _jc_array {
	_jc_word	lockword;
	_jc_type	*type;
	const jint	length;
};

/*
 * Scalar arrays.
 */
#define _JC_DECLARE_SCALAR_ARRAY(type0, ctype)				\
struct _jc_ ## type0 ## _array {					\
	_jc_word	lockword;					\
	_jc_type	*type;						\
	const jint	length;						\
	ctype		elems[0];					\
};									\
typedef struct _jc_ ## type0 ## _array	_jc_ ## type0 ## _array;

_JC_DECLARE_SCALAR_ARRAY(boolean, jboolean);
_JC_DECLARE_SCALAR_ARRAY(byte, jbyte);
_JC_DECLARE_SCALAR_ARRAY(char, jchar);
_JC_DECLARE_SCALAR_ARRAY(short, jshort);
_JC_DECLARE_SCALAR_ARRAY(int, jint);
_JC_DECLARE_SCALAR_ARRAY(long, jlong);
_JC_DECLARE_SCALAR_ARRAY(float, jfloat);
_JC_DECLARE_SCALAR_ARRAY(double, jdouble);

/*
 * Reference arrays.
 *
 * Elements are indexed backwards, i.e., 0 is elems[-1], 1 is elems[-2], etc.
 * An easy way to convert between the two is one's complement, i.e., ~i.
 * See also _JC_REF_ELEMENT() macro.
 */
struct _jc_object_array {
	_jc_object		*elems[0];
	_jc_word		lockword;
	_jc_type		*type;
	const jint		length;
};

/************************************************************************
 *			Interpreted Method Info				*
 ************************************************************************/

struct _jc_iinc {
	_jc_uint16	local;
	jshort	  	value;
};

struct _jc_invoke {
	_jc_method	*method;
	_jc_uint16  	pop;
};

struct _jc_multianewarray {
	_jc_type	*type;
	unsigned char	dims;
};

struct _jc_lookup {
	jint		match;
	_jc_insn	*target;
};

struct _jc_string_info {
	_jc_object	*string;
	const char	*utf8;
};

struct _jc_lookupswitch {
	_jc_insn	*default_target;
	_jc_uint16	num_pairs;
	_jc_lookup	pairs[0];
};

struct _jc_tableswitch {
	jint		low;
	jint		high;
	_jc_insn	*default_target;
	_jc_insn	*targets[0];
};

struct _jc_interp_trap {
	_jc_insn	*start;
	_jc_insn	*end;
	_jc_insn	*target;
	_jc_type	*type;
};

struct _jc_linemap {
	_jc_uint16	index;
	_jc_uint16	line;
};

struct _jc_field_info {
	_jc_field	*field;
	union {
	    void	*data;
	    jint	offset;
	}		u;
};

union _jc_insn_info {
	_jc_invoke		invoke;
	_jc_field_info		field;
	_jc_iinc		iinc;
	_jc_multianewarray	multianewarray;
	_jc_type		*type;
	_jc_lookupswitch	*lookupswitch;
	_jc_tableswitch		*tableswitch;
	_jc_insn		*target;
	_jc_uint16		local;
	_jc_value		constant;
	_jc_string_info 	string;
};

struct _jc_insn {
	_jc_word		action;
	_jc_insn_info		info;
};

/************************************************************************
 *			Methods, Fields, Types, Etc.			*
 ************************************************************************/

/* Field descriptor */
struct _jc_field {
	const char		*name;
	const char		*signature;
	_jc_type		*class;
	_jc_type		*type;
	_jc_uint16		access_flags;
	int			offset;		/* in object or class_fields */
	void			*initial_value;	/* initialized static only */
};

/* Interpreted method info */
struct _jc_method_code {
	_jc_insn	*insns;
	_jc_interp_trap	*traps;
	_jc_linemap	*linemaps;
	_jc_uint16	max_stack;
	_jc_uint16	max_locals;
	_jc_uint16	num_traps;
	_jc_uint16	num_linemaps;
	_jc_uint16	num_insns;
	_jc_uint16	num_params2;
};

/* Method descriptor */
struct _jc_method {
	const char		*name;
	const char		*signature;
	_jc_type		*class;
	_jc_type		**param_types;
	_jc_type		*return_type;
	unsigned char		*param_ptypes;
	_jc_type		**exceptions;
	const void		*function;	/* code or interp trampoline */
	_jc_word		vtable_index;	/* index in vtable, mtable */
	_jc_uint16		access_flags;
	_jc_uint16		num_parameters;
	_jc_uint16		num_exceptions;
	_jc_uint16		signature_hash_bucket;
	const void		*native_function;
	_jc_method_code		code;
};

/* Type info specific to array types */
struct _jc_array_type {
	jint			dimensions;
	_jc_type		*base_type;
	_jc_type		*element_type;
};

/*
 * Type info specific to non-array types
 *
 * The field and method lists must be sorted by name, then signature.
 */
struct _jc_nonarray_type {
	jshort			block_size_index;
	_jc_uint16		num_vmethods;
	_jc_uint16		num_fields;
	_jc_uint16		num_methods;
	_jc_uint16		num_virtual_refs;
	_jc_uint16		num_inner_classes;
	_jc_uint32		instance_size;
	const char		*source_file;
	_jc_field		**fields;	
	_jc_method		**methods;
	_jc_type		***instanceof_hash_table;
	void			*class_fields;
	_jc_inner_class		*inner_classes;
	_jc_type		*outer_class;

	/* These fields are filled in at run-time */
	_jc_classfile		*cfile;
	_jc_env			*initializing_thread;
	_jc_super_info		*supers;
	_jc_method		**mtable;		/* "method vtable" */
};

/* Java type info */
struct _jc_type {
	const char		*name;
	_jc_type		*superclass;
	_jc_uint16		access_flags;
	_jc_uint16		flags;

	/* Interfaces info */
	_jc_uint16		num_interfaces;
	_jc_type		**interfaces;
	_jc_method		***imethod_hash_table;
	_jc_method		**imethod_quick_table;

	/* Specific array/non-array info */
	union {
	    _jc_nonarray_type	nonarray;
	    _jc_array_type	array;
	}			u;

	/* These fields are filled in at run-time */
	_jc_class_loader	*loader;
	_jc_word		initial_lockword;
	_jc_object		*instance;	/* java.lang.Class instance */
	_jc_splay_node		node;		/* in loader->defined_types */

	/* Pointers to method functions */
	const void		*vtable[0];
};

/* Inner class info */
struct _jc_inner_class {
	_jc_type		*type;
	_jc_uint16		access_flags;
};

/* Exception catcher */
struct _jc_catch_frame {
	_jc_catch_frame		*next;		/* next deeper catch frame */
	sigjmp_buf		context;	/* how to catch the exception */
};

/************************************************************************
 *				Macros					*
 ************************************************************************/

/*
 * Compare two long values.
 */
#define _JC_LCMP(x, y)							\
    ({									\
	const jlong _x = (x);						\
	const jlong _y = (y);						\
									\
	(_x > _y) - (_x < _y);						\
    })

/*
 * Compare two floating point values, with result 'greater than' if
 * either value is NaN.
 */
#define _JC_FCMPG(x, y)							\
    ({									\
	const jfloat _x = (x);						\
	const jfloat _y = (y);						\
									\
	(_x != _x || _y != _y) ? 1 : (_x > _y) - (_x < _y);		\
    })
#define _JC_DCMPG(x, y)							\
    ({									\
	const jdouble _x = (x);						\
	const jdouble _y = (y);						\
									\
	(_x != _x || _y != _y) ? 1 : (_x > _y) - (_x < _y);		\
    })

/*
 * Compare two floating point values, with result 'less than' if
 * either value is NaN.
 */
#define _JC_FCMPL(x, y)							\
    ({									\
	const jfloat _x = (x);						\
	const jfloat _y = (y);						\
									\
	(_x != _x || _y != _y) ? -1 : (_x > _y) - (_x < _y);		\
    })
#define _JC_DCMPL(x, y)							\
    ({									\
	const jdouble _x = (x);						\
	const jdouble _y = (y);						\
									\
	(_x != _x || _y != _y) ? -1 : (_x > _y) - (_x < _y);		\
    })

/*
 * Zero-filled arithmetic right shift for 'int' and 'long'.
 */
#define _JC_IUSHR(x, y)							\
    ({									\
	const jint _x = (x);						\
	const jint _y = (y) & 0x1f;					\
									\
	(jint)(((_jc_uint32)_x) >> _y);					\
    })
#define _JC_LUSHR(x, y)							\
    ({									\
	const jlong _x = (x);						\
	const jlong _y = (y) & 0x3f;					\
									\
	(jlong)(((_jc_uint64)_x) >> _y);				\
    })

/*
 * Signed-extended arithmetic right shift for 'int' and 'long'.
 */
#if _JC_SIGNED_RIGHT_SHIFT
#define _JC_ISHR(x, y)							\
    ({									\
	const jint _x = (x);						\
	const jint _y = (y) & 0x1f;					\
									\
	_x >> _y;							\
    })
#define _JC_LSHR(x, y)							\
    ({									\
	const jlong _x = (x);						\
	const jlong _y = (y) & 0x3f;					\
									\
	_x >> _y;							\
    })
#else	/* !_JC_SIGNED_RIGHT_SHIFT */
#define _JC_ISHR(x, y)							\
    ({									\
	const jint _x = (x);						\
	const jint _y = (y) & 0x1f;					\
	jint _result;							\
									\
	_result = _x >> _y;						\
	if (_x < 0)							\
		_result |= ~0 << (32 - _y);				\
	_result;							\
    })
#define _JC_LSHR(x, y)							\
    ({									\
	const jlong _x = (x);						\
	const jlong _y = (y) & 0x3f;					\
	jlong _result;							\
									\
	_result = _x >> _y;						\
	if (_x < 0)							\
		_result |= ~0 << (64 - _y);				\
	_result;							\
    })
#endif	/* !_JC_SIGNED_RIGHT_SHIFT */

/*
 * Conversion from floating point -> integral types.
 */
#define _JC_CAST_FLT2INT(vtype, ctype, value)				\
    ({									\
	const vtype _val = (value);					\
	const ctype _min = (ctype)(_JC_JLONG(1)				\
				<< (sizeof(ctype) * 8 - 1));		\
	const ctype _max = ~_min;					\
									\
	_val >= _max ? _max :						\
	_val <= _min ? _min :						\
	_val != _val ? 0 :						\
	(ctype)_val;							\
    })

/************************************************************************
 *				External data				*
 ************************************************************************/

/* Primitive classes */
extern _jc_type _jc_boolean$prim$type;
extern _jc_type _jc_byte$prim$type;
extern _jc_type _jc_char$prim$type;
extern _jc_type _jc_short$prim$type;
extern _jc_type _jc_int$prim$type;
extern _jc_type _jc_long$prim$type;
extern _jc_type _jc_float$prim$type;
extern _jc_type _jc_double$prim$type;
extern _jc_type _jc_void$prim$type;

/* Primitive array classes */
_JC_DECL_ARRAYS(boolean, prim$type);
_JC_DECL_ARRAYS(byte, prim$type);
_JC_DECL_ARRAYS(char, prim$type);
_JC_DECL_ARRAYS(short, prim$type);
_JC_DECL_ARRAYS(int, prim$type);
_JC_DECL_ARRAYS(long, prim$type);
_JC_DECL_ARRAYS(float, prim$type);
_JC_DECL_ARRAYS(double, prim$type);

/* Empty interface method lookup tables */
extern _jc_method	*_jc_empty_quick_table[_JC_IMETHOD_HASHSIZE];
extern _jc_method	**_jc_empty_imethod_table[_JC_IMETHOD_HASHSIZE];

#endif	/* _JC_DEFS_H_ */

