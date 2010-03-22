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
package org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand;

import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.JPDATestOptions;

/**
 * Base class for debuggers that are used in tests for DebuggerOnDemand functionality.
 */
public abstract class LaunchedDebugger extends JDWPTestCase {
    
    // synchronization channel between debugger and debuggee
    protected JPDADebuggeeSynchronizer synchronizer;

    // synchronization channel between debugger and test
    protected JPDADebuggeeSynchronizer testSynchronizer;
    
    // name of tested debuggee class
    public static final String DEBUGGEE_CLASS_NAME = 
    	"org/apache/harmony/jpda/tests/jdwp/DebuggerOnDemand/OnthowDebuggerLaunchDebuggee";

    // signature of the tested debuggee class
    public static final String DEBUGGEE_CLASS_SIGNATURE = "L" + DEBUGGEE_CLASS_NAME + ";";
    
    /**
     * This method is invoked right before attaching to debuggee VM.
     * It forces to use attaching connector and fixed transport address.
     */
/*
    protected void beforeDebuggeeStart(JDWPOnDemandDebuggeeWrapper debugeeWrapper) {
    	settings.setAttachConnectorKind();
        if (settings.getTransportAddress() == null) {
            settings.setTransportAddress(JPDADebuggerOnDemandOptions.DEFAULT_ATTACHING_ADDRESS);
        }
        logWriter.println("DEBUGGER: Use ATTACH connector kind");
       
        super.beforeDebuggeeStart(debuggeeWrapper);
    }
 */
    	
    /**
     * Overrides inherited method to resume debuggee VM and then to establish
     * sync connection with debuggee and server.
     */
    protected void internalSetUp() throws Exception {

        // estabslish synch connection with test
        logWriter.println("Establish synch connection between debugger and test");
        JPDADebuggerOnDemandOptions debuggerSettings = new JPDADebuggerOnDemandOptions();
        testSynchronizer = new JPDADebuggerOnDemandSynchronizer(logWriter, debuggerSettings);
        testSynchronizer.startClient();
        logWriter.println("Established synch connection between debugger and test");

        // handle JDWP connection with debuggee
        super.internalSetUp();

    	// establish synch connection with debuggee
        logWriter.println("Establish synch connection between debugger and debuggee");
    	synchronizer = new JPDADebuggeeSynchronizer(logWriter, settings);;
        synchronizer.startClient();
        logWriter.println("Established synch connection between debugger and debuggee");
    }
    
    /**
     * Creates wrapper for debuggee process.
     */
    protected JDWPUnitDebuggeeWrapper createDebuggeeWrapper() {
        return new JPDADebuggerOnDemandDebuggeeWrapper(settings, logWriter);
    }
    
    /**
     * Receives initial EXCEPTION event if debuggee is suspended on event.
     */
    protected void receiveInitialEvent() {
        if (settings.isDebuggeeSuspend()) {
            initialEvent = 
                debuggeeWrapper.vmMirror.receiveCertainEvent(JDWPConstants.EventKind.EXCEPTION);
            logWriter.println("Received inital EXCEPTION event");
            debuggeeWrapper.resume();
            logWriter.println("Resumed debuggee VM");
        }
    }

    /**
     * Overrides inherited method to close sync connection upon exit.
     */
    protected void internalTearDown() {
        // close synch connection with debuggee
        if (synchronizer != null) {
            synchronizer.stop();
            logWriter.println("Closed synch connection between debugger and debuggee");
        }

        // close synch connection with test
        if (testSynchronizer != null) {
            testSynchronizer.stop();
            logWriter.println("Closed synch connection between debugger and test");
        }

        // close connections with debuggee
        super.internalTearDown();
    }

	protected String getDebuggeeClassName() {
        return null;
    }
    
	///////////////////////////////////////////////////////////////////

	/**
     * This class contains information about frame of a java thread. 
     */
    public class FrameInfo {
        long frameID;
        Location location;
        
        public FrameInfo(long frameID, Location location) {
            super();
            this.frameID = frameID;
            this.location = location;
        }
        
        /**
         * @return Returns the frameID.
         */
        public long getFrameID() {
            return frameID;
        }
        /**
         * @return Returns the location.
         */
        public Location getLocation() {
            return location;
        }
    }
    
