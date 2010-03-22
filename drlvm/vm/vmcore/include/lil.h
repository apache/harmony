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
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  


#ifndef _LIL_H_
#define _LIL_H_

#include <stdio.h>
#include "open/types.h"
#include "vm_core_types.h"
#include "tl/memory_pool.h"
#include "m2n.h"

/* This file describes LIL - the architecture-independent language used to generate code stubs etc.

Goals:
* Lightweight - i.e. small language, quality code generation possible with small simple algorithms
* Remove most architecture dependencies from code
* (Future) allow inlining of VM stubs into JITed code
* (Future) allow inlining of GC or Thread/Sync code via LIL
* (Possibly future) be the lowest ir of a simple JIT

Design constaints:
* Keep language syntax LL(1) so that parsing can be done with a recursive descent parser
* Make single-pass register allocation possible
* Allow easy experimentation with calling conventions

The basic unit of LIL is a code stub.  It is intended to be the entry point of a function that can be
called by managed or native code, and executes in the presence of an activation frame.  The frame consists
of up to six parts: inputs, standard places, an m2n frame, locals, outputs, and a return variable.  Which
parts are present in the activation frame, and how big each part is, can vary across points in the code stub.

A LIL code stub is an entry and a sequence of statements.  The entry specifies the inputs and their types, the
number of standard places, the type of the return, and the calling convention (which determines where the inputs,
return address, and return goes, which registers are caller save vs callee save, and who pops the inputs; standard
places are determined by LIL and the platform, and are intended for trampolines).  An entry can have an "arbitrary"
signature, meaning that the number and type of inputs and type of return is unknown (the stub must work for all
possibilities).  After entry the declared number
of input variables are available, the declared number of standard places are available, there is no m2n frame,
there are no locals, there are no outputs, and there is no return variable.

cs ::= entry i:cc:sig; ss
ss ::= s | s ss

The statements are:

s ::= :l;
|  locals i;
|  std_places i;
|  alloc v,i;
|  v=uop o;
|  v=o op o;
|  v=o;
|  v=ts;
|  handles=o;
|  ld v,addr(,zx|,sx)?;
|  st addr,o;
|  inc addr;
|  cas addr=o,o,l;
|  j l;
|  jc cond,l;
|  out cc:sig;
|  in2out cc:RT;
|  calli o;
|  ret;
|  push_m2n i oh;
|  m2n_save_all;
|  pop_m2n;
|  print str, o;

uop ::= neg | not | sx1 | sx2 | sx4 | zx1 | zx2 | zx4 | ...
op ::= + | - | * | & | << | ...

addr ::= [ov osv i :T ackrel]
ov ::= | v+
osv ::= | 1*v+ | 2*v+ | 4*v+ | 8*v+
acqrel ::= | ,acquire | ,release

cond ::= v=0 | v!=0 | o=o | o!=o | o<o | o<=o | o<u o | o<=u o | ...

calli ::= call | call.noret | tailcall
oh ::= | , handles

o ::= (v | i) (:t)?

cc ::= platform | jni | managed | rth | stdcall
platform = default calling convention used by VM
jni      = calling convention used by JNI
managed  = calling convention used by jitted code
rth      = calling convention used to call runtime helpers
stdcall  = Windows's stdcall calling convention

l is a label (sequence of alpha, digit, _, starting with alpha or _)
v is a variable, one of: i0, i1, ... (input variables), sp0, sp1, ... (standard places), l0, l1, ... (local variables),
o0, o1, ... (output variables), r (return variable)
i is an immediate (sequence of digits, or 0x hex digits)
str is either a string enclosed in single quotes, or a number denoting an address

Operand semantics:

An operand with a v stands for that variable; an operand with an i stands for that immediate; an operand with an optional :t is cast to the
type t (ie it has type t).  Only certain casts are allowed by the verifier: the types ref, pint, and g4/g8 (depending upon architecture) are
castable to each other.  Note that casting refs to/from the other types may be GC unsafe.

Statement semantics:

:l declares a label l.  A LIL code stub is not valid if such a label merges control flow with inconsistent
activation frames.  Specifically, the number and types of inputs must be the same (currently there is no way they
could not be), only the minimum of the number of standard places will be available after the statement, the m2n frame
must either be present in all incoming edges or absent in all incoming edges, the number of locals must be the same, the
number of outputs must be the same, and
the return variable will be defined after the statement only if it is defined on all incoming edges.

locals i makes i locals available in the activation frame.  If there were locals available previously, they are lost.

std_places i makes i standard places available in the activation frame.  If there were standard places available previously,
they are lost.

alloc v, i allocates i contigous bytes of space whose lifetime is up to the next ret, tailcall, or pop_m2n instruction.
The address of this
space is placed into v.  The space will not be moved for any reason.  The amount of space allocated by such instructions must
be statically determinable for each code point in a code stub.  This instruction may not appear between out/in2out and
call/call.noret/tailcall instructions.

v=uop o evaluates o to a value, performs unary operation uop on it, and stores the result in v.

v=o1 op o2 evaluates o1 & o2 to values, performs binary operation op on them, and stores the result in v.

v=ts places a pointer to the current thread's thread structure into v.

handles=o evaluates o to a value and sets the handles pointer in the m2n frame to this value.  This instruction must be
dominated by a push_m2n handles instruction.

ld v,addr evaluates addr to an address (see below) and loads the value (of type given in addr) at this address into v.  If ,zx appears
at the end then the value is zero extended into v, which gets type pint; if ,sx appears at the end then the value is sign extended into v,
which gets pint; otherwise, v gets the same type as in addr.

st addr,o evaluates addr to an address (see below) o to a value and stores the value into the contents (of type given in addr) at the address.

inc addr evaluates addr to an address (ses below) and increments the value (of type given in addr) at this address.

cas addr=o1,o2,l evaluates addr to an address (see below) a, o1 to a value v1, and o2 to a value v2.  Then it atomically: compares
the value (of type gien in addr) to o1 and if equal swaps the contents of a with o2.  If the match fails then it jumps to label l.

ov+osv+i:T acqrel evaluates to (0 | contents of v) + (0 | 1 * contents of v | 2 * contents of v | 4 * contents of v |
8 * contents of v) + i.  Further if acqrel is acquire then the operation in which it appears has acquire semantics, and if
it is release then the operation in which it appears has release semantics.

j l jumps to label l.  A LIL code stub is invalid if l is not a declared label in the code stub.

jc cond, l either continues with the next statement or jumps to label l, depending upon the evaluation of cond.  A LIL
code stub is invalid if l is not declared in the stub.  The conditions are evaluated as follows.  v=0 will branch if v
has value 0; v!=0 will branch if v does not have value 0; o1=o2 will branch if o1 & o2 evaluate to the same value; o1!=o2
will branch if o1 & o2 evaluate to different values; o1<o2 will branch if o1 evaluates to a value signed less than what
o2 evaluates to; o1<=o2 will branch if o1 evaluates to a value signed less than or equal what
o2 evaluates to; o1<u o2 will branch if o1 evaluates to a value unsigned less than what
o2 evaluates to; o1<=u o2 will branch if o1 evaluates to a value unsigned less than or equal what
o2 evaluates to.

out cc:Ts:RT sets up in the activation frame the given number of outputs with the given types, cc says what calling
convention will be used, and RT declares the return type.  This statement preserves the inputs, standard places, locals,
return, alloced memory, and m2n frame, but destroys the outputs.

in2out cc:RT sets up outputs equal in number and type to the inputs, copies the inputs to the outputs, cc says what calling
convention will be used, and maybe different from the code stub's calling convention; the return type is given by RT.
This statement preserves the inputs, standard places, locals, return, and m2n frame, but redefines the outputs.  An in2out
is not allowed if the entry signature is arbitrary.

call o evaluates o to an address and calls it.  A LIL code stub is invalid unless call o is dominated by an out statement
or is dominated by an in2out statement.  The calling convention declared in the dominating statement is used for this call.
This statement preserves inputs, locals, alloced memory, and the m2n frame, but destroys the standard places, outputs, and
redefines the return to be the value returned from the call.

call.noret o is similar to call o, but the LIL write is asserting the the function never returns.  The LIL code generator
is free to not generate code to deal with the function returning.

tailcall o is equivalent to in2out cc:RT;call o;ret; where cc is the code stub's calling convention and RT is the code stub's
return type, except that the LIL code generator might optimise it, and the entry signature may be arbitrary.

ret returns from the code stub.  A LIL code stub is invalid if the return is not defined and the return type of the code stub
is not void, or if there is an m2n frame.  The calling convention of the code stub determines how the return is done.  A return
will automatically free locals, outputs, memory allocated by alloc, and standard places.  A return is not allowed if the entry
signature is arbitrary.

push_m2n i,h adds an m2n frame to the activation frame.  A LIL code stub is invalid if there is already an m2n frame or if an
alloc could be performed before push_m2n.  This instruction preserves inputs, standard places, and locals, but destroys outputs
and the return.  The optional handles keyword specifies if the m2n frame will have handles or not.  The i parameter specifies the
method associated with the M2nFrame (or 0 for no association).

m2n_save_all saves all callee saves registers in the m2n frame.  On some architectures not all such registers are saved on a push m2n,
just the ones that are relevant to most m2n operations.  See the M2n module for details.  This statement must be dominated by a push_m2n
and it preserves the context.

pop_m2n removes the m2n frame from the activation frame.  This statement must be dominated by a push_m2n instruction and with a
consistent handles declaration.  If handles is declared then pop_m2n will free all handles attached to this m2n frame.  The instruction
preserves inputs, locals, and the return, but destorys standard places, alloc-ed memory, and the outputs.

print outputs a message to the screen in debug mode, and does nothing in
release mode.  Its two arguments will be passed to a printf-like function,
with the usual semantics.

A LIL code stub is invalid if it does not end in a terminal instruction, that is, a ret, j, call.noret, or tailcall.

The types are:

T ::= g1 | g2 | g4 | g8 | f4 | f8 | ref | pint
RT ::= T | void

sig ::= Ts:RT | arbitrary
Ts ::= | T,Ts

The meaning of the types is: g1 means a one-byte general-purpose (ie not floating point) quantitiy, g2 means two-byte, g4
means 4-bytes, g8 menas eight-byte, f4 means a 4-byte floating-point quantity, f8 means 8-byte, ref means a pointer into the
managed heap, pint means a platform-sized general-purpose
quantity which might be an integer or an unmanaged pointer, void means there is nothing returned.

The LIL syntax allows comments that start with // and go to the end of the line.
*/

