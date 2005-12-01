/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdlib.h>
#include "jclglob.h"
#include "nethelp.h"
#include "helpers.h"
#include "jclprots.h"

#if defined(LINUX)
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>   /* for struct in_addr */
#include <sys/ioctl.h>
#include <net/if.h>       /* for struct ifconf */
#endif

#include "portsock.h"

void setDatagramPacketAddress (JNIEnv * env, jobject datagramPacket,
             jobject anInetAddress);
void setDatagramPacketPort (JNIEnv * env, jobject datagramPacket, U_16 hPort);
void updateAddress (JNIEnv * env, hysockaddr_t sockaddrP,
        jobject senderAddress);
void updatePacket (JNIEnv * env, hysockaddr_t sockaddrP,
       jobject datagramPacket, I_32 bytesRead);
void setDatagramPacketLength (JNIEnv * env, jobject datagramPacket,
            I_32 length);

/**
 * A helper method, to set the remote address into the DatagramPacket.
 *
 * @param env           pointer to the JNI library
 * @param datagramPacket  pointer to the java DatagramPacket object to update
 * @param anInetAddress   pointer to the java InetAddress to update the packet with
 *
 */

void
setDatagramPacketAddress (JNIEnv * env, jobject datagramPacket,
        jobject anInetAddress)
{
  jfieldID fid = JCL_CACHE_GET (env, FID_java_net_DatagramPacket_address);
  (*env)->SetObjectField (env, datagramPacket, fid, anInetAddress);
}

/**
 * A helper method, to set the remote port into the java DatagramPacket.
 *
 * @param env           pointer to the JNI library
 * @param datagramPacket  pointer to the java DatagramPacket object to update
 * @param hPort         the port value to update the packet with, in host order
 */

void
setDatagramPacketPort (JNIEnv * env, jobject datagramPacket, U_16 hPort)
{
  jfieldID fid = JCL_CACHE_GET (env, FID_java_net_DatagramPacket_port);
  (*env)->SetIntField (env, datagramPacket, fid, hPort);
}

/**
 * A helper method, to set the data length into a java DatagramPacket.
 *
 * @param env           pointer to the JNI library
 * @param datagramPacket  pointer to the java DatagramPacket object to update
 * @param length          the length value to update the packet with
 */

void
setDatagramPacketLength (JNIEnv * env, jobject datagramPacket, I_32 length)
{
  jfieldID fid = JCL_CACHE_GET (env, FID_java_net_DatagramPacket_length);
  (*env)->SetIntField (env, datagramPacket, fid, length);
}

/**
 * A helper method, to update the java DatagramPacket argument.  Used after receiving a datagram packet, 
 * to update the DatagramPacket with the network address and port of the sending machine.
 *
 * @param env           pointer to the JNI library
 * @param sockaddrP     pointer to the hysockaddr struct with the sending host address & port
 * @param datagramPacket  pointer to the java DatagramPacket object to update
 * @param bytesRead     the bytes read value to update the packet with
 */

void
updatePacket (JNIEnv * env, hysockaddr_t sockaddrP, jobject datagramPacket,
        I_32 bytesRead)
{
  PORT_ACCESS_FROM_ENV (env);
  jobject anInetAddress;
  U_16 nPort;
  U_32 length;
  U_32 scope_id = 0;
  jbyte byte_array[HYSOCK_INADDR6_LEN];
  hysock_sockaddr_address6 (sockaddrP, (U_8 *) byte_array, &length,
          &scope_id);

  nPort = hysock_sockaddr_port (sockaddrP);
  anInetAddress =
    newJavaNetInetAddressGenericB (env, byte_array, length, scope_id);

  setDatagramPacketAddress (env, datagramPacket, anInetAddress);
  setDatagramPacketPort (env, datagramPacket, hysock_ntohs (nPort));
  setDatagramPacketLength (env, datagramPacket, bytesRead);
}

/**
 * A helper method, to set address of the java InetAddress argument.
 *
 * @param env           pointer to the JNI library
 * @param sockaddrP     pointer to the hysockaddr struct containing the network address
 * @param senderAddress pointer to the java InetAddress object to update
 */

void
updateAddress (JNIEnv * env, hysockaddr_t sockaddrP, jobject senderAddress)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte ipv4Addr[16];
  U_32 length;
  U_32 scope_id = 0;
  hysock_sockaddr_address6 (sockaddrP, (U_8 *) ipv4Addr, &length, &scope_id);
  (*env)->SetObjectField (env, senderAddress,
        JCL_CACHE_GET (env,
           FID_java_net_InetAddress_address),
        newJavaByteArray (env, ipv4Addr, length));
  if (jcl_supports_ipv6 (env) && (scope_id != 0))
    {
      jclass tempClass = JCL_CACHE_GET (env, CLS_java_net_InetAddress);
      jfieldID fid = NULL;

      fid = (*env)->GetFieldID (env, tempClass, "scope_id", "I");
      if ((*env)->ExceptionCheck (env))
        {
          (*env)->ExceptionClear (env);
        }
      else
        {
          (*env)->SetIntField (env, senderAddress, fid, scope_id);
        }
    }
}

/* HAS_JAVA_NET_CONNECT_EXCEPTION */
#define HAS_JAVA_NET_CONNECT_EXCEPTION
/**
 * Create a new socket, for datagrams.  The system socket is created and 'linked' to the
 * the java PlainDatagramSocketImpl by setting the file descriptor value (which is an integer 
 * reference to socket maintained by the system).
 *
 * @param env       pointer to the JNI library
 * @param thisClz     pointer to the class of the receiver (of the java message)
 * @param thisObjFD pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param preferIPv4Stack if application preference is to use only IPv4 sockets (default is false)
 */

void JNICALL
Java_java_net_PlainDatagramSocketImpl_createDatagramSocketImpl (JNIEnv * env,
                jclass
                thisClz,
                jobject
                thisObjFD,
                jboolean
                preferIPv4Stack)
{
  createSocket (env, thisObjFD, HYSOCK_DGRAM, preferIPv4Stack);
}

/**
 * Peek for data available for reading on the socket and answer the sending host address and port.
 * This call is used to enforce secure reads.  The peek function does not remove data from the system
 * input queue, so that if the originating host address/port is acceptable to the security policy, a subsequent
 * read operation may be issued to get the data.  The peek is implemented as a recvfrom with the peek flag set,
 * so the call otherwise behaves as a recvfrom call.
 *
 * @param env           pointer to the JNI library
 * @param thisClz         pointer to the class of the receiver (of the java message)
 * @param fileDescriptor    pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param senderAddress   pointer to the java InetAddress object to update with the sender address
 * @param timeout       the read timeout, in milliSeconds
 *
 * @return  the port on the host sending the data
 * @exception SocketException if an error occurs during the call
 */

jint JNICALL
Java_java_net_PlainDatagramSocketImpl_peekDatagramImpl (JNIEnv * env,
              jclass thisClz,
              jobject
              fileDescriptor,
              jobject senderAddress,
              jint timeout)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t hysocketP;
  hysockaddr_struct sockaddrP;
  char msg[1] = { 0 };
  I_32 msgLen = 1;
  I_32 result;
  I_32 flags = 0;
  jint hport;
  jbyte nlocalAddrBytes[HYSOCK_INADDR6_LEN];

  result = pollSelectRead (env, fileDescriptor, timeout, TRUE);
  if (0 > result)
    return (jint) 0;

  hysocketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (hysocketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return (jint) 0;
    }

  hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nlocalAddrBytes,
       HYSOCK_INADDR_LEN, HYADDR_FAMILY_AFINET4, 0, 0, 0,
       hysocketP);

  result = hysock_setflag (HYSOCK_MSG_PEEK, &flags);
  if (0 > result)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
  result =
    hysock_readfrom (hysocketP, (U_8 *) msg, msgLen, flags, &sockaddrP);

/* Note, the msgsize error is acceptable as the read buffer was set to a nominal length.
  Updating sockaddrP is the purpose of this call. */
  if (result < 0 && result != HYPORT_ERROR_SOCKET_MSGSIZE)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
  else
    {
      updateAddress (env, &sockaddrP, senderAddress);
      hport = (jint) hysock_ntohs (hysock_sockaddr_port (&sockaddrP));
      return hport;
    }
}

void JNICALL
Java_java_net_PlainDatagramSocketImpl_oneTimeInitialization (JNIEnv * env,
                   jclass clazz,
                   jboolean
                   ipv6support)
{
  jclass lookupClass;
  jfieldID fid;

  netInitializeIDCaches (env, ipv6support);

  lookupClass = (*env)->FindClass (env, "java/net/DatagramPacket");
  if (!lookupClass)
    return;

  fid =
    (*env)->GetFieldID (env, lookupClass, "address",
      "Ljava/net/InetAddress;");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_net_DatagramPacket_address, fid);

  fid = (*env)->GetFieldID (env, lookupClass, "length", "I");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_net_DatagramPacket_length, fid);

  fid = (*env)->GetFieldID (env, lookupClass, "port", "I");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_net_DatagramPacket_port, fid);
}

/**
 * Receive data on this socket and update the DatagramPacket with the data and sender address/port.
 * If the timeout value is 0, the call may block indefinitely waiting for data otherwise
 * if no data is received within the timeout, it will return throw an exception.  Note, the
 * data & msgLength arguments are fields within the DatagramPacket, passed explicitly to
 * save doing accesses within the native code.
 *
 * @param env           pointer to the JNI library
 * @param thisClz         pointer to the class of the receiver (of the java message)
 * @param fileDescriptor    pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param datagramPacket  pointer to the java DatagramPacket object to update with data & address/port information
 * @param data            pointer to the java read buffer
 * @param   offset            offset into the buffer to start reading data from
 * @param msgLength     the length of the read buffer
 * @param timeout       the read timeout, in milliSeconds
 * @param   peek          choice whether to peek or receive the datagram
 *
 * @return  the number of bytes read
 * @exception InterruptedIOException, SocketException if an error occurs during the call
 */

jint JNICALL
Java_java_net_PlainDatagramSocketImpl_receiveDatagramImpl2 (JNIEnv * env,
                  jclass thisClz,
                  jobject
                  fileDescriptor,
                  jobject
                  datagramPacket,
                  jbyteArray data,
                  jint offset,
                  jint msgLength,
                  jint timeout,
                  jboolean peek)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t hysocketP;
  hysockaddr_struct sockaddrP;
  jbyte *message;
  I_32 result, localCount;
  I_32 flags = HYSOCK_NOFLAGS;
  jbyte nlocalAddrBytes[HYSOCK_INADDR6_LEN];

  result = pollSelectRead (env, fileDescriptor, timeout, TRUE);
  if (0 > result)
    return (jint) 0;

  hysocketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (hysocketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return (jint) 0;
    }

  hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nlocalAddrBytes,
       HYSOCK_INADDR_LEN, HYADDR_FAMILY_AFINET4, 0, 0, 0,
       hysocketP);

  localCount = (msgLength < 65536) ? msgLength : 65536;
  message = jclmem_allocate_memory (env, localCount);
  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  if (peek)
    {
      result = hysock_setflag (HYSOCK_MSG_PEEK, &flags);
      if (result)
        {
          jclmem_free_memory (env, message);
          throwJavaNetSocketException (env, result);
          return (jint) 0;
        }
    }
  result =
    hysock_readfrom (hysocketP, message, localCount, flags, &sockaddrP);
  if (result > 0)
    (*env)->SetByteArrayRegion (env, data, offset, result, message);
  jclmem_free_memory (env, message);
  if (result < 0)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
  else
    {
      updatePacket (env, &sockaddrP, datagramPacket, result);
      return (jint) result;
    }
}

/**
 * Disconnect the Datagram socket.  This allows the socket to be used to sendto and or receive from any addres
*  once again
 *
 * @param env           pointer to the JNI library
 * @param thisClz         pointer to the class of the receiver (of the java message)
 * @param fileDescriptor      pointer to the socket file descriptor
 *
 * @exception SocketException if an error occurs disconneting from the remote host
 */
