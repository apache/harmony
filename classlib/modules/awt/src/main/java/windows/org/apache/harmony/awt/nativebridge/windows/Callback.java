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
 * @author Rustem Rafikov
 */
package org.apache.harmony.awt.nativebridge.windows;

import org.apache.harmony.awt.internal.nls.Messages;

public class Callback {

    public interface Handler {
        long windowProc(long hwnd, int msg, long wParam, long lParam);
    }

    private static long callbackWNDPROC = 0;
    private static long callbackOFNHOOKPROC = 0;
    private static long callbackDataTransferProc = 0;

    private static Handler handler;
    private static Handler handlerOFN;
    private static Handler handlerDataTransfer;

    static {
        org.apache.harmony.awt.Utils.loadLibrary("Win32Wrapper"); //$NON-NLS-1$
        callbackWNDPROC = initCallBackWNDPROC();
        callbackOFNHOOKPROC = initCallBackOFNHOOKPROC();
        callbackDataTransferProc = initCallBackDataTransferProc();
    }

    public static long registerCallback(Handler h) {
        if (handler != null && handler != h) {
            // awt.1E=Attempt to replace WindowProc handler
            throw new RuntimeException(Messages.getString("awt.1E")); //$NON-NLS-1$
        }
        handler = h;
        return callbackWNDPROC;
    }
    
    public static long registerCallbackOFN(Handler h) {        
        handlerOFN = h;
        return callbackOFNHOOKPROC;
    }

    public static long registerCallbackDataTransfer(Handler h) {        
        handlerDataTransfer = h;
        return callbackDataTransferProc;
    }

    private static native long initCallBackWNDPROC();
    private static native long initCallBackOFNHOOKPROC();
    private static native long initCallBackDataTransferProc();

    /**
     * Calls registred java method. Called from JNI code.
     */
    static long runCallbackWNDPROC(long p1, int p2, long p3, long p4) {
        return (handler != null) ? handler.windowProc(p1, p2, p3, p4) : 0;
    }
    
    static long runCallbackOFNHOOKPROC(long p1, int p2, long p3, long p4) {
        return (handlerOFN != null) ? handlerOFN.windowProc(p1, p2, p3, p4) : 0;
    }

    static long runCallbackDataTransferProc(long p1, int p2, long p3, long p4) {
        return (handlerDataTransfer != null) ? 
                handlerDataTransfer.windowProc(p1, p2, p3, p4) : 0;
    }
}
