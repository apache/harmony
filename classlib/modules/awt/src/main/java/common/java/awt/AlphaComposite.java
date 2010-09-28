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
 * @author Igor V. Stolyarov
 */
package java.awt;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

import org.apache.harmony.awt.gl.ICompositeContext;
import org.apache.harmony.awt.internal.nls.Messages;


public final class AlphaComposite implements Composite {

    public static final int CLEAR = 1;

    public static final int SRC = 2;

    public static final int DST = 9;

    public static final int SRC_OVER = 3;

    public static final int DST_OVER = 4;

    public static final int SRC_IN = 5;

    public static final int DST_IN = 6;

    public static final int SRC_OUT = 7;

    public static final int DST_OUT = 8;

    public static final int SRC_ATOP = 10;

    public static final int DST_ATOP = 11;

    public static final int XOR = 12;

    public static final AlphaComposite Clear = new AlphaComposite(CLEAR);

    public static final AlphaComposite Src = new AlphaComposite(SRC);

    public static final AlphaComposite Dst = new AlphaComposite(DST);

    public static final AlphaComposite SrcOver = new AlphaComposite(SRC_OVER);

    public static final AlphaComposite DstOver = new AlphaComposite(DST_OVER);

    public static final AlphaComposite SrcIn = new AlphaComposite(SRC_IN);

    public static final AlphaComposite DstIn = new AlphaComposite(DST_IN);

    public static final AlphaComposite SrcOut = new AlphaComposite(SRC_OUT);

    public static final AlphaComposite DstOut = new AlphaComposite(DST_OUT);

    public static final AlphaComposite SrcAtop = new AlphaComposite(SRC_ATOP);

    public static final AlphaComposite DstAtop = new AlphaComposite(DST_ATOP);

    public static final AlphaComposite Xor = new AlphaComposite(XOR);

    private int rule;
    private float alpha;

    private AlphaComposite(int rule, float alpha){
        if(rule < CLEAR || rule > XOR) {
            // awt.11D=Unknown rule
            throw new IllegalArgumentException(Messages.getString("awt.11D")); //$NON-NLS-1$
        }
        if(alpha < 0.0f || alpha > 1.0f) {
            // awt.11E=Wrong alpha value
            throw new IllegalArgumentException(Messages.getString("awt.11E")); //$NON-NLS-1$
        }

        this.rule = rule;
        this.alpha = alpha;
    }

    private AlphaComposite(int rule){
        this(rule, 1.0f);
    }

    public CompositeContext createContext(ColorModel srcColorModel,
            ColorModel dstColorModel, RenderingHints hints) {
        return new ICompositeContext(this, srcColorModel, dstColorModel);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AlphaComposite)) {
            return false;
        }
        AlphaComposite other = (AlphaComposite)obj;
        return (this.rule == other.getRule() && this.alpha == other.getAlpha());
    }

    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(alpha);
        int tmp = hash >>> 24;
        hash <<= 8;
        hash |= tmp;
        hash ^= rule;
        return hash;
    }

    public int getRule() {
        return rule;
    }

    public float getAlpha() {
        return alpha;
    }

    public static AlphaComposite getInstance(int rule, float alpha) {
        if(alpha == 1.0f) {
            return getInstance(rule);
        }
        return new AlphaComposite(rule, alpha);
    }

    public static AlphaComposite getInstance(int rule) {
        switch(rule){
        case CLEAR:
            return Clear;
        case SRC:
            return Src;
        case DST:
            return Dst;
        case SRC_OVER:
            return SrcOver;
        case DST_OVER:
            return DstOver;
        case SRC_IN:
            return SrcIn;
        case DST_IN:
            return DstIn;
        case SRC_OUT:
            return SrcOut;
        case DST_OUT:
            return DstOut;
        case SRC_ATOP:
            return SrcAtop;
        case DST_ATOP:
            return DstAtop;
        case XOR:
            return Xor;
        default:
            // awt.11D=Unknown rule
            throw new IllegalArgumentException(Messages.getString("awt.11D")); //$NON-NLS-1$
        }
    }

}

