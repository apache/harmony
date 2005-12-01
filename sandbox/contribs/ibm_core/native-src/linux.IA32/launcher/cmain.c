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

#include "hycomp.h"
#include "hyport.h"
//#include "libhlp.h" /* haCmdlineOptions structure moved to here */ 
#include <stdlib.h>             /* for malloc for atoe and abort */

struct haCmdlineOptions
{
  int argc;
  char **argv;
  char **envp;
  HyPortLibrary *portLibrary;
};
extern UDATA VMCALL gpProtectedMain (void *arg);

static UDATA VMCALL
genericSignalHandler (struct HyPortLibrary *portLibrary, U_32 gpType,
                      void *gpInfo, void *userData)
{
  PORT_ACCESS_FROM_PORT (portLibrary);
  U_32 category;

  hytty_printf (PORTLIB, "\nAn unhandled error (%d) has occurred.\n", gpType);

  for (category = 0; category < HYPORT_SIG_NUM_CATEGORIES; category++)
    {
      U_32 infoCount = hysig_info_count (gpInfo, category);
      U_32 infoKind, index;
      void *value;
      const char *name;

      for (index = 0; index < infoCount; index++)
        {
          infoKind = hysig_info (gpInfo, category, index, &name, &value);

          switch (infoKind)
            {
            case HYPORT_SIG_VALUE_32:
              hytty_printf (PORTLIB, "%s=%08.8x\n", name, *(U_32 *) value);
              break;
            case HYPORT_SIG_VALUE_64:
            case HYPORT_SIG_VALUE_FLOAT_64:
              hytty_printf (PORTLIB, "%s=%016.16llx\n", name,
                            *(U_64 *) value);
              break;
            case HYPORT_SIG_VALUE_STRING:
              hytty_printf (PORTLIB, "%s=%s\n", name, (const char *) value);
              break;
            case HYPORT_SIG_VALUE_ADDRESS:
              hytty_printf (PORTLIB, "%s=%p\n", name, *(void **) value);
              break;
            }
        }
    }

  abort ();

  /* UNREACHABLE */
  return 0;
}

static UDATA VMCALL
signalProtectedMain (HyPortLibrary * portLibrary, void *arg)
{
  return gpProtectedMain (arg);
}

int
main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  struct haCmdlineOptions options;
  int rc = 257;
  UDATA result;

  /* Use portlibrary version which we compiled against, and have allocated space
   * for on the stack.  This version may be different from the one in the linked DLL.
   */
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 ==
      hyport_init_library (&hyportLibrary, &portLibraryVersion,
                           sizeof (HyPortLibrary)))
    {
      options.argc = argc;
      options.argv = argv;
      options.envp = envp;
      options.portLibrary = &hyportLibrary;

      if (hyportLibrary.sig_protect (&hyportLibrary,
                                     signalProtectedMain, &options,
                                     genericSignalHandler, NULL,
                                     HYPORT_SIG_FLAG_SIGALLSYNC,
                                     &result) == 0)
        {
          rc = result;
        }

      hyportLibrary.port_shutdown_library (&hyportLibrary);
    }

  return rc;
}
