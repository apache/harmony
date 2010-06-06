/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#ifndef _LOOP_INFO_H_
#define _LOOP_INFO_H_

#include "Loop.h"
#include "expr.h"

namespace Jitrino {

class ScalarEvolution;

// Information of a loop in loop nest.

class LoopInfo
{
public:
  LoopInfo (LoopNode *l)
    : loop (l), single_exit_edge (NULL), continue_edge (NULL),
      exit_branch_inst (NULL), iteration_number (NULL) {}

  LoopNode* get_loop () const { return loop; }

  Edge* get_single_exit_edge () const { return single_exit_edge; }

  Edge* get_continue_edge () const { return continue_edge; }

  BranchInst* get_exit_branch_inst () const { return exit_branch_inst; }

  Expr* get_iteration_number () const { return iteration_number; }

  // Set SINGLE_EXIT_EDGE and EXIT_BRANCH_INST for LOOP and return the
  // SINGLE_EXIT_EDGE.
  Edge* find_single_exit ();

  // Compute iteration number of LOOP with SCEV_ANALYZER and save the
  // result to ITERATION_NUMBER and return it.
  Expr* set_iteration_number (ScalarEvolution &scev_analyzer);

  // Return the edge from the single latch to the header of LOOP.
  Edge* get_single_latch_edge () const;

  // Return the edge from the single preheader to the header of LOOP.
  Edge* get_single_preheader_edge () const;

  // Return the single latch node, i.e. the only node in LOOP that is
  // the predecessor of header of LOOP.
  Node* get_single_latch_node () const;

  // Return the single preheader node of LOOP.
  Node* get_single_preheader_node () const;

private:
  static Edge* single_exit (LoopNode *);

private:
  LoopNode *loop;

  // The single exit edge of LOOP.
  Edge *single_exit_edge;

  // The edge of the continue branch from the exit node.
  Edge *continue_edge;

  // The single exit branch instruction of LOOP.
  BranchInst *exit_branch_inst;

  // The number of iterations of LOOP.
  Expr *iteration_number;
};

typedef std::vector<LoopInfo> LoopInfos;

}

#endif
