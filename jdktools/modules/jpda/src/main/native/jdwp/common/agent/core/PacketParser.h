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
 * @file
 * PacketParser.h
 *
 */

#ifndef _PACKET_PARSER_H_
#define _PACKET_PARSER_H_
    
#include "jdwp.h"
#include "jdwpTransport.h"
#include "AgentBase.h"
#include "jdwpTypes.h"

#include "jni.h"

namespace jdwp {

    /** 
     * PacketWrapper is a wrapper for <code>jdwpPacket</code> providing methods to
     * read the <code>jdwpPacket</code> header values.
     */
    class PacketWrapper : public AgentBase {

    public:

        /**
         * A default constructor.
         */
        PacketWrapper();

        /**
         * Returns <code>TRUE</code> if the packet was read from the transport.
         */
        bool IsPacketInitialized();

        /**
         * Returns the value of the length field of the JDWP packet.
         */
        jint GetLength() const {
            return m_packet.type.cmd.len;
        }

        /**
         * Returns the value of the ID field of the JDWP packet.
         */
        jint GetId() const {
            return m_packet.type.cmd.id;
        }

        /**
         * Returns the value of the flags field of the JDWP packet. 
         */
        jbyte GetFlags() const {
            return m_packet.type.cmd.flags;
        }

        /**
         * Returns the value of the command-set field of the JDWP packet. 
         */
        jdwpCommandSet GetCommandSet() const {
            return static_cast<jdwpCommandSet>(m_packet.type.cmd.cmdSet);
        }

        /**
         * Returns the value of the command field of the JDWP packet.
         */
        jdwpCommand GetCommand() const {
            return static_cast<jdwpCommand>(m_packet.type.cmd.cmd);
        }

        /**
         * Returns the value of the error field of the JDWP packet.
         */
        jint GetError() const {
            return m_packet.type.reply.errorCode;
        }

    protected:

        /**
         * The internal class GCList stores a global references and
         * allocated memory for an instance of the PacketWrapper 
         * class. All allocated links are freeing on Reset().
         */
        class GCList : public AgentBase {

        public:

            /**
             * A constructor.
             */
            GCList();

            /**
             * Stores the reference to allocated memory.
             *
             * @param ref - string
             */
            void StoreStringRef(char* ref);

            /**
             * Stores the reference to the JNI global reference.
             *
             * @param globalRef - global reference
             */
            void StoreGlobalRef(jobject globalRef);

            /**
             * Deletes all stored references.
             *
             * @param jni - the JNI interface pointer
             */
            void Reset(JNIEnv *jni);

            void ReleaseData();

            /**
             * Moves all data to other GCList instance.
             *
             * @param to - another GC list
             */
            void MoveData(GCList* to);

        private:
            // memory references
            unsigned int m_memoryRefAllocatedSize;
            char** m_memoryRef;
            unsigned int m_memoryRefPosition;

            // global references
            unsigned int m_globalRefAllocatedSize;
            jobject* m_globalRef;
            unsigned int m_globalRefPosition;
        };

        jdwpPacket m_packet;
        GCList m_garbageList;

        /**
         * Resets all data.
         *
         * @param jni - the JNI interface pointer
         */
        void Reset(JNIEnv *jni);

        void ReleaseData();

        /**
         * Moves all data to another PacketWrapper instance.
         *
         * @param jni - the JNI interface pointer
         * @param to  - another packet wrapper
         */
        void MoveData(JNIEnv *jni, PacketWrapper* to);
    };

    /**
     * The InputPacketParser  class supports reading data from 
     * <code>jdwpPacket</code>. Inherited from PacketWrapper it 
     * contains methods to read <code>jdwpPacket</code> header values and 
     * extend methods for incremental reading from the packet's data. For the 
     * JDWP types, such as <code>ObjectID</code>, <code>ReferenceTypeID</code>,
	 * it returns a global reference to the object, and puts the created reference 
     * into special collections. The Reset() method deletes all 
     * the created a global references.
     */
    class InputPacketParser : public PacketWrapper {

