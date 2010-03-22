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

#ifndef _Included_PNGDecoder
#define _Included_PNGDecoder

#include <stdlib.h> // Strange because it's already included in pngconf.h, but it's needed
//#include <setjmp.h>

#include "png.h"
#include "org_apache_harmony_awt_gl_image_PngDecoder.h"
#include "exceptions.h"

// Define "boolean" as int if not defined 
#ifndef __RPCNDR_H__    // don't redefine if rpcndr.h already defined it 
typedef int boolean;
#endif
#ifndef FALSE     
#define FALSE 0   
#endif
#ifndef TRUE
#define TRUE  1
#endif

// Field ids
jfieldID img_PNG_imageWidthID;
jfieldID img_PNG_imageHeightID;
jfieldID img_PNG_colorTypeID;
jfieldID img_PNG_bitDepthID;
jfieldID img_PNG_cmapID;
jfieldID img_PNG_updateFromScanlineID;
jfieldID img_PNG_numScanlinesID;
jfieldID img_PNG_intOutID;
jfieldID img_PNG_byteOutID;

jmethodID img_PNG_returnHeaderID;

typedef struct png_decoder_info_tag {
  png_structp png_ptr;
  png_infop info_ptr;
  unsigned char *inputBuffer;  

  png_uint_32 rowbytes; // Bytes for one row of data
  png_uint_32 width;
  png_uint_32 height;

  int channels;
  int bitDepth;
  int colorType;

  unsigned char *imageData;

  boolean doneDecoding;

  int updateFromScanline; // Current row
  png_uint_32 numScanlines; // How much we decoded during this call of processData

  // JNI-related vars
  JNIEnv *env;
  jobject obj;
  jarray jImageData;
  jarray jInputData;

  // Nonlocal goto - only way to deal with errors in libpng...
  jmp_buf jmpBuf;
} png_decoder_info;

typedef png_decoder_info* png_decoder_info_ptr;

void gl_error_fn(png_structp png_ptr, png_const_charp error_msg);
void gl_warning_fn(png_structp png_ptr, png_const_charp warning_msg);
void gl_info_callback(png_structp png_ptr, png_infop info);
void gl_end_callback(png_structp png_ptr, png_infop info);
void gl_row_callback(
    png_structp png_ptr, 
    png_bytep new_row,
    png_uint_32 row_num, 
    int pass);

png_decoder_info_ptr initPng();
void processData(png_decoder_info_ptr decoderInfo, png_bytep buffer, png_uint_32 length);
void destroyPng(png_decoder_info_ptr *decoderInfo);
void releaseArrays(png_decoder_info_ptr decoderInfo);

#endif
