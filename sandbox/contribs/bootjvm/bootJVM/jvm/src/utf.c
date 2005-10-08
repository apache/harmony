/*!
 * @file utf.c
 *
 * @brief Manipulate UTF-8 CONSTANT_Utf8_info character strings.
 *
 * There are three character string types in this program:
 * null-terminated @link #rchar (rchar)@endlink strings
 * @e ala 'C' language, UTF-8
 * @link #CONSTANT_Utf8_info (CONSTANT_Utf8_info)@endlink strings,
 * and Unicode @link #jchar (jchar)[]@endlink strings.
 *
 * Convert one or UTF-8 (jbyte) bytes to and from Unicode (jchar)
 * characters, plus related functions, like comparison and string
 * length.
 *
 * Why are these functions called @b utf_XXX() instead of @b utf8_XXX()?
 * Originally, they were called such, but when the JDK 1.5 class file
 * spec, section 4, was reviewed (after working with the 1.2/1.4
 * versions), it was discovered that certain other @b UTF-xx formats
 * were also provided in the spec, even if not accurately defined.
 * (Due to errors in the revised class file specification, the 21-bit
 * UTF characters (6 bytes) will not be implemented until a definitive
 * correction is located.  However, in anticipation of this correction,
 * the functions are now named utf_XXX() without respect to character
 * bit width.)  Notice, however, that the spec, section 4, defines a
 * CONSTANT_Utf8 and a CONSTANT_Utf8_info.  Therefore, these
 * designations will remain in the code unless changed in the spec.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/utf.c $ \$Id: utf.c 0 09/28/2005 dlydick $
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
ARCH_COPYRIGHT_APACHE(utf, c, "$URL: https://svn.apache.org/path/name/utf.c $ $Id: utf.c 0 09/28/2005 dlydick $");


#include <string.h>

#include "jvmcfg.h" 
#include "cfmacros.h"
#include "classfile.h"
#include "nts.h"
#include "util.h"


/*!
 * Store a Unicode @c @b ? when invalid UTF state found,
 * adj return code
 */
#define MAP_INVALID_UTF8_TO_QUESTION_MARK *outbfr++ = (jchar) '?'; \
                                         inbfr++

/*! Detect NUL character and quit when found */
#define RETURN_IF_NUL_BYTE if (UTF8_FORBIDDEN_ZERO == *inbfr) \
                           {return(charcnvcount); }

/*!
 * @brief Convert UTF8 buffer into Unicode buffer.
 *
 *
 * @param[in]  utf_inbfr  UTF string structure
 *
 * @param[out] outbfr     Buffer for resulting Unicode character string
 *
 *
 * @returns  Two returns, one a buffer, the other a count:
 *
 *    *outbfr        Unicode version of @b utf_inbfr string in @b outbfr
 *
 *    charcnvcount   (Return value of function)  Number of Unicode
 *                     characters in @b outbfr.  This will only be the
 *                     same as @b length when ALL UTF characters are
 *                     ASCII.  It will otherwise be less than that.
 *
 * SPEC AMBIGUITY:  In case of invalid characters, a Unicode
 * @c @b ? is inserted and processing continues.  In this way,
 * the result string will still be invalid, but at least it will be
 * proper Unicode.  This may prove more than is necessary, but the
 * spec says nothing at all about this matter.  Since the NUL character
 * may not appear in UTF-8, if a buffer is terminated by a NUL in the
 * first @c @b utf_inbfr->length bytes, termination will be
 * assumed.  If a @link #UTF8_FORBIDDEN_MIN UTF8_FORBIDDEN_xxx@endlink
 * character is read, it is converted to a Unicode @c @b ? also.
 *
 */

