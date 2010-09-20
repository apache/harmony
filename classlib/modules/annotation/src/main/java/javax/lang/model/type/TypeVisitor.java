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

package javax.lang.model.type;

public interface TypeVisitor<R, P> {
    R visit(TypeMirror t);

    R visit(TypeMirror t, P p);

    R visitArray(ArrayType t, P p);

    R visitDeclared(DeclaredType t, P p);

    R visitError(ErrorType t, P p);

    R visitExecutable(ExecutableType t, P p);

    R visitNoType(NoType t, P p);

    R visitNull(NullType t, P p);

    R visitPrimitive(PrimitiveType t, P p);

    R visitTypeVariable(TypeVariable t, P p);

    R visitUnknown(TypeMirror t, P p);

    R visitWildcard(WildcardType t, P p);
}
