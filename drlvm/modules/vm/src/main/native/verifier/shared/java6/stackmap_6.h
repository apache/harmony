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
#ifndef __STACKMAP6_H__
#define __STACKMAP6_H__

#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../base/stackmap_x.h"

//Constant for parsing StackMapTable attribute 
enum StackMapTableItems {
    ITEM_TOP = 0,
    ITEM_INTEGER = 1,
    ITEM_FLOAT = 2,
    ITEM_DOUBLE = 3,
    ITEM_LONG = 4,
    ITEM_NULL = 5,
    ITEM_UNINITIALIZEDTHIS = 6,
    ITEM_OBJECT = 7,
    ITEM_UNINITIALIZED = 8
};


//StackMapElement structure represens recorded verification types
//it's read from class file StackMapTable attribute
struct StackmapElement_6 {
    //list of IncomingType constraint
    _SmConstant const_val;
};

//WorkMapElement structure represent an element of the workmap vector -- vector of the derived types
//in Java6 verification type might be constant (or known) only 
struct WorkmapElement_6 {
    //the value
    _SmConstant const_val;      //either a constant (known-type)

    //// Java5 anachonisms ////
    void setJsrModified() {};
    int isJsrModified() { return 1;};
    SmConstant getAnyPossibleValue() { return const_val; }
    SmConstant getConst() { return const_val; }
};

//WorkmapElement type with some constructors
struct _WorkmapElement_6 : WorkmapElement_6 {
    _WorkmapElement_6(WorkmapElement_6 other) {
        const_val = other.const_val;
    }

    _WorkmapElement_6(SmConstant c) {
        const_val = c;
    }
};


//Store stackmap data for the given instruction
// the list is used to organize storing Props as a HashTable
struct PropsHead_6 : public PropsHeadBase {
    typedef MapHead<StackmapElement_6> StackmapHead;

    //possible properties
    StackmapHead stackmap;

    //get stackmap stored here
    StackmapHead *getStackmap() {
        return &stackmap;
    }
};

#endif
