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
#include <stdio.h>
#include "recompute.h"
#include "../java5/stackmap_5.h"
#include "time.h"

/**
 * Recomputes StackMapTable attribute.
 */
vf_Result
vf_recompute_stackmaptable(Method_Handle method, U_8** attrBytes, char** error,
                           void* trustedPairs )
{
    Class_Handle klass = method_get_class(method);
    vf_Result result = VF_OK;

    // Create context
    SharedClasswideData classwide(klass);
    vf_Context_5e context(classwide, trustedPairs);

    // Verify method with Java5 verifier
    result = context.recompute_stackmaptable(method);

    if (result != VF_OK) {
        vf_create_error_message(method, context, error);
    }

    *attrBytes = context.written_stackmap;
    return result;
} // vf_recompute_stackmaptable

vf_Result vf_Context_5e::recompute_stackmaptable(Method_Handle method ) {
    written_stackmap = 0;

    //nothing to verify
    if( !method_get_bytecode_length( method ) ) {
        return VF_OK;
    }


    vf_Result tcr;
    int entryNo = 0;
    parsedDataSz = 0;

    // Verify method with Java5 verifier
    if( (tcr=verify_method(method)) != VF_OK) {
        return tcr;
    }

    if( (tcr=do_recompute()) != VF_OK) {
        return tcr;
    }

    create_method_initial_workmap();
    lastLocalsNo = m_max_locals;
    while( lastLocalsNo ) {
        if( workmap->elements[lastLocalsNo-1].getConst() != SM_BOGUS ) {
            break;
        }
        lastLocalsNo--;
    }
    lastInstr = -1;

    curFrame = newWorkmap(m_stack_start + m_max_stack);
    for( Address instr = 0; instr < m_code_length; instr++) {
        PropsHead_5 *head = (PropsHead_5*)props.getInstrProps(instr);
        if (head) {
            if( (tcr=fillCurFrame(head)) != VF_OK ) {
                return tcr;
            }
            entryNo++;
            assert(instr == head->instr);
            writeStackMapFrame(head->instr);

            /////////////////////////////////
            workmap->depth = curFrame->depth;
            tc_memcpy(&workmap->elements[0], &curFrame->elements[0],
                sizeof(WorkmapElement_5) * (m_stack_start + workmap->depth));
        }
    }

    if( written_stackmap ) {
        written_stackmap[7] = (U_8) entryNo & 0xFF;
        entryNo >>= 8;

        written_stackmap[6] = (U_8) entryNo & 0xFF;
        ////////////
        written_stackmap[5] = (U_8) attrLen & 0xFF;
        attrLen >>= 8;

        written_stackmap[4] = (U_8) attrLen & 0xFF;
        attrLen >>= 8;

        written_stackmap[3] = (U_8) attrLen & 0xFF;
        attrLen >>= 8;

        written_stackmap[2] = (U_8) attrLen & 0xFF;
    }

    return VF_OK;
} // recompute_stackmaptable

vf_Result vf_Context_5e::fillCurFrame(PropsHead_5 *head) {
    curFrame->depth = head->is_workmap() ? head->getWorkmap()->depth : head->getStackmap()->depth;
    
    int high_word_expected = false;

    for( unsigned i = 0; i < m_stack_start + curFrame->depth; i++ ) {
        if( i == m_max_locals && i < m_stack_start ) continue;

        SmConstant value = get_node_value(head, i);

        if( high_word_expected && value != SM_HIGH_WORD ) {
            curFrame->elements[i-1].const_val = SmConstant(SM_BOGUS);
        }

        high_word_expected = value.isLongOrDouble();
        curFrame->elements[i].const_val = value.isLongOrDouble() && (i + 1 == m_max_locals || i + 1 == m_stack_start + curFrame->depth) ? SM_BOGUS : value;
    }
    return VF_OK;
}

