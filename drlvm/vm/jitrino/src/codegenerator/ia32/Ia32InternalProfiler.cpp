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
 * @author Nikolay A. Sidelnikov
 */
#include <cstring>
#include "Ia32IRManager.h"

namespace Jitrino
{
namespace Ia32{

    template<class T> struct AttrDesc {
        T value;
        const char * name;
    };

    struct BBStats {
        int64 * bbExecCount;
        U_32 *    counters;

        BBStats() : bbExecCount(NULL), counters(NULL) {}
    };

    struct MethodStats {
        std::string methodName;
        StlMap<int, BBStats> bbStats; //bbID, bbExecCount, array of counters
        MethodStats(std::string s, MemoryManager& mm) : methodName(s), bbStats(mm) {}
    };

typedef StlVector<MethodStats *> Statistics;

template<class T> class FilterAttr {
    public:
        T       value;
        bool    isInitialized;
        bool    isNegative;

        FilterAttr(T v, bool i = false, bool n = false) : value(v), isInitialized(i), isNegative(n) {};
        
        FilterAttr& operator=(const FilterAttr& c) {
            FilterAttr& f = *this;
            f.value = c.value;
            f.isInitialized = c.isInitialized;
            f.isNegative = c.isNegative;
            return f;
        }
};

struct OpndFilter {
        bool                            isInitialized;
        int                             opNum;
        FilterAttr<Inst::OpndRole>      opndRole;
        FilterAttr<OpndKind>            opndKind;
        FilterAttr<RegName>             regName;
        FilterAttr<MemOpndKind>         memOpndKind;

        OpndFilter() : isInitialized(false), opNum(-1), opndRole(Inst::OpndRole_Null), opndKind(OpndKind_Null), regName(RegName_Null), memOpndKind(MemOpndKind_Null) {}
        
        OpndFilter& operator=(const OpndFilter& c) {
            OpndFilter& f = *this;
            f.isInitialized = c.isInitialized;      
            f.opNum = c.opNum;
            f.opndRole          =c.opndRole;    
            f.opndKind          =c.opndKind;    
            f.regName           =c.regName;     
            f.memOpndKind       =c.memOpndKind; 
            return f;
        }

};

struct Filter {
        bool    isInitialized;
        bool    isNegative;
        bool    isOR;

        FilterAttr<Mnemonic>                                mnemonic;
        FilterAttr<I_32>                                   operandNumber;
        FilterAttr<Opnd::RuntimeInfo::Kind>                 rtKind;
        FilterAttr<VM_RT_SUPPORT>                           rtHelperID;
        FilterAttr<std::string>                             rtIntHelperName;
        FilterAttr<bool>                                    isNative;
        FilterAttr<bool>                                    isStatic;
        FilterAttr<bool>                                    isSynchronized;
        FilterAttr<bool>                                    isNoInlining;
        FilterAttr<bool>                                    isInstance;
        FilterAttr<bool>                                    isFinal;
        FilterAttr<bool>                                    isVirtual;
        FilterAttr<bool>                                    isAbstract;
        FilterAttr<bool>                                    isClassInitializer;
        FilterAttr<bool>                                    isInstanceInitializer;
        FilterAttr<bool>                                    isStrict;
        FilterAttr<bool>                                    isInitLocals;

        StlMap<int, OpndFilter> operandFilters;

        Filter() : isInitialized(false), isNegative(false), isOR(false), mnemonic(Mnemonic_NULL), operandNumber(-1), rtKind(Opnd::RuntimeInfo::Kind_Null), rtHelperID(VM_RT_UNKNOWN), rtIntHelperName("none"), isNative(false), isStatic(false), isSynchronized(false), isNoInlining(false), isInstance(false), isFinal(false), isVirtual(false), isAbstract(false), isClassInitializer(false), isInstanceInitializer(false), isStrict(false), isInitLocals(false), operandFilters(Jitrino::getGlobalMM()) {}

