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
 * @author Dennis Ushakov
 */
package javax.swing;

import java.util.Arrays;
import java.util.EventListener;

public class AbstractSpinnerModelTest extends BasicSwingTestCase {
    private AbstractSpinnerModel model;

    private ChangeController chl;

    private static class TestListener implements EventListener {
    };

    @Override
    public void setUp() {
        model = new AbstractSpinnerModel() {
            public Object getNextValue() {
                return null;
            }

            public Object getPreviousValue() {
                return null;
            }

            public Object getValue() {
                return "test";
            }

            public void setValue(Object value) {
                fireStateChanged();
            }
        };
        chl = new ChangeController();
    }

    @Override
    public void tearDown() {
        model = null;
        chl = null;
    }

    public void testAddRemoveChangeListener() {
        model.addChangeListener(chl);
        assertEquals(1, model.listenerList.getListenerCount());
        model.removeChangeListener(chl);
        assertEquals(0, model.listenerList.getListenerCount());
    }

    public void getChangeListeners() {
        model.addChangeListener(chl);
        assertTrue(Arrays.asList(model.getChangeListeners()).contains(chl));
    }

    public void testFireStateChanged() {
        model.addChangeListener(chl);
        model.setValue(null);
        assertTrue(chl.isChanged());
    }

    public void testGetListeners() {
        final TestListener testListener = new TestListener();
        model.listenerList.add(TestListener.class, testListener);
        assertTrue(Arrays.asList(model.getListeners(TestListener.class)).contains(testListener));
    }
}
