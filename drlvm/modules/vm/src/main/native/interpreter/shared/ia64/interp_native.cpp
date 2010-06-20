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
#include "interpreter.h"
#include "interpreter_exports.h"
#include "interpreter_imports.h"
#include "exceptions.h"
#include "mon_enter_exit.h"

#include "interp_native.h"
#include "interp_defs.h"
#include "ini.h"
#include "vtable.h"
#include "open/vm_method_access.h"



/* implementation of "get_stacked_register_address" for interpreter */
uint64* interpreter_get_stacked_register_address(uint64* bsp, unsigned reg) {
    M2nFrame *m2n = (M2nFrame*) bsp;
    switch(reg) {
        case M2N_SAVED_M2NFL:
            return (uint64*) &m2n->prev_m2nf;
        case M2N_OBJECT_HANDLES:
            return (uint64*) &m2n->local_object_handles;
        case M2N_METHOD:
            INFO("get_stacked_register_address for method:");
            LDIE(74, "Unexpected register");
        case M2N_FRAME_TYPE:
            return (uint64*) &m2n->current_frame_type;
            
        default:
            INFO("get_stacked_register_address: " << (int)reg);
            LDIE(74, "Unexpected register");
    }
    return 0;
}

extern "C" {
    int64 invokeJNI(uword *args, uword *fpargs, int64 count, int64 frame, GenericFunctionPointer f);

    ManagedObject** invokeJNI_Ref(uword*,uword*,int64,int64,GenericFunctionPointer);
    void* invokeJNI_Obj(uword*,uword*,int64,int64,GenericFunctionPointer);
    float invokeJNI_Float(uword*,uword*,int64,int64,GenericFunctionPointer);
    double invokeJNI_Double(uword*,uword*,int64,int64,GenericFunctionPointer);
    I_32 invokeJNI_Int(uword*,uword*,int64,int64,GenericFunctionPointer);
    int16 invokeJNI_Short(uword*,uword*,int64,int64,GenericFunctionPointer);
    I_8 invokeJNI_Byte(uword*,uword*,int64,int64,GenericFunctionPointer);
    uint16 invokeJNI_Char(uword*,uword*,int64,int64,GenericFunctionPointer);
}

