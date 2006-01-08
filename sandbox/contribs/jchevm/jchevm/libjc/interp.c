
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

/* Public variables */
const _jc_word	*_jc_interp_targets;

/* Internal functions */
static jint	_jc_interp(_jc_env *env, _jc_method *const method);
static int	_jc_lookup_compare(const void *v1, const void *v2);
static void	_jc_vinterp(_jc_env *env, va_list args);
static void	_jc_vinterp_native(_jc_env *env, va_list args);

/*
 * Macros used in _jc_interp()
 */
#ifndef NDEBUG

#define CHECK_PC_SP()							\
    do {								\
	_JC_ASSERT(sp >= locals + code->max_locals);			\
	_JC_ASSERT(sp <= locals + code->max_locals + code->max_stack);	\
	_JC_ASSERT(pc >= code->insns					\
	    && pc < code->insns + code->num_insns);			\
	_JC_ASSERT(pc->action != 0);					\
	_JC_ASSERT(ticker > 0);						\
    } while (0)

#else	/* !NDEBUG */

#define CHECK_PC_SP()							\
    do { } while (0)							\

#endif	/* !NDEBUG */

#define JUMP(_pc)							\
    do {								\
	pc = (_pc);							\
	CHECK_PC_SP();							\
	if (_JC_UNLIKELY(--ticker == 0))				\
		goto periodic_check;					\
	goto *(void *)pc->action;					\
    } while (0)

#define NEXT()								\
    do {								\
	CHECK_PC_SP();							\
	goto *(void *)(++pc)->action;					\
    } while (0)

#define RERUN()								\
    do {								\
	CHECK_PC_SP();							\
	goto *(void *)pc->action;					\
    } while (0)

#define STACKI(i)	(*(jint *)(sp + (i)))
#define STACKF(i)	(*(jfloat *)(sp + (i)))
#define STACKJ(i)	(*(jlong *)(sp + (i)))
#define STACKD(i)	(*(jdouble *)(sp + (i)))
#define STACKL(i)	(*(_jc_object **)(sp + (i)))
#define STACKW(i)	(sp[i])
#define LOCALI(i)	(*(jint *)(locals + i))
#define LOCALF(i)	(*(jfloat *)(locals + i))
#define LOCALJ(i)	(*(jlong *)(locals + i))
#define LOCALD(i)	(*(jdouble *)(locals + i))
#define LOCALL(i)	(*(_jc_object **)(locals + i))
#define PUSHI(v)	do { STACKI(0) = (v); sp++; } while (0)
#define PUSHF(v)	do { STACKF(0) = (v); sp++; } while (0)
#define PUSHJ(v)	do { STACKJ(0) = (v); sp += 2; } while (0)
#define PUSHD(v)	do { STACKD(0) = (v); sp += 2; } while (0)
#define PUSHL(v)	do { STACKL(0) = (v); sp++; } while (0)
#define POP(i)		(sp -= (i))

#define INFO(f)		(pc->info.f)

#define NULLPOINTERCHECK(x)						\
    do {								\
	if (_JC_UNLIKELY((x) == NULL))					\
		goto null_pointer_exception;				\
    } while (0)

#define ARRAYCHECK(array, i)						\
    do {								\
	_jc_array *const _array = (_jc_array *)(array);			\
	jint _i = (i);							\
									\
	NULLPOINTERCHECK(_array);					\
	if (_JC_UNLIKELY(_i < 0 || _i >= _array->length)) {		\
		stack_frame.pc = pc;					\
		_jc_post_exception_msg(env,				\
		    _JC_ArrayIndexOutOfBoundsException, "%d", _i);	\
		goto exception;						\
	}								\
    } while (0)

#define INITIALIZETYPE(t)						\
    do {								\
    	_jc_type *const _type = (t);					\
									\
	if (_JC_UNLIKELY(!_JC_FLG_TEST(_type, INITIALIZED))) {		\
		stack_frame.pc = pc;					\
		if (_jc_initialize_type(env, _type) != JNI_OK)		\
			goto exception;					\
	}								\
    } while (0)

#define PERIODIC_CHECK_TICKS	32

/*
 * Java interpreter. The "args" must contain two elements for long/double.
 *
 * If successful, return value is stored in env->retval and JNI_OK returned.
 * Otherwise, an exception is posted and JNI_ERR is returned.
 */
