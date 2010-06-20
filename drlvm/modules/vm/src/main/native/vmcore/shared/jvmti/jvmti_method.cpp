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
 * JVMTI method API
 */
#define LOG_DOMAIN "jvmti.method"
#include "cxxlog.h"
#include "vm_log.h"
#include <string.h>

#include "open/vm_method_access.h"
#include "Class.h"
#include "class_member.h"
#include "vm_strings.h"
#include "jvmti_direct.h"
#include "object_handles.h"
#include "jvmti_utils.h"

#include "jvmti_interface.h"
#include "suspend_checker.h"
#include "jvmti_internal.h"
#include "environment.h"
#include "exceptions.h"
#include "interpreter_exports.h"
#include "jvmti_break_intf.h"

/*
 * Get Method Name (and Signature)
 *
 * For the method indicated by method, return the method name via
 * name_ptr and method signature via signature_ptr.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetMethodName(jvmtiEnv* env,
                   jmethodID method,
                   char** name_ptr,
                   char** signature_ptr,
                   char** generic_ptr)
{
    TRACE("GetMethodName called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if( !method ) return JVMTI_ERROR_NULL_POINTER;
    // Either name_ptr, signature_ptr, or generic_ptr can be NULL
    // In this case they are not returned

    char* mtd_name;
    char* mtd_sig;
    Method* mtd = reinterpret_cast<Method*>(method);
    jvmtiError err;
    if( name_ptr )
    {
        const String* name = mtd->get_name();
        err = _allocate(name->len + 1, (unsigned char**)(&mtd_name));
        if(err != JVMTI_ERROR_NONE)
            return err;
        // copy method name
        strcpy(mtd_name, name->bytes);
        *name_ptr = mtd_name;
    }

    if( signature_ptr )
    {
        const String* sig = mtd->get_descriptor();
        err = _allocate(sig->len + 1, (unsigned char**)(&mtd_sig));
        if(err != JVMTI_ERROR_NONE)
        {
            if(name_ptr && mtd_name)
                _deallocate((unsigned char*)mtd_name);
            return err;
        }
        // copy method signature
        strcpy(mtd_sig, sig->bytes);
        *signature_ptr = mtd_sig;
    }

    // ppervov: no generics support in VM as of yet
    if( generic_ptr )
        *generic_ptr = NULL;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Method Declaring Class
 *
 * For the method indicated by method, return the class that
 * defined it via declaring_class_ptr.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetMethodDeclaringClass(jvmtiEnv* env,
                             jmethodID method,
                             jclass* declaring_class_ptr)
{
    TRACE("GetMethodDeclaringClass called for " << method);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if( !method ) return JVMTI_ERROR_INVALID_FIELDID;
    if( !declaring_class_ptr ) return JVMTI_ERROR_NULL_POINTER;

    Method* mtd = (Method*)method;
    Class* cls = mtd->get_class();

    ObjectHandle hclss = struct_Class_to_java_lang_Class_Handle(cls);
    ObjectHandle newH = NewLocalRef(p_TLS_vmthread->jni_env, hclss);

    *declaring_class_ptr = (jclass)newH;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Method Modifiers
 *
 * For the method indicated by method, return the access flags
 * via modifiers_ptr.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetMethodModifiers(jvmtiEnv* env,
                        jmethodID method,
                        jint* modifiers_ptr)
{
    TRACE("GetMethodModifiers called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if( !method ) return JVMTI_ERROR_NULL_POINTER;
    if( !modifiers_ptr ) return JVMTI_ERROR_NULL_POINTER;

    *modifiers_ptr = 0;
    Method* mtd = reinterpret_cast<Method*>(method);
    if( mtd->is_public() ) *modifiers_ptr |= ACC_PUBLIC;
    if( mtd->is_private() ) *modifiers_ptr |= ACC_PRIVATE;
    if( mtd->is_protected() ) *modifiers_ptr |= ACC_PROTECTED;
    if( mtd->is_static() ) *modifiers_ptr |= ACC_STATIC;
    if( mtd->is_final() ) *modifiers_ptr |= ACC_FINAL;
    if( mtd->is_synchronized() ) *modifiers_ptr |= ACC_SYNCHRONIZED;
    if( mtd->is_native() ) *modifiers_ptr |= ACC_NATIVE;
    if( mtd->is_abstract() ) *modifiers_ptr |= ACC_ABSTRACT;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Max Locals
 *
 * For the method indicated by method, return the number of local
 * variable slots used by the method, including the local variables
 * used to pass parameters to the method on its invocation.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetMaxLocals(jvmtiEnv* env,
                  jmethodID method,
                  jint* max_ptr)
{
    TRACE("GetMaxLocals called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if( !method ) return JVMTI_ERROR_NULL_POINTER;
    if( !max_ptr ) return JVMTI_ERROR_NULL_POINTER;

    *max_ptr = reinterpret_cast<Method*>(method)->get_max_locals();

    return JVMTI_ERROR_NONE;
}

/*
 * Get Arguments Size
 *
 * For the method indicated by method, return via max_ptr the
 * number of local variable slots used by the method's arguments.
 * Note that two-word arguments use two slots
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetArgumentsSize(jvmtiEnv* env,
                      jmethodID method,
                      jint* size_ptr)
{
    TRACE("GetArgumentsSize called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if( !method ) return JVMTI_ERROR_NULL_POINTER;
    if( !size_ptr ) return JVMTI_ERROR_NULL_POINTER;

    *size_ptr = reinterpret_cast<Method*>(method)->get_num_arg_slots();

    return JVMTI_ERROR_NONE;
}

/*
 * Get Line Number Table
 *
 * For the method indicated by method, return a table of source
 * line number entries. The size of the table is returned via
 * entry_count_ptr and the table itself is returned via table_ptr.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetLineNumberTable(jvmtiEnv* env,
                        jmethodID method,
                        jint* entry_count_ptr,
                        jvmtiLineNumberEntry** table_ptr)
{
    TRACE("GetLineNumberTable called");
    SuspendEnabledChecker sec;
    int index,
        count;
    Method *method_ptr;
    jvmtiError result;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_get_line_numbers);

    /**
     * Check entry_count_ptr and table_ptr
     */
    if( !entry_count_ptr || !table_ptr ) {
        return JVMTI_ERROR_NULL_POINTER;
    }
    /**
     * Check method
     */
    if( !method ) {
        return JVMTI_ERROR_INVALID_METHODID;
    } else if( ((Method*)method)->is_native() ) {
        return JVMTI_ERROR_NATIVE_METHOD;
    }

    /**
     * Get method line number table entries number
     */
    method_ptr = (Method*)method;
    count = method_ptr->get_line_number_table_size();
    if( count == 0 ) {
        return JVMTI_ERROR_ABSENT_INFORMATION;
    }

    /**
     * Allocate memory for line number table
     */
    *entry_count_ptr = count;
    result = _allocate( count * sizeof(jvmtiLineNumberEntry),
                        (unsigned char**)table_ptr );
    if( result != JVMTI_ERROR_NONE ) {
        return result;
    }

    /**
     * Set line number table
     */
    for( index = 0; index < count; ++index)
    {
        jvmtiLineNumberEntry* entry = *table_ptr + index;
        method_ptr->get_line_number_entry(index,
            &(entry->start_location),
            &(entry->line_number));
    }

    return JVMTI_ERROR_NONE;
} // jvmtiGetLineNumberTable

