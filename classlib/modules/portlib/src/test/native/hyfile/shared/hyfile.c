/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "hycomp.h"
#include "hyport.h"
#include "hycunit.h"
#include "utf8encode.h"

void cleanup(HyPortLibrary hyportLibrary);
void generateAbsoluteFilePath(struct HyPortLibrary *hyportLibrary,char *dist , char *fileName);
void forVprintfTest(struct HyPortLibrary *hyportLibrary,IDATA fd,char* format,...);

int test_hyfile_open(struct HyPortLibrary *hyportLibrary);
int test_hyfile_write(struct HyPortLibrary *hyportLibrary);
int test_hyfile_write_text(struct HyPortLibrary *hyportLibrary);
int test_hyfile_printf(struct HyPortLibrary *hyportLibrary);
int test_hyfile_sync(struct HyPortLibrary *hyportLibrary);
int test_hyfile_set_length(struct HyPortLibrary *hyportLibrary);
int test_hyfile_move(struct HyPortLibrary *hyportLibrary);
int test_hyfile_seek(struct HyPortLibrary *hyportLibrary);
int test_hyfile_read(struct HyPortLibrary *hyportLibrary);
int test_hyfile_length(struct HyPortLibrary *hyportLibrary);
int test_hyfile_attr(struct HyPortLibrary *hyportLibrary);
int test_hyfile_error_message(struct HyPortLibrary *hyportLibrary);
int test_hyfile_findfirst(struct HyPortLibrary *hyportLibrary);
int test_hyfile_findnext(struct HyPortLibrary *hyportLibrary);
int test_hyfile_findclose(struct HyPortLibrary *hyportLibrary);
int test_hyfile_mkdir(struct HyPortLibrary *hyportLibrary);
int test_hyfile_unlinkdir(struct HyPortLibrary *hyportLibrary);
int test_hyfile_lastmod(struct HyPortLibrary *hyportLibrary);
int test_hyfile_vprintf(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret = 0;

  printf("hyfile:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");

  Hytest_init(&hyportLibrary, "Portlib.Hyfile");
  Hytest_func(&hyportLibrary, test_hyfile_open, "hyfile_open");
  Hytest_func(&hyportLibrary, test_hyfile_write, "hyfile_write");
  Hytest_func(&hyportLibrary, test_hyfile_write_text, "hyfile_write_text");
  Hytest_func(&hyportLibrary, test_hyfile_printf, "hyfile_printf");
  Hytest_func(&hyportLibrary, test_hyfile_sync, "hyfile_sync");
  Hytest_func(&hyportLibrary, test_hyfile_set_length, "hyfile_set_length");
  Hytest_func(&hyportLibrary, test_hyfile_move, "hyfile_move");
  Hytest_func(&hyportLibrary, test_hyfile_seek, "hyfile_seek");
  Hytest_func(&hyportLibrary, test_hyfile_read, "hyfile_read");
  Hytest_func(&hyportLibrary, test_hyfile_length, "hyfile_length");
  Hytest_func(&hyportLibrary, test_hyfile_attr, "hyfile_attr");
  Hytest_func(&hyportLibrary, test_hyfile_error_message, "hyfile_error_message");
  Hytest_func(&hyportLibrary, test_hyfile_findfirst, "hyfile_findfirst");
  Hytest_func(&hyportLibrary, test_hyfile_findnext, "hyfile_findnext");
  Hytest_func(&hyportLibrary, test_hyfile_findclose, "hyfile_findclose");
  Hytest_func(&hyportLibrary, test_hyfile_mkdir, "hyfile_mkdir");
  Hytest_func(&hyportLibrary, test_hyfile_unlinkdir, "hyfile_unlinkdir");
  Hytest_func(&hyportLibrary, test_hyfile_lastmod, "hyfile_lastmod");
  Hytest_func(&hyportLibrary, test_hyfile_vprintf, "hyfile_vprintf");
  ret = Hytest_close_and_output(&hyportLibrary);  
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  printf("  portlib shutdown\n");

  return ret;
}

