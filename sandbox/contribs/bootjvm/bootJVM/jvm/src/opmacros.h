/*!
 * @file opmacros.h
 *
 * @brief Java Virtual Machine inner loop execution support macros.
 *
 * Much of the JVM inner loop is repetitious, yet not so much so
 * that opcodes can simply be grouped together completely.  This set
 * of macros is used to implement almost all of the opcodes, sometimes
 * in their entirety.
 *
 *
 * @section Control
 *
 * \$URL$
 *
 * \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 * 
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * 
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_HEADER_COPYRIGHT_APACHE(opmacros, h,
"$URL$",
"$Id$");


/*!
 * @name Operand retrieval macros.
 *
 * @brief Fetch operand(s) for an opcode using these macros.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Retrieve a one-byte operand that the PC points to.
 *
 *
 * Store the one-byte operand into the requested @link #u1 u1@endlink
 * variable, then increment the program counter to the next byte code
 * following it.
 *
 * @param u1var  Name of a @link #u1 u1@endlink variable that will
 *               receive the single byte of operand from with the
 *               instruction.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e pcode         Read byte codes
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(u1var)
 *                           Receives operand contents
 * </li>
 * </ul>
 *
 */
#define GET_U1_OPERAND(u1var)             \
    u1var = *((u1 *) &pcode[pc->offset]); \
    pc->offset += sizeof(u1)
 

/*!
 * @brief Retrieve a two-byte operand that the PC points to.
 *
 *
 * Store the two-byte operand into the requested @link #u2 u2@endlink
 * variable, then increment the program counter to the next byte code
 * following it.
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that will
 *               receive the two bytes of operand from with the
 *               instruction.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e pcode         Read byte codes
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(u2var)
 *                           Receives operand contents
 * </li>
 * </ul>
 *
 */
#define GET_U2_OPERAND(u2var)                  \
    u2var = GETRS2((u2 *) &pcode[pc->offset]); \
    pc->offset += sizeof(u2)
 

/*!
 * @brief Retrieve a four-byte operand that the PC points to.
 *
 *
 * Store the four-byte operand into the requested @link #u1 u1@endlink
 * variable, then increment the program counter to the next byte code
 * following it.
 *
 * @param u4var  Name of a @link #u4 u4@endlink variable that will
 *               receive the four bytes of operand from with the
 *               instruction.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e pcode         Read byte codes
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(u4var)
 *                           Receives operand contents
 * </li>
 * </ul>
 *
 */
#define GET_U4_OPERAND(u4var)                  \
    u4var = GETRI4((u4 *) &pcode[pc->offset]); \
    pc->offset += sizeof(u4)
 

/*!
 * @brief Retrieve 1- or 2-byte index, depending on @b WIDE context
 *
 * If came from a @b WIDE opcode, get 2-byte index as the operand
 * for this opcode, otherwise, get 1-byte index as the operand.
 * Then convert that value into a (@link #jint jint@endlink) for
 * use as the index into the local variable array in the current
 * stack frame.
 *
 *
 * @param jintvar  Any variable of type (@link #jint jint@endlink)
 *
 * @param ldadjust Integer zero (0) or one (1) to account for the added
 *                 local variable slot used by (@link #jdouble
                   jdouble@endlink) and (@link #jlong jlong@endlink)
 *                 local variables.  Zero is for all single-word types,
 *                 (@link #jbyte jbyte@endlink) through (@link #jint
                   jint@endlink), where one is for these two other
 *                 types.  This parameter is used to check that
 *                 the @b jintvar index is not out of range, and this
 *                 adjustment must be used to check for the second
 *                 word of the last local variable in case it is of
 *                 one of the two affected types.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e iswide contains the state of whether or not the
 *                    @b WIDE opcode immediately preceded this opcode.
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(jintvar)
 *                           Any @link #jint jint@endlink variable may
 *                           be used.  It holds the resulting index.
 * </li>
 * <li>  @c @e op1u1 used for scratch storage when not using a
 *                   @b WIDE index
 * </li>
 * <li>  @c @e op1u2 used for scratch storage when a @b WIDE index
 *                   is being used.
 * </li>
 * <li>  @c @e iswide is reset to @link #rfalse rfalse@endlink after
 *                    reading its current state.
 * </li>
 * </ul>
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
           if requested local variable is out of bounds@endlink.
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-6 Is @b VerifyError the proper
 *       error to throw?  Or is there one more specific to this
 *       problem?
 */
#define GET_WIDE_OR_NORMAL_INDEX(jintvar, ldadjust)             \
    if (rtrue == iswide)                                        \
    {                                                           \
        GET_U2_OPERAND(op1u2);                                  \
        jintvar = (jint) (jushort) op1u2;                       \
    }                                                           \
    else                                                        \
    {                                                           \
        GET_U1_OPERAND(op1u1);                                  \
        jintvar = (jint) (jubyte) op1u1;                        \
    }                                                           \
    /* Check index larger than number of local variables */     \
    if (jintvar + ldadjust >=                                   \
        STACK(thridx,                                           \
              JVMREG_FRAME_CURRENT_LOCAL_STORAGE_SIZE(thridx))) \
    {                                                           \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
    }                                                           \
    iswide = rfalse; /* Extra ; */


/*@} */ /* End of grouped definitions */


/*!
 * @name Type cast suppression macros.
 *
 * @brief Most opcode functional operations assume a
 * (@link #jint jint@endlink) data type, but some need to be typeless.
 * Therefore, these macros are supplied to assist in moving data
 * around without @e any change to the actual binary contents,
 * regardless of the target data type.  This is effectively means a
 * suppression of type casting.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Force conversion of any Java type variable
 * of @c @b sizeof(jint) into a @link #jfloat jfloat@endlink
 * variable, but without conversion of contents.
 *
 *
 * This macro is typically used to move a
 * @link #jint jint@endlink into a @link #jint jint@endlink
 * word, but suppress type conversion between the
 * source and destination variables.
 *
 * @warning For comments on the dangers of using this macro,
 *          please refer to @link #FORCE_JINT() FORCE_JINT()@endlink.
 *
 *
 * @param var_sizeofjint  Any 32-bit variable.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(var_sizeofjint)
 *                           Any 32-bit variable
 * </li>
 * </ul>
 *
 *
 * @returns (jfloat) version of @b var_sizeofjint without conversion
 *          of contents (such as jint-to-jfloat might want to do).
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-4 A careful review of this macro
 *       across different compilers is very much in order.
 *
 */
#define FORCE_JFLOAT(var_sizeofjint) \
    (*((jfloat *) ((jvoid *) &var_sizeofjint)))


/*!
 * @brief Force conversion of any Java type variable
 * of @c @b sizeof(jint) into a @link #jint jint@endlink
 * variable, but without conversion of contents.
 *
 *
 * This macro is typically used to move a
 * @link #jvm_object_hash jobject@endlink reference or a
 * @link #jfloat jfloat@endlink into a @link #jint jint@endlink
 * word, but suppress type conversion between the
 * source and destination variables.  It derives the
 * address of the 32-bit source value, casts it as a
 * pointer to the destination data type, then extracts
 * that type.
 *
 * @warning This macro @e must have a 32-bit word as its source.
 *          For use with smaller types, perform a widening conversion
 *          first (such as @link #jboolean jboolean@endlink) to
 *          @link #jint jint@endlink.  Then and only then will
 *          the target type work correctly, for no attempt is made
 *          to validate the contents of the input parameter.
 *
 * @warning Since this macro takes the address of its source parameter,
 *          it will only work for variables, not for expressions!
 *
 *
 * @param var_sizeofjint  Any 32-bit variable.  If it is a smaller
 *                        type, such as (jboolean), perform a
 *                        widening conversion into (jint) first.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(var_sizeofjint)
 *                           Any 32-bit variable
 * </li>
 * </ul>
 *
 *
 * @returns (jint) version of @b var_sizeofjint without conversion
 *          of contents (such as jfloat-to-jint might want to do).
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-2 A careful review of this macro
 *       across different compilers is very much in order.
 *
 */
#define FORCE_JINT(var_sizeofjint) \
    (*((jint *) ((jvoid *) &var_sizeofjint)))


/*!
 * @brief Force conversion of any Java type variable
 * of @c @b sizeof(jlong) into a @link #jlong jlong@endlink
 * variable, but without conversion of contents.
 *
 *
 * This macro is typically used to move a
 * @link #jdouble jdouble@endlink into a @link #jlong jlong@endlink
 * word, but suppress type conversion between the
 * source and destination variables.  It derives the
 * address of the 64-bit source value, casts it as a
 * pointer to the destination data type, then extracts
 * that type.
 *
 * @warning This macro @e must have a 64-bit word as its source.
 *          No no attempt is made to validate the contents of
 *          the input parameter.
 *
 * @warning Since this macro takes the address of its source parameter,
 *          it will only work for variables, not for expressions!
 *
 *
 * @param var_sizeofjlong Any 64-bit variable.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(var_sizeofjlong)
 *                           Any 64-bit variable
 * </li>
 * </ul>
 *
 *
 * @returns (jlong) version of @b var_sizeofjlong without conversion
 *          of contents (such as jdouble-to-jlong might want to do).
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-3 A careful review of this macro
 *       across different compilers is very much in order.
 *
 */
#define FORCE_JLONG(var_sizeofjlong) \
    (*((jlong *) ((jvoid *) &var_sizeofjlong)))


/*!
 * @brief Force nothing at all.  Used by
 * @link #SINGLE_ARITHMETIC_BINARY() SINGLE_ARITHMETIC_BINARY()@endlink
 * in support of non-floating point data types.  Floating point
 * arithmetic will use @link #FORCE_JINT() FORCE_JINT@endlink there.
 *
 *
 * @param any_var  Any variable name.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(any_var)  Any variable
 * </li>
 * </ul>
 *
 *
 * @returns exactly what was passed in through @e var_sizeofjint
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-5 A careful review of this
 *       macro across different compilers is very much in order.
 *
 */
#define FORCE_NOTHING(any_var) (any_var)


/*@} */ /* End of grouped definitions */


/*!
 * @name Reference variable verification macros.
 *
 * @brief Verify some basic features of a particular object hash.
 *
 */

/*@{ */ /* Begin grouped definitions */


