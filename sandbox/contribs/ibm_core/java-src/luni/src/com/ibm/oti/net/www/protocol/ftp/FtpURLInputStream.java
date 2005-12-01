/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.oti.net.www.protocol.ftp;


import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * This class associates a given inputStream with a control socket. This ensures
 * the control socket Object stays live while the stream is in use
 */
class FtpURLInputStream extends InputStream {

	InputStream is; // Actual input stream

	java.net.Socket controlSocket;

	public FtpURLInputStream(InputStream is, Socket controlSocket) {
		this.is = is;
		this.controlSocket = controlSocket;
	}

	public int read() throws IOException {
		return is.read();
	}

	public int read(byte[] buf, int off, int nbytes) throws IOException {
		return is.read(buf, off, nbytes);
	}

	public synchronized void reset() throws IOException {
		is.reset();
	}

	public synchronized void mark(int limit) {
		is.mark(limit);
	}

	public boolean markSupported() {
		return is.markSupported();
	}

	public void close() {
		try {
			is.close();
		} catch (Exception e) {
			// ignored
		}
		try {
			controlSocket.close();
		} catch (Exception e) {
			//ignored
		}
	}

	public int available() throws IOException {
		return is.available();
	}

	public long skip(long sbytes) throws IOException {
		return is.skip(sbytes);
	}

}
