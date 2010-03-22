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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.rmi.tests.java.rmi.activation;

import java.rmi.activation.ActivationGroupDesc;
import java.rmi.activation.ActivationGroupDesc.CommandEnvironment;
import java.util.Properties;
import junit.framework.TestCase;

public class ActivationGroupDescTest extends TestCase {
    public void testActivationGroupDescPropertiesCommandEnvironment() {
        ActivationGroupDesc agd = new ActivationGroupDesc(null, null);
        assertNull(agd.getClassName());
        assertNull(agd.getLocation());
        assertNull(agd.getData());
        assertNull(agd.getPropertyOverrides());
        assertNull(agd.getCommandEnvironment());
        Properties p = new Properties();
        p.setProperty("key", "value");
        CommandEnvironment ce = new ActivationGroupDesc.CommandEnvironment("cmd", new String[] {
                "option1", "option1" });
        agd = new ActivationGroupDesc(p, ce);
        assertNull(agd.getClassName());
        assertNull(agd.getLocation());
        assertNull(agd.getData());
        assertNotSame(p, agd.getPropertyOverrides());
        assertEquals(p, agd.getPropertyOverrides());
        assertEquals(ce, agd.getCommandEnvironment());
    }

    public void testActivationGroupDescStringStringMarshalledObjectPropertiesCommandEnvironment() {
        ActivationGroupDesc agd = new ActivationGroupDesc(null, null, null, null, null);
        assertNull(agd.getClassName());
        assertNull(agd.getLocation());
        assertNull(agd.getData());
        assertNull(agd.getPropertyOverrides());
        assertNull(agd.getCommandEnvironment());
        Properties p = new Properties();
        p.setProperty("key", "value");
        CommandEnvironment ce = new ActivationGroupDesc.CommandEnvironment("cmd", new String[] {
                "option1", "option1" });
        agd = new ActivationGroupDesc("className", "location", null, p, ce);
        assertEquals("className", agd.getClassName());
        assertEquals("location", agd.getLocation());
        assertNull(agd.getData());
        assertNotSame(p, agd.getPropertyOverrides());
        assertEquals(p, agd.getPropertyOverrides());
        assertEquals(ce, agd.getCommandEnvironment());
    }

    public void testHashCode() {
        ActivationGroupDesc agd = new ActivationGroupDesc(null, null, null, null, null);
        assertEquals(agd.hashCode(), new ActivationGroupDesc(null, null, null, null, null)
                .hashCode());
        agd = new ActivationGroupDesc("className", "codebase", null, null, null);
        assertEquals(agd.hashCode(), new ActivationGroupDesc("className", "codebase", null,
                null, null).hashCode());
    }

    public void testEqualsObject() {
        ActivationGroupDesc agd = new ActivationGroupDesc(null, null, null, null, null);
        assertEquals(agd, new ActivationGroupDesc(null, null, null, null, null));
        Properties p = new Properties();
        p.setProperty("key", "value");
        CommandEnvironment ce = new ActivationGroupDesc.CommandEnvironment("cmd", new String[] {
                "option1", "option1" });
        agd = new ActivationGroupDesc("className", "location", null, p, ce);
        assertEquals(agd, new ActivationGroupDesc("className", "location", null, p, ce));
    }
}
