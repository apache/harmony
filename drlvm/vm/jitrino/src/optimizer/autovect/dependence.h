/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#ifndef _DEPENDENCE_H_
#define _DEPENDENCE_H_

#include "scalar-evolution.h"

namespace Jitrino {

class DdgNode;
class DdgEdge;

// Vector of DdgNode.
typedef std::vector<DdgNode*> DdgNodes;
// Vector of DdgEdge 
typedef std::vector<DdgEdge*> DdgEdges;

// Node of a data dependence graph (DDG).  In a DDG, each node may
// represent a single load or store array access statement or a pure
// (without side effects) scalar statement or a phi node.  Each edge
// from node n1 to n2 denotes that n2 depends on n1 either due to a
// true scalar data dependence or because there is (or may be, from
// static analysis view) a runtime CFG path from n1 to n2 on which n1
// and n2 access the same memory location and at least one of them
// writes into it.  In general, one statement may access more than one
// data reference, but in current (simplified) IR, each instruction
// accesses at most one data reference.

class DdgNode
{
public:
  enum NodeKind
    {
      // Scalar statements with scalar evolution function, including
      // induction variables and loop-invariant variables in a loop
      // nest.  We don't need to analyze data dependence relations
      // from them to others and the transformer should process them
      // differently comparing to the following two kinds.
      ddg_scalar_with_scev,

      // Scalar statements other than the above kind.
      ddg_scalar_without_scev,

      // Memory access statements.
      ddg_memory
    };

public:
  DdgNode (NodeKind k, int d, Inst *i, SsaOpnd *b, SsaOpnd *x, Expr *f, bool r)
    : kind (k), id (d), inst (i), base (b), index (x), scev_fn (f), is_read (r)
  {
  }

  // Return true iff this->inst is a store instruction.

  bool is_store () const { return kind == ddg_memory && !is_read; }

public:
  const NodeKind kind;

  // The index of this node in the DdgNode array.
  int id;

  // Edges into and out of this node.
  DdgEdges preds;
  DdgEdges succs;

  // The instruction that accesses a data reference (array).
  Inst *inst;

  // The following are for array access statements.  If and only if
  // BASE == NULL, this node represents a pure scalar statments.

  // The base part of the array access address.  An array access
  // address is the operand of ldind or stind instruction, which can
  // be decomposed into BASE + INDEX form.  The BASE part is the base
  // address of an array object, and the INDEX part is an integer
  // expression.
  SsaOpnd *base;

  // The index part of the array access address if type == ddg_memory,
  // or the destination operand of INST if type == ddg_scalar_with_scev.
  SsaOpnd *index;

  // Evolution function of INDEX.  Now we only support one dimension
  // since jave only support one.
  Expr *scev_fn;

  // True when INST read this data reference. 
  bool is_read;
};

// Edge of a data dependence graph.  Empty direction vector denotes
// a dependence connected by a scalar SSA variable.

class DdgEdge
{
public:
  // Data dependence direction of direction vectors.  The dependence
  // analysis must guarantee that we can safely ignore all meaningless
  // direction vectors represented by a compound form vector, i.e.
  // (*, *) equals (<, *) and (=, <=) (ignoring (>, *) and (=, >)).
  // When attaching a direction vector whose first non-'=' element is
  // '*' to an DDG edge, the analysis algorithm must also create a
  // corresponding reversed DDG edge to represent the ignored
  // direction vectors of that edge.
  enum Direction
    {
      dir_lt,   // <
      dir_eq,   // =
      dir_gt,   // >
      dir_le,   // <=
      dir_lg,   // <>
      dir_ge,   // >=
      dir_all,  // <=> (*)
    };

  // Vector of direction vectors.
  typedef std::vector<Direction*> DirectionVectors;

public:
  DdgEdge (DdgNode *s, DdgNode *d, bool cr = false)
    : src (s), dst (d), computation_relevant (cr),
      runtime_alias_test (false)
  {
    s->succs.push_back (this);
    d->preds.push_back (this);
  }

  ~DdgEdge ()
  {
    // Release the direction vectors owned by this edge.
    while (!dir_vects.empty ())
      {
        delete[] dir_vects.back ();
        dir_vects.pop_back ();
      }
  }

public:
  // Source and destination of this edge.
  DdgNode *src;
  DdgNode *dst;

  // Direction vectors of this edge.
  DirectionVectors dir_vects;

  // Denoting whether this DDG edge is due to a variable used for
  // computation.  Those variables include operands of computation
  // instructions and source value of store instructions.
  bool computation_relevant;

