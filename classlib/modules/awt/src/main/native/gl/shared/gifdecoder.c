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

#include "gifdecoder.h"
#include "hycomp.h"

/*
 * Class:     org_apache_harmony_awt_gl_image_GifDecoder
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_image_GifDecoder_initIDs
(JNIEnv *env, jclass cls) {
  jclass gifDataStreamClass = (*env)->FindClass(env, gifDataStreamClassName);
  jclass colorTableClass = (*env)->FindClass(env, colorTableClassName);
  jclass logicalScreenClass = (*env)->FindClass(env, logicalScreenClassName);
  jclass graphicBlockClass = (*env)->FindClass(env, graphicBlockClassName);

  // Gif decoder
  img_GIF_forceRGBID = (*env)->GetFieldID(env, cls, "forceRGB", "Z");
  img_GIF_hNativeDecoderID = (*env)->GetFieldID(env, cls, "hNativeDecoder", "J");
  img_GIF_bytesConsumedID = (*env)->GetFieldID(env, cls, "bytesConsumed", "I");

  img_GIF_setCommentID = (*env)->GetMethodID(env, cls, "setComment", "(Ljava/lang/String;)V");
  // Gif data stream
  img_GIF_ds_logicalScreenID = (*env)->GetFieldID(env, gifDataStreamClass, "logicalScreen", logicalScreenClassSignature);
  img_GIF_ds_loopCountID = (*env)->GetFieldID(env, gifDataStreamClass, "loopCount", "I");
  img_GIF_ds_completedID = (*env)->GetFieldID(env, gifDataStreamClass, "completed", "Z");
  // Gif logical screen
  img_GIF_ls_logicalScreenWidthID = (*env)->GetFieldID(env, logicalScreenClass, "logicalScreenWidth", "I");
  img_GIF_ls_logicalScreenHeightID = (*env)->GetFieldID(env, logicalScreenClass, "logicalScreenHeight", "I");
  img_GIF_ls_backgroundColorID = (*env)->GetFieldID(env, logicalScreenClass, "backgroundColor", "I");
  img_GIF_ls_globalColorTableID = (*env)->GetFieldID(env, logicalScreenClass, "globalColorTable", colorTableClassSignature);
  img_GIF_ls_completedID = (*env)->GetFieldID(env, logicalScreenClass, "completed", "Z");
  // Gif color table
  img_GIF_ct_colorsID = (*env)->GetFieldID(env, colorTableClass, "colors", "[B");
  img_GIF_ct_sizeID = (*env)->GetFieldID(env, colorTableClass, "size", "I");
  img_GIF_ct_completedID = (*env)->GetFieldID(env, colorTableClass, "completed", "Z");
  // Gif graphic block
  img_GIF_gb_imageLeftID = (*env)->GetFieldID(env, graphicBlockClass, "imageLeft", "I");
  img_GIF_gb_imageTopID = (*env)->GetFieldID(env, graphicBlockClass, "imageTop", "I");
  img_GIF_gb_imageWidthID = (*env)->GetFieldID(env, graphicBlockClass, "imageWidth", "I");
  img_GIF_gb_imageHeightID = (*env)->GetFieldID(env, graphicBlockClass, "imageHeight", "I");
  img_GIF_gb_disposalMethodID = (*env)->GetFieldID(env, graphicBlockClass, "disposalMethod", "I");
  img_GIF_gb_delayTimeID = (*env)->GetFieldID(env, graphicBlockClass, "delayTime", "I");
  img_GIF_gb_transparentColorID = (*env)->GetFieldID(env, graphicBlockClass, "transparentColor", "I");
  img_GIF_gb_interlaceID = (*env)->GetFieldID(env, graphicBlockClass, "interlace", "Z");
  img_GIF_gb_imageDataID = (*env)->GetFieldID(env, graphicBlockClass, "imageData", "[B");
  img_GIF_gb_rgbImageDataID = (*env)->GetFieldID(env, graphicBlockClass, "rgbImageData", "[I");
  img_GIF_gb_currYID = (*env)->GetFieldID(env, graphicBlockClass, "currY", "I");
  img_GIF_gb_completedID = (*env)->GetFieldID(env, graphicBlockClass, "completed", "Z");
}

/*
 * Class:     org_apache_harmony_awt_gl_image_GifDecoder
 * Method:    releaseNativeDecoder
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_image_GifDecoder_releaseNativeDecoder
(JNIEnv *env, jclass cls, jlong hDecoder) {
  // Cleanup if image was truncated
  if(hDecoder)
    free((GifDecoder *) ((IDATA)hDecoder));
}

/*
 * Class:     org_apache_harmony_awt_gl_image_GifDecoder
 * Method:    decode
 * Signature: ([BIJLorg/apache/harmony/awt/gl/image/GifDecoder$GifDataStream;Lorg/apache/harmony/awt/gl/image/GifDecoder$GifGraphicBlock;)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_awt_gl_image_GifDecoder_decode
(JNIEnv *env, 
 jobject obj, 
 jbyteArray jInput, 
 jint bytesInBuffer, 
 jlong hDecoder, 
 jobject dataStream, 
 jobject currBlock) {

  GIF_RETVAL retval = STATUS_OK;
  GifDecoder *decoder = getDecoder(env, obj, dataStream, (GifDecoder*) ((IDATA)hDecoder));
  int scanlinesDecoded;    

  decoder->input = decoder->inputPtr = 
    (*env)->GetPrimitiveArrayCritical(env, jInput, 0);
  decoder->bytesInBuffer += bytesInBuffer;
  bytesInBuffer = decoder->bytesInBuffer;

  while(retval == STATUS_OK && decoder->bytesInBuffer > 0) {
    switch(decoder->state) {
      case STATE_INIT: {
        retval = readHeader(env, decoder);
        break;
      }

      case STATE_AT_GLOBAL_COLOR_TABLE: {
        retval = loadColorTable(env, decoder->jGlobalColorTable, decoder);
        break;
      }

      case STATE_AT_LOCAL_COLOR_TABLE: {
        retval = loadColorTable(env, NULL, decoder);
        break;
      }

      case STATE_BLOCK_BEGINNING: {
        unsigned char blockLabel = *(decoder->inputPtr);
        switch(blockLabel) {
          case EXTENSION_INTRODUCER:
            retval = readExtension(env, decoder, currBlock);
            break;
          case IMAGE_SEPARATOR:
            retval = readImageDescriptor(env, currBlock, decoder);
            break;
          case GIF_TRAILER:
            retval = STATUS_EOF;
            break;
        }
        break;
      }

      case STATE_STARTING_DECOMPRESSION: {
        retval = initDecompression(env, decoder, currBlock);
        break;
      }

      case STATE_DECOMPRESSING: {
        if(!decoder->interlace)
          retval = decompress(env, currBlock, decoder);
        else
          retval = decompressInterlaced(env, currBlock, decoder);
        break;
      }

      case STATE_READING_COMMENT:{
        retval = readComment(env, decoder);
        break;
      }

      case STATE_SKIPPING_BLOCKS: {
        retval = skipData(decoder);
        break;
      }

      default:
        // Should never execute this!
        break;
    }
  }
  
  // Copy unconsumed data to the start of the input buffer
  if(decoder->bytesInBuffer > 0) {
    memmove(decoder->input, decoder->inputPtr, decoder->bytesInBuffer);
  }

  (*env)->ReleasePrimitiveArrayCritical(env, jInput, decoder->input, 0);

  (*env)->SetIntField(
      env, 
      obj, 
      img_GIF_bytesConsumedID, 
      bytesInBuffer - decoder->bytesInBuffer
    );

  if(decoder->stateVars.imageDataStarted) {
    if(!decoder->interlace) {
      scanlinesDecoded = decoder->pixelsDecoded / decoder->currentWidth -
        decoder->oldPixelsDecoded / decoder->currentWidth;
      decoder->oldPixelsDecoded = decoder->pixelsDecoded;
    } else {
      if(retval == STATUS_LINE_COMPLETED && decoder->pass < MAX_PASS) {
        scanlinesDecoded = 1;
        if(decoder->currScanline >= 0)
          (*env)->SetIntField(env, currBlock, img_GIF_gb_currYID, decoder->currScanline);

        decoder->scanlineOffset = 0;
      } else {
        scanlinesDecoded = 0;
      }
    }
  } else {
    scanlinesDecoded = 0;
  }
  
  if(retval == STATUS_FRAME_COMPLETED) {
    decoder->oldPixelsDecoded = decoder->pixelsDecoded = 0;
  }

  // Free the decoder if decoding is finished
  if(retval == STATUS_EOF) {
    free(decoder);
    decoder = NULL;    
  }

  (*env)->SetLongField(env, obj, img_GIF_hNativeDecoderID, (jlong) ((IDATA)decoder));

  return scanlinesDecoded;
}

// Utility function to perform fast buffer format conversion
static void toIntARGB(unsigned char *in, unsigned int *out, int lengthInPixels) {
  // Go in backward direction
  unsigned char *inCurrPtr = in + lengthInPixels*3 - 1; // end of the input buffer

  // end of the output buffer
  unsigned int *outCurrPtr = out + lengthInPixels - 1;

  unsigned int val;

  while (inCurrPtr > in) {
    val = *(inCurrPtr--);           // B
    val |= (*(inCurrPtr--))<<8;   // G
    val |= (*(inCurrPtr--))<<16;  // R
    val |= 0xFF000000; // Alpha = 1
    *(outCurrPtr--) = val;
  }
}

/*
 * Class:     org_apache_harmony_awt_gl_image_GifDecoder
 * Method:    toRGB
 * Signature: ([B[BI)[I
 */