/*!
 * @brief Verify that an object reference is to a one-dimensional array
 * of a given primative type and is within array bounds.
 *
 * It is assumed that the object hash is valid, having been passed
 * through @link #VERIFY_OBJECT_HASH() VERIFY_OBJECT_HASH@endlink first.
 * 
 *
 * @param objhash  Object hash of an object reference from the JVM stack
 *
 * @param basetype A @link #BASETYPE_CHAR_B BASETYPE_CHAR_x@endlink
 *                 to verify that array is of this type.
 *
 * @param arridx   Index into array
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDSEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDSEXCEPTION
           if index is not within limits of the array size@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR
 *         @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if array is not of the expected type@endlink.
 *
 * @todo HARMONY-6-jvm-opmacros.h-1 Needs unit testing with some
 *       real data.
 *
 */
#define VERIFY_ARRAY_REFERENCE(objhash, basetype, arridx)              \
    if ((!(OBJECT_STATUS_ARRAY & OBJECT(objhash).status))  ||          \
        (basetype != OBJECT(objhash).arraybasetype)        ||          \
        ( 1       != OBJECT(objhash).arraydims))                       \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INTERNALERROR);      \
/*NOTREACHED*/                                                         \
    }                                                                  \
    if (arridx >= OBJECT(objhash).arraylength)                         \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_EXCEPTION,          \
                    JVMCLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDSEXCEPTION);\
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Verify that an object reference is within supported range,
 * is in use, and does not refer to a null object.
 *
 * @param objhash  Object hash of an object reference from the JVM stack
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION
           if object hash is to a null object@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR
 *         @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if object hash is out of bounds@endlink.
 *
 */
#define VERIFY_OBJECT_HASH(objhash)                                    \
    CHECK_NOT_NULL_OBJECT_HASH(objhash);                               \
    if ((JVMCFG_MAX_OBJECTS   <= objhash)                 ||           \
        (!(OBJECT_STATUS_INUSE & OBJECT(objhash).status)) ||           \
        (OBJECT_STATUS_NULL & OBJECT(objhash).status))                 \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INTERNALERROR);      \
/*NOTREACHED*/                                                         \
    }


/*@} */ /* End of grouped definitions */


/*!
 * @name Data movement macros.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Retrieve value by data type from either class static field or
 * object instance field.
 *
 *
 * @param data_array_slot  Expression pointing to the class' or object's
 *                         @b XXX_data[] array, namely a
 *                         (@link #jvalue jvalue@endlink *).
 *                         Typically a fixed set of two expressions.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(data_array_slot)
 *                           Any @link #jvalue jvalue@endlink variable.
 * </li>
 * <li> @c @e pcpma_Fieldref CONSTANT_Fieldref_info pointer to current
 *                           field
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e jitmp1         Used for intermediate
 *                           @link #jint jint@endlink storage
 * </li>
 * <li> @c @e jitmp2         Used for intermediate
 *                           @link #jint jint@endlink storage
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-11 The various type casting games
 *       of integer/sub-integer and integer/float/double and
 *       integer/objhash need to be carefully scrutinized for
 *        correctness at run time.
 *
 * @todo HARMONY-6-jvm-opmacros.h-12 Is @B BASTYPE_CHAR_ARRAY a
 *       legal case for @b GETSTATIC and @b GETFIELD ?
 *
 */
#define GETDATA(data_array_slot)                                       \
    switch (pcpma_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM)      \
    {                                                                  \
        case BASETYPE_CHAR_B:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array_slot._jbyte);                       \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_C:                                          \
            PUSH(thridx,                                               \
                (jint) data_array_slot._jchar);                        \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_D:                                          \
            bytegames_split_jdouble(data_array_slot._jdouble,          \
                                    &jitmp1,                           \
                                    &jitmp2);                          \
            /*                                                         \
             * DO NOT push from a 64-bit word! @link #PUSH()           \
               PUSH@endlink was only designed to operate on 32-bit     \
             * data types.  Instead, use two instances.                \
             */                                                        \
            PUSH(thridx, jitmp1);                                      \
            PUSH(thridx, jitmp2);                                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_F:                                          \
            /*                                                         \
             * DO NOT pop into a jfloat!  This will consider           \
             * the source as an integer to be converted instead        \
             * of a 32-bit floating point word stored in a 32-bit      \
             * integer word on the stack.  Instead, use the            \
             * FORCE_JFLOAT() macro to sustain contents across         \
             * type boundaries.                                        \
             */                                                        \
            jitmp1 = FORCE_JINT(data_array_slot._jfloat);              \
            PUSH(thridx, jitmp1);                                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_I:                                          \
            PUSH(thridx,                                               \
                 (jint) /* ... redundant */ data_array_slot._jint);    \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_J:                                          \
            bytegames_split_jlong(data_array_slot._jlong,              \
                                  &jitmp1,                             \
                                  &jitmp2);                            \
            /*                                                         \
             * DO NOT push from a 64-bit word! @link #PUSH()           \
               PUSH@endlink was only designed to operate on 32-bit     \
             * data types.  Instead, use two instances.                \
             */                                                        \
            PUSH(thridx, jitmp1);                                      \
            PUSH(thridx, jitmp2);                                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_L:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array_slot._jobjhash);                    \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_S:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array_slot._jshort);                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_Z:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array_slot._jboolean);                    \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_ARRAY:                                      \
            PUSH(thridx,                                               \
                 (jint) data_array_slot._jarray);                      \
            break;                                                     \
                                                                       \
        case LOCAL_BASETYPE_ERROR:                                     \
        default:                                                       \
            /* Something is @e very wrong if code gets here */         \
            thread_throw_exception(thridx,                             \
                                   THREAD_STATUS_THREW_ERROR,          \
                                   JVMCLASS_JAVA_LANG_VERIFYERROR);    \
/*NOTREACHED*/                                                         \
            break;                                                     \
    }


/*!
 * @brief Store out value by data type into either class static field
 * or object instance field.
 *
 *
 * @param data_array_slot  Expression pointing to the class' or object's
 *                         @b XXX_data[] array, namely a
 *                         (@link #jvalue jvalue@endlink *).
 *                         Typically a fixed set of two expressions.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(data_array_slot)
 *                           Any @link #jvalue jvalue@endlink variable.
 * </li>
 * <li> @c @e pcpma_Fieldref CONSTANT_Fieldref_info pointer to current
 *                           field
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e jitmp1         Used for intermediate
 *                           @link #jint jint@endlink storage
 * </li>
 * <li> @c @e jitmp2         Used for intermediate
 *                           @link #jint jint@endlink storage
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-9 The various type casting games
 *       of integer/sub-integer and integer/float/double and
 *       integer/objhash need to be carefully scrutinized for
 *       correctness at run time.
 *
 * @todo HARMONY-6-jvm-opmacros.h-10 Is @b BASTYPE_CHAR_ARRAY a legal
 *       case for @b PUTSTATIC and @b PUTFIELD ?
 *
 */
#define PUTDATA(data_array_slot)                                       \
    switch (pcpma_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM)      \
    {                                                                  \
        case BASETYPE_CHAR_B:                                          \
            POP(thridx,                                                \
                data_array_slot._jbyte,                                \
                jbyte);                                                \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_C:                                          \
            POP(thridx,                                                \
                data_array_slot._jchar,                                \
                jchar);                                                \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_D:                                          \
            /*                                                         \
             * DO NOT pop into a 64-bit word!  @link #POP() POP@endlink\
             * was only designed to operate on 32-bit data types.      \
             * Instead, use two instances.  Besides, these halves      \
             * needs to get pushed through bytegames_combine_jdouble() \
             * anyway to retrieve the final                            \
             * @link #jdouble jdouble@endlink value.                   \
             */                                                        \
            POP(thridx, jitmp2, jint);                                 \
            POP(thridx, jitmp1, jint);                                 \
            data_array_slot._jdouble =                                 \
                bytegames_combine_jdouble(jitmp1, jitmp2);             \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_F:                                          \
            /*                                                         \
             * DO NOT pop into a jfloat!  This will consider           \
             * the source as an integer to be converted instead        \
             * of a 32-bit floating point word stored in a 32-bit      \
             * integer word on the stack.  Instead, use the            \
             * FORCE_JFLOAT() macro to sustain contents across         \
             * type boundaries.                                        \
             */                                                        \
            POP(thridx, jitmp1, jint);                                 \
            data_array_slot._jfloat = FORCE_JFLOAT(jitmp1);            \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_I:                                          \
            POP(thridx,                                                \
                data_array_slot._jint,                                 \
                jint);                                                 \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_J:                                          \
            /*                                                         \
             * DO NOT pop into a 64-bit word!  @link #POP() POP@endlink\
             * was only designed to operate on 32-bit data types.      \
             * Instead, use two instances.  Besides, these halves      \
             * needs to get pushed through bytegames_combine_jlong()   \
             * anyway to retrieve the final                            \
             * @link #jlong jlong@endlink value.                       \
             */                                                        \
            POP(thridx, jitmp2, jint);                                 \
            POP(thridx, jitmp1, jint);                                 \
            data_array_slot._jlong =                                   \
                bytegames_combine_jlong(jitmp1,jitmp2);                \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_L:                                          \
            POP(thridx,                                                \
                data_array_slot._jobjhash,                             \
                jvm_object_hash);                                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_S:                                          \
            POP(thridx,                                                \
                data_array_slot._jshort,                               \
                jshort);                                               \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_Z:                                          \
            POP(thridx,                                                \
                data_array_slot._jboolean,                             \
                jboolean);                                             \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_ARRAY:                                      \
            POP(thridx,                                                \
                data_array_slot._jarray,                               \
                jvm_object_hash);                                      \
            break;                                                     \
                                                                       \
        case LOCAL_BASETYPE_ERROR:                                     \
        default:                                                       \
            /* Something is @e very wrong if code gets here */         \
            thread_throw_exception(thridx,                             \
                                   THREAD_STATUS_THREW_ERROR,          \
                                   JVMCLASS_JAVA_LANG_VERIFYERROR);    \
/*NOTREACHED*/                                                         \
            break;                                                     \
    }

/*@} */ /* End of grouped definitions */


