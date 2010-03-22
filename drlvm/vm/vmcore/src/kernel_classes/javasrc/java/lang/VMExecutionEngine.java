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
/**
 * @author Evgueni Brevnov, Roman S. Bushmanov
 */
package java.lang;

import java.util.Properties;
import java.util.Vector;

/**
 * Provides the methods to interact with VM Execution Engine that are used by
 * different classes from the <code>java.lang</code> package, such as System,
 * Runtime.
 * <p>
 * This class must be implemented according to the common policy for porting
 * interfaces - see the porting interface overview for more detailes.
 * 
 * @api2vm
 */
final class VMExecutionEngine {

    /**
     * keeps Runnable objects of the shutdown sequence 
     */
    private static Vector<Runnable> shutdownActions = new Vector<Runnable>(); 

    /**
     * This class is not supposed to be instantiated.
     */
    private VMExecutionEngine() {
    }

    /**
     * Terminates a Virtual Machine with possible invocation of finilization
     * methods. This method is used by the {@link Runtime#exit(int)
     * Runtime.exit(int status)} and {@link Runtime#halt(int)
     * Runtime.halt(int satus)} methods implementation. When it is called by the
     * <code>Runtime.exit(int status)</code> method all shutdown hook threads
     * should have been already finished. The needFinilization argument must
     * be true if uninvoked finilizers should be called on VM exit. The
     * implementation simply calls the 
     * {@link VMExecutionEngine#exit(int, boolean, Runnable[]) 
     * VMExecutionEngine.exit(int status, boolean needFinalization, 
     * Runnable[] shutdownSequence)} method with an array of
     * <code>Runnable</code> objects which were registered before by means of
     * the {@link VMExecutionEngine#registerShutdownAction(Runnable) 
     * VMExecutionEngine.registerShutdownAction(Runnable action)} method.  
     * 
     * @param status exit status
     * @param needFinalize specifies that finalization must be performed. If
     *        true then it perfoms finalization of all not finalized objects
     *        that have finalizers
     * @api2vm
     */
    static void exit(int status, boolean needFinalization) {
        exit(status, needFinalization, shutdownActions.toArray(new Runnable[0]));
    }


    /**
     * Call to this method forces VM to
     * <ol>
     *   <li> Execute uninvoked finilizers if needFinalization is true </li>
     *   <li> Forcibly stop all running non-system threads
     *   <li> Sequentially execute the <code>run</code> method for each object
     *        of the shutdownSequence array. The execution starts from the last
     *        element of the array. No threads will be created to perform
     *        execution. If uncatched exception occurs it's stack trace is
     *        printed to the error stream and the next element of the array if
     *        any will be executed
     *   <li> Exit  
     * </ol>    
     * 
     * @param status exit status
     * @param needFinalization indicates that finilization should be performed 
     * @param shutdownSequence array of shutdown actions
     * @api2vm
     */
    private static native void exit(int status, boolean needFinalization,
                                    Runnable[] shutdownSequence);
    
    /**
     * This method provides an information about the assertion status specified
     * via command line options.
     * <p>
     *  
     * @see java.lang.Class#desiredAssertionStatus()
     * 
     * @param clss the class to be initialized with the assertion status. Note, 
     * assertion status is applicable to top-level classes only, therefore
     * any member/local class passed is a subject to conversion to corresponding
     * top-level declaring class. Also, <code>null</code> argument can be used to 
     * check if any assertion was specified through command line options.
     * @param recursive controls whether this method should check exact match
     * with name of the class, or check (super)packages recursively 
     * (most specific one has precedence).
     * @param defaultStatus if no specific package setting found, 
     * this value may override command-line defaults. This parameter is
     * actual only when <code>recursive == true</code>. 
     * @see java.lang.ClassLoader#setDefaultAssertionStatus(boolean) 
     * @return 0 - unspecified, &lt; 0 - false, &gt; 0 - true
     * @api2vm
     */
    static native int getAssertionStatus(Class clss, boolean recursive, 
            int defaultStatus);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#availableProcessors() Runtime.availableProcessors()}
     * method.
     * @api2vm
     */
    static native int getAvailableProcessors();

    /**
     * This method satisfies the requirements of the specification for the
     * {@link System#getProperties() System.getProperties()} method.
     * <p>
     * Additionally a class library implementation may relay on existance of
     * the following properties "vm.boot.class.path" & "vm.boot.library.path".
     * The "vm.boot.class.path" property can be used to load classes and
     * resources which reside in the bootstrap sequence of the VM.
     * The "vm.boot.library.path" property can be used to find libraries that
     * should be obtatined by classes which were loaded by bootstrap class loader.      
     * @api2vm
     */
    static native Properties getProperties();

    /**
     * Adds the specified action to the list of shutdown actions. The
     * {@link Runnable#run() Runnable.run()} method of the specified action 
     * object is executed after all non-system threads have been stopped. Last
     * registered action is executed before previously registered actions. Each
     * action should not create threads inside. It is expected that registered 
     * actions doesn't requre a lot of time to complete.
     * <p>
     * Typicily one may use this method to close open files, connections etc. on
     * Virtual Machine exit       
     * @param action action which should be performed on VM exit
     * @api2vm
     */
    public static void registerShutdownAction(Runnable action) {
        shutdownActions.add(action);
    }

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#traceInstructions(boolean)
     * Runtime.traceInstructions(boolean on)} method.
     * @api2vm
     */
    static native void traceInstructions(boolean enable);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#traceMethodCalls(boolean)
     * Runtime.traceMethodCalls(boolean on)} method.
     * @api2vm
     */
    static native void traceMethodCalls(boolean enable);

    /**
     * Returns the current system time in milliseconds since 
     * the Unix epoch (midnight, 1 Jan, 1970).
     * @api2vm
     */
    static native long currentTimeMillis();

    /**
     * Returns the current value of a system timer with the best accuracy
     * the OS can provide, in nanoseconds.
     * @api2vm
     */
    static native long nanoTime();
    
    /**
     * Returns platform-specific name of the specified library.
     * @api2vm
     */
    static native String mapLibraryName(String libname);
}
