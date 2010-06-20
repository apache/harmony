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

#define LOG_DOMAIN "init.properties"
#include "cxxlog.h"
#include "properties.h"

class PropValue
{
public:
    char* value;

    PropValue(const char* v) {
        value = (v == NULL ? NULL : (char*)strdup(v));
    }
    ~PropValue() {
        STD_FREE(value);
    }
};

Properties::Properties() 
{
    if (APR_SUCCESS != apr_pool_create(&local_ht_pool, NULL)) {
        LDIE(9, "Cannot initialize properties pool");
    }
    hashtables_array = apr_hash_make(local_ht_pool);
    rwlock_array = NULL;
    if (APR_SUCCESS != apr_thread_rwlock_create(&rwlock_array, local_ht_pool)) {
        LDIE(10, "Cannot initialize properties table mutex");
    }
}

void Properties::set(const char * key, const char * value) 
{
    TRACE("set property " << key << " = " << value);

    if (APR_SUCCESS != apr_thread_rwlock_wrlock(rwlock_array)) {
        LDIE(11, "Cannot lock properties table");
    }
    PropValue* val = (PropValue*) apr_hash_get(hashtables_array, (const void*) key, APR_HASH_KEY_STRING);
    apr_hash_set(hashtables_array, (const void*) strdup(key), APR_HASH_KEY_STRING, (const void*) new PropValue(value));
    if (val != NULL) {
        delete(val);
    }
    if (APR_SUCCESS != apr_thread_rwlock_unlock(rwlock_array)) {
        LDIE(12, "Cannot unlock properties table");
    }
}

void Properties::set_new(const char * key, const char * value)
{
    TRACE("try to set property " << key << " = " << value);

    if (APR_SUCCESS != apr_thread_rwlock_wrlock(rwlock_array)) {
        LDIE(11, "Cannot lock properties table");
    }
    PropValue* val = (PropValue*) apr_hash_get(hashtables_array, (const void*) key, APR_HASH_KEY_STRING);
    if (NULL == val) {
        apr_hash_set(hashtables_array, (const void*) strdup(key), APR_HASH_KEY_STRING, (const void*) new PropValue(value));
        TRACE("defined property " << key);
    } else {
        TRACE("property is already defined: " << key);
    }

    if (APR_SUCCESS != apr_thread_rwlock_unlock(rwlock_array)) {
        LDIE(12, "Cannot unlock properties table");
    }
}

char* Properties::get(const char * key) {
    char* return_value= NULL;
    if (APR_SUCCESS != apr_thread_rwlock_rdlock(rwlock_array)) {
        LDIE(11, "Cannot lock properties table");
    }
    PropValue* val = (PropValue*) apr_hash_get(hashtables_array, (const void*) key, APR_HASH_KEY_STRING);
    if (val != NULL) {
        return_value = strdup(val->value);
    }
    if (APR_SUCCESS != apr_thread_rwlock_unlock(rwlock_array)) {
        LDIE(12, "Cannot unlock properties table");
    }
    return return_value;
}

void Properties::destroy(char* value) {
    STD_FREE((void*)value);
}

bool Properties::is_set(const char * key) {
    if (apr_hash_get(hashtables_array, (const void*) key, APR_HASH_KEY_STRING) == NULL) {
        return FALSE;
    } else {
        return TRUE;
    }
}

void Properties::unset(const char * key) {
    apr_hash_index_t* hi;
    const void* deleted_key;
    if (APR_SUCCESS != apr_thread_rwlock_wrlock(rwlock_array)) {
        LDIE(11, "Cannot lock properties table");
    }
    PropValue* val = (PropValue*) apr_hash_get(hashtables_array, (const void*) key, APR_HASH_KEY_STRING);
    if (val != NULL) {
        for (hi = apr_hash_first(local_ht_pool, hashtables_array); hi; hi = apr_hash_next(hi)) {
            apr_hash_this(hi, &deleted_key, NULL, NULL);
            if (!(strncmp(key, (const char*)deleted_key, strlen(key)) && 
               strncmp((const char*)deleted_key, key, strlen((const char*)deleted_key)))) {
                break;
            }
        }
        apr_hash_set(hashtables_array, (const void*)key, APR_HASH_KEY_STRING, NULL);
        STD_FREE((void*)deleted_key);
        delete(val);
    }
    if (APR_SUCCESS != apr_thread_rwlock_unlock(rwlock_array)) {
        LDIE(12, "Cannot unlock properties table");
    }
}

char** Properties::get_keys() {
    apr_hash_index_t* hi;
    int properties_count = 0;
    const void* key;

    if (APR_SUCCESS != apr_thread_rwlock_rdlock(rwlock_array)) {
        LDIE(11, "Cannot lock properties table");
    }
    for (hi = apr_hash_first(local_ht_pool, hashtables_array); hi; hi = apr_hash_next(hi)) {
        properties_count++;
    }
    char** return_value = (char**) STD_MALLOC(sizeof(char*) * (properties_count + 1));
    properties_count = 0;
    for (hi = apr_hash_first(local_ht_pool, hashtables_array); hi; hi = apr_hash_next(hi)) {
            apr_hash_this(hi, &key, NULL, NULL);
            return_value[properties_count++] = (char*)strdup((char*)key);
    }
    return_value[properties_count] = NULL;
    if (APR_SUCCESS != apr_thread_rwlock_unlock(rwlock_array)) {
        LDIE(12, "Cannot unlock properties table");
    }
    return return_value;
}

char** Properties::get_keys_staring_with(const char* prefix) {
    apr_hash_index_t* hi;
    int properties_count = 0;
    const void* key;

    if (APR_SUCCESS != apr_thread_rwlock_rdlock(rwlock_array)) {
        LDIE(11, "Cannot lock properties table");
    }
    for (hi = apr_hash_first(local_ht_pool, hashtables_array); hi; hi = apr_hash_next(hi)) {
        apr_hash_this(hi, &key, NULL, NULL);
        if (!strncmp(prefix, (char*)key, strlen(prefix)))
            properties_count++;
    }
    char** return_value = (char**) STD_MALLOC(sizeof(char*) * (properties_count + 1));
    properties_count = 0;
    for (hi = apr_hash_first(local_ht_pool, hashtables_array); hi; hi = apr_hash_next(hi)) {
        apr_hash_this(hi, &key, NULL, NULL);
        if (!strncmp(prefix, (char*)key, strlen(prefix))) {
            return_value[properties_count++] = (char*)strdup((char*)key);
        }
    }
    return_value[properties_count] = NULL;
    if (APR_SUCCESS != apr_thread_rwlock_unlock(rwlock_array)) {
        LDIE(12, "Cannot unlock properties table");
    }
    return return_value;
}

void Properties::destroy(char** keys) {
    int i = 0;
    while (keys[i] != NULL) {
        STD_FREE((void*)keys[i++]);
    }
    STD_FREE(keys);
}
