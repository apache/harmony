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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.AccessController;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

/**
 * This class implements ASN.1 decoding, but just the smallest subset possible,
 * to support the JCA.
 * 
 * A good starting point for understanding ASN.1 and its encodings is
 * 
 * A Layman's Guide to a Subset of ASN.1, BER, and DER An RSA Laboratories
 * Technical Note Burton S. Kaliski Jr
 * 
 * This document can be obtained from many different sources on the web. One
 * source is http://www.columbia.edu/~ariel/ssleay/layman.html
 * 
 * The "Specification of Abstract Syntax Notation One (ASN.1)" is ITU-T
 * Recommendation X.208 (it can be purchased off http://www.itu.int/). A more
 * useful document is the "Specification of Basic Encoding Rules for Abstract
 * Syntax Notation One (ASN.1)", ITU-T recommendation X.209 (also available at
 * the same URL above, for a fee as well).
 * 
 */
public class ASN1Decoder {

	// Where data comes from
	private PositionedInputStream input;

	// How many levels of nesting we have so far
	private int nesting = 0;

	// When reading a sequence, this indicates which item we are reading
	private int sequenceItem = 0;

	// Implicit and explicit tags "override" the standard ones
	private TypeMapper[] tagConfiguration;

	// if we should collect bytes as we read
	private boolean collectBytes;

	// The actual bytes collected
	private ByteArrayOutputStream bytesCollected;

	// Tag for END of BER contents
	public static final int END_OF_BER_CONTENTS = 0;

	// Tag for BOOLEAN
	public static final int BOOLEAN = 01;

	// Tag for INTEGER
	public static final int INTEGER = 02;

	// Tag for Bit String
	public static final int BIT_STRING = 03;

	// Tag for Octet String
	public static final int OCTET_STRING = 04;

	// Tag for null
	public static final int NULL = 05;

	// Tag for Object Identifier
	public static final int OBJECT_IDENTIFIER = 06;

	// Tag for SEQUENCE
	public static final int SEQUENCE = 16;

	// Tag for SET
	public static final int SET = 17;

	// Tag for BMPString
	public static final int BMP_STRING = 30;

	// Tag for NumericString
	public static final int NUMERIC_STRING = 18;

	// Tag for PrintableString
	public static final int PRINTABLE_STRING = 19;

	// Tag for TeletxtString/T61String
	public static final int T61_STRING = 20;

	// Tag for VideotextString
	public static final int VIDEOTEXT_STRING = 21;

	// Tag for IA5String
	public static final int IA5_STRING = 22;

	// Tag for UTFString
	public static final int UTF_STRING = 12;

	// Tag for UTCTime
	public static final int UTC_TIME = 23;

	// Tag for GeneralizedTime
	public static final int GENERALIZED_TIME = 24;

	// possible values for field elementClass
	public static final int CLASS_UNIVERSAL = 0;

	public static final int EXPLICIT = 160;

	public static final int LENGTH_UNKNOWN = -1;

	/**
	 * We map each ASN.1 to an existing Java type. However, in the case of
	 * BitString we decided to add a new class (byte[] is already used for OCTET
	 * STRING).
	 */
	public static class BitString {

		/**
		 * Number of bits of the representation below which are actually unused.
		 * WARNING: ASN1Encoder and ASN1Decoder assume that the number of
		 * unusedBits fit in a byte.
		 */
		public int unusedBits;

		/**
		 * The actual byte[] representation for the BIT STRING
		 */
		public byte[] data;

		/**
		 * Constructs a new BitString.
		 * 
		 * @param unusedBits
		 *            int number of bits of the representation which are unused.
		 *            WARNING: ASN1Encoder and ASN1Decoder assume that the
		 *            number of unusedBits fit in a byte.
		 * 
		 * @param bytes
		 *            byte[] The actual byte array representation.
		 */
		public BitString(int unusedBits, byte[] bytes) {
			this.unusedBits = unusedBits;
			data = bytes;
		}

		/**
		 * Returns the number of unused bits for the receiver
		 * 
		 * @return int How many bits aren't really used in the bit
		 *         representation in "data"
		 */
		public int bitLength() {
			return data.length * 8 - unusedBits;
		}

		/**
		 * Returns the Nth bit in the receiver (numbered from left to right) as
		 * a boolean. Return true if set, false otherwise.
		 * 
		 * @param bitPosition
		 *            int What bit to return. Bit numbers start at 0, from left
		 *            to right.
		 * @return boolean true if the bit is set, false otherwise.
		 */
		public boolean bitAt(int bitPosition) {
			// First representation octet [0] is where bits 0..7 are, and so on
			int whichOctet = (bitPosition / 8);
			int octet = data[whichOctet] & 0xff;
			// In which bit we must look in that octet, from left to right
			int octetBit = bitPosition % 8;
			return ((octet >>> (7 - octetBit)) & 1) == 1; // is the bit set ?
		}
	}

	/**
	 * An ASN.1 decoder will read data from an input and produce a tree of
	 * Nodes, much like a parse tree. For each ASN.1 type read, there will be a
	 * corresponding Node. Some types are actually collections of other types
	 * (for instance, a SEQUENCE or a SET) so in this case the Node will have
	 * subnodes.
	 * 
	 * Subnodes are "addressed" by position, using the same convention as arrays
	 * in Java. For instance, one can ask for subnode 0 of a SEQUENCE Node.
	 */
	public static class Node {
		public static final int TAG_IMPLICIT = 1;

