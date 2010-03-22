/* Licensed to the Apache Software Foundation (ASF) under one or more
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

package org.apache.harmony.tests.java.util.regex;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

/**
 * TODO Type description
 */
@SuppressWarnings("nls")
public class PatternSyntaxExceptionTest extends TestCase {
    public void testCase() {
        String regex = "(";
        try {
            Pattern.compile(regex);
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException e) {
            // TOFIX: Commented out assertEquals tests...
            // TOFIX: should we match exception strings?
            // assertEquals("Unclosed group", e.getDescription());
            assertEquals(1, e.getIndex());
            // assertEquals("Unclosed group near index 1\n(\n ^",
            // e.getMessage());
            assertEquals(regex, e.getPattern());
        }
    }

    public void testCase2() {
        String regex = "[4-";
        try {
            Pattern.compile(regex);
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException e) {
            // TOFIX: Commented out assertEquals tests...
            // TOFIX: should we match exception strings?
            // assertEquals("Illegal character range", e.getDescription());
            assertEquals(3, e.getIndex());
            // assertEquals("Illegal character range near index 3\n[4-\n ^",
            // e.getMessage());
            assertEquals(regex, e.getPattern());
        }
    }

    /**
     * @tests serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {
        PatternSyntaxException object = new PatternSyntaxException("TESTDESC",
                "TESTREGEX", 3);
        SerializationTest.verifySelf(object, PATTERNSYNTAXEXCEPTION_COMPARATOR);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        PatternSyntaxException object = new PatternSyntaxException("TESTDESC",
                "TESTREGEX", 3);
        SerializationTest.verifyGolden(this, object,
                PATTERNSYNTAXEXCEPTION_COMPARATOR);
    }

    // Regression test for HARMONY-3787
    public void test_objectStreamField() {
        ObjectStreamClass objectStreamClass = ObjectStreamClass
                .lookup(PatternSyntaxException.class);
        assertNotNull(objectStreamClass.getField("desc"));
    }

    // comparator for BatchUpdateException field updateCounts
    private static final SerializableAssert PATTERNSYNTAXEXCEPTION_COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            // do common checks for all throwable objects
            SerializationTest.THROWABLE_COMPARATOR.assertDeserialized(initial,
                    deserialized);

            PatternSyntaxException initPatternSyntaxException = (PatternSyntaxException) initial;
            PatternSyntaxException dserPatternSyntaxException = (PatternSyntaxException) deserialized;

            // verify fields
            assertEquals(initPatternSyntaxException.getDescription(),
                    dserPatternSyntaxException.getDescription());
            assertEquals(initPatternSyntaxException.getPattern(),
                    dserPatternSyntaxException.getPattern());
            assertEquals(initPatternSyntaxException.getIndex(),
                    dserPatternSyntaxException.getIndex());
        }
    };
}
