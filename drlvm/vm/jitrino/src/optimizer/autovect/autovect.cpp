/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#include "optpass.h"
#include "FlowGraph.h"
#include "escapeanalyzer.h"
#include "scalar-evolution.h"
#include "dependence.h"

namespace Jitrino {

class SingleLoopVectorizer
{
public:
  SingleLoopVectorizer (IRManager &irm, ScalarEvolution &sceva)
    : ir_manager (irm),
      memory ("SingleLoopVectorizer"),
      scev_analyzer (sceva),
      data_dependence (irm.getLoopTree (), scev_analyzer),
      var_to_var_vec (memory, 107),
      nodes_in_loop (memory, irm.getFlowGraph().getMaxNodeId ())
  {
  }

  // Determine whether the vectorization should be done for LOOP.

  bool
  should_vectorize (LoopNode *loop)
  {
    if (!data_dependence.compute_ddg_for (loop)
        // This class can only vectorize single loop.
        || data_dependence.get_loop_nest().size () > 1)
      return false;

    loop_info = &data_dependence.get_loop_nest()[0];

    find_ddg_sccs ();

    if (compute_runtime_alias_tests ())
      // There are runtime alias tests generated, recompute the SCCs.
      find_ddg_sccs ();

    // Check operations, access consecutive etc. and profitability.
    if (compute_actions_and_profit () <= 0)
      return false;

    if (Log::isEnabled ())
      Log::out() << "Should be vectorized.\n";

    const Nodes &loop_nodes = loop_info->get_loop()->getNodesInLoop ();

    nodes_in_loop.clear ();

    // Store the nodes in loop before performing any transformations.
    for (Nodes::const_iterator it = loop_nodes.begin ();
         it != loop_nodes.end ();
         it++)
      nodes_in_loop.setBit ((*it)->getId ());

    // Store the header node, latch node and preheader node before
    // performing any transformations since the loop-tree will become
    // invalid after the CFG is modified.
    header_node = loop_info->get_loop()->getHeader ();
    latch_node = loop_info->get_single_latch_node ();
    preheader_edge = loop_info->get_single_preheader_edge ();

    return true;
  }

  // Transform the vectorizable single loop.

  void
  transform ()
  {
    transform_single_loop ();
  }

private:

  // Pair of DDG nodes.
  typedef std::pair<DdgNode *, DdgNode *> DdgNodePair;

  // The set for storing distinct DDG node pairs.
  typedef std::set<DdgNodePair> DdgNodePairSet;

  // Vector of SsaOpnd
  typedef std::vector<SsaOpnd*> SsaOpndVec;

  // The hash table for mapping variables to variable vectors.
  class MapSsaOpndToSsaOpndVec : public HashTable<SsaOpnd, SsaOpndVec>
  {
  public:
    MapSsaOpndToSsaOpndVec (MemoryManager &m, U_32 s)
      : HashTable<SsaOpnd, SsaOpndVec> (m, s) {}

  protected:
    virtual bool keyEquals (SsaOpnd *key1, SsaOpnd *key2) const
    {
      return key1 == key2;
    }

    virtual U_32 getKeyHashCode (SsaOpnd *key) const
    {
      return (U_32)((long)key >> 2);
    }
  };

  // Actions for processing each DDG node.
  enum Action
    {
      act_nothing = -3,     // do nothing
      act_on_demand = -2,   // compute on demand
      act_unroll = -1,      // unroll the instruction
      act_unknown = 0,      // unset yet
      // Vectorize the instruction if the action is in
      // [act_min_vf, act_max_vf].
      act_min_vf = 2,       // minimal vector factor
      act_max_vf = 64       // maximal vector factor
    };

private:
  IRManager &ir_manager;

  MemoryManager memory;

  ScalarEvolution &scev_analyzer;

  // Used to analyze the loop nest and construct its DDG.
  DataDependence data_dependence;

  // The loop-info of the single loop to be processed.
  const LoopInfo *loop_info;

  // The common (maximal) vectorization factor.
  int common_vf;

  // Stores SCCs of the DDG.
  DdgNodes ddg_sccs;
  // SCC leader of each node.  Two nodes has the same SCC leader iff
  // they are in the same SCC.
  DdgNodes scc_leader;
  // For finding SCCs of the DDG.
  std::vector<int> ddg_dfs_num;
  std::vector<int> ddg_dfs_low_num;
  DdgNodes ddg_node_stack;
  std::vector<bool> is_ddg_node_in_stack;
  int next_ddg_dfs_num;

  // Edges to be tested alias at runtime.
  DdgEdges edges_to_be_runtime_tested;

  // Whether each DDG node should be vectorized.
  std::vector<Action> action_for_node;

  // The map for mapping variables to variable vectors that contain
  // corresponding variables after vectorization or unrolling.
  MapSsaOpndToSsaOpndVec var_to_var_vec;

  // Nodes in the loop of LOOP_INFO saved before transformation.
  StlBitVector nodes_in_loop;

  // Header node, latch node and preheader edge of the loop of
  // LOOP_INFO saved before transformation.
  Node *header_node;
  Node *latch_node;
  Edge *preheader_edge;

private:

  // Used for sorting DDG nodes in ascending order of the node ID.

  static bool ddg_node_id_less (DdgNode *n1, DdgNode *n2)
  {
    return n1->id < n2->id;
  }

  // Depth-first search the DDG and find all SCCs.

  void
  ddg_dfs (DdgNode *node)
  {
    int id = node->id;

    ddg_dfs_low_num[id] = ddg_dfs_num[id] = next_ddg_dfs_num++;
    ddg_node_stack.push_back (node);
    is_ddg_node_in_stack[id] = true;

    for (unsigned i = 0; i < node->preds.size (); i++)
      {
        DdgEdge *pred_edge = node->preds[i];

        if (pred_edge->runtime_alias_test)
          // The edge is ensured to not exist by runtime alias test.
          continue;

        DdgNode *node1 = pred_edge->src;
        int id1 = node1->id;

        if (!ddg_dfs_num[id1])
          {
            ddg_dfs (node1);
            ddg_dfs_low_num[id] = std::min (ddg_dfs_low_num[id], ddg_dfs_low_num[id1]);
          }

        if (ddg_dfs_num[id1] < ddg_dfs_num[id] && is_ddg_node_in_stack[id1])
          ddg_dfs_low_num[id] = std::min (ddg_dfs_low_num[id], ddg_dfs_num[id1]);
      }

    if (ddg_dfs_low_num[id] == ddg_dfs_num[id])
      {
        int i = ddg_node_stack.size ();
        DdgNode *node1;
        DdgNodes::iterator first = ddg_sccs.end ();

        do
          {
            node1 = ddg_node_stack[--i];
            is_ddg_node_in_stack[node1->id] = false;
            ddg_sccs.push_back (node1);
            scc_leader[node1->id] = node;
          } while (node1 != node);

        // Sort elements of the SCC in ascending order of the node ID.
        std::sort (first, ddg_sccs.end (), ddg_node_id_less);

        // Use NULL to separate SCC sets.
        ddg_sccs.push_back (NULL);

        ddg_node_stack.resize (i);
      }
  }

  // Find all SCCs of the DDG in the reversed DFS order along the
  // backward direction of DDG edges.  The output ddg_dfs is in the
  // following format: {root_of_ddg_dfs, {n11, ..., n1i, NULL}, {n21,
  // ... n2j, NULL}, ..., {nk1, ... nkl, NULL}, NULL},
  // {root_of_ddg_dfs, {...}, ..., {...}, NULL}.  Nodes in the same
  // SCC are sorted in the ascending order of the node ID.

  void
  find_ddg_sccs ()
  {
    const DdgNodes &ddg_nodes = data_dependence.get_ddg_nodes ();

    ddg_sccs.clear ();
    ddg_sccs.reserve (ddg_nodes.size () * 4);
    scc_leader.clear ();
    scc_leader.resize (ddg_nodes.size ());
    ddg_dfs_num.clear ();
    ddg_dfs_num.resize (ddg_nodes.size ());
    ddg_dfs_low_num.clear ();
    ddg_dfs_low_num.resize (ddg_nodes.size ());
    ddg_node_stack.clear ();
    is_ddg_node_in_stack.clear ();
    is_ddg_node_in_stack.resize (ddg_nodes.size ());
    next_ddg_dfs_num = 1;

    for (unsigned i = 0; i < ddg_nodes.size (); i++)
      {
        DdgNode *ddg_node = ddg_nodes[i];

        if (ddg_node->kind != DdgNode::ddg_scalar_with_scev)
          // Instructions to be vectorized or unrolled.
          {
            if (!ddg_dfs_num[i])
              {
                // Push the root node of this DFS tree.
                ddg_sccs.push_back (ddg_node);
                // Search and store the DFS tree of SCCs.
                ddg_dfs (ddg_node);
                // Separate different DFS trees of SCCs.
                ddg_sccs.push_back (NULL);
              }
          }
      }
  }

  // Examine all edges between nodes in the same SCC to determine
  // whether they can and should be tested alias at runtime and store
  // such edges into edges_to_be_runtime_tested and return true if
  // such edges exist.

  bool
  compute_runtime_alias_tests ()
  {
    const DdgNodes &ddg_nodes = data_dependence.get_ddg_nodes ();

    edges_to_be_runtime_tested.clear ();

    for (unsigned i = 0; i < ddg_nodes.size (); i++)
      {
        DdgNode *node = ddg_nodes[i];

        if (node->kind != DdgNode::ddg_memory)
          // Only need to consider memory access nodes.
          continue;

        for (unsigned j = 0; j < node->preds.size (); j++)
          {
            DdgEdge *pred_edge = node->preds[j];
            DdgNode *node1 = pred_edge->src;

            if (node1->kind == DdgNode::ddg_memory
                && scc_leader[node1->id] == scc_leader[node->id]
                && node1->base != node->base)
              // node1 is also a memory access node and they are in
              // the same SCC, and they may be alias to each other,
              // test the edge at runtime.
              {
                pred_edge->runtime_alias_test = true;
                edges_to_be_runtime_tested.push_back (pred_edge);

                if (Log::isEnabled ())
                  Log::out() << "DDG edge to be tested alias at runtime: (I"
                             << pred_edge->src->inst->getId () << ", I"
                             << pred_edge->dst->inst->getId () << ")\n";
              }
          }
      }

    return edges_to_be_runtime_tested.size () > 0;
  }

  // Return true if we need to do nothing for the instruction with
  // OPCODE.

  bool
  need_to_do_nothing (Opcode opcode)
  {
    switch (opcode)
      {
      case Op_TauPoint:
      case Op_TauAnd:
      case Op_TauSafe:
      case Op_PseudoThrow:
      case Op_MethodEntry:
      case Op_MethodEnd:
        // FIXME: Check whether it's safe to do nothing for them.
        return true;

      default:
        return false;
      }
  }

  // Return the size of TYPE_TAG in bit if it's a basic type that may
  // be vectorized, return 0 otherwise.

  static int
  type_size_in_bit (Type::Tag type_tag)
  {
    switch (type_tag)
      {
      case Type::Int8:
      case Type::UInt8:
        return 8;

      case Type::Int16:
      case Type::UInt16:
        return 16;

      case Type::Int32:
      case Type::UInt32:
      case Type::Single:
        return 32;

      case Type::Int64:
      case Type::UInt64:
      case Type::Double:
        return 64;

      default:
        return 0;
      }
  }

  // Return the vectorization factor (VF) of type TYPE_TAG.

  static int
  vf_of_type (Type::Tag type_tag)
  {
    int type_size = type_size_in_bit (type_tag);

    // TODO: for now we only support the fixed 128 bytes vectors.
    return type_size ? 128 / type_size : 0;
  }

  // Return true iff DDG_NODE has a variant predecessor, i.e. a node
  // whose action is not act_nothing.

  bool
  has_variant_pred (DdgNode *ddg_node)
  {
    for (unsigned i = 0; i < ddg_node->preds.size (); i++)
      {
        DdgEdge *pred_edge = ddg_node->preds[i];

        if (!pred_edge->runtime_alias_test
            && action_for_node[pred_edge->src->id] != act_nothing)
          return true;
      }

    return false;
  }

  // Return true iff OPND is a constant.

  static bool
  is_constant (SsaOpnd *opnd)
  {
    return opnd->getInst()->getOpcode () == Op_LdConstant;
  }

  // Return true iff INST is a vectorizable instruction.

