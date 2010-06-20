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

using namespace std;

#ifdef _WIN32
static int64 __declspec(naked) __stdcall invokeJNI(U_32 *args, int sz, GenericFunctionPointer f) {
    __asm {
        push ebp
        mov ebp, esp
        push ecx
        mov eax, [ebp+8]
        mov ecx, [ebp+12]
        lea eax, [eax+ecx*4-4]
        sub eax, esp
        l:
        push [esp+eax]
        loop l
        mov ecx, [ebp-4]
        mov eax, [ebp+16]
        call eax
        leave
        ret
    }
}
#else /* Linux */
extern "C" {
    int64 invokeJNI(U_32 *args, int sz, GenericFunctionPointer f);
}

#endif

typedef double (*DoubleFuncPtr)(uword*,int,GenericFunctionPointer);
typedef ManagedObject** (*RefFuncPtr)(uword*,int,GenericFunctionPointer);
typedef void* (*ObjFuncPtr)(uword*,int,GenericFunctionPointer);
typedef float (*FloatFuncPtr)(uword*,int,GenericFunctionPointer);
typedef I_32 (*IntFuncPtr)(uword*,int,GenericFunctionPointer);
typedef int16 (*ShortFuncPtr)(uword*,int,GenericFunctionPointer);
typedef I_8 (*ByteFuncPtr)(uword*,int,GenericFunctionPointer);
typedef uint16 (*CharFuncPtr)(uword*,int,GenericFunctionPointer);

DoubleFuncPtr invokeJNI_Double = (DoubleFuncPtr) invokeJNI;
RefFuncPtr invokeJNI_Ref = (RefFuncPtr) invokeJNI;
ObjFuncPtr invokeJNI_Obj = (ObjFuncPtr) invokeJNI;
IntFuncPtr invokeJNI_Int = (IntFuncPtr) invokeJNI;
FloatFuncPtr invokeJNI_Float = (FloatFuncPtr) invokeJNI;
ShortFuncPtr invokeJNI_Short = (ShortFuncPtr) invokeJNI;
CharFuncPtr invokeJNI_Char = (CharFuncPtr) invokeJNI;
ByteFuncPtr invokeJNI_Byte = (ByteFuncPtr) invokeJNI;