/*!
 * @name Arithmetic macros.
 *
 * @brief The arithmetic macros typically implement an entire
 * arithmetic or logical operation that uses two operands.
 * The single-operand (unary) arithmetic and logical operations are
 * typically implemented more explicitly.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Binary arithmetic and logic operations for
 * double-precision words
 *
 * Perform add, subtract, multiply, divide, remainder,
 * and, or, xor.  The word type is dependent on the types
 * of the input parameters, namely (@link #jlong jlong@endlink) and
 * (@link #jdouble jdouble@endlink).
 *
 *
 * @param var1        First of two operands
 *
 * @param var2        Second of two operands
 *
 * @param varint1     MS half of vars as @link #jint jint@endlink
 *
 * @param varint2     LS half of vars as @link #jint jint@endlink
 *
 * @param vartype     data type of @b var1 and @b var2
 *
 * @param _OPERATOR_  One of:  + - * / % & | ^
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
#define DOUBLE_ARITHMETIC_BINARY(var1, var2, varint1, varint2, \
                                 vartype, _OPERATOR_)          \
    POP(thridx, varint2, jint);                                \
    POP(thridx, varint1, jint);                                \
    var1 = bytegames_combine_##vartype(varint1, varint2);      \
                                                               \
    POP(thridx, varint2, jint);                                \
    POP(thridx, varint1, jint);                                \
    var2 = bytegames_combine_##vartype(varint1, varint2);      \
                                                               \
    var1 = var1 _OPERATOR_ var2;                               \
                                                               \
    bytegames_split_##vartype(var1, &varint1, &varint2);       \
    PUSH(thridx, varint1);                                     \
    PUSH(thridx, varint2); /* Extra ; */


/*!
 * @brief Binary arithmetic and logic operations for
 * single-precision words
 *
 * Perform add, subtract, multiply, divide, remainder, and, or, xor.
 * The word type is dependent on the types of the input parameters,
 * namely (@link #jint jint@endlink) and (@link #jfloat jfloat@endlink).
 * All sub-integer types use this version of the arithmetic macro.
 *
 *
 * @param var1        First of two integer/sub-integer operands
 *
 * @param var2        Second of two integer/sub-integer operands
 *
 * @param _OPERATOR_  One of:  + - * / % & | ^
 *
 * @param _FORCE_     One of: FORCE_JINT FORCE_NOTHING
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
#define SINGLE_ARITHMETIC_BINARY(var1, var2, _OPERATOR_, _FORCE_) \
    POP(thridx, var1, jint);                                      \
    POP(thridx, var2, jint);                                      \
                                                                  \
    var1 = var1 _OPERATOR_ var2;                                  \
                                                                  \
    PUSH(thridx, _FORCE_(var1)); /* Extra ; */


/*@} */ /* End of grouped definitions */


/*!
 * @name Floating point arithmetic special case condition handler macros
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Check special cases of double-precision floating point numbers
 * as part of conversion to other formats.
 *
 *
 * The opcodes @b D2x where 'x' is long, float, or integer need to
 * perform testing on @b NAN cases and on positive and negative
 * infinity cases.
 *
 * @param jdoublevar     Double-precision floating point variable
 *                       as @link #jdouble jdouble@endlink to examine
 *
 * @param jlongvar       Long integer (size of double-precision floating
 *                       point) as @link #jlong jlong@endlink scratch
 *                       variable.
 *
 * @param resultvar      Result variable of any type
 *
 * @param resultNaN      Value for @c @b resultvar when @c @b jfloatvar
 *                       is a @b NAN case.
 *
 * @param resultPosZero  Value for @c @b resultvar when @c @b jfloatvar
 *                       is a positive zero.
 *
 * @param resultLargePos Largest positive integer that @c @b resultvar
 *                       can hold.
 *
 * @param resultNegZero  Value for @c @b resultvar when @c @b jfloatvar
 *                       is a negative zero.
 *
 * @param resultLargeNeg Largest negative integer that @c @b resultvar
 *                       can hold.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(jdoublevar)
 *                           Any @link #jdouble jdouble@endlink variable
 * </li>
 * </ul>
 *
 *
 * @returns into an @c @b else case to set @c @b resultvar when no
 *          special cases happened.  <b>This is the normal way that
 *          this macro should return except when the special cases
 *          actually occur, which should be rare</b>.
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(jlongvar)
 *                           Any @link #jlong jlong@endlink variable.
 *                           Temporary scratch storage.
 * </li>
 * <li> @c @e macro_expansion_of(resultvar)
 *                           Any integer or floating-point variable
 *                           of any type.  Will end up with either the
 *                           special-case test result as provided by
 *                           @c @b resultNaN, @c @b resultPosZero,
 *                           @c @b resultLargePos, @c @b resultNegZero,
 *                           or @c @b resultLargeNeg.
 * </li>
 * </ul>
 *
 *
 */
#define IF_JDOUBLE_SPECIAL_CASES(jdoublevar,                \
                                 jlongvar,                  \
                                 resultvar,                 \
                                 resultNaN,                 \
                                 resultPosZero,             \
                                 resultLargePos,            \
                                 resultNegZero,             \
                                 resultLargeNeg)            \
    jlongvar = FORCE_JLONG(jdoublevar);                     \
    if (JDOUBLE_IS_NAN(jlongvar))                           \
    {                                                       \
        resultvar = resultNaN;                              \
    }                                                       \
    else                                                    \
    if (JDOUBLE_POSITIVE_ZERO == (julong) jlongvar)         \
    {                                                       \
        resultvar = resultPosZero;                          \
    }                                                       \
    else                                                    \
    if ((JDOUBLE_POSITIVE_INFINITY == (julong) jlongvar) || \
        ((0.0 < jdoublevar) &&                              \
         (((jdouble) resultLargePos) <= jdoublevar)))       \
    {                                                       \
        resultvar = resultLargePos;                         \
    }                                                       \
    else                                                    \
    if (JDOUBLE_NEGATIVE_ZERO == (julong) jlongvar)         \
    {                                                       \
        resultvar = resultNegZero;                          \
    }                                                       \
    else                                                    \
    if ((JDOUBLE_NEGATIVE_INFINITY == (julong) jlongvar) || \
        ((0.0 > jdoublevar) &&                              \
         ((0.0 - (jdouble) resultLargeNeg) >= jdoublevar))) \
    {                                                       \
        resultvar = resultLargeNeg;                         \
    } /* Continue _directly_ with the final 'else' condition... */


/*!
 * @brief Check special cases of single-precision floating point numbers
 * as part of conversion to other formats.
 *
 *
 * The opcodes @b F2x where 'x' is long, double, or integer need to
 * perform testing on @b NAN cases and on positive and negative
 * infinity cases.
 *
 * @param jfloatvar      Single-precision floating point variable
 *                       as @link #jfloat jfloat@endlink to examine
 *
 * @param jintvar        Normal integer (size of single-precision
 *                       floating point) as @link #jlong jlong@endlink
 *                       scratch variable.
 *
 * @param resultvar      Result variable of any type
 *
 * @param resultNaN      Value for @c @b resultvar when @c @b jfloatvar
 *                       is a @b NAN case.
 *
 * @param resultPosZero  Value for @c @b resultvar when @c @b jfloatvar
 *                       is a positive zero.
 *
 * @param resultLargePos Largest positive integer that @c @b resultvar
 *                       can hold.
 *
 * @param resultNegZero  Value for @c @b resultvar when @c @b jfloatvar
 *                       is a negative zero.
 *
 * @param resultLargeNeg Largest negative integer that @c @b resultvar
 *                       can hold.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(jfloatvar)
 *                           Any @link #jfloat jfloat@endlink variable.
 * </li>
 * </ul>
 *
 *
 * @returns into an @c @b else case to set @c @b resultvar when no
 *          special cases happened.  <b>This is the normal way that
 *          this macro should return except when the special cases
 *          actually occur, which should be rare</b>.
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(jintvar)
 *                           Any @link #jint jint@endlink variable.
 *                           Temporary scratch storage.
 * </li>
 * <li> @c @e macro_expansion_of(resultvar)
 *                           Any integer or floating-point variable
 *                           of any type.  Will end up with either the
 *                           special-case test result as provided by
 *                           @c @b resultNaN, @c @b resultPosZero,
 *                           @c @b resultLargePos, @c @b resultNegZero,
 *                           or @c @b resultLargeNeg.
 * </li>
 * </ul>
 *
 */
#define IF_JFLOAT_SPECIAL_CASES(jfloatvar,                \
                                jintvar,                  \
                                resultvar,                \
                                resultNaN,                \
                                resultPosZero,            \
                                resultLargePos,           \
                                resultNegZero,            \
                                resultLargeNeg)           \
    jintvar = FORCE_JINT(jfloatvar);                      \
    if (JFLOAT_IS_NAN(jintvar))                           \
    {                                                     \
        resultvar = resultNaN;                            \
    }                                                     \
    else                                                  \
    if (JFLOAT_POSITIVE_ZERO == (juint) jintvar)          \
    {                                                     \
        resultvar = resultPosZero;                        \
    }                                                     \
    else                                                  \
    if ((JFLOAT_POSITIVE_INFINITY == (juint) jintvar) ||  \
        ((0.0 < jfloatvar) &&                             \
         (((jfloat) resultLargePos) <= jfloatvar)))       \
    {                                                     \
        resultvar = resultLargePos;                       \
    }                                                     \
    else                                                  \
    if (JFLOAT_NEGATIVE_ZERO == (juint) jintvar)          \
    {                                                     \
        resultvar = resultNegZero;                        \
    }                                                     \
    else                                                  \
    if ((JFLOAT_NEGATIVE_INFINITY == (juint) jintvar) ||  \
        ((0.0 > jfloatvar) &&                             \
         ((0.0 - (jfloat) resultLargeNeg) >= jfloatvar))) \
    {                                                     \
        resultvar = resultLargeNeg;                       \
    } /* Continue _directly_ with the final 'else' condition... */


/*@} */ /* End of grouped definitions */


/*!
 * @name Validate a constant_pool entry in opcode inner loop.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Check that a @c @b constant_pool entry contains
 * a specific of tag for this operation.
 *
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that
 *               contains a @c @b constant_pool entry to be examined.
 *
 * @param cptag1 First @c @b constant_pool tag that is valid for this
 *               operation.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
         if the constant_pool entry does not have the right tag@endlink.
 *
 */
#define CHECK_CP_TAG(u2var, cptag1)                             \
    if (cptag1 != CP_TAG(pcfs, u2var))                          \
    {                                                           \
        /* Somebody is confused */                              \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
    }


/*!
 * @brief Check that a @c @b constant_pool entry contains
 * the right kind of tag for this operation, from a choice of two.
 *
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that
 *               contains a @c @b constant_pool entry to be examined.
 *
 * @param cptag1 First @c @b constant_pool tag that is valid for this
 *               operation.
 *
 * @param cptag2 Second @c @b constant_pool tag that is valid for this
 *               operation.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
         if the constant_pool entry does not have the right tag@endlink.
 *
 */
#define CHECK_CP_TAG2(u2var, cptag1, cptag2)                    \
    if ((cptag1 != CP_TAG(pcfs, u2var)) &&                      \
        (cptag2 != CP_TAG(pcfs, u2var)))                        \
    {                                                           \
        /* Somebody is confused */                              \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
    }


