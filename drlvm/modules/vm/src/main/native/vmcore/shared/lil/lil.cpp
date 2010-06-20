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
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "lil.h"
#include "open/vm_type_access.h"
#include "open/vm_method_access.h"
#include "nogc.h"

// Forward decs of local functions

static LilInstruction* lil_find_label(LilCodeStub*, LilLabel);

////////////////////////////////////////////////////////////////////////////////////////
// The LIL internal representation

struct LilSig {
    LilCc cc;
    bool arbitrary;           // If true then num_arg_types must be 0
    unsigned num_arg_types;   // Must be 0 if arbitrary is true
    LilType* arg_types;
    enum LilType ret_type;
};

struct LilCond {
    enum LilPredicate tag;
    LilOperand o1, o2;
};

enum LilInstructionTag {
    LIT_Label, LIT_Locals, LIT_StdPlaces, LIT_Alloc, LIT_Asgn, LIT_Ts, LIT_Handles, LIT_Ld, LIT_St, LIT_Inc, LIT_Cas,
    LIT_J, LIT_Jc, LIT_Out, LIT_In2Out, LIT_Call, LIT_Ret,
    LIT_PushM2N, LIT_M2NSaveAll, LIT_PopM2N, LIT_Print
};

struct LilInstruction {
    enum LilInstructionTag tag;
    union {
        struct {
            LilLabel l;
            LilInstructionContext* ctxt;  // Computed lazily
        } label;
        unsigned locals;
        unsigned std_places;
        struct {
            LilVariable dst;
            unsigned num_bytes;
        } alloc;
        struct {
            enum LilOperation op;
            LilVariable dst;
            LilOperand o1, o2;
        } asgn;
        LilVariable ts;
        LilOperand handles;
        // Both ld, st, inc, & case use ldst
        // operand is dst for ld, src for st, unused for inc, src for case
        // cmp and l are used only for cas
        // [base,scale,index,offset] is src for ld, dst for st
        // base present iff is_base
        // index present iff is_index
        struct {
            LilType t;
            bool is_base, is_index;
            LilVariable base, index;
            LilOperand operand;
            unsigned scale;
            POINTER_SIZE_SINT offset;
            LilAcqRel acqrel;
            LilLdX extend;
            LilOperand compare;
            LilLabel l;
        } ldst;
        LilLabel j;
        struct {
            LilCond c;
            LilLabel l;
        } jc;
        LilSig out;
        struct {
            LilCallKind k;
            LilOperand target;
        } call;
        struct {
            Method_Handle method;
            frame_type current_frame_type;
            bool handles;
        } push_m2n;
        struct {
            char *str;
            LilOperand arg;
        } print;
    } u;
    struct LilInstruction* next;
};


enum LilCodeStubContexts { LCSC_NotComputed, LCSC_Computed, LCSC_Error };

struct LilCodeStub {
    tl::MemoryPool* my_memory;
    unsigned cur_gen_label;
    unsigned num_std_places;
    LilSig sig;
    LilInstruction* is;
    LilCodeStubContexts ctxt_state;
    unsigned num_is, max_std_places, max_locals;
    LilInstructionContext* init_ctxt; // Computed lazily

    // size of generated code
    size_t compiled_code_size;

    // a list of BBs in this code stub; null if the FG has not been initializsed; computed lazily
    LilBb* bb_list_head;
    unsigned num_bbs;
};



//*** Freers

VMEXPORT void lil_free_code_stub(LilCodeStub* cs)
{
    delete cs->my_memory;
}

//*** Freers - end


////////////////////////////////////////////////////////////////////////////////////
// The LIL Parser

// The LIL parser is loosely designed as a scannerless recursive-descent parser.
// The various functions take a pointer to a source string pointer and update
// the pointer as they parse.  Most functions are designed to advance the pointer
// only by token amounts.  Functions that parse something and return a pointer
// to the structure, return NULL on failure.  Other functions that parse directly
// a structure, take a pointer to the structure to parse into, and return a boolean
// to indicate success; the latter also include parsing primitive types.
// Parsing functions other than for code stubs do not cleanup allocated memory as
// this is arena deallocated in parse_code_stub.

// Here are some character classes used in various parts of the parser

#define alpha(c) (((c)>='a' && (c)<='z') || ((c)>='A' && (c)<='Z') || (c)=='_')
#define num(c) ((c)>='0' && (c) <='9')
#define hexnum(c) (num(c) || ((c)>='a' && (c)<='f') || ((c)>='A' && (c)<='F'))
#define alphanum(c) (alpha(c) || num(c))

static void error(const char** src, const char* err1, const char* err2="")
{
    FILE* err_f = stderr;
    fprintf(err_f, "lil parse error: %s%s\n\t", err1, err2);
    unsigned count=0;
    const char* s =*src;
    while (s[0] && count++<70) {
        fputc((s[0]<=' ' ? ' ' : s[0]), err_f);
        s++;
    }
    fputc('\n', err_f);
    fflush(err_f);
}

// Skip over any white space
static void lil_skip_ws(const char** src)
{
    while ((*src)[0]==' ' || (*src)[0]=='\n' || (*src)[0]=='\r' || (*src)[0]=='\t' || (*src)[0]=='/') {
        if ((*src)[0]=='/')
            if ((*src)[1]=='/')
                while ((*src)[0] && (*src)[0]!='\n') ++*src;
            else
                return;
        else
            ++*src;
    }
}

struct LilVarArgs {
    unsigned current;
    va_list val;
};

static char lil_parse_percent(const char** src, LilVarArgs* va)
{
    if ((*src)[0]!='%') { error(src, "expecting %", ""); return '\0'; }
    unsigned i=1, v=0;
    while (num((*src)[i]))
        v = 10*v+((*src)[i++]-'0');
    if (v!=va->current) { error(src, "%s not numbered sequentially", ""); return '\0'; }
    va->current++;
    *src += i+1;
    return (*src)[-1];
}

// Check and skip over a specific "keyword"
// This function does not change *src on failure
// (except to skip whitespace),
// and this is needed in some code below
static bool lil_parse_kw_no_error(const char** src, const char* kw)
{
    lil_skip_ws(src);
    unsigned c;
    for (c=0; kw[c]; c++)
        if ((*src)[c]!=kw[c]) return false;
    // If the keyword could be an identifier or number then check that the source is
    // not a longer identifier or number
    if (alphanum(kw[c-1]) && alphanum((*src)[c])) return false;
    *src += c;
    return true;
}

static bool lil_parse_kw(const char** src, const char* kw)
{
    bool res = lil_parse_kw_no_error(src, kw);
    if (!res) error(src, "expected ", kw);
    return res;
}

// Parse a calling convention
static bool lil_parse_cc(const char** src, LilCc* cc)
{
    lil_skip_ws(src);
    switch ((*src)[0]) {
    case 'j': // jni
        if (!lil_parse_kw(src, "jni")) return false;
        *cc = LCC_Jni;
        return true;
    case 'm': // managed
        if (!lil_parse_kw(src, "managed")) return false;
        *cc = LCC_Managed;
        return true;
    case 'p': // platform
        if (!lil_parse_kw(src, "platform")) return false;
        *cc = LCC_Platform;
        return true;
    case 'r': // rth
        if (!lil_parse_kw(src, "rth")) return false;
        *cc = LCC_Rth;
        return true;
    case 's': // stdcall
        if (!lil_parse_kw(src, "stdcall")) return false;
        *cc = LCC_StdCall;
        return true;
    default:
        error(src, "bad calling convention", "");
        return false;
    }
}

// Parse a type
// If ret is true then void is allowed otherwise not
static bool lil_parse_type(const char** src, LilType* t, bool ret)
{
    lil_skip_ws(src);
    switch ((*src)[0]) {
    case 'f': // f4, f8
        if ((*src)[1]=='4') *t = LT_F4;
        else if ((*src)[1]=='8') *t = LT_F8;
        else { error(src, "expecting f4 or f8", ""); return false; }
        if (alphanum((*src)[2])) { error(src, "expecting f4 or f8", ""); return false; }
        *src += 2;
        return true;
    case 'g': // g1, g2, g4, g8
        if ((*src)[1]=='1') *t = LT_G1;
        else if ((*src)[1]=='2') *t = LT_G2;
        else if ((*src)[1]=='4') *t = LT_G4;
        else if ((*src)[1]=='8') *t = LT_G8;
        else { error(src, "expecting g1, g2, g4, or g8", ""); return false; }
        if (alphanum((*src)[2])) { error(src, "expecting g1, g2, g4, or g8", ""); return false; }
        *src += 2;
        return true;
    case 'r': // refmethod_get_name(meth)
        if (!lil_parse_kw(src, "ref")) return false;
        *t = LT_Ref;
        return true;
    case 'p': // pint
        if (!lil_parse_kw(src, "pint")) return false;
        *t = LT_PInt;
        return true;
    case 'v': // void
        if (!ret) { error(src, "cannot have void type", ""); return false; }
        if (!lil_parse_kw(src, "void")) return false;
        *t = LT_Void;
        return true;
    default:
        error(src, "bad type", "");
        return false;
    }
}

static LilType type_info_to_lil_type(Type_Info_Handle tih, bool handles)
{
    if (type_info_is_managed_pointer(tih)) {
        return LT_PInt;
    }
    VM_Data_Type dt = type_info_get_type(tih);
    switch (dt) {
    case VM_DATA_TYPE_INT8:
    case VM_DATA_TYPE_UINT8: return LT_G1;
    case VM_DATA_TYPE_INT16:
    case VM_DATA_TYPE_UINT16: return LT_G2;
    case VM_DATA_TYPE_INT32:
    case VM_DATA_TYPE_UINT32: return LT_G4;
    case VM_DATA_TYPE_INT64:
    case VM_DATA_TYPE_UINT64: return LT_G8;
    case VM_DATA_TYPE_INTPTR:
    case VM_DATA_TYPE_UINTPTR: return LT_PInt;
    case VM_DATA_TYPE_F8: return LT_F8;
    case VM_DATA_TYPE_F4: return LT_F4;
    case VM_DATA_TYPE_BOOLEAN: return LT_G1;
    case VM_DATA_TYPE_CHAR: return LT_G2;
    case VM_DATA_TYPE_CLASS:
    case VM_DATA_TYPE_ARRAY: return (handles ? LT_PInt : LT_Ref);
    case VM_DATA_TYPE_VOID: return LT_Void;
    case VM_DATA_TYPE_VALUE:{
        // ? 20030613: I really don't want this code here, but for now...
        Class_Handle UNUSED ch = type_info_get_class(tih);
        assert(ch);
        LDIE(52, "Unexpected data type");
    }
    case VM_DATA_TYPE_MP:
    case VM_DATA_TYPE_UP:
        return LT_PInt;
    case VM_DATA_TYPE_STRING:
    case VM_DATA_TYPE_INVALID:
    default: DIE(("Unknown data type")); for(;;);
    }
}

