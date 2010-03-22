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



#define PADDING32(x) ((4 - ((x) & 3)) & 3)



// Conversion to/from big endian (icc profiles require big endian encoding)

static icInt32Number swapBytes32(icInt32Number value)
{
  BYTE byte1;
  BYTE byte2;
  LPBYTE pByte = (LPBYTE) &value;

  byte1 = *pByte++;
  byte2 = *pByte++;
  *(pByte-1) = *pByte;
  *pByte++ = byte2;
  *(pByte-3) = *pByte;
  *pByte = byte1;

  return value;
}


// Unsafe but simplier then LCMS memory access

typedef struct {
  LPBYTE Base;    // Base pointer
  LPBYTE CurrPtr; // Moving pointer
  LCMSBOOL needsUpdate; // Tag sizes changed or tags added/removed
  LCMSBOOL needsUpdateCriticalTags; // Critical tags changed
  size_t size;
} MemBuffer;

static MemBuffer* openMemBuffer(LPBYTE Ptr, size_t size) {
  MemBuffer *buffer = malloc(sizeof(MemBuffer));

  // Need to copy memory to protect from caller's modifications
  buffer->Base = buffer->CurrPtr = malloc(size);
  buffer->size = size;

  CopyMemory(buffer->Base, Ptr, size);

  buffer->needsUpdate = FALSE;
  buffer->needsUpdateCriticalTags = FALSE;

  return buffer;
}

static LCMSBOOL allocMemBuffer(LPVOID f, size_t size) {
  MemBuffer* memBuffer = (MemBuffer*) f;
    
  memBuffer->CurrPtr = malloc(size);
  memBuffer->size = size;

  if(!memBuffer->CurrPtr) { // Out of memory
    memBuffer->CurrPtr = memBuffer->Base;
    return FALSE;
  } 
  
  memBuffer->Base = memBuffer->CurrPtr;

  return TRUE;
}

static LCMSBOOL closeMemBuffer(MemBuffer **buffer) {
  if(*buffer)
    free(*buffer);
  *buffer = NULL;

  return TRUE;
}

static size_t readMemBuffer(LPVOID buffer, size_t size, size_t count, LPVOID f) {   
  MemBuffer* memBuffer = (MemBuffer*) f;
  size_t len = size * count;
  CopyMemory(buffer, memBuffer->CurrPtr, len);
  memBuffer->CurrPtr += len;
  
  return count;
}

static LCMSBOOL seekMemBuffer(LPVOID f, size_t offset) {
  MemBuffer* memBuffer = (MemBuffer*) f;
  memBuffer->CurrPtr = memBuffer->Base + offset;

  return FALSE; 
}

static size_t tellMemBuffer(LPVOID f) {
  MemBuffer* memBuffer = (MemBuffer*) f;
  return memBuffer->CurrPtr - memBuffer->Base;
}

static LCMSBOOL writeMemBuffer(LPVOID f, size_t size, LPBYTE Data) {
  MemBuffer* memBuffer = (MemBuffer*) f;

  if (size == 0) return TRUE;
       
  if (memBuffer != NULL) {
    CopyMemory(memBuffer->CurrPtr, Data, size);
    memBuffer->CurrPtr += size;
  }
  
  return TRUE;
}


static size_t zeroMemBuffer(LPVOID f, size_t size) {
  MemBuffer* memBuffer = (MemBuffer*) f;

  if (size == 0) return TRUE;
       
  if (memBuffer != NULL) {
    ZeroMemory(memBuffer->CurrPtr, size);
    memBuffer->CurrPtr += size;
  }

  return size;
}


