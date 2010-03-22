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

#define CDEV_CURRENT_FUNCTION _comment_
/**
 * @file
 * @ingroup Port
 * @brief file
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>
#include "hyport.h"
#include "utf8decode.h"

#define CDEV_CURRENT_FUNCTION hybuf_write_text
/**
* Output the buffer onto the another buffer as text. The in buffer is a UTF8-encoded array of chars.
* It is converted to the appropriate platform encoding.
*
* @param[in] portLibrary The port library
* @param[in] buf buffer of text to be converted.
* @param[in] nbytes size of buffer of text to be converted.
*
* @return buffer of converted to the appropriate platform encoding text.
*/
char *VMCALL
hybuf_write_text (struct HyPortLibrary * portLibrary,
                  const char *buf, IDATA nbytes)
{
    IDATA i;
    int newlines = 0, highchars = 0;
    char *newBuf = NULL;
    IDATA newLen;
    char *outBuf = (char*)buf;

    /* scan the buffer for any characters which need to be converted */
    for (i = 0; i < nbytes; i++)
    {
        if (outBuf[i] == '\n')
        {
            newlines += 1;
        }
        else if ((U_8) outBuf[i] & 0x80)
        {
            highchars += 1;
        }
    }
    newlines = 0;
    /* if there are any non-ASCII chars, convert to Unicode and then to the local code page */
    if (highchars)
    {
        U_16 *wBuf;
        newLen = (nbytes + newlines) * 2;
        wBuf = portLibrary->mem_allocate_memory (portLibrary, newLen);
        if (wBuf)
        {
            U_8 *in = (U_8 *) outBuf;
            U_8 *end = in + nbytes;
            U_16 *out = wBuf;

            while (in < end)
            {
                if (*in == '\n')
                {
                    *out++ = (U_16) '\r';
                    *out++ = (U_16) '\n';
                    in += 1;
                }
                else
                {
                    U_32 numberU8Consumed =
                        decodeUTF8CharN (in, out++, end - in);
                    if (numberU8Consumed == 0)
                    {
                        break;
                    }
                    in += numberU8Consumed;
                }
            }
            /* in will be NULL if an error occurred */
            if (in)
            {
                UINT codePage = GetConsoleOutputCP ();
                IDATA wLen = out - wBuf;
                IDATA mbLen =
                    WideCharToMultiByte (codePage, 0, wBuf, wLen, NULL, 0, NULL,
                    NULL);
                if (mbLen > 0)
                {
                    newBuf = portLibrary->mem_allocate_memory (portLibrary, mbLen + 1);
                    /* if we couldn't allocate the buffer, just output the data the way it was */
                    if (newBuf)
                    {
                        WideCharToMultiByte (codePage, 0, wBuf, wLen, newBuf,
                            mbLen, NULL, NULL);
                        outBuf = newBuf;
                        nbytes = mbLen;
                        newBuf[nbytes] = '\0';
                        newBuf = NULL;
                    }
                }
            }
                portLibrary->mem_free_memory (portLibrary, wBuf);
        }
    }
    else if (newlines)
    {
        /* change any LFs to CRLFs */
        newLen = nbytes + newlines;
        newBuf = portLibrary->mem_allocate_memory (portLibrary, newLen + 1);
        /* if we couldn't allocate the buffer, just output the data the way it was */
        if (newBuf)
        {
            char *cursor = newBuf;
            for (i = 0; i < nbytes; i++)
            {
                if (outBuf[i] == '\n')
                    *cursor++ = '\r';
                *cursor++ = outBuf[i];
            }
            if (outBuf != buf)
            {
                portLibrary->mem_free_memory (portLibrary, outBuf);
            }
            outBuf = newBuf;
            nbytes = newLen;
            outBuf[nbytes] = '\0';

        }
    }
    if (outBuf == buf) {
        outBuf = portLibrary->mem_allocate_memory (portLibrary, nbytes + 1);
        memcpy((void*)outBuf, (const void*)buf, nbytes);
        outBuf[nbytes] = '\0';
    }
    return outBuf;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_read_text
/**
 * Read a line of text from the file into buf.  Text is converted from the platform file encoding to UTF8.
 * This is mostly equivalent to fgets in standard C.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd File descriptor.
 * @param[in,out] buf Buffer for read in text.
 * @param[in] nbytes Size of buffer.
 *
 * @return buf on success, NULL on failure.
 */
char *VMCALL
hyfile_read_text (struct HyPortLibrary *portLibrary, IDATA fd, char *buf,
		  IDATA nbytes)
{
  char temp[64];
  IDATA count, i;
  char *cursor = buf;

  if (nbytes <= 0)
    {
      return 0;
    }

  /* discount 1 for the trailing NUL */
  nbytes -= 1;

  while (nbytes)
    {
      count = sizeof (temp) > nbytes ? nbytes : sizeof (temp);
      count = portLibrary->file_read (portLibrary, fd, temp, count);

      /* ignore translation for now */
      if (count < 0)
	{
	  if (cursor == buf)
	    {
	      return NULL;
	    }
	  else
	    {
	      break;
	    }
	}

      for (i = 0; i < count; i++)
	{
	  char c = temp[i];

	  if (c == '\r')
	    {			/* EOL */
	      /* is this a bare CR, or part of a CRLF pair? */
	      portLibrary->file_seek (portLibrary, fd, i - count + 1,
				      HySeekCur);
	      count = portLibrary->file_read (portLibrary, fd, temp, 1);
	      if (count && temp[0] == '\n')
		{
		  /* matched CRLF pair */
		  *cursor++ = '\n';
		}
	      else
		{
		  /* this was a bare CR -- back up */
		  *cursor++ = '\r';
		  portLibrary->file_seek (portLibrary, fd, -1, HySeekCur);
		}
	      *cursor = '\0';
	      return buf;
	    }
	  else if (c == '\n')
	    {			/* this can only be a bare LF */
	      portLibrary->file_seek (portLibrary, fd, i - count + 1,
				      HySeekCur);
	      *cursor++ = '\n';
	      *cursor = '\0';
	      return buf;
	    }
	  else
	    {
	      *cursor++ = c;
	    }

	}
      nbytes -= count;
    }

  *cursor = '\0';
  return buf;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_write_text
/**
 * Output the buffer onto the stream as text. The buffer is a UTF8-encoded array of chars.
 * It is converted to the appropriate platform encoding.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd the file descriptor.
 * @param[in] buf buffer of text to be output.
 * @param[in] nbytes size of buffer of text to be output.
 *
 * @return 0 on success, negative error code on failure.
 */
IDATA VMCALL
hyfile_write_text (struct HyPortLibrary * portLibrary, IDATA fd,
		   const char *buf, IDATA nbytes)
{
  IDATA result;
  IDATA i;
  int newlines = 0, highchars = 0;
  char stackBuf[512];
  char *newBuf = stackBuf;
  IDATA newLen;

  /* scan the buffer for any characters which need to be converted */
  for (i = 0; i < nbytes; i++)
    {
      if (buf[i] == '\n')
	{
	  newlines += 1;
	}
      else if ((U_8) buf[i] & 0x80)
	{
	  highchars += 1;
	}
    }

  /* if there are any non-ASCII chars, convert to Unicode and then to the local code page */
  if (highchars)
    {
      U_16 wStackBuf[512];
      U_16 *wBuf = wStackBuf;
      newLen = (nbytes + newlines) * 2;
      if (newLen > sizeof (wStackBuf))
	{
	  wBuf = portLibrary->mem_allocate_memory (portLibrary, newLen);
	}
      if (wBuf)
	{
	  U_8 *in = (U_8 *) buf;
	  U_8 *end = in + nbytes;
	  U_16 *out = wBuf;

	  while (in < end)
	    {
	      if (*in == '\n')
		{
		  *out++ = (U_16) '\r';
		  *out++ = (U_16) '\n';
		  in += 1;
		}
	      else
		{
		  U_32 numberU8Consumed =
		    decodeUTF8CharN (in, out++, end - in);
		  if (numberU8Consumed == 0)
		    {
		      break;
		    }
		  in += numberU8Consumed;
		}
	    }
	  /* in will be NULL if an error occurred */
	  if (in)
	    {
	      UINT codePage = GetConsoleOutputCP ();
	      IDATA wLen = out - wBuf;
	      IDATA mbLen =
		WideCharToMultiByte (codePage, 0, wBuf, wLen, NULL, 0, NULL,
				     NULL);
	      if (mbLen > 0)
		{
		  if (mbLen > sizeof (stackBuf))
		    {
		      newBuf =
			portLibrary->mem_allocate_memory (portLibrary, mbLen);
		      /* if we couldn't allocate the buffer, just output the data the way it was */
		    }
		  if (newBuf)
		    {
		      WideCharToMultiByte (codePage, 0, wBuf, wLen, newBuf,
					   mbLen, NULL, NULL);
		      buf = newBuf;
		      nbytes = mbLen;
		    }
		}
	    }
	  if (wBuf != wStackBuf)
	    {
	      portLibrary->mem_free_memory (portLibrary, wBuf);
	    }
	}
    }
  else if (newlines)
    {
      /* change any LFs to CRLFs */
      newLen = nbytes + newlines;
      if (newLen > sizeof (stackBuf))
	{
	  newBuf = portLibrary->mem_allocate_memory (portLibrary, newLen);
	  /* if we couldn't allocate the buffer, just output the data the way it was */
	}
      if (newBuf)
	{
	  char *cursor = newBuf;
	  for (i = 0; i < nbytes; i++)
	    {
	      if (buf[i] == '\n')
		*cursor++ = '\r';
	      *cursor++ = buf[i];
	    }
	  buf = newBuf;
	  nbytes = newLen;
	}
    }

  result = portLibrary->file_write (portLibrary, fd, (void *) buf, nbytes);

  if (newBuf != stackBuf && newBuf != NULL)
    {
      portLibrary->mem_free_memory (portLibrary, newBuf);
    }

  return (result == nbytes) ? 0 : result;
}

#undef CDEV_CURRENT_FUNCTION
