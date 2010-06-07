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

#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <assert.h>

#include "mkernel.h"

#define DYNAMIC_OPEN
#include "VMInterface.h"
#include "open/vm_properties.h"
#include "open/vm_class_manipulation.h"
#include "open/vm_class_loading.h"
#include "open/vm_type_access.h"
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_loading.h"
#include "open/vm_class_info.h"
#include "open/vm_interface.h"
#include "open/vm_ee.h"
#include "open/vm.h"
#include "jit_import_rt.h"
#include "jit_runtime_support.h"

#include "Type.h"

#include "CompilationContext.h"
#include "Log.h"
#include "JITInstanceContext.h"
#include "PlatformDependant.h"

#include "VMMagic.h"

namespace Jitrino {

vm_adaptor_t VMInterface::vm = 0;


static  allocation_handle_get_class_t  allocation_handle_get_class = 0;

//Class
static  class_get_array_element_class_t  class_get_array_element_class = 0;
static  class_get_array_element_size_t  class_get_array_element_size = 0; 
static  class_get_array_of_class_t  class_get_array_of_class = 0;
static  class_is_non_ref_array_t class_is_non_ref_array = 0;
static  class_get_name_t class_get_name = 0;
static  class_get_super_class_t  class_get_super_class = 0;
static  class_get_depth_t  class_get_depth = 0;
static  class_get_vtable_t  class_get_vtable = 0;
static  class_get_allocation_handle_t  class_get_allocation_handle = 0;
static  class_get_object_size_t  class_get_object_size = 0;
static  class_cp_get_num_array_dimensions_t class_cp_get_num_array_dimensions = 0;
static  class_get_class_of_primitive_type_t  class_get_class_of_primitive_type = 0;
static  class_get_const_string_intern_addr_t class_get_const_string_intern_addr = 0;
static  class_cp_get_const_type_t class_cp_get_const_type = 0;
static  class_cp_get_const_addr_t class_cp_get_const_addr = 0;
static  class_get_method_by_name_t class_get_method_by_name = 0;
static  class_get_field_by_name_t class_get_field_by_name = 0;
static  class_get_class_loader_t  class_get_class_loader = 0;

static  class_is_array_t  class_is_array = 0;
static  class_is_enum_t  class_is_enum = 0;
static  class_is_final_t  class_is_final = 0;
static  class_is_throwable_t  class_is_throwable = 0;
static  class_is_interface_t  class_is_interface = 0;
static  class_is_abstract_t  class_is_abstract = 0;
static  class_is_initialized_t  class_is_initialized = 0;
static  class_is_finalizable_t  class_is_finalizable = 0;
static  class_is_instanceof_t class_is_instanceof = 0;
static  class_is_support_fast_instanceof_t  class_is_support_fast_instanceof = 0;// class_is_support_fast_instanceof
static  class_is_primitive_t  class_is_primitive = 0;

static  vm_lookup_class_with_bootstrap_t  vm_lookup_class_with_bootstrap = 0;
static  class_lookup_method_recursively_t class_lookup_method_recursively = 0;

// Const Pool
static  class_cp_get_field_type_t class_cp_get_field_type = 0;// VM_Data_Type class_cp_get_field_type(Class_Handle src_class, unsigned short cp_index);
static  class_cp_get_entry_class_name_t class_cp_get_entry_class_name = 0;//const char* class_cp_get_entry_class_name(Class_Handle cl, unsigned short index);
static  class_cp_get_entry_name_t class_cp_get_entry_name = 0;//const char* class_cp_get_entry_name(Class_Handle cl, unsigned short index);
static  class_cp_get_entry_descriptor_t class_cp_get_entry_descriptor = 0;//const char* class_cp_get_entry_descriptor(Class_Handle cl, unsigned short index);
static  class_cp_is_entry_resolved_t class_cp_is_entry_resolved = 0;//bool class_cp_is_entry_resolved(Compile_Handle ch, Class_Handle clazz, unsigned cp_index);
static  class_cp_get_class_name_t class_cp_get_class_name =0;//const char* class_cp_get_class_name(Class_Handle cl, unsigned index);


//Field

static  field_get_address_t  field_get_address = 0;
static  field_get_class_t  field_get_class = 0;
static  field_get_descriptor_t field_get_descriptor = 0;
static  field_get_name_t field_get_name = 0;
static  field_get_offset_t  field_get_offset = 0;
static  field_get_type_info_t  field_get_type_info = 0;
static  field_is_final_t  field_is_final = 0;
static  field_is_magic_t  field_is_magic = 0; //Boolean field_is_magic(Field_Handle fh);
static  field_is_private_t  field_is_private = 0;
static  field_is_static_t  field_is_static = 0;
static  field_is_volatile_t  field_is_volatile = 0;

//Method

static  method_get_overriding_method_t method_get_overriding_method = 0;
static  method_get_info_block_jit_t method_get_info_block_jit = 0;
static  method_get_info_block_size_jit_t method_get_info_block_size_jit = 0;
static  method_get_name_t method_get_name = 0;
static  method_get_descriptor_t method_get_descriptor = 0;
static  method_get_bytecode_t method_get_bytecode = 0;
static  method_get_bytecode_length_t  method_get_bytecode_length = 0;
static  method_get_max_stack_t  method_get_max_stack = 0;
static  method_get_exc_handler_number_t  method_get_exc_handler_number = 0;
static  method_get_vtable_offset_t  method_get_vtable_offset = 0;
static  method_get_indirect_address_t  method_get_indirect_address = 0;
static  method_get_native_func_addr_t  method_get_native_func_addr = 0;
static  method_get_max_locals_t  method_get_max_locals = 0;
static  method_args_get_number_t  method_args_get_number = 0;
static  method_args_get_type_info_t method_args_get_type_info = 0;
static  method_ret_type_get_type_info_t  method_ret_type_get_type_info = 0;
static  method_get_signature_t method_get_signature = 0;
static  method_get_class_t  method_get_class = 0;
static  method_get_exc_handler_info_t method_get_exc_handler_info = 0;
static  method_get_code_block_addr_jit_new_t method_get_code_block_addr_jit_new = 0;
static  method_get_code_block_size_jit_new_t method_get_code_block_size_jit_new = 0;
static  method_get_side_effects_t  method_get_side_effects = 0;

static  method_has_annotation_t method_has_annotation = 0;
static  method_is_private_t  method_is_private = 0;
static  method_is_static_t  method_is_static = 0;
static  method_is_native_t  method_is_native = 0;
static  method_is_synchronized_t  method_is_synchronized = 0;
static  method_is_final_t  method_is_final = 0;
static  method_is_abstract_t  method_is_abstract = 0;
static  method_is_strict_t  method_is_strict = 0;
static  method_is_no_inlining_t  method_is_no_inlining = 0;


static  method_set_side_effects_t method_set_side_effects = 0;
static  method_set_num_target_handlers_t method_set_num_target_handlers = 0;
static  method_set_target_handler_info_t method_set_target_handler_info = 0;

static  method_lock_t  method_lock = 0;
static  method_unlock_t  method_unlock = 0;

static  method_allocate_code_block_t method_allocate_code_block = 0;
static  method_allocate_data_block_t method_allocate_data_block = 0;
static  method_allocate_info_block_t method_allocate_info_block = 0;
static  method_allocate_jit_data_block_t method_allocate_jit_data_block = 0;


//Object
static  object_get_vtable_offset_t  object_get_vtable_offset = 0;


//Resolve
static  resolve_class_t resolve_class = 0;
static  resolve_class_new_t resolve_class_new = 0;
static  resolve_special_method_t resolve_special_method = 0;
static  resolve_interface_method_t resolve_interface_method = 0;
static  resolve_static_method_t resolve_static_method = 0;
static  resolve_virtual_method_t resolve_virtual_method = 0;
static  resolve_nonstatic_field_t resolve_nonstatic_field = 0;
static  resolve_static_field_t resolve_static_field = 0;

//Type Info

static  type_info_create_from_java_descriptor_t type_info_create_from_java_descriptor = 0;
static  type_info_get_type_name_t type_info_get_type_name = 0;
static  type_info_get_class_t  type_info_get_class = 0;
static  type_info_get_class_no_exn_t  type_info_get_class_no_exn = 0;
static  type_info_get_num_array_dimensions_t  type_info_get_num_array_dimensions = 0;
static  type_info_get_type_info_t  type_info_get_type_info = 0;

static  type_info_is_void_t  type_info_is_void = 0;
static  type_info_is_reference_t  type_info_is_reference = 0;
static  type_info_is_resolved_t  type_info_is_resolved = 0;
static  type_info_is_primitive_t  type_info_is_primitive = 0;
static  type_info_is_vector_t  type_info_is_vector = 0;

//Vector
static  vector_get_first_element_offset_t  vector_get_first_element_offset = 0; //vector_first_element_offset_class_handle
static  vector_get_length_offset_t  vector_get_length_offset = 0; //vector_length_offset

//Vm
static  vm_tls_alloc_t  vm_tls_alloc = 0; //IDATA VMCALL hythread_tls_alloc(hythread_tls_key_t *handle) 
static  vm_tls_get_offset_t  vm_tls_get_offset = 0; //UDATA VMCALL hythread_tls_get_offset(hythread_tls_key_t key)
static  vm_tls_get_request_offset_t  vm_tls_get_request_offset = 0; //DATA VMCALL hythread_tls_get_request_offset
static  vm_tls_is_fast_t  vm_tls_is_fast = 0;//UDATA VMCALL hythread_uses_fast_tls
static  vm_get_tls_offset_in_segment_t  vm_get_tls_offset_in_segment = 0;//IDATA VMCALL hythread_get_hythread_offset_in_tls(void)

static  vm_properties_destroy_keys_t  vm_properties_destroy_keys = 0;//void vm_properties_destroy_keys(char** keys)
static  vm_properties_destroy_value_t  vm_properties_destroy_value = 0;//void vm_properties_destroy_value(char* value)
static  vm_properties_get_keys_t  vm_properties_get_keys = 0;//char** vm_properties_get_keys(PropertyTable table_number);
static  vm_properties_get_keys_starting_with_t vm_properties_get_keys_starting_with = 0;
static  vm_properties_get_value_t vm_properties_get_value = 0;//char* vm_properties_get_value(const char* key, PropertyTable table_number)


static  vm_get_system_object_class_t  vm_get_system_object_class = 0;
static  vm_get_system_class_class_t  vm_get_system_class_class = 0; // get_system_class_class
static  vm_get_system_string_class_t  vm_get_system_string_class = 0;
static  vm_get_vtable_base_address_t  vm_get_vtable_base_address = 0; //POINTER_SIZE_INT vm_get_vtable_base_address()
static  vm_get_heap_base_address_t  vm_get_heap_base_address = 0; //vm_get_heap_base_address
static  vm_get_heap_ceiling_address_t  vm_get_heap_ceiling_address = 0; //vm_get_heap_ceiling_address
static  vm_is_heap_compressed_t  vm_is_heap_compressed = 0;//vm_is_heap_compressed();
static  vm_is_vtable_compressed_t  vm_is_vtable_compressed = 0;//vm_is_vtable_compressed();
static  vm_patch_code_block_t vm_patch_code_block = 0;
static  vm_compile_method_t vm_compile_method = 0;
static  vm_register_jit_recompiled_method_callback_t vm_register_jit_recompiled_method_callback = 0;
static  vm_compiled_method_load_t vm_compiled_method_load = 0;
static  vm_helper_get_addr_t vm_helper_get_addr = 0;
static  vm_helper_get_addr_optimized_t vm_helper_get_addr_optimized = 0;
static  vm_helper_get_by_name_t  vm_helper_get_by_name = 0;
static  vm_helper_get_calling_convention_t  vm_helper_get_calling_convention = 0;
static  vm_helper_get_interruptibility_kind_t  vm_helper_get_interruptibility_kind = 0;
static  vm_helper_get_magic_helper_t  vm_helper_get_magic_helper = 0;
static  vm_helper_get_name_t vm_helper_get_name = 0;

//VTable
static  vtable_get_class_t  vtable_get_class = 0;

static vm_enumerate_root_reference_t vm_enumerate_root_reference = 0;
static vm_enumerate_compressed_root_reference_t vm_enumerate_compressed_root_reference = 0;
static vm_enumerate_root_interior_pointer_t vm_enumerate_root_interior_pointer = 0;

#undef GET_INTERFACE

#define GET_INTERFACE(get_adapter, func_name) \
    (func_name##_t)get_adapter(#func_name); assert(func_name)


    bool VMInterface::setVmAdapter(vm_adaptor_t a) {
        vm = a;
        allocation_handle_get_class = GET_INTERFACE(vm, allocation_handle_get_class);

        //Class
        class_get_array_element_class = GET_INTERFACE(vm, class_get_array_element_class);
        class_get_array_element_size = GET_INTERFACE(vm, class_get_array_element_size); 
        class_get_array_of_class = GET_INTERFACE(vm, class_get_array_of_class);
        class_is_non_ref_array = GET_INTERFACE(vm, class_is_non_ref_array);
        class_get_name = GET_INTERFACE(vm, class_get_name);
        class_get_super_class = GET_INTERFACE(vm, class_get_super_class);
        class_get_depth = GET_INTERFACE(vm, class_get_depth);
        class_get_vtable = GET_INTERFACE(vm, class_get_vtable);
        class_get_allocation_handle = GET_INTERFACE(vm, class_get_allocation_handle);
        class_get_object_size = GET_INTERFACE(vm, class_get_object_size);
        class_cp_get_num_array_dimensions = GET_INTERFACE(vm, class_cp_get_num_array_dimensions);
        class_get_class_of_primitive_type = GET_INTERFACE(vm, class_get_class_of_primitive_type);
        class_get_const_string_intern_addr = GET_INTERFACE(vm, class_get_const_string_intern_addr);
        class_cp_get_const_type = GET_INTERFACE(vm, class_cp_get_const_type);
        class_cp_get_const_addr = GET_INTERFACE(vm, class_cp_get_const_addr);
        class_get_method_by_name = GET_INTERFACE(vm, class_get_method_by_name);
        class_get_field_by_name = GET_INTERFACE(vm, class_get_field_by_name);
        class_get_class_loader = GET_INTERFACE(vm, class_get_class_loader);

        class_is_array = GET_INTERFACE(vm, class_is_array);
        class_is_enum = GET_INTERFACE(vm, class_is_enum);
        class_is_final = GET_INTERFACE(vm, class_is_final);
        class_is_throwable = GET_INTERFACE(vm, class_is_throwable);
        class_is_interface = GET_INTERFACE(vm, class_is_interface);
        class_is_abstract = GET_INTERFACE(vm, class_is_abstract);
        class_is_initialized = GET_INTERFACE(vm, class_is_initialized);
        class_is_finalizable = GET_INTERFACE(vm, class_is_finalizable);
        class_is_instanceof = GET_INTERFACE(vm, class_is_instanceof);
        class_is_support_fast_instanceof = GET_INTERFACE(vm, class_is_support_fast_instanceof);
        class_is_primitive = GET_INTERFACE(vm, class_is_primitive);

        vm_lookup_class_with_bootstrap = GET_INTERFACE(vm, vm_lookup_class_with_bootstrap);
        class_lookup_method_recursively = GET_INTERFACE(vm, class_lookup_method_recursively);

        // Const Pool
        class_cp_get_field_type = GET_INTERFACE(vm, class_cp_get_field_type);
        class_cp_get_entry_class_name = GET_INTERFACE(vm, class_cp_get_entry_class_name);
        class_cp_get_entry_name = GET_INTERFACE(vm, class_cp_get_entry_name);
        class_cp_get_entry_descriptor = GET_INTERFACE(vm, class_cp_get_entry_descriptor);
        class_cp_is_entry_resolved = GET_INTERFACE(vm, class_cp_is_entry_resolved);
        class_cp_get_class_name = GET_INTERFACE(vm, class_cp_get_class_name);


        //Field

        field_get_address = GET_INTERFACE(vm, field_get_address);
        field_get_class = GET_INTERFACE(vm, field_get_class);
        field_get_descriptor = GET_INTERFACE(vm, field_get_descriptor);
        field_get_name = GET_INTERFACE(vm, field_get_name);
        field_get_offset = GET_INTERFACE(vm, field_get_offset);
        field_get_type_info = GET_INTERFACE(vm, field_get_type_info);
        field_is_final = GET_INTERFACE(vm, field_is_final);
        field_is_magic = GET_INTERFACE(vm, field_is_magic);
        field_is_private = GET_INTERFACE(vm, field_is_private);
        field_is_static = GET_INTERFACE(vm, field_is_static);
        field_is_volatile = GET_INTERFACE(vm, field_is_volatile);

        //Method

        method_get_overriding_method = GET_INTERFACE(vm, method_get_overriding_method);
        method_get_info_block_jit = GET_INTERFACE(vm, method_get_info_block_jit);
        method_get_info_block_size_jit = GET_INTERFACE(vm, method_get_info_block_size_jit);
        method_get_name = GET_INTERFACE(vm, method_get_name);
        method_get_descriptor = GET_INTERFACE(vm, method_get_descriptor);
        method_get_bytecode = GET_INTERFACE(vm, method_get_bytecode);
        method_get_bytecode_length = GET_INTERFACE(vm, method_get_bytecode_length);
        method_get_max_stack = GET_INTERFACE(vm, method_get_max_stack);
        method_get_exc_handler_number = GET_INTERFACE(vm, method_get_exc_handler_number);
        method_get_vtable_offset = GET_INTERFACE(vm, method_get_vtable_offset);
        method_get_indirect_address = GET_INTERFACE(vm, method_get_indirect_address);
        method_get_native_func_addr = GET_INTERFACE(vm, method_get_native_func_addr);
        method_get_max_locals = GET_INTERFACE(vm, method_get_max_locals);
        method_args_get_number = GET_INTERFACE(vm, method_args_get_number);
        method_args_get_type_info = GET_INTERFACE(vm, method_args_get_type_info);
        method_ret_type_get_type_info = GET_INTERFACE(vm, method_ret_type_get_type_info);
        method_get_signature = GET_INTERFACE(vm, method_get_signature);
        method_get_class = GET_INTERFACE(vm, method_get_class);
        method_get_exc_handler_info = GET_INTERFACE(vm, method_get_exc_handler_info);
        method_get_code_block_addr_jit_new = GET_INTERFACE(vm, method_get_code_block_addr_jit_new);
        method_get_code_block_size_jit_new = GET_INTERFACE(vm, method_get_code_block_size_jit_new);
        method_get_side_effects = GET_INTERFACE(vm, method_get_side_effects);

        method_has_annotation = GET_INTERFACE(vm, method_has_annotation);
        method_is_private = GET_INTERFACE(vm, method_is_private);
        method_is_static = GET_INTERFACE(vm, method_is_static);
        method_is_native = GET_INTERFACE(vm, method_is_native);
        method_is_synchronized = GET_INTERFACE(vm, method_is_synchronized);
        method_is_final = GET_INTERFACE(vm, method_is_final);
        method_is_abstract = GET_INTERFACE(vm, method_is_abstract);
        method_is_strict = GET_INTERFACE(vm, method_is_strict);
        method_is_no_inlining = GET_INTERFACE(vm, method_is_no_inlining);


        method_set_side_effects = GET_INTERFACE(vm, method_set_side_effects);
        method_set_num_target_handlers = GET_INTERFACE(vm, method_set_num_target_handlers);
        method_set_target_handler_info = GET_INTERFACE(vm, method_set_target_handler_info);

        method_lock = GET_INTERFACE(vm, method_lock);
        method_unlock = GET_INTERFACE(vm, method_unlock);

        method_allocate_code_block = GET_INTERFACE(vm, method_allocate_code_block);
        method_allocate_data_block = GET_INTERFACE(vm, method_allocate_data_block);
        method_allocate_info_block = GET_INTERFACE(vm, method_allocate_info_block);
        method_allocate_jit_data_block = GET_INTERFACE(vm, method_allocate_jit_data_block);


        //Object
        object_get_vtable_offset = GET_INTERFACE(vm, object_get_vtable_offset);


        //Resolve
        resolve_class = GET_INTERFACE(vm, resolve_class);
        resolve_class_new = GET_INTERFACE(vm, resolve_class_new);
        resolve_special_method = GET_INTERFACE(vm, resolve_special_method);
        resolve_interface_method = GET_INTERFACE(vm, resolve_interface_method);
        resolve_static_method = GET_INTERFACE(vm, resolve_static_method);
        resolve_virtual_method = GET_INTERFACE(vm, resolve_virtual_method);
        resolve_nonstatic_field = GET_INTERFACE(vm, resolve_nonstatic_field);
        resolve_static_field = GET_INTERFACE(vm, resolve_static_field);

        //Type Info

        type_info_create_from_java_descriptor = GET_INTERFACE(vm, type_info_create_from_java_descriptor);
        type_info_get_type_name = GET_INTERFACE(vm, type_info_get_type_name);
        type_info_get_class = GET_INTERFACE(vm, type_info_get_class);
        type_info_get_class_no_exn = GET_INTERFACE(vm, type_info_get_class_no_exn);
        type_info_get_num_array_dimensions = GET_INTERFACE(vm, type_info_get_num_array_dimensions);
        type_info_get_type_info = GET_INTERFACE(vm, type_info_get_type_info);

        type_info_is_void = GET_INTERFACE(vm, type_info_is_void);
        type_info_is_reference = GET_INTERFACE(vm, type_info_is_reference);
        type_info_is_resolved = GET_INTERFACE(vm, type_info_is_resolved);
        type_info_is_primitive = GET_INTERFACE(vm, type_info_is_primitive);
        type_info_is_vector = GET_INTERFACE(vm, type_info_is_vector);

        //Vector
        vector_get_first_element_offset = GET_INTERFACE(vm, vector_get_first_element_offset);
        vector_get_length_offset = GET_INTERFACE(vm, vector_get_length_offset);

        //Vm
        vm_tls_alloc = GET_INTERFACE(vm, vm_tls_alloc);
        vm_tls_get_offset = GET_INTERFACE(vm, vm_tls_get_offset);
        vm_tls_get_request_offset = GET_INTERFACE(vm, vm_tls_get_request_offset);
        vm_tls_is_fast = GET_INTERFACE(vm, vm_tls_is_fast);
        vm_get_tls_offset_in_segment = GET_INTERFACE(vm, vm_get_tls_offset_in_segment);

        vm_properties_destroy_keys = GET_INTERFACE(vm, vm_properties_destroy_keys);
        vm_properties_destroy_value = GET_INTERFACE(vm, vm_properties_destroy_value);
        vm_properties_get_keys = GET_INTERFACE(vm, vm_properties_get_keys);
        vm_properties_get_keys_starting_with = GET_INTERFACE(vm, vm_properties_get_keys_starting_with);
        vm_properties_get_value = GET_INTERFACE(vm, vm_properties_get_value);


        vm_get_system_object_class = GET_INTERFACE(vm, vm_get_system_object_class);
        vm_get_system_class_class = GET_INTERFACE(vm, vm_get_system_class_class);
        vm_get_system_string_class = GET_INTERFACE(vm, vm_get_system_string_class);
        vm_get_vtable_base_address = GET_INTERFACE(vm, vm_get_vtable_base_address);
        vm_get_heap_base_address = GET_INTERFACE(vm, vm_get_heap_base_address);
        vm_get_heap_ceiling_address = GET_INTERFACE(vm, vm_get_heap_ceiling_address);
        vm_is_heap_compressed = GET_INTERFACE(vm, vm_is_heap_compressed);
        vm_is_vtable_compressed = GET_INTERFACE(vm, vm_is_vtable_compressed);
        vm_patch_code_block = GET_INTERFACE(vm, vm_patch_code_block);
        vm_compile_method = GET_INTERFACE(vm, vm_compile_method);
        vm_register_jit_recompiled_method_callback = GET_INTERFACE(vm, vm_register_jit_recompiled_method_callback);
        vm_compiled_method_load = GET_INTERFACE(vm, vm_compiled_method_load);
        vm_helper_get_addr = GET_INTERFACE(vm, vm_helper_get_addr);
        vm_helper_get_addr_optimized = GET_INTERFACE(vm, vm_helper_get_addr_optimized);
        vm_helper_get_by_name = GET_INTERFACE(vm, vm_helper_get_by_name);
        vm_helper_get_calling_convention = GET_INTERFACE(vm, vm_helper_get_calling_convention);
        vm_helper_get_interruptibility_kind = GET_INTERFACE(vm, vm_helper_get_interruptibility_kind);
        vm_helper_get_magic_helper = GET_INTERFACE(vm, vm_helper_get_magic_helper);
        vm_helper_get_name = GET_INTERFACE(vm, vm_helper_get_name);

        //VTable
        vtable_get_class = GET_INTERFACE(vm, vtable_get_class);

        vm_enumerate_root_reference = GET_INTERFACE(vm, vm_enumerate_root_reference);
        vm_enumerate_compressed_root_reference = GET_INTERFACE(vm, vm_enumerate_compressed_root_reference);
        vm_enumerate_root_interior_pointer = GET_INTERFACE(vm, vm_enumerate_root_interior_pointer);

        return true;
    }

    bool VMInterface::isValidFeature(const char* id) {
        return NULL != vm(id);
    }



// The JIT info block is laid out as:
//    header
//    stack info
//    GC info

U_8*
methodGetStacknGCInfoBlock(Method_Handle method, JIT_Handle jit)
{
    U_8* addr = method_get_info_block_jit(method, jit);
    addr += sizeof(void*);    // skip the header
    return addr;
}


U_32
methodGetStacknGCInfoBlockSize(Method_Handle method, JIT_Handle jit)
{
    U_32  size = method_get_info_block_size_jit(method, jit);
    return (size - sizeof(void *));     // skip the header
}

void*       
VMInterface::getTypeHandleFromVTable(void* vtHandle){
    return vtable_get_class((VTable_Handle)vtHandle);
}


// TODO: free TLS key on JIT deinitilization
U_32
VMInterface::flagTLSSuspendRequestOffset(){
    return (U_32)vm_tls_get_request_offset();
}

U_32
VMInterface::flagTLSThreadStateOffset() {
    static UDATA key = 0;
    static size_t offset = 0;
    if (key == 0) {
        vm_tls_alloc(&key);
        offset = vm_tls_get_offset(key);
    }
    assert(fit32(offset));
    return (U_32)offset;
}

I_32
VMInterface::getTLSBaseOffset() {
    return (I_32) vm_get_tls_offset_in_segment();
}

bool
VMInterface::useFastTLSAccess() {
    return 0 != vm_tls_is_fast();
}

bool
VMInterface::isVTableCompressed() {
    return vm_is_vtable_compressed();
}

bool
VMInterface::areReferencesCompressed() {
    return vm_is_heap_compressed();
}
//////////////////////////////////////////////////////////////////////////////
///////////////////////// VMTypeManager /////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//
// VM specific type manager
//
void*
TypeManager::getBuiltinValueTypeVMTypeHandle(Type::Tag type) {
    switch (type) {
    case Type::Void:    return class_get_class_of_primitive_type(VM_DATA_TYPE_VOID);
    case Type::Boolean: return class_get_class_of_primitive_type(VM_DATA_TYPE_BOOLEAN);
    case Type::Char:    return class_get_class_of_primitive_type(VM_DATA_TYPE_CHAR);
    case Type::Int8:    return class_get_class_of_primitive_type(VM_DATA_TYPE_INT8);
    case Type::Int16:   return class_get_class_of_primitive_type(VM_DATA_TYPE_INT16);
    case Type::Int32:   return class_get_class_of_primitive_type(VM_DATA_TYPE_INT32);
    case Type::Int64:   return class_get_class_of_primitive_type(VM_DATA_TYPE_INT64);
    case Type::IntPtr:  return class_get_class_of_primitive_type(VM_DATA_TYPE_INTPTR);
    case Type::UIntPtr: return class_get_class_of_primitive_type(VM_DATA_TYPE_UINTPTR);
    case Type::UInt8:   return class_get_class_of_primitive_type(VM_DATA_TYPE_UINT8);
    case Type::UInt16:  return class_get_class_of_primitive_type(VM_DATA_TYPE_UINT16);
    case Type::UInt32:  return class_get_class_of_primitive_type(VM_DATA_TYPE_UINT32);
    case Type::UInt64:  return class_get_class_of_primitive_type(VM_DATA_TYPE_UINT64);
    case Type::Single:  return class_get_class_of_primitive_type(VM_DATA_TYPE_F4);
    case Type::Double:  return class_get_class_of_primitive_type(VM_DATA_TYPE_F8);
    case Type::Float:   return NULL;
    case Type::TypedReference: assert(0);
    default:  break;
    }
    return NULL;
}

void 
VMInterface::rewriteCodeBlock(U_8* codeBlock, U_8*  newCode, size_t size) {
    vm_patch_code_block(codeBlock, newCode, size);
}

void*
VMInterface::getSystemObjectVMTypeHandle() {
    return vm_get_system_object_class();
}

void*
VMInterface::getSystemClassVMTypeHandle() {
    return vm_get_system_class_class();
}

void*
VMInterface::getSystemStringVMTypeHandle() {
    return vm_get_system_string_class();
}

void*
VMInterface::getArrayVMTypeHandle(void* elemVMTypeHandle,bool isUnboxed) {
    //if (isUnboxed)
    //    return class_get_array_of_unboxed((Class_Handle) elemVMTypeHandle);
    return class_get_array_of_class((Class_Handle) elemVMTypeHandle);
}

void*
VMInterface::getArrayElemVMTypeHandle(void* vmTypeHandle) {
    return class_get_array_element_class((Class_Handle) vmTypeHandle);
}

const char* VMInterface::getTypeName(void* vmTypeHandle) {
    return class_get_name((Class_Handle) vmTypeHandle);
}

bool
VMInterface::isArrayOfPrimitiveElements(void* vmClassHandle) {
    return class_is_non_ref_array((Class_Handle) vmClassHandle);
}

bool
VMInterface::isEnumType(void* vmTypeHandle) {
    return class_is_enum((Class_Handle) vmTypeHandle);
}

bool
VMInterface::isValueType(void* vmTypeHandle) {
    return class_is_primitive((Class_Handle) vmTypeHandle);
}

bool
VMInterface::isLikelyExceptionType(void* vmTypeHandle) {
    return class_is_throwable((Class_Handle) vmTypeHandle)?true:false;
}

bool        
VMInterface::getClassFastInstanceOfFlag(void* vmTypeHandle) {
    return class_is_support_fast_instanceof((Class_Handle) vmTypeHandle)?true:false;
}

int 
VMInterface::getClassDepth(void* vmTypeHandle) {
    return class_get_depth((Class_Handle) vmTypeHandle);
}

U_32
VMInterface::getArrayLengthOffset() {
    return vector_get_length_offset();
}

U_32
VMInterface::getArrayElemOffset(void* vmElemTypeHandle,bool isUnboxed) {
    //if (isUnboxed)
      //  return vector_first_element_offset_unboxed((Class_Handle) vmElemTypeHandle);
    return vector_get_first_element_offset((Class_Handle) vmElemTypeHandle);
}

bool
VMInterface::isSubClassOf(void* vmTypeHandle1,void* vmTypeHandle2) {
    if (vmTypeHandle1 == (void*)(POINTER_SIZE_INT)0xdeadbeef ||
        vmTypeHandle2 == (void*)(POINTER_SIZE_INT)0xdeadbeef ) {
        return false;
    }
    return class_is_instanceof((Class_Handle) vmTypeHandle1,(Class_Handle) vmTypeHandle2)?true:false;
}    

U_32
VMInterface::getArrayElemSize(void * vmTypeHandle) {
    return class_get_array_element_size((Class_Handle) vmTypeHandle);
}

U_32
VMInterface::getObjectSize(void * vmTypeHandle) {
    return class_get_object_size((Class_Handle) vmTypeHandle);
}

void*       VMInterface::getSuperTypeVMTypeHandle(void* vmTypeHandle) {
    return class_get_super_class((Class_Handle)vmTypeHandle);
}
bool        VMInterface::isArrayType(void* vmTypeHandle) {
    return class_is_array((Class_Handle)vmTypeHandle)?true:false;
}
bool        VMInterface::isFinalType(void* vmTypeHandle) {
    return class_is_final((Class_Handle)vmTypeHandle)?true:false;
}
bool        VMInterface::isInterfaceType(void* vmTypeHandle)  {
    return class_is_interface((Class_Handle)vmTypeHandle)?true:false;
}
bool        VMInterface::isAbstractType(void* vmTypeHandle) {
    return class_is_abstract((Class_Handle)vmTypeHandle)?true:false;
}
bool        VMInterface::needsInitialization(void* vmTypeHandle) {
    return class_is_initialized((Class_Handle)vmTypeHandle)?false:true;
}
bool        VMInterface::isFinalizable(void* vmTypeHandle) {
    return class_is_finalizable((Class_Handle)vmTypeHandle)?true:false;
}
bool        VMInterface::isInitialized(void* vmTypeHandle) {
    return class_is_initialized((Class_Handle)vmTypeHandle)?true:false;
}
void*       VMInterface::getVTable(void* vmTypeHandle) {
    return (void *) class_get_vtable((Class_Handle)vmTypeHandle);
}

//
// Allocation handle to be used with calls to runtime support functions for
// object allocation
//
void*       VMInterface::getAllocationHandle(void* vmTypeHandle) {
    return (void *) class_get_allocation_handle((Class_Handle) vmTypeHandle);
}

U_32      VMInterface::getVTableOffset()
{
    return object_get_vtable_offset();
}

void*
VMInterface::getVTableBase() {
    return vm_get_vtable_base_address();
}

void*       VMInterface::getTypeHandleFromAllocationHandle(void* vmAllocationHandle)
{
    return allocation_handle_get_class((Allocation_Handle)vmAllocationHandle);
}



//////////////////////////////////////////////////////////////////////////////
///////////////////////// MethodDesc //////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

MethodDesc::MethodDesc(Method_Handle m, JIT_Handle jit, CompilationInterface* ci, U_32 id)
: TypeMemberDesc(id, ci), drlMethod(m),
methodSig(method_get_signature(m)),
handleMap(NULL),
jitHandle(jit){}

const char*  MethodDesc::getName() const        {return method_get_name(drlMethod);}
const char*  MethodDesc::getSignatureString() const {return method_get_descriptor(drlMethod); }

bool         MethodDesc::isPrivate() const      {return method_is_private(drlMethod)?true:false;}
bool         MethodDesc::isStatic() const       {return method_is_static(drlMethod)?true:false;}
bool         MethodDesc::isInstance() const     {return method_is_static(drlMethod)?false:true;}
bool         MethodDesc::isNative() const       {return method_is_native(drlMethod)?true:false;}
bool         MethodDesc::isSynchronized() const {return method_is_synchronized(drlMethod)?true:false;}
bool         MethodDesc::isFinal() const        {return method_is_final(drlMethod)?true:false;}
bool         MethodDesc::isVirtual() const      {return isInstance() && !isPrivate();}
bool         MethodDesc::isAbstract() const     {return method_is_abstract(drlMethod)?true:false;}
// FP strict
bool         MethodDesc::isStrict() const       {return method_is_strict(drlMethod)?true:false;}
bool         MethodDesc::isClassInitializer() const {return strcmp(getName(), "<clinit>") == 0; }
bool         MethodDesc::isInstanceInitializer() const {return strcmp(getName(), "<init>") == 0; }

//
// Method info
//

const U_8*   MethodDesc::getByteCodes() const   {return method_get_bytecode(drlMethod);}
U_32       MethodDesc::getByteCodeSize() const {return (U_32) method_get_bytecode_length(drlMethod);}
uint16       MethodDesc::getMaxStack() const    {return (uint16) method_get_max_stack(drlMethod);}
U_32       MethodDesc::getNumHandlers() const {return method_get_exc_handler_number(drlMethod);}
U_32       MethodDesc::getOffset() const      {return method_get_vtable_offset(drlMethod);}
void*        MethodDesc::getIndirectAddress() const {return method_get_indirect_address(drlMethod);}
void*        MethodDesc::getNativeAddress() const {return method_get_native_func_addr(drlMethod);}

U_32    MethodDesc::getNumVars() const        {return method_get_max_locals(drlMethod);}

U_32    
MethodDesc::getNumParams() const {
    return method_args_get_number(methodSig);
}

Type*    
MethodDesc::getParamType(U_32 paramIndex) const {
    Type_Info_Handle typeHandle = method_args_get_type_info(methodSig,paramIndex);
    return compilationInterface->getTypeFromDrlVMTypeHandle(typeHandle);
}

Type*
MethodDesc::getReturnType() const {
    Type_Info_Handle typeHandle = method_ret_type_get_type_info(methodSig);
    return compilationInterface->getTypeFromDrlVMTypeHandle(typeHandle);
}

Class_Handle MethodDesc::getParentHandle() const {
    return method_get_class(drlMethod);
}

void MethodDesc::getHandlerInfo(unsigned short index,
                                unsigned short* beginOffset,
                                unsigned short* endOffset,
                                unsigned short* handlerOffset,
                                unsigned short* handlerClassIndex) const
{
    method_get_exc_handler_info(drlMethod,index,beginOffset,endOffset,handlerOffset,handlerClassIndex);
}

// accessors for method info, code and data
U_8* MethodDesc::getInfoBlock() const {
    return methodGetStacknGCInfoBlock(drlMethod, getJitHandle());
}

U_32       MethodDesc::getInfoBlockSize() const {
    return methodGetStacknGCInfoBlockSize(drlMethod, getJitHandle());
}

U_8* MethodDesc::getCodeBlockAddress(I_32 id) const {
    return method_get_code_block_addr_jit_new(drlMethod,getJitHandle(), id);
}

U_32       MethodDesc::getCodeBlockSize(I_32 id) const {
    return method_get_code_block_size_jit_new(drlMethod,getJitHandle(), id);
}

bool
MethodDesc::isNoInlining() const {
    return method_is_no_inlining(drlMethod)?true:false;
}    

bool
TypeMemberDesc::isParentClassIsLikelyExceptionType() const {
    Class_Handle ch = getParentHandle();
    return class_is_throwable(ch);
}

const char*
CompilationInterface::getSignatureString(MethodDesc* enclosingMethodDesc, U_32 methodToken) {
    Class_Handle enclosingDrlVMClass = enclosingMethodDesc->getParentHandle();
    return class_cp_get_entry_descriptor(enclosingDrlVMClass, (unsigned short)methodToken);
}

Method_Side_Effects
MethodDesc::getSideEffect() const {
    return method_get_side_effects(drlMethod);
}

void
MethodDesc::setSideEffect(Method_Side_Effects mse) {
    method_set_side_effects(drlMethod, mse);
}

void        
MethodDesc::setNumExceptionHandler(U_32 numHandlers) {
    method_set_num_target_handlers(drlMethod,getJitHandle(),numHandlers);
}

void
MethodDesc::setExceptionHandlerInfo(U_32 exceptionHandlerNumber,
                                    U_8*   startAddr,
                                    U_8*   endAddr,
                                    U_8*   handlerAddr,
                                    NamedType* exceptionType,
                                    bool   exceptionObjIsDead) 
{
    void* exn_handle;
    assert(exceptionType);
    if (exceptionType->isSystemObject() || exceptionType->isUnresolvedObject()) {
        exn_handle = NULL;
    } else {
        exn_handle = exceptionType->getRuntimeIdentifier();
    }
    method_set_target_handler_info(drlMethod,
        getJitHandle(),
        exceptionHandlerNumber,
        startAddr,
        endAddr,
        handlerAddr,
        (Class_Handle) exn_handle,
        exceptionObjIsDead ? TRUE : FALSE);
}


//////////////////////////////////////////////////////////////////////////////
///////////////////////// FieldDesc ///////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

const char*   FieldDesc::getName() const       {return field_get_name(drlField);}
const char*   FieldDesc::getSignatureString() const {return field_get_descriptor(drlField); }
bool          FieldDesc::isPrivate() const     {return field_is_private(drlField)?true:false;}
bool          FieldDesc::isStatic() const      {return field_is_static(drlField)?true:false;}
//
// this field is constant after it is initialized
// can only be mutated by constructor (instance fields) or
// type initializer (static fields)
//
bool          FieldDesc::isInitOnly() const     {return field_is_final(drlField)?true:false;}    
// accesses to field cannot be reordered or CSEed
bool          FieldDesc::isVolatile() const    {return field_is_volatile(drlField)?true:false;}
bool          FieldDesc::isMagic() const    {return field_is_magic(drlField)?true:false;}
void*         FieldDesc::getAddress() const    {return field_get_address(drlField);} // for static fields


Class_Handle FieldDesc::getParentHandle() const {
    return field_get_class(drlField);
}

NamedType*
TypeMemberDesc::getParentType()    {
    TypeManager& typeManager = compilationInterface->getTypeManager();
    Class_Handle parentClassHandle = getParentHandle();
    if (class_is_primitive(parentClassHandle)) {
        assert(0);
        return typeManager.getValueType(parentClassHandle);
    }
    return typeManager.getObjectType(parentClassHandle);
}

Type*
FieldDesc::getFieldType() {
    Type_Info_Handle typeHandle = field_get_type_info(drlField);
    return compilationInterface->getTypeFromDrlVMTypeHandle(typeHandle);
}

U_32
FieldDesc::getOffset() const {
    return field_get_offset(drlField);
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////// CompilationInterface /////////////////////////
//////////////////////////////////////////////////////////////////////////////

Type*
CompilationInterface::getTypeFromDrlVMTypeHandle(Type_Info_Handle typeHandle) {
    Type* type = NULL;
    if (type_info_is_void(typeHandle)) {
        // void return type
        type = typeManager.getVoidType();
    } else if (type_info_is_reference(typeHandle)) {
        bool lazy = typeManager.isLazyResolutionMode();
        if (lazy && !type_info_is_resolved(typeHandle)) {
            const char* kname = type_info_get_type_name(typeHandle);
            assert(kname!=NULL);
            bool forceResolve = VMMagicUtils::isVMMagicClass(kname);
            if (!forceResolve) {
                return typeManager.getUnresolvedObjectType();
            }
        }
        Class_Handle classHandle = type_info_get_class_no_exn(typeHandle);
        if (!classHandle) {
            assert(!lazy);
            return NULL;
        }
        type = typeManager.getObjectType(classHandle);
    } else if (type_info_is_primitive(typeHandle)) {
        // value type
        Class_Handle valueTypeHandle = type_info_get_class(typeHandle);
        if (!valueTypeHandle)
            return NULL;
        type = typeManager.getValueType(valueTypeHandle);
    } else if (type_info_is_vector(typeHandle)) {
        // vector
        bool lazy = typeManager.isLazyResolutionMode();
        if (lazy && !type_info_is_resolved(typeHandle)) {
            Type* elemType = typeManager.getUnresolvedObjectType();
            U_32 dims =  type_info_get_num_array_dimensions(typeHandle);
            Type* arrayType = NULL;
            while (dims!=0) {
                arrayType = typeManager.getArrayType(arrayType==NULL ? elemType : arrayType);
                dims--;
            }
            assert(arrayType!=NULL && arrayType->isArrayType());
            return arrayType;
        }
        Type_Info_Handle elemTypeInfo = type_info_get_type_info(typeHandle);
        Type* elemType = getTypeFromDrlVMTypeHandle(elemTypeInfo);
        if (!elemType) {
            assert(!lazy);
            return NULL;
        }
        type = typeManager.getArrayType(elemType);
    } else {
        // should not get here
        assert(0);
    }
    return type;
}

MethodDesc*
CompilationInterface::getMagicHelper(VM_RT_SUPPORT id){
    Method_Handle mh = vm_helper_get_magic_helper(id);
    return mh ? getMethodDesc(mh) : NULL;
}

const char*
CompilationInterface::getRuntimeHelperName(VM_RT_SUPPORT id){
    return vm_helper_get_name(id);
}

VM_RT_SUPPORT CompilationInterface::str2rid( const char * helperName ) {
    return vm_helper_get_by_name(helperName);
}

void*        
CompilationInterface::getRuntimeHelperAddress(VM_RT_SUPPORT id) {
    return vm_helper_get_addr(id);
}

void*        
CompilationInterface::getRuntimeHelperAddressForType(VM_RT_SUPPORT id, Type* type) {
    Class_Handle handle = NULL;
    if (type != NULL && type->isNamedType())
        handle = (Class_Handle) ((NamedType *)type)->getVMTypeHandle();
    void* addr = vm_helper_get_addr_optimized(id, handle);
    assert(addr != NULL);
    return addr;
}

HELPER_CALLING_CONVENTION 
CompilationInterface::getRuntimeHelperCallingConvention(VM_RT_SUPPORT id) {
    return vm_helper_get_calling_convention(id);
}

bool
CompilationInterface::isInterruptible(VM_RT_SUPPORT id)  {
    return INTERRUPTIBLE_ALWAYS == vm_helper_get_interruptibility_kind(id);
}

bool
CompilationInterface::mayBeInterruptible(VM_RT_SUPPORT id)  {
    return INTERRUPTIBLE_NEVER != vm_helper_get_interruptibility_kind(id);
}

bool
CompilationInterface::compileMethod(MethodDesc *method) {
    if (Log::isEnabled()) {
        Log::out() << "Jitrino requested compilation of " <<
            method->getParentType()->getName() << "::" <<
            method->getName() << method->getSignatureString() << ::std::endl;
    }
    JIT_Result res = vm_compile_method(getJitHandle(), method->getMethodHandle());
    return res == JIT_SUCCESS ? true : false;
}



const void* 
CompilationInterface::getStringInternAddr(MethodDesc* enclosingMethodDesc,
                                                U_32 stringToken) {
    Class_Handle enclosingDrlVMClass = enclosingMethodDesc->getParentHandle();
    return class_get_const_string_intern_addr(enclosingDrlVMClass,stringToken);
}

Type*
CompilationInterface::getConstantType(MethodDesc* enclosingMethodDesc,
                                         U_32 constantToken) {
    Class_Handle enclosingDrlVMClass = enclosingMethodDesc->getParentHandle();
    VM_Data_Type drlType = class_cp_get_const_type(enclosingDrlVMClass,constantToken);
    switch (drlType) {
    case VM_DATA_TYPE_STRING:   return typeManager.getSystemStringType(); 
    case VM_DATA_TYPE_CLASS:    return typeManager.getSystemClassType(); 
    case VM_DATA_TYPE_F8:   return typeManager.getDoubleType();
    case VM_DATA_TYPE_F4:    return typeManager.getSingleType();
    case VM_DATA_TYPE_INT32:      return typeManager.getInt32Type();
    case VM_DATA_TYPE_INT64:     return typeManager.getInt64Type();
    default: assert(0);
    }
    assert(0);
    return NULL;
}

const void*
CompilationInterface::getConstantValue(MethodDesc* enclosingMethodDesc,
                                          U_32 constantToken) {
    Class_Handle enclosingDrlVMClass = enclosingMethodDesc->getParentHandle();
    return class_cp_get_const_addr(enclosingDrlVMClass,constantToken);
}

MethodDesc*
CompilationInterface::getOverridingMethod(NamedType* type, MethodDesc *methodDesc) {
    if (type->isUnresolvedType()) {
        return NULL;
    }
    Method_Handle m = method_get_overriding_method((Class_Handle) type->getVMTypeHandle(),
                         methodDesc->getMethodHandle());
    if (!m)
        return NULL;
    return getMethodDesc(m);
}

void         CompilationInterface::setNotifyWhenMethodIsRecompiled(MethodDesc * methodDesc, 
                                                                      void * callbackData) {
    Method_Handle drlMethod = methodDesc->getMethodHandle();
    vm_register_jit_recompiled_method_callback(getJitHandle(),drlMethod, 
        getMethodToCompile()->getMethodHandle(), callbackData);
}

void CompilationInterface::sendCompiledMethodLoadEvent(MethodDesc* methodDesc, MethodDesc* outerDesc,
        U_32 codeSize, void* codeAddr, U_32 mapLength, 
        AddrLocation* addrLocationMap, void* compileInfo) {

    Method_Handle method = methodDesc->getMethodHandle();
    Method_Handle outer  = outerDesc->getMethodHandle();

    vm_compiled_method_load(method, codeSize, codeAddr, mapLength, addrLocationMap, compileInfo, outer); 
}

void * VMInterface::getHeapBase() {
    return vm_get_heap_base_address();
}

void * VMInterface::getHeapCeiling() {
    return vm_get_heap_ceiling_address();
}

ObjectType * CompilationInterface::findClassUsingBootstrapClassloader( const char * klassName ) {
    Class_Handle cls = vm_lookup_class_with_bootstrap(klassName);
    if( NULL == cls ) {
        return NULL;
    }
    return getTypeManager().getObjectType(cls);
};


MethodDesc* CompilationInterface::resolveMethod( ObjectType* klass, const char * methodName, const char * methodSig) {
    Class_Handle cls = (Class_Handle)klass->getVMTypeHandle();
    assert( NULL != cls );  
    Method_Handle mh = class_lookup_method_recursively( cls, methodName, methodSig);
    if( NULL == mh ) {
        return NULL;
    }
    return getMethodDesc(mh, NULL);
};

JIT_Handle
CompilationInterface::getJitHandle() const {
    return getCompilationContext()->getCurrentJITContext()->getJitHandle();
}


bool MethodDesc::hasAnnotation(NamedType* type) const {
    return method_has_annotation(drlMethod, (Class_Handle)type->getVMTypeHandle());
}

void FieldDesc::printFullName(::std::ostream &os) { 
    os<<getParentType()->getName()<<"::"<<field_get_name(drlField); 
}
void MethodDesc::printFullName(::std::ostream& os) {
    os<<getParentType()->getName()<<"::"<<getName()<<method_get_descriptor(drlMethod);
}

FieldDesc*    CompilationInterface::getFieldDesc(Field_Handle field) {
    FieldDesc* fieldDesc = fieldDescs->lookup(field);
    if (fieldDesc == NULL) {
        fieldDesc = new (memManager)
            FieldDesc(field,this,nextMemberId++);
        fieldDescs->insert(field,fieldDesc);
    }
    return fieldDesc;
}

MethodDesc*   CompilationInterface:: getMethodDesc(Method_Handle method, JIT_Handle jit) {
    assert(method);
    MethodDesc* methodDesc = methodDescs->lookup(method);
    if (methodDesc == NULL) {
        methodDesc = new (memManager)
            MethodDesc(method, jit, this, nextMemberId++);
        methodDescs->insert(method,methodDesc);
    }
    return methodDesc;
}

CompilationInterface::CompilationInterface(Compile_Handle c, 
                                           Method_Handle m, JIT_Handle jit, 
                                           MemoryManager& mm, OpenMethodExecutionParams& comp_params, 
                                           CompilationContext* cc, TypeManager& tpm) :
compilationContext(cc), memManager(mm),
typeManager(tpm), compilation_params(comp_params)
{
    fieldDescs = new (mm) PtrHashTable<FieldDesc>(mm,32);
    methodDescs = new (mm) PtrHashTable<MethodDesc>(mm,32);
    compileHandle = c;
    nextMemberId = 0;
    methodToCompile = NULL;
    methodToCompile = getMethodDesc(m, jit);
    flushToZeroAllowed = false;
}

void CompilationInterface::lockMethodData(void)    { 
    assert(methodToCompile);
    Method_Handle mh = methodToCompile->getMethodHandle();
    method_lock(mh);
}

void CompilationInterface::unlockMethodData(void)  { 
    assert(methodToCompile);
    Method_Handle mh = methodToCompile->getMethodHandle();
    method_unlock(mh);
}

U_8* CompilationInterface::allocateCodeBlock(size_t size, size_t alignment, CodeBlockHeat heat, I_32 id, 
bool simulate) {
    return method_allocate_code_block(methodToCompile->getMethodHandle(), getJitHandle(), 
        size, alignment, heat, id, simulate ? CAA_Simulate : CAA_Allocate);
}

U_8* CompilationInterface::allocateDataBlock(size_t size, size_t alignment) {
    return method_allocate_data_block(methodToCompile->getMethodHandle(),getJitHandle(),size, alignment);
}

U_8* CompilationInterface::allocateInfoBlock(size_t size) {
    size += sizeof(void *);
    U_8* addr = method_allocate_info_block(methodToCompile->getMethodHandle(),getJitHandle(),size);
    return (addr + sizeof(void *));
}

U_8* CompilationInterface::allocateJITDataBlock(size_t size, size_t alignment) {
    return method_allocate_jit_data_block(methodToCompile->getMethodHandle(),getJitHandle(),size, alignment);
}

MethodDesc*     CompilationInterface::getMethodDesc(Method_Handle method) {
    return getMethodDesc(method, getJitHandle());
}

static U_32 getArrayDims(Class_Handle cl, U_32 cpIndex) {
    return class_cp_get_num_array_dimensions(cl, (unsigned short)cpIndex);
}

static NamedType* getUnresolvedType(TypeManager& typeManager, Class_Handle enclClass, U_32 cpIndex) {
    U_32 arrayDims = getArrayDims(enclClass, cpIndex);
    NamedType * res = typeManager.getUnresolvedObjectType();
    while (arrayDims > 0) {
        res = typeManager.getArrayType(res);
        arrayDims --;
    }
    return res;
}

NamedType* CompilationInterface::resolveNamedType(Class_Handle enclClass, U_32 cpIndex) {
    //this method is allowed to use only for unresolved exception types
    Class_Handle ch = resolve_class(compileHandle,enclClass,cpIndex);
    if (ch == NULL) {
        return typeManager.getUnresolvedObjectType();
    }
    assert(!class_is_primitive(ch));
    ObjectType* res = typeManager.getObjectType(ch);    
    assert(res->isLikelyExceptionType()); //double check that this method is used only to resolve exception types when verifier is off
    return res;
}

NamedType* CompilationInterface::getNamedType(Class_Handle enclClass, U_32 cpIndex, ResolveNewCheck checkNew) {
    Class_Handle ch = NULL;
    if (typeManager.isLazyResolutionMode() && !class_cp_is_entry_resolved(enclClass, cpIndex)) {
        const char* className = class_cp_get_class_name(enclClass, cpIndex);
        bool forceResolve = VMMagicUtils::isVMMagicClass(className);
        if (!forceResolve) {
            return getUnresolvedType(typeManager, enclClass, cpIndex);
        }
    }
    if (checkNew == ResolveNewCheck_DoCheck) {
        ch = resolve_class_new(compileHandle,enclClass,cpIndex);
    } else {
        ch = resolve_class(compileHandle,enclClass,cpIndex);
    }
    if (ch == NULL) {
        return typeManager.getUnresolvedObjectType();
    }
    if (class_is_primitive(ch)) {
        return typeManager.getValueType(ch);
    }
    return typeManager.getObjectType(ch);    
}

Type* CompilationInterface::getTypeFromDescriptor(Class_Handle enclClass, const char* descriptor) {
    Class_Loader_Handle loader = class_get_class_loader(enclClass);
    Type_Info_Handle tih = type_info_create_from_java_descriptor(loader, descriptor);
    return getTypeFromDrlVMTypeHandle(tih);
}

MethodDesc* 
CompilationInterface::getSpecialMethod(Class_Handle enclClass, U_32 cpIndex) {
    Method_Handle res = NULL;
    bool lazy = typeManager.isLazyResolutionMode();
    if (!lazy || class_cp_is_entry_resolved(enclClass, cpIndex)) {
        res =  resolve_special_method(compileHandle,enclClass, cpIndex);
    }
    if (!res) return NULL;
    return getMethodDesc(res);
}    

MethodDesc* 
CompilationInterface::getInterfaceMethod(Class_Handle enclClass, U_32 cpIndex) {
    Method_Handle res = NULL;
    bool lazy = typeManager.isLazyResolutionMode();
    if (!lazy || class_cp_is_entry_resolved(enclClass, cpIndex)) {
        res =  resolve_interface_method(compileHandle,enclClass, cpIndex);
    }
    if (!res) return NULL;
    return getMethodDesc(res);
}    

MethodDesc* 
CompilationInterface::getStaticMethod(Class_Handle enclClass, U_32 cpIndex) {
    Method_Handle res = NULL;
    bool lazy = typeManager.isLazyResolutionMode();
    if (!lazy || class_cp_is_entry_resolved(enclClass, cpIndex)) {
        res =  resolve_static_method(compileHandle,enclClass, cpIndex);
    }
    if (!res) return NULL;
    return getMethodDesc(res);
}    

MethodDesc* 
CompilationInterface::getVirtualMethod(Class_Handle enclClass, U_32 cpIndex) {
    Method_Handle res = NULL;
    bool lazy = typeManager.isLazyResolutionMode();
    if (!lazy || class_cp_is_entry_resolved(enclClass, cpIndex)) {
        res =  resolve_virtual_method(compileHandle,enclClass, cpIndex);
    }
    if (!res) return NULL;
    return getMethodDesc(res);
}    


FieldDesc*  
CompilationInterface::getNonStaticField(Class_Handle enclClass, U_32 cpIndex, bool putfield) {
    Field_Handle res = NULL;
    bool lazy = typeManager.isLazyResolutionMode();
    if (!lazy || class_cp_is_entry_resolved(enclClass, cpIndex)) {
        res = resolve_nonstatic_field(compileHandle, enclClass, cpIndex, putfield);
    }
    if (!res) {
        return NULL;
    }
    return getFieldDesc(res);
}


FieldDesc*  
CompilationInterface::getStaticField(Class_Handle enclClass, U_32 cpIndex, bool putfield) {
    Field_Handle res = NULL;
    bool lazy = typeManager.isLazyResolutionMode();
    if (!lazy || class_cp_is_entry_resolved(enclClass, cpIndex)) {
        res = resolve_static_field(compileHandle, enclClass, cpIndex, putfield);
    }
    if (!res) {
        return NULL;
    }
    return getFieldDesc(res);
}

MethodDesc*  
CompilationInterface::getMethodByName(Class_Handle enclClass, const char* name) {
    Method_Handle res = class_get_method_by_name(enclClass, name);
    assert(res != NULL); // this functionality should be used only for those resolved for sure
    return getMethodDesc(res);
}

FieldDesc*  
CompilationInterface::getFieldByName(Class_Handle enclClass, const char* name) {
    Field_Handle res = class_get_field_by_name(enclClass, name);
    return res == NULL ? NULL : getFieldDesc(res);
}

Type*
CompilationInterface::getFieldType(Class_Handle enclClass, U_32 cpIndex) {
    VM_Data_Type drlType = class_cp_get_field_type(enclClass, (unsigned short)cpIndex);
    bool lazy = typeManager.isLazyResolutionMode();
    switch (drlType) {
        case VM_DATA_TYPE_BOOLEAN:  return typeManager.getBooleanType();
        case VM_DATA_TYPE_CHAR:     return typeManager.getCharType();
        case VM_DATA_TYPE_INT8:     return typeManager.getInt8Type();
        case VM_DATA_TYPE_INT16:    return typeManager.getInt16Type();
        case VM_DATA_TYPE_INT32:      return typeManager.getInt32Type();
        case VM_DATA_TYPE_INT64:     return typeManager.getInt64Type();
        case VM_DATA_TYPE_F8:   return typeManager.getDoubleType();
        case VM_DATA_TYPE_F4:    return typeManager.getSingleType();
        case VM_DATA_TYPE_ARRAY:
        
        case VM_DATA_TYPE_CLASS:    
                if (lazy) {
                    const char* fieldTypeName = class_cp_get_entry_descriptor(enclClass, cpIndex);
                    assert(fieldTypeName);
                    return getTypeFromDescriptor(enclClass, fieldTypeName);
                } 
                return typeManager.getUnresolvedObjectType();

        case VM_DATA_TYPE_VOID:     // class_cp_get_field_type can't return VOID
        case VM_DATA_TYPE_STRING:   // class_cp_get_field_type can't return STRING
        default: assert(0);
    }
    assert(0);
    return NULL;
}

const char* 
CompilationInterface::getMethodName(Class_Handle enclClass, U_32 cpIndex) {
    return class_cp_get_entry_name(enclClass, cpIndex);
}

const char* 
CompilationInterface::getMethodClassName(Class_Handle enclClass, U_32 cpIndex) {
    return class_cp_get_entry_class_name(enclClass, cpIndex);
}

const char* 
CompilationInterface::getFieldSignature(Class_Handle enclClass, U_32 cpIndex) {
    return class_cp_get_entry_descriptor(enclClass, cpIndex);
}

::std::ostream& operator<<(::std::ostream& os, Method_Handle method) { 
    os << class_get_name(method_get_class(method)) << "." 
        << method_get_name(method) << method_get_descriptor(method);
    return os;
}

VMPropertyIterator::VMPropertyIterator(
    MemoryManager& mm, const char* prefix): mm(mm), key(NULL), value(NULL), iterator(0)
{
    keys = vm_properties_get_keys_starting_with(prefix, VM_PROPERTIES);
}

VMPropertyIterator::~VMPropertyIterator() {
    vm_properties_destroy_keys(keys);
}

bool
VMPropertyIterator::next() {
    if (!keys[iterator]) {
        return false;
    }
    key = mm.copy(keys[iterator]);
    char* nval = vm_properties_get_value(keys[iterator++], VM_PROPERTIES);
    value = mm.copy(nval);
    vm_properties_destroy_value(nval);
    return true;
}

void GCInterface::enumerateRootReference(void** reference) {
    vm_enumerate_root_reference(reference, FALSE);
}

void GCInterface::enumerateCompressedRootReference(U_32* reference) {
    vm_enumerate_compressed_root_reference(reference, FALSE);
}

void GCInterface::enumerateRootManagedReference(void** slotReference, size_t slotOffset) {
    vm_enumerate_root_interior_pointer(slotReference, slotOffset, FALSE);
}

void ThreadDumpEnumerator::enumerateRootReference(void** reference) {
    //vm_check_if_monitor(reference, 0, 0, 0, FALSE, 1);
}

void ThreadDumpEnumerator::enumerateCompressedRootReference(U_32* reference) {
    //vm_check_if_monitor(0, 0, reference, 0, FALSE, 2);
}

void ThreadDumpEnumerator::enumerateRootManagedReference(void** slotReference, size_t slotOffset) {
    //vm_check_if_monitor(slotReference, 0, 0, slotOffset, FALSE, 3);
}

} //namespace Jitrino
