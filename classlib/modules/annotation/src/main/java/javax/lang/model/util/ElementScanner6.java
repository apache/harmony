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

import java.util.Iterator;
import java.util.List;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ElementScanner6<R, P> extends AbstractElementVisitor6<R, P> {
    protected R DEFAULT_VALUE;

    protected ElementScanner6() {
        DEFAULT_VALUE = null;
    }

    protected ElementScanner6(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    public R scan(Element e) {
        return scan(e, null);
    }

    public R scan(Element e, P p) {
        return e.accept(this, p);
    }

    public R scan(Iterable<? extends Element> iterable, P p) {
        Iterator<? extends Element> it = iterable.iterator();
        R r = DEFAULT_VALUE;
        while (it.hasNext()) {
            Element element = it.next();
            r = scan(element, p);
        }
        return r;
    }

    public R visitExecutable(ExecutableElement e, P p) {
        List<? extends Element> list = e.getParameters();
        R result = DEFAULT_VALUE;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            result = element.accept(this, p);
        }
        return result;
    }

    public R visitPackage(PackageElement e, P p) {
        List<? extends Element> list = e.getEnclosedElements();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            element.accept(this, p);
        }
        return e.accept(this, p);
    }

    public R visitType(TypeElement e, P p) {
        List<? extends Element> list = e.getEnclosedElements();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            element.accept(this, p);
        }
        return e.accept(this, p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        List<? extends Element> list = e.getEnclosedElements();
        R result = DEFAULT_VALUE;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            result = element.accept(this, p);
        }
        return result;
    }

    public R visitVariable(VariableElement e, P p) {
        List<? extends Element> list = e.getEnclosedElements();
        R result = DEFAULT_VALUE;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            result = element.accept(this, p);
        }
        return result;
    }
}