    protected FrameInfo[] jdwpGetFrames(long threadID, int startFrame, int length) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FramesCommand);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsInt(startFrame);
        packet.setNextValueAsInt(length);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        if (!checkReplyPacketWithoutFail(reply, "ThreadReference::FramesCommand command")) {
            throw new TestErrorException("Error during performing ThreadReference::Frames command");
        }
               
        int frames = reply.getNextValueAsInt();
        FrameInfo[] frameInfos = new FrameInfo[frames];
        for (int i = 0; i < frames; i++) {
            long frameID = reply.getNextValueAsLong();
            Location location = reply.getNextValueAsLocation();
            frameInfos[i] = new FrameInfo(frameID, location);
        }
        return frameInfos;
    }
    
    protected long getClassIDBySignature(String signature) {
        logWriter.println("=> Getting reference type ID for class: " + signature);
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        packet.setNextValueAsString(signature);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        if (!checkReplyPacketWithoutFail(reply, "VirtualMachine::ClassesBySignature command")) {
            throw new TestErrorException("Error during performing VirtualMachine::ClassesBySignature command");
        }
        int classes = reply.getNextValueAsInt();
        logWriter.println("=> Returned number of classes: " + classes);
        long classID = 0;
        for (int i = 0; i < classes; i++) {
            reply.getNextValueAsByte();
            classID = reply.getNextValueAsReferenceTypeID();
            reply.getNextValueAsInt();
            // we need the only class, even if there were multiply ones
            break;
        }
        assertTrue("VirtualMachine::ClassesBySignature command returned invalid classID:<" +
                classID + "> for signature " + signature, classID > 0);
        return classID;
    }
    
    protected void printStackFrame(int NumberOfFrames, FrameInfo[] frameInfos) {
        for (int i = 0; i < NumberOfFrames; i++) {
            logWriter.println(" ");
            logWriter
                    .println("=> #" + i + " frameID=" + frameInfos[i].frameID);
            String methodName = "";
            try {
                methodName = getMethodName(frameInfos[i].location.classID,
                           frameInfos[i].location.methodID);
            } catch (TestErrorException e) {
                throw new TestErrorException(e);
            }
            logWriter.println("=> method name=" + methodName);
        }
        logWriter.println(" ");
    }
    
    protected String getMethodName(long classID, long methodID) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        packet.setNextValueAsClassID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        if (!checkReplyPacketWithoutFail(reply, "ReferenceType::Methods command")) {
            throw new TestErrorException("Error during performing ReferenceType::Method command");
        }
        int methods = reply.getNextValueAsInt();
        for (int i = 0; i < methods; i++) {
            long mid = reply.getNextValueAsMethodID();
            String name = reply.getNextValueAsString();
            reply.getNextValueAsString();
            reply.getNextValueAsInt();
            if (mid == methodID) {
                return name;
            }
        }
        return "unknown";
    }

    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * This class provides functionality to establish synch connection between debugger and test.  
     */
    protected static class JPDADebuggerOnDemandSynchronizer extends JPDADebuggeeSynchronizer {

        public JPDADebuggerOnDemandSynchronizer(LogWriter logWriter, JPDADebuggerOnDemandOptions settings) {
            super(logWriter, settings);
            this.settings = settings;
        }

        public int getSyncPortNumber() {
            return ((JPDADebuggerOnDemandOptions)settings).getSyncDebuggerPortNumber();
        }
    }

    /**
     * This class provides customization of DebuggeeWrapper that attaches to already launched debuggee.
     */
    protected static class JPDADebuggerOnDemandDebuggeeWrapper extends JDWPUnitDebuggeeWrapper {

        public JPDADebuggerOnDemandDebuggeeWrapper(JPDATestOptions settings, LogWriter logWriter) {
            super(settings, logWriter);
        }
    	
    	/**
         * Attaches to already launched debuggee process and
         * establishes JDWP connection.
         */
        public void start() {
            try {
                transport = createTransportWrapper();
                openConnection();
                logWriter.println("Established connection");
            } catch (Exception e) {
                throw new TestErrorException(e);
            }
        }

        /**
         * Closes all connections but does not wait for debuggee process to exit.
         */
        public void stop() {
            disposeConnection();
            closeConnection();
        }
    }

    /**
     * This class provides additional options to debuggers, that are used in DebuggerOnDemand tests.
     * Currently the following additional options are supported:
     * <ul>
     * <li><i>jpda.settings.syncDebuggerPort</i> 
     *  - port number for sync connection between debugger and test
     * </ul>
     *
     */
    protected static class JPDADebuggerOnDemandOptions extends JPDATestOptions {

        public static final String DEFAULT_SYNC_DEBUGGER_PORT = "9899";
    	
        /**
         * Returns port number string for sync connection between debugger and test.
         */
        public String getSyncDebuggerPortString() {
            return getProperty("jpda.settings.syncDebuggerPort", null);
        }
        
        /**
         * Returns port number for sync connection between debugger and test.
         */
        public int getSyncDebuggerPortNumber() {
            String buf = getSyncDebuggerPortString();
            if (buf == null) {
                buf = DEFAULT_SYNC_DEBUGGER_PORT;
            }

            try {
                return Integer.parseInt(buf);
            } catch (NumberFormatException e) {
                throw new TestErrorException(e);
            }
        }
    }
}