// Parse a signature (cc:Ts:RT)
// Currently this function can only handle a maximum number of
// argument types
#define MAX_ARG_TYPES 20
static bool lil_parse_sig(const char** src, tl::MemoryPool* mem, LilVarArgs* va, LilSig* s)
{
    if (!lil_parse_cc(src, &(s->cc))) return false;
    if (!lil_parse_kw(src, ":")) return false;
    lil_skip_ws(src);
    if ((*src)[0]=='%') {
        char specifier = lil_parse_percent(src, va);
        bool jni;
        switch (specifier) {
        case 'm':
            jni = false;
            break;
        case 'j':
            jni = true;
            break;
        default:
            if (specifier) error(src, "bad specifier for signature", "");
            return false;
        }
        Method_Handle m = va_arg(va->val, Method_Handle);
        Method_Signature_Handle sig = method_get_signature(m);
        unsigned num_method_args = method_args_get_number(sig);
        unsigned num_args = num_method_args;
        if (jni) {
            num_args++;
            if (method_is_static(m)) num_args++;
        }
        s->arbitrary = false;
        s->num_arg_types = num_args;
        s->arg_types = (LilType*)mem->alloc(num_args*sizeof(LilType));
        unsigned cur=0;
        if (jni) {
            s->arg_types[cur++] = LT_PInt;
            if (method_is_static(m)) s->arg_types[cur++] = LT_PInt;
        }
        for(unsigned i=0; i<num_method_args; i++)
            s->arg_types[cur++] = type_info_to_lil_type(method_args_get_type_info(sig, i), jni);
        s->ret_type = type_info_to_lil_type(method_ret_type_get_type_info(sig), jni);
    } else if ((*src)[0]=='a') {
        if (!lil_parse_kw(src, "arbitrary")) return false;
        s->arbitrary = true;
        s->num_arg_types = 0;  // This is important to the validity checks
        s->arg_types = NULL;
        s->ret_type = LT_Void;
    } else {
        s->arbitrary = false;
        if ((*src)[0]!=':') {
            LilType ts[MAX_ARG_TYPES];
            unsigned cur = 0;
            for(;;) {
                assert(cur<MAX_ARG_TYPES);
                if (!lil_parse_type(src, ts+cur, false)) return false;
                ++cur;
                lil_skip_ws(src);
                if ((*src)[0]==':') break;
                else if ((*src)[0]!=',') { error(src, "expecting , or :", ""); return false; }
                ++*src;
            }
            s->num_arg_types = cur;
            s->arg_types = (LilType*)mem->alloc(cur*sizeof(LilType));
            for(unsigned i=0; i<cur; i++) s->arg_types[i] = ts[i];
        } else {
            s->num_arg_types = 0;
            s->arg_types = NULL;
        }
        if (!lil_parse_kw(src, ":")) return false;
        if (!lil_parse_type(src, &(s->ret_type), true)) return false;
    }
    return true;
}

