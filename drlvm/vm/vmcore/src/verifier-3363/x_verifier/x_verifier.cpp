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
#include "recompute.h"
#include "x_verifier.h"
#include "../java6/context_6.h"
#include "time.h"

/**
 * Allocates an empty verification context for a class, 
 * to be passed back to the verifier upon verification requests.
 * Memory must be disposed by calling free_verification_context 
 * (see below).
 * @param klass - class handler
 * @return a verification context for the class
 */
verification_context
allocate_verification_context(Class_Handle klass) {
    return (verification_context) (new SharedClasswideData(klass));
}


/**
 * Initializes the verification context with method's information. 
 * This function must be called before instrumenting the method.
 * The resulting context should be passed back to the verifier 
 * upon verification requests
 * @param method - method handler
 * @param[in,out] context - verification context of the 
 * method's defining class
 * @return error code 
 */
vf_Result
init_verification_context_for_method(Method_Handle method, verification_context context) {
    vf_Context_6 context6( *((SharedClasswideData*)context) );
    return context6.verify_method(method);
}


/**
 * Recomputes the StackMapTable of a method using the verification 
 * context created and initialized prior
 * to method instrumentation
 * @param[out] attrBytes - a pointer to a newly allocated StackMapTable
 * attribute. Memory must be disposed by calling free_stackmaptable
 * (see below).
 * @param method - method handler
 * @param context - class and method verification context
 * @return error code
 */
vf_Result recompute_stackmaptable(U_8** attrBytes, Method_Handle method,
                                  verification_context context)
{
    char* error_message;
    vf_Result result = vf_recompute_stackmaptable(method,
        attrBytes, &error_message,
        ((SharedClasswideData*)context)->class_constraints);
    if(result != VF_OK) {
        vf_release_memory(error_message);
    }
    return result;
}


/**
 * Frees memory allocated for a StackMapTable attribute
 */
void free_stackmaptable(U_8 *attrBytes) {
    tc_free(attrBytes);
}

/**
 * Frees memory allocated for a verification context
 */
void free_verification_context (verification_context context) {
    delete ((SharedClasswideData*)context);
}

