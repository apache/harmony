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
#ifndef _OBJECT_HANDLES_H
#define _OBJECT_HANDLES_H

/**
 * @file
 * This module manages object handles and other forms of GC root protection. It is
 * not exposed outside VM - other modules should use pure JNI instead.
 *
 * There are three managed pointer protection facilities:
 *   1) Object handles on M2nFrames
 *   2) Native object handles
 *   3) GC frames
 * GC frames are C++ objects that protect object references and managed pointers directly.
 * A function can set up a GC frame to protect its references and then pass those references
 * to functions it calls.
 * Handles protect indirectly.
 * A function creates handles for the references and passes the handles rather than the references.
 */

#include "vm_core_types.h"
#include "object_layout.h"
#include "jni.h"

/**
 * GC frames store memory locations which contain managed pointers, so they are updated
 * during garbage collection. Note, GC suspension must be disabled when created or deleting
 * a frame and when adding objects or managed pointers.
 */
#define GC_FRAME_DEFAULT_SIZE 10
class GcFrame {
public:
    GcFrame();
    ~GcFrame();

    void add_object(ManagedObject**);
    void add_managed_pointer(ManagedPointer*);

    // Enumerate all roots being protected including those of subsequent frames.
    void enumerate();

private:
    void ensure_capacity();

    // A GcFrameNode contains capacity elements which can hold either objects references or managed pointers.
    // The objects come first, ie, from index 0 to obj_size-1
    // The managed pointers come last, ie, from index obj_size to obj_size+mp_size-1
    // Inv: obj_size+mp_size<=capacity
    struct GcFrameNode {
        unsigned obj_size, mp_size, capacity;
        GcFrameNode* next;
        void** elements[GC_FRAME_DEFAULT_SIZE];  // Objects go several first, then managed pointers
    } firstSetOfNodes;
    GcFrameNode* nodes;
    GcFrame* next;
};

//////////////////////////////////////////////////////////////////////////
// Handles

// GC must be disabled when object is set, and the object loaded from object
// is only valid while GC is disabled.
// FIXME will be empty as in JNI
struct _jobject { ManagedObject* object; };

typedef jobject ObjectHandle;

///**
// * Reference to an object.
// */
//struct _ObjectHandle {
//    ManagedObject* object;
//};
//
///**
// * Internal object handle. The difference with <code>jobject</code> is
// * <code>jobject</code> can be passed to user JNI code and freed by
// * <code>DeleteLocalRef</code>.
// */
//typedef const struct _ObjectHandle* ObjectHandle;
//
///**
// * Reference to an instance of <code>java.lang.Class</code>.
// */
//struct _ClassHandle: _ObjectHandle {};
//
///**
// * <code>ObjectHandle</code> which refers to a class.
// */
//typedef const struct _ClassHandle* ClassHandle;



#ifndef NDEBUG
/**
 * Checks if an object is exactly <code>java.lang.Class.class</code>.
 * This function must be called when GC suspension is disabled.
 */
bool
managed_object_is_java_lang_class(ManagedObject*);

/**
 * Checks if an object is exactly <code>java.lang.Class.class</code>.
 * This function must be called when GC suspension is enabled.
 */
bool
object_is_java_lang_class(ObjectHandle);

/**
 * Checks if an object's virtual table points to the correct class.
 * This function must be called when GC suspension is disabled.
 */
bool
managed_object_is_valid(ManagedObject*);

/**
 * Checks if an object's virtual table points to the correct class.
 * This function must be called when GC suspension is enabled.
 */
bool
object_is_valid(ObjectHandle);
#endif /* NDEBUG */


/*
 * Global handles
 */

/**
 * Creates global handle, which needs to be explicitly freed.
 */
ObjectHandle oh_allocate_global_handle();
ObjectHandle oh_allocate_global_handle_from_jni();
/**
 * Frees global handle.
 */
void oh_deallocate_global_handle(ObjectHandle);
/**
 * Called during root enumeration process to enumerate global handles.
 */
void oh_enumerate_global_handles();

