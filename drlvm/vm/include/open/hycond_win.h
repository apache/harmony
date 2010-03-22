/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @file
 * Condition variable implementation for Windows, based on wait queues.
 */
#ifndef _HYCOND_WIN_H
#define _HYCOND_WIN_H

#ifndef _WIN32
#error This code is only supposed to work on Windows
#endif // _WIN32

  #if _MSC_VER >= 1300 || __INTEL_COMPILER
  // workaround for the
  // http://www.microsoft.com/msdownload/platformsdk/sdkupdate/2600.2180.7/contents.htm
  #include <winsock2.h>
  #endif

#include <windows.h>

struct waiting_node {
   // notification event
   HANDLE event;
   // doubly-linked queue
   struct waiting_node *prev;
   struct waiting_node *next;
};

// queue based condition implementation
struct HyCond {
    // Synchronization is necessary because signal() caller is not required
    // to hold mutex associated with the condition variable.
    osmutex_t queue_mutex;
    // head-tail marker node
    struct waiting_node dummy_node;
};

#endif // _HYCOND_WIN_H
