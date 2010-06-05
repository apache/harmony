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
 * Created on 06.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.EventPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.exceptions.TimeoutException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for CLASS_UNLOAD event.
 */
public class ClassUnloadTest extends JDWPEventTestCase {

	public static final String TESTED_CLASS_NAME = 
		"org.apache.harmony.jpda.tests.jdwp.Events.ClassUnloadTestedClass";
	
	public static final String TESTED_CLASS_SIGNATURE = 
		"L" + TESTED_CLASS_NAME.replace('.', '/') + ";";
	
    protected String getDebuggeeClassName() {
        return ClassUnloadDebuggee.class.getName();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClassUnloadTest.class);
    }

    /**
     * This testcase is for CLASS_UNLOAD event.
     */
    public void testClassUnloadEvent() {
        logWriter.println("==> testClassUnloadEvent started");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        ReplyPacket reply = null;
        int foundClasses = 0;
       
        // commented out because it may leave JNI references to the tested class,
        //   which will prevent it from garbage collecting and unloading
/*        
        // check that tested class is loaded before unloading it
        logWriter.println("=> Find tested class by signature: " + TESTED_CLASS_SIGNATURE);
        reply = debuggeeWrapper.vmMirror.getClassBySignature(TESTED_CLASS_SIGNATURE);
        foundClasses = reply.getNextValueAsInt();
        logWriter.println("=> Found clases: " + foundClasses);

        if (foundClasses <= 0) {
        	fail("Tested class was not found: count=" + foundClasses);
        }
*/
        
        logWriter.println("=> Set request for ClasUnload event: " + TESTED_CLASS_NAME);
        reply = debuggeeWrapper.vmMirror.setClassUnload(TESTED_CLASS_NAME);
        checkReplyPacket(reply, "Set CLASS_UNLOAD event");
        int requestID = reply.getNextValueAsInt();
        logWriter.println("=> Created requestID for ClassUnload event: " + requestID);
        
        logWriter.println("=> Release debuggee");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        logWriter.println("=> Wait for class unload event");
		EventPacket event = null;
        try {
			event = debuggeeWrapper.vmMirror.receiveEvent(settings.getTimeout());
	        logWriter.println("=> Event received");
		} catch (TimeoutException e) {
	        logWriter.println("=> ClassUnload event was not received (class might be not really unloaded)");
		} catch (Exception e) {
	        logWriter.println("=> Exception during receiving ClassUnload event: " + e);
	        throw new TestErrorException(e);
		}

        logWriter.println("=> Clear request for ClassUnload event");
        reply = debuggeeWrapper.vmMirror.clearEvent(JDWPConstants.EventKind.CLASS_UNLOAD, requestID);
        
        logWriter.println("=> Try to find tested class by signature: " + TESTED_CLASS_SIGNATURE);
        reply = debuggeeWrapper.vmMirror.getClassBySignature(TESTED_CLASS_SIGNATURE);
        foundClasses = reply.getNextValueAsInt();
        logWriter.println("=> Found clases: " + foundClasses);
		
		logWriter.println("=> Wait for class status message from debuggee");
        String status = synchronizer.receiveMessage();
        logWriter.println("=> Debuggee reported class status: " + status);

        if (event != null) {
			// check event data
			ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
	        
	        assertEquals("Invalid number of events,", 1, parsedEvents.length);
	        assertEquals("Invalid event kind,", JDWPConstants.EventKind.CLASS_UNLOAD 
	        		, parsedEvents[0].getEventKind()
	                , JDWPConstants.EventKind.getName(JDWPConstants.EventKind.CLASS_UNLOAD)
	                , JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
	        assertEquals("Invalid event request,", requestID 
	        		, parsedEvents[0].getRequestID());
	
	        // check that unloaded class was not found after event
	        if (foundClasses > 0) {
	        	fail("Tested class was found after ClasUnload event: count=" + foundClasses);
	        }

	        logWriter.println("=> Resume debuggee on event");
	        debuggeeWrapper.resume();
		} else { 
	        // check if tested class not found without event
	        if (foundClasses <= 0) {
	        	fail("No ClassUnload event, but tested class not found: count=" + foundClasses);
	        }

	        // check if debuggee reported tested class unloaded without event
	        if ("UNLOADED".equals(status)) {
	        	fail("No ClassUnload event, but tested class was unloaded");
	        }
		}
    
        logWriter.println("=> Release debuggee");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> testClassUnloadEvent ended");
    }
}