/*
 * Weak global handles
 */

/**
 * Creates weak global handle, which needs to be explicitly freed.
 */
ObjectHandle oh_allocate_weak_global_handle_from_jni();
/**
 * Frees weak global handle.
 */
void oh_deallocate_weak_global_handle(ObjectHandle);
/* For interface simplicity, weak global handles are also enumerated in function oh_enumerate_global_handles */

//////////////////////////////////////////////////////////////////////////
// Local Handles

// Local handles are stored in either M2nFrames or in native object handles
// structures.  The most recent such frame or structure is used for allocation.
// M2nFrame handles are freed when the frame is popped.  Native object handles
// are freed upon exit from the scope that declared the native object handles
// structure.  Handles are invalid and should not be used after they are freed.

VMEXPORT // temporary solution for interpreter unplug
ObjectHandle oh_allocate_local_handle();

ObjectHandle oh_allocate_local_handle_from_jni();

ObjectHandle oh_convert_to_local_handle(ManagedObject* pointer);
ObjectHandle oh_copy_to_local_handle(ObjectHandle oh);

// The following function invalidates a local handle and allows (at next GC) the collection
// of the object if it is now garbage.
void oh_discard_local_handle(ObjectHandle);

struct ObjectHandles;

VMEXPORT // temporary solution for interpreter unplug
void oh_enumerate_handles(ObjectHandles*);
void oh_free_handles(ObjectHandles*);

//VMEXPORT // temporary solution for interpreter unplug
class VMEXPORT NativeObjectHandles {
public:
    NativeObjectHandles();
    ~NativeObjectHandles();

    // Allocate a handle in this structure
    ObjectHandle allocate();

    // Enumerate all handles in this and subsequent structures
    void enumerate();

private:
    friend void oh_discard_local_handle(ObjectHandle);
    ObjectHandles* handles;
    NativeObjectHandles* next;
};

//////////////////////////////////////////////////////////////////////////
// Routines for creating handles in stubs

// Fill ObjectHandles sructure as empty.
void oh_null_init_handles(ObjectHandles* handles);

// Add to a LIL code stub code to allocate space for handles structures
// This LIL code will initialise the structures except for the actual object references, and will
// set the handles pointer in the M2nFrame
//   number_handles - Number of handles to allocate space for
//   base_var       - LIL variable to hold pointer to base of handles structure
//   helper_var     - LIL variable to use to initialise structure
LilCodeStub* oh_gen_allocate_handles(LilCodeStub*, unsigned number_handles, const char* base_var, const char* helper_var);

// Calculate the offset of the base of a previously allocated structure to a particular handle
// The base variable (see oh_gen_allocate_handles) plus this offset is the value to use for the handle
POINTER_SIZE_INT oh_get_handle_offset(unsigned handle_indx);

// Initialise a handle in a previously allocated structure
//   base_var    - LIL variable that points to the base of the handles structure
//   handle_indx - Index of handle to initialise
//   val         - LIL operand for the object reference to use to initialise handle
//   null_check  - If true check for a managed null and store an unmanaged null instead
LilCodeStub* oh_gen_init_handle(LilCodeStub*, const char* base_var, unsigned handle_indx, const char* val, bool null_check);

//////////////////////////////////////////////////////////////////////////
// 20031218: Needed for old stub code
// To be deprecated soon - do not use in new code!

struct ObjectHandlesOld {
    _jobject handle;
    ObjectHandlesOld* prev;
    ObjectHandlesOld* next;
    bool allocated_on_the_stack;
};

struct ObjectHandlesNew {
#ifdef _IPF_
    U_32 capacity;
    U_32 size;
#else //IA32
    uint16 capacity;
    uint16 size;
#endif //IA32
    ObjectHandlesNew* next;
    ManagedObject* refs[1];
};

// free and delete all local object handles
VMEXPORT // temporary solution for interpreter unplug
void free_local_object_handles2(ObjectHandles*);
void free_local_object_handles3(ObjectHandles*);

#endif // _OBJECT_HANDLES
