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
 * @author Ilya S. Okomin
 *
 */
#include <Windows.h>
#include <WinGDI.h>
#include <Winnls.h>
#include <Winreg.h>
#include <stdio.h>

#include <tchar.h>
#include <string.h>

#include "winFont.h"
#include "org_apache_harmony_awt_gl_font_NativeFont.h"
#include "org_apache_harmony_awt_gl_font_WinGlyph.h"
#include "exceptions.h"

#include <FontLibExports.h> // Font file tables parsing routines for embedFontNative method

static LCIDS lcidTable;
static FontRecords fonts;           /* Cached system fonts data             */
static int famSize = 256;           /* Size of families array               */
static TCHAR** families = NULL;     /* Cached set of system families set    */
static int famCount = 0;            /* Number of families                   */
static int counter = 0;             /* static counter for call back access functions    */

/* Reverses DWORD bytes order */
unsigned long dwReverse(DWORD data)
{
    unsigned char *dataElems = (unsigned char *) &data;
    return (unsigned long)((dataElems[0]<<24) | (dataElems[1]<<16) | (dataElems[2]<<8) | dataElems[3]);
}

/* Reverses WORD bytes order */
unsigned short wReverse(WORD data)
{
    return (unsigned short)(((data<<8) & 0xFF00) | ((data>>8) & 0x00FF));
}

/* Converts point size to logical coordinate space size */
int PointSizetoLogical(HDC hDC, int points, int divisor)
{
    POINT P[2] = // two POINTs in device space whose distance is the needed height
    {
        { 0, 0 },
        { 0, GetDeviceCaps(hDC, LOGPIXELSY) * points } 
    };

    DPtoLP(hDC, P, 2); // map device coordinate to logical size

    return labs(P[1].y - P[0].y) / 72 / divisor;
}

/* Returns font style of the LOGFONT structure */
int getFontStyle(LOGFONT lf){
    int ret = FONT_PLAIN;

    if (lf.lfItalic){
        ret |= FONT_ITALIC;
    }

    if (lf.lfWeight > 500){
        ret |= FONT_BOLD;
    }

    return ret;
}

/* Get array of Unicode Ranges [start1,end1,start2,end2...]
 Description: if parameter (ranges == NULL) then function returns ranges array size equal to
 number of ranges multiplied by 2 */

int GetUnicodeRanges (HFONT hFont, int **ranges){
    HDC hDC = CreateCompatibleDC(NULL);
    HGDIOBJ hOld = SelectObject(hDC, hFont);
    int i, rangesCount, index;
    int arrSize;
    DWORD size; 
    GLYPHSET * pGlyphSet = NULL;

    size = GetFontUnicodeRanges(hDC, NULL);
    pGlyphSet = (GLYPHSET *) malloc(size);

    if (pGlyphSet == NULL){
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return -1;
    }

    pGlyphSet->cbThis = size;
    size = GetFontUnicodeRanges(hDC, pGlyphSet);
    if (size == 0){
        free(pGlyphSet);
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return -1;
    }

    rangesCount = pGlyphSet->cRanges;
    arrSize = rangesCount << 1;
    
    if (*ranges == NULL){
        *ranges = (int *)malloc (arrSize * sizeof(int)); // number of int elements
    } else {
        *ranges = (int *)realloc (*ranges, arrSize * sizeof(int)); // number of int elements
    }

    if (*ranges == NULL){
        free(pGlyphSet);
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return arrSize;
    }

    for (i=0; i < rangesCount; i++){
        index = i << 1; 
        (*ranges)[index] = pGlyphSet->ranges[i].wcLow;
        (*ranges)[index + 1] = pGlyphSet->ranges[i].wcLow + pGlyphSet->ranges[i].cGlyphs -1;
    }

    free(pGlyphSet);
    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    
    return arrSize;
}

/* Adds FontRecord to the list, returns 1 if success, returns 0 if malloc failed */
int addFontRecord(int index, int style, int type, TCHAR *faceName){
    int i;

    // If the number of records exceeds the size of structure
    // we double the size of families array.
    if ( fonts.count == fonts.size) {
        fonts.size = fonts.size << 1;
        fonts.indices = (int *)realloc(fonts.indices, fonts.size * sizeof(int));
        if (fonts.indices == NULL){
            return FONTLIB_ERROR;
        }
        fonts.types = (int *)realloc(fonts.types, fonts.size * sizeof(int));
        if (fonts.types == NULL){
            return FONTLIB_ERROR;
        }
        fonts.styles = (int *)realloc(fonts.styles, fonts.size * sizeof(int));
        if (fonts.styles == NULL){
            return FONTLIB_ERROR;
        }
        fonts.faceNames = (TCHAR **)realloc(fonts.faceNames, fonts.size * sizeof(TCHAR *));
        if (fonts.faceNames == NULL){
            return FONTLIB_ERROR;
        }

    }
    
    fonts.indices[fonts.count] = index;
    fonts.styles[fonts.count] = style;
    fonts.types[fonts.count] = type;
    
    fonts.faceNames[fonts.count] = faceName;

    for (i = 0; ((fonts.indices[i] != index) || (fonts.styles[i] != style)); i ++);

    if(i == fonts.count){
        fonts.count++;
    }

    return FONTLIB_SUCCESS;
    
}

/* Adds family name to the list, returns 1 if success, returns 0 if malloc failed */
int addFamily(TCHAR *fam){

    int i;

    // If the number of records exceeds the size of structure
    // we double the size of families array.
    if ( famCount == famSize) {
        famSize = famSize << 1;
        families = (TCHAR **)realloc(families, famSize*sizeof(TCHAR *));
        if (families == NULL){
            return FONTLIB_ERROR;
        }
    }
    families[famCount] = (TCHAR *)malloc((_tcslen(fam)+1) * sizeof(TCHAR));
    _tcscpy( families[famCount], fam);
    
    for (i = 0; (_tcscmp(families[i], fam) != 0); i++);

    if(i == famCount){
        famCount++;
    }

    return FONTLIB_SUCCESS;
}


/* Callback processing function for families enumeration */

int static CALLBACK EnumFontFamProc(ENUMLOGFONTEX *lpelfe, NEWTEXTMETRICEX *lpntme, 
                                      int FontType, LPARAM lParam) 
{
    if (FontType & (TRUETYPE_FONTTYPE | DEVICE_FONTTYPE)){
        return addFamily((TCHAR *)(lpelfe->elfLogFont.lfFaceName));
    }
    return FONTLIB_SUCCESS;
} 

