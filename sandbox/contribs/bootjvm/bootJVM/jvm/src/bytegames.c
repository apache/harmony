/*!
 * @file bytegames.c
 *
 * @brief Perform byte swapping, word swapping, byte-aligned accesses,
 * non-aligned multi-byte items, etc.
 *
 * General utilities for playing shell games with real machine bytes
 * for both the real machine and the Java virtual machine.
 *
 * Some machine architectures use little-endian versus big-endian byte
 * ordering, some architectures do not natively support 2-byte or
 * 4-byte or 8-byte word addressing on addesses that are not aligned
 * to those boundaries.  The JVM does not care about such things,
 * but must accomodate real machine implementations in a way that
 * they do not complain about it at run time.  There area also
 * functions here that provide such support.  They typically are
 * embedded in widely-used macros from
 * @link jvm/src/cfmacros.h cfmacros.h@endlink and
 * @link jvm/src/util.h util.h@endlink.
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
ARCH_SOURCE_COPYRIGHT_APACHE(bytegames, c,
"$URL$",
"$Id$");

 
#include "jvmcfg.h"
#include "util.h"

/*!
 * @name Unions for 1- 2- and 4- and 8-byte value ordering conversions.
 *
 * @brief Load in a value in one format and extract them in another.
 *
 * These unions are generic enough to be able to support signed and
 * unsigned values for loading and storing 1-, 2-, 4-, and 8-byte
 * integers.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Structure for shuffling two-byte values.
 *
 * Load into @b _usval as an rushort and extract as bytes,
 * or vice versa.
 *
 */
typedef union
{
    rushort _usval; /**< Unsigned short representation */

    struct
    {
        rbyte a;
        rbyte b;

    } _byteval;     /**< Unsigned byte representation */

} twobyte;

/*!
 * @brief Structure for shuffling four-byte values
 *
 * Load into @b _ruival as an ruint or @b _juival as a juint
 * and extract as bytes, or vice versa.
 *
 */
typedef union
{
    ruint  _ruival;  /**< Real unsigned int representation */

    juint  _juival;  /**< Java unsigned int representation */

    jfloat _jfval;   /**< Java float representation */

    struct
    {
        rbyte a;
        rbyte b;
        rbyte c;
        rbyte d;

    } _byteval;     /**< Unsigned byte representation */

} fourbyte;

/*!
 * @brief Structure for shuffling eight-byte values
 *
 * This structure handles both real types @link #rlong rlong@endlink
 * @link #rdouble rdouble@endlink and Java types
 * @link #jlong jlong@endlink and @link #jdouble jdouble@endlink
 *
 * Load into one of @b _rulval, @b _rdval, @b _julval, or @b _jdval,
 * respectively, and extract as two unsigned integers from @b _intval
 * or as bytes from @b _byteval.  Any combination of loading and storing
 * is permissible, although only certain combinations are useful for 
 * a given context.  (Namely, storing as @link #jdouble jdouble@endlink
 * and extracting as two @link #ruint ruint@endlink values doe not
 * really make much sense.)
 *
 */
typedef union
{
    rulong  _rulval;  /**< Real unsigned long long representation */

    rdouble _rdval;   /**< Real double representation */

    julong  _julval;  /**< Java unsigned long long representation */

    jdouble _jdval;   /**< Java double representation */

    struct
    {
#ifdef ARCH_BIG_ENDIAN
        fourbyte ms;
        fourbyte ls;
#else
        fourbyte ls;
        fourbyte ms;
#endif

    } _intval;        /**< Unsigned int representation */

    struct
    {
        rbyte a;
        rbyte b;
        rbyte c;
        rbyte d;
        rbyte e;
        rbyte f;
        rbyte g;
        rbyte h;

    } _byteval;       /**< Unsigned byte representation */

} eightbyte;

/*@} */ /* End of grouped definitions */


