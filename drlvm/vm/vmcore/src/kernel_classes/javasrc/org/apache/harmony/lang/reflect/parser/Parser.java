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

package org.apache.harmony.lang.reflect.parser;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.apache.harmony.lang.reflect.implementation.ParameterizedTypeImpl;
import org.apache.harmony.lang.reflect.implementation.TypeVariableImpl;
import org.apache.harmony.lang.reflect.repository.ParameterizedTypeRepository;
import org.apache.harmony.lang.reflect.repository.TypeVariableRepository;
import org.apache.harmony.lang.reflect.support.AuxiliaryChecker;
import org.apache.harmony.lang.reflect.support.AuxiliaryCreator;
import org.apache.harmony.lang.reflect.support.AuxiliaryFinder;
import org.apache.harmony.lang.reflect.support.AuxiliaryLoader;
import org.apache.harmony.lang.reflect.support.AuxiliaryUtil;

/**
 * @author Serguei S. Zapreyev
 */
public class Parser {

    public static enum SignatureKind {
        FIELD_SIGNATURE(2),
        METHOD_SIGNATURE(3),
        CONSTRUCTOR_SIGNATURE(4),
        CLASS_SIGNATURE(1);
        SignatureKind(int value) { this.value = value; }
        private final int value;
        public int value() { return value; }
    }
    
    public static InterimGenericDeclaration parseSignature(String signature, SignatureKind kind, java.lang.reflect.GenericDeclaration startPoint) throws GenericSignatureFormatError {
        return SignatureParser.parseSignature(signature, kind.value());
    }
    
