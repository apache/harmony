/*!
 * @file unicode.c
 *
 * @brief Manipulate Unicode (@link #jchar jchar@endlink)[]
 * character strings.
 *
 * There are three character string types in this program:
 * null-terminated @link #rchar (rchar)@endlink strings
 * @e ala 'C' language, UTF-8
 * @link #CONSTANT_Utf8_info (CONSTANT_Utf8_info)@endlink strings,
 * and Unicode @link #jchar (jchar)[]@endlink strings.
 *
 * Unicode (@link #jchar jchar@endlink) character utilities
 * that do @e not involve UTF8.
 *
 * ALL referenced to type (@link #jchar jchar@endlink) involve
 * Unicode characters throughout all of the code.  Manipulations
 * of them should take place @e only through these utilities.
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
ARCH_SOURCE_COPYRIGHT_APACHE(unicode, c,
"$URL$",
"$Id$");


#include <string.h>

#include "jvmcfg.h" 
#include "cfmacros.h"
#include "classfile.h"


/*
 *
 * Convert Unicode buffer into UTF8 buffer.
 *
 *
 * @param[in]  inbfr   Unicode character string
 *
 * @param[in]  length  Number of bytes in @b inbfr
 *
 * @param[out] outbfr  UTF8 byte string
 *
 *
 * @returns UTF8 structure containing length and character buffer (plus
 *          tag), but return in (cp_info_dup) for full proper word
 *          alignment. When done with the data, call HEAP_FREE_DATA()
 *          on it.
 *
 *    @c @b rc-\>bytes    UTF8 version of @b inbfr string in @b outbfr
 *
 *    @c @b rc-\>length   Number of UTF8 bytes in
 *                        @c @b rc-\>bytes.  This will
 *                        only be the same as input @b length
 *                        when ALL UTF8 characters are 7-bit
 *                        ASCII.  It will otherwise be less
 *                        than that.
 */

