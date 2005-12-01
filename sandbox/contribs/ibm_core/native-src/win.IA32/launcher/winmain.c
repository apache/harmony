/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <windows.h>
#include "jni.h"
#include "hyport.h"
//#include "libhlp.h"

/* external prototypes */
UDATA VMCALL gpProtectedMain PROTOTYPE ((void *arg));

char **getArgvCmdLine
PROTOTYPE ((HyPortLibrary * portLibrary, LPTSTR buffer, int *finalArgc));
int WINAPI WinMain
PROTOTYPE ((HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine,
      int nShowCmd));
void freeArgvCmdLine PROTOTYPE ((HyPortLibrary * portLibrary, char **argv));

struct haCmdlineOptions
{
  int argc;
  char **argv;
  char **envp;
  HyPortLibrary *portLibrary;
};

int WINAPI
WinMain (HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine,
   int nShowCmd)
{
  int argc = 0, rc;
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  struct haCmdlineOptions options;
  char **argv;

  /* Use portlibrary version which we compiled against, and have allocated space
   * for on the stack.  This version may be different from the one in the linked DLL.
   */

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
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
}

/*
 *  Takes the command line (1 string) and breaks it into an
 *  argc, argv[] style list.
 *  Understands LFNs and strips quotes off the exe name.
 *  also converts the string to ASCII.
 */
char **
getArgvCmdLine (HyPortLibrary * portLibrary, LPTSTR buffer, int *finalArgc)
{

#define QUOTE_CHAR  34

  int argc = 0, currentArg, i, asciiLen;
  char *asciiCmdLine;
  char **argv;
  PORT_ACCESS_FROM_PORT (portLibrary);

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
  argv = hymem_allocate_memory (argc * sizeof (char *));
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
freeArgvCmdLine (HyPortLibrary * portLibrary, char **argv)
{
  PORT_ACCESS_FROM_PORT (portLibrary);

  hymem_free_memory (argv);
}
