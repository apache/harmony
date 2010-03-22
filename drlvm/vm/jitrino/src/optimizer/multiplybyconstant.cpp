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
 * @author Intel, Pavel A. Ozhdikhin
 *
 */

#undef STANDALONE_TEST

#include <iostream>
#include <fstream>
#include <float.h>
#include <cstdlib>
#include <algorithm>

#ifdef STANDALONE_TEST

#include "standalonetest2.h"

#else

#include "Opcode.h"
#include "Opnd.h"
#include "Type.h"
#include "Inst.h"
#include "IRBuilder.h"
#include "BitSet.h"
#include "Log.h"
#include "optimizer.h"
#include "simplifier.h"
#include "constantfolder.h"
#include "deadcodeeliminator.h"
#include "optarithmetic.h"
#include "irmanager.h"
#include "CompilationContext.h"

#include <float.h>
#include <math.h>

#include "Stl.h"
#include "simplifier.h"
#include "optarithmetic.h"



namespace Jitrino {

#ifndef DEBUG_MULTIPLYBYCONSTANT
#define DEBUGPRINT(x)
#define DEBUGPRINT2(x,y)

#else
void indent(int indentby) {
    for (int i=0; i<indentby; i++) {
        Log::out() << " ";
    }
}
#define DEBUGPRINT(x) indent(2*depth); Log::out() << x << ::std::endl
#define DEBUGPRINT2(x,y) indent(2*depth); Log::out() << x; y.print(Log::out()); Log::out() << ::std::endl
#endif

#endif


struct MulOp {
    enum Op { pushc=0, pushy=1, swap=2, dup=3, shladd=4, add=5, sub=6, neg=7, shiftl=8, dup2, dup3, numMulOps=11 };
};

const int methodLen = 400;
const int stackDepth = 400;
const int numMulOpnds[MulOp::numMulOps] = { 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0 };

const int COST_LOADZERO = 0;
const int COST_LOADY = 0;
const int COST_NEG = 1;
const int COST_ADD = 1;
const int COST_SUB = 1;

class MulMethod {
private:
public:
    int data[methodLen];
    int len;
    bool foripf;
    int COST_ADDSHIFT_SMALL;
    int COST_SHIFT;
    int SMALL_SHIFT_MAXBITS;
    
    MulMethod(bool ipf) : len(0), foripf(ipf) {
        if (ipf) {
            COST_ADDSHIFT_SMALL = 1;
            COST_SHIFT = 1;
            SMALL_SHIFT_MAXBITS = 4;
        } else {
            COST_ADDSHIFT_SMALL = 2;
            COST_SHIFT = 4;
            SMALL_SHIFT_MAXBITS = 3;
        }
    };

    void append(MulOp::Op op0) { 
        assert((0<=op0)&&(op0<MulOp::numMulOps)); 
        assert(numMulOpnds[op0]==0); 
        data[len] = op0; len += 1; 
    };
    void append(MulOp::Op op0op, int opnd0) { 
        int op0 = (int) op0op;
        assert((0<=op0)&&(op0<MulOp::numMulOps)); 
        assert(numMulOpnds[op0]==1); 
        data[len] = op0; data[len+1] = opnd0; len += 2; 
    };

    void makespace(int k) {
        assert(len+k <= methodLen);
        assert(k>0);
        for (int i=len+k; i>k; i--) {
            data[i] = data[i-k];
        }
        len += k;
    };
    void prepend(MulOp::Op op0) {
        assert((0<=op0)&&(op0<MulOp::numMulOps)); 
        assert(numMulOpnds[op0]==0); 
        makespace(1); data[0] = op0;
    };
    void prepend(MulOp::Op op0, int opnd0) {
        assert((0<=op0)&&(op0<MulOp::numMulOps)); 
        assert(numMulOpnds[op0]==1); 
        makespace(2); data[0] = op0; data[1] = opnd0;
    };

    void append(const MulMethod &other) {
        assert(len+other.len <= methodLen);
        for (int i=0; i < other.len; i++) {
            data[len+i] = other.data[i];
        }
        len += other.len;
    }
    void prepend(const MulMethod &other) {
        makespace(other.len);
        for (int i=0; i < other.len; i++) {
            data[i] = other.data[i];
        }
    }

