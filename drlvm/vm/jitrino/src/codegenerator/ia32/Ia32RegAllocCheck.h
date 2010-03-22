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
 * @author Sergey L. Ivashin
 */

#if !defined(__IA32REGALLOCCHECK_H_INCLUDED__)
#define __IA32REGALLOCCHECK_H_INCLUDED__

#include "Ia32IRManager.h"


namespace Jitrino
{
namespace Ia32
{


//========================================================================================
// class RegAllocCheck
//========================================================================================
/**
 *  This class used for debug purposes;
 *
 *  (1) It checks that there are no two operands which are assigned to the same 
 *      regsiter and both are alive at some instruction.
 *
 *  (2) It checks that the operand assignment is consistent with the operand constraint.
 *
 *  (3) (if enabled by 'checkloc' argument) It checks that all operands are  assigned 
 *      to memory or register.
 *
 */

class RegAllocCheck 
{

public:
    
    RegAllocCheck (const IRManager& x)      :irm(x), mm("RegAllocCheck") {}

    bool run (bool checkloc);

protected:

    typedef U_32 RegMask;

    const IRManager& irm;
    MemoryManager     mm;
    size_t    opandcount;
    const Node*  bblock;
    const Node*  lastbb;
    bool headprinted;
    int  errors;

    void checkLiveness    ();
    void checkLocations   ();
    void checkConstraints ();
    ::std::ostream& error ();
    ::std::ostream& header ();
};


} //namespace Ia32
} //namespace Jitrino
#endif