static LCMSBOOL updateHeader(LPLCMSICCPROFILE Icc) {
  icHeader head;

  readMemBuffer(&head, sizeof(icHeader), 1, Icc -> stream);

  // Stop processing if data are corrupted
  if (swapBytes32(head.magic) != icMagicNumber) return FALSE;

  Icc->Version = swapBytes32(head.version);
  Icc->DeviceClass = swapBytes32(head.deviceClass);
  Icc->ColorSpace = swapBytes32(head.colorSpace);
  Icc->PCS = swapBytes32(head.pcs);
  Icc->flags = swapBytes32(head.flags);
  Icc->RenderingIntent = (icRenderingIntent) swapBytes32(head.renderingIntent);

  // For now ICC spec states that illuminant should be D50 in all profiles
  Icc->Illuminant = *cmsD50_XYZ();

  return TRUE;
}


int findTag(LPLCMSICCPROFILE Icc, icTagTypeSignature sig) {

  int i;

  for (i=0; i < Icc->TagCount; i++) {
    if (sig == Icc->TagNames[i])
      return i;
    }

  return -1; // Not found

}



int getValidTagCount(LPLCMSICCPROFILE Icc) {

  int tagCount = 0, i;



  // Get the number of valid tags
  for (i=0; i < Icc->TagCount; i++) {
    if (Icc->TagNames[i])
      tagCount++;
  }

  return tagCount;
}



size_t updateTagOffsets(LPLCMSICCPROFILE Icc) {

  int i;

  size_t offset = 0;

  size_t padding;  



  offset = sizeof(icHeader) + // Header

           sizeof(icInt32Number) + // Tag count

           sizeof(icTag) * getValidTagCount(Icc); // Tag table



  seekMemBuffer(Icc->stream, offset);



  for(i=0; i < Icc->TagCount; i++) {

    if(Icc->TagNames[i]) {

      padding = PADDING32(offset);

      offset += padding;

      Icc->TagOffsets[i] = offset;

      offset += Icc->TagSizes[i];

    }

  }



  // Pad the last tag (required according to icc spec)

  padding = PADDING32(offset);

  offset += padding;

  

  return offset;

}



void updateMemoryBuffer(LPLCMSICCPROFILE Icc) {

  int i;

  icTag tag;

  size_t oldTagOffsets[MAX_TABLE_TAG];  

  size_t profileSize, padding;

  LPBYTE oldBasePtr = NULL;

  icInt32Number tmp;



  MemBuffer *memBuffer = (MemBuffer*) Icc->stream;

  if(!memBuffer->needsUpdate) 
    return;
  

  // Save old tag offsets and base pointer

  CopyMemory(oldTagOffsets, Icc->TagOffsets, sizeof(size_t)*MAX_TABLE_TAG);

  oldBasePtr = memBuffer->Base; // save pointer to the old buffer



  // Calculate new tag offsets

  profileSize = updateTagOffsets(Icc);

  memBuffer->size = profileSize;



  // Ajust mem buffer and set profile size to the new value

  allocMemBuffer(Icc->stream, profileSize);

  seekMemBuffer(Icc->stream, 0);

  tmp = swapBytes32((icInt32Number) profileSize);

  writeMemBuffer(Icc->stream, sizeof(icInt32Number), (LPBYTE) &tmp);

  

  // Copy the rest of the header from the old buffer

  writeMemBuffer(Icc->stream, 

             sizeof(icHeader) - sizeof(icInt32Number), 

             oldBasePtr + sizeof(icInt32Number) );



  // Write tag count

  tmp = swapBytes32(getValidTagCount(Icc));

  writeMemBuffer(Icc->stream, sizeof(icInt32Number), (LPBYTE) &tmp);



  // Write tag table

  for(i=0; i < Icc->TagCount; i++) {

    if(Icc->TagNames[i]) {

      tag.sig = swapBytes32(Icc->TagNames[i]);

      tag.offset = swapBytes32((icInt32Number) Icc->TagOffsets[i]);

      tag.size = swapBytes32((icInt32Number) Icc->TagSizes[i]);



      writeMemBuffer(Icc->stream, sizeof(icTag), (LPBYTE) &tag);

    }

  }



  // Write tag data

  for(i=0; i < Icc->TagCount; i++) {

    if(Icc->TagNames[i]) {

      seekMemBuffer(Icc->stream, Icc->TagOffsets[i]);



      if(Icc->TagPtrs[i]) {

        writeMemBuffer(Icc->stream, Icc->TagSizes[i], Icc->TagPtrs[i]);

        free(Icc->TagPtrs[i]);

        Icc->TagPtrs[i] = NULL;

      } else {

        writeMemBuffer(Icc->stream, Icc->TagSizes[i], oldBasePtr + oldTagOffsets[i]);

      }



      // Pad the data with zeroes

      padding = PADDING32(Icc->TagSizes[i]);

      zeroMemBuffer(Icc->stream, padding);

    }

  }



  // Pad the last tag with zeroes

  padding = PADDING32(Icc->TagSizes[i]);

  zeroMemBuffer(Icc->stream, padding);



  memBuffer->needsUpdate = FALSE;

  free(oldBasePtr);

}



