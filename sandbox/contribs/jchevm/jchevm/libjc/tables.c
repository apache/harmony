
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
 * Mapping from Java type to Java type signature letter.
 */
const char _jc_prim_chars[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	'Z',
	[_JC_TYPE_BYTE]=	'B',
	[_JC_TYPE_CHAR]=	'C',
	[_JC_TYPE_SHORT]=	'S',
	[_JC_TYPE_INT]=		'I',
	[_JC_TYPE_LONG]=	'J',
	[_JC_TYPE_FLOAT]=	'F',
	[_JC_TYPE_DOUBLE]=	'D',
	[_JC_TYPE_VOID]=	'V',
	[_JC_TYPE_REFERENCE]=	'L',
};

/*
 * Mapping from Java type signature letter to Java type.
 */
const u_char _jc_sig_types[0x100] = {
	['Z']=		_JC_TYPE_BOOLEAN,
	['B']=		_JC_TYPE_BYTE,
	['C']=		_JC_TYPE_CHAR,
	['S']=		_JC_TYPE_SHORT,
	['I']=		_JC_TYPE_INT,
	['J']=		_JC_TYPE_LONG,
	['F']=		_JC_TYPE_FLOAT,
	['D']=		_JC_TYPE_DOUBLE,
	['V']=		_JC_TYPE_VOID,
	['L']=		_JC_TYPE_REFERENCE,
	['[']=		_JC_TYPE_REFERENCE
};

/*
 * Size of various Java types.
 */
const size_t _jc_type_sizes[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	sizeof(jboolean),
	[_JC_TYPE_BYTE]=	sizeof(jbyte),
	[_JC_TYPE_CHAR]=	sizeof(jchar),
	[_JC_TYPE_SHORT]=	sizeof(jshort),
	[_JC_TYPE_INT]=		sizeof(jint),
	[_JC_TYPE_LONG]=	sizeof(jlong),
	[_JC_TYPE_FLOAT]=	sizeof(jfloat),
	[_JC_TYPE_DOUBLE]=	sizeof(jdouble),
	[_JC_TYPE_REFERENCE]=	sizeof(void *)
};

/*
 * Alignment requirements of various Java types.
 */
const size_t _jc_type_align[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	__alignof__(jboolean),
	[_JC_TYPE_BYTE]=	__alignof__(jbyte),
	[_JC_TYPE_CHAR]=	__alignof__(jchar),
	[_JC_TYPE_SHORT]=	__alignof__(jshort),
	[_JC_TYPE_INT]=		__alignof__(jint),
	[_JC_TYPE_LONG]=	__alignof__(jlong),
	[_JC_TYPE_FLOAT]=	__alignof__(jfloat),
	[_JC_TYPE_DOUBLE]=	__alignof__(jdouble),
	[_JC_TYPE_REFERENCE]=	__alignof__(void *),
};

/*
 * Whether each Java type requires one or two Java stack words.
 */
const u_char _jc_dword_type[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	JNI_FALSE,
	[_JC_TYPE_BYTE]=	JNI_FALSE,
	[_JC_TYPE_CHAR]=	JNI_FALSE,
	[_JC_TYPE_SHORT]=	JNI_FALSE,
	[_JC_TYPE_INT]=		JNI_FALSE,
	[_JC_TYPE_LONG]=	JNI_TRUE,
	[_JC_TYPE_FLOAT]=	JNI_FALSE,
	[_JC_TYPE_DOUBLE]=	JNI_TRUE,
	[_JC_TYPE_REFERENCE]=	JNI_FALSE,
};

/*
 * Size of the head part in the various array object types.
 */
