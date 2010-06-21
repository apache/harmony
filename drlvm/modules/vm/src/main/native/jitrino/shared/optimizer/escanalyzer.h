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
 * @author Intel, Natalya V. Golovleva
 *
 */

#ifndef _ESCANALYSIS_H_
#define _ESCANALYSIS_H_

#include "Stl.h"
#include "optpass.h"
#include "FlowGraph.h"
#include "irmanager.h"
#include "mkernel.h"

namespace Jitrino {

/**
 * Performs escape analysis for the compiled method. 
 * Creates data connection graph, sets states (local, argument escaped, global escaped)
 * for objects created in compiled method, runs escape analysis related optimizations:
 *  - monitor elimination optimization;
 *  - scalar replacement optimization.
 * Saves information about method arguments and return value states for future usage. 
 */

class EscAnalyzer {

public:
/**
 * Creates EscAnalyzer class for compiled method. 
 * @param mm - memory manager,
 * @param argSource - session action,
 * @param irm - compiled method IR manager.
 */
    EscAnalyzer(MemoryManager& mm, SessionAction* argSource, IRManager& irm);
    
/**
 * Performs escape analysis and runs escape analysis related optimizations. 
 */
    void doAnalysis();

    // read from command line to output debug information
    U_32 allProps;

private:
/**
 * Creates EscAnalyzer class for callee method escape analysis. 
 * @param parent - parent (caller method) EscAnalyzer class
 * @param irm - compiled method IR manager.
 */
    EscAnalyzer(EscAnalyzer* parent, IRManager& irm);

    // initial level to start callee method escape analysis
    static const int maxMethodExamLevel_default = 0;

    // Connection Graph Structure
// CnG node types
    enum CnGNodeTypes {
        NT_OBJECT   = 8,  // Op_LdRef,Op_NewObj,Op_NewArray,Op_NewMultiArray    
        NT_DEFARG   = NT_OBJECT+1,  // formal parameter - Op_DefArg
        NT_RETVAL   = 16,           // Op_DirectCall,Op_IndirectMemoryCall-returned by method 
        NT_CATCHVAL = NT_RETVAL+1,  // catched value
        NT_LDOBJ    = 32,           // Op_LdConstant,Op_TauLdInd,Op_LdVar, 
                                                    // Op_TauStaticCast,Op_TauCast 
        NT_INTPTR   = NT_LDOBJ+1,   // Op_SaveRet
        NT_VARVAL   = NT_LDOBJ+2,   // Op_StVar,Op_Phi
        NT_ARRELEM  = NT_LDOBJ+3,   // Op_LdArrayBaseAddr,Op_AddScaledIndex
        NT_REF      = NT_LDOBJ+4,   // reference value - Op_LdFieldAddr,
                                                    // Op_LdStaticAddr, Op_TauCast, Op_TauStaticCast
        NT_STFLD    = 64,           // Op_LdStaticAddr
        NT_INSTFLD  = NT_STFLD+1,   // Op_LdFieldAddr
        NT_ACTARG   = 128,          // Op_DirectCall,Op_IndirectMemoryCall
        NT_EXITVAL  = 256,          // returned value - Op_Return
        NT_THRVAL   = NT_EXITVAL+1, // thrown value - Op_Throw
        NT_LDVAL    = 512,          // Op_TauLdInd, Op_TauStInd
        NT_OBJS     = NT_OBJECT|NT_RETVAL|NT_LDOBJ  //for findCnGNode_op
    };
// CnG node reference types
    static const U_32 NR_PRIM = 0;
    static const U_32 NR_REF = 1;
    static const U_32 NR_ARR = 2;
    static const U_32 NR_REFARR = 3;
// CnG edge types
    static const U_32 ET_POINT = 1;  // Op_TauLdInd (loaded value)
    static const U_32 ET_DEFER = 2;
    static const U_32 ET_FIELD = 3;  // Op_LdFieldAddr (object field), Op_AddScaledIndex
// CG node states
    static const U_32 GLOBAL_ESCAPE = 1;
    static const U_32 ARG_ESCAPE = 2;
    static const U_32 NO_ESCAPE = 3;
    static const U_32 ESC_MASK = 3;
    static const U_32 BIT_MASK = 56;
    static const U_32 LOOP_CREATED = 8;
    static const U_32 OUT_ESCAPED = 16;
    static const U_32 VIRTUAL_CALL = 32;


