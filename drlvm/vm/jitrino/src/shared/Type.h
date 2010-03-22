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
 * @author Intel, Mikhail Y. Fursov
 *
 */
#ifndef _TYPE_H_
#define _TYPE_H_

#include <iostream>
#include <string.h>
#include "open/types.h"
#include "VMInterface.h"
#include "MemoryManager.h"
#include "HashTable.h"

#include "port_threadunsafe.h"

namespace Jitrino {

class SsaOpnd;
//
// forward declarations
//
class PtrType;
class FunctionPtrType;
class NamedType;
class ArrayType;
class MethodPtrType;
class UnresolvedMethodPtrType;
class ValueNameType;
class ITablePtrObjType;
class OrNullType;
class TypeManager;
class TypeFailStream;
class TypeFactsManager;
class MethodSignature;
typedef SsaOpnd* ValueName;

class Type {
public:
    enum Tag {
        // (1) Value types
        // --------------------
        //   (1.1) Built-in value types
        Tau,     // essentially a void type, used for 
        Void,    
        Boolean,
        Char,
        IntPtr,  // ptr-sized integer
        Int8,
        Int16,
        Int32, // 4-byte integer
        Int64, // 8-byte integer
        UIntPtr, // unsigned ptr-sized integer
        UInt8,
        UInt16,
        UInt32, // unsigned 4-byte integer
        UInt64, // unsigned 8-byte integer
        Single,
        Double,
        Float, // machine-specific float
        TypedReference, // typed reference
        
        //     (1.2) User-defined un-boxed value type
        Value, // user-defined value type

        // Offset type
        Offset,             // offset into an object
        OffsetPlusHeapbase, // heap base plus offset

        // (2) Reference types
        // --------------------
        //   (2.1) Object types

        //     (2.1.1) Built-in object types
        SystemObject, // System.Object
        SystemClass,  // System.Class
        SystemString, // System.String

        // special null object reference
        NullObject,
        UnresolvedObject,

        //     (2.1.2) Array type
        Array,

        //     (2.1.3) User-defined object
        Object, // object reference

        //        (2.1.4) User-defined boxed value object
        BoxedValue,

        //   (2.2) Ptr types
        UnmanagedPtr, // unmanaged pointer
        ManagedPtr, // managed pointer
        MethodPtr, // function pointer
        VTablePtr, // vtable pointer

        // (2) Compressed Reference types
        // --------------------
        //   (2.1) Object types

        //     (2.1.1) Built-in object types
        CompressedSystemObject, // System.Object
        CompressedSystemClass,  // System.Class
        CompressedSystemString, // System.String

        // special null object reference
        CompressedNullObject,
        // Unresolved Object
        CompressedUnresolvedObject,

        //     (2.1.2) Array type
        CompressedArray,
        
        //     (2.1.3) User-defined object
        CompressedObject, // object reference
        
        CompressedMethodPtr, // function pointer
        CompressedVTablePtr, // vtable pointer

        // Additional types for LBS project
        OrNull,
        VTablePtrObj,
        ITablePtrObj,
        ArrayLength,
        ArrayElementType,
        Singleton,

        NumTypeTags,
        InavlidTag = NumTypeTags
    };
    Type(Tag t) : tag(t), id(genNextTypeId()) {}
    virtual ~Type() {}