  static bool
  is_vectorizable (Inst *inst)
  {
    Opcode opcode = inst->getOpcode ();
    Type::Tag inst_type = inst->getType ();

    switch (opcode)
      {
      case Op_Add:
      case Op_Sub:
        // Support for all numeric types.
        return Type::isNumeric (inst_type);

      case Op_Mul:
        // Support for all numeric types except 8-bit integers.
        return (Type::isNumeric (inst_type)
                && type_size_in_bit (inst_type) > 8);

      case Op_TauDiv:
        // Current SSE only supports floating point division.
        return Type::isFloatingPoint (inst_type);

      case Op_And:
      case Op_Or:
      case Op_Xor:
      case Op_Not:
        // Support for all integer types.
        return Type::isInteger (inst_type);

      case Op_Shl:
        // Support for all integer types except 8-bit integers.
        return (is_constant (inst->getSrc(1)->asSsaOpnd())
                && Type::isInteger (inst_type)
                && type_size_in_bit (inst_type) > 8);

      case Op_Shr:
        // Support for 16 and 32 bit signed integer and 16 ~ 64 bit
        // unsigned integers.
        return (is_constant (inst->getSrc(1)->asSsaOpnd())
                && ((Type::isSignedInteger (inst_type)
                     && (type_size_in_bit (inst_type) == 16
                         || type_size_in_bit (inst_type) == 32))
                    || (Type::isUnsignedInteger (inst_type)
                        && (type_size_in_bit (inst_type) > 8))));

      case Op_Conv:
      case Op_ConvZE:
        // TODO: Only support conversion between int32 and float now.
        return (type_size_in_bit (inst->getSrc(0)->getType()->tag)
                == type_size_in_bit (inst_type)
                && type_size_in_bit (inst_type) == 32);

      case Op_LdConstant:
      case Op_Copy:
      case Op_LdVar:
      case Op_StVar:
        return true;

      case Op_TauLdInd:
        // Don't vectorize the load whose type is different from its
        // destination type, e.g. ldind.i1 [t0] -> t1:i4 generated from
        // classlib:nio_char/.../java/org/.../charset/ISO_8859_1.java
        // "((int) arr[i] & 0xFF)" in decodeLoop.
        return inst->getDst()->getType()->tag == inst_type;

      case Op_TauStInd:
        // Similar to Op_TauLdInd
        return inst->getSrc(0)->getType()->tag == inst_type;

      default:
        return false;
      }
  }

  // Compute vectorization factor (VF) from the type of DDG_NODE.  If
  // it cannot be vectorized, return act_unroll, otherwise return the
  // VF of that node.  VF is the number denoting how many scalar
  // elements can be processed in one corresponding vector operation
  // for a given scalar type.

  Action
  compute_vf_of_ddg_node (DdgNode *ddg_node)
  {
    Inst *inst = ddg_node->inst;
    Opcode opcode = inst->getOpcode ();
    Type::Tag inst_type = inst->getType ();

    if (!has_variant_pred (ddg_node))
      // If this node has no variant predecessor, it's invariant and
      // nothing needs to be done.
      return act_nothing;

    if (opcode == Op_AddScaledIndex)
      // Compute variant array addresses on demand rather than unroll
      // them for all iterations at once.
      return act_on_demand;

    if (!is_vectorizable (inst))
      // Unroll unvectorizable variant instructions.
      return act_unroll;

    if (int vf = vf_of_type (inst_type))
      return (Action)vf;
    else
      // The type is too complex and can't be vectorized.
      return act_unroll;
  }

  // Compute initial actions for nodes in a SCC and update COMMON_VF.

  void
  compute_actions_for_ddg_scc (DdgNodes::iterator &iter)
  {
    if (*(iter + 1))
      // The SCC forms cycles (equivalent to that it contains more
      // than one node since the instructions are decomposed into the
      // simple form), set the action for those nodes to act_unroll.
      do
        action_for_node[(*iter)->id] = act_unroll;
      while (*++iter);
    else
      // Otherwise, it conatins a single node.
      {
        DdgNode *ddg_node = *iter++;
        int id = ddg_node->id;
        Inst *inst = ddg_node->inst;
        Action action = act_unknown;
        PolyChrec *chrec;
        Integer *step;

        switch (ddg_node->kind)
          {
          case DdgNode::ddg_scalar_with_scev:
            action = act_unroll;
            break;

          case DdgNode::ddg_scalar_without_scev:
            action = (need_to_do_nothing (inst->getOpcode ())
                      ? act_nothing
                      : compute_vf_of_ddg_node (ddg_node));
            break;

          case DdgNode::ddg_memory:
            if (!(chrec = ddg_node->scev_fn->as_poly_chrec ())
                || ((step = chrec->right->as_integer ())
                    && step->value == 1))
              // The access function is loop-invariant or is
              // consecutive, try to vectorize it.
              action = compute_vf_of_ddg_node (ddg_node);
            else
              action = act_unroll;

            break;
          }

        if (action > common_vf)
          // Record the maximal vectorization factor.
          common_vf = action;

        action_for_node[id] = action;
      }
  }

  // Optimize actions of each DDG node (now, we reset actions of
  // costly nodes that have been set as to be vectorized in the
  // initial action computation to act_unroll) and compute the profit.

  int
  optimize_actions_and_compute_profit ()
  {
    const DdgNodes &ddg_nodes = data_dependence.get_ddg_nodes ();
    int profit = 0;

    for (unsigned i = 0; i < ddg_nodes.size (); i++)
      {
        DdgNode *node = ddg_nodes[i];
        int id = node->id;

        if (action_for_node[id] < act_min_vf)
          continue;

        profit++;
      }

    return profit;
  }

  // Compute actions for each DDG node and the overall profit.

  int
  compute_actions_and_profit ()
  {
    const DdgNodes &ddg_nodes = data_dependence.get_ddg_nodes ();

    common_vf = act_unknown;
    action_for_node.clear ();
    action_for_node.resize (ddg_nodes.size (), act_unknown);

    for (DdgNodes::iterator i = ddg_sccs.begin (); i != ddg_sccs.end (); i++)
      {
        // Skip the DFS root.
        i++;

        // Process all SCCs in this DFS tree.
        do
          compute_actions_for_ddg_scc (i);
        while (*++i);
      }

    return optimize_actions_and_compute_profit ();
  }

  // Generate runtime alias test code between preheader_node and
  // header_node.  If all tests succeed, jump to header_node,
  // otherwise, jump to a new initialization node to epilogue_header.

