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
 * @author Intel, Gregory Shimansky
 */  


#include "cxxlog.h"
#include "jni.h"
#include "jni_direct.h"
#include "jni_utils.h"

#include "Class.h"
#include "environment.h"
#include "open/gc.h"
#include "object_handles.h"
#include "open/vm_util.h"
#include "vm_threads.h"

#include "ini.h"
#include "exceptions.h"


jfieldID JNICALL GetFieldID(JNIEnv * jni_env,
                            jclass clazz,
                            const char *name,
                            const char *sig)
{
    TRACE2("jni", "GetFieldID called");
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Class* clss = jclass_to_struct_Class(clazz);
    Field *field = class_lookup_field_recursive(clss, name, sig);
    if (NULL == field || field->is_static())
    {
        ThrowNew_Quick(jni_env, "java/lang/NoSuchFieldError", name);
        return 0;
    }
    TRACE2("jni", "GetFieldID " << clss->get_name()->bytes
        << "." << name << " " << sig << " = " << (jfieldID)field);

    assert(!field->is_static());
    return (jfieldID)field;
} //GetFieldID



// non-standard
jfieldID JNICALL GetFieldID_Quick(JNIEnv * jni_env,
                                  const char *class_name,
                                  const char *field_name,
                                  const char *sig)
{
    assert(hythread_is_suspend_enabled());

    Global_Env * vm_env = jni_get_vm_env(jni_env);
    if (exn_raised()) return 0;

    String *class_string = vm_env->string_pool.lookup(class_name);
    assert(hythread_is_suspend_enabled());
    Class *clss =
        class_load_verify_prepare_from_jni(vm_env, class_string);
    if(!clss) {
        return 0;
    }
    Field *field = class_lookup_field_recursive(clss, field_name, sig);
    return (jfieldID)field;
} //GetFieldID_Quick

jfieldID JNICALL GetStaticFieldID(JNIEnv * jni_env,
                                  jclass clazz,
                                  const char *name,
                                  const char *sig)
{
    TRACE2("jni", "GetStaticFieldID called");
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Class* clss = jclass_to_struct_Class(clazz);
    Field *field = class_lookup_field_recursive(clss, name, sig);
    if(NULL == field || !field->is_static())
    {
        ThrowNew_Quick(jni_env, "java/lang/NoSuchFieldError", name);
        return 0;
    }
    TRACE2("jni", "GetStaticFieldID " << clss->get_name()->bytes
        << "." << name << " " << sig << " = " << (jfieldID)field);

    assert(field->is_static());
    return (jfieldID)field;
} //GetStaticFieldID

/////////////////////////////////////////////////////////////////////////////
// begin Get<Type>Field functions


jobject JNICALL GetObjectFieldOffset(JNIEnv* UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return NULL;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*) h->object;
    ManagedObject** field_addr = (ManagedObject**)(java_ref + offset);
    ManagedObject* val = get_raw_reference_pointer(field_addr);
    ObjectHandle new_handle = NULL; 
    if (val != NULL) {
        new_handle = oh_allocate_local_handle_from_jni();
        if (new_handle == NULL) {
            tmn_suspend_enable();   //---------------------------------^
            return NULL;
        }
       new_handle->object = val;
    } 

    tmn_suspend_enable();        //---------------------------------^

    return (jobject)new_handle;
}

