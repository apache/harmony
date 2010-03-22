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

#include <iostream>
#include "Opcode.h"
#include "optarithmetic.h"
#include "Log.h"
#include "optimizer.h"

namespace Jitrino {


// new MODIFIER system
//   to allow some checking of modifiers and opcodes, an Operation value is checked
//   at construction time to make sure that the modifiers appropriate to the Opcode
//   are present.
//
// To add a new Modifier, use "NewModifier1" and "NewModifier2" as a model, 
// but please leave them there as models for others.  See Opcode.h as well;
// there, numbers (_IsShiftedBy and Modifier::Kind values) should be adjusted after adding
// the new modifier before Modifier1
//
// Next, any instruction which uses the modifier must have the modifier as part of its
// modifierKind field in opcodeTable.  Unfortunately, due to typing issues when there are
// several modifiers present, the combination must be defined in Modifer::Kind.  See
// the definition of Overflow_and_Exception_and_Strict in Opcode.h for an example.
//
// (There may be a way to abstract some of the numeric constants, but I was hitted by a compiler
// issues with a more elegant solution and backed off to this.)

struct Mobility {
    enum Kind {
        None,           // phis, labels, pis, and the like
        Movable,        // totally movable (but must also check for Overflow modifier)
        CSEable,        // can be CSEed
        Check,          // see also 
        Exception,      // can generate an exception
        StoreOrSync,    // affects memory state
        Call,           // may end a block, may have exception, may affect memory state
        ControlFlow,    // is control flow
        Load,           // is a load
        MustEndBlock    // --- not used
    };
};

typedef Modifier::Kind MK;
typedef Mobility MB;

struct OpcodeInfo {
    Opcode opcode;
    bool essential;
    Mobility::Kind mobility;
    MK::Enum modifierKind;
    const char *opcodeString;
    const char *opcodeFormatString;
};

static OpcodeInfo opcodeTable[] = {
    { Op_Add,                   false, MB::Movable,       MK::Overflow_and_Exception_and_Strict,     "add   ",        "add%m   %s -) %l"             },
    { Op_Mul,                   false, MB::Movable,       MK::Overflow_and_Exception_and_Strict,     "mul   ",        "mul%m   %s -) %l",             },
    { Op_Sub,                   false, MB::Movable,       MK::Overflow_and_Exception_and_Strict,     "sub   ",        "sub%m   %s -) %l",             },
    { Op_TauDiv,                false, MB::Movable,       MK::Signed_and_Strict,                     "div   ",        "div%m   %0,%1 ((%2)) -) %l",             }, // (opnds must already be checked for 0/overflow)
    { Op_TauRem,                false, MB::Movable,       MK::Signed_and_Strict,                     "rem   ",        "rem%m   %0,%1 ((%2)) -) %l",             }, // (opnds must already be checked for 0/overflow)
    { Op_Neg,                   false, MB::Movable,       MK::None,                                  "neg   ",        "neg       %s -) %l",           },
    { Op_MulHi,                 false, MB::Movable,       MK::Signed,                                "mulhi ",        "mulhi%m %s -) %l",             }, // SignedModifier (but only signed needed now)
    { Op_Min,                   false, MB::Movable,       MK::None,                                  "min   ",        "min       %s -) %l", },
    { Op_Max,                   false, MB::Movable,       MK::None,                                  "max   ",        "max       %s -) %l", },
    { Op_Abs,                   false, MB::Movable,       MK::None,                                  "abs   ",        "abs       %s -) %l", },
    { Op_And,                   false, MB::Movable,       MK::None,                                  "and   ",        "and       %s -) %l",           },
    { Op_Or,                    false, MB::Movable,       MK::None,                                  "or    ",        "or        %s -) %l",           },
    { Op_Xor,                   false, MB::Movable,       MK::None,                                  "xor   ",        "xor       %s -) %l",           },
    { Op_Not,                   false, MB::Movable,       MK::None,                                  "not   ",        "not       %s -) %l",           },
    { Op_Select,                false, MB::Movable,       MK::None,                                  "select",        "select %s -) %l",              }, // (src1 ? src2 : src3)
    { Op_Conv,                  false, MB::Movable,       MK::Overflow_and_Exception_and_Strict,     "conv  ",        "conv%t%m %s -) %l",            }, 
    { Op_ConvZE,                false, MB::Movable,       MK::Overflow_and_Exception_and_Strict,     "convze ",        "conv_ze_%t%m %s -) %l",            }, 
    { Op_ConvUnmanaged,         false, MB::StoreOrSync,   MK::Overflow_and_Exception_and_Strict,     "convu ",        "conv_unm_%t%m %s -) %l",           }, 
    { Op_Shladd,                false, MB::Movable,       MK::None,                                  "shladd",        "shladd %s -) %l",              }, // no mods, 2nd operand must be LdConstant
    { Op_Shl,                   false, MB::Movable,       MK::ShiftMask,                             "shl   ",        "shl%m  %s -) %l",              }, 
    { Op_Shr,                   false, MB::Movable,       MK::ShiftMask_and_Signed,                  "shr   ",        "shr%m  %s -) %l",              }, 
    { Op_Cmp,                   false, MB::Movable,       MK::Comparison,                            "cmp   ",        "c%m:%t %s -) %l",              }, 
    { Op_Cmp3,                  false, MB::Movable,       MK::Comparison,                            "cmp3  ",        "c3%m:%t %s -) %l",             }, // 3-way compare, e.g.: ((s0>s1)?1:((s1>s0)?-1:0))
    { Op_Branch,                true,  MB::ControlFlow,   MK::Comparison,                            "br    ",        "if c%m.%t %s goto %l",         }, 
    { Op_Jump,                  true,  MB::ControlFlow,   MK::None,                                  "jmp   ",        "goto %l",                      }, // (different from the CLI jmp opcode) 
    { Op_Switch,                true,  MB::ControlFlow,   MK::None,                                  "switch",        "switch (%l)[%0]",              },
    { Op_DirectCall,            true,  MB::Call,          MK::Exception,                             "call  ",        "call      %d(%p) ((%0,%1)) -) %l  %b",       },
    { Op_TauVirtualCall,        true,  MB::Call,          MK::Exception,                             "callvirt",      "callvrt   [%2.%d](%a) ((%0,%1)) -) %l  %b",  },
    { Op_IndirectCall,          true,  MB::Call,          MK::Exception,                             "calli",         "calli     [%0](%a) ((%1,%2)) -) %l",     },
    { Op_IndirectMemoryCall,    true,  MB::Call,          MK::Exception,                             "callimem",      "callimem  [%0](%a) ((%1,%2)) -) %l",     },
    { Op_JitHelperCall,         true,  MB::Call,          MK::Exception,                             "callhelper",    "callhelper %d(%s) -) %l",       },
    { Op_VMHelperCall,          true,  MB::Call,          MK::Exception,                             "callvmhelper",  "callvmhelper %d(%s) -) %l  %b",    },
    { Op_Return,                true,  MB::ControlFlow,   MK::None,                                  "return",        "return    %s",                 },
    { Op_Catch,                 true,  MB::ControlFlow,   MK::None,                                  "catch",         "catch        -) %l",           },
    { Op_Throw,                 true,  MB::Exception,     MK::Throw,                                 "throw ",        "throw     %0 %b",                 },               
    { Op_PseudoThrow,           true,  MB::Exception,     MK::Exception,                             "pseudoThrow ",  "pseudoThrow %b",                  },               
    { Op_ThrowSystemException,  true,  MB::Exception,     MK::None,                                  "throwsys ",     "throwsys %d %b",                  },
    { Op_ThrowLinkingException, true,  MB::Exception,     MK::None,                                  "throwLink ",    "throwLink",                    },
    { Op_JSR,                   true,  MB::Call,          MK::None,                                  "jsr",           "jsr %l",                       }, // Java only, JSR's -- DELETE
    { Op_Ret,                   true,  MB::ControlFlow,   MK::None,                                  "ret",           "ret       %s",                 }, // Java only, JSR's -- DELETE
    { Op_SaveRet,               true,  MB::ControlFlow,   MK::None,                                  "saveret",       "saveret      -) %l",           }, // Java only, JSR's -- DELETE
    // Move instruction                                                                                                
    { Op_Copy,                  false, MB::Movable,       MK::None,                                  "copy",          "copy      %s -) %l",           },
    { Op_DefArg,                true,  MB::None,          MK::DefArg,                                "defarg",        "defarg%m -) %l",               },
    // Load instructions                                                                                               
    { Op_LdConstant,            false, MB::Movable,       MK::None,                                  "ldc   ",        "ldc%t    #%c -) %l",           },
    { Op_LdRef,                 false, MB::Movable,       MK::AutoCompress,                          "ldref ",        "ldref%m (%d) -) %l  %b",       },
    { Op_LdVar,                 false, MB::None,          MK::None,                                  "ldvar ",        "ldvar     %0 -) %l",           },
    { Op_LdVarAddr,             false, MB::Movable,       MK::None,                                  "ldvara",        "ldvara    %0 -) %l",           },
    { Op_TauLdInd,              false, MB::Load,          MK::AutoCompress_Speculative,              "ldind",         "ldind%m:%t [%0] ((%1,%2)) -) %l",          },
    { Op_TauLdField,            false, MB::Load,          MK::AutoCompress,                          "ldfld",         "ldfld:%t [%0.%d] ((%1,%2)) -) %l",       },
    { Op_LdStatic,              false, MB::Load,          MK::AutoCompress,                          "ldsfld",        "ldsfld:%t [%d] -) %l",         },
    { Op_TauLdElem,             false, MB::Load,          MK::AutoCompress,                          "ldelem",        "ldelem:%t [%0[%1]] ((%2,%3)) -) %l",     },
    { Op_LdFieldAddr,           false, MB::Movable,       MK::None,                                  "ldflda",        "ldflda    [%0.%d] -) %l",      },
    { Op_LdStaticAddr,          false, MB::Movable,       MK::None,                                  "ldsflda",       "ldsflda   [%d] -) %l",         },
    { Op_LdElemAddr,            false, MB::Movable,       MK::None,                                  "ldelema",       "ldelema   [%0[%1]] -) %l",     },
    { Op_TauLdVTableAddr,       false, MB::Movable,       MK::None,                                  "ldvtable",      "ldvtable  %0 ((%1)) -) %l",           },
    { Op_TauLdIntfcVTableAddr,  false, MB::Movable,       MK::None,                                  "ldintfcvt",     "ldintfcvt %0,%d -) %l",        }, 
    { Op_TauLdVirtFunAddr,      false, MB::CSEable,       MK::None,                                  "ldvfn ",        "ldvfn     [%0.%d] ((%1)) -) %l",      },   
    { Op_TauLdVirtFunAddrSlot,  false, MB::CSEable,       MK::None,                                  "ldvfnslot",     "ldvfnslot [%0.%d] ((%1)) -) %l",      },
    { Op_LdFunAddr,             false, MB::CSEable,       MK::None,                                  "ldfn  ",        "ldfn      [%d] -) %l",         },
    { Op_LdFunAddrSlot,         false, MB::CSEable,       MK::None,                                  "ldfnslot",      "ldfnslot  [%d] -) %l",         },
    { Op_GetVTableAddr,         false, MB::Movable,       MK::None,                                  "getvtable",     "getvtable %d -) %l",           }, // obtains the address of the vtable for a particular object type
    { Op_GetClassObj,           false, MB::Movable,       MK::None,                                  "getclassobj",   "getclassobj %d -) %l",           }, // obtains the java.lang.class  object for a particular type
    { Op_TauArrayLen,           false, MB::CSEable,       MK::None,                                  "arraylen ",     "arraylen  %0 ((%1,%2)) -) %l",           },       
    { Op_LdArrayBaseAddr,       false, MB::CSEable,       MK::None,                                  "ldbase",        "ldbase    %s -) %l",           }, // load the base (zero'th element) address of array
    { Op_AddScaledIndex,        false, MB::Movable,       MK::None,                                  "addindex",      "addindex  %s -) %l",           }, // Add a scaled index to an array element address
    { Op_StVar,                 true,  MB::None,          MK::None,                                  "stvar ",        "stvar     %0 -) %l",           },
    { Op_TauStInd,              true,  MB::StoreOrSync,   MK::Store_AutoCompress,                    "stind",         "stind%m:%t %0 ((%2,%3,%4)) -) [%1]",        },
    { Op_TauStField,            true,  MB::StoreOrSync,   MK::Store_AutoCompress,                    "stfld",         "stfld%m:%t %0 ((%2,%3)) -) [%1.%d]",     },
    { Op_TauStElem,             true,  MB::StoreOrSync,   MK::Store_AutoCompress,                    "stelem",        "stelem%m:%t %0 ((%3,%4,%5)) -) [%1[%2]]",   },
    { Op_TauStStatic,           true,  MB::StoreOrSync,   MK::Store_AutoCompress,                    "stsfld",        "stsfld:%t %0 ((%1)) -) [%d]",         },                    
    { Op_TauStRef,              true,  MB::StoreOrSync,   MK::Store_AutoCompress,                    "stref ",        "stref%m   %0 ((%3,%4,%5)) -) [%1 %2] ",      }, // high-level version that will make a call to the VM
    { Op_TauCheckBounds,        false, MB::Check,         MK::Overflow_and_Exception,                "chkbounds",     "chkbounds %1 .lt. %0 -) %l  %b",         }, // takes index and array length arguments,               },
    { Op_TauCheckLowerBound,    false, MB::Check,         MK::Overflow_and_Exception,                "chklb",         "chklb %0 .le. %1 -) %l",             }, // throws unless src0 <= src1
    { Op_TauCheckUpperBound,    false, MB::Check,         MK::Overflow_and_Exception,                "chkub",         "chkub %0 .lt. %1 -) %l",             }, // throws unless src0 < src1
    { Op_TauCheckNull,          false, MB::Check,         MK::Exception_and_DefArg,                  "chknull",       "chknull   %0 -) %l  %b",           }, // throws NullPointerException if src is null
    { Op_TauCheckZero,          false, MB::Check,         MK::Exception,                             "chkzero",       "chkzero   %0 -) %l  %b",           }, // for divide by zero exceptions (div and rem)
    { Op_TauCheckDivOpnds,      false, MB::Check,         MK::Exception,                             "chkdivopnds",   "chkdivopnds %0,%1 -) %l",            }, // for signed divide overflow in CLI (div/rem of MAXNEGINT, -1): generates an ArithmeticException
    { Op_TauCheckElemType,      false, MB::Check,         MK::Exception,                             "chkelemtype",   "chkelemtype %0,%1 ((%2,%3)) -) %l",            }, // Array element type check for aastore
    { Op_TauCheckFinite,        false, MB::Check,         MK::Exception,                             "ckfinite",      "ckfinite  %s -) %l",           }, // throws ArithmeticException if value is NaN or +- inifinity
    { Op_NewObj,                false, MB::Exception,     MK::Exception,                             "newobj",        "newobj    %d -) %l  %b",       }, // OutOfMemoryException
    { Op_NewArray,              false, MB::Exception,     MK::Exception,                             "newarray",      "newarray  %d[%0] -) %l  %b",   }, // OutOfMemoryException, NegativeArraySizeException
    { Op_NewMultiArray,         false, MB::Exception,     MK::Exception,                             "newmultiarray", "newmultiarray %d[%s] -) %l",   }, // OutOfMemoryException, NegativeArraySizeException
    { Op_TauMonitorEnter,       true,  MB::StoreOrSync,   MK::None,                                  "monenter",      "monenter  %0 ((%1))",                 }, // (opnd must be non-null)
    { Op_TauMonitorExit,        true,  MB::StoreOrSync,   MK::Exception,                             "monexit",       "monexit   %0 ((%1))",                 }, // (opnd must be non-null), IllegalMonitorStateException
    { Op_TypeMonitorEnter,      true,  MB::StoreOrSync,   MK::None,                                  "tmonenter",     "monenter  %d",                 },
    { Op_TypeMonitorExit,       true,  MB::StoreOrSync,   MK::Exception,                             "tmonexit",      "monexit   %d",                 },
    { Op_LdLockAddr,            false, MB::Movable,       MK::None,                                  "ldlockaddr",    "ldlockaddr %0 -) %l",          }, // yields ref:int16
    { Op_IncRecCount,           true,  MB::StoreOrSync,   MK::None,                                  "increccnt",     "increccnt %s",                 }, // allows BalancedMonitorEnter to be used with regular MonitorExit
    { Op_TauBalancedMonitorEnter, true,  MB::StoreOrSync,   MK::None,                                  "balmonenter",   "balmonenter %0,%1 ((%2)) -) %l",         }, // (opnd must be non-null), postdominated by BalancedMonitorExit
    { Op_BalancedMonitorExit,   true,  MB::StoreOrSync,   MK::None,                                  "balmonexit",    "balmonexit %s",                }, // (cannot yield exception),               },dominated by BalancedMonitorEnter
    { Op_TauOptimisticBalancedMonitorEnter,  true,  MB::StoreOrSync,   MK::None,                                  "optbalmonenter",   "optbalmonenter %0,%1 ((%2)) -) %l",         }, // (opnd must be non-null), postdominated by BalancedMonitorExit
    { Op_OptimisticBalancedMonitorExit,   true,  MB::StoreOrSync,   MK::Exception,                                  "optbalmonexit",    "optbalmonexit %s",                }, // (cannot yield exception),               },dominated by BalancedMonitorEnter
    { Op_MonitorEnterFence,     true,  MB::StoreOrSync,   MK::None,                                  "monenterfence", "monenterfence %0",             }, // (opnd must be non-null)
    { Op_MonitorExitFence,      true,  MB::StoreOrSync,   MK::None,                                  "monexitfence",  "monexitfence  %0",             }, // (opnd must be non-null)
    { Op_TauStaticCast,         false, MB::Movable,       MK::None,                                  "staticcast",    "staticcast %0,%d ((%1)) -) %l",       }, // Compile-time assertion.  Asserts that cast is legal.
    { Op_TauCast,               false, MB::Check,         MK::Exception,                             "cast  ",        "cast      %0,%d ((%1)) -) %l  %b",        }, // CastException (suceeds if argument is null, returns casted object)
    { Op_TauAsType,             false, MB::Movable,       MK::None,                                  "astype",        "astype    %0,%d -) %l",        }, // returns casted object if argument is an instance of, null otherwise
    { Op_TauInstanceOf,         false, MB::Movable,       MK::None,                                  "instanceof",    "instanceof %0,%d ((%1)) -) %l",}, // returns true if argument is an instance of type T, tau opnd isNonNull
    { Op_InitType,              true,  MB::CSEable,       MK::Exception,                             "inittype",      "inittype  %d  %b",             }, // can throw a linking exception during class initialization
    { Op_Label,                 true,  MB::None,          MK::None,                                  "label ",        "%l: %b",                       }, // special label instructions for branch labels, finally, catch
    { Op_MethodEntry,           true,  MB::None,          MK::None,                                  "methodentry",   "--- MethodEntry(%d): (%s)  %b",}, // method entry label
    { Op_MethodEnd,             true,  MB::None,          MK::None,                                  "methodend",     "+++ MethodEnd(%d) (%s)",       }, // end of a method
    
    // Memory instructions                                                                                             
    // Special SSA nodes                                                                                               
    { Op_Phi,                   false, MB::None,          MK::None,                                  "phi   ",        "phi(%s)      -) %l",           }, // merge point
    { Op_TauPi,                 false, MB::Movable,       MK::None,                                  "pi    ",        "pi(%0 : %d) ((%1)) -) %l",           }, // liverange split based on condition 
                                                                                                                       
    // Profile instrumentation instructions                   
    { Op_IncCounter,            true,  MB::None,          MK::None,                                  "inccounter",    "inccounter(%d)",               }, // Increment a profile counter by 1
    { Op_Prefetch,              true,  MB::StoreOrSync,   MK::None,                                  "prefetch",      "prefetch %0 ",            }, //StoreOrSync

    // Compressed Pointer instructions
    { Op_UncompressRef,         false, MB::Movable,       MK::None,                                  "uncmpref",               "uncmpref %s -) %l",      },
    { Op_CompressRef,           false, MB::Movable,       MK::None,                                  "cmpref",                 "cmpref %s -) %l",      },
    { Op_LdFieldOffset,         false, MB::Movable,       MK::None,                                  "ldfldoff",               "ldfldoff  [.%d] -) %l",      },
    { Op_LdFieldOffsetPlusHeapbase,  
                                false, MB::Movable,       MK::None,                                  "ldfldophb",              "ldfldoffphb [.%d] -) %l",      },
    { Op_LdArrayBaseOffset,     false, MB::Movable,       MK::None,                                  "ldbaseoff",             "ldbaseoff -) %l",      },
    { Op_LdArrayBaseOffsetPlusHeapbase, 
                                false, MB::Movable,       MK::None,                                  "ldbaseoffphb",             "ldbaseoffphb -) %l",      },
    { Op_LdArrayLenOffset,     false, MB::Movable,        MK::None,                                  "ldlenoff",              "ldlenoff -) %l",      },
    { Op_LdArrayLenOffsetPlusHeapbase, 
                                false, MB::Movable,       MK::None,                                  "ldlenoffphb",             "ldlenoffphb -) %l",      },
    { Op_AddOffset,             false, MB::Movable,       MK::None,                                  "addoffset",              "addoff %s -) %l",      },
    { Op_AddOffsetPlusHeapbase, false, MB::Movable,       MK::None,                                  "addoffphb",              "addoffphb %s -) %l",      },

    { Op_TauPoint,              false, MB::None,          MK::None,                                  "taupoint ",        "taupoint() -) %l",           }, // mark
    { Op_TauEdge,              false, MB::None,          MK::None,                                   "tauedge ",        "tauedge() -) %l",           }, // mark
    { Op_TauAnd,                false, MB::Movable,       MK::None,                                  "tauand ",        "tauand       %s -) %l",        },
    { Op_TauUnsafe,             false, MB::None,          MK::None,                                  "tauunsafe",        "tauunsafe() -) %l",           }, // mark
    { Op_TauSafe,               false, MB::None,          MK::None,                                  "tauunsafe",        "tausafe() -) %l",           }, // mark

    { Op_TauCheckCast,          false, MB::Check,         MK::Exception,                             "tauchkcast ",        "tauchkcast      %0,%d ((%1)) -) %l",        }, // CastException (suceeds if argument is null, returns casted object)
    { Op_TauHasType,            false, MB::Movable,       MK::None,                             "tauhastype ",        "tauhastype      %0,%d -) %l",        }, // temporary declaration that source is of given type
    { Op_TauHasExactType,       false, MB::CSEable,       MK::None,                             "tauexacttype ",        "tauexacttype      %0,%d -) %l",        }, // temporary declaration that source is exactly of given type
    { Op_TauIsNonNull,          true, MB::CSEable,       MK::None,                             "tauisnonnull ",        "tauisnonnull      %0 -) %l",        }, // temporary declaration that source null
    { Op_IdentHC,              true,  MB::Call,   MK::None,                                  "identityHC",      "identityHC %s -) %l ",            }, 
};                                                             

unsigned short Modifier::encode(Opcode opcode, U_32 numbits) const
{
    assert((opcode >= 0) && (opcode < NumOpcodes));
    Modifier::Kind::Enum kinds = opcodeTable[opcode].modifierKind;
    if (kinds == 0) return 0;
    U_32 encoded = 0;
    U_32 bitsused = 0;
    if ((kinds & Modifier::Kind::Overflow) != 0) addEncoding(encoded, bitsused, Overflow_Mask, Overflow_None, Overflow_Unsigned, OverflowModifier_IsShiftedBy, OverflowModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Signed) != 0) addEncoding(encoded, bitsused, Signed_Mask, SignedOp, UnsignedOp, SignedModifier_IsShiftedBy, SignedModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Comparison) != 0) addEncoding(encoded, bitsused, Cmp_Mask, Cmp_EQ, Cmp_NonZero, ComparisonModifier_IsShiftedBy, ComparisonModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::ShiftMask) != 0) addEncoding(encoded, bitsused, ShiftMask_Mask, ShiftMask_None, ShiftMask_Masked, ShiftMaskModifier_IsShiftedBy, ShiftMaskModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Strict) != 0) addEncoding(encoded, bitsused, Strict_Mask, Strict_No, Strict_Yes, StrictModifier_IsShiftedBy, StrictModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::DefArg) != 0) addEncoding(encoded, bitsused, DefArg_Mask, DefArgNoModifier, DefArgBothModifiers, DefArgModifier_IsShiftedBy, DefArgModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Store) != 0) addEncoding(encoded, bitsused, Store_Mask, Store_NoWriteBarrier, Store_WriteBarrier, StoreModifier_IsShiftedBy, StoreModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Exception) != 0) addEncoding(encoded, bitsused, Exception_Mask, Exception_Sometimes, Exception_Never, ExceptionModifier_IsShiftedBy, ExceptionModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::AutoCompress) != 0) addEncoding(encoded, bitsused, AutoCompress_Mask, AutoCompress_Yes, AutoCompress_No, AutoCompressModifier_IsShiftedBy, AutoCompressModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Speculative) != 0) addEncoding(encoded, bitsused, 
         Speculative_Mask, Speculative_Yes, Speculative_No, SpeculativeModifier_IsShiftedBy, SpeculativeModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Throw) != 0) addEncoding(encoded, bitsused, Throw_Mask, Throw_NoModifier, Throw_CreateStackTrace, ThrowModifier_IsShiftedBy, ThrowModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::NewModifier1) != 0) addEncoding(encoded, bitsused, NewModifier1_Mask, NewModifier1_Value1, NewModifier1_Value2, NewModifier1_IsShiftedBy, NewModifier1_BitsToEncode);
    if ((kinds & Modifier::Kind::NewModifier2) != 0) addEncoding(encoded, bitsused, NewModifier2_Mask, NewModifier2_Value1, NewModifier2_Value3, NewModifier2_IsShiftedBy, NewModifier2_BitsToEncode);
    assert(bitsused <= numbits);
    unsigned short usencoded = (unsigned short) encoded;
    assert(encoded == (U_32) usencoded);
    if (0 && Log::isEnabled()) {
        Log::out() << ::std::endl << "Modifier " << ::std::hex << (int) value << " and Opcode " 
                   << (int) opcode << " encoded as " << (int) usencoded << ::std::dec << ::std::endl;
    }
    return (unsigned short) usencoded;
}

