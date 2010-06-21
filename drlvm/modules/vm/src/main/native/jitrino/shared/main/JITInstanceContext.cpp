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
* @author Mikhail Y. Fursov
*/

#include "JITInstanceContext.h"
#include "PMF.h"

namespace Jitrino {

JITInstanceContext::JITInstanceContext(MemoryManager& _mm, JIT_Handle _jitHandle, const char* _jitName) 
: jitHandle(_jitHandle), jitName(_jitName)
, profInterface(NULL), mm(_mm)
{
    useJet = isNameReservedForJet(_jitName);
    pmf = new (mm) PMF(mm, *this);
}


JITInstanceContext* JITInstanceContext::getContextForJIT(JIT_Handle jitHandle) {
    return Jitrino::getJITInstanceContext(jitHandle);
}


}