cp_info_dup *unicode_cnv2utf(jchar *inbfr, jshort length)
{
    ARCH_FUNCTION_NAME(unicode_cnv2utf);

    jshort bytecnvcount = 0;
    jshort unicodecnvcount;
    jubyte *outbfr;

    /*
     * Make two passes through input string, one for UTF8 length (for
     * proper heap allocation size), one for the conversion.
     *
     * Pass 1: calculate result size for heap allocation.  This is
     *         merely a stripped-down edition of pass 2, only
     *         incrementing input buffer pointer and byte count.
     */
    for (unicodecnvcount = 0;
         unicodecnvcount < length;
         unicodecnvcount++)
    {
        /* Process one-byte UTF8 conversion */
        if ((UTF8_SINGLE_MIN <= *inbfr) &&
            (UTF8_SINGLE_MAX >= *inbfr))
        {
            /*
             * Calculate a narrowing conversion,
             * but 9 MS bits are all zeroes, so no value change.
             */
            inbfr++;
            bytecnvcount++;
        }
        else
        {
            /* Calculate two-byte UTF8 conversion */
            if (((UNICODE_DOUBLE_MIN <= *inbfr) &&
                 (UNICODE_DOUBLE_MAX >= *inbfr))

            /* Also handle special case of NUL as two-byte character. */
                || (UNICODE_DOUBLE_NUL == *inbfr))
            {
                outbfr++;
                bytecnvcount++;

                inbfr++;
                bytecnvcount++;
            }
            else
            {
                /*
                 * Calculate three-byte UTF8 conversion-- all remaining
                 * cases, UNICODE_TRIPLE_MIN to UNICODE_TRIPLE_MAX
                 */
                bytecnvcount++;

                bytecnvcount++;

                inbfr++;
                bytecnvcount++;
            }
        }
    } /* for () */

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
                                        bytecnvcount,
                                    rfalse);

    /* Prepare output structure with everything but character cnv */
    CONSTANT_Utf8_info *pcpui = PTR_THIS_CP_Utf8(rc);
    pcpui->tag = CONSTANT_Utf8;
    pcpui->length = bytecnvcount;
    outbfr = (jubyte *) pcpui->bytes;

    /* Pass 2:  Perform conversion itself */
    bytecnvcount = 0;

    for (unicodecnvcount = 0;
         unicodecnvcount < length;
         unicodecnvcount++)
    {
        /* Process one-byte UTF8 conversion */
        if ((UTF8_SINGLE_MIN <= *inbfr) &&
            (UTF8_SINGLE_MAX >= *inbfr))
        {
            /*
             * Perform a narrowing conversion,
             * but 9 MS bits are all zeroes, so no value change.
             */
            *outbfr++ = UTF8_SINGLE_MASK0 & ((jbyte) (*inbfr++));
            bytecnvcount++;
        }
        else
        {
            /* Process two-byte UTF8 conversion */
            if (((UNICODE_DOUBLE_MIN <= *inbfr) &&
                 (UNICODE_DOUBLE_MAX >= *inbfr))

            /* Also handle special case of NUL as two-byte character. */
                || (UNICODE_DOUBLE_NUL == *inbfr))
            {
                *outbfr    = (*inbfr >> UTF8_DOUBLE_FIRST_SHIFT) &
                            UTF8_DOUBLE_FIRST_MASK0;
                *outbfr++ |= UTF8_DOUBLE_FIRST_VAL;
                bytecnvcount++;

                *outbfr    = (*inbfr++) & UTF8_DOUBLE_SECOND_MASK0;
                *outbfr++ |= UTF8_DOUBLE_SECOND_VAL;
                bytecnvcount++;
            }
            else
            {
                /*
                 * Process three-byte UTF8 conversion-- all remaining
                 * cases, UNICODE_TRIPLE_MIN to UNICODE_TRIPLE_MAX
                 */
                *outbfr    = (*inbfr >> UTF8_TRIPLE_FIRST_SHIFT) &
                            UTF8_TRIPLE_FIRST_MASK0;
                *outbfr++ |= UTF8_TRIPLE_FIRST_VAL;
                bytecnvcount++;

                *outbfr    = (*inbfr >> UTF8_TRIPLE_SECOND_SHIFT) &
                            UTF8_TRIPLE_SECOND_MASK0;
                *outbfr++ |= UTF8_TRIPLE_SECOND_VAL;
                bytecnvcount++;

                *outbfr    = (*inbfr++) & UTF8_TRIPLE_THIRD_MASK0;
                *outbfr++ |= UTF8_TRIPLE_THIRD_VAL;
                bytecnvcount++;
            }
        }
    } /* for () */

    return(rc);

} /* END of unicode_cnv2utf() */


/*!
 * @brief Compare two Unicode strings of any length, @b s1 minus @b s2
 *
 *
 * @param  s1   First string to compare
 *
 * @param  l1   Length of first string
 *
 * @param  s2   Second string to compare
 *
 * @param  l2   Length of second string
 *
 *
 * @returns lexicographical difference of <b><code>s1 - s2</code></b>.
 *          Notice that the (jchar) data is unsigned, the (jshort)
 *          result is signed, due to the arithmetic nature of the
 *          calculation.
 *
 */
jshort unicode_strcmp(jchar *s1, u2 l1, jchar *s2, u2 l2)
{
    ARCH_FUNCTION_NAME(unicode_strcmp);

    /* Compare shortest common run length */
    rint cmplen = (l1 < l2) ? l1 : l2;

    /* Perform Unicode strlen() function */
    rint i;
    jshort rc = 0;

    for (i = 0; i < cmplen; i++)
    {
        rc = s1[i] - s2[i];
        if (0 != rc)
        {
            break;
        }
    }

    /*
     * THIS LOGIC IS THE SAME AS FOR s1_s2_strncmp(), BUT
     * OPERATES ON (jchar) instead of (rchar).
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
        return((jshort) s1[l2]);
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
        return((jshort) (0 - s2[l1]));
    }
} /* END of unicode_strcmp() */


/* EOF */
