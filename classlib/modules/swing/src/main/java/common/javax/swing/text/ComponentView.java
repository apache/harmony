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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.HashMap;

import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class ComponentView extends View {

    private static final class ComponentViewLayout implements LayoutManager {
        private static final HashMap componentViews = new HashMap();
        
        public void layoutContainer(final Container c) {
            for (int i = 0; i < c.getComponentCount(); i++) {
                Component child = c.getComponent(i);
                View componentView = (View)componentViews.get(child);
                if (componentView == null) {
                    break;
                }
                Shape bounds1 = null;
                Shape bounds2 = null;
                try {
                    final JTextComponent textComponent = (JTextComponent)c;
                    bounds1 = textComponent.modelToView(componentView.getStartOffset());
                    bounds2 = textComponent.modelToView(componentView.getEndOffset());
                } catch (BadLocationException e) {
                }
                if (bounds1 == null || bounds2 == null) {
                    return;
                }
                Rectangle bounds = ((Rectangle)bounds1).union((Rectangle)bounds2);
                child.setBounds(bounds.x, bounds.y, bounds.width,
                                bounds.height);
            }
        }
        
        public Dimension minimumLayoutSize(Container c) {
            return null;
        }

        public Dimension preferredLayoutSize(Container c) {
            return null;
        }

        public void addLayoutComponent(String name, Component c) {
        }

        public void removeLayoutComponent(Component c) {
        }
    }

    private static final int EMPTY_SPAN = 0;
    private Component component;

    public ComponentView(final Element element) {
        super(element);
    }

    public final Component getComponent() {
        return component;
    }

    public float getPreferredSpan(final int axis) {
        isAxisValid(axis);
        if (getParent() != null) {
            if (axis == View.X_AXIS) {
                return component.getPreferredSize().width + 2;
            }
            return component.getPreferredSize().height;
        }
        return EMPTY_SPAN;
    }

    public float getMinimumSpan(final int axis) {
        isAxisValid(axis);
        if (getParent() != null) {
            if (axis == View.X_AXIS) {
                return component.getMinimumSize().width + 2;
            }
            return component.getMinimumSize().height;
        }
        return EMPTY_SPAN;
    }

    public float getMaximumSpan(final int axis) {
        isAxisValid(axis);
        if (getParent() != null) {
            if (axis == View.X_AXIS) {
                return component.getMaximumSize().width + 2;
            }
            return component.getMaximumSize().height;
        }
        return EMPTY_SPAN;
    }

    public float getAlignment(final int axis) {
        if (component != null) {
            if (axis == View.X_AXIS) {
                return component.getAlignmentX();
            }
            if (axis == View.Y_AXIS) {
                return component.getAlignmentY();
            }
        }
        return View.ALIGN_CENTER;
    }

    public Shape modelToView(final int pos, final Shape shape,
                             final Bias bias) throws BadLocationException {
        return TextUtils.modelToIconOrComponentView(this, pos, shape, bias);
    }

    public int viewToModel(final float x,
                           final float y,
                           final Shape shape,
                           final Bias[] biasReturn) {

        final Rectangle bounds = shape.getBounds();
        if (x > bounds.width / 2 + bounds.x - 1) {
            biasReturn[0] = Position.Bias.Backward;
            return getEndOffset();
        }
        biasReturn[0] = Position.Bias.Forward;
        return getStartOffset();
    }

    public void setParent(final View parent) {
        if (parent == null) {
            if (component != null && component.getParent() != null) {
                component.getParent().remove(component);
            }
            super.setParent(parent);
        } else {
            if (getParent() == null) {
                super.setParent(parent);
                if (component == null) {
                    component = createComponent();
                    ComponentViewLayout.componentViews.put(component, this);
                }
            } 
            final Container container = getContainer();
            if (container != null) {
                container.add(component);
                if (container.getLayout() == null) {
                    container.setLayout(new ComponentViewLayout());
                }
            }
        }
    }

    public void paint(final Graphics g, final Shape shape) {
    }

    protected Component createComponent() {
        final AttributeSet attrs = getAttributes();
        return attrs == null ? null : StyleConstants.getComponent(attrs);
    }

    private void isAxisValid(final int axis) {
        if (axis != X_AXIS && axis != Y_AXIS) {
            throw new IllegalArgumentException(Messages.getString("swing.00", axis)); //$NON-NLS-1$
        }
    }
    
    
}
