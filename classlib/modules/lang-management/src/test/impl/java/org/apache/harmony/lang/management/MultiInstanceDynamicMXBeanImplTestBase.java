/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */

package org.apache.harmony.lang.management;

import java.util.List;

import javax.management.AttributeList;
import javax.management.ReflectionException;

import org.apache.harmony.lang.management.DynamicMXBeanImpl;


public abstract class MultiInstanceDynamicMXBeanImplTestBase extends
        DynamicMXBeanImplTestBase {

    protected List<DynamicMXBeanImpl> mbList;

    public void testInvoke() throws Exception {
        // Default for DynamicMXBeanImpl that has no operations to invoke...
        for (DynamicMXBeanImpl mb : mbList) {
            // No operations to invoke...
            try {
                Object retVal = mb.invoke("KissTheRoadOfRoratonga",
                        new Object[] { new Long(7446), new Long(54) },
                        new String[] { "java.lang.Long", "java.lang.Long" });
                fail("Should have thrown a ReflectionException.");
            } catch (ReflectionException ignore) {
            }
        }// end for
    }

    public void testGetAttributesBad() throws Exception {
        for (DynamicMXBeanImpl mb : mbList) {
            String[] badNames = { "Cork", "Galway" };
            AttributeList attributes = mb.getAttributes(badNames);
            assertNotNull(attributes);
            // No attributes will have been returned.
            assertTrue(attributes.size() == 0);

        }// end for
    }
}
