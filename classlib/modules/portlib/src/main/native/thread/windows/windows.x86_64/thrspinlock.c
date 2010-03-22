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

/*
 * @file
 * @ingroup Thread
 */

#include <windows.h>
#include <stdlib.h>
#include "hycomp.h"
#include "hythread.h"
#include "hymutex.h"

UDATA hythread_spinlock_swapState(hythread_monitor_t monitor, UDATA newState) {
  // TODO: need implementation
  return (UDATA)0;
}

IDATA hythread_spinlock_acquire(hythread_t self, hythread_monitor_t monitor) {
  // TODO: need implementation
  return (IDATA)0;
}


