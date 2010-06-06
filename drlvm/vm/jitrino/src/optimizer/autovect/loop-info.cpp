/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#include "loop-info.h"
#include "scalar-evolution.h"

namespace Jitrino {

Edge*
LoopInfo::find_single_exit ()
{
  Edge *exit_edge = single_exit (loop);

  if (!exit_edge)
    return NULL;

  single_exit_edge = exit_edge;

  Node *exit_node = exit_edge->getSourceNode();
  Inst *cond_inst = (Inst*)exit_node->getLastInst ();

  while (cond_inst->getOpcode () != Op_Branch)
    {
      cond_inst = cond_inst->getPrevInst ();

      if (!cond_inst)
        return NULL;
    }

  exit_branch_inst = cond_inst->asBranchInst ();

  const Edges &out_edges = exit_node->getOutEdges ();
  assert (out_edges.size () == 2);
  continue_edge = (out_edges[0] == exit_edge ? out_edges[1] : out_edges[0]);

  return exit_edge;
}

Expr*
LoopInfo::set_iteration_number (ScalarEvolution &scev_analyzer)
{
  return iteration_number = scev_analyzer.number_of_iterations (*this);
}

Edge*
LoopInfo::get_single_latch_edge () const
{
  Node *header = loop->getHeader ();
  const Edges &in_edges = header->getInEdges ();

  assert (in_edges.size () == 2);

  return in_edges[(loop->inLoop (in_edges[0]->getSourceNode ())
                   ? 0 : 1)];
}

Edge*
LoopInfo::get_single_preheader_edge () const
{
  Node *header = loop->getHeader ();
  const Edges &in_edges = header->getInEdges ();

  assert (in_edges.size () == 2);

  return in_edges[(loop->inLoop (in_edges[0]->getSourceNode ())
                   ? 1 : 0)];
}

Node*
LoopInfo::get_single_latch_node () const
{
  return get_single_latch_edge()->getSourceNode ();
}

Node*
LoopInfo::get_single_preheader_node () const
{
  return get_single_preheader_edge()->getSourceNode ();
}

// Return the exit edge of LOOP if it's the only exit edge of LOOP.
// Otherwise, return NULL.  This static method should probably be
// defined in the LoopNode class.

Edge*
LoopInfo::single_exit (LoopNode *loop)
{
  const Nodes &nodes = loop->getNodesInLoop ();
  Edge *exit_edge = NULL;

  for (unsigned i = 0; i < nodes.size (); i++)
    {
      const Edges &edges = nodes[i]->getOutEdges ();

      for (unsigned j = 0; j < edges.size (); j++)
        if (!edges[j]->getTargetNode()->isDispatchNode ()
            && loop->isLoopExit (edges[j]))
          {
            if (exit_edge)
              // There is more than one exit edge of LOOP.
              return NULL;

            exit_edge = edges[j];
          }
    }

  return exit_edge;
}

}
