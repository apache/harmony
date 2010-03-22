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
package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.util.Arrays;

import org.apache.harmony.awt.internal.nls.Messages;

public abstract class ColorModel implements Transparency {

    protected int pixel_bits;  // Pixel length in bits

    protected int transferType;

    ColorSpace cs;

    boolean hasAlpha;

    boolean isAlphaPremultiplied;

    int transparency;

    int numColorComponents;

    int numComponents;

    int[] bits;             // Array of components masks 

    int[] maxValues = null; // Max values that may be represent by color
                            // components

    int maxBitLength;       // Max length color components in bits

    private static ColorModel RGBdefault;

    protected ColorModel(int pixel_bits, int[] bits, ColorSpace cspace,
            boolean hasAlpha, boolean isAlphaPremultiplied, int transparency,
            int transferType) {

        if (pixel_bits < 1) {
            // awt.26B=The number of bits in the pixel values is less than 1
            throw new IllegalArgumentException(Messages.getString("awt.26B")); //$NON-NLS-1$
        }

        if (bits == null) {
            // awt.26C=bits is null
            throw new NullPointerException(Messages.getString("awt.26C")); //$NON-NLS-1$
        }

        int sum = 0;
        for (int element : bits) {
            if (element < 0) {
                // awt.26D=The elements in bits is less than 0
                throw new IllegalArgumentException(Messages.getString("awt.26D")); //$NON-NLS-1$
            }
            sum += element;
        }

        if (sum < 1) {
            // awt.26E=The sum of the number of bits in bits is less than 1
            throw new NullPointerException(Messages.getString("awt.26E")); //$NON-NLS-1$
        }

        if (cspace == null) {
            // awt.26F=The cspace is null
            throw new IllegalArgumentException(Messages.getString("awt.26F")); //$NON-NLS-1$
        }

        if (transparency < Transparency.OPAQUE ||
               transparency > Transparency.TRANSLUCENT) {
            // awt.270=The transparency is not a valid value
            throw new IllegalArgumentException(Messages.getString("awt.270")); //$NON-NLS-1$
        }

        this.pixel_bits = pixel_bits;
        this.bits = bits.clone();

        maxValues = new int[bits.length];
        maxBitLength = 0;
        for (int i = 0; i < maxValues.length; i++) {
            maxValues[i] = (1 << bits[i]) - 1;
            if (bits[i] > maxBitLength) {
                maxBitLength = bits[i];
            }
        }

        cs = cspace;
        this.hasAlpha = hasAlpha;
        this.isAlphaPremultiplied = isAlphaPremultiplied;
        numColorComponents = cs.getNumComponents();

        if (hasAlpha) {
            numComponents = numColorComponents + 1;
        } else {
            numComponents = numColorComponents;
        }

        this.transparency = transparency;
        this.transferType = transferType;

    }

    public ColorModel(int bits) {

        if (bits < 1) {
            // awt.271=The number of bits in bits is less than 1
            throw new IllegalArgumentException(Messages.getString("awt.271")); //$NON-NLS-1$
        }

        pixel_bits = bits;
        transferType = getTransferType(bits);
        cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        hasAlpha = true;
        isAlphaPremultiplied = false;
        transparency = Transparency.TRANSLUCENT;

        numColorComponents = 3;
        numComponents = 4;

        this.bits = null;
    }

    public Object getDataElements(int[] components, int offset, Object obj) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public Object getDataElements(float[] normComponents, int normOffset,
            Object obj) {
        int unnormComponents[] = getUnnormalizedComponents(normComponents,
                normOffset, null, 0);
        return getDataElements(unnormComponents, 0, obj);
    }

    public Object getDataElements(int rgb, Object pixel) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public WritableRaster getAlphaRaster(WritableRaster raster) {
        return null;
    }

    public ColorModel coerceData(WritableRaster raster,
            boolean isAlphaPremultiplied) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    @Override
    public String toString() {
        // The output format based on 1.5 release behaviour. 
        // It could be reveled such way:
        // ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB,
        // false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        // System.out.println(cm.toString());
        return "ColorModel: Color Space = " + cs.toString() + "; has alpha = " //$NON-NLS-1$ //$NON-NLS-2$
                + hasAlpha + "; is alpha premultipied = " //$NON-NLS-1$
                + isAlphaPremultiplied + "; transparency = " + transparency //$NON-NLS-1$
                + "; number color components = " + numColorComponents //$NON-NLS-1$
                + "; pixel bits = " + pixel_bits + "; transfer type = " //$NON-NLS-1$ //$NON-NLS-2$
                + transferType;
    }