  // The flag denoting that the alias relation of the two nodes
  // connected by this edge should be tested at runtime.
  bool runtime_alias_test;
};

// The data dependence analysis class.

class DataDependence
{
public:
  DataDependence (LoopTree *t, ScalarEvolution &s)
    : memory ("DataDependence"), loop_tree (t), scev_analyzer (s),
      inst_to_ddg_node (memory, 103), failed_reason (NULL)
  {
  }

  // Find all data references in loop nest starting at LOOP and compute
  // dependences among them.  If successful, return true and store the
  // corresponding results into DDG_NODES, otherwise return false.
  bool compute_ddg_for (LoopNode *loop);

  const LoopInfos& get_loop_nest () const { return loop_nest; }

  const DdgNodes& get_ddg_nodes () const { return ddg_nodes; }

  DdgNode* ddg_node_of_inst (Inst *inst)
  {
    return inst_to_ddg_node.lookup (inst);
  }

  // For debugging:

  void debug_data_dependences (LoopNode *);

private:
  // The strict weak ordering predicate for ordering nodes in reverse
  // post-num order, so that if n1 is above n2 in the CFG without back
  // edge, n1 must precede n2 when sorting.  This guarantees that if
  // there is a CFG path from n1 to n2 without passing though any back
  // edge, n1 must preceed n2.  If all back edges are covered by loops,
  // i.e. if passing through a back edge, some iteration number must
  // change, then if n1 precees n2, there cannot be loop-independent
  // dependences from n2 to n1 since there is no paths from n2 to n1
  // without passing through any back edge (without changing any
  // iteration number).  With this order, we only need to test
  // loop-independent dependences for one direction.
  static bool node_post_num_greater (Node *n1, Node *n2)
  {
    return n1->getPostNum () > n2->getPostNum ();
  }

  // Coefficient array of a linear function corresponding to the
  // LOOP_NEST.  The constant part is in the first slot, i.e. for
  // a loop nest <i, j, k> and an access function 1 + 2i + 3j + 4k,
  // the coefficient array is {1, 2, 3, 4}.  The size of coefficient
  // array should be greater than the size of LOOP_NEST by one.
  typedef std::vector<Expr*> Coefficients;

  // The hash table for mapping instructions to DDG nodes.
  class MapInstToDdgNode : public HashTable<Inst, DdgNode>
  {
  public:
    MapInstToDdgNode (MemoryManager &m, U_32 s)
      : HashTable<Inst, DdgNode> (m, s) {}

  protected:
    virtual bool keyEquals (Inst *key1, Inst *key2) const
    {
      return key1 == key2;
    }

    virtual U_32 getKeyHashCode (Inst *key) const
    {
      return (U_32)((long)key >> 2);
    }
  };

private:
  const char* compute_ddg_for_1 (LoopNode *);

  bool find_loop_nest_1 (LoopNode *);
  bool find_loop_nest (LoopNode *);
  bool compute_iteration_numbers ();

  static bool scalar_statement_p (Opcode);
  Expr* compute_scev (SsaOpnd *, Inst *);
  void add_ddg_node (DdgNode::NodeKind, Inst *, SsaOpnd *, SsaOpnd *, Expr *, bool);
  bool is_loop_exit_node (Node *);
  const char* find_ddg_nodes (LoopNode *);

  bool loop_nest_invariant_base_p (SsaOpnd *);

  bool compute_data_dependence_between (DdgNode *, DdgNode *);

  void extract_coefficients_1 (Expr *, Coefficients &);
  void extract_coefficients (Expr *, Coefficients &);

  bool gcd_test (const Coefficients &, const Coefficients &);

  void test_iv_info (const Coefficients &, const Coefficients &,
                     int &, int &, bool &);

  DdgEdge::Direction* create_all_dir_vector ();
  static DdgEdge::Direction reverse_direction (DdgEdge::Direction);

  bool ziv_test (const Coefficients &, const Coefficients &,
                 DdgNode *, DdgNode *);
  bool strong_siv_test (const Coefficients &, const Coefficients &, int,
                        DdgNode *, DdgNode *);
  bool miv_test (const Coefficients &, const Coefficients &,
                 DdgNode *, DdgNode *);

  void dump_dir_vect (std::ostream &, const DdgEdge::Direction *);
  void dump_dir_vects (std::ostream &, DdgEdge *);
  void dump_ddg_node (std::ostream &, const DdgNode *);
  void dump_analysis_result (LoopNode *);

private:
  MemoryManager memory;

  LoopTree *loop_tree;

  ScalarEvolution &scev_analyzer;

  // The vector of the loop nest, from outmost to innermost.
  LoopInfos loop_nest;

  // Nodes of the resulting data dependence graph.
  DdgNodes ddg_nodes;

  // Map instructions in loop_nest to nodes in ddg_nodes.
  MapInstToDdgNode inst_to_ddg_node;

  // Failed reason of the analysis.
  const char *failed_reason;
};

}

#endif
