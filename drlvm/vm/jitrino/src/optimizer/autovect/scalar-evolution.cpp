/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#include <algorithm>
#include "scalar-evolution.h"

namespace Jitrino {

Expr*
ScalarEvolution::analyze_scev (SsaOpnd *var, LoopNode *loop)
{
  VarLoop var_loop (var, loop);
  Expr *result = cached_scev.lookup (&var_loop);

  if (!result)
    // The <VAR, LOOP> pair has not bee analyzed, do it now.
    {
      result = analyze_scev_1 (var, loop);
      cached_scev.insert (new (memory) VarLoop (var, loop), result);
    }

  return result;
}

// Helper function for analyze_scev

Expr*
ScalarEvolution::analyze_scev_1 (SsaOpnd *var, LoopNode *loop)
{
  if (!var->getType()->isInteger ())
    return NULL;

  var = strip_copies (var);

  Inst* inst = var->getInst ();
  Opcode opcode = inst->getOpcode ();

  // Return the constant in spite of whether it's defined in LOOP.
  if (opcode == Op_LdConstant)
    return new (memory) Integer (var->getType (), inst->asConstInst()->getValue().i4);

  // If VAR is defined outside LOOP and is not a result of multiply
  // instruction, return the symbolic form (parameter).  Allowing
  // expansion of multiply instructions may make GCD more accurate.
  if (!loop->inLoop (inst->getNode ()) && !is_mul_inst (inst))
    return new (memory) Variable (var->getType (), var);

  switch (opcode)
    {
    case Op_Phi:
      if (loop->getHeader () == inst->getNode ())
        return analyze_scev_for_loop_phi (inst, loop);

      // TODO: Ignore other complex cases for now.
      return NULL;

    case Op_Add:
    case Op_Sub:
    case Op_Mul:
    case Op_Shl:
      {
        SsaOpnd *op1 = inst->getSrc(0)->asSsaOpnd ();
        SsaOpnd *op2 = inst->getSrc(1)->asSsaOpnd ();
        Expr *ev1 = analyze_scev (op1, loop);
        Expr *ev2 = ev1 ? analyze_scev (op2, loop) : NULL;

        if (!ev2)
          return NULL;

        if (opcode == Op_Shl)
          {
            if (Integer *itmp = ev2->as_integer ())
              // Convert constant left-shift to multiply.
              {
                opcode = Op_Mul;
                ev2 = new (memory) Integer (ev2->get_type (), 1 << itmp->value);
              }
            else
              // We don't support non-constant shifts.
              return NULL;
          }

        return fold_build (opcode, ev1, ev2);
      }

    case Op_Shladd:
      {
        SsaOpnd *op1 = inst->getSrc(0)->asSsaOpnd ();
        SsaOpnd *op2 = inst->getSrc(1)->asSsaOpnd ();
        SsaOpnd *op3 = inst->getSrc(2)->asSsaOpnd ();
        Expr *ev1 = analyze_scev (op1, loop);
        Expr *ev2 = ev1 ? analyze_scev (op2, loop) : NULL;
        Integer *itmp = ev2 ? ev2->as_integer () : NULL;
        Expr *ev3 = itmp ? analyze_scev (op3, loop) : NULL;

        if (!ev3)
          return NULL;

        return fold_build (Op_Add,
                           fold_build (Op_Mul, ev1,
                                       new (memory) Integer (itmp->get_type (),
                                                             1 << itmp->value)),
                           ev3);
      }

    default:
      return NULL;
    }
}

Expr*
ScalarEvolution::number_of_iterations (LoopInfo &loop_info)
{
  LoopNode *loop = loop_info.get_loop ();
  Edge *exit_edge = loop_info.find_single_exit ();
  BranchInst *cond_inst = loop_info.get_exit_branch_inst ();

  if (!cond_inst)
    return NULL;

  SsaOpnd *op0 = cond_inst->getSrc(0)->asSsaOpnd ();
  Expr *scev0 = analyze_scev (op0, loop);
  Expr *scev1;
  ComparisonModifier cond_code = cond_inst->getComparisonModifier ();

  // Convert the two unary operations to standard binary operations.
  // Why use these two unary operations in IR?  They're not convenient.
  switch (cond_code)
    {
    case Cmp_Zero:
      cond_code = Cmp_EQ;
      scev1 = int_zero;
      break;

    case Cmp_NonZero:
      cond_code = Cmp_NE_Un;
      scev1 = int_zero;
      break;

    default:
      {
        SsaOpnd *op1 = cond_inst->getSrc(1)->asSsaOpnd ();
        scev1 = analyze_scev (op1, loop);
      }
    }

  if (!scev0 || !scev1
      || (bool)scev0->as_poly_chrec () == (bool)scev1->as_poly_chrec ())
    // It's too complex, just give up.
    return NULL;

  PolyChrec *chrec = (scev0->as_poly_chrec () ? scev0 : scev1)->as_poly_chrec ();
  Expr *first_exit_value;

  if (cond_inst->getTargetLabel ()
      != exit_edge->getTargetNode()->getFirstInst ())
    // When condition is true, continue to run the loop.
    switch (cond_code)
      {
      case Cmp_NE_Un:
      case Cmp_GT:
      case Cmp_GT_Un:
        first_exit_value = scev0->as_poly_chrec () ? scev1 : scev0;
        break;

      case Cmp_GTE:
      case Cmp_GTE_Un:
        first_exit_value = (scev0->as_poly_chrec ()
                            ? fold_build (Op_Sub, scev1, int_one)
                            : fold_build (Op_Add, scev0, int_one));
        break;

      default:
        return NULL;
      }
  else
    // When condition is true, exit the loop.
    switch (cond_code)
      {
      case Cmp_GT:
      case Cmp_GT_Un:
        first_exit_value = (scev0->as_poly_chrec ()
                            ? fold_build (Op_Add, scev1, int_one)
                            : fold_build (Op_Sub, scev0, int_one));
        break;

      case Cmp_EQ:
      case Cmp_GTE:
      case Cmp_GTE_Un:
        first_exit_value = scev0->as_poly_chrec () ? scev1 : scev0;
        break;

      default:
        return NULL;
      }

  // TODO: The Op_TauDiv should be something like Op_CeilDiv, which
  // denotes a ceiling division, i.e. the division for integer result
  // that rounds the quotient toward infinity.
  return fold_build (Op_TauDiv,
                     fold_build (Op_Sub, first_exit_value, chrec->left),
                     chrec->right);
}

bool
ScalarEvolution::is_mul_inst (Inst *inst)
{
  switch (inst->getOpcode ())
    {
    case Op_Mul:
      return true;

    case Op_Shl:
      {
        SsaOpnd *op1 = strip_copies (inst->getSrc(1)->asSsaOpnd ());

        return op1->getInst()->getOpcode () == Op_LdConstant;
      }

    case Op_Shladd:
      {
        SsaOpnd *op1 = strip_copies (inst->getSrc(1)->asSsaOpnd ());
        SsaOpnd *op2 = strip_copies (inst->getSrc(2)->asSsaOpnd ());

        return (op1->getInst()->getOpcode () == Op_LdConstant
                && op2->getInst()->getOpcode () == Op_LdConstant
                && op2->getInst()->asConstInst()->getValue().i4 == 0);
      }

    default:
      return false;
    }
}

Expr*
ScalarEvolution::instantiate_scev (Expr *scev, LoopNode *use_loop, LoopNode *wrto_loop)
{
  if (!scev)
    return scev;

  Opcode opcode = scev->get_opcode ();

  switch (opcode)
    {
    case Op_LdConstant:
      return scev;

    case Op_LdVar:
      {
        Variable *var = scev->as_variable ();
        Inst* inst = var->opnd->getInst ();

        // If VAR is defined outside WRTO_LOOP, leave it as symbolic form.
        if (!wrto_loop->inLoop (inst->getNode ()))
          return var;

        LoopNode *def_loop = loop_tree->getLoopNode (inst->getNode (), false);
        LoopNode *common_loop = loop_tree->findCommonLoop (use_loop, def_loop);

        scev = analyze_scev (var->opnd, common_loop);

        if (scev)
          scev = instantiate_scev (scev, use_loop, wrto_loop);

        return scev;
      }

    case Op_Branch:
      {
        PolyChrec *chrec = scev->as_poly_chrec ();
        Expr *left = instantiate_scev (chrec->left, use_loop, wrto_loop);
        Expr *right = left ? instantiate_scev (chrec->right, use_loop, wrto_loop) : NULL;

        return (!right ? NULL
                : (left == chrec->left && right == chrec->right ? chrec
                   : fold_build_poly_chrec (chrec->get_type (), left, right, chrec->loop)));
      }

    case Op_Add:
    case Op_Sub:
    case Op_Mul:
      {
        OpExpr *expr = scev->as_op_expr ();
        Expr *op0 = instantiate_scev (expr->opnd[0], use_loop, wrto_loop);
        Expr *op1 = op0 ? instantiate_scev (expr->opnd[1], use_loop, wrto_loop) : NULL;

        return (!op1 ? NULL
                : (op0 == expr->opnd[0] && op1 == expr->opnd[1] ? expr
                   : fold_build (opcode, op0, op1)));
      }

    default:
      return NULL;
    }
}

// Return true if PHI is a loop phi node.

bool
ScalarEvolution::loop_phi_node_p (Inst *phi)
{
  assert (phi->getOpcode () == Op_Phi);

  return loop_tree->isLoopHeader (phi->getNode ());
}

// INST must be a loop-phi node of LOOP.

Expr*
ScalarEvolution::analyze_scev_for_loop_phi (Inst *phi, LoopNode *loop)
{
  assert (loop_phi_node_p (phi));

  Expr *init = NULL;

  // Find the initial value of PHI in LOOP.
  for (unsigned i = 0; i < phi->getNumSrcOperands (); i++)
    {
      SsaOpnd *arg = strip_copies (phi->getSrc(i)->asSsaOpnd ());
      Inst *inst = arg->getInst ();

      if (loop->inLoop (inst->getNode ()))
        continue;

      if (init)
        // TODO: We don't support multi-entries for now.
        return NULL;
      else
        // The ARG must be loop-invariant.  compute_step certainly
        // can handle this simple case, so we just use it.
        init = compute_step (arg, phi, loop);
    }

  // Every loop must have at least one entry.
  assert (init);

  Expr *ev_fn = NULL;

  // Compute step part of the evolution of PHI in LOOP.
  for (unsigned i = 0; i < phi->getNumSrcOperands (); i++)
    {
      SsaOpnd *arg = phi->getSrc(i)->asSsaOpnd ();

      if (!loop->inLoop (arg->getInst()->getNode ()))
        continue;

      if (ev_fn)
        // TODO: We don't support multi-backedges for now.
        return NULL;
      else
        {
          ev_fn = compute_step (arg, phi, loop);

          if (!ev_fn || !ev_fn->as_poly_chrec ())
            // Return dont-know since step is not a polynomial chrec.
            return NULL;
          else
            ev_fn->as_poly_chrec()->left = init;
        }
    }

  return ev_fn;
}

// Compute step part for loop-phi node PHI of LOOP by tracing SSA flows
// starting from one of its argument ARG to itself.  It may be another
// polynomial chrec of LOOP, but currently we don't support higher degree
// polynomial, i.e. only loop-invariant steps are allowed for now.

Expr*
ScalarEvolution::compute_step (SsaOpnd *arg, Inst *start_phi, LoopNode *loop)
{
  arg = strip_copies (arg);

  Inst* inst = arg->getInst ();
  Opcode opcode = inst->getOpcode ();

  // Return the constant in spite of whether it's defined in LOOP.
  if (opcode == Op_LdConstant)
    return new (memory) Integer (arg->getType (), inst->asConstInst()->getValue().i4);

  // If ARG is defined outside LOOP and is not a result of multiply
  // instruction, return the symbolic form (parameter).  Allowing
  // expansion of multiply instructions may make GCD more accurate.
  if (!loop->inLoop (inst->getNode ()) && !is_mul_inst (inst))
    return new (memory) Variable (arg->getType (), arg);

  switch (opcode)
    {
    case Op_Phi:
      if (inst == start_phi)
        // Reach the start phi, return the chrec with step zero.
        // Don't use fold_build_poly_chrec here as we need the PolyChrec
        // object to denote that we have reached the start phi.
        return new (memory) PolyChrec (arg->getType (), NULL, int_zero, loop);

      if (!loop_phi_node_p (inst))
        // TODO: Ignore conditional phi nodes for now.
        return NULL;

      // TODO: Ignore all complex cases for now, including when INST is
      // a loop-phi-node of LOOP and of inner loop of LOOP.
      return NULL;

    case Op_Add:
    case Op_Sub:
    case Op_Mul:
    case Op_Shl:
    case Op_Shladd:
      {
        SsaOpnd *op1 = inst->getSrc(0)->asSsaOpnd ();
        SsaOpnd *op2 = inst->getSrc(1)->asSsaOpnd ();
        Expr *ev1 = compute_step (op1, start_phi, loop);
        Expr *ev2 = ev1 ? compute_step (op2, start_phi, loop) : NULL;

        if (!ev2)
          return NULL;

        // Is it necessary to use the trinary instruction Op_Shladd?
        // It complicates problems and leads to ugly code as follows.
        if (opcode == Op_Shl || opcode == Op_Shladd)
          {
            Integer *i2 = ev2->as_integer ();

            if (!i2)
              return NULL;

            if (opcode == Op_Shl)
              // If shift a constant, convert it to multiply.
              {
                ev2 = new (memory) Integer (i2->get_type (), 1 << i2->value);
                opcode = Op_Mul;
              }
            else
              {
                SsaOpnd *op3 = inst->getSrc(2)->asSsaOpnd ();
                Expr *ev3 = compute_step (op3, start_phi, loop);
                Integer *i3 = ev3 ? ev3->as_integer () : NULL;

                if (!i3)
                  return NULL;

                if (i2->value == 0)
                  // Shift zero bit, convert the shift-add to add.
                  {
                    ev2 = ev3;
                    opcode = Op_Add;
                  }
                else if (i3->value == 0)
                  // Add zero, convert the shift-add to multiply.
                  {
                    ev2 = new (memory) Integer (i2->get_type (), 1 << i2->value);
                    opcode = Op_Mul;
                  }
                else
                  // We don't support non-trivial trinary operations.
                  return NULL;
              }
          }

        switch (((bool)ev1->as_poly_chrec () << 1) + (bool)ev2->as_poly_chrec ())
          {
          case 0:
            return fold_build (opcode, ev1, ev2);

          case 1:
            if (opcode == Op_Sub)
              return NULL;

            std::swap (ev1, ev2);

          case 2:
            {
              if (opcode == Op_Mul)
                return NULL;

              PolyChrec *chrec = ev1->as_poly_chrec();
              chrec->right = fold_build (opcode, chrec->right, ev2);
              return chrec;
            }

          case 3:
            // Can't be represented by a polynomial chrec.
            return NULL;
          }
      }

    default:
      return NULL;
    }
}

Expr*
ScalarEvolution::fold (Opcode opcode, Expr *e1, Expr *e2)
{
  if (PolyChrec *chrec1 = e1->as_poly_chrec ())
    {
      if (PolyChrec *chrec2 = e2->as_poly_chrec ())
        {
          if (chrec1->loop == chrec2->loop)
            switch (opcode)
              {
              case Op_Add:
              case Op_Sub:
                return fold_build_poly_chrec (e1->get_type (),
                                              fold_build (opcode, chrec1->left, chrec2->left),
                                              fold_build (opcode, chrec1->right, chrec2->right),
                                              chrec1->loop);
              default:
                ;
              }
          else if (loop_tree->isAncestor (chrec1->loop, chrec2->loop))
            return fold_build_poly_chrec (e1->get_type (),
                                          fold_build (opcode, chrec1, chrec2->left),
                                          chrec2->right,
                                          chrec2->loop);
          else if (loop_tree->isAncestor (chrec2->loop, chrec1->loop))
            return fold_build_poly_chrec (e1->get_type (),
                                          fold_build (opcode, chrec1->left, chrec2),
                                          chrec1->right,
                                          chrec1->loop);
        }
      else
        switch (opcode)
          {
          case Op_Add:
          case Op_Sub:
            return fold_build_poly_chrec (e1->get_type (),
                                          fold_build (opcode, chrec1->left, e2),
                                          chrec1->right, chrec1->loop);

          case Op_Mul:
            return fold_build_poly_chrec (e1->get_type (),
                                          fold_build (opcode, chrec1->left, e2),
                                          fold_build (opcode, chrec1->right, e2),
                                          chrec1->loop);

          default:
            ;
          }

      return NULL;
    }
  else if (PolyChrec *chrec2 = e2->as_poly_chrec ())
    {
      switch (opcode)
        {
        case Op_Add:
          return fold_build_poly_chrec (e1->get_type (),
                                        fold_build (opcode, e1, chrec2->left),
                                        chrec2->right, chrec2->loop);

        case Op_Mul:
          return fold_build_poly_chrec (e1->get_type (),
                                        fold_build (opcode, e1, chrec2->left),
                                        fold_build (opcode, e1, chrec2->right),
                                        chrec2->loop);

        case Op_Sub:
          return fold_build_poly_chrec (e1->get_type (),
                                        fold_build (opcode, e1, chrec2->left),
                                        fold_build (opcode, int_zero, chrec2->right),
                                        chrec2->loop);

        default:
          ;
        }

      return NULL;
    }
  else if (Integer *i1 = e1->as_integer ())
    {
      // 0 + e2 => e2, 1 * e2 => e2
      if ((opcode == Op_Add && i1->value == 0)
          || (opcode == Op_Mul && i1->value == 1))
        return e2;

      // 0 * e2 => 0
      if (opcode == Op_Mul && i1->value == 0)
        return i1;

      if (Integer *i2 = e2->as_integer ())
        switch (opcode)
          {
          case Op_Add:
            return new (memory) Integer (i1->get_type (), i1->value + i2->value);

          case Op_Sub:
            return new (memory) Integer (i1->get_type (), i1->value - i2->value);

          case Op_Mul:
            return new (memory) Integer (i1->get_type (), i1->value * i2->value);

          case Op_TauDiv:
            // TODO: We don't handle the floor and ceiling problem for now.
            // Rather, we only process their common case and leave other cases
            // unchanged.
            if (i1->value % i2->value == 0)
              return new (memory) Integer (i1->get_type (), i1->value / i2->value);
            else
              return NULL;

          default:
            ;
          }
    }
  else if (Integer *i2 = e2->as_integer ())
    {
      // e1 + 0 => e1, e1 * 1 => e1, e1 / 1 => e1
      if (((opcode == Op_Add || opcode == Op_Sub) && i2->value == 0)
          || ((opcode == Op_Mul || opcode == Op_TauDiv) && i2->value == 1))
        return e1;

      // e1 * 0 => 0
      if (opcode == Op_Mul && i2->value == 0)
        return i2;
    }

  return fold_polynomial (opcode, e1, e2);
}

// Fold two polynomials into such form c + a * b + d * e + ...
// Return the folded expression if successful, otherwise return NULL.

Expr*
ScalarEvolution::fold_polynomial (Opcode opcode, Expr *e1, Expr *e2)
{
  Opcode code1 = e1->get_opcode ();
  Opcode code2 = e2->get_opcode ();

  switch (opcode)
    {
    case Op_Add:
    case Op_Sub:
      // If the latter is an add or sub, the folding always succeeds.
      if (code2 == Op_Add || code2 == Op_Sub)
        {
          OpExpr *oe2 = e2->as_op_expr ();

          // p1 + (p2 + t) => (p1 + p2) + t
          return fold_build (code2 == opcode ? Op_Add : Op_Sub,
                             fold_build (opcode, e1, oe2->opnd[0]),
                             oe2->opnd[1]);
        }

      // Now, the latter is a single term.
      if (code1 == Op_Add || code1 == Op_Sub)
        {
          OpExpr *oe1 = e1->as_op_expr ();

          // If folding (p1 + t2) succeeds, (p1 + t1) + t2 => (p1 + t2) + t1
          if (Expr *left = fold (opcode, oe1->opnd[0], e2))
            return fold_build (code1, left, oe1->opnd[1]);
          // If folding (t1 + t2) succeeds, (p1 + t1) + t2 => p1 + (t1 + t2)
          else if (Expr *right = fold (code1 == opcode ? Op_Add : Op_Sub,
                                       oe1->opnd[1], e2))
            return fold_build (code1, oe1->opnd[0], right);
          // Otherwise, t2 cannot be folded into e1 and the folding fails.
          else
            return NULL;
        }

      // Now, both e1 and e2 are terms.
      return term_add (opcode, e1, e2);

    case Op_Mul:
      // If the latter is an add or sub, folding always succeeds.
      if (code2 == Op_Add || code2 == Op_Sub)
        {
          OpExpr *oe2 = e2->as_op_expr ();

          // p1 * (p2 + t) => (p1 * p2) + (p1 * t)
          return fold_build (code2,
                             fold_build (Op_Mul, e1, oe2->opnd[0]),
                             fold_build (Op_Mul, e1, oe2->opnd[1]));
        }

      // Now, the latter is a term.
      if (code1 == Op_Add || code1 == Op_Sub)
        {
          OpExpr *oe1 = e1->as_op_expr ();

          // (p1 + t1) * t2 => p1 * t2 + t1 * t2
          return fold_build (code1,
                             fold_build (Op_Mul, oe1->opnd[0], e2),
                             fold_build (Op_Mul, oe1->opnd[1], e2));
        }

      // Now, both are terms.
      return term_mul (e1, e2);

    case Op_TauDiv:
      if (!e2->as_integer ())
        // TODO: We only support constant divisor for now.
        return NULL;

      if (code1 == Op_Add || code1 == Op_Sub)
        {
          OpExpr *oe1 = e1->as_op_expr ();

          // (p1 + t1) / c => p1 / c + t1 / c
          Expr *opnd0 = fold (Op_TauDiv, oe1->opnd[0], e2);
          Expr *opnd1 = opnd0 ? fold (Op_TauDiv, oe1->opnd[1], e2) : NULL;

          // Succeeds only when both sub foldings succeed.
          return opnd1 ? build (code1, opnd0, opnd1) : NULL;
        }

      // Now, e1 is a term.
      return term_div (e1, e2);

    default:
      return NULL;
    }
}

// Return true iff E1 and E2 are both Variable and have the same opnd.

bool
ScalarEvolution::same_opnd_p (Expr *e1, Expr *e2)
{
  Variable *v1, *v2;

  return ((v1 = e1->as_variable ()) && (v2 = e2->as_variable ())
          && v1->opnd == v2->opnd);
}

// Try to add or sub two terms T1 and T2 into one term.  If the result
// is one term, return it, otherwise return NULL.  For example,
// term_add (Op_add, a * b, 2 * a * b) returns 3 * a * b,
// but term_add (Op_Add, 2 * a * b, 3 * a * c) returns NULL.
// We assume factors in terms have been sorted.

Expr*
ScalarEvolution::term_add (Opcode opcode, Expr *t1, Expr *t2)
{
  assert (opcode == Op_Add || opcode == Op_Sub);

  if (OpExpr *mul1 = t1->as_op_expr ())
    {
      if (t1->get_opcode () != Op_Mul)
        // Only fold multiply operations
        return NULL;

      if (OpExpr *mul2 = t2->as_op_expr ())
        {
          if (t2->get_opcode () != Op_Mul)
            // Only fold multiply operations
            return NULL;

          if (!same_opnd_p (mul1->opnd[1], mul2->opnd[1]))
            return NULL;

          // If folding (t1 + t2) succeeds, (t1 * a) + (t2 * a) => (t1 + t2) * a.
          if (Expr *left = fold (opcode, mul1->opnd[0], mul2->opnd[0]))
            return fold_build (Op_Mul, left, mul1->opnd[1]);
        }
      else if (same_opnd_p (mul1->opnd[1], t2))
        {
          // If folding (t1 + 1) succeeds, (t1 * a) + a => (t1 + 1) * a.
          if (Expr *left = fold (opcode, mul1->opnd[0], int_one))
            return fold_build (Op_Mul, left, mul1->opnd[1]);
        }
    }
  else if (OpExpr *mul2 = t2->as_op_expr ())
    {
      if (t2->get_opcode () != Op_Mul)
        // Only fold multiply operations
        return NULL;

      if (same_opnd_p (t1, mul2->opnd[1]))
        {
          // If folding (1 + t2) succeeds, a + (t2 * a) => (1 + t2) * a.
          if (Expr *left = fold (opcode, int_one, mul2->opnd[0]))
            return fold_build (Op_Mul, left, mul2->opnd[1]);
        }
    }
  else if (same_opnd_p (t1, t2))
    return fold_build (Op_Mul, fold (opcode, int_one, int_one), t1);

  return NULL;
}

// Return true iff factor F1 should appear befor factor F2 in a
// normalized term.

bool
ScalarEvolution::factor_precede_p (Expr *f1, Expr *f2)
{
  if (f1->as_integer () && !f2->as_integer ())
    // Constant should be in the front of a term.
    return true;

  if (Variable *v1 = f1->as_variable ())
    if (Variable *v2 = f2->as_variable ())
      if (v1->opnd->getId () < v2->opnd->getId ())
        // Sort variables by their ID order.
        return true;

  return false;
}

// Multiply two terms into one term and return it.  Non-one constant
// always appears in the front of the result term, and variables are
// sorted in the order of their operand ID.

Expr*
ScalarEvolution::term_mul (Expr *t1, Expr *t2)
{
  if (t1->get_opcode () == Op_TauDiv)
    {
      OpExpr *div1 = t1->as_op_expr();

      if (div1->opnd[1] == t2)
        // t11 / t12 * t12 => t11
        return div1->opnd[0];
    }

  if (t2->get_opcode () == Op_TauDiv)
    {
      OpExpr *div2 = t2->as_op_expr();

      if (div2->opnd[1] == t1)
        // t22 * (t21 / t22) => t21
        return div2->opnd[0];
    }

  // If t2 is a multiply expression, always perform the folding
  // t1 * (t21 * t22) => (t1 * t21) * t22
  if (OpExpr *mul2 = t2->as_op_expr ())
    {
      if (t2->get_opcode () != Op_Mul)
        // Only fold multiply operations
        return NULL;

      return fold_build (Op_Mul,
                         fold_build (Op_Mul, t1, mul2->opnd[0]),
                         mul2->opnd[1]);
    }

  // Now t2 is a single integer or variable.
  if (OpExpr *mul1 = t1->as_op_expr ())
    {
      if (t1->get_opcode () != Op_Mul)
        // Only fold multiply operations
        return NULL;

      if (!factor_precede_p (t2, mul1->opnd[1]))
        // It has been in the ordered form, nothing needs to change.
        return NULL;
      else
        // Otherwise, t2 shoud precede mul1->opnd[1].  Perform
        // the folding (t11 * v1) * t2 => (t11 * t2) * v1
        return build (Op_Mul, fold_build (Op_Mul, mul1->opnd[0], t2),
                      mul1->opnd[1]);
    }

  // Now, t1 and t2 are either an integer or a variable.
  if (!factor_precede_p (t2, t1))
    // t2 shouldn't precede t1, the folding fails.
    return NULL;
  else
    // Otherwise, exchange them.
    return build (Op_Mul, t2, t1);
}

// Fold t1 / t2.

Expr*
ScalarEvolution::term_div (Expr *t1, Expr *t2)
{
  Integer *i2 = t2->as_integer ();

  // For now, t2 must be a constant integer.
  if (!i2)
    return NULL;

  // It should be catched before.
  assert (i2->value != 1);

  if (OpExpr *mul1 = t1->as_op_expr ())
    {
      assert (t1->get_opcode () == Op_Mul);

      Expr *opnd0 = fold (Op_TauDiv, mul1->opnd[0], t2);

      return opnd0 ? build (Op_Mul, opnd0, mul1->opnd[1]) : NULL;
    }

  return NULL;
}

Expr*
ScalarEvolution::fold_build (Opcode opcode, Expr *e1, Expr *e2)
{
  if (Expr *folded = fold (opcode, e1, e2))
    return folded;

  return build (opcode, e1, e2);
}

Expr*
ScalarEvolution::build (Opcode opcode, Expr *e1, Expr *e2)
{
  return new (memory) OpExpr (e1->get_type (), opcode, e1, e2);
}

// Create a new polynomial chrec if RIGHT is not zero, otherwise
// return LEFT, i.e. do the folding {left, +, 0}_l => left.

Expr*
ScalarEvolution::fold_build_poly_chrec (Type *type, Expr *left,
                                        Expr *right, LoopNode *loop)
{
  Integer *itmp = right->as_integer ();

  return (itmp && itmp->value == 0 ? left
          : new (memory) PolyChrec (type, left, right, loop));
}

int
ScalarEvolution::number_gcd (int a, int b)
{
  if (a > b)
    return number_gcd (b, a);

  for (;;)
    {
      int c = b % a;

      if (c == 0)
        return a;

      b = a;
      a = c;
    }
}

Expr*
ScalarEvolution::term_gcd (Expr *t1, Expr *t2)
{
  if (t1->int_zero_p ())
    {
      if (!t2->int_zero_p ())
        // gcd (0, t2) => t2
        return t2;
      else
        // gcd (0, 0) => 1
        return int_one;
    }

  if (t2->int_zero_p ())
    // gcd (t1, 0) => t1
    return t1;

  if (OpExpr *mul1 = t1->as_op_expr ())
    {
      if (mul1->get_opcode () != Op_Mul)
        // Only process terms.
        return int_one;

      if (OpExpr *mul2 = t2->as_op_expr ())
        {
          if (mul2->get_opcode () != Op_Mul)
            // Only process terms.
            return int_one;

          if (same_opnd_p (mul1->opnd[1], mul2->opnd[1]))
            // Found a common factor, perform the conversion
            // gcd (t1 * a, t2 * a) => gcd (t1, t2) * a
            return fold_build (Op_Mul,
                               term_gcd (mul1->opnd[0], mul2->opnd[0]),
                               mul1->opnd[1]);
          else if (factor_precede_p (mul1->opnd[1], mul2->opnd[1]))
            // a precedes b, gcd (t1 * a, t2 * b) => gcd (t1 * a, t2)
            return term_gcd (mul1, mul2->opnd[0]);
          else
            // Otherwise, gcd (t1 * a, t2 * b) => gcd (t1, t2 * b)
            return term_gcd (mul1->opnd[0], mul2);
        }
      else
        // gcd (t1 * a, b) => gcd (b, t1 * a).  See the following code.
        return term_gcd (t2, t1);
    }

  // Now, t1 is a single factor.
  if (OpExpr *mul2 = t2->as_op_expr ())
    {
      if (mul2->get_opcode () != Op_Mul)
        // Only process terms.
        return int_one;

      if (same_opnd_p (t1, mul2->opnd[1]))
        // gcd (a, t2 * a) => a
        return t1;
      else if (factor_precede_p (t1, mul2->opnd[1]))
        // a precedes b, gcd (a, t2 * b) => gcd (a, t2)
        return term_gcd (t1, mul2->opnd[0]);
      else
        // Otherwise, gcd (a, t2 * b) => 1
        return int_one;
    }

  // Now, t1 and t2 are either an integer or a variable.
  if (same_opnd_p (t1, t2))
    // gcd (a, a) => a
    return t1;

  if (Integer *i1 = t1->as_integer ())
    if (Integer *i2 = t2->as_integer ())
      return new (memory) Integer (i1->get_type (),
                                   number_gcd (i1->value, i2->value));

  return int_one;
}

// Return false only when DIVIDEND and DIVISOR are both terms, and we
// are sure that DIVIDEND cannot be exactly divided by DIVISOR.

bool
ScalarEvolution::term_may_divided_by_p (Expr *dividend, Expr *divisor)
{
  if (OpExpr *mul1 = dividend->as_op_expr ())
    {
      if (mul1->get_opcode () != Op_Mul)
        // Only process terms.  Make conservative assumption for others.
        return true;

      if (OpExpr *mul2 = divisor->as_op_expr ())
        {
          if (mul2->get_opcode () != Op_Mul)
            // Only process terms.  Make conservative assumption for others.
            return true;

          if (same_opnd_p (mul1->opnd[1], mul2->opnd[1]))
            // Common divisor can be safely removed.
            return term_may_divided_by_p (mul1->opnd[0], mul2->opnd[0]);
          else if (factor_precede_p (mul1->opnd[1], mul2->opnd[1]))
            // There is a variable of divisor not in dividend, ignore
            // the last variable of divisor since it may equal 1 (the
            // most conservative assumption).  This is safe because
            // (a * b) % c != 0 implies (a * b) % (c * d) != 0.
            return term_may_divided_by_p (mul1, mul2->opnd[0]);
          else
            // There is a variable of dividend not in divisor.  We can
            // only make this conservative assumption without information
            // of values of variables, since that variable may equal
            // any value even the whole divisor.
            return true;
        }
      else
        // See the comments of the last case above.
        return true;
    }

  // Now, dividend is a single factor.
  if (OpExpr *mul2 = divisor->as_op_expr ())
    {
      if (mul2->get_opcode () != Op_Mul)
        // Only process terms.  Make conservative assumption for others.
        return true;

      if (same_opnd_p (dividend, mul2->opnd[1]))
        // Common divisor can be safely removed.
        return term_may_divided_by_p (int_one, mul2->opnd[0]);
      else if (factor_precede_p (dividend, mul2->opnd[1]))
        // There is a variable of divisor not in dividend.  See above.
        return term_may_divided_by_p (dividend, mul2->opnd[0]);
      else
        // There is a variable of dividend not in divisor.  See above.
        return true;
    }

  // Now, dividend and divisor are either an integer or a variable.

  if (Integer *i1 = dividend->as_integer ())
    if (Integer *i2 = divisor->as_integer ())
      return i1->value % i2->value == 0;

  return true;
}

bool
ScalarEvolution::may_divided_by_p (Expr *dividend, Expr *divisor)
{
  assert (!divisor->int_zero_p ());

  if (OpExpr *opexpr = dividend->as_op_expr ())
    {
      switch (opexpr->get_opcode ())
        {
        case Op_Add:
        case Op_Sub:
          // Return false only when one part can be divided and the
          // other part cannot.  Note that even when two parts can
          // neither be divided, the whole may.  For example, 5x + 1
          // and 2 when x may equal 1 during runtime.
          return (may_divided_by_p (opexpr->opnd[0], divisor)
                  == may_divided_by_p (opexpr->opnd[1], divisor));

        default:
          ;
        }
    }

  return term_may_divided_by_p (dividend, divisor);
}

void
ScalarEvolution::debug_scev_for_all (LoopNode *loop)
{
  // Process all inter loops first.
  for (LoopNode *child = loop->getChild (); child; child = child->getSiblings ())
    debug_scev_for_all (child);

  const Nodes &nodes = loop->getNodesInLoop ();

  for (Nodes::const_iterator i = nodes.begin (); i != nodes.end (); i++)
    {
      Node *node = *i;
      Inst *label = (Inst*)node->getLabelInst ();

      for (Inst *inst = label->getNextInst ();
           inst && inst != label;
           inst = inst->getNextInst ())
        {
          SsaOpnd *var = inst->getDst()->asSsaOpnd ();

          if (var)
            {
              Expr *scev = analyze_scev (var, loop);

              if (Log::isEnabled () && scev)
                {
                  Log::out() << "SCEV of ";
                  var->print (Log::out ());
                  Log::out() << ": " << scev << "\n";
                }
            }
        }
    }
}

}
