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

#ifndef _CLASSIC_ABCD_SOLVER_H
#define _CLASSIC_ABCD_SOLVER_H

#include <assert.h>
#include <iostream>
#include <climits>

#include "open/types.h"
#include "Stl.h"

namespace Jitrino {

class IOpnd {
public:
    IOpnd(U_32 id, bool is_phi = false, bool is_constant = false) :
        _id(id), _phi(is_phi), _const(is_constant),
        _unconstrained(false), _value(0)
    {}

    IOpnd() { assert(0); }
    virtual ~IOpnd() {};

    void setPhi(bool s = true) { _phi = s; }
    bool isPhi() const { return _phi; }

    void setIsConstant(bool s = true) { _const = s; }
    bool isConstant() const { return _const; }

    void setConstant(I_32 val) { setIsConstant(true); _value = val; }
    I_32 getConstant() const { assert(isConstant()); return _value; }

    void setUnconstrained(bool unc) { _unconstrained = unc; }
    bool isUnconstrained() { return _unconstrained; }

    void setID(U_32 id) { _id = id; }
    U_32 getID() const { return _id; }

    virtual void printName(std::ostream& os) const;
    void printFullName(std::ostream& os) const;
private:
    U_32 _id;
    bool   _phi, _const, _unconstrained;
    I_32  _value;
};

class BoundState {
public:
    BoundState() : _upper(true) {}
    BoundState(bool upper) : _upper(upper) {}

    void setUpper(bool upper = true) { _upper = upper; }

    bool isUpper() const { return _upper; }
private:

    bool _upper;
};

class HasBoundState {
public:
    HasBoundState(const BoundState& bs) : _bound_state(bs) {}

    const BoundState& getBoundState() { return _bound_state; }

    bool isUpper() const { return _bound_state.isUpper(); }
private:
    const BoundState& _bound_state;
};

class IneqEdge {
public:
    IneqEdge(IOpnd* src, IOpnd* dst, I_32 len) :
        _src(src), _dst(dst), _length(len)
    {}
    IOpnd* getSrc() const { return _src; }
    IOpnd* getDst() const { return _dst; }
    I_32 getLength() const { return _length; }
    void setLength(I_32 len) { _length = len; }
private:
    IOpnd *_src, *_dst;
    I_32 _length;
};

typedef StlList<IneqEdge*> EdgeList;
typedef StlSet<IneqEdge*> EdgeSet;

class TwoStateEdgeList {
public:
    TwoStateEdgeList(MemoryManager& mm) :
        _mm(mm),
        _permanent(mm),
        _lower(mm),
        _upper(mm),
        _is_lower(false)
    {
    }

    bool isLowerState() { return _is_lower; }

    void addEdge(IneqEdge* edge)
    {
        _permanent.push_back(edge);
    }

    void addEdgeSingleState(IneqEdge *edge, bool is_lower)
    {
        EdgeList* list = is_lower ? (&_lower) : (&_upper);
        list->push_back(edge);
    }

    // the iterator is 'invalidated' when the state of the TwoStateEdgeList
    // changes
    class iterator {
    public:
        void next()
        {
            if ( _first_it != _first_end ) {
                _first_it++;
            }else if ( _second_it != _second_end ) {
                _second_it++;
            }else{
                assert(0);
            }
        }

        IneqEdge* get()
        {
            if ( _first_it != _first_end ) {
                return (*_first_it);
            }else if ( _second_it != _second_end ) {
                return (*_second_it);
            }else{
                return NULL;
            }
        }

        bool operator==(iterator& other)
        {
            return _first_it == other._first_it &&
                _second_it == other._second_it;
        }

        bool operator!=(iterator& other)
        {
            return !(*this == other);
        }
    private:
        friend class TwoStateEdgeList;
        iterator(EdgeList* first, EdgeList* second, bool is_end)
        {
            assert(first && second);
            _first_end = first->end();
            _second_end = second->end();
            if ( !is_end ) {
                _first_it = first->begin();
                _second_it = second->begin();
            }else{
                _first_it = _first_end;
                _second_it = _second_end;
            }
        }

