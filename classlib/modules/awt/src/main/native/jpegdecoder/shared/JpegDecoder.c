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

#include "JPEGDecoder.h"

typedef enum TAG_DECODER_STATE{
  INIT,
  START_DECOMPRESS,
  DECOMPRESS_STARTED,
  CONSUME_INPUT,
  PREPARE_OUTPUT_SCAN,
  DO_OUTPUT_SCAN,
  READ_DONE,
  DECOMPRESS_DESTROYED    
} DECODER_STATE;

// Error manager
typedef struct tag_gl_jpeg_error_mgr{
  struct jpeg_error_mgr base;
    jmp_buf jmp_buffer;
} gl_jpeg_error_mgr;

METHODDEF(void) gl_jpeg_error_exit(j_common_ptr cinfo) {
    gl_jpeg_error_mgr* myerr = (gl_jpeg_error_mgr*) cinfo->err;
  /*
    char buffer[JMSG_LENGTH_MAX];
    (*cinfo->err->format_message)(cinfo, buffer);
    Print message if needed here
  */
  // Now we are skipping errors silently
    longjmp(myerr->jmp_buffer, 1);
}

typedef struct tag_jpeg_source_mgr {
  struct jpeg_source_mgr base;

  JOCTET *jpeg_buffer;//[MAX_BUFFER];

    int valid_buffer_length;
  int buffer_size;
    size_t skip_input_bytes;
    boolean at_eof;
    boolean final_pass;
    boolean decoding_done;
    boolean do_progressive;
} gl_jpeg_source_mgr;

METHODDEF(void) gl_jpeg_dummy_decompress(j_decompress_ptr cinfo) {}

METHODDEF(boolean) gl_jpeg_fill_input_buffer(j_decompress_ptr cinfo) {
  gl_jpeg_source_mgr* srcmgr = (gl_jpeg_source_mgr*) cinfo->src;

    if ( srcmgr->at_eof ) {
        // Insert a fake EOI marker - as jpeglib advises
        srcmgr->jpeg_buffer[0] = (JOCTET) 0xFF;
        srcmgr->jpeg_buffer[1] = (JOCTET) JPEG_EOI;
        srcmgr->base.bytes_in_buffer = 2;
        srcmgr->base.next_input_byte = (JOCTET *) srcmgr->jpeg_buffer;
        return TRUE;
    } else {
        return FALSE;  // I/O suspension mode on
    }
}

METHODDEF(void) gl_jpeg_skip_input_data(j_decompress_ptr cinfo, long num_bytes) {
  gl_jpeg_source_mgr* srcmgr;
    size_t skipbytes;

  if(num_bytes <= 0) return; // required noop

    srcmgr = (gl_jpeg_source_mgr*) cinfo->src;
    srcmgr->skip_input_bytes += num_bytes;
  
    skipbytes = MIN(srcmgr->base.bytes_in_buffer, srcmgr->skip_input_bytes);

    if(skipbytes < srcmgr->base.bytes_in_buffer) {
            memmove(srcmgr->jpeg_buffer,
                srcmgr->base.next_input_byte + skipbytes,
                srcmgr->base.bytes_in_buffer - skipbytes);
    }

    srcmgr->base.bytes_in_buffer -= skipbytes;
    srcmgr->valid_buffer_length = (int) srcmgr->base.bytes_in_buffer;
    srcmgr->skip_input_bytes -= skipbytes;

    /* adjust data for jpeglib */
    cinfo->src->next_input_byte = (JOCTET *) srcmgr->jpeg_buffer;
    cinfo->src->bytes_in_buffer = (size_t) srcmgr->valid_buffer_length;
}

GLOBAL(boolean) gl_jpeg_init_source_mgr(j_decompress_ptr cinfo, int bufferSize) {

  int perfBufferSize;

  gl_jpeg_source_mgr* mgr = (gl_jpeg_source_mgr*) cinfo->src;
  // jpeg_source_mgr fields
  mgr->base.init_source = gl_jpeg_dummy_decompress;
    mgr->base.fill_input_buffer = gl_jpeg_fill_input_buffer;
    mgr->base.skip_input_data = gl_jpeg_skip_input_data;
    mgr->base.resync_to_restart = jpeg_resync_to_restart; // Use default
    mgr->base.term_source = gl_jpeg_dummy_decompress;
  
  perfBufferSize = bufferSize * 8;
 
  if (perfBufferSize < MIN_BUFFER) {
      mgr->buffer_size = MIN_BUFFER;
  } else if (perfBufferSize > MAX_BUFFER){
      mgr->buffer_size = MAX_BUFFER;
  } else {
      mgr->buffer_size = perfBufferSize;
  }
  mgr->jpeg_buffer = malloc(mgr->buffer_size);
  if(!mgr->jpeg_buffer) {
    return FALSE;
  }

    mgr->base.bytes_in_buffer = 0;
    mgr->base.next_input_byte = mgr->jpeg_buffer;

    // gl_jpeg_source_mgr fields
    mgr->valid_buffer_length = 0;
    mgr->skip_input_bytes = 0;
    mgr->at_eof = 0;
    mgr->final_pass = FALSE;
    mgr->decoding_done = FALSE;

  return TRUE;
}

typedef struct tag_gl_decompress_struct {
  struct jpeg_decompress_struct decompress;
    gl_jpeg_error_mgr   errorMgr;
    gl_jpeg_source_mgr  srcMgr;
  DECODER_STATE       decoderState;

  // Pointer to output buffer
  JSAMPARRAY outBuffer;
  int outBufferSize;
  size_t scanlineSize;
  int maxScanlines;
} gl_decompress_struct;

GLOBAL(void) gl_decompress_struct_destroy(gl_decompress_struct* glDecompress);

void outOfMemory(JNIEnv *env, jobject obj, gl_decompress_struct *glDecompress) {
  if(glDecompress) {
    gl_decompress_struct_destroy(glDecompress);
    free(glDecompress);
  }

  (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, -1);
    (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, 0);

  throwNewExceptionByName(env, "java/lang/OutOfMemoryError", "Out of memory");
}

GLOBAL(void) gl_decompress_struct_init(gl_decompress_struct** glDecompressPtr, int bufferSize) {
  gl_decompress_struct* glDecompress = *glDecompressPtr;

  if(glDecompress == NULL) { // Allocate new decompress struct
    glDecompress = malloc(sizeof(gl_decompress_struct));
    
    if(!glDecompress) // Out of memory
      return;

    *glDecompressPtr = glDecompress;    
  }

  // Clear standard jpeg decompress struct
  memset(&(glDecompress->decompress), 0, sizeof(glDecompress->decompress));

  // Set up error manager
  glDecompress->decompress.err = jpeg_std_error(&(glDecompress->errorMgr.base));
  glDecompress->errorMgr.base.error_exit = gl_jpeg_error_exit;

  // Init jpeg decompress struct
    jpeg_create_decompress(&glDecompress->decompress);

  // Set up source manager
  glDecompress->decompress.src = &(glDecompress->srcMgr.base);
  if(!gl_jpeg_init_source_mgr(&glDecompress->decompress, bufferSize)) { // Out of memory
    free(glDecompress);
    *glDecompressPtr = NULL;
    return;
  }

  //glDecompress->decompress.UseIPP = FALSE;

  glDecompress->decoderState = INIT;
}

GLOBAL(void) gl_decompress_struct_destroy(gl_decompress_struct* glDecompress) {
  int i;
  jpeg_destroy_decompress(&glDecompress->decompress);
  
  if(glDecompress->outBuffer) {
    for(i=0; i<glDecompress->outBufferSize; i++) {
      free(glDecompress->outBuffer[i]);
    }
    free(glDecompress->outBuffer);
    glDecompress->outBuffer = NULL;
  }
  
  // Cleanup input buffer
  if(glDecompress->srcMgr.jpeg_buffer)
    free(glDecompress->srcMgr.jpeg_buffer);
}

