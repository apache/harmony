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
 * @author Pavel Dolgov
 */
#include <windows.h>

#include <atlbase.h>
#include <atlcom.h>
#include <atlcoll.h>

#include <oleidl.h>
#include <shlobj.h>
#include <MLang.h>

#include <stdio.h>

#pragma comment(lib, "gdi32.lib")

class AtlModule : public CAtlModuleT<AtlModule> {
} _AtlModule;

#include "WinDataTransfer.h"

// Data format names
static const char * FORMAT_TEXT = "text/plain";
static const char * FORMAT_FILE_LIST = "application/x-java-file-list";
static const char * FORMAT_URL = "application/x-java-url";
static const char * FORMAT_HTML = "text/html";
static const char * FORMAT_IMAGE = "image/x-java-image";
static const wchar_t * FORMAT_SERIALIZED = 
        L"application/x-java-serialized-object";

// Windows clipboard formats
UINT WinDataObject::cfShellUrlA = 0;
UINT WinDataObject::cfShellUrlW = 0;
UINT WinDataObject::cfHTML = 0;
FORMATETC WinDataObject::textFormats[] = {
        { CF_UNICODETEXT, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL },
        { CF_TEXT, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL } };
FORMATETC WinDataObject::fileListFormat =
        { CF_HDROP, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
FORMATETC WinDataObject::urlFormats[] = {
        { 0, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL },
        { 0, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL } };
FORMATETC WinDataObject::htmlFormat =
        { 0, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
FORMATETC WinDataObject::imageFormats[] = {
        { CF_DIB, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL },
        { CF_ENHMETAFILE, 0, DVASPECT_CONTENT, -1, TYMED_ENHMF } };

// Cached JNI data
static JavaVM * jvm = NULL;
static jclass classWinDataTransfer = NULL;
static jclass classString = NULL;

static jclass classDataSnapshot = NULL;
static jmethodID methodDataSnapshotGetNativeFormats = NULL;
static jmethodID methodDataSnapshotGetText = NULL;
static jmethodID methodDataSnapshotGetFileList = NULL;
static jmethodID methodDataSnapshotGetURL = NULL;
static jmethodID methodDataSnapshotGetHTML = NULL;
static jmethodID methodDataSnapshotGetRawBitmapHeader = NULL;
static jmethodID methodDataSnapshotGetRawBitmapBuffer8 = NULL;
static jmethodID methodDataSnapshotGetRawBitmapBuffer16 = NULL;
static jmethodID methodDataSnapshotGetRawBitmapBuffer32 = NULL;
static jmethodID methodDataSnapshotGetSerializedObject = NULL;

static jclass classWinDropTarget = NULL;
static jmethodID methodWinDropTargetDragEnter = NULL;
static jmethodID methodWinDropTargetDragLeave = NULL;
static jmethodID methodWinDropTargetDragOver = NULL;
static jmethodID methodWinDropTargetDrop = NULL;

static jclass classWinDragSource = NULL;
static jmethodID methodWinDragSourceContinueDrag = NULL;
static jmethodID methodWinDragSourceGiveFeedback = NULL;
static jmethodID methodWinDragSourceEndDrag = NULL;

static const int RAW_BITMAP_HEADER_LENGTH = 7;

inline static JNIEnv * getEnv() {
    JNIEnv* env;
    jvm->GetEnv((void**)&env, JNI_VERSION_1_2);
    return env;
}



JNIEXPORT void JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_init(
               JNIEnv * env, jclass clazz) {

    HRESULT hr = OleInitialize(NULL);

    if (jvm != NULL) {
        return;
    }

    WinDataObject::registerFormats();
    
    env->GetJavaVM(&jvm);
    classWinDataTransfer = (jclass)env->NewGlobalRef(clazz);

    classString = env->FindClass("java/lang/String");
    classString = (jclass)env->NewGlobalRef(classString);
    
    classDataSnapshot = env->FindClass("org/apache/harmony/awt/datatransfer/DataSnapshot");
    classDataSnapshot = (jclass)env->NewGlobalRef(classDataSnapshot);

    methodDataSnapshotGetNativeFormats = env->GetMethodID(
            classDataSnapshot, "getNativeFormats", "()[Ljava/lang/String;");
    methodDataSnapshotGetText = env->GetMethodID(
            classDataSnapshot, "getText", "()Ljava/lang/String;");
    methodDataSnapshotGetFileList = env->GetMethodID(
            classDataSnapshot, "getFileList", "()[Ljava/lang/String;");
    methodDataSnapshotGetURL = env->GetMethodID(
            classDataSnapshot, "getURL", "()Ljava/lang/String;");
    methodDataSnapshotGetHTML = env->GetMethodID(
            classDataSnapshot, "getHTML", "()Ljava/lang/String;");
    methodDataSnapshotGetRawBitmapHeader = env->GetMethodID(
            classDataSnapshot, "getRawBitmapHeader", "()[I");
    methodDataSnapshotGetRawBitmapBuffer8 = env->GetMethodID(
            classDataSnapshot, "getRawBitmapBuffer8", "()[B");
    methodDataSnapshotGetRawBitmapBuffer16 = env->GetMethodID(
            classDataSnapshot, "getRawBitmapBuffer16", "()[S");
    methodDataSnapshotGetRawBitmapBuffer32 = env->GetMethodID(
            classDataSnapshot, "getRawBitmapBuffer32", "()[I");
    methodDataSnapshotGetSerializedObject = env->GetMethodID(
            classDataSnapshot, "getSerializedObject", "(Ljava/lang/String;)[B");

    classWinDropTarget = env->FindClass("org/apache/harmony/awt/datatransfer/windows/WinDropTarget");
    classWinDropTarget = (jclass)env->NewGlobalRef(classWinDropTarget);

    methodWinDropTargetDragEnter = env->GetMethodID(
            classWinDropTarget, "dragEnter", "(JIIII)I");
    methodWinDropTargetDragLeave = env->GetMethodID(
            classWinDropTarget, "dragLeave", "()V");
    methodWinDropTargetDragOver = env->GetMethodID(
            classWinDropTarget, "dragOver", "(IIII)I");
    methodWinDropTargetDrop = env->GetMethodID(
            classWinDropTarget, "drop", "(JIIII)I");

    classWinDragSource = env->FindClass("org/apache/harmony/awt/datatransfer/windows/WinDragSource");;
    classWinDragSource = (jclass)env->NewGlobalRef(classWinDragSource);
    methodWinDragSourceContinueDrag = env->GetMethodID(
            classWinDragSource, "continueDrag", "()V");
    methodWinDragSourceGiveFeedback = env->GetMethodID(
            classWinDragSource, "giveFeedback", "(IZ)V");
    methodWinDragSourceEndDrag = env->GetMethodID(
            classWinDragSource, "endDrag", "(IZ)V");
}

JNIEXPORT jlong JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getOleClipboardDataObject(
               JNIEnv *, jclass) {
    IDataObject * dataObject = NULL;
    HRESULT hr = OleGetClipboard(&dataObject);
    if (SUCCEEDED(hr)) {
        return (jlong)dataObject;
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_releaseDataObject(
               JNIEnv *, jclass, jlong pointer) {
    if (pointer == 0) {
        return;
    }
    IDataObject * dataObject = (IDataObject *)pointer;
    dataObject->Release();
}

JNIEXPORT jstring JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectText(
        JNIEnv * env, jclass, jlong pointer) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::getText(env, (IDataObject *)pointer);
}

JNIEXPORT jobjectArray JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectFileList(
        JNIEnv * env, jclass, jlong pointer) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::getFileList(env, (IDataObject *)pointer);
}

JNIEXPORT jstring JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectURL(
        JNIEnv * env, jclass, jlong pointer) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::getURL(env, (IDataObject *)pointer);
}

JNIEXPORT jstring JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectHTML(
        JNIEnv * env, jclass, jlong pointer) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::getHTML(env, (IDataObject *)pointer);
}

