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
package javax.swing.text;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

import java.awt.Shape;

import javax.swing.text.Position.Bias;

/**
 * Utility class to support testing of
 * <code>View.getNextVisualPositionFrom</code> method.
 */
public final class VisualPositionHelper {
    private static final Bias[] biasRet = new Bias[1];
    private static final Bias[] biases = new Bias[] {Bias.Forward,
                                                     Bias.Backward};

    private VisualPositionHelper() {
    }

    public static void assertNextPosition(final int expectedPosition,
                                          final int offset,
                                          final int direction,
                                          final View view,
                                          final Shape allocation)
        throws BadLocationException {

        for (Bias bias : biases) {
            biasRet[0] = null;
            assertEquals(bias + " from " + offset,
                         expectedPosition,
                         view.getNextVisualPositionFrom(offset, bias,
                                                        allocation, direction,
                                                        biasRet));
            assertSame(Bias.Forward, biasRet[0]);
        }
    }

    public static void assertNextBiasedPosition(final int expectedPosition,
                                                final int offset,
                                                final int direction,
                                                final View view,
                                                final Shape allocation,
                                                final Bias bias)
        throws BadLocationException {

        biasRet[0] = null;
        assertEquals(bias + " from " + offset,
                     expectedPosition,
                     view.getNextVisualPositionFrom(offset, bias,
                                                    allocation, direction,
                                                    biasRet));
        assertSame(Bias.Forward, biasRet[0]);
    }

    public static void assertNextForwardPosition(final int expectedPosition,
                                                 final int offset,
                                                 final int direction,
                                                 final View view,
                                                 final Shape allocation)
        throws BadLocationException {

        assertNextBiasedPosition(expectedPosition,
                                 offset, direction, view,
                                 allocation, Bias.Forward);
    }

    public static void assertNextBackwardPosition(final int expectedPosition,
                                                  final int offset,
                                                  final int direction,
                                                  final View view,
                                                  final Shape allocation)
        throws BadLocationException {

        assertNextBiasedPosition(expectedPosition,
                                 offset, direction, view,
                                 allocation, Bias.Backward);
    }
}
