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
#ifndef __STACKMAP_H__
#define __STACKMAP_H__

#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "ver_utils.h"

//predefined verification types
enum SmConstPredefined {
    SM_NONE                         = 0,
    SM_LOW_WORD                     = 1,
    SM_REF_OR_UNINIT_OR_RETADR      = 3,
    SM_REF_OR_UNINIT                = 5,
    SM_THISUNINIT                   = 7,
    SM_ANYARRAY                     = 9,
    SM_NULL                         = 11,
    SM_HIGH_WORD                    = 13,
    SM_INTEGER                      = 15,
    SM_FLOAT                        = 17,
    SM_BOGUS                        = 19,
    SM_LONG                         = 21,
    SM_DOUBLE                       = 23,
};

//verification types with comparision operators
struct _SmConstant {
    unsigned c;

    int operator ==(_SmConstant other) {
        return c == other.c;
    }

    int operator ==(unsigned other) {
        return c == other;
    }

    int operator !=(_SmConstant other) {
        return c != other.c;
    }

    int operator !=(unsigned other) {
        return c != other;
    }


};

static const unsigned TYPE_RETADDR   = 0x2000000;
static const unsigned TYPE_REFERENCE = 0x4000000;
static const unsigned TYPE_NEWOBJECT = 0x8000000;

//verification types with convinient functions
struct SmConstant : _SmConstant {
    //all constants except SM_NONE must be odd

    //default constructor
    SmConstant() {}

    //creating from unsigned
    SmConstant(unsigned int other) {
        c = other;
    }

    //copy constructor
    SmConstant(const _SmConstant other) {
        c = other.c;
    }

    ///////////////////////////////////////

    //is it a RETADDR verification type? (that's pushed by JSR instructions)
    int isRetAddr() {
        return c & TYPE_RETADDR;
    }

    //is it a reference? (like Object)
    int isReference() {
        return c & TYPE_REFERENCE;
    }

    //is it a new object? (e.g. just created by 'new' instruction)
    int isNewObject() {
        return c & TYPE_NEWOBJECT;
    }

    //is it a primitive verification type? (e.g. int, long)
    int isPrimitive() {
        return !(c & (TYPE_NEWOBJECT | TYPE_REFERENCE | TYPE_RETADDR));
    }

    //is it a two-word type?
    int isLongOrDouble() {
        return c == SM_LONG || c == SM_DOUBLE;
    }

    //does merge with any other type results in SM_BOGUS?
    int isNonMergeable() {
        return (c & (TYPE_NEWOBJECT|TYPE_RETADDR)) || c == SM_THISUNINIT;
    }

    ///////////////////////////////////////

    //for a reference: return class id in the table (see tpool)
    int getReferenceIdx() {
        assert(isReference());
        return (c & ~TYPE_REFERENCE) >> 1;
    }

    //for 'new' type: return address of the 'new' instruction created this SmConstant
    Address getNewInstr() {
        assert(isNewObject());
        return (c & ~TYPE_NEWOBJECT) >> 1;
    }

    //for RetAddress: return address of the subroutine start (i.e. target of JSR instruction)
    //Note: this is different from what is recorded in RetAddress type when actual execution happens
    Address getRetInstr() {
        assert(isRetAddr());
        return (c & ~TYPE_RETADDR) >> 1;
    }

    ///////////////////////////////////////

    //create "new object" verification type corresponding to 'instr'
    static SmConstant getNewObject(Address instr) {
        return ((instr<<1) | (TYPE_NEWOBJECT | 1));
    }

    //create "ret address" verification type corresponding to subroutine startig at 'instr'
    //Note: this is different from what is recorded in RetAddress type when actual execution happens
    static SmConstant getRetAddr(Address instr) {
        return ((instr<<1) | (TYPE_RETADDR | 1));
    }

    //create "object" verification type
    static SmConstant getReference(unsigned idx) {
        return ((idx<<1) | (TYPE_REFERENCE | 1));
    }

};

//Store various data for the given instruction. Possible data are: StackMap vector, WorkMap vector,
// the list is used to organize storing Props as a HashTable
struct PropsHeadBase {
    // Address of the instruction for which this properties are stored
    // or 0xFFFF if this is a subroutine data for previous PropsHead
    // TODO: if instr_flags are not optimized, introduce a 'subroutine data' flag and get rid of 0xFFFF instructions
    Address instr;

    //next property in the list
    PropsHeadBase* next;
};

#endif