  void
  generate_runtime_alias_tests (Node *epilogue_header, Node *exit_node,
                                LabelInst *true_target_label)
  {
    if (edges_to_be_runtime_tested.empty ())
      return;

    assert (header_node->getOutEdges().size () == 2);

    ControlFlowGraph &cfg = ir_manager.getFlowGraph ();
    // The bit vector containing all nodes so that duplicateRegion
    // won't generate ldvar/stvar.
    StlBitVector all_nodes (memory, cfg.getMaxNodeId (), true);
    DefUseBuilder def_uses (memory);

    def_uses.initialize (cfg);

    // Duplicate a new initialization node from alias testing fail
    // branch to the unvectorized loop.
    Node *init_node = FlowGraph::duplicateSingleNode (ir_manager,
                                                      header_node,
                                                      all_nodes,
                                                      def_uses);

    // Add edge from init_node to epilogue_header and to exit_node.
    cfg.replaceEdgeTarget (init_node->getOutEdges()[0], epilogue_header, true);
    cfg.replaceEdgeTarget (init_node->getOutEdges()[1], exit_node, true);
    ((BranchInst*)init_node->getLastInst())->replaceTargetLabel
      (true_target_label);

    // FIXME: We should also adjust phi nodes of init_node since it
    // has only one predecessor, but not doing it won't cause wrong
    // code with the current strange wrong SSA form in jitrino.

    InstFactory &inst_factory = ir_manager.getInstFactory ();
    DdgNodePairSet tested_ddg_node_pairs;
    Edge *cur_preheader_edge = preheader_edge;
    LabelInst *init_node_label = (LabelInst*)init_node->getLabelInst ();

    // Create alias testing blocks.
    for (unsigned i = 0; i < edges_to_be_runtime_tested.size (); i++)
      {
        DdgEdge *ddg_edge = edges_to_be_runtime_tested[i];
        DdgNode *node1 = ddg_edge->src;
        DdgNode *node2 = ddg_edge->dst;

        if (node1->id > node2->id)
          std::swap (node1, node2);

        if (!tested_ddg_node_pairs.insert (DdgNodePair (node1, node2)).second)
          // The DDG node pair has been tested, continue.
          continue;

        // Create the new preheader for the loop.
        LabelInst *new_preheader_label = inst_factory.makeLabel ();
        Node *new_preheader = cfg.createBlockNode (new_preheader_label);

        // Redirect the current preheader edge that goes into the loop
        // to the new preheader.
        cfg.replaceEdgeTarget (cur_preheader_edge, new_preheader, true);

        // Add edge from the new preheader to the initialization node
        // going to the preheader of the epilogue.
        cfg.addEdge (new_preheader, init_node);

        // Add edge from the new preheader to the header of the loop.
        cur_preheader_edge = cfg.addEdge (new_preheader, header_node);

        // If alias exists, jump to the initialization node to execute
        // the unvectorized loop.
        BranchInst *test_inst = (BranchInst*)inst_factory.makeBranch
          (Cmp_EQ, node1->base->getType()->tag,
           node1->base, node2->base, init_node_label);

        new_preheader->appendInst (test_inst);
      }
  }

  // Return the number of slots for vector versions of TYPE_TAG.

  int
  num_of_vector_slot (Type::Tag type_tag)
  {
    int vf = vf_of_type (type_tag);
    return vf ? common_vf / vf : 0;
  }

  // Set MAPPED_VAR to the SLOT-th slot of variable vector of VAR.
  // The first [0, COMMON_VF - 1] slots are for scalar versions of VAR
  // corresponding to each iteration.  The COMMON_VF-th and following
  // slots are for the vector versions of VAR.

  void
  set_mapped_var (SsaOpnd *var, SsaOpnd *mapped_var, int slot)
  {
    SsaOpndVec *var_vec = var_to_var_vec.lookup (var);

    if (!var_vec)
      {
        var_vec = new (memory) SsaOpndVec
          (common_vf + num_of_vector_slot (var->getType()->tag));
        var_to_var_vec.insert (var, var_vec);
      }

    assert ((slot >= common_vf) == mapped_var->getType()->isVector ()
            && slot < (int)var_vec->size ());

    (*var_vec)[slot] = mapped_var;
  }

  // Create and return a new operand from scalar operand OPND.  The
  // new operand is pushed to the mappped-to vector of OPND.  If
  // SLOG >= COMMON_VF, create the vector version, otherwise create a
  // scalar version with the same type of OPND.

  SsaOpnd*
  create_opnd_from (Opnd *opnd, int slot, bool force_tmp = false)
  {
    bool vector_p = slot >= common_vf;
    TypeManager &type_manager = ir_manager.getTypeManager ();
    OpndManager &opnd_manager = ir_manager.getOpndManager ();
    Type *opnd_type = opnd->getType ();
    Type *new_type;
    SsaOpnd *new_opnd;

    if (vector_p)
      new_type = type_manager.getVectorType (opnd_type->asNamedType (),
                                             vf_of_type (opnd_type->tag));
    else
      new_type = opnd_type;

    if (opnd->isSsaTmpOpnd () || force_tmp)
      new_opnd = opnd_manager.createSsaTmpOpnd (new_type);
    else if (opnd->isSsaVarOpnd ())
      {
        // If creating a new operand with the same type, reuse the
        // same VarOpnd.  This is necessary rather than just for
        // saving memory.  If we use different variables to create the
        // SSA variable, the rest passes will generate wrong code
        // without reporting any error.  It's very dangerous.
        VarOpnd *var = (vector_p
                        ? opnd_manager.createVarOpnd (new_type, false)
                        : opnd->asSsaVarOpnd()->getVar ());

        new_opnd = opnd_manager.createSsaVarOpnd (var);
      }
    else
      assert (0);

    set_mapped_var (opnd->asSsaOpnd (), new_opnd, slot);

    return new_opnd;
  }

  // Return true iff VAR is defined outside the loop.

  bool
  is_defined_outside_loop (Opnd *var)
  {
    return !data_dependence.ddg_node_of_inst (var->getInst ());
  }

  // Return true iff VAR is loop-invariant.

  bool
  is_loop_invariant (Opnd *var)
  {
    DdgNode *node = data_dependence.ddg_node_of_inst (var->getInst ());

    if (!node)
      // It's defined outside the loop, so it's loop-invariant.
      return true;

    Action action = action_for_node[node->id];

    // This test is valid only if the action of the node has been set.
    assert (action != act_unknown);

    // An instruction inside the loop is invariant iff it doesn't need
    // to be vectorized or unrolled.
    return action == act_nothing;
  }

