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

void cleanup(HyPortLibrary hyportLibrary);
void generateAbsoluteFilePath(struct HyPortLibrary *hyportLibrary,char *dist , char *fileName);
int test_hybuf_write_text(struct HyPortLibrary *hyportLibrary);
int test_hyfile_write_text(struct HyPortLibrary *hyportLibrary);
int test_hyfile_read_text(struct HyPortLibrary *hyportLibrary);



int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  char* buf=NULL;
  int ret;
  
  printf("hyfiletext:\n");
  
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }
  
  
  Hytest_init(&hyportLibrary, "Portlib.Hyfiletext");
  Hytest_func(&hyportLibrary, test_hybuf_write_text, "hybuf_write_text");
  Hytest_func(&hyportLibrary, test_hyfile_write_text, "hyfile_write_text");
  Hytest_func(&hyportLibrary, test_hyfile_read_text, "hyfile_read_text");
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
  hyportLibrary.file_unlink(&hyportLibrary, "hytexttest.tmp");
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

int test_hybuf_write_text(struct HyPortLibrary *hyportLibrary)
{
  char inbuf[] = {0xE2,0x89,0xA0};
  char* outbuf = NULL;
  outbuf = hyportLibrary->buf_write_text(hyportLibrary,inbuf,3);
  if(!outbuf)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to run function buf_write_text %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  printf("  %s",outbuf);
  return 0;
}

int test_hyfile_write_text(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char tmpAbsolutePath[255];
  char* outbuf = NULL;
  char inbuf[] = {0xE2,0x89,0xA0};
  
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytexttest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (!fd) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytexttest.tmp%s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_write_text(hyportLibrary, fd, inbuf ,3);
  if(rc != 0)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to write text to hytexttest.tmp %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;	
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}

int test_hyfile_read_text(struct HyPortLibrary *hyportLibrary)
{
  IDATA fd;
  IDATA rc;
  char* out2 = NULL;
  char outbuf[10];
  char inbuf[] = {0xE2,0x89,0xA0};
  char tmpAbsolutePath[255];
  
  generateAbsoluteFilePath(hyportLibrary,tmpAbsolutePath,"hytexttest.tmp");
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenCreate | HyOpenWrite | HyOpenTruncate,
                               0600);
  printf("  fd = %d\n", fd);
  if (!fd) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open file hytexttest.tmp%s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_write_text(hyportLibrary, fd, inbuf ,3);
  if(rc != 0)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to write text to hytexttest.tmp %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;	
  }
  
  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  fd = hyportLibrary->file_open(hyportLibrary, tmpAbsolutePath,
                               HyOpenRead, 0000);
  printf("  fd = %d\n", fd);
  if (!fd) {
    Hytest_setErrMsg(hyportLibrary, "Failed to open hytest.tmp2 for read %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
 
  out2 = hyportLibrary->file_read_text(hyportLibrary, fd, outbuf, 10);
  if(!out2)
  {
    Hytest_setErrMsg(hyportLibrary, "Failed to read text from hytexttest.tmp %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;	
  }
  else
  {
    printf("  %s\n",outbuf);
  }

  rc = hyportLibrary->file_close(hyportLibrary, fd);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_close failed %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  
  rc = hyportLibrary->file_unlink(hyportLibrary, tmpAbsolutePath);
  if (rc != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyfile_unlink failed %s (%s)\n",
            hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    cleanup(*hyportLibrary);
    return -1;
  }
  return 0;
}
