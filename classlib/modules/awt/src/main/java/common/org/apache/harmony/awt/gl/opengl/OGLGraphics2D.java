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

import org.apache.harmony.awt.gl.CommonGraphics2D;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.render.NullBlitter;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.misc.accessors.LockedArray;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.font.GlyphVector;
import java.util.Arrays;
import java.math.BigInteger;

public final class OGLGraphics2D extends CommonGraphics2D {

    private static final int[] BLEND_RULE_MAPPING_SRC_PREMULT = new int[] {
        -1, // No rule
        GLDefs.GL_ZERO, // 1, Clear
        GLDefs.GL_ONE, // 2, Src
        GLDefs.GL_ONE, // 3, SrcOver
        GLDefs.GL_ONE_MINUS_DST_ALPHA, // 4, DstOver
        GLDefs.GL_DST_ALPHA, // 5, SrcIn
        GLDefs.GL_ZERO, // 6, DstIn
        GLDefs.GL_ONE_MINUS_DST_ALPHA, // 7, SrcOut
        GLDefs.GL_ZERO, // 8, DstOut
        GLDefs.GL_ZERO, // 9, Dst
        GLDefs.GL_DST_ALPHA, // 10, SrcAtop
        GLDefs.GL_ONE_MINUS_DST_ALPHA, // 11, DstAtop
        GLDefs.GL_ONE_MINUS_DST_ALPHA // 11, Xor
    };

    private static final int[] BLEND_RULE_MAPPING_DST = new int[] {
        -1, // No rule
        GLDefs.GL_ZERO, // 1, Clear
        GLDefs.GL_ZERO, // 2, Src
        GLDefs.GL_ONE_MINUS_SRC_ALPHA, // 3, SrcOver
        GLDefs.GL_ONE, // 4, DstOver
        GLDefs.GL_ZERO, // 5, SrcIn
        GLDefs.GL_SRC_ALPHA, // 6, DstIn
        GLDefs.GL_ZERO, // 7, SrcOut
        GLDefs.GL_ONE_MINUS_SRC_ALPHA, // 8, DstOut
        GLDefs.GL_ONE, // 9, Dst
        GLDefs.GL_ONE_MINUS_SRC_ALPHA, // 10, SrcAtop
        GLDefs.GL_SRC_ALPHA, // 11, DstAtop
        GLDefs.GL_ONE_MINUS_SRC_ALPHA // 11, Xor
    };

    private static final boolean[] HAVE_MAPPING_NO_PREMULT = new boolean[] {
        false, // No rule
        true, // 1, Clear
        true, // 2, Src
        true, // 3, SrcOver
        false, // 4, DstOver
        false, // 5, SrcIn
        true, // 6, DstIn
        false, // 7, SrcOut
        true, // 8, DstOut
        true, // 9, Dst
        false, // 10, SrcAtop
        false, // 11, DstAtop
        false // 11, Xor
    };

    private static final int[] BLEND_RULE_MAPPING_SRC_NO_PREMULT = new int[] {
        -1, // No rule
        GLDefs.GL_ZERO, // 1, Clear
        GLDefs.GL_SRC_ALPHA, // 2, Src
        GLDefs.GL_SRC_ALPHA, // 3, SrcOver
        -1, // 4, DstOver
        -1, // 5, SrcIn
        GLDefs.GL_ZERO, // 6, DstIn
        -1, // 7, SrcOut
        GLDefs.GL_ZERO, // 8, DstOut
        GLDefs.GL_ZERO, // 9, Dst
        -1, // 10, SrcAtop
        -1, // 11, DstAtop
        -1 // 11, Xor
    };

    private static final int[] BLEND_RULE_MAPPING_DST_NO_ALPHA = new int[] {
        -1, // No rule
        GLDefs.GL_ZERO, // 1, Clear
        GLDefs.GL_ZERO, // 2, Src
        GLDefs.GL_ZERO, // 3, SrcOver
        GLDefs.GL_ONE, // 4, DstOver
        GLDefs.GL_ZERO, // 5, SrcIn
        GLDefs.GL_ONE, // 6, DstIn
        GLDefs.GL_ZERO, // 7, SrcOut
        GLDefs.GL_ZERO, // 8, DstOut
        GLDefs.GL_ONE, // 9, Dst
        GLDefs.GL_ZERO, // 10, SrcAtop
        GLDefs.GL_ONE, // 11, DstAtop
        GLDefs.GL_ZERO, // 11, Xor
    };

    private static final GL gl = GL.getInstance();

    private final double[] javaTransformMx = new double[6];
    private final double[] glTransformMx = new double[16];

    private OGLContextManager ctxmgr;
    //private long oglContext;

    private NativeWindow nwin;
    Rectangle winBounds; // Cached native window bounds

    // Can't use transform from CommonGraphics, want to get all mra's untransformed
    private AffineTransform glTransform = new AffineTransform();

    private final byte fgRgba[] = new byte[4];
    boolean opaqueColor = true;
    private float acAlpha = 1.0f;

    private boolean oglPaint = true;
    private boolean texPaint = false;
    private boolean nativeLines = true;
    private boolean scalingTransform = false;

    private short stipplePattern;
    private int stippleFactor;

    /**
     * gradTexName is a 2 pixel 1d texture object, used to draw gradient.
     */
    private int gradTexName = 0;
    /**
     * gradObjectPlane is used for 1d texture coordinates generation
     * when gradient paint is enabled
     */
    private double gradObjectPlane[];
    private boolean isGPCyclic;

    private long oshdc = 0; // device context for windows offscreen image

