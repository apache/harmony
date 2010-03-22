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

#ifndef _VMINTERFACE_H_
#define _VMINTERFACE_H_

#include <ostream>
#include "open/types.h"
#include "open/rt_types.h"
#include "open/rt_helpers.h"
#include "open/em.h"
#include "open/ee_em_intf.h"

#define NULL_POINTER_EXCEPTION          "java/lang/NullPointerException"
#define INDEX_OUT_OF_BOUNDS             "java/lang/ArrayIndexOutOfBoundsException"
#define ARRAY_STORE_EXCEPTION           "java/lang/ArrayStoreException"
#define DIVIDE_BY_ZERO_EXCEPTION        "java/lang/ArithmeticException"
#define DEFAUlT_COSTRUCTOR_NAME         "<init>"
#define DEFAUlT_COSTRUCTOR_DESCRIPTOR   "()V"


namespace Jitrino {

// external and forward declarations
class TypeManager;
class Type;
class NamedType;
class ObjectType;
class MethodPtrType;
class MemoryManager;
class CompilationContext;
class CompilationInterface;
template <class ELEM_TYPE> class PtrHashTable;


class VMInterface {
public:
    //
    // VM specific methods for types
    //
    static void*       getSystemObjectVMTypeHandle();
    static void*       getSystemClassVMTypeHandle();
    static void*       getSystemStringVMTypeHandle();
    static void*       getArrayVMTypeHandle(void* elemVMTypeHandle,bool isUnboxed);
    static const char* getTypeName(void* vmTypeHandle);
    static void*       getSuperTypeVMTypeHandle(void* vmTypeHandle);
    static void*       getArrayElemVMTypeHandle(void* vmTypeHandle);
    static bool        isArrayType(void* vmTypeHandle);
    static bool        isArrayOfPrimitiveElements(void* vmTypeHandle);
    static bool        isEnumType(void* vmTypeHandle);
    static bool        isValueType(void* vmTypeHandle);
    static bool        isFinalType(void* vmTypeHandle);
    static bool        isLikelyExceptionType(void* vmTypeHandle);
    static bool        isInterfaceType(void* vmTypeHandle);
    static bool        isAbstractType(void* vmTypeHandle);
    static bool        needsInitialization(void* vmTypeHandle);
    static bool        isFinalizable(void* vmTypeHandle);
    static bool        getClassFastInstanceOfFlag(void* vmTypeHandle);
    static int         getClassDepth(void* vmTypeHandle);
    static bool        isInitialized(void* vmTypeHandle);
    static void*       getVTable(void* vmTypeHandle);
    static void*       getRuntimeClassHandle(void* vmTypeHandle);
    static void*       getAllocationHandle(void* vmTypeHandle);
    static bool        isSubClassOf(void* vmTypeHandle1,void* vmTypeHandle2);
    static U_32      getArrayElemOffset(void* vmElemTypeHandle,bool isUnboxed);
    static U_32      getArrayElemSize(void * vmTypeHandle);
    static U_32      getObjectSize(void * vmTypeHandle);
    static U_32      getArrayLengthOffset();

    static void*       getTypeHandleFromAllocationHandle(void* vmAllocationHandle);
    static void*       getTypeHandleFromVTable(void* vtHandle);

    static U_32      flagTLSSuspendRequestOffset();
    static U_32      flagTLSThreadStateOffset();
    static I_32       getTLSBaseOffset();
    static bool        useFastTLSAccess();


    // returns true if vtable pointers are compressed
    static bool          isVTableCompressed();

    // returns the offset of an object's virtual table
    static U_32      getVTableOffset();
    // returns the base for all vtables (addend to compressed vtable pointer)
    static void*      getVTableBase();

    // returns true if instance fields that are references are compressed
    static bool        areReferencesCompressed();

    //
    // returns the base for the heap (addend to compressed heap references)
    //
    static void*       getHeapBase();
    static void*       getHeapCeiling();