/*!
 * @brief Check that a @c @b constant_pool entry contains
 * the right kind of tag for this operation, from a choice of three.
 *
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that
 *               contains a @c @b constant_pool entry to be examined.
 *
 * @param cptag1 First @c @b constant_pool tag that is valid for this
 *               operation.
 *
 * @param cptag2 Second @c @b constant_pool tag that is valid for this
 *               operation.
 *
 * @param cptag3 Third @c @b constant_pool tag that is valid for this
 *               operation.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
         if the constant_pool entry does not have the right tag@endlink.
 *
 */
#define CHECK_CP_TAG3(u2var, cptag1, cptag2, cptag3)            \
    if ((cptag1 != CP_TAG(pcfs, u2var)) &&                      \
        (cptag2 != CP_TAG(pcfs, u2var)) &&                      \
        (cptag3 != CP_TAG(pcfs, u2var)))                        \
    {                                                           \
        /* Somebody is confused */                              \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
    }

/*@} */ /* End of grouped definitions */


/*!
 * @name Class, method, field, and program counter support macros.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Calculate ClassFile pointer from a class reference.
 *
 * During the calculation, various scratch variables are
 * loaded and used to simplify the code.  Two final results
 * include a (CONSTANT_Class_info *) stored in the local variable
 * @b pcpma_Class stored the local variable @b pcfsmisc
 * and a (CONSTANT_Class_info *) stored in the local variable
 * @b pcpma_Class
 *
 * @param clsnameidx  @c @b constant_pool index into class file of
 *                    current class (as indicated in the program
 *                    counter) that is a class reference entry.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(clsnameidx)
 *                           Any expression resolving to a
 *                           @c @b constant_pool index
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e pcpma          pointer to a @c @b constant_pool entry
 * </li>
 * <li> @c @e pcpma_Class   @c @b pcpma as a CONSTANT_Class_info pointer
 * </li>
 * <li> @c @e clsidxmisc     Class index of fully bound class referenced
 *                           by @c @b pcpma_Class
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * </ul>
 *
 *
 */
#define CALCULATE_CLASS_INFO_FROM_CLASS_REFERENCE(clsnameidx)          \
    pcpma       = pcfs->constant_pool[clsnameidx];                     \
    pcpma_Class = PTR_THIS_CP_Class(pcpma);                            \
                                                                       \
    clsidxmisc = pcpma_Class->LOCAL_Class_binding.clsidxJVM;           \
                                                                       \
    /*                                                                 \
     * Try to resolve this class before attempting to load.            \
     * It could be that it has been loaded but is not yet              \
     * resolved enough.                                                \
     */                                                                \
    if (jvm_class_index_null == clsidxmisc)                            \
    {                                                                  \
        (rvoid) linkage_resolve_class(GET_PC_FIELD_IMMEDIATE(thridx,   \
                                                             clsidx),  \
                                      rfalse);                         \
                                                                       \
        clsidxmisc = pcpma_Class->LOCAL_Class_binding.clsidxJVM;       \
                                                                       \
        /* Now try to load it again if resolution failed to locate it*/\
        if (jvm_class_index_null == clsidxmisc)                        \
        {                                                              \
            /* Need local var to avoid possible expansion confusion */ \
            jvm_constant_pool_index cpidxOLD = clsnameidx;             \
                                                                       \
            /* If class is not loaded, retrieve it by UTF8 class name*/\
            LATE_CLASS_LOAD(cpidxOLD);                                 \
        }                                                              \
    }                                                                  \
    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs; /* Extra ; */


/*!
 * @brief Calculate field_info pointer from a field reference.
 *
 * During the calculation, various scratch variables are
 * loaded and used to simplify the code.  Two final results
 * include a (field_info *) stored the local variable @b pfld
 * and a (CONSTANT_Fieldref_info *) stored in the local variable
 * @b pcpma_Fieldref
 *
 * @param Fieldref  @c @b constant_pool index into class file of current
 *                  class (as indicated in the program counter) that
 *                  is a method reference entry.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(Fieldref)
 *                           Any expression resolving to a
 *                           @c @b constant_pool index
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e pcpma          pointer to a @c @b constant_pool entry
 * </li>
 * <li> @c @e pcpma_Fieldref @c @b pcpma as a CONSTANT_Fieldref_info
 *                           pointer
 * </li>
 * <li> @c @e clsidxmisc     Class index of fully bound class referenced
 *                           by @c @b pcpma_Fieldref
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * <li>  @c @e pfld          field_info table entry referenced by
 *                           @c @b pcpma_Fieldref
 * </li>
 * <li>  @c @e fluidxmisc    Field lookup index of field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
           if requested field is not found in the class@endlink.
 *
 */
#define CALCULATE_FIELD_INFO_FROM_FIELD_REFERENCE(Fieldref)            \
    pcpma           = pcfs->constant_pool[Fieldref];                   \
    pcpma_Fieldref = PTR_THIS_CP_Fieldref(pcpma);                      \
    clsidxmisc     = pcpma_Fieldref->LOCAL_Fieldref_binding.clsidxJVM; \
                                                                       \
    /*                                                                 \
     * Try to resolve this class before attempting to load.            \
     * It could be that it has been loaded but is not yet              \
     * resolved enough.                                                \
     */                                                                \
    if (jvm_class_index_null == clsidxmisc)                            \
    {                                                                  \
        (rvoid) linkage_resolve_class(GET_PC_FIELD_IMMEDIATE(thridx,   \
                                                             clsidx),  \
                                      rfalse);                         \
                                                                       \
        clsidxmisc = pcpma_Fieldref->LOCAL_Fieldref_binding.clsidxJVM; \
                                                                       \
        /* Now try to load it again if resolution failed to locate it*/\
        if (jvm_class_index_null == clsidxmisc)                        \
        {                                                              \
            /* If class is not loaded, retrieve it by UTF8 class name*/\
            LATE_CLASS_LOAD(pcpma_Fieldref->class_index);              \
                                                                       \
            /* Check if field exists in loaded class */                \
            clsidxmisc = pcpma_Fieldref                                \
                           ->LOCAL_Fieldref_binding.clsidxJVM;         \
            if (jvm_class_index_null == clsidxmisc)                    \
            {                                                          \
                thread_throw_exception(thridx,                         \
                                       THREAD_STATUS_THREW_ERROR,      \
                                JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR);  \
/*NOTREACHED*/                                                         \
            }                                                          \
        }                                                              \
    }                                                                  \
                                                                       \
    fluidxmisc     = pcpma_Fieldref->LOCAL_Fieldref_binding.fluidxJVM; \
    if (jvm_field_index_bad == fluidxmisc)                             \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                                JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR);  \
/*NOTREACHED*/                                                         \
    }                                                                  \
                                                                       \
    pcfsmisc       = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs;           \
    fluidxmisc     = pcpma_Fieldref->LOCAL_Fieldref_binding.fluidxJVM; \
                                                                       \
    if (rtrue == pcpma_Fieldref->LOCAL_Fieldref_binding.fluidxJVM)     \
    {                                                                  \
        pfld = pcfsmisc->fields[CLASS(clsidxmisc)                      \
                                  .object_instance_field_lookup        \
                                     [fluidxmisc]];                    \
    }                                                                  \
    else                                                               \
    {                                                                  \
        pfld = pcfsmisc->fields[CLASS(clsidxmisc)                      \
                                  .class_static_field_lookup           \
                                     [fluidxmisc]];                    \
    }


/*!
 * @brief Report whether a method is an \<init\> method or not.
 *
 *
 * Perform simple string comparison of a method name string to
 * the instance initialization method name string.
 *
 * @param pcfs    ClassFile pointer of method in class to review
 *
 * @param cpidx   @c @b constant_pool index of method name string
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(pcfs)
 *                           Any expression that resolves to a
 *                           ClassFile pointer
 * </li>
 * <li> @c @e macro_expansion_of(cpidx)
 *                           Any expression that resolves to a
 *                           @c @b constant_pool index
 * </li>
 * </ul>
 *
 *
 * @returns @link #rtrue rtrue@endlink if the method is the
 *          \<init\> method.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 */
#define IS_INIT_METHOD(pcfs, cpidx)                                   \
    ((0 == utf_prchar_pcfs_strcmp(CONSTANT_UTF8_INSTANCE_CONSTRUCTOR, \
                                  pcfs,                               \
                                  cpidx))                             \
     ? rtrue                                                          \
     : rfalse)


/*!
 * @brief Attempt to load a class that is not currently loaded.
 *
 *
 * @param clsnameidx  CONSTANT_Utf8_info @c @b constant_pool index
 *                    to class name
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(clsnameidx)
 *                           Any expression resolving to a
 *                           @c @b constant_pool index
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e pcpma          pointer to a @c @b constant_pool entry,
 *                           ultimately pointing to the entry containing
 *                           the name of the input @c @b clsnameidx
 * </li>
 * <li> @c @e pcpma_Class   @c @b pcpma as a CONSTANT_Class_info pointer
 * </li>
 * <li> @c @e pcpma_Utf8     @c @b pcpma as a CONSTANT_Utf8_info pointer
 * </li>
 * <li> @c @e prchar_clsname Null-terminated string version of class
 *                           name @c @b clsnameidx
 * </li>
 * <li> @c @e clsidxmisc     Class index of class named in
 *                           @c @b clsnamidx after it has been loaded.
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR
 *         @link #JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR
           if requested class cannot be located@endlink.
 *
 */
#define LATE_CLASS_LOAD(clsnameidx)                                  \
                                                                     \
    pcpma       = pcfs->constant_pool[clsnameidx]; /* Class name */  \
    pcpma_Class = PTR_THIS_CP_Class(pcpma);                          \
                                                  /* UTF8 string */  \
    pcpma       = pcfs->constant_pool[pcpma_Class->name_index];      \
    pcpma_Utf8  = PTR_THIS_CP_Utf8(pcpma);                           \
                                                                     \
    prchar_clsname = utf_utf2prchar(pcpma_Utf8);                     \
                                                                     \
    /* Try again to load class */                                    \
    clsidxmisc = class_load_resolve_clinit(prchar_clsname,           \
                                           thridx,                   \
                                           rfalse,                   \
                                           rfalse);                  \
                                                                     \
    HEAP_FREE_DATA(prchar_clsname);                                  \
                                                                     \
    /* If class is irretrievable, abort */                           \
    if (jvm_class_index_null == clsidxmisc)                          \
    {                                                                \
        thread_throw_exception(thridx,                               \
                               THREAD_STATUS_THREW_ERROR,            \
                           JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR); \
/*NOTREACHED*/                                                       \
    }                                                                \
                                                                     \
    /* Go resolve the current class again to pick up changes */      \
    (rvoid) linkage_resolve_class(GET_PC_FIELD_IMMEDIATE(thridx,     \
                                                         clsidx),    \
                                  rfalse); /* Extra ; */


