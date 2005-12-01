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

package java.nio.channels.spi;


import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.InterruptibleChannel;

/**
 * This class roots the implementation of interruptable channels.
 * <p>
 * The basic usage pattern for an interruptible channel is to invoke
 * <code>begin()</code> before any IO operations, then
 * <code>end(boolean)</code> after completing the operation. The argument to
 * the end method shows whether there has been any change to the java
 * environment that is visible to the API user.
 * </p>
 * 
 */
public abstract class AbstractInterruptibleChannel implements Channel,
		InterruptibleChannel {

	private boolean isClosed = false;

	/**
	 * Default constructor.
	 */
	public AbstractInterruptibleChannel() {
		super();
	}

	/**
	 * Answers whether the channel is open.
	 * 
	 * @return true if the channel is open, and false if it is closed.
	 * @see java.nio.channels.Channel#isOpen()
	 */
	public boolean isOpen() {
		return !isClosed;
	}

	/**
	 * Closes the channel.
	 * <p>
	 * If the channel is already closed then this method has no effect,
	 * otherwise it closes the receiver via the implCloseChannel method.
	 * </p>
	 * 
	 * @see java.nio.channels.Channel#close()
	 */
	public synchronized final void close() throws IOException {
		if (!isClosed) {
			closeChannel();
			isClosed = true;
		}
	}

	/**
	 * Start an IO operation that is potentially blocking.
	 * <p>
	 * Once the operation is completed the applicaion should invoke a
	 * corresponding <code>end(boolean)</code>.
	 */
	protected synchronized final void begin() {
		// TODO
	}

	/**
	 * End an IO operation that was previously started with <code>begin()</code>.
	 * 
	 * @param success
	 *            pass true if the operation succeeded and had a side effcet on
	 *            the Java system, or false if not.
	 * @throws AsynchronousCloseException
	 *             the channel was closed while the IO operation was in
	 *             progress.
	 * @throws java.nio.channels.ClosedByInterruptException
	 *             the thread conducting the IO operation was interrupted.
	 */
	protected final void end(boolean success) throws AsynchronousCloseException {
		// TODO
	}

	/**
	 * Implements the close channel behavior.
	 * <p>
	 * Closes the channel with a guarantee that the channel is not currently
	 * closed via <code>close()</code> and that the method is thread-safe.
	 * </p>
	 * <p>
	 * any outstanding threads blocked on IO operations on this channel must be
	 * released with either a normal return code, or an
	 * <code>AsynchronousCloseException</code>.
	 * 
	 * @throws IOException
	 *             if a problem occurs closig the channel.
	 */
	protected abstract void closeChannel() throws IOException;
}