JNIEXPORT jobject JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectImage(
        JNIEnv * env, jclass, jlong pointer, jintArray header) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::getBitmap(env, (IDataObject *)pointer, header);
}

JNIEXPORT jbyteArray JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectSerialized(
        JNIEnv * env, jclass, jlong pointer, jstring nativeFormat) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::getSerialized(env, (IDataObject *)pointer, nativeFormat);
}

JNIEXPORT jstring JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getSystemDefaultCharset(
        JNIEnv * env, jclass) {

    CHARSETINFO charset;
    UINT_PTR lcid = GetSystemDefaultLCID();
    BOOL ok = TranslateCharsetInfo((DWORD*)lcid, &charset, TCI_SRCLOCALE);
    if (!ok) {
        return NULL;
    }
    
    CoInitialize(NULL);
    CComPtr<IMultiLanguage> multiLanguage;
    HRESULT hr = multiLanguage.CoCreateInstance(CLSID_CMultiLanguage);
    if (FAILED(hr)) {
        return NULL;
    }
    
    MIMECPINFO info;
    memset(&info, 0, sizeof(MIMECPINFO));
    hr = multiLanguage->GetCodePageInfo(charset.ciACP, &info);
    if (FAILED(hr)) {
        return NULL;
    }

    return env->NewString((const jchar *)info.wszWebCharset, (jsize)wcslen(info.wszWebCharset));
}

JNIEXPORT jobjectArray JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_getDataObjectFormats(
        JNIEnv * env, jclass, jlong pointer) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::enumFormats(env, (IDataObject *)pointer);
}

JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_isDataObjectFormatAvailable(
        JNIEnv * env, jclass, jlong pointer, jstring nativeFormat) {
    if (pointer == 0) {
        return NULL;
    }
    return WinDataObject::queryFormat(env, (IDataObject *)pointer, nativeFormat);
}

JNIEXPORT void JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_setClipboardContents(
        JNIEnv * env, jclass, jobject dataSnapshot) {
    if (dataSnapshot == NULL) {
        OleSetClipboard(NULL);
        return;
    }

    WinDataObject * winDataObject = WinDataObject::CreateInstance();
    winDataObject->init(env, dataSnapshot);
    OleSetClipboard(winDataObject);
    OleFlushClipboard();
    winDataObject->Release();
}

JNIEXPORT void JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_startDrag(
        JNIEnv * env, jclass, jobject dataSnapshot, jobject dragSource, jint actions) {
    if (dataSnapshot == NULL || dragSource == NULL || actions == 0) {
        return;
    }

    WinDataObject * winDataObject = WinDataObject::CreateInstance();
    winDataObject->init(env, dataSnapshot);

    WinDragSource * winDragSource = WinDragSource::CreateInstance();
    winDragSource->init(env, dragSource);

       DWORD dwEffect = 0;
    HRESULT hr = DoDragDrop(winDataObject, winDragSource,
                            (DWORD)actions, &dwEffect);

    winDataObject->Release();
    winDragSource->Release();

    getEnv()->CallVoidMethod(dragSource, 
            methodWinDragSourceEndDrag, (jint)dwEffect, (hr == DRAGDROP_S_DROP));
}

JNIEXPORT jlong JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_registerDropTarget(
        JNIEnv * env, jclass, jlong hWnd, jobject targetObj) {

       WinDropTarget * target = WinDropTarget::CreateInstance();
       target->init((HWND)hWnd, targetObj);

       HRESULT hr = CoLockObjectExternal(target, TRUE, FALSE);
    if (SUCCEEDED(hr)) {
          hr = RegisterDragDrop((HWND)hWnd, target);
    }
    if (FAILED(hr)) {
        target->Release();
        return 0;
    }

    IDropTarget * pdt = static_cast<IDropTarget *>(target);
       return (jlong)pdt;
}

JNIEXPORT void JNICALL
Java_org_apache_harmony_awt_nativebridge_windows_WinDataTransfer_revokeDropTarget(
        JNIEnv * env, jclass, jlong hWnd, jlong target) {

       RevokeDragDrop((HWND)hWnd);
       if (target != 0) {
        IDropTarget * pdt = (IDropTarget *)target;
               CoLockObjectExternal(pdt, FALSE, TRUE);
               pdt->Release();
       }
}



void WinDataObject::registerFormats() {
    cfShellUrlW = RegisterClipboardFormat(CFSTR_INETURLW);
    cfShellUrlA = RegisterClipboardFormat(CFSTR_INETURLA);
    cfHTML = RegisterClipboardFormat(TEXT("HTML Format"));
    htmlFormat.cfFormat = cfHTML;
    urlFormats[0].cfFormat = cfShellUrlW;
    urlFormats[1].cfFormat = cfShellUrlA;
}

jstring WinDataObject::getStringA(JNIEnv * env, const char * cstr) {
    if (cstr == NULL) {
        return env->NewString((const jchar *)L"", 0);
    }
    jstring jstr = NULL;
    int len = MultiByteToWideChar(CP_ACP, 0, cstr, -1, NULL, 0);
    if (len > 0) {
        wchar_t * wstr = new wchar_t[len];
        if (len == MultiByteToWideChar(CP_ACP, 0, cstr, -1, wstr, len)) {
            jstr = env->NewString((const jchar *)wstr, (jsize)wcslen(wstr));
        }
        delete [] wstr;
    }
    return jstr;
}

jstring WinDataObject::getStringA(JNIEnv * env, HGLOBAL hGlobal) {
    char * cstr = (char *)GlobalLock(hGlobal);
    jstring jstr = getStringA(env, cstr);
    GlobalUnlock(hGlobal);
    return jstr;
}

jstring WinDataObject::getStringW(JNIEnv * env, HGLOBAL hGlobal) {
    wchar_t * wstr = (wchar_t *)GlobalLock(hGlobal);
    if (wstr == NULL) {
        return env->NewString((const jchar *)L"", 0);
    }
    jstring jstr = env->NewString((const jchar *)wstr, (jsize)wcslen(wstr));
    GlobalUnlock(hGlobal);
    return jstr;
}