Modifier::Modifier(Opcode opcode, U_32 encoding) : value(0)
{
    assert((opcode >= 0) && (opcode < NumOpcodes));
    Modifier::Kind::Enum kinds = opcodeTable[opcode].modifierKind;
    if (kinds == 0) return;
    U_32 bitsused = 0;
    if ((kinds & Modifier::Kind::Overflow) != 0) addDecoding(encoding, bitsused, Overflow_Mask, Overflow_None, Overflow_Unsigned, OverflowModifier_IsShiftedBy, OverflowModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Signed) != 0) addDecoding(encoding, bitsused, Signed_Mask, SignedOp, UnsignedOp, SignedModifier_IsShiftedBy, SignedModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Comparison) != 0) addDecoding(encoding, bitsused, Cmp_Mask, Cmp_EQ, Cmp_NonZero, ComparisonModifier_IsShiftedBy, ComparisonModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::ShiftMask) != 0) addDecoding(encoding, bitsused, ShiftMask_Mask, ShiftMask_None, ShiftMask_Masked, ShiftMaskModifier_IsShiftedBy, ShiftMaskModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Strict) != 0) addDecoding(encoding, bitsused, Strict_Mask, Strict_No, Strict_Yes, StrictModifier_IsShiftedBy, StrictModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::DefArg) != 0) addDecoding(encoding, bitsused, DefArg_Mask, DefArgNoModifier, DefArgBothModifiers, DefArgModifier_IsShiftedBy, DefArgModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Store) != 0) addDecoding(encoding, bitsused, Store_Mask, Store_NoWriteBarrier, Store_WriteBarrier, StoreModifier_IsShiftedBy, StoreModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Exception) != 0) addDecoding(encoding, bitsused, Exception_Mask, Exception_Sometimes, Exception_Never, ExceptionModifier_IsShiftedBy, ExceptionModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::AutoCompress) != 0) addDecoding(encoding, bitsused, AutoCompress_Mask, AutoCompress_Yes, AutoCompress_No, AutoCompressModifier_IsShiftedBy, AutoCompressModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Speculative) != 0) addDecoding(encoding, bitsused, Speculative_Mask, Speculative_Yes, Speculative_No, SpeculativeModifier_IsShiftedBy, SpeculativeModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::Throw) != 0) addDecoding(encoding, bitsused, Throw_Mask, Throw_NoModifier, Throw_CreateStackTrace, ThrowModifier_IsShiftedBy, ThrowModifier_BitsToEncode);
    if ((kinds & Modifier::Kind::NewModifier1) != 0) addDecoding(encoding, bitsused, NewModifier1_Mask, NewModifier1_Value1, NewModifier1_Value2, NewModifier1_IsShiftedBy, NewModifier1_BitsToEncode);
    if ((kinds & Modifier::Kind::NewModifier2) != 0) addDecoding(encoding, bitsused, NewModifier2_Mask, NewModifier2_Value1, NewModifier2_Value3, NewModifier2_IsShiftedBy, NewModifier2_BitsToEncode);

    assert(bitsused <= OPERATION_MODIFIER_BITS);

    if (0 && Log::isEnabled()) {
        Log::out() << ::std::endl << "Opcode " << ::std::hex << (int) opcode << " and bits " << (int) encoding 
                   << " decoded to Modifier " << (int) value << ::std::dec << ::std::endl;
    }
    assert(((U_32)encode(opcode, OPERATION_MODIFIER_BITS)) == encoding);
}


