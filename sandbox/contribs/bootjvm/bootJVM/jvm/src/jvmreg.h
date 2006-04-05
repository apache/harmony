#ifndef _jvmreg_h_included_
#define _jvmreg_h_included_

/*!
 * @file jvmreg.h
 *
 * @brief Registers of the Java Virtual Machine, including
 * the program counter, stack pointer, frame pointer, etc.
 *
 * Definition of the JVM registers for this real machine
 * implementation, namely, the stack area and program counter.
 * There are a significant number of macros available for
 * navigating push-up stack area, the stack pointer, and
 * stack frame.
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

ARCH_HEADER_COPYRIGHT_APACHE(jvmreg, h,
"$URL$",
"$Id$");

/*!
 * @brief Program counter.
 *
 * The program counter is the location in the code array of a
 * JVM method of current JVM instruction.  Definition includes
 * class, method, attribute of code area, and offset in the
 * code array.
 *
 * @todo  HARMONY-6-jvm-jvmreg.h-1 Does an @p @b arraydims item need
 *        to be added for processing of array classes, or is the
 *        scalar concept sufficient in code?
 */
typedef struct
{
    jvm_class_index     clsidx;   /**< class[clsidx] of code location*/
    jvm_method_index    mthidx;   /**< method[mthidx] of code location*/
    jvm_attribute_index codeatridx;/**< attributes[atridx] of code */
    jvm_attribute_index excpatridx;/**< attributes[atridx] of
                                                           exceptions */
    jvm_pc_offset       offset;    /**< instruction within code area */
} jvm_pc;


/*!
 * The JVM's virtual operation codes are defined as single bytes.
 * This type definition is used to address them.
 */
typedef u1 jvm_virtual_opcode;


/*!
 * @name Two types of exception tables.
 *
 * The JVM exception index table (defined by @b Exceptions attribute)
 * and JVM exception table (defined inside @b Code attribute) are both
 * simple offsets into their respective tables.  Both are bound up
 * directly with the program counter, so are defined here.
 */
/*@{ */

typedef u2 jvm_exception_table_index;
typedef u2 jvm_exception_index_table_index;

/*@} */



/*!
 * @brief Access structures of stack at certain index
 */
#define STACK(thridx, stkidx) THREAD(thridx).stack[stkidx]


/*!
 * @name Stack frame geometry.
 *
 * Frame height = PC + GC + FP + max_locals (from code atr) + SA (aka
 *  max_stack, SA being scratch area, the operand stack).
 * Minimum will be a zero-sized max_locals plus current SA.
 */

/*@{ */ /* Begin grouped definitions */

                                /*! Each of 5 items takes 1 stack word*/
#define JVMREG_STACK_PC_HEIGHT 5


                                 /*! Frame pointer takes one word */
#define JVMREG_STACK_FP_HEIGHT 1

#ifdef CONFIG_WORDSIZE64
                                 /*! GC real machine 64-bit ptr
                                        takes 2 words */
#define JVMREG_STACK_GC_HEIGHT 2
#else
                                 /*! GC real machine 32-bit ptr
                                        takes 1 word */
#define JVMREG_STACK_GC_HEIGHT 1
#endif

                                 /*! Num local storage words
                                        takes 1 word */
#define JVMREG_STACK_LS_HEIGHT 1

/*!
 * @warning WATCH OUT! When invoking POP_GC() you are working with a
 *          partially torn down frame, so can't use this macro:
 */
#define JVMREG_STACK_MIN_FRAME_HEIGHT (JVMREG_STACK_GC_HEIGHT + \
                                    JVMREG_STACK_FP_HEIGHT + \
                                    JVMREG_STACK_LS_HEIGHT)

/*! Offsets from current FP of local storage size word */
#define JVMREG_STACK_LS_OFFSET JVMREG_STACK_LS_HEIGHT

/*! Offsets from current FP of garbage collection pointer */
#define JVMREG_STACK_GC_OFFSET JVMREG_STACK_LS_OFFSET \
                               + JVMREG_STACK_GC_HEIGHT

/*! Offsets from current FP of old frame pointer */
#define JVMREG_STACK_FP_OFFSET JVMREG_STACK_GC_OFFSET \
                               + JVMREG_STACK_FP_HEIGHT

/*! Offsets from current FP of stack pointer */
#define JVMREG_STACK_PC_OFFSET JVMREG_STACK_FP_OFFSET \
                               + JVMREG_STACK_PC_HEIGHT