/*
 * Get Method Location
 *
 * For the method indicated by method, return the beginning and
 * ending addresses through start_location_ptr and end_location_ptr.
 * In a conventional byte code indexing scheme, start_location_ptr
 * will always point to zero and end_location_ptr will always
 * point to the byte code count minus one.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetMethodLocation(jvmtiEnv* env,
                       jmethodID method,
                       jlocation* start_location_ptr,
                       jlocation* end_location_ptr)
{
    TRACE("GetMethodLocation called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    /**
     * Check start_location_ptr and end_location_ptr
     */
    if( !start_location_ptr || !end_location_ptr ) return JVMTI_ERROR_NULL_POINTER;

    /**
     * Check start_location_ptr and end_location_ptr
     */
    if( !method ) return JVMTI_ERROR_NULL_POINTER;

    *start_location_ptr = 0;
    *end_location_ptr = reinterpret_cast<Method*>(method)->get_byte_code_size()-1;
    return JVMTI_ERROR_NONE;
}

/*
 * Get Local Variable Table
 *
 * Return local variable information.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetLocalVariableTable(jvmtiEnv* env,
                           jmethodID method,
                           jint* entry_count_ptr,
                           jvmtiLocalVariableEntry** table_ptr)
{
    TRACE("GetLocalVariableTable called");
    SuspendEnabledChecker sec;
    int len,
        index,
        count;
    char *pointer;
    Method *method_ptr;
    jvmtiError result;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_access_local_variables);

    /**
     * Check entry_count_ptr and table_ptr
     */
    if( !entry_count_ptr || !table_ptr ) {
        return JVMTI_ERROR_NULL_POINTER;
    }
    /**
     * Check method
     */
    if( !method ) {
        return JVMTI_ERROR_INVALID_METHODID;
    } else if( ((Method*)method)->is_native() ) {
        return JVMTI_ERROR_NATIVE_METHOD;
    }

    /**
     * Get method local variable table number entries
     */
    method_ptr = (Method*)method;
    count = method_ptr->get_local_var_table_size();
    if( count == 0 ) {
        return JVMTI_ERROR_ABSENT_INFORMATION;
    }

    /**
     * Allocate memory for local variable table
     */
    *entry_count_ptr = count;
    result = _allocate( count * sizeof(jvmtiLocalVariableEntry),
                        (unsigned char**)table_ptr );
    if( result != JVMTI_ERROR_NONE ) {
        return result;
    }

    /**
     * Set local variable table
     */
    for( index = 0; index < count; index++)
    {
        String *name, *type, *generic_type;
        jvmtiLocalVariableEntry* entry = *table_ptr + index;
        method_ptr->get_local_var_entry(index,
            &(entry->start_location),
            &(entry->length),
            &(entry->slot),
            &name, &type, &generic_type);
        // allocate memory for name
        len = get_utf8_length_of_8bit( (const U_8*)name->bytes, name->len);
        result = _allocate( len + 1, (unsigned char**)&pointer );
        if( result != JVMTI_ERROR_NONE ) {
            return result;
        }
        // copy variable name
        utf8_from_8bit( pointer, (const U_8*)name->bytes, name->len);
        // set variable name
        entry->name = pointer;
        // allocate memory for signature
        len = get_utf8_length_of_8bit( (const U_8*)type->bytes, type->len);
        result = _allocate( len + 1, (unsigned char**)&pointer );
        if( result != JVMTI_ERROR_NONE ) {
            return result;
        }
        // copy variable signature
        utf8_from_8bit( pointer, (const U_8*)type->bytes, type->len);
        // set variable signature
        entry->signature = pointer;
        // set variable slot

        if (generic_type) {
            // allocate memory for generic_signature
            len = get_utf8_length_of_8bit( (const U_8*)generic_type->bytes, generic_type->len);
            result = _allocate( len + 1, (unsigned char**)&pointer );
            if( result != JVMTI_ERROR_NONE ) {
                return result;
            }
            // copy variable generic_signature
            utf8_from_8bit( pointer, (const U_8*)generic_type->bytes, generic_type->len);
            // set variable generic_signature
            entry->generic_signature = pointer;
        } else {
            entry->generic_signature = NULL;
        }
    }

    return JVMTI_ERROR_NONE;
} // jvmtiGetLocalVariableTable

