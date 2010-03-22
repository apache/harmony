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
 * @author Ilya S. Okomin
 */
package java.awt.font;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.apache.harmony.awt.internal.nls.Messages;

public abstract class GraphicAttribute {

    public static final int TOP_ALIGNMENT = -1;

    public static final int BOTTOM_ALIGNMENT = -2;

    public static final int ROMAN_BASELINE = 0;

    public static final int CENTER_BASELINE = 1;

    public static final int HANGING_BASELINE = 2;

    // the alignment of this GraphicAttribute
    private int alignment;

    protected GraphicAttribute(int align) {
        if ((align < BOTTOM_ALIGNMENT) || (align > HANGING_BASELINE)) {
            // awt.198=Illegal alignment argument
            throw new IllegalArgumentException(Messages.getString("awt.198")); //$NON-NLS-1$
        }
        this.alignment = align;
    }

    public abstract void draw(Graphics2D graphics, float x, float y);

    public abstract float getAdvance();

    public final int getAlignment() {
        return this.alignment;
    }

    public abstract float getAscent();

    public Rectangle2D getBounds() {
        float ascent = getAscent();
        float advance = getAdvance();
        float descent = getDescent();

        // Default implementation - see API documentation.
        return new Rectangle2D.Float(0, -ascent, advance, ascent + descent);
    }

    public abstract float getDescent();

    public GlyphJustificationInfo getJustificationInfo() {
        
        /* Default implementation.
         * Since documentation doesn't describe default values,
         * they were calculated based on 1.5 release 
         * behavior and can be obtained using next test sample:
         * 
         *    // Create GraphicAttribute class implementation
         *    public class MyGraphicAttribute extends GraphicAttribute {
         *        protected MyGraphicAttribute(int align) {
         *            super(align);
         *        }
         *
         *        public float getDescent() {
         *           return 0;
         *        }
         *
         *        public float getAdvance() {
         *           return 1;
         *        }
         *
         *        public void draw(Graphics2D g2, float x, float y) {
         *        }
         *
         *        public float getAscent() {
         *            return 0;
         *        }
         *    }
         *
         *    MyGraphicAttribute myGA = gat.new MyGraphicAttribute(0);
         *    // print justification parameters
         *    System.out.println(myGA.getJustificationInfo().growAbsorb);
         *    System.out.println(myGA.getJustificationInfo().shrinkAbsorb);
         *    System.out.println(myGA.getJustificationInfo().growLeftLimit);
         *    System.out.println(myGA.getJustificationInfo().growPriority);
         *    System.out.println(myGA.getJustificationInfo().growRightLimit);
         *    System.out.println(myGA.getJustificationInfo().shrinkLeftLimit);
         *    System.out.println(myGA.getJustificationInfo().shrinkPriority);
         *    System.out.println(myGA.getJustificationInfo().shrinkRightLimit);
         *    System.out.println(myGA.getJustificationInfo().weight);
         */
        float advance = getAdvance();
        return new GlyphJustificationInfo(
                                    advance,
                                    false,
                                    GlyphJustificationInfo.PRIORITY_INTERCHAR,
                                    advance / 3,
                                    advance / 3,
                                    false,
                                    GlyphJustificationInfo.PRIORITY_WHITESPACE,
                                    0,
                                    0);
    }

}

