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

#include <stdio.h>
#include "Opnd.h"
#include "Type.h"
#include "VMInterface.h"
#include "PlatformDependant.h"

namespace Jitrino {

U_32 Type::nextTypeId = 1;

bool Type::mayAlias(TypeManager* typeManager, Type* t1, Type* t2)
{
    assert(t1 && t2);
    t1 = t1->getNonValueSupertype();
    t2 = t2->getNonValueSupertype();
    if (t1==t2) return true;
    if (t1->isUnresolvedType() || t2->isUnresolvedType()) {
        return false;
    }
    return typeManager->isSubTypeOf(t1, t2) || typeManager->isSubClassOf(t2, t1);
}

bool Type::mayAliasPtr(Type* t1, Type* t2)
{
    assert(t1->isPtr() || t1->isObject() || t1->isMethodPtr() || t1->isVTablePtr());
    assert(t2->isPtr() || t2->isObject() || t2->isMethodPtr() || t2->isVTablePtr());

    // References off of null objects are invalid, so should not interfere 
    // with other accesses or each other.  (Although invalid, these seems
    // to show up, at least briefly, due to constant folding).
    if (t1->isNullObject() || t2->isNullObject() || t1->isUnresolvedType() || t2->isUnresolvedType()) {
        return false;
    }

    if (t1==t2) return true;

    // Assume no information about unmanaged pointers.
    if(t1->isUnmanagedPtr() || t2->isUnmanagedPtr())
        return true;

    if(t1->isObject()) {
        if(t2->isObject()) {
            ObjectType* o1 = (ObjectType*) t1;
            ObjectType* o2 = (ObjectType*) t2;
            return o1 == o2 || o1->isSubClassOf(o2) || o2->isSubClassOf(o1);
        }
    } else if(t1->isManagedPtr()) {
        if(t2->isManagedPtr()) {
            PtrType* p1 = (PtrType*) t1;
            PtrType* p2 = (PtrType*) t2;
            Type* e1 = p1->getPointedToType()->getNonValueSupertype();
            Type* e2 = p2->getPointedToType()->getNonValueSupertype();
            return e1==e2;
        }
    } else if(t1->isMethodPtr()) {
        if(t2->isMethodPtr()) {
            MethodPtrType* p1 = (MethodPtrType*) t1;
            MethodPtrType* p2 = (MethodPtrType*) t2;
            MethodDesc* m1 = p1->getMethodDesc();
            MethodDesc* m2 = p2->getMethodDesc();
            return (m1->getId() == m2->getId()) && mayAliasPtr(m1->getParentType(), m2->getParentType());
        }
    } else if(t1->isVTablePtr()) {
        if(t2->isVTablePtr()) {
            VTablePtrType* p1 = (VTablePtrType*) t1;
            VTablePtrType* p2 = (VTablePtrType*) t2;
            return mayAliasPtr(p1->getBaseType(), p2->getBaseType());
        }
    }

    return false;
}

Type* TypeManager::getPrimitiveType(Type::Tag t)
{
    switch (t) {
    case Type::Int8:    return getInt8Type();
    case Type::Int16:   return getInt16Type();
    case Type::Int32:   return getInt32Type();
    case Type::Int64:   return getInt64Type();
    case Type::UInt8:   return getUInt8Type();
    case Type::UInt16:  return getUInt16Type();
    case Type::UInt32:  return getUInt32Type();
    case Type::UInt64:  return getUInt64Type();
    case Type::Single:  return getSingleType();
    case Type::Double:  return getDoubleType();
    case Type::Float:   return getFloatType();
    case Type::IntPtr:  return getIntPtrType();
    case Type::UIntPtr: return getUIntPtrType();
    case Type::Void: return getVoidType();
    case Type::Tau:     return getTauType();
    case Type::Boolean: return getBooleanType();
    case Type::Char:    return getCharType();
    default:            assert(0); return NULL;
    }
}

Type* TypeManager::toInternalType(Type* t)
{
    switch (t->tag) {
    case Type::Tau:
    case Type::Void:
    case Type::IntPtr:
    case Type::Int32:
    case Type::Int64:
    case Type::UIntPtr:
    case Type::UInt32:
    case Type::UInt64:
    case Type::Single:
    case Type::Double:
    case Type::Float:
    case Type::SystemObject:
    case Type::SystemClass:
    case Type::SystemString:
    case Type::NullObject:
    case Type::Array:
    case Type::Object:
    case Type::UnresolvedObject:
    case Type::UnmanagedPtr:
    case Type::ManagedPtr:
    case Type::CompressedSystemObject:
    case Type::CompressedSystemClass:
    case Type::CompressedSystemString:
    case Type::CompressedNullObject:
    case Type::CompressedUnresolvedObject:
    case Type::CompressedArray:
    case Type::CompressedObject:
    case Type::CompressedMethodPtr:
    case Type::CompressedVTablePtr:
    case Type::Singleton:
    case Type::VTablePtrObj:
    case Type::ITablePtrObj:
    case Type::ArrayLength:
    case Type::ArrayElementType:
        return t;
    case Type::Boolean:
    case Type::Char:
    case Type::Int8:
    case Type::Int16:
    case Type::UInt8:
    case Type::UInt16:
        return getInt32Type();
    default:
        assert(0);
        return NULL;
    }
}

bool TypeManager::isResolvedAndSubClassOf(Type* type1, Type *type2) {
    if (type1->isUnresolvedType() || type2->isUnresolvedType()) {
        return false;
    }
    return isSubClassOf(type1, type2);
}

bool TypeManager::isSubTypeOf(Type *type1, Type *type2) {
    if (type1==type2) return true;
    bool oneIsCompressed = type1->isCompressedReference();
    bool twoIsCompressed = type2->isCompressedReference();

    if (oneIsCompressed != twoIsCompressed) return false;

    switch (type1->tag) {
    case Type::SystemClass:
        // java/lang/class has only one ancestor - java/lang/Object
        return type2->tag == Type::SystemObject;
    case Type::SystemString:
    case Type::Object:
    object_type:
        // These are subtypes of other object types according to the VM
        if (type2->isObject() && type2!=getNullObjectType()) {
            ObjectType* ot1 = type1->asObjectType();
            ObjectType* ot2 = type2->asObjectType();
            assert(ot1 && ot2);
            return ot1->isSubClassOf(ot2);
        } else {
            return false;
        }
    case Type::NullObject:
        // Subtype of any reference type
        if (type2->isObject()) return true;
        if (type2->isArrayElement() && type2->getNonValueSupertype()->isObject()) return true;
        return false;
    case Type::Array:
        // Subtype of an array of a super-type of its element type, or as an object type
        if (type2->isArrayType()) {
            ArrayType* at1 = type1->asArrayType();
            ArrayType* at2 = type2->asArrayType();
            assert(at1 && at2);
            return isSubTypeOf(at1->getElementType(), at2->getElementType());
        } else {
            goto object_type;
        }
    case Type::Singleton:
        // Subtype of its declared type
        {
            ValueNameType* vnt = type1->asValueNameType();
            assert(vnt);
            return isSubTypeOf(vnt->getUnderlyingType(), type2);
        }
    case Type::ArrayLength:
        // Subtype of itself and I_32
        return type2==getInt32Type();
    case Type::ArrayElementType:
        // Subtype of the declared array element type
        return isSubTypeOf(toInternalType(type1->getNonValueSupertype()), type2);
    case Type::Tau:
    case Type::Void:
    case Type::Boolean:
    case Type::IntPtr:
    case Type::Int8:
    case Type::Int16:
    case Type::Int32:
    case Type::Int64:
    case Type::UIntPtr:
    case Type::UInt8:
    case Type::UInt16:
    case Type::UInt32:
    case Type::UInt64:
    case Type::Single:
    case Type::Double:
    case Type::Float:
    case Type::SystemObject:
    case Type::ManagedPtr:
    case Type::VTablePtrObj:
        // These types are only subtypes of themselves, which is handled above
    default:
        return false;
    }
}


ObjectType * TypeManager::getCommonObjectType(ObjectType *o1, ObjectType *o2) {
    ObjectType *common = NULL;
    if (o1->isUnresolvedType()){
        common = o2;
    } else if (o2->isUnresolvedType()) {
        common = o1;
    } else {
        for ( ; o2 != NULL; o2 = o2->getSuperType()) {
            if (o1->isSubClassOf(o2)) {
                common = o2;
                break;
            } else if (o2->isSubClassOf(o1)) {
                common = o1;
                break;
            }
        }
    }
    return common;
}

Type*   TypeManager::getCommonType(Type *type1, Type* type2) {
    assert(type1 != NULL && type2 != NULL);
    if (type1 == type2)
        return type1;
    Type *common = NULL;
    bool oneIsCompressed = type1->isCompressedReference();
    assert(type1->isCompressedReference() == type2->isCompressedReference());
    if (type1->isUnresolvedType() || type2->isUnresolvedType()) {
        if (type1->isNullObject()) return type2;
        if (type2->isNullObject()) return type1;
        return type1->isUnresolvedType() ? type1 : type2;
    }
    if ( type2->isObject() && (oneIsCompressed 
        ? (type1 == getCompressedNullObjectType()) 
        : (type1 == getNullObjectType())) ) {
        return type2;
    } else if ( type1->isObject() && (oneIsCompressed
        ? (type2 == getCompressedNullObjectType()) 
        : (type2 == getNullObjectType())) ) {
        return type1;
    } else if (type1->isArrayType()) {
        if (type2->isArrayType()) {
            type1 = ((ArrayType*)type1)->getElementType();
            type2 = ((ArrayType*)type2)->getElementType();
            if (type1->isObject() && type2->isObject()) {
                common = getArrayType(getCommonObjectType((ObjectType*)type1,(ObjectType*)type2),
                                      oneIsCompressed);
            } else {
                common = (oneIsCompressed 
                          ? getCompressedSystemObjectType() 
                          : getSystemObjectType());
            }
        }
        else if (type2->isObject())
            common = (oneIsCompressed ? getCompressedSystemObjectType() : getSystemObjectType());
    } else if (type1->isObject()) {
        if (type2->isArrayType())
            common = (oneIsCompressed ? getCompressedSystemObjectType() : getSystemObjectType());
        else if (type2->isObject())
            common = getCommonObjectType((ObjectType*)type1,(ObjectType*)type2);
    }
  
    // Note: common may be NULL at this point.  This means type1 and type2
    // are incompatible.
    return common;
}

TypeManager::TypeManager(MemoryManager& mm) :
    memManager(mm),
    floatType(),
    typedReference(Type::TypedReference),
    theSystemStringType(NULL), 
    theSystemObjectType(NULL),
    theUnresolvedObjectType(NULL),
    theSystemClassType(NULL),
    nullObjectType(Type::NullObject), 
    offsetType(Type::Offset),
    offsetPlusHeapbaseType(Type::OffsetPlusHeapbase),