JNIEXPORT jintArray JNICALL Java_org_apache_harmony_awt_gl_image_GifDecoder_toRGB
(JNIEnv *env, jclass cls, jbyteArray jSrc, jbyteArray jColormap, jint transparentColor) {
  unsigned int numPixels = (*env)->GetArrayLength(env, jSrc);
  unsigned int cmapSize = (*env)->GetArrayLength(env, jColormap);
  // Create INT_ARGB colormap
  unsigned int *intARGBColormap = malloc(cmapSize*sizeof(int));
  jintArray jDst = (*env)->NewIntArray(env, numPixels);
  unsigned char *src = (*env)->GetPrimitiveArrayCritical(env, jSrc, 0);
  unsigned char *colormap = (*env)->GetPrimitiveArrayCritical(env, jColormap, 0);
  unsigned int *dst = (*env)->GetPrimitiveArrayCritical(env, jDst, 0);
  unsigned int *dstPtr = dst;
  unsigned char *srcPtr = src;

  // Fill it
  toIntARGB(colormap, intARGBColormap, cmapSize);

  if(transparentColor != IMPOSSIBLE_VALUE)
    intARGBColormap[transparentColor] &= 0x00FFFFFF;

  // Convert pixels
  while(numPixels--) {
    *(dstPtr++) = intARGBColormap[*(srcPtr++)];
  }

  (*env)->ReleasePrimitiveArrayCritical(env, jDst, dst, 0);
  (*env)->ReleasePrimitiveArrayCritical(env, jSrc, src, JNI_ABORT); /* We does not change this array */
  (*env)->ReleasePrimitiveArrayCritical(env, jColormap, colormap, JNI_ABORT); /* We does not change this array */
  
  free(intARGBColormap);  

  return jDst;
}

