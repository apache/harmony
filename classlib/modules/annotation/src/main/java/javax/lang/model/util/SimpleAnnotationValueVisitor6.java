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

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleAnnotationValueVisitor6<R, P> extends
        AbstractAnnotationValueVisitor6<R, P> {

    protected final R DEFAULT_VALUE;

    protected SimpleAnnotationValueVisitor6() {
        DEFAULT_VALUE = null;
    }

    protected SimpleAnnotationValueVisitor6(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    protected R defaultAction(Object o, P p) {
        return DEFAULT_VALUE;
    }

    public R visitAnnotation(AnnotationMirror a, P p) {
        return defaultAction(a, p);
    }

    public R visitArray(List vals, P p) {
        return defaultAction(vals, p);
    }

    public R visitBoolean(boolean b, P p) {
        return defaultAction(b, p);
    }

    public R visitByte(byte b, P p) {
        return defaultAction(b, p);
    }

    public R visitChar(char c, P p) {
        return defaultAction(c, p);
    }

    public R visitDouble(double d, P p) {
        return defaultAction(d, p);
    }

    public R visitEnumConstant(VariableElement c, P p) {
        return defaultAction(c, p);
    }

    public R visitFloat(float f, P p) {
        return defaultAction(f, p);
    }

    public R visitInt(int i, P p) {
        return defaultAction(i, p);
    }

    public R visitLong(long i, P p) {
        return defaultAction(i, p);
    }

    public R visitShort(short s, P p) {
        return defaultAction(s, p);
    }

    public R visitString(String s, P p) {
        return defaultAction(s, p);
    }

    public R visitType(TypeMirror t, P p) {
        return defaultAction(t, p);
    }

}