static jint
_jc_interp(_jc_env *const env, _jc_method *const method)
{
#define ACTION(name)  [_JC_ ## name]= (_jc_word)&&do_ ## name
#define TARGET(name)  							\
	_JC_ASSERT(JNI_FALSE);		/* should never fall through */	\
	do_ ## name:   asm ("/***** " #name " *****/");
	static const _jc_word actions[0x100] = {
		ACTION(aaload),
		ACTION(aastore),
		ACTION(aconst_null),
		ACTION(aload),
		ACTION(aload_0),
		ACTION(aload_1),
		ACTION(aload_2),
		ACTION(aload_3),
		ACTION(anewarray),
		ACTION(areturn),
		ACTION(arraylength),
		ACTION(astore),
		ACTION(astore_0),
		ACTION(astore_1),
		ACTION(astore_2),
		ACTION(astore_3),
		ACTION(athrow),
		ACTION(baload),
		ACTION(bastore),
		ACTION(caload),
		ACTION(castore),
		ACTION(checkcast),
		ACTION(d2f),
		ACTION(d2i),
		ACTION(d2l),
		ACTION(dadd),
		ACTION(daload),
		ACTION(dastore),
		ACTION(dcmpg),
		ACTION(dcmpl),
		ACTION(dconst_0),
		ACTION(dconst_1),
		ACTION(ddiv),
		ACTION(dload),
		ACTION(dload_0),
		ACTION(dload_1),
		ACTION(dload_2),
		ACTION(dload_3),
		ACTION(dmul),
		ACTION(dneg),
		ACTION(drem),
		ACTION(dreturn),
		ACTION(dstore),
		ACTION(dstore_0),
		ACTION(dstore_1),
		ACTION(dstore_2),
		ACTION(dstore_3),
		ACTION(dsub),
		ACTION(dup),
		ACTION(dup2),
		ACTION(dup2_x1),
		ACTION(dup2_x2),
		ACTION(dup_x1),
		ACTION(dup_x2),
		ACTION(f2d),
		ACTION(f2i),
		ACTION(f2l),
		ACTION(fadd),
		ACTION(failure),
		ACTION(faload),
		ACTION(fastore),
		ACTION(fcmpg),
		ACTION(fcmpl),
		ACTION(fconst_0),
		ACTION(fconst_1),
		ACTION(fconst_2),
		ACTION(fdiv),
		ACTION(fload),
		ACTION(fload_0),
		ACTION(fload_1),
		ACTION(fload_2),
		ACTION(fload_3),
		ACTION(fmul),
		ACTION(fneg),
		ACTION(frem),
		ACTION(freturn),
		ACTION(fstore),
		ACTION(fstore_0),
		ACTION(fstore_1),
		ACTION(fstore_2),
		ACTION(fstore_3),
		ACTION(fsub),
		ACTION(getfield_b),
		ACTION(getfield_c),
		ACTION(getfield_d),
		ACTION(getfield_f),
		ACTION(getfield_i),
		ACTION(getfield_j),
		ACTION(getfield_l),
		ACTION(getfield_s),
		ACTION(getfield_z),
		ACTION(getstatic),
		ACTION(getstatic_b),
		ACTION(getstatic_c),
		ACTION(getstatic_d),
		ACTION(getstatic_f),
		ACTION(getstatic_i),
		ACTION(getstatic_j),
		ACTION(getstatic_l),
		ACTION(getstatic_s),
		ACTION(getstatic_z),
		ACTION(goto),
		ACTION(i2b),
		ACTION(i2c),
		ACTION(i2d),
		ACTION(i2f),
		ACTION(i2l),
		ACTION(i2s),
		ACTION(iadd),
		ACTION(iaload),
		ACTION(iand),
		ACTION(iastore),
		ACTION(iconst_0),
		ACTION(iconst_1),
		ACTION(iconst_2),
		ACTION(iconst_3),
		ACTION(iconst_4),
		ACTION(iconst_5),
		ACTION(iconst_m1),
		ACTION(idiv),
		ACTION(if_acmpeq),
		ACTION(if_acmpne),
		ACTION(if_icmpeq),
		ACTION(if_icmpge),
		ACTION(if_icmpgt),
		ACTION(if_icmple),
		ACTION(if_icmplt),
		ACTION(if_icmpne),
		ACTION(ifeq),
		ACTION(ifge),
		ACTION(ifgt),
		ACTION(ifle),
		ACTION(iflt),
		ACTION(ifne),
		ACTION(ifnonnull),
		ACTION(ifnull),
		ACTION(iinc),
		ACTION(iload),
		ACTION(iload_0),
		ACTION(iload_1),
		ACTION(iload_2),
		ACTION(iload_3),
		ACTION(imul),
		ACTION(ineg),
		ACTION(instanceof),
		ACTION(invokeinterface),
		ACTION(invokespecial),
		ACTION(invokestatic),
		ACTION(invokestatic2),
		ACTION(invokevirtual),
		ACTION(ior),
		ACTION(irem),
		ACTION(ireturn),
		ACTION(ishl),
		ACTION(ishr),
		ACTION(istore),
		ACTION(istore_0),
		ACTION(istore_1),
		ACTION(istore_2),
		ACTION(istore_3),
		ACTION(isub),
		ACTION(iushr),
		ACTION(ixor),
		ACTION(jsr),
		ACTION(l2d),
		ACTION(l2f),
		ACTION(l2i),
		ACTION(ladd),
		ACTION(laload),
		ACTION(land),
		ACTION(lastore),
		ACTION(lcmp),
		ACTION(lconst_0),
		ACTION(lconst_1),
		ACTION(ldc),
		ACTION(ldc2_w),
		ACTION(ldc_string),
		ACTION(ldiv),
		ACTION(lload),
		ACTION(lload_0),
		ACTION(lload_1),
		ACTION(lload_2),
		ACTION(lload_3),
		ACTION(lmul),
		ACTION(lneg),
		ACTION(lookupswitch),
		ACTION(lor),
		ACTION(lrem),
		ACTION(lreturn),
		ACTION(lshl),
		ACTION(lshr),
		ACTION(lstore),
		ACTION(lstore_0),
		ACTION(lstore_1),
		ACTION(lstore_2),
		ACTION(lstore_3),
		ACTION(lsub),
		ACTION(lushr),
		ACTION(lxor),
		ACTION(monitorenter),
		ACTION(monitorexit),
		ACTION(multianewarray),
		ACTION(new),
		ACTION(newarray),
		ACTION(nop),
		ACTION(pop),
		ACTION(pop2),
		ACTION(putfield_b),
		ACTION(putfield_c),
		ACTION(putfield_d),
		ACTION(putfield_f),
		ACTION(putfield_i),
		ACTION(putfield_j),
		ACTION(putfield_l),
		ACTION(putfield_s),
		ACTION(putfield_z),
		ACTION(putstatic),
		ACTION(putstatic_b),
		ACTION(putstatic_c),
		ACTION(putstatic_d),
		ACTION(putstatic_f),
		ACTION(putstatic_i),
		ACTION(putstatic_j),
		ACTION(putstatic_l),
		ACTION(putstatic_s),
		ACTION(putstatic_z),
		ACTION(ret),
		ACTION(return),
		ACTION(saload),
		ACTION(sastore),
		ACTION(swap),
		ACTION(tableswitch),
	};
	_jc_method_code *const code = &method->code;
	_jc_java_stack stack_frame;
	_jc_method *imethod;
	_jc_word *locals;
	_jc_object *lock;
	_jc_insn *pc;
	_jc_word *sp;
	jint ticker;
	jint status;

	/* Special hack to copy out target offsets */
	if (_JC_UNLIKELY(method == NULL)) {
		env->retval.l = (_jc_object *)actions;
		return JNI_OK;
	}

	/* Sanity check */
	_JC_ASSERT(env->sp != NULL);
	_JC_ASSERT(env->sp >= env->stack_data);
	_JC_ASSERT(env->sp <= env->stack_data + env->vm->java_stack_size);

	/* Is method abstract? */
	if (_JC_UNLIKELY(_JC_ACC_TEST(method, ABSTRACT))) {
		_jc_post_exception_msg(env, _JC_AbstractMethodError,
		    "%s.%s%s", method->class->name, method->name,
		    method->signature);
		return JNI_ERR;
	}

	/* Check for C stack overflow */
#if _JC_DOWNWARD_STACK
	if (_JC_UNLIKELY((char *)&env < env->stack_limit
	    && (env->in_vmex & (1 << _JC_StackOverflowError)) == 0))
#else
	if (_JC_UNLIKELY((char *)&env > env->stack_limit
	    && (env->in_vmex & (1 << _JC_StackOverflowError)) == 0))
#endif
	{
		_jc_post_exception(env, _JC_StackOverflowError);
		return JNI_ERR;
	}

	/* Create Java stack space */
	locals = env->sp;
	env->sp = locals + code->max_locals + code->max_stack;

	/* Check Java stack overflow */
	if (_JC_UNLIKELY(env->sp > env->stack_data_end)) {
		_jc_post_exception(env, _JC_StackOverflowError);
		goto fail;
	}

	/* Synchronize */
	if (_JC_UNLIKELY(_JC_ACC_TEST(method, SYNCHRONIZED))) {
		lock = _JC_ACC_TEST(method, STATIC) ?
		    method->class->instance : LOCALL(0);
		if (_JC_UNLIKELY(_jc_lock_object(env, lock) != JNI_OK))
			goto fail;
	} else
		lock = NULL;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method->class, INTERFACE)
	    || strcmp(method->name, "<clinit>") == 0);
	_JC_ASSERT(_JC_FLG_TEST(method->class, RESOLVED));
	_JC_ASSERT(!_JC_ACC_TEST(method, NATIVE));
	_JC_ASSERT(code->insns != NULL);
	_JC_ASSERT(code->num_insns > 0);

	/* Push Java stack frame */
	memset(&stack_frame, 0, sizeof(stack_frame));
	stack_frame.next = env->java_stack;
	stack_frame.method = method;
	env->java_stack = &stack_frame;

	/* Begin execution */
	ticker = PERIODIC_CHECK_TICKS;
	sp = locals + code->max_locals;
	pc = code->insns;
	RERUN();

TARGET(aaload)
    {
	_jc_object_array *array;
	jint index;

	POP(1);
	array = (_jc_object_array *)STACKL(-1);
	index = STACKI(0);
	ARRAYCHECK(array, index);
	_JC_ASSERT(_JC_LW_TEST(array->lockword, ARRAY)
	    && _JC_LW_EXTRACT(array->lockword, TYPE) == _JC_TYPE_REFERENCE);
	STACKL(-1) = array->elems[~index];
	NEXT();
    }