/*!
 * @brief Load crucial local variables that describe a virtual method
 *
 * Load the @c @b pcfs and @c @b pcode variables to point to the
 * current class file and the current methods, respectively.
 * This must be done upon entrance to the inner loop and upon
 * execution of @c @b xxRETURN opcodes. 
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li>  @c @e pcfs points to class file for current class.
 * </li>
 * <li>  @c @e pcode points to first opcode in this method.
 * </li>
 * </ul>
 *
 */
#define LOAD_METHOD_CONTEXT                                 \
    pcfs = THIS_PCFS(thridx);                               \
    pcode = DEREFERENCE_PC_CODE_BASE(thridx); /* Extra ; */


/*!
 * @brief Adjust program counter by a 2-byte relative offset value.
 *
 *
 * @param offset2var  Variable of type @link #u2 u2@endlink holding
 *                    destination offset to load into program counter.
 *
 * @param instrlen   Adjust offset by size of this virtual instruction
 *                   as specified in this expression.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(offsetvar)
 *                           Any @link #u2 u2@endlink variable.
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 */
#define LOAD_TARGET_PC_OFFSET(offset2var, instrlen)             \
    /* This will create a signed value */                       \
    pc->offset += (jvm_pc_offset)                               \
                  ((jvm_pc_offset_actual_size) offset2var)      \
                   - instrlen;                                  \
                                                                \
    /*!                                                         \
     * @todo HARMONY-6-jvm-opmacros.h-7 Need to check           \
     *       whether max PC value itself is legal, thus         \
     *       whether comparison should be @b &lt; or @b &lt;=   \
     *                                                          \
     */                                                         \
                                                                \
    /*                                                          \
     * Don't need a lower bound test since offset is unsigned   \
     */                                                         \
    if (jvm_pc_offset_bad < pc->offset)                         \
    {                                                           \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
    }


/*!
 * @brief Calculate number of pad bytes added after table opcodes
 *
 * Skip several bytes in the program counter based on the LS 2 bits
 * of the address of the @c @b OPCODE_AA_TABLESWITCH opcode, per
 * comments in the JVM spec.  This is also relevant to the
 * @c @b OPCODE_AB_LOOKUPSWITCH opcode.
 *
 *
 * @internal  Concerning the number of pad bytes, per JVM spec:
 *            "Immediately after the @e tableswitch opcode,
 *            between 0 and 3 null bytes (zeroed bytes, not
 *            the null object) are inserted as padding.  The
 *            number of null bytes is chosen so that the following
 *            byte begins at an address that is a multiple of
 *            4 bytes from the start of the current method (the
 *            opcode of its first instruction)."
 *
 *            Notice that in this design, each method @e always
 *            starts at offset zero, namely at offset
 *            @link #CODE_CONSTRAINT_START_PC
              CODE_CONSTRAINT_START_PC@endlink.  Therefore, the
 *            pad calculation can simply be done modulo 4, that is,
 *            mod(sizeof(u4)).  Notice that this also meets the
 *            constraint described in the note at the end of the
 *            opcode description:
 *
 *            "The alignment required of the 4-byte operands of the
 *            @e tableswitch instruction guarantees 4-byte alignment
 *            of those operands if an only if the method that contains
 *            the @e tableswitch starts onf a 4-byte boundary."
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li>  @c @e jptmp stores offset of opcode by adjusting from
 *                   @c @b pc->offset back to opcode address.
 * </li>
 * </ul>
 *
 */
#define SWITCH_PAD_PC                              \
    jptmp       = pc->offset - sizeof(u1);         \
    pc->offset += (sizeof(u4) - sizeof(opcode));   \
    pc->offset &= ~(sizeof(u4) - 1); /* Extra ; */


/*!
 * @brief Synchronize to this method's class' object monitor before
 * method invocation.
 *
 * This macro is designed to be used @e only within the context of
 * the @b INVOKExxx opcodes.  This is due to the conditional
 * adjustment of the program counter.  @e All other uses of
 * objectutil_synchronize() should be performed in other manners.
 *
 * If this method is a synchronized method, attempt to gain MLOCK.
 * If successful, carry on with opcode.  If not, unwind PC to
 * beginning of instruction and relinquish.  The thread model will
 * re-enter the opcode when the lock has finally been acquired.
 *
 *
 * @internal This macro conditionally breaks out of @c @b switch(opcode)
 *           statement, leaving PC pointing to this opcode.  Since the
 *           thread state will no longer be @b RUNNING , no more
 *           opcodes will be run on this thread until this thread
 *           successfully arbitrates for the lock again.  At that time,
 *           it will try this opcode again.
 *
 *
 * @todo HARMONY-6-jvm-opmacros.h-15 Verify that the above
 *       call to objectutil_synchronize() does not cause a
 *       problem trying to get restarted, such as too many
 *       attempts to lock the object or on the other end
 *       with not enough attempts to unlock it. (?)
 *
 *
 * @warning Be @e absolutely sure that the block level that this
 *          macro is invoked from will @c @b break from the outer
 *          giant @c @b switch(opcode){} statement instead of from some
 *          inner @c @b switch() or @c @b for() or @c @b while() or
 *          other block structure that can act on @c @b break
 *          statements.  If this injunction is not followed, then
 *          method synchronization will @e not happen properly.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li>  @c @e none
 * </li>
 * </ul>
 *
 */
#define SYNCHRONIZE_METHOD_INVOCATION                                  \
    if (ACC_SYNCHRONIZED & pmth->access_flags)                         \
    {                                                                  \
        if (rfalse == objectutil_synchronize(                          \
                          CLASS(clsidxmisc).class_objhash,             \
                          thridx))                                     \
        {                                                              \
                            /* size of opcode +   size of operand */   \
            pc->offset -= (    sizeof(u1)     +   sizeof(u2)        ); \
                                                                       \
            break;                                                     \
        }                                                              \
    }


/*@} */ /* End of grouped definitions */


/*!
 * @name Runtime validation macros.
 *
 * Some of these macros should have their functionality ultimately
 * moved to a byte code verifier that evaluates the correctness
 * of the compiled rendition of the source code, while others
 * operate on conditions known only at run time.
 *
 */


/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Check if this method is an @c @b abstract method,
 * that is, not having a concrete implementation.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 if requested method is a method with a concrete implementatino@endlink.
 *
 */
#define CHECK_ABSTRACT_METHOD                                          \
                                                                       \
    /* Must not be a concrete method */                                \
    if (!(ACC_ABSTRACT & pmth->access_flags))                          \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
\
/* What exception gets thrown here? Need "not" of InstantiationError */\
\
                               JVMCLASS_JAVA_LANG_INSTANTIATIONERROR); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if this field is a final field in the current class.
 *
 *
 * Determine if a final field is in the current class.  If so, fine,
 * but otherwise it is in a superclass.  This is an error.
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e pcpma_Fieldref CONSTANT_Fieldref_info pointer to current
 *                           field
 * </li>
 * <li>  @c @e pfld          field_info table entry of current field
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR
 *         @link #JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR
           if requested field is final, but in a superclass@endlink.
 *
 */
#define CHECK_FINAL_FIELD_CURRENT_CLASS                          \
                                                                 \
    {                                                            \
        jvm_class_index clsidxTMP;                               \
                                                                 \
        GET_PC_FIELD(thridx, clsidxTMP, clsidx);                 \
                                                                 \
        /* A final field must _not_ be found in a superclass */  \
        if ((ACC_FINAL & pfld->access_flags) &&                  \
            (clsidxTMP != pcpma_Fieldref                         \
                            ->LOCAL_Fieldref_binding.clsidxJVM)) \
        {                                                        \
            thread_throw_exception(thridx,                       \
                                   THREAD_STATUS_THREW_ERROR,    \
                       JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR);   \
/*NOTREACHED*/                                                   \
        }                                                        \
    }


/*!
 * @brief Check if this field is an object instance field.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pfld          field_info table entry of current field
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
 *         @link #JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
           if requested method is a static field@endlink.
 *
 */
#define CHECK_INSTANCE_FIELD                                     \
                                                                 \
    /* Must be an instance field */                              \
    if (ACC_STATIC & pfld->access_flags)                         \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
               JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR); \
/*NOTREACHED*/                                                   \
    }


/*!
 * @brief Check if this method is an object instance method.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
           if requested method is a static method@endlink.
 *
 */
#define CHECK_INSTANCE_METHOD                                    \
                                                                 \
    /* Must be an instance method */                             \
    if (ACC_STATIC & pmth->access_flags)                         \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
                               JVMCLASS_JAVA_LANG_VERIFYERROR);  \
/*NOTREACHED*/                                                   \
    }


/*!
 * @brief Check if this object is from a concrete class, that is,
 * not from an @c @b abstract class.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e objhashmisc   Object table hash of current object
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
           if requested object is an abstract object@endlink.
 *
 */
#define CHECK_NOT_ABSTRACT_CLASS                                       \
                                                                       \
    /* Must not be from an abstract class */                           \
    if (ACC_ABSTRACT &                                                 \
        OBJECT_CLASS_LINKAGE(objhashmisc)->pcfs->access_flags)         \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INSTANTIATIONERROR); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if this method is a concrete method, that is,
 * not @c @b abstract .
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
           if requested method is an abstract method@endlink.
 *
 */
#define CHECK_NOT_ABSTRACT_METHOD                                      \
                                                                       \
    /* Must not be an abstract method */                               \
    if (ACC_ABSTRACT & pmth->access_flags)                             \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INSTANTIATIONERROR); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if this object is a scalar, that is, not an array.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e objhashmisc   Object table hash of current object
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
           if requested method is an array object@endlink.
 *
 */
#define CHECK_NOT_ARRAY_OBJECT                                         \
                                                                       \
    /* Must not be an array object */                                  \
    if (OBJECT_STATUS_ARRAY &                                          \
        CLASS(OBJECT_CLASS_LINKAGE(objhashmisc)->clsidx).status)       \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INSTANTIATIONERROR); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if this method is @e not a \<clinit\> method
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
           if requested method is an array object@endlink.
 *
 */
#define CHECK_NOT_CLINIT_METHOD                                  \
                                                                 \
    /* Must not be a class or instance constructor */            \
    if (0 == utf_prchar_pcfs_strcmp(                             \
                          LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR, \
                                    pcfsmisc,                    \
                                    pmth->name_index))           \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
                               JVMCLASS_JAVA_LANG_VERIFYERROR);  \
