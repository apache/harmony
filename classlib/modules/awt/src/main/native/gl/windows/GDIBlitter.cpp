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
 
#include "SurfaceDataStructure.h"
#include "bitmapSurface.h"
#include "org_apache_harmony_awt_gl_windows_GDISurface.h"
#include "org_apache_harmony_awt_gl_windows_GDIBlitter.h"
#include "blitter.h"
#include "GDIBlitter.h"
#include "gl_GDIPlus.h"

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_windows_GDISurface_createSurfData
(JNIEnv *env, jobject obj, jlong gi){

    SURFACE_STRUCTURE *surf = (SURFACE_STRUCTURE *)calloc(sizeof(SURFACE_STRUCTURE), 1);
    surf->gi = (GraphicsInfo *)gi;
    return (jlong)surf;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_GDISurface_dispose
(JNIEnv *env, jobject obj, jlong surfDataPtr){

    if(surfDataPtr) free((void *)surfDataPtr);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_GDIBlitter_bltBGImage
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
  jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, 
  jint bgcolor, jint compType, jfloat alpha, jdoubleArray matrix, jintArray clip, 
  jint numVertex, jboolean invalidated, jintArray dirtyRegions, jint regCount){

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      srcSurf->invalidated = invalidated != 0;
      HDC tmpDC = CreateCompatibleDC(dstSurf->gi->hdc);
      int w = srcSurf->width;
      int h = srcSurf->height;
      HBITMAP tmpBmp = CreateCompatibleBitmap(dstSurf->gi->hdc, w, h);
      SelectObject(tmpDC, tmpBmp);

      BYTE a = (BYTE)((bgcolor >> 24) & 0xff);
      BYTE r = (BYTE)((bgcolor >> 16) & 0xff);
      BYTE g = (BYTE)((bgcolor >> 8) & 0xff);
      BYTE b = (BYTE)(bgcolor & 0xff);
      r = MUL(a, r);
      g = MUL(a, g);
      b = MUL(a, b);

      HBRUSH brush = CreateSolidBrush(RGB(r, g, b));
      SelectObject(tmpDC, brush);
      PatBlt(tmpDC, 0, 0, w, h, PATCOPY);
      
      int *regions;
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

      if(initBitmap(srcSurf, env, srcData, true, regions, regCount)){
          BLENDFUNCTION bf;
          bf.AlphaFormat = AC_SRC_ALPHA;
          bf.BlendOp = AC_SRC_OVER;
          bf.BlendFlags = 0;
          bf.SourceConstantAlpha = 255;
          AlphaBlend(tmpDC, 0, 0, w, h, srcSurf->srcDC, 0, 0, w, h, bf);
      }

      UCHAR srca = (UCHAR)(alpha * 255 + 0.5);

      XFORM currentTransform, transform;
      if(matrix != NULL){

          jdouble * mtrx = (jdouble *)env->GetPrimitiveArrayCritical(matrix, 0);
          jdouble * old_mtrx = mtrx;

          transform.eM11 = (FLOAT)(*mtrx++);
          transform.eM12 = (FLOAT)(*mtrx++);
          transform.eM21 = (FLOAT)(*mtrx++);
          transform.eM22 = (FLOAT)(*mtrx++);
          transform.eDx = (FLOAT)(*mtrx++);
          transform.eDy = (FLOAT)(*mtrx);

          env->ReleasePrimitiveArrayCritical(matrix, old_mtrx, 0);

          SetGraphicsMode(dstSurf->gi->hdc, GM_ADVANCED);
          GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetWorldTransform(dstSurf->gi->hdc, &transform);
      }

      HRGN oldClip = setGdiClip(env, dstSurf->gi->hdc, clip, numVertex);

      BLITSTRUCT blitStruct;
      memset(&blitStruct, 0, sizeof(BLITSTRUCT));

      switch(compType){
          case COMPOSITE_CLEAR:
          case COMPOSITE_SRC_OUT:
              blitStruct.blitFunctintType = BIT_BLT;
              blitStruct.rastOp = BLACKNESS;
              break;

          case COMPOSITE_SRC:
          case COMPOSITE_SRC_IN:
              blitStruct.blitFunctintType = BIT_BLT;
              if(srca == 0) blitStruct.rastOp = BLACKNESS;
              else blitStruct.rastOp = SRCCOPY;
              break;

          case COMPOSITE_DST:
          case COMPOSITE_DST_OVER:
              return;

          case COMPOSITE_SRC_ATOP:
          case COMPOSITE_SRC_OVER:
              if(srca == 255){
                  blitStruct.blitFunctintType = BIT_BLT;
                  blitStruct.rastOp = SRCCOPY;
              }else{

                  blitStruct.blitFunctintType = ALPHA_BLEND;
                  blitStruct.blendFunc.AlphaFormat = 0;
                  blitStruct.blendFunc.BlendOp = AC_SRC_OVER;
                  blitStruct.blendFunc.BlendFlags = 0;
                  blitStruct.blendFunc.SourceConstantAlpha = srca;
              }
              break;

          case COMPOSITE_DST_IN:
          case COMPOSITE_DST_ATOP:
              if(srca != 0) return;
              blitStruct.blitFunctintType = BIT_BLT;
              blitStruct.rastOp = BLACKNESS;
              break;

          case COMPOSITE_DST_OUT:
          case COMPOSITE_XOR:
              if(srca != 255) return;
              blitStruct.blitFunctintType = BIT_BLT;
              blitStruct.rastOp = BLACKNESS;
              break;
      }

      switch(blitStruct.blitFunctintType){
          case ALPHA_BLEND:
              AlphaBlend(dstSurf->gi->hdc, dstX, dstY, width, height, tmpDC,
                      srcX, srcY, width, height, blitStruct.blendFunc);
              break;

          case TRANSPARENT_BLT:
              TransparentBlt(dstSurf->gi->hdc, dstX, dstY, width, height, tmpDC,
                  srcX, srcY, width, height, srcSurf->rtc);
              break;

          default:
              BitBlt(dstSurf->gi->hdc, dstX, dstY, width, height, tmpDC,
                  srcX, srcY, blitStruct.rastOp);
              break;
      }
      if(matrix){
          SetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetGraphicsMode(dstSurf->gi->hdc, GM_COMPATIBLE);
      }
      restoreGdiClip(dstSurf->gi->hdc, oldClip);

      DeleteObject(brush);
      DeleteObject(tmpBmp);
      DeleteDC(tmpDC);

  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_GDIBlitter_bltImage
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
  jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, 
  jint compType, jfloat alpha, jdoubleArray matrix, jintArray clip, 
  jint numVertex, jboolean invalidated, jintArray dirtyRegions, jint regCount){

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      UCHAR srca = (UCHAR)(alpha * 255 + 0.5);

      BLITSTRUCT blitStruct;
      memset(&blitStruct, 0, sizeof(BLITSTRUCT));

      srcSurf->invalidated = invalidated != 0;
      
      int *regions;
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

      if(!initBlitData(srcSurf, env, srcData, compType, srca, &blitStruct, regions, regCount)){
          return;
      }

      XFORM currentTransform, transform;
      if(matrix != NULL){

          jdouble * mtrx = (jdouble *)env->GetPrimitiveArrayCritical(matrix, 0);
          jdouble * old_mtrx = mtrx;

          transform.eM11 = (FLOAT)(*mtrx++);
          transform.eM12 = (FLOAT)(*mtrx++);
          transform.eM21 = (FLOAT)(*mtrx++);
          transform.eM22 = (FLOAT)(*mtrx++);
          transform.eDx = (FLOAT)(*mtrx++);
          transform.eDy = (FLOAT)(*mtrx);

          env->ReleasePrimitiveArrayCritical(matrix, old_mtrx, 0);

          SetGraphicsMode(dstSurf->gi->hdc, GM_ADVANCED);
      }

      HRGN oldClip = setGdiClip(env, dstSurf->gi->hdc, clip, numVertex);

      switch(blitStruct.blitFunctintType){
          case ALPHA_BLEND:
              GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
              SetWorldTransform(dstSurf->gi->hdc, &transform);
              AlphaBlend(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->srcDC,
                      srcX, srcY, width, height, blitStruct.blendFunc);
              break;

          case TRANSPARENT_BLT:
              GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
              SetWorldTransform(dstSurf->gi->hdc, &transform);
              TransparentBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->srcDC,
                  srcX, srcY, width, height, srcSurf->rtc);
              break;

          case COMPOSITE_BLT:
              {
                  void *srcDataPtr = env->GetPrimitiveArrayCritical((jarray)srcData, 0);
                  CompositeBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf, srcDataPtr,
                      srcX, srcY, compType, srca, &currentTransform, &transform);
                  env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
              }
              break;
          default:
              GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
              SetWorldTransform(dstSurf->gi->hdc, &transform);
              BitBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->srcDC,
                  srcX, srcY, blitStruct.rastOp);
              break;
      }

      if(matrix){
          SetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetGraphicsMode(dstSurf->gi->hdc, GM_COMPATIBLE);
      }
      restoreGdiClip(dstSurf->gi->hdc, oldClip);

  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_GDIBlitter_bltBitmap
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, 
  jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, 
  jint compType, jfloat alpha, jdoubleArray matrix, jintArray clip, 
  jint numVertex){

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      UCHAR srca = (UCHAR)(alpha * 255 + 0.5);

      XFORM currentTransform, transform;
      if(matrix != NULL){

          jdouble * mtrx = (jdouble *)env->GetPrimitiveArrayCritical(matrix, 0);
          jdouble * old_mtrx = mtrx;

          transform.eM11 = (FLOAT)(*mtrx++);
          transform.eM12 = (FLOAT)(*mtrx++);
          transform.eM21 = (FLOAT)(*mtrx++);
          transform.eM22 = (FLOAT)(*mtrx++);
          transform.eDx = (FLOAT)(*mtrx++);
          transform.eDy = (FLOAT)(*mtrx);

          env->ReleasePrimitiveArrayCritical(matrix, old_mtrx, 0);

          SetGraphicsMode(dstSurf->gi->hdc, GM_ADVANCED);
          GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetWorldTransform(dstSurf->gi->hdc, &transform);
      }

      HRGN oldClip = setGdiClip(env, dstSurf->gi->hdc, clip, numVertex);

      BLITSTRUCT blitStruct;
      memset(&blitStruct, 0, sizeof(BLITSTRUCT));

      switch(compType){
          case COMPOSITE_CLEAR:
          case COMPOSITE_SRC_OUT:
              blitStruct.blitFunctintType = BIT_BLT;
              blitStruct.rastOp = BLACKNESS;
              break;

          case COMPOSITE_SRC:
          case COMPOSITE_SRC_IN:
              blitStruct.blitFunctintType = BIT_BLT;
              if(srca == 0) blitStruct.rastOp = BLACKNESS;
              else blitStruct.rastOp = SRCCOPY;
              break;

          case COMPOSITE_DST:
          case COMPOSITE_DST_OVER:
              return;

          case COMPOSITE_SRC_ATOP:
          case COMPOSITE_SRC_OVER:
              if(srca == 255){
                  blitStruct.blitFunctintType = BIT_BLT;
                  blitStruct.rastOp = SRCCOPY;
              }else{

                  blitStruct.blitFunctintType = ALPHA_BLEND;
                  blitStruct.blendFunc.AlphaFormat = 0;
                  blitStruct.blendFunc.BlendOp = AC_SRC_OVER;
                  blitStruct.blendFunc.BlendFlags = 0;
                  blitStruct.blendFunc.SourceConstantAlpha = srca;
              }
              break;

          case COMPOSITE_DST_IN:
          case COMPOSITE_DST_ATOP:
              if(srca != 0) return;
              blitStruct.blitFunctintType = BIT_BLT;
              blitStruct.rastOp = BLACKNESS;
              break;

          case COMPOSITE_DST_OUT:
          case COMPOSITE_XOR:
              if(srca != 255) return;
              blitStruct.blitFunctintType = BIT_BLT;
              blitStruct.rastOp = BLACKNESS;
              break;
      }

      switch(blitStruct.blitFunctintType){
          case ALPHA_BLEND:
              AlphaBlend(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->gi->hdc,
                      srcX, srcY, width, height, blitStruct.blendFunc);
              break;

          case TRANSPARENT_BLT:
              TransparentBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->gi->hdc,
                  srcX, srcY, width, height, srcSurf->rtc);
              break;

          default:
              BitBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->gi->hdc,
                  srcX, srcY, blitStruct.rastOp);
              break;
      }
      if(matrix){
          SetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetGraphicsMode(dstSurf->gi->hdc, GM_COMPATIBLE);
      }
      restoreGdiClip(dstSurf->gi->hdc, oldClip);
  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_GDIBlitter_xorImage
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
  jint dstX, jint dstY, jlong dstSurfStruct, jint width, jint height, jint xorcolor, 
  jdoubleArray matrix, jintArray clip, jint numVertex, jboolean invalidated, 
  jintArray dirtyRegions, jint regCount){

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      srcSurf->invalidated = invalidated != 0;

      int *regions;
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
      if(!initBitmap(srcSurf, env, srcData, false, regions, regCount)) return;

      BYTE r = (BYTE)((xorcolor >> 16) & 0xff);
      BYTE g = (BYTE)((xorcolor >> 8) & 0xff);
      BYTE b = (BYTE)(xorcolor & 0xff);

      HBRUSH brush = CreateSolidBrush(RGB(r, g, b));


      XFORM currentTransform, transform;
      if(matrix != NULL){

          jdouble * mtrx = (jdouble *)env->GetPrimitiveArrayCritical(matrix, 0);
          jdouble * old_mtrx = mtrx;

          transform.eM11 = (FLOAT)(*mtrx++);
          transform.eM12 = (FLOAT)(*mtrx++);
          transform.eM21 = (FLOAT)(*mtrx++);
          transform.eM22 = (FLOAT)(*mtrx++);
          transform.eDx = (FLOAT)(*mtrx++);
          transform.eDy = (FLOAT)(*mtrx);

          env->ReleasePrimitiveArrayCritical(matrix, old_mtrx, 0);

          SetGraphicsMode(dstSurf->gi->hdc, GM_ADVANCED);
          GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetWorldTransform(dstSurf->gi->hdc, &transform);
      }

      HRGN oldClip = setGdiClip(env, dstSurf->gi->hdc, clip, numVertex);

      HGDIOBJ oldBrush = SelectObject(dstSurf->gi->hdc, brush);

      if(srcSurf->has_alpha){

          int scanline_word = srcSurf->width / 16;
          if(srcSurf->width % 16 != 0) scanline_word++;

          BYTE *pm = (BYTE *)calloc(scanline_word * srcSurf->height * 2, 1);

          int byteIdx = 0;
          unsigned int *p = (unsigned int *)srcSurf->bmpData;
          for(int y = 0; y < srcSurf->height; y++){
              for(int x = 0, shift = 7; x < srcSurf->width; x++, shift--, p++){
                  if(shift < 0 ){
                      shift = 7;
                      byteIdx++;
                  } 
                  unsigned int pixel = (*p >> 24) & 0xff;
                  if(pixel > 127) pm[byteIdx] |= 1 << shift;
              }
              if(byteIdx % 2 != 0) byteIdx++;
              else byteIdx += 2;
          }      

          HBITMAP mask = CreateBitmap(srcSurf->width, srcSurf->height, 1, 1, pm);
          free(pm);
          MaskBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->srcDC,
                  srcX, srcY, mask, srcX, srcY, MAKEROP4(0x960169, 0xAA0029));
          DeleteObject(mask);
      }else{

          BitBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->srcDC,
                  srcX, srcY, 0x960169);
      }

      SelectObject(dstSurf->gi->hdc, oldBrush);


      if(matrix){
          SetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetGraphicsMode(dstSurf->gi->hdc, GM_COMPATIBLE);
      }
      restoreGdiClip(dstSurf->gi->hdc, oldClip);

      DeleteObject(brush);

  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_windows_GDIBlitter_xorBitmap
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jint dstX, 
  jint dstY, jlong dstSurfStruct, jint width, jint height, jint xorcolor, 
  jdoubleArray matrix, jintArray clip, jint numVertex){

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      BYTE r = (BYTE)((xorcolor >> 16) & 0xff);
      BYTE g = (BYTE)((xorcolor >> 8) & 0xff);
      BYTE b = (BYTE)(xorcolor & 0xff);

      HBRUSH brush = CreateSolidBrush(RGB(r, g, b));

      XFORM currentTransform, transform;
      if(matrix != NULL){

          jdouble * mtrx = (jdouble *)env->GetPrimitiveArrayCritical(matrix, 0);
          jdouble * old_mtrx = mtrx;

          transform.eM11 = (FLOAT)(*mtrx++);
          transform.eM12 = (FLOAT)(*mtrx++);
          transform.eM21 = (FLOAT)(*mtrx++);
          transform.eM22 = (FLOAT)(*mtrx++);
          transform.eDx = (FLOAT)(*mtrx++);
          transform.eDy = (FLOAT)(*mtrx);

          env->ReleasePrimitiveArrayCritical(matrix, old_mtrx, 0);

          SetGraphicsMode(dstSurf->gi->hdc, GM_ADVANCED);
          GetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetWorldTransform(dstSurf->gi->hdc, &transform);
      }

      HRGN oldClip = setGdiClip(env, dstSurf->gi->hdc, clip, numVertex);

      HGDIOBJ oldBrush = SelectObject(dstSurf->gi->hdc, brush);

      BitBlt(dstSurf->gi->hdc, dstX, dstY, width, height, srcSurf->gi->hdc,
                  srcX, srcY, 0x00960169);
      SelectObject(dstSurf->gi->hdc, oldBrush);


      if(matrix){
          SetWorldTransform(dstSurf->gi->hdc, &currentTransform);
          SetGraphicsMode(dstSurf->gi->hdc, GM_COMPATIBLE);
      }
      restoreGdiClip(dstSurf->gi->hdc, oldClip);

      DeleteObject(brush);


  }