/*
 * Class:     org_apache_harmony_awt_gl_image_JpegDecoder
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_image_JpegDecoder_initIDs
(JNIEnv *env, jclass cls) {  
    img_JPEG_imageWidthID = (*env)->GetFieldID(env, cls, "imageWidth", "I");
    if(img_JPEG_imageWidthID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }

    img_JPEG_imageHeightID = (*env)->GetFieldID(env, cls, "imageHeight", "I");
    if(img_JPEG_imageHeightID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }

    img_JPEG_progressiveID = (*env)->GetFieldID(env, cls, "progressive", "Z");
    if(img_JPEG_progressiveID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }

    img_JPEG_jpegColorSpaceID = (*env)->GetFieldID(env, cls, "jpegColorSpace", "I");
    if(img_JPEG_jpegColorSpaceID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }

    img_JPEG_bytesConsumedID = (*env)->GetFieldID(env, cls, "bytesConsumed", "I");
    if(img_JPEG_bytesConsumedID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }

    img_JPEG_currScanlineID = (*env)->GetFieldID(env, cls, "currScanline", "I");
    if(img_JPEG_currScanlineID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }

    img_JPEG_hNativeDecoderID = (*env)->GetFieldID(env, cls, "hNativeDecoder", "J");
    if(img_JPEG_hNativeDecoderID == NULL) {
        throwNPException(env, "Unable to get field ID");
    }
}

// Utility function to perform fast buffer format conversion
static void toIntRGB(unsigned char *in, unsigned char *out, int lengthInPixels) {
  // To reverse representation faster go in backward direction
  unsigned char *inCurrPtr = in + lengthInPixels*3 - 1; // end of the input buffer

  // end of the output buffer
  unsigned int *outCurrPtr = (unsigned int *)(out + lengthInPixels*4) - 1;
  unsigned int val; 

  while (inCurrPtr > in) {
    val = *(inCurrPtr--);        // B
    val |= (*(inCurrPtr--))<<8;  // G
    val |= (*(inCurrPtr--))<<16; // R
    val |= 0xFF000000;           // Set alpha to 255
    *(outCurrPtr--) = val;
  }
}


/*
 * Class:     org_apache_harmony_awt_gl_image_JpegDecoder
 * Method:    releaseNativeDecoder
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_image_JpegDecoder_releaseNativeDecoder
(JNIEnv *env, jclass cls, jlong hglDecompress) {
  // Cleanup if image was truncated
  gl_decompress_struct *glDecompress = (gl_decompress_struct*) ((IDATA)hglDecompress);
  if(glDecompress) {
    gl_decompress_struct_destroy(glDecompress);
    free(glDecompress);
  }
}

#define RETURN_JAVA_ARRAY return intOut == NULL ? byteOut : intOut

/*
 * Class:     org_apache_harmony_awt_gl_image_JpegDecoder
 * Method:    decode
 * Signature: ([BIJ)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_awt_gl_image_JpegDecoder_decode
(JNIEnv *env, 
 jobject obj, 
 jbyteArray jbuffer, 
 jint bytesInBuffer, 
 jlong hglDecompress
) {
  int consumed, freeInJpegBuffer;
  unsigned char* buffer;
  jbyteArray byteOut = NULL;
  jintArray intOut = NULL;

  gl_decompress_struct *glDecompress =
    (gl_decompress_struct*) ((IDATA)hglDecompress);

  if(glDecompress == NULL) {
    gl_decompress_struct_init(&glDecompress, bytesInBuffer);
    if(glDecompress == NULL) { // Out of memory
      outOfMemory(env, obj, NULL);
      return 0;
    }
  }

  // Silently skip data if EOF already encountered
  if(glDecompress->srcMgr.at_eof) {
    (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, bytesInBuffer);
    (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, (jlong) ((IDATA)glDecompress));
    RETURN_JAVA_ARRAY;
    }

  if(setjmp(glDecompress->errorMgr.jmp_buffer)) {
        // this is fatal
    (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, -1);
        (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, 0);
    RETURN_JAVA_ARRAY;
    }

  freeInJpegBuffer = glDecompress->srcMgr.buffer_size - glDecompress->srcMgr.valid_buffer_length;
  // Grow jpeg input buffer (if needed) when buffered-image mode is used
  if(glDecompress->decompress.buffered_image) {    
    if(freeInJpegBuffer < bytesInBuffer) {
      JOCTET *tmp;
      glDecompress->srcMgr.buffer_size += MAX_BUFFER;

      if((tmp = realloc(glDecompress->srcMgr.jpeg_buffer, glDecompress->srcMgr.buffer_size))) {        
        glDecompress->srcMgr.jpeg_buffer = tmp;
              
        freeInJpegBuffer = 
          glDecompress->srcMgr.buffer_size - glDecompress->srcMgr.valid_buffer_length;
      } else {
        outOfMemory(env, obj, glDecompress);
        return 0;
      }
    }
  }

  consumed = MIN(bytesInBuffer, freeInJpegBuffer);

  // filling buffer with the new data  
  buffer = (*env)->GetPrimitiveArrayCritical(env, jbuffer, 0);
  memcpy(glDecompress->srcMgr.jpeg_buffer + glDecompress->srcMgr.valid_buffer_length, buffer, consumed);
  // move the unconsumed data to the beginning of the java buffer
  if(consumed < bytesInBuffer && consumed > 0) { 
    memcpy(buffer, buffer+consumed, bytesInBuffer-consumed);
  }
  (*env)->ReleasePrimitiveArrayCritical(env, jbuffer, buffer, 0);

    glDecompress->srcMgr.valid_buffer_length += consumed;

    if(glDecompress->srcMgr.skip_input_bytes) {
        int skipbytes = (int) MIN((size_t) glDecompress->srcMgr.valid_buffer_length, glDecompress->srcMgr.skip_input_bytes);

        if(skipbytes < glDecompress->srcMgr.valid_buffer_length) {
            memmove(glDecompress->srcMgr.jpeg_buffer,
                glDecompress->srcMgr.jpeg_buffer + skipbytes,
                glDecompress->srcMgr.valid_buffer_length - skipbytes);
        }

        glDecompress->srcMgr.valid_buffer_length -= skipbytes;
        glDecompress->srcMgr.skip_input_bytes -= skipbytes;

        // still more bytes to skip
        if(glDecompress->srcMgr.skip_input_bytes) {
            if(consumed <= 0)
        assert(0); // This is illegal

      (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, consumed);
      (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, (jlong) ((IDATA)glDecompress));
      RETURN_JAVA_ARRAY;
        }
    } // if(glDecompress->srcMgr.skip_input_bytes)

  glDecompress->srcMgr.base.next_input_byte = (JOCTET *) glDecompress->srcMgr.jpeg_buffer;
    glDecompress->srcMgr.base.bytes_in_buffer = (size_t) glDecompress->srcMgr.valid_buffer_length;

  if(glDecompress->decoderState == INIT) {
    if(jpeg_read_header(&glDecompress->decompress, TRUE) != JPEG_SUSPENDED) {
      int width, height;

      width = glDecompress->decompress.image_width / glDecompress->decompress.scale_denom;
      height = glDecompress->decompress.image_height / glDecompress->decompress.scale_denom;

      (*env)->SetIntField(env, obj, img_JPEG_imageWidthID, width);
      (*env)->SetIntField(env, obj, img_JPEG_imageHeightID, height);

            glDecompress->decoderState = START_DECOMPRESS;
        }
    }

    if(glDecompress->decoderState == START_DECOMPRESS) {
    int i;

    glDecompress->srcMgr.do_progressive = 
      jpeg_has_multiple_scans( &glDecompress->decompress );

    // Pass to java progressive or not
    (*env)->SetBooleanField(env, obj, 
      img_JPEG_progressiveID, 
      glDecompress->srcMgr.do_progressive);

    glDecompress->decompress.buffered_image = glDecompress->srcMgr.do_progressive;

        // setup image sizes
        jpeg_calc_output_dimensions( &glDecompress->decompress );

    // Accept 2 output color spaces: GRAY and RGB. 
    // Alpha channel will be probably supported in the future...
        if (glDecompress->decompress.out_color_space != JCS_GRAYSCALE) {
            glDecompress->decompress.out_color_space = JCS_RGB;
      glDecompress->scanlineSize = glDecompress->decompress.image_width * 3;
    } else {
      glDecompress->scanlineSize = glDecompress->decompress.image_width;
    }

    // Pass output color space to java 
    (*env)->SetIntField(env, obj, 
      img_JPEG_jpegColorSpaceID, 
      glDecompress->decompress.out_color_space);

        glDecompress->decompress.do_fancy_upsampling = TRUE;
        glDecompress->decompress.do_block_smoothing = FALSE;
        glDecompress->decompress.quantize_colors = FALSE;
        glDecompress->decompress.dct_method = JDCT_FASTEST;

        // false: IO suspension
        if(jpeg_start_decompress(&glDecompress->decompress)) {
            glDecompress->decoderState =
        glDecompress->srcMgr.do_progressive ? DECOMPRESS_STARTED : DO_OUTPUT_SCAN;
        }

    // Calculate output buffer size and allocate memory for it
    glDecompress->maxScanlines = MAX((int)(MAX_BUFFER / glDecompress->scanlineSize), 1);
    glDecompress->outBufferSize = glDecompress->maxScanlines; // * glDecompress->scanlineSize;    

    glDecompress->outBuffer = malloc(glDecompress->outBufferSize * sizeof(JSAMPROW));
    if(!glDecompress->outBuffer) { // Out of memory
      outOfMemory(env, obj, glDecompress);
      return 0;
    }

    for(i = 0; i < glDecompress->outBufferSize; i++) {
      if(!(glDecompress->outBuffer[i] = malloc(glDecompress->scanlineSize))) {
        if(i == 0) { // Cannot allocate buffer for one scanline
          free(glDecompress->outBuffer);
          outOfMemory(env, obj, glDecompress);
          return 0;
        } else { // Proceed with smaller buffer
          glDecompress->maxScanlines = glDecompress->outBufferSize = i - 1;
          break;
        }
      }
    }
    }

    if(glDecompress->decoderState == DECOMPRESS_STARTED) {
        glDecompress->decoderState = CONSUME_INPUT;
    }

    if(glDecompress->decoderState == CONSUME_INPUT) {
        int retval;

        do {
      retval = jpeg_consume_input(&glDecompress->decompress);
        } while (retval != JPEG_SUSPENDED && retval != JPEG_REACHED_EOI && retval != JPEG_REACHED_SOS);

        if( glDecompress->srcMgr.final_pass
            || retval == JPEG_REACHED_EOI
            || retval == JPEG_REACHED_SOS) {
            glDecompress->decoderState = PREPARE_OUTPUT_SCAN;
        }
    }

    if(glDecompress->decoderState == PREPARE_OUTPUT_SCAN) {
        if ( jpeg_start_output(
          &glDecompress->decompress, 
          glDecompress->decompress.input_scan_number) 
        ) {
            glDecompress->decoderState = DO_OUTPUT_SCAN;
        }
    }

    if(glDecompress->decoderState == DO_OUTPUT_SCAN) {
    int oldoutput_scanline, completed_scanlines, lengthPixels, scanlines_read;
    JSAMPARRAY outbufferPtr;

        if(glDecompress->srcMgr.decoding_done) { // Decoding done, just keep eating input data
      (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, consumed);
      (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, (jlong) ((IDATA)glDecompress));
      RETURN_JAVA_ARRAY;
        }

    oldoutput_scanline = glDecompress->decompress.output_scanline;
    completed_scanlines = 0;

    outbufferPtr = glDecompress->outBuffer;
    
    // here decoding goes
        while(glDecompress->decompress.output_scanline < glDecompress->decompress.output_height &&
                (scanlines_read = jpeg_read_scanlines(
            &glDecompress->decompress, 
            (JSAMPARRAY)(outbufferPtr), 
            glDecompress->maxScanlines - completed_scanlines))) {
      completed_scanlines = glDecompress->decompress.output_scanline - oldoutput_scanline; 
      outbufferPtr+=scanlines_read;//glDecompress->scanlineSize;
      if(glDecompress->maxScanlines - completed_scanlines <= 0) 
        break;
    }

        (*env)->SetIntField(env, obj, img_JPEG_currScanlineID, glDecompress->decompress.output_scanline);
    
    // Create java array and fill it with data
    lengthPixels = completed_scanlines * glDecompress->decompress.image_width;
    if(glDecompress->decompress.out_color_space == JCS_GRAYSCALE) {
      unsigned char *outputBuffer;
      int i, offset;
      byteOut = (*env)->NewByteArray(env, lengthPixels);
      outputBuffer = (unsigned char*) (*env)->GetPrimitiveArrayCritical(env, byteOut, 0);
      for(i=0, offset=0; i<completed_scanlines;i++, offset+=glDecompress->decompress.image_width) {
        JSAMPROW outBufferPtr = glDecompress->outBuffer[i];
        memcpy(outputBuffer+offset, outBufferPtr, glDecompress->decompress.image_width);
      }
      (*env)->ReleasePrimitiveArrayCritical(env, byteOut, outputBuffer, 0);
    } else { // JCS_RGB
      unsigned char *outputBuffer;
      int i, offset;
      intOut = (*env)->NewIntArray(env, lengthPixels);
      outputBuffer = (unsigned char*)(*env)->GetPrimitiveArrayCritical(env, intOut, 0);
      // Expand 24->32 bpp for each scanline.
      for(i=0, offset=0; i<completed_scanlines;i++, offset+=glDecompress->decompress.image_width*4/*sizeof java integer*/) {
        JSAMPROW outBufferPtr = glDecompress->outBuffer[i];
        toIntRGB(outBufferPtr, outputBuffer + offset, glDecompress->decompress.image_width);
      }
      (*env)->ReleasePrimitiveArrayCritical(env, intOut, outputBuffer, 0);
    }

        if(glDecompress->decompress.output_scanline >= glDecompress->decompress.output_height) {
            if ( glDecompress->srcMgr.do_progressive ) {
                jpeg_finish_output(&glDecompress->decompress);
                glDecompress->srcMgr.final_pass = jpeg_input_complete(&glDecompress->decompress);

                glDecompress->srcMgr.decoding_done =
          glDecompress->srcMgr.final_pass && 
          glDecompress->decompress.input_scan_number == 
            glDecompress->decompress.output_scan_number;
            } else {
                glDecompress->srcMgr.decoding_done = TRUE;
            }
            if(!glDecompress->srcMgr.decoding_done) {
                // don't return until necessary!
                glDecompress->decoderState = DECOMPRESS_STARTED;
            }
        }

        if(glDecompress->decoderState == DO_OUTPUT_SCAN && glDecompress->srcMgr.decoding_done) {
            glDecompress->srcMgr.at_eof = TRUE;

            (void) jpeg_finish_decompress(&glDecompress->decompress);
            (void) gl_decompress_struct_destroy(glDecompress);

            glDecompress->decoderState = READ_DONE;
      free(glDecompress);

      (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, 0);
            (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, 0/*(jlong) glDecompress*/);
      RETURN_JAVA_ARRAY;
        }
    }

    if(glDecompress->srcMgr.base.bytes_in_buffer    &&
     glDecompress->srcMgr.jpeg_buffer != glDecompress->srcMgr.base.next_input_byte
    ) {
        memmove(glDecompress->srcMgr.jpeg_buffer,
            glDecompress->srcMgr.base.next_input_byte,
            glDecompress->srcMgr.base.bytes_in_buffer);
    }
    glDecompress->srcMgr.valid_buffer_length = (int) glDecompress->srcMgr.base.bytes_in_buffer;
 
  (*env)->SetIntField(env, obj, img_JPEG_bytesConsumedID, consumed);
  (*env)->SetLongField(env, obj, img_JPEG_hNativeDecoderID, (jlong) ((IDATA)glDecompress));
  RETURN_JAVA_ARRAY;
}
