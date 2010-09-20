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

import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

public interface Elements {
    List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e);

    List<? extends Element> getAllMembers(TypeElement type);

    Name getBinaryName(TypeElement type);

    String getConstantExpression(Object value);

    String getDocComment(Element e);

    Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
            AnnotationMirror a);

    Name getName(CharSequence cs);

    PackageElement getPackageElement(CharSequence name);

    PackageElement getPackageOf(Element type);

    TypeElement getTypeElement(CharSequence name);

    boolean hides(Element hider, Element hidden);

    boolean isDeprecated(Element e);

    boolean overrides(ExecutableElement overrider,
            ExecutableElement overridden, TypeElement type);

    void printElements(Writer w, Element... elements);
}
