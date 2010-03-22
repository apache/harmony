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

package org.apache.harmony.sql.tests.javax.transaction.xa;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import junit.framework.TestCase;

public class XAResourceTest extends TestCase {

    /*
     * Public statics test
     */
    public void testPublicStatics() {

        HashMap<String, Integer> thePublicStatics = new HashMap<String, Integer>();
        thePublicStatics.put("XA_OK", new Integer(0));
        thePublicStatics.put("XA_RDONLY", new Integer(3));
        thePublicStatics.put("TMSUSPEND", new Integer(33554432));
        thePublicStatics.put("TMSUCCESS", new Integer(67108864));
        thePublicStatics.put("TMSTARTRSCAN", new Integer(16777216));
        thePublicStatics.put("TMRESUME", new Integer(134217728));
        thePublicStatics.put("TMONEPHASE", new Integer(1073741824));
        thePublicStatics.put("TMNOFLAGS", new Integer(0));
        thePublicStatics.put("TMJOIN", new Integer(2097152));
        thePublicStatics.put("TMFAIL", new Integer(536870912));
        thePublicStatics.put("TMENDRSCAN", new Integer(8388608));

        Class<?> xAResourceClass;
        try {
            xAResourceClass = Class.forName("javax.transaction.xa.XAResource");
        } catch (ClassNotFoundException e) {
            fail("javax.transaction.xa.XAResource class not found!");
            return;
        } // end try

        Field[] theFields = xAResourceClass.getDeclaredFields();
        int requiredModifier = Modifier.PUBLIC + Modifier.STATIC
                + Modifier.FINAL;

        int countPublicStatics = 0;
        for (Field element : theFields) {
            String fieldName = element.getName();
            int theMods = element.getModifiers();
            if (Modifier.isPublic(theMods) && Modifier.isStatic(theMods)) {
                try {
                    Object fieldValue = element.get(null);
                    Object expectedValue = thePublicStatics.get(fieldName);
                    if (expectedValue == null) {
                        fail("Field " + fieldName + " missing!");
                    } // end
                    assertEquals("Field " + fieldName + " value mismatch: ",
                            expectedValue, fieldValue);
                    assertEquals("Field " + fieldName + " modifier mismatch: ",
                            requiredModifier, theMods);
                    countPublicStatics++;
                } catch (IllegalAccessException e) {
                    fail("Illegal access to Field " + fieldName);
                } // end try
            } // end if
        } // end for

    } // end method testPublicStatics

} // end class XAResourceTest
