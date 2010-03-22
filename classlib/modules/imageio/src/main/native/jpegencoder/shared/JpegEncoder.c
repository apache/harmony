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
/*
 * @author Rustem Rafikov
 */

#include <string.h>
#include <stdlib.h>
#include <assert.h>

#include "org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter.h"
#include "jpeglib.h"
#include "setjmp.h"

#include "exceptions.h"

#define MAX_BUFFER 32768


typedef struct {
    struct jpeg_error_mgr base;
    jmp_buf jmp_buffer;
} enc_error_mgr, * enc_error_mgr_ptr;

typedef struct {
  JOCTET *jpeg_buffer;
  jobject ios;
} enc_client_data, * enc_client_data_ptr;


static jmethodID IOSwriteID;
static jmethodID getScanlineID;
static jmethodID QTableGetTableID;

JavaVM *jvm;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    return JNI_VERSION_1_2;
}

static JNIEnv* get_env() {
    JNIEnv *e;
    (*jvm)->GetEnv(jvm, (void **)&e, JNI_VERSION_1_2);
    return e;
}

/**
 * Sets DQT tables
 */
static void setupDQTs(JNIEnv *env, j_compress_ptr cinfo, jobjectArray dqts) {

    JQUANT_TBL *quant_ptr;
    jint len;
    int i, j;
    jobject slot;
    jint *arrPtr;

    if (!dqts)  {
        //-- TODO: write warning
        return;
    }

    len = (*env)->GetArrayLength(env, dqts);

    for (i = 0; i < len; i++) {
        slot = (*env)->GetObjectArrayElement(env, dqts, i);
        arrPtr = (*env)->GetPrimitiveArrayCritical(env, slot, NULL);

        if (cinfo->quant_tbl_ptrs[i] == NULL) {
            cinfo->quant_tbl_ptrs[i] = jpeg_alloc_quant_table((j_common_ptr)cinfo);
        }
        quant_ptr = cinfo->quant_tbl_ptrs[i];

        for (j = 0; j < 64; j++) {
            quant_ptr->quantval[j] = arrPtr[j];
        }
        quant_ptr->sent_table = FALSE;
        (*env)->ReleasePrimitiveArrayCritical(env, slot, arrPtr, 0);
        (*env)->DeleteLocalRef(env, slot);
    }
}

/*
 * Initialize destination --- called by jpeg_start_compress
 * before any data is actually written.
 */
METHODDEF(void) ios_init_destination(j_compress_ptr cinfo) {
}

/**
 * Called when output buffer becomes filled
 */
METHODDEF(boolean) ios_empty_output_buffer(j_compress_ptr cinfo) {

    struct jpeg_compress_struct * cs;
    enc_client_data_ptr cdata;
    enc_error_mgr_ptr err_mgr;

    jbyteArray java_buffer;
    JOCTET *native_buffer;
    JNIEnv *env;

    cs = (struct jpeg_compress_struct *) cinfo;
    cdata = (enc_client_data_ptr) cs->client_data;
    err_mgr = (enc_error_mgr_ptr) cs->err;

    env = get_env();
    java_buffer = (*env)->NewByteArray(env, MAX_BUFFER);
    native_buffer = (JOCTET *)(*env)->GetPrimitiveArrayCritical(env, java_buffer, NULL);
    memcpy(native_buffer, cdata->jpeg_buffer, MAX_BUFFER);
    (*env)->ReleasePrimitiveArrayCritical(env, java_buffer, native_buffer, 0);
    (*env)->CallVoidMethod(env, cdata->ios, IOSwriteID, java_buffer, 0, MAX_BUFFER);

    // checking for an exception in the java method
    if ((*env)->ExceptionOccurred(env)) {
        err_mgr->base.error_exit((j_common_ptr) cinfo);
    }
    
    cs->dest->next_output_byte = cdata->jpeg_buffer;
    cs->dest->free_in_buffer = MAX_BUFFER;
    
    return TRUE;
}

/*
 * Terminate destination --- called by jpeg_finish_compress
 */
METHODDEF(void) ios_term_destination(j_compress_ptr cinfo) {


    struct jpeg_compress_struct * cs;
    enc_client_data_ptr cdata;
    enc_error_mgr_ptr err_mgr;

    jbyteArray java_buffer;
    JOCTET *native_buffer;
    JNIEnv *env;

    cs = (struct jpeg_compress_struct *) cinfo;
    cdata = (enc_client_data_ptr) cs->client_data;
    err_mgr = (enc_error_mgr_ptr) cs->err;

    env = get_env();
    java_buffer = (*env)->NewByteArray(env, MAX_BUFFER);
    native_buffer = (JOCTET *)(*env)->GetPrimitiveArrayCritical(env, java_buffer, NULL);
    memcpy(native_buffer, cdata->jpeg_buffer, MAX_BUFFER);
    (*env)->ReleasePrimitiveArrayCritical(env, java_buffer, native_buffer, 0);
    (*env)->CallVoidMethod(env, cdata->ios, IOSwriteID, java_buffer, 0, MAX_BUFFER - cs->dest->free_in_buffer);

    // checking for an exception in the java method
    if ((*env)->ExceptionOccurred(env)) {
        err_mgr->base.error_exit((j_common_ptr) cinfo);
    }

    cs->dest->next_output_byte = NULL;
    cs->dest->free_in_buffer = 0;
}

