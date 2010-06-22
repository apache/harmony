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
 * @author Vyacheslav P. Shakin
 */

#ifndef _IA32_CODE_GENERATOR_
#define _IA32_CODE_GENERATOR_

#include "CodeGenIntfc.h"
#include "Ia32IRManager.h"
#include "Ia32CodeGeneratorFlags.h"

namespace Jitrino
{
namespace Ia32{

//========================================================================================================
class MethodSplitter;



//========================================================================================================
// class CodeGenerator  -- main class for IA32 back end
//========================================================================================================
/** 
class CodeGenerator is the main class for IA32 back end.
It servers as a driver controlling the whole process of native code generation.
*/

class CodeGenerator : public ::Jitrino::CodeGenerator {
public:

    //-----------------------------------------------------------------------------------
    CodeGenerator(){};
    
    virtual ~CodeGenerator() {}
    
    virtual void genCode(::Jitrino::SessionAction* sa, ::Jitrino::MethodCodeSelector&);
};


}} // namespace Ia32
#endif // _IA32_CODE_GENERATOR
