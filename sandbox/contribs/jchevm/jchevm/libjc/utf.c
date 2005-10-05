
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id: utf.c,v 1.2 2004/07/18 02:19:12 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * Decode a UTF-8 byte array. If 'buf' is not NULL, write the decoded
 * characters into the buffer, which must be large enough.
 *
 * Returns the length in characters of the decoded string, or -1 if
 * the UTF-8 bytes are not valid.
 */
jint
_jc_utf_decode(const u_char *utf, jint ulen, jchar *buf)
{
	jint clen;
	jint i;

	for (clen = i = 0; i < ulen; clen++) {
		if ((utf[i] & 0x80) == 0) {
			if (utf[i] == 0)
				return -1;
			if (buf != NULL)
				buf[clen] = utf[i];
			i++;
		} else if ((utf[i] & 0xe0) == 0xc0) {
			if (i + 1 >= ulen)
				return -1;
			if ((utf[i + 1] & 0xc0) != 0x80)
				return -1;
			if (buf != NULL) {
				buf[clen] = ((utf[i] & 0x1f) << 6)
				    | (utf[i + 1] & 0x3f);
			}
			i += 2;
		} else if ((utf[i] & 0xf0) == 0xe0) {
			if (i + 2 >= ulen)
				return -1;
			if ((utf[i + 1] & 0xc0) != 0x80)
				return -1;
			if ((utf[i + 2] & 0xc0) != 0x80)
				return -1;
			if (buf != NULL) {
				buf[clen] = ((utf[i] & 0x0f) << 12)
				    | ((utf[i + 1] & 0x3f) << 6)
				    | (utf[i + 2] & 0x3f);
			}
			i += 3;
		} else
			return -1;
	}
	return clen;
}

/*
 * Encode a character array into UTF-8. If 'buf' is not NULL, write
 * the encoded bytes into the buffer, which must be large enough.
 * The buffer is not NUL terminated.
 *
 * Returns the length in bytes of the encoded string.
 */
size_t
_jc_utf_encode(const jchar *chars, jint clen, u_char *buf)
{
	size_t blen;
	jint i;

	for (blen = i = 0; i < clen; i++) {
		const jchar ch = chars[i];

		if (ch >= 0x01 && ch <= 0x7f) {
			if (buf != NULL)
				buf[blen] = (u_char)ch;
			blen++;
		} else if ((ch & 0xf800) == 0) {
			if (buf != NULL) {
				buf[blen] = 0xc0 | ((ch >> 6) & 0x1f);
				buf[blen + 1] = 0x80 | (ch & 0x3f);
			}
			blen += 2;
		} else {
			if (buf != NULL) {
				buf[blen] = 0xe0 | ((ch >> 12) & 0x0f);
				buf[blen + 1] = 0x80 | ((ch >> 6) & 0x3f);
				buf[blen + 2] = 0x80 | (ch & 0x3f);
			}
			blen += 3;
		}
	}
	return blen;
}

