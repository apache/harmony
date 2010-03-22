/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#ifndef _OPEN_GC_H
#define _OPEN_GC_H

/**
 * @file
 * GC interface exposed to VM.
 *
 * These are the functions that a GC built as a DLL must export.
 * Some functions may be optional and are marked as such.
 *
 * This is a global include file which provides to the VM an interface to the
 * GC. This interface is the only supported interface that the VM should call
 * to talk to the GC. All routines in this C interface will begin with <code>gc_</code>.
 *
 * The GC expects that there is a <code>vm_gc.h</code> file holding the only  
 * interface that the GC will use to talk to the VM.
 *
 * In order to eliminate dependency on certain types such as (VTable *) we 
 * have eliminated them from this interface and replace them with (void *).
 * While this might appear to be unfortunate it allows us to eliminate any 
 * knowledge of the class and VTable structures that are not of interest 
 * to the GC.
 */

#include "open/types.h"


#ifdef __cplusplus
extern "C" {
#endif



/**
 * <code>GCExport</code> is used to declare functions exported by GC.
 */
#ifndef PLATFORM_POSIX
#ifdef BUILDING_VM
#define GCExport
#else /* #ifdef BUILDING_VM */
#define GCExport __declspec(dllexport)
#endif /* #ifdef BUILDING_VM */
#else
#define GCExport
#endif /* #ifndef PLATFORM_POSIX */

#define BITS_PER_BYTE 8

// Signed arithmetic is used when we do allocation pointer/limit compares.
// In order to do this all sizes must be positive so when we want to overflow
// instead of setting the high bit we set the next to high bit. If we set the
// high bit and the allocation buffer is at the top of memory we might not
// detect an overflow the unsigned overflow would produce a small positive
// number that is smaller then the limit.

#define NEXT_TO_HIGH_BIT_SET_MASK (1<<((sizeof(unsigned) * BITS_PER_BYTE)-2))
#define NEXT_TO_HIGH_BIT_CLEAR_MASK ~NEXT_TO_HIGH_BIT_SET_MASK

#if defined(USE_GC_STATIC) || defined(BUILDING_GC)

/** 
 * @name Routines to support the initialization and termination of GC
 */
//@{

/**
 * Is called by VM to start GC initialization sequence.
 *
 * This function is expected to initialize the GC internal data structures.
 * The VM should call this *before* any other calls to this interface
 * The GC assumes that the VM is ready to support a GC if it 
 * calls this function.
 */
GCExport int gc_init();

/**
 * May be called at various points the VM decides are GC-safe.
 * The GC may ignore this, or it may force a root set enumeration, or it may
 * execute a full GC.
 *
 * @note Optional debug interface.
 */
GCExport void gc_test_safepoint();




/**
 * If the GC supports a "bump-the-pointer" style allocation, where the GC's
 * thread-local information contains a "current" pointer and a "limit" pointer,
 * then it should return <code>TRUE</code>, and it should set <code>*offset_of_current</code>
 * to be the offset into the GC thread block of the "current" pointer, and similar for
 * <code>*offset_of_limit</code> and the "limit" pointer. If not, then it should return
 * <code>FALSE</code>.
 */
GCExport Boolean gc_supports_frontier_allocation(unsigned *offset_of_current, unsigned *offset_of_limit);

/**
 * This API is used by the VM to notify the GC that the
 * VM has completed bootstrapping and initialization, and 
 * is henceforth ready to field requests for enumerating 
 * live references.
 *
 * Prior to this function being called the GC might see some
 * strange sights such as <code>NULL</code> or incomplete vtables. The GC will
 * need to consider these as normal and work with the VM to ensure 
 * that bootstrapping works. This means that the GC will make few
 * demands on the VM prior to this routine being called.
 *
 * However, once called the GC will feel free to do 
 * stop-the-world collections and will assume that the entire
 * <code>gc_import.h</code> interface is available and fully functioning.
 *
 * If this routine is called twice the result is undefined.
 */
GCExport void gc_vm_initialized();

/**
 * This is called once the VM has no use for the heap or the 
 * garbage collector data structures. The assumption is that the 
 * VM is exiting but needs to give the GC time to run destructors 
 * and free up memory it has gotten from the OS.
 * After this routine has been called the VM can not relie on any
 * data structures created by the GC.
 *
 * Errors: If <code>gc_enumerate_finalizable_objects</code> has been called and
 *         <code>gc_wrapup</code> gc discovers an object that has not had it
 *         finalizer run then it will attempt to report an error.
 */
GCExport void gc_wrapup();

/**
 * Is called by the VM to enumerate the root reference.
 */
GCExport void gc_add_root_set_entry(Managed_Object_Handle *ref, Boolean is_pinned);

/**
 * Resembles <code>gc_add_root_set_entry()</code> but is passed the address of a slot
 * containing a compressed reference.
 */
GCExport void gc_add_compressed_root_set_entry(U_32 *ref, Boolean is_pinned);

/**
 * Is called by the VM to enumerate weak root reference.
 *
 * @param slot          - a pointer to the slot, containing the weak root
 * @param is_pinned     - <code>TRUE</code> denotes that object pointed-to from this slot
 *                        should not be moved during garbage collection
 * @param is_short_weak - <code>TRUE</code> means that the weak root must be cleared
 *                        before object becomes eligible for finalization
 */
GCExport void gc_add_weak_root_set_entry(Managed_Object_Handle *slot, 
    Boolean is_pinned, Boolean is_short_weak);

/**
 * Enumerate a managed pointer.  
 * The pointer can be declared as pinned. The pointer can
 * point to the managed heap or any other area where data can be stored: stack
 * or static fields. It is the responsibility of the GC to ignore pointers
 * that are not in the managed heap.
 *
 * @note Is this function needed for Java? -salikh
 */
GCExport void gc_add_root_set_entry_managed_pointer(void **slot,
                                                    Boolean is_pinned);

/**
 * Call from the VM to the gc to enumerate an interior pointer. <code>**ref</code> is a
 * slot holding a pointer into the interior of an object. The base of the
 * object is located at <code>*ref</code> - offset. The strategy employed is to place the
 * slot, the object base and the offset into a <code>slot_base_offset</code> table. We then
 * call <code>gc_add_root_set_entry</code> with the slot in the table holding the base of
 * the object. Upon completion of the garbage collection the routine
 * <code>fixup_interior_pointers</code> is called and the <code>slot_base_offset</code> table is
 * traversed and the new interior pointer is calculated by adding the base of
 * the object and the offset. This new interior pointer value is then placed
 * into the slot.
 *
 * This routine can be called multiple times with the same interiour pointer
 * without any problems. The offset is checked to make sure it is positive but
 * the logic is not dependent on this fact.
 *
 * @note Optional function, never called by Java virtual machine.
 */
GCExport void gc_add_root_set_entry_interior_pointer (void **slot, int offset, Boolean is_pinned);
 



/*
 * *****
 * *
 * *  Routines to support the allocation and initialization of objects.
 * * 
 * *****
 */

/**
 * @page allocation Allocation of objects.
 *
 * There is a tension between fast allocation of objects and 
 * honoring various constraints the VM might place on the object. 
 * These constraints include registering the objects for 
 * finalization, aligning the objects on multiple word boundaries, 
 * pinning objects for performance reasons, registering objects 
 * related to weak pointers and so forth.
 *
 * We have tried to resolve this tension by overloading the 
 * size argument that is passed to the allocation routine. If 
 * the size of the argument has a high bit of 0, then the 
 * allocation routine will assume that no constraints exist 
 * on the allocation of this object and allocation can potentially 
 * be made very fast. If on the other hand the size is large then 
 * the routine will query the class data structure to determine 
 * what constraints are being made on the allocation of this object.
 *
 * The gc_import.h interface will provide the following masks
 * to allow the gc to quickly determine the constraints.
 * <PRE>
 *      CL_PROP_NON_REF_ARRAY_MASK 0x1000
 *      CL_PROP_ARRAY_MASK         0x2000
 *      CL_PROP_PINNED_MASK        0x4000
 *      CL_PROP_FINALIZABLE_MASK   0x8000
 *      CL_PROP_ALIGNMENT_MASK     0x0FFF
 * </PRE>
 */

/**
 * This routine is the primary routine used to allocate objects. 
 * It assumes nothing about the state of the VM internal data 
 * structures or the runtime stack. If gc_malloc_or_null is able 
 * to allocate the object without invoking a GC or calling the VM
 * then it does so. It places p_vtable into the object, ensures 
 * that the object is zeroed and then returns a ManagedObject 
 * pointer to the object. If it is not able to allocate the object 
 * without invoking a GC then it returns NULL.
 *
 * @param size            - the size of the object to allocate. If the high bit
 *                          set then various constraints as described above are
 *                          placed on the allocation of this object.
 * @param type            - a pointer to the vtable of the class being 
 *                          allocated. This routine will place this value 
 *                          in the appropriate slot of the new object.
 * @param thread_pointer  - a pointer to the GC's thread-local space
 *
 * This is like <code>gc_malloc_or_null</code>, except that it passes a pointer to
 * the thread's GC-specific space as a third argument. This prevents
 * the GC from having to immediately call <code>vm_get_thread_curr_alloc_block()</code>
 * as its first task.
 *
 * @note Rename of <code>gc_malloc_with_thread_pointer()</code>.
 */
GCExport Managed_Object_Handle gc_alloc_fast(unsigned size, 
                                             Allocation_Handle type,
                                             void *thread_pointer);

/**
 * This routine is used to allocate an object. See the above 
 * discussion on the overloading of size. {link allocation}
 *
 * @param size           - the size of the object to allocate. If the high bit
 *                         set then various constraints as described above are
 *                         placed on the allocation of this object.
 * @param type           - a pointer to the vtable of the class being allocated.
 *                         This routine will place this value in the 
 *                         appropriate slot of the new object.
 * @param thread_pointer - a pointer to the GC's thread-local space
 * 
 * @note Rename of <code>gc_malloc_or_null_with_thread_pointer()</code>.
 */
GCExport Managed_Object_Handle gc_alloc(unsigned size, 
                                        Allocation_Handle type,
                                        void *thread_pointer);


/**
 * For bootstrapping situations, when we still don't have
 * a class for the object. This routine is only available prior to 
 * a call to the call <code>gc_vm_initialized<code>. If it is called after
 * the call to <code>gc_vm_initialized</code> then the results are undefined. 
 * The GC places <code>NULL</code> in the vtable slot of the newly allocated
 * object.
 * 
 * The object allocated will be pinned, not finalizable and not an array.
 *
 * @param size - the size of the object to allocate. The high bit
 *               will never be set on this argument.
 *
 * @return The newly allocated object.
 *
 * @note Will be renamed to <code>gc_alloc_pinned_noclass()</code> to comply with 
 *       accepted naming conventions.
 */
GCExport Managed_Object_Handle gc_pinned_malloc_noclass(unsigned size);

/**
 * Allocate pinned forever object 
 *
 * @note Not implemented.
 */
GCExport Managed_Object_Handle gc_alloc_pinned(unsigned size, Allocation_Handle type, void *thread_pointer);


//@}
/** @name Routines to support write barriers
 */
//@{


/**
 * @return <code>TRUE</code> if the GC requires write barriers before every store to
 *         a field of a reference type.
 */
GCExport Boolean gc_requires_barriers();

//@}
/** @name Routines to support threads
 */
//@{


/**
 * This routine is called during thread startup to set
 * an initial nursery for the thread.
 *
 * @note <code>gc_thread_init</code> and <code>gc_thread_kill</code> assume that
 *       the current thread is the one we are interested in. If we passed in the 
 *       thread then these things could be cross inited and cross killed.
 */
GCExport void gc_thread_init(void *gc_information);

/**
 * This is called just before the thread is reclaimed.
 */
GCExport void gc_thread_kill(void *gc_information);

/** 
 * Opaque handle for threads.
 */
typedef void* Thread_Handle;     

/**
 * GC may call this function asynchronously any time it wants to get thread list.
 * This function signals VM to obtain thread lock and start thread iteration.
 *
 * \li vm obtains thread lock
 * \li vm repeatedly calls <code>gc_iterate_thread(thread)</code>
 * \li vm releases thread lock
 *
 * @note Not implemented.
 */
VMEXPORT void vm_iterate_threads();
 
/**
 * VM calls this method repeatedly to iterate over the list of java threads,
 * initiated earlier by calling <code>vm_iterate_threads()</code>.
 *
 * Thread creation and termination is locked during this iteration.
 *
 * gc may do one of the following:
 * 1. store thread handle for later use 
 * 2. enumerate thread right now, while
 *    holding thread lock (using <code>vm_suspend_thread(thread)</code> and
 *    <code>vm_enumerate_thread_root_set(thread)</code>).
 *
 * @note Not implemented.
 */
GCExport void gc_iterate_thread(Thread_Handle thread);
 
 
/**
 * GC calls this method to request VM to
 * suspend an individual thread.
 * After the thread is suspended, 
 * 0 is returned on success
 *
 * Thread may have been terminated already,
 * in this case non-zero value is returned,
 * and no additional actions are taken.
 *
 * GC calls this VM function when it wants a thread
 * to be suspended for stack enumeration or 
 * read/write barrier change.
 * 
 * blocks until synchronously call <code>gc_thread_suspended(thread)</code> 
 * or asynchronously delegate enumeration to thread
 * (self-enumeration)
 *
 * @note We need a way to signal that process of thread suspension
 *       is complete.
 *
 * @note Not implemented.
 */
VMEXPORT void vm_suspend_thread(Thread_Handle thread);

/**
 * VM calls this GC callback when it's accomplished the requested
 * operation of suspending a thread in gc-safe point
 *
 * May be called synchronously from the same context
 * as <code>vm_suspend_thread()</code> in case of cross-enumeration, or
 * may be called asynchronously from the specified
 * thread context in case of self-enumeration.
 *
 * After this function completes, 
 * the thread is resumed automatically.
 *
 * GC is expected to call a limited subset 
 * of GC-VM interface functions from this callback:
 * \li <code>vm_enumerate_thread_root_set(thread)</code>
 * \li <code>vm_install_write_barrier(...)</code>  
 *     (hypothetical, not designed yet)
 * \li make a thread stack snapshot for later analysis
 *
 * @note Not implemented.
 */
GCExport void gc_thread_suspended (Thread_Handle thread);

/**
 * GC calls this function to command VM to enumerate a thread,
 * which was earlier suspenden using <code>vm_suspend_thread()</code>.
 *
 * In response to this call, VM repeatedly calls <code>gc_add_root_set_entry()</code> to
 * enumerate thread stacks and local handles
 *
 * @note Not implemented.
 */
VMEXPORT void vm_enumerate_thread_root_set(Thread_Handle thread);

/**
 * GC calls this function to command VM to enumerate global slots.
 *
 * During enumeration of global root set, either all threads need 
 * to be suspended, or write barrier installed.
 *
 * Apparently some operations should be blocked in VM, like class loading,
 * which itself creates new global reference slots.
 * It is not clear to me if we should require stopping the world to use
 * this function or introduce new system-wide lock on operations that
 * change the number of global reference slots.
 *
 * This function calls <code>gc_add_root_set_entry()</code> for all global reference
 * slots.
 *
 * @note Not implemented.
 */
VMEXPORT void vm_enumerate_global_root_set();
//@}
/** @name Routines to support the functionality required by the Java language specification
 */
//@{

/**
 * API for the VM to force a GC, typically in response to a call to 
 * <code>java.lang.Runtime.gc</code>.
 */
GCExport void gc_force_gc();



/**
 * API for the VM to determine the current GC heap size, typically in response to a
 * call to <code>java.lang.Runtime.totalMemory</code>.
 */
GCExport int64 gc_total_memory();

/**
 * API for the VM to determine the maximum GC heap size, typically in response to a
 * call to <code>java.lang.Runtime.maxMemory</code>.
 */
GCExport int64 gc_max_memory();


/**
 * API for the VM to get an approximate view of the free space, 
 * typically in response to a call to <code>java.lang.Runtime.freeMemory</code>.
 */
GCExport int64 gc_free_memory();


/**
 * @return <code>TRUE</code> if the object is pinned.
 *
 * Routine to support the functionality required by JNI to see if an object is pinned.
 */
GCExport Boolean gc_is_object_pinned (Managed_Object_Handle obj);


/*
 * *****
 * *
 * *  Routines to handle the GC area in the VTable.
 * * 
 * *****
 */

/**
 * The VM calls this function after a new class has been prepared.
 * The GC can use a call interface to gather relevant information about
 * that class and store it in area of the VTable that is reserved for GC.
 * The information cached in the VTable should be used by the GC in
 * performance sensitive functions like object scanning.
 */
GCExport void gc_class_prepared(Class_Handle ch, VTable_Handle vth);


/*
 * *****
 * *
 * *  Routines to handle the <code>java.lang.management</code> requests.
 * * 
 * *****
 */

/**
 * <p>
 * The number of collections that have been executed by this collector. A
 * value of <code>-1</code> means that collection counts are undefined for
 * this collector.
 * </p>
 * 
 * @return The number of collections executed.
 */
GCExport int64 gc_get_collection_count();

/**
 * <p>
 * The approximate, cumulative time (in microseconds) spent executing
 * collections for this collector.
 * </p>
 * 
 * @return The time spent collecting garbage.
 */
GCExport int64 gc_get_collection_time();



#else /* #if defined(USE_GC_STATIC) || defined(BUILDING_GC) */

/**
 * The below variables are used in the runtime dynamic linking of
 * garbage collector with virtual machine executable.
 */

extern void (*gc_add_root_set_entry)(Managed_Object_Handle *ref, Boolean is_pinned);
extern void (*gc_add_compressed_root_set_entry)(U_32 *ref, Boolean is_pinned);
extern void (*gc_add_root_set_entry_interior_pointer)(void **slot, int offset, Boolean is_pinned);
extern void (*gc_add_weak_root_set_entry)(Managed_Object_Handle *ref1, Boolean is_pinned,Boolean is_short_weak);
extern void (*gc_add_root_set_entry_managed_pointer)(void **slot, Boolean is_pinned);
extern void (*gc_class_prepared)(Class_Handle ch, VTable_Handle vth);
extern int64 (*gc_get_collection_count)();
extern int64 (*gc_get_collection_time)();
VMEXPORT extern void (*gc_force_gc)();
VMEXPORT extern int64 (*gc_free_memory)();
extern int (*gc_init)();
extern Boolean (*gc_supports_frontier_allocation)(unsigned *offset_of_current, unsigned *offset_of_limit);
extern Boolean (*gc_is_object_pinned)(Managed_Object_Handle obj);
extern Managed_Object_Handle (*gc_alloc)(unsigned size, 
                                         Allocation_Handle type,
                                         void *thread_pointer);
extern Managed_Object_Handle (*gc_alloc_fast)(unsigned size, 
                                              Allocation_Handle type,
                                              void *thread_pointer);
extern void (*gc_vm_initialized)();
//extern Managed_Object_Handle (*gc_pinned_malloc_noclass)(unsigned size);
extern void (*gc_thread_init)(void *gc_information);
extern void (*gc_thread_kill)(void *gc_information);
VMEXPORT extern int64 (*gc_total_memory)();
VMEXPORT extern int64 (*gc_max_memory)();
extern void (*gc_wrapup)();
extern Boolean (*gc_requires_barriers)();
extern void (*gc_test_safepoint)();


extern void (*gc_pin_object)(Managed_Object_Handle* p_object);
extern void (*gc_unpin_object)(Managed_Object_Handle* p_object);
extern I_32 (*gc_get_hashcode)(Managed_Object_Handle);
extern I_32 (*gc_get_hashcode0) (Managed_Object_Handle p_object);
extern Managed_Object_Handle (*gc_get_next_live_object)(void *iterator);
extern void (*gc_iterate_heap)();
extern void (*gc_finalize_on_exit)();
extern void (*gc_set_mutator_block_flag)();
extern Boolean (*gc_clear_mutator_block_flag)();



#endif /* #if defined(USE_GC_STATIC) || defined(BUILDING_GC) */




/**
 * Granularity of object alignment.
 *
 * Objects are aligned on 4 or 8 bytes. If they are aligned on 8 bytes then
 * Arrays will be required to start on the indicated alignement. This means that
 * for 8 byte alignment on the IA32 the header will look like this:
 *
 * U_32 gc_header_lock_hash
 * VTable *vt
 * U_32 array_length
 * U_32 padding
 * the array elements.
 */
#ifdef POINTER64
#define GC_OBJECT_ALIGNMENT 8
#else
#define GC_OBJECT_ALIGNMENT 4
#endif



#if !defined(USE_GC_STATIC) && !defined(BUILDING_GC)

/*
 * The below variables are used in the runtime dynamic linking of
 * garbage collector with virtual machine executable.
 */

extern Boolean (*gc_supports_compressed_references)();

extern void (*gc_heap_write_ref)(Managed_Object_Handle p_base_of_object_with_slot,
                                 unsigned offset,
                                 Managed_Object_Handle value);
extern void (*gc_heap_slot_write_ref)(Managed_Object_Handle p_base_of_object_with_slot,
                                      Managed_Object_Handle *p_slot,
                                      Managed_Object_Handle value);
extern void (*gc_heap_slot_write_ref_compressed)(Managed_Object_Handle p_base_of_object_with_slot,
                                                 U_32 *p_slot,
                                                 Managed_Object_Handle value);
extern void (*gc_heap_write_global_slot)(Managed_Object_Handle *p_slot,
                                         Managed_Object_Handle value);
extern void (*gc_heap_write_global_slot_compressed)(U_32 *p_slot,
                                                    Managed_Object_Handle value);
extern void (*gc_heap_wrote_object)(Managed_Object_Handle p_base_of_object_just_written);

extern Boolean (*gc_heap_copy_object_array)(Managed_Object_Handle src_array, unsigned int src_start, Managed_Object_Handle dst_array, unsigned int dst_start, unsigned int length);
/* 
 * The variables below are exported by the VM so other DLLs modules
 * may use them. <code>dll_gc.cpp</code> initializes them to the addresses exported
 * by GC DLL.
 */

VMEXPORT extern unsigned int (*gc_time_since_last_gc)();
extern void (*gc_write_barrier)(Managed_Object_Handle p_base_of_obj_with_slot);
VMEXPORT extern void * (*gc_heap_base_address)();
VMEXPORT extern void * (*gc_heap_ceiling_address)();

extern Boolean (*gc_supports_class_unloading)();

#else // USE_GC_STATIC

//@}
/** @name Routines to support various write barriers
 */
//@{

/**
 * @return <code>TRUE</code> if references within objects and vector 
 *         elements are to be treated as offsets rather than raw pointers.
 */
GCExport Boolean gc_supports_compressed_references();

/**
 * These interfaces are marked for replacement for the IPF by the following
 * <code>gc_heap_write_mumble</code> interface.
 *
 * @deprecated Will be removed soon.
 */
GCExport void gc_write_barrier(Managed_Object_Handle p_base_of_obj_with_slot);

/**
 * There are two flavors for historical reasons. The compiler for IA32 will
 * produce code for the version using an offset.
 *
 * @deprecated Will be removed soon.
 */
GCExport void gc_heap_wrote_object (Managed_Object_Handle p_base_of_object_just_written);

/**
 * * By calling this function VM notifies GC that a array copy operation should be performed.
 * *
 * * This function is for write barriers on array copy operations
 * */
GCExport Boolean gc_heap_copy_object_array(Managed_Object_Handle src_array, unsigned int src_start, Managed_Object_Handle dst_array, unsigned int dst_start, unsigned int length);

/**
 * By calling this function VM notifies GC that a heap reference was written to
 * global slot.
 *
 * There are some global slots that are shared by different threads. Write
 * barriers implementation needs to know about writes to these slots. One
 * example of such slots is in the string pools used by the class loader. 
 */
GCExport void gc_heap_write_global_slot(Managed_Object_Handle *p_slot,
                                        Managed_Object_Handle value);

/**
 * VM should call this function on heap reference writes to global slots.
 *
 * The "compressed" versions of the functions support updates to slots containing 
 * compressed references that are heap offsets; these functions handle details of 
 * converting raw reference pointers to compressed references before updating slots.
 */
GCExport void gc_heap_write_global_slot_compressed(U_32 *p_slot,
                                                   Managed_Object_Handle value);

/**
 * VM should call this function on heap reference writes to heap objects.
 */
GCExport void gc_heap_write_ref (Managed_Object_Handle p_base_of_object_with_slot,
                                 unsigned offset,
                                 Managed_Object_Handle value);
/**
 * @copydoc gc_heap_write_ref()
 */
GCExport void gc_heap_slot_write_ref (Managed_Object_Handle p_base_of_object_with_slot,
                                      Managed_Object_Handle *p_slot,
                                      Managed_Object_Handle value);

/**
 * @copydoc gc_heap_write_ref()
 */
GCExport void gc_heap_slot_write_ref_compressed (Managed_Object_Handle p_base_of_object_with_slot,
                                                 U_32 *p_slot,
                                                 Managed_Object_Handle value);



/**
 * Pin object.
 */
GCExport void gc_pin_object (Managed_Object_Handle* p_object);

/**
 * Unpin object.
 */
GCExport void gc_unpin_object (Managed_Object_Handle* p_object);

/**
 * Get identity hashcode.
 */
GCExport I_32 gc_get_hashcode (Managed_Object_Handle object);

/**
 * Get object hashcode.
 */
GCExport I_32 gc_get_hashcode (Managed_Object_Handle p_object);

/**
 * Iterate all live objects in heap.
 * Should be use only in classloader for class unloading purposes.
 */
GCExport Managed_Object_Handle gc_get_next_live_object(void *iterator);

/**
 * Iterates all objects in the heap.
 * This function calls <code>vm_iterate_object()</code> for each
 * iterated object.
 * Used for JVMTI Heap Iteration.
 * Should be called only in stop-the-world setting
 *
 * @see <code>vm_gc.h#vm_iterate_object()</code>
 */
GCExport void gc_iterate_heap();

/**
 * Moves all finalizable objects to vm finalization queue
 */
GCExport void gc_finalize_on_exit();

/**
 * Sets the mutator need block flag in case of heavy finalizable object load
 */
GCExport void gc_set_mutator_block_flag();

/**
 * Clears the mutator need block flag when heavy finalizable object load lightens
 */
GCExport Boolean gc_clear_mutator_block_flag();

GCExport Boolean gc_supports_class_unloading();

// XXX move this elsewhere -salikh
#ifdef JNIEXPORT

//@}
/** @name Routines to support soft, weak, and phantom reference objects
 */
//@{

/**
 * reference   - the reference object to register.
 * referent    - the referent of the reference object that is to be
 *               retrieved with the get method.
 *
 * The weak reference code written in Java and the support code provide by the
 * VM must agree on what the layout of a <code>Java.lang.ref.Reference</code> object looks
 * like and agree that any subclassing will only append fields to the agreed
 * upon layout. This seems reasonable.
 *
 * In addition the support code will have exclusive knowledge and control of a
 * single field (called <code>the_referent</code>) which holds the reference to the target
 * object. The java code will assume that this field is a read only integer
 * and should not be traced by the gc. The <code>Java.lang.ref.ReferenceQueue</code> layout
 * needs to also be known by the supporting code so that it can move reference
 * objects onto the queues at the appropriate times. The Java code uses normal
 * mechanisms to load the Reference classes and to create a reference.
 * 
 * The constructor code however needs to call the appropriate register function
 * listed below based upon whether we have a soft, weak, or phantom reference.
 * The VM support code will fill in the referent field. The routine
 * <code>gc_get_referent</code> will return the value in this field.
 *
 * @note The phantom reference method get will not use the <code>gc_get_referent</code> 
 *       but instead just return <code>NULL</code> as required by the spec.
 * 
 * @note XXX Why are they in gc_export.h? -salikh
 */
JNIEXPORT void JNICALL 
Java_java_lang_ref_Reference_enqueue_reference (JNIEnv *the_env, 
                                                jobject p_obj);

JNIEXPORT jobject JNICALL 
Java_java_lang_ref_Reference_get (JNIEnv *the_env, 
                                  jobject p_obj);

JNIEXPORT void JNICALL 
Java_java_lang_ref_Reference_register_phantom_ref (JNIEnv *the_env, jobject p_obj, 
                                                   jobject referent);

JNIEXPORT void JNICALL 
Java_java_lang_ref_Reference_register_soft_ref (JNIEnv *the_env, jobject p_obj, 
                                                jobject referent);

JNIEXPORT void JNICALL 
Java_java_lang_ref_Reference_register_weak_ref (JNIEnv *the_env, jobject p_obj, 
                                                jobject referent);
/*******/
//@}
#endif


/**
 * API for the VM to get the time since the last GC happened. 
 * Returns an unsigned long value in milliseconds.
 *
 * @note Is this call really needed in GC interface? -salikh 2005-05-12
 */
GCExport unsigned int gc_time_since_last_gc();

/**
 * @return The base address of the heap.
 *
 * API for VM to determine the starting and ending adddresses of the heap.
 */
GCExport void *gc_heap_base_address();

/**
 * @return The top address of the heap.
 */
GCExport void *gc_heap_ceiling_address();

#endif // USE_GC_STATIC


#ifdef __cplusplus
}
#endif

/**
 * \page gc_finalization_and_weak_refs Finalization and weak references design in GC
 *
 * \section gc_finalization Finalization
 *
 * According to the JVM specification, VM must call non-trivial finalization methods
 * before reclaiming the spaced used by object. Java API specification adds more
 * requiremens by stating that soft references must be cleared before weak references,
 * weak references must be cleared before object becomes eligible for finalization,
 * and that phantom references must be cleared last. JNI specification adds a little
 * bit more by specifying global weak references strength to be about the same as 
 * phantom reference strengh, however, without requiring any particular interaction
 * from them. (In the code we sometimes refer to weak references as <i>short weak
 * references<i>, and call JNI weak global references <i>long weak references</i>.
 *
 * @sa <code>gc_add_weak_root_set_entry()</code> for more details
 *
 * The requirements described above can be met using following algorithm.
 * \li All weak reference classes can be identified on the stage of class preparation,
 *     when VM calls <code>gc_class_prepared()</code> callback.
 * \li We start marking with regular (strong) roots, and traverse only strong references.
 *     During the process of marking all objects of the reference classes are collected
 *     to the reference lists. As we traverse only strong references, only strongly reachable
 *     reference will be scheduled for clearing and enqueueing.
 * \li At the end of marking we have strongly reachable objects marked. Unmarked objects
 *     may be any of softly, weakly, f-, or phantomly reachable.
 * \li Then we consider all strongly reachable soft references, and make 
 *     a decision on whether we need to clear them, based on the pending
 *     allocation request (if any), the current heap size, the information
 *     collected during marking and information about general GC dynamics.
 * \li Soft references which were chosen for clearing are reset, and other soft
 *     references are retraced, this time by both strong and soft references.
 *     Technically speaking, we add soft reference object pointers to the roots array,
 *     and add referent offset to the reference field array of SoftReference class,
 *     then restart regular mark task. During this trace, all softly reachable objects
 *     will be marked, and all softly (and strongly reachable) weak reference
 *     objects collected in a reference list.
 * \li Next we consider the list of weak references, and clear the references
 *     which point to dead (unmarked) objects. Note, that ordering of handling
 *     of soft and weak references is important and gives as the guarantees
 *     required by the Java specifications.
 * \li Short weak root handling is similar to weak references.
 * \li Finalizable objects are considered to find out objects, which became
 *     unreachable during this collection. Objects to be finalized are then
 *     revived by adding the finalizable queue to the root set and restarting
 *     mark process.
 * \li Phantom references are considered, and the references to unmarked 
 *     objects are cleared.
 * \li Long weak roots are handled in exactly the same way as short weak roots.
 * \li Note, that as the references are cleared, they are also added to the list
 *     of references to be enqueued. This list is later transferred to the VM
 *     using <code>vm_enqueue_reference()</code> function.
 * \li Weak reference objects require special handling of their referent field,
 *     because it needs to be reported as an updatable slot before compaction.
 *     This is performed as a last step, when we have a guarantee that all
 *     unmarked objects are dead at that moment.
 *
 * \section gc_finalization_and_weak_refs_requirements Adopted Requirements
 *
 * Current implementation of weak references places the following requirements
 * \li All reference objects must have exactly one non-regular object reference,
 *     and this reference must be at the same offset for references of all types.
 *     GC calls <code>class_is_reference()</code> to find out whether the class represents
 *     weak reference and finds out the referent offset by calling VM function 
 *     <code>class_get_referent_offset()</code>. Note, that referent offset being constant
 *     for all kinds of references is not enforced by this interface.
 * \li VM must not enqueue references during call of <code>vm_enqueue_reference()</code>
 *     because this may lead to deadlocks on reference queue monitors due
 *     to the fact that <code>vm_enqueue_reference()</code> is called during stop-the-world
 *     phase of garbage collection.
 */


/**
 * \page gc_vm_interface GC-VM interface
 *
 * The interface between garbage collector and virtual machine is
 * bidirectional:
 * <UL>
 * <LI>gc.h contains functions, which GC exports for use in VM.
 * <LI><code>vm_gc.h</code> contains VM functions which are available to GC during its
 * operation.
 * </UL>
 *  
 * A number of conventions exist, which are not easily expressed as C++ header
 * files. These include:
 * <UL>
 * <LI>Thread local nurseries, see gc_supports_frontier_allocation().
 * <LI>Read and Write barrier interface, gc_heap_write_ref() and others.
 * </UL>
 *
 * The conceptual overview of the interface is given in @link guide GC Writers'
 * guide @endlink
 */

#endif // _OPEN_GC_H
