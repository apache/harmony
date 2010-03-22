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

public abstract class Spring {

    static final class HeightSpring extends Spring {
        final Component component;
        private int value = Spring.UNSET;

        public int getMinimumValue() {
            return trim(component.getMinimumSize().height);
        }

        public int getPreferredValue() {
            return trim(component.getPreferredSize().height);
        }

        public int getMaximumValue() {
            return trim(component.getMaximumSize().height);
        }

        public int getValue() {
            return (value != Spring.UNSET ? value : getPreferredValue());
        }

        public void setValue(final int value) {
            this.value = value;
        }

        public String toString() {
            return "[Height of " + component.getClass().getName() + ": ("
                   + getMinimumValue() + ", " + getPreferredValue() + ", "
                   + getMaximumValue() + ")]";
        }

        private HeightSpring(final Component component) {
            this.component = component;
        }
    }

    static final class WidthSpring extends Spring {
        final Component component;
        private int value = Spring.UNSET;

        public int getMinimumValue() {
            return trim(component.getMinimumSize().width);
        }

        public int getPreferredValue() {
            return trim(component.getPreferredSize().width);
        }

        public int getMaximumValue() {
            return trim(component.getMaximumSize().width);
        }

        public int getValue() {
            return (value != Spring.UNSET ? value : getPreferredValue());
        }

        public void setValue(final int value) {
            this.value = value;
        }

        public String toString() {
            return "[Width of " + component.getClass().getName() + ": ("
                   + getMinimumValue() + ", " + getPreferredValue() + ", "
                   + getMaximumValue() + ")]";
        }

        private WidthSpring(final Component component) {
            this.component = component;
        }
    }


    private static final class ConstantSpring extends Spring {
        private final int minValue;
        private final int prefValue;
        private final int maxValue;
        private int value;

        public int getMinimumValue() {
            return minValue;
        }

        public int getPreferredValue() {
            return prefValue;
        }

        public int getMaximumValue() {
            return maxValue;
        }

        public int getValue() {
            return (value != Spring.UNSET ? value : getPreferredValue());
        }

        public void setValue(final int value) {
            this.value = value;
        }

        public String toString() {
            return "[" + minValue + ", " + prefValue + ", " + maxValue + "]";
        }

        private ConstantSpring(final int pref) {
            this(pref, pref, pref);
        }

        private ConstantSpring(final int min, final int pref, final int max) {
            this.maxValue = max;
            this.minValue = min;
            this.prefValue = pref;
            value = pref;
        }
    }

    private static final class MinusSpring extends Spring {
        private final Spring sourceSpring;

        public int getMinimumValue() {
            int val = sourceSpring.getMaximumValue();
            return negativeValue(val);
        }

        public int getPreferredValue() {
            int val = sourceSpring.getPreferredValue();
            return negativeValue(val);
        }

        public int getMaximumValue() {
            int val = sourceSpring.getMinimumValue();
            return negativeValue(val);
        }

        public int getValue() {
            int val = sourceSpring.getValue();
            return negativeValue(val);
        }

        public void setValue(final int value) {
                sourceSpring.setValue(value != Spring.UNSET ? (-1) * value
                                                              : Spring.UNSET);
        }

        public String toString() {
            return "(-" + sourceSpring + ")";
        }

        private int negativeValue(final int val) {
            int value = trim(val);
            switch (value) {
            case TRIMMED_MAX_VALUE:
                return TRIMMED_MIN_VALUE;

            case TRIMMED_MIN_VALUE:
                return TRIMMED_MAX_VALUE;

            default:
                return (-1) * value;
            }
        }

        private MinusSpring(final Spring spring) {
            sourceSpring = spring;
        }
    }

    private static final class MaxSpring extends Spring {
        private final Spring sourceSpring1;
        private final Spring sourceSpring2;

        private int minValue = Spring.UNSET;
        private int prefValue = Spring.UNSET;
        private int maxValue = Spring.UNSET;
        private int value = Spring.UNSET;