// Parse an immediate string
static bool lil_parse_string(const char** src, char** str)
{
    static char buffer[1000];
    lil_skip_ws(src);
    if ((*src)[0] != '\'')
        return false;
    (*src)++;  // skip opening quote
    int len;
    for (len=0; **src != '\'' && **src != '\0';  ++*src, ++len) {
        if (len >= 1000) {
            error(src, "string too long (more than 1000 chars)");
            return false;
        }
        buffer[len] = **src;
    }
    if (**src == '\0') {
        error(src, "open string");
        return false;
    }
    // skip closing quote
    ++*src;
    buffer[len] = '\0';
    *str = (char *) malloc_fixed_code_for_jit(strlen(buffer) + 1, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    strcpy(*str, buffer);
    return true;
}


// Parse an immediate
static bool lil_parse_number(const char** src, LilVarArgs* va, POINTER_SIZE_INT* num)
{
    lil_skip_ws(src);
    if ((*src)[0]=='%') {
        char specifier = lil_parse_percent(src, va);
        if (specifier=='i') {
            *num = va_arg(va->val, POINTER_SIZE_INT);
            return true;
        }
        else {
            if (specifier) error(src, "bad specifier for immediate", "");
            return false;
        }
    } else if ((*src)[0]=='0' && (*src)[1]=='x') {
        *num=0;
        unsigned c=2;
        while (hexnum((*src)[c])) {
            // NB the following statement is dependent upon ASCII ordering
            *num = *num * 16 + ((*src)[c]>='a' ? (*src)[c]-'a'+10 : (*src)[c]>='A' ? (*src)[c]-'A'+10 : (*src)[c]-'0');
            c++;
        }
        if (c==2) { error(src, "0x must be followed by at least one hexidigit", ""); return false; }
        *src += c;
        return true;
    } else {
        *num=0;
        unsigned c=0;
        while (num((*src)[c])) {
            *num = *num * 10 + (*src)[c]-'0';
            c++;
        }
        if (c==0) { error(src, "expecting number", ""); return false; }
        *src += c;
        return true;
    }
}

// Parse a label
static LilLabel lil_parse_label(const char** src, unsigned* cur_gen_label, tl::MemoryPool* mem)
{
    lil_skip_ws(src);
    if ((*src)[0]=='%') {
        unsigned index;
        switch ((*src)[1]) {
        case 'g':
            index = ++*cur_gen_label;
            break;
        case 'p':
            index = *cur_gen_label;
            break;
        case 'n':
            index = *cur_gen_label + 1;
            break;
        case 'o':
            index = *cur_gen_label + 2;
            break;
        default:
            error(src, "bad % specifier in label", "");
            return NULL;
        }
        char* buf = (char*)mem->alloc(17);
        sprintf(buf, "$%x", index);
        *src += 2;
        return buf;
    }
    if (!alpha((*src)[0])) { error(src, "null label", ""); return NULL; }
    unsigned c=1;
    while (alphanum((*src)[c])) c++;
    char* buf = (char*)mem->alloc(c+1);
    for(unsigned i=0; i<c; i++) buf[i] = (*src)[i];
    buf[c] = '\0';
    *src += c;
    return buf;
}

// Parse a variable
static bool lil_parse_variable(const char** src, LilVarArgs* va, LilVariable* v)
{
    unsigned start;
    lil_skip_ws(src);
    switch ((*src)[0]) {
    case 'i': v->tag = LVK_In; start = 1; break;
    case 'l': v->tag = LVK_Local; start = 1; break;
    case 'o': v->tag = LVK_Out; start = 1; break;
    case 's':
        if ((*src)[1]!='p') { error(src, "expecting variable", ""); return false; }
        v->tag = LVK_StdPlace;
        start = 2;
        break;
    case 'r':
        if (alphanum((*src)[1])) { error(src, "expecting variable", ""); return false; }
        v->tag = LVK_Ret;
        v->index = 0;
        ++*src;
        return true;
    default:
        error(src, "expecting variable", "");
        return false;
    }
    if ((*src)[start]=='%') {
        *src += start;
        char specifier = lil_parse_percent(src, va);
        if (specifier=='i') {
            v->index = va_arg(va->val, int);
            return true;
        } else {
            if (specifier) error(src, "bad specifier for variable index", "");
            return false;
        }
    } else {
        unsigned c=0;
        v->index = 0;
        while (num((*src)[start+c])) {
            v->index = v->index*10 + (*src)[start+c]-'0';
            c++;
        }
        if (c==0 || alpha((*src)[start+c])) { error(src, "bad variable", ""); return false; }
        *src += start+c;
        return true;
    }
}


// Parse an operand
static bool lil_parse_operand(const char** src, LilVarArgs* va, LilOperand* o)
{
    lil_skip_ws(src);
    if (((*src)[0]>='0' && (*src)[0]<='9') || (*src)[0]=='%') {
        o->is_immed = true;
        if (!lil_parse_number(src, va, &(o->val.imm))) return false;
    } else {
        o->is_immed = false;
        if (!lil_parse_variable(src, va, &(o->val.var))) return false;
    }
    lil_skip_ws(src);
    if ((*src)[0]==':') {
        o->has_cast = true;
        ++*src;
        if (!lil_parse_type(src, &o->t, false)) return false;
    } else {
        o->has_cast = false;
    }
    return true;
}

// Parse a condition
static bool lil_parse_cond(const char** src, LilVarArgs* va, LilCond* c)
{
    if (!lil_parse_operand(src, va, &(c->o1))) return false;
    lil_skip_ws(src);
    if ((*src)[0]=='=')
        c->tag = LP_Eq, ++*src;
    else if ((*src)[0]=='!' && (*src)[1]=='=')
        c->tag = LP_Ne, *src += 2;
    else if ((*src)[0]=='<')
        if ((*src)[1]=='=')
            if ((*src)[2]=='u')
                if (alphanum((*src)[3]))
                    { error(src, "expecting predicate", ""); return false; }
                else
                    c->tag = LP_Ule, *src += 3;
            else
                c->tag = LP_Le, *src += 2;
        else if ((*src)[1]=='u')
            if (alphanum((*src)[2]))
                { error(src, "expecting predicate", ""); return false; }
            else
                c->tag = LP_Ult, *src += 2;
        else
            c->tag = LP_Lt, ++*src;
    else
        { error(src, "expecting predicate", ""); return false; }
    if (!lil_parse_operand(src, va, &(c->o2))) return false;
    // Fixup some special cases
    if (!c->o1.is_immed && c->o2.is_immed && c->o2.val.imm==0)
        if (c->tag==LP_Eq)
            c->tag = LP_IsZero;
        else if (c->tag==LP_Ne)
            c->tag = LP_IsNonzero;
    return true;
}

static bool lil_parse_plusminus(const char** src, int* sign)
{
    lil_skip_ws(src);
    if ((*src)[0]=='+') {
        *sign = +1;
        ++*src;
        return true;
    } else if ((*src)[0]=='-') {
        *sign = -1;
        ++*src;
        return true;
    } else {
        error(src, "expecting + or -", "");
        return false;
    }
}

// Parse an address (part of load or store)
static bool lil_parse_address(const char** src, LilVarArgs* va, LilInstruction* i)
{
    int sign = +1;
    if (!lil_parse_kw(src, "[")) return false;
    lil_skip_ws(src);
    if (alpha((*src)[0])) {
        if (!lil_parse_variable(src, va, &(i->u.ldst.base))) return false;
        if (!lil_parse_plusminus(src, &sign)) return false;
        i->u.ldst.is_base = true;
    } else {
        i->u.ldst.is_base = false;
    }
    // Parse a number, this could be the scale or the immediate,
    // which will be determined by what follows
    POINTER_SIZE_INT n;
    if (!lil_parse_number(src, va, &n)) return false;
    lil_skip_ws(src);
    if (lil_parse_kw_no_error(src, "*")) {
        if (sign<0) { error(src, "cannot subtract scaled index", ""); return false; }
        i->u.ldst.is_index = true;
        i->u.ldst.scale = (unsigned) n;
        if (!lil_parse_variable(src, va, &(i->u.ldst.index))) return false;
        if (!lil_parse_plusminus(src, &sign)) return false;
        if (!lil_parse_number(src, va, &n)) return false;
    } else {
        i->u.ldst.is_index = false;
        i->u.ldst.scale = 0;
    }
    i->u.ldst.offset = sign * (POINTER_SIZE_SINT) n;
    if (!lil_parse_kw(src, ":")) return false;
    if (!lil_parse_type(src, &i->u.ldst.t, false)) return false;
    if (lil_parse_kw_no_error(src, ",")) {
        if (lil_parse_kw_no_error(src, "acquire")) {
            i->u.ldst.acqrel = LAR_Acquire;
        } else if (lil_parse_kw_no_error(src, "release")) {
            i->u.ldst.acqrel = LAR_Release;
        } else {
            error(src, "expecting acquire or release", "");
            return false;
        }
    } else {
        i->u.ldst.acqrel = LAR_None;
    }
    if (!lil_parse_kw(src, "]")) return false;
    return true;
}

static bool lil_parse_load_extend(const char** src, LilInstruction* i)
{
    if (lil_parse_kw_no_error(src, ",")) {
        if (lil_parse_kw_no_error(src, "sx")) {
            i->u.ldst.extend = LLX_Sign;
        } else if (lil_parse_kw_no_error(src, "zx")) {
            i->u.ldst.extend = LLX_Zero;
        } else {
            error(src, "expecting sx or zx", "");
            return false;
        }
    } else {
        i->u.ldst.extend = LLX_None;
    }
    return true;
}

// Parse an instruction
static LilInstruction* lil_parse_instruction(const char** src, tl::MemoryPool* mem, unsigned* cgl, LilVarArgs* va, LilSig* entry_sig)
{
    LilInstruction* i = (LilInstruction*)mem->alloc(sizeof(LilInstruction));
    lil_skip_ws(src);
    // Look ahead at characters until the instruction form is determined
    // Then parse that form
    // Not the most maintainable code, but it keeps the design simple for now
    switch ((*src)[0]) {
    case ':': // :label
        ++*src;
        i->tag = LIT_Label;
        i->u.label.l = lil_parse_label(src, cgl, mem);
        if (!i->u.label.l) return NULL;
        i->u.label.ctxt = NULL;
        break;
    case 'a':  // alloc
        {
            i->tag = LIT_Alloc;
            if (!lil_parse_kw(src, "alloc")) return NULL;
            if (!lil_parse_variable(src, va, &(i->u.alloc.dst))) return NULL;
            if (!lil_parse_kw(src, ",")) return NULL;
            POINTER_SIZE_INT n;
            if (!lil_parse_number(src, va, &n)) return NULL;
            i->u.alloc.num_bytes = (unsigned) n;
            break;
        }
    case 'c': // call, call.noret, cas
        if ((*src)[1] && (*src)[2]=='s') {
            i->tag = LIT_Cas;
            if (!lil_parse_kw(src, "cas")) return NULL;
            if (!lil_parse_address(src, va, i)) return NULL;
            if (!lil_parse_kw(src, "=")) return NULL;
            if (!lil_parse_operand(src, va, &i->u.ldst.compare)) return NULL;
            if (!lil_parse_kw(src, ",")) return NULL;
            if (!lil_parse_operand(src, va, &i->u.ldst.operand)) return NULL;
            if (!lil_parse_kw(src, ",")) return NULL;
            i->u.ldst.l = lil_parse_label(src, cgl, mem);
            if (!i->u.ldst.l) return NULL;
            break;
        } else if ((*src)[1] && (*src)[2] && (*src)[3] && (*src)[4]=='.') {
            i->tag = LIT_Call;
            if (!lil_parse_kw(src, "call.noret")) return NULL;
            i->u.call.k = LCK_CallNoRet;
        } else {
            i->tag = LIT_Call;
            if (!lil_parse_kw(src, "call")) return NULL;
            i->u.call.k = LCK_Call;
        }
        if (!lil_parse_operand(src, va, &(i->u.call.target))) return NULL;
        break;
    case 'h': // handles
        i->tag = LIT_Handles;
        if (!lil_parse_kw(src, "handles")) return NULL;
        if (!lil_parse_kw(src, "=")) return NULL;
        if (!lil_parse_operand(src, va, &(i->u.handles))) return NULL;
        break;
    case 'i': // =, in2out, inc
        if (num((*src)[1]) || (*src)[1]=='%') goto asgn;
        // in2out, inc
        if ((*src)[1]=='n' && (*src)[2]=='c') {
            // inc
            i->tag = LIT_Inc;
            if (!lil_parse_kw(src, "inc")) return NULL;
            if (!lil_parse_address(src, va, i)) return NULL;
        } else {
            // in2out
            if (entry_sig->arbitrary) { error(src, "in2out in an arbitrary code stub", ""); return NULL; }
            i->tag = LIT_In2Out;
            if (!lil_parse_kw(src, "in2out")) return NULL;
            if (!lil_parse_cc(src, &(i->u.out.cc))) return NULL;
            i->u.out.arbitrary = false;
            i->u.out.num_arg_types = entry_sig->num_arg_types;
            i->u.out.arg_types = entry_sig->arg_types;
            if (!lil_parse_kw(src, ":")) return NULL;
            if (!lil_parse_type(src, &(i->u.out.ret_type), true)) return NULL;
        }
        break;
    case 'j': // j, jc
        if ((*src)[1]=='c') { // jc
            i->tag = LIT_Jc;
            if (!lil_parse_kw(src, "jc")) return NULL;
            if (!lil_parse_cond(src, va, &(i->u.jc.c))) return NULL;
            if (!lil_parse_kw(src, ",")) return NULL;
            i->u.jc.l = lil_parse_label(src, cgl, mem);
            if (!i->u.jc.l) return NULL;
        } else { // j
            i->tag = LIT_J;
            if (!lil_parse_kw(src, "j")) return NULL;
            i->u.j = lil_parse_label(src, cgl, mem);
            if (!i->u.j) return NULL;
        }
        break;
    case 'l': // =, locals, ld
        if (num((*src)[1]) || (*src)[1]=='%') {
            goto asgn;
        } else if ((*src)[1]=='d') {
            // ld
            i->tag = LIT_Ld;
            if (!lil_parse_kw(src, "ld")) return NULL;
            if (!lil_parse_variable(src, va, &(i->u.ldst.operand.val.var))) return NULL;
            if (!lil_parse_kw(src, ",")) return NULL;
            if (!lil_parse_address(src, va, i)) return NULL;
            if (!lil_parse_load_extend(src, i)) return NULL;
        } else {
            // locals
            POINTER_SIZE_INT n;
            i->tag = LIT_Locals;
            if (!lil_parse_kw(src, "locals")) return NULL;
            if (!lil_parse_number(src, va, &n)) return NULL;
            i->u.locals = (unsigned) n;
        }
        break;
    case 'm': // m2n_save_all
        if (!lil_parse_kw(src, "m2n_save_all")) return NULL;
        i->tag = LIT_M2NSaveAll;
        break;
    case 'o': // =, out
        if (num((*src)[1]) || (*src)[1]=='%') goto asgn;
        // out
        i->tag = LIT_Out;
        if (!lil_parse_kw(src, "out")) return NULL;
        if (!lil_parse_sig(src, mem, va, &(i->u.out))) return NULL;
        break;
    case 'p': // push_m2n, pop_m2n, print
        if ((*src)[1]=='u') {
            // push_m2n
            i->tag = LIT_PushM2N;
            if (!lil_parse_kw(src, "push_m2n")) return NULL;
            i->u.push_m2n.method = NULL;
            i->u.push_m2n.current_frame_type = FRAME_UNKNOWN;
            POINTER_SIZE_INT n;
            POINTER_SIZE_INT ft;
            if (!lil_parse_number(src, va, &n))
                return NULL;
            else
                i->u.push_m2n.method = (Method_Handle) n;
            if (!lil_parse_kw(src, ",")) return NULL;
            lil_skip_ws(src);
            if (!lil_parse_number(src, va, &ft))
                return NULL;
            else
                i->u.push_m2n.current_frame_type = (frame_type)ft;
            lil_skip_ws(src);
            if ((*src)[0]!=';') {
                if (!lil_parse_kw(src, ",")) return NULL;
                if (!lil_parse_kw(src, "handles")) return NULL;
                i->u.push_m2n.handles = true;
            } else {
                i->u.push_m2n.handles = false;
            }
        }
        else if ((*src)[1]=='o') {
            // pop_m2n
            i->tag = LIT_PopM2N;
            if (!lil_parse_kw(src, "pop_m2n")) return NULL;
        }
        else {
            // print
            i->tag = LIT_Print;
            if (!lil_parse_kw(src, "print"))
                return NULL;
            lil_skip_ws(src);
            if ((*src)[0] == '\'') {
                // immediate string
                if (!lil_parse_string(src, &i->u.print.str))
                    return NULL;
            }
            else {
                // look for string address
                POINTER_SIZE_INT n;
                if (!lil_parse_number(src, va, &n))
                    return NULL;
                else
                    i->u.print.str = (char *) n;
            }
            lil_skip_ws(src);
            if ((*src)[0] == ';') {
                // create a dummy immediate 0 operand
                i->u.print.arg.is_immed = true;
                i->u.print.arg.val.imm = 0;
                i->u.print.arg.has_cast = false;
                i->u.print.arg.t = LT_Ref;
            }
            else {
                if (!lil_parse_kw(src, ",") ||
                    !lil_parse_operand(src, va, &i->u.print.arg))
                    return NULL;
            }
        }
        break;
    case 'r': // =, ret
        if ((*src)[1]!='e') goto asgn;
        // ret
        i->tag = LIT_Ret;
        if (!lil_parse_kw(src, "ret")) return NULL;
        break;
    case 's': // =, std_places, st
        if ((*src)[1]=='p') goto asgn;
        if ((*src)[1] && (*src)[2]=='d') {
            // std_places
            i->tag = LIT_StdPlaces;
            if (!lil_parse_kw(src, "std_places")) return NULL;
            POINTER_SIZE_INT n;
            if (!lil_parse_number(src, va, &n))
                return NULL;
            else
                i->u.std_places = (unsigned) n;
        } else {
            // st
            i->tag = LIT_St;
            if (!lil_parse_kw(src, "st")) return NULL;
            if (!lil_parse_address(src, va, i)) return NULL;
            if (!lil_parse_kw(src, ",")) return NULL;
            if (!lil_parse_operand(src, va, &(i->u.ldst.operand))) return NULL;
        }
        break;
    case 't': // tailcall
        i->tag = LIT_Call;
        i->u.call.k = LCK_TailCall;
        if (!lil_parse_kw(src, "tailcall")) return NULL;
        if (!lil_parse_operand(src, va, &(i->u.call.target))) return NULL;
        break;
    default:
        error(src, "expecting instruction", "");
        return NULL;
    asgn: // =, v=ts
        i->tag = LIT_Asgn;
        if (!lil_parse_variable(src, va, &(i->u.asgn.dst))) return NULL;
        if (!lil_parse_kw(src, "=")) return NULL;
        lil_skip_ws(src);
        // At this point there is either ts, a unary op, or an operand
        if ((*src)[0]=='t') {
            i->tag = LIT_Ts;
            i->u.ts = i->u.asgn.dst;
            if (!lil_parse_kw(src, "ts")) return NULL;
        } else if ((*src)[0]=='n' || ((*src)[0]=='s' && (*src)[1]=='x') || (*src)[0]=='z') {
            // Unary operation
            // This code relies on parse_kw not changing src on failure since white space is skipped
            if (lil_parse_kw_no_error(src, "not")) i->u.asgn.op = LO_Not;
            else if (lil_parse_kw_no_error(src, "neg")) i->u.asgn.op = LO_Neg;
            else if (lil_parse_kw_no_error(src, "sx1")) i->u.asgn.op = LO_Sx1;
            else if (lil_parse_kw_no_error(src, "sx2")) i->u.asgn.op = LO_Sx2;
            else if (lil_parse_kw_no_error(src, "sx4")) i->u.asgn.op = LO_Sx4;
            else if (lil_parse_kw_no_error(src, "zx1")) i->u.asgn.op = LO_Zx1;
            else if (lil_parse_kw_no_error(src, "zx2")) i->u.asgn.op = LO_Zx2;
            else if (lil_parse_kw_no_error(src, "zx4")) i->u.asgn.op = LO_Zx4;
            else { error(src, "expecting unary operation", ""); return NULL; }
            if (!lil_parse_operand(src, va, &(i->u.asgn.o1))) return NULL;
        } else {
            // Operand
            if (!lil_parse_operand(src, va, &(i->u.asgn.o1))) return NULL;
            // Now the statement can either end or have a binary op followed by an operand
            lil_skip_ws(src);
            if ((*src)[0]!=';') {
                if ((*src)[0]=='+') i->u.asgn.op = LO_Add, ++*src;
                else if ((*src)[0]=='-') i->u.asgn.op = LO_Sub, ++*src;
                else if ((*src)[0]=='*') i->u.asgn.op = LO_SgMul, ++*src;
                else if ((*src)[0]=='<' && (*src)[1]=='<') i->u.asgn.op = LO_Shl, *src += 2;
                else if ((*src)[0]=='&') i->u.asgn.op = LO_And, ++*src;
                else { error(src, "expecting binary operation", ""); return NULL; }
                if (!lil_parse_operand(src, va, &(i->u.asgn.o2))) return NULL;
            } else {
                i->u.asgn.op = LO_Mov;
            }
        }
        break;
    }
    if (!lil_parse_kw(src,";")) return NULL;
    return i;
}

LilCodeStub* lil_parse_code_stub(const char* src, ...)
{
    assert(src);
    const char** cur = &src;
    LilVarArgs va;
    va.current = 0;
    va_start(va.val, src);

    tl::MemoryPool* mem = new tl::MemoryPool;
    LilCodeStub* cs = (LilCodeStub*)mem->alloc(sizeof(LilCodeStub));
    cs->my_memory = mem;
    cs->cur_gen_label = 0;
    cs->is = NULL;
    cs->ctxt_state = LCSC_NotComputed;
    cs->num_is = 0;
    cs->init_ctxt = NULL;
    cs->compiled_code_size = 0;

    LilInstruction *i, **tail=&(cs->is);
    // originally no BB information is present; lil_cs_init_fg() will create it
    cs->bb_list_head = NULL;
    cs->num_bbs = 0;

    if (!lil_parse_kw(cur, "entry")) goto clean_up;
    POINTER_SIZE_INT n;
    if (!lil_parse_number(cur, &va, &n)) {
        goto clean_up;
    }
    else {
        cs->num_std_places = (unsigned) n;
    }
    if (!lil_parse_kw(cur, ":")) goto clean_up;
    if (!lil_parse_sig(cur, mem, &va, &(cs->sig))) goto clean_up;
    if (!lil_parse_kw(cur, ";")) goto clean_up;

    lil_skip_ws(cur);
    while ((*cur)[0]) {
        i = lil_parse_instruction(cur, mem, &cs->cur_gen_label, &va, &cs->sig);
        if (!i) goto clean_up;
        *tail = i;
        i->next = NULL;
        tail = &(i->next);
        lil_skip_ws(cur);
        cs->num_is++;
    };

    va_end(va.val);
    return cs;

 clean_up:
    va_end(va.val);
    delete mem;
    return NULL;
}

LilCodeStub* lil_parse_onto_end(LilCodeStub* cs, const char* src, ...)
{
    if (!cs) return NULL;
    assert(src);
    const char** cur = &src;
    LilVarArgs va;
    va.current = 0;
    va_start(va.val, src);

    tl::MemoryPool* mem = cs->my_memory;
    LilInstruction *i=cs->is, **tail=&(cs->is);
    while (i) { tail = &i->next; i=i->next; }

    lil_skip_ws(cur);
    while ((*cur)[0]) {
        i = lil_parse_instruction(cur, mem, &cs->cur_gen_label, &va, &(cs->sig));
        if (!i) goto clean_up;
        *tail = i;
        i->next = NULL;
        tail = &(i->next);
        lil_skip_ws(cur);
        cs->num_is++;
    };

    va_end(va.val);
    return cs;

 clean_up:
    va_end(va.val);
    delete mem;
    return NULL;
}

////////////////////////////////////////////////////////////////////////////////////
// Contexts

enum LilContextState { LCS_Unchanged, LCS_Changed, LCS_Error, LCS_Terminal };

struct LilInstructionContext {
    LilContextState s;
    unsigned num_std_places;
    LilType* std_place_types;
    LilM2nState m2n;
    unsigned num_locals;
    LilType* local_types;
    LilSig* out_sig;
    LilType ret;
    unsigned amt_alloced;
};

static void lil_new_context2(LilCodeStub* cs, LilInstructionContext* ic)
{
    ic->std_place_types = (LilType*)cs->my_memory->alloc(cs->max_std_places*sizeof(LilType));
    ic->local_types     = (LilType*)cs->my_memory->alloc(cs->max_locals    *sizeof(LilType));
}

static LilInstructionContext* lil_new_context(LilCodeStub* cs)
{
    LilInstructionContext* ic = (LilInstructionContext*)cs->my_memory->alloc(sizeof(LilInstructionContext));
    lil_new_context2(cs, ic);
    return ic;
}

// Copy c1 to c2
static void lil_copy_context(LilCodeStub* cs, LilInstructionContext* c1, LilInstructionContext* c2)
{
    *c2 = *c1;
    for(unsigned i=0; i<cs->max_std_places; i++) c2->std_place_types[i] = c1->std_place_types[i];
    for(unsigned j=0; j<cs->max_locals;     j++) c2->local_types    [j] = c1->local_types    [j];
}

unsigned lil_ic_get_num_std_places(LilInstructionContext* ic)
{
    return ic->num_std_places;
}

LilM2nState lil_ic_get_m2n_state(LilInstructionContext* ic)
{
    return ic->m2n;
}

unsigned lil_ic_get_num_locals(LilInstructionContext* ic)
{
    return ic->num_locals;
}

LilType lil_ic_get_local_type(LilInstructionContext* ic, unsigned idx)
{
    assert(idx<ic->num_locals);
    return ic->local_types[idx];
}

LilSig* lil_ic_get_out_sig(LilInstructionContext* ic)
{
    return ic->out_sig;
}

LilType lil_ic_get_ret_type(LilInstructionContext* ic)
{
    return ic->ret;
}

unsigned lil_ic_get_amt_alloced(LilInstructionContext* ic)
{
    return ic->amt_alloced;
}

LilType lil_ic_get_type(LilCodeStub* cs, LilInstructionContext* c, LilVariable* v, bool silent)
{
    switch (v->tag) {
    case LVK_In:
        if (silent && v->index>=cs->sig.num_arg_types) return LT_Void;
        assert(v->index<cs->sig.num_arg_types);
        return cs->sig.arg_types[v->index];
    case LVK_StdPlace:
        if (silent && v->index>=c->num_std_places) return LT_Void;
        assert(v->index<c->num_std_places);
        return c->std_place_types[v->index];
    case LVK_Local:
        if (silent && v->index>=c->num_locals) return LT_Void;
        assert(v->index<c->num_locals);
        return c->local_types[v->index];
        //break; //remark #111: statement is unreachable
    case LVK_Out:
        if (silent && v->index>=c->out_sig->num_arg_types) return LT_Void;
        assert(c->out_sig && v->index<c->out_sig->num_arg_types);
        return c->out_sig->arg_types[v->index];
    case LVK_Ret:
        return c->ret;
    default: DIE(("Unknown variable kind")); for(;;);
    }
}

LilType lil_ic_get_type_aux(LilCodeStub* cs, LilInstructionContext* c, LilOperand* o, bool silent)
{
    if (o->has_cast)
        return o->t;
    else
        if (o->is_immed)
            return LT_PInt;
        else
            return lil_ic_get_type(cs, c, &o->val.var, silent);
}

LilType lil_ic_get_type(LilCodeStub* cs, LilInstructionContext* c, LilOperand* o)
{
    return lil_ic_get_type_aux(cs, c, o, false);
}

static void lil_ic_set_type(LilInstructionContext* c, LilVariable* v, LilType t)
{
    switch (v->tag) {
    case LVK_In:
        // Ignore
        break;
    case LVK_StdPlace:
        if (v->index<c->num_std_places)
            c->std_place_types[v->index] = t;
        break;
    case LVK_Local:
        if (v->index<c->num_locals)
            c->local_types[v->index] = t;
        break;
    case LVK_Out:
        // Ignore
        break;
    case LVK_Ret:
        c->ret = t;
        break;
    default: DIE(("Unknown variable kind"));
    }
}

static LilType lil_type_asgn(LilCodeStub* cs, LilInstructionContext* c, LilInstruction* i);

LilType lil_instruction_get_dest_type(LilCodeStub *cs,
                                      LilInstruction *i,
                                      LilInstructionContext *ctxt) {
    switch (i->tag) {
    case LIT_Alloc:
        return LT_PInt;
    case LIT_Asgn:
        return lil_type_asgn(cs, ctxt, i);
    case LIT_Ts:
        return LT_PInt;
    case LIT_Ld:
        return i->u.ldst.t;
    default:
        return LT_Void;
    }
}

static LilType lil_type_unary_op(LilOperation op, LilType t)
{
    switch (op) {
    case LO_Mov:
    case LO_Neg:
    case LO_Not:
        return t;
    case LO_Sx1:
    case LO_Sx2:
    case LO_Sx4:
    case LO_Zx1:
    case LO_Zx2:
    case LO_Zx4:
        return t;
    default:
        DIE(("Unexpected operation"));
        return LT_Void;
    }
}

static LilType lil_type_binary_op(LilOperation UNREF op, LilType t1, LilType UNREF t2)
{
    return t1;
}

static LilType lil_type_asgn(LilCodeStub* cs, LilInstructionContext* c, LilInstruction* i)
{
    if (lil_operation_is_binary(i->u.asgn.op))
        return lil_type_binary_op(i->u.asgn.op, lil_ic_get_type_aux(cs, c, &i->u.asgn.o1, true), lil_ic_get_type_aux(cs, c, &i->u.asgn.o2, true));
    // ? 20040205: This is a hack to get the object allocation fastpath to type check
    else if (i->u.asgn.op == LO_Sx4 && !i->u.asgn.o1.is_immed && i->u.asgn.o1.val.var.tag == LVK_In && i->u.asgn.o1.val.var.index == 0)
        return LT_PInt;
    else
        return lil_type_unary_op(i->u.asgn.op, lil_ic_get_type_aux(cs, c, &i->u.asgn.o1, true));
}

// Compute the context at the beginning of an instruction given the context that falls through to it
// This function mutates c in place
static void lil_pre_context(LilInstructionContext* c, LilCodeStub* cs, LilInstruction* i)
{
    if (i->tag==LIT_Label && i->u.label.ctxt) lil_copy_context(cs, i->u.label.ctxt, c);
}

// Compute the context following the given instruction in the given code stub given the context before the instruction
// This function mutates c in place
// Note:
//   1) If the instruction is terminal the context state is set to LCS_Terminal

static void lil_next_context(LilInstructionContext* c, LilCodeStub* cs, LilInstruction* i)
{
    unsigned j;
    switch (i->tag) {
    case LIT_Label:
        break;
    case LIT_Locals:
        c->num_locals = i->u.locals;
        for(j=0; j<c->num_locals; j++) c->local_types[j] = LT_Void;
        break;
    case LIT_StdPlaces:
        c->num_std_places = i->u.std_places;
        for(j=0; j<c->num_std_places; j++) c->std_place_types[j] = LT_Void;
        break;
    case LIT_Alloc:
        c->amt_alloced += i->u.alloc.num_bytes;
        lil_ic_set_type(c, &i->u.alloc.dst, LT_PInt);
        break;
    case LIT_Asgn:{
        LilType t = lil_type_asgn(cs, c, i);
        lil_ic_set_type(c, &i->u.asgn.dst, t);
        break;}
    case LIT_Ts:
        lil_ic_set_type(c, &i->u.ts, LT_PInt);
        break;
    case LIT_Handles:
        break;
    case LIT_Ld:
        lil_ic_set_type(c, &i->u.ldst.operand.val.var, (i->u.ldst.extend==LLX_None ? i->u.ldst.t : LT_PInt));
        break;
    case LIT_St:
        break;
    case LIT_Inc:
        break;
    case LIT_Cas:
        break;
    case LIT_J:
        c->s = LCS_Terminal;
        break;
    case LIT_Jc:
        break;
    case LIT_Out:
    case LIT_In2Out:
        c->out_sig = &i->u.out;
        break;
    case LIT_Call:
        if (c->out_sig) c->ret = c->out_sig->ret_type;
        c->num_std_places = 0;
        c->out_sig = NULL;
        if (i->u.call.k!=LCK_Call) c->s = LCS_Terminal;
        break;
    case LIT_Ret:
        c->amt_alloced = 0;
        c->num_locals = 0;
        c->num_std_places = 0;
        c->out_sig = NULL;
        c->s = LCS_Terminal;
        break;
    case LIT_PushM2N:
        c->m2n = (i->u.push_m2n.handles ? LMS_Handles : LMS_NoHandles);
        c->out_sig = NULL;
        c->ret = LT_Void;
        break;
    case LIT_M2NSaveAll:
        break;
    case LIT_PopM2N:
        c->m2n = LMS_NoM2n;
        c->amt_alloced = 0;
        c->num_std_places = 0;
        c->out_sig = NULL;
        break;
    case LIT_Print:
        // nothing to do here
        break;
    default: DIE(("Unknown instruction tag"));
    }
}

// Are two signatures equal?

static bool lil_sig_equal(LilSig* s1, LilSig* s2)
{
    if (s1->cc!=s2->cc || s1->num_arg_types!=s2->num_arg_types || s1->ret_type!=s2->ret_type) return false;
    for(unsigned i=0; i<s1->num_arg_types; i++)
        if (s1->arg_types[i] != s2->arg_types[i]) return false;
    return true;
}

// Merge the contexts of two control flow edges
// This function mutates c1 in place to the merged context, setting the state to changed if it changed and error if the merge is invalid

static void lil_merge_contexts(LilInstructionContext* c1, LilInstructionContext* c2)
{
    if (c1->s==LCS_Error) return;
    if (c1->num_std_places > c2->num_std_places) { c1->s = LCS_Changed; c1->num_std_places = c2->num_std_places; }
    for(unsigned j=0; j<c1->num_std_places; j++)
        if (c1->std_place_types[j]!=c2->std_place_types[j]) { c1->s = LCS_Changed; c1->num_std_places = j; break; }
    if (c1->m2n != c2->m2n) {
        fprintf(stderr, "Context mismatch: different M2N states\n");
        fflush(stderr);
        c1->s = LCS_Error;
        return;
    }
    if (c1->num_locals != c2->num_locals) {
        fprintf(stderr, "Context mismatch: different number of locals\n");
        fflush(stderr);
        c1->s = LCS_Error;
        return;
    }
    for(unsigned k=0; k<c1->num_locals; k++)
        if (c1->local_types[k]!=c2->local_types[k]) {
            fprintf(stderr, "Context mismatch: different types at local %d\n", k);
            fflush(stderr);
            c1->s = LCS_Error;
            return;
        }
    if (c1->out_sig) {
        if (!c2->out_sig || !lil_sig_equal(c1->out_sig, c2->out_sig)) {
            fprintf(stderr, "Context mismatch: different output signatures\n");
            fflush(stderr);
            c1->s = LCS_Error;
            return;
        }
    } else {
        if (c2->out_sig) {
            fprintf(stderr, "Context mismatch: different output signatures\n");
            fflush(stderr);
            c1->s = LCS_Error;
            return; }
    }
    if (c1->ret!=LT_Void && c1->ret!=c2->ret) { c1->s = LCS_Changed; c1->ret = LT_Void; }
    if (c1->amt_alloced != c2->amt_alloced) {
        fprintf(stderr, "Context mismatch: different allocated memory amounts\n");
        fflush(stderr);
        c1->s = LCS_Error;
        return;
    }
}

// Merge a context from a control transfer instruction into one of the label it transfers to
// returns false if the label is not found

static bool lil_merge_context_with_label(LilCodeStub* cs, LilLabel l, LilInstructionContext* c)
{
    LilInstruction* i = lil_find_label(cs, l);
    if (i == NULL)
        return false;

    if (i->u.label.ctxt) {
        lil_merge_contexts(i->u.label.ctxt, c);
    } else {
        i->u.label.ctxt = lil_new_context(cs);
        lil_copy_context(cs, c, i->u.label.ctxt);
        i->u.label.ctxt->s = LCS_Changed;
    }
    return true;
}

// Merge a fall through context into a label instruction
// Note that if c->s is LCS_Terminal then there was no fall through as the previous instruction was terminal

static void lil_merge_context_with_label_instruction(LilCodeStub* cs, LilInstruction* i, LilInstructionContext* c)
{
    if (c->s==LCS_Terminal) {
        assert(i->u.label.ctxt);
        lil_copy_context(cs, i->u.label.ctxt, c);
        return;
    }
    if (i->u.label.ctxt) {
        lil_merge_contexts(i->u.label.ctxt, c);
        lil_copy_context(cs, i->u.label.ctxt, c);
    } else {
        i->u.label.ctxt = lil_new_context(cs);
        lil_copy_context(cs, c, i->u.label.ctxt);
        i->u.label.ctxt->s = LCS_Changed;
    }
}

// Compute the context of each label in a code stub so that contexts can be tracked as instructions are iterated over

void lil_compute_contexts(LilCodeStub* cs)
{
    // If already determined then return
    if (!cs || cs->ctxt_state!=LCSC_NotComputed) return;

    // Count stuff
    cs->max_std_places = cs->num_std_places;
    cs->max_locals = 0;
    for(LilInstruction* i = cs->is; i; i=i->next) {
        if (i->tag==LIT_StdPlaces && i->u.std_places>cs->max_std_places) cs->max_std_places = i->u.std_places;
        if (i->tag==LIT_Locals && i->u.locals>cs->max_locals) cs->max_locals = i->u.locals;
    }

    // Compute the initial context
    cs->init_ctxt = (LilInstructionContext*)cs->my_memory->alloc(sizeof(LilInstructionContext));
    lil_new_context2(cs, cs->init_ctxt);
    cs->init_ctxt->s = LCS_Unchanged;
    cs->init_ctxt->num_locals = 0;
    cs->init_ctxt->out_sig = NULL;
    cs->init_ctxt->num_std_places = cs->num_std_places;
    for(unsigned k=0; k<cs->num_std_places; k++) cs->init_ctxt->std_place_types[k] = LT_PInt;
    cs->init_ctxt->ret = LT_Void;
    cs->init_ctxt->m2n = LMS_NoM2n;
    cs->init_ctxt->amt_alloced = 0;

    // Fairly standard dataflow analysis
    // while (something needs recomputing)
    //   iterate through instructions from beginning to end
    //     if label          then: merge current context with label's stored context
    //     if j or jc or cas then: merge current context with target
    //     in all cases:           compute next context
    LilInstructionContext cur_ctxt;
    lil_new_context2(cs, &cur_ctxt);
    bool changed = true;
    while (changed) {
        changed = false;
        lil_copy_context(cs, cs->init_ctxt, &cur_ctxt);
        LilInstructionIterator iter(cs, false);
        unsigned inum = 0;
        while (!iter.at_end()) {
            LilInstruction* i = iter.get_current();
            if (i->tag == LIT_Label) {
                lil_merge_context_with_label_instruction(cs, i, &cur_ctxt);
                if (!i->u.label.ctxt || i->u.label.ctxt->s==LCS_Error) {
                    fprintf(stderr, "Error merging contexts at label: %s\n", i->u.label.l);
                    fflush(stderr);
                    cs->ctxt_state = LCSC_Error;
                    return;
                }
                if (i->u.label.ctxt->s==LCS_Changed) { i->u.label.ctxt->s = LCS_Unchanged; changed = true; }
            }
            lil_next_context(&cur_ctxt, cs, i);
            if (cur_ctxt.s==LCS_Error) {
                fprintf(stderr, "Error: computed next context for instruction %d:\n", inum);
                fflush(stderr);
                lil_print_instruction(stderr, i);
                cs->ctxt_state = LCSC_Error;
                return;
            }
            if (i->tag == LIT_J) {
                if (!lil_merge_context_with_label(cs, i->u.j, &cur_ctxt)) {
                    fprintf(stderr, "Error: label %s does not exist\n", i->u.j);
                    fflush(stderr);
                    cs->ctxt_state = LCSC_Error;
                    return;
                }
            } else if (i->tag == LIT_Jc) {
                if (!lil_merge_context_with_label(cs, i->u.jc.l, &cur_ctxt)) {
                    fprintf(stderr, "Error: label %s does not exist\n", i->u.jc.l);
                    fflush(stderr);
                    cs->ctxt_state = LCSC_Error;
                    return;
                }
            } else if (i->tag == LIT_Cas) {
                if (!lil_merge_context_with_label(cs, i->u.ldst.l, &cur_ctxt)) {
                    fprintf(stderr, "Error: label %s does not exist\n", i->u.ldst.l);
                    fflush(stderr);
                    cs->ctxt_state = LCSC_Error;
                    return;
                }
            }
            iter.goto_next();
            ++inum;
        }
    }
    cs->ctxt_state = LCSC_Computed;
}

////////////////////////////////////////////////////////////////////////////////////
// Iteragators (other than validity)

VMEXPORT LilSig* lil_cs_get_sig(LilCodeStub* cs) { return &cs->sig; }

VMEXPORT unsigned lil_cs_get_num_instructions(LilCodeStub* cs)
{
    return cs->num_is;
}

VMEXPORT unsigned lil_cs_get_num_BBs(LilCodeStub *cs) {
    if (cs->bb_list_head == NULL)
        LilBb::init_fg(cs);
    assert(cs->bb_list_head != NULL);
    return cs->num_bbs;
}

VMEXPORT LilBb* lil_cs_get_entry_BB(LilCodeStub* cs) {
    if (cs->bb_list_head == NULL)
        LilBb::init_fg(cs);
    assert(cs->bb_list_head != NULL);
    return cs->bb_list_head;
}

VMEXPORT unsigned lil_cs_get_max_std_places(LilCodeStub * cs) {
    return cs->max_std_places;
}

VMEXPORT unsigned lil_cs_get_max_locals(LilCodeStub * cs) {
    return cs->max_locals;
}

VMEXPORT void lil_cs_set_code_size(LilCodeStub * cs, size_t size) {
    cs->compiled_code_size = size;
}

VMEXPORT size_t lil_cs_get_code_size(LilCodeStub * cs) {
    return cs->compiled_code_size;
}

static LilInstruction* lil_find_label(LilCodeStub* cs, LilLabel l)
{
    for(LilInstruction* i=cs->is; i; i=i->next)
        if (i->tag==LIT_Label && strcmp(i->u.label.l, l)==0)
            return i;
    return NULL;
}

VMEXPORT LilInstructionIterator::LilInstructionIterator(LilCodeStub* _cs, bool _track_ctxt)
    : cs(_cs), bb(NULL), cur(_cs->is), track_ctxt(_track_ctxt)
{
    if (track_ctxt) {
        lil_compute_contexts(cs);
        ctxt = lil_new_context(cs);
        lil_copy_context(cs, cs->init_ctxt, ctxt);
        if (cs->is) lil_pre_context(ctxt, cs, cs->is);
    }
}


VMEXPORT LilInstructionIterator::LilInstructionIterator(LilCodeStub* _cs, LilBb *_bb, bool _track_ctxt)
    : cs(_cs), bb(_bb), track_ctxt(_track_ctxt)
{
    assert(bb != NULL);
    cur = bb->get_first();

    if (track_ctxt) {
        lil_compute_contexts(cs);
        ctxt = lil_new_context(cs);
        // current context is the initial context of the BB!
        lil_copy_context(cs, _bb->get_context(), ctxt);
        if (cur)
            lil_pre_context(ctxt, cs, cur);
    }
}


VMEXPORT bool LilInstructionIterator::at_end()
{
    return cur==NULL;
}

VMEXPORT LilInstruction* LilInstructionIterator::get_current()
{
    return cur;
}

VMEXPORT LilInstructionContext* LilInstructionIterator::get_context()
{
    assert(track_ctxt);
    return ctxt;
}

VMEXPORT void LilInstructionIterator::goto_next()
{
    if (track_ctxt && cur) lil_next_context(ctxt, cs, cur);
    if (cur) {
        // if this is a BB iterator, gotta check for BB end
        if (bb != NULL && cur == bb->get_last())
            cur = NULL;
        else
            cur = cur->next;
    }
    if (track_ctxt && cur) lil_pre_context(ctxt, cs, cur);
}


VMEXPORT LilInstructionVisitor::LilInstructionVisitor()
{
}


void lil_visit_instruction(LilInstruction* i, LilInstructionVisitor* v)
{
    switch (i->tag) {
    case LIT_Label:
        v->label(i->u.label.l);
        break;
    case LIT_Locals:
        v->locals(i->u.locals);
        break;
    case LIT_StdPlaces:
        v->std_places(i->u.std_places);
        break;
    case LIT_Alloc:
        v->alloc(&i->u.alloc.dst, i->u.alloc.num_bytes);
        break;
    case LIT_Asgn:
        v->asgn(&i->u.asgn.dst, i->u.asgn.op, &i->u.asgn.o1, &i->u.asgn.o2);
        break;
    case LIT_Ts:
        v->ts(&i->u.ts);
        break;
    case LIT_Handles:
        v->handles(&i->u.handles);
        break;
    case LIT_Ld:
        v->ld(i->u.ldst.t, &i->u.ldst.operand.val.var, (i->u.ldst.is_base ? &i->u.ldst.base : NULL),
              i->u.ldst.scale, (i->u.ldst.is_index ? &i->u.ldst.index : NULL), i->u.ldst.offset, i->u.ldst.acqrel, i->u.ldst.extend);
        break;
    case LIT_St:
        v->st(i->u.ldst.t, (i->u.ldst.is_base ? &i->u.ldst.base : NULL),
              i->u.ldst.scale, (i->u.ldst.is_index ? &i->u.ldst.index : NULL), i->u.ldst.offset, i->u.ldst.acqrel,
              &i->u.ldst.operand);
        break;
    case LIT_Inc:
        v->inc(i->u.ldst.t, (i->u.ldst.is_base ? &i->u.ldst.base : NULL),
               i->u.ldst.scale, (i->u.ldst.is_index ? &i->u.ldst.index : NULL), i->u.ldst.offset, i->u.ldst.acqrel);
        break;
    case LIT_Cas:
        v->cas(i->u.ldst.t, (i->u.ldst.is_base ? &i->u.ldst.base : NULL),
               i->u.ldst.scale, (i->u.ldst.is_index ? &i->u.ldst.index : NULL), i->u.ldst.offset, i->u.ldst.acqrel,
               &i->u.ldst.compare, &i->u.ldst.operand, i->u.ldst.l);
        break;
    case LIT_J:
        v->j(i->u.j);
        break;
    case LIT_Jc:
        v->jc(i->u.jc.c.tag, &i->u.jc.c.o1, &i->u.jc.c.o2, i->u.jc.l);
        break;
    case LIT_Out:
        v->out(&i->u.out);
        break;
    case LIT_In2Out:
        v->in2out(&i->u.out);
        break;
    case LIT_Call:
        v->call(&i->u.call.target, i->u.call.k);
        break;
    case LIT_Ret:
        v->ret();
        break;
    case LIT_PushM2N:
        v->push_m2n(i->u.push_m2n.method, i->u.push_m2n.current_frame_type, i->u.push_m2n.handles);
        break;
    case LIT_M2NSaveAll:
        v->m2n_save_all();
        break;
    case LIT_PopM2N:
        v->pop_m2n();
        break;
    case LIT_Print:
        v->print(i->u.print.str, &i->u.print.arg);
        break;
    default: DIE(("Unknown instruction tag"));
    }
}

LilVariableKind lil_variable_get_kind(LilVariable* v) { return v->tag; }
unsigned lil_variable_get_index(LilVariable* v) { return v->index; }

bool lil_variable_is_equal(LilVariable* v1, LilVariable* v2)
{
    return v1->tag==v2->tag && v1->index==v2->index;
}

bool lil_operand_is_immed(LilOperand* o) { return o->is_immed; }
POINTER_SIZE_INT lil_operand_get_immed(LilOperand* o) {
    assert(o->is_immed);
    return o->val.imm;
}

LilVariable* lil_operand_get_variable(LilOperand* o) {
    assert(!o->is_immed);
    return &o->val.var;
}

LilCc lil_sig_get_cc(LilSig* sig) { return sig->cc; }
bool lil_sig_is_arbitrary(LilSig* sig) { return sig->arbitrary; }
unsigned lil_sig_get_num_args(LilSig* sig) { return sig->num_arg_types; }
LilType lil_sig_get_arg_type(LilSig* sig, unsigned num) {
    assert(num < sig->num_arg_types);
    return sig->arg_types[num];
}
LilType lil_sig_get_ret_type(LilSig* sig) { return sig->ret_type; }

bool lil_predicate_is_binary(enum LilPredicate c)
{
    switch (c) {
    case LP_IsZero:
    case LP_IsNonzero:
        return false;
    case LP_Eq:
    case LP_Ne:
    case LP_Le:
    case LP_Lt:
    case LP_Ule:
    case LP_Ult:
        return true;
    default:
        DIE(("Unknown predicate"));
        return false; // not reached
    }
}

bool lil_predicate_is_signed(LilPredicate p) {
    return (p == LP_Ule || p == LP_Ult);
}

bool lil_operation_is_binary(enum LilOperation op)
{
    switch (op) {
    case LO_Mov:
    case LO_Neg:
    case LO_Not:
    case LO_Sx1:
    case LO_Sx2:
    case LO_Sx4:
    case LO_Zx1:
    case LO_Zx2:
    case LO_Zx4:
        return false;
    case LO_Add:
    case LO_Sub:
    case LO_SgMul:
    case LO_Shl:
    case LO_And:
        return true;
    default:
        DIE(("Unknown operation"));
        return false; // not reached
    }
}

////////////////////////////////////////////////////////////////////////////////////
// Validity

static bool lil_is_valid_asgn(LilCodeStub* cs, LilInstructionContext* c, LilVariable* v, LilType t)
{
    switch (v->tag) {
    case LVK_In:
        return v->index<cs->sig.num_arg_types && cs->sig.arg_types[v->index]==t;
    case LVK_StdPlace:
        return v->index<c->num_std_places;
    case LVK_Local:
        return v->index<c->num_locals;
    case LVK_Out:
        return c->out_sig && v->index<c->out_sig->num_arg_types && c->out_sig->arg_types[v->index]==t;
    case LVK_Ret:
        return v->index<1;
    default:
        return false;
    }
}

static bool lil_is_valid_variable(LilCodeStub* cs, LilInstructionContext* c, LilVariable* v)
{
    switch (v->tag) {
    case LVK_In:       return v->index<cs->sig.num_arg_types;
    case LVK_StdPlace: return v->index<c->num_std_places;
    case LVK_Local:    return v->index<c->num_locals;
    case LVK_Out:      return c->out_sig && v->index<c->out_sig->num_arg_types;
    case LVK_Ret:      return v->index<1 && c->ret!=LT_Void;
    default:           return false;
    }
}

#ifdef _IPF_
#define PLATFORM_INT LT_G8
#else
#define PLATFORM_INT LT_G4
#endif

static bool lil_are_types_compatible(LilType t1, LilType t2)
{
    return t1==t2 || ((t1==LT_Ref || t1==LT_PInt || t1==PLATFORM_INT) && (t2==LT_Ref || t2==LT_PInt || t2==PLATFORM_INT));
}

static bool lil_is_valid_operand(LilCodeStub* cs, LilInstructionContext* c, LilOperand* o)
{
    if (!o->is_immed && !lil_is_valid_variable(cs, c, &(o->val.var))) return false;
    if (o->has_cast) {
        LilType raw_type = lil_ic_get_type(cs, c, o);
        if (!lil_are_types_compatible(o->t, raw_type)) return false;
    }
    return true;
}

static bool lil_is_valid_address(LilCodeStub* cs, LilInstructionContext* c, LilInstruction* i)
{
    if (i->u.ldst.is_base) {
        if (!lil_is_valid_variable(cs, c, &(i->u.ldst.base))) return false;
        LilType t = lil_ic_get_type(cs, c, &(i->u.ldst.base), true);
        if (t!=LT_Ref && t!=LT_PInt && t!=PLATFORM_INT) return false;
    }
    if (i->u.ldst.is_index) {
        if (!lil_is_valid_variable(cs, c, &(i->u.ldst.index))) return false;
        LilType t = lil_ic_get_type(cs, c, &(i->u.ldst.index), true);
        if (t!=LT_Ref && t!=LT_PInt && t!=PLATFORM_INT) return false;
    }
    return true;
}

static bool lil_is_valid_label(LilCodeStub* cs, LilLabel l)
{
    return lil_find_label(cs, l)!=NULL;
}

static bool lil_verify_unary_op(LilOperation UNREF op, LilType UNREF t)
{
    return true;
}

static bool lil_verify_binary_op(LilOperation op, LilType t1, LilType t2)
{
    if (op==LO_Shl)
        return t2==LT_PInt;
    else
        return t1==t2;
}

static bool lil_verify_binary_cond(LilPredicate UNREF p, LilType t1, LilType t2)
{
    return t1==t2;
}

static bool lil_print_err(const char *s, LilInstruction* i, unsigned inst_number)
{
    fprintf(stderr, "lil code stub invalid at instruction %d: %s\n  ", inst_number, s);
    if (i) lil_print_instruction(stdout, i);
    fflush(stderr);
    return false;
}

#define ERR(s) { return lil_print_err(s, i, inst_number); }

bool lil_is_valid(LilCodeStub* cs)
{
    unsigned inst_number = 0;
    LilInstruction* i = NULL;

    if (!cs) ERR("code stub is null");

    lil_compute_contexts(cs);
    if (cs->ctxt_state == LCSC_Error) ERR("control flow contexts inconsistent");

    // Check instructions
    bool last_was_terminal = false;
    LilInstructionIterator iter(cs, true);
    while (!iter.at_end()) {
        inst_number++;
        i = iter.get_current();
        LilInstructionContext* c = iter.get_context();

        switch (i->tag) {
        case LIT_Label:
        {
            LilLabel l = i->u.label.l;
            LilInstruction *label_def = lil_find_label(cs, l);
            if (label_def != i) {
                char buffer[256];
                sprintf(buffer, "label %s redefined", l);
                ERR(buffer);
            }
            break;
        }
        case LIT_Locals:
            break;
        case LIT_StdPlaces:
            break;
        case LIT_Alloc:
            if (c->out_sig) ERR("alloc between out and call");
            if (!lil_is_valid_variable(cs, c, &(i->u.alloc.dst))) ERR("invalid variable in alloc");
            break;
        case LIT_Asgn:
            if (!lil_is_valid_operand(cs, c, &(i->u.asgn.o1))) ERR("invalid first operand in assignment");
            if (lil_operation_is_binary(i->u.asgn.op)) {
                if (!lil_is_valid_operand(cs, c, &(i->u.asgn.o2))) ERR("invalid second operand in assignment");
                if (!lil_verify_binary_op(i->u.asgn.op, lil_ic_get_type(cs, c, &i->u.asgn.o1), lil_ic_get_type(cs, c, &i->u.asgn.o2)))
                    ERR("operand type mismatch in assignment");
            } else {
                if (!lil_verify_unary_op(i->u.asgn.op, lil_ic_get_type(cs, c, &i->u.asgn.o1)))
                    ERR("operand type mismatch in assignment");
            }
            // ? 20040205: This is a hack to get the object allocation fastpath to type check
            if (i->u.asgn.op == LO_Sx4 && !i->u.asgn.o1.is_immed && i->u.asgn.o1.val.var.tag == LVK_In && i->u.asgn.o1.val.var.index == 0) {
                if (!lil_is_valid_asgn(cs, c, &i->u.asgn.dst, LT_PInt)) 
                    ERR("invalid destination or type incompatibility in assignment");
            } else if (!lil_is_valid_asgn(cs, c, &i->u.asgn.dst, lil_type_asgn(cs, c, i))) {
                ERR("invalid destination or type incompatibility in assignment");
            }
            break;
        case LIT_Ts:
            if (!lil_is_valid_asgn(cs, c, &i->u.ts, LT_PInt)) ERR("invalid destination in ts");
            break;
        case LIT_Handles:
            if (c->m2n!=LMS_Handles) ERR("handles not dominated by push_m2n handles");
            if (!lil_is_valid_operand(cs, c, &(i->u.handles))) ERR("invalid operand in handles");
            if (lil_ic_get_type(cs, c, &i->u.handles)!=LT_PInt) ERR("operand not platform int in handles");
            break;
        case LIT_Ld:
            if (!lil_is_valid_asgn(cs, c, &i->u.ldst.operand.val.var, (i->u.ldst.extend==LLX_None ? i->u.ldst.t : LT_PInt)))
                ERR("invalid destination in load");
            if (!lil_is_valid_address(cs, c, i)) ERR("invalid address in load");
            break;
        case LIT_St:
            if (!lil_is_valid_address(cs, c, i)) ERR("invalid address in store");
            if (!i->u.ldst.operand.is_immed && i->u.ldst.t!=lil_ic_get_type(cs, c, &i->u.ldst.operand)) ERR("type mismatch in store");
            if (!lil_is_valid_operand(cs, c, &(i->u.ldst.operand))) ERR("invalid source in store");
            break;
        case LIT_Inc:
            if (!lil_is_valid_address(cs, c, i)) ERR("invalid address in inc");
            break;
        case LIT_Cas:
            if (!lil_is_valid_address(cs, c, i)) ERR("invalid address in cas");
            if (!i->u.ldst.compare.is_immed && i->u.ldst.t!=lil_ic_get_type(cs, c, &i->u.ldst.operand)) ERR("type mismatch in cas compare");
            if (!i->u.ldst.operand.is_immed && i->u.ldst.t!=lil_ic_get_type(cs, c, &i->u.ldst.operand)) ERR("type mismatch in cas source");
            if (!lil_is_valid_operand(cs, c, &(i->u.ldst.compare))) ERR("invalid compare in cas");
            if (!lil_is_valid_operand(cs, c, &(i->u.ldst.operand))) ERR("invalid source in cas");
            if (!lil_is_valid_label(cs, i->u.ldst.l)) ERR("bad target in cas");
            break;
        case LIT_J:
            if (!lil_is_valid_label(cs, i->u.j)) ERR("bad target in j");
            break;
        case LIT_Jc:
            // Should do typechecks here
            if (!lil_is_valid_label(cs, i->u.jc.l)) ERR("bad target in jc");
            if (!lil_is_valid_operand(cs, c, &(i->u.jc.c.o1))) ERR("invalid first operand in condition");
            if (lil_predicate_is_binary(i->u.jc.c.tag)) {
                if (!lil_is_valid_operand(cs, c, &(i->u.jc.c.o2))) ERR("invalid second operand in condition");
                if (!lil_verify_binary_cond(i->u.jc.c.tag, lil_ic_get_type(cs, c, &(i->u.jc.c.o1)), lil_ic_get_type(cs, c, &(i->u.jc.c.o2))))
                    ERR("operand type mismatch in conditional jump");
            }
            break;
        case LIT_Out:
            break;
        case LIT_In2Out:
            if (cs->sig.arbitrary) ERR("in2out in arbitrary code stub");
            break;
        case LIT_Call:
            if (i->u.call.k!=LCK_TailCall && !c->out_sig) ERR("call not dominated by out or in2out");
            if (!lil_is_valid_operand(cs, c, &(i->u.call.target))) ERR("invalid operand in call");
            if (lil_ic_get_type(cs, c, &i->u.call.target)!=LT_PInt) ERR("operand not platform int in call");
            break;
        case LIT_Ret:
            if (cs->sig.arbitrary) ERR("cannot return from an arbitrary signature entry");
            if (cs->sig.ret_type!=LT_Void && cs->sig.ret_type!=c->ret)
                ERR("ret with invalid return value");
            if (c->m2n) ERR("return with m2n");
            break;
        case LIT_PushM2N:
            if (c->amt_alloced>0) ERR("alloc before push_m2n");
            if (c->m2n!=LMS_NoM2n) ERR("push m2n twice");
            break;
        case LIT_M2NSaveAll:
            if (c->m2n==LMS_NoM2n) ERR("m2n save all not dominated by push m2n");
            break;
        case LIT_PopM2N:
            if (c->m2n==LMS_NoM2n) ERR("pop m2n not dominated by push");
            break;
        case LIT_Print:
            if (!lil_is_valid_operand(cs, c, &i->u.print.arg))
                ERR("invalid argument to print");
            break;
        default:
            ERR("unknown instruction");
        }

        iter.goto_next();
        last_was_terminal = (c->s==LCS_Terminal);
    }

    if (!last_was_terminal) ERR("last instruction not terminal");

    return true;
}

////////////////////////////////////////////////////////////////////////////////////
// Printing Utilities

void lil_print_cc(FILE* out, enum LilCc cc)
{
    switch (cc) {
    case LCC_Platform: fprintf(out, "platform"); break;
    case LCC_Managed: fprintf(out, "managed"); break;
    case LCC_Rth: fprintf(out, "rth"); break;
    case LCC_Jni: fprintf(out, "jni"); break;
    case LCC_StdCall: fprintf(out, "stdcall"); break;
    default: DIE(("Unknown calling convention"));
    }
    fflush(out);
}

void lil_print_type(FILE* out, enum LilType t)
{
    switch (t) {
    case LT_G1: fprintf(out, "g1"); break;
    case LT_G2: fprintf(out, "g2"); break;
    case LT_G4: fprintf(out, "g4"); break;
    case LT_G8: fprintf(out, "g8"); break;
    case LT_F4: fprintf(out, "f4"); break;
    case LT_F8: fprintf(out, "f8"); break;
    case LT_Ref: fprintf(out, "ref"); break;
    case LT_PInt: fprintf(out, "pint"); break;
    case LT_Void: fprintf(out, "void"); break;
    default: DIE(("Unknown LIL type"));
    }
    fflush(out);
}

void lil_print_sig(FILE* out, LilSig* sig)
{
    assert(sig);
    lil_print_cc(out, sig->cc);
    fprintf(out, ":");
    if (sig->arbitrary) {
        fprintf(out, "arbitrary");
    } else {
        for(unsigned i=0; i<sig->num_arg_types; i++) {
            if (i>0) fprintf(out, ",");
            lil_print_type(out, sig->arg_types[i]);
        }
        fprintf(out, ":");
        lil_print_type(out, sig->ret_type);
    }
    fflush(out);
}

void lil_print_variable(FILE* out, LilVariable* v)
{
    assert(v);
    switch (v->tag) {
    case LVK_In:
        fprintf(out, "i");
        break;
    case LVK_StdPlace:
        fprintf(out, "sp");
        break;
    case LVK_Out:
        fprintf(out, "o");
        break;
    case LVK_Local:
        fprintf(out, "l");
        break;
    case LVK_Ret:
        assert(v->index==0);
        fprintf(out, "r");
        return;
    default: DIE(("Unknown kind"));
    }
    fprintf(out, "%d", v->index);
    fflush(out);
}

void lil_print_operand(FILE* out, LilOperand* o)
{
    assert(o);
    if (o->is_immed)
        // since imm is POINTER_SIZE_INT, %p should work in all cases
        fprintf(out, "0x%p", (void *)o->val.imm);
    else
        lil_print_variable(out, &(o->val.var));
    if (o->has_cast) {
        fprintf(out, ":");
        lil_print_type(out, o->t);
    }
    fflush(out);
}

void lil_print_address(FILE* out, LilInstruction* i)
{
    bool printed_term = false;
    fprintf(out, "[");
    if (i->u.ldst.is_base) {
        lil_print_variable(out, &(i->u.ldst.base));
        printed_term = true;
    }
    if (i->u.ldst.is_index) {
        if (printed_term)
            fprintf(out, "+");
        fprintf(out, "%d*", i->u.ldst.scale);
        lil_print_variable(out, &(i->u.ldst.index));
        printed_term = true;
    }
    if (i->u.ldst.offset != 0) {
        if (printed_term)
            fprintf(out, "+");
        fprintf(out, "0x%p", (void *)i->u.ldst.offset);
        printed_term = true;
    }
    if (!printed_term)
        fprintf(out, "0x0");
    fprintf(out, ":");
    lil_print_type(out, i->u.ldst.t);
    switch (i->u.ldst.acqrel) {
    case LAR_None:    break;
    case LAR_Acquire: fprintf(out, ",acquire"); break;
    case LAR_Release: fprintf(out, ",release"); break;
    default: DIE(("Unexpected acqrel value"));
    }
    fprintf(out, "]");
    fflush(out);
}

void lil_print_instruction(FILE* out, LilInstruction* i)
{
    assert(i);
    switch (i->tag) {
    case LIT_Label:
        fprintf(out, ":%s", i->u.label.l);
        break;
    case LIT_Locals:
        fprintf(out, "locals %d", i->u.locals);
        break;
    case LIT_StdPlaces:
        fprintf(out, "std_places %d", i->u.std_places);
        break;
    case LIT_Alloc:
        fprintf(out, "alloc ");
        lil_print_variable(out, &i->u.alloc.dst);
        fprintf(out, ",0x%x", i->u.alloc.num_bytes);
        break;
    case LIT_Asgn:
        lil_print_variable(out, &(i->u.asgn.dst));
        fprintf(out, "=");
        switch (i->u.asgn.op) {
        case LO_Mov:
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Add:
            lil_print_operand(out, &(i->u.asgn.o1));
            fprintf(out, "+");
            lil_print_operand(out, &(i->u.asgn.o2));
            break;
        case LO_Sub:
            lil_print_operand(out, &(i->u.asgn.o1));
            fprintf(out, "-");
            lil_print_operand(out, &(i->u.asgn.o2));
            break;
        case LO_SgMul:
            lil_print_operand(out, &(i->u.asgn.o1));
            fprintf(out, "*");
            lil_print_operand(out, &(i->u.asgn.o2));
            break;
        case LO_Shl:
            lil_print_operand(out, &(i->u.asgn.o1));
            fprintf(out, "<<");
            lil_print_operand(out, &(i->u.asgn.o2));
            break;
        case LO_And:
            lil_print_operand(out, &(i->u.asgn.o1));
            fprintf(out, "&");
            lil_print_operand(out, &(i->u.asgn.o2));
            break;
        case LO_Neg:
            fprintf(out, "-");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Not:
            fprintf(out, "not ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Sx1:
            fprintf(out, "sx1 ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Sx2:
            fprintf(out, "sx2 ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Sx4:
            fprintf(out, "sx4 ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Zx1:
            fprintf(out, "zx1 ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Zx2:
            fprintf(out, "zx2 ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        case LO_Zx4:
            fprintf(out, "zx4 ");
            lil_print_operand(out, &(i->u.asgn.o1));
            break;
        default: DIE(("Unknown operation"));
        }
        break;
    case LIT_Ts:
        lil_print_variable(out, &i->u.ts);
        fprintf(out, "=ts");
        break;
    case LIT_Handles:
        fprintf(out, "handles=");
        lil_print_operand(out, &i->u.handles);
        break;
    case LIT_Ld:
        fprintf(out, "ld ");
        lil_print_variable(out, &(i->u.ldst.operand.val.var));
        fprintf(out, ",");
        lil_print_address(out, i);
        if (i->u.ldst.extend==LLX_Sign) fprintf(out, ",sx");
        if (i->u.ldst.extend==LLX_Zero) fprintf(out, ",zx");
        break;
    case LIT_St:
        fprintf(out, "st ");
        lil_print_address(out, i);
        fprintf(out, ",");
        lil_print_operand(out, &(i->u.ldst.operand));
        break;
    case LIT_Inc:
        fprintf(out, "inc ");
        lil_print_address(out, i);
        break;
    case LIT_Cas:
        fprintf(out, "cas ");
        lil_print_address(out, i);
        fprintf(out, "=");
        lil_print_operand(out, &(i->u.ldst.compare));
        fprintf(out, ",");
        lil_print_operand(out, &(i->u.ldst.operand));
        fprintf(out, ",%s", i->u.ldst.l);
        break;
    case LIT_J:
        fprintf(out, "j %s", i->u.j);
        break;
    case LIT_Jc:
        fprintf(out, "jc ");
        switch (i->u.jc.c.tag) {
        case LP_IsZero:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, "=0");
            break;
        case LP_IsNonzero:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, "!=0");
            break;
        case LP_Eq:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, "=");
            lil_print_operand(out, &(i->u.jc.c.o2));
            break;
        case LP_Ne:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, "!=");
            lil_print_operand(out, &(i->u.jc.c.o2));
            break;
        case LP_Le:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, "<=");
            lil_print_operand(out, &(i->u.jc.c.o2));
            break;
        case LP_Lt:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, "<");
            lil_print_operand(out, &(i->u.jc.c.o2));
            break;
        case LP_Ule:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, " <=u ");
            lil_print_operand(out, &(i->u.jc.c.o2));
            break;
        case LP_Ult:
            lil_print_operand(out, &(i->u.jc.c.o1));
            fprintf(out, " <u ");
            lil_print_operand(out, &(i->u.jc.c.o2));
            break;
        default: DIE(("Unknown predicate"));
        }
        fprintf(out, ",%s", i->u.jc.l);
        break;
    case LIT_Out:
        fprintf(out, "out ");
        lil_print_sig(out, &(i->u.out));
        break;
    case LIT_In2Out:
        fprintf(out, "in2out ");
        lil_print_cc(out, i->u.out.cc);
        fprintf(out, ":");
        lil_print_type(out, i->u.out.ret_type);
        break;
    case LIT_Call:
        switch (i->u.call.k) {
        case LCK_Call:
            fprintf(out, "call ");
            break;
        case LCK_CallNoRet:
            fprintf(out, "call.noret ");
            break;
        case LCK_TailCall:
            fprintf(out, "tailcall ");
            break;
        default: DIE(("Unknown call kind"));
        }
        lil_print_operand(out, &(i->u.call.target));
        break;
    case LIT_Ret:
        fprintf(out, "ret");
        break;
    case LIT_PushM2N:
        fprintf(out, "push_m2n %p, frame_type= %p", i->u.push_m2n.method, i->u.push_m2n.current_frame_type);
        if (i->u.push_m2n.handles) fprintf(out, ",handles");
        break;
    case LIT_M2NSaveAll:
        fprintf(out, "m2n_save_all");
        break;
    case LIT_PopM2N:
        fprintf(out, "pop_m2n");
        break;
    case LIT_Print:
        fprintf(out, "print %p, ", i->u.print.str);
        lil_print_operand(out, &i->u.print.arg);
        fprintf(out, "\n");
        break;
    default:
        DIE(("Unknown instruction tag"));
    };
    fprintf(out, ";\n");
    fflush(out);
}

