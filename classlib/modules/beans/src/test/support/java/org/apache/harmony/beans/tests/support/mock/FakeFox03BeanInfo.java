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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class FakeFox03BeanInfo extends SimpleBeanInfo {

    PropertyDescriptor propdescr[];

    EventSetDescriptor eventdescr[];

    public FakeFox03BeanInfo() {
        super();
        try {
            propdescr = new PropertyDescriptor[] { new PropertyDescriptor(
                    "Other", FakeFox03.class) };
            eventdescr = new EventSetDescriptor[] { new EventSetDescriptor(
                    SomeOtherObject.class, "SomeOther",
                    SomeOtherListener.class, "aMethod") };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor descriptor = new BeanDescriptor(FakeFox03.class);
        descriptor.setName("SomeOtherBean Descriptor");
        return descriptor;
    }

    public static final Image someOtherImage = new BufferedImage(1, 1,
            BufferedImage.TYPE_3BYTE_BGR);

    public Image getIcon(int iconKind) {
        return someOtherImage;

    }

    public int getDefaultPropertyIndex() {
        System.out
                .println("SomeOtherBeanBeanInfo.getDefaultPropertyIndex() called...");
        return propdescr.length - 1;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return propdescr;
    }

    public int getDefaultEventIndex() {
        return eventdescr.length - 1;
    }

    public EventSetDescriptor[] getEventSetDescriptors() {
        return eventdescr;
    }

    private interface SomeOtherListener {
        public void aMethod(SomeOtherEvent s);
    }

    private class SomeOtherObject {
        public void addFakeFox03BeanInfo$SomeOtherListener(
                SomeOtherListener l) {
        }

        public void removeFakeFox03BeanInfo$SomeOtherListener(
                SomeOtherListener l) {
        }
    }

    private class SomeOtherEvent {
    }
}