    static void        rewriteCodeBlock(U_8* codeBlock, U_8* newCode, size_t size);

    static bool setVmAdapter(vm_adaptor_t vm);
    static bool isValidFeature(const char* id);

protected:
    static vm_adaptor_t vm;
};


///////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////
//            O P A Q U E   D E S C' S  I M P L E M E N T E D   B Y    V M             //
///////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////

class TypeMemberDesc {
public:
    TypeMemberDesc(U_32 id, CompilationInterface* ci)
        : id(id), compilationInterface(ci) {}
    virtual ~TypeMemberDesc() {}

    U_32      getId() const { return id; }
    NamedType*  getParentType();
    bool        isParentClassIsLikelyExceptionType() const;


    virtual const char* getName() const           = 0;
    virtual const char* getSignatureString() const = 0;
    virtual void        printFullName(::std::ostream& os) = 0;
    virtual Class_Handle getParentHandle() const  = 0;

    virtual bool        isPrivate() const          = 0;
    virtual bool        isStatic() const          = 0;

protected:
    U_32 id;
    CompilationInterface* compilationInterface;

};

///Field representation for resolved fields
class FieldDesc : public TypeMemberDesc {
public:
    FieldDesc(Field_Handle field, CompilationInterface* ci, U_32 id) 
        : TypeMemberDesc(id, ci), drlField(field) {} 

        const char*   getName() const;
        const char*   getSignatureString() const;
        void          printFullName(::std::ostream &os);
        Class_Handle  getParentHandle() const;
        bool          isPrivate() const;
        bool          isStatic() const;
        //
        // this field is constant after it is initialized
        // can only be mutated by constructor (instance fields) or
        // type initializer (static fields)
        //
        bool          isInitOnly() const;
        // accesses to field cannot be reordered or CSEed
        bool          isVolatile() const;
        bool          isMagic() const;
        Type*         getFieldType();
        U_32        getOffset() const; // for non-static fields
        void*         getAddress() const; // for static fields
        Field_Handle  getFieldHandle() const  {return drlField; }

private:
    Field_Handle drlField;
};

///Method representation for resolved methods
class MethodDesc : public TypeMemberDesc {
public:
    MethodDesc(Method_Handle m, JIT_Handle jit, CompilationInterface* ci = NULL, U_32 id = 0);

        const char*  getName() const;
        const char*  getSignatureString() const;
        void         printFullName(::std::ostream& os);
        Class_Handle getParentHandle() const;

        bool         isPrivate() const;
        bool         isStatic() const;
        bool         isInstance() const;
        bool         isNative() const;
        bool         isSynchronized() const;
        bool         isNoInlining() const;
        bool         isFinal() const;
        bool         isVirtual() const;
        bool         isAbstract() const;
        // FP strict
        bool         isStrict() const;
        bool         isClassInitializer() const;
        bool         isInstanceInitializer() const;

        //
        // Method info
        //

        const U_8*  getByteCodes() const;
        U_32       getByteCodeSize() const;
        uint16       getMaxStack() const;
        U_32       getNumHandlers() const;
        void getHandlerInfo(unsigned short index, unsigned short* beginOffset, 
            unsigned short* endOffset, unsigned short* handlerOffset,
            unsigned short* handlerClassIndex) const;
        bool         hasAnnotation(NamedType* type) const;

        //
        // accessors for method info, code and data
        //
        U_8*     getInfoBlock() const;
        U_32   getInfoBlockSize() const;
        U_8*     getCodeBlockAddress(I_32 id) const;
        U_32   getCodeBlockSize(I_32 id) const;

        // sets and gets MethodSideEffect property for the compiled method
        Method_Side_Effects getSideEffect() const;
        void setSideEffect(Method_Side_Effects mse);

