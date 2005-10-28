/*!
 * @file nts.c
 *
 * @brief Manipulate null-terminated (@link #rchar rchar@endlink)
 * character strings.
 *
 * There are three character string types in this program:
 * null-terminated @link #rchar (rchar)@endlink strings
 * @e ala 'C' language, UTF-8
 * @link #CONSTANT_Utf8_info (CONSTANT_Utf8_info)@endlink strings,
 * and Unicode @link #jchar (jchar)[]@endlink strings.
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
ARCH_SOURCE_COPYRIGHT_APACHE(nts, c,
"$URL$",
"$Id$");


#include "jvmcfg.h" 
#include "cfmacros.h"
#include "classfile.h"
#include "nts.h"


/*!
 * @brief Convert null-terminated string buffer into UTF8 buffer.
 *
 *
 * @param    inbfr   String (rchar *) string
 *
 *
 * @returns UTF8 structure containing length and rchar bfr (plus tag),
 *          but return in (cp_info_dup) for full proper word alignment.
 *          When done with the data, call HEAP_FREE_DATA() on it.
 *
 *    @c @b rc-\>bytes      UTF8 version of @b inbfr string
 *
 *    @c @b rc-\>length     Number of UTF8 bytes in
 *                          @c @b rc-\>bytes
 */

cp_info_dup *nts_prchar2utf(rchar *inbfr)
{
    ARCH_FUNCTION_NAME(nts_prchar2utf);

    jshort len = portable_strlen(inbfr);

    /*
     * Allocate enough heap space for output string, but within the
     * context of the output result type.  The size calculation
     * replaces generic (cp_info) with specifc (CONSTANT_Utf8_info)
     * info, adjusting for the amount of string data to be stored
     * into the result.
     */
    cp_info_dup *rc = HEAP_GET_DATA(sizeof(cp_info_dup) -
                                        sizeof(cp_info) +
                                        sizeof(CONSTANT_Utf8_info) -
                                        sizeof(u1) +
                                        len,
                                    rfalse);

    /* Move (rchar *) string into (CONSTANT_Utf8_info) */
    CONSTANT_Utf8_info *pcpui = PTR_THIS_CP_Utf8(rc);
    pcpui->tag = CONSTANT_Utf8;
    pcpui->length = len;

    portable_memcpy((jubyte *) pcpui->bytes, inbfr, len);

    rc->empty[0] = FILL_INFO_DUP0;
    rc->empty[1] = FILL_INFO_DUP1;
    rc->empty[2] = FILL_INFO_DUP2;

    return(rc);

} /* END of nts_prchar2utf() */


/*!
 * @brief Convert null-terminated string into Unicode buffer.
 *
 *
 * @param[in]  inbfr     Null-terminated string
 *
 * @param[out] outbfr    Buffer for resulting Unicode character string.
 *                       This buffer will need to be the same size in
 *                       Unicode (jchar) characters as @b inbfr is in
 *                       native characters (rchar) since the
 *                       conversion is simply putting the ASCII
 *                       into the LS byte of the Unicode character.
 *
 *
 * @returns  Two returns, one a buffer, the other a count:
 *
 *    *outbfr        Unicode version of @b inbfr string in @b outbfr
 *
 *    charcnvcount   (Return value of function)  Number of Unicode
 *                     characters in @b outbfr.
 *
 */

jshort nts_prchar2unicode(rchar *inbfr, jchar *outbfr)
{
    ARCH_FUNCTION_NAME(nts_prchar2unicode);

    jshort charcnvcount;

    jchar inbfrcnv;

    jshort len = portable_strlen(inbfr);

    
    for (charcnvcount = 0; charcnvcount < len; charcnvcount++)
    {
        /* Put ASCII into LS byte of output */
        inbfrcnv = 0;
        inbfrcnv |= inbfr[charcnvcount];

        outbfr[charcnvcount] = inbfrcnv;

    }

    /* Done.  Return number of characters processed */
    return(charcnvcount);

} /* END of nts_prchar2unicode() */