void findNonExistColor(DWORD &tcolor, DWORD *colormap, UINT size){
     UINT *tmp = (UINT *)malloc(sizeof(UINT) * 257);
     memset(tmp, 0, sizeof(UINT) * size);
     UINT idx;
     for(UINT i = 0; i < size; i++){
         idx = 0x1ff & *(colormap + i);
         *(tmp + idx) = 1;
     }
     for(UINT i = 0; i < 257; i++){
         if(*(tmp + i) == 0){
             tcolor = i;
             break;
         }
     }
     free(tmp);
 }

BOOL isRepeatColor(UINT idx, DWORD *colormap, UINT size){
     DWORD tcolor = *(colormap + idx);
     BOOL repeat = false;
     for(UINT i = 0; i < size; i++){
         if(i == idx) continue;
         if((tcolor ^ *(colormap + i)) == 0){
             repeat = true;
             break;
         }
     }
     return repeat;
 }

BOOL initBlitData
(SURFACE_STRUCTURE *srcSurf, JNIEnv *env, jobject srcData, UINT compType, UCHAR srcConstAlpha, BLITSTRUCT *blitStruct, int *dirtyRegions, int regCount){

    switch(compType){
        case COMPOSITE_CLEAR:
        case COMPOSITE_SRC_OUT:
            blitStruct->blitFunctintType = BIT_BLT;
            blitStruct->rastOp = BLACKNESS;
            return true;

        case COMPOSITE_SRC:
        case COMPOSITE_SRC_IN:
            if(srcConstAlpha == 0){
                blitStruct->blitFunctintType = BIT_BLT;
                blitStruct->rastOp = BLACKNESS;
                return true;
            }
            if(srcSurf->invalidated || srcSurf->isAlphaPre != false){
                if(!initBitmap(srcSurf, env, srcData, false, dirtyRegions, regCount)) return false;
            }
            blitStruct->blitFunctintType = BIT_BLT;
            blitStruct->rastOp = SRCCOPY;
            return true;

        case COMPOSITE_SRC_OVER:
        case COMPOSITE_SRC_ATOP:
            if(srcSurf->invalidated || srcSurf->isAlphaPre != true){
                if(!initBitmap(srcSurf, env, srcData, true, dirtyRegions, regCount)) return false;
            }
            if(srcSurf->transparency != GL_OPAQUE || srcConstAlpha != 255){
                blitStruct->blitFunctintType = ALPHA_BLEND;
                blitStruct->blendFunc.AlphaFormat = srcSurf->transparency != GL_OPAQUE ? AC_SRC_ALPHA : 0;
                blitStruct->blendFunc.BlendOp = AC_SRC_OVER;
                blitStruct->blendFunc.BlendFlags = 0;
                blitStruct->blendFunc.SourceConstantAlpha = srcConstAlpha;
            }else{
                blitStruct->blitFunctintType = BIT_BLT;
                blitStruct->rastOp = SRCCOPY;
            }
            return true;

        case COMPOSITE_DST:
        case COMPOSITE_DST_OVER:
            return false;

        case COMPOSITE_DST_IN:
        case COMPOSITE_DST_ATOP:
            if(srcConstAlpha == 0){
                blitStruct->blitFunctintType = BIT_BLT;
                blitStruct->rastOp = BLACKNESS;
                return true;
            }
            blitStruct->blitFunctintType = COMPOSITE_BLT;
            return true;

        case COMPOSITE_DST_OUT:
        case COMPOSITE_XOR:
            if(srcConstAlpha != 255) return false; 
            if(srcConstAlpha == 255 && srcSurf->transparency == GL_OPAQUE){
                blitStruct->blitFunctintType = BIT_BLT;
                blitStruct->rastOp = BLACKNESS;
                return true;
            }
            blitStruct->blitFunctintType = COMPOSITE_BLT;
            return true;

        default:
            return false;
    }
}

