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
 * @author Gregory Shimansky
 */  
/*
 * JVMTI field API
 */

#include "jvmti_direct.h"
#include "Class.h"
#include "object_handles.h"
#include "vm_strings.h"
#include "jvmti_utils.h"
#include "jvmti_internal.h"
#include "cxxlog.h"

#include "suspend_checker.h"

/*
 * Get Field Name (and Signature)
 *
 * For the field indicated by klass and field, return the field
 * name via name_ptr and field signature via signature_ptr.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetFieldName(jvmtiEnv* env,
                  jclass klass,
                  jfieldID field,
                  char** name_ptr,
                  char** signature_ptr,
                  char** generic_ptr)
{
    TRACE2("jvmti.field", "GetFieldName called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (! is_valid_class_object(klass))
        return JVMTI_ERROR_INVALID_CLASS;

    if( !field ) return JVMTI_ERROR_INVALID_FIELDID; // (25)

    char* fld_name;
    char* fld_sig;
    Field* fld = reinterpret_cast<Field*>(field);
    jvmtiError err;
    if( name_ptr )
    {
        const String* name = fld->get_name();
        err = _allocate(name->len + 1, (unsigned char**)(&fld_name));
        if(err != JVMTI_ERROR_NONE)
            return err;
        // copy field name
        strcpy(fld_name, name->bytes);
        *name_ptr = fld_name;
    }

    if( signature_ptr )
    {
        const String* sig = fld->get_descriptor();
        err = _allocate(sig->len + 1, (unsigned char**)(&fld_sig));
        if(err != JVMTI_ERROR_NONE)
        {
            if(name_ptr && fld_name)
                _deallocate((unsigned char*)fld_name);
            return err;
        }
        // copy field signature
        strcpy(fld_sig, sig->bytes);
        *signature_ptr = fld_sig;
    }

    // ppervov: no generic signatures support in VM as of yet
    if( generic_ptr )
        *generic_ptr = NULL;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Field Declaring Class
 *
 * For the field indicated by klass and field return the class
 * that defined it via declaring_class_ptr. The declaring class
 * will either be klass, a superclass, or an implemented interface.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetFieldDeclaringClass(jvmtiEnv* env,
                            jclass klass,
                            jfieldID field,
                            jclass* declaring_class_ptr)
{
    TRACE2("jvmti.field", "GetFieldDeclaringClass called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (! is_valid_class_object(klass))
        return JVMTI_ERROR_INVALID_CLASS;
    
    if( !field ) return JVMTI_ERROR_INVALID_FIELDID;

    if( !declaring_class_ptr ) return JVMTI_ERROR_NULL_POINTER;

    Class* cls = reinterpret_cast<Field*>(field)->get_class();

    ObjectHandle hclss = struct_Class_to_java_lang_Class_Handle(cls);
    ObjectHandle newH = NewLocalRef(p_TLS_vmthread->jni_env, hclss);

    *declaring_class_ptr = (jclass)newH;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Field Modifiers
 *
 * For the field indicated by klass and field return the access
 * flags via modifiers_ptr.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetFieldModifiers(jvmtiEnv* env,
                       jclass klass,
                       jfieldID field,
                       jint* modifiers_ptr)
{
    TRACE2("jvmti.field", "GetFieldModifiers called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (! is_valid_class_object(klass))
        return JVMTI_ERROR_INVALID_CLASS;
    
    if( !field ) return JVMTI_ERROR_INVALID_FIELDID;

    if( !modifiers_ptr ) return JVMTI_ERROR_NULL_POINTER;

    *modifiers_ptr = 0;
    Field* fld = (Field*)field;
    if( fld->is_public() ) *modifiers_ptr |= ACC_PUBLIC;
    if( fld->is_private() ) *modifiers_ptr |= ACC_PRIVATE;
    if( fld->is_protected() ) *modifiers_ptr |= ACC_PROTECTED;
    if( fld->is_static() ) *modifiers_ptr |= ACC_STATIC;
    if( fld->is_final() ) *modifiers_ptr |= ACC_FINAL;
    if( fld->is_volatile() ) *modifiers_ptr |= ACC_VOLATILE;
    if( fld->is_transient() ) *modifiers_ptr |= ACC_TRANSIENT;

    return JVMTI_ERROR_NONE;
}

/*
 * Is Field Synthetic
 *
 * For the field indicated by klass and field, return a value
 * indicating whether the field is synthetic via is_synthetic_ptr.
 * Synthetic fields are generated by the compiler but not present
 * in the original source code.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiIsFieldSynthetic(jvmtiEnv* env,
                      jclass klass,
                      jfieldID field,
                      jboolean* is_synthetic_ptr)
{
    TRACE2("jvmti.field", "IsFieldSynthetic called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};
    CHECK_EVERYTHING();
    CHECK_CAPABILITY(can_get_synthetic_attribute);

    if (! is_valid_class_object(klass))
        return JVMTI_ERROR_INVALID_CLASS;

    if( !field ) return JVMTI_ERROR_INVALID_FIELDID;
    if( !is_synthetic_ptr ) return JVMTI_ERROR_NULL_POINTER;

    *is_synthetic_ptr = (jboolean)(reinterpret_cast<Field*>(field)->is_synthetic() ? JNI_TRUE : JNI_FALSE);

    return JVMTI_ERROR_NONE;
} // jvmtiIsFieldSynthetic

