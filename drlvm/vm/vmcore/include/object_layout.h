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

#ifndef _OBJECT_LAYOUT_H_
#define _OBJECT_LAYOUT_H_

// Define USE_COMPRESSED_VTABLE_POINTERS here to enable compressed vtable
// pointers within objects.
#ifdef POINTER64
#define USE_COMPRESSED_VTABLE_POINTERS
#endif // POINTER64

#include <assert.h>
#include "open/types.h"
//#include "open/hythread_ext.h"
#include "jni.h"
#include "open/vm.h"
#include "open/gc.h"

typedef struct VTable VTable;

#ifdef __cplusplus
extern "C" {
#endif

#ifdef POINTER64

#if !defined(REFS_USE_COMPRESSED) && !defined(REFS_USE_UNCOMPRESSED)
#ifndef REFS_USE_RUNTIME_SWITCH
#define REFS_USE_RUNTIME_SWITCH
#endif
#endif

#else // POINTER64
// 32-bit platform always uses uncompressed 32-bit references
#ifdef REFS_USE_COMPRESSED
#undef REFS_USE_COMPRESSED
#endif
#ifdef REFS_USE_RUNTIME_SWITCH
#undef REFS_USE_RUNTIME_SWITCH
#endif

#ifndef REFS_USE_UNCOMPRESSED
#define REFS_USE_UNCOMPRESSED
#endif

#endif // POINTER64

// Definitions for building ifdefs
#if defined(REFS_USE_RUNTIME_SWITCH) || defined(REFS_USE_UNCOMPRESSED)
#define REFS_RUNTIME_OR_UNCOMPRESSED
#endif

#if defined(REFS_USE_RUNTIME_SWITCH) || defined(REFS_USE_COMPRESSED)
#define REFS_RUNTIME_OR_COMPRESSED
#endif

/// Raw and compressed reference pointers

typedef ManagedObject*  RAW_REFERENCE;
typedef U_32          COMPRESSED_REFERENCE;

// Useful macros: REFS_IS_COMPRESSED_MODE effectively specifies compressed mode
// REF_SIZE returns size of type used for references
// REF_MANAGED_NULL defines 'null' reference value for current compression mode
// REF_INIT_BY_ADDR initializes reference with value of proper size
#if defined(REFS_USE_COMPRESSED)

#define REFS_IS_COMPRESSED_MODE 1
#define REF_SIZE (sizeof(U_32))
#define REF_MANAGED_NULL VM_Global_State::loader_env->heap_base
#define REF_INIT_BY_ADDR(_ref_addr_, _val_)                                 \
    *((COMPRESSED_REFERENCE*)(_ref_addr_)) = (COMPRESSED_REFERENCE)(_val_)

#elif defined(REFS_USE_UNCOMPRESSED)

#define REFS_IS_COMPRESSED_MODE 0
#define REF_SIZE (sizeof(ManagedObject*))
#define REF_MANAGED_NULL NULL
#define REF_INIT_BY_ADDR(_ref_addr_, _val_)                                 \
    *((ManagedObject**)(_ref_addr_)) = (ManagedObject*)(_val_)

#else // for REFS_USE_RUNTIME_SWITCH

#define REFS_IS_COMPRESSED_MODE                                             \
                    (VM_Global_State::loader_env->compress_references)
#define REF_SIZE (VM_Global_State::loader_env->compress_references ?        \
                    sizeof(COMPRESSED_REFERENCE) :                          \
                    sizeof(ManagedObject *))
#define REF_MANAGED_NULL                                                    \
    (VM_Global_State::loader_env->compress_references ?                     \
            VM_Global_State::loader_env->heap_base : NULL)
#define REF_INIT_BY_ADDR(_ref_addr_, _val_)                                 \
    if (VM_Global_State::loader_env->compress_references) {                 \
        *((COMPRESSED_REFERENCE*)(_ref_addr_)) =                            \
                                    (COMPRESSED_REFERENCE)(_val_);          \
    } else {                                                                \
        *((ManagedObject**)(_ref_addr_)) = (ManagedObject*)(_val_);         \
    }
#endif

// Helper macros for using in 'if'
#ifdef REFS_USE_RUNTIME_SWITCH
#define REFS_RUNTIME_SWITCH_IF if (REFS_IS_COMPRESSED_MODE) {
#define REFS_RUNTIME_SWITCH_ELSE } else {
#define REFS_RUNTIME_SWITCH_ENDIF }
#else // REFS_USE_RUNTIME_SWITCH
#define REFS_RUNTIME_SWITCH_IF
#define REFS_RUNTIME_SWITCH_ELSE
#define REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_USE_RUNTIME_SWITCH


