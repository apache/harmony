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

#include <algorithm>

// global
#include "cxxlog.h"
#include "open/gc.h"
#include "open/vm_gc.h"

// VM-internal
#include "environment.h"    // Global_Env
//#include "open/vm_util.h"   // VM_Global_State
#include "jvmti_direct.h"   // TIEnv
#include "vm_arrays.h"      // vm_array_size

// private
#include "jvmti_tags.h"

jlong TITags::get(Managed_Object_Handle obj) {
    tag_pair **tp = ti_get_object_tptr(obj);
    if (*tp != NULL) {
        return (*tp)->tag;
    } else {
        return 0;
    }
}

void TITags::update(Managed_Object_Handle obj, jlong tag, tag_pair** tp)
{
    assert((*tp) == NULL ||
            ((*tp)->obj == obj && tp == ti_get_object_tptr(obj)));
    if (tag != 0) {
        if ((*tp) != NULL) {
            // update tag if we already had a pair
            (*tp)->tag = tag;
        } else {
            // add new tag pair
            tag_pair pair;
            pair.obj = obj;
            pair.tag = tag;
            tags.push_back(pair);
            *tp = &(tags.back());
        }
    } else {
        // remove tag if we had any
        if ((*tp) != NULL) {
            tags.erase(tags.find(*tp));
            *tp = NULL;
        }
    }
}

void TITags::set(Managed_Object_Handle obj, jlong tag) {
    tag_pair **tp = ti_get_object_tptr(obj);
    if (!*tp) {
        // the object was not tagged before
        if (tag != 0) {
            // only need to store non-zero tags
            tag_pair pair;
            pair.obj = obj;
            pair.tag = tag;
            tags.push_back(pair);
            *tp = &(tags.back());
        }
    } else {
        // the object was tagged before
        if (tag != 0) {
            // update the tag value
            (*tp)->tag = tag;
        } else {
            if (*tp) {
                // remove the tag
                tags.erase(tags.find(*tp));
                *tp = NULL;
            }
        }
    }
}

void TITags::enumerate() {
    tag_pair_list::iterator i;
    for (i = tags.begin(); i != tags.end(); i++) {
        // (2) false = not pinned,
        // (3) true = "long" weak root = reset after finalization
        gc_add_weak_root_set_entry(&i->obj, false, true);
    }
}

void TITags::get_objects_with_tags(
        std::set<jlong> & tagset,
        std::list<tag_pair> & objects)
{
    assert(objects.empty());
    tag_pair_list::iterator i;
    for (i = tags.begin(); i != tags.end(); i++) {
        if (tagset.find(i->tag) != tagset.end()) {
            objects.push_back(*i);
        }
    }
}

void TITags::iterate ()
{
    tag_pair_list::iterator i;
    for (i = tags.begin(); i != tags.end(); i++) {
        assert(i->obj);
        bool r = vm_iterate_object(i->obj);
        // terminate iteration if vm_iterate_object
        // returns false
        if (false == r) return;
    }
}

void jvmti_send_object_free_event(TIEnv* ti_env, jlong tag)
{
    jvmtiEventObjectFree func =
        (jvmtiEventObjectFree)
        ti_env->get_event_callback(JVMTI_EVENT_OBJECT_FREE);

    if (NULL != func) {
        // user call backs are supposed to be
        // called in suspend-enabled mode.
        // switching is safe because we are in stop-the-world phase
        tmn_suspend_enable(); // ----vv
        TRACE2("jvmti.event.of", "Callback JVMTI_EVENT_OBJECT_FREE called");
        func((jvmtiEnv*)ti_env, tag);
        TRACE2("jvmti.event.of", "Callback JVMTI_EVENT_OBJECT_FREE finished");
        tmn_suspend_disable(); // ----^^
    }
}

void TITags::clean_reclaimed_object_tags(bool send_event, TIEnv* ti_env)
{
    tag_pair_list::iterator i;
    for (i = tags.begin(); i != tags.end(); i++) {
        if (i->obj == NULL) {
            TRACE2("jvmti.tags", "object tagged by " << i->tag << " reclaimed"
                    << (send_event ? ", event sent" : ""));
            if (send_event) {
                jvmti_send_object_free_event(ti_env, i->tag);
            }
            tags.erase(i--);
        }
    }
}

void jvmti_clean_reclaimed_object_tags()
{
    Global_Env *env = VM_Global_State::loader_env;
    // this event is sent from stop-the-world setting
    assert(!hythread_is_suspend_enabled());

    DebugUtilsTI *ti = env->TI;
    if (!ti->isEnabled())
        return;

    TIEnv *ti_env = ti->getEnvironments();
    TIEnv *next_env;
    while (NULL != ti_env)
    {
        next_env = ti_env->next;
        bool send_event = jvmti_should_report_event(JVMTI_EVENT_OBJECT_FREE);

        TITags* tags = ti_env->tags;
        if (tags != NULL) {
            tags->clean_reclaimed_object_tags(send_event, ti_env);
        }

        ti_env = next_env;
    }
}

void TITags::clear()
{
    assert(!hythread_is_suspend_enabled());
    tag_pair_list::iterator i;
    for (i = tags.begin(); i != tags.end(); i++) {
        update(i->obj, 0, ti_get_object_tptr(i->obj));
    }
}
