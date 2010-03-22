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
#ifndef vmizip_h
#define vmizip_h

#include "hycomp.h"
#include "vmi.h"

#ifdef __cplusplus
extern "C" {
#endif

/* function return codes */
#define ZIP_ERR_FILE_READ_ERROR  -1
#define ZIP_ERR_NO_MORE_ENTRIES  -2
#define ZIP_ERR_OUT_OF_MEMORY  -3
#define ZIP_ERR_UNKNOWN_FILE_TYPE  -4
#define ZIP_ERR_UNSUPPORTED_FILE_TYPE  -5
#define ZIP_ERR_FILE_CORRUPT  -6
#define ZIP_ERR_BUFFER_TOO_SMALL  -7
#define ZIP_ERR_ENTRY_NOT_FOUND  -8
#define ZIP_ERR_FILE_OPEN_ERROR  -9
#define ZIP_ERR_FILE_CLOSE_ERROR  -10
#define ZIP_ERR_INTERNAL_ERROR  -11

/* flags used in VMIZipEntry compressionMethod */
#define ZIP_CM_Stored  0
#define ZIP_CM_Shrunk  1
#define ZIP_CM_Reduced1  2
#define ZIP_CM_Reduced2  3
#define ZIP_CM_Reduced3  4
#define ZIP_CM_Imploded  6
#define ZIP_CM_Reduced4  5
#define ZIP_CM_Tokenized  7
#define ZIP_CM_Deflated  8

/* flags used in zip_getZipEntry(), zip_getZipEntryFromOffset(), and zip_getNextZipEntry() */
#define ZIP_FLAG_FIND_DIRECTORY 1
#define ZIP_FLAG_READ_DATA_POINTER 2

/* flags used in zip_openZipFile() */
#define ZIP_FLAG_OPEN_CACHE 1

typedef struct VMIZipEntry
{
	U_8 *data;
	U_8 *filename;
	U_8 *extraField;
	U_8 *fileComment;
	I_32 dataPointer;
	I_32 filenamePointer;
	I_32 extraFieldPointer;
	I_32 fileCommentPointer;
	U_32 compressedSize;
	U_32 uncompressedSize;
	U_32 crc32;
	U_16 filenameLength;
	U_16 extraFieldLength;
	U_16 fileCommentLength;
	U_16 internalAttributes;
	U_16 versionCreated;
	U_16 versionNeeded;
	U_16 flags;
	U_16 compressionMethod;
	U_16 lastModTime;
	U_16 lastModDate;
	U_8 internalFilename[80];
} VMIZipEntry;

typedef struct VMIZipFile
{
	U_8 *filename;
	void *cache;
	void *cachePool;
	I_32 fd;
	I_32 pointer;
	U_8 internalFilename[80];
	U_8 type;
	char _vmipadding0065[3];  /* 3 bytes of automatic padding */
} VMIZipFile;

struct HyZipCache; /* forward declaration */

typedef struct VMIZipFunctionTable {
	I_32 (PVMCALL zip_closeZipFile) (VMInterface * vmi, VMIZipFile * zipFile) ;
	void (PVMCALL zip_freeZipEntry) (VMInterface * vmi, VMIZipEntry * entry) ;
	I_32 (PVMCALL zip_getNextZipEntry) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * zipEntry, IDATA * nextEntryPointer, I_32 flags) ;
	I_32 (PVMCALL zip_getZipEntry) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, const char *filename, I_32 flags) ;
	I_32 (PVMCALL zip_getZipEntryComment) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize) ;
	I_32 (PVMCALL zip_getZipEntryData) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize) ;
	I_32 (PVMCALL zip_getZipEntryExtraField) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize) ;
	I_32 (PVMCALL zip_getZipEntryFromOffset) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, IDATA offset, I_32 flags) ;
	I_32 (PVMCALL zip_getZipEntryRawData) (VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize, U_32 offset) ;
	void (PVMCALL zip_initZipEntry) (VMInterface * vmi, VMIZipEntry * entry) ;
	I_32 (PVMCALL zip_openZipFile) (VMInterface * vmi, char *filename, VMIZipFile * zipFile, I_32 flags) ;
	void (PVMCALL zip_resetZipFile) (VMInterface * vmi, VMIZipFile * zipFile, IDATA * nextEntryPointer) ;
	IDATA (PVMCALL zipCache_enumElement) (void *handle, char *nameBuf, UDATA nameBufSize, UDATA * offset) ;
	IDATA (PVMCALL zipCache_enumGetDirName) (void *handle, char *nameBuf, UDATA nameBufSize) ;
	void (PVMCALL zipCache_enumKill) (void *handle) ;
	IDATA (PVMCALL zipCache_enumNew) (struct HyZipCache * zipCache, char *directoryName, void **handle) ;

	void *reserved;
} VMIZipFunctionTable;


#ifdef __cplusplus
}
#endif

#endif     /* vmizip_h */
