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

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.TypeVariable;
import org.apache.harmony.lang.reflect.parser.InterimParameterizedType;
import org.apache.harmony.lang.reflect.repository.TypeVariableRepository;

import org.apache.harmony.lang.reflect.parser.*;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * Finder provides kinds of finding.
 */

/*
 * ------------------------------------------------------------------------------------------------
 * TODO list:
 * 1. Maybe, the work with TypeVariableRepository (at least in this class) will be removed at all
 *    if it will be so inefficient as now (see the marked on left by /STARSTAR/ code introduced on Junuary 25, 2006
 * ------------------------------------------------------------------------------------------------
 */
public final class AuxiliaryFinder {
    
    /**
     * This method returns the generic class declaration which a parameterized type is derived from.
     * 
     * @param fldType a parsered information produced from a parameterized type signature.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found generic declaration for this type variable definition or null 
     *         if a generic declaration for this type variable does not exist at all.
     */
    public static Class findGenericClassDeclarationForParameterizedType(InterimParameterizedType fldType, Object startPoint) throws ClassNotFoundException {
        Class klass = null;
        if ((klass = verifyParameterizedType(fldType, startPoint)) != null) return klass;
//############################################################################################################################################
// The below fragment seems not to work after verifyParameterizedType implementation and the just above code line insertion
// but it should be retained until being 100% aware (just though the incorrect basing on $ is used here):
//FRAGMENT START V
        InterimType ownerType = fldType.ownerType;
        String binaryClassName = null;
        String tmp = fldType.rawType.classTypeName.substring(1).replace('/', '.'); // cut the first "L" (reference symbol) and change "/" by "."
        int ind;
        if ((ind = tmp.lastIndexOf('$')) != -1) {
            binaryClassName = tmp.substring(ind + 1);
        } else {
            binaryClassName = tmp;
        }
        while (ownerType != null && ownerType instanceof InterimParameterizedType) {
            tmp = ((InterimParameterizedType)ownerType).rawType.classTypeName.substring(1).replace('/', '.'); // cut the first "L" (reference symbol) and change "/" by "."
            if ((ind = tmp.lastIndexOf('$')) != -1) {
                tmp = tmp.substring(ind + 1);
            } else {
            }
            binaryClassName = tmp+"$"+binaryClassName;
            ownerType = ((InterimParameterizedType)ownerType).ownerType;
        } 
        if (ownerType != null && ownerType instanceof InterimClassType) {
            tmp = ((InterimClassType)ownerType).classTypeName.substring(1).replace('/', '.'); // cut the first "L" (reference symbol) and change "/" by "."
            binaryClassName = tmp+"$"+binaryClassName;
        } else if (ownerType != null) { // BUG
            int i = 0, j = 1; i = j/i;
        }
        klass = AuxiliaryLoader.findClass(binaryClassName, startPoint); // XXX: should we propagate the class loader of initial user's request (Field.getGenericType()) or use this one?
        return klass; //it may be null
//FRAGMENT FINISH ^
//############################################################################################################################################
    }