/*@} */ /* End of grouped definitions */


/*!
 * @name Reading and writing the stack pointer.
 *
 * @brief Load/store values from/to the stack pointer itself, 
 * not stack memory it points to.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 * @param value   (jvm_sp) value to store into stack pointer.
 *
 *
 * @returns The @b GET_SP() macros return a (jvm_sp) value of
 *          a stack pointer, the others return
 *          @link #rvoid rvoid@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define GET_SP(thridx) (THREAD(thridx).sp)


#define PUT_SP_IMMEDIATE(thridx, value) THREAD(thridx).sp = value

/*@} */ /* End of grouped definitions */


/*!
 * @name Reading and writing into the stack area.
 *
 *
 * @param thridx  Thread index of thread whose stack it to
 *                be manipulated.
 *
 * @param idx     Index into stack from SP of this thread's stack.
 *
 * @param value   (@link #jint jint@endlink) value to store into
 *                stack location.
 *
 * @param cast    Data type (cast) for return value
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Read a word at given depth in stack.
 *
 * @returns word at @c @b idx as requested (cast) data type.
 *
 */
#define GET_SP_WORD(thridx, idx, cast) \
    ((cast) (STACK(thridx, GET_SP(thridx) - idx)))

/*!
 * @brief Write a word at given depth in stack.
 *
 * @warning The @c @b value parameter @e must be of
 *          type @link #jint jint@endlink.  It will
 *          be cast as such in order to fit, but if
 *          it is a @link #jfloat jfloat@endlink or
 *          wider than this, the wrong value @e will
 *          be stored without compiler or run-time
 *          notification.
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
#define PUT_SP_WORD(thridx, idx, value) \
    STACK(thridx, GET_SP(thridx) - idx) = (jint) (value)

/*!
 * @brief Read a word at given frame pointer offset in stack.
 *
 * This macro is typically used to read parts of the stack frame
 * <em>not including</em> the local variable area.  Therefore,
 * the index is @e positive, unlike @link #GET_SP_WORD()
   GET_SP_WORD@endlink.
 *
 * @returns word at @c @b idx as requested (cast) data type.
 *
 */
#define GET_FP_WORD(thridx, idx, cast) \
    ((cast) (STACK(thridx, GET_FP(thridx) + idx)))

/*!
 * @brief Write a word at given frame pointer offset in stack.
 *
 * This macro is typically used to read parts of the stack frame
 * <em>not including</em> the local variable area.  Therefore,
 * the index is @e positive, unlike @link #PUT_SP_WORD()
   PUT_SP_WORD@endlink.
 *
 * @warning The @c @b value parameter @e must be of
 *          type @link #jint jint@endlink.  It will
 *          be cast as such in order to fit, but if
 *          it is a @link #jfloat jfloat@endlink or
 *          wider than this, the wrong value @e will
 *          be stored without compiler or run-time
 *          notification.
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
#define PUT_FP_WORD(thridx, idx, value) \
    STACK(thridx, GET_FP(thridx) + idx) = (jint) (value)

/*@} */ /* End of grouped definitions */


/*!
 * @name Reading and writing the frame pointer.
 *
 * @brief Load/store values from/to the frame pointer.
 *
 *
 * @param thridx  Thread index of thread whose frame pointer
 *                is to be referenced.
 *
 * @param value   (@link #jvm_sp jvm_sp@endlink) value to store
 *                into frame pointer.
 *
 *
 * @returns The @b GET_FP() macros return a (jvm_sp) value of
 *          a stack pointer, the others
 *          return @link #rvoid rvoid@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define GET_FP(thridx) (THREAD(thridx).fp)

#define PUT_FP_IMMEDIATE(thridx, value)  THREAD(thridx).fp = (value)

/*@} */ /* End of grouped definitions */


/*!
 * @brief Retrieve the stack frame garbage collection pointer.
 *
 *
 * @param thridx  Thread index of thread whose garbage collection
 *                pointer is to be referenced.
 *
 *
 * @returns (@link #rvoid rvoid@endlink *) is a real machine untyped
 *          pointer that is stored in a JVM stack location.
 *          Adjustments for 32-bit and 64-bit real machine pointers
 *          are already considered by @link #JVMREG_STACK_GC_HEIGHT
            JVMREG_STACK_GC_HEIGHT@endlink.
 *
 */
#define GET_GC(thridx) \
    ((rvoid *) &STACK(thridx, GET_FP(thridx) + JVMREG_STACK_GC_OFFSET))

/*!
 * @name Move the stack pointer up and down.
 *
 * @brief Increment/decrement SP (N/A to frame pointer).
 *
 * It is safe to reference @b SP in the @p @b value parameter.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 * @param value   (jint) value to change stack pointer by.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

/*@{ */ /* Begin grouped definitions */

#define INC_SP(thridx, value) THREAD(thridx).sp += (value)
#define DEC_SP(thridx, value) THREAD(thridx).sp -= (value)

/*@} */ /* End of grouped definitions */


/*!
 * @name Stack push and pop for any 32-bit data type.
 *
 * @brief Push/pop item to/from stack (MUST be integer size!).
 *
 * @warning <b>DO NOT</b> push/pop any 64-bit item, namely
 *          any @link #jlong jlong@endlink or
 *          @link #jdouble jdouble@endlink value.
 *          The stack pointer is only adjusted by
 *          a single 32-bit word.  Use two operations
 *          to push/pop each half of such types.  There
 *          are a number of examples in code, both in
 *          @link jvm/src/opcode.c opcode.c@endlink and
 *          @link jvm/src/native.c native.c@endlink.
 *
 * @warning It is @e not safe to reference @b SP in the @p @b item
 *          parameter of @b POP().
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 * @param item    (@b cast) variable to push/pop to/from the stack.
 *
 * @param cast     Type cast of variable being poped from the stack.
 *
 *
 * @returns @link #PUSH() PUSH()@endlink returns
 *          @link #rvoid rvoid@endlink, @link #POP() POP()@endlink
 *          returns the requested (jint) value.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PUSH(thridx, item) INC_SP(thridx, 1); \
                           PUT_SP_WORD(thridx, 0, (item))

#define POP(thridx, item, cast)  item = GET_SP_WORD(thridx, 0, cast); \
                                 DEC_SP(thridx, 1)

/*@} */ /* End of grouped definitions */

/*!
 * @name Stack push and pop of frame pointer.
 *
 * @brief Push/pop FP to/from stack.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 *
 * @returns @link #PUSH_FP() PUSH_FP()@endlink returns
 *          @link #rvoid rvoid@endlink, @link #POP_FP() POP_FP()@endlink
 *          returns the requested (jvm_sp) value.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PUSH_FP(thridx) PUSH(thridx, THREAD(thridx).fp)

#define POP_FP(thridx)   POP(thridx, THREAD(thridx).fp, jvm_sp)

/*@} */ /* End of grouped definitions */


/*!
 * @name Stack push and pop of garbage collection pointer.
 *
 * @brief Push/pop GC pointer to/from stack.
 *
 * Due to the potential for multiple GC implementations in this JVM,
 * the GC pointer here is @e not related to any one of them.  Instead,
 * it is cast here as a simple @link #rvoid rvoid@endlink pointer so as
 * to support all of them.
 *
 * @todo  HARMONY-6-jvm-jvmreg.h-2 Verify that the 64-bit real pointer
 *        calculations work properly for @b PUSH_GC() and @b POP_GC()
 *        (namely, where use fo JVMREG_STACK_GC_HEIGHT is involved)
 *
 * @warning WATCH OUT! When invoking @b POP_GC() you are working with a
 *          partially torn down frame, so can't use standard macros
 *          to calculate stack offsets!
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PUSH_GC(thridx, num_locals)                                    \
    INC_SP(thridx, JVMREG_STACK_GC_HEIGHT);                            \
    ((rvoid *)                                                         \
     *(&STACK(thridx, THREAD(thridx).sp - JVMREG_STACK_GC_HEIGHT+1))) =\
        GC_STACK_NEW(thridx, num_locals)

#define POP_GC(thridx)                                               \
    GC_STACK_DELETE(thridx,                                          \
                     ((rvoid **)                                     \
                      &STACK(thridx, THREAD(thridx).sp -             \
                                       JVMREG_STACK_GC_HEIGHT + 1)), \
                     (&STACK(thridx, THREAD(thridx).sp -             \
                                     JVMREG_STACK_GC_HEIGHT + 1 -    \
                                     JVMREG_STACK_LS_HEIGHT - 1)));  \
    DEC_SP(thridx, JVMREG_STACK_GC_HEIGHT)

/*@} */ /* End of grouped definitions */


