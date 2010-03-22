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
 * @author Oleg V. Khaschansky
 * 
 */

#include "NativeCMM.h"
#include <string.h>

static LCMSBOOL cmsInitialized = FALSE; 
static char *errMsg = NULL;

int gl_cmsErrorHandler(int errorCode, const char *msg) {
  if(errorCode == LCMS_ERRC_ABORTED) {
    // Throw exception later, after returning control from cmm
#if defined(ZOS) || defined(LINUX) || defined(FREEBSD) || defined(MACOSX)
    errMsg = strdup(msg);
#else
    errMsg = _strdup(msg);
#endif
  }

  return 1;
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmOpenProfile
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL 
    Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmOpenProfile(JNIEnv *env, jclass cls, jbyteArray data)
{
  jbyte *byteData = (*env)->GetByteArrayElements (env, data, 0);
  DWORD dataSize = (*env)->GetArrayLength (env, data);
    cmsHPROFILE hProfile;

  // Set up error handling if needed
  if(!cmsInitialized) {
    cmsErrorAction(LCMS_ERROR_SHOW);
    cmsSetErrorHandler(gl_cmsErrorHandler);
    cmsInitialized = TRUE;
  }

    hProfile = cmmOpenProfile((LPBYTE)byteData, dataSize);

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);

    if(hProfile == NULL) {
        newCMMException(env, errMsg); // Throw java exception if error occured
        free(errMsg);
        errMsg = NULL;
    }

  // Return obtained handle
  return (jlong) ((IDATA)hProfile);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmCloseProfile
 * Signature: (J)V
 */
JNIEXPORT void JNICALL 
    Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmCloseProfile(JNIEnv *env, jclass cls, jlong profileID)
{
  cmsHPROFILE hProfile = (cmsHPROFILE) ((IDATA)profileID);

    if(!cmsCloseProfile(hProfile)) {
        newCMMException(env, errMsg); // Throw java exception if error occured
        free(errMsg);
        errMsg = NULL;
    }        
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfileSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfileSize(JNIEnv *env, jclass cls, jlong profileID)
{
  cmsHPROFILE hProfile = (cmsHPROFILE) ((IDATA)profileID);
  return (jint) cmmGetProfileSize(hProfile);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfile
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfile(
    JNIEnv *env,
    jclass cls,
    jlong profileID,
    jbyteArray data)
{
    cmsHPROFILE hProfile = (cmsHPROFILE) ((IDATA)profileID);
    unsigned profileSize = (unsigned) (*env)->GetArrayLength (env, data);
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, 0);

  cmmGetProfile(hProfile, (LPBYTE)byteData, profileSize);

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfileElement
 * Signature: (JI[B)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfileElement
  (JNIEnv *env, jclass cls, jlong profileID, jint tagSignature, jbyteArray data)
{
  size_t dataSize = (*env)->GetArrayLength(env, data);
    icTagSignature ts = tagSignature;
    cmsHPROFILE hProfile = (cmsHPROFILE) ((IDATA)profileID);
    jbyte *byteData = (*env)->GetByteArrayElements (env, data, 0);



  if(ts == HEADER_TAG_ID) {
        if(!cmmGetProfileHeader(hProfile, (LPBYTE)byteData, dataSize)) {
            newCMMException(env, errMsg); // Throw java exception if error occured
            free(errMsg);
            errMsg = NULL;
        }
    } else {
        if(!cmmGetProfileElement(hProfile, ts, (LPBYTE)byteData, &dataSize)) {
            newCMMException(env, errMsg); // Throw java exception if error occured
            free(errMsg);
            errMsg = NULL;
        }
    }

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfileElementSize
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfileElementSize
  (JNIEnv *env, jclass cls, jlong profileID, jint tagSignature)
{

    long size;
    icTagSignature ts = tagSignature;
    cmsHPROFILE hProfile = (cmsHPROFILE) ((IDATA)profileID);

    if (ts == HEADER_TAG_ID) {
        size = HEADER_SIZE;
    } else {
        size = cmmGetProfileElementSize(hProfile, ts);
    }

  if(size < 0)
    newCMMException(env, "Profile element not found"); // Throw java exception if error occured

    return (jint) size;
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmSetProfileElement
 * Signature: (JI[B)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmSetProfileElement
  (JNIEnv *env, jclass cls, jlong profileID, jint tagSignature, jbyteArray data)
{
    cmsHPROFILE hProfile = (cmsHPROFILE) ((IDATA)profileID);
    jbyte *byteData = (*env)->GetByteArrayElements (env, data, 0);
    size_t dataSize = (*env)->GetArrayLength(env, data);
    icTagSignature ts = tagSignature;

    if(ts == HEADER_TAG_ID) {
    if(dataSize != sizeof(icHeader))
      newCMMException(env, "Invalid size of the data"); // Throw java exception 

        if(!cmmSetProfileHeader(hProfile, (LPBYTE)byteData))
            newCMMException(env, "Invalid header data"); // Throw java exception if error occured

    } else {
      if(!cmmSetProfileElement(hProfile, ts, byteData, dataSize)) {
            newCMMException(env, errMsg); // Throw java exception if error occured
            free(errMsg);
            errMsg = NULL;
      }
    }

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmCreateMultiprofileTransform
 * Signature: ([J[I)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmCreateMultiprofileTransform
(JNIEnv *env, jclass cls, jlongArray jProfileHandles, jintArray jRenderingIntents) {
  cmsHTRANSFORM xform;

  jint *renderingIntentsData;
  int intent, i;

  int nProfiles = (*env)->GetArrayLength (env, jProfileHandles);
    jlong *profileHandlesData = (*env)->GetLongArrayElements (env, jProfileHandles, 0);

    // Convert to appropriate size
    cmsHPROFILE *profileHandles = malloc(sizeof(cmsHPROFILE)*nProfiles);
    for(i=0; i<nProfiles; i++) {
      profileHandles[i] = (cmsHPROFILE) ((IDATA)profileHandlesData[i]);
    }

  // XXX - Todo - consider getting all rendering intents
    renderingIntentsData = (*env)->GetIntArrayElements(env, jRenderingIntents, 0);
  intent = renderingIntentsData[0];

  xform = cmmCreateMultiprofileTransform(profileHandles, nProfiles, intent);

    (*env)->ReleaseLongArrayElements(env, jProfileHandles, profileHandlesData, 0);
    (*env)->ReleaseIntArrayElements(env, jRenderingIntents, renderingIntentsData, 0);
  
  free(profileHandles);

  if(xform == NULL) 
    newCMMException(env, "Can't create ICC transform"); // Throw java exception

  return (jlong) ((IDATA)xform);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmDeleteTransform
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmDeleteTransform
  (JNIEnv *env, jclass cls, jlong transformHandle)
{
    cmsHTRANSFORM xform = (cmsHTRANSFORM) ((IDATA)transformHandle);

    if(xform != NULL) {
        cmsDeleteTransform(xform);
    } else
        throwNPException(env, "Invalid ICC transform passed to CMM");
}

static long getScanlineStrideFromFormat(ImageFormat *fmt) {
  return 
    fmt->cols * 
    T_BYTES(fmt->cmmFormat) * 
    (T_CHANNELS(fmt->cmmFormat) + T_EXTRA(fmt->cmmFormat));
}

static long getPixelStrideFromFormat(ImageFormat *fmt) {
  return 
    T_BYTES(fmt->cmmFormat) * 
    (T_CHANNELS(fmt->cmmFormat) + T_EXTRA(fmt->cmmFormat));
}

static long getSampleSizeFromFormat(ImageFormat *fmt) {
  return T_BYTES(fmt->cmmFormat) ? T_BYTES(fmt->cmmFormat) : 8;
}

static void copyAlphaChannel(
  BYTE *srcPtr, BYTE *dstPtr, 
  int srcPixelStride, int dstPixelStride, 
  int srcSampleSize, int dstSampleSize,
  int nPixels
) {
  register LPBYTE src = srcPtr, dst = dstPtr;
  int i;

  if(srcSampleSize == 1 && dstSampleSize == 1) {
    for(i=0; i<nPixels; i++) {
      *dst=*src;
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else if(srcSampleSize == 2 && dstSampleSize == 2) {
    for(i=0; i<nPixels; i++) {      
      *dst=*src;
      *(dst+1)=*(src+1);
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else if(srcSampleSize == 2 && dstSampleSize == 1) {
    for(i=0; i<nPixels; i++) {
      *((LPWORD)dst)=RGB_8_TO_16(*src);
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else if(srcSampleSize == 1 && dstSampleSize == 2) {
    for(i=0; i<nPixels; i++) {
      *dst=RGB_16_TO_8(*((LPWORD)src));
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else { // All other sample types, very slow
    double d = 0;
    
    double srcMax = 1 << srcSampleSize*8;
    double dstMax = 1 << dstSampleSize*8;

    for(i=0; i<nPixels; i++) {
      if(srcSampleSize == 0) {
        d = *((double *) (src));
      } else {
        int sh;
        for(sh=0; sh<srcSampleSize; sh++) {
          d+=((*(src+sh)) << (sh*8));
        }
        d /= (srcMax-1);        
      }

      if(dstSampleSize == 0) {
        *((double *) (src)) = d;
      } else {
        int sh;
        long l = (long)(d * (dstMax-1) + 0.5);        
        for(sh=0; sh<dstSampleSize; sh++) {
          *(dst+sh) = (BYTE) (l % 256);
          l /= 256;
        }        
      }

      src += srcPixelStride;
      dst += dstPixelStride;
    }
  }
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmTranslateColors
 * Signature: (JLorg/apache/harmony/awt/gl/color/NativeImageFormat;Lorg/apache/harmony/awt/gl/color/NativeImageFormat;)V
 */ 
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmTranslateColors
  (JNIEnv *env, jclass cls, jlong transformHandle, jobject src, jobject dst)
{
  int srcPixels, dstPixels;
  ImageFormat *srcFormat, *dstFormat;
  int srcSampleSize, dstSampleSize;
  int srcPixelStride, dstPixelStride;
  BYTE *srcPtr, *dstPtr;
  int i;
  LCMSBOOL copyAlpha = FALSE;
  LCMSBOOL fillAlpha = FALSE;

    cmsHTRANSFORM xform = (cmsHTRANSFORM) ((IDATA)transformHandle);


    srcFormat = getImageFormat(env, src);
    dstFormat = getImageFormat(env, dst);

  // Do we have to copy alpha?
  copyAlpha = srcFormat->alphaOffset >= 0 && dstFormat->alphaOffset >= 0;
  fillAlpha = dstFormat->alphaOffset >= 0;

  if(copyAlpha) {
    srcSampleSize = getSampleSizeFromFormat(srcFormat);
    dstSampleSize = getSampleSizeFromFormat(dstFormat);
    srcPixelStride = getPixelStrideFromFormat(srcFormat);
    dstPixelStride = getPixelStrideFromFormat(dstFormat);
  } else if(fillAlpha) {
    dstPixelStride = getPixelStrideFromFormat(dstFormat);
  }

  srcPixels = srcFormat->cols * srcFormat->rows;
  dstPixels = dstFormat->cols * dstFormat->rows;
  
  cmsChangeBuffersFormat(xform, srcFormat->cmmFormat, dstFormat->cmmFormat);

  srcPtr = srcFormat->imageData + srcFormat->dataOffset;
  dstPtr = dstFormat->imageData + dstFormat->dataOffset;
  
  if(srcFormat->scanlineStride < 0 && dstFormat->scanlineStride < 0) {
    
    if(copyAlpha) { // Copy alpha
      copyAlphaChannel(
        srcPtr + srcFormat->alphaOffset, dstPtr + dstFormat->alphaOffset, 
        srcPixelStride, dstPixelStride, 
        srcSampleSize, dstSampleSize, 
        dstPixels
      ); 
    } else if(fillAlpha) { // Fill with 1's
      memset(dstPtr, 0xFF, dstPixels*dstPixelStride); 
    }

    cmsDoTransform(xform, srcPtr, dstPtr, dstPixels); // Call LCMS!

  } else { // Process each scanline
    if(srcFormat->scanlineStride < 0)
      srcFormat->scanlineStride = getScanlineStrideFromFormat(srcFormat);
    if(dstFormat->scanlineStride < 0)
      dstFormat->scanlineStride = getScanlineStrideFromFormat(dstFormat);

    for(i=0; i<srcFormat->rows; i++) {
      
      if(copyAlpha) { // Copy Alpha
        copyAlphaChannel(
          srcPtr + srcFormat->alphaOffset, dstPtr + dstFormat->alphaOffset, 
          srcPixelStride, dstPixelStride, 
          srcSampleSize, dstSampleSize, 
          srcFormat->cols
        ); 
      } else if(fillAlpha) { // Fill with 1's
        memset(dstPtr, 0xFF, dstFormat->cols*dstPixelStride); 
      }

      cmsDoTransform(xform, srcPtr, dstPtr, dstFormat->cols);

      srcPtr += srcFormat->scanlineStride;
      dstPtr += dstFormat->scanlineStride;
    }
  }

    releaseImageFormat(env, srcFormat);
    releaseImageFormat(env, dstFormat);
}

/*
void main() {
  DWORD size;
  size_t tagSz;
  LPLCMSICCPROFILE hPf, hPfGrey;
  LPVOID pyccBuff, greyBuff;
  DWORD sig;
  unsigned char* data = NULL;
  cmsHTRANSFORM xform, xform1;

  FILE *f = fopen("C:\\jrockit-j2sdk1.4.2_04\\jre\\lib\\cmm\\srgb.pf", "rb");
  fseek(f, 0L, SEEK_END);
  size = ftell(f);
  pyccBuff = malloc(size);
  fseek(f, 0L, SEEK_SET);
  fread(pyccBuff, 1, size, f);
  fclose(f);

  hPf = cmmOpenProfile(pyccBuff, size);
  
  sig = TAG_SIGNATURE('w','t','p','t');
  tagSz = cmmGetProfileElementSize(hPf, sig);
  data = malloc(tagSz);
  cmmGetProfileElement(hPf, sig, data, &tagSz);
  memset(data, 0, tagSz);
  data[0] = 0x58;
  data[1] = 0x59;
  data[2] = 0x5A;
  data[3] = 0x20;
  cmmSetProfileElement(hPf, sig, data, tagSz);
  free(data);

  // Save the profile
  size = (DWORD) cmmGetProfileSize(hPf);
  pyccBuff = realloc(pyccBuff, size);
  cmmGetProfile(hPf, pyccBuff, size);
  f = fopen("C:\\jrockit-j2sdk1.4.2_04\\jre\\lib\\cmm\\qq.icc", "wb");
  fwrite(pyccBuff, 1, size, f);
  fclose(f);
///
  cmmCloseProfile(hPf);
  hPf = cmmOpenProfile(pyccBuff, size);
///
// Open Grey profile
  f = fopen("C:\\jrockit-j2sdk1.4.2_04\\jre\\lib\\cmm\\gray.pf", "rb");
  fseek(f, 0L, SEEK_END);
  size = ftell(f);
  greyBuff = malloc(size);
  fseek(f, 0L, SEEK_SET);
  fread(greyBuff, 1, size, f);
  fclose(f);

  hPfGrey = cmmOpenProfile(greyBuff, size);
/////////////////////
// Transforms
  {
    unsigned char in[4] = {0, 0, 0, 0};
    unsigned char gray[2] = {0,0};
    unsigned char out[4];

    xform = cmmCreateTransform(hPf, TYPE_RGBA_8, hPfGrey, TYPE_GRAYA_8, INTENT_PERCEPTUAL, 0);
    xform1 = cmmCreateTransform(hPfGrey, TYPE_GRAYA_8, hPf, TYPE_RGBA_8, INTENT_PERCEPTUAL, 0);
    cmsDoTransform(xform, in, gray, 1);
    cmsDoTransform(xform1, gray, out, 1);
    cmsDeleteTransform(xform);
    cmsDeleteTransform(xform1);

//    LPLCMSICCPROFILE profiles[3] = {hPf, hPfGrey, hPf};
//    xform = cmmCreateMultiprofileTransform(profiles, 3, INTENT_PERCEPTUAL);
//    cmsChangeBuffersFormat(xform, TYPE_RGBA_8, TYPE_RGBA_8);
//    cmsDoTransform(xform, in, out, 1);
//    cmsDeleteTransform(xform);
    
  }
/////////////////////
  cmmCloseProfile(hPf);
  cmmCloseProfile(hPfGrey);
  free(pyccBuff);
}
*/