    public OGLGraphics2D(NativeWindow nwin, int tx, int ty, MultiRectArea clip) {
        if (nwin instanceof OGLVolatileImage.OGLOffscreenWindow) {
            OGLVolatileImage.OGLOffscreenWindow offWin = (OGLVolatileImage.OGLOffscreenWindow) nwin;
            oshdc = offWin.getHdc();
        }

        this.nwin = nwin;

        ctxmgr = (OGLContextManager) getDeviceConfiguration();
        //long oglContext = ctxmgr.getOGLContext();

        // Get the viewport (=native window) width and height
        winBounds = nwin.getBounds();

        if (clip != null) {
            Rectangle bounds = clip.getBounds();
            dstSurf = new OGLSurface(bounds.width, bounds.height, this);
        } else {
            dstSurf = new OGLSurface(winBounds.width, winBounds.height, this);
        }

        makeCurrent();

        resetBounds();

        // Apply the translation
        gl.glLoadIdentity();
        glTransform = AffineTransform.getTranslateInstance(tx, ty);
        gl.glTranslated(tx, ty, 0);

        // Super class constructor sets untransformed clip
        setTransformedClip(clip);

        origPoint = new Point(tx, ty);

        blitter = OGLBlitter.getInstance();

        if (!FontManager.IS_FONTLIB) {
            jtr = new OGLTextRenderer();
        }
    }

    public OGLGraphics2D(NativeWindow nwin, int tx, int ty, int width, int height) {
        this(nwin, tx, ty, new MultiRectArea(new Rectangle(width, height)));
    }

    @Override
    public Graphics create() {
        OGLGraphics2D res = new OGLGraphics2D(
                nwin,
                0, 0,
                dstSurf.getWidth(), dstSurf.getHeight()
        );

        copyInternalFields(res);

        // Have to copy transform and clip explicitly, since we use opengl transforms
        res.setTransform(new AffineTransform(glTransform));
        if (clip == null) {
            res.setTransformedClip(null);
        } else {
            res.setTransformedClip(new MultiRectArea(clip));
        }

        return res;
    }