  // Return the mapped variable of the SLOT-th element of the var
  // vector.  It may generates codes to pack or extract values from
  // the existing variables if the their types didn't match.

  SsaOpnd*
  get_mapped_var (SsaOpnd *var, int slot)
  {
    InstFactory &inst_factory = ir_manager.getInstFactory ();
    TypeManager &type_manager = ir_manager.getTypeManager ();
    OpndManager &opnd_manager = ir_manager.getOpndManager ();

    // Use get_array_addr for AddScaledIndex instructions.
    if (var->getInst()->getOpcode () == Op_AddScaledIndex)
      {
        // Not support vectors of addresses.
        assert (slot < common_vf);
        return get_array_addr (var, slot);
      }

    if (slot < common_vf)
      // Use the scalar value of the SLOT-th iteration of VAR.
      {
        if (is_loop_invariant (var))
          // Return VAR directly since it's invariant in the loop nest.
          return var;

        SsaOpndVec *var_vec = var_to_var_vec.lookup (var);

        assert (var_vec);

        if ((*var_vec)[slot])
          // The corresponding scalar version exists, return it.
          return (*var_vec)[slot];
        else
          {
            // The vectorization factor of the type of VAR.
            int vf = vf_of_type (var->getType()->tag);
            // The slot of the vector version containing the SLOT-th
            // scalar version of VAR.
            int vec_slot = common_vf + slot / vf;
            // The offset in the vector version of the needed scalar.
            int offset = slot % vf;

            // If there is not the scalar version yet, the vector
            // version must exit.
            assert (vec_slot < (int)var_vec->size () && (*var_vec)[vec_slot]);

            SsaOpnd *vec_tmp = (*var_vec)[vec_slot];

            if (SsaVarOpnd *vec_var = vec_tmp->asSsaVarOpnd ())
              // In Jitrino, VarOpnd must be loaded into a temp
              // variable to be used.  If not, failure may occur in
              // CodeSelect.cpp and not easy to find the root reason.
              // This makes implementation of optimization very
              // inconvenient and error-prone.  It also makes the
              // generated IR contain many unnecessary ldvar and
              // stvar's, which are essentially copy instructions.
              {
                vec_tmp = opnd_manager.createSsaTmpOpnd (vec_var->getType ());
                latch_node->appendInst (inst_factory.makeLdVar (vec_tmp, vec_var));
              }

            SsaOpnd *slot_tmp = opnd_manager.createSsaTmpOpnd
              (type_manager.getInt32Type ());
            SsaOpnd *extracted = create_opnd_from (var, slot, true);

            latch_node->appendInst (inst_factory.makeLdConst (slot_tmp, offset));
            latch_node->appendInst (inst_factory.makeVecExtract
                                    (extracted, vec_tmp, slot_tmp));

            return extracted;
          }
      }
    else
      // Use the vector values of several iterations of VAR.
      {
        SsaOpndVec *var_vec = var_to_var_vec.lookup (var);

        if (!var_vec)
          {
            // VAR must be loop invariant and can be duplicated into
            // a vector.
            assert (is_loop_invariant (var));

            SsaOpnd *vect_opnd = create_opnd_from (var, slot);
            Inst *conv_to_vect = inst_factory.makeConv
              ((Modifier (Overflow_None) | Modifier (Exception_Never)
                | Modifier (Strict_No)), Type::Vector, vect_opnd, var);

            latch_node->appendInst (conv_to_vect);

            // We must get here at the first time of using VAR.
            assert (slot == common_vf);

            // Fill all vector slots with the same vector version.
            for (int size = common_vf + num_of_vector_slot (var->getType()->tag);
                 ++slot < size;)
              set_mapped_var (var, vect_opnd, slot);

            return vect_opnd;
          }

        assert (slot < (int)var_vec->size ());

        if ((*var_vec)[slot])
          // Return the vector version if it exists.
          return (*var_vec)[slot];
        else
          // Pack corresponding scalar variables to a vector variable.
          {
            SsaOpnd *packed_vec = create_opnd_from (var, slot, true);
            int vf = vf_of_type (var->getType()->tag);
            Opnd **srcs = new (memory) Opnd*[vf];

            for (int begin = (slot - common_vf) * vf, i = begin, j = 0;
                 i < begin + vf; i++, j++)
              {
                srcs[j] = (*var_vec)[i];
                assert (srcs[j]);

                if (SsaVarOpnd *var_src = srcs[j]->asSsaVarOpnd ())
                  // Load it to SsaTmpVarOpnd if its an SsaVarOpnd (JitrinoRestriction)
                  {
                    SsaTmpOpnd *tmp = opnd_manager.createSsaTmpOpnd (var_src->getType ());
                    latch_node->appendInst (inst_factory.makeLdVar (tmp, var_src));
                    srcs[j] = tmp;
                  }
              }

            latch_node->appendInst
              (inst_factory.makeVecPackScalars (packed_vec, vf, srcs));

            return packed_vec;
          }
      }
  }

  // ADDR is a scaled array address, i.e. a result of addindex.  This
  // function returns the ITER-th value of ADDR.  The AddScaledIndex
  // instructions can be set as act_unroll, but the unrolling may
  // generate useless AddScaledIndex instructions.  Thus, we set it as
  // act_nothing and create them on demand here rather than unroll it
  // for all iterations and then use get_mapped_var to get it.

  SsaOpnd*
  get_array_addr (SsaOpnd *addr, int iter)
  {
    if (is_loop_invariant (addr))
      // Return ADDR directly since it's invariant in the loop nest.
      return addr;

    if (iter == 0)
      // Reuse the original result for the first iteration.
      {
        set_mapped_var (addr, addr, 0);
        return addr;
      }

    SsaOpndVec *var_vec = var_to_var_vec.lookup (addr);

    assert (var_vec && iter < (int)var_vec->size ());

    if ((*var_vec)[iter])
      // The address of the ITER-th iteration has been computed, so
      // reuse it.
      return (*var_vec)[iter];

    // Generate the addindex for the ITER-th iteration.
    Inst *inst = addr->getInst ();
    SsaOpnd *new_addr = create_opnd_from (inst->getDst (), iter);
    InstFactory &inst_factory = ir_manager.getInstFactory ();
    Inst * new_inst = inst_factory.makeAddScaledIndex
      (new_addr, inst->getSrc (0),
       get_mapped_var (inst->getSrc(1)->asSsaOpnd (), iter));

    latch_node->appendInst (new_inst);

    return new_addr;
  }

