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
#ifndef __VTABLE_H__
#define __VTABLE_H__

/**
 * @file
 * virtual method table of a class
 */
#include <open/types.h>
struct Class;

extern "C" {
typedef struct {
    unsigned char** table;  // pointer into methods array of Intfc_Table below
    Class* intfc_class;      // id of interface
} Intfc_Table_Entry;

typedef struct Intfc_Table {
#ifdef POINTER64
    // see INTFC_TABLE_OVERHEAD
    U_32 dummy;   // padding
#endif
    U_32 n_entries;
    Intfc_Table_Entry entry[1];
} Intfc_Table;

#define INTFC_TABLE_OVERHEAD    (sizeof(void*))

#ifdef POINTER64
#define OBJECT_HEADER_SIZE 0
// The size of an object reference. Used by arrays of object to determine
// the size of an element.
#define OBJECT_REF_SIZE 8
#else // POINTER64
#define OBJECT_HEADER_SIZE 0
#define OBJECT_REF_SIZE 4
#endif // POINTER64

#define GC_BYTES_IN_VTABLE (sizeof(void*))
#define MAX_FAST_INSTOF_DEPTH 5

/**
* @return The number of superclass hierarchy elements that are
*         stored within the vtable. This is for use with fast type checking.
*/
inline unsigned vm_max_fast_instanceof_depth()
{
    return MAX_FAST_INSTOF_DEPTH;
}

typedef struct VTable {
    U_8 _gc_private_information[GC_BYTES_IN_VTABLE];
    ManagedObject*             jlC; 
    unsigned int             vtmark; 
    Class* clss;

    // See the masks in vm_for_gc.h.
    U_32 class_properties;

    // Offset from the top by CLASS_ALLOCATED_SIZE_OFFSET
    // The number of bytes allocated for this object. It is the same as
    // instance_data_size with the constraint bit cleared. This includes
    // the OBJECT_HEADER_SIZE as well as the OBJECT_VTABLE_POINTER_SIZE
    unsigned int allocated_size;

    unsigned short array_element_size;
    unsigned short array_element_shift;
    
    // cached values, used for helper inlining to avoid extra memory access
    unsigned char** intfc_table_0;
    Class*          intfc_class_0;
    unsigned char** intfc_table_1;
    Class*          intfc_class_1;
    unsigned char** intfc_table_2;
    Class*          intfc_class_2;

    Intfc_Table* intfc_table;   // interface table; NULL if no intfc table
    Class *superclasses[MAX_FAST_INSTOF_DEPTH];
    unsigned char* methods[1];  // code for methods
} VTable;
#define VTABLE_OVERHEAD (sizeof(VTable) - sizeof(void *))
// The "- sizeof(void *)" part subtracts out the "unsigned char *methods[1]" contribution.

VTable *create_vtable(Class *p_class, unsigned n_vtable_entries);

} // extern "C"


#endif
