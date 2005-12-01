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

package java.lang;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * This class, with the exception of the exec() APIs, must be implemented by the
 * vm vendor. The exec() APIs must first do any required security checks, and
 * then call com.ibm.oti.lang.SystemProcess.create(). The Runtime interface.
 */

public class Runtime {
	/**
	 * Execute progAray[0] in a seperate platform process The new process
	 * inherits the environment of the caller.
	 * 
	 * @param progArray
	 *            the array containing the program to execute as well as any
	 *            arguments to the program.
	 * @exception java.io.IOException
	 *                if the program cannot be executed
	 * @exception SecurityException
	 *                if the current SecurityManager disallows program execution
	 * @see SecurityManager#checkExec
	 */
	public Process exec(String[] progArray) throws java.io.IOException {
		return null;
	}

	/**
	 * Execute progArray[0] in a seperate platform process The new process uses
	 * the environment provided in envp
	 * 
	 * @param progArray
	 *            the array containing the program to execute a well as any
	 *            arguments to the program.
	 * @param envp
	 *            the array containing the environment to start the new process
	 *            in.
	 * @exception java.io.IOException
	 *                if the program cannot be executed
	 * @exception SecurityException
	 *                if the current SecurityManager disallows program execution
	 * @see SecurityManager#checkExec
	 */
	public Process exec(String[] progArray, String[] envp)
			throws java.io.IOException {
		return null;
	}

	/**
	 * Execute progArray[0] in a seperate platform process The new process uses
	 * the environment provided in envp
	 * 
	 * @param progArray
	 *            the array containing the program to execute a well as any
	 *            arguments to the program.
	 * @param envp
	 *            the array containing the environment to start the new process
	 *            in.
	 * @exception java.io.IOException
	 *                if the program cannot be executed
	 * @exception SecurityException
	 *                if the current SecurityManager disallows program execution
	 * @see SecurityManager#checkExec
	 */
	public Process exec(String[] progArray, String[] envp, File directory)
			throws java.io.IOException {
		return null;
	}

	/**
	 * Execute program in a seperate platform process The new process inherits
	 * the environment of the caller.
	 * 
	 * @param prog
	 *            the name of the program to execute
	 * @exception java.io.IOException
	 *                if the program cannot be executed
	 * @exception SecurityException
	 *                if the current SecurityManager disallows program execution
	 * @see SecurityManager#checkExec
	 */
	public Process exec(String prog) throws java.io.IOException {
		return null;
	}

	/**
	 * Execute prog in a seperate platform process The new process uses the
	 * environment provided in envp
	 * 
	 * @param prog
	 *            the name of the program to execute
	 * @param envp
	 *            the array containing the environment to start the new process
	 *            in.
	 * @exception java.io.IOException
	 *                if the program cannot be executed
	 * @exception SecurityException
	 *                if the current SecurityManager disallows program execution
	 * @see SecurityManager#checkExec
	 */
	public Process exec(String prog, String[] envp) throws java.io.IOException {
		return null;
	}

	/**
	 * Execute prog in a seperate platform process The new process uses the
	 * environment provided in envp
	 * 
	 * @param prog
	 *            the name of the program to execute
	 * @param envp
	 *            the array containing the environment to start the new process
	 *            in.
	 * @param directory
	 *            the initial directory for the subprocess, or null to use the
	 *            directory of the current process
	 * @exception java.io.IOException
	 *                if the program cannot be executed
	 * @exception SecurityException
	 *                if the current SecurityManager disallows program execution
	 * @see SecurityManager#checkExec
	 */
	public Process exec(String prog, String[] envp, File directory)
			throws java.io.IOException {
		return null;
	}

	/**
	 * Causes the virtual machine to stop running, and the program to exit. If
	 * runFinalizersOnExit(true) has been invoked, then all finalizers will be
	 * run first.
	 * 
	 * @param code
	 *            the return code.
	 * @exception SecurityException
	 *                if the running thread is not allowed to cause the vm to
	 *                exit.
	 * @see SecurityManager#checkExit
	 */
	public void exit(int code) {
		return;
	}

	/**
	 * Answers the amount of free memory resources which are available to the
	 * running program.
	 * 
	 */
	public long freeMemory() {
		return 0L;
	};

	/**
	 * Indicates to the virtual machine that it would be a good time to collect
	 * available memory. Note that, this is a hint only.
	 * 
	 */
	public void gc() {
		return;
	};

	/**
	 * Return the single Runtime instance
	 * 
	 */
	public static Runtime getRuntime() {
		return null;
	}

	/**
	 * Loads and links the library specified by the argument.
	 * 
	 * @param pathName
	 *            the absolute (ie: platform dependent) path to the library to
	 *            load
	 * @exception UnsatisfiedLinkError
	 *                if the library could not be loaded
	 * @exception SecurityException
	 *                if the library was not allowed to be loaded
	 */
	public void load(String pathName) {
		return;
	}

	/**
	 * Loads and links the library specified by the argument.
	 * 
	 * @param libName
	 *            the name of the library to load
	 * @exception UnsatisfiedLinkError
	 *                if the library could not be loaded
	 * @exception SecurityException
	 *                if the library was not allowed to be loaded
	 */
	public void loadLibrary(String libName) {
		return;
	}

	/**
	 * Provides a hint to the virtual machine that it would be useful to attempt
	 * to perform any outstanding object finalizations.
	 * 
	 */
	public void runFinalization() {
		return;
	};

	/**
	 * Ensure that, when the virtual machine is about to exit, all objects are
	 * finalized. Note that all finalization which occurs when the system is
	 * exiting is performed after all running threads have been terminated.
	 * 
	 * @param run
	 *            true means finalize all on exit.
	 * @deprecated This method is unsafe.
	 */
	public static void runFinalizersOnExit(boolean run) {
		return;
	};

	/**
	 * Answers the total amount of memory resources which is available to (or in
	 * use by) the running program.
	 * 
	 */
	public long totalMemory() {
		return 0L;
	};

	public void traceInstructions(boolean enable) {
		return;
	}

	public void traceMethodCalls(boolean enable) {
		return;
	}

	/**
	 * @deprecated Use InputStreamReader
	 */
	public InputStream getLocalizedInputStream(InputStream stream) {
		return null;
	}

	/**
	 * @deprecated Use OutputStreamWriter
	 */
	public OutputStream getLocalizedOutputStream(OutputStream stream) {
		return null;
	}

	/**
	 * Registers a new virtual-machine shutdown hook.
	 * 
	 * @param hook
	 *            the hook (a Thread) to register
	 */
	public void addShutdownHook(Thread hook) {
		return;
	}

	/**
	 * De-registers a previously-registered virtual-machine shutdown hook.
	 * 
	 * @param hook
	 *            the hook (a Thread) to de-register
	 * @return true if the hook could be de-registered
	 */
	public boolean removeShutdownHook(Thread hook) {
		return false;
	}

	/**
	 * Causes the virtual machine to stop running, and the program to exit.
	 * Finalizers will not be run first. Shutdown hooks will not be run.
	 * 
	 * @param code
	 *            the return code.
	 * @exception SecurityException
	 *                if the running thread is not allowed to cause the vm to
	 *                exit.
	 * @see SecurityManager#checkExit
	 */
	public void halt(int code) {
		return;
	}

	/**
	 * Return the number of processors, always at least one.
	 */
	public int availableProcessors() {
		return 0;
	}

	/**
	 * Return the maximum memory that will be used by the virtual machine, or
	 * Long.MAX_VALUE.
	 */
	public long maxMemory() {
		return 0L;
	}

}
