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
#ifndef __INSTR_PROPS5_H_
#define __INSTR_PROPS5_H_

#include <assert.h>
#include "../base/stackmap.h"

//store flags and properties (stackmaps, workmaps, etc) for each instruction
class InstrProps : public InstrPropsBase {
private:
    //array of bit flags
    U_8* packed_flags;

    //returns flags for the instruction 'instr'. other bits are not necessary 0s
    int get_dirty_mask(Address instr) {
        int b = packed_flags[instr/4];
        b = b >> ((instr % 4) * 2);
        return b;
    }

    //bit OR flags for the instruction 'instr' with 'mask'
    void fill_mask(Address instr, int mask) {
        assert((mask & ~3) == 0);
        mask = mask << ((instr % 4) * 2);

        packed_flags[instr/4] |= mask;
    }

    //clears bits the are set in the 'mask' (& ~mask) for the instruction 'instr'
    void clear_mask(Address instr, int mask) {
        assert((mask & ~3) == 0);
        mask = mask << (instr % 4) * 2;

        packed_flags[instr/4] &= ~mask;
    }

public:
    //initializes the class. this function is invoked once per method - removes old data in initializes storages.
    void init(Memory &mem, int code_len) {
        InstrPropsBase::init(mem, code_len);
        packed_flags = (U_8*)mem.calloc( ((code_len/4) & ~3) + 4);
    }

    //pass1: 00 - new (or dead code), 01 - parsed, 10 - middle of instruction, 11 - 'special' parsed (special == has stackmap)
    //returns 1 if mask is 01 (parsed) or 11 ('special' parsed  special == has stackmap)
    int isParsePassed(Address instr) {
        return get_dirty_mask(instr) & 1;
    }

    //pass1: 00 - new (or dead code), 01 - parsed, 10 - middle of instruction, 11 - 'special' parsed (special == has stackmap)
    //returns 1 if mask is 10 (middle of instruction)
    int isOperand(Address instr) {
        return (get_dirty_mask(instr) & 3) == 2;
    }

    //pass1: 00 - new (or dead code), 01 - parsed, 10 - middle of instruction, 11 - 'special' parsed (special == has stackmap)
    //setls low mask bit to 1
    void setParsePassed(Address instr) {
        fill_mask(instr, 1);
    }

    //pass1: 00 - new (or dead code), 01 - parsed, 10 - middle of instruction, 11 - 'special' parsed (special == has stackmap)
    //sets mask to 10
    int setOperand(Address instr) {
        int idx = instr/4;
        int shift = ((instr % 4) * 2);

        int mask01 = 1 << shift;
        int mask10 = 2 << shift;

        //is an instruction
        if( packed_flags[idx] & mask01 ) return 0;

        //mark as a middle
        packed_flags[idx] |= mask10;
        return 1;
    }

    //pass1: 00 - new (or dead code), 01 - parsed, 10 - middle of instruction, 11 - 'special' parsed (special == has stackmap)
    //when we calculate stackmaps, we set lower bit to 1 after the first pass
    //set mask to 11
    void setMultiway(Address instr) {
        fill_mask(instr, 3);
    }

    //pass2: 01 - new, 11 - special, 00 - passed (or unused), 10 - special passed (or unused)
    //when we calculate stackmaps: 01 - beginning of new or dead, 11 - special or middle, 00 passed, 10 special passed
    //for all instructions (except unuzed) returns 1 if it's 'passed' or 'special passed'
    //return 0 otherwise
    int isDataflowPassed(Address instr) {
        return !(get_dirty_mask(instr) & 1);
    }

    //return 1 for special and special passed instructions (instructions that are achievable by multiple passes)
    int isMultiway(Address instr) { //II_MULTIWAY
        return get_dirty_mask(instr) & 2;
    }

    //mark instruction as passed
    void setDataflowPassed(Address instr) {
        clear_mask(instr, 1);
    }

    void pass2started(int stackmapattr_calculation, Address code_length) {
        //touch dead and middles so that we can distinguish between two times passed and dead 
        if( stackmapattr_calculation ) {
            for( Address instr = 0; instr < code_length; instr++ ) {
                fill_mask(instr, 1);
            }
        }
    }

    int isDeadCodeStart(Address instr) { //dead block begins with 01, middles are 11, ends with 10 or 00
        return (get_dirty_mask(instr) & 3) == 1;
    }
};

#endif
