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
 */

package org.apache.harmony.awt.gl.linux;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.AffineTransformOp;

import org.apache.harmony.awt.gl.*;
import org.apache.harmony.awt.gl.render.Blitter;
import org.apache.harmony.awt.gl.render.JavaBlitter;
import org.apache.harmony.awt.gl.render.NativeImageBlitter;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;

import org.apache.harmony.awt.internal.nls.Messages;

public class XBlitter implements Blitter {

    static final XBlitter inst = new XBlitter();

    public static XBlitter getInstance(){
        return inst;
    }
    public void blit(int srcX, int srcY, Surface srcSurf, int dstX, int dstY,
            Surface dstSurf, int width, int height, AffineTransform sysxform,
            AffineTransform xform, Composite comp, Color bgcolor,
            MultiRectArea clip) {

        if(xform == null){
            blit(srcX, srcY, srcSurf, dstX, dstY, dstSurf, width, height,
                    sysxform, comp, bgcolor, clip);
        }else{
            double scaleX = xform.getScaleX();
            double scaleY = xform.getScaleY();
            double scaledX = dstX / scaleX;
            double scaledY = dstY / scaleY;
            AffineTransform at = new AffineTransform();
            at.setToTranslation(scaledX, scaledY);
            xform.concatenate(at);
            sysxform.concatenate(xform);
            blit(srcX, srcY, srcSurf, 0, 0, dstSurf, width, height,
                    sysxform, comp, bgcolor, clip);
        }
    }