bool is_compressed_reference(COMPRESSED_REFERENCE value);

VMEXPORT COMPRESSED_REFERENCE compress_reference(ManagedObject *obj);
VMEXPORT ManagedObject* uncompress_compressed_reference(COMPRESSED_REFERENCE compressed_ref);


// Given the address of a slot containing a reference, returns the raw reference pointer whether the slot held
// a compressed or uncompressed.reference.
inline ManagedObject *get_raw_reference_pointer(ManagedObject **slot_addr)
{
#ifdef REFS_USE_RUNTIME_SWITCH
    if (vm_is_heap_compressed()) {
#endif // REFS_USE_RUNTIME_SWITCH
#ifdef REFS_RUNTIME_OR_COMPRESSED
    COMPRESSED_REFERENCE offset = *((COMPRESSED_REFERENCE *)slot_addr);
    assert(is_compressed_reference(offset));
    if (offset != 0) {
        return (ManagedObject*)((POINTER_SIZE_INT)vm_get_heap_base_address() + offset);
    }

    return NULL;
#endif // REFS_RUNTIME_OR_COMPRESSED
REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
        return *slot_addr;
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
REFS_RUNTIME_SWITCH_ENDIF
} //get_raw_reference_pointer


// Store the reference "_value_" in the slot at address "_slot_addr_"
// in the object "_object_".
#if defined(REFS_USE_COMPRESSED)
#define STORE_REFERENCE(_object_, _slot_addr_, _value_)                     \
        gc_heap_slot_write_ref_compressed((Managed_Object_Handle)(_object_),\
                                          (U_32*)(_slot_addr_),           \
                                          (Managed_Object_Handle)(_value_))
#elif defined(REFS_USE_UNCOMPRESSED)
#define STORE_REFERENCE(_object_, _slot_addr_, _value_)                     \
        gc_heap_slot_write_ref((Managed_Object_Handle)(_object_),           \
                               (Managed_Object_Handle*)(_slot_addr_),       \
                               (Managed_Object_Handle)(_value_))
#else // for REFS_USE_RUNTIME_SWITCH
#define STORE_REFERENCE(_object_, _slot_addr_, _value_)                     \
    if (VM_Global_State::loader_env->compress_references) {                 \
        gc_heap_slot_write_ref_compressed((Managed_Object_Handle)(_object_),\
                                          (U_32*)(_slot_addr_),           \
                                          (Managed_Object_Handle)(_value_));\
    } else {                                                                \
        gc_heap_slot_write_ref((Managed_Object_Handle)(_object_),           \
                               (Managed_Object_Handle*)(_slot_addr_),       \
                               (Managed_Object_Handle)(_value_));           \
    }
#endif // REFS_USE_RUNTIME_SWITCH


// Store the reference "value" in the static field or
// other global slot at address "slot_addr".
#if defined(REFS_USE_COMPRESSED)
#define STORE_GLOBAL_REFERENCE(_slot_addr_, _value_)                    \
        gc_heap_write_global_slot_compressed((U_32*)(_slot_addr_),    \
                                   (Managed_Object_Handle)(_value_))
#elif defined(REFS_USE_UNCOMPRESSED)
#define STORE_GLOBAL_REFERENCE(_slot_addr_, _value_)                    \
        gc_heap_write_global_slot((Managed_Object_Handle*)(_slot_addr_),\
                                  (Managed_Object_Handle)(_value_))
#else // for REFS_USE_RUNTIME_SWITCH
#define STORE_GLOBAL_REFERENCE(_slot_addr_, _value_)                    \
    if (VM_Global_State::loader_env->compress_references) {             \
        gc_heap_write_global_slot_compressed((U_32*)(_slot_addr_),    \
                                  (Managed_Object_Handle)(_value_));    \
    } else {                                                            \
        gc_heap_write_global_slot((Managed_Object_Handle*)(_slot_addr_),\
                                  (Managed_Object_Handle)(_value_));    \
    }
#endif // REFS_USE_RUNTIME_SWITCH



// The object layout is currently as follows
//
//    * VTable* / U_32 vt_offset  +
//  .-* U_32 lockword             +- get_constant_header_size()
//  |   [I_32 array lenth]
//  | * [void* tag pointer]
//  |   [padding]
//  |   fields / array elements
//  |
//  `- get_size()
//
// tag pointer field is present iff ManagedObject::_tag_pointer is true
// length field is present in array objects

typedef struct ManagedObject {
#if defined USE_COMPRESSED_VTABLE_POINTERS
    union {
    U_32 vt_offset;
    POINTER_SIZE_INT padding;
    };
    union {
    U_32 obj_info;
    POINTER_SIZE_INT padding2;
    };

    VTable *vt_unsafe() { return (VTable*)(vt_offset + (UDATA)vm_get_vtable_base_address()); }
    VTable *vt() { assert(vt_offset); return vt_unsafe(); }
    static VTable *allocation_handle_to_vtable(Allocation_Handle ah) {
        return (VTable *) ((UDATA)ah + (UDATA)vm_get_vtable_base_address());
    }
    static bool are_vtable_pointers_compressed() { return true; }
#else // USE_COMPRESSED_VTABLE_POINTERS
    VTable *vt_raw;
    union {
    U_32 obj_info;
    POINTER_SIZE_INT padding;
    };
    VTable *vt_unsafe() { return vt_raw; }
    VTable *vt() { assert(vt_raw); return vt_unsafe(); }
    static VTable *allocation_handle_to_vtable(Allocation_Handle ah) {
        return (VTable *) ah;
    }
    static bool are_vtable_pointers_compressed() { return false; }
#endif // USE_COMPRESSED_VTABLE_POINTERS

    static unsigned header_offset() { return (unsigned)(POINTER_SIZE_INT)(&((ManagedObject*)NULL)->obj_info); }

    /// returns the size of constant object header part (vt pointer and obj_info)
    static size_t get_constant_header_size() { return sizeof(ManagedObject); }
    /// returns the size of object header including dynamically enabled fields.
    static size_t get_size() { 
        return get_constant_header_size() + (_tag_pointer ? sizeof(void*) : 0); 
    }

    U_32 get_obj_info() { return obj_info; }
    void set_obj_info(U_32 value) { obj_info = value; }
    U_32* get_obj_info_addr() {
        return (U_32*)((char*)this + header_offset());
    }

    /**
     * returns address of a tag pointer field in a _non-array_ object.
     * For array objects use VM_Vector::get_tag_pointer_address().
     */
    void** get_tag_pointer_address() {
        assert(_tag_pointer);
        return (void**)((char*)this + get_constant_header_size());
    }

    VMEXPORT static bool _tag_pointer;
} ManagedObject;



// See gc_for_vm, any change here needs to be reflected in Partial_Reveal_JavaArray.
typedef struct VM_Vector
{
    ManagedObject object;
    I_32 length;

    static size_t length_offset() { return (size_t)(&((VM_Vector*)NULL)->length); }
    I_32 get_length() { return length; }
    void set_length(I_32 len) { length = len; }

    void** get_tag_pointer_address() {
        assert(ManagedObject::_tag_pointer);
        int offset = sizeof(VM_Vector);
        return (void**)((char*)this + offset);
    }

} VM_Vector;




// Every vector has two pointers that can be found in any VM object
// and an I_32 field to hold the length (i.e., the number of elements).
// The combined size of those fields is:
#define VM_VECTOR_RT_OVERHEAD ((unsigned)(ManagedObject::get_size() + sizeof(I_32)))


// The offset to the first element of a vector with 8-byte elements.
#define VM_VECTOR_FIRST_ELEM_OFFSET_8 ((VM_VECTOR_RT_OVERHEAD + 7) & ~7)

// The offset to the first element of a vector with 1, 2, or 4-byte elements.
// After the code in CVS gets stable, change this to be
// the same on IPF and IA-32.
#ifdef POINTER64
#define VM_VECTOR_FIRST_ELEM_OFFSET_1_2_4 VM_VECTOR_FIRST_ELEM_OFFSET_8
#else
#define VM_VECTOR_FIRST_ELEM_OFFSET_1_2_4 VM_VECTOR_RT_OVERHEAD
#endif

// References are either 4 or 8-byte wide.
#ifdef POINTER64
#define VM_VECTOR_FIRST_ELEM_OFFSET_REF VM_VECTOR_FIRST_ELEM_OFFSET_8
#else
#define VM_VECTOR_FIRST_ELEM_OFFSET_REF VM_VECTOR_FIRST_ELEM_OFFSET_1_2_4
#endif




#ifdef __cplusplus
}
#endif

#endif // _OBJECT_LAYOUT_H_





