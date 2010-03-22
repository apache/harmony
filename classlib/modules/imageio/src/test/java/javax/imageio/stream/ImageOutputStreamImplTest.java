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
import java.util.Arrays;

import javax.imageio.stream.ImageInputStreamImplTest.BasicImageInputStreamImpl;

import junit.framework.TestCase;

public class ImageOutputStreamImplTest extends TestCase {

	public void testWriteShot() throws IOException {
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(2);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeShort(Short.MAX_VALUE);
		assertEquals(Short.MAX_VALUE, in.readShort());

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeShort(Short.MAX_VALUE);
		assertEquals(Short.MAX_VALUE, in.readShort());
	}

	public void testWriteInt() throws IOException {
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(4);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeInt(Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, in.readInt());

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeInt(Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, in.readInt());
	}

	public void testWriteLong() throws IOException {
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(8);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeLong(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, in.readLong());

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeLong(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, in.readLong());
	}

	public void testWriteChars() throws IOException {
		final char[] buff = new char[4];
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				2 * buff.length);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeChars("test");
		in.readFully(buff, 0, 4);
		assertEquals("test", new String(buff));

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeChars("test");
		in.readFully(buff, 0, 4);
		assertEquals("test", new String(buff));

		out.reset();
		in.reset();
		out.writeChars(" test".toCharArray(), 1, 4);
		in.readFully(buff, 0, 4);
		assertEquals("test", new String(buff));
	}

	public void testWriteShorts() throws IOException {
		final short[] src = new short[] { 1, 2, 3 };
		final short[] dest = new short[3];
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				2 * dest.length);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeShorts(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeShorts(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));
	}

	public void testWriteInts() throws IOException {
		final int[] src = new int[] { 1, 2, 3 };
		final int[] dest = new int[3];
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				4 * dest.length);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeInts(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeInts(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));
	}

	public void testWriteLongs() throws IOException {
		final long[] src = new long[] { 1, 2, 3 };
		final long[] dest = new long[3];
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				8 * dest.length);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeLongs(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeLongs(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));
	}

	public void testWriteFloats() throws IOException {
		final float[] src = new float[] { 1, 2, 3 };
		final float[] dest = new float[3];
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				4 * dest.length);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeFloats(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeFloats(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));
	}

	// FIXME: it looks like there is a bug in Double.doubleToLongBits
	public void _testWriteDoubles() throws IOException {
		final double[] src = new double[] { 1, 2, 3 };
		final double[] dest = new double[3];
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				8 * dest.length);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeDoubles(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));

		out.reset();
		in.reset();
		out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		out.writeDoubles(src, 0, 3);
		in.readFully(dest, 0, 3);
		assertTrue(Arrays.equals(src, dest));
	}

	public void testWriteUTF() throws IOException {
		final BasicImageOutputStreamImpl out = new BasicImageOutputStreamImpl(
				100);
		final ImageInputStream in = new BasicImageInputStreamImpl(out.buff);

		out.writeUTF("test");
		assertEquals("test", in.readUTF());

		// FIXME: fails with ByteOrder.LITTLE_ENDIAN
		// out.reset();
		// in.reset();
		// out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		// in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		// out.writeUTF("test");
		// assertEquals("test", in.readUTF());
	}

	static class BasicImageOutputStreamImpl extends ImageOutputStreamImpl {

		byte[] buff;

		public BasicImageOutputStreamImpl(final int capacity) {
			this(capacity, ByteOrder.BIG_ENDIAN);
		}

		public BasicImageOutputStreamImpl(final int capacity,
				final ByteOrder order) {
			buff = new byte[capacity];
			setByteOrder(order);
		}

		@Override
		public void write(int b) throws IOException {
			buff[(int) streamPos++] = (byte) b;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			System.arraycopy(b, off, buff, (int) streamPos, len);
			streamPos += len;
		}

		@Override
		public void reset() throws IOException {
			super.reset();
			streamPos = 0;
			bitOffset = 0;
		}

		@Override
		public int read() throws IOException {
			throw new RuntimeException("Write only");
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			throw new RuntimeException("Write only");
		}
	}
}
