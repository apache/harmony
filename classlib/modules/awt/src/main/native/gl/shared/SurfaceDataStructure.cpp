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
#include <stdlib.h>
#include <memory.h>

#include "SurfaceDataStructure.h"
#include "org_apache_harmony_awt_gl_ImageSurface.h"
#include "LUTTables.h"

#ifdef _WIN32

#include "GDIBlitter.h"

#endif


int parseMask(unsigned int mask, int *shift, int *maxVal){
    int bits = 0;
    *shift = 0;
    *maxVal = 0;
    if (mask != 0) {
        // Deleting final zeros
        while ((mask & 1) == 0) {
            mask >>= 1;
            (*shift)++;
        }
        *maxVal = mask;
        // Counting component bits
        while ((mask & 1) == 1) {
            mask >>= 1;
            bits++;
        }
    }
    return bits;
}


int getShift(unsigned int mask){
    int shift = 0;
    if (mask != 0) {
        while ((mask & 1) == 0) {
            mask >>= 1;
            shift++;
        }
    }
    return shift;
}

inline void updateCache
(SURFACE_STRUCTURE *srcSurf, JNIEnv *env, jobject srcData, bool alphaPre, int x, int y, int width, int height){

    int src_stride, dst_stride, src_offset, dst_offset;
    int h = height;
    int w = width;

    void *bmpDataPtr = srcSurf->bmpData;
    void *srcDataPtr = env->GetPrimitiveArrayCritical((jarray)srcData, 0);

    switch(srcSurf->ss_type){

        case INT_RGB:
            {
                unsigned int *src, *dst;

#ifdef unix
                unsigned int *s, *d;
#endif

                src_stride = srcSurf->scanline_stride;
                dst_stride = srcSurf->width;

                src_offset = y * src_stride + x;
                dst_offset = y * dst_stride + x;
                src = (unsigned int *)srcDataPtr + src_offset;
                dst = (unsigned int *)bmpDataPtr + dst_offset;

                for(int _y = 0; _y < h; _y++, src += src_stride, dst += dst_stride){
#ifdef _WIN32
                    memcpy(dst, src, w * sizeof(int));
#endif

#ifdef unix
                    s = src, d = dst;
                    for(int _x = 0; _x < w; _x++, s++, d++){
                        *d = 0xff000000 | *s;
                    }
#endif
                }
            }
            break;

        case INT_ARGB:
            {
                if(alphaPre){
                    unsigned char *src, *s, *dst, *d, sa;

                    src_stride = srcSurf->scanline_stride_byte;
                    dst_stride = srcSurf->width << 2;

                    src_offset = y * src_stride + ((x + w) << 2) - 1;
                    dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                    src = (unsigned char *)srcDataPtr + src_offset;
                    dst = (unsigned char *)bmpDataPtr + dst_offset;

                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            sa = *s--;
                            *d-- = sa;
                            if(sa != 255){
                                *d-- = MUL(sa, *s--);
                                *d-- = MUL(sa, *s--);
                                *d-- = MUL(sa, *s--);
                                srcSurf->hasRealAlpha = true;
                            }else{
                                *d-- = *s--;
                                *d-- = *s--;
                                *d-- = *s--;
                            }
                        }
                    }

                    srcSurf->isAlphaPre = true;
                }else{
                    unsigned int *src, *dst;

                    src_stride = srcSurf->scanline_stride;
                    dst_stride = srcSurf->width;

                    src_offset = y * src_stride + x;
                    dst_offset = y * dst_stride + x;
                    src = (unsigned int *)srcDataPtr + src_offset;
                    dst = (unsigned int *)bmpDataPtr + dst_offset;

                    for(int _y = 0; _y < h; _y++, src += src_stride, dst += dst_stride){
                        memcpy(dst, src, w * sizeof(int));
                    }

                    srcSurf->isAlphaPre = false;
                }
            }
            break;

        case INT_ARGB_PRE:
            {
                unsigned char *src, *s, *dst, *d, sa;

                src_stride = srcSurf->scanline_stride_byte;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + ((x + w) << 2) - 1;
                dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                if(alphaPre){
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            sa = *s--;
                            *d-- = sa;
                            *d-- = *s--;
                            *d-- = *s--;
                            *d-- = *s--;
                            if(sa != 255){
                                srcSurf->hasRealAlpha = true;
                            }
                        }
                    }
                    srcSurf->isAlphaPre = true;
                }else{
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            sa = *s--;
                            *d-- = sa;
                            *d-- = DIV(sa, *s--);
                            *d-- = DIV(sa, *s--);
                            *d-- = DIV(sa, *s--);
                        }
                    }
                    srcSurf->isAlphaPre = false;
                }
            }
            break;

        case INT_BGR:
            {
                unsigned char *src, *s, *dst, *d;

                src_stride = srcSurf->scanline_stride_byte;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + ((x + w) << 2) - 1;
                dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                    s = src;
                    d = dst;

                    for(int _x = w; _x > 0; _x--){
                        *d = 255;
                        s--;
                        *(d - 3) = *s--;
                        *(d - 2) = *s--;
                        *(d - 1) = *s--;
                        d -= 4;
                    }
                }
            }
            break;

        case BYTE_BGR:
            {
                unsigned char *src, *s, *dst, *d;

                src_stride = srcSurf->scanline_stride_byte;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + (x + w) * 3 - 1;
                dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                    s = src;
                    d = dst;

                    for(int _x = w; _x > 0; _x--){
                        *d-- = 255;
                        *d-- = *s--;
                        *d-- = *s--;
                        *d-- = *s--;
                    }
                }
            }
            break;

        case BYTE_ABGR:
            {
                unsigned char *src, *s, *dst, *d, a, r, g, b;

                src_stride = srcSurf->scanline_stride_byte;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + ((x + w) << 2) - 1;
                dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                if(alphaPre){
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            r = *s--;
                            g = *s--;
                            b = *s--;
                            a = *s--;
                            *d-- = a;
                            if(a != 255){
                                *d-- = MUL(a, r);
                                *d-- = MUL(a, g);
                                *d-- = MUL(a, b);
                                srcSurf->hasRealAlpha = true;
                            }else{
                                *d-- = r;
                                *d-- = g;
                                *d-- = b;
                            }
                        }
                    }
                    srcSurf->isAlphaPre = true;
                }else{
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            r = *s--;
                            g = *s--;
                            b = *s--;
                            a = *s--;
                            *d-- = a;
                            *d-- = r;
                            *d-- = g;
                            *d-- = b;
                        }
                    }
                    srcSurf->isAlphaPre = false;
                }
            }
            break;

        case BYTE_ABGR_PRE:
            {
                unsigned char *src, *s, *dst, *d, a, r, g, b;

                src_stride = srcSurf->scanline_stride_byte;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + ((x + w) << 2) - 1;
                dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                if(alphaPre){
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;
 
                        for(int _x =  w; _x > 0; _x--){
                            r = *s--;
                            g = *s--;
                            b = *s--;
                            a = *s--;
                            if(a != 255){
                                srcSurf->hasRealAlpha = true;
                            }
                            *d-- = a;
                            *d-- = r;
                            *d-- = g;
                            *d-- = b;
                        }
                    }
                    srcSurf->isAlphaPre = true;
                }else{
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;
  
                        for(int _x = w; _x > 0; _x--){
                            r = *s--;
                            g = *s--;
                            b = *s--;
                            a = *s--;
                            *d-- = a;
                            if(a != 255){
                                *d-- = DIV(a, r);
                                *d-- = DIV(a, g);
                                *d-- = DIV(a, b);
                            }else{
                                *d-- = r;
                                *d-- = g;
                                *d-- = b;
                            }
                        }
                    }
                    srcSurf->isAlphaPre = false;
                }
            }
            break;

        case USHORT_555:
        case USHORT_565:
            {
                unsigned char *dst, *d;
                unsigned short *src, *s, pixel;

                unsigned int mr = srcSurf->max_red;
                unsigned int mg = srcSurf->max_green;
                unsigned int mb = srcSurf->max_red;
                unsigned int rm = srcSurf->red_mask;
                unsigned int gm = srcSurf->green_mask;
                unsigned int bm = srcSurf->blue_mask;
                unsigned int rs = srcSurf->red_sht;
                unsigned int gs = srcSurf->green_sht;
                unsigned int bs = srcSurf->blue_sht;

                src_stride = srcSurf->scanline_stride;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + x + w - 1;
                dst_offset = y * dst_stride + ((x + w) << 2) - 1;
                src = (unsigned short *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                    d = dst;
                    s = src;
                    for(int _x = w; _x > 0; _x--){
                        pixel = *s--;
                        *d-- = 255;
                        *d-- = DIV(mb, ((pixel & rm) >> rs));
                        *d-- = DIV(mg, ((pixel & gm) >> gs));
                        *d-- = DIV(mr, ((pixel & bm) >> bs));
                    }
                }
            }
            break;

        case USHORT_GRAY:
            {
                unsigned char *dst, *d, pixel;
                unsigned short *src, *s;

                src_stride = srcSurf->scanline_stride;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + (x << 1);
                dst_offset = y * dst_stride + (x << 2);
                src = (unsigned short *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                    s = src;
                    d = dst;
                    for(int _x =  w; _x > 0; _x--){
                        pixel = (unsigned char)(*s++ / 257);
                        *d++ = pixel;
                        *d++ = pixel;
                        *d++ = pixel;
                        *d++ = 255;
                    }
                }
            }
            break;

        case BYTE_BINARY:
            {
                unsigned char *src, *s;
                unsigned int *dst, *d, pixel, bitnum, elem, shift, bitMask;

                unsigned int pixelBits = srcSurf->pixel_stride;
                int *cm = srcSurf->colormap;

                src_stride = srcSurf->scanline_stride;
                dst_stride = srcSurf->width;

                src_offset = y * src_stride;
                dst_offset = y * dst_stride + x;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned int *)bmpDataPtr + dst_offset;

                for(int y = h; y > 0; y--, src += src_stride, dst += dst_stride){
                    d = dst;

                    for(int x = 0; x < w; x++){
                        bitnum = x * pixelBits;
                        s = src + bitnum / 8;
                        elem = *s;
                        shift = 8 - (bitnum & 7) - pixelBits;
                        bitMask = (1 << pixelBits) - 1;
                        pixel = (elem >> shift) & bitMask;
                        *d++ = 0xff000000 | *(cm + pixel);
                    }
                }
            }
            break;

        case BYTE_INDEXED:
            {
                int transparency = srcSurf->transparency;
                unsigned char *src, *s;
                unsigned int *dst, *d, pixel, r, g, b, a;
                int *cm = srcSurf->colormap;
                int tp = srcSurf->transparent_pixel;

                src_stride = srcSurf->scanline_stride;
                dst_stride = srcSurf->width;

                src_offset = y * src_stride + x + w - 1;
                dst_offset = y * dst_stride + x + w - 1;
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned int *)bmpDataPtr + dst_offset;


                if(transparency == GL_OPAQUE){
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            *d-- = 0xff000000 | *(cm + *s--);
                        }
                    }
                }else if(transparency == GL_BITMASK){
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            pixel = *s--;
                            if(pixel != tp){
                                *d-- = 0xff000000 | *(cm + pixel);
                            }else{
                                srcSurf->hasRealAlpha = true;
                                *d-- = 0;
                            }
                        }
                    }
                }else{
                    for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                        s = src;
                        d = dst;

                        for(int _x = w; _x > 0; _x--){
                            pixel = *(cm + *s--);
                            a = (pixel >> 24) & 0xff;
                            if(alphaPre){
                                if(a == 255){
                                    *d-- = pixel;
                                }else{
                                    r = (pixel >> 16) & 0xff;
                                    g = (pixel >> 8) & 0xff;
                                    b = pixel & 0xff;
                                    r = MUL(a, r);
                                    g = MUL(a, g);
                                    b = MUL(a, b);
                                    *d-- = (a << 24) | (r << 16) | (g << 8) | b;
                                }
                                srcSurf->isAlphaPre = true;
                            }else{
                                if(a == 0) *d-- = 0;
                                else *d-- = pixel;
                                srcSurf->isAlphaPre = false;
                            }
                        }
                    }
                }
            }
            break;

        case BYTE_GRAY:
            {
                unsigned char *src, *s, *dst, *d, pixel;
                src_stride = srcSurf->scanline_stride;
                dst_stride = srcSurf->width << 2;

                src_offset = y * src_stride + x;
                dst_offset = y * dst_stride + (x << 2);
                src = (unsigned char *)srcDataPtr + src_offset;
                dst = (unsigned char *)bmpDataPtr + dst_offset;

                for(int _y = h; _y > 0; _y--, src += src_stride, dst += dst_stride){
                    s = src;
                    d = dst;

                    for(int _x = w; _x > 0; _x--){
                        pixel = *s++;
                        *d++ = pixel;
                        *d++ = pixel;
                        *d++ = pixel;
                        *d++ = 255;
                    }
                }
            }
            break;
    }
    env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_ImageSurface
 * Method:    createSurfStruct
 * Signature: (IIIIIIIIII[I[II[IIZ[I[IIZZI)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_ImageSurface_createSurfStruct
(JNIEnv * env, jobject obj, jint surfType, jint width, jint height, jint cmType, 
    jint csType, jint smType, jint dataType, jint numComponents, jint pixelStride, 
    jint scanlineStride, jintArray bits, jintArray masks, jint colorMapSize, 
    jintArray colorMap, jint transpPixel, jboolean isGrayPalette, jintArray bankIndeces, 
    jintArray bandOffsets, jint offset, jboolean hasAlpha, jboolean isAlphaPre, 
    jint transparency){

        SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)calloc(sizeof(SURFACE_STRUCTURE), 1);

        if(surf != NULL){
            surf->ss_type = surfType;
            surf->width = width;
            surf->height = height;
            surf->cm_type = cmType;
            surf->cs_type = csType;
            surf->data_type = dataType;
            surf->num_components = numComponents;
            surf->pixel_stride = pixelStride;
            surf->scanline_stride = scanlineStride;
            surf->offset = offset;
            surf->has_alpha = hasAlpha;
            surf->isAlphaPre = isAlphaPre != 0;
            surf->transparency = transparency;

            if(dataType == TYPE_BYTE){
                surf->scanline_stride_byte = scanlineStride;
            }else if(dataType == TYPE_USHORT){
                surf->scanline_stride_byte = scanlineStride << 1;
            }else if(dataType == TYPE_INT){
                surf->scanline_stride_byte = scanlineStride << 2;
            }

            void *p;
            int *s, *d;

            switch(cmType){
                case DIRECT_CM:
                    surf->bits = (int *)malloc(surf->num_components * sizeof(int));
                    p = env->GetPrimitiveArrayCritical(bits, 0);
                    d = surf->bits;
                    s = (int *)p;

                    for(int i = 0; i < numComponents; i++){
                        *d++ = *s++;
                    }

                    env->ReleasePrimitiveArrayCritical(bits, p, 0);

                    p = env->GetPrimitiveArrayCritical(masks, 0);
                    s = (int *)p;
                                 
                    surf->red_mask = *s++;
                    surf->green_mask = *s++;
                    surf->blue_mask = *s++;
                    if(hasAlpha){
                        surf->alpha_mask = *s;
                    }
                    env->ReleasePrimitiveArrayCritical(masks, p, 0);

                    surf->red_sht = getShift(surf->red_mask);
                    surf->max_red = (1 << surf->bits[0]) - 1;
                    surf->green_sht = getShift(surf->green_mask);
                    surf->max_green = (1 << surf->bits[1]) - 1;
                    surf->blue_sht = getShift(surf->blue_mask);
                    surf->max_blue = (1 << surf->bits[2]) - 1;
                    if(hasAlpha){
                        surf->alpha_sht = getShift(surf->alpha_mask);
                        surf->max_alpha = ( 1 << surf->bits[3]) - 1;
                    }
                    break;

                case INDEX_CM:
                    surf->colormap_size = colorMapSize;
                    surf->transparent_pixel = transpPixel;
                    surf->isGrayPallete = isGrayPalette;
                    surf->colormap = (int *)malloc(colorMapSize * sizeof(int));

                    p = env->GetPrimitiveArrayCritical(colorMap, 0);
                    memcpy(surf->colormap, p, colorMapSize * sizeof(int));
                    env->ReleasePrimitiveArrayCritical(colorMap, p, 0);
                    break;

                case COMPONENT_CM:
                    surf->bank_indexes = (int *)malloc(numComponents * sizeof(int));
                    surf->band_offsets = (int *)malloc(numComponents * sizeof(int));

                    p = env->GetPrimitiveArrayCritical(bankIndeces, 0);
                    memcpy((void *)surf->bank_indexes, p, numComponents * sizeof(int));
                    env->ReleasePrimitiveArrayCritical(bankIndeces, p, 0);

                    p = env->GetPrimitiveArrayCritical(bandOffsets, 0);
                    memcpy((void *)surf->band_offsets, p, numComponents * sizeof(int));
                    env->ReleasePrimitiveArrayCritical(bandOffsets, p, 0);
                    break;
            }
            surf->invalidated = true;
            surf->bmp_byte_stride = surf->width << 2;

#ifdef _WIN32

            surf->bmpInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
            surf->bmpInfo.bmiHeader.biWidth = surf->width;
            surf->bmpInfo.bmiHeader.biHeight = -surf->height;
            surf->bmpInfo.bmiHeader.biPlanes = 1;
            surf->bmpInfo.bmiHeader.biBitCount = 32;
            surf->bmpInfo.bmiHeader.biSizeImage = surf->bmp_byte_stride * surf->height;
            surf->bmpInfo.bmiHeader.biCompression = BI_BITFIELDS;
            DWORD *colors = (DWORD *)surf->bmpInfo.bmiColors;
            colors[0] = 0xff0000;
            colors[1] = 0xff00;
            colors[2] = 0xff;

            HDC dc = GetDC(NULL);
            surf->srcDC = CreateCompatibleDC(NULL);
            if(GetDeviceCaps(dc, BITSPIXEL) != 32) {
                surf->bitmap = CreateDIBSection(NULL, (BITMAPINFO *)&surf->bmpInfo, DIB_RGB_COLORS, &surf->bmpData, NULL, 0);
            } else {
                surf->bitmap = CreateCompatibleBitmap(dc, surf->width, surf->height);
                surf->bmpData = malloc(surf->bmp_byte_stride  * surf->height);
                surf->isTrueColor = TRUE;
            }
            ReleaseDC(NULL, dc);
            if(surf->srcDC != NULL && surf->bitmap != NULL){
                SelectObject(surf->srcDC, surf->bitmap);
            }
            surf->isAlphaPre = true;
#else
            surf->bmpData = malloc(surf->bmp_byte_stride  * surf->height);
#endif
        }
        return (jlong)surf;
  
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_ImageSurface_updateCache
(JNIEnv *env, jobject obj, jlong ptr, jobject data, jboolean alphaPre){

    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)ptr;
    jlong cachePtr = 0;
    if(surf != NULL){
        updateCache(surf, env, data, alphaPre != 0, 0, 0, surf->width, surf->height);
        cachePtr = (jlong)surf->bmpData;
    }
    return cachePtr;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_ImageSurface_dispose
(JNIEnv *env, jobject obj, jlong ptr){

    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)ptr;

    if(surf != NULL){
        if(surf->bits) free(surf->bits);
        if(surf->colormap) free(surf->colormap);
        if(surf->bank_indexes) free(surf->bank_indexes);
        if(surf->band_offsets) free(surf->band_offsets);
#ifdef _WIN32
        if(surf->isTrueColor){
            if(surf->bmpData) free(surf->bmpData);
        }
        if(surf->bitmap) DeleteObject(surf->bitmap);
        if(surf->srcDC) DeleteDC(surf->srcDC);
#else
        if(surf->bmpData) free(surf->bmpData);
#endif
        free(surf);
    }
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_ImageSurface_setImageSize
(JNIEnv *env, jobject obj, jlong ptr, jint width, jint height) {

    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)ptr;
    surf->scanline_stride = surf->scanline_stride / surf->width * width;
    surf->scanline_stride_byte = surf->scanline_stride_byte / surf->width * width;
    surf->width = width;
    surf->height = height;
}
