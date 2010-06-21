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

#include "aliasanalyzer.h"
#include "Type.h"
#include "Opnd.h"

namespace Jitrino {

bool
TypeAliasAnalyzer::mayAlias(Opnd* op1, Opnd* op2) {
    // If types and context are compatible, the two operands may alias.
    return mayAlias(op1->getType(), op2->getType()); 
}

bool
TypeAliasAnalyzer::mayAlias(Type* t1, Type* t2) {
    return Type::mayAliasPtr(t1, t2);
}

} //namespace Jitrino 