/*!
 * @name Push and pop of local variables.
 *
 * @brief Add/remove empty space for local storage to/from stack.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 * @param value   (@link #jint jint@endlink) value to change
 *                stack pointer by.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PUSH_LOCAL(thridx, items) INC_SP(thridx, items) /*! alias */
#define POP_LOCAL(thridx, items)  DEC_SP(thridx, items) /*! alias */

/*@} */ /* End of grouped definitions */


/*!
 * @name Locate position in stack frame of its components.
 *
 * @brief Locate the local storage size, garbage collection pointer,
 * old frame pointer, and start of local storage area.
 *
 * The current frame pointer always points to the first word of
 * the local storage area of the stack frame.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 *
 * @returns (jvm_sp) stack offset value of requested item.
 *
 * @todo  HARMONY-6-jvm-jvmreg.h-3 Verify that
 *        @link #JVMREG_FRAME_CURRENT_LOCAL_VAR_AREA()
          JVMREG_FRAME_CURRENT_LOCAL_VAR_AREA()@endlink does not
 *        need a '+1' addition.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMREG_FRAME_CURRENT_LOCAL_STORAGE_SIZE(thridx) \
    (GET_FP(thridx) + JVMREG_STACK_LS_OFFSET)

#define JVMREG_FRAME_GC(thridx) \
    (GET_FP(thridx) + JVMREG_STACK_GC_OFFSET)

#define JVMREG_FRAME_PREVIOUS_FP(thridx) \
    (GET_FP(thridx) + JVMREG_STACK_FP_OFFSET)

#define JVMREG_FRAME_CURRENT_LOCAL_VAR_AREA(thridx) \
    (GET_FP(thridx) /* + 1 */ - \
     STACK(thridx, JVMREG_FRAME_CURRENT_LOCAL_STORAGE_SIZE(thridx)))

/*@} */ /* End of grouped definitions */


/*!
 * @name Macros for manipulating program counter.
 *
 * @brief load the program counter and store its contents from/to
 * an arbitrary location, from/to the stack, and load with an
 * immediate value.
 *
 *
 * @param _thridx      Thread index of thread whose stack pointer
 *                     is to be referenced.
 *
 * @param _source      (@link #jvm_pc jvm_pc@endlink) value to be loaded
 *                     into program counter.
 *
 * @param _clsidx      (@link #jvm_class_index jvm_class_index@endlink)
 *                     value to be loaded into program counter class
 *                     index
 *
 * @param _mthidx      (@link #jvm_method_index
                       jvm_method_index@endlink) value to be loaded
 *                     into program counter class index
 *
 * @param _codeatridx  (@link #jvm_attribute_index
                       jvm_attribute_index@endlink) value to be loaded
 *                     into program counter class index
 *
 * @param _excpatridx  (@link #jvm_attribute_index
                       jvm_attribute_index@endlink) value to be loaded
 *                     into program counter class index
 *
 * @param _excpatridx  (@link #jvm_attribute_index
                       jvm_attribute_index@endlink) value to be loaded
 *                     into program counter class index
 *
 * @param _offset      (@link #jvm_pc_offset jvm_pc_offset@endlink)
 *                     value to be loaded into program counter class
 *                     index
 *
 * @param _field       Field from program counter to extract.
 *
 *
 * @returns (jvm_sp) stack offset value of requested item.
 *
 */
/*@{ */ /* Begin grouped definitions */

#define PUT_PC(_thridx, _source)                      \
    THREAD(_thridx).pc.clsidx     = (_source).clsidx; \
    THREAD(_thridx).pc.mthidx     = (_source).mthidx; \
    THREAD(_thridx).pc.codeatridx = (_source).atridx; \
    THREAD(_thridx).pc.excpatridx = (_source).atridx; \
    THREAD(_thridx).pc.offset     = (_source).offset

#define PUT_PC_IMMEDIATE(_thridx,                  \
                         _clsidx,                  \
                         _mthidx,                  \
                         _codeatridx,              \
                         _excpatridx,              \
                         _offset)                  \
    THREAD(_thridx).pc.clsidx     = (_clsidx);     \
    THREAD(_thridx).pc.mthidx     = (_mthidx);     \
    THREAD(_thridx).pc.codeatridx = (_codeatridx); \
    THREAD(_thridx).pc.excpatridx = (_excpatridx); \
    THREAD(_thridx).pc.offset     = (_offset)