const size_t _jc_array_head_sizes[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	_JC_OFFSETOF(_jc_boolean_array, elems),
	[_JC_TYPE_BYTE]=	_JC_OFFSETOF(_jc_byte_array, elems),
	[_JC_TYPE_CHAR]=	_JC_OFFSETOF(_jc_char_array, elems),
	[_JC_TYPE_SHORT]=	_JC_OFFSETOF(_jc_short_array, elems),
	[_JC_TYPE_INT]=		_JC_OFFSETOF(_jc_int_array, elems),
	[_JC_TYPE_LONG]=	_JC_OFFSETOF(_jc_long_array, elems),
	[_JC_TYPE_FLOAT]=	_JC_OFFSETOF(_jc_float_array, elems),
	[_JC_TYPE_DOUBLE]=	_JC_OFFSETOF(_jc_double_array, elems),
	[_JC_TYPE_REFERENCE]=	sizeof(_jc_object_array),
};

/*
 * Names of various primitive types.
 */
const char *const _jc_prim_names[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	"boolean",
	[_JC_TYPE_BYTE]=	"byte",
	[_JC_TYPE_CHAR]=	"char",
	[_JC_TYPE_SHORT]=	"short",
	[_JC_TYPE_INT]=		"int",
	[_JC_TYPE_LONG]=	"long",
	[_JC_TYPE_FLOAT]=	"float",
	[_JC_TYPE_DOUBLE]=	"double",
	[_JC_TYPE_VOID]=	"void",
	[_JC_TYPE_REFERENCE]=	"object"
};

/*
 * Method intepreter gateway functions.
 */
const void *const _jc_interp_funcs[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	_jc_interp_z,
	[_JC_TYPE_BYTE]=	_jc_interp_b,
	[_JC_TYPE_CHAR]=	_jc_interp_c,
	[_JC_TYPE_SHORT]=	_jc_interp_s,
	[_JC_TYPE_INT]=		_jc_interp_i,
	[_JC_TYPE_LONG]=	_jc_interp_j,
	[_JC_TYPE_FLOAT]=	_jc_interp_f,
	[_JC_TYPE_DOUBLE]=	_jc_interp_d,
	[_JC_TYPE_VOID]=	_jc_interp_v,
	[_JC_TYPE_REFERENCE]=	_jc_interp_l,
};
const void *const _jc_interp_native_funcs[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	_jc_interp_native_z,
	[_JC_TYPE_BYTE]=	_jc_interp_native_b,
	[_JC_TYPE_CHAR]=	_jc_interp_native_c,
	[_JC_TYPE_SHORT]=	_jc_interp_native_s,
	[_JC_TYPE_INT]=		_jc_interp_native_i,
	[_JC_TYPE_LONG]=	_jc_interp_native_j,
	[_JC_TYPE_FLOAT]=	_jc_interp_native_f,
	[_JC_TYPE_DOUBLE]=	_jc_interp_native_d,
	[_JC_TYPE_VOID]=	_jc_interp_native_v,
	[_JC_TYPE_REFERENCE]=	_jc_interp_native_l,
};

/*
 * Mapping from Java primitive type to Java wrapper class name.
 */
const char *const _jc_prim_wrapper_class[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	"java/lang/Boolean",
	[_JC_TYPE_BYTE]=	"java/lang/Byte",
	[_JC_TYPE_CHAR]=	"java/lang/Character",
	[_JC_TYPE_SHORT]=	"java/lang/Short",
	[_JC_TYPE_INT]=		"java/lang/Integer",
	[_JC_TYPE_LONG]=	"java/lang/Long",
	[_JC_TYPE_FLOAT]=	"java/lang/Float",
	[_JC_TYPE_DOUBLE]=	"java/lang/Double",
	[_JC_TYPE_VOID]=	"java/lang/Void"
};

/*
 * Maximum allowable array length for the various Java types.
 * Any longer and the total object size would overflow 'size_t'.
 */
const jlong _jc_type_max_array_length[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	(jlong)SIZE_T_MAX / sizeof(jboolean),
	[_JC_TYPE_BYTE]=	(jlong)SIZE_T_MAX / sizeof(jbyte),
	[_JC_TYPE_CHAR]=	(jlong)SIZE_T_MAX / sizeof(jchar),
	[_JC_TYPE_SHORT]=	(jlong)SIZE_T_MAX / sizeof(jshort),
	[_JC_TYPE_INT]=		(jlong)SIZE_T_MAX / sizeof(jint),
	[_JC_TYPE_LONG]=	(jlong)SIZE_T_MAX / sizeof(jlong),
	[_JC_TYPE_FLOAT]=	(jlong)SIZE_T_MAX / sizeof(jfloat),
	[_JC_TYPE_DOUBLE]=	(jlong)SIZE_T_MAX / sizeof(jdouble),
	[_JC_TYPE_REFERENCE]=	(jlong)SIZE_T_MAX / sizeof(void *)
};

/*
 * Field sorting order.
 *
 * This must sort the same as org.dellroad.jc.cgen.Util.OrderTypeSwitch.
 */
const int _jc_field_type_sort[_JC_TYPE_MAX] = {
	[_JC_TYPE_BOOLEAN]=	4,
	[_JC_TYPE_BYTE]=	4,
	[_JC_TYPE_CHAR]=	3,
	[_JC_TYPE_SHORT]=	3,
	[_JC_TYPE_INT]=		2,
	[_JC_TYPE_FLOAT]=	2,
	[_JC_TYPE_LONG]=	1,
	[_JC_TYPE_DOUBLE]=	1,
	[_JC_TYPE_REFERENCE]=	0,
};

/*
 * Names of VM exceptions.
 */
#define VMEX_ENTRY(name)	[_JC_ ## name]= "java/lang/" #name
const char *const _jc_vmex_names[_JC_VMEXCEPTION_MAX] = {
	VMEX_ENTRY(AbstractMethodError),
	VMEX_ENTRY(ArithmeticException),
	VMEX_ENTRY(ArrayIndexOutOfBoundsException),
	VMEX_ENTRY(ArrayStoreException),
	VMEX_ENTRY(ClassCastException),
	VMEX_ENTRY(ClassCircularityError),
	VMEX_ENTRY(ClassFormatError),
	VMEX_ENTRY(ClassNotFoundException),
	VMEX_ENTRY(CloneNotSupportedException),
	VMEX_ENTRY(ExceptionInInitializerError),
	[_JC_IOException] = "java/io/IOException",
	VMEX_ENTRY(IllegalAccessError),
	VMEX_ENTRY(IllegalAccessException),
	VMEX_ENTRY(IllegalArgumentException),
	VMEX_ENTRY(IllegalMonitorStateException),
	VMEX_ENTRY(IllegalThreadStateException),
	VMEX_ENTRY(IncompatibleClassChangeError),
	VMEX_ENTRY(InstantiationError),
	VMEX_ENTRY(InstantiationException),
	VMEX_ENTRY(InternalError),
	VMEX_ENTRY(InterruptedException),
	[_JC_InvocationTargetException]
	    = "java/lang/reflect/InvocationTargetException",
	VMEX_ENTRY(LinkageError),
	VMEX_ENTRY(NegativeArraySizeException),
	VMEX_ENTRY(NoClassDefFoundError),
	VMEX_ENTRY(NoSuchFieldError),
	VMEX_ENTRY(NoSuchMethodError),
	VMEX_ENTRY(NullPointerException),
	VMEX_ENTRY(OutOfMemoryError),
	VMEX_ENTRY(StackOverflowError),
	VMEX_ENTRY(ThreadDeath),
	VMEX_ENTRY(UnsatisfiedLinkError),
	VMEX_ENTRY(UnsupportedClassVersionError),
};

/*
 * Hex chars.
 */
const char _jc_hex_chars[16] = "0123456789abcdef";

/*
 * Printable names of the verbosity flags.
 */
const char *const _jc_verbose_names[_JC_VERBOSE_MAX] = {
	[_JC_VERBOSE_CLASS]=		"class",
	[_JC_VERBOSE_GC]=		"gc",
	[_JC_VERBOSE_JNI]=		"jni",
	[_JC_VERBOSE_EXCEPTIONS]=	"exceptions",
	[_JC_VERBOSE_RESOLUTION]=	"resolution",
	[_JC_VERBOSE_INIT]=		"init",
	[_JC_VERBOSE_JNI_INVOKE]=	"jni-invoke",
};

/*
 * Bytecode names.
 */
#define BYTECODE(name)	[_JC_ ## name]= #name
const char *const _jc_bytecode_names[0x100] = {
	BYTECODE(aaload),
	BYTECODE(aastore),
	BYTECODE(aconst_null),
	BYTECODE(aload),
	BYTECODE(aload_0),
	BYTECODE(aload_1),
	BYTECODE(aload_2),
	BYTECODE(aload_3),
	BYTECODE(anewarray),
	BYTECODE(areturn),
	BYTECODE(arraylength),
	BYTECODE(astore),
	BYTECODE(astore_0),
	BYTECODE(astore_1),
	BYTECODE(astore_2),
	BYTECODE(astore_3),
	BYTECODE(athrow),
	BYTECODE(baload),
	BYTECODE(bastore),
	BYTECODE(bipush),
	BYTECODE(caload),
	BYTECODE(castore),
	BYTECODE(checkcast),
	BYTECODE(d2f),
	BYTECODE(d2i),
	BYTECODE(d2l),
	BYTECODE(dadd),
	BYTECODE(daload),
	BYTECODE(dastore),
	BYTECODE(dcmpg),
	BYTECODE(dcmpl),
	BYTECODE(dconst_0),
	BYTECODE(dconst_1),
	BYTECODE(ddiv),
	BYTECODE(dload),
	BYTECODE(dload_0),
	BYTECODE(dload_1),
	BYTECODE(dload_2),
	BYTECODE(dload_3),
	BYTECODE(dmul),
	BYTECODE(dneg),
	BYTECODE(drem),
	BYTECODE(dreturn),
	BYTECODE(dstore),
	BYTECODE(dstore_0),
	BYTECODE(dstore_1),
	BYTECODE(dstore_2),
	BYTECODE(dstore_3),
	BYTECODE(dsub),
	BYTECODE(dup),
	BYTECODE(dup_x1),
	BYTECODE(dup_x2),
	BYTECODE(dup2),
	BYTECODE(dup2_x1),
	BYTECODE(dup2_x2),
	BYTECODE(f2d),
	BYTECODE(f2i),
	BYTECODE(f2l),
	BYTECODE(fadd),
	BYTECODE(failure),
	BYTECODE(faload),
	BYTECODE(fastore),
	BYTECODE(fcmpg),
	BYTECODE(fcmpl),
	BYTECODE(fconst_0),
	BYTECODE(fconst_1),
	BYTECODE(fconst_2),
	BYTECODE(fdiv),
	BYTECODE(fload),
	BYTECODE(fload_0),
	BYTECODE(fload_1),
	BYTECODE(fload_2),
	BYTECODE(fload_3),
	BYTECODE(fmul),
	BYTECODE(fneg),
	BYTECODE(frem),
	BYTECODE(freturn),
	BYTECODE(fstore),
	BYTECODE(fstore_0),
	BYTECODE(fstore_1),
	BYTECODE(fstore_2),
	BYTECODE(fstore_3),
	BYTECODE(fsub),
	BYTECODE(getfield),
	BYTECODE(getfield_z),
	BYTECODE(getfield_b),
	BYTECODE(getfield_c),
	BYTECODE(getfield_s),
	BYTECODE(getfield_i),
	BYTECODE(getfield_j),
	BYTECODE(getfield_f),
	BYTECODE(getfield_d),
	BYTECODE(getfield_l),
	BYTECODE(getstatic),
	BYTECODE(getstatic_z),
	BYTECODE(getstatic_b),
	BYTECODE(getstatic_c),
	BYTECODE(getstatic_s),
	BYTECODE(getstatic_i),
	BYTECODE(getstatic_j),
	BYTECODE(getstatic_f),
	BYTECODE(getstatic_d),
	BYTECODE(getstatic_l),
	BYTECODE(goto),
	BYTECODE(goto_w),
	BYTECODE(i2b),
	BYTECODE(i2c),
	BYTECODE(i2d),
	BYTECODE(i2f),
	BYTECODE(i2l),
	BYTECODE(i2s),
	BYTECODE(iadd),
	BYTECODE(iaload),
	BYTECODE(iand),
	BYTECODE(iastore),
	BYTECODE(iconst_m1),
	BYTECODE(iconst_0),
	BYTECODE(iconst_1),
	BYTECODE(iconst_2),
	BYTECODE(iconst_3),
	BYTECODE(iconst_4),
	BYTECODE(iconst_5),
	BYTECODE(idiv),
	BYTECODE(if_acmpeq),
	BYTECODE(if_acmpne),
	BYTECODE(if_icmpeq),
	BYTECODE(if_icmpne),
	BYTECODE(if_icmplt),
	BYTECODE(if_icmpge),
	BYTECODE(if_icmpgt),
	BYTECODE(if_icmple),
	BYTECODE(ifeq),
	BYTECODE(ifne),
	BYTECODE(iflt),
	BYTECODE(ifge),
	BYTECODE(ifgt),
	BYTECODE(ifle),
	BYTECODE(ifnonnull),
	BYTECODE(ifnull),
	BYTECODE(iinc),
	BYTECODE(iload),
	BYTECODE(iload_0),
	BYTECODE(iload_1),
	BYTECODE(iload_2),
	BYTECODE(iload_3),
	BYTECODE(imul),
	BYTECODE(ineg),
	BYTECODE(instanceof),
	BYTECODE(invokeinterface),
	BYTECODE(invokespecial),
	BYTECODE(invokestatic),
	BYTECODE(invokestatic2),
	BYTECODE(invokevirtual),
	BYTECODE(ior),
	BYTECODE(irem),
	BYTECODE(ireturn),
	BYTECODE(ishl),
	BYTECODE(ishr),
	BYTECODE(istore),
	BYTECODE(istore_0),
	BYTECODE(istore_1),
	BYTECODE(istore_2),
	BYTECODE(istore_3),
	BYTECODE(isub),
	BYTECODE(iushr),
	BYTECODE(ixor),
	BYTECODE(jsr),
	BYTECODE(jsr_w),
	BYTECODE(l2d),
	BYTECODE(l2f),
	BYTECODE(l2i),
	BYTECODE(ladd),
	BYTECODE(laload),
	BYTECODE(land),
	BYTECODE(lastore),
	BYTECODE(lcmp),
	BYTECODE(lconst_0),
	BYTECODE(lconst_1),
	BYTECODE(ldc),
	BYTECODE(ldc_w),
	BYTECODE(ldc_string),
	BYTECODE(ldc2_w),
	BYTECODE(ldiv),
	BYTECODE(lload),
	BYTECODE(lload_0),
	BYTECODE(lload_1),
	BYTECODE(lload_2),
	BYTECODE(lload_3),
	BYTECODE(lmul),
	BYTECODE(lneg),
	BYTECODE(lookupswitch),
	BYTECODE(lor),
	BYTECODE(lrem),
	BYTECODE(lreturn),
	BYTECODE(lshl),
	BYTECODE(lshr),
	BYTECODE(lstore),
	BYTECODE(lstore_0),
	BYTECODE(lstore_1),
	BYTECODE(lstore_2),
	BYTECODE(lstore_3),
	BYTECODE(lsub),
	BYTECODE(lushr),
	BYTECODE(lxor),
	BYTECODE(monitorenter),
	BYTECODE(monitorexit),
	BYTECODE(multianewarray),
	BYTECODE(new),
	BYTECODE(newarray),
	BYTECODE(nop),
	BYTECODE(pop),
	BYTECODE(pop2),
	BYTECODE(putfield),
	BYTECODE(putfield_z),
	BYTECODE(putfield_b),
	BYTECODE(putfield_c),
	BYTECODE(putfield_s),
	BYTECODE(putfield_i),
	BYTECODE(putfield_j),
	BYTECODE(putfield_f),
	BYTECODE(putfield_d),
	BYTECODE(putfield_l),
	BYTECODE(putstatic),
	BYTECODE(putstatic_z),
	BYTECODE(putstatic_b),
	BYTECODE(putstatic_c),
	BYTECODE(putstatic_s),
	BYTECODE(putstatic_i),
	BYTECODE(putstatic_j),
	BYTECODE(putstatic_f),
	BYTECODE(putstatic_d),
	BYTECODE(ret),
	BYTECODE(return),
	BYTECODE(saload),
	BYTECODE(sastore),
	BYTECODE(sipush),
	BYTECODE(swap),
	BYTECODE(tableswitch),
	BYTECODE(wide),
};
#undef BYTECODE

