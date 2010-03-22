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

package org.apache.harmony.awt.gl.opengl;

import org.apache.harmony.awt.gl.render.Blitter;
import org.apache.harmony.awt.gl.*;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.misc.accessors.LockedArray;

import java.awt.geom.AffineTransform;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;

public class OGLBlitter implements Blitter {
    private static final GL gl = GL.getInstance();

    private static final OGLBlitter inst = new OGLBlitter();

    private static final class OGLImageParams {
        OGLImageParams(
                int format, int intFormat,
                int type, int alignment,
                boolean requiresConversion
        ) {
            oglFormat = format;
            oglIntFormat = intFormat;
            oglType = type;
            oglAlignment = alignment;
            this.requiresConversion = requiresConversion;
        }

        int oglFormat;
        int oglIntFormat;
        int oglType;
        int oglAlignment;
        boolean requiresConversion;
    }

    static final class OGLTextureParams {
        OGLTextureParams(int tName, int p2w, int p2h, int w, int h) {
            textureName = tName;
            this.p2w = p2w;
            this.p2h = p2h;
            width = w;
            height = h;
        }

        int textureName;

        // Actual texture width and height
        int p2w;
        int p2h;

        // Size of the used part of the texture
        int width;
        int height;

        final void deleteTexture() {
            deleteTexture(textureName);
            textureName = 0;
        }

        static final void deleteTexture(int textureName) {
            if (textureName != 0) {
                Int32Pointer texPtr =
                        NativeBridge.getInstance().createInt32Pointer(1, true);
                texPtr.set(0, textureName);
                gl.glDeleteTextures(1, texPtr);
                texPtr.free();
            }
        }
    };

    private static final OGLImageParams IMAGE_TYPE_MAPPING[] = new OGLImageParams [] {
        new OGLImageParams(GLDefs.GL_BGRA, 4, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, true), // TYPE_CUSTOM = 0
        new OGLImageParams(GLDefs.GL_BGR, 3, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, false), // TYPE_INT_RGB = 1
        new OGLImageParams(GLDefs.GL_BGRA, 4, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, false), // TYPE_INT_ARGB = 2
        new OGLImageParams(GLDefs.GL_BGRA, 4, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, false), // TYPE_INT_ARGB_PRE = 3
        new OGLImageParams(GLDefs.GL_RGB, 3, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, false), // TYPE_INT_BGR = 4
        new OGLImageParams(GLDefs.GL_BGR, 3, GLDefs.GL_UNSIGNED_BYTE, 1, false), // TYPE_3BYTE_BGR = 5
        new OGLImageParams(GLDefs.GL_RGBA, 4, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, false), // TYPE_4BYTE_ABGR = 6
        new OGLImageParams(GLDefs.GL_RGBA, 4, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, false), // TYPE_4BYTE_ABGR_PRE = 7
        new OGLImageParams(GLDefs.GL_RGB, 3, GLDefs.GL_UNSIGNED_SHORT_5_6_5, 2, false), // TYPE_USHORT_565_RGB = 8
        new OGLImageParams(GLDefs.GL_BGR, 3, GLDefs.GL_UNSIGNED_SHORT_1_5_5_5_REV, 2, false), // TYPE_USHORT_555_RGB = 9
        new OGLImageParams(GLDefs.GL_LUMINANCE, 1, GLDefs.GL_UNSIGNED_BYTE, 1, false), // TYPE_BYTE_GRAY = 10
        new OGLImageParams(GLDefs.GL_LUMINANCE, 1, GLDefs.GL_UNSIGNED_SHORT, 2, false), // TYPE_USHORT_GRAY = 11
        new OGLImageParams(GLDefs.GL_BGR, 3, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, true), // TYPE_BYTE_BINARY = 12
        new OGLImageParams(GLDefs.GL_BGRA, 4, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV, 4, true) // TYPE_BYTE_INDEXED = 13
    };

    public static OGLBlitter getInstance(){
        return inst;
    }

