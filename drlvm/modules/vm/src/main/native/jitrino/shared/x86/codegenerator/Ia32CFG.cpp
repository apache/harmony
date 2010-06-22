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
 * @author Vyacheslav P. Shakin, Mikhail Y. Fursov
 */

#include "Log.h"
#include "Ia32CFG.h"
#include "Ia32IRManager.h"

namespace Jitrino{
namespace Ia32 {

void CGNode::verify() {
}

void BasicBlock::verify() {
#ifdef _DEBUG
    CGNode::verify();
    Inst* last = (Inst*)getLastInst();
    if (last) {
        last->verify();
    } else {
        assert(getOutDegree()<=2);
        assert(getUnconditionalEdge()!=NULL || getExceptionEdge()!=NULL);
    }
#endif
}

void* BasicBlock::getCodeStartAddr() const  {
    return (U_8*)irm.getCodeStartAddr()+getCodeOffset(); 
}


}}; // namespace 
