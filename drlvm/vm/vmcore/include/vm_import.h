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
// These are the functions that modules like JIT and GC provide for the core VM.
//

#ifndef _VM_IMPORT_H
#define _VM_IMPORT_H



#ifdef __cplusplus
extern "C" {
#endif


// Return a string representing the kind of the module.
// Today one of two return values is expected "GC" or "JIT" to identify
// a GC and a JIT module respectively.
VMIMPORT const char *get_module_kind();



// A human-friendly name of the module, e.g. "JITRINO", "IA-32 O0 JIT",
// "DRL GC V4".
VMIMPORT const char *get_module_name();



// A version string that will be printed along with the module name
// when the VM is invoked with the -version command line argument.
// The format is up to the component, e.g., "v 3.12.1.6", "Build 3278", or
// "2003-01-30".
VMIMPORT const char *get_module_version();



// Returns the string identifying the required version of the interface.
// In our simple versioning model, we assume that all component interfaces
// are indentified by the same string.  This function will be used to prevent
// the use of a component that uses an outdated interface.
// In the future we may consider a more elaborate versioning scheme but
// for now we use this simple scheme.
VMIMPORT const char *get_required_interface_version();




#ifdef __cplusplus
}
#endif


#endif // _VM_IMPORT_H