void
interpreter_execute_native_method(
        Method *method,
        jvalue *return_value,
        jvalue *args) {
    assert(!hythread_is_suspend_enabled());

    DEBUG_TRACE("\n<<< interpreter_invoke_native: " << method);

    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> interpreter_invoke_native >>>\n");
        return;
    }

    M2N_ALLOC_MACRO;
    hythread_suspend_enable();
    
    int sz = method->get_num_arg_slots();
    uword *arg_words = (uword*) ALLOC_FRAME((sz + 2) * sizeof(uword));

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
            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                // zero extend
                arg_words[argId++] = (word) args[pos++].i;
                break;

            case JAVA_TYPE_LONG:
            case JAVA_TYPE_DOUBLE:
                Value2 val;
                val.i64 = args[pos++].j;
                arg_words[argId++] = val.v[0].i;
                arg_words[argId++] = val.v[1].i;
                break;
            default:
                DIE(("Invalid java type"));
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


    jvalue *resultPtr = return_value;
    Java_Type ret_type = method->get_return_java_type();

    switch(ret_type) {
        case JAVA_TYPE_VOID:
            invokeJNI(arg_words, argId, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                jobject obj = (jobject) invokeJNI_Obj(arg_words, argId, f);
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
            resultPtr->i = invokeJNI_Int(arg_words, argId, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_FLOAT:
            resultPtr->f = invokeJNI_Float(arg_words, argId, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_LONG:
            resultPtr->j = invokeJNI(arg_words, argId, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        case JAVA_TYPE_DOUBLE:
            resultPtr->d = invokeJNI_Double(arg_words, argId, f);
            hythread_suspend_disable();
            M2N_FREE_MACRO;
            break;

        default:
            DIE(("Invalid java type"));
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
    FREE_FRAME(arg_words);
}

void
interpreterInvokeStaticNative(StackFrame& prevFrame, StackFrame& frame, Method *method) {

    
    GenericFunctionPointer f = interpreterGetNativeMethodAddr(method);
    if (f == 0) {
        DEBUG_TRACE("<EXCEPTION> native_invoke_static >>>\n");
        return;
    }

    DEBUG_TRACE("\n<<< native_invoke_static     : " << method);
    TRACE("interpreter static native: " << frame.method);

    M2N_ALLOC_MACRO;
    
    frame.This = *(method->get_class()->get_class_handle());
    int sz = method->get_num_arg_slots();
    uword *args = (uword*) ALLOC_FRAME((sz + 2) * sizeof(uword));
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
                    ASSERT_OBJECT(ref);
                    if (ref == 0) {
                        args[argId++] = 0;
                    } else {
                        args[argId++] = (uword) &ref;
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
            case JAVA_TYPE_BOOLEAN:
            case JAVA_TYPE_CHAR:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                // zero extend
                args[argId++] = prevFrame.stack.pick(pos--).u;
                break;

            case JAVA_TYPE_LONG:
            case JAVA_TYPE_DOUBLE:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                args[argId+0] = prevFrame.stack.pick(pos-0).u;
                args[argId+1] = prevFrame.stack.pick(pos-1).u;
                argId += 2;
                pos -= 2;
                break;
            default:
                DIE(("Invalid java type"));
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

    hythread_suspend_enable();

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            {
                invokeJNI(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);
            }
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject **ref = invokeJNI_Ref(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                if (ref != 0) {
                    ASSERT_OBJECT(*ref);
                    if (!*ref) {
                        INFO(
                        "VM WARNING: Reference with null value returned from jni function:\n"
                        "VM WARNING: Method name: " << method <<
                        "\nVM WARNING: Not allowed, return NULL (0) instead\n");
                    }
                    prevFrame.stack.pick().ref = *ref;
                } else {
                    prevFrame.stack.pick().ref = 0;
                }
                prevFrame.stack.ref() = FLAG_OBJECT;
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
            {
                I_8 res = invokeJNI_Byte(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32)res;
            }
            break;

        case JAVA_TYPE_CHAR:
            {
                uint16 res = invokeJNI_Char(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().u = (U_32) res;
            }
            break;

        case JAVA_TYPE_SHORT:
            {
                int16 res = invokeJNI_Short(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_INT:
            {
                Value res;
                res.i = invokeJNI_Int(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_FLOAT:
            {
                Value res;
                res.f = invokeJNI_Float(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_LONG:
            {
                Value2 res;
                res.i64 = invokeJNI(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        case JAVA_TYPE_DOUBLE:
            {
                Value2 res;
                res.d = invokeJNI_Double(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        default:
            DIE(("Invalid java type"));
    }

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
                    ASSERT_OBJECT(ref);
                    if (ref == 0) {
                        args[argId++] = 0;
                    } else {
                        args[argId++] = (uword) &ref;
                    }
                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_FLOAT:
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
            case JAVA_TYPE_DOUBLE:
                ASSERT_TAGS(!prevFrame.stack.ref(pos));
                ASSERT_TAGS(!prevFrame.stack.ref(pos-1));
                args[argId+0] = prevFrame.stack.pick(pos-0).u;
                args[argId+1] = prevFrame.stack.pick(pos-1).u;
                argId += 2;
                pos -= 2;
                break;
            default:
                DIE(("Invalid java type"));
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
    
    hythread_suspend_enable();

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            {
                invokeJNI(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);
            }
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject ** ref = invokeJNI_Ref(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                if (ref != 0) {
                    ASSERT_OBJECT(*ref);

                    if (!*ref) {
                        INFO(
                        "VM WARNING: Reference with null value returned from jni function:\n"
                        "VM WARNING: Method name: " << method << 
                        "\nVM WARNING: Not allowed, return NULL (0) instead\n");
                    }
                    prevFrame.stack.pick().ref = *ref;
                } else {
                    prevFrame.stack.pick().ref = 0;
                }
                prevFrame.stack.ref() = FLAG_OBJECT;
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
            {
                I_8 res = invokeJNI_Byte(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_CHAR:
            {
                uint16 res = invokeJNI_Char(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().u = (U_32) res;
            }
            break;

        case JAVA_TYPE_SHORT:
            {
                int16 res = invokeJNI_Short(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick().i = (I_32) res;
            }
            break;

        case JAVA_TYPE_INT:
            {
                Value res;
                res.i = invokeJNI_Int(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_FLOAT:
            {
                Value res;
                res.f = invokeJNI_Float(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push();
                prevFrame.stack.pick() = res;
            }
            break;

        case JAVA_TYPE_LONG:
            {
                Value2 res;
                res.i64 = invokeJNI(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        case JAVA_TYPE_DOUBLE:
            {
                Value2 res;
                res.d = invokeJNI_Double(args, argId, f);
                hythread_suspend_disable();
                prevFrame.stack.popClearRef(sz);

                prevFrame.stack.push(2);
                prevFrame.stack.setLong(0, res);
            }
            break;

        default:
            DIE(("Invalid java type"));
    }

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