jstring WinDataObject::getStringA(JNIEnv * env, IDataObject * dataObject, 
                                 UINT format) {
    FORMATETC fmt = { format, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
    STGMEDIUM stgmed;
    jstring jstr = NULL;
    if(SUCCEEDED(dataObject->GetData(&fmt, &stgmed)))
    {
        jstr = getStringA(env, stgmed.hGlobal);
        ReleaseStgMedium(&stgmed);
    }
    return jstr;
}

jstring WinDataObject::getStringW(JNIEnv * env, IDataObject * dataObject,
                                 UINT format) {
    FORMATETC fmt = { format, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
    STGMEDIUM stgmed;
    jstring jstr = NULL;
    if(SUCCEEDED(dataObject->GetData(&fmt, &stgmed)))
    {
        jstr = getStringW(env, stgmed.hGlobal);
        ReleaseStgMedium(&stgmed);
    }
    return jstr;
}

jobjectArray WinDataObject::getFileListA(JNIEnv * env, const char * files) {
    const char * cstr = files;
    jsize count = 0;
    while (*cstr) {
        cstr += strlen(cstr) + 1;
        count ++;
    }
    jobjectArray result = env->NewObjectArray(count, classString, NULL);
    cstr = files;
    for (jsize i=0; *cstr; i++) {
        jstring jstr = getStringA(env, cstr);
        cstr += strlen(cstr) + 1;
        env->SetObjectArrayElement(result, i, jstr);
    }
    return result;
}

jobjectArray WinDataObject::getFileListW(JNIEnv * env, const wchar_t * files) {
    const wchar_t * wstr = files;
    jsize count = 0;
    while (*wstr) {
        wstr += wcslen(wstr) + 1;
        count ++;
    }
    jobjectArray result = env->NewObjectArray(count, classString, NULL);
    wstr = files;
    for (jsize i=0; *wstr; i++) {
        size_t len = wcslen(wstr);
        jstring jstr = env->NewString((const jchar *)wstr, (jsize)len);
        wstr += len + 1;
        env->SetObjectArrayElement(result, i, jstr);
    }
    return result;
}

jstring WinDataObject::getText(JNIEnv * env, IDataObject * dataObject) {
    jstring jstr = getStringW(env, dataObject, CF_UNICODETEXT);
    if (jstr != NULL) {
        return jstr;
    }
    return getStringA(env, dataObject, CF_TEXT);
}

jobjectArray WinDataObject::getFileList(JNIEnv * env, IDataObject * dataObject) {
    STGMEDIUM stgmed;
    if(FAILED(dataObject->GetData(&fileListFormat, &stgmed))) {
        return NULL;
    }

    jobjectArray fileList = NULL;
    DROPFILES * drop = (DROPFILES *)GlobalLock(stgmed.hGlobal);
    BYTE * fileNames = (BYTE *)drop + drop->pFiles;
    if (drop->fWide) {
        fileList = getFileListW(env, (wchar_t *)fileNames);
    } else {
        fileList = getFileListA(env, (char *)fileNames);
    }
    GlobalUnlock(stgmed.hGlobal);
    ReleaseStgMedium(&stgmed);

    return fileList;
}

jstring WinDataObject::getURL(JNIEnv * env, IDataObject * dataObject) {
    jstring jstr = getStringW(env, dataObject, cfShellUrlW);
    if (jstr != NULL) {
        return jstr;
    }
    return getStringA(env, dataObject, cfShellUrlA);
}

int WinDataObject::parseCfHtmlTag(const char * cstr, const char * tag) {
    const char * pos = strstr(cstr, tag);
    if (pos == NULL) {
        return -1;
    }
    return atoi(pos + strlen(tag));
}

jstring WinDataObject::getHTML(JNIEnv * env, IDataObject * dataObject) {
    STGMEDIUM stgmed;
    HRESULT hr = dataObject->GetData(&htmlFormat, &stgmed);
    if(FAILED(hr)) {
        return NULL;
    }

    jstring html = NULL;

    int length = (int)GlobalSize(stgmed.hGlobal);
    const char * cstr = (const char *)GlobalLock(stgmed.hGlobal);

    int start = parseCfHtmlTag(cstr, "StartHTML:");
    int end = parseCfHtmlTag(cstr, "EndHTML:");
    if (start < 0 || end < 0) {
        start = parseCfHtmlTag(cstr, "StartFragment:");
        end = parseCfHtmlTag(cstr, "EndFragment:");
    }

    if (start > 0 && end > 0 && start < end && end <= length) {
        char * buffer = new char[end - start + 2];
        memcpy(buffer, cstr + start, end - start + 1);
        buffer[end - start + 1] = 0;
        html = env->NewStringUTF(buffer);
        delete [] buffer;
    }

    GlobalUnlock(stgmed.hGlobal);
    ReleaseStgMedium(&stgmed);

    return html;
}

jobject WinDataObject::getBitmap(JNIEnv * env, IDataObject * dataObject, jintArray header) {
    STGMEDIUM stgmed;
    
    for (int i = 0; i < sizeof(imageFormats)/sizeof(FORMATETC); i++) {
        if(SUCCEEDED(dataObject->GetData(&imageFormats[i], &stgmed))) {
            jobject result = NULL;
            
            switch (imageFormats[i].cfFormat) {
                case CF_DIB:
                {
                    LPVOID data = (LPVOID)GlobalLock(stgmed.hGlobal);
                    result = getDIB(env, (BITMAPINFO *)data, header);
                    GlobalUnlock(stgmed.hGlobal);
                    break;
                }
                case CF_ENHMETAFILE:
                {
                    result = getEnhMetaFile(env, stgmed.hEnhMetaFile, header);
                    break;
                }
            }

            ReleaseStgMedium(&stgmed);
        
            if (result != NULL) {
                return result;
            }
        }
    }

    return NULL;
}

jobject WinDataObject::getDIB(JNIEnv * env, BITMAPINFO * info, jintArray header) {
    BITMAPINFOHEADER & hdr = info->bmiHeader;
    BYTE * colors = (BYTE *)info + hdr.biSize;
    BYTE * buffer = colors + hdr.biClrUsed * sizeof(RGBQUAD);
    jint rMask = 0, gMask = 0, bMask = 0;
    jint bitCount = hdr.biBitCount;
    jint stride = hdr.biWidth;
    jsize height = labs(hdr.biHeight);
    jobject result = NULL;
    
    switch(hdr.biCompression) {
    case BI_RGB:
        if (bitCount == 32 || bitCount == 24) {
            rMask = 0xFF;
            gMask = 0xFF00;
            bMask = 0xFF0000;
        } else if (bitCount == 16) {
            rMask = 0x7C00;
            gMask = 0x3E0;
            bMask = 0x1F;
        }
        break;
    case BI_BITFIELDS:
        rMask = ((jint*)colors)[0];
        gMask = ((jint*)colors)[1];
        bMask = ((jint*)colors)[2];
        buffer += 3 * sizeof(RGBQUAD);
        break;
    default:
        // TODO: convert to 24 or 32 bit RGB bitmap
        return NULL;
    }
    
    switch (hdr.biBitCount) {
    case 32: 
    {
        jsize length = stride * height;
        jintArray array = env->NewIntArray(length);
        jint * pixels = (jint *)buffer;
        if (hdr.biHeight > 0) {
            pixels += stride * (hdr.biHeight - 1);
            for (jsize i = 0; i < hdr.biHeight; i++, pixels -= stride) {
                env->SetIntArrayRegion(array, i * stride, hdr.biWidth, pixels);
            }
        } else {
            env->SetIntArrayRegion(array, 0, length, pixels);
        }
        result = array;
        break;
    }
    case 24:
    {
        stride = hdr.biWidth * 3;
        if (stride % 4 > 0) {
            stride += 4 - stride % 4;
        }
        jsize length = stride * height;
        jbyteArray array = env->NewByteArray(length);
        jbyte * pixels = (jbyte *)buffer;
        if (hdr.biHeight > 0) {
            pixels += stride * (hdr.biHeight - 1);
            for (jsize i = 0; i < hdr.biHeight; i++, pixels -= stride) {
                env->SetByteArrayRegion(array, i * stride, hdr.biWidth * 3, pixels);
            }
        } else {
            env->SetByteArrayRegion(array, 0, length, pixels);
        }
        result = array;
        break;
    }
    case 16:
    case 15:
    {
        stride = hdr.biWidth + hdr.biWidth % 2;
        jsize length = stride * height;
        jshortArray array = env->NewShortArray(length);
        jshort * pixels = (jshort *)buffer;
        if (hdr.biHeight > 0) {
            pixels += stride * (hdr.biHeight - 1);
            for (jsize i = 0; i < hdr.biHeight; i++, pixels -= stride) {
                env->SetShortArrayRegion(array, i * stride, hdr.biWidth, pixels);
            }
        } else {
            env->SetShortArrayRegion(array, 0, length, pixels);
        }
        result = array;
        break;
    }
    case 8:
    {
        bitCount = 32;
        const jint opaque = 0xFF000000;
        rMask = 0xFF0000;
        gMask = 0xFF00;
        bMask = 0xFF;
        if (stride % 4 > 0) {
            stride += 4 - stride % 4;
        }
        
        jsize length = hdr.biWidth * height;
        jintArray array = env->NewIntArray(length);
        BYTE * pixels = buffer;
        jint * palette = (jint *)colors;
        jint * line = new jint[hdr.biWidth];
        jint step = stride;
        if (hdr.biHeight > 0) {
            pixels += stride * (hdr.biHeight - 1);
            step = -stride;
        }
        for (jsize i = 0; i < height; i++, pixels += step) {
            for (int j = 0; j < hdr.biWidth; j++) {
                BYTE c = pixels[j];
                line[j] = (c < hdr.biClrUsed) ? (palette[c] | opaque) : opaque;
            }
            env->SetIntArrayRegion(array, i * hdr.biWidth, hdr.biWidth, line);
        }
        delete [] line;
        stride = hdr.biWidth;
        result = array;
        break;
    }
    default:
        // TODO: convert to 24 or 32 bit RGB bitmap
        return NULL;
    }
    jint rawHeader[] = { hdr.biWidth, height, stride, 
                         bitCount, rMask, gMask, bMask };
    env->SetIntArrayRegion(header, 0, 
            RAW_BITMAP_HEADER_LENGTH, rawHeader);

    return result;    
}

jobject WinDataObject::getEnhMetaFile(JNIEnv * env, HENHMETAFILE hEMF, jintArray header) {
    const float screenResulutionUnit = 0.01f; // in millimetres
    ENHMETAHEADER hemf;
    memset(&hemf, 0, sizeof(ENHMETAHEADER));
    hemf.iType = EMR_HEADER;
    hemf.nSize = sizeof(ENHMETAHEADER);
    hemf.dSignature = ENHMETA_SIGNATURE;

    if (GetEnhMetaFileHeader(hEMF, sizeof(ENHMETAHEADER), &hemf) == 0) {
        return NULL;
    }
    
    HDC screenDC = GetDC(NULL);
    
    int width = hemf.rclFrame.right - hemf.rclFrame.left;
    int height = hemf.rclFrame.bottom - hemf.rclFrame.top;
    
    float hRes = (float)GetDeviceCaps(screenDC, HORZRES) /
            (float)GetDeviceCaps(screenDC, HORZSIZE);
    float vRes = (float)GetDeviceCaps(screenDC, VERTRES) /
            (float)GetDeviceCaps(screenDC, VERTSIZE);
    width = (int)(width * screenResulutionUnit * hRes);
    height = (int)(height * screenResulutionUnit * vRes);
    int screenBits = GetDeviceCaps(screenDC, BITSPIXEL);
    
    HDC memoryDC = CreateCompatibleDC(screenDC);
    HBITMAP hBitmap = CreateCompatibleBitmap(memoryDC, width, height);
    SelectObject(memoryDC, hBitmap);
    ReleaseDC(NULL, screenDC);
    
    jobject result = NULL;
    RECT rc = {0, 0, width, height };
    HBRUSH hbr = CreateSolidBrush(0xFFFFFFFF);
    FillRect(memoryDC, &rc, hbr);
    DeleteObject(hbr);
    
    BOOL ok = PlayEnhMetaFile(memoryDC, hEMF, &rc);
    BITMAPINFO info;
    memset(&info, 0, sizeof(BITMAPINFO));
    info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    info.bmiHeader.biPlanes = 1;
    info.bmiHeader.biWidth = width;
    info.bmiHeader.biHeight = -height;
    info.bmiHeader.biBitCount = (screenBits > 16) ? 32 : 16;
    info.bmiHeader.biCompression = BI_RGB;
    if (ok) {
        ok = GetDIBits(memoryDC, hBitmap, 0, 0, NULL, &info, DIB_RGB_COLORS);
    }
    int stride = 0;
    if (ok) {
        switch (info.bmiHeader.biBitCount) {
        case 32:
            stride = info.bmiHeader.biWidth * 4;
            break;
        case 16:
            stride = (info.bmiHeader.biWidth + info.bmiHeader.biWidth % 2) * 2;
            break;
        default:
            ok = FALSE;
        }
    }
    BYTE * bitmap = NULL;
    if (ok) {
        bitmap = new BYTE[sizeof(BITMAPINFOHEADER) + stride * height];
        BYTE * buffer = bitmap + sizeof(BITMAPINFOHEADER);
        memcpy(bitmap, &info.bmiHeader, sizeof(BITMAPINFOHEADER));
        ok = GetDIBits(memoryDC, hBitmap, 0, height, buffer, 
                (BITMAPINFO *)bitmap, DIB_RGB_COLORS);
    }
    if (ok) {
        result = getDIB(env, (BITMAPINFO *)bitmap, header);
    }
    if (bitmap != NULL) {
        delete [] bitmap;
    }
    DeleteObject(hBitmap);
    DeleteDC(memoryDC);
    
    return result;
}

jobjectArray WinDataObject::enumFormats(JNIEnv * env, IDataObject * dataObject) {
    CComPtr<IEnumFORMATETC> enumFormats;
    HRESULT hr = dataObject->EnumFormatEtc(DATADIR_GET, &enumFormats);
    if (FAILED(hr)) {
        return NULL;
    }

    enum { maskText = 1, maskFileList = 2, maskUrl = 4, maskHtml = 8, maskImage = 16 };
    DWORD formatsMask = 0;
    
    CSimpleArray<jstring> formatList;

    FORMATETC format;
    DWORD count = 0;
    
    while (enumFormats->Next(1, &format, &count) == S_OK) {
        if (format.ptd != NULL) {
            CoTaskMemFree(format.ptd);
        }
        if (format.dwAspect != DVASPECT_CONTENT) {
            continue;
        }
        if ((format.tymed & TYMED_ENHMF) != 0 
                && format.cfFormat == CF_ENHMETAFILE) {
            jstring jstr = env->NewStringUTF(FORMAT_IMAGE);
            formatList.Add(jstr);
            continue;
        }
        if ((format.tymed & TYMED_HGLOBAL) == 0) {
            continue;
        }
        if (format.cfFormat == CF_UNICODETEXT || format.cfFormat == CF_TEXT) {
            jstring jstr = env->NewStringUTF(FORMAT_TEXT);
            formatList.Add(jstr);
            continue;
        }
        if (format.cfFormat == CF_HDROP) {
            jstring jstr = env->NewStringUTF(FORMAT_FILE_LIST);
            formatList.Add(jstr);
            continue;
        }
        if (format.cfFormat == CF_DIB) {
            jstring jstr = env->NewStringUTF(FORMAT_IMAGE);
            formatList.Add(jstr);
            continue;
        }
        if (format.cfFormat == cfShellUrlA || format.cfFormat == cfShellUrlW) {
            jstring jstr = env->NewStringUTF(FORMAT_URL);
            formatList.Add(jstr);
            continue;
        }
        if (format.cfFormat == cfHTML) {
            jstring jstr = env->NewStringUTF(FORMAT_HTML);
            formatList.Add(jstr);
            continue;
        }
        jstring jstr = getSerializedFormatName(env, format.cfFormat);
        if (jstr != NULL) {
            formatList.Add(jstr);
        }
    }
    enumFormats.Release();

    jobjectArray result = env->NewObjectArray((jsize)formatList.GetSize(), classString, NULL);
    for (jsize i = 0; i < (jsize)formatList.GetSize(); i++) {
        env->SetObjectArrayElement(result, i, formatList[i]);
    }
    return result;
}

jbyteArray WinDataObject::getSerialized(JNIEnv * env, IDataObject * dataObject, 
                                        jstring nativeFormat) {
    FORMATETC format = { 0, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
    jboolean isCopy;
    const jchar * name = env->GetStringChars(nativeFormat, &isCopy);
    format.cfFormat = RegisterClipboardFormatW((LPCWSTR)name);
    env->ReleaseStringChars(nativeFormat, name);
    
    STGMEDIUM stgmed;
    if(FAILED(dataObject->GetData(&format, &stgmed))) {
        return NULL;
    }

    jsize length = (jsize)GlobalSize(stgmed.hGlobal);
    jbyte * bytes = (jbyte *)GlobalLock(stgmed.hGlobal);
    jbyteArray array = env->NewByteArray(length);
    env->SetByteArrayRegion(array, 0, length, bytes);

    GlobalUnlock(stgmed.hGlobal);
    ReleaseStgMedium(&stgmed);
    return array;
}

jboolean WinDataObject::queryFormat(IDataObject * dataObject, UINT format, 
                                    DWORD tymed) {
    FORMATETC fmt = { format, 0, DVASPECT_CONTENT, -1, tymed };
    return (SUCCEEDED(dataObject->QueryGetData(&fmt))) ? JNI_TRUE : JNI_FALSE;
}

jboolean WinDataObject::queryFormat(JNIEnv * env, 
                                    IDataObject * dataObject,
                                    jstring nativeFormat) {
    const char * formatNameA = 
            (const char *)env->GetStringUTFChars(nativeFormat, NULL);
    BOOL found = FALSE;
    jboolean result = JNI_FALSE;
    if (strcmp(formatNameA, FORMAT_TEXT) == 0) {
        found = TRUE;
        result = queryFormat(dataObject, CF_TEXT)
              || queryFormat(dataObject, CF_UNICODETEXT);

    } else if (strcmp(formatNameA, FORMAT_FILE_LIST) == 0) {
        found = TRUE;
        result = queryFormat(dataObject, CF_HDROP);

    } else if (strcmp(formatNameA, FORMAT_URL) == 0) {
        found = TRUE;
        result = queryFormat(dataObject, cfShellUrlW)
              || queryFormat(dataObject, cfShellUrlA);

    } else if (strcmp(formatNameA, FORMAT_HTML) == 0) {
        found = TRUE;
        result = queryFormat(dataObject, cfHTML);

    } else if (strcmp(formatNameA, FORMAT_IMAGE) == 0) {
        found = TRUE;
        result = queryFormat(dataObject, CF_DIB)
              || queryFormat(dataObject, CF_ENHMETAFILE, TYMED_ENHMF);
    }
    env->ReleaseStringUTFChars(nativeFormat, formatNameA);
    if (found) {
        return result;
    }
    const jchar * formatNameW = env->GetStringChars(nativeFormat, NULL);
    if (wcsstr((const wchar_t *)formatNameW, FORMAT_SERIALIZED) != NULL) {
        UINT format = RegisterClipboardFormatW((LPCWSTR)formatNameW);
        if (format != 0) {
            result = queryFormat(dataObject, format);
        }
    }
    env->ReleaseStringChars(nativeFormat, formatNameW);
    return result;
}

jstring WinDataObject::getSerializedFormatName(JNIEnv * env, UINT format) {
    const int formatNameLength = 512;
    wchar_t formatName[formatNameLength];
    if (0 != GetClipboardFormatNameW(format, formatName, formatNameLength) ) {
        if (wcsstr(formatName, FORMAT_SERIALIZED) != NULL) {
            return env->NewString((const jchar *)formatName, (jsize)wcslen(formatName));
        }
    }
    return NULL;
}


WinDataObject::WinDataObject() {
    dataSnapshotGlobalRef = NULL;
}

WinDataObject::~WinDataObject() {
    if (dataSnapshotGlobalRef != NULL) {
        getEnv()->DeleteGlobalRef(dataSnapshotGlobalRef);
    }
}

int WinDataObject::getFormatsForName(const char * formatName, 
                                     FORMATETC ** formats) {

    if (strcmp(formatName, FORMAT_TEXT) == 0) {
        *formats = textFormats;
        return sizeof(textFormats)/sizeof(FORMATETC);
    }
    if (strcmp(formatName, FORMAT_FILE_LIST) == 0) {
        *formats = &fileListFormat;
        return 1;
    }
    if (strcmp(formatName, FORMAT_URL) == 0) {
        *formats = urlFormats;
        return sizeof(urlFormats)/sizeof(FORMATETC);
    }
    if (strcmp(formatName, FORMAT_HTML) == 0) {
        *formats = &htmlFormat;
        return 1;
    }
    if (strcmp(formatName, FORMAT_IMAGE) == 0) {
        *formats = imageFormats;
        return sizeof(imageFormats)/sizeof(FORMATETC);
    }
    return 0;
}

int WinDataObject::getFormatsForName(JNIEnv * env, jstring formatName, 
                                     FORMATETC ** formats) {
    const char * formatNameA = 
            (const char *)env->GetStringUTFChars(formatName, NULL);
    int cnt = getFormatsForName(formatNameA, formats);
    env->ReleaseStringUTFChars(formatName, formatNameA);
    return cnt;
}

UINT WinDataObject::getSerializedFormat(JNIEnv * env, jstring formatName) {
    const jchar * formatNameW = env->GetStringChars(formatName, NULL);
    UINT result = 0;
    if (wcsstr((const wchar_t *)formatNameW, FORMAT_SERIALIZED) != NULL) {
        result = RegisterClipboardFormatW((LPCWSTR)formatNameW);
    }
    env->ReleaseStringChars(formatName, formatNameW);
    return result;
}

void WinDataObject::init(JNIEnv * env, jobject dataSnapshot) {
    dataSnapshotGlobalRef = env->NewGlobalRef(dataSnapshot);

    jobjectArray nativeFormats = (jobjectArray)env->CallObjectMethod(
            dataSnapshot, methodDataSnapshotGetNativeFormats);
    jsize formatCount = env->GetArrayLength(nativeFormats);
    
    for (jsize i = 0; i < formatCount; i++) {
        jstring formatName = 
                (jstring)(env->GetObjectArrayElement(nativeFormats, i));
        FORMATETC * list;
        int listLen = getFormatsForName(env, formatName, &list);
        for (int j = 0; j < listLen; j++) {
            formatArray.Add(list[j]);
        }
        if (listLen == 0) {
            UINT serializedFormat = getSerializedFormat(env, formatName);
            if (serializedFormat != 0) {
                FORMATETC fmt = { serializedFormat, 0, DVASPECT_CONTENT, -1, TYMED_HGLOBAL };
                formatArray.Add(fmt);
            }
        }
    }
}

STDMETHODIMP WinDataObject::GetData(FORMATETC * pFormatEtc, STGMEDIUM * pMedium) {
    if (pFormatEtc == NULL || pMedium == NULL) {
        return E_INVALIDARG;
    }
    if (pFormatEtc->tymed != TYMED_HGLOBAL) {
        return DV_E_FORMATETC;
    }
    
    int idx = getFormatIndex(pFormatEtc);
    if(idx < 0) {
        return DV_E_FORMATETC;
    }

    pMedium->tymed = TYMED_HGLOBAL;
    pMedium->pUnkForRelease = NULL;
    
    if (pFormatEtc->cfFormat == CF_UNICODETEXT || 
            pFormatEtc->cfFormat == CF_TEXT) {
        return getText(pFormatEtc->cfFormat == CF_UNICODETEXT, pMedium);       
    }

    if (pFormatEtc->cfFormat == CF_HDROP) {
        return getFileList(pMedium);
    }

    if (pFormatEtc->cfFormat == cfHTML) {
        return getHTML(pMedium);
    }

    if (pFormatEtc->cfFormat == cfShellUrlW || 
            pFormatEtc->cfFormat == cfShellUrlA) {
        return getURL(pFormatEtc->cfFormat == cfShellUrlA, pMedium);       
    }

    for (int i = 0; i < sizeof(imageFormats)/sizeof(FORMATETC); i++) {
        if (pFormatEtc->cfFormat == imageFormats[i].cfFormat) {
            return getImage(pFormatEtc->cfFormat, pMedium);
        }
    }

    return getSerializedObject(pFormatEtc->cfFormat, pMedium);
}

STDMETHODIMP WinDataObject::GetDataHere(FORMATETC *,STGMEDIUM *) {
    return DATA_E_FORMATETC;
}

STDMETHODIMP WinDataObject::QueryGetData(FORMATETC * pFormatEtc) {
    HRESULT hr = (getFormatIndex(pFormatEtc) >= 0) ? S_OK: DV_E_FORMATETC;
    return hr;
}

STDMETHODIMP WinDataObject::GetCanonicalFormatEtc(FORMATETC *,
                                                  FORMATETC * pFormatEtcOut) {
    pFormatEtcOut->ptd = NULL;
    return E_NOTIMPL;
}

STDMETHODIMP WinDataObject::SetData(FORMATETC * pFormatetc, 
                                    STGMEDIUM * pmedium, BOOL fRelease) {
    return E_NOTIMPL;
}

STDMETHODIMP WinDataObject::EnumFormatEtc(DWORD dwDirection, 
                                          IEnumFORMATETC ** ppEnumFormatEtc) {
    if(dwDirection == DATADIR_GET) {
        return SHCreateStdEnumFmtEtc((UINT)formatArray.GetSize(), 
                formatArray.GetData(), ppEnumFormatEtc);
    }
    return E_NOTIMPL;
}

STDMETHODIMP WinDataObject::DAdvise(FORMATETC *,DWORD,IAdviseSink *,DWORD *) {
    return OLE_E_ADVISENOTSUPPORTED;
}

STDMETHODIMP WinDataObject::DUnadvise(DWORD) {
    return OLE_E_ADVISENOTSUPPORTED;
}

STDMETHODIMP WinDataObject::EnumDAdvise(IEnumSTATDATA **) {
    return OLE_E_ADVISENOTSUPPORTED;
}

int WinDataObject::getFormatIndex(FORMATETC * fmt) {
    for (int i=0; i<formatArray.GetSize(); i++) {
        const FORMATETC & f = formatArray[i];
        if (f.cfFormat == fmt->cfFormat
                && f.ptd == fmt->ptd
                && f.dwAspect == fmt->dwAspect
                && f.lindex == fmt->lindex
                && f.tymed == fmt->tymed) {
            return (int)i;
        }
    }
    return -1;
}

HGLOBAL WinDataObject::getTextGlobal(JNIEnv * env, jstring text, BOOL unicode) {
    jsize len = env->GetStringLength(text);
    
    if (unicode) {
        wchar_t * wstr = (wchar_t *)GlobalAlloc(GMEM_FIXED,
                (len + 1) * sizeof(wchar_t));
        if (wstr == NULL) {
            return NULL;
        }
        env->GetStringRegion(text, 0, len, (jchar *)wstr);
        wstr[len] = 0;
        return (HGLOBAL)wstr;
    }
    
    // convert to ASCII text
    wchar_t * wstr = new wchar_t[len + 1];
    if (wstr == NULL) {
        return NULL;
    }
    env->GetStringRegion(text, 0, len, (jchar *)wstr);
    wstr[len] = 0;
    
    int cLen = WideCharToMultiByte(CP_ACP, 0, wstr, -1, NULL, 0, NULL, NULL);
    char * cstr = (char *)GlobalAlloc(GMEM_FIXED, cLen);
    if (cstr == NULL) {
        delete [] wstr;
        return NULL;
    }
    WideCharToMultiByte(CP_ACP, 0, wstr, -1, cstr, cLen, NULL, NULL);
    delete [] wstr;
    
    return (HGLOBAL)cstr;
}

HRESULT WinDataObject::getText(BOOL unicode, STGMEDIUM * pMedium) {
    JNIEnv * env = getEnv();
    jstring text = (jstring)env->CallObjectMethod(dataSnapshotGlobalRef, 
                                        methodDataSnapshotGetText);
    if (text == NULL) {
        return DV_E_FORMATETC;
    }
    pMedium->hGlobal = getTextGlobal(env, text, unicode);
    return (pMedium->hGlobal != NULL) ? S_OK : E_OUTOFMEMORY;
}

HRESULT WinDataObject::getHTML(STGMEDIUM * pMedium) {
    JNIEnv * env = getEnv();
    jstring text = (jstring)env->CallObjectMethod(dataSnapshotGlobalRef, 
                                        methodDataSnapshotGetHTML);
    if (text == NULL) {
        return DV_E_FORMATETC;
    }
    
    const char startFragment[] = "<!--StartFragment-->";
    const char endFragment[] = "<!--EndFragment-->";
    const char headerFormat[] = 
        "Version:1.0\r\nStartHTML:%010d\r\nEndHTML:%010d\r\nStartFragment:%010d\r\nEndFragment:%010d\r\n";
    char header[sizeof(headerFormat) + 40];
    wsprintfA(header, headerFormat, 0, 0, 0, 0);
    size_t headerLen = strlen(header);
    
    jboolean isCopy;
    const char * utfStr = env->GetStringUTFChars(text, &isCopy);

    const char * pStart = strstr(utfStr, startFragment);
    const char * pEnd = strstr(utfStr, endFragment);
    size_t iLen = strlen(utfStr);
    size_t iStart = (pStart != NULL) ? 
            (pStart - utfStr) + strlen(startFragment) : 0;
    size_t iEnd = (pEnd != NULL) ? (pEnd - utfStr) : iLen;

    char * cstr = (char *)GlobalAlloc(GMEM_FIXED, headerLen + iLen + 1);
    wsprintfA(cstr, headerFormat, headerLen, headerLen + iLen + 1,
                headerLen + iStart, headerLen + iEnd);
    memcpy(cstr + headerLen, utfStr, iLen);
    cstr[headerLen + iLen] = 0;
    
    env->ReleaseStringUTFChars(text, utfStr);

    pMedium->hGlobal = cstr;
    return S_OK;
}

HRESULT WinDataObject::getFileList(STGMEDIUM * pMedium) {
    JNIEnv * env = getEnv();
    jobjectArray fileList = 
            (jobjectArray)env->CallObjectMethod(dataSnapshotGlobalRef, 
                                        methodDataSnapshotGetFileList);
    if (fileList == NULL) {
        return DV_E_FORMATETC;
    }

    jsize listLength = env->GetArrayLength(fileList);
    jsize charCount = 0, i;
    for (i=0; i<listLength; i++) {
        jstring jstr = 
                (jstring)env->GetObjectArrayElement(fileList, i);
        jsize len = (jstr != NULL) ? env->GetStringLength(jstr) : 0;
        if (len != 0) {
            charCount += len + 1;
        }
    }
    if (charCount == 0) {
        return DV_E_FORMATETC;
    }

    BYTE * buffer = (BYTE *)GlobalAlloc(GMEM_FIXED,
            sizeof(DROPFILES) + (charCount + 1) * sizeof(wchar_t));
    DROPFILES * dropFiles = (DROPFILES *)buffer;
    memset(dropFiles, 0, sizeof(DROPFILES));
    dropFiles->fWide = TRUE;
    dropFiles->pFiles = sizeof(DROPFILES);
    wchar_t * wstr = (wchar_t *)(buffer + sizeof(DROPFILES));

    charCount = 0;
    for (i=0; i<listLength; i++) {
        jstring jstr =
                (jstring)env->GetObjectArrayElement(fileList, i);
        jsize len = (jstr != NULL) ? env->GetStringLength(jstr) : 0;
        if (len != 0) {
            env->GetStringRegion(jstr, 0, len, (jchar *)(wstr + charCount));
            charCount += len + 1;
            wstr[charCount - 1] = 0;
        }
    }
    wstr[charCount] = 0;

    pMedium->hGlobal = buffer;
    return S_OK;
}

HRESULT WinDataObject::getURL(BOOL unicode, STGMEDIUM * pMedium) {
    JNIEnv * env = getEnv();
    jstring url = (jstring)env->CallObjectMethod(dataSnapshotGlobalRef, 
                                        methodDataSnapshotGetURL);
    if (url == NULL) {
        return DV_E_FORMATETC;
    }
    pMedium->hGlobal = getTextGlobal(env, url, unicode);
    return (pMedium->hGlobal != NULL) ? S_OK : E_OUTOFMEMORY;
}

HRESULT WinDataObject::getImage(UINT format, STGMEDIUM * pMedium) {
    if (format != CF_DIB) {
        return DV_E_FORMATETC;
    }
    JNIEnv * env = getEnv();
    jintArray bitmapBuffer = 
            (jintArray)env->CallObjectMethod(dataSnapshotGlobalRef, 
                    methodDataSnapshotGetRawBitmapBuffer32);
    jintArray bitmapHeader = 
            (jintArray)env->CallObjectMethod(dataSnapshotGlobalRef, 
                    methodDataSnapshotGetRawBitmapHeader);
    if (bitmapBuffer == NULL || bitmapHeader == NULL) {
        return DV_E_FORMATETC;
    }

    jsize headerLength = env->GetArrayLength(bitmapHeader);
    if (headerLength != RAW_BITMAP_HEADER_LENGTH) {
        return DV_E_FORMATETC;
    }

    jint rawHeader[RAW_BITMAP_HEADER_LENGTH];
    env->GetIntArrayRegion(bitmapHeader, 0, 
            RAW_BITMAP_HEADER_LENGTH, rawHeader);

    int width = rawHeader[0];
    int height = rawHeader[1];
    // Accept only 32-bit RGB bitmap
    if (rawHeader[2] != width || rawHeader[3] != 32 
            || rawHeader[4] != 0xFF0000 
            || rawHeader[5] != 0xFF00 
            || rawHeader[6] != 0xFF) {
        return DV_E_FORMATETC;
    }

    pMedium->hGlobal = GlobalAlloc(GMEM_FIXED, 
            sizeof(BITMAPINFOHEADER) + width * height * 4);
    if (pMedium->hGlobal == NULL) {
        return E_OUTOFMEMORY;
    }
    BITMAPINFOHEADER * hdr = (BITMAPINFOHEADER *)(pMedium->hGlobal);
    memset(hdr, 0, sizeof(BITMAPINFOHEADER));
    hdr->biSize = sizeof(BITMAPINFOHEADER);
    hdr->biWidth = width;
    hdr->biHeight = -height;
    hdr->biPlanes = 1;
    hdr->biBitCount = 32;
    hdr->biCompression = BI_RGB;

    jint * rawBuffer = (jint *)(hdr + 1);
    env->GetIntArrayRegion(bitmapBuffer, 0, 
            width * height, rawBuffer);
    return S_OK;
}

HRESULT WinDataObject::getSerializedObject(
        UINT format, STGMEDIUM * pMedium) {
    JNIEnv * env = getEnv();
    jstring jstr = getSerializedFormatName(env, format);
    if (jstr == NULL) {
        return DV_E_FORMATETC;
    }
    jbyteArray bytes = (jbyteArray)env->CallObjectMethod(dataSnapshotGlobalRef, 
                                        methodDataSnapshotGetSerializedObject, jstr);
    if (bytes == NULL) {
        return DV_E_FORMATETC;
    }

    jsize length = env->GetArrayLength(bytes);
    pMedium->hGlobal = GlobalAlloc(GMEM_FIXED, length);
    if (pMedium->hGlobal == NULL) {
        return E_OUTOFMEMORY;
    }
    env->GetByteArrayRegion(bytes, 0, length, (jbyte *)pMedium->hGlobal);
    return S_OK;
}


WinDropTarget::WinDropTarget() {
    hWnd = NULL;
    dropTargetGlobalRef = NULL;
}

WinDropTarget::~WinDropTarget(void) {
    if (dropTargetGlobalRef != NULL) {
        getEnv()->DeleteGlobalRef(dropTargetGlobalRef);
    }
}

void WinDropTarget::init(HWND hwnd, jobject targetObj) {
    hWnd = hwnd;
    dropTargetGlobalRef = getEnv()->NewGlobalRef(targetObj);
}

HRESULT WinDropTarget::DragEnter(IDataObject * pDataObject, DWORD grfKeyState, POINTL pt, DWORD * pdwEffect) {
    currentData = NULL;
    if (pdwEffect == NULL) {
        return E_POINTER;
    }

    *pdwEffect &= fireDragEnter(pDataObject, grfKeyState, pt, *pdwEffect);

    if (*pdwEffect != DROPEFFECT_NONE) {
        currentData = pDataObject;
    }

    return S_OK;
}

HRESULT WinDropTarget::DragLeave() {
    fireDragLeave();
    currentData = NULL;
    return S_OK;
}

HRESULT WinDropTarget::DragOver(DWORD grfKeyState, POINTL pt, DWORD *pdwEffect) {
    if (pdwEffect == NULL) {
        return E_POINTER;
    }

    *pdwEffect &= fireDragOver(grfKeyState, pt, *pdwEffect);

    return S_OK;
}

HRESULT WinDropTarget::Drop(IDataObject *pDataObject, DWORD grfKeyState, POINTL pt, DWORD *pdwEffect) {
    if (pdwEffect == NULL) {
        currentData = NULL;
        return E_POINTER;
    }

    *pdwEffect &= fireDrop(pDataObject, grfKeyState, pt, *pdwEffect);
    currentData = NULL;
    return S_OK;
}

int WinDropTarget::fireDrop(IDataObject * pDataObject, DWORD keyState, POINTL pt, DWORD allowedActions) {
    JNIEnv * env = getEnv();
    jint dropAction = (jint)getDropAction(keyState, allowedActions);
    return env->CallIntMethod(dropTargetGlobalRef, methodWinDropTargetDrop, 
        (jlong)pDataObject, (jint)pt.x, (jint)pt.y, dropAction, (jint)allowedActions);
}

int WinDropTarget::fireDragEnter(IDataObject * pDataObject, DWORD keyState, POINTL pt, DWORD allowedActions) {
    JNIEnv * env = getEnv();
    jint dropAction = (jint)getDropAction(keyState, allowedActions);
    return env->CallIntMethod(dropTargetGlobalRef, methodWinDropTargetDragEnter,
        (jlong)pDataObject, (jint)pt.x, (jint)pt.y, dropAction, (jint)allowedActions);
}

int WinDropTarget::fireDragOver(DWORD keyState, POINTL pt, DWORD allowedActions) {
    JNIEnv * env = getEnv();
    jint dropAction = (jint)getDropAction(keyState, allowedActions);
    return env->CallIntMethod(dropTargetGlobalRef, methodWinDropTargetDragOver,
        (jint)pt.x, (jint)pt.y, dropAction, (jint)allowedActions);
}

void WinDropTarget::fireDragLeave() {
    JNIEnv * env = getEnv();
    env->CallVoidMethod(dropTargetGlobalRef, methodWinDropTargetDragLeave);
}

DWORD WinDropTarget::getDropAction(DWORD keyState, DWORD allowedActions) {
    if(keyState & MK_CONTROL) {
        if(keyState & MK_SHIFT) {
                       return DROPEFFECT_LINK;
        } else {
            return DROPEFFECT_COPY;
        }
    } else if(keyState & MK_SHIFT) {
        return DROPEFFECT_MOVE;
    }
    if (allowedActions & DROPEFFECT_MOVE) {
        return DROPEFFECT_MOVE;
    }
    if (allowedActions & DROPEFFECT_COPY) {
        return DROPEFFECT_COPY;
    }
    if (allowedActions & DROPEFFECT_LINK) {
        return DROPEFFECT_LINK;
    }
    return DROPEFFECT_NONE;
}


WinDragSource::WinDragSource() {
    dragSourceGlobalRef = NULL;
}

WinDragSource::~WinDragSource() {
    if (dragSourceGlobalRef != NULL) {
        getEnv()->DeleteGlobalRef(dragSourceGlobalRef);
    }
}

STDMETHODIMP WinDragSource::QueryContinueDrag(BOOL fEscapePressed,
                                              DWORD grfKeyState) {
    if(fEscapePressed == TRUE)
        return DRAGDROP_S_CANCEL;   

    if((grfKeyState & MK_LBUTTON) == 0)
        return DRAGDROP_S_DROP;

    getEnv()->CallVoidMethod(dragSourceGlobalRef, 
            methodWinDragSourceContinueDrag);
    return S_OK;
}

STDMETHODIMP WinDragSource::GiveFeedback(DWORD dwEffect) {
    jint actions = (jint)(dwEffect & ~DROPEFFECT_SCROLL);
    jboolean scroll = (dwEffect & DROPEFFECT_SCROLL) != 0;
    getEnv()->CallVoidMethod(dragSourceGlobalRef, 
            methodWinDragSourceGiveFeedback, actions, scroll);

    return DRAGDROP_S_USEDEFAULTCURSORS;
}

void WinDragSource::init(JNIEnv * env, jobject winDragSource) {
    dragSourceGlobalRef = env->NewGlobalRef(winDragSource);
}
