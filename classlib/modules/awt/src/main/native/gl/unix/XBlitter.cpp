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
#include <math.h>

#include "XSurfaceInfo.h"
#include "SurfaceDataStructure.h"

#include "org_apache_harmony_awt_gl_linux_XBlitter.h"
#include "org_apache_harmony_awt_gl_linux_XSurface.h"
#include "org_apache_harmony_awt_gl_linux_PixmapSurface.h"

#define ZERO 1E-10

int error_handler(Display *display, XErrorEvent *myerr){
    char msg[1024];
    XGetErrorText(display, myerr->error_code, msg, 1024);
    fprintf(stderr, "Error code: %d, Error message: %s\n", myerr->error_code, msg);
    return 0;
}

/*
 * Method: org.apache.harmony.awt.gl.linux.XSurface.createSurfData(JJJJII)J
 */
JNIEXPORT jlong JNICALL 
Java_org_apache_harmony_awt_gl_linux_XSurface_createSurfData
    (JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jlong visual_info, jint width, jint height)
{
    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)calloc(sizeof(SURFACE_STRUCTURE), 1);
    surf->display = (Display *)display;
    surf->drawable = (Drawable)drawable;
    surf->gc = (GC)gc;
    surf->visual_info = (XVisualInfo *)visual_info;
    surf->width = width;
    surf->height = height;
    return (jlong)surf;
}

/*
 * Method: org.apache.harmony.awt.gl.linux.XSurface.dispose(J)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_XSurface_dispose
    (JNIEnv *env, jobject obj, jlong surfPtr)
{
    if(surfPtr) free((void *)surfPtr);
}

/*
 * Method: org.apache.harmony.awt.gl.linux.PixmapSurface.createSurfData(JJJII)J
 */
JNIEXPORT jlong JNICALL 
Java_org_apache_harmony_awt_gl_linux_PixmapSurface_createSurfData
    (JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong visual_info, jint width, jint height)
{
    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)calloc(sizeof(SURFACE_STRUCTURE), 1);
    surf->display = (Display *)display;
    surf->drawable = (Drawable)drawable;
    surf->gc = XCreateGC(surf->display, surf->drawable, 0, NULL);
    surf->visual_info = (XVisualInfo *)visual_info;
    surf->width = width;
    surf->height = height;
    return (jlong)surf;
}

