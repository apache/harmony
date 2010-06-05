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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class SpinnerDateModelTest extends BasicSwingTestCase {
    private SpinnerDateModel model;

    private ChangeController chl;

    private Date now;

    private Date past;

    private Date future;

    @Override
    public void setUp() {
        model = new SpinnerDateModel();
        chl = new ChangeController(false);
        model.addChangeListener(chl);
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        now = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        past = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        future = calendar.getTime();
    }

    @Override
    public void tearDown() {
        model = null;
        chl = null;
        now = null;
        past = null;
        future = null;
    }

    @SuppressWarnings("deprecation")
    public void testSpinnerDateModel() {
        assertEquals(model.getDate().getDay(), (now.getDay() + 1) % 7);
        assertEquals(Calendar.DAY_OF_MONTH, model.getCalendarField());
        assertNull(model.getStart());
        assertNull(model.getEnd());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerDateModel(null, null, null, Calendar.DAY_OF_MONTH);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerDateModel(now, future, null, Calendar.DAY_OF_MONTH);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerDateModel(now, null, past, Calendar.DAY_OF_MONTH);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerDateModel(now, null, null, 666);
            }
        });
    }

    public void testSetGetStart() {
        model.setStart(past);
        assertTrue(chl.isChanged());
        assertSame(past, model.getStart());
        model.setValue(now);
        model.setStart(future);
        model.setStart(now);
        chl.reset();
        model.setStart(now);
        assertFalse(chl.isChanged());
    }

    public void testSetGetEnd() {
        model.setEnd(future);
        assertTrue(chl.isChanged());
        assertSame(future, model.getEnd());
        model.setValue(now);
        model.setEnd(past);
        model.setEnd(now);
        chl.reset();
        model.setEnd(now);
        assertFalse(chl.isChanged());
    }

    public void testSetGetValue() {
        model.setValue(now);
        assertTrue(chl.isChanged());
        assertNotSame(now, model.getValue());
        assertEquals(now, model.getValue());
        assertNotSame(model.getValue(), model.getValue());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setValue(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setValue("test");
            }
        });
        model.setEnd(past);
        model.setValue(now);
        model.setValue(now);
        chl.reset();
        model.setValue(now);
        assertFalse(chl.isChanged());
    }

    public void testGetDate() {
        model.setValue(now);
        assertNotSame(now, model.getDate());
        assertEquals(model.getDate(), model.getValue());
    }

    public void testSetGetCalendarField() {
        model.setCalendarField(Calendar.ERA);
        assertTrue(chl.isChanged());
        assertEquals(Calendar.ERA, model.getCalendarField());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setCalendarField(666);
            }
        });
        model.setCalendarField(Calendar.DAY_OF_WEEK);
        chl.reset();
        model.setCalendarField(Calendar.DAY_OF_WEEK);
        assertFalse(chl.isChanged());
    }

    @SuppressWarnings("deprecation")
    public void testGetNextValue() {
        model.setValue(now.clone());
        model.setEnd(future);
        now.setDate(now.getDate() + 1);
        assertEquals(now, model.getNextValue());
        now.setDate(now.getDate() - 1);
        model.setCalendarField(Calendar.MINUTE);
        now.setMinutes(now.getMinutes() + 1);
        assertEquals(now, model.getNextValue());
        model.setCalendarField(Calendar.YEAR);
        assertNull(model.getNextValue());
    }

    @SuppressWarnings("deprecation")
    public void testGetPreviousValue() {
        model.setValue(now.clone());
        model.setStart(past);
        now.setDate(now.getDate() - 1);
        assertEquals(now, model.getPreviousValue());
        now.setDate(now.getDate() + 1);
        model.setCalendarField(Calendar.MINUTE);
        now.setMinutes(now.getMinutes() - 1);
        assertEquals(now, model.getPreviousValue());
        model.setCalendarField(Calendar.YEAR);
        assertNull(model.getPreviousValue());
    }
}