void Modifier::addEncoding(U_32 &encoded, U_32 &bitsused, U_32 mask, 
                           U_32 minval, U_32 maxval, 
                           U_32 isshiftedby,
                           U_32 bitstoencode) const
{
    U_32 thisval = value & mask;

    assert((minval <= thisval) && (thisval <= maxval));
    U_32 encodedval = ((thisval - minval) >> isshiftedby) << bitsused;
    encoded = encoded | encodedval ;
    bitsused = bitsused + bitstoencode;
}

void Modifier::addDecoding(U_32 &encoding, U_32 &bitsused, U_32 mask, 
                           U_32 minval, U_32 maxval, 
                           U_32 isshiftedby,
                           U_32 bitstoencode)
{
    U_32 maskedbits = (1 << bitstoencode)-1;
    U_32 shifted = ((encoding >> bitsused) & maskedbits) << isshiftedby;
    U_32 decoded = shifted + minval;
    U_32 masked = decoded & mask;
    assert(masked != 0);
    value |= masked;
    bitsused = bitsused + bitstoencode;
}

const char* 
Operation::getModifierString() const {
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    switch (info.modifierKind) {
    case MK::None:
        return "";
        
    case MK::Overflow_and_Exception:
        {
            enum OverflowModifier omod = getOverflowModifier();
            enum ExceptionModifier emod = getExceptionModifier();
            switch (emod) {
            case Exception_Sometimes:
                switch (omod) {
                case Overflow_None:     return "";
                case Overflow_Signed:   return ".ovf";
                case Overflow_Unsigned: return ".ovf.un";
                default: assert(0);
                }
            case Exception_Always:
                switch (omod) {
                case Overflow_None:     assert(0);
                case Overflow_Signed:   return ".ovf.throw";
                case Overflow_Unsigned: return ".ovf.un.throw";
                default: assert(0);
                }
            case Exception_Never:
                switch (omod) {
                case Overflow_None:     return "";
                case Overflow_Signed:   return ".ovf.safe";
                case Overflow_Unsigned: return ".ovf.un.safe";
                default: assert(0);
                }
            default: assert(0);
            }
        }
    break;
    case MK::Overflow_and_Exception_and_Strict:
        {
            enum OverflowModifier omod = getOverflowModifier();
            enum ExceptionModifier emod = getExceptionModifier();
            enum StrictModifier smod = getStrictModifier();
            switch (emod) {
            case Exception_Sometimes:
                assert(smod == Strict_No);
                switch (omod) {
                case Overflow_None:     return "";
                case Overflow_Signed:   return ".ovf";
                case Overflow_Unsigned: return ".ovf.un";
                default: assert(0);
                }
            case Exception_Always:
                assert(smod == Strict_No);
                switch (omod) {
                case Overflow_None:     assert(0);
                case Overflow_Signed:   return ".ovf.throw";
                case Overflow_Unsigned: return ".ovf.un.throw";
                default: assert(0);
                }
            case Exception_Never:
                switch (omod) {
                case Overflow_None:     
                    switch (smod) {
                    case Strict_No:
                        return "";
                    case Strict_Yes:
                        return ".strict";
                    default:
                        assert(0);
                    }
                    return "";
                case Overflow_Signed:   
                    assert(smod == Strict_No);
                    return ".ovf.safe";
                case Overflow_Unsigned: 
                    assert(smod == Strict_No);
                    return ".ovf.un.safe";
                default: assert(0);
                }
            default: assert(0);
            }
        }
    break;
    case MK::Signed:
        {
            enum SignedModifier smod = getSignedModifier();
            switch (smod) {
            case SignedOp:   return "";
            case UnsignedOp: return ".un";
            default: assert(0);
            }
        }
    break;
    case MK::Signed_and_Strict:
        {
            enum SignedModifier smod = getSignedModifier();
            enum StrictModifier stmod = getStrictModifier();
            switch (smod) {
            case SignedOp:   
                switch (stmod) {
                case Strict_No:  return "";
                case Strict_Yes: return ".strict";
                default: assert(0);
                }
                return "";
            case UnsignedOp: 
                switch (stmod) {
                case Strict_No:  return "";
                case Strict_Yes: return ".un.strict";
                default: assert(0);
                }
                return ".un";
            default: assert(0);
            }
        }
    break;
    case MK::Comparison:
        {
            enum ComparisonModifier mod = getComparisonModifier();
            switch (mod) {
            case Cmp_EQ: return "eq";
            case Cmp_NE_Un: return "ne";
            case Cmp_GT: return "gt";
            case Cmp_GT_Un: return "gtu";
            case Cmp_GTE: return "ge";
            case Cmp_GTE_Un: return "geu";
            case Cmp_Zero: return "z";
            case Cmp_NonZero: return "nz";
            default:
                assert(0);
            }
        }
    break;
    case MK::ShiftMask:
        {
            enum ShiftMaskModifier shmod = getShiftMaskModifier();
            switch (shmod) {
            case ShiftMask_None:   return "";
            case ShiftMask_Masked: return ".mask";
            default: assert(0);
            }
        }
    break;
    case MK::ShiftMask_and_Signed:
        {
            enum SignedModifier smod = getSignedModifier();
            enum ShiftMaskModifier shmod = getShiftMaskModifier();
            switch (smod) {
            case SignedOp:
                switch (shmod) {
                case ShiftMask_None:   return "";
                case ShiftMask_Masked: return ".mask";
                default: assert(0);
                }
            case UnsignedOp:
                switch (shmod) {
                case ShiftMask_None:   return ".un";
                case ShiftMask_Masked: return ".un.mask";
                default: assert(0);
                }
            default: assert(0);
            }
        }
    break;
    case MK::Strict:
        {
            enum StrictModifier stmod = getStrictModifier();
            switch (stmod) {
            case Strict_Yes: return ".strict"; 
            case Strict_No: return "";
            default: assert(0);
            }
        }
    break;
    case MK::DefArg:
        {
            enum DefArgModifier damod = getDefArgModifier();
            switch (damod) {
            case DefArgNoModifier: return "";
            case NonNullThisArg: return ".ths";
            case SpecializedToExactType: return ".xt";
            case DefArgBothModifiers: return ".ths.xt";
            default: assert(0);
            }
        }
    break;
    case MK::Store:
        {
            enum StoreModifier smod = getStoreModifier();
            switch (smod) {
            case Store_NoWriteBarrier: return "";
            case Store_WriteBarrier: return ".wb";
            default: assert(0);
            }
        }
    break;
    case MK::Store_AutoCompress:
        {
            enum StoreModifier smod = getStoreModifier();
            enum AutoCompressModifier acmod = getAutoCompressModifier();
            switch (smod) {
            case Store_NoWriteBarrier: 
                switch (acmod) {
                case AutoCompress_Yes: return "";
                case AutoCompress_No: return ".unc";
                default: assert(0);
                }
                break;
            case Store_WriteBarrier: 
                switch (acmod) {
                case AutoCompress_Yes: return ".wb";
                case AutoCompress_No: return ".wb.unc";
                default: assert(0);
                }
                break;
            default:
                assert(0);
            }
            break;
        }
    case MK::AutoCompress_Speculative:
        {
            enum AutoCompressModifier acmod = getAutoCompressModifier();
            enum SpeculativeModifier smod = getSpeculativeModifier();
            switch (acmod) {
            case AutoCompress_Yes: 
                switch(smod) {
                case Speculative_Yes: return ".s";
                case Speculative_No:  return "";
        default: assert(false);
                }
            case AutoCompress_No: 
                switch(smod) {
                case Speculative_Yes: return ".s.unc";
                case Speculative_No:  return ".unc";
        default: assert(false);
                }
            default: assert(0);
            }
            break;
        }
    case MK::Exception:
        {
            enum ExceptionModifier emod = getExceptionModifier();
            switch (emod) {
            case Exception_Sometimes: return "";
            case Exception_Always: return ".throw";
            case Exception_Never: return ".safe";
            default: assert(0);
            }
            break;
        }
    case MK::AutoCompress:
        {
            enum AutoCompressModifier acmod = getAutoCompressModifier();
            switch (acmod) {
            case AutoCompress_Yes: return "";
            case AutoCompress_No: return ".unc";
            default: assert(0);
            }
            break;
        }
    case MK::Throw:
        {
            // if this occurs in conjunction with other modifiers, then
            // should modify a case above to check for it
            enum ThrowModifier throwmod = getThrowModifier();
            switch (throwmod) {
            case Throw_NoModifier: return "";
            case Throw_CreateStackTrace: return ".newtrace";
            default: assert(0);
            }
            break;
        }
    case MK::NewModifier1:
        {
            // if this occurs in conjunction with other modifiers, then
            // should modify a case above to check for it
            enum NewModifier1 new1mod = getNewModifier1();
            switch (new1mod) {
            case NewModifier1_Value1: return ".new1v1";
            case NewModifier1_Value2: return ".new1v2";
            default: assert(0);
            }
            break;
        }
    case MK::NewModifier2:
        {
            // if this occurs in conjunction with other modifiers, then
            // should modify a case above to check for it
            enum NewModifier2 new2mod = getNewModifier2();
            switch (new2mod) {
            case NewModifier2_Value1: return ".new2v1";
            case NewModifier2_Value2: return ".new2v2";
            case NewModifier2_Value3: return ".new2v3";
            default: assert(0);
            }
            break;
        }
    default:
        assert(0);
    }
    assert(0);
    return "ERROR";
}