    public:

        /**
         * Creates an empty instance of <code>InputPacketComposer</code>.
         */
        InputPacketParser(): PacketWrapper(), m_position(0) {}

        /** 
         * Reads the packet from transport into the internal packet structure.
         *
         * @throws InternalErrorException if not enough bytes in 
         *         the packet data exist.
         */
        int ReadPacketFromTransport();

        /** 
         * Sequentially reads the byte value from the JDWP packet's data. 
         *
         * @return Next byte value from the packet's data.
         *
         * @throws InternalErrorException if the number of bytes in the packet
         *         data is not enough.
         */
        jbyte ReadByte();

        /**
         * Sequentially reads boolean value from the JDWP packet's data. 
         *
         * @return The next boolean value from the packet's data.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         */
        jboolean ReadBoolean();

        /** 
         * Sequentially reads int value from the JDWP packet's data. 
         *
         * @return The next int value from the packet's data.
         *
         * @throws InternalErrorException if the number 
         * of bytes in the packet data is not enough.
         */
        jint ReadInt();

        /** 
         * Sequentially reads a long value from the JDWP packet's data.
         *
         * @return A long byte value from the packet's data.
         *
         * @throws InternalErrorException if the number 
         * of bytes in the packet data is not enough.
         */
        jlong ReadLong();
        
        /**
         * Sequentially reads <code>ObjectID</code> value from the JDWP 
         * packet's data.
         * In contrast with the <code>ReadObjectID</code> method, 
         * <code>ReadRawObjectID</code> does not map
         * <code>ObjectID</code> to the jobject.
         *
         * @return The raw <code>ObjectID</code> value read form the packet.
         *
         * @throws InternalErrorException if the number of bytes
         *         in the packet data is not enough.
         */
        ObjectID ReadRawObjectID();

        /** 
         * Sequentially reads <code>ObjectID</code> value from JDWP 
         * packet's data and converts it to <code>jobject</code> through 
         * <code>ObjectManager</code>. Returns the JNI global reference to 
         * <code>jobject</code> or null. 
         * The given global reference is stored inside PacketWrapper 
         * and can be destroyed with the method Reset().
         * The given method should be used only in special cases where null 
         * <code>ObjectID</code> is acceptible.
         * In usual cases the method <code>ReadObjectID()</code> should be used.
         *
         * @param jni - the JNI interface pointer
         * 
         * @return The JNI global reference to <code>jobject</code> or null 
         * reference for null <code>ObjectID</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ObjectID</code> is invalid or the object was 
         *         garbage-collected.
         */
        jobject ReadObjectIDOrNull(JNIEnv *jni);