    const Tag tag;
    bool    isBuiltinValue()   {return isBuiltinValue(tag); }
    bool    isNumeric()        {return isNumeric(tag);      }
    bool    isReference()      {return isReference(tag);    }
    bool    isInteger()        {return isInteger(tag);      }
    bool    isManagedPtr()     {return isManagedPtr(tag);   }
    bool    isUnmanagedPtr()   {return isUnmanagedPtr(tag); }
    bool    isFP()             {return isFloatingPoint(tag);}
    bool    isVoid()           {return (tag == Type::Void);        }
    bool    isBoolean()        {return (tag == Type::Boolean);     }
    bool    isChar()           {return (tag == Type::Char);        }
    bool    isInt1()           {return (tag == Type::Int8);        }
    bool    isInt2()           {return (tag == Type::Int16);       }
    bool    isInt4()           {return (tag == Type::Int32);       }
    bool    isInt8()           {return (tag == Type::Int64);       }
    bool    isIntPtr()         {return (tag == Type::IntPtr);      }
    bool    isUInt1()          {return (tag == Type::UInt8);       }
    bool    isUInt2()          {return (tag == Type::UInt16);      }
    bool    isUInt4()          {return (tag == Type::UInt32);      }
    bool    isUInt8()          {return (tag == Type::UInt64);      }
    bool    isUIntPtr()        {return (tag == Type::UIntPtr);     }
    bool    isSingle()         {return (tag == Type::Single);      }
    bool    isDouble()         {return (tag == Type::Double);      }
    bool    isFloat()          {return (tag == Type::Float);       }
    bool    isOffset()         {return (tag == Type::Offset);       }
    bool    isOffsetPlusHeapbase()         {return (tag == Type::OffsetPlusHeapbase);       }
    bool    isSystemObject()   {return isSystemObject(tag); }
    bool    isSystemClass()    {return isSystemClass(tag); }
    bool    isSystemString()   {return (tag == Type::SystemString);}
    bool    isSignedInteger()  {return isSignedInteger(tag);}
    bool    isFloatingPoint()  {return isFloatingPoint(tag);}
    bool    isNullObject()     {return isNullObject(tag);}
    bool    isObject()         {return isObject(tag);}
    bool    isUnresolvedObject() {return isUnresolvedObject(tag);}
    bool    isUnresolvedMethodPtrType() {return asUnresolvedMethodPtrType()!=NULL;}
    virtual bool    isUnresolvedType() {return false;}
    bool    isArray()          {return isArray(tag);}
    bool    isValue()          {return isValue(tag);}
    bool    isPtr()            {return isPtr(tag);}
    bool    isUserValue()      {return isUserValue(tag);}
    bool    isUserObject()     {return isUserObject(tag);}
    bool    isClass()          {return isObject();}
    virtual bool isInterface() {return false;}
    virtual bool isAbstract()  {return false;}
    bool    isValueType()      {return isValue();}
    bool    isEnumType()       {return isEnum();}
    bool    isArrayType()      {return isArray();}
    bool    isMethodPtr()      {return isMethodPtr(tag); }
    bool    isVTablePtr()      {return isVTablePtr(tag); }
    bool    isCompressedReference() { return isCompressedReference(tag); }
    virtual bool isEnum()      {return false;}
    bool isNonEnumUserValue()  {return isUserValue() && !isEnum();}
    virtual bool isNamedType() {return false;}
    bool    isSingleton()      {return (tag == Type::Singleton);  }
    bool    isOrNull()         {return (tag == Type::OrNull);  }
    bool    isVTablePtrObj()   {return (tag == Type::VTablePtrObj);  }
    bool    isITablePtrObj()   {return (tag == Type::ITablePtrObj);  }
    bool    isArrayLength()    {return (tag == Type::ArrayLength);  }
    bool    isArrayElement()   {return (tag == Type::ArrayElementType);  }
    virtual void print(::std::ostream& os);
    virtual ObjectType*       asObjectType()         {return NULL;}
    virtual PtrType*          asPtrType()            {return NULL;}
    virtual FunctionPtrType*  asFunctionPtrType()    {return NULL;}
    virtual NamedType*        asNamedType()          {return NULL;}
    virtual ArrayType*        asArrayType()          {return NULL;}
    virtual MethodPtrType*    asMethodPtrType()      {return NULL;}
    virtual UnresolvedMethodPtrType* asUnresolvedMethodPtrType() { return NULL; }
    virtual ValueNameType*    asValueNameType()      {return NULL;}
    virtual ITablePtrObjType* asITablePtrObjType()   {return NULL;}
    virtual OrNullType*       asOrNullType()         {return NULL;}
    // Return the nearest (non-strict) super-type whose outermost constructor is not value dependent
    virtual Type*             getNonValueSupertype() {return this;}
    
    virtual const char*    getName();
    const char*    getPrintString() {return getPrintString(tag);}