const char* 
Operation::getOpcodeString() const {
    assert(opcode < NumOpcodes);
    assert(opcodeTable[opcode].opcode == opcode);
    const char* s = opcodeTable[opcode].opcodeString;
    return s;
}
 
const char* 
Operation::getOpcodeFormatString() const {
    assert(opcode < NumOpcodes);
    assert(opcodeTable[opcode].opcode == opcode);
    const char* s = opcodeTable[opcode].opcodeFormatString;
    return s;
}

bool Operation::canThrow() const
{
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    if ((info.mobility == MB::Exception) || (info.mobility == MB::Check)) {
        return true;
    }
    
    if (opcode == Op_InitType) return true; 

    Modifier mod = getModifier();
    if (mod.hasExceptionModifier()) {
        enum ExceptionModifier emod = mod.getExceptionModifier();
        if (emod != Exception_Never) return true;
    }
    return false;
}

bool Operation::isCheck() const
{
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    if ((info.mobility == MB::Check)) {
        return true;
    }
    return false;
}

bool Operation::isMovable() const
{
    if (canThrow()) return false;
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    switch (info.mobility) {
    case MB::None: return false;
    case MB::Movable: return true;  // overflow case is handled above
    case MB::CSEable: return false; 
    case MB::Check: return false;
    case MB::Exception: return false;
    case MB::StoreOrSync: return false;
    case MB::Call: return false;
    case MB::ControlFlow: return false;
    case MB::MustEndBlock: return false;
    case MB::Load: return false;
    default: assert(0);
    }
    return false;
}

