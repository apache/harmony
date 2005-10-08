/*!
 * @file opcode.c
 *
 * @brief Java Virtual Machine inner loop virtual instruction execution.
 *
 * A specific group of exceptions that may be thrown by JVM execution
 * (spec section 2.16.4 unless specified otherwise) are not typically
 * checked by the Java program and stop thread execution unless caught
 * by a Java @c @b catch block.
 *
 * <ul>
 * <li>
 * @b ArithmeticException    Integer divide by zero
 * </li><li>
 * @b ArrayStoreException    Storage compatibility error between
 *                           array type lvalue vs component rvalue
 * </li><li>
 * @b ClassCastException     Type narrowing loses significance or
 *                           casting of an object to a type that
 *                           is not valid.
 * </li><li>
 * @b IllegalMonitorStateException  Thread attempted to wait() or
 *                           notify()  on an object that it has
 *                           not locked.
 * </li><li>
 * @b IndexOutOfBoundsException An index or a subrange was outside the
 *                           limits \>=0 unto \< lenghth/size for
 *                           an array, string, or vector.
 * </li><li>
 * @b NegativeArraySizeException Attempted to create an array with a
 *                           negative number of elements.
 * 
 * </li><li>
 * @b NullPointerException   An object reference to a
 *                           @link #jvm_object_hash_null
                             jvm_object_hash_null@endlink object
 *                           was attempted instead of to a real
 *                           and valid object.
 *
 * </li><li>
 * @b SecurityException      Violation of security policy.
 * </li>
 * </ul>
 *
 *
 *
 * A suite of errors may also be thrown that Java programs normally
 * to not attempt to @c @b catch and which terminate JVM
 * execution:
 *
 * <ul>
 * <li>
 * @link #JVMCLASS_JAVA_LANG_LINKAGEERROR LinkageError@endlink Loading,
 *                    linking, or initialization error
 *                    (2.17.2, 2.17.3, 2.17.4).  May also be
 *                    thrown at run time.
 * </li>
 *
 * <li>
 * Loading Errors (2.17.2):
 * <ul>
 *     <li>
 *     @b ClassFormatError        Binary data is not a valid class file.
 *     </li><li>
 *     @b ClassCircularityError   Class hierarchy eventually references
 *                                itself.
 *     </li><li>
 *     @b NoClassDefFoundError    Class cannot be found by loader.
 *     </li><li>
 *     @b UnsupportedClassVersionError JVM does not support this version
 *                                of the class file specification.
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Linking Errors (2.17.3) (subclass of @link
   #JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR 
   IncompatibleClassChangeError@endlink):
 * <ul>
 *     <li>
 *     @b NosuchFieldError      Attempt to reference non-existent field
 *     </li><li>
 *     @b NoSuchMethodError     Attempt to reference non-existent method
 *     </li><li>
 *     @b InstantiationError    Attempt to instantiate abstract class
 *     </li><li>
 *     @b IllegalAccessError    Attempt to reference a field or method
 *                              that is not in scope (typically not
 *                              not @c @b public ).
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Verification Errors (2.17.3):
 * <ul>
 *     <li>
 *     @b VerifyError    Class fails integrity checks of the verifier.
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Initialization Errors (2.17.4):
 * <ul>
 *     <li>
 *     @b ExceptionInInitializerError   A static initializer of a static
 *                                   field initializer threw something
 *                                   that was neither a
 *                                   @c @b java.lang.Error
 *                                   or its subclass.
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Run-time instances of @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
   LinkageError@endlink:
 * <ul>
 *     <li>
 *     @b AbstractMethodError    Invocation of an abstract method.
 *     </li><li>
 *     @b UnsatisfiedLinkError   Cannot load native method (shared obj).
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Resource limitations, via subclass of @link
   #JVMCLASS_JAVA_LANG_VIRTUALMACHINEERROR VirtualMachineError@endlink:
 * <ul>
 *     <li>
 *     @b InternalError         JVM software or host software/hardware
 *                              (may occur asynchronously, any time)
 *     </li><li>
 *     @b OutOfMemoryError      JVM cannot get enough memory for
 *                              request, even after GC/mmgmt.
 *     </li><li>
 *     @b StackOverflowError    Out of JVM stack space, typically due
 *                              to infinite recursion on a method.
 *     </li><li>
 *     @b UnknownError          JVM cannot determine the actual cause.
 *     </li>
 * </ul>
 * </li>
 * </ul>
 *
 *
 * @todo The code fragment macros used by the opcode switch in
 * @link #opcode_run() opcode_run()@endlink need to have the
 * local variables documented as to which as required upon
 * macro startup and which are set for use at macro completion.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/opcode.c $ \$Id: opcode.c 0 09/28/2005 dlydick $
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
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_COPYRIGHT_APACHE(opcode, c, "$URL: https://svn.apache.org/path/name/opcode.c $ $Id: opcode.c 0 09/28/2005 dlydick $");

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h"
#include "gc.h" 
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "method.h"
#include "native.h"
#include "opcode.h"
#include "utf.h"
#include "util.h"

/*!
 * @name Macro support for inner loop opcodes.
 *
 * @brief Common operations that are used in numerous opcodes
 * are gathered here so as to improve accuracy of implementation
 * and simplify the code.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Retrieve a two-byte operand that the PC points to.
 *
 *
 * Store thw two-byte operand into the requested @link #u2 u2@endlink
 * variable, then increment the program counter to the next byte code
 * following it.
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that will
 *               receive the two bytes of operand from with the
 *               instruction.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
#define GET_U2_OPERAND(u2var)                  \
    u2var = GETRS2((u2 *) &pcode[pc->offset]); \
    pc->offset += sizeof(u2)
 

/*!
 * @name Validate a constant_pool entry
 *
 */


/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Check that a constant_pool entry contains
 * a specific of tag for this operation.
 *
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that
 *               contains a constant_pool entry to be examined.
 *
 * @param cptag1 First constant_pool tag that is valid for this
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
 * @brief Check that a constant_pool entry contains
 * the right kind of tag for this operation, from a choice of two.
 *
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that
 *               contains a constant_pool entry to be examined.
 *
 * @param cptag1 First constant_pool tag that is valid for this
 *               operation.
 *
 * @param cptag2 Second constant_pool tag that is valid for this
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
 * @brief Check that a constant_pool entry contains
 * the right kind of tag for this operation, from a choice of three.
 *
 *
 * @param u2var  Name of a @link #u2 u2@endlink variable that
 *               contains a constant_pool entry to be examined.
 *
 * @param cptag1 First constant_pool tag that is valid for this
 *               operation.
 *
 * @param cptag2 Second constant_pool tag that is valid for this
 *               operation.
 *
 * @param cptag3 Third constant_pool tag that is valid for this
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
 *          the target type work correctly.
 *
 * @warning Since this macro takes the address of its source parameter,
 * it will only work for variables, not for expressions!
 *
 *
 * @param var_sizeofjint  Any 32-bit variable.  If it is a smaller
 *                        type, such as (jboolean), perform a
 *                        widening conversion into (jint) first.
 *
 * @returns (jint) version of @b var_sizeofjint without conversion
 *          of contents (such as jfloat-to-jint might want to do).
 *
 *
 * @todo A careful review of this macro across different compilers
 *       is very much in order.
 *
 */
#define FORCE_JINT(var_sizeofjint) \
    (*((jint *) ((jvoid *) &var_sizeofjint)))


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
 * @returns (jfloat) version of @b var_sizeofjint without conversion
 *          of contents (such as jint-to-jfloat might want to do).
 *
 *
 * @todo A careful review of this macro across different compilers
 *       is very much in order.
 *
 */
