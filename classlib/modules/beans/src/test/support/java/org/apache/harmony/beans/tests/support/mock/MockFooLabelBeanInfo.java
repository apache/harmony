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

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class MockFooLabelBeanInfo extends SimpleBeanInfo {
    Class<MockFooLabel> clazz = MockFooLabel.class;

    String suffix = ".MockFooLabelBeanInfo";

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor beanDesc = new BeanDescriptor(clazz);
        beanDesc.setName(beanDesc.getName() + suffix);
        return beanDesc;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] pds = new PropertyDescriptor[1];
        try {
            PropertyDescriptor pd = new PropertyDescriptor("text", clazz);
            pd.setName(pd.getName() + suffix);
            pds[0] = pd;
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return pds;
    }

}