/** 
 * The error handler
 */
METHODDEF(void) ios_jpeg_error_exit(j_common_ptr cinfo) {
    enc_error_mgr_ptr err_mgr = (enc_error_mgr_ptr) cinfo->err;
#ifdef DEBUG
    printf("in jpeg_error_exit()\n");
#endif //-- DEBUG
    longjmp(err_mgr->jmp_buffer, 1);
}

/**
 * It is called from initCompressionObjects
 */
//GLOBAL(CompressStruct*) ios_create_compress(JNIEnv *env) {
GLOBAL(struct jpeg_compress_struct *) ios_create_compress(JNIEnv *env) {

    struct jpeg_compress_struct * cinfo;
    struct jpeg_destination_mgr * dest_mgr;

    enc_error_mgr_ptr err_mgr;
    enc_client_data_ptr client_data;

    //-- create compress struct
    cinfo = malloc(sizeof(struct jpeg_compress_struct));
    if (!cinfo) {
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return 0;
    }

    //-- create error manager
    err_mgr = malloc(sizeof(enc_error_mgr));
    if (!err_mgr) {
        free(cinfo);
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return 0;
    }
    

    cinfo->err = jpeg_std_error(&(err_mgr->base));
    err_mgr->base.error_exit = ios_jpeg_error_exit;


    //-- TODO setjmp before every call to libjpeg
    jpeg_create_compress(cinfo);

    dest_mgr = malloc(sizeof(struct jpeg_destination_mgr));
    if (!dest_mgr) {
        free(cinfo);
        free(err_mgr);
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return 0;
    }
    cinfo->dest = dest_mgr;

    client_data = malloc(sizeof(enc_client_data));

    if (!client_data) {
        free(cinfo);
        free(err_mgr);
        free(dest_mgr);
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return 0;
    }
    cinfo->client_data = client_data;
    client_data->ios = NULL;
    client_data->jpeg_buffer = malloc(MAX_BUFFER);
    if (!client_data->jpeg_buffer) {
        free(cinfo);
        free(err_mgr);
        free(dest_mgr);
        free(client_data);
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return 0;
    }
    dest_mgr->next_output_byte = client_data->jpeg_buffer;
    dest_mgr->free_in_buffer = MAX_BUFFER;

    dest_mgr->init_destination = ios_init_destination;
    dest_mgr->empty_output_buffer = ios_empty_output_buffer;
    dest_mgr->term_destination = ios_term_destination;
    
    return cinfo;
}

/*
 * Class:     org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter
 * Method:    dispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter_dispose(JNIEnv *env, jobject obj, jlong handle) {

    struct jpeg_compress_struct * cinfo = (struct jpeg_compress_struct *) (IDATA)handle;
    enc_client_data_ptr cdata = (enc_client_data_ptr) cinfo->client_data;

    if (cdata->ios != NULL) {
        (*env)->DeleteGlobalRef(env, cdata->ios);
        cdata->ios = NULL;
    }
    if (cdata->jpeg_buffer != NULL) {
        free(cdata->jpeg_buffer);
    }
    
    free(cinfo->dest);
    free(cinfo->client_data);
    free(cinfo->err);

    jpeg_destroy_compress(cinfo);
    free(cinfo);
}

/*
 * Class:     org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter
 * Method:    initWriterIds
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter_initWriterIds(JNIEnv *env, jclass encoderClass, jclass iosClass) {

    //-- ImageOutputStream.write(byte[], int, int)
    IOSwriteID = (*env)->GetMethodID(env, iosClass, "write", "([BII)V");
    //-- call back to getScanLine(int);
    getScanlineID = (*env)->GetMethodID(env, encoderClass, "getScanLine", "(I)V");

    //-- init jvm var
    (*env)->GetJavaVM(env, &jvm);
}

/*
 * Class:     org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter
 * Method:    initCompressionObj
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL 
Java_org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter_initCompressionObj(JNIEnv *env, jobject encoder) {
    return (jlong) (IDATA)ios_create_compress(env);
}

/*
 * Class:     org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter
 * Method:    setIOS
 * Signature: (Ljavax/imageio/stream/ImageOutputStream;J)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter_setIOS(JNIEnv *env, jobject encoder, jobject iosObj, jlong handle) {

    struct jpeg_compress_struct * cinfo = (struct jpeg_compress_struct *) (IDATA)handle;
    enc_client_data_ptr cdata = (enc_client_data_ptr) cinfo->client_data;

    if (cdata->ios != NULL) {
        (*env)->DeleteGlobalRef(env, cdata->ios);
        cdata->ios = NULL;
    }
    cdata->ios = (*env)->NewGlobalRef(env, iosObj);
    if (cdata->ios == NULL) {
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return;
    }
    cinfo->dest->next_output_byte = cdata->jpeg_buffer;
    cinfo->dest->free_in_buffer = MAX_BUFFER;
}


/*
 * Class:     org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter
 * Method:    encode
 * Signature: ([BIIIIIIIZ[[IJ)Z
 */
