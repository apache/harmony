
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
