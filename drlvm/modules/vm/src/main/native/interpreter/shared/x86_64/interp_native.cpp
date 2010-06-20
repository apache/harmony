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
 * @author Ivan Volosyuk
 */  

#include "interpreter.h"
#include "interpreter_exports.h"
#include "interpreter_imports.h"

#include "vtable.h"
#include "exceptions.h"
#include "mon_enter_exit.h"
#include "open/vm_method_access.h"
#include "interp_native.h"
#include "interp_defs.h"
#include "ini.h"



extern "C" {
    int64 invokeJNI(uint64 *args, uword n_fps, uword n_stacks, GenericFunctionPointer f);
}

typedef double (*DoubleFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef ManagedObject** (*RefFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef void* (*ObjFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef float (*FloatFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef I_32 (*IntFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef int16 (*ShortFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef I_8 (*ByteFuncPtr)(uword*,uword,word,GenericFunctionPointer);
typedef uint16 (*CharFuncPtr)(uword*,uword,word,GenericFunctionPointer);

DoubleFuncPtr invokeJNI_Double = (DoubleFuncPtr) invokeJNI;
ObjFuncPtr invokeJNI_Obj = (ObjFuncPtr) invokeJNI;
RefFuncPtr invokeJNI_Ref = (RefFuncPtr) invokeJNI;
IntFuncPtr invokeJNI_Int = (IntFuncPtr) invokeJNI;
FloatFuncPtr invokeJNI_Float = (FloatFuncPtr) invokeJNI;
ShortFuncPtr invokeJNI_Short = (ShortFuncPtr) invokeJNI;
CharFuncPtr invokeJNI_Char = (CharFuncPtr) invokeJNI;
ByteFuncPtr invokeJNI_Byte = (ByteFuncPtr) invokeJNI;

#ifdef _WIN32
#define MAX_REG_FLOATS  4
#define MAX_REG_INTS  4
#else
#define MAX_REG_FLOATS  8
#define MAX_REG_INTS  6
#endif

void
interpreter_execute_native_method(
        Method *method,
        jvalue *return_value,
        jvalue *args) {
    assert(!hythread_is_suspend_enabled());


    DEBUG_TRACE("\n<<< interpreter_invoke_native: " << method);

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> native_invoke_virtual>>>\n");
        return;
    }
    M2N_ALLOC_MACRO;
    hythread_suspend_enable();

    int sz = method->get_num_arg_slots();

    int n_ints = 0;
#ifdef _WIN32 /* important difference in calling conventions */
#define n_fps n_ints
#else
    int n_fps = 0;
#endif
    int n_stacks = 0;
    uword *out_args = (uword*) ALLOC_FRAME(8 + sizeof(char) + (MAX_REG_FLOATS + sz + 2) * sizeof(uword));
    uword *fps = out_args + 1;
    uword *ints = fps + MAX_REG_FLOATS;
    uword *stacks = ints + MAX_REG_INTS;

    int pos = 0;
    ints[n_ints++] = (uword) get_jni_native_intf();

    jobject _this;
    if (method->is_static()) {
        _this = (jobject) method->get_class()->get_class_handle();
    } else {
        _this = args[pos++].l;
    }
    ints[n_ints++] = (uword) _this;

    const char *mtype = method->get_descriptor()->bytes + 1;
    assert(mtype != 0);

    for(; *mtype != ')'; mtype++) {
        switch(*mtype) {
            case JAVA_TYPE_CLASS:
            case JAVA_TYPE_ARRAY:
                {
                    jobject obj = args[pos++].l;
                    ObjectHandle UNUSED h = (ObjectHandle) obj;
                    if (n_ints != MAX_REG_INTS) {
                        ints[n_ints++] = (uword) obj;
                    } else {
                        stacks[n_stacks++] = (uword) obj;
                    }
                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_SHORT:
            case JAVA_TYPE_BYTE:
            case JAVA_TYPE_INT:
                // sign extend
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = (uword)(word) args[pos++].i;
                } else {
                    stacks[n_stacks++] = (uword)(word) args[pos++].i;
                }
                break;

            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                // zero extend
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = (word) args[pos++].i;
                } else {
                    stacks[n_stacks++] = (word) args[pos++].i;
                }
                break;

            case JAVA_TYPE_LONG:
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = args[pos++].j;
                } else {
                    stacks[n_stacks++] = args[pos++].j;
                }
                break;

            case JAVA_TYPE_FLOAT:
                if (n_fps != MAX_REG_FLOATS) {
                    *(float*)&fps[n_fps++] = args[pos++].f;
                } else {
                    *(float*)&stacks[n_stacks++] = args[pos++].f;
                }

            case JAVA_TYPE_DOUBLE:
                if (n_fps != MAX_REG_FLOATS) {
                    fps[n_fps++] = args[pos++].j;
                } else {
                    stacks[n_stacks++] = args[pos++].j;
                }

            default:
                LDIE(53, "Unexpected java type");
        }
    }
//    assert(argId <= sz + 2);

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT)
        method_entry_callback(method);

    if (method->is_synchronized()) {
        assert(hythread_is_suspend_enabled());
        jthread_monitor_enter(_this);
    }

    jvalue *resultPtr = return_value;
    Java_Type ret_type = method->get_return_java_type();

    switch(ret_type) {
        case JAVA_TYPE_VOID:
            invokeJNI(out_args, n_fps, n_stacks, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                jobject obj = (jobject) invokeJNI_Obj(out_args, n_fps, n_stacks, f);
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
            resultPtr->i = invokeJNI_Int(out_args, n_fps, n_stacks, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_FLOAT:
            resultPtr->f = invokeJNI_Float(out_args, n_fps, n_stacks, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_LONG:
            resultPtr->j = invokeJNI(out_args, n_fps, n_stacks, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_DOUBLE:
            resultPtr->d = invokeJNI_Double(out_args, n_fps, n_stacks, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

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
    FREE_FRAME(out_args);
}

void
interpreterInvokeStaticNative(StackFrame& prevFrame, StackFrame& frame, Method *method) {

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> interpreter_invoke_native >>>\n");
        return;
    }

    DEBUG_TRACE("\n<<< native_invoke_static     : " << method);
    TRACE("interpreter static native: " << frame.method);

    M2N_ALLOC_MACRO;
    
    int sz = method->get_num_arg_slots();

    int n_ints = 0;
#ifdef _WIN32 /* important difference in calling conventions */
#define n_fps n_ints
#else
    int n_fps = 0;
#endif
    int n_stacks = 0;
    uword *out_args = (uword*) ALLOC_FRAME(8 + sizeof(char) + (MAX_REG_FLOATS + sz + 2) * sizeof(uword));
    uword *fps = out_args + 1;
    uword *ints = fps + MAX_REG_FLOATS;
    uword *stacks = ints + MAX_REG_INTS;

    frame.This = *(method->get_class()->get_class_handle());
    ints[n_ints++] = (uword) get_jni_native_intf();
    ints[n_ints++] = (uword) &frame.This; 
    int pos = sz - 1;
    uword arg;

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
                        arg = 0;
                    } else {
#ifdef REF32
                        ObjectHandle new_handle = oh_allocate_local_handle();
                        new_handle->object = UNCOMPRESS_INTERP(ref);
                        arg = (uword) new_handle;
#else
                        arg = (uword) &ref;
#endif
                    }
                    if (n_ints != MAX_REG_INTS) {
                        ints[n_ints++] = arg;
                    } else {
                        stacks[n_stacks++] = arg;
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
                arg = (uword)(word) prevFrame.stack.pick(pos--).i;
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                break;

            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                // zero extend
                arg = prevFrame.stack.pick(pos--).u;
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                break;

            case JAVA_TYPE_LONG:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                arg = prevFrame.stack.getLong(pos-1).u64;
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                pos -= 2;
                break;

            case JAVA_TYPE_FLOAT:
                {
                    ASSERT_TAGS(!prevFrame.stack.ref(pos));
                    // zero extend
                    float farg = prevFrame.stack.pick(pos--).f;
                    if (n_fps != MAX_REG_FLOATS) {
                        *(float*)&fps[n_fps++] = farg;
                    } else {
                        *(float*)&stacks[n_stacks++] = farg;
                    }
                }
                break;

            case JAVA_TYPE_DOUBLE:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                arg = prevFrame.stack.getLong(pos-1).u64;

                if (n_fps != MAX_REG_FLOATS) {
                    fps[n_fps++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                pos -= 2;
                break;

            default:
                LDIE(53, "Unexpected java type");
        }
    }
    assert(*mtype == ')');

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT) {
        method_entry_callback(method);
    }

    if (method->is_synchronized()) {
        vm_monitor_enter_wrapper(frame.This);
    }

    hythread_suspend_enable();

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            {
                invokeJNI(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);
            }
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject **ref = invokeJNI_Ref(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

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
                    prevFrame.stack.pick().ref = COMPRESS_INTERP(*ref);
                } else {
                    prevFrame.stack.pick().ref = 0;
                }
                prevFrame.stack.ref() = FLAG_OBJECT;
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
            {
                I_8 res = invokeJNI_Byte(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32)res;
            }
            break;

        case JAVA_TYPE_CHAR:
            {
                uint16 res = invokeJNI_Char(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().u = (U_32) res;
            }
            break;

        case JAVA_TYPE_SHORT:
            {
                int16 res = invokeJNI_Short(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_INT:
            {
                Value res;
                res.i = invokeJNI_Int(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_FLOAT:
            {
                Value res;
                res.f = invokeJNI_Float(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_LONG:
            {
                Value2 res;
                res.i64 = invokeJNI(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        case JAVA_TYPE_DOUBLE:
            {
                Value2 res;
                res.d = invokeJNI_Double(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

    if (method->is_synchronized()) {
        vm_monitor_exit_wrapper(frame.This);
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_EXIT_EVENT)
        method_exit_callback_with_frame(method, prevFrame);

    M2N_FREE_MACRO;
    FREE_FRAME(out_args);
    DEBUG_TRACE("native_invoke_static >>>\n");
}

void
interpreterInvokeVirtualNative(StackFrame& prevFrame, StackFrame& frame, Method *method, int sz) {
    assert(method->is_native());
    assert(!method->is_static());

    TRACE("interpreter virtual native: " << frame.method);
    DEBUG_TRACE("\n<<< native_invoke_virtual: " << method);

    int n_ints = 0;
#ifdef _WIN32 /* important difference in calling conventions */
#define n_fps n_ints
#else
    int n_fps = 0;
#endif
    int n_stacks = 0;
    uword *out_args = (uword*) ALLOC_FRAME(8 + sizeof(char) + (MAX_REG_FLOATS + sz + 2) * sizeof(uword));
    uword *fps = out_args + 1;
    uword *ints = fps + MAX_REG_FLOATS;
    uword *stacks = ints + MAX_REG_INTS;

    ints[n_ints++] = (uword) get_jni_native_intf();
    ints[n_ints++] = (uword) &frame.This;
    int pos = sz - 2;

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> native_invoke_virtual>>>\n");
        return;
    }
    M2N_ALLOC_MACRO;

    const char *mtype = method->get_descriptor()->bytes + 1;
    assert(mtype != 0);
    uword arg;

    for(; pos >= 0; mtype++) {

        switch(*mtype) {
            case JAVA_TYPE_CLASS:
            case JAVA_TYPE_ARRAY:
                {
                    ASSERT_TAGS(prevFrame.stack.ref(pos));
                    REF& ref = prevFrame.stack.pick(pos--).ref;
                    ASSERT_OBJECT(UNCOMPRESS_INTERP(ref));
                    if (ref == 0) {
                        arg = 0;
                    } else {
#ifdef REF32
                        ObjectHandle new_handle = oh_allocate_local_handle();
                        new_handle->object = UNCOMPRESS_INTERP(ref);
                        arg = (uword) new_handle;
#else
                        arg = (uword) &ref;
#endif
                    }
                    if (n_ints != MAX_REG_INTS) {
                        ints[n_ints++] = arg;
                    } else {
                        stacks[n_stacks++] = arg;
                    }
                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                // zero extend
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                arg = prevFrame.stack.pick(pos--).u;
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                break;

            case JAVA_TYPE_BYTE:
            case JAVA_TYPE_SHORT:
            case JAVA_TYPE_INT:
                // sign extend
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                arg = (uword)(word) prevFrame.stack.pick(pos--).i;
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                break;

            case JAVA_TYPE_LONG:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                arg = prevFrame.stack.getLong(pos-1).u64;
                if (n_ints != MAX_REG_INTS) {
                    ints[n_ints++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                pos -= 2;
                break;

            case JAVA_TYPE_FLOAT:
                {
                    ASSERT_TAGS(!prevFrame.stack.ref(pos));
                    float farg = prevFrame.stack.pick(pos--).f;
                    if (n_fps != MAX_REG_FLOATS) {
                        *(float*)&fps[n_fps++] = farg;
                    } else {
                        *(float*)&stacks[n_stacks++] = farg;
                    }
                }
                break;
            case JAVA_TYPE_DOUBLE:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                arg = prevFrame.stack.getLong(pos-1).u64;
                if (n_fps != MAX_REG_FLOATS) {
                    fps[n_fps++] = arg;
                } else {
                    stacks[n_stacks++] = arg;
                }
                pos -= 2;
                break;
            default:
                LDIE(53, "Unexpected java type");
        }
    }
    assert(*mtype == ')');

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT) {
        method_entry_callback(method);
    }

    if (method->is_synchronized()) {
        vm_monitor_enter_wrapper(frame.This);
    }
    
    hythread_suspend_enable();

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            {
                invokeJNI(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);
            }
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject ** ref = invokeJNI_Ref(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

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
                    prevFrame.stack.pick().ref = COMPRESS_INTERP(*ref);
                } else {
                    prevFrame.stack.pick().ref = 0;
                }
                prevFrame.stack.ref() = FLAG_OBJECT;
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
            {
                I_8 res = invokeJNI_Byte(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_CHAR:
            {
                uint16 res = invokeJNI_Char(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().u = (U_32) res;
            }
            break;

        case JAVA_TYPE_SHORT:
            {
                int16 res = invokeJNI_Short(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_INT:
            {
                Value res;
                res.i = invokeJNI_Int(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_FLOAT:
            {
                Value res;
                res.f = invokeJNI_Float(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_LONG:
            {
                Value2 res;
                res.i64 = invokeJNI(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        case JAVA_TYPE_DOUBLE:
            {
                Value2 res;
                res.d = invokeJNI_Double(out_args, n_fps, n_stacks, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

    if (method->is_synchronized()) {
        vm_monitor_exit_wrapper(frame.This);
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_EXIT_EVENT)
        method_exit_callback_with_frame(method, prevFrame);

    M2N_FREE_MACRO;
    DEBUG_TRACE("native_invoke_virtual >>>\n");
    FREE_FRAME(out_args);
}
