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

package org.apache.harmony.lang.reflect.implementation;

import java.lang.reflect.TypeVariable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.apache.harmony.lang.reflect.support.*;
import org.apache.harmony.lang.reflect.parser.*;
import org.apache.harmony.lang.reflect.repository.*;

/**
 * @author Serguei S. Zapreyev
 */
public final class TypeVariableImpl<D extends GenericDeclaration> implements TypeVariable<D> {
    private Type[] bounds;
    private final D genericDeclaration;
    private final String name;
    private final InterimTypeParameter pTypeParameter;

    /**
     * @param genericDeclaration declaration where a type variable is declared
     * @param name type variable name
     * @param pTypeParameter type variable intermediate representation
     * @api2vm
     */
    public TypeVariableImpl(D genericDeclaration, String name, InterimTypeParameter pTypeParameter) {
        Class klass = null;
        if (genericDeclaration instanceof Class) {
            klass = (Class)genericDeclaration;
        } else {
            klass = (genericDeclaration instanceof Method ? ((Method)genericDeclaration).getDeclaringClass() : ((Constructor)genericDeclaration).getDeclaringClass());
        }        

        while (klass != null) {
            // TODO: it should be revised to provide the correct classloader for resolving.
            AuxiliaryLoader.resolve(klass);
            klass = klass.getDeclaringClass();
        }

        this.genericDeclaration = genericDeclaration;
        this.name = AuxiliaryFinder.transform(name);
        this.bounds = null; // Creating a type variable must not cause the creation of corresponding bounds. So, the creation should be initiated at a moment of first getBounds invocation.
        this.pTypeParameter = pTypeParameter;
    }

    public boolean equals(Object other) {
        // XXX: The bounds comparing seems to be not actual here because
        // 1. equality of variables is defined by belonging to the same declaration and a parameter's names coincedence
        // 2. creating a type variable must not cause the creation of corresponding bounds
        // Therefore we seem not to have to initiate bounds creation here.
        // Nevertheless, we can just compare bound reflections within contents of both pTypeParameter fields (type variable intermediate representations)

        //return other != null && other instanceof TypeVariable && ((TypeVariable)other).name.equals(name) && ((TypeVariable)other).genericDeclaration.equals(genericDeclaration) ? true : false;
        return other != null && other instanceof TypeVariable && ((TypeVariable)other).getName().equals(name) && 
               ( genericDeclaration instanceof Class ?
                       ((Class)genericDeclaration).equals((Class)((TypeVariable)other).getGenericDeclaration()) :
                       ( genericDeclaration instanceof Method ?
                         ((Method)genericDeclaration).equals((Method)((TypeVariable)other).getGenericDeclaration()) :
                           ((Constructor)genericDeclaration).equals((Constructor)((TypeVariable)other).getGenericDeclaration()) ) );
    }

