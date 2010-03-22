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

package javax.sql.rowset.serial;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Vector;

import org.apache.harmony.sql.internal.nls.Messages;

public class SerialJavaObject implements Serializable, Cloneable {

    private static final long serialVersionUID = -1465795139032831023L;

    /**
     * obj and chain are defined in serialized form.
     */
    private Object obj;

    @SuppressWarnings( { "unchecked", "unused" })
    // Required by serialized form
    private Vector chain;

    private transient Field[] fields;

    public SerialJavaObject(Object obj) throws SerialException {
        if (null == obj) {
            throw new NullPointerException();
        }
        if (!(obj instanceof Serializable)) {
            throw new SerialException(Messages.getString("sql.41"));
        }
        this.obj = obj;
    }

    public Field[] getFields() throws SerialException {
        if (fields == null) {
            fields = obj.getClass().getFields();
        }
        return fields;
    }

    public Object getObject() throws SerialException {
        return obj;
    }
}