/*NOTREACHED*/                                                   \
    }


/*!
 * @brief Check if this method is @e not an \<init\> method
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
           if requested method is an array object@endlink.
 *
 */
#define CHECK_NOT_INIT_METHOD                                    \
                                                                 \
    /* Must not be an instance constructor */                    \
    if (0 == utf_prchar_pcfs_strcmp(                             \
                             CONSTANT_UTF8_INSTANCE_CONSTRUCTOR, \
                                    pcfsmisc,                    \
                                    pmth->name_index))           \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
                               JVMCLASS_JAVA_LANG_VERIFYERROR);  \
/*NOTREACHED*/                                                   \
    }                                                            \


/*!
 * @brief Check if this object is from a normal class, that is,
 * not from an interface class.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e objhashmisc   Object table hash of current object
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
 *         @link #JVMCLASS_JAVA_LANG_INSTANTIATIONERROR
           if requested object is from an interface class@endlink.
 *
 */
#define CHECK_NOT_INTERFACE_CLASS                                      \
                                                                       \
    /* Must not be from an interface class */                          \
    if (ACC_INTERFACE &                                                \
        OBJECT_CLASS_LINKAGE(objhashmisc)->pcfs->access_flags)         \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INSTANTIATIONERROR); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if this object hash is not a null object hash.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e objhashmisc   Object table hash of current object
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION
           if object hash is to a null object@endlink.
 *
 */
#define CHECK_NOT_NULL_OBJECT_HASH(objhash)                            \
                                                                       \
    /* Must not be a null object hash */                               \
    if (jvm_object_hash_null == objhash)                               \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_EXCEPTION,          \
                             JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if the class hierarchy is followed, per opcode.
 *
 *
 * This macro is used where @b INVOKESPECIAL is being processed,
 * once for normal virtual methods, once for native methods.
 * It implements a passage of the JVM spec for this opcode.
 *
 * @param _clsidx      Class index containing class where
 *                     resolved method is defined.
 *
 * @param _objhash     Object to examine
 *
 * @param _isinitmethod @link #rtrue rtrue@endlink if invoked method is
 *                     the \<init\> method.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(_clsidx)
 *                           Any expression that resolves to a
 *                           class index
 * </li>
 * <li> @c @e macro_expansion_of(_objhash)
 *                           Any @link #jvm_object_hash
                             jvm_object_hash@endlink variable.
 * </li>
 * <li> @c @e macro_expansion_of(_isinitmethod)
 *                           Any expression that resolves to a
 *                           @link #rboolean rboolean@endlink
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 */
#define CHECK_OBJECT_CLASS_STRUCTURE(_clsidx, _objhash, _isinitmethod) \
    switch(opcode)                                                     \
    {                                                                  \
        case OPCODE_B9_INVOKEINTERFACE:                                \
            /* This logic @e should follow next bit, per spec */       \
            if (rfalse ==                                              \
                classutil_class_implements_interface(                  \
                    OBJECT_CLASS_LINKAGE(_objhash)->clsidx,            \
                    _clsidx))                                          \
            {                                                          \
                thread_throw_exception(thridx,                         \
                                       THREAD_STATUS_THREW_ERROR,      \
                      JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR);\
/*NOTREACHED*/                                                         \
            }                                                          \
        /*! @warning NO @c @b break statement here! Continue... */     \
                                                                       \
        case OPCODE_B6_INVOKEVIRTUAL:                                  \
            if (rfalse ==                                              \
                classutil_class_is_a(                                  \
                    GET_PC_FIELD_IMMEDIATE(thridx, clsidx),            \
                    OBJECT_CLASS_LINKAGE(_objhash)->clsidx))           \
            {                                                          \
                thread_throw_exception(thridx,                         \
                                       THREAD_STATUS_THREW_ERROR,      \
                               JVMCLASS_JAVA_LANG_ABSTRACTMETHODERROR);\
/*NOTREACHED*/                                                         \
            }                                                          \
            break;                                                     \
                                                                       \
        case OPCODE_B7_INVOKESPECIAL:                                  \
            CHECK_SUPERCLASS_VALID_METHOD_FOUND(_clsidx,               \
                                                _isinitmethod);        \
            break;                                                     \
                                                                       \
     /* case OPCODE_B8_INVOKESTATIC:                                   \
            break; */                                                  \
    }


/*!
 * @brief Check if this method is a public method.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pmth          method_info table entry of current method
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR
 *         @link #JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR
           if requested method is not a public method@endlink.
 *
 */
#define CHECK_PUBLIC_METHOD                                            \
                                                                       \
    /* Must be a public method */                                      \
    if (!(ACC_PUBLIC & pmth->access_flags))                            \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR); \
/*NOTREACHED*/                                                         \
    }


/*!
 * @brief Check if this field is a static field.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li>  @c @e pfld          field_info table entry of current field
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
 *         @link #JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
           if requested field is an object instance field@endlink.
 *
 */
#define CHECK_STATIC_FIELD                                       \
                                                                 \
    /* Must be a static field */                                 \
    if (!(ACC_STATIC & pfld->access_flags))                      \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
               JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR); \
/*NOTREACHED*/                                                   \
    }


/*!
 * @brief Check if this method is a static method.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(u2var)
 *                           Receives operand contents
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e macro_expansion_of(u2var)
 *                           Receives operand contents
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
 *         @link #JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
           if requested method is an object instance method@endlink.
 *         Presumably the compiler did its job properly, so there is
 *         no need to invoke a @b VerifyError instead.
 *
 */
#define CHECK_STATIC_METHOD                                      \
                                                                 \
    /* Must be a static method */                                \
    if (!(ACC_STATIC & pmth->access_flags))                      \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
              JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR);  \
/*NOTREACHED*/                                                   \
    }


/*!
 * @brief Check if the superclass of the current class contains a
 * matching method.
 *
 *
 * This macro is used where @b INVOKESPECIAL is being processed,
 * once for normal virtual methods, once for native methods.
 * It implements a passage of the JVM spec for this opcode.
 *
 * @param _clsidx       Class index containing class where
 *                      resolved method is defined.
 *
 * @param _isinitmethod @link #rtrue rtrue@endlink if invoked method is
 *                      the \<init\> method.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(_clsidx)
 *                           Any expression that resolves to a
 *                           class index
 * </li>
 * <li> @c @e macro_expansion_of(_isinitmethod)
 *                           Any expression that resolves to a
 *                           @link #rboolean rboolean@endlink
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 */
#define CHECK_SUPERCLASS_VALID_METHOD_FOUND(_clsidx, _isinitmethod) \
    if ((ACC_SUPER &                                                \
        CLASS(GET_PC_FIELD_IMMEDIATE(thridx,                        \
                                     clsidx)).status)   &&          \
                                                                    \
        /* "is a" tests for subclasses _and_ parent class */        \
        ((GET_PC_FIELD_IMMEDIATE(thridx, clsidx) !=                 \
          _clsidx) &&                                               \
         (rtrue ==                                                  \
          classutil_class_is_a(                                     \
              GET_PC_FIELD_IMMEDIATE(thridx, clsidx),               \
              _clsidx)))                                &&          \
                                                                    \
        (rfalse == (_isinitmethod)))                                \
    {                                                               \
        /*                                                          \
         * Proceed unless superclass of current class               \
         * does not contain the resolved method.                    \
         */                                                         \
        if (rfalse ==                                               \
            classutil_class_is_a(                                   \
                CLASS_OBJECT_LINKAGE(                               \
                    GET_PC_FIELD_IMMEDIATE(thridx, clsidx))         \
                      ->pcfs                                        \
                        ->super_class,                              \
                _clsidx))                                           \
        {                                                           \
            exit_throw_exception(EXIT_JVM_OBJECT,                   \
                   JVMCLASS_JAVA_LANG_ABSTRACTMETHODERROR);         \
/*NOTREACHED*/                                                      \
        }                                                           \
    }


/*!
 * @brief Check for field lookup index in local field binding.
 *
 *
 * @param fluidx  Field lookup index from a local field binding
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(fluidx)
 *                           Any expression resolving to a
 *                           field lookup index
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
      if requested class static field is not found in the class@endlink.
 *
 */
#define CHECK_VALID_FIELDLOOKUPIDX(fluidx)                           \
    if (jvm_field_lookup_index_bad == fluidx)                        \
    {                                                                \
        thread_throw_exception(thridx,                               \
                               THREAD_STATUS_THREW_ERROR,            \
                               JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR); \
/*NOTREACHED*/                                                       \
    }


/*@} */ /* End of grouped definitions */

#ifdef I_AM_OPCODE_C

/*!
 * @brief Java Virtual Machine operation code name strings
 *
 * This array is used when displaying the program counter and opcode
 * at that address during debug.
 *
 */ 

