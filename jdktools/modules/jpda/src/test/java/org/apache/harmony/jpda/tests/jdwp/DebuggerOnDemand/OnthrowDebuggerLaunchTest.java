/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Aleksander V. Budniy
 */

/**
 * Created on 5.06.2006
 */
package org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPRawTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeProcessWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.JPDATestOptions;

/**
 * This test case exercises possibility of debugge to invoke debugger on demand with "onthrow" option.
 * 
 * @see org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthowDebuggerLaunchDebuggee
 * @see org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthrowLaunchDebugger001
 * @see org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthrowLaunchDebugger002
 */
public class OnthrowDebuggerLaunchTest extends JDWPRawTestCase {

    public static final String EXCEPTION_CLASS_FOR_DEBUGGER = 
                   "org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.ExceptionForDebugger";

    public static final String DEBUGGEE_CLASS = 
                   "org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthowDebuggerLaunchDebuggee";

    /**
     * Test launches debuggee (without establishing synchronization connection)
     * with options suspend=y,onuncaught=n, debuggee executes
     * <code>OnthrowLaunchedDebugger001</code>, test establishes synch
     * connection with debugger and in cycle receives messages from debugger.
     */
    public void testDebuggerLaunch001() {
        logWriter.println("==> testDebuggerLaunch started");

        String DEBUGGER_NAME = "org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthrowLaunchDebugger001";
        String isSuspend = "y";
        String isOnuncaught = "n";

        performTest(DEBUGGER_NAME, isSuspend, isOnuncaught);

        logWriter.println("==> testDebuggerLaunch ended");
    }

    /**
     * Test launches debuggee (without establishing synchronization connection)
     * with option suspend=y,onuncaught=n, debuggee executes
     * <code>OnthrowLaunchedDebugger002</code>, test establishes synch
     * connection with debugger and in cycle receives messages from debugger.
     * 
     */

    public void testDebuggerLaunch002() {
        logWriter.println("==> testDebuggerLaunch002 started");

        String DEBUGGER_NAME = "org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthrowLaunchDebugger002";
        String isSuspend = "y";
        String isOnuncaught = "n";

        performTest(DEBUGGER_NAME, isSuspend, isOnuncaught);

        logWriter.println("==> testDebuggerLaunch002 ended");
    }

    
    /**
     * Test launches debuggee (without establishing synchronization connection)
     * with option suspend=n,onuncaught=n debuggee executes
     * <code>OnthrowLaunchedDebugger001</code>, test establishes synch
     * connection with debugger and in cycle receives messages from debugger.
     * 
     */
    public void testDebuggerLaunch003() {
        logWriter.println("==> testDebuggerLaunch started");

        String DEBUGGER_NAME = "org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthrowLaunchDebugger001";
        String isSuspend = "n";
        String isOnuncaught = "n";

        performTest(DEBUGGER_NAME, isSuspend, isOnuncaught);

        logWriter.println("==> testDebuggerLaunch ended");
    }
    
    /**
     * Test executes debuggee (without establishing synchronization connection)
     * with option suspend=n,onuncaught=n debuggee executes
     * <code>OnthrowLaunchedDebugger002</code>, test establishes synch
     * connection with debugger and in cycle receives messages from debugger.
     * 
     */
    public void testDebuggerLaunch004() {
        logWriter.println("==> testDebuggerLaunch started");

        String DEBUGGER_NAME = "org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthrowLaunchDebugger002";
        String isSuspend = "n";
        String isOnuncaught = "n";

        performTest(DEBUGGER_NAME, isSuspend, isOnuncaught);

        logWriter.println("==> testDebuggerLaunch ended");
    }

