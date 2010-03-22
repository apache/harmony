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
  * @author Viskov Nikolay
  *
  */

#include "pngencoder.h"
#include <stdio.h>

/*
 * Class:     org_apache_harmony_awt_gl_image_PngEncoder
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_imageio_plugins_png_PNGImageWriter_initIDs
(JNIEnv *env, jclass cls, jclass iosClass) {
      //-- ImageOutputStream.write(byte[], int, int)
  img_IOSwriteID = (*env)->GetMethodID(env, iosClass, "write", "([BII)V");
}

void gl_write_data(png_structp png_ptr, png_bytep data, png_size_t length) {
  png_encoder_info_ptr encoderInfo = png_get_io_ptr(png_ptr);
  
  jbyteArray java_buffer;
  unsigned char *native_buffer;

  JNIEnv *env = encoderInfo->env;

  png_size_t bufferLength = IO_BUFFER_SIZE - encoderInfo->freeBytesInIOBuffer;

  //printf("%u", length);

  if (encoderInfo->freeBytesInIOBuffer < length) {
      if (IO_BUFFER_SIZE < length) {          
          java_buffer = (*env)->NewByteArray(env, (jsize) (length + bufferLength));
          native_buffer = (unsigned char *)(*env)->GetPrimitiveArrayCritical(env, java_buffer, NULL);

          memcpy(native_buffer, encoderInfo->ioBuffer, bufferLength);
          memcpy(native_buffer + bufferLength, data, length);

          (*env)->ReleasePrimitiveArrayCritical(env, java_buffer, native_buffer, 0);
          
          (*env)->CallVoidMethod(env, encoderInfo->ios, img_IOSwriteID, java_buffer, 0, length + bufferLength);

          encoderInfo->freeBytesInIOBuffer = IO_BUFFER_SIZE;
      } else {
          gl_flush_data(encoderInfo->png_ptr);
          memcpy(encoderInfo->ioBuffer, data, length);
          encoderInfo->freeBytesInIOBuffer -= length;
      }
  } else {
      memcpy(encoderInfo->ioBuffer + bufferLength, data, length);
      encoderInfo->freeBytesInIOBuffer -= length;
  }
  //printf("passed\n");
}

void gl_flush_data(png_structp png_ptr) {
  png_encoder_info_ptr encoderInfo = png_get_io_ptr(png_ptr);
  
  jbyteArray java_buffer;
  unsigned char *native_buffer;

  png_size_t bufferLength = IO_BUFFER_SIZE - encoderInfo->freeBytesInIOBuffer;

  JNIEnv *env = encoderInfo->env;

  //printf("flush ");

  java_buffer = (*env)->NewByteArray(env, (jsize) bufferLength);
  native_buffer = (unsigned char *)(*env)->GetPrimitiveArrayCritical(env, java_buffer, NULL);
  memcpy(native_buffer, encoderInfo->ioBuffer, bufferLength);
  (*env)->ReleasePrimitiveArrayCritical(env, java_buffer, native_buffer, 0);
  
  (*env)->CallVoidMethod(env, encoderInfo->ios, img_IOSwriteID, java_buffer, 0, bufferLength);

  encoderInfo->freeBytesInIOBuffer = IO_BUFFER_SIZE;

  //printf("passed\n");
}

/*
 * Class:     org_apache_harmony_awt_gl_image_PngEncoder
 * Method:    decode
 * Signature: ([BILjava/lang/Object;IIII[IIZ)I
 */
