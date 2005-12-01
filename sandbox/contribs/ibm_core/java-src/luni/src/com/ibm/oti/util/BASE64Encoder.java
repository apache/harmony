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
 * This class encodes strings or byte arrays into base 64 according to the the
 * specification given by the RFC 1521 (5.2).
 */
public class BASE64Encoder {
	static char digits[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
			'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
			'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'+', '/' };

	/**
	 * Constructs an instance of this class.
	 */
	private BASE64Encoder() {
		super();
	}

	/**
	 * This method encodes the byte array into a char array in base 64 according
	 * to the specification given by the RFC 1521 (5.2).
	 * 
	 * @param data
	 *            char[] the encoded char array
	 * @return byte[] the byte array that needs to be encoded
	 */
	public static byte[] encode(byte[] data) {
		int sourceChunks = data.length / 3;
		int len = ((data.length + 2) / 3) * 4;
		byte[] result = new byte[len];
		int extraBytes = data.length - (sourceChunks * 3);
		// Each 4 bytes of input (encoded) we end up with 3 bytes of output
		int dataIndex = 0;
		int resultIndex = 0;
		int allBits = 0;
		for (int i = 0; i < sourceChunks; i++) {
			allBits = 0;
			// Loop 3 times gathering input bits (3 * 8 = 24)
			for (int j = 0; j < 3; j++) {
				allBits = (allBits << 8) | (data[dataIndex++] & 0xff);
			}

			// Loop 4 times generating output bits (4 * 6 = 24)
			for (int j = resultIndex + 3; j >= resultIndex; j--) {
				result[j] = (byte) digits[(allBits & 0x3f)]; // Bottom 6 bits
				allBits = allBits >>> 6;
			}
			resultIndex += 4; // processed 4 result bytes
		}
		// Now we do the extra bytes in case the original (non-encoded) data
		// is not multiple of 4 bytes
		switch (extraBytes) {
		case 1:
			allBits = data[dataIndex++]; // actual byte
			allBits = allBits << 8; // 8 bits of zeroes
			allBits = allBits << 8; // 8 bits of zeroes
			// Loop 4 times generating output bits (4 * 6 = 24)
			for (int j = resultIndex + 3; j >= resultIndex; j--) {
				result[j] = (byte) digits[(allBits & 0x3f)]; // Bottom 6 bits
				allBits = allBits >>> 6;
			}
			// 2 pad tags
			result[result.length - 1] = (byte) '=';
			result[result.length - 2] = (byte) '=';
			break;
		case 2:
			allBits = data[dataIndex++]; // actual byte
			allBits = (allBits << 8) | (data[dataIndex++] & 0xff); // actual
																	// byte
			allBits = allBits << 8; // 8 bits of zeroes
			// Loop 4 times generating output bits (4 * 6 = 24)
			for (int j = resultIndex + 3; j >= resultIndex; j--) {
				result[j] = (byte) digits[(allBits & 0x3f)]; // Bottom 6 bits
				allBits = allBits >>> 6;
			}
			// 1 pad tag
			result[result.length - 1] = (byte) '=';
			break;
		}
		return result;
	}
}
