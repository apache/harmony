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

package org.apache.harmony.beans.tests.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Assert;

public class TInspectorCluster {

    public static final String ANCESTOR_STRING = "ancestor";

    public static final String OFFSPRING_STRING = "offspring";

    public static final boolean ANCESTOR_BOOLEAN = Boolean.FALSE;

    public static final boolean OFFSPRING_BOOLEAN = Boolean.TRUE;

    public static final char ANCESTOR_CHARACTER = 'A';

    public static final char OFFSPRING_CHARACTER = 'O';

    public static final short ANCESTOR_SHORT = 2;

    public static final short OFFSPRING_SHORT = -2;

    public static final int ANCESTOR_INTEGER = Integer.MIN_VALUE;

    public static final int OFFSPRING_INTEGER = Integer.MAX_VALUE;

    public static final long ANCESTOR_LONG = 453298580984320l;

    public static final long OFFSPRING_LONG = -453298580984320l;

    public static final float ANCESTOR_FLOAT = 0.5f;

    public static final float OFFSPRING_FLOAT = -0.5f;

    public static final double ANCESTOR_DOUBLE = 0.12;

    public static final double OFFSPRING_DOUBLE = -0.12;

    public static final MockA ANCESTOR_OBJECT = new MockA();

    public static final MockB OFFSPRING_OBJECT = new MockB();

    public static final List<MockA> ANCESTOR_OBJECT_LIST = new ArrayList<MockA>();

    public static final List<MockB> OFFSPRING_OBJECT_LIST = new ArrayList<MockB>();

    private static String calledMethodName = null;

    private static Object calledMethodResult = null;

    private static Object[] calledmethodArguments = null;

    static {
        ANCESTOR_OBJECT_LIST.add(new MockA());
        OFFSPRING_OBJECT_LIST.add(new MockB());
    }

    public interface Ancestor {
    }

    public interface Offspring extends Ancestor {
    }

    public interface Visitor<T> {

        public T visit(Ancestor o);

    }

    /*
     * check whether the right method is called
     */
    public static void assertMethodCalled(String methodName,
            Object[] arguments, Object expectResult) {
        Assert.assertEquals(methodName, calledMethodName);
        Assert.assertTrue(Arrays.equals(arguments, calledmethodArguments));
        Assert.assertEquals(expectResult, calledMethodResult);
        reset();
    }

    private static void reset() {
        calledMethodName = null;
        calledMethodResult = null;
        calledmethodArguments = null;
    }

    public static class StringInspector implements Visitor<String> {

        public String visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_STRING;
            return null;
        }

        public String visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_STRING;
            return null;
        }

    }

    public static class BooleanInspector implements Visitor<Boolean> {

        public Boolean visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_BOOLEAN;
            return null;
        }

        public Boolean visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_BOOLEAN;
            return null;
        }

    }

    public static class CharacterInspector implements Visitor<Character> {

        public Character visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_CHARACTER;
            return null;
        }

        public Character visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_CHARACTER;
            return null;
        }

    }

    public static class ShortInspector implements Visitor<Short> {

        public Short visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_SHORT;
            return null;
        }

        public Short visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_SHORT;
            return null;
        }

    }

    public static class IntegerInspector implements Visitor<Integer> {

        public Integer visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_INTEGER;
            return null;
        }

        public Integer visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_INTEGER;
            return null;
        }

    }

    public static class LongInspector implements Visitor<Long> {

        public Long visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_LONG;
            return null;
        }

        public Long visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_LONG;
            return null;
        }

    }

    public static class FloatInspector implements Visitor<Float> {

        public Float visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_FLOAT;
            return null;
        }

        public Float visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_FLOAT;
            return null;
        }

    }

    public static class DoubleInspector implements Visitor<Double> {

        public Double visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_DOUBLE;
            return null;
        }

        public Double visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_DOUBLE;
            return null;
        }

    }

    public static class ObjectInspector implements Visitor<Object> {

        public Object visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_OBJECT;
            return null;
        }

        public Object visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_OBJECT;
            return null;
        }

    }

    public static class ObjectListInspector implements Visitor<List> {

        public List visit(Ancestor o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = ANCESTOR_OBJECT_LIST;
            return null;
        }

        public List visit(Offspring o) {
            calledMethodName = "visit";
            calledmethodArguments = new Object[] { o };
            calledMethodResult = OFFSPRING_OBJECT_LIST;
            return null;
        }
    }

}

class MockA {

    public static final String NAME = "A";

}

class MockB {

    public static final String NAME = "B";

}
