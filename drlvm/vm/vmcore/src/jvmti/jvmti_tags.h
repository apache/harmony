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

#ifndef _JVMTI_TAGS_H_
#define _JVMTI_TAGS_H_
/*
 * JVMTI tags datatypes
 */

#include <list>
#include <set>

#include "jvmti_types.h"
#include "open/types.h"
#include "vtable.h"

#include "ulist.h"

struct tag_pair {
    Managed_Object_Handle obj;
    jlong tag;

    bool operator==(tag_pair p) {
        return (obj == p.obj);
    }
};

typedef ulist<tag_pair> tag_pair_list;

struct TITags {
    private:
    tag_pair_list tags;

    public:
    TITags() : tags(128) {}

    /// gets tag.
    /// @return tag value or 0 if no tag exist
    jlong get(Managed_Object_Handle obj);

    /// sets tag.
    /// Tag value of 0 means remove tag.
    void set(Managed_Object_Handle obj, jlong tag);

    /// updates the tag list by either
    /// updating tag value, inserting new entry
    /// or removing entry
    void update(Managed_Object_Handle obj, jlong tag, tag_pair** tptr);

    /// constructs a list of tagged objects filtered by tag values.
    /// @param[in] tagset - the set of tag values to search
    /// @param[out] objects - the set of found tag-object pairs
    void get_objects_with_tags(std::set<jlong> & tagset, std::list<tag_pair> & objects);
 
    /// enumerates all tagged objects as weak roots
    void enumerate();
    
    /// calls vm_iterate_object for each tagged object
    void iterate();

    /// removes reclaimed objects from tag list,
    /// @param send_event - true means that jvmti_send_object_free_event() 
    ///   will be called for each reclaimed object tag.
    /// @param ti_env - the pass-through parameter 
    ///   to jvmti_send_object_free_event()
    void clean_reclaimed_object_tags(bool send_event, TIEnv* ti_env);

    /// deletes all tags.
    void clear();
};

/**
 * returns location of object tag pointer ("tptr").
 */
inline tag_pair** ti_get_object_tptr(Managed_Object_Handle obj)
{
    ManagedObject *o = (ManagedObject*)obj;
    if (o->vt()->clss->is_array()) {
        return (tag_pair**)((VM_Vector*)obj)->get_tag_pointer_address();
    } else {
        return (tag_pair**)((ManagedObject*)obj)->get_tag_pointer_address();
    }
}

// update tag pointer in objects when the tag pair has been moved
// ("magically" called by ulist<tag_pair>::erase())
inline void element_moved(tag_pair* from, tag_pair* to) {
    assert(from->obj == to->obj);
    assert(from->tag == to->tag);
    // if object was already reclaimed and weak root reset,
    // the object->tag_pair pointer does not need to be updated
    if (from->obj != NULL) {
        tag_pair** tptr = ti_get_object_tptr(from->obj);
        *tptr = to;
    }
}


#endif // _JVMTI_TAGS_H_