    public int[] getComponents(Object pixel, int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public float[] getNormalizedComponents(Object pixel,
            float[] normComponents, int normOffset) {

        if (pixel == null) {
            // awt.294=pixel is null
            throw new NullPointerException(Messages.getString("awt.294")); //$NON-NLS-1$
        }

        int unnormComponents[] = getComponents(pixel, null, 0);
        return getNormalizedComponents(unnormComponents, 0, normComponents,
                normOffset);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ColorModel)) {
            return false;
        }
        ColorModel cm = (ColorModel) obj;

        return (pixel_bits == cm.getPixelSize() &&
               transferType == cm.getTransferType() &&
               cs.getType() == cm.getColorSpace().getType() &&
               hasAlpha == cm.hasAlpha() &&
               isAlphaPremultiplied == cm.isAlphaPremultiplied() &&
               transparency == cm.getTransparency() &&
               numColorComponents == cm.getNumColorComponents() &&
               numComponents == cm.getNumComponents() &&
               Arrays.equals(bits, cm.getComponentSize()));
    }

    public int getRed(Object inData) {
        return getRed(constructPixel(inData));
    }

    public int getRGB(Object inData) {
        return (getAlpha(inData) << 24 | getRed(inData) << 16 |
               getGreen(inData) << 8 | getBlue(inData));
    }

    public int getGreen(Object inData) {
        return getGreen(constructPixel(inData));
    }

    public int getBlue(Object inData) {
        return getBlue(constructPixel(inData));
    }

    public int getAlpha(Object inData) {
        return getAlpha(constructPixel(inData));
    }

    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public boolean isCompatibleSampleModel(SampleModel sm) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public SampleModel createCompatibleSampleModel(int w, int h) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public boolean isCompatibleRaster(Raster raster) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public final ColorSpace getColorSpace() {
        return cs;
    }

    public float[] getNormalizedComponents(int[] components, int offset,
            float normComponents[], int normOffset) {
        if (bits == null) {
            // awt.26C=bits is null
            throw new UnsupportedOperationException(Messages.getString("awt.26C")); //$NON-NLS-1$
        }

        if (normComponents == null) {
            normComponents = new float[numComponents + normOffset];
        }

        if (hasAlpha && isAlphaPremultiplied) {
            float normAlpha =
                (float) components[offset + numColorComponents] /
                    maxValues[numColorComponents];
            if (normAlpha != 0.0f) {
                for (int i = 0; i < numColorComponents; i++) {
                    normComponents[normOffset + i] =
                        components[offset + i] /
                            (normAlpha * maxValues[i]);
                }
                normComponents[normOffset + numColorComponents] = normAlpha;
            } else {
                for (int i = 0; i < numComponents; i++) {
                    normComponents[normOffset + i] = 0.0f;
                }
            }
        } else {
            for (int i = 0; i < numComponents; i++) {
                normComponents[normOffset + i] =
                    (float) components[offset + i] /
                        maxValues[i];
            }
        }

        return normComponents;
    }

    public int getDataElement(int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public int[] getUnnormalizedComponents(float normComponents[],
            int normOffset, int components[], int offset) {

        if (bits == null) {
            // awt.26C=bits is null
            throw new UnsupportedOperationException(Messages.getString("awt.26C")); //$NON-NLS-1$
        }

        if (normComponents.length - normOffset < numComponents) {
            // awt.273=The length of normComponents minus normOffset is less than numComponents
            throw new IllegalArgumentException(Messages.getString("awt.273")); //$NON-NLS-1$
        }

        if (components == null) {
            components = new int[numComponents + offset];
        } else {
            if (components.length - offset < numComponents) {
                // awt.272=The length of components minus offset is less than numComponents
                throw new IllegalArgumentException(Messages.getString("awt.272")); //$NON-NLS-1$
            }
        }

        if (hasAlpha && isAlphaPremultiplied) {
            float alpha = normComponents[normOffset + numColorComponents];
            for (int i = 0; i < numColorComponents; i++) {
                components[offset + i] = (int) (normComponents[normOffset + i]
                        * maxValues[i] * alpha + 0.5f);
            }
            components[offset + numColorComponents] =
                (int) (normComponents[normOffset + numColorComponents] *
                        maxValues[numColorComponents] + 0.5f);
        } else {
            for (int i = 0; i < numComponents; i++) {
                components[offset + i] =
                    (int) (normComponents[normOffset + i] *
                            maxValues[i] + 0.5f);
            }
        }

        return components;
    }

    public int getDataElement(float normComponents[], int normOffset) {
        int unnormComponents[] = getUnnormalizedComponents(normComponents,
                normOffset, null, 0);
        return getDataElement(unnormComponents, 0);
    }

    public int[] getComponents(int pixel, int components[], int offset) {
        throw new UnsupportedOperationException("This method is not " + //$NON-NLS-1$
                "supported by this ColorModel"); //$NON-NLS-1$
    }

    public abstract int getRed(int pixel);

    public int getRGB(int pixel) {
        return (getAlpha(pixel) << 24 | getRed(pixel) << 16
                | getGreen(pixel) << 8 | getBlue(pixel));
    }

    public abstract int getGreen(int pixel);

    public int getComponentSize(int componentIdx) {
        if (bits == null) {
            // awt.26C=bits is null
            throw new NullPointerException(Messages.getString("awt.26C")); //$NON-NLS-1$
        }

        if (componentIdx < 0 || componentIdx >= bits.length) {
            // awt.274=componentIdx is greater than the number of components or less than zero
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.274")); //$NON-NLS-1$
        }

        return bits[componentIdx];
    }

    public abstract int getBlue(int pixel);

    public abstract int getAlpha(int pixel);

    public int[] getComponentSize() {
        if (bits != null) {
            return bits.clone();
        }
        return null;
    }

    public final boolean isAlphaPremultiplied() {
        return isAlphaPremultiplied;
    }

    public final boolean hasAlpha() {
        return hasAlpha;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        int tmp;

        if (hasAlpha) {
            hash ^= 1;
            hash <<= 8;
        }
        if (isAlphaPremultiplied) {
            hash ^= 1;
            hash <<= 8;
        }

        tmp = hash >>> 24;
        hash ^= numColorComponents;
        hash <<= 8;
        hash |= tmp;

        tmp = hash >>> 24;
        hash ^= transparency;
        hash <<= 8;
        hash |= tmp;

        tmp = hash >>> 24;
        hash ^= cs.getType();
        hash <<= 8;
        hash |= tmp;

        tmp = hash >>> 24;
        hash ^= pixel_bits;
        hash <<= 8;
        hash |= tmp;

        tmp = hash >>> 24;
        hash ^= transferType;
        hash <<= 8;
        hash |= tmp;

        if (bits != null) {

            for (int element : bits) {
                tmp = hash >>> 24;
                hash ^= element;
                hash <<= 8;
                hash |= tmp;
            }

        }

        return hash;
    }

    public int getTransparency() {
        return transparency;
    }

    public final int getTransferType() {
        return transferType;
    }

    public int getPixelSize() {
        return pixel_bits;
    }

    public int getNumComponents() {
        return numComponents;
    }

    public int getNumColorComponents() {
        return numColorComponents;
    }

    public static ColorModel getRGBdefault() {
        if (RGBdefault == null) {
            RGBdefault = new DirectColorModel(32, 0x00ff0000, 0x0000ff00,
                    0x000000ff, 0xff000000);
        }
        return RGBdefault;
    }

    /*
     * Construct INT pixel representation from Object
     * @param obj
     *
     * @return
     */
    private int constructPixel(Object obj) {
        int pixel = 0;

        switch (getTransferType()) {

        case DataBuffer.TYPE_BYTE:
            byte[] bPixel = (byte[]) obj;
            if(bPixel.length > 1) {
                // awt.275=This pixel representation is not suuported by tis Color Model
                throw new UnsupportedOperationException(Messages.getString("awt.275")); //$NON-NLS-1$
            }
            pixel = bPixel[0] & 0xff;
            break;

        case DataBuffer.TYPE_USHORT:
            short[] sPixel = (short[]) obj;
            if(sPixel.length > 1) {
                // awt.275=This pixel representation is not suuported by tis Color Model
                throw new UnsupportedOperationException(Messages.getString("awt.275")); //$NON-NLS-1$
            }
            pixel = sPixel[0] & 0xffff;
            break;

        case DataBuffer.TYPE_INT:
            int[] iPixel = (int[]) obj;
            if(iPixel.length > 1) {
                // awt.275=This pixel representation is not suuported by tis Color Model
                throw new UnsupportedOperationException(Messages.getString("awt.275")); //$NON-NLS-1$
            }
            pixel = iPixel[0];
            break;

        default:
            // awt.22D=This transferType ( {0} ) is not supported by this color model
            throw new UnsupportedOperationException(Messages.getString("awt.22D", //$NON-NLS-1$
                   transferType));

        }
        return pixel;
    }

    static int getTransferType(int bits) {
        if (bits <= 8) {
            return DataBuffer.TYPE_BYTE;
        } else if (bits <= 16) {
            return DataBuffer.TYPE_USHORT;
        } else if (bits <= 32) {
            return DataBuffer.TYPE_INT;
        } else {
            return DataBuffer.TYPE_UNDEFINED;
        }
    }

    @Override
    public void finalize() {
        // This method is added for the API compatibility
        // Don't need to call super since Object's finalize is always empty
    }
}