    public Type[] getBounds() {
        // It's time for real bounds creation.
        if (bounds == null) {
            Object startPoint = null;
            if (this.genericDeclaration instanceof Class) {
                startPoint = (Object) this.genericDeclaration;
            } else if (this.genericDeclaration instanceof Method) {
                startPoint = (Object) ((Method)this.genericDeclaration).getDeclaringClass();
            } else if (this.genericDeclaration instanceof Constructor) {
                startPoint = (Object) ((Constructor)this.genericDeclaration).getDeclaringClass();
            }

            int l = pTypeParameter.interfaceBounds.length + 1;
            bounds = new Type[l];
            if (pTypeParameter.classBound == null) {
                bounds[0] = (Type) Object.class;
            } else {
                if (pTypeParameter.classBound instanceof InterimParameterizedType) {
                    java.lang.reflect.ParameterizedType pType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) pTypeParameter.classBound, ((InterimParameterizedType) pTypeParameter.classBound).signature, startPoint);
                    if (pType == null) {
                        try {
                            AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) pTypeParameter.classBound, startPoint);
                        } catch(Throwable e) {
                            throw new TypeNotPresentException(((InterimParameterizedType) pTypeParameter.classBound).rawType.classTypeName.substring(1).replace('/', '.'), e);
                        }
                        // check the correspondence of the formal parameter number and the actual argument number:
                        AuxiliaryChecker.checkArgsNumber((InterimParameterizedType) pTypeParameter.classBound, startPoint); // the MalformedParameterizedTypeException may raise here
                        try {
                            pType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) pTypeParameter.classBound, startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) pTypeParameter.classBound, startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) pTypeParameter.classBound, startPoint));
                        } catch(ClassNotFoundException e) {
                            throw new TypeNotPresentException(e.getMessage(), e);
                        }
                        ParameterizedTypeRepository.registerParameterizedType(pType, (InterimParameterizedType) pTypeParameter.classBound, ((InterimParameterizedType) pTypeParameter.classBound).signature, startPoint);
                    }
                    bounds[0] = (Type) pType;
                } else if (pTypeParameter.classBound instanceof InterimClassType) {
                    try {
                        bounds[0] = (Type)
                            AuxiliaryLoader.findClass(((InterimClassType)pTypeParameter.classBound).classTypeName.substring(1).replace('/', '.'), startPoint);
                    } catch (ClassNotFoundException e) {
                            throw new TypeNotPresentException(((InterimClassType)pTypeParameter.classBound).classTypeName.substring(1).replace('/', '.'), e);
                    } catch (ExceptionInInitializerError e) {
                    } catch (LinkageError e) {
                    }
                }
            }
            for (int i = 1; i < l; i++) {
                if (pTypeParameter.interfaceBounds[i - 1] instanceof InterimParameterizedType) {
                    java.lang.reflect.ParameterizedType pType = ParameterizedTypeRepository.findParameterizedType((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], ((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1]).signature, startPoint);
                    if (pType == null) {
                        try {
                            AuxiliaryFinder.findGenericClassDeclarationForParameterizedType((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], startPoint);
                        } catch(Throwable e) {
                            throw new TypeNotPresentException(((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1]).rawType.classTypeName.substring(1).replace('/', '.'), e);
                        }
                        // check the correspondence of the formal parameter number and the actual argument number:
                        AuxiliaryChecker.checkArgsNumber((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], startPoint); // the MalformedParameterizedTypeException may raise here
                        try {
                            pType = new ParameterizedTypeImpl(AuxiliaryCreator.createTypeArgs((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], startPoint), AuxiliaryCreator.createRawType((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], startPoint), AuxiliaryCreator.createOwnerType((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], startPoint));
                        } catch(ClassNotFoundException e) {
                            throw new TypeNotPresentException(e.getMessage(), e);
                        }
                        ParameterizedTypeRepository.registerParameterizedType(pType, (InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1], ((InterimParameterizedType) pTypeParameter.interfaceBounds[i - 1]).signature, startPoint);
                    }
                    bounds[i] = (Type) pType;
                } else if (pTypeParameter.interfaceBounds[i - 1] instanceof InterimClassType) {
                    try {
                        bounds[i] = (Type) AuxiliaryLoader.findClass(((InterimClassType)pTypeParameter.interfaceBounds[i - 1]).classTypeName.substring(1).replace('/', '.'), startPoint);
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(((InterimClassType)pTypeParameter.interfaceBounds[i - 1]).classTypeName.substring(1).replace('/', '.'), e);
                    } catch (ExceptionInInitializerError e) {
                    } catch (LinkageError e) {
                    }
                }
            }
        }
        return (Type[])bounds.clone();
    }

    public D getGenericDeclaration() {
        return genericDeclaration;
    }
    
    public String getName() {
        return name;
    }

    public int hashCode() {
        //return super.hashCode();
        return getName().hashCode() ^ 
               ( genericDeclaration instanceof Class ?
                       ((Class)genericDeclaration).getName().hashCode() :
                       (genericDeclaration instanceof Method ?
                         ((Method)genericDeclaration).hashCode() :
                           ((Constructor)genericDeclaration).hashCode()));
    }
    
    public String toString() {
        return name;
    }
}
