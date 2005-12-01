/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
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


import java.io.IOException;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * TODO Type description
 * 
 */
public abstract class SelectableChannel extends AbstractInterruptibleChannel
		implements Channel {

	protected SelectableChannel() {
		super();
	}

	public abstract Object blockingLock();

	public abstract SelectableChannel configureBlocking(boolean block)
			throws IOException;

	public abstract boolean isBlocking();

	public abstract boolean isRegistered();

	public abstract SelectionKey keyFor(Selector sel);

	public abstract SelectorProvider provider();

	public final SelectionKey register(Selector selector, int operations)
			throws ClosedChannelException {
		return register(selector, operations, null);
	}

	public abstract SelectionKey register(Selector sel, int ops, Object att)
			throws ClosedChannelException;

	public abstract int validOps();

}
