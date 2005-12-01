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

package com.ibm.oti.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.ibm.oti.util.ASN1Decoder.BMPString;
import com.ibm.oti.util.ASN1Decoder.BitString;
import com.ibm.oti.util.ASN1Decoder.CertificateSet;
import com.ibm.oti.util.ASN1Decoder.Data;
import com.ibm.oti.util.ASN1Decoder.Explicit;
import com.ibm.oti.util.ASN1Decoder.GeneralizedTime;
import com.ibm.oti.util.ASN1Decoder.Set;
import com.ibm.oti.util.ASN1Decoder.Set2;
import com.ibm.oti.util.ASN1Decoder.UTCTime;

/**
 * This class implements ASN.1 encoding, but just the smallest subset possible,
 * to support the JCA. For more details, see comment in class ASN1Decoder.
 */
public class ASN1Encoder {

	private OutputStream output; // Where data is written to

	/**
	 * Constructs a new ASN1Encoder.
	 * 
	 * @param output
	 *            OutputStream Where the bytes will be written to
	 */
	public ASN1Encoder(OutputStream output) {
		this.output = output;
	}

	/**
	 * Returns the ASN.1 encoding of an object. This object must be one of the
	 * java mapping types (see writeObject()).
	 * 
	 * @param obj
	 *            Object the object to encode
	 * @return byte[] the encoded ASN.1 representation of the given object or
	 *         null if this object is not a mapped one.
	 */
	public static byte[] getEncoding(Object obj) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ASN1Encoder enc = new ASN1Encoder(out);
		try {
			enc.writeObject(obj);
		} catch (ASN1Exception e) {
			return null;
		}
		return out.toByteArray();
	}

	/**
	 * Shared API for writing a byte to the output.
	 * 
	 * @param oneByte
	 *            int The byte to write
	 * @param out
	 *            OutputStream Where to write the byte
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeByte(int oneByte, OutputStream out)
			throws ASN1Exception {
		try {
			out.write(oneByte);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Shared API for writing bytes to the output.
	 * 
	 * @param bytes
	 *            byte[] The bytes to write
	 * @param out
	 *            OutputStream Where to write the bytes
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeBytes(byte[] bytes, OutputStream out)
			throws ASN1Exception {
		try {
			out.write(bytes);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes a given tag number into the output stream.
	 * 
	 * @param tag
	 *            int Code for the tag to be written
	 * @param out
	 *            OutputStream Where to write the tag
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeTagNumber(int tag, OutputStream out)
			throws ASN1Exception {
		if ((tag == ASN1Decoder.SEQUENCE) || (tag == ASN1Decoder.SET))
			tag |= 32; // We set bit 6
		writeByte(tag, out);
	}

	/**
	 * Writes a given tag number into the receiver's output stream.
	 * 
	 * @param tag
	 *            int Code for the tag to be written
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeTagNumber(int tag) throws ASN1Exception {
		writeTagNumber(tag, output);
	}

	/**
	 * Writes an ASN.1 INTEGER type into the output
	 * 
	 * @param value
	 *            BigInteger The value to write as an ASN.1 INTEGER
	 * @param out
	 *            OutputStream Where to write the bytes
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeInteger(BigInteger value, OutputStream out)
			throws ASN1Exception {
		byte[] representation = value.toByteArray();
		writeTagNumber(ASN1Decoder.INTEGER, out); // tag
		writeLength(representation.length, out); // integer length
		writeBytes(representation, out);
	}

	/**
	 * Writes an ASN.1 OCTET STRING type into the output
	 * 
	 * @param bytes
	 *            byte[] The value to write as an ASN.1 OCTET STRING
	 * @param out
	 *            OutputStream Where to write the bytes
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeOctetString(byte[] bytes, OutputStream out)
			throws ASN1Exception {
		writeTagNumber(ASN1Decoder.OCTET_STRING, out); // tag
		writeLength(bytes.length, out); // integer length
		writeBytes(bytes, out);
	}

	/**
	 * Writes an ASN.1 OCTET STRING type into the receiver's output
	 * 
	 * @param bytes
	 *            byte[] The value to write as an ASN.1 OCTET STRING
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeOctetString(byte[] bytes) throws ASN1Exception {
		writeOctetString(bytes, output);
	}

	/**
	 * Writes an ASN.1 BIT STRING type into the output
	 * 
	 * @param bitString
	 *            BitString The value to write as an ASN.1 BIT STRING
	 * @param out
	 *            OutputStream Where to write the bytes
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeBitString(BitString bitString, OutputStream out)
			throws ASN1Exception {
		writeTagNumber(ASN1Decoder.BIT_STRING, out); // tag
		writeLength(bitString.data.length + 1, out); // integer length
		writeByte(bitString.unusedBits, out); // Must be 255 or less.
		writeBytes(bitString.data, out);
	}

	/**
	 * Writes an ASN.1 BIT STRING type into the receiver's output
	 * 
	 * @param bitString
	 *            BitString The value to write as an ASN.1 BIT STRING
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeBitString(BitString bitString) throws ASN1Exception {
		writeBitString(bitString, output);
	}

	/**
	 * Writes an ASN.1 Object Identifier (OID) type into the output
	 * 
	 * @param oid
	 *            int[] The value to write as an ASN.1 Object Identifier.
	 * @param out
	 *            OutputStream Where to write the bytes.
	 * @exception ASN1Exception
	 *                If an error happens when writing.
	 */
	private static void writeObjectIdentifier(int[] oid, OutputStream out)
			throws ASN1Exception {

		for (int i = 0; i < oid.length; ++i)
			if (oid[i] < 0)
				throw new ASN1Exception();

		// The first byte which is written out is an odd encoding
		// of the first two ints in the oid.
		if (oid.length < 2)
			throw new ASN1Exception();
		if (oid[1] > 39)
			throw new ASN1Exception();
		short firstByte = (short) (oid[0] * 40 + oid[1]);
		if (firstByte > 255 || firstByte < 0)
			throw new ASN1Exception();

		// Construct an array containing the bytes to write out
		// _in_reverse_order_.
		// (Use an array of shorts to make sure we don't go negative.)
		short[] bytesToWrite = new short[oid.length * 5];
		int byteIndex = 0;
		for (int i = oid.length - 1; i > 1; i--) {
			int current = oid[i];
			bytesToWrite[byteIndex++] = (short) (current & 0x7f);
			for (current /= 128; current > 0; current /= 128)
				bytesToWrite[byteIndex++] = (short) ((current & 0x7f) + 0x80);
		}
		bytesToWrite[byteIndex] = firstByte;

		// Write out the tag indicating we are writing an OID.
		writeTagNumber(ASN1Decoder.OBJECT_IDENTIFIER, out);

		// Write out the number of bytes we will write.
		writeLength(byteIndex + 1, out);

		// Now, walk back down the array writing out the previously
		// constructed values.
		while (byteIndex >= 0)
			writeByte(bytesToWrite[byteIndex--], out);
	}

	/**
	 * Writes an ASN.1 Object Identifier (OID) type into the receiver's output
	 * 
	 * @param oid
	 *            int[] The value to write as an ASN.1 Object Identifier
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeObjectIdentifier(int[] oid) throws ASN1Exception {
		writeObjectIdentifier(oid, output);
	}

	/**
	 * Writes the length of the next ASN.1 object in the stream.
	 * 
	 * @param len
	 *            int The length, in bytes, of the next ASN.1 object that will
	 *            be written
	 * @param out
	 *            OutputStream Where to write the value
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeLength(int len, OutputStream out)
			throws ASN1Exception {
		if (len < 0x80) { // 1-byte form
			writeByte(len, out);
			return;
		}
		// can't need more than 4 bytes since len is int.
		short[] representation = new short[5];
		int i;
		for (i = 0; len > 0; len >>>= 8)
			representation[i++] = (short) (len & 0xFF);
		// Write length with tag bit to indicate multiple octets
		writeByte(i | 0x80, out);
		while (--i >= 0)
			writeByte(representation[i], out);
	}

	/**
	 * Writes the length of the next ASN.1 object into the receiver's stream.
	 * 
	 * @param len
	 *            int The length, in bytes, of the next ASN.1 object that will
	 *            be written
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeLength(int len) throws ASN1Exception {
		writeLength(len, output);
	}

	/**
	 * Writes an ASN.1 INTEGER type into the receiver's output
	 * 
	 * 
	 * @param value
	 *            BigInteger The value to write as an ASN.1 INTEGER
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeInteger(BigInteger value) throws ASN1Exception {
		writeInteger(value, output);
	}

	/**
	 * Writes an ASN.1 SEQUENCE of INTEGERs into the receiver's output
	 * 
	 * @param values
	 *            BigInteger[] The values to write as an ASN.1 SEQUENCE of
	 *            INTEGER
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	public void writeIntegers(BigInteger[] values) throws ASN1Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
		for (int i = 0; i < values.length; i++) {
			writeInteger(values[i], buffer);
		}
		writeTagNumber(ASN1Decoder.SEQUENCE, output); // tag
		writeLength(buffer.size(), output); // integer length
		try {
			buffer.writeTo(output);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes an ASN.1 SEQUENCE into the receiver's output
	 * 
	 * @param values
	 *            Object[] The values to write as an ASN.1 SEQUENCE
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeSequence(Object[] values) throws ASN1Exception {
		writeSequence(values, output);
	}

	/**
	 * Writes an ASN.1 SEQUENCE into the output
	 * 
	 * @param values
	 *            Object[] The values to write as an ASN.1 SEQUENCE
	 * @param out
	 *            OutputStream Where to write the values
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeSequence(Object[] values, OutputStream out)
			throws ASN1Exception {
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		for (int i = 0; i < values.length; i++) {
			writeObject(values[i], newOut);
		}
		writeTagNumber(ASN1Decoder.SEQUENCE, out);
		// How many bytes inside this sequence
		writeLength(newOut.size(), out);
		// And now we write the actual bytes to the target stream
		try {
			newOut.writeTo(out);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes an ASN.1 object to the receiver's output according to its Java
	 * type. Different Java types map to corresponding ASN.1 types.
	 * 
	 * @param value
	 *            Object The value to write as an ASN.1 object
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	public void writeObject(Object value) throws ASN1Exception {
		writeObject(value, output);
	}

	/**
	 * Writes an ASN.1 object to the output according to its Java type.
	 * Different Java types map to corresponding ASN.1 types.
	 * 
	 * @param value
	 *            Object The value to write as an ASN.1 object
	 * @param out
	 *            OutputStream Where to write the values
	 * @exception ASN1Exception
	 *                If an error happens when writing or if no corresponding
	 *                ASN.1 type exists
	 */
	private static void writeObject(Object value, OutputStream out)
			throws ASN1Exception {
		if (value == null) {
			writeTagNumber(ASN1Decoder.NULL, out);
			writeLength(0, out);
			return;
		}

		if (value instanceof BigInteger) {
			writeInteger((BigInteger) value, out);
			return;
		}

		if (value instanceof byte[]) {
			writeOctetString((byte[]) value, out);
			return;
		}

		if (value instanceof int[]) {
			writeObjectIdentifier((int[]) value, out);
			return;
		}

		if (value instanceof Object[]) {
			writeSequence((Object[]) value, out);
			return;
		}

		if (value instanceof BitString) {
			writeBitString((BitString) value, out);
			return;
		}

		if (value instanceof UTCTime) {
			writeUTCTime(((UTCTime) value).utcTime, out);
			return;
		}

		if (value instanceof GeneralizedTime) {
			writeGeneralizedTime(((GeneralizedTime) value).generalizedTime, out);
			return;
		}

		if (value instanceof Set) {
			writeSet(((Set) value).sequence, out);
			return;
		}

		if (value instanceof Set2) {
			writeSet2(((Set2) value).sequence, out);
			return;
		}

		if (value instanceof CertificateSet) {
			writeCertificateSet(((CertificateSet) value).sequence, out);
			return;
		}

		if (value instanceof Explicit) {
			writeExplicit(((Explicit) value).type, out);
			return;
		}

		if (value instanceof BMPString) {
			writeBMPString(((BMPString) value).bmpString, out);
			return;
		}

		if (value instanceof String) {
			writeUTF8String((String) value, out);
			return;
		}

		if (value instanceof Data) {
			writeBytes(((Data) value).data, out);
			return;
		}

		throw new ASN1Exception(); // Unknown type
	}

	/**
	 * Gets the StringBuffer representation of date from int[].
	 * 
	 * @param date
	 *            int[] the int[] representation of date
	 * @param yearLength
	 *            int the length of year representation
	 * 
	 * @return StringBuffer the representation of date
	 */
	private static StringBuffer getDateString(int[] date, int yearLength) {
		StringBuffer buf = new StringBuffer();

		if (yearLength > 4) {
			buf.append(date[0]);
		} else {
			if (date[0] < 10)
				buf.append("0");
			buf.append(date[0]);
		}
		for (int i = 1; i < date.length; i++) {
			if (date[i] < 10)
				buf.append("0");
			buf.append(date[i]);
		}

		return buf;
	}

	/**
	 * Writes an ASN.1 Generalized Date structure to the input.
	 * 
	 * @param generalizedTime
	 *            byte[] Representing the ASN.1 Generalized Date structure
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 * 
	 */
	private static void writeGeneralizedTime(Date generalizedTime,
			OutputStream out) throws ASN1Exception {
		int[] date = new int[6];
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTime(generalizedTime);
		date[0] = cal.get(Calendar.YEAR);
		date[1] = cal.get(Calendar.MONTH);
		date[2] = cal.get(Calendar.DATE);
		date[3] = cal.get(Calendar.MONTH);
		date[4] = cal.get(Calendar.MINUTE);
		date[5] = cal.get(Calendar.SECOND);
		StringBuffer buf = getDateString(date, 4);
		String timeZoneName = "Z"; // GMT
		// !! check timezone name
		buf.append(timeZoneName);
		writeTagNumber(ASN1Decoder.GENERALIZED_TIME, out);
		try {
			byte[] bytes = buf.toString().getBytes("ISO8859_1");
			writeLength(bytes.length, out); // integer length
			writeBytes(bytes, out);
		} catch (UnsupportedEncodingException e) {
			// will not happen
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Writes Generalized Date to input.
	 * 
	 * @param generalizedTime
	 *            Date Generalized Date structure
	 * 
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected void writeGeneralizedTime(Date generalizedTime)
			throws ASN1Exception {
		writeGeneralizedTime(generalizedTime, output);
	}

	/**
	 * Writes an ASN.1 PrintableString structure to the output.
	 * 
	 * @param string
	 *            String PrintableString to write
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writePrintableString(String string, OutputStream out)
			throws ASN1Exception {
		writeTagNumber(ASN1Decoder.PRINTABLE_STRING, out);
		try {
			byte[] bytes = string.getBytes("ISO8859_1");
			writeLength(bytes.length, out); // integer length
			writeBytes(bytes, out);
		} catch (UnsupportedEncodingException e) {
			// will not happen
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Writes a PrintableString to the output.
	 * 
	 * @param string
	 *            String PrintableString to write
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 * 
	 */
	protected void writePrintableString(String string) throws ASN1Exception {
		writePrintableString(string, output);
	}

	/**
	 * Writes an ASN.1 PrintableString structure to the output.
	 * 
	 * @param string
	 *            String PrintableString to write
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeUTF8String(String string, OutputStream out)
			throws ASN1Exception {
		writeTagNumber(ASN1Decoder.UTF_STRING, out);
		try {
			byte[] bytes = string.getBytes("UTF8");
			writeLength(bytes.length, out); // integer length
			writeBytes(bytes, out);
		} catch (UnsupportedEncodingException e) {
			// will not happen
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Writes a PrintableString to the output.
	 * 
	 * 
	 * @param string
	 *            String PrintableString to write
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeUTF8String(String string) throws ASN1Exception {
		writeUTF8String(string, output);
	}

	/**
	 * Writes an ASN.1 Set structure to the output.
	 * 
	 * 
	 * @param sequence
	 *            Object[] representing the ASN.1 Sequence
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeSet(Object[] sequence, OutputStream out)
			throws ASN1Exception {
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		writeSequence(sequence, newOut);
		writeTagNumber(ASN1Decoder.SET, out);
		// how many bytes inside this SET
		writeLength(newOut.size(), out);
		try {
			newOut.writeTo(out);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes an PKCS7 CertificateSet structure to the output. Difference
	 * between a Set and CertificateSet? The type is set to 0 to indicate that
	 * it is the optional CertificateSet field
	 * 
	 * @param sequence
	 *            Object[] representing the ASN.1 Sequence
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeCertificateSet(Object[] sequence, OutputStream out)
			throws ASN1Exception {
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		for (int i = 0; i < sequence.length; i++) {
			writeObject(sequence[i], newOut);
		}
		writeTagNumber(0xA0, out);
		// How many bytes inside this SET
		writeLength(newOut.size(), out);
		// And now we write the actual bytes to the target stream
		try {
			newOut.writeTo(out);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes an ASN.1 Set structure to the output. Difference between writeSet
	 * and writeSet2 : the former writes a sequence under the set that contains
	 * the objects of the SET. writeSet2 writes only the actual set.
	 * 
	 * @param sequence
	 *            Object[] representing the ASN.1 Sequence
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeSet2(Object[] sequence, OutputStream out)
			throws ASN1Exception {
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		for (int i = 0; i < sequence.length; i++) {
			writeObject(sequence[i], newOut);
		}
		writeTagNumber(ASN1Decoder.SET, out);
		// How many bytes inside this SET
		writeLength(newOut.size(), out);
		// And now we write the actual bytes to the target stream
		try {
			newOut.writeTo(out);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes an ASN.1 BMPString to the output.
	 * 
	 * @param bmpString
	 *            String representing the ASN.1 BMPString
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeBMPString(String bmpString, OutputStream out)
			throws ASN1Exception {
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		byte[] bytes = null;
		try {
			bytes = bmpString.getBytes("UnicodeBigUnmarked");
		} catch (UnsupportedEncodingException uee) {
			throw new ASN1Exception(Msg.getString("K018f", uee));
		}
		writeTagNumber(ASN1Decoder.BMP_STRING, out);
		writeLength(bytes.length, out); // integer length
		writeBytes(bytes, out);
		try {
			newOut.writeTo(out);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Writes a Set to the output.
	 * 
	 * @param sequence
	 *            Object[] representing the ASN.1 Sequence
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeSet(Object[] sequence) throws ASN1Exception {
		writeSet(sequence, output);
	}

	/**
	 * Writes a Set to the output.
	 * 
	 * @param sequence
	 *            Object[] representing the ASN.1 Sequence
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeSet2(Object[] sequence) throws ASN1Exception {
		writeSet2(sequence, output);
	}

	/**
	 * Writes a Set to the output.
	 * 
	 * @param sequence
	 *            Object[] representing the ASN.1 Sequence
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeCertificateSet(Object[] sequence) throws ASN1Exception {
		writeCertificateSet(sequence, output);
	}

	/**
	 * Writes an ASN.1 UTC Date structure to the output.
	 * 
	 * @param utcTime
	 *            byte[] representing the ASN.1 UTC Date
	 * @param out
	 *            OutputStream Where to write the values
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeUTCTime(Date utcTime, OutputStream out)
			throws ASN1Exception {
		int[] date = new int[6];
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTime(utcTime);
		date[0] = cal.get(Calendar.YEAR);
		if (date[0] >= 2000)
			date[0] -= 2000; // According to the X.509 spec
		else
			date[0] -= 1900;
		date[1] = cal.get(Calendar.MONTH) + 1;
		date[2] = cal.get(Calendar.DATE);
		date[3] = cal.get(Calendar.HOUR_OF_DAY);
		date[4] = cal.get(Calendar.MINUTE);
		date[5] = cal.get(Calendar.SECOND);
		StringBuffer buf = getDateString(date, 2);
		// GMT
		buf.append("Z");
		writeTagNumber(ASN1Decoder.UTC_TIME, out);
		try {
			byte[] bytes = buf.toString().getBytes("ISO8859_1");
			writeLength(bytes.length, out); // integer length
			writeBytes(bytes, out);
		} catch (UnsupportedEncodingException e) {
			// will not happen
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Writes an UTC Date to the output.
	 * 
	 * @param utcTime
	 *            Date representing UTC Date
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	protected void writeUTCTime(Date utcTime) throws ASN1Exception {
		writeUTCTime(utcTime, output);
	}

	/**
	 * Writes an [0] EXPLICIT tagged ASN.1 type to the output.
	 * 
	 * @param type
	 *            Object representing the ASN.1 type
	 * 
	 * @exception ASN1Exception
	 *                If an error happens when writing
	 */
	private static void writeExplicit(Object type, OutputStream out)
			throws ASN1Exception {
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		writeObject(type, newOut);
		try {
			writeTagNumber(ASN1Decoder.EXPLICIT, out);
			writeLength(newOut.size(), out); // Length of the encoded type.
			// And now we write the type's bytes to the target stream
			newOut.writeTo(out);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Encodes a node using DER encoding.
	 * 
	 * @param node
	 *            ASN1Decoder.Node the node to encode
	 * 
	 * @return byte[] the DER encoding of the node
	 */
	public static byte[] encodeNode(ASN1Decoder.Node node) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			encodeNode(node, out);
		} catch (ASN1Exception e) {
			return null;
		}
		return out.toByteArray();
	}

	/**
	 * Encodes a node using DER encoding to the specified output stream.
	 * 
	 * @param node
	 *            ASN1Decoder.Node the node to encode
	 * @param out
	 *            OutputSTream the stream to write the DER encoding to
	 */
	private static void encodeNode(ASN1Decoder.Node node, OutputStream out)
			throws ASN1Exception {
		if (node.originalType == -1)
			node.originalType = node.type;
		if (node.tagtype == ASN1Decoder.Node.TAG_EXPLICIT) {
			// wrap with an outer tag ( |128 for context-sensitive class and |32
			// for constructed)
			writeTagNumber(node.originalType | 160, out);

			if (node.type != -1) {
				// encode the real node
				ASN1Decoder.Node realNode = new ASN1Decoder.Node();
				realNode.data = node.data;
				realNode.isPrimitive = node.isPrimitive;
				realNode.type = node.type;
				realNode.originalType = node.type;
				realNode.tagtype = ASN1Decoder.Node.TAG_IMPLICIT;
				ByteArrayOutputStream elementOut = new ByteArrayOutputStream();
				encodeNode(realNode, elementOut);
				writeLength(elementOut.size(), out);
				try {
					out.write(elementOut.toByteArray());
				} catch (IOException e) {
				}
			} else {
				// don't encode the inner node
				writeLength(0, out);
			}
		} else {
			if (node.type == ASN1Decoder.SET) {
				// encode the subnodes
				ASN1Decoder.Node[] elements = (ASN1Decoder.Node[]) node.data;
				Object[] encodings = new Object[elements.length];
				int encodedLength = 0;
				for (int i = 0; i < elements.length; i++) {
					ByteArrayOutputStream elementOut = new ByteArrayOutputStream();
					encodeNode(elements[i], elementOut);
					encodings[i] = elementOut.toByteArray();
					encodedLength += elementOut.size();
				}

				// order the encoded subnodes in lexicographic ascending order
				Object[] sortedEncodings = new Object[encodings.length];
				for (int i = 0; i < sortedEncodings.length; i++) {
					byte[] smallestEncoding = null;
					int smallestEncodingIndex = -1;
					for (int j = 0; j < encodings.length; j++) {
						byte[] newSmallestEncoding = getSmallestEncoding(
								smallestEncoding, (byte[]) encodings[j]);
						if (newSmallestEncoding != smallestEncoding) {
							smallestEncoding = newSmallestEncoding;
							smallestEncodingIndex = j;
						}
					}
					encodings[smallestEncodingIndex] = null;
					sortedEncodings[i] = smallestEncoding;
				}

				// write the encoding to the output stream
				writeTagNumber(ASN1Decoder.SET, out);
				writeLength(encodedLength, out);
				for (int i = 0; i < sortedEncodings.length; i++) {
					try {
						out.write((byte[]) sortedEncodings[i]);
					} catch (IOException e) {
					}
				}
				return;
			}

			if (node.type == ASN1Decoder.SEQUENCE) {
				// encode the subnodes
				ASN1Decoder.Node[] elements = (ASN1Decoder.Node[]) node.data;
				Object[] elementEncodings = new Object[elements.length];
				int encodedLength = 0;
				for (int i = 0; i < elements.length; i++) {
					ByteArrayOutputStream elementOut = new ByteArrayOutputStream();
					encodeNode(elements[i], elementOut);
					elementEncodings[i] = elementOut.toByteArray();
					encodedLength += elementOut.size();
				}

				// write the encoding to the output stream
				writeTagNumber(ASN1Decoder.SEQUENCE, out);
				writeLength(encodedLength, out);
				for (int i = 0; i < elementEncodings.length; i++) {
					try {
						out.write((byte[]) elementEncodings[i]);
					} catch (IOException e) {
					}
				}
				return;
			}

			if (node.type == ASN1Decoder.OBJECT_IDENTIFIER) {
				writeObjectIdentifier((int[]) node.data, out);
				return;
			}

			if (node.type == ASN1Decoder.UTC_TIME) {
				writeUTCTime((Date) node.data, out);
				return;
			}

			if (node.type == ASN1Decoder.OCTET_STRING) {
				writeOctetString((byte[]) node.data, out);
				return;
			}

			if (node.type == ASN1Decoder.NULL) {
				writeTagNumber(ASN1Decoder.NULL, out);
				writeLength(0, out);
				return;
			}

			if (node.type == ASN1Decoder.INTEGER) {
				writeInteger((BigInteger) node.data, out);
				return;
			}

			if (node.type == ASN1Decoder.BIT_STRING) {
				writeBitString((BitString) node.data, out);
				return;
			}

			if (node.type == ASN1Decoder.PRINTABLE_STRING) {
				writePrintableString((String) node.data, out);
				return;
			}

			if (node.type == ASN1Decoder.UTF_STRING) {
				writeUTF8String((String) node.data, out);
				return;
			}

			throw new ASN1Exception();
		}
	}

	/*
	 * Lexiographically compares the two DER encodings and returns the smaller
	 * of the two. If one is null, the other is returned. If both are null, null
	 * is returned.
	 */
	private static byte[] getSmallestEncoding(byte[] e1, byte[] e2) {
		if (e1 == null)
			return e2;
		if (e2 == null)
			return e1;

		int i = 0;
		while (true) {
			// if the end of one of the octet strings is reached, return the
			// longer of the two
			if (i == e1.length)
				return e2;
			if (i == e2.length)
				return e1;

			// if the octet strings have different values at i, return the
			// larger
			if (e1[i] < e2[i])
				return e1;
			if (e2[i] < e1[i])
				return e2;

			i++;
		}
	}

}
