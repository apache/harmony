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

package com.ibm.oti.util;


/**
 * This class decodes strings or byte arrays in base 64 encoding according to
 * the specification given by the RFC 1521.
 */
public class BASE64Decoder {

	private static final byte equalSign = (byte) '=';

	/**
	 * Constructs an instance of this class.
	 */
	private BASE64Decoder() {
		super();
	}

	/**
	 * This method decodes the byte array in base 64 encoding into a char array
	 * Base 64 encoding has to be according to the specification given by the
	 * RFC 1521 (5.2).
	 * 
	 * @param data
	 *            byte[] the encoded byte array
	 * @return char[] the decoded byte array
	 */
	public static byte[] decode(byte[] data) {
		int lastRealDataIndex;
		for (lastRealDataIndex = data.length - 1; data[lastRealDataIndex] == equalSign; lastRealDataIndex--) {
			// empty
		}
		// original data digit is 8 bits long, but base64 digit is 6 bits long
		int padBytes = data.length - 1 - lastRealDataIndex;
		int byteLength = data.length * 6 / 8 - padBytes;
		byte[] result = new byte[byteLength];
		// Each 4 bytes of input (encoded) we end up with 3 bytes of output
		int dataIndex = 0;
		int resultIndex = 0;
		int allBits = 0;
		// how many result chunks we can process before getting to pad bytes
		int resultChunks = (lastRealDataIndex + 1) / 4;
		for (int i = 0; i < resultChunks; i++) {
			allBits = 0;
			// Loop 4 times gathering input bits (4 * 6 = 24)
			for (int j = 0; j < 4; j++) {
				allBits = (allBits << 6) | decodeDigit(data[dataIndex++]);
			}

			// Loop 3 times generating output bits (3 * 8 = 24)
			for (int j = resultIndex + 2; j >= resultIndex; j--) {
				result[j] = (byte) (allBits & 0xff); // Bottom 8 bits
				allBits = allBits >>> 8;
			}
			resultIndex += 3; // processed 3 result bytes
		}
		// Now we do the extra bytes in case the original (non-encoded) data
		// was not multiple of 3 bytes

		switch (padBytes) {
		case 1: // 1 pad byte means 3 (4-1) extra Base64 bytes of input, 18
				// bits, of which only 16 are meaningful
			// Or: 2 bytes of result data
			allBits = 0;
			// Loop 3 times gathering input bits
			for (int j = 0; j < 3; j++) {
				allBits = (allBits << 6) | decodeDigit(data[dataIndex++]);
			}
			// NOTE - The code below ends up being equivalent to allBits =
			// allBits>>>2
			// But we code it in a non-optimized way for clarity

			// The 4th, missing 6 bits are all 0
			allBits = allBits << 6;

			// The 3rd, missing 8 bits are all 0
			allBits = allBits >>> 8;
			// Loop 2 times generating output bits
			for (int j = resultIndex + 1; j >= resultIndex; j--) {
				result[j] = (byte) (allBits & 0xff); // Bottom 8 bits
				allBits = allBits >>> 8;
			}
			break;

		case 2: // 2 pad bytes mean 2 (4-2) extra Base64 bytes of input, 12 bits
				// of data, of which only 8 are meaningful
			// Or: 1 byte of result data
			allBits = 0;
			// Loop 2 times gathering input bits
			for (int j = 0; j < 2; j++) {
				allBits = (allBits << 6) | decodeDigit(data[dataIndex++]);
			}
			// NOTE - The code below ends up being equivalent to allBits =
			// allBits>>>4
			// But we code it in a non-optimized way for clarity

			// The 3rd and 4th, missing 6 bits are all 0
			allBits = allBits << 6;
			allBits = allBits << 6;

			// The 3rd and 4th, missing 8 bits are all 0
			allBits = allBits >>> 8;
			allBits = allBits >>> 8;
			result[resultIndex] = (byte) (allBits & 0xff); // Bottom 8 bits
			break;
		}
		return result;
	}

	/**
	 * This method converts a Base 64 digit to its numeric value.
	 * 
	 * @param data
	 *            digit (character) to convert
	 * @return int value for the digit
	 */
	static int decodeDigit(byte data) {
		char charData = (char) data;
		if (charData <= 'Z' && charData >= 'A')
			return (charData - 'A');

		if (charData <= 'z' && charData >= 'a')
			return (charData - 'a' + 26);
		if (charData <= '9' && charData >= '0')
			return (charData - '0' + 52);

		switch (charData) {
		case '+':
			return 62;
		case '/':
			return 63;
		default:
			throw new IllegalArgumentException(Msg.getString("K008c", charData));
		}
	}
}
