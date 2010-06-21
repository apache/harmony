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
 */
 
#include "Ia32IRManager.h"
#include "LoopTree.h"
#include "Ia32Printer.h"

namespace Jitrino {
namespace Ia32 {

    
/************************************************************************/
/************************************************************************/
/*  FREQUENCY CALCULATION                                               */
/************************************************************************/
/************************************************************************/
#define PROB_UNKNOWN_BB 0.5
#define PROB_LOOP_EXIT  (PROB_UNKNOWN_BB * 0.1)
#define PROB_EXCEPTION 0.0000001
#define ACCEPTABLE_DOUBLE_PRECISION_LOSS  0.000000001 
#define ABS(a) (((a) > (0)) ? (a) : -(a))
#define DEFAULT_ENTRY_NODE_FREQUENCY 10000

static void fixEdgeProbs(ControlFlowGraph* cfg);

void IRManager::fixEdgeProfile() {
    
    if (fg->hasEdgeProfile() && fg->isEdgeProfileConsistent()) {
        return;
    }

    //step1: fix edge-probs, try to reuse old probs as much as possible..
    fixEdgeProbs(fg);
    
    //step2 : calculate frequencies
    fg->smoothEdgeProfile();
}

void fixEdgeProbs(ControlFlowGraph* cfg) {
    LoopTree* lt = cfg->getLoopTree();
    lt->rebuild(false);


    //fix edge-probs, try to reuse old probs as much as possible..    
    const Nodes& nodes = cfg->getNodesPostOrder(); //fix only reachable
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        const Edges& edges = node->getOutEdges();
        if (edges.empty()) {
            assert(node == cfg->getExitNode());
            continue;
        }
        double sumProb = 0;
        bool foundNotEstimated = false;
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
            Edge* edge = *ite;
            double prob = edge->getEdgeProb();
            sumProb+=prob;
            foundNotEstimated = foundNotEstimated || prob <= 0;
        }
        if (sumProb==1 && !foundNotEstimated) {
            continue; //ok, nothing to fix
        }
        //now fix probs..
        if (foundNotEstimated) {
            sumProb = 0;
            for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
                Edge* edge = *ite;
                double prob = edge->getEdgeProb();
                if (prob <= 0) {
                    Node* target = edge->getTargetNode();
                    if (!target->isBlockNode() || prob == 0) {
                        prob = PROB_EXCEPTION;
                    } else {
                        prob = lt->isLoopExit(edge) ? PROB_LOOP_EXIT : PROB_UNKNOWN_BB;
                    }
                    edge->setEdgeProb(prob);
                }
                assert(edge->getEdgeProb() > 0);
                sumProb+=prob;
            }
        }
        double mult = 1 / sumProb;

        sumProb = 0;
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
            Edge* e = *ite;
            double prob = e->getEdgeProb();
            prob = prob * mult;
            assert(prob > 0);
            e->setEdgeProb(prob);
#ifdef _DEBUG
            sumProb+=prob;
#endif              
        }
        assert(ABS(1-sumProb) < ACCEPTABLE_DOUBLE_PRECISION_LOSS);
    }
    if (cfg->getEntryNode()->getExecCount()<=0) {
        cfg->getEntryNode()->setExecCount(DEFAULT_ENTRY_NODE_FREQUENCY);
    }
    cfg->setEdgeProfile(true);
}

}} //namespaces


