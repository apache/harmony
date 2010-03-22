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
import java.awt.Dimension;

public class SpringTest extends SwingTestCase {
    private Spring spring;
    private Spring spring1;
    private Spring spring2;

    private Component component;

    private Marker componentGetMinimumSizeCalled;
    private Marker componentGetPreferedSizeCalled;
    private Marker componentGetMaximumSizeCalled;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        componentGetMinimumSizeCalled = new Marker();
        componentGetPreferedSizeCalled = new Marker();
        componentGetMaximumSizeCalled = new Marker();

        component = new JButton("test");
        initComponentSizes(component);
    }

    public void testConstant() throws Exception {
        Spring spring;
        spring = Spring.constant(5);
        assertSizes(5, 5, 5, 5, spring);
        spring.setValue(10);
        assertSizes(5, 5, 5, 10, spring);
        spring = Spring.constant(1, 2, 3);
        assertSizes(1, 2, 3, 2, spring);
        spring.setValue(10);
        assertSizes(1, 2, 3, 10, spring);
        spring1 = Spring.constant(5);
        spring2 = Spring.constant(5);
        assertFalse(spring1.equals(spring2));
    }

    public void testConstant_UNSET() throws Exception {
        spring = Spring.constant(5);
        spring.setValue(1);
        assertEquals(1, spring.getValue());
        spring.setValue(Spring.UNSET);
        assertEquals(5, spring.getValue());
        spring = Spring.constant(2, 5, 6);
        spring.setValue(1);
        assertEquals(1, spring.getValue());
        spring.setValue(Spring.UNSET);
        assertEquals(spring.getPreferredValue(), spring.getValue());
    }

    public void testConstant_Overflow() throws Exception {
        spring = Spring.constant(4, 5, 6);
        spring.setValue(Integer.MAX_VALUE - 5);
        assertEquals(Integer.MAX_VALUE - 5, spring.getValue());
    }

    public void testMinus() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Spring.minus(null).getValue();
            }
        });
        spring1 = Spring.constant(12, 13, 15);
        spring = Spring.minus(spring1);
        assertSizes(-spring1.getMaximumValue(), -spring1.getPreferredValue(),
                    -spring1.getMinimumValue(), -spring1.getValue(), spring);
        component = new JButton("Test");
        setComponentSizes(component, new Dimension(59, 25));
        spring1 = Spring.width(component);
        spring = Spring.minus(spring1);
        assertSizes(-spring1.getMaximumValue(), -spring1.getPreferredValue(),
                    -spring1.getMinimumValue(), -spring1.getValue(), spring);
        assertEquals(-spring.getMinimumValue(), component.getMinimumSize().width);
        component.setMinimumSize(new Dimension(111, 112));
        assertEquals(-spring.getMaximumValue(), component.getMinimumSize().width);
        spring.setValue(333);
        assertEquals(-333, spring1.getValue());
        spring1.setValue(1);
        assertSizes(-59, -59, -111, spring);
        component.setMinimumSize(new Dimension(101, 201));
        component.setPreferredSize(new Dimension(102, 202));
        component.setMaximumSize(new Dimension(103, 203));
        assertSizes(-103, -102, -101, spring);
    }

    public void testMinus_Overflow() throws Exception {
        initComponentSizes(component);
        spring = Spring.minus(Spring.width(component));
        spring.setValue(Integer.MAX_VALUE - 5);
        if (isHarmony()) {
            assertEquals(Short.MAX_VALUE, spring.getValue());
        } else {
            assertEquals(Integer.MAX_VALUE - 5, spring.getValue());
        }
        spring = Spring.minus(Spring.width(component));
        component.setMinimumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MIN_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(-Integer.MAX_VALUE, spring.getMaximumValue());
        }
        spring = Spring.minus(Spring.width(component));
        component.setMinimumSize(new Dimension(Integer.MIN_VALUE, Integer.MIN_VALUE));
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MAX_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(Integer.MIN_VALUE, spring.getMaximumValue());
        }
    }

    public void testMinus_SizesCashing() throws Exception {
        spring1 = Spring.width(component);
        spring = Spring.minus(spring1);
        if (isHarmony()) {
            assertSizes(Short.MIN_VALUE, -75, -2, spring);
        } else {
            assertSizes(-1 * Short.MAX_VALUE, -75, -2, spring);
        }
        setComponentSizes(component, new Dimension(1, 1));
        assertSizes(-1, -1, -1, spring);
        spring1.setValue(0);
        assertEquals(0, spring.getValue());
        assertEquals(0, spring1.getValue());
        spring.setValue(3);
        assertEquals(3, spring.getValue());
        assertEquals(-3, spring1.getValue());
    }

    public void testMinus_UNSET() throws Exception {
        spring1 = Spring.constant(4, 5, 6);
        spring1.setValue(1);
        spring = Spring.minus(spring1);
        spring.setValue(Spring.UNSET);
        assertSizes(4, 5, 6, 5, spring1);
        spring1 = Spring.constant(4, 5, 6);
        spring2 = Spring.sum(Spring.constant(5), spring1);
        spring = Spring.minus(spring2);
        spring.setValue(Spring.UNSET);
        spring.getMaximumValue();
        spring.setValue(Spring.UNSET);
        spring.setValue(10);
        assertEquals(-15, spring1.getValue());
        spring1.setValue(100);
        assertEquals(100, spring1.getValue());
        spring.setValue(10);
        assertEquals(-15, spring1.getValue());
    }

    public void testSum() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Spring.max(null, Spring.constant(11)).getValue();
            }
        });
        Component c1 = new JLabel("ss");
        c1.setPreferredSize(new Dimension(15, 15));
        c1.setMaximumSize(new Dimension(15, 15));
        c1.setMinimumSize(new Dimension(15, 15));
        Component c2 = new JTextField("ss");
        c2.setPreferredSize(new Dimension(19, 19));
        c2.setMinimumSize(new Dimension(19, 19));
        Spring max = Spring.max(Spring.max(Spring.constant(0), Spring.height(c1)),
                                Spring.height(c2));
        Spring max6 = Spring.sum(max, Spring.constant(6));
        spring1 = Spring.sum(Spring.sum(Spring.sum(Spring.constant(6), max6), max6), max6);

        spring2 = max6;
        spring = Spring.sum(spring1, spring2);
        spring.setValue(418);
        assertEquals(315, spring1.getValue()); //314
        assertEquals(103, spring2.getValue()); //103

        spring1 = Spring.constant(24, 81, 3 * Spring.TRIMMED_MAX_VALUE);
        spring2 = Spring.constant(6, 25, Spring.TRIMMED_MAX_VALUE);
        spring = Spring.sum(spring1, spring2);
        spring.setValue(418);
        assertEquals(314, spring1.getValue()); //314
        assertEquals(104, spring2.getValue()); //103

        checkStrains(Spring.constant(2), Spring.constant(1, 2, 3), 2, 2, 0);
        checkStrains(Spring.constant(1, 2, 3), Spring.constant(2), 2, 0, 2);
        checkStrains(Spring.constant(2), Spring.constant(1, 2, 3), 4, 2, 2);
        checkStrains(Spring.constant(1, 2, 3), Spring.constant(2), 4, 2, 2);
        checkStrains(Spring.constant(2), Spring.constant(1, 2, 3), 6, 2, 4);
        checkStrains(Spring.constant(1, 2, 3), Spring.constant(2), 6, 4, 2);
        checkStrains(Spring.constant(2, 2, 1), Spring.constant(2, 2, 4), 2, 2, 0);
        checkStrains(Spring.constant(2, 2, 4), Spring.constant(2, 2, 1), 2, 2, 0);
        checkStrains(Spring.constant(2, 2, 1), Spring.constant(2, 2, 4), 4, 2, 2);
        checkStrains(Spring.constant(2, 2, 4), Spring.constant(2, 2, 1), 4, 2, 2);
        checkStrains(Spring.constant(2, 2, 1), Spring.constant(2, 2, 4), 6, 0, 6);
        checkStrains(Spring.constant(2, 2, 4), Spring.constant(2, 2, 1), 6, 6, 0);
        checkStrains(Spring.constant(2, 2, 2), Spring.constant(2, 2, 2), 2, 2, 0);
        checkStrains(Spring.constant(2, 2, 2), Spring.constant(2, 2, 2), 4, 2, 2);
        checkStrains(Spring.constant(2, 2, 2), Spring.constant(2, 2, 2), 6, 2, 4);
        checkStrains(Spring.constant(1, 3, 3), Spring.constant(1, 2, 2), 3, 2, 1);
        checkStrains(Spring.constant(1, 2, 2), Spring.constant(1, 3, 3), 3, 2, 1);
        checkStrains(Spring.constant(1, 3, 3), Spring.constant(1, 2, 2), 5, 3, 2);
        checkStrains(Spring.constant(1, 2, 2), Spring.constant(1, 3, 3), 5, 2, 3);
        checkStrains(Spring.constant(1, 3, 3), Spring.constant(1, 2, 2), 7, 3, 4);
        checkStrains(Spring.constant(1, 2, 2), Spring.constant(1, 3, 3), 7, 2, 5);
        checkStrains(Spring.constant(1, 3, 1), Spring.constant(1, 1, 3), 2, 1, 1);
        checkStrains(Spring.constant(1, 1, 3), Spring.constant(1, 3, 1), 2, 1, 1);
        checkStrains(Spring.constant(1, 3, 1), Spring.constant(1, 1, 3), 4, 3, 1);
        checkStrains(Spring.constant(1, 1, 3), Spring.constant(1, 3, 1), 4, 1, 3);
        if (isHarmony()) {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(1, 1, 3), 6,
                         Spring.TRIMMED_MAX_VALUE + 3, 6 - Spring.TRIMMED_MAX_VALUE - 3);
        } else {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(1, 1, 3), 6,
                         Spring.UNSET + 3, 6 - Spring.UNSET - 3);
        }
        checkStrains(Spring.constant(1, 1, 3), Spring.constant(1, 3, 1), 6, 1, 5);
        checkStrains(Spring.constant(1, 3, 1), Spring.constant(0, 1, 3), 2, 2, 0);
        checkStrains(Spring.constant(0, 1, 3), Spring.constant(1, 3, 1), 2, 1, 1);
        checkStrains(Spring.constant(1, 3, 1), Spring.constant(0, 1, 3), 4, 3, 1);
        checkStrains(Spring.constant(0, 1, 3), Spring.constant(1, 3, 1), 4, 1, 3);
        checkStrains(Spring.constant(0, 3, 1), Spring.constant(1, 1, 3), 4, 3, 1);
        if (isHarmony()) {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(0, 1, 3), 6,
                         Spring.TRIMMED_MAX_VALUE + 3, 6 - Spring.TRIMMED_MAX_VALUE - 3);
        } else {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(0, 1, 3), 6,
                         Spring.UNSET + 3, 6 - Spring.UNSET - 3);
        }
        checkStrains(Spring.constant(0, 1, 3), Spring.constant(1, 3, 1), 6, 1, 5);
        if (isHarmony()) {
            checkStrains(Spring.constant(0, 3, 1), Spring.constant(1, 1, 3), 6,
                         Spring.TRIMMED_MAX_VALUE + 3, 6 - Spring.TRIMMED_MAX_VALUE - 3);
        } else {
             checkStrains(Spring.constant(0, 3, 1), Spring.constant(1, 1, 3), 6,
                          Spring.UNSET + 3, 6 - Spring.UNSET - 3);
        }
        checkStrains(Spring.constant(3, 1, 3), Spring.constant(1, 3, 1), 2, 1, 1);
        if (isHarmony()) {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(3, 1, 3), 2,
                         Spring.TRIMMED_MAX_VALUE + 3, 2 - Spring.TRIMMED_MAX_VALUE - 3);
        } else {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(3, 1, 3), 2,
                         Spring.UNSET + 3, 2 - Spring.UNSET - 3);
        }
        checkStrains(Spring.constant(3, 1, 3), Spring.constant(1, 3, 1), 4, 1, 3);
        checkStrains(Spring.constant(1, 3, 1), Spring.constant(3, 1, 3), 4, 3, 1);
        checkStrains(Spring.constant(3, 1, 3), Spring.constant(1, 3, 1), 6, 1, 5);
        if (isHarmony()) {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(3, 1, 3), 6,
                         Spring.TRIMMED_MAX_VALUE + 3, 6 - Spring.TRIMMED_MAX_VALUE - 3);
        } else {
            checkStrains(Spring.constant(1, 3, 1), Spring.constant(3, 1, 3), 6,
                         Spring.UNSET + 3, 6 - Spring.UNSET - 3);
        }
        checkStrains(Spring.constant(2, 2, 5), Spring.constant(3), 3, 2, 1);
        checkStrains(Spring.constant(3), Spring.constant(2, 2, 5), 3, 3, 0);
        checkStrains(Spring.constant(2, 2, 5), Spring.constant(3), 5, 2, 3);
        checkStrains(Spring.constant(3), Spring.constant(2, 2, 5), 5, 3, 2);
        checkStrains(Spring.constant(2, 2, 5), Spring.constant(3), 7, 4, 3);
        checkStrains(Spring.constant(3), Spring.constant(2, 2, 5), 7, 3, 4);
        if (isHarmony()) {
            checkStrains(Spring.constant(2, 4, 3), Spring.constant(3, 1, 0), 2,
                         Spring.TRIMMED_MAX_VALUE + 4, 2 - Spring.TRIMMED_MAX_VALUE - 4);
        } else {
            checkStrains(Spring.constant(2, 4, 3), Spring.constant(3, 1, 0), 2,
                         Spring.UNSET + 4, 2 - Spring.UNSET - 4);
        }
        if (isHarmony()) {
            checkStrains(Spring.constant(2, 4, 3), Spring.constant(3, 1, 0), 2,
                         Spring.TRIMMED_MAX_VALUE + 4, 2 - Spring.TRIMMED_MAX_VALUE - 4);
        } else {
            checkStrains(Spring.constant(2, 4, 3), Spring.constant(3, 1, 0), 2,
                         Spring.UNSET + 4, 2 - Spring.UNSET - 4);
        }
        checkStrains(Spring.constant(2, 3, 4), Spring.constant(3, 1, 1), 2, 5, -3);

        if (isHarmony()) {
            checkStrains(Spring.constant(2, 3, 3), Spring.constant(3, 1, 0), 2, 5, -3);
        } else {
            checkStrains(Spring.constant(2, 3, 3), Spring.constant(3, 1, 0), 2, 3, -1);
        }
    }

    public void testSum_Overflow() throws Exception {
        spring1 = Spring.constant(0);
        spring2 = Spring.width(component);
        spring = Spring.sum(spring1, spring2);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Short.MAX_VALUE, spring.getMaximumValue());
        spring1 = Spring.constant(4, 5, 6);
        spring = Spring.sum(spring1, spring2);
        assertEquals(Short.MAX_VALUE + 6, spring.getMaximumValue());
        spring1 = Spring.constant(4, 5, Integer.MAX_VALUE);
        spring = Spring.sum(spring1, spring2);
        assertEquals(Integer.MAX_VALUE + Short.MAX_VALUE, spring.getMaximumValue());
        component.setMaximumSize(new Dimension(Integer.MIN_VALUE, Integer.MIN_VALUE));
        spring1 = Spring.constant(4, 5, -6);
        spring = Spring.sum(spring1, spring2);
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MIN_VALUE - 6, spring.getMaximumValue());
        } else {
            assertEquals(Integer.MIN_VALUE - 6, spring.getMaximumValue());
        }
        spring1 = Spring.constant(4, 5, Integer.MIN_VALUE);
        spring = Spring.sum(spring1, spring2);
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MIN_VALUE + Integer.MIN_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(0, spring.getMaximumValue());
        }
    }

    public void testSum_SizesCashing() throws Exception {
        spring1 = Spring.constant(4, 5, 6);
        spring2 = Spring.width(component);
        spring = Spring.sum(spring1, spring2);
        assertSizes(6, 80, Short.MAX_VALUE + 6, spring);
        setComponentSizes(component, new Dimension(1, 1));
        assertSizes(6, 80, Short.MAX_VALUE + 6, spring);
        spring2.setValue(0);
        assertEquals(5, spring.getValue());
        assertEquals(5, spring1.getValue());
        assertEquals(0, spring2.getValue());
        spring.setValue(3);
        assertSizes(6, 80, Short.MAX_VALUE + 6, spring);
        assertEquals(3, spring.getValue());
        assertEquals(4, spring1.getValue());
        assertEquals(-1, spring2.getValue());
        assertSizes(6, 80, Short.MAX_VALUE + 6, spring);
        spring1.setValue(3);
        assertEquals(3, spring.getValue());
        assertSizes(6, 80, Short.MAX_VALUE + 6, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(5, 6, 7, 6, spring);
        spring1.setValue(10);
        assertSizes(5, 6, 7, 6, spring);
        spring.setValue(Spring.UNSET);
        spring1.setValue(10);
        assertSizes(5, 6, 7, 11, spring);
        spring1.setValue(100);
        assertSizes(5, 6, 7, 11, spring);
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(10, 20));
        assertSizes(5, 15, 7, 15, spring);
        component.setPreferredSize(new Dimension(100, 200));
        assertSizes(5, 15, 7, 15, spring);
        spring.setValue(Spring.UNSET);
        component.setMinimumSize(new Dimension(10, 20));
        assertSizes(14, 105, 7, 105, spring);
        component.setMinimumSize(new Dimension(100, 200));
        assertSizes(14, 105, 7, 105, spring);
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(10, 20));
        component.setMinimumSize(new Dimension(30, 40));
        assertSizes(34, 15, 7, spring);
        component.setPreferredSize(new Dimension(100, 200));
        assertSizes(34, 15, 7, spring);
        spring.getValue();
        component.setMinimumSize(new Dimension(300, 400));
        assertSizes(34, 15, 7, 105, spring);
        spring.setValue(Spring.UNSET);
        spring.getValue();
        assertEquals(105, spring.getPreferredValue());
        component.setMinimumSize(new Dimension(30, 40));
        assertEquals(34, spring.getMinimumValue());
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(10, 20));
        component.setMinimumSize(new Dimension(30, 40));
        assertSizes(34, 15, 7, spring);
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(100, 200));
        assertSizes(34, 105, 7, spring);
    }

    public void testSum_UNSET() throws Exception {
        spring1 = Spring.constant(2, 4, 5);
        spring2 = Spring.constant(5);
        spring1.setValue(4);
        spring2.setValue(5);
        spring = Spring.sum(spring1, spring2);
        spring.getValue();
        spring.setValue(Spring.UNSET);
        assertSizes(2, 4, 5, 4, spring1);
        assertSizes(5, 5, 5, 5, spring2);
        spring.setValue(Spring.UNSET);
        spring.setValue(100);
        assertSizes(2, 4, 5, 95, spring1);
        assertSizes(5, 5, 5, 5, spring2);
        assertSizes(7, 9, 10, 100, spring);
        spring1.setValue(1);
        spring2.setValue(1);
        spring.setValue(Spring.UNSET);
        spring.setValue(-10);
        assertSizes(2, 4, 5, -15, spring1);
        assertSizes(5, 5, 5, 5, spring2);
        assertSizes(7, 9, 10, -10, spring);
        spring = Spring.sum(spring1, spring2);
        spring1.setValue(1);
        spring2.setValue(1);
        spring = Spring.sum(spring1, spring2);
        assertSizes(7, 9, 10, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(2, 4, 5, 1, spring1);
        assertSizes(5, 5, 5, 1, spring2);
        assertSizes(7, 9, 10, 2, spring);
        spring1 = Spring.constant(4, 5, 6);
        spring2 = Spring.width(component);
        component.setMinimumSize(new Dimension(4, 14));
        component.setPreferredSize(new Dimension(5, 15));
        component.setMaximumSize(new Dimension(6, 16));
        spring = Spring.sum(spring1, spring2);
        spring1.setValue(1);
        spring2.setValue(2);
        spring.setValue(3);
        assertEquals(2, spring1.getValue());
        assertEquals(1, spring2.getValue());
        component.setMaximumSize(new Dimension(60, 160));
        spring.setValue(3);
        assertEquals(2, spring1.getValue());
        assertEquals(1, spring2.getValue());
    }

    public void testMax() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Spring.max(null, Spring.constant(11)).getValue();
            }
        });

        spring1 = Spring.constant(12, 13, 15);
        spring2 = Spring.constant(11, 11, 13);
        spring = Spring.max(spring1, spring2);
        spring.setValue(100);
        assertEquals(100, spring1.getValue());
        assertEquals(11, spring2.getValue());
        assertEquals(100, spring.getValue());

        spring.setValue(10);
        assertEquals(10, spring1.getValue());
        assertEquals(10, spring2.getValue());
        assertEquals(10, spring.getValue());

        spring.setValue(Spring.UNSET);
        spring.setValue(10);
        assertEquals(10, spring1.getValue());
        assertEquals(10, spring2.getValue());
        assertEquals(10, spring.getValue());

        spring1 = Spring.constant(12, 13, 15);
        spring2 = Spring.constant(11, 12, 13);
        spring = Spring.max(spring1, spring2);
        assertEquals(spring.getMaximumValue(), Math.max(spring1.getMaximumValue(), spring2
                .getMaximumValue()));
        assertEquals(spring.getValue(), Math.max(spring1.getValue(), spring2.getValue()));
        spring.setValue(335);
        assertEquals(335, spring.getValue());
        if (spring1.getValue() > spring2.getValue()) {
            assertEquals(335, spring1.getValue());
        } else {
            assertEquals(335, spring2.getValue());
        }

        spring1 = Spring.constant(6, 13, 24);
        spring2 = Spring.constant(11, 12, 13);
        spring = Spring.max(spring1, spring2);
        assertSizes(11, 13, 24, 13, spring);
    }

    public void testMax_SizesCashing() throws Exception {
        spring1 = Spring.constant(5);
        spring2 = Spring.width(component);
        spring = Spring.max(spring1, spring2);
        assertSizes(5, 75, Short.MAX_VALUE, spring);
        setComponentSizes(component, new Dimension(1, 1));
        assertSizes(5, 75, Short.MAX_VALUE, spring);
        spring2.setValue(0);
        assertEquals(5, spring.getValue());
        assertEquals(5, spring1.getValue());
        assertEquals(0, spring2.getValue());
        spring.setValue(3);
        assertSizes(5, 75, Short.MAX_VALUE, spring);
        assertEquals(3, spring.getValue());
        assertEquals(3, spring1.getValue());
        assertEquals(1, spring2.getValue());
        assertSizes(5, 75, Short.MAX_VALUE, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(5, 5, 5, 5, spring);
        spring1.setValue(10);
        spring2.setValue(10);
        assertSizes(5, 5, 5, 5, spring);
        spring.setValue(Spring.UNSET);
        spring1.setValue(10);
        assertSizes(5, 5, 5, 10, spring);
        spring1.setValue(100);
        assertSizes(5, 5, 5, 10, spring);
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(10, 20));
        assertSizes(5, 10, 5, 10, spring);
        component.setPreferredSize(new Dimension(100, 200));
        assertSizes(5, 10, 5, 10, spring);
        spring.setValue(Spring.UNSET);
        component.setMinimumSize(new Dimension(10, 20));
        assertSizes(10, 100, 5, 100, spring);
        component.setMinimumSize(new Dimension(100, 200));
        assertSizes(10, 100, 5, 100, spring);
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(10, 20));
        component.setMinimumSize(new Dimension(30, 40));
        assertSizes(30, 10, 5, spring);
        component.setPreferredSize(new Dimension(100, 200));
        assertSizes(30, 10, 5, spring);
        spring.getValue();
        component.setMinimumSize(new Dimension(300, 400));
        assertSizes(30, 10, 5, 100, spring);
        spring.setValue(Spring.UNSET);
        spring.getValue();
        assertEquals(100, spring.getPreferredValue());
        component.setMinimumSize(new Dimension(30, 40));
        assertEquals(30, spring.getMinimumValue());
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(10, 20));
        component.setMinimumSize(new Dimension(30, 40));
        assertSizes(30, 10, 5, spring);
        spring.setValue(Spring.UNSET);
        component.setPreferredSize(new Dimension(100, 200));
        assertSizes(30, 100, 5, spring);
    }

    public void testMax_Overlow() throws Exception {
        spring1 = Spring.constant(6, 13, 24);
        spring2 = Spring.constant(11, 12, Integer.MAX_VALUE);
        spring = Spring.max(spring1, spring2);
        if (isHarmony()) {
            assertSizes(11, 13, Spring.TRIMMED_MAX_VALUE, 13, spring);
        } else {
            assertSizes(11, 13, Integer.MAX_VALUE, 13, spring);
        }
        spring1 = Spring.constant(6, 13, Integer.MAX_VALUE);
        spring2 = Spring.constant(11, 12, 13);
        spring = Spring.max(spring1, spring2);
        if (isHarmony()) {
            assertSizes(11, 13, Spring.TRIMMED_MAX_VALUE, 13, spring);
        } else {
            assertSizes(11, 13, Integer.MAX_VALUE, 13, spring);
        }
    }

    public void testMax_UNSET() throws Exception {
        spring1 = Spring.constant(5);
        spring2 = Spring.constant(4);
        spring1.setValue(1);
        spring2.setValue(1);
        spring = Spring.max(spring1, spring2);
        spring.getValue();
        spring.setValue(Spring.UNSET);
        assertSizes(5, 5, 5, 5, spring1);
        assertSizes(4, 4, 4, 4, spring2);
        spring.setValue(Spring.UNSET);
        spring.setValue(10);
        assertSizes(5, 5, 5, 10, spring1);
        assertSizes(4, 4, 4, 4, spring2);
        spring1.setValue(1);
        spring2.setValue(1);
        spring = Spring.max(spring1, spring2);
        assertSizes(5, 5, 5, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(5, 5, 5, 1, spring1);
        assertSizes(4, 4, 4, 1, spring2);
        spring1 = Spring.constant(4, 4, 6);
        spring2 = Spring.constant(4, 5, 5);
        spring = Spring.max(spring1, spring2);
        spring1.setValue(2);
        spring2.setValue(1);
        spring.setValue(Math.max(spring1.getPreferredValue(), spring2.getPreferredValue()) + 5);
        assertEquals(spring1.getPreferredValue(), spring1.getValue());
        assertEquals(10, spring2.getValue());
        spring1 = Spring.constant(4, 5, 6);
        spring2 = Spring.width(component);
        component.setMinimumSize(new Dimension(4, 14));
        component.setPreferredSize(new Dimension(5, 15));
        component.setMaximumSize(new Dimension(6, 16));
        spring = Spring.max(spring1, spring2);
        spring1.setValue(2);
        spring2.setValue(1);
        spring.setValue(6);
        assertSizes(4, 5, 6, 6, spring1);
        assertSizes(4, 5, 6, 5, spring2);
        component.setPreferredSize(new Dimension(25, 35));
        spring.setValue(6);
        assertSizes(4, 5, 6, 5, spring1);
        assertSizes(4, 25, 6, 6, spring2);
    }

    public void testScale() {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Spring.scale(null, 1).getValue();
            }
        });
        testScale(1);
        testScale(0);
        testScale(5);
        testScale(1.3f);
        testScale(1.5f);
        testScale(1.7f);
        testScale(-5);
        testScale(-1.3f);
        testScale(-1.5f);
        testScale(-1.7f);
    }

    public void testScale_Overflow() throws Exception {
        component = new JTextField();
        spring = Spring.scale(Spring.width(component), 2f);
        spring.setValue(2147483642);
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MAX_VALUE, spring.getValue());
        } else {
            assertEquals(2147483647, spring.getValue());
        }
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MAX_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(2 * Short.MAX_VALUE, spring.getMaximumValue());
        }
        spring = Spring.scale(Spring.width(component), 2f);
        component.setMaximumSize(new Dimension(Integer.MIN_VALUE, Integer.MIN_VALUE));
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MIN_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(Integer.MIN_VALUE, spring.getMaximumValue());
        }
        spring = Spring.scale(Spring.width(component), -2f);
        component.setMaximumSize(new Dimension(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(-8, spring.getMaximumValue());
    }

    public void testScale_SizesCashing() throws Exception {
        spring1 = Spring.width(component);
        spring = Spring.scale(spring1, 2);
        if (isHarmony()) {
            assertSizes(4, 150, Short.MAX_VALUE, spring);
        } else {
            assertSizes(4, 150, 2 * Short.MAX_VALUE, spring);
        }
        setComponentSizes(component, new Dimension(1, 1));
        assertSizes(2, 2, 2, spring);
        spring1.setValue(1);
        assertEquals(2, spring.getValue());
        assertEquals(1, spring1.getValue());
        spring.setValue(6);
        assertSizes(2, 2, 2, spring);
        assertEquals(6, spring.getValue());
        assertEquals(3, spring1.getValue());
        spring.setValue(Spring.UNSET);
        assertSizes(2, 2, 2, spring);
        assertEquals(spring.getPreferredValue(), spring.getValue());
        // No caching
    }

    public void testScale_UNSET() throws Exception {
        spring1 = Spring.width(component);
        spring1.setValue(10);
        spring = Spring.scale(spring1, 2);
        component.setMinimumSize(new Dimension(11, 12));
        component.setPreferredSize(new Dimension(13, 14));
        component.setMaximumSize(new Dimension(15, 16));
        assertSizes(22, 26, 30, 20, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(22, 26, 30, 2 * spring1.getPreferredValue(), spring);
        component.setPreferredSize(new Dimension(101, 102));
        assertEquals(101, spring1.getValue());
        assertEquals(202, spring.getValue());
        component.setPreferredSize(new Dimension(201, 202));
        assertEquals(201, spring1.getValue());
        assertEquals(402, spring.getValue());
        component.setPreferredSize(new Dimension(201, 202));
        assertEquals(201, spring1.getValue());
        assertEquals(402, spring.getValue());
        spring1.setValue(11);
        assertEquals(22, spring.getValue());
    }

    public void testWidth() {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Spring.width(null).getValue();
            }
        });
        spring1 = Spring.width(new JButton());
        spring2 = Spring.width(new JButton());
        assertFalse(spring1.equals(spring2));

        component = new JButton();
        spring1 = Spring.width(component);
        spring2 = Spring.width(component);
        assertFalse(spring1.equals(spring2));

        final Marker getMinimumSizeCalled = new Marker();
        final Marker getMaximumSizeCalled = new Marker();
        final Marker getPreferedSizeCalled = new Marker();
        component = new JButton("Test") {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Dimension getMinimumSize() {
                getMinimumSizeCalled.setOccurred();
                return super.getMinimumSize();
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                getPreferedSizeCalled.setOccurred();
                return super.getPreferredSize();
            }

            @Override
            public java.awt.Dimension getMaximumSize() {
                getMaximumSizeCalled.setOccurred();
                return super.getMaximumSize();
            }
        };
        initComponentSizes(component);
        spring = Spring.width(component);
        assertFalse(getPreferedSizeCalled.isOccurred());
        assertFalse(getMinimumSizeCalled.isOccurred());
        assertFalse(getMaximumSizeCalled.isOccurred());
        getPreferedSizeCalled.reset();
        spring.getPreferredValue();
        assertTrue(getPreferedSizeCalled.isOccurred());
        getPreferedSizeCalled.reset();
        spring.getPreferredValue();
        assertTrue(getPreferedSizeCalled.isOccurred());
        getMinimumSizeCalled.reset();
        spring.getMinimumValue();
        assertTrue(getMinimumSizeCalled.isOccurred());
        getMaximumSizeCalled.reset();
        spring.getMaximumValue();
        assertTrue(getMaximumSizeCalled.isOccurred());
        assertSizes(component.getMinimumSize().width, component.getPreferredSize().width,
                Short.MAX_VALUE, component.getPreferredSize().width, spring);
        spring.setValue(10);
        assertSizes(component.getMinimumSize().width, component.getPreferredSize().width,
                Short.MAX_VALUE, 10, spring);
        component.setMinimumSize(new Dimension(11, 12));
        component.setPreferredSize(new Dimension(13, 14));
        component.setMaximumSize(new Dimension(15, 16));
        assertSizes(11, 13, 15, 10, spring);
        component.setSize(new Dimension(100, 200));
        assertSizes(11, 13, 15, 10, spring);
    }

    public void testWidth_UNSET() throws Exception {
        spring = Spring.width(component);
        spring.setValue(10);
        component.setMinimumSize(new Dimension(11, 12));
        component.setPreferredSize(new Dimension(13, 14));
        component.setMaximumSize(new Dimension(15, 16));
        assertSizes(11, 13, 15, 10, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(11, 13, 15, spring.getPreferredValue(), spring);
        component.setPreferredSize(new Dimension(101, 102));
        assertEquals(101, spring.getValue());
        component.setPreferredSize(new Dimension(201, 102));
        assertEquals(201, spring.getValue());
        component.setSize(new Dimension(500, 600));
        spring.setValue(Spring.UNSET);
        assertSizes(11, 201, 15, 201, spring);
    }

    public void testWidth_Overflow() throws Exception {
        spring = Spring.width(component);
        spring.setValue(Integer.MAX_VALUE - 5);
        assertEquals(Integer.MAX_VALUE - 5, spring.getValue());
        spring.setValue(Integer.MAX_VALUE - 5);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Short.MAX_VALUE, spring.getMaximumValue());
        spring = Spring.width(component);
        component.setMaximumSize(new Dimension(Integer.MIN_VALUE, Integer.MIN_VALUE));
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MIN_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(Integer.MIN_VALUE, spring.getMaximumValue());
        }
    }

    public void testHeight() {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Spring.height(null).getValue();
            }
        });

        component = new JButton("Test") {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Dimension getMinimumSize() {
                componentGetMinimumSizeCalled.setOccurred();
                return super.getMinimumSize();
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                componentGetPreferedSizeCalled.setOccurred();
                return super.getPreferredSize();
            }

            @Override
            public java.awt.Dimension getMaximumSize() {
                componentGetMaximumSizeCalled.setOccurred();
                return super.getMaximumSize();
            }
        };
        initComponentSizes(component);
        spring = Spring.height(component);
        assertFalse(componentGetPreferedSizeCalled.isOccurred());
        assertFalse(componentGetMinimumSizeCalled.isOccurred());
        assertFalse(componentGetMaximumSizeCalled.isOccurred());
        componentGetPreferedSizeCalled.reset();
        spring.getPreferredValue();
        assertTrue(componentGetPreferedSizeCalled.isOccurred());
        componentGetPreferedSizeCalled.reset();
        spring.getPreferredValue();
        assertTrue(componentGetPreferedSizeCalled.isOccurred());
        componentGetMinimumSizeCalled.reset();
        spring.getMinimumValue();
        assertTrue(componentGetMinimumSizeCalled.isOccurred());
        componentGetMaximumSizeCalled.reset();
        spring.getMaximumValue();
        assertTrue(componentGetMaximumSizeCalled.isOccurred());
        assertSizes(component.getMinimumSize().height, component.getPreferredSize().height,
                Short.MAX_VALUE, component.getPreferredSize().height, spring);
        spring.setValue(10);
        assertSizes(component.getMinimumSize().height, component.getPreferredSize().height,
                Short.MAX_VALUE, 10, spring);
        component.setMinimumSize(new Dimension(11, 12));
        component.setPreferredSize(new Dimension(13, 14));
        component.setMaximumSize(new Dimension(15, 16));
        assertSizes(12, 14, 16, 10, spring);
    }

    public void testHeight_UNSET() throws Exception {
        spring = Spring.height(component);
        spring.setValue(10);
        component.setMinimumSize(new Dimension(11, 12));
        component.setPreferredSize(new Dimension(13, 14));
        component.setMaximumSize(new Dimension(15, 16));
        assertSizes(12, 14, 16, 10, spring);
        spring.setValue(Spring.UNSET);
        assertSizes(12, 14, 16, spring.getPreferredValue(), spring);
        component.setPreferredSize(new Dimension(101, 102));
        assertEquals(102, spring.getValue());
        component.setPreferredSize(new Dimension(201, 202));
        assertEquals(202, spring.getValue());
    }

    public void testHeight_Overflow() throws Exception {
        component = new JTextField();
        spring = Spring.height(component);
        spring.setValue(Integer.MAX_VALUE - 5);
        assertEquals(Integer.MAX_VALUE - 5, spring.getValue());
        spring.setValue(Integer.MAX_VALUE - 5);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Short.MAX_VALUE, spring.getMaximumValue());
        spring = Spring.height(component);
        component.setMaximumSize(new Dimension(Integer.MIN_VALUE, Integer.MIN_VALUE));
        if (isHarmony()) {
            assertEquals(Spring.TRIMMED_MIN_VALUE, spring.getMaximumValue());
        } else {
            assertEquals(Integer.MIN_VALUE, spring.getMaximumValue());
        }
    }

    public void testToString() {
        component = new JButton("Test");
        setComponentSizes(component, new Dimension(59, 25));
        Spring spring1 = Spring.constant(1, 2, 3);
        Spring spring2 = Spring.width(component);
        if (isHarmony()) {
            assertEquals("[1, 2, 3]", spring1.toString());
            assertEquals("[5, 5, 5]", Spring.constant(5).toString());
            assertEquals("[Width of javax.swing.JButton: (59, 59, 59)]", spring2.toString());
            assertEquals("[Height of javax.swing.JButton: (25, 25, 25)]",
                         Spring.height(component).toString());
            assertEquals("(-[1, 2, 3])", Spring.minus(spring1).toString());
            assertEquals("([1, 2, 3] + [Width of javax.swing.JButton: (59, 59, 59)])",
                         Spring.sum(spring1, spring2).toString());
            assertEquals("max([1, 2, 3], [Width of javax.swing.JButton: (59, 59, 59)])",
                         Spring.max(spring1, spring2).toString());
            assertEquals("(0.3 * [1, 2, 3])", Spring.scale(spring1, 0.3f).toString());
        }
    }

    public static void assertSizes(final int min, final int pref, final int max,
            final Spring spring) {
        assertEquals(min, spring.getMinimumValue());
        assertEquals(pref, spring.getPreferredValue());
        assertEquals(max, spring.getMaximumValue());
    }

    public static void assertSizes(final int min, final int pref, final int max,
            final int value, final Spring spring) {
        assertSizes(min, pref, max, spring);
        assertEquals(value, spring.getValue());
    }

    public static void assertValues(final Spring expectedSpring, final Spring spring) {
        assertSizes(expectedSpring.getMinimumValue(), expectedSpring.getPreferredValue(),
                expectedSpring.getMaximumValue(), expectedSpring.getValue(), spring);
    }

    private void initComponentSizes(Component c) {
        c.setMinimumSize(new Dimension(2, 2));
        c.setPreferredSize(new Dimension(75, 25));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    private void checkStrains(final Spring spring1, final Spring spring2, final int newValue,
            final int val1, final int val2) {
        Spring sum = Spring.sum(spring1, spring2);
        assertSizes(spring1.getMinimumValue() + spring2.getMinimumValue(), spring1
                .getPreferredValue()
                + spring2.getPreferredValue(), spring1.getMaximumValue()
                + spring2.getMaximumValue(), spring1.getValue() + spring2.getValue(), sum);
        sum.setValue(newValue);
        assertEquals(val1, spring1.getValue());
        assertEquals(val2, spring2.getValue());
    }

    private void testScale(final float factor) {
        spring1 = Spring.constant(12, 13, 15);
        spring = Spring.scale(spring1, factor);
        if (factor > 0) {
            assertSizes(Math.round(factor * spring1.getMinimumValue()),
                        Math.round(factor * spring1.getPreferredValue()),
                        Math.round(factor * spring1.getMaximumValue()),
                        Math.round(factor * spring1.getValue()), spring);
        } else {
            assertSizes(Math.round(factor * spring1.getMaximumValue()),
                        Math.round(factor * spring1.getPreferredValue()),
                        Math.round(factor * spring1.getMinimumValue()),
                        Math.round(factor * spring1.getValue()), spring);
        }
        assertFalse(Integer.MAX_VALUE == spring1.getMinimumValue());
        assertFalse(Integer.MAX_VALUE == spring1.getMaximumValue());
        assertFalse(Integer.MAX_VALUE == spring1.getPreferredValue());
        spring.setValue(3);
        if (factor != 0) {
            assertEquals(Math.round(spring.getValue() / factor), spring1.getValue());
        } else {
            assertEquals(Integer.MAX_VALUE, spring1.getValue());
        }
        spring.setValue(0);
        assertEquals(0, spring1.getValue());
        assertFalse(spring1.getPreferredValue() == spring1.getValue());
        spring.setValue(-6);
        if (factor != 0) {
            assertEquals(Math.round(spring.getValue() / factor), spring1.getValue());
        } else {
            assertEquals(spring1.getPreferredValue(), spring1.getValue());
        }
        spring.setValue(Spring.UNSET);
        assertEquals(spring.getPreferredValue(), spring.getValue());
    }

    private void setComponentSizes(Component component, Dimension size) {
        component.setMinimumSize(size);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
    }
}
