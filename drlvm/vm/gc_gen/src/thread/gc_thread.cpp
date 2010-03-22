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
 * @author Xiao-Feng Li, 2006/10/05
 */

#include "gc_thread.h"

static hythread_tls_key_t tls_gc_key;       
POINTER_SIZE_INT tls_gc_offset;
hythread_group_t gc_thread_group = NULL;

#if defined(ALLOC_ZEROING) && defined(ALLOC_PREFETCH)
POINTER_SIZE_INT PREFETCH_DISTANCE = 1024;
POINTER_SIZE_INT ZEROING_SIZE = 2*KB;
POINTER_SIZE_INT PREFETCH_STRIDE = 64;
Boolean PREFETCH_ENABLED = FALSE;
#endif

void gc_tls_init()
{
  hythread_tls_alloc(&tls_gc_key);
  tls_gc_offset = hythread_tls_get_offset(tls_gc_key);  
  
  return;
}
