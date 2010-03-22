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
 
#ifndef __SURFACE_STRUCTURE__
#define __SURFACE_STRUCTURE__

#include <stdio.h>
#include <jni.h>

#ifdef _WIN32
#include "gl_GDIPlus.h"
#endif

#if defined(unix) || defined(MACOSX)
#include "XSurfaceInfo.h"
#endif

// Color Space constants
#define sRGB_CS        1  
#define LINEAR_RGB_CS  2
#define LINEAR_GRAY_CS 3


// Color Model constants
#define DIRECT_CM      1
#define INDEX_CM       2
#define COMPONENT_CM   3

// Transparancy constants
#define GL_OPAQUE         1
#define GL_BITMASK        2
#define GL_TRANSLUCENT    3

// Data Type constants
#define TYPE_BYTE      0
#define TYPE_USHORT    1
#define TYPE_SHORT     2
#define TYPE_INT       3
#define TYPE_FLOAT     4
#define TYPE_DOUBLE    5

// Sample Model constants
#define SPPSM          1  // Single Pixel Packed Sample Model
#define MPPSM          2  // Multi Pixel Packed Sample Model
#define CSM            3  // Component Sample Model
#define PISM           4  // Pixel Interleaved Sample Model
#define BSM            5  // Banded Sample Model

// Surface Structure type constants
#define INT_RGB        1
#define INT_ARGB       2
#define INT_ARGB_PRE   3
#define INT_BGR        4
#define BYTE_BGR       5
#define BYTE_ABGR      6
#define BYTE_ABGR_PRE  7
#define USHORT_565     8
#define USHORT_555     9
#define BYTE_GRAY      10
#define USHORT_GRAY    11
#define BYTE_BINARY    12
#define BYTE_INDEXED   13
#define CUSTOM         0

// Surface Data Type constants
#define BYTE_DATA      0
#define USHORT_DATA    1
#define SHORT_DATA     2
#define INT_DATA       3
#define FLOAT_DATA     4
#define DOUBLE_DATA    5



typedef struct _SURFACE_STRUCTURE{
    int ss_type;         // Surface Structure type
    int cs_type;         // Color Space type
    int cm_type;         // Color Model type
    int sm_type;         // Sample Model type

    int data_type;       // Surface Data type
    int num_components;  // Number Color & Alpha components
    unsigned char has_alpha;
    unsigned char alpha_pre;
    int transparency;

    int width;
    int height;

    int pixel_stride;
    int scanline_stride;
    int scanline_stride_byte;

    int *bits;           // An array of the number of bits per color/alpha component

    int offset;          // Offset in the data elements from the beginig of data array

    // Direct Color Model
    int red_mask;
    int green_mask;
    int blue_mask;
    int alpha_mask;

    int red_sht;
    int green_sht;
    int blue_sht;
    int alpha_sht;

    int max_red;
    int max_green;
    int max_blue;
    int max_alpha;

    // Index Color Model
    int transparent_pixel;     // Index of the fully transparent pixel
    int colormap_size;
    int *colormap;
    unsigned char isGrayPallete;

    // Component Color Model
    int *bank_indexes;
    int *band_offsets;

       // Cached Data
    long bmp_byte_stride;
    void *bmpData;
    bool hasRealAlpha;
    bool invalidated;
    bool isAlphaPre;

#ifdef _WIN32
    // WinVolataileImage
    GraphicsInfo *gi;
    GLBITMAPINFO bmpInfo;

    HBITMAP bitmap;
    HDC srcDC;
    DWORD rtc;
    BOOL isTrueColor;
#endif

#if defined(unix) || defined(MACOSX)
    // XVolatileImage
    XImage *ximage;

    Display *display;
    Drawable drawable;
    GC gc;
    XVisualInfo *visual_info;
#endif


}SURFACE_STRUCTURE;

int parseMask(unsigned int, int *, int *);
int getShift(unsigned int);

extern inline void updateCache(SURFACE_STRUCTURE *, JNIEnv *, jobject, bool, int, int, int, int);

#endif
