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
package org.apache.harmony.tests.tools.serialver;

import org.apache.harmony.tools.ClassProvider;
import org.apache.harmony.tools.serialver.Clazz;
import org.apache.harmony.tools.serialver.Main;

import junit.framework.*;

public class Test extends TestCase {

    public void testCalculeSUID() throws ClassNotFoundException {
        ClassProvider classProvider = new ClassProvider(null, null, false);

        Clazz clazz = new Clazz(classProvider, "org.apache.harmony.tests.tools.serialver.Point");
        assertEquals("-2044326457932868113", "" + Main.calculeSUID(clazz));

        clazz = new Clazz(classProvider, "java.lang.String");
        assertEquals("-6849794470754667710", "" + Main.calculeSUID(clazz));

        clazz = new Clazz(classProvider, "java.util.Random");
        assertEquals("3905348978240129619", "" + Main.calculeSUID(clazz));
    }

    public void testIsSerializable() throws ClassNotFoundException {
        ClassProvider classProvider = new ClassProvider(null, null, false);

        Clazz clazz = new Clazz(classProvider, "org.apache.harmony.tests.tools.serialver.NoSerialize");
        assertEquals(false, Main.isSerializable(clazz));

        clazz = new Clazz(classProvider, "java.lang.Thread");
        assertEquals(false, Main.isSerializable(clazz));

        clazz = new Clazz(classProvider, "java.io.File");
        assertEquals(true, Main.isSerializable(clazz));
    }

}
