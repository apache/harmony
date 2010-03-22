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

package org.apache.harmony.beans.tests.support.beancontext;

import java.util.ArrayList;
import java.util.List;
import junit.framework.Assert;

/**
 * Record method invocation input and output.
 */
public class MethodInvocationRecords extends Assert {

    public static final Object IGNORE = "IGNORE";

    private final List<List<Object>> records = new ArrayList<List<Object>>();

    private int assertIndex = 0;

    public void clear() {
        records.clear();
        assertIndex = 0;
    }

    public void add(String methodName, Object returnValue) {
        List<Object> rec = new ArrayList<Object>();
        rec.add(methodName);
        rec.add(returnValue);
        records.add(rec);
    }

    public void add(String methodName, Object arg1, Object returnValue) {
        List<Object> rec = new ArrayList<Object>();
        rec.add(methodName);
        rec.add(arg1);
        rec.add(returnValue);
        records.add(rec);
    }

    public void add(String methodName, Object arg1, Object arg2,
            Object returnValue) {
        List<Object> rec = new ArrayList<Object>();
        rec.add(methodName);
        rec.add(arg1);
        rec.add(arg2);
        rec.add(returnValue);
        records.add(rec);
    }

    public void add(String methodName, Object arg1, Object arg2, Object arg3,
            Object returnValue) {
        List<Object> rec = new ArrayList<Object>();
        rec.add(methodName);
        rec.add(arg1);
        rec.add(arg2);
        rec.add(arg3);
        rec.add(returnValue);
        records.add(rec);
    }

    public void add(String methodName, Object arg1, Object arg2, Object arg3,
            Object arg4, Object returnValue) {
        List<Object> rec = new ArrayList<Object>();
        rec.add(methodName);
        rec.add(arg1);
        rec.add(arg2);
        rec.add(arg3);
        rec.add(arg4);
        rec.add(returnValue);
        records.add(rec);
    }

    public void assertRecord(String methodName, Object returnValue) {
        List<Object> rec = records.get(assertIndex++);
        int count = 0;
        assertEquals(methodName, rec.get(count++));
        if (returnValue != IGNORE) {
            assertEquals(returnValue, rec.get(count++));
        } else {
            count++;
        }
        assertEquals(count, rec.size());
    }

    public void assertRecord(String methodName, Object arg1, Object returnValue) {
        List<Object> rec = records.get(assertIndex++);
        int count = 0;
        assertEquals(methodName, rec.get(count++));
        if (arg1 != IGNORE) {
            assertEquals(arg1, rec.get(count++));
        } else {
            count++;
        }
        if (returnValue != IGNORE) {
            assertEquals(returnValue, rec.get(count++));
        } else {
            count++;
        }
        assertEquals(count, rec.size());
    }

    public void assertRecord(String methodName, Object arg1, Object arg2,
            Object returnValue) {
        List<Object> rec = records.get(assertIndex++);
        int count = 0;
        assertEquals(methodName, rec.get(count++));
        if (arg1 != IGNORE) {
            assertEquals(arg1, rec.get(count++));
        } else {
            count++;
        }
        if (arg2 != IGNORE) {
            assertEquals(arg2, rec.get(count++));
        } else {
            count++;
        }
        if (returnValue != IGNORE) {
            assertEquals(returnValue, rec.get(count++));
        } else {
            count++;
        }
        assertEquals(count, rec.size());
    }

    public void assertRecord(String methodName, Object arg1, Object arg2,
            Object arg3, Object returnValue) {
        List<Object> rec = records.get(assertIndex++);
        int count = 0;
        assertEquals(methodName, rec.get(count++));
        if (arg1 != IGNORE) {
            assertEquals(arg1, rec.get(count++));
        } else {
            count++;
        }
        if (arg2 != IGNORE) {
            assertEquals(arg2, rec.get(count++));
        } else {
            count++;
        }
        if (arg3 != IGNORE) {
            assertEquals(arg3, rec.get(count++));
        } else {
            count++;
        }
        if (returnValue != IGNORE) {
            assertEquals(returnValue, rec.get(count++));
        } else {
            count++;
        }
        assertEquals(count, rec.size());
    }

    public void assertRecord(String methodName, Object arg1, Object arg2,
            Object arg3, Object arg4, Object returnValue) {
        List<Object> rec = records.get(assertIndex++);
        int count = 0;
        assertEquals(methodName, rec.get(count++));
        if (arg1 != IGNORE) {
            assertEquals(arg1, rec.get(count++));
        } else {
            count++;
        }
        if (arg2 != IGNORE) {
            assertEquals(arg2, rec.get(count++));
        } else {
            count++;
        }
        if (arg3 != IGNORE) {
            assertEquals(arg3, rec.get(count++));
        } else {
            count++;
        }
        if (arg4 != IGNORE) {
            assertEquals(arg4, rec.get(count++));
        } else {
            count++;
        }
        if (returnValue != IGNORE) {
            assertEquals(returnValue, rec.get(count++));
        } else {
            count++;
        }
        assertEquals(count, rec.size());
    }

    public void assertEndOfRecords() {
        assertEquals(assertIndex, records.size());
    }

    public String getMethodName() {
        List<Object> rec = records.get(assertIndex);
        return (String) rec.get(0);
    }

    public Object getArg(int i) {
        List<Object> rec = records.get(assertIndex);
        return rec.get(i + 1);
    }

    public Object getReturnValue(int i) {
        List<Object> rec = records.get(assertIndex);
        return rec.get(rec.size() - 1);
    }

    @Override
    public String toString() {
        return records.toString();
    }

}