void cleanup(HyPortLibrary hyportLibrary)
{
  char tmpAbsolutePath[255];
  generateAbsoluteFilePath(&hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  hyportLibrary.file_unlink(&hyportLibrary, tmpAbsolutePath);
  generateAbsoluteFilePath(&hyportLibrary,tmpAbsolutePath,"hytest.tmp2");
  hyportLibrary.file_unlink(&hyportLibrary, tmpAbsolutePath);
  generateAbsoluteFilePath(&hyportLibrary,tmpAbsolutePath,"hytest.dir.tmp");
  hyportLibrary.file_unlinkdir(&hyportLibrary, tmpAbsolutePath);
}

void generateAbsoluteFilePath(struct HyPortLibrary *hyportLibrary,char *dist , char *fileName)
{
  char* buf;
  int i=0;
  hyportLibrary->sysinfo_get_executable_name(hyportLibrary,"",&buf);
  i=strlen(buf)-1;
  for(;i>=0;i--)
  {
    if(buf[i]==DIR_SEPARATOR)
    {
      buf[i+1]='\0';
      break;
    }
  }
  strcpy(dist,buf);
  strcpy(&dist[i+1],fileName);
  hyportLibrary->mem_free_memory(hyportLibrary, buf);
}

int test_hyfile_open(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_open......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_write(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  IDATA bytes;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_write......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s\n(%s)",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  bytes = hyportLibrary->file_write(hyportLibrary, fd, (void*)"testing", 7);
  printf("  hyfile_write wrote %d bytes\n", bytes);
  if (bytes != 7) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%d] not [%d] (%s)\n", 7, bytes,HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_write_text(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_write_text......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_write_text(hyportLibrary, fd, "testing", 7);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_write_text write failed: %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_printf(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  IDATA bytes;
  char tmpAbsolutePath[255];
  char buf[255];

#ifdef ZOS
#pragma convlit(suspend)
#endif
  char resultString[] = "000000009   -0002    3.14";
#ifdef ZOS
#pragma convlit(resume)
#endif
  
  printf("testing hyfile_printf......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }

  hyportLibrary->file_printf(hyportLibrary, fd, "%09d %7.4d %7.2f",9,-2,3.1415926535);
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenRead, 0000);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open hytest.tmp for read %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  bytes = hyportLibrary->file_read(hyportLibrary, fd, buf, 25);
  printf("  bytes = %d\n", bytes);
  buf[bytes] = '\0';
  printf("  buf = %s\n", buf);
  
  if (bytes != 25) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%d] not [%d](%s)\n",25,bytes,HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  

  if (strcmp(buf, resultString) != 0) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%s] not [%s] (%s)\n", resultString, buf, HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_sync(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_sync......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  hyportLibrary->file_printf(hyportLibrary, fd, "%d%c%s\n", 1, '2', "3");
  rc = hyportLibrary->file_sync(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_sync failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_set_length(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_set_length......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_set_length (hyportLibrary, fd, 200);
  if(0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to set file hytest.tmp2's length  %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_move(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  char tmp2AbsolutePath[255];
  
  printf("testing hyfile_move......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  generateAbsoluteFilePath(hyportLibrary,tmp2AbsolutePath,"hytest2.tmp");
  rc = hyportLibrary->file_move(hyportLibrary, tmpAbsolutePath, tmp2AbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_move failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmp2AbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_seek(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  I_64 offset;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_seek......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_set_length (hyportLibrary, fd, 200);
  if(0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to set file hytest.tmp2's length  %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  offset = hyportLibrary->file_seek(hyportLibrary, fd, -193, HySeekEnd);
  if (offset != 7) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%d] not [%d] (%s)\n",7,offset,HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_read(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  IDATA bytes;
  char tmpAbsolutePath[255];
  char buf[20];

#ifdef ZOS
#pragma convlit(suspend)
#endif
  char resultString[] = "01234";
#ifdef ZOS
#pragma convlit(resume)
#endif
  
  printf("testing hyfile_read......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_write_text(hyportLibrary, fd, "0123456789", 10);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_write_text write failed: %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenRead, 0000);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open hytest.tmp for read %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  bytes = hyportLibrary->file_read(hyportLibrary, fd, buf, 5);
  printf("  bytes = %d\n", bytes);
  buf[bytes] = '\0';
  printf("  buf = %s\n", buf);
  
  if (bytes != 5) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%d] not [%d] (%s)\n",5,bytes,HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }

  if (strcmp(buf, resultString) != 0) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%s] not [%s] (%s)\n", resultString, buf, HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_length(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  I_64 length;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_length......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_set_length (hyportLibrary, fd, 200);
  if(0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to set file hytest.tmp2's length  %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  length = hyportLibrary->file_length(hyportLibrary, tmpAbsolutePath);
  if (length != 200) {
    Hytest_setErrMsg(hyportLibrary, "Output should be [%d] not [%d] (%s)\n",200,length,HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_attr(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  printf("testing hyfile_attr......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    hyportLibrary->file_close(hyportLibrary, fd);
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  if (hyportLibrary->file_attr(hyportLibrary, tmpAbsolutePath) != HyIsFile) {
    Hytest_setErrMsg(hyportLibrary, "hytest.tmp has incorrect type(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_error_message(struct HyPortLibrary *hyportLibrary)
{
  printf("testing hyfile_error_message......\n");
  printf("  file_error_message: %s\n",hyportLibrary->file_error_message(hyportLibrary));
  return 0;
}

int test_hyfile_findfirst(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  char tmpAbsolutePath[255];
  char buf[100];
  
  printf("testing hyfile_findfirst......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"");

  
  fd = hyportLibrary->file_findfirst(hyportLibrary , tmpAbsolutePath, buf);
  printf("  resultbuf: %s\n",buf);
  if(fd==-1){
    Hytest_setErrMsg(hyportLibrary, "Can not find file.(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;	
  }
  return 0;
}

int test_hyfile_findnext(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  char buf[100];
  
  printf("testing hyfile_findnext......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"");
  
  fd = hyportLibrary->file_findfirst(hyportLibrary , tmpAbsolutePath, buf);
  printf("  resultbuf: %s\n",buf);
  if(fd==-1){
    Hytest_setErrMsg(hyportLibrary, "Can not find file.(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;	
  }
  
  rc = hyportLibrary->file_findnext(hyportLibrary, fd, buf);
  if(rc == 0 ) {
    printf("  find next successfully! resultbuf: %s\n",buf);
  }
  else if(rc == -1){
   printf("  find failed!\n");
  }
  
  return 0;
}

int test_hyfile_findclose(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  char buf[100];
  
  printf("testing hyfile_findclose......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"");
  
  fd = hyportLibrary->file_findfirst(hyportLibrary , tmpAbsolutePath, buf);
  printf("  resultbuf: %s\n",buf);
  if(fd==-1){
    Hytest_setErrMsg(hyportLibrary, "Can not find file.(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_findnext(hyportLibrary, fd, buf);
  if(rc == 0 ) {
    printf("  find next successfully! resultbuf: %s\n",buf);
  }
  else if(rc == -1){
   printf("  find failed!\n");
  }
  
  hyportLibrary->file_findclose(hyportLibrary , fd);
  return 0;
}

int test_hyfile_mkdir(struct HyPortLibrary *hyportLibrary)
{
  IDATA rc;
  char tmpAbsolutePath[255];
  char utf8Dir[10];
  int utf8len=0;
  
  utf8Dir[0]='d';
  utf8Dir[1]='i';
  utf8Dir[2]='r';
  utf8len = encodeUTF8CharN (0x3400, &utf8Dir[3], 7);
  utf8Dir[utf8len+3]='\0';
  
  printf("testing hyfile_mkdir......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,utf8Dir);
  
  rc = hyportLibrary->file_mkdir(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_mkdir failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  if (hyportLibrary->file_attr(hyportLibrary, tmpAbsolutePath) != HyIsDir) {
    Hytest_setErrMsg(hyportLibrary, "hytest.dir.tmp has incorrect type(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlinkdir(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlinkdir failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_unlinkdir(struct HyPortLibrary *hyportLibrary)
{
  IDATA rc;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_unlinkdir......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.dir.tmp");
  rc = hyportLibrary->file_mkdir(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_mkdir failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  if (hyportLibrary->file_attr(hyportLibrary, tmpAbsolutePath) != HyIsDir) {
    Hytest_setErrMsg(hyportLibrary, "hytest.dir.tmp has incorrect type(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlinkdir(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlinkdir failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_lastmod(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  I_64 time;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_lastmod......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  time = hyportLibrary->file_lastmod (hyportLibrary, tmpAbsolutePath);  
  if (time == -1) {
    Hytest_setErrMsg(hyportLibrary, "fail to get last modify time(%s)\n",HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_vprintf(struct HyPortLibrary *hyportLibrary)
{
  
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  
  printf("testing hyfile_printf......\n");
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (fd==-1) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytest.tmp%s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  forVprintfTest(hyportLibrary,fd,"%09d",9);
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s(%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

void forVprintfTest(struct HyPortLibrary *hyportLibrary,IDATA fd,char* format,...)
{
  va_list args;
  va_start (args, format);
  hyportLibrary->file_vprintf (hyportLibrary, fd, format, args);
  va_end (args);
}