vf_Result vf_Context_5e::do_recompute( ) {
    parseTrustedPairs();

    vf_Result tcr;

    // iterator
    int i;

    //we skip workmap vectors when do iteration
    //workmap vectors may contain constant, refs to stackmap elements that are parts of other vectrs
    //(and thus iterated thru), or temporary stackmap elements created by 
    //iterate thru all stackmaps
    ITERATE_THRU_STACKMAP_VECTORS({
        //insert back refs to split to sub-graphs later
        if( (tcr = mantain_node_consistency(el)) != VF_OK ) {
            return tcr;
        }
        insert_back_refs(el);
    });


    if( (tcr = mantain_arc_consistency(0)) != VF_OK ) {
        return tcr;
    }

    //iterate thru all stackmaps
    ITERATE_THRU_STACKMAP_VECTORS({
        //fully calculate subgraph that contains this node
        //if it's not yet calculated
        if( (tcr = calculate_subgraph(el)) != VF_OK ) {
            return tcr;
        }
    });


    for( Address instr = 0; instr < m_code_length; instr++ ) {
        if( props.isDeadCodeStart(instr) ) { //dead block begins with 01, middles are 11, ends with 10 or 00
            Address dead_code_start = instr;
            //replace dead code with {NOP, NOP, ..., NOP, ATHROW}
            while( !props.isDataflowPassed(instr) ) {
                m_bytecode[instr++] = OP_NOP;
            }
            m_bytecode[instr-1] = OP_ATHROW;


            //dead code start: place workmap here
            assert(!props.getInstrProps(dead_code_start));

            PropsHead_5* dead = newWorkmapProps(m_stack_start + 1);
            props.setInstrProps(dead_code_start, dead);
            WorkmapHead *wm = dead->getWorkmap();

            PropsHead_5* alive = (PropsHead_5*)props.getInstrProps(instr);
            assert( alive );

            for( unsigned i = 0; i < m_stack_start; i++ ) {
                wm->elements[i].const_val = get_node_value(alive, 1);
            }

            wm->depth = 1;
            wm->elements[m_stack_start].const_val = SmConstant(SM_NULL);

            reduceTryBlocks(dead_code_start, instr);
        }
    }
    return VF_OK;
}


//insert reversed generic constraints
void vf_Context_5e::insert_back_refs(StackmapElement_5 *el) {
    //stackmap is know already for this element ==> it won't participae in backtracking
    if (is_node_calculated(el)) return;

    GenericCnstr *gen = el->firstGenericCnstr();
    while( gen ) {
        ((StackmapElement_Ex*)(gen->variable))->newReversedGenericCnstr_safe(&mem, el);
        gen = gen->next();
    }

    //add incoming value if it does not have
    ArrayCnstr *arr = el->firstArrayCnstr();
    if ( arr && ((StackmapElement_Ex*)(arr->variable))->newReversedArrayCnstr_safe(&mem, el) ) {
        //we need this recursion to cover temporary values created at AALOAD instr
        //since they are not covered by ITERATE_THRU_STACKMAP_VECTORS
        insert_back_refs(arr->variable);
    }
}

