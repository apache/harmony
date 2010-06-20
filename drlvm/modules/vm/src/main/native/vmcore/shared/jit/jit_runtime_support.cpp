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
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include <float.h>
#include <math.h>
#include <stdlib.h>

#include "open/gc.h"
#include "vtable.h"
#include "open/types.h"
#include "open/bytecodes.h"
#include "open/vm_class_manipulation.h"

#include "Class.h"
#include "environment.h"
#include "exceptions.h"
#include "exceptions_jit.h"
#include "ini.h"
#include "jit_runtime_support.h"
#include "jit_runtime_support_common.h"
#include "jni_utils.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "m2n.h"
#include "object_layout.h"
#include "object_handles.h"
#include "vm_arrays.h"
#include "vm_strings.h"
#include "vm_threads.h"
#include "open/vm_ee.h"

#include "jvmti_interface.h"
#include "compile.h"

#include "dump.h"
#include "vm_stats.h"
#include "port_threadunsafe.h"

// macro that gets the offset of a certain field within a struct or class type
#define OFFSET(Struct, Field) \
    ((POINTER_SIZE_SINT) (&(((Struct *) NULL)->Field) - NULL))

#define OFFSET_INST(inst_ptr, Field) \
    ((POINTER_SIZE_SINT) ((char*)(&inst_ptr->Field) - (char*)inst_ptr))

// macro that gets the size of a field within a struct or class
#define SIZE(Struct, Field) \
    (sizeof(((Struct *) NULL)->Field))

//////////////////////////////////////////////////////////////////////////
// Object Creation

///////////////////////////////////////////////////////////
// New Object and New Array
Vector_Handle vm_rt_new_vector(Class *vector_class, int length) {
    ASSERT_THROW_AREA;
    Vector_Handle result;
    BEGIN_RAISE_AREA;
    result = vm_new_vector(vector_class, length);
    END_RAISE_AREA;
    exn_rethrow_if_pending();
    return result;
}

Vector_Handle
vm_rt_multianewarray_recursive(Class    *c,
                             int      *dims_array,
                             unsigned  dims)
{
    ASSERT_THROW_AREA;
    Vector_Handle result;
    BEGIN_RAISE_AREA;
    result = vm_multianewarray_recursive(c, dims_array, dims);
    END_RAISE_AREA;
    exn_rethrow_if_pending();
    return result;
}

Vector_Handle vm_rt_new_vector_using_vtable_and_thread_pointer(
        int length, Allocation_Handle vector_handle, void *tp) {
    ASSERT_THROW_AREA;
    Vector_Handle result;
    BEGIN_RAISE_AREA;
    result = vm_new_vector_using_vtable_and_thread_pointer(length, vector_handle, tp);
    END_RAISE_AREA;
    exn_rethrow_if_pending();
    return result;
}

// Create a multidimensional array
// Doesn't return if exception occured
Vector_Handle rth_multianewarrayhelper();

