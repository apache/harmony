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
 * @author Dennis Ushakov
 */

package javax.swing;

import java.io.Serializable;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class SpinnerNumberModel extends AbstractSpinnerModel implements Serializable {

    private static final Number DEFAULT_VALUE = new Integer(0);
    private static final Number DEFAULT_STEP = new Integer(1);

    private Number value;
    private Comparable minimum;
    private Comparable maximum;
    private Number stepSize;

    public SpinnerNumberModel(final Number value, final Comparable minimum, final Comparable maximum, final Number stepSize) {
        if (value == null || stepSize == null) {
            throw new IllegalArgumentException(Messages.getString("swing.5E")); //$NON-NLS-1$
        }
        if (minimum != null && minimum.compareTo(value) > 0) {
            throw new IllegalArgumentException(Messages.getString("swing.5F")); //$NON-NLS-1$
        }
        if (maximum != null && maximum.compareTo(value) < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.5F")); //$NON-NLS-1$
        }
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }

    public SpinnerNumberModel(final int value, final int minimum, final int maximum, final int stepSize) {
        this(new Integer(value), new Integer(minimum), new Integer(maximum), new Integer(stepSize));
    }

    public SpinnerNumberModel(final double value, final double minimum, final double maximum, final double stepSize) {
        this(new Double(value), new Double(minimum), new Double(maximum), new Double(stepSize));
    }

    public SpinnerNumberModel() {
        this(DEFAULT_VALUE, null, null, DEFAULT_STEP);
    }

    public void setMinimum(final Comparable minimum) {
        if (this.minimum != minimum) {
            this.minimum = minimum;
            fireStateChanged();
        }
    }

    public Comparable getMinimum() {
        return minimum;
    }

    public void setMaximum(final Comparable maximum) {
        if (this.maximum != maximum) {
            this.maximum = maximum;
            fireStateChanged();
        }
    }

    public Comparable getMaximum() {
        return maximum;
    }

    public void setStepSize(final Number stepSize) {
        if (stepSize == null || !(stepSize instanceof Number)) {
            throw new IllegalArgumentException(Messages.getString("swing.60")); //$NON-NLS-1$
        }
        if (this.stepSize != stepSize) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }

    public Number getStepSize() {
        return stepSize;
    }

    public Object getNextValue() {
        Number result = inc(value, stepSize, 1);
        return (maximum == null) ? result :
                                   (maximum.compareTo(result) < 0) ? null : result;
    }

    public Object getPreviousValue() {
        Number result = inc(value, stepSize, -1);
        return (minimum == null) ? result :
                                   (minimum.compareTo(result) > 0) ? null : result;
    }

    public Number getNumber() {
        return value;
    }

    public Object getValue() {
        return getNumber();
    }

    public void setValue(final Object value) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(Messages.getString("swing.5B")); //$NON-NLS-1$
        }
        if (this.value != value) {
            this.value = (Number)value;
            fireStateChanged();
        }
    }

    private Number inc(final Number val, final Number step, final int sgn) {
        if (val instanceof Integer) {
            return new Integer(val.intValue() + step.intValue() * sgn);
        }
        if (val instanceof Long) {
            return new Long(val.longValue() + step.longValue() * sgn);
        }
        if (val instanceof Float) {
            return new Float(val.floatValue() + step.floatValue() * sgn);
        }
        if (val instanceof Double) {
            return new Double(val.doubleValue() + step.doubleValue() * sgn);
        }
        if (val instanceof Short) {
            return new Short((short)(val.shortValue() + step.shortValue() * sgn));
        }
        return new Byte((byte)(val.byteValue() + step.byteValue() * sgn));
    }

}
