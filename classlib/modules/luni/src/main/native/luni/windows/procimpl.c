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

#include <windows.h>
#include <string.h>
#include "vmi.h"
#include "procimpl.h"
#include "harmonyglob.h"

/*Kill the Process with ID procHandle */

int
termProc (IDATA procHandle)
{
  int retVal = 0;
  retVal = TerminateProcess ((HANDLE) procHandle, 0);
  return retVal;
}

/* Wait in the current thread until the Process procHandle */
/* has finished executing          */
int
waitForProc (IDATA procHandle)
{
  DWORD procstat = 0;

  procstat = WaitForSingleObject ((HANDLE) procHandle, INFINITE);
  GetExitCodeProcess ((HANDLE) procHandle, &procstat);
  return (int) procstat;
}

/*
 *  int execProgram()
 * 
 *  does a fork/execvp to launch the program
 * 
 *  returns :
 *     0     successful
 *     1001  fork failure errno = ENOMEM
 *     1002  fork failure errno = EAGAIN
 *     1003  pipe failure errno = EMFILE
 *     1004  chdir failure errno = ENOENT
 *     -1    error, unknown
 * 
 *   TODO - fill in windows error codes 
 * 
 *   Note - there is one error code 'namespace' for execProgram
 *          please coordinate w/ other platform impls
 */int
