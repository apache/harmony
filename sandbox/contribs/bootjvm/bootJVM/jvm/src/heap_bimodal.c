/*!
 * @file heap_bimodal.c
 *
 * @brief @b bimodal heap management functions
 *
 * This is the second of two implementations of heap management.
 * It extends the simple malloc/free scheme by adding a single
 * large allocation for requests smaller tha a certain size.
 * This was added due to an apparent internal limit in @c @b malloc(3)
 * or perhaps a kernel limit that after a certain number of
 * calls and/or bytes of allocation and freeing, returns NULL
 * for no apparent reason.
 *
 * The common header file @link jvm/src/heap.h gc.h@endlink defines
 * the prototypes for all heap allocation implementations by way
 * of the @link #CONFIG_HEAP_TYPE_SIMPLE CONFIG_HEAP_TYPE_xxx@endlink
 * symbol definitions.
 *
 *
 * Here is a note taken from the original project
 * @link ./README README@endlink file:
 *
 *     <i>(16) When nearing the end of the initial development, I ran
 *     across what is probably a memory configuration limit on my
 *     Solaris platform, which I did not bother to track down, but
 *     rather work around.  It seems that when calling malloc(3C) or
 *     malloc(3MALLOC), after 2,280 malloc() allocations and 612 free()
 *     invocations, there is something under the covers that does a
 *     @b SIGSEGV, and it can happen in either routine.  I therefore
 *     extended the heap mechanism to allocate 1M slots of 'n' bytes
 *     for small allocations up to this size.  Everything else still
 *     uses malloc().  In this way, I was able to finish development
 *     on the JVM and release it to the ASF in a more timely manner.
 *     In other words, I will let the team fix it!  I am not sure that
 *     the real project wants a static 'n + 1' MB data area just
 *     hanging around the runtime just because I did not take time to
 *     tune the system configuration!</i>
 *
 * This modified algorithm makes exactly two @b malloc() calls, one for
 * an array of fixed size slots of (@link #rbyte rbyte@endlink)
 * and the other for an array of (@link #rboolean rboolean@endlink)
 * for @link #rtrue rtrue@endlink/@link #rfalse rfalse@endlink on
 * whether a fixed-size memory slot is in use or not.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/heap_bimodal.c $ \$Id: heap_bimodal.c 0 09/28/2005 dlydick $
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
ARCH_COPYRIGHT_APACHE(heap_bimodal, c, "$URL: https://svn.apache.org/path/name/heap_bimodal.c $ $Id: heap_bimodal.c 0 09/28/2005 dlydick $");

#if defined(CONFIG_HEAP_TYPE_BIMODAL) || defined(CONFIG_COMPILE_ALL_OPTIONS)


#include <errno.h>
#include <stdlib.h>

#include "jvmcfg.h"
#include "exit.h"
#include "gc.h"
#include "heap.h"
#include "jvmclass.h"
#include "util.h"


/*!
 * @brief Small heap allocation area.
 *
 * For allocations up to @b n bytes, use this instead of system
 * allocation and thus minimize number of calls to @b malloc()
 * and @b free().  After 2,280 calls to @b malloc(3C) and 612 to
 * @b free(3C), the library would kick out a @b SIGSEGV for no apparent
 * reason.  Use of @b malloc(3MALLOC) and @b free(3MALLOC) did
 * the same thing, with @b -lmalloc.  Use of @b -lbsdmalloc was
 * at an even lower number.  Therefore, it seems best to delay the
 * issue (See quotation above from @link ./README README@endlink file.)
 *
 * The value of the @link #HEAP_SLOT_SIZE HEAP_SLOT_SIZE@endlink
 * definition <b>ABSOLUTELY MUST BE A MULTIPLE of 4!!! </b>
 * (prefer 8 for future 64-bit implementation).  This is
 * <b><code>sizeof(rvoid *)</code></b> and must be such to always
 * fundamentally avoid @b SIGSEGV on 4-byte accesses.
 */