        public int getMinimumValue() {
            if (minValue == Spring.UNSET) {
                minValue  = trim(Math.max(sourceSpring1.getMinimumValue(),
                                          sourceSpring2.getMinimumValue()));
            }
            return minValue;
        }

        public int getPreferredValue() {
            if (prefValue == Spring.UNSET) {
                prefValue  = trim(Math.max(sourceSpring1.getPreferredValue(),
                                           sourceSpring2.getPreferredValue()));
            }
            return prefValue;
        }

        public int getMaximumValue() {
            if (maxValue == Spring.UNSET) {
                maxValue  = trim(Math.max(sourceSpring1.getMaximumValue(),
                                          sourceSpring2.getMaximumValue()));
            }
            return maxValue;
        }

        public int getValue() {
            if (value == Spring.UNSET) {
                value = Math.max(sourceSpring1.getValue(),
                                 sourceSpring2.getValue());
            }
            return value;
        }

        public void setValue(final int value) {
            if (value == Spring.UNSET) {
                minValue = Spring.UNSET;
                prefValue = Spring.UNSET;
                maxValue = Spring.UNSET;
                if (this.value != Spring.UNSET) {
                    sourceSpring1.setValue(value);
                    sourceSpring2.setValue(value);
                }
                this.value = Spring.UNSET;
                return;
            }

            this.value = value;
            final int pref1 = sourceSpring1.getPreferredValue();
            final int pref2 = sourceSpring2.getPreferredValue();

            if (pref1 < pref2) {
                sourceSpring1.setValue(Math.min(value, pref1));
                sourceSpring2.setValue(value);
            } else {
                sourceSpring1.setValue(value);
                sourceSpring2.setValue(Math.min(value, pref2));
            }
        }

        public String toString() {
            return "max(" + sourceSpring1 + ", "
                   + sourceSpring2 + ")";
        }

        private MaxSpring(final Spring spring1, final Spring spring2) {
            sourceSpring1 = spring1;
            sourceSpring2 = spring2;
        }
    }

    private static final class SumSpring extends Spring {
        private final Spring sourceSpring1;
        private final Spring sourceSpring2;

        private int minValue = Spring.UNSET;
        private int prefValue = Spring.UNSET;
        private int maxValue = Spring.UNSET;
        private int value = Spring.UNSET;

        public int getMinimumValue() {
            if (minValue == Spring.UNSET) {
                minValue  = sourceSpring1.getMinimumValue()
                                 + sourceSpring2.getMinimumValue();
            }
            return minValue;
        }

        public int getPreferredValue() {
            if (prefValue == Spring.UNSET) {
                prefValue  = sourceSpring1.getPreferredValue()
                                  + sourceSpring2.getPreferredValue();
            }
            return prefValue;
        }

        public int getMaximumValue() {
            if (maxValue == Spring.UNSET) {
                maxValue  = sourceSpring1.getMaximumValue()
                                 + sourceSpring2.getMaximumValue();
            }
            return maxValue;
        }

        public int getValue() {
            if (value == Spring.UNSET) {
                value  = trim(trim(sourceSpring1.getValue())
                              + trim(sourceSpring2.getValue()));
            }
            return value;
        }

        public void setValue(final int value) {
            if (value == Spring.UNSET) {
                minValue = Spring.UNSET;
                prefValue = Spring.UNSET;
                maxValue = Spring.UNSET;
                if (this.value != Spring.UNSET) {
                    sourceSpring1.setValue(value);
                    sourceSpring2.setValue(value);
                }
                this.value = Spring.UNSET;
                return;
            }

            final int c;
            final int c1;
 //           final int c2;
            int val1;

            boolean compression = (value <= getPreferredValue());
            this.value = value;

            if (compression) {
                c = getPreferredValue() - getMinimumValue();
                c1 = sourceSpring1.getPreferredValue()
                     - sourceSpring1.getMinimumValue();
//                c2 = sourceSpring2.getPreferredValue()
//                     - sourceSpring2.getMinimumValue();
            } else {
                c =  getMaximumValue() - getPreferredValue();
                c1 = sourceSpring1.getMaximumValue()
                     - sourceSpring1.getPreferredValue();
//                c2 = sourceSpring2.getMaximumValue()
//                     - sourceSpring2.getPreferredValue();

            }

            val1 = (value - getPreferredValue());
            if (c == 0) {
                if ((val1 * c1 >= 0)) {
                    val1 = sourceSpring1.getPreferredValue();
                } else {
                    val1 = Spring.TRIMMED_MAX_VALUE
                           + sourceSpring1.getPreferredValue();
                }
            } else {
                val1 = val1 * c1 / c + sourceSpring1.getPreferredValue();
            }

            sourceSpring1.setValue(val1);
            sourceSpring2.setValue(value - val1);
        }

