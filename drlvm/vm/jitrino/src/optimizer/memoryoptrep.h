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

#ifndef _MEMORY_OPT_REP_H
#define _MEMORY_OPT_REP_H

#include "./ssa/SSA.h"

namespace Jitrino {

class Type;
class AliasAnalyzer;

// each AliasRep represents a set of memory locations
// note that we don't distinguish singletons, that's a separate issue
// overlap between sets is tested by using AliasManager::mayAlias()
class AliasRep {
public:
    enum Kind {
        NullKind,   // empty set
        GlobalKind, // overlaps all globals
        LocalKind, // overlaps all non-globals
        AnyKind,    // full set: everything
        
        UnknownKind, // other opnd, should not be vtable, methodptr, arraylen, field

        ObjectFieldKind, // parameterized by object and field desc
        UnresolvedObjectFieldKind, // field of unresolved type
        ArrayElementKind, // offset into array, parameterized by array object and offset opnd

        StaticFieldKind, // just field, type is pulled out too
        UnresolvedStaticFieldKind, // just field, type is pulled out too

        ObjectVtableKind, // opnd
        MethodPtrKind, // opnd, desc
        FunPtrKind, // opnd
        TypeVtableKind, // type
        ArrayLenKind, // opnd
        NewObjectKind, // opnd (all fields initialized by NewObject or NewArray)
        NewTypeKind, // type (all fields initialized by <clinit>

        FinishObjectKind, // all final fields of an object
        FinishTypeKind, // all final fields of a type
        LockKind, // opnd
        TypeLockKind // type
    } kind;
    Opnd *opnd;
    Opnd *idx;
    Opnd *enclClass;
    Type *type;
    TypeMemberDesc *desc;
    int id;

    AliasRep(int) : kind(NullKind), opnd(0), idx(0), enclClass(0), type(0), desc(0), id(0) {};
    AliasRep(Kind k=NullKind) : kind(k), opnd(0), idx(0), enclClass(0),type(0), desc(0), id(0) {};
    AliasRep(Kind k, Opnd *op) : kind(k), opnd(op), idx(0), enclClass(0),type(0), desc(0), id(0) {
    };
    AliasRep(Kind k, Opnd *op, Opnd *idx0) : kind(k), opnd(op), idx(idx0), enclClass(0), type(0), desc(0), id(0) {
    };
    AliasRep(Kind k, Opnd *op, TypeMemberDesc *md) : kind(k), opnd(op), idx(0), enclClass(0), type(0), desc(md), id(0) {
    };
    AliasRep(Kind k, Type *t) : kind(k), opnd(0), idx(0), enclClass(0), type(t), desc(0), id(0) {
    };
    AliasRep(Kind k, TypeMemberDesc *md) : kind(k), opnd(0), idx(0), enclClass(0), type(0), desc(md), id(0) {
    };
    
    AliasRep(Kind k, Opnd* op, Opnd* enc, Opnd* idx0) : kind(k), opnd(op), idx(idx0), enclClass(enc), type(0), desc(0), id(0) {
    };

    void dump(::std::ostream &os) const;
    void print(::std::ostream &os) const;

    bool isObjectFinal() const; // all pointed to locations are final for one object
    Opnd *getFinalObject() const;     //    get a representation for that object
    bool isTypeFinal() const;   // all pointed to locations are final for one type
    NamedType *getFinalType() const;     //    get a representation for that type
    bool isEmpty() const { return (kind == NullKind); };