void
interpreter_execute_native_method(
        Method *method,
        jvalue *return_value,
        jvalue *args) {
    assert(!hythread_is_suspend_enabled());

    DEBUG_TRACE("\n<<< interpreter_invoke_native: "
           << method->get_class()->get_name()->bytes << " "
           << method->get_name()->bytes
           << method->get_descriptor()->bytes);

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> interpreter_invoke_native >>>\n");
        return;
    }

    M2N_ALLOC_MACRO;
    hythread_suspend_enable();
    
    int sz = method->get_num_arg_slots();
    uword *arg_words = (uword*) ALLOC_FRAME((sz + 2) * sizeof(uword));
    uword fpargs[6 + 1/* for fptypes */];
    // types of fpargs[6], 0 - float, 1 - double
    char *fptypes = (char*)&fpargs[6];
    int nfpargs = 0;

    int argId = 0;
    int pos = 0;
    arg_words[argId++] = (uword) get_jni_native_intf();

    jobject _this;
    if (method->is_static()) {
        _this = (jobject) method->get_class()->get_class_handle();
    } else {
        _this = args[pos++].l;
    }
    arg_words[argId++] = (uword) _this;

    const char *mtype = method->get_descriptor()->bytes + 1;
    assert(mtype != 0);

    for(; *mtype != ')'; mtype++) {
        switch(*mtype) {
            case JAVA_TYPE_CLASS:
            case JAVA_TYPE_ARRAY:
                {
                    jobject obj = args[pos++].l;
                    ObjectHandle UNUSED h = (ObjectHandle) obj;
                    arg_words[argId++] = (uword) obj;

                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_SHORT:
            case JAVA_TYPE_BYTE:
            case JAVA_TYPE_INT:
                // sign extend
                arg_words[argId++] = (uword)(word) args[pos++].i;
                break;

            case JAVA_TYPE_FLOAT:
                if (nfpargs < 6) {
                    fptypes[nfpargs] = 0;
                    *(float*)&fpargs[nfpargs++] = args[pos].f;
                }
                pos++;
                break;

            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                // zero extend
                arg_words[argId++] = (word) args[pos++].i;
                break;

            case JAVA_TYPE_DOUBLE:
                if (nfpargs < 6) {
                    fptypes[nfpargs] = 1;
                    fpargs[nfpargs++] = args[pos].j;
                }
                pos++;
                break;

            case JAVA_TYPE_LONG:
                arg_words[argId++] = args[pos++].j;
                break;
            default:
                LDIE(53, "Unexpected java type");
        }
    }
    assert(argId <= sz + 2);

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT)
        method_entry_callback(method);

    if (method->is_synchronized()) {
        assert(hythread_is_suspend_enabled());
        jthread_monitor_enter(_this);
    }

    int frameSize = 0;
    if (argId > 8) {
        frameSize = -((((argId - 8) + 1) & ~1) << 3);
    }

    jvalue *resultPtr = return_value;
    Java_Type ret_type = method->get_return_java_type();

    DEBUG("invokeJNI:3 \n");

    switch(ret_type) {
        case JAVA_TYPE_VOID:
            invokeJNI(arg_words, fpargs, argId, frameSize, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                jobject obj = (jobject) invokeJNI_Obj(arg_words, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                if (obj) {
                    ManagedObject *ref = obj->object;
                    M2N_FREE_MACRO;
                    ObjectHandle new_handle = oh_allocate_local_handle();
                    new_handle->object = ref;
                    resultPtr->l = new_handle;
                } else {
                    M2N_FREE_MACRO;
                    resultPtr->l = NULL;
                }
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
            resultPtr->i = invokeJNI_Int(arg_words, fpargs, argId, frameSize, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_FLOAT:
            resultPtr->f = invokeJNI_Float(arg_words, fpargs, argId, frameSize, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_LONG:
            resultPtr->j = invokeJNI(arg_words, fpargs, argId, frameSize, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_DOUBLE:
            resultPtr->d = invokeJNI_Double(arg_words, fpargs, argId, frameSize, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        default:
            LDIE(53, "Unexpected java type");
    }
    TRACE("invokeJNI: done\n");

    if (exn_raised()) {
        if ((resultPtr != NULL) && (ret_type != JAVA_TYPE_VOID)) {   
            DEBUG_TRACE("<EXCEPTION> ");
            resultPtr->l = 0; //clear result
        }
    }

    if (method->is_synchronized()) {
        vm_monitor_exit_wrapper(_this->object);
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_EXIT_EVENT) {
        jvalue val;
        method_exit_callback(method,
                exn_raised(),
                resultPtr != 0 ? *resultPtr : (val.j = 0, val));
    }

    DEBUG_TRACE("interpreter_invoke_native >>>\n");
    FREE_FRAME(arg_words);
}

void
interpreterInvokeStaticNative(StackFrame& prevFrame, StackFrame& frame, Method *method) {

    DEBUG_TRACE("\n<<< native_invoke_static     : " << method);
    TRACE("interpreter static native: " << frame.method);

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> interpreter_invoke_native >>>\n");
        return;
    }

    M2N_ALLOC_MACRO;
    
    frame.This = *(method->get_class()->get_class_handle());
    int sz = method->get_num_arg_slots();
    uword *args = (uword*) ALLOC_FRAME((sz + 2) * sizeof(uword));
    uword fpargs[6 + 1/* for fptypes */];
    // types of fpargs[6], 0 - float, 1 - double
    char *fptypes = (char*)&fpargs[6];
    int nfpargs = 0;

    args[0] = (uword) get_jni_native_intf();
    args[1] = (uword) &frame.This; 
    int argId = 2;
    int pos = sz - 1;

    const char *mtype = method->get_descriptor()->bytes + 1;
    assert(mtype != 0);

    for(; pos >= 0; mtype++) {

        switch(*mtype) {
            case JAVA_TYPE_CLASS:
            case JAVA_TYPE_ARRAY:
                {
                    ASSERT_TAGS(prevFrame.stack.ref(pos));
                    REF& ref = prevFrame.stack.pick(pos--).ref;
                    ASSERT_OBJECT(UNCOMPRESS_INTERP(ref));
                    if (ref == 0) {
                        args[argId++] = 0;
                    } else {
#ifdef REF32
                        ObjectHandle new_handle = oh_allocate_local_handle();
                        new_handle->object = UNCOMPRESS_INTERP(ref);
                        args[argId++] = (uword) new_handle;
#else
                        args[argId++] = (uword) &ref;
#endif
                    }
                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_SHORT:
            case JAVA_TYPE_BYTE:
            case JAVA_TYPE_INT:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                // sign extend
                args[argId++] = (uword)(word) prevFrame.stack.pick(pos--).i;
                break;

            case JAVA_TYPE_FLOAT:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                if (nfpargs < 6) {
                    fptypes[nfpargs] = 0;
                    *(float*)&fpargs[nfpargs++] = prevFrame.stack.pick(pos).f;
                }
                pos--;
                break;

            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                // zero extend
                args[argId++] = prevFrame.stack.pick(pos--).u;
                break;

            case JAVA_TYPE_LONG:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                args[argId] = prevFrame.stack.getLong(pos-1).u64;
                argId++;
                pos-= 2;
                break;

            case JAVA_TYPE_DOUBLE:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                args[argId] = prevFrame.stack.getLong(pos-1).u64;
                if (nfpargs < 6) {
                    fptypes[nfpargs] = 1;
                    fpargs[nfpargs++] = prevFrame.stack.getLong(pos-1).u64;
                }
                argId++;
                pos-= 2;
                break;
            default:
                LDIE(53, "Unexpected java type");
        }
    }
    assert(*mtype == ')');
    assert(argId <= sz + 2);

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT) {
        method_entry_callback(method);
    }

    if (method->is_synchronized()) {
        vm_monitor_enter_wrapper(frame.This);
    }

    int frameSize = 0;
    if (argId > 8) {
        frameSize = -((((argId - 8) + 1) & ~1) << 3);
    }


    hythread_suspend_enable();

    DEBUG("invokeJNI: 1\n");

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            {
                invokeJNI(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);
            }
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject **ref = invokeJNI_Ref(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                REF stack_ref;
                prevFrame.stack.push();
                if (ref != 0) {
                    ASSERT_OBJECT(*ref);
                    if (!*ref) {
                        INFO(
                        "VM WARNING: Reference with null value returned from jni function:\n"
                        "VM WARNING: Method name: "
                        << method->get_class()->get_name()->bytes
                        << "/" << method->get_name()->bytes
                        << method->get_descriptor()->bytes <<
                        "\nVM WARNING: Not allowed, return NULL (0) instead\n");
                    }
                    if (*ref) {
                        stack_ref = COMPRESS_INTERP(*ref);
                    } else {
                        stack_ref = 0;
                    }
                } else {
                    stack_ref = 0;
                }
                prevFrame.stack.pick().ref = stack_ref;
                prevFrame.stack.ref() = FLAG_OBJECT;
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
            {
                I_8 res = invokeJNI_Byte(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32)res;
            }
            break;

        case JAVA_TYPE_CHAR:
            {
                uint16 res = invokeJNI_Char(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().u = (U_32) res;
            }
            break;

        case JAVA_TYPE_SHORT:
            {
                int16 res = invokeJNI_Short(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_INT:
            {
                Value res;
                res.i = invokeJNI_Int(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_FLOAT:
            {
                Value res;
                res.f = invokeJNI_Float(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_LONG:
            {
                Value2 res;
                res.i64 = invokeJNI(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        case JAVA_TYPE_DOUBLE:
            {
                Value2 res;
                res.d = invokeJNI_Double(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        default:
            LDIE(53, "Unexpected java type");
    }
    TRACE("invokeJNI: done\n");

    if (method->is_synchronized()) {
        vm_monitor_exit_wrapper(frame.This);
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_EXIT_EVENT)
        method_exit_callback_with_frame(method, prevFrame);

    M2N_FREE_MACRO;
    FREE_FRAME(args);
    DEBUG_TRACE("native_invoke_static >>>\n");
}

void
interpreterInvokeVirtualNative(StackFrame& prevFrame, StackFrame& frame, Method *method, int sz) {
    assert(method->is_native());
    assert(!method->is_static());

    TRACE("interpreter virtual native: " << frame.method);
    DEBUG_TRACE("\n<<< native_invoke_virtual: " << method);

    uword *args = (uword*) ALLOC_FRAME((sz + 1) * sizeof(uword));
    uword fpargs[6 + 1 /* for fptypes */];
    // types of fpargs[6], 0 - float, 1 - double
    char *fptypes = (char*)&fpargs[6];
    int nfpargs = 0;

    args[0] = (uword) get_jni_native_intf();
    args[1] = (uword) &frame.This;
    int argId = 2;
    int pos = sz - 2;

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> native_invoke_virtual>>>\n");
        return;
    }
    M2N_ALLOC_MACRO;

    const char *mtype = method->get_descriptor()->bytes + 1;
    assert(mtype != 0);

    for(; pos >= 0; mtype++) {

        switch(*mtype) {
            case JAVA_TYPE_CLASS:
            case JAVA_TYPE_ARRAY:
                {
                    ASSERT_TAGS(prevFrame.stack.ref(pos));
                    REF& ref = prevFrame.stack.pick(pos--).ref;
                    ASSERT_OBJECT(UNCOMPRESS_INTERP(ref));
                    if (ref == 0) {
                        args[argId++] = 0;
                    } else {
#ifdef REF32
                        ObjectHandle new_handle = oh_allocate_local_handle();
                        new_handle->object = UNCOMPRESS_INTERP(ref);
                        args[argId++] = (uword) new_handle;
#else
                        args[argId++] = (uword) &ref;
#endif
                    }
                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_FLOAT:
                // zero extend
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                if (nfpargs < 6) {
                    fptypes[nfpargs] = 0;
                    *(float*)&fpargs[nfpargs++] = prevFrame.stack.pick(pos).f;
                }
                pos--;
                break;

            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                // zero extend
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                args[argId++] = prevFrame.stack.pick(pos--).u;
                break;

            case JAVA_TYPE_BYTE:
            case JAVA_TYPE_SHORT:
            case JAVA_TYPE_INT:
                // sign extend
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                args[argId++] = (uword)(word) prevFrame.stack.pick(pos--).i;
                break;

            case JAVA_TYPE_LONG:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                args[argId] = prevFrame.stack.getLong(pos-1).u64;
                argId++;
                pos-=2;
                break;

            case JAVA_TYPE_DOUBLE:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                args[argId] = prevFrame.stack.getLong(pos-1).u64;
                if (nfpargs < 6) {
                    fptypes[nfpargs] = 1;
                    fpargs[nfpargs++] = prevFrame.stack.getLong(pos-1).u64;
                }
                argId++;
                pos-=2;
                break;
            default:
                LDIE(53, "Unexpected java type");
        }
    }
    assert(*mtype == ')');
    assert(argId <= sz + 2);

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT) {
        method_entry_callback(method);
    }

    if (method->is_synchronized()) {
        vm_monitor_enter_wrapper(frame.This);
    }
    
    int frameSize = 0;
    if (argId > 8) {
        frameSize = -((((argId - 8) + 1) & ~1) << 3);
    }
    
    hythread_suspend_enable();

    DEBUG("invokeJNI: 2\n");

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            {
                invokeJNI(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);
            }
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject ** ref = invokeJNI_Ref(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                REF stack_ref;
                prevFrame.stack.push();
                if (ref != 0) {
                    ASSERT_OBJECT(*ref);
                    if (!*ref) {
                        INFO(
                        "VM WARNING: Reference with null value returned from jni function:\n"
                        "VM WARNING: Method name: "
                        << method->get_class()->get_name()->bytes
                        << "/" << method->get_name()->bytes
                        << method->get_descriptor()->bytes <<
                        "\nVM WARNING: Not allowed, return NULL (0) instead\n");

                    }
                    if (*ref) {
                        stack_ref = COMPRESS_INTERP(*ref);
                    } else {
                        stack_ref = 0;
                    }
                } else {
                    stack_ref = 0;
                }
                prevFrame.stack.pick().ref = stack_ref;
                prevFrame.stack.ref() = FLAG_OBJECT;
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
            {
                I_8 res = invokeJNI_Byte(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_CHAR:
            {
                uint16 res = invokeJNI_Char(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().u = (U_32) res;
            }
            break;

        case JAVA_TYPE_SHORT:
            {
                int16 res = invokeJNI_Short(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_INT:
            {
                Value res;
                res.i = invokeJNI_Int(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_FLOAT:
            {
                Value res;
                res.f = invokeJNI_Float(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_LONG:
            {
                Value2 res;
                res.i64 = invokeJNI(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        case JAVA_TYPE_DOUBLE:
            {
                Value2 res;
                res.d = invokeJNI_Double(args, fpargs, argId, frameSize, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        default:
            LDIE(53, "Unexpected java type");
    }
    TRACE("invokeJNI: done\n");

    if (method->is_synchronized()) {
        vm_monitor_exit_wrapper(frame.This);
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_EXIT_EVENT)
        method_exit_callback_with_frame(method, prevFrame);

    M2N_FREE_MACRO;
    DEBUG_TRACE("native_invoke_virtual >>>\n");
    FREE_FRAME(args);
}