/*
 * Get Bytecodes
 *
 * For the method indicated by method, return the byte codes that
 * implement the method. The number of bytecodes is returned via
 * bytecode_count_ptr. The byte codes themselves are returned via
 * bytecodes_ptr.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetBytecodes(jvmtiEnv* env,
                  jmethodID method,
                  jint* bytecode_count_ptr,
                  unsigned char** bytecodes_ptr)
{
    TRACE("GetBytecodes called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_get_bytecodes);

    /**
     * Check is_native_ptr
     */
    if( !bytecode_count_ptr || !bytecodes_ptr ) {
        return JVMTI_ERROR_NULL_POINTER;
    }
    /**
     * Check method
     */
    if( !method ) {
        return JVMTI_ERROR_INVALID_METHODID;
    }

    Method* mtd = (Method*)method;
    if( mtd->is_native() ) return JVMTI_ERROR_NATIVE_METHOD;
    if( mtd->get_byte_code_addr() == NULL ) return JVMTI_ERROR_OUT_OF_MEMORY;

    *bytecode_count_ptr = mtd->get_byte_code_size();
    jvmtiError err = _allocate( *bytecode_count_ptr, bytecodes_ptr );
    if( err != JVMTI_ERROR_NONE ) return err;
    memcpy( *bytecodes_ptr, mtd->get_byte_code_addr(), *bytecode_count_ptr );

    if (interpreter_enabled())
    {
        TIEnv *p_env = (TIEnv *)env;
        VMBreakPoints* vm_brpt = p_env->vm->vm_env->TI->vm_brpt;

        LMAutoUnlock lock(vm_brpt->get_lock());

        for (VMBreakPoint* bpt = vm_brpt->find_method_breakpoint(method); bpt;
             bpt = vm_brpt->find_next_method_breakpoint(bpt, method))
        {
            (*bytecodes_ptr)[bpt->location] =
                (unsigned char)bpt->saved_byte;
        }
    }

    return JVMTI_ERROR_NONE;
}