        //
        //    Exception registration API. 
        //
        void        setNumExceptionHandler(U_32 numHandlers);
        void        setExceptionHandlerInfo(U_32 exceptionHandlerNumber,
            U_8* startAddr,
            U_8* endAddr,
            U_8* handlerAddr,
            NamedType* exceptionType,
            bool exceptionObjIsDead);


        //
        // DRL kernel
        //
        U_32       getOffset() const;
        void*        getIndirectAddress() const;
        void*        getNativeAddress() const;

        U_32    getNumVars() const;

        Method_Handle    getMethodHandle() const   {return drlMethod;}

        //
        // handleMap method are used to register/unregister main map for all Container handlers
        void* getHandleMap() const {return handleMap;}
        void setHandleMap(void* hndMap) {handleMap = hndMap;}

        U_32    getNumParams() const;
        Type*     getParamType(U_32 paramIndex) const;
        Type*     getReturnType() const;

private:
    JIT_Handle   getJitHandle() const {return jitHandle;}
    Method_Handle               drlMethod;
    Method_Signature_Handle  methodSig;
    void* handleMap;
    JIT_Handle                  jitHandle;
};

enum ResolveNewCheck{ResolveNewCheck_NoCheck, ResolveNewCheck_DoCheck};

class CompilationInterface {
public:
    CompilationInterface(Compile_Handle c,
        Method_Handle m,
        JIT_Handle jit,
        MemoryManager& mm,
        OpenMethodExecutionParams& comp_params, 
        CompilationContext* cc, TypeManager& tpm);

    //
    //    System exceptions
    //
    enum SystemExceptionId {
        Exception_NullPointer = 0,
        Exception_ArrayIndexOutOfBounds,
        Exception_ArrayTypeMismatch,
        Exception_DivideByZero,
        Num_SystemExceptions
    };

    static const char*   getRuntimeHelperName(VM_RT_SUPPORT helperId);
    /**
     * Returns helper ID by its string representation. Name comparison 
     * is case-insensitive.
     * If the helperName is unknown, then VM_RT_UNKNOWN is returned.
     */
    static VM_RT_SUPPORT str2rid( const char * helperName );

    HELPER_CALLING_CONVENTION getRuntimeHelperCallingConvention(VM_RT_SUPPORT id);
    bool        isInterruptible(VM_RT_SUPPORT id);
    bool        mayBeInterruptible(VM_RT_SUPPORT id);
    void*       getRuntimeHelperAddress(VM_RT_SUPPORT);
    void*       getRuntimeHelperAddressForType(VM_RT_SUPPORT, Type*);
    MethodDesc* getMagicHelper(VM_RT_SUPPORT);

    Type*      getFieldType(Class_Handle enclClass, U_32 cpIndex);

    NamedType* getNamedType(Class_Handle enclClass, U_32 cpIndex, ResolveNewCheck check = ResolveNewCheck_NoCheck);
    Type*      getTypeFromDescriptor(Class_Handle enclClass, const char* descriptor);

    //this method is obsolete and will be removed. Use getNamedType if unsure.
    NamedType* resolveNamedType(Class_Handle enclClass, U_32 cpIndex);


    static const char* getMethodName(Class_Handle enclClass, U_32 cpIndex);
    static const char* getMethodClassName(Class_Handle enclClass, U_32 cpIndex);
    static const char* getFieldSignature(Class_Handle enclClass, U_32 cpIndex);

    MethodDesc* getStaticMethod(Class_Handle enclClass, U_32 cpIndex);
    MethodDesc* getVirtualMethod(Class_Handle enclClass, U_32 cpIndex);
    MethodDesc* getSpecialMethod(Class_Handle enclClass, U_32 cpIndex);
    MethodDesc* getInterfaceMethod(Class_Handle enclClass, U_32 cpIndex);

    FieldDesc*  getNonStaticField(Class_Handle enclClass, U_32 cpIndex, bool putfield);
    FieldDesc*  getStaticField(Class_Handle enclClass, U_32 cpIndex, bool putfield);