    struct CnGNode;
    struct CnGRef {
        CnGNode* cngNodeTo;
        U_32 edgeType;
        Inst* edgeInst;
    };

    typedef StlList<CnGRef*> CnGRefs;   
    typedef StlList<Inst*> Insts;
    typedef StlList<U_32> NodeMDs;    

    // connection graph node structure
    struct CnGNode {
        U_32 cngNodeId;   // CnG node id
        U_32 opndId;      // opnd id  (0 for NT_ACTARG) 
        void* refObj;       // MethodDesc* for NT_ACTARG, Inst* for fields, Opnd* for others
        U_32 nodeType;    // CnG node types
        U_32 nodeRefType; // CnG node reference types
        U_32 instrId;
        CnGNode* lNode;     // ldind from lNode for ldflda & ldsflda
        U_32 state;       // escape state
        NodeMDs* nodeMDs;   // list of NT_ACTARG nodes
        Inst* nInst;        // ref to inst 
        U_32 argNumber;   // number of arg for NT_DEFARG & NT_ACTARG (0 for others)
        CnGRefs* outEdges;  // cngNode out edges
    };

    struct MemberIdent {
        char* parentName;
        char* name;
        char* signature;
    };

    struct InstFld;
    typedef StlList<InstFld*> InstFlds; 

    struct InstFld {
        MemberIdent* fldIdent;
        U_32 state;
        InstFlds* instFlds;   // contained instance fields
    };

    struct ParamInfo {
        U_32 paramNumber;
        U_32 state;
        InstFlds* instFlds;   // contained instance fields
    }; 

    typedef StlList<ParamInfo*> ParamInfos; 

    // structure of saved by EA information about compiled method
    struct CalleeMethodInfo {
        MemberIdent* methodIdent;
        U_32 numberOfArgs;
        U_32 properties;     // native, final, virtual ...
        ParamInfos* paramInfos;
        U_32 retValueState;
        bool mon_on_this;
    };

    // list of saved by EA information about compiled methods
    typedef StlList<CalleeMethodInfo*> CalleeMethodInfos;

    // connection graph edge structure
    struct CnGEdge {
        CnGNode* cngNodeFrom;
        CnGRefs* refList;
    };

    // list of connection graph nodes definition
    typedef StlList<CnGNode*> CnGNodes; 
    // list of connection graph edges definition
    typedef StlList<CnGEdge*> CnGEdges; 
    // list of object Ids definition
    typedef StlList<U_32> ObjIds; 


    static CalleeMethodInfos* calleeMethodInfos;
    static Mutex calleeMethodInfosLock;
    
    // variables to read command line options
    const char* debug_method;
    bool do_sync_removal;
    bool do_sync_removal_vc;
    bool do_sync_removal_sm;
    bool do_scalar_repl;
    bool do_esc_scalar_repl;
    bool do_scalar_repl_only_final_fields_in_use;
    bool do_scalar_repl_final_fields;
    const char* execCountMultiplier_string;
    double ec_mult;
    bool print_scinfo;
    bool compressedReferencesArg; // for makeTauLdInd 

    TranslatorAction* translatorAction;

    MemoryManager& eaMemManager;
    IRManager& irManager;
    MethodDesc& mh;            // analyzed method header
    CompilationInterface &compInterface;

    // list of connection graph nodes
    CnGNodes* cngNodes;
    // list of connection graph edges
    CnGEdges* cngEdges;
    // list of instructions for edge creation
    Insts* exam2Insts;

    // maximum level to analyze callee methods (from command line or default set)
    U_32 maxMethodExamLevel;
    // set level for escape analysis
    U_32 method_ea_level;

    U_32 lastCnGNodeId;
    U_32 curMDNode;
    int defArgNumber;
    U_32 initNodeType;  // type of initial scanned node

    // lists to help do CFG scan
    ObjIds *scannedObjs;
    ObjIds *scannedObjsRev;
    ObjIds *scannedInsts;
    ObjIds *scannedSucNodes;

    // format 32 0 operand used by the optimizations
    SsaTmpOpnd* i32_0;
    // format 32 1 operand used by the optimizations
    SsaTmpOpnd* i32_1;

    // list of Op_MethodEnd instructions (for scalar replacement optimization)
    Insts* methodEndInsts;
    // list of Op_Branch and Op_TauCheckNull instructions (for scalar replacement optimization)
    Insts* checkInsts;