void lil_print_entry(FILE* out, LilCodeStub* cs)
{
    fprintf(out, "entry %d:", cs->num_std_places);
    lil_print_sig(out, &(cs->sig));
    fprintf(out, ";\n");
    fflush(out);
}

void lil_print_code_stub(FILE* out, LilCodeStub* cs)
{
    assert(cs);
    lil_print_entry(out, cs);
    for(LilInstruction* i=cs->is; i; i=i->next)
        lil_print_instruction(out, i);
}

////////////////////////////////////////////////////////////////////////////////////
// Basic Blocks

// private constructor; create BBs by calling init_fg()
LilBb::LilBb(LilCodeStub *_cs, LilInstruction *_start,
             LilInstructionContext* ctxt_at_start):
    cs(_cs), start(_start), end(NULL),
    ctxt(NULL),
    fallthru(NULL),
    branch_target(NULL),
    num_pred(0),
    next(NULL), id(-1)
{
    // add this to the end of the stub's BB list
    if (cs->bb_list_head == NULL)
        cs->bb_list_head = this;
    else {
        LilBb* last_bb = cs->bb_list_head;
        while (last_bb->next != NULL)
            last_bb = last_bb->next;
        last_bb->next = this;
    }

    // store a copy of the current context in ctxt
    assert(ctxt_at_start != NULL);
    ctxt = lil_new_context(cs);
    lil_copy_context(cs, ctxt_at_start, ctxt);
}  // LilBb::LilBb