GifDecoder* getDecoder(JNIEnv *env, jobject obj, jobject dataStream, GifDecoder* decoder) {
  if(!decoder) {
    decoder = malloc(sizeof(GifDecoder));

    decoder->stateVars.commentString = NULL;
    decoder->stateVars.forceRGB = FALSE;
    decoder->stateVars.imageDataStarted = FALSE;
    decoder->state = STATE_INIT;
    decoder->bytesInBuffer = 0;

    decoder->transparentColorIndex = IMPOSSIBLE_VALUE;
    decoder->useLocalColorTable = FALSE;
    decoder->stateVars.backgroundConverted = FALSE;

    decoder->comment = NULL;
    decoder->commentLength = 0;

    (*env)->SetLongField(env, obj, img_GIF_hNativeDecoderID, 0);
  }

  decoder->jDataStream = dataStream;
  decoder->jDecoder = obj;

  return decoder;
}

GIF_RETVAL readHeader(JNIEnv *env, GifDecoder *decoder) {
  if(decoder->bytesInBuffer >= SIZE_HEADER) {
    unsigned char colorTableSize;
    unsigned char flags;

    decoder->jLogicalScreen = (*env)->GetObjectField(env, decoder->jDataStream, img_GIF_ds_logicalScreenID);
    decoder->jGlobalColorTable = (*env)->GetObjectField(env, decoder->jLogicalScreen, img_GIF_ls_globalColorTableID);

    decoder->inputPtr += 6; // Skip header - already checked in java
    
    // Get width/height
    decoder->logicalScreenWidth = *((unsigned short *) decoder->inputPtr);
    decoder->inputPtr += 2;
    (*env)->SetIntField(env, decoder->jLogicalScreen, img_GIF_ls_logicalScreenWidthID, decoder->logicalScreenWidth);

    decoder->logicalScreenHeight = *((unsigned short *) decoder->inputPtr);
    decoder->inputPtr += 2;
    (*env)->SetIntField(env, decoder->jLogicalScreen, img_GIF_ls_logicalScreenHeightID, decoder->logicalScreenHeight);
    
    // Get flags
    flags = *(decoder->inputPtr++);
    decoder->hasGlobalColorTable = flags & LS_GLOBAL_COLOR_TABLE_MASK;
    
    if(decoder->hasGlobalColorTable) {
      colorTableSize = flags & LS_GLOBAL_COLOR_TABLE_SIZE_MASK;
      decoder->globalColorTable.size = 1 << (colorTableSize+1); // 2^(CR + 1)
      (*env)->SetIntField(env, decoder->jGlobalColorTable, img_GIF_ct_sizeID, decoder->globalColorTable.size);

      // Get the background color
      decoder->backgroundColor = *(decoder->inputPtr++);
      (*env)->SetIntField(env, decoder->jLogicalScreen, img_GIF_ls_backgroundColorID, decoder->backgroundColor);

    } else { // Skip background color
      decoder->backgroundColor = 0;  
      decoder->inputPtr++;
    }

    decoder->inputPtr++; // Skip pixel aspect ratio

    decoder->bytesInBuffer -= SIZE_HEADER;

    // Logical screen is loaded
    // Set completed flag in java
    (*env)->SetBooleanField(env, decoder->jLogicalScreen, img_GIF_ls_completedID, TRUE);

    if(decoder->hasGlobalColorTable) {
      decoder->state = STATE_AT_GLOBAL_COLOR_TABLE;
    } else { // No global color table, force RGB, set completed flag in java
      (*env)->SetBooleanField(env, decoder->jGlobalColorTable, img_GIF_ct_completedID, TRUE);
      
      decoder->stateVars.forceRGB = TRUE;
      (*env)->SetBooleanField(env, decoder->jDecoder, img_GIF_forceRGBID, TRUE);

      decoder->state = STATE_BLOCK_BEGINNING;
    }

    return STATUS_OK;
  }

  return STATUS_BUFFER_EMPTY;
}

GIF_RETVAL loadColorTable(JNIEnv *env, jobject jColorTable, GifDecoder *decoder) {
  int tableSizeInBytes, tableSize;
  jbyte *colors;
  jbyteArray jColors;
  ColorTable *cTable;
  boolean global;

  if(jColorTable != NULL) { // Loading a global color table
    cTable = &decoder->globalColorTable;
    global = TRUE;
  } else { // Loading a local color table
    cTable = &decoder->localColorTable;
    global = FALSE;
  }

  tableSize = cTable->size;
  tableSizeInBytes = tableSize * 3;

  if(decoder->bytesInBuffer < tableSizeInBytes)
    return STATUS_BUFFER_EMPTY; // Not enough bytes in buffer
    
  if(jColorTable != NULL) {
    jColors = (*env)->GetObjectField(env, jColorTable, img_GIF_ct_colorsID);
    
    colors = (*env)->GetPrimitiveArrayCritical(env, jColors, 0);
    memcpy(colors, decoder->inputPtr, tableSizeInBytes);
    (*env)->ReleasePrimitiveArrayCritical(env, jColors, colors, 0);

    (*env)->SetBooleanField(env, jColorTable, img_GIF_ct_completedID, TRUE);
  }
  
  // Convert to int RGB format always. Could be optimized easily if it worth it.    
  toIntARGB(decoder->inputPtr, cTable->rgbTable, tableSize);

  decoder->bytesInBuffer -= tableSizeInBytes;
  decoder->inputPtr += tableSizeInBytes;

  if(global)
    decoder->state = STATE_BLOCK_BEGINNING;
  else {
    decoder->stateVars.imageDataStarted = TRUE; // OK to set it here - already passed GCE
    decoder->state = STATE_STARTING_DECOMPRESSION;
  }

  return STATUS_OK;
}

