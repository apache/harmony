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

#ifndef _IA32_CODE_GENERATORFLAGS_
#define _IA32_CODE_GENERATORFLAGS_

namespace Jitrino
{
    namespace Ia32{

//-----------------------------------------------------------------------------------
/** struct Flags is a bit set defining various options controling 
the Ia32 code generator

These flags can be set directly in the code of from the VM command line
if VM supports this
*/
struct CGFlags {
    CGFlags () {
        earlyDCEOn = false;
    }
    bool    earlyDCEOn;
};
}} //namespace
#endif
