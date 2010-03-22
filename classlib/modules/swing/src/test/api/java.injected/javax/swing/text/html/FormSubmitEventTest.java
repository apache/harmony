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
 * @author Vadim L. Bogdanov
 */

package javax.swing.text.html;

import java.util.Arrays;

import javax.swing.SwingTestCase;

public class FormSubmitEventTest extends SwingTestCase {

    public FormSubmitEventTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetData() {
        if (!isHarmony()) {
            return;
        }

        String data = "data";
        FormSubmitEvent.MethodType method = FormSubmitEvent.MethodType.GET;
        FormSubmitEvent event = new FormSubmitEvent(new Object(), null, null,
                                                    null, null, null,
                                                    method, data);

        assertSame(data, event.getData());
    }

    public void testGetMethod() {
        if (!isHarmony()) {
            return;
        }

        String data = "data";
        FormSubmitEvent.MethodType method = FormSubmitEvent.MethodType.GET;
        FormSubmitEvent event = new FormSubmitEvent(new Object(), null, null,
                                                    null, null, null,
                                                    method, data);

        assertSame(method, event.getMethod());
    }

    public void testMethodType() {
        assertSame(FormSubmitEvent.MethodType.GET,
                   FormSubmitEvent.MethodType.valueOf("GET"));
        assertSame(FormSubmitEvent.MethodType.POST,
                   FormSubmitEvent.MethodType.valueOf("POST"));
        testExceptionalCase(new IllegalArgumentCase() {
            public void exceptionalAction() throws Exception {
                FormSubmitEvent.MethodType.valueOf("SOMETHING_ELSE");
            }
        });

        FormSubmitEvent.MethodType[] values = FormSubmitEvent.MethodType.values();
        assertEquals(2, values.length);
        assertEquals(0,
                     Arrays.asList(values).indexOf(FormSubmitEvent.MethodType.GET));
        assertEquals(1,
                     Arrays.asList(values).indexOf(FormSubmitEvent.MethodType.POST));
    }
}
