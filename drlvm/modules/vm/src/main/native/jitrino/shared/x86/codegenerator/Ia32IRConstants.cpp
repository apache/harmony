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

#include "Ia32IRConstants.h"

namespace Jitrino
{
namespace Ia32{

///////////////////////////////////////////////////////////////////////////////////////////////

//_________________________________________________________________________________________________
ConditionMnemonic reverseConditionMnemonics[ConditionMnemonic_Count]=
{
    ConditionMnemonic_NO,
    ConditionMnemonic_O,
    ConditionMnemonic_NB,
    ConditionMnemonic_NAE,
    ConditionMnemonic_NZ,
    ConditionMnemonic_Z,
    ConditionMnemonic_NBE,
    ConditionMnemonic_NA,

    ConditionMnemonic_NS,
    ConditionMnemonic_S,
    ConditionMnemonic_NP,
    ConditionMnemonic_P,
    ConditionMnemonic_NL,
    ConditionMnemonic_NGE,
    ConditionMnemonic_NLE,
    ConditionMnemonic_NG,
};

//_________________________________________________________________________________________________
ConditionMnemonic swappedConditionMnemonics[ConditionMnemonic_Count]=
{
    ConditionMnemonic_NO,
    ConditionMnemonic_O,
    ConditionMnemonic_A,
    ConditionMnemonic_BE,
    ConditionMnemonic_Z,
    ConditionMnemonic_NZ,
    ConditionMnemonic_AE,
    ConditionMnemonic_B,

    ConditionMnemonic_NS,
    ConditionMnemonic_S,
    ConditionMnemonic_P,
    ConditionMnemonic_NP,
    ConditionMnemonic_G,
    ConditionMnemonic_LE,
    ConditionMnemonic_GE,
    ConditionMnemonic_L,
};

//_________________________________________________________________________________________________
Mnemonic            getBaseConditionMnemonic(Mnemonic mn)
{
    return 
        mn>=Mnemonic_Jcc && mn<Mnemonic_Jcc+16?Mnemonic_Jcc:
        mn>=Mnemonic_CMOVcc && mn<Mnemonic_CMOVcc+16?Mnemonic_CMOVcc:
        mn>=Mnemonic_SETcc && mn<Mnemonic_SETcc+16?Mnemonic_SETcc:
        Mnemonic_NULL;
}

//_________________________________________________________________________________________________
ConditionMnemonic   reverseConditionMnemonic(ConditionMnemonic cm)
{
    return reverseConditionMnemonics[cm];
}

//_________________________________________________________________________________________________
ConditionMnemonic   swapConditionMnemonic(ConditionMnemonic cm)
{
    return swappedConditionMnemonics[cm];
}

//_________________________________________________________________________________________________
U_32 countOnes(U_32 mask)
{
    U_32 count=0;
    for (U_32 m=1; m; m<<=1) if ((mask & m)!=0) count++;
    return count;
}

}}; // namespace Ia32