        void moveEnd()
        {
            _first_it = _first_end;
            _second_it = _second_end;
        }

        EdgeList::iterator _first_it, _second_it, _first_end, _second_end;
    };

    iterator begin()
    {
        iterator ret(&_permanent, getSecond(), false /* is_end */);
        return ret;
    }

    iterator end()
    {
        iterator ret(&_permanent, getSecond(), true);
        return ret;
    }

    static iterator emptyIterator()
    {
        static MemoryManager mm("Memory Manager for EdgeList::empty_iterator");
        static EdgeList* empty_list = new(mm) EdgeList(mm);
        iterator ret(empty_list, empty_list, false /* is_end */);
        return ret;
    }

private:
    friend class TwoStateOpndToEdgeListMap;
    friend class InequalityGraph;

    void setState(bool is_lower) { _is_lower = is_lower; }

    EdgeList* getSecond() { return getOneStateEdgeList(_is_lower); }

    EdgeList* getPermanentEdgeList() { return &_permanent; }

    EdgeList* getOneStateEdgeList(bool lower_state)
    {
        return lower_state ? &_lower : &_upper;
    }

    MemoryManager& _mm;
    EdgeList _permanent, _lower, _upper;
    bool _is_lower;
};

class TwoStateOpndToEdgeListMap {
    typedef StlMap<U_32, TwoStateEdgeList* > MapIdTo2stList;
public:
    TwoStateOpndToEdgeListMap(MemoryManager& mm) :
        _is_lower(false),
        _mm(mm),
        _map(mm)
    {}

    void setState(bool is_lower);

    bool isLowerState() { return _is_lower; }

    void addEdge(U_32 opnd_id, IneqEdge* edge);

    void addEdgeSingleState(U_32 opnd_id, IneqEdge *edge, bool is_lower);

    TwoStateEdgeList::iterator eListBegin(U_32 opnd_id) const;

    TwoStateEdgeList::iterator eListEnd(U_32 opnd_id) const;
private:
    friend class InequalityGraph;

    // return the corresponding list or NULL
    TwoStateEdgeList* get2stListByOpnd(IOpnd* opnd) const
    {
        MapIdTo2stList::const_iterator it = _map.find(opnd->getID());
        if ( it == _map.end() ) {
            return NULL;
        }
        return it->second;
    }

    bool _is_lower;
    MemoryManager& _mm;
    MapIdTo2stList _map;
};

class InequalityGraph {
public:
    typedef TwoStateEdgeList::iterator edge_iterator;
private:
    typedef StlMap<U_32, StlList<IneqEdge*> > OpndEdgeMap;

public:
    InequalityGraph(MemoryManager& mem_mgr) : 
        _mem_mgr(mem_mgr), 
        _id_to_opnd_map(mem_mgr),
        _edges(mem_mgr),
        _opnd_to_inedges_map_2st(mem_mgr),
        _opnd_to_outedges_map_2st(mem_mgr),
        _is_lower(false)
    {}

    // Inequality Graph can be visible in two states: lower and upper
    // In both states operands are the same. Some edges are visible in both
    // states, some only in one of two
    void setState(bool is_lower);

    bool isLowerState() { return _is_lower; }

    // add edge by operands visible in all states
    void addEdge(IOpnd* from, IOpnd* to, I_32 distance);

    // add edge by operand IDs visible in all states
    void addEdge(U_32 id_from, U_32 id_to, I_32 distance);

    // add edge by operand IDs visible in a given state
    void addEdgeSingleState(U_32 id_from, U_32 id_to, I_32 distance, bool is_lower);

    // add edge by operands visible in a given state
    void addEdgeSingleState(IOpnd* from, IOpnd* to, I_32 distance, bool is_lower);