// Multianewarray helper
static NativeCodePtr rth_get_lil_multianewarray(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall::ref;");
        assert(cs);
        if (dyn_count) {
            cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
            assert(cs);
        }
        cs = lil_parse_onto_end(cs,
            "push_m2n 0, %0i;"
            "out platform::ref;"
            "call %1i;"
            "pop_m2n;"
            "ret;",
            (UDATA)FRAME_POPABLE,
            rth_multianewarrayhelper);
        assert(cs && lil_is_valid(cs));
        
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "multianewarray", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

///////////////////////////////////////////////////////////
// Load Constant String or Class

static ManagedObject * rth_ldc_ref_helper(Class *c, unsigned cp_index) 
    {
    ASSERT_THROW_AREA;
    ConstantPool& cp = c->get_constant_pool();
    if(cp.is_string(cp_index))
    {
        return vm_instantiate_cp_string_slow(c, cp_index);
    } 
    else if (cp.is_class(cp_index))
    {
        assert(!hythread_is_suspend_enabled());
 
        Class *objClass = NULL;
        BEGIN_RAISE_AREA;
        hythread_suspend_enable();
        objClass = c->_resolve_class(VM_Global_State::loader_env, cp_index);
        hythread_suspend_disable();
        END_RAISE_AREA;
 
        if (objClass) {
            return struct_Class_to_java_lang_Class(objClass);
        }
        if (!exn_raised()) {
            BEGIN_RAISE_AREA;
            class_throw_linking_error(c, cp_index, 0);
            END_RAISE_AREA;
        } else {
            exn_rethrow();
        }
    }
    exn_throw_by_name("java/lang/InternalError", "Unsupported ldc argument");
    return NULL;
}

static NativeCodePtr rth_get_lil_ldc_ref(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
        const POINTER_SIZE_INT next_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;
        const unsigned handles_size = (unsigned)(sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*16);
        const unsigned cap_and_size = (unsigned)((0<<16) | 16);
        ManagedObject* (*p_instantiate_ref)(Class*,unsigned) = rth_ldc_ref_helper;
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint,g4:ref;");
        assert(cs);
        if (dyn_count) {
            cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
            assert(cs);
        }
#ifdef _IPF_
         cs = lil_parse_onto_end(cs,
            "push_m2n 0, %0i;"
            "in2out platform:ref;"
            "call %1i;"
            "pop_m2n;"
            "ret;",
            (UDATA)FRAME_POPABLE,
            p_instantiate_ref);
#else
        cs = lil_parse_onto_end(cs,
            "push_m2n 0, %0i, handles;"
            "locals 1;"
            "alloc l0, %1i;"
            "st[l0+%2i:g4], %3i;"
            "st[l0+%4i:pint], 0;"
            "handles=l0;"
            "in2out platform:ref;"
            "call %5i;"
            "pop_m2n;"
            "ret;",
            (UDATA)FRAME_POPABLE,
            (UDATA)handles_size, (UDATA)cap_off, (UDATA)cap_and_size, (UDATA)next_off,
            p_instantiate_ref);
#endif
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "ldc_ref", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

///////////////////////////////////////////////////////////
// Checkcast and Instanceof

// The generic type test sequence first checks for whether the fast scheme
// can be used, if so it uses it, otherwise it calls into the VM to do the test.
// This is parameterized by:
//   * Is there a null test, if so is null in the type or not
//   * Should the test outcome be returned or should an exception be thrown on failure, object returned on success
//   * Is the type known
//   * Is the sequence for a stub or for inlining (affects how exceptions are delt with)
//   * Stats stuff: dyn_count for the dynamic call counter, and a stats update function with prototype void (*)(ManagedObject*, Class*)
// The object is in i0, and class in i1 (even if fixed unless there is no stats updater function)

enum RthTypeTestNull { RTTN_NoNullCheck, RTTN_NullMember, RTTN_NullNotMember };
enum RthTypeTestResult { RTTR_ReturnOutcome, RTTR_ThrowStub, RTTR_ThrowInline };
typedef void (*RthTypeTestStatsUpdate)(ManagedObject*, Class*);

static LilCodeStub* rth_gen_lil_type_test(LilCodeStub* cs, RthTypeTestNull null,
                                          RthTypeTestResult res, Class* type)
{
    // We need to get the vtable in more than one place below
    // Here are some macros to helper smooth over the compressed vtables
    const POINTER_SIZE_INT vtable_off = object_get_vtable_offset();
    const POINTER_SIZE_INT vtable_add = vm_is_vtable_compressed() ? (POINTER_SIZE_INT)vm_get_vtable_base_address() : 0;

    // Setup locals
    cs = lil_parse_onto_end(cs, (type ? "locals 1;" : "locals 2;"));
    assert(cs);

    // Null check
    if (null!=RTTN_NoNullCheck) {
        if (null==RTTN_NullMember) {
            cs = lil_parse_onto_end(cs, "jc i0!=%0i:ref,null_check_failed;",
                VM_Global_State::loader_env->managed_null);
            cs = lil_parse_onto_end(cs, (res==RTTR_ReturnOutcome ? "r=1:g4; ret;" : "r=i0; ret;"));
            cs = lil_parse_onto_end(cs, ":null_check_failed;");
        } else {
            cs = lil_parse_onto_end(cs, "jc i0=%0i:ref,failed;",
                VM_Global_State::loader_env->managed_null);
        }
        assert(cs);
    }

    // Fast sequence
    const size_t is_fast_off = Class::get_offset_of_fast_instanceof_flag();
    const size_t depth_off = Class::get_offset_of_depth();
    const POINTER_SIZE_INT supertable_off = (POINTER_SIZE_INT)&((VTable*)NULL)->superclasses;
    bool do_slow = true;
    if (type) {
        if(type->get_fast_instanceof_flag()) {
            cs = lil_parse_onto_end(cs,
                vm_is_vtable_compressed() ? "ld l0,[i0+%0i:g4],zx;" : "ld l0,[i0+%0i:pint];",
                vtable_off);            
            assert(cs);
            cs = lil_parse_onto_end(cs,
                "ld l0,[l0+%0i:pint];"
                "jc l0!=%1i,failed;",
                vtable_add+supertable_off+sizeof(Class*)*(type->get_depth()-1), type);
            do_slow = false;
            assert(cs);
        }
    } else {
        cs = lil_parse_onto_end(cs,
            "ld l0,[i1+%0i:g4];"
            "jc l0=0,slowpath;"
            "ld l1,[i1+%1i:g4],zx;",
            is_fast_off, depth_off);
        assert(cs);
        cs = lil_parse_onto_end(cs,
            vm_is_vtable_compressed() ? "ld l0,[i0+%0i:g4],zx;" : "ld l0,[i0+%0i:pint];",
            vtable_off);
        assert(cs);
        cs = lil_parse_onto_end(cs,
            "ld l0,[l0+%0i*l1+%1i:pint];"
            "jc l0!=i1,failed;",
            (UDATA)sizeof(Class*),
            vtable_add+supertable_off-sizeof(Class*));  // -4/8 because we want to index with depth-1
        assert(cs);
    }

    //*** Success, before slow path
    cs = lil_parse_onto_end(cs, (res==RTTR_ReturnOutcome ? "r=1:g4; ret;" : "r=i0; ret;"));
    assert(cs);

    //*** Slow sequence
    const POINTER_SIZE_INT clss_off = (POINTER_SIZE_INT)&((VTable*)NULL)->clss;
    Boolean (*p_subclass)(Class_Handle, Class_Handle) = class_is_subtype;
    if (do_slow) {
        cs = lil_parse_onto_end(cs,
            ":slowpath;"
            "out platform:pint,pint:g4;");
        assert(cs);
        cs = lil_parse_onto_end(cs,
            vm_is_vtable_compressed() ? "ld l0,[i0+%0i:g4],zx;" : "ld l0,[i0+%0i:pint];",
            vtable_off);
        assert(cs);
        cs = lil_parse_onto_end(cs,
            "ld o0,[l0+%0i:pint];"
            "o1=i1;"
            "call %1i;",
            vtable_add+clss_off, p_subclass);
        assert(cs);
        if (res==RTTR_ReturnOutcome) {
            cs = lil_parse_onto_end(cs, "ret;");
        } else {
            cs = lil_parse_onto_end(cs, "jc r=0,failed;");
            cs = lil_parse_onto_end(cs, "r=i0; ret;");
        }
        assert(cs);
    }

    //*** Failure
    if (res==RTTR_ReturnOutcome) {
        cs = lil_parse_onto_end(cs, ":failed; r=0:g4; ret;");
    } else {
        GenericFunctionPointer thrw = lil_npc_to_fp(exn_get_rth_throw_class_cast_exception());
        cs = lil_parse_onto_end(cs, (res==RTTR_ThrowStub ? ":failed; tailcall %0i;" : ":failed; call.noret %0i;"), thrw);
    }
    assert(cs && lil_is_valid(cs));

    return cs;
}

#ifdef VM_STATS
// General stats update
static void rth_type_test_update_stats(VTable* sub, Class* super)
{
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_type_checks ++;
    if (sub->clss == super)
        VM_Statistics::get_vm_stats().num_type_checks_equal_type ++;
    if (super->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_type_checks_fast_decision ++;
    else if (super->is_array())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_array ++;
    else if (super->is_interface())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_interface ++;
    else if (super->get_depth() >= vm_max_fast_instanceof_depth())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_too_deep ++;
    UNSAFE_REGION_END
}

// Checkcast stats update
static void rth_update_checkcast_stats(ManagedObject* o, Class* super)
{
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_checkcast ++;
    if (o == (ManagedObject*)VM_Global_State::loader_env->managed_null) {
        VM_Statistics::get_vm_stats().num_checkcast_null++;
    } else {
        if (o->vt()->clss == super)
            VM_Statistics::get_vm_stats().num_checkcast_equal_type ++;
        if (super->get_fast_instanceof_flag())
            VM_Statistics::get_vm_stats().num_checkcast_fast_decision ++;
        rth_type_test_update_stats(o->vt(), super);
    }
    UNSAFE_REGION_END
}

// Instanceof stats update
static void rth_update_instanceof_stats(ManagedObject* o, Class* super)
{
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_instanceof++;
    super->instanceof_slow_path_taken();
    if (o == (ManagedObject*)VM_Global_State::loader_env->managed_null) {
        VM_Statistics::get_vm_stats().num_instanceof_null++;
    } else {
        if (o->vt()->clss == super)
            VM_Statistics::get_vm_stats().num_instanceof_equal_type ++;
        if (super->get_fast_instanceof_flag())
            VM_Statistics::get_vm_stats().num_instanceof_fast_decision ++;
        rth_type_test_update_stats(o->vt(), super);
    }
    UNSAFE_REGION_END
}
#endif // VM_STATS

// Checkcast helper
static NativeCodePtr rth_get_lil_checkcast(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:ref,pint:ref;");

#ifdef VM_STATS
        if (dyn_count) {
            cs = lil_parse_onto_end(cs,
                "inc [%0i:pint]; in2out platform:void; call %1i;",
                dyn_count, rth_update_checkcast_stats);
            assert(cs);
        }
#endif

        cs = rth_gen_lil_type_test(cs, RTTN_NullMember, RTTR_ThrowStub, NULL);
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "rth_checkcast", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

// Instanceof Helper
static NativeCodePtr rth_get_lil_instanceof(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:ref,pint:g4;");
#ifdef VM_STATS
        assert(dyn_count);
        cs = lil_parse_onto_end(cs, "inc [%0i:pint]; in2out platform:void; call %1i;", dyn_count, rth_update_instanceof_stats);
        assert(cs);
#endif
        cs = rth_gen_lil_type_test(cs, RTTN_NullNotMember, RTTR_ReturnOutcome, NULL);
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "rth_instanceof", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

///////////////////////////////////////////////////////////
// Array Stores

// Store a reference into an array at a given index and return NULL,
// or return the Class* for the exception to throw.
static Class* rth_aastore(Vector_Handle array, int idx, ManagedObject* elem)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_aastore ++;
#endif // VM_STATS

    Global_Env *env = VM_Global_State::loader_env;
    ManagedObject* null_ref = (ManagedObject*)VM_Global_State::loader_env->managed_null;
    if (array == null_ref) {
        return env->java_lang_NullPointerException_Class;
    } else if (((U_32)idx) >= (U_32)get_vector_length(array)) {
        return env->java_lang_ArrayIndexOutOfBoundsException_Class;
    } else if (elem != null_ref) {
        assert(get_vector_vtable(array));
        Class* array_class = get_vector_vtable(array)->clss;
        assert(array_class);
        assert(array_class->is_array());
#ifdef VM_STATS
        VTable* vt = get_vector_vtable(array);
        if (vt == cached_object_array_vtable_ptr)
            VM_Statistics::get_vm_stats().num_aastore_test_object_array++;
        if (elem->vt()->clss == array_class->get_array_element_class())
            VM_Statistics::get_vm_stats().num_aastore_equal_type ++;
        if (array_class->get_array_element_class()->get_fast_instanceof_flag())
            VM_Statistics::get_vm_stats().num_aastore_fast_decision ++;
#endif // VM_STATS
        if (class_is_subtype_fast(elem->vt(), array_class->get_array_element_class())) {
            STORE_REFERENCE((ManagedObject*)array, get_vector_element_address_ref(array, idx), (ManagedObject*)elem);
        } else {
            return env->java_lang_ArrayStoreException_Class;
        }
    } else {
        // elem is null. We don't have to check types for a null reference.
        // We also don't have to record stores of null references.
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_aastore_null ++;
#endif // VM_STATS

        ManagedObject** elem_ptr = get_vector_element_address_ref(array, idx);
        REF_INIT_BY_ADDR(elem_ptr, NULL);
    }
    return NULL;
} //rth_aastore

static NativeCodePtr rth_get_lil_aastore(int * dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        Class* (*p_aastore)(Vector_Handle, int, ManagedObject*) = rth_aastore;
        // The args are the array to store into, the index, and the element ref to store\n"
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:ref,pint,ref:void;");
#ifdef VM_STATS
        assert(dyn_count);
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
#endif
        cs = lil_parse_onto_end(cs,
            "in2out platform:pint;"
            // rth_aastore either returns NULL or the ClassHandle of an exception to throw \n
            "call %0i;"
            "jc r!=0,aastore_failed;"
            "ret;"
            ":aastore_failed;"
            "std_places 1;"
            "sp0=r;"
            "tailcall %1i;",
            p_aastore,
            lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "rth_aastore", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

static bool rth_aastore_test(ManagedObject* elem, Vector_Handle array)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_aastore_test++;
#endif // VM_STATS

    ManagedObject* null_ref = (ManagedObject*)VM_Global_State::loader_env->managed_null;
    if (array == null_ref) {
        return false;
    }
    if (elem == null_ref) {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_aastore_test_null++;
#endif // VM_STATS
        return true;
    }

    VTable* vt = get_vector_vtable(array);
    if (vt == cached_object_array_vtable_ptr) {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_aastore_test_object_array++;
#endif // VM_STATS
        return true;
    }

    Class* array_class = vt->clss;
    assert(array_class);
    assert(array_class->is_array());

#ifdef VM_STATS
    if (elem->vt()->clss == array_class->get_array_element_class())
        VM_Statistics::get_vm_stats().num_aastore_test_equal_type ++;
    if (array_class->get_array_element_class()->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_aastore_test_fast_decision ++;
#endif // VM_STATS
    return (class_is_subtype_fast(elem->vt(), array_class->get_array_element_class()) ? true : false);
} //rth_aastore_test

static NativeCodePtr rth_get_lil_aastore_test(int * dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        bool (*p_aastore_test)(ManagedObject*, Vector_Handle) = rth_aastore_test;
        // The args are the element ref to store and the array to store into\n
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:ref,ref:void;");
        assert(cs);
#ifdef VM_STATS
        assert(dyn_count);
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
#endif
        cs = lil_parse_onto_end(cs,
            "in2out platform:pint;"
            "call %0i;"
            "ret;",
            p_aastore_test);
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "rth_aastore_test", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}


//////////////////////////////////////////////////////////////////////////
// Misc

///////////////////////////////////////////////////////////
// Throw linking exception helper

static NativeCodePtr rth_get_lil_throw_linking_exception(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        void (*p_throw_linking_error)(Class_Handle ch, unsigned index, unsigned opcode) =
            vm_rt_class_throw_linking_error;
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint,g4,g4:void;");
        assert(cs);
        if (dyn_count) {
            cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
            assert(cs);
        }
        cs = lil_parse_onto_end(cs,
            "push_m2n 0, 0;"
            "m2n_save_all;"
            "in2out platform:void;"
            "call.noret %0i;",
            p_throw_linking_error);
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "rth_throw_linking_exception", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
} // rth_get_lil_throw_linking_exception

///////////////////////////////////////////////////////////
// Get interface vtable

// Return the interface vtable for interface iid within object obj
// Or NULL if no such interface exists for obj
static void* rth_get_interface_vtable(ManagedObject* obj, Class* iid)
{
    assert(obj && obj->vt() && obj->vt()->clss);
    return Class::helper_get_interface_vtable(obj, iid);
}

// Get interface vtable helper
static NativeCodePtr rth_get_lil_get_interface_vtable(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        void* (*p_get_ivtable)(ManagedObject*, Class*) = rth_get_interface_vtable;
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:ref,pint:pint;");
        assert(cs);
        if (dyn_count) {
            cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
            assert(cs);
        }
        cs = lil_parse_onto_end(cs,
            "jc i0=%0i:ref,null;"
            "in2out platform:pint;"
            "call %1i;"
            "jc r=0,notfound;"
            "ret;"
            ":notfound;"
            "tailcall %2i;"
            ":null;"
            "r=0;"
            "ret;",
            VM_Global_State::loader_env->managed_null, p_get_ivtable,
            lil_npc_to_fp(exn_get_rth_throw_incompatible_class_change_exception()));
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "rth_get_interface_vtable", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

///////////////////////////////////////////////////////////
// Class Initialize

// Is a class initialized
static POINTER_SIZE_INT is_class_initialized(Class *clss)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_is_class_initialized++;
    clss->initialization_checked();
#endif // VM_STATS
    assert(!hythread_is_suspend_enabled());
    return clss->is_initialized();
} //is_class_initialized

void vm_rt_class_initialize(Class *clss)
{
    ASSERT_THROW_AREA;
    BEGIN_RAISE_AREA;
    class_initialize(clss);
    END_RAISE_AREA;
    exn_rethrow_if_pending();
}

// It's a rutime helper. So exception throwed from it.
void vm_rt_class_throw_linking_error(Class_Handle ch, unsigned index, unsigned opcode)
{
    ASSERT_THROW_AREA;
    BEGIN_RAISE_AREA;
    class_throw_linking_error(ch, index, opcode);
    END_RAISE_AREA;
    exn_rethrow_if_pending();
}


// Initialize class helper
static NativeCodePtr rth_get_lil_initialize_class(int* dyn_count)
{
    static NativeCodePtr addr = NULL;

    if (!addr) {
        POINTER_SIZE_INT (*p_is_inited)(Class*) = is_class_initialized;
        void (*p_init)(Class*) = class_initialize;
        void (*p_rethrow)() = exn_rethrow;
        LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint:void;");
        assert(cs);
#ifdef VM_STATS
        assert(dyn_count);
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
#endif 
        cs = lil_parse_onto_end(cs,
            "in2out platform:pint;"
            "call %0i;"
            "jc r=0,not_initialized;"
            "ret;"
            ":not_initialized;"
            "push_m2n 0, %1i;"
            "m2n_save_all;"
            "in2out platform:void;"
            "call %2i;"
            "locals 2;"
            "l0 = ts;"
            "ld l1, [l0 + %3i:ref];"
            "jc l1 != 0,_exn_raised;"
            "ld l1, [l0 + %4i:ref];"
            "jc l1 != 0,_exn_raised;"
            "pop_m2n;"
            "ret;"
            ":_exn_raised;"
            "out platform::void;"
            "call.noret %5i;",
            p_is_inited, (UDATA)(FRAME_JNI | FRAME_POPABLE), p_init,
            OFFSET(VM_thread, thread_exception.exc_object),
            OFFSET(VM_thread, thread_exception.exc_class),
            p_rethrow);
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);
        
        DUMP_STUB(addr, "rth_get_initialize_class", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }

    return addr;
}

//////////////////////////////////////////////////////////////////////////
// Get LIL version of Runtime Helper

static NativeCodePtr rth_wrap_exn_throw(int* dyncount, const char* name, NativeCodePtr stub)
{
#ifdef VM_STATS
    static SimpleHashtable wrappers(13);
    int _junk;
    NativeCodePtr wrapper;

    assert(dyncount);

    if (wrappers.lookup(stub, &_junk, &wrapper)) return wrapper;

    LilCodeStub* cs = lil_parse_code_stub(
        "entry 0:stdcall:arbitrary;"
        "inc [%0i:g4];"
        "tailcall %1i;",
        dyncount, lil_npc_to_fp(stub));
    assert(cs && lil_is_valid(cs));
    wrapper = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(wrapper, name, lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    wrappers.add(stub, 0, wrapper);
    return wrapper;
#else
    return stub;
#endif
}

static NativeCodePtr rth_get_lil_gc_safe_point(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }
    void (*hythread_safe_point_ptr)() = jvmti_safe_point;
    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall::void;");
    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
    }
    cs = lil_parse_onto_end(cs,
        "push_m2n 0, %0i;"
        "out platform::void;"
        "call %1i;"
        "pop_m2n;"
        "ret;",
        (UDATA)(FRAME_NON_UNWINDABLE | FRAME_POPABLE | FRAME_SAFE_POINT),
        hythread_safe_point_ptr);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_gc_safe_point", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}

static NativeCodePtr rth_get_lil_tls_base(int * dyn_count) {
    return (NativeCodePtr)hythread_self;
}

static NativeCodePtr rth_get_lil_jvmti_method_enter_callback(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (addr) {
            return addr;
    }
    void (*jvmti_method_enter_callback_ptr)(Method_Handle) = jvmti_method_enter_callback;
    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint:void;");
    assert(cs);
    if (dyn_count) {
            cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
            assert(cs);
        }
    cs = lil_parse_onto_end(cs,
        "push_m2n 0, %0i;"
        "in2out platform:void;"
        "call %1i;"
        "pop_m2n;"
        "ret;",
        (UDATA)FRAME_POPABLE,
        jvmti_method_enter_callback_ptr);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_jvmti_method_enter_callback", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}

static NativeCodePtr rth_get_lil_jvmti_method_exit_callback(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }
    void (*jvmti_method_exit_callback_ptr)(Method_Handle, jvalue *) = jvmti_method_exit_callback;
    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint,pint:void;");
    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
    }
    cs = lil_parse_onto_end(cs,
        "push_m2n 0, %0i;"
        "in2out platform:void;"
        "call %1i;"
        "pop_m2n;"
        "ret;",
        (UDATA)FRAME_POPABLE,
        jvmti_method_exit_callback_ptr);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_jvmti_method_exit_callback", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}

static NativeCodePtr rth_get_lil_jvmti_field_access_callback(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
        }
    void (*jvmti_field_access_callback_ptr)(Field_Handle, Method_Handle,
            jlocation, ManagedObject*) = jvmti_field_access_callback;

    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint,pint,g8,pint:void;");
    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
    }
    cs = lil_parse_onto_end(cs,
            "push_m2n 0, %0i;"
            "in2out platform:void;"
            "call %1i;"
            "pop_m2n;"
            "ret;",
            (UDATA)FRAME_POPABLE,
            jvmti_field_access_callback_ptr);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_jvmti_field_access_callback", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;

    //static NativeCodePtr addr = NULL;
    //if (addr) {
    //        return addr;
    //    }
    //LilCodeStub* cs = lil_parse_code_stub(
    //    "entry 0:stdcall:pint,pint,g8,pint:void;"
    //    "push_m2n 0, 0;"
    //    "in2out platform:void;"
    //    "call %0i;"
    //    "pop_m2n;"
    //    "ret;",
    //    jvmti_field_access_callback);
    //assert(cs && lil_is_valid(cs));
    //addr = LilCodeGenerator::get_platform()->compile(cs, "rth_jvmti_field_access_callback", dump_stubs);
    //lil_free_code_stub(cs);
    //return addr;
}

static NativeCodePtr rth_get_lil_jvmti_field_modification_callback(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
        }
    void (*jvmti_field_modification_callback_ptr)(Field_Handle, Method_Handle,
            jlocation, ManagedObject*, jvalue*) = jvmti_field_modification_callback;
    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall:pint,pint,g8,pint,pint:void;");
    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
        }
    cs = lil_parse_onto_end(cs,
            "push_m2n 0, %0i;"
            "in2out platform:void;"
            "call %1i;"
            "pop_m2n;"
            "ret;",
            (UDATA)FRAME_POPABLE,
            jvmti_field_modification_callback_ptr);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_jvmti_field_modification_callback", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;

    //static NativeCodePtr addr = NULL;
    //if (addr) {
    //    return addr;
    //}
    //LilCodeStub* cs = lil_parse_code_stub(
    //    "entry 0:stdcall:pint,pint,g8,pint,pint:void;"
    //    "push_m2n 0, 0;"
    //    "in2out platform:void;"
    //    "call %0i;"
    //    "pop_m2n;"
    //    "ret;",
    //    jvmti_field_modification_callback);
    //assert(cs && lil_is_valid(cs));
    //addr = LilCodeGenerator::get_platform()->compile(cs, "rth_jvmti_field_modification_callback", dump_stubs);
    //lil_free_code_stub(cs);
    //return addr;
}


//////////////////////////////////////////////////////////////////////////
//lazy resolution helpers

typedef void* f_resolve(Class_Handle, unsigned);
typedef void* f_resolve_int(Class_Handle, unsigned, unsigned);
typedef void* f_resolve_managed(Class_Handle, unsigned, ManagedObject*);

enum ResolveResType {
    ResolveResType_Managed, 
    ResolveResType_Unmanaged
};

///generates stub for 2 params helpers: ClassHandle, cpIndex
static NativeCodePtr rth_get_lil_stub_withresolve(int * dyn_count, f_resolve foo, const char* stub_name, ResolveResType type)
{
    LilCodeStub* cs = NULL;
    const char* in2out = NULL;
    const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
    const POINTER_SIZE_INT next_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;
    const unsigned handles_size = (unsigned)(sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*8);
    const unsigned cap_and_size = (unsigned)((0<<16) | 8);

    if (type == ResolveResType_Unmanaged) {
        cs = lil_parse_code_stub("entry 0:stdcall:pint,pint:pint;");
        in2out = "in2out platform:pint;";
    } else {
        assert(type == ResolveResType_Managed);
        cs = lil_parse_code_stub("entry 0:stdcall:pint,pint:ref;");
        in2out = "in2out platform:ref;";
    }
    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
    }
#ifdef _IPF_
    cs = lil_parse_onto_end(cs, "push_m2n 0, %0i;", (UDATA)(FRAME_POPABLE));
#else
    cs = lil_parse_onto_end(cs, "push_m2n 0, %0i, handles;", (UDATA)(FRAME_POPABLE));
    assert(cs);
    cs = lil_parse_onto_end(cs,
        "locals 1;"
        "alloc l0, %0i;"
        "st[l0+%1i:g4], %2i;"
        "st[l0+%3i:pint], 0;"
        "handles=l0;",
        (UDATA)handles_size,
        (UDATA)cap_off, 
        (UDATA)cap_and_size,
        (UDATA)next_off);
#endif
    assert(cs);
    cs = lil_parse_onto_end(cs, in2out);
    assert(cs);
    cs = lil_parse_onto_end(cs, 
        "call %0i;"
        "pop_m2n;"
        "ret;",
        (void*)foo);

    assert(cs && lil_is_valid(cs));
    NativeCodePtr addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, stub_name, lil_cs_get_code_size(cs));
    lil_free_code_stub(cs);

    return addr;
}
 

