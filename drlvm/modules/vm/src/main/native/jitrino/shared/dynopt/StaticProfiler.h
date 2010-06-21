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
* @author Intel, Mikhail Y. Fursov
*
*/

#ifndef _JITRINO_STATIC_PROFILER_H_
#define _JITRINO_STATIC_PROFILER_H_

#include "optpass.h"
namespace Jitrino {

class FlowGraph;
class Node;
class Edge;


class StaticProfiler {
public:
    
    // estimates edge probabilities and calculates node frequencies
    static void estimateGraph(IRManager& irm, double entryFreq, bool cleanOldEstimations=false);
    
    static void fixEdgeProbs(IRManager& irm);
};

}//namespace
#endif