    bool shortLog;
    bool verboseLog;
    // common method for both EscAnalyzer constructors
    void init();
    // output escape analysis flags
    void showFlags(std::ostream& os);

/**
 * Scans specified CFG node instructions to create CnG nodes.
 * Adds some instructions to exam2Instrs list to create CnG edges later.
 * @param node - control flow graph node.
 */
    void instrExam(Node* node);

/**
 * Scans instructions from exam2Instrs list to create CnG edges. 
 */
    void instrExam2();

/**
 * Creates new CnG node for specified instruction and adds it to cngNodes list. 
 * @param inst - instruction,
 * @param type - operand type,
 * @param ntype - CnG node type.
 * @return created CnGnode. 
 */
    CnGNode* addCnGNode(Inst* inst, Type* type, U_32 ntype);

/**
 * Creates new CnG node for specified instruction dst operand. 
 * @param inst - instruction,
 * @param type - operand type,
 * @param ntype - CnG node type.
 * @return created CnGnode. 
 */
    CnGNode* addCnGNode_op(Inst* inst, Type* type, U_32 ntype);

/**
 * Creates new CnG node for specified call instruction argument. 
 * @param inst - instruction,
 * @param mpt - callee method description,
 * @param ntype - CnG node type,
 * @param narg - method argument.
 * @return created CnGnode. 
 */
    CnGNode* addCnGNode_mp(Inst* inst, MethodPtrType* mpt, U_32 ntype, U_32 narg);

/**
 * Creates new CnG node for specified return or throw instruction. 
 * @param inst - instruction,
 * @param ntype - CnG node type.
 * @return created CnGnode. 
 */
    CnGNode* addCnGNode_ex(Inst* inst, U_32 ntype);

/**
 * Creates new field CnG node for specified instruction. 
 * @param inst - instruction,
 * @param ntype - CnG node type.
 * @return created CnGnode. 
 */
    CnGNode* addCnGNode_fl(Inst* inst, U_32 ntype);

/**
 * Finds CnG node for specified operand Id. 
 * @param nId - operand Id.
 * @return found CnGnode, or <code>NULL</code>. 
 */
    CnGNode* findCnGNode_op(U_32 nId);

/**
 * Finds CnG node for specified CnG node Id. 
 * @param nId - CnG node Id.
 * @return found CnGnode, or <code>NULL</code>. 
 */
    CnGNode* findCnGNode_id(U_32 nId);

/**
 * Finds CnG node for specified instruction Id. 
 * @param nId - instruction Id.
 * @return found CnGnode, or <code>NULL</code>. 
 */
    CnGNode* findCnGNode_in(U_32 nId);

/**
 * Finds NT_ACTARG CnG node for specified instruction Id and argument number. 
 * @param iId - instruction Id,
 * @param aId - argument number.
 * @return found CnGnode, or <code>NULL</code>. 
 */
    CnGNode* findCnGNode_mp(U_32 iId, U_32 aId);

/**
 * Finds field CnG node for specified instruction and CnG node type. 
 * @param inst - instruction,
 * @param ntype - CnG node type.
 * @return found CnGnode, or <code>NULL</code>. 
 */
    CnGNode* findCnGNode_fl(Inst* inst, U_32 ntype);

/**
 * Creates new CnG edge for specified instruction, adds it to cngEdges and
 * cgnfrom->outEdges lists.
 * @param cgnfrom - CnG node from,
 * @param cgnto - CnG node to,
 * @param etype - edge type,
 * @param inst - instruction.
 * @return created CnGnode. 
 */
    void addEdge(CnGNode* cgnfrom, CnGNode* cgnto, U_32 etype, Inst* inst);

/**
 * Sets escape state for method created objects.
 */
    void setCreatedObjectStates();

/**
 * Scans CnG nodes beginning with specified node to set 
 * GLOBAL_ESCAPE or VIRTUAL_CALL states.
 * @param cgnfrom - CnG node to begin scan,
 * @param check_var_src - sign to check variable operands.
 */
    void scanCnGNodeRefsGE(CnGNode* cgn, bool check_var_src);

/**
 * Scans CnG nodes beginning with specified node to set 
 * ARG_ESCAPE states.
 * @param cgnfrom - CnG node to begin scan,
 * @param check_var_src - sign to check variable operands.
 */
    void scanCnGNodeRefsAE(CnGNode* cgn, bool check_var_src);

/**
 * Scans connection graph nodes beginning with specified not global escaped node to set 
 * corresponding state to array elements and fields values.
 * @param node - connection graph node to begin scan.
 */
    void checkSubobjectStates(CnGNode* node);

/**
 * Finds specified method escape analysis information in the common repository and compiles 
 * the method if it is required and possible. 
 * @param md - method description,
 * @param inst - method call instruction.
 * @return method info, if it was found, NULL - otherwise.
 */
    CalleeMethodInfo* findMethodInfo(MethodDesc* md,Inst* inst);

/**
 * Finds specified method escape analysis information in the common repository. 
 * @param ch1 - method package name,
 * @param ch2 - method name,
 * @param ch3 - method signature.
 * @return method info, if it was found, NULL - otherwise.
 */
    CalleeMethodInfo* getMethodInfo(const char* ch1,const char* ch2,const char* ch3);

/**
 * Runs escape analysis for direct call method. 
 * @param call - direct call instruction.
 */
    void scanCalleeMethod(Inst* call);

/**
 * Runs translator session for callee method. 
 * @param inlineCC - compilation interface.
 */
    void runTranslatorSession(CompilationContext& inlineCC);

/**
 * Runs optimizations for callee method. 
 * @param irManager - IR manager.
 */
    void optimizeTranslatedCode(IRManager& irManager);

/**
 * Saves escape analysis method information into common repository.
 */
    void saveScannedMethodInfo();

/**
 * Returns specified method parameter state. 
 * @param mi - escape analysis method information,
 * @param np - parameter number.
 * @return specified parameter state.
 */
    U_32 getMethodParamState(CalleeMethodInfo* mi, U_32 np);

/**
 * Collects instructions creating objects that are not GLOBAL_ESCAPE. 
 */
    void markNotEscInsts();

/**
 * Performs variable operands fixup after done optimizations. 
 * @param irManager - IR manager.
 */
    void eaFixupVars(IRManager& irm);

/**
 * Outputs connection graph nodes information into the specified log. 
 * @param text - output information naming,
 * @param os - log.
 */
    void printCnGNodes(const char* text,::std::ostream& os);

/**
 * Outputs connection graph node information into the specified log. 
 * @param cgn - connection graph node,
 * @param os - log.
 */
    void printCnGNode(CnGNode* cgn,::std::ostream& os);

/**
* Outputs method name information into the specified log. 
* @param inst - call instruction,
* @param os - log stream
*/
void printCallMethodName(Inst* inst, ::std::ostream& os);

/**
 * Creates string representing CnG node type. 
 * @param cgn - connection graph node.
 * @return srting representing CnG node type.
 */
    std::string nodeTypeToString(CnGNode* cgn);

/**
 * Outputs connection graph edges information into the specified log. 
 * @param text - output information naming,
 * @param os - log.
 */
    void printCnGEdges(const char* text,::std::ostream& os); 

/**
 * Creates string representing CnG edge type. 
 * @param cgn - connection graph node.
 * @return srting representing CnG edge type.
 */
    std::string edgeTypeToString(CnGRef* edr);

/**
 * Outputs information about reference objects created in analyzed method into the specified log. 
 * @param os - log.
 */
    void printRefInfo(::std::ostream& os); 

/**
 * Outputs information about CnG node with all nodes it refers to. 
 * @param cgn - connection graph node,
 * @param text - text printed before CnG node information,
 * @param os - log.
 */
    void printCnGNodeRefs(CnGNode* cgn, std::string text,::std::ostream& os);

/**
 * Outputs origin operands of specified instruction operands that aren't tau operand
 * into the specified log.
 * @param inst - instruction,
 * @param text - text to print before the instruction,
 * @param os - log.
 */
    void lObjectHistory(Inst* inst,std::string text,::std::ostream& os);

/**
 * Outputs information about objects created in analyzed method into the specified log. 
 * @param os - log.
 */
    void printCreatedObjectsInfo(::std::ostream& os);

/**
 * Outputs common information about created objects into log file. 
 */
    void createdObjectInfo();

/**
 * Prints origin operands of specified instruction.
 * @param inst - specified instruction,
 * @param all - if <code>true</code> for all source operands of the instruction,
 *              if <code>false</code> for main source operand of some instructions,
 * @param text - text to print before the instruction.
 */
    void printOriginObjects(Inst* inst, bool all, std::string text="  ");

/**
 * Prints escape analysis method information for specified parameter.
 * @param mi - method info from common repository.
 */
    void printMethodInfo(CalleeMethodInfo* mi);

/**
 * Outputs information about instruction kind into the specified log.
 * @param inst - instruction,
 * @param os - log.
 */
    void what_inst(Inst* inst,::std::ostream& os); 

/**
 * Outputs debug information about the specified type into the specified log.
 * @param type - reference type,
 * @param os - log.
 */
    void ref_type_info(Type* type,::std::ostream& os);

/**
 * Outputs debug information about the specified instruction into the specified log.
 * @param inst - instruction,
 * @param os - log.
 */
    void debug_inst_info(Inst* inst,::std::ostream& os);

/**
 * Outputs debug information about the specified operand into the specified log.
 * @param opnd - operand,
 * @param os - log.
 */
    void debug_opnd_info(Opnd* opnd,::std::ostream& os);