execProgram (JNIEnv * vmthread, jobject recv,
       char *command[], int commandLength,
       char *env[], int envSize, char *dir,
       IDATA * procHandle, IDATA * inHandle, IDATA * outHandle,
       IDATA * errHandle)
{
  int retVal, envLength = 0;
  int returnCode = -1;
  HANDLE inr = NULL, inw = NULL, outr = NULL, outw = NULL, errr = NULL;
  HANDLE errw = NULL, inDup = NULL, outDup = NULL, errDup = NULL;
  STARTUPINFO sinfo;
  PROCESS_INFORMATION pinfo;
  SECURITY_ATTRIBUTES sAttrib;
  char *commandAsString = NULL, *envString = NULL;
  int i, length;
  size_t l;
  char *ptr;
  char *argi;
  char *needToBeQuoted;
  PORT_ACCESS_FROM_ENV (vmthread);

  if (!commandLength) {
    return 0;
  }
  
  ZeroMemory (&sinfo, sizeof (sinfo));
  ZeroMemory (&pinfo, sizeof (pinfo));
  ZeroMemory (&sAttrib, sizeof (sAttrib));

  /* Allow handle inheritance */
  sAttrib.bInheritHandle = 1;
  sAttrib.nLength = sizeof (sAttrib);
  sinfo.cb = sizeof (sinfo);
  sinfo.dwFlags = STARTF_USESTDHANDLES;

  /* Create the pipes to pass to the new process */
  retVal = CreatePipe (&outr, &outw, &sAttrib, 512);

  if (!retVal) { 
    goto failed;
  }

  retVal = CreatePipe (&inr, &inw, &sAttrib, 512);

  if (!retVal) {
    goto failed;
  }

  retVal = CreatePipe (&errr, &errw, &sAttrib, 512);

  if (!retVal) {
    goto failed;
  }

  /* fprintf(stdout,"fd:errw ==> %d\n",errw);fflush(stdout); */

  /* Dup Non-Inherit and close inheritable  handles */
  retVal = DuplicateHandle (GetCurrentProcess (), inw,
          GetCurrentProcess (), &inDup, 0,
          FALSE, DUPLICATE_SAME_ACCESS);

  if (!retVal) {
    goto failed;
  }

  CloseHandle (inw);
  inw = NULL;

  retVal = DuplicateHandle (GetCurrentProcess (), outr,
          GetCurrentProcess (), &outDup, 0,
          FALSE, DUPLICATE_SAME_ACCESS);

  if (!retVal) {
    goto failed;
  }

  CloseHandle (outr);
  outr = NULL;

  retVal = DuplicateHandle (GetCurrentProcess (), errr,
          GetCurrentProcess (), &errDup, 0,
          FALSE, DUPLICATE_SAME_ACCESS);

  if (!retVal) {
    goto failed;
  }

  CloseHandle (errr);
  errr = NULL;

  sinfo.hStdOutput = outw;
  sinfo.hStdError = errw;
  sinfo.hStdInput = inr;
  *inHandle = (IDATA) inDup;
  *outHandle = (IDATA) outDup;
  *errHandle = (IDATA) errDup;

  /*Build the environment block */
  if (envSize)
    {
      int i;
      char *envBldr;
      envLength = envSize + 1;  /*Length of strings + null terminators + final null terminator */

      for (i = 0; i < envSize; i++) {
        envLength += strlen (env[i]);
      }
      
      envString = (char *) jclmem_allocate_memory (env, envLength);

      if (!envString) {
        goto failed;
      }

	  envBldr = envString;

      for (i = 0; i < envSize; i++)
        {
          strcpy (envBldr, env[i]);
          envBldr += (strlen (env[i]) + 1); /* +1 for null terminator */
        }
      *envBldr = '\0';
    }

  /* Windoz needs a char* command line :-( unlike regular C exec* functions ! */
  /* Therefore we need to REbuild the line that has been sliced in java...    */
  /* Subtle : if a token embbeds a <space>, the token will be quoted (only    */
  /*          if it hasn't been quoted yet) The quote char is "               */

  /* Note (see "XXX references in the code)
     Our CDev scanner/parser does not handle '"' correctly. A workaround is to close
     the '"' with another " , embedded in a C comment. 
   */

  needToBeQuoted = (char *) jclmem_allocate_memory (env, commandLength);

  if (!needToBeQuoted) {
    goto failed;
  }

  memset (needToBeQuoted, '\0', commandLength);

  length = commandLength; /*add 1 <blank> between each token + a reserved place for the last NULL */
  for (i = commandLength; --i >= 0;)
    {
      int commandILength, j;
      char *commandStart;
      length += (commandILength = strlen (command[i]));
      /* check_for_embbeded_space */
      if (commandILength > 0)
        {
          commandStart = command[i];
          if (commandStart[0] != '"' /*"XXX */ )
            {
              for (j = 0; j < commandILength; j++)
                {
                  if (commandStart[j] == ' ')
                    {
                      needToBeQuoted[i] = '\1'; /* a random value, different from zero though */
                      length += 2;  /* two quotes are added */
                      if (commandILength > 1
                        && commandStart[commandILength - 1] == '\\'
                        && commandStart[commandILength - 2] != '\\') {
                        length++; /* need to double slash */
                      }
                      break;
                    }
                }
            }
        }     /* end of check_for_embbeded_space */
    }
  ptr = commandAsString = (char *) jclmem_allocate_memory (env, length);
  if (!commandAsString)
    {
      jclmem_free_memory (env, needToBeQuoted);
      goto failed;
    }
  for (i = 0; i < commandLength; i++)
    {
      l = strlen (argi = command[i]);
      if (needToBeQuoted[i])
        {
          (*ptr) = '"' /*"XXX */ ;
          ptr++;
        }
      memcpy (ptr, argi, l);
      ptr += l;
      if (needToBeQuoted[i])
        {
          if (l > 1 && *(ptr - 1) == '\\' && *(ptr - 2) != '\\') {
            *ptr++ = '\\';
          }

          (*ptr) = '"' /*"XXX */ ;
          ptr++;
        }
      (*ptr) = ' ';   /* put a <blank> between each token */
      ptr++;
    }
  (*(ptr - 1)) = '\0';    /*commandLength > 0 ==> valid operation */
  jclmem_free_memory (env, needToBeQuoted);

  /* If running on WinNT or Win2K, send CREATE_NO_WINDOW to console apps */
  retVal = CreateProcess (NULL, commandAsString, NULL, NULL, TRUE, GetVersion () & 0x80 ? 0 : CREATE_NO_WINDOW, /*use DEBUG_ONLY_THIS_PROCESS for smoother debugging, however */
        envString, dir, &sinfo, &pinfo);
  jclmem_free_memory (env, commandAsString);
  /* retVal is non-zero if successful */

  if (!retVal) {
    goto failed;
  }

  if (envSize) {
    jclmem_free_memory (env, envString);
  }

  *procHandle = (IDATA) pinfo.hProcess;
  /* Close Handles passed to child */
  CloseHandle (inr);
  CloseHandle (outw);
  CloseHandle (errw);
  CloseHandle (pinfo.hThread);  /*implicitly created, a leak otherwise */
  
  return 0;

failed:
  if (envSize)
    jclmem_free_memory (env, envString);
  if (outr)
    CloseHandle (outr);
  if (outw)
    CloseHandle (outw);
  if (inr)
    CloseHandle (inr);
  if (inw)
    CloseHandle (inw);
  if (errr)
    CloseHandle (errr);
  if (errw)
    CloseHandle (errw);
  if (inDup)
    CloseHandle (inDup);
  if (outDup)
    CloseHandle (outDup);
  if (errDup)
    CloseHandle (errDup);
    
  return returnCode;
}

/* Stream handling support */

/* Return the number of bytes available to be read from the */
/* pipe sHandle              */
int
getAvailable (IDATA sHandle)
{
  int retVal = 0, availBytes = 0;

  retVal = PeekNamedPipe ((HANDLE) sHandle,
        NULL, (DWORD) NULL, NULL, &availBytes, NULL);
  /* Error case returns zero */
  if (!retVal)
    {
      if (ERROR_BROKEN_PIPE == GetLastError ())
        return 0;
      else
        return -1;
    }
  return availBytes;
}

/*Close the procHandle */

int
closeProc (IDATA procHandle)
{
  return CloseHandle ((HANDLE) procHandle);
}