// Datatypes

struct LilCodeStub;
struct LilInstruction;
class LilBb;
struct LilSig;

enum LilVariableKind { LVK_In, LVK_StdPlace, LVK_Out, LVK_Local, LVK_Ret };

enum LilOperation {
    LO_Mov, LO_Add, LO_Sub, LO_SgMul, LO_Shl, LO_And, LO_Neg, LO_Not, LO_Sx1, LO_Sx2, LO_Sx4,
    LO_Zx1, LO_Zx2, LO_Zx4
};

enum LilPredicate {
    LP_IsZero, LP_IsNonzero, LP_Eq, LP_Ne, LP_Le, LP_Lt, LP_Ule, LP_Ult
};

enum LilAcqRel { LAR_None, LAR_Acquire, LAR_Release };
enum LilLdX { LLX_None, LLX_Zero, LLX_Sign };

enum LilCallKind { LCK_Call, LCK_CallNoRet, LCK_TailCall };

enum LilType { LT_G1, LT_G2, LT_G4, LT_G8, LT_F4, LT_F8, LT_Ref, LT_PInt, LT_Void };
enum LilCc { LCC_Platform, LCC_Managed, LCC_Rth, LCC_Jni, LCC_StdCall };

struct LilVariable {
    enum LilVariableKind tag;
    unsigned index;
};

