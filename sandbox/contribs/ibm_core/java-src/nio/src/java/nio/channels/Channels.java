/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package java.nio.channels;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import com.ibm.misc.util.NotYetImplementedException;

/**
 * Provides convenience methods to java.io package streams.
 * 
 */
public final class Channels {

	/*
	 * Not intended to be instantiated. 
	 */
	private Channels() {
		super();
	}

	/**
	 * Answers an input stream on the given channel.
	 * 
	 * @param channel the channel to be wrapped in an InputStream.
	 * @return an InputStream that takes bytes from the given byte channel.
	 */
	public static InputStream newInputStream(ReadableByteChannel channel) {
		// TODO
		throw new NotYetImplementedException();
	}

	/**
	 * Answers an output stream on the given channel.
	 * 
	 * @param channel the channel to be wrapped in an OutputStream.
	 * @return an OutputStream that puts bytes onto the given byte channel.
	 */
	public static OutputStream newOutputStream(WritableByteChannel channel) {
		// TODO
		throw new NotYetImplementedException();
	}

	/**
	 * Answers a channel on the given input stream.
	 * @param inputStream the stream to be wrapped in a byte channel.
	 * @return a byte channel that reads bytes from the input stream.
	 */
	public static ReadableByteChannel newChannel(InputStream inputStream) {
		//TODO
		throw new NotYetImplementedException();
	}

	/**
	 * Answers a channel on the given output stream.
	 * @param outputStream the stream to be wrapped in a byte channel.
	 * @return a byte channel that writes bytes to the output stream.
	 */
	public static WritableByteChannel newChannel(OutputStream outputStream) {
		//TODO
		throw new NotYetImplementedException();
	}

	public static Reader newReader(ReadableByteChannel channel,
			CharsetDecoder decoder, int minBufferCapacity) {
		//TODO
		throw new NotYetImplementedException();
	}

	public static Reader newReader(ReadableByteChannel channel,
			String charsetName) {
		return newReader(channel, Charset.forName(charsetName).newDecoder(), -1);
	}

	public static Writer newWriter(WritableByteChannel channel,
			CharsetEncoder encoder, int minBufferCapacity) {
		// TODO
		throw new NotYetImplementedException();
	}

	public static Writer newWriter(WritableByteChannel channel,
			String charsetName) {
		return newWriter(channel, Charset.forName(charsetName).newEncoder(), -1);
	}
}