    FieldDesc*  getFieldByName(Class_Handle enclClass, const char* name);
    MethodDesc* getMethodByName(Class_Handle enclClass, const char* name);
    

    /**
     * Returns a system class by its name or NULL if no such class found.
     */
    ObjectType * findClassUsingBootstrapClassloader( const char * klassName );

    // resolve-by-name methods
    
    /**
     * Recursively looks up for a given method with a given signature in the given class.
     * Returns NULL if no such method found.
     */
    MethodDesc* resolveMethod(ObjectType * klass, const char * methodName, const char * methodSig);

    // Class type is a subclass of ch=mh->getParentType()  The function returns
    // a method description for a method overriding mh in type or in the closest
    // superclass of ch that overrides mh.
    MethodDesc* getOverridingMethod(NamedType *type, MethodDesc * methodDesc);


    const void*  getStringInternAddr(MethodDesc* enclosingMethodDesc, U_32 stringToken);
    Type*        getConstantType(MethodDesc* enclosingMethodDesc, U_32 constantToken);
    const void*  getConstantValue(MethodDesc* enclosingMethodDesc, U_32 constantToken);
    const char*  getSignatureString(MethodDesc* enclosingMethodDesc, U_32 methodToken);

    // Memory allocation API
    // all of these are for the method being compiled
    U_8*   allocateCodeBlock(size_t size, size_t alignment, CodeBlockHeat heat, 
        I_32 id, bool simulate);

    U_8*   allocateDataBlock(size_t size, size_t alignment);

    U_8*   allocateInfoBlock(size_t size);

    U_8*   allocateJITDataBlock(size_t size, size_t alignment);

    /**
     * Acquires a lock to protect method's data modifications (i.e. code/info 
     * block allocations, exception handlers registration, etc) in 
     * multi-threaded compilation.
     * The lock *must not* surround a code which may lead to execution of 
     * managed code, or a race and hang happen.
     * For example, the managed code execution may happen during a resolution
     * (invocation of resolve_XXX) to locate a class through a custom class 
     * loader.
     * Note, that the lock is *not* per-method, and shared across all the 
     * methods.
     */
    void    lockMethodData(void);

    /**
     * Releases a lock which protects method's data.
     */
    void    unlockMethodData(void);

    // methods that register JIT to be notified of various events
    void    setNotifyWhenMethodIsRecompiled(MethodDesc * methodDesc, void * callbackData);

    // write barrier instructions
    bool    needWriteBarriers() const {
        return compilation_params.exe_insert_write_barriers;
    }

    bool    isBCMapInfoRequired() const {
        bool res = compilation_params.exe_do_code_mapping;
        // exe_do_code_mapping should be used for different ti related byte code
        // mapping calculations
        // full byte code mapping could be enabled by IRBuilder flag now 
        // this method used to access to byte code low level maps and
        // enables byte codes for stack traces only
        //        res = true;
        return res;
    }
    void    setBCMapInfoRequired(bool is_supported) const {
        compilation_params.exe_do_code_mapping = is_supported;
    }

    bool    isCompileLoadEventRequired() const {
        return compilation_params.exe_notify_compiled_method_load;
    }

    void    sendCompiledMethodLoadEvent(MethodDesc* methodDesc, MethodDesc* outerDesc,
        U_32 codeSize, void* codeAddr, U_32 mapLength, 
        AddrLocation* addrLocationMap, void* compileInfo);

    OpenMethodExecutionParams& getCompilationParams() const { 
        return compilation_params;
    }

    /**
     * Requests VM to request this JIT to synchronously (in the same thread) compile given method.
     * @param method method to compile
     * @return true on successful compilation, false otherwise
     */
    bool compileMethod(MethodDesc *method);

    // returns the method to compile
    MethodDesc*     getMethodToCompile() const {return methodToCompile;}

