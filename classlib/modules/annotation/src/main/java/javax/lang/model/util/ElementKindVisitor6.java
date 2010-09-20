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

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

public class ElementKindVisitor6<R, P> extends SimpleElementVisitor6<R, P> {
    protected ElementKindVisitor6() {
        DEFAULT_VALUE = null;
    }

    protected ElementKindVisitor6(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    public R visitExecutable(ExecutableElement e, P p) {
        switch (e.getKind()) {
        case CONSTRUCTOR:
            return visitExecutableAsConstructor(e, p);
        case INSTANCE_INIT:
            return visitExecutableAsInstanceInit(e, p);
        case METHOD:
            return visitExecutableAsMethod(e, p);
        case STATIC_INIT:
            return visitExecutableAsStaticInit(e, p);
        }
        return null;
    }

    public R visitExecutableAsConstructor(ExecutableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitExecutableAsInstanceInit(ExecutableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitExecutableAsMethod(ExecutableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitExecutableAsStaticInit(ExecutableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitPackage(PackageElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitType(TypeElement e, P p) {
        switch (e.getKind()) {
        case ANNOTATION_TYPE:
            return visitTypeAsAnnotationType(e, p);
        case CLASS:
            return visitTypeAsClass(e, p);
        case ENUM:
            return visitTypeAsEnum(e, p);
        case INTERFACE:
            return visitTypeAsInterface(e, p);
        }
        return null;
    }

    public R visitTypeAsAnnotationType(TypeElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitTypeAsClass(TypeElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitTypeAsEnum(TypeElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitTypeAsInterface(TypeElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        if (e.getKind() == ElementKind.TYPE_PARAMETER) {
            return defaultAction(e, p);
        }
        return null;
    }

    public R visitVariable(VariableElement e, P p) {
        switch (e.getKind()) {
        case ENUM_CONSTANT:
            return visitVariableAsEnumConstant(e, p);
        case EXCEPTION_PARAMETER:
            return visitVariableAsExceptionParameter(e, p);
        case FIELD:
            return visitVariableAsField(e, p);
        case LOCAL_VARIABLE:
            return visitVariableAsLocalVariable(e, p);
        case PARAMETER:
            return visitVariableAsParameter(e, p);
        }
        return null;
    }

    public R visitVariableAsEnumConstant(VariableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitVariableAsExceptionParameter(VariableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitVariableAsField(VariableElement e, P p) {
        return defaultAction(e, p);

    }

    public R visitVariableAsLocalVariable(VariableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitVariableAsParameter(VariableElement e, P p) {
        return defaultAction(e, p);
    }
}
