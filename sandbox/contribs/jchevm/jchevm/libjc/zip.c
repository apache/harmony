
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
 * $Id$
 */

#include "libjc.h"

/* Internal functions */
static int	_jc_zip_scan(_jc_zip *zip, ...);
static jint	_jc_zip_unstore(_jc_env *env, _jc_zip *zip,
			_jc_zip_entry *zent, void *data);
static jint	_jc_zip_inflate(_jc_env *env, _jc_zip *zip,
			_jc_zip_entry *zent, void *data);
static void	*_jc_zip_alloc(void *cookie, unsigned int num,
			unsigned int size);
static void	_jc_zip_free(void *cookie, void *mem);
static int	_jc_zip_entry_cmp(const void *v1, const void *v2);

/*
 * Open a ZIP file.
 *
 * If there is an error, an exception is stored and NULL is returned.
 */
_jc_zip *
_jc_zip_open(_jc_env *env, const char *path)
{
	jshort num_entries;
	jint signature;
	jint offset;
	_jc_zip *zip;
	int i;

	/* Create new zip structure */
	if ((zip = _jc_vm_zalloc(env, sizeof(*zip))) == NULL)
		goto fail;
	zip->fd = -1;
	if ((zip->path = _jc_vm_strdup(env, path)) == NULL)
		goto fail;

	/* Open file */
	if ((zip->fd = open(path, O_RDONLY)) == -1) {
		_JC_EX_STORE(env, IOException, "can't open ZIP file `%s': %s",
		    zip->path, strerror(errno));
		goto fail;
	}
	(void)fcntl(zip->fd, F_SETFD, 1);

	/* Seek to start of central directory meta-info at end of file */
	if (lseek(zip->fd, (off_t)-ZIP_DIRECTORY_INFO_LEN, SEEK_END) == -1) {
		_JC_EX_STORE(env, IOException, "can't seek to directory meta-"
		    "info in ZIP file `%s': %s", zip->path, strerror(errno));
		goto fail;
	}

	/* Read central directory meta-info */
	if (_jc_zip_scan(zip, -4, &signature, 6, NULL,
	    -2, &num_entries, 4, NULL, -4, &offset, 0) != JNI_OK) {
		_JC_EX_STORE(env, IOException, "can't read directory meta-info"
		    " in ZIP file `%s': %s", zip->path, strerror(errno));
		goto fail;
	}

	/* Check signature */
	if (signature != ZIP_DIRECTORY_SIGNATURE) {
		_JC_EX_STORE(env, IOException, "invalid directory signature"
		    " 0x%08x != 0x%08x in ZIP file `%s'", signature,
		    ZIP_DIRECTORY_SIGNATURE, zip->path);
		goto fail;
	}

	/* Seek to start of central directory */
	if (lseek(zip->fd, (off_t)offset, SEEK_SET) == -1) {
		_JC_EX_STORE(env, IOException, "can't seek to directory in ZIP"
		    " file `%s': %s", zip->path, strerror(errno));
		goto fail;
	}

	/* Allocate directory entries */
	if ((zip->entries = _jc_vm_zalloc(env,
	    num_entries * sizeof(*zip->entries))) == NULL)
		goto fail;
	zip->num_entries = num_entries;

	/* Read directory entries */
	for (i = 0; i < num_entries; i++) {
		_jc_zip_entry *const zent = &zip->entries[i];
		jshort comment_len;
		jshort extra_len;
		jshort name_len;
		off_t pos;

		/* Read directory entry */
		if (_jc_zip_scan(zip, -4, &signature, 6, NULL,
		    -2, &zent->method, 4, NULL, -4, &zent->crc,
		    -4, &zent->comp_len, -4, &zent->uncomp_len,
		    -2, &name_len, -2, &extra_len, -2, &comment_len,
		    8, NULL, -4, &offset, 0) != JNI_OK) {
			_JC_EX_STORE(env, IOException, "can't read entry"
			    " directory #%d in ZIP file `%s': %s", i + 1,
			    zip->path, strerror(errno));
			goto fail;
		}

		/* Check signature */
		if (signature != ZIP_DIRENTRY_SIGNATURE) {
			_JC_EX_STORE(env, IOException, "invalid signature"
			    " 0x%08x != 0x%08x for directory entry #%d in ZIP"
			    " file `%s'", signature, ZIP_DIRENTRY_SIGNATURE,
			    i + 1, zip->path);
			goto fail;
		}

		/* Allocate buffer for file name */
		if ((zent->name = _jc_vm_alloc(env, name_len + 1)) == NULL)
			goto fail;

		/* Read entry file name */
		if (_jc_zip_scan(zip, name_len, zent->name, 0) != JNI_OK) {
			_JC_EX_STORE(env, IOException, "can't read directory"
			    " entry #%d in ZIP file `%s': %s", i + 1,
			    zip->path, strerror(errno));
			goto fail;
		}
		zent->name[name_len] = '\0';

		/* Skip over extra and comment fields */
		if (_jc_zip_scan(zip,
		    extra_len + comment_len, NULL, 0) != JNI_OK) {
			_JC_EX_STORE(env, IOException, "can't read entry `%s'"
			    " in ZIP file `%s': %s", zent->name, zip->path,
			    strerror(errno));
			goto fail;
		}

		/* Save current position */
		if ((pos = lseek(zip->fd, 0, SEEK_CUR)) == (off_t)-1) {
			_JC_EX_STORE(env, IOException, "can't seek entry `%s'"
			    " in ZIP file `%s': %s", zent->name, zip->path,
			    strerror(errno));
			goto fail;
		}

		/* Jump to entry local header extra data length field */
		if (lseek(zip->fd,
		    (off_t)(offset + ZIP_LOCAL_HEADER_EXTRA_OFFSET),
		    SEEK_SET) == (off_t)-1) {
			_JC_EX_STORE(env, IOException, "can't seek entry `%s'"
			    " in ZIP file `%s': %s", zent->name, zip->path,
			    strerror(errno));
			goto fail;
		}

		/* Read length of local extra data */
		if (_jc_zip_scan(zip, -2, &extra_len, 0) != JNI_OK) {
			_JC_EX_STORE(env, IOException, "can't read entry `%s'"
			    " in ZIP file `%s': %s", zent->name, zip->path,
			    strerror(errno));
			goto fail;
		}

		/* Compute offset of actual file data */
		zent->offset = offset
		    + ZIP_LOCAL_HEADER_LEN + name_len + extra_len;

		/* Jump back to previous position in directory */
		if (lseek(zip->fd, pos, SEEK_SET) == (off_t)-1) {
			_JC_EX_STORE(env, IOException, "can't seek entry `%s'"
			    " in ZIP file `%s': %s", zent->name, zip->path,
			    strerror(errno));
			goto fail;
		}
	}

	/* Sort the entries by name for faster searching */
	qsort(zip->entries, zip->num_entries,
	    sizeof(*zip->entries), _jc_zip_entry_cmp);

	/* Done */
	return zip;

fail:
	/* Clean up after failure */
	_jc_zip_close(&zip);
	return NULL;
}

/*
 * Close a ZIP file.
 */
void
_jc_zip_close(_jc_zip **zipp)
{
	_jc_zip *zip = *zipp;

	/* Sanity check */
	if (zip == NULL)
		return;
	*zipp = NULL;

	/* Free resources */
	while (zip->num_entries > 0) {
		_jc_zip_entry *const zent = &zip->entries[--zip->num_entries];

		_jc_vm_free(&zent->name);
	}
	_jc_vm_free(&zip->entries);
	_jc_vm_free(&zip->path);
	if (zip->fd != -1)
		close(zip->fd);
	_jc_vm_free(&zip);
}

/*
 * Search for an entry in a ZIP file.
 *
 * Returns the entry index, or -1 if unsuccessful.
 * No exceptions stored or posted.
 */
int
_jc_zip_search(_jc_zip *zip, const char *name)
{
	int base;
	int lim;

	/* Search for entry using binary search */
        for (base = 0, lim = zip->num_entries; lim != 0; lim >>= 1) {
		const int target = base + (lim >> 1);
		const _jc_zip_entry *const zent = &zip->entries[target];
		int diff;

		if ((diff = strcmp(name, zent->name)) == 0)
			return target;
		if (diff > 0) {
			base = target + 1;
			lim--;
		}
        }
	return -1;
}

/*
 * Extract an entry from a ZIP file and return its length.
 *
 * Stores an exception and returns -1 if unsuccessful.
 */
jint
_jc_zip_read(_jc_env *env, _jc_zip *zip, int indx, u_char *data)
{
	_jc_zip_entry *const zent = &zip->entries[indx];

	/* Sanity check */
	_JC_ASSERT(indx >= 0 && indx < zip->num_entries);

	/* Decompress data */
	switch (zent->method) {
	case ZIP_METHOD_STORED:
		if (_jc_zip_unstore(env, zip, zent, data) != JNI_OK)
			return JNI_ERR;
		break;
	case ZIP_METHOD_DEFLATED:
		if (_jc_zip_inflate(env, zip, zent, data) != JNI_OK)
			return JNI_ERR;
		break;
	default:
		_JC_EX_STORE(env, IOException, "unsupported compression method"
		    " %d for entry `%s' in ZIP file `%s'", zent->method,
		    zent->name, zip->path);
		return JNI_ERR;
	}

	/* Done */
	return JNI_OK;
}

/*
 * Extract a stored entry.
 *
 * Stores an exception and returns JNI_ERR if unsuccessful.
 */
static jint
_jc_zip_unstore(_jc_env *env, _jc_zip *zip, _jc_zip_entry *zent, void *data)
{
	int i;
	int r;

	/* Sanity check */
	if (zent->comp_len != zent->uncomp_len) {
		_JC_EX_STORE(env, IOException, "inconsistent lengths %d != %d"
		    " for entry `%s' in ZIP file `%s'", zent->comp_len,
		    zent->uncomp_len, zent->name, zip->path);
		return JNI_ERR;
	}

	/* Read data */
	for (i = 0; i < zent->comp_len; i += r) {
		if ((r = pread(zip->fd, (char *)data + i,
		    zent->comp_len - i, zent->offset + i)) == -1) {
			_JC_EX_STORE(env, IOException, "can't read entry `%s'"
			    " in ZIP file `%s': %s", zent->name, zip->path,
			    strerror(errno));
			return JNI_ERR;
		}
		if (r == 0) {
			_JC_EX_STORE(env, IOException, "premature EOF reading"
			    " entry `%s' in ZIP file `%s'", zent->name,
			    zip->path);
			return JNI_ERR;
		}
	}

	/* Done */
	return JNI_OK;
}

/*
 * Extract a deflated entry.
 *
 * Stores an exception and returns JNI_ERR if unsuccessful.
 */
