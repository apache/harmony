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
 * @author Pavel Dolgov
 */

#include <jni.h>

#include <windows.h>
#include <objidl.h>

#include "gl_GDIPlus.h"
#include "org_apache_harmony_awt_theme_windows_WinThemeGraphics.h"


HRGN setGdiClip(JNIEnv * env, HDC hdc, jintArray clip, jint clipLength) {

    HRGN hOldClipRgn = CreateRectRgn(0, 0, 1, 1);
    if (GetClipRgn(hdc, hOldClipRgn) != 1) {
        DeleteObject(hOldClipRgn);
        hOldClipRgn = NULL;
    }

    if (clipLength <= 0) {
        return hOldClipRgn;
    }

    jint * clipData = new jint[clipLength];
    env->GetIntArrayRegion(clip, 1, clipLength, clipData);
    
    HRGN hRgn = CreateRectRgn(clipData[0], clipData[1], clipData[2]+1, clipData[3]+1);
    SelectClipRgn(hdc, hRgn);
    DeleteObject(hRgn);
    
    for (int i = 4; i < clipLength; i += 4) {
        hRgn = CreateRectRgn(clipData[i], clipData[i+1], clipData[i+2]+1, clipData[i+3]+1);
        ExtSelectClipRgn(hdc, hRgn, RGN_OR);
        DeleteObject(hRgn);
    }
    
    delete [] clipData;

    return hOldClipRgn;
}

void restoreGdiClip(HDC hdc, HRGN hOldClipRgn) {
    SelectClipRgn(hdc, (HRGN)hOldClipRgn);
    if (hOldClipRgn != NULL) {
        DeleteObject((HRGN)hOldClipRgn);
    }
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_setGdiClip
        (JNIEnv * env, jclass clazz, jlong gip, jintArray clip, jint clipLength) {

    return (jlong)setGdiClip(env, ((GraphicsInfo *)gip)->hdc, clip, clipLength);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_restoreGdiClip
        (JNIEnv * env, jclass clazz, jlong gip, jlong hOldClipRgn) {

    GraphicsInfo *gi = (GraphicsInfo *)gip;
    restoreGdiClip(gi->hdc, (HRGN)hOldClipRgn);
}

static void (__stdcall *drawThemeBackground) (void*, void*, int, int, void*, void*)(NULL);
static BOOL isUxThemeAvailable(true);

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_drawXpBackground
        (JNIEnv * env, jclass clazz, jlong gip, jint x, jint y, jint w, jint h, 
        jlong hTheme, jint type, jint state) {

    if (!isUxThemeAvailable) {
        return;
    }

    if (drawThemeBackground == NULL) {
        HMODULE libUxTheme = LoadLibrary("UxTheme");
        isUxThemeAvailable = (libUxTheme != NULL);

        if (!isUxThemeAvailable) {
            return;
        }

        drawThemeBackground = (void (__stdcall *) (void*, void*, int, int, void*, void*)) GetProcAddress(libUxTheme, "DrawThemeBackground");
    }

    GraphicsInfo *gi = (GraphicsInfo *)gip;

    RECT bounds = { (int)x, (int)y, (int)x + (int)w, (int)y + (int)h };
    drawThemeBackground((void*) hTheme, (void*) gi->hdc, (int) type, (int) state, (void*) &bounds, (void*) NULL);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_drawClassicBackground
        (JNIEnv * env, jclass clazz, jlong gip, jint x, jint y, jint w, jint h, 
        jint type, jint state) {

    GraphicsInfo *gi = (GraphicsInfo *)gip;

    RECT bounds = { (int)x, (int)y, (int)x + (int)w, (int)y + (int)h };
    DrawFrameControl(gi->hdc, &bounds, type, state);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_fillBackground
        (JNIEnv * env, jclass clazz, jlong gip, jint x, jint y, jint w, jint h, 
        jint rgb, jboolean solid) {
    
    GraphicsInfo *gi = (GraphicsInfo *)gip;

    DWORD r = (rgb >> 16) & 0xFF;
    DWORD g = (rgb >> 8) & 0xFF;
    DWORD b = rgb & 0xFF;
    HBRUSH hBrush = CreateSolidBrush(RGB(r, g, b));

    RECT rc = { x, y, x + w, y + h };
    
    if (solid) {
        FillRect(gi->hdc, &rc, hBrush);
    } else {
        FrameRect(gi->hdc, &rc, hBrush);
        InflateRect(&rc, -1, -1);
        FrameRect(gi->hdc, &rc, hBrush);
    }
    DeleteObject(hBrush);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_drawFocusRect
        (JNIEnv * env, jclass clazz, jlong gip, jint x, jint y, jint w, jint h) {

    GraphicsInfo *gi = (GraphicsInfo *)gip;

    RECT bounds = { (int)x, (int)y, (int)x + (int)w, (int)y + (int)h };
    DrawFocusRect(gi->hdc, &bounds);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_drawEdge
        (JNIEnv * env, jclass clazz, jlong gip, jint x, jint y, jint w, jint h, jint type) {
        
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    RECT bounds = { (int)x, (int)y, (int)x + (int)w, (int)y + (int)h };
    DrawEdge(gi->hdc, &bounds, type, BF_RECT);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_theme_windows_WinThemeGraphics_fillHatchedSysColorRect
        (JNIEnv * env, jclass clazz, jlong gip, jint x, jint y, jint w, jint h, jint sysColor1, jint sysColor2) {
        
    GraphicsInfo *gi = (GraphicsInfo *)gip;
    
    RECT bounds = { (int)x, (int)y, (int)x + (int)w, (int)y + (int)h };
    
    WORD bits[2] = { 0x80, 0x40 };
    HBITMAP hBmp = CreateBitmap(2, 2, 1, 1, &bits);
    HBRUSH hBrush = CreatePatternBrush(hBmp);
    int oldBkMode = GetBkMode(gi->hdc);
    SetBkMode(gi->hdc, TRANSPARENT);
    SetBkColor(gi->hdc, GetSysColor(sysColor1));
    SetTextColor(gi->hdc, GetSysColor(sysColor2));
    FillRect(gi->hdc, &bounds, hBrush);
    SetBkMode(gi->hdc, oldBkMode);
    DeleteObject(hBrush);
    DeleteObject(hBmp);
}
