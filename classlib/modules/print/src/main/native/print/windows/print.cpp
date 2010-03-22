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
 * @author Aleksei V. Ivaschenko 
 */ 

/*
 * This file contains implementations of native methods of
 * GDIClient and PSInterpreter.
 * Defines the entry point for the DLL application.
 */

#include "print.h"
#include "org_apache_harmony_x_print_GDIClient.h"
#include "org_apache_harmony_x_print_PSInterpreter.h"
#include "org_apache_harmony_x_print_Win32PrintServiceProvider.h"

BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
                     )
{
    return TRUE;
}

//----------------- ServiceFinder functions -----------------------------------

/*
 * TODO:   support older systems.
 */
char * GetDefaultPrintService() {
    char *buffer = new char[MAX_PATH];
    DWORD buffer_size = MAX_PATH;

    if (GetDefaultPrinter(buffer, &buffer_size)) {
        return buffer;
    } else {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            delete[] buffer;
            buffer = new char[buffer_size];
            if (GetDefaultPrinter(buffer, &buffer_size)) {
                return buffer;
            }
        }
    }
    return NULL;
}

char ** GetPrintServices(DWORD *count) {
    unsigned char * pPrinterEnum = NULL;
    DWORD cbNeeded, cReturned;

    if (!EnumPrinters(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS,
                      NULL, 4, pPrinterEnum, 0, &cbNeeded, &cReturned)) {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            pPrinterEnum = new unsigned char[cbNeeded];
            if (EnumPrinters(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS,
                             NULL, 4, pPrinterEnum, cbNeeded, &cbNeeded, &cReturned)) {
                PRINTER_INFO_4 *printers = (PRINTER_INFO_4 *)pPrinterEnum;
                char ** names = new char *[cReturned];
                for (DWORD i = 0; i < cReturned; i++) {
                    names[i] = new char[strlen(printers[i].pPrinterName) + 1];
                    strcpy(names[i], printers[i].pPrinterName);
                }
                *count = cReturned;
                return names;
            }
        }
    }
    return NULL;
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_x_print_Win32PrintServiceProvider_findPrintServices
(JNIEnv *env, jclass owner) {
    jobjectArray result = NULL;
    DWORD services = 0;
    char ** names = GetPrintServices(&services);
    if (services > 0) {
        jclass jstring_class = env->FindClass("java/lang/String");
        result = env->NewObjectArray(services, jstring_class, jstring());
        for (DWORD i = 0; i < services; i++) {
            jstring name = env->NewStringUTF(names[i]); 
            env->SetObjectArrayElement(result, i, name);
            env->DeleteLocalRef(name);
        }
    }
    return result;
}

JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_Win32PrintServiceProvider_findDefaultPrintService
(JNIEnv *env, jclass owner) {
    char *serviceUTF = GetDefaultPrintService();
    jstring service = NULL;
    if (serviceUTF != NULL) {
        service = env->NewStringUTF(serviceUTF);
        delete[] serviceUTF;
    }
    return service;
}

//----------------- PrintService flavors --------------------------------------

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_checkPostScript
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *printer = env->GetStringUTFChars(printerName, 0);
    jboolean res = JNI_FALSE;

    HDC prndc = CreateDC(NULL, printer, NULL, NULL);
    if (prndc != NULL) {
        DWORD esc = POSTSCRIPT_PASSTHROUGH;
        if (ExtEscape(prndc, QUERYESCSUPPORT, sizeof(esc), (LPCSTR)&esc, 0, NULL) != 0) {
            return JNI_TRUE;
        }
        esc = GETTECHNOLOGY;
        if (ExtEscape(prndc, QUERYESCSUPPORT, sizeof(esc), (LPCSTR)&esc, 0, NULL) != 0) {
            char *tech = new char[MAX_PATH + 1];
            if (ExtEscape(prndc, esc, 0, NULL, MAX_PATH, tech) > 0) {
                char *lower = strlwr(tech);
                if (strstr(lower, "postscript") != NULL) {
                    return JNI_TRUE;
                }
            }
        }
    }
    DeleteDC(prndc);

    return res;
}