    compressedSystemStringType(NULL), 
    compressedSystemObjectType(NULL),
    compressedUnresolvedObjectType(NULL),
    compressedSystemClassType(NULL),
    compressedNullObjectType(Type::CompressedNullObject), 

    userValueTypes(mm,32), 
    userObjectTypes(mm,32), 
    managedPtrTypes(mm,32), 
    unmanagedPtrTypes(mm,32), 
    arrayTypes(mm,32), 
    methodPtrTypes(mm,32),
    vtablePtrTypes(mm,32),

    compressedUserObjectTypes(mm,32), 
    compressedArrayTypes(mm,32), 
    compressedMethodPtrTypes(mm,32),
    compressedVtablePtrTypes(mm,32),

    singletonTypes(mm, 32),
    orNullTypes(mm, 32),
    vtableObjTypes(mm, 32),

    arrayLengthTypes(mm, 32),
    arrayBaseTypes(mm, 32),
    arrayElementTypes(mm, 32),
    arrayIndexTypes(mm, 32),
    methodPtrObjTypes(mm,32),
    unresMethodPtrTypes(mm, 32),
    itableObjTypes(mm, 32),

    areReferencesCompressed(false)
{
    tauType=voidType=booleanType=charType=intPtrType=int8Type=int16Type=NULL;
    int32Type=int64Type=uintPtrType=uint8Type=uint16Type=NULL;
       uint32Type=uint64Type=singleType=doubleType=floatType=NULL;
       systemObjectVMTypeHandle = systemClassVMTypeHandle = systemStringVMTypeHandle = NULL;
       lazyResolutionMode = false;
}

NamedType* 
TypeManager::initBuiltinType(Type::Tag tag) {
    void* vmTypeHandle = getBuiltinValueTypeVMTypeHandle(tag);
    NamedType* type = new (memManager) NamedType(tag,vmTypeHandle,*this);
    if (vmTypeHandle != NULL) {
        userValueTypes.insert(vmTypeHandle,type);
    }
    return type;
}

void
TypeManager::init() {
    areReferencesCompressed = VMInterface::areReferencesCompressed();
    void* systemStringVMTypeHandle = VMInterface::getSystemStringVMTypeHandle();
    void* systemObjectVMTypeHandle = VMInterface::getSystemObjectVMTypeHandle();
    void* systemClassVMTypeHandle  = VMInterface::getSystemClassVMTypeHandle();
    theSystemStringType = new (memManager) 
        ObjectType(Type::SystemString,systemStringVMTypeHandle,*this);
    theSystemObjectType = new (memManager) 
        ObjectType(Type::SystemObject,systemObjectVMTypeHandle,*this);
    theUnresolvedObjectType= new (memManager) 
        ObjectType(Type::UnresolvedObject,systemObjectVMTypeHandle,*this);
    theSystemClassType = new (memManager) 
        ObjectType(Type::SystemClass,systemClassVMTypeHandle,*this);
    userObjectTypes.insert(systemStringVMTypeHandle,theSystemStringType);
    userObjectTypes.insert(systemObjectVMTypeHandle,theSystemObjectType);
    userObjectTypes.insert(systemClassVMTypeHandle,theSystemClassType);

    compressedSystemStringType = new (memManager) 
        ObjectType(Type::CompressedSystemString,systemStringVMTypeHandle,*this);
    compressedSystemObjectType = new (memManager) 
        ObjectType(Type::CompressedSystemObject,systemObjectVMTypeHandle,*this);
    compressedUnresolvedObjectType = new (memManager) 
        ObjectType(Type::CompressedUnresolvedObject,systemObjectVMTypeHandle,*this);
    compressedSystemClassType = new (memManager) 
        ObjectType(Type::CompressedSystemClass,systemClassVMTypeHandle,*this);
    compressedUserObjectTypes.insert(systemStringVMTypeHandle,compressedSystemStringType);
    compressedUserObjectTypes.insert(systemObjectVMTypeHandle,compressedSystemObjectType);
    compressedUserObjectTypes.insert(systemClassVMTypeHandle,compressedSystemClassType);

    tauType = new (memManager) Type(Type::Tau);
    voidType = new (memManager) Type(Type::Void);
    booleanType = initBuiltinType(Type::Boolean);
    charType = initBuiltinType(Type::Char);
    intPtrType = initBuiltinType(Type::IntPtr);
    int8Type = initBuiltinType(Type::Int8);
    int16Type = initBuiltinType(Type::Int16);
    int32Type = initBuiltinType(Type::Int32);
    int64Type = initBuiltinType(Type::Int64);
    uintPtrType = initBuiltinType(Type::UIntPtr);
    uint8Type = initBuiltinType(Type::UInt8);
    uint16Type = initBuiltinType(Type::UInt16);
    uint32Type = initBuiltinType(Type::UInt32);
    uint64Type = initBuiltinType(Type::UInt64);
    singleType = initBuiltinType(Type::Single);
    doubleType = initBuiltinType(Type::Double);
    floatType = initBuiltinType(Type::Float);
}

ArrayType*    
TypeManager::getArrayType(Type* elemType, bool isCompressed, void* arrayVMTypeHandle) {
    PtrHashTable<ArrayType> &lookupTable = 
        isCompressed ? compressedArrayTypes : arrayTypes;
    if (elemType->isObject() || elemType->isValue()) {
        if (elemType->isCompressedReference()) {
            elemType = uncompressType(elemType);
        }
        NamedType* elemNamedType = (NamedType*)elemType;
        //
        // change lookup to vmtypehandle of elem (elemVMTypeHandle)
        //
        ArrayType* type = lookupTable.lookup(elemNamedType);
        if (type == NULL) {
            bool isUnboxed = elemType->isValue();
            if(elemType->isUserValue()) {
                isUnboxed = false;
            }
            if (arrayVMTypeHandle == NULL) {
                if (elemNamedType->isUnresolvedType()) {
                    arrayVMTypeHandle = NULL;
                } else {
                    arrayVMTypeHandle = VMInterface::getArrayVMTypeHandle(elemNamedType->getVMTypeHandle(),isUnboxed);
                }
            }
            type = new (memManager)  ArrayType(elemNamedType,arrayVMTypeHandle,*this, isCompressed);
            if (type->isUnresolvedType() || type->getAllocationHandle()!=0) 
            { // type can be cached
                lookupTable.insert(elemNamedType,type);
            }
        }
        return type;
     }
    assert(0);
    return NULL;
}

ObjectType*    
TypeManager::getObjectType(void* vmTypeHandle, bool isCompressed) {
    if (VMInterface::isArrayType(vmTypeHandle)) {
        void* elemClassHandle = VMInterface::getArrayElemVMTypeHandle(vmTypeHandle);
        assert(elemClassHandle != NULL); 
        NamedType* elemType;
        if (VMInterface::isArrayOfPrimitiveElements(vmTypeHandle)) {
            elemType = getValueType(elemClassHandle);
        } else {
            elemType = getObjectType(elemClassHandle, areReferencesCompressed);
        }
        return getArrayType(elemType, isCompressed, vmTypeHandle);
    }
    PtrHashTable<ObjectType> &typeTable = (isCompressed 
                                           ? compressedUserObjectTypes 
                                           : userObjectTypes);
    ObjectType* type = typeTable.lookup(vmTypeHandle);
    if (type == NULL) {
        type = new (memManager) ObjectType(vmTypeHandle, *this, isCompressed);
        typeTable.insert(vmTypeHandle,type);
    }
    return type;
}

NamedType*    
TypeManager::getValueType(void* vmTypeHandle) {
    NamedType* type = userValueTypes.lookup(vmTypeHandle);
    if (type == NULL) {
        assert(0);
        type = new (memManager) UserValueType(vmTypeHandle,*this);
        userValueTypes.insert(vmTypeHandle,type);
    }
    return type;
}


PtrType*  
TypeManager::getManagedPtrType(Type* pointedToType) {
    PtrType* type = managedPtrTypes.lookup(pointedToType);
    if (type == NULL) {
        type = new (memManager) PtrType(pointedToType,true);
        managedPtrTypes.insert(pointedToType,type);
    }
    return type;
}
PtrType*
TypeManager::getUnmanagedPtrType(Type* pointedToType) {
    PtrType* type = unmanagedPtrTypes.lookup(pointedToType);
    if (type == NULL) {
        type = new (memManager) PtrType(pointedToType,false);
        unmanagedPtrTypes.insert(pointedToType,type);
    }
    return type;
}
MethodPtrType*    
TypeManager::getMethodPtrType(MethodDesc* methodDesc) {
    MethodPtrType* type = methodPtrTypes.lookup(methodDesc);
    if (type == NULL) {
        type = new (memManager) MethodPtrType(methodDesc,*this);
        methodPtrTypes.insert(methodDesc,type);
    }
    return type;
}

UnresolvedMethodPtrType*    
TypeManager::getUnresolvedMethodPtrType(ObjectType* enclosingClass, U_32 cpIndex, MethodSignature* sig) {
    PtrHashTable<UnresolvedMethodPtrType>* methodsPerClass = unresMethodPtrTypes.lookup(enclosingClass);
    if (!methodsPerClass) {
        methodsPerClass = new (memManager) PtrHashTable<UnresolvedMethodPtrType>(memManager, 32);
        unresMethodPtrTypes.insert(enclosingClass, methodsPerClass);
    }
    UnresolvedMethodPtrType* methType = methodsPerClass->lookup((void*)(POINTER_SIZE_INT)cpIndex);
    if (!methType) {
        methType = new (memManager) UnresolvedMethodPtrType(enclosingClass, cpIndex, *this, 
            sig->getNumParams(), sig->getParamTypes(), sig->getRetType(), sig->getSignatureString());
        methodsPerClass->insert((void*)(POINTER_SIZE_INT)cpIndex, methType);
    }
    return methType;
}

MethodPtrType* 
TypeManager::getMethodPtrObjType(ValueName obj, MethodDesc* methodDesc) {
    PtrHashTable<MethodPtrType>* ptrTypes = methodPtrObjTypes.lookup(methodDesc);
    if (!ptrTypes) {
        ptrTypes = new (memManager) PtrHashTable<MethodPtrType>(memManager, 32);
        methodPtrObjTypes.insert(methodDesc, ptrTypes);
    }
    MethodPtrType* ptrType = ptrTypes->lookup(obj);
    if (!ptrType) {
        ptrType = new (memManager) MethodPtrType(methodDesc, *this, false, obj);
        ptrTypes->insert(obj, ptrType);
    }
    return ptrType;
}

VTablePtrType*    
TypeManager::getVTablePtrType(Type* type) {
    VTablePtrType* vtableType = vtablePtrTypes.lookup(type);
    if (vtableType == NULL) {
        vtableType = new (memManager) VTablePtrType(type);
        vtablePtrTypes.insert(type,vtableType);
    }
    return vtableType;
}

OrNullType* 
TypeManager::getOrNullType(Type* t) {
    OrNullType* orNullType = orNullTypes.lookup(t);
    if (!orNullType) {
        orNullType = new (memManager) OrNullType(t);
        orNullTypes.insert(t, orNullType);
    }
    return orNullType;
}

ValueNameType* 
TypeManager::getVTablePtrObjType(ValueName val) {
    ValueNameType* vtablePtrType = vtableObjTypes.lookup(val);
    if (!vtablePtrType) {
        vtablePtrType = new (memManager) ValueNameType(Type::VTablePtrObj, val, getIntPtrType());
        vtableObjTypes.insert(val, vtablePtrType);
    }
    return vtablePtrType;
}

ValueNameType* 
TypeManager::getITablePtrObjType(ValueName val, NamedType* itype) {
    PtrHashTable<ITablePtrObjType>* itableTypes = itableObjTypes.lookup(val);
    if (!itableTypes) {
        itableTypes = new (memManager) PtrHashTable<ITablePtrObjType>(memManager, 32);
        itableObjTypes.insert(val, itableTypes);
    }
    ITablePtrObjType* itablePtrType = itableTypes->lookup(itype);
    if (!itablePtrType) {
        itablePtrType = new (memManager) ITablePtrObjType(val, itype, getIntPtrType());
        itableTypes->insert(itype, itablePtrType);
    }
    return itablePtrType;
}

ValueNameType* 
TypeManager::getArrayLengthType(ValueName val) {
    ValueNameType* arrayLengthType = arrayLengthTypes.lookup(val);
    if (!arrayLengthType) {
        arrayLengthType = new (memManager) ValueNameType(Type::ArrayLength, val, getInt32Type());
        arrayLengthTypes.insert(val, arrayLengthType);
    }
    return arrayLengthType;
}

PtrType* 
TypeManager::getArrayBaseType(ValueName val) {
    PtrType* arrayBaseType = arrayBaseTypes.lookup(val);
    if (!arrayBaseType) {
        Type* elementType = getArrayElementType(val);
        arrayBaseType = new (memManager) PtrType(elementType, true, val);
        arrayBaseTypes.insert(val, arrayBaseType);
    }
    return arrayBaseType;
}

PtrType* 
TypeManager::getArrayIndexType(ValueName array, ValueName index)
{
    PtrHashTable<PtrType>* indexTypes = arrayIndexTypes.lookup(array);
    if (!indexTypes) {
        indexTypes = new (memManager) PtrHashTable<PtrType>(memManager, 32);
        arrayIndexTypes.insert(array, indexTypes);
    }
    PtrType* indexType = indexTypes->lookup(index);
    if (!indexType) {
        Type* elementType = getArrayElementType(array);
        indexType = new (memManager) PtrType(elementType, true, array, index);
        indexTypes->insert(index, indexType);
    }
    return indexType;
}

bool    
ObjectType::_isFinalClass() {
    if (isUnresolvedType()) {
        return false;
    }
    return VMInterface::isFinalType(vmTypeHandle);
}

bool    
ObjectType::isInterface() {
    assert(!isUnresolvedType());
    return VMInterface::isInterfaceType(vmTypeHandle);
}

bool    
ObjectType::isAbstract() {
    assert(!isUnresolvedType());
    return VMInterface::isAbstractType(vmTypeHandle);
}

bool    
NamedType::needsInitialization() {
    return VMInterface::needsInitialization(vmTypeHandle);
}

bool    
NamedType::isFinalizable() {
    assert(!isUnresolvedType());
    if (isInterface()) {
        return false;
    }
    return VMInterface::isFinalizable(vmTypeHandle);
}


bool    
NamedType::isLikelyExceptionType() {
    assert(!isUnresolvedType());
    return VMInterface::isLikelyExceptionType(vmTypeHandle);
}

void*
NamedType::getRuntimeIdentifier() {
    assert(!isUnresolvedType());
    return vmTypeHandle;
}

ObjectType*
ObjectType::getSuperType() {
    assert(!isUnresolvedObject());
    void* superTypeVMTypeHandle = VMInterface::getSuperTypeVMTypeHandle(vmTypeHandle);
    if (superTypeVMTypeHandle)
        return typeManager.getObjectType(superTypeVMTypeHandle,
                                         isCompressedReference());
    else
        return 0;
}

//
// returns the vtable address of this boxed type
// returns NULL if the type has not yet been prepared by the VM kernel
//
void*    
ObjectType::getVTable() {
    assert(!isUnresolvedType());
    return VMInterface::getVTable(vmTypeHandle);
}

//
// returns the allocation handle for use with runtime allocation support functions.
//
void*    
ObjectType::getAllocationHandle() {
    assert(!isUnresolvedType());
    return VMInterface::getAllocationHandle(vmTypeHandle);
}
//
// returns true if this type is a subclass of otherType
//
bool
ObjectType::isSubClassOf(NamedType *other) {
    assert(!isUnresolvedType());
    return VMInterface::isSubClassOf(vmTypeHandle,other->getRuntimeIdentifier());
}

//
// yields the corresponding uncompressed reference type
//
Type*
TypeManager::uncompressType(Type *compRefType)
{
    switch (compRefType->tag) {
    case Type::CompressedNullObject:
        return getNullObjectType();
    case Type::CompressedArray:
        {
            NamedType* elemType = ((ArrayType*)compRefType)->getElementType();
            return getArrayType(elemType, false); // uncompressed
        }
    case Type::CompressedSystemObject:
        return getSystemObjectType();
    case Type::CompressedUnresolvedObject:
        return getUnresolvedObjectType();
    case Type::CompressedSystemClass:
        return getSystemClassType();
    case Type::CompressedSystemString:
        return getSystemStringType();
    case Type::CompressedObject:
        {
            void *vmTypeHandle = ((NamedType *)compRefType)->getVMTypeHandle();
            return getObjectType(vmTypeHandle, false); // uncompressed
        }
    default:
        assert(0);
        return 0;
    }
}

//
// yields the corresponding compressed reference type
//
Type*
TypeManager::compressType(Type *uncompRefType)
{
    switch (uncompRefType->tag) {
    case Type::NullObject:
        return getCompressedNullObjectType();
    case Type::Array:
        {
            NamedType* elemType = ((ArrayType*)uncompRefType)->getElementType();
            return getArrayType(elemType, true); // compressed
        }
    case Type::SystemObject:
        return getCompressedSystemObjectType();
    case Type::UnresolvedObject:
        return getCompressedUnresolvedObjectType();
    case Type::SystemClass:
        return getCompressedSystemClassType();
    case Type::SystemString:
        return getCompressedSystemStringType();
    case Type::Object:
        {
            void *vmTypeHandle = ((NamedType *)uncompRefType)->getVMTypeHandle();
            return getObjectType(vmTypeHandle, true); // compressed
        }
    default:
        assert(0);
        return 0;
    }
}

//
//  Returns size of the object
//
U_32
ObjectType::getObjectSize() {
    assert(!isUnresolvedObject());
    return VMInterface::getObjectSize(vmTypeHandle);
}

const char* 
ObjectType::getName() {
    if (isUnresolvedObject()) {
        return ".Unresolved";
    } else if (isUnresolvedArray()) {
        return ".Unresolved[]";
    }
    assert(vmTypeHandle);
    return VMInterface::getTypeName(vmTypeHandle);
}

bool 
ObjectType::getFastInstanceOfFlag() {
    if (isUnresolvedType()) {
        return false;
    }
    return VMInterface::getClassFastInstanceOfFlag(vmTypeHandle);
}

int 
ObjectType::getClassDepth() {
    assert(!isUnresolvedType());
    return VMInterface::getClassDepth(vmTypeHandle);
}

//
// for array types, returns byte offset of the first element of the array
//
U_32    
ArrayType::getArrayElemOffset()    {
    bool isUnboxed = elemType->isValueType();
    if (elemType->isUnresolvedType()) {
        //workaround to keep assertion in public getVMTypeHandle() method for unresolved types
        //actually any suitable object is OK here to get an offset
        return VMInterface::getArrayElemOffset(typeManager.getSystemObjectType()->getVMTypeHandle(),isUnboxed);
    }
    return VMInterface::getArrayElemOffset(elemType->getVMTypeHandle(),isUnboxed);
}

//
// for array types, returns byte offset of the array's length field
//
U_32    
ArrayType::getArrayLengthOffset() {
    return VMInterface::getArrayLengthOffset();
}

bool    ArrayType::isUnresolvedArray() const {
    bool res = elemType->isUnresolvedObject();
    res = res || (elemType->isArrayType() && elemType->asArrayType()->isUnresolvedArray());
    return res;
}

// predefined value types
const char*
Type::getName() {
    const char* s;
    switch (tag) {
    case Type::Void:             s = "VSystem/Void";     break;
    case Type::Tau:              s = "VSystem/Tau";     break;
    case Type::Boolean:          s = "VSystem/Boolean";  break;
    case Type::Char:             s = "VSystem/Char";     break;
    case Type::IntPtr:           s = "VSystem/IntPtr";   break;
    case Type::Int8:             s = "VSystem/SByte";    break;
    case Type::Int16:            s = "VSystem/Int16";    break;
    case Type::Int32:            s = "VSystem/Int32";    break;
    case Type::Int64:            s = "VSystem/Int64";    break;
    case Type::UIntPtr:          s = "VSystem/UIntPtr";  break;
    case Type::UInt8:            s = "VSystem/UInt8";    break;
    case Type::UInt16:           s = "VSystem/UInt16";   break;
    case Type::UInt32:           s = "VSystem/UInt32";   break;
    case Type::UInt64:           s = "VSystem/UInt64";   break;
    case Type::Single:           s = "VSystem/Single";   break;
    case Type::Double:           s = "VSystem/Double";   break;
    case Type::Float:            s = "VSystem/Float";    break;
    case Type::TypedReference:   s = "VSystem/TypedRef"; break;
    default:               s = "VSystem/???";      break;
    }
    return s;
}

const char* 
UserValueType::getName() {
    return VMInterface::getTypeName(vmTypeHandle);
}

//-----------------------------------------------------------------------------
// Method for printing types.  Move to an IRPrinter file.
//-----------------------------------------------------------------------------
void    Type::print(::std::ostream& os) {
    const char* s;
    switch (tag) {
    case Tau:              s = "tau"; break;
    case Void:             s = "void"; break;
    case Boolean:          s = "bool"; break;
    case Char:             s = "char"; break;
    case IntPtr:           s = "intptr"; break;
    case Int8:             s = "I_8"; break;
    case Int16:            s = "int16"; break;
    case Int32:            s = "I_32"; break;
    case Int64:            s = "int64"; break;
    case UIntPtr:          s = "uintptr"; break;
    case UInt8:            s = "U_8"; break;
    case UInt16:           s = "uint16"; break;
    case UInt32:           s = "U_32"; break;
    case UInt64:           s = "uint64"; break;
    case Single:           s = "single"; break;
    case Double:           s = "double"; break;
    case Float:            s = "float"; break;
    case TypedReference:   s = "typedref"; break;
    case Value:            s = "value"; break;
    case SystemObject:     s = "object"; break;
    case SystemClass:      s = "class"; break;
    case SystemString:     s = "string"; break;
    case Array:            s = "[]"; break;
    case Object:           s = "object"; break;
    case NullObject:       s = "null_object"; break;
    case UnresolvedObject: s = "unres_object"; break;
    case Offset:           s = "offset"; break;
    case OffsetPlusHeapbase: s = "offsetplushb"; break;
    case UnmanagedPtr:     s = "ptr"; break;
    case ManagedPtr:       s = "&"; break;
    case MethodPtr:        s = "method"; break;
    case VTablePtr:        s = "vtable"; break;
    case CompressedSystemObject:     s = "cmpobject"; break;
    case CompressedSystemClass:      s = "cmpclass"; break;
    case CompressedSystemString:     s = "cmpstring"; break;
    case CompressedArray:            s = "cmp[]"; break;
    case CompressedObject:           s = "cmpo"; break;
    case CompressedNullObject:       s = "cmpnull"; break;
    case CompressedUnresolvedObject: s = "cmpunreso"; break;
    default:               s = "???"; break;
    }
    os << s;
}

void    UserValueType::print(::std::ostream& os) {
    os << "struct:" << getName();
}

void    EnumType::print(::std::ostream& os) {
    os << "enum:" << getName();
}

extern const char *messageStr(const char *);

void    ObjectType::print(::std::ostream& os) {
    if (isCompressedReference()) {
        os << "clsc:" << getName();
    } else {
        os << "cls:" << getName();
    }
}

void    ArrayType::print(::std::ostream& os) {
    if (isCompressedReference()) {
        os << "[]c" << getName();
    } else {
        elemType->print(os);
        os << "[]";
    }
}

void    PtrType::print(::std::ostream& os) {
    if (isManagedPtr()) {
        os << "ref:";
    } else    {
        os << "ptr:";
    }
    if (array) {
        os << "(";
        array->print(os);
        if (index) {
            os << ",";
            index->print(os);
        }
        os << ")";
    }
    pointedToType->print(os);
}

Type* MethodPtrType::getParamType(U_32 i)
{
    return (i==0 && object ? typeManager.getSingletonType(object) : methodDesc->getParamType(i));
}

void    MethodPtrType::print(::std::ostream& os) {
    if (object) {
        os << "method("; object->print(os); os << "):";
    } else {
        os << "method:";
    }
    os << messageStr(methodDesc->getName());
}

void UnresolvedMethodPtrType::print(::std::ostream& os) {
    os<<signatureStr;
}


void    VTablePtrType::print(::std::ostream& os) {
    os << "vtb:";
    baseType->print(os);
}

void ValueNameType::print(::std::ostream& os)
{
    os << getPrintString() << "("; getValueName()->print(os); os << ")";
}

Type* ValueNameType::getUnderlyingType()
{
    assert(getValueName() && getValueName()->getType());
    return getValueName()->getType();
}

ArrayType* ValueNameType::getUnderlyingArrayType()
{
    assert(tag==Type::ArrayElementType || tag==Type::ArrayLength);
    ArrayType* at = getUnderlyingType()->asArrayType();
    assert(at);
    return at;
}

void ITablePtrObjType::print(::std::ostream& os)
{
    os << getPrintString() << "("; getValueName()->print(os); os << ","; itype->print(os); os << ")";
}

const char *
Type::getPrintString(Tag t) {
    const char* s;
    switch (t) {
    case Tau:             s = "tau"; break;
    case Void:            s = "v  "; break;
    case Boolean:         s = "b  "; break;
    case Char:            s = "chr"; break;
    case IntPtr:          s = "i  "; break;
    case Int8:            s = "i1 "; break;
    case Int16:           s = "i2 "; break;
    case Int32:           s = "i4 "; break;
    case Int64:           s = "i8 "; break;
    case UIntPtr:         s = "u  "; break;
    case UInt8:           s = "u1 "; break;
    case UInt16:          s = "u2 "; break;
    case UInt32:          s = "u4 "; break;
    case UInt64:          s = "u8 "; break;
    case Single:          s = "r4 "; break;
    case Double:          s = "r8 "; break;
    case Float:           s = "r  "; break;
    case TypedReference:  s = "trf"; break;
    case Value:           s = "val"; break;
    case SystemObject:    s = "obj"; break;
    case SystemClass:     s = "cls"; break;
    case SystemString:    s = "str"; break;
    case NullObject:      s = "nul"; break;
    case Offset:          s = "off"; break;
    case OffsetPlusHeapbase: s = "ohb"; break;
    case Array:           s = "[] "; break;
    case Object:          s = "o  "; break;
    case UnresolvedObject:s = "uno  "; break;
    case UnmanagedPtr:    s = "*  "; break;
    case ManagedPtr:      s = "&  "; break;
    case MethodPtr:       s = "fun"; break;
    case VTablePtr:       s = "vtb"; break;
    case CompressedSystemObject:    s = "cob"; break;
    case CompressedSystemClass:     s = "ccl"; break;
    case CompressedSystemString:    s = "cst"; break;
    case CompressedNullObject:      s = "cnl"; break;
    case CompressedUnresolvedObject:s = "cun"; break;
    case CompressedArray:           s = "c[]"; break;
    case CompressedObject:          s = "co "; break;
    case VTablePtrObj:    s = "vtb"; break;
    case ITablePtrObj:    s = "itb"; break;
    case ArrayLength:     s = "len"; break;
    case ArrayElementType:s = "elem"; break;
    default:              s = "???"; break;
    }
    return s;
}

ValueNameType* TypeManager::getSingletonType(ValueName val) {
    ValueNameType* singletonType = singletonTypes.lookup(val);
    if (!singletonType) {
        singletonType = new (memManager) ValueNameType(Type::Singleton, val, val->getType());
        singletonTypes.insert(val, singletonType);
    }
    return singletonType;
}

ValueNameType* TypeManager::getArrayElementType(ValueName val) {
    ValueNameType* arrayElementType = arrayElementTypes.lookup(val);
    if (!arrayElementType) {
        ArrayType* arrayType = val->getType()->asArrayType();
        assert(arrayType);
        Type* elementType = arrayType->getElementType();
        if (areReferencesCompressed && elementType->isObject())
            elementType = compressType(elementType);
        arrayElementType = new (memManager) ValueNameType(Type::ArrayElementType, val, elementType);
        arrayElementTypes.insert(val, arrayElementType);
    }
    return arrayElementType;
}

Type* TypeManager::convertToOldType(Type* t)
{
    switch (t->tag) {
    case Type::Tau:
    case Type::Void:
    case Type::Boolean:
    case Type::Char:
    case Type::IntPtr:
    case Type::Int8:
    case Type::Int16:
    case Type::Int32:
    case Type::Int64:
    case Type::UIntPtr:
    case Type::UInt8:
    case Type::UInt16:
    case Type::UInt32:
    case Type::UInt64:
    case Type::Single:
    case Type::Double:
    case Type::Float:
    case Type::TypedReference:
    case Type::Value:
    case Type::Offset:
    case Type::OffsetPlusHeapbase:
    case Type::SystemObject:
    case Type::SystemClass:
    case Type::SystemString:
    case Type::NullObject:
    case Type::Array:
    case Type::Object:
    case Type::BoxedValue:
    case Type::VTablePtr:
    case Type::CompressedSystemObject:
    case Type::CompressedSystemClass:
    case Type::CompressedSystemString:
    case Type::CompressedNullObject:
    case Type::CompressedUnresolvedObject:
    case Type::CompressedArray:
    case Type::CompressedObject:
    case Type::CompressedVTablePtr:
        return t;
    case Type::UnmanagedPtr:
    case Type::ManagedPtr:
        {
            PtrType* pt = t->asPtrType();
            assert(pt);
            Type* ptt = convertToOldType(pt->getPointedToType());
            if (t->tag==Type::ManagedPtr)
                return getManagedPtrType(ptt);
            else
                return getUnmanagedPtrType(ptt);
        }
    case Type::MethodPtr:
        {
            MethodPtrType* mpt = t->asMethodPtrType();
            assert(mpt);
            return getMethodPtrType(mpt->getMethodDesc());
        }
    case Type::CompressedMethodPtr:
        {
            MethodPtrType* mpt = t->asMethodPtrType();
            assert(mpt);
            return compressType(getMethodPtrType(mpt->getMethodDesc()));
        }
    case Type::OrNull:
        {
            OrNullType* ont = t->asOrNullType();
            assert(ont);
            return convertToOldType(ont->getBaseType());
        }
    case Type::VTablePtrObj:
        {
            ValueNameType* vnt = t->asValueNameType();
            Type* vt = vnt->getUnderlyingType();
            return getVTablePtrType(vt);
        }
    case Type::ITablePtrObj:
        {
            ITablePtrObjType* itpot = t->asITablePtrObjType();
            return getVTablePtrType(itpot->getInterfaceType());
        }
    case Type::ArrayLength:
        return getInt32Type();
    case Type::ArrayElementType:
    case Type::Singleton:
        return convertToOldType(t->getNonValueSupertype());
    default:
        assert(0);
        return NULL;
    }
}

/*
The following structure and array contain a mapping between Type::Tag and its string representation. 
The array must be
    ordered by Tag
    must cover all available Tag-s
The 'tag; field exists only in debug build and is excluded from the release 
bundle. It's used to control whether the array is arranged properly.
*/
#ifdef _DEBUG
#define DECL_TAG_ITEM(tag, printout)    { Type::tag, #tag, printout }
#else
    #define DECL_TAG_ITEM(tag, printout)    { #tag, printout }
#endif

static const struct {
#ifdef _DEBUG
    Type::Tag       tag;
#endif
    const char *    name;
    // the [5] has no special meaning. That was the max number of 
    // chars used in the existing code, so I decided to keep the 5. 
    // it's ok to increae it if neccessary.
    char        print_name[5];
}
type_tag_names[] = {

    DECL_TAG_ITEM(Tau, "tau "),
    DECL_TAG_ITEM(Void, "v   "),
    DECL_TAG_ITEM(Boolean, "b   "),
    DECL_TAG_ITEM(Char, "chr "),

    DECL_TAG_ITEM(IntPtr, "i   "),
    DECL_TAG_ITEM(Int8, "i1  "),
    DECL_TAG_ITEM(Int16, "i2  "),
    DECL_TAG_ITEM(Int32, "i4  "),
    DECL_TAG_ITEM(Int64, "i8  "),
    DECL_TAG_ITEM(UIntPtr, "u   "),

    DECL_TAG_ITEM(UInt8, "u1  "),
    DECL_TAG_ITEM(UInt16, "u2  "),
    DECL_TAG_ITEM(UInt32, "u4  "),
    DECL_TAG_ITEM(UInt64, "u8  "),

    DECL_TAG_ITEM(Single, "r4  "),
    DECL_TAG_ITEM(Double, "r8  "),
    DECL_TAG_ITEM(Float, "r  "),

    DECL_TAG_ITEM(TypedReference, "trf "),
    DECL_TAG_ITEM(Value, "val"),

    DECL_TAG_ITEM(Offset, "off "),
    DECL_TAG_ITEM(OffsetPlusHeapbase, "ohb "),

    DECL_TAG_ITEM(SystemObject, "obj "),
    DECL_TAG_ITEM(SystemClass,  "cls "),
    DECL_TAG_ITEM(SystemString, "str "),
    DECL_TAG_ITEM(NullObject, "nul "),
    DECL_TAG_ITEM(Array, "[]  "),
    DECL_TAG_ITEM(Object, "o   "),
    DECL_TAG_ITEM(BoxedValue, "bval"),

    DECL_TAG_ITEM(UnmanagedPtr, "*   "),
    DECL_TAG_ITEM(ManagedPtr, "&   "),
    DECL_TAG_ITEM(MethodPtr, "fun "),
    DECL_TAG_ITEM(VTablePtr, "vtb "),

    DECL_TAG_ITEM(CompressedSystemObject, "cob "),
    DECL_TAG_ITEM(CompressedSystemClass,  "ccl "),
    DECL_TAG_ITEM(CompressedSystemString, "cst "),
    DECL_TAG_ITEM(CompressedNullObject, "cnl "),
    DECL_TAG_ITEM(CompressedUnresolvedObject, "cun "),
    DECL_TAG_ITEM(CompressedArray, "c[] "),
    DECL_TAG_ITEM(CompressedObject, "co  "),

    DECL_TAG_ITEM(CompressedMethodPtr, "cfun"),
    DECL_TAG_ITEM(CompressedVTablePtr, "cvtb"),
    DECL_TAG_ITEM(OrNull, "ornl"),

    DECL_TAG_ITEM(VTablePtrObj, "vtb "),
    DECL_TAG_ITEM(ITablePtrObj, "itb "),
    DECL_TAG_ITEM(ArrayLength, "len "),
    DECL_TAG_ITEM(ArrayElementType, "elem"),
    DECL_TAG_ITEM(Singleton, "ston"),
    DECL_TAG_ITEM(NumTypeTags, "XXXX"),
};

static const U_32 type_tag_names_count = sizeof(type_tag_names)/sizeof(type_tag_names[0]);

#ifdef _DEBUG
static inline void checkArray() {
    static bool doArrayCheck = true;
    if( !doArrayCheck ) return;
    doArrayCheck = false;
    for( U_32 i=0; i<type_tag_names_count; i++ ) {
        assert( (U_32)(type_tag_names[i].tag) == i );
    }
}
#else
    #define checkArray()
#endif

Type::Tag Type::str2tag(const char * tagname) {
    checkArray();

    for( U_32 i=0; i<type_tag_names_count; i++ ) {
        if( 0 == strcmpi(type_tag_names[i].name, tagname) ) {
            return (Tag)i; // the map is ordered, thus '[i].tag == tag'
        }
    }
    return InavlidTag;
}

const char * Type::tag2str(Tag t) {
    checkArray();

    assert( t >= 0 && t < NumTypeTags );
    return type_tag_names[t].name;
}


} //namespace Jitrino 
