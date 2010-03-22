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
#ifndef _WIN_DATA_TRANSFER_H_
#define _WIN_DATA_TRANSFER_H_

#include "org_apache_harmony_awt_nativebridge_windows_WinDataTransfer.h"

template<class BASE> class CreatableComObject
{
public:
    static BASE * CreateInstance() {
        CComObject<BASE> * p = NULL;
        CComObject<BASE>::CreateInstance(&p);
        if (p) {
            p->AddRef();
        }
        return p;
    }
};

class WinDataObject : 
    public CComObjectRootEx<CComSingleThreadModel>,
    public IDataObject,
    public CreatableComObject<WinDataObject>
{
    BEGIN_COM_MAP(WinDataObject)
        COM_INTERFACE_ENTRY(IDataObject)
    END_COM_MAP()
public:

// Construction/destruction/initialization
    WinDataObject();
    ~WinDataObject();
    void init(JNIEnv * env, jobject dataSnapshot);
    static void registerFormats();
    
// Helper methods
    static jstring getText(JNIEnv * env, IDataObject * dataObject);
    static jobjectArray getFileList(JNIEnv * env, IDataObject * dataObject);
    static jstring getURL(JNIEnv * env, IDataObject * dataObject);
    static jstring getHTML(JNIEnv * env, IDataObject * dataObject);
    static jobject getBitmap(JNIEnv * env, IDataObject * dataObject, jintArray header);
    static jobjectArray enumFormats(JNIEnv * env, IDataObject * dataObject);
    static jbyteArray getSerialized(JNIEnv * env, IDataObject * dataObject, jstring nativeFormat);
    static jboolean queryFormat(JNIEnv * env, IDataObject * dataObject, jstring nativeFormat);


// IDataObject methods
    STDMETHOD(GetData)(FORMATETC *,STGMEDIUM *);
    STDMETHOD(GetDataHere)(FORMATETC *,STGMEDIUM *);
    STDMETHOD(QueryGetData)(FORMATETC *);
    STDMETHOD(GetCanonicalFormatEtc)(FORMATETC *,FORMATETC *);
    STDMETHOD(SetData)(FORMATETC *,STGMEDIUM *,BOOL);
    STDMETHOD(EnumFormatEtc)(DWORD,IEnumFORMATETC ** );
    STDMETHOD(DAdvise)(FORMATETC *,DWORD,IAdviseSink *,DWORD *);
    STDMETHOD(DUnadvise)(DWORD);
    STDMETHOD(EnumDAdvise)(IEnumSTATDATA **);

private:
       static UINT cfShellUrlA;
       static UINT cfShellUrlW;
       static UINT cfHTML;
    static FORMATETC textFormats[];
    static FORMATETC fileListFormat;
    static FORMATETC urlFormats[];
    static FORMATETC htmlFormat;
    static FORMATETC imageFormats[];

    static HGLOBAL copyMemory(HGLOBAL hGlobal);

    static jstring getStringA(JNIEnv * env, const char * cstr);
    static jstring getStringA(JNIEnv * env, HGLOBAL hGlobal);
    static jstring getStringW(JNIEnv * env, HGLOBAL hGlobal);

    static jstring getStringA(JNIEnv * env, IDataObject * dataObject, UINT format);
    static jstring getStringW(JNIEnv * env, IDataObject * dataObject, UINT format);

    static jobjectArray getFileListA(JNIEnv * env, const char * files);
    static jobjectArray getFileListW(JNIEnv * env, const wchar_t * files);

    static int parseCfHtmlTag(const char * cstr, const char * tag);

    static jobject getDIB(JNIEnv * env, BITMAPINFO * info, jintArray header);
    static jobject getEnhMetaFile(JNIEnv * env, HENHMETAFILE hEMF, jintArray header);
    static jboolean queryFormat(IDataObject * dataObject, UINT format, DWORD tymed = TYMED_HGLOBAL);
    static jstring getSerializedFormatName(JNIEnv * env, UINT format);

    static HGLOBAL getTextGlobal(JNIEnv * env, jstring text, BOOL unicode);

    /** Translate format name to array of native formats,
     *  returns format count or zero if format name is unknown */
    int getFormatsForName(const char * formatName, FORMATETC ** formats);
    int getFormatsForName(JNIEnv * env, jstring formatName, FORMATETC ** formats);

    /** Find a format in formatArray, return its index or -1 if not found */
    int getFormatIndex(FORMATETC * fmt);
    
    UINT getSerializedFormat(JNIEnv * env, jstring formatName);
    
    HRESULT getText(BOOL unicode, STGMEDIUM * pMedium);
    HRESULT getHTML(STGMEDIUM * pMedium);
    HRESULT getFileList(STGMEDIUM * pMedium);
    HRESULT getURL(BOOL unicode, STGMEDIUM * pMedium);
    HRESULT getImage(UINT format, STGMEDIUM * pMedium);
    HRESULT getSerializedObject(UINT format, STGMEDIUM * pMedium);

    CSimpleArray<FORMATETC> formatArray;
    jobject dataSnapshotGlobalRef;
};

class WinDropTarget :
    public CComObjectRootEx<CComSingleThreadModel>,
    public IDropTarget,
    public CreatableComObject<WinDropTarget>
{
    BEGIN_COM_MAP(WinDropTarget)
        COM_INTERFACE_ENTRY(IDropTarget)
    END_COM_MAP()
public:
    WinDropTarget();
    ~WinDropTarget(void);
    void init(HWND hwnd, jobject targetObj);

    STDMETHOD(DragEnter)(IDataObject * pDataObject, DWORD grfKeyState, POINTL pt, DWORD * pdwEffect);
    STDMETHOD(DragLeave)();
    STDMETHOD(DragOver)(DWORD grfKeyState, POINTL pt, DWORD * pdwEffect);
    STDMETHOD(Drop)(IDataObject * pDataObject, DWORD grfKeyState, POINTL pt, DWORD * pdwEffect);

private:

    int fireDrop(IDataObject * pDataObject, DWORD keyState, POINTL pt, DWORD allowedActions);
    int fireDragEnter(IDataObject * pDataObject, DWORD keyState, POINTL pt, DWORD allowedActions);
    int fireDragOver(DWORD keyState, POINTL pt, DWORD allowedActions);
    void fireDragLeave();

    DWORD getDropAction(DWORD keyState, DWORD allowedActions);

    HWND hWnd;
    jobject dropTargetGlobalRef;
    CComPtr<IDataObject> currentData;
};

class WinDragSource : 
    public CComObjectRootEx<CComSingleThreadModel>,
    public IDropSource,
    public CreatableComObject<WinDragSource>
{
    BEGIN_COM_MAP(WinDragSource)
        COM_INTERFACE_ENTRY(IDropSource)
    END_COM_MAP()
public:

// Construction/destruction/initialization
    WinDragSource();
    ~WinDragSource();
    void init(JNIEnv * env, jobject winDragSource);

// IDropSource methods
    STDMETHOD(QueryContinueDrag)(BOOL fEscapePressed, DWORD grfKeyState);
    STDMETHOD(GiveFeedback)(DWORD dwEffect);

private:
    jobject dragSourceGlobalRef;
};

#endif
