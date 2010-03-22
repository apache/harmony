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
 * @author Intel, Pavel Afremov
 */

#define LOG_DOMAIN "vm.object_queue"
#include "cxxlog.h"

#include "open/types.h"

#include "lock_manager.h"
#include "object_layout.h"
#include "jthread.h"

#include "vm_process.h"
#include "Class.h"
#include "environment.h"
#include "ini.h"
#include "m2n.h"
#include "exceptions.h"
#include "compile.h"
#include "nogc.h"
#include "jit_runtime_support.h"
#include "finalize.h"
#include "vtable.h"
#include "jit_import_rt.h"
#include "finalizer_thread.h"     /* added for NATIVE FINALIZER THREAD */
#include "ref_enqueue_thread.h"   /* added for NATIVE REFERENCE ENQUEUE THREAD */
#include "classloader.h"
#include "thread_generic.h"

#ifndef USE_GC_STATIC
__declspec(dllexport) 
#endif

// FINALIZER_THREAD means that thread is finalizer thread
#define FINALIZER_THREAD    0x1

// FINALIZER_STARTER means that thread is running inside run_finalizers function
#define FINALIZER_STARTER   0x2

//
// This code holds the logic that deals with finalization as well as weak/soft/phantom 
// references. Finalization runs arbitrary code that can include synchronization logic.
//
// There are two different times that finalizers and enqueues can be run.
//
// The first is to have a separate thread that receives the objects that need to be
// finalized and executes them sometime after the GC has completed. This is probable
// what was intended by the designers of the finalization and weak/soft/phantom 
// reference features of the language. 
//
// The second approach that can be taken is to run the finalization or enqueues code  
// immediately after the gc is complete prior to resuming the thread that caused the gc
// to happen.
//
// The first scheme was implemented for finalization and second is used for 
// weak/soft/phantom references.
//

hylatch_t begin_run_finalizer;

class Object_Queue
{
    ManagedObject **objects;
    unsigned capacity;
    unsigned num_objects;

    Lock_Manager objects_lock;
    const char*  log_domain;

    void reallocate(unsigned new_capacity);
    
public:
    Object_Queue();
    Object_Queue(const char* log_domain);
    ~Object_Queue(){
        STD_FREE(objects);
    }
    void add_object(ManagedObject *p_obj);
    ManagedObject* remove_object();
    int getLength();
    
    void enumerate_for_gc();
}; //Object_Queue

class Objects_To_Finalize: public Object_Queue
{
    // workaround part to ignore classes java.nio.charset.CharsetEncoder
    // java.io.FileDescriptor & java.io.FileOutputStream during finalization on exit
    bool is_class_ignored(Class* test);
    bool classes_cached;
    Lock_Manager ignore_lock;
    Class* FileDescriptor;
    Class* FileOutputStream;

    //jobject get_work_lock();
    //jboolean* get_work_lock();
    //jboolean* get_work_lock();
    bool fields_obtained;
    void check_fields_obtained();
    Lock_Manager obtain_fields_lock;
    jobject work_lock;
    jboolean* shutdown;
    jboolean* on_exit;
public:
    Objects_To_Finalize() : Object_Queue("finalize") {
        classes_cached = false;
        fields_obtained = false;
        work_lock = NULL;
        on_exit = NULL;
    };
    // redefine of add method 
    void add_object(ManagedObject *p_obj);
    
    void run_finalizers();
    int do_finalization(int quantity);
    void obtain_fields();
}; //Objects_To_Finalize

class References_To_Enqueue: public Object_Queue
{
public:
    References_To_Enqueue() : Object_Queue("ref") {};
    void enqueue_references();
}; //References_To_Enqueue



Object_Queue::Object_Queue() {
    Object_Queue::Object_Queue("unknown");
}

Object_Queue::Object_Queue(const char* log_domain)
{
    objects     = 0;
    capacity    = 0;
    num_objects = 0;
    this-> log_domain =  log_domain;
    reallocate(128);
} //Object_Queue::Object_Queue


/**
 * Allocates array to save objects. Should be called from synchronized block.
 * Now it's called from add_object only.
 */
 