    bool operator < (const AliasRep &other) const {
        return ((kind < other.kind) || 
                ((kind == other.kind) && 
                 ((opnd < other.opnd) ||
                  ((opnd == other.opnd) &&
                   ((enclClass < other.enclClass) ||
                    ((enclClass== other.enclClass) &&
                     ((type < other.type) ||
                      ((type == other.type) &&
                       ((desc < other.desc) ||
                        ((desc == other.desc) &&
                         (idx < other.idx)))))))))));
    }
    bool operator == (const AliasRep &other) const {
        return ((kind == other.kind) &&
                (opnd == other.opnd) &&
                (enclClass == other.enclClass) &&
                (type == other.type) &&
                (desc == other.desc) &&
                (idx == other.idx));
    }
    operator size_t() const {
        return (((size_t) kind) ^
                ((size_t) opnd) ^
                ((size_t) type) ^
                ((size_t) desc) ^
                ((size_t) enclClass));
    }
};

class AliasManager {
    friend class MemoryOpt;
public:
    typedef StlVector<AliasRep> AliasList;
private:
    U_32 numAliasReps;
    AliasList allAliasReps;
    typedef StlHashMap<AliasRep, StlVectorSet<AliasRep> *> AliasRep2AliasReps;
    AliasRep2AliasReps canon2others;
    typedef StlHashMap<AliasRep, AliasRep> Alias2Alias;
    Alias2Alias other2canon;
    typedef StlHashMap<AliasRep, AliasList *> Ancestors;
    Ancestors ancestors;

    AliasRep findOrInsertAlias(AliasRep rep);
    // use only during initial alias set construction
    bool isDuplicate(const AliasRep &a, const AliasRep &b);
    void sawDuplicate(const AliasRep &ar, const AliasRep &canon);
public:

    AliasRep getReference(Opnd *addr); // examine for an indirect addr

    AliasRep getObjectField(Opnd *obj, TypeMemberDesc *field);
    AliasRep getUnresolvedObjectField(Opnd *obj, Opnd* enclClass, Opnd* cpIdx);
    AliasRep getStaticField(TypeMemberDesc *field);
    AliasRep getUnresolvedStaticField(Opnd* enclClass, Opnd* cpIdx);
    AliasRep getArrayElementByType(Type *elementType);
    AliasRep getLock(Opnd *obj);
    AliasRep getLock(Type *type);

    AliasRep getAny();
    AliasRep getAnyGlobal();
    AliasRep getVtableOf(Opnd *opnd);
    AliasRep getMethodPtr(Opnd *opnd, MethodDesc *desc);
    AliasRep getFunPtr(Opnd *opnd);
    AliasRep getNoMemory();
    AliasRep getAnyEscaping();
    AliasRep getAnyLocal();
    AliasRep getVtableOf(NamedType *type);
    AliasRep getArrayLen(Opnd *opnd);
    AliasRep getArrayElements(Opnd *array, Opnd *offset, Opnd *length);
    AliasRep getObjectNew(Opnd *opnd); // fields initialized by NewObject or NewArray
    AliasRep getTypeNew(NamedType *type); // (final static) fields initialized by type initialization
    AliasRep getFinishObject(Opnd *obj); // final fields of object
    AliasRep getFinishType(NamedType *type); // final fields of type

    MemoryManager &mm;
    AliasAnalyzer *analyzer;
    TypeManager* typeManager;

    void dumpAliasReps(::std::ostream &os) const;

    AliasManager(MemoryManager &mm0, AliasAnalyzer *aa, TypeManager* tm) :
        numAliasReps(0), allAliasReps(mm0), canon2others(mm0), other2canon(mm0), ancestors(mm0), mm(mm0), analyzer(aa), typeManager(tm) {};

    bool mayAlias(const AliasRep &a, const AliasRep &b);