    public void blit(
            int srcX, int srcY, Surface srcSurf,
            int dstX, int dstY, Surface dstSurf,
            int width, int height,
            AffineTransform sysxform, AffineTransform xform,
            Composite comp, Color bgcolor, MultiRectArea clip
    ) {
        int type = xform.getType();
        switch (type) {
            case AffineTransform.TYPE_TRANSLATION:
                dstX += xform.getTranslateX();
                dstY += xform.getTranslateY();
            case AffineTransform.TYPE_IDENTITY:
                 blit(srcX, srcY, srcSurf, dstX, dstY, dstSurf,
                         width, height, sysxform, comp, bgcolor, clip);
                break;
            default:
                AffineTransform at = (AffineTransform) sysxform.clone();
                at.concatenate(xform);
                blit(srcX, srcY, srcSurf, dstX, dstY, dstSurf,
                        width, height, at, comp, bgcolor, clip);
        }
    }

    public void blit(
            int srcX, int srcY, Surface srcSurf,
            int dstX, int dstY, Surface dstSurf,
            int width, int height,
            AffineTransform sysxform,
            Composite comp, Color bgcolor, MultiRectArea clip
    ) {
        int type = sysxform.getType();
        switch (type) {
            case AffineTransform.TYPE_TRANSLATION:
                dstX += sysxform.getTranslateX();
                dstY += sysxform.getTranslateY();
            case AffineTransform.TYPE_IDENTITY:
                blitImpl(
                        srcX, srcY, srcSurf,
                        dstX, dstY, dstSurf,
                        width, height,
                        comp, bgcolor, clip,
                        null
                );
                break;
            default:
                blitImpl(
                        srcX, srcY, srcSurf,
                        dstX, dstY, dstSurf,
                        width, height,
                        comp, bgcolor, clip,
                        sysxform
                );
        }
    }

    public void blit(
            int srcX, int srcY, Surface srcSurf,
            int dstX, int dstY, Surface dstSurf,
            int width, int height,
            Composite comp, Color bgcolor, MultiRectArea clip
    ) {
        blitImpl(
            srcX, srcY, srcSurf,
            dstX, dstY, dstSurf,
            width, height,
            comp, bgcolor, clip,
            null
        );
    }

