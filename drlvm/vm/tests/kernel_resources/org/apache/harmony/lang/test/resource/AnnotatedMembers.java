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
package org.apache.harmony.lang.test.resource;

import org.apache.harmony.lang.annotation.AllTypesAntn;

/**
 * @author Alexey V. Varlamov
 */
@AnotherAntn
public abstract class AnnotatedMembers {
    @AnotherAntn
    public Object foo;
    
    @AnotherAntn
    public abstract void bar();

    public abstract void buz(@AnotherAntn String param);
    
    @AnotherAntn
    public AnnotatedMembers() {}
    
    @AllTypesAntn(classValue=AnotherAntn.class)
    public Object acme;
}