static jint
_jc_zip_inflate(_jc_env *env, _jc_zip *zip, _jc_zip_entry *zent, void *data)
{
	z_stream zs;
	int i;
	int r;

	/* Initialize decompression state */
	memset(&zs, 0, sizeof(zs));
	zs.zalloc = _jc_zip_alloc;
	zs.zfree = _jc_zip_free;
	zs.opaque = env;
	zs.next_out = data;
	zs.avail_out = zent->uncomp_len;
	switch (inflateInit2(&zs, -MAX_WBITS)) {
	case Z_OK:
		break;
	case Z_MEM_ERROR:
		return JNI_ERR;
	case Z_VERSION_ERROR:
		_JC_EX_STORE(env, IOException, "error reading entry `%s' in"
		    " ZIP file `%s': %s", zent->name, zip->path,
		    "incompatible zlib version");
		return JNI_ERR;
	default:
		_JC_ASSERT(JNI_FALSE);
	}

	/* Read and inflate data */
	for (i = 0; i < zent->comp_len; i += r) {
		char buf[512];
		int to_read;
		int flush;

		/* Read the next chunk of data */
		to_read = zent->comp_len - i;
		if (to_read > sizeof(buf))
			to_read = sizeof(buf);
		if ((r = pread(zip->fd, buf,
		    to_read, zent->offset + i)) == -1) {
			_JC_EX_STORE(env, IOException, "error reading entry"
			    " `%s' in ZIP file `%s': %s", zent->name,
			    zip->path, strerror(errno));
			goto fail;
		}
		if (r == 0) {
			_JC_EX_STORE(env, IOException, "error reading entry"
			    " `%s' in ZIP file `%s': %s", zent->name,
			    zip->path, "premature EOF");
			goto fail;
		}

		/*
		 * Check for uncompressed data appearing to be too big.
		 * A bug in zlib somewhere causes this to happen sometimes.
		 */
		if (zs.avail_out == 0) {
			r = inflateEnd(&zs);
			_JC_ASSERT(r == Z_OK);
			return JNI_OK;
		}

		/* Decompress the chunk we just read */
		zs.next_in = (Bytef *) buf;
		zs.avail_in = r;
		flush = (i + r == zent->comp_len) ? Z_FINISH : Z_SYNC_FLUSH;
		switch (inflate(&zs, flush)) {
		case Z_OK:
			_JC_ASSERT(zs.avail_in == 0);
			continue;
		case Z_BUF_ERROR:		/* bug in zlib causes this */
		case Z_STREAM_END:
			if (zs.avail_out != 0 || i + r != zent->comp_len)
				goto bad_length;
			r = inflateEnd(&zs);
			_JC_ASSERT(r == Z_OK);
			return JNI_OK;
		case Z_NEED_DICT:
			_JC_EX_STORE(env, IOException, "error reading entry'"
			    " `%s in ZIP file `%s': %s", zent->name, zip->path,
			    "dictionary required");
			goto fail;
		case Z_DATA_ERROR:
			_JC_EX_STORE(env, IOException, "error reading entry"
			    " `%s' in ZIP file `%s': %s", zent->name,
			    zip->path, "corrupted entry");
			goto fail;
		case Z_MEM_ERROR:
			goto fail;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}

bad_length:
	/* Uncompressed data had the wrong length */
	_JC_EX_STORE(env, IOException, "error reading entry `%s' in ZIP file"
	    " `%s': %s", zent->name, zip->path, "incorrect length");

fail:
	/* Clean up after failure */
	inflateEnd(&zs);
	return JNI_ERR;
}

/*
 * Function to read consecutive data bytes from the ZIP file and perform
 * optional conversion from little endian. A negative count means 16 or
 * 32 bit conversion is performed.
 *
 * NOTE: Does not post or store an exception in the error case.
 */
static int
_jc_zip_scan(_jc_zip *zip, ...)
{
	jint rtn = JNI_OK;
	va_list args;

	va_start(args, zip);
	while (JNI_TRUE) {
		const int count = va_arg(args, int);
		const int num_bytes = (count < 0 ? -count : count);
		void *const data = va_arg(args, void *);
		u_char buf[16];
		u_char *dest;
		int i;
		int r;

		/* A zero count terminates the list */
		if (count == 0)
			break;

		/* Read bytes into provided buffer or byte-swap buffer */
		if (data == NULL || count < 0) {
			_JC_ASSERT(num_bytes <= sizeof(buf));
			dest = buf;
		} else
			dest = data;
		for (i = 0; i < num_bytes; i += r) {
			if ((r = read(zip->fd,
			    dest + i, num_bytes - i)) == -1) {
				rtn = JNI_ERR;
				goto done;
			}
			if (r == 0) {
				errno = ESPIPE;			/* XXX */
				rtn = JNI_ERR;
				goto done;
			}
		}

		/* If no conversion needed, just continue */
		if (data == NULL || count >= 0)
			continue;

		/* Do byte-swapping */
		switch (num_bytes) {
		case 1:
			*((u_char *)data) = buf[0];
			break;
		case 2:
			*((jshort *)data) = buf[0] | (buf[1] << 8);
			break;
		case 4:
			*((jint *)data) = buf[0] | (buf[1] << 8)
			    | (buf[2] << 16) | (buf[3] << 24);
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}

done:
	/* Clean up and return */
	va_end(args);
	return rtn;
}

/*
 * Function to sort ZIP entries by name.
 */
static int
_jc_zip_entry_cmp(const void *v1, const void *v2)
{
	const _jc_zip_entry *const zent1 = v1;
	const _jc_zip_entry *const zent2 = v2;

	return strcmp(zent1->name, zent2->name);
}

/*
 * Memory allocation callback for zlib.
 */
static void *
_jc_zip_alloc(void *cookie, unsigned int num, unsigned int size)
{
	_jc_env *const env = cookie;

	return _jc_vm_alloc(env, num * size);
}

/*
 * Memory free callback for zlib.
 */
static void
_jc_zip_free(void *cookie, void *mem)
{
	_jc_vm_free(&mem);
}