//----------------- PrintService attribute functions --------------------------

PRINTER_INFO_2 *GetPrinterInfo(const char *name) {
    PRINTER_INFO_2 *result = NULL;
    char *editableName = new char[strlen(name) + 1];
    strcpy(editableName, name);

    HANDLE hPrinter;
    if (OpenPrinter(editableName, &hPrinter, NULL)) {
        unsigned char *buffer = NULL;
        DWORD cbNeeded;
        if (!GetPrinter(hPrinter, 2, buffer, 0, &cbNeeded)) {
            if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
                buffer = new unsigned char[cbNeeded];
                if (GetPrinter(hPrinter, 2, buffer, cbNeeded, &cbNeeded)) {
                    result = (PRINTER_INFO_2 *)buffer;
                }
            }
        }
    }
    delete[] editableName;
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_getColorSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jboolean result = JNI_FALSE;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        if (DeviceCapabilities(name, info->pPortName, DC_COLORDEVICE, NULL, NULL) == 1) {
            result = JNI_TRUE;
        }
        free(info);
    }
    return result;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_GDIClient_getPagesPerMinute
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jint result = 0;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        info->pDevMode->dmColor = DMCOLOR_MONOCHROME;
        result = DeviceCapabilities(name, info->pPortName, DC_PRINTRATEPPM, NULL, info->pDevMode);
        free(info);
    }
    return result;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_GDIClient_getPagesPerMinuteColor
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jint result = 0;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        info->pDevMode->dmColor = DMCOLOR_COLOR;
        result = DeviceCapabilities(name, info->pPortName, DC_PRINTRATEPPM, NULL, info->pDevMode);
        free(info);
    }
    return result;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_GDIClient_getPrinterIsAcceptingJobs
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jint result = -1;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        if (info->Status & (PRINTER_STATUS_ERROR |
                            PRINTER_STATUS_NO_TONER |
                            PRINTER_STATUS_NOT_AVAILABLE |
                            PRINTER_STATUS_OFFLINE |
                            PRINTER_STATUS_OUT_OF_MEMORY |
                            PRINTER_STATUS_OUTPUT_BIN_FULL |
                            PRINTER_STATUS_PAPER_JAM |
                            PRINTER_STATUS_PAGE_PUNT |
                            PRINTER_STATUS_PAPER_OUT |
                            PRINTER_STATUS_PAPER_PROBLEM |
                            PRINTER_STATUS_PAUSED |
                            PRINTER_STATUS_PENDING_DELETION |
                            PRINTER_STATUS_USER_INTERVENTION)) {
            result = 0;
        } else {
            result = 1;
        }
        free(info);
    }
    return result;
}

JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_GDIClient_getPrinterLocation
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jstring result = NULL;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        if (info->pLocation != NULL && strcmp(info->pLocation, "") != 0) {
            result = env->NewStringUTF(info->pLocation);
        }
        free(info);
    }
    return result;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_GDIClient_getQueuedJobCount
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jint result = -1;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        result = info->cJobs;
        free(info);
    }
    return result;
}

