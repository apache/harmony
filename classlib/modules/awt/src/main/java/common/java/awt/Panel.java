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

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

public class Panel extends Container implements Accessible {

    private static final long serialVersionUID = -2728009084054400034L;

    protected  class AccessibleAWTPanel extends AccessibleAWTContainer {

        private static final long serialVersionUID = -6409552226660031050L;

        @Override
        public AccessibleRole getAccessibleRole() {
            toolkit.lockAWT();
            try {
                return AccessibleRole.PANEL;
            } finally {
                toolkit.unlockAWT();
            }
        }

    }

    public Panel() {
        this(new FlowLayout());
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Panel(LayoutManager layout) {
        toolkit.lockAWT();
        try {
            super.setLayout(layout);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            return super.getAccessibleContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTPanel();
    }

    @Override
    ComponentBehavior createBehavior() {        
        return GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance() ? 
                new LWBehavior(this) : 
                new HWBehavior(this) ;
    }

    @Override
    String autoName() {
        return ("panel" + toolkit.autoNumber.nextPanel++); //$NON-NLS-1$
    }
}

