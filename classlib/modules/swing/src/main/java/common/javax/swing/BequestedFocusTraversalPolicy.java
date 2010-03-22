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

package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

import org.apache.harmony.x.swing.internal.nls.Messages;

class BequestedFocusTraversalPolicy extends FocusTraversalPolicy {
    private final FocusTraversalPolicy ancestor;

    private final Component fixedComponent;

    private final Component fixedNextComponent;

    /**
     * Creates <code>FocusTraversalPolicy</code> that inherits all values
     * returned by existing <code>FocusTraversalPolicy</code> and overlaps
     * only two of them: value returned by <code>getComponentAfter()</code> for
     * <code>fixedComponent</code> and <code>getComponentBefore()</code> for
     * <code>fixedNextComponent</code>.
     * @throws <code>IllegalArgumentException</code> if <code>ancestor</code> is <code>null</code>
     */
    public BequestedFocusTraversalPolicy(final FocusTraversalPolicy ancestor,
            final Component fixedComponent, final Component fixedNextComponent) {
        super();
        this.ancestor = ancestor;
        if (this.ancestor == null) {
            throw new IllegalArgumentException(Messages.getString("swing.06")); //$NON-NLS-1$
        }
        this.fixedComponent = fixedComponent;
        this.fixedNextComponent = fixedNextComponent;
    }

    /**
     * returns <code>fixedNextComponent</code> for <code>fixedComponent</code> or
     * delegates call to <code>ancestor</code>
     */
    @Override
    public Component getComponentAfter(final Container container, final Component c) {
        if (c == fixedComponent) {
            return fixedNextComponent;
        }
        return ancestor.getComponentAfter(container, c);
    }

    /**
     * returns <code>fixedComponent</code> for <code>fixedNextComponent</code> or
     * delegates call to <code>ancestor</code>
     */
    @Override
    public Component getComponentBefore(final Container container, final Component c) {
        if (c == fixedNextComponent) {
            return fixedComponent;
        }
        return ancestor.getComponentBefore(container, c);
    }

    /**
     * delegates call to <code>ancestor</code>
     */
    @Override
    public Component getDefaultComponent(final Container container) {
        return ancestor.getDefaultComponent(container);
    }

    /**
     * delegates call to <code>ancestor</code>
     */
    @Override
    public Component getFirstComponent(final Container container) {
        return ancestor.getFirstComponent(container);
    }

    /**
     * delegates call to <code>ancestor</code>
     */
    @Override
    public Component getLastComponent(final Container container) {
        return ancestor.getLastComponent(container);
    }
}