//----------------- Print request attribute functions -------------------------

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_GDIClient_getCopiesSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jint result = 0;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        result = DeviceCapabilities(name, info->pPortName, DC_COPIES, NULL, NULL);
        free(info);
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_getSidesSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jboolean result = JNI_FALSE;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        if (DeviceCapabilities(name, info->pPortName, DC_DUPLEX, NULL, NULL) == 1) {
            result = JNI_TRUE;
        }
        free(info);
    }
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_x_print_GDIClient_getMediaSizesSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jobjectArray result = NULL;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        int numSizes = DeviceCapabilities(name, info->pPortName, DC_PAPERSIZE, NULL, NULL);
        if (numSizes > 0) {
            POINT *sizes = new POINT[numSizes];
            int numSizes = DeviceCapabilities(name, info->pPortName, DC_PAPERSIZE, (LPSTR)sizes, NULL);
            if (numSizes > 0) {
                jclass IntArrayClass = env->FindClass("[I");
                result = env->NewObjectArray(numSizes, IntArrayClass, NULL);
                for (int i = 0; i < numSizes; i++) {
                    jintArray size = env->NewIntArray(2);
                    int *nativeArray = new int[2];
                    nativeArray[0] = sizes[i].x;
                    nativeArray[1] = sizes[i].y;
                    env->SetIntArrayRegion(size, 0, 2, (jint *)nativeArray);
                    env->SetObjectArrayElement(result, i, size);
                }
            }
        }

#ifdef PRINTING_DEBUG
        int count = DeviceCapabilities(name, info->pPortName, DC_PAPERS, NULL, NULL);
        if( count > 0 ) {
            printf("\n\n");
            WORD *papers = new WORD[count];
            count = DeviceCapabilities(name, info->pPortName, DC_PAPERS, (LPSTR)papers, NULL);
            printf("Found %d paper types.\n", count);
            for (int i = 0; i < count; i++) {
                printf("%d: %d\n", i+1, papers[i]);
            }
            delete [] papers;
        }
        
        int cnames = DeviceCapabilities(name, info->pPortName, DC_PAPERNAMES, NULL, NULL);
        if( cnames > 0 ) {
            printf("\n\n");
            char (*papernames)[64] = new char[cnames][64];
            cnames = DeviceCapabilities(name, info->pPortName, DC_PAPERNAMES, (LPSTR)papernames, NULL);
            printf("Found %d paper names.\n", cnames);
            for (int i = 0; i < cnames; i++) {
                printf("%d: %s\n", i+1, papernames[i]);
            }
            delete [] papernames;
        }
#endif /* PRINTING_DEBUG */
        free(info);
    }
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_x_print_GDIClient_getMediaNames
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jobjectArray result = NULL;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        int numNamesFirst = DeviceCapabilities(name, info->pPortName, DC_PAPERNAMES, NULL, NULL);
        if (numNamesFirst > 0) {
            char *buffer = new char[65 * numNamesFirst];
            int numNames = DeviceCapabilities(name, info->pPortName, DC_PAPERNAMES, (LPSTR)buffer, NULL);
            if (numNames == numNamesFirst) {
                jclass jstring_class = env->FindClass("java/lang/String");
                result = env->NewObjectArray(numNames, jstring_class, jstring());
                for (int i = 0; i < numNames; i++) {
                    char *name = buffer + i*64;
                    jstring jname = env->NewStringUTF(name); 
                    env->SetObjectArrayElement(result, i, jname);
                    env->DeleteLocalRef(jname);
                }
            }
            delete[] buffer;
        }
        free(info);
    }
    return result;
}

JNIEXPORT jintArray JNICALL Java_org_apache_harmony_x_print_GDIClient_getMediaIDs
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jintArray result = NULL;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        int numPapersFirst = DeviceCapabilities(name, info->pPortName, DC_PAPERS, NULL, NULL);
        if (numPapersFirst > 0) {
            WORD *ids = new WORD[numPapersFirst];
            int numPapers = DeviceCapabilities(name, info->pPortName, DC_PAPERS, (LPSTR)ids, NULL);
            if (numPapers == numPapersFirst) {
                result = env->NewIntArray(numPapers);
                int *intIDs = new int[numPapers];
                for (int i = 0; i < numPapers; i++) {
                    intIDs[i] = (int)ids[i];
                }
                env->SetIntArrayRegion(result, 0, numPapers, (jint *)intIDs);
                delete[] intIDs;
            }
            delete[] ids;
        }
        free(info);
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_getCollateSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jboolean result = JNI_FALSE;

    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        if (DeviceCapabilities(name, info->pPortName, DC_COLLATE, NULL, NULL) == 1) {
            result = JNI_TRUE;
        }
        free(info);
    }
    return result;
}


