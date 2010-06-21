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
 * @author Mikhail Y. Fursov
 */


#ifndef _IA32_GC_SAFE_POINTS_H_
#define _IA32_GC_SAFE_POINTS_H_

#include "Ia32IRManager.h"
namespace Jitrino
{
namespace Ia32 {


class GCPointsBaseLiveRangeFixer: public SessionAction {
public:
    void runImpl();
    U_32 getSideEffects() const {return sideEffect;}
    U_32 getNeedInfo()const{ return 0;}
private:
    U_32 sideEffect;    
};

class GCSafePointsInfo;

#define MPTR_OFFSET_UNKNOWN 2147483647 //((1<<31)-1) maxint -> illegal offset

class MPtrPair {
    friend class GCSafePointsInfo;
    Opnd* mptr;
    Opnd* base;
    int   offset;
public:    
    MPtrPair(Opnd* _mptr, Opnd* _base, int _offset) 
        : mptr(_mptr), base(_base), offset (_offset)
    {
        assert(mptr != NULL);
    }
    
    MPtrPair(const MPtrPair& p) : mptr(p.mptr), base(p.base), offset(p.offset){}
    
    //STL requirement : MPtrPair is stored by value in vector
    MPtrPair() : mptr(NULL), base(NULL), offset(MPTR_OFFSET_UNKNOWN){}

    inline MPtrPair& operator= (const MPtrPair& p) {
        mptr = p.mptr;
        base = p.base;
        offset = p.offset;
        return *this;
    }
    inline bool operator==(const MPtrPair& p) const {return (p.mptr == mptr && p.base == base && p.offset == offset);}
    inline bool operator<(const MPtrPair& p) const {
        if (mptr!=p.mptr) {
            return mptr < p.mptr;
        }
        if (base!=p.base) {
            return base < p.base;
        }
        if (offset!=p.offset) {
            return offset < p.offset;
        }
        return false;
    }
    
    inline bool equalBaseAndMptr(const MPtrPair& p) const {return  p.mptr == mptr && p.base == base;}
    static bool equalMptrs(const MPtrPair& p1, const MPtrPair& p2) {return p1.mptr == p2.mptr;}

    Opnd* getMptr() const { return mptr;}
    Opnd* getBase() const {return base;}
    int   getOffset() const  { return offset;}

    
};

typedef StlVector<MPtrPair> GCSafePointPairs;
typedef StlMap<U_32, GCSafePointPairs*> GCSafePointPairsMap;

class GCSafePointsInfo {

public:
    /** FIX_BASES Mode -> works on 3-address IR, calculates pairs and resolves base-ambiguity
        CALC_OFFSETS -> works on 2-address IR, calculates offsets only, does not resolves base-ambibuity */
    enum Mode {MODE_1_FIX_BASES, MODE_2_CALC_OFFSETS};

    GCSafePointsInfo(MemoryManager& mm, IRManager& irm, Mode mode);
    bool hasPairs() const {return !pairsByGCSafePointInstId.empty();}
    
    GCSafePointPairs& getGCSafePointPairs(const Inst* gcSafePointInst) const {
        GCSafePointPairsMap::const_iterator it = pairsByGCSafePointInstId.find(gcSafePointInst->getId());
        assert(it!=pairsByGCSafePointInstId.end());
        return *(*it).second;
    }
    
    const StlSet<Opnd*>& getStaticFieldMptrs() const {return staticMptrs;}
   bool isStaticFieldMptr(Opnd* opnd) const { return staticMptrs.find(opnd)!=staticMptrs.end();}        
    void dump(const char* stage) const;    
   
    static bool isGCSafePoint(const Inst* inst)  {return IRManager::isGCSafePoint(inst);}
    static bool graphHasSafePoints(const IRManager& irm);
    static bool blockHasSafePoints(const Node* b);
    /** if pairs!=NULL counts only gcpoints with non-empty pairs*/
    static U_32 getNumSafePointsInBlock(const Node* b, const GCSafePointPairsMap* pairsMap = NULL);
    static MPtrPair* findPairByMPtrOpnd(GCSafePointPairs& pairs, const Opnd* mptr);
   static bool isManaged(Opnd* op) { return op->getType()->isObject() || op->getType()->isManagedPtr();}

private:
    void _calculate();
    void insertLivenessFilters();
    void calculateMPtrs();
    void filterLiveMPtrsOnGCSafePoints();
   void filterStaticMptrs(Inst* inst);        

    void checkPairsOnNodeExits() const ;
    void checkPairsOnGCSafePoints() const ;

    /** fills res with pairs on 'node' entry, merges equal pairs and resolves ambiguous mptrs.
     *  side effect: while resolving ambiguous mptrs could modify pred blocks -> adds verdef
     */
    void derivePairsOnEntry(const Node* node, GCSafePointPairs& res);
    void updatePairsOnInst(Inst* inst, GCSafePointPairs& res) ;
    void addAllLive(const BitSet* ls, GCSafePointPairs& res, const GCSafePointPairs& predBlockPairs) const ;
    
    bool hasEqualElements(GCSafePointPairs& p1, GCSafePointPairs& p2) const  {
        if (p1.size()!=p2.size()) {
            return FALSE;
        }
        std::sort(p1.begin(), p1.end());
        std::sort(p2.begin(), p2.end());
        return std::equal(p1.begin(), p1.end(), p2.begin());
    };

    void setLivenessFilter(const Inst* inst, const BitSet* ls);
    const BitSet* findLivenessFilter(const Inst* inst) const;
    void removePairByMPtrOpnd(GCSafePointPairs& pairs, const Opnd* mptr) const;
    void runLivenessFilter(Inst* inst, GCSafePointPairs& res) const;
    Opnd* getBaseAccordingMode(Opnd* opnd) const;
    void  updateMptrInfoInPairs(GCSafePointPairs& res, Opnd* newMptr, Opnd* fromOpnd, I_32 offset, bool fromOpndIsBase);
    //returns offset if offsetOpnd is immediate ir MPTR_OFFSET_UNKNOWN if not */
    I_32 getOffsetFromImmediate(Opnd* offsetOpnd) const;

    MemoryManager&  mm;
    IRManager&      irm;
    
    
    StlVector<GCSafePointPairs*> pairsByNode;
    GCSafePointPairsMap         pairsByGCSafePointInstId;
    StlMap<const Inst*, const BitSet*> livenessFilter;
    StlMap<Inst*, Opnd*> ambiguityFilters;
    StlSet<Opnd*> staticMptrs;
    bool allowMerging;

    /** stat fields */
    U_32 opndsAdded; 
    U_32 instsAdded;
    Mode   mode;

};


}} //namespace

#endif
