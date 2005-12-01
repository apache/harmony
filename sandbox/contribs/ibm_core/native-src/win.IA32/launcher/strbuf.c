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

#include <string.h>

#include "libhlp.h"

#define MIN_GROWTH 128

HyStringBuffer *
strBufferCat (struct HyPortLibrary * portLibrary, HyStringBuffer * buffer,
        const char *string)
{
  UDATA len = strlen (string);

  buffer = strBufferEnsure (portLibrary, buffer, len);
  if (buffer)
    {
      strcat (buffer->data, string);
      buffer->remaining -= len;
    }

  return buffer;
}

HyStringBuffer *
strBufferEnsure (struct HyPortLibrary * portLibrary, HyStringBuffer * buffer,
     UDATA len)
{

  if (buffer == NULL)
    {
      PORT_ACCESS_FROM_PORT (portLibrary);
      UDATA newSize = len > MIN_GROWTH ? len : MIN_GROWTH;
      buffer = hymem_allocate_memory (newSize + 1 + sizeof (UDATA));  /* 1 for null terminator */
      if (buffer != NULL)
        {
          buffer->remaining = newSize;
          buffer->data[0] = '\0';
        }
      return buffer;
    }

  if (len > buffer->remaining)
    {
      PORT_ACCESS_FROM_PORT (portLibrary);
      UDATA newSize = len > MIN_GROWTH ? len : MIN_GROWTH;
      HyStringBuffer *new =
        hymem_allocate_memory (strlen (buffer->data) + newSize +
                  sizeof (UDATA) + 1);
      if (new)
        {
          new->remaining = newSize;
          strcpy (new->data, buffer->data);
        }
      hymem_free_memory (buffer);
      return new;
    }

  return buffer;
}

HyStringBuffer *
strBufferPrepend (struct HyPortLibrary * portLibrary, HyStringBuffer * buffer,
      char *string)
{
  UDATA len = strlen (string);

  buffer = strBufferEnsure (portLibrary, buffer, len);
  if (buffer)
    {
      memmove (buffer->data + len, buffer->data, strlen (buffer->data) + 1);
      strncpy (buffer->data, string, len);
      buffer->remaining -= len;
    }

  return buffer;
}

char *
strBufferData (HyStringBuffer * buffer)
{
  return buffer ? buffer->data : NULL;
}
