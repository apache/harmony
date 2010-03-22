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

#include "pngdecoder.h"

/*
 * Class:     org_apache_harmony_awt_gl_image_PngDecoder
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_image_PngDecoder_initIDs
(JNIEnv *env, jclass cls) {
  img_PNG_imageWidthID = (*env)->GetFieldID(env, cls, "imageWidth", "I");
  img_PNG_imageHeightID = (*env)->GetFieldID(env, cls, "imageHeight", "I");
  img_PNG_colorTypeID = (*env)->GetFieldID(env, cls, "colorType", "I");
  img_PNG_bitDepthID = (*env)->GetFieldID(env, cls, "bitDepth", "I");
  img_PNG_cmapID = (*env)->GetFieldID(env, cls, "cmap", "[B");
  img_PNG_updateFromScanlineID = (*env)->GetFieldID(env, cls, "updateFromScanline", "I");
  img_PNG_numScanlinesID = (*env)->GetFieldID(env, cls, "numScanlines", "I");
  img_PNG_intOutID = (*env)->GetFieldID(env, cls, "intOut", "[I");
  img_PNG_byteOutID = (*env)->GetFieldID(env, cls, "byteOut", "[B");

  img_PNG_returnHeaderID = (*env)->GetMethodID(env, cls, "returnHeader", "()V");
}

/*
 * Class:     org_apache_harmony_awt_gl_image_PngDecoder
 * Method:    decode
 * Signature: ([BIJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_image_PngDecoder_decode
(JNIEnv *env, jobject obj, jbyteArray jInput, jint bytesInBuffer, jlong hDecoder) {  
  png_decoder_info_ptr decoderInfo = (png_decoder_info_ptr) ((IDATA)hDecoder);
  
  // Init decoder if needed
  if(!decoderInfo) {
    if(!(decoderInfo = initPng())) {
      throwNewExceptionByName(env, "java/lang/RuntimeException",
                              "Can't create native PNG decoder");
      return 0; // NULL
    }
  }
  
  if(setjmp(decoderInfo->jmpBuf)) { // Only way to deal with errors in libpng
    destroyPng(&decoderInfo);
    return 0;
  }

  // Update JNI-related fields
  decoderInfo->env = env;
  decoderInfo->obj = obj;
  
  // Get java array for image data if it is created (header is passed already)
  if(decoderInfo->colorType >= 0) {
    if(decoderInfo->colorType == PNG_COLOR_TYPE_RGB || 
       decoderInfo->colorType == PNG_COLOR_TYPE_RGB_ALPHA) {
      decoderInfo->jImageData = (*env)->GetObjectField(env, obj, img_PNG_intOutID);    
    } else {
      decoderInfo->jImageData = (*env)->GetObjectField(env, obj, img_PNG_byteOutID);    
    }
    decoderInfo->imageData = (*env)->GetPrimitiveArrayCritical(env, decoderInfo->jImageData, 0);
  }  

  // Obtain input data
  decoderInfo->jInputData = jInput;
  decoderInfo->inputBuffer = (*env)->GetPrimitiveArrayCritical(env, jInput, 0);  

  // Now process data
  processData(decoderInfo, decoderInfo->inputBuffer, bytesInBuffer);

  releaseArrays(decoderInfo);

  if(decoderInfo->doneDecoding)
    destroyPng(&decoderInfo);

  return (jlong) ((IDATA)decoderInfo);
}

/*
 * Class:     org_apache_harmony_awt_gl_image_PngDecoder
 * Method:    releaseNativeDecoder
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_image_PngDecoder_releaseNativeDecoder
(JNIEnv *env, jclass cls, jlong hDecoder) {
  png_decoder_info_ptr decoderInfo = (png_decoder_info_ptr) ((IDATA)hDecoder);
}

void releaseArrays(png_decoder_info_ptr decoderInfo) {
  JNIEnv *env = decoderInfo->env;
  jobject obj = decoderInfo->obj;

  // Release arrays
  if(decoderInfo->jInputData && decoderInfo->inputBuffer) {
    (*env)->ReleasePrimitiveArrayCritical(
      env, decoderInfo->jInputData, decoderInfo->inputBuffer, 0
    );
    decoderInfo->inputBuffer = NULL;
  }

  if(decoderInfo->jImageData && decoderInfo->imageData) {
    (*env)->ReleasePrimitiveArrayCritical(
      env, decoderInfo->jImageData, decoderInfo->imageData, 0
    );
    decoderInfo->imageData = NULL;
  }
}

void gl_error_fn(png_structp png_ptr, png_const_charp error_msg) {
  png_decoder_info_ptr decoderInfo = png_get_error_ptr(png_ptr);
  throwNewExceptionByName(decoderInfo->env, "java/lang/RuntimeException",
                          error_msg);
  
  if(decoderInfo) { // Else there's no way to terminate correctly
    longjmp(decoderInfo->jmpBuf, 1);
  }
}

void gl_warning_fn(png_structp png_ptr, png_const_charp warning_msg) {
}

void destroyPng(png_decoder_info_ptr *decoderInfoP) {
  png_decoder_info_ptr decoderInfo = *decoderInfoP;
  releaseArrays(decoderInfo);

  if(decoderInfo) {
    if(decoderInfo->png_ptr && decoderInfo->info_ptr)
      png_destroy_read_struct(&decoderInfo->png_ptr, &decoderInfo->info_ptr, NULL);

    free(decoderInfo);
    *decoderInfoP = NULL;
  }
}

void gl_info_callback(png_structp png_ptr, png_infop info) {
  JNIEnv *env;
  jobject obj;
  boolean hasTRNS = FALSE;

  // Get pointer to global info
  png_decoder_info_ptr decoderInfo = (png_decoder_info_ptr) png_get_progressive_ptr(png_ptr);

  // Read in header information
  png_get_IHDR(
    decoderInfo->png_ptr, 
    decoderInfo->info_ptr, 
    &decoderInfo->width,
    &decoderInfo->height, 
    &decoderInfo->bitDepth, 
    &decoderInfo->colorType, 
    NULL, NULL, NULL
  );
  

  png_set_interlace_handling(decoderInfo->png_ptr);
  
  if(decoderInfo->bitDepth < 8)
    png_set_packing(decoderInfo->png_ptr);

  if (decoderInfo->bitDepth == 16)
    png_set_strip_16(decoderInfo->png_ptr);

  if (decoderInfo->colorType == PNG_COLOR_TYPE_RGB)
    png_set_filler(decoderInfo->png_ptr, 0xFF, PNG_FILLER_AFTER);

  if (decoderInfo->colorType == PNG_COLOR_TYPE_GRAY_ALPHA)
    png_set_gray_to_rgb(decoderInfo->png_ptr);

  // Note that if there's tRNS tag, PLTE images will be converted to RGBA by the libpng
  if (png_get_valid(decoderInfo->png_ptr, decoderInfo->info_ptr, PNG_INFO_tRNS)) {
    png_set_tRNS_to_alpha(decoderInfo->png_ptr);
    hasTRNS = TRUE;
  }
/*
  if(decoderInfo->colorType == PNG_COLOR_TYPE_GRAY_ALPHA) {
    png_set_expand(decoderInfo->png_ptr);
  }
*/
/*
  if(png_get_bKGD(decoderInfo->png_ptr, decoderInfo->info_ptr, &imageBackground)) {
    png_set_background(
      decoderInfo->png_ptr, 
      imageBackground, 
      PNG_BACKGROUND_GAMMA_FILE, 
      1, 1.0
    );

    if (decoderInfo->colorType == PNG_COLOR_TYPE_RGBA) // It is converted to RGB, so filler needed
      png_set_filler(decoderInfo->png_ptr, 0xFF, PNG_FILLER_AFTER);
  }
*/
  png_read_update_info(decoderInfo->png_ptr, decoderInfo->info_ptr);

  // Update image parameters after all transformations are specified
  decoderInfo->rowbytes = png_get_rowbytes(decoderInfo->png_ptr, decoderInfo->info_ptr);
  decoderInfo->colorType = png_get_color_type(decoderInfo->png_ptr, decoderInfo->info_ptr);
  
  // Don't update bit depth since image data aren't changed, only expanded to 8 bpc
  // If tRNS tag presents data are really expanded so bit depth should be 8
  // If bit depth was 16 data are stripped to 8 bpc
  //decoderInfo->bitDepth = png_get_bit_depth(decoderInfo->png_ptr, decoderInfo->info_ptr);
  if(decoderInfo->bitDepth == 16 || hasTRNS)
    decoderInfo->bitDepth = 8;
  
  // Set java fields...
  env = decoderInfo->env;
  obj = decoderInfo->obj;

  // images that have width or height > 2^31 aren't supported :)
  (*env)->SetIntField(env, obj, img_PNG_imageWidthID, decoderInfo->width);
  (*env)->SetIntField(env, obj, img_PNG_imageHeightID, decoderInfo->height);
  (*env)->SetIntField(env, obj, img_PNG_bitDepthID, decoderInfo->bitDepth);
  (*env)->SetIntField(env, obj, img_PNG_colorTypeID, decoderInfo->colorType);
  
  // Update palette if needed
  if(decoderInfo->colorType == PNG_COLOR_TYPE_PALETTE) {
    int cmapLength;
    png_colorp cmap;
    jbyteArray jCmap;
    unsigned char *cmapData;

    png_get_PLTE(decoderInfo->png_ptr, decoderInfo->info_ptr, &cmap, &cmapLength);

    // Copy palette to java array
    jCmap = (*env)->NewByteArray(env, cmapLength*3);
    cmapData = (*env)->GetPrimitiveArrayCritical(env, jCmap, 0);
    memcpy(cmapData, cmap, cmapLength*3);
    (*env)->ReleasePrimitiveArrayCritical(env, jCmap, cmapData, 0);

    (*env)->SetObjectField(env, obj, img_PNG_cmapID, jCmap);
  }

  (*env)->CallVoidMethod(env, obj, img_PNG_returnHeaderID);
  if ((*env)->ExceptionCheck(env))
    longjmp(decoderInfo->jmpBuf, 1);

 
  // Get java array for image data
  if(decoderInfo->colorType == PNG_COLOR_TYPE_RGB || 
     decoderInfo->colorType == PNG_COLOR_TYPE_RGB_ALPHA) {
     // Need BGR for java color models
     png_set_bgr(decoderInfo->png_ptr);
     decoderInfo->jImageData = (*env)->GetObjectField(env, obj, img_PNG_intOutID);    
  } else {
    decoderInfo->jImageData = (*env)->GetObjectField(env, obj, img_PNG_byteOutID);    
  }
  decoderInfo->imageData = (*env)->GetPrimitiveArrayCritical(env, decoderInfo->jImageData, 0);
}