/*
 * Is Method Native
 *
 * For the method indicated by method, return a value indicating
 * whether the method is native via is_native_ptr
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiIsMethodNative(jvmtiEnv* env,
                    jmethodID method,
                    jboolean* is_native_ptr)
{
    TRACE("IsMethodNative called for " << method);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    /**
     * Check is_native_ptr
     */
    if( !is_native_ptr ) {
        return JVMTI_ERROR_NULL_POINTER;
    }
    /**
     * Check method
     */
    if( !method ) {
        return JVMTI_ERROR_INVALID_METHODID;
    }

    *is_native_ptr = (jboolean)(reinterpret_cast<Method*>(method)->is_native()?JNI_TRUE:JNI_FALSE);
    TRACE("IsMethodNative(" << method << ") = " << *is_native_ptr);

    return JVMTI_ERROR_NONE;
}

/*
 * Is Method Synthetic
 *
 * For the method indicated by method, return a value indicating
 * whether the method is synthetic via is_synthetic_ptr. Synthetic
 * methods are generated by the compiler but not present in the
 * original source code.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiIsMethodSynthetic(jvmtiEnv* env,
                       jmethodID method,
                       jboolean* is_synthetic_ptr)
{
    TRACE("IsMethodSynthetic called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();
    CHECK_CAPABILITY(can_get_synthetic_attribute);

    /**
     * Check is_synthetic_ptr
     */
    if( !is_synthetic_ptr ) {
        return JVMTI_ERROR_NULL_POINTER;
    }
    /**
     * Check method
     */
    if( !method ) {
        return JVMTI_ERROR_INVALID_METHODID;
    }

    /**
     * Get method synthetic
     */
    *is_synthetic_ptr = (jboolean)(reinterpret_cast<Method*>(method)->is_synthetic() ? JNI_TRUE : JNI_FALSE);

    return JVMTI_ERROR_NONE;
} // jvmtiIsMethodSynthetic

/*
 * Is Method Obsolete
 *
 * Has this method been made obsolete with RedefineClasses?
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiIsMethodObsolete(jvmtiEnv* env,
                      jmethodID method,
                      jboolean* is_obsolete_ptr)
{
    TRACE("IsMethodObsolete called for " << method);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();
    CHECK_CAPABILITY(can_redefine_classes);

    /**
     * Check is_obsolete_ptr
     */
    if( !is_obsolete_ptr ) {
        return JVMTI_ERROR_NULL_POINTER;
    }
    /**
     * Check method
     */
    if( !method ) {
        return JVMTI_ERROR_INVALID_METHODID;
    }
    /**
     * Since we don't have RedefineClasses yet, always return false
     */
    *is_obsolete_ptr = JNI_FALSE;

    TRACE("IsMethodObsolete(" << method << ") = " << *is_obsolete_ptr);
    return JVMTI_ERROR_NONE;
} // jvmtiIsMethodObsolete

void jvmti_method_enter_callback(Method_Handle method)
{
    BEGIN_RAISE_AREA;
    TRACE2("jvmti.event.method.entry", "MethodEntry: " << method);
    jvmti_process_method_entry_event(reinterpret_cast<jmethodID>(method));
    END_RAISE_AREA;
}

void jvmti_method_exit_callback(Method_Handle method, jvalue* return_value)
{
    BEGIN_RAISE_AREA;
    TRACE2("jvmti.event.method.exit", "MethodExit: " << method);

    if (method->get_return_java_type() != JAVA_TYPE_VOID)
        jvmti_process_method_exit_event((jmethodID)method, JNI_FALSE, *return_value);
    else
    {
        jvalue jv = { 0 };
        jvmti_process_method_exit_event((jmethodID)method, JNI_FALSE, jv);
    }

    END_RAISE_AREA;
}
