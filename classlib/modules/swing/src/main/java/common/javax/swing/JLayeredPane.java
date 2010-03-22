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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Hashtable;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * <p>
 * <i>JLabel</i> implements a container with support for layers.
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JLayeredPane extends JComponent implements Accessible {
    private static final long serialVersionUID = -2657754894035116214L;

    private static final int DELAULT_LAYER_NUMBER = 0;

    private static final int PALETTE_LAYER_NUMBER = 100;

    private static final int MODAL_LAYER_NUMBER = 200;

    private static final int POPUP_LAYER_NUMBER = 300;

    private static final int DRAG_LAYER_NUMBER = 400;

    private static final int FRAME_CONTENT_LAYER_NUMBER = -30000;

    public static final Integer DEFAULT_LAYER;

    public static final Integer PALETTE_LAYER;

    public static final Integer MODAL_LAYER;

    public static final Integer POPUP_LAYER;

    public static final Integer DRAG_LAYER;

    public static final Integer FRAME_CONTENT_LAYER;

    public static final String LAYER_PROPERTY = "layeredContainerLayer";
    static {
        DEFAULT_LAYER = new Integer(DELAULT_LAYER_NUMBER);
        PALETTE_LAYER = new Integer(PALETTE_LAYER_NUMBER);
        MODAL_LAYER = new Integer(MODAL_LAYER_NUMBER);
        POPUP_LAYER = new Integer(POPUP_LAYER_NUMBER);
        DRAG_LAYER = new Integer(DRAG_LAYER_NUMBER);
        FRAME_CONTENT_LAYER = new Integer(FRAME_CONTENT_LAYER_NUMBER);
    }

    /**
     * Implements accessibility support for <code>JLayeredPane</code>
     */
    protected class AccessibleJLayeredPane extends AccessibleJComponent {
        private static final long serialVersionUID = 3492700363505144784L;

        /**
         * Constructs new <code>AccessibleJLayeredPane</code>.
         */
        protected AccessibleJLayeredPane() {
            super();
        }

        /**
         * Returns the accessible role of the object.
         *
         * @return <code>AccessibleRole</code> that describes the accessible
         *         role of the object
         */
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LAYERED_PANE;
        }
    }

    /**
     *  The hash table used to store layers for components.
     */
    private Hashtable<Component, Integer> componentToLayer;

    /**
     * Constructs new <code>JLayeredPane</code>
     */
    public JLayeredPane() {
        super();
    }

    /**
     * Adds the specified component to the container using the specified
     * index and the specified constraints.
     *
     * @param comp component to add
     * @param constraints constraints to be applied (layer)
     * @param index position of the component in the layer, where
     *        -1 means the bottommost position and 0 means the topmost position
     */
    @Override
    protected void addImpl(final Component comp, final Object constraints, final int index) {
        super.remove(comp);
        int layer = DELAULT_LAYER_NUMBER;
        Object newConstraints = constraints;
        if (constraints == null) {
            layer = getLayer(comp);
        } else if (constraints instanceof Integer) {
            layer = ((Integer) constraints).intValue();
            newConstraints = null;
            rememberLayerForComponent(comp, layer);
        }
        super.addImpl(comp, newConstraints, insertIndexForLayer(layer, index));
        /*
         * Unlike other containers, JLayeredPane has to validate added
         * components immediatelly (this is used, for example, in tool tips
         * implementation)
         */
        comp.validate();
    }

    /**
     * Returns <code>AccessibleContext</code> for <code>JLayeredPane</code>.
     *
     * @return <code>AccessibleContext</code> associated with this layered pane
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJLayeredPane();
        }
        return accessibleContext;
    }

    /**
     * Returns the hash table that maps components to layers.
     *
     * @return the hash table that maps components to layers
     */
    //XXX: 1.5 migration: uncomment
    //protected Hashtable<Component,Integer> getComponentToLayer()
    protected Hashtable<java.awt.Component, java.lang.Integer> getComponentToLayer() {
        if (componentToLayer == null) {
            componentToLayer = new Hashtable<java.awt.Component, java.lang.Integer>();
        }
        return componentToLayer;
    }

    /**
     * Returns string representation of this layered pane.
     *
     * @return string representation of this layered pane
     */
    @Override
    protected String paramString() {
        return super.paramString();
    }

    /**
     * Returns the object that represents the specified layer.
     *
     * @param layer the specified layer
     *
     * @return the object that represents the specified layer
     */
    protected Integer getObjectForLayer(final int layer) {
        // TODO: replace with Integer.valueOf(layer) after migrating to 1.5
        switch (layer) {
            case DELAULT_LAYER_NUMBER:
                return DEFAULT_LAYER;
            case PALETTE_LAYER_NUMBER:
                return PALETTE_LAYER;
            case MODAL_LAYER_NUMBER:
                return MODAL_LAYER;
            case POPUP_LAYER_NUMBER:
                return POPUP_LAYER;
            case DRAG_LAYER_NUMBER:
                return DRAG_LAYER;
            case FRAME_CONTENT_LAYER_NUMBER:
                return FRAME_CONTENT_LAYER;
        }
        return new Integer(layer);
    }

    /**
     * Paints the layered pane within the specified graphic context.
     *
     * @param g the graphic context
     */
    @Override
    public void paint(final Graphics g) {
        // JLayeredPane doesn't have UI,
        // it has to paint its background by itself
        if (ui == null && isOpaque()) {
            // paint background
            Color savedColor = g.getColor();
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(savedColor);
        }
        super.paint(g);
    }

    /**
     * Sets the layer and the position for the specified component.
     *
     * @param c the component to set layer to
     * @param layer the layer to set
     * @param position the position to set, 0 means the topmost position and
     *        -1 means the bottommost position
     */
    public void setLayer(final Component c, final int layer, final int position) {
        int index = getIndexOf(c);
        if (index == -1 || index == insertIndexForLayer(layer, position)) {
            rememberLayerForComponent(c, layer);
            return;
        }
        addImpl(c, getObjectForLayer(layer), position);
    }

    /**
     * Sets the layer for the specified component.
     *
     * @param c the component to set layer to
     * @param layer the layer to set
     */
    public void setLayer(final Component c, final int layer) {
        setLayer(c, layer, -1);
    }

    /**
     * Sets the position of the component inside its layer.
     * 0 means the topmost position and -1 means the bottommost position.
     *
     * @param c the component to move
     * @param position the position to set
     */
    public void setPosition(final Component c, final int position) {
        int layer = getLayer(c);
        int index = getIndexOf(c);
        if (index == -1) {
            // do nothing if c is not in the container
            return;
        }
        setLayer(c, layer, position);
    }

    /**
     * Moves the component to the top of its layer (position 0);
     *
     * @param c the component to move
     */
    public void moveToFront(final Component c) {
        setPosition(c, 0);
    }

    /**
     * Moves the component to the bottom of its layer (position -1);
     *
     * @param c the component to move
     */
    public void moveToBack(final Component c) {
        setPosition(c, -1);
    }

    /**
     *
     * @param c
     *
     * @return position of component c in its layer
     *         -1 if c is not in the containter
     */
    public int getPosition(final Component c) {
        int index = getIndexOf(c);
        int layer = getLayer(c);
        int pos = -1;
        for (; index >= 0 && getLayer(getComponent(index)) == layer; index--) {
            pos++;
        }
        return pos;
    }

    /**
     *
     * @param c
     *
     * @return layer of component c
     */
    public int getLayer(final Component c) {
        Object layer = getComponentToLayer().get(c);
        if (layer != null) {
            return ((Integer) layer).intValue();
        }
        if (c instanceof JComponent) {
            return getLayer((JComponent) c);
        }
        return 0;
    }

    public int getIndexOf(final Component c) {
        return getComponentZOrder(c);
    }

    public Component[] getComponentsInLayer(final int layer) {
        int size = getComponentCountInLayer(layer);
        Component[] result = new Component[size];
        if (size == 0) {
            return result;
        }
        int i = insertIndexForLayer(layer, 0);
        int j = 0;
        for (; i < getComponentCount() && layer == getLayer(getComponent(i)); i++, j++) {
            result[j] = getComponent(i);
        }
        return result;
    }

    protected int insertIndexForLayer(final int layer, final int position) {
        return insertIndexForLayer(layer, position, 0);
    }

    private int insertIndexForLayer(final int layer, final int position, final int startPosition) {
        assert startPosition == 0 || getLayer(getComponent(startPosition - 1)) > layer : "startPosition in the middle of the current layer";
        if (getComponentCount() == 0) {
            return 0;
        }
        // the bottommost position of layer n
        // is equivalent to topmost position of level n-1
        int adjustedPosition = position;
        int adjustedLayer = layer;
        if (position == -1) {
            adjustedPosition = 0;
            adjustedLayer = layer - 1;
        }
        int result = startPosition;
        // looking position depending on layer
        for (; result < getComponentCount(); result++) {
            if (getLayer(getComponent(result)) <= adjustedLayer) {
                break;
            }
        }
        // looking for position depending on index in layer
        for (; result < getComponentCount() && adjustedPosition > 0; result++, adjustedPosition--) {
            if (getLayer(getComponent(result)) != adjustedLayer) {
                break;
            }
        }
        return result;
    }

    @Override
    public void remove(final int index) {
        Component comp = getComponent(index);
        getComponentToLayer().remove(comp);
        super.remove(index);
    }

    @Override
    public void removeAll() {
        for (int i = getComponentCount() - 1; i >= 0; i--) {
            remove(i);
        }
    }

    public int getComponentCountInLayer(final int layer) {
        if (layer < lowestLayer() || layer > highestLayer()) {
            return 0;
        }
        int start = insertIndexForLayer(layer, 0);
        return insertIndexForLayer(layer, -1, start) - start;
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return getComponentCount() <= 1;
    }

    public int lowestLayer() {
        if (getComponentCount() == 0) {
            return 0;
        }
        return getLayer(getComponent(getComponentCount() - 1));
    }

    public int highestLayer() {
        if (getComponentCount() == 0) {
            return 0;
        }
        return getLayer(getComponent(0));
    }

    public static JLayeredPane getLayeredPaneAbove(final Component c) {
        return (JLayeredPane) SwingUtilities.getAncestorOfClass(JLayeredPane.class, c);
    }

    public static void putLayer(final JComponent c, final int layer) {
        Integer l = (Integer) c.getClientProperty(LAYER_PROPERTY);
        if (l == null || l.intValue() != layer) {
            // the layer is really changed
            c.putClientProperty(LAYER_PROPERTY, new Integer(layer));
        }
    }

    public static int getLayer(final JComponent c) {
        Integer layer = ((Integer) c.getClientProperty(LAYER_PROPERTY));
        return layer == null ? 0 : layer.intValue();
    }

    private void rememberLayerForComponent(final Component c, final int layer) {
        if (c instanceof JComponent) {
            putLayer((JComponent) c, layer);
        }
        getComponentToLayer().put(c, getObjectForLayer(layer));
    }
}
