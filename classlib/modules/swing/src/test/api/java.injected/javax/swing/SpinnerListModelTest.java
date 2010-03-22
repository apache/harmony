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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class SpinnerListModelTest extends BasicSwingTestCase {
    private SpinnerListModel model;

    private ChangeController chl;

    @Override
    public void setUp() {
        model = new SpinnerListModel();
        chl = new ChangeController();
        model.addChangeListener(chl);
    }

    @Override
    public void tearDown() {
        model = null;
        chl = null;
    }

    public void testSpinnerListModel() {
        List list = model.getList();
        assertNotNull(list);
        assertEquals(1, list.size());
        list = new ArrayList();
        list.add("listline1");
        list.add("listline2");
        list.add("listline3");
        model = new SpinnerListModel(list);
        assertSame(list, model.getList());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerListModel((Object[]) null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerListModel(new ArrayList());
            }
        });
        list.clear();
        list.add(null);
        list.add(null);
        list.add(null);
        model = new SpinnerListModel(list);
        assertNull(model.getList().get(2));
        assertNull(model.getValue());
        assertNull(model.getPreviousValue());
        assertNull(model.getNextValue());
        Object[] values = { "arrline1", "arrline2", new Integer(3) };
        model = new SpinnerListModel(values);
        assertEquals(model.getList(), Arrays.asList(values));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerListModel(new Object[0]);
            }
        });
    }

    public void testModelList() {
        List list = model.getList();
        assertNotNull(list);
        assertEquals(1, list.size());
        list = new ArrayList();
        list.add(null);
        list.add(null);
        list.add(null);
        model = new SpinnerListModel(list);
        assertNull(model.getList().get(2));
        assertNull(model.getValue());
        assertNull(model.getPreviousValue());
        assertNull(model.getNextValue());
    }

    public void testSetGetList() {
        List list = new ArrayList();
        list.add("listline1");
        list.add("listline2");
        list.add("listline3");
        model.setList(list);
        assertSame(list, model.getList());
        assertTrue(chl.isChanged());
        chl.reset();
        list.clear();
        list.add(new Integer(1));
        assertEquals(new Integer(1), model.getValue());
        assertFalse(chl.isChanged());
        chl.reset();
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setList(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setList(new ArrayList());
            }
        });
        assertSame(list, model.getList());
        assertFalse(chl.isChanged());
    }

    public void testSetGetValue() {
        List list = new ArrayList();
        list.add(new Integer(1));
        list.add(new Integer(2));
        list.add(new Integer(3));
        model.setList(list);
        assertEquals(new Integer(1), model.getValue());
        chl.reset();
        Object obj = new Integer(3);
        model.setValue(obj);
        assertEquals(obj, model.getValue());
        assertTrue(chl.isChanged());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Object obj = new Integer(13);
                model.setValue(obj);
            }
        });
    }

    public void testNextPreviousValue() {
        List list = new ArrayList();
        list.add(new Integer(1));
        list.add(new Integer(2));
        list.add(new Integer(3));
        model.setList(list);
        assertEquals(new Integer(1), model.getValue());
        assertNull(model.getPreviousValue());
        assertEquals(new Integer(2), model.getNextValue());
        assertEquals(new Integer(1), model.getValue());
        model.setValue(model.getNextValue());
        assertEquals(new Integer(3), model.getNextValue());
        model.setValue(model.getNextValue());
        assertEquals(new Integer(3), model.getValue());
        assertNull(model.getNextValue());
    }
}