#define FORCE_JFLOAT(var_sizeofjint) \
    (*((jfloat *) ((jvoid *) &var_sizeofjint)))


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
 * @returns @link #rvoid rvoid@endlink.
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
 * @param clsnameidx  constant_pool index into class file of current
 *                    class (as indicated in the program counter)
 *                    that is a class reference entry.
 *
 *
 * @returns @link #rvoid rvoid@endlink.
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
 * @param clsnameidx  CONSTANT_Utf8_info constant_pool index
 *                    to class name
 *
 * @return @link #rvoid rvoid@endlink
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
 * @param Methodref  constant_pool index into class file of current
 *                   class (as indicated in the program counter) that
 *                   is a method reference entry.
 *
 *
 * @returns @link #rvoid rvoid@endlink.
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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


#if 0
/*!
 * @brief Check if this method is an @c @b abstract method,
 * that is, not having a concrete implementation.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @return @link #rvoid rvoid@endlink
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
#endif


/*!
 * @brief Check if this method is a concrete method, that is,
 * not @c @b abstract .
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @param Fieldref  constant_pool index into class file of current
 *                  class (as indicated in the program counter) that
 *                  is a method reference entry.
 *
 *
 * @returns @link #rvoid rvoid@endlink.
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @return @link #rvoid rvoid@endlink
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
 * @returns @link #rtrue rtrue@endlink if this field takes two
 * accesses, otherwise @link #rfalse rfalse@endlink for smaller types.
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
 * @brief Store out value by data type into either class static field
 * or object instance field.
 *
 *
 * @param data_array  Expression pointing to the class' or object's
 *                    @b XXX_data[] array, namely a (jvalue *).
 *                    Typically a fixed set of two expressions.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @see PUTFIELD
 *
 * @see PUTSTATIC
 *
 *
 * @todo The various type casting games of integer/sub-integer
 *       and integer/float/double and integer/objhash need to be
 *       carefully scrutinized for correctness at run time.
 *
 * @todo Is BASTYPE_CHAR_ARRAY a legal case for @b PUTSTATIC and
 *       @b PUTFIELD ?
 *
 */
