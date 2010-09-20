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
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleElementVisitor6<R, P> extends AbstractElementVisitor6<R, P> {

    protected R DEFAULT_VALUE;

    protected SimpleElementVisitor6() {
        DEFAULT_VALUE = null;
    }

    protected SimpleElementVisitor6(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    protected R defaultAction(Element e, P p) {
        return DEFAULT_VALUE;
    }

    public R visitExecutable(ExecutableElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitPackage(PackageElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitType(TypeElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        return defaultAction(e, p);
    }

    public R visitVariable(VariableElement e, P p) {
        return defaultAction(e, p);
    }

}