jobject JNICALL GetObjectField(JNIEnv * jni_env,
                               jobject obj,
                               jfieldID fieldID)
{
    TRACE2("jni", "GetObjectField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return NULL;
    assert(!f->is_static());
    return GetObjectFieldOffset(jni_env, obj, f->get_offset());
} //GetObjectField


jboolean JNICALL GetBooleanFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jboolean val = *(jboolean *)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jboolean JNICALL GetBooleanField(JNIEnv *jni_env,
                                 jobject obj,
                                 jfieldID fieldID)
{
    TRACE2("jni", "GetBooleanField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetBooleanFieldOffset(jni_env, obj, f->get_offset());
} //GetBooleanField


jbyte JNICALL GetByteFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jbyte val = *(jbyte *)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jbyte JNICALL GetByteField(JNIEnv * jni_env,
                           jobject obj,
                           jfieldID fieldID)
{
    TRACE2("jni", "GetByteField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());
    
    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetByteFieldOffset(jni_env, obj, f->get_offset());
} //GetByteField


jchar JNICALL GetCharFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();     //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jchar val = *(jchar *)(java_ref + offset);

    tmn_suspend_enable();                        //---------------------------------^

    return val;
}

jchar JNICALL GetCharField(JNIEnv * jni_env,
                           jobject obj,
                           jfieldID fieldID)
{
    TRACE2("jni", "GetCharField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetCharFieldOffset(jni_env, obj, f->get_offset());
} //GetCharField


jshort JNICALL GetShortFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jshort val = *(jshort *)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jshort JNICALL GetShortField(JNIEnv * jni_env,
                             jobject obj,
                             jfieldID fieldID)
{
    TRACE2("jni", "GetShortField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetShortFieldOffset(jni_env, obj, f->get_offset());
} //GetShortField


jint JNICALL GetIntFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jint val = *(jint *)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jint JNICALL GetIntField(JNIEnv * jni_env,
                         jobject obj,
                         jfieldID fieldID)
{
    TRACE2("jni", "GetIntField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetIntFieldOffset(jni_env, obj, f->get_offset());
} //GetIntField


jlong JNICALL GetLongFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jlong val = *(jlong*)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jlong JNICALL GetLongField(JNIEnv * jni_env,
                           jobject obj,
                           jfieldID fieldID)
{
    TRACE2("jni", "GetLongField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetLongFieldOffset(jni_env, obj, f->get_offset());
} //GetLongField


jfloat JNICALL GetFloatFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jfloat val = *(jfloat*)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jfloat JNICALL GetFloatField(JNIEnv * jni_env,
                             jobject obj,
                             jfieldID fieldID)
{
    TRACE2("jni", "GetFloatField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetFloatFieldOffset(jni_env, obj, f->get_offset());
} //GetFloatField


jdouble JNICALL GetDoubleFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return 0;
    
    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    jdouble val = *(jdouble*)(java_ref + offset);

    tmn_suspend_enable();        //---------------------------------^

    return val;
}

jdouble JNICALL GetDoubleField(JNIEnv * jni_env,
                               jobject obj,
                               jfieldID fieldID)
{
    TRACE2("jni", "GetDoubleField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(!f->is_static());
    return GetDoubleFieldOffset(jni_env, obj, f->get_offset());
} //GetDoubleField



// end Get<Type>Field functions
/////////////////////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////////////////////
// begin Set<Type>Field functions


void JNICALL SetObjectFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jobject value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;
    ObjectHandle v = (ObjectHandle)value;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    ManagedObject **field_addr = (ManagedObject **)(java_ref + offset);

    ManagedObject *val;
    if (v != NULL) {
        val = v->object;
    } else {
        // A null object handle, so a null reference.
        val = NULL;
    }
    STORE_REFERENCE((ManagedObject *)java_ref, field_addr, val);

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetObjectField(JNIEnv * jni_env,
                            jobject obj,
                            jfieldID fieldID,
                            jobject value)
{
    TRACE2("jni", "SetObjectField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetObjectFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetObjectField


void JNICALL SetBooleanFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jboolean value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    *(jboolean*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetBooleanField(JNIEnv * jni_env,
                             jobject obj,
                             jfieldID fieldID,
                             jboolean value)
{
    TRACE2("jni", "SetBooleanField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetBooleanFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetBooleanField


void JNICALL SetByteFieldOffset(JNIEnv * jni_env, jobject obj, jint offset, jbyte value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    Global_Env * vm_env = jni_get_vm_env(jni_env);
    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    if(vm_env->compact_fields)
        *(jbyte*)(java_ref + offset) = value;
    else
        *(jint*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetByteField(JNIEnv * jni_env,
                          jobject obj,
                          jfieldID fieldID,
                          jbyte value)
{
    TRACE2("jni", "SetByteField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetByteFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetByteField


void JNICALL SetCharFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jchar value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    *(jchar*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetCharField(JNIEnv * jni_env,
                          jobject obj,
                          jfieldID fieldID,
                          jchar value)
{
    TRACE2("jni", "SetCharField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetCharFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetCharField


void JNICALL SetShortFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jshort value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    Global_Env * vm_env = jni_get_vm_env(jni_env);
    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    if (vm_env->compact_fields)
        *(jshort*)(java_ref + offset) = value;
    else
        *(jint*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetShortField(JNIEnv * jni_env,
                           jobject obj,
                           jfieldID fieldID,
                           jshort value)
{
    TRACE2("jni", "SetShortField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetShortFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetShortField


void JNICALL SetIntFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jint value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    *(jint*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetIntField(JNIEnv * jni_env,
                         jobject obj,
                         jfieldID fieldID,
                         jint value)
{
    TRACE2("jni", "SetIntField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetIntFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetIntField


void JNICALL SetLongFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jlong value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    *(jlong*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetLongField(JNIEnv * jni_env,
                          jobject obj,
                          jfieldID fieldID,
                          jlong value)
{
    TRACE2("jni", "SetLongField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetLongFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetLongField


void JNICALL SetFloatFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jfloat value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    *(jfloat*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetFloatField(JNIEnv * jni_env,
                           jobject obj,
                           jfieldID fieldID,
                           jfloat value)
{
    TRACE2("jni", "SetFloatField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetFloatFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetFloatField


void JNICALL SetDoubleFieldOffset(JNIEnv * UNREF jni_env, jobject obj, jint offset, jdouble value)
{
    assert(hythread_is_suspend_enabled());
    ObjectHandle h = (ObjectHandle)obj;

    if (exn_raised()) return;

    tmn_suspend_disable();       //---------------------------------v

    U_8* java_ref = (U_8*)h->object;
    *(jdouble*)(java_ref + offset) = value;

    tmn_suspend_enable();        //---------------------------------^
}

void JNICALL SetDoubleField(JNIEnv * jni_env,
                            jobject obj,
                            jfieldID fieldID,
                            jdouble value)
{
    TRACE2("jni", "SetDoubleField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Field *f = (Field *)fieldID;
    assert(f);
    assert(IsInstanceOf(jni_env, obj, struct_Class_to_jclass(f->get_class())));

    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(!f->is_static());
    SetDoubleFieldOffset(jni_env, obj, f->get_offset(), value);
} //SetDoubleField



// end Set<Type>Field functions
/////////////////////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////////////////////
// begin GetStatic<Type>Field functions

#ifdef LAZY_CP_STRING_INSTANTIATION
#include "jit_runtime_support_common.h"
#endif

jobject JNICALL GetStaticObjectField(JNIEnv * jni_env,
                                     jclass UNREF clazz,
                                     jfieldID fieldID)
{
    TRACE2("jni", "GetStaticObjectField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return NULL;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return NULL;
    assert(f->is_static());
    tmn_suspend_disable();       //---------------------------------v
    ManagedObject **field_addr = (ManagedObject **)f->get_address();

    ObjectHandle new_handle;
    // compress static fields.
    ManagedObject *val = get_raw_reference_pointer(field_addr);
    if (val != NULL) {
        new_handle = oh_allocate_local_handle_from_jni();
        if (new_handle != NULL) {
            new_handle->object = val;
        }
    } else {
        new_handle = NULL;
    }

    tmn_suspend_enable();        //---------------------------------^

    return (jobject)new_handle;
} //GetStaticObjectField

jboolean JNICALL GetStaticBooleanField(JNIEnv * jni_env,
                                       jclass UNREF clazz,
                                       jfieldID fieldID)
{
    TRACE2("jni", "GetStaticBooleanField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jboolean *field_addr = (jboolean *)f->get_address();
    return *field_addr;
} //GetStaticBooleanField

jbyte JNICALL GetStaticByteField(JNIEnv * jni_env,
                                 jclass UNREF clazz,
                                 jfieldID fieldID)
{
    TRACE2("jni", "GetStaticByteField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jbyte *field_addr = (jbyte *)f->get_address();
    return *field_addr;
} //GetStaticByteField

jchar JNICALL GetStaticCharField(JNIEnv * jni_env,
                                 jclass UNREF clazz,
                                 jfieldID fieldID)
{
    TRACE2("jni", "GetStaticCharField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jchar *field_addr = (jchar *)f->get_address();
    return *field_addr;
} //GetStaticCharField

jshort JNICALL GetStaticShortField(JNIEnv * jni_env,
                                   jclass UNREF clazz,
                                   jfieldID fieldID)
{
    TRACE2("jni", "GetStaticShortField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jshort *field_addr = (jshort *)f->get_address();
    return *field_addr;
} //GetStaticShortField

jint JNICALL GetStaticIntField(JNIEnv * jni_env,
                               jclass UNREF clazz,
                               jfieldID fieldID)
{
    TRACE2("jni", "GetStaticIntField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jint *field_addr = (jint *)f->get_address();
    return *field_addr;
} //GetStaticIntField

jlong JNICALL GetStaticLongField(JNIEnv * jni_env,
                                 jclass UNREF clazz,
                                 jfieldID fieldID)
{
    TRACE2("jni", "GetStaticLongField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jlong *field_addr = (jlong *)f->get_address();
    return *field_addr;
} //GetStaticLongField

jfloat JNICALL GetStaticFloatField(JNIEnv * jni_env,
                                  jclass UNREF clazz,
                                  jfieldID fieldID)
{
    TRACE2("jni", "GetStaticFloatField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jfloat *field_addr = (jfloat *)f->get_address();
    return *field_addr;
} //GetStaticFloatField

jdouble JNICALL GetStaticDoubleField(JNIEnv * jni_env,
                                     jclass UNREF clazz,
                                     jfieldID fieldID)
{
    TRACE2("jni", "GetStaticDoubleField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return 0;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return 0;
    assert(f->is_static());
    jdouble *field_addr = (jdouble *)f->get_address();
    return *field_addr;
} //GetStaticDoubleField



// end GetStatic<Type>Field functions
/////////////////////////////////////////////////////////////////////////////




/////////////////////////////////////////////////////////////////////////////
// begin SetStatic<Type>Field functions


void JNICALL SetStaticObjectField(JNIEnv * jni_env,
                                  jclass UNREF clazz,
                                  jfieldID fieldID,
                                  jobject value)
{
    TRACE2("jni", "SetStaticObjectField called, id = " << fieldID);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    tmn_suspend_disable();       //---------------------------------v
    void* field_addr = f->get_address();
    ObjectHandle v = (ObjectHandle)value;

    ManagedObject *val = NULL;
    if (v != NULL) {
        val = v->object;
    }
    // compress static fields.
    STORE_GLOBAL_REFERENCE(field_addr, val);

    tmn_suspend_enable();        //---------------------------------^
} //SetStaticObjectField


void JNICALL SetStaticBooleanField(JNIEnv * jni_env,
                                   jclass UNREF clazz,
                                   jfieldID fieldID,
                                   jboolean value)
{
    TRACE2("jni", "SetStaticBooleanField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jboolean *field_addr = (jboolean *)f->get_address();
    *field_addr = value;
} //SetStaticBooleanField


void JNICALL SetStaticByteField(JNIEnv * jni_env,
                                jclass UNREF clazz,
                                jfieldID fieldID,
                                jbyte value)
{
    TRACE2("jni", "SetStaticByteField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Global_Env * vm_env = jni_get_vm_env(jni_env);
    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jbyte *field_addr = (jbyte *)f->get_address();
    jint *field_addr_int = (jint *)f->get_address();
    if (vm_env->compact_fields)
        *field_addr = value;
    else
        *field_addr_int = value;
} //SetStaticByteField


void JNICALL SetStaticCharField(JNIEnv * jni_env,
                                jclass UNREF clazz,
                                jfieldID fieldID,
                                jchar value)
{
    TRACE2("jni", "SetStaticCharField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jchar *field_addr = (jchar *)f->get_address();
    *field_addr = value;
} //SetStaticCharField


void JNICALL SetStaticShortField(JNIEnv * jni_env,
                                 jclass UNREF clazz,
                                 jfieldID fieldID,
                                 jshort value)
{
    TRACE2("jni", "SetStaticShortField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    Global_Env * vm_env = jni_get_vm_env(jni_env);
    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jshort *field_addr = (jshort *)f->get_address();
    jint *field_addr_int = (jint *)f->get_address();
    if (vm_env->compact_fields)
        *field_addr = value;
    else
        *field_addr_int = value;
} //SetStaticShortField


void JNICALL SetStaticIntField(JNIEnv * jni_env,
                               jclass UNREF clazz,
                               jfieldID fieldID,
                               jint value)
{
    TRACE2("jni", "SetStaticIntField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jint *field_addr = (jint *)f->get_address();
    *field_addr = value;
} //SetStaticIntField



void JNICALL SetStaticLongField(JNIEnv * jni_env,
                                jclass UNREF clazz,
                                jfieldID fieldID,
                                jlong value)
{
    TRACE2("jni", "SetStaticLongField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jlong *field_addr = (jlong *)f->get_address();
    *field_addr = value;
} //SetStaticLongField


void JNICALL SetStaticFloatField(JNIEnv * jni_env,
                                 jclass UNREF clazz,
                                 jfieldID fieldID,
                                 jfloat value)
{
    TRACE2("jni", "SetStaticFloatField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jfloat *field_addr = (jfloat *)f->get_address();
    *field_addr = value;
} //SetStaticFloatField


void JNICALL SetStaticDoubleField(JNIEnv * jni_env,
                                  jclass UNREF clazz,
                                  jfieldID fieldID,
                                  jdouble value)
{
    TRACE2("jni", "SetStaticDoubleField called, id = " << fieldID << " value = " << value);
    assert(hythread_is_suspend_enabled());

    if (exn_raised()) return;

    Field *f = (Field *)fieldID;
    assert(f);
    if (!ensure_initialised(jni_env, f->get_class())) return;
    assert(f->is_static());
    jdouble *field_addr = (jdouble *)f->get_address();
    *field_addr = value;
} //SetStaticDoubleField



// end SetStatic<Type>Field functions
/////////////////////////////////////////////////////////////////////////////