    /**
     * Method prepares cmd for launching debuggee and debugger. Passes
     * debugger's cmd as parameter "launch" to debuggee's cmd. Then launches
     * debuggee and wait for synch connection from debugger, that should be
     * launched by debuggee. After that, method starts receiving messages in
     * loop from debugger. Messages of three types: OK, FAIL, END. In case of
     * FAIL or END messages, loop is ended.
     * 
     * @param DEBUGGER_NAME
     *            name of debugger that debuggee will launch
     * @param isSuspendDebuggee
     *            option defines should debuggee be suspended on start
     * @param isOnuncaught
     *            parameter that is passed to debuggee (see JDWP agent launch
     *            options)
     */
    void performTest(String debuggerName, String isSuspendDebuggee, String isOnuncaught) {

        try {
	        // prepare command line for debugger and debuggee processes
	
	        String address = settings.getTransportAddress();
	        String debuggerCmd = prepareDebuggerCmd(debuggerName, address, 
                    isSuspendDebuggee, isOnuncaught);
	        logWriter.println("=> Debugger command: " + debuggerCmd);
	
	        String debuggeeCmd = prepareDebuggeeCmd(debuggerCmd, address, 
	                                      isSuspendDebuggee, isOnuncaught);
	        logWriter.println("=> Debuggee command: " + debuggeeCmd);
	
	        // launch debuggee process, which will launch debugger process 
	
	        debuggeeWrapper.launchProcessAndRedirectors(debuggeeCmd);
	
	        // listen for synch connection from launched debugger 
	
	        logWriter.println("=> Listen for synch connection from launched debugger");
	        debuggerSynchronizer.startServer();
	        logWriter.println("=> Synch connection with launched debugger established");
        } catch (Exception e) {
            throw new TestErrorException(e);
        }

        // exchange synch messages with debugger

        for (;;) {
            String message = debuggerSynchronizer.receiveMessage();
            if (message != null) {
                logWriter.println("=> Message received from DEBUGGER: " + message);
                if (message.equals("FAILURE")) {
                    logWriter.println("##FAILURE: error message received from debugger");
                    fail("Some error received from debugger");
                } else if (message.equals("END")) {
                    logWriter.println("=> Debugger ends work");
                    break;
                } else if (!message.equals("OK")) {
                    logWriter.println("##FAILURE: unexpected message received from debugger");
                    fail("Unexpected message received from debugger");
                }
            } else {
                logWriter.println("##FAILURE: null message received from debugger");
                fail("Null message received from debugger");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////

    protected String getDebuggeeClassName() {
        return DEBUGGEE_CLASS;
    }

    /**
     * Creates wrapper for debuggee process.
     */
    protected JDWPUnitDebuggeeProcessWrapper createDebuggeeWrapper() {
        return new JDWPUnitDebuggeeProcessWrapper(settings, logWriter);
    }

    /**
     * Creates wrapper for synch connection with debugger.
     */
    protected JPDADebuggeeSynchronizer createDebuggerSyncronizer(){
        return new JPDADebuggeeSynchronizer(logWriter, settings);
    }

    /**
     * Creates wrapper object for accessing test options;
     */
    protected JPDATestOptions createTestOptions() {
        return new LaunchedDebugger.JPDADebuggerOnDemandOptions();
    }
    
    protected void internalSetUp() throws Exception {
        super.internalSetUp();

        // set fixed address for attaching connection

        String address = settings.getTransportAddress();
        if (address == null) {
        	settings.setTransportAddress(TestOptions.DEFAULT_ATTACHING_ADDRESS);
        }

        // set fixed port for debuggee sync connection

    	debuggeeSyncPort = settings.getSyncPortString();
    	if (debuggeeSyncPort == null) {
    		debuggeeSyncPort = TestOptions.DEFAULT_STATIC_SYNC_PORT;
    	}
    	
        // prepare for synch connection with debugger

        logWriter.println("=> Prepare synch connection with debugger");
        debuggerSynchronizer = createDebuggerSyncronizer();
        debuggerSyncPortNumber = debuggerSynchronizer.bindServer();

        // create wrapper for debuggee process

        debuggeeWrapper = createDebuggeeWrapper();
    }

    /**
     * Overrides inherited method to stop started debuggee VM and close all
     * connections.
     */
    protected void internalTearDown() {
        if (debuggerSynchronizer != null) {
            logWriter.println("Close synch connection with debugger");
            debuggerSynchronizer.stop();
        }

        if (debuggeeWrapper != null) {
            debuggeeWrapper.finishProcessAndRedirectors();
            logWriter.println("Finished debuggee VM process and closed connection");
        }
        super.internalTearDown();
    }

    /**
     * Prepares command line for launching debuggee.
     *
     * @param debuggerCmd cmd to launch debugger. Value of parameter "launch"
     * @param agentAddress address for connection with debugger
     * @param isSuspendDebuggee should debuggee be suspended on start
     * @param isOnuncaught should debuggee waits for uncaught exception (see JDWP agent launch options)
     * @return command line for launching debuggee
     */
    private String prepareDebuggerCmd(String debuggerName, String transportAddress,
            String isSuspendDebuggee, String isOnuncaught) {

        String cmdLine = settings.getDebuggeeJavaPath() 
            + " -cp " + settings.getDebuggeeClassPath()
            + " -Djpda.settings.connectorKind=attach"
	        + " -Djpda.settings.debuggeeSuspend=" + isSuspendDebuggee
	        + " -Djpda.settings.transportAddress=" + transportAddress
	        + " -Djpda.settings.syncDebuggerPort=" + debuggerSyncPortNumber
	        + " -Djpda.settings.syncPort=" + debuggeeSyncPort
	        + " " + debuggerName;
   	
        return cmdLine;
    }

    /**
     * Prepares command line for launching debuggee.
     *
     * @param debuggerCmd cmd to launch debugger. Value of parameter "launch"
     * @param agentAddress address for connection with debugger
     * @param isSuspendDebuggee should debuggee be suspended on start
     * @param isOnuncaught should debuggee waits for uncaught exception (see JDWP agent launch options)
     * @return command line for launching debuggee
     */
    private String prepareDebuggeeCmd(String debuggerCmd, String transportAddress,
            String isSuspendDebuggee, String isOnuncaught) {

    	String cmdLine = settings.getDebuggeeJavaPath() 
    	        + " -cp "
                + settings.getDebuggeeClassPath() 
    	        + " \""
                + "-agentlib:" + settings.getDebuggeeAgentName()
                + "=transport=dt_socket,address=" + transportAddress 
                + ",server=y"
                + ",suspend=" + isSuspendDebuggee 
                + ",onuncaught=" + isOnuncaught 
                + ",onthrow=" + EXCEPTION_CLASS_FOR_DEBUGGER
                + ",launch=\'" + debuggerCmd + "\'"
                + "," + settings.getDebuggeeAgentExtraOptions()
    	        + "\""
                + " " + settings.getDebuggeeVMExtraOptions()
                + " -Djpda.settings.syncPort=" + debuggeeSyncPort 
                + " " + getDebuggeeClassName();

        return cmdLine;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(OnthrowDebuggerLaunchTest.class);
    }

    protected JDWPUnitDebuggeeProcessWrapper debuggeeWrapper;

    protected JPDADebuggeeSynchronizer debuggerSynchronizer;
    private int debuggerSyncPortNumber;
    private String debuggeeSyncPort;
}
