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
#include "devmode_struct_wrapper.h"
#include <windows.h>

#define DMSTR(jlongVal) ((DEVMODEW *) (unsigned long) jlongVal)

JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmDeviceName(JNIEnv * env, jclass c, jlong ptr) {
	if (DMSTR(ptr)->dmDeviceName != NULL) {
		return (*env)->NewString(env, DMSTR(ptr)->dmDeviceName,
				wcslen(DMSTR(ptr)->dmDeviceName));
	}

	return NULL;
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmFields(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmFields;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmOrientation(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmOrientation;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmOrientation(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmOrientation = val;
	DMSTR(ptr)->dmFields |= DM_ORIENTATION;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPaperSize(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmPaperSize;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPaperSize(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmPaperSize = val;
	DMSTR(ptr)->dmFields |= DM_PAPERSIZE;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPaperLength(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmPaperLength;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPaperLength(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmPaperLength = val;
	DMSTR(ptr)->dmFields |= DM_PAPERLENGTH;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPaperWidth(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmPaperWidth;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPaperWidth(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmPaperWidth = val;
	DMSTR(ptr)->dmFields |= DM_PAPERWIDTH;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmScale(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmScale;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmScale(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmScale = val;
	DMSTR(ptr)->dmFields |= DM_SCALE;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmCopies(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmCopies;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmCopies(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmCopies = val;
	DMSTR(ptr)->dmFields |= DM_COPIES;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmDefaultSource(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmDefaultSource;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmDefaultSource(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmDefaultSource = val;
	DMSTR(ptr)->dmFields |= DM_DEFAULTSOURCE;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmPrintQuality(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmPrintQuality;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmPrintQuality(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmPrintQuality = val;
	DMSTR(ptr)->dmFields |= DM_PRINTQUALITY;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmColor(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmColor;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmColor(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmColor = val;
	DMSTR(ptr)->dmFields |= DM_COLOR;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmDuplex(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmDuplex;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmDuplex(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmDuplex = val;
	DMSTR(ptr)->dmFields |= DM_DUPLEX;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmYResolution(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmYResolution;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmYResolution(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmYResolution = val;
	DMSTR(ptr)->dmFields |= DM_YRESOLUTION;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmTTOption(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmTTOption;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmTTOption(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmTTOption = val;
	DMSTR(ptr)->dmFields |= DM_TTOPTION;
}

JNIEXPORT jshort JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_getDmCollate(JNIEnv * env, jclass c, jlong ptr) {
	return DMSTR(ptr)->dmCollate;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_setDmCollate(JNIEnv * env, jclass c, jlong ptr, jshort val) {
	DMSTR(ptr)->dmCollate = val;
	DMSTR(ptr)->dmFields |= DM_COLLATE;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_DevmodeStructWrapper_releaseStruct(JNIEnv * env, jclass c, jlong ptr) {
	free(DMSTR(ptr));
}
