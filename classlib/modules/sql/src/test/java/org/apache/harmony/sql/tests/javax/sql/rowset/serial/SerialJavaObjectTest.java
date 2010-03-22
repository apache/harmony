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

package org.apache.harmony.sql.tests.javax.sql.rowset.serial;

import java.io.Serializable;
import java.util.Arrays;

import javax.sql.rowset.serial.SerialException;
import javax.sql.rowset.serial.SerialJavaObject;

import junit.framework.TestCase;

public class SerialJavaObjectTest extends TestCase {
    public void test_Constructor() throws Exception {
        TransientFieldClass tfc = new TransientFieldClass();
        SerialJavaObject sjo;
        try {
            sjo = new SerialJavaObject(tfc);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // excepted
        }
        try {
            NonSerialiableClass nsc = new NonSerialiableClass();
            sjo = new SerialJavaObject(nsc);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // excepted
        }

        SerializableClass sc = new SerializableClass();
        sjo = new SerialJavaObject(sc);
        assertSame(sc, sjo.getObject());
        Arrays.equals(sjo.getFields(), sc.getClass().getFields());

        try {
            new SerialJavaObject(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    static class TransientFieldClass {
        transient int i;

        String s;
    }

    static class NonSerialiableClass {
        int i;

        String s;
    }

    static class StaticFieldClass {
        static int i;

        String s;
    }

    static class SerializableClass implements Serializable {
        private static final long serialVersionUID = 0L;

        int i;

        String s;
    }
}