JNIEXPORT jintArray JNICALL Java_org_apache_harmony_x_print_GDIClient_getResolutionsSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jintArray resolutions = NULL;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        int count = DeviceCapabilities(name, info->pPortName, DC_ENUMRESOLUTIONS, NULL, NULL);
        if (count > 0) {
            LONG *nativeArray = new LONG[count * 2];
            resolutions = env->NewIntArray(count * 2);
            if (DeviceCapabilities(name, info->pPortName, DC_ENUMRESOLUTIONS, (LPSTR)nativeArray, NULL) > 0) {
                jint *intArray = new jint[count * 2];
                for (int i = 0; i < count * 2; i++) {
                    intArray[i] = (jint)((int)nativeArray[i]);
                }
                env->SetIntArrayRegion(resolutions, 0, count * 2, intArray);     
                delete[] intArray;
            }
            delete[] nativeArray;
        }
        free(info);
    }
    return resolutions;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_getOrientationSupported
(JNIEnv *env, jclass owner, jstring printerName) {
    const char *name = env->GetStringUTFChars(printerName, 0);
    jboolean result = JNI_FALSE;
    
    PRINTER_INFO_2 *info = GetPrinterInfo(name);
    if (info != NULL) {
        int orientation = DeviceCapabilities(name, info->pPortName, DC_ORIENTATION, NULL, NULL);
        if (orientation == 0 || orientation == 90 || orientation == 270) {
            result = JNI_TRUE;
        }
        free(info);
    }
    return result;
}

//----------------- Common DocPrintJob and PS interpreter structures ----------

#define ATTRIBUTE_UNION_SIZE    8

typedef struct _PrintRequestAttributes {
    int Copies;
    int Sides;
    int PaperID;
    int Collate;
    int Chromaticity;
    int Orientation;
    int XResolution;
    int YResolution;
} PrintRequestAttributes;

typedef union _AttributesUnion {
    int numbers[ATTRIBUTE_UNION_SIZE];
    PrintRequestAttributes fields;
} AttributesUnion;

//----------------- DocPrintJob functions -------------------------------------

