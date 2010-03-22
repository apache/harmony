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
#ifndef __RECOMPUTE_H__
#define __RECOMPUTE_H__

#include <assert.h>
#include <string.h>
#include "../java5/context_5.h"
#include "x_class_interface.h"


vf_Result vf_recompute_stackmaptable(Method_Handle method, U_8** attrBytes, char** error, void* trustedPairs);

//possible relations between verificaton types
enum ConstraintType_Ex {
    CTX_REVERSED_GENERIC = 4,
    CTX_REVERSED_ARRAY2REF = 5,
    CTX_POSSIBLE_STACKMAP = 6
};

struct ReversedGenericCnstr : Constraint {
    ReversedGenericCnstr *next() {
        return (ReversedGenericCnstr *) Constraint::next(Constraint::next(), CTX_REVERSED_GENERIC);
    }
};

struct ReversedArrayCnstr : Constraint {
    ReversedArrayCnstr *next() {
        return (ReversedArrayCnstr *) Constraint::next(Constraint::next(), CTX_REVERSED_ARRAY2REF);
    }
};

struct StackmapAttrCnstr : Constraint {
    int depth;

    StackmapAttrCnstr *next() {
        return (StackmapAttrCnstr *) Constraint::next(Constraint::next(), CTX_POSSIBLE_STACKMAP);
    }
};

struct StackmapElement_Ex : StackmapElement_5 { //TODO: should be rewritten to save footprint
    ReversedGenericCnstr *firstReversedGenericCnstr() {
        return (ReversedGenericCnstr*)Constraint::next(others, CTX_REVERSED_GENERIC);
    }

    ReversedArrayCnstr *firstReversedArrayCnstr() {
        return (ReversedArrayCnstr*)Constraint::next(others, CTX_REVERSED_ARRAY2REF);
    }

    StackmapAttrCnstr *firstStackmapAttrCnstr() {
        return (StackmapAttrCnstr*)Constraint::next(others, CTX_POSSIBLE_STACKMAP);
    }

    int newReversedGenericCnstr_safe(Memory *mem, StackmapElement_5 *to) {
        ReversedGenericCnstr *rgen = firstReversedGenericCnstr();
        while( rgen ) {
            if( rgen->variable == to ) return false;
            rgen = rgen->next();
        }
        newConstraint(mem, CTX_REVERSED_GENERIC)->variable = to;
        return true;
    }

    int newReversedArrayCnstr_safe(Memory *mem, StackmapElement_5 *to) {
        ReversedArrayCnstr *rarr = firstReversedArrayCnstr();
        while( rarr ) {
            if( rarr->variable == to ) return false;
            rarr = rarr->next();
        }
        newConstraint(mem, CTX_REVERSED_ARRAY2REF)->variable = to;
        return true;
    }

    void newStackmapAttrCnstr(Memory *mem, SmConstant value) {
        StackmapAttrCnstr *sm = (StackmapAttrCnstr *)mem->malloc(sizeof(StackmapAttrCnstr));

        sm->nxt = others;
        sm->type = (ConstraintType)CTX_POSSIBLE_STACKMAP;
        sm->value = value.c;
        sm->depth = 0;

        others = sm;
    }

    void removeIncoming(IncomingType *inc) {
        IncomingType *ptr = firstIncoming();
        assert(ptr);

        if( inc == ptr ) {
            POINTER_SIZE_INT mask = (POINTER_SIZE_INT)incoming & 3;
            incoming = (IncomingType *) ((POINTER_SIZE_INT)inc->next() | mask);
        } else {
            while( ptr->next() != inc ) {
                ptr = ptr->next();
                assert(ptr);
            }
            ptr->nxt = inc->nxt;
        }
    }

    void removeOther(Constraint *o) {
        Constraint *ptr = firstOthers();
        assert(ptr);

        if( o == ptr ) {
            others = o->next();
        } else {
            while( ptr->next() != o ) {
                ptr = ptr->next();
                assert(ptr);
            }
            ptr->nxt = o->nxt;
        }
    }

};

struct RefInfo {
    union {
        SmConstant *supercls;
        unsigned    mask;
    };

    void init() {
        mask = 0xFFFFFFFF;
    }

    int is_being_calculated() {
        return mask == 0xFFFFFFFE;
    }

    void set_being_calculated() {
        mask = 0xFFFFFFFE;
    }

    int is_calculated() {
        return mask != 0xFFFFFFFF && !is_being_calculated();
    }

    int is_interface() {
        return !supercls;
    }

    void set_interface() {
        supercls = 0;
        *((SmConstant*)&mask) = SM_NONE;
    }

    SmConstant *superclasses() {
        //always return valis pointer
        return supercls ? supercls : (SmConstant*)&mask;
    }

    void set_superclasses(SmConstant *s) {
        mask = 0; //just in case
        supercls = s;
    }
};

struct TrustedPair {
    SmConstant from;
    SmConstant to;
};

//
// Context - main class of Type Checker
//

