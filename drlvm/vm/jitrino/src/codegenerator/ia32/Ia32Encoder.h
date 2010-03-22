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

#ifndef _IA32_ENCODER_H_
#define _IA32_ENCODER_H_

#include "open/types.h"
#include "Stl.h"
#include "MemoryManager.h"
#include "Type.h"
#include "enc_base.h"
#include "Ia32Constraint.h"

namespace Jitrino {
namespace Ia32 {

class Inst;

class Encoder : public EncoderBase {
public:
    // legacy code - for compatibility 
    typedef OpndRolesDesc   OpndRolesDescription;

    /** 
     * struct OpcodeDescription contains the description of an opcode
     */
    struct OpcodeDescription {
        OpndRolesDescription    opndRoles;
        Constraint              opndConstraints[IRMaxNativeOpnds];
    };

    /** struct OpcodeGroupDescription contains the description of an opcode group */
    struct OpcodeGroup {
        Mnemonic                    mnemonic;
        U_32                      properties;
        OpndRolesDescription        opndRoles;
        Constraint                  opndConstraints[IRMaxNativeOpnds];
        OpndRolesDesc               implicitOpndRoles;
        RegName                     implicitOpndRegNames[3];
        U_32                      extendedToNativeMap[IRMaxExtendedOpnds];
        const char *                printMnemonic;
    };

    struct FindInfo {
        Mnemonic                        mnemonic;
        unsigned                        opndCount;
        unsigned                        defOpndCount;
        Constraint                      opndConstraints[IRMaxExtendedOpnds];
        bool                            isExtended;
        const OpcodeGroup * opcodeGroup;
    };

    static bool matches(Constraint co, Constraint ci, U_32 opndRoles, bool allowAliases);
    static const OpcodeGroup* findOpcodeGroup(const FindInfo& fi);
    static bool matches(const OpcodeGroup* og, const FindInfo& fi, bool any);
    static bool isOpndAllowed(const Encoder::OpcodeGroup * og, U_32 i, Constraint co, bool isExtended, bool any);

    /**
     * Retunrs an empty opcode group.
     */
    static const OpcodeGroup* getDummyOpcodeGroup(){ return &dummyOpcodeGroup; }
    /**
     * Inserts the executable bytes into the byte stream.
     */
    static U_8 * emit(U_8* stream, const Inst * inst);
    /** 
     * struct MemOpndConstraints represents an element of an array of
     * special memory operands. This array is referenced by the Constraint.getMemoryConstraintIndex()
     * The 0 element of the constraint contains a default constraint set for the common addressing method:
     * base+index*scale+displacement
    */
    struct MemOpndConstraints {
        Constraint constraints[MemOpndSubOpndKind_Count];
    };

    static const MemOpndConstraints * getMemOpndConstraints(U_32 idx)
    { assert(idx<lengthof(memOpndConstraints)); return memOpndConstraints+idx; }

    static Constraint getMemOpndSubOpndConstraint(Constraint memOpndConstraint, U_32 subOpndIndex) {
        return getMemOpndConstraints(0)->constraints[subOpndIndex];
    }

    /**
     * Returns a Constraint which describes all available registers of a given kind.
     * Currently, it only returns 0xFF for GP and XMM regs, and '1' (a single register)
     * for FP and Status registers.
     * For other kinds it returns empty Constraint.
     */
    static Constraint getAllRegs(OpndKind regKind);
    /**
     * Returns properties (see Inst::Properties) for a given mnemonic.
     */
    static U_32 getMnemonicProperties(Mnemonic mn);

    /**
     * Returns properties (see Inst::Properties) for a given MnemonicDesc.
     */
    static U_32 getMnemonicProperties(const MnemonicDesc& mdesc);
private:
    /**
     * Empty opcode group.
     */
    static const OpcodeGroup        dummyOpcodeGroup;
    static const MemOpndConstraints         memOpndConstraints[16];
    
    /**
     * Maximum number of groups per mnemonic. No arithmetics behind the 
     * number, just measured.
     */
    static const unsigned MAX_OPCODE_GROUPS = 20;
    /** 
     * Struct OpcodeGroupsHolder represents an item in
     * the array which maps mnemonics to a set of opcode group 
     * descriptions for the mnemonic.
     */
    struct OpcodeGroupsHolder {
        unsigned                count;
        OpcodeGroup             opgroups[MAX_OPCODE_GROUPS];
    };
    
    /**
     * Returns a table representing a mapping of a Mnemonic into 
     * its OpcodeGroups.
     */
    static const OpcodeGroupsHolder * getOpcodeGroups(void);
    /**
     * Initializes the mapping table.
     * Called once from the getOpcodeGroups() to initialize its 
     * static map.
     */
    static void initOpcodeGroups(OpcodeGroupsHolder * table);
    /**
     * A helper function for initOpcodeGroups(), used to initialize 
     * a single item of OpcodeGroupsHolder.
     */
    static void buildOGHolder(OpcodeGroupsHolder * mitem, 
                            const MnemonicDesc& mdesc, const OpcodeDesc * opcodes);

    /**
     * A helper function for buildHolder().
     */
    static void initOD(OpcodeDescription& od, const OpcodeDesc * opcode);
    /**
     * A helper function for buildHolder().
     */
    static void initOG(OpcodeGroup& og, 
                        const MnemonicDesc& mdesc);
    /**
     * A helper function for buildHolder().
     * Counts weak and strong constraints for the given group.
     */
    static void finalizeOG(OpcodeGroup& og);
};


}}; // namespace Jitrino::Ia32

#endif