#ifdef PRINTING_DEBUG
FILE *file = NULL;
#endif /* PRINTING_DEBUG */

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_GDIClient_startDocPrinter
(JNIEnv *env, jclass owner, jstring printerName, jintArray printAttributes, jstring jobName, jstring destination) {
    const char *printer = env->GetStringUTFChars(printerName, 0);
    const char *jn = env->GetStringUTFChars(jobName, 0);
    char *jname = new char[strlen(jn) + 1];
    char *fname = NULL;
    strcpy(jname, jn);
    if (destination != NULL) {
        const char *fn = env->GetStringUTFChars(destination, 0);
        fname = new char[strlen(fn) + 1];
        strcpy(fname, fn);
    }
    AttributesUnion attributes;
    jboolean isCopy = JNI_FALSE;
    int *numbers = (int *)env->GetIntArrayElements(printAttributes, &isCopy);
    for (int i = 0; i < ATTRIBUTE_UNION_SIZE; i++) {
        attributes.numbers[i] = numbers[i];
    }

    jint res = 0;
    HANDLE hPrinter = NULL;

    if (OpenPrinter((LPTSTR)printer, &hPrinter, NULL) != 0) {
        DOC_INFO_1 dinfo;
        dinfo.pDocName = jname;
        dinfo.pDatatype = "RAW";
        dinfo.pOutputFile = fname;

        DEVMODE *pDevMode = NULL;
        int size = DocumentProperties(NULL, hPrinter, NULL, pDevMode, NULL, 0);
        pDevMode = (DEVMODE *)malloc(size);
//        if (DocumentProperties(NULL, hPrinter, NULL, pDevMode, NULL, DM_PROMPT) == IDOK) {
        if (DocumentProperties(NULL, hPrinter, NULL, pDevMode, NULL, DM_OUT_BUFFER) == IDOK) {

#ifdef PRINTING_DEBUG
            printf("DEVMODE received successfuly!\n");
#endif /* PRINTING_DEBUG */

            if (attributes.fields.Copies > 0) {
                pDevMode->dmCopies = (short)attributes.fields.Copies;
                pDevMode->dmFields |= DM_COPIES;
            }
            if (attributes.fields.Sides > 0) {
                pDevMode->dmDuplex = (short)attributes.fields.Sides;
                pDevMode->dmFields |= DM_DUPLEX;
            }
            if (attributes.fields.PaperID > 0) {
                pDevMode->dmPaperSize = (short)attributes.fields.PaperID;
                pDevMode->dmFields |= DM_PAPERSIZE;
            }
            if (DocumentProperties(NULL, hPrinter, NULL,
                                   pDevMode, pDevMode,
                                   DM_IN_BUFFER | DM_OUT_BUFFER) == IDOK) {
            }
        } else {

#ifdef PRINTING_DEBUG
            printf("Can't get initial DEVMODE.\n");
#endif /* PRINTING_DEBUG */

        }

        if (StartDocPrinter(hPrinter, 1, (LPBYTE)&dinfo) != 0) {
            res = *((int *)&hPrinter);
        }
    }

#ifdef PRINTING_DEBUG
    if (res == 0) {
        DWORD error = GetLastError();
        printf("Error occurred while starting: %d.\n", error);
    }
    if (res != 0) {
        file = fopen("C:\\from_Native.ps", "wb+");
    }
#endif /* PRINTING_DEBUG */

    return res;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_writePrinter
(JNIEnv *env, jclass owner, jbyteArray array, jint size, jint printerID) {
    HANDLE hPrinter = *((HANDLE *)&printerID);
    if (hPrinter != NULL) {
        signed char *data;
        data = (signed char *)env->GetByteArrayElements(array, NULL);
#ifdef PRINTING_DEBUG
        if (file != NULL) {
            fwrite(data, sizeof(signed char), size, file);
        }
#endif /* PRINTING_DEBUG */
        DWORD written = 0;
        if (WritePrinter(hPrinter, data, size, &written) != 0) {
            env->ReleaseByteArrayElements(array, (jbyte *)data, JNI_ABORT);
            return JNI_TRUE;
        }
    }
#ifdef PRINTING_DEBUG
    if (res == JNI_FALSE) {
        DWORD error = GetLastError();
        printf("Error occurred while transfering data to printer: %d.\n", error);
    }
#endif /* PRINTING_DEBUG */
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_GDIClient_endDocPrinter
(JNIEnv *env, jclass owner, jint printerID) {
    HANDLE hPrinter = *((HANDLE *)&printerID);
#ifdef PRINTING_DEBUG
    if (file != NULL) {
        fclose(file);
        file = NULL;
    }
#endif /* PRINTING_DEBUG */
    if (hPrinter != NULL) {
        if (EndDocPrinter(hPrinter) != 0) {
            ClosePrinter(hPrinter);
            hPrinter = NULL;
            return JNI_TRUE;
        }
    }
#ifdef PRINTING_DEBUG
    if (res == JNI_FALSE) {
        DWORD error = GetLastError();
        printf("Error occurred while ending: %d.\n", error);
    }
#endif /* PRINTING_DEBUG */
    return JNI_FALSE;
}

//----------------- Interpreter functions -------------------------------------

#define MAX_PRINTER_CONTEXTS 20
HDC printerDCs[] = { NULL, NULL, NULL, NULL, NULL,
                     NULL, NULL, NULL, NULL, NULL,
                     NULL, NULL, NULL, NULL, NULL,
                     NULL, NULL, NULL, NULL, NULL };

int psWidths[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
int psHeights[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

int getFreePDCID() {
    for (int i = 0; i < MAX_PRINTER_CONTEXTS; i++) {
        if (printerDCs[i] == NULL) {
            return i;
        }
    }
    return -1;
}

int obtainPrinterDC(const char *printer, int psWidth, int psHeight) {
    int pdcID = getFreePDCID();
    if (pdcID < 0) {
        return -1;
    }
    printerDCs[pdcID] = CreateDC("WINSPOOL", printer, NULL, NULL);    // In win 9x/Me "WINSPOOL" should not be used.
    if (printerDCs[pdcID] == NULL) {
        return -1;
    }
    psWidths[pdcID] = psWidth;
    psHeights[pdcID] = psHeight;
    return pdcID;
}

void releasePrinterDC(const int pdcID) {
    if (pdcID >= 0) {
        DeleteDC(printerDCs[pdcID]);
        printerDCs[pdcID] = NULL;
        psWidths[pdcID] = 0;
        psHeights[pdcID] = 0;
    }
}

void setAttributes(const char *printer, const int pdcID, AttributesUnion attributes) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        DEVMODE *pDevMode = NULL;
        HANDLE hPrinter;
        if (OpenPrinter((LPTSTR)printer, &hPrinter, NULL) != 0) {
            int size = DocumentProperties(NULL, hPrinter, NULL, pDevMode, NULL, 0);
            pDevMode = (DEVMODE *)malloc(size);
            if (DocumentProperties(NULL, hPrinter, NULL, pDevMode, NULL, DM_OUT_BUFFER) == IDOK) {
                pDevMode->dmFields = 0;
                if (attributes.fields.PaperID > 0) {
                    pDevMode->dmPaperSize = (short)attributes.fields.PaperID;
                    pDevMode->dmFields |= DM_PAPERSIZE;
                }

                if (attributes.fields.Copies > 0) {
                    pDevMode->dmCopies = (short)attributes.fields.Copies;
                    pDevMode->dmFields |= DM_COPIES;
                }

                if (attributes.fields.Collate > 0) {
                    if (attributes.fields.Collate == 1) {
                        pDevMode->dmCollate = DMCOLLATE_TRUE;
                    } else {
                        pDevMode->dmCollate = DMCOLLATE_FALSE;
                    }
                    pDevMode->dmFields |= DM_COLLATE;
                }

                if (attributes.fields.Sides > 0) {
                    pDevMode->dmDuplex = (short)attributes.fields.Sides;
                    pDevMode->dmFields |= DM_DUPLEX;
                }

                if (attributes.fields.Chromaticity > 0) {
                    if (attributes.fields.Chromaticity == 1) {
                        pDevMode->dmColor = DMCOLOR_MONOCHROME;
                    } else {
                        pDevMode->dmColor = DMCOLOR_COLOR;
                    }
                    pDevMode->dmFields |= DM_COLOR;
                }

                if (attributes.fields.Orientation > 0) {
                    if (attributes.fields.Orientation == 2) {
                        pDevMode->dmOrientation = DMORIENT_LANDSCAPE;
                    } else {
                        pDevMode->dmOrientation = DMORIENT_PORTRAIT;
                    }
                    pDevMode->dmFields |= DM_ORIENTATION;
                }

                if (attributes.fields.XResolution > 0) {
                    pDevMode->dmPrintQuality = (short)attributes.fields.XResolution;
                    pDevMode->dmFields |= DM_PRINTQUALITY;
                }

                if (attributes.fields.YResolution > 0) {
                    pDevMode->dmYResolution = (short)attributes.fields.YResolution;
                    pDevMode->dmFields |= DM_YRESOLUTION;
                }

                if (pDevMode->dmFields != 0) {
#ifdef PRINTING_DEBUG
                    printf("Setting up print attributes.\n");
#endif /* PRINTING_DEBUG */
                    ResetDC(pdc, pDevMode);
                }
            }
            ClosePrinter(hPrinter);
        }
    }
}

int startDocument(const int pdcID, const char *jname, const char *fname) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        DOCINFO info;
        info.cbSize = sizeof(DOCINFO);
        
        info.lpszDocName = jname;
        info.lpszDatatype = NULL;
        info.lpszOutput = fname;
        info.fwType = 0;
        if (StartDoc(pdc, &info) > 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int startPage(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (StartPage(pdc) > 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int finishPage(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (EndPage(pdc) > 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int finishDocument(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (EndDoc(pdc) > 0) {
            releasePrinterDC(pdcID);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int transDoubleX(const int pdcID, jdouble x) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        double width = (double)GetDeviceCaps(pdc, HORZRES);
        double k = width / psWidths[pdcID];
        return (int)(x * k);
    }
    return (int)x;
}

int transDoubleY(const int pdcID, jdouble y) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        double height = (double)GetDeviceCaps(pdc, VERTRES);
        double k = height / psHeights[pdcID];
        return (int)(y * k);
    }
    return (int)y;
}

int client_LineTo(const int pdcID, int x, int y) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (LineTo(pdc, x, y) > 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int client_MoveTo(const int pdcID, int x, int y) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (MoveToEx(pdc, x, y, NULL) > 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int client_Arc(const int pdcID, int x, int y, int r, float alpha, float beta) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (AngleArc(pdc, x, y, r, alpha, beta) > 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int client_SetColor(const int pdcID, double red, double green, double blue) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        int r = (int)(255.0 * red);
        int g = (int)(255.0 * green);
        int b = (int)(255.0 * blue);
        HPEN pen = CreatePen(PS_SOLID, 0, RGB(r, g, b));
        HPEN oldPen = (HPEN)SelectObject(pdc, pen);
        if (oldPen != NULL) {
            DeleteObject(oldPen);
            HBRUSH brush = CreateSolidBrush(RGB(r, g, b));
            HBRUSH oldBrush = (HBRUSH)SelectObject(pdc, brush);
            if (oldBrush != NULL) {
                DeleteObject(oldBrush);
                if (SetTextColor(pdc, RGB(r, g, b)) != CLR_INVALID) {
                    return JNI_TRUE;
                }
            }
        }
    }
    return JNI_FALSE;
}

int client_BeginPath(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (BeginPath(pdc)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int client_ClosePath(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (CloseFigure(pdc)) {
            if (EndPath(pdc)) {
                return JNI_TRUE;
            }
        }
    }
    return JNI_FALSE;
}

int client_StrokePath(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (StrokePath(pdc)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int client_FillPath(const int pdcID) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        if (FillPath(pdc)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

int client_DrawImage(const int pdcID, int x, int y, int sw, int sh, int *data, int w, int h) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        HDC screen = GetDC(NULL);
        HDC bmpdc = CreateCompatibleDC(screen);
        HBITMAP bmp = NULL;
        if ((bmp = CreateBitmap(w, h, 1, 32, (void *)data)) != NULL) {
            SelectObject(bmpdc, bmp);
//            SetStretchBltMode(pdc, COLORONCOLOR);
            if (StretchBlt(pdc, x, y,
                sw, sh,
                bmpdc, 0, 0, w, h, SRCCOPY) != 0) {
                DeleteObject(bmp);
                DeleteDC(bmpdc);
                return JNI_TRUE;
            }
        }
    }
    return JNI_FALSE;
}

int client_DrawText(const int pdcID, const char *text) {
    HDC pdc = printerDCs[pdcID];
    if (pdc != NULL) {
        POINT point;
        if (MoveToEx(pdc, 0, 0, &point)) {
            if (TextOut(pdc, point.x, point.y, text, (int)strlen(text))) {
                if (MoveToEx(pdc, point.x, point.y, NULL)) {
                    return JNI_TRUE;
                }
            }
        }
    }
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_x_print_PSInterpreter_obtainServiceID
(JNIEnv *env, jclass owner, jstring printerName, jint width, jint height) {
    const char *printer = env->GetStringUTFChars(printerName, 0);
    return obtainPrinterDC(printer, (int)width, (int)height);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_x_print_PSInterpreter_releaseServiceID
(JNIEnv *env, jclass owner, jint serviceID) {
    releasePrinterDC((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_startDocument
(JNIEnv *env, jclass owner, jstring printerName, jint serviceID,
 jintArray printAttributes, jstring jobName, jstring destination) {
    const char *printer = env->GetStringUTFChars(printerName, 0);
    const char *jname = env->GetStringUTFChars(jobName, 0);
    const char *fname = NULL;
    if (destination != NULL) {
        fname = env->GetStringUTFChars(destination, 0);
    }
    AttributesUnion attributes;
    jboolean isCopy = JNI_FALSE;
    int *numbers = (int *)env->GetIntArrayElements(printAttributes, &isCopy);
    for (int i = 0; i < ATTRIBUTE_UNION_SIZE; i++) {
        attributes.numbers[i] = numbers[i];
    }
    setAttributes(printer, (int)serviceID, attributes);
    return (jboolean)startDocument((int)serviceID, jname, fname);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_startPage
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)startPage((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_endPage
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)finishPage((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_endDocument
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)finishDocument((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_setRGBColor
(JNIEnv *env, jclass owner, jint serviceID, jdouble red, jdouble green, jdouble blue) {
    return (jboolean)client_SetColor((int)serviceID, (double)red, (double)green, (double)blue);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_moveTo
(JNIEnv *env, jclass owner, jint serviceID, jdouble x, jdouble y) {
    return (jboolean)client_MoveTo((int)serviceID, transDoubleX(serviceID, x), transDoubleY(serviceID, psHeights[(int)serviceID] - y));
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_lineTo
(JNIEnv *env, jclass owner, jint serviceID, jdouble x, jdouble y) {
    return (jboolean)client_LineTo((int)serviceID, transDoubleX(serviceID, x), transDoubleY(serviceID, psHeights[(int)serviceID] - y));
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_drawArc
(JNIEnv *env, jclass owner, jint serviceID, jdouble x, jdouble y, jdouble r, jdouble alpha, jdouble beta) {
    return (jboolean)client_Arc((int)serviceID, transDoubleX(serviceID, x), transDoubleY(serviceID, psHeights[(int)serviceID] - y),
        transDoubleX(serviceID, r), (float)alpha, (float)beta);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_beginPath
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)client_BeginPath((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_closePath
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)client_ClosePath((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_strokePath
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)client_StrokePath((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_fillPath
(JNIEnv *env, jclass owner, jint serviceID) {
    return (jboolean)client_FillPath((int)serviceID);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_drawImage
(JNIEnv *env, jclass owner, jint serviceID, jdouble x, jdouble y,
        jdouble sw, jdouble sh, jintArray data, jint w, jint h) {
    int *bits;
    if (data != NULL) {
        bits = (int *)env->GetIntArrayElements(data, NULL);
    } else {
        return JNI_FALSE;
    }
    jboolean result = (jboolean)client_DrawImage((int)serviceID,
        transDoubleX(serviceID, x), transDoubleY(serviceID, psHeights[(int)serviceID] - y),
        transDoubleX(serviceID, sw), transDoubleY(serviceID, sh),
        bits, (int)w, (int)h);
    env->ReleaseIntArrayElements(data, (jint *)bits, JNI_ABORT);
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_drawText
(JNIEnv *env, jclass owner, jint serviceID, jstring text) {
    const char *string = env->GetStringUTFChars(text, 0);
    return client_DrawText((int)serviceID, string);
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_clipPath
(JNIEnv *env, jclass owner, jint serviceID) {
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_rotate
(JNIEnv *env, jclass owner, jint serviceID, jdouble alpha) {
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_x_print_PSInterpreter_setFont
(JNIEnv *env, jclass owner, jint serviceID, jstring font) {
    const char *name = env->GetStringUTFChars(font, 0);
    return JNI_TRUE;
}
