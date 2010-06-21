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

#ifndef _IA32_PRINTER_H_
#define _IA32_PRINTER_H_

#include "open/types.h"
#include "Stl.h"
#include "MemoryManager.h"
#include "Type.h"
#include "CodeGenIntfc.h"
#include "Ia32IRManager.h"
#include "PrintDotFile.h"
#include "LogStream.h"
#include <fstream>

namespace Jitrino
{
namespace Ia32{

//========================================================================================
// class Printer
//========================================================================================
/** class Printer is the base class in the Ia32 CG debug printing framework
It allows to specify a print output destination, and imposes the base structure of 
the printed document (header, body, end).

Each printXXX method receives the indent operand for recursive indentation of the output
*/
class Printer
{
public:
    /** creates Printer instance for the specified IRManager 
    Allows to provide an optinal title
    */
    Printer(const IRManager * irm=0, const char * _title=0)
        :irManager(irm), title(_title), os(0)
         {if (irManager==NULL) irManager = CompilationContext::getCurrentContext()->getLIRManager();}

    /** destructs Printer instance */
    virtual ~Printer() {}

    /** Sets os as the current output destination */
    Printer& setStream(::std::ostream& os);

    Printer& open(char* fname);
    void close();

    /** print everything supposed to be printed to the current output */
    virtual void print(U_32 indent=0);
    
    virtual void printHeader(U_32 indent=0);
    virtual void printEnd(U_32 indent=0);
    virtual void printBody(U_32 indent=0);

    /** Prints indent tabs */
    virtual void printIndent(U_32 indent)
        { while (indent--) getStream()<<'\t';  }
    static void printIndent(::std::ostream& os, U_32 indent)
        { Printer p; p.setStream(os); p.printIndent(indent); }

    /** returns the current output desitnation */
    ::std::ostream& getStream(){ assert(os); return *os; }

    /** returns the title set in the constuctor */
    const char *        getTitle(){ return title; }
    /** returns a pointer to the IRManager associated with this printer */
    const IRManager *   getIRManager(){ return irManager; }
protected:
    const IRManager *   irManager;
    const char *        title;
    LogStream           logs;
    ::std::ostream *        os;

};

//========================================================================================
// class OpcodeDescriptionPrinter
//========================================================================================
/** class OpcodeDescriptionPrinter prints the current table 
of opcode description groups and introduces methods for printing of basic  
LIR parts: Constraints, RegNames, opnd roles.
*/
class OpcodeDescriptionPrinter: public Printer
{
public:
    OpcodeDescriptionPrinter(const IRManager * irm=0, const char * _title=0)
        :Printer(irm, _title){}

    virtual void printConstraint(Constraint constraint);
    static void printConstraint(::std::ostream& os, Constraint constraint)
    { OpcodeDescriptionPrinter p; p.setStream(os); p.printConstraint(constraint); }

    virtual void printRegName(const RegName regName);
    static void printRegName(::std::ostream& os, const RegName regName)
    { OpcodeDescriptionPrinter p; p.setStream(os); p.printRegName(regName); }

    virtual void printOpndRoles(U_32 roles);
    static void printOpndRoles(::std::ostream& os, U_32 roles)
    { OpcodeDescriptionPrinter p; p.setStream(os); p.printOpndRoles(roles); }

    virtual void printOpndRolesDescription(const Encoder::OpndRolesDescription * ord);
    static void printOpndRolesDescription(::std::ostream& os, Encoder::OpndRolesDescription * ord)
    { OpcodeDescriptionPrinter p; p.setStream(os); p.printOpndRolesDescription(ord); }

    virtual void printOpcodeDescription(const Encoder::OpcodeDescription * od, U_32 indent=0);
    static void printOpcodeDescription(::std::ostream& os, Encoder::OpcodeDescription * od, U_32 indent=0)
    { OpcodeDescriptionPrinter p; p.setStream(os); p.printOpcodeDescription(od, indent); }

    virtual void printOpcodeGroup(const Encoder::OpcodeGroup* og, U_32 indent=0);
    static void printOpcodeGroup(::std::ostream& os, Encoder::OpcodeGroup* og, U_32 indent=0)
    { OpcodeDescriptionPrinter p; p.setStream(os); p.printOpcodeGroup(og, indent); }

