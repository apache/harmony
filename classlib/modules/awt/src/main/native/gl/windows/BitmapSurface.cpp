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
 
#include "BitmapSurface.h"
#include "org_apache_harmony_awt_gl_windows_BitmapSurface.h"

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_BitmapSurface_createSurfData
(JNIEnv *env, jobject obj, jlong gi, jint width, jint height){

    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)calloc(sizeof(SURFACE_STRUCTURE), 1);
    surf->width = width;
    surf->height = height;
    surf->gi = (GraphicsInfo *)gi;
    parseFormat(surf);
    return (jlong)surf;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_BitmapSurface_dispose
(JNIEnv *env, jobject obj, jlong surfDataPtr){

    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)surfDataPtr;
    if(surf){
        if(surf->bits) free(surf->bits);
        if(surf->colormap) free(surf->colormap);
        if(surf->bank_indexes) free(surf->bank_indexes);
        if(surf->band_offsets) free(surf->band_offsets);
        if(surf->bmpData) free(surf->bmpData);
        free(surf);
    }
}

void parseFormat(SURFACE_STRUCTURE *surfStruct){

      surfStruct->bmpInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
      GetDIBits(surfStruct->gi->hdc, surfStruct->gi->bmp, 0, 1, NULL, (BITMAPINFO *)&surfStruct->bmpInfo, DIB_RGB_COLORS);
      GetDIBits(surfStruct->gi->hdc, surfStruct->gi->bmp, 0, 1, NULL, (BITMAPINFO *)&surfStruct->bmpInfo, DIB_RGB_COLORS);

      DWORD imageSize = surfStruct->bmpInfo.bmiHeader.biSizeImage;
      surfStruct->scanline_stride_byte = imageSize / surfStruct->bmpInfo.bmiHeader.biHeight;
      surfStruct->pixel_stride = surfStruct->bmpInfo.bmiHeader.biBitCount;
      surfStruct->transparency = GL_OPAQUE;
      surfStruct->alpha_pre = false;

      if(surfStruct->bmpInfo.bmiHeader.biCompression == BI_BITFIELDS){
          DWORD *mask = (DWORD *)surfStruct->bmpInfo.bmiColors;
          surfStruct->alpha_mask = 0;
          if(mask[0] == 0x7c00 && mask[1] == 0x03e0 && mask[2] == 0x1f){
              surfStruct->ss_type = USHORT_555;
              surfStruct->red_mask = 0x7c00;
              surfStruct->green_mask = 0x03e0;
              surfStruct->blue_mask = 0x1f;
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte >> 1;
          }else if(mask[0] == 0xf800 && mask[1] == 0x07e0 && mask[2] == 0x1f){
              surfStruct->ss_type = USHORT_565;
              surfStruct->red_mask = 0xf800;
              surfStruct->green_mask = 0x07e0;
              surfStruct->blue_mask = 0x1f;
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte >> 1;
          }else if(mask[0] == 0xff0000 && mask[1] == 0xff00 && mask[2] == 0xff){
              surfStruct->ss_type = INT_RGB;
              surfStruct->red_mask = 0xff0000;
              surfStruct->green_mask = 0xff00;
              surfStruct->blue_mask = 0xff;
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte >> 2;
          }else{
              surfStruct->ss_type = -1;
              return;
          }
      }else{
          if(surfStruct->bmpInfo.bmiHeader.biBitCount <= 8){
              if(surfStruct->bmpInfo.bmiHeader.biBitCount <= 4){
                  surfStruct->ss_type = BYTE_BINARY;
              }else{
                  surfStruct->ss_type = BYTE_INDEXED;
              }

              surfStruct->colormap_size = 1 << surfStruct->pixel_stride;
              surfStruct->colormap = (int *)malloc(surfStruct->colormap_size * sizeof(int));
              memcpy(surfStruct->colormap, surfStruct->bmpInfo.bmiColors, surfStruct->colormap_size * sizeof(DWORD));
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte;
          }else if(surfStruct->bmpInfo.bmiHeader.biBitCount <= 16){
              surfStruct->ss_type = USHORT_555;
              surfStruct->red_mask = 0x7c00;
              surfStruct->green_mask = 0x03e0;
              surfStruct->blue_mask = 0x1f;
              surfStruct->alpha_mask = 0;
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte >> 1;
          }else if(surfStruct->bmpInfo.bmiHeader.biBitCount <= 24){
              surfStruct->ss_type = BYTE_BGR;
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte;
          }else{
              surfStruct->ss_type = BYTE_ABGR;
              surfStruct->scanline_stride = surfStruct->scanline_stride_byte >> 2;
          }
      }
      surfStruct->bmpInfo.bmiHeader.biHeight = -surfStruct->bmpInfo.bmiHeader.biHeight;
}  