struct LilOperand {
    bool is_immed;
    union {
        LilVariable var;
        POINTER_SIZE_INT imm;
    } val;
    bool has_cast;
    LilType t;
};


typedef const char* LilLabel;

//*** Creators

// Parse a string into code stub, the string can contain % constructs to refer to addition parameters
// Current the % syntax is: %ds where d is a decimal number that starts at 0 and increments by 1 through the string
// and s is a specifier.  The following specifiers are allowed in the following circumstances:
//   i in an immediate, takes the next argument as a pointer-size int (int on IA32, long long on IPF) as the value of the immediate
//   i in a variable, takes the next argument as a platform int as the index (not valid for ret)
//   m in a signature, takes the next argument as a Method_Handle and constructs the standard managed signature for it
//   j in a signature, takes the next argument as a Method_Handle and constructs the standard JNI signature for it
// % may also be used in a label to get generated labels, there are no numbers, but the specifiers are:
//   g in a label, generates a label
//   p in a label, use the previously generated label
//   n in a label, use the next generated label
//   o in a label, use the next next generated label
// Return NULL on parsing error
LilCodeStub* lil_parse_code_stub(const char* src, ...);
// Parse a string as a sequence of instructions and add them to the end of the input code stub.
// The % syntax above can be used.
// Return NULL on parsing error and the input is freed, otherwise the return is the same as the parameter.
LilCodeStub* lil_parse_onto_end(LilCodeStub*, const char* src, ...);