    // yields a list of ancestors in any order
    const AliasList *getAncestors(const AliasRep &a);
private:
    // adds parents to result, returning true if nonempty
    // yields a list of ancestors in any order
    void computeAncestors(const AliasRep &a, AliasList *result);
};

class InstMemBehavior {
public:
    bool acquire, release;
    StlVectorSet<AliasRep> defs;
    StlVectorSet<AliasRep> uses;
    InstMemBehavior(MemoryManager &mm) 
        : acquire(false), release(false), defs(mm), uses(mm)
    {}; 
    InstMemBehavior(const StlVectorSet<AliasRep> &defs0, 
                    const StlVectorSet<AliasRep> &uses0)
        : acquire(false), release(false), defs(defs0), uses(uses0) {};
};

class AliasDefSites : public StlHashMap<AliasRep, VarDefSites *> {
    MemoryManager &mm;
    U_32 numNodes;
public:
    typedef StlHashMap<AliasRep, VarDefSites *> BaseType;
    AliasDefSites(MemoryManager &m, U_32 n): BaseType(m), mm(m), numNodes(n) {};
    void addAliasDefSite(const AliasRep &rep, Node* node) {
        VarDefSites* aliasSites = (*this)[rep];
        if (!aliasSites) {
            aliasSites = new (mm) VarDefSites(mm, numNodes);
            (*this)[rep] = aliasSites;
        }
        aliasSites->addDefSite(node);
    };
    bool hasAliasDefSite(const AliasRep &rep, Node* node) {
        iterator aliasSitesIter = this->find(rep);
        if (aliasSitesIter == end()) {
            return false;
        }
        VarDefSites* aliasSites = (*aliasSitesIter).second;
        return aliasSites->isDefSite(node);
    };
};

class MemPhiSites {
public:
    MemoryManager &mm;
    size_t numNodes;
    StlVector<AliasRep> **nodeArray;
    MemPhiSites(MemoryManager &mm0, size_t numNodes) : mm(mm0), nodeArray() {
        nodeArray = new (mm0) StlVector<AliasRep> *[numNodes];
        for (size_t i = 0; i < numNodes; i++) {
            nodeArray[i] = 0;
        }
    }
    void addMemPhi(Node *n, const AliasRep &rep) {
        size_t i = n->getDfNum();
        StlVector<AliasRep> *a = nodeArray[i];
        if (!a) {
            a = new (mm) StlVector<AliasRep>(mm);
            nodeArray[i] = a;
        }
        a->push_back(rep);
    }
    const StlVector<AliasRep> *getMemPhis(Node *n) const {
        size_t i = n->getDfNum();
        return nodeArray[i];
    }
};

struct AliasBinding {
    Inst *inst;
    U_32 when;
    AliasBinding(Inst *inst0=0, U_32 when0=0) : inst(inst0), when(when0) {};
};

bool operator !=(const AliasBinding &a, const AliasBinding &b) {
    return ((a.inst != b.inst) || (a.when != b.when));
}
    
class DescendentSet : public StlVectorSet<AliasRep> {
public:
    DescendentSet(MemoryManager &m, U_32 depth0) : StlVectorSet<AliasRep>(m), depth(depth0) {};
    DescendentSet(MemoryManager &m, DescendentSet *other, U_32 depth0)
        : StlVectorSet<AliasRep>(m), depth(depth0)
    {
        insert(other->begin(), other->end());
    }
    U_32 depth;
};

class AliasRenameMap {
    typedef SparseScopedMap<AliasRep, AliasBinding> DefMap;
    typedef SparseScopedMap<AliasRep, DescendentSet *> DescendentMap;
    DefMap defMap;
    DescendentMap activeDescendents;
    U_32 depth;
    U_32 timeCount;
    AliasManager *aliasManager;
    MemoryManager &mm;
public:
    AliasRenameMap(MemoryManager &m, AliasManager *am0, 
                   size_t sizeEstimate1,
                   size_t sizeEstimate2) : 
        defMap(sizeEstimate1, m), 
        activeDescendents(sizeEstimate2, m), 
        depth(0), timeCount(0),
        aliasManager(am0), mm(m) {};

    void enter_scope();
    void exit_scope();
    void insert(const AliasRep &rep, Inst *defInst);
    // finds most recent relevant defs
    
    typedef StlVectorSet<Inst *> DefsSet;
    void lookup(const AliasRep &rep, DefsSet &defs); 
};

} //namespace Jitrino 

#endif // _MEMORY_OPT_REP_H
