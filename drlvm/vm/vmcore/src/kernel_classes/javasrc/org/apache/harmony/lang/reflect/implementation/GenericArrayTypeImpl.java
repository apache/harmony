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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * @author Serguei S. Zapreyev
 */
public final class GenericArrayTypeImpl implements GenericArrayType {
    private final Type nextLayer;
    
    public GenericArrayTypeImpl(Type nextLayer) {
        this.nextLayer = nextLayer;
    }
    
    public boolean equals(Object other) {
        //return super.equals(other);
        if (!(other instanceof GenericArrayTypeImpl)) {
            return false;
        }
        Type next = nextLayer;
        Type next2 = ((GenericArrayTypeImpl)other).nextLayer;
        while (next instanceof GenericArrayTypeImpl && next2 instanceof GenericArrayTypeImpl) {
            if (!next.equals(next2)) {
                return false;
            }
            next = ((GenericArrayTypeImpl)next).nextLayer;
            next2 = ((GenericArrayTypeImpl)next2).nextLayer;
        }
        if (next.getClass().isInstance(next2)) {
            return next.equals(next2);
        }
        return false;
    }
    
    public Type getGenericComponentType() {
        return nextLayer;
    }

    public int hashCode() {
        //return super.hashCode();
        int res = 0;
        Type next = nextLayer;
        while (next instanceof GenericArrayType) {
            res += 1;
            next = ((GenericArrayTypeImpl)next).nextLayer;
        }
        return res ^ next.hashCode();
    }
    
    public String toString() {
        // TODO: this body should be reimplemented effectively.
        StringBuffer sb = new StringBuffer();
        if(nextLayer instanceof GenericArrayType){
            sb.append(getGenericComponentType().toString());
        } else {
            if(nextLayer instanceof Class){
                sb.append(((Class)nextLayer).getName());
            } else {
                sb.append(nextLayer.toString());
            }
        }
        sb.append("[]");
        return sb.toString();
    }
}