  // Transform the scalar instruction INST to the instruction that
  // performs the computation of iterations [ITER, ITER + NUM) in
  // parallel and return the new instruction.

  Inst*
  transform_one_instruction (Inst *inst, int iter, int num)
  {
    assert (iter % num == 0);

    InstFactory &inst_factory = ir_manager.getInstFactory ();
    TypeManager &type_manager = ir_manager.getTypeManager ();
    OpndManager &opnd_manager = ir_manager.getOpndManager ();
    int slot = num > 1 ? (common_vf + iter / num) : iter;
    Type::Tag type_tag = num > 1 ? Type::Vector : inst->getType ();
    Opcode opcode = inst->getOpcode ();

    // Process special cases first.
    switch (opcode)
      {
      case Op_Shl:
      case Op_Shr:
        return inst_factory.makeInst (opcode, inst->getModifier (),
                                      type_tag,
                                      create_opnd_from (inst->getDst (), slot),
                                      get_mapped_var (inst->getSrc(0)->asSsaOpnd (),
                                                      slot),
                                      // Always use the scalar version
                                      // of the shift amount value.
                                      get_mapped_var (inst->getSrc(1)->asSsaOpnd (),
                                                      iter));

      case Op_Conv:
      case Op_ConvZE:
        {
          if (num == 1)
            // It's a scalar conversion, leave it to the general code.
            break;

          SsaOpnd *src = inst->getSrc(0)->asSsaOpnd ();
          SsaOpnd *dst = inst->getDst()->asSsaOpnd ();
          int src_vf = vf_of_type (src->getType()->tag);
          int dst_vf = vf_of_type (dst->getType()->tag);

          if (src_vf == dst_vf)
            // Verctorization factors of SRC and DST are same, leave
            // it to the general code.
            break;

          // TODO: Not support vectorizing conversions from larger
          // size to smaller size for now.
          assert (src_vf > dst_vf);

          int src_slot = common_vf + iter / src_vf;
          SsaOpnd *iter_tmp = opnd_manager.createSsaTmpOpnd
            (type_manager.getInt32Type ());
          Inst *iter_inst = inst_factory.makeLdConst
            (iter_tmp, iter % src_vf);
          SsaOpnd *new_dst = create_opnd_from (dst, slot);
          Inst *new_inst = inst_factory.makeVecExtract
            (new_dst, get_mapped_var (src, src_slot), iter_tmp);

          new_inst->insertAfter (iter_inst);

          // Return the first instruction of the instruction list.
          return iter_inst;
        }

      case Op_TauLdInd:
        return inst_factory.makeTauLdInd (inst->getModifier (), type_tag,
                                          create_opnd_from (inst->getDst (), slot),
                                          get_array_addr (inst->getSrc(0)->asSsaOpnd (),
                                                          iter),
                                          inst->getSrc (1), inst->getSrc (2));

      case Op_TauStInd:
        return inst_factory.makeTauStInd (inst->getModifier (), type_tag,
                                          get_mapped_var (inst->getSrc(0)->asSsaOpnd (),
                                                          slot),
                                          get_array_addr (inst->getSrc(1)->asSsaOpnd (),
                                                          iter),
                                          inst->getSrc (2), inst->getSrc (3), inst->getSrc (4));

      case Op_LdVar:
        // The ldvar and stvar are of class VarAccessInst rather than
        // Inst, so we can only make them with the specific functions
        // rather than the general makeInst.  Otherwise, later phases
        // (rather than this) may fail.  I think this is not a good
        // idea to design the IR in this way.  It's error-prone.  The
        // type conversion for the arguments is also inconvenient.
        {
          SsaOpnd *dst = create_opnd_from (inst->getDst (), slot);
          SsaOpnd *opnd = get_mapped_var (inst->getSrc(0)->asSsaOpnd (), slot);
          if (SsaVarOpnd *var_opnd = opnd->asSsaVarOpnd ())
            return inst_factory.makeLdVar (dst, var_opnd);
          else
            {
              // opnd must be the result of a vector extraction or
              // packing or ldvar.
              assert (opnd->getInst()->getOpcode () == Op_VecExtract
                      || opnd->getInst()->getOpcode () == Op_VecPackScalars
                      || opnd->getInst()->getOpcode () == Op_LdVar);
              return inst_factory.makeCopy (dst, opnd);
            }
        }

      case Op_StVar:
        return inst_factory.makeStVar (create_opnd_from (inst->getDst (), slot)->asSsaVarOpnd (),
                                       get_mapped_var (inst->getSrc(0)->asSsaOpnd (), slot));

      case Op_Shladd:
        return inst_factory.makeShladd (create_opnd_from (inst->getDst (), slot),
                                        get_mapped_var (inst->getSrc(0)->asSsaOpnd (),
                                                        slot),
                                        get_mapped_var (inst->getSrc(1)->asSsaOpnd (),
                                                        slot),
                                        get_mapped_var (inst->getSrc(2)->asSsaOpnd (),
                                                        slot));

      case Op_TauDiv:
        return inst_factory.makeTauDiv (inst->getModifier (),
                                        create_opnd_from (inst->getDst (), slot),
                                        get_mapped_var (inst->getSrc(0)->asSsaOpnd (),
                                                        slot),
                                        get_mapped_var (inst->getSrc(1)->asSsaOpnd (),
                                                        slot),
                                        inst->getSrc (2));
      default:
        ;
      }

    // Process general cases.
    switch (inst->getNumSrcOperands ())
      {
      case 0:
        return inst_factory.makeInst (opcode, inst->getModifier (),
                                      type_tag,
                                      create_opnd_from (inst->getDst (), slot));
      case 1:
        return inst_factory.makeInst (opcode, inst->getModifier (),
                                      type_tag,
                                      create_opnd_from (inst->getDst (), slot),
                                      get_mapped_var (inst->getSrc(0)->asSsaOpnd (),
                                                      slot));
      case 2:
        return inst_factory.makeInst (opcode, inst->getModifier (),
                                      type_tag,
                                      create_opnd_from (inst->getDst (), slot),
                                      get_mapped_var (inst->getSrc(0)->asSsaOpnd (),
                                                      slot),
                                      get_mapped_var (inst->getSrc(1)->asSsaOpnd (),
                                                      slot));
      }

    assert (0);
    return NULL;
  }