/*
  This function skips sequence of sub-blocks with input data organized according
  to GIF spec. Required for skipping unrecognized extensions.
*/
GIF_RETVAL skipData(GifDecoder *decoder) {
  int dataSize;
  while((dataSize = *(decoder->inputPtr))) {  
    if(dataSize + 1 > decoder->bytesInBuffer) // Need to load more data, suspending
      return STATUS_BUFFER_EMPTY;
    
    // Advance to next data block
    decoder->bytesInBuffer -= dataSize + 1; // One byte is for size of block itself
    decoder->inputPtr += dataSize + 1;
  }

  decoder->inputPtr++; // Skip zero size block
  decoder->bytesInBuffer--;

  decoder->state = STATE_BLOCK_BEGINNING;
  return STATUS_OK;
}

/*
  This function saves comments from the sequence of sub-blocks organized according
  to GIF spec into the character string.
*/
GIF_RETVAL readComment(JNIEnv *env, GifDecoder *decoder) {
  int dataSize;
  while((dataSize = *(decoder->inputPtr))) {  
    if(dataSize + 1 > decoder->bytesInBuffer) // Need to load more data, suspending
      return STATUS_BUFFER_EMPTY;
    
    decoder->inputPtr++; // Skip size of the block
    decoder->commentLength += dataSize;

    decoder->comment = realloc(decoder->comment, decoder->commentLength);
    memcpy(
      decoder->comment + decoder->commentLength - dataSize, 
      decoder->inputPtr, 
      dataSize
    );

    // Advance to next data block    
    decoder->bytesInBuffer -= dataSize + 1; // One byte is for size of block itself
    decoder->inputPtr += dataSize;
  }

  decoder->inputPtr++; // Skip zero size block
  decoder->bytesInBuffer--;

  sendCommentToJava(env, decoder);

  decoder->state = STATE_BLOCK_BEGINNING;
  return STATUS_OK;
}

void readGraphicControl(JNIEnv *env, GifDecoder *decoder, jobject currBlock) {
  unsigned char transparentColorIndex;
  int disposalMethod;  
  boolean transparentColorFlag = FALSE;

  unsigned char flags = *(decoder->inputPtr++); // Read packed fields

  unsigned int delayTime = (*((unsigned short *)decoder->inputPtr))*10;
  decoder->inputPtr += 2;
  
  transparentColorIndex = *(decoder->inputPtr);
  decoder->inputPtr += 2; // Skip block terminator also;

  disposalMethod = (flags & GC_DISPOSAL_METHOD_MASK) >> GC_DISPOSAL_METHOD_BIT_OFFSET;
  transparentColorFlag = flags & GC_TRANSPARENT_COLOR_MASK;
  
  (*env)->SetIntField(env, currBlock, img_GIF_gb_delayTimeID, delayTime);
  (*env)->SetIntField(env, currBlock, img_GIF_gb_disposalMethodID, disposalMethod);
  
  // Postpone setting of transparent color in java until state STATE_STARTING_DECOMPRESSION
  // since we don't know if local color table presents
  if(transparentColorFlag) { 
    decoder->transparentColorIndex = transparentColorIndex;
    if(decoder->stateVars.imageDataStarted) {
      (*env)->SetBooleanField(env, decoder->jDecoder, img_GIF_forceRGBID, TRUE);
      decoder->stateVars.forceRGB = TRUE;
    }
  } else {
    decoder->transparentColorIndex = IMPOSSIBLE_VALUE;
  }

  decoder->bytesInBuffer -= SIZE_GRAPHIC_CONTROL;
}

GIF_RETVAL readExtension(JNIEnv *env, GifDecoder *decoder, jobject currBlock) {
  unsigned char extensionSize = *(decoder->inputPtr+2);
  unsigned char extensionType;
  
  if(decoder->bytesInBuffer < extensionSize + 3) { // Stop, we need more data
    return STATUS_BUFFER_EMPTY;
  }

  decoder->inputPtr++; // Skip extension introducer

  extensionType = *(decoder->inputPtr);  
  decoder->inputPtr += 2; // Skip size, already got it
  decoder->bytesInBuffer -= 3; // Decrease buffer size as we advanced

  switch(extensionType) {
    case GRAPHIC_CONTROL_EXTENSION:
      readGraphicControl(env, decoder, currBlock);
      break;

    case COMMENT_EXTENSION: {
      decoder->inputPtr -= 1; // No extension size, usual sub-blocks, go back
      decoder->bytesInBuffer += 1;
      decoder->state = STATE_READING_COMMENT;
      return readComment(env, decoder);
    }

    case APPLICATION_EXTENSION:
      if(extensionSize == SIZE_NETSCAPE_EXT && 
         !strncmp(decoder->inputPtr, "NETSCAPE2.0", SIZE_NETSCAPE_EXT)) {        
        if(*(decoder->inputPtr + extensionSize) == 3) { // Magic size of the sub-block in netscape ext
          unsigned short loopCount;
          decoder->inputPtr += extensionSize;
          decoder->bytesInBuffer -= extensionSize;
          loopCount = *((unsigned short *) (decoder->inputPtr+2));
          (*env)->SetIntField(env, decoder->jDataStream, img_GIF_ds_loopCountID, loopCount);
          return skipData(decoder);
        } // If the extension is invalid proceed to default
      }

    case PLAIN_TEXT_EXTENSION:
    default:
      decoder->inputPtr += extensionSize;
      decoder->bytesInBuffer -= extensionSize;
      if(!(*(decoder->inputPtr))) { // No additional data
        decoder->inputPtr++;
        decoder->bytesInBuffer--;
        decoder->state = STATE_BLOCK_BEGINNING;
      } else {
        decoder->state = STATE_SKIPPING_BLOCKS;
        return skipData(decoder);
      }
  }

  return STATUS_OK;
}

