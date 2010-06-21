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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#include "IpfType.h"

namespace Jitrino {
namespace IPF {

bool ipfLogIsOn = false;
bool ipfVerifyIsOn = true;
bool ipfConstantFolding = true;

//============================================================================//
// IpfType
//============================================================================//

int16 IpfType::getSize(DataKind dataKind) { 
    
    switch(dataKind) {
        case DATA_BASE       : return  8;
        case DATA_MPTR       : return  8;
        case DATA_I8         : return  1;
        case DATA_U8         : return  1;
        case DATA_I16        : return  2;
        case DATA_U16        : return  2;
        case DATA_I32        : return  4;
        case DATA_U32        : return  4;
        case DATA_I64        : return  8;
        case DATA_U64        : return  8;
        case DATA_S          : return  4;
        case DATA_D          : return  8;
        case DATA_F          : return 16;
        case DATA_P          : return  1;
        case DATA_B          : return  8;
        case DATA_IMM        : return  8;
        case DATA_CONST_REF  : return  8;
        case DATA_NODE_REF   : return  8;
        case DATA_METHOD_REF : return  8;
        case DATA_SWITCH_REF : return  8;
        case DATA_INVALID    : break;
    }

    IPF_ERR << " unexpected dataKind " << dataKind << endl;
    return 0;
}

//----------------------------------------------------------------------------------------//

bool IpfType::isReg(OpndKind opndKind) { 

    switch(opndKind) {
        case OPND_G_REG   : 
        case OPND_F_REG   : 
        case OPND_P_REG   : 
        case OPND_B_REG   : 
        case OPND_A_REG   : 
        case OPND_IP_REG  : 
        case OPND_UM_REG  : return true;
        case OPND_IMM     : return false;
        case OPND_INVALID : break;
    }

    IPF_ERR << " unexpected opndKind " << opndKind << endl;
    return 0;
}
    
//----------------------------------------------------------------------------------------//

bool IpfType::isGReg(OpndKind opndKind) { 

    switch(opndKind) {
        case OPND_G_REG   : return true; 
        case OPND_F_REG   : 
        case OPND_P_REG   : 
        case OPND_B_REG   : 
        case OPND_A_REG   : 
        case OPND_IP_REG  : 
        case OPND_UM_REG  :
        case OPND_IMM     : return false;
        case OPND_INVALID : break;
    }

    IPF_ERR << " unexpected opndKind " << opndKind << endl;
    return 0;
}
    
//----------------------------------------------------------------------------------------//

bool IpfType::isFReg(OpndKind opndKind) { 

    switch(opndKind) {
        case OPND_G_REG   : return false; 
        case OPND_F_REG   : return true;
        case OPND_P_REG   : 
        case OPND_B_REG   : 
        case OPND_A_REG   : 
        case OPND_IP_REG  : 
        case OPND_UM_REG  :
        case OPND_IMM     : return false;
        case OPND_INVALID : break;
    }

    IPF_ERR << " unexpected opndKind " << opndKind << endl;
    return 0;
}
    
//----------------------------------------------------------------------------------------//

bool IpfType::isImm(OpndKind opndKind) { 

    switch(opndKind) {
        case OPND_G_REG   : 
        case OPND_F_REG   : 
        case OPND_P_REG   : 
        case OPND_B_REG   : 
        case OPND_A_REG   : 
        case OPND_IP_REG  : 
        case OPND_UM_REG  : return false;
        case OPND_IMM     : return true;
        case OPND_INVALID : break;
    }

    IPF_ERR << " unexpected opndKind " << opndKind << endl;
    return 0;
}
    
//----------------------------------------------------------------------------------------//

bool IpfType::isSigned(DataKind dataKind) { 
    
    switch(dataKind) {
        case DATA_I8  : 
        case DATA_I16 : 
        case DATA_I32 : 
        case DATA_I64 : 
        case DATA_S   : 
        case DATA_D   : 
        case DATA_F   : return true;
        default       : return false;;
    }
}

//----------------------------------------------------------------------------------------//

bool IpfType::isFloating(DataKind dataKind) { 
    
    switch(dataKind) {
        case DATA_S   :
        case DATA_D   :
        case DATA_F   : return true;
        default       : return false;;
    }
}

} // IPF
} // Jitrino