void Object_Queue::reallocate(unsigned new_capacity)
{
    ManagedObject **new_table =
        (ManagedObject **)STD_MALLOC(new_capacity * sizeof(ManagedObject *));
        
    // print trace info about queue enlarge
    TRACE2( log_domain, "The " <<  log_domain << " queue capacity is enlarged to " 
            << new_capacity << " units.");
        
    // asserts that queue information is correct 
    assert(new_table);
    assert(num_objects <= capacity);
    
    // if queue already contains objects, copies it to new array and frees memory for previous one.
    if(objects) {
        memcpy(new_table, objects, num_objects * sizeof(ManagedObject *));
        STD_FREE(objects);
    }
    
    // saves new queue information
    objects = new_table;
    capacity = new_capacity;
} //Object_Queue::reallocate


/**
 * Adds object to queue
 */
void Object_Queue::add_object(ManagedObject *p_obj)
{
    objects_lock._lock();
    
    // reallocates if there is not enough place to save new one
    if(num_objects >= capacity) {
        reallocate(capacity * 2);
    }
    
    // asserts that queue information is correct 
    assert(num_objects < capacity);

    // adds object to queue
    objects[num_objects++] = p_obj;
    objects_lock._unlock();
} //Object_Queue::add_object

/**
 * Removes latest object from queue and returns it as result.
 */
ManagedObject*
Object_Queue::remove_object() {
    ManagedObject* removed_object;
    objects_lock._lock();

    // if there is any objects in queue, remove the latest from there to return as result
    if (0 < num_objects) {
        removed_object = this->objects[--num_objects];
    } else {
        removed_object = NULL;
    }
    objects_lock._unlock();
    return removed_object;
} //Object_Queue::remove_object

/**
 * Returns length of queue
 */
int Object_Queue::getLength() {
   unsigned result;
   
   // synchronization used there to avoid of return very old value
   objects_lock._lock();
   result = this->num_objects;
   objects_lock._unlock();
   
   // there unfresh value can be returned, but this value was correct after function start 
   return result;
}

/**
 * Enumerates queue for GC
 */
void Object_Queue::enumerate_for_gc()
{
    // locks here because some native code can work during gc until tmn_suspend_disable() call
    objects_lock._lock();

    // print trace info about queue enlarge
    TRACE2( log_domain, "The " <<  log_domain << " queue length is " << num_objects << " units");

    // enumerate elements in the queue
    for(unsigned i = 0; i < num_objects; i++) {
        vm_enumerate_root_reference((void **)&(objects[i]), FALSE);
    }
    
    // unlock 
    objects_lock._unlock();
} //Object_Queue::enumerate_for_gc

void Objects_To_Finalize::add_object(ManagedObject *p_obj)
{
    Class* finalizer_thread =
        VM_Global_State::loader_env->java_lang_FinalizerThread_Class;

    if (!finalizer_thread) {
        return;
    } else {
        Object_Queue::add_object(p_obj);
    }
} //Objects_To_Finalize::add_object

// workaround method to ignore classes
// java.io.FileDescriptor & java.io.FileOutputStream during finalization on exit
bool Objects_To_Finalize::is_class_ignored(Class* test) {
    assert(!hythread_is_suspend_enabled());
    
    if (!classes_cached) {
    
        tmn_suspend_enable();
        
        String* FileDescriptorName =
                VM_Global_State::loader_env->string_pool.lookup("java/io/FileDescriptor");
        String* FileOutputStreamName =
                VM_Global_State::loader_env->string_pool.lookup("java/io/FileOutputStream");
        
        Class* FileDescriptor = 
            VM_Global_State::loader_env->bootstrap_class_loader->LoadVerifyAndPrepareClass(
                VM_Global_State::loader_env, FileDescriptorName);
        Class* FileOutputStream = 
            VM_Global_State::loader_env->bootstrap_class_loader->LoadVerifyAndPrepareClass(
                VM_Global_State::loader_env, FileOutputStreamName);
        
        tmn_suspend_disable();
        ignore_lock._lock();
        this->FileDescriptor = FileDescriptor;
        this->FileOutputStream = FileOutputStream;
        classes_cached = true;
        ignore_lock._unlock();
    }
    return ((test==FileDescriptor) || (test==FileOutputStream));
}