  void
  unroll_ddg_scc (DdgNodes::iterator &iter)
  {
    InstFactory &inst_factory = ir_manager.getInstFactory ();
    OpndManager &opnd_manager = ir_manager.getOpndManager ();
    DdgNodes::iterator it;

    // Unroll the nodes common_vf times.
    for (int i = 0; i < common_vf; i++)
      {
        it = iter;

        do
          {
            DdgNode *node = *it;
            Inst *inst = node->inst;

            if (inst->getOpcode () == Op_Phi)
              // Map dst of phi node to the value of the i-th
              // iteration.
              {
                assert (inst->getNumSrcOperands () == 2);

                SsaOpnd *dst = inst->getDst()->asSsaOpnd ();
                SsaOpnd *mapped_dst = dst;

                if (i > 0)
                  {
                    SsaOpnd *src_from_loop = inst->getSrc
                      ((is_defined_outside_loop (inst->getSrc (0))
                        ? 1 : 0))->asSsaOpnd ();

                    mapped_dst = get_mapped_var (src_from_loop, i - 1);
                  }
                else
                  {
                    // Load it to a temp variable now to avoid being
                    // overwritten by later stvar to this variable.
                    // (JitrinoRestriction)
                    mapped_dst = opnd_manager.createSsaTmpOpnd
                      (mapped_dst->getType ());
                    latch_node->appendInst
                      (inst_factory.makeLdVar (mapped_dst, dst->asSsaVarOpnd ()));
                  }

                set_mapped_var (dst, mapped_dst, i);
              }
            else
              {
                if (node->kind == DdgNode::ddg_scalar_with_scev && i == 0)
                  // Leave the instructions of induction variables at
                  // original places and just save the mapping.  We
                  // don't move them to the end of the latch node
                  // because the loop-exit condition may use them.
                  {
                    SsaOpnd *dst = inst->getDst()->asSsaOpnd ();
                    set_mapped_var (dst, dst, i);
                  }
                else
                  {
                    Inst *new_inst = transform_one_instruction (inst, i, 1);
                    latch_node->appendInst (new_inst);
                  }
              }
          }while (*++it);
      }

    it = iter;

    // Adjust source operands of phi nodes in the SCC and remove array
    // store instructions.
    do
      {
        DdgNode *node = *it;
        Inst *inst = node->inst;

        if (inst->getOpcode () == Op_Phi)
          {
            int src_idx = (is_defined_outside_loop (inst->getSrc (0))
                           ? 1 : 0);
            SsaOpnd *src_from_loop = inst->getSrc(src_idx)->asSsaOpnd ();
            SsaOpnd *new_src = get_mapped_var (src_from_loop, common_vf - 1);

            if (new_src->isSsaTmpOpnd ())
              {
                // new_src must be the result of a vector extraction.
                assert (new_src->getInst()->getOpcode () == Op_VecExtract
                        || new_src->getInst()->getOpcode () == Op_VecPackScalars);

                SsaVarOpnd *var = opnd_manager.createSsaVarOpnd
                  (src_from_loop->asSsaVarOpnd()->getVar ());

                latch_node->appendInst (inst_factory.makeStVar (var, new_src));
                new_src = var;
              }

            inst->setSrc (src_idx, new_src);
          }
        else if (node->kind != DdgNode::ddg_scalar_with_scev)
          // Remove the original duplicated instructions.
          // FIXME: We should check whether their results are used
          // outside the loop.
          inst->unlink ();
      }while (*++it);

    iter = it;
  }

  void
  process_ddg_scc (DdgNodes::iterator &iter)
  {
    DdgNode *node = *iter;
    Inst *inst = node->inst;
    Inst *vect_inst;
    Action action = action_for_node[(*iter)->id];

    switch (action)
      {
      case act_nothing:
      case act_on_demand:
        iter++;
        break;

      case act_unroll:
        unroll_ddg_scc (iter);
        break;

      default:
        // ACTION is the VF of NODE
        assert (action >= act_min_vf);

        for (int i = 0; i < common_vf; i += action)
          {
            vect_inst = transform_one_instruction (inst, i, action);
            latch_node->appendInst (vect_inst);
            // Remove the original instruction.
            // FIXME: We should check whether their results are used
            // outside the loop.
            inst->unlink ();
          }

        iter++;
      }

    assert (*iter == NULL);
  }

  // Transform all vectorizable instructions of loop in LOOP_INFO into
  // vector form.

  void
  transform_instructions ()
  {
    for (DdgNodes::iterator i = ddg_sccs.begin (); i != ddg_sccs.end (); i++)
      {
        i++;

        // Process all SCCs in this DFS tree.
        do
          process_ddg_scc (i);
        while (*++i);
      }
  }

  // Adjust the check instruction of the loop so that it won't run
  // over the original boundary due to the vectorization.

  void
  adjust_old_check_inst ()
  {
    LoopNode *loop = loop_info->get_loop ();
    BranchInst *cond_inst = loop_info->get_exit_branch_inst ();

    // TODO: We don't support Cmp_Zero and Cmp_NonZero for now though
    // number_of_iterations has supported them.
    assert (cond_inst->getNumSrcOperands () == 2);

    SsaOpnd *op0 = cond_inst->getSrc(0)->asSsaOpnd ();
    SsaOpnd *op1 = cond_inst->getSrc(1)->asSsaOpnd ();
    Expr *scev0 = scev_analyzer.analyze_scev (op0, loop);
    Expr *scev1 = scev_analyzer.analyze_scev (op1, loop);

    // The index of the boundary operand in COND_INST and the chrec
    // of the induction variable.
    int boundary_index;
    SsaOpnd *boundary_op;
    PolyChrec *chrec;

    if ((chrec = scev0->as_poly_chrec ()))
      {
        boundary_index = 1;
        boundary_op = op1;
      }
    else
      {
        chrec = scev1->as_poly_chrec ();
        assert (chrec);
        boundary_index = 0;
        boundary_op = op0;
      }

    // Generate instructions to compute:
    // boundary_op - (number_of_iterations * chrec->right) % (chrec->right * common_vf).

    Type *type = boundary_op->getType ();

    // scaled_iter_num = number_of_iterations * chrec->right
    Expr *scaled_iter_num = scev_analyzer.fold_build
      (Op_Mul, loop_info->get_iteration_number (), chrec->right);

    // stride_mul_vf = chrec->right * common_vf
    Expr *stride_mul_vf = scev_analyzer.fold_build
      (Op_Mul, chrec->right, new (memory) Integer (type, common_vf));

    // remainder = scaled_iter_num % stride_mul_vf
    Expr *remainder = scev_analyzer.fold_build
      (Op_TauRem, scaled_iter_num, stride_mul_vf);

    // adjusted = boundary_op - remainder
    Expr *adjusted = scev_analyzer.fold_build
      (Op_Sub, new (memory) Variable (type, boundary_op), remainder);

    SsaOpnd *adjusted_tmp = adjusted->gen_insts_before (cond_inst, ir_manager);

    // Update the boundary operand of cond_inst.
    cond_inst->setSrc (boundary_index, adjusted_tmp);
  }

