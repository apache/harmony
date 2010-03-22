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
#define LOG_DOMAIN "enumeration"
#include "cxxlog.h"

#include "vtable.h"
#include "root_set_enum_internal.h"
#include "GlobalClassLoaderIterator.h"
#include <apr_time.h>
#include "unloading.h"
#include "thread_manager.h"
#include "interpreter.h"
#include "finalize.h"
#include "jvmti_direct.h"


////////// M E A S U R E M E N T of thread suspension time///////////
apr_time_t _start_time, _end_time;
apr_time_t thread_suspend_time = 0;

static inline void
vm_time_start_hook(apr_time_t *start_time)
{   
    *start_time = apr_time_now();
}


static inline apr_time_t
vm_time_end_hook(apr_time_t *start_time, apr_time_t *end_time)
{   
    *end_time = apr_time_now();
    apr_time_t time = *end_time - *start_time;
    //STATS(event << ": " << time);
    return time;
}
//////////////////////////////////////////////////////////////////////


static void 
vm_enumerate_the_current_thread(VM_thread * vm_thread)
{
    assert(p_TLS_vmthread == vm_thread);
    // Process roots for the current thread
    //assert(p_TLS_vmthread->gc_status == zero);
    //p_TLS_vmthread->gc_status = gc_at_safepoint;
    vm_enumerate_thread(vm_thread);

    // Enumeration for this thread is complete.
    //p_TLS_vmthread->gc_status = gc_enumeration_done;

} // vm_enumerate_the_current_thread

//
// This stops all the threads before it enumerates any of the threads.
// This is important for parallel collectors since if all the threads
// aren't stopped then there might be some confusion about which roots
// are to be processed by which GC threads. In particular if a root is
// enumerated as belonging to some heap and a running mutator thread
// changes the value in the slot to point to another heap the gc threads
// will get confused. So stop all the threads and then do the enumeration.
//


static void
stop_the_world_root_set_enumeration()
{
    vm_thread_t current_vm_thread;

    TRACE2("vm.gc", "stop_the_world_root_set_enumeration()");

    // Run through list of active threads and suspend each one of them.

    INFO2("threads","Start thread suspension ");
    vm_time_start_hook(&_start_time);   //thread suspension time measurement        
    
    hythread_iterator_t iterator;
    hythread_suspend_all(&iterator, NULL);

    // no matter how counter-intuitive,
    // gc_force_gc() expects gc_enabled_status == disabled,
    // but, obviously, at a GC safepoint.
    // See gc-safety (aka suspend-safety) rules explained elsewhere
    // -salikh 2005-05-12
    // it is convenient to have gc_enabled_status == disabled
    // during the enumeration -salikh
    hythread_suspend_disable();

    thread_suspend_time = vm_time_end_hook(&_start_time, &_end_time);
    INFO2("tm.suspend","Thread suspension time: "<< thread_suspend_time <<" mksec");

    if(jvmti_should_report_event(JVMTI_EVENT_GARBAGE_COLLECTION_START)) {
        jvmti_send_gc_start_event();
    }

    if(gc_supports_class_unloading()) class_unloading_clear_mark_bits();

    current_vm_thread = p_TLS_vmthread;
    // Run through list of active threads and enumerate each one of them.
    hythread_t tm_thread = hythread_iterator_next(&iterator);    
    while (tm_thread) {
        vm_thread_t thread = jthread_get_vm_thread(tm_thread);
        //assert(thread);
        if (thread && thread != current_vm_thread) {
            vm_enumerate_thread(thread);
            // Enumeration for this thread is complete.
            //thread->gc_status = gc_enumeration_done;
            //assert(thread->gc_status==gc_enumeration_done);
            //thread->gc_status=gc_enumeration_done;
        }
        tm_thread = hythread_iterator_next(&iterator);
    }

    vm_enumerate_the_current_thread(current_vm_thread);

    // finally, process all the global refs
    vm_enumerate_root_set_global_refs();

    TRACE2("enumeration", "enumeration complete");

} // stop_the_world_root_set_enumeration




// Entry point into root-set-enumeration code.

