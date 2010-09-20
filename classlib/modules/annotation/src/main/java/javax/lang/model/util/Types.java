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
package javax.lang.model.util;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

public interface Types {
    Element asElement(TypeMirror t);

    TypeMirror asMemberOf(DeclaredType containing, Element element);

    TypeElement boxedClass(PrimitiveType p);

    TypeMirror capture(TypeMirror t);

    boolean contains(TypeMirror t1, TypeMirror t2);

    List<? extends TypeMirror> directSupertypes(TypeMirror t);

    TypeMirror erasure(TypeMirror t);

    ArrayType getArrayType(TypeMirror componentType);

    DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
            TypeMirror... typeArgs);

    DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs);

    NoType getNoType(TypeKind kind);

    NullType getNullType();

    PrimitiveType getPrimitiveType(TypeKind kind);

    WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound);

    boolean isAssignable(TypeMirror t1, TypeMirror t2);

    boolean isSameType(TypeMirror t1, TypeMirror t2);

    boolean isSubsignature(ExecutableType m1, ExecutableType m2);

    boolean isSubtype(TypeMirror t1, TypeMirror t2);

    PrimitiveType unboxedType(TypeMirror t);
}