JNIEXPORT jboolean JNICALL 
Java_org_apache_harmony_x_imageio_plugins_jpeg_JPEGImageWriter_encode(JNIEnv *env, 
    jobject callerObj, jbyteArray arr, jint srcWidth, jint width, jint height, jint deltaX, 
    jint in_cs, jint out_cs, jint numBands, jboolean progressive, jobjectArray dqts, jlong handle) 
{

    JSAMPROW row_pointer;
    struct jpeg_compress_struct * cinfo;
    enc_error_mgr_ptr err_mgr;
    
    int i, j;
    int cur_scanline;
    unsigned char * native_buffer;
    unsigned char * rowPtr;

    jboolean optimizeHuffman = FALSE;

    row_pointer = (JSAMPROW) malloc(width * numBands);

    if (!row_pointer) {
        throwNewOutOfMemoryError(env, "Unable to allocate memory for IJG structures");
        return FALSE;
    }

    
    cinfo = (struct jpeg_compress_struct *) (IDATA)handle;
    err_mgr = (enc_error_mgr_ptr) cinfo->err;

    if (setjmp(err_mgr->jmp_buffer)) {
        if (!(*env)->ExceptionOccurred(env)) {
            char msg_buffer[JMSG_LENGTH_MAX];
            cinfo->err->format_message((j_common_ptr)cinfo, msg_buffer);
            throwNewExceptionByName(env, "javax/imageio/IIOException",
                                    msg_buffer);
        }
        if (row_pointer) {
            free(row_pointer);
        }
        return FALSE;
    }

    cinfo->image_width = width;
    cinfo->image_height = height;
    cinfo->input_components = numBands;
    cinfo->in_color_space = in_cs;
    
    jpeg_set_defaults(cinfo);
    jpeg_set_colorspace(cinfo, out_cs);
    cinfo->optimize_coding = optimizeHuffman;

    //-- TRUE - for pure abbrivated images (wo tables) creation 
    //-- if you want to write some tables set "sent_table = FALSE" after 
    //-- this call for the table to be emitted. 
    //jpeg_suppress_tables(&cinfo, TRUE);
    jpeg_suppress_tables(cinfo, FALSE);

    setupDQTs(env, cinfo, dqts);

    //-- only simple progression sequence
    if (progressive) {
        jpeg_simple_progression(cinfo);
    }

    //-- TRUE forces all "sent_table = FALSE" so all tables will be written
    jpeg_start_compress(cinfo, TRUE);

    //-- use this for tables-only files and abbrivated images.
    //-- If using jpeg_suppress_tables(&cinfo, TRUE):
    //-- Only DQT sent_table = FALSE right now -> so DQT will be written but not DHT.
    //-- uncomment when custom huffman tables be used.
    //jpeg_start_compress(&cinfo, FALSE);

    cur_scanline = 0;

    while (cinfo->next_scanline < cinfo->image_height) {
       (*env)->CallVoidMethod(env, callerObj, getScanlineID, cur_scanline);

       // checking for an exception in the java method
       if ((*env)->ExceptionOccurred(env)) {
           //c_struct->exception_in_callback = TRUE;
           cinfo->err->error_exit((j_common_ptr) cinfo);
       }

       native_buffer = (JOCTET*) (*env)->GetPrimitiveArrayCritical(env, arr, NULL);

       // subsampling and copying to internal array
       rowPtr = row_pointer;
       for (i = 0; i < srcWidth * numBands; i += numBands * deltaX) {
           for (j = 0; j < numBands; j++) {
               *rowPtr++ = native_buffer[i + j];
           }
       }
       (*env)->ReleasePrimitiveArrayCritical(env, arr, native_buffer, 0);

       jpeg_write_scanlines(cinfo, &row_pointer, 1);

       cur_scanline++;
    }

    jpeg_finish_compress(cinfo);
    free(row_pointer);

    return TRUE;
}

