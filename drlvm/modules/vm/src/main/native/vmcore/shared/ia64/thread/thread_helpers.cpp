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
 * @file thread_helpers_ipf.c
 * Missing definition to ipf compile
 */

#include <open/hythread_ext.h>
#include <thread_helpers.h>
#include "jthread.h"

#include <assert.h>

void *dummy_tls_func()
{
    LDIE(56, "shouldn't get here");
}


fast_tls_func *get_tls_helper(hythread_tls_key_t key)
{
    return dummy_tls_func;
}
