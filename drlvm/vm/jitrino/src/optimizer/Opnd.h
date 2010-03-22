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

#ifndef _OPND_H_
#define _OPND_H_

#include <iostream>

#include "open/types.h"
#include "Type.h"
#include "MemoryManager.h"

namespace Jitrino {

class Inst;
class OpndManager;
class OpndRenameTable;
class IRBuilder;

//
// forward declarations
//
class SsaOpnd;
class SsaTmpOpnd;
class SsaVarOpnd;
class VarOpnd;
class PiOpnd;

class OpndBase {    // rename to Opnd
public:
    virtual ~OpndBase() {}
    U_32  getId() const                 {return id;}
    Type*   getType() const               {return type;}
    void    setType(Type *t)              {type = t;}
    void    setProperties(U_32 v)       {properties = v;}
    U_32  getProperties() const         {return properties;}
    //
    // methods for dynamic type checking and casting
    //
    bool            isNull() const       {return (type == NULL);}
    virtual bool    isVarOpnd() const    {return false;}
    virtual bool    isSsaOpnd() const    {return false;}
    virtual bool    isSsaVarOpnd() const {return false;}
    virtual bool    isSsaTmpOpnd() const {return false;}
    virtual bool    isPiOpnd() const     {return false;}
    SsaTmpOpnd* asSsaTmpOpnd() const {
        return isSsaTmpOpnd()?(SsaTmpOpnd*)this:NULL;
    }
    SsaVarOpnd* asSsaVarOpnd() const {
        return isSsaVarOpnd()?(SsaVarOpnd*)this:NULL;
    }
    VarOpnd*    asVarOpnd() const {
        return isVarOpnd()?(VarOpnd*)this:NULL;
    }
    SsaOpnd* asSsaOpnd() const {
        return isSsaOpnd()?(SsaOpnd*)this:NULL;
    }
    PiOpnd*    asPiOpnd() {
        return isPiOpnd()?(PiOpnd*)this:NULL;
    }

    //
    // for debug only
    //
    virtual void    print(::std::ostream&) const {}
    virtual void    printWithType(::std::ostream& os) const;
protected:
    //
    // Constructor
    //
    OpndBase(Type* t,U_32 i) : properties(0), type(t), id(i) {}
    //
    // default constructor creates a null opnd
    //
    OpndBase() : properties(0), type(NULL), id(0) {}
private:
    friend class OpndManager;
    U_32    properties;
    Type*     type;
    //
    // used by code generator to map to back end operand
    //
    U_32    id;
};

class Opnd : public OpndBase {
public:
    virtual ~Opnd() {}
    void    setIsGlobal(bool val)   {isGlobalFlag = val;}
    bool    isGlobal() const        {return isGlobalFlag;}
    //
    // eventually move these down 1 level to SsaOpnd
    //
    Inst*           getInst() const     {assert(!isVarOpnd()); return inst;}
    void            setInst(Inst* i)    {assert(!isVarOpnd()); inst = i;}
public:
    virtual void    print(::std::ostream&) const;
protected:
    friend class OpndManager;
    Opnd(Type* t,U_32 i) : OpndBase(t,i), isGlobalFlag(false), inst(NULL) {}
    Opnd() : OpndBase(), isGlobalFlag(false), inst(NULL) {}
    bool    isGlobalFlag;
    //
    // move down to SsaOpnd
    //
    Inst*   inst; // only for single-assignment opnds (tmp & ssa)
};

class SsaOpnd : public Opnd {
public:
    virtual ~SsaOpnd() {}
    virtual bool isSsaOpnd() const    {return true;}
protected:
    friend class OpndManager;
    SsaOpnd(Type* t,U_32 i) : Opnd(t,i) 
        {}

};

class SsaTmpOpnd : public SsaOpnd {
public:
    virtual ~SsaTmpOpnd() {}
    virtual bool isSsaTmpOpnd() const    {return true;}
private:
    friend class OpndManager;
    SsaTmpOpnd(Type* t,U_32 i) : SsaOpnd(t,i) {}
};

class PiOpnd : public SsaOpnd {
public:
    virtual ~PiOpnd() {}
    virtual bool isSsaTmpOpnd() const    {return false;};
    virtual bool isPiOpnd() const        {return true;};
    const Opnd *getOrg() const { return orgOpnd; };
    Opnd *getOrg() { return orgOpnd; };
    virtual void    print(::std::ostream&) const;
private:
    friend class OpndManager;
    PiOpnd(Opnd *orgOpnd0, U_32 i) : 
        SsaOpnd(orgOpnd0->getType(),i), 
        orgOpnd(orgOpnd0)
        {};
    Opnd *orgOpnd;
};

class VarAccessInst;

class VarOpnd : public Opnd {
public:
    virtual ~VarOpnd() {}
    virtual bool    isVarOpnd() const      {return true;}
    bool            isAddrTaken() const    {return isAddrTakenFlag;}
    void            setAddrTaken()         {isAddrTakenFlag = true;}
    bool            isPinned() const       {return isPinnedFlag;}
    U_32          getNumLoads() const    {return numLoads;}
    U_32          getNumStores() const   {return numStores;}
    VarOpnd*        getNextVarOpnd()       {return nextVarInMethod;}
    VarOpnd*        getPrevVarOpnd()       {return prevVarInMethod;}
    void            addVarAccessInst(VarAccessInst*);
    VarAccessInst*  getVarAccessInsts()    {return varAccessInsts;}
    bool            isDead() const         {return isDeadFlag;}
    void            setDeadFlag(bool flag) { isDeadFlag = flag; }
    //
    // debug only
    //
    void            print(::std::ostream& os) const;
    virtual void    printWithType(::std::ostream& os) const;
private:
    friend class OpndManager;
    VarOpnd(Type* t,U_32 i,VarOpnd* next,bool isPinned) 
        : Opnd(t,i), nextVarInMethod(0), prevVarInMethod(0),varAccessInsts(NULL)
    {
        isPinnedFlag = isPinned;
        isDeadFlag   = isAddrTakenFlag = false;
        numLoads = numStores = 0;
        if (next) {
            assert(next->prevVarInMethod->nextVarInMethod == next);
            nextVarInMethod = next;
            prevVarInMethod = next->prevVarInMethod;
            next->prevVarInMethod = this;
            prevVarInMethod->nextVarInMethod= this;
        } else {
            nextVarInMethod = this;
            prevVarInMethod = this;
        }
    }
    //
    // private fields
    //
    bool            isDeadFlag;
    bool            isPinnedFlag;
    bool            isAddrTakenFlag;
    U_32          numLoads;
    U_32          numStores;
    VarOpnd*        nextVarInMethod;
    VarOpnd*        prevVarInMethod;
    VarAccessInst*  varAccessInsts;     // ldvar, stvars that access this var
};

//
//
//
class SsaVarOpnd : public SsaOpnd {
public:
    virtual ~SsaVarOpnd() {}
    virtual bool isSsaVarOpnd() const {return true;}
    VarOpnd* getVar()           {return var;}
    //
    // debug only
    //
    virtual void print(::std::ostream& os) const;
    virtual void printWithType(::std::ostream& os) const;