///generates stub for 3 params helpers: ClassHandle, cpIndex, ManagedObject* ref
static NativeCodePtr rth_get_lil_stub_withresolve(int * dyn_count, f_resolve_managed foo, const char* stub_name, ResolveResType type)
{
    LilCodeStub* cs = NULL;
    const char* in2out = NULL;
    const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
    const POINTER_SIZE_INT next_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;
    const unsigned handles_size = (unsigned)(sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*8);
    const unsigned cap_and_size = (unsigned)((0<<16) | 8);

    if (type == ResolveResType_Unmanaged) {
        cs = lil_parse_code_stub("entry 0:stdcall:pint,pint,ref:pint;");
        in2out = "in2out platform:pint;";
    } else {
        assert(type == ResolveResType_Managed);
        cs = lil_parse_code_stub("entry 0:stdcall:pint,pint,ref:ref;");
        in2out = "in2out platform:ref;";
    }

    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
    }

#ifdef _IPF_
    cs = lil_parse_onto_end(cs, "push_m2n 0, %0i;", (UDATA)(FRAME_POPABLE));
#else
    cs = lil_parse_onto_end(cs, "push_m2n 0, %0i, handles;", (UDATA)(FRAME_POPABLE));
    assert(cs);
    cs = lil_parse_onto_end(cs,
        "locals 1;"
        "alloc l0, %0i;"
        "st[l0+%1i:g4], %2i;"
        "st[l0+%3i:pint], 0;"
        "handles=l0;",
        (UDATA)handles_size,
        (UDATA)cap_off, (UDATA)cap_and_size,
        next_off);
