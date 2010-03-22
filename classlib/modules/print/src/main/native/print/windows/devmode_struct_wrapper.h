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

#ifndef _Included_org_apache_harmony_x_print_DevmodeStructWrapper
#define _Included_org_apache_harmony_x_print_DevmodeStructWrapper
#ifdef __cplusplus
extern "C" {
#endif
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_ORIENTATION
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_ORIENTATION 1L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_PAPERSIZE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_PAPERSIZE 2L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_PAPERLENGTH
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_PAPERLENGTH 4L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_PAPERWIDTH
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_PAPERWIDTH 8L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_SCALE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_SCALE 16L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_COPIES
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_COPIES 256L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_DEFAULTSOURCE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_DEFAULTSOURCE 512L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_PRINTQUALITY
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_PRINTQUALITY 1024L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_COLOR
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_COLOR 2048L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_DUPLEX
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_DUPLEX 4096L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_YRESOLUTION
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_YRESOLUTION 8192L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_TTOPTION
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_TTOPTION 16384L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DM_COLLATE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DM_COLLATE 32768L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMORIENT_PORTRAIT
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMORIENT_PORTRAIT 1L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMORIENT_LANDSCAPE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMORIENT_LANDSCAPE 2L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMRES_HIGH
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMRES_HIGH -4L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMRES_MEDIUM
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMRES_MEDIUM -3L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMRES_DRAFT
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMRES_DRAFT -1L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMDUP_SIMPLEX
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMDUP_SIMPLEX 1L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMDUP_VERTICAL
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMDUP_VERTICAL 2L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMDUP_HORIZONTAL
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMDUP_HORIZONTAL 3L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLLATE_FALSE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLLATE_FALSE 0L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLLATE_TRUE
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLLATE_TRUE 1L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLOR_MONOCHROME
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLOR_MONOCHROME 1L
#undef org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLOR_COLOR
#define org_apache_harmony_x_print_DevmodeStructWrapper_DMCOLOR_COLOR 2L
/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmDeviceName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmDeviceName
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmFields
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmFields
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmOrientation
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmOrientation
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmOrientation
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmOrientation
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmPaperSize
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPaperSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmPaperSize
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPaperSize
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmPaperLength
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPaperLength
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmPaperLength
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPaperLength
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmPaperWidth
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPaperWidth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmPaperWidth
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPaperWidth
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmScale
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmScale
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmScale
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmScale
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmCopies
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmCopies
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmCopies
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmCopies
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmDefaultSource
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmDefaultSource
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmDefaultSource
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmDefaultSource
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmPrintQuality
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPrintQuality
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmPrintQuality
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPrintQuality
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmColor
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmColor
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmColor
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmColor
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmDuplex
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmDuplex
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmDuplex
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmDuplex
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmYResolution
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmYResolution
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmYResolution
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmYResolution
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmTTOption
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmTTOption
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmTTOption
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmTTOption
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    getDmCollate
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmCollate
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    setDmCollate
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmCollate
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     org_apache_harmony_x_print_DevmodeStructWrapper
 * Method:    releaseStruct
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_releaseStruct
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
/* Header for class org_apache_harmony_x_print_DevmodeStructWrapper_CustomPaper */

#ifndef _Included_org_apache_harmony_x_print_DevmodeStructWrapper_CustomPaper
#define _Included_org_apache_harmony_x_print_DevmodeStructWrapper_CustomPaper
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif
/* Header for class org_apache_harmony_x_print_DevmodeStructWrapper_Paper */

#ifndef _Included_org_apache_harmony_x_print_DevmodeStructWrapper_Paper
#define _Included_org_apache_harmony_x_print_DevmodeStructWrapper_Paper
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif
/* Header for class org_apache_harmony_x_print_DevmodeStructWrapper_StdPaper */

#ifndef _Included_org_apache_harmony_x_print_DevmodeStructWrapper_StdPaper
#define _Included_org_apache_harmony_x_print_DevmodeStructWrapper_StdPaper
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif
