/*!
 * @file opmacros.h
 *
 * @brief Java Virtual Machine inner loop execution support macros.
 *
 * Much of the JVM inner loop is repetitious, yet not so much so
 * that opcodes can simply be grouped together completely.  This set
 * of macros is used to implement almost all of the opcodes, typically
 * in their entirety.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/opmacros.h $
 *
 * \$Id: opmacros.h 0 09/28/2005 dlydick $
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
 * @version \$LastChangedRevision: 0 $
 *
 * @date \$LastChangedDate: 09/28/2005 $
 *
 * @author \$LastChangedBy: dlydick $
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_HEADER_COPYRIGHT_APACHE(opmacros, h,
"$URL: https://svn.apache.org/path/name/opmacros.h $",
"$Id: opmacros.h 0 09/28/2005 dlydick $");


/*!
 * @name Macro support for inner loop opcodes.
 *
 * @brief Common operations that are used in numerous opcodes
 * are gathered here so as to improve accuracy of implementation
 * and simplify the code at the expense of explicit code constructions.
 * (Notice how many some of these macros are used to see why this
 * approach is recommended!)  Any debugging will fix the whole lot.
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
 * <li> @c @e pc->offset     Add @c @b sizeof(u1) to current value
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
 * <li> @c @e pc->offset     Add @c @b sizeof(u2) to current value
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
 * <li> @c @e pc->offset     Add @c @b sizeof(u4) to current value
 * </li>
 * </ul>
 *
 */
#define GET_U4_OPERAND(u4var)                  \
    u4var = GETRI4((u4 *) &pcode[pc->offset]); \
    pc->offset += sizeof(u4)
 

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
    if (jvm_object_hash_null == objhash)                               \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_EXCEPTION,          \
                              JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION);\
/*NOTREACHED*/                                                         \
    }                                                                  \
    if ((JVMCFG_MAX_OBJECTS   <= objhash)                 ||           \
        (!(OBJECT_STATUS_INUSE & OBJECT(objhash).status)) ||           \
        (OBJECT_STATUS_NULL & OBJECT(objhash).status))                 \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                               JVMCLASS_JAVA_LANG_INTERNALERROR);      \
/*NOTREACHED*/                                                         \
    }


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
    if (arridx >= OBJECT(objhash).arraylength[0])                      \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_EXCEPTION,          \
                    JVMCLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDSEXCEPTION);\
/*NOTREACHED*/                                                         \
    }


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

/*!
 * @brief Calculate method_info pointer from program counter
 *
 * During the calculation, various scratch variables are
 * loaded and used to simplify the code.  The final result
 * is a (method_info *) stored the local variable @b pmth
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
 * <li>  @c @e clsidxmisc    Class index field from program counter
 * </li>
 * <li>  @c @e mthidxmisc    Method index field from program counter
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by program counter
 * </li>
 * <li>  @c @e pmth          Method pointer in classs file field from
 *                           class referenced by program counter
 * </li>
 * </ul>
 *
 */
#define CALCULATE_METHOD_INFO_FROM_PC                    \
    clsidxmisc = GET_PC_FIELD_IMMEDIATE(thridx, clsidx); \
    mthidxmisc = GET_PC_FIELD_IMMEDIATE(thridx, mthidx); \
    pcfsmisc   = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs; \
    pmth       = METHOD(clsidxmisc, mthidxmisc)


/*!
 * @brief Calculate ClassFile pointer from a class reference.
 *
 * During the calculation, various scratch variables are
 * loaded and used to simplify the code.  Two final results
 * include a (CONSTANT_Class_info *) stored in the local variable
 * @b pcpd_Class stored the local variable @b pcfsmisc
 * and a (CONSTANT_Class_info *) stored in the local variable
 * @b pcpd_Class
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
 * <li>  @c @e pcfs->constant_pool  Examine a @c @b constant_pool entry
 * </li>
 * </ul>
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e pcpd           pointer to a @c @b constant_pool entry
 * </li>
 * <li> @c @e pcpd_Class     @c @b pcpd as a CONSTANT_Class_info pointer
 * </li>
 * <li> @c @e clsidxmisc     Class index of fully bound class referenced
 *                           by @c @b pcpd_Class
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * </ul>
 *
 *
 */
