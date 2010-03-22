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
 * @author Oleg V. Khaschansky
 *
 * @date: Sep 29, 2005
 */

package java.awt.image;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import org.apache.harmony.awt.gl.AwtImageBackdoorAccessor;
import org.apache.harmony.awt.internal.nls.Messages;

public class ConvolveOp implements BufferedImageOp, RasterOp {

    public static final int EDGE_ZERO_FILL = 0;

    public static final int EDGE_NO_OP = 1;

    private Kernel kernel;
    private int edgeCond;
    private RenderingHints rhs = null;

    static {
        // TODO
        //System.loadLibrary("imageops");
    }

    public ConvolveOp(Kernel kernel, int edgeCondition, RenderingHints hints) {
        this.kernel = kernel;
        this.edgeCond = edgeCondition;
        this.rhs = hints;
    }

    public ConvolveOp(Kernel kernel) {
        this.kernel = kernel;
        this.edgeCond = EDGE_ZERO_FILL;
    }

    public final Kernel getKernel() {
        return (Kernel) kernel.clone();
    }

    public final RenderingHints getRenderingHints() {
        return rhs;
    }

    public int getEdgeCondition() {
        return edgeCond;
    }

    public final Rectangle2D getBounds2D(Raster src) {
        return src.getBounds();
    }

