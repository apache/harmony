/*!
 * @file thread.c
 *
 * @brief Create and manage real machine Java thread structures.
 *
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
ARCH_COPYRIGHT_APACHE(thread, c, "$URL$ $Id$");


 
#include <strings.h>

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h" 
#include "gc.h" 
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h" 
#include "method.h" 
#include "util.h"
#include "utf.h"


/*!
 * @brief Initialize the thread area of the JVM model.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid thread_init()
{
    for (CURRENT_THREAD = jvm_thread_index_null;
         CURRENT_THREAD < JVMCFG_MAX_THREADS;
         CURRENT_THREAD++)
    {
        jvm_thread_index thridx = CURRENT_THREAD;

        /********** Init thread state ******************/

        THREAD(thridx).id = thridx;

        THREAD(thridx).name = HEAP_GET_DATA(THREAD_NAME_MAX_LEN,rfalse);
        sprintfLocal(THREAD(thridx).name, "thread %d", thridx);

        THREAD(thridx).thread_objhash = jvm_object_hash_null;

        THREAD(thridx).priority   = THREAD_PRIORITY_MIN;

        THREAD(thridx).status     = THREAD_STATUS_EMPTY;

        THREAD(thridx).prev_state = THREAD_STATE_DEAD;
        THREAD(thridx).this_state = THREAD_STATE_DEAD;
        THREAD(thridx).next_state = THREAD_STATE_DEAD;

        THREAD(thridx).jointarget = jvm_thread_index_null;
        THREAD(thridx).sleeptime  = 0;
        THREAD(thridx).locktarget = jvm_object_hash_null;


        /********** Init thread's code facilities ******/

        PUT_PC_IMMEDIATE(thridx,
                         jvm_class_index_null,
                         jvm_method_index_bad,
                         jvm_attribute_index_bad,
                         jvm_attribute_index_bad,
                         jvm_pc_offset_bad);

        THREAD(thridx).pass_instruction_count = 0;
        THREAD(thridx).thread_instruction_count = 0;

        THREAD(thridx).stack          = (jint *) rnull;
        THREAD(thridx).sp             = JVMCFG_NULL_SP;
        THREAD(thridx).fp             = JVMCFG_NULL_SP;
        THREAD(thridx).fp_end_program = JVMCFG_NULL_SP;

    } /* For CURRENT_THREAD */

    /*! @todo  What should the priority be on these threads? */
    /*
     * Forcibly allocate jvmcfg_thread_index_null thread,
     * system thread, and GC thread.  (These threads are
     * @e never deallocated.)
     */
    THREAD(jvm_thread_index_null).priority = THREAD_PRIORITY_MAX;
    THREAD(jvm_thread_index_null).status = THREAD_STATUS_INUSE |
                                           THREAD_STATUS_NULL;
    THREAD(jvm_thread_index_null).this_state = THREAD_STATE_NEW;
    THREAD(jvm_thread_index_null).next_state = THREAD_STATE_NEW;

    THREAD(JVMCFG_SYSTEM_THREAD).priority = THREAD_PRIORITY_MIN;
    THREAD(JVMCFG_SYSTEM_THREAD).status =   THREAD_STATUS_EMPTY;

    THREAD(JVMCFG_GC_THREAD).priority = THREAD_PRIORITY_MAX;
    THREAD(JVMCFG_GC_THREAD).status = THREAD_STATUS_INUSE;
    THREAD(JVMCFG_GC_THREAD).this_state = THREAD_STATE_NEW;
    THREAD(JVMCFG_GC_THREAD).next_state = THREAD_STATE_NEW;


    /* Last thread allocated */
    pjvm->thread_new_last = JVMCFG_GC_THREAD;

    /* First thread to run */
    CURRENT_THREAD = JVMCFG_SYSTEM_THREAD;

    /* Declare this module initialized */
    jvm_thread_initialized = rtrue;

    return;

} /* END of thread_init() */


/*!
 * @brief Look up table for names of thread states.
 *
 */