        Filter& operator=(const Filter& c) {
            Filter& f = *this;
            f.isNegative = c.isNegative;
            f.isInitialized = c.isInitialized;
            f.isOR = c.isOR;
            f.mnemonic=c.mnemonic;
            f.operandNumber=c.operandNumber;
            f.rtKind=c.rtKind;
            f.rtHelperID=c.rtHelperID;
            f.rtIntHelperName=c.rtIntHelperName;
            f.isNative=c.isNative;
            f.isStatic=c.isStatic;
            f.isSynchronized=c.isSynchronized;
            f.isNoInlining            =  c.isNoInlining;              
            f.isInstance             =  c.isInstance;                
            f.isFinal               =  c.isFinal;                   
            f.isVirtual              =  c.isVirtual;                 
            f.isAbstract              =  c.isAbstract;                
            f.isClassInitializer      =  c.isClassInitializer;        
            f.isInstanceInitializer   =  c.isInstanceInitializer;     
            f.isStrict                =  c.isStrict;                  
            f.isInitLocals            =  c.isInitLocals;              

            for(StlMap<int, OpndFilter>::const_iterator it = c.operandFilters.begin(); it !=c.operandFilters.end(); it++) {
                f.operandFilters[it->first] = it->second;
            }
            return f;
        }
};

struct Counter {
        std::string     name;
        std::string     title;
        bool            isSorting;
        Filter          filter;

        Counter() : isSorting(false) {}
};

class Config {
public:
    StlVector<Counter> counters;

    bool printBBStats;
    Config(MemoryManager& mm) : counters(mm), printBBStats(false) {};
};


//========================================================================================
// class InternalProfiler
//========================================================================================
/**
    class InternalProfiler collects information about methods 
    
*/

class InternalProfilerAct : public Action {
public:
    void init();
    void deinit() { dumpIt(); config = NULL; }

protected:
    void readConfig(Config * config);
    void dumpIt();

    Config * config;
    Statistics * statistics;

friend class InternalProfiler;
};


class InternalProfiler : public SessionAction {
public: 
    void runImpl();

protected:
    void addCounters(MethodDesc& methodDesc);

