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

#ifndef _JAVA_SUPPORT_H_
#define _JAVA_SUPPORT_H_

#include "open/types.h"
#include "../common/gc_platform.h"

extern Class_Handle GCHelper_clss;
extern Boolean java_helper_inlined;

void HelperClass_set_GenMode(Boolean status);
void HelperClass_set_NosBoundary(void* boundary);

#endif /*_JAVA_SUPPORT_H_*/


