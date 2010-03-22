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
/**
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt;

import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.ObjectAccessor;

/**
 * This class allows modifying class fields, including 'final' ones.
 * It's designed for de-serialization purposes
 */
public class FieldsAccessor {

    private final Class<?> clazz;
    private final Object object;
    private final ObjectAccessor accessor;

    /**
     * Prepare FieldsAccessor to the work
     * @param clazz - exact class being de-serialized.
     *  Good example: <code>Component.class</code>
     *  Bad example: <code>this.getClass()</code>
     * @param object - object being modified
     */
    public FieldsAccessor(Class<?> clazz, Object object) {
        accessor = AccessorFactory.getObjectAccessor();
        this.clazz = clazz;
        this.object = object;
    }

    /**
     * Set field value
     * @param fieldName
     * @param value
     */
    public void set(String fieldName, Object value) {
        long id = accessor.getFieldID(clazz, fieldName);
        accessor.setObject(object, id, value);
    }

}
