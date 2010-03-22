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

#ifndef _Included_JPEGDecoder
#define _Included_JPEGDecoder

#include <string.h>
#include <stdlib.h>
#include <assert.h>

#include "org_apache_harmony_awt_gl_image_GifDecoder.h"

// Names of GifDecoder's inner classes
const char gifDataStreamClassName[] = "org/apache/harmony/awt/gl/image/GifDecoder$GifDataStream";
const char colorTableClassName[] = "org/apache/harmony/awt/gl/image/GifDecoder$GifColorTable";
const char colorTableClassSignature[] = "Lorg/apache/harmony/awt/gl/image/GifDecoder$GifColorTable;";
const char logicalScreenClassName[] = "org/apache/harmony/awt/gl/image/GifDecoder$GifLogicalScreen";
const char logicalScreenClassSignature[] = "Lorg/apache/harmony/awt/gl/image/GifDecoder$GifLogicalScreen;";
const char graphicBlockClassName[] = "org/apache/harmony/awt/gl/image/GifDecoder$GifGraphicBlock";

// Gif decoder
jfieldID img_GIF_forceRGBID;
jfieldID img_GIF_hNativeDecoderID;
jfieldID img_GIF_bytesConsumedID;
jmethodID img_GIF_setCommentID;
// Gif data stream
jfieldID img_GIF_ds_logicalScreenID;
jfieldID img_GIF_ds_loopCountID;
jfieldID img_GIF_ds_completedID;
// Gif logical screen
jfieldID img_GIF_ls_logicalScreenWidthID;
jfieldID img_GIF_ls_logicalScreenHeightID;
jfieldID img_GIF_ls_backgroundColorID;
jfieldID img_GIF_ls_globalColorTableID;
jfieldID img_GIF_ls_completedID;
// Gif color table
jfieldID img_GIF_ct_colorsID;
jfieldID img_GIF_ct_sizeID;
jfieldID img_GIF_ct_completedID;
// Gif graphic block
jfieldID img_GIF_gb_imageLeftID;
jfieldID img_GIF_gb_imageTopID;
jfieldID img_GIF_gb_imageWidthID;
jfieldID img_GIF_gb_imageHeightID;
jfieldID img_GIF_gb_disposalMethodID;
jfieldID img_GIF_gb_delayTimeID;
jfieldID img_GIF_gb_transparentColorID;
jfieldID img_GIF_gb_interlaceID;
jfieldID img_GIF_gb_imageDataID;
jfieldID img_GIF_gb_rgbImageDataID;
jfieldID img_GIF_gb_currYID;
jfieldID img_GIF_gb_completedID;

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

// Initial value for color variables. Will be never used as a color 
// since it is translucent
#define IMPOSSIBLE_VALUE 0x0FFFFFFF

// Decompressor
#define MAX_BITS 12
#define MAX_CODE 0xFFF // Max code which could be stored in 12 bits
#define IMPOSSIBLE_CODE 0xFFFFFFFF // Just to make it simplier to fill array with it
#define MAX_PASS 4 // Number of passes for interlaced images

// Sizes of data blocks
// Header + Logical screen size
#define SIZE_HEADER           6 + 7
#define SIZE_IMAGE_DESCRIPTOR 10
#define SIZE_GRAPHIC_CONTROL  5
#define SIZE_NETSCAPE_EXT     11

// Logical screen descriptor masks
#define LS_GLOBAL_COLOR_TABLE_MASK       0x80
#define LS_GLOBAL_COLOR_TABLE_SIZE_MASK  0x07
// Image descriptor masks
#define ID_LOCAL_COLOR_TABLE_MASK       0x80
#define ID_LOCAL_COLOR_TABLE_SIZE_MASK  0x07
#define ID_INTERLACE_MASK               0x40
// Graphic control masks
#define GC_TRANSPARENT_COLOR_MASK     0x01
#define GC_DISPOSAL_METHOD_MASK       0x1C
#define GC_DISPOSAL_METHOD_BIT_OFFSET 2

// Block markers
#define EXTENSION_INTRODUCER      0x21
#define APPLICATION_EXTENSION     0xFF
#define COMMENT_EXTENSION         0xFE
#define GRAPHIC_CONTROL_EXTENSION 0xF9
#define PLAIN_TEXT_EXTENSION      0x01
#define IMAGE_SEPARATOR           0x2C
#define GIF_TRAILER               0x3B


// Decoder states
typedef enum tag_DECODER_STATE {
  STATE_INIT,
  STATE_AT_LOCAL_COLOR_TABLE,
  STATE_AT_GLOBAL_COLOR_TABLE,
  STATE_BLOCK_BEGINNING,
  STATE_STARTING_DECOMPRESSION,
  STATE_DECOMPRESSING,
  STATE_READING_COMMENT,
  STATE_SKIPPING_BLOCKS
} DECODER_STATE;