#endif
    assert(cs);
    cs = lil_parse_onto_end(cs, in2out);
    assert(cs);
    cs = lil_parse_onto_end(cs, 
        "call %0i;"
        "pop_m2n;"
        "ret;",
        (void*)foo);

    assert(cs && lil_is_valid(cs));
    NativeCodePtr addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, stub_name, lil_cs_get_code_size(cs));
    lil_free_code_stub(cs);

    return addr;
}

///generates stub for 3 params helpers: ClassHandle, cpIndex, + some unsigned param
static NativeCodePtr rth_get_lil_stub_withresolve(int * dyn_count, f_resolve_int foo, const char* stub_name, ResolveResType type)
{
    LilCodeStub* cs = NULL;
    const char* in2out = NULL;
    const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
    const POINTER_SIZE_INT next_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;
    const unsigned handles_size = (unsigned)(sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*8);
    const unsigned cap_and_size = (unsigned)((0<<16) | 8);

    if (type == ResolveResType_Unmanaged) {
        cs = lil_parse_code_stub("entry 0:stdcall:pint,pint,pint:pint;");
        in2out = "in2out platform:pint;";
    } else {
        assert(type == ResolveResType_Managed);
        cs = lil_parse_code_stub("entry 0:stdcall:pint,pint,pint:ref;");
        in2out = "in2out platform:ref;";
    }

    assert(cs);
    if (dyn_count) {
        cs = lil_parse_onto_end(cs, "inc [%0i:pint];", dyn_count);
        assert(cs);
    }

#ifdef _IPF_
    cs = lil_parse_onto_end(cs, "push_m2n 0, %0i;", (UDATA)(FRAME_POPABLE));
#else
    cs = lil_parse_onto_end(cs, "push_m2n 0, %0i, handles;", (UDATA)(FRAME_POPABLE));
    assert(cs);
    cs = lil_parse_onto_end(cs,
        "locals 1;"
        "alloc l0, %0i;"
        "st[l0+%1i:g4], %2i;"
        "st[l0+%3i:pint], 0;"
        "handles=l0;",
        (UDATA)handles_size,
        (UDATA)cap_off, (UDATA)cap_and_size,
        next_off);
#endif
    assert(cs);
    cs = lil_parse_onto_end(cs, in2out);
    assert(cs);
    cs = lil_parse_onto_end(cs, 
        "call %0i;"
        "pop_m2n;"
        "ret;",
        (void*)foo);


    assert(cs && lil_is_valid(cs));
    NativeCodePtr addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, stub_name, lil_cs_get_code_size(cs));
    lil_free_code_stub(cs);

    return addr;
}


static inline Class* resolveClassNoExnCheck(Class_Handle klass, unsigned cp_idx, bool checkNew) {
    Global_Env* env = VM_Global_State::loader_env;
    Class* objClass = NULL;
    assert(!hythread_is_suspend_enabled());
    hythread_suspend_enable();
    if (checkNew) {
        objClass = resolve_class_new_env(env, klass, cp_idx, true);    
    } else {
        objClass = klass->_resolve_class(env, cp_idx);
    }
    hythread_suspend_disable();
    if (objClass==NULL) {
        class_throw_linking_error(klass, cp_idx, OPCODE_NEW);
    }
    return objClass;
}

static inline Class* resolveClass(Class_Handle klass, unsigned cp_idx, bool checkNew) {
    Class* res = NULL;
    BEGIN_RAISE_AREA;
    res = resolveClassNoExnCheck(klass, cp_idx, checkNew);
    END_RAISE_AREA;
    return res;
}

static inline void initializeClass(Class* cls) {
    if (cls->is_initialized()) {
        return;
    }

    assert(!hythread_is_suspend_enabled());
    BEGIN_RAISE_AREA;
    hythread_suspend_enable();
    Global_Env* env = VM_Global_State::loader_env;
    cls->verify_constraints(env);
    hythread_suspend_disable();
    END_RAISE_AREA;

    BEGIN_RAISE_AREA;
    cls->initialize();
    END_RAISE_AREA;
}

//resolving a class: used for multianewarray helper by JIT in lazyresolution mode
static void * rth_initialize_class_withresolve(Class_Handle klass, unsigned cp_idx)
{
    ASSERT_THROW_AREA;

    //resolve and init object class
    Class* objClass = resolveClass(klass, cp_idx, false);
    initializeClass(objClass);    
    return objClass;
}

static NativeCodePtr rth_get_lil_initialize_class_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_initialize_class_withresolve, 
            "rth_initialize_class_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}


///OPCODE_NEW
static void * rth_newobj_withresolve(Class_Handle klass, unsigned cp_idx)
{
    ASSERT_THROW_AREA;

    //resolve and init object class
    Class* objClass = resolveClass(klass, cp_idx, true);
    initializeClass(objClass);

    //create new object and return;    
    void* tls=vm_get_gc_thread_local();
    unsigned size = objClass->get_allocated_size();
    Allocation_Handle ah = objClass->get_allocation_handle();
    void* res=vm_malloc_with_thread_pointer(size, ah, tls);
    assert(res!=NULL);
    return res;
}

static NativeCodePtr rth_get_lil_newobj_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_newobj_withresolve, 
            "rth_new_obj_withresolve", ResolveResType_Managed);    
    }
    return addr;
}


