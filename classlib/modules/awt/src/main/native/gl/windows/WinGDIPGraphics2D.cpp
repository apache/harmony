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
 * @author Alexey A. Petrenko
 */
#include <jni.h>

#include <stdio.h>

#include "gl_GDIPlus.h"
#include "java_awt_BasicStroke.h"
#include "java_awt_geom_PathIterator.h"
#include "org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D.h"

static jclass classSystem = NULL;
static jmethodID gcMethodID = NULL;

/*
 * This method is used when application runs out of GDI resources.
 * Garbage collector should destroy unneeded classes and release some GDI resources
 */
static inline int runGC(JNIEnv *env) {
    if (!classSystem || !gcMethodID) {
        classSystem = env->FindClass("java/lang/System");
        if (!classSystem)
            return 0;
        gcMethodID = env->GetStaticMethodID(classSystem, "gc", "()V");
        if (!gcMethodID)
            return 0;
    }
    
    env->CallStaticVoidMethod(classSystem, gcMethodID);
    return 1;
}

static jclass runtimeException = NULL;

/*
 * This methods throws RuntimeException with
 * "Out of GDI resources." message
 */
static inline void throwRuntimeException(JNIEnv *env) {
    if (!runtimeException) {
        runtimeException = env->FindClass("java/lang/RuntimeException");
    }
    
    env->ThrowNew(runtimeException, "Out of GDI resources.");
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    gdiPlusStartup
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_gdiPlusStartup
  (JNIEnv *env, jclass clazz) {
    GdiplusStartupInput input;
    ULONG_PTR gdipToken;
    GdiplusStartup(&gdipToken, &input, NULL);
    return (jlong)gdipToken;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    gdiPlusShutdown
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_gdiPlusShutdown
  (JNIEnv *env, jclass clazz, jlong token) {
  ULONG_PTR gdipToken = (ULONG_PTR)token;
  GdiplusShutdown(gdipToken);
}


/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    createGraphicsInfo
 * Signature: (JIIII)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_createGraphicsInfo
  (JNIEnv *env, jobject obj, jlong hwnd, jint x, jint y, jint width, jint height) {
    //fprintf(stderr, "createGraphicsInfo\n");
    GraphicsInfo *gi = (GraphicsInfo *) malloc(sizeof(GraphicsInfo));
    
    gi->hwnd = (HWND)hwnd;
    gi->hdc = GetDC(gi->hwnd);
    gi->graphics = new Graphics(gi->hdc);
    gi->pen = 0;
    gi->brush = 0;
    gi->bmp = 0;
    gi->matrix = new Matrix();

    gi->clip = new Region(Rect(x, y, width, height));
    gi->graphics->SetClip(gi->clip);

    return (jlong)gi;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    createGraphicsInfoFor
 * Signature: (JC)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_createGraphicsInfoFor
  (JNIEnv * env, jobject obj, jlong hdc, jchar pageUnit) {
        GraphicsInfo * gi = (GraphicsInfo *) calloc(1, sizeof(GraphicsInfo));

        gi->hdc = (HDC) hdc;
        gi->graphics = new Graphics(gi->hdc);
        gi->pen = 0;
        gi->brush = 0;
        gi->bmp = 0;
        gi->matrix = new Matrix();

        gi->clip = new Region();
        gi->graphics->SetClip(gi->clip);

        gi->graphics->SetPageUnit((Gdiplus::Unit) pageUnit);

        return (jlong)gi;
}

/*
 * Creates compatible GraphicsInfo structure for specified device context
 */
static inline GraphicsInfo *createCompatibleImageInfo(JNIEnv *env, HDC hdc, jint width, jint height) {
    GraphicsInfo *gi = (GraphicsInfo *) malloc(sizeof(GraphicsInfo));

       // To avoid issue of joint operation Windows NetMeeting and GL,
       // we create HDC and Bitmap for Volatile Image which will compatible 
       // HDC of the entire screen. It may leads to other issues on 
       // multimonitor systems in the future.
    
    gi->hwnd = 0;
       HDC dc = GetDC(NULL);
    //gi->hdc = CreateCompatibleDC(hdc);
    gi->hdc = CreateCompatibleDC(dc);
    if (gi->hdc == NULL) {
        // We are out of GDI resources
        runGC(env);
        //gi->hdc = CreateCompatibleDC(hdc);
        gi->hdc = CreateCompatibleDC(dc);
        if (gi->hdc == NULL)
            throwRuntimeException(env);
    }
        
    
    // Creating bitmap and setting it to DC
    //gi->bmp = CreateCompatibleBitmap(hdc, width, height);
    gi->bmp = CreateCompatibleBitmap(dc, width, height);
    if (gi->bmp == NULL) {
        // We are out of GDI resources
        runGC(env);
        //gi->bmp = CreateCompatibleBitmap(hdc, width, height);
        gi->bmp = CreateCompatibleBitmap(dc, width, height);
        if (gi->bmp == NULL)
            throwRuntimeException(env);
    }
    SelectObject(gi->hdc, gi->bmp);
       ReleaseDC(NULL, dc);
    
    gi->graphics = new Graphics(gi->hdc);
    gi->pen = 0;
    gi->brush = 0;   
    gi->matrix = 0;

    gi->clip = new Region(Rect(0, 0, width, height));
    gi->graphics->SetClip(gi->clip);
    
    return gi; 
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    createCompatibleImageInfo
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_createCompatibleImageInfo__JII
  (JNIEnv *env, jclass clazz, jlong hwnd, jint width, jint height) {
    HDC hdc = GetWindowDC((HWND)hwnd);
    
    GraphicsInfo *gi = createCompatibleImageInfo(env, hdc, width, height);

    ReleaseDC((HWND)hwnd, hdc);
    return (jlong)gi;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    createCompatibleImageInfo
 * Signature: ([BII)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_createCompatibleImageInfo___3BII
  (JNIEnv *env, jclass clazz, jbyteArray ida, jint width, jint height) {
    char *id = (char *)env->GetByteArrayElements(ida, NULL);
    HDC hdc = CreateDC(NULL, id, NULL, NULL);
    env->ReleaseByteArrayElements(ida, (jbyte *)id, JNI_ABORT);
    
    GraphicsInfo *gi = createCompatibleImageInfo(env, hdc, width, height);

    DeleteDC(hdc);
    return (jlong)gi;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    copyImageInfo
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_copyImageInfo
  (JNIEnv *env , jobject obj, jlong poriggi) {
    GraphicsInfo *gi = (GraphicsInfo *) malloc(sizeof(GraphicsInfo));
    GraphicsInfo *origgi = (GraphicsInfo *)poriggi;
    
    gi->hwnd = 0;
    gi->hdc = origgi->hdc;
    gi->bmp = 0;
    gi->graphics = new Graphics(gi->hdc);

    gi->clip = new Region();
    if (origgi->graphics != NULL) {
        origgi->graphics->GetClip(gi->clip);
    }
    
    gi->graphics->SetClip(gi->clip);
    gi->pen = (origgi->pen != NULL)?origgi->pen->Clone():0;
    gi->brush = (origgi->brush != NULL)?origgi->brush->Clone():0;
    gi->matrix = (origgi->matrix != NULL)?origgi->matrix->Clone():new Matrix();

    return (jlong)gi;
}


/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    disposeGraphicsInfo
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_disposeGraphicsInfo
  (JNIEnv *env, jclass clazz, jlong pgi) {
    //fprintf(stderr, "disposeGraphicsInfo\n");
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (gi == 0)
        return;
    
    if (gi->graphics)
        delete(gi->graphics);
    
    if (gi->pen) {
        delete(gi->pen);
    }
        
    if (gi->brush) {
        delete(gi->brush);
    }

    if (gi->matrix) {
        delete(gi->matrix);
    }

    if (gi->clip) {
        delete(gi->clip);
    }

    // If hwnd and bmp are 0 then we should not destroy HDC 
    // because it's a copy of VolatileImage
    if (gi->hwnd != 0 || gi->bmp != 0) {    
        if (gi->bmp)
            DeleteObject(gi->bmp);
        
        if (gi->hwnd)
            ReleaseDC(gi->hwnd, gi->hdc);
        else
            DeleteDC(gi->hdc);
    }
        
    free(gi);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    copyArea
 * Signature: (JIIIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_copyArea
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height, jint dx, jint dy) 
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    Point p(x, y);
    gi->matrix->TransformPoints(&p, 1);

    BitBlt(gi->hdc, p.X+dx, p.Y+dy, width, height, gi->hdc, p.X, p.Y, SRCCOPY);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    setSolidBrush
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_setSolidBrush
  (JNIEnv *env, jobject obj, jlong pgi, jint r, jint g, jint b, jint a) {
    //fprintf(stderr, "setSolidBrush\n");
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (gi->brush)
        delete(gi->brush);

    gi->brush = new SolidBrush(Color(a, r, g, b));
    
    if (gi->brush == 0)
        fprintf(stderr, "Created brush is null!\n");
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    setLinearGradientBrush
 * Signature: (JIIIIIIIIIIIIZ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_setLinearGradientBrush
  (JNIEnv *env, jobject obj, jlong pgi, jint x1, jint y1, jint r1, jint g1, jint b1, jint a1, jint x2, jint y2, jint r2, jint g2, jint b2, jint a2, jboolean cyclic) {
    //fprintf(stderr, "setLinearGradientBrush\n");
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (gi->brush)
        delete(gi->brush);

    gi->brush = new LinearGradientBrush(Point(x1, y1), Point(x2, y2), Color(a1, r1, g1, b1), Color(a2, r2, g2, b2));
    
    if (cyclic == JNI_TRUE)
        ((LinearGradientBrush *)gi->brush)->SetWrapMode(WrapModeTileFlipXY);
}  

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    fillRects
 * Signature: (J[I)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_fillRects
  (JNIEnv *env, jobject obj, jlong pgi, jintArray va, jint len) {
    //fprintf(stderr, "fillRects: %d\n", len);
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (len < 4)
        return;

    jint *vertices = (jint *)malloc(sizeof(jint)*len);
    env->GetIntArrayRegion(va, 1, len, vertices);
    
    for (int i = 0; i < len; i += 4) {
        int x = vertices[i];
        int y = vertices[i+1];
        int w = vertices[i+2]-vertices[i]+1;
        int h = vertices[i+3]-vertices[i+1]+1;
        
        gi->graphics->FillRectangle(gi->brush, x, y, w, h);
    }

    free(vertices);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    getDC
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_getDC
  (JNIEnv *env, jobject obj, jlong gip) {
    return (jlong)((GraphicsInfo *)gip)->hdc;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    setPen
 * Signature: (JFIIF[FIF)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_setPen
  (JNIEnv *env, jobject obj, jlong gip, jfloat lineWidth, jint endCap, jint lineJoin, jfloat miterLimit, jfloatArray jdash, jint dashLen, jfloat dashPhase)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    jboolean res = JNI_TRUE;
    if (gi->pen)
        delete gi->pen;
        
    gi->pen = new Pen(gi->brush, lineWidth);
    res &= gi->pen != NULL;

    switch (endCap) {
        case java_awt_BasicStroke_CAP_BUTT:
            res &= gi->pen->SetLineCap(LineCapFlat, LineCapFlat, DashCapFlat );
            break;

        case java_awt_BasicStroke_CAP_SQUARE:
            res &= gi->pen->SetLineCap(LineCapSquare, LineCapSquare, DashCapFlat);
            break;

        case java_awt_BasicStroke_CAP_ROUND:
            res &= gi->pen->SetLineCap(LineCapRound, LineCapRound, DashCapRound);
            break;
    }

    switch (lineJoin) {
        case java_awt_BasicStroke_JOIN_BEVEL:
            res &= gi->pen->SetLineJoin(LineJoinBevel);
            break;

        case java_awt_BasicStroke_JOIN_MITER:
            res &= gi->pen->SetLineJoin(LineJoinMiter);
            break;

        case java_awt_BasicStroke_JOIN_ROUND:
            res &= gi->pen->SetLineJoin(LineJoinRound);
            break;
    }

    res &= gi->pen->SetMiterLimit(miterLimit);

    if (dashLen == 0)
        return res;

    float *dash = (float *)env->GetPrimitiveArrayCritical(jdash, 0);
    // In GDI+ the length of each dash and space in the dash pattern
    // is the product of the element value in the array and the width
    // So we should divide all java values to the width
    if (dashLen > 1) {
        for (int i = 0; i < dashLen; i++)
            dash[i] /= lineWidth;
        res &= gi->pen->SetDashPattern(dash, dashLen);
    } else {
        REAL sdash[2] = {dash[0]/lineWidth, dash[0]/lineWidth};
        res &= gi->pen->SetDashPattern(sdash, 2);
    }
    env->ReleasePrimitiveArrayCritical(jdash, dash, JNI_ABORT);
    res &= gi->pen->SetDashOffset(dashPhase);

    return res;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    deletePen
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_deletePen
  (JNIEnv *env, jobject obj, jlong gip) 
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    if (gi->pen)
        delete gi->pen;
    gi->pen = NULL;
}

/*
 * Converts specified int array into GraphicsPath object
 */
static inline GraphicsPath *createGraphicsPath(JNIEnv *env, jfloatArray jpath, jint len, jint winding) 
{
    jfloat *path = (jfloat *)malloc(sizeof(jfloat)*len);
    env->GetFloatArrayRegion(jpath, 0, len, path);

    
    GraphicsPath *res = new GraphicsPath((winding == java_awt_geom_PathIterator_WIND_EVEN_ODD)?FillModeAlternate:FillModeWinding);
    float x1 = 0;
    float y1 = 0;
    float mx = 0;
    float my = 0;
    for (int i = 0; i < len; i++) {
        int seg = (int)path[i];
        switch (seg) {
            case java_awt_geom_PathIterator_SEG_MOVETO:
                res->StartFigure();
                x1 = path[i+1];
                y1 = path[i+2];
                mx = path[i+1];
                my = path[i+2];
                i += 2;
                break;
            case java_awt_geom_PathIterator_SEG_LINETO:
                res->AddLine(x1, y1, path[i+1], path[i+2]);
                x1 = path[i+1];
                y1 = path[i+2];
                i += 2;
                break;
            case java_awt_geom_PathIterator_SEG_CLOSE:
                res->AddLine(x1, y1, mx, my);
                x1 = mx;
                y1 = my;
                res->CloseFigure();
                break;
        }
    }
    
    free(path);
    
    return res;
}    

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    drawShape
 * Signature: (J[FI)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawShape
  (JNIEnv *env, jobject obj, jlong gip, jfloatArray jpath, jint len, jint winding)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    GraphicsPath *path = createGraphicsPath(env, jpath, len, winding);
    gi->graphics->DrawPath(gi->pen, path);

    delete path;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    fillShape
 * Signature: (J[FI)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_fillShape
  (JNIEnv *env, jobject obj, jlong gip, jfloatArray jpath, jint len, jint winding)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    GraphicsPath *path = createGraphicsPath(env, jpath, len, winding);
    gi->graphics->FillPath(gi->brush, path);

    delete path;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    drawLine
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine
  (JNIEnv *env, jobject obj, jlong gip, jint x1, jint y1, jint x2, jint y2)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    gi->graphics->SetTransform(gi->matrix);

    if (x1 == x2 && y1 == y2)
        gi->graphics->DrawLine(gi->pen, ((REAL)x1)-0.1f, (REAL)y1, ((REAL)x2)+0.1f, (REAL)y2);
    else
        gi->graphics->DrawLine(gi->pen, x1, y1, x2, y2);
    
    gi->graphics->ResetTransform();
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    drawRect
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawRect
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height)
{
    if (width == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine(env, obj, gip, x, y, x, y+height);
        return;
    } else if (height == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine(env, obj, gip, x, y, x+width, y);
        return;
    }
    
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    gi->graphics->SetTransform(gi->matrix);

    gi->graphics->DrawRectangle(gi->pen, x, y, width, height);

    gi->graphics->ResetTransform();
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    drawOval
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawOval
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height)
{
    if (width == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine(env, obj, gip, x, y, x, y+height);
        return;
    } else if (height == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine(env, obj, gip, x, y, x+width, y);
        return;
    }

    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    gi->graphics->SetTransform(gi->matrix);

    gi->graphics->DrawEllipse(gi->pen, x, y, width, height);

    gi->graphics->ResetTransform();
}
 
/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    fillRect
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_fillRect
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height)
{
    if (width == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine(env, obj, gip, x, y, x, y+height);
        return;
    } else if (height == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_drawLine(env, obj, gip, x, y, x+width, y);
        return;
    }
    
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    gi->graphics->SetTransform(gi->matrix);

    gi->graphics->FillRectangle(gi->brush, x, y, width, height);

    gi->graphics->ResetTransform();
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    setClip
 * Signature: (J[II)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_setClip
  (JNIEnv *env, jobject obj, jlong gip, jintArray va, jint len)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;

    if (len == 0) {
        gi->graphics->SetClip(Rect(-10, -10, 0, 0));
        return;
    }
    
    if (len < 4) {
        gi->graphics->ResetClip();
        return;
    }

    jint *vertices = (jint *)malloc(sizeof(jint)*len);
    env->GetIntArrayRegion(va, 1, len, vertices);

    int x = vertices[0];
    int y = vertices[1];
    int w = vertices[2]-vertices[0]+1;
    int h = vertices[3]-vertices[1]+1;
        
    Region clip(Rect(x, y, w, h));

    for (int i = 4; i < len; i += 4) {
        x = vertices[i];
        y = vertices[i+1];
        w = vertices[i+2]-vertices[i]+1;
        h = vertices[i+3]-vertices[i+1]+1;

        clip.Union(Rect(x, y, w, h));
    }
    gi->graphics->SetClip(&clip);

    free(vertices);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    resetClip
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_resetClip
  (JNIEnv *env, jobject obj, jlong gip)
{
    if (gip == 0)
        return;
        
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    gi->graphics->SetClip(gi->clip);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D
 * Method:    setNativeTransform
 * Signature: (J[D)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIPGraphics2D_setNativeTransform
  (JNIEnv *env, jobject obj, jlong gip, jdoubleArray jMatrix)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;

    double *matrix = env->GetDoubleArrayElements(jMatrix, NULL);
    gi->matrix->SetElements((REAL)matrix[0],(REAL)matrix[1],(REAL)matrix[2],(REAL)matrix[3],(REAL)matrix[4],(REAL)matrix[5]);
    env->ReleaseDoubleArrayElements(jMatrix, matrix, JNI_ABORT);

}

