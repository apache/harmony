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

#include <windows.h>
#include "org_apache_harmony_awt_gl_windows_WinGraphicsEnvironment.h"
#include "gl_GDIPlus.h"

#define MAX_MONITOR_NUMBER 256

typedef struct _Monitors {
    JNIEnv *env;
    jobject monitors[MAX_MONITOR_NUMBER];
    int index;
    jclass deviceClass;
    jmethodID deviceInit;
} Monitors;

/*
 * This is a callback function for EnumDisplayMonitors.
 * It creates a new instance of WinGraphicsDevice class
 * and stores it in the array.
 */
BOOL CALLBACK MonitorDevices(HMONITOR hMonitor, HDC hdc, LPRECT rect, LPARAM data)
{
    MONITORINFOEX mi;
    mi.cbSize = sizeof(MONITORINFOEX);
    GetMonitorInfo(hMonitor, &mi);

    Monitors *m = (Monitors *)data;

    jstring id = m->env->NewStringUTF(mi.szDevice);

    m->monitors[m->index++] = m->env->NewObject(m->deviceClass, m->deviceInit,
        rect->left, rect->top, rect->right, rect->bottom, id, mi.dwFlags & MONITORINFOF_PRIMARY);

    return TRUE;
}

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGraphicsEnvironment
 * Method:    enumerateDisplayDevices
 * Signature: ()[Lorg/apache/harmony/awt/gl/windows/WinGraphicsDevice;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_awt_gl_windows_WinGraphicsEnvironment_enumerateDisplayDevices
  (JNIEnv *env, jobject obj) 
{
    Monitors monitors;
    monitors.env = env;
    monitors.index = 0;
    monitors.deviceClass = env->FindClass("org/apache/harmony/awt/gl/windows/WinGraphicsDevice");
    monitors.deviceInit = env->GetMethodID(monitors.deviceClass, "<init>", "(IIIILjava/lang/String;Z)V"); 
    
    if (EnumDisplayMonitors(NULL, NULL, &MonitorDevices, (LPARAM)&monitors) == 0)
        return 0;

    jobjectArray array = env->NewObjectArray(monitors.index, monitors.deviceClass, 0);
    
    for (int i = 0; i < monitors.index; i++)
        env->SetObjectArrayElement(array, i, monitors.monitors[i]); 
    
    return array;
}

#define PF_32RGB888    1
#define PF_16RGB565    2
#define PF_16RGB555    3
#define PF_24BGR888    4
#define PF_8BPP        5
#define PF_4BPP        6
#define PF_2BPP        7
#define PF_1BPP        8
#define PF_UNKNOWN     0

/*
 * Class:     org_apache_harmony_awt_gl_windows_WinGraphicsConfiguration
 * Method:    createColorModel
 * Signature: (J)Ljava/awt/ColorModel;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_awt_gl_windows_WinGraphicsConfiguration_createColorModel
  (JNIEnv *env, jobject obj, jlong hdc)
{
    jclass cmClass;
    jmethodID cmInit;
    jobject colorModel;

    GLBITMAPINFO bmpInfo;
    HBITMAP bmp;

    int pfType;
    int bits;

    DWORD *mask;

    HDC dc = (HDC)hdc;
    bmp = CreateCompatibleBitmap(dc, 1, 1);

    memset(&bmpInfo, 0, sizeof(GLBITMAPINFO));

    bmpInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    GetDIBits(dc, bmp, 0, 1, NULL, (BITMAPINFO *)&bmpInfo, DIB_RGB_COLORS);
    GetDIBits(dc, bmp, 0, 1, NULL, (BITMAPINFO *)&bmpInfo, DIB_RGB_COLORS);

    DeleteObject(bmp);

    bits = bmpInfo.bmiHeader.biBitCount;

    mask = (DWORD *)bmpInfo.bmiColors;
    if(bmpInfo.bmiHeader.biCompression == BI_BITFIELDS){

        if(mask[0] == 0x7c00 && mask[1] == 0x03e0 && mask[2] == 0x1f){

             pfType = PF_16RGB555;

        }else if(mask[0] == 0xf800 && mask[1] == 0x07e0 && mask[2] == 0x1f){

             pfType = PF_16RGB565;

        }else if(mask[0] == 0xff0000 && mask[1] == 0xff00 && mask[2] == 0xff){

             pfType = PF_32RGB888;

        }else{

             pfType = PF_UNKNOWN;

        }
    }else{
        switch(bits){
        
        case 1:
            pfType = PF_1BPP;
            break;
        case 2:
            pfType = PF_2BPP;
            break;
        case 4:
            pfType = PF_4BPP;
            break;
        case 8:
            pfType = PF_8BPP;
            break;
        case 16:
            pfType = PF_16RGB555;
            break;
        case 24:
            pfType = PF_24BGR888;
            break;
        case 32:
            pfType = PF_32RGB888;
            break;
        default:
            pfType = PF_UNKNOWN;
        }
    }

    switch(pfType){
    
    case PF_1BPP:
    case PF_2BPP:
    case PF_4BPP:
    case PF_8BPP:
        {
            int cmapSize = 1 << bits;
            int cmapByteSize = cmapSize << 2;
            jbyteArray cmap = env->NewByteArray(cmapByteSize);
            void *cmapPtr = env->GetPrimitiveArrayCritical(cmap, 0);
            memcpy(cmapPtr, bmpInfo.bmiColors, cmapByteSize);
            env->ReleasePrimitiveArrayCritical(cmap, cmapPtr, 0);
            cmClass = env->FindClass("java/awt/image/IndexColorModel");
            cmInit = env->GetMethodID(cmClass, "<init>", "(II[BIZ)V"); 
            colorModel = env->NewObject(cmClass, cmInit, bits, cmapSize, cmap, 0, 0);
        }
        break;

    case PF_16RGB555:
    case PF_16RGB565:
        cmClass = env->FindClass("java/awt/image/DirectColorModel");
        cmInit = env->GetMethodID(cmClass, "<init>", "(IIII)V"); 
        colorModel = env->NewObject(cmClass, cmInit, bits, mask[0], mask[1], mask[2]);
        break;

    case PF_32RGB888:
        cmClass = env->FindClass("java/awt/image/DirectColorModel");
        cmInit = env->GetMethodID(cmClass, "<init>", "(IIII)V"); 
        colorModel = env->NewObject(cmClass, cmInit, 24, mask[0], mask[1], mask[2]);
        break;

    case PF_24BGR888:
        {
            cmClass = env->FindClass("java/awt/image/ComponentColorModel");
            cmInit = env->GetMethodID(cmClass, "<init>", "(Ljava/awt/color/ColorSpace;ZZII)V");

            jclass csClass = env->FindClass("java/awt/color/ColorSpace");
            jmethodID csInit = env->GetStaticMethodID(csClass, "getInstance", "(I)Ljava/awt/color/ColorSpace");
            jfieldID csFiled = env->GetStaticFieldID(csClass, "CS_sRGB", "I");
            jint csType = env->GetStaticIntField(csClass, csFiled);

            jobject cs = env->CallStaticObjectMethod(csClass, csInit, csType);
            colorModel = env->NewObject(cmClass, cmInit, cs, 0, 0, 1, 0);
        }
        break;

    default:
        cmClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(cmClass, "Unknown Graphics Device Pixel Format");

    }
    return colorModel;
}