		public static final int TAG_EXPLICIT = 2;

		// This is a convenient way to access the last subnode of a Node.
		// Instead of having to check the length of the collection of (sub)nodes
		// and having to decrement one, this value is recognized internally as
		// the LAST subnode.
		// So, things like LAST-1 can also be used to fetch the node before the
		// last, and so on.
		public static final int LAST = Integer.MAX_VALUE;

		// A Java object representing the decoded bytes for an ANS.1 type
		public Object data;

		// What kind of node (INTEGER. SEQUENCE, etc) as read from the stream
		public int originalType = -1;

		// How the originalType is encoded
		public int tagtype = TAG_IMPLICIT;

		// What kind of node (INTEGER. SEQUENCE, etc) this is, after remappings.
		public int type;

		// If this node is primitive.
		public boolean isPrimitive;

		// 00=universal, 01=application,10=context-specific,11=private
		int elementClass;

		// Where the encoded representation starts in the input bytes
		public int startPosition;

		// Where the encoded representation ends in the input bytes
		public int endPosition;

		static private String lineTerminator = null;

		static {
			lineTerminator = (String) AccessController
					.doPrivileged(new PriviAction("line.separator"));
		}

		/**
		 * Returns true if the corresponding ASN.1 type class is "universal";
		 * false otherwise.
		 * 
		 * @return boolean true if the corresponding ASN.1 type class is
		 *         "universal"; false otherwise.
		 */
		public boolean isUniversal() {
			return elementClass == 0;
		}

		/**
		 * Returns true if the corresponding ASN.1 type class is "universal";
		 * false otherwise.
		 * 
		 * @return boolean true if the corresponding ASN.1 type class is
		 *         "application"; false otherwise.
		 */
		public boolean isApplication() {
			return elementClass == 1;
		}

		/**
		 * Returns true if the corresponding ASN.1 type class is
		 * "context-specific"; false otherwise.
		 * 
		 * @return boolean true if the corresponding ASN.1 type class is
		 *         "context-specific"; false otherwise.
		 */
		public boolean isContextSpecific() {
			return elementClass == 2;
		}

		/**
		 * Returns true if the corresponding ASN.1 type class is "private";
		 * false otherwise.
		 * 
		 * @return boolean true if the corresponding ASN.1 type class is
		 *         "private"; false otherwise.
		 */
		public boolean isPrivate() {
			return elementClass == 3;
		}

		/**
		 * Returns the Nth subnode in the receiver. If the receiver does not
		 * have subnodes or if the index is not valid, returns null. Index
		 * "LAST" can be used to refer to the last subnode in the receiver.
		 * LAST-1, LAST-2, etc will also work (item before last, 2 before last,
		 * etc).
		 * 
		 * @param position
		 *            int Index of the node we want to fetch
		 * @return Node The subnode at the give position, or null if position is
		 *         not valid.
		 */
		public Node subnode(int position) {
			try {
				int len = ((Node[]) data).length;
				if (position >= len) // indexing relative to last position
					position = len - 1 - (LAST - position);

				return ((Node[]) data)[position];
			} catch (Exception e) {
				return null;
			}
		}

		/**
		 * Returns the subnode in the receiver with a given original type (type
		 * redirection can happen, so this API compares based on the original
		 * type read from the stream). If no subnodes with the given original
		 * type exist, return null.
		 * 
		 * @param origType
		 *            int Code for original type as read from the stream
		 * @return Node Subnode in the receiver with a given original type, null
		 *         if none found.
		 */
		public Node subnodeWithOriginalType(int origType) {
			try {
				Node[] subnodes = (Node[]) data;
				for (int i = 0; i < subnodes.length; i++) {
					Node subnode = subnodes[i];
					if (subnode.originalType == origType)
						return subnode;
				}
			} catch (Exception e) {
			}
			return null;
		}

		/**
		 * In cases where the receiver has multiple levels of subnodes
		 * (representing a tree) it is possible to access a subnode many levels
		 * down with this API. An array of indices is provided, and the indices
		 * will be used to fetch the subnode down the tree. For instance, if
		 * {0,3,7} is passed, the receiver's 0th subnode will be fetched, then
		 * this node's 3rd subnode, and then this last one's 7th subnode will be
		 * returned. As usual, indices are 0-based. If any of the indices is
		 * invalid, null is returned. Note that the use of LAST as a flag to
		 * indicate the last item is not supported by this API (it could/should
		 * be added). Therefore, something like { 0, LAST, LAST-2} will not
		 * work. Contrast this with method subnode().
		 * 
		 * @param positionIndices
		 *            int[] Array of indices for addressing subnodes down the
		 *            tree from the receiver.
		 * @return Node Subnode from under the receiver's tree of nodes, given
		 *         the positions provided.
		 */
		public Object subnode(int[] positionIndices) {
			try {
				Object result = ((Node[]) data)[positionIndices[0]];
				for (int i = 1; i < positionIndices.length; i++)
					result = ((Node[]) ((Node) result).data)[positionIndices[i]];
				return result;
			} catch (Exception e) {
				return null;
			}
		}