jshort utf_utf2unicode(CONSTANT_Utf8_info *utf_inbfr, jchar *outbfr)
{
    jshort charcnvcount;

    jubyte *inbfr = (jubyte *) utf_inbfr->bytes;

    for (charcnvcount = 0;
         charcnvcount < utf_inbfr->length;
         charcnvcount++)
    {
        RETURN_IF_NUL_BYTE;
        if (UTF8_SINGLE_MAX >= *inbfr)
        {
            /* Process one-byte form */
            *outbfr++ = (jchar) *inbfr++;
        }
        else
        {
            /* Process two-byte form */
            if (UTF8_TRIPLE_FIRST_VAL > *inbfr)
            {
                if (UTF8_DOUBLE_FIRST_VAL > *inbfr)
                {
                    MAP_INVALID_UTF8_TO_QUESTION_MARK;
                    continue;
                }

                /* Store top half of Unicode character */
                *outbfr = (jchar)
                          (((*inbfr++) & UTF8_DOUBLE_FIRST_MASK0)
                            << UTF8_DOUBLE_FIRST_SHIFT);

                /* Abort if next byte is NUL */
                RETURN_IF_NUL_BYTE;

                if ((UTF8_DOUBLE_SECOND_VAL | UTF8_DOUBLE_SECOND_MASK0)
                    < *inbfr)
                {
                    /*
                     * Map invalid forms to @c @b ? and
                     * move to next char 
                     */
                    MAP_INVALID_UTF8_TO_QUESTION_MARK;
                    continue;
                }

                /* Store bottom half of Unicode character */
                *outbfr++ |= (jchar)
                             ((*inbfr++) & UTF8_DOUBLE_SECOND_MASK0);
            }
            else
            {
                /* Process three-byte form */
                if ((UTF8_TRIPLE_FIRST_VAL | UTF8_TRIPLE_FIRST_MASK0)
                       < *inbfr)
                {
                    /* This also considers UTF8_FORBIDDEN_MIN/MAX
                         bytes */
                    MAP_INVALID_UTF8_TO_QUESTION_MARK;
                    continue;
                }

                /* Store top third of Unicode character */
                *outbfr = (jchar)
                          (((*inbfr++) & UTF8_TRIPLE_FIRST_MASK0)
                            << UTF8_TRIPLE_FIRST_SHIFT);

                /* Abort if next byte is NUL */
                RETURN_IF_NUL_BYTE;

                if ((UTF8_TRIPLE_SECOND_VAL | UTF8_TRIPLE_SECOND_MASK0)
                    < *inbfr)
                {
                    /*
                     * Map invalid forms to @c @b ? and
                     * move to next char 
                     */
                    MAP_INVALID_UTF8_TO_QUESTION_MARK;
                    continue;
                }

                /* Store middle third of Unicode character */
                *outbfr |= (jchar)
                           (((*inbfr++) & UTF8_TRIPLE_SECOND_MASK0)
                             << UTF8_TRIPLE_SECOND_SHIFT);

                /* Abort if next byte is NUL */
                RETURN_IF_NUL_BYTE;

                if ((UTF8_TRIPLE_THIRD_VAL | UTF8_TRIPLE_THIRD_MASK0)
                    < *inbfr)
                {
                    /*
                     * Map invalid forms to @c @b ? and
                     * move to next char 
                     */
                    MAP_INVALID_UTF8_TO_QUESTION_MARK;
                    continue;
                }

                /* Store bottom third of Unicode character */
                *outbfr++ |= (jchar)
                             ((*inbfr++) & UTF8_TRIPLE_THIRD_MASK0);
            }
        }

    } /* for (i) */

    /* Done.  Return number of characters processed */
    return(charcnvcount);

} /* END of utf_utf2unicode() */


/*!
 * @brief Convert a UTF string from a (CONSTANT_Utf8_info *) into a
 * null-terminated string by allocating heap and copying the UTF data.
 *
 * When done with result, perform HEAP_FREE_DATA(result).
 *
 * @param   src   Pointer to UTF string, most likely from constant pool
 *
 * @returns Null-terminated string in heap or
 *          @link #rnull rnull@endlink if heap alloc error.
 *
 */

rchar *utf_utf2prchar(CONSTANT_Utf8_info *src)
{
    /* Allocate heap for UTF data plus NUL byte */
    rchar *rc = HEAP_GET_DATA(sizeof(rchar) + src->length, rfalse);

    /* Copy to heap area */
    memcpy(rc, &src->bytes[0], src->length);

    /* Append NUL character */
    rc[src->length] = '\0';

    /* Produce result */
    return(rc);

} /* END of utf_utf2prchar() */


