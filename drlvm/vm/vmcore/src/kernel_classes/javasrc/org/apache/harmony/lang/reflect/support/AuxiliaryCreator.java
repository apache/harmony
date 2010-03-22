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
package org.apache.harmony.lang.reflect.support;

import java.lang.reflect.Type;
import java.lang.TypeNotPresentException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.lang.reflect.TypeVariable;

import org.apache.harmony.lang.reflect.parser.InterimParameterizedType;
import org.apache.harmony.lang.reflect.parser.InterimTypeVariable;
import org.apache.harmony.lang.reflect.parser.InterimGenericArrayType;
import org.apache.harmony.lang.reflect.support.AuxiliaryChecker;
import org.apache.harmony.lang.reflect.support.AuxiliaryFinder;
import org.apache.harmony.lang.reflect.repository.*;

import org.apache.harmony.lang.reflect.parser.*;
import org.apache.harmony.lang.reflect.implementation.*;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * Finder provides kinds of finding.
 */
public final class AuxiliaryCreator {
    
    /**
     * This method creates generic array type.
     * 
     * @param ppType a parsered information produced from a generic array type signature.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return a Type object representing a generic array type.
     */
    public static Type createGenericArrayType(InterimGenericArrayType ppType, Object startPoint) {
        InterimType nextLayer = ppType.nextLayer;
        if (nextLayer instanceof InterimParameterizedType) {
            ParameterizedType pType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) nextLayer, startPoint);
            if (pType == null) {
                try {
                AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) nextLayer, startPoint);
                } catch(Throwable e) {
                    throw new TypeNotPresentException(((InterimParameterizedType) nextLayer).rawType.classTypeName.substring(1).replace('/', '.'), e);
                }
                // check the correspondence of the formal parameter number and the actual argument number:
                AuxiliaryChecker.checkArgsNumber((InterimParameterizedType) nextLayer, startPoint); //the MalformedParameterizedTypeException may raise here
                try {
                    pType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) nextLayer, startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) nextLayer, startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) nextLayer, startPoint));
                } catch(ClassNotFoundException e) {
                    throw new TypeNotPresentException(e.getMessage(), e);
                }
                ParameterizedTypeRepository.registerParameterizedType(pType, (InterimParameterizedType) nextLayer, ((InterimParameterizedType)nextLayer).signature, startPoint);
            }
            return new GenericArrayTypeImpl((Type) pType);
        } else if (nextLayer instanceof InterimTypeVariable) {
            String tvName = ((InterimTypeVariable) nextLayer).typeVariableName;
            TypeVariable variable = TypeVariableRepository.findTypeVariable(tvName, startPoint);
            if (variable == null) {
                variable =  AuxiliaryFinder.findTypeVariable(tvName, startPoint);
                if (variable == null) {
                    return (Type) null; // compatible behaviour
                }
            }
            return new GenericArrayTypeImpl((Type) variable);
        } else if (nextLayer instanceof InterimClassType) {
            Type cType;
            try {
                cType = (Type) AuxiliaryLoader.findClass(((InterimClassType)nextLayer).classTypeName.substring((((InterimClassType)nextLayer).classTypeName.charAt(0)=='L'? 1 : 0)).replace('/', '.'), startPoint);
            } catch(ClassNotFoundException e) {
                throw new TypeNotPresentException(((InterimClassType)nextLayer).classTypeName.substring((((InterimClassType)nextLayer).classTypeName.charAt(0)=='L'? 1 : 0)).replace('/', '.'), e);
            }
            return new GenericArrayTypeImpl(cType);
        } else { // GenericArrayType again
            return new GenericArrayTypeImpl(AuxiliaryCreator.createGenericArrayType((InterimGenericArrayType)nextLayer, startPoint));
        }
    }
    
    /**
     * This method creates the owner's type for a parameterized type.
     * 
     * @param ppType a parsered information produced from a parameterized type signature.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return a created owner's type.
     */
    public static Type createOwnerType(InterimParameterizedType ppType, Object startPoint) throws ClassNotFoundException {
        // raise to owner level:
        InterimType nextppType = ppType.ownerType; // XXX:???Can it be of InterimTypeVariable/InterimClassType type
        if (nextppType == null) {
            return null;
        }
        // create owner type
        if (nextppType instanceof InterimParameterizedType) {
            ParameterizedType pType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) nextppType, startPoint);
            if (pType == null) {
                try {
                    AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) nextppType, startPoint);
                } catch(Throwable e) {
                    throw new TypeNotPresentException(((InterimParameterizedType) nextppType).rawType.classTypeName.substring(1).replace('/', '.'), e);
                }
                // check the correspondence of the formal parameter number and the actual argument number:
                AuxiliaryChecker.checkArgsNumber((InterimParameterizedType)nextppType, startPoint); // the MalformedParameterizedTypeException may raise here
                try {
                    pType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) nextppType, startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) nextppType, startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) nextppType, startPoint));
                } catch(ClassNotFoundException e) {
                    throw new TypeNotPresentException(e.getMessage(), e);
                }
                ParameterizedTypeRepository.registerParameterizedType(pType, (InterimParameterizedType) nextppType, ((InterimParameterizedType)nextppType).signature, startPoint);
            }
            return (Type) pType;
        } else { //ClassType
            return AuxiliaryLoader.findClass(((InterimClassType) nextppType).classTypeName.substring(1).replace('/', '.'), startPoint); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
        }
    }
    
    /**
     * This method creates the raw type for a parameterized type.
     * 
     * @param ppType a parsered information produced from a parameterized type signature.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return a created raw type.
     */
    public static Type createRawType(InterimParameterizedType ppType, Object startPoint) throws ClassNotFoundException {
        return (Type) AuxiliaryFinder.findGenericClassDeclarationForParameterizedType(ppType, startPoint); // it may be null
    }
    
    /**
     * This method creates Type object representing the actual type argument.
     * 
     * @param pType a parsered information of actual parameter.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return a Type object representing the actual type argument.
     */
    public static Type createTypeArg(InterimType pType, Object startPoint) throws ClassNotFoundException {
        Type res;
            if (pType instanceof InterimParameterizedType) {
                ParameterizedType cType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) pType, startPoint);
                if (cType == null) {
                    try {
                        AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) pType, startPoint);
                    } catch(Throwable e) {
                        throw new TypeNotPresentException(((InterimParameterizedType) pType).rawType.classTypeName.substring(1).replace('/', '.'), e);
                    }
                    // check the correspondence of the formal parameter number and the actual argument number:
                    AuxiliaryChecker.checkArgsNumber((InterimParameterizedType) pType, startPoint); // the MalformedParameterizedTypeException may raise here
                    cType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) pType, startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) pType, startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) pType, startPoint));
                    ParameterizedTypeRepository.registerParameterizedType(cType, (InterimParameterizedType) pType, ((InterimParameterizedType)pType).signature, startPoint);
                }
                res = (Type) cType;
            } else if (pType instanceof InterimTypeVariable) {
                String tvName = ((InterimTypeVariable) pType).typeVariableName;
                TypeVariable variable = TypeVariableRepository.findTypeVariable(tvName, startPoint);
                if (variable == null) {
                    variable =  AuxiliaryFinder.findTypeVariable(tvName, startPoint);
                    if (variable == null) {
                        return (Type) null;
                    }
                }
                res = (Type) variable;
            } else if (pType instanceof InterimWildcardType) {
                WildcardType wType = WildcardTypeRepository.findWildcardType((InterimWildcardType) pType, WildcardTypeRepository.recoverWildcardSignature((InterimWildcardType) pType), startPoint);
                if (wType == null) {
                    // The MalformedParameterizedTypeException and TypeNotPresentException should not be raised yet.
                    // These ones can be produced only via WildcardType.getUpperBounds() or WildcardType.getLowerBounds.
                    wType = new WildcardTypeImpl((InterimWildcardType) pType, startPoint);
                    WildcardTypeRepository.registerWildcardType(wType, (InterimWildcardType) pType, WildcardTypeRepository.recoverWildcardSignature((InterimWildcardType) pType), startPoint);
                }
                res = (Type) wType;
            } else if (pType instanceof InterimGenericArrayType) {
                res = AuxiliaryCreator.createGenericArrayType((InterimGenericArrayType)pType, startPoint);
            } else { // ClassType
                String className = ((InterimClassType)
                        pType).classTypeName.substring(1).replace('/', '.');
                res = (Type) AuxiliaryLoader.findClass(className, startPoint); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
            }
        return res;
    }

    /**
     * This method creates an array of Type objects representing the actual type arguments to this type.
     * 
     * @param ppType a parsered information produced from a parameterized type signature.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return an array of Type objects representing the actual type arguments to this type.
     */
    public static Type[] createTypeArgs(InterimParameterizedType ppType, Object startPoint) {
        InterimType args[] = ppType.parameters;
        if (args == null) {
            return new Type[0];
        }
        int len = args.length;
        Type res[] = new Type[len];
        for (int i = 0; i < len; i++) {
            try {
                res[i] = createTypeArg(args[i], startPoint);
            } catch(ClassNotFoundException e) {
                throw new TypeNotPresentException(((InterimClassType)args[i]).classTypeName.substring(1).replace('/', '.'), e); // ClassNotFoundException may appear here only for InterimClassType, see AuxiliaryCreator.createTypeArg.
            }
        }
        return res;
    }
}