    bool checkScanned(ObjIds* ids, U_32 id) {
        ObjIds::iterator it;
        if (ids == NULL) {
            return false;
        }
        for (it = ids->begin( ); it != ids->end( ); it++ ) {
            if ((*it)==id) {
                return true;
            }
        }
        return false;
    }
    bool checkScannedObjs(U_32 id) {return checkScanned(scannedObjs, id);}
    bool checkScannedObjsRev(U_32 id) {return checkScanned(scannedObjsRev, id);}
    bool checkScannedInsts(U_32 id) {return checkScanned(scannedInsts, id);}
    bool checkScannedSucNodes(U_32 id) {return checkScanned(scannedSucNodes, id);}

    U_32 getEscState(CnGNode* n) {
        return (n->state)&ESC_MASK;
    }
    void setEscState(CnGNode* n, U_32 st) {
        n->state = ((n->state)&BIT_MASK)+st;
    }
    U_32 getFullState(CnGNode* n) {
        return n->state;
    }
    void setFullState(CnGNode* n, U_32 st) {
        n->state = st;
    }
    U_32 getLoopCreated(CnGNode* n) {
        return (n->state)&LOOP_CREATED;
    }
    void setLoopCreated(CnGNode* n) {
        n->state = n->state|LOOP_CREATED;
    }
    void remLoopCreated(CnGNode* n) {
        n->state = (n->state|LOOP_CREATED)^LOOP_CREATED;
    }
    U_32 getOutEscaped(CnGNode* n) {
        return (n->state)&OUT_ESCAPED;
    }
    void setOutEscaped(CnGNode* n) {
        n->state = n->state|OUT_ESCAPED;
    }
    void remOutEscaped(CnGNode* n) {
        n->state = (n->state|OUT_ESCAPED)^OUT_ESCAPED;
    }
    U_32 getVirtualCall(CnGNode* n) {
        return (n->state)&VIRTUAL_CALL;
    }
    void setVirtualCall(CnGNode* n) {
        n->state = n->state|VIRTUAL_CALL;
    }
    void remVirtualCall(CnGNode* n) {
        n->state = (n->state|VIRTUAL_CALL)^VIRTUAL_CALL;
    }
    void printState(CnGNode* n,::std::ostream& os=Log::out()) {
        os << getEscState(n) << " (" << (getFullState(n)>>3) << ")";
    }
    void printState(U_32 st,::std::ostream& os=Log::out()) {
        os << (st&ESC_MASK) << " (" << (st>>3) << ")";
    }
    bool isGlobalState(U_32 state) {
        if ((state&ESC_MASK)==GLOBAL_ESCAPE||(state&VIRTUAL_CALL)!=0)
            return true;
        return false;
    }
    bool isStateNeedGEFix(U_32 state, U_32 nType) {
        if ((state&ESC_MASK)==GLOBAL_ESCAPE)
            return false;
        if ((state&OUT_ESCAPED)!=0 && initNodeType != NT_STFLD)
            return false;
        if ((state&OUT_ESCAPED)!=0 && initNodeType == NT_DEFARG && nType != NT_DEFARG)
            return false;
        return true;
    }

// Monitors elimination optimization