void JNICALL
Java_java_net_PlainDatagramSocketImpl_disconnectDatagramImpl (JNIEnv * env,
                    jclass thisClz,
                    jobject
                    fileDescriptor)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte nAddrBytes[HYSOCK_INADDR6_LEN];
  U_16 nPort = 0;
  I_32 result;
  hysocket_t socketP;
  hysockaddr_struct sockaddrP;

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }

  /* the address itself should not matter as the protocol family is AF_UNSPEC.  This tells connect to 
     disconnect the Datagram */
  memset (nAddrBytes, 0, HYSOCK_INADDR6_LEN);
  hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, HYSOCK_INADDR_LEN,
       HYADDR_FAMILY_UNSPEC, nPort, 0, 0, socketP);

  /* there is the possiblity of an exception here */
  result = hysock_connect (socketP, &sockaddrP);

  /* will likely need to eat the correct exception here.  Leave as is until we figure out what that exception will be */
  if (0 != result)
    {
      throwJavaNetSocketException (env, result);
      return;
    }
}

/**
 * Receive data on this socket and update the DatagramPacket with the data and sender address/port.
 * If the timeout value is 0, the call may block indefinitely waiting for data otherwise
 * if no data is received within the timeout, it will return throw an exception.  Note, the
 * data & msgLength arguments are fields within the DatagramPacket, passed explicitly to
 * save doing accesses within the native code.
 *
 * @param env         pointer to the JNI library
 * @param thisClz       pointer to the class of the receiver (of the java message)
 * @param fileDescriptor    pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param datagramPacket  pointer to the java DatagramPacket object to update with data & address/port information
 * @param data          pointer to the java read buffer
 * @param offset             offset into the buffer to start reading data from
 * @param msgLength     the length of the read buffer
 * @param timeout       the read timeout, in milliSeconds
 * @param peek                       consume the data packet or not
 *
 * @return  the number of bytes read
 * @exception InterruptedIOException, SocketException if an error occurs during the call
 */

jint JNICALL
Java_java_net_PlainDatagramSocketImpl_recvConnectedDatagramImpl (JNIEnv * env,
                 jclass
                 thisClz,
                 jobject
                 fileDescriptor,
                 jobject
                 datagramPacket,
                 jbyteArray
                 data,
                 jint offset,
                 jint
                 msgLength,
                 jint timeout,
                 jboolean
                 peek)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t hysocketP;
  jbyte *message;
  I_32 result;
  I_32 localCount;
  I_32 flags = HYSOCK_NOFLAGS;

  /* check if there is any data to be read before we go ahead and do the read */
  result = pollSelectRead (env, fileDescriptor, timeout, TRUE);
  if (0 > result)
    {
      return (jint) 0;
    }

  /* get the handle to the socket */
  hysocketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (hysocketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return (jint) 0;
    }

  /* allocate the buffer into which data will be read */
  localCount = (msgLength < 65536) ? msgLength : 65536;
  message = jclmem_allocate_memory (env, localCount);
  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }

  /* check for peek option, if so set the appropriate flag */
  if (peek)
    {
      result = hysock_setflag (HYSOCK_MSG_PEEK, &flags);
      if (result)
        {
          jclmem_free_memory (env, message);
          throwJavaNetSocketException (env, result);
          return (jint) 0;
        }
    }

  /* read the data and copy it to the return array, then free the buffer as we
     no longer need it */
  result = hysock_read (hysocketP, message, localCount, flags);
  if (result > 0)
    {
      (*env)->SetByteArrayRegion (env, data, offset, result, message);
    }
  jclmem_free_memory (env, message);
  if (result < 0)
    {
      if ((HYPORT_ERROR_SOCKET_CONNRESET == result)
        || (HYPORT_ERROR_SOCKET_CONNECTION_REFUSED == result))
        {
          throwJavaNetPortUnreachableException (env, result);
          return (jint) 0;
        }
      else
        {
          throwJavaNetSocketException (env, result);
          return (jint) 0;
        }
    }
  else
    {
      /* update the packet with the legth of data received.
         Since we are connected we did not get back an address.  This
         address is cached within the PlainDatagramSocket  java object and is filled in at
         the java level */
      setDatagramPacketLength (env, datagramPacket, result);
      return (jint) result;
    }
}

