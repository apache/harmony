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
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.ipp;

public class IppOperation {
    public static final short PRINT_JOB = 0x0002;
    public static final short PRINT_URI = 0x0003;
    public static final short VALIDATE_JOB = 0x0004;
    public static final short CREATE_JOB = 0x0005;
    public static final short SEND_DOCUMENT = 0x0006;
    public static final short SEND_URI = 0x0007;
    public static final short CANCEL_JOB = 0x0008;
    public static final short GET_JOB_ATTRIBUTES = 0x0009;
    public static final short GET_JOBS = 0x000A;
    public static final short GET_PRINTER_ATTRIBUTES = 0x000B;
    public static final short HOLD_JOB = 0x000C;
    public static final short RELEASE_JOB = 0x000D;
    public static final short RESTART_JOB = 0x000E;
    public static final short RESERVED_FOR_A_FUTURE_OPERATION = 0x000F;
    public static final short PAUSE_PRINTER = 0x0010;
    public static final short RESUME_PRINTER = 0x0011;
    public static final short PURGE_JOBS = 0x0012;
    public static final short TAG_CUPS_GET_DEFAULT = 0x4001;
    public static final short TAG_CUPS_GET_PRINTERS = 0x4002;

    public static String getString(String op) {
        return IppResources.getString("IppOperation." + op);
    }

    public static String getString(int op) {
        switch (op) {
        case PRINT_JOB:
            return IppResources.getString("IppOperation.PRINT_JOB");
        case PRINT_URI:
            return IppResources.getString("IppOperation.PRINT_URI");
        case VALIDATE_JOB:
            return IppResources.getString("IppOperation.VALIDATE_JOB");
        case CREATE_JOB:
            return IppResources.getString("IppOperation.CREATE_JOB");
        case SEND_DOCUMENT:
            return IppResources.getString("IppOperation.SEND_DOCUMENT");
        case SEND_URI:
            return IppResources.getString("IppOperation.SEND_URI");
        case CANCEL_JOB:
            return IppResources.getString("IppOperation.CANCEL_JOB");
        case GET_JOB_ATTRIBUTES:
            return IppResources.getString("IppOperation.GET_JOB_ATTRIBUTES");
        case GET_JOBS:
            return IppResources.getString("IppOperation.GET_JOBS");
        case GET_PRINTER_ATTRIBUTES:
            return IppResources.getString("IppOperation.GET_PRINTER_ATTRIBUTES");
        case HOLD_JOB:
            return IppResources.getString("IppOperation.HOLD_JOB");
        case RELEASE_JOB:
            return IppResources.getString("IppOperation.RELEASE_JOB");
        case RESTART_JOB:
            return IppResources.getString("IppOperation.RESTART_JOB");
        case RESERVED_FOR_A_FUTURE_OPERATION:
            return IppResources.getString("IppOperation.RESERVED_FOR_A_FUTURE_OPERATION");
        case PAUSE_PRINTER:
            return IppResources.getString("IppOperation.PAUSE_PRINTER");
        case RESUME_PRINTER:
            return IppResources.getString("IppOperation.RESUME_PRINTER");
        case PURGE_JOBS:
            return IppResources.getString("IppOperation.PURGE_JOBS");
        }
        return null;
    }

}