TARGET(aastore)
    {
	_jc_object_array *array;
	_jc_object *obj;
	jint index;

	POP(3);
	array = (_jc_object_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	obj = STACKL(2);
	if (_JC_LIKELY(obj != NULL)) {
		stack_frame.pc = pc;
		switch (_jc_assignable_from(env, obj->type,
		    array->type->u.array.element_type)) {
		case 1:
			break;
		case 0:
			_jc_post_exception_msg(env, _JC_ArrayStoreException,
			    "`%s' not an instanceof `%s'", obj->type->name,
			    array->type->u.array.element_type->name);
			/* FALLTHROUGH */
		case -1:
			goto exception;
		}
	}
	array->elems[~index] = obj;
	NEXT();
    }
TARGET(aconst_null)
	PUSHL(NULL);
	NEXT();
TARGET(aload)
	PUSHL(LOCALL(INFO(local)));
	NEXT();
TARGET(aload_0)
	PUSHL(LOCALL(0));
	NEXT();
TARGET(aload_1)
	PUSHL(LOCALL(1));
	NEXT();
TARGET(aload_2)
	PUSHL(LOCALL(2));
	NEXT();
TARGET(aload_3)
	PUSHL(LOCALL(3));
	NEXT();
TARGET(anewarray)
    {
	_jc_array *array;

	stack_frame.pc = pc;
	if (_JC_UNLIKELY((array = _jc_new_array(env,
	    INFO(type), STACKI(-1))) == NULL))
		goto exception;
	STACKL(-1) = (_jc_object *)array;
	NEXT();
    }
TARGET(areturn)
	env->retval.l = STACKL(-1);
	goto done;
TARGET(arraylength)
    {
	_jc_array *array;

	NULLPOINTERCHECK(array = (_jc_array *)STACKL(-1));
	STACKI(-1) = array->length;
	NEXT();
    }
TARGET(astore)
	POP(1);
	LOCALL(INFO(local)) = STACKL(0);
	NEXT();
TARGET(astore_0)
	POP(1);
	LOCALL(0) = STACKL(0);
	NEXT();
TARGET(astore_1)
	POP(1);
	LOCALL(1) = STACKL(0);
	NEXT();
TARGET(astore_2)
	POP(1);
	LOCALL(2) = STACKL(0);
	NEXT();
TARGET(astore_3)
	POP(1);
	LOCALL(3) = STACKL(0);
	NEXT();
TARGET(athrow)
	POP(1);
	NULLPOINTERCHECK(STACKL(0));
	stack_frame.pc = pc;
	_jc_post_exception_object(env, STACKL(0));
	goto exception;
TARGET(baload)
    {
	_jc_array *array;
	jint index;

	POP(1);
	array = (_jc_array *)STACKL(-1);
	index = STACKI(0);
	ARRAYCHECK(array, index);
	STACKI(-1) = ((_jc_byte_array *)array)->elems[index];
	NEXT();
    }
TARGET(bastore)
    {
	_jc_array *array;
	jint index;
	int ptype;

	POP(3);
	array = (_jc_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	ptype = _JC_LW_EXTRACT(array->lockword, TYPE);
	_JC_ASSERT(ptype == _JC_TYPE_BOOLEAN || ptype == _JC_TYPE_BYTE);
	if (ptype == _JC_TYPE_BOOLEAN)
		((_jc_boolean_array *)array)->elems[index] = STACKI(2) & 0x1;
	else
		((_jc_byte_array *)array)->elems[index] = STACKI(2) & 0xff;
	NEXT();
    }
TARGET(caload)
    {
	_jc_char_array *array;
	jint index;

	POP(1);
	array = (_jc_char_array *)STACKL(-1);
	index = STACKI(0);
	ARRAYCHECK(array, index);
	STACKI(-1) = array->elems[index];
	NEXT();
    }
TARGET(castore)
    {
	_jc_char_array *array;
	jint index;

	POP(3);
	array = (_jc_char_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	array->elems[index] = STACKI(2) & 0xffff;
	NEXT();
    }
TARGET(checkcast)
    {
	_jc_object *const obj = STACKL(-1);

	if (_JC_LIKELY(obj != NULL)) {
		_jc_type *const type = INFO(type);

		stack_frame.pc = pc;
		switch (_jc_assignable_from(env, obj->type, type)) {
		case 1:
			break;
		case 0:
			_jc_post_exception_msg(env, _JC_ClassCastException,
			    "can't cast `%s' to `%s'", obj->type->name,
			    type->name);
			/* FALLTHROUGH */
		case -1:
			goto exception;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}
	NEXT();
    }
TARGET(dconst_0)
	PUSHD(0);
	NEXT();
TARGET(dconst_1)
	PUSHD(1);
	NEXT();
TARGET(d2f)
	POP(1);
	STACKF(-1) = STACKD(-1);
	NEXT();
TARGET(d2i)
	POP(1);
	STACKI(-1) = _JC_CAST_FLT2INT(jdouble, jint, STACKD(-1));
	NEXT();
TARGET(d2l)
	STACKJ(-2) = _JC_CAST_FLT2INT(jdouble, jlong, STACKD(-2));
	NEXT();
TARGET(dadd)
	POP(2);
	STACKD(-2) += STACKD(0);
	NEXT();
TARGET(daload)
    {
	_jc_double_array *array;
	jint index;

	array = (_jc_double_array *)STACKL(-2);
	index = STACKI(-1);
	ARRAYCHECK(array, index);
	STACKD(-2) = array->elems[index];
	NEXT();
    }
TARGET(dastore)
    {
	_jc_double_array *array;
	jint index;

	POP(4);
	array = (_jc_double_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	array->elems[index] = STACKD(2);
	NEXT();
    }
TARGET(dcmpg)
	POP(3);
	STACKI(-1) = _JC_DCMPG(STACKD(-1), STACKD(1));
	NEXT();
TARGET(dcmpl)
	POP(3);
	STACKI(-1) = _JC_DCMPL(STACKD(-1), STACKD(1));
	NEXT();
TARGET(ddiv)
	POP(2);
	STACKD(-2) /= STACKD(0);
	NEXT();
TARGET(dload)
	PUSHD(LOCALD(INFO(local)));
	NEXT();
TARGET(dload_0)
	PUSHD(LOCALD(0));
	NEXT();
TARGET(dload_1)
	PUSHD(LOCALD(1));
	NEXT();
TARGET(dload_2)
	PUSHD(LOCALD(2));
	NEXT();
TARGET(dload_3)
	PUSHD(LOCALD(3));
	NEXT();
TARGET(dmul)
	POP(2);
	STACKD(-2) *= STACKD(0);
	NEXT();
TARGET(dneg)
	STACKD(-2) = -STACKD(-2);
	NEXT();
TARGET(drem)
	POP(2);
	STACKD(-2) = fmod(STACKD(-2), STACKD(0));
	NEXT();
TARGET(dreturn)
	env->retval.d = STACKD(-2);
	goto done;
TARGET(dstore)
	POP(2);
	LOCALD(INFO(local)) = STACKD(0);
	NEXT();
TARGET(dstore_0)
	POP(2);
	LOCALD(0) = STACKD(0);
	NEXT();
TARGET(dstore_1)
	POP(2);
	LOCALD(1) = STACKD(0);
	NEXT();
TARGET(dstore_2)
	POP(2);
	LOCALD(2) = STACKD(0);
	NEXT();
TARGET(dstore_3)
	POP(2);
	LOCALD(3) = STACKD(0);
	NEXT();
TARGET(dsub)
	POP(2);
	STACKD(-2) -= STACKD(0);
	NEXT();
TARGET(dup)
	STACKW(0) = STACKW(-1);
	POP(-1);
	NEXT();
TARGET(dup_x1)
	STACKW(0) = STACKW(-1);
	STACKW(-1) = STACKW(-2);
	STACKW(-2) = STACKW(0);
	POP(-1);
	NEXT();
TARGET(dup_x2)
	STACKW(0) = STACKW(-1);
	STACKW(-1) = STACKW(-2);
	STACKW(-2) = STACKW(-3);
	STACKW(-3) = STACKW(0);
	POP(-1);
	NEXT();
TARGET(dup2)
	STACKW(1) = STACKW(-1);
	STACKW(0) = STACKW(-2);
	POP(-2);
	NEXT();
TARGET(dup2_x1)
	STACKW(1) = STACKW(-1);
	STACKW(0) = STACKW(-2);
	STACKW(-1) = STACKW(-3);
	STACKW(-2) = STACKW(1);
	STACKW(-3) = STACKW(0);
	POP(-2);
	NEXT();
TARGET(dup2_x2)
	STACKW(1) = STACKW(-1);
	STACKW(0) = STACKW(-2);
	STACKW(-1) = STACKW(-3);
	STACKW(-2) = STACKW(-4);
	STACKW(-3) = STACKW(1);
	STACKW(-4) = STACKW(0);
	POP(-2);
	NEXT();
TARGET(f2d)
	POP(-1);
	STACKD(-2) = STACKF(-2);
	NEXT();
TARGET(f2i)
	STACKI(-1) = _JC_CAST_FLT2INT(jfloat, jint, STACKF(-1));
	NEXT();
TARGET(f2l)
	POP(-1);
	STACKJ(-2) = _JC_CAST_FLT2INT(jfloat, jlong, STACKF(-2));
	NEXT();
TARGET(fadd)
	POP(1);
	STACKF(-1) += STACKF(0);
	NEXT();
TARGET(faload)
    {
	_jc_float_array *array;
	jint index;

	POP(1);
	array = (_jc_float_array *)STACKL(-1);
	index = STACKI(0);
	ARRAYCHECK(array, index);
	STACKF(-1) = array->elems[index];
	NEXT();
    }
TARGET(failure)
	stack_frame.pc = pc;
	_jc_post_exception_msg(env, _JC_InternalError, "failure opcode");
	goto exception;
TARGET(fastore)
    {
	_jc_float_array *array;
	jint index;

	POP(3);
	array = (_jc_float_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	array->elems[index] = STACKF(2);
	NEXT();
    }
TARGET(fcmpg)
	POP(1);
	STACKI(-1) = _JC_FCMPG(STACKF(-1), STACKF(0));
	NEXT();
TARGET(fcmpl)
	POP(1);
	STACKI(-1) = _JC_FCMPL(STACKF(-1), STACKF(0));
	NEXT();
TARGET(fconst_0)
	PUSHF(0);
	NEXT();
TARGET(fconst_1)
	PUSHF(1);
	NEXT();
TARGET(fconst_2)
	PUSHF(2);
	NEXT();
TARGET(fdiv)
	POP(1);
	STACKF(-1) /= STACKF(0);
	NEXT();
TARGET(fload)
	PUSHF(LOCALF(INFO(local)));
	NEXT();
TARGET(fload_0)
	PUSHF(LOCALF(0));
	NEXT();
TARGET(fload_1)
	PUSHF(LOCALF(1));
	NEXT();
TARGET(fload_2)
	PUSHF(LOCALF(2));
	NEXT();
TARGET(fload_3)
	PUSHF(LOCALF(3));
	NEXT();
TARGET(fmul)
	POP(1);
	STACKF(-1) *= STACKF(0);
	NEXT();
TARGET(fneg)
	STACKF(-1) = -STACKF(-1);
	NEXT();
TARGET(frem)
	POP(1);
	STACKF(-1) = fmod(STACKF(-1), STACKF(0));
	NEXT();
TARGET(freturn)
	env->retval.f = STACKF(-1);
	goto done;
TARGET(fstore)
	POP(1);
	LOCALF(INFO(local)) = STACKF(0);
	NEXT();
TARGET(fstore_0)
	POP(1);
	LOCALF(0) = STACKF(0);
	NEXT();
TARGET(fstore_1)
	POP(1);
	LOCALF(1) = STACKF(0);
	NEXT();
TARGET(fstore_2)
	POP(1);
	LOCALF(2) = STACKF(0);
	NEXT();
TARGET(fstore_3)
	POP(1);
	LOCALF(3) = STACKF(0);
	NEXT();
TARGET(fsub)
	POP(1);
	STACKF(-1) -= STACKF(0);
	NEXT();
TARGET(getfield_z)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKI(-1) = *(jboolean *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_b)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKI(-1) = *(jbyte *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_c)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKI(-1) = *(jchar *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_s)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKI(-1) = *(jshort *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_i)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKI(-1) = *(jint *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_j)
    {
    	_jc_object *obj;

	POP(-1);
	NULLPOINTERCHECK(obj = STACKL(-2));
	STACKJ(-2) = *(jlong *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_f)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKF(-1) = *(jfloat *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_d)
    {
    	_jc_object *obj;

	POP(-1);
	NULLPOINTERCHECK(obj = STACKL(-2));
	STACKD(-2) = *(jdouble *)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getfield_l)
    {
    	_jc_object *obj;

	NULLPOINTERCHECK(obj = STACKL(-1));
	STACKL(-1) = *(_jc_object **)((char *)obj + INFO(field).u.offset);
	NEXT();
    }
TARGET(getstatic)
    {
	_jc_field *const field = INFO(field).field;

	/* Initialize field's class */
	INITIALIZETYPE(field->class);

	/* Update instruction and execute again */
	switch (_jc_sig_types[(u_char)*field->signature]) {
	case _JC_TYPE_BOOLEAN:
		pc->action = _jc_interp_targets[_JC_getstatic_z];
		break;
	case _JC_TYPE_BYTE:
		pc->action = _jc_interp_targets[_JC_getstatic_b];
		break;
	case _JC_TYPE_CHAR:
		pc->action = _jc_interp_targets[_JC_getstatic_c];
		break;
	case _JC_TYPE_SHORT:
		pc->action = _jc_interp_targets[_JC_getstatic_s];
		break;
	case _JC_TYPE_INT:
		pc->action = _jc_interp_targets[_JC_getstatic_i];
		break;
	case _JC_TYPE_FLOAT:
		pc->action = _jc_interp_targets[_JC_getstatic_f];
		break;
	case _JC_TYPE_LONG:
		pc->action = _jc_interp_targets[_JC_getstatic_j];
		break;
	case _JC_TYPE_DOUBLE:
		pc->action = _jc_interp_targets[_JC_getstatic_d];
		break;
	case _JC_TYPE_REFERENCE:
		pc->action = _jc_interp_targets[_JC_getstatic_l];
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
		break;
	}
	RERUN();
    }
TARGET(getstatic_z)
	PUSHI(*(jboolean *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_b)
	PUSHI(*(jbyte *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_c)
	PUSHI(*(jchar *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_s)
	PUSHI(*(jshort *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_i)
	PUSHI(*(jint *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_j)
	PUSHJ(*(jlong *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_f)
	PUSHF(*(jfloat *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_d)
	PUSHD(*(jdouble *)INFO(field).u.data);
	NEXT();
TARGET(getstatic_l)
	PUSHL(*(_jc_object **)INFO(field).u.data);
	NEXT();
TARGET(goto)
	JUMP(INFO(target));
TARGET(i2b)
	STACKI(-1) = (jbyte)STACKI(-1);
	NEXT();
TARGET(i2c)
	STACKI(-1) = (jchar)STACKI(-1);
	NEXT();
TARGET(i2d)
	POP(-1);
	STACKD(-2) = STACKI(-2);
	NEXT();
TARGET(i2f)
	STACKF(-1) = STACKI(-1);
	NEXT();
TARGET(i2l)
	POP(-1);
	STACKJ(-2) = STACKI(-2);
	NEXT();
TARGET(i2s)
	STACKI(-1) = (jshort)STACKI(-1);
	NEXT();
TARGET(iadd)
	POP(1);
	STACKI(-1) += STACKI(0);
	NEXT();
TARGET(iaload)
    {
	_jc_int_array *array;
	jint index;

	POP(1);
	array = (_jc_int_array *)STACKL(-1);
	index = STACKI(0);
	ARRAYCHECK(array, index);
	STACKI(-1) = array->elems[index];
	NEXT();
    }
TARGET(iand)
	POP(1);
	STACKI(-1) &= STACKI(0);
	NEXT();
TARGET(iastore)
    {
	_jc_int_array *array;
	jint index;

	POP(3);
	array = (_jc_int_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	array->elems[index] = STACKI(2);
	NEXT();
    }
TARGET(iconst_m1)
	PUSHI(-1);
	NEXT();
TARGET(iconst_0)
	PUSHI(0);
	NEXT();
TARGET(iconst_1)
	PUSHI(1);
	NEXT();
TARGET(iconst_2)
	PUSHI(2);
	NEXT();
TARGET(iconst_3)
	PUSHI(3);
	NEXT();
TARGET(iconst_4)
	PUSHI(4);
	NEXT();
TARGET(iconst_5)
	PUSHI(5);
	NEXT();
TARGET(idiv)
	POP(1);
	if (_JC_UNLIKELY(STACKI(0) == 0))
		goto arithmetic_exception;
	STACKI(-1) /= STACKI(0);
	NEXT();
TARGET(if_acmpeq)
	POP(2);
	if (STACKL(0) == STACKL(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_acmpne)
	POP(2);
	if (STACKL(0) != STACKL(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_icmpeq)
	POP(2);
	if (STACKI(0) == STACKI(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_icmpne)
	POP(2);
	if (STACKI(0) != STACKI(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_icmplt)
	POP(2);
	if (STACKI(0) < STACKI(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_icmpge)
	POP(2);
	if (STACKI(0) >= STACKI(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_icmpgt)
	POP(2);
	if (STACKI(0) > STACKI(1))
		JUMP(INFO(target));
	NEXT();
TARGET(if_icmple)
	POP(2);
	if (STACKI(0) <= STACKI(1))
		JUMP(INFO(target));
	NEXT();
TARGET(ifeq)
	POP(1);
	if (STACKI(0) == 0)
		JUMP(INFO(target));
	NEXT();
TARGET(ifne)
	POP(1);
	if (STACKI(0) != 0)
		JUMP(INFO(target));
	NEXT();
TARGET(iflt)
	POP(1);
	if (STACKI(0) < 0)
		JUMP(INFO(target));
	NEXT();
TARGET(ifge)
	POP(1);
	if (STACKI(0) >= 0)
		JUMP(INFO(target));
	NEXT();
TARGET(ifgt)
	POP(1);
	if (STACKI(0) > 0)
		JUMP(INFO(target));
	NEXT();
TARGET(ifle)
	POP(1);
	if (STACKI(0) <= 0)
		JUMP(INFO(target));
	NEXT();
TARGET(ifnonnull)
	POP(1);
	if (STACKL(0) != NULL)
		JUMP(INFO(target));
	NEXT();
TARGET(ifnull)
	POP(1);
	if (STACKL(0) == NULL)
		JUMP(INFO(target));
	NEXT();
TARGET(iinc)
	LOCALI(INFO(iinc).local) += INFO(iinc).value;
	NEXT();
TARGET(iload)
	PUSHI(LOCALI(INFO(local)));
	NEXT();
TARGET(iload_0)
	PUSHI(LOCALI(0));
	NEXT();
TARGET(iload_1)
	PUSHI(LOCALI(1));
	NEXT();
TARGET(iload_2)
	PUSHI(LOCALI(2));
	NEXT();
TARGET(iload_3)
	PUSHI(LOCALI(3));
	NEXT();
TARGET(imul)
	POP(1);
	STACKI(-1) *= STACKI(0);
	NEXT();
TARGET(ineg)
	STACKI(-1) = -STACKI(-1);
	NEXT();
TARGET(instanceof)
    {
    	jint result;

	stack_frame.pc = pc;
	if (_JC_UNLIKELY((result = _jc_instance_of(env,
	    STACKL(-1), INFO(type))) == -1))
		goto exception;
	STACKI(-1) = result;
	NEXT();
    }
TARGET(invokestatic)
    {
	/* Initialize method's class */
	INITIALIZETYPE(INFO(invoke).method->class);

	/* Update instruction and execute again */
	pc->action = actions[_JC_invokestatic2];
	RERUN();
    }
TARGET(invokespecial)
    {
	const _jc_invoke *const invoke = &INFO(invoke);

	/* Get method */
	imethod = invoke->method;

	/* Check for null */
	NULLPOINTERCHECK(STACKL(-invoke->pop));

	/* Invoke */
	goto invoke;
    }
TARGET(invokevirtual)
    {
	const _jc_invoke *const invoke = &INFO(invoke);
	_jc_object *obj;
	_jc_type *vtype;

	/* Check for null */
	NULLPOINTERCHECK(obj = STACKL(-invoke->pop));

	/* Resolve virtual method */
	vtype = _JC_LW_TEST(obj->lockword, ARRAY) ?
	    env->vm->boot.types.Object : obj->type;
	imethod = vtype->u.nonarray.mtable[invoke->method->vtable_index];

	/* Invoke */
	goto invoke;
    }
TARGET(invokestatic2)
    {
	const _jc_invoke *const invoke = &INFO(invoke);

	/* Get method */
	imethod = invoke->method;

	/* Invoke */
	goto invoke;
    }
TARGET(invokeinterface)
    {
	const _jc_invoke *const invoke = &INFO(invoke);
	_jc_method *const *methodp;
	_jc_method *quick;
	_jc_object *obj;

	/* Get interface method */
	imethod = invoke->method;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(imethod->class, INTERFACE));

	/* Check for null */
	NULLPOINTERCHECK(obj = STACKL(-invoke->pop));

	/* Verify object implements the interface */
	stack_frame.pc = pc;
	switch (_jc_assignable_from(env, obj->type, imethod->class)) {
	case 0:
		_jc_post_exception_msg(env,
		    _JC_IncompatibleClassChangeError,
		    "`%s' does not implement interface `%s'",
		    obj->type->name, imethod->class->name);
		goto exception;
	case 1:
		break;
	case -1:
		goto exception;
	}

	/* Sanity check */
	_JC_ASSERT(obj->type->imethod_quick_table != NULL);
	_JC_ASSERT(obj->type->imethod_hash_table != NULL);
	_JC_ASSERT(imethod->signature_hash_bucket
	    < _JC_IMETHOD_HASHSIZE);

	/* Try quick hash table lookup */
	if (_JC_LIKELY((quick = obj->type->imethod_quick_table[
	    imethod->signature_hash_bucket]) != NULL)) {
		imethod = quick;
		goto got_method;
	}

	/* Lookup interface method entry point in hash table */
	if (_JC_UNLIKELY((methodp = obj->type->imethod_hash_table[
	    imethod->signature_hash_bucket]) == NULL))
		goto not_found;
	do {
		_jc_method *const entry = *methodp;

		if (strcmp(entry->name, imethod->name) == 0
		    && strcmp(entry->signature,
		      imethod->signature) == 0) {
			imethod = entry;
			goto got_method;
		}
		methodp++;
	} while (*methodp != NULL);
	if (_JC_UNLIKELY(*methodp == NULL)) {
not_found:		_jc_post_exception_msg(env, _JC_AbstractMethodError,
		    "%s.%s%s invoked from %s.%s%s on a %s",
		    imethod->class->name, imethod->name,
		    imethod->signature, method->class->name,
		    method->name, method->signature, obj->type->name);
		goto exception;
	}

got_method:
	/* Verify method is public */
	if (_JC_UNLIKELY(!_JC_ACC_TEST(imethod, PUBLIC))) {
		_jc_post_exception_msg(env, _JC_IllegalAccessError,
		    "%s.%s%s invoked from %s.%s%s on a %s",
		    imethod->class->name, imethod->name,
		    imethod->signature, method->class->name,
		    method->name, method->signature, obj->type->name);
		goto exception;
	}

	/* Invoke it */
	goto invoke;
    }

invoke:
    {
	const _jc_invoke *const invoke = &INFO(invoke);
	jint status;

	/* Pop the stack, leaving parameters above the top */
	POP(invoke->pop);

	/* Invoke the method */
	stack_frame.pc = pc;
	if (_JC_UNLIKELY(_JC_ACC_TEST(imethod, NATIVE))) {

		/* Invoke native method */
		status = _jc_invoke_native_method(env, imethod, JNI_TRUE, sp);
	} else {

		/* Invoke Java method */
		env->sp = sp;
		status = _jc_interp(env, imethod);
		env->sp = locals + code->max_locals + code->max_stack;
	}

	/* Did method throw an exception? */
	if (_JC_UNLIKELY(status != JNI_OK))
		goto exception;

	/* Push return value, if any */
	switch (imethod->param_ptypes[imethod->num_parameters]) {
	case _JC_TYPE_BOOLEAN:
	case _JC_TYPE_BYTE:
	case _JC_TYPE_CHAR:
	case _JC_TYPE_SHORT:
	case _JC_TYPE_INT:
		PUSHI(env->retval.i);
		break;
	case _JC_TYPE_FLOAT:
		PUSHF(env->retval.f);
		break;
	case _JC_TYPE_REFERENCE:
		PUSHL(env->retval.l);
		break;
	case _JC_TYPE_LONG:
		PUSHJ(env->retval.j);
		break;
	case _JC_TYPE_DOUBLE:
		PUSHD(env->retval.d);
		break;
	case _JC_TYPE_VOID:
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
	}
	NEXT();
    }
TARGET(ior)
	POP(1);
	STACKI(-1) |= STACKI(0);
	NEXT();
TARGET(irem)
	POP(1);
	if (_JC_UNLIKELY(STACKI(0) == 0))
		goto arithmetic_exception;
	STACKI(-1) %= STACKI(0);
	NEXT();
TARGET(ireturn)
	env->retval.i = STACKI(-1);
	goto done;
TARGET(ishl)
	POP(1);
	STACKI(-1) <<= STACKI(0) & 0x1f;
	NEXT();
TARGET(ishr)
	POP(1);
	STACKI(-1) = _JC_ISHR(STACKI(-1), STACKI(0) & 0x1f);
	NEXT();
TARGET(istore)
	POP(1);
	LOCALI(INFO(local)) = STACKI(0);
	NEXT();
TARGET(istore_0)
	POP(1);
	LOCALI(0) = STACKI(0);
	NEXT();
TARGET(istore_1)
	POP(1);
	LOCALI(1) = STACKI(0);
	NEXT();
TARGET(istore_2)
	POP(1);
	LOCALI(2) = STACKI(0);
	NEXT();
TARGET(istore_3)
	POP(1);
	LOCALI(3) = STACKI(0);
	NEXT();
TARGET(isub)
	POP(1);
	STACKI(-1) -= STACKI(0);
	NEXT();
TARGET(iushr)
	POP(1);
	STACKI(-1) = _JC_IUSHR(STACKI(-1), STACKI(0) & 0x1f);
	NEXT();
TARGET(ixor)
	POP(1);
	STACKI(-1) ^= STACKI(0);
	NEXT();
TARGET(jsr)
	PUSHL((void *)(pc + 1));
	JUMP(INFO(target));
TARGET(l2d)
	STACKD(-2) = STACKJ(-2);
	NEXT();
TARGET(l2f)
	POP(1);
	STACKF(-1) = STACKJ(-1);
	NEXT();
TARGET(l2i)
	POP(1);
	STACKI(-1) = STACKJ(-1);
	NEXT();
TARGET(ladd)
	POP(2);
	STACKJ(-2) += STACKJ(0);
	NEXT();
TARGET(laload)
    {
	_jc_long_array *array;
	jint index;

	array = (_jc_long_array *)STACKL(-2);
	index = STACKI(-1);
	ARRAYCHECK(array, index);
	STACKJ(-2) = array->elems[index];
	NEXT();
    }
TARGET(land)
	POP(2);
	STACKJ(-2) &= STACKJ(0);
	NEXT();
TARGET(lastore)
    {
	_jc_long_array *array;
	jint index;

	POP(4);
	array = (_jc_long_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	array->elems[index] = STACKJ(2);
	NEXT();
    }
TARGET(lcmp)
	POP(3);
	STACKI(-1) = _JC_LCMP(STACKJ(-1), STACKJ(1));
	NEXT();
TARGET(lconst_0)
	PUSHJ(0);
	NEXT();
TARGET(lconst_1)
	PUSHJ(1);
	NEXT();
TARGET(ldc)
	*sp++ = *((_jc_word *)&INFO(constant));
	NEXT();
TARGET(ldc_string)
    {
    	_jc_string_info *const info = &INFO(string);
	_jc_resolve_info rinfo;
	_jc_object *string;

	/* Create intern'd string */
	stack_frame.pc = pc;
	if (_JC_UNLIKELY((string = _jc_new_intern_string(env,
	    info->utf8, strlen(info->utf8))) == NULL))
		goto exception;

	/* Create reference list with one reference */
	memset(&rinfo, 0, sizeof(rinfo));
	rinfo.loader = method->class->loader;
	rinfo.implicit_refs = &string;
	rinfo.num_implicit_refs = 1;

	/* Add implicit reference to string from class loader */
	if (_JC_UNLIKELY(_jc_merge_implicit_refs(env, &rinfo) != JNI_OK)) {
		_jc_post_exception_info(env);
		goto exception;
	}

	/* Update instruction and execute again */
	info->string = string;
	pc->action = actions[_JC_ldc];
	RERUN();
    }
TARGET(ldc2_w)
	*sp++ = ((_jc_word *)&INFO(constant))[0];
	*sp++ = ((_jc_word *)&INFO(constant))[1];
	NEXT();
TARGET(ldiv)
	POP(2);
	if (_JC_UNLIKELY(STACKJ(0) == 0))
		goto arithmetic_exception;
	STACKJ(-2) /= STACKJ(0);
	NEXT();
TARGET(lload)
	PUSHJ(LOCALJ(INFO(local)));
	NEXT();
TARGET(lload_0)
	PUSHJ(LOCALJ(0));
	NEXT();
TARGET(lload_1)
	PUSHJ(LOCALJ(1));
	NEXT();
TARGET(lload_2)
	PUSHJ(LOCALJ(2));
	NEXT();
TARGET(lload_3)
	PUSHJ(LOCALJ(3));
	NEXT();
TARGET(lmul)
	POP(2);
	STACKJ(-2) *= STACKJ(0);
	NEXT();
TARGET(lneg)
	STACKJ(-2) = -STACKJ(-2);
	NEXT();
TARGET(lookupswitch)
    {
	_jc_lookupswitch *const lsw = INFO(lookupswitch);
	_jc_lookup *entry;
	_jc_lookup key;

	POP(1);
	key.match = STACKI(0);
	entry = bsearch(&key, lsw->pairs, lsw->num_pairs,
	    sizeof(*lsw->pairs), _jc_lookup_compare);
	JUMP(entry != NULL ? entry->target : lsw->default_target);
    }
TARGET(lor)
	POP(2);
	STACKJ(-2) |= STACKJ(0);
	NEXT();
TARGET(lrem)
	POP(2);
	if (_JC_UNLIKELY(STACKJ(0) == 0))
		goto arithmetic_exception;
	STACKJ(-2) %= STACKJ(0);
	NEXT();
TARGET(lreturn)
	env->retval.j = STACKJ(-2);
	goto done;
TARGET(lshl)
	POP(1);
	STACKJ(-2) <<= STACKI(0) & 0x3f;
	NEXT();
TARGET(lshr)
	POP(1);
	STACKJ(-2) = _JC_LSHR(STACKJ(-2), STACKI(0));
	NEXT();
TARGET(lstore)
	POP(2);
	LOCALJ(INFO(local)) = STACKJ(0);
	NEXT();
TARGET(lstore_0)
	POP(2);
	LOCALJ(0) = STACKJ(0);
	NEXT();
TARGET(lstore_1)
	POP(2);
	LOCALJ(1) = STACKJ(0);
	NEXT();
TARGET(lstore_2)
	POP(2);
	LOCALJ(2) = STACKJ(0);
	NEXT();
TARGET(lstore_3)
	POP(2);
	LOCALJ(3) = STACKJ(0);
	NEXT();
TARGET(lsub)
	POP(2);
	STACKJ(-2) -= STACKJ(0);
	NEXT();
TARGET(lushr)
	POP(1);
	STACKJ(-2) = _JC_LUSHR(STACKJ(-2), STACKI(0));
	NEXT();
TARGET(lxor)
	POP(2);
	STACKJ(-2) ^= STACKJ(0);
	NEXT();
TARGET(monitorenter)
	POP(1);
	NULLPOINTERCHECK(STACKL(0));
	stack_frame.pc = pc;
	if (_JC_UNLIKELY(_jc_lock_object(env, STACKL(0)) != JNI_OK))
		goto exception;
	NEXT();
TARGET(monitorexit)
	POP(1);
	NULLPOINTERCHECK(STACKL(0));
	stack_frame.pc = pc;
	if (_JC_UNLIKELY(_jc_unlock_object(env, STACKL(0)) != JNI_OK))
		goto exception;
	NEXT();
TARGET(multianewarray)
    {
	_jc_multianewarray *const info = &INFO(multianewarray);
	_jc_array *array;
	jint *sizes;
	int i;

	POP(info->dims);
	sizes = &STACKI(0);			/* overwrite popped stack */
	if (sizeof(_jc_word) != sizeof(jint)) {
		for (i = 0; i < info->dims; i++)
			sizes[i] = STACKI(i);
	}
	stack_frame.pc = pc;
	if (_JC_UNLIKELY((array = _jc_new_multiarray(env,
	    info->type, info->dims, sizes)) == NULL))
		goto exception;
	PUSHL((_jc_object *)array);
	NEXT();
    }
TARGET(new)
    {
	_jc_object *obj;

	stack_frame.pc = pc;
	if (_JC_UNLIKELY((obj = _jc_new_object(env, INFO(type))) == NULL))
		goto exception;
	PUSHL(obj);
	NEXT();
    }
TARGET(newarray)
    {
	_jc_array *array;

	POP(1);
	stack_frame.pc = pc;
	if (_JC_UNLIKELY((array = _jc_new_array(env,
	    INFO(type), STACKI(0))) == NULL))
		goto exception;
	PUSHL((_jc_object *)array);
	NEXT();
    }
TARGET(nop)
	NEXT();
TARGET(pop)
	POP(1);
	NEXT();
TARGET(pop2)
	POP(2);
	NEXT();
TARGET(putfield_z)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(jboolean *)((char *)STACKL(0)
	    + INFO(field).u.offset) = STACKI(1) & 0x01;
	NEXT();
TARGET(putfield_b)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(jbyte *)((char *)STACKL(0) + INFO(field).u.offset) = STACKI(1);
	NEXT();
TARGET(putfield_c)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(jchar *)((char *)STACKL(0) + INFO(field).u.offset) = STACKI(1);
	NEXT();
TARGET(putfield_s)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(jshort *)((char *)STACKL(0) + INFO(field).u.offset) = STACKI(1);
	NEXT();
TARGET(putfield_i)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(jint *)((char *)STACKL(0) + INFO(field).u.offset) = STACKI(1);
	NEXT();
TARGET(putfield_j)
	POP(3);
	NULLPOINTERCHECK(STACKL(0));
	*(jlong *)((char *)STACKL(0) + INFO(field).u.offset) = STACKJ(1);
	NEXT();
TARGET(putfield_f)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(jfloat *)((char *)STACKL(0) + INFO(field).u.offset) = STACKF(1);
	NEXT();
TARGET(putfield_d)
	POP(3);
	NULLPOINTERCHECK(STACKL(0));
	*(jdouble *)((char *)STACKL(0) + INFO(field).u.offset) = STACKD(1);
	NEXT();
TARGET(putfield_l)
	POP(2);
	NULLPOINTERCHECK(STACKL(0));
	*(_jc_object **)((char *)STACKL(0) + INFO(field).u.offset) = STACKL(1);
	NEXT();
TARGET(putstatic)
    {
	_jc_field *const field = INFO(field).field;

	/* Initialize field's class */
	INITIALIZETYPE(field->class);

	/* Update instruction and execute again */
	switch (_jc_sig_types[(u_char)*field->signature]) {
	case _JC_TYPE_BOOLEAN:
		pc->action = actions[_JC_putstatic_z];
		break;
	case _JC_TYPE_BYTE:
		pc->action = actions[_JC_putstatic_b];
		break;
	case _JC_TYPE_CHAR:
		pc->action = actions[_JC_putstatic_c];
		break;
	case _JC_TYPE_SHORT:
		pc->action = actions[_JC_putstatic_s];
		break;
	case _JC_TYPE_INT:
		pc->action = actions[_JC_putstatic_i];
		break;
	case _JC_TYPE_FLOAT:
		pc->action = actions[_JC_putstatic_f];
		break;
	case _JC_TYPE_LONG:
		pc->action = actions[_JC_putstatic_j];
		break;
	case _JC_TYPE_DOUBLE:
		pc->action = actions[_JC_putstatic_d];
		break;
	case _JC_TYPE_REFERENCE:
		pc->action = actions[_JC_putstatic_l];
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
		break;
	}
	RERUN();
    }
TARGET(putstatic_z)
	POP(1);
	*(jboolean *)INFO(field).u.data = STACKI(0) & 0x01;
	NEXT();
TARGET(putstatic_b)
	POP(1);
	*(jbyte *)INFO(field).u.data = STACKI(0);
	NEXT();
TARGET(putstatic_c)
	POP(1);
	*(jchar *)INFO(field).u.data = STACKI(0);
	NEXT();
TARGET(putstatic_s)
	POP(1);
	*(jshort *)INFO(field).u.data = STACKI(0);
	NEXT();
TARGET(putstatic_i)
	POP(1);
	*(jint *)INFO(field).u.data = STACKI(0);
	NEXT();
TARGET(putstatic_j)
	POP(2);
	*(jlong *)INFO(field).u.data = STACKJ(0);
	NEXT();
TARGET(putstatic_f)
	POP(1);
	*(jfloat *)INFO(field).u.data = STACKF(0);
	NEXT();
TARGET(putstatic_d)
	POP(2);
	*(jdouble *)INFO(field).u.data = STACKD(0);
	NEXT();
TARGET(putstatic_l)
	POP(1);
	*(_jc_object **)INFO(field).u.data = STACKL(0);
	NEXT();
TARGET(ret)
	JUMP((_jc_insn *)LOCALL(INFO(local)));
TARGET(return)
	goto done;
TARGET(saload)
    {
	_jc_short_array *array;
	jint index;

	POP(1);
	array = (_jc_short_array *)STACKL(-1);
	index = STACKI(0);
	ARRAYCHECK(array, index);
	STACKI(-1) = array->elems[index];
	NEXT();
    }
TARGET(sastore)
    {
	_jc_short_array *array;
	jint index;

	POP(3);
	array = (_jc_short_array *)STACKL(0);
	index = STACKI(1);
	ARRAYCHECK(array, index);
	array->elems[index] = STACKI(2);
	NEXT();
    }
TARGET(swap)
    {
	_jc_word temp;

	temp = STACKW(-2);
	STACKW(-2) = STACKW(-1);
	STACKW(-1) = temp;
	NEXT();
    }
TARGET(tableswitch)
    {
	_jc_tableswitch *const tsw = INFO(tableswitch);
	jint key;

	POP(1);
	key = STACKI(0);
	JUMP((key >= tsw->low && key <= tsw->high) ?
	    tsw->targets[key - tsw->low] : tsw->default_target);
    }

periodic_check:
	if (_JC_UNLIKELY(_jc_thread_check(env) != JNI_OK))
		goto exception;
	ticker = PERIODIC_CHECK_TICKS;
	RERUN();

null_pointer_exception:
	stack_frame.pc = pc;
	_jc_post_exception(env, _JC_NullPointerException);
	goto exception;

arithmetic_exception:
	stack_frame.pc = pc;
	_jc_post_exception(env, _JC_ArithmeticException);
	goto exception;

exception:
    {
	int i;

	/* Sanity check */
	_JC_ASSERT(env->pending != NULL);

	/* Check this method for a matching trap */
	for (i = 0; i < code->num_traps; i++) {
		_jc_jvm *const vm = env->vm;
		_jc_interp_trap *const trap = &code->traps[i];
		_jc_object *e;

		/* See if trap matches */
		if (pc < trap->start || pc >= trap->end)
			continue;
		if ((e = _jc_retrieve_exception(env, trap->type)) == NULL)
			continue;

		/* Verbosity for caught exception */
		if ((env->vm->verbose_flags
		    & (1 << _JC_VERBOSE_EXCEPTIONS)) != 0) {
			_jc_printf(vm, "[verbose %s: caught via trap"
			    " %d (%d-%d) in %s.%s%s in thread %p: ",
			    _jc_verbose_names[_JC_VERBOSE_EXCEPTIONS],
			    i, trap->start, trap->end,
			    method->class->name, method->name,
			    method->signature, env);
			_jc_fprint_exception_headline(env, stdout, e);
			_jc_printf(vm, "]\n");
		}

		/* Clear stack, push exception, and proceed with handler */
		sp = locals + code->max_locals + 1;
		STACKL(-1) = e;
		JUMP(trap->target);
	}

	/* Exception not caught */
	status = JNI_ERR;
	goto finish;

fail:
	status = JNI_ERR;
	goto out;

done:
	status = JNI_OK;

finish:
	/* Sanity check */
	_JC_ASSERT(status == JNI_OK || env->pending != NULL);

	/* De-synchronize if necessary */
	if (_JC_UNLIKELY(lock != NULL)) {
		_jc_rvalue retval;

		/* Temporarily save return value */
		retval = env->retval;

		/* Unlock monitor */
		if (_JC_UNLIKELY(_jc_unlock_object(env, lock) != JNI_OK))
			status = JNI_ERR;

		/* Restore return value */
		env->retval = retval;
	}

out:
	/* Pop Java stack frame */
	env->java_stack = stack_frame.next;
	env->sp = locals;

	/* Done */
	return status;
    }
}

/*
 * Fill in the interpreter instruction target table.
 */
void
_jc_interp_get_targets(_jc_env *env)
{
	/* Get actions table via kludge */
	(void)_jc_interp(env, NULL);
	_jc_interp_targets = (const _jc_word *)env->retval.l;
}

/*
 * Comparison function for binary search of lookupswitch tables.
 */
static int
_jc_lookup_compare(const void *v1, const void *v2)
{
	const _jc_lookup *const l1 = v1;
	const _jc_lookup *const l2 = v2;

	return (l1->match > l2->match) - (l1->match < l2->match);
}

/*
 * Look up a Java source file line number.
 */
int
_jc_interp_pc_to_jline(_jc_method *method, int index)
{
	_jc_method_code *const code = &method->code;
	_jc_linemap *base;
	int span;

	/* Sanity check */
	_JC_ASSERT(_JC_FLG_TEST(method->class, RESOLVED));
	_JC_ASSERT(index >= 0 && index < code->num_insns);

	/* Binary search for line number */
	for (base = code->linemaps, span = code->num_linemaps;
	    span != 0; span >>= 1) {
		_jc_linemap *const sample = &base[span >> 1];

		if (index <= sample->index)
			continue;
		if (index > (sample + 1)->index) {
			base = sample + 1;
			span--;
			continue;
		}
		return sample->line;
	}

	/* Not found */
	return 0;
}

/*
 * Type-specific gateway functions into _jc_vinterp()
 */
#define _JC_INTERP_ENTRY(_letter, _name, _type, _rtn)			\
_type									\
_jc_ ## _name ## _ ## _letter(_jc_env *env, ...)			\
{									\
	va_list args;							\
									\
	va_start(args, env);						\
	_jc_v ## _name(env, args);					\
	va_end(args);							\
	return _rtn;							\
}
_JC_INTERP_ENTRY(z, interp, jboolean, (jboolean)env->retval.i)
_JC_INTERP_ENTRY(b, interp, jbyte, (jbyte)env->retval.i)
_JC_INTERP_ENTRY(c, interp, jchar, (jchar)env->retval.i)
_JC_INTERP_ENTRY(s, interp, jshort, (jshort)env->retval.i)
_JC_INTERP_ENTRY(i, interp, jint, env->retval.i)
_JC_INTERP_ENTRY(j, interp, jlong, env->retval.j)
_JC_INTERP_ENTRY(f, interp, jfloat, env->retval.f)
_JC_INTERP_ENTRY(d, interp, jdouble, env->retval.d)
_JC_INTERP_ENTRY(l, interp, _jc_object *, env->retval.l)
_JC_INTERP_ENTRY(v, interp, void, )
_JC_INTERP_ENTRY(z, interp_native, jboolean, (jboolean)env->retval.i)
_JC_INTERP_ENTRY(b, interp_native, jbyte, (jbyte)env->retval.i)
_JC_INTERP_ENTRY(c, interp_native, jchar, (jchar)env->retval.i)
_JC_INTERP_ENTRY(s, interp_native, jshort, (jshort)env->retval.i)
_JC_INTERP_ENTRY(i, interp_native, jint, env->retval.i)
_JC_INTERP_ENTRY(j, interp_native, jlong, env->retval.j)
_JC_INTERP_ENTRY(f, interp_native, jfloat, env->retval.f)
_JC_INTERP_ENTRY(d, interp_native, jdouble, env->retval.d)
_JC_INTERP_ENTRY(l, interp_native, _jc_object *, env->retval.l)
_JC_INTERP_ENTRY(v, interp_native, void, )

/*
 * Entry point for interpreted methods when invoked from JCNI methods.
 * We get here by way of a trampoline set up by _jc_build_trampoline(),
 * which also sets env->interp to the method we are going to interpret.
 */
static void
_jc_vinterp(_jc_env *env, va_list args)
{
#ifndef NDEBUG
	const jboolean was_interpreting = env->interpreting;
#endif
	jboolean allocated_stack = JNI_FALSE;
	_jc_method *const method = env->interp;
	_jc_word *sp;
	jint status;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, NATIVE));
	_JC_ASSERT(_JC_FLG_TEST(method->class, RESOLVED));

	/* For a brand new thread, allocate its Java stack here */
	if (env->sp == NULL) {
		_jc_jvm *const vm = env->vm;

		/* Sanity check */
		_JC_ASSERT(env->stack_data == NULL);
		_JC_ASSERT(env->stack_data_end == NULL);

		/* Allocate Java stack */
		if ((env->stack_data = _jc_vm_alloc(env,
		    vm->java_stack_size * sizeof(*env->stack_data))) == NULL) {
			_jc_post_exception_info(env);
			_jc_throw_exception(env);
		}
		env->stack_data_end = env->stack_data
		    + vm->java_stack_size - _JC_JAVA_STACK_MARGIN;
		env->sp = env->stack_data;
		allocated_stack = JNI_TRUE;
	}

	/* Place paramters over top of the stack like _jc_interp() expects */
	sp = env->sp;

	/* Check Java stack overflow */
	if (sp + 1 + method->code.num_params2 > env->stack_data_end) {
		_jc_post_exception(env, _JC_StackOverflowError);
		_jc_throw_exception(env);
	}

	/* Push 'this' if method is non-static */
	if (!_JC_ACC_TEST(method, STATIC)) {
		_jc_object *const this = va_arg(args, _jc_object *);

		_JC_ASSERT(this != NULL);
		*sp++ = (_jc_word)this;
	}

	/* Push method parameters, occupying two slots for long/double */
	for (i = 0; i < method->num_parameters; i++) {
		switch (method->param_ptypes[i]) {
		case _JC_TYPE_BOOLEAN:
			*sp++ = (jint)(jboolean)va_arg(args, jint);
			break;
		case _JC_TYPE_BYTE:
			*sp++ = (jint)(jbyte)va_arg(args, jint);
			break;
		case _JC_TYPE_CHAR:
			*sp++ = (jint)(jchar)va_arg(args, jint);
			break;
		case _JC_TYPE_SHORT:
			*sp++ = (jint)(jshort)va_arg(args, jint);
			break;
		case _JC_TYPE_INT:
			*sp++ = va_arg(args, jint);
			break;
		case _JC_TYPE_FLOAT:
		    {
			jfloat param = (jfloat)va_arg(args, jdouble);

			memcpy(sp, &param, sizeof(param));
			sp++;
			break;
		    }
		case _JC_TYPE_LONG:
		    {
			jlong param = va_arg(args, jlong);

			memcpy(sp, &param, sizeof(param));
			sp += 2;
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			jdouble param = va_arg(args, jdouble);

			memcpy(sp, &param, sizeof(param));
			sp += 2;
			break;
		    }
		case _JC_TYPE_REFERENCE:
			*sp++ = (_jc_word)va_arg(args, _jc_object *);
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}
	_JC_ASSERT(sp - env->sp == method->code.num_params2
	    + !_JC_ACC_TEST(method, STATIC));

#ifndef NDEBUG
        /* Signals are not OK now */
	env->interpreting = JNI_TRUE;
#endif

	/* Invoke method */
	status = _jc_interp(env, method);

#ifndef NDEBUG
        /* Restore debug flag */
	env->interpreting = was_interpreting;
#endif

	/* Free Java stack */
	if (allocated_stack) {
		_JC_ASSERT(env->sp == env->stack_data);
		_jc_vm_free(&env->stack_data);
		env->stack_data_end = NULL;
		env->sp = NULL;
	}

	/* Throw exception if any */
	if (status != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * Same thing as _jc_vinterp() but for native methods.
 */
static void
_jc_vinterp_native(_jc_env *env, va_list args)
{
	_jc_method *const method = env->interp;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, NATIVE));
	_JC_ASSERT(_JC_FLG_TEST(method->class, RESOLVED));

	/* Invoke method */
	if (_jc_invoke_native_method(env, method, JNI_FALSE, args) != JNI_OK)
		_jc_throw_exception(env);
}

