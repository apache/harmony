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

#include "cmmapi.h"
/*
typedef struct {
  cmsHTRANSFORM *xforms; // Transforms
  int count; // Number of transforms
} MultiprofileXform;

LCMSBOOL doMultiprofileTransform(MultiprofileXform *xform, LPVOID in, LPVOID out, unsigned int size) {
    cmsHTRANSFORM* Transforms = xform->xforms;
    int i, bpp;
    DWORD outFmt;

    LPVOID tmpIn = in, tmpOut = NULL;

    for(i=0; i < xform->count; i++) {
      _LPcmsTRANSFORM p = (_LPcmsTRANSFORM) (LPSTR) Transforms[i];

      if(i != xform->count-1) {
        // Calculate bytes per pixel
        outFmt = p->OutputFormat;
        bpp = (T_EXTRA(outFmt) + T_CHANNELS(outFmt)) * (T_BYTES(outFmt) + 1);
        tmpOut = malloc(bpp * size);
      } else {
        tmpOut = out;
      }

      cmsDoTransform(Transforms[i], tmpIn, tmpOut, size);

      if(i)
        free(tmpIn);

      tmpIn = tmpOut;
    }

    return TRUE;
}
*/

void cmmPrecalculatedXformImpl(_LPcmsTRANSFORM p, LPVOID in, LPVOID out, unsigned int Size)
{
  LPBYTE inPtr = in;
  LPBYTE outPtr = out;
  
  WORD xformIn[MAXCHANNELS];
  WORD xformOut[MAXCHANNELS];
  
  unsigned int i;

  for (i=0; i < Size; i++) {
    inPtr = p->FromInput(p, xformIn, inPtr);
    cmsEvalLUT(p->DeviceLink, xformIn, xformOut);
    outPtr = p->ToOutput(p, xformOut, outPtr);
  }
}

int cmmMultiprofileSampler(WORD In[], WORD Out[], LPVOID Cargo) {
  int i;
  cmsHTRANSFORM* Transforms = (cmsHTRANSFORM*)Cargo;

  // Need to go from In to Out at least once
  cmsDoTransform(Transforms[0], In, Out, 1);
  
  for (i=1; Transforms[i]; i++)
    cmsDoTransform(Transforms[i], Out, Out, 1);
    
  return TRUE;
}

#define CLEANUP_AND_RETURN(retval) {\
    if(Grid) cmsFreeLUT(Grid); \
    for(;i>0;--i) if(transforms[i]) cmsDeleteTransform(transforms[i]);\
    return (retval);}


// Simplified version of multiprofile transform creator
// Flags are removed from arguments. 
// Gamut checking and named color profiles are not supported.
// WARNING: I/O pixel formats should be specified for the created transform later by caller.
cmsHTRANSFORM cmmCreateMultiprofileTransform(cmsHPROFILE hProfiles[], int nProfiles, int Intent) 
{   
  DWORD inFmt, outFmt;
  cmsHPROFILE hTargetProfile, hProfile;   
  icColorSpaceSignature csIn, csOut;   

  LPLUT Grid = NULL;
  int nGridPoints, nInChannels, nOutChannels = 3, i = 0;

  _LPcmsTRANSFORM p; // Resulting transform        

  cmsHTRANSFORM transforms[255]; // Cannot merge more than 255 profiles
  ZeroMemory(transforms, sizeof(transforms));
  if (nProfiles > 255) {
    return NULL; // Too many profiles
  }

  // Check if there are any named color profiles
  for (i=0; i < nProfiles; i++) {
    if (cmsGetDeviceClass(hProfiles[i]) == icSigNamedColorClass ||
        cmsGetDeviceClass(hProfiles[i]) == icSigLinkClass) {
      return NULL; // Unsupported named color and device link profiles
    }
  }

  // Create a placeholder transform with dummy I/O formats to place LUT in it
  p = (_LPcmsTRANSFORM) 
    cmsCreateTransform(NULL, TYPE_RGB_8, NULL, TYPE_RGB_8, Intent, cmsFLAGS_NULLTRANSFORM);

  p->EntryColorSpace = cmsGetColorSpace(hProfiles[0]);    

  // Gater information about first input profile
  hProfile = hProfiles[0];
  csIn = cmsGetColorSpace(hProfile);
  nInChannels  = _cmsChannelsOf(csIn);
  inFmt = BYTES_SH(2) | CHANNELS_SH(nInChannels);

  // Create a sequence
  for (i=1; i < nProfiles; i++) {
    // Gather output parameters
    hTargetProfile = hProfiles[i];
    csOut = cmsGetColorSpace(hTargetProfile);
    nOutChannels = _cmsChannelsOf(csOut);               
    outFmt = BYTES_SH(2)|CHANNELS_SH(nOutChannels);
        
    transforms[i-1] = 
      cmsCreateTransform(
        hProfile, inFmt, 
        hTargetProfile, outFmt, 
        Intent, cmsFLAGS_NOTPRECALC | cmsFLAGS_NOTCACHE
      );           
    
    if(transforms[i-1] == NULL)
      CLEANUP_AND_RETURN(NULL); // Incompatible profiles?

    // Assign output parameters to input
    hProfile = hTargetProfile;
    csIn = csOut;
    nInChannels = nOutChannels;               
    inFmt = outFmt;
  }

  p->ExitColorSpace = csOut;
  transforms[i] = NULL; // End marker 

  p->InputProfile  = hProfiles[0];
  p->OutputProfile = hProfiles[nProfiles-1];

  nGridPoints = _cmsReasonableGridpointsByColorspace(p->EntryColorSpace, 0);
   
  nInChannels  = _cmsChannelsOf(cmsGetColorSpace(p->InputProfile));

  // Create 3DCLUT
  if (! (Grid = cmsAllocLUT())) 
    CLEANUP_AND_RETURN(NULL);

  Grid = cmsAlloc3DGrid(Grid, nGridPoints, nInChannels, nOutChannels);

  _cmsComputePrelinearizationTablesFromXFORM(transforms, nProfiles-1, Grid);
      
  // Compute device link on 16-bit basis                
  if (!cmsSample3DGrid(Grid, cmmMultiprofileSampler, (LPVOID) transforms, Grid -> wFlags)) 
    CLEANUP_AND_RETURN(NULL);
  
  // Put the new LUT into resulting transform
  p->DeviceLink = Grid;
  // Set transform method
  p->xform = cmmPrecalculatedXformImpl;

  // Commented out since it is not clear if it is correct or not
  // Sequential transforms gives same result as multiprofile with this call commented out
  /*  
  if(Intent != INTENT_ABSOLUTE_COLORIMETRIC)
      _cmsFixWhiteMisalignment(p);
  */

  // Don't clean LUT
  Grid = NULL;

  CLEANUP_AND_RETURN((cmsHTRANSFORM) p);
}

#undef CLEANUP_AND_RETURN
