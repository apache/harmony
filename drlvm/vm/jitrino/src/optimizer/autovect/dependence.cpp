/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#include "FlowGraph.h"
#include "dependence.h"

namespace Jitrino {

bool
DataDependence::compute_ddg_for (LoopNode *loop)
{
  loop_nest.clear ();
  ddg_nodes.clear ();
  failed_reason = compute_ddg_for_1 (loop);
  dump_analysis_result (loop);

  return failed_reason == NULL;
}

// Helper function of compute_ddg_for.  Return NULL if successful,
// otherwise return the failed reasion string.

const char*
DataDependence::compute_ddg_for_1 (LoopNode *loop)
{
  if (!find_loop_nest (loop))
    return "find_loop_nest FAILED";

  if (!compute_iteration_numbers ())
    return "compute_iteration_numbers FAILED";

  if (const char *err = find_ddg_nodes (loop))
    return err;

  // Compute dependences connected by scalar variables.
  for (int i = 0; i < (int)ddg_nodes.size (); i++)
    {
      DdgNode *n1= ddg_nodes[i];
      Inst *inst = n1->inst;

      for (unsigned j = 0; j < inst->getNumSrcOperands (); j++)
        if (DdgNode *n2 = inst_to_ddg_node.lookup
            (inst->getSrc(j)->asSsaOpnd()->getInst ()))
          {
            // All operands of normal computations and the source
            // operand of stores are computation relevant.
            bool cr = (n1->kind == DdgNode::ddg_scalar_without_scev
                       || (n1->is_store () && j == 0));
            new (memory) DdgEdge (n2, n1, cr);
          }
    }

  // Compute dependences connected by memory references.
  for (int i = 0; i < (int)ddg_nodes.size () - 1; i++)
    {
      if (ddg_nodes[i]->kind != DdgNode::ddg_memory)
        continue;

      for (unsigned j = i + 1; j < ddg_nodes.size (); j++)
        if (ddg_nodes[j]->kind == DdgNode::ddg_memory)
          compute_data_dependence_between (ddg_nodes[i], ddg_nodes[j]);
    }

  return NULL;
}

// Helper function of find_loop_nest.

bool
DataDependence::find_loop_nest_1 (LoopNode *loop)
{
  if (loop->getSiblings ())
    return false;

  loop_nest.push_back (LoopInfo (loop));

  if (loop->getChild ())
    return find_loop_nest_1 (loop->getChild ());

  return true;
}

// Find and store loop nest starting at loop into LOOP_NEST.

bool
DataDependence::find_loop_nest (LoopNode *loop)
{
  loop_nest.push_back (LoopInfo (loop));

  if (loop->getChild ())
    return find_loop_nest_1 (loop->getChild ());

  return true;
}

// Set iteration numbers for each loop in this->LOOP_NEST.  If any one
// of them cannot be computed (NULL returned), return false.

bool
DataDependence::compute_iteration_numbers ()
{
  for (unsigned i = 0; i < loop_nest.size (); i++)
    if (!loop_nest[i].set_iteration_number (scev_analyzer))
      return false;

  return true;
}

bool
DataDependence::scalar_statement_p (Opcode opcode)
{
  return ((opcode >= Op_Add && opcode <= Op_Cmp) || opcode == Op_Phi
          || opcode == Op_LdVar || opcode == Op_StVar || opcode == Op_Copy);
}

// Compute the scalar evolution for using INDEX at INST.

Expr*
DataDependence::compute_scev (SsaOpnd *index, Inst *inst)
{
  LoopNode *loop = loop_tree->getLoopNode (inst->getNode (), false);
  Expr *scev = (index ? scev_analyzer.analyze_scev (index, loop)
                : scev_analyzer.get_int_zero ());

  return scev_analyzer.instantiate_scev (scev, loop, loop_nest[0].get_loop ());
}

// Create and push back a new DDG node and set its ID automatically.

void
DataDependence::add_ddg_node (DdgNode::NodeKind k, Inst *i, SsaOpnd *b,
                              SsaOpnd *x, Expr *f, bool r)
{
  DdgNode *node = new (memory) DdgNode (k, ddg_nodes.size (), i, b, x, f, r);

  ddg_nodes.push_back (node);
  inst_to_ddg_node.insert (node->inst, node);
}

// Return true if NODE is a loop exit node.

bool
DataDependence::is_loop_exit_node (Node *node)
{
  const Edges &edges = node->getOutEdges ();

  for (unsigned i = 0; i < edges.size (); i++)
    if (loop_tree->isLoopExit (edges[i]))
      return true;

  return false;
}

// Find all DDG nodes (memory-access instructions) in LOOP and store
// them to NODES.  Return true if all data references are array references.

const char*
DataDependence::find_ddg_nodes (LoopNode *loop)
{
  const Nodes &nodes_in_loop = loop->getNodesInLoop ();
  std::vector<Node*> bbs (nodes_in_loop.size ());

  // Copy nodes in loop to bbs so that we can sort them.
  std::copy (nodes_in_loop.begin (), nodes_in_loop.end (), bbs.begin ());
  // Sort blocks.  For reasons, see comments of node_post_num_greater.
  std::sort (bbs.begin (), bbs.end (), node_post_num_greater);

  for (std::vector<Node*>::const_iterator i = bbs.begin ();
       i != bbs.end ();
       i++)
    {
      Node *node = *i;
      Inst *label = (Inst*)node->getLabelInst ();
      SsaOpnd *base = NULL, *index = NULL;
      Expr *fn;

      for (Inst *inst = label->getNextInst ();
	   inst && inst != label;
	   inst = inst->getNextInst ())
        switch (Opcode opcode = inst->getOpcode ())
          {
          case Op_TauLdInd:
            if (!extract_base_and_index (inst->getSrc(0)->asSsaOpnd (), base, index))
              return "extract_base_and_index FAILED";

            if (!loop_nest_invariant_base_p (base))
              return "checking loop-invariant array base FAILED";

            if (!(fn = compute_scev (index, inst)))
              return "computing access function FAILED";

            add_ddg_node (DdgNode::ddg_memory, inst, base, index, fn, true);
            break;

          case Op_TauStInd:
            if (!extract_base_and_index (inst->getSrc(1)->asSsaOpnd (), base, index))
              return "extract_base_and_index FAILED";

            if (!loop_nest_invariant_base_p (base))
              return "checking loop-invariant array base FAILED";

            if (!(fn = compute_scev (index, inst)))
              return "computing access function FAILED";

            add_ddg_node (DdgNode::ddg_memory, inst, base, index, fn, false);
            break;

          case Op_DirectCall:
          case Op_TauVirtualCall:
          case Op_IndirectCall:
          case Op_IndirectMemoryCall:
            // TODO: Handle call instructions that may clobber memory.
            return "checking instruction pattern FAILED";

          case Op_JitHelperCall:
          case Op_VMHelperCall:
            // They shouldn't clobber array contents.  Is it right?
            break;

          case Op_Branch:
            // TODO: We currently disallow all control dependences
            // except the loop exit branch.
            if (!is_loop_exit_node (node))
              return "found non-loop-exit branch FAILED";
            else
              // Ignore the loop-exit branch (it shoud be the only one).
              break;

          case Op_AddScaledIndex:
          // case Op_TauCheckBounds:
          case Op_TauPoint:
          case Op_TauAnd:
          case Op_TauSafe:
          case Op_PseudoThrow:
          case Op_MethodEntry:
          case Op_MethodEnd:
            add_ddg_node (DdgNode::ddg_scalar_without_scev, inst,
                          NULL, NULL, NULL, false);
            break;

          default:
            if (scalar_statement_p (opcode))
              {
                SsaOpnd *dst = inst->getDst()->asSsaOpnd ();

                if (Expr *scev = compute_scev (dst, inst))
                  // It's an inductiion variable, simply save it.
                  add_ddg_node (DdgNode::ddg_scalar_with_scev, inst,
                                NULL, dst, scev, false);
                else
                  // Otherwise, add a pure scalar statement node.
                  add_ddg_node (DdgNode::ddg_scalar_without_scev, inst,
                                NULL, NULL, NULL, false);
              }
            else
              return "checking instruction pattern FAILED";
          }
    }

  return NULL;
}

// Return true iff BASE is invariant in this loop nest.

bool
DataDependence::loop_nest_invariant_base_p (SsaOpnd *base)
{
  Inst *inst = base->getInst ();

  if (inst->getOpcode () == Op_LdArrayBaseAddr)
    // Get the definition instruction of the array object.
    inst = strip_copies(inst->getSrc(0)->asSsaOpnd ())->getInst ();

  return !loop_nest[0].get_loop()->inLoop (inst->getNode ());
}

// Computes data dependence relation between NODE1 and NODE2 connected
// by memory references.  If the dependence exists, create and set the
// edge for the two nodes and return true, otherwise return false.

bool
DataDependence::compute_data_dependence_between (DdgNode *node1, DdgNode *node2)
{
  if (node1->is_read && node2->is_read)
    return false;

  bool has_dependence = false;
  Coefficients coef1, coef2;

  extract_coefficients (node1->scev_fn, coef1);
  extract_coefficients (node2->scev_fn, coef2);

  if (!gcd_test (coef1, coef2))
    return has_dependence;

  int iv_num, siv_index;
  bool strong_siv;

  test_iv_info (coef1, coef2, iv_num, siv_index, strong_siv);

  if (iv_num == 0)
    ziv_test (coef1, coef2, node1, node2);
  if (iv_num == 1 && strong_siv)
    has_dependence |= strong_siv_test (coef1, coef2, siv_index, node1, node2);
  else
    has_dependence |= miv_test (coef1, coef2, node1, node2);

  return has_dependence;
}

// Helper function of extract_coefficients.

void
DataDependence::extract_coefficients_1 (Expr *fn, Coefficients &coef)
{
  if (PolyChrec *chrec = fn->as_poly_chrec ())
    {
      extract_coefficients_1 (chrec->left, coef);

      assert (coef.size () > 0);

      // Fill zero coefficients for omited variables.
      for (int i = coef.size (); loop_nest[i - 1].get_loop () != chrec->loop; i++)
        coef.push_back (scev_analyzer.get_int_zero ());

      coef.push_back (chrec->right);
    }
  else
    coef.push_back (fn);
}

// Extract coefficients from the access function FN corresponding to
// this->LOOP_NEST.  See the comment of the type Coefficient.

void
DataDependence::extract_coefficients (Expr *fn, Coefficients &coef)
{
  coef.clear ();
  // Extract coefficients in FN.
  extract_coefficients_1 (fn, coef);

  // Fill zero coefficients for the rest omited iteration variables.
  for (unsigned i = coef.size (); i <= loop_nest.size (); i++)
    coef.push_back (scev_analyzer.get_int_zero ());
}

// Perform the GCD test on fn1 = a_0 + a_1 * x_1 + ... + a_n * x_n
// and fn2 = b_0 + b_1 * y_1 + ... + b_n * y_n, i.e. test whether
// gcd (a_1, ..., a_n, b_1, ..., b_n) divides b_0 - a_0.

bool
DataDependence::gcd_test (const Coefficients &fn1, const Coefficients &fn2)
{
  Expr *init_diff = scev_analyzer.fold_build (Op_Sub, fn2[0], fn1[0]);

  if (init_diff->int_zero_p ())
    // Zero can be exactly divided by any integer.
    return true;

  Expr *gcd = scev_analyzer.term_gcd (fn1[1], fn2[1]);

  for (unsigned i = 2; i < fn1.size (); i++)
    {
      gcd = scev_analyzer.term_gcd (gcd, fn1[i]);
      gcd = scev_analyzer.term_gcd (gcd, fn2[i]);
    }

  if (gcd->int_one_p ())
    // One can exactly divide any integer.
    return true;

  return scev_analyzer.may_divided_by_p (init_diff, gcd);
}

// Test induction variable information.  IV_NUM returns the number of
// IVs in FN1 and FN2.  If it's one, SIV_INDEX returns the common
// index of the single induction variable in FN1 and FN2.  If the
// coefficients of the common IV is same, STRONG_SIV returns true.
// If IV_NUM > 1, SIV_INDEX and STRONG_SIV are meaningless and should
// be ignored.

void
DataDependence::test_iv_info (const Coefficients &fn1, const Coefficients &fn2,
                              int &iv_num, int &siv_index, bool &strong_siv)
{
  iv_num = 0;
  siv_index = 0;
  strong_siv = false;

  for (unsigned i = 1; i < fn1.size (); i++)
    {
      if (fn1[i]->int_zero_p () && fn2[i]->int_zero_p ())
        continue;

      iv_num++;
      siv_index = i;
      Expr *diff = scev_analyzer.fold (Op_Sub, fn1[i], fn2[i]);
      strong_siv = diff && diff->int_zero_p ();
    }
}

// Create a direction vector with all possible directions, i.e.
// (*, ..., *)

DdgEdge::Direction*
DataDependence::create_all_dir_vector ()
{
  DdgEdge::Direction *dir_vect = new DdgEdge::Direction[loop_nest.size ()];

  for (unsigned i = 0; i < loop_nest.size (); i++)
    dir_vect[i] = DdgEdge::dir_all;

  return dir_vect;
}

// Return the reverse direction of DIR

DdgEdge::Direction
DataDependence::reverse_direction (DdgEdge::Direction dir)
{
  switch (dir)
    {
    case DdgEdge::dir_lt:
      return DdgEdge::dir_gt;
    case DdgEdge::dir_eq:
      return DdgEdge::dir_eq;
    case DdgEdge::dir_gt:
      return DdgEdge::dir_lt;
    case DdgEdge::dir_le:
      return DdgEdge::dir_ge;
    case DdgEdge::dir_lg:
      return DdgEdge::dir_lg;
    case DdgEdge::dir_ge:
      return DdgEdge::dir_le;
    case DdgEdge::dir_all:
      return DdgEdge::dir_all;
    default:
      assert (0);
      return DdgEdge::dir_all;
    }
}

// Perform the ZIV test.

bool
DataDependence::ziv_test (const Coefficients &fn1, const Coefficients &fn2,
                          DdgNode *node1, DdgNode *node2)
{
  Expr *diff = scev_analyzer.fold (Op_Sub, fn1[0], fn2[0]);

  if (diff && diff->int_nonzero_p ())
    // No dependence.
    return false;

  // Create all-direction dependences between NODE1 and NODE2.
  DdgEdge *edge1 = new (memory) DdgEdge (node1, node2);
  DdgEdge *edge2 = new (memory) DdgEdge (node2, node1);

  edge1->dir_vects.push_back (create_all_dir_vector ());
  edge2->dir_vects.push_back (create_all_dir_vector ());

  return true;
}

// Perform the strong SIV test.

bool
DataDependence::strong_siv_test (const Coefficients &fn1, const Coefficients &fn2,
                                 int siv_index, DdgNode *node1, DdgNode *node2)
{
  Expr *diff = scev_analyzer.fold (Op_TauDiv,
                                   scev_analyzer.fold_build (Op_Sub, fn1[0], fn2[0]),
                                   fn1[siv_index]);

  // TODO: We should test whether diff is in the proper range here
  // including whether the distance is greater than the vector factor.

  Integer *itmp;

  if (diff && (itmp = diff->as_integer ()))
    {
      if (siv_index == 1 && (itmp->value != 0 || loop_nest.size () == 1))
        // The first non-'=' element of the direction vector is not
        // '*', so we only need to create one edge.
        {
          DdgEdge *edge = (itmp->value < 0 ? new (memory) DdgEdge (node2, node1)
                           : new (memory) DdgEdge (node1, node2));
          DdgEdge::Direction* dir_vect = create_all_dir_vector ();

          dir_vect[0] = itmp->value ? DdgEdge::dir_lt : DdgEdge::dir_eq;
          edge->dir_vects.push_back (dir_vect);
        }
      else
        // Otherwise, it should be separated into two edges.
        {
          DdgEdge *edge1 = new (memory) DdgEdge (node1, node2);
          DdgEdge *edge2 = new (memory) DdgEdge (node2, node1);
          DdgEdge::Direction* dir_vect1 = create_all_dir_vector ();
          DdgEdge::Direction* dir_vect2 = create_all_dir_vector ();
          DdgEdge::Direction direction = (itmp->value > 0 ? DdgEdge::dir_lt
                                          : itmp->value < 0 ? DdgEdge::dir_gt
                                          : DdgEdge::dir_eq);

          dir_vect1[siv_index - 1] = direction;
          dir_vect2[siv_index - 1] = reverse_direction (direction);
          edge1->dir_vects.push_back (dir_vect1);
          edge2->dir_vects.push_back (dir_vect2);
        }
    }
  else
    {
      // Create two all-direction dependences between NODE1 and NODE2.
      DdgEdge *edge1 = new (memory) DdgEdge (node1, node2);
      DdgEdge *edge2 = new (memory) DdgEdge (node2, node1);

      edge1->dir_vects.push_back (create_all_dir_vector ());
      edge2->dir_vects.push_back (create_all_dir_vector ());
    }

  return true;
}

bool
DataDependence::miv_test (const Coefficients &fn1, const Coefficients &fn2,
                          DdgNode *node1, DdgNode *node2)
{
  // Create two all-direction dependences between NODE1 and NODE2.
  DdgEdge *edge1 = new (memory) DdgEdge (node1, node2);
  DdgEdge *edge2 = new (memory) DdgEdge (node2, node1);

  edge1->dir_vects.push_back (create_all_dir_vector ());
  edge2->dir_vects.push_back (create_all_dir_vector ());

  return true;
}

void
DataDependence::dump_dir_vect (std::ostream &out,
                               const DdgEdge::Direction *dir_vect)
{
  bool first_dir = true;

  out << "(";

  for (unsigned i = 0; i < loop_nest.size (); i++)
    {
      if (!first_dir)
        out << ", ";

      first_dir = false;

      switch (dir_vect[i])
        {
        case DdgEdge::dir_lt:
          out << "<";
          break;

        case DdgEdge::dir_eq:
          out << "=";
          break;

        case DdgEdge::dir_gt:
          out << ">";
          break;

        case DdgEdge::dir_le:
          out << "<=";
          break;

        case DdgEdge::dir_lg:
          out << "<>";
          break;

        case DdgEdge::dir_ge:
          out << ">=";
          break;

        case DdgEdge::dir_all:
          out << "*";
          break;
        }
    }

  out << ")";
}

void
DataDependence::dump_dir_vects (std::ostream &out, DdgEdge *edge)
{
  const DdgEdge::DirectionVectors &dir_vects = edge->dir_vects;

  out << "{";

  if (dir_vects.empty ())
    // It's a dependence edge connected by scalar variables.
    out << (edge->computation_relevant ? "SC" : "S");
  else
    {
      bool first_vect = true;

      for (unsigned i = 0; i < dir_vects.size (); i++)
        {
          if (!first_vect)
            out << ", ";

          first_vect = false;
          dump_dir_vect (out, dir_vects[i]);
        }
    }

  out << "}";
}

void
DataDependence::dump_ddg_node (std::ostream &out, const DdgNode *node)
{
  out << "I" << node->inst->getId () << ": ";

  switch (node->kind)
    {
    case DdgNode::ddg_scalar_with_scev:
      out << "variable: ";
      node->index->print (out);
      out << ", scev_fn: " << node->scev_fn;
      break;

    case DdgNode::ddg_scalar_without_scev:
      out << "ddg_scalar_without_scev";
      break;

    case DdgNode::ddg_memory:
      out << "base: ";
      node->base->print (out);
      out << ", index: ";

      if (node->index)
        node->index->print (out);
      else
        out << "0";

      out << ", " << "scev_fn: " << node->scev_fn;
      out << ", " << (node->is_read ? "read" : "write");
    }

  out << "\n  PREDS:";

  for (unsigned i = 0; i < node->preds.size (); i++)
    {
      DdgEdge *edge = node->preds[i];
      out << " I" << edge->src->inst->getId () << ":";
      dump_dir_vects (out, edge);
    }

  out << "\n  SUCCS:";

  for (unsigned i = 0; i < node->succs.size (); i++)
    {
      DdgEdge *edge = node->succs[i];
      out << " I" << edge->dst->inst->getId () << ":";
      dump_dir_vects (out, edge);
    }

  out << "\n";
}

void
DataDependence::dump_analysis_result (LoopNode *loop)
{
  if (!Log::isEnabled ())
    return;

  Log::out() << "*************** Data dependence analysis for loop nest <";
  bool first_loop = true;;

  // Print pre-numbers of loops of the loop nest.
  for (LoopInfos::const_iterator i = loop_nest.begin ();
       i != loop_nest.end ();
       i++)
    {
      if (!first_loop)
        Log::out() << ", ";

      first_loop = false;
      Log::out() << (*i).get_loop()->getPreNum ();
    }

  Log::out() << ">: "
             << (failed_reason ? failed_reason : "SUCCESSFUL")
             << " ***************\n";

  // Print nodes of each loop.
  for (unsigned i = 0; i < loop_nest.size (); i++)
    {
      Log::out() << "  Loop " << loop_nest[i].get_loop()->getPreNum ()
                 << ": iteration_number = " << loop_nest[i].get_iteration_number ()
                 << ", nodes = {";

      const Nodes &nodes = loop_nest[i].get_loop()->getNodesInLoop ();
      bool first_node = true;

      for (Nodes::const_iterator j = nodes.begin ();
           j != nodes.end ();
           j++)
        {
          if (!first_node)
            Log::out() << ", ";

          first_node = false;
          FlowGraph::printLabel (Log::out (), *j);
        }

      Log::out() << "}\n";
    }

  Log::out() << "Data dependence graph nodes:\n";

  // Print DDG nodes in the loop nest.
  for (DdgNodes::iterator i = ddg_nodes.begin (); i != ddg_nodes.end (); i++)
    dump_ddg_node (Log::out (), *i);
}

void DataDependence::debug_data_dependences (LoopNode *loop)
{
  // Process all inter loops first.
  for (LoopNode *child = loop->getChild (); child; child = child->getSiblings ())
    debug_data_dependences (child);

  compute_ddg_for (loop);
}

}
