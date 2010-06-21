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

#include "IpfIrPrinter.h"
#include "CompilationContext.h"
#include "PMFAction.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// Compare two opnds by id value
//========================================================================================//

bool greaterOpnd (Opnd *o1, Opnd *o2) { return o1->getId() > o2->getId(); }
 
//========================================================================================//
// IrPrinter
//========================================================================================//

IrPrinter::IrPrinter(Cfg &cfg_) :
    mm(cfg_.getMM()),
    cfg(cfg_),
    ofs(NULL) {
    
    if (Log::isEnabled() == false) return;

    CompilationContext *cc        = CompilationContext::getCurrentContext();
    SessionAction      *session   = cc->getCurrentSessionAction();
    LogStream          &logStream = session->log(LogStream::CT);
    strcpy(logDir, logStream.getFileName());
    int len = strlen(logDir);
    logDir[len - 6] = 0;
}

//----------------------------------------------------------------------------------------//

void IrPrinter::printLayoutDot(char *logFile) {

    char logName[500];
    strcpy(logName, logDir);
    strcat(logName, logFile);
    ofs = new(mm) ofstream(logName);
    
    BbNode *node = (BbNode *)cfg.getEnterNode();
    BbNode *succ = node->getLayoutSucc();
    
    printHead();
    while(succ != NULL) {
        printNodeDot(node);
        *ofs << "  "   << node->getId();
        *ofs << " -> " << succ->getId() << endl;
        node = succ;
        succ = node->getLayoutSucc();
    }

    printNodeDot(node);
    printTail();
    ofs->close();
}

//----------------------------------------------------------------------------------------//

void IrPrinter::printCfgDot(char *logFile) {

    char logName[500];
    strcpy(logName, logDir);
    strcat(logName, logFile);
    ofs = new(mm) ofstream(logName);
    
    NodeVector &nodes = cfg.search(SEARCH_POST_ORDER);

    printHead();
    for(U_32 i=0; i<nodes.size(); i++) {
        printNodeDot(nodes[i]);
        EdgeVector &edges = nodes[i]->getOutEdges();
        for(U_32 j=0; j<edges.size(); j++) {
            printEdgeDot(edges[j]);
        }
    }
    printTail();
    ofs->close();
}

//----------------------------------------------------------------------------------------//

void IrPrinter::printAsm(ostream &os_) {
    
    os = &os_;
    
    BbNode *node = cfg.getEnterNode();
    
    *os << endl;
    *os << "----------- Code dump ----------------------------------------" << endl;
    while(node != NULL) {
        printNodeAsm(node);
        node = node->getLayoutSucc();
    }
}

//----------------------------------------------------------------------------------------//
// Dot file printing
//----------------------------------------------------------------------------------------//

void IrPrinter::printEdgeDot(Edge *edge) {

    *ofs << "  "   << edge->getSource()->getId();
    *ofs << " -> " << edge->getTarget()->getId();
    switch(edge->getEdgeKind()) { 
        case EDGE_EXCEPTION : *ofs << "[color=red";  break;
        case EDGE_DISPATCH  : *ofs << "[color=green"; break;
        case EDGE_BRANCH    : *ofs << "[color=blue"; break;
        case EDGE_THROUGH   : *ofs << "[color=black"; break;
        default             : *ofs << "[color=yellow";
    }

    *ofs << ",label=\"" << edge->getProb() << "\"];" << ::std::endl;
}

//----------------------------------------------------------------------------------------//

void IrPrinter::printNodeDot(Node *node) {

    *ofs << "  " << node->getId();
    *ofs << " [label=\"{";

    switch(node->getNodeKind()) {
        case NODE_BB       : *ofs << "BB"       << node->getId(); break;
        case NODE_DISPATCH : *ofs << "Dispatch" << node->getId(); break;
        case NODE_UNWIND   : *ofs << "Unwind"   << node->getId(); break;
        default            : *ofs << "Invalid"  << node->getId(); break;;
    }

    Node *loopHeader = node->getLoopHeader();
    if (loopHeader != NULL) {
        *ofs << "(" << loopHeader->getId() << ")";
    }

    if(node->getNodeKind() == NODE_BB) {
        InstVector &insts = ((BbNode *)node)->getInsts();
        for(U_32 i=0; i<insts.size(); i++) {
            *ofs << "\\n" << insts[i]->getInstMnemonic();
            CompVector &compList = insts[i]->getComps();
            for(uint16 j=0; j<compList.size(); j++) *ofs << insts[i]->getCompMnemonic(compList[j]);
        }
    }

    *ofs << "}\"];" << endl;
}

