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

#include "vmizip.h"
#include "zipsup.h"
#include "hyport.h"

I_32 VMCALL 
vmizip_getZipEntryData(VMInterface * vmi, struct VMIZipFile * zipFile, struct VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize) 
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getZipEntryData(PORTLIB, zipFile, entry, buffer, bufferSize);
}

I_32 VMCALL
vmizip_getZipEntryFromOffset(VMInterface * vmi, struct VMIZipFile * zipFile, struct VMIZipEntry * entry, IDATA offset, I_32 flags) 
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getZipEntryFromOffset(PORTLIB, zipFile, entry, offset);
}

void VMCALL
vmizip_resetZipFile(VMInterface * vmi, struct VMIZipFile * zipFile, IDATA * nextEntryPointer)
{
	PORT_ACCESS_FROM_VMI(vmi);
	zip_resetZipFile(PORTLIB, zipFile, nextEntryPointer);
}

I_32 VMCALL 
vmizip_getNextZipEntry(VMInterface * vmi, struct VMIZipFile * zipFile, struct VMIZipEntry * zipEntry, IDATA * nextEntryPointer, I_32 flags)
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getNextZipEntry(PORTLIB, zipFile, zipEntry, nextEntryPointer);
}

I_32 VMCALL 
vmizip_getZipEntry(VMInterface * vmi, struct VMIZipFile * zipFile, struct VMIZipEntry * entry, const char *filename, I_32 flags)
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getZipEntry(PORTLIB, zipFile,entry, filename, flags);
}

I_32 VMCALL 
vmizip_getZipEntryExtraField(VMInterface * vmi, struct VMIZipFile * zipFile, struct VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize)
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getZipEntryExtraField(PORTLIB, zipFile, entry, buffer, bufferSize);
}

I_32 VMCALL
vmizip_getZipEntryRawData(VMInterface * vmi, VMIZipFile * zipFile, VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize, U_32 offset)
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getZipEntryRawData(PORTLIB, zipFile, entry, buffer, bufferSize, offset);
}


void VMCALL
vmizip_initZipEntry(VMInterface * vmi, struct VMIZipEntry * entry)
{
	PORT_ACCESS_FROM_VMI(vmi);
	zip_initZipEntry(PORTLIB, entry);
}

I_32 VMCALL
vmizip_openZipFile(VMInterface * vmi, char *filename, struct VMIZipFile * zipFile, I_32 flags)
{
	PORT_ACCESS_FROM_VMI(vmi);
	
	VMIZipFunctionTable *zipFuncs = (*vmi)->GetZipFunctions(vmi);
	/* This is a synchonization hole, should probably add a mutex to control setting this variable. */
	if ( zipFuncs->reserved == NULL ) {
		zipFuncs->reserved = zipCachePool_new(PORTLIB);
	}
	
	return zip_openZipFile(PORTLIB, filename, zipFile, (flags & ZIP_FLAG_OPEN_CACHE) ? (HyZipCachePool *)zipFuncs->reserved : NULL );
}

void VMCALL
vmizip_freeZipEntry(VMInterface * vmi, struct VMIZipEntry * entry)
{
	PORT_ACCESS_FROM_VMI(vmi);
	zip_freeZipEntry(PORTLIB, entry);
}

I_32 VMCALL
vmizip_closeZipFile(VMInterface * vmi, struct VMIZipFile * zipFile)
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_closeZipFile(PORTLIB, zipFile);
}

I_32 VMCALL
vmizip_getZipEntryComment(VMInterface * vmi, struct VMIZipFile * zipFile, struct VMIZipEntry * entry, U_8 * buffer, U_32 bufferSize) 
{
	PORT_ACCESS_FROM_VMI(vmi);
	return zip_getZipEntryComment(PORTLIB, zipFile, entry, buffer, bufferSize);
}

IDATA VMCALL
vmizipCache_enumGetDirName(void *handle, char *nameBuf, UDATA nameBufSize)
{
	return zipCache_enumGetDirName(handle, nameBuf, nameBufSize);
}

IDATA VMCALL
vmizipCache_enumNew(struct HyZipCache * zipCache, char *directoryName, void **handle)
{
	return zipCache_enumNew(zipCache, directoryName, handle);
}
	
IDATA VMCALL 
vmizipCache_enumElement(void *handle, char *nameBuf, UDATA nameBufSize, UDATA * offset)
{
	return zipCache_enumElement(handle, nameBuf, nameBufSize, offset);
}

void VMCALL
vmizipCache_enumKill(void *handle)
{
	zipCache_enumKill(handle);
}

VMIZipFunctionTable VMIZipLibraryTable = {
	vmizip_closeZipFile,
	vmizip_freeZipEntry,
	vmizip_getNextZipEntry,
	vmizip_getZipEntry,
	vmizip_getZipEntryComment,
	vmizip_getZipEntryData,
	vmizip_getZipEntryExtraField,
	vmizip_getZipEntryFromOffset,
	vmizip_getZipEntryRawData,
	vmizip_initZipEntry,
	vmizip_openZipFile,
	vmizip_resetZipFile,
	vmizipCache_enumElement,
	vmizipCache_enumGetDirName,
	vmizipCache_enumKill,
	vmizipCache_enumNew,
	NULL
};