    /**
     * This auxiliary method returns true if the particular type variable
     * is defined in the specified generic declaration.
     * 
     * @param typeVariableName a name of a type variable.
     * @param declaration a generic declaration to check.
     * @return true if the specified declaration defines the specified type variable, false otherwise.
     */
    private static boolean hasGenericDeclaration(String typeVariableName, GenericDeclaration declaration) {
        TypeVariable[] vars = declaration.getTypeParameters();
        if (vars != null) {
            for (TypeVariable var : vars) {
                if (var.getName().equals(typeVariableName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * This method returns generic declaration where a type variable is defined in.
     * 
     * @param typeVariableName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found generic declaration for this type variable definition or null 
     *         if a generic declaration for this type variable does not exist at all.
     */
    public static GenericDeclaration findGenericDeclarationForTypeVariable(String typeVariableName, Object startPoint) {
        if (startPoint instanceof Class || startPoint instanceof Member) {
            Class klass;
            if (startPoint instanceof Class) {
                klass = (Class) startPoint;
            } else { // Member: Field, Method or Constructor
                if (!(startPoint instanceof Field)) { // Method or Constructor
                    if (hasGenericDeclaration(typeVariableName, (GenericDeclaration) startPoint)) {
                        return (GenericDeclaration) startPoint;
                    }
                }
                klass = ((Member) startPoint).getDeclaringClass();
            }
            while (klass != null) {
                if (hasGenericDeclaration(typeVariableName, klass)) {
                    return klass;
                }
                klass = klass.getDeclaringClass();
            }
        } 
        return null;
    }

    /**
     * This auxiliary method returns TypeVariable corresponding to the name
     * of type variable in the specified declaration.
     * 
     * @param typeVariableName a name of a type variable.
     * @param declaration a generic declaration to check.
     * @return the found type variable.
     */
    private static TypeVariable findTypeVariableInDeclaration(String typeVariableName, GenericDeclaration declaration) {
        TypeVariable variable = TypeVariableRepository.findTypeVariable(typeVariableName, declaration);
        if (variable != null) return variable;
        TypeVariable[] vars = declaration.getTypeParameters();
        if (vars != null) {
            for (TypeVariable var : vars) {
                if (var.getName().equals(typeVariableName)) {
                    /*
                     * Yes, it may be very inefficient now (for example, getTypeParameters()
                     * invokation above can just registry a TV but we need to recheck it in
                     * this line) but after all the TV-repository's functionality implementation
                     * it will be time to improvement.
                     * So, it was placed in repository just after an TV-instance creation but
                     * then it was removed (since we did not find it into the invoking method
                     * of this method look there at line with
                     * TypeVariableRepository.findTypeVariable(...) method invokation and also
                     * we did not find it in just above if-condition).
                     * As a consequence, we should reregistry it again as long as it become
                     * so popular again.
                     */
                    if (TypeVariableRepository.findTypeVariable(typeVariableName, declaration) == null) {
                        TypeVariableRepository.registerTypeVariable(var, typeVariableName, declaration);
                    }
                    return var;
                }
            }
        }
        return null;
    }
        
    /**
     * This method returns TypeVariable corresponding to the name of type variable in the current scope.
     * 
     * @param typeVariableName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found type variable.
     */
    public static TypeVariable findTypeVariable(String typeVariableName, Object startPoint) {
        typeVariableName = transform(typeVariableName); // Is it needed at all?
        if (startPoint instanceof Class || startPoint instanceof Member) {
            Class klass;
            TypeVariable ret;
            if (startPoint instanceof Class) {
                klass = (Class) startPoint;
            } else { // Member: Field, Method or Constructor
                if (!(startPoint instanceof Field)) { // Method or Constructor
                    ret = findTypeVariableInDeclaration(typeVariableName, (GenericDeclaration) startPoint);
                    if (ret != null) return ret;
                }
                klass = ((Member) startPoint).getDeclaringClass();
            }
            while (klass != null) {
                ret = findTypeVariableInDeclaration(typeVariableName, klass);
                if (ret != null) return ret;
                klass = klass.getDeclaringClass();
            }
        } 
        return null;
    }
    
    /**
     * This method transforms String with Utf8 to String without Utf8.
     * 
     * @return the transformed string.
     */
    public static String transform(String ini) {
        int ind;
        if ((ind = ini.indexOf("\\")) != -1) {
                    String res = ind == 0 ? "" : ini.substring(0, ind);
            String di1 = ini.substring(ind+2, ind+2+2); // to ommit \0
            String di2 = ini.substring(ind+6, ind+6+2); // to ommit the following \0
            String di3;
            if (Integer.parseInt(di1.substring(0, 1), 16) < 0xE) { // range 0x0080 - 0x07ff , for example: \0ce\091
                res = res + new String(new char[]{(char)(((Integer.parseInt(di1, 16)&0x1f)<<6)+(Integer.parseInt(di2, 16)&0x3f))});
                return res + transform(ini.substring(ind+8));
            } else if (Integer.parseInt(di1.substring(0, 1), 16) < 0xd800 || Integer.parseInt(di1.substring(0, 1), 16) > 0xdfff) { // range 0x0800 - 0xffff , for example: \0ef\0bf\08f
                di3 = ini.substring(ind+10, ind+10+2); // to ommit the following \0
                res = res + new String(new char[]{(char)(((Integer.parseInt(di1, 16)&0xf)<<12)+((Integer.parseInt(di2, 16)&0x3f)<<6)+(Integer.parseInt(di3, 16)&0x3f))});
                return res + transform(ini.substring(ind+12));
            } else { // range 0x10000 - 0x10FFFF (high-surrogates range = 0xd800-0xdbff; low-surrogates range = 0xdc00-0xdfff; ) , for example: \0ed\0a0\0b5\0ed\0be\0af
                di3 = ini.substring(ind+10, ind+10+2); // to ommit the following \0
                String di5 = ini.substring(ind+18, ind+18+2); // to ommit the following \0
                String di6 = ini.substring(ind+22, ind+22+2); // to ommit the following \0
                res = res + new String(new char[]{(char)((((Integer.parseInt(di2, 16)&0xf) + ((Integer.parseInt(di2, 16)&0xf)!=0?1:0))<<16)+((Integer.parseInt(di3, 16)&0x3f)<<10)+((Integer.parseInt(di5, 16)&0xf)<<6)+(Integer.parseInt(di6, 16)&0x3f))});
                return res + transform(ini.substring(ind+24));
            }
        }
        return ini;
    }
    
    /**
     * To use in findGenericClassDeclarationForParameterizedType method. 
     */
    private static Class verifyParameterizedType(InterimParameterizedType fldType, Object startPoint) throws ClassNotFoundException {
        Class klass = AuxiliaryLoader.findClass(fldType.rawType.classTypeName.substring(1).replace('/', '.')/*fldType.rawType.classTypeName*/, startPoint);
        if (fldType.currentClauseName != null && fldType.currentClauseName.length() > 0) {
            return klass; // has been verified
        }
        
        if (!klass.isLocalClass() && !klass.isMemberClass()) {
            return klass;
        }
        String snm = klass.getSimpleName(); // It must not be anonymous because it is the parameterised one.
        int i = fldType.rawType.classTypeName.lastIndexOf("$"+snm);
        if (i == -1) {
            return klass;
        }
        String rtnm = fldType.rawType.classTypeName.substring(0, i);
        InterimParameterizedType newPT = null;
        
        if (fldType.ownerType == null) {
            try {
                if (AuxiliaryLoader.findClass(rtnm.substring(1).replace('/', '.'), startPoint) != null) {
                    // telescoping a final unit:
                    InterimClassType newCT = new InterimClassType();
                    newCT.classTypeName = rtnm;
                    fldType.ownerType = (InterimType) newCT;
                }
            } catch(ClassNotFoundException _) {
                return null;
            }
            return klass;
        } else {
            if (!rtnm.equals((fldType.ownerType instanceof InterimParameterizedType ? ((InterimParameterizedType)fldType.ownerType).rawType.classTypeName : ((InterimClassType)fldType.ownerType).classTypeName))) {
                try {
                    if (AuxiliaryLoader.findClass(rtnm.substring(1).replace('/', '.'), startPoint) != null) {
                        // telescoping an intermediate unit:
                        newPT = new InterimParameterizedType();
/* ### */                        newPT.signature = fldType.signature.substring(0, fldType.signature.lastIndexOf("$"+snm)); //XXX: ???
                        newPT.currentClauseName = snm;
                        newPT.parameters = null;
                        newPT.rawType = new InterimClassType();
                        newPT.rawType.classTypeName = rtnm;
                        newPT.ownerType = fldType.ownerType;                              
                        verifyParameterizedType(newPT, startPoint);
                        fldType.ownerType = newPT;
                    }
                } catch(ClassNotFoundException _) {
                    return null;
                }
            }
            return klass;
        }    
    }
}
