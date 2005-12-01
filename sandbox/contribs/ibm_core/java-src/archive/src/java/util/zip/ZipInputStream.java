/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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

package java.util.zip;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;

import com.ibm.oti.util.Msg;
import com.ibm.oti.util.Util;

/**
 * ZipInputStream is an input stream for reading zip files.
 * 
 * @see ZipEntry
 * @see ZipFile
 */
public class ZipInputStream extends InflaterInputStream implements ZipConstants {
	static final int DEFLATED = 8;

	static final int STORED = 0;

	static final int ZIPDataDescriptorFlag = 8;

	static final int ZIPLocalHeaderVersionNeeded = 20;

	private boolean zipClosed = false;

	private boolean entriesEnd = false;

	private boolean hasDD = false;

	private int entryIn = 0;

	private int inRead, lastRead = 0;

	ZipEntry currentEntry;

	private byte[] hdrBuf = new byte[LOCHDR - LOCVER];

	private CRC32 crc = new CRC32();

	private byte[] nameBuf = new byte[256];

	private char[] charBuf = new char[256];

	/**
	 * Constructs a new ZipInputStream on the specified input stream.
	 * 
	 * @param stream
	 *            the input stream
	 */
	public ZipInputStream(InputStream stream) {
		super(new PushbackInputStream(stream, BUF_SIZE), new Inflater(true));
		if (stream == null)
			throw new NullPointerException();
	}

	/**
	 * Closes this ZipInputStream.
	 */
	public void close() throws IOException {
		closeEntry(); // Close the current entry
		zipClosed = true;
		super.close();
	}

	/**
	 * Closes the current zip entry and positions to read the next entry.
	 */
	public void closeEntry() throws IOException {
		if (zipClosed)
			throw new IOException(Msg.getString("K0059"));
		if (currentEntry == null)
			return;
		if (currentEntry instanceof java.util.jar.JarEntry) {
			Attributes temp = ((JarEntry) currentEntry).getAttributes();
			if (temp != null && temp.containsKey("hidden"))
				return;
		}
		// Ensure all entry bytes are read
		skip(Long.MAX_VALUE);
		int inB, out;
		if (currentEntry.compressionMethod == DEFLATED) {
			inB = inf.getTotalIn();
			out = inf.getTotalOut();
		} else {
			inB = inRead;
			out = inRead;
		}
		int diff = 0;
		// Pushback any required bytes
		if ((diff = entryIn - inB) != 0)
			((PushbackInputStream) in).unread(buf, len - diff, diff);

		if (hasDD) {
			in.read(hdrBuf, 0, EXTHDR);
			if (getLong(hdrBuf, 0) != EXTSIG)
				throw new ZipException(Msg.getString("K0020"));
			currentEntry.crc = getLong(hdrBuf, EXTCRC);
			currentEntry.compressedSize = getLong(hdrBuf, EXTSIZ);
			currentEntry.size = getLong(hdrBuf, EXTLEN);
		}
		if (currentEntry.crc != crc.getValue())
			throw new ZipException(Msg.getString("K0077"));
		if (currentEntry.compressedSize != inB || currentEntry.size != out)
			throw new ZipException(Msg.getString("K00ae"));

		inf.reset();
		lastRead = inRead = entryIn = len = 0;
		crc.reset();
		currentEntry = null;
	}

	/**
	 * Reads the next zip entry from this ZipInputStream.
	 */
	public ZipEntry getNextEntry() throws IOException {
		if (currentEntry != null)
			closeEntry();
		if (entriesEnd)
			return null;

		int x = 0, count = 0;
		while (count != 4) {
			count += x = in.read(hdrBuf, count, 4 - count);
			if (x == -1)
				return null;
		}
		long hdr = getLong(hdrBuf, 0);
		if (hdr == CENSIG) {
			entriesEnd = true;
			return null;
		}
		if (hdr != LOCSIG)
			return null;

		// Read the local header
		count = 0;
		while (count != (LOCHDR - LOCVER)) {
			count += x = in.read(hdrBuf, count, (LOCHDR - LOCVER) - count);
			if (x == -1)
				throw new EOFException();
		}
		int version = getShort(hdrBuf, 0) & 0xff;
		if (version > ZIPLocalHeaderVersionNeeded)
			throw new ZipException(Msg.getString("K0008"));
		int flags = getShort(hdrBuf, LOCFLG - LOCVER);
		hasDD = ((flags & ZIPDataDescriptorFlag) == ZIPDataDescriptorFlag);
		int cetime = getShort(hdrBuf, LOCTIM - LOCVER);
		int cemodDate = getShort(hdrBuf, LOCTIM - LOCVER + 2);
		int cecompressionMethod = getShort(hdrBuf, LOCHOW - LOCVER);
		long cecrc = 0, cecompressedSize = 0, cesize = -1;
		if (!hasDD) {
			cecrc = getLong(hdrBuf, LOCCRC - LOCVER);
			cecompressedSize = getLong(hdrBuf, LOCSIZ - LOCVER);
			cesize = getLong(hdrBuf, LOCLEN - LOCVER);
		}
		int flen = getShort(hdrBuf, LOCNAM - LOCVER);
		if (flen == 0)
			throw new ZipException(Msg.getString("K000a"));
		int elen = getShort(hdrBuf, LOCEXT - LOCVER);

		count = 0;
		if (flen > nameBuf.length) {
			nameBuf = new byte[flen];
			charBuf = new char[flen];
		}
		while (count != flen) {
			count += x = in.read(nameBuf, count, flen - count);
			if (x == -1)
				throw new EOFException();
		}
		currentEntry = createZipEntry(Util.convertUTF8WithBuf(nameBuf, charBuf,
				0, flen));
		currentEntry.time = cetime;
		currentEntry.modDate = cemodDate;
		currentEntry.setMethod(cecompressionMethod);
		if (cesize != -1) {
			currentEntry.setCrc(cecrc);
			currentEntry.setSize(cesize);
			currentEntry.setCompressedSize(cecompressedSize);
		}
		if (elen > 0) {
			count = 0;
			byte[] e = new byte[elen];
			while (count != elen) {
				count += x = in.read(e, count, elen - count);
				if (x == -1)
					throw new EOFException();
			}
			currentEntry.setExtra(e);
		}
		return currentEntry;
	}