//*** Instruction Contexts

enum LilM2nState { LMS_NoM2n, LMS_NoHandles, LMS_Handles };
struct LilInstructionContext;

VMEXPORT unsigned lil_ic_get_num_std_places(LilInstructionContext*);
VMEXPORT LilM2nState lil_ic_get_m2n_state(LilInstructionContext*);
VMEXPORT unsigned lil_ic_get_num_locals(LilInstructionContext*);
VMEXPORT LilType lil_ic_get_local_type(LilInstructionContext*, unsigned);
VMEXPORT LilSig* lil_ic_get_out_sig(LilInstructionContext*);
VMEXPORT LilType lil_ic_get_ret_type(LilInstructionContext*);
VMEXPORT unsigned lil_ic_get_amt_alloced(LilInstructionContext*);

VMEXPORT LilType lil_ic_get_type(LilCodeStub*, LilInstructionContext*, LilOperand*);

// returns the type of an instruction's destination variable _after_ the
// instruction is performed.
// If the instruction has no destination, LT_Void is returned.
// Notice that this type may be different than the type returned by
// lil_ic_get_type for the destination variable, since the latter
// refers to the situation _before_ the instruction.
VMEXPORT LilType lil_instruction_get_dest_type(LilCodeStub*, LilInstruction*,
                                      LilInstructionContext*);


//*** Interogators

// Is a LIL code stub valid?
VMEXPORT bool lil_is_valid(LilCodeStub* cs);

// Return a code stubs entry signature
VMEXPORT LilSig* lil_cs_get_sig(LilCodeStub* cs);

// Return the number of instructions in a code stub
VMEXPORT unsigned lil_cs_get_num_instructions(LilCodeStub* cs);

// Return the number of BBs in a code stub
VMEXPORT unsigned lil_cs_get_num_BBs(LilCodeStub *cs);

// Return the first BB in the stub (from that one can get all BBs using get_next()
VMEXPORT LilBb* lil_cs_get_entry_BB(LilCodeStub* cs);

// Compute the contexts of each instruction in the code stub
// This function must be called to use instruction iterators that track contexts
VMEXPORT void lil_compute_contexts(LilCodeStub* cs);

// Return maximum number of std places used in the stub
VMEXPORT unsigned lil_cs_get_max_std_places(LilCodeStub * cs);

// Return maximum number of locals used in the stub
VMEXPORT unsigned lil_cs_get_max_locals(LilCodeStub * cs);

// Return size of generated code
VMEXPORT void lil_cs_set_code_size(LilCodeStub * cs, size_t size);

// Return size of generated code
VMEXPORT size_t lil_cs_get_code_size(LilCodeStub * cs);

// totally artificial limit; increase if needed
// the max number of predecessors any BB may have
#define MAX_BB_PRED 10


class LilBb {
private:
    // the code stub
    LilCodeStub *cs;

    // since bb code will appear consecutively in the stub,
    // all we need is the first and last instruction
    LilInstruction *start;
    LilInstruction *end;  // if end is NULL, the end of the stub is implied

    // the LIL context right before the first instruction of the BB
    LilInstructionContext* ctxt;

    // successors of this BB in the flowgraph
    LilBb* fallthru;
    LilBb* branch_target;

    // predecessors of this BB in the flowgraph
    unsigned num_pred;
    LilBb *pred[MAX_BB_PRED];
    // for forming BB lists
    LilBb* next;
    // ID; within each stub, IDs will be unique and go from 0 to <num BBs>-1
    int id;
    // private constructor; create BBs by calling init_fg()
    LilBb(LilCodeStub *_cs, LilInstruction *_start,
          LilInstructionContext* ctxt_at_start);

    // private operator new; create BBs using new_bb() only
    void* operator new(size_t sz, tl::MemoryPool& m);
    //MVM
    //add empty destructor to avoid warning
        void operator delete(void * UNREF obj, tl::MemoryPool& UNREF m){
        //do nothing
    };

public:

    // the only way to create new BBs; create a whole FG for a stub
    VMEXPORT static void init_fg(LilCodeStub*);

    // sets the last instruction of the BB
    VMEXPORT void set_last(LilInstruction *i);

    // gets the first instruction
    VMEXPORT LilInstruction* get_first();
    // gets the last instruction
    VMEXPORT LilInstruction* get_last();

    // get the context right before the start of the BB
    VMEXPORT LilInstructionContext* get_context();

    // does this bb contain instruction i?
    VMEXPORT bool contains(LilInstruction *i);

    // get the label of this BB; NULL if no label exists
    VMEXPORT LilLabel get_label();

    // set a fallthrough successor to this bb
    VMEXPORT void set_fallthru(LilBb *);
    // set a branch-target successor to this bb
    VMEXPORT void set_branch_target(LilBb *);

    // get the fallthrough and branch target successors;
    // either of them can be NULL if they don't exist
    VMEXPORT LilBb* get_fallthru();
    VMEXPORT LilBb* get_branch_target();

    // gets the i'th predecessor (NULL if i >= num_pred)
    VMEXPORT LilBb *get_pred(unsigned i);

    // gets the next BB in the list
    VMEXPORT LilBb *get_next();

    // returns whether this BB ends in a return instruction
    // (tailcall implies return!)
    VMEXPORT bool is_ret();

    // the id of this BB
    VMEXPORT int get_id();

    // true if this BB contains calls (which means it may throw exceptions)
    VMEXPORT bool does_calls();

    // true if this BB ends with a call.noret (which means it is probably cold code)
    VMEXPORT bool does_call_noret();

    // find a BB with the specified label; NULL if no such bb exists
    VMEXPORT static LilBb* get_by_label(LilCodeStub *cs, LilLabel l);

    // find a BB which contains the specified instruction; NULL if no such BB exists
    VMEXPORT static LilBb* get_by_instruction(LilCodeStub *cs, LilInstruction *i);

    // print a BB to a stream (does not print the BB's instructions)
    VMEXPORT void print(FILE* out);
};



// Iterate over the instructions in a code stub
class LilInstructionIterator {
 public:
    // Create an iterator from a code stub, if track_contexts then track the context prior to each instruction
    VMEXPORT LilInstructionIterator(LilCodeStub*, bool track_contexts);

    // Create an iterator for a BB in a code stub.
    // NOTE: track_contexts is currently NOT supported for this type of iterator
    VMEXPORT LilInstructionIterator(LilCodeStub*, LilBb*, bool track_contexts);

    // Is iterator past the end of the instructions?
    VMEXPORT bool at_end();
    // Return current instruction
    VMEXPORT LilInstruction* get_current();
    // Return context prior to current instruction, must have created iterator to track contexts
    VMEXPORT LilInstructionContext* get_context();
    // Goto the next instruction
    VMEXPORT void goto_next();

 private:
    LilCodeStub* cs;
    LilBb *bb;  // NULL, unless this is an iterator for a BB
    LilInstruction* cur;
    bool track_ctxt;
    LilInstructionContext* ctxt;
};



// Visitor pattern for instructions
class LilInstructionVisitor {
 protected:
    VMEXPORT LilInstructionVisitor();

 public:
    virtual void label(LilLabel) = 0;
    virtual void locals(unsigned) = 0;
    virtual void std_places(unsigned) = 0;
    virtual void alloc(LilVariable*, unsigned) = 0;
    virtual void asgn(LilVariable*, enum LilOperation, LilOperand*, LilOperand*) = 0;
    virtual void ts(LilVariable*) = 0;
    virtual void handles(LilOperand*) = 0;
    virtual void ld(LilType t, LilVariable* dst, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel, LilLdX) = 0;
    virtual void st(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel, LilOperand* src) = 0;
    virtual void inc(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel) = 0;
    virtual void cas(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel,
                     LilOperand* cmp, LilOperand* src, LilLabel) = 0;
    virtual void j(LilLabel) = 0;
    virtual void jc(LilPredicate, LilOperand*, LilOperand*, LilLabel) = 0;
    virtual void out(LilSig*) = 0;
    virtual void in2out(LilSig*) = 0;
    virtual void call(LilOperand*, LilCallKind) = 0;
    virtual void ret() = 0;
    virtual void push_m2n(Method_Handle method, frame_type current_frame_type, bool handles) = 0;
    virtual void m2n_save_all() = 0;
    virtual void pop_m2n() = 0;
    virtual void print(char *, LilOperand *) = 0;
};


