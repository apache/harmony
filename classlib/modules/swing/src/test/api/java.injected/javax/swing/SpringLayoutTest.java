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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.TextField;

import javax.swing.SpringLayout.Constraints;

public class SpringLayoutTest extends SwingTestCase {
    private Component component;
    private SpringLayout layout;
    private JPanel container;
    private JButton button;
    private JTextField textField;
    private JPanel panel;
    private JButton innerButton;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        layout = new SpringLayout();
        component = new JLabel("label");
        container = new JPanel(layout);
        button = new JButton();
        textField = new JTextField();
        panel = new JPanel();
        innerButton = new JButton();

        container.add(button);
        container.add(panel);
        container.add(textField);

        panel.add(innerButton);

        setComponentSizes(container, Spring.constant(0, 5, Integer.MAX_VALUE),
                          Spring.constant(0, 5, Integer.MAX_VALUE));

        setComponentSizes(button, Spring.constant(2, 5, 100),
                          Spring.constant(2, 5, 100));

        setComponentSizes(textField, Spring.constant(2, 10, Integer.MAX_VALUE),
                          Spring.constant(2, 10, Integer.MAX_VALUE));

        setComponentSizes(panel, Spring.constant(1, 3, Integer.MAX_VALUE),
                          Spring.constant(1, 3, Integer.MAX_VALUE));