/**
 * Send data on this socket to the nominated host address/port.
 *
 * @param env         pointer to the JNI library
 * @param thisClz       pointer to the class of the receiver (of the java message)
 * @param fileDescriptor    pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param data          pointer to the java read buffer
 * @param offset          offset into the buffer
 * @param msgLength     the length of the read buffer
 * @param bindToDevice    
 *
 * @return  the number of bytes sent
 * @exception SocketException if an error occurs during the call
 */

jint JNICALL
Java_java_net_PlainDatagramSocketImpl_sendConnectedDatagramImpl (JNIEnv * env,
                 jclass
                 thisClz,
                 jobject
                 fileDescriptor,
                 jbyteArray
                 data,
                 jint offset,
                 jint
                 msgLength,
                 jboolean
                 bindToDevice)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte *message;
  I_32 result = 0;
  I_32 sent = 0;
  hysocket_t socketP;
  int flags = HYSOCK_NOFLAGS;

  /* allocate a local buffer into which we will copy the data to be sent and which we will use 
     for the write call */
  message = jclmem_allocate_memory (env, msgLength);
  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  (*env)->GetByteArrayRegion (env, data, offset, msgLength, message);

  do
    {
      /* make sure the socket is still valid */
      socketP =
        (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env,
        fileDescriptor);
      if (!hysock_socketIsValid (socketP))
        {
          throwJavaNetSocketException (env,
            sent ==
            0 ? HYPORT_ERROR_SOCKET_BADSOCKET :
          HYPORT_ERROR_SOCKET_INTERRUPTED);
          return (jint) 0;
        }

      /* try to send the next block of data */
      result =
        hysock_write (socketP, message + sent, (I_32) msgLength - sent,
        flags);
      if (result < 0)
        {
          break;
        }
      sent += result;
    }
  while (sent < msgLength);

  /* ok free the buffer and return the length sent or an exception as appropriate  */
  jclmem_free_memory (env, message);

  if (result < 0)
    {
      if ((HYPORT_ERROR_SOCKET_CONNRESET == result)
        || (HYPORT_ERROR_SOCKET_CONNECTION_REFUSED == result))
        {
          throwJavaNetPortUnreachableException (env, result);
          return (jint) 0;
        }
      else
        {
          throwJavaNetSocketException (env, result);
          return (jint) 0;
        }
    }
  else
    {
      return (jint) result;
    }
}

/**
 * Connect the Datagram socket to the nominated remote host address/port.  The socket may then be used to send
 * and receive data from the remote host.
 *
 * @param env           pointer to the JNI library
 * @param thisClz         pointer to the class of the receiver (of the java message)
 * @param fileDescriptor    pointer to the socket file descriptor
 * @param remotePort      the port on the remote host to connect to
 * @param   trafficClass      trafficClass to be used when the datagram socket is connected
 * @param   inetAddress       the inetAddress object representing the address to be connected on. 
 *
 * @exception SocketException if an error occurs connected to the remote host
 */
void JNICALL
Java_java_net_PlainDatagramSocketImpl_connectDatagramImpl2 (JNIEnv * env,
                  jclass thisClz,
                  jobject
                  fileDescriptor,
                  jint remotePort,
                  jint trafficClass,
                  jobject
                  inetAddress)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte nAddrBytes[HYSOCK_INADDR6_LEN];
  int length;
  U_16 nPort;
  I_32 result;
  hysocket_t socketP;
  hysockaddr_struct sockaddrP;
  U_32 scope_id = 0;

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }

  netGetJavaNetInetAddressValue (env, inetAddress, nAddrBytes, &length);

  nPort = hysock_htons ((U_16) remotePort);
  if (length == HYSOCK_INADDR_LEN)
    {
      hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, length,
           HYADDR_FAMILY_AFINET4, nPort, 0, 0, socketP);
    }
  else
    {
      netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
      hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, length,
           HYADDR_FAMILY_AFINET6, nPort,
           (trafficClass & 0xFF) << 20, scope_id, socketP);
    }

  result = hysock_connect (socketP, &sockaddrP);
  if (0 != result)
    {
      throwJavaNetConnectException (env, result);

      return;
    }
}

