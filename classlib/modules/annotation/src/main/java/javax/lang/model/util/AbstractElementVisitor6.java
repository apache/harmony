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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.UnknownElementException;

public abstract class AbstractElementVisitor6<R, P> implements
        ElementVisitor<R, P> {
    protected AbstractElementVisitor6() {
        super();
    }

    public final R visit(Element e, P p) {
        return e.accept(this, p);
    }

    public final R visit(Element e) {
        return e.accept(this, null);
    }

    public R visitUnknown(Element e, P p) {
        throw new UnknownElementException(e, p);
    }
}