/*
 * Method: org.apache.harmony.awt.gl.linux.PixmapSurface.dispose(J)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_PixmapSurface_dispose
    (JNIEnv *env, jobject obj, jlong surfPtr)
{
    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)surfPtr;
    if(surf){
        if(surf->gc) XFreeGC(surf->display, surf->gc);
        if(surf->ximage) XDestroyImage(surf->ximage);
        
        free(surf);
    }
}

/*
 * Method: org.apache.harmony.awt.gl.linux.XBlitter.bltImage(IIJLjava/lang/Object;IIJIIZIIF[D[IIZ[II)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_XBlitter_bltImage
    (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
    jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, jboolean hasBackground, jint bgcolor,
    jint compType, jfloat alpha, jdoubleArray matrix, jintArray clip, jint numVertex, jboolean invalidated, 
    jintArray dirtyRegions, jint regCount)
{
    XSetErrorHandler(error_handler);

    jdouble *mtrx = NULL, *inv = NULL;

    if(matrix != 0){
        mtrx = (jdouble *)malloc(6 * sizeof(jdouble));
        env->GetDoubleArrayRegion(matrix, 0, 6, mtrx);

        jdouble det = mtrx[0] * mtrx[3] - mtrx[1] * mtrx[2];

        if(fabs(det) < ZERO){
            free(mtrx);
            return;
        }

        inv = (jdouble *)malloc(6 * sizeof(jdouble));
        inv[0] = mtrx[3] / det;
        inv[1] = -mtrx[1] / det;
        inv[2] = -mtrx[2] / det;
        inv[3] = mtrx[0] / det;
        inv[4] = (mtrx[2] * mtrx[5] - mtrx[3] * mtrx[4] ) / det;
        inv[5] = (mtrx[1] * mtrx[4] - mtrx[0] * mtrx[5] ) / det;
    }

    SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
    SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

    Display *display = dstSurf->display;
    Window root = XRootWindow(display, dstSurf->visual_info->screen);

    jint *verts = NULL;
    XRectangle *rects = NULL;
    int num_rects = 0;

    if(numVertex > 0){
        verts = (jint *)malloc(sizeof(jint)*numVertex);
        env->GetIntArrayRegion(clip, 1, numVertex, verts);

        num_rects = numVertex >> 2;
        rects = (XRectangle *)malloc(sizeof(XRectangle) * num_rects);

        for (int i = 0, j = 0; i < numVertex; i += 4, j++) {
            rects[j].x = (short)verts[i];
            rects[j].y = (short)verts[i + 1];
            rects[j].width = (unsigned short)(verts[i+2]-verts[i]+1);
            rects[j].height = (unsigned short)(verts[i+3]-verts[i+1]+1);
        }
        
        free(verts);
    }

    srcSurf->invalidated = invalidated != 0;

    if(srcSurf->invalidated){
        int *regions = NULL;
        if(dirtyRegions == 0){
            regCount = 4;
            regions = (int *)malloc(4 * sizeof(int));
            regions[0] = 0;
            regions[1] = 0;
            regions[2] = srcSurf->width - 1;
            regions[3] = srcSurf->height - 1;
        } else {
            regions = (int *)malloc(regCount * sizeof(int));
            env->GetIntArrayRegion(dirtyRegions, 1, regCount, regions);
        }

        for(int i = 0; i < regCount;){
            int x = regions[i++];
            int y = regions[i++];
            int w = regions[i++] - x + 1;
            int h = regions[i++] - y + 1;
            updateCache(srcSurf, env, srcData, true, x, y, w, h);
        }
       
        if(regions) free(regions);
    }

    int depth;

    if(srcSurf->has_alpha  || matrix) {
        depth = 32;
    } else {
        depth = 24;
    }

    XImage ximage;

    ximage.width = srcSurf->width;
    ximage.height = srcSurf->height;
    ximage.xoffset = 0;
    ximage.format = ZPixmap;
    ximage.data = (char *)srcSurf->bmpData;
    ximage.byte_order = LSBFirst;
    ximage.bitmap_unit = depth;
    ximage.bitmap_bit_order = MSBFirst;
    ximage.bitmap_pad = 32;
    ximage.depth = depth;
    ximage.bytes_per_line = srcSurf->bmp_byte_stride;
    ximage.bits_per_pixel = 32;
    ximage.red_mask = 0xff0000;
    ximage.green_mask = 0xff00;
    ximage.blue_mask = 0xff;

    if(dstSurf->visual_info->depth == depth && 
        depth == 24 && (((compType == 2 || compType == 5) && alpha != 0) ||
        ((compType == 3 || compType == 10) && alpha == 1.0))){

        XSetClipRectangles(display, dstSurf->gc, 0, 0, rects, num_rects, Unsorted);
        XPutImage(display, dstSurf->drawable, dstSurf->gc, &ximage, srcX, srcY, dstX, dstY, width, height);

        if(rects) free(rects);
     
        return;
    }
#ifdef _XRENDER_H_

    if(hasXRender){

        XRenderPictFormat *srcFormat;

        if(depth == 32) {
            srcFormat = XRenderFindStandardFormat(display, PictStandardARGB32);
        } else {
            srcFormat = XRenderFindStandardFormat(display, PictStandardRGB24);
        }
       
        Pixmap pixmap = XCreatePixmap(display, root, srcSurf->width, srcSurf->height, depth);

        GC gc = XCreateGC(display, pixmap, 0, 0);
        XPutImage(display, pixmap, gc, &ximage, 0, 0, 0, 0, srcSurf->width, srcSurf->height);
        XFreeGC(display, gc);

        Picture srcPict = XRenderCreatePicture(display, pixmap, srcFormat, 0, NULL);

        int op;

        switch(compType){
        case 1:
            op = PictOpClear;
            alpha = 1.0;
            break;
        case 2:
            op = PictOpSrc;
            if(alpha != 0) alpha = 1.0;
            break;
        case 3:
            op = PictOpOver;
            break;
        case 4:
            op = PictOpOverReverse;
            alpha = 1.0;
            break;
        case 5:
            op = PictOpIn;
            if(alpha != 0) alpha = 1.0;
            break;
        case 6:
            op = PictOpInReverse;
            if(alpha != 0) alpha = 1.0;
            break;
        case 7:
            op = PictOpOut;
            alpha = 1.0;
            break;
        case 8:
            op = PictOpOutReverse;
            if(alpha != 1.0) alpha = 0;
            break;
        case 9:
            op = PictOpDst;
            alpha = 1.0;
            break;
        case 10:
            op = PictOpAtop;
            break;
        case 11:
            op = PictOpAtopReverse;
            if(alpha != 0) alpha = 1.0;
            break;
        case 12:
            op = PictOpXor;
            if(alpha != 1.0) alpha = 0;
        }

        if(matrix != NULL){
            int _one = XDoubleToFixed( 1 );
            XTransform xform = {{
 	              { _one, 0, 0 },
              	{ 0, _one, 0 },
          	    { 0, 0, _one }
            }};

            xform.matrix[0][0] = XDoubleToFixed(inv[0]);
            xform.matrix[1][1] = XDoubleToFixed(inv[3]);
            xform.matrix[1][0] = XDoubleToFixed(inv[1]);
            xform.matrix[0][1] = XDoubleToFixed(inv[2]);

            xform.matrix[0][2] = (int)(mtrx[4] + 0.5);
            xform.matrix[1][2] = (int)(mtrx[5] + 0.5);

            free(mtrx);
            free(inv);

            XRenderSetPictureTransform( display, srcPict, &xform );
            XRenderSetPictureFilter(display, srcPict, FilterNearest, 0, 0);

            srcX -= xform.matrix[0][2];
            srcY -= xform.matrix[1][2];

        }

        XRenderPictFormat *dstFormat = XRenderFindVisualFormat (dstSurf->display, dstSurf->visual_info->visual);
        Picture dstPict = XRenderCreatePicture(dstSurf->display, dstSurf->drawable, dstFormat, 0, NULL);

        if(rects) XRenderSetPictureClipRectangles(display, dstPict, 0, 0, rects, num_rects);

        if(hasBackground){
            XRenderColor bgc;
            bgc.red = (unsigned short)((bgcolor & 0xff0000) >> 8);
            bgc.green = (unsigned short)(bgcolor & 0xff00);
            bgc.blue = (unsigned short)((bgcolor & 0xff) << 8);;
            bgc.alpha = (unsigned short)((bgcolor & 0xff000000) >> 16);

            Pixmap tmpPix = XCreatePixmap(display, root, srcSurf->width, srcSurf->height, depth);

            XRenderPictFormat *tmpFormat = XRenderFindStandardFormat(display, PictStandardARGB32);

            Picture tmpPict = XRenderCreatePicture(display, tmpPix, tmpFormat, 0, NULL);

            XRenderFillRectangle(display, PictOpSrc, tmpPict, &bgc, 0, 0, srcSurf->width, srcSurf->height);

            XRenderComposite(display, PictOpOver, srcPict, 0, tmpPict, 
                0, 0, 0, 0, 0, 0, srcSurf->width, srcSurf->height);

            XRenderFreePicture(display, srcPict);
            XFreePixmap(display, tmpPix);
            
            srcPict = tmpPict;
        }

        Picture alphaMask = 0;
        if(alpha != 1.0){
            XRenderColor c;
            c.red = c.green = c.blue = 0;
            c.alpha = (unsigned short)(((long)(alpha * 255 + 0.5)) << 8);
            Pixmap am = XCreatePixmap(display, root, width, height, 8);
            XRenderPictFormat *aformat = XRenderFindStandardFormat(display, PictStandardA8);
            alphaMask = XRenderCreatePicture(display, am, aformat, 0, NULL);
            XRenderFillRectangle(display, PictOpSrc, alphaMask, &c, 0, 0, width, height);
        }

        XRenderComposite(display, op, srcPict, alphaMask, dstPict, 
            srcX, srcY, 0, 0, dstX, dstY, width, height);

        if(alphaMask) XRenderFreePicture(display, alphaMask);
        XRenderFreePicture(display, srcPict);
        XRenderFreePicture(display, dstPict);

        if(pixmap) XFreePixmap(display, pixmap);
        if(rects) free(rects);

    } else {

#endif

#ifdef _XRENDER_H_

    }

#endif

}

/*
 * Method: org.apache.harmony.awt.gl.linux.XBlitter.bltPixmap(IIJIIJIIIF[D[II)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_XBlitter_bltPixmap
    (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, 
    jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, 
    jint compType, jfloat alpha, jdoubleArray matrix, jintArray clip, jint numVertex)
{
    XSetErrorHandler(error_handler);

    jdouble *mtrx = NULL, *inv = NULL;

    if(matrix != 0){
        mtrx = (jdouble *)malloc(6 * sizeof(jdouble));
        env->GetDoubleArrayRegion(matrix, 0, 6, mtrx);

        jdouble det = mtrx[0] * mtrx[3] - mtrx[1] * mtrx[2];

        if(fabs(det) < ZERO){
            free(mtrx);
            return;
        }

        inv = (jdouble *)malloc(6 * sizeof(jdouble));
        inv[0] = mtrx[3] / det;
        inv[1] = -mtrx[1] / det;
        inv[2] = -mtrx[2] / det;
        inv[3] = mtrx[0] / det;
        inv[4] = (mtrx[2] * mtrx[5] - mtrx[3] * mtrx[4] ) / det;
        inv[5] = (mtrx[1] * mtrx[4] - mtrx[0] * mtrx[5] ) / det;
    }

    SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
    SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

    Display *display = dstSurf->display;
    Window root = XRootWindow(display, dstSurf->visual_info->screen);

    jint *verts = NULL;
    XRectangle *rects = NULL;
    int num_rects = 0;

    if(numVertex > 0){
        verts = (jint *)malloc(sizeof(jint)*numVertex);
        env->GetIntArrayRegion(clip, 1, numVertex, verts);

        num_rects = numVertex >> 2;
        rects = (XRectangle *)malloc(sizeof(XRectangle) * num_rects);

        for (int i = 0, j = 0; i < numVertex; i += 4, j++) {
            rects[j].x = (short)verts[i];
            rects[j].y = (short)verts[i + 1];
            rects[j].width = (unsigned short)(verts[i+2]-verts[i]+1);
            rects[j].height = (unsigned short)(verts[i+3]-verts[i+1]+1);
        }

        free(verts);
    }

    if(!matrix && (((compType == 2 || compType == 5) && alpha != 0) ||
        ((compType == 3 || compType == 10) && alpha == 1.0))){

        XSetClipRectangles(display, dstSurf->gc, 0, 0, rects, num_rects, Unsorted);
        XCopyArea(display, srcSurf->drawable, dstSurf->drawable, dstSurf->gc, srcX, srcY, width, height, dstX, dstY);

        if(rects) free(rects);
        return;
    }

#ifdef _XRENDER_H_

    if(hasXRender){

        int op;

        switch(compType){
        case 1:
            op = PictOpClear;
            alpha = 1.0;
            break;
        case 2:
            op = PictOpSrc;
            if(alpha != 0) alpha = 1.0;
            break;
        case 3:
            op = PictOpOver;
            break;
        case 4:
            op = PictOpOverReverse;
            alpha = 1.0;
            break;
        case 5:
            op = PictOpIn;
            if(alpha != 0) alpha = 1.0;
            break;
        case 6:
            op = PictOpInReverse;
            if(alpha != 0) alpha = 1.0;
            break;
        case 7:
            op = PictOpOut;
            alpha = 1.0;
            break;
        case 8:
            op = PictOpOutReverse;
            if(alpha != 1.0) alpha = 0;
            break;
        case 9:
            op = PictOpDst;
            alpha = 1.0;
            break;
        case 10:
            op = PictOpAtop;
            break;
        case 11:
            op = PictOpAtopReverse;
            if(alpha != 0) alpha = 1.0;
            break;
        case 12:
            op = PictOpXor;
            if(alpha != 1.0) alpha = 0;
        }

        XRenderPictFormat *srcFormat = XRenderFindVisualFormat (srcSurf->display, srcSurf->visual_info->visual);
        Picture srcPict = XRenderCreatePicture(srcSurf->display, srcSurf->drawable, srcFormat, 0, NULL);

        XRenderPictFormat *dstFormat = XRenderFindVisualFormat (dstSurf->display, dstSurf->visual_info->visual);
        Picture dstPict = XRenderCreatePicture(dstSurf->display, dstSurf->drawable, dstFormat, 0, NULL);

        Pixmap pixmap = 0;

        if(matrix != NULL){
            int _one = XDoubleToFixed( 1 );
            XTransform xform = {{
 	              { _one, 0, 0 },
              	{ 0, _one, 0 },
          	    { 0, 0, _one }
            }};

            xform.matrix[0][0] = XDoubleToFixed(inv[0]);
            xform.matrix[1][1] = XDoubleToFixed(inv[3]);
            xform.matrix[1][0] = XDoubleToFixed(inv[1]);
            xform.matrix[0][1] = XDoubleToFixed(inv[2]);

            xform.matrix[0][2] = (int)(mtrx[4] + 0.5);
            xform.matrix[1][2] = (int)(mtrx[5] + 0.5);

            free(mtrx);
            free(inv);

            srcX -= xform.matrix[0][2];
            srcY -= xform.matrix[1][2];
       
            pixmap = XCreatePixmap(display, root, srcSurf->width, srcSurf->height, 32);

            XRenderPictFormat *format = XRenderFindStandardFormat(display, PictStandardARGB32);
            Picture picture = XRenderCreatePicture(display, pixmap, format, 0, NULL);

            XRenderComposite(display, PictOpSrc, srcPict, 0, picture, 
                0, 0, 0, 0, 0, 0, srcSurf->width, srcSurf->height);

            XRenderFreePicture(display, srcPict);
            srcPict = picture;

            XRenderSetPictureTransform( display, srcPict, &xform );
            XRenderSetPictureFilter(display, srcPict, FilterNearest, 0, 0);
        }

        if(rects) XRenderSetPictureClipRectangles(display, dstPict, 0, 0, rects, num_rects);

        Picture alphaMask = 0;

        if(alpha != 1.0){
            XRenderColor c;
            c.red = c.green = c.blue = 0;
            c.alpha = (unsigned short)(((long)(alpha * 255 + 0.5)) << 8);
            Pixmap am = XCreatePixmap(display, root, width, height, 8);
            XRenderPictFormat *aformat = XRenderFindStandardFormat(display, PictStandardA8);
            alphaMask = XRenderCreatePicture(display, am, aformat, 0, NULL);
            XRenderFillRectangle(display, PictOpSrc, alphaMask, &c, 0, 0, width, height);
        }

        XRenderComposite(display, op, srcPict, alphaMask, dstPict, 
            srcX, srcY, 0, 0, dstX, dstY, width, height);

        if(alphaMask) XRenderFreePicture(display, alphaMask);
        XRenderFreePicture(display, srcPict);
        XRenderFreePicture(display, dstPict);

        if(pixmap) XFreePixmap(display, pixmap);

        if(rects) free(rects);

    } else {

#endif

#ifdef _XRENDER_H_

    }

#endif

}

/*
 * Method: org.apache.harmony.awt.gl.linux.XBlitter.xorImage(IIJLjava/lang/Object;IIJIII[D[IIZ[II)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_XBlitter_xorImage
    (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
    jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, jint xorcolor, 
    jdoubleArray matrix, jintArray clip, jint numVertex, jboolean invalidated, 
    jintArray dirtyRegions, jint regCount)
{
}

/*
 * Method: org.apache.harmony.awt.gl.linux.XBlitter.xorPixmap(IIJIIJIII[D[II)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_XBlitter_xorPixmap
    (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, 
    jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, jint xorcolor, 
    jdoubleArray matrix, jintArray clip, jint numVertex)
{
}