#define CALCULATE_CLASS_INFO_FROM_CLASS_REFERENCE(clsnameidx)          \
    pcpd       = pcfs->constant_pool[clsnameidx];                      \
    pcpd_Class = PTR_THIS_CP_Class(pcpd);                              \
    clsidxmisc = pcpd_Class->LOCAL_Class_binding.clsidxJVM;            \
    if (jvm_class_index_null == clsidxmisc)                            \
    {                                                                  \
        /* Need local variable to avoid possible expansion confusion */\
        jvm_constant_pool_index cpidxOLD = clsnameidx;                 \
                                                                       \
        /* If class is not loaded, go retrieve it by UTF8 class name */\
        LATE_CLASS_LOAD(cpidxOLD);                                     \
    }                                                                  \
    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs; /* Extra ; */


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
 * <li> @c @e pcpd           pointer to a @c @b constant_pool entry,
 *                           ultimately pointing to the entry containing
 *                           the name of the input @c @b clsnameidx
 * </li>
 * <li> @c @e pcpd_Class     @c @b pcpd as a CONSTANT_Class_info pointer
 * </li>
 * <li> @c @e pcpd_Utf8      @c @b pcpd as a CONSTANT_Utf8_info pointer
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
    pcpd       = pcfs->constant_pool[clsnameidx]; /* Class name */   \
    pcpd_Class = PTR_THIS_CP_Class(pcpd);                            \
                                                  /* UTF8 string */  \
    pcpd       = pcfs->constant_pool[pcpd_Class->name_index];        \
    pcpd_Utf8  = PTR_THIS_CP_Utf8(pcpd);                             \
                                                                     \
    prchar_clsname = utf_utf2prchar(pcpd_Utf8);                      \
                                                                     \
    /* Try again to load class */                                    \
    clsidxmisc = class_load_resolve_clinit(prchar_clsname,           \
                                           CURRENT_THREAD,           \
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
    }




/*!
 * @brief Calculate method_info pointer from a method reference.
 *
 * During the calculation, various scratch variables are
 * loaded and used to simplify the code.  Two final results
 * include a (method_info *) stored the local variable @b pmth
 * and a (CONSTANT_Methodref_info *) stored in the local variable
 * @b pcpd_Methodref
 *
 * @param Methodref  @c @b constant_pool index into class file of
 *                   current class (as indicated in the program counter)
 *                   that is a method reference entry.
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(Methodref)
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
 * <li> @c @e pcpd           pointer to a @c @b constant_pool entry
 * </li>
 * <li> @c @e pcpd_Methodref @c @b pcpd as a CONSTANT_Methodref_info
 *                           pointer
 * </li>
 * <li> @c @e clsidxmisc     Class index of fully bound class referenced
 *                           by @c @b pcpd_Methodref
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * <li>  @c @e pmth          method_info table entry referenced by
 *                           @c @b pcpd_Methodref
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
           if requested method is not found in the class@endlink.
 *
 *
 */
#define CALCULATE_METHOD_INFO_FROM_METHOD_REFERENCE(Methodref)         \
    pcpd           = pcfs->constant_pool[Methodref];                   \
    pcpd_Methodref = PTR_THIS_CP_Methodref(pcpd);                      \
    clsidxmisc     = pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM;\
    if (jvm_class_index_null == clsidxmisc)                            \
    {                                                                  \
        /* If class is not loaded, go retrieve it by UTF8 class name */\
       LATE_CLASS_LOAD(pcpd_Methodref->class_index);                   \
                                                                       \
        /* Check if method exists in loaded class */                   \
        clsidxmisc = pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM;\
        if (jvm_class_index_null == clsidxmisc)                        \
        {                                                              \
            thread_throw_exception(thridx,                             \
                                   THREAD_STATUS_THREW_ERROR,          \
                                JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR); \
/*NOTREACHED*/                                                         \
        }                                                              \
    }                                                                  \
                                                                       \
    mthidxmisc = pcpd_Methodref->LOCAL_Methodref_binding.mthidxJVM;    \
    if (jvm_method_index_bad == mthidxmisc)                            \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                                JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR); \
/*NOTREACHED*/                                                         \
    }                                                                  \
                                                                       \
    pcfsmisc       = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs;           \
    pmth           = pcfsmisc->methods[mthidxmisc]