/* Callback processing function for EnumFontFamEx */
int static CALLBACK EnumFontProc(ENUMLOGFONTEX *lpelfe, NEWTEXTMETRICEX *lpntme, 
                                      int FontType, LPARAM lParam) 
{
    int style;
    int index;
    const int FACE_SIZE = 2*LF_FACESIZE+1; // family count + style size + 1 (space char)
    TCHAR *face = (TCHAR *)malloc(FACE_SIZE*sizeof(TCHAR));
    if (FontType & (TRUETYPE_FONTTYPE | DEVICE_FONTTYPE)){
        index = (int)lParam;
        style = getFontStyle(lpelfe->elfLogFont);
        // XXX: if style = plain, lpelfe->elfStyle == "Regular" in user's locale
        // we use simply family name

        face[FACE_SIZE-1] = 0;
        if (style != FONT_PLAIN){
            _sntprintf(face, FACE_SIZE, L"%s %s", lpelfe->elfLogFont.lfFaceName, lpelfe->elfStyle);
        }else   
            _tcsncpy( face, lpelfe->elfLogFont.lfFaceName, FACE_SIZE);

        return addFontRecord(index, style, FontType, face);
    }
    return FONTLIB_SUCCESS;
} 

/* Enumerates font families, returns 0 if there is error */
int enumFamilies(){
    HDC hDC = CreateCompatibleDC(NULL);
    LOGFONT lf; 
    int res;

    memset(& lf, 0, sizeof(lf));

    lf.lfCharSet = DEFAULT_CHARSET;
    lf.lfFaceName[0] = '\0';

    if (families == NULL){
        families = (TCHAR **)malloc(famSize * sizeof(TCHAR *));
        if (families == 0){
            DeleteDC(hDC);
            // Not enough memory to load font family names list
            return FONTLIB_ERROR;
        }
    } else {
        famCount = 0;
    }
    
    res = EnumFontFamiliesEx(hDC, & lf, (FONTENUMPROC) EnumFontFamProc, 0, 0);
    DeleteDC(hDC);

    if (res == FONTLIB_ERROR){
        // Not enough memory to enumerate font family names list.
        return FONTLIB_ERROR;
    }
    
    return FONTLIB_SUCCESS;
}

/* Enumerates font face names, returns 0 if there is error */
int enumFonts(){
    HDC hDC = CreateCompatibleDC(NULL);
    int i;
    int res;
    LOGFONT lf; 

    memset(& lf, 0, sizeof(lf));

    lf.lfCharSet = DEFAULT_CHARSET;

    // !! All font face and font family names cached only once.
    // In some cases when application is active new fonts can be 
    // installed(removed) into the system and it isn't taken into account. 
    // From the other hand it is too expencive in terms of performance re-cache 
    // names lists each time font to be created or removed from system this 
    // action must be performed every time new Font object in Java is being 
    // created. 
    // If system fonts changed current font enumeration implementation might 
    // rise an exception.
    
    if (families == NULL){
        enumFamilies();
    } 

    if (fonts.indices == NULL){
        fonts.size = 256;

        fonts.indices = (int *)realloc(fonts.indices, fonts.size * sizeof(int));
        if (fonts.indices == NULL){
            DeleteDC(hDC);
            return FONTLIB_ERROR;
        }
        fonts.types = (int *)realloc(fonts.types, fonts.size * sizeof(int));
        if (fonts.types == NULL){
            DeleteDC(hDC);
            return FONTLIB_ERROR;
        }
        fonts.styles = (int *)realloc(fonts.styles, fonts.size * sizeof(int));
        if (fonts.styles == NULL){
            DeleteDC(hDC);
            return FONTLIB_ERROR;
        }
    
        fonts.faceNames = (TCHAR **)realloc(fonts.faceNames, fonts.size * sizeof(TCHAR *));
        if (fonts.faceNames == NULL){
            DeleteDC(hDC);
            return FONTLIB_ERROR;
        }

    }
    fonts.count = 0;
    
    for (i = 0; i < famCount; i++){
        _tcsncpy(lf.lfFaceName, families[i], LF_FACESIZE);
        res = EnumFontFamiliesEx(hDC, & lf, (FONTENUMPROC) EnumFontProc, i, 0);

        if (res == FONTLIB_ERROR){
            DeleteDC(hDC);
            // Not enough memory to enumerate fonts list.
            return FONTLIB_ERROR;
        }
    }
    DeleteDC(hDC);
    return FONTLIB_SUCCESS;
}