    public void blit(int srcX, int srcY, Surface srcSurf, int dstX, int dstY,
            Surface dstSurf, int width, int height, AffineTransform sysxform, Composite comp,
            Color bgcolor, MultiRectArea clip) {


        if(srcSurf.isNativeDrawable()){
            double matrix[] = null;
            if(sysxform != null){
                int type = sysxform.getType();
                switch (type) {

                    case AffineTransform.TYPE_TRANSLATION:
                        dstX += sysxform.getTranslateX();
                        dstY += sysxform.getTranslateY();
                    case AffineTransform.TYPE_IDENTITY:
                        break;

                    default:
                        matrix = new double[6];
                        sysxform.getMatrix(matrix);
                        Rectangle transDstBounds = JavaBlitter.getBounds2D(sysxform, new Rectangle(dstX, dstY, width, height)).getBounds();
                        dstX = transDstBounds.x;
                        dstY = transDstBounds.y;
 
                        Rectangle transSrcBounds = JavaBlitter.getBounds2D(sysxform, new Rectangle(srcX, srcY, width, height)).getBounds();
                        srcX = transSrcBounds.x;
                        srcY = transSrcBounds.y;
                        width = transSrcBounds.width;
                        height = transSrcBounds.height;

                }


            }

            long dstSurfStruct = dstSurf.getSurfaceDataPtr();
            long srcSurfStruct = srcSurf.getSurfaceDataPtr();
            int clipRects[] = null;
            int numVertex = 0;
            if(clip != null){
                clipRects = clip.rect;
                numVertex = clipRects[0] - 1;
            }

            if(comp instanceof AlphaComposite){
                AlphaComposite ac = (AlphaComposite) comp;
                int compType = ac.getRule();
                float alpha = ac.getAlpha();

                if(srcSurf instanceof ImageSurface){
                    Object data = srcSurf.getData();

                    int dirtyRegions[] = ((ImageSurface) srcSurf).getDirtyRegions();
                    int regCount = 0;
                    if(dirtyRegions != null) regCount = dirtyRegions[0] - 1;
                    
                    boolean hasBackground = false;
                    int bgc = 0;

                    if(bgcolor != null && srcSurf.getTransparency() != Transparency.OPAQUE){
                        hasBackground = true;
                        bgc = bgcolor.getRGB();
                    }

                    synchronized(data){
                        bltImage(srcX, srcY, srcSurfStruct, srcSurf.getData(),
                             dstX, dstY, dstSurfStruct, width, height, 
                             hasBackground, bgc, compType, alpha, matrix, 
                             clipRects, numVertex, srcSurf.invalidated(), 
                             dirtyRegions, regCount);
                    }
                    srcSurf.validate();
                }else{
                    bltPixmap(srcX, srcY, srcSurfStruct,
                            dstX, dstY, dstSurfStruct,
                            width, height, compType, alpha, matrix,
                            clipRects, numVertex);
                }
            }else if(comp instanceof XORComposite){
                XORComposite xcomp = (XORComposite) comp;
                if(srcSurf instanceof ImageSurface){
                    Object data = srcSurf.getData();

                    int dirtyRegions[] = ((ImageSurface) srcSurf).getDirtyRegions();
                    int regCount = 0;
                    if(dirtyRegions != null) regCount = dirtyRegions[0] - 1;

                    synchronized(data){
                        xorImage(srcX, srcY, srcSurfStruct, data,
                                dstX, dstY, dstSurfStruct,
                                width, height, xcomp.getXORColor().getRGB(), matrix,
                                clipRects, numVertex, 
                                srcSurf.invalidated(), dirtyRegions, regCount);
                    }
                    srcSurf.validate();
                }else{
                    xorPixmap(srcX, srcY, srcSurfStruct,
                            dstX, dstY, dstSurfStruct,
                            width, height, xcomp.getXORColor().getRGB(), matrix,
                            clipRects, numVertex);
                }
            }else{
                // awt.17=Unknown Composite type : {0}
                throw new IllegalArgumentException(Messages.getString("awt.17",  //$NON-NLS-1$
                        comp.getClass()));
            }
        }else{
            BufferedImage bi;
            if(srcSurf.getTransparency() == Transparency.OPAQUE){
                bi = new BufferedImage(srcSurf.getWidth(), srcSurf.getHeight(), BufferedImage.TYPE_INT_RGB);
            }else{
                bi = new BufferedImage(srcSurf.getWidth(), srcSurf.getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
            Surface tmpSurf = AwtImageBackdoorAccessor.getInstance().getImageSurface(bi);
            JavaBlitter.getInstance().blit(0, 0, srcSurf, 0, 0, tmpSurf,
                    srcSurf.getWidth(), srcSurf.getHeight(),
                    AlphaComposite.Src, null, null);
            blit(srcX, srcY, tmpSurf, dstX, dstY, dstSurf,
                    width, height, comp, bgcolor, clip);
        }
    }

    // Native methods

    public void blit(int srcX, int srcY, Surface srcSurf, int dstX, int dstY,
            Surface dstSurf, int width, int height,
            Composite comp, Color bgcolor, MultiRectArea clip) {

        blit(srcX, srcY, srcSurf, dstX, dstY, dstSurf, width, height,
                null, comp, bgcolor, clip);
    }

    private native void bltImage(int srcX, int srcY, long srsSurfDataPtr,
            Object srcData, int dstX, int dstY, long dstSurfDataPtr,
            int width, int height, boolean hasBackground, int bgcolor, int compType,
            float alpha, double matrix[], int clip[], int numVertex, 
            boolean invalidated, int[] dirtyRegions, int regCount);

    private native void bltPixmap(int srcX, int srcY, long srsSurfDataPtr,
            int dstX, int dstY, long dstSurfDataPtr,
            int width, int height, int compType,
            float alpha, double matrix[], int clip[], int numVertex);

    private native void xorImage(int srcX, int srcY, long srsSurfDataPtr,
            Object srcData, int dstX, int dstY, long dstSurfDataPtr,
            int width, int height, int xorcolor, double matrix[], int clip[], 
            int numVertex, boolean invalidated, int[] dirtyRegions, int regCount);

    private native void xorPixmap(int srcX, int srcY, long srsSurfDataPtr,
            int dstX, int dstY, long dstSurfDataPtr,
            int width, int height, int xorcolor, double matrix[],
            int clip[], int numVertex);
}

