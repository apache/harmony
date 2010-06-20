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
#include "verifier.h"
#include "context_x.h"
#include "../java5/context_5.h"
#include "../java6/context_6.h"

// "virtual" methods

#define workmap_expect                  (((ActualClass*)this)->workmap_expect)
#define workmap_expect_strict           (((ActualClass*)this)->workmap_expect_strict)

#define add_incoming_value              (((ActualClass*)this)->add_incoming_value)
#define new_scalar_constraint           (((ActualClass*)this)->new_scalar_constraint)
#define new_scalar_array2ref_constraint (((ActualClass*)this)->new_scalar_array2ref_constraint)
#define new_bogus_propagation_constraint (((ActualClass*)this)->new_bogus_propagation_constraint)
#define new_ret_vector_constraint       (((ActualClass*)this)->new_ret_vector_constraint)
#define push_handler                    (((ActualClass*)this)->push_handler)

#define getStackmap                     (((ActualClass*)this)->getStackmap)

/////////////////////


#define POP_x( TYPE )                                                    \
    /* check stack */                                                    \
    if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement value = workmap_pop();                                \
                                                                         \
    /* check INs */                                                      \
    if( !workmap_expect_strict(value, TYPE) ) {                          \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \



    \
#define POP_x_x( TYPE )                                                  \
    /* check stack */                                                    \
    if( !workmap_can_pop(2) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement value1 = workmap_pop();                               \
    WorkmapElement value2 = workmap_pop();                               \
                                                                         \
    /* check INs */                                                      \
    if( !workmap_expect_strict(value1, TYPE) ||                          \
        !workmap_expect_strict(value2, TYPE) )                           \
    {                                                                    \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \



#define POP_xx( TYPE )                                                   \
    /* check stack */                                                    \
    if( !workmap_can_pop(2) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement hi_word = workmap_pop();                              \
    WorkmapElement low_word = workmap_pop();                             \
                                                                         \
    /* check INs */                                                      \
    if( !workmap_expect_strict(hi_word, SM_HIGH_WORD) ||                 \
        !workmap_expect_strict(low_word, TYPE) )                         \
    {                                                                    \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \



#define POP_xx_xx( TYPE )                                                \
    /* check stack */                                                    \
    if( !workmap_can_pop(4) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement hi_word1 = workmap_pop();                             \
    WorkmapElement low_word1 = workmap_pop();                            \
    WorkmapElement hi_word2 = workmap_pop();                             \
    WorkmapElement low_word2 = workmap_pop();                            \
                                                                         \
    /* check INs */                                                      \
    if( !workmap_expect_strict(hi_word1, SM_HIGH_WORD) ||                \
        !workmap_expect_strict(low_word1, TYPE) ||                       \
        !workmap_expect_strict(hi_word2, SM_HIGH_WORD) ||                \
        !workmap_expect_strict(low_word2, TYPE) )                        \
    {                                                                    \
        return error(VF_ErrorIncompatibleArgument,                       \
            "incompartible argument");                                   \
    }                                                                    \



#define POP_ref( TYPE ) {                                                \
    /* check stack */                                                    \
    if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement object_ref = workmap_pop();                           \
    \
    /* check INs */                                                      \
    if( !workmap_expect(object_ref, TYPE) ) {                            \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
}

#define POP_z( TYPE )                                                    \
    if( TYPE.isLongOrDouble() ) {                                        \
        POP_xx(TYPE);                                                    \
    } else {                                                             \
        /* check stack */                                                \
        if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow,         \
                "unable to pop from empty operand stack");               \
                                                                         \
        /* pop INs */                                                    \
        WorkmapElement value = workmap_pop();                            \
                                                                         \
        /* check INs */                                                  \
        if( !workmap_expect(value, TYPE) ) {                             \
            return error(VF_ErrorIncompatibleArgument,                   \
                    "incompartible argument");                           \
        }                                                                \
    }                                                                    \

#define VIEW_z( TYPE, depth )                                            \
    /* get INs */                                                        \
    WorkmapElement value = workmap_stackview(--depth);                   \
                                                                         \
    /* check INs */                                                      \
    if( !workmap_expect(value, TYPE) ) {                                 \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
                                                                         \
    if( TYPE.isLongOrDouble() ) {                                        \
        /* get INs */                                                    \
        WorkmapElement value = workmap_stackview(--depth);               \
                                                                         \
        /* check INs */                                                  \
        if( !workmap_expect(value, SM_HIGH_WORD) ) {                     \
            return error(VF_ErrorIncompatibleArgument,                   \
                    "incompartible argument");                           \
        }                                                                \
    }                                                                    \


#define PUSH_z( TYPE )                                                   \
    if( TYPE.isLongOrDouble() ) {                                        \
        if( !workmap_can_push(2) ) {                                     \
            return error(VF_ErrorStackOverflow, "stack overflow");       \
        }                                                                \
        workmap_2w_push_const(TYPE);                                     \
    } else {                                                             \
        if( !workmap_can_push(1) ) {                                     \
            return error(VF_ErrorStackOverflow, "stack overflow");       \
        }                                                                \
        workmap_push_const(TYPE);                                        \
    }                                                                    \


#define CHECK_xLOAD( idx, TYPE )                                         \
    if( !workmap_valid_local(idx) ) {                                    \
        return error(VF_ErrorLocals, "invalid local index");             \
    }                                                                    \
    if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow,            \
            "operand stack overflow");                                   \
                                                                         \
    WorkmapElement value = workmap_get_local(idx);                       \
                                                                         \
    if( !workmap_expect_strict(value, TYPE) ) {                          \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
                                                                         \
    workmap_push_const(TYPE);                                            \



#define CHECK_xxLOAD( idx, TYPE )                                        \
    if( !workmap_valid_2w_local(idx) ) {                                 \
        return error(VF_ErrorLocals, "invalid local index");             \
    }                                                                    \
    if( !workmap_can_push(2) ) return error(VF_ErrorDataFlow,            \
            "operand stack overflow");                                   \
                                                                         \
    WorkmapElement value_lo = workmap_get_local(idx);                    \
    WorkmapElement value_hi = workmap_get_local(idx+1);                  \
                                                                         \
    if( !workmap_expect_strict(value_lo, TYPE) ||                        \
        !workmap_expect_strict(value_hi, SM_HIGH_WORD) )                 \
    {                                                                    \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
                                                                         \
    workmap_2w_push_const(TYPE);                                         \



#define CHECK_ALOAD( idx )                                               \
    if( !workmap_valid_local(idx) ) return error(VF_ErrorLocals,         \
            "invalid local index");                                      \
    if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow,            \
            "operand stack overflow");                                   \
                                                                         \
    WorkmapElement value = workmap_get_local(idx);                       \
                                                                         \
    if( !workmap_expect(value, SM_REF_OR_UNINIT) ) {                     \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
                                                                         \
    workmap_push(value);                                                 \



#define CHECK_zALOAD( CHECK )                                            \
    /* check stack */                                                    \
    if( !workmap_can_pop(2) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement index = workmap_pop();                                \
    WorkmapElement arrayref = workmap_pop();                             \
                                                                         \
    /* check INs */                                                      \
    if( !workmap_expect_strict( index, SM_INTEGER ) ) {                  \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
                                                                         \
    if( !CHECK ) return error(VF_ErrorIncompatibleArgument,              \
            "incompartible argument");                                   \




#define CHECK_xSTORE( idx, TYPE )                                        \
    if( !workmap_valid_local(idx) ) return error(VF_ErrorLocals,         \
            "invalid local index");                                      \
    POP_x( TYPE )                                                        \
    workmap_set_local_const(idx, TYPE);                                  \



#define CHECK_xxSTORE( idx, TYPE )                                       \
    if( !workmap_valid_2w_local(idx) ) return error(VF_ErrorLocals,      \
            "invalid local index");                                      \
    POP_xx( TYPE );                                                      \
    workmap_set_2w_local_const(idx, TYPE);                               \



#define CHECK_ASTORE( idx )                                              \
    if( !workmap_valid_local(idx) ) return error(VF_ErrorLocals,         \
            "invalid local index");                                      \
                                                                         \
    if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    WorkmapElement ref = workmap_pop();                                  \
    if( !workmap_expect(ref, SM_REF_OR_UNINIT_OR_RETADR) ) {             \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \
                                                                         \
    workmap_set_local(idx, ref);                                         \



#define CHECK_xASTORE( CHECK, TYPE )                                     \
    /* check stack */                                                    \
    if( !workmap_can_pop(3) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement value = workmap_pop();                                \
    WorkmapElement index = workmap_pop();                                \
    WorkmapElement arrayref = workmap_pop();                             \
    \
    /* check INs */                                                      \
    if( !workmap_expect_strict( value, TYPE ) ||                         \
        !workmap_expect_strict( index, SM_INTEGER ) ||                   \
        !CHECK )                                                         \
    {                                                                    \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \


#define CHECK_xxASTORE( CHECK, TYPE )                                    \
    /* check stack */                                                    \
    if( !workmap_can_pop(4) ) return error(VF_ErrorDataFlow,             \
            "unable to pop from empty operand stack");                   \
                                                                         \
    /* pop INs */                                                        \
    WorkmapElement value_hi = workmap_pop();                             \
    WorkmapElement value_lo = workmap_pop();                             \
    WorkmapElement index = workmap_pop();                                \
    WorkmapElement arrayref = workmap_pop();                             \
    \
    /* check INs */                                                      \
    if( !workmap_expect_strict( value_lo, TYPE ) ||                      \
        !workmap_expect_strict( value_hi, SM_HIGH_WORD ) ||              \
        !workmap_expect_strict( index, SM_INTEGER ) ||                   \
        !CHECK )                                                         \
    {                                                                    \
        return error(VF_ErrorIncompatibleArgument,                       \
                "incompartible argument");                               \
    }                                                                    \




/////////////////////////////////////////////////////////////////////////////////////


/**
 * check type-safety of a single instruction (second pass in case of java5 verification)
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::dataflow_instruction(Address instr) {

    vf_Result tcr;
    OpCode opcode = (OpCode)m_bytecode[instr];
    processed_instruction = instr;

    bool wide = false;
    if( opcode == OP_WIDE ) {
        wide = true;
        opcode = (OpCode)m_bytecode[instr+1];
    }

    switch( opcode ) {
        case OP_AALOAD: {
            // check stack
            if( !workmap_can_pop(2) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement index = workmap_pop();
            WorkmapElement arrayref = workmap_pop();

            //check INs
            if( !workmap_expect_strict(index, SM_INTEGER) ||
                !workmap_expect(arrayref, tpool.sm_get_const_arrayref_of_object()) )
            {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //create OUTs
            WorkmapElement wme;
            if( (tcr = new_scalar_array2ref_constraint(&arrayref, &wme)) != VF_OK ) {
                return tcr;
            }

            //pop OUTs
            workmap_push(wme);

            break;
                        }

        case OP_AASTORE: {
            // check stack
            if( !workmap_can_pop(3) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement value = workmap_pop();
            WorkmapElement index = workmap_pop();
            WorkmapElement arrayref = workmap_pop();

            //check & bind INs
            if( !workmap_expect_strict(index, SM_INTEGER) ||
                !workmap_expect(value, tpool.sm_get_const_object()) ||
                !workmap_expect(arrayref, tpool.sm_get_const_arrayref_of_object()) )
            {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }
            break;
                         }

        case OP_ACONST_NULL: {
            // check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //create & pop OUTs
            workmap_push_const(SM_NULL);

            break;
                             }

        case OP_ALOAD: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //call macro
            CHECK_ALOAD( local_idx );

            break;
                       }

        case OP_ALOAD_0: case OP_ALOAD_1: 
        case OP_ALOAD_2: case OP_ALOAD_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_ALOAD_0;

            //call macro
            CHECK_ALOAD( local_idx );

            break;
                         }

        case OP_ANEWARRAY: {
            //check stack
            if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement count = workmap_pop();

            //check INs
            if( !workmap_expect_strict(count, SM_INTEGER) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");

            //get OUT type
            SmConstant arrayref;
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            if( !tpool.cpool_get_array(cp_idx, &arrayref) ) return error(VF_ErrorConstantPool, "incorrect type for anewarray");

            //push OUTs
            workmap_push_const( arrayref );
            break;
                           }

        case OP_ARETURN: {
            if( return_type == SM_BOGUS ) return error(VF_ErrorDataFlow, "returned a value from a void method");
            POP_ref( return_type );
            break;
                         }

        case OP_ARRAYLENGTH: {
            //check stack
            if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement arrayref = workmap_pop();

            //check INs
            if( !workmap_expect(arrayref, SM_ANYARRAY) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");

            //push OUTs
            workmap_push_const( SM_INTEGER );

            break;
                             }

        case OP_ASTORE: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //call MACRO
            CHECK_ASTORE( local_idx );

            break;
                        }

        case OP_ASTORE_0: case OP_ASTORE_1: 
        case OP_ASTORE_2: case OP_ASTORE_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_ASTORE_0;

            //call MACRO
            CHECK_ASTORE( local_idx );
            break;
                          }

        case OP_ATHROW: {
            POP_ref( tpool.sm_get_const_throwable() );
            break;
                        }

        case OP_BALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_bb();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_push_const( SM_INTEGER );

            break;
                        }    

        case OP_BASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_bb();

            //call MACRO that loads inegral types
            CHECK_xASTORE( workmap_expect( arrayref, type ), SM_INTEGER );

            break;
                         }    

        case OP_BIPUSH:
        case OP_SIPUSH: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //create and push OUTs
            workmap_push_const( SM_INTEGER );

            break;
                        }

        case OP_CALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_char();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_push_const( SM_INTEGER );

            break;
                        }

        case OP_CASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_char();

            //call MACRO that loads inegral types
            CHECK_xASTORE( workmap_expect( arrayref, type ), SM_INTEGER );

            break;
                         }    

        case OP_CHECKCAST: {
            //check stack
            if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement inref = workmap_pop();

            //check INs
            if( !workmap_expect(inref, tpool.sm_get_const_object()) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //check instruction & create OUTs
            SmConstant outref;
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            if( !tpool.cpool_get_class(cp_idx, &outref) ) return error(VF_ErrorConstantPool, "incorrect constantpool entry");

            //push OUTs
            workmap_push_const(outref);

            break;
                           }

        case OP_D2F: {
            POP_xx ( SM_DOUBLE );

            //push OUTs
            workmap_push_const(SM_FLOAT);
            break;
                     }

        case OP_D2I: {
            POP_xx ( SM_DOUBLE );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                     }

        case OP_D2L: {
            POP_xx ( SM_DOUBLE );

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                     }

        case OP_DADD: case OP_DDIV:
        case OP_DMUL: case OP_DREM: case OP_DSUB: {
            POP_xx_xx( SM_DOUBLE );

            //push OUTs
            workmap_2w_push_const(SM_DOUBLE);
            break;
                      }

        case OP_DALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_double();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_2w_push_const( SM_DOUBLE );
            break;
                        }

        case OP_DASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_double();

            //call MACRO that loads inegral types
            CHECK_xxASTORE( workmap_expect( arrayref, type ), SM_DOUBLE );

            break;
                         }

        case OP_DCMPL: case OP_DCMPG: {
            POP_xx_xx( SM_DOUBLE );

            //push OUTs
            workmap_push_const(SM_INTEGER);

            break;
                       }

        case OP_DCONST_0:
        case OP_DCONST_1: {
            //check stack
            if( !workmap_can_push(2) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_2w_push_const(SM_DOUBLE);
            break;
                          }

        case OP_DLOAD: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //call macro
            CHECK_xxLOAD( local_idx, SM_DOUBLE );
            break;
                       }

        case OP_DLOAD_0: case OP_DLOAD_1: 
        case OP_DLOAD_2: case OP_DLOAD_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_DLOAD_0;

            //call macro
            CHECK_xxLOAD( local_idx, SM_DOUBLE );
            break;
                         }

        case OP_DNEG: {
            POP_xx( SM_DOUBLE );

            //push OUTs
            workmap_2w_push_const(SM_DOUBLE);
            break;
                      }

        case OP_DRETURN: {
            if( return_type != SM_DOUBLE ) return error(VF_ErrorDataFlow, "incorrect type returned");
            POP_xx( SM_DOUBLE );

            break;
                         }

        case OP_DSTORE: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            CHECK_xxSTORE( local_idx, SM_DOUBLE );

            break;
                        }

        case OP_DSTORE_0: case OP_DSTORE_1: 
        case OP_DSTORE_2: case OP_DSTORE_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_DSTORE_0;

            CHECK_xxSTORE( local_idx, SM_DOUBLE );

            break;
                          }    

        case OP_DUP: {
            //check stack
            if( !workmap_can_pop(1) || !workmap_can_push(1) ) {
                return error(VF_ErrorDataFlow, "unable to pop/push");
            }

            //pop INs
            WorkmapElement value = workmap_pop();

            //check INs
            if( !workmap_expect(value, SM_LOW_WORD) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");

            //push OUTs
            workmap_push( value );
            workmap_push( value );
            break;
                     }

        case OP_DUP_X1: {
            //check stack
            if( !workmap_can_pop(2) || !workmap_can_push(1) ) {
                return error(VF_ErrorDataFlow, "unable to pop/push");
            }

            //pop INs
            WorkmapElement value1 = workmap_pop();
            WorkmapElement value2 = workmap_pop();

            //check INs
            if( !workmap_expect(value1, SM_LOW_WORD) || !workmap_expect(value2, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //push OUTs
            workmap_push( value1 );
            workmap_push( value2 );
            workmap_push( value1 );
            break;
                        }

        case OP_DUP_X2: {
            //check stack
            if( !workmap_can_pop(3) || !workmap_can_push(1) ) {
                return error(VF_ErrorDataFlow, "unable to pop/push");
            }

            //pop INs
            WorkmapElement value1 = workmap_pop();
            WorkmapElement value2 = workmap_pop();
            WorkmapElement value3 = workmap_pop();

            //check INs  !!! SM_HIGH_WORD is a two-word element: Long and Double are not !!!
            if( !workmap_expect(value1, SM_LOW_WORD) || !workmap_expect(value3, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //push OUTs
            workmap_push( value1 );
            workmap_push( value3 );
            workmap_push( value2 );
            workmap_push( value1 );

            break;
                        }    

        case OP_DUP2: {
            //check stack
            if( !workmap_can_pop(2) || !workmap_can_push(2) ) {
                return error(VF_ErrorDataFlow, "unable to pop/push");
            }

            //pop INs
            WorkmapElement value1 = workmap_pop();
            WorkmapElement value2 = workmap_pop();

            //check INs
            if( !workmap_expect(value2, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //push OUTs
            workmap_push( value2 );
            workmap_push( value1 );
            workmap_push( value2 );
            workmap_push( value1 );

            break;
                      }    

        case OP_DUP2_X1: {
            //check stack
            if( !workmap_can_pop(3) || !workmap_can_push(2) ) {
                return error(VF_ErrorDataFlow, "unable to pop/push");
            }

            //pop INs
            WorkmapElement value1 = workmap_pop();
            WorkmapElement value2 = workmap_pop();
            WorkmapElement value3 = workmap_pop();

            //check INs
            if( !workmap_expect(value2, SM_LOW_WORD) || !workmap_expect(value3, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //push OUTs
            workmap_push( value2 );
            workmap_push( value1 );
            workmap_push( value3 );
            workmap_push( value2 );
            workmap_push( value1 );

            break;
                         }

        case OP_DUP2_X2: {
            //check stack
            if( !workmap_can_pop(4) || !workmap_can_push(2) ) {
                return error(VF_ErrorDataFlow, "unable to pop/push");
            }

            //pop INs
            WorkmapElement value1 = workmap_pop();
            WorkmapElement value2 = workmap_pop();
            WorkmapElement value3 = workmap_pop();
            WorkmapElement value4 = workmap_pop();

            //check INs
            if( !workmap_expect(value2, SM_LOW_WORD) || !workmap_expect(value4, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //push OUTs
            workmap_push( value2 );
            workmap_push( value1 );
            workmap_push( value4 );
            workmap_push( value3 );
            workmap_push( value2 );
            workmap_push( value1 );

            break;
                         }    

        case OP_F2D: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            POP_x ( SM_FLOAT );

            //push OUTs
            workmap_2w_push_const(SM_DOUBLE);
            break;
                     }

        case OP_F2I: {
            POP_x ( SM_FLOAT );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                     }

        case OP_F2L: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            POP_x ( SM_FLOAT );

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                     }

        case OP_FADD: case OP_FDIV:
        case OP_FMUL: case OP_FREM:
        case OP_FSUB: {
            POP_x_x( SM_FLOAT );

            //push OUTs
            workmap_push_const(SM_FLOAT);
            break;
                      }

        case OP_FALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_float();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_push_const( SM_FLOAT );
            break;
                        }

        case OP_FASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_float();

            //call MACRO that loads inegral types
            CHECK_xASTORE( workmap_expect( arrayref, type ), SM_FLOAT );

            break;
                         }

        case OP_FCMPL: case OP_FCMPG: {
            POP_x_x( SM_FLOAT );

            //push OUTs
            workmap_push_const(SM_INTEGER);

            break;
                       }

        case OP_FCONST_0:
        case OP_FCONST_1:
        case OP_FCONST_2: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_push_const(SM_FLOAT);
            break;
                          }

        case OP_FLOAD: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //call macro
            CHECK_xLOAD( local_idx, SM_FLOAT );
            break;
                       }

        case OP_FLOAD_0: case OP_FLOAD_1: 
        case OP_FLOAD_2: case OP_FLOAD_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_FLOAD_0;

            //call macro
            CHECK_xLOAD( local_idx, SM_FLOAT );
            break;
                         }

        case OP_FNEG: {
            POP_x( SM_FLOAT );

            //push OUTs
            workmap_push_const(SM_FLOAT);
            break;
                      }

        case OP_FRETURN: {
            if( return_type != SM_FLOAT ) return error(VF_ErrorDataFlow, "incorrect type returned");
            POP_x( SM_FLOAT );

            break;
                         }

        case OP_FSTORE: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            CHECK_xSTORE( local_idx, SM_FLOAT );

            break;
                        }

        case OP_FSTORE_0: case OP_FSTORE_1: 
        case OP_FSTORE_2: case OP_FSTORE_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_FSTORE_0;

            CHECK_xSTORE( local_idx, SM_FLOAT );

            break;
                          }    

        case OP_GOTO: case OP_GOTO_W: case OP_NOP:
            break;


        case OP_I2B: case OP_I2C: case OP_I2S: {
            POP_x ( SM_INTEGER );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                     }

        case OP_I2D: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            POP_x ( SM_INTEGER );

            //push OUTs
            workmap_2w_push_const(SM_DOUBLE);
            break;
                     }

        case OP_I2F: {
            POP_x ( SM_INTEGER );

            //push OUTs
            workmap_push_const(SM_FLOAT);
            break;
                     }

        case OP_I2L: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            POP_x ( SM_INTEGER );

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                     }

        case OP_IADD: case OP_IAND:
        case OP_IDIV: case OP_IMUL:
        case OP_IOR:  case OP_IREM:
        case OP_ISHL: case OP_ISHR:
        case OP_ISUB: case OP_IUSHR:
        case OP_IXOR: {

            POP_x_x ( SM_INTEGER );

            //push OUTs
            workmap_push_const( SM_INTEGER );
            break;
                      }

        case OP_IALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_integer();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_push_const( SM_INTEGER );
            break;
                        }


        case OP_IASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_integer();

            //call MACRO that loads inegral types
            CHECK_xASTORE( workmap_expect( arrayref, type ), SM_INTEGER );

            break;
                         }

        case OP_ICONST_M1: case OP_ICONST_0: 
        case OP_ICONST_1:  case OP_ICONST_2:
        case OP_ICONST_3:  case OP_ICONST_4: 
        case OP_ICONST_5: {

            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                          }

        case OP_IF_ACMPEQ:
        case OP_IF_ACMPNE: {
            POP_ref( tpool.sm_get_const_object() );
            POP_ref( tpool.sm_get_const_object() );
            break;
                           }

        case OP_IF_ICMPEQ: case OP_IF_ICMPNE:
        case OP_IF_ICMPLT: case OP_IF_ICMPGE:
        case OP_IF_ICMPGT: case OP_IF_ICMPLE: {
            POP_x_x( SM_INTEGER );
            break;
                           }

        case OP_IFEQ: case OP_IFNE:
        case OP_IFLT: case OP_IFGE:
        case OP_IFGT: case OP_IFLE:
        case OP_LOOKUPSWITCH:
        case OP_TABLESWITCH: {
            POP_x( SM_INTEGER );
            break;
                             }

        case OP_IFNONNULL:
        case OP_IFNULL:
        case OP_MONITORENTER:
        case OP_MONITOREXIT: {
            POP_ref( tpool.sm_get_const_object() );
            break;
                             }

        case OP_IINC: {
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            if( !workmap_valid_local(local_idx) ) {
                return error(VF_ErrorLocals, "invalid local index");
            }

            WorkmapElement value = workmap_get_local(local_idx);

            if( !workmap_expect_strict(value, SM_INTEGER) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");

            break;
                      }

        case OP_ILOAD: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //call macro
            CHECK_xLOAD( local_idx, SM_INTEGER );
            break;
                       }

        case OP_ILOAD_0: case OP_ILOAD_1: 
        case OP_ILOAD_2: case OP_ILOAD_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_ILOAD_0;

            //call macro
            CHECK_xLOAD( local_idx, SM_INTEGER );
            break;
                         }

        case OP_INEG: {
            POP_x( SM_INTEGER );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                      }

        case OP_INSTANCEOF: {
            //check instruction
            unsigned cp_idx = read_uint16(m_bytecode + instr + 1);
            if( !tpool.cpool_is_reftype(cp_idx) ) return error(VF_ErrorConstantPool, "incorrect constantpool entry");

            POP_ref( tpool.sm_get_const_object() );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                            }

        case OP_IRETURN: {
            if( return_type != SM_INTEGER ) return error(VF_ErrorDataFlow, "incorrect type returned");
            POP_x( SM_INTEGER );

            break;
                         }

        case OP_ISTORE: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            CHECK_xSTORE( local_idx, SM_INTEGER );

            break;
                        }

        case OP_ISTORE_0: case OP_ISTORE_1: 
        case OP_ISTORE_2: case OP_ISTORE_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_ISTORE_0;

            CHECK_xSTORE( local_idx, SM_INTEGER );

            break;
                          }    

        case OP_L2D: {
            POP_xx ( SM_LONG );

            //push OUTs
            workmap_2w_push_const(SM_DOUBLE);
            break;
                     }

        case OP_L2F: {
            POP_xx ( SM_LONG );

            //push OUTs
            workmap_push_const(SM_FLOAT);
            break;
                     }

        case OP_L2I: {
            POP_xx ( SM_LONG );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                     }

        case OP_LADD: case OP_LAND: case OP_LDIV:
        case OP_LMUL: case OP_LOR:  case OP_LREM:
        case OP_LSUB: case OP_LXOR: {
            POP_xx_xx( SM_LONG );

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                      }

        case OP_LALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_long();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_2w_push_const( SM_LONG );
            break;
                        }

        case OP_LASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_long();

            //call MACRO that loads inegral types
            CHECK_xxASTORE( workmap_expect( arrayref, type ), SM_LONG );

            break;
                         }

        case OP_LCMP: {
            POP_xx_xx( SM_LONG );

            //push OUTs
            workmap_push_const(SM_INTEGER);
            break;
                      }

        case OP_LCONST_0:
        case OP_LCONST_1: {
            //check stack
            if( !workmap_can_push(2) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                          }

        case OP_LDC: {
            //check instruction and create OUTs
            unsigned cp_idx = m_bytecode[instr + 1];
            SmConstant el = tpool.cpool_get_ldcarg(cp_idx);
            if( el == SM_BOGUS ) return error(VF_ErrorConstantPool, "incorrect constantpool entry");

            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_push_const(el);
            break;
                     }

        case OP_LDC_W: {
            //check instruction and create OUTs
            unsigned cp_idx = read_uint16(m_bytecode + instr + 1);
            SmConstant el = tpool.cpool_get_ldcarg(cp_idx);
            if( el == SM_BOGUS ) return error(VF_ErrorConstantPool, "incorrect constantpool entry");

            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_push_const(el);
            break;
                       }

        case OP_LDC2_W: {
            //check instruction and create OUTs
            unsigned cp_idx = read_uint16(m_bytecode + instr + 1);
            SmConstant el = tpool.cpool_get_ldc2arg(cp_idx);
            if( el == SM_BOGUS ) return error(VF_ErrorConstantPool, "incorrect constantpool entry");

            //check stack
            if( !workmap_can_push(2) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //push OUTs
            workmap_2w_push_const(el);
            break;
                        }

        case OP_LLOAD: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //call macro
            CHECK_xxLOAD( local_idx, SM_LONG );
            break;
                       }

        case OP_LLOAD_0: case OP_LLOAD_1: 
        case OP_LLOAD_2: case OP_LLOAD_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_LLOAD_0;

            //call macro
            CHECK_xxLOAD( local_idx, SM_LONG );
            break;
                         }

        case OP_LNEG: {
            POP_xx( SM_LONG );

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                      }

        case OP_LRETURN: {
            if( return_type != SM_LONG ) return error(VF_ErrorDataFlow, "incorrect type returned");
            POP_xx( SM_LONG );

            break;
                         }

        case OP_LSTORE: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            CHECK_xxSTORE( local_idx, SM_LONG );

            break;
                        }

        case OP_LSTORE_0: case OP_LSTORE_1: 
        case OP_LSTORE_2: case OP_LSTORE_3: {
            //get local index from opcode
            U_8 local_idx =  opcode - OP_LSTORE_0;

            CHECK_xxSTORE( local_idx, SM_LONG );

            break;
                          }    

        case OP_LSHL: case OP_LSHR: case OP_LUSHR: {
            POP_x( SM_INTEGER );
            POP_xx( SM_LONG );

            //push OUTs
            workmap_2w_push_const(SM_LONG);
            break;
                      }

        case OP_MULTIANEWARRAY: {
            unsigned dims = m_bytecode[instr + 3];
            if( !dims ) {
                return error(VF_ErrorConstantPool, "incorrect format of multianewarray");
            }

            //check stack
            if( !workmap_can_pop(dims) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            for( unsigned i = 0; i < dims; i++ ) {
                WorkmapElement count = workmap_pop();

                //check INs
                if( !workmap_expect_strict(count, SM_INTEGER) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //get OUT type
            SmConstant arrayref;
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            if( !tpool.cpool_get_class(cp_idx, &arrayref, (int)dims) ) {
                return error(VF_ErrorConstantPool, "incorrect type for multianewarray");
            }

            //push OUTs
            workmap_push_const( arrayref );
            break;
                                }

        case OP_NEW: {
            //check instruction
            unsigned cp_idx = read_uint16(m_bytecode + instr + 1);

            //TODO: unused variable?
            SmConstant new_type = SM_BOGUS;

            //TODO: CONSTANTPOOL validation
            //if( !tpool.cpool_get_class(cp_idx, &new_type, 0xFFFFFFFF) ) return VF_ErrorConstantPool;
            if( !tpool.cpool_get_class(cp_idx, 0, -1) ) {
                return error(VF_ErrorConstantPool, "incorrect constantpool entry for new");
            }

            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //create OUTs
            SmConstant newobj = SmConstant::getNewObject(instr);

            //push OUTs
            workmap_push_const( newobj );

            break;
                     }

        case OP_NEWARRAY: {
            POP_x( SM_INTEGER );

            U_8 array_type = m_bytecode[instr + 1];
            if( array_type < 4 || array_type > 11 ) return error(VF_ErrorInstruction, "bad array type");

            SmConstant ref = tpool.sm_get_const_arrayref(array_type);
            workmap_push_const( ref);
            break;
                          }

        case OP_POP: {
            //check stack
            if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement value = workmap_pop();

            //check INs
            if( !workmap_expect(value, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }
            break;
                     }

        case OP_POP2: {
            //check stack
            if( !workmap_can_pop(2) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            workmap_pop();
            WorkmapElement lo_val = workmap_pop();

            //check INs
            if( !workmap_expect(lo_val, SM_LOW_WORD) ) {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }
            break;
                      }

        case OP_RETURN: {
            //check instruction
            if( return_type != SM_BOGUS ) return error(VF_ErrorDataFlow, "incorrect type returned");

            if( m_is_constructor && !workmap_expect_strict(workmap->elements[m_max_locals], tpool.sm_get_const_this()) ) {
                return error(VF_ErrorIncompatibleArgument, "has not called constructor of super class");
            }
            break;
                        }

        case OP_SALOAD: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_short();

            //call MACRO that loads inegral types
            CHECK_zALOAD( workmap_expect( arrayref, type ) );

            //create and push OUTs
            workmap_push_const( SM_INTEGER );

            break;
                        }

        case OP_SASTORE: {
            //get required array type
            SmConstant type = tpool.sm_get_const_arrayref_of_short();

            //call MACRO that loads inegral types
            CHECK_xASTORE( workmap_expect( arrayref, type ), SM_INTEGER );

            break;
                         }   

        case OP_SWAP: {
            //check stack
            if( !workmap_can_pop(2) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

            //pop INs
            WorkmapElement value1 = workmap_pop();
            WorkmapElement value2 = workmap_pop();

            //check INs
            if( !workmap_expect(value1, SM_LOW_WORD) ||
                !workmap_expect(value2, SM_LOW_WORD) )
            {
                return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            }

            //push OUTs
            workmap_push( value1 );
            workmap_push( value2 );

            break;
                      }


        case OP_GETFIELD: {
            //check and resolve instruction
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            SmConstant ref, value;
            if( !tpool.cpool_get_field(cp_idx, &ref, &value) ) return error(VF_ErrorUnknown, "incorrect constantpool entry");

            //pop INs
            vf_Result result;
            if( (result = popFieldRef(ref, cp_idx)) != VF_OK ) {
                return result;
            }

            //push OUTs
            PUSH_z(value);

            break;
                          }

        case OP_GETSTATIC: {
            //check and resolve instruction
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            SmConstant value;
            if( !tpool.cpool_get_field(cp_idx, 0, &value) ) return error(VF_ErrorUnknown, "incorrect constantpool entry");

            //push OUTs
            PUSH_z(value);

            break;
                           }

        case OP_PUTFIELD: {
            //check and resolve instruction
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            SmConstant expected_ref, expected_val;
            if( !tpool.cpool_get_field(cp_idx, &expected_ref, &expected_val) ) {
                return error(VF_ErrorUnknown, "incorrect constantpool entry");
            }

            //pop INs
            POP_z( expected_val );

            if (!workmap_can_pop(1)) {
                return error(VF_ErrorUnknown, "unable to pop from the empty stack");
            }

            WorkmapElement &w0 = workmap_stackview(0);
            if( w0.getAnyPossibleValue() != SM_THISUNINIT ) {
                vf_Result result;

                if( (result = popFieldRef(expected_ref, cp_idx)) != VF_OK ) {
                    return result;
                }
            } else if( expected_ref == tpool.sm_get_const_this() ) {
                if( !workmap_expect_strict(w0, SM_THISUNINIT) ) {
                    return error(VF_ErrorUnknown, "Incompatible argument");
                }

                workmap_pop();
            } else {
                return error(VF_ErrorUnknown, "incorrect uninitialized type");
            }

            break;
                          }

        case OP_PUTSTATIC: {
            //check and resolve instruction
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);
            SmConstant expected_val;
            if( !tpool.cpool_get_field(cp_idx, 0, &expected_val) ) return error(VF_ErrorUnknown, "incorrect constantpool entry");

            //pop INs
            POP_z( expected_val );

            break;
                           }

        case OP_INVOKEINTERFACE:
        case OP_INVOKESPECIAL:
        case OP_INVOKESTATIC:
        case OP_INVOKEVIRTUAL: {
            //check instruction
            unsigned short cp_idx = read_uint16(m_bytecode + instr + 1);

            //parse constant pool entrance
            const char *state;
            SmConstant expected_ref, expected_rettype;
            unsigned short name_idx;
            int args_sz;

            //get method's class
            if( !tpool.cpool_method_start(cp_idx, &state, &expected_ref, &name_idx, opcode) ||
                !tpool.cpool_method_get_rettype(&state, &expected_rettype, &args_sz) )
            {
                return error(VF_ErrorUnknown, "incorrect constantpool entry");
            }
            assert( args_sz && state || !args_sz && !state);

            if( opcode == OP_INVOKEINTERFACE ) {
                //TODO: is verifier the right place for this check?
                //check 'count' value for invokeinterface instruction
                U_8 count = m_bytecode[instr + 3];
                U_8 fourth = m_bytecode[instr + 4];
                if( count != args_sz + 1 || fourth ) {
                    return error(VF_ErrorUnknown, "incorrect invokeinterface instruction");
                }
            }

            if( args_sz > 255 || args_sz > 254 && opcode != OP_INVOKESTATIC ) {
                //TODO: is verifier the right place for this check?
                return error(VF_ErrorUnknown, "too many arguments for the method");
            }

            if( args_sz ) {
                if( !workmap_can_pop(args_sz) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

                //pop args
                SmConstant expected_arg;
                int arg_depth = args_sz;
                while( state ) {
                    if( !tpool.cpool_method_next_arg(&state, &expected_arg) ) {
                        return error(VF_ErrorUnknown, "incorrect constantpool entry");
                    }

                    /* pop INs */
                    VIEW_z( expected_arg, arg_depth );
                }
                workmap->depth -= args_sz;
            }

            if( tpool.cpool_method_is_constructor_call(name_idx) ) {
                if( opcode != OP_INVOKESPECIAL ) {
                    //TODO: is verifier the right place for this check?
                    return error(VF_ErrorUnknown, "constructor must be called by invokespecial");
                }

                //constructor must return void - if it is not checked somewhere else, add check here
                assert(expected_rettype == SM_BOGUS);

                //translate expected ref

                // check stack
                if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");

                // pop INs
                WorkmapElement uninit_object = workmap_pop();


                /* check INs */
                SmConstant uninit_value = uninit_object.getAnyPossibleValue();

                if( uninit_value != SM_THISUNINIT && !uninit_value.isNewObject() ||
                    !workmap_expect_strict(uninit_object, uninit_value) ) 
                {
                    return error(VF_ErrorIncompatibleArgument, "incompartible argument: new object expected");
                }

                assert( uninit_value != SM_NONE );
                SmConstant init_val = sm_convert_to_initialized( uninit_value );

                //exception might be thrown from the constructor, all uninit values will be invalid
                //BUT if try block contains both this and the next instruction then no extra actions is necessary:
                //SM_BOGUS will appear when the values are merged
                propagate_bogus_to_handlers(instr, uninit_value);

                //replace all uninit values on stack & locals & possibly "constructor called flag" with init value
                for( unsigned i = 0; i < + m_stack_start + workmap->depth; i++ ) {
                    WorkmapElement &wm_el = workmap->elements[i];

                    if( wm_el.getAnyPossibleValue() == uninit_value ) {

                        new_bogus_propagation_constraint(wm_el, init_val);

                        //respect changed_locals
                        if( i < m_stack_start ) {
                            changed_locals[ i ] = 1;

                            wm_el.setJsrModified();

                            //will be set later
                            //locals_changed = true;
                        }
                    }
                }

                //flags need to be checked
                locals_changed = true;

                //check that objectref is exactly necessary class
                if( uninit_value == SM_THISUNINIT ) {
                    assert(m_is_constructor);
                    assert(tpool.sm_get_const_this() != tpool.sm_get_const_object());

                    if( expected_ref != tpool.sm_get_const_this() && 
                        expected_ref != tpool.sm_get_const_super() )
                    {
                        return error(VF_ErrorUnknown, "incorrect uninitialized type");
                    }

                } else {
                    if( expected_ref != init_val ) return error(VF_ErrorUnknown, "incorrect uninitialized type");
                }
            } else if( opcode == OP_INVOKESPECIAL ) {
                //pop object ref (it must extend be either 'this' or a sub class of 'this')
                POP_ref( tpool.sm_get_const_this() );

                //check that 'expected_ref' is a super class of 'this'
                if( !tpool.mustbe_assignable(tpool.sm_get_const_this(), expected_ref) ) {
                    return error(VF_ErrorUnknown, "incorrect use of invokespecial");
                }
            } else if( opcode == OP_INVOKEVIRTUAL ) {
                vf_Result result;
                if( (result = popVirtualRef(expected_ref, cp_idx)) != VF_OK ) {
                    return result;
                }
            } else if( opcode != OP_INVOKESTATIC ) {
                //pop object ref
                POP_ref( expected_ref );
            }

            //push OUTs
            if( expected_rettype != SM_BOGUS ) {
                //is not void
                PUSH_z( expected_rettype );
            }
            break;
                               }
        case OP_JSR: case OP_JSR_W: {
            //check stack
            if( !workmap_can_push(1) ) return error(VF_ErrorDataFlow, "operand stack overflow");

            //extract JSR target. would be better to do it in dataflow_liner, but it's also not very good
            Address target = opcode == OP_JSR_W ? instr_get_int32_target(instr, m_bytecode + instr + 1) :
                instr_get_int16_target(instr, m_bytecode + instr + 1);

            //create OUTs
            SmConstant retaddr = SmConstant::getRetAddr(target);

            //push OUTs
            workmap_push_const(retaddr);
            break;
                     }
        case OP_RET: {
            //get local index from bytecode
            unsigned local_idx = wide ? read_uint16(m_bytecode + instr + 2) : m_bytecode[instr + 1];

            //check whether it is a valid local
            if( !workmap_valid_local(local_idx) ) {
                return error(VF_ErrorLocals, "invalid local index");
            }

            //get local type from there
            WorkmapElement value = workmap_get_local(local_idx);
            SmConstant retaddr = value.getAnyPossibleValue();
            assert(retaddr != SM_NONE);

            //expect ret address
            if( !retaddr.isRetAddr() || !workmap_expect_strict(value, retaddr) ) {
                return error(VF_ErrorIncompatibleArgument, "ret address expected");
            }

            //replace all copies of retaddr with SM_BOGUS to avoid recursion
            for( unsigned i = 0; i < m_stack_start + workmap->depth; i++ ) {
                WorkmapElement &wm_el = workmap->elements[i];

                if( wm_el.getAnyPossibleValue() == retaddr ) {
                    wm_el = _WorkmapElement(SM_BOGUS);
                    //don't need to track changed locals
                }
            }

            //actually is not a ret address: it's an address of subroutine's start
            return new_ret_vector_constraint(retaddr.getRetInstr());
                     }
        default:
            assert(0);
            return error(VF_ErrorInternal, "unreachable statement");
    }

    if( (tcr=dataflow_handlers(instr)) != VF_OK ) {
        return tcr;
    }
    return VF_OK;
}


/**
 * check type-safety for exception handlers of a single instruction (second pass in case of java5 verification)
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::dataflow_handlers(Address instr) {
    if( !m_handlecount || instr < next_start_pc && !no_locals_info && !locals_changed ) return VF_OK;

    uint16 start_pc;
    uint16 end_pc;
    uint16 handler_pc;
    uint16 handler_cp_index;

    int clean_required = false;
    int new_loop_start = m_handlecount;
    int new_loop_finish = 0;

    //no info or hit new try block
    if( instr >= next_start_pc ) {
        no_locals_info = 1;
        loop_start = 0;
        loop_finish = m_handlecount;
        next_start_pc = m_code_length; //some big enough value
    }

    for( unsigned short idx = loop_start; idx < loop_finish; idx++ ) {

        method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc,
            &handler_pc, &handler_cp_index );

        if( instr < start_pc ) {
            // calcculate some constants to optimize exception handling
            if( next_start_pc > start_pc ) next_start_pc = start_pc;
        } else if( instr < end_pc ) {
            vf_Result tcr;

            if( idx < new_loop_start ) {
                new_loop_start = idx;
            }
            new_loop_finish = idx + 1;

            if( no_locals_info ) {
                push_handler(handler_pc);

                if( (tcr=new_handler_vector_constraint(handler_pc)) != VF_OK ) {
                    return tcr;
                }

                // calcculate some constants to optimize exception handling
                clean_required = true;
            } else if( locals_changed ) {

                StackmapHead *handler = getStackmap(handler_pc);

                //merge non-stack variables
                for( unsigned i = 0; i < m_stack_start; i++ ) {
                    if( changed_locals[ i ] ) {
                        WorkmapElement *from = &workmap->elements[i];
                        StackmapElement *to = &handler->elements[i];

                        if( (tcr=new_scalar_constraint(from, to)) != VF_OK ) {
                            return tcr;
                        }
                    }
                }    
                clean_required = true;
            }
        }
    }

    if( clean_required ) {
        for( unsigned i = 0; i < m_stack_start; i++ ) {
            changed_locals[i] = 0;
        }
        locals_changed = 0;
        no_locals_info = 0;
    }

    loop_start = new_loop_start;
    loop_finish = new_loop_finish;

    return VF_OK;
}


/**
 * Creates a workmap for zero instruction from method's arguments (second pass in case of java5 verification).
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::create_method_initial_workmap() {
    // allocate memory for working stack map
    workmap = newWorkmap(m_stack_start + m_max_stack);

    unsigned local_idx = 0;

    workmap->depth = 0;
    if( !method_is_static( m_method ) ) {
        if( m_is_constructor ) {
            //constructr of a class differ then Object
            workmap_set_local_const(local_idx++, SM_THISUNINIT );

            //another constructor has to be called
            workmap->elements[m_max_locals] = _WorkmapElement(SM_THISUNINIT);
            changed_locals[ m_max_locals ] = 1;
        } else {
            workmap_set_local_const(local_idx++, tpool.sm_get_const_this());
        }
    }

    const char *state = method_get_descriptor( m_method );
    int args_sz;
    if( !tpool.cpool_method_get_rettype(&state, &return_type, &args_sz) ) {
        return error(VF_ErrorUnknown, "illegal constantpool entry");
    }

    assert( args_sz && state || !args_sz && !state);

    SmConstant argument;
    while ( state ) {
        tpool.cpool_method_next_arg( &state, &argument);
        if( argument == SM_DOUBLE || argument == SM_LONG ) {
            workmap_set_2w_local_const(local_idx, argument);
            local_idx += 2;
        } else {
            workmap_set_local_const(local_idx++, argument);
        }
        assert(local_idx <= m_max_locals);
    }

    while ( local_idx < m_max_locals ) {
        workmap_set_local_const(local_idx++, SM_BOGUS);
    }

    return VF_OK;
}


/**
 * check conditions for accessing protected non-static fields in different package
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::popFieldRef(SmConstant expected_ref, unsigned short cp_idx) {
    int check = tpool.checkFieldAccess( expected_ref, cp_idx);

    if( check != vf_TypePool::_FALSE ) {
        assert(check == vf_TypePool::_TRUE);

        //pop object ref (it must extend be either 'this' or a sub class of 'this')
        POP_ref( tpool.sm_get_const_this() );

        //check that 'expected_ref' is a super class of 'this'
        if( !tpool.mustbe_assignable(tpool.sm_get_const_this(), expected_ref) ) {
            return error(VF_ErrorUnknown, "incorrect use of invokespecial");
        }
    } else {
        POP_ref( expected_ref );
    }
    return VF_OK;
}


/**
 * check conditions for accessing protected virtual methods in different package
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::popVirtualRef(SmConstant expected_ref, unsigned short cp_idx) {
    int check = tpool.checkVirtualAccess( expected_ref, cp_idx);

    if( check != vf_TypePool::_FALSE ) {
        if( !workmap_can_pop(1) ) return error(VF_ErrorDataFlow, "unable to pop from empty operand stack");
        WorkmapElement value = workmap_pop();

        if( check == vf_TypePool::_CLONE ) {
            //if the first value is an array ==> expect array here
            SmConstant c = value.getAnyPossibleValue();
            if( c.isReference() && tpool.sm_get_refname(c)[0] == '[' ) {
                if( !workmap_expect(value, SM_ANYARRAY) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");
                if( !workmap_expect(value, expected_ref) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");
            } else {
                check = vf_TypePool::_TRUE;
            }
        }

        if ( check != vf_TypePool::_CLONE ) {
            assert(check == vf_TypePool::_TRUE);

            //pop object ref (it must extend be either 'this' or a sub class of 'this')
            if( !workmap_expect(value, tpool.sm_get_const_this()) ) return error(VF_ErrorIncompatibleArgument, "incompartible argument");

            //check that 'expected_ref' is a super class of 'this'
            if( !tpool.mustbe_assignable(tpool.sm_get_const_this(), expected_ref) ) {
                return error(VF_ErrorUnknown, "incorrect use of invokespecial");
            }
        }
    } else {
        POP_ref( expected_ref );
    }
    return VF_OK;
}


/**
 * create constraint vector in case of a branch 
 * simple conatraints are created for pairs of both locals and stack (current must be assignable to target) 
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::new_generic_vector_constraint_impl(StackmapHead *target) {
    if( target->depth != workmap->depth ) return error(VF_ErrorStackDepth, "stack depth does not match");

    //merge all variables
    for( unsigned i = 0; i < m_stack_start + workmap->depth; i++ ) {
        WorkmapElement *from = &workmap->elements[i];
        StackmapElement *to = &target->elements[i];

        vf_Result tcr;
        if( (tcr=new_scalar_constraint(from, to)) != VF_OK ) {
            return tcr;
        }
    }    

    return VF_OK;
}


/**
 * create constraint vector for exception handler
 * simple conatraints are created for pairs of local variable (current must be assignable to start of exception handler)
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::new_handler_vector_constraint(Address handler_instr) {
    vf_Result tcr;

    StackmapHead *handler = getStackmap(handler_instr);
    assert(handler);

    //merge local and stack variables
    for( unsigned i = 0; i < m_stack_start; i++ ) {
        WorkmapElement *from = &workmap->elements[i];
        StackmapElement *to = &handler->elements[i];

        if( (tcr=new_scalar_constraint(from, to)) != VF_OK ) {
            return tcr;
        }
    }    

    return VF_OK;
}


/**
 * specail care for <init> calls in try blocks
 */
template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
vf_Result vf_Context_x<ActualClass, WorkmapElement, _WorkmapElement, StackmapElement>::propagate_bogus_to_handlers(Address instr, SmConstant uninit_value) {
    if( !m_handlecount ) return VF_OK;

    //loop start and loop finish calculated in dataflow_handlers
    for( unsigned short idx = loop_start; idx < loop_finish; idx++ ) {
        uint16 start_pc;
        uint16 end_pc;
        uint16 handler_pc;
        uint16 handler_cp_index;

        method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc,
            &handler_pc, &handler_cp_index );

        if( instr < end_pc && instr >= start_pc ) {
            vf_Result tcr;

            StackmapHead *handler = getStackmap(handler_pc);
            assert(handler);

            //merge non-stack variables
            for( unsigned i = 0; i < m_stack_start; i++ ) {
                if( workmap->elements[i].getAnyPossibleValue() == uninit_value ) {
                    if( (tcr=add_incoming_value( SM_BOGUS, &handler->elements[i] )) != VF_OK ) {
                        return tcr;
                    }
                }    
            }
        }
    }
    return VF_OK;
}

#pragma warning (disable:4660)  // warning C4660: template-class specialization is already instantiated
template class vf_Context_x<vf_Context_5, WorkmapElement_5, _WorkmapElement_5, StackmapElement_5>;
template class vf_Context_x<vf_Context_6, WorkmapElement_6, _WorkmapElement_6, StackmapElement_6>;