// Forces LCMS to update whitepoint, blackpoint and chromatic adaptation matrix

// First mem buffer should be updated to read valid values from it

void updateCriticalTags(LPLCMSICCPROFILE Icc) {

  LPVOID savedData = NULL;

  int idx;

  cmsCIEXYZ* dummy;



  MemBuffer *memBuffer = (MemBuffer*) Icc->stream;



  if(!memBuffer->needsUpdateCriticalTags) // Nothing to do

    return;



  idx = findTag(Icc, icSigMediaWhitePointTag);

  dummy = cmsD50_XYZ();

  if(idx >= 0) { // There is media white point already     
    cmsAddTag(Icc, icSigMediaWhitePointTag, dummy); // Force LCMS to read critical tags
    free(Icc->TagPtrs[idx]); // Get rid of dummy media white point
    Icc->TagPtrs[idx] = NULL; 
  } else {
    cmsAddTag(Icc, icSigMediaWhitePointTag, dummy); // Force LCMS to read critical tags
    // Get rid of dummy media white point
    free(Icc->TagPtrs[Icc->TagCount-1]);
    Icc->TagPtrs[Icc->TagCount-1] = NULL;
    Icc->TagNames[Icc->TagCount-1] = 0;
    Icc->TagSizes[Icc->TagCount-1] = 0;
    Icc->TagOffsets[Icc->TagCount-1] = 0;
    Icc->TagCount--;
  }

  memBuffer->needsUpdateCriticalTags = FALSE;
}



void updateAll(LPLCMSICCPROFILE Icc) {

  updateMemoryBuffer(Icc);

  updateCriticalTags(Icc);

}



// ------------------------- API


#pragma warning(disable : 4028)
LPLCMSICCPROFILE cmmOpenProfile(LPBYTE dataPtr, DWORD dwSize) {

  LPLCMSICCPROFILE Icc = cmsOpenProfileFromMem(dataPtr, dwSize);

  if(!Icc) // Probably invalid data
    return NULL;

  // Get rid of LCMS IO

  closeMemBuffer(Icc->stream);
  Icc->stream = openMemBuffer(dataPtr, dwSize);

  return Icc;

}





LCMSBOOL cmmCloseProfile(LPLCMSICCPROFILE hProfile) {

  return cmsCloseProfile(hProfile);

}



size_t cmmGetProfileSize(LPLCMSICCPROFILE hProfile) {

  updateMemoryBuffer(hProfile);

  return ((MemBuffer*)hProfile->stream)->size;

}



void cmmGetProfile(LPLCMSICCPROFILE hProfile, LPBYTE data, size_t dataSize) {

  MemBuffer *memBuffer = (MemBuffer*)hProfile->stream;

  updateMemoryBuffer(hProfile);

  CopyMemory(data, memBuffer->Base, dataSize);

}