typedef enum tag_GIF_RETVAL {
  STATUS_OK = 0,
  STATUS_BUFFER_EMPTY,
  STATUS_FRAME_COMPLETED,
  STATUS_LINE_COMPLETED,
  STATUS_NULL_POINTER,
  STATUS_EOF
} GIF_RETVAL;

typedef struct tag_ColorTable {
  unsigned int rgbTable[256];
  unsigned int size; // in entries
} ColorTable;

typedef struct tag_DecoderStateVars {
  // Buffer for comments specified in comment extension
  unsigned char *commentString;

  // Need this to track the case when transparent color index specified
  // but java ColorModel is already created. In this case we are enabling 
  // forceRGB flag and start transmitting int RGB data instead of indexed colors
  boolean imageDataStarted;
  boolean forceRGB;
  boolean backgroundConverted;
} DecoderStateVars;

// Masks required for reading codes from accumulated bits
unsigned const int codeMasks[] = {
        0x0000, 0x0001, 0x0003, 0x0007,
        0x000f, 0x001f, 0x003f, 0x007f,
        0x00ff, 0x01ff, 0x03ff, 0x07ff,
        0x0fff
};

// Required for interlaced images
unsigned const int startingScanline[] = {0, 4, 2, 1, -8};
unsigned const int scanlineIncrement[] = {8, 8, 4, 2, 0};

typedef struct tag_DecompressInfo {
  unsigned int accumbits;
  unsigned int shiftState;

  unsigned int bytesLeftInBlock;

  unsigned int currCodeSize; // Current code size
  unsigned int initCodeSize; // Initial code size
  // Represents max possible code which could be decoded at the moment
  unsigned int currKnownCode; 

  unsigned int currCode; // Current code
  unsigned int currPrefix; // Current prefix code
  unsigned int lastCode;
  unsigned int clearCode;
  unsigned int eoiCode;

  unsigned int maxCodeMinusOne;

  unsigned int  stack[MAX_CODE];
  unsigned int  prefix[MAX_CODE+1];
  unsigned char suffix[MAX_CODE+1];

  unsigned int stackPointer;
} DecompressInfo;

typedef struct tag_GifDecoder {  
  // Java objects
  jobject jDecoder;
  jobject jLogicalScreen; 
  jobject jGlobalColorTable;
  jobject jDataStream;
  
  jbyteArray jByteOut;
  jintArray jIntOut;

  /////////////////////////////////////////
  DECODER_STATE state;

  unsigned char *input; // input data buffer
  unsigned char *inputPtr; // Pointer to current position in buffer
  int bytesInBuffer; // Bytes left in buffer

  unsigned short logicalScreenWidth;
  unsigned short logicalScreenHeight;

  unsigned short currentWidth;
  unsigned short currentHeight;

  boolean hasGlobalColorTable;
  boolean useLocalColorTable;
  ColorTable globalColorTable;
  ColorTable localColorTable;

  unsigned char backgroundColor;

  DecoderStateVars stateVars;
  DecompressInfo decompress;

  unsigned char *outputBuffer;
  unsigned int pixelsDecoded;
  unsigned int oldPixelsDecoded;
  boolean doProcessing; // Skip any information after eoi

  // Transparent color index
  unsigned int transparentColorIndex;

  // Interlaced images
  boolean interlace;
  unsigned int increment;
  unsigned int pass;
  unsigned int scanlineOffset;
  int currScanline;
  int leftInBlock;
  boolean scanlineWritten;

  // For comment extensions
  char *comment;
  unsigned int commentLength;
} GifDecoder;

GifDecoder* getDecoder(JNIEnv *env, jobject obj, jobject dataStream, GifDecoder* decoder);
GIF_RETVAL readHeader(JNIEnv *env, GifDecoder *decoder);
GIF_RETVAL loadColorTable(JNIEnv *env, jobject jColorTable, GifDecoder *decoder);
GIF_RETVAL skipData(GifDecoder *decoder);
GIF_RETVAL readComment(JNIEnv *env, GifDecoder *decoder);
GIF_RETVAL readExtension(JNIEnv *env, GifDecoder *decoder, jobject currBlock);
GIF_RETVAL readImageDescriptor(JNIEnv *env, jobject currBlock, GifDecoder *decoder);
void resetDecompressorState(GifDecoder *decoder);
GIF_RETVAL initDecompression(JNIEnv *env, GifDecoder *decoder, jobject currBlock);
GIF_RETVAL decompress(JNIEnv *env, jobject currBlock, GifDecoder *decoder);
void readGraphicControl(JNIEnv *env, GifDecoder *decoder, jobject currBlock);
void convertBackgroundColor(JNIEnv *env, GifDecoder *decoder);
void initInterlaceVars(GifDecoder *decoder);
GIF_RETVAL decompressInterlaced(JNIEnv *env, jobject currBlock, GifDecoder *decoder);
void sendCommentToJava(JNIEnv *env, GifDecoder *decoder);
#endif