void gl_end_callback(png_structp png_ptr, png_infop info) {
  png_decoder_info_ptr decoderInfo = (png_decoder_info_ptr) png_get_progressive_ptr(png_ptr);
  decoderInfo->doneDecoding = TRUE;
}

void gl_row_callback(
    png_structp png_ptr, 
    png_bytep new_row,
    png_uint_32 row_num, 
    int pass) {
  // Get pointer to global info
  png_decoder_info_ptr decoderInfo = (png_decoder_info_ptr) png_get_progressive_ptr(png_ptr);

  png_progressive_combine_row(
    png_ptr,
    decoderInfo->imageData + decoderInfo->rowbytes*row_num, 
    new_row
  );

  if(decoderInfo->updateFromScanline < 0)
    decoderInfo->updateFromScanline = row_num;
  decoderInfo->numScanlines++;
}

png_decoder_info_ptr initPng() {
  png_decoder_info_ptr decoderInfo = calloc(sizeof(png_decoder_info), 1);
  if(!decoderInfo) {
    return NULL;
  }

  decoderInfo->png_ptr = png_create_read_struct(
    PNG_LIBPNG_VER_STRING,
    decoderInfo, 
    gl_error_fn, 
    gl_warning_fn
  );
    
  if(!decoderInfo->png_ptr) {
    return NULL;
  }

  decoderInfo->info_ptr = png_create_info_struct(decoderInfo->png_ptr);

  if (!decoderInfo->info_ptr) {
    png_destroy_read_struct(&decoderInfo->png_ptr, png_infopp_NULL, png_infopp_NULL);
    return NULL;
  }

  png_set_progressive_read_fn(
    decoderInfo->png_ptr, 
    decoderInfo,
    gl_info_callback, 
    gl_row_callback, 
    gl_end_callback
  );

  decoderInfo->colorType = -1; // Will notify us that we have not read header yet...

  return decoderInfo;
}


void processData(png_decoder_info_ptr decoderInfo, png_bytep buffer, png_uint_32 length) {
  JNIEnv *env = decoderInfo->env;
  jobject obj = decoderInfo->obj;

  decoderInfo->updateFromScanline = -1;
  decoderInfo->numScanlines = 0;

  png_process_data(decoderInfo->png_ptr, decoderInfo->info_ptr, buffer, length);
  
  // If at least one full pass was performed we need to send all the data to consumers
  if(decoderInfo->numScanlines > decoderInfo->height) {
    decoderInfo->numScanlines = decoderInfo->height;
    decoderInfo->updateFromScanline = 0;
  }
  (*env)->SetIntField(env, obj, img_PNG_updateFromScanlineID, decoderInfo->updateFromScanline);
  (*env)->SetIntField(env, obj, img_PNG_numScanlinesID, decoderInfo->numScanlines);
}
