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

package com.ibm.oti.lang;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class provides the exec() implementation required for java.lang.Runtime.
 * 
 * Instances of class Process provide control of and access to platform
 * processes. This is the concrete implementation of class java.lang.Process.
 * 
 * @see java.lang.Runtime
 */
public class SystemProcess extends java.lang.Process {
	InputStream err; // STDERR for the process

	InputStream out; // STDOUT for the process

	OutputStream in; // STDIN for the process

	long handle = -1; // Handle to OS process struct

	/*
	 * When exitCodeAvailable == 1, exitCode has a meaning. When exitCode is
	 * available, it means that the underlying process had finished (for sure).
	 */
	boolean exitCodeAvailable = false;

	int exitCode;

	Object lock;

	boolean waiterStarted = false;

	Throwable exception = null;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization();

	static {
		oneTimeInitialization();
	}

	/**
	 * Prevents this class from being instantiated.
	 */
	private SystemProcess() {
	}

	/**
	 * Answers a Process hooked to an OS process.
	 * 
	 * @param progArray
	 *            the array containing the program to execute as well as any
	 *            arguments to the program.
	 * @param envp
	 *            the array containing the environment to start the new process
	 *            in.
	 * @param directory
	 *            the directory to start the process in, or null
	 * 
	 * @return a java.lang.Process
	 * 
	 * @throws IOException
	 *             when a problem occurs
	 * @throws NullPointerException
	 *             when progArray or envp are null, or contain a null element in
	 *             the array.
	 */
	public static Process create(String[] progArray, String[] envp,
			final File directory) throws IOException {
		final byte[][] progBytes, envBytes;
		progBytes = new byte[progArray.length][];
		for (int i = 0; i < progArray.length; i++)
			progBytes[i] = com.ibm.oti.util.Util.getBytes(progArray[i]);
		envBytes = new byte[envp.length][];
		for (int i = 0; i < envp.length; i++)
			envBytes[i] = com.ibm.oti.util.Util.getBytes(envp[i]);

		final SystemProcess p = new SystemProcess();

		p.lock = new Object();

		Runnable waitingThread = new Runnable() {
			public void run() {
				long[] procVals = null;
				try {
					procVals = createImpl(p, progBytes, envBytes,
							directory == null ? null : com.ibm.oti.util.Util
									.getBytes(directory.getPath()));
				} catch (Throwable e) {
					/* Creation errors need to be passed to the user thread. */
					synchronized (p.lock) {
						p.exception = e;
						p.waiterStarted = true;
						p.lock.notifyAll();
					}
					return;
				}
				p.handle = procVals[0];
				p.in = new ProcessOutputStream(procVals[1]);
				p.out = new ProcessInputStream(procVals[2]);
				p.err = new ProcessInputStream(procVals[3]);

				synchronized (p.lock) {
					p.waiterStarted = true;
					p.lock.notifyAll();
				}

				p.exitCode = p.waitForCompletionImpl();
				synchronized (p.lock) {
					p.closeImpl();
					p.handle = -1;
				}
				p.exitCodeAvailable = true;
				synchronized (p.lock) {
					try {
						p.in.close();
					} catch (IOException e) {
					}
					p.lock.notifyAll();
				}
			}
		};
		Thread wait = new Thread(waitingThread);
		wait.setDaemon(true);
		wait.start();

		try {
			synchronized (p.lock) {
				while (!p.waiterStarted)
					p.lock.wait();
				if (p.exception != null) {
					/* Re-throw exception that originated in the helper thread */
					p.exception.fillInStackTrace();
					if (p.exception instanceof IOException)
						throw (IOException) p.exception;
					else if (p.exception instanceof Error)
						throw (Error) p.exception;
					else
						throw (RuntimeException) p.exception;
				}
			}
		} catch (InterruptedException e) {
			throw new InternalError();
		}

		return p;
	}

	protected synchronized static native long[] createImpl(Process p,
			byte[][] progArray, byte[][] envp, byte[] dir)
			throws java.io.IOException;

	/**
	 * Stops the process associated with the receiver.
	 */
	public void destroy() {
		synchronized (lock) {
			if (handle != -1)
				destroyImpl();
		}
	}

	/**
	 * Internal implementation of the code that stops the process associated
	 * with the receiver.
	 */
	private native void destroyImpl();

	/**
	 * Internal implementation of the code that closes the handle.
	 */
	native void closeImpl();

	/**
	 * Answers the exit value of the receiving Process. It is available only
	 * when the OS subprocess is finished.
	 * 
	 * @return int The exit value of the process.
	 */
	public int exitValue() {
		synchronized (lock) {
			if (!exitCodeAvailable)
				throw new IllegalThreadStateException();
			return exitCode;
		}
	}

	/**
	 * Answers the receiver's error output stream.
	 * <p>
	 * Note: This is an InputStream which allows reading of the other process'
	 * "stderr".
	 * 
	 * @return InputStream The receiver's process' stderr.
	 */
	public java.io.InputStream getErrorStream() {
		return err;
	}

	/**
	 * Answers the receiver's standard output stream.
	 * <p>
	 * Note: This is an InputStream which allows reading of the other process'
	 * "stdout".
	 * 
	 * @return InputStream The receiver's process' stdout.
	 */
	public java.io.InputStream getInputStream() {
		return out;
	}

	/**
	 * Answers the receiver's standard input stream
	 * <p>
	 * Note: This is an OutputStream which allows writing to the other process'
	 * "stdin".
	 * 
	 * @return OutputStream The receiver's process' stdout.
	 */
	public java.io.OutputStream getOutputStream() {
		return in;
	}

	/**
	 * Causes the calling thread to wait for the process associated with the
	 * receiver to finish executing.
	 * 
	 * @throws InterruptedException
	 *             If the calling thread is interrupted
	 * 
	 * @return int The exit value of the Process being waited on
	 */
	public int waitFor() throws InterruptedException {
		synchronized (lock) {
			/*
			 * if the exitCode is available, it means that the underlying OS
			 * process is already dead, so the exitCode is just returned whitout
			 * any other OS checks
			 */
			while (!exitCodeAvailable)
				lock.wait();
			return exitCode;
		}
	}

	/**
	 * Internal implementation of the code that waits for the process to
	 * complete.
	 * 
	 * @return int The exit value of the process.
	 */
	native int waitForCompletionImpl();
}