    private final void blitImpl(
            int srcX, int srcY, Surface srcSurf,
            int dstX, int dstY, Surface dstSurf,
            int width, int height,
            Composite comp, Color bgcolor, MultiRectArea clip,
            AffineTransform xform
    ) {
        OGLSurface oglDstSurf = (OGLSurface) dstSurf;
        OGLGraphics2D oglg = oglDstSurf.oglg;
        oglg.makeCurrent();

        // Set the requested clip, saving current clip
        MultiRectArea oldClip = (MultiRectArea) oglg.getClip();
        boolean needRestoreClip = false;
        if ((clip == null && oldClip != null) || !clip.equals(oldClip)) {
            oglg.setTransformedClip(clip);
            needRestoreClip = true;
        } else {
            oldClip = null;
        }

        // Fill the background if needed
        if (
                srcSurf.getColorModel().getTransparency() != Transparency.OPAQUE &&
                bgcolor != null
        ) {
            if (xform == null) {
                oglg.fillRect(dstX, dstY, width, height);
            } else {
                Rectangle bounds = new Rectangle(srcX, srcY, width, height);
                Shape tBounds = xform.createTransformedShape(bounds);
                AffineTransform dstT = AffineTransform.getTranslateInstance(dstX, dstY);
                tBounds = dstT.createTransformedShape(tBounds);
                Color savedc = oglg.getColor();
                oglg.setColor(bgcolor);
                oglg.fill(tBounds);
                oglg.setColor(savedc);
            }
        }

        if (srcX != 0 || srcY != 0) {
            gl.glPixelStoref(GLDefs.GL_UNPACK_SKIP_PIXELS, srcX);
            gl.glPixelStoref(GLDefs.GL_UNPACK_SKIP_ROWS, srcY);
        }

        ColorModel srcCM = srcSurf.getColorModel();

        boolean needRestoreComposite = false;
        boolean needPremultiply = false;
        if (comp instanceof AlphaComposite) {
            if (
                    !oglg.getComposite().equals(comp) ||
                    !srcCM.isAlphaPremultiplied() ||
                    srcCM.hasAlpha() == oglg.opaqueColor
            ) {
                needPremultiply =
                        OGLGraphics2D.enableAlphaComposite(
                                (AlphaComposite) comp,
                                srcCM.isAlphaPremultiplied(),
                                srcCM.hasAlpha()
                        );
                needRestoreComposite = true;
            }
        } else {
            // XXX - todo - custom composite
        }

        boolean oglSrc = false; //srcSurf instanceof OGLSurface;

        if (xform == null && !oglSrc) {
            // glCopyPixels works very slow on some NV GPU's.
            /*
            boolean copied = (srcSurf instanceof OGLSurface) &&
                oglg.copyArea(
                        srcX, srcY,
                        width, height,
                        dstX, dstY,
                        ((OGLSurface) srcSurf).oglg
                );

            if(!copied) {
            */
            blitImg2OGL(
                    srcSurf,
                    width, height,
                    dstX, dstY,
                    needPremultiply
            );
            //}
        } else {
            OGLTextureParams tp;
            if (oglSrc) { // We have opengl source with or w/o transform
                tp = blitOGL2OGLTexCached(
                        (OGLSurface) srcSurf,
                        oglDstSurf,
                        srcX, srcY,
                        dstX, dstY,
                        width, height
                );

                if (tp == null) { // Can't copy texture, use readPixels
                    oglSrc = false;
                    tp = blitImg2OGLTexCached(
                            srcSurf,
                            width, height,
                            needPremultiply
                    );
                }
            } else { // Non opengl source
                tp = blitImg2OGLTexCached(
                        srcSurf,
                        width, height,
                        needPremultiply
                );
            }

            double xCoord = (double)width/tp.p2w;
            double yCoord = (double)height/tp.p2h;

            double vertices[] = new double[8];
            if (!oglSrc) {
                vertices[0] = srcX;
                vertices[1] = srcY;
                vertices[2] = srcX + width;
                vertices[3] = srcY;
                vertices[4] = srcX + width;
                vertices[5] = srcY + height;
                vertices[6] = srcX;
                vertices[7] = srcY + height;
            } else {
                vertices[6] = srcX;
                vertices[7] = srcY;
                vertices[4] = srcX + width;
                vertices[5] = srcY;
                vertices[2] = srcX + width;
                vertices[3] = srcY + height;
                vertices[0] = srcX;
                vertices[1] = srcY + height;
            }

            if (xform != null) {
                xform.transform(vertices, 0, vertices, 0, 4);
            }

            for (int i = 0; i < vertices.length; i++) {
                vertices[i] += (i%2 == 0) ? dstX : dstY;
            }

            gl.glEnable(GLDefs.GL_TEXTURE_2D);
            gl.glBegin(GLDefs.GL_QUADS);

            gl.glTexCoord2d(0.0, 0.0);
            gl.glVertex2d(vertices[0], vertices[1]);
            gl.glTexCoord2d(xCoord, 0.0);
            gl.glVertex2d(vertices[2], vertices[3]);
            gl.glTexCoord2d(xCoord, yCoord);
            gl.glVertex2d(vertices[4], vertices[5]);
            gl.glTexCoord2d(0.0, yCoord);
            gl.glVertex2d(vertices[6], vertices[7]);

            gl.glEnd();
            gl.glFlush();
            gl.glDisable(GLDefs.GL_TEXTURE_2D);

            //tp.deleteTexture();
        }

        if (needRestoreClip) {
            oglg.setTransformedClip(oldClip);
        }

        if (needRestoreComposite) {
            oglg.checkComposite();
        }
    }

    private void blitImg2OGL(
            Surface srcSurf,
            int width, int height,
            int dstX, int dstY,
            boolean needPremultiply
    ) {
        OGLImageParams imageParams = IMAGE_TYPE_MAPPING[srcSurf.getSurfaceType()];
        boolean requiresConversion = imageParams.requiresConversion;
        if (requiresConversion || needPremultiply) {
            imageParams = IMAGE_TYPE_MAPPING[BufferedImage.TYPE_INT_ARGB_PRE];
        }

        gl.glPixelStorei(GLDefs.GL_UNPACK_ALIGNMENT, imageParams.oglAlignment);

        ColorModel srcCM = srcSurf.getColorModel();

        // Obtain image data in opengl-compatible format and draw the image
        if (requiresConversion || needPremultiply) {
            // Set the raster position to the destination point
            gl.glRasterPos2i(0, 0);
            gl.glBitmap(0, 0, 0, 0, dstX, -dstY, 0);

            gl.glPixelZoom(1, -1);

            // needPremultiply should be always false for OGLSurface, type cast is safe
            gl.glDrawPixels(
                    width, height,
                    imageParams.oglFormat, imageParams.oglType,
                    ((ImageSurface) srcSurf).getCachedData(
                            srcCM.isAlphaPremultiplied() || needPremultiply
                    )
            );
        } else {
            Object data;
            if (srcSurf instanceof OGLSurface) {
                data = ((OGLSurface) srcSurf).getBottomToTopData();

                // Set the raster position to the destination point
                gl.glRasterPos2i(0, 0);
                gl.glBitmap(0, 0, 0, 0, dstX, -dstY-height, 0);

                gl.glPixelZoom(1, 1);
            } else {
                data = srcSurf.getData();

                // Set the raster position to the destination point
                gl.glRasterPos2i(0, 0);
                gl.glBitmap(0, 0, 0, 0, dstX, -dstY, 0);

                gl.glPixelZoom(1, -1);
            }

            LockedArray ldata = Utils.arraccess.lockArrayShort(data);
            gl.glDrawPixels(
                    width, height,
                    imageParams.oglFormat, imageParams.oglType,
                    ldata.getAddress()
            );
            ldata.release();
        }
    }

