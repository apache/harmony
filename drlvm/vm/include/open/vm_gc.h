/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


#ifndef _OPEN_VM_GC_H
#define _OPEN_VM_GC_H

/**
 * @file
 * These are the functions that a GC built as a DLL may call.
 */


#include <stdio.h> 
#include "open/types.h"

#ifdef __cplusplus
extern "C" {
#endif



/**
 * @return The number of bytes allocated by VM in VTable
 *         for use by GC.
 */
VMEXPORT size_t vm_number_of_gc_bytes_in_vtable();

/**
 * @return The number of bytes allocated by VM in thread-local
 *         storage for use by GC.
 */
VMEXPORT size_t vm_number_of_gc_bytes_in_thread_local();

/**
 * @return The pointer to thread-local area of current thread.
 */
VMEXPORT void *vm_get_gc_thread_local();

/**
 * Initializes the lock that guards all GC-related operations in the VM.
 */
VMEXPORT void vm_gc_lock_init();

/**
 * Acquire the lock that guards all GC-related operations in the VM.
 * If the lock can't be acquired the thread waits until the lock is available.
 * This operation marks the current thread as being safe for root set
 * enumeration.
 */
VMEXPORT void vm_gc_lock_enum();

/**
 * Release the system-wide lock acquired by <code>vm_gc_lock_enum()</code>.
 * The thread is marked as unsafe for root set enumeration.
 */
VMEXPORT void vm_gc_unlock_enum();


/**
 * GC calls this function to command VM to start root set enumeration.
 *
 * Root set enumeration for all managed threads.
 */
VMEXPORT void vm_enumerate_root_set_all_threads();


/**
 * GC calls this function to restart managed threads after root set 
 * enumeration is complete.
 *
 * This function resumes all threads suspended by 
 * vm_enumerate_root_set_all_threads()
 */
VMEXPORT void vm_resume_threads_after();

/**
 * GC calls this function in stop the world state when all live objects
 * are marked. This is the callback to classloader allowing it to
 * gather needed statics for class unloading.
 * 
 * @sa gc interface functions: gc_get_next_live_object(void *iterator)
 */
VMEXPORT void vm_classloader_iterate_objects(void *iterator);

/**
 * GC calls this function during heap iteration to iterate
 * one object. The GC aborts heap iteration if this function
 * returns false.
 *
 * @return <code>TRUE</code> to continue heap iteration, <code>FALSE</code> to abort
 * 
 * @sa gc.h#gc_iterate_heap()
 */
VMEXPORT bool vm_iterate_object(Managed_Object_Handle object);

/**
 * GC calls this function to hint VM that finalizers may need to be run
 * and references enqueued. This method is guaranteed not to hold global
 * GC lock. 
 *
 * @note The function introduced as a workaround for running finalizers
 *       until a complete solution with finalizer thread is implemented.
 */
VMEXPORT void vm_hint_finalize();

VMEXPORT bool is_it_finalize_thread();

/**
 * thread state as concerns root set enumeration.
 */
enum safepoint_state {
    nill = 0,

    /** 
     * Thread is stopped for root set enumeration,
     * as is the whole world (all managed threads).
     */
    enumerate_the_universe,

    /** 
     * Thread is stopped for root set enumeration
     */
    java_suspend_one_thread,

    /**
     * Thread is stopped by java debugger.
     */
    java_debugger
};

/**
 * @return <code>TRUE</code> if no apparent trash was found in the object.
 * 
 * Used for debugging.
 */
VMEXPORT Boolean verify_object_header(void *ptr);

/**
 *  Routines to support lifecycle management of resources associated
 *  with a java object
 */

VMEXPORT void vm_notify_obj_alive(void *);
VMEXPORT void vm_reclaim_native_objs();


/*
 * *****
 * *
 * *  Routines to support finalization of objects.
 * * 
 * *****
 */

//@{
/**
 * GC should call this function when an object becomes
 * "f-reachable, finalizable"
 * The VM later finalizes those objects in a way that
 * is not part of this interface.
 *
 * VM must not call finalizer immediately to prevent
 * deadlocks in user code, because this functions
 * may be called during the stop-the-world phase.
 */
VMEXPORT void vm_finalize_object(Managed_Object_Handle p_obj);

VMEXPORT void set_native_finalizer_thread_flag(Boolean flag);

VMEXPORT void vm_heavy_finalizer_block_mutator(void);
//@}

/**
 * GC should call this function when an phantom reference object
 * is to be enqueued, i.e. when the reference is not reachable anymore.
 */
VMEXPORT void vm_enqueue_reference(Managed_Object_Handle p_obj);

VMEXPORT void set_native_ref_enqueue_thread_flag(Boolean flag);

/*
 * Returns handle of a class for a specified vtable
 *
 * @param vh - handle of vtable to retrieve class for
 *
 * @return class handle for a specified vtable
 */
VMEXPORT Class_Handle vtable_get_class(VTable_Handle vh);

/**
 * GC calls this function for each live object it finds in heap.
 * This is used for finding unreferenced class loaders for class
 * unloading.
 * Notifies VM that live object of this class was found in the heap
 *
 * @param clss - class of live object in Java heap
 */
VMEXPORT void vm_notify_live_object_class(Class_Handle clss);

#define CL_PROP_ALIGNMENT_MASK      0x00FFF     ///< @sa <code>class_properties</code>
#define CL_PROP_NON_REF_ARRAY_MASK  0x01000     ///< @sa <code>class_properties</code>
#define CL_PROP_ARRAY_MASK          0x02000     ///< @sa <code>class_properties</code>
#define CL_PROP_PINNED_MASK         0x04000     ///< @sa <code>class_properties</code>
#define CL_PROP_FINALIZABLE_MASK    0x08000     ///< @sa <code>class_properties</code>


/**
 * @section class_properties Class properties flags
 * 3322|2222|2222|1111|1111|1100|0000|0000
 * 1098|7654|3210|9876|5432|1098|7654|3210
 *                          ^^^^^^^^^^^^^^------ CL_PROP_ALIGNMENT_MASK
 *                        ^--------------------- CL_PROP_NON_REF_ARRAY_MASK
 *                       ^---------------------- CL_PROP_ARRAY_MASK
 *                      ^----------------------- CL_PROP_PINNED_MASK
 *                     ^------------------------ CL_PROP_FINALIZABLE_MASK 
 */


/**
 * extract the recursion counter from object lockword.
 */
#define P_RECURSION_BYTE(x)       ( (U_8 *)(((x)->get_obj_info_addr())) + 1 )  

#ifdef GC_PUBLIC_PRIVATE

/**
 * mask of recursion counter
 */
#define RECURSION_MASK 0x7f

/**
 * mask of recursion counter shifted to its position in lockword.
 */
#define RECURSION_MASK_SHIFTED 0x7f00
#define PUBLIC_PRIVATE_MASK 0x80
#endif /* #ifdef GC_PUBLIC_PRIVATE */


#ifdef __cplusplus
}
#endif