/*!
 * @brief Compare two strings of any length, and potentially neither
 * null-terminated, that is, could be a UTF string.
 *
 * If strings are of equal length, this function is equivalent
 * to @c @b strcmp(3).  If not of equal length, result is like
 * comparing @c @b n bytes of @c @b strncmp(3), where non-equal
 * result is returned, but if equal result, it is like
 * @c @b n+1, where the final byte is a @c @b \\0 (NUL)
 * character, so longer string's @c @b n+1 character
 * is reported, either as positive value (@b s1 longer) or as
 * negative value (@b s2 longer).
 *
 * This function should be used on ALL string comparisons that
 * potentially involve lack of NUL termination, namely, @e anything
 * to do with UTF strings of any sort.  It is recommended also for
 * any null-terminated string just so all string comparisons work
 * @e exactly alike, no matter whether (rchar *) or UTF, whether of
 * equal length or not.
 *
 * @param  s1        (rchar *) to first string
 *
 * @param  l1        Length of string @b s1, regardless of any
 *                     null termination being present or absent
 *                    in @b s1.
 *
 * @param  s2        (rchar *) to second string
 *
 * @param  l2        length of string @b s2, regardless of any
 *                     null termination being present or absent
 *                     in @b s2.
 *
 * @returns lexicographical difference of <b><code>s1 - s2</code></b>.
 *          Notice that the (rchar) data is implicitly unsigned
 *          (although the actual signage is left to the compiler),
 *          while the (jbyte) result is explicitly signed, due to the
 *          arithmetic nature of the calculation.
 *
 */
static jbyte s1_s2_strncmp(u1 *s1, int l1, u1 *s2, int l2)
{
    /* Compare shortest common run length */
    int cmplen = (l1 < l2) ? l1 : l2;
    jbyte rc = strncmp(s1, s2, cmplen);

    /*
     * THIS LOGIC IS THE SAME AS FOR unicode_strncmp(), BUT
     * OPERATES ON (jchar) instead of (rchar)
     */

    /* Return from several permutations of strlen */
    if (l1 == l2)
    {
        return(rc);
    }
    else
    if (l1 > l2)
    {
        /*
         * If a difference existed, return it, else use
         * the last character of @b s1 as character minus
         * NUL byte (or zero), which equals character.
         */
        if (0 != rc)
        {
            return(rc);
        }

        /*
         * First character of @b s1 past length of @b s2 
         */
        return((jbyte) s1[l2]);
    }
    else
    {
        /* If a difference existed, return it, else use end of @b s2 */
        /*
         * If a difference existed, return it, else use
         * the last character of @b s1 as NUL byte (or zero)
         * minus character, which equals negative of character.
         */
        if (0 != rc)
        {
            return(rc);
        }

        /* First character of @b s2 past length of @b s1 */
        return((jbyte) (0 - s2[l1]));
    }
} /* END of s1_s2_strncmp() */


/*!
 * @brief Compare two UTF strings from constant_pool, @b s1 minus @b s2
 *
 * @param s1   First of two UTF strings to compare
 *
 * @param s2   Second of two UTF strings to compare
 *
 * @returns lexicographical value of first difference in strings,
 *          else 0.
 *
 */
jbyte utf_utf_strcmp(CONSTANT_Utf8_info *s1, CONSTANT_Utf8_info *s2)
{
    /* Perform unified comparison of both UTF strings */
    return(s1_s2_strncmp(s1->bytes, s1->length, s2->bytes, s2->length));

} /* END of utf_utf_strcmp() */


/*!
 * @brief Compare contents of null-terminated string to contents of
 * a UTF string from a class file structure.
 *
 * @param  s1     Null-terminated string name
 *
 * @param  pcfs2  ClassFile where UTF string is found
 *
 * @param  cpidx2 Index in @b pcfs2 constant_pool of UTF string
 *
 *
 * @returns lexicographical value of first difference in strings,
 *          else 0.
 *
 */
jbyte utf_prchar_pcfs_strcmp(rchar                   *s1,
                             ClassFile               *pcfs2,
                             jvm_constant_pool_index  cpidx2)
{
    int l1 = strlen(s1);

    u1 *s2 = PTR_CP_THIS_STRNAME(pcfs2, cpidx2);

    int l2 = CP_THIS_STRLEN(pcfs2, cpidx2);

    /* Perform unified comparison of null-terminated vs UTF string */
    return(s1_s2_strncmp(s1, l1, s2, l2));

} /* END of utf_prchar_pcfs_strcmp() */