    static bool isTau(Tag tag) {
        return (tag == Tau);
    }
    static bool isValue(Tag tag) {
        return (Void <= tag && tag <= Value);
    }
    static bool isBuiltinValue(Tag tag) {
        return (Void <= tag && tag <= TypedReference);
    }
    static bool isUserValue(Tag tag) {
        return (tag == Value);
    }
    static bool isNumeric(Tag tag) {
        return (IntPtr <= tag && tag <= Float);
    }
    static bool isInteger(Tag tag) {
        return (IntPtr <= tag && tag <= UInt64);
    }
    static bool isSignedInteger(Tag tag) {
        return (IntPtr <= tag && tag <= Int64);
    }
    static bool isUnsignedInteger(Tag tag) {
        return (UIntPtr <= tag && tag <= UInt64);
    }
    static bool isFloatingPoint(Tag tag) {
        return (Single <= tag && tag <= Float);
    }
    static bool isOffset(Tag tag) {
        return (tag == Offset);
    }
    static bool isOffsetPlusHeapbase(Tag tag) {
        return (tag == OffsetPlusHeapbase);
    }
    static bool isReference(Tag tag) {
        return (SystemObject <= tag && tag <= ITablePtrObj);
    }
    static bool isCompressedReference(Tag tag, CompilationInterface &compInt) {
        if (VMInterface::areReferencesCompressed()) {
            // Note: When a reference is converted to a managed pointer, it is converted from a 
            // compressed pointer in the heap to a raw pointer in the heap
            return ((SystemObject <= tag && tag <= BoxedValue)
                    || (CompressedSystemObject <= tag && 
                        tag <= CompressedObject));
        }
        return false;
    }
    static bool isCompressedReference(Tag tag) {
        return (CompressedSystemObject <= tag && tag <= CompressedObject);
    }
    static Tag compressReference(Tag tag) {
        switch (tag) {
        case SystemObject:     return CompressedSystemObject;
        case SystemClass:      return CompressedSystemClass;
        case SystemString:     return CompressedSystemString;
        case NullObject:       return CompressedNullObject;
        case UnresolvedObject: return CompressedUnresolvedObject;
        case Array:            return CompressedArray;
        case Object:           return CompressedObject;
        case MethodPtr:        return CompressedMethodPtr;
        case VTablePtr:        return CompressedVTablePtr;
        default:
            assert(0); return Void;
        }
    }
    static Tag unCompressReference(Tag tag) {
        switch (tag) {
        case CompressedSystemObject:     return SystemObject;
        case CompressedSystemClass:      return SystemClass;
        case CompressedSystemString:     return SystemString;
        case CompressedNullObject:       return NullObject;
        case CompressedUnresolvedObject: return UnresolvedObject;
        case CompressedArray:            return Array;
        case CompressedObject:           return Object;
        case CompressedMethodPtr:        return MethodPtr;
        case CompressedVTablePtr:        return VTablePtr;
        default:
            assert(0); return Void;
        }
    }
    static bool isObject(Tag tag) {
        return (((SystemObject <= tag) && (tag <= Object)) ||
                ((CompressedSystemObject <= tag) && (tag <= CompressedObject)));
    }
    static bool isUnresolvedObject(Tag tag) {
        return ((tag == UnresolvedObject) || (tag == CompressedUnresolvedObject));
    }
    static bool isNullObject(Tag tag) {
        return ((tag == NullObject) || (tag == CompressedNullObject));
    }
    static bool isArray(Tag tag) {
        return ((tag == Array) || (tag == CompressedArray));;
    }
    static bool isBuiltinObject(Tag tag) {
        return (isSystemObject(tag) || isSystemClass(tag) || isSystemString(tag));
    }
    static bool isSystemObject(Tag tag) {
        return ((tag == SystemObject) || (tag == CompressedSystemObject));
    }
    static bool isSystemClass(Tag tag) {
        return ((tag == SystemClass) || (tag == CompressedSystemClass));
    }
    static bool isSystemString(Tag tag) {
        return ((tag == SystemString)||(tag == CompressedSystemString));
    }
    static bool isUserObject(Tag tag) {
        return ((tag == Object)||(tag == CompressedObject));
    }
    static bool isPtr(Tag tag) {
        return (UnmanagedPtr <= tag && tag <= ManagedPtr);
    }
    static bool isManagedPtr(Tag tag) {
        return (tag == ManagedPtr);
    }
    static bool isUnmanagedPtr(Tag tag) {
        return (tag == UnmanagedPtr);
    }
    static bool isMethodPtr(Tag tag) {
        return ((tag == MethodPtr)||(tag == CompressedMethodPtr));
    }
    static bool isVTablePtr(Tag tag) {
        return ((tag == VTablePtr)||(tag == CompressedVTablePtr));
    }
    static const char* getPrintString(Tag);
    /**
     * Converts a string representation of a Tag to appropriate Tag. 
     * The search is case insensitive.
     * Returns Tag::InavlidTag if no mapping found.
     */
    static Tag str2tag(const char * tagname);
    /**
     * Converts a Tag into its string representation, i.e. "Void" or "UInt32".
     * Never returns NULL, but pointer to string like "invalid type tag" instead.
     * Don't mess with getPrintString() which returns a short 4-symbol abbreviation.
     */
    static const char * tag2str(Tag tag);

