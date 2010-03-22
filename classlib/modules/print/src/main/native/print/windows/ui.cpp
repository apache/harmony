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
 * @author Irina A. Arkhipets 
 */ 

// 
// ui.cpp : Defines the entry point for the DLL application.
// Created at January, 20, 2005
// Author - Irina Arkhipets
//
// Things to do - update changed printing attributes in 
// java.awt.PrinterJob class
// Waiting while all attrs supported by our product &
// native Alexey's native function for getting 
// java attributes from the printer DEVMODE structure
//

//#include "stdafx.h"
#include <Windows.h>

#include "org_apache_harmony_x_print_awt_PSPrinterJob.h"

#include <commdlg.h>
#include <wingdi.h>
#include <stdlib.h>
#include <Windef.h>
#include <winspool.h>

BOOL getPrinterStructures(char*, HGLOBAL*, HGLOBAL*);

//-----------------------------------------------------------------------------

// This function calls PrintDlg Windows GDI API function which shows 
// standard native Windows dialog for the printer selecting.
//
// Returns: 
// Printer name if user clicked OK
// NULL if user cancelled the dialog
//
// If user clicked OK, this function also calls setCopies(int) method 
// of the DefaultPrinterJob class
// Please, update this function if you need to change some other attributes
// (for example, probably we need to set Collate attribute)
//

JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_DefaultPrinterJob_getPrinter
(JNIEnv * env, jobject obj, jstring service, jint copies) {
    PRINTDLG pd;
    HWND hwnd=NULL;
    char *prnName=NULL;

    ZeroMemory(&pd, sizeof(pd));
    pd.lStructSize = sizeof(pd);
    pd.hwndOwner   = hwnd;
    pd.hDevNames   = NULL;    
    pd.hDevMode    = NULL;    
    pd.Flags       = PD_RETURNDC | PD_HIDEPRINTTOFILE | 
                     PD_NOPAGENUMS | PD_NOSELECTION; 
    pd.nCopies     = (WORD)copies;

    // Get correct DEVMODE and DEVNAMES structures for the selected printer
    getPrinterStructures((char *)env->GetStringUTFChars(service,0),
        &(pd.hDevNames),&(pd.hDevMode));

    if (PrintDlg(&pd)==TRUE) {
        jclass jCls = (env)->GetObjectClass(obj);
        jmethodID jMetID = env->GetMethodID(jCls, "setCopies", "(I)V"); 
        LPDEVNAMES lpdn = (LPDEVNAMES)GlobalLock(pd.hDevNames);
        prnName=(char*)
            (calloc(lpdn->wOutputOffset-lpdn->wDeviceOffset+1,sizeof(char)));
        if(prnName!=NULL) {
            LPDEVMODE dm = (LPDEVMODE)GlobalLock(pd.hDevMode);
            env->CallVoidMethod(obj,jMetID, dm->dmCopies);
            GlobalUnlock(pd.hDevMode); 
            CopyMemory((LPBYTE)prnName, ((LPBYTE)lpdn+lpdn->wDeviceOffset),
                lpdn->wOutputOffset-lpdn->wDeviceOffset);
        }

        DeleteDC(pd.hDC);
        GlobalUnlock(pd.hDevNames); 
        GlobalFree(pd.hDevNames); 
        GlobalFree(pd.hDevMode);   
        return env->NewStringUTF(prnName);
    }
    
    return NULL;
}

// This is duplicate for PSPrinterJob class
JNIEXPORT jstring JNICALL Java_org_apache_harmony_x_print_awt_PSPrinterJob_getPrinter
(JNIEnv * env, jobject obj, jstring service, jint copies) {
    return Java_org_apache_harmony_x_print_DefaultPrinterJob_getPrinter(env, obj, service, copies);
}

//-----------------------------------------------------------------------------

// returns DEVMODE and DEVNAMES for the printer
BOOL getPrinterStructures(char* printerName, 
                          HGLOBAL* devNames, HGLOBAL* devMode) {
    HANDLE printerHandle;
    PRINTER_INFO_2 *printerInfo = NULL;

    // Open printer
    if (!OpenPrinter(printerName, &printerHandle, NULL)) 
        return FALSE;
    
    // Get PRINTER_INFO_STRUCTURE size
    DWORD byteNeeded, byteUsed;
    GetPrinter(printerHandle, 2, NULL, 0, &byteNeeded);
    if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
        ClosePrinter(printerHandle);
        return FALSE;
    }       

    // Allocate PRINTER_INFO_2 structure
    printerInfo = (PRINTER_INFO_2 *)malloc(byteNeeded);
    if (!printerInfo){
        ClosePrinter(printerHandle);
        return FALSE;
    }

    // Get PRINTER_INFO_2 structure for the printer
    if (!GetPrinter(printerHandle, 2, 
        (unsigned char *)printerInfo, byteNeeded, &byteUsed)) {
        free(printerInfo);
        ClosePrinter(printerHandle);
        return FALSE;
    }

    // Close printer
    ClosePrinter(printerHandle);

    // Allocate global handle for the DEVMODE structure
    HGLOBAL hDevMode = GlobalAlloc(GHND, sizeof(*printerInfo->pDevMode) +
        printerInfo->pDevMode->dmDriverExtra);
    if(hDevMode==NULL) {
        free(printerInfo);
        return FALSE;
    }   

    // Cope DEVMODE from PRINTER_INFO_2 structure to the allocated memory
    DEVMODE* pDevMode = (DEVMODE*)GlobalLock(hDevMode);
    memcpy(pDevMode, printerInfo->pDevMode, 
        sizeof(*printerInfo->pDevMode) + printerInfo->pDevMode->dmDriverExtra);
    GlobalUnlock(hDevMode);

    // Driver name length in chars
    DWORD driverNameLen = lstrlen(printerInfo->pDriverName)+1;
    // Printer name length
    DWORD printerNameLen = lstrlen(printerInfo->pPrinterName)+1;
    // Port name length
    DWORD portNameLen = lstrlen(printerInfo->pPortName)+1;

    // Allocate global handle for the DEVNAMES
    HGLOBAL hDevNames = GlobalAlloc(GHND, sizeof(DEVNAMES)+
        (driverNameLen + printerNameLen + portNameLen)*sizeof(TCHAR));
    if(hDevNames==NULL) {
        free(printerInfo);
        GlobalFree(hDevMode);
        return FALSE;
    }

    // Current offset for the names fields in DEVNAMES
    int offset = sizeof(DEVNAMES);

    DEVNAMES* pDevNames=(DEVNAMES*)GlobalLock(hDevNames);

    // Copy driver name to global DEVNAMES
    pDevNames->wDriverOffset = offset;
    memcpy((LPSTR)pDevNames+offset, 
        printerInfo->pDriverName, driverNameLen*sizeof(TCHAR));
    offset+=driverNameLen*sizeof(TCHAR);

    // Copy printer name to global DEVNAMES
    pDevNames->wDeviceOffset=offset;
    memcpy((LPSTR)pDevNames+offset, 
        printerInfo->pPrinterName, printerNameLen*sizeof(TCHAR));
    offset+=printerNameLen;

    // Copy port name to global DEVNAMES
    pDevNames->wOutputOffset=offset;
    memcpy((LPSTR)pDevNames+offset, 
        printerInfo->pPortName, portNameLen*sizeof(TCHAR));

    // Default printer flag
    pDevNames->wDefault = 0;

    GlobalUnlock(hDevNames);
    free(printerInfo);

    *devMode = hDevMode;
    *devNames = hDevNames;
    
    return TRUE;
}