GIF_RETVAL readImageDescriptor(JNIEnv *env, jobject currBlock, GifDecoder *decoder) {
  unsigned short *tmp;
  unsigned char flags;
  boolean interlace;

  if(decoder->bytesInBuffer < SIZE_IMAGE_DESCRIPTOR)
    return STATUS_BUFFER_EMPTY;

  decoder->inputPtr++; // Skip image separator

  tmp = (unsigned short *) decoder->inputPtr;

  (*env)->SetIntField(env, currBlock, img_GIF_gb_imageLeftID, *(tmp++));
  (*env)->SetIntField(env, currBlock, img_GIF_gb_imageTopID, *(tmp++));
  decoder->currentWidth = *(tmp++);
  (*env)->SetIntField(env, currBlock, img_GIF_gb_imageWidthID, decoder->currentWidth);
  decoder->currentHeight = *(tmp++);
  (*env)->SetIntField(env, currBlock, img_GIF_gb_imageHeightID, decoder->currentHeight);

  decoder->inputPtr += 8;
  flags = *(decoder->inputPtr++);

  decoder->bytesInBuffer -= SIZE_IMAGE_DESCRIPTOR; // Image descriptor passed

  interlace = (flags & ID_INTERLACE_MASK) ? 1 : 0;
  (*env)->SetBooleanField(env, currBlock, img_GIF_gb_interlaceID, interlace);
  decoder->interlace = interlace;

  if(flags & ID_LOCAL_COLOR_TABLE_MASK) { // Local color table present
    decoder->stateVars.forceRGB = TRUE;
    (*env)->SetBooleanField(env, decoder->jDecoder, img_GIF_forceRGBID, TRUE);

    decoder->localColorTable.size = 1 << ((flags & ID_LOCAL_COLOR_TABLE_SIZE_MASK) + 1);
    decoder->useLocalColorTable = TRUE;
    decoder->state = STATE_AT_LOCAL_COLOR_TABLE;
  } else {
    decoder->useLocalColorTable = FALSE;
    decoder->stateVars.imageDataStarted = TRUE; // OK to set it here - already passed GCE
    decoder->state = STATE_STARTING_DECOMPRESSION;
  }

  // Greate image data array in java
  if(decoder->stateVars.forceRGB) {
    decoder->jIntOut = (*env)->NewIntArray(env, decoder->currentWidth * decoder->currentHeight);
    (*env)->SetObjectField(env, currBlock, img_GIF_gb_rgbImageDataID, decoder->jIntOut);
  } else {
    decoder->jByteOut = (*env)->NewByteArray(env, decoder->currentWidth * decoder->currentHeight);
    (*env)->SetObjectField(env, currBlock, img_GIF_gb_imageDataID, decoder->jByteOut);
  }

  return STATUS_OK;
}

void resetDecompressorState(GifDecoder *decoder) {
  decoder->decompress.currCodeSize = decoder->decompress.initCodeSize;
  decoder->decompress.maxCodeMinusOne = (1 << decoder->decompress.initCodeSize) - 2;
  // This is a running code, it will be incremented by 2 before actual processing,
  // so we make it here equal to "FIRST AVAILABLE CODE" - 2 which is clear code
  decoder->decompress.currKnownCode = decoder->decompress.clearCode; 
  decoder->decompress.lastCode = IMPOSSIBLE_CODE;

  // Fill prefix with IMPOSSIBLE_CODE
  memset(decoder->decompress.prefix, 0xFF, (MAX_CODE+1)*sizeof(unsigned int)); 
}

void removeTransparencyFromTables(GifDecoder *decoder) {
  int i;
  for(i=0; i<255; i++) {
    decoder->localColorTable.rgbTable[i] |= 0xFF000000;
    decoder->globalColorTable.rgbTable[i] |= 0xFF000000;
  }
}