    // Could locations containing values of these types be the same?
    static bool mayAlias(TypeManager*, Type*, Type*);
    // The two types are pointer types, could their values be the same (i.e. point to the same locations)?
    static bool mayAliasPtr(Type*, Type*);

    U_32 getId() { return id; }

    // This is for switching integer types depending on the size
#ifdef POINTER64
    static bool isIntegerOf4Signed(Tag tag)    { return tag == Int32; }
    static bool isIntegerOf4Unsigned(Tag tag)  { return tag == UInt32; }
    static bool isIntegerOf8Signed(Tag tag)    { return tag == Int64  || tag == IntPtr; }
    static bool isIntegerOf8Unsigned(Tag tag)  { return tag == UInt64 || tag == UIntPtr; }
#else
    static bool isIntegerOf4Signed(Tag tag)    { return tag == Int32  || tag == IntPtr; }
    static bool isIntegerOf4Unsigned(Tag tag)  { return tag == UInt32 || tag == UIntPtr; }
    static bool isIntegerOf8Signed(Tag tag)    { return tag == Int64; }
    static bool isIntegerOf8Unsigned(Tag tag)  { return tag == UInt64; }
#endif
    static bool isIntegerOf4Bytes(Tag tag) { return isIntegerOf4Signed(tag) || isIntegerOf4Unsigned(tag); }
    static bool isIntegerOf8Bytes(Tag tag) { return isIntegerOf8Signed(tag) || isIntegerOf8Unsigned(tag); }
    

protected:
    virtual bool    _isFinalClass()    {return false;}
    
    static U_32 genNextTypeId() {
        // this operation can be unsafe until types are cached only per compilation session
        // and every compilation session is performed in a single thread
        UNSAFE_REGION_START
        nextTypeId++;
        UNSAFE_REGION_END
        return nextTypeId;
    }

private:
    const U_32 id;
    static U_32 nextTypeId;
};

///////////////////////////////////////////////////////////////////////////////
//
// Pointer types
//
///////////////////////////////////////////////////////////////////////////////

class PtrType : public Type {
public:
    PtrType(Type* t,bool isManagedPtr,ValueName array=NULL, ValueName index=NULL) 
        : Type(isManagedPtr ? ManagedPtr : UnmanagedPtr), pointedToType(t), array(array), index(index) {}
    virtual ~PtrType() {}

    PtrType* asPtrType() { return this; }
    Type*   getPointedToType()            {return pointedToType;}
    ValueName getArrayName() { return array; }
    ValueName getIndexName() { return index; }
    virtual void    print(::std::ostream& os);
private:
    Type*     pointedToType;
    ValueName array;
    ValueName index;
};

class FunctionPtrType : public Type {
public:
    FunctionPtrType(bool isCompressed=false) : Type(isCompressed ? CompressedMethodPtr : MethodPtr) { }
    virtual ~FunctionPtrType() {}

    FunctionPtrType* asFunctionPtrType() { return this; }
    virtual U_32 getNumParams() = 0;
    virtual Type* getParamType(U_32) = 0;
    virtual Type* getReturnType() = 0;
    virtual bool isInstance() = 0;
};

class MethodPtrType : public FunctionPtrType {
public:
    MethodPtrType(MethodDesc* md, TypeManager& tm, bool isCompressed=false, ValueName obj=NULL) 
        : FunctionPtrType(isCompressed), methodDesc(md), typeManager(tm), object(obj) {}
    virtual ~MethodPtrType() {}

