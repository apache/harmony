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
 * @author Pavel Pervov
 */


#define LOG_DOMAIN "verifier"
#include "cxxlog.h"

#include "verifier.h"
#include "Class.h"
#include "classloader.h"
#include "environment.h"
#include "open/vm.h"
#include "lock_manager.h"

bool Class::verify(const Global_Env * env)
{
    // fast path
    if (m_state >= ST_BytecodesVerified)
        return true;

    LMAutoUnlock aulock(m_lock);
    if (m_state >= ST_BytecodesVerified)
        return true;

    if (is_array()) {
        // no need do bytecode verification for arrays
        m_state = ST_BytecodesVerified;
        return true;
    }

    /**
     * Get verifier enable status
     */
    Boolean is_forced = env->verify_all;
    Boolean is_strict = env->verify_strict;
    Boolean is_bootstrap = m_class_loader->IsBootstrap();
    Boolean is_enabled = env->verify;

    /**
     * Verify class
     */
    if (is_enabled == 1 && !is_interface()
        && (is_bootstrap == FALSE || is_forced == TRUE)) {
        char *error;
        vf_Result result =
            vf_verify_class(this, is_strict, &error);
        if (VF_OK != result) {
            aulock.ForceUnlock();
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                                      "java/lang/VerifyError", error);
            vf_release_memory(error);
            return false;
        }
    }
    m_state = ST_BytecodesVerified;

    return true;
} // Class::verify


bool Class::verify_constraints(const Global_Env * env)
{
    // fast path
    switch (m_state) {
    case ST_ConstraintsVerified:
    case ST_Initializing:
    case ST_Initialized:
    case ST_Error:
        return true;
    }

    // lock class
    lock();

    // check verification stage again
    switch (m_state) {
    case ST_ConstraintsVerified:
    case ST_Initializing:
    case ST_Initialized:
    case ST_Error:
        unlock();
        return true;
    }
    assert(m_state == ST_Prepared);

    if (is_array()) {
        // no need do constraint verification for arrays
        m_state = ST_ConstraintsVerified;
        unlock();
        return true;
    }
    // get verifier enable status
    Boolean is_strict = env->verify_strict;

    // unlock a class before calling to verifier
    unlock();

    // check method constraints
    char *error;
    vf_Result result =
        vf_verify_class_constraints(this, is_strict, &error);

    // lock class and check result
    lock();
    switch (m_state) {
    case ST_ConstraintsVerified:
    case ST_Initializing:
    case ST_Initialized:
    case ST_Error:
        unlock();
        return true;
    }
    if (VF_OK != result) {
        unlock();
        if (result == VF_ErrorLoadClass) {
            // Exception is raised by class loading
            // and passed through verifier unchanged
            assert(exn_raised());
        } else {
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                                      "java/lang/VerifyError", error);
            vf_release_memory(error);
        }
        return false;
    }
    m_state = ST_ConstraintsVerified;

    // unlock class
    unlock();

    return true;
} // Class::verify_constraints