		public String toString() {
			return toString(0);
		}

		private String toString(int indent) {
			if ((type == SEQUENCE) || (type == SET)) {
				StringBuffer sb = new StringBuffer();
				Node[] subNodes = (Node[]) data;
				if (type == SEQUENCE) {
					sb.append("[SEQUENCE]");
				} else {
					sb.append("[SET]");
				}
				sb.append("(" + subNodes.length + ") ... "
						+ (endPosition - startPosition + 1));
				for (int i = 0; i < subNodes.length; i++) {
					sb.append(lineTerminator);
					for (int j = 0; j < indent + 2; j++) {
						sb.append(" ");
					}
					sb.append(subNodes[i].toString(indent + 2));
				}

				return sb.toString();
			}
			if (data == null) {
				return ("NULL");
			} else if (data instanceof ASN1Decoder.BitString) {
				return ("[BIT_STRING] " + getStringForByteArray(((ASN1Decoder.BitString) data).data));
			} else if (data instanceof byte[]) {
				return ("[BIT_STRING] " + getStringForByteArray((byte[]) data));
			} else if (data instanceof int[]) {
				StringBuffer result = new StringBuffer();
				result.append("OID ");
				int[] ints = (int[]) data;
				result.append(ints[0]);
				for (int i = 1; i < ints.length; i++) {
					result.append("." + ints[i]);
				}
				return result.toString();
			} else {
				return data.toString();
			}
		}

		private static String getStringForByteArray(byte[] data) {
			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < data.length; i++) {
				sb.append(Integer.toHexString((data[i] >> 4) & 0x0F));
				sb.append(Integer.toHexString((data[i] & 0x0F)));
				sb.append(" ");
			}