/*!
 * @brief Format a string buffer into UTF8 buffer with Java class
 * information, including number of array dimensions.
 *
 *
 * @param    inbfr     String (rchar *) string
 *
 * @param    arraydims Number of array dimensions
 *
 *
 * @returns UTF8 structure containing length and rchar bfr (plus tag),
 *          but return in (cp_info_dup) for full proper word alignment.
 *          When done with the data, call HEAP_FREE_DATA() on it.
 *          With @b inbfr of @c @b some/path/name/filename,
 *          the result will be, with 3 array dimensions:
 *
 * @verbatim

                 [[[Lsome/path/name/filename;\0

   @endverbatim
 *
 *           The string then has a @c @b \\0 NUL character
 *           appended to it for strfn() convenience, but this is
 *           not reported in the UTF8 string length.
 *
 *
 *    @c @b rc-\>bytes     UTF8 version of @b inbfr string
 *
 *    @c @b rc-\>length    Number of UTF8 bytes in
 *                         @c @b rc-\>bytes
 *
 */

cp_info_dup *nts_prchar2utf_classname(rchar         *inbfr,
                                      jvm_array_dim  arraydims)
{
    ARCH_FUNCTION_NAME(nts_prchar2utf_classname);

    jshort inbfrlen = portable_strlen(inbfr);

    /*
     * Allocate enough heap space for output string, but within the
     * context of the output result type.  The size calculation
     * replaces generic (cp_info) with specifc (CONSTANT_Utf8_info)
     * info, adjusting for the amount of string data to be stored
     * into the result, as adjusted for Java class name formatting.
     */

    /* This calculation follows the above description of text format */
    int fmtlen = arraydims +      /* Bracket characters */
                 sizeof(u1) +     /* Type specifier */
                 inbfrlen +       /* Data */
                 sizeof(u1) +     /* Type terminator */
                 sizeof(u1);      /* NUL character */

    cp_info_dup *rc =
        HEAP_GET_DATA(sizeof(cp_info_dup) -   /* Enclosing structure */
                          sizeof(cp_info) +   /* Basic type */
                          sizeof(CONSTANT_Utf8_info) - /* UTF8 type */
                          sizeof(u1) +        /* Data place holder */
                          fmtlen,             /* UTF8 data area */
                      rfalse);

    /* Move (rchar *) string into (CONSTANT_Utf8_info) */
    CONSTANT_Utf8_info *pcpui = PTR_THIS_CP_Utf8(rc);
    pcpui->tag = CONSTANT_Utf8;
    pcpui->length = fmtlen - sizeof(u1);/*Adjust out trailing \0 rchar*/

    /* Format array dimensions, Java type name, class name, terminator*/
    jvm_utf_string_index utfidx = 0;
    for (utfidx = 0; utfidx < arraydims; utfidx++)
    {
        pcpui->bytes[utfidx] = BASETYPE_CHAR_ARRAY;
    }

    pcpui->bytes[utfidx] = BASETYPE_CHAR_L;
    utfidx++;

    portable_memcpy((jubyte *) &pcpui->bytes[utfidx], inbfr, inbfrlen);

    rc->empty[0] = FILL_INFO_DUP0;
    rc->empty[1] = FILL_INFO_DUP1;
    rc->empty[2] = FILL_INFO_DUP2;

    pcpui->bytes[utfidx + inbfrlen]     = BASETYPE_CHAR_L_TERM;
    pcpui->bytes[utfidx + inbfrlen + 1] = '\0';

    return(rc);

} /* END of nts_prchar2utf_classname() */