    //TODO: generic warning
    /**
     * ################################################################################
     * for j.l.r.Constructor
     * ################################################################################
     */
    //TODO: synchronization on constructor?    
    /**
     * initializes generalized exeptions
     */
    public static Type[] getGenericExceptionTypes(Constructor constructor, String signature) {
        Type[] genericExceptionTypes = null;
        //So, here it can be ParameterizedType or TypeVariable or ordinary reference class type elements.
            Object startPoint = constructor; 
            //FIXME: Performance enhancement
            String constrSignature = AuxiliaryUtil.toUTF8(signature); // getting this method signature
            if (constrSignature == null) {
                //FIXME: Performance enhancement                    
                return constructor.getExceptionTypes();
            }
                // constrSignature&constrGenDecl is also the "hard" way to rethrow GenericSignatureFormatError each time for a while
            InterimConstructorGenericDecl constrGenDecl = (InterimConstructorGenericDecl) Parser.parseSignature(constrSignature, SignatureKind.CONSTRUCTOR_SIGNATURE, (GenericDeclaration)startPoint); // GenericSignatureFormatError can be thrown here
            InterimType[] throwns = constrGenDecl.throwns;
            if (throwns == null) {
                //FIXME: Performance enhancement                    
                return constructor.getExceptionTypes();
            }
            int l = throwns.length;
            genericExceptionTypes = new Type[l];
            for (int i = 0; i < l; i++) {
                if (throwns[i] instanceof InterimParameterizedType) {
                    ParameterizedType pType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) throwns[i], ((InterimParameterizedType) throwns[i]).signature, startPoint);
                    if (pType == null) {
                        try {
                            AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) throwns[i], startPoint);
                        } catch(Throwable e) {
                            throw new TypeNotPresentException(((InterimParameterizedType) throwns[i]).rawType.classTypeName.substring(1).replace('/', '.'), e);
                        }
                        //check the correspondence of the formal parameter number and the actual argument number:
                        AuxiliaryChecker.checkArgsNumber((InterimParameterizedType) throwns[i], startPoint); // the MalformedParameterizedTypeException may raise here
                        try {
                            pType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) throwns[i], startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) throwns[i], startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) throwns[i], startPoint));
                        } catch(ClassNotFoundException e) {
                            throw new TypeNotPresentException(e.getMessage(), e);
                        }
                        ParameterizedTypeRepository.registerParameterizedType(pType, (InterimParameterizedType) throwns[i], ((InterimParameterizedType) throwns[i]).signature, startPoint);
                    }
                    genericExceptionTypes[i] = (Type) pType; 
                } else if (throwns[i] instanceof InterimClassType) {
                    try {
                        genericExceptionTypes[i] = (Type) AuxiliaryLoader.findClass(((InterimClassType)throwns[i]).classTypeName.substring(1).replace('/', '.'), startPoint); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(((InterimClassType)throwns[i]).classTypeName.substring(1).replace('/', '.'), e);
                    } catch (ExceptionInInitializerError e) {
                    } catch (LinkageError e) {
                    }
                } else if (throwns[i] instanceof InterimTypeVariable) {
                    String tvName = ((InterimTypeVariable) throwns[i]).typeVariableName;
                    TypeVariable variable = TypeVariableRepository.findTypeVariable(tvName, startPoint);

                    if (variable == null) {
                        variable =  AuxiliaryFinder.findTypeVariable(tvName, startPoint);
                        if (variable == null) {
                            genericExceptionTypes[i] = (Type) null;
                            break;
                        }
                    }
                    genericExceptionTypes[i] = (Type) variable;
                } else {
                    // Internal Error
                }
            }
            return genericExceptionTypes;
    }
    
    /**
     * initializes type parameters
     */
    @SuppressWarnings("unchecked")
    public static TypeVariable<? extends Constructor>[] getTypeParameters(Constructor constructor, String signature) {
        //So, here it can be only TypeVariable elements.
        
        TypeVariable<Constructor>[] typeParameters = null;
        Object startPoint = constructor;

        //FIXME: performance enhancement
        String constrSignature = AuxiliaryUtil.toUTF8(signature); // getting this method signature

        if (constrSignature == null) {
            return new TypeVariable[0];  // can't use <generic> for arrays...
        }

        //FIXME: performance enhancement
        InterimConstructorGenericDecl constrGenDecl = (InterimConstructorGenericDecl) Parser
                    .parseSignature(constrSignature,
                            SignatureKind.CONSTRUCTOR_SIGNATURE,
                            (GenericDeclaration) startPoint); // GenericSignatureFormatError
                                                                // can be
                                                                // thrown
                                                                // here
        InterimTypeParameter[] pTypeParameters = constrGenDecl.typeParameters;

        if (pTypeParameters == null) {
            return new TypeVariable[0]; // can't use <generic> for arrays...
        }
        int l = pTypeParameters.length;
        typeParameters = new TypeVariable[l];  // can't use <generic> for arrays...

        for (int i = 0; i < l; i++) {
            String tvName = pTypeParameters[i].typeParameterName;
            TypeVariable variable = new TypeVariableImpl(
                    (GenericDeclaration) constructor, tvName,
                    pTypeParameters[i]);
            TypeVariableRepository.registerTypeVariable(variable, tvName,
                    startPoint);
            typeParameters[i] = variable;
        }
        return typeParameters;
    }
    
    /**
     * initializes generalized parameters
     */
    public static synchronized Type[] getGenericParameterTypes(Constructor constructor, String signature) {
        //So, here it can be ParameterizedType or TypeVariable or ordinary reference class type elements.
        Type[] genericParameterTypes = null;
        Object startPoint = constructor;
        //FIXME: performance enhancement
        String constrSignature = AuxiliaryUtil.toUTF8(signature); // getting this method
        if (constrSignature == null) {
            //FIXME: performance enhancement
            return constructor.getParameterTypes();
        }
        
        // GenericSignatureFormatError can be thrown here
        //FIXME: performance enhancement
        InterimConstructorGenericDecl constrGenDecl = (InterimConstructorGenericDecl) Parser
                    .parseSignature(constrSignature,
                            SignatureKind.CONSTRUCTOR_SIGNATURE,
                            (GenericDeclaration) startPoint); 

        InterimType[] methodParameters = constrGenDecl.methodParameters;
        if (methodParameters == null) {
            return new Type[0];
        }
        int l = methodParameters.length;
        genericParameterTypes = new Type[l];
        for (int i = 0; i < l; i++) {
            if (methodParameters[i] instanceof InterimParameterizedType) {
                ParameterizedType pType = ParameterizedTypeRepository
                        .findParameterizedType(
                                (InterimParameterizedType) methodParameters[i],
                                ((InterimParameterizedType) methodParameters[i]).signature,
                                startPoint);
                if (pType == null) {
                    try {
                        AuxiliaryFinder
                                .findGenericClassDeclarationForParameterizedType(
                                        (InterimParameterizedType) methodParameters[i],
                                        startPoint);
                    } catch (Throwable e) {
                        throw new TypeNotPresentException(
                                ((InterimParameterizedType) methodParameters[i]).rawType.classTypeName
                                        .substring(1).replace('/', '.'), e);
                    }
                    // check the correspondence of the formal parameter
                    // number and the actual argument number:
                    AuxiliaryChecker.checkArgsNumber(
                            (InterimParameterizedType) methodParameters[i],
                            startPoint); // the
                                            // MalformedParameterizedTypeException
                                            // may raise here
                    try {
                        pType = new ParameterizedTypeImpl(
                                AuxiliaryCreator
                                        .createTypeArgs(
                                                (InterimParameterizedType) methodParameters[i],
                                                startPoint),
                                AuxiliaryCreator
                                        .createRawType(
                                                (InterimParameterizedType) methodParameters[i],
                                                startPoint),
                                AuxiliaryCreator
                                        .createOwnerType(
                                                (InterimParameterizedType) methodParameters[i],
                                                startPoint));
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(e.getMessage(), e);
                    }
                    ParameterizedTypeRepository
                            .registerParameterizedType(
                                    pType,
                                    (InterimParameterizedType) methodParameters[i],
                                    ((InterimParameterizedType) methodParameters[i]).signature,
                                    startPoint);
                }
                genericParameterTypes[i] = (Type) pType;
            } else if (methodParameters[i] instanceof InterimClassType) {
                try {
                    genericParameterTypes[i] = (Type) AuxiliaryLoader
                            .findClass(((InterimClassType) methodParameters[i]).classTypeName
                                    .substring(
                                            (((InterimClassType) methodParameters[i]).classTypeName
                                                    .charAt(0) == 'L' ? 1
                                                    : 0)).replace('/', '.'), startPoint); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(
                            ((InterimClassType) methodParameters[i]).classTypeName
                                    .substring(
                                            (((InterimClassType) methodParameters[i]).classTypeName
                                                    .charAt(0) == 'L' ? 1
                                                    : 0)).replace('/', '.'),
                            e);
                } catch (ExceptionInInitializerError e) {
                } catch (LinkageError e) {
                }
            } else if (methodParameters[i] instanceof InterimTypeVariable) {
                String tvName = ((InterimTypeVariable) methodParameters[i]).typeVariableName;
                TypeVariable variable = TypeVariableRepository
                        .findTypeVariable(tvName, startPoint);
                if (variable == null) {
                    variable = AuxiliaryFinder.findTypeVariable(tvName,
                            startPoint);
                    if (variable == null) {
                        genericParameterTypes[i] = (Type) null;
                        continue;
                    }
                }
                genericParameterTypes[i] = (Type) variable;
            } else if (methodParameters[i] instanceof InterimGenericArrayType) {
                genericParameterTypes[i] = AuxiliaryCreator
                        .createGenericArrayType(
                                (InterimGenericArrayType) methodParameters[i],
                                startPoint);
            } else {
                // Internal Error
            }
        }
        return genericParameterTypes;
    }
    

    /**
     * ################################################################################
     * for j.l.r.Field
     * ################################################################################
     */
    public static Type parseFieldGenericType(Field field, String rawSignature) throws GenericSignatureFormatError {
        Object startPoint = field.getDeclaringClass();
        String signature = AuxiliaryUtil.toUTF8(rawSignature);
        if (signature == null) {
            return field.getType();
        }
        InterimFieldGenericDecl decl = (InterimFieldGenericDecl) Parser
                .parseSignature(signature, SignatureKind.FIELD_SIGNATURE,
                        (GenericDeclaration) startPoint);
        InterimGenericType fldType = decl.fieldType;
        if (fldType instanceof InterimTypeVariable) {
            String tvName = ((InterimTypeVariable) fldType).typeVariableName;
            TypeVariable variable = TypeVariableRepository
                    .findTypeVariable(tvName, startPoint);
            if (variable == null) {
                variable = AuxiliaryFinder.findTypeVariable(tvName,
                        startPoint);
                if (variable == null) {
                    return (Type) null;
                }
            }
            return (Type) variable;
        } else if (fldType instanceof InterimParameterizedType) {
            ParameterizedType pType = ParameterizedTypeRepository
                    .findParameterizedType(
                            (InterimParameterizedType) fldType,
                            ((InterimParameterizedType) fldType).signature,
                            startPoint);
            if (pType == null) {
                try {
                    AuxiliaryFinder
                            .findGenericClassDeclarationForParameterizedType(
                                    (InterimParameterizedType) fldType,
                                    startPoint);
                } catch (Throwable e) {
                    throw new TypeNotPresentException(
                            ((InterimParameterizedType) fldType).rawType.classTypeName
                                    .substring(1).replace('/', '.'), e);
                }
                // check the correspondence of the formal parameter number
                // and the actual argument number:
                AuxiliaryChecker.checkArgsNumber(
                        (InterimParameterizedType) fldType, startPoint); // the
                                                                            // MalformedParameterizedTypeException
                                                                            // may
                                                                            // raise
                                                                            // here
                try {
                    pType = new ParameterizedTypeImpl(AuxiliaryCreator
                            .createTypeArgs(
                                    (InterimParameterizedType) fldType,
                                    startPoint), AuxiliaryCreator
                            .createRawType(
                                    (InterimParameterizedType) fldType,
                                    startPoint), AuxiliaryCreator
                            .createOwnerType(
                                    (InterimParameterizedType) fldType,
                                    startPoint));
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(e.getMessage(), e);
                }
                ParameterizedTypeRepository.registerParameterizedType(
                        pType, (InterimParameterizedType) fldType,
                        ((InterimParameterizedType) fldType).signature,
                        startPoint);
            }
            return pType;
        } else if (fldType instanceof InterimGenericArrayType) {
            return AuxiliaryCreator.createGenericArrayType(
                    (InterimGenericArrayType) fldType, startPoint);
        } else {
            return field.getType();
        }
    }
    
    
    /**
     * ################################################################################
     * for j.l.r.Method
     * ################################################################################
     */
    
    /**
     * initializes type parameters
     */
    @SuppressWarnings("unchecked")
    public static TypeVariable[] getTypeParameters(Method method, String signature) {
        // So, here it can be only TypeVariable elements.
        TypeVariable[] typeParameters;
        Object startPoint = method;
        // FIXME: performance enhancement
        String methSignature = AuxiliaryUtil.toUTF8(signature); // getting this
                                                                // method
        // FIXME: performance enhancement                       // signature
        if (methSignature == null) {
            return new TypeVariable[0];
        }
        // FIXME: performance enhancement
        InterimMethodGenericDecl methGenDecl = (InterimMethodGenericDecl) Parser
                .parseSignature(methSignature, SignatureKind.METHOD_SIGNATURE,
                        (GenericDeclaration) startPoint); // GenericSignatureFormatError
                                                            // can be thrown
                                                            // here
        InterimTypeParameter[] pTypeParameters = methGenDecl.typeParameters;
        if (pTypeParameters == null) {
            return new TypeVariable[0];
        }
        int l = pTypeParameters.length;
        typeParameters = new TypeVariable[l];
        for (int i = 0; i < l; i++) {
            String tvName = pTypeParameters[i].typeParameterName;
            TypeVariable variable = new TypeVariableImpl(
                    (GenericDeclaration) method, tvName,
                    methGenDecl.typeParameters[i]);
            TypeVariableRepository.registerTypeVariable(variable, tvName,
                    startPoint);
            typeParameters[i] = variable;
        }
        return typeParameters;
    }      
    
    public static Type getGenericReturnTypeImpl(Method method, String signature) throws GenericSignatureFormatError {
        Object startPoint = method;
        // FIXME: performance enhancement
        String methSignature;
        methSignature = AuxiliaryUtil.toUTF8(signature);
        if (methSignature == null) {
            // FIXME: performance enhancement
            return (Type) method.getReturnType();
        }
        // FIXME: performance enhancement
        InterimMethodGenericDecl methGenDecl = (InterimMethodGenericDecl) Parser
                .parseSignature(methSignature, SignatureKind.METHOD_SIGNATURE,
                        (GenericDeclaration) startPoint);
        InterimType mthdType = methGenDecl.returnValue;
        if (mthdType instanceof InterimTypeVariable) {
            String tvName = ((InterimTypeVariable) mthdType).typeVariableName;
            TypeVariable variable = TypeVariableRepository.findTypeVariable(
                    tvName, startPoint);
            if (variable == null) {
                variable = AuxiliaryFinder.findTypeVariable(tvName, startPoint);
                if (variable == null) {
                    return (Type) null; // compatible behaviour
                }
            }
            return (Type) variable;
        } else if (mthdType instanceof InterimParameterizedType) {
            ParameterizedType pType = ParameterizedTypeRepository
                    .findParameterizedType((InterimParameterizedType) mthdType,
                            ((InterimParameterizedType) mthdType).signature,
                            startPoint);
            if (pType == null) {
                try {
                    AuxiliaryFinder
                            .findGenericClassDeclarationForParameterizedType(
                                    (InterimParameterizedType) mthdType,
                                    startPoint);
                } catch (Throwable e) {
                    throw new TypeNotPresentException(
                            ((InterimParameterizedType) mthdType).rawType.classTypeName
                                    .substring(1).replace('/', '.'), e);
                }
                // check the correspondence of the formal parameter number and
                // the actual argument number:
                AuxiliaryChecker.checkArgsNumber(
                        (InterimParameterizedType) mthdType, startPoint); // the
                                                                            // MalformedParameterizedTypeException
                                                                            // may
                                                                            // raise
                                                                            // here
                try {
                    pType = new ParameterizedTypeImpl(AuxiliaryCreator
                            .createTypeArgs(
                                    (InterimParameterizedType) mthdType,
                                    startPoint), AuxiliaryCreator
                            .createRawType((InterimParameterizedType) mthdType,
                                    startPoint), AuxiliaryCreator
                            .createOwnerType(
                                    (InterimParameterizedType) mthdType,
                                    startPoint));
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(e.getMessage(), e);
                }
                ParameterizedTypeRepository.registerParameterizedType(pType,
                        (InterimParameterizedType) mthdType,
                        ((InterimParameterizedType) mthdType).signature,
                        startPoint);
            }
            return (Type) pType;
        } else if (mthdType instanceof InterimGenericArrayType) {
            return AuxiliaryCreator.createGenericArrayType(
                    (InterimGenericArrayType) mthdType, startPoint);
        } else {
            return method.getReturnType();
        }
    }
    

    /**
     * initializes generalized exeptions
     */
    public static Type[] getGenericExceptionTypes(Method method, String signature) {
        // So, here it can be ParameterizedType or TypeVariable or ordinary
        // reference class type elements.
        Type[] genericExceptionTypes = null;
        Object startPoint = method;
        // FIXME: performance enhancement
        String methSignature = AuxiliaryUtil.toUTF8(signature); // getting
                                                                // this
                                                                // method
        // FIXME: performance enhancement                       // signature
        if (methSignature == null) {
            return method.getExceptionTypes();
        }
        // FIXME: performance enhancement
        InterimMethodGenericDecl methGenDecl = (InterimMethodGenericDecl) Parser.parseSignature(
                methSignature, SignatureKind.METHOD_SIGNATURE,
                (GenericDeclaration) startPoint); // GenericSignatureFormatError
                                                    // can be thrown here
        InterimType[] throwns = methGenDecl.throwns;
        if (throwns == null) {
            return method.getExceptionTypes();
        }
        int l = throwns.length;
        genericExceptionTypes = new Type[l];
        for (int i = 0; i < l; i++) {
            if (throwns[i] instanceof InterimParameterizedType) {
                ParameterizedType pType = ParameterizedTypeRepository
                        .findParameterizedType(
                                (InterimParameterizedType) throwns[i],
                                ((InterimParameterizedType) throwns[i]).signature,
                                startPoint);
                if (pType == null) {
                    try {
                        AuxiliaryFinder
                                .findGenericClassDeclarationForParameterizedType(
                                        (InterimParameterizedType) throwns[i],
                                        startPoint);
                    } catch (Throwable e) {
                        throw new TypeNotPresentException(
                                ((InterimParameterizedType) throwns[i]).rawType.classTypeName
                                        .substring(1).replace('/', '.'), e);
                    }
                    // check the correspondence of the formal parameter
                    // number and the actual argument number:
                    AuxiliaryChecker.checkArgsNumber(
                            (InterimParameterizedType) throwns[i],
                            startPoint); // the
                                            // MalformedParameterizedTypeException
                                            // may raise here
                    try {
                        pType = new ParameterizedTypeImpl(
                                AuxiliaryCreator
                                        .createTypeArgs(
                                                (InterimParameterizedType) throwns[i],
                                                startPoint),
                                AuxiliaryCreator
                                        .createRawType(
                                                (InterimParameterizedType) throwns[i],
                                                startPoint),
                                AuxiliaryCreator
                                        .createOwnerType(
                                                (InterimParameterizedType) throwns[i],
                                                startPoint));
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(e.getMessage(), e);
                    }
                    ParameterizedTypeRepository
                            .registerParameterizedType(
                                    pType,
                                    (InterimParameterizedType) throwns[i],
                                    ((InterimParameterizedType) throwns[i]).signature,
                                    startPoint);
                }
                genericExceptionTypes[i] = (Type) pType;
            } else if (throwns[i] instanceof InterimClassType) {
                try {
                    genericExceptionTypes[i] = (Type) AuxiliaryLoader
                            .findClass(((InterimClassType) throwns[i]).classTypeName
                                    .substring(
                                            (((InterimClassType) throwns[i]).classTypeName
                                                    .charAt(0) == 'L' ? 1
                                                    : 0)).replace('/', '.'), startPoint); // XXX:
                                                                                // should
                                                                                // we
                                                                                // propagate
                                                                                // the
                                                                                // class
                                                                                // loader
                                                                                // of
                                                                                // initial
                                                                                // user's
                                                                                // request
                                                                                // (Field.getGenericType())
                                                                                // or
                                                                                // use
                                                                                // this
                                                                                // one?
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(
                            ((InterimClassType) throwns[i]).classTypeName
                                    .substring(
                                            (((InterimClassType) throwns[i]).classTypeName
                                                    .charAt(0) == 'L' ? 1
                                                    : 0)).replace('/', '.'),
                            e);
                } catch (ExceptionInInitializerError e) {
                } catch (LinkageError e) {
                }
            } else if (throwns[i] instanceof InterimTypeVariable) {
                String tvName = ((InterimTypeVariable) throwns[i]).typeVariableName;
                TypeVariable variable = TypeVariableRepository
                        .findTypeVariable(tvName, startPoint);
                if (variable == null) {
                    variable = AuxiliaryFinder.findTypeVariable(tvName,
                            startPoint);
                    if (variable == null) {
                        genericExceptionTypes[i] = (Type) null;
                        break;
                    }
                }
                genericExceptionTypes[i] = (Type) variable;
            } else {
                // Internal Error
            }

        }
        return genericExceptionTypes;
    }
    
    /**
     * initializes generalized parameters
     */
    public static Type[] getGenericParameterTypes(Method method,
            String signature) {
        // So, here it can be ParameterizedType or TypeVariable or ordinary
        // reference class type elements.
        Type[] genericParameterTypes = null;
        Object startPoint = method;
        String methSignature = AuxiliaryUtil.toUTF8(signature); // getting this
                                                                // method
                                                                // signature
        if (methSignature == null) {
            return method.getParameterTypes();
        }
        InterimMethodGenericDecl methGenDecl = (InterimMethodGenericDecl) Parser
                .parseSignature(methSignature, SignatureKind.METHOD_SIGNATURE,
                        (GenericDeclaration) startPoint); // GenericSignatureFormatError
                                                            // can be thrown
                                                            // here
        InterimType[] methodParameters = methGenDecl.methodParameters;
        if (methodParameters == null) {
            return new Type[0];
        }
        int l = methodParameters.length;
        genericParameterTypes = new Type[l];
        for (int i = 0; i < l; i++) {
            if (methodParameters[i] instanceof InterimParameterizedType) {
                ParameterizedType pType = ParameterizedTypeRepository
                        .findParameterizedType(
                                (InterimParameterizedType) methodParameters[i],
                                ((InterimParameterizedType) methodParameters[i]).signature,
                                startPoint);
                if (pType == null) {
                    try {
                        AuxiliaryFinder
                                .findGenericClassDeclarationForParameterizedType(
                                        (InterimParameterizedType) methodParameters[i],
                                        startPoint);
                    } catch (Throwable e) {
                        throw new TypeNotPresentException(
                                ((InterimParameterizedType) methodParameters[i]).rawType.classTypeName
                                        .substring(1).replace('/', '.'), e);
                    }
                    // check the correspondence of the formal parameter number
                    // and the actual argument number:
                    AuxiliaryChecker.checkArgsNumber(
                            (InterimParameterizedType) methodParameters[i],
                            startPoint); // the
                                            // MalformedParameterizedTypeException
                                            // may raise here
                    try {
                        pType = new ParameterizedTypeImpl(
                                AuxiliaryCreator
                                        .createTypeArgs(
                                                (InterimParameterizedType) methodParameters[i],
                                                startPoint),
                                AuxiliaryCreator
                                        .createRawType(
                                                (InterimParameterizedType) methodParameters[i],
                                                startPoint),
                                AuxiliaryCreator
                                        .createOwnerType(
                                                (InterimParameterizedType) methodParameters[i],
                                                startPoint));
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(e.getMessage(), e);
                    }
                    ParameterizedTypeRepository
                            .registerParameterizedType(
                                    pType,
                                    (InterimParameterizedType) methodParameters[i],
                                    ((InterimParameterizedType) methodParameters[i]).signature,
                                    startPoint);
                }
                genericParameterTypes[i] = (Type) pType;
            } else if (methodParameters[i] instanceof InterimClassType) {
                try {
                    genericParameterTypes[i] = (Type) AuxiliaryLoader
                            .findClass(((InterimClassType) methodParameters[i]).classTypeName
                                    .substring(
                                            (((InterimClassType) methodParameters[i]).classTypeName
                                                    .charAt(0) == 'L' ? 1 : 0))
                                    .replace('/', '.'), startPoint); // XXX: should we
                                                            // propagate the
                                                            // class loader of
                                                            // initial user's
                                                            // request
                                                            // (Field.getGenericType())
                                                            // or use this one?
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(
                            ((InterimClassType) methodParameters[i]).classTypeName
                                    .substring(
                                            (((InterimClassType) methodParameters[i]).classTypeName
                                                    .charAt(0) == 'L' ? 1 : 0))
                                    .replace('/', '.'), e);
                } catch (ExceptionInInitializerError e) {
                } catch (LinkageError e) {
                }
            } else if (methodParameters[i] instanceof InterimTypeVariable) {
                String tvName = ((InterimTypeVariable) methodParameters[i]).typeVariableName;
                TypeVariable variable = TypeVariableRepository
                        .findTypeVariable(tvName, startPoint);
                if (variable == null) {
                    variable = AuxiliaryFinder.findTypeVariable(tvName,
                            startPoint);
                    if (variable == null) {
                        genericParameterTypes[i] = (Type) null;
                        continue;
                    }
                }
                genericParameterTypes[i] = (Type) variable;
            } else if (methodParameters[i] instanceof InterimGenericArrayType) {
                genericParameterTypes[i] = AuxiliaryCreator
                        .createGenericArrayType(
                                (InterimGenericArrayType) methodParameters[i],
                                startPoint);
            } else {
                // Internal Error
            }
        }
        return genericParameterTypes;
    }    
    
    
    /**
     * ################################################################################
     * for j.l.Class
     * ################################################################################
     */
    @SuppressWarnings("unchecked")
    public static TypeVariable[] getTypeParameters(Class c, String rawSignature) {
        TypeVariable[] typeParameters = null;
        //So, here it can be only TypeVariable elements.
            Object startPoint = c;
            String signature = AuxiliaryUtil.toUTF8(rawSignature); // getting this class signature
            if (signature == null) {
                return typeParameters =  new TypeVariable[0];
            }
            InterimClassGenericDecl decl = (InterimClassGenericDecl) Parser.parseSignature(signature, SignatureKind.CLASS_SIGNATURE, (GenericDeclaration)startPoint); // GenericSignatureFormatError can be thrown here
            InterimTypeParameter[] pTypeParameters = decl.typeParameters;
            if (pTypeParameters == null) {
                return typeParameters =  new TypeVariable[0];
            }
            int l = pTypeParameters.length;
            typeParameters = new TypeVariable[l];
            for (int i = 0; i < l; i++) {
                String tvName = pTypeParameters[i].typeParameterName;
                TypeVariable variable = new TypeVariableImpl((GenericDeclaration)c, tvName, decl.typeParameters[i]);
                TypeVariableRepository.registerTypeVariable(variable, tvName, startPoint);
                typeParameters[i] = variable;               
            }
        return typeParameters;
    } 
    
    public static Type getGenericSuperClass(Class c, String rawSignature) {
        Type genericSuperclass = null;
        Object startPoint = (Object) c; // It should be this class itself
                                        // because, for example, superclass may
                                        // be a parameterized type with
                                        // parameters which are the generic
                                        // parameters of this class
        String signature = AuxiliaryUtil.toUTF8(rawSignature); // getting this class signature
        if (signature == null) {
            return genericSuperclass = c.getSuperclass();
        }
        InterimClassGenericDecl decl = (InterimClassGenericDecl) Parser
                .parseSignature(signature, SignatureKind.CLASS_SIGNATURE,
                        (GenericDeclaration) startPoint); // GenericSignatureFormatError
                                                            // can be thrown
                                                            // here
        InterimType superClassType = decl.superClass;
        if (superClassType == null) {
            return genericSuperclass = c.getSuperclass();
        }
        if (superClassType instanceof InterimParameterizedType) {
            ParameterizedType pType = ParameterizedTypeRepository
                    .findParameterizedType(
                            (InterimParameterizedType) superClassType,
                            ((InterimParameterizedType) superClassType).signature,
                            startPoint);
            if (pType == null) {
                try {
                    AuxiliaryFinder
                            .findGenericClassDeclarationForParameterizedType(
                                    (InterimParameterizedType) superClassType,
                                    startPoint);
                } catch (Throwable e) {
                    throw new TypeNotPresentException(
                            ((InterimParameterizedType) superClassType).rawType.classTypeName
                                    .substring(1).replace('/', '.'), e);
                }
                // check the correspondence of the formal parameter number and
                // the actual argument number:
                AuxiliaryChecker.checkArgsNumber(
                        (InterimParameterizedType) superClassType, startPoint); // the
                                                                                // MalformedParameterizedTypeException
                                                                                // may
                                                                                // raise
                                                                                // here
                try {
                    pType = new ParameterizedTypeImpl(AuxiliaryCreator
                            .createTypeArgs(
                                    (InterimParameterizedType) superClassType,
                                    startPoint), AuxiliaryCreator
                            .createRawType(
                                    (InterimParameterizedType) superClassType,
                                    startPoint), AuxiliaryCreator
                            .createOwnerType(
                                    (InterimParameterizedType) superClassType,
                                    startPoint));
                } catch (ClassNotFoundException e) {
                    throw new TypeNotPresentException(e.getMessage(), e);
                }
                ParameterizedTypeRepository.registerParameterizedType(pType,
                        (InterimParameterizedType) superClassType, signature,
                        startPoint);
            }
            genericSuperclass = (Type) pType;
        } else if (superClassType instanceof InterimClassType) {
            try {
                genericSuperclass = (Type) c
                        .getClass()
                        .getClassLoader()
                        //FIXME: any potential issue to change findClass->loadClass
                        .loadClass(
                                AuxiliaryFinder
                                        .transform(((InterimClassType) superClassType).classTypeName
                                                .substring(1).replace('/', '.'))); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
            } catch (ClassNotFoundException e) {
                throw new TypeNotPresentException(
                        ((InterimClassType) superClassType).classTypeName
                                .substring(1).replace('/', '.'), e);
            } catch (ExceptionInInitializerError e) {
            } catch (LinkageError e) {
            }
        } else {
            // Internal Error
        }
        return genericSuperclass;
    }
    
    @SuppressWarnings("unchecked")
    public static Type[] getGenericInterfaces(Class c, String rawSignature){
        
        Type[] genericInterfaces = null;
        
        //So, here it can be only ParameterizedType or ordinary reference class type elements.
        if (c.isArray()) {
            return genericInterfaces = new Type[]{Cloneable.class, Serializable.class};
        }
        if (genericInterfaces == null) {
            Object startPoint = c;  // It should be this class itself because, for example, an interface may be a parameterized type with parameters which are the generic parameters of this class
            String signature = AuxiliaryUtil.toUTF8(rawSignature); // getting this class signature
            if (signature == null) {
                return genericInterfaces = c.getInterfaces();
            }
            InterimClassGenericDecl decl = (InterimClassGenericDecl) Parser.parseSignature(signature, SignatureKind.CLASS_SIGNATURE, (GenericDeclaration)startPoint); //GenericSignatureFormatError can be thrown here
            InterimType[] superInterfaces = decl.superInterfaces;
            if (superInterfaces == null) {
                return genericInterfaces =  c.getInterfaces();
            }
            int l = superInterfaces.length;
            genericInterfaces = new Type[l];
            for (int i = 0; i < l; i++) { 
                if (superInterfaces[i] instanceof InterimParameterizedType) {
                    ParameterizedType pType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) superInterfaces[i], ((InterimParameterizedType) superInterfaces[i]).signature, startPoint);
                    if (pType == null) {
                        try {
                            AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) superInterfaces[i], startPoint);
                        } catch(Throwable e) {
                            throw new TypeNotPresentException(((InterimParameterizedType) superInterfaces[i]).rawType.classTypeName.substring(1).replace('/', '.'), e);
                        }
                        //check the correspondence of the formal parameter number and the actual argument number:
                        AuxiliaryChecker.checkArgsNumber((InterimParameterizedType) superInterfaces[i], startPoint); // the MalformedParameterizedTypeException may raise here
                        try {
                            pType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) superInterfaces[i], startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) superInterfaces[i], startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) superInterfaces[i], startPoint));
                        } catch(ClassNotFoundException e) {
                            throw new TypeNotPresentException(e.getMessage(), e);
                        }
                        ParameterizedTypeRepository.registerParameterizedType(pType, (InterimParameterizedType) superInterfaces[i], signature, startPoint);
                    }
                    genericInterfaces[i] = (Type) pType; 
                } else if (superInterfaces[i] instanceof InterimClassType) {
                    try {
                        if(c.getClass().getClassLoader() != null){
                            //FIXME: any potential issue to change findClass->loadClass
                            genericInterfaces[i] = (Type) c.getClass().getClassLoader().loadClass(AuxiliaryFinder.transform(((InterimClassType)superInterfaces[i]).classTypeName.substring(1).replace('/', '.'))); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
                        } else {
                            genericInterfaces[i] = (Type) AuxiliaryLoader.findClass(AuxiliaryFinder.transform(((InterimClassType)superInterfaces[i]).classTypeName.substring(1).replace('/', '.')), startPoint); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
                        }
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(((InterimClassType)superInterfaces[i]).classTypeName.substring(1).replace('/', '.'), e);
                    } catch (ExceptionInInitializerError e) {
                    } catch (LinkageError e) {
                    }
                } else {
                    // Internal Error
                }
            }
        }
        return genericInterfaces;
    }
}