//OPCODE_ANEWARRAY
static void *rth_newarray_withresolve(Class_Handle klass, unsigned cp_idx, unsigned arraySize) {
    ASSERT_THROW_AREA;
    
    //resolve object class (newarray does not require initializing the element class)
    Class* objClass = resolveClass(klass, cp_idx, false);
    // following initialize() is commented, see details in HARMONY-6020. Basically, VM spec $2.17.4 
    // does not say the creation of an array of type T needs iniatializing T.
    //initializeClass(objClass);
    assert(!objClass->is_primitive());

    void* res = NULL;

    Class* arrayClass = NULL;
    BEGIN_RAISE_AREA;
    hythread_suspend_enable();
    arrayClass = class_get_array_of_class(objClass);
    hythread_suspend_disable();
    END_RAISE_AREA
    
    BEGIN_RAISE_AREA;
    //create new array and return;    
    res = vm_new_vector(arrayClass, (int)arraySize);
    END_RAISE_AREA

    assert(res!=NULL);
    return res;
}

static NativeCodePtr rth_get_lil_newarray_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_newarray_withresolve,
            "rth_newarray_withresolve", ResolveResType_Managed);    
    }
    return addr;
}


///OPCODE_INVOKESPECIAL

static void *rth_invokespecial_addr_withresolve(Class_Handle klass, unsigned cp_idx) {
    ASSERT_THROW_AREA;
    
    Method* m = NULL;
    
    BEGIN_RAISE_AREA;
    hythread_suspend_enable();
    Global_Env* env = VM_Global_State::loader_env;
    m = resolve_special_method_env(env, klass, cp_idx, true);
    hythread_suspend_disable();
    END_RAISE_AREA;

    initializeClass(m->get_class());
    return m->get_indirect_address();
}


static NativeCodePtr rth_get_lil_invokespecial_addr_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_invokespecial_addr_withresolve,
            "rth_invokespecial_addr_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}



///OPCODE_INVOKESTATIC

static void *rth_invokestatic_addr_withresolve(Class_Handle klass, unsigned cp_idx) {
    ASSERT_THROW_AREA;

    Method* m = NULL;

    BEGIN_RAISE_AREA;
    hythread_suspend_enable();
    Global_Env* env = VM_Global_State::loader_env;
    m = resolve_static_method_env(env, klass, cp_idx, true);
    hythread_suspend_disable();
    END_RAISE_AREA;

    initializeClass(m->get_class());
    return m->get_indirect_address();
}

static NativeCodePtr rth_get_lil_invokestatic_addr_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_invokestatic_addr_withresolve,
            "rth_invokestatic_addr_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}

///OPCODE_INVOKEVIRTUAL

static void *rth_invokevirtual_addr_withresolve(Class_Handle klass, unsigned cp_idx, ManagedObject* obj) {
    ASSERT_THROW_AREA;

    Global_Env* env = VM_Global_State::loader_env;
    if (obj == (ManagedObject*)VM_Global_State::loader_env->managed_null) {
        exn_throw_by_class(env->java_lang_NullPointerException_Class);
        return obj;
    }

    Method* m = NULL;

    BEGIN_RAISE_AREA;
    jobject obj_h = oh_allocate_local_handle(); //make object reference visible to GC
    obj_h->object = obj;

    hythread_suspend_enable();
    m = resolve_virtual_method_env(env, klass, cp_idx, true);
    hythread_suspend_disable();

    obj = obj_h->object;
    oh_discard_local_handle(obj_h);
    END_RAISE_AREA;
    assert(m!=NULL);


    assert(obj!=NULL);
    assert(obj->vt()!=NULL);
    Class* objClass = obj->vt()->clss;
    assert(objClass!=NULL);
    assert(objClass->is_initialized() || objClass->is_initializing());
    assert(m->get_class()->is_initialized() || m->get_class()->is_initializing());

    unsigned method_index = m->get_index();
    assert(method_index<objClass->get_number_of_virtual_method_entries());
    Method* method_to_call = objClass->get_method_from_vtable(method_index);
    
    assert(method_to_call->get_class()->is_initialized() || method_to_call->get_class()->is_initializing());
    return method_to_call->get_indirect_address();

}

static NativeCodePtr rth_get_lil_invokevirtual_addr_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_invokevirtual_addr_withresolve,
            "rth_invokevirtual_addr_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}


///OPCODE_INVOKEINTERFACE
static void *rth_invokeinterface_addr_withresolve(Class_Handle klass, unsigned cp_idx, ManagedObject* obj) {
    ASSERT_THROW_AREA;

    Global_Env* env = VM_Global_State::loader_env;
    if (obj == (ManagedObject*)VM_Global_State::loader_env->managed_null) {

        //Returning zero address and not generating NPE if the object is null 
        //is safe in terms of preserving program semantics(the exception will be generated 
        //on the first method invocation) and
        //allows profitable code transformations such as hoisting vm helper outside the loop body 
        //This is our current convention which touches all variants of this helper, 
        //If it changes, all variants must be fixed. 

        //exn_throw_by_class(env->java_lang_NullPointerException_Class);
        return obj;
    }

    Method* m = NULL;

    BEGIN_RAISE_AREA;
    jobject obj_h = oh_allocate_local_handle(); //make object reference visible to GC
    obj_h->object = obj;

    hythread_suspend_enable();
    m = resolve_interface_method_env(env, klass, cp_idx, true);
    hythread_suspend_disable();

    obj = obj_h->object;
    oh_discard_local_handle(obj_h);
    END_RAISE_AREA; // rethrow NoSuchMethodError if any
    assert(m!=NULL);

    assert(obj!=NULL);
    assert(obj->vt()!=NULL);
    Class* objClass = obj->vt()->clss;
    assert(objClass!=NULL);
    assert(objClass->is_initialized() || objClass->is_initializing());

    unsigned base_index = 0;
    if(m->get_class()->is_interface()) {
        char* infc_vtable = (char*)Class::helper_get_interface_vtable(obj, m->get_class());
        if(infc_vtable == NULL) {
            exn_throw_by_name("java/lang/IncompatibleClassChangeError", objClass->get_name()->bytes);
            return NULL;
        }
        base_index = (unsigned)(infc_vtable - (char*)objClass->get_vtable()->methods)/sizeof(char*);
    }
    Method* infc_method = objClass->get_method_from_vtable(base_index + m->get_index());
    if(infc_method == NULL) {
        // objClass does not implement interface method
        char* msg = (char*)STD_ALLOCA(objClass->get_name()->len + 1
            + m->get_name()->len + m->get_descriptor()->len + 1);
        strcpy(msg, objClass->get_name()->bytes);
        strcat(msg, ".");
        strcat(msg, m->get_name()->bytes);
        strcat(msg, m->get_descriptor()->bytes);
        exn_throw_by_name("java/lang/AbstractMethodError", msg);
        return NULL; // not reachable
    }
    if(infc_method->is_protected() || infc_method->is_private()) {
        // objClass overrides interface method as protected or private
        char* msg = (char*)STD_ALLOCA(objClass->get_name()->len + 1
            + m->get_name()->len + m->get_descriptor()->len + 1);
        strcpy(msg, objClass->get_name()->bytes);
        strcat(msg, ".");
        strcat(msg, m->get_name()->bytes);
        strcat(msg, m->get_descriptor()->bytes);
        exn_throw_by_name("java/lang/IllegalAccessError", msg);
        return NULL; // not reachable
    }
    assert(infc_method->get_class()->is_initialized() || objClass->is_initializing());
    return infc_method->get_indirect_address();
}

static NativeCodePtr rth_get_lil_invokeinterface_addr_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_invokeinterface_addr_withresolve,
            "rth_invokeinterface_addr_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}


///OPCODE_GETFIELD
///OPCODE_PUTFIELD

static void *rth_get_nonstatic_field_offset_withresolve(Class_Handle klass, unsigned cp_idx, unsigned putfield) {
    ASSERT_THROW_AREA;

    Field* f = NULL;

    BEGIN_RAISE_AREA;
    hythread_suspend_enable();
    Global_Env* env = VM_Global_State::loader_env;
    f = resolve_nonstatic_field_env(env, klass, cp_idx, putfield, true);
    hythread_suspend_disable();
    END_RAISE_AREA;

    assert(f->get_class()->is_initialized() || f->get_class()->is_initializing());
    return (void*)(POINTER_SIZE_INT)f->get_offset();
}

static NativeCodePtr rth_get_lil_nonstatic_field_offset_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_get_nonstatic_field_offset_withresolve,
            "rth_get_nonstatic_field_offset_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}


///OPCODE_GETSTATIC
///OPCODE_PUTSTATIC

static void *rth_get_static_field_addr_withresolve(Class_Handle klass, unsigned cp_idx, unsigned putfield) {
    ASSERT_THROW_AREA;

    Field* f = NULL;

    BEGIN_RAISE_AREA;
    hythread_suspend_enable();
    Global_Env* env = VM_Global_State::loader_env;
    f = resolve_static_field_env(env, klass, cp_idx, putfield, true);
    hythread_suspend_disable();
    END_RAISE_AREA;

    initializeClass(f->get_class());
    return f->get_address();
}

static NativeCodePtr rth_get_lil_static_field_addr_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_get_static_field_addr_withresolve,
            "rth_get_static_field_addr_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}


///OPCODE_CHECKCAST

static void *rth_checkcast_withresolve(Class_Handle klass, unsigned cp_idx, ManagedObject* obj) {
    if (obj == (ManagedObject*)VM_Global_State::loader_env->managed_null) {
        return obj;
    }

    Class* objClass = obj->vt()->clss;
    Class* castClass = NULL;

    BEGIN_RAISE_AREA
    jobject obj_h = oh_allocate_local_handle(); //make object reference visible to GC
    obj_h->object = obj;

    castClass = resolveClassNoExnCheck(klass, cp_idx, false);

    obj = obj_h->object;
    oh_discard_local_handle(obj_h);
    END_RAISE_AREA

    Boolean res = class_is_subtype(objClass, castClass);
    if (!res) {
        exn_throw_by_name("java/lang/ClassCastException", objClass->get_java_name()->bytes);
    }
    return obj;
}

