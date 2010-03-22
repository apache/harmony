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
#include "win_printer_factory.h"
#include <windows.h>
#include <stdio.h>

// Cast pointer to jlong
#define PCAST(p) (jlong) (signed long) p
// Cast jlong to pointer
#define JCAST(jlongVal, type) (type) (unsigned long) jlongVal
#define TOHDC(jlongVal) JCAST(jlongVal, HDC)

// ----------------   Error handlers   -----------------------
static char * getFileName(const char * path) {
	char * p = (char *) (path + strlen(path));

	for (; p > path; p--) {
		if ((*p == '/') || (*p == '\\')) {
			p++;
			return p;
		}
	}

	return NULL;
}

static void handleError(const char * func, const char * file, unsigned line,
		JNIEnv * env, char * msg) {
	const char * format = "%s(%s:%i): %s";
	char buff[512];

	sprintf(buff, format, func, getFileName(file), line, msg);
	(*env)->ThrowNew(env, (*env)->FindClass(env, "javax/print/PrintException"), buff);
}

static void handleLastError(const char * func, const char * file,
		unsigned line, JNIEnv * env) {
	char * msg;

	FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
	FORMAT_MESSAGE_IGNORE_INSERTS, NULL, GetLastError(),
	MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (char *) &msg, 0, NULL);

	handleError(func, file, line, env, msg);
}

// ----------------   Auxiliary functions   -----------------

static void * getPrinterInfo(HANDLE handle, DWORD level) {
	void * info;
	DWORD size;

	// Calculate buffer size
	GetPrinterW(handle, level, NULL, 0, &size);
	info = calloc(1, size);

	if (info == NULL) {
		SetLastError(ERROR_NOT_ENOUGH_MEMORY);
		return NULL;
	}

	if (!GetPrinterW(handle, level, (LPBYTE) info, size, &size)) {
		return NULL;
	}

	return info;
}

static DWORD getPrinterCapabilities(JNIEnv * env, jlong handle, WORD query,
		void * output) {
	DWORD result;
	PRINTER_INFO_2W * info = (PRINTER_INFO_2W *) getPrinterInfo(JCAST(handle, HANDLE), 2);

	if (info == NULL) {
		SetLastError(ERROR_NOT_ENOUGH_MEMORY);
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
		return -1;
	}

	result = DeviceCapabilitiesW((*info).pPrinterName, (*info).pPortName, query, output, NULL);
	free(info);
	return result;
}

// ----------------   JNI functions   -----------------------

JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getDefaultPrinterName(JNIEnv * env, jclass c) {
	jstring name = NULL;
	unsigned short buff[1024];
	unsigned int i;
	unsigned int size = GetProfileStringW(L"windows", L"device", L",,,", buff,
			sizeof(buff) / sizeof(unsigned short));

	for (i = 0; i < size; i++) {
		if (buff[i] == L',') {
			if (i > 0) {
				name = (*env)->NewString(env, buff, i);
			}
			break;
		}
	}

	return name;
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getConnectedPrinterNames(JNIEnv * env, jclass c) {
	DWORD i;
	DWORD cbNeeded;
	DWORD cReturned;
	PRINTER_INFO_4W * printers;
	jobjectArray result;

	// Compute buffer size
	EnumPrintersW(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS, NULL, 4,
			NULL, 0, &cbNeeded, &cReturned);

	if (cbNeeded == 0) {
		handleError(__FUNCTION__,__FILE__ , __LINE__, env, "Unknown buffer size");
		return NULL;
	}

	printers = (PRINTER_INFO_4W *) calloc(1, cbNeeded);

	if (printers == NULL) {
		SetLastError(ERROR_NOT_ENOUGH_MEMORY);
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
		return NULL;
	}

	if (EnumPrintersW(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS, NULL, 4,
					(LPBYTE) printers, cbNeeded, &cbNeeded, &cReturned)) {
		result = (*env)->NewObjectArray(env, cReturned,
				(*env)->FindClass(env, "java/lang/String"), NULL);

		for (i = 0; i < cReturned; i++) {
			(*env)->SetObjectArrayElement(env, result, i,
					(*env)->NewString(env, printers[i].pPrinterName,
							wcslen(printers[i].pPrinterName)));
		}
	} else {
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
	}

	free(printers);
	return result;
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterHandle(JNIEnv * env, jclass c, jstring jprinterName) {
	HANDLE handle;
	const unsigned short * printerName = (*env)->GetStringChars(env, jprinterName, NULL);

	if (!OpenPrinterW((unsigned short *) printerName, &handle, NULL)) {
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
	}

	(*env)->ReleaseStringChars(env, jprinterName, printerName);
	return PCAST(handle);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_releasePrinterHandle(JNIEnv * env, jclass c, jlong handle) {
	if (!ClosePrinter(JCAST(handle, HANDLE))) {
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
	}
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterProps(JNIEnv * env, jclass c, jstring jprinterName, jlong handle) {
	DEVMODEW * dm;
	const unsigned short * printerName = (*env)->GetStringChars(env, jprinterName, NULL);
	LONG dmSize = DocumentPropertiesW(NULL, JCAST(handle, HANDLE),
			(unsigned short *) printerName, NULL, NULL, 0);

	if (dmSize < 0) {
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
		(*env)->ReleaseStringChars(env, jprinterName, printerName);
		return 0;
	}

	dm = calloc(1, dmSize);

	if (dm == NULL) {
		SetLastError(ERROR_NOT_ENOUGH_MEMORY);
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
		return 0;
	}

	if (DocumentPropertiesW(NULL, JCAST(handle, HANDLE),
					(unsigned short *) printerName, dm, NULL, DM_OUT_BUFFER) != IDOK) {
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
		return 0;
	}

	(*env)->ReleaseStringChars(env, jprinterName, printerName);
	return PCAST(dm);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterDC(JNIEnv * env, jclass c, jstring jprinterName, jlong pDevMode) {
	const unsigned short * printerName = (*env)->GetStringChars(env, jprinterName, NULL);
	HDC pdc = CreateDCW(L"WINSPOOL", printerName, NULL, JCAST(pDevMode, DEVMODEW *));

	(*env)->ReleaseStringChars(env, jprinterName, printerName);

	if (pdc == NULL) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
		return 0;
	}

	return PCAST(pdc);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_releasePrinterDC(JNIEnv * env, jclass c, jlong pdc) {
	if (!DeleteDC(TOHDC(pdc))) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_startDoc(JNIEnv * env, jclass c, jlong pdc, jstring jdocName, jstring jfilePath) {
	jint jobId;
	DOCINFOW docInfo = {0};

	docInfo.cbSize = sizeof(docInfo);
	docInfo.lpszDocName = (*env)->GetStringChars(env, jdocName, NULL);

	if (jfilePath != NULL) {
		docInfo.lpszOutput = (*env)->GetStringChars(env, jfilePath, NULL);
	}

	if ((jobId = (jint) StartDocW(TOHDC(pdc), &docInfo)) <= 0) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}

	(*env)->ReleaseStringChars(env, jdocName, docInfo.lpszDocName);

	if (jfilePath != NULL) {
		(*env)->ReleaseStringChars(env, jdocName, docInfo.lpszOutput);
	}

	return jobId;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_endDoc(JNIEnv * env, jclass c, jlong pdc) {
	if (EndDoc(TOHDC(pdc)) <= 0) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_startPage(JNIEnv * env, jclass c, jlong pdc) {
	if (StartPage(TOHDC(pdc)) <= 0) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_endPage(JNIEnv * env, jclass c, jlong pdc) {
	if (EndPage(TOHDC(pdc)) <= 0) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getQueuedJobs(JNIEnv * env, jclass c, jlong handle) {
	jint result;
	PRINTER_INFO_2W * info = (PRINTER_INFO_2W *) getPrinterInfo(JCAST(handle, HANDLE), 2);

	if (info == NULL) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
		return 0;
	}

	result = (jint) (*info).cJobs;
	free(info);

	return result;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPixelsPerInchX(JNIEnv * env, jclass c, jlong pdc) {
	return GetDeviceCaps(TOHDC(pdc), LOGPIXELSX);
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPixelsPerInchY(JNIEnv * env, jclass c, jlong pdc) {
	return GetDeviceCaps(TOHDC(pdc), LOGPIXELSY);
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPaperPhysicalWidth(JNIEnv * env, jclass c, jlong pdc) {
	return GetDeviceCaps(TOHDC(pdc), PHYSICALWIDTH);
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPaperPhysicalHeight(JNIEnv * env, jclass c, jlong pdc) {
	return GetDeviceCaps(TOHDC(pdc), PHYSICALHEIGHT);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getPrinterStatus(JNIEnv * env, jclass c, jlong handle) {
	jlong result;
	PRINTER_INFO_2W * info = (PRINTER_INFO_2W *) getPrinterInfo(JCAST(handle, HANDLE), 2);

	if (info == NULL) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
		return 0;
	}

	result = PCAST((*info).Status);
	free(info);
	return result;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_isColorPrintingSupported(JNIEnv * env, jclass c, jlong handle) {
	int result = getPrinterCapabilities(env, handle, DC_COLORDEVICE, NULL);

	if(result == -1) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
		return JNI_FALSE;
	} else if(result == 1) {
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_isCollatingSupported(JNIEnv * env, jclass c, jlong handle) {
	if(getPrinterCapabilities(env, handle, DC_COLLATE, NULL) == 1) {
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_isDuplexSupported(JNIEnv * env, jclass c, jlong handle) {
	if(getPrinterCapabilities(env, handle, DC_DUPLEX, NULL) == 1) {
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

JNIEXPORT jintArray JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getSupportedPaperSizes(JNIEnv * env, jclass c, jlong handle) {
	jintArray result;
	POINT * points;
	jint paper[2];
	unsigned i;
	DWORD size = getPrinterCapabilities(env, handle, DC_PAPERSIZE, NULL);

	if ((signed long) size == -1) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
		return NULL;
	}

	result = (*env)->NewIntArray(env, size * 2);
	points = (POINT *) calloc(sizeof(POINT), size);

	if ((result == NULL) || (points == NULL)) {
		SetLastError(ERROR_NOT_ENOUGH_MEMORY);
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
        free(points);
		return NULL;
	}

	if (getPrinterCapabilities(env, handle, DC_PAPERSIZE, points) == -1) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
        free(points);
		return NULL;
	}

	for(i = 0; i < size; i++) {
		paper[0] = (jint) points[i].x;
		paper[1] = (jint) points[i].y;
		(*env)->SetIntArrayRegion(env, result, i*2, 2, paper);
	}

	free(points);
	return result;
}

JNIEXPORT jintArray JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getSupportedResolutions(JNIEnv * env, jclass c, jlong handle) {
	jintArray result;
	jint * resolutions;
	LONG * buff;
	unsigned i;
	DWORD size = getPrinterCapabilities(env, handle, DC_ENUMRESOLUTIONS, NULL);

	if ((signed long) size == -1) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
		return NULL;
	}

	size *=2;
	result = (*env)->NewIntArray(env, size);
	buff = (LONG *) calloc(sizeof(LONG), size);
	resolutions = (jint *) calloc(sizeof(jint), size);

	if ((result == NULL) || (buff == NULL) || (resolutions == NULL)) {
		SetLastError(ERROR_NOT_ENOUGH_MEMORY);
		handleLastError(__FUNCTION__,__FILE__ , __LINE__, env);
        free(buff);
        free(resolutions);
		return NULL;
	}

	if (getPrinterCapabilities(env, handle, DC_ENUMRESOLUTIONS, buff) == -1) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
        free(buff);
        free(resolutions);
		return NULL;
	}

	for(i = 0; i < size; i++) {
		resolutions[i] = (jint) buff[i];
	}

	(*env)->SetIntArrayRegion(env, result, 0, size, resolutions);
	free(buff);
	free(resolutions);
	return result;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getLandscapeOrientationDegree(JNIEnv * env, jclass c, jlong handle) {
	return getPrinterCapabilities(env, handle, DC_ORIENTATION, NULL);
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_getMaxNumberOfCopies(JNIEnv * env, jclass c, jlong handle) {
	jint result = getPrinterCapabilities(env, handle, DC_COPIES, NULL);

	if (result == -1) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}
	return result;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_WinPrinterFactory_cancelPrinterJob(JNIEnv *env , jclass c, jlong handle, jint jobId) {
	if (!SetJobW(JCAST(handle, HANDLE), (DWORD) jobId, 0, NULL, JOB_CONTROL_DELETE)) {
		handleLastError(__FUNCTION__, __FILE__, __LINE__, env);
	}
}