/*
 * Bytecode stack adjustments. This only contains instructions
 * that _jc_compute_stack_depth() would see.
 */
#define BYTECODE(name, amount)						\
	[_JC_ ## name]= ((signed char)((amount) ^ _JC_STACKADJ_INVALID))
const signed char _jc_bytecode_stackadj[0x100] = {
	BYTECODE(aaload, -1),
	BYTECODE(aastore, -3),
	BYTECODE(aload, 1),
	BYTECODE(anewarray, 0),
	BYTECODE(areturn, -1),
	BYTECODE(arraylength, 0),
	BYTECODE(astore, -1),
	BYTECODE(athrow, -1),
	BYTECODE(baload, -1),
	BYTECODE(bastore, -3),
	BYTECODE(caload, -1),
	BYTECODE(castore, -3),
	BYTECODE(checkcast, 0),
	BYTECODE(d2f, -1),
	BYTECODE(d2i, -1),
	BYTECODE(d2l, 0),
	BYTECODE(dadd, -2),
	BYTECODE(daload, 0),
	BYTECODE(dastore, -4),
	BYTECODE(dcmpg, -3),
	BYTECODE(dcmpl, -3),
	BYTECODE(ddiv, -2),
	BYTECODE(dload, 2),
	BYTECODE(dmul, -2),
	BYTECODE(dneg, 0),
	BYTECODE(drem, -2),
	BYTECODE(dreturn, -2),
	BYTECODE(dstore, -2),
	BYTECODE(dsub, -2),
	BYTECODE(dup, 1),
	BYTECODE(dup_x1, 1),
	BYTECODE(dup_x2, 1),
	BYTECODE(dup2, 2),
	BYTECODE(dup2_x1, 2),
	BYTECODE(dup2_x2, 2),
	BYTECODE(f2d, 1),
	BYTECODE(f2i, 0),
	BYTECODE(f2l, 1),
	BYTECODE(fadd, -1),
	BYTECODE(faload, -1),
	BYTECODE(fastore, -3),
	BYTECODE(fcmpg, -1),
	BYTECODE(fcmpl, -1),
	BYTECODE(fdiv, -1),
	BYTECODE(fload, 1),
	BYTECODE(fmul, -1),
	BYTECODE(fneg, 0),
	BYTECODE(frem, -1),
	BYTECODE(freturn, -1),
	BYTECODE(fstore, -1),
	BYTECODE(fsub, -1),
	BYTECODE(getfield_z, 0),
	BYTECODE(getfield_b, 0),
	BYTECODE(getfield_c, 0),
	BYTECODE(getfield_s, 0),
	BYTECODE(getfield_i, 0),
	BYTECODE(getfield_j, 1),
	BYTECODE(getfield_f, 0),
	BYTECODE(getfield_d, 1),
	BYTECODE(getfield_l, 0),
	/* getstatic: variable */
	BYTECODE(goto, 0),
	BYTECODE(i2b, 0),
	BYTECODE(i2c, 0),
	BYTECODE(i2d, 1),
	BYTECODE(i2f, 0),
	BYTECODE(i2l, 1),
	BYTECODE(i2s, 0),
	BYTECODE(iadd, -1),
	BYTECODE(iaload, -1),
	BYTECODE(iand, -1),
	BYTECODE(iastore, -3),
	BYTECODE(idiv, -1),
	BYTECODE(if_acmpeq, -2),
	BYTECODE(if_acmpne, -2),
	BYTECODE(if_icmpeq, -2),
	BYTECODE(if_icmpne, -2),
	BYTECODE(if_icmplt, -2),
	BYTECODE(if_icmpge, -2),
	BYTECODE(if_icmpgt, -2),
	BYTECODE(if_icmple, -2),
	BYTECODE(ifeq, -1),
	BYTECODE(ifne, -1),
	BYTECODE(iflt, -1),
	BYTECODE(ifge, -1),
	BYTECODE(ifgt, -1),
	BYTECODE(ifle, -1),
	BYTECODE(ifnonnull, -1),
	BYTECODE(ifnull, -1),
	BYTECODE(iinc, 0),
	BYTECODE(iload, 1),
	BYTECODE(imul, -1),
	BYTECODE(ineg, 0),
	BYTECODE(instanceof, 0),
	/* invokeinterface: variable */
	/* invokespecial: variable */
	/* invokestatic: variable */
	/* invokevirtual: variable */
	BYTECODE(ior, -1),
	BYTECODE(irem, -1),
	BYTECODE(ireturn, -1),
	BYTECODE(ishl, -1),
	BYTECODE(ishr, -1),
	BYTECODE(istore, -1),
	BYTECODE(isub, -1),
	BYTECODE(iushr, -1),
	BYTECODE(ixor, -1),
	BYTECODE(jsr, 1),
	BYTECODE(l2d, 0),
	BYTECODE(l2f, -1),
	BYTECODE(l2i, -1),
	BYTECODE(ladd, -2),
	BYTECODE(laload, 0),
	BYTECODE(land, -2),
	BYTECODE(lastore, -4),
	BYTECODE(lcmp, -3),
	BYTECODE(ldc, 1),
	BYTECODE(ldc_string, 1),
	BYTECODE(ldc2_w, 2),
	BYTECODE(ldiv, -2),
	BYTECODE(lload, 2),
	BYTECODE(lmul, -2),
	BYTECODE(lneg, 0),
	BYTECODE(lookupswitch, -1),
	BYTECODE(lor, -2),
	BYTECODE(lrem, -2),
	BYTECODE(lreturn, -2),
	BYTECODE(lshl, -1),
	BYTECODE(lshr, -1),
	BYTECODE(lstore, -2),
	BYTECODE(lsub, -2),
	BYTECODE(lushr, -1),
	BYTECODE(lxor, -2),
	BYTECODE(monitorenter, -1),
	BYTECODE(monitorexit, -1),
	/* multianewarray: variable */
	BYTECODE(new, 1),
	BYTECODE(newarray, 0),
	BYTECODE(nop, 0),
	BYTECODE(pop, -1),
	BYTECODE(pop2, -2),
	BYTECODE(putfield_z, -2),
	BYTECODE(putfield_b, -2),
	BYTECODE(putfield_c, -2),
	BYTECODE(putfield_s, -2),
	BYTECODE(putfield_i, -2),
	BYTECODE(putfield_j, -3),
	BYTECODE(putfield_f, -2),
	BYTECODE(putfield_d, -3),
	BYTECODE(putfield_l, -2),
	/* putstatic: variable */
	BYTECODE(ret, 0),
	BYTECODE(return, 0),
	BYTECODE(saload, -1),
	BYTECODE(sastore, -3),
	BYTECODE(swap, 0),
	BYTECODE(tableswitch, -1),
};
#undef BYTECODE

