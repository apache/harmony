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

#ifndef _VM_PROPERTIES_H
#define _VM_PROPERTIES_H

#include <apr_hash.h>
#include <apr_pools.h>
#include <apr_thread_rwlock.h>

class Properties
{
public:
    Properties();
    ~Properties() {
        apr_pool_destroy(local_ht_pool);
    }
    void set(const char * key, const char * value);
    /** set property only if it's not set yet */
    void set_new(const char * key, const char * value);
    char* get(const char * key);
    void destroy(char* value);
    bool is_set(const char * key);
    void unset(const char * key);
    char** get_keys();
    char** get_keys_staring_with(const char* prefix);
    void destroy(char** keys);
private:
    apr_pool_t* local_ht_pool;
    apr_hash_t* hashtables_array;
    apr_thread_rwlock_t* rwlock_array;
};


#endif // _VM_PROPERTIES_H

