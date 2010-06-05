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
 * @author Anton Avtamonov
 */
package javax.swing;

public class PopupFactoryRTest extends BasicSwingTestCase {
    public PopupFactoryRTest(final String name) {
        super(name);
    }

    public void testGetPopup() throws Exception {
        Popup p1 = PopupFactory.getSharedInstance().getPopup(null, new JPanel(), 10, 10);
        p1.show();
        p1.hide();
        p1.hide();
        Popup p11 = PopupFactory.getSharedInstance().getPopup(null, new JPanel(), 20, 20);
        Popup p12 = PopupFactory.getSharedInstance().getPopup(null, new JPanel(), 20, 20);
        if (isHarmony()) {
            assertSame(p1, p11);
        }
        assertNotSame(p11, p12);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PopupFactoryRTest.class);
    }
}