    MethodPtrType* asMethodPtrType() { return this; }
    U_32 getNumParams() { return methodDesc->getNumParams(); }
    Type* getParamType(U_32 i);
    Type* getReturnType() { return methodDesc->getReturnType(); }
    bool isInstance() { return methodDesc->isInstance(); }
    virtual MethodDesc*     getMethodDesc()         {return methodDesc;}
    void print(::std::ostream& os);
    ValueName getThisValueName() { return object; }
private:
    MethodDesc* methodDesc;
    TypeManager& typeManager;
    ValueName object;
};


class UnresolvedMethodPtrType : public MethodPtrType {
public:
    UnresolvedMethodPtrType(ObjectType* _enclosingClass, U_32 _cpIndex, TypeManager& tm, 
        U_32 _nParams, Type** _paramTypes, Type* _returnType,  const char* _signatureStr,
        bool isCompressed=false, ValueName obj=NULL) 
        : MethodPtrType(NULL, tm, isCompressed, obj), 
        enclosingClass(_enclosingClass), cpIndex(_cpIndex),
        nParams(_nParams), paramTypes(_paramTypes), returnType(_returnType), signatureStr(_signatureStr)
    {}

    virtual ~UnresolvedMethodPtrType() {}

    UnresolvedMethodPtrType* asUnresolvedMethodPtrType() { return this; }
    U_32 getNumParams() {return nParams;}
    Type* getParamType(U_32 n){ assert(n<nParams); return paramTypes[n];}
    Type* getReturnType() {return returnType;}
    bool isInstance() { assert(0); return false;}
    virtual MethodDesc* getMethodDesc() {assert(0); return NULL;}

    void print(::std::ostream& os);
    virtual bool isUnresolvedType() {return true;}
private:
    void initSignature();
    ObjectType* enclosingClass;
    U_32      cpIndex;
    
    U_32 nParams;
    Type** paramTypes;
    Type* returnType;
    const char* signatureStr;
};



class VTablePtrType : public Type {
public:
    VTablePtrType(Type* t, bool isCompressed=false) : Type(isCompressed? CompressedVTablePtr : VTablePtr), 
                                                      baseType(t) {}
    virtual ~VTablePtrType() {}

    Type*          getBaseType()            {return baseType;}
    virtual void   print(::std::ostream& os);
private:
    Type*  baseType;
};

///////////////////////////////////////////////////////////////////////////////
//
// Non-Pointer Named types: Value and reference (object) types
// Should we call these named types?
//
///////////////////////////////////////////////////////////////////////////////

class NamedType : public Type {
public:
    NamedType(Tag t, void* td, TypeManager& tm) 
        : Type(t), vmTypeHandle(td), typeManager(tm) {}
    virtual ~NamedType() {}

    NamedType* asNamedType() { return this; }
    bool needsInitialization();
    bool isFinalizable();
    bool isLikelyExceptionType();
    bool isNamedType() {return true;}
    //
    // Returns the runtime identifier for this type; 
    // It is used to communicate a type to the runtime.
    // It is used, for example, to create new objects, perform 
    // dynamic type checking, and look up interface vtables.
    //
    void*    getRuntimeIdentifier();   
    void*    getVMTypeHandle()        {assert(!isUnresolvedType()); return vmTypeHandle;}
protected:
    void*            vmTypeHandle;
    TypeManager&    typeManager;
};

class ValueType : public Type {
public:
    ValueType(Tag t) : Type(t) {}
    virtual ~ValueType() {}
};

class UserValueType : public NamedType {
public:
    UserValueType(void* td,TypeManager& tm) : NamedType(Value,td,tm) {}
    virtual ~UserValueType() {}

    virtual const char* getName();