//----------------------------------------------------------------------------------------//

void IrPrinter::printHead() {

    *ofs << "digraph dotgraph {" << ::std::endl;
    *ofs << "  center=TRUE;" << ::std::endl;
    *ofs << "  margin=\".2,.2\";" << ::std::endl;
    *ofs << "  ranksep=\".25\";" << ::std::endl;
    *ofs << "  nodesep=\".20\";" << ::std::endl;
    *ofs << "  page=\"20,20\";" << ::std::endl;
    *ofs << "  ratio=auto;" << ::std::endl;
    *ofs << "  fontpath=\"c:\\winnt\\fonts\";" << ::std::endl;
    *ofs << "  node [shape=record,fontname=\"Courier\",fontsize=9];" << ::std::endl;
    *ofs << "  edge [fontname=\"Courier\",fontsize=9];" << ::std::endl;
}

//----------------------------------------------------------------------------------------//

void IrPrinter::printTail() {
    *ofs << "}" << ::std::endl;
}

//----------------------------------------------------------------------------------------//
// Asm file printing
//----------------------------------------------------------------------------------------//

void IrPrinter::printNodeAsm(BbNode *node) {

    *os << ".L" << node->getId() << ":" << endl;

    InstVector &insts = ((BbNode *)node)->getInsts();
    for(U_32 i=0; i<insts.size(); i++) *os << "  " << toString(insts[i]) << endl;
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(Inst *inst) {

    OpndVector &opnds    = inst->getOpnds();
    uint16     numDst    = inst->getNumDst();
    CompVector &compList = inst->getComps();
    RegOpnd    *qp       = (RegOpnd *)opnds[0];

    ostringstream oss;
    oss << "(" << toString(qp) << ")";

    while(oss.tellp() < 9) oss << " ";
    oss << inst->getInstMnemonic();        // print instruction mnemonic
    for(uint16 i=0; i<compList.size(); i++) oss << inst->getCompMnemonic(compList[i]);
    oss << " ";
    while(oss.tellp() < 21) oss << " ";

    InstCode icode = inst->getInstCode();
    bool lddone = false;
    if (icode>=INST_ST_FIRST && icode<=INST_ST_LAST) {
        oss << "[";
    }
    uint16 i = 1;                           // The first opnd is qp. It has been printed already
    while(i < numDst+1) { 
        oss << toString(opnds[i]); 
        if(++i >= numDst+1) break;
        oss << ", ";
    }
    if (icode>=INST_ST_FIRST && icode<=INST_ST_LAST) {
        oss << toString(opnds[i]); 
        i++;
        oss << "]";
    }

    if(i>1 && i<opnds.size()) {            // if we have printed dst opnd - align and print "="
        while(oss.tellp() < 24) oss << " ";
        oss << " = ";
    }

    while(i < opnds.size()) { 
        if (!lddone && icode>=INST_LD_FIRST && icode<=INST_LD_LAST) {
            oss << "[";
        }
        oss << toString(opnds[i]); 
        if (!lddone && icode>=INST_LD_FIRST && icode<=INST_LD_LAST) {
            oss << "]";
            lddone = true;
        }
        if(++i >= opnds.size()) break;
        oss << ", ";
    }
    return oss.str();                     // return string representation of ostringstream
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(Opnd *opnd) {

    ostringstream oss;
    if(opnd->getOpndKind() == OPND_INVALID) { oss << "invalid" << opnd->getId(); return oss.str(); }

    // opnd is register 
    if(opnd->isReg()) {
        RegOpnd *reg = (RegOpnd *)opnd;
        I_32   num  = reg->getLocation();

        if (num == LOCATION_INVALID) {
            num = reg->getId();
            switch(reg->getOpndKind()) {
                case OPND_A_REG: oss << "A" << num; break;
                case OPND_G_REG: oss << "R" << num; break;
                case OPND_F_REG: oss << "F" << num; break;
                case OPND_P_REG: oss << "P" << num; break;
                case OPND_B_REG: oss << "B" << num; break;
                default: oss << "??";
            }
            return oss.str();
        } 

        if (reg->isMem() == true) {
            switch(reg->getOpndKind()) {
                case OPND_A_REG: oss << "stack_a" << num; break;
                case OPND_G_REG: oss << "stack_r" << num; break;
                case OPND_F_REG: oss << "stack_f" << num; break;
                case OPND_P_REG: oss << "stack_p" << num; break;
                case OPND_B_REG: oss << "stack_b" << num; break;
                default: oss << "??";
            }
        } else {
            switch(reg->getOpndKind()) {
                case OPND_A_REG: oss << "a" << num; break;
                case OPND_G_REG: oss << "r" << num; break;
                case OPND_F_REG: oss << "f" << num; break;
                case OPND_P_REG: oss << "p" << num; break;
                case OPND_B_REG: oss << "b" << num; break;
                default: oss << "??";
            }
        }
        return oss.str();
    }

    // opnd is imm
    DataKind dataKind = opnd->getDataKind();

    if (dataKind == DATA_CONST_REF) { 
        oss << "const"; 
        return oss.str(); 
    }
    
    if (dataKind == DATA_SWITCH_REF) { 
        oss << "switch"; 
        return oss.str(); 
    }
    
    if (dataKind == DATA_NODE_REF)  {
        BbNode *targetNode = ((NodeRef*) opnd)->getNode();
        if (targetNode == NULL) oss << "unknown target";
        else                    oss << ".L" << targetNode->getId(); 
        return oss.str();
    }
    
    if (dataKind == DATA_METHOD_REF)  {
        MethodDesc *method = ((MethodRef *)opnd)->getMethod();
        if (method == NULL) oss << "unknown method";
        else                oss << method->getParentType()->getName() << "." << method->getName(); 
        return oss.str();
    }

    if (dataKind == DATA_IMM) {
        int64 val = opnd->getValue();
        if(val > 100000) oss << hex << "0x" << val << dec;
        else             oss << val;
        return oss.str();
    }
    
    oss << "ERROR";
    return oss.str();
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(QpNode *qpNode) {
    ostringstream oss;
    oss << "mask: " << boolString(qpNode->getNodeMask()) << " comp: " << boolString(qpNode->getCompMask());
    oss << " dead: " << boolString(qpNode->getLiveMask());
    return oss.str();
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(OpndSet &opndSet) {
    
    ostringstream oss;
    MemoryManager mml("IpfIrPrinter.OpndSet.toString");
    OpndVector opndVector(mml);
    opndVector.insert(opndVector.begin(), opndSet.begin(), opndSet.end());
    sort(opndVector.begin(), opndVector.end(), ptr_fun(greaterOpnd));
    
    return toString(opndVector);
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(RegOpndSet &opndSet) {
    
    ostringstream oss;
    MemoryManager mml("IpfIrPrinter.RegOpndSet.toString");
    OpndVector opndVector(mml);
    opndVector.insert(opndVector.begin(), opndSet.begin(), opndSet.end());
    sort(opndVector.begin(), opndVector.end(), ptr_fun(greaterOpnd));
    
    return toString(opndVector);
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(OpndVector &opndVector) {
    
    ostringstream oss;
    if (opndVector.size() == 0) return oss.str();
    
    oss << toString(opndVector[0]);
    for(uint16 i=1; i<opndVector.size(); i++) {
        oss << ", " << toString(opndVector[i]);
    }
    return oss.str();
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(InstVector &insts) {
    
    ostringstream oss;
    for(uint16 i=0; i<insts.size(); i++) {
        oss << "      " << toString(insts[i]) << endl;
    }
    return oss.str();
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(InstList &instList) {
    
    ostringstream oss;
    for(InstListIterator i=instList.begin(); i!=instList.end(); i++) {
        oss << "      " << toString(*i) << endl;
    }
    return oss.str();
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(Chain &chain) {

    ostringstream oss;
    for(ChainIterator i=chain.begin(); i!=chain.end();) {
        oss << "node" << (*i)->getId();
        i++;
        if (i!=chain.end()) oss << "->";
    }
    return oss.str();
}
    
//----------------------------------------------------------------------------------------//

string IrPrinter::toString(MethodDesc *methodDesc) {

    ostringstream oss;

    if (methodDesc->getParentType() != NULL) {
        oss << methodDesc->getParentType()->getName() << ".";
    }
    oss << methodDesc->getName();
    oss << methodDesc->getSignatureString();

    return oss.str();
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(NodeKind nodeKind) {
    
    string s;
    switch(nodeKind) {
        case NODE_BB      : s = "NODE_BB";       break;
        case NODE_DISPATCH: s = "NODE_DISPATCH"; break;
        case NODE_UNWIND  : s = "NODE_UNWIND";   break;
        case NODE_INVALID : s = "NODE_INVALID";  break;
    }

    if (s.empty()) IPF_ERR << " unexpected nodeKind " << nodeKind << endl;
    return s;
}

//----------------------------------------------------------------------------------------//

string IrPrinter::toString(EdgeKind edgeKind) {
    
    string s;
    switch(edgeKind) {
        case EDGE_BRANCH   : s = "EDGE_BRANCH";    break;
        case EDGE_THROUGH  : s = "EDGE_THROUGH";   break;
        case EDGE_DISPATCH : s = "EDGE_DISPATCH";  break;
        case EDGE_EXCEPTION: s = "EDGE_EXCEPTION"; break;
        case EDGE_INVALID  : s = "EDGE_INVALID";   break;
    }

    if (s.empty()) IPF_ERR << " unexpected edgeKind " << edgeKind << endl;
    return s;
}

//----------------------------------------------------------------------------//

string IrPrinter::toString(OpndKind opndKind) {
    
    string s;
    switch(opndKind) {
        case OPND_G_REG  : s = "OPND_G_REG";   break;
        case OPND_F_REG  : s = "OPND_F_REG";   break;
        case OPND_P_REG  : s = "OPND_P_REG";   break;
        case OPND_B_REG  : s = "OPND_B_REG";   break;
        case OPND_A_REG  : s = "OPND_A_REG";   break;
        case OPND_IP_REG : s = "OPND_IP_REG";  break;
        case OPND_UM_REG : s = "OPND_UM_REG";  break;
        case OPND_IMM    : s = "OPND_IMM";     break;
        case OPND_INVALID: s = "OPND_INVALID"; break;
    }

    if (s.empty()) IPF_ERR << " unexpected opndKind " << opndKind << endl;
    return s;
}

//----------------------------------------------------------------------------//

string IrPrinter::toString(DataKind dataKind) {
    
    string s;
    switch(dataKind) {
        case DATA_I8         : s = "DATA_I8";         break;
        case DATA_U8         : s = "DATA_U8";         break;
        case DATA_I16        : s = "DATA_I16";        break;
        case DATA_U16        : s = "DATA_U16";        break;
        case DATA_I32        : s = "DATA_I32";        break;
        case DATA_U32        : s = "DATA_U32";        break;
        case DATA_I64        : s = "DATA_I64";        break;
        case DATA_U64        : s = "DATA_U64";        break;
        case DATA_S          : s = "DATA_S";          break;
        case DATA_D          : s = "DATA_D";          break;
        case DATA_F          : s = "DATA_F";          break;
        case DATA_P          : s = "DATA_P";          break;
        case DATA_B          : s = "DATA_B";          break;
        case DATA_IMM        : s = "DATA_IMM";        break;
        case DATA_BASE       : s = "DATA_BASE";       break;
        case DATA_MPTR       : s = "DATA_MPTR";       break;
        case DATA_CONST_REF  : s = "DATA_CONST_REF";  break;
        case DATA_NODE_REF   : s = "DATA_NODE_REF";   break;
        case DATA_METHOD_REF : s = "DATA_METHOD_REF"; break;
        case DATA_SWITCH_REF : s = "DATA_SWITCH_REF"; break;
        case DATA_INVALID    : s = "DATA_INVALID";    break;
    }

    if (s.empty()) IPF_ERR << " unexpected dataKind " << dataKind << endl;
    return s;
}

//----------------------------------------------------------------------------//

string IrPrinter::boolString(uint64 mask, uint16 size) {
    uint64 m = 1 << size - 1; 
    ostringstream oss;
    for (int i=0; i<size; i++, m >>= 1) {
        if (mask & m) oss << "1";
        else          oss << "0";
    }
    return oss.str();
}

} // IPF
} // Jitrino
