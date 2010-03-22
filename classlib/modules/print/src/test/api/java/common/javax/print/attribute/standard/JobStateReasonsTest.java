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
 * @author Elena V. Sayapina 
 */ 

package javax.print.attribute.standard;

import java.util.ArrayList;

import junit.framework.TestCase;

public class JobStateReasonsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JobStateReasonsTest.class);
    }

    static {
        System.out.println("JobStateReasons testing...");
    }



    /*
     * JobStateReasons() constructor testing. 
     */
    public final void testJobStateReasons() {
        JobStateReasons jsrs = new JobStateReasons();
        assertEquals(0, jsrs.size());
    }

    /*
     * JobStateReasons(Collection collection) constructor testing. 
     */
    public final void testJobStateReasonsCollection() {

        ArrayList list = new ArrayList(5);
        list.add(JobStateReason.ABORTED_BY_SYSTEM);
        list.add(JobStateReason.COMPRESSION_ERROR);
        list.add(JobStateReason.DOCUMENT_ACCESS_ERROR);
        list.add(JobState.ABORTED);
        try {
            JobStateReasons jsreasons = new JobStateReasons(list);
            fail("Constructor doesn't throw ClassCastException if " +
                    "some element of the collection isn't JobStateReason");
        } catch (ClassCastException e) {
        }

        list.remove(JobState.ABORTED);
        JobStateReasons jsrs = new JobStateReasons(list);
        JobStateReason reason = null;
        list.add(reason);
        try {
            JobStateReasons jsreasons = new JobStateReasons(list);
            fail("Constructor doesn't throw NullPointerException if " +
                    "some element of the collection is null");
        } catch (NullPointerException e) {
        }

        list.remove(reason);
        JobStateReasons jsreasons = new JobStateReasons(list);
        assertEquals(3, jsreasons.size());
        assertTrue(jsreasons.contains(JobStateReason.ABORTED_BY_SYSTEM));
        assertTrue(jsreasons.contains(JobStateReason.COMPRESSION_ERROR));
        assertTrue(jsreasons.contains(JobStateReason.DOCUMENT_ACCESS_ERROR));
    }

    /*
     * JobStateReasons(int initialCapacity) constructor testing. 
     */
    public final void testJobStateReasonsint() {
        try {
            JobStateReasons jsreasons = new JobStateReasons(-1);
            fail("Constructor doesn't throw IllegalArgumentException if " +
            "initialCapacity < 0");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     *add(JobStateReason jsr) method testing. 
     */
    public final void testAdd() {

        JobStateReasons jsreasons = new JobStateReasons(5);
        assertTrue(jsreasons.add(JobStateReason.ABORTED_BY_SYSTEM));
        assertFalse(jsreasons.add(JobStateReason.ABORTED_BY_SYSTEM));

        try {
            assertTrue(jsreasons.add(null));
            fail("Method doesn't throw NullPointerException if " +
                    "adding element is null");
        } catch (NullPointerException e) {
        }

        ArrayList list = new ArrayList(5);
        list.add(JobStateReason.COMPRESSION_ERROR);
        list.add(JobStateReason.JOB_PRINTING);
        jsreasons = new JobStateReasons(list);
        assertTrue(jsreasons.add(JobStateReason.ABORTED_BY_SYSTEM));
        assertFalse(jsreasons.add(JobStateReason.JOB_PRINTING));
    }

    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {
        JobStateReasons jsreasons = new JobStateReasons();
        assertEquals(JobStateReasons.class, jsreasons.getCategory());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        JobStateReasons jsreasons = new JobStateReasons();
        assertEquals("job-state-reasons", jsreasons.getName());
    }


}
