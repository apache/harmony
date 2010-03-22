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

import java.lang.reflect.WildcardType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.MalformedParameterizedTypeException;

import org.apache.harmony.lang.reflect.parser.InterimWildcardType;
import org.apache.harmony.lang.reflect.parser.InterimClassType;
import org.apache.harmony.lang.reflect.support.AuxiliaryCreator;

/**
 * @author Serguei S. Zapreyev
 */
public final class WildcardTypeImpl implements WildcardType {
    private final Object startPoint;
    private Type[] lowerBounds;
    private Type[] upperBounds;
    private final InterimWildcardType wildCardTypeBillet;
    
    public WildcardTypeImpl(InterimWildcardType billet, Object startPoint) {
        this.upperBounds = null;
        this.lowerBounds = null;
        this.wildCardTypeBillet = billet;
        this.startPoint = startPoint; // XXX: It seems to be a temporary decision to introduce startPoint
                                      // It's not thought of finally. For example, it's not clear
                                      // how can it influence on the WildcardTypeRepository.java
                                      // ( "? extends TVAR" may be not equal to "? extends TVAR"
                                      // because of first TVAR and second TVAR may be different type variables
                                      // with the same name but I don't take it into account now in the repository algorithm
                                      // I check there only the sugnatures equality :( .
    }
    
    public boolean equals(Object other) {
        //return super.equals(other);
        if (!(other instanceof WildcardTypeImpl)) {
            return false;
        }
        boolean res = true;
        Type atl[] = (lowerBounds == null ? getLowerBounds() : lowerBounds);
        Type atl2[] = (((WildcardTypeImpl)other).lowerBounds == null ? ((WildcardTypeImpl)other).getLowerBounds() : ((WildcardTypeImpl)other).lowerBounds);
        if (atl.length != atl2.length) {
            return false;
        }
        for(int i = 0; i < atl.length; i++) {
            res = res && atl[i].equals(atl2[i]);
        }
        Type atu[] = (upperBounds == null ? getUpperBounds() : upperBounds);
        Type atu2[] = (((WildcardTypeImpl)other).upperBounds == null ? ((WildcardTypeImpl)other).getUpperBounds() : ((WildcardTypeImpl)other).upperBounds);
        if (atu.length != atu2.length) {
            return false;
        }
        for(int i = 0; i < atu.length; i++) {
            res = res && atu[i].equals(atu2[i]);
        }
        return res;
    }
    
    public Type[] getLowerBounds() throws TypeNotPresentException, MalformedParameterizedTypeException {
        if (lowerBounds == null) {
            if (wildCardTypeBillet.boundsType == false) {
                int l = wildCardTypeBillet.bounds.length;
                lowerBounds = new Type[l];
                for (int i = 0; i < l; i++) {
                    // it can be InterimTypeVariable or InterimParameterizedType or InterimClassType.
                    // The MalformedParameterizedTypeException and TypeNotPresentException should be raised here if it needs.
                    try {
                        lowerBounds[i] = AuxiliaryCreator.createTypeArg(wildCardTypeBillet.bounds[i], this.startPoint);
                    } catch(ClassNotFoundException e) {
                        throw new TypeNotPresentException(((InterimClassType)wildCardTypeBillet.bounds[i]).classTypeName.substring(1).replace('/', '.'), e); // ClassNotFoundException may appear here only for InterimClassType, see AuxiliaryCreator.createTypeArg.
                    }
                }
            } else {
                lowerBounds = new Type[0];
            }
        }
        return (Type[])this.lowerBounds.clone();
    }
    
    public Type[] getUpperBounds() throws TypeNotPresentException, MalformedParameterizedTypeException {
        if (upperBounds == null) {
            if (wildCardTypeBillet.boundsType) {
                int l = wildCardTypeBillet.bounds.length;
                upperBounds = new Type[l];
                for (int i = 0; i < l; i++) {
                    // it can be InterimTypeVariable or InterimParameterizedType or InterimClassType.
                    // The MalformedParameterizedTypeException and TypeNotPresentException should be raised here if it needs.
                    try {
                        upperBounds[i] = AuxiliaryCreator.createTypeArg(wildCardTypeBillet.bounds[i], this.startPoint);
                    } catch(ClassNotFoundException e) {
                        throw new TypeNotPresentException(((InterimClassType)wildCardTypeBillet.bounds[i]).classTypeName.substring(1).replace('/', '.'), e); // ClassNotFoundException may appear here only for InterimClassType, see AuxiliaryCreator.createTypeArg.
                    }
                }
            } else {
                upperBounds = new Type[1];
                upperBounds[0] = (Type) Object.class;
            }
        }
        return (Type[])this.upperBounds.clone();
    }

    public int hashCode() {
        //return super.hashCode();
        int res = 0;
        Type atl[] = (lowerBounds == null ? getLowerBounds() : lowerBounds);
        for(int i = 0; i < atl.length; i++) {
            res ^= atl[i].hashCode();
        }
        Type atu[] = (upperBounds == null ? getUpperBounds() : upperBounds);
        for(int i = 0; i < atu.length; i++) {
            res ^= atu[i].hashCode();
        }
        return res;
    }
    
    public String toString() {
        // TODO: this body should be reimplemented effectively.
        StringBuffer sb = new StringBuffer();
        sb.append("?");        Type at[] = (lowerBounds == null ? getLowerBounds() : lowerBounds);
        if (at.length != 0) {
            if (at[0] instanceof Class) {
                sb.append(" super "+((Class)at[0]).getName());            } else {                sb.append(" super "+((TypeVariable)at[0]).getName());            }
            return sb.toString();
        }
        at = (upperBounds == null ? getUpperBounds() : upperBounds);
        if (at[0] instanceof Class) {
            String ts = ((Class)at[0]).getName();
            if (!ts.equals("java.lang.Object")) {
                sb.append(" extends "+ts);
            }        } else {            sb.append(" extends "+((TypeVariable)at[0]).getName());        }
        return sb.toString();
    }
}