/*!
 * @brief Compare contents of UTF string to contents of a UTF string
 * from a class file structure.
 *
 * @param  s1     UTF string name
 *
 * @param  pcfs2  ClassFile where UTF string is found
 *
 * @param  cpidx2 Index in @b pcfs2 constant_pool of UTF string
 *
 *
 * @returns lexicographical value of first difference in strings,
 *          else 0.
 *
 */
jbyte utf_pcfs_strcmp(CONSTANT_Utf8_info      *s1,
                      ClassFile               *pcfs2,
                      jvm_constant_pool_index  cpidx2)
{
    u1 *s2 = PTR_CP_THIS_STRNAME(pcfs2, cpidx2);

    int l2 = CP_THIS_STRLEN(pcfs2, cpidx2);

    /* Perform unified comparison of null-terminated vs UTF string */
    return(s1_s2_strncmp(s1->bytes, s1->length, s2, l2));

} /* END of utf_pcfs_strcmp() */


/*!
 * @brief Common generic comparison, all parameters regularized.
 *
 * Compare a UTF or null-terminated string containing a
 * formatted or unformatted class name with an @e unformatted UTF
 * string from constant_pool.
 * Compare @b s1 minus @b s2, but skipping, where applicable,
 * the @b s1 initial BASETYPE_CHAR_L and the terminating
 * BASETYPE_CHAR_L_TERM, plus any array dimension modifiers.  The second
 * string is specified by a constant_pool index.  Notice that there
 * are @e NO formatted class string names in the (CONSTANT_Class_info)
 * entries of the constant_pool because such would be redundant.  (Such
 * entries @e are the @e formal definition of the class.)
 *
 *
 * @param s1     UTF string pointer to u1 array of characters.
 *
 * @param l1     length of @b s1.
 *
 * @param pcfs2  ClassFile structure containing second string
 *               (containing an @e unformatted class name)
 *
 * @param cpidx2 constant_pool index of CONSTANT_Class_info entry
 *               whose name will be compared (by getting its
 *               @link CONSTANT_Class_info#name_index name_index@endlink
 *               and the UTF string name of it)
 *
 *
 * @returns lexicographical value of first difference in strings,
 *          else 0.
 *
 */
static jbyte utf_common_classname_strcmp(u1                      *s1,
                                         int                      l1,
                                         ClassFile               *pcfs2,
                                         jvm_constant_pool_index cpidx2)
{
    CONSTANT_Class_info *pci = PTR_CP_ENTRY_CLASS(pcfs2, cpidx2);

    u1 *s2 = PTR_CP_THIS_STRNAME(pcfs2, pci->name_index);
    int l2 = CP_THIS_STRLEN(pcfs2, pci->name_index);

    if (rtrue == nts_prchar_isclassformatted(s1))
    {
        s1++; /* Point PAST the BASETYPE_CHAR_L character */
        l1--;

        u1 *ps1end = strchr(s1, BASETYPE_CHAR_L_TERM);

        /* Should @e always be @link #rtrue rtrue@endlink */
        if (rnull != ps1end)
        {
            l1 = ps1end - (u1 *) s1; /* Adjust for terminator */
        }
    }


    /*
     * Perform unified comparison of (possibly) null-terminated
     * vs UTF string
     */
    return(s1_s2_strncmp(s1, l1, s2, l2));

} /* END of utf_common_classname_strcmp() */


/*!
 * @brief Compare a null-terminated string containing a
 * formatted or unformatted class name with an @e unformatted UTF
 * string from constant_pool.
 *
 *
 * @param s1     Null-terminated string to compare, containing
 *               formatted @e or unformatted class name
 *               (utf_prchar_classname_strcmp() only).
 *
 * @param pcfs2  ClassFile structure containing second string
 *               (containing an @e unformatted class name)
 *
 * @param cpidx2 constant_pool index of CONSTANT_Class_info entry
 *               whose name will be compared (by getting its
 *               @link CONSTANT_Class_info#name_index name_index@endlink
 *               and the UTF string name of it)
 *
 *
 * @returns lexicographical value of first difference in strings,
 *          else 0.
 *
 */
