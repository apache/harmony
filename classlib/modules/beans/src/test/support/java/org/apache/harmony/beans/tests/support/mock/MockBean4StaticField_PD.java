/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.harmony.beans.tests.support.mock;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;

public class MockBean4StaticField_PD extends PersistenceDelegate {

    @Override
    public Expression instantiate(Object oldInstance, Encoder out) {
        Expression exp = null;

        try {
            exp = new Expression(MockBean4StaticField.class.getField("inst"),
                    "get", new Object[] { null });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return exp;
    }

}