//calculates simple cases: if we can define for sure least general superclass to all incoming classes
//or we deal with primitives, we replease all incoming types with "the right" type which will be recorded
//to the attribute later
//
//otherwise we build a list of candidates (probably containing just single element though)
//a difference between list of candidates of single element and reliable "the right" type is that
//when we have more classes loaded, list of candidates might be wider, but the "right" type would be the same
//this difference is intended and maintained for future use, e.g. if we gt instrumentation and can't
//build attribute for the class, we may consider further algorithm improving
vf_Result vf_Context_5e::mantain_node_consistency(StackmapElement_5 *el) {

    vf_Result tcr;

    //first we call this function for ArrayConstraint target since they are not accessible by ITERATE_THRU_STACKMAP_VECTORS
    ArrayCnstr *arr = el->firstArrayCnstr(); 
    if( arr && (tcr = mantain_node_consistency(arr->variable)) != VF_OK ) {
        return tcr;
    }

    assert( el->firstIncoming() );
    assert(!((StackmapElement_Ex*)el)->firstStackmapAttrCnstr());

    if( el->firstIncoming()->value == SM_THISUNINIT && !el->firstIncoming()->next() ) {
        //preserve uninits (don't replace with BOGUS even if nobody's checking this local) 
        //for proper calculation of the flag
        return VF_OK;
    }

    if( !el->firstExpected() && !el->firstGenericCnstr() && !el->firstArrayCnstr() ) {
        //it's safe to put it before "if( !el->firstIncoming()->next() ) return VF_OK;"
        //since we treat as target as multiway and thus each constant remains constant 
        //in Java6 verification
        //(we don't record expected values for constants that was the risk)
        
        //otherwise: nobody cares
        el->firstIncoming()->value.c = SM_BOGUS;
        el->firstIncoming()->nxt = 0;
        return VF_OK;
    }

    if( !el->firstIncoming()->next() ) return VF_OK;

    //MORE THAN ONE incoming element exists

    int array_of_primitive_exists = false;
    int assignable_from_object_exists = false;

    unsigned max_arr_dims = 0;
    unsigned min_arr_dims = 0xFFFFFFFF;

    //first we remove NULLs from incoming values, check whether non-references exist among incomings
    //see arrays and compare their dimensions, see if there is Object or arrays of Object among incoming
    for( IncomingType *inc = el->firstIncoming(); inc; inc = inc->next() ) {
        //we don't use isNonMergeable() here since there are different merging rules for Java5 and Java6
        if( inc->value == SM_NULL ) {
            //remove SM_NULLs
            ((StackmapElement_Ex*)el)->removeIncoming(inc);
        } else if( !SmConstant(inc->value).isReference() ) {
            //we can merge objects only: all other types produce SM_BOGUS
            el->firstIncoming()->value.c = SM_BOGUS;
            el->firstIncoming()->nxt = 0;
            return VF_OK;
        } else {
            //if there are arrays of differetn dimensions incoming ==> we know how to handle this, see below
            unsigned dims = array_dims(inc->value);
            max_arr_dims = max_arr_dims < dims ? dims : max_arr_dims;
            min_arr_dims = min_arr_dims > dims ? dims : min_arr_dims;

            //if inerface or object is coming
            SmConstant z = get_zerodim(inc->value);

            if( z.isPrimitive()) {
                array_of_primitive_exists = true;
            } else {
                assignable_from_object_exists |= isObjectOrInterface(z);
            }
        }
    }

    //after removing of NULL only a single element remained?
    if( !el->firstIncoming()->next() ) return VF_OK;

    if( max_arr_dims != min_arr_dims || assignable_from_object_exists ) {
        //incoming types contain arrays of different dimensions
        //we can calculae most general type: array of dimension min_arr_dims and element type java/lang/Object

        //if all dimensions are the same but there is an object or interface there
        //we also can calculate
        el->firstIncoming()->value = get_object_array(min_arr_dims);
        el->firstIncoming()->nxt = 0;
        return VF_OK;
    }

    if( array_of_primitive_exists ) {
        //there is a mix of arrays of primitive and other arrays of the same dimensions
        assert(min_arr_dims);
        //reduce dimensions by one and create array ob objects
        el->firstIncoming()->value = get_object_array(min_arr_dims - 1);
        el->firstIncoming()->nxt = 0;
        return VF_OK;
    }

    //////////////////////////////// NOW WE CALCULATE BACKTRACKING DATA //////////////////////////////

    //first we reduce set of expected values: we remove interfaces, Objects, all primitive types, and arrays of objects
    //(if we have array of less dimension than incoming type than its elements are objects or interfaces)
    for( ExpectedType *exp = el->firstExpected(); exp; exp=exp->next() ) {
        SmConstant value = exp->value;
        if( !value.isReference() || array_dims(value) < min_arr_dims || isObjectOrInterface(get_zerodim(value)) ) {
            ((StackmapElement_Ex*)el)->removeOther(exp);
        }
    }


    //now we need to create a set of possible stackmap solutions
    //S={s_i} so that each s_i is a super class of each incoming and a sub class of each expected value
    SmConstant sm_value = el->firstIncoming()->value;
    SmConstant z = get_zerodim(sm_value);

    parseTrustedData(z);
    int possible_stackmap_cnt = 0;
    for( SmConstant *sm = parsedTrustedData[z.getReferenceIdx()].superclasses(); *sm != SM_NONE; sm++ ) {
        sm_value = get_ref_array(min_arr_dims, *sm);
        if( check_possible_stackmap(el, sm_value) ) {
            ((StackmapElement_Ex*)el)->newStackmapAttrCnstr(&mem, sm_value);
            possible_stackmap_cnt++;
        }
    }

    if( !possible_stackmap_cnt ) {
        return error(VF_ErrorInternal, "not enough data to build stackmaptable attr: need to load more classes");
    }

    //now we reduce the set by eliminating those classes that have subclasses in this set
    if( possible_stackmap_cnt > 1 ) {
        for( StackmapAttrCnstr* sm1 = ((StackmapElement_Ex*)el)->firstStackmapAttrCnstr(); sm1; sm1 = sm1->next() ) {
            for( StackmapAttrCnstr* sm2 = sm1->next(); sm2; sm2 = sm2->next() ) {

                if( knownly_assignable(sm1->value, sm2->value) ) {
                    //remove this stackmap element, since its subclass exists in the set
                    ((StackmapElement_Ex*)el)->removeOther(sm2);
                    break;
                }

                if( knownly_assignable(sm2->value, sm1->value) ) {
                    //replace sm1 with sm2 and remove original sm2 
                    //i.e. remove sm1 actually, since its subclass exists in the set
                    sm1->value = sm2->value; 
                    assert(sm1->depth == sm2->depth);
                    ((StackmapElement_Ex*)el)->removeOther(sm2);
                    break;
                }
            }
        }
    }

    arc_stack.push(el);

    return VF_OK;
}


