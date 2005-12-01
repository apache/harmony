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

/**
 * This class must be implemented by the vm vendor. This class is a placeholder
 * for environments which explicitely manage the action of a "Just In Time"
 * compiler.
 * 
 * @see Cloneable
 */
public final class Compiler {

	/**
	 * Low level interface to the JIT compiler. Can return any object, or null
	 * if no JIT compiler is available.
	 * 
	 * @return Object result of executing command
	 * @param cmd
	 *            Object a command for the JIT compiler
	 */
	public static Object command(Object cmd) {
		return null;
	}

	/**
	 * Compiles the class using the JIT compiler. Answers true if the
	 * compilation was successful, or false if it failed or there was no JIT
	 * compiler available.
	 * 
	 * @return boolean indicating compilation success
	 * @param classToCompile
	 *            java.lang.Class the class to JIT compile
	 */
	public static boolean compileClass(Class classToCompile) {
		return false;
	}

	/**
	 * Compiles all classes whose name matches the argument using the JIT
	 * compiler. Answers true if the compilation was successful, or false if it
	 * failed or there was no JIT compiler available.
	 * 
	 * @return boolean indicating compilation success
	 * @param nameRoot
	 *            String the string to match against class names
	 */
	public static boolean compileClasses(String nameRoot) {
		return false;
	}

	/**
	 * Disable the JIT compiler
	 * 
	 */
	public static void disable() {
		return;
	};

	/**
	 * Disable the JIT compiler
	 * 
	 */
	public static void enable() {
		return;
	};

}
