/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#ifndef _SCALAR_EVOLUTION_H_
#define _SCALAR_EVOLUTION_H_

#include "loop-info.h"

namespace Jitrino {

class ScalarEvolution
{
public:
  ScalarEvolution (LoopTree *t)
    : memory ("ScalarEvolution"), loop_tree (t), cached_scev (memory, 97)
  {
    int_zero = new (memory) Integer (NULL, 0);
    int_one = new (memory) Integer (NULL, 1);
  }

  // Return the evolution function of VAR with respect to LOOP, which
  // can be represented by the polynomial chain of recurrences (chrec).
  // At all program points that exactly belong to LOOP (not to sub-loops),
  // and where VAR is available, the value of VAR always equals to the
  // result of applying the evolution function on the iteration number
  // of LOOP, say i.  The evolution function can only contain variables
  // defined outside LOOP, because a variable defined in LOOP can either
  // be represented by a polynomial chrec that don't contain variables
  // defined in LOOP, or be a function of i that cannot be represented
  // by a polynomial chrec.  The variables defined outside LOOP are
  // parameters of the evolution function and can be instantiated.
  Expr* analyze_scev (SsaOpnd *var, LoopNode *loop);

  // Compute and return the number of iterations of LOOP_INFO.  Return
  // NULL if it cannot be computed (e.g. it has multiple latches).
  Expr* number_of_iterations (LoopInfo &loop_info);

  // Instantiate parameters of SCEV used in USE_LOOP with respect to
  // WRTO_LOOP.  Returned function is an evolution function of iteration
  // numbers of loops in [WRTO_LOOP, USE_LOOP].
  Expr* instantiate_scev (Expr *scev, LoopNode *use_loop, LoopNode *wrto_loop);

  Integer* get_int_zero () const { return int_zero; }

  // Fold the expression (E1 OPCODE E2).  Return the folded expression
  // if successful, otherwise return NULL.
  Expr* fold (Opcode opcode, Expr *e1, Expr *e2);

  // Build the folded expression (E1 OPCODE E2).
  Expr* fold_build (Opcode opcode, Expr *e1, Expr *e2);

  // Build the expression (E1 OPCODE E2).
  Expr* build (Opcode opcode, Expr *e1, Expr *e2);

  // Return the GCD of two integer.
  static int number_gcd (int a, int b);

  // Return the GCD of two terms.
  Expr* term_gcd (Expr *t1, Expr *t2);

  // Return false only when DIVIDEND is a polynomial and DIVISOR is a
  // single term, and we are sure that DIVIDEND cannot be exactly
  // divided by DIVISOR.  For all other cases, simply return true.
  bool may_divided_by_p (Expr *dividend, Expr *divisor);

  // For debugging:

  void debug_scev_for_all (LoopNode *loop);

private:
  // A pair of <var, loop>.
  class VarLoop
  {
  public:
    VarLoop (SsaOpnd *v, LoopNode *l) : var (v), loop (l) {}

  public:
    SsaOpnd *var;
    LoopNode *loop;
  };

  // The hash table for mapping pairs of <var, loop> to expressions.
  class MapVarLoopToExpr : public HashTable<VarLoop, Expr>
  {
  public:
    MapVarLoopToExpr (MemoryManager &m, U_32 s)
      : HashTable<VarLoop, Expr> (m, s) {}

  protected:
    virtual bool keyEquals (VarLoop *key1, VarLoop *key2) const
    {
      return key1->var == key2->var && key1->loop == key2->loop;
    }

    virtual U_32 getKeyHashCode (VarLoop *key) const
    {
      return (U_32)(((long)key->var ^ (long)key->loop) >> 2);
    }
  };

private:
  Expr* analyze_scev_1 (SsaOpnd *, LoopNode *);
  bool loop_phi_node_p (Inst *);
  Expr* analyze_scev_for_loop_phi (Inst *, LoopNode *);
  Expr* compute_step (SsaOpnd *, Inst *, LoopNode *);

  static bool is_mul_inst (Inst *);

  Expr* fold_polynomial (Opcode, Expr *, Expr *);

  static bool same_opnd_p (Expr *, Expr *);
  Expr* term_add (Opcode, Expr *, Expr *);

  static bool factor_precede_p (Expr *, Expr *);
  Expr* term_mul (Expr *, Expr *);
  Expr* term_div (Expr *, Expr *);

  bool term_may_divided_by_p (Expr *, Expr *);

  Expr* fold_build_poly_chrec (Type *, Expr *, Expr *, LoopNode *);

private:
  MemoryManager memory;

  LoopTree *loop_tree;

  // Hash set of <KEY:(var, loop), VALUE:evolution_function> pairs for
  // caching analyzed results.
  MapVarLoopToExpr cached_scev;

  Integer *int_zero;
  Integer *int_one;
};

}

#endif
