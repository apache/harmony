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

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleTypeVisitor6<R, P> extends AbstractTypeVisitor6<R, P> {

    protected R DEFAULT_VALUE;

    protected SimpleTypeVisitor6() {
        DEFAULT_VALUE = null;
    }

    protected SimpleTypeVisitor6(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    protected R defaultAction(TypeMirror e, P p) {
        return DEFAULT_VALUE;
    }

    public R visitArray(ArrayType t, P p) {
        return defaultAction(t, p);
    }

    public R visitDeclared(DeclaredType t, P p) {
        return defaultAction(t, p);
    }

    public R visitError(ErrorType t, P p) {
        return defaultAction(t, p);
    }

    public R visitExecutable(ExecutableType t, P p) {
        return defaultAction(t, p);
    }

    public R visitNoType(NoType t, P p) {
        return defaultAction(t, p);
    }

    public R visitNull(NullType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitive(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitTypeVariable(TypeVariable t, P p) {
        return defaultAction(t, p);
    }

    public R visitWildcard(WildcardType t, P p) {
        return defaultAction(t, p);
    }

}