// private operator new; create BBs using new_bb() only
void* LilBb::operator new(size_t sz, tl::MemoryPool& m) {
    return m.alloc(sz);
}  // LilBb::operator new

int LilBb::get_id() {
    return id;
}  // LilBb::get_id

// sets the last instruction of the BB
void LilBb::set_last(LilInstruction *i) {
    // can't set the last instruction twice!
    assert(end == NULL);
    end = i;
}  // LilBb::set_last

// gets the first instruction
LilInstruction* LilBb::get_first() {
    return start;
}  // LilBb::get_first

// gets the last instruction
LilInstruction* LilBb::get_last() {
    return end;
}  // LilBb::get_last

LilInstructionContext* LilBb::get_context() {
    return ctxt;
}  // LilBb::get_context

// does this bb contain instruction i?
bool LilBb::contains(LilInstruction *i) {
    for (LilInstruction* j = start; j != NULL;  j++) {
        if (j == i)
            return true;
        if (j == end)
            break;
    }
    return false;
}  // LilBb::contains

// get the label of this BB; NULL if no label exists
LilLabel LilBb::get_label() {
    if (start->tag == LIT_Label)
        return start->u.label.l;
    return NULL;
}  // LilBb::get_label

// set a fallthrough successor to this bb
void LilBb::set_fallthru(LilBb *succ) {
    fallthru = succ;
    assert(succ->num_pred < MAX_BB_PRED);
    succ->pred[succ->num_pred++] = this;
}  // LilBb::set_fallthru

