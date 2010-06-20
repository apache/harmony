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
 * JVMTI internal functions
 */

#define LOG_DOMAIN "jvmti.internal"
#include "cxxlog.h"

#include "environment.h"
#include "vtable.h"
#include "jvmti_internal.h"

static Boolean is_valid_instance(jobject obj, Class* clss)
{
    if (NULL == obj)
        return false;

    tmn_suspend_disable();       //---------------------------------v
    ObjectHandle h = (ObjectHandle)obj;
    ManagedObject *mo = h->object;

    // Check that reference pointer points to the heap
    if (mo < (ManagedObject *)VM_Global_State::loader_env->heap_base ||
        mo > (ManagedObject *)VM_Global_State::loader_env->heap_end)
    {
        tmn_suspend_enable();
        return false;
    }

    // Check that object is an instance of clss or subclasss
    if (mo->vt() == NULL)
    {
        tmn_suspend_enable();
        return false;
    }

    Class* object_clss = mo->vt()->clss;
    Boolean result = object_clss->is_instanceof(clss);
    tmn_suspend_enable();        //---------------------------------^

    return result;
}

Boolean is_valid_throwable_object(jobject exc)
{
    return is_valid_instance(exc, VM_Global_State::loader_env->java_lang_Throwable_Class);
}

Boolean is_valid_thread_object(jthread thread)
{
    return is_valid_instance(thread, VM_Global_State::loader_env->java_lang_Thread_Class);
}

Boolean is_valid_thread_group_object(jthreadGroup group)
{
    return is_valid_instance(group, VM_Global_State::loader_env->java_lang_ThreadGroup_Class);
}

Boolean is_valid_class_object(jclass klass)
{
    return is_valid_instance(klass, VM_Global_State::loader_env->JavaLangClass_Class);
}