    @Override
    public void finalize() {
        super.finalize();
        try {
            EventQueue.invokeAndWait(
                    new Runnable() {
                        public void run () { dispose(); }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace(); // Something bad happened
        }
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return env.getDefaultScreenDevice().getDefaultConfiguration();
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        makeCurrent();

        gl.glPixelZoom(1, 1);

        // Raster position could be outside of the viewport, use glBitmap
        gl.glRasterPos2i(0, 0);
        gl.glBitmap(0, 0, 0, 0, x + dx, -y-dy-height, 0);

        x += glTransform.getTranslateX() + 0.5;
        y += glTransform.getTranslateY() + 0.5;

        gl.glCopyPixels(
                x, winBounds.height-y-height,
                width, height,
                GLDefs.GL_COLOR
        );
        gl.glFlush();
        getSurface().updateScene();
    }

    @Override
    public void setPaint(Paint paint) {
        if (paint == null)
            return;
                
        super.setPaint(paint);

        makeCurrent();

        if (paint instanceof Color) {
            deactivateTexturePaint();
            deactivateGradientPaint();
            setColor((Color)paint);
            oglPaint = true;
            texPaint = false;
        } else if (paint instanceof GradientPaint) {
            deactivateTexturePaint();
            activateGradientPaint((GradientPaint) paint);
            oglPaint = true;
            texPaint = false;
        } else if (paint instanceof TexturePaint) {
            deactivateGradientPaint();
            activateTexturePaint((TexturePaint) paint);
            oglPaint = true;
            texPaint = true;
        } else {
            deactivateTexturePaint();
            deactivateGradientPaint();
            oglPaint = false;
            texPaint = false;
        }
    }

    void resetPaint() {
        if (gradTexName != 0) {
            deactivateTexturePaint();
            reactivateGradientPaint();
        } else {
            setPaint(paint);
        }
    }

    @Override
    public void setColor(Color color) {
        if (color == null) {
            return;
        }

        super.setColor(color);

        makeCurrent();

        // Set gl color
        int val = color.getRGB();
        fgRgba[0] = (byte) ((val >> 16) & 0xFF);
        fgRgba[1] = (byte) ((val >> 8) & 0xFF);
        fgRgba[2] = (byte) (val & 0xFF);
        fgRgba[3] = (byte) ((val >> 24) & 0xFF);

        // Need to premultiply alpha
        if ((fgRgba[3] & 0xFF) != 0xFF || acAlpha != 1.0f) {
            float alpha = (fgRgba[3] & 0xFF) / 255.0f;
            fgRgba[0] = (byte) ((fgRgba[0] & 0xFF) * acAlpha * alpha + 0.5);
            fgRgba[1] = (byte) ((fgRgba[1] & 0xFF) * acAlpha * alpha + 0.5);
            fgRgba[2] = (byte) ((fgRgba[2] & 0xFF) * acAlpha * alpha + 0.5);
            // Also use acAlpha
            fgRgba[3] = (byte) ((fgRgba[3] & 0xFF) * acAlpha + 0.5);
        }

        LockedArray lColor = Utils.arraccess.lockArrayShort(fgRgba);
        gl.glColor4ubv(lColor.getAddress());
        lColor.release();

        // If color becomes translucent, probably need to enable blending
        if (opaqueColor != ((fgRgba[3] & 0xFF) == 0xFF)) {
            opaqueColor = (fgRgba[3] & 0xFF) == 0xFF;
            checkComposite();
        }

        // And, finally, update paint
        if (texPaint) {
            deactivateTexturePaint();
            texPaint = false;
        } else if (gradTexName != 0) {
            deactivateGradientPaint();
        }
        paint = color;
        oglPaint = true;
    }

    @Override
    public void dispose() {
        super.dispose();
        /*
        if (oglContext != 0) {
            ctxmgr.destroyOGLContext(oglContext);
            oglContext = 0;
        }*/
    }

    final void makeCurrent() {
        long oglContext = ctxmgr.getOGLContext(nwin.getId(), oshdc);
        ctxmgr.makeCurrent(oglContext, nwin.getId(), oshdc);
        OGLContextValidator.validateContext(this);
    }

    private int[] createVertexArray(MultiRectArea mra) {
        int rect[] = mra.rect;
        int resSize = (rect[0]-1) << 1;

        int[] res = null;

        if (resSize > 0) {
            //int resSize = nRects*8;
            int rectOffset = 1;
            res = new int[resSize];
            for (int i = 0; i < resSize; i += 8, rectOffset += 4) {
                // Left, top
                res[i] = rect[rectOffset];
                res[i+1] = rect[rectOffset+1];
                // Right, top
                res[i+2] = rect[rectOffset+2] + 1;
                res[i+3] = rect[rectOffset+1];
                // Right, bottom
                res[i+4] = rect[rectOffset+2] + 1;
                res[i+5] = rect[rectOffset+3] + 1;
                // Left, bottom
                res[i+6] = rect[rectOffset];
                res[i+7] = rect[rectOffset+3] + 1;
            }
        }

        return res;
    }

    @Override
    protected void fillMultiRectAreaColor(MultiRectArea mra) {
        makeCurrent();

        if (((mra.rect[0]-1)) > 0) { // Have something to draw
            int vertices[] = createVertexArray(mra);
            LockedArray lVertices = Utils.arraccess.lockArrayShort(vertices);

            gl.glVertexPointer(2, GLDefs.GL_INT, 0, lVertices.getAddress());

            // At least one configuration (ATI MOBILITY FIRE GL T2 card on win32) has
            // problems with the large arrays, so workaround is used when
            // the number of vertices exceeds 1024
            if (vertices.length < 2048) {
                gl.glDrawArrays(GLDefs.GL_QUADS, 0, vertices.length/2);
            } else {
                int iters = vertices.length / 2048;
                for (int i = 0; i < iters; i++) {
                    gl.glDrawArrays(GLDefs.GL_QUADS, i*1024, 1024);
                }
                gl.glDrawArrays(GLDefs.GL_QUADS, iters*1024, (vertices.length % 2048)/2);
            }

            lVertices.release();
        }

        gl.glFlush();
        getSurface().updateScene();
    }

    @Override
    public void drawString(String str, float x, float y) {
        makeCurrent();
        
        if (paint instanceof Color) {
            AffineTransform at = (AffineTransform) glTransform.clone();
            jtr.drawString(this, str, x, y);
            setTransform(at);
        } else {
            this.fill(font.createGlyphVector(this.getFontRenderContext(), str).getOutline(x, y));            
        }
        
        gl.glFlush();
        getSurface().updateScene();
    }

    @Override    
    public void drawGlyphVector(GlyphVector gv, float x, float y) {
        makeCurrent();
        
        if (paint instanceof Color) {
            AffineTransform at = (AffineTransform) glTransform.clone();
            jtr.drawGlyphVector(this, gv, x, y);
            setTransform(at);
        } else {
            this.fill(gv.getOutline(x, y));            
        }
        
        gl.glFlush();
        getSurface().updateScene();
     }


    @Override
    protected void setTransformedClip(MultiRectArea clip) {
        super.setTransformedClip(clip);
        //if (oglContext != 0) {
        if (ctxmgr != null) {
            makeCurrent();

            if(clip == null) { // Disable clip
                gl.glDisable(GLDefs.GL_STENCIL_TEST);
                gl.glDisable(GLDefs.GL_SCISSOR_TEST);

            } else if(clip.rect[0] < 5) { // Emplty clip, no drwing allowed
                gl.glDisable(GLDefs.GL_STENCIL_TEST);
                gl.glEnable(GLDefs.GL_SCISSOR_TEST);
                gl.glScissor(0, 0, 0, 0);

            } else if (clip.rect[0] == 5) { // Define scissor box - have only one clip rect
                gl.glDisable(GLDefs.GL_STENCIL_TEST);
                gl.glEnable(GLDefs.GL_SCISSOR_TEST);

                // Need to transform clip origin, since ogl transform
                // is not applied by glScissor
                // Probably should be (int) (glTransform.getTranslateX() + 0.5)
                // but this gives a 1 pixel error with swing scrolling
                int tx = (int) (glTransform.getTranslateX());
                int ty = (int) (glTransform.getTranslateY());

                gl.glScissor(
                        clip.rect[1] + tx,
                        winBounds.height - clip.rect[4] - 1 - ty,
                        clip.rect[3] - clip.rect[1] + 1,
                        clip.rect[4] - clip.rect[2] + 1
                );
            } else { // Several clip rects, use stencil

                gl.glDisable(GLDefs.GL_SCISSOR_TEST);
                gl.glEnable(GLDefs.GL_STENCIL_TEST);

                gl.glClear(GLDefs.GL_STENCIL_BUFFER_BIT);

                gl.glColorMask(
                        (byte) GLDefs.GL_FALSE,
                        (byte) GLDefs.GL_FALSE,
                        (byte) GLDefs.GL_FALSE,
                        (byte) GLDefs.GL_FALSE
                );
                gl.glStencilFunc(GLDefs.GL_ALWAYS, 0x1, 0x1);
                gl.glStencilOp(GLDefs.GL_REPLACE, GLDefs.GL_REPLACE, GLDefs.GL_REPLACE);

                // Draw the clip area into the stencil buffer
                fillMultiRectAreaColor(clip);

                gl.glColorMask(
                        (byte) GLDefs.GL_TRUE,
                        (byte) GLDefs.GL_TRUE,
                        (byte) GLDefs.GL_TRUE,
                        (byte) GLDefs.GL_TRUE
                );
                gl.glStencilFunc(GLDefs.GL_EQUAL, 0x1, 0x1);
                gl.glStencilOp(GLDefs.GL_KEEP, GLDefs.GL_KEEP, GLDefs.GL_KEEP);
            }
        }
    }

    @Override
    public void setTransform(AffineTransform transform) {
        // If transform is scaling drop native lines and use rasterizer
        if ((transform.getType() & AffineTransform.TYPE_MASK_SCALE) != 0) {
            scalingTransform = true;
        } else {
            scalingTransform = false;
        }

        //if (oglContext != 0) {
        if (ctxmgr != null) {
            double clipTranslationX =
                    - transform.getTranslateX() + glTransform.getTranslateX();
            double clipTranslationY =
                    - transform.getTranslateY() + glTransform.getTranslateY();

            this.glTransform = transform;

            if (clip != null) {
                clip.translate(
                        Math.round((float) clipTranslationX),
                        Math.round((float) clipTranslationY)
                );
            }

            makeCurrent();
            gl.glLoadIdentity();

            switch (transform.getType()) {
                case AffineTransform.TYPE_TRANSLATION: {
                    gl.glTranslated(transform.getTranslateX(), transform.getTranslateY(), 0);
                    break;
                }

                case AffineTransform.TYPE_GENERAL_SCALE:
                case AffineTransform.TYPE_UNIFORM_SCALE: {
                    gl.glScaled(transform.getScaleX(), transform.getScaleY(), 0);
                    break;
                }

                case AffineTransform.TYPE_IDENTITY:
                    break;

                default: {
                    transform.getMatrix(javaTransformMx);
                    Arrays.fill(glTransformMx, 0);
                    glTransformMx[0] = javaTransformMx[0];
                    glTransformMx[1] = javaTransformMx[1];
                    glTransformMx[4] = javaTransformMx[2];
                    glTransformMx[5] = javaTransformMx[3];
                    glTransformMx[12] = javaTransformMx[4];
                    glTransformMx[13] = javaTransformMx[5];
                    glTransformMx[10] = 1;
                    glTransformMx[15] = 1;

                    LockedArray lMx = Utils.arraccess.lockArrayShort(glTransformMx);
                    gl.glLoadMatrixd(lMx.getAddress());
                    lMx.release();
                }
            }

            // Fix line rasterization
            gl.glTranslated(0.375, 0.375, 0);

            // Update paint if it is TexturePaint or GradientPaint
            if (texPaint || gradTexName != 0) {
                resetPaint();
            }
        }
    }

    @Override
    public Shape getClip() {
        if (clip == null) {
            return null;
        }

        MultiRectArea res = new MultiRectArea(clip);
        return res;
    }

    @Override
    public Rectangle getClipBounds() {
        if (clip == null) {
            return null;
        }

        Rectangle res = (Rectangle) clip.getBounds().clone();
        return res;
    }

    @Override
    public AffineTransform getTransform() {
        return (AffineTransform) glTransform.clone();
    }

    @Override
    public void rotate(double theta) {
        glTransform.rotate(theta);
        setTransform(glTransform);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        glTransform.rotate(theta, x, y);
        setTransform(glTransform);
    }

    @Override
    public void scale(double sx, double sy) {
        glTransform.scale(sx, sy);
        setTransform(glTransform);
    }

    @Override
    public void shear(double shx, double shy) {
        glTransform.shear(shx, shy);
        setTransform(glTransform);
    }

    @Override
    public void transform(AffineTransform at) {
        AffineTransform newTransform = (AffineTransform) glTransform.clone();
        newTransform.concatenate(at);
        setTransform(newTransform);
    }

    @Override
    public void translate(double tx, double ty) {
        AffineTransform newTransform = (AffineTransform) glTransform.clone();
        newTransform.translate(tx, ty);
        setTransform(newTransform);
    }

    @Override
    public void translate(int tx, int ty) {
        AffineTransform newTransform = (AffineTransform) glTransform.clone();
        newTransform.translate(tx, ty);
        setTransform(newTransform);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        MultiRectArea mra = new MultiRectArea(x, y, x+width-1, y+height-1);

        if (clip == null) {
            setTransformedClip(mra);
        } else {
            clip.intersect(mra);
            setTransformedClip(clip);
        }
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (oglPaint) {
            makeCurrent();
            gl.glRectd(x, y, x + width, y + height);
            gl.glFlush();
        } else {
            super.fillRect(x, y, width, height);
        }

        getSurface().updateScene();
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (nativeLines && !scalingTransform && oglPaint) {
            makeCurrent();

            int vertices[] = new int[4];
            vertices[0] = x1;
            vertices[1] = y1;
            vertices[2] = x2;
            vertices[3] = y2;

            LockedArray lVertices = Utils.arraccess.lockArrayShort(vertices);
            gl.glVertexPointer(2, GLDefs.GL_INT, 0, lVertices.getAddress());
            gl.glDrawArrays(GLDefs.GL_LINES, 0, vertices.length/2);
            lVertices.release();

            gl.glFlush();
        } else {
            super.drawLine(x1, y1, x2, y2);
        }

        getSurface().updateScene();
    }

    private final void resetClip() {
        setTransformedClip(clip);
    }

    private final void resetColor() {
        setColor(fgColor);
    }

    private final void resetTransform() {
        setTransform(glTransform);
    }

    private final void resetBounds() {
        // Set viewport and projection
        // Always set the viewport to the whole window
        gl.glViewport(0, 0, winBounds.width, winBounds.height);

        gl.glMatrixMode(GLDefs.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.gluOrtho2D(0, winBounds.width, winBounds.height, 0);

        gl.glMatrixMode(GLDefs.GL_MODELVIEW);
    }

    private static class OGLContextValidator {
        private static final ThreadLocal<OGLGraphics2D> localCurrentGraphics = new ThreadLocal<OGLGraphics2D>();

        private static final void validateContext(OGLGraphics2D g) {
            OGLGraphics2D lastGraphics = localCurrentGraphics.get();

            if (lastGraphics == null) {
                // No graphics was used before in the current thread, so
                // opengl context of this thread should be initialized first

                // Single-buffered
                gl.glDrawBuffer(GLDefs.GL_FRONT);
                gl.glReadBuffer(GLDefs.GL_FRONT);

                gl.glEnableClientState(GLDefs.GL_VERTEX_ARRAY);
                gl.glDisable(GLDefs.GL_DITHER);

                localCurrentGraphics.set(g);

                if (g.blitter instanceof NullBlitter) {
                    // Called from constructor, skipping validation.
                    // Everything will be done in the constructor.
                    return;
                }
                // New context in the thread, but old graphics from other thread,
                // have to validate all.
                g.resetBounds();
                g.resetColor();
                g.resetTransform();
                g.resetClip();
                g.checkComposite();
                g.resetStroke();
                g.resetPaint();
                return;
            } else if (g == lastGraphics) { // Should have all the attributes in place
                return;
            }

            localCurrentGraphics.set(g);

            if (!g.winBounds.equals(lastGraphics.winBounds)) {
                g.resetBounds();
            }

            if (!g.fgColor.equals(lastGraphics.fgColor)) {
                g.resetColor();
            }

            boolean transformDiffers = !g.glTransform.equals(lastGraphics.glTransform);
            if (transformDiffers) {
                g.resetTransform();
            }

            if (g.clip == null) {
                if (lastGraphics.clip != null) {
                    g.resetClip();
                }
            } else if (!g.clip.equals(lastGraphics.clip) || transformDiffers) {
                g.resetClip();
            }

            if (!g.composite.equals(lastGraphics.composite)) {
                g.checkComposite();
            }

            if (!g.stroke.equals(lastGraphics.stroke)) {
                g.resetStroke();
            }

            if (!g.paint.equals(lastGraphics.paint)) {
                g.resetPaint();
            }
        }
    }

    @Override
    public void setComposite(Composite composite) {
        super.setComposite(composite);

        makeCurrent();
        checkComposite();
    }

    /**
     * NOTE - caller should call makeCurrent() before calling this method
     */
    final void checkComposite() {
        if (composite instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) composite;
            acAlpha = ac.getAlpha();

            if (
                    ac.getAlpha() == 1 && opaqueColor &&
                    (ac.getRule() == AlphaComposite.SRC_OVER ||
                     ac.getRule() == AlphaComposite.SRC)
            ) {
                gl.glDisable(GLDefs.GL_BLEND);
            } else {
                enableAlphaComposite(ac, true, true);
                //gl.glEnable(GLDefs.GL_BLEND);
            }

            if (ac.getAlpha() != 1) {
                setColor(bgColor);
            }
        } else {
            acAlpha = 1.0f;
        }
    }

    /**
     * NOTE - caller should call makeCurrent() before calling this method
     * @param ac
     * @param srcPremult
     * @param srcHasAlpha
     * @return true if caller should not premultiply the source, false otherwise
     */
    final static boolean enableAlphaComposite(AlphaComposite ac, boolean srcPremult, boolean srcHasAlpha) {
        float acAlpha = ac.getAlpha();
        int acRule = ac.getRule();

        int srcFactor;
        int dstFactor;
        int alphaFactor = BLEND_RULE_MAPPING_SRC_PREMULT[acRule];

        boolean needPremultiply = false;

        if (srcHasAlpha) {
            if (srcPremult || !HAVE_MAPPING_NO_PREMULT[acRule]) {
                srcFactor = BLEND_RULE_MAPPING_SRC_PREMULT[acRule];
                if (!srcPremult) { // Caller should premultiply the source
                    needPremultiply = true;
                }
            } else {
                srcFactor = BLEND_RULE_MAPPING_SRC_NO_PREMULT[acRule];
            }
            dstFactor = BLEND_RULE_MAPPING_DST[acRule];
        } else {
            srcFactor = BLEND_RULE_MAPPING_SRC_PREMULT[acRule];

            if (srcFactor == GLDefs.GL_ONE && acAlpha == 1) {
                gl.glDisable(GLDefs.GL_BLEND);
                return true;
            }

            dstFactor = BLEND_RULE_MAPPING_DST_NO_ALPHA[acRule];
        }

        gl.glEnable(GLDefs.GL_BLEND);

        if (srcFactor == alphaFactor) {
            gl.glBlendFunc(srcFactor, dstFactor);
        } else {
            gl.glBlendFuncSeparate(srcFactor, dstFactor, alphaFactor, dstFactor);
        }

        // Setup alpha scaling for the case when alpha in AlphaComposite != 1
        gl.glPixelTransferf(GLDefs.GL_ALPHA_SCALE, acAlpha);
        if (srcPremult || needPremultiply) {
            gl.glPixelTransferf(GLDefs.GL_RED_SCALE, acAlpha);
            gl.glPixelTransferf(GLDefs.GL_GREEN_SCALE, acAlpha);
            gl.glPixelTransferf(GLDefs.GL_BLUE_SCALE, acAlpha);
        } else {
            gl.glPixelTransferf(GLDefs.GL_RED_SCALE, 1);
            gl.glPixelTransferf(GLDefs.GL_GREEN_SCALE, 1);
            gl.glPixelTransferf(GLDefs.GL_BLUE_SCALE, 1);
        }

        return needPremultiply;
    }

    final OGLSurface getSurface() {
        return (OGLSurface) dstSurf;
    }

    void readPixels(int x, int y, int w, int h, Object buffer, boolean topToBottom) {
        // Save current graphics to restore current context after returning pixels
        OGLGraphics2D currGraphics = OGLContextValidator.localCurrentGraphics.get();
        makeCurrent();
/*
        x += glTransform.getTranslateX() + 0.5;
        y += glTransform.getTranslateY() + 0.5;
*/
        LockedArray lBuffer = Utils.arraccess.lockArrayShort(buffer);

        if (topToBottom) {
            // Need to read scanlines one-by-one to make them go from top to bottom.
            // OpenGL allows to read from bottom to top only.
            int sourceRow = winBounds.height-y-1;
            for (int i=0; i<h; i++, sourceRow--) {
                gl.glPixelStorei(GLDefs.GL_PACK_SKIP_ROWS, i);
                gl.glReadPixels(
                        x, sourceRow,
                        w, 1,
                        GLDefs.GL_BGRA, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV,
                        lBuffer.getAddress()
                );
            }
        } else {
            gl.glReadPixels(
                    x, winBounds.height-y-h,
                    w, h,
                    GLDefs.GL_BGRA, GLDefs.GL_UNSIGNED_INT_8_8_8_8_REV,
                    lBuffer.getAddress()
            );
        }

        lBuffer.release();

        gl.glPixelStorei(GLDefs.GL_PACK_SKIP_ROWS, 0);

        if (currGraphics != null && currGraphics!= this) {
            currGraphics.makeCurrent();
        }
    }

    @Override
    public void setStroke(Stroke stroke) {
        super.setStroke(stroke);
        if (stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) stroke;
            if (bs.getLineWidth() <= 1.) {
                if (bs.getDashArray() != null) {
                    if (setLineStipplePattern(bs.getDashArray(), bs.getDashPhase())) {
                        nativeLines = true;
                        resetStroke();
                        return;
                    }
                } else {
                    nativeLines = true;
                    stipplePattern = 0;
                    resetStroke();
                    return;
                }
            }
        }

        nativeLines = false;
        stipplePattern = 0;
    }

    /**
     * Calculates opengl line stipple parameters from dashArray and dashPhase taken from
     * BasicStroke.
     * @param dashArray - array, taken from BasicStroke
     * @param dashPhase - phase, taken from BasicStroke
     * @return fasle if it is impossible to use glLineStipple, true otherwise.
     */
    private boolean setLineStipplePattern(float dashArray[], float dashPhase) {
        // if there's odd number of elements in the dash array, we repeat it twice
        int longArrLength = dashArray.length % 2 == 0 ? dashArray.length : dashArray.length * 2;
        long longArray[] = new long[longArrLength];
        long longPhase = Math.round(dashPhase);
        BigInteger gcd = BigInteger.valueOf(Math.round(dashArray[0]));
        int sum = 0;
        for (int i = 0; i < longArrLength; i++) {
            longArray[i] = Math.round(dashArray[i % dashArray.length]);
            gcd = gcd.gcd(BigInteger.valueOf(longArray[i]));
            sum += longArray[i];
        }

        if (dashPhase != 0) {
            gcd = gcd.gcd(BigInteger.valueOf(longPhase));
            longPhase /= gcd.longValue();
        }

        sum /= gcd.longValue();

        if (sum > 16) {
            return false;
        }

        int repeatNum;

        switch (sum) {
            case 1:
                repeatNum = 16;
                break;
            case 2:
                repeatNum = 8;
                break;
            case 4:
                repeatNum = 4;
                break;
            case 8:
                repeatNum = 2;
                break;
            case 16:
                repeatNum = 1;
                break;
            default:
                return false;
        }

        int intPattern = 0;
        stippleFactor = (int) gcd.longValue();

        for (int i=0; i<repeatNum; i++) {
            for (int j = longArray.length-1; j >= 0; j--) {
                long currPatternLen = longArray[j]/stippleFactor;
                intPattern <<= currPatternLen;
                if ((j & 0x1) == 0) {
                    intPattern |= (1 << currPatternLen) - 1;
                }
            }
        }

        if (longPhase != 0) { // cyclic shift
            intPattern = (intPattern >>> longPhase) | (intPattern << (16-longPhase));
        }

        stipplePattern = (short) (intPattern & 0xFFFF);

        return true;
    }

    /**
     * Validates stroke.
     */
    void resetStroke() {
        if (nativeLines) {
            // Use opengl only for 1-width lines, don't need to set width
            if (stipplePattern == 0) {
                gl.glDisable(GLDefs.GL_LINE_STIPPLE);
            } else {
                gl.glEnable(GLDefs.GL_LINE_STIPPLE);
                gl.glLineStipple(stippleFactor, stipplePattern);
            }
        } else {
            gl.glDisable(GLDefs.GL_LINE_STIPPLE);
        }
    }

    @Override
    public void drawPolygon(Polygon polygon) {
        drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    @Override
    public void drawPolygon(int[] xpoints, int[] ypoints, int npoints) {
        drawPoly(xpoints, ypoints, npoints, true);
    }

    @Override
    public void drawPolyline(int[] xpoints, int[] ypoints, int npoints) {
        drawPoly(xpoints, ypoints, npoints, false);
    }

    private final void drawPoly(int[] xpoints, int[] ypoints, int npoints, boolean closed) {
        if (nativeLines && !scalingTransform && oglPaint) {
            makeCurrent();

            int vertices[] = new int[npoints<<1];
            for (int i = 0; i < npoints; i++) {
                vertices[i<<1] = xpoints[i];
                vertices[(i<<1) + 1] = ypoints[i];
            }

            LockedArray lVertices = Utils.arraccess.lockArrayShort(vertices);
            gl.glVertexPointer(2, GLDefs.GL_INT, 0, lVertices.getAddress());
            gl.glDrawArrays(
                    closed ? GLDefs.GL_LINE_LOOP : GLDefs.GL_LINE_STRIP,
                    0,
                    vertices.length / 2
            );
            lVertices.release();

            gl.glFlush();
        } else {
            if (closed) {
                super.drawPolygon(xpoints, ypoints, npoints);
            } else {
                super.drawPolyline(xpoints, ypoints, npoints);
            }
        }

        getSurface().updateScene();
    }

    @Override
    public void draw(Shape s) {
        // To get proper rasterization quality need to
        // perform scaling before rasterization
        if (scalingTransform) {
            s = stroke.createStrokedShape(s);
            s = glTransform.createTransformedShape(s);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            fillMultiRectArea(jsr.rasterize(s, 0.5));
            gl.glPopMatrix();
        } else {
            super.draw(s);
        }
    }

    void activateTexturePaint(TexturePaint p) {
        Rectangle2D r = p.getAnchorRect();
        /*
        if (r.getX() != 0 || r.getY() != 0) {
            gl.glPixelStoref(GLDefs.GL_UNPACK_SKIP_PIXELS, (float)r.getX());
            gl.glPixelStoref(GLDefs.GL_UNPACK_SKIP_ROWS, (float)r.getY());
        }
        */
        Surface srcSurf = Surface.getImageSurface(p.getImage());

        int width = (int) r.getWidth();
        int height = (int) r.getHeight();

        OGLBlitter oglBlitter = (OGLBlitter) blitter;

        OGLBlitter.OGLTextureParams tp = oglBlitter.blitImg2OGLTexCached(
                srcSurf,
                srcSurf.getWidth(), srcSurf.getHeight(),
                true
        );

        gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_WRAP_S, GLDefs.GL_REPEAT);
        gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_WRAP_T, GLDefs.GL_REPEAT);

        gl.glTexGeni(GLDefs.GL_S, GLDefs.GL_TEXTURE_GEN_MODE, GLDefs.GL_OBJECT_LINEAR);
        gl.glTexGeni(GLDefs.GL_T, GLDefs.GL_TEXTURE_GEN_MODE, GLDefs.GL_OBJECT_LINEAR);

        double sObjPlane[] = new double[4];
        double tObjPlane[] = new double[4];

        // If texture is power of two, take [0;1]x[0;1] square - this is the texture
        // coordinates range. Then create mapping of anchor rect onto it.
        // For NPOT texture take [0;size/p2size]x[0;size/p2size] square.
        double widthFactor = (double) width / tp.width * tp.p2w;
        double heightFactor = (double) height / tp.height * tp.p2h;

        sObjPlane[0] = 1. / widthFactor;
        sObjPlane[1] = 0;
        sObjPlane[2] = 0;
        sObjPlane[3] = -r.getX() / widthFactor;

        tObjPlane[0] = 0;
        tObjPlane[1] = 1. / heightFactor;
        tObjPlane[2] = 0;
        tObjPlane[3] = -r.getY() / heightFactor;

        LockedArray la = Utils.arraccess.lockArrayShort(sObjPlane);
        gl.glTexGendv(GLDefs.GL_S, GLDefs.GL_OBJECT_PLANE, la.getAddress());
        la.release();
        la = Utils.arraccess.lockArrayShort(tObjPlane);
        gl.glTexGendv(GLDefs.GL_T, GLDefs.GL_OBJECT_PLANE, la.getAddress());
        la.release();

        gl.glEnable(GLDefs.GL_TEXTURE_GEN_S);
        gl.glEnable(GLDefs.GL_TEXTURE_GEN_T);

        gl.glEnable(GLDefs.GL_TEXTURE_2D);
    }
    /*
    static final void reactivateTexturePaint() {
        gl.glEnable(GLDefs.GL_TEXTURE_GEN_S);
        gl.glEnable(GLDefs.GL_TEXTURE_GEN_T);
        gl.glEnable(GLDefs.GL_TEXTURE_2D);
    }
    */
    static final void deactivateTexturePaint() {
        gl.glDisable(GLDefs.GL_TEXTURE_GEN_S);
        gl.glDisable(GLDefs.GL_TEXTURE_GEN_T);
        gl.glDisable(GLDefs.GL_TEXTURE_2D);
    }