static NativeCodePtr rth_get_lil_checkcast_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_checkcast_withresolve,
            "rth_checkcast_withresolve", ResolveResType_Managed);    
    }
    return addr;
}

//OPCODE_INSTANCEOF
static void *rth_instanceof_withresolve(Class_Handle klass, unsigned cp_idx, ManagedObject* obj) {
    ASSERT_THROW_AREA;

    Class* castClass = NULL;

    BEGIN_RAISE_AREA
    jobject obj_h = oh_allocate_local_handle(); //make object reference visible to GC
    obj_h->object = obj;
   
    castClass = resolveClassNoExnCheck(klass, cp_idx, false);

    obj = obj_h->object;
    oh_discard_local_handle(obj_h);
    END_RAISE_AREA

    int res = vm_instanceof(obj, castClass);
    return (void*)(POINTER_SIZE_INT)res;
}

static NativeCodePtr rth_get_lil_instanceof_withresolve(int * dyn_count) {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        addr = rth_get_lil_stub_withresolve(dyn_count, rth_instanceof_withresolve,
            "rth_instanceof_withresolve", ResolveResType_Unmanaged);    
    }
    return addr;
}

//end of lazy resolution helpers
//////////////////////////////////////////////////////////////////////////
#if (defined PLATFORM_POSIX) && (defined _IA32_)
ManagedObject* __attribute__ ((__stdcall__)) rth_struct_Class_to_java_lang_Class(Class *clss) {
#elif defined _IA32_
ManagedObject* __stdcall rth_struct_Class_to_java_lang_Class(Class *clss) {
#else
ManagedObject* rth_struct_Class_to_java_lang_Class(Class *clss) {
#endif
    return struct_Class_to_java_lang_Class(clss);
}



NativeCodePtr rth_get_lil_helper(VM_RT_SUPPORT f)
{
    int* dyn_count = NULL;
#ifdef VM_STATS
    dyn_count = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)f, 0, NULL);
#endif

    switch(f) {
    case VM_RT_MULTIANEWARRAY_RESOLVED:
        return rth_get_lil_multianewarray(dyn_count);
    case VM_RT_LDC_STRING:
        return rth_get_lil_ldc_ref(dyn_count);
    // Exceptions
    case VM_RT_THROW:
    case VM_RT_THROW_SET_STACK_TRACE:
        return rth_wrap_exn_throw(dyn_count, "rth_throw", exn_get_rth_throw());
    case VM_RT_THROW_LAZY:
        return rth_wrap_exn_throw(dyn_count, "rth_throw_lazy", exn_get_rth_throw_lazy());
    case VM_RT_THROW_LINKING_EXCEPTION:
        return rth_get_lil_throw_linking_exception(dyn_count);

    case VM_RT_CLASS_2_JLC:
        return (NativeCodePtr)rth_struct_Class_to_java_lang_Class;

    // Type tests
    case VM_RT_CHECKCAST:
        return rth_get_lil_checkcast(dyn_count);
    case VM_RT_INSTANCEOF:
        return rth_get_lil_instanceof(dyn_count);
    case VM_RT_AASTORE:
        return rth_get_lil_aastore(dyn_count);
    case VM_RT_AASTORE_TEST:
        return rth_get_lil_aastore_test(dyn_count);
    // Misc
    case VM_RT_GET_INTERFACE_VTABLE_VER0:
        return rth_get_lil_get_interface_vtable(dyn_count);
    case VM_RT_INITIALIZE_CLASS:
        return rth_get_lil_initialize_class(dyn_count);
    case VM_RT_GC_SAFE_POINT:
        return rth_get_lil_gc_safe_point(dyn_count);
    case VM_RT_GC_GET_TLS_BASE:
        return rth_get_lil_tls_base(dyn_count);
    // JVMTI
    case VM_RT_JVMTI_METHOD_ENTER_CALLBACK:
        return rth_get_lil_jvmti_method_enter_callback(dyn_count);
    case VM_RT_JVMTI_METHOD_EXIT_CALLBACK:
        return rth_get_lil_jvmti_method_exit_callback(dyn_count);
    case VM_RT_JVMTI_FIELD_ACCESS_CALLBACK:
        return rth_get_lil_jvmti_field_access_callback(dyn_count);
    case VM_RT_JVMTI_FIELD_MODIFICATION_CALLBACK:
        return rth_get_lil_jvmti_field_modification_callback(dyn_count);
    // Non-VM
    case VM_RT_NEWOBJ_WITHRESOLVE:
        return rth_get_lil_newobj_withresolve(dyn_count);
    case VM_RT_NEWARRAY_WITHRESOLVE:
        return rth_get_lil_newarray_withresolve(dyn_count);
    case VM_RT_INITIALIZE_CLASS_WITHRESOLVE:
        return rth_get_lil_initialize_class_withresolve(dyn_count);
    case VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE:
        return rth_get_lil_nonstatic_field_offset_withresolve(dyn_count);
    case VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE:
        return rth_get_lil_static_field_addr_withresolve(dyn_count);
    case VM_RT_CHECKCAST_WITHRESOLVE:
        return rth_get_lil_checkcast_withresolve(dyn_count);
    case VM_RT_INSTANCEOF_WITHRESOLVE:
        return rth_get_lil_instanceof_withresolve(dyn_count);
    case VM_RT_GET_INVOKESTATIC_ADDR_WITHRESOLVE:
        return rth_get_lil_invokestatic_addr_withresolve(dyn_count);
    case VM_RT_GET_INVOKEINTERFACE_ADDR_WITHRESOLVE:
        return rth_get_lil_invokeinterface_addr_withresolve(dyn_count);
    case VM_RT_GET_INVOKEVIRTUAL_ADDR_WITHRESOLVE:
        return rth_get_lil_invokevirtual_addr_withresolve(dyn_count);
    case VM_RT_GET_INVOKE_SPECIAL_ADDR_WITHRESOLVE:
        return rth_get_lil_invokespecial_addr_withresolve(dyn_count);

    default:
        return NULL;
    }
}

//**********************************************************************//
// End new LIL runtime support
//**********************************************************************//


/////////////////////////////////////////////////////////////

// begin Java object allocation

/**
 * Intermediary function called from rintime helpers for
 * slow path allocation to check for out of memory conditions.
 * Throws OutOfMemoryError if allocation function returns null.
 */
void *vm_malloc_with_thread_pointer(
        unsigned size, Allocation_Handle ah, void *tp) {
    ASSERT_THROW_AREA;
    assert(!hythread_is_suspend_enabled());

    void *result = NULL;
    BEGIN_RAISE_AREA;
    result = gc_alloc(size,ah,tp);
    END_RAISE_AREA;
    exn_rethrow_if_pending();

    if (!result) {
        exn_throw_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return 0; // whether this return is reached or not is solved via is_unwindable state
    }
    return result;
}

ManagedObject* vm_rt_class_alloc_new_object(Class *c)
{
    ASSERT_THROW_AREA;
    ManagedObject* result;
    BEGIN_RAISE_AREA;
    result = class_alloc_new_object(c);
    END_RAISE_AREA;
    exn_rethrow_if_pending();
    return result;
}


ManagedObject* class_alloc_new_object(Class* c)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());
    assert(struct_Class_to_java_lang_Class(c)); 

    class_initialize(c);
    ManagedObject* obj = c->allocate_instance();
    if(!obj) {
        exn_raise_object(
            VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return NULL; // whether this return is reached or not is solved via is_unwindable state
    }
    return obj;
} // class_alloc_new_object

ManagedObject *class_alloc_new_object_using_vtable(VTable *vtable)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());
    assert(struct_Class_to_java_lang_Class(vtable->clss));
    return class_alloc_new_object(vtable->clss);
} //class_alloc_new_object_using_vtable

ManagedObject* class_alloc_new_object_and_run_default_constructor(Class* clss)
{
    return class_alloc_new_object_and_run_constructor(clss, 0, 0);
} // class_alloc_new_object_and_run_default_constructor

ManagedObject*
class_alloc_new_object_and_run_constructor(Class* clss,
                                           Method* constructor,
                                           U_8* constructor_args)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());
    assert(struct_Class_to_java_lang_Class(clss)); 

    ObjectHandle obj = oh_allocate_local_handle();
    obj->object = class_alloc_new_object(clss);
    if(!obj->object) {
        return NULL;
    }

    if(!constructor) {
        // Get the default constructor
        Global_Env* env = VM_Global_State::loader_env;
        constructor = clss->lookup_method(env->Init_String, env->VoidVoidDescriptor_String);
        assert(constructor);
    }

    // Every argument is at least 4 bytes long
    int num_args_estimate = constructor->get_num_arg_slots();
    jvalue* args = (jvalue*)STD_MALLOC(num_args_estimate * sizeof(jvalue));
    args[0].l = (jobject)obj;

    int arg_num = 1;
    U_8* argp = constructor_args;
    Arg_List_Iterator iter = constructor->get_argument_list();
    Java_Type typ;
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        switch(typ) {
        case JAVA_TYPE_BOOLEAN:
            argp -= sizeof(jint);
            args[arg_num].z = *(jboolean *)argp;
            break;
        case JAVA_TYPE_BYTE:
            argp -= sizeof(jint);
            args[arg_num].b = *(jbyte *)argp;
            break;
        case JAVA_TYPE_CHAR:
            argp -= sizeof(jint);
            args[arg_num].c = *(jchar *)argp;
            break;
        case JAVA_TYPE_SHORT:
            argp -= sizeof(jint);
            args[arg_num].s = *(jshort *)argp;
            break;
        case JAVA_TYPE_INT:
            argp -= sizeof(jint);
            args[arg_num].i = *(jint *)argp;
            break;
        case JAVA_TYPE_LONG:
            argp -= sizeof(jlong);
            args[arg_num].j = *(jlong *)argp;
            break;
        case JAVA_TYPE_DOUBLE:
            argp -= sizeof(jdouble);
            args[arg_num].d = *(jdouble *)argp;
            break;
        case JAVA_TYPE_FLOAT:
            argp -= sizeof(jfloat);
            args[arg_num].f = *(jfloat *)argp;
            break;
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
            {
                argp -= sizeof(ManagedObject*);
                ObjectHandle h = oh_allocate_local_handle();
                h->object = *(ManagedObject**) argp;
                args[arg_num].l = h;
            }
            break;
        default:
            LDIE(53, "Unexpected java type");
            break;
        }
        iter = advance_arg_iterator(iter);
        arg_num++;
        assert(arg_num <= num_args_estimate);
    }
    assert(!hythread_is_suspend_enabled());
    vm_execute_java_method_array((jmethodID) constructor, 0, args);

    if (exn_raised()) {
        LDIE(18, "class constructor has thrown an exception");
    }

    STD_FREE(args);

    return obj->object;
} //class_alloc_new_object_and_run_constructor


