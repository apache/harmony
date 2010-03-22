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
#include <X11/Xft/Xft.h>
#include <freetype/tttables.h>
#include <freetype/t1tables.h>

#include <jni.h>

#include "FontLibExports.h"

#if (FREETYPE_MAJOR >= 2 && FREETYPE_MINOR >= 2)

#undef HAS_FREETYPE_INTERNAL
#include <freetype/ftsizes.h>
#include <freetype/ftglyph.h>
#ifndef FALSE
#define FALSE (0)
#define TRUE (1)
#endif /* FALSE */

#else /* FREETYPE_MAJOR ... */

#define HAS_FREETYPE_INTERNAL 1
#include <freetype/internal/tttypes.h>

#endif /* FREETYPE_MAJOR ... */

#include <freetype/ftsnames.h>

#include <stdio.h>
#include <stdlib.h>

#include<string.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>

#include <wchar.h>
#include <math.h>

#include "LinuxNativeFont.h"
#include "exceptions.h"
#include "org_apache_harmony_awt_gl_font_LinuxNativeFont.h"
/* debug code marker - to print debug information change to DEBUG */
#   define NO_DEBUG

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getFontFamiliesNames
 * Signature: ()[Ljava/lang/String;
 * Returns array of Strings that represents list of all font families names
 * available on the system.  
 */
JNIEXPORT jobjectArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getFontFamiliesNames(JNIEnv *env, jclass jobj){

    jobjectArray families;

    XftObjectSet *os = 0;
    XftPattern *xftPattern;
    XftFontSet *fs;
    int j, i;
    int len;
    int numFamilies;
    FcChar8 *family;
    char** famList;
    jclass strClass;
    jstring initStr;


    /* Create pattern */
    xftPattern = XftPatternCreate();

    if (!XftPatternAddBool (xftPattern, XFT_OUTLINE, True)){
        throwNPException(env, "Outline value can't be added to XftPattern");
    }

    /* Just need to add which fields you want to list */
    os = XftObjectSetBuild (XFT_FAMILY, (char *) 0);

    fs = FcFontList (0, xftPattern, os);

    XftObjectSetDestroy (os);

    if(xftPattern){
        XftPatternDestroy(xftPattern);
    }

    if (!fs){
        throwNPException(env, "Font list can't be created");
        return NULL;
    } else {
        numFamilies = fs->nfont;
        famList = (char** )malloc(numFamilies * sizeof(char *));

        for (j = 0; j < numFamilies; j++) {

#ifdef DEBUG
            FcChar8* font = FcNameUnparse (fs->fonts[j]);
            printf ("%s\n", font);
            free (font);
#endif /* DEBUG */

            if (XftPatternGetString (fs->fonts[j], XFT_FAMILY, 0, &family) == XftResultMatch){
        
#ifdef DEBUG
                printf ("       %s", family);
#endif /* DEBUG */

                len = (strlen((char *)family)+1);
                famList[j] = (char*)malloc(sizeof(char) * len);

                strncpy(famList[j], (char *)family, len);
            } else {
                /* 
                 * TODO
                 * We should add special handling for this case
                 */
                famList[j] = 0;
            }
        }
        XftFontSetDestroy (fs);
    }

    strClass = (*env)->FindClass(env, "java/lang/String");
    initStr = (*env)->NewStringUTF(env, "");

    families = (jobjectArray)(*env)->NewObjectArray(env, 
        numFamilies,  
        strClass,
        initStr);

    if (families == NULL){
        for (i = 0;i < numFamilies;i++){
            free(famList[i]);
        }
        free(famList);
        throwNPException(env, "Not enough memory to create families list");
        return NULL;
    }

    for (i = 0;i < numFamilies;i++){
        // number of chars == length of string -1
        (*env)->SetObjectArrayElement(env, families,i,(*env)->NewStringUTF(env, famList[i]));
        free(famList[i]);
    }
    free(famList);

    return families;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    embedFontNative
 * Signature: (Ljava/lang/String;)Z
 *
 * Returns true if the new font was added to the system, false otherwise.
 * Methods checks if the number of system fonts changed after font configuration
 * was rebuilt.
 */
JNIEXPORT jstring JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_embedFontNative(JNIEnv *env, jclass obj, jstring fName){

    int fontAdded = FALSE;
    FcConfig *config;

    config = FcConfigGetCurrent();
    if (!config){
        return 0;
    }

    char *path = (char *)(*env)->GetStringUTFChars(env, fName, 0);
    fontAdded = FcConfigAppFontAddFile(config, (unsigned char*)path);

    unsigned short *familyName = 0;

    if (fontAdded) {
        getFontFamilyName(path, &familyName);
    }    

    (*env)->ReleaseStringUTFChars(env, fName, path);    
    
    jstring res = 0;
    if (fontAdded && familyName) {
        int len = 0;
        for (; familyName[len] != 0; len++);
        res = (*env)->NewString(env, (jchar *)familyName, len);
        free(familyName);
    }

    return res;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    initializeFont
 * Signature: (Lorg/apache/harmony/awt/gl/font/LinuxFont;Ljava/lang/String;IILjava/lang/String;)J
 * Initiailzes native Xft font object from specified parameters and returns 
 * font handle, also sets font type to the font peer parameter. 
 * NullPointerException is thrown if there are errors in native code.
 */
JNIEXPORT jlong JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_initializeFont(JNIEnv *env, jclass jobj, jobject linuxFont, jstring jName, jint jStyle, jint size, jstring jFaceStyle){

    jclass cls;
    jfieldID fid;
    jmethodID mid;
    jboolean iscopy;

    int slant;
    int weight;
    const char* name;

    XftPattern *pattern;
    XftPattern *matchPattern;
    XftResult result;
    Display *dpy;
    int scr;
    FcChar8 *faceStyle = NULL;
    XftFont *xftFnt;
    int font_type = FONT_TYPE_UNDEF;
    double fSize;
    FT_Face face;
    
    /* Initialize part */
    cls = (*env)->GetObjectClass(env, linuxFont);

    fid=(*env)->GetFieldID(env, cls, "display", "J");
    if (fid == 0) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }
    dpy = (Display *)(long)(*env)->GetLongField(env, linuxFont, fid);

    if (dpy == NULL){
        throwNPException(env, "Cannot connect to XServer");
        return (jlong)(IDATA)NULL;
    }

    if (jStyle & FONT_BOLD) {
        // TODO: need to be defined from TextAttributes
        weight = XFT_WEIGHT_BOLD;
#ifdef DEBUG
        printf("Weight is bold");
#endif // DEBUG 
    } else {
        // TODO: need to be defined from TextAttributes
        weight = XFT_WEIGHT_MEDIUM;
#ifdef DEBUG
        printf("Weight is medium");
#endif // DEBUG 
    }

    if (jStyle & FONT_ITALIC){
        slant = XFT_SLANT_ITALIC;
    } else {
        slant = XFT_SLANT_ROMAN;
    }

    fid=(*env)->GetFieldID(env, cls, "screen", "I");
    if (fid == 0) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }
    scr = (*env)->GetIntField(env, linuxFont, fid);

    name = (*env)->GetStringUTFChars(env, jName, &iscopy);
    if (jFaceStyle){
        faceStyle = (FcChar8 *)(*env)->GetStringUTFChars(env, jFaceStyle, &iscopy);
    }
        
    /* Xft part */
    /* Create pattern */
    pattern = XftPatternCreate();

    if (!XftPatternAddString (pattern, XFT_FAMILY, name)){
        if(name){
            (*env)->ReleaseStringUTFChars(env, jName, name);
        }
        if(faceStyle){
            (*env)->ReleaseStringUTFChars(env, jFaceStyle, (char *)faceStyle);
        }
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding family name to XFTPattern structure");
    }

    /* We do not need name any more */
    if(name){
        (*env)->ReleaseStringUTFChars(env, jName, name);
    }

    if (faceStyle && !XftPatternAddString (pattern, XFT_STYLE, faceStyle)){
        if(faceStyle){
            (*env)->ReleaseStringUTFChars(env, jFaceStyle, (char *)faceStyle);
        }
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding style name to XFTPattern structure");
    }

    /* We do not need faceStyle any more */
    if(faceStyle){
        XftPatternDestroy (pattern);
        (*env)->ReleaseStringUTFChars(env, jFaceStyle, (char *)faceStyle);
    }

    if (!XftPatternAddInteger(pattern, XFT_SLANT, slant)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font slant to XFTPattern structure");
    }

    if (!XftPatternAddInteger(pattern, XFT_WEIGHT, weight)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font weight to XFTPattern structure");
    }

    /* We set antialias mode for simple text rendering without antialiasing */
    if (!XftPatternAddBool(pattern, XFT_ANTIALIAS, False)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font antialias set to false to XFTPattern structure");
    }

    /*
     *
     *  To comply with Java specification and results we have  to use DPI value
     *  equals to 96. Actually, it is properly to use resolution Y value instead of DPI value
     *  the correct formula  resolutionY = (XDisplayHeight(dpy, scr)/(XDisplayHeightMM(dpy, scr)/25.4));
     *  hence size = size / (double)resolutionY * 72;
     */

    fSize = (double)size / 96 * 72;

    if (!XftPatternAddDouble (pattern, XFT_SIZE, fSize)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font size to XFTPattern structure");
    }

/*  if (!XftPatternAddBool (pattern, FC_HINTING, True)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font hinting set to false to XFTPattern structure");
    }
*/

    if (!XftPatternAddBool (pattern, XFT_RENDER, True)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font RENDER set to true to XFTPattern structure");
    }


    if (!XftPatternAddBool (pattern, FC_AUTOHINT, True)){
        XftPatternDestroy (pattern);
        throwNPException(env, "Error during adding font autohinting set to false to XFTPattern structure");
    }
        
    matchPattern = XftFontMatch (dpy, scr, pattern, &result);

#ifdef DEBUG
    printf ("Pattern ");
    FcPatternPrint (pattern);
    if (matchPattern)
    {
        printf ("Match ");
        FcPatternPrint (matchPattern);
    }
    else {
        printf ("No Match\n");
        }
#endif /* DEBUG */

    XftPatternDestroy (pattern);

    if (!matchPattern){
        XftPatternDestroy (matchPattern);
        return (jlong)(IDATA)NULL;
    }
    
    xftFnt = XftFontOpenPattern (dpy, matchPattern);

    if (!xftFnt){
        XftPatternDestroy (matchPattern);
        return (jlong)(IDATA)NULL;
    }

    /* defining font type */
    face = XftLockFace(xftFnt);
    if ((face->face_flags & FT_FACE_FLAG_SCALABLE) &&
            !(face->face_flags & FT_FACE_FLAG_FIXED_SIZES) &&
            !(face->face_flags & FT_FACE_FLAG_SFNT)) {
            font_type = FONT_TYPE_T1;
    } else {
        font_type = FONT_TYPE_TT;
    }
    XftUnlockFace(xftFnt);

    if (font_type == FONT_TYPE_UNDEF){
        XftFontClose (dpy, xftFnt);
        return (jlong)(IDATA)NULL;
    }

    /* Set Font type in LinuxFont object (upcall) */
    mid=(*env)->GetMethodID(env, cls, "setFontType", "(I)V");

    if (mid == 0) {
#ifdef DEBUG
        printf("Can't find \"setFontType\" method");
#endif // DEBUG 
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }

    (*env)->CallVoidMethod(env, linuxFont, mid, font_type);

    if((*env)->ExceptionOccurred(env)) {
#ifdef DEBUG
        printf("Error occured when calling \"setFontType\" method");
#endif // DEBUG 
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }

    return (jlong)(IDATA)xftFnt;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    initializeFontFromFP
 * Signature: (Lorg/apache/harmony/awt/gl/font/LinuxFont;Ljava/lang/String;)J
 * Initializes native Xft font object from xlfd string and returns font handle,  
 * also sets font type to the font peer parameter. If font 
 * that is described by the given xlfd doesn't exist onto a system returned value
 * is null. NullPointerException is thrown if there are errors in native code.
 */
JNIEXPORT jlong JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_initializeFontFromFP(JNIEnv *env, jclass jobj, jobject linuxFont, jstring jXLFD, jint size){

    jclass cls;
    jfieldID fid;
    jmethodID mid;
    jboolean iscopy;

    const char* xlfd;
    Display *dpy;
    int scr;
    XftFont *xftFnt;
    int font_type = FONT_TYPE_UNDEF;
    FT_Face face;

    char **fn; 
    int n = 0;
    int buf_size;
    char *buffer;
    
    XftPattern *pattern;
    XftPattern *matchPattern;
    XftResult result;

    // Initialize part
    cls = (*env)->GetObjectClass(env, linuxFont);

    /* get display value */
    fid=(*env)->GetFieldID(env, cls, "display", "J");
    if (fid == 0) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }
    dpy = (Display *)(long)(*env)->GetLongField(env, linuxFont, fid);

    if (dpy == NULL){
        throwNPException(env, "Cannot connect to XServer");
        return (jlong)(IDATA)NULL;
    }
    
    xlfd = (*env)->GetStringUTFChars(env, jXLFD, &iscopy);
    buf_size = (*env)->GetStringLength(env, jXLFD) + 8;
    buffer = (char *)malloc(buf_size);

    snprintf(buffer, buf_size, xlfd, (int)((double)(size*10) / 96 * 72));
    (*env)->ReleaseStringUTFChars(env, jXLFD, xlfd);

    /* get screen value */
    fid=(*env)->GetFieldID(env, cls, "screen", "I");
    if (fid == 0) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }
    scr = (*env)->GetIntField(env, linuxFont, fid);

    /* check if there any fonts with specified xlfd structure */
    fn = XListFonts( dpy, buffer, 10, &n );
    if (fn != NULL){
        XFreeFontNames(fn);
    }

    if (n == 0){
        free(buffer);
        return (jlong)(IDATA)NULL;
    }

    /* Xft part */
    /* Create xlfd pattern */
    pattern = XftXlfdParse(buffer, False, True);
    
    /* We set antialias mode for simple text rendering without antialiasing */
    if (!XftPatternAddBool(pattern, XFT_ANTIALIAS, False)){
        throwNPException(env, "Error during adding font antialias set to false to XFTPattern structure");
    }

    if (!XftPatternAddBool (pattern, XFT_RENDER, True)){
        throwNPException(env, "Error during adding font RENDER set to true to XFTPattern structure");
    }


    if (!XftPatternAddBool (pattern, FC_AUTOHINT, True)){
        throwNPException(env, "Error during adding font autohinting set to false to XFTPattern structure");
    }

    
    matchPattern = XftFontMatch (dpy, scr, pattern, &result);

#ifdef DEBUG
    printf ("Pattern ");
    FcPatternPrint (pattern);
    if (matchPattern)
    {
        printf ("Match ");
        FcPatternPrint (matchPattern);
    }
    else {
        printf ("No Match\n");
        }
#endif // DEBUG 

    XftPatternDestroy (pattern);

    if (!matchPattern){
        XftPatternDestroy (matchPattern);
        return (long)NULL;
    }
    
    xftFnt = XftFontOpenPattern (dpy, matchPattern);

    if (!xftFnt){
        XftPatternDestroy (matchPattern);
        return (long)NULL;
    }

    free(buffer);

    if (!xftFnt){
        return (long)NULL;
    }

    face = XftLockFace(xftFnt);

    if ((face->face_flags & FT_FACE_FLAG_SCALABLE) &&
            !(face->face_flags & FT_FACE_FLAG_FIXED_SIZES)){
        if (face->face_flags & FT_FACE_FLAG_SFNT){
            font_type = FONT_TYPE_TT;
        } else {
            font_type = FONT_TYPE_T1;
        }
    }
    XftUnlockFace(xftFnt);
    

    if (font_type == FONT_TYPE_UNDEF){
        XftFontClose (dpy, xftFnt);
        return (jlong)(IDATA)NULL;
    }

    /* Set Font type in LinuxFont object (upcall) */
    mid=(*env)->GetMethodID(env, cls, "setFontType", "(I)V");

    if (mid == 0) {
#ifdef DEBUG
        printf("Can't find \"setFontType\" method");
#endif // DEBUG 
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }
 
    (*env)->CallVoidMethod(env, linuxFont, mid, font_type);

    if((*env)->ExceptionOccurred(env)) {
#ifdef DEBUG
        printf("Error occured when calling \"setFontType\" method");
#endif // DEBUG 
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return (jlong)(IDATA)NULL;
    }

    return (long)xftFnt;

}