void Objects_To_Finalize::check_fields_obtained() {
    if (!fields_obtained) {
        obtain_fields();
    }
}

void Objects_To_Finalize::obtain_fields() {
    obtain_fields_lock._lock();

    if (!fields_obtained) {
        Class* finalizer_thread =
            VM_Global_State::loader_env->java_lang_FinalizerThread_Class;

        if (!finalizer_thread) {
            obtain_fields_lock._unlock();
            return;
        }

        Field* work_lock_field = class_lookup_field_recursive(
                finalizer_thread,
                "workLock", "Ljava/lang/Object;");
        Field* shutdown_field = class_lookup_field_recursive(
                finalizer_thread,
                "shutdown", "Z");
        Field* on_exit_field = class_lookup_field_recursive(
                finalizer_thread,
                "onExit", "Z");

        assert(work_lock_field);
        assert(shutdown_field);
        assert(on_exit_field);

        tmn_suspend_disable();
        ManagedObject* work_lock_addr = get_raw_reference_pointer(
            (ManagedObject **)work_lock_field->get_address());
        assert(work_lock_addr);
        work_lock = oh_allocate_global_handle();
        work_lock->object = work_lock_addr;
        assert(work_lock);
        tmn_suspend_enable();

        shutdown = (jboolean*) shutdown_field->get_address();
        on_exit = (jboolean*) on_exit_field->get_address();
        assert(shutdown);
        assert(on_exit);

        fields_obtained = true;
    }
    obtain_fields_lock._unlock();
}

void Objects_To_Finalize::run_finalizers()
{
    assert(hythread_is_suspend_enabled());

    Class* finalizer_thread =
        VM_Global_State::loader_env->java_lang_FinalizerThread_Class;

    if (!finalizer_thread) {
        return;
    }

    check_fields_obtained();

    int num_objects = getLength() + vm_get_references_quantity();

    if (num_objects == 0) {
        return;
    }

    if ((p_TLS_vmthread->finalize_thread_flags & (FINALIZER_STARTER | FINALIZER_THREAD)) != 0) {
        TRACE2("finalize", "recursive finalization prevented");
        return;
    }

    p_TLS_vmthread->finalize_thread_flags |= FINALIZER_STARTER;
    TRACE2("finalize", "run_finalizers() started");

    // saves curent thread exception risen before running finalizers
    // if any and clears it to allow java to work
    jthrowable cte = exn_get();
    exn_clear();
    
    if (FRAME_COMPILATION ==
            (FRAME_COMPILATION & m2n_get_frame_type(m2n_get_last_frame()))) {
        assert(work_lock);

        IDATA r = jthread_monitor_try_enter(work_lock);

        if (r == TM_ERROR_NONE) {
            jthread_monitor_notify_all(work_lock);
            jthread_monitor_exit(work_lock);
        }
    } else {
        Method* finalize_meth = class_lookup_method_recursive(finalizer_thread,
            "startFinalization", "(Z)V");
        assert(finalize_meth);

        jvalue args[1];
        args[0].z = false;

        tmn_suspend_disable();
        vm_execute_java_method_array((jmethodID) finalize_meth, 0, args);
        tmn_suspend_enable();
    }

#ifndef NDEBUG
    if (exn_raised()) {
        INFO2("finalize", "Uncaught exception "
            << exn_get_name()
            << " while running a wakeFinalization in FinalizerThread");        
    }
#endif
    exn_clear();

    // restores curent thread exception risen before if any
    if (NULL != cte) {
        exn_raise_object(cte);
    }
    p_TLS_vmthread->finalize_thread_flags &= ~FINALIZER_STARTER;
} //Objects_To_Finalize::run_finalizers