bool Operation::isCSEable() const {
    assert(opcode < NumOpcodes);
    assert(opcodeTable[opcode].opcode == opcode);
    Mobility::Kind mobility = opcodeTable[opcode].mobility;
    switch (mobility) {
    case MB::Movable: // even operations with overflow are CSEable; first instance will throw
    case MB::CSEable:
    case MB::Check:
        return true;
    case MB::Load:    // is CSEable for final fields, but we can't check that here.
    default:
        return false;
    }
}

bool Operation::isStoreOrSync() const {
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    switch (info.mobility) {
    case MB::Call:
    case MB::StoreOrSync:
        return true;
    default:
    break;
    }
    return false;
}

bool Operation::mustEndBlock() const {
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    switch (info.mobility) {
    case MB::MustEndBlock:
    case MB::Exception:
        return true;
    case MB::ControlFlow:
        if (opcode == Op_Catch) return false;
        return true;
    case MB::CSEable:
        if (opcode == Op_InitType) return true;
    case MB::Call:
    case MB::Movable:
    case MB::Check:
    case MB::StoreOrSync:
        // must check Exception modifier
        {
            if (hasExceptionModifier()) {
                enum ExceptionModifier exc = getExceptionModifier();
                switch (exc) {
                case Exception_Never:
                    return false;
                case Exception_Sometimes:
                case Exception_Always:
                    return true;
                default:
                    assert(0);
                }
            } else
                return false;
        }
    default:
        return false;
    }
}

bool Operation::isLoad() const
{
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    return (info.mobility == MB::Load);
}

bool Operation::isNonEssential() const
{
    assert(opcode < NumOpcodes);
    const OpcodeInfo &info = opcodeTable[opcode];
    assert(info.opcode == opcode);
    if (info.essential) {
        return false;
    } else {
        Modifier mod = getModifier();
        if (mod.hasExceptionModifier()) {
            enum ExceptionModifier emod = mod.getExceptionModifier();
            if (emod != Exception_Never) {
                return false;
            }
        }
        return true;
    }
}

bool Operation::isConstant() const
{
    switch (opcode) {
    case Op_LdConstant:
    case Op_LdRef:
    case Op_LdVarAddr:
    case Op_GetVTableAddr:
    case Op_LdFieldOffset:
    case Op_LdFieldOffsetPlusHeapbase:
    case Op_LdArrayBaseOffset:
    case Op_LdArrayBaseOffsetPlusHeapbase:
    case Op_LdArrayLenOffset:
    case Op_LdArrayLenOffsetPlusHeapbase:
        return true;
    default:
        return false;
    }
}

} //namespace Jitrino 