// set a branch-target successor to this bb
void LilBb::set_branch_target(LilBb *succ) {
    branch_target = succ;
    assert(succ->num_pred < MAX_BB_PRED);
    succ->pred[succ->num_pred++] = this;
}  // LilBb::set_branch_target

// get the fallthrough and branch target successors;
// either of them can be NULL if they don't exist
LilBb* LilBb::get_fallthru() {
    return fallthru;
}  // LilBb::get_fallthru

LilBb* LilBb::get_branch_target() {
    return branch_target;
}  // LilBb::get_branch_target


// gets the i'th predecessor (NULL if i >= num_pred)
LilBb *LilBb::get_pred(unsigned i) {
    return (i < num_pred) ? pred[i] : NULL;
}  // LilBb::get_pred

// gets the next BB in the list
LilBb* LilBb::get_next() {
    return next;
}  // LilBb::get_next

// returns whether this BB ends in a return instruction
// (tailcall implies return!)
bool LilBb::is_ret() {
    return (end != NULL &&
            (end->tag == LIT_Ret ||
             (end->tag == LIT_Call && end->u.call.k == LCK_TailCall)));
}


// true if this BB contains calls (which means it may throw exceptions)
bool LilBb::does_calls() {
    for (LilInstruction *i=start;  i != NULL;  i = i->next) {
        if (i->tag == LIT_Call)
            return true;
        if (i == end)
            break;
    }
    return false;
}