    int apply(int y, int *latency = 0) {
        int thestack[stackDepth];
        int when[stackDepth];
        thestack[0] = 0xdeadbeef;
        when[0] = -1;
        int ip = 0;
        int sp = 0;
        while (ip < len) {
            enum MulOp::Op op = (enum MulOp::Op) data[ip];
            ip += 1;
            assert(sp < stackDepth);
            switch (op) {
            case MulOp::pushc: assert(ip<len); thestack[sp] = data[ip++]; when[sp] = 0; sp += 1; break;
            case MulOp::pushy: thestack[sp] = y; when[sp] = COST_LOADY; sp += 1; break;
            case MulOp::swap: assert(sp >= 2); ::std::swap(thestack[sp-1], thestack[sp-2]); ::std::swap(when[sp-1], when[sp-2]); break;
            case MulOp::dup: assert(sp >= 1); thestack[sp] = thestack[sp-1]; when[sp] = when[sp-1]; sp += 1; break;
            case MulOp::dup2: assert(sp >= 2); thestack[sp] = thestack[sp-2]; when[sp] = when[sp-2]; sp += 1; break;
            case MulOp::dup3: assert(sp >= 2); thestack[sp] = thestack[sp-3]; when[sp] = when[sp-3]; sp += 1; break;
            case MulOp::add: assert(sp >= 2); 
                thestack[sp-2] = thestack[sp-2] + thestack[sp-1]; 
                when[sp-2] = ::std::max(when[sp-2],when[sp-1]) + COST_ADD;
                sp -=1; 
                break;
            case MulOp::sub: assert(sp >= 2); 
                thestack[sp-2] = thestack[sp-2] - thestack[sp-1]; 
                when[sp-2] = ::std::max(when[sp-2],when[sp-1]) + COST_SUB;
                sp -=1; break;
            case MulOp::shladd: assert(sp >= 2); 
                assert(ip<len);
                assert(data[ip] <= SMALL_SHIFT_MAXBITS);
                thestack[sp-2] = (thestack[sp-2] << data[ip++]) + thestack[sp-1]; 
                when[sp-2] = ::std::max(when[sp-2],
                                      when[sp-1]) + COST_ADDSHIFT_SMALL;
                sp -=1; break;
            case MulOp::neg: assert(sp >= 1); 
                thestack[sp-1] = -thestack[sp-1];
                when[sp-1] = when[sp-1] + COST_NEG;
                break;
            case MulOp::shiftl: assert(sp >= 1); 
                assert(ip<len);
                thestack[sp-1] = thestack[sp-1] << data[ip++];
                when[sp-1] = when[sp-1]+ COST_SHIFT;
                break;
            default:
                assert(0);
            }
        }
        if (sp != 1) {
            ::std::cerr << ::std::endl;
            ::std::cerr << "sp != 1 after applying: ";
            printOps(::std::cerr); ::std::cerr << ::std::endl;
            assert(0);
        }
        if (latency) { *latency = when[0]; }
        return thestack[0];
    };
    void printOps(::std::ostream &outs) const {
        int ip = 0;
        while (ip < len) {
            enum MulOp::Op op = (enum MulOp::Op) data[ip];
            ip += 1;
            switch (op) {
            case MulOp::pushc: outs << "push " << data[ip++] << "; "; break;
            case MulOp::pushy: outs << "push y; "; break;
            case MulOp::swap: outs << "swap; "; break;
            case MulOp::dup: outs << "dup; "; break;
            case MulOp::dup2: outs << "dup2; "; break;
            case MulOp::dup3: outs << "dup3; "; break;
            case MulOp::add: outs << "add; "; break;
            case MulOp::sub: outs << "sub; "; break;
            case MulOp::shladd: outs << "shladd " << data[ip++] << "; "; break;
            case MulOp::neg: outs << "neg; "; break;
            case MulOp::shiftl: outs << "shiftl " << data[ip++] << "; "; break;
            default: break;
            }
        }
    }

