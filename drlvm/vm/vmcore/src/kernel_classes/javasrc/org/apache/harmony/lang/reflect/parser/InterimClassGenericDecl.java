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

package org.apache.harmony.lang.reflect.parser;

import java.util.Arrays;

/**
 * @author Serguei S. Zapreyev
 */
public final class InterimClassGenericDecl implements InterimGenericDeclaration {
    public InterimType superClass;
    public InterimType superInterfaces[];
    public InterimTypeParameter typeParameters[];
    
    public String toString() { 
        return "InterimClassGenericDecl: super=" + superClass
        + " intfs=" + (superInterfaces == null ? "[]" : Arrays.asList(superInterfaces).toString())
        + " params=" + (typeParameters == null ? "[]" : Arrays.asList(typeParameters).toString());
    }

}