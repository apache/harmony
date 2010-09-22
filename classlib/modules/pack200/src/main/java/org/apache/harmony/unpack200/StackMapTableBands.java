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
package org.apache.harmony.unpack200;

import java.util.ArrayList;
import java.util.List;

import org.apache.harmony.unpack200.bytecode.CPClass;
import org.apache.harmony.unpack200.bytecode.StackMapTableAttribute;

/**
 * Set of bands that hold data about StackMapTable attributes
 */
public class StackMapTableBands {

    private final int[][] frameT;
    private int frameTIndex;

    private final int[] localN;
    private int localNIndex;

    private final int[] stackN;
    private int stackNIndex;

    private final int[] offset;
    private int offsetIndex;

    private final int[] t;
    int tIndex = 0;

    private final CPClass[] rc;
    private int rcIndex;

    private final int[] p;
    private int pIndex;

    public StackMapTableBands(
            int[][] frameT, int[] localN,
            int[] stackN, int[] offset,
            int[] t, CPClass[] rc,
            int[] p) {
        this.frameT = frameT;
        this.localN = localN;
        this.stackN = stackN;
        this.offset = offset;
        this.t = t;
        this.rc = rc;
        this.p = p;
    }

    public StackMapTableAttribute nextStackMapTableAttribute() {
        List frames = new ArrayList();

        for (int i = 0; i < frameT[frameTIndex].length; i++) {
            int frameType = frameT[frameTIndex][i];
            StackMapTableAttribute.Frame frame = new StackMapTableAttribute.Frame(frameType);
            if(frameType >= 64 && frameType <= 127) {
                List stack = new ArrayList();
                stack.add(getNextVerificationTypeInfo());
                frame.setStack(stack);
            } else if (frameType == 247) {
                frame.setOffsetDelta(offset[offsetIndex++]);
                List stack = new ArrayList();
                stack.add(getNextVerificationTypeInfo());
                frame.setStack(stack);
            } else if (frameType >= 248 && frameType <= 251) {
                frame.setOffsetDelta(offset[offsetIndex++]);
            } else if (frameType >= 252 && frameType <= 254) {
                frame.setOffsetDelta(offset[offsetIndex++]);
                List locals = new ArrayList();
                int numLocals = frameType -251;
                for (int j = 0; j < numLocals; j++) {
                    locals.add(getNextVerificationTypeInfo());
                }
                frame.setLocals(locals);
            } else if (frameType == 255) {
                frame.setOffsetDelta(offset[offsetIndex++]);
                List locals = new ArrayList();
                int numLocals = localN[localNIndex++];
                for (int j = 0; j < numLocals; j++) {
                    locals.add(getNextVerificationTypeInfo());
                }
                frame.setLocals(locals);
                List stack = new ArrayList();
                int numberOnStack = stackN[stackNIndex++];
                for (int j = 0; j < numberOnStack; j++) {
                    stack.add(getNextVerificationTypeInfo());
                }
                frame.setStack(stack);
            }
            frames.add(frame);
        }

        StackMapTableAttribute stackMapTableAttribute = new StackMapTableAttribute(frames);
        frameTIndex++;
        return stackMapTableAttribute;
    }

    private StackMapTableAttribute.VerificationTypeInfo getNextVerificationTypeInfo() {
        int tag = t[tIndex++];
        if(tag == 7) {
            CPClass classRef = rc[rcIndex++];
            return new StackMapTableAttribute.VerificationTypeInfo(tag, classRef);
        } else if (tag == 8) {
            int offset = p[pIndex++];
            return new StackMapTableAttribute.VerificationTypeInfo(tag, offset);
        } else {
            return new StackMapTableAttribute.VerificationTypeInfo(tag);
        }
    }

}