#define GET_PC(_thridx, _target)                       \
    _target.clsidx     = THREAD(_thridx).pc.clsidx     \
    _target.mthidx     = THREAD(_thridx).pc.mthidx     \
    _target.codeatridx = THREAD(_thridx).pc.codeatridx \
    _target.excpatridx = THREAD(_thridx).pc.excpatridx \
    _target.offset     = THREAD(_thridx).pc.offset

#define GET_PC_FIELD_IMMEDIATE(_thridx, _field) \
    THREAD(_thridx).pc._field

#define GET_PC_FIELD(_thridx, _target, _field) \
    _target            = GET_PC_FIELD_IMMEDIATE(_thridx, _field)

#define PUSH_PC(_thridx)                          \
    PUSH(_thridx, THREAD(_thridx).pc.offset);     \
    PUSH(_thridx, THREAD(_thridx).pc.excpatridx); \
    PUSH(_thridx, THREAD(_thridx).pc.codeatridx); \
    PUSH(_thridx, THREAD(_thridx).pc.mthidx);     \
    PUSH(_thridx, THREAD(_thridx).pc.clsidx); /* Extra ; */

#define POP_PC(_thridx)                                       \
    POP(_thridx, THREAD(_thridx).pc.clsidx, jvm_class_index); \
    POP(_thridx, THREAD(_thridx).pc.mthidx, u2);              \
    POP(_thridx, THREAD(_thridx).pc.codeatridx, u2);          \
    POP(_thridx, THREAD(_thridx).pc.excpatridx, u2);          \
    POP(_thridx, THREAD(_thridx).pc.offset, jvm_pc_offset); /*Extra ; */

/*@} */ /* End of grouped definitions */


/*!
 * @name Location of the old program counter in the current stack frame.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMREG_STACK_PC_CLSIDX_OFFSET     JVMREG_STACK_PC_OFFSET - 0
#define JVMREG_STACK_PC_MTHIDX_OFFSET     JVMREG_STACK_PC_OFFSET - 1
#define JVMREG_STACK_PC_CODEATRIDX_OFFSET JVMREG_STACK_PC_OFFSET - 2
#define JVMREG_STACK_PC_EXCPATRIDX_OFFSET JVMREG_STACK_PC_OFFSET - 3
#define JVMREG_STACK_PC_OFFSET_OFFSET     JVMREG_STACK_PC_OFFSET - 4

/*@} */ /* End of grouped definitions */


/*!
 * @name Navigate the code area of of the current method.
 *
 * @brief Calculate pointers to several important real machine
 * addresses in the class file of the current method of a
 * specific thread:
 *
 * <ul>
 * <li>
 *   <b>(1)</b> current program counter in THIS_PC() macro.
 * </li>
 * <li>
 *   <b>(2)</b> current class file in THIS_PCFS() macro.
 * </li>
 * <li>
 *   <b>(3)</b> PC exception index table base (list of legal exceptions)
 *              in DEREFERENCE_PC_EXCEPTIONS_ATTRIBUTE() macro.
 * </li>
 * <li>
 *   <b>(4)</b> PC exception base (list of actual exceptions thrown)
 *              in DEREFERENCE_PC_EXCEPTION_TABLE() macro.
 * </li>
 * <li>
 *   <b>(5)</b> PC base (start of method) in DEREFERENCE_PC_CODE_BASE()
 *              macro.
 * </li>
 * <li>
 *   <b>(6)</b> current opcode in DEREFERENCE_PC_CODE_CURRENT_OPCODE()
 *              macro.
 * </li>
 * </ul>
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @def THIS_PC()
 *
 * @brief Locate program counter for a given thread.
 *
 *
 * @param thridx  Thread table index of program counter to locate.
 *
 *
 * @returns address of program counter in this thread table.
 *
 */
#define THIS_PC(thridx) (&THREAD(thridx).pc)


/*!
 * @def THIS_PCFS()
 *
 * @brief Locate ClassFile for current program counter
 *  on a given thread.
 *
 *
 * @param thridx  Thread table index of class to locate for
 *                current program counter.
 *
 *
 * @returns address of ClassFile structure containing code at
 *          current program counter on this thread.
 *
 */
