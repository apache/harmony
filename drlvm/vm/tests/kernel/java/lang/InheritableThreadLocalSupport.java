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
 * @author Elena Semukhina
 */

package java.lang;

public class InheritableThreadLocalSupport extends Thread {
    
    static Object localO = null;
    static Integer localI = null;
    static Integer newLocalI = new Integer(10);

    public boolean parentInheritableLocalObjectOK = false;
    public boolean childInheritableLocalObjectOK = false;
    public boolean grandChildInheritableLocalObjectOK = false;
    public boolean parentInheritableLocalIntegerOK = false;
    public boolean childInheritableLocalIntegerOK = false;
    public boolean grandChildInheritableLocalIntegerOK = false;
    
    static InheritableThreadLocal<Object> itlo = new InheritableThreadLocal<Object>() {

        protected synchronized Object initialValue() {
            localO = new Object();
            return localO;
        }
    };

    static InheritableThreadLocal<Integer> itli = new InheritableThreadLocal<Integer>() {

        protected synchronized Integer initialValue() {
            localI = new Integer(5);
            return localI;
        }

        protected synchronized Integer childValue(Integer parentValue) {
            return new Integer(parentValue.intValue() * 2);
        }
    };

    public void run() {
        Object valO = itlo.get();
        if (valO.equals(localO)) {
            parentInheritableLocalObjectOK = true;
        }
        Integer valI = itli.get();
        if (valI.equals(localI)) {
            parentInheritableLocalIntegerOK = true;
        }
        Child child = new Child();
        child.start();
        try {
            child.join();
        } catch (InterruptedException ie) {
            return;
        }
        childInheritableLocalObjectOK = child.childInheritableLocalObjectOK;
        grandChildInheritableLocalObjectOK = child.grandChildInheritableLocalObjectOK;
        childInheritableLocalIntegerOK = child.childInheritableLocalObjectOK;
        grandChildInheritableLocalIntegerOK = child.grandChildInheritableLocalObjectOK;
    }
}

    class Child extends InheritableThreadLocalSupport {

        public void run() {
            Object valO = itlo.get();
            if (valO.equals(localO)) {
                childInheritableLocalObjectOK = true;
            }
            Integer valI = itli.get();
            if (valI.intValue() == newLocalI.intValue()) {
                childInheritableLocalIntegerOK = true;
            }
            GrandChild gChild = new GrandChild();
            gChild.start();
            try {
                gChild.join();
            } catch (InterruptedException ie) {
                return;
            }
            grandChildInheritableLocalObjectOK = gChild.grandChildInheritableLocalObjectOK;
            grandChildInheritableLocalIntegerOK = gChild.grandChildInheritableLocalIntegerOK;
        }
    }

    class GrandChild extends InheritableThreadLocalSupport {

        public void run() {
            Object valO = itlo.get();
            if (valO.equals(localO)) {
                grandChildInheritableLocalObjectOK = true;
            }
            Integer valI = itli.get();
            if (valI.equals(newLocalI)) {
                grandChildInheritableLocalIntegerOK = true;
            }
        }
    }