// end Java object allocation
/////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////
// begin instanceof


static void update_general_type_checking_stats(VTable *sub, Class *super)
{
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_type_checks++;
    if (sub->clss == super)
        VM_Statistics::get_vm_stats().num_type_checks_equal_type++;
    if (super->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_type_checks_fast_decision++;
    else if (super->is_array())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_array++;
    else if (super->is_interface())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_interface++;
    else if (super->get_depth() >= vm_max_fast_instanceof_depth())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_too_deep++;
    UNSAFE_REGION_END
#endif // VM_STATS
}

void vm_instanceof_update_stats(ManagedObject *obj, Class *super)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_instanceof++;
    super->instanceof_slow_path_taken();
    if (obj == (ManagedObject*)VM_Global_State::loader_env->managed_null)
        VM_Statistics::get_vm_stats().num_instanceof_null++;
    else
    {
        if (obj->vt()->clss == super)
            VM_Statistics::get_vm_stats().num_instanceof_equal_type ++;
        if (super->get_fast_instanceof_flag())
            VM_Statistics::get_vm_stats().num_instanceof_fast_decision ++;
        update_general_type_checking_stats(obj->vt(), super);
    }
#endif
}

void vm_checkcast_update_stats(ManagedObject *obj, Class *super)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_checkcast ++;
    if (obj == (ManagedObject*)VM_Global_State::loader_env->managed_null)
        VM_Statistics::get_vm_stats().num_checkcast_null++;
    else
    {
        if (obj->vt()->clss == super)
            VM_Statistics::get_vm_stats().num_checkcast_equal_type ++;
        if (super->get_fast_instanceof_flag())
            VM_Statistics::get_vm_stats().num_checkcast_fast_decision ++;
        update_general_type_checking_stats(obj->vt(), super);
    }
#endif
}



/************************************************************
 * Auxiliary functions and macros, used by the generators of
 * LIL checkcast and instanceof stubs
 */

