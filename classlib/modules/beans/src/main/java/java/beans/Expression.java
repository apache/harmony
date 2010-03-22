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

package java.beans;

import org.apache.harmony.beans.internal.nls.Messages;

public class Expression extends Statement {

    boolean valueIsDefined = false;

    Object value;

    public Expression(Object value, Object target, String methodName,
            Object[] arguments) {
        super(target, methodName, arguments);
        this.value = value;
        this.valueIsDefined = true;
    }

    public Expression(Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
        this.value = null;
        this.valueIsDefined = false;
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();

            if (!valueIsDefined) {
                sb.append("<unbound>"); //$NON-NLS-1$
            } else {
                if (value == null) {
                    sb.append("null"); //$NON-NLS-1$
                } else {
                    sb.append(convertClassName(value.getClass()));
                }
            }
            sb.append('=');
            sb.append(super.toString());

            return sb.toString();
        } catch (Exception e) {
            return new String(Messages.getString("beans.0D", e.getClass())); //$NON-NLS-1$
        }
    }

    public void setValue(Object value) {
        this.value = value;
        this.valueIsDefined = true;
    }

    public Object getValue() throws Exception {
        if (!valueIsDefined) {
            value = invokeMethod();
            valueIsDefined = true;
        }
        return value;
    }
}