int Objects_To_Finalize::do_finalization(int quantity) {
    /* BEGIN: added for NATIVE FINALIZER THREAD */
    Boolean native_finalizer_thread_flag = get_native_finalizer_thread_flag();
    Boolean native_finalizer_shutdown, native_finalizer_on_exit;
    /* END: added for NATIVE FINALIZER THREAD */
    
    //SetThreadPriority(GetCurrentThread(),THREAD_PRIORITY_HIGHEST);
    if(!native_finalizer_thread_flag)    // added for NATIVE FINALIZER THREAD
        p_TLS_vmthread->finalize_thread_flags = FINALIZER_THREAD;

    int i;
    tmn_suspend_disable();
    ObjectHandle handle = oh_allocate_local_handle();
    tmn_suspend_enable();
    jvalue args[1];
    args[0].l = (jobject) handle;

    /* BEGIN: modified for NATIVE FINALIZER THREAD */
    if(!native_finalizer_thread_flag){
        assert(VM_Global_State::loader_env->java_lang_FinalizerThread_Class);
        check_fields_obtained();
        assert(shutdown);
        assert(on_exit);
        native_finalizer_shutdown = (Boolean)*shutdown;
        native_finalizer_on_exit = (Boolean)*on_exit;
    }
    /* END: modified for NATIVE FINALIZER THREAD */

    for (i=0; ((i<quantity)||(0==quantity)); i++) {
    
        // shutdown flag in FinalizerThread set after finalization on exit is completed
        /* BEGIN: modified for NATIVE FINALIZER THREAD */
        if(native_finalizer_thread_flag)
            native_finalizer_shutdown = get_finalizer_shutdown_flag();
        if(native_finalizer_shutdown)
            return i;
        /* END: modified for NATIVE FINALIZER THREAD */

        tmn_suspend_disable();
        ManagedObject* object = remove_object();
        handle->object = object;
        tmn_suspend_enable();

        if (object == NULL) {
            return i;
        }
        
        if(get_native_finalizer_thread_flag()){
            int finalizable_obj_num = getLength();
            sched_heavy_finalizer_in_finalization(finalizable_obj_num, i);
        }
        
        tmn_suspend_disable();
        assert(handle->object->vt()->clss);
        Class *clss = handle->object->vt()->clss;
        assert(clss);
        
        /* BEGIN: modified for NATIVE FINALIZER THREAD */
        if(native_finalizer_thread_flag) {
            native_finalizer_on_exit = get_finalizer_on_exit_flag();
            if(native_finalizer_on_exit  && is_class_ignored(clss)) {
                tmn_suspend_enable();
                continue;
            }
        }
        /* END: modified for NATIVE FINALIZER THREAD */
        if ((*on_exit)  && is_class_ignored(clss)) {
            tmn_suspend_enable();
            continue;
        }
        
        Method *finalize = class_lookup_method_recursive(clss,
            VM_Global_State::loader_env->FinalizeName_String,
            VM_Global_State::loader_env->VoidVoidDescriptor_String);

        assert(finalize);
        TRACE2("finalize", "finalize object " << handle->object->vt()->clss);
        vm_execute_java_method_array( (jmethodID) finalize, 0, args);
        tmn_suspend_enable();

#ifndef NDEBUG
        if (exn_raised()) {
            tmn_suspend_disable();
            assert(handle->object->vt()->clss);
            INFO2("finalize", "Uncaught exception "
                << exn_get_name()
                << " while running a finalize of the object"
                << object->vt()->clss << ".");
            tmn_suspend_enable();            
        }
#endif
        exn_clear();
    }
    return i;
} //Objects_To_Finalize::do_finalization

void References_To_Enqueue::enqueue_references()
{
    TRACE2("ref", "enqueue_references() started");

    jvalue args[1], r;

    tmn_suspend_disable();
    ObjectHandle handle = oh_allocate_local_handle();
    tmn_suspend_enable();

    args[0].l = (jobject) handle;

    // saves curent thread exception risen before enqueuing references
    // if any and clears it to allow java to work
    jthrowable cte = exn_get();
    exn_clear();

    while(true) {
        tmn_suspend_disable();
        ManagedObject* object = remove_object();
        handle->object = object;
        tmn_suspend_enable();

        if (object == NULL) {
            // restores curent thread exception risen before if any
            if (NULL != cte) {
                exn_raise_object(cte);
            }
            return;
        }
        tmn_suspend_disable();
        assert(handle->object->vt()->clss);
        Class *clss = handle->object->vt()->clss;
        TRACE2("ref","Enqueueing reference " << (handle->object));
        Method *enqueue = class_lookup_method_recursive(clss,
            VM_Global_State::loader_env->EnqueueName_String,
            VM_Global_State::loader_env->VoidBooleanDescriptor_String);
        assert(enqueue);
        vm_execute_java_method_array( (jmethodID) enqueue, &r, args);
        tmn_suspend_enable();

#ifndef NDEBUG
        if (exn_raised()) {
            tmn_suspend_disable();
            assert(object->vt()->clss);
            INFO2("ref", "Uncaught exception "
                << exn_get_name()
                << " while running a enqueue method of the object"
                << object->vt()->clss << ".");
            tmn_suspend_enable();
            
        }
#endif
        exn_clear();
    }
    // restores curent thread exception risen before if any
    if (NULL != cte) {
        exn_raise_object(cte);
    }

    TRACE2("ref", "enqueue_references() completed");
} //Objects_To_Finalize::notify_reference_queues


