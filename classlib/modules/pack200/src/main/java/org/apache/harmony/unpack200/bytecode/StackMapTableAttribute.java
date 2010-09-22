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
package org.apache.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.pack200.Pack200Exception;

/**
 * StackMapTable attribute
 */
public class StackMapTableAttribute extends BCIRenumberedAttribute {

    private static CPUTF8 attributeName;

    private final List frames;

    public static void setAttributeName(CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    public StackMapTableAttribute(List frames) {
        super(attributeName);
        this.frames = frames;
    }

    public String toString() {
        return "StackMapTableAttribute";
    }

    protected int getLength() {
        int length = 2;
        for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
            Frame frame = (Frame) iterator.next();
            length += frame.getLength();
        }
        return length;
    }

    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(frames.size());
        for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
            Frame frame = (Frame) iterator.next();
            frame.writeBody(dos);
        }
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
            Frame frame = (Frame) iterator.next();
            frame.resolve(pool);
        }
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
        List classFileEntries = new ArrayList();
        classFileEntries.add(attributeName);
        for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
            Frame frame = (Frame) iterator.next();
            classFileEntries.addAll(frame.getNestedClassFileEntries());
        }
        ClassFileEntry[] entries = new ClassFileEntry[classFileEntries.size()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = (ClassFileEntry) classFileEntries.get(i);
        }
        return entries;
    }

    protected int[] getStartPCs() {
        // Don't do anything here because we've overridden renumber.
        return null;
    }

    public void renumber(List byteCodeOffsets) throws Pack200Exception {
        for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
            Frame frame = (Frame) iterator.next();
            frame.renumber(byteCodeOffsets);
        }
    }

    public static class Frame {

        private final int frameType;
        private int offsetDelta;
        private List locals;
        private List stack;

        public Frame(int frameType) {
            this.frameType = frameType;
        }

        public void setOffsetDelta(int offsetDelta) {
            this.offsetDelta = offsetDelta;
        }

        public void setLocals(List locals) {
            this.locals = locals;
        }

        public void setStack(List stack) {
            this.stack = stack;
        }

        protected void resolve(ClassConstantPool pool) {
            if (locals != null) {
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    info.resolve(pool);
                }
            }
            if (stack != null) {
                for (Iterator iterator = stack.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    info.resolve(pool);
                }
            }
        }

        public int getLength() {
            int length = 0;
            if (frameType <= 63) {
                length = 1;
            } else if (frameType <= 127) {
                VerificationTypeInfo info = (VerificationTypeInfo) stack.get(0);
                length = 1 + info.getLength();
            } else if (frameType == 247) {
                VerificationTypeInfo info = (VerificationTypeInfo) stack.get(0);
                length = 3 + info.getLength();
            } else if (frameType >= 248 && frameType <= 251) {
                length = 3;
            } else if (frameType >= 252 && frameType <= 254) {
                length = 3;
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    length += info.getLength();
                }
            } else if (frameType == 255) {
                length = 7;
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    length += info.getLength();
                }
                for (Iterator iterator = stack.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    length += info.getLength();
                }
            }
            return length;
        }

        public void renumber(List byteCodeOffsets) {
            if(locals != null) {
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    if(info.tag == 8) {
                        info.offset = ((Integer)byteCodeOffsets.get(info.offset)).intValue();
                    }
                }
            }
            if(stack != null) {
                for (Iterator iterator = stack.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    if(info.tag == 8) {
                        info.offset = ((Integer)byteCodeOffsets.get(info.offset)).intValue();
                    }
                }
            }
        }

        public List getNestedClassFileEntries() {
            List nested = new ArrayList();
            if(locals != null) {
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator.next();
                    if(info.cpClass != null) {
                        nested.add(info.cpClass);
                    }
                }
            }
            if(stack != null) {
                for (Iterator iterator = stack.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator.next();
                    if(info.cpClass != null) {
                        nested.add(info.cpClass);
                    }
                }
            }
            return nested;
        }

        public void writeBody(DataOutputStream dos) throws IOException {
            dos.writeByte(frameType);
            if (frameType >= 64 && frameType <= 127) {
                VerificationTypeInfo info = (VerificationTypeInfo) stack.get(0);
                info.writeBody(dos);
            } else if (frameType == 247) {
                dos.writeShort(offsetDelta);
                VerificationTypeInfo info = (VerificationTypeInfo) stack.get(0);
                info.writeBody(dos);
            } else if (frameType >= 248 && frameType <= 251) {
                dos.writeShort(offsetDelta);
            } else if (frameType >= 252 && frameType <= 254) {
                dos.writeShort(offsetDelta);
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    info.writeBody(dos);
                }
            } else if (frameType == 255) {
                dos.writeShort(offsetDelta);
                dos.writeShort(locals.size());
                for (Iterator iterator = locals.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    info.writeBody(dos);
                }
                dos.writeShort(stack.size());
                for (Iterator iterator = stack.iterator(); iterator.hasNext();) {
                    VerificationTypeInfo info = (VerificationTypeInfo) iterator
                            .next();
                    info.writeBody(dos);
                }
            }
        }
    }

    public static class VerificationTypeInfo {

        private final int tag;
        private final int length;
        private int offset;
        private CPClass cpClass;
        private int cpClassIndex;

        public VerificationTypeInfo(int tag) {
            this.tag = tag;
            length = 1;
        }

        public VerificationTypeInfo(int tag, CPClass cpClass) {
            this.tag = tag;
            this.cpClass = cpClass;
            length = 3;
        }

        public VerificationTypeInfo(int tag, int offset) {
            this.tag = tag;
            this.offset = offset;
            length = 3;
        }

        public int getLength() {
            return length;
        }

        public void writeBody(DataOutputStream dos) throws IOException {
            dos.writeByte(tag);
            if (tag == 7) {
                dos.writeShort(cpClassIndex);
            } else if (tag == 8) {
                dos.writeShort(offset);
            }
        }

        protected void resolve(ClassConstantPool pool) {
            if(cpClass != null) {
                cpClass.resolve(pool);
                cpClassIndex = pool.indexOf(cpClass);
            }
        }
    }

}