/**********************************************************************/
/*!
 * @name Unaligned multi-byte access support.
 *
 * @brief Store and retrieve arbitrary 2- and 4-byte values from
 * unaligned addresses without causing @b SIGSEGV signals by performing
 * 1-byte addressing on these locations.
 *
 * The 2- and 4-byte functions are typically used for absorbing class
 * file stream data into a ClassFile structure.  Of particular
 * interest is
 * @link #classfile_load_classdata() classfile_load_classdata()@endlink,
 * where it is used to retrieve many different types of 2- and 4-byte
 * data.  The 8-byte functions are used by @link
   #class_get_constant_field_attribute()
   class_get_constant_field_attribute()@endlink for retrieving
 * @link #jlong jlong@endlink and @link #jdouble jdouble@endlink
 * constants.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Retrieve any generic 2-byte value (16 bits), whether or not
 * its is aligned on a 2-byte (that is, even address) boundary.
 *
 * This function was written to suppress @b SIGSEGV issues on
 * GCC -m32 binaries on a Solaris 9.
 *
 * Typical usage is in a situation of:
 * @verbatim
  
       rshort *pshort;
       ...
       val = *pshort;   ... If pshort is at odd address, throw SIGSEGV.
  
   @endverbatim
 *
 * Thus convert to:
 * @verbatim
  
       rshort *pshort;
       ...
       val = bytegames_getrs2(pshort);    ... No signal this way.
  
   @endverbatim
 *
 * This causes two one-byte accesses to happen instead of a single
 * two-byte access, eliminating the cause of @b SIGSEGV, unless, of
 * course, the pointer is off in the weeds instead of looking at
 * valid memory.
 *
 *
 * @param  ptr2   Pointer to @link #rushort rushort@endlink location.
 *
 *
 * @returns  16-bit value at *ptr2, properly byte swapped.
 *
 * @see bytegames_getrs2_le()
 *
 */
rushort bytegames_getrs2(rushort *ptr2)
{
    ARCH_FUNCTION_NAME(bytegames_getrs2);

    twobyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr2;

    wholeval._byteval.a = *ptr1++;
    wholeval._byteval.b = *ptr1;

#ifdef ARCH_LITTLE_ENDIAN
    wholeval._usval = bytegames_swap2(wholeval._usval);
#endif

    return(wholeval._usval);

} /* END of bytegames_getrs2() */


/*!
 * @brief Retrieve any generic 2-byte value (16 bits), source buffer
 * ordered <b>little endian</b>, whether or not its is aligned on
 * a 2-byte (that is, even address) boundary.
 *
 * This function is distinct from @b bytegames_getrs2() in that it
 * assumes @b little endian source data instead of @b big endian
 * source data.
 *
 *
 * @param  ptr2   Pointer to @link #rushort rushort@endlink location.
 *
 *
 * @returns  16-bit value at *ptr2, properly byte swapped.
 *
 * @see bytegames_getrs2()
 */
rushort bytegames_getrs2_le(rushort *ptr2)
{
    ARCH_FUNCTION_NAME(bytegames_getrs2_le);

    twobyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr2;

    wholeval._byteval.a = *ptr1++;
    wholeval._byteval.b = *ptr1;

#ifdef ARCH_BIT_ENDIAN
    wholeval._usval = bytegames_swap2(wholeval._usval);
#endif

    return(wholeval._usval);

} /* END of bytegames_getrs2_le() */


/*!
 * @brief Store any generic 2-byte value (16 bits), whether or not
 * its is aligned on a 2-byte (that is, even address) boundary.
 *
 * This function is the inverse of
 * @link #bytegames_getrs2() bytegames_getrs2()@endlink
 * above, which see for further explanation.
 *
 *
 * @param  ptr2   Pointer to @link #rushort rushort@endlink location.
 *
 * @param  val2   A @link #rushort rushort@endlink value to be stored.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid bytegames_putrs2(rushort *ptr2, rushort val2)
{
    ARCH_FUNCTION_NAME(bytegames_putrs2);

    twobyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr2;

    wholeval._usval = val2;

#ifdef ARCH_LITTLE_ENDIAN
    wholeval._usval = bytegames_swap2(wholeval._usval);
#endif

    *ptr1++ = wholeval._byteval.a;
    *ptr1   = wholeval._byteval.b;

    return;

} /* END of bytegames_putrs2() */


/*!
 * @brief 4-byte version of
 * @link #bytegames_getrs2() bytegames_getrs2()@endlink, but
 * performs two odd-byte accesses, not just one.
 *
 * This causes four one-byte accesses to happen instead of a single
 * four-byte access, eliminating the cause of @b SIGSEGV.
 *
 * @param   ptr4   Pointer to @link #ruint ruint@endlink location.
 *
 *
 * @returns 32-bit value at *ptr4, properly byte swapped.
 *
 * @see bytegames_getri4_le()
 *
 */