#define THIS_PCFS(thridx) \
    (CLASS_OBJECT_LINKAGE(THIS_PC(thridx)->clsidx)->pcfs)


/*!
 * @def DEREFERENCE_PC_GENERIC_ATTRIBUTE()
 *
 * @brief Untyped attribute in method table for current program
 * counter on a given thread.
 *
 *
 * @param thridx    Thread table index of attribute to locate for
 *                  current program counter.
 *
 * @param pc_member jvm_pc member index in current program counter to
 *                  pick out and examine.
 *
 *
 * @returns Plain attribute address (no special attribute type)
 *          of an attribute in method area of the current program
 *          counter on this thread, or @link #rnull rnull@endlink if
 *          a @b BAD attribute .
 *
 *
 * @todo HARMONY-6-jvm-jvmreg.h-4 Watch out for when @p @b codeatridx
 *       is a BAD index, namely
 *       @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *
 */
#define DEREFERENCE_PC_GENERIC_ATTRIBUTE(thridx, pc_member) \
    ((jvm_attribute_index_bad != THIS_PC(thridx)->pc_member) \
     ? (&THIS_PCFS(thridx)->methods[THIS_PC(thridx)->mthidx] \
                            ->attributes[THIS_PC(thridx)->pc_member] \
                              ->ai) \
     : rnull)


/*!
 * @def DEREFERENCE_PC_EXCEPTIONS_ATTRIBUTE()
 *
 * @brief Exceptions_attribute in method table for current program
 * counter on a given thread.
 *
 *
 * @param thridx  Thread table index of @b Exceptions attribute to
 *                locate for current program counter.
 *
 *
 * @returns Address of @b Exceptions attribute in method area of
 *          the current program counter on this thread.
 *
 */
#define DEREFERENCE_PC_EXCEPTIONS_ATTRIBUTE(thridx) \
    ((Exceptions_attribute *) \
     DEREFERENCE_PC_GENERIC_ATTRIBUTE(thridx, excpatridx))


/*!
 * @def DEREFERENCE_PC_CODE_ATTRIBUTE()
 *
 * @brief Code_attribute in method table for current program counter
 * on a given thread.
 *
 *
 * @param thridx  Thread table index of @b Code attribute to locate for
 *                current program counter.
 *
 *
 * @returns Address of @b Code attribute in method area of
 *          the current program counter on this thread, or
 *          @link #rnull rnull@endlink if not present in class file.
 *
 */
#define DEREFERENCE_PC_CODE_ATTRIBUTE(thridx) \
    ((Code_attribute *) \
     DEREFERENCE_PC_GENERIC_ATTRIBUTE(thridx, codeatridx))


/*!
 * @def DEREFERENCE_PC_EXCEPTION_TABLE()
 *
 * @brief Real address of exception table of this method in
 * current program counter on a given thread.
 *
 *
 * @param thridx  Thread table index of program counter to locate.
 *
 *
 * @returns address of program counter in this thread table, or
 *          @link #rnull rnull@endlink if not present in class file.
 *
 */
#define DEREFERENCE_PC_EXCEPTION_TABLE(thridx) \
    (DEREFERENCE_PC_CODE_ATTRIBUTE(thridx)->exception_table)


/*!
 * @def DEREFERENCE_PC_CODE_BASE
 *
 * @brief Real address of first opcode of this method in current
 * program counter on a given thread.
 *
 *
 * @param thridx  Thread table index of opcode to locate for
 *                current program counter.
 *
 *
 * @returns Real machine address of @e first opcode in current
 *          program counter on this thread.
 *
 */
#define DEREFERENCE_PC_CODE_BASE(thridx) \
    (DEREFERENCE_PC_CODE_ATTRIBUTE(thridx)->code)


/*!
 * @def DEREFERENCE_PC_CODE_CURRENT_OPCODE()
 *
 * @brief Real address of first opcode of this method in current
 * program counter on a given thread.
 *
 *
 * @param thridx  Thread table index of opcode to locate for
 *                current program counter.
 *
 *
 * @returns Real machine address of @e current opcode in current
 *          program counter on this thread.
 *
 */
#define DEREFERENCE_PC_CODE_CURRENT_OPCODE(thridx) \
    (&DEREFERENCE_PC_CODE_BASE(thridx)[THIS_PC(thridx)->offset])

/*@} */ /* End of grouped definitions */