// true if this BB ends with a call.noret (which means it is probably
// cold code)
bool LilBb::does_call_noret() {
    return (end != NULL && end->tag == LIT_Call &&
            end->u.call.k == LCK_CallNoRet);
}

// find a BB with the specified label
// NULL if no such bb exists
LilBb* LilBb::get_by_label(LilCodeStub *cs, LilLabel l) {
    assert(l != NULL);
    LilBb *bb = cs->bb_list_head;

    while (bb != NULL &&
           (bb->get_label() == NULL || strcmp(bb->get_label(), l)))
        bb = bb->next;
    return bb;
}  // LilBb::get_by_label

// find a BB which contains the specified instruction
// NULL if no such BB exists
LilBb* LilBb::get_by_instruction(LilCodeStub *cs, LilInstruction *i) {
    LilBb* bb = cs->bb_list_head;

    while (bb != NULL && !bb->contains(i))
        bb = bb->next;

    return bb;
}  // LilBb::get_by_instruction

// print a BB to a stream
// (does not print the BB's instructions)
void LilBb::print(FILE* out) {
    fprintf(out, "-- BB %d ", id);

    // print predecessors
    fprintf(out, "(pred:");
    if (num_pred == 0)
        fprintf(out, " none)");
    else {
        for (unsigned i=0; i<num_pred; i++)
            fprintf(out, " %d", get_pred(i)->id);
        fprintf(out, ")");
    }

    // print successors
    fprintf(out, " (succ:");
    if (fallthru != NULL)
        fprintf(out, " %d", fallthru->id);
    if (branch_target != NULL)
        fprintf(out, " %d", branch_target->id);
    if (fallthru == NULL && branch_target == NULL)
        fprintf(out, " none");
    fprintf(out, ")");

    // print label
    if (get_label() != NULL)
        fprintf(out, " (label: %s)", get_label());

    fprintf(out, " --\n");
    fflush(out);
}  // void LilBb::print


