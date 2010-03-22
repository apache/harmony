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
 * @author Vyacheslav P. Shakin
 */

#ifndef _IA32_CALLING_CONVENTION_H_
#define _IA32_CALLING_CONVENTION_H_

#include "open/types.h"
#include "Type.h"
#include "Ia32IRConstants.h"
#include "Ia32Constraint.h"

/**
 * When entering a function, obey the (sp)%%4 == 0 rule.
 */
#define STACK_ALIGN4         (0x00000004)

/**
 * When entering a function, obey the (sp+8)%%16 == 0 rule
 * (Required by Intel 64 calling convention).
 */
#define STACK_ALIGN_HALF16    (0x00000008)

/**
 * When entering a function, obey the (sp)%%16 == 0 rule.
 */
#define STACK_ALIGN16         (0x00000010)


#ifdef _EM64T_
    #define STACK_REG RegName_RSP
    #define STACK_ALIGNMENT STACK_ALIGN_HALF16  
#else
    #define STACK_REG RegName_ESP
    #define STACK_ALIGNMENT STACK_ALIGN16  
#endif

namespace Jitrino
{
namespace Ia32{


//=========================================================================================================


//========================================================================================
// class CallingConvention
//========================================================================================

/**
 * Interface CallingConvention describes a particular calling convention.
 * 
 * As calling convention rules can be more or less formally defined,
 * it is worth to define this entity as a separate class or interface
 * Implementers of this interface are used as arguments to some IRManager methods 
 */
class CallingConvention
{   
public: 
    //--------------------------------------------------------------

    struct OpndInfo
    {
        U_32              typeTag;
        U_32              slotCount;
        bool                isReg;
        U_32              slots[4];
    };

    //--------------------------------------------------------------
    enum ArgKind
    {
        ArgKind_InArg,
        ArgKind_RetArg
    };

    virtual ~CallingConvention() {}

    /** Fills the infos array with information how incoming arguments or return values are passed 
    according to this calling convention 
    */
    virtual void    getOpndInfo(ArgKind kind, U_32 argCount, OpndInfo * infos) const =0;

    /** Returns a mask describing registers of regKind which are to be preserved by a callee
    */
    virtual Constraint  getCalleeSavedRegs(OpndKind regKind) const =0;
    
    /** Returns true if restoring arg stack is callee's responsibility
    */
    virtual bool    calleeRestoresStack() const =0;

    /** True arguments are pushed from the last to the first, false in the other case
    */
    virtual bool    pushLastToFirst()const =0;
    
    /**
     * Defines stack pointer alignment on method enter.
     */
    virtual U_32 getStackAlignment()const { return 0; }
    
    /**
     * Maps a string representation of CallingConvention to the 
     * appropriate CallingConvention_* item. 
     * If cc_name is NULL, then default for this platform convention 
     * is returned.
     */
    static const CallingConvention * str2cc(const char * cc_name);
};


typedef StlVector<const CallingConvention *> CallingConventionVector;


//========================================================================================
// STDCALLCallingConvention
//========================================================================================

/**
 * Implementation of CallingConvention for the STDCALL calling convention
 */
class STDCALLCallingConventionIA32: public CallingConvention
{   
public: 
    
    virtual             ~STDCALLCallingConventionIA32() {}
    virtual void        getOpndInfo(ArgKind kind, U_32 argCount, OpndInfo * infos) const;
    virtual Constraint  getCalleeSavedRegs(OpndKind regKind) const;
    virtual bool        calleeRestoresStack() const{ return true; }
    virtual bool        pushLastToFirst() const { return true; }

};

class STDCALLCallingConventionEM64T: public CallingConvention
{   
public: 
    
    virtual             ~STDCALLCallingConventionEM64T() {}
    virtual void        getOpndInfo(ArgKind kind, U_32 argCount, OpndInfo * infos) const;
    virtual Constraint  getCalleeSavedRegs(OpndKind regKind) const;
    virtual bool        calleeRestoresStack() const { return false; }
    virtual U_32      getStackAlignment() const { return STACK_ALIGNMENT; }
    virtual bool        pushLastToFirst() const { return true; }

};

//========================================================================================
// CDECL CallingConvention
//========================================================================================

/**
 * Implementation of CallingConvention for the CDECL calling convention
 */
class CDECLCallingConventionIA32: public STDCALLCallingConventionIA32
{   
public: 
    virtual         ~CDECLCallingConventionIA32() {}
    virtual bool    calleeRestoresStack() const { return false; }
};

class CDECLCallingConventionEM64T: public STDCALLCallingConventionEM64T
{   
public: 
    virtual         ~CDECLCallingConventionEM64T() {}
};

//========================================================================================
//  Managed CallingConvention
//========================================================================================

/**
 * Implementation of CallingConvention for the Managed calling convention
 */
class ManagedCallingConventionIA32: public STDCALLCallingConventionIA32
{   
public: 
    virtual         ~ManagedCallingConventionIA32() {}
    virtual void    getOpndInfo(ArgKind kind, U_32 argCount, OpndInfo * infos) const;
    virtual bool    pushLastToFirst( ) const { return false; }
    virtual U_32  getStackAlignment() const { return STACK_ALIGNMENT; }
};

class ManagedCallingConventionEM64T: public STDCALLCallingConventionEM64T
{   
public: 
    virtual         ~ManagedCallingConventionEM64T() {}
};


//========================================================================================
//  MultiArray CallingConvention
//========================================================================================

/**
 * Special calling convention for multi-array creation.
 * It's CDECL like but always passes arguments through the stack. 
 */
class MultiArrayCallingConventionIA32: public CDECLCallingConventionIA32
{
public:
    virtual         ~MultiArrayCallingConventionIA32() {}
};

class MultiArrayCallingConventionEM64T: public CDECLCallingConventionEM64T
{
public:
    virtual         ~MultiArrayCallingConventionEM64T() {}    
    virtual void    getOpndInfo(ArgKind kind, U_32 argCount, OpndInfo * infos) const;
};

#ifdef _EM64T_
typedef STDCALLCallingConventionEM64T       STDCALLCallingConvention;
typedef CDECLCallingConventionEM64T         CDECLCallingConvention;
typedef ManagedCallingConventionEM64T       ManagedCallingConvention;
typedef MultiArrayCallingConventionEM64T    MultiArrayCallingConvention;
#else
typedef STDCALLCallingConventionIA32        STDCALLCallingConvention;
typedef CDECLCallingConventionIA32          CDECLCallingConvention;
typedef ManagedCallingConventionIA32        ManagedCallingConvention;
typedef MultiArrayCallingConventionIA32     MultiArrayCallingConvention;
#endif

extern STDCALLCallingConvention     CallingConvention_STDCALL;
extern CDECLCallingConvention       CallingConvention_CDECL;
extern ManagedCallingConvention     CallingConvention_Managed;
extern MultiArrayCallingConvention  CallingConvention_MultiArray;

}; // namespace Ia32
}
#endif