vf_Result vf_Context_5e::mantain_arc_consistency(int depth) {
    vf_Result tcr;
    while( !arc_stack.is_empty() ) {
        if( (tcr=arc_consistensy_in_node( arc_stack.pop(), depth)) != VF_OK ) {
            return tcr;
        }
    }
    return VF_OK;
}

//check that each stackmap candidates in the nodes accessible from the current one
//has an arc-consitent value in the current mode 
//the method is called each time set of candidates for the given node is changed
vf_Result vf_Context_5e::arc_consistensy_in_node(StackmapElement_5 *el, int depth) {

    for( Constraint *adjacent = el->firstOthers(); adjacent; adjacent = adjacent->next() ) {
        if( !is_arc(adjacent) ) continue;

        int possible_stackmap_cnt = 0;
        int changed = false;

        for( StackmapAttrCnstr *sm2 = ((StackmapElement_Ex*)adjacent->variable)->firstStackmapAttrCnstr(); sm2; sm2 = sm2->next()) {

            //is a stackmap and not removed
            if( !sm2->depth ) {

                StackmapAttrCnstr *sm_this = ((StackmapElement_Ex*)el)->firstStackmapAttrCnstr();
                for(; sm_this; sm_this=sm_this->next() ) {
                    //temporarily removed element?
                    if( sm_this->depth ) continue;

                    int valid_arc_found = is_direct_arc(adjacent) ? 
                        knownly_assignable(sm_this->value, sm2->value, adjacent->type == CT_ARRAY2REF) :
                        knownly_assignable(sm2->value, sm_this->value, (int)adjacent->type == (int)CTX_REVERSED_ARRAY2REF);

                    if( valid_arc_found ) {
                        //stackmap has a superclass in a branch target ==> this arc is OK, check next one
                        possible_stackmap_cnt++;
                        break;
                    }
                }

                if( !sm_this ) {
                    //no valid arcs - remove possible stackmap
                    if( depth ) {
                        //remove temporarily
                        sm2->depth = depth;
                    } else {
                        //remove permanently {
                        ((StackmapElement_Ex*)adjacent->variable)->removeOther(sm2);
                    }
                    changed = true;
                }
            }
        }

        if( changed ) {
            if( !possible_stackmap_cnt ) {
                return depth ? VF_ErrorInternal : 
                error(VF_ErrorInternal, "not enough data to build stackmaptable attr: need to load more classes");
            }
            arc_stack.push( adjacent->variable );
        }
    }
    return VF_OK;
}