jbyte utf_prchar_classname_strcmp(rchar                   *s1,
                                  ClassFile               *pcfs2,
                                  jvm_constant_pool_index  cpidx2)
{
    return(utf_common_classname_strcmp((u1 *) s1,
                                       strlen(s1),
                                       pcfs2,
                                       cpidx2));

} /* END of utf_prchar_classname_strcmp() */


/*!
 * @brief Compare a UTF string containing a
 * formatted or unformatted class name with an @e unformatted UTF
 * string from constant_pool.
 *
 *
 * @param s1     UTF string to compare, containing formatted @e or
 *               unformatted class name.
 *
 * @param pcfs2  ClassFile structure containing second string
 *               (containing an @e unformatted class name)
 *
 * @param cpidx2 constant_pool index of CONSTANT_Class_info entry
 *               whose name will be compared (by getting its
 *               @link CONSTANT_Class_info#name_index name_index@endlink
 *               and the UTF string name of it)
 *
 *
 * @returns lexicographical value of first difference in strings,
 *          else 0.
 *
 */
jbyte utf_classname_strcmp(CONSTANT_Utf8_info      *s1,
                           ClassFile               *pcfs2,
                           jvm_constant_pool_index  cpidx2)
{
    return(utf_common_classname_strcmp(s1->bytes,
                                       s1->length,
                                       pcfs2,
                                       cpidx2));

} /* END of utf_classname_strcmp() */


/*!
 * @brief Report the number of array dimensions prefixing a Java type
 * string.
 *
 * No overflow condition is reported since it is assumed that @b inbfr
 * is formatted with correct length.  Notice that because this logic
 * checks @e only for array specifiers and does not care about the rest
 * of the string, it may be used to evaluate field descriptions, which
 * will not contain any class formatting information.
 *
 * If there is even a @e remote possibility that more than
 * CONSTANT_MAX_ARRAY_DIMS dimensions will be found, compare
 * the result of this function with the result of utf_isarray().
 * If there is a discrepancy, then there was an overflow here.
 * Properly formatted class files will @e never contain code with
 * this condition.
 *
 * @note  This function is identical to nts_get_arraydims() except
 *        that it works on (CONSTANT_Utf8_info *) instead of (rchar *).
 *
 *
 * @param    inbfr   CONSTANT_Utf8_info string.
 *
 *
 * @returns  Number of array dimensions in string.  For example,
 *           this string contains three array dimensions:
 *
 *               @c @b [[[Lsome/path/name/filename;
 *
 *           If more than CONSTANT_MAX_ARRAY_DIMS are located, the
 *           result is zero-- no other error is reported.
 *
 */

jvm_array_dim utf_get_utf_arraydims(CONSTANT_Utf8_info *inbfr)
{
    /* Make return code wider than max to check overflow */
    u4 rc = 0;

    /* Start scanning at beginning of string */
    u1 *pclsname = (u1 *) &inbfr->bytes[0];

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

} /* END of utf_get_utf_arraydims() */


/*!
 * @brief Test whether or not a Java type string is an array or not.
 *
 *
 * @param    inbfr   CONSTANT_Utf8_info string.
 *
 *
 * @returns  @link #rtrue rtrue@endlink if this is an array
 *           specfication, else @link #rfalse rfalse@endlink.
 *
 */

rboolean utf_isarray(CONSTANT_Utf8_info *inbfr)
{
  return((BASETYPE_CHAR_ARRAY == (u1)inbfr->bytes[0]) ? rtrue : rfalse);

} /* END of utf_isarray() */


/*!
 * @brief Convert and an un-formatted class name UTF string (of the
 * type @c @b ClassName and not of type
 * @c @b [[[LClassName) from a (CONSTANT_Utf8_info *) into
 * a null-terminated string with Java class formatting items.  Result
 * is delivered in a heap-allocated buffer.  When done with result,
 * perform HEAP_FREE_DATA(result) to return that buffer to the heap.
 *
 * This function @e will work on formatted class names
 * @c @b [[[LClassName; and the difference is benign,
 * but that is not its purpose.
 *
 * @param  src   Pointer to UTF string, most likely from constant pool
 *
 * @returns Null-terminated string @c @b LClasSName; in heap
 *          or @link #rnull rnull@endlink if heap alloc error.
 *
 */