#define HEAP_SLOT_SIZE       160

/*!
 * @brief Number of slots of this size.
 *
 * Any number of slots is possible, up to the reasonable
 * resource limits of the machine.
 */
#define HEAP_NUMBER_OF_SLOTS 1048576


/*!
 * @brief Pointer for physical allocation for slots in use.
 */
static rboolean *pheap_slot_in_use;

/*!
 * @brief Pointer for physical allocation of heap block to manage.
 */
static rbyte *pheap_slot;

/*!
 * @brief Start up heap management methodology.
 *
 * In a malloc/free scheme, there is nothing
 * to do, but here, the two blocks @link #pheap_slot_in_use@endlink
 * and @link #pheap_slot pheap_slot@endlink must be allocated and
 * initialized.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *        @return @link #rvoid rvoid@endlink
 *
 */
rvoid heap_init_bimodal()
{
    rlong heapidx;

    /* Set up slot flags */
    pheap_slot_in_use =
        malloc(sizeof(rboolean) * HEAP_NUMBER_OF_SLOTS);

    if (rnull == pheap_slot_in_use)
    {
        sysErrMsg("heap_init", "Cannot allocate slot flag storage");
        exit_jvm(EXIT_HEAP_ALLOC);
/*NOTREACHED*/
    }

    /* Initialize flag array to @link #rfalse rfalse@endlink */
    for (heapidx = 0;
         heapidx < HEAP_NUMBER_OF_SLOTS;
         heapidx++)
    {
        pheap_slot_in_use[heapidx] = rfalse;
    }


    /* Set up slot storage itself, do not need to initialize */
    pheap_slot =
        malloc(sizeof(rbyte) *
        HEAP_SLOT_SIZE *
        HEAP_NUMBER_OF_SLOTS);

    if (rnull == pheap_slot)
    {
        free(pheap_slot_in_use);

        sysErrMsg("heap_init", "Cannot allocate slot storage");
        exit_jvm(EXIT_HEAP_ALLOC);
/*NOTREACHED*/
    }

    /* Declare this module initialized */
    jvm_heap_initialized = rtrue;

    return;

} /* END of heap_init_bimodal() */


/*!
 * @brief Most recent error code from @c @b malloc(3), for use
 * by heap_get_error_bimodal().
 */
static int heap_last_errno = ERROR0;


/*!
 * @brief Number of calls to @c @b malloc(3).
 *
 * One of four global variables providing rudimentary statistics
 * for heap allocation history.
 *
 * @see heap_free_count
 * @see malloc_free_count
 * @see slot_free_count
 */
static rlong heap_malloc_count = 0;

/*!
 * @brief Number of calls to @c @b free(3).
 *
 * One of four global variables providing rudimentary statistics
 * for heap allocation history.
 *
 * @see heap_malloc_count
 * @see malloc_free_count
 * @see slot_free_count
 */
static rlong heap_free_count   = 0;

/*!
 * @brief Number of allocations made from pheap_slot.
 *
 * One of four global variables providing rudimentary statistics
 * for heap allocation history.
 *
 * @see heap_malloc_count
 * @see heap_free_count
 * @see slot_free_count
 */
static rlong slot_alloc_count  = 0;

/*!
 * @brief Number of allocations freed from pheap_slot.
 *
 * One of four global variables providing rudimentary statistics
 * for heap allocation history.
 *
 * @see heap_malloc_count
 * @see heap_free_count
 * @see slot_malloc_count
 */
static rlong slot_free_count   = 0;


/*!
 * @brief Original heap allocation method that uses
 * @e only @c @b malloc(3) and @c @b free(3).
 *
 *
 * @param size         Number of bytes to allocate
 *
 * @param clrmem_flag  Set memory to all zeroes
 *                     (@link #rtrue rtrue@endlink) or not
 *                     (@link #rfalse rfalse@endlink).
 *                     If @link #rtrue rtrue@endlink,
 *                     clear the allocated block, otherwise
 *                     return it with its existing contents.
 *
 *
 * @return (@link #rvoid rvoid@endlink *) to allocated area.
 *         This pointer may be cast to any desired data type.  If
 *         size of zero bytes is requested, return
 *         @link #rnull rnull@endlink and let caller croak
 *         on @b SIGSEGV.  If no memory is available
 *         or some OS system call error happened, throw error,
 *         but do @e not return.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no memory is available@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         if other allocation error@endlink.
 *
 */
