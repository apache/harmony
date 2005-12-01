/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * ZipEntry represents an entry in a zip file.
 * 
 * @see ZipFile
 * @see ZipInputStream
 */
public class ZipEntry implements ZipConstants, Cloneable {
	String name, comment;

	long compressedSize = -1, crc = -1, size = -1, dataOffset = -1;

	int compressionMethod = -1, time = -1, modDate = -1;

	byte[] extra;

	/**
	 * Zip entry state: Deflated
	 */
	public static final int DEFLATED = 8;

	/**
	 * Zip entry state: Stored
	 */
	public static final int STORED = 0;

	/**
	 * Constructs a new ZipEntry with the specified name.
	 * 
	 * @param name
	 *            the name of the zip entry
	 */
	public ZipEntry(String name) {
		if (name == null)
			throw new NullPointerException();
		if (name.length() > 0xFFFF)
			throw new IllegalArgumentException();
		this.name = name;
	}

	/**
	 * Gets the comment for this ZipEntry.
	 * 
	 * @return the comment for this ZipEntry, or null if there is no comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Gets the compressed size of this ZipEntry.
	 * 
	 * @return the compressed size, or -1 if the compressed size has not been
	 *         set
	 */
	public long getCompressedSize() {
		return compressedSize;
	}

	/**
	 * Gets the crc for this ZipEntry.
	 * 
	 * @return the crc, or -1 if the crc has not been set
	 */
	public long getCrc() {
		return crc;
	}

	/**
	 * Gets the extra information for this ZipEntry.
	 * 
	 * @return a byte array containing the extra information, or null if there
	 *         is none
	 */
	public byte[] getExtra() {
		return extra;
	}

	/**
	 * Gets the compression method for this ZipEntry.
	 * 
	 * @return the compression method, either DEFLATED, STORED or -1 if the
	 *         compression method has not been set
	 */
	public int getMethod() {
		return compressionMethod;
	}

	/**
	 * Gets the name of this ZipEntry.
	 * 
	 * @return the entry name
	 */
	public String getName() {
		return name;
	}

	long getOffset() {
		return dataOffset;
	}

	/**
	 * Gets the uncompressed size of this ZipEntry.
	 * 
	 * @return the uncompressed size, or -1 if the size has not been set
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Gets the last modification time of this ZipEntry.
	 * 
	 * @return the last modification time as the number of milliseconds since
	 *         Jan. 1, 1970
	 */
	public long getTime() {
		if (time != -1) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(1980 + ((modDate >> 9) & 0x7f), ((modDate >> 5) & 0xf) - 1,
					modDate & 0x1f, (time >> 11) & 0x1f, (time >> 5) & 0x3f,
					(time & 0x1f) << 1);
			return cal.getTime().getTime();
		}
		return -1;
	}

	/**
	 * Answers if this ZipEntry is a directory.
	 * 
	 * @return <code>true</code> when this ZipEntry is a directory,
	 *         <code>false<code> otherwise
	 */
	public boolean isDirectory() {
		return name.charAt(name.length() - 1) == '/';
	}

	/**
	 * Sets the comment for this ZipEntry.
	 * 
	 * @param string
	 *            the comment
	 */
	public void setComment(String string) {
		if (string == null || string.length() <= 0xFFFF)
			this.comment = string;
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Sets the compressed size for this ZipEntry.
	 * 
	 * @param value
	 *            the compressed size
	 */
	public void setCompressedSize(long value) {
		compressedSize = value;
	}

	/**
	 * Sets the crc for this ZipEntry.
	 * 
	 * @param value
	 *            the crc
	 * 
	 * @throws IllegalArgumentException
	 *             if value is < 0 or > 0xFFFFFFFFL
	 */
	public void setCrc(long value) {
		if (value >= 0 && value <= 0xFFFFFFFFL)
			crc = value;
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Sets the extra information for this ZipEntry.
	 * 
	 * @param data
	 *            a byte array containing the extra information
	 * 
	 * @throws IllegalArgumentException
	 *             when the length of data is > 0xFFFF bytes
	 */
	public void setExtra(byte[] data) {
		if (data == null || data.length <= 0xFFFF)
			extra = data;
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Sets the compression method for this ZipEntry.
	 * 
	 * @param value
	 *            the compression method, either DEFLATED or STORED
	 * 
	 * @throws IllegalArgumentException
	 *             when value is not DEFLATED or STORED
	 */
	public void setMethod(int value) {
		if (value != STORED && value != DEFLATED)
			throw new IllegalArgumentException();
		compressionMethod = value;
	}

	void setName(String entryName) {
		name = entryName;
	}

	void setOffset(long value) {
		dataOffset = value;
	}

	/**
	 * Sets the uncompressed size of this ZipEntry.
	 * 
	 * @param value
	 *            the uncompressed size
	 * 
	 * @throws IllegalArgumentException
	 *             if value is < 0 or > 0xFFFFFFFFL
	 */
	public void setSize(long value) {
		if (value >= 0 && value <= 0xFFFFFFFFL)
			size = value;
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Sets the last modification time of this ZipEntry.
	 * 
	 * @param value
	 *            the last modification time as the number of milliseconds since
	 *            Jan. 1, 1970
	 */
	public void setTime(long value) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date(value));
		int year = cal.get(Calendar.YEAR);
		if (year < 1980) {
			modDate = 0x21;
			time = 0;
		} else {
			modDate = cal.get(Calendar.DATE);
			modDate = (cal.get(Calendar.MONTH) + 1 << 5) | modDate;
			modDate = ((cal.get(Calendar.YEAR) - 1980) << 9) | modDate;
			time = cal.get(Calendar.SECOND) >> 1;
			time = (cal.get(Calendar.MINUTE) << 5) | time;
			time = (cal.get(Calendar.HOUR_OF_DAY) << 11) | time;
		}
	}

	/**
	 * Answers the string representation of this ZipEntry.
	 * 
	 * @return the string representation of this ZipEntry
	 */
	public String toString() {
		return name;
	}

	ZipEntry(String name, String comment, byte[] extra, long modTime,
			long size, long compressedSize, long crc, int compressionMethod,
			long modDate, long offset) {
		this.name = name;
		this.comment = comment;
		this.extra = extra;
		this.time = (int) modTime;
		this.size = size;
		this.compressedSize = compressedSize;
		this.crc = crc;
		this.compressionMethod = compressionMethod;
		this.modDate = (int) modDate;
		this.dataOffset = offset;
	}

	/**
	 * Constructs a new ZipEntry using the values obtained from ze.
	 * 
	 * @param ze
	 *            ZipEntry from which to obtain values.
	 */
	public ZipEntry(ZipEntry ze) {
		this.name = ze.name;
		this.comment = ze.comment;
		this.time = ze.time;
		this.size = ze.size;
		this.compressedSize = ze.compressedSize;
		this.crc = ze.crc;
		this.compressionMethod = ze.compressionMethod;
		this.modDate = ze.modDate;
		this.extra = ze.extra;
		this.dataOffset = ze.dataOffset;
	}

	/**
	 * Returns a shallow copy of this entry
	 * 
	 * @return a copy of this entry
	 */
	public Object clone() {
		return new ZipEntry(this);
	}

	/**
	 * Returns the hashCode for this ZipEntry.
	 * 
	 * @return the hashCode of the entry
	 */
	public int hashCode() {
		return name.hashCode();
	}
}