static char *opcode_names[OPCODE_COUNT] =
{
    "NOP",          "ACONST_NULL",      "ICONST_M1",    "ICONST_0",
    "ICONST_1",     "ICONST_2",         "ICONST_3",     "ICONST_4",
    "ICONST_5",     "LCONST_0",         "LCONST_1",     "FCONST_0",
    "FCONST_1",     "FCONST_2",         "DCONST_0",     "DCONST_1",
    "BIPUSH",       "SIPUSH",           "LDC",          "LDC_W",
    "LDC2_W",       "ILOAD",            "LLOAD",        "FLOAD",
    "DLOAD",        "ALOAD",            "ILOAD_0",      "ILOAD_1",
    "ILOAD_2",      "ILOAD_3",          "LLOAD_0",      "LLOAD_1",
    "LLOAD_2",      "LLOAD_3",          "FLOAD_0",      "FLOAD_1",
    "FLOAD_2",      "FLOAD_3",          "DLOAD_0",      "DLOAD_1",
    "DLOAD_2",      "DLOAD_3",          "ALOAD_0",      "ALOAD_1",
    "ALOAD_2",      "ALOAD_3",          "IALOAD",       "LALOAD",
    "FALOAD",       "DALOAD",           "AALOAD",       "BALOAD",
    "CALOAD",       "SALOAD",           "ISTORE",       "LSTORE",
    "FSTORE",       "DSTORE",           "ASTORE",       "ISTORE_0",
    "ISTORE_1",     "ISTORE_2",         "ISTORE_3",     "LSTORE_0",
    "LSTORE_1",     "LSTORE_2",         "LSTORE_3",     "FSTORE_0",
    "FSTORE_1",     "FSTORE_2",         "FSTORE_3",     "DSTORE_0",
    "DSTORE_1",     "DSTORE_2",         "DSTORE_3",     "ASTORE_0",
    "ASTORE_1",     "ASTORE_2",         "ASTORE_3",     "IASTORE",
    "LASTORE",      "FASTORE",         "DASTORE",       "AASTORE",
    "BASTORE",      "CASTORE",         "SASTORE",       "POP",
    "POP2",         "DUP",             "DUP_X1",        "DUP_X2",
    "DUP2",         "DUP2_X1",         "DUP2_X2",       "SWAP",
    "IADD",         "LADD",            "FADD",          "DADD",
    "ISUB",         "LSUB",            "FSUB",          "DSUB",
    "IMUL",         "LMUL",            "FMUL",          "DMUL",
    "IDIV",         "LDIV",            "FDIV",          "DDIV",
    "IREM",         "LREM",            "FREM",          "DREM",
    "INEG",         "LNEG",            "FNEG",          "DNEG",
    "ISHL",         "LSHL",            "ISHR",          "LSHR",
    "IUSHR",        "LUSHR",           "IAND",          "LAND",
    "IOR",          "LOR",             "IXOR",          "LXOR",
    "IINC",         "I2L",             "I2F",           "I2D",
    "L2I",          "L2F",             "L2D",           "F2I",
    "F2L",          "F2D",             "D2I",           "D2L",
    "D2F",          "I2B",             "I2C",           "I2S",
    "LCMP",         "FCMPL",           "FCMPG",         "DCMPL",
    "DCMPG",        "IFEQ",            "IFNE",          "IFLT",
    "IFGE",         "IFGT",            "IFLE",          "IF_ICMPEQ",
    "IF_ICMPNE",    "IF_ICMPLT",       "IF_ICMPGE",     "IF_ICMPGT",
    "IF_ICMPLE",    "IF_ACMPEQ",       "IF_ACMPNE",     "GOTO",
    "JSR",          "RET",             "TABLESWITCH",   "LOOKUPSWITCH",
    "IRETURN",      "LRETURN",         "FRETURN",       "DRETURN",
    "ARETURN",      "RETURN",          "GETSTATIC",     "PUTSTATIC",
    "GETFIELD",     "PUTFIELD",        "INVOKEVIRTUAL", "INVOKESPECIAL",
    "INVOKESTATIC", "INVOKEINTERFACE", "XXXUNUSEDXXX1", "NEW",
    "NEWARRAY",     "ANEWARRAY",       "ARRAYLENGTH",   "ATHROW",
    "CHECKCAST",    "INSTANCEOF",      "MONITORENTER",  "MONITOREXIT",
    "WIDE",         "MULTIANEWARRAY",  "IFNULL",        "IFNONNULL",
    "GOTO_W",       "JSR_W",           "BREAKPOINT",    "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "UNUSED",        "UNUSED",
    "UNUSED",       "UNUSED",          "IMPDEP1",       "IMPDEP2"
};

/*!
 * @brief Return number of operand bytes used by this opcode.
 *
 * @param pcode     Pointer to code array being disassembled
 * @param pc        Program counter of code to disassemble
 * @param iswide    This opcode is now in the context of a WIDE opcode.
 *
 * @returns Number of operand bytes needed by this opcode.  In the
 *          case of the @b LOOKUPSWITCH and @b TABLESWITCH opcodes,
 *          only the first few operands are shown.
 */
static int operand_bytecount(jvm_virtual_opcode *pcode,
                             jvm_pc             *pc,
                             rboolean            iswide)
{
    jvm_virtual_opcode opcode;

    switch(pcode[pc->offset]) /* opcode */
    {
        case OPCODE_00_NOP:
        case OPCODE_01_ACONST_NULL:
        case OPCODE_02_ICONST_M1:
        case OPCODE_03_ICONST_0:
        case OPCODE_04_ICONST_1:
        case OPCODE_05_ICONST_2:
        case OPCODE_06_ICONST_3:
        case OPCODE_07_ICONST_4:
        case OPCODE_08_ICONST_5:
        case OPCODE_09_LCONST_0:
        case OPCODE_0A_LCONST_1:
        case OPCODE_0B_FCONST_0:
        case OPCODE_0C_FCONST_1:
        case OPCODE_0D_FCONST_2:
        case OPCODE_0E_DCONST_0:
        case OPCODE_0F_DCONST_1:
        case OPCODE_1A_ILOAD_0:
        case OPCODE_1B_ILOAD_1:
        case OPCODE_1C_ILOAD_2:
        case OPCODE_1D_ILOAD_3:
        case OPCODE_1E_LLOAD_0:
        case OPCODE_1F_LLOAD_1:
        case OPCODE_20_LLOAD_2:
        case OPCODE_21_LLOAD_3:
        case OPCODE_22_FLOAD_0:
        case OPCODE_23_FLOAD_1:
        case OPCODE_24_FLOAD_2:
        case OPCODE_25_FLOAD_3:
        case OPCODE_26_DLOAD_0:
        case OPCODE_27_DLOAD_1:
        case OPCODE_28_DLOAD_2:
        case OPCODE_29_DLOAD_3:
        case OPCODE_2A_ALOAD_0:
        case OPCODE_2B_ALOAD_1:
        case OPCODE_2C_ALOAD_2:
        case OPCODE_2D_ALOAD_3:
        case OPCODE_2E_IALOAD:
        case OPCODE_2F_LALOAD:
        case OPCODE_30_FALOAD:
        case OPCODE_31_DALOAD:
        case OPCODE_32_AALOAD:
        case OPCODE_33_BALOAD:
        case OPCODE_34_CALOAD:
        case OPCODE_35_SALOAD:
        case OPCODE_3B_ISTORE_0:
        case OPCODE_3C_ISTORE_1:
        case OPCODE_3D_ISTORE_2:
        case OPCODE_3E_ISTORE_3:
        case OPCODE_3F_LSTORE_0:
        case OPCODE_40_LSTORE_1:
        case OPCODE_41_LSTORE_2:
        case OPCODE_42_LSTORE_3:
        case OPCODE_43_FSTORE_0:
        case OPCODE_44_FSTORE_1:
        case OPCODE_45_FSTORE_2:
        case OPCODE_46_FSTORE_3:
        case OPCODE_47_DSTORE_0:
        case OPCODE_48_DSTORE_1:
        case OPCODE_49_DSTORE_2:
        case OPCODE_4A_DSTORE_3:
        case OPCODE_4B_ASTORE_0:
        case OPCODE_4C_ASTORE_1:
        case OPCODE_4D_ASTORE_2:
        case OPCODE_4E_ASTORE_3:
        case OPCODE_4F_IASTORE:
        case OPCODE_50_LASTORE:
        case OPCODE_51_FASTORE:
        case OPCODE_52_DASTORE:
        case OPCODE_53_AASTORE:
        case OPCODE_54_BASTORE:
        case OPCODE_55_CASTORE:
        case OPCODE_56_SASTORE:
        case OPCODE_57_POP:
        case OPCODE_58_POP2:
        case OPCODE_59_DUP:
        case OPCODE_5A_DUP_X1:
        case OPCODE_5B_DUP_X2:
        case OPCODE_5C_DUP2:
        case OPCODE_5D_DUP2_X1:
        case OPCODE_5E_DUP2_X2:
        case OPCODE_5F_SWAP:
        case OPCODE_60_IADD:
        case OPCODE_61_LADD:
        case OPCODE_62_FADD:
        case OPCODE_63_DADD:
        case OPCODE_64_ISUB:
        case OPCODE_65_LSUB:
        case OPCODE_66_FSUB:
        case OPCODE_67_DSUB:
        case OPCODE_68_IMUL:
        case OPCODE_69_LMUL:
        case OPCODE_6A_FMUL:
        case OPCODE_6B_DMUL:
        case OPCODE_6C_IDIV:
        case OPCODE_6D_LDIV:
        case OPCODE_6E_FDIV:
        case OPCODE_6F_DDIV:
        case OPCODE_70_IREM:
        case OPCODE_71_LREM:
        case OPCODE_72_FREM:
        case OPCODE_73_DREM:
        case OPCODE_74_INEG:
        case OPCODE_75_LNEG:
        case OPCODE_76_FNEG:
        case OPCODE_77_DNEG:
        case OPCODE_78_ISHL:
        case OPCODE_79_LSHL:
        case OPCODE_7A_ISHR:
        case OPCODE_7B_LSHR:
        case OPCODE_7C_IUSHR:
        case OPCODE_7D_LUSHR:
        case OPCODE_7E_IAND:
        case OPCODE_7F_LAND:
        case OPCODE_80_IOR:
        case OPCODE_81_LOR:
        case OPCODE_82_IXOR:
        case OPCODE_83_LXOR:
        case OPCODE_85_I2L:
        case OPCODE_86_I2F:
        case OPCODE_87_I2D:
        case OPCODE_88_L2I:
        case OPCODE_89_L2F:
        case OPCODE_8A_L2D:
        case OPCODE_8B_F2I:
        case OPCODE_8C_F2L:
        case OPCODE_8D_F2D:
        case OPCODE_8E_D2I:
        case OPCODE_8F_D2L:
        case OPCODE_90_D2F:
        case OPCODE_91_I2B:
        case OPCODE_92_I2C:
        case OPCODE_93_I2S:
        case OPCODE_94_LCMP:
        case OPCODE_95_FCMPL:
        case OPCODE_96_FCMPG:
        case OPCODE_97_DCMPL:
        case OPCODE_98_DCMPG:
        case OPCODE_AC_IRETURN:
        case OPCODE_AD_LRETURN:
        case OPCODE_AE_FRETURN:
        case OPCODE_AF_DRETURN:
        case OPCODE_B0_ARETURN:
        case OPCODE_B1_RETURN:
        case OPCODE_BA_XXXUNUSEDXXX1:
        case OPCODE_BE_ARRAYLENGTH:
        case OPCODE_BF_ATHROW:
        case OPCODE_CA_BREAKPOINT:
        case OPCODE_CB_UNUSED:
        case OPCODE_CC_UNUSED:
        case OPCODE_CD_UNUSED:
        case OPCODE_CE_UNUSED:
        case OPCODE_CF_UNUSED:
        case OPCODE_D0_UNUSED:
        case OPCODE_D1_UNUSED:
        case OPCODE_D2_UNUSED:
        case OPCODE_D3_UNUSED:
        case OPCODE_D4_UNUSED:
        case OPCODE_D5_UNUSED:
        case OPCODE_D6_UNUSED:
        case OPCODE_D7_UNUSED:
        case OPCODE_D8_UNUSED:
        case OPCODE_D9_UNUSED:
        case OPCODE_DA_UNUSED:
        case OPCODE_DB_UNUSED:
        case OPCODE_DC_UNUSED:
        case OPCODE_DD_UNUSED:
        case OPCODE_DE_UNUSED:
        case OPCODE_DF_UNUSED:
        case OPCODE_E0_UNUSED:
        case OPCODE_E1_UNUSED:
        case OPCODE_E2_UNUSED:
        case OPCODE_E3_UNUSED:
        case OPCODE_E4_UNUSED:
        case OPCODE_E5_UNUSED:
        case OPCODE_E6_UNUSED:
        case OPCODE_E7_UNUSED:
        case OPCODE_E8_UNUSED:
        case OPCODE_E9_UNUSED:
        case OPCODE_EA_UNUSED:
        case OPCODE_EB_UNUSED:
        case OPCODE_EC_UNUSED:
        case OPCODE_ED_UNUSED:
        case OPCODE_EE_UNUSED:
        case OPCODE_EF_UNUSED:
        case OPCODE_F0_UNUSED:
        case OPCODE_F1_UNUSED:
        case OPCODE_F2_UNUSED:
        case OPCODE_F3_UNUSED:
        case OPCODE_F4_UNUSED:
        case OPCODE_F5_UNUSED:
        case OPCODE_F6_UNUSED:
        case OPCODE_F7_UNUSED:
        case OPCODE_F8_UNUSED:
        case OPCODE_F9_UNUSED:
        case OPCODE_FA_UNUSED:
        case OPCODE_FB_UNUSED:
        case OPCODE_FC_UNUSED:
        case OPCODE_FD_UNUSED:
        case OPCODE_FE_IMPDEP1:
        case OPCODE_FF_IMPDEP2:
            return(0);

        case OPCODE_10_BIPUSH:
        case OPCODE_12_LDC:
        case OPCODE_BC_NEWARRAY:
            return(1);

        case OPCODE_11_SIPUSH:
        case OPCODE_13_LDC_W:
        case OPCODE_14_LDC2_W:
        case OPCODE_84_IINC:
        case OPCODE_99_IFEQ:
        case OPCODE_9A_IFNE:
        case OPCODE_9B_IFLT:
        case OPCODE_9C_IFGE:
        case OPCODE_9D_IFGT:
        case OPCODE_9E_IFLE:
        case OPCODE_9F_IF_ICMPEQ:
        case OPCODE_A0_IF_ICMPNE:
        case OPCODE_A1_IF_ICMPLT:
        case OPCODE_A2_IF_ICMPGE:
        case OPCODE_A3_IF_ICMPGT:
        case OPCODE_A4_IF_ICMPLE:
        case OPCODE_A5_IF_ACMPEQ:
        case OPCODE_A6_IF_ACMPNE:
        case OPCODE_A7_GOTO:
        case OPCODE_A8_JSR:
        case OPCODE_B2_GETSTATIC:
        case OPCODE_B3_PUTSTATIC:
        case OPCODE_B4_GETFIELD:
        case OPCODE_B5_PUTFIELD:
        case OPCODE_B6_INVOKEVIRTUAL:
        case OPCODE_B7_INVOKESPECIAL:
        case OPCODE_B8_INVOKESTATIC:
        case OPCODE_BB_NEW:
        case OPCODE_BD_ANEWARRAY:
        case OPCODE_C0_CHECKCAST:
        case OPCODE_C1_INSTANCEOF:
        case OPCODE_C6_IFNULL:
        case OPCODE_C7_IFNONNULL:
            return(2);

        case OPCODE_C2_MONITORENTER:
        case OPCODE_C3_MONITOREXIT:
        case OPCODE_C4_WIDE:
        case OPCODE_C5_MULTIANEWARRAY:
            return(3);

        case OPCODE_B9_INVOKEINTERFACE:
        case OPCODE_C8_GOTO_W:
        case OPCODE_C9_JSR_W:
            return(4);

        case OPCODE_15_ILOAD:
        case OPCODE_16_LLOAD:
        case OPCODE_17_FLOAD:
        case OPCODE_18_DLOAD:
        case OPCODE_19_ALOAD:
        case OPCODE_36_ISTORE:
        case OPCODE_37_LSTORE:
        case OPCODE_38_FSTORE:
        case OPCODE_39_DSTORE:
        case OPCODE_3A_ASTORE:
        case OPCODE_A9_RET:
            return((rtrue == iswide) ? 1 : 2);


        case OPCODE_AA_TABLESWITCH:
                return(/* pad byte count */
                       (sizeof(u4) - sizeof(opcode) -
                        (pc->offset & (sizeof(u4) - sizeof(opcode))))

                       + 16); /* Always at least 16 bytes of operands */


        case OPCODE_AB_LOOKUPSWITCH:
                return(/* pad byte count */
                       (sizeof(u4) - sizeof(opcode) -
                        (pc->offset & (sizeof(u4) - sizeof(opcode))))

                       + 16); /* Always at least 16 bytes of operands */

        default:
/*NOTREACHED*/
            return(0); /* There are no other possible cases */

    } /* switch(opcode) */

/*NOTREACHED*/

} /* End of operand_bytecount() */