	/* Read 4 bytes from the buffer and store it as an int */

	/**
	 * Reads up to the specified number of uncompressed bytes into the buffer
	 * starting at the offset.
	 * 
	 * @param buffer
	 *            a byte array
	 * @param start
	 *            the starting offset into the buffer
	 * @param length
	 *            the number of bytes to read
	 * @return the number of bytes read
	 */
	public int read(byte[] buffer, int start, int length) throws IOException {
		if (zipClosed)
			throw new IOException(Msg.getString("K0059"));
		if (inf.finished() || currentEntry == null)
			return -1;
		// avoid int overflow, check null buffer
		if (start <= buffer.length && length >= 0 && start >= 0
				&& buffer.length - start >= length) {
			if (currentEntry.compressionMethod == STORED) {
				int csize = (int) currentEntry.size;
				if (inRead >= csize)
					return -1;
				if (lastRead >= len) {
					lastRead = 0;
					if ((len = in.read(buf)) == -1)
						return -1;
					entryIn += len;
				}
				int toRead = length > len ? len - lastRead : length;
				if ((csize - inRead) < toRead)
					toRead = csize - inRead;
				System.arraycopy(buf, lastRead, buffer, start, toRead);
				lastRead += toRead;
				inRead += toRead;
				crc.update(buffer, start, toRead);
				return toRead;
			}
			if (inf.needsInput()) {
				fill();
				if (len > 0)
					entryIn += len;
			}
			int read = 0;
			try {
				read = inf.inflate(buffer, start, length);
			} catch (DataFormatException e) {
				throw new ZipException(e.getMessage());
			}
			if (read == 0 && inf.finished())
				return -1;
			crc.update(buffer, start, read);
			return read;
		}
		throw new ArrayIndexOutOfBoundsException();
	}

	/**
	 * Skips up to the specified number of bytes in the current zip entry.
	 * 
	 * @param value
	 *            the number of bytes to skip
	 * @return the number of bytes skipped
	 */
	public long skip(long value) throws IOException {
		long skipped = 0;
		int x = 0;
		byte[] b = new byte[1024];
		while (skipped != value) {
			x = read(b, 0, (int) (b.length > value ? value : b.length));
			if (x == -1)
				return skipped;
			skipped += x;
		}
		return skipped;
	}

	/**
	 * Answers 1 if the EOF has been reached, otherwise returns 0.
	 * 
	 * @return 0 after EOF of current entry, 1 otherwise
	 */
	public int available() throws IOException {
		if (zipClosed)
			throw new IOException(Msg.getString("K0059"));
		if (currentEntry == null)
			return 1;
		if (currentEntry.compressionMethod == STORED) {
			if (inRead >= currentEntry.size)
				return 0;
		} else {
			if (inf.finished())
				return 0;
		}
		return 1;
	}

	protected ZipEntry createZipEntry(String name) {
		return new ZipEntry(name);
	}

	private int getShort(byte[] buffer, int off) {
		return (buffer[off] & 0xFF) | ((buffer[off + 1] & 0xFF) << 8);
	}

	private long getLong(byte[] buffer, int off) {
		long l = 0;
		l |= (buffer[off] & 0xFF);
		l |= (buffer[off + 1] & 0xFF) << 8;
		l |= (buffer[off + 2] & 0xFF) << 16;
		l |= ((long) (buffer[off + 3] & 0xFF)) << 24;
		return l;
	}
}