static Objects_To_Finalize objects_to_finalize;

void vm_finalize_object(Managed_Object_Handle p_obj)
{
    objects_to_finalize.add_object((ManagedObject *)p_obj);
    if(get_native_finalizer_thread_flag()){
        int finalizable_obj_num = objects_to_finalize.getLength();
        sched_heavy_finalizer(finalizable_obj_num);
    }
} //vm_finalize_object

void vm_run_pending_finalizers()
{
    NativeObjectHandles nhs;
    assert(hythread_is_suspend_enabled());
    /* BEGIN: modified for NATIVE FINALIZER THREAD */
    if(get_native_finalizer_thread_flag()) {
        activate_finalizer_threads(FALSE);
    }
    else {
        objects_to_finalize.run_finalizers();
    }
    /* END: modified for NATIVE FINALIZER THREAD */
} //vm_run_pending_finalizers

int vm_do_finalization(int quantity)
{
    assert(hythread_is_suspend_enabled());
    return objects_to_finalize.do_finalization(quantity);
} //vm_do_finalization

bool is_it_finalize_thread() {
    return ((p_TLS_vmthread->finalize_thread_flags & FINALIZER_THREAD) != 0);
}

void vm_enumerate_objects_to_be_finalized()
{
    TRACE2("enumeration", "enumeration objects to be finalized");
    INFO2("stats.finalize", "Enumerating finalize queue");
    objects_to_finalize.enumerate_for_gc();
} //vm_enumerate_objects_to_be_finalized

int vm_get_finalizable_objects_quantity()
{
    return objects_to_finalize.getLength();
}

/* returns true if finalization system is turned on, and false otherwise */
bool vm_finalization_is_enabled()
{
    return VM_Global_State::loader_env->java_lang_FinalizerThread_Class != NULL;
}

void vm_obtain_finalizer_fields() {
    objects_to_finalize.obtain_fields();
}

// -- Code to deal with Reference Queues that need to be notified.

static References_To_Enqueue references_to_enqueue;

void vm_enumerate_references_to_enqueue()
{
    TRACE2("enumeration", "enumeration pending references to be enqueued");
    INFO2("stats.finalize", "Enumerating reference queue");
    references_to_enqueue.enumerate_for_gc();
} //vm_enumerate_references_to_enqueue

void vm_enqueue_reference(Managed_Object_Handle obj)
{
    TRACE2("ref", obj << " is being added to enqueue list");
    references_to_enqueue.add_object((ManagedObject *)obj);
} // vm_enqueue_reference

void vm_activate_ref_enqueue_thread()
{
    if(get_native_ref_enqueue_thread_flag())
        activate_ref_enqueue_thread(FALSE);
}

void vm_enqueue_references()
{
    /* BEGIN: modified for NATIVE REFERENCE ENQUEUE THREAD */
    if(get_native_ref_enqueue_thread_flag())
        activate_ref_enqueue_thread(FALSE);
    else
        references_to_enqueue.enqueue_references();
    /* END: modified for NATIVE REFERENCE ENQUEUE THREAD */
} //vm_enqueue_references

int vm_get_references_quantity()
{
    return references_to_enqueue.getLength();
}

/* added for NATIVE REFERENCE ENQUEUE THREAD */
void vm_ref_enqueue_func(void)
{
    references_to_enqueue.enqueue_references();
}