    public final Rectangle2D getBounds2D(BufferedImage src) {
        return getBounds2D(src.getRaster());
    }

    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Float();
        }

        dstPt.setLocation(srcPt);
        return dstPt;
    }

    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster();
    }

    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
        if (dstCM == null) {
            dstCM = src.getColorModel();
        }

        if (dstCM instanceof IndexColorModel) {
            dstCM = ColorModel.getRGBdefault();
        }

        WritableRaster r =
                dstCM.isCompatibleSampleModel(src.getSampleModel()) ?
                src.getRaster().createCompatibleWritableRaster(src.getWidth(), src.getHeight()) :
                dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight());

        return new BufferedImage(
                dstCM,
                r,
                dstCM.isAlphaPremultiplied(),
                null
        );
    }

    public final WritableRaster filter(Raster src, WritableRaster dst) {
        if (src == null) { // Should throw according to spec
            // awt.256=Source raster is null
            throw new NullPointerException(Messages.getString("awt.256")); //$NON-NLS-1$
        }

        if (src == dst){
            // awt.257=Source raster is equal to destination
            throw new IllegalArgumentException(Messages.getString("awt.257")); //$NON-NLS-1$
        }

        if (dst == null) {
            dst = createCompatibleDestRaster(src);
        } else if (src.getNumBands() != dst.getNumBands()) {
            // awt.258=Number of source bands ({0}) is not equal to number of destination bands ({1})
            throw new IllegalArgumentException(
                Messages.getString("awt.258", src.getNumBands(), dst.getNumBands())); //$NON-NLS-1$
        }

        // TODO
        //if (ippFilter(src, dst, BufferedImage.TYPE_CUSTOM) != 0)
            if (slowFilter(src, dst) != 0) {
                // awt.21F=Unable to transform source
                throw new ImagingOpException (Messages.getString("awt.21F")); //$NON-NLS-1$
            }

        return dst;
    }

    private int slowFilter(Raster src, WritableRaster dst) {
        try {
            SampleModel sm = src.getSampleModel();

            int numBands = src.getNumBands();
            int srcHeight = src.getHeight();
            int srcWidth = src.getWidth();

            int xOrigin = kernel.getXOrigin();
            int yOrigin = kernel.getYOrigin();
            int kWidth = kernel.getWidth();
            int kHeight = kernel.getHeight();
            float[] data = kernel.getKernelData(null);

            int srcMinX = src.getMinX();
            int srcMinY = src.getMinY();
            int dstMinX = dst.getMinX();
            int dstMinY = dst.getMinY();

            int srcConvMaxX = srcWidth - (kWidth - xOrigin - 1);
            int srcConvMaxY = srcHeight - (kHeight - yOrigin - 1);

            int[] maxValues = new int[numBands];
            int[] masks = new int[numBands];
            int[] sampleSizes = sm.getSampleSize();

            for (int i=0; i < numBands; i++){
                maxValues[i] = (1 << sampleSizes[i]) - 1;
                masks[i] = ~(maxValues[i]);
            }

            // Processing bounds
            float[] pixels = null;
            pixels = src.getPixels(srcMinX, srcMinY, srcWidth, srcHeight, pixels);
            float[] newPixels = new float[pixels.length];
            int rowLength = srcWidth*numBands;
            if (this.edgeCond == ConvolveOp.EDGE_NO_OP){
                // top
                int start = 0;
                int length = yOrigin*rowLength;
                System.arraycopy(pixels, start, newPixels, start, length);
                // bottom
                start = (srcHeight - (kHeight - yOrigin - 1))*rowLength;
                length = (kHeight - yOrigin - 1)*rowLength;
                System.arraycopy(pixels, start, newPixels, start, length);
                // middle
                length = xOrigin*numBands;
                int length1 = (kWidth - xOrigin - 1)*numBands;
                start = yOrigin*rowLength;
                int start1 = (yOrigin+1)*rowLength - length1;
                for (int i = yOrigin; i < (srcHeight - (kHeight - yOrigin - 1)); i ++) {
                    System.arraycopy(pixels, start, newPixels, start, length);
                    System.arraycopy(pixels, start1, newPixels, start1, length1);
                    start +=rowLength;
                    start1 +=rowLength;
                }

            }

            // Cycle over pixels to be calculated
            for (int i = yOrigin; i < srcConvMaxY; i++){
                for (int j = xOrigin; j < srcConvMaxX; j++){

                    // Take kernel data in backward direction, convolution
                    int kernelIdx = data.length - 1;

                    int pixelIndex = i * rowLength + j * numBands;
                    for (int hIdx = 0, rasterHIdx = i - yOrigin;
                         hIdx < kHeight;
                         hIdx++, rasterHIdx++
                            ){
                        for (int wIdx = 0, rasterWIdx = j - xOrigin;
                             wIdx < kWidth;
                             wIdx++, rasterWIdx++
                                ){
                            int curIndex = rasterHIdx * rowLength + rasterWIdx * numBands;
                            for (int idx=0; idx < numBands; idx++){
                                newPixels[pixelIndex+idx] += data[kernelIdx] * pixels[curIndex+idx];
                            }
                            kernelIdx--;
                        }
                    }

                    // Check for overflow now
                    for (int idx=0; idx < numBands; idx++){
                        if (((int)newPixels[pixelIndex+idx] & masks[idx]) != 0) {
                            if (newPixels[pixelIndex+idx] < 0) {
                                newPixels[pixelIndex+idx] = 0;
                            } else {
                                newPixels[pixelIndex+idx] = maxValues[idx];
                            }
                        }
                    }
                }
            }

            dst.setPixels(dstMinX, dstMinY, srcWidth, srcHeight, newPixels);
        } catch (Exception e) { // Something goes wrong, signal error
            return 1;
        }
        return 0;
    }

    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (src == null) {
            // awt.259=Source image is null
            throw new NullPointerException(Messages.getString("awt.259")); //$NON-NLS-1$
        }

        if (src == dst){
            // awt.25A=Source equals to destination
            throw new IllegalArgumentException(Messages.getString("awt.25A")); //$NON-NLS-1$
        }

        ColorModel srcCM = src.getColorModel();
        BufferedImage finalDst = null;

        if (srcCM instanceof IndexColorModel) {
            src = ((IndexColorModel)srcCM).convertToIntDiscrete(src.getRaster(), true);
            srcCM = src.getColorModel();
        }

        if (dst == null) {
            dst = createCompatibleDestImage(src, srcCM);
        } else {
            if (!srcCM.equals(dst.getColorModel())) {
                // Treat BufferedImage.TYPE_INT_RGB and BufferedImage.TYPE_INT_ARGB as same
                if (
                        !((src.getType() == BufferedImage.TYPE_INT_RGB ||
                           src.getType() == BufferedImage.TYPE_INT_ARGB) &&
                          (dst.getType() == BufferedImage.TYPE_INT_RGB ||
                           dst.getType() == BufferedImage.TYPE_INT_ARGB))
                ) {
                    finalDst = dst;
                    dst = createCompatibleDestImage(src, srcCM);
                }
            }
        }

        // Skip alpha channel for TYPE_INT_RGB images
        // TODO
        //if (ippFilter(src.getRaster(), dst.getRaster(), src.getType()) != 0)
            if (slowFilter(src.getRaster(), dst.getRaster()) != 0) {
                // awt.21F=Unable to transform source
                throw new ImagingOpException (Messages.getString("awt.21F")); //$NON-NLS-1$
            }

        if (finalDst != null) {
            Graphics2D g = finalDst.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.drawImage(dst, 0, 0, null);
        } else {
            finalDst = dst;
        }

        return finalDst;
    }

    // TODO remove when this method is used
    @SuppressWarnings("unused")
    private int ippFilter(Raster src, WritableRaster dst, int imageType) {
        int srcStride, dstStride;
        boolean skipChannel = false;
        int channels;
        int offsets[] = null;

        switch (imageType) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR: {
                channels = 4;
                srcStride = src.getWidth()*4;
                dstStride = dst.getWidth()*4;
                skipChannel = true;
                break;
            }

            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE: {
                channels = 4;
                srcStride = src.getWidth()*4;
                dstStride = dst.getWidth()*4;
                break;
            }

            case BufferedImage.TYPE_BYTE_GRAY: {
                channels = 1;
                srcStride = src.getWidth();
                dstStride = dst.getWidth();
                break;
            }

            case BufferedImage.TYPE_3BYTE_BGR: {
                channels = 3;
                srcStride = src.getWidth()*3;
                dstStride = dst.getWidth()*3;
                break;
            }

            case BufferedImage.TYPE_USHORT_GRAY: // TODO - could be done in native code?
            case BufferedImage.TYPE_USHORT_565_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_BYTE_BINARY: {
                return slowFilter(src, dst);
            }

            default: {
                SampleModel srcSM = src.getSampleModel();
                SampleModel dstSM = dst.getSampleModel();

                if (
                        srcSM instanceof PixelInterleavedSampleModel &&
                        dstSM instanceof PixelInterleavedSampleModel
                ) {
                    // Check PixelInterleavedSampleModel
                    if (
                            srcSM.getDataType() != DataBuffer.TYPE_BYTE ||
                            dstSM.getDataType() != DataBuffer.TYPE_BYTE
                    ) {
                        return slowFilter(src, dst);
                    }

                    channels = srcSM.getNumBands(); // Have IPP functions for 1, 3 and 4 channels
                    if (!(channels == 1 || channels == 3 || channels == 4)) {
                        return slowFilter(src, dst);
                    }

                    srcStride = ((ComponentSampleModel) srcSM).getScanlineStride();
                    dstStride = ((ComponentSampleModel) dstSM).getScanlineStride();
                } else if (
                        srcSM instanceof SinglePixelPackedSampleModel &&
                        dstSM instanceof SinglePixelPackedSampleModel
                ) {
                    // Check SinglePixelPackedSampleModel
                    SinglePixelPackedSampleModel sppsm1 = (SinglePixelPackedSampleModel) srcSM;
                    SinglePixelPackedSampleModel sppsm2 = (SinglePixelPackedSampleModel) dstSM;

                    channels = sppsm1.getNumBands();

                     // TYPE_INT_RGB, TYPE_INT_ARGB...
                    if (
                            sppsm1.getDataType() != DataBuffer.TYPE_INT ||
                            sppsm2.getDataType() != DataBuffer.TYPE_INT ||
                            !(channels == 3 || channels == 4)
                    ) {
                        return slowFilter(src, dst);
                    }

                    // Check compatibility of sample models
                    if (
                            !Arrays.equals(sppsm1.getBitOffsets(), sppsm2.getBitOffsets()) ||
                            !Arrays.equals(sppsm1.getBitMasks(), sppsm2.getBitMasks())
                    ) {
                        return slowFilter(src, dst);
                    }

                    for (int i=0; i<channels; i++) {
                        if (sppsm1.getSampleSize(i) != 8) {
                            return slowFilter(src, dst);
                        }
                    }

                    if (channels == 3) { // Cannot skip channel, don't know which is alpha...
                        channels = 4;
                    }

                    srcStride = sppsm1.getScanlineStride() * 4;
                    dstStride = sppsm2.getScanlineStride() * 4;
                } else {
                    return slowFilter(src, dst);
                }

                // Fill offsets if there's a child raster
                if (src.getParent() != null || dst.getParent() != null) {
                    if (
                            src.getSampleModelTranslateX() != 0 ||
                            src.getSampleModelTranslateY() != 0 ||
                            dst.getSampleModelTranslateX() != 0 ||
                            dst.getSampleModelTranslateY() != 0
                    ) {
                        offsets = new int[4];
                        offsets[0] = -src.getSampleModelTranslateX() + src.getMinX();
                        offsets[1] = -src.getSampleModelTranslateY() + src.getMinY();
                        offsets[2] = -dst.getSampleModelTranslateX() + dst.getMinX();
                        offsets[3] = -dst.getSampleModelTranslateY() + dst.getMinY();
                    }
                }
            }
        }

        Object srcData, dstData;
        AwtImageBackdoorAccessor dbAccess = AwtImageBackdoorAccessor.getInstance();
        try {
            srcData = dbAccess.getData(src.getDataBuffer());
            dstData = dbAccess.getData(dst.getDataBuffer());
        } catch (IllegalArgumentException e) {
            return -1; // Unknown data buffer type
        }

        return ippFilter32f(
            kernel.data, kernel.getWidth(), kernel.getHeight(),
            kernel.getXOrigin(), kernel.getYOrigin(), edgeCond,
            srcData, src.getWidth(), src.getHeight(), srcStride,
            dstData, dst.getWidth(), dst.getHeight(), dstStride,
            channels, skipChannel, offsets
        );
    }

    private native int ippFilter32f(
                float kernel[], int kWidth, int kHeight,
                int anchorX, int anchorY, int borderType,
                Object src, int srcWidth, int srcHeight, int srcStride,
                Object dst, int dstWidth, int dstHeight, int dstStride,
                int channels, boolean skipChannel, int offsets[]
            );
}

