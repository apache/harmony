/*!
 * @file jlClass.c
 *
 * @brief Native implementation of @c @b java.lang.Class
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

#include "arch.h"
ARCH_COPYRIGHT_APACHE(jlClass, c, "$URL$ $Id$");


#include "jvmcfg.h"
#include "classfile.h"
#include "linkage.h"
#include "jvm.h"


/*!
 * @name Native implementation of class static functions.
 *
 * The class index of the current class is always passed
 * as the first parameter.
 *
 * @note These @c @b java.lang.Class methods are unusual in that
 * they does not require a @c @b jobject (in parlance of this
 * implementation, a @link #jvm_object_hash jvm_object_hash@endlink)
 * to run because they are declared as @c @b static methods.  As
 * implemented here, the usual @b objhashthis parameter is therefore
 * replaced by * @b clsidxthis.  The thread context is located in
 * @link #CURRENT_THREAD CURRENT_THREAD@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*@} */ /* End of grouped definitions */


/*!
 * @name Native implementation of object instance functions.
 *
 * The object hash of @c @b this object is always passed
 * as the first parameter.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Native implementation
 * of @c @b java.lang.Class.isArray()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if this class is an array,
 *          else @link #jfalse jfalse@endlink.
 *
 */

jboolean jlClass_isArray(jvm_object_hash objhashthis)
{
    jvm_class_index clsidx = OBJECT_CLASS_LINKAGE(objhashthis)->clsidx;

    if (jvm_class_index_null == clsidx)
    { 
        return(jfalse);
    }

    return((CLASS(clsidx).status & CLASS_STATUS_ARRAY)
           ? jtrue
           : jfalse);

} /* END of jlClass_isArray() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Class.isPrimative()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if this class is a primative,
 *          else @link #jfalse jfalse@endlink.
 *
 */

jboolean jlClass_isPrimative(jvm_object_hash objhashthis)
{
    jvm_class_index clsidx = OBJECT_CLASS_LINKAGE(objhashthis)->clsidx;

    if (jvm_class_index_null == clsidx)
    { 
        return(jfalse);
    }

    return((CLASS(clsidx).status & CLASS_STATUS_PRIMATIVE)
           ? jtrue
           : jfalse);

} /* END of jlClass_isPrimative() */

/*@} */ /* End of grouped definitions */


/* EOF */
