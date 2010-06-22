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
 * @author Alexander Astapchuk
 */
 /**
  * @file
  * @brief Concentrates all the codegen's knowledge about the TLS and
  * related things.
  */


#ifndef _IA32_TLS_H_
#define _IA32_TLS_H_

#include "Ia32IRManager.h"

namespace Jitrino {
namespace Ia32 {

/**
 * @brief Creates a sequence that loads TLS base.
 *
 * Create a sequence of instructions that loads TLS base into an Opnd.
 * The TLS base is created with the provided type.
 *
 * The sequence may be empty, for example, on Windows, where the TLS base
 * is simple memory reference FS:[0x14].
 * The sequence may include a call, or may result in just several memory loads.
 * The instructions are appended at the end of ctrlNode.
 */
Opnd* createTlsBaseLoadSequence(IRManager& irManager, Node* ctrlNode, Type* tlsBaseType);


/**
 * Same as 3-args method, but presumes type for the TLS base as I_8*.
 */
Opnd* createTlsBaseLoadSequence(IRManager& irManager, Node* ctrlNode);


}}; // ~Jitrino::Ia32

#endif  // ifdef _IA32_TLS_H_