rchar *utf_utf2prchar_classname(CONSTANT_Utf8_info *src)
{
    /* Retrieve string from UTF data first */
    rchar *pstr = utf_utf2prchar(src);

    if (rnull == pstr)
    {
        return(pstr);
    }

    /* Allocate heap for formatted version */

    rchar *rc = HEAP_GET_DATA(sizeof(rchar) + /* Type specifier */
                              sizeof(rchar) + /* Type spec terminator */
                              sizeof(rchar) + /* NUL character */
                              src->length,    /* data */
                             rfalse);

    int pstrlen = strlen(pstr);
    rboolean isfmt  = nts_prchar_isclassformatted(pstr);

    if (rtrue == isfmt)
    {
        /*
         * Copy entire string plus NUL character into heap area,
         * ignoring excess allocation when formatting is @e added
         * to string.
         */
        memcpy(&rc[0], pstr, pstrlen);
        rc[pstrlen] = '\0';
    }
    else
    {
        /* Initial formatting */
        rc[0] = BASETYPE_CHAR_L;

        /* Copy to heap area */
        memcpy(&rc[1], pstr, pstrlen);

        /* Append end formatting and NUL character */
        rc[1 + pstrlen] = BASETYPE_CHAR_L_TERM;
        rc[2 + pstrlen] = '\0';
    }

    HEAP_FREE_DATA(pstr);


    /* Produce result */
    return(rc);

} /* END of utf_utf2prchar_classname() */


/*!
 * @brief Verify if a UTF string contains class formatting or not.
 *
 *
 * @param  src   Pointer to UTF string, most likely from constant pool
 *
 *
 * @returns @link #rtrue rtrue@endlink if string is formtted as
 *          @c @b LClasSName; but
 *          @link #rfalse rfalse@endlink otherwise, may also have
 *          array descriptor prefixed, thus @c @b [[LClassName;
 *
 *
 * @note  This function works just like nts_prchar_isclassformatted()
 *        except that it works on (CONSTANT_Utf8_info) strings rather
 *        than on (rchar *) strings.
 */

rboolean utf_utf_isclassformatted(CONSTANT_Utf8_info *src)
{
    jvm_utf_string_index utfidx;
    rboolean rc = rfalse;

    /* Chk array or class specifier.  If neither, cannot be formatted */
    switch (src->bytes[0])
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
    u1 *pbytes = src->bytes;

    for (utfidx = 0; utfidx < src->length; utfidx++)
    {
        if (BASETYPE_CHAR_L_TERM == pbytes[utfidx])
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
    jvm_array_dim arraydims = utf_get_utf_arraydims(src);

    /* If any array specs, look immediately past them for class spec */
    if (BASETYPE_CHAR_L == pbytes[arraydims])
    {
        return(rtrue);
    }
    else
    {
        return(rfalse);
    }

} /* END of utf_utf_isclassformatted() */


/*!
 *
 * @brief Strip a UTF string of any class formatting it contains
 * and return result in a heap-allocated buffer.
 *
 * When done with this result, perform HEAP_DATA_FREE(result) to
 * return buffer to heap.
 *
 *
 * @param  inbfr   Pointer to UTF string that is potentially formatted
 *                 as @c @b LClassName; and which may also have
 *                 array descriptor prefixed, thus
 *                 @c @b [[LClassName; .  This will
 *                 typically be an entry from the constant_pool.
 *
 *
 * @returns heap-allocated buffer containing @c @b ClassName
 *          with no formatting, regardless of input formatting or
 *          lack thereof.
 *
 *
 * @note  This function works just like
 *        nts_prchar2prchar_unformatted_classname() except that
 *        it takes a (CONSTANT_Utf8_info) string rather
 *        than a (rchar *) string and returns a (CONSTANT_Utf8_info *).
 *
 */

cp_info_dup *utf_utf2utf_unformatted_classname(cp_info_dup *inbfr)
{
    rchar *pstr = utf_utf2prchar(PTR_THIS_CP_Utf8(inbfr));

    rchar *punf = nts_prchar2prchar_unformatted_classname(pstr);

    HEAP_FREE_DATA(pstr);

    cp_info_dup *rc = nts_prchar2utf(punf);

    HEAP_FREE_DATA(punf);

    return(rc);

} /* END of utf_utf2utf_unformatted_classname() */


/* EOF */
