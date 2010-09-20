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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class ElementFilter {

    private static List allIn(Iterable<? extends Element> elements,
            ElementKind e) {
        List list = new ArrayList<ExecutableElement>();
        for (Element object : elements) {
            if (object.getKind().equals(e)) {
                list.add(object);
            }
        }
        return list;
    }

    public static Set allIn(Set<? extends Element> elements, ElementKind e) {
        Set set = new HashSet<ExecutableElement>();
        for (Element object : elements) {
            if (object.getKind().equals(e)) {
                set.add(object);
            }
        }
        return set;
    }

    public static List<ExecutableElement> constructorsIn(
            Iterable<? extends Element> elements) {
        return allIn(elements, ElementKind.CONSTRUCTOR);
    }

    public static Set<ExecutableElement> constructorsIn(
            Set<? extends Element> elements) {
        return allIn(elements, ElementKind.CONSTRUCTOR);
    }

    public static List<VariableElement> fieldsIn(
            Iterable<? extends Element> elements) {
        return allIn(elements, ElementKind.FIELD);
    }

    public static Set<VariableElement> fieldsIn(Set<? extends Element> elements) {
        return allIn(elements, ElementKind.FIELD);
    }

    public static List<ExecutableElement> methodsIn(
            Iterable<? extends Element> elements) {
        return allIn(elements, ElementKind.METHOD);
    }

    public static Set<ExecutableElement> methodsIn(
            Set<? extends Element> elements) {
        return allIn(elements, ElementKind.METHOD);
    }

    public static List<PackageElement> packagesIn(
            Iterable<? extends Element> elements) {
        return allIn(elements, ElementKind.PACKAGE);
    }

    public static Set<PackageElement> packagesIn(Set<? extends Element> elements) {
        return allIn(elements, ElementKind.PACKAGE);
    }

    public static List<TypeElement> typesIn(Iterable<? extends Element> elements) {
        List<TypeElement> list = new ArrayList<TypeElement>();
        for (Element object : elements) {
            switch (object.getKind()) {
            case ANNOTATION_TYPE:
            case CLASS:
            case ENUM:
                list.add((TypeElement)object);
            default:
                continue;
            }
        }
        return list;
    }

    public static Set<TypeElement> typesIn(Set<? extends Element> elements) {
        Set<TypeElement> set = new HashSet<TypeElement>();
        for (Element object : elements) {
            switch (object.getKind()) {
            case ANNOTATION_TYPE:
            case CLASS:
            case ENUM:
                set.add((TypeElement)object);
            default:
                continue;
            }
        }
        return set;
    }
}
