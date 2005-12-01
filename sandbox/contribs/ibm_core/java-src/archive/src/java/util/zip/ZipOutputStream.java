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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import com.ibm.oti.util.Msg;

/**
 * ZipOuputStream is used to write ZipEntries to the underlying stream. Output
 * from ZipOutputStream conforms to the ZipFile file format.
 * 
 * @see ZipInputStream
 * @see ZipEntry
 */
public class ZipOutputStream extends DeflaterOutputStream implements
		ZipConstants {

	public static final int DEFLATED = 8;

	public static final int STORED = 0;

	static final int ZIPDataDescriptorFlag = 8;

	static final int ZIPLocalHeaderVersionNeeded = 20;

	private String comment;

	private Vector entries = new Vector();

	private int compressMethod = DEFLATED;

	private int compressLevel = Deflater.DEFAULT_COMPRESSION;

	private ByteArrayOutputStream cDir = new ByteArrayOutputStream();

	private ZipEntry currentEntry;

	private CRC32 crc = new CRC32();

	private int offset = 0, curOffset = 0, nameLength;

	private byte[] nameBytes;

	/**
	 * Contructs a new ZipOutputStream on p1
	 * 
	 * @param p1
	 *            OutputStream The InputStream to output to
	 */
	public ZipOutputStream(OutputStream p1) {
		super(p1, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
	}

	/**
	 * Closes the current ZipEntry if any. Closes the underlying output stream.
	 * 
	 * @exception IOException
	 *                If an error occurs closing the stream
	 */
	public void close() throws IOException {
		finish();
		out.close();
		out = null;
	}

	/**
	 * Closes the current ZipEntry. Any entry terminal data is written to the
	 * underlying stream.
	 * 
	 * @exception IOException
	 *                If an error occurs closing the entry
	 */
	public void closeEntry() throws IOException {
		if (cDir == null)
			throw new IOException(Msg.getString("K0059"));
		if (currentEntry == null)
			return;
		if (currentEntry.getMethod() == DEFLATED) {
			super.finish();
		}

		// Verify values for STORED types
		if (currentEntry.getMethod() == STORED) {
			if (crc.getValue() != currentEntry.crc)
				throw new ZipException(Msg.getString("K0077"));
			if (currentEntry.size != crc.tbytes)
				throw new ZipException(Msg.getString("K00ae"));
		}
		curOffset = LOCHDR;

		// Write the DataDescriptor
		if (currentEntry.getMethod() != STORED) {
			curOffset += EXTHDR;
			writeLong(out, EXTSIG);
			writeLong(out, currentEntry.crc = crc.getValue());
			writeLong(out, currentEntry.compressedSize = def.getTotalOut());
			writeLong(out, currentEntry.size = def.getTotalIn());
		}
		// Update the CentralDirectory
		writeLong(cDir, CENSIG);
		writeShort(cDir, ZIPLocalHeaderVersionNeeded); // Version created
		writeShort(cDir, ZIPLocalHeaderVersionNeeded); // Version to extract
		writeShort(cDir, currentEntry.getMethod() == STORED ? 0
				: ZIPDataDescriptorFlag);
		writeShort(cDir, currentEntry.getMethod());
		writeShort(cDir, currentEntry.time);
		writeShort(cDir, currentEntry.modDate);
		writeLong(cDir, crc.getValue());
		if (currentEntry.getMethod() == DEFLATED) {
			curOffset += writeLong(cDir, def.getTotalOut());
			writeLong(cDir, def.getTotalIn());
		} else {
			curOffset += writeLong(cDir, crc.tbytes);
			writeLong(cDir, crc.tbytes);
		}
		curOffset += writeShort(cDir, nameLength);
		if (currentEntry.extra != null)
			curOffset += writeShort(cDir, currentEntry.extra.length);
		else
			writeShort(cDir, 0);
		String c;
		if ((c = currentEntry.getComment()) != null)
			writeShort(cDir, c.length());
		else
			writeShort(cDir, 0);
		writeShort(cDir, 0); // Disk Start
		writeShort(cDir, 0); // Internal File Attributes
		writeLong(cDir, 0); // External File Attributes
		writeLong(cDir, offset);
		cDir.write(nameBytes);
		nameBytes = null;
		if (currentEntry.extra != null)
			cDir.write(currentEntry.extra);
		offset += curOffset;
		if (c != null)
			cDir.write(c.getBytes());
		currentEntry = null;
		crc.reset();
		def.reset();
		done = false;
	}

	/**
	 * Indicates that all entries have been written to the stream. Any terminal
	 * Zipfile information is written to the underlying stream.
	 * 
	 * @exception IOException
	 *                If an error occurs while finishing
	 */
	public void finish() throws IOException {
		if (out == null)
			throw new IOException(Msg.getString("K0059"));
		if (cDir == null)
			return;
		if (entries.size() == 0)
			throw new ZipException(Msg.getString("K00b6"));
		if (currentEntry != null)
			closeEntry();
		int cdirSize = cDir.size();
		// Write Central Dir End
		writeLong(cDir, ENDSIG);
		writeShort(cDir, 0); // Disk Number
		writeShort(cDir, 0); // Start Disk
		writeShort(cDir, entries.size()); // Number of entries
		writeShort(cDir, entries.size()); // Number of entries
		writeLong(cDir, cdirSize); // Size of central dir
		writeLong(cDir, offset); // Offset of central dir
		if (comment != null) {
			writeShort(cDir, comment.length());
			cDir.write(comment.getBytes());
		} else
			writeShort(cDir, 0);
		// Write the central dir
		out.write(cDir.toByteArray());
		cDir = null;

	}

	/**
	 * Writes entry information for ze to the underlying stream. Data associated
	 * with the entry can then be written using write(). After data is written
	 * closeEntry() must be called to complete the storing of ze on the
	 * underlying stream.
	 * 
	 * @param ze
	 *            ZipEntry to store
	 * @exception IOException
	 *                If an error occurs storing the entry
	 * @see #write
	 */
	public void putNextEntry(ZipEntry ze) throws java.io.IOException {
		if (ze.getMethod() == STORED
				|| (compressMethod == STORED && ze.getMethod() == -1)) {
			if (ze.crc == -1)
				/* [MSG "K0077", "Crc mismatch"] */
				throw new ZipException(Msg.getString("K0077"));
			if (ze.size == -1 && ze.compressedSize == -1)
				/* [MSG "K00ae", "Size mismatch"] */
				throw new ZipException(Msg.getString("K00ae"));
			if (ze.size != ze.compressedSize && ze.compressedSize != -1
					&& ze.size != -1)
				/* [MSG "K00ae", "Size mismatch"] */
				throw new ZipException(Msg.getString("K00ae"));
		}
		/* [MSG "K0059", "Stream is closed"] */
		if (cDir == null)
			throw new IOException(Msg.getString("K0059"));
		if (currentEntry != null)
			closeEntry();
		if (entries.contains(ze.name))
			/* [MSG "K0066", "Entry already exists: {0}"] */
			throw new ZipException(Msg.getString("K0066", ze.name));
		nameLength = utf8Count(ze.name);
		if (nameLength > 0xffff)
			/* [MSG "K01a7", "Name too long: {0}"] */
			throw new IllegalArgumentException(Msg.getString("K01a7", ze.name));

		def.setLevel(compressLevel);
		currentEntry = ze;
		entries.add(currentEntry.name);
		if (currentEntry.getMethod() == -1)
			currentEntry.setMethod(compressMethod);
		writeLong(out, LOCSIG); // Entry header
		writeShort(out, ZIPLocalHeaderVersionNeeded); // Extraction version
		writeShort(out, currentEntry.getMethod() == STORED ? 0
				: ZIPDataDescriptorFlag);
		writeShort(out, currentEntry.getMethod());
		if (currentEntry.getTime() == -1)
			currentEntry.setTime(System.currentTimeMillis());
		writeShort(out, currentEntry.time);
		writeShort(out, currentEntry.modDate);

		if (currentEntry.getMethod() == STORED) {
			if (currentEntry.size == -1)
				currentEntry.size = currentEntry.compressedSize;
			else if (currentEntry.compressedSize == -1)
				currentEntry.compressedSize = currentEntry.size;
			writeLong(out, currentEntry.crc);
			writeLong(out, currentEntry.size);
			writeLong(out, currentEntry.size);
		} else {
			writeLong(out, 0);
			writeLong(out, 0);
			writeLong(out, 0);
		}
		writeShort(out, nameLength);
		if (currentEntry.extra != null)
			writeShort(out, currentEntry.extra.length);
		else
			writeShort(out, 0);
		nameBytes = toUTF8Bytes(currentEntry.name, nameLength);
		out.write(nameBytes);
		if (currentEntry.extra != null)
			out.write(currentEntry.extra);
	}

	/**
	 * Sets the ZipFile comment associated with the file being written.
	 * 
	 */
	public void setComment(String comment) {
		if (comment.length() > 0xFFFF)
			throw new IllegalArgumentException(Msg.getString("K0068"));
		this.comment = comment;
	}

	/**
	 * Sets the compression level to be used for writing entry data. This level
	 * may be set on a per entry basis. level must have a value between 0 and
	 * 10.
	 * 
	 */
	public void setLevel(int level) {
		if (level < Deflater.DEFAULT_COMPRESSION
				|| level > Deflater.BEST_COMPRESSION)
			throw new IllegalArgumentException();
		compressLevel = level;
	}

	/**
	 * Sets the compression method to be used when compressing entry data.
	 * method must be one of STORED or DEFLATED.
	 * 
	 * @param method
	 *            Compression method to use
	 */
	public void setMethod(int method) {
		if (method != STORED && method != DEFLATED)
			throw new IllegalArgumentException();
		compressMethod = method;

	}

	private long writeLong(OutputStream os, long i) throws java.io.IOException {
		// Write out the long value as an unsigned int
		os.write((int) (i & 0xFF));
		os.write((int) (i >> 8) & 0xFF);
		os.write((int) (i >> 16) & 0xFF);
		os.write((int) (i >> 24) & 0xFF);
		return i;
	}

	private int writeShort(OutputStream os, int i) throws java.io.IOException {
		os.write(i & 0xFF);
		os.write((i >> 8) & 0xFF);
		return i;

	}

	/**
	 * Writes data for the current entry to the underlying stream.
	 * 
	 * @exception IOException
	 *                If an error occurs writing to the stream
	 */
	public void write(byte[] buffer, int off, int nbytes)
			throws java.io.IOException {
		// avoid int overflow, check null buf
		if ((off > buffer.length) || (nbytes < 0) || (off < 0)
				|| (buffer.length - off < nbytes)) {
			throw new IndexOutOfBoundsException();
		}

		if (currentEntry == null) {
			/* [MSG "K00ab", "No active entry"] */
			throw new ZipException(Msg.getString("K00ab"));
		}

		if (currentEntry.getMethod() == STORED)
			out.write(buffer, off, nbytes);
		else
			super.write(buffer, off, nbytes);
		crc.update(buffer, off, nbytes);
	}

	static int utf8Count(String value) {
		int total = 0;
		for (int i = value.length(); --i >= 0;) {
			char ch = value.charAt(i);
			if (ch < 0x80)
				total++;
			else if (ch < 0x800)
				total += 2;
			else
				total += 3;
		}
		return total;
	}

	static byte[] toUTF8Bytes(String value, int length) {
		byte[] result = new byte[length];
		int pos = result.length;
		for (int i = value.length(); --i >= 0;) {
			char ch = value.charAt(i);
			if (ch < 0x80)
				result[--pos] = (byte) ch;
			else if (ch < 0x800) {
				result[--pos] = (byte) (0x80 | (ch & 0x3f));
				result[--pos] = (byte) (0xc0 | (ch >> 6));
			} else {
				result[--pos] = (byte) (0x80 | (ch & 0x3f));
				result[--pos] = (byte) (0x80 | ((ch >> 6) & 0x3f));
				result[--pos] = (byte) (0xe0 | (ch >> 12));
			}
		}
		return result;
	}
}