    bool passFilter(Inst * inst, Filter& filter);
    bool passOpndFilter(Inst * inst, Opnd * opnd, Filter& filter, OpndFilter& opndFltr);    
};

const AttrDesc<Inst::OpndRole> opndRoles[] = {
    {Inst::OpndRole_Null,"Null"},
    {Inst::OpndRole_Use,"Use"},
    {Inst::OpndRole_Def, "Def"},
    {Inst::OpndRole_UseDef,"UseDef"},
    {Inst::OpndRole_FromEncoder, "FromEncoder"},
    {Inst::OpndRole_Explicit, "Explicit"},
    {Inst::OpndRole_Auxilary, "Auxilary"},
    {Inst::OpndRole_Changeable, "Changeable"},
    {Inst::OpndRole_Implicit, "Implicit"},
    {Inst::OpndRole_InstLevel, "InstLevel"},
    {Inst::OpndRole_MemOpndSubOpnd, "MemOpndSubOpnd"},
    {Inst::OpndRole_OpndLevel, "OpndLevel"},
    {Inst::OpndRole_ForIterator, "ForIterator"},
    {Inst::OpndRole_All, "All"},
    {Inst::OpndRole_AllDefs, "AllDefs"},
    {Inst::OpndRole_AllUses, "AllUses"},
};

const AttrDesc<MemOpndKind> memOpndKinds[] = {
    {MemOpndKind_Null, "Null"},
    {MemOpndKind_StackAutoLayout, "StackAutoLayout"},
    {MemOpndKind_StackManualLayout, "StackManualLayout"}, 
    {MemOpndKind_Stack, "Stack"}, 
    {MemOpndKind_Heap, "Heap"}, 
    {MemOpndKind_ConstantArea, "ConstantArea"}, 
    {MemOpndKind_Any, "Any"},
};

const AttrDesc<Opnd::RuntimeInfo::Kind> rtKinds[] = {
    {Opnd::RuntimeInfo::Kind_Null, "Null"},
    {Opnd::RuntimeInfo::Kind_AllocationHandle,  "AllocationHandle"},
    {Opnd::RuntimeInfo::Kind_TypeRuntimeId, "TypeRuntimeId" },
    {Opnd::RuntimeInfo::Kind_MethodRuntimeId,"MethodRuntimeId"  },  
    {Opnd::RuntimeInfo::Kind_StringDescription, "StringDescription" },
    {Opnd::RuntimeInfo::Kind_Size,  "Size"  },
    {Opnd::RuntimeInfo::Kind_HelperAddress, "HelperAddress"},
    {Opnd::RuntimeInfo::Kind_InternalHelperAddress, "InternalHelperAddress"},
    {Opnd::RuntimeInfo::Kind_StaticFieldAddress,"StaticFieldAddress"},
    {Opnd::RuntimeInfo::Kind_FieldOffset,"FieldOffset"},
    {Opnd::RuntimeInfo::Kind_VTableAddrOffset,"VTableAddrOffset"},
    {Opnd::RuntimeInfo::Kind_VTableConstantAddr,"VTableConstantAddr"},
    {Opnd::RuntimeInfo::Kind_MethodVtableSlotOffset,"MethodVtableSlotOffset"},
    {Opnd::RuntimeInfo::Kind_MethodIndirectAddr,"MethodIndirectAddr"},
    {Opnd::RuntimeInfo::Kind_MethodDirectAddr,"MethodDirectAddr"},
    {Opnd::RuntimeInfo::Kind_ConstantAreaItem,"ConstantAreaItem"},
};

static ActionFactory<InternalProfiler, InternalProfilerAct> _iprof("iprof");

void InternalProfilerAct::init() {
    MemoryManager& mm = Jitrino::getGlobalMM();
    config = new(mm) Config(mm);
    statistics = new(mm) Statistics(mm);
    readConfig(config);
}

void InternalProfilerAct::readConfig(Config * config) {
    std::string configString;
    std::ifstream configFile;
    const char* fname;
    if ((fname = getArg("config")) == 0)
        fname = "iprof.cfg";
    configFile.open(fname, std::ios::in);

    bool rc = false;
    if (configFile.is_open()) {
        std::string line;
        U_32 ln = 0;
        bool opened = false;
        int num = -1;
        while (std::getline(configFile, line)) {
            ln++;
            if(!line.empty() && (line.find("#")!= 0)) {
                const char * c_line = line.c_str();
                if(std::strstr(c_line, "Config") == c_line) {
                    if(((int)line.find("PrintBBStats") != -1) && ((int)line.find("true")!=-1))
                        config->printBBStats = true;
                } else if (line.find("Counter.") == 0) {
                    if(!opened) {
                        opened = true;
                        num = (int)config->counters.size();
                        config->counters.push_back(Counter());
                        int pos1 = (int)line.find(".");
                        int pos2 = (int)line.find_first_of(".=" , pos1+1);

                        config->counters[num].name =    line.substr(pos1+1, pos2-pos1-1);
                    }
                    if((int)line.find(".Title=")!=-1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        config->counters[num].title=std::string(val);
                    } else if (((int)line.find(".IsOR=")!=-1) && ((int)line.find("true")!=-1)) {
                        config->counters[num].filter.isOR=true;
                    } else if ((int)line.find(std::string(config->counters[num].name)+"=")!=-1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        for(U_32 i = 0; i < config->counters.size(); i++) {
                            if(std::string(config->counters[i].name) == val) {
                                config->counters[num].filter = config->counters[i].filter;
                                break;
                            }
                        }
                    } else if((int)line.find(".Mnemonic")!=-1) {
                        char * mnem = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(mnem) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.mnemonic.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.mnemonic.value=EncoderBase::str2mnemonic(mnem);
                            config->counters[num].filter.mnemonic.isInitialized=true;
                        }
                    } else if (std::strstr(line.c_str(), ".OpndNumber")) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.operandNumber.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.operandNumber.value=atoi(val);
                            config->counters[num].filter.operandNumber.isInitialized=true;
                        }
                    } else if ((int)line.find(".Operand.") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        int pos = int(line.find(".Operand.")+9);
                        std::string v = line.substr(pos, line.find_first_of(".", pos)-pos);
                        int opNum;
                        if(v == "*")
                            opNum = -1;
                        else 
                            opNum = atoi(v.c_str());
                        config->counters[num].filter.operandFilters[opNum].opNum = opNum;
                        config->counters[num].filter.operandFilters[opNum].isInitialized = true;
                        if ((int)line.find(".OpndRole") != -1) {
                            if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                                config->counters[num].filter.operandFilters[opNum].opndRole.isNegative=true;
                            } else {
                                config->counters[num].filter.isInitialized=true;
                                config->counters[num].filter.operandFilters[opNum].opndRole.isInitialized=true;
                                for (U_32 i = 0; i<lengthof(opndRoles); i++) {
                                    if(std::string(opndRoles[i].name) == val)
                                        config->counters[num].filter.operandFilters[opNum].opndRole.value=opndRoles[i].value;
                                }
                            }
                        } else if ((int)line.find(".OpndKind") != -1) {
                            if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                                config->counters[num].filter.operandFilters[opNum].opndKind.isNegative=true;
                            } else {
                                config->counters[num].filter.isInitialized=true;
                                config->counters[num].filter.operandFilters[opNum].opndKind.isInitialized=true;
                                config->counters[num].filter.operandFilters[opNum].opndKind.value=getOpndKind(val);
                            }
                        } else if ((int)line.find(".RegName") != -1) {
                            if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                                config->counters[num].filter.operandFilters[opNum].opndRole.isNegative=true;
                            } else {
                                config->counters[num].filter.isInitialized=true;
                                config->counters[num].filter.operandFilters[opNum].regName.isInitialized=true;
                                config->counters[num].filter.operandFilters[opNum].regName.value = getRegName(val);
                            }
                        } else if ((int)line.find(".MemOpndKind") != -1) {
                            if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                                config->counters[num].filter.operandFilters[opNum].memOpndKind.isNegative=true;
                            } else {
                                config->counters[num].filter.isInitialized=true;
                                config->counters[num].filter.operandFilters[opNum].memOpndKind.isInitialized=true;
                                for (U_32 i = 0; i<lengthof(memOpndKinds); i++) {
                                    if(std::string(memOpndKinds[i].name) == val)
                                        config->counters[num].filter.operandFilters[opNum].memOpndKind.value=memOpndKinds[i].value;
                                }
                            }
                        }
                    } else if ((int)line.find(".RuntimeInfo.Kind") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.rtKind.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            for (U_32 i = 0; i<lengthof(rtKinds); i++) {
                                if(std::string(rtKinds[i].name) == val)
                                    config->counters[num].filter.rtKind.value=rtKinds[i].value;
                            }
                                //CompilationInterface::str2rid(val);
                            config->counters[num].filter.rtKind.isInitialized=true;
                        }
                    } else if ((int)line.find(".RuntimeInfo.HelperID") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.rtHelperID.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.rtHelperID.value=CompilationInterface::str2rid(val);
                            config->counters[num].filter.rtHelperID.isInitialized=true;
                        }
                    } else if ((int)line.find(".RuntimeInfo.IntHelperName") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.rtIntHelperName.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.rtIntHelperName.value=std::string(val);
                            config->counters[num].filter.rtIntHelperName.isInitialized=true;
                        }
                    } else if ((int)line.find(".isNative") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isNative.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isNative.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isNative.isInitialized=true;
                        }
                    } else if ((int)line.find(".isStatic") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isStatic.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isStatic.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isStatic.isInitialized=true;
                        }
                    } else if ((int)line.find(".isSynchronized") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isSynchronized.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isSynchronized.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isSynchronized.isInitialized=true;
                        }
                    } else if ((int)line.find(".isNoInlining") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isNoInlining.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isNoInlining.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isNoInlining.isInitialized=true;
                        }
                    } else if ((int)line.find(".isInstance") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isInstance.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isInstance.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isInstance.isInitialized=true;
                        }
                    } else if ((int)line.find(".isFinal") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isFinal.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isFinal.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isFinal.isInitialized=true;
                        }
                    } else if ((int)line.find(".isVirtual") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isVirtual.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isVirtual.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isVirtual.isInitialized=true;
                        }
                    } else if ((int)line.find(".isAbstract") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isAbstract.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isAbstract.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isAbstract.isInitialized=true;
                        }
                    } else if ((int)line.find(".isClassInitializer") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isClassInitializer.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isClassInitializer.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isClassInitializer.isInitialized=true;
                        }
                    } else if ((int)line.find(".isInstanceInitializer") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isInstanceInitializer.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isInstanceInitializer.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isInstanceInitializer.isInitialized=true;
                        }
                    } else if ((int)line.find(".isStrict") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isStrict.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isStrict.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isStrict.isInitialized=true;
                        }
                    } else if ((int)line.find(".isInitLocals") != -1) {
                        char * val = (char *)std::strstr(line.c_str(),"=")+1;
                        if ((std::string(val) == "true") && (std::strstr(line.c_str(), "IsNegative"))) {
                            config->counters[num].filter.isInitLocals.isNegative=true;
                        } else {
                            config->counters[num].filter.isInitialized=true;
                            config->counters[num].filter.isInitLocals.value=(std::string(val) == "true")? true : false;
                            config->counters[num].filter.isInitLocals.isInitialized=true;
                        }
                    }
                } else if (std::strstr(c_line, "[begin]") == c_line) {
                } else if (std::strstr(c_line, "[end]") == c_line) {
                    opened = false;
                } else if (std::strstr(c_line, "#") == c_line) {
                } else {
                    ::std::cerr<<"iprof: BAD LINE("<<ln<<") in configuration file"<<::std::endl;
                    exit(1);
                }
                configString+=line+"\n";
            }
        }
        rc = !configString.empty();
    } 
    if (!rc) {
        ::std::cerr<<"iprof: Can't read configuration"<<::std::endl;
    }
}