    @Override
    protected void fillMultiRectAreaPaint(MultiRectArea mra) {
        if (oglPaint) {
            fillMultiRectAreaColor(mra);
        } else {
            super.fillMultiRectAreaPaint(mra);
        }
    }

    private final void activateGradientPaint(GradientPaint gp) {
        byte twoPixels[] = new byte[8];
        int val1 = gp.getColor1().getRGB();
        int val2 = gp.getColor2().getRGB();
        twoPixels[0] = (byte) ((val1 >> 16) & 0xFF);
        twoPixels[1] = (byte) ((val1 >> 8) & 0xFF);
        twoPixels[2] = (byte) (val1 & 0xFF);
        twoPixels[3] = (byte) ((val1 >> 24) & 0xFF);
        twoPixels[4] = (byte) ((val2 >> 16) & 0xFF);
        twoPixels[5] = (byte) ((val2 >> 8) & 0xFF);
        twoPixels[6] = (byte) (val2 & 0xFF);
        twoPixels[7] = (byte) ((val2 >> 24) & 0xFF);

        // Get gradient endpoints in the device space
        Point2D p1 = gp.getPoint1();
        Point2D p2 = gp.getPoint2();
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        /**
         * Let us denote by a1, a2 and a3 object plane coefficients for texture s coordinate:
         *      s = a1*x + a2*y + a3
         * Want to create following mapping for the texture s coordinate:
         *  x1,y1 -> 0,25
         *  x2,y2 -> 0,75
         *  x3,y3 -> 0,25
         * where (x3-x1,y3-y1) is perpendicular to (x2-x1,y2-y1).
         * 0,25 and 0,75 are centers of the first and second pixels, where
         * the color should have the full intensity.
         * From this have 3 equations for a1, a2 and a3.
         * They are solved, using cramer's rule in the code below.
         */
        double x3 = x1+y2-y1;
        double y3 = y1+x1-x2;
        double d1 = (y3 - y1) * 0.5;
        double d2 = (x1 - x3) * 0.5;
        double d = -((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
        double a1 = d1/d;
        double a2 = d2/d;
        double a3 = 0.25 - a1*x1 - a2*y1;

        gradObjectPlane = new double[4];
        gradObjectPlane[0] = a1;
        gradObjectPlane[1] = a2;
        gradObjectPlane[2] = 0;
        gradObjectPlane[3] = a3;

        // Create 1D texture object
        Int32Pointer texPtr =
                NativeBridge.getInstance().createInt32Pointer(1, true);
        gl.glGenTextures(1, texPtr);
        gradTexName = texPtr.get(0);
        gl.glBindTexture(GLDefs.GL_TEXTURE_1D, gradTexName);
        texPtr.free();
        gl.glTexParameteri(GLDefs.GL_TEXTURE_1D, GLDefs.GL_TEXTURE_MAG_FILTER, GLDefs.GL_LINEAR);
        gl.glTexParameteri(GLDefs.GL_TEXTURE_1D, GLDefs.GL_TEXTURE_MIN_FILTER, GLDefs.GL_LINEAR);
        gl.glTexEnvf(GLDefs.GL_TEXTURE_ENV, GLDefs.GL_TEXTURE_ENV_MODE, GLDefs.GL_REPLACE);

        // Setup texture coordinates generation
        isGPCyclic = gp.isCyclic();
        gl.glTexParameteri(
                GLDefs.GL_TEXTURE_1D,
                GLDefs.GL_TEXTURE_WRAP_S,
                isGPCyclic ? GLDefs.GL_REPEAT : GLDefs.GL_CLAMP_TO_EDGE
        );
        gl.glTexGeni(GLDefs.GL_S, GLDefs.GL_TEXTURE_GEN_MODE, GLDefs.GL_OBJECT_LINEAR);
        LockedArray la = Utils.arraccess.lockArrayShort(gradObjectPlane);
        gl.glTexGendv(GLDefs.GL_S, GLDefs.GL_OBJECT_PLANE, la.getAddress());
        la.release();
        gl.glEnable(GLDefs.GL_TEXTURE_GEN_S);

        // Load data into texture
        la = Utils.arraccess.lockArrayShort(twoPixels);
        gl.glTexImage1D(
                GLDefs.GL_TEXTURE_1D,
                0,
                GLDefs.GL_RGBA,
                2,
                0,
                GLDefs.GL_RGBA,
                GLDefs.GL_UNSIGNED_BYTE,
                la.getAddress()
        );
        la.release();

        // Enable 1D texture
        gl.glEnable(GLDefs.GL_TEXTURE_1D);
    }

    private final void reactivateGradientPaint() {
        gl.glBindTexture(GLDefs.GL_TEXTURE_1D, gradTexName);
        LockedArray la = Utils.arraccess.lockArrayShort(gradObjectPlane);
        gl.glTexGendv(GLDefs.GL_S, GLDefs.GL_OBJECT_PLANE, la.getAddress());
        la.release();
        gl.glTexParameteri(
                GLDefs.GL_TEXTURE_1D,
                GLDefs.GL_TEXTURE_WRAP_S,
                isGPCyclic ? GLDefs.GL_REPEAT : GLDefs.GL_CLAMP_TO_EDGE
        );
        gl.glEnable(GLDefs.GL_TEXTURE_1D);
    }

    private final void deactivateGradientPaint() {
        gl.glDisable(GLDefs.GL_TEXTURE_1D);
        if (gradTexName != 0) {
            OGLBlitter.OGLTextureParams.deleteTexture(gradTexName);
            gradTexName = 0;
        }
    }

    /**
     * This method supposes that context is already current and validated.
     * It only changes current read drawable to the drawable of other
     * OGLGraphics2D object. It returnes false if contexts of this OGLGraphics2D
     * and OGLGraphics2D passed in read parameter differs. To provide normal operation
     * after using read drawable caller should restore state by calling makeCurrent().
     * @param read - OGLGraphics2D which provides read drawable
     * @return true on success
     */
    private final boolean setCurrentRead(OGLGraphics2D read) {
        long oglContext = ctxmgr.getOGLContext(nwin.getId(), oshdc);
        if (read.ctxmgr.getOGLContext(read.nwin.getId(), read.oshdc) != oglContext)
            return false;

        ctxmgr.makeContextCurrent(
                oglContext,
                nwin.getId(), read.nwin.getId(),
                oshdc, read.oshdc
        );
        return true;
    }

    boolean copyArea(int x, int y, int width, int height, int dx, int dy, OGLGraphics2D read, boolean texture) {
        if (!setCurrentRead(read)) {
            return false;
        }

        gl.glPixelStoref(GLDefs.GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStoref(GLDefs.GL_UNPACK_SKIP_ROWS, 0);

        gl.glPixelZoom(1, 1);

        // Raster position could be outside of the viewport, use glBitmap
        gl.glRasterPos2i(0, 0);
        gl.glBitmap(0, 0, 0, 0, dx, -dy-height, 0);

        x += read.glTransform.getTranslateX() + 0.5;
        y += read.glTransform.getTranslateY() + 0.5;

        if (!texture) {
            gl.glCopyPixels(
                    x, read.winBounds.height-y-height,
                    width, height,
                    GLDefs.GL_COLOR
            );
            gl.glFlush();

        } else {
            gl.glCopyTexSubImage2D(
                    GLDefs.GL_TEXTURE_2D, 0,
                    0, 0,
                    x, read.winBounds.height-y-height,
                    width, height
            );
        }

        getSurface().updateScene();

        makeCurrent();
        return true;
    }
}