    enum What { IsConstant, IsY, IsSub };
    static void printSub(::std::ostream &outs, int onstack, enum What what) {
        switch (what) {
        case IsConstant: outs << onstack; break;
        case IsY: outs << "Y"; break;
        case IsSub: outs << "t" << onstack; break;
        default: assert(0);
            
        }
    }
    void print(::std::ostream &outs) const {
        int thestack[stackDepth];
        What what[stackDepth];
        thestack[0] = 0xdeadbeef;
        what[0] = IsConstant;
        int ip = 0;
        int sp = 0;
        int subnum = 0;
        if (len == 0)
            return; // no reduction
        while (ip < len) {
            enum MulOp::Op op = (enum MulOp::Op) data[ip];
            ip += 1;
            assert(sp < stackDepth);
            switch (op) {
            case MulOp::pushc: assert(ip<len); thestack[sp] = data[ip++]; what[sp] = IsConstant; sp += 1; break;
            case MulOp::pushy: thestack[sp] = 0; what[sp] = IsY; sp += 1; break;
            case MulOp::swap: assert(sp >= 2); ::std::swap(thestack[sp-1], thestack[sp-2]); ::std::swap(what[sp-1], what[sp-2]); break;
            case MulOp::dup: assert(sp >= 1); thestack[sp] = thestack[sp-1]; what[sp] = what[sp-1]; sp += 1; break;
            case MulOp::dup2: assert(sp >= 2); thestack[sp] = thestack[sp-2]; what[sp] = what[sp-2]; sp += 1; break;
            case MulOp::dup3: assert(sp >= 2); thestack[sp] = thestack[sp-3]; what[sp] = what[sp-3]; sp += 1; break;
            case MulOp::add: 
                {
                    assert(sp >= 2); 
                    int thissub = ++subnum;
                    outs << "t" << thissub << " = add ";
                    printSub(outs, thestack[sp-2], what[sp-2]); outs << ", ";
                    printSub(outs, thestack[sp-1], what[sp-1]); outs << "; ";
                    thestack[sp-2] = thissub; what[sp-2] = IsSub; sp -=1; 
                    break;
                }
            case MulOp::sub: 
                {
                    assert(sp >= 2); 
                    int thissub = ++subnum;
                    outs << "t" << thissub << " = sub ";
                    printSub(outs, thestack[sp-2], what[sp-2]); outs << ", ";
                    printSub(outs, thestack[sp-1], what[sp-1]); outs << "; ";
                    thestack[sp-2] = thissub; what[sp-2] = IsSub; sp -=1; 
                    break;
                }
            case MulOp::shladd: 
                {
                    assert(sp >= 2); 
                    assert(ip<len);
                    assert(data[ip] <= SMALL_SHIFT_MAXBITS);
                    int thissub = ++subnum;
                    outs << "t" << thissub << " = shladd ";
                    printSub(outs, thestack[sp-2], what[sp-2]); 
                    outs << ", " << data[ip++] << ", ";
                    printSub(outs, thestack[sp-1], what[sp-1]); outs << "; ";
                    thestack[sp-2] = thissub; what[sp-2] = IsSub; sp -=1; 
                    break;
                }
            case MulOp::neg: 
                {
                    assert(sp >= 1); 
                    int thissub = ++subnum;
                    outs << "t" << thissub << " = neg ";
                    printSub(outs, thestack[sp-1], what[sp-1]); outs << "; ";
                    thestack[sp-1] = thissub; what[sp-1] = IsSub;
                    break;
                }
            case MulOp::shiftl: 
                {
                    assert(sp >= 1); 
                    assert(ip<len);
                    int thissub = ++subnum;
                    outs << "t" << thissub << " = shiftl ";
                    printSub(outs, thestack[sp-1], what[sp-1]); outs << ", " << data[ip++] << "; ";
                    thestack[sp-1] = thissub; what[sp-1] = IsSub;
                    break;
                }
            default:
                assert(0);
            }
        }
        if (sp != 1) {
            ::std::cerr << ::std::endl;
            ::std::cerr << "sp != 1 after applying: ";
            printOps(::std::cerr); ::std::cerr << ::std::endl;
            assert(0);
        }

        outs << "r = ";
        printSub(outs, thestack[sp-1], what[sp-1]);
    };

    int getCost() {
        if (len == 0) {
            return 10000;
        } else {
            int latency;
            apply(13, &latency);
            return latency;
        }
    }
#ifndef STANDALONE_TEST
    Opnd *genCode(Type *type, Simplifier *simp, Opnd *y)
    {
        Opnd *thestack[stackDepth];
        Type::Tag tag = y->getType()->tag;
        bool width32 = ((tag == Type::Int32) || (tag == Type::UInt32));
        thestack[0] = 0;
        int ip = 0;
        int sp = 0;
        if (len == 0)
            return NULL; // no reduction
        while (ip < len) {
            enum MulOp::Op op = (enum MulOp::Op) data[ip];
            ip += 1;
            assert(sp < stackDepth);
            switch (op) {
            case MulOp::pushc: {
                assert(ip<len); 
                int val = data[ip]; ip += 1;
                Opnd *op = (width32 
                            ? simp->genLdConstant((I_32)(val))->getDst()
                            : simp->genLdConstant((int64)(val))->getDst());
                thestack[sp] = op;
                sp += 1;
            } break;
            case MulOp::pushy: thestack[sp] = y; sp += 1; break;
            case MulOp::swap: assert(sp >= 2); ::std::swap(thestack[sp-1], thestack[sp-2]); break;
            case MulOp::dup: assert(sp >= 1); thestack[sp] = thestack[sp-1]; sp += 1; break;
            case MulOp::dup2: assert(sp >= 2); thestack[sp] = thestack[sp-2]; sp += 1; break;
            case MulOp::dup3: assert(sp >= 2); thestack[sp] = thestack[sp-3]; sp += 1; break;
            case MulOp::add: assert(sp >= 2); 
                thestack[sp-2] = simp->genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), thestack[sp-2], thestack[sp-1])->getDst();
                sp -=1; 
                break;
            case MulOp::sub: assert(sp >= 2); 
                thestack[sp-2] = simp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), thestack[sp-2], thestack[sp-1])->getDst(); 
                sp -=1; break;
            case MulOp::shladd: { 
                assert(sp >= 2); 
                assert(ip<len);
                int val = data[ip]; ip += 1;
                assert(val <= SMALL_SHIFT_MAXBITS);
                Opnd *op = simp->genLdConstant((I_32)(val))->getDst();
                thestack[sp-2] = simp->genShladd(type, thestack[sp-2], op, thestack[sp-1])->getDst(); 
                sp -=1; } break;
            case MulOp::neg: assert(sp >= 1); 
                thestack[sp-1] = simp->genNeg(type, thestack[sp-1])->getDst();
                break;
            case MulOp::shiftl: {
                assert(sp >= 1); 
                assert(ip<len);
                int val = data[ip]; ip += 1;
                Opnd *op = simp->genLdConstant((I_32)(val))->getDst();
                thestack[sp-1] = simp->genShl(type, ShiftMask_None,
                                              thestack[sp-1], op)->getDst();
            } break;
            default:
                assert(0);
            }
        }
        
        assert(sp == 1);
        return thestack[0];
    }
