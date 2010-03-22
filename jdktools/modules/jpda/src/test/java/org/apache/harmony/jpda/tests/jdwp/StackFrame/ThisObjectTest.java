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
 * @author Anton V. Karnachuk
 */

/**
 * Created on 24.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.StackFrame;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for StackFrame.ThisObject command.
 */
public class ThisObjectTest extends JDWPStackFrameTestCase {

    public static String[] KNOWN_METHOD_NAMES = {
        "nestledMethod1",
        "nestledMethod2",
        "nestledMethod3",
    };
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ThisObjectTest.class);
    }

    /**
     * This testcase exercises StackFrame.ThisObject command.
     * <BR>The test starts StackTraceDebuggee and
     * checks if StackFrame.ThisObject command returns correct data for each stack frame 
     * of main thread in debuggee, taking into account calls to known methods.
     */
    public void testThisObjectTest001() {
        logWriter.println("==> ThisObjectTestTest001 started");
        //boolean success = true;
        
        // select main thread
        String mainThreadName = synchronizer.receiveMessage();
        logWriter.println("==> Searching for main thread by name: " + mainThreadName);
        long mainThread = debuggeeWrapper.vmMirror.getThreadID(mainThreadName);
        logWriter.println("==> Found main thread: " + mainThread);
        
        // release on run()
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // pass nestledMethod1()
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // pass nestledMethod2()
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // enter nestledMethod3()
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        if ( mainThread == -1 ) {
            logWriter.println("## FAILURE: main thread is not found!");
            //assert True(false);
            fail("main thread is not found!");
        }

        // suspend thread
        logWriter.println("==> Suspending main thread");
        jdwpSuspendThread(mainThread);
        
        logWriter.println("==> Getting frames count");
        int frameCount = jdwpGetFrameCount(mainThread);
        logWriter.println("==> frames count = " + frameCount);
        
        logWriter.println("==> Getting frames");
        FrameInfo[] frameIDs = jdwpGetFrames(mainThread, 0, frameCount);
        logWriter.println("==> frames count = " + frameIDs.length);
        assertEquals("Invlid number of frames,", frameCount, frameIDs.length);
        //assertTrue(frameIDs.length == frameCount);
        
        for (int i = 0; i < frameCount; i++) {
            logWriter.println("\n==> frame #" + i);

            long frameID = frameIDs[i].frameID;
            logWriter.println("==> frameID=" + frameID);
            if (frameID == 0) {
                logWriter.println("## FAILURE: ThreadReference.Frames returned NULL FrameID for frame #" + i);
                //success = false;
                fail("ThreadReference.Frames returned NULL FrameID for frame #" + i);
                continue;
            }
            
            // logWriter.println("  location=" + frameIDs[i].location);

            String methodName = debuggeeWrapper.vmMirror.getMethodName(frameIDs[i].location.classID, frameIDs[i].location.methodID);
            logWriter.println("==> method name=" + methodName);

            String methodSig = debuggeeWrapper.vmMirror.getMethodSignature(frameIDs[i].location.classID, frameIDs[i].location.methodID);
            logWriter.println("==> method signature=" + methodSig);

            String classSig = debuggeeWrapper.vmMirror.getClassSignature(frameIDs[i].location.classID);
            logWriter.println("==> class signature=" + classSig);

            // get ThisObject
            logWriter.println("==> Send StackFrame::ThisObject command...");
            CommandPacket packet = new CommandPacket(
                    JDWPCommands.StackFrameCommandSet.CommandSetID,
                    JDWPCommands.StackFrameCommandSet.ThisObjectCommand);
            packet.setNextValueAsThreadID(mainThread);
            packet.setNextValueAsLong(frameIDs[i].getFrameID());
            
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            long knownMethodsThisObject = 0;
            if (reply.getErrorCode() == JDWPConstants.Error.NONE) {
                TaggedObject thisObject = reply.getNextValueAsTaggedObject();
                logWriter.println("==> thisObject:");
                logWriter.println("==> tag=" + thisObject.tag + "(" 
                        + JDWPConstants.Tag.getName(thisObject.tag) + ")");
                logWriter.println("==> id=" + thisObject.objectID);
                if (thisObject.objectID != 0) {
                    long classID = getObjectReferenceType(thisObject.objectID);
                    logWriter.println("==> class=" + debuggeeWrapper.vmMirror.getClassSignature(classID));
                }

                for (int j = 0; j < KNOWN_METHOD_NAMES.length; j++) {
                    if (KNOWN_METHOD_NAMES[j].equals(methodName)) {
                        logWriter.println("==> frame for known method: " + KNOWN_METHOD_NAMES[j]);
                        if (thisObject.objectID == 0) {
                            logWriter.println
                            ("## FAILURE: StackFrame.ThisObject returned NULL ObjectID for known method: " + methodName);
                            //success = false;
                            fail("StackFrame.ThisObject returned NULL ObjectID for known method: " + methodName);
                        } else {
                            if ( knownMethodsThisObject != 0 ) {
                                if ( knownMethodsThisObject != thisObject.objectID ) {
                                    logWriter.println
                                    ("## FAILURE: Returned unexpected ObjectID for known method: " + methodName);
                                    logWriter.println
                                    ("## Expected ObjectID: " + knownMethodsThisObject);
                                    //success = false;
                                    fail("Returned unexpected ObjectID for known method: " + methodName);
                                }
                            } else {
                                knownMethodsThisObject = thisObject.objectID;
                            }
                        }
                        if (thisObject.tag != JDWPConstants.Tag.OBJECT_TAG) {
                            logWriter.println
                            ("## FAILURE: StackFrame.ThisObject returned not OBJECT_TAG for known method: " + methodName);
                            //success = false;
                            fail("StackFrame.ThisObject returned not OBJECT_TAG for known method: " + methodName);
                        }
                    }
                }
                
                String mainMethod = "main";
                if (mainMethod.equals(methodName)) {
                    logWriter.println("==> frame for method: " + mainMethod);
                    if (thisObject.objectID != 0) {
                        logWriter.println
                        ("## FAILURE: Returned unexpected ObjectID for method: " + mainMethod);
                        logWriter.println
                        ("## Expected ObjectID: " + 0);
                        //success = false;
                        fail("Returned unexpected ObjectID for method: " + mainMethod);
                    }
                    if (thisObject.tag != JDWPConstants.Tag.OBJECT_TAG) {
                        logWriter.println
                        ("## FAILURE: StackFrame.ThisObject returned not OBJECT_TAG for method: " + mainMethod);
                        //success = false;
                        fail("StackFrame.ThisObject returned not OBJECT_TAG for method: " + mainMethod);
                    }
                }

                assertAllDataRead(reply);
                /*if (!reply.isAllDataRead()) {
                    logWriter.println("## FAILURE: Extra bytes in reply for StackFrame.ThisObject");
                    success = false;
                }*/
                
            } else {
                logWriter.println
                ("## FAILURE: StackFrame::ThisObject command returns unexpected ERROR = "
                        + reply.getErrorCode() 
                        + "(" + JDWPConstants.Error.getName(reply.getErrorCode()) + ")");
                logWriter.println("## Expected ERROR = " + JDWPConstants.Error.NONE 
                        + "(" + JDWPConstants.Error.getName(JDWPConstants.Error.NONE) + ")");
                //success = false;
                fail("StackFrame::ThisObject command returns unexpected ERROR = " + reply.getErrorCode() 
                        + "(" + JDWPConstants.Error.getName(reply.getErrorCode()) + ")"
                        + ", Expected ERROR = " + JDWPConstants.Error.NONE 
                        + "(" + JDWPConstants.Error.getName(JDWPConstants.Error.NONE) + ")");
            }
        }

        // resume thread
        logWriter.println("==> Resuming main thread");
        jdwpResumeThread(mainThread);    

        // release nestledMethod3()
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        //assertTrue(success);
        logWriter.println("==> ThisObjectTestTest001 finished");
    }
}