// LilInstructionVisitor_Default provides empty implementations for the
// abstract functions in LilInstructionVisitor.  It is a convenient base
// class for scanners that need to act only on a few instructions.
class LilInstructionVisitor_Default : public LilInstructionVisitor {
 protected:
    LilInstructionVisitor_Default():
        LilInstructionVisitor() {}

 public:
    void label(LilLabel) {}
    void locals(unsigned) {}
    void std_places(unsigned) {}
    void alloc(LilVariable*, unsigned) {}
    void asgn(LilVariable*, enum LilOperation, LilOperand*, LilOperand*) {}
    void ts(LilVariable*) {}
    void handles(LilOperand*) {}
    void ld(LilType UNREF t, LilVariable* UNREF dst, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel, LilLdX) {}
    void st(LilType UNREF t, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel, LilOperand* UNREF src) {}
    void inc(LilType UNREF t, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel) {}
    void cas(LilType UNREF t, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel,
             LilOperand* UNREF cmp, LilOperand* UNREF src, LilLabel) {}
    void j(LilLabel) {}
    void jc(LilPredicate, LilOperand*, LilOperand*, LilLabel) {}
    void out(LilSig*) {}
    void in2out(LilSig*) {}
    void call(LilOperand*, LilCallKind) {}
    void ret() {}
    void push_m2n(Method_Handle UNREF method, frame_type UNREF current_frame_type, bool UNREF handles) {}
    void m2n_save_all() {}
    void pop_m2n() {}
    void print(char *, LilOperand *) {}
};


// Visit instruction using a visitor
VMEXPORT void lil_visit_instruction(LilInstruction*, LilInstructionVisitor*);


// Return variable's kind
VMEXPORT LilVariableKind lil_variable_get_kind(LilVariable* v);
// Return variable's index
VMEXPORT unsigned lil_variable_get_index(LilVariable* v);
// Are two variables the same
VMEXPORT bool lil_variable_is_equal(LilVariable* v1, LilVariable* v2);

// Is operand an immediate?
VMEXPORT bool lil_operand_is_immed(LilOperand* o);
// If operand is an immediate return the immediate value
VMEXPORT POINTER_SIZE_INT lil_operand_get_immed(LilOperand* o);
// If operand is not an immediate return the variable it represents
VMEXPORT LilVariable* lil_operand_get_variable(LilOperand* o);

// Return a signature's calling convention
VMEXPORT LilCc lil_sig_get_cc(LilSig* sig);
// Return whether this signature is arbitrary
VMEXPORT bool lil_sig_is_arbitrary(LilSig *sig);
// Return the number of arguments for a signature (0 if arbitrary)
VMEXPORT unsigned lil_sig_get_num_args(LilSig* sig);
// Return the num-th argument type of a signature (zero based) (undefined if signature is arbitrary)
VMEXPORT LilType lil_sig_get_arg_type(LilSig* sig, unsigned num);
// Return the return type of a signature
VMEXPORT LilType lil_sig_get_ret_type(LilSig* sig);

// Is an operation binary?
VMEXPORT bool lil_operation_is_binary(enum LilOperation op);
// Is a predicate binary?
VMEXPORT bool lil_predicate_is_binary(enum LilPredicate c);
// Is a predicate signed?
VMEXPORT bool lil_predicate_is_signed(LilPredicate c);

//*** Printers

// Print a type to a stream
VMEXPORT void lil_print_type(FILE*, LilType);
// Print a signature to a stream
VMEXPORT void lil_print_sig(FILE*, LilSig*);
// Print an instruction to a stream
VMEXPORT void lil_print_instruction(FILE*, LilInstruction* i);
// Print a code stub to a stream
VMEXPORT void lil_print_code_stub(FILE*, LilCodeStub* cs);
// Print just the entry of a code stub to a stream
VMEXPORT void lil_print_entry(FILE*, LilCodeStub* cs);

//*** Freers

// Return all resources associated with a code stub
// Post: code stub no longer usable
VMEXPORT void lil_free_code_stub(LilCodeStub* cs);

//*** Function pointer utilities

VMEXPORT GenericFunctionPointer lil_npc_to_fp(NativeCodePtr);

#endif // _LIL_H_
