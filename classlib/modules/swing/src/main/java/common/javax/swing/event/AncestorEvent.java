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
 * @author Anton Avtamonov
 */

package javax.swing.event;

import java.awt.AWTEvent;
import java.awt.Container;
import javax.swing.JComponent;

public class AncestorEvent extends AWTEvent {
    public static final int ANCESTOR_ADDED = 1;
    public static final int ANCESTOR_MOVED = 3;
    public static final int ANCESTOR_REMOVED = 2;

    private final Container ancestor;
    private final Container ancestorParent;

    public AncestorEvent(final JComponent source, final int id, final Container ancestor, final Container ancestorParent) {
        super(source, id);
        this.ancestor = ancestor;
        this.ancestorParent = ancestorParent;
    }

    public JComponent getComponent() {
        return (JComponent)getSource();
    }

    public Container getAncestorParent() {
        return ancestorParent;
    }

    public Container getAncestor() {
        return ancestor;
    }
}

