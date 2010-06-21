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
 * @author Intel, Pavel A. Ozhdikhin
 *
 */

#ifndef _ALIASANALYSIS_H_
#define _ALIASANALYSIS_H_

#include "open/types.h"
#include "MemoryAttribute.h"

namespace Jitrino {

class Opnd;
class Type;

class AliasAnalyzer
{
public:
    
    virtual ~AliasAnalyzer() {}

    /**
     * Return false if op1 and op2 cannot point to same location in memory.
     * Both op1 and op2 must be reference or pointer typed.  The method will
     * assert otherwise. 
     **/
    virtual bool mayAlias(Opnd* op1, Opnd* op2) = 0;

    /**
     * Return true if the object pointed to by op1 may escape from 
     * the method where it is defined, or is a global.
     **/

    virtual bool mayEscape(Opnd* op1) { return true; };
};

class TypeAliasAnalyzer : public AliasAnalyzer
{
public:
    virtual ~TypeAliasAnalyzer() {}

    bool mayAlias(Opnd* op1, Opnd* op2);

    bool mayAlias(Type* t1, Type* t2);

private:
    

    MemoryAttributeManager _mam;
};

} //namespace Jitrino 

#endif // _ALIASANALYSIS_H_