#endif // !STANDALONE_TEST

};

template <typename inttype, int width>
bool testMul(MulMethod &method, inttype d, inttype num);

template <typename inttype, int width>
void planMulCompound(MulMethod &m, inttype d, int depth);

template <typename inttype, int width>
void planMulLookup(MulMethod &m, inttype d, int depth);

template <typename inttype, int width>
void planMulFactor(MulMethod &m, inttype d, int depth);

template <typename inttype, int width>
void planMulLarge(MulMethod &m, inttype d, int depth);

template <typename inttype, int width>
void planMulNeg(MulMethod &m, inttype d, int depth);

template <typename inttype, int width>
void planMul(MulMethod &m, inttype d, int depth);

template <typename inttype, int width>
void planMul(MulMethod &m, inttype d, int depth) {
    DEBUGPRINT("planMul(" << (int)d << ")");
    if (d == -1) {
        m.append(MulOp::pushy); m.append(MulOp::neg);
    } else if (d == 1) {
        m.append(MulOp::pushy);
    } else if (d == 0) {
        m.append(MulOp::pushc, 0);
    } else if (d == -d) {
        int k = width - 1;
        m.append(MulOp::pushy); m.append(MulOp::shiftl, k);
    } else {
    /* ONLY DO REDUCTION FOR THE TWO CASES */
        if ((d < 32) && (d > 0)) {
            planMulLookup<inttype, width>(m, d, depth+1);
        } else if (isPowerOf2(d)) {
            int k = whichPowerOf2<inttype, width>(d);
            m.append(MulOp::pushy); m.append(MulOp::shiftl, k);
        }
    /* 
        if (d < 0) {
            planMulNeg<inttype, width>(m, d, depth);
        } else if (d < 32) {
            planMulLookup<inttype, width>(m, d, depth+1);
        } else if (isPowerOf2(d)) {
            int k = whichPowerOf2<inttype, width>(d);
            m.append(MulOp::pushy); m.append(MulOp::shiftl, k);
        } else {
            planMulLarge<inttype, width>(m, d, depth+1);
        }
    */
    }
#ifdef TEST_OUTPUT
    DEBUGPRINT2("planMul(" << d << ")", res);
    for (int i=0; i<10000; i += 17) {
        inttype num = i;
        if (!(testMul<inttype, width>(m, res, d, num) &&
              testMul<inttype, width>(m, res, d, -num) &&
              testMul<inttype, width>(m, res, d, 0x7fffffff+num) &&
              testMul<inttype, width>(m, res, d, 0x7fffffff-num))) {
            return;
        }
    }
#endif
}