// appends code that throws a ClassCastException to a LIL stub
static LilCodeStub *gen_lil_throw_ClassCastException(LilCodeStub *cs) {
    // if instanceof returns false, throw an exception
    LilCodeStub *cs2 = lil_parse_onto_end
        (cs,
         "std_places 1;"
         "sp0 = %0i;\n"
         "tailcall %1i;",
         exn_get_class_cast_exception_type(),
         lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(cs2 != NULL);
    return cs2;
}  // gen_lil_throw_ClassCastException


// emits the slow path of the instanceof / checkcast check
// notice that this is different from gen_lil_instanceof_stub_slow;
// the former is meant to be used as part of a bigger stub that decides
// that the slow path is appropriate, whereas the latter is a self-contained
// stub.
static LilCodeStub* gen_lil_typecheck_slowpath(LilCodeStub *cs,
                                               bool is_checkcast) {
    /* slow case; call class_is_subtype(obj->vtable()->clss, super)
     * Here's how to do this, normally *OR* with compressed refs
     *
     * l0 = obj->vtable *OR* l0 = obj->vt_offset
     * o0 = l0->clss    *OR* o0 = (l0+vtable_base)->clss
     * o1 = i1
     * call class_is_subtype(o0, o1)
     */
    LilCodeStub *cs2;
    if (vm_is_vtable_compressed())
    {
        cs2 = lil_parse_onto_end
            (cs,
            "ld l0, [i0+%0i:g4],zx;",
            (POINTER_SIZE_INT)object_get_vtable_offset());
    }
    else
    {
        cs2 = lil_parse_onto_end
            (cs, 
            "ld l0, [i0+%0i:ref];",
            (POINTER_SIZE_INT)object_get_vtable_offset());
    }

    cs2 = lil_parse_onto_end
        (cs2,
         "out platform:ref,pint:g4;"
         "ld o0, [l0+%0i:ref];"
         "o1 = i1;"
         "call %1i;",
         OFFSET(VTable, clss) + (vm_is_vtable_compressed() ? (UDATA)vm_get_vtable_base_address() : 0),
         (void*) class_is_subtype);

    if (is_checkcast) {
        // if class_is_subtype returns true, return the object, otherwise jump to exception code
        cs2 = lil_parse_onto_end
            (cs2,
             "jc r=0, failed;"
             "r = i0;"
             "ret;");
    }
    else {
        // just return whatever class_is_subtype returned
        cs2 = lil_parse_onto_end
            (cs2,
             "ret;");
    }

    assert(cs2 != NULL);
    return cs2;
}  // gen_lil_typecheck_slowpath


// emits the fast path of the instanceof / checkcast check
// presupposes that all necessary checks have already been performed,
// and that the fast path is appropriate.
static LilCodeStub* gen_lil_typecheck_fastpath(LilCodeStub *cs,
                                               bool is_checkcast) {

    /* fast case; check whether
     *  (obj->vt()->superclasses[super->depth-1] == super)
     * Here's how to do this, normally *OR* with compressed refs
     *
     * l0 = obj->vtable *OR* l0 = obj->vt_offset
     * l1 = i1->depth
     * l2 = l0->superclasses[l1-1] *OR*
     *     (l0+vtable_base)->superclasses[l1-1]
     * check if i1 == l2
     */
    LilCodeStub *cs2;
    if (vm_is_vtable_compressed())
    {
        cs2 = lil_parse_onto_end
            (cs,
            "ld l0, [i0+%0i:g4],zx;",
            (POINTER_SIZE_INT)object_get_vtable_offset());
    }
    else
    {
        cs2 = lil_parse_onto_end
            (cs, 
            "ld l0, [i0+%0i:ref];",
            (POINTER_SIZE_INT)object_get_vtable_offset());
    }

    cs2 = lil_parse_onto_end
        (cs2,
         "ld l1, [i1 + %0i: g4],zx;"
         "ld l2, [l0 + %1i*l1 + %2i: pint];"
         "jc i1 != l2, failed;",
         Class::get_offset_of_depth(),
         (POINTER_SIZE_INT)sizeof(Class*),
         OFFSET(VTable, superclasses) - sizeof(Class*) + (vm_is_vtable_compressed() ? (UDATA)vm_get_vtable_base_address() : 0)
         );

    if (is_checkcast) {
        // return the object on success
        cs2 = lil_parse_onto_end
            (cs2,
             "r = i0;"
             "ret;");
    }
    else {
        // instanceof; return 1 on success, 0 on failure
        cs2 = lil_parse_onto_end
            (cs2,
             "r=1:g4;"
             "ret;"
             ":failed;"
             "r=0:g4;"
             "ret;");
    }

    assert(cs != NULL);
    return cs2;
}  // gen_lil_typecheck_fastpath


/*
 * Auxiliary functions for LIL and Instanceof - END
 ***************************************************/


/*****************************************************************
 * Functions that generate LIL stubs for checkcast and instanceof
 */


// creates a LIL code stub for checkcast or instanceof
// can be used by both IA32 and IPF code
LilCodeStub *gen_lil_typecheck_stub(bool is_checkcast)
{
    LilCodeStub* cs = NULL;

    // check if object address is NULL
    if (is_checkcast) {
        // args: ManagedObject *obj, Class *super; returns a ManagedObject*
        cs = lil_parse_code_stub
        ("entry 0:stdcall:ref,pint:ref;"
         "jc i0!=%0i:ref,nonnull;"
         "r=i0;"  // return obj if obj==NULL
         "ret;",
         VM_Global_State::loader_env->managed_null);
    }
    else {
        // args: ManagedObject *obj, Class *super; returns a boolean
        cs = lil_parse_code_stub
        ("entry 0:stdcall:ref,pint:g4;"
         "jc i0!=%0i:ref,nonnull;"
         "r=0:g4;"  // return FALSE if obj==NULL
         "ret;",
         VM_Global_State::loader_env->managed_null);
    }

    // check whether the fast or the slow path is appropriate
    cs = lil_parse_onto_end
        (cs,
         ":nonnull;"
         "locals 3;"
         // check if super->is_suitable_for_fast_instanceof
         "ld l0, [i1 + %0i: g4];"
         "jc l0!=0:g4, fast;",
         Class::get_offset_of_fast_instanceof_flag());

    // append the slow path right here
    cs = gen_lil_typecheck_slowpath(cs, is_checkcast);

    // generate a "fast:" label, followed by the fast path
    cs = lil_parse_onto_end(cs, ":fast;");
    cs = gen_lil_typecheck_fastpath(cs, is_checkcast);

    if (is_checkcast) {
        // if the check has failed, throw an exception
        cs = lil_parse_onto_end(cs, ":failed;");
        cs = gen_lil_throw_ClassCastException(cs);
    }

    assert(lil_is_valid(cs));
    return cs;
}  // gen_lil_typecheck_stub


/*
 * Functions that generate LIL stubs for checkcast and instanceof - END
 ***********************************************************************/


void vm_aastore_test_update_stats(ManagedObject *elem, Vector_Handle array)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_aastore_test++;
    if(elem == (ManagedObject*)VM_Global_State::loader_env->managed_null)
    {
        VM_Statistics::get_vm_stats().num_aastore_test_null ++;
        return;
    }
    VTable *vt = get_vector_vtable(array);
    if (vt == cached_object_array_vtable_ptr)
    {
        VM_Statistics::get_vm_stats().num_aastore_test_object_array++;
        return;
    }
    Class *array_class = vt->clss;
    if (elem->vt()->clss == array_class->get_array_element_class())
        VM_Statistics::get_vm_stats().num_aastore_test_equal_type ++;
    if (array_class->get_array_element_class()->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_aastore_test_fast_decision ++;
    update_general_type_checking_stats(elem->vt(), array_class->get_array_element_class());
#endif
}


Boolean class_is_subtype_fast(VTable *sub, Class *super)
{
    update_general_type_checking_stats(sub, super);
    return sub->clss->is_instanceof(super);
} // class_is_subtype_fast


// Returns TRUE if "s" represents a class that is a subtype of "t",
// according to the Java instanceof rules.
//
// No VM_STATS values are modified here.
Boolean class_is_subtype(Class *s, Class *t)
{
    if (s == t) {
        return TRUE;
    }

    Global_Env *env = VM_Global_State::loader_env;

    Class *object_class = env->JavaLangObject_Class;
    assert(object_class != NULL);

    if(s->is_array()) {
        if (t == object_class) {
            return TRUE;
        }
        if(t == env->java_io_Serializable_Class) {
            return TRUE;
        }
        if(t == env->java_lang_Cloneable_Class) {
            return TRUE;
        }
        if(!t->is_array()) {
            return FALSE;
        }

        return class_is_subtype(s->get_array_element_class(), t->get_array_element_class());
    } else {
        if(!t->is_interface()) {
            for(Class *c = s; c; c = c->get_super_class()) {
                if(c == t){
                    return TRUE;
                }
            }
        } else {
            for(Class *c = s; c; c = c->get_super_class()) {
                unsigned n_intf = c->get_number_of_superinterfaces();
                for(unsigned i = 0; i < n_intf; i++) {
                    Class* intf = c->get_superinterface(i);
                    assert(intf);
                    assert(intf->is_interface());
                    if(class_is_subtype(intf, t)) {
                        return TRUE;
                    }
                }
            }
        }
    }

    return FALSE;
} //class_is_subtype



// 20030321 The instanceof JIT support routines expect to be called directly from managed code. 
VMEXPORT // temporary solution for interpreter unplug
int __stdcall vm_instanceof(ManagedObject *obj, Class *c)
{
    assert(!hythread_is_suspend_enabled());

#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_instanceof++;
    c->instanceof_slow_path_taken();
#endif

    ManagedObject *null_ref = (ManagedObject *)VM_Global_State::loader_env->managed_null;
    if (obj == null_ref) {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_instanceof_null++;
#endif
        return 0;
    }
    assert(obj->vt());
#ifdef VM_STATS
    if (obj->vt()->clss == c)
        VM_Statistics::get_vm_stats().num_instanceof_equal_type ++;
    if (c->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_instanceof_fast_decision ++;
#endif // VM_STATS
    return class_is_subtype_fast(obj->vt(), c);
} //vm_instanceof

// end instanceof
/////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////
// begin aastore and aastore_test


// 20030321 This JIT support routine expects to be called directly from managed code. 
// The return value is 1 if the element reference can be stored into the array, or 0 otherwise.
int __stdcall
vm_aastore_test(ManagedObject *elem,
                 Vector_Handle array)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_aastore_test++;
#endif // VM_STATS

    ManagedObject *null_ref = (ManagedObject *)VM_Global_State::loader_env->managed_null;
    if (array == null_ref) {
        return 0;
    }
    if (elem == null_ref) {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_aastore_test_null++;
#endif // VM_STATS
        return 1;
    }

    VTable *vt = get_vector_vtable(array);
    if (vt == cached_object_array_vtable_ptr) {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_aastore_test_object_array++;
#endif // VM_STATS
        return 1;
    }

    Class *array_class = vt->clss;
    assert(array_class);
    assert(array_class->is_array());

#ifdef VM_STATS
    if (elem->vt()->clss == array_class->get_array_element_class())
        VM_Statistics::get_vm_stats().num_aastore_test_equal_type ++;
    if (array_class->get_array_element_class()->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_aastore_test_fast_decision ++;
#endif // VM_STATS
    return class_is_subtype_fast(elem->vt(), array_class->get_array_element_class());
} //vm_aastore_test


// 20030505 This JIT support routine expects to be called directly from managed code. 
// The return value is either NULL or the ClassHandle for an exception to throw.
void * __stdcall
vm_rt_aastore(Vector_Handle array, int idx, ManagedObject *elem)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_aastore ++;
#endif // VM_STATS

    Global_Env *env = VM_Global_State::loader_env;
    ManagedObject *null_ref = (ManagedObject *)VM_Global_State::loader_env->managed_null;
    if (array == null_ref) {
        return env->java_lang_NullPointerException_Class;
    } else if (((U_32)idx) >= (U_32)get_vector_length(array)) {
        return env->java_lang_ArrayIndexOutOfBoundsException_Class;
    } else if (elem != null_ref) {
        Class *array_class = get_vector_vtable(array)->clss;
        assert(array_class);
        assert(array_class->is_array());
#ifdef VM_STATS
        // XXX - Should update VM_Statistics::get_vm_stats().num_aastore_object_array
        if (elem->vt()->clss == array_class->get_array_element_class())
            VM_Statistics::get_vm_stats().num_aastore_equal_type ++;
        if (array_class->get_array_element_class()->get_fast_instanceof_flag())
            VM_Statistics::get_vm_stats().num_aastore_fast_decision ++;
#endif // VM_STATS
        if (class_is_subtype_fast(elem->vt(), array_class->get_array_element_class())) {
            STORE_REFERENCE((ManagedObject *)array, get_vector_element_address_ref(array, idx), (ManagedObject *)elem);
        } else {
            return env->java_lang_ArrayStoreException_Class;
        }
    } else {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_aastore_null ++;
#endif // VM_STATS
        // elem is null. We don't have to check types for a null reference.
        // We also don't have to record stores of null references.
        ManagedObject** elem_ptr = get_vector_element_address_ref(array, idx);
        REF_INIT_BY_ADDR(elem_ptr, NULL);
    }
    return NULL;
} //vm_rt_aastore


// end aastore and aastore_test
/////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////
// begin get_interface_vtable

void *vm_get_interface_vtable(ManagedObject *obj, Class *iid)
{
    return rth_get_interface_vtable(obj, iid);
} //vm_get_interface_vtable


// end get_interface_vtable
/////////////////////////////////////////////////////////////


/**
 * @brief Generates an VM's helper to invoke the provided function.
 *
 * The helper takes 'void*' parameter which is passed to the function after
 * some preparation made (namely GC and stack info are prepared to allow GC
 * to work properly).
 *
 * The function must follow stdcall convention, which takes 'void*' and
 * returns 'void*', so does the helper.
 * On a return from the function, the helper checks whether an exception
 * was raised for the current thread, and rethrows it if necessary.
 */
VMEXPORT void * vm_create_helper_for_function(void* (*fptr)(void*))
{
    static const char * lil_stub =
        "entry 0:stdcall:pint:pint;"    // the single argument is 'void*'
        "push_m2n 0, %0i;"              // create m2n frame
        "out stdcall::void;"
        "call %1i;"                     // call hythread_suspend_enable()
        "in2out stdcall:pint;"          // reloads input arg into output
        "call %2i;"                     // call the foo
        "locals 3;"                     //
        "l0 = r;"                       // save result
        "out stdcall::void;"
        "call %3i;"                     // call hythread_suspend_disable()
        "l1 = ts;"
        "ld l2, [l1 + %4i:ref];"
        "jc l2 != 0,_exn_raised;"       // test whether an exception happened
        "ld l2, [l1 + %5i:ref];"
        "jc l2 != 0,_exn_raised;"       // test whether an exception happened
        "pop_m2n;"                      // pop out m2n frame
        "r = l0;"                       // no exceptions pending, restore ..
        "ret;"                          // ret value and exit
        ":_exn_raised;"                 //
        "out platform::void;"           //
        "call.noret %6i;";              // re-throw exception

    void * fptr_rethrow = (void*)&exn_rethrow;
    void * fptr_suspend_enable = (void*)&hythread_suspend_enable;
    void * fptr_suspend_disable = (void*)&hythread_suspend_disable;

    LilCodeStub* cs = lil_parse_code_stub(
        lil_stub, (FRAME_COMPILATION | FRAME_POPABLE),
        fptr_suspend_enable, (void*)fptr, fptr_suspend_disable,
        OFFSET(VM_thread, thread_exception.exc_object),
        OFFSET(VM_thread, thread_exception.exc_class),
        fptr_rethrow);
    assert(lil_is_valid(cs));
    void * addr = LilCodeGenerator::get_platform()->compile(cs);
    
    DUMP_STUB(addr, "generic_wrapper", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
};