        public String toString() {
            return "(" + sourceSpring1 + " + "
                   + sourceSpring2 + ")";
        }

        private SumSpring(final Spring spring1, final Spring spring2) {
            sourceSpring1 = spring1;
            sourceSpring2 = spring2;
        }
    }

    private static final class ScaleSpring extends Spring {
        private final Spring sourceSpring;
        private float factor;

        private ScaleSpring(final Spring spring, final float factor) {
            this.factor = factor;
            sourceSpring = spring;
        }

        public int getMinimumValue() {
            int val =  factor > 0 ? sourceSpring.getMinimumValue()
                                    : sourceSpring.getMaximumValue();
            return trim(Math.round(trim(val) * factor));
        }

        public int getPreferredValue() {
            return trim(Math.round(trim(sourceSpring.getPreferredValue())
                                   * factor));
        }

        public int getMaximumValue() {
            int val =  factor > 0 ? sourceSpring.getMaximumValue()
                                    : sourceSpring.getMinimumValue();
            return trim(Math.round(trim(val) * factor));
        }

        public int getValue() {
            return trim(Math.round(trim(sourceSpring.getValue()) * factor));
        }

        public void setValue(final int value) {
            int newValue;
            if (value == Spring.UNSET || value == 0) {
                newValue = value;
            } else {
                if (factor != 0) {
                    newValue = Math.round(value / factor);
                } else {
                    newValue = sourceSpring.getPreferredValue();
                    newValue =  value > 0 ? Integer.MAX_VALUE
                                            : sourceSpring.getPreferredValue();
                }

            }
            sourceSpring.setValue(newValue);
        }

        public String toString() {
            return "(" + factor + " * " + sourceSpring + ")";
        }

    }

    public static final int UNSET = -2147483648;

    static final int TRIMMED_MAX_VALUE = Short.MAX_VALUE;
    static final int TRIMMED_MIN_VALUE = Short.MIN_VALUE;


    protected Spring() {
    }

    public abstract int getMinimumValue();

    public abstract int getPreferredValue();

    public abstract int getMaximumValue();

    public abstract void setValue(int value);

    public abstract int getValue();

    public static Spring constant(final int pref) {
        return new ConstantSpring(pref);
    }

    public static Spring constant(final int min, final int pref,
                                  final int max) {
        return new ConstantSpring(min, pref, max);
    }

    public static Spring minus(final Spring spring) {
        return new MinusSpring(spring);
    }

    public static Spring sum(final Spring s1, final Spring s2) {
        return new SumSpring(s1, s2);
    }

    public static Spring max(final Spring s1, final Spring s2) {
        return new MaxSpring(s1, s2);
    }

    public static Spring scale(final Spring s, final float factor) {
        return new ScaleSpring(s, factor);
    }

    public static Spring width(final Component c) {
        return new WidthSpring(c);
    }

    public static Spring height(final Component c) {
        return new HeightSpring(c);
    }

    private static int trim(final int value) {
        if (value > TRIMMED_MAX_VALUE) {
            return  TRIMMED_MAX_VALUE;
        } else if (value <  TRIMMED_MIN_VALUE) {
            return TRIMMED_MIN_VALUE;
        }
        return value;
    }
}
