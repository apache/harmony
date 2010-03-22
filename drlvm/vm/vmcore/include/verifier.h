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
 * @author Pavel Rebriy
 */
#ifndef _VERIFIER_H_
#define _VERIFIER_H_

#include "open/types.h"

enum vf_Result
{
    VF_OK,
    VF_ErrorUnknown,            // unknown error occured
    VF_ErrorInstruction,        // instruction coding error
    VF_ErrorConstantPool,       // bad constant pool format
    VF_ErrorLocals,             // incorrect usage of local variables
    VF_ErrorBranch,             // incorrect local branch offset
    VF_ErrorStackOverflow,      // stack overflow
    VF_ErrorStackUnderflow,     // stack underflow
    VF_ErrorStackDepth,         // inconstant stack deep to basic block
    VF_ErrorCodeEnd,            // falling off the end of the code
    VF_ErrorHandler,            // error in method handler
    VF_ErrorDataFlow,           // data flow error
    VF_ErrorIncompatibleArgument,       // incompatible argument to function
    VF_ErrorLoadClass,          // error load class
    VF_ErrorResolve,            // error resolve field/method
    VF_ErrorJsrRecursive,       // found a recursive subroutine call sequence
    VF_ErrorJsrMultipleRet,     // subroutine splits execution into several
    // <code>ret</code> instructions
    VF_ErrorJsrLoadRetAddr,     // loaded return address from local variable
    VF_ErrorJsrOther,           // invalid subroutine
    VF_ClassNotLoaded,          // verified class not loaded yet
    VF_ErrorInternal,           // error in verification process
    VF_ErrorStackmap,           // error in stackmap attribute
    VF_Continue                 // intermediate status, continue analysis
};

/**
 * Function provides initial verification of class.
 * @param klass     - class handler
 * @param verifyAll - if flag is set, verifier provides full verification checks
 * @param error     - error message of verifier
 * @return Class verification result.
 * @note Assertion is raised if klass is equal to null.
 */
vf_Result
vf_verify_class(Class_Handle klass, unsigned verifyAll, char** error);

/**
 * Function provides final constraint checks for a given class.
 * @param klass     - klass handler
 * @param verifyAll - if flag is set, verifier provides full verification checks
 * @param error     - error message of verifier
 * @return Class verification result.
 * @note Assertion is raised if klass or error_message are equal to null.
 */
vf_Result
vf_verify_class_constraints(Class_Handle klass, unsigned verifyAll,
                            char** error);

/**
 * Function releases error message previously allocated to report an error.
 * @param error - error message of verifier
 * @note Assertion is raised if error_message is equal to null.
 */
void vf_release_memory(void* error);

/**
 * Function releases verify data in class loader.
 * @param data - verify data
 * @note Assertion is raised if data is equal to null.
 */
void vf_release_verify_data(void* data);

#endif // _VERIFIER_H_
