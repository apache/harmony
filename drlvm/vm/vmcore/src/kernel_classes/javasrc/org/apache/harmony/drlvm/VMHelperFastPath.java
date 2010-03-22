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
 * @author Mikhail Y. Fursov
 */ 

package org.apache.harmony.drlvm;

import org.apache.harmony.drlvm.VMHelper;
import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

class VMHelperFastPath {

    private static final int VTABLE_SUPERCLASSES_OFFSET = getVtableSuperclassesOffset();
    private static final int CLASS_INF_TYPE_0_OFFSET  = getVtableIntfTypeOffset(0);
    private static final int CLASS_INF_TABLE_0_OFFSET = getVtableIntfTableOffset(0);
    private static final int CLASS_INF_TYPE_1_OFFSET  = getVtableIntfTypeOffset(1);
    private static final int CLASS_INF_TABLE_1_OFFSET = getVtableIntfTableOffset(1);
    private static final int CLASS_INF_TYPE_2_OFFSET  = getVtableIntfTypeOffset(2);
    private static final int CLASS_INF_TABLE_2_OFFSET = getVtableIntfTableOffset(2);

    private VMHelperFastPath() {}


//TODO: leave only one version 
//TODO: refactor code to use getVtableIntfTableOffset method (+ avoid extra comparisons while refactoring)

    @Inline
    public static Address getInterfaceVTable3(Address intfType, Object obj)  {
        
        //Returning zero address if the object is null 
        //is safe in terms of preserving program semantics(the exception will be generated on the first method invocation) and
        //allows profitable code transformations such as hoisting vm helper outside the loop body.
        //This is our current convention which touches all variants of this helper, 
        //If it changes, all variants must be fixed. 
            
        if(obj==null){
                return Address.zero();
        }
            
        Address vtableAddr = VMHelper.getVTable(obj);

        Address inf0Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_0_OFFSET));
        if (inf0Type.EQ(intfType)) {
            return vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TABLE_0_OFFSET));               
        }
    
        Address inf1Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_1_OFFSET));
        if (inf1Type.EQ(intfType)) {
            return vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TABLE_1_OFFSET));               
        }

        Address inf2Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_2_OFFSET));
        if (inf2Type.EQ(intfType)) {
            return vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TABLE_2_OFFSET));               
        }
        //slow path version
        return VMHelper.getInterfaceVTable(obj, intfType);
    }

    @Inline
    public static Address getInterfaceVTable2(Address intfType, Object obj)  {
        Address vtableAddr = VMHelper.getVTable(obj);

        Address inf0Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_0_OFFSET));
        if (inf0Type.EQ(intfType)) {
            return vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TABLE_0_OFFSET));               
        }
    
        Address inf1Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_1_OFFSET));
        if (inf1Type.EQ(intfType)) {
            return vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TABLE_1_OFFSET));               
        }

        //slow path version
        return VMHelper.getInterfaceVTable(obj, intfType);
    }

    @Inline
    public static Address getInterfaceVTable1(Address intfType, Object obj)  {
        Address vtableAddr = VMHelper.getVTable(obj);

        Address inf0Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_0_OFFSET));
        if (inf0Type.EQ(intfType)) {
            return vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TABLE_0_OFFSET));               
        }
    
        //slow path version
        return VMHelper.getInterfaceVTable(obj, intfType);
    }

    @Inline
    public static boolean instanceOf(Address castType, Object obj) {
        if (obj == null) {
            return false;
        }
        if (VMHelper.isInterface(castType)) {
            Address vtableAddr = VMHelper.getVTable(obj);

            Address inf0Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_0_OFFSET));
            if (inf0Type.EQ(castType)) {
                return true;
            }
    
            Address inf1Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_1_OFFSET));
            if (inf1Type.EQ(castType)) {
                return true;
            }
        } else if (!VMHelper.isArray(castType)) {
            int fastCheckDepth=VMHelper.getFastTypeCheckDepth(castType);
            if (fastCheckDepth!=0)  {
                return fastClassInstanceOf(obj, castType, fastCheckDepth);
            }
        } 
        return VMHelper.instanceOf(obj, castType);
        
    }

    @Inline
    public static void checkCast(Address castType, Object obj) {
        if (obj == null) {
            return;
        }
        if (VMHelper.isInterface(castType)) {
            Address vtableAddr = VMHelper.getVTable(obj);

            Address inf0Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_0_OFFSET));
            if (inf0Type.EQ(castType)) {
                return;
            }
    
            Address inf1Type = vtableAddr.loadAddress(Offset.fromIntZeroExtend(CLASS_INF_TYPE_1_OFFSET));
            if (inf1Type.EQ(castType)) {
                return;
            }
        } else if (!VMHelper.isArray(castType)) {
            int fastCheckDepth=VMHelper.getFastTypeCheckDepth(castType);
            if (fastCheckDepth!=0 && fastClassInstanceOf(obj, castType, fastCheckDepth)) {
                return;
            }
        } 
        VMHelper.checkCast(obj, castType);
    }

    @Inline
    public static boolean fastClassInstanceOf(Object obj, Address castType, int fastCheckDepth) {
        Address objVtableAddr = VMHelper.getVTable(obj);
        Address objClassType  = objVtableAddr.loadAddress(Offset.fromIntZeroExtend(VMHelper.VTABLE_CLASS_OFFSET));

        if (objClassType.EQ(castType)) {
            return true;
        }
        if (VMHelper.isFinal(castType)) {
            return false;
        }
       
        int subTypeOffset = VTABLE_SUPERCLASSES_OFFSET + VMHelper.POINTER_TYPE_SIZE*(fastCheckDepth-1);
        Address depthSubType = objVtableAddr.loadAddress(Offset.fromIntZeroExtend(subTypeOffset));
        return depthSubType.EQ(castType);
    }


    private static native int getVtableIntfTypeOffset(int n);
    private static native int getVtableIntfTableOffset(int n);
    private static native int getVtableSuperclassesOffset();
}