template <typename inttype, int width>
void planMulNeg(MulMethod &m, inttype d, int depth) {
    DEBUGPRINT("planMulNeg(" << (int)d << ")");

    if (depth == 1) {
        // this seems to win by 2 cycles only in about 2/25000 cases
        // and by 1 cycle only in about 1/50 cases
        DEBUGPRINT("planMulNeg(" << (int)d << ") case 1");
        MulMethod res1(m.foripf);
        planMul<inttype, width>(res1, -d, depth+1); res1.append(MulOp::neg);

        // this seems to win by 2 cycles in about 1/250 cases
        // and by 1 cycle in 1/10 cases
        // but that's not enough to justify a search
        DEBUGPRINT("planMulNeg(" << (int) d << ") case 2");
        MulMethod res2(m.foripf);
        inttype dinv = ~d;
        int numones = nlz<inttype, width>(dinv);
        int shiftby = width - numones;
        inttype newd = d + ((inttype)1 << shiftby);
        if (shiftby <= m.SMALL_SHIFT_MAXBITS) {
            DEBUGPRINT("planMulNeg(" << (int) d << ") case 2a, shiftby=" << shiftby);
            res2.append(MulOp::pushy); res2.append(MulOp::neg);
            planMul<inttype, width>(res2, newd, depth+1);
            res2.append(MulOp::shladd, shiftby);
        } else {
            DEBUGPRINT("planMulNeg(" << (int) d << ") case 2b, shiftby=" << shiftby);
            planMul<inttype, width>(res2, newd, depth+1);
            res2.append(MulOp::pushy); res2.append(MulOp::shiftl, shiftby);
            res2.append(MulOp::sub);
        }    

        DEBUGPRINT("planMulNeg(" << (int) d << ") case 3");
        MulMethod res3(m.foripf);
        planMulCompound<inttype, width>(res3, d, depth+1);
        
        int latency1 = res1.getCost();
        int latency2 = res2.getCost();
        int latency3 = res3.getCost();
        if ((latency1 <= latency2) && (latency1 <= latency3)) {
            if (latency2 < latency3) {
                DEBUGPRINT("choose neg method 1 by " << latency2-latency1 << " over 2");
            } else if (latency3 < latency2) {
                DEBUGPRINT("choose neg method 1 by " << latency3-latency1 << " over 3");
            } else {
                DEBUGPRINT("choose neg method 1 by " << latency2-latency1 << " over 2 & 3");
            }
            m.append(res1);
        } else if (latency2 < latency3) {
            if (latency1 < latency3) {
                DEBUGPRINT("choose neg method 2 by " << latency1 - latency2 << " over 1");
            } if (latency1 > latency3) {
                DEBUGPRINT("choose neg method 2 by " << latency3 - latency2 << " over 3");
            } else {
                DEBUGPRINT("choose neg method 2 by " << latency3 - latency2 << " over 1 & 3");
            }
            m.append(res2);
        } else {
            if (latency1 < latency2) {
                DEBUGPRINT("choose neg method 3 by " << latency1 - latency3 << " over 1");
            } if (latency1 > latency2) {
                DEBUGPRINT("choose neg method 3 by " << latency2 - latency3 << " over 2");
            } else {
                DEBUGPRINT("choose neg method 3 by " << latency2 - latency3 << " over 1 & 2");
            }
            m.append(res3);
        }
    } else {
        DEBUGPRINT("planMulNeg(" << (int) d << ") case 3a");
        planMulCompound<inttype, width>(m, d, depth+1);
    }

    DEBUGPRINT("done planMulNeg(" << (int) d << ")");
}

template <typename inttype, int width>
void planMulFactor(MulMethod &m, inttype d, int depth)
{
    DEBUGPRINT("planMulFactor(" << (int) d << ")");
    MulMethod res2(m.foripf);
    int shiftby = m.SMALL_SHIFT_MAXBITS;
    inttype factor = (1 << shiftby) + 1;

    // small factors
    while (factor > 2) {
        if ((d % factor) == 0) {
            DEBUGPRINT("planMulFactor(" << (int) d << ") found factor " << (int) factor);
            MulMethod res1(m.foripf);
            inttype quot = d / factor;
            planMul<inttype, width>(res1, quot, depth+1);
            res1.append(MulOp::dup);
            res1.append(MulOp::shladd, shiftby);
            m.append(res1); 
            return;
        }
        factor = (factor >> 1) + 1;
        shiftby -= 1;
    }

    // bigger factors
    shiftby = width - 2;
    factor = (1 << shiftby) + 1;
    while (factor > (1 << m.SMALL_SHIFT_MAXBITS) + 1) {
        if ((d % factor) == 0) {
            DEBUGPRINT("planMulFactor(" << (int) d << ") found factor " << (int) factor);
            MulMethod res1(m.foripf);
            inttype quot = d / factor;
            planMul<inttype, width>(res1, quot, depth+1);
            res1.append(MulOp::dup);
            res1.append(MulOp::shiftl, shiftby);
            res1.append(MulOp::add);
            m.append(res1); 
            return;
        }
        factor = (factor >> 1) + 1;
        shiftby -= 1;
    }

    // bigger factors
    shiftby = width - 2;
    factor = (1 << (int) shiftby) - 1;
    while (factor > 3) {
        if ((d % factor) == 0) {
            MulMethod res1(m.foripf);
            if (d > 0) {
                DEBUGPRINT("planMulFactor(" << (int) d << ") found factor " << (int) factor);
                inttype quot = d / factor;
                planMul<inttype, width>(res1, quot, depth+1);
                res1.append(MulOp::dup);
                res1.append(MulOp::shiftl, shiftby);
                res1.append(MulOp::swap);
                res1.append(MulOp::sub);
                m.append(res1); 
                return;
            } else {
                DEBUGPRINT("planMulFactor(" << (int) d << ") found factor " << (int) -factor);
                inttype quot = -d / factor;
                planMul<inttype, width>(res1, quot, depth+1);
                res1.append(MulOp::dup);
                res1.append(MulOp::shiftl, shiftby);
                res1.append(MulOp::sub);
                m.append(res1); 
                return;
            }
        }
        factor = ((factor+1) >> 1) - 1;
        shiftby -= 1;
    }

    DEBUGPRINT("done planMulFactor(" << (int) d << ")");
    m.append(res2);
}