void InternalProfilerAct::dumpIt() {
    if(!config || !config->counters.size())
        return;
    const char* fname;
    if ((fname = getArg("out")) == 0)
        fname = "iprof.stat";
    std::ofstream outFile(fname, std::ios::ate);
    outFile << "Method name\t";
    for(U_32 i = 0; i < config->counters.size(); i++) {
        std::string fName = config->counters[i].title != "" ? config->counters[i].title : config->counters[i].name;
        outFile << fName << "\t";
    }
    outFile << "\n";
    
    for(Statistics::const_iterator it = statistics->begin(); it != statistics->end(); it++) {
        MethodStats * stats = *it;
        outFile << stats->methodName.c_str()  << "\t";
        BBStats bbs = stats->bbStats[-1];
        for(U_32 i = 0; i < config->counters.size(); i++) {
            int64 count;
            std::string name = config->counters[i].name;
            if((name == "ByteCodeSize") || (name == "ExcHandlersNum")) {
                count = bbs.counters[i];
            } else if (name == "MaxBBExec") {
                count = 0;
                for(StlMap<int, BBStats>::iterator iter = stats->bbStats.begin(); iter != stats->bbStats.end(); iter++) {
                    if(iter->second.counters[i]*(*(iter->second.bbExecCount)) > count) {
                        count = iter->second.counters[i] * (*(iter->second.bbExecCount)) ;
                    }
                }
            } else if (name == "HottestBBNum") {
                int64 c = 0;
                count = 0;
                for(StlMap<int, BBStats>::iterator iter = stats->bbStats.begin(); iter != stats->bbStats.end(); iter++) {
                    if(iter->first != -1) {
                        int64 j = iter->second.counters[i]*(*(iter->second.bbExecCount)) ;
                        if(j > c) {
                            c = iter->second.counters[i] * (*(iter->second.bbExecCount)) ;
                            count = iter->first;
                        }
                    }
                }
            } else if (name == "MethodExec") {
                count = *(stats->bbStats[0].bbExecCount);
            } else {
                count = 0;
                for(StlMap<int, BBStats>::iterator iter = stats->bbStats.begin(); iter != stats->bbStats.end(); iter++) {
                    if(iter->first != -1)
                        count += iter->second.counters[i] * (*(iter->second.bbExecCount)) ;
                }
            }
            outFile << count << "\t";
        }
        outFile << std::endl;
        if(config->printBBStats) {
            for(StlMap<int, BBStats>::iterator iter = stats->bbStats.begin(); iter != stats->bbStats.end(); iter++) {
                if(iter->first == -1)
                    continue;
                outFile << "BB_" << iter->first << "_" << stats->methodName.c_str() << "\t";
                for(U_32 i = 0; i < config->counters.size(); i++) {
                    int64 outValue = iter->second.counters[i] * (*(iter->second.bbExecCount));
                    outFile <<  outValue << "\t";
                }
                outFile << std::endl;
            }
        }
    }
}