/**
 * \page vm_finalization_and_weak_refs Design of finalization and weak references in VM.
 *
 * @author Salikh Zakirov
 * @version written on 2005-05-31
 *
 * \section vm_finalization_interactions Object Finalization
 *
 * As described elsewhere, VM calls function gc_class_prepared()
 * as a mandatory step of class preparation process, in order that GC
 * have a chance to create its own class-specific structure (known as GCVT).
 * During this call, GC can call class_is_finalizable() to find out
 * whether the instances of this class require finalizers to be run.
 * The result is stored in GCVT for later use.
 *
 * At a later stage, when VM requests an object to be allocated by calling
 * gc_alloc(), or gc_alloc_fast(), GC consults its GCVT to find out whether
 * this class of objects needs to be finalized, and if so, adds the object
 * reference in the finalizable queue, maintained by the GC. Allocation 
 * of finalizable objects is guarded from being handled by the inlined
 * fast path by object size overloading hack (see \ref allocation).
 * This is needed due to the optimized nature of allocation fast path:
 * fast path assumes that objects don't need any special handling,
 * and we must ensure fast path fails for all object that do require
 * special handling such as finalization.
 *
 * Later, when the garbage is being collected, GC walks over the finalizable
 * object list and checks if the objects became eligible for finalization (i.e.
 * not reachable otherwise). The GC side of the story is described in more detail
 * in \ref gc_finalization_and_weak_refs Object chosen for finalization are then "revived"
 * and reported to the VM using vm_finalize_object(). Reviving is performed by
 * marking the object in order to prevent it from being collected before the
 * finalizer has been run. VM places all reported objects to its internal
 * finalization queue and runs the finalizers in a separate thread at a later
 * (unspecified) time.  Note, that while running finalization in a dedicated
 * thread is not directly required by the java specification, it is highly
 * desirable in order to improve overall VM robustness, because finalizers may
 * contain user code of arbitrary complexity, including synchronization with
 * other user threads. Thus, running finalizers as a step of garbage collection
 * process while other user threads are suspended will introduce a risk of
 * deadlock and thus must be avoided.
 *
 * As finalization queue stores direct references to java heap, it is
 * must be handled properly during heap compaction, by adding the locations
 * of the pointers to the list of the slots updated during compaction.
 *
 * \section vm_finalization_requirements Finalization requirements
 *
 * The process described above places following requirements
 * \li Finalizable objects must be allocated by calling into GC, that is 
 *     not by the inlined fast path.
 * \li vm_finalize_object() must defer running of finalizers to a later
 *     stage after the user java threads are resumed.
 *
 *
 * \section vm_weak_refs Weak references
 *
 * See the description of how weak references work in GC: 
 * \ref gc_finalization_and_weak_refs
 * 
 */



#endif // _OPEN_VM_GC_H