    virtual void print(U_32 indent=0);
};

//========================================================================================
// class IRPrinter
//========================================================================================
/** class IRPrinter performs debug printing of a CFG in the text format

It also introduces methods to print LIR elements in the text format.
*/
class IRPrinter: public OpcodeDescriptionPrinter
{
public:
    enum OpndFlavor{
        OpndFlavor_Type=0x1,
        OpndFlavor_Location=0x2, 
        OpndFlavor_RuntimeInfo=0x4, 
        OpndFlavor_All=0xffffffff,
        OpndFlavor_Default=OpndFlavor_Type|OpndFlavor_Location
    };

    IRPrinter(const IRManager * irm=0, const char * _title=0)
        :OpcodeDescriptionPrinter(irm, _title), instFilter((U_32)Inst::Kind_Inst), 
        opndFlavor((U_32)OpndFlavor_All), opndRolesFilter((U_32)(Inst::OpndRole_InstLevel|Inst::OpndRole_Use|Inst::OpndRole_Def))
    {}

    IRPrinter & setInstFilter(U_32 kinds){ instFilter=kinds; return *this; }
    IRPrinter & setOpndFlavor(U_32 f){ opndFlavor=f; return *this; }
    IRPrinter & setOpndRolesFilter(U_32 orf){ opndRolesFilter=orf; return *this; }

    virtual void printNodeName(const Node * node);
    static void printNodeName(::std::ostream& os, const Node * node)
    { IRPrinter p; p.setStream(os); p.printNodeName(node); }

    virtual void printNodeHeader(const Node * node, U_32 indent=0);
    virtual void printNodeInstList(const Node* bb, U_32 indent=0);

    virtual void printNode(const Node * node, U_32 indent=0);
    static void printNode(::std::ostream& os, const Node * node, U_32 indent=0)
    { IRPrinter p; p.setStream(os); p.printNode(node, indent); }

    virtual void printEdge(const Edge * edge);
    static void printEdge(::std::ostream& os, const Edge * edge)
    { IRPrinter p; p.setStream(os); p.printEdge(edge); }

    virtual void printInst(const Inst * inst);
    static void printInst(::std::ostream& os, const Inst * inst, 
        U_32 opndRolesFilter=Inst::OpndRole_Explicit|Inst::OpndRole_Auxilary|Inst::OpndRole_Use|Inst::OpndRole_Def,
        U_32 opndFlavor=OpndFlavor_Default
    )
    { IRPrinter p; p.setStream(os); p.setOpndRolesFilter(opndRolesFilter); p.setOpndFlavor(opndFlavor); p.printInst(inst); }

    virtual U_32 printInstOpnds(const Inst * inst, U_32 opndRolesFilter);

    virtual void printOpndRoles(U_32 roles);
    static void printOpndRoles(::std::ostream& os, U_32 roles)
    { IRPrinter p; p.setStream(os); p.printOpndRoles(roles); }

    void printOpndName(const Opnd * opnd);
    static void printOpndName(::std::ostream& os, const Opnd * opnd)
    { IRPrinter p; p.setStream(os); p.printOpndName(opnd); }

    virtual U_32 getOpndNameLength(Opnd * opnd);

    virtual void printOpnd(const Inst * inst, U_32 idx, bool isLiveBefore=false, bool isLiveAfter=false);

    virtual void printOpnd(const Opnd * opnd, U_32 roles=0, bool isLiveBefore=false, bool isLiveAfter=false);
    static void printOpnd(::std::ostream& os, const Opnd * opnd, U_32 of=OpndFlavor_Default)
    { IRPrinter p; p.setStream(os); p.setOpndFlavor(of); p.printOpnd(opnd); }

    virtual void printRuntimeInfo(const Opnd::RuntimeInfo * ri);
    static void printRuntimeInfo(::std::ostream& os, const Opnd::RuntimeInfo * ri)
    { IRPrinter p; p.setStream(os); p.printRuntimeInfo(ri); }

    virtual void printType(const Type * type);
    static void printType(::std::ostream& os, const Type * type)
    { IRPrinter p; p.setStream(os); p.printType(type); }

    virtual void printCFG(U_32 indent=0);

    virtual void printBody(U_32 indent=0);
    virtual void print(U_32 indent=0);

    static const char * getPseudoInstPrintName(Inst::Kind k);

protected:
    U_32      instFilter;
    U_32      opndFlavor;
    U_32      opndRolesFilter;
};

inline ::std::ostream& operator << (::std::ostream& os, Opnd * opnd){ IRPrinter::printOpnd(os, opnd); return os; }

//========================================================================================
// class IROpndPrinter
//========================================================================================
class IROpndPrinter: public IRPrinter
{
public:
    IROpndPrinter(const IRManager * irm=0, const char * _title=0)
        :IRPrinter(irm, _title){}

    void printBody(U_32 indent=0);
    void printHeader(U_32 indent=0);
};

//========================================================================================
// class IRLivenessPrinter
//========================================================================================
class IRLivenessPrinter: public IRPrinter
{
public:
    IRLivenessPrinter(const IRManager * irm=0, const char * _title=0)
        :IRPrinter(irm, _title){}
    void printNode(const Node * node, U_32 indent=0);

    virtual void printLiveSet(const BitSet * ls);
    static void printLiveSet(::std::ostream& os, const IRManager& irm, const BitSet * ls)
    { IRLivenessPrinter p(&irm); p.setStream(os); p.printLiveSet(ls); }
};

//========================================================================================
// class IRInstConstraintPrinter
//========================================================================================
class IRInstConstraintPrinter: public IRPrinter
{
public:
    IRInstConstraintPrinter(const IRManager * irm=0, const char * _title=0)
        :IRPrinter(irm, _title){}

    virtual void printOpnd(const Inst * inst, U_32 opndIdx, bool isLiveBefore=false, bool isLiveAfter=false);
};

//========================================================================================
// class IRDotPrinter
//========================================================================================
/** class IRPrinter performs debug printing of a CFG in the dot-file format

It overrides certain methods to print LIR elements in the dot-file format.
*/
class IRDotPrinter: public IRPrinter
{
public:
    IRDotPrinter(const IRManager * irm=0, const char * _title=0)
        :IRPrinter(irm, _title){}

    virtual void printNode(const Node * node);
    virtual void printEdge(const Edge * edge);

    virtual void printLayoutEdge(const BasicBlock * from, const BasicBlock * to);

    virtual void printTraversalOrder(CGNode::OrderType orderType);
    virtual void printLiveness();

    virtual void printCFG(U_32 indent=0);

    virtual void printHeader(U_32 indent=0);
    virtual void printEnd(U_32 indent=0);
    virtual void printBody(U_32 indent=0);
    virtual void print(U_32 indent=0);
    
};

//========================================================================================
// class IRDotPrinter
//========================================================================================
/** class IRLivenessPrinter performs debug printing of a CFG in the dot-file format
focusing in liveness information for operands

*/
class IRLivenessDotPrinter: public IRDotPrinter
{
public:
    IRLivenessDotPrinter(const IRManager * irm=0, const char * _title=0)
        :IRDotPrinter(irm, _title){}

    virtual void printNode(const Node * node);

    virtual void printBody(U_32 indent=0);

private:
    void printLivenessForInst(const StlVector<Opnd*> opnds, const BitSet * ls0, const BitSet * ls1);
    char * getRegString(char * str, Constraint c, StlVector<Opnd *> opnds);

};


//========================================================================================
// LIR logging helpers
//========================================================================================
void dumpIR(
            const IRManager * irManager,
            U_32 stageId,
            const char * readablePrefix,
            const char * readableStageName,
            const char * stageTagName,
            const char * subKind1, 
            const char * subKind2=0,
            U_32 instFilter=Inst::Kind_Inst, 
            U_32 opndFlavor=IRPrinter::OpndFlavor_All,
            U_32 opndRolesFilter=Inst::OpndRole_Explicit|Inst::OpndRole_Auxilary|Inst::OpndRole_Implicit|Inst::OpndRole_Use|Inst::OpndRole_Def
            );

void printDot(
            const IRManager * irManager,
            U_32 stageId,
            const char * readablePrefix,
            const char * readableStageName,
            const char * stageTagName,
            const char * subKind1, 
            const char * subKind2=0,
            U_32 instFilter=Inst::Kind_Inst, 
            U_32 opndFlavor=IRPrinter::OpndFlavor_All,
            U_32 opndRolesFilter=Inst::OpndRole_Explicit|Inst::OpndRole_Auxilary|Inst::OpndRole_Implicit|Inst::OpndRole_Use|Inst::OpndRole_Def
            );

void printRuntimeArgs(::std::ostream& os, U_32 opndCount, CallingConvention::OpndInfo * infos, JitFrameContext * context);
void printRuntimeOpnd(::std::ostream& os, TypeManager & tm, Type::Tag typeTag, const void * p);
void printRuntimeObjectOpnd(::std::ostream& os, TypeManager & tm, const void * p);
void printRuntimeObjectContent(::std::ostream& os, TypeManager & tm, Type * type, const void * p);

void __stdcall printRuntimeOpndInternalHelper(const void * p) stdcall__;


}}; // namespace Ia32

#endif
