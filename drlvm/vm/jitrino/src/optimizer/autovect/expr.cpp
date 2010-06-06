/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#include "expr.h"

namespace Jitrino {

bool
Expr::int_zero_p () const
{
  Integer *i = as_integer ();

  return i && i->value == 0;
}

bool
Expr::int_nonzero_p () const
{
  Integer *i = as_integer ();

  return i && i->value != 0;
}

bool
Expr::int_one_p () const
{
  Integer *i = as_integer ();

  return i && i->value == 1;
}

SsaOpnd* Integer::gen_insts_before (Inst *inst, IRManager &irm)
{
  TypeManager &type_manager = irm.getTypeManager ();
  OpndManager &opnd_manager = irm.getOpndManager ();
  InstFactory &inst_factory = irm.getInstFactory ();

  Type *type = type_manager.getInt32Type ();
  SsaTmpOpnd *result = opnd_manager.createSsaTmpOpnd (type);
  Inst *new_inst = inst_factory.makeLdConst (result, value);

  new_inst->insertBefore (inst);

  return result;
}

SsaOpnd* Variable::gen_insts_before (Inst *inst, IRManager &irm)
{
  SsaVarOpnd *ssa_var_opnd = opnd->asSsaVarOpnd ();

  if (!ssa_var_opnd)
    {
      assert (opnd->asSsaTmpOpnd ());

      return opnd;
    }

  OpndManager &opnd_manager = irm.getOpndManager ();
  InstFactory &inst_factory = irm.getInstFactory ();

  Type *type = opnd->getType ();
  SsaTmpOpnd *result = opnd_manager.createSsaTmpOpnd (type);
  Inst *new_inst = inst_factory.makeLdVar (result, ssa_var_opnd);

  new_inst->insertBefore (inst);

  return result;
}

SsaOpnd* OpExpr::gen_insts_before (Inst *inst, IRManager &irm)
{
  TypeManager &type_manager = irm.getTypeManager ();
  OpndManager &opnd_manager = irm.getOpndManager ();
  InstFactory &inst_factory = irm.getInstFactory ();

  SsaOpnd *opnd0 = opnd[0]->gen_insts_before (inst, irm);
  SsaOpnd *opnd1 = opnd[1]->gen_insts_before (inst, irm);

  Type *type = opnd0->getType ();
  SsaTmpOpnd *result = opnd_manager.createSsaTmpOpnd (type);
  Opcode opcode = get_opcode ();

  switch (opcode)
    {
    case Op_TauDiv:
    case Op_TauRem:
      {
        Modifier mod = Modifier (SignedOp) | Modifier (Strict_No);
        SsaTmpOpnd *tau_safe = opnd_manager.createSsaTmpOpnd (type_manager.getTauType ());
        Inst *tau_safe_inst = inst_factory.makeTauSafe (tau_safe);
        Inst *new_inst;

        if (opcode == Op_TauDiv)
          new_inst = inst_factory.makeTauDiv (mod, result,
                                              opnd0, opnd1, tau_safe);
        else
          new_inst = inst_factory.makeTauRem (mod, result,
                                              opnd0, opnd1, tau_safe);

        tau_safe_inst->insertBefore (inst);
        new_inst->insertBefore (inst);

        break;
      }

    default:
      {
        Modifier mod = (Modifier (Overflow_None)
                        | Modifier (Exception_Never)
                        | Modifier (Strict_No));
        Inst *new_inst = inst_factory.makeInst (get_opcode (), mod,
                                                type->tag,
                                                result, opnd0, opnd1);
        new_inst->insertBefore (inst);
      }
    }

  return result;
}

std::ostream&
operator<< (std::ostream &out, const Expr *e)
{
  if (!e)
    out << "NULL";
  else if (Integer *i = e->as_integer ())
    out << i->value;
  else if (Variable *v = e->as_variable ())
    v->opnd->print (out);
  else if (OpExpr *o = e->as_op_expr ())
    {
      Opcode opcode = o->get_opcode ();
      const char *op;

      switch (opcode)
        {
        case Op_Add:
          op = "+";
          break;

        case Op_Sub:
          op = "-";
          break;

        case Op_Mul:
          op = "*";
          break;

        case Op_TauDiv:
          op = "/";
          break;

        default:
          op = "??";
        }

      out << '(';
      out << o->opnd[0];
      out << ' ' << op << ' ';
      out << o->opnd[1];
      out << ')';
    }
  else if (PolyChrec *c = e->as_poly_chrec ())
    {
      out << '{';
      out << c->left;
      out << ", +, ";
      out << c->right;
      out << "}_";
      out << c->loop->getPreNum ();
    }

  return out;
}

SsaOpnd*
strip_copies (SsaOpnd *var)
{
  Inst *inst = var->getInst ();

  switch (inst->getOpcode ())
    {
    case Op_StVar:
    case Op_LdVar:
    case Op_Copy:
      return strip_copies (inst->getSrc(0)->asSsaOpnd ());

    default:
      return var;
    }
}

bool
extract_base_and_index (SsaOpnd *var, SsaOpnd *&base, SsaOpnd *&index)
{
  var = strip_copies (var);

  Inst *inst = var->getInst ();
  Opcode opcode = inst->getOpcode ();

  if (opcode == Op_AddScaledIndex)
    {
      SsaOpnd *op0 = strip_copies (inst->getSrc(0)->asSsaOpnd ());
      SsaOpnd *op1 = strip_copies (inst->getSrc(1)->asSsaOpnd ());

      if (op0->getInst()->getOpcode () == Op_AddScaledIndex)
        // We don't allow addresses like (base + index) + index
        return false;

      base = op0;
      index = op1;
    }
  else
    {
      base = var;
      index = NULL;
    }

  return true;
}

}