    //
    // returns size & alignment of the un-boxed value
    //
    virtual U_32        getUnboxedSize(){assert(0); return 0;}
    //virtual U_32        getUnboxedAlignment();
    void        print(::std::ostream& os);
protected:
    UserValueType(Tag t,void* td,TypeManager& tm) : NamedType(t,td,tm) {}
};

class EnumType : public UserValueType {
public:
    EnumType(void* td,TypeManager& tm,Type* ut) 
        : UserValueType(td,tm), underlyingType(ut) {}
    Type*    getUnderlyingType()    {return underlyingType;}
    bool     isEnum()               {return true;}
    void     print(::std::ostream& os);
private:
    Type*    underlyingType;
};

class ObjectType : public NamedType {
public:
    ObjectType(void* td, TypeManager& tm, bool isCompressed=false) 
        : NamedType(isCompressed ? CompressedObject : Object,td,tm) {}
    ObjectType(Tag t, void* td, TypeManager& tm)
        : NamedType(t, td, tm) {}
    ObjectType* asObjectType() { return this; }
    //
    // isFinalClass & isSealed should be moved to ObjectType
    //
    bool    isFinalClass()    {
        return (tag == Type::SystemString || _isFinalClass());
    }
    virtual bool isSealed()    {return isFinalClass();}
    ObjectType*     getSuperType();
    const    char*    getName();

    bool getFastInstanceOfFlag();
    int getClassDepth();
    virtual bool    isUnresolvedType() {return isUnresolvedObject() || isUnresolvedArray();}

    //
    // returns the vtable address of this boxed type
    // returns NULL if the type has not yet been prepared by the VM kernel
    //
    void*           getVTable();
    //
    // returns the allocation handle to use with runtime allocation support functions.
    //
    void*           getAllocationHandle();
    //
    // returns true if this type is a subclass of otherType
    //
    bool            isSubClassOf(NamedType *);
    //
    //  Returns size of the object
    //
    U_32          getObjectSize();
    //
    // for boxed value types, returns byte offset of the un-boxed value
    //

    virtual bool    isUnresolvedArray() const {return false;}
    U_32          getUnboxedOffset();
    bool            isInterface();
    bool            isAbstract();
    virtual void    print(::std::ostream& os);
protected:
    virtual bool    _isFinalClass();
};
class ArrayType : public ObjectType {
public:
    ArrayType(NamedType* elemType_,void* td,TypeManager& tm, bool isCompressed) 
        : ObjectType(isCompressed ? CompressedArray : Array, td, tm), elemType(elemType_) 
    {
    }
    ArrayType* asArrayType() { return this; }
    NamedType*    getElementType()        {return elemType;}
    virtual bool    isUnresolvedArray() const;
    //
    // for array types, returns byte offset of the first element of the array
    //
    U_32    getArrayElemOffset();
    //
    // for array types, returns byte offset of the array's length field
    //
    U_32    getArrayLengthOffset();
    virtual void    print(::std::ostream& os);
protected:
    virtual bool    _isFinalClass() {
        if (elemType->isObject()) 
            return ((ObjectType*)elemType)->isFinalClass();
        else
            return true;
    }
private:
    NamedType*    elemType;
};

class ValueNameType : public Type {
public:
    ValueNameType(Tag t, ValueName v, Type* nvs) : Type(t), value(v), nonValueSuper(nvs) { }

    ValueNameType* asValueNameType() { return this; }
    ValueName getValueName() { return value; }
    virtual void print(::std::ostream& os);

    virtual Type* getNonValueSupertype() { return nonValueSuper; }
    Type* getUnderlyingType();

    // Only for ArrayBase, ArrayLength, or ArrayElement types
    ArrayType* getUnderlyingArrayType();

private:
    ValueName value;
    Type* nonValueSuper;
};

class ITablePtrObjType : public ValueNameType {
public:
    ITablePtrObjType(ValueName v, NamedType* interfaceType, Type* nvs) : ValueNameType(Type::ITablePtrObj, v, nvs), itype(interfaceType) { }

    ITablePtrObjType* asITablePtrObjType() { return this;}
    NamedType* getInterfaceType() { return itype; }
    void print(::std::ostream& os);
private:
    NamedType* itype;
};

class OrNullType : Type {
public:
    OrNullType(Type* t) : Type(Type::OrNull), base(t) { }
    OrNullType* asOrNullType() { return this; }
    Type* getBaseType() { return base; }
private:
    Type* base;
};

class TypeManager {
public:
    TypeManager(MemoryManager& mm);
    virtual ~TypeManager() {}

    void    init();
    //MemoryManager&  getMemManager()        {return memManager;}