/*!
 * @brief Report the number of array dimensions prefixing a Java type
 * string.
 *
 * No overflow condition is reported since it is assumed that @b inbfr
 * is a valid (rchar *) string.  Notice that because this logic checks
 * @e only for array specifiers and does not care about the rest of the
 * string, it may be used to evaluate field descriptions, which will
 * not contain any class formatting information.
 *
 * If there is even a @e remote possibility that more than
 * CONSTANT_MAX_ARRAY_DIMS dimensions will be found, compare the
 * result of this function with the result of nts_prchar_isarray().
 *  If there is a discrepancy, then there was an overflow here.
 * Properly formatted class files will @e never contain code with
 * this condition.
 *
 * @note  This function is identical to nts_get_prchararraydims()
 *        except that it works on (rchar *) instead of
 *        (CONSTANT_Utf8_info *).
 *
 *
 * @param    inbfr   (rchar *) string.
 *
 *
 * @returns Number of array dimensions in string.  For example,
 *          this string contains three array dimensions:
 *
 * @verbatim

                 [[[Lsome/path/name/filename;

   @endverbatim
 *
 *           The string does @e not have a @c @b \\0 NUL
 *           character appended in this instance.  If more than
 *           CONSTANT_MAX_ARRAY_DIMS are located, the
 *           result is zero-- no other error is reported.
 *
 */

jvm_array_dim nts_get_prchar_arraydims(rchar *inbfr)
{
    ARCH_FUNCTION_NAME(nts_get_prchar_arraydims);

    /* Make return code wider than max to check overflow */
    u4 rc = 0;

    /* Start scanning at beginning of string */
    u1 *pclsname = (u1 *) inbfr;

    /* Keep scanning until no more array specifications are found */
    while (BASETYPE_CHAR_ARRAY == *pclsname++)
    {
        rc++; 
    }

    /* Check overflow, return default if so, else number of dimensions*/
    if (CONSTANT_MAX_ARRAY_DIMS < rc)
    {
        return(LOCAL_CONSTANT_NO_ARRAY_DIMS);
    }
    else
    {
        /* Perform narrowing conversion into proper type for max */
        return((jvm_array_dim) rc);
    }

} /* END of nts_get_prchar_arraydims() */


/*!
 * @brief Test whether or not a Java type string is an array or not.
 *
 *
 * @param    inbfr   (rchar *) string.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this is an array
 *          specfication, else @link #rfalse rfalse@endlink.
 *
 */

rboolean nts_prchar_isarray(rchar *inbfr)
{
    ARCH_FUNCTION_NAME(nts_prchar_isarray);

    return((BASETYPE_CHAR_ARRAY == (u1) inbfr[0]) ? rtrue : rfalse);

} /* END of nts_prchar_isarray() */


/*!
 * @brief Verify if a null-terminated string contains PRIMATIVE
 * formatting or not.  May be prefixed with array specifiers.
 * Everything after the base type character is ignored.
 *
 *
 * @param  src   Pointer to null-terminated string.
 *
 *
 * @returns @link #rtrue rtrue@endlink if string is formtted as
 *          @c @b LClassName; but
 *          @link #rfalse rfalse@endlink otherwise, may also have
 *          array descriptor prefixed,
 *          thus @c @b [[LClassName;
 *
 *          @link #rtrue rtrue@endlink if string is formatted as
 *          @c @b \@ (where @c @b \@ is any
 *          @link #BASETYPE_CHAR_B BASETYPE_CHAR_x@endlink character),
 *          @link #rfalse rfalse@endlink otherwise.  May also have
 *          array descriptor prefixed,
 *          thus @c @b [[\@, eg, @c @b [[I or @c @b [[[[D
 *
 */

rboolean nts_prchar_isprimativeformatted(rchar *src)
{
    ARCH_FUNCTION_NAME(nts_isprimativeformatted);

    jvm_array_dim arraydims = nts_get_prchar_arraydims(src);

    /*
     * Chk if @e any primative base type,
     * but NOT class (the @c @b L fmt) 
     */
    switch (src[arraydims])
    {
        case BASETYPE_CHAR_B:
        case BASETYPE_CHAR_C:
        case BASETYPE_CHAR_D:
        case BASETYPE_CHAR_F:
        case BASETYPE_CHAR_I:
        case BASETYPE_CHAR_J:
        case BASETYPE_CHAR_S:
        case BASETYPE_CHAR_Z:
            return(rtrue);

        default:
            return(rfalse);
    }

} /* END of nts_prchar_isprimativeformatted() */


