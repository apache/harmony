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

/**
 * @file
 * These are the functions that a JIT built as a DLL may call.
 */

#ifndef _JIT_IMPORT_RT_H
#define _JIT_IMPORT_RT_H

#include "open/types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Begin root set enumeration functions.
 *
 * Enumerate a root entry. The argument is a pointer to the memory
 * location that contains a managed reference. The GC may update
 * this location if the object pointed to has been moved during
 * collection.
 */
DECLARE_OPEN(void, vm_enumerate_root_reference, (Managed_Object_Handle *ref, BOOLEAN is_pinned));

// Enumerate a weak root entry.  The argument is a pointer to the memory
// location that contains a managed reference.  The GC may update
// this location if the object pointed to has been moved during
// collection.
DECLARE_OPEN(void, vm_enumerate_weak_root_reference, (Managed_Object_Handle *ref, BOOLEAN is_pinned));

/**
 * Resembles vm_enumerate_root_reference(), but is passed the 
 * address of a slot containing a compressed reference.
 */
DECLARE_OPEN(void, vm_enumerate_compressed_root_reference, (U_32 *ref, BOOLEAN is_pinned));

/** 
 * Like vm_enumerate_root_reference(), but the first argument 
 * points to a location that contains a pointer to an inside of an object.
 */
DECLARE_OPEN(void, vm_enumerate_root_interior_pointer, (void **slot, size_t offset, BOOLEAN is_pinned));

/** 
 * Enumerates alive references for owned monitor calculation.
 */
DECLARE_OPEN(void, vm_check_if_monitor, (void  **reference,
                                       void  **base_reference,
                                       U_32 *  compressed_reference, 
                                       size_t  slotOffset, 
                                       BOOLEAN pinned,
                                       U_32    type));

#ifdef __cplusplus
}
#endif

#endif // _JIT_IMPORT_RT_H