/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getNumGlyphsNative
 * Signature: (J)I
 * Returns number of glyphs in specified XftFont if success.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getNumGlyphsNative(JNIEnv *env, jclass obj, jlong fnt){

    jint numGlyphs = 0;
    XftFont *xftFnt = (XftFont *)(long)fnt;
    FT_Face face;

    if(!xftFnt){
        return 0;
    }
    face = XftLockFace(xftFnt);

    if(!face)
        return 0;
    numGlyphs = face->num_glyphs;

#ifdef DEBUG
    printf("Num glyphs = %d\n", numGlyphs);
#endif /* DEBUG */

    XftUnlockFace(xftFnt);
    

    return numGlyphs;

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    canDisplayCharNative
 * Signature: (JC)Z
 * Returns true, if XftFont object can display specified char.
 */
JNIEXPORT jboolean JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_canDisplayCharNative(JNIEnv *env, jclass obj, jlong fnt, jchar c){

    jboolean canDisplay = 0;

    /* TODO: implement method - or we can use getGlyphCode results to find out,
     * whether we can display char or not.
     */

    return canDisplay;

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getFamilyNative
 * Signature: (J)Ljava/lang/String;
 * Returns family name of the XftFont object.
 */
JNIEXPORT jstring JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getFamilyNative(JNIEnv *env, jclass obj, jlong fnt){

    XftFont *xftFnt = (XftFont *)(long)fnt;

    jstring familyName = NULL;
    char* family;

    if (XftPatternGetString (xftFnt->pattern, XFT_FAMILY, 0, &family) != XftResultMatch){
        throwNPException(env, "Can not get font family value");
    }

    familyName = (*env)->NewStringUTF(env, family);

    return familyName;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getFontNameNative
 * Signature: (J)Ljava/lang/String;
 * Returns face name of the XftFont object.
 */
JNIEXPORT jstring JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getFontNameNative(JNIEnv *env, jclass obj, jlong fnt){

    // !! at the moment only EN locale font names used
    jstring faceName = NULL;
    XftFont *xftFnt = (XftFont *)(long)fnt;
    FT_Face face;
    const int BUF_SIZE = 64;
    char name[BUF_SIZE];

    if(!xftFnt){
        return 0;
    }
    face = XftLockFace(xftFnt);

    if(!face)
        return 0;
    
    snprintf(name, BUF_SIZE, "%s %s", face->family_name, face->style_name);

#ifdef DEBUG
        printf("Face name = %s\n", name);
#endif // DEBUG 

    XftUnlockFace(xftFnt);
  

    faceName = (*env)->NewStringUTF(env, name); 

    return faceName;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getFontPSNameNative
 * Signature: (J)Ljava/lang/String;
 * Returns XftFont's postscript name.
 * Returned value is the name of the font in system default locale or
 * for english langid if there is no results for default locale settings.
 */
JNIEXPORT jstring JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getFontPSNameNative(JNIEnv *env, jclass obj, jlong fnt){

    XftFont *xftFnt = (XftFont *)(long)fnt;
    FT_Face face;
    const char* name;
    jstring psName;

    if(!xftFnt){
        return 0;
    }
    face = XftLockFace(xftFnt);

    if(!face)
        return 0;
    
    name = FT_Get_Postscript_Name(face);
#ifdef DEBUG
        printf("PostScript name = %s\n", name);
#endif /* DEBUG */


    psName = (*env)->NewStringUTF(env, name);
    
    XftUnlockFace(xftFnt);
    

    return psName;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    pFontFree
 * Signature: (JJ)V
 * Disposes XftFont object.
 */
JNIEXPORT void JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_pFontFree(JNIEnv *env, jclass obj, jlong fnt, jlong display){

    Display *dpy = (Display *)(long)display;
    XftFont *xftFnt = (XftFont *)(long)fnt;
    if(xftFnt){
        XftFontClose (dpy, xftFnt);
    }
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getItalicAngleNative
 * Signature: (JI)F
 * Returns tangent of Italic angle of given Font.
 * Returned value is null and NullPointerException is thrown if there is Xft error.
 */
JNIEXPORT jfloat JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getItalicAngleNative(JNIEnv *env, jclass obj, jlong fnt, int fontType){

    jfloat italicAngle = 0.0;
    XftFont *xftFnt = (XftFont *)(long)fnt;
    FT_Face face;
#if HAS_FREETYPE_INTERNAL
    TT_Face tt_face;
#endif
    TT_HoriHeader* hh;
    float rise;
    float run;
    PS_FontInfoRec afont_info;
    FT_Error err;
    double pi = 3.1415926535;

    if(!xftFnt){
        return 0;
    }
    face = XftLockFace(xftFnt);

    if(!face){
        return 0;
    }
    if (fontType == FONT_TYPE_TT){
#if HAS_FREETYPE_INTERNAL
            tt_face = (TT_Face)face;
            hh = &(tt_face->horizontal);
#else
            hh = (TT_HoriHeader*)FT_Get_Sfnt_Table(face, ft_sfnt_hhea);
#endif
            rise = (float)hh->caret_Slope_Rise;
            run = (float)hh->caret_Slope_Run;
            italicAngle = run / rise ;
    } else {
        err =  FT_Get_PS_Font_Info( face, &afont_info);
        if (err){
            XftUnlockFace(xftFnt);
            
            return italicAngle;
        }
        italicAngle = tan(((double)afont_info.italic_angle * pi) / 180);
    }

#ifdef DEBUG
    printf("Italic angle value = %f\n", italicAngle);
#endif /* DEBUG */

    XftUnlockFace(xftFnt);
    

    return italicAngle;

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getFonts
 * Signature: ()[Ljava/lang/String;
 * Returns an array of available system fonts names.
 * In case of errors NullPointerException is thrown.
 */
JNIEXPORT jobjectArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getFonts(JNIEnv *env, jclass obj){

    jobjectArray fonts;

    XftObjectSet *os = 0;
    XftPattern *xftPattern;
    int len;
        XftFontSet *fs;
    int j, i;
    int numFonts;
    const int BUF_SIZE = 128;
    FcChar8 font[BUF_SIZE];
    FcChar8 *family;
    FcChar8 *style;
    char** fontList;
    char* fstr="%s-%s-%d"; // "family name"-"styleName"-"style"
    int fontStyle;
    int weight;
    int slant;
    jclass strClass;
    jstring initStr;

    /* Create pattern */
    xftPattern = XftPatternCreate();

    if (!XftPatternAddBool (xftPattern, XFT_OUTLINE, True)){
        throwNPException(env, "Outline value can't be added to XftPattern");
    }

    /* Just need to add which fields you want to list */
    os = XftObjectSetBuild (XFT_FAMILY, XFT_STYLE, XFT_SLANT, XFT_WEIGHT, (char *) 0);

    fs = FcFontList (0, xftPattern, os);

    XftObjectSetDestroy (os);

    if(xftPattern){
        XftPatternDestroy(xftPattern);
    }

    if (!fs){
        throwNPException(env, "Font list can't be created");
        return NULL;
    } else  {
        numFonts = fs->nfont;
        fontList = (char** )malloc(numFonts * sizeof(char *));

        for (j = 0; j < numFonts; j++){
            if (XftPatternGetString (fs->fonts[j], XFT_FAMILY, 0, &family) != XftResultMatch){
                throwNPException(env, "Couldn't get font family name");
            }

#ifdef DEBUG
            {
             FcChar8* font = FcNameUnparse (fs->fonts[j]);
             printf ("%s\n", font);
             free(font);
            }
#endif /* DEBUG */

            if (XftPatternGetString (fs->fonts[j], XFT_STYLE, 0, &style) != XftResultMatch) {
                throwNPException(env, "Couldn't get font style name");
            }
            
            if (XftPatternGetInteger (fs->fonts[j], XFT_SLANT, 0, &slant) != XftResultMatch) {
                throwNPException(env, "Couldn't get font slant");
            }

            if (XftPatternGetInteger (fs->fonts[j], XFT_WEIGHT, 0, &weight) != XftResultMatch) {
                throwNPException(env, "Couldn't get font weight");
            }

            if (weight <= XFT_WEIGHT_MEDIUM) {
                fontStyle = FONT_PLAIN;
            } else {
                fontStyle = FONT_BOLD;
            }

            if (slant != XFT_SLANT_ROMAN){
                fontStyle |= FONT_ITALIC;
            }

            len = snprintf((char *)font, BUF_SIZE, fstr, family, style, fontStyle);

            if (len < 0){
                len = BUF_SIZE;
            }

            fontList[j] = (char*)malloc(sizeof(char) * (len+1));
            strncpy(fontList[j], (char *)font, len);
            fontList[j][len] = 0;
        }
        XftFontSetDestroy (fs);
    }


    strClass = (*env)->FindClass(env, "java/lang/String");
    initStr = (*env)->NewStringUTF(env, "");

    fonts = (jobjectArray)(*env)->NewObjectArray(env,
        numFonts,
        strClass,
        initStr);

    if (fonts == NULL){
        for (i = 0;i < numFonts;i++){
            free(fontList[i]);
        }
        free(fontList);

        throwNPException(env, "Not enough memory to create families list");
        return (jobjectArray)NULL;
    }

    for (i = 0;i < numFonts;i++){
        (*env)->SetObjectArrayElement(env, fonts,i,(*env)->NewStringUTF(env, fontList[i])); // number of chars == length of string -1
        free(fontList[i]);
    }
    free(fontList);

    return fonts;

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getNativeLineMetrics
 * Signature: (JIZZI)[F
 * Returns array of values of font metrics corresponding to the given XftFont 
 * font object. NullPointerException is thrown in case of errors.
 */
JNIEXPORT jfloatArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getNativeLineMetrics(JNIEnv *env, jclass obj, jlong fnt, jint fontHeight, jboolean isAntialiased, jboolean usesFractionalMetrics, jint fontType){
    jfloatArray metrics;
    jfloat values[17];

    XftFont *xftFnt = (XftFont *)(long)fnt;
    FT_Face face;
    TT_OS2* os2;
#if HAS_FREETYPE_INTERNAL
    TT_Face tt_face;
#endif
    int units_per_EM;
    FT_Size_Metrics size_metrics;
    FT_Size size;
    float mltpl;
    
    if (!xftFnt){
        return NULL;
    }
#ifdef DEBUG
    printf("XFT Ascent = %d\n", xftFnt->ascent);
    printf("XFT Descent = %d\n", xftFnt->descent);
    printf("XFT Height = %d\n", xftFnt->height);
#endif /* DEBUG */

    face = XftLockFace(xftFnt);

    if(!face){
        return NULL;
    }
    units_per_EM = face->units_per_EM;
    if (units_per_EM == 0){
//          throwNPException(env, "Units per EM value is equals to zero");
        XftUnlockFace(xftFnt);
        
        
        return NULL;
    }
    values[16] = units_per_EM;
    mltpl = (float)fontHeight / units_per_EM;
    values[0] = (float)(face->ascender) * mltpl;    // Ascent value
    values[1] = (float)(face->descender) * mltpl;   // -Descent value
    values[2] = (float)(face->height) * mltpl  - values[0] + values[1]; // External Leading value
    values[3] = (float)(face->underline_thickness) * mltpl;     // Underline size value
    values[4] = (float)(face->underline_position) * mltpl;  // Underline position value

    if (fontType == FONT_TYPE_TT){
#if HAS_FREETYPE_INTERNAL
        tt_face = (TT_Face)face;
        os2 = &(tt_face->os2);
#else
        os2 = (TT_OS2*)FT_Get_Sfnt_Table(face, ft_sfnt_os2);
#endif
        values[5] = (float)(os2->yStrikeoutSize) * mltpl;    // Strikeout size value
        values[6] = (float)(os2->yStrikeoutPosition) * mltpl;    // Strikeout position value
    } else {
        values[5] = values[3];
        // !!Workaround: for Type1 fonts strikethrough position = (-ascent+descent)/2
        values[6] = (-values[0] - values[1])/2;
    }

    values[7] = (float)(face->bbox.xMax - face->bbox.xMin)* mltpl;  // Max char width

    size = face->size;
    size_metrics = size->metrics;
    values[8] = (int)size_metrics.ascender >> 6;    // Ascent value
    values[9] = (int)size_metrics.descender >> 6;   // Descent value
    values[10] = (int)(size_metrics.height >> 6)  - values[8] + values[9]; // External Leading value
    values[11] = (int)values[3];    // Underline size value
    values[12] = (int)values[4];    // Underline position value

    values[13] = (int)values[5];    // Strikeout size value
    values[14] = (int)values[6];    // Strikeout sposition value

    values[15] = (int)values[7];    // Max char width

#ifdef DEBUG
    printf("Ascent = %f\n", values[0]);
    printf("Descent = %f\n", values[1]);
    printf("External Leading = %f\n", values[2]);
    printf("Underline size = %f\n", values[3]);
    printf("Underline position = %f\n", values[4]);
    printf("Strikeout size = %f\n", values[5]);
    printf("Strikeout position = %f\n", values[6]);
    printf("Max char width = %f\n", values[7]);

    printf("Pixel Ascent = %f\n", values[8]);
    printf("Pixel Descent = %f\n", values[9]);
    printf("Pixel External Leading = %f\n", values[10]);
    printf("Pixel Underline size = %f\n", values[11]);
    printf("Pixel Underline position = %f\n", values[12]);
    printf("Pixel Strikeout size = %f\n", values[13]);
    printf("Pixel Strikeout position = %f\n", values[14]);
    printf("Pixel Max char width = %f\n", values[15]);

#endif /* DEBUG */

    XftUnlockFace(xftFnt);
    

    metrics = (*env)->NewFloatArray(env, 17);
    (*env)->SetFloatArrayRegion(env, metrics, 0, 17, values);

    return metrics;

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getGlyphInfoNative
 * Signature: (JCI)[F
 * Returns array of glyph metrics values for the specified character
 * null is returned and NullPointerException is thrown in case of FreeType 
 * errors.
 */
JNIEXPORT jfloatArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getGlyphInfoNative(JNIEnv *env, jclass obj, jlong fnt, jchar chr, jint fontHeight){

    jfloatArray results = 0;
    jfloat values[11];

    XftFont *font = (XftFont *)(long)fnt;
    const float CONST_96_DIV_76 = (float)96 / 72;
    float mltpl;
    FT_Face face;
    FT_GlyphSlot glyphslot;
    FT_Glyph_Metrics  metrics;

    FT_Pos  width;         /* glyph width  */
    FT_Pos  height;        /* glyph height */

    FT_Pos  horiBearingX;  /* left side bearing in horizontal layouts */
    FT_Pos  horiBearingY;  /* top side bearing in horizontal layouts  */
    FT_Pos  horiAdvance;   /* advance width for horizontal layout     */

    FT_Pos  vertBearingX;  /* left side bearing in vertical layouts */
    FT_Pos  vertBearingY;  /* top side bearing in vertical layouts  */
    FT_Pos  vertAdvance;   /* advance height for vertical layout    */
    int units_per_EM;
    FT_Error error;

    if (!font){
        return results;
    }
    face = XftLockFace(font);

    if(!face){
        return results;
    }
    units_per_EM = face->units_per_EM;

    error = FT_Load_Char(face, (FT_ULong)chr, FT_LOAD_RENDER | FT_LOAD_TARGET_MONO);

    if(error){
//        throwNPException(env, "FT_Load_char : FreeType error");
        XftUnlockFace(font);
    
        return NULL;
    }
    
    glyphslot = face->glyph;
    metrics = glyphslot->metrics;          

    width      = metrics.width;         /* glyph width  */
    height     = metrics.height;        /* glyph height */

    horiBearingX = metrics.horiBearingX;  /* left side bearing in horizontal layouts */
    horiBearingY = metrics.horiBearingY;  /* top side bearing in horizontal layouts  */
    horiAdvance  = metrics.horiAdvance;   /* advance width for horizontal layout     */

    vertBearingX = metrics.vertBearingX;  /* left side bearing in vertical layouts */
    vertBearingY = metrics.vertBearingY;  /* top side bearing in vertical layouts  */
    vertAdvance  = metrics.vertAdvance;   /* advance height for vertical layout    */

    /* Multyplier to obtain proper values in pixels of the metrics */
    mltpl = CONST_96_DIV_76 / units_per_EM;

#ifdef DEBUG

    printf("\n   glyph metrics char = %d: : \n", chr);
    printf("width = %d : height = %d \n", (int)width >> 6, (int)height >> 6);
    printf("ghoriBearingX = %d : ghoriBearingY = %d \n", horiBearingX >> 6, horiBearingY>> 6);
    printf("gvertBearingX = %d : gvertBearingY = %d \n", vertBearingX>> 6, vertBearingY>> 6);
    printf("ghoriAdvance = %d : gvertAdvance = %d \n", horiAdvance>> 6, vertAdvance>> 6);
#endif /* DEBUG*/

    XftUnlockFace(font);
    
    values[0] = (horiBearingX >> 6) + (horiBearingX & 0x3F)/64; // Glyph Precise Bounds : X
    values[1] = (horiBearingY  >> 6) + (horiBearingY & 0x3F)/64;// Glyph Precise Bounds : Y
    values[2] = (horiAdvance >> 6) + (horiAdvance  & 0x3F)/64; // Precise AdvanceX
    values[3] = 0;//(vertAdvance >> 6) + (vertAdvance  & 0x3F)/64; // Precise AdvanceY ?= Ascent+Descent
    values[4] = (width >> 6) + (width  & 0x3F)/64; // Glyph Precise Bounds : width
    values[5] = (height >> 6) + (height  & 0x3F)/64; // Glyph Precise Bounds : height
    
    results = (*env)->NewFloatArray(env, 6);
    (*env)->SetFloatArrayRegion(env, results, 0, 6, values);

    return results;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getGlyphPxlInfoNative
 * Signature: (JJC)[I
 * Returns array of glyph metrics values in pixels for the specified character
 * null is returned and NullPointerException is thrown in case of FreeType errors.
 */
JNIEXPORT jintArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getGlyphPxlInfoNative(JNIEnv *env, jclass obj, jlong display, jlong fnt, jchar chr){

    Display * dpy = (Display *)(long)display;
    jintArray metricsArr = NULL;
    jint values[6];
    XftFont *font = (XftFont *)(long)fnt;
    XGlyphInfo extents;
    FT_Face face;
    FT_BBox  acbox;
    FT_Glyph        glyph;                                         
    FT_Error error;
    
    if (!font){
        return metricsArr;
    }
    face = XftLockFace(font);

    if (!face){
        return metricsArr;
    }
    error = FT_Load_Char(face, (FT_ULong)chr, FT_LOAD_RENDER | FT_LOAD_TARGET_MONO);

    if(error){
        XftUnlockFace(font);
    
        return metricsArr;
    }

    error = FT_Get_Glyph( face->glyph, &glyph );

    if(error){
        throwNPException(env, "getGlyphPxlInfoNative 1 : FreeType error");
        XftUnlockFace(font);
        
        return metricsArr;
    }

    FT_Glyph_Get_CBox(glyph,
                      2,   //FT_GLYPH_BBOX_PIXELS
                     &acbox);
    FT_Done_Glyph(glyph);
    XftUnlockFace(font);
    

    XftTextExtents16 (dpy,
              font,
              &chr,
              1,
              &extents);
#ifdef DEBUG

    printf("char = %d; y = %d; x = %d; height = %d; width = %d; advX = %d; advY = %d \n",
            chr, extents.y, extents.x, extents.height, extents.width, extents.xOff, extents.yOff);
#endif // DEBUG 

    values[0] = - extents.x ; // Glyph Pixels Bounds : X
    values[1] = extents.y ; // Glyph Pixels Bounds : Y
    values[2] = extents.xOff; // Pixels AdvanceX
    values[3] = extents.yOff; // Pixels AdvanceY ?= Ascent+Descent
    values[4] = acbox.xMax-acbox.xMin;  // Glyph Pixels Bounds : width
    values[5] = acbox.yMax-acbox.yMin; // Glyph Pixels Bounds : height
    
    metricsArr = (*env)->NewIntArray(env, 6);
    (*env)->SetIntArrayRegion(env, metricsArr, 0, 6, values);

    return metricsArr;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getGlyphCodesNative
 * Signature: (JLjava/lang/String;I)[I
 * Returns glyphs code corresponding to the characters in String specified, null 
 * is returned if failure. NullPointerException is thrown in case of Display 
 * is null.
 */
JNIEXPORT jintArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getGlyphCodesNative(JNIEnv *env, jclass obj, jlong fnt, jstring str, jint len){

    jintArray glyphsCodes = NULL;

    // TODO: implement method

    return glyphsCodes;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getGlyphCodeNative
 * Signature: (JCJ)I
 * Returns glyph code corresponding to the specified character, null is 
 * returned if failure. NullPointerException is thrown in case of Display is null.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getGlyphCodeNative(JNIEnv *env, jclass obj, jlong fnt, jchar chr, jlong display){

    jint code = 0xFFFF;
    Display *dpy = (Display *)(long)display;
    XftFont *font = (XftFont *)(long)fnt;

    if (!font){
        return 0xFFFF;
    }
    
    code = XftCharIndex (dpy, font, chr);

    return (code == 0) ? 0xFFFF : code;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    RemoveFontResource
 * Signature: (Ljava/lang/String;)I
 * Description: Re-cache ~/.fonts directory, after temporary font-file is deleted.
 */
JNIEXPORT jint JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_RemoveFontResource(JNIEnv *env, jclass obj, jstring fName){

    FcChar8* dirName;
    jboolean iscopy;
    FcConfig *config;
    FcFontSet *set;
    FcStrSet *subdirs;
    FcStrList *list;
    jboolean result = TRUE;

    /* get current congif */
    config = FcConfigGetCurrent();

    if (!config){
        return FALSE;
    }

    dirName = (FcChar8*)((*env)->GetStringUTFChars(env, fName, &iscopy));

    list = FcConfigGetConfigDirs (config);
    set = FcFontSetCreate ();
    subdirs = FcStrSetCreate ();

    /* scan dir for changes */
    result = result && FcDirScan (set, subdirs, 0, FcConfigGetBlanks (config), dirName, FcFalse);

    /* save changes to the config */
    result = result && FcDirSave (set, subdirs, dirName);

    /* rebuild fonts list */
    FcConfigBuildFonts(config);

    FcFontSetDestroy (set);
    FcStrSetDestroy (subdirs);
    FcStrListDone (list);

    (*env)->ReleaseStringUTFChars(env, fName, (char *)dirName);

    return result;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    NativeInitGlyphBitmap
 * Signature: (JC)J
 * Returns pointer to FreeType FT_Bitmap that represents bitmap
 * of the character specified or 0 if failures in native code.
 */
JNIEXPORT jlong JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_NativeInitGlyphBitmap(JNIEnv *env, jclass jobj,  jlong fnt, jchar chr){

    XftFont *font = (XftFont *)(long)fnt;
    FT_Glyph glyph;
    FT_BitmapGlyph  glyph_bitmap;                                  
    int size;

    FT_Error error;
    FT_Face face;
    FT_Bitmap ft_bitmap;
                                             
    if (!font){
        return 0;
    }
    face = XftLockFace(font);

    if (!face){
        return 0;
    }
    
    error = FT_Load_Char(face, (FT_ULong)chr, FT_LOAD_RENDER | FT_LOAD_TARGET_MONO);
    
    if(error){        
        throwNPException(env, "NativeInitGlyphBitmap : FreeType error");

        XftUnlockFace(font);
    
        return (jlong)(IDATA)NULL;
    }

    error = FT_Get_Glyph( face->glyph, &glyph );

    if(error){
//        throwNPException(env, "NativeInitGlyphBitmap 1 : FreeType error");
        XftUnlockFace(font);
    
        return (jlong)(IDATA)NULL;
    }        
    // convert to a bitmap (default render mode + destroy old)     
    if ( glyph->format != FT_GLYPH_FORMAT_BITMAP ) {                                                              
        error = FT_Glyph_To_Bitmap( &glyph, (FT_LOAD_RENDER | FT_LOAD_TARGET_MONO),  
                                      0, 1 );                          
        if ( error ){
            // glyph unchanged                              
            FT_Done_Glyph( glyph );
            XftUnlockFace(font);
            return (jlong)(IDATA)NULL;
        }
    }                                                              
    
    glyph_bitmap = (FT_BitmapGlyph)(glyph);                          
    ft_bitmap = glyph_bitmap->bitmap;
        
    
    GlyphBitmap *gbmp = (GlyphBitmap *)malloc(sizeof(GlyphBitmap));
    gbmp->left = glyph_bitmap->left;
    gbmp->top = glyph_bitmap->top;
    gbmp->bitmap.rows = ft_bitmap.rows;
    gbmp->bitmap.width = ft_bitmap.width;
    gbmp->bitmap.pitch = ft_bitmap.pitch;
    gbmp->bitmap.buffer = malloc(ft_bitmap.rows * ft_bitmap.pitch);
    size= ft_bitmap.rows * ft_bitmap.pitch;
    memcpy(gbmp->bitmap.buffer, ft_bitmap.buffer, size);

    FT_Done_Glyph(glyph);
    XftUnlockFace(font);
    return (long)gbmp;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    NativeFreeGlyphBitmap
 * Signature: (J)V
 * Disposes GlyphBitmap memory block. 
 */
JNIEXPORT void JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_NativeFreeGlyphBitmap(JNIEnv *env, jclass jobj,  
            jlong bitmap){
    GlyphBitmap *gbmp = (GlyphBitmap *)(long)bitmap;
    free(gbmp->bitmap.buffer);
    free(gbmp);
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    drawStringNative
 * Signature: (JJJJII[CIJ)V
 * Draws text on XftDraw with specified parameters.
 */
JNIEXPORT void 
    JNICALL Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_drawStringNative
      (JNIEnv *env, jclass jobj,  jlong xftDraw, jlong display,
            jlong colormap, jlong font, jint x, jint y, jcharArray str, jint len, jlong color){

    jboolean iscopy;

    XftDraw *draw = (XftDraw *)(long)xftDraw;
    Display *dpy = (Display *)(long)display;
    Colormap  cmap = (Colormap)colormap;
    XftFont *fnt = (XftFont *)(long)font;
    XftColor xftColor;
    XRenderColor renderColor;
    XftChar16 *string;
    XColor *xcolor = (XColor *)(long)color;

    XftDrawSetSubwindowMode(draw, IncludeInferiors/*mode*/); 

    string = (XftChar16 *)(*env)->GetCharArrayElements(env, str, &iscopy);

    /* Creating XRenderColor structure */
    renderColor.red = xcolor->red;
    renderColor.green = xcolor->green;
    renderColor.blue = xcolor->blue;
    renderColor.alpha = 0xFFFF;

    /* Creating XftColor structure */
    if (XAllocColor (dpy, cmap, xcolor)){
        xftColor.pixel = xcolor->pixel;
    }
    xftColor.color.red = renderColor.red;
    xftColor.color.green = renderColor.green;
    xftColor.color.blue = renderColor.blue;
    xftColor.color.alpha = renderColor.alpha;

    XftDrawString16 (draw,
             &xftColor,
             fnt,
             x,
             y,
             string,
             len);

    (*env)->ReleaseCharArrayElements(env, str, string, iscopy);
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    createXftDrawNative
 * Signature: (JJJ)J
 * Returns XftDraw handle created from specified parameters.
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_createXftDrawNative
  (JNIEnv *env, jclass jobj, jlong display, jlong drawable, jlong visual){

        XftDraw *draw;
    Display *dpy = (Display *)(long)display;
    Drawable drwbl = (Drawable)drawable;

    /* Creating xftDraw structure */
    draw = XftDrawCreate (dpy, drwbl, (Visual *)(long)visual, 0);

    return (long)draw;

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    xftDrawSetSubwindowModeNative
 * Signature: (JI)V
 * Set new subwindow mode to XftDraw object.
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_xftDrawSetSubwindowModeNative
  (JNIEnv *env, jclass jobj, jlong xftDraw, jint mode){

        XftDraw *draw = (XftDraw *)(long)xftDraw;

        XftDrawSetSubwindowMode(draw, mode); 

}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    XftDrawSetClipRectangles
 * Signature: (JIIJI)Z
 * Sets clipping rectangles in Xft drawable to the specified clipping rectangles. 
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_XftDrawSetClipRectangles
  (JNIEnv *env, jclass jobj, jlong xftDraw, jint xOrigin, jint yOrigin, jlong rects, jint n){

    XftDraw *draw = (XftDraw *)(long)xftDraw;
    XRectangle *xrects = (XRectangle *)(long)rects;

    return XftDrawSetClipRectangles (draw, xOrigin, yOrigin, xrects, n);

}


/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    freeXftDrawNative
 * Signature: (J)V
 * Destroys XftDraw object.
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_freeXftDrawNative
  (JNIEnv *env, jclass jobj, jlong xftDraw){

    XftDraw *draw = (XftDraw *)(long)xftDraw;

    XftDrawDestroy(draw);
}

/* Initializes FreeType FT_Outline structure with the source FT_Outline data. */
int createOutline(FT_Outline *outline, FT_Outline *srcOutline){
    int size;
    outline->flags = srcOutline->flags | FT_OUTLINE_OWNER;
    outline->n_points = srcOutline->n_points;
    outline->n_contours = srcOutline->n_contours;

    size = srcOutline->n_points * sizeof(FT_Vector);
    outline->points = (FT_Vector *)malloc(size);
    if (!outline->points){
        return 0;
    }
    memcpy(outline->points, srcOutline->points, size);

    size = srcOutline->n_points * sizeof(char);
    outline->tags = (char *)malloc(size);
    if (!outline->tags){
        free(outline->points);
        return 0;
    }
    memcpy(outline->tags, srcOutline->tags, size);

    size = srcOutline->n_contours * sizeof(short);
    outline->contours = (short *)malloc(size);
    if (!outline->contours){
        free(outline->points);
        free(outline->tags);
        return 0;
    }
    memcpy(outline->contours, srcOutline->contours, size);

    return 1;
}

/* Disposes FreeType FT_Outline structure. */
void freeOutline(FT_Outline *outline){
    free(outline->points);
    free(outline->tags);
    free(outline->contours);

    free(outline);
}


/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    getGlyphOutline
 * Signature: (JC)J
 * Returns pointer to the FreeType FT_Outline structure. 
 */
JNIEXPORT jlong JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getGlyphOutline(JNIEnv *env, jclass obj, jlong fnt, jchar chr){

    XftFont *font = (XftFont *)(long)fnt;
    FT_Face face;
    FT_Error error;
    FT_Outline *outline = NULL;
    
    if (!font){
        return 0;
    }
    face = XftLockFace(font);

    if(!face){
        return 0;
    }
    error = FT_Load_Char(face, (FT_ULong)chr, FT_LOAD_RENDER | FT_LOAD_TARGET_MONO);
    
    if(error){
        throwNPException(env, "getGlyphOutline : FreeType error");
        XftUnlockFace(font);
    
        return (jlong)(IDATA)NULL;
    }

    if ((face->glyph->format & ft_glyph_format_outline) != 0){
        outline = (FT_Outline *)malloc(sizeof(FT_Outline));
        if (!createOutline(outline, &face->glyph->outline)){
            free(outline);
        }
    }   
    
    XftUnlockFace(font);
    
    return (long)outline;
}

/*
 * Class:     org_apache_harmony_awt_gl_font_LinuxNativeFont
 * Method:    freeGlyphOutline
 * Signature: (JC)J
 * Disposes FT_Outline memory block.
 */
JNIEXPORT void JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_freeGlyphOutline(JNIEnv *env, jclass obj, jlong j_outline){
    FT_Outline *outline = (FT_Outline *)(long)j_outline;
    
    freeOutline(outline);
}

/*
 *  Returns desierd tag index from the list.
 */
int getTagIndex(Tag* tagList, int count, Tag value){
    int result = -1;
    int i;

    for (i = 0; i < count; i++){
        if (* (DWORD *)tagList[i] == * (DWORD *)value){
            return result = i;
        }
    }
    return result;

}

/*
 * Returns an array of pairs of coordinates [x1, y1, x2, y2...] from 
 * FreeType FT_Vector structure.  
 */
JNIEXPORT jfloatArray JNICALL Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getPointsFromFTVector
    (JNIEnv *env, jclass obj, jlong points, jint size){

    jfloatArray flArray;
    float * fpPoints = (float *)malloc(sizeof(float) * size * 2);
    int i;
    FT_Vector *ft_points = (FT_Vector *)(long)points;
    for(i = 0; i < size; i++){
        fpPoints[i*2] = (float)((int)ft_points[i].x + 32)/64;
        fpPoints[i*2 + 1] = (float)(-(int)ft_points[i].y + 32)/64;
    }

    flArray=(*env)->NewFloatArray(env, size*2);
    (*env)->SetFloatArrayRegion(env, flArray, 0, size*2, fpPoints);
    free(fpPoints);
    return flArray;
}

/* Returns an array of extrametrics of the font. */
JNIEXPORT jfloatArray JNICALL 
    Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getExtraMetricsNative(JNIEnv *env, jclass obj, jlong fnt, jint fontHeight, jint fontType) {

    jfloatArray metrics;
    jfloat values[9];

    XftFont *xftFnt = (XftFont *)(long)fnt;
    FT_Face face;
    TT_OS2* os2;
#if HAS_FREETYPE_INTERNAL
     TT_Face tt_face;
#endif
    int units_per_EM;
    float mltpl;
    
    if (!xftFnt){
        return NULL;
    }

    face = XftLockFace(xftFnt);

    if(!face){
        return NULL;
    }
    units_per_EM = face->units_per_EM;
    if (units_per_EM == 0){
//          throwNPException(env, "Units per EM value is equals to zero");
        XftUnlockFace(xftFnt);
        
        return NULL;
    }
    mltpl = (float)fontHeight / units_per_EM;
    
    if (fontType == FONT_TYPE_TT){
#if HAS_FREETYPE_INTERNAL
        tt_face = (TT_Face)face;
        os2 = &(tt_face->os2);
#else
        os2 = (TT_OS2*)FT_Get_Sfnt_Table(face, ft_sfnt_os2);
#endif

        values[0] = (float)(os2->xAvgCharWidth) * mltpl; // the average width of characters in the font
        values[1] = (float)(os2->ySubscriptXSize) * mltpl; // horizontal size for subscripts 
        values[2] = (float)(os2->ySubscriptYSize) * mltpl; // vertical size for subscripts 
        values[3] = (float)(os2->ySubscriptXOffset) * mltpl; // horizontal offset for subscripts
        values[4] = (float)(os2->ySubscriptYOffset) * mltpl; // vertical offset value for subscripts
        values[5] = (float)(os2->ySuperscriptXSize) * mltpl; // horizontal size for superscripts
        values[6] = (float)(os2->ySuperscriptYSize) * mltpl; // vertical size for superscripts
        values[7] = (float)(os2->ySuperscriptXOffset) * mltpl; // horizontal offset for superscripts 
        values[8] = (float)(os2->ySuperscriptYOffset) * mltpl; // vertical offset for superscripts 
    } else {
        values[0] = 0.0f; // the average width of characters in the font
        values[1] = 0.7f * fontHeight; // horizontal size for subscripts 
        values[2] = 0.65f * fontHeight;; // vertical size for subscripts 
        values[3] = 0.0f; // horizontal offset for subscripts
        values[4] = 0.15f * fontHeight; // vertical offset value for subscripts
        values[5] = 0.7f * fontHeight; // horizontal size for superscripts
        values[6] = 0.65f * fontHeight; // vertical size for superscripts
        values[7] = 0.0f; // horizontal offset for superscripts 
        values[8] = 0.45 * fontHeight; // vertical offset for superscripts 
    }  
    XftUnlockFace(xftFnt);
    
    metrics = (*env)->NewFloatArray(env, 9);
    (*env)->SetFloatArrayRegion(env, metrics, 0, 9, values);

    return metrics;
       
}

/*
 * Getting antialiased font from existing font 
 */
JNIEXPORT jlong JNICALL
	Java_org_apache_harmony_awt_gl_font_LinuxNativeFont_getAntialiasedFont(
		JNIEnv *env, jclass jobj, jlong font, jlong display, jboolean isAntialiasing){
	
    XftFont *fnt = (XftFont *)(long)font;
    Display *dpy = (Display *)(long)display;

    XftResult result;
    XftPattern *mpattern = XftFontMatch(dpy, DefaultScreen(dpy),fnt->pattern,&result); 

    XftPatternDel(mpattern, XFT_ANTIALIAS);
    if (isAntialiasing) {
        if (!XftPatternAddBool(mpattern, XFT_ANTIALIAS, True)) {
            throwNPException(env,
                "Error during adding font antialias set to true to XFTPattern structure");
        }
	}
    else {
        if (!XftPatternAddBool(mpattern, XFT_ANTIALIAS, False)) {
            throwNPException(env,
                "Error during adding font antialias set to false to XFTPattern structure");
        }
    }

    XftFont *aaFnt = XftFontOpenPattern(dpy, mpattern);
    return (long)aaFnt;
}