void InternalProfiler::runImpl() {
    addCounters(irManager->getMethodDesc());
}

void InternalProfiler::addCounters(MethodDesc& methodDesc) {
    MemoryManager& mm = Jitrino::getGlobalMM();
    MethodStats* ms = new(mm) MethodStats(std::string(methodDesc.getParentType()->getName())+"::"+methodDesc.getName()+methodDesc.getSignatureString(), mm);

    InternalProfilerAct& storage = *static_cast<InternalProfilerAct*>(getAction());
    storage.statistics->push_back(ms);
    //method external properties, no need to count
    U_32 cSize = (U_32)storage.config->counters.size();
    if (!cSize)
        return;
    ms->bbStats[-1].counters= new(mm) U_32[cSize];
    for(U_32 i = 0; i < cSize ; i++) {
        ms->bbStats[-1].counters[i] = 0;
    }
    ms->bbStats[-1].bbExecCount= new(mm) int64[1];
    *(ms->bbStats[-1].bbExecCount)  = 0;
    for(U_32 i  = 0; i < cSize ; i++) {
        Counter c  = storage.config->counters[i];
        if(c.name == std::string("ByteCodeSize")) {
            ms->bbStats[-1].bbExecCount= new(mm) int64[1];
            *(ms->bbStats[-1].bbExecCount)  = 0;
            
            ms->bbStats[-1].counters[i] = methodDesc.getByteCodeSize();
        } else if (c.name == std::string("ExcHandlersNum")) {
            int n = methodDesc.getNumHandlers();
            ms->bbStats[-1].counters[i] = n;
        }
    }
    //cycle by all insts
    IRManager & irm=getIRManager();
    const Nodes& nodes = irm.getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(),end = nodes.end();it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()){
            ms->bbStats[node->getId()].counters= new(mm) U_32[cSize];
            for(U_32 i = 0; i < cSize ; i++) {
                ms->bbStats[node->getId()].counters[i] = 0;
            }
            for (Inst * inst=(Inst*)node->getFirstInst(); inst!=NULL; inst=inst->getNextInst()){
                if(!inst->hasKind(Inst::Kind_PseudoInst) || inst->hasKind(Inst::Kind_EntryPointPseudoInst)) {
                    for(U_32 i  = 0; i < cSize ; i++) {
                        Counter c  = storage.config->counters[i];
                        if(std::string(c.name) == "MaxBBExec" || std::string(c.name) == "HottestBBNum" || std::string(c.name) == "BBExec" ) {
                                ms->bbStats[node->getId()].counters[i] =1;
                        } else {
                            bool matched = passFilter(inst, c.filter);
                            if (matched) {
                                ms->bbStats[node->getId()].counters[i]++;
                                ms->bbStats[-1].counters[i]++;
                            }
                        }
                    }
                }
            }
            ms->bbStats[node->getId()].bbExecCount= new(mm) int64[1];
            *(ms->bbStats[node->getId()].bbExecCount)   = 0;
            node->prependInst(irManager->newInst(Mnemonic_POPFD));
#ifndef _EM64T_
            node->prependInst(irManager->newInst(Mnemonic_ADC, irManager->newMemOpnd(irManager->getTypeFromTag(Type::Int32), MemOpndKind_Heap, NULL, int((U_8*)(ms->bbStats[node->getId()].bbExecCount) + 4)), irManager->newImmOpnd(irManager->getTypeFromTag(Type::Int32),0)));

            node->prependInst(irManager->newInst(Mnemonic_ADD, irManager->newMemOpnd(irManager->getTypeFromTag(Type::Int32), MemOpndKind_Heap, NULL, int(ms->bbStats[node->getId()].bbExecCount)), irManager->newImmOpnd(irManager->getTypeFromTag(Type::Int32),1)));
#endif
            node->prependInst(irManager->newInst(Mnemonic_PUSHFD));
        }
    }