    Type* getPrimitiveType(Type::Tag);
    // Convert type to the type which variable will have
    // All sub 32-bit primitives become 32-bit primitives
    Type* toInternalType(Type*);

    Type*         getTauType()             {return tauType;}
    Type*         getVoidType()            {return voidType;}
    NamedType*    getBooleanType()         {return booleanType;}
    NamedType*    getCharType()            {return charType;}
    NamedType*    getIntPtrType()          {return intPtrType;}
    NamedType*    getInt8Type()            {return int8Type;}
    NamedType*    getInt16Type()           {return int16Type;}
    NamedType*    getInt32Type()           {return int32Type;}
    NamedType*    getInt64Type()           {return int64Type;}
    NamedType*    getUIntPtrType()         {return uintPtrType;}
    NamedType*    getUInt8Type()           {return uint8Type;}
    NamedType*    getUInt16Type()          {return uint16Type;}
    NamedType*    getUInt32Type()          {return uint32Type;}
    NamedType*    getUInt64Type()          {return uint64Type;}
    NamedType*    getSingleType()          {return singleType;}
    NamedType*    getDoubleType()          {return doubleType;}
    NamedType*    getFloatType()           {return floatType;}
    Type*         getNullObjectType()      {return &nullObjectType;}
    ValueType*    getTypedReference()      {return &typedReference;}
    Type*         getOffsetType()          {return &offsetType;}
    Type*         getOffsetPlusHeapbaseType()   {return &offsetPlusHeapbaseType;}
    ObjectType*   getSystemStringType()    {return theSystemStringType;}
    ObjectType*   getSystemObjectType()    {return theSystemObjectType;}
    ObjectType*   getUnresolvedObjectType(){return theUnresolvedObjectType;}
    ObjectType*   getSystemClassType()     {return theSystemClassType;}

    Type*         getCompressedNullObjectType()  {return &compressedNullObjectType;}
    ObjectType*   getCompressedSystemStringType(){return compressedSystemStringType;}
    ObjectType*   getCompressedSystemObjectType(){return compressedSystemObjectType;}
    ObjectType*   getCompressedUnresolvedObjectType(){return compressedUnresolvedObjectType;}
    ObjectType*   getCompressedSystemClassType() {return compressedSystemClassType;}

    NamedType*    getValueType(void* vmTypeHandle);
    ObjectType*   getObjectType(void* vmTypeHandle, bool isCompressed=false);
    ArrayType*    getArrayType(Type* elemType, bool isCompressed=false, void* arrayVMTypeHandle=NULL);
    void          initArrayType(Type* elemType, bool isCompressed, void* arrayVMTypeHandle);
    Type*         getCommonType(Type* type1, Type *type2);
    ObjectType*   getCommonObjectType(ObjectType *o1, ObjectType *o2);
    bool          isResolvedAndSubClassOf(Type* type1, Type *type2);
    bool          isSubClassOf(Type* type1, Type *type2) { return isSubTypeOf(type1, type2); }
    bool          isSubTypeOf(Type* type1, Type* type2);

    Type*         uncompressType(Type *compRefType);
    Type*         compressType(Type *uncompRefType);

    PtrType*        getManagedPtrType(Type* pointedToType);
       
    PtrType*        getUnmanagedPtrType(Type* pointedToType);

    MethodPtrType*    getMethodPtrType(MethodDesc* methodDesc);

    UnresolvedMethodPtrType*    getUnresolvedMethodPtrType(ObjectType* enclosingClass, U_32 cpIndex, MethodSignature* sig);
        
    MethodPtrType* getMethodPtrObjType(ValueName obj, MethodDesc* methodDesc);
        
    VTablePtrType*    getVTablePtrType(Type* type);

    OrNullType* getOrNullType(Type* t);

    ValueNameType* getVTablePtrObjType(ValueName val);

    ValueNameType* getITablePtrObjType(ValueName val, NamedType* itype);
        
    ValueNameType* getArrayLengthType(ValueName val);
        
    PtrType* getArrayBaseType(ValueName val);
        
    PtrType* getArrayIndexType(ValueName array, ValueName index);

    ValueNameType* getArrayElementType(ValueName val);
    ValueNameType* getSingletonType(ValueName val);

    // Convert a type to a pre-LBS project type
    Type * convertToOldType(Type*);