        /** 
         * Sequentially reads <code>ObjectID</code> value from the JDWP 
         * packet's data and converts it to the <code>jobject</code> 
         * through <code>ObjectManager</code>. Returns the JNI global reference 
         * to the given <code>jobject</code>. The global reference is stored 
         * inside PacketWrapper and can be destroyed 
         * with the method Reset().
         *
         * @param jni - the JNI interface pointer
         *
         * @return The JNI global reference to <code>jobject</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a 
         *         global reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ObjectID</code> is invalid or object was garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) if 
         *         <code>ObjectID</code> is null.
         */
        jobject ReadObjectID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>ReferenceTypeID</code> value from 
         * the JDWP packet's data and converts it to the <code>jclass</code> 
         * through <code>ObjectManager</code>. Returns JNI global reference to the 
         * given <code>jclass</code> or null.
         * The global reference is stored inside PacketWrapper and can 
         * be destroyed with the method Reset().
         * 
         * The given method should be used only in special cases where null 
         * <code>referenceTypeID</code> is acceptible.
         * In usual cases the method <code>ReadReferenceTypeID()</code> should 
         * be used.
         *
         * @param jni - the JNI interface pointer
         * 
         * @return The JNI global reference to the <code>jobject</code>.
         *
         * @throws InternalErrorException if the number of bytes in 
         *         the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws <code>AgentException(JDWP_ERROR_INVALID_CLASS)</code> if 
         *         <code>ReferenceTypeID</code> is invalid.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         the corresponding class was garbage-collected.
         */
        jclass ReadReferenceTypeIDOrNull(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>ReferenceTypeID</code> value from the 
         * JDWP packet's data and converts it to the <code>jclass</code> through 
         * <code>ObjectManager</code>. Returns JNI global reference to that 
         * <code>jclass</code>.
         * The given global reference is stored inside PacketWrapper
         * and can be destroyed with the method Reset().
         * The given method should be used only in special cases where null 
         * <code>referenceTypeID</code> is acceptible.
         * In usual cases the method <code>ReadReferenceTypeID()</code> should 
         * be used.
         *
         * @param jni - the JNI interface pointer
         * 
         * @return The JNI global reference to the <code>jobject</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create global 
         *         reference.
         * @throws <code>AgentException(JDWP_ERROR_INVALID_CLASS)</code> 
         *         if <code>ReferenceTypeID</code> is invalid.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) 
         *         if the corresponding class was garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) 
         *         if <code>ReferenceTypeID</code> is null.
         */
        jclass ReadReferenceTypeID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>FieldID</code> value from the JDWP 
         * packet's data and converts it to the <code>jfieldID</code> 
         * through <code>ObjectManager</code>.
         *
         * @param jni - the JNI interface pointer
         *
         * @return <code>jfieldID</code>
         *
         * @throws InternalErrorException if the number of 
         *         bytes in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create 
         *         a global reference.
         */
        jfieldID ReadFieldID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>MethodID</code> value from the JDWP 
         * packet's data and converts it to the <code>jmethodID</code> 
         * through <code>ObjectManager</code>.
         *
         * @param jni - the JNI interface pointer
         *
         * @return <code>jmethodID</code>
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create 
         *         a global reference.
         */
        jmethodID ReadMethodID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>FrameID</code> value from the JDWP 
         * packet's data and converts it to the <code>jint</code> through 
         * <code>ObjectManager</code>.
         *
         * @param jni - the JNI interface pointer
         * @return <code>jint</code>
         *
         * @throws InternalErrorException if the number of 
         *         bytes in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a 
         *         global reference.
         */
        jint ReadFrameID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>Location</code> object from the 
         * JDWP packet's data and converts it to <code>jdwpLocation</code> 
         * through <code>ObjectManager</code>.
         *
         * @param jni - the JNI interface pointer
         * 
         * @return <code>jdwpLocation</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a 
         *         global reference.
         */
        jdwpLocation ReadLocation(JNIEnv *jni);

        /** 
         * Sequentially reads <code>ThreadID</code> value from JDWP 
         * packet's data and converts it to <code>jthread</code> through 
         * <code>ObjectManager</code>. Returns the JNI global reference to 
         * <code>jthread</code> or null. 
         * The given global reference is stored inside PacketWrapper 
         * and can be destroyed with the method Reset().
         * The given method should be used only in special cases where null 
         * <code>ThreadID</code> is acceptible.
         * In usual cases the method <code>ReadThreadID()</code> should be used.
         *
         * @param jni - the JNI interface pointer
         * 
         * @return The JNI global reference to <code>jthread</code> or null 
         * reference for null <code>ThreadID</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ThreadID</code> is invalid or the object was 
         *         garbage-collected.
         */
        jthread ReadThreadIDOrNull(JNIEnv *jni);
        