void vf_Context_5e::push_subgraph(StackmapElement_5 *el) {
    if( subgraph_stack.instack( el ) ) return;

    subgraph_stack.push( el );

    for( Constraint *adjacent = el->firstOthers(); adjacent; adjacent = adjacent->next() ) {
        if( is_arc(adjacent) ) push_subgraph(adjacent->variable);
    }
}


vf_Result vf_Context_5e::calculate_subgraph(StackmapElement_5 *el) {
    if( is_node_calculated(el) || no_stackmap_choice((StackmapElement_Ex*)el) ) return VF_OK;

    subgraph_stack.init();

    //push all elements accessible from el on stack
    push_subgraph(el);

    //TODO: remove calculated nodes

    //do backtracking
    return do_backtracking(0);
}

vf_Result vf_Context_5e::do_backtracking(int old_depth) {
    if( old_depth == subgraph_stack.get_depth() ) {
        //put breakpoint here
        //solution found!!!
        return VF_OK;
    }

    StackmapElement_Ex *cur = (StackmapElement_Ex*)subgraph_stack.at(old_depth);
    int depth = old_depth + 1;

    if( is_node_calculated(cur) || no_stackmap_choice((StackmapElement_Ex*)cur) ) {
        return do_backtracking(depth);
    }

    //first 'remove' all remaining stackmaps
    StackmapAttrCnstr *sm;
    for( sm = cur->firstStackmapAttrCnstr(); sm; sm = sm->next() ) {
        assert(sm->depth < depth);
        if( !sm->depth ) sm->depth = depth;
    }

    //then try to include them one-by-one 
    for( sm = cur->firstStackmapAttrCnstr(); sm; sm = sm->next() ) {
        if( sm->depth != depth ) continue;

        sm->depth = 0;
        arc_stack.push(cur);
        if( mantain_arc_consistency(depth) == VF_OK && do_backtracking(depth) == VF_OK ) {
            //solution has been found
            return VF_OK;
        }

        //the value is inconsistent ==> do cleanup
        for( int i = depth; i < subgraph_stack.get_depth(); i++) {
            for( StackmapAttrCnstr *sm = ((StackmapElement_Ex*)subgraph_stack.at(i))->firstStackmapAttrCnstr(); sm; sm = sm->next() ) {
                assert(sm->depth <= depth + 1);
                if( sm->depth >= depth ) sm->depth = 0;
            }
        }
        sm->depth = depth;
    }
    return depth ? VF_ErrorInternal : error(VF_ErrorInternal, "not enough data to build stackmaptable attr: need to load more classes");
}

