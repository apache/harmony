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
#include "verifier.h"
#include "../java5/context_5.h"
#include "../java6/context_6.h"
#ifndef _NDEBUG
#include "class_interface.h"
#endif

#include "open/vm_class_manipulation.h"
#include "open/vm_method_access.h"
#include "open/vm_class_loading.h"

/**
* Provides initial java-5 verification of class.
*
* If when verifying the class a check of type "class A must be assignable to class B" needs to be done 
* and either A or B is not loaded at the moment then a constraint 
* "class A must be assignable to class B" is recorded into the classloader's data
*/
vf_Result
vf_verify5_class(Class_Handle klass, unsigned verifyAll, char** error)
{
    int index;
    vf_Result result = VF_OK;

    // Create context
    SharedClasswideData classwide(klass);
    vf_Context_5 context(classwide);

    // Verify method
    for( index = 0; index < class_get_method_number( klass ); index++ ) {
        result = context.verify_method(class_get_method( klass, index ));

        if (result != VF_OK) {
            vf_create_error_message(class_get_method(klass, index), context, error);
            break;
        }
    }

    /**
    * Set method constraints
    */
    context.set_class_constraints();

    return result;
} // vf_verify5_class

/**
* Provides initial java-6 verification of class.
*
* If when verifying the class a check of type "class A must be assignable to class B" needs to be done 
* and either A or B is not loaded at the moment then a constraint 
* "class A must be assignable to class B" is recorded into the classloader's data
*/
vf_Result
vf_verify6_class(Class_Handle klass, unsigned verifyAll, char **error )
{
    int index;
    vf_Result result = VF_OK;

    // Create contexts
    SharedClasswideData classwide(klass);
    vf_Context_5 context5(classwide);
    vf_Context_6 context6(classwide);

    bool skip_java6_verification_attempt = false;

    // Verify method
    for( index = 0; index < class_get_method_number( klass ); index++ ) {
        Method_Handle method = class_get_method( klass, index );

        //try Java6 verifying (using StackMapTable attribute)
        if( !skip_java6_verification_attempt || method_get_stackmaptable(method) ) {
            result = context6.verify_method(method);

            if (result != VF_OK) {
                //skip Java6 attempts for further methods unless they have StackMapTable
                skip_java6_verification_attempt = true;
                if (result == VF_ErrorStackmap) {
                    //corrupted StackMapTable ==> throw an Error?
                    vf_create_error_message(method, context6, error);
                    return result;
                }
            }
        }

        if( result != VF_OK ) {
            //try Java5 verifying
            result = context5.verify_method(method);
            if (result != VF_OK) {
                vf_create_error_message(method, context5, error);
                return result;
            }
        }
    }

    /**
    * Set method constraints
    */
    context5.set_class_constraints();

    return result;
} // vf_verify6_class


/**
* Provides initial verification of a class.
*
* If when verifying the class a check of type "class A must be assignable to class B" needs to be done 
* and either A or B is not loaded at the moment then a constraint 
* "class A must be assignable to class B" is recorded into the classloader's data
*/
vf_Result
vf_verify_class( Class_Handle klass, unsigned verifyAll, char **error ) {
    return class_get_version(klass) >= 50 ? vf_verify6_class(klass, verifyAll, error) : vf_verify5_class(klass, verifyAll, error);
}

/**
* Function verifies all the constraints "class A must be assignable to class B"
* that are recorded into the classloader for the given class
* If some class is not loaded yet -- load it now
*/
vf_Result
vf_verify_class_constraints(Class_Handle klass, unsigned verifyAll, char** error)
{

    // get class loader of current class
    Class_Loader_Handle class_loader = class_get_class_loader(klass);

    // get class loader verify data
    vf_ClassLoaderData_t *cl_data =
        (vf_ClassLoaderData_t*)class_loader_get_verifier_data_ptr(class_loader);

    // check class loader data
    if( cl_data == NULL ) {
        // no constraint data
        return VF_OK;
    }

    // get class hash and memory pool
    vf_Hash *hash = cl_data->hash;

    // get constraints for class
    vf_HashEntry_t *hash_entry = hash->Lookup( class_get_name( klass ) );
    if( !hash_entry || !hash_entry->data_ptr ) {
        // no constraint data
        return VF_OK;
    }

    // check method constraints
    vf_TypeConstraint *constraint = (vf_TypeConstraint*)hash_entry->data_ptr;
    for( ; constraint; constraint = constraint->next )
    {
        vf_Result result = vf_force_check_constraint( klass, constraint );
        if( result != VF_OK ) {
            vf_create_error_message(klass, constraint, error);
            return result;
        }
    }

    return VF_OK;
} // vf_verify_method_constraints

/**
* Releases verify data in class loader (used to store constraints)
*/
void
vf_release_verify_data( void *data )
{
    vf_ClassLoaderData_t *cl_data = (vf_ClassLoaderData_t*)data;

    delete cl_data->string;
    delete cl_data->hash;
    delete cl_data->pool;
} // vf_release_verify_data

#ifndef _NDEBUG
void method_remove_exc_handler( Method_Handle method, unsigned short idx ) {
    //unimplemented, needed for stack map re-computation testing
    assert(0);
}

void method_modify_exc_handler_info( Method_Handle method, unsigned short idx,
                                     unsigned short start_pc, unsigned short end_pc,
                                     unsigned short handler_pc, unsigned short handler_cp_index )
{
    //unimplemented
    assert(0);
}

unsigned short class_cp_get_class_entry(Class_Handle k_class, const char* name) {
    for( unsigned short i = 1; i < class_cp_get_size( k_class ); i++ ) {
        if( class_cp_get_tag( k_class, i ) == _CONSTANT_Class ) {
            unsigned short name_idx = class_cp_get_class_name_index(k_class, i);
            const char* cp_name = class_cp_get_utf8_bytes( k_class, name_idx );

            if( !strcmp(name, cp_name) ) return i;
        }
    }
    return 0;
}
#endif

