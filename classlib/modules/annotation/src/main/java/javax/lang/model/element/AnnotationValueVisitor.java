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

package javax.lang.model.element;

import java.util.List;

import javax.lang.model.type.TypeMirror;

public interface AnnotationValueVisitor<R, P> {
    R visit(AnnotationValue av);

    R visit(AnnotationValue av, P p);

    R visitAnnotation(AnnotationMirror a, P p);

    R visitArray(List<? extends AnnotationValue> vals, P p);

    R visitBoolean(boolean b, P p);

    R visitByte(byte b, P p);

    R visitChar(char c, P p);

    R visitDouble(double d, P p);

    R visitEnumConstant(VariableElement c, P p);

    R visitFloat(float f, P p);

    R visitInt(int i, P p);

    R visitLong(long i, P p);

    R visitShort(short s, P p);

    R visitString(String s, P p);

    R visitType(TypeMirror t, P p);

    R visitUnknown(AnnotationValue av, P p);
}