    void setVar(VarOpnd *newvar) { 
        assert(newvar->getType() == var->getType());
        var = newvar;
    };
private:
    friend class OpndManager;
    SsaVarOpnd(U_32 i, VarOpnd* v) : SsaOpnd(v->getType(),i), var(v) {}
    VarOpnd* var;
};

class NullOpnd : public Opnd {
};


class OpndManager {
public:
    OpndManager(TypeManager& tm,MemoryManager& mm) 
        : typeManager(tm), memManager(mm) {
        nextPiOpndId=nextSsaOpndId=nextVarId=1;
        varOpnds = NULL;
        numArgs = 0;
    }
    SsaTmpOpnd* createSsaTmpOpnd(Type* type)    {
       return new (memManager) 
           SsaTmpOpnd(getOpndTypeFromLdType(type),nextSsaOpndId++);
    }
    SsaTmpOpnd* createArgOpnd(Type* type) {
        numArgs++;
        return createSsaTmpOpnd(type);
    }
    VarOpnd*    createVarOpnd(Type* type,bool isPinned)    {
        varOpnds = new (memManager) 
            VarOpnd(getOpndTypeFromLdType(type),nextVarId++,varOpnds,isPinned);
        return varOpnds;
    }
    SsaVarOpnd* createSsaVarOpnd(VarOpnd* var) {
        return new (memManager) SsaVarOpnd(nextSsaOpndId++,var);
    }
    PiOpnd* createPiOpnd(Opnd* orgOpnd) {
        return new (memManager) PiOpnd(orgOpnd, nextPiOpndId++);
    }
    //
    // Instruction Opnds can have only a subset of types.  This method converts
    // from the general set of types to the legal set of types for opnds.
    //
    Type* getOpndTypeFromLdType(Type* ldType);
    //
    // Change DefUse so that it does not depend on the Opnd::id
    // Use a hashtable instead.
    //
    int         getUniqueId(Opnd *op) {
        int id = op->getId();
        if (op->isVarOpnd()) id += (nextSsaOpndId-1);
        return id;
    }
    static Opnd*  getNullOpnd()    {return &_nullOpnd;}
    VarOpnd*      getVarOpnds()    {return varOpnds;}
    U_32        getNumVarOpnds() {return nextVarId;}
    U_32        getNumSsaOpnds() {return nextSsaOpndId;}
    U_32        getNumArgs()     {return numArgs;}
    U_32        getNumPiOpnds() {return nextPiOpndId;}

    void          deleteVar(VarOpnd *var);
private:
    //
    // private fields
    //
    static NullOpnd  _nullOpnd;
    U_32          nextSsaOpndId;
    U_32          nextPiOpndId;
    U_32          nextVarId;
    U_32          numArgs;
    VarOpnd*        varOpnds;
    TypeManager&    typeManager;
    MemoryManager&  memManager;
};

class OpndRenameTable : public HashTable<Opnd,Opnd> {
public:
    OpndRenameTable(MemoryManager& mm, U_32 size = 16, bool renameSSA = true): 
        HashTable<Opnd,Opnd>(mm,size) {renameSsaOpnd = renameSSA;}
    
    virtual ~OpndRenameTable() {}    

    Opnd *getMapping(Opnd *from)   {return lookup(from); }
    void  setMapping(Opnd *from, Opnd *to) {
        insert(from,to);
    }
    Opnd* rename(Opnd* opndToRename) {
        // if the argument is in the rename table, take it
        // otherwise use the original operand
        Opnd *lkp = getMapping(opndToRename);
        if (lkp != NULL)
            return lkp;
        return opndToRename;
    }
    Opnd* duplicate(OpndManager& opndManager, Opnd* opndToRename);
protected:
    virtual bool keyEquals(Opnd* key1,Opnd* key2) const {
        return key1 == key2;
    }
    virtual U_32 getKeyHashCode(Opnd* key) const {
        // return hash of address bits
        return (key ? key->getId() : 0);
    }
private:
    bool renameSsaOpnd;
};

} //namespace Jitrino 

#endif // _OPND_H_