    ObjectType*   getObjectTypeFromAllocationHandle(void* vmAllocationHandle, bool isCompressed=false)
    {
        if (vmAllocationHandle==NULL)
            return NULL;
        void * vmTypeHandle = VMInterface::getTypeHandleFromAllocationHandle(vmAllocationHandle);
        if ( vmTypeHandle==NULL 
                || vmTypeHandle>(void*)-100
                || ((POINTER_SIZE_INT)vmTypeHandle&0x3)!=0 ) {
            return NULL;
        }
        return getObjectType(vmTypeHandle, isCompressed);
    }

    void setLazyResolutionMode(bool flag) {lazyResolutionMode = flag;}
    bool isLazyResolutionMode() const {return lazyResolutionMode;}

private:
    MemoryManager& memManager;
    // singletons for the built-in types
    Type*    voidType;
    Type*    tauType;
    NamedType*    booleanType;
    NamedType*    charType;
    NamedType*    intPtrType;
    NamedType*    int8Type;
    NamedType*    int16Type;
    NamedType*    int32Type;
    NamedType*    int64Type;
    NamedType*    uintPtrType;
    NamedType*    uint8Type;
    NamedType*    uint16Type;
    NamedType*    uint32Type;
    NamedType*    uint64Type;
    NamedType*    singleType;
    NamedType*    doubleType;
    NamedType*    floatType;
    ValueType     typedReference;
    ObjectType*   theSystemStringType;
    ObjectType*   theSystemObjectType;
    ObjectType*   theUnresolvedObjectType;
    ObjectType*   theSystemClassType;
    Type          nullObjectType;
    Type          offsetType;
    Type          offsetPlusHeapbaseType;
    ObjectType*   compressedSystemStringType;
    ObjectType*   compressedSystemObjectType;
    ObjectType*   compressedUnresolvedObjectType;
    ObjectType*   compressedSystemClassType;
    Type          compressedNullObjectType;
    // hashtable for user-defined object and value types
    PtrHashTable<NamedType>        userValueTypes;
    PtrHashTable<ObjectType>       userObjectTypes;
    PtrHashTable<PtrType>          managedPtrTypes;
    PtrHashTable<PtrType>          unmanagedPtrTypes;
    PtrHashTable<ArrayType>        arrayTypes;
    PtrHashTable<MethodPtrType>    methodPtrTypes;
    PtrHashTable<VTablePtrType>    vtablePtrTypes;

    PtrHashTable<ObjectType>       compressedUserObjectTypes;
    PtrHashTable<ArrayType>        compressedArrayTypes;
    PtrHashTable<MethodPtrType>    compressedMethodPtrTypes;
    PtrHashTable<VTablePtrType>    compressedVtablePtrTypes;

    PtrHashTable<ValueNameType>    singletonTypes;
    PtrHashTable<OrNullType>       orNullTypes;
    PtrHashTable<ValueNameType>    vtableObjTypes;
    PtrHashTable<ValueNameType>    arrayLengthTypes;
    PtrHashTable<PtrType>          arrayBaseTypes;
    PtrHashTable<ValueNameType>    arrayElementTypes;

    PtrHashTable< PtrHashTable<PtrType> >          arrayIndexTypes;
    PtrHashTable< PtrHashTable<MethodPtrType> >    methodPtrObjTypes;
    PtrHashTable< PtrHashTable<UnresolvedMethodPtrType> > unresMethodPtrTypes;
    PtrHashTable< PtrHashTable<ITablePtrObjType> > itableObjTypes;

    bool areReferencesCompressed;

    NamedType* initBuiltinType(Type::Tag tag);
    void*       getBuiltinValueTypeVMTypeHandle(Type::Tag);

    void*        systemObjectVMTypeHandle;
    void*        systemClassVMTypeHandle;
    void*        systemStringVMTypeHandle;
    bool         lazyResolutionMode;
};

class MethodSignature {
public:
    MethodSignature(){}
    virtual ~MethodSignature(){}

    virtual U_32 getNumParams() const = 0;
    virtual Type** getParamTypes() const = 0;
    virtual Type* getRetType() const = 0;
    virtual const char* getSignatureString() const = 0;
};
} //namespace Jitrino 

#endif // _TYPE_H_
