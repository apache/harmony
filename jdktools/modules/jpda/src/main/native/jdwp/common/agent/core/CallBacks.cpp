/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include "CallBacks.h"

using namespace jdwp;
using namespace CallBacks;

//-----------------------------------------------------------------------------
// Heap callbacks, used in Instances, InstanceCounts command
//-----------------------------------------------------------------------------

     /**
      * Describes a reference from an object or the VM (the referrer) 
      * to another object (the referree) or a heap root to a referree. 
      */
    jint JNICALL CallBacks::HeapReferenceCallback
        (jvmtiHeapReferenceKind reference_kind, 
         const jvmtiHeapReferenceInfo* reference_info, 
         jlong class_tag, 
         jlong referrer_class_tag, 
         jlong size, 
         jlong* tag_ptr, 
         jlong* referrer_tag_ptr, 
         jint length, 
         void* user_data) {
             jlong tag_value = *(static_cast<jlong *>(user_data));
            *tag_ptr = tag_value;
            return JVMTI_VISIT_OBJECTS;
     }

     
//-----------------------------------------------------------------------------
// Heap callbacks, used in ReferringObject command
//-----------------------------------------------------------------------------

     /**
      * Describes a reference from an object or the VM (the referrer) 
      * to another object (the referree) or a heap root to a referree. It 
      * will be invoked by ReferringObject command.
      */
     jint JNICALL CallBacks::HeapReferenceCallback_ReferringObject
        (jvmtiHeapReferenceKind reference_kind, 
         const jvmtiHeapReferenceInfo* reference_info, 
         jlong class_tag, 
         jlong referrer_class_tag, 
         jlong size, 
         jlong* tag_ptr, 
         jlong* referrer_tag_ptr, 
         jint length, 
         void* user_data) {
             jlong targetObjTag = *(static_cast<jlong *>(user_data));
             jlong *ptr = static_cast<jlong *>(user_data);
             ptr++;
             jlong referrerObjTag = *ptr;
             if(*tag_ptr == targetObjTag && referrer_tag_ptr != NULL) {
                *referrer_tag_ptr = referrerObjTag;
             }
            return JVMTI_VISIT_OBJECTS;
     }
 