ruint bytegames_getri4(ruint *ptr4)
{
    ARCH_FUNCTION_NAME(bytegames_getri4);

    fourbyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr4;

    wholeval._byteval.a = *ptr1++;
    wholeval._byteval.b = *ptr1++;
    wholeval._byteval.c = *ptr1++;
    wholeval._byteval.d = *ptr1;

#ifdef ARCH_LITTLE_ENDIAN
    wholeval._ruival = bytegames_swap4(wholeval._ruival);
#endif

    return(wholeval._ruival);

} /* END of bytegames_getri4() */


/*!
 * @brief 4-byte version of
 * @link #bytegames_getrs2() bytegames_getrs2()@endlink, source
 * buffer order <b>little endian</b>, and performs two odd-byte
 * accesses, not just one.
 *
 * This function is distinct from @b bytegames_getri4() in that it
 * assumes @b little endian source data instead of @b big endian
 * source data.
 *
 * @param   ptr4   Pointer to @link #ruint ruint@endlink location.
 *
 *
 * @returns 32-bit value at *ptr4, properly byte swapped.
 *
 * @see bytegames_getri4()
 *
 */
ruint bytegames_getri4_le(ruint *ptr4)
{
    ARCH_FUNCTION_NAME(bytegames_getri4);

    fourbyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr4;

    wholeval._byteval.a = *ptr1++;
    wholeval._byteval.b = *ptr1++;
    wholeval._byteval.c = *ptr1++;
    wholeval._byteval.d = *ptr1;

#ifdef ARCH_BIG_ENDIAN
    wholeval._ruival = bytegames_swap4(wholeval._ruival);
#endif

    return(wholeval._ruival);

} /* END of bytegames_getri4_le() */


/*!
 * @brief Store any generic 4-byte value (32 bits), whether or not
 * its is aligned on a 2-byte (that is, even address) boundary.
 *
 * This function is the inverse of
 * @link #bytegames_getri4() bytegames_getri4()@endlink
 * above, which see for further explanation.
 *
 *
 * @param  ptr4   Pointer to @link #ruint ruint@endlink location.
 *
 * @param  val4   An @link #ruint ruint@endlink value to be stored.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid bytegames_putri4(ruint *ptr4, ruint val4)
{
    ARCH_FUNCTION_NAME(bytegames_putri4);

    fourbyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr4;

    wholeval._ruival = val4;

#ifdef ARCH_LITTLE_ENDIAN
    wholeval._ruival = bytegames_swap4(wholeval._ruival);
#endif

    *ptr1++ = wholeval._byteval.a;
    *ptr1++ = wholeval._byteval.b;
    *ptr1++ = wholeval._byteval.c;
    *ptr1   = wholeval._byteval.d;

    return;

} /* END of bytegames_putri4() */


/*!
 * @brief 8-byte version of
 * @link #bytegames_getri4() bytegames_getri4()@endlink, but
 * performs four odd-byte accesses, not just one.
 *
 * This causes eight one-byte accesses to happen instead of a single
 * eight-byte access, eliminating the cause of @b SIGSEGV.
 *
 *
 * @param  ptr8   Pointer to @link #rulong rulong@endlink location.
 *
 *
 * @returns  64-bit value from *ptr8, properly byte swapped.
 *
 */
rulong bytegames_getrl8(rulong *ptr8)
{
    ARCH_FUNCTION_NAME(bytegames_getrl8);

    eightbyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr8;

    wholeval._byteval.a = *ptr1++;
    wholeval._byteval.b = *ptr1++;
    wholeval._byteval.c = *ptr1++;
    wholeval._byteval.d = *ptr1++;
    wholeval._byteval.e = *ptr1++;
    wholeval._byteval.f = *ptr1++;
    wholeval._byteval.g = *ptr1++;
    wholeval._byteval.h = *ptr1;

#ifdef ARCH_LITTLE_ENDIAN
    wholeval._rulval = bytegames_swap8(wholeval._rulval);
#endif

    return(wholeval._rulval);

} /* END of bytegames_getrl8() */