class vf_Context_5e : public vf_Context_5 {
public:
    vf_Context_5e(SharedClasswideData &classwide, void *_unparsedPairs) :
      vf_Context_5(classwide), parsedTrustedData(0), parsedDataSz(0), unparsedPairs(_unparsedPairs)
      {
          //we would like to flush StackMapTable attribute from this method
          stackmapattr_calculation = true;
      }

    vf_Result recompute_stackmaptable(Method_Handle method);

    ~vf_Context_5e() {
        tc_free(parsedTrustedData);
    }
        
    U_8* written_stackmap;

protected:
    void writeStackMapFrame(Address instr);
    vf_Result fillCurFrame(PropsHead_5 *head);
    void writeStackMapFrame_Full(Address offset);
    void writeStackMapFrame_SameLocalsOneStack(Address offset);
    void writeStackMapFrame_Same(Address offset);
    void writeStackMapFrame_Cut(Address offset, int attr_delta, int workmap_delta);
    void writeStackMapFrame_Append(Address offset, int attr_delta, int workmap_delta);
    void writeStackMapElements(Address start, U_32 cnt);

    void writeByte(uint16 byte) {
        assert(byte < 256);
        if( !written_stackmap ) {
            attrLen = 2; // reserve uint16 for number of enries
            attrSz = 0;
        }



        if( attrLen + 6 >= attrSz ) {
            attrSz += 4096;
            written_stackmap = (U_8*) tc_realloc(written_stackmap, attrSz);
        }

        written_stackmap[attrLen + 6] = (U_8) byte;
        attrLen++;
    }


    unsigned lastLocalsNo;
    int lastInstr;
    WorkmapHead *curFrame;
    ///////////////////////
    vf_Result do_recompute();
    vf_Result mantain_node_consistency(StackmapElement_5 *el);
    vf_Result arc_consistensy_in_node(StackmapElement_5 *el, int depth);
    vf_Result mantain_arc_consistency(int depth);
    vf_Result calculate_subgraph(StackmapElement_5 *el);
    vf_Result do_backtracking(int depth);

    void push_subgraph(StackmapElement_5 *el);
    void insert_back_refs(StackmapElement_5 *el);

    SmConstant get_node_value(PropsHead_5 *head, int i) {
        StackmapElement_Ex *el;

        if( head->is_workmap() ) {
            WorkmapHead *w = head->getWorkmap();
            if( !w->elements[i].isVariable() ) {
                return w->elements[i].getConst();
            }

            el = (StackmapElement_Ex*)(w->elements[i].getVariable());
        } else {
            el = (StackmapElement_Ex*)(&head->getStackmap()->elements[i]);
        }

        if( is_node_calculated(el) ) {
            return el->firstIncoming()->value;
        }
        assert(no_stackmap_choice(el));

        for( StackmapAttrCnstr *sm = el->firstStackmapAttrCnstr(); sm; sm=sm->next() ) {
            if( !sm->depth ) {
                return sm->value;
            }
        }
        assert(0);
        return SM_BOGUS;
    }

    int is_node_calculated(StackmapElement_5 *el) {
        assert(el->firstIncoming());
        //assert( el->firstIncoming()->next() || !SmConstant(el->firstIncoming()->value).isReference() || class_get_cp_class_entry(k_class, tpool.sm_get_refname(el->firstIncoming()->value)));
        return !el->firstIncoming()->next();
    }

    int no_stackmap_choice(StackmapElement_Ex *el) {
        assert(el->firstStackmapAttrCnstr());
        int stackmapcnt = 0;
        for( StackmapAttrCnstr *sm = el->firstStackmapAttrCnstr(); sm; sm=sm->next() ) {
            if( !sm->depth ) {
                stackmapcnt++;
                if( stackmapcnt == 2 ) return false;
            }
        }
        assert(stackmapcnt == 1);
        //assert( class_get_cp_class_entry(k_class, tpool.sm_get_refname(el->firstStackmapAttrCnstr()->value)));
        return true;
    }

    SmConstant get_object_array(int dimension) {
        return dimension ? get_ref_array(dimension, "java/lang/Object") : tpool.sm_get_const_object();
    }

    SmConstant get_ref_array(int dimension, SmConstant ref) {
        return dimension ? get_ref_array(dimension, tpool.sm_get_refname(ref)) : ref;
    }

    SmConstant get_ref_array(int dimension, const char* ref_name) {
        assert(dimension);
        int name_len = (int)strlen(ref_name);

        char *name = (char*)mem.malloc(name_len + dimension + 2);
        tc_memset(name, '[', dimension);
        name[dimension] = 'L';
        tc_memcpy(name + dimension + 1, ref_name, name_len);
        name[dimension + name_len + 1] = ';';

        SmConstant ret = tpool.get_ref_type(name, name_len + dimension + 2);
        mem.dealloc_last(name, name_len + dimension + 2);
        return ret;
    }


    SmConstant get_zerodim(SmConstant value) {
        const char* name = tpool.sm_get_refname(value);
        if( name[0] != '[' ) return value;

        while( name[0] == '[' ) name++;
        return tpool.get_type(name);
    }

    unsigned array_dims(SmConstant ref) {
        const char* start = tpool.sm_get_refname(ref);
        const char* name = start;
        while( name[0] == '[' ) name++;
        return (unsigned)(name-start);
    }