template <typename inttype, int width>
void planMulLarge(MulMethod &m, inttype d, int depth)
{
    if (depth < 6) {
        MulMethod res1(m.foripf);
        planMulCompound<inttype, width>(res1, d, depth);
        
        MulMethod res2(m.foripf);
        planMulFactor<inttype, width>(res2, d, depth);
        
        int latency1 = res1.getCost();
        int latency2 = res2.getCost();
        if (latency1 <= latency2) {
            DEBUGPRINT("choose large method 1: compound");
            m.append(res1);
        } else {
            DEBUGPRINT("choose large method 2: factor");
            m.append(res2);
        }
    } else {
        planMulCompound<inttype, width>(m, d, depth);
    }
}

template <typename inttype, int width>
void planMulCompound(MulMethod &m, inttype d, int depth) {
    assert((d < 0) || (d > 31));
    DEBUGPRINT("planMulCompound(" << (int) d << ")");

    inttype delta = d ^ (d >> 1); // bits differing from from bit to left
    int numChanges = popcount<inttype, width>(delta);
    int deltaright = ntz<inttype, width>(delta);
    int deltaleft = nlz<inttype, width>(delta);

    int bitswidth = width - deltaright - deltaleft - 1;
    int small_shift_maxbits = m.SMALL_SHIFT_MAXBITS;

    if ((numChanges < 4) ||
        (bitswidth <= 5)) { // small enough to worry about sign/rightbit issues
        DEBUGPRINT("case few changes");
        // we assume we've dealt with <31 elsewhere, so we must have some shifting ahead of us.
        if ((d & 1) == 1) {
            inttype dinv = ~d;
            DEBUGPRINT("case odd");
            int rightones = ntz<inttype, width>(dinv);
            if (rightones == 1) {
                inttype dsub1 = d-1;
                int dsub1rightzeros = ntz<inttype, width>(dsub1);
                if (dsub1rightzeros > small_shift_maxbits) {
                    planMul<inttype, width>(m, 
                                            dsub1 >> small_shift_maxbits, 
                                            depth+1); 
                    m.append(MulOp::pushy); 
                    m.append(MulOp::shladd, small_shift_maxbits);
                } else {
                    planMul<inttype, width>(m, 
                                            dsub1 >> dsub1rightzeros, 
                                            depth+1); 
                    m.append(MulOp::pushy); 
                    m.append(MulOp::shladd, dsub1rightzeros);
                }
            } else if (rightones <= small_shift_maxbits) {
                planMul<inttype, width>(m, (d+1)>>rightones, depth+1); 
                m.append(MulOp::pushy); 
                m.append(MulOp::neg);
                m.append(MulOp::shladd, rightones);
            } else {
                planMul<inttype, width>(m, (d+1)>>small_shift_maxbits, 
                                        depth+1); 
                m.append(MulOp::pushy); m.append(MulOp::neg);
                m.append(MulOp::shladd, small_shift_maxbits);
            }
        } else {
            DEBUGPRINT("case even");
            int rightzeros = deltaright+1;
            assert((rightzeros == ntz<inttype, width>(d)));
            if (bitswidth <= 5) {
                DEBUGPRINT("case narrow");
                if ((rightzeros <= small_shift_maxbits) &&
                    (rightzeros + bitswidth >= 2 * small_shift_maxbits)) {
                    DEBUGPRINT("case smallshiftadd");
                    inttype newd = d + ((inttype)1 << rightzeros);
                    m.append(MulOp::pushy); m.append(MulOp::neg); 
                    planMul<inttype, width>(m, newd, depth+1); 
                    m.append(MulOp::shladd, rightzeros);
                } else {
                    inttype newd = d >> rightzeros;
                    DEBUGPRINT("case smallshift");
                    planMul<inttype, width>(m, newd, depth+1); 
                    m.append(MulOp::shiftl, rightzeros);
                }
            } else {
                DEBUGPRINT("case even, wider");
                inttype deltasubright = delta - ((inttype)1 << deltaright);
                int deltasubright_ntz = ntz<inttype, width>(deltasubright);
                int region2 = deltasubright_ntz - deltaright +1;
                assert(region2 > 0);
                if (region2 > 2) {
                    inttype newd = d + ((inttype)1 << rightzeros);
                    if (rightzeros <= small_shift_maxbits) {
                        DEBUGPRINT("case wide smalladdshift");
                        m.append(MulOp::pushy); m.append(MulOp::neg);
                        planMul<inttype, width>(m, newd, depth+1);
                        m.append(MulOp::shladd, rightzeros);
                    } else {
                        DEBUGPRINT("case wide sub of shift");
                        planMul<inttype, width>(m, newd, depth+1);
                        m.append(MulOp::pushy); m.append(MulOp::shiftl, rightzeros);
                        m.append(MulOp::sub);
                    }
                } else if (region2 == 2) {
                    inttype newd = d - (3 << rightzeros);
                    DEBUGPRINT("case wide addshift3");
                    if (rightzeros <= small_shift_maxbits) {
                        m.append(MulOp::pushy); m.append(MulOp::pushy); 
                        m.append(MulOp::shladd, 1);
                        planMul<inttype, width>(m, newd, depth+1);
                        m.append(MulOp::shladd, rightzeros);
                    } else {
                        DEBUGPRINT("case wide add shift3");
                        planMul<inttype, width>(m, newd >> small_shift_maxbits,
                                                depth+1);
                        m.append(MulOp::pushy); m.append(MulOp::pushy); 
                        m.append(MulOp::shladd, 1);
                        m.append(MulOp::shiftl, rightzeros);
                        m.append(MulOp::shladd, small_shift_maxbits);
                    }
                } else { // region2 == 1
                    inttype newd = d - ((inttype)1 << rightzeros);
                    if (rightzeros <= small_shift_maxbits) {
                        DEBUGPRINT("case wide smalladdshift1");
                        m.append(MulOp::pushy);
                        planMul<inttype, width>(m, newd, depth+1);
                        m.append(MulOp::shladd, rightzeros);
                    } else {
                        DEBUGPRINT("case wide sub of shift");
                        planMul<inttype, width>(m, newd >> small_shift_maxbits,
                                                depth+1);
                        m.append(MulOp::pushy);
                        m.append(MulOp::shiftl, rightzeros);
                        m.append(MulOp::shladd, small_shift_maxbits);
                    }
                }
            }
        }
    } else { // bitswidth > 5
        assert(numChanges >= 2);
        DEBUGPRINT("case wide");

        if (numChanges == 2) {
            DEBUGPRINT("case numChanges==2");
            if ((numChanges&1)==0) {
                DEBUGPRINT("case wide changes=2 even");
                int rightzeros = deltaright+1;
                int leftzeros = deltaleft;
                assert((rightzeros == ntz<inttype, width>(d)));
                assert((leftzeros == nlz<inttype, width>(d)));
                if (rightzeros <= small_shift_maxbits) {
                    DEBUGPRINT("case wide smallshiftadd");
                    assert(rightzeros+bitswidth >= 2 * small_shift_maxbits);
                    m.append(MulOp::pushy); m.append(MulOp::neg);
                    m.append(MulOp::pushy); m.append(MulOp::shiftl, width - leftzeros);
                    m.append(MulOp::shladd, rightzeros);
                } else {
                    DEBUGPRINT("case wide narrow sub");
                    m.append(MulOp::pushy); m.append(MulOp::shiftl, width - leftzeros);
                    m.append(MulOp::pushy); m.append(MulOp::shiftl, rightzeros);
                    m.append(MulOp::sub);
                }
            } else {
                DEBUGPRINT("case wide changes=2 odd");
            }
        } else {
            // first try: use point width/2 to split number
            DEBUGPRINT("case numChanges>2");
            
            int rightzeros = ((d & 1)!=0) ? 0 : deltaright+1;
#ifndef NDEBUG
            int leftzeros = ((d & ((inttype)1<<(width-1))) != 0) ? 0 : deltaleft;
#endif
            assert((rightzeros == ntz<inttype, width>(d)));
            assert((leftzeros == nlz<inttype, width>(d)));

            int k = (numChanges + 1) >> 2;
            assert(k > 0); // we had numChanges>=3 above.
            inttype stripright = d;
            while (k > 0) {
                // turn rightmost group of 0s into 1s, no effect if none
                inttype striprightzeros = stripright | (stripright - 1);
                // turn rightmost group of 1s into 0s, must be some
                inttype srzinv = ~striprightzeros; // first invert
                inttype sroinv = srzinv | (srzinv - 1); // strip 0s
                stripright = ~sroinv; // invert back
                k -= 1;
            }
            // now stripright is d but with 0s from about the middle
            // of changes to the right
            // find a point one place to the left of the set of 0s
            int zeropoint = ntz<inttype, width>(stripright);
            inttype zerobit = (inttype(1))<<zeropoint;

            assert((d & zerobit) != 0);
            assert((d & (zerobit>>1)) == 0);

            DEBUGPRINT("zerobit is " << (int) zerobit);
            DEBUGPRINT("zeropoint is " << (int) zeropoint);
            inttype rightmask  = zerobit - 1;
            inttype leftmask  = ~rightmask;
            DEBUGPRINT("leftmask is " << (int) leftmask << ", rightmask is " << (int) rightmask);
            
            inttype rightd = d & rightmask;
            inttype leftd = d & leftmask;
            
            DEBUGPRINT("leftd is " << (int) leftd << ", rightd is " << (int) rightd);
            
            int leftdrightzeros = ntz<inttype, width>(leftd);
            
            if (rightzeros == 0) {
                DEBUGPRINT("case 0");
                if (leftdrightzeros < small_shift_maxbits) {
                    DEBUGPRINT("case 0a");
                    planMul<inttype, width>(m, leftd >> leftdrightzeros, 
                                            depth+1);
                    planMul<inttype, width>(m, rightd, depth+1);
                    m.append(MulOp::shladd, leftdrightzeros);
                } else {
                    planMul<inttype, width>(m, leftd, depth+1);
                    planMul<inttype, width>(m, rightd, depth+1);
                    m.append(MulOp::add);
                }
            } else if ((0 < rightzeros) && (rightzeros <= small_shift_maxbits)) {
                DEBUGPRINT("case 1");
                planMul<inttype, width>(m, rightd >> rightzeros, depth+1);
                planMul<inttype, width>(m, leftd, depth+1);
                m.append(MulOp::shladd, rightzeros);
            } else {
                DEBUGPRINT("case 3");
                planMul<inttype, width>(m, leftd >> small_shift_maxbits, 
                                        depth+1);
                planMul<inttype, width>(m, rightd, depth+1);
                m.append(MulOp::shladd, small_shift_maxbits);
            }
            DEBUGPRINT("done");
        }
    }
}