GIF_RETVAL initDecompression(JNIEnv *env, GifDecoder *decoder, jobject currBlock) {
  DecompressInfo *di = &decoder->decompress;
  unsigned int transparentColor; 

  // Now we know all color tables info, so it's possible to set transparent color in java
  if(decoder->stateVars.forceRGB) {
    // If we switched in ARGB mode we need to have ARGB background
    convertBackgroundColor(env, decoder);

    // Fix the value in the color table - make it transparent
    removeTransparencyFromTables(decoder);
    if(decoder->transparentColorIndex != IMPOSSIBLE_VALUE) {
      decoder->localColorTable.rgbTable[decoder->transparentColorIndex]  &= 0x00FFFFFF;
      decoder->globalColorTable.rgbTable[decoder->transparentColorIndex] &= 0x00FFFFFF;

      transparentColor = decoder->useLocalColorTable ? 
        decoder->localColorTable.rgbTable[decoder->transparentColorIndex] :
        decoder->globalColorTable.rgbTable[decoder->transparentColorIndex];
    } else {
      transparentColor = IMPOSSIBLE_VALUE;
    }
  } else {
    transparentColor = decoder->transparentColorIndex;
  }
  (*env)->SetIntField(env, currBlock, img_GIF_gb_transparentColorID, transparentColor);


  decoder->outputBuffer = NULL;
  
  di->shiftState = 0;
  decoder->pixelsDecoded = 0;
  decoder->oldPixelsDecoded = 0;

  di->initCodeSize = *(decoder->inputPtr++) + 1;
  decoder->bytesInBuffer--;

  di->clearCode = 1 << (di->initCodeSize - 1);
  di->eoiCode = di->clearCode + 1;
  //di->currCode = di->clearCode + 2;
  
  resetDecompressorState(decoder);

  di->stackPointer = 0;
  di->accumbits = 0;

  decoder->doProcessing = TRUE;
  decoder->state = STATE_DECOMPRESSING;

  // Init variables needed to support interlacing
  if(decoder->interlace)
    initInterlaceVars(decoder);

  return STATUS_OK;
}

void intFromIndexModel(
    unsigned char *in, 
    unsigned int *out, 
    int lengthInPixels, 
    GifDecoder *decoder
  ) {

  unsigned int *rgbTable;
  int i;

  if(lengthInPixels == 0) // Nothing to do
    return;

  if(decoder->useLocalColorTable) {
    rgbTable = decoder->localColorTable.rgbTable;
  } else {
    rgbTable = decoder->globalColorTable.rgbTable;
  }

  for(i = 0; i < lengthInPixels; i++) {
    out[i] = rgbTable[in[i]];
  }
} 

unsigned int getPrefix(unsigned int code, DecompressInfo *di) {
  int i = MAX_CODE; // Limits the number of iterations in case if something wrong
  while (code > di->clearCode && i--) 
    code = di->prefix[code];

  return code;
}

void pushStack(DecompressInfo *di) {
  int i = MAX_CODE; // Limits the number of iterations in case if something wrong
  
  while(di->currPrefix > di->clearCode && di->currPrefix < MAX_CODE && i--) {
    di->stack[di->stackPointer++] = di->suffix[di->currPrefix];
    di->currPrefix = di->prefix[di->currPrefix];
  }

  di->stack[di->stackPointer++] = di->currPrefix;
}

GIF_RETVAL decompress(JNIEnv *env, jobject currBlock, GifDecoder *decoder) {
  unsigned char *output, *outPtr;
  unsigned int i = 0; // Counter for decoded pixels
  GIF_RETVAL retval = STATUS_OK;
  
  if(decoder->stateVars.forceRGB) {
    output = malloc(decoder->currentHeight*decoder->currentWidth);
  } else {
    decoder->jByteOut = (*env)->GetObjectField(env, currBlock, img_GIF_gb_imageDataID);
    output = (*env)->GetPrimitiveArrayCritical(env, decoder->jByteOut, 0);
  }

  outPtr = output + decoder->pixelsDecoded;

  for(;;) {
    int savedBlockSize = *(decoder->inputPtr);    
    int blockSize = savedBlockSize; 
    DecompressInfo *di = &decoder->decompress;

    if(decoder->bytesInBuffer < savedBlockSize + 1) { // Cannot read next block
      retval = STATUS_BUFFER_EMPTY;
      break;
    }

    decoder->inputPtr++;
    decoder->bytesInBuffer--;
    
    if(savedBlockSize == 0) { // Frame completed
      decoder->state = STATE_BLOCK_BEGINNING;
      (*env)->SetBooleanField(env, currBlock, img_GIF_gb_completedID, TRUE);
      // Pass control to java code - need to call imageComplete and create new GifGraphicBlock
      retval = STATUS_FRAME_COMPLETED; 
      break;
    }

    if(decoder->doProcessing) {
      unsigned char *savedPtr = decoder->inputPtr;
      for(;;) {
        if(di->shiftState < di->currCodeSize) {
          // Need to read at least one more byte
          di->accumbits |= (*(decoder->inputPtr++) << di->shiftState);
          di->shiftState += 8;
          blockSize--;
        } else {
          di->currCode = di->accumbits & codeMasks[di->currCodeSize];
          di->accumbits >>= di->currCodeSize;
          di->shiftState -= di->currCodeSize;

          // Check if we need to increase code size          
          if(++di->currKnownCode > di->maxCodeMinusOne && di->currCodeSize < MAX_BITS) {
            unsigned int maxCodePlusOne = di->maxCodeMinusOne + 2;
            di->currCodeSize++;
            di->maxCodeMinusOne = (maxCodePlusOne << 1) - 2;
          }

          // Now process the obtained code
          if (di->currCode == di->eoiCode) { // EOI encountered, stop processing
            decoder->inputPtr += blockSize;
            // Ensure that no more data will be decoded
            di->shiftState = 0;
            blockSize = 0;
            decoder->doProcessing = FALSE;
          } else if(di->currCode == di->clearCode) { // Clear code - reset code size
            resetDecompressorState(decoder);
          } else { // Regular code
            if(di->currCode < di->clearCode) { // It is just a scalar pixel
              outPtr[i++] = di->currCode;
            } else { // Search for prefix for this code 
              // Code not added yet, corresponding prefix is the last code, 
              // suffix is the first character of the last code
              if(di->prefix[di->currCode] == IMPOSSIBLE_CODE) {
                di->currPrefix = di->lastCode;
                di->suffix[di->currKnownCode] = 
                  di->stack[di->stackPointer++] = getPrefix(di->lastCode, di);
              } else { // Set prefix to current code
                di->currPrefix = di->currCode;
              }
              
              pushStack(di);
                
              // Now pop the stack to get the output
              while (di->stackPointer)
                outPtr[i++] = di->stack[--(di->stackPointer)];
            }

            if(di->lastCode != IMPOSSIBLE_CODE) {
              // Set prefix to last code and update suffix
              di->prefix[di->currKnownCode] = di->lastCode;

              // Suffix depends on wether it is a special case or not
              if(di->currCode == di->currKnownCode) {
                di->suffix[di->currKnownCode] = getPrefix(di->lastCode, di);
              } else {
                di->suffix[di->currKnownCode] = getPrefix(di->currCode, di);
              }
            }

            // Save the last code
            di->lastCode = di->currCode;
          }
        }

        // Break if no more bytes to load in this block
        if(!blockSize && di->shiftState < di->currCodeSize) 
          break;
      } // while

    } else {
      decoder->inputPtr += savedBlockSize;
    }
    
    decoder->bytesInBuffer -= savedBlockSize;
  }

  decoder->pixelsDecoded = decoder->oldPixelsDecoded + i;

  if(decoder->stateVars.forceRGB) { // Need to convert to RGB from index color model
    unsigned int *intOut, *intSrcPtr;
    decoder->jIntOut = (*env)->GetObjectField(env, currBlock, img_GIF_gb_rgbImageDataID);
    intOut = (*env)->GetPrimitiveArrayCritical(env, decoder->jIntOut, 0);
    intSrcPtr = intOut + decoder->oldPixelsDecoded;

    intFromIndexModel(
      outPtr, 
      intSrcPtr, 
      decoder->pixelsDecoded - decoder->oldPixelsDecoded,
      decoder);

    (*env)->ReleasePrimitiveArrayCritical(env, decoder->jIntOut, intOut, 0);

    free(output);
  } else {
    (*env)->ReleasePrimitiveArrayCritical(env, decoder->jByteOut, output, 0);
  }

  return retval;
}