/*!
 * @brief Store any generic 8-byte value (64 bits), whether or not
 * its is aligned on a 2-byte (that is, even address) boundary.
 *
 * @brief This function is the inverse of
 * @link #bytegames_getrl8() bytegames_getrl8()@endlink above, which see
 * for further explanation.
 *
 * @attention NOTICE THAT THE TERM "long" and THE CAST @b (long) ARE
 *            FASTIDIOUSLY AVOIDED IN ORDER TO REMOVE ANY DOUBT AS
 *            TO WHAT IS A 32-BIT VALUE AND WHAT IS A 64-BIT VALUE!
 *
 *
 * @param  ptr8   Pointer to @link #rulong rulong@endlink location.
 *
 * @param  val8   A @link #rulong rulong@endlink value to be stored.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid bytegames_putrl8(rulong *ptr8, rulong val8)
{
    ARCH_FUNCTION_NAME(bytegames_putrl8);

    eightbyte wholeval;

    rbyte *ptr1 = (rbyte *) ptr8;

    wholeval._rulval = val8;

#ifdef ARCH_LITTLE_ENDIAN
    wholeval._rulval = bytegames_swap8(wholeval._rulval);
#endif

    *ptr1++ = wholeval._byteval.a;
    *ptr1++ = wholeval._byteval.b;
    *ptr1++ = wholeval._byteval.c;
    *ptr1++ = wholeval._byteval.d;
    *ptr1++ = wholeval._byteval.e;
    *ptr1++ = wholeval._byteval.f;
    *ptr1++ = wholeval._byteval.g;
    *ptr1   = wholeval._byteval.h;

    return;

} /* END of bytegames_putrl8() */

/*@} */ /* End of grouped definitions */


/**********************************************************************/
/*!
 * @name Byte swapping support.
 *
 * @brief Swap 2- and 4- and 8-byte values, especially for support of
 * little-endian real machines.
 *
 * Since the Java virtual machine is defined as a big-endian
 * architecture, these functions are supplied to map between the
 * big-endian JVM and a little-endian real machine implementation.
 *
 * The 2- and 4-byte functions are typically used for swapping class
 * file stream data as it is being absorbed into a ClassFile structure,
 * as well as for other general uses.  Of particular interest is
 * @link #classfile_load_classdata() classfile_load_classdata()@endlink,
 * where they are used to swap many different types of 2- and 4-byte
 * data.  The 8-byte functions are provided for completeness.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Swap 2 bytes for a little-endian machine
 * @link #rushort rushort@endlink value to a big-endian
 * Java real machine implementation @link #rushort rushort@endlink
 * value.
 *
 * Available but not used in big-endian architectures.
 *
 * This routine is mapped for little-endian machines to
 * @link #MACHINE_JSHORT_SWAP MACHINE_JSHORT_SWAP()@endlink.
 *
 * @verbatim
  
   INPUT BYTE ORDER:    (a) (b)     rshort: (ab)
  
   OUTPUT BYTE ORDER:   (b) (a)     rshort: (ba)
  
   @endverbatim
 *
 *
 * @param    val    two-byte @link #rushort rushort@endlink to swap
 *
 *
 * @returns  swapped byte version of @b val
 *
 */
rushort bytegames_swap2(rushort val)
{
    ARCH_FUNCTION_NAME(bytegames_swap2);

    twobyte wholeval;

    rbyte tmp;

    wholeval._usval = val;

    tmp = wholeval._byteval.a;
    wholeval._byteval.a = wholeval._byteval.b;
    wholeval._byteval.b = tmp;

    return(wholeval._usval);

} /* END of bytegames_swap2() */

/*!
 * @brief Swap 4 bytes for a little-endian machine
 * @link #ruint ruint@endlink value to a big-endian
 * Java real machine implementation
 * @link #ruint ruint@endlink value.
 *
 * Available but not used in big-endian architectures.
 *
 * This routine is mapped for little-endian machines to
 * @link #MACHINE_JINT_SWAP MACHINE_JINT_SWAP()@endlink.
 *
 * @verbatim
  
   INPUT BYTE ORDER:    (a) (b) (c) (d)     rint: (abcd)
  
   OUTPUT BYTE ORDER:   (d) (c) (b) (a)     rint: (dcba)
  
   @endverbatim
 *
 *
 * @param    val    four-byte @link #ruint ruint@endlink to swap
 *
 *
 * @returns  swapped byte version of @b val
 *
 */
ruint bytegames_swap4(ruint  val)
{
    ARCH_FUNCTION_NAME(bytegames_swap4);

    fourbyte wholeval;

    rbyte tmp;

    wholeval._ruival = val;

    tmp = wholeval._byteval.a;
    wholeval._byteval.a = wholeval._byteval.d;
    wholeval._byteval.d = tmp;

    tmp = wholeval._byteval.b;
    wholeval._byteval.b = wholeval._byteval.c;
    wholeval._byteval.c = tmp;

    return(wholeval._ruival);

} /* END of bytegames_swap4() */