/*!
 * @brief Check for code attribute index in local method binding.
 *
 *
 * @param codeatridx  Code attribute index from a local method binding
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li>  @c @e thridx        Thread table index of current thread
 *                           (input parameter to
 *                           @link #opcode_run opcode_run()@endlink)
 * </li>
 * <li> @c @e macro_expansion_of(codeatridx)
 *                           Any expression that resolves to an
 *                           attribute index
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
 * @throws JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
      if requested class static field is not found in the class@endlink.
 *
 */
#define CHECK_VALID_CODEATRIDX(codeatridx)                            \
    if (jvm_attribute_index_bad == codeatridx)                        \
    {                                                                 \
        thread_throw_exception(thridx,                                \
                               THREAD_STATUS_THREW_ERROR,             \
                               JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR); \
/*NOTREACHED*/                                                        \
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
 * <li> @c @e pc->offset     Add @c @b sizeof(u2) to current value
 * </li>
 * </ul>
 *
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
           if requested method is an object instance method@endlink.
 *
 */
#define CHECK_STATIC_METHOD                                      \
                                                                 \
    /* Must be a static method */                                \
    if (!(ACC_STATIC & pmth->access_flags))                      \
    {                                                            \
        thread_throw_exception(thridx,                           \
                               THREAD_STATUS_THREW_ERROR,        \
                               JVMCLASS_JAVA_LANG_VERIFYERROR);  \
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
 * @brief Calculate field_info pointer from a field reference.
 *
 * During the calculation, various scratch variables are
 * loaded and used to simplify the code.  Two final results
 * include a (field_info *) stored the local variable @b pfld
 * and a (CONSTANT_Fieldref_info *) stored in the local variable
 * @b pcpd_Fieldref
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
 * <li> @c @e pcpd           pointer to a @c @b constant_pool entry
 * </li>
 * <li> @c @e pcpd_Fieldref @c @b pcpd as a CONSTANT_Fieldref_info
 *                           pointer
 * </li>
 * <li> @c @e clsidxmisc     Class index of fully bound class referenced
 *                           by @c @b pcpd_Fieldref
 * </li>
 * <li>  @c @e pcfsmisc      @c @b pcfs class file field from class
 *                           referenced by @c @b clsidxmisc
 * </li>
 * <li>  @c @e pfld          field_info table entry referenced by
 *                           @c @b pcpd_Fieldref
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
    pcpd           = pcfs->constant_pool[Fieldref];                    \
    pcpd_Fieldref = PTR_THIS_CP_Fieldref(pcpd);                        \
    clsidxmisc     = pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM;  \
    if (jvm_class_index_null == clsidxmisc)                            \
    {                                                                  \
        /* If class is not loaded, go retrieve it by UTF8 class name */\
        LATE_CLASS_LOAD(pcpd_Fieldref->class_index);                   \
                                                                       \
        /* Check if field exists in loaded class */                    \
        clsidxmisc = pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM;  \
        if (jvm_class_index_null == clsidxmisc)                        \
        {                                                              \
            thread_throw_exception(thridx,                             \
                                   THREAD_STATUS_THREW_ERROR,          \
                                JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR);  \
/*NOTREACHED*/                                                         \
        }                                                              \
    }                                                                  \
                                                                       \
    fluidxmisc     = pcpd_Fieldref->LOCAL_Fieldref_binding.fluidxJVM;  \
    if (jvm_field_index_bad == fluidxmisc)                             \
    {                                                                  \
        thread_throw_exception(thridx,                                 \
                               THREAD_STATUS_THREW_ERROR,              \
                                JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR);  \
/*NOTREACHED*/                                                         \
    }                                                                  \
                                                                       \
    pcfsmisc       = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs;           \
    fluidxmisc     = pcpd_Fieldref->LOCAL_Fieldref_binding.fluidxJVM;  \
    pfld           = pcfsmisc                                          \
                       ->fields[CLASS(clsidxmisc)                      \
                                 .class_static_field_lookup[fluidxmisc]]


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
 * <li> @c @e pcpd_Fieldref  CONSTANT_Fieldref_info pointer to current
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
            (clsidxTMP != pcpd_Fieldref                          \
                            ->LOCAL_Fieldref_binding.clsidxJVM)) \
        {                                                        \
            thread_throw_exception(thridx,                       \
                                   THREAD_STATUS_THREW_ERROR,    \
                       JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR);   \
/*NOTREACHED*/                                                   \
        }                                                        \
    }