/*!
 * @brief Verify if a null-terminated string contains CLASS formatting
 * or not.
 *
 *
 * @param  src   Pointer to null-terminated string.
 *
 *
 * @returns @link #rtrue rtrue@endlink if string is formatted as
 *          @c @b LClasSName; but
 *          @link #rfalse rfalse@endlink otherwise.  May also have
 *          array descriptor prefixed, thus @c @b [[LClassName;
 *
 *
 * @note  This function works just like utf_isclassformatted() except
 *        that it works on (rchar *) strings rather than
 *        on (CONSTANT_Utf8_info) strings.
 */

rboolean nts_prchar_isclassformatted(rchar *src)
{
    ARCH_FUNCTION_NAME(nts_isclassformatted);

    u2 idx;
    rint rc = rfalse;

    /* Chk array or class specifier.  If neither, cannot be formatted */
    switch (src[0])
    {
        case BASETYPE_CHAR_ARRAY:
        case BASETYPE_CHAR_L:
            break;
        default:
            return(rfalse);
    }

    /*
     * Now assume a potentially formatted string.
     * Check for termination byte next.  If not present,
     * nothing else matters and string cannot be formatted.
     */
    u1 *pbytes = src;
    int len    = portable_strlen(src);

    for (idx = 0; idx < len; idx++)
    {
        if (BASETYPE_CHAR_L_TERM == pbytes[idx])
        {
            rc = rtrue;
            break;
        }
    }

    /* If not terminated, then cannot be class formatted */
    if (rfalse == rc)
    {
        return(rc);
    }

    /* Check initial formatting, including array spec */
    jvm_array_dim arraydims = nts_get_prchar_arraydims(src);

    /* If any array specs, look immediately past them for class spec */
    if (BASETYPE_CHAR_L == pbytes[arraydims])
    {
        return(rtrue);
    }
    else
    {
        return(rfalse);
    }

} /* END of nts_prchar_isclassformatted() */


/*!
 * @brief Strip a null-terminated string of any class formatting it
 * contains and return result in a heap-allocated buffer.  When done
 * with this result, perform HEAP_DATA_FREE(result) to return buffer
 * to heap.
 *
 *
 * @param  inbfr Pointer to null-terminated string that is potentially
 *               formatted as @c @b LClassName; and which may
 *               also have array descriptor prefixed, thus 
 *               @c @b [[LClassName;
 *
 *
 * @returns heap-allocated buffer containing @c @b ClassName
 *          with no formatting, regardless of input formatting or lack
 *          thereof.
 *
 *
 * @note  This function works just like
 *        utf_utf2utf_unformatted_classname()
 *        except that it takes a (rchar *) string
 *        rather than a (CONSTANT_Utf8_info) string
 *        and returns a (rchar *).
 *
 */

rchar *nts_prchar2prchar_unformatted_classname(rchar *inbfr)
{
    ARCH_FUNCTION_NAME(nts_prchar2prcahr_unformatted_classname);

    int inbfrlen            = portable_strlen(inbfr);
    rint isfmt              = nts_prchar_isclassformatted(inbfr);
    jvm_array_dim arraydims = nts_get_prchar_arraydims(inbfr);
    rchar *psemi;
    int allocsize;
    int startposn;

    if (rtrue == isfmt)
    {
        psemi = portable_strchr(inbfr, BASETYPE_CHAR_L_TERM);
        psemi--;

        allocsize = inbfrlen -   /* Input data size */
                    arraydims -  /* Array specifiers */
                    sizeof(u1) - /* Type specifier */
                    sizeof(u1) + /* Type terminator */
                    sizeof(u1);  /* NUL terminator */

        startposn = arraydims + sizeof(u1);  /* Skip array & type */
    }
    else
    {
        psemi = (rchar *) rnull;
        allocsize = inbfrlen +   /* Input data size */
                    sizeof(u1);  /* NUL terminator */

        startposn = 0; /* Copy the whole string */
    }

    rchar *rc = HEAP_GET_DATA(allocsize, rfalse);

    /* Extract input class name from input buffer, add null char */
    portable_memcpy(rc, &inbfr[startposn], allocsize);
    rc[allocsize - sizeof(u1)] = '\0';

    return(rc);

} /* END of nts_prchar2prchar_unformatted_classname() */


/* EOF */
