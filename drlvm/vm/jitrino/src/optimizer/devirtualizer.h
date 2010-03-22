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

#ifndef _DEVIRTUALIZER_H_
#define _DEVIRTUALIZER_H_

#include "optpass.h"
#include "irmanager.h"

namespace Jitrino {

class Devirtualizer {
public:
    Devirtualizer(IRManager& irm, SessionAction* sa = NULL);

    void guardCallsInRegion(IRManager& irm, DominatorTree* tree);

    void unguardCallsInRegion(IRManager& irm);

    static bool isGuardableVirtualCall(Inst* inst, MethodInst*& methodInst, Opnd*& base, 
        Opnd* & tauNullChecked, Opnd*&tauTypesChecked, U_32 &argOffset, bool &isIntfCall);


private:
    void guardCallsInBlock(IRManager& irm, Node* node);
    void genGuardedDirectCall(IRManager& irm, Node* node, Inst* call, MethodDesc* methodDesc, ObjectType* valuedType, Opnd *tauNullChecked, Opnd *tauTypesChecked, U_32 argOffset);
    bool doGuard(IRManager& irm, Node* node, MethodDesc& methodDesc);
    ObjectType *getTopProfiledCalleeType(IRManager& irm, MethodDesc *origMethodDesc, Inst *call);

    bool _hasProfileInfo;
    bool _doProfileOnlyGuardedDevirtualization;
    bool _doAggressiveGuardedDevirtualization;
    bool _devirtSkipExceptionPath;
    float _devirtBlockHotnessMultiplier;
    bool _devirtSkipJLObjectMethods;
    bool _devirtInterfaceCalls;
    bool _devirtVirtualCalls;
    bool _devirtAbstractCalls;
    bool _devirtUsingProfile;

    //unguard pass params
    int _directCallPercent;
    int _directCallPercientOfEntry;

    TypeManager& _typeManager;
    InstFactory& _instFactory;
    OpndManager& _opndManager;
};

} //namespace Jitrino 

#endif //_DEVIRTUALIZER_H_
