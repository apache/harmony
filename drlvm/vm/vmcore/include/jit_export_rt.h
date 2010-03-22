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

//
// These are the functions that a JIT built as a DLL must export for
// the purpose of runtime interaction.
//

#ifndef _JIT_EXPORT_RT_H
#define _JIT_EXPORT_RT_H

#include "open/types.h"
#include "open/rt_types.h"
#include "open/em.h"

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus


///////////////////////////////////////////////////////
// begin direct call support


// The following are optional functions used by the direct call-related JIT interface. 
// These functions are implemented by a JIT and invoked by the VM. They allow a JIT 
// to be notified whenever, e.g., a VM data structure changes that would require 
// code patching or recompilation.
 
// The callback that corresponds to vm_register_jit_recompiled_method_callback.  
// The JIT should return TRUE if any code was modified (consequently the VM will ensure 
// correctness such as synchronizing I- and D-caches), and FALSE otherwise.
JITEXPORT Boolean 
JIT_recompiled_method_callback(JIT_Handle jit,
                               Method_Handle  recompiled_method,
                               void          *callback_data); 


// end direct call support
///////////////////////////////////////////////////////



///////////////////////////////////////////////////////
// begin stack unwinding

JITEXPORT void
JIT_unwind_stack_frame(JIT_Handle         jit, 
                       Method_Handle      method,
                       JitFrameContext* context
                       );

JITEXPORT void 
JIT_get_root_set_from_stack_frame(JIT_Handle             jit,
                                  Method_Handle          method,
                                  GC_Enumeration_Handle  enum_handle,
                                  JitFrameContext* context
                                  );

JITEXPORT void 
JIT_get_root_set_for_thread_dump(JIT_Handle             jit,
                                  Method_Handle          method,
                                  GC_Enumeration_Handle  enum_handle,
                                  JitFrameContext* context
                                  );
/**
 * Returns number of methods which were inlined at the specified location (zero if none)
 * @param jit - a JIT which produced the code
 * @param prt - corresponding inline info
 * @param offset - offset in native code relative to code block start
 */
JITEXPORT U_32 
JIT_get_inline_depth(JIT_Handle jit, 
                     InlineInfoPtr   ptr, 
                     U_32          offset);

/**
* Returns specified inlined method (null if not found).
* The inlined methods are indexed as [max_depth..1], 
* so the topmost method on the stack has maximum inline depth and
* enclosing methods have descending indicies. 
* Zero depth would mean nearest non-inlined method and should not be used here.
* @param jit - a JIT which produced the code
* @param prt - corresponding inline info
* @param offset - offset in native code relative to code block start
* @param inline_depth - index of the inlined method
*/
JITEXPORT Method_Handle
JIT_get_inlined_method(JIT_Handle jit, 
                       InlineInfoPtr ptr, 
                       U_32 offset,
                       U_32 inline_depth);

/**
* Returns bytecode offset at specified inlined method for the native code (zero if unknown).
* The inlined methods are indexed as [max_depth..1], 
* so the topmost method on the stack has maximum inline depth and
* enclosing methods have descending indicies. 
* Zero depth would mean nearest non-inlined method and should not be used here.
* @param jit - a JIT which produced the code
* @param prt - corresponding inline info
* @param offset - offset in native code relative to code block start
* @param inline_depth - index of the inlined method
*/
JITEXPORT uint16
JIT_get_inlined_bc(JIT_Handle jit, 
                   InlineInfoPtr ptr, 
                   U_32 offset, 
                   U_32 inline_depth);

JITEXPORT void
JIT_fix_handler_context(JIT_Handle         jit,
                        Method_Handle      method,
                        JitFrameContext* context
                        );

JITEXPORT void *
JIT_get_address_of_this(JIT_Handle               jit,
                        Method_Handle            method,
                        const JitFrameContext* context
                        );

// end stack unwinding
///////////////////////////////////////////////////////



///////////////////////////////////////////////////////
// begin compressed references


// Returns TRUE if the JIT will compress references within objects and vector elements by representing 
// them as offsets rather than raw pointers. The JIT should call the VM function vm_is_heap_compressed()
// during initialization in order to decide whether it should compress references.
JITEXPORT Boolean 
JIT_supports_compressed_references(JIT_Handle jit);

// end compressed references
///////////////////////////////////////////////////////

#ifdef __cplusplus
}
#endif // __cplusplus


#endif // _JIT_EXPORT_RT_H