    TypeManager&    getTypeManager() const {return typeManager;}
    MemoryManager&  getMemManager() const {return memManager;}

    Type*           getTypeFromDrlVMTypeHandle(Type_Info_Handle);

    FieldDesc*      getFieldDesc(Field_Handle field);
    MethodDesc*     getMethodDesc(Method_Handle method);

    void setCompilationContext(CompilationContext* cc) {compilationContext = cc;}

    CompilationContext* getCompilationContext() const {return compilationContext;}

private:
    /** 
     * Settings per compilation session: vminterface + optimizer flags and so on..
     * Today we pass compilation interface through the all compilation. To avoid global
     * changes in JIT subcomponents interfaces CompilationContext struct is placed here.
     */
    CompilationContext* compilationContext;

    JIT_Handle      getJitHandle() const;
    MethodDesc*     getMethodDesc(Method_Handle method, JIT_Handle jit);

    MemoryManager&              memManager;
    PtrHashTable<FieldDesc>*    fieldDescs;
    PtrHashTable<MethodDesc>*   methodDescs;
    TypeManager&                typeManager;
    MethodDesc*                 methodToCompile;
    Compile_Handle              compileHandle;
    bool                        flushToZeroAllowed;
    U_32                      nextMemberId;
    OpenMethodExecutionParams&  compilation_params;
};

class GCInterface {
public:
    GCInterface(GC_Enumeration_Handle gcHandle) : gcHandle(gcHandle) {}
    virtual ~GCInterface() {}

    virtual void enumerateRootReference(void** reference);

    virtual void enumerateCompressedRootReference(U_32* reference);

    virtual void enumerateRootManagedReference(void** slotReference, size_t slotOffset);

private:
    GC_Enumeration_Handle gcHandle;
};


class ThreadDumpEnumerator : public GCInterface {
public:
    ThreadDumpEnumerator() : GCInterface(NULL) {}

    virtual void enumerateRootReference(void** reference);

    virtual void enumerateCompressedRootReference(U_32* reference);

    virtual void enumerateRootManagedReference(void** slotReference, size_t slotOffset);
};

//
// The Persistent Instruction Id is used to generate a profile instruction map and to 
// feedback profile information into Jitrino.
//
class PersistentInstructionId {
public:
    PersistentInstructionId() 
        : methodDesc(NULL), localInstructionId((U_32)-1) {}

        PersistentInstructionId(MethodDesc* methodDesc, U_32 localInstructionId) 
            : methodDesc(methodDesc), localInstructionId(localInstructionId) {}

            bool isValid() const { return (methodDesc != NULL); }

            MethodDesc& getMethodDesc() const { return *methodDesc; }
            U_32 getLocalInstructionId() const { return localInstructionId; }

            // For IPF codegen to store block ids into pid
            bool hasValidLocalInstructionId() const { return localInstructionId != (U_32)-1; }

            bool operator==(const PersistentInstructionId& pid) { return methodDesc == pid.methodDesc && localInstructionId == pid.localInstructionId; }
private:
    MethodDesc* methodDesc;     // The source method at point the id was generated
    U_32 localInstructionId;  // The persistent local instruction id
};

inline ::std::ostream& operator<<(::std::ostream& os, const PersistentInstructionId& pid) { 
    os << (pid.isValid() ? pid.getMethodDesc().getName() : "NULL") << ":" 
        << (unsigned int) pid.getLocalInstructionId(); 
    return os;
}

::std::ostream& operator<<(::std::ostream& os, Method_Handle mh);

class VMPropertyIterator {
public:
    VMPropertyIterator(MemoryManager& m, const char* prefix);
    ~VMPropertyIterator();
    bool next();
    char* getKey() const { return key; }
    char* getValue() const { return value; }
private:
    MemoryManager& mm;
    char* key;
    char* value;
    char** keys;
    size_t iterator;
};

} //namespace Jitrino 

#endif // _VMINTERFACE_H_