JNIEXPORT jint JNICALL 
Java_org_apache_harmony_x_imageio_plugins_png_PNGImageWriter_encode
(JNIEnv *env, 
 jobject obj, 
 jbyteArray jInput, 
 jint bytesInBuffer, 
 jint bytePixelSize,
 jobject iosObj, 
 jint imageWidth, 
 jint imageHeight, 
 jint bitDipth, 
 jint colorType, 
 jintArray palette, 
 jint paletteSize, 
 jboolean isInterlace) {



  png_encoder_info_ptr encoderInfo;
  int i;

  if(!(encoderInfo = initPng())) {
    throwNewExceptionByName(env, "java/lang/RuntimeException",
                            "Can't create native PNG encoder");
    return 2; // NULL
  }
  
  if(setjmp(encoderInfo->jmpBuf)) { // Only way to deal with errors in libpng
    destroyPng(&encoderInfo);
    return 2;
  }

  png_set_write_fn(
      encoderInfo->png_ptr,
      encoderInfo, 
      gl_write_data,
      gl_flush_data
      );

  encoderInfo->ios = iosObj;

  // Update JNI-related fields
  encoderInfo->env = env;
  encoderInfo->obj = obj;   

  encoderInfo->jInputData = jInput;
  encoderInfo->jPalette = palette;

  png_set_IHDR(encoderInfo->png_ptr, encoderInfo->info_ptr, imageWidth, imageHeight,
      bitDipth, colorType, isInterlace ? PNG_INTERLACE_ADAM7 : PNG_INTERLACE_NONE,
       PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);

  if (colorType == PNG_COLOR_TYPE_PALETTE) {
      jint *tmpPalette = (jint *) (*env)->GetPrimitiveArrayCritical(env, palette, 0);   

      png_colorp pngPalette = (png_colorp) malloc(paletteSize * sizeof(png_color));

      //png_bytep alpha = (png_bytep) malloc(paletteSize * sizeof(png_byte)); 

      for (i = 0;  i < paletteSize;  i ++) {          
          //printf("n%u = %u\n", i, (tmpPalette[i] & 0xff000000));
          (pngPalette + i)->red =
             (png_byte) ((tmpPalette[i] & 0x00ff0000) >> 16);
          (pngPalette + i)->green =
             (png_byte) ((tmpPalette[i] & 0x0000ff00) >> 8);
          (pngPalette + i)->blue = (png_byte) tmpPalette[i] & 0x000000ff;

          //alpha[i] = 256 - (tmpPalette[i] & 0xff000000);
      }

      png_set_PLTE(encoderInfo->png_ptr, encoderInfo->info_ptr, pngPalette, paletteSize);      

      //png_set_tRNS(encoderInfo->png_ptr, encoderInfo->info_ptr, alpha, paletteSize, NULL);

      //free(alpha);
      free(pngPalette);
      (*env)->ReleasePrimitiveArrayCritical(env, palette, tmpPalette, 0); 
  }  

  encoderInfo->inputBuffer = (*env)->GetPrimitiveArrayCritical(env, jInput, 0);    

  encoderInfo->tmpBuffer = (png_bytepp) malloc(imageHeight * sizeof(png_bytep));

  for (i = 0;  i < imageHeight;  i ++) {
      encoderInfo->tmpBuffer[i] = encoderInfo->inputBuffer + (i * (imageWidth * bytePixelSize));
  }

  png_set_rows(encoderInfo->png_ptr, encoderInfo->info_ptr, encoderInfo->tmpBuffer);
  png_write_png(encoderInfo->png_ptr, encoderInfo->info_ptr, PNG_TRANSFORM_IDENTITY, NULL);  

  free(encoderInfo->tmpBuffer);
  encoderInfo->tmpBuffer = NULL;

  (*env)->ReleasePrimitiveArrayCritical(env, jInput, encoderInfo->inputBuffer, 0);    
  encoderInfo->inputBuffer = NULL;
  
  png_write_end(encoderInfo->png_ptr, encoderInfo->info_ptr);

  png_write_flush(encoderInfo->png_ptr);

  destroyPng(&encoderInfo);
  
  return 0;
}

void gl_error_fn(png_structp png_ptr, png_const_charp error_msg) {
  png_encoder_info_ptr encoderInfo = png_get_error_ptr(png_ptr);
  throwNewExceptionByName(encoderInfo->env, "java/lang/RuntimeException",
                          error_msg);
  
  if(encoderInfo) { // Else there's no way to terminate correctly
    longjmp(encoderInfo->jmpBuf, 1);
  }
}

void gl_warning_fn(png_structp png_ptr, png_const_charp warning_msg) {
}

void destroyPng(png_encoder_info_ptr *encoderInfoP) {
  png_encoder_info_ptr encoderInfo = *encoderInfoP;

  JNIEnv *env = encoderInfo->env;

  //printf("now = %u\n",(encoderInfo->inputBuffer));

  // Release arrays
  if(encoderInfo->jInputData && encoderInfo->inputBuffer) {
    (*env)->ReleasePrimitiveArrayCritical(
      env, encoderInfo->jInputData, encoderInfo->inputBuffer, 0
    );
    encoderInfo->inputBuffer = NULL;
  }

  if (encoderInfo->tmpBuffer) {
      free(encoderInfo->tmpBuffer);
  }
  encoderInfo->tmpBuffer = NULL;

  if(encoderInfo->png_ptr && encoderInfo->info_ptr)      
    png_destroy_write_struct(&encoderInfo->png_ptr, &encoderInfo->info_ptr);

  free(encoderInfo);
  encoderInfo = NULL;
}

png_encoder_info_ptr initPng() {
  png_encoder_info_ptr encoderInfo = calloc(sizeof(png_encoder_info), 1);
  if(!encoderInfo) {
    return NULL;
  }  

  encoderInfo->png_ptr = png_create_write_struct(
    PNG_LIBPNG_VER_STRING,
    encoderInfo, 
    gl_error_fn, 
    gl_warning_fn
  );
    
  if(!encoderInfo->png_ptr) {
    return NULL;
  }

  encoderInfo->info_ptr = png_create_info_struct(encoderInfo->png_ptr);

  if (!encoderInfo->info_ptr) {
    png_destroy_write_struct(&encoderInfo->png_ptr, png_infopp_NULL);
    return NULL;
  }

  encoderInfo->freeBytesInIOBuffer = IO_BUFFER_SIZE;

  return encoderInfo;
}