const rchar *thread_state_names[THREAD_STATE_MAX_STATE + 1 + 1] =
{
    THREAD_STATE_NEW_DESC,
    THREAD_STATE_START_DESC,
    THREAD_STATE_RUNNABLE_DESC,
    THREAD_STATE_RUNNING_DESC,
    THREAD_STATE_COMPLETE_DESC,
    THREAD_STATE_BLOCKINGEVENT_DESC,
    THREAD_STATE_BLOCKED_DESC,
    THREAD_STATE_UNBLOCKED_DESC,
    THREAD_STATE_SYNCHRONIZED_DESC,
    THREAD_STATE_RELEASE_DESC,
    THREAD_STATE_WAIT_DESC,
    THREAD_STATE_NOTIFY_DESC,
    THREAD_STATE_LOCK_DESC,
    THREAD_STATE_ACQUIRE_DESC,
    THREAD_STATE_DEAD_DESC,
    THREAD_STATE_BADLOGIC_DESC,

    /* For out of bounds indices, both <0 and >max */
    THREAD_STATE_ILLEGAL_DESC
};

/*!
 * @brief Map state numbers to state names.
 *
 *
 * @param  state   state number per @b THREAD_STATE_xxx definitions
 *
 * @returns string name of that state
 *
 */
const rchar *thread_state_get_name(rushort state)
{

/* Unsigned type makes this unnecessary **
    if (THREAD_STATE_MIN_STATE > state)
    {
        ** illegal state number **
        return(thread_state_names[THREAD_STATE_MAX_STATE + 1]);
    }
*/

    if (THREAD_STATE_MAX_STATE < state)
    {
        /* illegal state number */
        return(thread_state_names[THREAD_STATE_MAX_STATE + 1]);
    }

    return(thread_state_names[state]);

} /* END of thread_state_get_name() */


/*!
 * @brief Locate an unused thread table slot for a new thread.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns Thread table index of an empty slot.
 *          Throw error if no slots.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no thread slots are available@endlink.
 *
 */