    /**
     * Calculates the next power of 2 from the size
     * @param size - arbitrary positive integer
     * @return next to the size power of 2
     */
    private final static int p2(int size) {
        size--;
        size |= size >> 1;
        size |= size >> 2;
        size |= size >> 4;
        size |= size >> 8;
        size |= size >> 16;
        return ++size;
    }

    private final OGLTextureParams blitImg2OGLTex(
            Surface srcSurf,
            int width, int height,
            boolean needPremultiply,
            OGLTextureParams tp
    ) {
        OGLImageParams imageParams = IMAGE_TYPE_MAPPING[srcSurf.getSurfaceType()];
        boolean requiresConversion = imageParams.requiresConversion;
        if (requiresConversion || needPremultiply) {
            imageParams = IMAGE_TYPE_MAPPING[BufferedImage.TYPE_INT_ARGB_PRE];
        }

        gl.glPixelStorei(GLDefs.GL_UNPACK_ALIGNMENT, imageParams.oglAlignment);

        ColorModel srcCM = srcSurf.getColorModel();

        int p2w = p2(width);
        int p2h = p2(height);

        if (tp == null) {
            Int32Pointer texPtr =
                    NativeBridge.getInstance().createInt32Pointer(1, true);

            gl.glGenTextures(1, texPtr);
            int texName = texPtr.get(0);
            gl.glBindTexture(GLDefs.GL_TEXTURE_2D, texName);
            texPtr.free();

            gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MAG_FILTER, GLDefs.GL_NEAREST);
            gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MIN_FILTER, GLDefs.GL_NEAREST);