/*!
 * @brief Swap 8 bytes for a little-endian machine
 * @link #rulong rulong@endlink value to a big-endian
 * Java real machine implementation
 * @link #rulong rulong@endlink value.
 *
 * Available but not used in big-endian architectures.
 *
 * This routine is mapped for little-endian machines to
 * @link #MACHINE_JLONG_SWAP MACHINE_JLONG_SWAP()@endlink.
 *
 * @verbatim
  
   INPUT BYTE ORDER:  (a) (b) (c) (d) (e) (f) (g) (h)  rlong: (abcdefgh)
  
   OUTPUT BYTE ORDER: (h) (g) (f) (e) (d) (c) (b) (a)  rlong: (hgfedcba)
  
   @endverbatim
 *
 *
 * @param    val    eight-byte @link #rulong rulong@endlink to swap
 *
 *
 * @returns  swapped byte version of @b val
 *
 */
rulong bytegames_swap8(rulong val)
{
    ARCH_FUNCTION_NAME(bytegames_swap8);

    eightbyte wholeval;

    rbyte tmp;

    wholeval._rulval = val;

    tmp = wholeval._byteval.a;
    wholeval._byteval.a = wholeval._byteval.h;
    wholeval._byteval.h = tmp;

    tmp = wholeval._byteval.b;
    wholeval._byteval.b = wholeval._byteval.g;
    wholeval._byteval.g = tmp;

    tmp = wholeval._byteval.c;
    wholeval._byteval.c = wholeval._byteval.f;
    wholeval._byteval.f = tmp;

    tmp = wholeval._byteval.d;
    wholeval._byteval.d = wholeval._byteval.e;
    wholeval._byteval.e = tmp;

    return(wholeval._rulval);

} /* END of bytegames_swap8() */


/*!
 * @brief Mix up 8 bytes for a little-endian machine
 * @link #rushort rushort@endlink value to a big-endian
 * Java real machine implementation
 * @link #rushort rushort@endlink value in the same way as
 * @link #bytegames_swap8() bytegames_swap8()@endlink,
 * but store the words MS first, LS second.
 *
 * Available but not used in big-endian architectures.
 *
 * This routine is mapped for little-endian machines to
 * @link #MACHINE_JLONG_MIX MACHINE_JLONG_MIX()@endlink.
 *
 * @verbatim
  
   INPUT BYTE ORDER:  (a) (b) (c) (d) (e) (f) (g) (h)  rlong: (abcdefgh)
  
   OUTPUT BYTE ORDER: (d) (c) (b) (a) (h) (g) (f) (e)  rlong: (dcbahgfe)
  
   @endverbatim
 *
 *
 * @param    val    eight-byte @link #rulong rulong@endlink to swap/mix
 *
 *
 * @returns  swapped/mixed byte version of @b val
 *
 */
rulong bytegames_mix8(rulong val)
{
    ARCH_FUNCTION_NAME(bytegames_mix8);

    eightbyte wholeval;

    rbyte tmp;

    wholeval._rulval = val;

    tmp = wholeval._byteval.a;
    wholeval._byteval.a = wholeval._byteval.d;
    wholeval._byteval.d = tmp;

    tmp = wholeval._byteval.b;
    wholeval._byteval.b = wholeval._byteval.c;
    wholeval._byteval.c = tmp;

    tmp = wholeval._byteval.e;
    wholeval._byteval.e = wholeval._byteval.h;
    wholeval._byteval.h = tmp;

    tmp = wholeval._byteval.f;
    wholeval._byteval.f = wholeval._byteval.e;
    wholeval._byteval.e = tmp;

    return(wholeval._rulval);

} /* END of bytegames_mix8() */

/*@} */ /* End of grouped definitions */


