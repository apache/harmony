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

#define CDEV_CURRENT_FUNCTION _comment_
/**
 * @internal 
 * @file
 * @ingroup Port
 * @brief Error Handling
 *
 * Helper utilities for @ref hyerror.c.  This file reduces the amount of code duplication as formatting of messages
 * from the OS is the only part of error handling that can not be handled in a generic manner.
 *
 * These functions are not accessible via the port library function table.
 */
#undef CDEV_CURRENT_FUNCTION

#include <string.h>
#include "portpriv.h"
#include "portnls.h"
#include "hyportptb.h"

#define CDEV_CURRENT_FUNCTION errorMessage
/**
 * @internal
 * @brief Error Handling
 *
 * Given an error code save the OS error message to the ptBuffers and return
 * a reference to the saved message.
 *
 * @param[in] portLibrary The port library
 * @param[in] errorCode The platform specific error code to look up.
 *
 * @note By the time this function is called it is known that ptBuffers are
 *       not NULL.  It is possible, however, that the
 *       buffer to hold the error message has not yet been allocated.
 *
 * @note Buffer is managed by the port library, do not free
 */
const char *VMCALL
errorMessage (struct HyPortLibrary *portLibrary, I_32 errorCode)
{
  PortlibPTBuffers_t ptBuffers;
  char *message;
  int rc = 0, i, out;
  WCHAR ubuffer[HYERROR_DEFAULT_BUFFER_SIZE];
#if defined(UNICODE)
#define buffer ubuffer
#else
  char buffer[HYERROR_DEFAULT_BUFFER_SIZE];
#endif

  ptBuffers = hyport_tls_peek (portLibrary);
  if (0 == ptBuffers->errorMessageBufferSize)
    {
      ptBuffers->errorMessageBuffer =
	portLibrary->mem_allocate_memory (portLibrary,
					  HYERROR_DEFAULT_BUFFER_SIZE);
      if (NULL == ptBuffers->errorMessageBuffer)
	{
	  return "";
	}
      ptBuffers->errorMessageBufferSize = HYERROR_DEFAULT_BUFFER_SIZE;
    }
  message = ptBuffers->errorMessageBuffer;

  rc =
    FormatMessage (FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
		   NULL, errorCode, 0, (LPTSTR) buffer,
		   HYERROR_DEFAULT_BUFFER_SIZE, NULL);
  if (rc == 0)
    {
      const char *format;
      format = portLibrary->nls_lookup_message (portLibrary,
						HYNLS_DO_NOT_PRINT_MESSAGE_TAG
						| HYNLS_DO_NOT_APPEND_NEWLINE,
						HYNLS_PORT_ERROR_OPERATION_FAILED,
						"Operation Failed: %d (%s failed: %d)");
      portLibrary->str_printf (portLibrary, message,
			       ptBuffers->errorMessageBufferSize, format,
			       errorCode, "FormatMessage", GetLastError ());
      message[ptBuffers->errorMessageBufferSize - 1] = '\0';
      return message;
    }
#if !defined(UNICODE)
  rc =
    MultiByteToWideChar (CP_ACP, MB_PRECOMPOSED, buffer, -1, ubuffer,
			 HYERROR_DEFAULT_BUFFER_SIZE);
  if (rc == 0)
    {
      const char *format;
      format = portLibrary->nls_lookup_message (portLibrary,
						HYNLS_DO_NOT_PRINT_MESSAGE_TAG
						| HYNLS_DO_NOT_APPEND_NEWLINE,
						HYNLS_PORT_ERROR_OPERATION_FAILED,
						"Operation Failed: %d (%s failed: %d)");
      portLibrary->str_printf (portLibrary, message,
			       ptBuffers->errorMessageBufferSize, format,
			       errorCode, "MultiByteToWideChar",
			       GetLastError ());
      message[ptBuffers->errorMessageBufferSize - 1] = '\0';
      return message;
    }
#else
#undef buffer
#endif

  out = sprintf (message, "(%d) ", errorCode);
  for (i = 0; i < rc; i++)
    {
      UDATA ch = ubuffer[i];
      if (ch == '\r')
	{			/* Strip CR */
	  continue;
	}
      if (ch == '\n')
	{			/* Convert LF to space */
	  ch = ' ';
	}
      if (ch < 0x80)
	{
	  if ((out + 2) >= HYERROR_DEFAULT_BUFFER_SIZE)
	    break;
	  message[out++] = (char) ch;
	}
      else if (ch < 0x800)
	{
	  if ((out + 3) >= HYERROR_DEFAULT_BUFFER_SIZE)
	    break;
	  message[out++] = (char) (0x80 | (ch & 0x3f));
	  message[out++] = (char) (0xc0 | (ch >> 6));
	}
      else
	{
	  if ((out + 4) >= HYERROR_DEFAULT_BUFFER_SIZE)
	    break;
	  message[out++] = (char) (0x80 | (ch & 0x3f));
	  message[out++] = (char) (0x80 | ((ch >> 6) & 0x3f));
	  message[out++] = (char) (0xe0 | (ch >> 12));
	}
    }
  message[out] = '\0';

  /* There may be extra spaces at the end of the message, due to stripping of the LF
   * it is replaced by a space.  Multi-line OS messages are thus one long continuous line for us
   */
  while (iswspace (message[out]) || (message[out] == '\0'))
    {
      message[out--] = '\0';
    }
  return message;
}

#undef CDEV_CURRENT_FUNCTION