/*!
 * @name Geometry of a complete stack frame.
 *
 * @brief A complete stack frame is pushed every time a JVM virtual
 * method is called, where old program counter and old stack state
 * must be saved and a new one created on top of it.  The macro
 * @b PUSH_FRAME() is designed for this use.  For example,
 * here is an existing stack frame:
 *
 * @verbatim
       SP -->  [scratch] (operand stack, >= 0 words)  ... higher address
                 ...                                        ...
               [scratch]                                    ...
               [top of previous frame]                      ...
       FP -->  [locals of previous frame]             ... lower address
  
   @endverbatim
 *
 * During @b PUSH_FRAME(n), there are @b n words of local storage
 * allocated for JVM method scratch area, where @b n >= 0, up to
 * the maximum stack size less the top few words:
 *
 * @verbatim
  
   new SP -->  [PC of next JVM instruction]           ... higher address
               [old FP]                                     ...
               [GC pointer for this NEW frame]              ...
               [value 'n', size of LS (local storage) area beneath]
   new FP -->  [local n-1]                                  ...
                 ...                                        ...
               [local 2]                                    ...
               [local 1]                                    ...
               [local 0]                                    ...
  
   --- end of NEW frame ---
  
   old SP -->  [scratch] (same data shown above)            ...
                 ...                                        ...
               [scratch]                                    ...
               [top of previous frame]                      ...
   old FP -->  [locals of previous frame]             ... lower address
  
   --- end of OLD frame ---
  
   @endverbatim
 *
 *
 * POP_FRAME() does the reverse or @b PUSH_FRAME and removes
 * the top stack frame.  The final @b POP_FRAME() will have a
 * stored (old) frame pointer containing
 * @link #JVMCFG_NULL_SP JVMCFG_NULL_SP@endlink, so when this
 * frame is popped, @link #opcode_run() opcode_run()@endlink
 * will detect that this thread has finished running.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 * @param locals  Number of local variables to be reserved
 *                in the new stack frame.
 *
 *
 * @returns @link #rvoid rvoid@endlink.
 *
 *
 * @note The original layout of the local variable area was from
 *       <b><code>FP[0]</code></b> to
 *       <b><code>FP[0 - n + 1]</code></b>.  However, that would
 *       mean that in order to perform a virtual method call, the
 *       method's parameter block of @c @b p parameters would
 *       have to be popped off the JVM stack, stored somewhere, a
 *       new frame of @c @b n locals built with
 *       @link #PUSH_FRAME() PUSH_FRAME@endlink, and the parm block
 *       put into the local variables.  Even if the frame was built
 *       with <b><code>n - p</code></b> locals on top of that block,
 *       it would @e still have to be reversed so that
 *       <b><code>parm[0]</code></b> would reside in
 *       <b><code>FP[0]</code></b>. This overhead can be avoided by
 *       reversing the order of the local variable index to correspond
 *       directly to the parameters as they are found on the stack
 *       at @link #PUSH_FRAME PUSH_FRAME@endlink time.  This will
 *       correspond to the first @c @b p of the @c @b n local
 *       variables, where <b><code>n \>= p + m</code></b> at all times.
 *       Remember that for static methods, <b><code>m := 0</code></b>,
 *       namely, no @c @b this object hash is supplied, but for object
 *       instance methods, <b><code>m := 1</code></b> for the @c @b this
 *       object hash.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PUSH_FRAME(thridx, locals)                                     \
    PUSH_LOCAL(thridx, (locals));                                      \
    PUSH(thridx, (locals));                                            \
    PUSH_GC(thridx, (locals));                                         \
    PUSH_FP(thridx);                                                   \
    PUT_FP_IMMEDIATE(thridx,                                           \
                      GET_SP(thridx) - JVMREG_STACK_MIN_FRAME_HEIGHT); \
    PUSH_PC(thridx); /* Extra ; */

#define POP_FRAME(thridx)                                  \
    POP_PC(thridx);                                        \
    POP_FP(thridx);                                        \
    POP_GC(thridx);                                        \
    POP_LOCAL(thridx, (1 + GET_SP_WORD(thridx, 0, jint))); /* Extra ; */

/*@} */ /* End of grouped definitions */