static rvoid *heap_get_common_simple_bimodal(int size,
                                             rboolean clrmem_flag)
{
    rvoid *rc;

    rc = malloc(size);

    /*
     * If specific errors are returned, GC could free up some heap,
     * so run it and try again-- ONCE.  If it fails a second time,
     * so be it.  Let the application deal with the problem.
     */
    if (rnull == rc)
    {
        switch(errno)
        {
            case ENOMEM:
            case EAGAIN:
                GC_RUN(rtrue);
                rc = malloc(size);

                if (rnull == rc)
                {
                    switch(errno)
                    {
                        case ENOMEM:
                        case EAGAIN:
                            exit_throw_exception(EXIT_HEAP_ALLOC,
                                   JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
/*NOTREACHED*/
                        default:
                            /*
                             * Preserve errno for later inspection.
                             * By doing it this way, other OS system
                             * calls will not interfere with its value
                             * and it can be inspected at leisure.
                             */
                            heap_last_errno = errno;

                            exit_throw_exception(EXIT_HEAP_ALLOC,
                                      JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
                    }
                }
                break;

            default:
                /*
                 * Preserve errno for later inspection.
                 * By doing it this way, other OS system
                 * calls will not interfere with its value
                 * and it can be inspected at leisure.
                 */
                heap_last_errno = errno;

                exit_throw_exception(EXIT_HEAP_ALLOC,
                                     JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
        }
    }

    /* Clear block if requested */
    if (rtrue == clrmem_flag)
    {
        rbyte *pb = (rbyte *) rc;

        int i;
        for (i = 0; i < size; i++)
        {
            pb[i] = '\0';
        }
    }

    heap_malloc_count++;

    return(rc);

} /* END of heap_get_common_simple_bimodal() */


/*!
 * @brief Allocate memory from heap to caller, judging which mode
 * to use for allocation.
 *
 * When finished, this pointer should be sent back to
 * @link #heap_free_data_bimodal() heap_free_xxxx_bimodal()@endlink
 * for reallocation.
 *
 * @warning  Much of the JVM initialization ABSOLUTELY DEPENDS on
 *           setting of the @b clrmem_flag value to
 *           @link #rtrue rtrue@endlink so that allocated
 *           structures contain all zeroes.  If the heap
 *           allocation scheme changes, this functionality needs
 *           to be brought forward or change much of the code, not
 *           only init code, but throughout the whole corpus.
 *
 * @remarks  If @c @b malloc(3) returns an error other than out of
 *           memory errors, then the system @b errno is saved out
 *           into @link #heap_last_errno heap_last_errno@endlink
 *           for retrieval by @c @b perror(3) or other user response.
 *           This is typically useful for system-level debugging
 *           when the OS or OS resources, security, etc., may be
 *           getting in the way of proper allocation.
 *
 *
 * @param   size         Number of bytes to allocate
 *
 * @param   clrmem_flag  Set memory to all zeroes
 *                       (@link #rtrue rtrue@endlink) or
 *                       not (@link #rfalse rfalse@endlink).
 *                       If @link #rtrue rtrue@endlink,
 *                       clear the allocated block, otherwise
 *                       return it with its existing contents.
 *
 *
 * @return (@link #rvoid rvoid@endlink *) to allocated area.
 *         This pointer may be cast to any desired data type.  If
 *         size of zero bytes is requested, return
 *         @link #rnull rnull@endlink and let caller croak
 *         on @b SIGSEGV.  If no memory is available
 *         or some OS system call error happened, throw error,
 *         but do @e not return.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no memory is available@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         if other allocation error@endlink.
 *
 */
static rvoid *heap_get_common_bimodal(int size, rboolean clrmem_flag)
{
    rvoid *rc; /* Separate LOCATE_SLOT calc. from return() for debug */
    rc = (rvoid *) rnull;

    /*
     * Return rnull pointer when zero size requested.
     * Let caller fix that problem.
     */
    if (0 == size)
    {
        return((rvoid *) rnull);
    }

    errno = ERROR0;   /* Clear out error code before calling */


    /* Use pre-allocated area for small requests */
    if (HEAP_SLOT_SIZE >= size)
    {
        /* Mark last allocated-- faster than always starting at 0 */
        static rlong heapidxLAST = HEAP_NUMBER_OF_SLOTS - 1;

        rlong heapidx;  /* Just in case of large number of slots */
        rlong count;

        /* Scan flag array for first open slot */

        /* Study heap twice, before and after GC,and use same code*/
#define LOCATE_SLOT                                                \
                                                                   \
        for (count = 0, heapidx = 1 + heapidxLAST;                 \
             count < HEAP_NUMBER_OF_SLOTS;                         \
             count++, heapidx++)                                   \
        {                                                          \
            /* Wrap around last allocation to beginning */         \
            if (HEAP_NUMBER_OF_SLOTS == heapidx)                   \
            {                                                      \
                heapidx = 0;                                       \
            }                                                      \
                                                                   \
            if (rfalse == pheap_slot_in_use[heapidx])              \
            {                                                      \
                /* Reserve a slot, return its data area pointer */ \
                pheap_slot_in_use[heapidx] = rtrue;                \
                                                                   \
                /* Also report which slot was last allocated */    \
                heapidxLAST = heapidx;                             \
                                                                   \
                /* Count slot allocations */                       \
                slot_alloc_count++;                                \
                                                                   \
                rc = (rvoid *)                                     \
                     &pheap_slot[heapidx *                         \
                                 sizeof(rbyte) *                   \
                                 HEAP_SLOT_SIZE];                  \
                /* Clear block if requested */                     \
                if (rtrue == clrmem_flag)                          \
                {                                                  \
                    rbyte *pb = (rbyte *) rc;                      \
                                                                   \
                    rint i;                                        \
                    for (i = 0; i < size; i++)                     \
                    {                                              \
                        pb[i] = '\0';                              \
                    }                                              \
                }                                                  \
                break;                                             \
            }                                                      \
        }

        LOCATE_SLOT;
        if (rnull != rc)
        {
            return(rc);
        }

        /* If could not allocate, do one retry after GC */
        GC_RUN(rtrue);

        /* Scan flag array a second time for first open slot */

        LOCATE_SLOT;

        if (rnull != rc)
        {
            return(rc);
        }

        /* Sorry, nothing available, throw error */

        heap_last_errno = ERROR0; /* No OS error */

        exit_throw_exception(EXIT_HEAP_ALLOC,
                             JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
    }
    else
    {
        return(heap_get_common_simple_bimodal(size, clrmem_flag));
    }
/*NOTREACHED*/
    return((rvoid *) rnull); /* Satisfy compiler */

} /* END of heap_get_common_bimodal() */


/*!
 * @brief Allocate memory for a @b method from heap to caller.
 *
 * When finished, this pointer should be sent back to
 * @link #heap_free_method_bimodal() heap_free_method_bimodal()@endlink
 * for reallocation.
 *
 * @remarks This implementation makes no distinction betwen
 *          "method area heap" and any other usage.  Other
 *          implementations may choose to implement the
 *          JVM Spec section 3.5.4 more rigorously.
 *
 *
 * @param   size         Number of bytes to allocate
 *
 * @param   clrmem_flag  Set memory to all zeroes
 *                       (@link #rtrue rtrue@endlink) or
 *                       not (@link #rfalse rfalse@endlink)
 *
 *
 * @return (@link #rvoid rvoid@endlink *) to allocated area.
 *         This pointer may be cast to any desired data type.  If
 *         size of zero bytes is requested, return
 *         @link #rnull rnull@endlink and let
 *         caller croak on @b SIGSEGV.  If no memory is available
 *         or some OS system call error happened, throw error,
 *         but do @e not return.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no memory is available@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         if other allocation error@endlink.
 *
 */
rvoid *heap_get_method_bimodal(int size, rboolean clrmem_flag)
{
    return(heap_get_common_bimodal(size, clrmem_flag));

} /* END of heap_get_method_bimodal() */


/*!
 * @brief Allocate memory for a @b stack area from heap to caller.
 *
 * When finished, this pointer should be sent back
 * to @link #heap_free_stack_bimodal() heap_free_stack_bimodal()@endlink
 * for reallocation.
 *
 *
 * @param   size         Number of bytes to allocate
 *
 * @param   clrmem_flag  Set memory to all zeroes
 *                       (@link #rtrue rtrue@endlink) or
 *                       not (@link #rfalse rfalse@endlink)
 *
 *
 * @return (@link #rvoid rvoid@endlink *) to allocated area.
 *         This pointer may be cast to any desired data type.  If
 *         size of zero bytes is requested, return
 *         @link #rnull rnull@endlink and let
 *         caller croak on @b SIGSEGV.  If no memory is available
 *         or some OS system call error happened, throw error,
 *         but do @e not return.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no memory is available@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         if other allocation error@endlink.
 *
 *
 *
 */
rvoid *heap_get_stack_bimodal(int size, rboolean clrmem_flag)
{
    return(heap_get_common_bimodal(size, clrmem_flag));

} /* END of heap_get_stack_bimodal() */


/*!
 * @brief Allocate memory for a @b data area from heap to caller.
 *
 * When finished, this pointer should be sent back
 * to @link #heap_free_data_bimodal() heap_free_data_bimodal()@endlink
 * for reallocation.
 *
 *
 * @param   size         Number of bytes to allocate
 *
 * @param   clrmem_flag  Set memory to all zeroes
 *                       (@link #rtrue rtrue@endlink) or
 *                       not (@link #rfalse rfalse@endlink)
 *
 *
 * @return (@link #rvoid rvoid@endlink *) to allocated area.
 *         This pointer may be cast to any desired data type.  If
 *         size of zero bytes is requested, return
 *         @link #rnull rnull@endlink and let
 *         caller croak on @b SIGSEGV.  If no memory is available
 *         or some OS system call error happened, throw error,
 *         but do @e not return.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no memory is available@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         if other allocation error@endlink.
 *
 *
 *
 */
rvoid *heap_get_data_bimodal(int size, rboolean clrmem_flag)
{
    return(heap_get_common_bimodal(size, clrmem_flag));

} /* END of heap_get_data_bimodal() */


/*********************************************************************/
/*!
 * @brief Release a previously allocated block back into the heap for
 * future reallocation.
 *
 * If a @link #rnull rnull@endlink pointer is passed in, ignore
 * the request.
 *
 *
 * @param  pheap_block  An (@link #rvoid rvoid@endlink *) previously
 *                      returned by one of the
 *                      @link #heap_get_data_bimodal()
                        heap_get_XXX_bimodal()@endlink functions.
 *
 *
 * @return @link #rvoid rvoid@endlink
 *
 *
 */
static rvoid heap_free_common_bimodal(rvoid *pheap_block)
{
    /* Ignore @link #rnull rnull@endlink pointer */
    if (rnull != pheap_block)
    {
        /*
         * Free pre-allocated area from deallocation of
         * small requests, all of which were allocated
         * in this block
         */
        if ((((rbyte *) &pheap_slot[0]) <= ((rbyte *) pheap_block))
            &&
            (((rbyte *)
              &pheap_slot[sizeof(rbyte)  *
                          HEAP_SLOT_SIZE *
                          HEAP_NUMBER_OF_SLOTS]) >
                                           ((rbyte *) pheap_block)))
        {
            rlong heapidx =
                (((rbyte *) pheap_block) - &pheap_slot[0]) /
                HEAP_SLOT_SIZE;
   
            pheap_slot_in_use[heapidx] = rfalse;

            /* Count slots freed */
            slot_free_count++;

            return;
        }
        else
        {
            /* Free larger requests */
            heap_free_count++;

            free(pheap_block);

            return;
        }
    }

    return;

} /* END of heap_free_common_bimodal() */


/*!
 * @brief Release a previously allocated @b method block back into
 * the heap for future reallocation.
 *
 * @remarks  This implementation makes no distinction between
 *           <b>method area heap</b> and any other usage.  Other
 *           implementations may choose to implement the
 *           JVM Spec section 3.5.4 more rigorously.
 *
 *
 * @param  pheap_block  An (@link #rvoid rvoid@endlink *) previously
 *                      returned by @link #heap_get_method_bimodal()
                        heap_get_method_bimodal()@endlink
 *
 *
 * @return @link #rvoid rvoid@endlink
 *
 */
rvoid heap_free_method_bimodal(rvoid *pheap_block)
{
    heap_free_common_bimodal(pheap_block);

} /* END of heap_free_method_bimodal() */


/*!
 * @brief Release a previously allocated @b stack block back into
 * the heap for future reallocation.
 *
 *
 * @param  pheap_block  An (@link #rvoid rvoid@endlink *) previously
 *                      returned by @link #heap_get_stack_bimodal()
                        heap_get_stack_bimodal()@endlink
 *
 *
 * @return @link #rvoid rvoid@endlink
 *
 */
rvoid heap_free_stack_bimodal(rvoid *pheap_block)
{
    heap_free_common_bimodal(pheap_block);

} /* END of heap_free_stack_bimodal() */


/*!
 * @brief Release a previously allocated @b data block back
 * into the heap for future reallocation.
 *
 *
 * @param  pheap_block  An (@link #rvoid rvoid@endlink *) previously
 *                      returned by @link #heap_get_data_bimodal()
                        heap_get_data_bimodal()@endlink
 *
 *
 * @return @link #rvoid rvoid@endlink
 *
 */
rvoid heap_free_data_bimodal(rvoid *pheap_block)
{
    heap_free_common_bimodal(pheap_block);

} /* END of heap_free_data_bimodal() */


/*!
 * @brief Allocation failure diagnostic.
 *
 * Returns an @b errno value per @b "errno.h" if a
 * @link #rnull rnull@endlink pointer
 * is passed in, namely from the most recent call to a heap
 * allocation function.  It may only be called once before the
 * value is cleared.  If a non-null pointer is passed in,
 * @link #ERROR0 ERROR0@endlink is returned and the error status is
 * again cleared.
 *
 *
 * @param   badptr   Return value from heap allocation function.
 *
 *
 * @returns @link #ERROR0 ERROR0@endlink when no error was found
 *          or non-null @b badptr given.
 *          @link #heap_last_errno heap_last_errno@endlink
 *          value otherwise.
 *
 */
int  heap_get_error_bimodal(rvoid *badptr)
{
    int rc;

    if (rnull == badptr)
    {
        rc = heap_last_errno;
        heap_last_errno = ERROR0;
        return(rc);
    }
    else
    {
        heap_last_errno = ERROR0;

        return(ERROR0);
    }

} /* END of heap_get_error_bimodal() */


/*!
 * @brief Shut down up heap management after JVM execution is finished.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *        @return @link #rvoid rvoid@endlink
 *
 */
rvoid heap_shutdown_bimodal()
{
    heap_last_errno = ERROR0;

    /* Declare this module uninitialized */
    jvm_heap_initialized = rfalse;

    return;

} /* END of heap_shutdown_bimodal() */

#endif /* CONFIG_HEAP_TYPE_BIMODAL || CONFIG_OPTIONS_COMPILE_ALL */


/* EOF */
