#ifndef _jvalue_h_included_
#define _jvalue_h_included_

/*!
 * @file jvalue.h
 *
 * @brief Java aggregate type references for object definitions.
 *
 * See also <em>The Java Virtual Machine Specification,
 * version 2, Section 4</em>, table 4.2.
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
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
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */


ARCH_COPYRIGHT_APACHE(jvalue, h, "$URL$ $Id$");

/*!
 * @brief Java aggregate type references for object definitions.
 *
 * This union contains literally a grand union of all Java data types,
 * including both primatives and references types for the purpose of
 * allowing all object instance fields to be stored in @link
   robject#object_instance_field_data object_instance_field_data@endlink
 * as an array in an @link robject object table entry@endlink
 * without special treatment.  Static fields may be stored in this the
 * same way in
 * @link rclass#class_static_field_data class_static_field_data@endlink
 * of a
 * @link rclass class table entry@endlink.  (All sub-integer primative
 * data types are store inside of @link #jint (jint)@endlink and cast
 * in/out at runtime.)
 *
 * The following types are are casts of _jint, thus:
 * @verbatim
                          jvalue v;     ... a composite value

                          jint     i;   ... integer primative

                          jbyte    b;   ... sub-integer primatives
                          jboolean z;
                          jshort   s;
                          jchar    c;

                          jfloat   f;   ... float is same size as jint
                          jobjhash o;   ... object reference same size
  
                          jlong    l;   ... TWO jint words
                          jdouble  d;   ... TWO jint words
  
   (See also spec table 4.6)
  
                          i       = v._jint;

                          b       = v._jbyte;
                          z       = v._jboolean;
                          s       = v._jshort;
                          c       = v._jchar;

                          f       = v._jfloat;
                          o       = v._jobjhash;

                          l       = v._jlong;
                          d       = v._jlong;
  
   and vice versa:
  
                          v._jint     = i;

                          v._jbyte    = b;
                          v._jbyte    = b;
                          v._jboolean = z;
                          v._jshort   = s;
                          v._jchar    = c;

                          v._jfloat   = f;
                          v._jobjhash = o;

                          v._jlong    = l;
                          v._jdouble  = d;
   @endverbatim
 *
 * Although most of the items in this union are Java primatives, there
 * are also contained herein are two members that are @e not primatives,
 * namely the object reference hash and the array reference hash.
 * By implementing them here, both primative and reference,
 * @e all possible Java data types are represented in @e one data
 * structure, which is @e very handy for concise object representations
 * without @e any redundant data structures in different places.
 *
 * Notice that for @link #CONFIG_WORDWIDTH32 CONFIG_WORDWIDTH32@endlink
 * implementations, the @link #jlong (jlong)@endlink will be the
 * longest data type, as an 8-byte integer.  Notice @e also that for
 * @link #CONFIG_WORDWIDTH64 CONFIG_WORDWIDTH64@endlink implementations,
 * this will not change because there are no types such as pointers
 * that will change sizes here.  Since this typedef will be used
 * @e extensively in the runtime environment, this inherent constraint
 * can help plan maximum heap sizing.
 *
 */
typedef union
{
    jbyte    _jbyte;    /**< Sub-integer primative @link
                                   #jbyte jbyte@endlink */
    jboolean _jboolean; /**< Sub-integer primative @link
                                   #jboolean jboolean@endlink */
    jshort   _jshort;   /**< Sub-integer primative @link
                                   #jshort jshort@endlink */
    jchar    _jchar;    /**< Sub-integer primative @link
                                   #jchar jchar@endlink */

    jint             _jint;   /**< Primative @link #jint jint@endlink,
                                 per tables 4.2/4.6 */
    jlong            _jlong;  /**< Primative @link #jint jlong@endlink,
                                 per tables 4.2/4.6 */
    jfloat           _jfloat; /**< Primative @link #jint jfloat@endlink,
                                 per tables 4.2/4.6 */
    jdouble          _jdouble;/**<
                                 Primative @link #jint jdouble@endlink,
                                 per tables 4.2/4.6*/

    jvm_object_hash  _jstring;/**< Object hash for the quasi-primative
                                 @c @b java.lang.String .
                                 Except for this one item, table 4.6 is
                                 a subsest of table 4.2. */

    /*
     * Implementation of object references and array references.
     */
    jvm_object_hash  _jarray; /**< Object hash of next lower array dim*/
    jvm_object_hash  _jobjhash;/**< Object hash of an arbitrary object*/

} jvalue;

#endif /* _jvalue_h_included_ */


/* EOF */
