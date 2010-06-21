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
 * @author Intel, George A. Timoshenko
 *
 */

#include <assert.h>

#include "open/types.h"
#include "MemoryManager.h"
#include "VMInterface.h"
#include "JavaTranslator.h"
#include "ByteCodeParser.h"
#include "JavaByteCodeTranslator.h"
#include "Log.h"
#include "FlowGraph.h"

namespace Jitrino {

void JavaTranslator::translateMethod(CompilationInterface& ci, MethodDesc& methodDesc, IRBuilder& irBuilder) {
    
    U_32 byteCodeSize = methodDesc.getByteCodeSize();
    const unsigned char* byteCodes = methodDesc.getByteCodes();
    MemoryManager  translatorMemManager("JavaTranslator::translateMethod.translatorMemManager");

    JavaFlowGraphBuilder cfgBuilder(irBuilder.getInstFactory()->getMemManager(),irBuilder);

    ByteCodeParser parser((const U_8*)byteCodes,byteCodeSize);
    // generate code
    JavaByteCodeTranslator translator(ci,
                              translatorMemManager,
                              irBuilder,
                              parser,
                              methodDesc, 
                              *irBuilder.getTypeManager(),
                              cfgBuilder);
                              // isInlined
    parser.parse(&translator);
    cfgBuilder.build();
}

} //namespace Jitrino 
