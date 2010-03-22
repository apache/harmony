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
 * @author Evgueni Brevnov
 */

#include "m2n.h"

const U_32 FRAME_UNKNOWN = 0x00;
const U_32 FRAME_NON_UNWINDABLE = 0x80;
const U_32 FRAME_JNI = 0x01 | FRAME_NON_UNWINDABLE;
const U_32 FRAME_COMPILATION = 0x02 | FRAME_NON_UNWINDABLE;
const U_32 FRAME_UNPOPABLE = 0x0000;
const U_32 FRAME_POPABLE = 0x0100;
const U_32 FRAME_POP_NOW = 0x0200;
const U_32 FRAME_POP_DONE = FRAME_POPABLE | FRAME_POP_NOW;
const U_32 FRAME_POP_MASK = 0x0700;
const U_32 FRAME_SAFE_POINT = 0x0800;
const U_32 FRAME_MODIFIED_STACK = 0x1000;
