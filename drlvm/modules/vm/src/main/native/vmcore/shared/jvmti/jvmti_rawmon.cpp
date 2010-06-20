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
 * @author Gregory Shimansky, Pavel Rebriy
 */  
/*
 * JVMTI raw monitor API
 */

#include <apr_time.h>

#include "platform_lowlevel.h"
#include "lock_manager.h"
#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "vm_threads.h"
#include "vm_process.h"
#include "cxxlog.h"
#include "ti_thread.h"
#include "suspend_checker.h"
#include "exceptions.h"


/*
 * Create Raw Monitor
 *
 * Create a raw monitor
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiCreateRawMonitor(jvmtiEnv* env,
                      const char* name,
                      jrawMonitorID* monitor_ptr)
{
    /**
     * Monitor trace
     */
    TRACE2("jvmti.monitor", "CreateRawMonitor called, name = " << name);
    if (name == NULL || monitor_ptr == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }
    //FIXME: integration, add name.
    return (jvmtiError)jthread_raw_monitor_create(monitor_ptr);
} // jvmtiCreateRawMonitor

/*
 * Destroy Raw Monitor
 *
 * Destroy the raw monitor. If the monitor being destroyed has
 * been entered by this thread, it will be exited before it is
 * destroyed. If the monitor being destroyed has been entered by
 * another thread, an error will be returned and the monitor will
 * not be destroyed.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiDestroyRawMonitor(jvmtiEnv* env,
                       jrawMonitorID monitor)
{
    return (jvmtiError)jthread_raw_monitor_destroy(monitor);
} // jvmtiDestroyRawMonitor

/*
 * Raw Monitor Enter
 *
 * Gain exclusive ownership of a raw monitor.
 * The same thread may enter a monitor more then once.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiRawMonitorEnter(jvmtiEnv* env,
                     jrawMonitorID monitor)
{
    return (jvmtiError)jthread_raw_monitor_enter(monitor);
} // jvmtiRawMonitorEnter

/*
 * Raw Monitor Exit
 *
 * Release exclusive ownership of a raw monitor.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiRawMonitorExit(jvmtiEnv* env,
                    jrawMonitorID monitor)
{
    return (jvmtiError)jthread_raw_monitor_exit(monitor);
} // jvmtiRawMonitorExit

/*
 * Raw Monitor Wait
 *
 * Wait for notification of the raw monitor.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiRawMonitorWait(jvmtiEnv* env,
                    jrawMonitorID monitor,
                    jlong millis)
{
    jvmtiError res = (jvmtiError)jthread_raw_monitor_wait(monitor, millis);
    if (exn_raised() && res == JVMTI_ERROR_INTERRUPT)
        return JVMTI_ERROR_NONE;
    return res;
} // jvmtiRawMonitorWait



/*
 * Raw Monitor Notify
 *
 * Notify a single thread waiting on the raw monitor.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiRawMonitorNotify(jvmtiEnv* env,
                      jrawMonitorID monitor)
{
    /**
     * Monitor trace
     */
    TRACE2("jvmti.monitor", "RawMonitorNotify called, id = " << monitor);
    return (jvmtiError)jthread_raw_monitor_notify(monitor);
} // jvmtiRawMonitorNotify

/*
 * Raw Monitor Notify All
 *
 * Notify all threads waiting on the raw monitor.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiRawMonitorNotifyAll(jvmtiEnv* env,
                         jrawMonitorID monitor)
{
    /**
     * Monitor trace
     */
    TRACE2("jvmti.monitor", "RawMonitorNotifyAll called, id = " << monitor);
    return (jvmtiError)jthread_raw_monitor_notify_all(monitor);
} // jvmtiRawMonitorNotifyAll