/*!
 * @brief Check if this field requires two @link #jint jint@endlink
 * accesses or just one.
 *
 *
 * JVM stack operations and local variable accesses need to know
 * if the datum to be moved takes one @link #jint jint@endlink slot
 * or two.  Items of types @link #jlong jlong@endlink and
 * @link #jdouble jdouble@endlink take two such accesses, all others
 * take just one.
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * <b>Local variables read:</b>
 * <ul>
 * <li> @c @e pcpd_Fieldref  CONSTANT_Fieldref_info pointer to current
 *                           field
 * </li>
 * </ul>
 *
 *
 * @returns @link #rtrue rtrue@endlink if this field takes two
 *          accesses, otherwise @link #rfalse rfalse@endlink for
 *          smaller types.
 *
 *
 * <b>Local variables written:</b>
 * <ul>
 * <li> @c @e none
 * </li>
 * </ul>
 *
 */
#define CHECK_TWO_ACCESSES                                         \
                                                                   \
    (((pcpd_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM ==      \
       BASETYPE_CHAR_J)                                         || \
      (pcpd_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM ==      \
       BASETYPE_CHAR_D))                                           \
    ? rtrue                                                        \
    : rfalse)


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
    if (CODE_CONSTRAINT_CODE_LENGTH_MAX < pc->offset)           \
    {                                                           \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
    }


/*!
 * @brief Adjust program counter by a 4-byte relative offset value.
 *
 *
 * @param offset4var  Variable of type @link #u4 u4@endlink holding
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
 * <li> @c @e macro_expansion_of(offset4var)
 *                           Any @link #u4 u4@endlink variable.
 * </li>
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
#define LOAD_TARGET_PC_OFFSET_WIDE(offset4var, instrlen)        \
    /* This will create a signed value */                       \
    pc->offset += ((jvm_pc_offset) offset4var)                  \
                   - instrlen;                                  \
                                                                \
    /*!                                                         \
     * @todo HARMONY-6-jvm-opmacros.h-8 Need to check           \
     *       whether max PC value itself is legal, thus         \
     *       whether comparison should be @b &lt; or @b &lt;=   \
     *                                                          \
     */                                                         \
                                                                \
    /*                                                          \
     * Don't need a lower bound test since offset is unsigned   \
     */                                                         \
    if (CODE_CONSTRAINT_CODE_LENGTH_MAX < pc->offset)           \
    {                                                           \
        thread_throw_exception(thridx,                          \
                               THREAD_STATUS_THREW_ERROR,       \
                               JVMCLASS_JAVA_LANG_VERIFYERROR); \
/*NOTREACHED*/                                                  \
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
 * <li> @c @e pcpd_Fieldref  CONSTANT_Fieldref_info pointer to current
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
 * @see PUTFIELD
 *
 * @see PUTSTATIC
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
    switch (pcpd_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM)       \
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


/*!
 * @brief Store out value by data type into class static field.
 *
 */
#define PUTSTATIC                                                  \
    PUTDATA(CLASS(pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM) \
              .class_static_field_data[fluidxmisc])


/*!
 * @brief Store out value by data type into object instance field.
 *
 */
#define PUTFIELD                                                     \
     PUTDATA(OBJECT(pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM) \
               .object_instance_field_data[fluidxmisc])


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
 * <li> @c @e pcpd_Fieldref  CONSTANT_Fieldref_info pointer to current
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
 * @see GETFIELD
 *
 * @see GETSTATIC
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
    switch (pcpd_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM)       \
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
 * @brief Retrieve value by data type from class static field.
 *
 */
#define GETSTATIC                                                  \
    GETDATA(CLASS(pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM) \
              .class_static_field_data[fluidxmisc])


/*!
 * @brief Retrieve value by data type from object instance field.
 *
 */
#define GETFIELD                                                    \
    GETDATA(OBJECT(pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM) \
              .object_instance_field_data[fluidxmisc])


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


/* EOF */