    struct MonUnit {
        U_32 opndId;
        Insts* monInsts;
        Insts* icallInsts;
    };
    typedef StlList<MonUnit*> MonInstUnits; 
    // list to collect info about monitor instruction operands
    MonInstUnits* monitorInstUnits ;

/**
 * Adds monitor instruction to monitorInstUnits. 
 * @param inst - monitor instruction.
 */
    void addMonInst(Inst* inst);

/**
 * Finds opndId specified monitor unit in monitorInstUnits. 
 * @param opndId - monitor instruction operand Id.
 * @return <code>MonUnit</code>, if found, or <code>NULL</code> otherwise. 
 */
    MonUnit* findMonUnit(U_32 opndId);

/**
 * Adds call instruction to the specified monitor unit. 
 * @param mu - monitor unit,
 * @param inst - call instruction.
 */
    void addMonUnitVCall(MonUnit* mu, Inst* inst); 

/**
 * Checks, that method contains monitor instructions with parameter 
 * which is <code>this</code> or subobject of this.
 * @return <code>true</code>, if such monitors exist, or <code>false</code> otherwise. 
 */
    bool checkMonitorsOnThis();

/**
 * Performs monitors elimination optimization. 
 */
    void scanSyncInsts();

/**
 * Marks (in CFG nodes bitset) nodes that are 'locked' by monitor instructions. 
 * @param bs - CFG nodes bitset,
 * @param syncInsts - list of monitor instruction for the same operand.
 */
    void markLockedNodes(BitSet* bs, Insts* syncInsts);