void convertBackgroundColor(JNIEnv *env, GifDecoder *decoder) {
  if(!decoder->stateVars.backgroundConverted) {
    unsigned int backgroundColor;
    decoder->jLogicalScreen = (*env)->GetObjectField(env, decoder->jDataStream, img_GIF_ds_logicalScreenID);
    backgroundColor = 
      (*env)->GetIntField(env, decoder->jLogicalScreen, img_GIF_ls_backgroundColorID);
    if(backgroundColor != IMPOSSIBLE_VALUE)
      backgroundColor = decoder->useLocalColorTable ?
        decoder->localColorTable.rgbTable[backgroundColor] :
        decoder->globalColorTable.rgbTable[backgroundColor];
    (*env)->SetIntField(env, decoder->jLogicalScreen, img_GIF_ls_backgroundColorID, backgroundColor);
  }

  decoder->stateVars.backgroundConverted = TRUE;
}

void updateScanline(GifDecoder *decoder) {
  if(decoder->scanlineWritten) { // Else complete scanline first
    decoder->currScanline += decoder->increment;

    if(decoder->currScanline >= decoder->currentHeight) {
      decoder->pass++;
      decoder->currScanline = startingScanline[decoder->pass];
      decoder->increment = scanlineIncrement[decoder->pass];
    }

    decoder->scanlineWritten = FALSE;
  }
}