template <typename inttype, int width>
void planMulLookup(MulMethod &m, inttype d, int depth) {
    assert((d>=0) && (d < 32));
    DEBUGPRINT("planMulLookup(" << (int) d << ")");
    switch (d) {
    case 0: m.append(MulOp::pushc, 0); break;
    case 1: m.append(MulOp::pushy); break;
    case 2: m.append(MulOp::pushy); m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 3: m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1); break;
    case 4: m.append(MulOp::pushy); m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 2); break;
    case 5: m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2); break;
    case 6: 
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 7:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1); break;
    case 8: m.append(MulOp::pushy); m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 3); break;
    case 9: m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 3); break;
    case 10:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 11:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1); break;
    case 12:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 2); break;
    case 13:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 2); break;
    case 14:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 15:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::dup); m.append(MulOp::shladd, 2); break;
    case 16:
        m.append(MulOp::pushy); 
        if (m.SMALL_SHIFT_MAXBITS == 4) {
            m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 4);
        } else if (m.SMALL_SHIFT_MAXBITS == 3) {
            m.append(MulOp::shiftl, 4);
        } else {
            assert(0);
        }
        break;
    case 17:
        m.append(MulOp::pushy); m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 3);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1);
        break;
    case 18:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 3); 
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 19:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 3); 
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1); break;
    case 20:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2); 
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 2); break;
    case 21:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2); 
        m.append(MulOp::pushy); m.append(MulOp::shladd, 2); break;
    case 22:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 23:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1); break;
    case 24:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 3); break;
    case 25:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 3); break;
    case 26:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 27:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::dup); m.append(MulOp::shladd, 3); break;
    case 28:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 2); break;
    case 29:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 2); break;
    case 30:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::dup); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushc, 0); m.append(MulOp::shladd, 1); break;
    case 31:
        m.append(MulOp::pushy); m.append(MulOp::dup); m.append(MulOp::shladd, 1);
        m.append(MulOp::dup); m.append(MulOp::shladd, 2);
        m.append(MulOp::pushy); m.append(MulOp::shladd, 1); break;
    default:
        assert(0);
    }
}

#ifdef STANDALONE_TEST

#include "testharness2.h"

#else // !STANDALONE_TEST
Opnd *
Simplifier::planMul32(I_32 multiplier, Opnd *opnd)
{
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    MulMethod method(!optimizerFlags.ia32_code_gen);
    planMul<I_32, 32>(method, multiplier, 1);
    if (Log::isEnabled()) {
        Log::out() << "in multiply(" << (int) multiplier << ", ";
        opnd->print(Log::out());
        Log::out() << "), method is ";
        method.print(Log::out()); Log::out() << ::std::endl;
    }
    return method.genCode(opnd->getType(), this, opnd);
}

Opnd *
Simplifier::planMul64(int64 multiplier, Opnd *opnd)
{
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    MulMethod method(!optimizerFlags.ia32_code_gen);
    planMul<int64, 64>(method, multiplier, 1);
    if (Log::isEnabled()) {
        Log::out() << "in multiply(" << (int) multiplier << ", ";
        opnd->print(Log::out());
        Log::out() << "), method is ";
        method.print(Log::out()); Log::out() << ::std::endl;
    }
    return method.genCode(opnd->getType(), this, opnd);
}

#endif

} //namespace Jitrino 
