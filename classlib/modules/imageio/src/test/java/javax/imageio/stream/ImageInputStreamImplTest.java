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

package javax.imageio.stream;

import java.io.IOException;
import java.nio.ByteOrder;

import junit.framework.TestCase;

public class ImageInputStreamImplTest extends TestCase {

	public void testReadLine() throws IOException {
		final ImageInputStream in = new BasicImageInputStreamImpl(
				"line1\nline2\rline3\r\nline4".getBytes("ISO8859_1"));

		assertEquals("line1", in.readLine());
		assertEquals("line2", in.readLine());
		assertEquals("line3", in.readLine());
		assertEquals("line4", in.readLine());
	}

	public void testReadBit() throws IOException {
		final ImageInputStream in = new BasicImageInputStreamImpl(
				new byte[] { (byte) 150 });

		assertEquals(1, in.readBit());
		assertEquals(0, in.readBit());
		assertEquals(0, in.readBit());
		assertEquals(1, in.readBit());
		assertEquals(0, in.readBit());
		assertEquals(1, in.readBit());
		assertEquals(1, in.readBit());
		assertEquals(0, in.readBit());
	}

	public void testReadBits() throws IOException {
		final ImageInputStream in = new BasicImageInputStreamImpl(
				Long.MAX_VALUE);

		assertEquals(3, in.readBits(3));
		assertEquals(1023, in.readBits(10));
		in.reset();
		assertEquals(Long.MAX_VALUE, in.readBits(64));
	}

	public void testReadLong() throws IOException {
		ImageInputStream in = new BasicImageInputStreamImpl(Long.MAX_VALUE);

		assertEquals(Long.MAX_VALUE, in.readLong());

		in = new BasicImageInputStreamImpl(Long.MAX_VALUE,
				ByteOrder.LITTLE_ENDIAN);
		assertEquals(Long.MAX_VALUE, in.readLong());
	}

	public void testReadInt() throws IOException {
		ImageInputStream in = new BasicImageInputStreamImpl(Integer.MAX_VALUE);

		in.readInt();
		assertEquals(Integer.MAX_VALUE, in.readInt());

		in = new BasicImageInputStreamImpl(Integer.MAX_VALUE,
				ByteOrder.LITTLE_ENDIAN);
		assertEquals(Integer.MAX_VALUE, in.readInt());
	}

	public void testReadShort() throws IOException {
		ImageInputStream in = new BasicImageInputStreamImpl(Short.MAX_VALUE);

		in.readInt();
		in.readShort();
		assertEquals(Short.MAX_VALUE, in.readShort());

		in = new BasicImageInputStreamImpl(Short.MAX_VALUE,
				ByteOrder.LITTLE_ENDIAN);
		assertEquals(Short.MAX_VALUE, in.readShort());
	}

	static class BasicImageInputStreamImpl extends ImageInputStreamImpl {
		final byte[] buff;

		public BasicImageInputStreamImpl(final long value) {
			this(value, ByteOrder.BIG_ENDIAN);
		}

		public BasicImageInputStreamImpl(final long value, final ByteOrder order) {
			if (order == ByteOrder.BIG_ENDIAN) {
				buff = new byte[] { (byte) (value >> 56), (byte) (value >> 48),
						(byte) (value >> 40), (byte) (value >> 32),
						(byte) (value >> 24), (byte) (value >> 16),
						(byte) (value >> 8), (byte) (value) };
			} else {
				buff = new byte[] { (byte) value, (byte) (value >> 8),
						(byte) (value >> 16), (byte) (value >> 24),
						(byte) (value >> 32), (byte) (value >> 40),
						(byte) (value >> 48), (byte) (value >> 56) };
			}
			setByteOrder(order);
		}

		public BasicImageInputStreamImpl(final byte[] buff) {
			this.buff = buff;
		}

		@Override
		public int read() throws IOException {
			bitOffset = 0;
			return (streamPos >= buff.length) ? -1
					: (buff[(int) streamPos++] & 0xff);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int i = 0;
			int curByte = -1;

			for (; (i < len) && ((curByte = read()) != -1); i++) {
				b[off] = (byte) curByte;
				off++;
			}

			return (i == 0) && (curByte == -1) ? -1 : i;
		}

		@Override
		public void reset() throws IOException {
			super.reset();
			streamPos = 0;
			bitOffset = 0;
		}
	}
}
