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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ParameterizedTypeImpl implements ParameterizedType {
    private final Type[] args;
    private final Type rawType;
    private final Type typeOwner;

    public ParameterizedTypeImpl(Type[] args, Type rawType, Type typeOwner) {
        this.args = args;
        this.rawType = rawType;
        this.typeOwner = typeOwner;
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof ParameterizedType)) { 
            return false;
        }
        ParameterizedType otherType = (ParameterizedType)other;
        Type[] arr = otherType.getActualTypeArguments(); 
        if (args.length != arr.length) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (!args[i].equals(arr[i])) {
                return false;
            }
        }
        return rawType.equals(otherType.getRawType()) 
        && (typeOwner == otherType.getOwnerType() 
                || typeOwner != null 
                && typeOwner.equals(otherType.getOwnerType()));
    }

    public Type[] getActualTypeArguments() {
        return (Type[])args.clone();
    }

    public Type getOwnerType() {
        return typeOwner;
    }

    public Type getRawType() {
        return rawType;
    }

    public int hashCode() {
        int ah = 0; 
        for(int i = 0; i < args.length; i++) {
            ah += args[i].hashCode();
        }
        if (typeOwner != null) {
            ah ^= typeOwner.hashCode();
        }
        return ah ^ rawType.hashCode();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (typeOwner!=null) {
            sb.append((typeOwner instanceof Class ? 
                    ((Class)typeOwner).getName() : typeOwner.toString()));
            sb.append('.').append(((Class)getRawType()).getSimpleName());
        } else {
            sb.append(((Class)getRawType()).getName());
        }
        if (args.length > 0) {
            sb.append("<");
            for (int i = 0; i < args.length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                if (args[i] instanceof Class) {
                    sb.append(((Class)args[i]).getName());
                } else {
                    sb.append(args[i].toString());
                } 
            }
            sb.append(">");
        }
        return sb.toString();
    }
}