    void addOpnd(IOpnd* opnd);

    edge_iterator inEdgesBegin(IOpnd* opnd) const;

    edge_iterator inEdgesEnd(IOpnd* opnd) const;

    edge_iterator outEdgesBegin(IOpnd* opnd) const;

    edge_iterator outEdgesEnd(IOpnd* opnd) const;

    void printDotFile(std::ostream& os) const;

    bool isEmpty() const { return _id_to_opnd_map.empty(); }

    IOpnd* findOpnd(U_32 id) const;

    MemoryManager& getMemoryManager() { return _mem_mgr; }
private:
    friend class InequalityOpndIterator;
    friend class InequalityGraphPrinter;
    typedef StlMap<U_32, IOpnd*> IdToOpndMap;

    static bool has_other_opnd_with_same_id(IdToOpndMap& map, IOpnd* opnd);

    void printDotHeader(std::ostream& os) const;
    void printDotBody(std::ostream& os) const;
    void printDotEnd(std::ostream& os) const;

    IOpnd* getOpndById(U_32 id) const;

    enum PrnEdgeType
    {
        tPERM_EDGE,
        tLOWER_EDGE,
        tUPPER_EDGE
    };

    void printEdge(std::ostream& os, IneqEdge* e, PrnEdgeType t) const;

    void prnDotEdgeList
        (std::ostream& os, IOpnd* from_opnd, 
         EdgeList* lst, PrnEdgeType type) const;

    void printListWithSetExcluded (std::ostream& os,
         StlSet<IneqEdge*>* edge_set, EdgeList* elist, PrnEdgeType ptype) const;

    MemoryManager& _mem_mgr;
    IdToOpndMap _id_to_opnd_map;
    EdgeList _edges;

    TwoStateOpndToEdgeListMap
        _opnd_to_inedges_map_2st, _opnd_to_outedges_map_2st;
    bool _is_lower;
};

class Bound : public HasBoundState {
public:
    Bound(I_32 bnd, const BoundState& bs) : HasBoundState(bs), _bound(bnd) {}

    // bound - I_32 -> Bound
    Bound(Bound* bound, I_32 val, const BoundState& bs);

    void printFullName(std::ostream& os);

    static bool leq(Bound* bound1, Bound* bound2);

    static bool eq(Bound* bound1, Bound* bound2);

    static bool leq_int32(Bound* bound1, I_32 value);

    static bool int32_leq(I_32 value, Bound* bound1);

    // returns (dst_val - src_val <= bound)
    static bool const_distance_leq(I_32 src_val, I_32 dst_val, Bound* bound);

private:
    friend class BoundAllocator;
    I_32 _bound;
};

class TrueReducedFalseChart;

class BoundAllocator {
public:
    BoundAllocator(MemoryManager& mem_mgr) : _mem_mgr(mem_mgr) {}

    Bound* create_inc1(Bound* bound);

    Bound* create_dec1(Bound* bound);

    Bound* create_dec_const(Bound* bound, I_32 cnst);

    TrueReducedFalseChart* create_empty_TRFChart();

private:
    friend class MemoizedDistances;

    MemoryManager& getMemoryManager() { return _mem_mgr; }

    Bound* newBound(I_32 val, const BoundState& bs);
    MemoryManager& _mem_mgr;
};

enum ProveResult {
    True = 2,
    Reduced = 1,
    False = 0
};

typedef ProveResult (*meet_func_t)(ProveResult, ProveResult);

class TrueReducedFalseChart {
public:
    TrueReducedFalseChart() :
        _max_false(NULL),
        _min_true(NULL),
        _min_reduced(NULL),
        _bound_alloc(NULL)
    {assert(0);}

    TrueReducedFalseChart(BoundAllocator* alloc) :
        _max_false(NULL),
        _min_true(NULL),
        _min_reduced(NULL),
        _bound_alloc(alloc)
    {}