/*!
 * @name Access the stack frame.
 *
 * @brief Read and write (jint) values from and to the current
 * stack frame.
 *
 * Accesses are @e always as (jint), casting is performed
 * outside of these macros.
 *
 *
 * @param thridx  Thread index of thread whose stack pointer
 *                is to be referenced.
 *
 * @param frmidx  Stack frame index (local variable index) of stack
 *                frame to be referenced.
 *
 * @param cast    Arbitrary data type that is of size (jint) or
 *                smaller.  This includes (jbyte), (jboolean),
 *                (jchar), (jshort), (jshort), and (jobject).
 *
 */
/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Read a local variable from the stack frame
 *
 * @note For (jlong) and (jdouble) local variables, there are
 *       @e two accesses required per JVM spec to retrieve the two
 *       (jint) words of such a data type.  Use bytegames_combine_long()
 *       and bytegames_combine_double(), respectively, to combine the
 *       two words into a single variable.  The parameters may be
 *       read directly from the stack frame with this macro.
 *
 *
 * @returns (jint) value of a local variable in the stack frame.
 *
 */
#define GET_LOCAL_VAR(thridx, frmidx) \
    ((jint) (STACK(thridx, \
                 JVMREG_FRAME_CURRENT_LOCAL_VAR_AREA(thridx) + frmidx)))

/*!
 * @brief Cast a @link #GET_LOCAL_VAR() GET_LOCAL_VAR()@endlink
 * as any arbitrary data type.
 *
 * @note This macro is not appropriate for (jlong) and (jdouble)
 *       data types since they require two (jint) local variable
 *       stack frame accesses.
 *
 * @returns @b (cast) value of (jint) local variable.
 *
 */
#define GET_TYPED_LOCAL_VAR(thridx, frmidx, cast) \
    ((cast) GET_LOCAL_VAR(thridx, frmidx)))


/*!
 * @brief Real machine address of a local variable in the stack frame
 *
 *
 * @returns Real machine (jint *) address of a (jint) local variable
 *          in the stack frame.
 *
 */
#define JINT_ADDRESS_LOCAL_VAR(thridx, frmidx) \
    ((jint *) &STACK(thridx, \
                  JVMREG_FRAME_CURRENT_LOCAL_VAR_AREA(thridx) + frmidx))


/*!
 * @brief Write a local variable from the stack frame
 *
 * @note For (jlong) and (jdouble) local variables, there are
 *       @e two accesses required per JVM spec to store the two
 *       (jint) words of such a data type.  Use bytegames_split_long()
 *       and bytegames_split_double(), respectively, to split
 *       a single variable into its two words.  The parameters may be
 *       written directly into the stack frame with this macro.
 *
 *
 * @returns (jint) value of a local variable in the stack frame.
 *
 */
#define PUT_LOCAL_VAR(thridx, frmidx, value) \
    *JINT_ADDRESS_LOCAL_VAR(thridx, frmidx) = (jint) (value)


/*@} */ /* End of grouped definitions */

/*!
 * @name Navigate stack frame in depth.
 *
 * @brief Use these macros to iteratively point up and down the
 * stack from from current fram to bottom of the stack.
 *
 *
 * @param thridx   Thread index of thread whose stack pointer
 *                 is to be referenced.
 *
 * @param some_fp  Stack pointer index of stack frame to be referenced.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @returns Requested frame pointer
 *
 */
#define NEXT_STACK_FRAME_GENERIC(thridx, some_fp) \
    STACK(thridx, some_fp + JVMREG_STACK_FP_OFFSET)

/*!
 * @returns @link #rtrue rtrue@endlink if @p some_fp now points
 *          to the bottom of the stack frame.
 *
 */
#define CHECK_FINAL_STACK_FRAME_GENERIC(thridx, some_fp)           \
    ((JVMCFG_NULL_SP == NEXT_STACK_FRAME_GENERIC(thridx, some_fp)) \
     ? rtrue                                                       \
     : rfalse)

#define FIRST_STACK_FRAME(thridx) GET_FP(thridx)

#define NEXT_STACK_FRAME(thridx) \
    NEXT_STACK_FRAME_GENERIC(thridx, GET_FP(thridx))

#define CHECK_FINAL_STACK_FRAME(thridx) \
    CHECK_FINAL_STACK_FRAME_GENERIC(thridx, GET_FP(thridx))

#define CHECK_FINAL_STACK_FRAME_ULTIMATE(thridx) \
    (JVMCFG_NULL_SP == GET_FP(thridx))

/*@} */ /* End of grouped definitions */


#endif /* _jvmreg_h_included_ */


/* EOF */