			return sb.toString().toUpperCase();
		}

	}

	/**
	 * Adds new class represents ASN.1 object SET,
	 */
	public static class Set {
		public Object[] sequence = null;

		public Set(Object[] s) {
			sequence = s;
		}
	}

	/**
	 * Adds new class represents ASN.1 object SET,
	 * 
	 */
	public static class Set2 {
		public Object[] sequence = null;

		public Set2(Object[] s) {
			sequence = s;
		}
	}

	/**
	 * Represents a CertificateSet as defined in PKCS7
	 */
	public static class CertificateSet {
		public Object[] sequence = null;

		public CertificateSet(Object[] s) {
			sequence = s;
		}
	}

	/**
	 * Adds new class representing a [0] EXPLICIT tagged ASN.1 type.
	 */
	public static class Explicit {
		public Object type = null;

		public Explicit(Object obj) {
			type = obj;
		}
	}

	/**
	 * Adds new class representing ASN.1 object BMPString,
	 */
	public static class BMPString {
		public String bmpString = null;

		public BMPString(String value) {
			bmpString = value;
		}
	}

	/**
	 * Adds new class represents ASN.1 object UTC_TIME
	 */
	public static class UTCTime {
		public Date utcTime = null;

		public UTCTime(Date date) {
			utcTime = date;
		}
	}

	/**
	 * Adds new class represents ASN.1 object GENERALIZED_TIME
	 */
	public static class GeneralizedTime {
		public Date generalizedTime = null;

		public GeneralizedTime(Date date) {
			generalizedTime = date;
		}
	}

	/**
	 * Adds new class represents all kinds of data.
	 */
	public static class Data {
		public byte[] data = null;

		public Data(byte[] d) {
			data = d;
		}
	}

	/**
	 * Constructs a new ASN1Decoder.
	 * 
	 * @param input
	 *            InputStream The source of bytes where to read bytes from
	 */
	public ASN1Decoder(InputStream input) {
		this.input = new PositionedInputStream(input);
	}

	/**
	 * Constructs a new ASN1Decoder.
	 * 
	 * @param input
	 *            InputStream The source of bytes where to read bytes from
	 */
	public ASN1Decoder(InputStream input, boolean set2) {
		this.input = new PositionedInputStream(input);
	}

	/**
	 * Shared API for reading byte from the input. Useful for when collecting
	 * bytes from the input.
	 * 
	 * @return int The actual byte read
	 * @exception ASN1Exception
	 *                If an error happens when reading
	 */
	private int readByte() throws ASN1Exception {
		int octet;
		try {
			octet = input.read();
			if (octet < 0)
				throw new ASN1Exception();
			if (collectBytes)
				this.bytesCollected.write(octet);

			return octet & 0xff;
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Parses a String representation of a ASN.1 UTC date and returns it as a
	 * Date object
	 * 
	 * @param dateString
	 *            String ASN.1 representation of the date, UTC format
	 * @return Date Date object after parsing is done
	 */
	private Date parseUTCDate(String dateString) {
		int date[] = parseYMD(dateString, 2); // 2-digit year
		if (date[0] <= 49)
			date[0] += 2000; // According to the X.509 spec
		else
			date[0] += 1900; // According to the X.509 spec
		return dateFromArray(dateString, date);
	}

	/**
	 * Parses a String representation of a generalized date and returns it as a
	 * Date object
	 * 
	 * @param dateString
	 *            String ASN.1 representation of the date, generalized format
	 * @return Date Date object after parsing is done
	 */
	private Date parseGeneralizedDate(String dateString) {
		int date[] = parseYMD(dateString, 4); // 4-digit year
		return dateFromArray(dateString, date);
	}

	/**
	 * Given String representation of a date and the already parsed values for
	 * day, month and year, it returns a Date object. The string representation
	 * is needed to obtain time zone information.
	 * 
	 * @param dateString
	 *            String ASN.1 representation of the date
	 * @return Date Date object after parsing is done
	 */
	private Date dateFromArray(String dateString, int[] date) {
		String timeZoneName = getTZ(dateString);
		TimeZone tz = TimeZone.getTimeZone(timeZoneName);
		Calendar cal = Calendar.getInstance(tz);
		// -1 because 0 = january in this calendar
		cal.set(date[0], date[1] - 1, date[2], date[3], date[4], date[5]);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/**
	 * Parses year, month and day information from an ASN.1 date in String
	 * representation, based on the number of digits used for representing the
	 * year.
	 * 
	 * @param dateString
	 *            String ASN.1 representation of the date
	 * @param yearDigits
	 *            int How many digits are used to represent the Date
	 * @return Date Date object after parsing is done
	 */
	private int[] parseYMD(String dateString, int yearDigits) {
		int index = 0;
		int digitLength = yearDigits;
		int[] date = new int[6];
		for (int i = 0; i < date.length; i++) {
			// Date string may not contain seconds */
			if (i == 5) {
				char next = dateString.charAt(index);
				if (next < '0' || next > '9')
					break;
			}
			date[i] = Integer.parseInt(dateString.substring(index, index
					+ digitLength));
			index += digitLength;
			digitLength = 2; // User-defined just for year
		}
		return date;
	}

	/**
	 * Parses time zone information from an ASN.1 date in String representation.
	 * Returns just the time zone part, as a String
	 * 
	 * @param dateString
	 *            String ASN.1 representation of the date
	 * @return String The time zone name for the date provided
	 */
	private String getTZ(String dateString) {
		String timeZoneName = "GMT";
		char offsetIndicator = dateString.charAt(dateString.length() - 1);
		if (offsetIndicator != 'Z') { // Not GMT
			timeZoneName += dateString.substring(dateString.length() - 5,
					dateString.length()); // All the rest
		}
		return timeZoneName;
	}

	/**
	 * Configure whether the receiver should collect bytes from the input or
	 * not. If collected, these bytes can be fetched with collectedBytes(), and
	 * therefore for each node the corresponding bytes can be obtained.
	 * 
	 * @param doCollectBytes
	 *            boolean Indicates whether the input bytes should be collected
	 *            (true) or not (false)
	 * @see #collectedBytes
	 */
	public void collectBytes(boolean doCollectBytes) {
		this.collectBytes = doCollectBytes;
		if (doCollectBytes)
			this.bytesCollected = new ByteArrayOutputStream(1024);
		else
			this.bytesCollected = null;
	}

	/**
	 * Returns the collected bytes from the input, or null if the receiver is
	 * not configured to collect bytes.
	 * 
	 * @return byte[] The bytes collected from the input
	 * @see #collectBytes
	 */
	public byte[] collectedBytes() {
		if (collectBytes) {
			return this.bytesCollected.toByteArray();
		}
		return null;
	}

	/**
	 * Read ASN.1 data from the input stream and returns it as a Node.
	 * 
	 * @return Node a Node structure representing ASN.1 data from the input
	 *         stream
	 */
	public Node readContents() throws ASN1Exception {
		Node node = new Node();
		node.startPosition = input.currentPosition();
		readTag(node);
		// Tag configuration only allowed for tags that are not universal
		if (node.elementClass != CLASS_UNIVERSAL)
			computeTypeRedirection(node);

		switch (node.type) {
		case (SEQUENCE):
			node.data = readSequence();
			break;
		case (SET):
			node.data = readSet();
			break;
		case (BOOLEAN):
			node.data = readBoolean();
			break;
		case (INTEGER):
			node.data = readInteger();
			break;
		// After null tag we must find 0x00, which is the length
		case (NULL):
			if (readByte() != 0)
				throwASN1Exception();
			break;
		case (OBJECT_IDENTIFIER):
			node.data = readObjectIdentifier();
			break;
		case (OCTET_STRING):
			node.data = readOctetString();
			break;
		case (NUMERIC_STRING):
			node.data = readNumericString();
			break;
		case (PRINTABLE_STRING):
			node.data = readPrintableString();
			break;
		case (BMP_STRING):
			node.data = readBMPString();
			break;
		case (IA5_STRING):
			node.data = readIA5String();
			break;
		case (UTF_STRING):
			node.data = readUTFString();
			break;
		case (T61_STRING):
			node.data = readT61String();
			break;
		case (VIDEOTEXT_STRING):
			node.data = readVideotextString();
			break;
		case (BIT_STRING):
			node.data = readBitString();
			break;
		case (UTC_TIME):
			node.data = readUTCTime();
			break;
		case (GENERALIZED_TIME):
			node.data = readGeneralizedTime();
			break;
		case (END_OF_BER_CONTENTS):
			int len = readLength(); // should be zero
			if (len != 0)
				throw new ASN1Exception(Msg.getString("K0088"));
			return null;
		default:
			throw new ASN1Exception(Msg.getString("K0089", node.type));
		}

		node.endPosition = input.currentPosition() - 1;
		return node;
	}

	/**
	 * Read ASN.1 data from the input stream and returns it as a Node.
	 * 
	 * @return Object a Java Object structure representing ASN.1 data from the
	 *         input stream
	 */
	public Object readContentsToObject() throws ASN1Exception {
		Node node = new Node();
		node.startPosition = input.currentPosition();
		readTag(node);
		// Tag configuration only allowed for tags that are not universal
		if (node.elementClass != CLASS_UNIVERSAL)
			computeTypeRedirection(node);

		switch (node.type) {
		case (SEQUENCE):
			node.data = readSequenceToObject();
			break;
		case (SET):
			node.data = readSetToObject();
			break;
		case (BOOLEAN):
			node.data = readBoolean();
			break;
		case (INTEGER):
			node.data = readInteger();
			break;
		// After null tag we must find 0x00, which is the length
		case (NULL):
			if (readByte() != 0)
				throwASN1Exception();
			break;
		case (OBJECT_IDENTIFIER):
			node.data = readObjectIdentifier();
			break;
		case (OCTET_STRING):
			node.data = readOctetString();
			break;
		case (NUMERIC_STRING):
			node.data = readNumericString();
			break;
		case (PRINTABLE_STRING):
			node.data = readPrintableString();
			break;
		case (BMP_STRING):
			node.data = readBMPString();
			break;
		case (IA5_STRING):
			node.data = readIA5String();
			break;
		case (UTF_STRING):
			node.data = readUTFString();
			break;
		case (T61_STRING):
			node.data = readT61String();
			break;
		case (VIDEOTEXT_STRING):
			node.data = readVideotextString();
			break;
		case (BIT_STRING):
			node.data = readBitString();
			break;
		case (UTC_TIME):
			node.data = readUTCTimeToObject();
			break;
		case (GENERALIZED_TIME):
			node.data = readGeneralizedTimeToObject();
			break;
		case (END_OF_BER_CONTENTS):
			int len = readLength(); // should be zero
			if (len != 0)
				throw new ASN1Exception(Msg.getString("K0088"));
			return null;
		default:
			throw new ASN1Exception(Msg.getString("K0089", node.type));
		}

		node.endPosition = input.currentPosition() - 1;
		return node.data;
	}

	/**
	 * Reads bytes from the input provided into the buffer also provided. The
	 * number of bytes read will be equal to the buffer size, and they will be
	 * stored starting at position 0.
	 * 
	 * @param source
	 *            InputStream source of data
	 * @param buffer
	 *            byte[] where to read bytes into. Bytes will be stored starting
	 *            at position 0
	 */
	private void readFully(java.io.InputStream source, byte[] buffer)
			throws ASN1Exception {

		try {
			readFully(source, buffer, 0, buffer.length);
		} catch (IOException ioe) {
			throw new ASN1Exception(ioe.toString());
		}
	}

	/**
	 * Reads bytes from the input provided into the buffer also provided. The
	 * number of bytes read will be equal to count, and they will be stored
	 * starting at position offset.
	 * 
	 * @param source
	 *            InputStream source of data
	 * @param buffer
	 *            byte[] where to read bytes into.
	 * @param offset
	 *            int initial position where to store the bytes
	 * @param count
	 *            int how many bytes will be read
	 */
	private void readFully(InputStream source, byte[] buffer, int offset,
			int count) throws IOException {

		int totalRead = 0;
		int toRead = count;

		while (totalRead < count) {
			int readNow = source.read(buffer, offset + totalRead, toRead);
			if (readNow <= 0)
				throw new IOException(Msg.getString("K008a", new Object[] {
						Integer.toString(readNow), Integer.toString(toRead),
						source }));
			if (collectBytes)
				this.bytesCollected.write(buffer, offset + totalRead, readNow);

			totalRead += readNow;
			toRead -= readNow;
		}
	}

	/**
	 * Reads an ASN.1 UTC Date structure from the input and returns it as a Date
	 * object.
	 * 
	 * @return Date a Date representing the ASN.1 UTC Date structure from the
	 *         input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Date readUTCTime() throws ASN1Exception {
		String UTCASCII = convertToString(readOctetString());
		return parseUTCDate(UTCASCII);
	}

	/**
	 * Reads an ASN.1 UTC Date structure from the input and returns it as a
	 * UTCTime object.
	 * 
	 * @return Date a Date representing the ASN.1 UTC Date structure from the
	 *         input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected UTCTime readUTCTimeToObject() throws ASN1Exception {
		String UTCASCII = convertToString(readOctetString());
		return new UTCTime(parseUTCDate(UTCASCII));
	}

	/**
	 * Reads an ASN.1 Generalized Date structure from the input and returns it
	 * as a Date object.
	 * 
	 * @return Date a Date representing the ASN.1 Generalized Date structure
	 *         from the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Date readGeneralizedTime() throws ASN1Exception {
		String generalizedASCII = convertToString(readOctetString());
		return parseGeneralizedDate(generalizedASCII);
	}

	/**
	 * Reads an ASN.1 Generalized Date structure from the input and returns it
	 * as a GeneralizedTime object.
	 * 
	 * @return Date a Date representing the ASN.1 Generalized Date structure
	 *         from the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected GeneralizedTime readGeneralizedTimeToObject()
			throws ASN1Exception {
		String generalizedASCII = convertToString(readOctetString());
		return new GeneralizedTime(parseGeneralizedDate(generalizedASCII));
	}

	/**
	 * Reads an ASN.1 INTEGER type from the input and returns it as a BigInteger
	 * 
	 * @return BigInteger The next INTEGER object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected BigInteger readInteger() throws ASN1Exception {
		int length = readLength();
		byte[] integer = new byte[length];
		readFully(input, integer);
		return (new BigInteger(1, integer));
	}

	/**
	 * Reads an ASN.1 BOOLEAN type from the input and returns it as a Boolean
	 * 
	 * @return Boolean The next BOOLEAN object represented as an ASN.1 structure
	 *         in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Boolean readBoolean() throws ASN1Exception {
		int length = readLength();
		// a boolean should have only 1 byte
		if (length != 1)
			throwASN1Exception();
		int bool = readByte();
		if (bool == 0) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	/**
	 * Reads and returns the length of the next ASN.1 object in the stream.
	 * returns LENGTH_UNKNOWN if the ASN.1 object has BER encoding, and
	 * therefore length is not known a priori (the actual contents have to be
	 * read, and it ends in 00 00).
	 * 
	 * @return int the length of the next ASN.1 object in the stream, or
	 *         LENGTH_UNKNOWN if the length is not known before reading the
	 *         actual contents.
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected int readLength() throws ASN1Exception {
		int len = readByte();
		if (len < 0x80) // 1-byte form
			return len;

		if (len == 0x80)
			return LENGTH_UNKNOWN; // BER encoding

		// multiple octets to describe length;
		int lenOctets = len & 0x7f; // Discard top bit of the byte
		len = 0;
		for (int i = 0; i < lenOctets; i++) {
			len = len * 256 + readByte();
		}
		if (len < 0)
			throwASN1Exception();
		return len;
	}

	/**
	 * Reads an ASN.1 Object Identifier (OID) type from the input and returns it
	 * as an int array
	 * 
	 * @return int[] The next Object identifier object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected int[] readObjectIdentifier() throws ASN1Exception {
		int length = readLength();
		int[] oid = new int[length];
		for (int i = 0; i < oid.length; i++) {
			oid[i] = readByte();
		}
		// First byte encodes 2 integers
		int size = oid.length + 1;
		// Compute how many actual integers the encoded octets hold
		for (int i = 1; i < oid.length; i++)
			if (oid[i] >= 128)
				size--;

		int[] result = new int[size];
		// Now convert encoded octets to ints
		result[0] = oid[0] / 40;
		result[1] = oid[0] % 40;
		int indexOID = 1;
		int indexResult = 2;
		while (indexOID < oid.length) {
			result[indexResult] *= 128;
			if (oid[indexOID] < 128) {
				result[indexResult] += oid[indexOID];
				indexResult++;
			} else {
				result[indexResult] += (oid[indexOID] - 128);
			}
			indexOID++;
		}
		return result;
	}

	/**
	 * Reads an ASN.1 Octet String type from the input and returns it as a byte
	 * array
	 * 
	 * @return byte[] The next Octet String object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected byte[] readOctetString() throws ASN1Exception {
		int length = readLength();
		byte[] bytes = new byte[length];
		readFully(input, bytes);
		return bytes;
	}

	/**
	 * Reads an ASN.1 Numeric String type from the input and returns it as a
	 * String
	 * 
	 * @return String The next Numeric String object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected String readNumericString() throws ASN1Exception {
		return convertToString(readOctetString());
	}

	/**
	 * Reads an ASN.1 Printable String type from the input and returns it as a
	 * String
	 * 
	 * @return String The next Printable String object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected String readPrintableString() throws ASN1Exception {
		return convertToString(readOctetString());
	}

	/**
	 * Reads an ASN.1 IA5 String type from the input and returns it as a String
	 * 
	 * @return String The next IA5 String object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected String readIA5String() throws ASN1Exception {
		return convertToString(readOctetString());
	}

	/**
	 * Reads an ASN.1 UTFString type from the input and returns it as a String
	 * 
	 * @return String The next UTFString object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected String readUTFString() throws ASN1Exception {
		String result = null;
		try {
			result = new String(readOctetString(), "UTF8");
		} catch (UnsupportedEncodingException e) {
			throw new ASN1Exception(Msg.getString("K0220", e));
		}
		return result;
	}

	/**
	 * Reads an ASN.1 T61 String type from the input and returns it as a String
	 * 
	 * @return String The next T61 String object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected String readT61String() throws ASN1Exception {
		return convertToString(readOctetString());
	}

	/**
	 * Reads an ASN.1 Videotext String type from the input and returns it as a
	 * String
	 * 
	 * @return String The next Videotext String object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected String readVideotextString() throws ASN1Exception {
		return convertToString(readOctetString());
	}

	/**
	 * Reads an ASN.1 BIT STRING type from the input and returns it as a
	 * BitString
	 * 
	 * @return BitString The next BitString object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected BitString readBitString() throws ASN1Exception {
		int length = readLength();
		int unusedBits = readByte();
		byte[] bytes = new byte[length - 1];
		readFully(input, bytes);
		return new BitString(unusedBits, bytes);
	}

	/**
	 * Reads an ASN.1 SEQUENCE type from the input and returns it as a Node[]
	 * 
	 * @return Node[] The next SEQUENCE object represented as an ASN.1 structure
	 *         in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Node[] readSequence() throws ASN1Exception {
		int length = readLength();
		int totalRead = 0;
		Vector elements = new Vector();
		nesting++;
		int seqItem = 1;
		while (totalRead < length || length == LENGTH_UNKNOWN) {
			int posBefore = input.currentPosition();
			// This is needed for recursive calls
			this.sequenceItem = seqItem;
			Node contents = readContents();
			// BER end of contents marker
			if (length == LENGTH_UNKNOWN && contents == null)
				break;
			elements.addElement(contents);
			int posAfter = input.currentPosition();
			int read = posAfter - posBefore;
			totalRead += read;
			seqItem++;
		}
		nesting--;

		if (length != LENGTH_UNKNOWN)
			// Length was known in advance. Read too many ?
			if (totalRead != length)
				throwASN1Exception();

		Node[] result = new Node[elements.size()];
		elements.copyInto(result);
		return result;
	}

	/**
	 * Reads an ASN.1 SEQUENCE type from the input and returns it as an Object[]
	 * 
	 * @return Node[] The next SEQUENCE object represented as an ASN.1 structure
	 *         in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Object[] readSequenceToObject() throws ASN1Exception {
		int length = readLength();
		int totalRead = 0;
		Vector elements = new Vector();
		nesting++;
		int seqItem = 1;
		while (totalRead < length || length == LENGTH_UNKNOWN) {
			int posBefore = input.currentPosition();
			// This is needed for recursive calls
			this.sequenceItem = seqItem;
			Object contents = readContentsToObject();
			// BER end of contents marker
			if (length == LENGTH_UNKNOWN && contents == null)
				break;
			elements.addElement(contents);
			int posAfter = input.currentPosition();
			int read = posAfter - posBefore;
			totalRead += read;
			seqItem++;
		}
		nesting--;

		if (length != LENGTH_UNKNOWN)
			// Length was known in advance. Read too many ?
			if (totalRead != length)
				throwASN1Exception();

		Object[] result = new Object[elements.size()];
		elements.copyInto(result);
		return result;
	}

	/**
	 * Reads an ASN.1 SET type from the input and returns it as a Node[]
	 * 
	 * @return Node[] The next SET object represented as an ASN.1 structure in
	 *         the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Node[] readSet() throws ASN1Exception {
		return readSequence();
	}

	/**
	 * Reads an ASN.1 SET type from the input and returns it as a Set
	 * 
	 * @return Set The next SET object represented as an ASN.1 structure in the
	 *         input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Set readSetToObject() throws ASN1Exception {
		return new Set(readSequenceToObject());
	}

	/**
	 * Reads an ASN.1 BMPString type from the input and returns it as a Node
	 * 
	 * @return BMPString The next SET object represented as an ASN.1 structure
	 *         in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected BMPString readBMPString() throws ASN1Exception {
		int length = readLength();
		byte[] bytes = new byte[length];
		readFully(input, bytes);
		String value = null;
		try {
			value = new String(bytes, "UnicodeBigUnmarked");
		} catch (UnsupportedEncodingException uee) {
			throw new ASN1Exception(Msg.getString("K018f", uee));
		}
		return new BMPString(value);
	}

	/**
	 * Reads an ASN.1 constructed type from the input and returns it as a Node[]
	 * 
	 * @return Node[] The next constructed object represented as an ASN.1
	 *         structure in the input
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected Node[] readConstructed() throws ASN1Exception {
		return readSequence();
	}

	/**
	 * Reads the tag code for the next an ASN.1 type from the input
	 * 
	 * @param node
	 *            Node[] The Node currently being read
	 * @exception ASN1Exception
	 *                If the format is incorrect
	 */
	protected void readTag(Node node) throws ASN1Exception {
		int octet = readByte();

		// *** ASN.1 numbers bits from 1 to 8
		node.elementClass = octet >> 6; // Bits 7 and 8 have class
		node.isPrimitive = (octet & 32) == 0; // Bit 6 is set
		int type = octet & 31; // Bottom 5 bits have tag number

		if (type == 31) { // high-tag number form
			type = 0;
			int next;
			do {
				next = readByte();
				// digits are n base 128
				type = type * 128 + (next & 127);
				// when the bit is not set, stop
			} while ((next & 128) != 0);
		}
		node.originalType = type;
		// Tag redirection can override this later on
		node.type = type;
	}

	/**
	 * An ASN.1 data file itself is not self-describing. There are ambiguities
	 * in the DER/BER encodings that can only be resolved by looking at the
	 * original grammar describing a given ASN.1 type.Therefore, in theory one
	 * needs a custom ASN.1 decoder for each type of input file (PKCS7, etc).
	 * This would result in more code (space) and would require more time to
	 * implement.
	 * 
	 * Due to time constraints we chose to implement a generic ASN.1 Decoder
	 * that supports DER/BER but with some sort of control when types are read
	 * and their actual contents are interpreted based on their tag codes. One
	 * example of the ambiguity that is only resolved by the grammar is a
	 * TBSCertificate in a X.509 Certificate. One of the fields (version) is
	 * 
	 * <pre>
	 *    
	 *   	version	[0] EXPLICIT Version DEFAULT v1
	 *     Version ::= INTEGER {v1(0) , v2(1), v3(2) }
	 *     
	 * </pre>
	 * 
	 * In this case, when the ASN.1 decoder reads tag number "0", it should
	 * actually interpret it as if it were the tag for an INTEGER. Different
	 * ASN.1 types have different "type redirection" in their grammars, so it
	 * will depend on the type. What is more complicated, the same tag number
	 * can be mapped to a different tag number (as above) in different parts of
	 * the structure tree. In one level [0] may map to an INTEGER, whereas in
	 * another subpart of the tree it may map to a BOOLEAN, etc.
	 * 
	 * In order to maintain the decoder simple and configurable, we define the
	 * notion of a TypeMapper. It allows clients of a decoder to configure
	 * custom tag redirection, or type mappings. This is the reason why every
	 * Node read has the original type (the one read from the input) and the
	 * final type, after remappings. For most of the nodes they will have the
	 * same value, but in some cases they will be different.
	 * 
	 * A TypeMapper allows the user to set a type value based on the original
	 * type read, the depth in the "parse tree" and the index of the element
	 * being read. Strictly speaking just the depth and teh sequence number are
	 * not enough to identify a unique point in a parse tree without
	 * ambiguities. However, we have found that in practice, for all the ASN.1
	 * structures used by JCA, this is enough. There is also the originalType
	 * parameter, which is likely to be different even if the nesting and the
	 * sequenceItem are the same.
	 */

	public static interface TypeMapper {
		/**
		 * Maps a given type/tag at a given nesting in an ASN.1 structure being
		 * decoded, for the Nth field/item read, into a new type/tag.
		 * 
		 * @param originalType
		 *            int Actual type/tag value read from the input stream
		 * @param nesting
		 *            int How many levels down from the root node we are
		 * @param sequenceItem
		 *            int Indicates that is the Nth element/subnode being read
		 *            for this level/nesting
		 * 
		 * @return int New tag/type value, the tag redirection. This is what teh
		 *         decoder will use to actually read the contents. So, [0] could
		 *         be mapped to INTEGER, for instance
		 */
		public int map(int originalType, int nesting, int sequenceItem);
	}

	/**
	 * Configue type redirection for a given nesting level in the Node tree for
	 * this decoder.
	 * 
	 * @param nestingDepth
	 *            How many levels down from the root node we are
	 * @param mapper
	 *            The user-defined mapper to use at the given nesting
	 * 
	 * @see TypeMapper
	 */
	public void configureTypeRedirection(int nestingDepth, TypeMapper mapper) {
		if (tagConfiguration == null) {
			// We don't have a data structure yet, so create one
			// extra space just previewing expansion
			tagConfiguration = new TypeMapper[nestingDepth + 5];
		}

		if (tagConfiguration.length <= nestingDepth) {
			// We have the data structure, but not big enough for all nestings
			TypeMapper[] old = tagConfiguration;
			// extra space just previewing expansion
			tagConfiguration = new TypeMapper[nestingDepth + 5];
			for (int i = 0; i < old.length; i++) {
				tagConfiguration[i] = old[i];
			}
		}
		tagConfiguration[nestingDepth] = mapper;
	}

	/**
	 * Compute the type redirection for the given node.
	 * 
	 * @param node
	 *            Node Node for which type redirection may be necessary
	 */
	private void computeTypeRedirection(Node node) {
		if (tagConfiguration == null) {
			if (node.type == 0) {
				// Type redirection for [0] EXPLICIT in PKCS#12
				try {
					readLength();
					readTag(node);
				} catch (ASN1Exception e) {
				}
			}
			return; // Not even a data structure, so return
		}

		if (nesting >= tagConfiguration.length) {
			return; // Not even an entry, so return
		}
		TypeMapper mapper = tagConfiguration[nesting];
		if (mapper == null) {
			return; // Not even a mapper, so return
		}
		node.type = mapper.map(node.originalType, nesting, sequenceItem);
	}

	/**
	 * Simply throws an ASN1Exception. Defined as a method just to save space
	 * with UTF data (message and parameter computation is shared).
	 * 
	 * @exception ASN1Exception
	 *                Always throws
	 */
	private void throwASN1Exception() throws ASN1Exception {
		throw new ASN1Exception(Msg.getString("K008b", input.currentPosition()));
	}

	/**
	 * Returns the contents object of the encoded bytes.
	 * 
	 * @param bytes
	 *            byte[] the ASN.1 encoded object
	 */
	public static Object getDecoded(byte[] bytes) throws ASN1Exception {
		if (bytes == null)
			throw new ASN1Exception(Msg.getString("K0190"));
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		ASN1Decoder dec = new ASN1Decoder(in);
		return dec.readContentsToObject();
	}

	private static String convertToString(byte[] bytes) {
		try {
			return new String(bytes, "ISO8859_1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.toString());
		}
	}
}