        /** 
         * Sequentially reads the <code>ThreadID</code> value from the JDWP
         * packet's data and convert it to <code>jthreadGroup</code> through 
         * <code>ObjectManager</code>. Returns JNI global reference to the 
         * given object. 
         * This global reference is stored inside PacketWrapper 
         * and can be destroyed with the method Reset().
         *
         * @param jni - the JNI interface pointer
         *
         * @return The JNI global reference to <code>jthread</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ThreadID</code> is invalid or object was garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) if 
         *         <code>ThreadID</code> is null.
         */
        jthread ReadThreadID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>ThreadGroupID</code> value from the 
         * JDWP packet's data and converts it to <code>jthreadGroup</code> 
         * through <code>ObjectManager</code>. Returns the JNI global reference 
         * to the given object. 
         * This global reference is stored inside PacketWrapper 
         * and can be destroyed with the method Reset().
         *
         * @param jni - the JNI interface pointer
         *
         * @return The JNI global reference to <code>jthreadGroup</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ThreadGroupID</code> is invalid or the object was 
         *         garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) if 
         *         <code>ThreadGroupID</code> is null.
         */
        jthreadGroup ReadThreadGroupID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>StringID</code> value from the JDWP 
         * packet's data. 
         * <code>StringID</code> is mapped through <code>ObjectManager</code> 
         * to <code>jstring</code>. 
         * <code>ReadStringID()</code> returns the JNI global reference to that
         * <code>jstring</code>.
         * The given global reference is stored inside PacketWrapper
         * and can be destroyed through the <code>Reset(JNIEnv *)</code> method.
         *
         * @param jni - the JNI interface pointer
         *
         * @return The JNI global reference to <code>jstring</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>StringID</code> is invalid or the object was 
         *         garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) 
         *         if <code>StringID</code> is null.
         */
        jstring ReadStringID(JNIEnv *jni);

        /** 
         * Sequentially reads the <code>ArrayID</code> value from the JDWP 
         * packet's data. 
         * <code>ArrayID</code> is mapped through <code>ObjectManager</code> 
         * to <code>jstring</code>. 
         * <code>ReadStringID()</code> returns the JNI global reference to 
         * that <code>jstring</code>.
         * The given global reference is stored inside PacketWrapper 
         * and can be destroyed through the <code>Reset(JNIEnv *)</code> method.
         *
         * @param jni - the JNI interface pointer
         *
         * @return The JNI global reference to <code>jarray</code>.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ArrayID</code> is invalid or the object was 
         *         garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) if 
         * <code>ArrayID</code> is null.
         */
        jarray ReadArrayID(JNIEnv *jni);

        /** 
         * Sequentially reads the string value from the JDWP packet's data.
         * One does not need to dispose this allocated memory, which is stored 
         * inside PacketWrapper and can be destroyed through the 
         * <code>Reset(JNIEnv *)</code> method.
         *
         * @return The string value.
         *
         * @throws InternalErrorException if the number of bytes 
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         */
        char* ReadString();

        /** 
         * Sequentially reads the string value from the JDWP packet's data.
         * The given method allocates memory for the string value.
         * You must destroy this memory with your own.
         *
         * @return The string value.
         *
         * @throws InternalErrorException if the number of bytes
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         */
        char* ReadStringNoFree();

        /** 
         * Sequentially reads the <code>JDWP.Tag</code> value and the following 
         * JDWP value. If the value is a reference type, it is converted to 
         * <code>jobject</code> through <code>ObjectManager</code> and a global 
         * reference is stored in returning <code>jdwpTaggedValue</code>.
         * All global references are stored inside PacketWrapper
         * and can be destroyed with the method Reset().
         *
         * @param jni - the JNI interface pointer
         * 
         * @return <code>jdwpTaggedValue</code>.
         *
         * @throws InternalErrorException if the number of bytes in 
         *         the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ObjectID</code> is invalid or the object was 
         *         garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) if 
         *         <code>ObjectID</code> is null.
         */
        jdwpTaggedValue ReadValue(JNIEnv *jni);

        /** 
         * Sequentially reads the JDWP value for the selected <code>JDWP.Tag</code> 
         * value. If the value is a reference type, it is converted to 
         * <code>jobject</code> through <code>ObjectManager</code> and a global 
         * reference is returned.
         * All global references are stored inside PacketWrapper 
         * and can be destroyed with the method Reset().
         *
         * @param jni - the JNI interface pointer
         * @param tag - the <code>jdwpTag</code> tag
         *
         * @return <code>jvalue</code>.
         *
         * @throws InternalErrorException if the number of bytes
         *         in the packet data is not enough.
         * @throws OutOfMemoryException if cannot create a global 
         *         reference.
         * @throws AgentException(JDWP_ERROR_INVALID_OBJECT) if 
         *         <code>ObjectID</code> is invalid or the object was 
         *         garbage-collected.
         * @throws AgentException(JDWP_ERROR_NULL_POINTER) if 
         *         <code>ObjectID</code> is null.
         */
        jvalue ReadUntaggedValue(JNIEnv *jni, jdwpTag tag);

        /** 
         * Disposes all stored global references and allocated strings 
         * and prepares InputPacketParser  for
         * working with the next packets.
         * 
         * @param jni - the JNI interface pointer
         */
        void Reset(JNIEnv *jni);

        /** 
         * Moves all data to other InputPacketParser  instance. 
         * 
         * @param jni - the JNI interface pointer
         * @param to  - another InputPacketParser 
         */
        void MoveData(JNIEnv *jni, InputPacketParser* to);

        void ReleaseData();

    protected:
        jchar ReadChar();
        jshort ReadShort();
        jfloat ReadFloat();
        jdouble ReadDouble();

    private:
        int m_position;

        void ReadBigEndianData(void* data, int len); 
        //void ReadRawData(void* data, int len);
    };

    /**
     * OutputPacketComposer  class supports writing data to
     * <code>jdwpPacket</code>. Inherited from PacketWrapper 
     * it contain methods to read <code>jdwpPacket</code> header values 
     * and extend methods for writing packet's headers and incremental 
     * writing packet's data. 
     */
    class OutputPacketComposer : public PacketWrapper {

    public:

        /** 
         * Gets current position in reply packet,
         * to which next data will be written.
         * 
         * @return the current position in reply packet.
         */
        size_t GetPosition();

        void ReleaseData();

        /** 
         * Sets current position in reply packet
         * to the newPosition, passed as parameter.
         * Next data will be written in reply packet
         * starting with the newPosition.
         * This function is intended to rewrite some written
         * data with new value and should be used carefully.
         * Exemplary scenario is the following:
         * - currentPosition = GetPosition();
         * - currentLength = GetLength();
         * - SetPosition(newPosition);
         * - Rewriting data to the newPosition;
         * - SetPosition(currentPosition);
         * - SetLength(currentLength);
         * 
         * @param newPosition - new position in reply packet.
         */
        void SetPosition(size_t newPosition);

        /** 
         * Sets current length of jdwp packet
         * to the new value, passed as parameter.
         * This function can be used to restore length 
         * of reply packet to the right value after
         * rewriting some data in packet and,
         * in this case, it should be used carefully.
         * Exemplary scenario is the following:
         * - currentPosition = GetPosition();
         * - currentLength = GetLength();
         * - SetPosition(newPosition);
         * - Rewriting data to the newPosition;
         * - SetPosition(currentPosition);
         * - SetLength(currentLength);
         * 
         * @param newLength - new length of reply packet.
         */
        void SetLength(jint newLength) { m_packet.type.cmd.len = newLength; }

        /**
         * Creates an empty instance of OutputPacketComposer .
         */
        OutputPacketComposer()
            : PacketWrapper(), m_position(0), m_allocatedSize(0)
            , m_registeredObjectIDTable(0), m_registeredObjectIDCount(0)
            , m_registeredObjectIDTableSise(0) {}

        /**
         * Fills header fields with the values specific for the new JDWP reply.
         *
         * @param id        - the reply ID
         * @param errorCode - the JDWP error code
         */
        void CreateJDWPReply(jint id, jdwpError errorCode);

        /**
         * Fills header fields with the values specific for the new JDWP event.
         *
         * @param id         - event ID
         * @param commandSet - the JDWP command set
         * @param command    - the JDWP command
         *
         * @throws InternalErrorException
         */
        void CreateJDWPEvent(jint id, jdwpCommandSet commandSet, jdwpCommand command);
        
        /**
         * Writes an enclosed packet to transport.
         * 
         * @throws TransportException.
         */
        int WritePacketToTransport();

        /** 
         * Sets an error code.
         * Should be used for the JDWP reply only.
         *
         * @param error - the JDWP error code
         */
        void SetError(jdwpError error) {
            m_packet.type.reply.errorCode = (jshort)error;
        }

        /**
         * Sequentially writes the byte value to the JDWP packet's data.
         *
         * @param value - the <code>jbyte</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteByte(jbyte value);

        /**
         * Sequentially writes the boolean value to the JDWP packet's data.
         * 
         * @param value - the <code>jboolean</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteBoolean(jboolean value);

        /**
         * Sequentially writes the int value to the JDWP packet's data.
         *
         * @param value - the <code>jint</code> value
         * @throws OutOfMemoryException.
         */
        void WriteInt(jint value);

        /**
         * Sequentially writes the long value to the JDWP packet's data.
         *
         * @param value- the <code>jlong</code> value
         * @throws OutOfMemoryException.
         */
        void WriteLong(jlong value);
        
        /**
         * Sequentially writes the <code>jobject</code> value to the 
         * JDWP packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jobject</code> value
         *
         * @throws AgentException.
         */
        void WriteObjectID(JNIEnv *jni, jobject value);

        /**
         * Sequentially writes the <code>jclass</code> value to  
         * the JDWP packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jclass</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteReferenceTypeID(JNIEnv *jni, jclass value);

        /**
         * Sequentially writes the <code>jfieldID</code> value to 
         * the JDWP packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jfieldID</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteFieldID(JNIEnv *jni, jfieldID value);

        /**
         * Sequentially writes the <code>jmethodID</code> value to the JDWP
         * packet's data.
         * 
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jmethodID</code> value
         * 
         * @throws OutOfMemoryException.
         */
        void WriteMethodID(JNIEnv *jni, jmethodID value);

        /**
         * Sequentially writes the <code>jthread</code> value to the JDWP 
         * packet's data.
         *
         * @param jni         - the JNI interface pointer
         * @param jvmThread   - the Java thread
         * @param frameDepth  - the frame depth
         * @param framesCount - the number of frames
         *
         * @throws OutOfMemoryException.
         */
        void WriteFrameID(JNIEnv *jni, jthread jvmThread, jint frameDepth, jint framesCount);

        /**
         * Sequentially writes the location value to the JDWP packet's data.
         *
         * @param jni      - the JNI interface pointer
         * @param typeTag  - the JDWP type tag
         * @param classID  - the Java class
         * @param methodID - the Java method ID
         * @param location - the Java location
         *
         * @throws OutOfMemoryException.
         */
        void WriteLocation(JNIEnv *jni, jdwpTypeTag typeTag, jclass classID, jmethodID methodID, jlocation location);

        /**
         * Sequentially writes the location value to the JDWP packet's data.
         * 
         * @param jni      - the JNI interface pointer
         * @param location - the JDWP location
         *
         * @throws OutOfMemoryException.
         */
        void WriteLocation(JNIEnv *jni, jdwpLocation *location);

        /**
         * Sequentially writes the <code>jthread</code> value to the JDWP 
         * packet's data.
         * 
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jthread</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteThreadID(JNIEnv *jni, jthread value);

        /**
         * Sequentially writes the <code>jthreadGroup</code> value to the JDWP 
         * packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jthreadGroup</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteThreadGroupID(JNIEnv *jni, jthreadGroup value);

        /**
         * Sequentially writes the <code>jstring</code> value to the JDWP 
         * packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jstring</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteStringID(JNIEnv *jni, jstring value);

        /**
         * Sequentially writes the <code>jarray</code> value to the JDWP 
         * packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param value - the <code>jarray</code> value
         *
         * @throws OutOfMemoryException.
         */
        void WriteArrayID(JNIEnv *jni, jarray value);
        
        /**
         * Sequentially writes the string to the JDWP packet's data.
         *
         * @param value - a null-terminated string
         *
         * @throws OutOfMemoryException.
         */
        void WriteString(const char* value);

        /**
         * Sequentially writes the string to the JDWP packet's data.
         *
         * @param value  - the string
         * @param length - the string length
         *
         * @throws OutOfMemoryException.
         */
        void WriteString(const char* value, jint length);

        /**
         * Sequentially writes the tagged-object ID.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @throws OutOfMemoryException.
         */
        void WriteTaggedObjectID(JNIEnv *jni, jobject object);

        /**
         * Sequentially writes the value object to the JDWP packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param tag   - the JDWP tag
         * @param value - the Java value object
         *
         * @throws OutOfMemoryException.
         */
        void WriteValue(JNIEnv *jni, jdwpTag tag, jvalue value);

        /**
         * Sequentially writes the array of value objects to the JDWP packet's 
         * data.
         *
         * @param jni    - the JNI interface pointer
         * @param tag    - the JDWP tag
         * @param length - the count of the Java value objects
         * @param value  - the array of the Java value objects
         *
         * @throws OutOfMemoryException.
         */
        void WriteValues(JNIEnv *jni, jdwpTag tag, jint length, jvalue* value);

        /**
         * Sequentially writes the untagged value object to the JDWP 
         * packet's data.
         *
         * @param jni   - the JNI interface pointer
         * @param tag   - the JDWP tag
         * @param value - the Java value object
         *
         * @throws OutOfMemoryException.
         */
        void WriteUntaggedValue(JNIEnv *jni, jdwpTag tag, jvalue value);

        /**
         * Sequentially writes the array length, then the byte array values 
         * to the JDWP packet's data.
         *
         * @param byte   - the pointer to the byte array
         * @param length - the byte array length
         *
         * @throws OutOfMemoryException.
         */
        void WriteByteArray(jbyte* byte, jint length);

        /** 
         * Disposes all stored references and prepares 
         * OutputPacketComposer  for working with the next packets.
         * 
         * @param jni - the JNI interface pointer
         */
        void Reset(JNIEnv *jni);

        /** 
         * Moves all data to another OutputPacketComposer  instance.
         *
         * @param jni - the JNI interface pointer
         * @param to  - the pointer to the instance of 
         *              OutputPacketComposer 
         */
        void MoveData(JNIEnv *jni, OutputPacketComposer* to);

    protected:
        void SetId(jint id) { m_packet.type.cmd.id = id; }
        void SetFlags(jbyte flags) { m_packet.type.cmd.flags = flags; }
        void SetCommandSet(jdwpCommandSet cmdSet) { m_packet.type.cmd.cmdSet = (jbyte)cmdSet; }
        void SetCommand(jdwpCommand command) { m_packet.type.cmd.cmd = (jbyte)command; }

        void WriteChar(jchar value);
        void WriteShort(jshort value);
        void WriteFloat(jfloat value);
        void WriteDouble(jdouble value);

    private:
        size_t m_position;
        size_t m_allocatedSize;
        
        ObjectID *m_registeredObjectIDTable;
        int m_registeredObjectIDCount;
        int m_registeredObjectIDTableSise; // in ObjectID

        void AllocateMemoryForData(int length);
        void WriteData(const void* data, int length);
        void WriteRawData(const void* data, int length);
        void WriteBigEndianData(void* data, int length);

        /** 
         * Registers given <code>objectID</code> in a special table containing 
         * all <code>ObjectID</code> values sent to the debugger by the JDWP agent 
         * in this reply packet. The given table is used to increase the count 
         * of references for all registered <code>ObjectID</code> values. For more 
         * information see the <code>IncreaseObjectIDRefCounts()</code> function.
         *
         * @param objectID - the <code>objectID</code> to be registered
         *
         * @exception OutOfMemoryException the same as 
         *            <code>AgentException (JDWP_ERROR_OUT_OF_MEMORY)</code> - if an
         *            out-of-memory error has occurred during execution of this 
         *            function while allocating memory for the table.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        void RegisterObjectID(ObjectID objectID);

        /** 
         * Increases by one the count of references for all <code>ObjectID</code> values
         * registered in the reply packet. This count for each <code>objectID</code>
         * means how many times given <code>objectID</code> was sent to the debugger 
         * by the JDWP agent as a part of the reply data in the reply packet. The 
         * count is used for correct disposing of given <code>objectID</code>. 
        */
        void IncreaseObjectIDRefCounts();

    };

    /**
     * The CommandParser class is a container for a pair of 
     * InputPacketParser and OutputPacketComposer  
     * packets that represents reply to the JDWP command.
     */
    class CommandParser : public AgentBase {

    public:

        /**
         * The given method initializes command and reply structures.
         * There should be no access for command and reply packet's data before
         * invoking ReadPacket().
         * 
         * @exception TransportException.
         */
        int ReadCommand();

        /**
         * Writes the reply packet to the transport and resets the 
         * CommandParser  object.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception TransportException
         */
        int WriteReply(JNIEnv *jni);

        /**
         * Moves all data to another CommandParser  instance.
         *
         * @param jni - the JNI interface pointer
         * @param to  - the pointer to another CommandParser 
         */
        void MoveData(JNIEnv *jni, CommandParser* to);

        /**
         * Public field for working with InputPacketParser .
         */
        InputPacketParser command;

        /**
         * Public field for working with OutputPacketComposer .
         */
        OutputPacketComposer reply;

        /**
         * Resets the given CommandParser  object.
         */
        void Reset(JNIEnv *jni);

        ~CommandParser();

    private:

    };

    /**
     * The CommandParser  class is a container for the 
     * OutputPacketComposer  packet representing the JDWP event.
     */
    class EventComposer : public AgentBase {

    public:

        /**
         * A constructor.
         *
         * @param id         - the JDWP event ID
         * @param commandSet - the JDWP command set
         * @param command    - the JDWP command
         * @param sp         - the suspend policy
         */
        EventComposer(jint id, jdwpCommandSet commandSet, jdwpCommand command,
            jdwpSuspendPolicy sp);

        ~EventComposer();

        /**
         * Writes a thread ID to the output packet and creates a global reference
         * to the thread.
         *
         * @param jni    - the JNI interface pointer
         * @param thread - the Java thread
         *
         * @exception OutOfMemoryException.
         */
        void WriteThread(JNIEnv *jni, jthread thread);

        /**
         * Disposes all stored references and prepares CommandParser 
         * for reading the next packets.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception TransportException.
         */
        int WriteEvent(JNIEnv *jni);

        /**
         * Resets the current JDWP event.
         *
         * @param jni - the JNI interface pointer
         */
        void Reset(JNIEnv *jni);

        /**
         * Gets a thread suspend policy.
         */
        jdwpSuspendPolicy GetSuspendPolicy() { return m_suspendPolicy; }

        /**
         * Gets a Java thread.
         */
        jthread GetThread() { return m_thread; }

        /**
         * The packet was written to the transport.
         */
        bool IsSent() { return m_isSent; }

        /**
         * The thread is waiting on a suspension point.
         */
        bool IsWaiting() { return m_isWaiting; }

        /**
         * Notifies that the thread is waiting on a suspension point.
         */
        void SetWaiting(bool waiting) { m_isWaiting = waiting; }

        /**
         * The thread is released after a suspension.
         */
        bool IsReleased() { return m_isReleased; }

        /**
         * Releases the thread after a suspension.
         */
        void SetReleased(bool released) { m_isReleased = released; }

        /**
         * The auto-death event.
         */
        bool IsAutoDeathEvent() { return m_isAutoDeathEvent; }

        /**
         * Sets the auto-death event.
         */
        void SetAutoDeathEvent(bool yes) { m_isAutoDeathEvent = yes; }

        /**
         * A public field for working with <code>OutputPacketComposer<code/>.
         */
        OutputPacketComposer event;

    private:

        jthread m_thread;
        jdwpSuspendPolicy m_suspendPolicy;
        volatile bool m_isSent;
        volatile bool m_isReleased;
        volatile bool m_isWaiting;
        volatile bool m_isAutoDeathEvent;

    };

}

#endif // _PACKET_PARSER_H_
