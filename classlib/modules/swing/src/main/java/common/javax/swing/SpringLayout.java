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
package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpringLayout implements LayoutManager2 {
    public static class Constraints {
        private Spring x;
        private Spring y;

        private final Spring[] constraintSprings = new Spring[6];

        private final ConstraintsOrder horizontalConstraintsOrder;
        private final ConstraintsOrder verticalConstraintsOrder;


        public Constraints() {
            this(null, null, null, null);
        }

        public Constraints(final Spring x, final Spring y) {
            this(x, y, null, null);
        }

        public Constraints(final Component c) {
            this(Spring.constant(c.getX()),
                 Spring.constant(c.getY()),
                 Spring.width(c),
                 Spring.height(c));
        }

        public Constraints(final Spring x, final Spring y,
                           final Spring width, final Spring height) {
             horizontalConstraintsOrder = new ConstraintsOrder(EAST_EDGE,
                                                               WEST_EDGE,
                                                               WIDTH);
             constraintSprings[WIDTH] = width;
             constraintSprings[WEST_EDGE] = x;
             deriveConstraint(EAST_EDGE);

             verticalConstraintsOrder = new ConstraintsOrder(SOUTH_EDGE,
                                                             NORTH_EDGE,
                                                             HEIGHT);
             constraintSprings[HEIGHT] = height;
             constraintSprings[NORTH_EDGE] = y;
             deriveConstraint(SOUTH_EDGE);
        }

        public void setX(final Spring x) {
            constraintSprings[WEST_EDGE] = x;
            deriveConstraint(horizontalConstraintsOrder.push(WEST_EDGE));
            this.x = null;
        }

        public Spring getX() {
           return x == null ? calculateX() : x;
        }

        public void setY(final Spring y) {
            constraintSprings[NORTH_EDGE] = y;
            deriveConstraint(verticalConstraintsOrder.push(NORTH_EDGE));
            this.y = null;
        }

        public Spring getY() {
            return y == null ? calculateY() : y;
        }

        public void setWidth(final Spring width) {
            constraintSprings[WIDTH] = width;
            deriveConstraint(horizontalConstraintsOrder.push(WIDTH));
            x = null;
        }

        public Spring getWidth() {
            return constraintSprings[WIDTH];
        }

        public void setHeight(final Spring height) {
            constraintSprings[HEIGHT] = height;
            deriveConstraint(verticalConstraintsOrder.push(HEIGHT));
            y = null;
        }

        public Spring getHeight() {
            return constraintSprings[HEIGHT];
        }

        public void setConstraint(final String edgeName, final Spring s) {
            final int edge = SpringLayout.getType(edgeName);

            switch (edge) {
                case EAST_EDGE :
                    constraintSprings[EAST_EDGE] = s;
                    deriveConstraint(horizontalConstraintsOrder.push(EAST_EDGE));
                    x = null;
                    break;
                case WEST_EDGE:
                    setX(s);
                    break;
                case SOUTH_EDGE:
                    constraintSprings[SOUTH_EDGE] = s;
                    deriveConstraint(verticalConstraintsOrder.push(SOUTH_EDGE));
                    y = null;
                    break;
                case NORTH_EDGE:
                    setY(s);
                    break;
                default:
                    break;
            }
        }

        public Spring getConstraint(final String edgeName) {
            final int constraintType = SpringLayout.getType(edgeName);
            if (constraintType >= 0) {
                 return constraintSprings[constraintType];
            }
            return null;
        }

        private void deriveConstraint(final byte type) {
            Spring newValue = null;
            switch (type) {
               case WEST_EDGE:
                   if (constraintSprings[EAST_EDGE] != null && getWidth() != null) {
                       newValue = Spring.sum(constraintSprings[EAST_EDGE],
                                             Spring.minus(getWidth()));
                   }
                   break;
               case EAST_EDGE:
                   if (constraintSprings[WEST_EDGE] != null && getWidth() != null) {
                       newValue =  Spring.sum(constraintSprings[WEST_EDGE],
                                              getWidth());
                   }
                   break;
               case WIDTH:
                   if (constraintSprings[EAST_EDGE] != null
                       && constraintSprings[WEST_EDGE] != null) {

                       newValue =
                           Spring.sum(constraintSprings[EAST_EDGE],
                                      Spring.minus(constraintSprings[WEST_EDGE]));
                   }
                   break;
               case NORTH_EDGE:
                   if (constraintSprings[SOUTH_EDGE] != null && getHeight() != null) {
                       newValue =  Spring.sum(constraintSprings[SOUTH_EDGE],
                                              Spring.minus(getHeight()));
                   }
                   break;
               case SOUTH_EDGE:
                   if (constraintSprings[NORTH_EDGE] != null && getHeight() != null) {
                       newValue =  Spring.sum(constraintSprings[NORTH_EDGE],
                                              getHeight());
                   }
                   break;
               case HEIGHT:
                   if (constraintSprings[SOUTH_EDGE] != null
                       && constraintSprings[NORTH_EDGE] != null) {

                       newValue =
                           Spring.sum(constraintSprings[SOUTH_EDGE],
                                      Spring.minus(constraintSprings[NORTH_EDGE]));
                   }
                   break;
               default:
                   return;
            }
            constraintSprings[type] = newValue;
        }

        private Spring calculateX() {
            return constraintSprings[WEST_EDGE];
        }

        private Spring calculateY() {
            return constraintSprings[NORTH_EDGE];
        }

        private void clearConstraints(final SpringLayout layout) {
            layout.markedSprings.clear();
            constraintSprings[WIDTH].setValue(Spring.UNSET);
            layout.markedSprings.clear();
            constraintSprings[HEIGHT].setValue(Spring.UNSET);

            layout.markedSprings.clear();
            constraintSprings[WEST_EDGE].setValue(Spring.UNSET);
            layout.markedSprings.clear();
            constraintSprings[EAST_EDGE].setValue(Spring.UNSET);

            layout.markedSprings.clear();
            constraintSprings[NORTH_EDGE].setValue(Spring.UNSET);
            layout.markedSprings.clear();
            constraintSprings[SOUTH_EDGE].setValue(Spring.UNSET);
        }
    }

    private static class ConstraintsOrder {
        private byte[] constraintsOrder = new byte[3];
        private int offset;
        public ConstraintsOrder(final byte constraintType1,
                                final byte constrintType2,
                                final byte constrintType3) {
            push(constraintType1);
            push(constrintType2);
            push(constrintType3);
        }

        public byte push(final byte constraintType) {
            final int nextOffset = (offset + 1) % 3;
            final int prevOffset = (offset + 2) % 3;
            final byte oldConstraintType = constraintsOrder[nextOffset];

            if (oldConstraintType == constraintType) {
                offset = nextOffset;
                return constraintsOrder[prevOffset];
            }
            if (peek() != constraintType) {
                if (constraintsOrder[prevOffset] == constraintType) {
                    constraintsOrder[prevOffset] = oldConstraintType;
                }
                constraintsOrder[nextOffset] = constraintType;

                offset = nextOffset;
            }
            return oldConstraintType;
        }

        public byte peek() {
            return constraintsOrder[offset];
        }
    }

    private static class ProxySpring extends Spring {
        private final byte edgeType;
        private final SpringLayout layout;
        private final Component component;

        public ProxySpring(final byte edgeType, final Component component,
                                 final SpringLayout layout) {
            this.edgeType = edgeType;
            this.layout = layout;
            this.component = component;
        }

        @Override
        public int getMinimumValue() {
            return getSpring().getMinimumValue();
        }

        @Override
        public int getPreferredValue() {
            return getSpring().getPreferredValue();
        }

        @Override
        public int getMaximumValue() {
            return getSpring().getMaximumValue();
        }

        @Override
        public int getValue() {
            final Spring s = getSpring();
            if (layout.calculatedSprings.containsKey(s)) {
                return layout.calculatedSprings.get(s).intValue();
            }
            if (layout.markedSprings.contains(s)) {
                return 0;
            }
            layout.markedSprings.add(s);
            final int value = s.getValue();
            layout.calculatedSprings.put(s, new Integer(value));
            return value;
        }

        @Override
        public void setValue(final int value) {
            final Spring s = getSpring();
            if (layout.markedSprings.contains(s)) {
                return;
            }
            layout.markedSprings.add(s);
            s.setValue(value);
        }

        @Override
        public String toString() {
            String edgeName;
            switch (edgeType) {
               case WEST_EDGE:
                   edgeName = "WEST";
                   break;
               case EAST_EDGE:
                   edgeName = "EAST";
                   break;
               case NORTH_EDGE:
                   edgeName = "NORTH";
                   break;
               case SOUTH_EDGE:
                   edgeName = "SOUTH";
                   break;
               default:
                   edgeName = "";
                   break;
            }
            return "[ProxySpring for " + edgeName + " edge of component "
                   + component.getClass().getName() + "]";
        }

        private Spring getSpring() {
            switch (edgeType) {
            case WEST_EDGE:
                return layout.getConstraints(component).getX();
            case EAST_EDGE:
                return Spring.sum(layout.getConstraints(component).getX(),
                                  layout.getConstraints(component).getWidth());
            case NORTH_EDGE:
                return layout.getConstraints(component).getY();

            case SOUTH_EDGE:
                return Spring.sum(layout.getConstraints(component).getY(),
                                  layout.getConstraints(component).getHeight());
            default:
                return null;
            }
        }
    }

    public static final String WEST = "West";
    public static final String EAST = "East";
    public static final String NORTH = "North";
    public static final String SOUTH = "South";

    private static final float CENTERED = 0.5f;
    private static final byte WEST_EDGE = 0;
    private static final byte EAST_EDGE = 1;
    private static final byte NORTH_EDGE = 2;
    private static final byte SOUTH_EDGE = 3;
    private static final byte WIDTH = 4;
    private static final byte HEIGHT = 5;

    private Map<Spring, Integer> calculatedSprings = new HashMap<Spring, Integer>();
    private Map<Component, Constraints> constraintsMap = new HashMap<Component, Constraints>();
    private Set<Spring> markedSprings = new HashSet<Spring>();

    public SpringLayout() {
    }

    public void addLayoutComponent(final String name, final Component c) {
        // Specified by LayoutManager2 but is not used
    }

    public void removeLayoutComponent(final Component c) {
        constraintsMap.remove(c);
    }

    public Dimension minimumLayoutSize(final Container container) {
        Constraints targetConstraints = getConstraints(container);
        initTargetConstrains(container, targetConstraints);

        return new Dimension(targetConstraints.getWidth()
                                 .getMinimumValue()
                             + container.getInsets().left
                             + container.getInsets().right,

                             targetConstraints.getHeight()
                                 .getMinimumValue()
                             + container.getInsets().top
                             + container.getInsets().bottom);
    }

    public Dimension preferredLayoutSize(final Container container) {
      Constraints targetConstraints = getConstraints(container);
      initTargetConstrains(container, targetConstraints);
      return new Dimension(targetConstraints.getWidth()
                               .getPreferredValue()
                           + container.getInsets().left
                           + container.getInsets().right,

                           targetConstraints.getHeight()
                               .getPreferredValue()
                           + container.getInsets().top
                           + container.getInsets().bottom);
    }

    public Dimension maximumLayoutSize(final Container container) {
        Constraints targetConstraints = getConstraints(container);
        initTargetConstrains(container, targetConstraints);

        return new Dimension(targetConstraints.getWidth()
                                 .getMaximumValue()
                             + container.getInsets().left
                             + container.getInsets().right,

                             targetConstraints.getHeight()
                                 .getMaximumValue()
                             + container.getInsets().top
                             + container.getInsets().bottom);
    }

    public void addLayoutComponent(final Component component,
                                   final Object constraints) {
        if (constraints != null && constraints instanceof Constraints) {
            constraintsMap.put(component, (Constraints)constraints);
        }
    }

    public float getLayoutAlignmentX(final Container p) {
        return CENTERED;
    }

    public float getLayoutAlignmentY(final Container p) {
        return CENTERED;
    }

    public void invalidateLayout(final Container p) {
        //Do nothing
    }

    public void putConstraint(final String edge1, final Component component1,
                              final int pad,
                              final String edge2, final Component component2) {

        putConstraint(edge1, component1,
                      Spring.constant(pad),
                      edge2, component2);
    }

    public void putConstraint(final String edge1, final Component component1,
                              final Spring pad,
                              final String edge2, final Component component2) {

        Constraints constraints1 =  getConstraints(component1);

        final byte edge1Type = getType(edge1);
        final byte edge2Type = getType(edge2);

        final boolean edge1IsHorizontal =  edge1Type == EAST_EDGE
                                           || edge1Type == WEST_EDGE;
        final boolean edge2IsHorizontal =  edge2Type == EAST_EDGE
                                           || edge2Type == WEST_EDGE;

        if ((edge1IsHorizontal &&  edge2IsHorizontal)
            || (!edge1IsHorizontal && !edge2IsHorizontal)) {

            constraints1.setConstraint(edge1,
                                       Spring.sum(new ProxySpring(edge2Type,
                                                                  component2,
                                                                  this),
                                        pad));
        }
    }

    public Constraints getConstraints(final Component component) {
        Constraints constraints = constraintsMap.get(component);
        if (constraints != null) {
            return constraints;
        }

        constraints = new Constraints(Spring.constant(0),
                                      Spring.constant(0),
                                      Spring.width(component),
                                      Spring.height(component));
        constraintsMap.put(component, constraints);
        return constraints;
    }

    public Spring getConstraint(final String edgeName,
                                final Component component) {

        return new ProxySpring(getType(edgeName),
                                     component,
                                     this);
    }

    public void layoutContainer(final Container container) {
        Component component;
        Constraints constraints;

        Constraints targetConstraints = getConstraints(container);
        initTargetConstrains(container, targetConstraints);
        targetConstraints.clearConstraints(this);

        if (container.getLayout() != this) {
            return;
        }

        for (int i = 0; i < container.getComponentCount(); i++) {
            getConstraints(container.getComponent(i)).clearConstraints(this);
        }
        calculatedSprings.clear();

        targetConstraints.getWidth().setValue(container.getWidth());
        targetConstraints.getHeight().setValue(container.getHeight());

        for (int i = 0; i < container.getComponentCount(); i++) {
            component = container.getComponent(i);
            constraints = getConstraints(component);

            component.setBounds(getValue(constraints.getX()),
                                getValue(constraints.getY()),
                                getValue(constraints.getWidth()),
                                getValue(constraints.getHeight()));
        }
    }

    private static byte getType(final String edgeName) {
        if (EAST.equals(edgeName)) {
            return EAST_EDGE;
        } else if (WEST.equals(edgeName)) {
            return WEST_EDGE;
        } else if (NORTH.equals(edgeName)) {
            return NORTH_EDGE;
        } else if (SOUTH.equals(edgeName)) {
            return SOUTH_EDGE;
        }
        return -1;
    }

    private int getValue(final Spring s) {
        if (!calculatedSprings.containsKey(s)) {
            markedSprings.clear();
            final int value = s.getValue();
            calculatedSprings.put(s, new Integer(value));
            return value;
        }
        return calculatedSprings.get(s).intValue();
    }

    private void initTargetConstrains(final Container target,
                                      final Constraints targetConstraints) {
        targetConstraints.setX(Spring.constant(0));
        targetConstraints.setY(Spring.constant(0));

        Spring width  = targetConstraints.getWidth();
        if (width instanceof Spring.WidthSpring) {
            if (((Spring.WidthSpring) width).component == target) {
                targetConstraints.setWidth(Spring.constant(0, 0,
                                                          Integer.MAX_VALUE));
            }
        }

        Spring height  = targetConstraints.getHeight();
        if (height instanceof Spring.HeightSpring) {
            if (((Spring.HeightSpring) height).component == target) {
                targetConstraints.setHeight(Spring.constant(0, 0,
                                                           Integer.MAX_VALUE));
            }
        }
    }
}