 /**
 * Marks (in CFG nodes bitset) nodes that are 'locked' by monitor instructions. 
 * @param node - next CFG node to search monexit instruction,
 * @param bs - CFG nodes bitset,
 * @param moninstop - monitor instruction operand,
 * @return <code>true</code>, if such monexit was found, or <code>false</code> otherwise. 
 */
    bool markLockedNodes2(Node* node, BitSet* bs, Opnd* moninstop);

/**
 * Checks instruction source operand states for specified instruction. 
 * @param inst - specified instruction,
 * @param st - instruction target operand state.
 * @return <code>GLOBAL_ESCAPE</code>, if any of source operand state is <code>GLOBAL_ESCAPE</code>,
 *         <code>ARG_ESCAPE</code>, if any of source operand state or st is <code>ARG_ESCAPE</code>,
 *         <code>NO_ESCAPE</code> otherwise.
 */
    U_32 checkState(Inst* inst,U_32 st);

/**
 * Checks if CnGNode operand is GLOBAL_ESCAPED while monitor instructions are executed. 
 * @param node - specified operand CnGNode,
 * @param syncInsts - list of monitor instructions for specified operand.
 * @return <code>true</code>, specified operand is global, when monitor instruction are executed,
 *         <code>false</code>, otherwise.
 */
    bool checkSencEscState(CnGNode* node,Insts* syncInsts);

/**
 * Collects all reachable in FlowGraph nodes from the specified node to scannedSucNodes list.
 * @param node - FlowGraph node,
 * @param syncInsts - instruction target operand state.
 */
    void collectSuccessors(Node* node);

/**
 * Inserts flag for specified monitor unit. Flag is set to 0 after operand creation instruction.
 * Flag is set to 1 before call instruction from vcInsts list.
 * Inserts flag check before monitor instruction from monInsts list.
 * If flag is equal to 0, monitor instruction isn't executed.
 * @param mu - monitor unit,
 * @param bs - CFG nodes bitset (with marked nodes for specified monitor unit).
 */
    void fixMonitorInstsVCalls(MonUnit* mu, BitSet* bs);

/**
 * Inserts flag check before monitor instruction.
 * If flag = 0 monitor instruction isn't executed.
 * @param syncInsts - monitor instruction list,
 * @param chk - value to check with (0 or 1),
 * @param muflag - monitor flag operand (VarOpnd* or SsaTmpOpnd*).
 */
    void insertFlagCheck(Insts* syncInsts, Opnd* muflag, U_32 chk);

/**
 * Removes monitor instructions from the specified monitor instruction list.
 * @param syncInsts - monitor instruction list.
 */
    void removeMonitorInsts(Insts* syncInsts);

/**
 * Removes flow graph node.
 * @param node - flow graph node.
 */
    void removeNode(Node* node);

/**
 * Performs monitors elimination optimization for specified monitor instructions with 
 * method <code>this</code> argument.
 * @param syncInsts - monitor instruction list.
 */
    void fixSyncMethodMonitorInsts(Insts* syncInsts);

/**
 * Inserts ReadJitHelperCall instruction to read state of method <code>this</code> argument
 * after first instruction in entry block.
 * @return <code>this</code> argument state 
 *              0 - <code>this</code> argument is thread local, 
 *              1 - <code>this</code> argument is thread global.
 */
    Opnd* insertReadJitHelperCall();

/**
 * Checks <code>this</code> argument of direct call synchronized methods.
 * If the argument doesn't escape callee method, and actual argument isn't global,
 * calls 'insertSaveJitHelperCall' to transfer <code>this</code> argument state 
 * to callee method.
 */
    void checkCallSyncMethod();

/**
 * Inserts SaveJitHelperCall instruction to store state of method <code>this</code> argument
 * before the specified instruction.
 * @param inst_before - instruction to insert SaveJitHelperCall before,
 * @param stVal - stored value.
 */
    void insertSaveJitHelperCall(Inst* inst_before, SsaTmpOpnd* stVal);

/**
 * Creates i32_0 or i32_1 SsaTmpOpnd (in accordance with value: 0 or 1)
 * if it wasn't created before.
 * Inserts ldc0 or ldc1 instruction after first instruction in entry Node, 
 * if SsaTmpOpnd was created.
 * @param value - 0 or 1.
 * @return  i32_0, if value = 0
 *          i32_1, if value = 1
 */
    SsaTmpOpnd* insertLdConst(U_32 value);



// Scalar replacement optimization

