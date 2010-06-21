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
 * @brief Encoder definitions specific for IA-32 and Intel 64 platforms.
 */
 
#if !defined(__ENC_IA32_H_INCLUDED__)
#define __ENC_IA32_H_INCLUDED__

#include "enc.h"

//#define ENCODER_ISOLATE
#include "enc_base.h"
#ifdef ENCODER_ISOLATE
    using namespace enc_ia32;
#endif

namespace Jitrino {
namespace Jet {
/**
 * @brief Converts given AR into platform-specific register name.
 * @param ar - AR to convert.
 * @param jt - defines the preferred size of the register. If #jvoid, then 
 * the wides possible register returned.
 */
RegName devirt(AR ar, jtype jt=jvoid);
/** 
 * @brief Converts platform-specific register name into AR.
 */
AR virt(RegName reg);

extern const Opnd eax;
extern const Opnd ecx;
extern const Opnd edx;

}
}; // ~namespace Jitrino::Jet

#endif  // __ENC_IA32_H_INCLUDED__