#ifndef _EM64T_
    ((BasicBlock *)irManager->getFlowGraph()->getEntryNode())->prependInst(irManager->newInst(Mnemonic_ADD, irManager->newMemOpnd(irManager->getTypeFromTag(Type::Int32), MemOpndKind_Heap, NULL, int(ms->bbStats[-1].bbExecCount)), irManager->newImmOpnd(irManager->getTypeFromTag(Type::Int32),1)));
#endif

}
bool InternalProfiler::passOpndFilter(Inst * inst, Opnd * opnd, Filter& filter, OpndFilter& opndFltr) {
    bool res = false;
    if(opndFltr.opndKind.isInitialized) {
        res = opnd->isPlacedIn(opndFltr.opndKind.value);
        if(opndFltr.opndKind.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(opndFltr.opndRole.isInitialized) {
        res = inst->getOpndRoles(opndFltr.opNum) & opndFltr.opndRole.value;
        if(opndFltr.opndRole.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(opndFltr.regName.isInitialized) {
        res = opndFltr.regName.value == opnd->getRegName();
        if(opndFltr.regName.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(opndFltr.memOpndKind.isInitialized) {
        res = opndFltr.memOpndKind.value == opnd->getMemOpndKind();
        if(opndFltr.memOpndKind.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    return filter.isOR ? false : true;
}

bool InternalProfiler::passFilter(Inst * inst, Filter& filter) {
    if(!filter.isInitialized)
        return false;
    bool res = false;
    if(filter.mnemonic.isInitialized) {
        res = (filter.mnemonic.value == inst->getMnemonic()) || (filter.mnemonic.value == Mnemonic_Null);
        if(filter.mnemonic.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.operandNumber.isInitialized) {
        res = filter.operandNumber.value == (int)inst->getOpndCount();
        if(filter.operandNumber.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }

    if(filter.operandFilters.size()) {
        for(StlMap<int, OpndFilter>::const_iterator it = filter.operandFilters.begin(); it !=filter.operandFilters.end(); it++) {
            OpndFilter opndFltr = it->second;
            if(!opndFltr.isInitialized)
                continue;
            if(opndFltr.opNum == -1) {
                for(U_32 i = 0; i < inst->getOpndCount(Inst::OpndRole_All) ; i++) {
                    Opnd * opnd = inst->getOpnd(i);
                    res = passOpndFilter(inst, opnd, filter, opndFltr);

                    if(filter.isOR && res)
                        return true;
                    if(!(filter.isOR || res))
                        return false;
                }
            } else if (opndFltr.opNum >= 0) {
                Opnd * opnd = opndFltr.opNum<(int)inst->getOpndCount(Inst::OpndRole_All) ? inst->getOpnd(opndFltr.opNum) : NULL;
                if(!opnd)
                    return false;
                res = passOpndFilter(inst, opnd, filter, opndFltr);

                if(filter.isOR && res)
                    return true;
                if(!(filter.isOR || res))
                    return false;
            } else {
                return false;
            }
        }
    }
    Opnd::RuntimeInfo * rt = NULL;
    if (inst->getMnemonic() == Mnemonic_CALL) {
        rt = inst->getOpnd(((ControlTransferInst*)inst)->getTargetOpndIndex())->getRuntimeInfo();
    }

    if(filter.rtKind.isInitialized) {
        if(!rt)
            return false;
        res = filter.rtKind.value == rt->getKind();
        if(filter.rtKind.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.rtHelperID.isInitialized) {
        if(!rt)
            return false;
        res = filter.rtHelperID.value == (VM_RT_SUPPORT)(POINTER_SIZE_INT)rt->getValue(0);
        if(filter.rtHelperID.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.rtIntHelperName.isInitialized) {
        if(!rt)
            return false;
        res = filter.rtIntHelperName.value == (char*)irManager->getInternalHelperInfo((const char*)rt->getValue(0))->pfn;
        if(filter.rtIntHelperName.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isNative.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isNative.value == ((MethodDesc *)rt->getValue(0))->isNative();
        if(filter.isNative.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isStatic.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isStatic.value == ((MethodDesc *)rt->getValue(0))->isStatic();
        if(filter.isStatic.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isSynchronized.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isSynchronized.value == ((MethodDesc *)rt->getValue(0))->isSynchronized();
        if(filter.isSynchronized.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isNoInlining.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isNoInlining.value == ((MethodDesc *)rt->getValue(0))->isNoInlining();
        if(filter.isNoInlining.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isInstance.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isInstance.value == ((MethodDesc *)rt->getValue(0))->isInstance();
        if(filter.isInstance.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isFinal.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isFinal.value == ((MethodDesc *)rt->getValue(0))->isFinal();
        if(filter.isFinal.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isVirtual.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isVirtual.value == ((MethodDesc *)rt->getValue(0))->isVirtual();
        if(filter.isVirtual.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isAbstract.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isAbstract.value == ((MethodDesc *)rt->getValue(0))->isAbstract();
        if(filter.isAbstract.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isClassInitializer.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isClassInitializer.value == ((MethodDesc *)rt->getValue(0))->isClassInitializer();
        if(filter.isClassInitializer.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isInstanceInitializer.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isInstanceInitializer.value == ((MethodDesc *)rt->getValue(0))->isInstanceInitializer();
        if(filter.isInstanceInitializer.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isStrict.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isStrict.value == ((MethodDesc *)rt->getValue(0))->isStrict();
        if(filter.isStrict.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }
    if(filter.isInitLocals.isInitialized) {
        if(!rt || ((rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) && (rt->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr)))
            return false;
        res = filter.isInitLocals.value == false;
        if(filter.isInitLocals.isNegative)
            res = !res;

        if(filter.isOR && res)
            return true;
        if(!(filter.isOR || res))
            return false;
    }

    return filter.isOR ? false : true;
}

}}; // namespace Ia32
