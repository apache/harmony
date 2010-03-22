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
 * @author Alexey A. Ivanov
 */
package javax.swing;

import junit.framework.TestCase;

/**
 * Tests object serialization. Extend this class, make appropriate
 * initialization and then call <code>super.setUp()</code>. If your object
 * doesn't override <code>equals()</code>, override
 * <code>testSerializable</code>, where check the object read is consistent
 * to the object saved.
 *
 */
public abstract class SerializableTestCase extends TestCase {
    /**
     * Object under test which will be serialized.
     */
    protected Object toSave;

    /**
     * This is where deserialized object will be stored.
     */
    protected Object toLoad;

    /**
     * Initialize toSave object here.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        toLoad = BasicSwingTestCase.serializeObject(toSave);
    }

    /**
     * Override this method to check that saved and restored objects are
     * identical. By default equals method is used.
     * @throws Exception
     */
    public void testSerializable() throws Exception {
        assertEquals(toSave, toLoad);
    }
}
