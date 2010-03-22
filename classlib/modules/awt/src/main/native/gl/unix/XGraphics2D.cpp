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
#include <stdlib.h>
#include <stdio.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xos.h>

#include "XSurfaceInfo.h"
#include "org_apache_harmony_awt_gl_linux_XGraphics2D.h"

JNIEXPORT jlong JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_createGC(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong valuemask, jlong values)
{
    return (jlong)XCreateGC((Display *)display, (Drawable)drawable, (unsigned long)valuemask, (XGCValues *)values);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_freeGC
(JNIEnv *env, jobject obj, jlong display, jlong gc)
{
    return (jint)XFreeGC((Display *)display, (GC)gc);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_setFunction
(JNIEnv *env, jobject obj, jlong display, jlong gc, jint function)
{
    return (jint)XSetFunction((Display *)display, (GC)gc, function);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_setForeground
(JNIEnv *env, jobject obj, jlong display, jlong gc, jlong colormap, jint argb_val)
{
    XColor *color = (XColor *)malloc(sizeof(XColor));
    color->red = (unsigned short) ((argb_val & 0x00FF0000) >> 8);
    color->green = (unsigned short) (argb_val & 0x0000FF00);
    color->blue = (unsigned short) ((argb_val & 0x000000FF) << 8);

    int ret = XAllocColor((Display *)display, (Colormap)colormap, color);

    if(!ret) return ret;

    ret = XSetForeground((Display *)display, (GC)gc, color->pixel);
    free(color);

    return ret;
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_copyArea
(JNIEnv *env, jobject obj, jlong display, jlong src, jlong dst, jlong gc, jint src_x, jint src_y, 
jint width, jint height, jint dst_x, jint dst_y)
{
    return XCopyArea((Display *)display, (Drawable)src, (Drawable)dst, (GC)gc, src_x, src_y, width, height, dst_x, dst_y);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_setClipMask
(JNIEnv *env, jobject obj, jlong display, jlong gc, jlong pixmap)
{
    return XSetClipMask((Display *)display, (GC)gc, (Pixmap)pixmap);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_setClipRectangles
(JNIEnv *env, jobject obj, jlong display, jlong gc, jint clip_x_origin, jint clip_y_origin, 
jintArray vertices, jint num_verts)
{
    jint *verts = NULL;
    XRectangle *rects = NULL;
    int num_rects = 0;

    if(num_verts > 0){
        verts = (jint *)malloc(sizeof(jint)*num_verts);
        env->GetIntArrayRegion(vertices, 1, num_verts, verts);

        num_rects = num_verts >> 2;
        rects = (XRectangle *)malloc(sizeof(XRectangle) * num_rects);

        for (int i = 0, j = 0; i < num_verts; i += 4, j++) {
            rects[j].x = (short)verts[i];
            rects[j].y = (short)verts[i + 1];
            rects[j].width = (unsigned short)(verts[i+2]-verts[i]+1);
            rects[j].height = (unsigned short)(verts[i+3]-verts[i+1]+1);
        }
    }

    int ret = XSetClipRectangles((Display *)display, (GC)gc, clip_x_origin, clip_y_origin, rects, num_rects, Unsorted);
    if(verts) free(verts);
    if(rects) free(rects);
    return ret;
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_drawArc
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jint x, jint y, jint width, jint height, jint startAngle, jint angle)
{
    return XDrawArc((Display *)display, (Drawable)drawable, (GC)gc, x ,y, width, height, startAngle, angle);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_drawLine
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jint x1, jint y1, jint x2, jint y2)
{
    return XDrawLine((Display *)display, (Drawable)drawable, (GC)gc, x1, y1, x2, y2);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_drawLines
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jshortArray points, jint num_points)
{
    jshort *verts = (jshort *)malloc(sizeof(jshort) * num_points);
    env->GetShortArrayRegion(points, 0, num_points, verts);

    int ret = XDrawLines((Display *)display, (Drawable)drawable, (GC)gc, (XPoint *)verts, num_points >> 1, CoordModeOrigin);
    if(verts) free(verts);
    return ret;
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_drawRectangle
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jint x, jint y, jint width, jint height)
{
    return XDrawRectangle((Display *)display, (Drawable)drawable, (GC)gc, x, y, width, height);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_fillRectangles
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jintArray vertices, jint num_verts)
{
    jint *verts = NULL;
    XRectangle *rects = NULL;
    int num_rects = 0;

    if(num_verts > 0){
        verts = (jint *)malloc(sizeof(jint)*num_verts);
        env->GetIntArrayRegion(vertices, 1, num_verts, verts);

        num_rects = num_verts >> 2;
        rects = (XRectangle *)malloc(sizeof(XRectangle) * num_rects);

        for (int i = 0, j = 0; i < num_verts; i += 4, j++) {
            rects[j].x = (short)verts[i];
            rects[j].y = (short)verts[i + 1];
            rects[j].width = (unsigned short)(verts[i+2]-verts[i]+1);
            rects[j].height = (unsigned short)(verts[i+3]-verts[i+1]+1);
        }

    }
    int ret = XFillRectangles((Display *)display, (Drawable)drawable, (GC)gc, rects, num_rects);
    if(verts) free(verts);
    if(rects) free(rects);
    return ret;
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_fillRectangle
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jint x, jint y, jint width, jint height)
{
    return XFillRectangle((Display *)display, (Drawable)drawable, (GC)gc, x, y, width, height);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_fillPolygon
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jshortArray points, jint num_points)
{
    jshort *verts = (jshort *)malloc(sizeof(jshort) * num_points);
    env->GetShortArrayRegion(points, 0, num_points, verts);

    int ret = XFillPolygon((Display *)display, (Drawable)drawable, (GC)gc, (XPoint *)verts, num_points >> 1, Nonconvex, CoordModeOrigin);
    if(verts) free(verts);
    return ret;
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_fillArc
(JNIEnv *env, jobject obj, jlong display, jlong drawable, jlong gc, jint x, jint y, jint width, jint height, jint startAngle, jint angle)
{
    return XFillArc((Display *)display, (Drawable)drawable, (GC)gc, x ,y, width, height, startAngle, angle);
}

JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_setStroke
(JNIEnv *env, jobject obj, jlong display, jlong gc, jint line_width, jint join_style, jint cap_style, 
jint dash_offset, jbyteArray dashes, jint len)
{
    jbyte *dash_list = NULL;
    XGCValues *values = (XGCValues *)malloc(sizeof(XGCValues));

    values->line_width = line_width;
    values->join_style = join_style;
    values->cap_style = cap_style;
    values->dash_offset = dash_offset;

    unsigned long mask = GCLineWidth | GCJoinStyle | GCCapStyle | GCDashOffset | GCLineStyle;
    if(!len){
        values->line_style = LineSolid;
    } else {
        dash_list = (jbyte *)malloc(len);
        env->GetByteArrayRegion(dashes, 0, len, dash_list);
        values->line_style = LineOnOffDash;
    }

    if(len == 1){
        values->dashes = *dash_list;
        mask |= GCDashList;
    }

    int ret = XChangeGC((Display *)display, (GC)gc, mask, values);

    if(!ret){
        if(dash_list) free(dash_list);
    }
    
    free(values);

    if(len > 1){
        ret = XSetDashes((Display *)display, (GC)gc, dash_offset, (char *)dash_list, len);
        free(dash_list);
    }

    return ret;
}

JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_gl_linux_XGraphics2D_flush
(JNIEnv *env, jobject obj, jlong display)
{
    XFlush((Display *)display);
}