LCMSBOOL cmmGetProfileElement(

    LPLCMSICCPROFILE hProfile, 

    icTagSignature sig, 

    LPBYTE data, 

    size_t *dataSize

) {

  int idx = findTag(hProfile, sig);



  if(idx < 0) {
    cmsSignalError(LCMS_ERRC_ABORTED, "Tagged profile element not found");
    return FALSE;    
  }

  *dataSize = MIN(*dataSize, hProfile->TagSizes[idx]);



  if(hProfile->TagPtrs[idx]) {

    CopyMemory(data, hProfile->TagPtrs[idx], *dataSize);

  } else {

    seekMemBuffer(hProfile->stream, hProfile->TagOffsets[idx]);

    readMemBuffer(data, 1, *dataSize, hProfile->stream);

  }



  return TRUE;

}



long cmmGetProfileElementSize(LPLCMSICCPROFILE hProfile, icTagSignature sig) {

  int idx = findTag(hProfile, sig);



  if(idx < 0) 

    return -1;

  else

    return hProfile->TagSizes[idx];

}



LCMSBOOL cmmGetProfileHeader(LPLCMSICCPROFILE hProfile, LPBYTE data, size_t size) {

  size_t bytesToRead = MIN(size, sizeof(icHeader));

  seekMemBuffer(hProfile->stream, 0);

  readMemBuffer(data, 1, bytesToRead, hProfile->stream);

  return TRUE;

}



LCMSBOOL cmmSetProfileHeader(LPLCMSICCPROFILE hProfile, LPBYTE data) {

  seekMemBuffer(hProfile->stream, 0);

  writeMemBuffer(hProfile->stream, sizeof(icHeader), data);

  

  if(!updateHeader(hProfile))

    return FALSE;



  return TRUE;

}



LCMSBOOL cmmSetProfileElement(LPLCMSICCPROFILE hProfile, icTagSignature sig, LPVOID data, size_t size) {

  MemBuffer *memBuffer;



  int idx = findTag(hProfile, sig);



  if(idx < 0) {
    idx = hProfile->TagCount;
    hProfile->TagCount++;
    if (hProfile->TagCount >= MAX_TABLE_TAG) {
      cmsSignalError(LCMS_ERRC_ABORTED, "Too many tags (%d)", MAX_TABLE_TAG);
      hProfile->TagCount = MAX_TABLE_TAG-1;
    }
  }


  memBuffer = (MemBuffer*) hProfile->stream;



  if(
    sig == icSigMediaWhitePointTag ||
    sig == icSigMediaBlackPointTag ||
    sig == icSigChromaticAdaptationTag

    ) 

    memBuffer->needsUpdateCriticalTags = TRUE;

  

  memBuffer->needsUpdate = TRUE; // Always needs update



  if(size == 0) {

    if(hProfile->TagPtrs[idx]) 

      free(hProfile->TagPtrs[idx]);

    hProfile->TagPtrs[idx] = NULL;

    hProfile->TagNames[idx] = 0;

    hProfile->TagOffsets[idx] = 0;

    hProfile->TagSizes[idx] = 0;

  } else {

    if(hProfile->TagPtrs[idx]) 

      hProfile->TagPtrs[idx] = realloc(hProfile->TagPtrs[idx], size);

    else

      hProfile->TagPtrs[idx] = malloc(size);

    hProfile->TagSizes[idx] = size;

    hProfile->TagOffsets[idx] = 0;

    hProfile->TagNames[idx] = sig;

    CopyMemory(hProfile->TagPtrs[idx], data, size);

  }



  return TRUE;

}



cmsHTRANSFORM cmmCreateTransform(cmsHPROFILE Input,
    DWORD InputFormat,
    cmsHPROFILE Output,
    DWORD OutputFormat,
    int Intent,
    DWORD dwFlags) {
  // Make sure that modifications are saved in the profile buffer for both in and out
  updateAll(Input);
  updateAll(Output);
  // Pass profiles to LCMS
  return cmsCreateTransform(Input, InputFormat, Output, OutputFormat, Intent, dwFlags);
}
