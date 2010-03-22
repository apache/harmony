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
 * @author Oleg V. Khaschansky
 * 
 */

#include "lcms.h"
#include "icc34.h"
#ifndef cmsFLAGS_NOTCACHE
#define cmsFLAGS_NOTCACHE (0)
#endif

#if (LCMS_VERSION < 117)
#define LCMSBOOL BOOL
#endif

// Just a useful macro
#undef MIN
#define MIN(a,b)    ((a) < (b) ? (a) : (b))


LPLCMSICCPROFILE cmmOpenProfile(LPBYTE dataPtr, DWORD dwSize);

LCMSBOOL cmmCloseProfile(LPLCMSICCPROFILE hProfile);



size_t cmmGetProfileSize(LPLCMSICCPROFILE hProfile);

void cmmGetProfile(LPLCMSICCPROFILE hProfile, LPBYTE data, size_t dataSize);



LCMSBOOL cmmGetProfileHeader(LPLCMSICCPROFILE hProfile, LPBYTE data, size_t dwLen);

LCMSBOOL cmmSetProfileHeader(LPLCMSICCPROFILE hProfile, LPBYTE data);



LCMSBOOL cmmGetProfileElement(LPLCMSICCPROFILE hProfile, icTagSignature sig, LPBYTE data, size_t *dataSize);

long cmmGetProfileElementSize(LPLCMSICCPROFILE hProfile, icTagSignature sig);

LCMSBOOL cmmSetProfileElement(LPLCMSICCPROFILE hProfile, icTagSignature sig, LPVOID data, size_t size);


cmsHTRANSFORM cmmCreateTransform(cmsHPROFILE Input,
    DWORD InputFormat,
    cmsHPROFILE Output,
    DWORD OutputFormat,
    int Intent,
    DWORD dwFlags);
cmsHTRANSFORM cmmCreateMultiprofileTransform(cmsHPROFILE hProfiles[], int nProfiles, int Intent);