static jvm_thread_index thread_allocate_slot(rvoid)
{
    jvm_thread_index thridx =
        (JVMCFG_MAX_THREADS == (1 + pjvm->thread_new_last))
        ? 1 + pjvm->thread_new_last
        : JVMCFG_FIRST_THREAD;

    /* Count allocated slots in case all slots are full */
    jvm_thread_index count  = 0;

    while(rtrue)
    {
        if (THREAD(thridx).status & THREAD_STATUS_INUSE)
        {
            /* Point to next slot, wrap around at end */
            thridx++;

            if (thridx == JVMCFG_MAX_THREADS)
            {
                thridx = JVMCFG_FIRST_THREAD;
            }

            /* Limit high value to end of table */
            if (pjvm->thread_new_last == JVMCFG_MAX_THREADS - 1)
            {
                pjvm->thread_new_last = JVMCFG_FIRST_THREAD;
            }


            /* Count this attempt and keep looking */
            count++;

            if (count == (JVMCFG_MAX_THREADS - JVMCFG_FIRST_THREAD))
            {
                /* No more slots, cannot continue */
                exit_throw_exception(EXIT_JVM_THREAD,
                                   JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
/*NOTREACHED*/
            }

            /* Keep looking */
            continue;
        }

        /*
         * Declare slot in use, but not initialized.
         */
        THREAD(thridx).status =
                               THREAD_STATUS_INUSE | THREAD_STATUS_NULL;


        /* This slot is empty, report it for allocation */

        pjvm->thread_new_last = thridx;

        return(thridx);
    }
/*NOTREACHED*/
    return(jvm_thread_index_null); /* Satisfy compiler */

} /* END of thread_allocate_slot() */


/*!
 * @name Thread allocation and loading.
 *
 * @brief Allocate a new thread from the thread area and load a class
 * to run on it.
 *
 * Activity on the system thread @link #JVMCFG_SYSTEM_THREAD
   JVMCFG_SYSTEM_THREAD@endlink will @e not attempt to load a
 * @c @b java.lang.Thread to represent a thread as an object.
 * This is because the system thread is for internal use only outside
 * the normal JVM runtime environment.
 *
 * As a variation, allocate the system thread
 * (@link #JVMCFG_SYSTEM_THREAD JVMCFG_SYSTEM_THREAD@endlink)
 * for an internal task.  This method-- thread_new_common()-- is the
 * common routine, where thread_new() is the general-purpose function
 * and thread_new_system() is for loading a class onto the system
 * thread.
 *
 * @warning Do @e not attempt to call this function
 *          with @b codeatridx set to @link #jvm_attribute_index_native
            jvm_attribute_index_native@endlink because this code
 *          does not apply to native methods.  Call that native
 *          method directly from the code instead!
 *
 * @param  thridx        Thread index of unused thread-- for
 *                         thread_new_common() only-- the others
 *                         generate this value and pass it in.
 *
 * @param  clsidx        Class index of code to run
 *
 * @param  mthidx        Method index of code to run
 *
 * @param  codeatridx    Method's attribute index of code to run
 *
 * @param  excpatridx    Method's attribute index of exception index tbl
 *
 * @param  priority      @link #THREAD_PRIORITY_NORM
                         THREAD_PRIORITY_xxx@endlink value for
 *                       thread priority
 *
 * @param  isdaemon      Daemon thread, @link #rtrue rtrue@endlink or
 *                       @link #rfalse rfalse@endlink
 *
 *
 * @returns index to thread slot containing this thread.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_STACKOVERFLOWERROR
 *         @link #JVMCLASS_JAVA_LANG_STACKOVERFLOWERROR
 *         if loading the class on this thread would overfill
 *         the JVM stack for this thread.@endlink.
 *         for this thread.
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no class slots or thread slots are available.@endlink.
 *
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Common function to load a class on a thread to be run.
 *
 * Given a thread index, load a method onto it to be run.
 * <em>Do not</em> use this function to load a method onto
 * a thread that has existing JVM execution active because
 * the stack is initialized and the method entry point loaded
 * into the PC.  See @link #opcode_run() opcode_run()@endlink
 * for examples of how to accomplish this in other ways.
 *
 */
static jvm_thread_index thread_new_common(jvm_thread_index    thridx,
                                          jvm_class_index     clsidx,
                                          jvm_method_index    mthidx,
                                         jvm_attribute_index codeatridx,
                                         jvm_attribute_index excpatridx,
                                          rint                priority,
                                          rboolean            isdaemon)
{
	
	
    /*
     * Declare slot in use, but not initialized.
     * (Redundant for most situations where
     * thread_allocate_slot() was called, but needed
     * for initializing classes like JVMCFG_NULL_THREAD
     * or JVMCFG_SYSTEM_THREAD with an absolute slot
     * number that was not searched for by the allocator.)
     */
    THREAD(thridx).status = THREAD_STATUS_INUSE | THREAD_STATUS_NULL;

    /* Check for stack overflow if this frame is loaded */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;
    Code_attribute *pca = (Code_attribute *)
                         &pcfs->methods[mthidx]->attributes[codeatridx]->ai;

    /* Check if this causes stack overflow, throw StackOverflowError */
    if (JVMCFG_MAX_SP <= (ruint)( GET_SP(thridx) +
                         JVMREG_STACK_MIN_FRAME_HEIGHT +
                         JVMREG_STACK_PC_HEIGHT +
                         pca->max_stack +
                         pca->max_locals))
    {
       sysDbgMsg(0,"thread.c", "thread_new_common() she's gonna blow! %d %d %d - %d %d\n",
       	thridx, JVMCFG_MAX_SP, (ruint)(GET_SP(thridx) +
                         JVMREG_STACK_MIN_FRAME_HEIGHT +
                         JVMREG_STACK_PC_HEIGHT +
                         pca->max_stack +
                         pca->max_locals), pca->max_stack, pca->max_locals);
       	
        exit_throw_exception(EXIT_THREAD_STACK,
                             JVMCLASS_JAVA_LANG_STACKOVERFLOWERROR);
/*NOTREACHED*/
    }

    /*!
     * If no stack overflow would occur during execution,
     * continue to load up a new thread.  Unless this class
     * will run on the system thread, start out by generating
     * a new @c @b java.lang.Thread object to represent
     * this thread in the object table.
     *
     * @todo  Need to also implement java.lang.ThreadGroup as a
     *        preparatory step to instantiating java.lang.Thread
     */
    if (JVMCFG_SYSTEM_THREAD == thridx)
    {
        THREAD(thridx).thread_objhash = jvm_object_hash_null;
    }
    else
    {
        jvm_class_index clsidxTHR =
            class_find_by_prchar(JVMCLASS_JAVA_LANG_THREAD);

        if (jvm_class_index_null == clsidxTHR)
        {
            /* unreserve thread and quit */
            THREAD(thridx).status = THREAD_STATUS_EMPTY;

            /* No more slots, cannot continue */
            exit_throw_exception(EXIT_JVM_THREAD,
                                 JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
/*NOTREACHED*/
        }

        jvm_table_linkage *ptl = CLASS_OBJECT_LINKAGE(clsidxTHR);

        jvm_object_hash objhashTHR =
            object_instance_new(OBJECT_STATUS_EMPTY,
                                ptl->pcfs,
                                ptl->clsidx,
                                LOCAL_CONSTANT_NO_ARRAY_DIMS,
                                (jint *) rnull,
                                rtrue,
                                thridx);

        THREAD(thridx).thread_objhash = objhashTHR;
        (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(jvm_object_hash_null,
                                         THREAD(thridx).thread_objhash);
    }

    /*
     * Only allocate stack to an allocated thread
     * (not in thread_init()), and thereafter keep
     * that allocation until thread_die().
     */
    if (rnull == THREAD(thridx).stack)
    {
        THREAD(thridx).stack =
            (jint *) HEAP_GET_STACK(JVMCFG_STACK_SIZE, rfalse);
    }

    /*
     * First two word of stack are reserved
     */
    PUT_SP_IMMEDIATE(thridx, JVMCFG_NULL_SP);

    /*
     * First 1 word meaningless in and of itself, but
     * when setting up the frame pointer for the first frame,
     * the fact that it is zero will mean that the inner
     * @c @b while() loop will not exit until a
     * @c @b return is done from the very last frame. 
     */
    PUT_SP_WORD(thridx, GET_SP(thridx), CLASSFILE_MAGIC);

    /*
     * FINAL fp MUST be @e zero (null SP) so CHECK_FINAL_STACK_FRAME()
     * works, if used.
     */
    PUT_FP_IMMEDIATE(thridx, GET_SP(thridx));

    /* with jvm_class_index_null class index, other fields meaningless*/
    PUT_PC_IMMEDIATE(thridx,
                     jvm_class_index_null,
                     jvm_method_index_bad,
                     jvm_attribute_index_bad,
                     jvm_attribute_index_bad,
                     jvm_pc_offset_bad);

    /*
     * Reserve first full stack frame, 1st amount of locals,
     * null FP, null PC.
     *
     * @note  jvmutil_print_stack_common() requires FP to hold a
     *        value of @link #JVMCFG_NULL_SP JVMCFG_NULL_SP@endlink
     *        as its terminating condition.  So also should @e any
     *        logic that scans the stack.  See in particular the
     *        @link #FIRST_STACK_FRAME() FIRST_STACK_FRAME()@endlink and
     *        @link #NEXT_STACK_FRAME() NEXT_STACK_FRAME()@endlink and
     *        @link #CHECK_FINAL_STACK_FRAME
              CHECK_FINAL_STACK_FRAME()@endlink macros.
     */
    PUSH_FRAME(thridx, pca->max_locals);

    /*
     * Stack frame now can accept operand stacking, including
     * requests for PUSH_FRAME(), which is used when calling
     * a JVM method or other subroutine.
     *
     * Upon final POP_FRAME(), the FP and PC will both be
     * empty, @link #JVMCFG_NULL_SP JVMCFG_NULL_SP@endlink and
     * @link #jvm_class_index_null jvm_class_index_null@endlink,
     * respectively.  At this point, the JVM thread
     * should stop running since there is nothing else
     * to do.
     *
     * Allocation complete, set priority and ISDAEMON status (as
     * requrested), and move next thread state to @b NEW, then
     * report to caller.
     */
    THREAD(thridx).priority = priority;
    threadstate_request_new(thridx);
    threadstate_activate_new(thridx);

    /*!
     * @todo Any time the isdaemon field is modified, MAKE SURE
     *       that the value in the @c @b java.lang.Thread
     *       instance variable is set to the same value.  This
     *       should @e never be an issue except when instantiating
     *       a new @c @b java.lang.Thread object since the
     *       API docs say that it can only be set @e once.
     */
    if (rtrue == isdaemon)
    {
        THREAD(thridx).status |= THREAD_STATUS_ISDAEMON;
    }

    /* Thread slot completely initialized */
    THREAD(thridx).status &= ~THREAD_STATUS_NULL;

    /* Load JVM program counter for first instruction */
    PUT_PC_IMMEDIATE(thridx,
                     clsidx,
                     mthidx,
                     codeatridx,
                     excpatridx,
                     CODE_CONSTRAINT_START_PC);

    return(thridx);

} /* END of thread_new_common() */


/*!
 * @brief Reserve the system thread and load a class to run on it.
 *
 * Reserve the system thread specifically-- see description
 * above for thread_new_common()
 *
 */
static jvm_thread_index thread_new_system(jvm_class_index     clsidx,
                                          jvm_method_index    mthidx,
                                         jvm_attribute_index codeatridx,
                                         jvm_attribute_index excpatridx,
                                          rint                priority,
                                          rboolean            isdaemon)
{
    /* Check if system thread is already in use */
    if (THREAD(JVMCFG_SYSTEM_THREAD).status & THREAD_STATUS_INUSE)
    {
        /* Somebody goofed */
        exit_throw_exception(EXIT_JVM_INTERNAL,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* If not already in use, reserve it and perform thread setup */
    return(thread_new_common(JVMCFG_SYSTEM_THREAD,
                             clsidx,
                             mthidx,
                             codeatridx,
                             excpatridx,
                             priority,
                             isdaemon));

} /* END of thread_new_system() */


/*!
 * @brief Allocate a new thread from the thread area and
 * load a class to run on it.
 *
 * General-purpuse version-- see description above for
 * thread_new_common()
 *
 */
static jvm_thread_index thread_new(jvm_class_index      clsidx,
                                   jvm_method_index     mthidx,
                                   jvm_attribute_index  codeatridx,
                                   jvm_attribute_index  excpatridx,
                                   rint                 priority,
                                   rboolean             isdaemon)
{
    jvm_thread_index thridx = thread_allocate_slot();

    /* Set up thread resources and finish */
    return(thread_new_common(thridx,
                             clsidx,
                             mthidx,
                             codeatridx,
                             excpatridx,
                             priority,
                             isdaemon));

} /* END of thread_new() */

/*@} */ /* End of grouped definitions */



/*!
 * @brief Load a class onto a thread during JVM initialization.
 *
 * Allocate a new thread and load a class (array or non-array),
 * prepare to invoke @c @b static methods, and prepare
 * to start thread.
 *
 * Classes loaded through this function will not be marked as
 * referenced, but will also not be marked for garbage collection,
 * either.
 *
 *
 * @param  clsname           Name of class to load.
 *
 * @param  mthname           Name of method in class.
 *
 * @param  mthdesc           Description of method parameters and
 *                           return type.
 *
 * @param  priority          THREAD_PRIORITY_xxx value for thread
 *                           priority.
 *
 * @param  isdaemon          Daemon thread, @link #rtrue rtrue@endlink
 *                           or @link #rfalse rfalse@endlink
 *
 * @param  usesystemthread   Allocate the system thread with
 *                           thread_new_system() instead of
 *                           a user thread via thread_new_system(),
 *                           @link #rtrue rtrue@endlink
 *                           or @link #rfalse rfalse@endlink.
 *
 * @param find_registerNatives When @link #rtrue rtrue@endlink,
 *                           will return the ordinal for
 *                           @link #JVMCFG_JLOBJECT_NMO_REGISTER 
                             JVMCFG_JLOBJECT_NMO_REGISTER@endlink and
 *                           @link #JVMCFG_JLOBJECT_NMO_UNREGISTER 
                             JVMCFG_JLOBJECT_NMO_UNREGISTER@endlink
 *                           as well as the other ordinals.  Once JVM
 *                           initialization is complete, this should
 *                           always be @link #rfalse rfalse@endlink
 *                           because all future classes should @e never
 *                           have local ordinals.
 *
 *
 * @returns Thread index of NEW thread, ready to move to START state,
 *          or throw error if no slots.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no class slots or thread slots are available.@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_CLASSNOTFOUNDEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_CLASSNOTFOUNDEXCEPTION
 *         if class cannot be located.@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         if the requested method is not found in the class
 *         or has no code area.@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_STACKOVERFLOWERROR
 *         @link #JVMCLASS_JAVA_LANG_STACKOVERFLOWERROR
 *         if loading the class on this thread would overfill
 *         the JVM stack for this thread.@endlink.
 *
 */
jvm_thread_index thread_class_load(rchar            *clsname,
                                   rchar            *mthname,
                                   rchar            *mthdesc,
                                   rint              priority,
                                   rboolean          isdaemon,
                                   rboolean          usesystemthread,
                                   rboolean        find_registerNatives)
{
     /*
     * Attempt to load requested class.  Notice that
     * an object is @e not being instantiated here,
     * so the @b arraylength parm (parm 3) can be
     * @link #rnull rnull@endlink.
     */
    jvm_class_index clsidx = class_load_from_prchar(clsname,
                                                   find_registerNatives,
                                                    (jint *) rnull);

     /* Point to @e this class structure, then get immediate superclass*/
    ClassFile *pcfs         = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;
    ClassFile *pcfs_recurse = pcfs;

    /*
     * Make special exception for @c @b java.lang.Object,
     * no superclass
     */
    if (CONSTANT_CP_DEFAULT_INDEX == pcfs_recurse->super_class)
    {
        pcfs_recurse = (ClassFile *) rnull;
    }
    else
    {
        pcfs_recurse =
            CLASS_OBJECT_LINKAGE(
                class_find_by_prchar(
                    PTR_CP1_CLASS_NAME_STRNAME(pcfs_recurse,
                                            pcfs_recurse->super_class)))
            ->pcfs;
    }

    /* Iterate through class table looking for superclasses */
    while(rnull != pcfs_recurse)
    {
        /* case (1), check if class is actually an interface */
        if (ACC_INTERFACE & pcfs_recurse->access_flags)
        {
            exit_throw_exception(EXIT_JVM_CLASS,
                       JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR);
/*NOTREACHED*/
        }

        /*
         * Take care of special case where
         * @link robject#super_class super_class@link is zero, namely
         * represents @c @b java.lang.Object, the only class
         * without a direct superclass.  This condition @e must be
         * the end of the scan.  If this condition is never met,
         * the possibility of a class circularity error is probably
         * almost 100%.
         */
        if (CONSTANT_CP_DEFAULT_INDEX == pcfs_recurse->super_class)
        {
            break;
        }

        /* case (2), check if class eventually references itself */
        if (0 == utf_prchar_classname_strcmp(
                     clsname,
                     pcfs_recurse,
                     pcfs_recurse->super_class))
        {
            /*
             * Don't permit recursive call to error handler
             * (this one should @e never happen)
             */
            if (0 == strcmp(clsname,
                            JVMCLASS_JAVA_LANG_CLASSCIRCULARITYERROR))
            {
                exit_throw_exception(EXIT_JVM_CLASS,
                              JVMCLASS_JAVA_LANG_CLASSCIRCULARITYERROR);
/*NOTREACHED*/
            }
        }

        /* Go get next higher superclass and scan again */
        pcfs_recurse =
            CLASS_OBJECT_LINKAGE(
                class_find_by_prchar(
                    PTR_CP1_CLASS_NAME_STRNAME(pcfs_recurse,
                                        pcfs_recurse->super_class)))
            ->pcfs;

    } /* while pcfs_recurse */

    /*
     * Locate starting PC in this class for this method with
     * this descriptor by scanning method[] table.
     * Since this function is @e never called from the JVM runtime,
     * the use of (rchar *) strings for mthname and mthdesc is
     * acceptable.  Normal usage would call method_find() directly
     * using pointers to UTF8 constant_pool entries.
     */

    jvm_method_index mthidx = method_find_by_prchar(clsidx,
                                                    mthname,
                                                    mthdesc);

    /* Failed to locate method name, throw ClassNotFoundException */
    if (jvm_method_index_bad == mthidx)
    {
        exit_throw_exception(EXIT_JVM_CLASS,
                             JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/* NOTREACHED*/
    }

    /* Since method name and description match, go run thread */
    jvm_attribute_index codeatridx =
        pcfs->methods[mthidx]->LOCAL_method_binding.codeatridxJVM;

    /* Load exception index table also, if present */
    jvm_attribute_index excpatridx =
        pcfs->methods[mthidx]->LOCAL_method_binding.excpatridxJVM;

    /* Load native method ordinal number, if applicable */
 /* jvm_native_method_ordinal nmord =
        pcfs->methods[mthidx]->LOCAL_method_binding.nmordJVM; */

    /* Typically will not happen-- if have valid method,also have code*/
    if (jvm_attribute_index_bad == codeatridx)
    {
        exit_throw_exception(EXIT_JVM_ATTRIBUTE,
                             JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/* NOTREACHED*/
    }

    /* Do not permit this operation for a native method request */
    if (jvm_attribute_index_native == codeatridx)
    {
        exit_throw_exception(EXIT_JVM_INTERNAL,
                             JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
    }
 
    /*
     * Everything was found.  Now allocate thread to run this
     * static method of this class.
     */
    if (rtrue == usesystemthread)
    {
        return(thread_new_system(clsidx,
                                 mthidx,
                                 codeatridx,
                                 excpatridx,
                                 priority,
                                 isdaemon));
    }
    else
    {
        return(thread_new(clsidx,
                          mthidx,
                          codeatridx,
                          excpatridx,
                          priority,
                          isdaemon));
    }

} /* END of thread_class_load() */


/*!
 * @brief Terminate and deallocate a thread that is currently in
 * the @b DEAD state.
 *
 *
 * @param thridx thread index of thread to be deallocated.
 *
 * @returns @link #rtrue rtrue@endlink if successful,
 *          else @link #rfalse rfalse@endlink.
 *
 */

rboolean thread_die(jvm_thread_index thridx)
{
    if (THREAD_STATE_DEAD == THREAD(thridx).this_state)
    {
        /*!
         * Mark ONLY fields requiring it, leave rest alone for
         * post-mortem use by developer.
         *
         * @todo  Could leave stack in place for post-mortem if
         *        it is assumed that there would be a reasonable
         *        amount of contents still valid in memory.
         */
        HEAP_FREE_STACK(THREAD(thridx).stack);
        THREAD(thridx).stack = (jint *) rnull;

        THREAD(thridx).status &= ~THREAD_STATUS_INUSE;

        return(rtrue);
    }

    return(rfalse);

} /* END of thread_die() */


/*!
 * @brief Shut down the thread area of the JVM model
 * at the end of JVM execution.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid thread_shutdown()
{
    jvm_thread_index thridx;

    for (thridx = jvm_thread_index_null;
         thridx < JVMCFG_MAX_THREADS;
         thridx++)
    {
        if (THREAD_STATUS_INUSE & THREAD(thridx).status)
        {
            /*
             * check stack frames only if stack area present
             */
            if (rnull != THREAD(thridx).stack)
            {
                /* Locate first (top) stack frame's FP */
                jvm_sp fp = FIRST_STACK_FRAME(thridx);

                /*
                 * First frame is @e greater than 0, and @e contains
                 * the NULL frame pointer.
                 */
                if (JVMCFG_NULL_SP == fp)
                {
                    continue;
                }

                /*
                 * Pop all stack frames, free local storage,
                 * clear object references, etc. (via POP_FRAME() macro)
                 */
                while(!CHECK_FINAL_STACK_FRAME_GENERIC(thridx, fp))
                {
                    /*
                     * Get next FP @e before popping that storage
                     * location off of the stack!
                     */
                    fp = NEXT_STACK_FRAME_GENERIC(thridx, fp);

                    POP_FRAME(thridx);
                }

                /*
                 * The current implementation does some of these steps
                 * internally, but be thorough so it does not break when
                 * the implementation changes.
                 */
                threadstate_request_badlogic(thridx);
                threadstate_activate_badlogic(thridx);
                threadstate_process_badlogic(thridx);
                threadstate_request_complete(thridx);
                threadstate_activate_complete(thridx);
                threadstate_process_complete(thridx);
                threadstate_request_dead(thridx);
                threadstate_activate_dead(thridx);
                threadstate_process_dead(thridx);
            }
        }

        /*
         * Keep thread name around usually
         * (not wiped out in thread_die() function)
         */
        if (rnull != THREAD(thridx).name)
        {
            HEAP_FREE_STACK(THREAD(thridx).name);
            /* THREAD(thridx).name = (rchar *) rnull; */
        }
    }

    /* This may result in a @e large garbage collection */
    GC_RUN(rfalse);

    /* Declare this module uninitialized */
    jvm_thread_initialized = rfalse;

    return;

} /* END of thread_shutdown() */


/*!
 * @brief Set up JVM thread-relative exception handler-- implements
 * @c @b setjmp(3)/longjmp(3).
 *
 *
 * @param thridx  Thread table index of thread for which to set up
 *                this exception handler.
 *
 *
 * @returns From normal setup, integer @link
            #THREAD_STATUS_EMPTY THREAD_STATUS_EMPTY@endlink.
 *          Otherwise, return error code passed in to
 *          @link #thread_throw_exception()
                   thread_throw_exception()@endlink as shown below.
 *
 *
 * @internal
 * @c @b setjmp(3) and @c @b struct[idx].member do _not_ get
 * along at all:
 *
 * @verbatim
      
       int nonlocal_rc =
                    setjmp(*THREAD(thridx).nonlocal_ThrowableEvent);
      
   @endverbatim
 *
 * To get this working, the @link rthread#pnonlocal_ThrowableEvent
 * nonlocal_ThrowableEvent@endlink had to be changed to a pointer
 * and a buffer allocated from heap.  This probably is due to the
 * @c @b typedef syntax of @c @b jmp_buf and a
 * long array, which pointers like.  However, instead of playing
 * games with addressing &element[0], a pointer works better because
 * not all @c @b jmp_buf implementations wil be a long array.
 *
 */

int thread_exception_setup(jvm_thread_index thridx)
{
    THREAD(thridx).pnonlocal_ThrowableEvent =
                                 HEAP_GET_DATA(sizeof(jmp_buf), rfalse);

    return(setjmp(*THREAD(thridx).pnonlocal_ThrowableEvent));

} /* END of thread_exception_setup() */
 

/*!
 * @brief Global handler setup for JVM thread errors and
 * exceptions-- implements @c @b setjmp(3).
 *
 *
 * @param thridx Thread index of thread where exception occurred.
 *
 * @param thread_status_bits Type of exception being generated,
 *          typically using one of the following exit codes from
 *          the status bits in
 *          @link jvm/src/thread.h thread.h@endlink
 *          per the definition there of each code:
 *
 * <ul><li> @link #THREAD_STATUS_THREW_EXCEPTION
                   THREAD_STATUS_THREW_EXCEPTION@endlink
 * </li>
 * <li>     @link #THREAD_STATUS_THREW_ERROR
                   THREAD_STATUS_THREW_ERROR@endlink
 * </li>
 * <li>     @link #THREAD_STATUS_THREW_THROWABLE
                   THREAD_STATUS_THREW_THROWABLE@endlink
 * </li>
 * <li>     @link #THREAD_STATUS_THREW_UNCAUGHT
                   THREAD_STATUS_THREW_UNCAUGHT@endlink
 * </li></ul>
 *
 * @param exception_name Null-terminated string name of error or
 *                       exception class to be invoked as a result
 *                       of this situation.
 *
 *
 * @returns non-local state restoration from setup via @c @b setjmp(3)
 *          as stored in @link
            #exit_LinkageError exit_LinkageError@endlink
 *          buffer by @link #exit_init() exit_init()@endlink
 *          in @link #jvm_init() jvm_init()@endlink before any of
 *          these errors could occur. All code invoking this
 *          function should use the standard @c @b lint(1) comment for
 *          "code not reached" as shown after the @c @b longjmp(3)
 *          function call in the source code of this function:
 *          <b>/</b><b>*NOTREACHED*</b><b>/</b>
 *
 */
rvoid thread_throw_exception(jvm_thread_index  thridx,
                             rushort           thread_status_bits,
                             rchar            *exception_name)
{
    /* Disallow invalid status from being reported */
    switch (thread_status_bits)
    {
        case THREAD_STATUS_THREW_EXCEPTION:
        case THREAD_STATUS_THREW_ERROR:
        case THREAD_STATUS_THREW_THROWABLE:
        case THREAD_STATUS_THREW_UNCAUGHT:
            break;
        default:
            exit_throw_exception(EXIT_JVM_INTERNAL,
                                 JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    THREAD(thridx).status |= thread_status_bits;

    THREAD(thridx).pThrowableEvent = exception_name;


    int rc = (int) thread_status_bits;

    longjmp(*THREAD(thridx).pnonlocal_ThrowableEvent, rc);
/*NOTREACHED*/

} /* END of thread_throw_exception() */


/* EOF */