            tp = new OGLTextureParams(texName, p2w, p2h, width, height);
        } else {
            gl.glBindTexture(GLDefs.GL_TEXTURE_2D, tp.textureName);
            tp.width = width;
            tp.height = height;
            tp.p2w = p2w;
            tp.p2h = p2h;
        }

        // XXX - todo - check for texture non p2 extension
        gl.glTexImage2D(
                GLDefs.GL_TEXTURE_2D, 0,
                imageParams.oglIntFormat,
                p2w, p2h, 0,
                imageParams.oglFormat, imageParams.oglType,
                0
        );

        // Obtain image data in opengl-compatible format and draw the image
        if (requiresConversion || needPremultiply) {
            // needPremultiply should be always false for OGLSurface, type cast is safe
            gl.glTexSubImage2D(
                    GLDefs.GL_TEXTURE_2D, 0, 0, 0,
                    //imageParams.oglIntFormat,
                    width, height, // 0,
                    imageParams.oglFormat, imageParams.oglType,
                    ((ImageSurface) srcSurf).getCachedData(
                            srcCM.isAlphaPremultiplied() || needPremultiply
                    )
            );
        } else {
            Object data = srcSurf.getData();
            LockedArray ldata = Utils.arraccess.lockArrayShort(data);
            gl.glTexSubImage2D(
                    GLDefs.GL_TEXTURE_2D, 0, 0, 0,
                    //imageParams.oglIntFormat,
                    width, height, //0,
                    imageParams.oglFormat, imageParams.oglType,
                    ldata.getAddress()
            );

            ldata.release();
        }

        return tp;
    }

    final OGLTextureParams blitImg2OGLTexCached(
            Surface srcSurf,
            int width, int height,
            boolean needPremultiply
    ) {
        TextureCache tc = TextureCache.getInstance();
        OGLTextureParams tp = tc.findTexture(srcSurf);

        if (tp != null) {
            if (width > tp.width || height > tp.height || !srcSurf.isCaheValid(tp)) {
                tp = blitImg2OGLTex(
                        srcSurf,
                        width, height,
                        needPremultiply,
                        tp
                );
                srcSurf.addValidCache(tp);
            } else {
                gl.glBindTexture(GLDefs.GL_TEXTURE_2D, tp.textureName);
            }
        } else {
            tp = blitImg2OGLTex(
                    srcSurf,
                    width, height,
                    needPremultiply,
                    null
            );
            tc.add(srcSurf, tp);
            srcSurf.addValidCache(tp);
            tc.cleanupTextures();
        }

        gl.glTexEnvf(GLDefs.GL_TEXTURE_ENV, GLDefs.GL_TEXTURE_ENV_MODE, GLDefs.GL_REPLACE);

        return tp;
    }

    private final OGLTextureParams blitOGL2OGLTexCached(
            OGLSurface srcSurf,
            OGLSurface dstSurf,
            int srcX, int srcY,
            int dstX, int dstY,
            int width, int height
    ) {
        TextureCache tc = TextureCache.getInstance();
        OGLTextureParams tp = tc.findTexture(srcSurf);

        if (tp != null) {
            if (width > tp.width || height > tp.height || !srcSurf.isCaheValid(tp)) {
                tp = blitOGL2OGLTex(
                        srcSurf,
                        dstSurf,
                        srcX, srcY,
                        dstX, dstY,
                        width, height,
                        tp
                );
                srcSurf.addValidCache(tp);
            } else {
                gl.glBindTexture(GLDefs.GL_TEXTURE_2D, tp.textureName);
            }
        } else {
            tp = blitOGL2OGLTex(
                    srcSurf,
                    dstSurf,
                    srcX, srcY,
                    dstX, dstY,
                    width, height,
                    null
            );
            tc.add(srcSurf, tp);
            srcSurf.addValidCache(tp);
            tc.cleanupTextures();
        }

        gl.glTexEnvf(GLDefs.GL_TEXTURE_ENV, GLDefs.GL_TEXTURE_ENV_MODE, GLDefs.GL_REPLACE);

        return tp;
    }

    private final OGLTextureParams blitOGL2OGLTex(
            OGLSurface srcSurf,
            OGLSurface dstSurf,
            int srcX, int srcY,
            int dstX, int dstY,
            int width, int height,
            OGLTextureParams tp
    ) {
        OGLImageParams imageParams = IMAGE_TYPE_MAPPING[BufferedImage.TYPE_INT_ARGB_PRE];
        gl.glPixelStorei(GLDefs.GL_UNPACK_ALIGNMENT, imageParams.oglAlignment);

        int p2w = p2(width);
        int p2h = p2(height);

        if (tp == null) {
            Int32Pointer texPtr =
                    NativeBridge.getInstance().createInt32Pointer(1, true);

            gl.glGenTextures(1, texPtr);
            int texName = texPtr.get(0);
            gl.glBindTexture(GLDefs.GL_TEXTURE_2D, texName);
            texPtr.free();

            gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MAG_FILTER, GLDefs.GL_NEAREST);
            gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MIN_FILTER, GLDefs.GL_NEAREST);

            tp = new OGLTextureParams(texName, p2w, p2h, width, height);
        } else {
            gl.glBindTexture(GLDefs.GL_TEXTURE_2D, tp.textureName);
            tp.width = width;
            tp.height = height;
            tp.p2w = p2w;
            tp.p2h = p2h;
        }

        // XXX - todo - check for texture non p2 extension
        gl.glTexImage2D(
                GLDefs.GL_TEXTURE_2D, 0,
                imageParams.oglIntFormat,
                p2w, p2h, 0,
                imageParams.oglFormat, imageParams.oglType,
                0
        );

        // Obtain image data in opengl-compatible format and draw the image
        boolean copied = dstSurf.oglg.copyArea(
                srcX, srcY,
                width, height,
                dstX, dstY,
                (srcSurf).oglg,
                true
        );

        return copied ? tp : null;
    }

}
