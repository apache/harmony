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
 * @author Vladimir A. Strigun
 */
#include <jni.h>

#include <stdio.h>

#include "gl_GDIPlus.h"
#include "java_awt_BasicStroke.h"
#include "java_awt_geom_PathIterator.h"
#include "org_apache_harmony_awt_gl_windows_WinGDIGraphics2D.h"

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
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    createGraphicsInfo
 * Signature: (JIIII)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_createGraphicsInfo
  (JNIEnv *env, jobject obj, jlong hwnd, jint x, jint y, jint width, jint height) {
    //fprintf(stderr, "createGraphicsInfo\n");
    GraphicsInfo *gi = (GraphicsInfo *) malloc(sizeof(GraphicsInfo));
    
    gi->hwnd = (HWND)hwnd;
    gi->hdc = GetDC(gi->hwnd);
    gi->hpen = NULL;
    gi->hbrush = NULL;
    //initialize default transform
    XFORM xform; xform.eM11=1; xform.eM12=0;  xform.eM21=0;  xform.eM22=1;  xform.eDx=0;  xform.eDy=0;
    gi->xform = xform;
    gi->gclip = CreateRectRgn(x,y,x+width,y+height);

    return (jlong)gi;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    createGraphicsInfoFor
 * Signature: (JC)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_createGraphicsInfoFor
  (JNIEnv * env, jobject obj, jlong hdc, jchar pageUnit) {
        GraphicsInfo * gi = (GraphicsInfo *) malloc(sizeof(GraphicsInfo));

    gi->hdc = (HDC) hdc;
    gi->hpen = NULL;
    gi->hbrush = NULL;
    //initialize default transform
    XFORM xform; xform.eM11=1; xform.eM12=0;  xform.eM21=0;  xform.eM22=1;  xform.eDx=0;  xform.eDy=0;
    gi->xform = xform;
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
    gi->hdc = CreateCompatibleDC(dc);
    if (gi->hdc == NULL) {
        // We are out of GDI resources
        runGC(env);
        gi->hdc = CreateCompatibleDC(dc);
        if (gi->hdc == NULL)
            throwRuntimeException(env);
    }
        
    
    // Creating bitmap and setting it to DC
    gi->bmp = CreateCompatibleBitmap(dc, width, height);
    if (gi->bmp == NULL) {
        // We are out of GDI resources
        runGC(env);
        gi->bmp = CreateCompatibleBitmap(dc, width, height);
        if (gi->bmp == NULL)
            throwRuntimeException(env);
    }
    SelectObject(gi->hdc, gi->bmp);
       ReleaseDC(NULL, dc);
    
    gi->hpen=NULL;
    gi->hbrush=NULL;
    gi->hrgn=NULL;

    XFORM xform; xform.eM11=1; xform.eM12=0;  xform.eM21=0;  xform.eM22=1;  xform.eDx=0;  xform.eDy=0;
    gi->xform = xform;
    gi->gclip = CreateRectRgn(0, 0, width, height);
    SelectClipRgn(gi->hdc, gi->gclip);

    return gi; 
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    createCompatibleImageInfo
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_createCompatibleImageInfo__JII
  (JNIEnv *env, jclass clazz, jlong hwnd, jint width, jint height) {
    HDC hdc = GetWindowDC((HWND)hwnd);
    
    GraphicsInfo *gi = createCompatibleImageInfo(env, hdc, width, height);

    ReleaseDC((HWND)hwnd, hdc);
    return (jlong)gi;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    createCompatibleImageInfo
 * Signature: ([BII)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_createCompatibleImageInfo___3BII
  (JNIEnv *env, jclass clazz, jbyteArray ida, jint width, jint height) {
    char *id = (char *)env->GetByteArrayElements(ida, NULL);
    HDC hdc = CreateDC(NULL, id, NULL, NULL);
    env->ReleaseByteArrayElements(ida, (jbyte *)id, JNI_ABORT);
    
    GraphicsInfo *gi = createCompatibleImageInfo(env, hdc, width, height);

    DeleteDC(hdc);
    return (jlong)gi;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    copyImageInfo
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_copyImageInfo
  (JNIEnv *env , jobject obj, jlong poriggi) {
    GraphicsInfo *gi = (GraphicsInfo *) malloc(sizeof(GraphicsInfo));
    GraphicsInfo *origgi = (GraphicsInfo *)poriggi;
    
    gi->hwnd = 0;
    gi->hdc = origgi->hdc;
    gi->bmp = 0;
    gi->hpen = origgi->hpen;
    gi->hbrush= origgi->hbrush;
    XFORM xform; xform.eM11=origgi->xform.eM11; xform.eM12=origgi->xform.eM12;  xform.eM21=origgi->xform.eM21;  xform.eM22=origgi->xform.eM22;  xform.eDx=origgi->xform.eDx;  xform.eDy=origgi->xform.eDy;
    gi->xform = xform;
    gi->hrgn = NULL;
    gi->gclip = origgi->gclip;

    return (jlong)gi;
}


/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    disposeGraphicsInfo
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_disposeGraphicsInfo
  (JNIEnv *env, jclass clazz, jlong pgi) {
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (gi == 0)
        return;

    if(gi->hpen)
        DeleteObject(gi->hpen);
    if(gi->hrgn){
        DeleteObject(gi->hrgn);
    }
    if(gi->gclip){
        DeleteObject(gi->gclip);
    }
    if(gi->hbrush)
        DeleteObject(gi->hbrush);

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
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    copyArea
 * Signature: (JIIIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_copyArea
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height, jint dx, jint dy) 
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    int x1, y1;
    x1 =(int)( x * gi->xform.eM11 + y * gi->xform.eM21 + gi->xform.eDx );
    y1 =(int)( x * gi->xform.eM12 + y * gi->xform.eM22 + gi->xform.eDy );
    BitBlt(gi->hdc, x1+dx, y1+dy, width, height, gi->hdc, x1, y1, SRCCOPY);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    setSolidBrush
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_setSolidBrush
  (JNIEnv *env, jobject obj, jlong pgi, jint r, jint g, jint b, jint a) {
    //fprintf(stderr, "setSolidBrush\n");
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (gi->hbrush)
        DeleteObject(gi->hbrush);
    gi->color=RGB(r,g,b);
    gi->hbrush = CreateSolidBrush(RGB(r, g, b));
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    setLinearGradientBrush
 * Signature: (JIIIIIIIIIIIIZ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_setLinearGradientBrush
  (JNIEnv *env, jobject obj, jlong pgi, jint x1, jint y1, jint r1, jint g1, jint b1, jint a1, jint x2, jint y2, jint r2, jint g2, jint b2, jint a2, jboolean cyclic) {
    //fprintf(stderr, "setLinearGradientBrush\n");
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    printf("linearGradientBrush is not implemented yet\n");
}  

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    fillRects
 * Signature: (J[I)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_fillRects
  (JNIEnv *env, jobject obj, jlong pgi, jintArray va, jint len) {
    //fprintf(stderr, "fillRects: %d\n", len);
    GraphicsInfo *gi = (GraphicsInfo *)pgi;
    
    if (len < 4)
        return;

    HDC hdc = gi->hdc;
    SelectClipRgn(hdc, gi->hrgn);
    jint *vertices = (jint *)malloc(sizeof(jint)*len);
    env->GetIntArrayRegion(va, 1, len, vertices);
    for (int i = 0; i < len; i += 4) {
        int x = vertices[i];
        int y = vertices[i+1];
        int x1 = vertices[i+2];
        int y1 = vertices[i+3];
        HRGN hrgn = CreateRectRgn(x,y,x1,y1);
        FillRgn(hdc, hrgn, gi->hbrush);
        DeleteObject(hrgn);
    }
    free(vertices);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    getDC
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_getDC
  (JNIEnv *env, jobject obj, jlong gip) {
    return (jlong)((GraphicsInfo *)gip)->hdc;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    setPen
 * Signature: (JFIIF[FIF)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_setPen
  (JNIEnv *env, jobject obj, jlong gip, jfloat lineWidth, jint endCap, jint lineJoin, jfloat miterLimit, jfloatArray jdash, jint dashLen, jfloat dashPhase)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    jboolean res = JNI_TRUE;
    if(gi->hpen)
        DeleteObject(gi->hpen);
    if(dashLen == 0 && lineJoin == java_awt_BasicStroke_JOIN_MITER && endCap == java_awt_BasicStroke_CAP_SQUARE) {
        gi->hpen = CreatePen(PS_SOLID, (int)lineWidth, gi->color);
        return res;
    }

    DWORD penStyle;
    penStyle = PS_GEOMETRIC;
    LOGBRUSH lb;
    if(dashLen == 0) {
        lb.lbStyle = BS_SOLID; 
    } else {
        lb.lbStyle = PS_SOLID; 
        penStyle |= PS_USERSTYLE;
    }

    switch (endCap) {
        case java_awt_BasicStroke_CAP_BUTT:
            penStyle |= PS_ENDCAP_FLAT;
            break;

        case java_awt_BasicStroke_CAP_SQUARE:
            penStyle |= PS_ENDCAP_SQUARE;
            break;

        case java_awt_BasicStroke_CAP_ROUND:
            penStyle |= PS_ENDCAP_ROUND;
            break;
    }

    switch (lineJoin) {
        case java_awt_BasicStroke_JOIN_BEVEL:
            penStyle |= PS_JOIN_BEVEL;
            break;

        case java_awt_BasicStroke_JOIN_MITER:
            penStyle |= PS_JOIN_MITER;
            break;

        case java_awt_BasicStroke_JOIN_ROUND:
            penStyle |= PS_JOIN_ROUND;
            break;
    }

    lb.lbColor = gi->color;

    if (dashLen == 0) {
        gi->hpen = ExtCreatePen(penStyle, (int)lineWidth, &lb, 0, NULL);
        return res;
    }
    float *dash = (float *)env->GetPrimitiveArrayCritical(jdash, 0);
    DWORD *dashes = new DWORD[dashLen];
    for(int i=0; i<dashLen; i++)
        dashes[i] = (DWORD)dash[i];
    gi->hpen = ExtCreatePen(penStyle, (int)lineWidth, &lb, dashLen, dashes);
    delete [] dashes;
    env->ReleasePrimitiveArrayCritical(jdash, dash, JNI_ABORT);

    return res;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    deletePen
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_deletePen
  (JNIEnv *env, jobject obj, jlong gip) 
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    if (gi->hpen)
        DeleteObject(gi->hpen);
    gi->hpen = NULL;
}


static inline void createGDIPath(JNIEnv *env, HDC dc, jfloatArray jpath, jint len, jint winding) 
{
    jfloat *path = (jfloat *)malloc(sizeof(jfloat)*len);
    env->GetFloatArrayRegion(jpath, 0, len, path);

    if(winding == java_awt_geom_PathIterator_WIND_EVEN_ODD)
        SetPolyFillMode(dc, ALTERNATE);
    else 
        SetPolyFillMode(dc, WINDING);

    BeginPath(dc);

    float x1 = 0;
    float y1 = 0;
    float mx = 0;
    float my = 0;
    for (int i = 0; i < len; i++) {
        int seg = (int)path[i];
        switch (seg) {
            case java_awt_geom_PathIterator_SEG_MOVETO:
                x1 = path[i+1];
                y1 = path[i+2];
                mx = path[i+1];
                my = path[i+2];
                MoveToEx(dc, (int)x1, (int)y1, NULL);
                i += 2;
                break;
            case java_awt_geom_PathIterator_SEG_LINETO:
                LineTo(dc, (int)x1, (int)y1);
                x1 = path[i+1];
                y1 = path[i+2];
                i += 2;
                break;
            case java_awt_geom_PathIterator_SEG_CLOSE:
                LineTo(dc, (int)x1, (int)y1);
                x1 = mx;
                y1 = my;
                CloseFigure(dc);
                break;
        }
    }
   
   EndPath(dc);
}    


/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    drawShape
 * Signature: (J[FI)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawShape
  (JNIEnv *env, jobject obj, jlong gip, jfloatArray jpath, jint len, jint winding)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;

    HDC hdc = gi->hdc;
    HGDIOBJ hPenOld = SelectObject(hdc, gi->hpen);
    HGDIOBJ hBrushOld = SelectObject(hdc, gi->hbrush);
    SelectClipRgn(hdc, gi->hrgn);
    BeginPath(hdc);
    createGDIPath(env, hdc, jpath, len, winding);
    StrokePath(hdc);
    SelectObject(hdc, hPenOld);
    SelectObject(hdc, hBrushOld);
    DeleteObject(hPenOld);
    DeleteObject(hBrushOld);
}


/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    fillShape
 * Signature: (J[FI)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_fillShape
  (JNIEnv *env, jobject obj, jlong gip, jfloatArray jpath, jint len, jint winding)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;

    HDC hdc = gi->hdc;
    HGDIOBJ hPenOld = SelectObject(hdc, gi->hpen);
    HGDIOBJ hBrushOld = SelectObject(hdc, gi->hbrush);
    SelectClipRgn(hdc, gi->hrgn);
    BeginPath(hdc);
    createGDIPath(env, hdc, jpath, len, winding);
    FillPath(hdc);
    SelectObject(hdc, hPenOld);
    SelectObject(hdc, hBrushOld);
    DeleteObject(hPenOld);
    DeleteObject(hBrushOld);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    drawLine
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine
  (JNIEnv *env, jobject obj, jlong gip, jint x1, jint y1, jint x2, jint y2)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    HDC dc = gi->hdc;
    HPEN oldPen = (HPEN)SelectObject(dc, gi->hpen);
    HBRUSH oldBrush = (HBRUSH)SelectObject(dc, gi->hbrush);
    XFORM currentTransform;
      
    SetGraphicsMode(dc, GM_ADVANCED);
    GetWorldTransform(dc, &currentTransform);
    SetWorldTransform(dc, &gi->xform);

    SelectClipRgn(dc, gi->hrgn);
    if (x1 == x2 && y1 == y2) {
        MoveToEx(dc, x1, y1, NULL);
        LineTo(dc, x1, y1);
    } else {
        MoveToEx(dc, x1, y1, NULL);
        LineTo(dc, x2, y2);
    }
    SetWorldTransform(dc, &currentTransform);
    SetGraphicsMode(dc, GM_COMPATIBLE);

    SelectObject(dc,oldPen);
    SelectObject(dc,oldBrush);
    DeleteObject(oldPen);
    DeleteObject(oldBrush);
}


/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    drawRect
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawRect
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height)
{
    if (width == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine(env, obj, gip, x, y, x, y+height);
        return;
    } else if (height == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine(env, obj, gip, x, y, x+width, y);
        return;
    }
   
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    HDC dc = gi->hdc;
    HPEN oldPen = (HPEN)SelectObject(dc, gi->hpen);
    HBRUSH oldBrush = (HBRUSH)SelectObject(dc, gi->hbrush);

    XFORM currentTransform;
    SetGraphicsMode(dc, GM_ADVANCED);
    GetWorldTransform(dc, &currentTransform);
    SetWorldTransform(dc, &gi->xform);

    SelectClipRgn(dc, gi->hrgn);
    BeginPath(dc);
    MoveToEx(dc, x, y, NULL);
    LineTo(dc, x+width, y);
    LineTo(dc, x+width, y+height);
    LineTo(dc, x, y+height);
    LineTo(dc, x, y);
    EndPath(dc);
    StrokePath(dc);

    SetWorldTransform(dc, &currentTransform);
    SetGraphicsMode(dc, GM_COMPATIBLE);

    SelectObject(dc,oldPen);
    SelectObject(dc,oldBrush);
    DeleteObject(oldPen);
    DeleteObject(oldBrush);
}



/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    drawOval
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawOval
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height)
{
    if (width == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine(env, obj, gip, x, y, x, y+height);
        return;
    } else if (height == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine(env, obj, gip, x, y, x+width, y);
        return;
    }

    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    HDC dc = gi->hdc;

    HPEN oldPen = (HPEN)SelectObject(dc, gi->hpen);
    HBRUSH  oldBrush = (HBRUSH)SelectObject(dc, gi->hbrush);

    XFORM currentTransform;
    SetGraphicsMode(dc, GM_ADVANCED);
    GetWorldTransform(dc, &currentTransform);
    SetWorldTransform(dc, &gi->xform);

    SelectClipRgn(dc, gi->hrgn);
    BeginPath(dc);
    Ellipse(dc, x, y, x+width, y+height);
    EndPath(dc);
    StrokePath(dc);

    SetWorldTransform(dc, &currentTransform);
    SetGraphicsMode(dc, GM_COMPATIBLE);

    SelectObject(dc,oldPen);
    SelectObject(dc,oldBrush);
    DeleteObject(oldPen);
    DeleteObject(oldBrush);
}




/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    fillRect
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_fillRect
  (JNIEnv *env, jobject obj, jlong gip, jint x, jint y, jint width, jint height)
{
    if (width == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine(env, obj, gip, x, y, x, y+height);
        return;
    } else if (height == 0) {
        Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_drawLine(env, obj, gip, x, y, x+width, y);
        return;
    }
    
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    HDC dc = gi->hdc;

    HPEN oldPen = (HPEN)SelectObject(dc, gi->hpen);
    HBRUSH oldBrush = (HBRUSH)SelectObject(dc, gi->hbrush);

    XFORM currentTransform;
    SetGraphicsMode(dc, GM_ADVANCED);
    GetWorldTransform(dc, &currentTransform);
    SetWorldTransform(dc, &gi->xform);

    SelectClipRgn(dc, gi->hrgn);
    BeginPath(dc);
    MoveToEx(dc, x, y, NULL);
    LineTo(dc, x+width, y);
    LineTo(dc, x+width, y+height);
    LineTo(dc, x, y+height);
    LineTo(dc, x, y);
    EndPath(dc);
    FillPath(dc);

    SetWorldTransform(dc, &currentTransform);
    SetGraphicsMode(dc, GM_COMPATIBLE);

    SelectObject(dc,oldPen);
    SelectObject(dc,oldBrush);
    DeleteObject(oldPen);
    DeleteObject(oldBrush);

}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    setClip
 * Signature: (J[II)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_setClip
  (JNIEnv *env, jobject obj, jlong gip, jintArray va, jint len)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;

    HDC hdc = gi->hdc;

    DeleteObject(gi->hrgn);
    HRGN hrgn = CreateRectRgn(-10,-10,0,0);
    if (len == 0) {
        SelectClipRgn(hdc, hrgn);
        gi->hrgn=hrgn;
        DeleteObject(hrgn);
        return;
    }
    if (len < 4) {
        SelectClipRgn(hdc, NULL);
        gi->hrgn = NULL;
        DeleteObject(hrgn);
        return;
    }


    jint *vertices = (jint *)malloc(sizeof(jint)*len);
    env->GetIntArrayRegion(va, 1, len, vertices);

    int x = vertices[0];
    int y = vertices[1];
    int x1 = vertices[2]+1;
    int y1 = vertices[3]+1;

    DeleteObject(hrgn);
    gi->hrgn = CreateRectRgn(x, y, x1, y1);

    SelectClipRgn(hdc, gi->hrgn);

    for (int i = 4; i < len; i += 4) {
        x = vertices[i];
        y = vertices[i+1];
        x1 = vertices[i+2]+1;
        y1 = vertices[i+3]+1;

        HRGN hrgn1 = CreateRectRgn(x, y, x1, y1);
        CombineRgn(gi->hrgn, gi->hrgn, hrgn1, RGN_OR);
        ExtSelectClipRgn(hdc, gi->hrgn, RGN_OR);
        DeleteObject(hrgn1);
    } 

    free(vertices);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    resetClip
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_resetClip
  (JNIEnv *env, jobject obj, jlong gip)
{
    if (gip == 0)
        return;
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    DeleteObject(gi->hrgn);
    gi->hrgn = NULL;
    SelectClipRgn(gi->hdc, gi->gclip);
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGDIGraphics2D
 * Method:    setNativeTransform
 * Signature: (J[D)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_WinGDIGraphics2D_setNativeTransform
  (JNIEnv *env, jobject obj, jlong gip, jdoubleArray jMatrix)
{
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    double *matrix = env->GetDoubleArrayElements(jMatrix, NULL);
    XFORM xForm;
    xForm.eM11 = (FLOAT) matrix[0];
    xForm.eM12 = (FLOAT) matrix[1];
    xForm.eM21 = (FLOAT) matrix[2];
    xForm.eM22 = (FLOAT) matrix[3];
    xForm.eDx  = (FLOAT) matrix[4];
    xForm.eDy  = (FLOAT) matrix[5];
    gi->xform = xForm;
    env->ReleaseDoubleArrayElements(jMatrix, matrix, JNI_ABORT);

}

