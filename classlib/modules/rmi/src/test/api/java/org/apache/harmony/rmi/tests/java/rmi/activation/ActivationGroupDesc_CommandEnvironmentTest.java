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

import java.rmi.activation.ActivationGroupDesc.CommandEnvironment;
import java.util.Arrays;
import junit.framework.TestCase;

public class ActivationGroupDesc_CommandEnvironmentTest extends TestCase {
    public void testCommandEnvironment() {
        CommandEnvironment ce = new CommandEnvironment(null, null);
        assertNull(ce.getCommandPath());
        assertNotNull(ce.getCommandOptions());
        assertEquals(0, ce.getCommandOptions().length);
        String[] options = new String[] { "option1", "option2" };
        ce = new CommandEnvironment("cmd", options);
        assertEquals("cmd", ce.getCommandPath());
        assertNotSame(options, ce.getCommandOptions());
        assertTrue(Arrays.equals(options, ce.getCommandOptions()));
    }

    public void testHashCode() {
        CommandEnvironment ce = new CommandEnvironment(null, null);
        assertEquals(ce.hashCode(), new CommandEnvironment(null, null).hashCode());
        ce = new CommandEnvironment("cmd", new String[] { "option1", "option2" });
        assertEquals(ce.hashCode(), new CommandEnvironment("cmd", new String[] { "option1",
                "option2" }).hashCode());
    }

    public void testEqualsObject() {
        CommandEnvironment ce = new CommandEnvironment(null, null);
        assertEquals(ce, new CommandEnvironment(null, null));
        ce = new CommandEnvironment("cmd", new String[] { "option1", "option2" });
        assertEquals(ce, new CommandEnvironment("cmd", new String[] { "option1", "option2" }));
    }
}