        setComponentSizes(innerButton, Spring.constant(2, 10, 60),
                          Spring.constant(2, 10, 60));
    }

    public void testSpringLayout() {
        SpringLayout.Constraints constrains = new SpringLayout.Constraints();
        assertNull(constrains.getX());
        assertNull(constrains.getY());
        assertNull(constrains.getWidth());
        assertNull(constrains.getHeight());
        constrains = new SpringLayout.Constraints(Spring.width(component), Spring.constant(0));
        assertNull(constrains.getWidth());
        assertNull(constrains.getHeight());
        SpringTest.assertSizes(0, 0, 0, 0, constrains.getY());
        SpringTest.assertSizes(component.getMinimumSize().width,
                component.getPreferredSize().width, component.getMaximumSize().width,
                constrains.getX());
        assertNull(constrains.getWidth());
        assertNull(constrains.getHeight());
        constrains = new SpringLayout.Constraints(component);
        SpringTest.assertSizes(0, 0, 0, 0, constrains.getX());
        SpringTest.assertSizes(0, 0, 0, 0, constrains.getY());
        SpringTest.assertSizes(component.getMinimumSize().width,
                component.getPreferredSize().width, component.getMaximumSize().width,
                constrains.getWidth());
        SpringTest.assertSizes(component.getMinimumSize().height,
                component.getPreferredSize().height, component.getMaximumSize().height,
                constrains.getHeight());
        constrains = new SpringLayout.Constraints(Spring.constant(1), Spring.constant(2),
                Spring.constant(3), Spring.constant(4));
        SpringTest.assertSizes(1, 1, 1, 1, constrains.getX());
        SpringTest.assertSizes(2, 2, 2, 2, constrains.getY());
        SpringTest.assertSizes(3, 3, 3, 3, constrains.getWidth());
        SpringTest.assertSizes(4, 4, 4, 4, constrains.getHeight());
        constrains = new SpringLayout.Constraints(Spring.constant(1), null);
        SpringTest.assertSizes(1, 1, 1, 1, constrains.getX());
        Container container = new JPanel();
        container.setLayout(layout);
        container.add(new JLabel(""));
        constrains = layout.getConstraints(component);
        SpringTest.assertSizes(0, 0, 0, 0, constrains.getX());
        SpringTest.assertSizes(0, 0, 0, 0, constrains.getY());
        SpringTest.assertSizes(component.getMinimumSize().width,
                component.getPreferredSize().width, component.getMaximumSize().width,
                constrains.getWidth());
        SpringTest.assertSizes(component.getMinimumSize().height,
                component.getPreferredSize().height, component.getMaximumSize().height,
                constrains.getHeight());
    }

    public void testGetLayoutAlignmentX() {
        JPanel container = new JPanel(layout);
        assertEquals(0.5f, layout.getLayoutAlignmentX(null), 0.01);
        assertEquals(0.5f, layout.getLayoutAlignmentX(container), 0.01);
    }

    public void testGetLayoutAlignmentY() {
        JPanel container = new JPanel(layout);
        assertEquals(0.5f, layout.getLayoutAlignmentY(null), 0.01);
        assertEquals(0.5f, layout.getLayoutAlignmentY(container), 0.01);
    }

    public void testGetConstraints() {
        Constraints constraints = layout.getConstraints(null);
        assertNotNull(constraints);
        layout.addLayoutComponent(component, "not_constraints");
        constraints = layout.getConstraints(component);
        assertNotNull(constraints);
        SpringTest.assertSizes(0, 0, 0, constraints.getX());
        SpringTest.assertSizes(0, 0, 0, constraints.getY());
        SpringTest.assertSizes(component.getMinimumSize().width,
                component.getPreferredSize().width, component.getMaximumSize().width,
                constraints.getWidth());
        SpringTest.assertSizes(component.getMinimumSize().height,
                component.getPreferredSize().height, component.getMaximumSize().height,
                constraints.getHeight());
        layout.addLayoutComponent(component, null);
        constraints = layout.getConstraints(component);
        assertNotNull(constraints);
        SpringTest.assertSizes(0, 0, 0, constraints.getX());
        SpringTest.assertSizes(0, 0, 0, constraints.getY());
        SpringTest.assertSizes(component.getMinimumSize().width,
                component.getPreferredSize().width, component.getMaximumSize().width,
                constraints.getWidth());
        SpringTest.assertSizes(component.getMinimumSize().height,
                component.getPreferredSize().height, component.getMaximumSize().height,
                constraints.getHeight());
        Constraints componentConstraints = new SpringLayout.Constraints(component);
        layout.addLayoutComponent(component, constraints);
        constraints = layout.getConstraints(component);
        assertFalse(componentConstraints.equals(constraints));
        SpringTest.assertValues(componentConstraints.getX(), constraints.getX());
        SpringTest.assertValues(componentConstraints.getY(), constraints.getY());
        SpringTest.assertValues(componentConstraints.getWidth(), constraints.getWidth());
        SpringTest.assertValues(componentConstraints.getHeight(), constraints.getHeight());

        assertNotNull(layout.getConstraints(new JLabel()));
    }

    public void testGetConstraint() {
        layout.putConstraint(SpringLayout.SOUTH, button, Spring.constant(5),
                             SpringLayout.NORTH, container);
        layout.putConstraint(SpringLayout.EAST, button, Spring.constant(5),
                             SpringLayout.WEST, container);

        layout.layoutContainer(container);

        Spring constraint = layout.getConstraint(SpringLayout.WEST, button);
        assertEquals(0, constraint.getValue());
        layout.getConstraints(button).setConstraint(SpringLayout.EAST, Spring.constant(100));
        layout.layoutContainer(container);
        assertEquals(95, constraint.getValue());

        assertNotNull(layout.getConstraint(SpringLayout.EAST, new JLabel()));
    }

    public void testPutConstraint_Cycles() throws Exception {
        if (isHarmony()) {
          layout.putConstraint(SpringLayout.EAST, button, 5, SpringLayout.WEST, panel);
          layout.putConstraint(SpringLayout.EAST, panel, 5, SpringLayout.WEST, textField);
          layout.putConstraint(SpringLayout.EAST, textField, 5, SpringLayout.WEST, button);
          layout.layoutContainer(container);
          layout.layoutContainer(container);

          layout.getConstraints(button);

          assertEdges(layout.getConstraints(button), -3, 2, 0, 5);
          assertEdges(layout.getConstraints(panel), -3, 0, 0, 3);
          assertEdges(layout.getConstraints(textField), -5, 5, 0, 10);

          layout.putConstraint(SpringLayout.EAST, textField, 5, SpringLayout.WEST, container);
          layout.layoutContainer(container);
        }
    }

    public void testPutConstraint() throws Exception {
        JFrame frame = new JFrame("testPutConstraint");
        container.setLayout(layout);
        layout.putConstraint(SpringLayout.SOUTH, button, 5, SpringLayout.NORTH, container);
        layout.putConstraint(SpringLayout.EAST, button, Spring.constant(5),
                             SpringLayout.WEST, container);

        frame.pack();
        frame.setVisible(true);
        frame.setSize(100, 200);
        assertEquals(0, button.getX());
        assertEquals(0, button.getY());

        layout.putConstraint(SpringLayout.EAST, button, Spring.constant(15),
                             SpringLayout.WEST, container);

        layout.layoutContainer(container);
        assertEquals(10, button.getX());
        assertEquals(0, button.getY());

        layout.putConstraint(SpringLayout.SOUTH, button,
                             null,
                             SpringLayout.NORTH, container);

        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
               layout.layoutContainer(container);
            }

        });
        assertEquals(10, button.getX());
        assertEquals(0, button.getY());


        //Independence from constraints order
        layout = new SpringLayout();
        container.setLayout(layout);
        JTextField textField = new JTextField();
        textField.getInsets().set(5, 5, 5, 5);
        setComponentSizes(textField, Spring.constant(2, 10, Integer.MAX_VALUE),
                          Spring.constant(2, 10, Integer.MAX_VALUE));
        container.add(textField);

        layout.getConstraints(button).setHeight(Spring.constant(50));

        layout.putConstraint(SpringLayout.NORTH, textField, Spring.constant(5),
                             SpringLayout.SOUTH, button);

        layout.putConstraint(SpringLayout.SOUTH, textField, Spring.constant(-10),
                             SpringLayout.SOUTH, container);

        container.setSize(200, 200);
        layout.layoutContainer(container);
        if (isHarmony()) {
            assertEquals(135, textField.getHeight());
        } else {
            assertEquals(10, textField.getHeight());
        }

        layout = new SpringLayout();
        textField = new JTextField();
        container.add(textField);
        container.setLayout(layout);
        layout.getConstraints(button).setHeight(Spring.constant(50));

        layout.putConstraint(SpringLayout.SOUTH, textField, Spring.constant(-10),
                             SpringLayout.SOUTH, container);

        layout.putConstraint(SpringLayout.NORTH, textField, Spring.constant(5),
                             SpringLayout.SOUTH, button);

        container.setSize(200, 200);
        layout.layoutContainer(container);
        assertEquals(135, textField.getHeight());

        component = new TextField();
        layout.putConstraint(SpringLayout.NORTH, textField, Spring.constant(5),
                             SpringLayout.SOUTH, component);
        layout.layoutContainer(container);
        assertEquals(185, textField.getHeight());
    }

    public void testAddLayoutComponent() throws Exception {
        Constraints constraints = new Constraints(Spring.constant(1), Spring.constant(2),
                                                  Spring.constant(3), Spring.constant(4));
        layout.addLayoutComponent(null, constraints);
        Constraints componentCnstraints = layout.getConstraints(null);
        assertNotNull(componentCnstraints);
        assertSame(componentCnstraints, constraints);

        layout.addLayoutComponent(button, null);
        componentCnstraints = layout.getConstraints(button);
        assertNotNull(componentCnstraints);
        assertEquals(componentCnstraints.getX().getValue(), 0);
        assertEquals(componentCnstraints.getY().getValue(), 0);
        assertEquals(componentCnstraints.getWidth().getValue(), button.getPreferredSize().width);
        assertEquals(componentCnstraints.getHeight().getValue(), button.getPreferredSize().height);

        layout.addLayoutComponent(button, constraints);
        componentCnstraints = layout.getConstraints(button);
        assertSame(componentCnstraints, constraints);
    }

    public void testRemoveLayoutComponent() {
        layout.removeLayoutComponent(null);
        layout.removeLayoutComponent(new JLabel());

        layout.addLayoutComponent(component, new SpringLayout.Constraints(component));
        layout.removeLayoutComponent(null);
        layout.removeLayoutComponent(new JLabel());

        JPanel panel = new JPanel(layout);
        panel.add(component);
        Constraints constraints1 = layout.getConstraints(component);
        layout.removeLayoutComponent(component);

        Constraints constraints2 = layout.getConstraints(component);
        assertNotSame(constraints1, constraints2);
        SpringTest.assertSizes(0, 0, 0, 0, constraints2.getX());
        SpringTest.assertSizes(0, 0, 0, 0, constraints2.getY());

        component = new JTextField();
        JLabel label = new JLabel("label");
        setComponentSizes(label, Spring.constant(5), Spring.constant(10));
        panel.add(component);
        panel.add(label);

        layout.getConstraints(label).setX(Spring.constant(15));
        layout.putConstraint(SpringLayout.WEST, component,
                             5,
                             SpringLayout.EAST, label);
        layout.layoutContainer(panel);
        assertEquals(15, label.getX());

        layout.removeLayoutComponent(label);
        layout.layoutContainer(panel);
        assertEquals(0, label.getX());
    }

    public void testInvalidateLayout() throws Exception {
        JFrame frame = new JFrame("testMinimumLayoutSize");
        frame.setContentPane(container);
        layout = new SpringLayout();
        container.setLayout(layout);
        Constraints buttonConstraints = layout.getConstraints(button);
        buttonConstraints.setHeight(Spring.constant(33, 34, 35));
        Constraints containerConstraints = layout.getConstraints(container);
        containerConstraints.setConstraint(SpringLayout.SOUTH,
                                           buttonConstraints.getConstraint(SpringLayout.SOUTH));
        containerConstraints.setConstraint(SpringLayout.EAST,
                                           buttonConstraints.getConstraint(SpringLayout.EAST));
        containerConstraints.setConstraint(SpringLayout.WEST, Spring.constant(3));        
        frame.pack();
        frame.setVisible(true);
        frame.setSize(100, 200);

        Spring width = buttonConstraints.getWidth();
        if (isHarmony()) {
            assertEquals(5, width.getValue());
        } else {
            assertEquals(115, width.getValue());
        }
        frame.setSize(200, 300);
        layout.invalidateLayout(container);
        assertSame(width, buttonConstraints.getWidth());
        if (isHarmony()) {
            assertEquals(5, width.getValue());
        } else {
            assertEquals(115, width.getValue());
        }
    }

    public void testLayoutContainer() throws Exception {
        button.setPreferredSize(new Dimension(11, 12));

        Constraints buttonConstraints = layout.getConstraints(button);
        buttonConstraints.setHeight(Spring.constant(35));
        Constraints containerConstraints = layout.getConstraints(container);
        containerConstraints.setConstraint(SpringLayout.SOUTH,
                                           buttonConstraints.getConstraint(SpringLayout.SOUTH));
        container.setSize(100, 200);
        layout.layoutContainer(container);
        assertBounds(0, 0, 11, 35, button.getBounds());

        containerConstraints.setConstraint(SpringLayout.EAST, buttonConstraints.getWidth());
        layout.layoutContainer(container);
        assertBounds(0, 0, 100, 35, button.getBounds());

        container.setSize(101, 201);
        buttonConstraints.setHeight(Spring.constant(40));
        layout.layoutContainer(innerButton);
        assertBounds(0, 0, 100, 35, button.getBounds());

        container.setSize(102, 202);
        buttonConstraints.setHeight(Spring.constant(50));
        layout.layoutContainer(textField);
        assertBounds(0, 0, 100, 35, button.getBounds());

        //putConstraints()
        layout = new SpringLayout();
        container.setLayout(layout);
        layout.putConstraint(SpringLayout.SOUTH, button, Spring.constant(5),
                             SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.SOUTH, button, Spring.constant(15),
                             SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.SOUTH, textField, Spring.constant(55),
                             SpringLayout.SOUTH, button);
        layout.putConstraint(SpringLayout.EAST, button, Spring.constant(15),
                             SpringLayout.WEST, panel);

        layout.putConstraint(SpringLayout.EAST, textField, Spring.constant(55),
                             SpringLayout.EAST, button);
        container.setSize(100, 200);
        layout.layoutContainer(container);

        Spring y = Spring.sum(Spring.sum(layout.getConstraint(SpringLayout.SOUTH, button),
                                         Spring.constant(55)),
                              Spring.minus(Spring.height(textField)));
        Spring height = Spring.height(textField);
        assertBounds(4, 3, 11, 12, button.getBounds());
        assertBounds(60, y.getValue(), 10, height.getValue(), textField.getBounds());

        layout = new SpringLayout();
        container.setLayout(layout);
        layout.putConstraint(SpringLayout.NORTH, button, Spring.constant(5),
                             SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, button, Spring.constant(15),
                             SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, textField, Spring.constant(55),
                             SpringLayout.SOUTH, button);
        layout.putConstraint(SpringLayout.WEST, button, Spring.constant(15),
                             SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, textField, Spring.constant(55),
                             SpringLayout.EAST, button);
        layout.layoutContainer(container);
        y = Spring.sum(layout.getConstraint(SpringLayout.SOUTH, button),
                       Spring.constant(55));
        height = Spring.height(textField);
        assertBounds(15, 15, 11, 12, button.getBounds());
        assertBounds(81, y.getValue(), 10, height.getValue(), textField.getBounds());
    }

    public void testMinimumLayoutSize() throws Exception {
        //container
        setComponentSizes(button, Spring.constant(2, 5, 100), Spring.constant(3, 6, 101));
        Constraints buttonConstraints = layout.getConstraints(button);
        buttonConstraints.setHeight(Spring.constant(33, 34, 35));
        Constraints containerConstraints = layout.getConstraints(container);
        containerConstraints.setConstraint(SpringLayout.SOUTH,
                                           buttonConstraints.getConstraint(SpringLayout.SOUTH));
        containerConstraints.setConstraint(SpringLayout.EAST,
                                           buttonConstraints.getConstraint(SpringLayout.EAST));
        containerConstraints.setConstraint(SpringLayout.WEST, Spring.constant(3));
        layout.layoutContainer(container);
        assertEquals(2, layout.minimumLayoutSize(container).width);
        assertEquals(33, layout.minimumLayoutSize(container).height);

        //putConstraints()
        assertEquals(button.getInsets().left + button.getInsets().right,
                     layout.minimumLayoutSize(button).width);

        assertEquals(panel.getInsets().left + panel.getInsets().right,
                     layout.minimumLayoutSize(panel).width);

        assertEquals(innerButton.getInsets().left + innerButton.getInsets().right,
                     layout.minimumLayoutSize(innerButton).width);

        buttonConstraints.setX(Spring.constant(20));
        assertEquals(button.getInsets().left + button.getInsets().right,
                     layout.minimumLayoutSize(button).width);
        assertEquals(0, layout.getConstraint(SpringLayout.EAST, button).getPreferredValue());

        buttonConstraints.setWidth(Spring.constant(100));
        assertEquals(100 + button.getInsets().left + button.getInsets().right,
                     layout.minimumLayoutSize(button).width);

        layout.getConstraints(innerButton).setX(Spring.constant(20));
        assertEquals(innerButton.getInsets().left + innerButton.getInsets().right,
                     layout.minimumLayoutSize(innerButton).width);

        layout.getConstraints(innerButton).setWidth(Spring.constant(100));
        assertEquals(100 + innerButton.getInsets().left + innerButton.getInsets().right,
                     layout.minimumLayoutSize(innerButton).width);

        layout.getConstraints(panel).setX(Spring.constant(20));
        assertEquals(panel.getInsets().left + panel.getInsets().right,
                     layout.minimumLayoutSize(panel).width);
        assertEquals(0, layout.getConstraint(SpringLayout.EAST, panel).getPreferredValue());

        layout.getConstraints(panel).setWidth(Spring.constant(100));
        assertEquals(100 + panel.getInsets().left + panel.getInsets().right,
                     layout.minimumLayoutSize(panel).width);
        assertEquals(100, layout.getConstraint(SpringLayout.EAST, panel).getPreferredValue());


        layout = new SpringLayout();
        container.setLayout(layout);
        layout.putConstraint(SpringLayout.SOUTH, button, Spring.constant(15),
                             SpringLayout.NORTH, container);
        layout.putConstraint(SpringLayout.EAST, button, Spring.constant(5),
                             SpringLayout.WEST, container);
        assertEquals(panel.getInsets().left + panel.getInsets().right,
                     layout.minimumLayoutSize(panel).width);
        assertEquals(innerButton.getInsets().left + innerButton.getInsets().right,
                     layout.minimumLayoutSize(innerButton).width);

        assertEquals(5 + button.getInsets().left + button.getInsets().right,
                     layout.minimumLayoutSize(button).width);
    }

    public void testPreferredLayoutSize() throws Exception {
        //container
        setComponentSizes(button,
                          Spring.constant(2, 5, 100),
                          Spring.constant(3, 6, 101));
        Constraints buttonConstraints = layout.getConstraints(button);
        buttonConstraints.setHeight(Spring.constant(33, 34, 35));
        Constraints containerConstraints = layout.getConstraints(container);
        containerConstraints.setConstraint(SpringLayout.SOUTH,
                                           buttonConstraints
                                               .getConstraint(SpringLayout.SOUTH));
        containerConstraints.setConstraint(SpringLayout.EAST,
                                           buttonConstraints
                                               .getConstraint(SpringLayout.EAST));
        containerConstraints.setConstraint(SpringLayout.WEST,
                                           Spring.constant(3));
        layout.layoutContainer(container);
        assertEquals(5, layout.preferredLayoutSize(container).width);
        assertEquals(34, layout.preferredLayoutSize(container).height);

        //putConstraints()
        assertEquals(button.getInsets().left
                     + button.getInsets().right,
                     layout.preferredLayoutSize(button).width);

        assertEquals(panel.getInsets().left
                     + panel.getInsets().right,
                     layout.preferredLayoutSize(panel).width);

        assertEquals(innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.preferredLayoutSize(innerButton).width);

        buttonConstraints.setX(Spring.constant(20));
        assertEquals(button.getInsets().left
                     + button.getInsets().right,
                     layout.preferredLayoutSize(button).width);
        assertEquals(0,
                     layout.getConstraint(SpringLayout.EAST, button)
                        .getPreferredValue());

        buttonConstraints.setWidth(Spring.constant(100));
        assertEquals(100
                     + button.getInsets().left
                     + button.getInsets().right,
                     layout.preferredLayoutSize(button).width);

        layout.getConstraints(innerButton).setX(Spring.constant(20));
        assertEquals(innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.preferredLayoutSize(innerButton).width);

        layout.getConstraints(innerButton).setWidth(Spring.constant(100));
        assertEquals(100
                     + innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.preferredLayoutSize(innerButton).width);

        layout.getConstraints(panel).setX(Spring.constant(20));
        assertEquals(panel.getInsets().left
                     + panel.getInsets().right,
                     layout.preferredLayoutSize(panel).width);

        assertEquals(0,
                     layout.getConstraint(SpringLayout.EAST, panel)
                        .getPreferredValue());

        layout.getConstraints(panel).setWidth(Spring.constant(100));
        assertEquals(100
                     + panel.getInsets().left
                     + panel.getInsets().right,
                     layout.preferredLayoutSize(panel).width);
        assertEquals(100,
                     layout.getConstraint(SpringLayout.EAST, panel)
                        .getPreferredValue());

        layout = new SpringLayout();
        container.setLayout(layout);
        layout.putConstraint(SpringLayout.SOUTH, button,
                             Spring.constant(15),
                             SpringLayout.NORTH, container);


        layout.putConstraint(SpringLayout.EAST, button,
                             Spring.constant(5),
                             SpringLayout.WEST, container);

        assertEquals(panel.getInsets().left
                     + panel.getInsets().right,
                     layout.preferredLayoutSize(panel).width);

        assertEquals(innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.preferredLayoutSize(innerButton).width);

        assertEquals(5
                     + button.getInsets().left
                     + button.getInsets().right,
                     layout.preferredLayoutSize(button).width);
    }

    public void testMaximumLayoutSize() throws Exception {
        //container
        setComponentSizes(button,
                          Spring.constant(2, 5, 100),
                          Spring.constant(3, 6, 101));
        Constraints buttonConstraints = layout.getConstraints(button);
        buttonConstraints.setHeight(Spring.constant(33, 34, 35));
        Constraints containerConstraints = layout.getConstraints(container);
        containerConstraints.setConstraint(SpringLayout.SOUTH,
                                           buttonConstraints
                                               .getConstraint(SpringLayout.SOUTH));
        containerConstraints.setConstraint(SpringLayout.EAST,
                                           buttonConstraints
                                               .getConstraint(SpringLayout.EAST));
        containerConstraints.setConstraint(SpringLayout.WEST,
                                           Spring.constant(3));
        layout.layoutContainer(container);
        assertEquals(100, layout.maximumLayoutSize(container).width);
        assertEquals(35, layout.maximumLayoutSize(container).height);

        //putConstraints()
        assertEquals(Integer.MAX_VALUE
                     + button.getInsets().left
                     + button.getInsets().right,
                     layout.maximumLayoutSize(button).width);

        assertEquals(Integer.MAX_VALUE
                     + panel.getInsets().left
                     + panel.getInsets().right,
                     layout.maximumLayoutSize(panel).width);

        assertEquals(Integer.MAX_VALUE
                     + innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.maximumLayoutSize(innerButton).width);

        buttonConstraints.setX(Spring.constant(20));
        assertEquals(Integer.MAX_VALUE
                     + button.getInsets().left
                     + button.getInsets().right,
                     layout.maximumLayoutSize(button).width);
        assertEquals(0,
                     layout.getConstraint(SpringLayout.EAST, button)
                        .getPreferredValue());

        buttonConstraints.setWidth(Spring.constant(100));
        assertEquals(100
                     + button.getInsets().left
                     + button.getInsets().right,
                     layout.maximumLayoutSize(button).width);

        layout.getConstraints(innerButton).setX(Spring.constant(20));
        assertEquals(Integer.MAX_VALUE
                     + innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.maximumLayoutSize(innerButton).width);

        layout.getConstraints(innerButton).setWidth(Spring.constant(100));
        assertEquals(100
                     + innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.maximumLayoutSize(innerButton).width);

        layout.getConstraints(panel).setX(Spring.constant(20));
        assertEquals(Integer.MAX_VALUE
                     + panel.getInsets().left
                     + panel.getInsets().right,
                     layout.maximumLayoutSize(panel).width);

        assertEquals(0,
                     layout.getConstraint(SpringLayout.EAST, panel)
                        .getPreferredValue());

        layout.getConstraints(panel).setWidth(Spring.constant(100));
        assertEquals(100
                     + panel.getInsets().left
                     + panel.getInsets().right,
                     layout.maximumLayoutSize(panel).width);
        assertEquals(100,
                     layout.getConstraint(SpringLayout.EAST, panel)
                        .getPreferredValue());

        layout = new SpringLayout();
        container.setLayout(layout);
        layout.putConstraint(SpringLayout.SOUTH, button,
                             Spring.constant(15),
                             SpringLayout.NORTH, container);

        layout.putConstraint(SpringLayout.EAST, button,
                             Spring.constant(5),
                             SpringLayout.WEST, container);

        assertEquals(Integer.MAX_VALUE
                     + panel.getInsets().left
                     + panel.getInsets().right,
                     layout.maximumLayoutSize(panel).width);

        assertEquals(Integer.MAX_VALUE
                     + innerButton.getInsets().left
                     + innerButton.getInsets().right,
                     layout.maximumLayoutSize(innerButton).width);

        assertEquals(5
                     + button.getInsets().left
                     + button.getInsets().right,
                     layout.maximumLayoutSize(button).width);
    }

    public void testConstraints_Constraints() throws Exception {
        Constraints constraints;

        //SpringLayout.Constraints()
        constraints = new SpringLayout.Constraints();
        assertNull(constraints.getConstraint(SpringLayout.WEST));
        assertNull(constraints.getConstraint(SpringLayout.EAST));
        assertNull(constraints.getConstraint(SpringLayout.NORTH));
        assertNull(constraints.getConstraint(SpringLayout.SOUTH));
        assertNull(constraints.getWidth());
        assertNull(constraints.getHeight());
        assertNull(constraints.getX());
        assertNull(constraints.getY());

        //SpringLayout.Constraints(Spring x, Spring y)
        constraints = new SpringLayout.Constraints(Spring.constant(1),
                                                   Spring.constant(2));
        SpringTest.assertSizes(1, 1, 1,
                               constraints.getConstraint(SpringLayout.WEST));
        assertNull(constraints.getConstraint(SpringLayout.EAST));
        SpringTest.assertSizes(2, 2, 2,
                               constraints.getConstraint(SpringLayout.NORTH));
        assertNull(constraints.getConstraint(SpringLayout.SOUTH));
        assertNull(constraints.getWidth());
        assertNull(constraints.getHeight());
        SpringTest.assertSizes(1, 1, 1, constraints.getX());
        SpringTest.assertSizes(2, 2, 2, constraints.getY());

        //SpringLayout.Constraints(Spring x, Spring y,
        //                         Spring width, Spring height)
        constraints = new SpringLayout.Constraints(Spring.constant(1),
                                                   Spring.constant(2),
                                                   Spring.constant(10),
                                                   Spring.constant(20));
        SpringTest.assertSizes(1, 1, 1, 1,
                               constraints.getConstraint(SpringLayout.WEST));
        SpringTest.assertSizes(11, 11, 11, 11,
                               constraints.getConstraint(SpringLayout.EAST));
        SpringTest.assertSizes(2, 2, 2, 2,
                               constraints.getConstraint(SpringLayout.NORTH));
        SpringTest.assertSizes(22, 22, 22, 22,
                               constraints.getConstraint(SpringLayout.SOUTH));
        SpringTest.assertSizes(1, 1, 1, 1, constraints.getX());
        SpringTest.assertSizes(2, 2, 2, 2, constraints.getY());
        SpringTest.assertSizes(10, 10, 10, 10, constraints.getWidth());
        SpringTest.assertSizes(20, 20, 20, 20, constraints.getHeight());

        //SpringLayout.Constraints(Component c)
        setComponentSizes(button,
                          Spring.constant(1, 2, 3),
                          Spring.constant(11, 12, 13));
        constraints = new SpringLayout.Constraints(button);

        SpringTest.assertSizes(0, 0, 0, 0,
                               constraints.getConstraint(SpringLayout.WEST));
        SpringTest.assertSizes(1, 2, 3, 2,
                               constraints.getConstraint(SpringLayout.EAST));
        SpringTest.assertSizes(0, 0, 0, 0,
                               constraints.getConstraint(SpringLayout.NORTH));
        SpringTest.assertSizes(11, 12, 13, 12,
                               constraints.getConstraint(SpringLayout.SOUTH));
        SpringTest.assertSizes(0, 0, 0, 0, constraints.getX());
        SpringTest.assertSizes(0, 0, 0, 0, constraints.getY());

        SpringTest.assertSizes(1, 2, 3, 2, constraints.getWidth());
        SpringTest.assertSizes(11, 12, 13, 12, constraints.getHeight());
    }

    public void testConstraints_SetConstraint() throws Exception {
        Constraints constraints;
        layout.getConstraints(container).setConstraint(SpringLayout.WEST,
                                                      Spring.constant(5));

        layout.getConstraints(button).setConstraint(SpringLayout.WEST,
                                                    Spring.constant(6));
        layout.getConstraints(button).setX(Spring.constant(7));

        constraints = layout.getConstraints(textField);
        constraints.setX(Spring.constant(8));
        constraints.setWidth(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(30));

        layout.getConstraints(panel).setConstraint(SpringLayout.WEST,
                                                   Spring.constant(7));
        constraints.setWidth(Spring.constant(20));
        layout.getConstraints(panel).setConstraint(SpringLayout.EAST,
                                                   Spring.constant(17));

        constraints = layout.getConstraints(innerButton);
        constraints.setX(Spring.constant(18));
        constraints.setWidth(Spring.constant(20));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(50));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        assertEdges(layout.getConstraints(container),
                    0, 200, 0, 300);

        assertEdges(layout.getConstraints(button),
                    7, 12, 0, 5);

        assertEdges(layout.getConstraints(textField),
                    10, 30, 0, 10);

        if (isHarmony()) {
            assertEdges(layout.getConstraints(panel),
                        7, 17, 0, 3);
        } else {
            assertEdges(layout.getConstraints(panel),
                        14, 17, 0, 3);
        }

        assertEdges(layout.getConstraints(innerButton),
                    30, 50, 0, 10);

        component = new JPanel();
        setComponentSizes(component, Spring.constant(10), Spring.constant(5));
        layout.getConstraints(panel).setConstraint(SpringLayout.NORTH,
                                                   Spring.constant(1));
        assertEdges(layout.getConstraints(component),
                    0, 10, 0, 5);
    }

    public void testConstraints_GetConstraint() throws Exception {
        Constraints constraints;
        layout.getConstraints(container).setConstraint(SpringLayout.WEST,
                                                      Spring.constant(5));

        layout.getConstraints(button).setConstraint(SpringLayout.WEST,
                                                    Spring.constant(6));
        layout.getConstraints(button).setX(Spring.constant(7));

        constraints = layout.getConstraints(textField);
        constraints.setX(Spring.constant(8));
        constraints.setWidth(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(30));

        layout.getConstraints(panel).setConstraint(SpringLayout.WEST,
                                                   Spring.constant(7));
        constraints.setWidth(Spring.constant(20));
        layout.getConstraints(panel).setConstraint(SpringLayout.EAST,
                                                   Spring.constant(17));

        constraints = layout.getConstraints(innerButton);
        constraints.setX(Spring.constant(18));
        constraints.setWidth(Spring.constant(20));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(50));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        constraints = layout.getConstraints(container);
        SpringTest.assertSizes(0, 0, 0, 0,
                               constraints.getConstraint(SpringLayout.WEST));
        SpringTest.assertSizes(0, 0, Integer.MAX_VALUE, 200,
                               constraints.getConstraint(SpringLayout.EAST));

        constraints = layout.getConstraints(button);
        Spring west = constraints.getConstraint(SpringLayout.WEST);
        Spring east = constraints.getConstraint(SpringLayout.EAST);
        SpringTest.assertSizes(7, 7, 7, 7, west);
        SpringTest.assertSizes(9, 12, 107, 12, east);

        constraints.setConstraint(SpringLayout.WEST,
                                  Spring.constant(200));
        constraints.setConstraint(SpringLayout.EAST,
                                  Spring.constant(100));
        constraints.setWidth(Spring.constant(7));
        layout.layoutContainer(container);
        SpringTest.assertSizes(7, 7, 7, 7, west);
        SpringTest.assertSizes(9, 12, 107, 12, east);

        constraints.getConstraint("wrong value");

        constraints = layout.getConstraints(innerButton);
        west = constraints.getConstraint(SpringLayout.WEST);
        east = constraints.getConstraint(SpringLayout.EAST);
        SpringTest.assertSizes(30, 30, 30, 30, west);
        SpringTest.assertSizes(50, 50, 50, 50, east);

        constraints = new SpringLayout.Constraints();
        constraints.setConstraint(SpringLayout.EAST,
                                  Spring.constant(5));
        assertNull(constraints.getConstraint(SpringLayout.WEST));
        SpringTest.assertSizes(5, 5, 5,
                               constraints.getConstraint(SpringLayout.EAST));
        assertNull(constraints.getWidth());

        constraints.setY(Spring.constant(3));
        SpringTest.assertSizes(3, 3, 3, constraints.getY());
        SpringTest.assertSizes(3, 3, 3,
                               constraints.getConstraint(SpringLayout.NORTH));
        assertNull(constraints.getConstraint(SpringLayout.SOUTH));

        constraints = new SpringLayout.Constraints();
        constraints.setHeight(Spring.constant(3));
        assertNull(constraints.getY());
        assertNull(constraints.getConstraint(SpringLayout.SOUTH));
        assertNull(constraints.getConstraint(SpringLayout.NORTH));

        constraints = new SpringLayout.Constraints(Spring.constant(1),
                                                   Spring.constant(2));
        constraints.setWidth(Spring.constant(10));
        SpringTest.assertSizes(11, 11, 11,
                               constraints.getConstraint(SpringLayout.EAST));

    }

    public void testConstraints_SetHeight() throws Exception {
        layout.getConstraints(container).setHeight(Spring.constant(5));
        layout.getConstraints(button).setHeight(Spring.constant(6));
        layout.getConstraints(button).setHeight(Spring.constant(8));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setHeight(Spring.constant(8));
        constraints.setY(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(40));

        layout.getConstraints(panel).setHeight(Spring.constant(6));
        constraints.setY(Spring.constant(20));


        layout.getConstraints(innerButton).setHeight(Spring.constant(15));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        assertEquals(300, container.getHeight());
        assertEquals(8, button.getHeight());
        assertEquals(8, textField.getHeight());
        assertEquals(6, panel.getHeight());
        assertEquals(0, innerButton.getHeight());
    }

    public void testConstraints_GetHeight() throws Exception {
        layout.getConstraints(container).setHeight(Spring.constant(5));
        layout.getConstraints(button).setHeight(Spring.constant(6));
        layout.getConstraints(button).setHeight(Spring.constant(8));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setHeight(Spring.constant(8));
        constraints.setY(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(40));

        layout.getConstraints(panel).setHeight(Spring.constant(6));
        constraints.setY(Spring.constant(20));


        layout.getConstraints(innerButton).setHeight(Spring.constant(15));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        SpringTest.assertSizes(5, 5, 5,
                               layout.getConstraints(container).getHeight());
        SpringTest.assertSizes(8, 8, 8,
                               layout.getConstraints(button).getHeight());
        SpringTest.assertSizes(8, 8, 8,
                               layout.getConstraints(textField).getHeight());
        SpringTest.assertSizes(6, 6, 6,
                               layout.getConstraints(panel).getHeight());
        SpringTest.assertSizes(15, 15, 15,
                               layout.getConstraints(innerButton).getHeight());

        container.setSize(200, 300);
        layout.layoutContainer(button);
        SpringTest.assertSizes(8, 8, 8,
                               layout.getConstraints(button).getHeight());

    }

    public void testConstraints_SetWidth() throws Exception {
        layout.getConstraints(container).setWidth(Spring.constant(5));
        layout.getConstraints(button).setWidth(Spring.constant(6));
        layout.getConstraints(button).setWidth(Spring.constant(8));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setWidth(Spring.constant(8));
        constraints.setX(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(40));

        layout.getConstraints(panel).setWidth(Spring.constant(6));
        constraints.setX(Spring.constant(20));


        layout.getConstraints(innerButton).setWidth(Spring.constant(15));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        assertEquals(200, container.getWidth());
        assertEquals(8, button.getWidth());
        assertEquals(20, textField.getWidth());
        assertEquals(6, panel.getWidth());
        assertEquals(0, innerButton.getWidth());
    }

    public void testConstraints_GetWidth() throws Exception {
        layout.getConstraints(container).setWidth(Spring.constant(5));
        layout.getConstraints(button).setWidth(Spring.constant(6));
        layout.getConstraints(button).setWidth(Spring.constant(8));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setWidth(Spring.constant(8));
        constraints.setX(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(40));

        layout.getConstraints(panel).setWidth(Spring.constant(6));
        constraints.setX(Spring.constant(20));


        layout.getConstraints(innerButton).setWidth(Spring.constant(15));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        SpringTest.assertSizes(5, 5, 5,
                               layout.getConstraints(container).getWidth());
        SpringTest.assertSizes(8, 8, 8,
                               layout.getConstraints(button).getWidth());
        SpringTest.assertSizes(20, 20, 20,
                               layout.getConstraints(textField).getWidth());
        SpringTest.assertSizes(6, 6, 6,
                               layout.getConstraints(panel).getWidth());
        SpringTest.assertSizes(15, 15, 15,
                               layout.getConstraints(innerButton).getWidth());

        container.setSize(200, 300);
        layout.layoutContainer(button);
        SpringTest.assertSizes(8, 8, 8,
                               layout.getConstraints(button).getWidth());
    }

    public void testConstraints_SetX() throws Exception {
        layout.getConstraints(container).setX(Spring.constant(5));
        layout.getConstraints(button).setX(Spring.constant(6));
        layout.getConstraints(button).setX(Spring.constant(7));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setX(Spring.constant(8));
        constraints.setWidth(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(30));

        layout.getConstraints(panel).setX(Spring.constant(7));
        constraints.setWidth(Spring.constant(20));


        layout.getConstraints(innerButton).setX(Spring.constant(17));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        assertEquals(0, container.getX());
        assertEquals(7, button.getX());
        assertEquals(10, textField.getX());
        assertEquals(7, panel.getX());
        assertEquals(0, innerButton.getX());
    }

    public void testConstraints_GetX() throws Exception {
        layout.getConstraints(container).setX(Spring.constant(5));
        layout.getConstraints(button).setX(Spring.constant(6));
        layout.getConstraints(button).setX(Spring.constant(7));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setX(Spring.constant(8));
        constraints.setWidth(Spring.constant(10));
        constraints.setConstraint(SpringLayout.EAST, Spring.constant(30));

        layout.getConstraints(panel).setX(Spring.constant(7));
        constraints.setWidth(Spring.constant(20));


        layout.getConstraints(innerButton).setX(Spring.constant(17));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        SpringTest.assertSizes(0, 0, 0,
                               layout.getConstraints(container).getX());
        SpringTest.assertSizes(7, 7, 7,
                               layout.getConstraints(button).getX());
        SpringTest.assertSizes(10, 10, 10,
                               layout.getConstraints(textField).getX());
        SpringTest.assertSizes(7, 7, 7,
                               layout.getConstraints(panel).getX());
        SpringTest.assertSizes(17, 17, 17,
                               layout.getConstraints(innerButton).getX());

        container.setSize(200, 300);
        layout.layoutContainer(button);
        SpringTest.assertSizes(0, 0, 0,
                               layout.getConstraints(button).getX());
    }

    public void testConstraints_SetY() throws Exception {
        layout.getConstraints(container).setY(Spring.constant(5));
        layout.getConstraints(button).setY(Spring.constant(6));
        layout.getConstraints(button).setY(Spring.constant(8));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setY(Spring.constant(8));
        constraints.setHeight(Spring.constant(10));
        constraints.setConstraint(SpringLayout.SOUTH, Spring.constant(40));

        layout.getConstraints(panel).setY(Spring.constant(6));
        constraints.setHeight(Spring.constant(20));


        layout.getConstraints(innerButton).setY(Spring.constant(15));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        assertEquals(0, container.getY());
        assertEquals(8, button.getY());
        assertEquals(20, textField.getY());
        assertEquals(6, panel.getY());
        assertEquals(0, innerButton.getY());
    }

    public void testConstraints_GetY() throws Exception {
        layout.getConstraints(container).setY(Spring.constant(5));
        layout.getConstraints(button).setY(Spring.constant(6));
        layout.getConstraints(button).setY(Spring.constant(8));

        Constraints constraints = layout.getConstraints(textField);
        constraints.setY(Spring.constant(8));
        constraints.setHeight(Spring.constant(10));
        constraints.setConstraint(SpringLayout.SOUTH, Spring.constant(40));

        layout.getConstraints(panel).setY(Spring.constant(6));
        constraints.setHeight(Spring.constant(20));


        layout.getConstraints(innerButton).setY(Spring.constant(15));

        container.setSize(200, 300);
        layout.layoutContainer(container);

        SpringTest.assertSizes(0, 0, 0,
                               layout.getConstraints(container).getY());
        SpringTest.assertSizes(8, 8, 8,
                               layout.getConstraints(button).getY());
        SpringTest.assertSizes(20, 20, 20,
                               layout.getConstraints(textField).getY());
        SpringTest.assertSizes(6, 6, 6,
                               layout.getConstraints(panel).getY());
        SpringTest.assertSizes(15, 15, 15,
                               layout.getConstraints(innerButton).getY());

        container.setSize(200, 300);
        layout.layoutContainer(button);
        SpringTest.assertSizes(0, 0, 0, layout.getConstraints(button).getY());
    }

    public static void setComponentSizes(final Component component,
                                   final Spring width, final Spring height) {
       component.setMinimumSize(new Dimension(width.getMinimumValue(),
                                              height.getMinimumValue()));
       component.setPreferredSize(new Dimension(width.getPreferredValue(),
                                                height.getPreferredValue()));
       component.setMaximumSize(new Dimension(width.getMaximumValue(),
                                              height.getMaximumValue()));
    }

    private void assertBounds(final int x, final int y,
                              final int width, final int height,
                              final Rectangle bounds) {
        assertLocation(x, y, bounds);
        assertSize(width, height, bounds);
    }

    private void assertEdges(final Constraints constraints,
                                 final int west, final int east,
                                 final int north, final int south) {
            assertEquals(west,
                         constraints.getConstraint(SpringLayout.WEST).getValue());
            assertEquals(east,
                         constraints.getConstraint(SpringLayout.EAST).getValue());
            assertEquals(north,
                         constraints.getConstraint(SpringLayout.NORTH).getValue());
            assertEquals(south,
                         constraints.getConstraint(SpringLayout.SOUTH).getValue());
        }
    private void assertLocation(final int x, final int y,
                              final Rectangle bounds) {
        assertEquals(x, bounds.x);
        assertEquals(y, bounds.y);
    }

    private void assertSize(final int width, final int height,
                            final Rectangle bounds) {
        assertEquals(width, bounds.width);
        assertEquals(height, bounds.height);
    }
}