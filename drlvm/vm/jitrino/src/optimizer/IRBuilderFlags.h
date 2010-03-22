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
* @version $
*
*/

#ifndef _IRBUILDERFLAGS_H_
#define _IRBUILDERFLAGS_H_

namespace Jitrino {
//
// flags that control how the IRBuilder expands and optimizes IR instructions
//
struct IRBuilderFlags {
    IRBuilderFlags () {
        expandMemAddrs = expandNullChecks = false;
        expandCallAddrs = expandVirtualCallAddrs = false;
        expandElemAddrs = expandElemTypeChecks = false;
        doSimplify = doCSE = false;
        insertMethodLabels = insertWriteBarriers = false;
        suppressCheckBounds = false;
        compressedReferences = false;
        genMinMaxAbs = false;
        genFMinMaxAbs = false;
        useNewTypeSystem = false;
    }
    /* expansion flags */
    bool expandMemAddrs      : 1;    // expand field/array element accesses
    bool expandElemAddrs     : 1;    // expand array elem address computation
    bool expandCallAddrs     : 1;    // expand fun address computation for direct calls
    bool expandVirtualCallAddrs : 1; // expand fun address computation for virtual calls
    bool expandNullChecks    : 1;    // explicit null checks
    bool expandElemTypeChecks: 1;    // explicit array elem type checks for stores
    /* optimization flags */
    bool doCSE               : 1;    // common subexpression elimination
    bool doSimplify          : 1;    // simplification
    /* label & debug insertion */
    bool insertMethodLabels  : 1;    // insert method entry/exit labels
    bool insertWriteBarriers : 1;    // insert write barriers to mem stores
    bool suppressCheckBounds : 1;
    bool compressedReferences: 1;    // are refs in heap compressed?
    bool genMinMaxAbs        : 1;
    bool genFMinMaxAbs       : 1;
    // LBS Project flags
    bool useNewTypeSystem    : 1;    // Use the new LBS type system rather than the old one
};

} //namespace

#endif // _IRBUILDERFLAGS_H_