    void addFalse(Bound* f_bound);

    void addReduced(Bound* r_bound);

    void addTrue(Bound* t_bound);

    bool hasBoundResult(Bound* bound) const;

    ProveResult getBoundResult(Bound* bound) const;

    Bound* getMaxFalseBound() { return _max_false; }

    Bound* getMinTrueBound() { return _min_true; }

    Bound* getMinReducedBound() { return _min_reduced; }

    void print(std::ostream& os) const;

private:
    void printBound(Bound* b, std::ostream& os) const;

    void clearRedundantReduced();

    Bound *_max_false, *_min_true, *_min_reduced;
    BoundAllocator* _bound_alloc;
};

class MemoizedDistances {
public:
    MemoizedDistances(BoundAllocator& alloc) :
        _bound_alloc(alloc),
        _map(_bound_alloc.getMemoryManager())
    {}

    void makeEmpty();

    // set [dest - source <= bound]
    void updateLeqBound(IOpnd* dest, Bound* bound, ProveResult res);

    bool hasLeqBoundResult(IOpnd* dest, Bound* bound) const;

    // returns [dest - source <= bound]
    //      that is True, Reduced or False
    ProveResult getLeqBoundResult(IOpnd* dest, Bound* bound) const;

    bool minTrueDistanceLeqBound(IOpnd* dest, Bound* bound);

    bool maxFalseDistanceGeqBound(IOpnd* dest, Bound* bound);

    bool minReducedDistanceLeqBound(IOpnd* dest, Bound* bound);

    void print(std::ostream& os) const;

private:
    void initOpnd(IOpnd* op);

    typedef StlMap<IOpnd*, TrueReducedFalseChart> OpndToTRFChart;
    BoundAllocator& _bound_alloc;
    OpndToTRFChart _map;
};

class ActiveOpnds {
typedef StlMap<IOpnd*, Bound*>::const_iterator iter_t;
public:
    ActiveOpnds(MemoryManager& mem_mgr) : _map(mem_mgr) {}

    void makeEmpty() { _map.clear(); }

    bool hasOpnd(IOpnd* opnd) const { return _map.find(opnd) != _map.end(); }

    Bound* getBound(IOpnd* opnd) const;

    void setBound(IOpnd* opnd, Bound* bound) { _map[opnd] = bound; }

    void clearOpnd(IOpnd* opnd) { _map.erase(_map.find(opnd)); }

    void print(std::ostream& os) const;

private:
    StlMap<IOpnd*, Bound*> _map;
};

class ClassicAbcdSolver {
public:
    ClassicAbcdSolver(InequalityGraph& i, MemoryManager& solver_mem_mgr) :
        _igraph(i),
        _source_opnd(NULL),
        _bound_alloc(solver_mem_mgr),
        _mem_distance(_bound_alloc),
        _active(solver_mem_mgr)
    {}

    bool demandProve
        (IOpnd* source, IOpnd* dest, I_32 bound_int, bool prove_upper_bound);

private:
    ProveResult prove(IOpnd* dest, Bound* bound, U_32 prn_level);

    void updateMemDistanceWithPredecessors
        (IOpnd* dest, Bound* bound, U_32 prn_level, meet_func_t meet_f);

    class Printer {
    public:
        Printer(U_32 level, std::ostream& os) : _level(level), _os(os) {}

        void prnLevel();

        void prnStr(const char* str);

        void prnStrLn(const char* str);

    private:
        U_32 _level;
        std::ostream& _os;
    };

    InequalityGraph& _igraph;
    IOpnd* _source_opnd;
    BoundAllocator _bound_alloc;
    MemoizedDistances _mem_distance;
    ActiveOpnds _active;
};

int classic_abcd_test_main();

} //namespace Jitrino

#endif /* _CLASSIC_ABCD_SOLVER_H */
