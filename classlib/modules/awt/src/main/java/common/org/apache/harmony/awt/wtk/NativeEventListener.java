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
 * @author Mikhail Danilov
 */
package org.apache.harmony.awt.wtk;

/**
 * Describes the installable cross-platform event handler interface
 */
public interface NativeEventListener {
    /**
     * The callback called when system event is processed by event loop.
     * NativeEvent is not the event but way to decode the event and
     * query system properties absent in the event.
     *
     * <p/>NativeEvent is valid only inside onEvent call and
     * shouldn't be stored for future use. This will cause
     * RuntimeException to be thrown.
     *
     * <p/> It's guarantied this method will be called on the
     * same thread event loop for the recipient window is run.
     *
     * <p/>This method is not always called from
     * NativeEventQueue.dispatchEventToListener depending on the
     * platform so the application shouldn't assume it.
     *
     * <p/>Returns wether the message is fully processed and
     * shouldn't be passed to system for additional processing.
     * Extreme care is required as most of the messages if cosumed
     * can cause undesired system behavior.
     *
     * @param event - valid only inside the call
     * @return if the message is consumed
     */
    boolean onEvent(NativeEvent event);

    /**
     * We do not want user event listeners calls
     * to be nested (read "unpredictable"). So we need the point
     * when we can guaranty that all user event listeners can be executed
     * strictly one by one. There is such a point - the end of null-nested
     * event handler. At that point onEventNestingEnd() method is called.
     */
    void onEventNestingEnd();

    /**
     * This method is called to prepare for event handling.
     */
    void onEventBegin();

    /**
     * This method is called to perform cleanup after event handling.
     */
    void onEventEnd();

    /**
     * This method is called when native event queue is awaked
     * from waiting of native event
     *
     */
    void onAwake();

    /**
     *
     * @return the synchronizer object
     */
    Synchronizer getSynchronizer();

    /**
     *
     * @return the WTK instance
     */
    WTK getWTK();

}