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

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>ComponentInputMap</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class ComponentInputMap extends InputMap {
    private static final long serialVersionUID = 1760753505284728053L;

    private JComponent component;

    public ComponentInputMap(JComponent component) {
        if (component == null) {
            throw new IllegalArgumentException(Messages.getString("swing.57")); //$NON-NLS-1$
        }
        this.component = component;
    }

    @Override
    public void put(KeyStroke keyStroke, Object key) {
        super.put(keyStroke, key);
        if (component != null) {
            component.componentInputMapChanged(this);
        }
    }

    @Override
    public void remove(KeyStroke keyStroke) {
        super.remove(keyStroke);
        component.componentInputMapChanged(this);
    }

    public JComponent getComponent() {
        return component;
    }

    @Override
    public void setParent(InputMap parent) {
        if (parent != null
                && (!(parent instanceof ComponentInputMap) || (((ComponentInputMap) parent)
                        .getComponent() != component))) {
            throw new IllegalArgumentException(Messages.getString("swing.4D")); //$NON-NLS-1$
        }
        super.setParent(parent);
        if (component != null) {
            component.componentInputMapChanged(this);
        }
    }

    @Override
    public void clear() {
        super.clear();
        component.componentInputMapChanged(this);
    }
}