#include "util.h" /* For definition of sysDbgMsg() */

/*!
 * @brief Opcode execution context, without operands
 */
#define OPCODE_MSG_FORMAT "thr=%04.4x " \
                          "SP=%04.4x TOS=%08.8x FP=%04.4x " \
                          " /OFP=%04.4x GC=%08.8x LS=%04.4x/  " \
                          "PC=%04.4x.%04.4x.%04.4x.%04.4x.%04.4x  " \
                          "%-15.15s %02.2x "

/*!
 * @brief Fixed group of debug print parameters.
 *
 */
#define OPCODE_MSG_FIXED_PARMS              \
    level,                                  \
    fn,                                     \
    format,                                 \
    thridx,                                 \
    GET_SP(thridx),                         \
    GET_SP_WORD(thridx, 0, jint),           \
    GET_FP(thridx),                         \
    GET_FP_WORD(thridx,                     \
                JVMREG_STACK_LS_HEIGHT      \
                  + JVMREG_STACK_GC_HEIGHT  \
                  + JVMREG_STACK_GC_HEIGHT, \
                jint),                      \
    GET_FP_WORD(thridx,                     \
                JVMREG_STACK_LS_HEIGHT      \
                  + JVMREG_STACK_GC_HEIGHT, \
                jint),                      \
    GET_FP_WORD(thridx,                     \
                JVMREG_STACK_LS_HEIGHT,     \
                jint),                      \
    pc->clsidx,                             \
    pc->mthidx,                             \
    pc->codeatridx,                         \
    pc->excpatridx,                         \
    pc->offset,                             \
    opcode_names[pcode[pc->offset]],        \
    pcode[pc->offset + 0 /* opcode */]

/*!
 * @brief Shortcut expression for addressing operand bytes
 *
 * @param  operand     Offset from opcode to index an operand byte
 *
 * @returns Java byte code operany byte at offset
 */
#define OPR(operand) (pcode[pc->offset + operand])

/*!
 * @brief disassemble opcode and show all operand bytes
 *
 * @param level     Messge debug level needed for display.
 * @param fn        Function name calling this utility.
 * @param thridx    Thread table index of code to disassemble.
 * @param pcode     Pointer to code array being disassembled.
 * @param pc        Program counter into the code being disassembled.
 * @param iswide    This opcode is now in the context of a WIDE opcode.
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
static void opcode_disassemble(jvm_debug_level_enum  level,
                               const char           *fn,
                               jvm_thread_index      thridx,
                               jvm_virtual_opcode   *pcode,
                               jvm_pc               *pc,
                               rboolean              iswide)
{
    ARCH_FUNCTION_NAME(opcode_disassemble);
    rint num_operand_bytes;
    char *format;

    num_operand_bytes = operand_bytecount(pcode, pc, iswide);


    switch(num_operand_bytes)
    {
        case 19:
            format = OPCODE_MSG_FORMAT " %02.2x%02.2x%02.2x\n"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS, OPR(1), OPR(2), OPR(3),
                      OPR(4),  OPR(5),  OPR(6),  OPR(7),
                      OPR(8),  OPR(9),  OPR(10), OPR(11),
                      OPR(12), OPR(13), OPR(14), OPR(15),
                      OPR(16), OPR(17), OPR(18), OPR(19));
            return;

        case 18:
            format = OPCODE_MSG_FORMAT " %02.2x%02.2x\n"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS, OPR(1), OPR(2),
                      OPR(3),  OPR(4),  OPR(5),  OPR(6),
                      OPR(7),  OPR(8),  OPR(9),  OPR(10),
                      OPR(11), OPR(12), OPR(13), OPR(14),
                      OPR(15), OPR(16), OPR(17), OPR(18));
            return;

        case 17:
            format = OPCODE_MSG_FORMAT " %02.2x\n"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS, OPR(1),
                      OPR(2),  OPR(3),  OPR(4),  OPR(5),
                      OPR(6),  OPR(7),  OPR(8),  OPR(9),
                      OPR(10), OPR(11), OPR(12), OPR(13),
                      OPR(14), OPR(15), OPR(16), OPR(17));
            return;

        case 16:
            format = OPCODE_MSG_FORMAT "\n"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x"
                                       " %02.2x%02.2x%02.2x%02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS,
                      OPR(1),  OPR(2),  OPR(3),  OPR(4),
                      OPR(5),  OPR(6),  OPR(7),  OPR(8),
                      OPR(9),  OPR(10), OPR(11), OPR(12),
                      OPR(13), OPR(14), OPR(15), OPR(16));
            return;

        case 4:
            format = OPCODE_MSG_FORMAT " %02.2x %02.2x %02.2x %02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS,
                      OPR(1), OPR(2), OPR(3), OPR(4));
            return;

        case 3:
            format = OPCODE_MSG_FORMAT " %02.2x %02.2x %02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS, OPR(1), OPR(2), OPR(3));
            return;

        case 2:
            format = OPCODE_MSG_FORMAT " %02.2x %02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS, OPR(1), OPR(2));
            return;

        case 1:
            format = OPCODE_MSG_FORMAT " %02.2x";
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS, OPR(1));
            return;

        case 0:
        default:  /* Should have no other cases, but just in case */
            format = OPCODE_MSG_FORMAT;
            sysDbgMsg(OPCODE_MSG_FIXED_PARMS);
            return;
    }

/*NOTREACHED*/

} /* END of opcode_disassemble() */

#endif /* I_AM_OPCODE_C */


/* EOF */