void 
vm_enumerate_root_set_all_threads()
{
    assert(hythread_is_suspend_enabled());
    stop_the_world_root_set_enumeration();
    assert(!hythread_is_suspend_enabled());
} //vm_enumerate_root_set_all_threads

void vm_update_jlc_for_class_unloading()
{
    TRACE2("classloader.unloading", "update jlc from Vtables");
    GlobalClassLoaderIterator ClIterator;
    ClassLoader *cl = ClIterator.first();
    while(cl) {
        GlobalClassLoaderIterator::ClassIterator itc;
        GlobalClassLoaderIterator::ReportedClasses RepClasses = cl->GetReportedClasses();
        Class* c;
        for (itc = RepClasses->begin(); itc != RepClasses->end(); itc++)
        {
          c = itc->second;
          assert(c);
          if(c->get_vtable())
            c->get_vtable()->jlC = *(c->get_class_handle());
        }
        ClassTable::iterator itl;
        ClassTable* p_loadedClasses = cl->GetLoadedClasses();
        if (!cl->IsBootstrap())
          for (itl = p_loadedClasses->begin(); itl != p_loadedClasses->end(); itl++)
          {
            c = itl->second;
            assert(c);
            if(c->get_vtable())
              c->get_vtable()->jlC = *(c->get_class_handle());
          }
        cl = ClIterator.next();
   }
}

// Called after GC from VM side....We need to restart all the mutators.
void vm_resume_threads_after()
{
    TRACE2("vm.gc", "vm_resume_threads_after()");

    vm_update_jlc_for_class_unloading();
    if(gc_supports_class_unloading()) class_unloading_start();

    if(jvmti_should_report_event(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH)) {
        jvmti_send_gc_finish_event();
    }
    jvmti_clean_reclaimed_object_tags();

    // Run through list of active threads and resume each one of them.
    hythread_suspend_enable();
    hythread_resume_all(NULL);

    // Make sure register stack is up-to-date with the potentially updated backing store
    si_reload_registers();

}  //vm_resume_threads_after

void vm_hint_finalize() {
    TRACE2("vm.hint", "vm_hint_finalize() started");
    // vm_hint_finalize() is called from GC function, 
    // which itself operates either with 
    // gc_enabled_status == disabled, e.g. from managed code,
    // but at the GC-safe point (because the collection was done)
    assert(!hythread_is_suspend_enabled());

    tmn_suspend_enable();
    assert(hythread_is_suspend_enabled());

    // Finalizers and reference enqueuing is performed from vm_hint_finalize(),
    // GC guarantees to call this function after the completion of collection,
    // *after* it releases global GC lock.

    // Several Reference Queues may need to be notified because the GC added References to them. Do that now.
    //TRACE2("ref", "Enqueueing references");
    //vm_enqueue_references();
    vm_activate_ref_enqueue_thread();
    
    // For now we run the finalizers immediately in the context of the thread which requested GC.
    // Eventually we may have a different scheme, e.g., a dedicated finalize thread.
    TRACE2("finalize", "Running pending finalizers");
    vm_run_pending_finalizers();
    TRACE2("finalize", "Completed vm_run_pending_finalizers");

    tmn_suspend_disable();
    TRACE2("vm.hint", "vm_hint_finalize() completed");
} //vm_hint_finalize



///////////////////////////////////////////
///////////////////////////////////////////



void vm_enumerate_thread(VM_thread *thread)
{
    if (interpreter_enabled()) {
        interpreter.interpreter_enumerate_thread(thread);
        return;
    }
    StackIterator* si;
    TRACE2("enumeration", "Enumerating thread " << thread << 
    (thread == p_TLS_vmthread ? ", this thread" : ", suspended in native code"));
     si = si_create_from_native(thread);
    vm_enumerate_root_set_single_thread_on_stack(si);    
    // Enumerate references associated with a thread that are not stored on the thread's stack.
    vm_enumerate_root_set_single_thread_not_on_stack(thread);
} //vm_enumerate_thread




//////////////////////////////////////////////////////////////////////////////
///////////////////////  LINUX/ WINDOWS specific /////////////////////////////
//////////////////////////////////////////////////////////////////////////////