/**********************************************************************/
/*!
 * @name Combine/split support for (jlong) and (jdouble).
 *
 * The @b combine operation takes two @link #jint jint@endlink words
 * as the MS and LS words of a @link #jlong jlong@endlink or 
 * @link #jdouble jdouble@endlink and returns a combined result.
 * This is typically used for retrieving JVM local variables or
 * operand stack parameters.
 *
 * The @b split operation is the reverse.  It takes a
 * @link #jlong jlong@endlink or @link #jdouble jdouble@endlink and
 * extracts two @link #jint jint@endlink words, typically for storage
 * into a JVM local variable or operand stack parameter.
 *
 * @todo HARMONY-6-jvm-bytegames.c-1 Verify that @e all references to
 *       these routines load and store the results in the proper order!
 *       The MS word @e must be stored first (local variable 'n' or
 *       first @link #PUSH() PUSH()@endlink to operand stack).  The
 *       LS word @e must be stored second (local variable 'n+1' or
 *       second @link #PUSH() PUSH()@endlink to operand stack).  The
 *       retrieval from the operand stack @e must be LS word as the
 *       first @link #POP() POP()@endlink, MS word as the second
 *       @link #POP() POP()@endlink operation.  This problem may
 *       occur especially in @link jvm/src/native.c native.c@endlink and
 *       @link jvm/src/opcode.c opcode.c@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Combine two @link #jint jint@endlink words
 * into a @link #jlong jlong@endlink.
 *
 * Pass in two sequential words as @link #jint jint@endlink from
 * the JVM stack frame or operand stack or any other appropriate
 * source and return a result as @link #jlong jlong@endlink
 *
 *
 * @param msword  Java integer at first index.
 *
 * @param lsword  Java integer at second index, directly subsequent
 *                to @b msword.
 *
 *
 * @returns  concatenation as @link #jlong jlong@endlink, properly
 *           byte ordered.
 *
 */
jlong bytegames_combine_jlong(jint msword, jint lsword)
{
    ARCH_FUNCTION_NAME(bytegames_combine_jlong);

    eightbyte wholeval;

    wholeval._intval.ms._juival = msword;
    wholeval._intval.ls._juival = lsword;

    return(wholeval._julval);

} /* END of bytegames_combine_jlong() */


/*!
 * @brief Combine two @link #jint jint@endlink words
 * into a @link #jdouble jdouble@endlink
 *
 * Pass in two sequential words as @link #jint jint@endlink from the
 * JVM stack frame or operand stack or any other appropriate source
 * and return a result as @link #jdouble jdouble@endlink.
 *
 *
 * @param msword  Java integer at first index.
 *
 * @param lsword  Java integer at second index, directly subsequent
 *                to @b msword.
 *
 *
 * @returns  concatenation as @link #jdouble jdouble@endlink ,
 *           properly byte ordered.
 *
 */
jdouble bytegames_combine_jdouble(jint msword, jint lsword)
{
    ARCH_FUNCTION_NAME(bytegames_combine_jdouble);

    eightbyte wholeval;

    wholeval._intval.ms._juival = msword;
    wholeval._intval.ls._juival = lsword;

    return(wholeval._jdval);

} /* END of bytegames_combine_jdouble() */


/*!
 * @brief Split a @link #jlong jlong@endlink into
 * two @link #jint jint@endlink words.
 *
 * Pass in a @link #jlong jlong@endlink and return two words
 * suitable for storing into a JVM stack frame as a pair of local
 * variables or into the operand stack as two sequential
 * words.  The first receives the MS word, the second
 * receives the LS word, which should be the next sequential
 * stack frame local variable or operand stack location.
 *
 *
 * @param[in]     splitlong Java long integer to split.
 *
 * @param[out]    msword    Address of MS half of split value.
 *
 * @param[out]    lsword    Address of LS half of split value.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid bytegames_split_jlong(jlong splitlong, jint *msword, jint *lsword)
{
    ARCH_FUNCTION_NAME(bytegames_split_jlong);

    eightbyte wholeval;

    wholeval._julval = splitlong;

    *msword = wholeval._intval.ms._juival;
    *lsword = wholeval._intval.ls._juival;

    return;

} /* END of bytegames_split_jlong() */


/*!
 * @brief Split a @link #jdouble jdouble@endlink into
 * two @link #jint jint@endlink words.
 *
 * Pass in a @link #jlong jlong@endlink and return two words
 * suitable for storing into a JVM stack frame as a pair of local
 * variables or into the operand stack as two sequential
 * words.  The first receives the MS word, the second
 * receives the LS word, which should be the next sequential
 * stack frame local variable or operand stack location.
 *
 *
 * @param[in]     splitdouble Java double float to split.
 *
 * @param[out]    msword    Address of MS half of split value.
 *
 * @param[out]    lsword    Address of LS half of split value.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid bytegames_split_jdouble(jdouble  splitdouble,
                              jint    *msword,
                              jint    *lsword)
{
    ARCH_FUNCTION_NAME(bytegames_split_jdouble);

    eightbyte wholeval;

    wholeval._jdval = splitdouble;

    *msword = wholeval._intval.ms._juival;
    *lsword = wholeval._intval.ls._juival;

    return;

} /* END of bytegames_split_jdouble() */

/*@} */ /* End of grouped definitions */


/* EOF */
