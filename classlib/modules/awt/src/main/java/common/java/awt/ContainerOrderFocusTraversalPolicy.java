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

package java.awt;

import java.io.Serializable;

import org.apache.harmony.awt.internal.nls.Messages;

public class ContainerOrderFocusTraversalPolicy extends FocusTraversalPolicy
        implements Serializable {
    private static final long serialVersionUID = 486933713763926351L;

    private boolean implicitDownCycleTraversal = true;

    public ContainerOrderFocusTraversalPolicy() {
    }

    protected boolean accept(Component aComp) {
        toolkit.lockAWT();
        try {
            // By default, this method will accept a Component if and only if it is
            // visible[together with parent !!!], displayable, enabled, and focusable.
            return (aComp.isVisible() && aComp.isDisplayable() &&
                    aComp.isEnabled() && aComp.isFocusable());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        toolkit.lockAWT();
        try {
            check(aContainer, aComponent);
            Container provider = findProvider(aContainer, aComponent);
            if (provider != null) {
                return getComponent(provider, aComponent, true);
            }
            Component nextComp = getComponent(aContainer, aComponent, false,
                                              true);
            return nextComp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void check(Container aContainer, Component component) {
        if (aContainer == null || component == null) {
            // awt.10B=aContainer and aComponent cannot be null
            throw new IllegalArgumentException(Messages.getString("awt.10B")); //$NON-NLS-1$
        }

        if (aContainer.isFocusCycleRoot()) {
            Component root = component.getFocusCycleRootAncestor();
            if ((root != aContainer) && (component != aContainer)) {
                // awt.10C=aContainer is not a focus cycle root of aComponent
                throw new IllegalArgumentException(Messages.getString("awt.10C")); //$NON-NLS-1$
            }
        } else if (!aContainer.isFocusTraversalPolicyProvider()) {
            // awt.10D=aContainer should be focus cycle root or focus traversal policy provider
            throw new IllegalArgumentException(Messages.getString("awt.10D")); //$NON-NLS-1$
        }
    }

    @Override
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        toolkit.lockAWT();
        try {
            check(aContainer, aComponent);
            Container provider = findProvider(aContainer, aComponent);
            if (provider != null) {
                return getComponent(provider, aComponent, false);
            }
            Component prevComp = getComponent(aContainer, aComponent, false, false);
            return prevComp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * if a Component is a child of a focus traversal policy provider, the next
     * and previous for this Component are determined using this focus traversal
     * policy provider's FocusTraversalPolicy.
     */
    private Component getComponent(Container provider,
                                   Component comp, boolean after) {
        if (provider.isFocusCycleRoot() ||
            !provider.isFocusTraversalPolicyProvider()) {

            return null;
        }
        FocusTraversalPolicy policy = provider.getFocusTraversalPolicy();
        Component nextComp = (after ? policy.getComponentAfter(provider, comp) :
                       policy.getComponentBefore(provider, comp));
        Component wrapComp = after ? policy.getFirstComponent(provider) :
                           policy.getLastComponent(provider);
        if (nextComp == wrapComp) {
            // don't wrap traversal inside providers:
            // just go to next/prev component after/before
            // provider in its root ancestor's cycle
            Container root = provider.getFocusCycleRootAncestor();
            if (root == null) {
                return null;
            }
            FocusTraversalPolicy rootPolicy = root.getFocusTraversalPolicy();
            nextComp = (after ? rootPolicy.getComponentAfter(root, provider) :
                                rootPolicy.getComponentBefore(root, provider));
        }
        return nextComp;
    }

   /*
    * Find first FTP provider between comp and its FCR container.
    * Return null if not found
    *
    */
    private Container findProvider(Container container, Component comp) {
        if (!container.isFocusCycleRoot()) {
            // prevent endless loop:
            // if container is already a provider don't
            // call its policy again
            return null;
        }
        Component curComp = comp;
        while ((curComp != null)) {
            Container parent = curComp.getRealParent();
            if ((parent != null) && parent.isFocusTraversalPolicyProvider()) {
                return parent;
            }
            curComp = parent;
        }
        return null;
    }

    @Override
    public Component getDefaultComponent(Container container) {
        toolkit.lockAWT();
        try {
            return getFirstComponent(container);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Component getFirstComponent(Container container) {
        toolkit.lockAWT();
        try {
            if (container == null) {
                // awt.10E=focusCycleRoot cannot be null
                throw new IllegalArgumentException(Messages.getString("awt.10E")); //$NON-NLS-1$
            }
            Component firstComp = getComponent(container, container, true, true);
            return firstComp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * return first acceptable component after/before[forward is true/false]
     * comp[ or comp itself if "include" is true] in "container order"
     */
    private Component getComponent(Container container, Component comp,
                                   boolean include, boolean forward) {
        Component curComp = comp;

        if (forward) {
            Component defComp = getFCRDefComp(curComp, container);
            if (defComp != null) {
                return defComp;
            }
        }
        if (!include) {
            curComp = oneStep(container, curComp, forward, true);
            if (forward) {
                Component defComp = getDefComp(curComp, container);
                if (defComp != null) {
                    return defComp;
                }
            }
        }
        boolean stop = false;
        while (!stop && (curComp != null) && !accept(curComp)) {
            if (forward) {
                Component defComp = getFCRDefComp(curComp, container);
                if (defComp != null) {
                    return defComp;
                }
            }
            curComp = oneStep(container, curComp, forward, !include);
            if (forward) {
                Component defComp = getDefComp(curComp, container);
                if (defComp != null) {
                    return defComp;
                }
            }
            stop = (curComp == comp);// stop if comp is traversed again

        }

        if (curComp == null) {
            return null;
        }
        return accept(curComp) ? curComp : null;
    }

    /**
     * @param comp
     * @param root
     * @return default component given by comp's FTP if comp is FTP provider and
     *         is not same as root, null otherwise
     */
    private Component getDefComp(Component comp, Container root) {
        if (!(comp instanceof Container)) {
            return null;
        }
        Container cont = (Container) comp;
        boolean isProvider = (!cont.isFocusCycleRoot() &&
                               cont.isFocusTraversalPolicyProvider());
        if (isProvider && (cont != root)) {
            return cont.getFocusTraversalPolicy().getDefaultComponent(cont);
        }
        return null;
    }

    /**
     * @param comp
     * @param root
     * @return default component given by comp's FTP if comp is FCR and implicit
     *         down cycle traversal is true and comp is not same as root, null
     *         otherwise
     */
    private Component getFCRDefComp(Component comp, Container root) {
        if (!(comp instanceof Container)) {
            return null;
        }
        Container cont = (Container) comp;
        boolean isFCR = cont.isFocusCycleRoot();
        boolean traverseFCR = (isFCR && (cont != root) &&
                               getImplicitDownCycleTraversal());
        if (traverseFCR) {
            return cont.getFocusTraversalPolicy().getDefaultComponent(cont);
        }
        return null;

    }

    private int getInitialIndex(Container parent, boolean forward) {
        return (forward ? 0 : (parent.getComponentCount() - 1));
    }

    /**
     * Make one step forward/backward in
     * container order from comp, do
     * not take any conditions, such as
     * accept(), into consideration. Don't
     * go into FCRs or FTP providers.
     * @param root
     * @param comp
     * @param forward
     * @return
     */
    private Component oneStep(Container root, Component comp,
                              boolean forward, boolean cycle) {
        if (root == null || comp == null) {
            return null;
        }
        Container parent = comp.getRealParent();
        if (parent == null) {
            parent = (Container) comp;
        }

        boolean same = (parent == comp);
        int maxIdx = parent.getComponentCount() - 1;
        if (maxIdx < 0) {
            return (same ? comp : null);
        }

        if (forward && (comp instanceof Container)) {
            Component firstComp = goDown(root, (Container)comp);
            if (firstComp != null) {
                return firstComp;
            }
        }

        int idx = parent.getComponentIndex(comp);

        if (idx < 0) {
            // if going back and actual parent is null - wrap traversal
            parent = null;
        }
        idx += (forward ? 1 : -1);
        Component nextComp = comp;
        if ((parent != null) && checkIndex(parent, idx)) {
            nextComp = parent.getComponent(idx);
            if (!forward && (nextComp instanceof Container)) {
                // go back & down into container
                nextComp = getCompInContainer((Container)nextComp, root);
            }
        } else {
            // go up to parent container
            nextComp = forward ? getCompAfterContainer(parent) : parent;
        }

        if (!root.isAncestorOf(nextComp) && (root != nextComp)) {
            // wrap if trying to get out of root:
            nextComp = cycle ? wrapTraversal(root, forward) : /*root*/null;
        }
        return nextComp;

    }

    private Component goDown(Container root, Container cont) {
        int idx = getInitialIndex(cont, true);
        if (!canGoDown(root, cont)) {
            idx = -1;// don't try to go into FCRs or FTPPs(?)
            // treat them just like containers with
            // no components inside
        }
        if (checkIndex(cont, idx)) {
            // go down into container first
            return cont.getComponent(idx);
        }
        return null;
    }

    private boolean canGoDown(Container root, Container cont) {
        if ((root == null) || (cont == null)) {
            return false;
        }
        return ((!cont.isFocusCycleRoot() &&
                !cont.isFocusTraversalPolicyProvider()) ||
                (cont == root));
    }

    /**
     * Find last component inside container
     * when traversing backward[going down into containers].
     * Skip focus cycle roots which are not
     * same as root.
     * @param container
     * @return
     */
    private Component getCompInContainer(Container container, Container root) {
        Component lastComp = container;
        while (lastComp instanceof Container) {
            Container cont = (Container) lastComp;
            if (!canGoDown(root, cont)) {
                break;
            }
            int idx = getInitialIndex(cont, false);
            if (!checkIndex(cont, idx)) {
                break;
            }
            lastComp = cont.getComponent(idx);
        }

        return lastComp;
    }

    /**
     * Find first component after
     * container[go up & forward]
     * @param parent
     * @return
     */
    private Component getCompAfterContainer(Container container) {
        Container parent = container;
        while (parent != null) {
            parent = container.getRealParent();
            Component nextComp = getComp(parent, container, true);
            if (nextComp != null) {
                return nextComp;
            }
            container = parent;
        }

        return null;
    }

    /**
     * Get component before/after "comp" in container
     * "parent". If there's no such component in
     * "parent" (comp was first or last) - return null
     * @param parent
     * @param container
     * @return
     */
    private Component getComp(Container parent, Component comp,
                              boolean forward) {
        if ((parent == null) || (comp == null)) {
            return null;
        }
        int idx = parent.getComponentIndex(comp);
        if (idx < 0) {
            return null;
        }
        idx += forward ? 1 : -1;
        if (checkIndex(parent, idx)) {
            return parent.getComponent(idx);
        }
        return null;
    }

    private Component wrapTraversal(Container container, boolean forward) {
        Component comp = forward ? getFirstComponent(container) :
                                   getLastComponent(container);
        return comp;
    }

    private boolean checkIndex(Container container, int idx) {
        return ((idx >= 0) && (idx < container.getComponentCount()));
    }

    @Override
    public Component getLastComponent(Container container) {
        toolkit.lockAWT();
        try {
            if (container == null) {
                // awt.10E=focusCycleRoot cannot be null
                throw new IllegalArgumentException(Messages.getString("awt.10E")); //$NON-NLS-1$
            }
            int count = container.getComponentCount();
            if ( count <= 0) {
                return accept(container) ? container : null;
            }
            Component lastComp = getComponent(container,
                                              getCompInContainer(container, container),
                                              true, false);
            return lastComp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean getImplicitDownCycleTraversal() {
        toolkit.lockAWT();
        try {
            return implicitDownCycleTraversal;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setImplicitDownCycleTraversal(boolean value) {
        toolkit.lockAWT();
        try {
            implicitDownCycleTraversal = value;
        } finally {
            toolkit.unlockAWT();
        }
    }

}