/**
 * Send data on this socket to the nominated host address/port.
 *
 * @param env           pointer to the JNI library
 * @param thisClz         pointer to the class of the receiver (of the java message)
 * @param fileDescriptor    pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param data            pointer to the java read buffer
 * @param msgLength     the length of the read buffer
 * @param targetPort        target port, in host order
 * @param   trafficClass      the traffic class value that should be use when sending the datagram
 * @param   inetAddress         object with the address to which the datagram should be sent
 *
 * @return  the number of bytes sent
 * @exception SocketException if an error occurs during the call
 */

jint JNICALL
Java_java_net_PlainDatagramSocketImpl_sendDatagramImpl2 (JNIEnv * env,
               jclass thisClz,
               jobject
               fileDescriptor,
               jbyteArray data,
               jint offset,
               jint msgLength,
               jint targetPort,
               jboolean
               bindToDevice,
               jint trafficClass,
               jobject inetAddress)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte *message;
  jbyte nhostAddrBytes[HYSOCK_INADDR6_LEN];
  int length;

  U_16 nPort;
  I_32 result = 0, sent = 0;
  hysocket_t socketP;
  hysockaddr_struct sockaddrP;
  int flags;
  U_32 scope_id = 0;

  netGetJavaNetInetAddressValue (env, inetAddress, nhostAddrBytes, &length);
  nPort = hysock_htons ((U_16) targetPort);

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (length == HYSOCK_INADDR6_LEN)
    {
      netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
      hysock_sockaddr_init6 (&sockaddrP, nhostAddrBytes, length,
           HYADDR_FAMILY_AFINET6, nPort,
           (trafficClass & 0xFF) << 20, scope_id, socketP);
    }
  else
    {
      hysock_sockaddr_init6 (&sockaddrP, nhostAddrBytes, length,
           HYADDR_FAMILY_AFINET4, nPort, 0, scope_id,
           socketP);
    }

  flags = HYSOCK_NOFLAGS;

  message = jclmem_allocate_memory (env, msgLength);
  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  (*env)->GetByteArrayRegion (env, data, offset, msgLength, message);

  do
    {
      socketP =
        getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
      if (!hysock_socketIsValid (socketP))
        {
          jclmem_free_memory (env, message);
          throwJavaNetSocketException (env,
            sent ==
            0 ? HYPORT_ERROR_SOCKET_BADSOCKET :
          HYPORT_ERROR_SOCKET_INTERRUPTED);
          return (jint) 0;
        }
      result =
        hysock_writeto (socketP, message + sent, (I_32) msgLength - sent,
        flags, &sockaddrP);
      if (result < 0)
        break;
      sent += result;
    }
  while (sent < msgLength);

  jclmem_free_memory (env, message);
  if (result < 0)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
  else
    {
      return (jint) result;
    }
}

/**
 * Bind the socket to the specified local address/port.  This call is made after socket creation
 * and prior to read/write operations.
 *
 * @param env         pointer to the JNI library
 * @param thisClz       pointer to the class of the receiver (of the java message)
 * @param fileDescriptor  pointer to the file descriptor of the socket to bind
 * @param localPort     the port, in host order, to bind the socket on
 * @param   inetAddress     the inetAddres object containing the address to bind on.
 *
 * @exception SocketException if an error occurs during the call
 */

