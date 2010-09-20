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

public interface ElementVisitor<R, P> {
    R visit(Element e);

    R visit(Element e, P p);

    R visitExecutable(ExecutableElement e, P p);

    R visitPackage(PackageElement e, P p);

    R visitType(TypeElement e, P p);

    R visitTypeParameter(TypeParameterElement e, P p);

    R visitUnknown(Element e, P p);

    R visitVariable(VariableElement e, P p);
}
