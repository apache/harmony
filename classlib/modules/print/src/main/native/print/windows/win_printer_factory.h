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
#include <jni.h>

#ifndef _Included_org_apache_harmony_x_print_WinPrinterFactory
#define _Included_org_apache_harmony_x_print_WinPrinterFactory
#ifdef __cplusplus
extern "C" {
#endif
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAUSED
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAUSED 1L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_ERROR
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_ERROR 2L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PENDING_DELETION
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PENDING_DELETION 4L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAPER_JAM
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAPER_JAM 8L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAPER_OUT
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAPER_OUT 16L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_MANUAL_FEED
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_MANUAL_FEED 32L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAPER_PROBLEM
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAPER_PROBLEM 64L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_OFFLINE
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_OFFLINE 128L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_IO_ACTIVE
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_IO_ACTIVE 256L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_BUSY
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_BUSY 512L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PRINTING
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PRINTING 1024L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_OUTPUT_BIN_FULL
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_OUTPUT_BIN_FULL 2048L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_NOT_AVAILABLE
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_NOT_AVAILABLE 4096L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_WAITING
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_WAITING 8192L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PROCESSING
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PROCESSING 16384L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_INITIALIZING
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_INITIALIZING 32768L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_WARMING_UP
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_WARMING_UP 65536L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_TONER_LOW
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_TONER_LOW 131072L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_NO_TONER
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_NO_TONER 262144L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAGE_PUNT
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_PAGE_PUNT 524288L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_USER_INTERVENTION
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_USER_INTERVENTION 1048576L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_OUT_OF_MEMORY
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_OUT_OF_MEMORY 2097152L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_DOOR_OPEN
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_DOOR_OPEN 4194304L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_SERVER_UNKNOWN
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_SERVER_UNKNOWN 8388608L
#undef org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_POWER_SAVE
#define org_apache_harmony_x_print_WinPrinterFactory_PRINTER_STATUS_POWER_SAVE 16777216L
/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getDefaultPrinterName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getDefaultPrinterName
  (JNIEnv *, jclass);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getConnectedPrinterNames
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getConnectedPrinterNames
  (JNIEnv *, jclass);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPrinterHandle
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterHandle
  (JNIEnv *, jclass, jstring);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    releasePrinterHandle
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_releasePrinterHandle
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPrinterProps
 * Signature: (Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterProps
  (JNIEnv *, jclass, jstring, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPrinterDC
 * Signature: (Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterDC
  (JNIEnv *, jclass, jstring, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    releasePrinterDC
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_releasePrinterDC
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    startDoc
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_startDoc
  (JNIEnv *, jclass, jlong, jstring, jstring);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    endDoc
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_endDoc
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    startPage
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_startPage
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    endPage
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_endPage
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getQueuedJobs
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getQueuedJobs
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPixelsPerInchX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPixelsPerInchX
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPixelsPerInchY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPixelsPerInchY
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPaperPhysicalWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPaperPhysicalWidth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPaperPhysicalHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPaperPhysicalHeight
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getPrinterStatus
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterStatus
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    isColorPrintingSupported
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_isColorPrintingSupported
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    isCollatingSupported
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_isCollatingSupported
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    isDuplexSupported
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_isDuplexSupported
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getSupportedPaperSizes
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getSupportedPaperSizes
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getSupportedResolutions
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getSupportedResolutions
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getLandscapeOrientationDegree
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getLandscapeOrientationDegree
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    getMaxNumberOfCopies
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getMaxNumberOfCopies
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_WinPrinterFactory
 * Method:    cancelPrinterJob
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_cancelPrinterJob
  (JNIEnv *, jclass, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif
