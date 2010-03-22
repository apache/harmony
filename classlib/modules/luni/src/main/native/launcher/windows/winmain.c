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
#include "jni.h"
#include "hyport.h"
//#include "libhlp.h"
#ifdef HY_NO_THR
#include "main_hlp.h"
#endif /* HY_NO_THR */

/* external prototypes */
UDATA VMCALL gpProtectedMain PROTOTYPE ((void *arg));
#ifdef HY_NO_THR
extern int main_addVMDirToPath PROTOTYPE((int argc, char **argv, char **envp));
#endif /* HY_NO_THR */

char **getArgvCmdLine
#ifndef HY_NO_THR
PROTOTYPE ((HyPortLibrary * portLibrary, LPTSTR buffer, int *finalArgc));
#else /* HY_NO_THR */
PROTOTYPE ((LPTSTR buffer, int *finalArgc));
#endif /* HY_NO_THR */
int WINAPI WinMain
PROTOTYPE ((HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine,
      int nShowCmd));
#ifndef HY_NO_THR
void freeArgvCmdLine PROTOTYPE ((HyPortLibrary * portLibrary, char **argv));
#else /* HY_NO_THR */
void freeArgvCmdLine PROTOTYPE ((char **argv));
#endif /* HY_NO_THR */

struct haCmdlineOptions
{
  int argc;
  char **argv;
  char **envp;
  HyPortLibrary *portLibrary;
};

#ifdef HY_NO_THR
typedef I_32 (PVMCALL hyport_init_library_type) (struct HyPortLibrary *portLibrary,
		struct HyPortLibraryVersion *version, 
		UDATA size);


#endif /* HY_NO_THR */
int WINAPI
WinMain (HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine,
   int nShowCmd)
{
  int argc = 0, rc;
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  struct haCmdlineOptions options;
  char **argv;
#ifdef HY_NO_THR
  UDATA portLibDescriptor;
  hyport_init_library_type port_init_library_func;
  
  argv = getArgvCmdLine (GetCommandLine (), &argc);
  if (argv == NULL) {
	  rc = -1;
	  goto cleanup;
  }

  /* determine which VM directory to use and add it to the path */
  rc = main_addVMDirToPath(argc, argv, NULL);
  if ( rc != 0 ) {
	  goto cleanup;
  }

  if ( 0 != main_open_port_library(&portLibDescriptor) ) {
	  fprintf( stderr, "failed to open hyprt library.\n" );
	  rc = -1;
	  goto cleanup;
  }

  if ( 0 != main_lookup_name( portLibDescriptor, "hyport_init_library", (UDATA *)&port_init_library_func) ) {
	  fprintf( stderr, "failed to find hyport_init_library function in hyprt library\n" );
	  rc = -1;
	  goto cleanup;
  }
#endif /* HY_NO_THR */

  /* Use portlibrary version which we compiled against, and have allocated space
   * for on the stack.  This version may be different from the one in the linked DLL.
   */

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
#ifndef HY_NO_THR
  rc =
    hyport_init_library (&hyportLibrary, &portLibraryVersion,
       sizeof (HyPortLibrary));
  if (0 != rc)
    {
      return -1;
    }

  argv = getArgvCmdLine (&hyportLibrary, GetCommandLine (), &argc);
  if (argv)
    {
      options.argc = argc;
      options.argv = argv;
      options.envp = NULL;
      options.portLibrary = &hyportLibrary;
      rc =
        hyportLibrary.gp_protect (&hyportLibrary, gpProtectedMain, &options);
      freeArgvCmdLine (&hyportLibrary, argv);
      hyportLibrary.port_shutdown_library (&hyportLibrary);
      return rc;
    }
  else
    {
      return -1;
    }
#else /* HY_NO_THR */
  rc = port_init_library_func (&hyportLibrary, &portLibraryVersion, sizeof (HyPortLibrary));
  if (0 != rc) {
	  goto cleanup;
  }
	  
  options.argc = argc;
  options.argv = argv;
  options.envp = NULL;
  options.portLibrary = &hyportLibrary;
  rc = hyportLibrary.gp_protect (&hyportLibrary, gpProtectedMain, &options);
  hyportLibrary.port_shutdown_library (&hyportLibrary);

cleanup:
	if (argv) {
		freeArgvCmdLine(argv);
	}
	return rc;
#endif /* HY_NO_THR */
}

/*
 *  Takes the command line (1 string) and breaks it into an
 *  argc, argv[] style list.
 *  Understands LFNs and strips quotes off the exe name.
 *  also converts the string to ASCII.
 */
char **
#ifndef HY_NO_THR
getArgvCmdLine (HyPortLibrary * portLibrary, LPTSTR buffer, int *finalArgc)
#else /* HY_NO_THR */
getArgvCmdLine (LPTSTR buffer, int *finalArgc)
#endif /* HY_NO_THR */
{

#define QUOTE_CHAR  34

  int argc = 0, currentArg, i, asciiLen;
  char *asciiCmdLine;
  char **argv;
#ifndef HY_NO_THR
  PORT_ACCESS_FROM_PORT (portLibrary);
#endif /* ! HY_NO_THR */

  asciiCmdLine = buffer;

  /* determine an upper bound on the # of args by counting spaces and tabs */
  argc = 2;
  asciiLen = strlen (buffer);
  for (i = 0; i < asciiLen; i++)
    {
      if (asciiCmdLine[i] == ' ' || asciiCmdLine[i] == '\t')
        argc++;
    }

  /* allocate the buffer for the args */
#ifndef HY_NO_THR
  argv = hymem_allocate_memory (argc * sizeof (char *));
#else /* HY_NO_THR */
  argv = main_mem_allocate_memory (argc * sizeof (char *));
#endif /* HY_NO_THR */
  if (!argv)
    return NULL;

  /* now fill in the argv array */
  currentArg = 0;
  if (QUOTE_CHAR == *asciiCmdLine)
    {       /* we have a quoted name. */
      argv[currentArg++] = ++asciiCmdLine;  /* move past the quote */
      while (QUOTE_CHAR != *asciiCmdLine)
        asciiCmdLine++;
      if (*asciiCmdLine)
        *asciiCmdLine++ = '\0'; /* past close quote, slam the close quote and advance */
    }

  /* Skip whitespace */
  while (*asciiCmdLine == ' ' || *asciiCmdLine == '\t'
    || *asciiCmdLine == '\r')
    asciiCmdLine++;

  /* Split up args */
  while (*asciiCmdLine)
    {
      if (QUOTE_CHAR == *asciiCmdLine)
        {
          /* Parse a quoted arg */
          argv[currentArg++] = ++asciiCmdLine;
          while (*asciiCmdLine && *asciiCmdLine != QUOTE_CHAR)
            {
              asciiCmdLine++;
            }
        }
      else
        {
          /* Whitespace separated arg */
          argv[currentArg++] = asciiCmdLine;
          while (*asciiCmdLine && *asciiCmdLine != ' '
            && *asciiCmdLine != '\t' && *asciiCmdLine != '\r')
            {
              asciiCmdLine++;
            }
        }

      /* Null terminate */
      if (*asciiCmdLine)
        *asciiCmdLine++ = '\0';

      /* Skip whitespace */
      while (*asciiCmdLine && *asciiCmdLine == ' ' || *asciiCmdLine == '\t'
        || *asciiCmdLine == '\r')
        asciiCmdLine++;
    }
  argv[currentArg] = NULL;
  *finalArgc = currentArg;
  return argv;
}

#undef QUOTE_CHAR
void
#ifndef HY_NO_THR
freeArgvCmdLine (HyPortLibrary * portLibrary, char **argv)
#else /* HY_NO_THR */
freeArgvCmdLine (char **argv)
#endif /* HY_NO_THR */
{
#ifndef HY_NO_THR
  PORT_ACCESS_FROM_PORT (portLibrary);

  hymem_free_memory (argv);
#else /* HY_NO_THR */
  main_mem_free_memory (argv);
#endif /* HY_NO_THR */
}