BOOL initBitmap
(SURFACE_STRUCTURE *srcSurf, JNIEnv *env, jobject srcData, bool alphaPre, int *dirtyRegions, int regCount){

    HBITMAP srcBmp = srcSurf->bitmap;
    if(!srcBmp){
        return false;
    }
    for(int i = 0; i < regCount;){
        int x = dirtyRegions[i++];
        int y = dirtyRegions[i++];
        int w = dirtyRegions[i++] - x + 1;
        int h = dirtyRegions[i++] - y + 1;
        updateCache(srcSurf, env, srcData, alphaPre, x, y, w, h);
        if(srcSurf->isTrueColor){
            SetDIBits(srcSurf->srcDC, srcSurf->bitmap, srcSurf->height - y - h, h, (int *)srcSurf->bmpData + y * srcSurf->width, (BITMAPINFO *)&srcSurf->bmpInfo, DIB_RGB_COLORS);
        }else{
            GdiFlush();
        }
    }
    return true;
}

void CompositeBlt
(HDC dstDC, jint dstX, jint dstY, jint width, jint height, SURFACE_STRUCTURE *srcSurf, 
        void * srcData, jint srcX, jint srcY, UINT compType, UCHAR alpha, PXFORM currentTransform, 
        PXFORM transform){

    HDC dc = GetDC(NULL);
    if(dc == NULL) return;

    HDC tmpDC = CreateCompatibleDC(dc);
    if(!tmpDC){
        ReleaseDC(NULL, dc);
        return;
    }

    HBITMAP tmpBitmap = CreateCompatibleBitmap(dc, width, height);
    if(tmpBitmap == NULL){
        ReleaseDC(NULL, dc);
        DeleteDC(tmpDC);
        return;
    }

    ReleaseDC(NULL, dc);

    SelectObject(tmpDC, tmpBitmap);

    GraphicsInfo *gi = (GraphicsInfo *)malloc(sizeof(GraphicsInfo));
    if(gi == NULL){
        DeleteObject(tmpBitmap);
        DeleteDC(tmpDC);
        return;
    }
    memset(gi, 0, sizeof(GraphicsInfo));

    SURFACE_STRUCTURE *tmpSurf = (SURFACE_STRUCTURE *)malloc(sizeof(SURFACE_STRUCTURE));
    if(tmpSurf == NULL){
        DeleteObject(tmpBitmap);
        DeleteDC(tmpDC);
        free(gi);
        return;
    }
    memset(tmpSurf, 0, sizeof(SURFACE_STRUCTURE));

    gi->hdc = tmpDC;
    gi->bmp = tmpBitmap;

    tmpSurf->width = width;
    tmpSurf->height = height;
    tmpSurf->gi = (GraphicsInfo *)gi;
    parseFormat(tmpSurf);
    tmpSurf->bmpData = (BYTE *)malloc(tmpSurf->bmpInfo.bmiHeader.biSizeImage);
    if(tmpSurf->bmpData == NULL){
        DeleteObject(tmpBitmap);
        DeleteDC(tmpDC);
        free(gi);
        free(tmpSurf);
        return;
    }

    BitBlt(tmpDC, 0, 0, width, height, dstDC, dstX, dstY, SRCCOPY);

    GetDIBits(tmpDC, tmpBitmap, 0, tmpSurf->height, tmpSurf->bmpData, 
                          (BITMAPINFO *)&tmpSurf->bmpInfo, DIB_RGB_COLORS);

    switch(compType){
        case COMPOSITE_DST_IN:
        case COMPOSITE_DST_ATOP:
            dst_atop_custom(srcX, srcY, srcSurf, srcData, 0, 0, tmpSurf, 
                    tmpSurf->bmpData, width, height, alpha);
            break;

        case COMPOSITE_DST_OUT:
        case COMPOSITE_XOR:
            dst_out_custom(srcX, srcY, srcSurf, srcData, 0, 0, tmpSurf, 
                    tmpSurf->bmpData, width, height, alpha);
    }

    SetDIBits(tmpDC, tmpBitmap, 0, tmpSurf->height, tmpSurf->bmpData, (BITMAPINFO *)&tmpSurf->bmpInfo, DIB_RGB_COLORS);

    GetWorldTransform(dstDC, currentTransform);
    SetWorldTransform(dstDC, transform);
    BitBlt(dstDC, dstX, dstY, width, height, tmpDC, 0, 0, SRCCOPY);

    DeleteObject(tmpBitmap);
    DeleteDC(tmpDC);

    free(gi);
    free(tmpSurf->bmpData);
    if(tmpSurf->bits) free(tmpSurf->bits);
    if(tmpSurf->colormap) free(tmpSurf->colormap);
    if(tmpSurf->bank_indexes) free(tmpSurf->bank_indexes);
    if(tmpSurf->band_offsets) free(tmpSurf->band_offsets);
    free(tmpSurf);

}