jboolean JNICALL
Java_java_net_PlainDatagramSocketImpl_socketBindImpl2 (JNIEnv * env,
                   jclass thisClz,
                   jobject fileDescriptor,
                   jint localPort,
                   jboolean doDevice,
                   jobject inetAddress)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte nlocalAddrBytes[HYSOCK_INADDR6_LEN];
  int length;
  U_16 nPort;
  I_32 result;
  hysocket_t socketP;
  hysockaddr_struct sockaddrP;
  jboolean bindToDevice = FALSE;
  jboolean equals_address = TRUE;
  U_32 scope_id = 0;

  /* This method still needs work for IPv6 support */

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return 0;
    }
  else
    {
      netGetJavaNetInetAddressValue (env, inetAddress, nlocalAddrBytes,
             &length);

      nPort = hysock_htons ((U_16) localPort);
#if defined(LINUX)
      for (i = 0; i < length; i++)
        {
          if (nlocalAddrBytes[i] != 0)
            {
              equals_address = FALSE;
              break;
            }
        }
      if (doDevice && !equals_address)
        {
          struct ifreq *ifr;
          struct ifconf ifc;
          char *ptr;
          int len = 128 * sizeof (struct ifreq);
          for (;;)
            {
              char *data = jclmem_allocate_memory (env, len);
              if (data == 0)
                {
                throwNewOutOfMemoryError (env,
                  "Cannot allocate SIOCGIFCONF buffer");
                return 0;
                }
              ifc.ifc_len = len;
              ifc.ifc_buf = data;
              if (ioctl ((int) socketP, SIOCGIFCONF, &ifc) != 0)
                {
                jclmem_free_memory (env, ifc.ifc_buf);
                throwJavaNetSocketException (env, errno);
                return 0;
                }
              if (ifc.ifc_len < len)
                break;
              jclmem_free_memory (env, data);
              len += 128 * sizeof (struct ifreq);
            }
          ptr = ifc.ifc_buf;
          while (ptr < (char *) ifc.ifc_buf + ifc.ifc_len)
            {
              struct sockaddr_in *inaddr;
              ifr = (struct ifreq *) ptr;
#if defined(LINUX)
              ptr += sizeof (ifr->ifr_name) + sizeof (struct sockaddr);
#else
              ptr +=
                sizeof (ifr->ifr_name) + max (sizeof (struct sockaddr),
                ifr->ifr_addr.sa_len);
#endif
              /*printf(" addr family: %d (%d)\n", ifr->ifr_addr.sa_family, AF_INET);*/
              inaddr = (struct sockaddr_in *) &ifr->ifr_addr;
              if (length > HYSOCK_INADDR_LEN)
                {
                  equals_address = FALSE;
                }
              else
                {
                 equals_address =
                    inaddr->sin_addr.s_addr == *((int *) nlocalAddrBytes);
                }
              if (ifr->ifr_addr.sa_family == AF_INET && equals_address)
                {
                  char *cptr;
                  /*printf("interface: %s\n", ifr->ifr_name);
                  printf(" addr: %x\n", inaddr->sin_addr.s_addr);*/
                  if ((cptr = strchr (ifr->ifr_name, ':')) != NULL)
                    *cptr = 0;
                  if (ioctl (SOCKET_CAST (socketP), SIOCGIFFLAGS, ifr) != 0)
                    {
                      jclmem_free_memory (env, ifc.ifc_buf);
                      throwJavaNetSocketException (env, errno);
                      return 0;
                    }
                  /*printf("flags: %x UP = %x BROADCAST = %x MULTICAST = %x LOOPBACK = %x POINTOPOINT = %x)\n",
                  ifr->ifr_flags, ifr->ifr_flags & IFF_UP, ifr->ifr_flags & IFF_BROADCAST,
                  ifr->ifr_flags & IFF_MULTICAST, ifr->ifr_flags & IFF_LOOPBACK, ifr->ifr_flags & IFF_POINTOPOINT);*/
                  if (ifr->ifr_flags & IFF_UP
                    && !(ifr->ifr_flags & IFF_POINTOPOINT))
                    {
                      result =
                        setsockopt (SOCKET_CAST (socketP), SOL_SOCKET,
                        SO_BINDTODEVICE, ifr,
                        sizeof (struct ifreq));
                      if (result == 0)
                        {
                          int value = TRUE;
                          memset (nlocalAddrBytes, 0, HYSOCK_INADDR6_LEN);
                          length = 0;
                          bindToDevice = TRUE;

#if defined(LINUX)
                          hysock_setopt_bool (socketP, HY_SOL_SOCKET,
                          HY_SO_REUSEADDR, &value);
#endif
                        }
                    }
                }
            }
          jclmem_free_memory (env, ifc.ifc_buf);
        }
#endif

      if (length == HYSOCK_INADDR6_LEN)
        {
          netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nlocalAddrBytes, length,
            HYADDR_FAMILY_AFINET6, nPort, 0, scope_id,
            socketP);
        }
      else
        {
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nlocalAddrBytes, length,
            HYADDR_FAMILY_AFINET4, nPort, 0, scope_id,
            socketP);
        }

      result = hysock_bind (socketP, &sockaddrP);
      if (0 != result)
        {
          throwJavaNetBindException (env, result);
          return 0;
        }
    }
  return bindToDevice;
}
