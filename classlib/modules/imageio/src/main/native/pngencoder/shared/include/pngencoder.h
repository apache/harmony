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

#ifndef _Included_PNGEncoder
#define _Included_PNGEncoder

#include <stdlib.h> // Strange because it's already included in pngconf.h, but it's needed
//#include <setjmp.h>

#include "png.h"
#include "org_apache_harmony_x_imageio_plugins_png_PNGImageWriter.h"
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

#define IO_BUFFER_SIZE 8192L

// Field ids
jmethodID img_IOSwriteID;

typedef struct png_encoder_info_tag {
  png_structp png_ptr;
  png_infop info_ptr;
  unsigned char *inputBuffer;

  png_bytepp tmpBuffer;

  jobject ios;

  //iobuffer vars
  unsigned char ioBuffer[IO_BUFFER_SIZE];
  size_t freeBytesInIOBuffer;

  // palette  
  //png_color_8_struct *png_palette;
  jintArray jPalette;

  // JNI-related vars
  JNIEnv *env;
  jobject obj;
  jbyteArray jInputData;

  // Nonlocal goto - only way to deal with errors in libpng...
  jmp_buf jmpBuf;
} png_encoder_info;

typedef png_encoder_info* png_encoder_info_ptr;

png_encoder_info_ptr initPng();
void destroyPng(png_encoder_info_ptr *decoderInfo);
void gl_flush_data(png_structp png_ptr);

#endif //_Included_PNGEncoder