/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getFontFamiliesNames
* Signature: ()[Ljava/lang/String;
* Description:  Returns array of all families installed onto the system.
*/
JNIEXPORT jobjectArray JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getFontFamiliesNames(JNIEnv *env, jclass obj) {

    int i = 0;
    int res;
    jobjectArray ret;
    jstring initStr ;
    jclass strClass;
    
    res = enumFamilies();

    if (res == FONTLIB_ERROR){
        throwNPException(env, "Not enough memory to enumerate font family names list.");
        return NULL;
    }

    strClass = env->FindClass("java/lang/String");
    initStr = env->NewStringUTF("");
    ret = (jobjectArray)env->NewObjectArray(famCount,  
        strClass,
        initStr);

    for (;i < famCount;i++){
        env->SetObjectArrayElement(ret,i,env->NewString((const jchar *)families[i], (jsize)_tcslen(families[i]))); // number of chars == length of string -1
    }
    return ret;

}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    enumSystemFonts
* Signature: ()V;
* Description: Sets arrays of available Font data in Java.
*/
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_font_NativeFont_enumSystemFonts(JNIEnv *env, jclass obj) {
    int i = 0;
    int res;
    jintArray indArr;
    jintArray typesArr;
    jintArray stylesArr;
    jclass cls;
    jmethodID mid;

    jclass strClass;
    jstring initStr;
    jobjectArray faces;
    res = enumFonts();

    if (res == FONTLIB_ERROR){
        throwNPException(env, "Not enough memory to enumerate font names list.");
        return;
    }

    cls= env->FindClass("org/apache/harmony/awt/gl/font/NativeFont");
    if (cls == 0) {
        throwNPException(env, "Can't find class NativeFont");
        return;
    }

    mid=env->GetStaticMethodID(cls, "setArrays", "([I[I[I[Ljava/lang/String;)V");
    if (mid == 0) {
        throwNPException(env, "Can't find method sendArrayResults");
        return;
    }

    typesArr = env->NewIntArray(fonts.count);
    stylesArr = env->NewIntArray(fonts.count);
    indArr = env->NewIntArray(fonts.count);

    strClass = env->FindClass("java/lang/String");
    initStr = env->NewStringUTF("");
    faces = (jobjectArray)env->NewObjectArray(fonts.count,  
        strClass,
        initStr);

    for (;i < fonts.count; i++){
        env->SetObjectArrayElement(faces,i,env->NewString((const jchar *)fonts.faceNames[i], (jsize)_tcslen(fonts.faceNames[i]))); // number of chars == length of string -1
    }


    if ((indArr == NULL) || (typesArr == NULL) || (stylesArr == NULL)){
        throwNPException(env, "Not enough memory to create font data arrays.");
        return;
    }

    env->SetIntArrayRegion(typesArr, 0, fonts.count, (jint *)fonts.types); 
    env->SetIntArrayRegion(stylesArr, 0, fonts.count, (jint *)fonts.styles); 
    env->SetIntArrayRegion(indArr, 0, fonts.count, (jint *)fonts.indices); 

    env->ExceptionClear();
    env->MonitorEnter(obj);
    env->CallStaticVoidMethod(cls, mid, typesArr, stylesArr, indArr, faces);
    env->MonitorExit(obj);

}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    embedFontNative
* Signature: (Ljava/lang/String;)Z
* Description: Returns TRUE if Font resource from font file added successfully to a system.
*/
JNIEXPORT jstring JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_embedFontNative(JNIEnv *env, jclass obj, jstring absPath) {

    jboolean iscopy;
    int fontAdded = 0;
    const TCHAR * path;
    path = (TCHAR *)(env->GetStringCritical(absPath, &iscopy));

    fontAdded = AddFontResourceEx(path, FR_PRIVATE, 0);
    
    fwchar_t *familyName = 0;

    if (fontAdded) {
        getFontFamilyName((fwchar_t *)path, &familyName);
    }    

    env->ReleaseStringCritical(absPath, (const jchar *)path);
    
    jstring res = 0;
    if (fontAdded && familyName) {
        int len = wcslen((wchar_t*)familyName);
        res = env->NewString((jchar *)familyName, len);
        delete familyName;
    }
    
    return res;
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    initializeFont
* Signature: (Lorg/apache/harmony/awt/gl/Font;)J
* Description: Returns pointer to Font object created with given parameters.
*/
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_font_NativeFont_initializeFont(JNIEnv *env, 
        jclass obj, jobject winFont, jstring jFace, jint style, jint size) {
    jclass cls;
    jmethodID mid;
    jboolean iscopy;
    jintArray ranges;
    int arrSize;
    jsize length;
    int * pRanges = NULL;
    const TCHAR * fontName;
    HFONT res;
    LOGFONT lf; // LOGFONT structure for given Font's parameters
    memset(& lf, 0, sizeof(lf));

    cls = env->GetObjectClass(winFont);

    lf.lfHeight = - size;
    
    lf.lfWidth = 0; // need to be defined
    lf.lfEscapement = 0; // need to be defined
    lf.lfOrientation = 0; // need to be defined

    if (style & FONT_BOLD) {
        lf.lfWeight = FW_BOLD; // need to be defined from TextAttributes
    } else {
        lf.lfWeight = FW_REGULAR; // need to be defined from TextAttributes
    }

    lf.lfItalic = (BYTE)(style & FONT_ITALIC);

    lf.lfStrikeOut = FALSE;
    lf.lfUnderline = FALSE;
    lf.lfCharSet = DEFAULT_CHARSET;
    lf.lfOutPrecision = OUT_TT_ONLY_PRECIS;
    lf.lfClipPrecision  = CLIP_DEFAULT_PRECIS;
    lf.lfQuality = NONANTIALIASED_QUALITY;//DEFAULT_QUALITY; // Rendering hints ?

    lf.lfPitchAndFamily = DEFAULT_PITCH;
    
    length = env->GetStringLength(jFace);
    fontName = (const TCHAR *)env->GetStringCritical(jFace, &iscopy);
    lf.lfFaceName[0] = 0;

    if ( fontName ) {
        // Due to MSDN length of FaceName parameter
        if (length < 32){
            _tcsncpy(lf.lfFaceName, fontName, length);
        }
    }

    env->ReleaseStringCritical(jFace, (const jchar *)fontName);

    res = CreateFontIndirect(& lf);

    GetObject(res, sizeof(lf), &lf);


    mid=env->GetMethodID(cls, "setLogicalHeight","(I)V");

    if (mid == 0) {
        // "Can't find method setUnicodeRanges"
        env->ExceptionDescribe();
        env->ExceptionClear();

        return (jlong)NULL;
    }

    env->CallVoidMethod(winFont, mid, lf.lfHeight);
    if(env->ExceptionOccurred()) {
        env->ExceptionDescribe();
        env->ExceptionClear();

        return (jlong)NULL;
    }

    mid=env->GetMethodID(cls, "setUnicodeRanges","([I)V");

    if (mid == 0) {
        // "Can't find method setUnicodeRanges"
        env->ExceptionDescribe();
        env->ExceptionClear();

        return (jlong)NULL;
    }

    // Get number of unicode range pairs' bounds values 
    arrSize = GetUnicodeRanges(res, &pRanges);
    if (arrSize == -1){
        throwNPException(env, "Error occured during getting an array of unicode ranges");

        return (jlong)NULL;
    }

    ranges =(jintArray)env->NewIntArray(arrSize);
    env->SetIntArrayRegion(ranges, 0, arrSize,(jint *)pRanges);
    free(pRanges);
    env->ExceptionClear();

    env->CallVoidMethod(winFont, mid, ranges);
    if(env->ExceptionOccurred()) {
        //  "error occured copying array up to Java"
        env->ExceptionDescribe();
        env->ExceptionClear();

        return (jlong)NULL;
    }

    return (jlong)res;
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    canDisplayCharNative
* Signature: (JC)Z
* Description: Returns TRUE if given char has glyph index in Font object.
*/
JNIEXPORT jboolean JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_canDisplayCharNative(JNIEnv *env, jclass obj, jlong fnt, jchar c) {

    DWORD size;
    WORD gi[1];
    HFONT hFont  = (HFONT)fnt; // Handle to Font object
    HGDIOBJ hOld;
    TCHAR sample[1];
    sample[0] = (TCHAR)c;
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    size = GetGlyphIndices(hDC, sample, 1, gi, GGI_MARK_NONEXISTING_GLYPHS);
    if (size == GDI_ERROR){
        throwNPException(env, "Error occured during getting char data in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return FALSE;
    }

    SelectObject(hDC, hOld);
    DeleteDC(hDC);
   
    return ((size != 0) && (gi[0] != 0xFFFF));
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getFamilyNative
* Signature: (J)Ljava/lang/String;
* Description: Returns Family Name of the given Font object.
*/
JNIEXPORT jstring JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getFamilyNative(JNIEnv *env, jclass obj, jlong fnt) {

    DWORD size;

    HFONT hFont  = (HFONT)fnt;
    HGDIOBJ hOld;
    jstring res;
    TCHAR name[LF_FULLFACESIZE];
    OUTLINETEXTMETRIC outm[3];
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);

    if (size == 0 ){
        throwNPException(env, "Error occured during getting text metrics in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }
    memset(& name, 0, sizeof(name));

    _tcscpy(name, (TCHAR *)((char *) outm + (int) outm[0].otmpFamilyName));

    res = env->NewString((const jchar *)name, (int)_tcslen(name));

    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    return res;
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getFontNameNative
* Signature: (J)Ljava/lang/String;
* Description: Returns Font Face Name of given Font object
*/
JNIEXPORT jstring JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getFontNameNative(JNIEnv *env, jclass obj, jlong fnt) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt;
    HGDIOBJ hOld;
    jstring res;
    TCHAR name[64];
    OUTLINETEXTMETRIC outm[3];
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
    if (size == 0 ){
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    memset(& name, 0, sizeof(name));
    _tcscpy(name, (TCHAR *)((char *) outm + (int) outm[0].otmpFaceName));

    res = env->NewString((const jchar *)name, (jsize)_tcslen(name));

    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    return res;

}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    pFontFree
* Signature: (J)I
* Description: Frees given Font object.
*/
JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_pFontFree(JNIEnv *env, jclass obj, jlong fnt) {

    HFONT hFont  = (HFONT)fnt;

    return DeleteObject(hFont);
}


/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    RemoveFontResource
* Signature: (Ljava/lang/String;)Z
* Description: Removes font resourse corresponding to the given Font object.
*/
JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_RemoveFontResource(JNIEnv *env, jclass obj, jstring absPath) {

    jboolean iscopy;
    int fontRemoved = 0;
    const TCHAR * path;

    path = (TCHAR *)(env->GetStringCritical(absPath, &iscopy));

    fontRemoved = RemoveFontResource(path);

    env->ReleaseStringCritical(absPath, (const jchar *)path);

    return fontRemoved;
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getItalicAngleNative
* Signature: (J)F
* Description: Returns tangent of Italic angle of given Font.
*/
JNIEXPORT jfloat JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getItalicAngleNative(JNIEnv *env, jclass obj, jlong fnt) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt; // Handle to Font object
    HGDIOBJ hOld;
    jfloat res;
    OUTLINETEXTMETRIC outm[3];
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
    if (size == 0 ){
        throwNPException(env, "Error occured during getting text outline metrics in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return 0;
    }

    res =  ((float)outm[0].otmsCharSlopeRun) / ((float)outm[0].otmsCharSlopeRise);

    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    return res;

}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getDefaultChar
* Signature: (J)F
* Description: Returns default char value of given Font.
*/
JNIEXPORT jchar JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getDefaultCharNative(JNIEnv *env, jclass obj, jlong fnt) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt; // Handle to Font object
    HGDIOBJ hOld;
    jchar res;
    TEXTMETRIC tm;
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    size = GetTextMetrics(hDC, &tm);
    if (size == 0 ){
        throwNPException(env, "Error occured during getting text metrics in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return 0;
    }

    res = (jchar)(tm.tmDefaultChar);

    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    return res;
}


/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getFonts
* Signature: ()[Ljava/lang/String;
* Description: Returns array of available Font Names.
*       !! Doesn't enumerate Type1 fonts !!
*/
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_awt_gl_font_NativeFont_getFonts(JNIEnv *env, jclass obj) {

    int i = 0;
    jobjectArray ret;
    jclass strClass;
    jstring initStr;
    const TCHAR * fontName;
    TCHAR * pdest;
    TCHAR ** fontNames; // list of font names
    int * fontSizes;    // list of sizes of font names
    int size = 256;
    int counter = 0;

    const TCHAR Key_Fonts[] = _T("SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Fonts");
    const TCHAR TrueType[] = _T(" (TrueType)");
    HKEY hKey;


    if ( RegOpenKeyEx(HKEY_LOCAL_MACHINE, Key_Fonts, 0, KEY_READ, & hKey)==ERROR_SUCCESS )
    {
        fontNames = (TCHAR **)malloc(sizeof(TCHAR *) * size);
        fontSizes = (int *)malloc(sizeof(int) * size);

        for (i=0; ; i++)
        {
            TCHAR szValueName[MAX_PATH];
            BYTE  szValueData[MAX_PATH];

            DWORD nValueNameLen = MAX_PATH;
            DWORD nValueDataLen = MAX_PATH;
            DWORD dwType;

            if ( RegEnumValue(hKey, i, szValueName, & nValueNameLen, NULL,
                & dwType, szValueData, & nValueDataLen) != ERROR_SUCCESS )
                break;

            fontName = (const TCHAR *) szValueName;
            pdest = (TCHAR *)_tcsstr( fontName, TrueType );
            if (pdest !=NULL ){
                if (counter == (size - 1)) {
                    size = size << 1;
                    fontNames = (TCHAR **)realloc(fontNames, sizeof(TCHAR *) * size);
                    fontSizes = (int *)realloc(fontSizes, sizeof(int) * size);
                }

                fontSizes[counter] = (int)(pdest - fontName);

                fontNames[counter] = (TCHAR*)calloc(fontSizes[counter], sizeof(TCHAR));
                _tcsncpy(fontNames[counter], fontName, fontSizes[counter]);

                counter++;
            }


        }
        RegCloseKey(hKey);
        strClass = env->FindClass("java/lang/String");
        initStr = env->NewStringUTF("");
        ret = (jobjectArray)env->NewObjectArray(counter,
            strClass,
            initStr);

        for (i = 0;i < counter;i++){
            env->SetObjectArrayElement(ret,i,env->NewString((const jchar *)fontNames[i], fontSizes[i]));
            free(fontNames[i]);
        }

        free(fontNames);
        free(fontSizes);

        return ret;
    } else {
        return NULL;
    }
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getNativeLineMetrics
* Signature: (JIZZI)[F
* Description: Returns common text metrics of the specified Font.
*/
JNIEXPORT jfloatArray JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getNativeLineMetrics(JNIEnv *env, jclass obj, jlong fnt, jint fontHeight, jboolean isAntialiased, jboolean usesFractionalMetrics, jint fontType) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt;
    HGDIOBJ hOld;
    HGDIOBJ hFontEM;
    OUTLINETEXTMETRIC outm[3];
    TEXTMETRIC tm;
    jfloat result[16];
    jfloatArray flArray;
    LOGFONT lf;
    int emsquare;
    int height;
    height = fontHeight;
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    // Getting current size of Font
    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
    if (size == 0 ){
        throwNPException(env, "Error occured during getting text outline metrics in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    tm = outm[0].otmTextMetrics;

    /* logical TEXTMETRICS */
    result[8] = (float)tm.tmAscent; // Precise Ascent value
    result[9] = (float)tm.tmDescent; // Precise Descent value

    if (fontType == FONT_TYPE_TT){
        result[10] = (float)tm.tmExternalLeading; // Precise Leading value
    } else {
        // Type1 has null tmExternalLeading, using tmInternalLeading value
        result[10] = (float)tm.tmInternalLeading;
    }
    result[11] = (float)outm[0].otmsUnderscoreSize; // Precise Underline size value
    result[12] = (float)outm[0].otmsUnderscorePosition; // Precise Underline position value
    result[13] = (float)outm[0].otmsStrikeoutSize; // Precise Strikeout line size value
    result[14] = (float)outm[0].otmsStrikeoutPosition; // Precise Strikeout line position value
    result[15] = (float)tm.tmMaxCharWidth; // Precise Max char width
    emsquare = outm[0].otmEMSquare; // EM Square size

    // Create font with height == "EM Square size" in device units to
    // get multyply factor
    GetObject(hFont, sizeof(lf), &lf);
    lf.lfHeight = -emsquare;
    lf.lfWidth = 0;
    hFontEM = CreateFontIndirect(&lf);

    SelectObject(hDC, hFontEM);

    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
    if (size == 0 ){
        throwNPException(env, "Error occured during getting text outline metrics in native code.");

        SelectObject(hDC, hOld);
        DeleteObject(hFontEM);
        DeleteDC(hDC);
        return NULL;
    }

    tm = outm[0].otmTextMetrics;


    result[0] = (float)(tm.tmAscent  * height)/emsquare; // Precise Ascent value
    result[1] = (float)(tm.tmDescent * height)/emsquare; // Precise Descent value
    if (fontType == FONT_TYPE_TT){
        result[2] = (float)(tm.tmExternalLeading * height)/emsquare; // Precise Leading value
    } else {
        // Type1 has null tmExternalLeading, using tmInternalLeading value
        result[2] = (float)(tm.tmInternalLeading * height)/emsquare;
    }
    result[3] = (float)(outm[0].otmsUnderscoreSize * height)/emsquare; // Precise Underline size value
    result[4] = (float)(outm[0].otmsUnderscorePosition * height)/emsquare; // Precise Underline position value
    result[5] = (float)(outm[0].otmsStrikeoutSize * height)/emsquare; // Precise Strikeout line size value
    result[6] = (float)(outm[0].otmsStrikeoutPosition * height)/emsquare; // Precise Strikeout line position value
    result[7] = (float)(tm.tmMaxCharWidth * height)/emsquare; // Precise Max char width

    flArray=env->NewFloatArray(16);
    env->SetFloatArrayRegion(flArray, 0, 16, result);

    SelectObject(hDC, hOld);
    DeleteObject(hFontEM);
    DeleteDC(hDC);

    return flArray;

}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getGlyphInfoNative
* Signature: (JCI)[F
* DEscription: Returns metrics of glyph of the specified character.
*/
JNIEXPORT jfloatArray JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getGlyphInfoNative(JNIEnv *env, jclass obj, jlong fnt, jchar chr, jint fontHeight) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt;
    HFONT hFontEM;
    HGDIOBJ hOld;
    ABC abcWidth[1];
    jfloatArray metrics;
    GLYPHMETRICS g_metrics;
    MAT2 mat2;
    OUTLINETEXTMETRIC outm[3];

    float arr[11];
    int height; // height of given Font
    LOGFONT lf;
    int emsquare;
    int res;
    height = fontHeight;
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    // Getting current height of given Font
    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
    if (size == 0 ){
        throwNPException(env, "Error occured during getting text outline metrics in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    // Set transform matrix values to identity matrix values
    ZeroMemory(&mat2, sizeof(MAT2));

    mat2.eM11.value = 1;
    mat2.eM12.value = 0;
    mat2.eM21.value = 0;
    mat2.eM22.value = 1;

    res = GetCharABCWidths(hDC, chr, chr, abcWidth);
    if (res == 0 ){
        throwNPException(env, "Error occured during getting char widths in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    arr[6] = (float) abcWidth[0].abcA; // Left glyph spacing
    arr[7] = (float) abcWidth[0].abcB; // Width of glyph
    arr[8] = (float) abcWidth[0].abcC; // Right glyph spacing

    res = GetGlyphOutline(hDC, chr, GGO_METRICS, & g_metrics, 0, NULL, & mat2);
    if (res == GDI_ERROR ){
        throwNPException(env, "Error occured during getting glyph outline in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    arr[9] = (float)g_metrics.gmBlackBoxX; // Glyph Bounds : width
    arr[10] = (float)g_metrics.gmBlackBoxY; // Glyph Bounds : height

    emsquare = outm[0].otmEMSquare; // EM Square size

    GetObject(hFont, sizeof(lf), &lf);
    lf.lfHeight = -emsquare;
    lf.lfWidth = 0;
    hFontEM = CreateFontIndirect(&lf);

    SelectObject(hDC, hFontEM);

    size = GetGlyphOutline(hDC, chr, GGO_METRICS, & g_metrics, 0, NULL, & mat2);
    if (size==GDI_ERROR){
        throwNPException(env, "Error occured during getting glyph outline metrics in native code.");
        SelectObject(hDC, hOld);
        DeleteObject(hFontEM);
        DeleteDC(hDC);
        return NULL;
    }

    arr[0] = (float)(g_metrics.gmptGlyphOrigin.x * height)/emsquare; // Precise Glyph Bounds : X
    arr[1] = (float)(g_metrics.gmptGlyphOrigin.y * height)/emsquare; // Precise Glyph Bounds : Y
    arr[2] = (float)(g_metrics.gmCellIncX * height)/emsquare; // Precise AdvanceX
    arr[3] = (float)(g_metrics.gmCellIncY * height)/emsquare; // Precise AdvanceY ?= Ascent+Descent
    arr[4] = (float)(g_metrics.gmBlackBoxX * height)/emsquare; // Precise Glyph Bounds : width
    arr[5] = (float)(g_metrics.gmBlackBoxY * height)/emsquare; // Precise Glyph Bounds : height


    metrics = env->NewFloatArray(11);
    env->SetFloatArrayRegion(metrics, 0, 11, arr);

    SelectObject(hDC, hOld);
    DeleteObject(hFontEM);
    DeleteDC(hDC);

    return metrics;

}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getGlyphPxlInfoNative
* Signature: (JCI)[F
* DEscription: Returns metrics of glyph of the specified character.
*/
JNIEXPORT jintArray JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getGlyphPxlInfoNative(JNIEnv *env, jclass obj, jlong fnt, jchar chr) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt;
    HGDIOBJ hOld;
    jintArray metrics;
    GLYPHMETRICS g_metrics;
    MAT2 mat2;
    int arr[6];
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    // Set transform matrix values to identity matrix values
    ZeroMemory(&mat2, sizeof(MAT2));

    mat2.eM11.value = 1;
    mat2.eM12.value = 0;
    mat2.eM21.value = 0;
    mat2.eM22.value = 1;

    size = GetGlyphOutline(hDC, chr, GGO_METRICS, & g_metrics, 0, NULL, & mat2);
    if (size==GDI_ERROR){
        throwNPException(env, "Error occured during getting glyph outline in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }


    arr[0] = g_metrics.gmptGlyphOrigin.x ; // Glyph Pixels Bounds : X
    arr[1] = g_metrics.gmptGlyphOrigin.y ; // Glyph Pixels Bounds : Y
    arr[2] = g_metrics.gmCellIncX; // Pixels AdvanceX
    arr[3] = g_metrics.gmCellIncY ; // Pixels AdvanceY ?= Ascent+Descent
    arr[4] = g_metrics.gmBlackBoxX ; // Glyph Pixels Bounds : width
    arr[5] = g_metrics.gmBlackBoxY ; // Glyph Pixels Bounds : height


    metrics = env->NewIntArray(6);
    env->SetIntArrayRegion(metrics, 0, 6, (jint *)arr);

    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    return metrics;
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getGlyphCodesNative
* Signature: (JLjava/lang/String;I)[I
* Description: Returns an array of Glyph codes corresponding to the specified string.
*/
JNIEXPORT jintArray JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getGlyphCodesNative(JNIEnv *env, jclass obj, jlong fnt, jstring str, jint len) {

    DWORD size;
    HFONT hFont  = (HFONT)fnt; // Font object handle
    HGDIOBJ hOld;
    jboolean isCopy;
    jintArray intArray;
    int leng = len;
    int i;
    TCHAR * chars; // Char vector
    WORD * gi;  // Vector of indices
    int * arr;
    HDC hDC = CreateCompatibleDC(NULL);
    
    gi = (WORD *)malloc(len*sizeof(WORD));
    hOld = SelectObject(hDC, hFont);

    chars = (TCHAR *)env->GetStringCritical(str, &isCopy);

    size = GetGlyphIndices(hDC, chars, leng, gi, 0);//GGI_MARK_NONEXISTING_GLYPHS);
    if (size == GDI_ERROR){
        throwNPException(env, "Error occured during getting glyph indices in native code.");
        free(gi);
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    arr = (int *)malloc(len*sizeof(int));

    for (i = 0; i < leng; i++ ){
        arr[i] = (int)gi[i];
    }

    intArray=env->NewIntArray(leng);
    env->SetIntArrayRegion(intArray, 0, leng, (jint *)arr);

    free(gi);
    free(arr);
    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    env->ReleaseStringCritical(str, (const jchar *)chars);

    return intArray;
}


/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    NativeInitGlyphImage
* Signature: (Lorg/apache/harmony/awt/gl/font/Glyph;)[B
* Description: Getting glyph raster using GetGlyphOutline - faster method
* We obtain only glyph representation in the "black box" the smallest box, surrounding glyph.
*/
JNIEXPORT jbyteArray JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_NativeInitGlyphImage(JNIEnv *env, jclass jobj, jobject glyph){

    jclass cls;
    jmethodID mid;
    jbyteArray bmp;
    HFONT hFont;
    HGDIOBJ hOld;
    MAT2 mat2;
    GLYPHMETRICS gMetrics;
    int bitsSize, size;
    DWORD * pBits;   // glyph bits array pointer
    jchar uChar;
    HDC hDC = CreateCompatibleDC(NULL);
    
    // cls - instance of Glyph class
    cls = env->GetObjectClass(glyph);

    // Get "Glyph.pFont" value
    mid=env->GetMethodID(cls,
        "getPFont",
        "()J");
    if (mid == 0) {
        // "Can't find method getChar"
        env->ExceptionDescribe();
        env->ExceptionClear();
        DeleteDC(hDC);        
        return NULL;
    }

    hFont = (HFONT)env->CallLongMethod(glyph, mid);

    // Get "Glyph.getChar()" value
    mid=env->GetMethodID(cls,
        "getChar",
        "()C");
    if (mid == 0) {
        // "Can't find method getChar"
        env->ExceptionDescribe();
        env->ExceptionClear();
        DeleteDC(hDC);
        return NULL;
    }

    uChar = env->CallCharMethod(glyph, mid);

    ZeroMemory(&mat2, sizeof(MAT2));

    mat2.eM11.value = 1;
    mat2.eM22.value = 1;
    
    hOld = SelectObject(hDC, hFont);
    bitsSize = GetGlyphOutline(hDC, uChar, GGO_BITMAP, & gMetrics, 0, NULL, &mat2);

    if (bitsSize==GDI_ERROR){
        throwNPException(env, "Error occured during getting glyph outline in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }

    size = bitsSize; 
    pBits = (DWORD *)malloc(size);  // memory allocation for bits data
    bmp = env->NewByteArray(size); // resulting Int Array ; 

    bitsSize = GetGlyphOutline(hDC, uChar, GGO_BITMAP, & gMetrics, bitsSize, pBits, &mat2);

    if ( bitsSize==GDI_ERROR){
        throwNPException(env, "Error occured during getting glyph outline in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        free(pBits);
        return NULL;
    }

    if ( bitsSize== 0){
        // Empty glyph like SPACE
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        free(pBits);
        return NULL;
    }

    env->SetByteArrayRegion(bmp, 0, size, (jbyte *)pBits);
    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    free(pBits);

    return bmp;
}

/*
* Class:     org_apache_harmony_awt_gl_font_NativeFont
* Method:    getGlyphCodeNative
* Signature: (JLjava/lang/String;I)[I
* Description: Returns Glyph code corresponding to the specified character.
*/
JNIEXPORT jint JNICALL 
Java_org_apache_harmony_awt_gl_font_NativeFont_getGlyphCodeNative(JNIEnv *env, jclass obj, jlong fnt, jchar chr) {

    HFONT hFont  = (HFONT)fnt; // Font object handle
    HGDIOBJ hOld;
    WORD gi[1];     // Vector of indices
    int size;
    TCHAR uChr[1];
    HDC hDC = CreateCompatibleDC(NULL);

    uChr[0] = (TCHAR)chr;
    hOld = SelectObject(hDC, hFont);

    size = GetGlyphIndices(hDC, uChr, 1, gi, 0);//GGI_MARK_NONEXISTING_GLYPHS);
    if (size==GDI_ERROR){
        throwNPException(env, "Error occured during getting glyph indices in native code.");
        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return 0;
    }

    SelectObject(hDC, hOld);
    DeleteDC(hDC);

    return (size == 0)?0:gi[0];
}

/*
 * Class:     org_apache_harmony_awt_gl_font_NativeFont
 * Method:    getGlyphOutline
 * Signature: (JCJ)I
 * Description: Sets Glyph outline corresponding to the specified character to 
 * the buffer parameter.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_getGlyphOutline
  (JNIEnv *env, jclass obj, jlong fnt, jchar uChar, jlong buffer, jint size){

    HFONT hFont  = (HFONT)fnt; // Font object handle
    HGDIOBJ hOld;
    MAT2 mat2;
    GLYPHMETRICS g_metrics;
    HDC hDC = CreateCompatibleDC(NULL);
    hOld = SelectObject(hDC, hFont);

    // Set transform matrix values to identity matrix values
    ZeroMemory(&mat2, sizeof(MAT2));

    mat2.eM11.value = 1;
    mat2.eM12.value = 0;
    mat2.eM21.value = 0;
    mat2.eM22.value = 1;

    size = GetGlyphOutline(hDC, uChar, GGO_NATIVE, &g_metrics, size, (LPVOID)buffer, &mat2);
    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    
    if (size==GDI_ERROR){
        throwNPException(env, "GDI :: GetGlyphOutline Error.");
        return 0;
    }
    return size;
}

/* Call back function that enumerates system locales */
BOOL CALLBACK EnumLocalesProc(LPTSTR lpLocaleString){
  
    LCID localeID = 0;
    TCHAR countryName[5] = {0};
    TCHAR langName[5] = {0};

    localeID = (LCID)_tcstoi64(lpLocaleString, NULL, 16 );

     if (GetLocaleInfo ( localeID, LOCALE_SISO639LANGNAME , langName, sizeof(langName))){
        lcidTable.lcids[lcidTable.count] = LANGIDFROMLCID(localeID);
        _tcscat(lcidTable.names[lcidTable.count], langName);

        if(GetLocaleInfo ( localeID, LOCALE_SISO3166CTRYNAME, countryName, sizeof(countryName))){
            _tcscat(lcidTable.names[lcidTable.count], L"_");
            _tcscat(lcidTable.names[lcidTable.count], countryName);
        }
        lcidTable.count++;

     } 
    return TRUE; 
}

/* Initialize arrays of short LCID strings and LCID values arrays. 
   Returns size of arrays if success, otherwise returns 0.
 */

JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_nativeInitLCIDsTable
    (JNIEnv *env, jclass obj, jobjectArray shortStrings, jshortArray LCIDs){

    int size;
    int i;

    if ((shortStrings == NULL) || (LCIDs == NULL)){
        EnumSystemLocales(EnumLocalesProc, // callback function
                  LCID_SUPPORTED   // locales
                );
        return lcidTable.count;
    }

    size = lcidTable.count;

    env->SetShortArrayRegion(LCIDs, 0, size, lcidTable.lcids);

    for (i = 0; i < size; i++){
            env->SetObjectArrayElement(shortStrings, i, env->NewString((const jchar *)(TCHAR *)lcidTable.names[i], (jsize)_tcslen(lcidTable.names[i])));
    }

    return size;
}


/*
 *  Returns desired tag index from the list.
 */
int getTagIndex(Tag* tagList, int count, Tag value){
    int result = -1;
    int i;

    for (i = 0; i < count; i++){
        if (strncmp((char *)&tagList[i], (char *)value, 4) == 0){
            return result = i;
        }
    }
    return result;

}

/*
 * Sets antialiasing mode using GDI+ objects defined in graphics info. 
 */
JNIEXPORT void JNICALL
       Java_org_apache_harmony_awt_gl_font_NativeFont_setAntialiasing(JNIEnv *env, jclass obj, 
       jlong gfxInfo, jboolean isAntialiasing){
       
       GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    Graphics *graphics = (Graphics *)gi->graphics;
       if(isAntialiasing)
               graphics->SetTextRenderingHint(TextRenderingHintAntiAlias);
       else
               graphics->SetTextRenderingHint(TextRenderingHintSingleBitPerPixel);
}

/*
 * Draws string at the specified coordinates using GDI+ objects defined in graphics info.
 * This method is applicable for drawing without affine transformes.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_gdiPlusDrawText
    (JNIEnv *env, jclass obj, jlong gfxInfo, jstring jText, jint length,
    jlong font, jfloat xOffset, jfloat yOffset){

    int result = 0;
    GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    HFONT hFont  = (HFONT)font;
    HDC   giHDC = (HDC)gi->hdc;
    Graphics *graphics = (Graphics *)gi->graphics;
    Brush *brush = (Brush *)gi->brush;
    const TCHAR *text;
    jboolean iscopy;
    Font *gdipFont = new Font(giHDC, hFont);

    PointF *origin = new PointF(xOffset, yOffset);

    text = (const TCHAR *)env->GetStringCritical( jText, &iscopy);

    result = graphics->DrawString(text,
            length,
            gdipFont,
            *origin,
            brush);

    env->ReleaseStringCritical(jText, (const jchar *)text);

    delete origin;
    delete gdipFont;

    return result;

}

/*
 * Draws transformed char according to the matrix at the specified position.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_gdiPlusDrawDriverChar
    (JNIEnv *env, jclass obj, jlong gfxInfo, jchar chr, jlong font, jfloat x, jfloat y, jint flags, jdoubleArray jAT){
    int result;
    const UINT16 *text = &((UINT16)chr);
    HFONT hFont  = (HFONT)font;
    GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    HDC   giHDC = (HDC)gi->hdc;
    Graphics *graphics = (Graphics *)gi->graphics;
    Brush *brush = (Brush *)gi->brush;
    Font *gdipFont = new Font(giHDC, hFont);

    PointF *origin = new PointF(x, y);
    double *at = env->GetDoubleArrayElements(jAT, NULL);
    Matrix *matrix = new Matrix((REAL)at[0],(REAL)at[1],(REAL)at[2],(REAL)at[3],(REAL)at[4],(REAL)at[5]);
    env->ReleaseDoubleArrayElements(jAT, at, JNI_ABORT);

    // save original transform
    Matrix *matrixOld = new Matrix();
    graphics->GetTransform(matrixOld);

    graphics->SetTransform(gi->matrix);

    result = graphics->DrawDriverString(text,
            1,
            gdipFont,
            brush,
            origin,
            flags,
            matrix);
    graphics->SetTransform(matrixOld);
    delete matrixOld;

    delete origin;
    delete matrix;
    delete gdipFont;

    return result;
}

/*
 * Draws string transformed according to the matrix, each character is drawn at 
 * the specified positions.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_gdiPlusDrawDriverString
    (JNIEnv *env, jclass obj, jlong gfxInfo, jstring jText, jint length, jlong font, jfloat x, jfloat y, jdoubleArray jPosArray, jint flags, jdoubleArray jAT){
    int result;
    HFONT hFont  = (HFONT)font;
    GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    HDC   giHDC = (HDC)gi->hdc;
    Graphics *graphics = (Graphics *)gi->graphics;
    Brush *brush = (Brush *)gi->brush;
    Font *gdipFont = new Font(giHDC, hFont);

    PointF *origin = new PointF(x, y);
    double *at = env->GetDoubleArrayElements(jAT, NULL);
    Matrix *matrix = new Matrix((REAL)at[0],(REAL)at[1],(REAL)at[2],(REAL)at[3],(REAL)at[4],(REAL)at[5]);
    env->ReleaseDoubleArrayElements(jAT, at, JNI_ABORT);

    // save original transform
    Matrix *matrixOld = new Matrix();
    graphics->GetTransform(matrixOld);

    graphics->SetTransform(gi->matrix);

    double *posArray = env->GetDoubleArrayElements(jPosArray, NULL);
    PointF *positions = new PointF[length];
    for(int i=0; i< length; i ++){
        positions[i].X = (REAL)(posArray[2*i] + x);
        positions[i].Y = (REAL)(posArray[2*i+1] + y);
    }
    env->ReleaseDoubleArrayElements(jPosArray, posArray, JNI_ABORT);

    const UINT16 *text = env->GetStringCritical( jText, NULL);

    result = graphics->DrawDriverString(text,
            length,
            gdipFont,
            brush,
            positions,
            flags,
            matrix);

    env->ReleaseStringCritical(jText, text);
    graphics->SetTransform(matrixOld);
    delete matrixOld;

    delete gdipFont;
    delete origin;
    delete matrix;
    delete[] positions;
    return result;
}

/*
 * Draws string transformed according to the matrix, each character is drawn at 
 * the specified positions.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_gdiPlusDrawDriverChars
    (JNIEnv *env, jclass obj, jlong gfxInfo, jcharArray jText, jint length, jlong font, jdoubleArray jPosArray, jint flags, jdoubleArray jAT){
    int result;
    HFONT hFont  = (HFONT)font;
    GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    HDC   giHDC = (HDC)gi->hdc;
    Graphics *graphics = (Graphics *)gi->graphics;
    Brush *brush = (Brush *)gi->brush;
    Font *gdipFont = new Font(giHDC, hFont);

    double *at = env->GetDoubleArrayElements(jAT, NULL);
    Matrix *matrix = new Matrix((REAL)at[0],(REAL)at[1],(REAL)at[2],(REAL)at[3],(REAL)at[4],(REAL)at[5]);
    env->ReleaseDoubleArrayElements(jAT, at, JNI_ABORT);

    double *posArray = (double *)malloc(length*2 * sizeof(double));
    env->GetDoubleArrayRegion(jPosArray, 0, length*2, posArray);
    PointF *positions = new PointF[length];
    for(int i=0; i< length; i ++){
        positions[i].X = (REAL)(posArray[2*i]);
        positions[i].Y = (REAL)(posArray[2*i+1]);
    }
    free(posArray);

    jchar *text = (jchar *)malloc(length * sizeof(jchar));
    env->GetCharArrayRegion(jText, 0, length, text);

    graphics->SetTransform(gi->matrix);

    result = graphics->DrawDriverString((const UINT16 *)text,
            length,
            gdipFont,
            brush,
            positions,
            flags,
            matrix);

    free(text);
    graphics->ResetTransform();

    delete gdipFont;
    delete matrix;
    delete[] positions;
    return result;
}

/* Returns float array of x and y values from array of POINTFX elements. */
JNIEXPORT jfloatArray JNICALL Java_org_apache_harmony_awt_gl_font_WinGlyph_getPoints
    (JNIEnv *env, jclass obj, jlong points, jint size){

    jfloatArray flArray;
    float * fpPoints = (float *)malloc(sizeof(float) * size * 2);
    int i;
    POINTFX *pointfx = (POINTFX *)points;
    for(i = 0; i < size; i++){
        fpPoints[i*2] = pointfx[i].x.value + pointfx[i].x.fract /65536.0f + 0.5f;
        fpPoints[i*2 + 1] = -pointfx[i].y.value - pointfx[i].y.fract /65536.0f + 0.5f;
    }

    flArray=env->NewFloatArray(size*2);
    env->SetFloatArrayRegion(flArray, 0, size*2, fpPoints);
    free(fpPoints);
    return flArray;
}

/* Releases hdc object in GDI+ Graphics object from the GraphicsInfo. */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_font_NativeFont_gdiPlusReleaseHDC
    (JNIEnv *env, jclass obj, jlong gfxInfo, jlong hdc){

    GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    Graphics *graphics = (Graphics *)gi->graphics;
    graphics->ReleaseHDC((HDC)hdc);
}

/* Returns hdc object of the GDI+ Graphics object from the GraphicsInfo. */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_font_NativeFont_gdiPlusGetHDC
    (JNIEnv *env, jclass obj, jlong gfxInfo){

    GraphicsInfo *gi = (GraphicsInfo *)gfxInfo;
    Graphics *graphics = (Graphics *)gi->graphics;

    return (jlong)graphics->GetHDC();
}

/*
 * Returns an array of extra font metrics result[]:
 *      result[0] - average width of characters in the font
 *      result[1] - horizontal size for subscripts
 *      result[2] - vertical size for subscripts
 *      result[3] - horizontal offset for subscripts
 *      result[4] - vertical offset value for subscripts
 *      result[5] - horizontal size for superscripts
 *      result[6] - vertical size for superscripts
 *      result[7] - horizontal offset for superscripts
 *      result[8] - vertical offset for superscripts
 */
JNIEXPORT jfloatArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_NativeFont_getExtraMetricsNative(JNIEnv *env, jclass obj, jlong fnt, jint fontSize, jint fontType) {
    
    // XXX: Subscript/superscript metrics are undefined for Type1. As a possible 
    // solution for Type1 we can use coefficients obtained from the TrueType values
    // (e.g. for the type1 font size 12 SubscriptSizeX coefficient equals 
    //  to the truetype font size 12 SubscriptSizeX value / size of the font):
    //  SubscriptSizeX == 0.7 * fontSize
    //  SubscriptSizeY == 0.65 * fontSize
    //  SubscriptOffsetX == 0
    //  SubscriptOffsetY == 0.15 * fontSize
    //  SuperscriptSizeX == 0.7 * fontSize
    //  SuperscriptSizeY == 0.65 * fontSize
    //  SuperscriptOffsetX == 0
    //  SuperscriptOffsetY == 0.45 * fontSize

    HFONT hFont  = (HFONT)fnt;
    HGDIOBJ hOld;
    OUTLINETEXTMETRIC outm[3];
    jfloat result[9];
    jfloatArray floatArray;
    HDC hDC = CreateCompatibleDC(NULL);
    DWORD size;
    TEXTMETRIC tm;
    HGDIOBJ hFontEM;
    LOGFONT lf;
    int emsquare;
    float mltpl;
  
    hOld = SelectObject(hDC, hFont);

    // Getting current size of Font
    size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
    if (size == 0 ){
        throwNPException(env, "Error occured during getting text outline metrics in native code.");

        SelectObject(hDC, hOld);
        DeleteDC(hDC);
        return NULL;
    }
    
    if (fontType == FONT_TYPE_TT) {
        emsquare = outm[0].otmEMSquare; // EM Square size

        // Create font with height == "EM Square size" in device units to
        // get multyply factor
        GetObject(hFont, sizeof(lf), &lf);
        lf.lfHeight = -emsquare;
        lf.lfWidth = 0;
        hFontEM = CreateFontIndirect(&lf);

        SelectObject(hDC, hFontEM);

        size = GetOutlineTextMetrics(hDC, sizeof(outm), outm);
        if (size == 0 ){
            throwNPException(env, "Error occured during getting text outline metrics in native code.");
            SelectObject(hDC, hOld);
            DeleteObject(hFontEM);
            DeleteDC(hDC);
            return NULL;
        }

        tm = outm[0].otmTextMetrics;
        
        // Multiplier for the precise values
        mltpl = (float)fontSize/emsquare;
    
        result[0] = tm.tmAveCharWidth * mltpl; // the average width of characters in the font
    
        result[1] = outm[0].otmptSubscriptSize.x * mltpl; // horizontal size for subscripts 
        result[2] = outm[0].otmptSubscriptSize.y * mltpl; // vertical size for subscripts 
        result[3] = outm[0].otmptSubscriptOffset.x * mltpl; // horizontal offset for subscripts
        result[4] = outm[0].otmptSubscriptOffset.y * mltpl; // vertical offset value for subscripts
        result[5] = outm[0].otmptSuperscriptSize.x * mltpl; // horizontal size for superscripts
        result[6] = outm[0].otmptSuperscriptSize.y * mltpl; // vertical size for superscripts
        result[7] = outm[0].otmptSuperscriptOffset.x * mltpl; // horizontal offset for superscripts 
        result[8] = outm[0].otmptSuperscriptOffset.y * mltpl; // vertical offset for superscripts 
    } else {
        result[1] = 0.7f * fontSize;
        result[2] = 0.65f * fontSize; 
        result[3] = 0.0f;
        result[4] = 0.15f * fontSize;
        result[5] = 0.7f * fontSize;
        result[6] = 0.65f * fontSize;
        result[7] = 0.0f;
        result[8] = 0.45f * fontSize; 
    }
    
    SelectObject(hDC, hOld);
    DeleteDC(hDC);
    
    floatArray=env->NewFloatArray(9);
    env->SetFloatArrayRegion(floatArray, 0, 9, result);

    return floatArray;
       
}