// initializes the flowgraph by creating a list of BBs
// BBs in the list appear in the same order as they appear in the source code
void LilBb::init_fg(LilCodeStub *cs) {
    LilInstructionIterator it(cs, true);

    LilInstruction *prev_inst = NULL;  // previous instruction
    LilBb* cur_bb = NULL;  // current BB

    while (!it.at_end()) {
        LilInstruction* inst = it.get_current();
        LilInstructionContext* ctxt = it.get_context();

        if (inst->tag == LIT_Label || cur_bb == NULL) {
            // close old bb, if it exists
            if (cur_bb != NULL) {
                assert(prev_inst != NULL);
                cur_bb->set_last(prev_inst);
            }

            // create new bb
            cur_bb = new(*cs->my_memory) LilBb(cs, inst, ctxt);
            cur_bb->id = cs->num_bbs++;
        }

        // check if the BB should end here
        if (inst->tag == LIT_J ||
            inst->tag == LIT_Jc ||
            inst->tag == LIT_Cas ||
            inst->tag == LIT_Ret ||
            (inst->tag == LIT_Call && inst->u.call.k != LCK_Call)) {
            cur_bb->set_last(inst);
            cur_bb = NULL;  // so that the next inst will start a new one
        }

        // advance pointers
        prev_inst = inst;
        it.goto_next();
    }

    // close the last BB
    if (cur_bb != NULL)
        cur_bb->set_last(prev_inst);

    // set up the successor / predecessor relations
    for (cur_bb = cs->bb_list_head;  cur_bb != NULL; cur_bb = cur_bb->next) {
        LilInstruction *last_i = cur_bb->end;
        assert(last_i != NULL);
        if (last_i->tag == LIT_J) {
            LilBb* succ = get_by_label(cs, last_i->u.j);
            assert(succ != NULL);
            cur_bb->set_branch_target(succ);
            continue;  // don't set fallthru
        }
        if (last_i->tag == LIT_Ret ||
            (last_i->tag == LIT_Call && last_i->u.call.k != LCK_Call)) {
            continue;  // terminal inst; don't set fallthru
        }
        if (last_i->tag == LIT_Jc) {
            LilBb* succ = get_by_label(cs, last_i->u.jc.l);
            assert(succ != NULL);
            cur_bb->set_branch_target(succ);
        }
        if (last_i->tag == LIT_Cas) {
            LilBb* succ = get_by_label(cs, last_i->u.ldst.l);
            assert(succ);
            cur_bb->set_branch_target(succ);
        }

        // set fallthru
        assert(cur_bb->next != NULL);
        cur_bb->set_fallthru(cur_bb->next);
    }
}  // LilBb::init_fg


// EOF: lil.cpp