  // Perform the vectorization transformation.
  // before:
  //  old_loop
  //    {
  //      A
  //      check (idxOpnd,limitOpnd)
  //      B
  //    }
  // after:
  //  unrolledIncOpnd = unrollCount * idx->increment
  //  unrolledLimitOpnd = limitOpnd-unrolledIncOpnd;
  //  runtime alias testing
  //  bodyA // FIXME: This has not been done.
  //  vectorized_loop
  //    {
  //      vectorized_A
  //      check(idxOpnd,unrolledLimitOpnd)
  //      vectorized_B
  //    }
  //  copy operands defined in A and used in B
  //  check (idxOpnd,limitOpnd)
  //  epilogue_loop
  //    {
  //      A
  //      check (idxOpnd,limitOpnd)
  //      B
  //    }

  void
  transform_single_loop ()
  {
    Edge *exit_edge = loop_info->get_single_exit_edge ();
    Edge *continue_edge = loop_info->get_continue_edge ();
    Node *exit_node = exit_edge->getTargetNode ();
    Node *continue_node = continue_edge->getTargetNode ();
    ControlFlowGraph &cfg = ir_manager.getFlowGraph ();
    DefUseBuilder def_uses (memory);

    def_uses.initialize (cfg);

    // Duplicate a new loop from the old loop.
    Node *epilogue_header = FlowGraph::duplicateRegion (ir_manager,
                                                        continue_node,
                                                        nodes_in_loop,
                                                        def_uses);

    // Create the preheader for the new loop.
    InstFactory &inst_factory = ir_manager.getInstFactory ();
    LabelInst *epilogue_preheader_label = inst_factory.makeLabel ();
    Node *epilogue_preheader = cfg.createBlockNode (epilogue_preheader_label);

    // Add edge from the preheader of the new loop to the exit node.
    cfg.addEdge (epilogue_preheader, exit_node);

    // Redirect the exit edge of the old loop to the preheader of the
    // new loop.
    cfg.replaceEdgeTarget (exit_edge, epilogue_preheader, true);

    // Add edge from the preheader to the header of the new loop.
    cfg.addEdge (epilogue_preheader, epilogue_header);

    BranchInst *old_check_inst = loop_info->get_exit_branch_inst ();
    LabelInst *true_target_label =
      (LabelInst*)(old_check_inst->getTargetLabel () == epilogue_preheader_label
                   ? exit_node : epilogue_header)->getLabelInst ();
    BranchInst *new_check_inst = (BranchInst*)inst_factory.makeBranch
      (old_check_inst->getComparisonModifier (),
       old_check_inst->getType (),
       old_check_inst->getSrc (0),
       old_check_inst->getSrc (1),
       true_target_label);

    epilogue_preheader->appendInst (new_check_inst);

    generate_runtime_alias_tests (epilogue_header, exit_node, true_target_label);

    transform_instructions ();

    adjust_old_check_inst ();

    // TODO: We should find the SSA variables that need to be put into
    // SSA form here and set all occurrences of them to their base
    // variables and call fixupvars pass to fix them up, rather than
    // dessa the whole program and ssa it again as done currently.
  }
};

// The automatic vectorization transformation.

class AutovectTransformation
{
public:
  AutovectTransformation (IRManager &irm)
    : ir_manager (irm),
      scev_analyzer (irm.getLoopTree ()),
      cur_loop_to_be_vectorized (NULL)
  {
  }

  void
  run ()
  {
    LoopTree *loop_tree = ir_manager.getLoopTree ();
    LoopNode *root = (LoopNode*)loop_tree->getRoot ();

    for (LoopNode *child = root->getChild (); child; child = child->getSiblings ())
      find_loops_to_be_vectorized (child);

    if (cur_loop_to_be_vectorized)
      {
        delete cur_loop_to_be_vectorized;
        cur_loop_to_be_vectorized = NULL;
      }

    unsigned num_to_be_vectorized = loops_to_be_vectorized.size ();

    if (Log::isEnabled () && num_to_be_vectorized)
      {
        ir_manager.getMethodDesc().printFullName (Log::out ());
        Log::out() << ": " << num_to_be_vectorized
                   << " loop(s) to be vectorized"
                   << std::endl;
      }

    for (unsigned i = 0; i < num_to_be_vectorized; i++)
      {
        loops_to_be_vectorized[i]->transform ();
        delete loops_to_be_vectorized[i];
      }

    loops_to_be_vectorized.clear ();
  }

private:
  IRManager &ir_manager;

  ScalarEvolution scev_analyzer;

  SingleLoopVectorizer *cur_loop_to_be_vectorized;

  std::vector<SingleLoopVectorizer*> loops_to_be_vectorized;

private:

  // Recursively find loops that should be vectorized.

  void
  find_loops_to_be_vectorized (LoopNode *loop)
  {
    LoopNode *child = loop->getChild ();

    if (!child)
      // It's a leaf loop node, check whether it should be vectorized.
      {
        if (!cur_loop_to_be_vectorized)
          cur_loop_to_be_vectorized = new SingleLoopVectorizer (ir_manager, scev_analyzer);

        if (cur_loop_to_be_vectorized->should_vectorize (loop))
          {
            loops_to_be_vectorized.push_back (cur_loop_to_be_vectorized);
            cur_loop_to_be_vectorized = NULL;
          }
      }
    else
      // Otherwise, walk into children nodes.
      for (; child; child = child->getSiblings ())
        find_loops_to_be_vectorized (child);
  }
};


DEFINE_SESSION_ACTION (AutovectPass, autovect, "Automotic vectorization")

void
AutovectPass::_run (IRManager &irm)
{
#if 0
  // LU:factor, SOR:execute, RSA:monReduction
  if (std::string (irm.getMethodDesc().getName ()) != "execute")
    return;
#endif

  AutovectTransformation autovect_transformation (irm);

  computeLoops (irm);
  autovect_transformation.run ();
}

}