void vf_Context_5e::parseTrustedData(SmConstant value, int knownly_interface) {
    int idx = value.getReferenceIdx();

    if( idx >= parsedDataSz ) {
        parsedTrustedData = (RefInfo*)tc_realloc(parsedTrustedData, (idx+1)*sizeof(RefInfo));
        for( int i = parsedDataSz; i <= idx; i++ ) {
            parsedTrustedData[i].init();
        }
        parsedDataSz = idx + 1;
    }

    if( parsedTrustedData[idx].is_calculated() ) return;

    if( parsedTrustedData[idx].is_being_calculated() || knownly_interface) {
         //already on stack
        parsedTrustedData[idx].set_interface();
        return;
    }
    parsedTrustedData[idx].set_being_calculated();

    SmConstant sup = SM_NONE;

    vf_ValidType *t = tpool.getVaildType(idx);
    if( !t->cls ) {
        t->cls = vf_resolve_class(k_class, t->name, false);
        if( !t->cls ) t->cls = CLASS_NOT_LOADED;
    }

    if( t->cls != CLASS_NOT_LOADED ) {
        if(class_is_interface(t->cls)) {
            parsedTrustedData[idx].set_interface();
            return;
        }
        Class_Handle h_sup = class_get_super_class(t->cls);
        if( h_sup ) {
            sup = tpool.get_ref_type( class_get_name(h_sup));
            int sup_idx = sup.getReferenceIdx();
            vf_ValidType *sup_type = tpool.getVaildType(sup_idx);

            if( sup_type->cls == CLASS_NOT_LOADED ) {
                //classloader delegation model is broken
                if( sup_idx < parsedDataSz && parsedTrustedData[sup_idx].is_calculated() ) {
                    parsedTrustedData[sup_idx].init();
                }
            }
                
            sup_type->cls = h_sup;
            parseTrustedData( sup );
        }            
    } else {
        if( value != tpool.sm_get_const_object() ) {
            sup = tpool.sm_get_const_object();
            parseTrustedData( sup );
        }
    }

    int c;
    for( c = 0; c < trustedPairsCnt; c++ ) {
        if( trustedPairs[c].from == value ) {
            parseTrustedData(trustedPairs[c].to);
        }
    }

    class_stack.init();
    class_stack.push(value);

    if( sup != SM_NONE ) { //i.e. value != java/lang/Object
        for( SmConstant *sm = parsedTrustedData[sup.getReferenceIdx()].superclasses(); *sm != SM_NONE; sm++ ) {
            if( !class_stack.instack(*sm) ) class_stack.push(*sm);
        }
    }

    for( c = 0; c < trustedPairsCnt; c++ ) {
        if( trustedPairs[c].from == value ) {
            for( SmConstant *sm = parsedTrustedData[trustedPairs[c].to.getReferenceIdx()].superclasses(); *sm != SM_NONE; sm++ ) {
                if( !class_stack.instack(*sm) ) class_stack.push(*sm);
            }
        }
    }

    if( parsedTrustedData[idx].is_interface() ) {
        while ( !class_stack.is_empty() ) parsedTrustedData[class_stack.pop().getReferenceIdx()].set_interface();
    } else {
        SmConstant* supercls = (SmConstant*)mem.malloc((class_stack.get_depth()+1)*sizeof(SmConstant));

        parsedTrustedData[idx].set_superclasses(supercls);
        int i = 0;
        while ( !class_stack.is_empty() ) supercls[i++] = class_stack.pop();
        supercls[i] = SM_NONE;
    }
}

void vf_Context_5e::parseTrustedPairs() {
    trustedPairsCnt = 0;
    if( !class_constraints ) return;

    vf_TypeConstraint *constraint;
    for( constraint = (vf_TypeConstraint*)unparsedPairs; constraint; constraint = constraint->next ) {
        trustedPairsCnt++;
    }
    trustedPairs = (TrustedPair*)mem.malloc(trustedPairsCnt * sizeof(TrustedPair));

    int i = 0;
    for( constraint = (vf_TypeConstraint*)unparsedPairs; constraint; constraint = constraint->next ) {
        trustedPairs[i].from = tpool.get_ref_type(constraint->source);
        tryResolve(trustedPairs[i].from);

        trustedPairs[i].to = tpool.get_ref_type(constraint->target);
        tryResolve(trustedPairs[i].to);

        i++;
    }

    tryResolve(tpool.sm_get_const_object());
    tryResolve(tpool.sm_get_const_this());
}