GIF_RETVAL decompressInterlaced(JNIEnv *env, jobject currBlock, GifDecoder *decoder) {
  unsigned char *output, *outPtr;
  GIF_RETVAL retval = STATUS_OK;
  unsigned int savedScanlineOffset = decoder->scanlineOffset;
  
  if(decoder->stateVars.forceRGB) {
    outPtr = output = malloc(decoder->currentWidth);
  } else {
    decoder->jByteOut = (*env)->GetObjectField(env, currBlock, img_GIF_gb_imageDataID);
    outPtr = output = (*env)->GetPrimitiveArrayCritical(env, decoder->jByteOut, 0);
  }

  for(;;) {
    int savedBlockSize, blockSize;
    DecompressInfo *di = &decoder->decompress;

    if(decoder->leftInBlock == 0) {
      savedBlockSize = *(decoder->inputPtr);    
      blockSize = savedBlockSize; 
    } else {
      savedBlockSize = decoder->leftInBlock;
      blockSize = savedBlockSize; 
    }
    
    if(decoder->bytesInBuffer < savedBlockSize + 1) { // Cannot read next block
      retval = STATUS_BUFFER_EMPTY;
      break;
    }
    
    if(decoder->leftInBlock == 0) { // Skip size itself
      decoder->inputPtr++;
      decoder->bytesInBuffer--;
    }
    
    if(savedBlockSize == 0) { // Frame completed
      decoder->state = STATE_BLOCK_BEGINNING;
      (*env)->SetBooleanField(env, currBlock, img_GIF_gb_completedID, TRUE);
      // Pass control to java code - need to call imageComplete and create new GifGraphicBlock
      retval = STATUS_FRAME_COMPLETED; 
      break;
    }

    if(decoder->doProcessing) {      
      updateScanline(decoder); // Set output scanline

      if(decoder->pass > 3) {
        decoder->inputPtr += blockSize;
        blockSize = 0;
        decoder->leftInBlock = 0;
        decoder->doProcessing = FALSE;
        decoder->bytesInBuffer -= savedBlockSize;
        continue;
      }

      if(decoder->stateVars.forceRGB) {
        outPtr = output;
      } else {
        outPtr = output + decoder->currentWidth * decoder->currScanline;
      }

      // pop what's left in the stack
      while (di->stackPointer && decoder->scanlineOffset < decoder->currentWidth) 
        outPtr[decoder->scanlineOffset++] = di->stack[--(di->stackPointer)];

      while(decoder->scanlineOffset < decoder->currentWidth) { // Decode one scanline
        if(di->shiftState < di->currCodeSize) {
          // Need to read at least one more byte
          di->accumbits |= (*(decoder->inputPtr++) << di->shiftState);
          di->shiftState += 8;
          blockSize--;
        } else {
          di->currCode = di->accumbits & codeMasks[di->currCodeSize];
          di->accumbits >>= di->currCodeSize;
          di->shiftState -= di->currCodeSize;

          // Check if we need to increase code size          
          if(++di->currKnownCode > di->maxCodeMinusOne && di->currCodeSize < MAX_BITS) {
            unsigned int maxCodePlusOne = di->maxCodeMinusOne + 2;
            di->currCodeSize++;
            di->maxCodeMinusOne = (maxCodePlusOne << 1) - 2;
          }

          // Now process the obtained code
          if (di->currCode == di->eoiCode) { // EOI encountered, stop processing
            decoder->inputPtr += blockSize;
            decoder->leftInBlock = 0;
            // Ensure that no more data will be decoded
            di->shiftState = 0;            
            blockSize = 0;
            decoder->doProcessing = FALSE;
          } else if(di->currCode == di->clearCode) { // Clear code - reset code size
            resetDecompressorState(decoder);
          } else { // Regular code
            if(di->currCode < di->clearCode) { // It is just a scalar pixel
              outPtr[decoder->scanlineOffset++] = di->currCode;
            } else { // Search for prefix for this code 
              // Code not added yet, corresponding prefix is the last code, 
              // suffix is the first character of the last code
              if(di->prefix[di->currCode] == IMPOSSIBLE_CODE) {
                di->currPrefix = di->lastCode;
                di->suffix[di->currKnownCode] = 
                  di->stack[di->stackPointer++] = getPrefix(di->lastCode, di);
              } else { // Set prefix to current code
                di->currPrefix = di->currCode;
              }
              
              pushStack(di);
                
              // Now pop the stack to get the output
              while (di->stackPointer && decoder->scanlineOffset < decoder->currentWidth)
                outPtr[decoder->scanlineOffset++] = di->stack[--(di->stackPointer)];
            }

            if(di->lastCode != IMPOSSIBLE_CODE) {
              // Set prefix to last code and update suffix
              di->prefix[di->currKnownCode] = di->lastCode;

              // Suffix depends on wether it is a special case or not
              if(di->currCode == di->currKnownCode) {
                di->suffix[di->currKnownCode] = getPrefix(di->lastCode, di);
              } else {
                di->suffix[di->currKnownCode] = getPrefix(di->currCode, di);
              }
            }

            // Save the last code
            di->lastCode = di->currCode;
          }
        }

        // Break if no more bytes to load in this block
        if(!blockSize && di->shiftState < di->currCodeSize) 
          break;
      } // while

    } else {
      decoder->inputPtr += savedBlockSize;
    }
    
    decoder->leftInBlock = blockSize;
    decoder->bytesInBuffer -= savedBlockSize - blockSize;

    if(decoder->scanlineOffset == decoder->currentWidth) {
      retval = STATUS_LINE_COMPLETED;
      decoder->scanlineWritten = TRUE;
      break;
    }
  }  

  if(decoder->stateVars.forceRGB) { // Need to convert to RGB from index color model
    if(decoder->currScanline >= 0 && decoder->scanlineOffset != 0 && decoder->pass < MAX_PASS) {
      unsigned int *intOut, *intSrcPtr;
      decoder->jIntOut = (*env)->GetObjectField(env, currBlock, img_GIF_gb_rgbImageDataID);
      intOut = (*env)->GetPrimitiveArrayCritical(env, decoder->jIntOut, 0);
    
      intSrcPtr = intOut + decoder->currScanline * decoder->currentWidth + savedScanlineOffset;

      intFromIndexModel(
        outPtr + savedScanlineOffset,
        intSrcPtr, 
        decoder->scanlineOffset - savedScanlineOffset,
        decoder);

      (*env)->ReleasePrimitiveArrayCritical(env, decoder->jIntOut, intOut, 0);
    }

    free(output);

  } else {
    (*env)->ReleasePrimitiveArrayCritical(env, decoder->jByteOut, output, 0);
  }  

  decoder->scanlineOffset %= decoder->currentWidth;  

  return retval;
}

void initInterlaceVars(GifDecoder *decoder) {
  decoder->increment = 8;
  decoder->pass = 0;
  decoder->scanlineOffset= 0;
  decoder->currScanline = -8;
  decoder->leftInBlock = 0;
  decoder->scanlineWritten = TRUE;
}

void sendCommentToJava(JNIEnv *env, GifDecoder *decoder) {
  jstring jStr;

  decoder->comment = realloc(decoder->comment, decoder->commentLength+1);
  *(decoder->comment + decoder->commentLength) = 0; // Set the last character to zero

  jStr = (*env)->NewStringUTF(env, decoder->comment);

  free(decoder->comment);
  decoder->comment = NULL;
  decoder->commentLength = 0;

  (*env)->CallVoidMethod(env, decoder->jDecoder, img_GIF_setCommentID, jStr);
}
