/* -*- mode: c++; indent-tabs-mode: nil; -*- */

#ifndef _EXPR_H_
#define _EXPR_H_

#include "irmanager.h"
#include "LoopTree.h"

namespace Jitrino {

class Expr;
class Integer;
class Variable;
class OpExpr;
class PolyChrec;

// General expression type.

class Expr
{
public:
  Type* get_type () const { return type; }

  Opcode get_opcode () const { return opcode; }

  Integer* as_integer () const { return opcode == Op_LdConstant ? (Integer*)this : NULL; }

  Variable* as_variable () const { return opcode == Op_LdVar ? (Variable*)this : NULL; }

  OpExpr* as_op_expr () const { return opcode >= Op_Add && opcode <= Op_Cmp3 ? (OpExpr*)this : NULL;}

  PolyChrec* as_poly_chrec () const { return opcode == Op_Branch ? (PolyChrec*)this : NULL; }

  // Return true iff this is an integer zero.
  bool int_zero_p () const;

  // Return true iff this is an integer nonzero.
  bool int_nonzero_p () const;

  // Return true iff this is an integer one.
  bool int_one_p () const;

  // Generate instructions for computing this expression before INST.
  // Return the SSA variable containing the final result.
  virtual SsaOpnd* gen_insts_before (Inst *inst, IRManager &irm) = 0;

  virtual ~Expr () {}

protected:
  Expr (Type *t, Opcode o) : type (t), opcode (o) {}

private:
  Type *type;
  const Opcode opcode;
};

// Integer constants of an expression tree (leaf nodes).
// Reuse the opcode Op_LdConstant to represent this class.

class Integer : public Expr
{
public:
  Integer (Type *t, I_32 v) : Expr (t, Op_LdConstant), value (v) {}

  virtual SsaOpnd* gen_insts_before (Inst *inst, IRManager &irm);

public:
  I_32 value;
};

// Variable of an expression tree (leaf nodes).
// Reuse the opcode Op_LdVar to represent this class.

class Variable : public Expr
{
public:
  Variable (Type *t, SsaOpnd *o) : Expr (t, Op_LdVar), opnd (o) {}

  virtual SsaOpnd* gen_insts_before (Inst *inst, IRManager &irm);

public:
  SsaOpnd *opnd;
};

// Expression with an operator (internal nodes of an expression tree).
// Reuse the opcodes from Op_Add through Op_Cmp3 to represent this class.

class OpExpr : public Expr
{
public:
  OpExpr (Type *t, Opcode op, Expr *e0, Expr *e1 = NULL) : Expr (t, op)
  {
    assert (op >= Op_Add && op <= Op_Cmp3);

    opnd[0] = e0; opnd[1] = e1;
  }

  virtual SsaOpnd* gen_insts_before (Inst *inst, IRManager &irm);

public:
  Expr *opnd[2];
};

// Polynomial chain of recurrences.
// Reuse the opcode Op_Branch to represent this class.

class PolyChrec : public Expr
{
public:
  PolyChrec (Type *t, Expr *l, Expr *r, LoopNode *p)
    : Expr (t, Op_Branch), left (l), right (r), loop (p)
  {
  }

  virtual SsaOpnd* gen_insts_before (Inst *inst, IRManager &irm)
  {
    assert (0);
    return NULL;
  }

public:
  Expr *left;
  Expr *right;
  // The loop this chrec is with respect to.
  LoopNode *loop;
};

std::ostream& operator<< (std::ostream &out, const Expr *e);

//====================================================================
//       The following are some frequently used utilities.
//====================================================================

// Strip copy instructions starting from VAR's definition instruction.

extern SsaOpnd* strip_copies (SsaOpnd *var);

// Extract base and index parts from array address (operand of ld/stind) VAR.
// Return false if the address is not in the normal form: base or base + index.
// For example, (base + index) + index is not normal.  The reason for checking
// this is that we want to try to ensure a property:
// base1 + index1 != base2 + index2 if base1 != base2.  With this,
// base1[i] will never alias to base2[j] as long as i != j.

extern bool extract_base_and_index (SsaOpnd *var, SsaOpnd *&base, SsaOpnd *&index);

}

#endif
