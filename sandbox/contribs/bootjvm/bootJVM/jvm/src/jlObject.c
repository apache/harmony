/*!
 * @file jlObject.c
 *
 * @brief Native implementation of @c @b java.lang.Object
 *
 * @todo  Perform intelligent check on input parameter
 *        @b objhash range for all functions.
 *
 * @todo  In real life, the @b objhashthis values and @b clsidxthis
 *        values will be valid or these functions could not be
 *        invoked since these data types are @e mandatory for
 *        referencing them.  This probably means that the parameter
 *        valididty checking could probably be relaxed.
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
ARCH_COPYRIGHT_APACHE(jlObject, c, "$URL$ $Id$");


#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"
#include "linkage.h"
#include "jvmclass.h"


/*!
 * @name Native implementation of class static functions.
 *
 * The class index of the current class is always passed
 * as the first parameter.
 *
 * @note These @c @b java.lang.Object methods are unusual in that
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
 * of @c @b java.lang.Object.getClass()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @c @b java.lang.Object of OBJECT(objhashthis)
 *
 */
jvm_object_hash jlObject_getClass(jvm_object_hash objhashthis)
{
    return(
        CLASS(OBJECT_CLASS_LINKAGE(objhashthis)->clsidx).class_objhash);

} /* END of jlObject_getClass() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.hashCode()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns input @b objhashthis by definition
 *
 */
jvm_object_hash jlObject_hashCode(jvm_object_hash objhashthis)
{
    return(objhashthis);

} /* END of jlObject_hashCode() */


/*!
 * @name Native implementations of java.lang.Object.wait() functions.
 *
 * @brief Implementation of related functions
 * @c @b java.lang.Object.wait() and
 * @c @b java.lang.Object.wait(jlong) .
 *
 * If this thread is not @link #THREAD_STATUS_INUSE
   THREAD_STATUS_INUSE@endlink, result is @link #jfalse jfalse@endlink.
 * Results are undefined if thread has the @b SLEEP, @b JOIN4EVER,
 * @b JOINTIMED, or @b INTERRUPTIBLEIO status or if thread has
 * been @b NOTIFIED or @b INTERRUPTED.
 *
 * This will only succeed if thread is in @b RUNNING state.
 *
 * It will fail of thread did not hold the object's monitor lock
 * so it could release it here.
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 * @param  sleeptime   Number of timer ticks (milliseconds) to sleep.
 *
 *
 * @returns @link #jvoid jvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
           if another thread had interrupted this thread@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_ILLEGALMONITORSTATEEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_ILLEGALMONITORSTATEEXCEPTION
       if current thread does not own the object's monitor lock@endlink.
 *
 *
 * @todo Make sure thread interruption logic below here is working.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Wait until object monitor lock is released.
 *
 */

jvoid jlObject_wait4ever(jvm_object_hash objhashthis)
{
    if (((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
         (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))   &&
        (OBJECT_STATUS_MLOCK & OBJECT(objhashthis).status)            &&
        (CURRENT_THREAD == (OBJECT(objhashthis).mlock_thridx)))
    {
        jvm_thread_index thridx =
                              OBJECT_CLASS_LINKAGE(objhashthis)->thridx;

        THREAD(thridx).status |= THREAD_STATUS_WAIT4EVER;
        (rvoid) objectutil_release(objhashthis, thridx);

        return;
    }

    /* This thread does not own the object's monitor lock */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                       JVMCLASS_JAVA_LANG_ILLEGALMONITORSTATEEXCEPTION);
/*NOTREACHED*/
    return; /* Satisfy compiler */

} /* END of JlObject_wait4ever() */


/*!
 * @brief Wait until object monitor lock is released or a timeout
 * period has expired.
 *
 */

jvoid jlObject_waittimed(jvm_object_hash objhashthis,
                         jlong           sleeptime)
{
    if (((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
         (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))   &&
        (OBJECT_STATUS_MLOCK & OBJECT(objhashthis).status)            &&
        (CURRENT_THREAD == (OBJECT(objhashthis).mlock_thridx)))
    {
        jvm_thread_index thridxthis =
                              OBJECT_CLASS_LINKAGE(objhashthis)->thridx;
        THREAD(thridxthis).status |= THREAD_STATUS_JOINTIMED;
        THREAD(thridxthis).sleeptime = sleeptime;
        (rvoid) objectutil_release(objhashthis, thridxthis);

        return;
    }

    /* This thread does not own the object's monitor lock */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                       JVMCLASS_JAVA_LANG_ILLEGALMONITORSTATEEXCEPTION);
/*NOTREACHED*/
    return; /* Satisfy compiler */

} /* END of JlObject_waittimed() */

/*@} */ /* End of grouped definitions */


#if 0
/*!
 * @brief Native implementation of @c @b java.lang.Object.clone()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns object hash of object clone.
 *
 *          @todo then need to throw SecurityException in
 *          outer JVM loop when @link #jfalse jfalse@endlink.
 *
 */

jvm_object_hash jlObject_clone(jvm_object_hash objhashthis)
{
    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        /* @todo Need to finish this implementation */

        return(jvm_object_hash_null);
    }
    return(jvm_object_hash_null);

} /* END of jlObject_clone() */
#endif

/*@} */ /* End of grouped definitions */



/* EOF */