#define PUTDATA(data_array)                                            \
    switch (pcpd_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM)       \
    {                                                                  \
        case BASETYPE_CHAR_B:                                          \
            POP(thridx,                                                \
                data_array._jbyte,                                     \
                jbyte);                                                \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_C:                                          \
            POP(thridx,                                                \
                data_array._jchar,                                     \
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
            data_array._jdouble = bytegames_combine_jdouble(jitmp1,    \
                                                            jitmp2);   \
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
            data_array._jfloat = FORCE_JFLOAT(jitmp1);                 \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_I:                                          \
            POP(thridx,                                                \
                data_array._jint,                                      \
                /* redundant: */ jint);                                \
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
            jltmp = bytegames_combine_jlong(jitmp1, jitmp2);           \
            data_array._jlong = jltmp;                                 \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_L:                                          \
            POP(thridx,                                                \
                data_array._jobjhash,                                  \
                jvm_object_hash);                                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_S:                                          \
            POP(thridx,                                                \
                data_array._jshort,                                    \
                jshort);                                               \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_Z:                                          \
            POP(thridx,                                                \
                data_array._jboolean,                                  \
                jboolean);                                             \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_ARRAY:                                      \
            POP(thridx,                                                \
                data_array._jarray,                                    \
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
 * @param data_array  Expression pointing to the class' or object's
 *                    @b XXX_data[] array, namely a (jvalue *).
 *                    Typically a fixed set of two expressions.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @see GETFIELD
 *
 * @see GETSTATIC
 *
 *
 * @todo The various type casting games of integer/sub-integer
 *       and integer/float/double and integer/objhash need to be
 *       carefully scrutinized for correctness at run time.
 *
 * @todo Is BASTYPE_CHAR_ARRAY a legal case for @b GETSTATIC and
 *       @b GETFIELD ?
 *
 */
#define GETDATA(data_array)                                            \
    switch (pcpd_Fieldref->LOCAL_Fieldref_binding.jvaluetypeJVM)       \
    {                                                                  \
        case BASETYPE_CHAR_B:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array._jbyte);                            \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_C:                                          \
            PUSH(thridx,                                               \
                (jint) data_array._jchar);                             \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_D:                                          \
            bytegames_split_jdouble(data_array._jdouble,               \
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
            jitmp1 = FORCE_JINT(data_array._jfloat);                   \
            PUSH(thridx, jitmp1);                                      \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_I:                                          \
            PUSH(thridx,                                               \
                 (jint) /* ... redundant */ data_array._jint);         \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_J:                                          \
            bytegames_split_jlong(data_array._jlong,                   \
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
                 (jint) data_array._jobjhash);                         \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_S:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array._jshort);                           \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_Z:                                          \
            PUSH(thridx,                                               \
                 (jint) data_array._jboolean);                         \
            break;                                                     \
                                                                       \
        case BASETYPE_CHAR_ARRAY:                                      \
            PUSH(thridx,                                               \
                 (jint) data_array._jarray);                           \
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
 * Handler linkage for end of thread detection.
 */
static jmp_buf opcode_end_thread_return;


/*!
 * @brief JVM inner loop setup for end of thread detection-- implements
 * @c @b setjmp(3) .
 *
 *
 * Use this function to arm handler for non-local exit from the
 * inner @c @b while() loop when a thread has finished
 * running.
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns From normal setup, integer
 *          @link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink.
 *          Otherwise, return
 *          @link #exit_code_enum exit_enum code enumeration@endlink
 *          from @link #opcode_end_thread_test()
            opcode_end_thread_test()@endlink.
 *
 */

static int opcode_end_thread_setup(rvoid)
{
    /*
     * Return point from @c @b longjmp(3) as declared
     * in @link #opcode_end_thread_test()
       opcode_end_thread_test()@endlink
     */
    return(setjmp(opcode_end_thread_return));

} /* END of opcode_end_thread_setup() */


/*!
 * @brief Detect end of thread when stack frame is empty at
 * @b return time and perform non-local return-- implements
 * @c @b longjmp(3) .
 *
 * Use this function to test for end of thread execution and to
 * perform a non-local return from the inner @c @b while()
 * loop when a thread has finished running.
 *
 * This function is invoked from within each of the Java
 * @b return family of opcodes to detect if the stack frame is
 * empty after a @link #POP_FRAME() POP_FRAME()@endlink.  If this
 * test passes, then the stack frame is empty and the thread has
 * nothing else to do.  It is therefore moved into the @b COMPLETE
 * state and a non-local return exits the while loop.
 *
 *
 * @param thridx  Thread index of thread to evaluate.
 *
 *
 * @returns @link #rvoid rvoid@endlink if end of thread test fails.
 *          If test passes, perform non-local state restoration from
 *          setup via @c @b setjmp(3) as stored in @link
            #opcode_end_thread_return opcode_end_thread_return@endlink.
 *
 */

static rvoid opcode_end_thread_test(jvm_thread_index thridx)
{
    /* Check if thread has finished running */

    /*!
     * Check both current end of program FP and final FP in case
     * something like a @c @b \<clinit\> or @c @b \<init\> was loaded
     * on top of a running program.
     *
     * @todo Should FP condition be fp_end_program <= THREAD().fp
     *       instead of < condition? 
     *
     */
    if (THREAD(thridx).fp_end_program < THREAD(thridx).fp)
    {
        /* Thread is still running something, so continue */
        return;
    }

    /* Thread has finished running, so return to @c @b setjmp(3) */

    int rc = (int) EXIT_JVM_THREAD;
    longjmp(opcode_end_thread_return, rc);
/*NOTREACHED*/

} /* END of opcode_end_thread_test() */


/*!
 * @brief Double-fault error state variable for throwable event.
 *
 * This boolean reports the the error-within-an-error state condition
 * within
 * @link #rvoid opcode_load_run_throwable()
   opcode_load_run_throwable()@endlink
 */
rboolean opcode_calling_java_lang_linkageerror =
    CHEAT_AND_USE_FALSE_TO_INITIALIZE;


/*!
 * @brief Load a @c @b java.lang.Throwable event, typically
 * an @b Error or @b Exception and run its @c @b \<clinit\> method
 * followed by its @c @b \<init\> method with default parameters.
 *
 * This function must @e not be called until
 * @c @b java.lang.Object , @c @b java.lang.Class,
 * @c @b java.lang.String , @c @b java.lang.Throwable
 * @c @b java.lang.Error have been loaded and initialized.
 *
 * There is @e no attempt to enforce which classes may be invoked
 * by this handler.  It is assumed that the caller will @e only
 * pass in subclasses of
 * @link #JVMCLASS_JAVA_LANG_ERROR JVMCLASS_JAVA_LANG_ERROR@endlink.
 * Anything else will produce undefined results.
 *
 * @warning <b>This handler is not a simple as it seems!</b>  You
 * absolutely @e must know what the non-local return mechanism
 * @c @b setjmp(3)/longjmp(3) is before attempting to figure it out!!!
 *
 * The strategy is a simple one:  Trap thrown errors by this handler
 * and trap a failure in that error class by throwing a @link
   #JVMCLASS_JAVA_LANG_INTERNALERROR
   JVMCLASS_JAVA_LANG_INTERNALERROR@endlink.  If that fails, give up.
 *
 * @b Details:  When this function is first called due to a thrown
 * error, it attempts to load and run that error class.  If all
 * is well, that class runs and everyone lives happily ever after.
 * If that class throws an error, however, this handler, having
 * been re-armed, is activated semi-recursively via @link
   #exit_throw_exception() exit_throw_exception()@endlink,
 * (that is, not entering at the top of function, but at the return
 * from @link #exit_throw_exception() exit_throw_exception()@endlink
 * with a non-zero return value), entering @e this code at
 * @link #exit_exception_setup exit_exception_setup()@endlink,
 * choosing the conditional branch != @link #EXIT_MAIN_OKAY
   EXIT_MAIN_OKAY@endlink and attempts to recursively load and
 * run @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
   JVMCLASS_JAVA_LANG_LINKAGEERROR@endlink.  If this is successful,
 * fine, call @link #exit_jvm() exit_jvm()@endlink and be done.
 * However, if even @e this fails and throws an error, the handler,
 * having been rearmed @e again by the attempt to invoke
 * @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
   JVMCLASS_JAVA_LANG_LINKAGEERROR@endlink, it again semi-recursively
 * is activated via @link #exit_throw_exception()
   exit_throw_exception()@endlink and again enters the code at
 * @link #exit_exception_setup() exit_exception_setup()@endlink.  This
 * time, the global
 * @link #opcode_calling_java_lang_linkageerror
   opcode_calling_java_lang_linkageerror@endlink is
 * @link #rtrue rtrue@endlink, so no more recursive invocations
 * are performed.  Instead, @link #exit_jvm() exit_jvm()@endlink
 * with the most recent @link #EXIT_MAIN_OKAY EXIT_xxx@endlink code
 * from @link #exit_throw_exception() exit_throw_exception()@endlink
 * and be done.
 *
 *
 * @param  pThrowableEvent  Null-terminated string name of
 *                          throwable class.
 *
 * @param  thridx           Thread table index of thread to load this
 *                          @c @b java.lang.Throwable sub-class
 *                          into.
 *
 *
 * @returns  @link #rvoid rvoid@endlink.  Either the
 *           @c @b java.lang.Throwable class loads and
 *           runs, or it loads @c @b java.lang.LinkageError
 *           and runs it, then returns to caller, or it exits
 *           due to an error somewhere in this sequence of
 *           events.
 *
 */

rvoid opcode_load_run_throwable(rchar            *pThrowableEvent,
                                jvm_thread_index  thridx)
{
    /******* Re-arm java.lang.LinkageError handler ***/


    /*!
     * @internal This call to exit_exception_setup() and the following
     * if (@link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink) statement
     * constitute the ugly part of this code as described above.
     * See also other recursive calls and their control via
     * @link #opcode_calling_java_lang_linkageerror
       opcode_calling_java_lang_linkageerror@endlink:
     *
     */
    int nonlocal_rc = exit_exception_setup();

    if (EXIT_MAIN_OKAY != nonlocal_rc)
    {
        /*!
         * @todo  Make this load and run the error class
         *        @c @b \<clinit\> and default @c @b \<init\> method
         *        instead of/in addition to fprintf().  Other
         *        exit_throw_exception() handlers will have invoked
         *        this method, so it @e must be rearmed @e again at
         *        this point, lest an error that invokes it causes
         *        an infinite loop.
         */

        /* Should never be true via exit_throw_exception() */
        if (rnull == exit_LinkageError_subclass)
        {
            exit_LinkageError_subclass = "unknown";
        }

        fprintfLocalStderr(
            "opcode_load_run_throwable:  Recursive Error %d (%s): %s\n",
            nonlocal_rc,
            exit_get_name(nonlocal_rc),
            exit_LinkageError_subclass);

        jvmutil_print_stack(thridx, (rchar *) rnull);

        /*
         * WARNING!!! Recursive call, but will only go 1 level deep.
         */
        if (rfalse == opcode_calling_java_lang_linkageerror)
        {
            opcode_calling_java_lang_linkageerror = rtrue;

            opcode_load_run_throwable(JVMCLASS_JAVA_LANG_LINKAGEERROR,
                                      rtrue);

            opcode_calling_java_lang_linkageerror = rfalse;
        }

        exit_jvm(nonlocal_rc);
/*NOTREACHED*/
    }

    if (jvm_thread_index_null == thridx)
    {
        sysErrMsg("opcode_load_run_throwable",
             "Invalid thread index %d for throwable class %s",
                thridx, pThrowableEvent);
        return;
    }

    /*!
     * @internal Load error class and run its @c @b \<clinit\> method.
     *
     * If an error is thrown by class_load_resolve_clinit(),
     * re-enter this error function recursively at
     * exit_exception_setup().
     */
    jvm_class_index clsidx =
        class_load_resolve_clinit(pThrowableEvent,
                                  thridx,
                                  rfalse, /* N/A due to valid thridx */
                                  rfalse);

    /*!
     * @internal Both mark (here) and unmark (below) class
     * so it gets garbage collected.
     */
    (rvoid) GC_CLASS_MKREF_FROM_CLASS(jvm_class_index_null, clsidx);


    /*!
     * @internal Instantiate error class object and run its default
     * @c @b \<init\> method with default parameters.
     *
     * If an error is thrown in objec_instance_new(), re-enter this
     * error function recursively at exit_exception_setup().
     */
    jvm_object_hash objhash=
        object_instance_new(THREAD_STATUS_EMPTY,
                            CLASS_OBJECT_LINKAGE(clsidx)->pcfs,
                            clsidx,
                            LOCAL_CONSTANT_NO_ARRAY_DIMS,
                            (jint *) rnull,
                            rtrue,
                            thridx);

    /*!
     * @internal Both mark and unmark object so it
     * gets garbage collected
     */
    (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(jvm_object_hash_null, objhash);
    (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(jvm_object_hash_null, objhash);

    /*!
     * @internal Unmarked from above-- since JVM is going down,
     * this may be irrelevant, but be consistent.
     */
    (rvoid) GC_CLASS_RMREF_FROM_CLASS(jvm_class_index_null, clsidx);

    return;

} /* END of opcode_load_run_throwable() */


/*!
 * @brief Inner loop of JVM virtual instruction execution engine.
 *
 * Only run the inner loop until:
 *
 * <ul>
 * <li> thread state changes </li>
 * <li> time slice expired </li>
 * <li> thread completes (when FP is not 0, that is,
 *      not @link #JVMCFG_NULL_SP JVMCFG_NULL_SP@endlink) </li>
 * </ul>
 *
 * Any remaining time on this time slice will go against
 * the next thread, which may only have a small amount
 * of time, even none at all.  This is a natural effect
 * of any time-slicing algorithm.
 *
 * Logic similar to the uncaught exception handler of this function
 * may be found in object_run_method() as far as initiating execution
 * of a JVM method.
 *
 * @todo  See if there is a better time-slicing algorithm that
 *        is just as easy to use and keeps good real clock time.
 * 
 * @todo:  having @c @b run_init_ (parm 7) for invocations of
 *         opject_instance_new() to be @link #rfalse rfalse@endlink
 *         the right thing to do for array initialization, namely
 *         opcodes @b NEWARRAY and @b ANEWARRAY ?  Initializing an
 *         array is really not a constructor type
 *         of operation, but the individual components
 *         (elements) of the array probably would be,
 *         and with default parameters.
 *
 *
 *
 * @param  thridx            Thread index of thread to run
 *
 * @param  check_timeslice   @link #rtrue rtrue@endlink if JVM time
 *                           slice preempts execution after maximum
 *                           time exceeded.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this method and/or time slice
 *          ran correctly (whether or not the thread finished running),
 *          or @link #rfalse rfalse@endlink if an
 *          uncaught exception was thrown or if an
 *          Error, Exception, or Throwable was thrown, or if a
 *          thread state could not be properly changed.
 *
 */
rboolean opcode_run(jvm_thread_index thridx,
                    rboolean check_timeslice)
{
    /*
     * Arm handler for the three conditions
     * java.lang.Error, Exception, and Throwable.
     *
     * Any JVM virtual execution that throws one of
     * these that is @e not covered by an exception
     * handler in the class will issue:
     *
     *     <b><code>
           longjmp(THREAD(thridx).nonlocal_ThrowableEvent, rc)
           </b></code>
     *
     * by way of @link #thread_throw_exception() 
       thread_throw_exception@endlink, which will return to the
     * @c @b else branch of this @c @b if .  It will
     * contain a @link rthread#status (rthread.status@endlink bit
     * @b THREAD_STATUS_xxx which may be examined there.  Notice that
     * @c @b int is wider than @c @b rushort and thus
     * will not lose any information in the implicit conversion.
     */

    /* Inner loop end of thread detection, init to unused value */
    int nonlocal_thread_return = EXIT_JVM_INTERNAL;

    /* Calls @c @b setjmp(3) to arm handler */
    int nonlocal_rc = thread_exception_setup(thridx);

    /* Show error case first due to @e long switch() following */
    if (THREAD_STATUS_EMPTY != nonlocal_rc)
    {
        /*
         * Examine only the @c @b longjmp(3) conditions (should be
         * irrelevant due to filter in @link #thread_throw_exception()
           thread_throw_exception()@endlink
         */
        nonlocal_rc &= (THREAD_STATUS_THREW_EXCEPTION |
                        THREAD_STATUS_THREW_ERROR |
                        THREAD_STATUS_THREW_THROWABLE |
                        THREAD_STATUS_THREW_UNCAUGHT);

        /*
         * Local copy of @link rthread#pThrowable pThrowable@endlink
         * for use below
         */
        rchar *pThrowableEvent = THREAD(thridx).pThrowableEvent;

        /*
         * Clear out the Throwable condition now that it
         * is being processed, in case of multiple exceptions.
         */
        THREAD(thridx).pThrowableEvent = (rchar *) rnull;
        THREAD(thridx).status &= ~(THREAD_STATUS_THREW_EXCEPTION |
                                   THREAD_STATUS_THREW_ERROR |
                                   THREAD_STATUS_THREW_THROWABLE |
                                   THREAD_STATUS_THREW_UNCAUGHT);

        /*
         * Process the specifics for each java.lang.Throwable
         * condition before doing the generic processing.
         */
        if (THREAD_STATUS_THREW_EXCEPTION & nonlocal_rc)
        {
            /*! @todo  What needs to go here? */
        }
        else
        if (THREAD_STATUS_THREW_ERROR & nonlocal_rc)
        {
            /*! @todo  What needs to go here? */
        }
        else
        if (THREAD_STATUS_THREW_THROWABLE & nonlocal_rc)
        {
            /*! @todo  What needs to go here? */
        }
        else
        if (THREAD_STATUS_THREW_UNCAUGHT & nonlocal_rc)
        {
            /*
             * Handle an uncaught exception.  @b pThrowableEvent will
             * be @link #rnull rnull@endlink here, so there is nothing
             * to get from it.
             */

            jvm_class_index clsidx = class_find_by_prchar(
                                        JVMCLASS_JAVA_LANG_THREADGROUP);

            if (jvm_class_index_null == clsidx)
            {
                /* Problem creating error class, so quit */
                sysErrMsg("opcode_run",
                     "Cannot find class %s",
                     JVMCLASS_JAVA_LANG_THREADGROUP);

                jvmutil_print_stack(thridx, (rchar *) rnull);

                exit_jvm(EXIT_JVM_CLASS);
/*NOTREACHED*/
            }

            /*!
             * @todo Get @c @b ThreadGroup logic working that
             * figures out which @c @b java.lang.ThreadGroup
             * this thread is a part of and invoke
             * @c @b java.lang.ThreadGroup.uncaughtException()
             * for that specific object instead of this general method.
             * Probably the class library will gripe about not knowing
             * which object to associate with the method call since
             * @c @b java.lang.ThreadGroup.uncaughtException()
             * is @e not a @c @b static method.
             */

            /*
             * Set FP lower boundary so Java @c @b return
             * instruction does not keep going after handler, check if
             * @c @b java.lang.ThreadGroup.uncaughtException()
             * is there, and run it.
             */
            jvm_sp fp_save_end_program = THREAD(thridx).fp_end_program;

            /*
             * Make JVM stop once
             * @c @b java.lang.ThreadGroup.uncaughtException()
             * is done
             */
            THREAD(thridx).fp_end_program = THREAD(thridx).fp;

            /* Continue getting pieces for PUT_PC_IMMEDIATE() */
            jvm_method_index mthidx =
                method_find_by_prchar(clsidx,
                                      JVMCFG_UNCAUGHT_EXCEPTION_METHOD,
                                      JVMCFG_UNCAUGHT_EXCEPTION_PARMS);

            if (jvm_method_index_bad == mthidx)
            {
                exit_throw_exception(EXIT_JVM_METHOD,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
            }

            /*
             * Load up entry point for Throwable call
             */
            ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

            jvm_attribute_index codeatridx =
                pcfs->methods[mthidx]
                        ->LOCAL_method_binding.codeatridxJVM;

            if (jvm_attribute_index_bad == codeatridx)
            {
                exit_throw_exception(EXIT_JVM_ATTRIBUTE,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
            }

            if (jvm_attribute_index_native == codeatridx)
            {
                /* Pass parameters for both local method and JNI call */
                native_run_method(thridx,
                                  pcfs
                                    ->methods[mthidx]
                                      ->LOCAL_method_binding.nmordJVM,
                                  clsidx,
                                  pcfs->methods[mthidx]->name_index,
                                  pcfs->methods[mthidx]
                                    ->descriptor_index);
            }
            else
            {
                Code_attribute *pca = (Code_attribute *)
                         &pcfs->methods[mthidx]->attributes[codeatridx];
                PUSH_FRAME(thridx, pca->max_locals);
                PUT_PC_IMMEDIATE(thridx,
                                 clsidx,
                                 mthidx,
                                 pcfs
                                   ->methods[mthidx]
                                   ->LOCAL_method_binding.codeatridxJVM,
                                 pcfs
                                   ->methods[mthidx]
                                   ->LOCAL_method_binding.excpatridxJVM,
                                 CODE_CONSTRAINT_START_PC);

                if (rfalse == opcode_run(thridx, rfalse))
                {
                    /* Problem running error class, so quit */
                    sysErrMsg("opcode_run",
                              "Cannot run exception method %s %s%s",
                              JVMCLASS_JAVA_LANG_THREADGROUP,
                              JVMCFG_UNCAUGHT_EXCEPTION_METHOD,
                              JVMCFG_UNCAUGHT_EXCEPTION_PARMS);

                    jvmutil_print_stack(thridx, (rchar *) rnull);

                    exit_jvm(EXIT_JVM_THREAD);
/*NOTREACHED*/
                }
            }

            /*
             * This would normally permit java.lang.Exception 
             * and java.lang.Throwable to continue by restoring
             * lower FP boundary, but unwind here anyway for
             * proper frame contents for later diagnostics.
             */
            THREAD(thridx).fp_end_program = fp_save_end_program;

            /* Attempt to shut down thread due to condition */
            if (rfalse == threadstate_request_complete(thridx))
            {
                sysErrMsg("opcode_run",
                 "Unable to move completed thread %d to '%s' state",
                          thridx,
                          thread_state_get_name(THREAD_STATE_COMPLETE));
                THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

                return(rfalse);
            }

            return(rfalse);

        } /* if THREAD_STATUS_THREW_UNCAUGHT */
        else
        {
            /*! @todo  What needs to go here, if anything? */
        }

        /* Completely handled THREAD_STATUS_THREW_UNCAUGHT above */
        if (nonlocal_rc & (THREAD_STATUS_THREW_EXCEPTION |
                           THREAD_STATUS_THREW_ERROR |
                           THREAD_STATUS_THREW_THROWABLE))
        {
            /*
             * Utilizing the current contents of the @c @b longjmp(3)
             * condition found in @b pThrowableEvent, which will
             * have been set when one of the status bits was set--
             * see @link jvm/src/threadutil.c threadutil.c@endlink
             * for several examples.
             *
             * When loading the error class, process its
             * @c @b \<clinit\> on any available thread, but process
             * its @C @b \<init\> on @e this thread so thread will be
             * done running after it has been processed (due to FP
             * change).
             */
            opcode_load_run_throwable(pThrowableEvent, thridx);

            /*
             * All conditions except java.lang.Exception should kill
             * the thread.
             *
             */
            if (!(THREAD_STATUS_THREW_EXCEPTION & nonlocal_rc))
            {
                /* Attempt to shut down thread due to code completion */
                if (rfalse == threadstate_request_complete(thridx))
                {
                    sysErrMsg("opcode_run",
                     "Unable to move aborted thread %d to '%s' state",
                              thridx,
                              thread_state_get_name(
                                                THREAD_STATE_COMPLETE));
                    THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

                    return(rfalse);
                }
            }

        } /* if nonlocal_rc */

    } /* if thread_exception_setup() */
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /* BEGIN GIANT SWITCH STATEMENT if(){}else{while(){switch()}} **/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    else
    {
        /*
         * Run inner JVM execution loop.
         */

        /*
         * Scratch area for operating the inner loop
         * and its key associations
         */
        jvm_pc             *pc    = THIS_PC(thridx);
        ClassFile          *pcfs  = THIS_PCFS(thridx);
        jvm_virtual_opcode *pcode = DEREFERENCE_PC_CODE_BASE(thridx);

        /* Scratch area for operand resolution */
        rboolean           iswide;
        iswide = rfalse;

        jvm_virtual_opcode opcode;
        u1                 op1u1idx; /* Operand 1 as a (u1) CP index */
        u2                 op1u2idx; /* Operand 1 as a (u2) CP index */
        u4                 op1u4off; /* Operand 1 as a (u4) offset */

        jint               jitmp1;  /* Opcode (jint) scratch area 1 */
        jint               jitmp2;  /* Opcode (jint) scratch area 2 */
        jlong              jltmp;   /* Opcode (jlong) scratch area */
        jfloat             jftmp;   /* Opcode (jfloat) scratch area */
        jdouble            jdtmp;   /* Opcode (jdouble) scratch area */


        /* Scratch area for Fieldref and Methodref navigation */
        cp_info_dup               *pcpd;
        CONSTANT_Class_info       *pcpd_Class;
        CONSTANT_Fieldref_info    *pcpd_Fieldref;
        CONSTANT_Methodref_info   *pcpd_Methodref;
        CONSTANT_NameAndType_info *pcpd_NameAndType;
        CONSTANT_Utf8_info        *pcpd_Utf8;

        field_info              *pfld;
        method_info             *pmth;

        ClassFile              *pcfsmisc;
        u2                      cpidxmisc;
        jvm_class_index         clsidxmisc;
        jvm_method_index        mthidxmisc;
        jvm_object_hash         objhashmisc;
        jvm_field_lookup_index  fluidxmisc;
        rchar                   *prchar_clsname;
        rushort                 special_obj_misc;


        /* Calls @c @b setjmp(3) to arm handler */
        nonlocal_thread_return = opcode_end_thread_setup();

        /* Show error case first due to @e long switch() following */
        if (EXIT_MAIN_OKAY != nonlocal_thread_return)
        {
            ; /* Nothing to do since this is not an error. */
        }
        else
        {

            /*!
             * @internal For best runtime efficiency, place tests in
             * order of most to least frequent occurrence.
             */

            while ( /* This thread is in the RUNNING state */
                   (THREAD_STATE_RUNNING == THREAD(thridx).this_state)&&

                   /* Time slice is still running or is N/A */
                   ((rfalse == check_timeslice) || /* or if true and */
                    (rfalse == pjvm->timeslice_expired)))
            {
                /* Retrieve next virtual opcode */
                opcode = pcode[pc->offset++];

/*
 * Due to the significant complexity of this @c @b switch
 * statement, the indentation is being reset to permit wider lines
 * of code with out breaking up expressions with the intention of
 * creating better readability of the code.
 */

static void dummy1(void) { char *p, *dummy2; dummy2 = p; dummy1(); }
#define STUB { dummy1(); }

switch(opcode)
{
case OPCODE_00_NOP:         /* Do nothing */
    break;

case OPCODE_01_ACONST_NULL: /* Push NULL onto stack */
    /*! @todo Test this opcode */
    STUB;
    PUSH(thridx, FORCE_JINT(jvm_object_hash_null));
    break;

case OPCODE_02_ICONST_M1:   /* Push constant -1, 0, 1, 2, 3, 4, 5 */
case OPCODE_03_ICONST_0:
case OPCODE_04_ICONST_1:
case OPCODE_05_ICONST_2:
case OPCODE_06_ICONST_3:
case OPCODE_07_ICONST_4:
case OPCODE_08_ICONST_5:
    PUSH(thridx, (((jint) opcode) - ((jint) OPCODE_03_ICONST_0)));
    break;

case OPCODE_09_LCONST_0:
case OPCODE_0A_LCONST_1:
    /*! @todo Test this opcode */
    STUB;
    jltmp = (((jlong) opcode) - ((jlong) OPCODE_09_LCONST_0));

    bytegames_split_jlong(jltmp, &jitmp1, &jitmp2);

    PUSH(thridx, jitmp1); /* ms word */
    PUSH(thridx, jitmp2); /* ls word */
    break;

case OPCODE_0B_FCONST_0:
case OPCODE_0C_FCONST_1:
case OPCODE_0D_FCONST_2:
    /*! @todo Test this opcode */
    STUB;
    jftmp = (jfloat) (((jint) opcode) - ((jint) OPCODE_0B_FCONST_0));

    PUSH(thridx, FORCE_JINT(jftmp));
    break;

case OPCODE_0E_DCONST_0:
case OPCODE_0F_DCONST_1:
    /*! @todo Test this opcode */
    STUB;
    jdtmp = (jdouble) (((jint) opcode) - ((jint) OPCODE_0E_DCONST_0));

    bytegames_split_jdouble(jltmp, &jitmp1, &jitmp2);

    PUSH(thridx, jitmp1); /* ms word */
    PUSH(thridx, jitmp2); /* ls word */

    break;

case OPCODE_10_BIPUSH:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_11_SIPUSH:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_12_LDC:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_13_LDC_W:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_14_LDC2_W:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_15_ILOAD:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_16_LLOAD:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_17_FLOAD:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_18_DLOAD:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_19_ALOAD:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_1A_ILOAD_0:
case OPCODE_1B_ILOAD_1:
case OPCODE_1C_ILOAD_2:
case OPCODE_1D_ILOAD_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_1E_LLOAD_0:
case OPCODE_1F_LLOAD_1:
case OPCODE_20_LLOAD_2:
case OPCODE_21_LLOAD_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_22_FLOAD_0:
case OPCODE_23_FLOAD_1:
case OPCODE_24_FLOAD_2:
case OPCODE_25_FLOAD_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_26_DLOAD_0:
case OPCODE_27_DLOAD_1:
case OPCODE_28_DLOAD_2:
case OPCODE_29_DLOAD_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_2A_ALOAD_0:
case OPCODE_2B_ALOAD_1:
case OPCODE_2C_ALOAD_2:
case OPCODE_2D_ALOAD_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_2E_IALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_2F_LALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_30_FALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_31_DALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_32_AALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_33_BALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_34_CALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_35_SALOAD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_36_ISTORE:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_37_LSTORE:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_38_FSTORE:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_39_DSTORE:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_3A_ASTORE:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;
    break;

case OPCODE_3B_ISTORE_0:
case OPCODE_3C_ISTORE_1:
case OPCODE_3D_ISTORE_2:
case OPCODE_3E_ISTORE_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_3F_LSTORE_0:
case OPCODE_40_LSTORE_1:
case OPCODE_41_LSTORE_2:
case OPCODE_42_LSTORE_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_43_FSTORE_0:
case OPCODE_44_FSTORE_1:
case OPCODE_45_FSTORE_2:
case OPCODE_46_FSTORE_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_47_DSTORE_0:
case OPCODE_48_DSTORE_1:
case OPCODE_49_DSTORE_2:
case OPCODE_4A_DSTORE_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_4B_ASTORE_0:
case OPCODE_4C_ASTORE_1:
case OPCODE_4D_ASTORE_2:
case OPCODE_4E_ASTORE_3:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_4F_IASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_50_LASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_51_FASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_52_DASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_53_AASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_54_BASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_55_CASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_56_SASTORE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_57_POP:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_58_POP2:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_59_DUP:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_5A_DUP_X1:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_5B_DUP_X2:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_5C_DUP2:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_5D_DUP2_X1:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_5E_DUP2_X2:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_5F_SWAP:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_60_IADD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_61_LADD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_62_FADD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_63_DADD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_64_ISUB:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_65_LSUB:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_66_FSUB:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_67_DSUB:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_68_IMUL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_69_LMUL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_6A_FMUL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_6B_DMUL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_6C_IDIV:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_6D_LDIV:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_6E_FDIV:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_6F_DDIV:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_70_IREM:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_71_LREM:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_72_FREM:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_73_DREM:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_74_INEG:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_75_LNEG:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_76_FNEG:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_77_DNEG:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_78_ISHL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_79_LSHL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_7A_ISHR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_7B_LSHR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_7C_IUSHR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_7D_LUSHR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_7E_IAND:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_7F_LAND:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_80_IOR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_81_LOR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_82_IXOR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_83_LXOR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_84_IINC:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_85_I2L:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_86_I2F:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_87_I2D:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_88_L2I:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_89_L2F:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_8A_L2D:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_8B_F2I:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_8C_F2L:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_8D_F2D:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_8E_D2I:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_8F_D2L:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_90_D2F:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_91_I2B:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_92_I2C:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_93_I2S:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_94_LCMP:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_95_FCMPL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_96_FCMPG:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_97_DCMPL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_98_DCMPG:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_99_IFEQ:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_9A_IFNE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_9B_IFLT:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_9C_IFGE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_9D_IFGT:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_9E_IFLE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_9F_IF_ICMPEQ:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A0_IF_ICMPNE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A1_IF_ICMPLT:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A2_IF_ICMPGE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A3_IF_ICMPGT:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A4_IF_ICMPLE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A5_IF_ACMPEQ:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A6_IF_ACMPNE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A7_GOTO:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A8_JSR:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_A9_RET:
    /*! @todo Write this opcode */
    STUB;
    iswide = rfalse;

    /*!
     * @todo  Is this test needed here,
     *         or only in @b xRETURN ?
     */
    opcode_end_thread_test(thridx);
    break;

case OPCODE_AA_TABLESWITCH:
    STUB;
    break;

case OPCODE_AB_LOOKUPSWITCH:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_AC_IRETURN:
    /*! @todo Write this opcode */
    STUB;
    opcode_end_thread_test(thridx);
    break;

    /*! @todo Write this opcode */
case OPCODE_AD_LRETURN:
    STUB;
    opcode_end_thread_test(thridx);
    break;
    /*! @todo Write this opcode */

case OPCODE_AE_FRETURN:
    STUB;
    opcode_end_thread_test(thridx);
    /*! @todo Write this opcode */
    break;

case OPCODE_AF_DRETURN:
    STUB;
    /*! @todo Write this opcode */
    opcode_end_thread_test(thridx);
    break;

case OPCODE_B0_ARETURN:
    /*! @todo Write this opcode */
    STUB;
    opcode_end_thread_test(thridx);
    break;

case OPCODE_B1_RETURN:

    CALCULATE_METHOD_INFO_FROM_PC;

    /*
     * If synchronized method, release MLOCK.
     */
    if (ACC_SYNCHRONIZED & pmth->access_flags)
    {
        (rvoid) objectutil_unsynchronize(
                    CLASS(clsidxmisc).class_objhash,
                    CURRENT_THREAD);
    }

    /*!
     * @todo  Implement test for same number of locks/unlocks
     *        per JVM spec section 8.13.
     */

    POP_FRAME(thridx);
/*
... Don't forget to set these to old method:
        pcfs  = THIS_PCFS(thridx);
        pcode = DEREFERENCE_PC_CODE_BASE(thridx);
*/
    opcode_end_thread_test(thridx);
    break;

case OPCODE_B2_GETSTATIC:

    /* Retrieve the constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2idx);

    /* Must reference a field */
    CHECK_CP_TAG(op1u2idx, CONSTANT_Fieldref);

    /* calc clsidxmisc and pcpd and pcpd_Fieldref */
    CALCULATE_FIELD_INFO_FROM_FIELD_REFERENCE(op1u2idx);

    /* Must be a valid reference to a method */
    CHECK_VALID_FIELDLOOKUPIDX(fluidxmisc);

    /* Must be a static field */
    CHECK_STATIC_FIELD;

    /* If it is a final field, it must be in the current class */
    CHECK_FINAL_FIELD_CURRENT_CLASS;

    /* Retrieve data from the static field now */
    GETSTATIC;

    break;

case OPCODE_B3_PUTSTATIC:

    /* Retrieve the constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2idx);

    /* Must reference a field */
    CHECK_CP_TAG(op1u2idx, CONSTANT_Fieldref);

    /* calc clsidxmisc and pcpd and pcpd_Fieldref */
    CALCULATE_FIELD_INFO_FROM_FIELD_REFERENCE(op1u2idx);

    /* Must be a valid reference to a method */
    CHECK_VALID_FIELDLOOKUPIDX(fluidxmisc);

    /* Must be a static field */
    CHECK_STATIC_FIELD;

    /* If it is a final field, it must be in the current class */
    CHECK_FINAL_FIELD_CURRENT_CLASS;

    /* Store data into the static field now */
    PUTSTATIC;

    break;

case OPCODE_B4_GETFIELD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_B5_PUTFIELD:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_B6_INVOKEVIRTUAL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_B7_INVOKESPECIAL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_B8_INVOKESTATIC:

    /* Retrieve the constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2idx);

    /* Must reference a method */
    CHECK_CP_TAG(op1u2idx, CONSTANT_Methodref);

    /* calc clsidxmisc and pcpd and pcpd_Methodref */
    CALCULATE_METHOD_INFO_FROM_METHOD_REFERENCE(op1u2idx);

    /* Must be a valid reference to a method */
    CHECK_VALID_CODEATRIDX(pcpd_Methodref
                             ->LOCAL_Methodref_binding.codeatridxJVM);

    /* Must be a static method */
    CHECK_STATIC_METHOD;

    /* Must not be an abstract method */
    CHECK_NOT_ABSTRACT_METHOD;

    /*
     * If synchronized method, attempt to gain MLOCK.
     * If successful, carry on with opcode.  If not,
     * unwind PC to beginning of instruction and
     * quit.  The thread model will re-enter the
     * opcode when the lock has been acquired. 
     */
    if (ACC_SYNCHRONIZED & pmth->access_flags)
    {
        if (rfalse == objectutil_synchronize(
                          CLASS(clsidxmisc).class_objhash,
                          CURRENT_THREAD))
        {
            pc->offset -= sizeof(u1); /* size of opcode */
            pc->offset -= sizeof(u2); /* size of operand */
            break;
        }
    }

    if (ACC_NATIVE & pmth->access_flags)
    {
        cpidxmisc        = pcpd_Methodref->name_and_type_index;
        pcpd             = pcfs->constant_pool[cpidxmisc];
        pcpd_NameAndType = PTR_THIS_CP_NameAndType(pcpd);

        native_run_method(CURRENT_THREAD,
                          pcpd_Methodref
                            ->LOCAL_Methodref_binding
                              .nmordJVM,
                          clsidxmisc,
                          pcpd_NameAndType->name_index,
                          pcpd_NameAndType->descriptor_index);
    }
    else
    {
        ; /* Start up Java code */
/*
... Don't forget to set these to new method:
        pcfs  = THIS_PCFS(thridx);
        pcode = DEREFERENCE_PC_CODE_BASE(thridx);
*/
    }
    break;

case OPCODE_B9_INVOKEINTERFACE:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_BA_XXXUNUSEDXXX1:
   goto unused_reserved_opcodes; /* Gag!  But it makes sense here */

case OPCODE_BB_NEW:

    /* Retrieve the constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2idx);

    /* Must reference a normal class, not an array or interface class */
    CHECK_CP_TAG(op1u2idx, CONSTANT_Class);
    CHECK_NOT_ABSTRACT_CLASS;
    CHECK_NOT_ARRAY_OBJECT;
    CHECK_NOT_INTERFACE_CLASS;

    /* calc clsidxmisc and pcpd and pcpd_Class and pcfsmisc */
#if 0
    CALCULATE_CLASS_INFO_FROM_CLASS_REFERENCE(op1u2idx);
#else
    pcpd       = pcfs->constant_pool[op1u2idx];
    pcpd_Class = PTR_THIS_CP_Class(pcpd);
    clsidxmisc = pcpd_Class->LOCAL_Class_binding.clsidxJVM;
    if (jvm_class_index_null == clsidxmisc)
    {
        /* Need local variable to avoid possible expansion confusion */

                                        /* pcpd_Class->name_index; */
        jvm_constant_pool_index cpidxOLD = op1u2idx;

        /* If class is not loaded, go retrieve it by UTF8 class name */
#if 0  
        LATE_CLASS_LOAD(cpidxOLD);
#else

        pcpd       = pcfs->constant_pool[cpidxOLD]; /* Class name */
        pcpd_Class = PTR_THIS_CP_Class(pcpd);
                                                      /* UTF8 string */
        pcpd       = pcfs->constant_pool[pcpd_Class->name_index];
        pcpd_Utf8  = PTR_THIS_CP_Utf8(pcpd);
                                                                      
        prchar_clsname = utf_utf2prchar(pcpd_Utf8);
                                                                      
        /* Try again to load class */
        clsidxmisc = class_load_resolve_clinit(prchar_clsname,
                                               CURRENT_THREAD,
                                               rfalse,
                                               rfalse);
        
        HEAP_FREE_DATA(prchar_clsname);
        
        /* If class is irretrievable, abort */
        if (jvm_class_index_null == clsidxmisc)
        {
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR);
/*NOTREACHED*/
        }
#endif
    }                                                                   
    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs; /* Extra ; */
#endif

    /* Create new object from this class */
    special_obj_misc = OBJECT_STATUS_EMPTY;

    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_THREAD,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_THREAD;
    }

    objhashmisc =
        object_instance_new(special_obj_misc,
                            pcfsmisc,
                            clsidxmisc,
                            0,
                            (rvoid *) rnull,
                            rfalse,
                            thridx);

    /* Store result to stack */
    PUSH(thridx, (jint) objhashmisc);

    break;

case OPCODE_BC_NEWARRAY:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_BD_ANEWARRAY:

    /* Retrieve the constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2idx);

    /*!
     * @todo Make sure that @e all of "class, array, or interface type"
     *       is supported by this test:
     */

    /* Must reference a class */
    CHECK_CP_TAG(op1u2idx, CONSTANT_Class);
    /* CHECK_CP_TAG2/3(op1u2idx, CONSTANT_Class,array? ,interface? ); */

    /* calc clsidxmisc and pcpd and pcpd_Class and pcfsmisc */
#if 1
    CALCULATE_CLASS_INFO_FROM_CLASS_REFERENCE(op1u2idx);
#else
    pcpd       = pcfs->constant_pool[op1u2idx];
    pcpd_Class = PTR_THIS_CP_Class(pcpd);
    clsidxmisc = pcpd_Class->LOCAL_Class_binding.clsidxJVM;
    if (jvm_class_index_null == clsidxmisc)
    {
        /* Need local variable to avoid possible expansion confusion */

                                           /* pcpd_Class->name_index; */
        jvm_constant_pool_index cpidxOLD = op1u2idx;

        /* If class is not loaded, go retrieve it by UTF8 class name */
#if 0  
        LATE_CLASS_LOAD(cpidxOLD);
#else

        pcpd       = pcfs->constant_pool[cpidxOLD]; /* Class name */
        pcpd_Class = PTR_THIS_CP_Class(pcpd);
                                                      /* UTF8 string */
        pcpd       = pcfs->constant_pool[pcpd_Class->name_index];
        pcpd_Utf8  = PTR_THIS_CP_Utf8(pcpd);
                                                                      
        prchar_clsname = utf_utf2prchar(pcpd_Utf8);
                                                                      
        /* Try again to load class */
        clsidxmisc = class_load_resolve_clinit(prchar_clsname,
                                               CURRENT_THREAD,
                                               rfalse,
                                               rfalse);
        
        HEAP_FREE_DATA(prchar_clsname);
        
        /* If class is irretrievable, abort */
        if (jvm_class_index_null == clsidxmisc)
        {
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR);
/*NOTREACHED*/
        }
#endif
    }                                                                   
    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs; /* Extra ; */
#endif

    /* Retrieve 'count' operand from TOS */
    POP(thridx, jitmp1, /* Redundant */ jint  );

    /* Cannot have negative number of array elements (zero is okay) */
    if (0 > jitmp1)
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_EXCEPTION,
                         JVMCLASS_JAVA_LANG_NEGATIVEARRAYSIZEEXCEPTION);
/*NOTREACHED*/
    }

    /* Create new object from this class, array, or interface */
#if 1
    special_obj_misc = OBJECT_STATUS_ARRAY;
#else
    special_obj_misc = OBJECT_STATUS_EMPTY;
    if (CLASS_STATUS_ARRAY & CLASS(clsidxmisc).status)
    {
        special_obj_misc |= OBJECT_STATUS_ARRAY;
    }
#endif

    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_THREAD,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_THREAD;
    }

    /* Notice that @b ANEWARRAY only handles a single array dimension */
    objhashmisc =
        object_instance_new(special_obj_misc,
                            pcfsmisc,
                            clsidxmisc,
                            ((OBJECT_STATUS_ARRAY & special_obj_misc)
                                 ? 1
                                 : 0),
                            &jitmp1,
                            rfalse, /*! @todo:  Is 'rfalse' correct? */
                            thridx);

    /* Store result to stack */
    PUSH(thridx, (jint) objhashmisc);

    break;

case OPCODE_BE_ARRAYLENGTH:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_BF_ATHROW:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C0_CHECKCAST:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C1_INSTANCEOF:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C2_MONITORENTER:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C3_MONITOREXIT:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C4_WIDE:
    /*! @todo Test this opcode */
    iswide = rtrue;  /* Will be read then cleared by other opcodes */
    break;

case OPCODE_C5_MULTIANEWARRAY:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C6_IFNULL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C7_IFNONNULL:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C8_GOTO_W:
    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_C9_JSR_W:
    /*! @todo Write this opcode */
    STUB;
    break;


/* Reserved opcodes: */
case OPCODE_CA_BREAKPOINT:
    /*! @todo Write this opcode */
    STUB;
    break;


/* Undefined and unused opcodes, reserved */
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

/* Reserved opcodes: */
case OPCODE_FE_IMPDEP1:

 unused_reserved_opcodes:

    /*! @todo Write this opcode */
    STUB;
    break;

case OPCODE_FF_IMPDEP2:
    /*! @todo Write this opcode */
    STUB;
    break;

} /* switch(opcode) */

/* Renew indentation... */

            } /* while ... */
        } /* if nonlocal_thread_return else */

    } /* if thread_exception_setup() else */

    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /* END OF GIANT SWITCH STATEMENT if(){}else{while(){switch()}}**/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/
    /***************************************************************/

    /* If the timer ticked, clear flag and process next thread */
    if((rtrue == check_timeslice) && (rtrue == pjvm->timeslice_expired))
    {
        pjvm->timeslice_expired = rfalse;
    }

    /*
     * If frame is empty, thread is done running, but if
     * @link #rthread.fp_end_program fp_end_program@endlink is being
     * used to control, say, a \<cliinit\> of a
     * @link #LATE_CLASS_LOAD() LATE_CLASS_LOAD@endlink, the thread is
     * still doing something even when the end-of-program indication
     * has occurred.  See also
     * @link #opcode_end_thread_test() opcode_end_thread_test()@endlink
     * for use of this same logic.
     *
     * Notice that this check does _not_ look for the fp-end-program
     * condition per @link #opcode_end_thread_test()
       opcode_end_thread_test()@endlink.  This means that the next
     * time this thread runs, execution will pick up with the next
     * instruction past a recent @c @b \<clinit\> or @c @b \<init\>
     * call.
     */
    if (CHECK_FINAL_STACK_FRAME_ULTIMATE(thridx) &&
        (THREAD_STATE_RUNNING == THREAD(thridx).this_state))
    {
        /* Attempt to shut down thread due to code completion */
        if (rfalse == threadstate_request_complete(thridx))
        {
            sysErrMsg("opcode_run",
             "Unable to move completed thread %d to '%s' state",
                      thridx,
                      thread_state_get_name(THREAD_STATE_COMPLETE));
            THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

            return(rfalse);
        }
    }

    /*!
     * If a thread completed running, and a proper request to the
     * @b COMPLETE state was issued, then it finished normally.
     *
     * @todo Should this @c @b if() statement be inside of
     *       the block requesting @b COMPLETE state?  Should a
     *       simple @c @b return(rtrue) be there?  Should
     *       this @c @b if() statement be expanded to
     *       consider other conditions?  Etc.  Just needs review
     *       for other possibilities.
     */
    if (EXIT_JVM_THREAD == nonlocal_thread_return)
    {
        return(rtrue);
    }

    /*
     * Move unhandled condition (@b nonlocal_rc)
     * and "thread is finished running" (@b nonlocal_thread_return)
     * conditions to the @b COMPLETE state, otherwise everything
     * ran fine and thread is still in the @b RUNNING state.
     */
    if ((THREAD_STATUS_EMPTY == nonlocal_rc) &&
        (EXIT_MAIN_OKAY      == nonlocal_thread_return))
    {
        return(rtrue);
    }
    else
    {
        /* Attempt to shut down thread due to condition */
        if (rfalse == threadstate_request_complete(thridx))
        {
            sysErrMsg("opcode_run",
             "Unable to move completed thread %d to '%s' state",
                      thridx,
                      thread_state_get_name(THREAD_STATE_COMPLETE));
            THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

            return(rfalse);
        }
        /* Return @link #rtrue rtrue@endlink if end of thread detected,
         * otherwise @link #rfalse rfalse@endlink because of unhandled
         * condition.
         */
        return(rfalse);
    }

} /* END of opcode_run() */


/* EOF */