    struct ScObjFld {
        VarOpnd* fldVarOpnd;
        Insts* ls_insts;
        FieldDesc* fd;
        bool isFinalFld;
    };
    // list to collect scalarized object candidates
    typedef StlList<ScObjFld*> ScObjFlds; 

    // output stream for scalar replacement optimization
    std::ostream& os_sc;

/**
 * Performs scalar replacement optimization for local objects 
 * (class instances and arrays).
 */
    void scanLocalObjects();

/**
 * Performs scalar replacement optimization for method escaped class instances.
 */
    void scanEscapedObjects();

/**
 * Performs scalar replacement optimization for local objects from the specified list.
 * @param loids - list of local objects CnG nodes Ids,
 */
    void doLOScalarReplacement(ObjIds* loids);

/**
 * Performs scalar replacement optimization for method escaped objects from the specified list.
 * @param loids - list of optimized objects CnG nodes Ids.
 */
    void doEOScalarReplacement(ObjIds* loids);

/**
 * Collects (using connection graph) information of onode object fields usage.
 * @param onode - connection graph node fields usage of which is collected
 * @param scObjFlds - list to collect onode field's usage.
 */
    void collectStLdInsts(CnGNode* onode, ScObjFlds* scObjFlds);

/**
 * Collects (using connection graph) call instructions which use optimized object 
 * as a parameter.
 * @param n - optimized object connection graph node Id,
 * @param vc_insts - list of call instructions,
 * @param vcids - list of call instructions ids.
 */
    void collectCallInsts(U_32 n, Insts* vc_insts, ObjIds* vcids);

/**
 * Performs scalar replacement optimization for optimized object field usage.
 * @param scfld - optimized object scalarizable field.
 */
    void scalarizeOFldUsage(ScObjFld* scfld);

/**
 * Checks if an object from the specified list can be removed and its fields/elements scalarized.
 * If an object cannot be optimized it is removed from the list.
 * @param lnoids - list of new object CnG nodes Ids,
 * @param lloids - list of load object CnG nodes Ids,
 * @param check_loc - if <code>true</code> checks for local objects,
 *                    if <code>false</code> checks for virtual call escaped objects.
 */
    void checkOpndUsage(ObjIds* lnoids, ObjIds* lloids, bool check_loc);

/**
 * Checks if a tau operand may be removed/replaced.
 * @param tau_inst - object CnG nodes Ids.
 * @return <code>true</code> if tau operand may be removed; 
 *         <code>false<code> otherwise.
 */
    bool checkTauOpnd(Inst* tau_inst);

/**
 * Checks if an object can be removed and its fields/elements scalarized.
 * @param lobjid - object CnG nodes Ids.
 * @return <code>true</code> if an object is used only in ldflda or ldbase instructions; 
 *         <code>false<code> otherwise.
 */
    bool checkOpndUsage(U_32 lobjid);

/**
 * Performs checks for CnGNode operand using connection graph.
 * @param scnode - CnG node of optimized operand
 * @param check_loc - if <code>true</code> checks for local objects,
 *                    if <code>false</code> checks for method call escaped objects.
 * @return CnGNode* for operand that may be optimized; 
 *         <code>NULL<code> otherwise.
 */
    CnGNode* checkCnGtoScalarize(CnGNode* scnode, bool check_loc);

/**
 * Checks if there is a path in CFG from node where object is created by a nob_inst instruction
 * to EXIT node and this object is not escaped to any method call on this path.
 * @param nob_inst - object creation instruction.
 * @return <code>execCount</code> of this path execution; 
 *         <code>0<code> otherwise.
 */
    double checkLocalPath(Inst* nob_inst);

/**
 * Checks if there is a path in CFG from node where object created by a nob_inst instruction
 * to EXIT node and this object is not escaped to any method call.
 * @param n - CFG node to scan,
 * @param obId - escaped optimized object Id,
 * @param cExecCount - current execCount.
 * @return <code>execCount</code> the most value of <code>execCount</code> and 
 *                                checkNextNodes execution for next after n node.
 */
    double checkNextNodes(Node* n, U_32 obId, double cExecCount);

/**
 * Checks flag and creates object before call instruction (if it was not created yet).
 * @param vc_insts    - list of call instructions optimized object is escaped to,
 * @param objs        - list of optimized object fields,
 * @param ob_var_opnd -  varOpnd replacing optimized object,
 * @param oid         - escaped optimized object Id.
 */
    void restoreEOCreation(Insts* vc_insts, ScObjFlds* objs, VarOpnd* ob_var_opnd, U_32 oid);

/**
 * Removes specified instruction from ControlFlowGraph.
 * If instruction can throw exception removes corresponding CFGEdge.
 * @param reminst - removed instruction.
 */
    void removeInst(Inst* reminst);

/**
 * Replaces first source operand of Op_MethodEnd instruction by NULL
 * for scalar replacement optimized object.
 * @param ob_id - optimized object Id.
 */
    void fixMethodEndInsts(U_32 ob_id);

/**
 * Finds (using connection graph) load varOpnd that should be optimized with 
 * new object operand.
 * @param vval - CnG node of target stvar instruction varOpnd.
 * @return CnGNode* - found optimized load varOpnd CnG node
 *         <code>NULL</code> otherwise.
 */
    CnGNode* getLObj(CnGNode* vval);

/**
 * Checks that all sources of optimized load varOpnd aren't null and
 * satisfy to specified conditions.
 * @param inst - ldvar instruction created optimized load varOpnd.
 * @return <code>true</code> if satisfied; 
 *         <code>false<code> otherwise.
 */
    bool checkVVarSrcs(Inst* inst);

/**
 * Checks that optimized object type satisfied to specified types.
 * @param otn - object type name.
 * @return <code>true</code> if satisfied; 
 *         <code>false<code> otherwise.
 */
    bool checkObjectType(const char* otn);

/**
 * Checks that all load varOpnd fields are in new object field usage list.
 * @param nscObjFlds - list of used fields of optimized new object,
 * @param lscObjFlds - list of used fields of optimized load varOpnd.
 * @return <code>true</code> if list of new object used field contains all 
 *                           load varOpnd used field; 
 *         <code>false<code> otherwise.
 */
    bool checkObjFlds(ScObjFlds* nscObjFlds, ScObjFlds* lscObjFlds);

/**
 * Removes check instructions for optimized load varOpnd.
 * @param ob_id - optimized load variable operand Id.
 */
    void fixCheckInsts(U_32 opId);

/**
 * Checks (using connection graph) if CnGNode operand has final fields and scalarizes them.
 * @param onode - CnG node of optimized operand,
 * @param scObjFlds - list to collect onode operand field usage.
 */
    void checkToScalarizeFinalFiels(CnGNode* onode, ScObjFlds* scObjFlds);


    void instrExam_processLdFieldAddr(Inst* inst);
    void instrExam2_processLdFieldAddr(Inst* inst);
    static void getLdFieldAddrInfo(Inst* inst, Type*& type, U_32& nType);

};

} //namespace Jitrino 

#endif // _ESCANALYSIS_H_
