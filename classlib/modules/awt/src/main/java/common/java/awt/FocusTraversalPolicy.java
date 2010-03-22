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
 * @author Dmitry A. Durnev
 */
package java.awt;


public abstract class FocusTraversalPolicy {

    Toolkit toolkit; // for locking/unlocking only

    public FocusTraversalPolicy() {
        toolkit = Toolkit.getDefaultToolkit();
    }

    public abstract Component getComponentAfter(Container a0, Component a1);

    public abstract Component getComponentBefore(Container a0, Component a1);

    public abstract Component getDefaultComponent(Container a0);

    public abstract Component getFirstComponent(Container a0);

    public Component getInitialComponent(Window window) {
        toolkit.lockAWT();
        try {
            Component defaultComp = getDefaultComponent(window);
            return defaultComp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public abstract Component getLastComponent(Container a0);

}

