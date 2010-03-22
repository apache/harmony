/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.compiler;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Provides dynamic 4-space indents usable for source code generation.
 * All indents are measured in 4-space units.
 *
 * This class' methods are designed to be called right from the output stream
 * composition, i. e. <code>String output =
 * ("first line" + indenter.increase() + "second line");</code>
 *
 * @author  Vasily Zakharov
 */
final class Indenter {

    /**
     * String used for indentation.
     */
    private final String stepString = "    "; //$NON-NLS-1$

    /**
     * Length of {@linkplain #stepString indentation string}.
     */
    private final int STEP_LENGTH = stepString.length();

    /**
     * Indent string.
     */
    private String currentIndent;

    /**
     * Create instance with zero starting indent.
     */
    Indenter() {
        currentIndent = new String();
    }

    /**
     * Create instance with specified starting indent.
     *
     * @param   indent
     *          Starting indent.
     */
    Indenter(int indent) {
        this();

        if (indent > 0) {
            increase(indent);
        }
    }

    /**
     * Returns current indent string.
     *
     * @return  Current indent string.
     */
    String indent() {
        return currentIndent;
    }

    /**
     * Increase current indent one step right.
     *
     * @return  Current (increased) indent string.
     */
    String increase() {
        return increase(1);
    }

    /**
     * Increase current indent the specified number of steps right.
     *
     * @param   steps
     *          Number of steps to increase current indent.
     *
     * @return  Current (increased) indent string.
     */
    String increase(int steps) {
        return currentIndent = tIncrease(steps);
    }

    /**
     * Decrease current indent one step left.
     *
     * @return  Current (decreased) indent string.
     *
     * @throws  IndexOutOfBoundsException
     *          If current indent is empty and thus cannot be decreased.
     */
    String decrease() throws IndexOutOfBoundsException {
        return decrease(1);
    }

    /**
     * Decrease current indent the specified number of steps left.
     *
     * @param   steps
     *          Number of steps to decrease current indent.
     *
     * @return  Current (decreased) indent string.
     *
     * @throws  IndexOutOfBoundsException
     *          If current indent is empty and thus cannot be decreased.
     */
    String decrease(int steps) throws IndexOutOfBoundsException {
        return currentIndent = tDecrease(steps);
    }

    /**
     * Increases current indent one step right, but returns empty string.
     *
     * @return  Empty string.
     */
    String hIncrease() {
        return hIncrease(1);
    }

    /**
     * Increases current indent the specified number of steps right,
     * but returns empty string.
     *
     * @param   steps
     *          Number of steps to increase current indent.
     *
     * @return  Empty string.
     */
    String hIncrease(int steps) {
        increase(steps);
        return ""; //$NON-NLS-1$
    }

    /**
     * Decreases current indent one step left, but returns empty string.
     *
     * @return  Empty string.
     *
     * @throws  IndexOutOfBoundsException
     *          If current indent is empty and thus cannot be decreased.
     */
    String hDecrease() throws IndexOutOfBoundsException {
        return hDecrease(1);
    }

    /**
     * Decreases current indent the specified number of steps left,
     * but returns empty string.
     *
     * @param   steps
     *          Number of steps to decrease indent.
     *
     * @return  Empty string.
     *
     * @throws  IndexOutOfBoundsException
     *          If current indent is empty and thus cannot be decreased.
     */
    String hDecrease(int steps) throws IndexOutOfBoundsException {
        decrease(steps);
        return ""; //$NON-NLS-1$
    }

    /**
     * Returns current indent temporary increased one step right.
     * The stored indent is not changed.
     *
     * @return  Increased indent string.
     */
    String tIncrease() {
        return tIncrease(1);
    }

    /**
     * Returns current indent temporary increased the specified number of steps
     * right. The stored indent is not changed.
     *
     * @param   steps
     *          Number of steps to increase current indent.
     *
     * @return  Increased indent string.
     */
    String tIncrease(int steps) {
        StringBuilder buffer = new StringBuilder(currentIndent);

        for (int i = 0; i < steps; i++) {
            buffer.append(stepString);
        }

        return buffer.toString();
    }

    /**
     * Returns current indent temporary decreased one step left.
     * The stored indent is not changed.
     *
     * @return  Decreased indent string.
     *
     * @throws  IndexOutOfBoundsException
     *          If current indent is empty and thus cannot be decreased.
     */
    String tDecrease() throws IndexOutOfBoundsException {
        return tDecrease(1);
    }

    /**
     * Returns current indent temporary decreased the specified number of steps
     * left. The stored indent is not changed.
     *
     * @param   steps
     *          Number of steps to decrease indent.
     *
     * @return  Decreased indent string.
     *
     * @throws  IndexOutOfBoundsException
     *          If current indent is empty and thus cannot be decreased.
     */
    String tDecrease(int steps) throws IndexOutOfBoundsException {
        return currentIndent.substring(0,
                (currentIndent.length() - (steps * STEP_LENGTH)));
    }

    /**
     * Return empty string if current indent is empty.
     * Throws {@link IllegalStateException} otherwise.
     *
     * @return  Empty string.
     *
     * @throws  IllegalStateException
     *          If current indent is not empty.
     */
    String assertEmpty() throws IllegalStateException {
        if (currentIndent.length() != 0) {
            // rmi.56=Indenter assertion failed: current indent is not empty
            throw new IllegalStateException(Messages.getString("rmi.56")); //$NON-NLS-1$
        }
        return ""; //$NON-NLS-1$
    }
}
