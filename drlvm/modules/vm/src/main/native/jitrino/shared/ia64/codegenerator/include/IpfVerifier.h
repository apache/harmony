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

#ifndef IPFVERIFIER_H_
#define IPFVERIFIER_H_

#include <vector>
#include <bitset>
#include <string>
#include "Type.h"
#include "IpfIrPrinter.h"

using namespace std;

namespace Jitrino {
namespace IPF {

class IpfVerifier {
  public:
         IpfVerifier(Cfg & cfg, CompilationInterface & compilationinterface);
         
    bool verifyMethod(char * str=NULL);
    bool verifyMethodInsts(char * str=NULL);
    bool verifyMethodNodes(char * str=NULL);
    bool verifyMethodEdges(char * str=NULL);
    bool verifyNodeInsts(BbNode  * node);
    bool verifyNodeEdges(BbNode  * node);
    bool verifyInst(string * res, Inst *);
    
protected:
    static bool cmp_cmp4(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);
    static bool fcmp(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);
    static bool ldx(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);
    static bool stx(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);
    static bool ldfx(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);
    static bool mov(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);
    static bool br(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls);

    static char * getDataKindStr(DataKind datakind);
    
    static void   edgeInvalid(Node* node, ostream& os);
    static void   edgeBranch( Node* node, Edge* edge, ostream& os);
    static void   edgeThrough(Node* node, Edge* edge, ostream& os);

private:
    Cfg & cfg;
    CompilationInterface & compilationinterface;
    MemoryManager & mm;
    MethodDesc * methodDesc;
    string * methodString;

};

} // IPF
} // Jitrino

#endif /*IPFVERIFIER_H_*/
