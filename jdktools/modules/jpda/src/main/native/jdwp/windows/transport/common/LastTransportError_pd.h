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

#ifndef _LASTTRANSPORTERROR_PD_H
#define _LASTTRANSPORTERROR_PD_H

#include <windows.h>

typedef DWORD ThreadId_t;

static inline bool ThreadId_equal(ThreadId_t treadId1, ThreadId_t treadId2)
{
    return (treadId1 == treadId2);
} // ThreadId_equal()

#endif // _LASTTRANSPORTERROR_PD_H
