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

import java.lang.reflect.MalformedParameterizedTypeException;

import org.apache.harmony.lang.reflect.parser.InterimParameterizedType;
import org.apache.harmony.lang.reflect.parser.InterimClassType;
import org.apache.harmony.lang.reflect.parser.*;

import org.apache.harmony.vm.VMGenericsAndAnnotations;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * AuxiliaryChecker provides kinds of check.
 */
public final class AuxiliaryChecker {
        
    /**
     * This method checks the correspondence of the formal parameter number and the actual argument number.
     * 
     * @param ppType a parsered information produced from a parameterized type signature.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return an array of Type objects representing the actual type arguments to this type.
     */
    public static void checkArgsNumber(InterimParameterizedType ppType, Object startPoint) throws MalformedParameterizedTypeException {
        // XXX: reprogram method (for example, to improve the preloop and the loop)
        InterimParameterizedType currentBit = ppType;
        InterimType currentBitArgs[] = currentBit.parameters;

        InterimClassType currentClass = currentBit.rawType;
        Class klazz = null;
        try{
            klazz = AuxiliaryLoader.findClass(AuxiliaryFinder.transform(currentClass.classTypeName.substring(1).replace('/', '.')), startPoint);
        } catch (Throwable e) {
            
        }
        
        String ccSignature = AuxiliaryUtil.toUTF8(VMGenericsAndAnnotations.getSignature(klazz));
        
        InterimClassGenericDecl decl;
        if (ccSignature != null) {    
            decl =  (InterimClassGenericDecl) Parser.parseSignature(ccSignature, Parser.SignatureKind.CLASS_SIGNATURE, (java.lang.reflect.GenericDeclaration)startPoint);

            if ((decl.typeParameters != null && currentBitArgs != null && decl.typeParameters.length != currentBitArgs.length) || (decl.typeParameters == null && currentBitArgs != null) || (decl.typeParameters != null && currentBitArgs == null)) {
                throw new MalformedParameterizedTypeException();
            }
        } else {
            if (currentBitArgs != null && currentBitArgs.length > 0) {
                throw new MalformedParameterizedTypeException();
            }
        }
        
        while (currentBit.ownerType != null) {
            InterimType pt = currentBit.ownerType;
            if (pt instanceof InterimParameterizedType) {
                currentBit = (InterimParameterizedType)currentBit.ownerType;
            } else {
                break;
            }
            currentBitArgs = currentBit.parameters;
            
            currentClass = currentBit.rawType;
            klazz = null;
            try{
                //klazz = ClassLoader.findClass(currentClass.classTypeName);
                klazz = AuxiliaryLoader.findClass(AuxiliaryFinder.transform(currentClass.classTypeName.substring(1).replace('/', '.')), startPoint);
            } catch (Throwable e) {
            
            }

            ccSignature = AuxiliaryUtil.toUTF8(VMGenericsAndAnnotations.getSignature(klazz));
            if (ccSignature != null) {    
                decl =  (InterimClassGenericDecl) Parser.parseSignature(ccSignature, Parser.SignatureKind.CLASS_SIGNATURE, (java.lang.reflect.GenericDeclaration)startPoint);

                if ((decl.typeParameters != null && currentBitArgs != null && decl.typeParameters.length != currentBitArgs.length) || (decl.typeParameters == null && currentBitArgs != null) || (decl.typeParameters != null && currentBitArgs == null)) {
                    throw new MalformedParameterizedTypeException();
                }
            } else {
                if (currentBitArgs != null && currentBitArgs.length > 0) {
                    throw new MalformedParameterizedTypeException();
                }
            }
        }
    }
}