    int isObjectOrInterface(SmConstant ref) {
        if( ref == tpool.sm_get_const_object() ) return true;

        Class_Handle h = tpool.sm_get_handler(ref);
        if( h && h != CLASS_NOT_LOADED ) return class_is_interface(h);

        parseTrustedData(ref);
        return parsedTrustedData[ref.getReferenceIdx()].is_interface();
    }

    int knownly_assignable(SmConstant from, SmConstant to, int array_element = 0) {
        if( from == to && !array_element ) return true;

        unsigned from_dims = array_dims(from);
        unsigned to_dims = array_dims(to) + (array_element ? 1 : 0);

        if( to_dims > from_dims ) return false;
        if( to_dims < from_dims ) return isObjectOrInterface( get_zerodim(to) );

        from = get_zerodim(from);
        to = get_zerodim(to);

        if( isObjectOrInterface(to) ) return true;

        parseTrustedData(from); // it may change class_handler for 'to'

        Class_Handle t = tpool.sm_get_handler(to);
        assert(t);

        for( SmConstant *sm = parsedTrustedData[from.getReferenceIdx()].superclasses(); *sm != SM_NONE; sm++ ) {
            if( *sm == to ) {
                return true;
            }

            if( t != CLASS_NOT_LOADED ) {
                Class_Handle f = tpool.sm_get_handler(*sm);
                if( f != CLASS_NOT_LOADED && vf_is_extending(f, t) ) return true;
            }
        }
        return false;
    }


    int check_possible_stackmap(StackmapElement_5 *el, SmConstant sm) {
        for( IncomingType *inc = el->firstIncoming()->next(); inc; inc = inc->next()) {
            if( !knownly_assignable(inc->value, sm) ) {
                //bad stackmap element
                return false;
            }
        }

        for( ExpectedType *exp = el->firstExpected(); exp; exp = exp->next() ) {
            if( !knownly_assignable(sm, exp->value) ) {
                //bad stackmap element
                return false;
            }
        }
        return true;
    }

    int is_arc(Constraint *c) {
        return is_direct_arc(c) || is_reversed_arc(c);
    }

    int is_direct_arc(Constraint *c) {
        return c->type == CT_GENERIC || c->type == CT_ARRAY2REF;
    }

    int is_reversed_arc(Constraint *c) {
        return (ConstraintType_Ex)c->type == CTX_REVERSED_GENERIC || (ConstraintType_Ex)c->type == CTX_REVERSED_ARRAY2REF;
    }


    void reduceTryBlocks(Address dead_code_start, Address dead_code_end) {
        uint16 start_pc;
        uint16 end_pc;
        uint16 handler_pc;
        uint16 handler_cp_index;

        for( uint16 idx = 0; idx < m_handlecount; idx++ ) {
            method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc, &handler_pc, &handler_cp_index );

            if( start_pc >= dead_code_start && end_pc <= dead_code_end ) {
                method_remove_exc_handler( m_method, idx );
                idx--;
            } else if( end_pc > dead_code_start && end_pc <= dead_code_end ) {
                method_modify_exc_handler_info( m_method, idx, start_pc, dead_code_start, handler_pc, handler_cp_index );
            } else if( start_pc >= dead_code_start && start_pc < dead_code_end  ) {
                method_modify_exc_handler_info( m_method, idx, dead_code_end, end_pc, handler_pc, handler_cp_index );
            }
        }
    }

    void parseTrustedData(SmConstant value, int knownly_interface = false);
    void parseTrustedPairs();

    void tryResolve(SmConstant ref) {
        vf_ValidType *f = tpool.getVaildType(ref.getReferenceIdx());
        if( !f->cls ) {
            f->cls = vf_resolve_class(k_class, f->name, false);
            if( !f->cls ) f->cls = CLASS_NOT_LOADED;
        }
    }


    FastStack<StackmapElement_5*> arc_stack;
    FastStack<StackmapElement_5*> subgraph_stack;
    FastStack<SmConstant> class_stack;
    
    RefInfo *parsedTrustedData;
    int parsedDataSz;

    TrustedPair* trustedPairs;
    int trustedPairsCnt;
    void* unparsedPairs;


    U_32 attrLen;
    U_32 attrSz;
};



#define ITERATE_THRU_STACKMAP_VECTORS(CODE)                                     \
for( i = 0; i < props.hash_size; i++ ) {                                        \
    PropsHead_5 *pro = (PropsHead_5*)(props.propHashTable[i]);                  \
    while(pro) {                                                                \
        if( !pro->is_workmap() ) {                                              \
            StackmapHead *sm = pro->getStackmap();                              \
                                                                                \
            for( unsigned j = 0; j < m_stack_start + sm->depth; j++ ) {         \
                /*skip uninit flag*/                                            \
                if( j == m_max_locals && j < m_stack_start ) continue;          \
                                                                                \
                StackmapElement_5 *el = &(sm->elements[j]);                     \
                CODE;                                                           \
            }                                                                   \
        }                                                                       \
        pro=(PropsHead_5 *)pro->next;                                           \
    }                                                                           \
}                                                                               \

#endif
