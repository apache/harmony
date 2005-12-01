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
#include "nethelp.h"
#include "jclglob.h"
#include "jclprots.h"
#include "portsock.h"

void setSocketImplPort (JNIEnv * env, jobject socketImpl, U_16 hPort);
void setSocketImplAddress (JNIEnv * env, jobject socketImpl,
                           jobject anInetAddress);
void updateSocket (JNIEnv * env, hysockaddr_t sockaddrP, hysocket_t socketNew,
                   jobject socketImpl, jobject fileDescriptorSocketImpl);

/**
 * A helper method, to set the remote address into the socketImpl.
 *
 * @param	env						pointer to the JNI library
 * @param	socketImpl			pointer to the java SocketImpl object to update
 * @param	anInetAddress		pointer to the java InetAddress to update the socket with
 */

void
setSocketImplAddress (JNIEnv * env, jobject socketImpl, jobject anInetAddress)
{
  jfieldID fid = JCL_CACHE_GET (env, FID_java_net_SocketImpl_address);
  (*env)->SetObjectField (env, socketImpl, fid, anInetAddress);
}

/**
 * A helper method, to set the remote port into the socketImpl.
 *
 * @param	env					pointer to the JNI library
 * @param	socketImpl		pointer to the java SocketImpl object to update
 * @param	hPort				the port number, in host order, to update the socket with
 */

void
setSocketImplPort (JNIEnv * env, jobject socketImpl, U_16 hPort)
{
  jfieldID fid = JCL_CACHE_GET (env, FID_java_net_SocketImpl_port);
  (*env)->SetIntField (env, socketImpl, fid, hPort);
}

/**
 * A helper method, to update the java SocketImpl argument.  Used after connecting, to 'link' the 
 * system socket with the java socketImpl and update the address/port fields with the values
 * corresponding to the remote machine.
 *
 * @param	env										pointer to the JNI library
 * @param	sockaddrP							pointer to the hysockaddr struct with the remote host address & port
 * @param	socketNew							pointer to the new hysocket
 * @param	socketImpl							pointer to the new java (connected) socket
 * @param	fileDescriptorSocketImpl		pointer to the java file descriptor of the socketImpl
 */

void
updateSocket (JNIEnv * env,
              hysockaddr_t sockaddrP, hysocket_t socketNew,
              jobject socketImpl, jobject fileDescriptorSocketImpl)
{
  PORT_ACCESS_FROM_ENV (env);
  U_8 nipAddress[HYSOCK_INADDR6_LEN];
  U_32 length;
  jobject anInetAddress;
  U_16 nPort;
  U_32 scope_id = 0;

  hysock_sockaddr_address6 (sockaddrP, nipAddress, &length, &scope_id);
  nPort = hysock_sockaddr_port (sockaddrP);
  anInetAddress =
    newJavaNetInetAddressGenericB (env, nipAddress, length, scope_id);

  setJavaIoFileDescriptorContentsAsPointer (env, fileDescriptorSocketImpl,
                                            socketNew);
  setSocketImplAddress (env, socketImpl, anInetAddress);
  setSocketImplPort (env, socketImpl, hysock_ntohs (nPort));
}

/**
 * Accept a connection request on the server socket, using the provided socket.  Further communication with the
 * remote requesting host may be performed using the provided socket, referred to in the java ServerSocket
 * class comment as the (local) host socket.
 * The accept may block indefinitely if a timeout is not set (the timeout is implemented via the selectRead function).
 *
 * @param	env								pointer to the JNI library
 * @param	thisClz							pointer to the class of the receiver (of the java message)
 * @param	fileDescriptorServer			pointer to the file descriptor of the ServerSocket to accept requests on
 * @param	socketImpl						pointer to the socket to use for subsequent communications to the remote host
 * @param	fileDescriptorSocketImpl	pointer to the socketImpl file descriptor object
 * @param	timeout							the timeout value, in milliSeconds
 *
 * @return	the InetAddress, representing the local host address to which the socket is bound
 */

void JNICALL
Java_java_net_SocketImpl_acceptStreamSocketImpl (JNIEnv * env, jclass thisClz,
                                                 jobject fileDescriptorServer,
                                                 jobject socketImpl,
                                                 jobject
                                                 fileDescriptorSocketImpl,
                                                 jint timeout)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  hysocket_t socketS, socketNew;
  hysockaddr_struct sockaddrP;
  jbyte nlocalAddrBytes[HYSOCK_INADDR6_LEN];

  result = pollSelectRead (env, fileDescriptorServer, timeout, TRUE);

  if (0 > result)
    return;

  socketS =
    getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptorServer);
  if (!hysock_socketIsValid (socketS))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }

  hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nlocalAddrBytes,
                         HYSOCK_INADDR_LEN, HYADDR_FAMILY_AFINET4, 0, 0, 0,
                         socketS);

  result = hysock_accept (socketS, &sockaddrP, &socketNew);
  if (0 != result)
    {
      throwJavaNetBindException (env, result);
    }
  else
    {
      updateSocket (env, &sockaddrP, socketNew, socketImpl,
                    fileDescriptorSocketImpl);
    }
}

/**
 * Answer the number of bytes that may be read from the socket without blocking.
 * This function must not block, so the selectRead function is used with the minimum 1 uSec timeout set.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the socket to query
 *
 * @return	the number of bytes that may be read from the socket without blocking
 */

jint JNICALL
Java_java_net_SocketImpl_availableStreamImpl (JNIEnv * env, jclass thisClz,
                                              jobject fileDescriptor)
{
#define MSGLEN 2048             /* This could be replaced by the default stack buffer size */
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t hysocketP;
  char message[MSGLEN];

  I_32 result, flags = 0;

  hysocketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (hysocketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return (jint) 0;
    }

  result = hysock_select_read (hysocketP, 0, 1, FALSE);

  if (HYPORT_ERROR_SOCKET_TIMEOUT == result)
    {
      return (jint) 0;          /* The read operation timed out, so answer 0 bytes available */
    }
  else if (0 > result)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
  result = hysock_setflag (HYSOCK_MSG_PEEK, &flags);    /* Create a 'peek' flag argument for the read operation */
  if (0 > result)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }

  result = hysock_read (hysocketP, (U_8 *) message, MSGLEN, flags);

  if (0 > result)
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
 * Create a socket, of type stream.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	thisObjFD			pointer to the socket file descriptor
 * @param preferIPv4Stack if application preference is to use only IPv4 sockets (default is false)
 * @exception	SocketException	if an error occurs creating the socket
 */

void JNICALL
Java_java_net_SocketImpl_createStreamSocketImpl (JNIEnv * env, jclass thisClz,
                                                 jobject thisObjFD,
                                                 jboolean preferIPv4Stack)
{
  hysocket_t socketP;
  createSocket (env, thisObjFD, HYSOCK_STREAM, preferIPv4Stack);
  socketP =
    (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env, thisObjFD);
  setPlatformBindOptions (env, socketP);
}

/**
 * Set the state of the ServerSocket to listen for connection requests, with the specified request backlog.
 * Attempts to connect to the server beyond the backlog length will fail.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the socket file descriptor
 * @param	backlog				the maximum number of outstanding requests, before requests are rejected
 *
 * @exception	SocketException	if an error occurs connected to the remote host
 */

void JNICALL
Java_java_net_SocketImpl_listenStreamSocketImpl (JNIEnv * env, jclass thisClz,
                                                 jobject fileDescriptor,
                                                 jint backlog)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t socketP;
  I_32 result;

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }
  else
    {
      result = hysock_listen (socketP, (I_32) backlog);
      if (result < 0)
        {
          throwJavaNetSocketException (env, result);
        }
    }
}

/**
 * Receive data on this socket into the buffer, at the nominated offset, up to the maximum number of bytes.
 * If the timeout value is 0, the call may block indefinitely waiting for data otherwise
 * if no data is received within the timeout, it will return throw an exception.  The number of bytes actually
 * received is returned.
 * The caller has MUST have validated the consistency of buffer size, offset & count.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java SocketImpl
 * @param	data						pointer to the java read buffer
 * @param	offset					the offset into the read buffer, to begin writing 
 * @param	count					the maximum number of bytes to read
 * @param	timeout				the read timeout, in milliSeconds
 *
 * @return	the number of bytes read
 *					-1 indicates end-of-file (no more data available to read)
 * @exception	InterruptedIOException, SocketException	if an error occurs during the call
 */

jint JNICALL
Java_java_net_SocketImpl_receiveStreamImpl (JNIEnv * env, jclass thisClz,
                                            jobject fileDescriptor,
                                            jbyteArray data, jint offset,
                                            jint count, jint timeout)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t hysocketP;
  jbyte *message;
  I_32 result, localCount;

/* TODO: ARRAY PINNING */
#define INTERNAL_RECEIVE_BUFFER_MAX 2048
  U_8 internalBuffer[INTERNAL_RECEIVE_BUFFER_MAX];

  result = pollSelectRead (env, fileDescriptor, timeout, TRUE);
  if (0 > result)
    return (jint) 0;

  hysocketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (hysocketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return (jint) 0;
    }

  localCount = (count < 65536) ? count : 65536;

  if (localCount > INTERNAL_RECEIVE_BUFFER_MAX)
    {
      message = jclmem_allocate_memory (env, localCount);
    }
  else
    {
      message = internalBuffer;
    }

  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  result =
    hysock_read (hysocketP, (U_8 *) message, localCount, HYSOCK_NOFLAGS);
  if (result > 0)
    (*env)->SetByteArrayRegion (env, data, offset, result, message);

  if (message != (jbyte *) internalBuffer)
    {
      jclmem_free_memory (env, message);
    }
#undef INTERNAL_MAX

  /* If no bytes are read, return -1 to signal 'endOfFile' to the Java input stream */
  if (0 < result)
    {
      return (jint) result;
    }
  else if (0 == result)
    {
      return (jint) - 1;
    }
  else
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
}

/**
 * Send the data in the buffer, from the nominated offset, up to the maximum number of bytes on this socket .
 * The number of bytes actually sent is returned.
 * The caller has MUST have validated the consistency of buffer size, offset & count.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java SocketImpl
 * @param	data						pointer to the java write buffer
 * @param	offset					the offset into the write buffer, to begin sending 
 * @param	count					the maximum number of bytes to write
 *
 * @return	the number of bytes sent
 * @exception	SocketException	if an error occurs during the call
 */

jint JNICALL
Java_java_net_SocketImpl_sendStreamImpl (JNIEnv * env, jclass thisClz,
                                         jobject fileDescriptor,
                                         jbyteArray data, jint offset,
                                         jint count)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t socketP;
  jbyte *message;
  I_32 result = 0, sent = 0;

/* TODO: ARRAY PINNING */
#define INTERNAL_SEND_BUFFER_MAX 512
  U_8 internalBuffer[INTERNAL_SEND_BUFFER_MAX];

  if (count > INTERNAL_SEND_BUFFER_MAX)
    {
      message = jclmem_allocate_memory (env, count);
    }
  else
    {
      message = internalBuffer;
    }

  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  (*env)->GetByteArrayRegion (env, data, offset, count, message);
  while (sent < count)
    {
      socketP =
        getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
      if (!hysock_socketIsValid (socketP))
        {
          if (message != (jbyte *) internalBuffer)
            {
              jclmem_free_memory (env, message);
            }

          throwJavaNetSocketException (env,
                                       sent ==
                                       0 ? HYPORT_ERROR_SOCKET_BADSOCKET :
                                       HYPORT_ERROR_SOCKET_INTERRUPTED);
          return (jint) 0;
        }
      result =
        hysock_write (socketP, (U_8 *) message + sent, (I_32) count - sent,
                      HYSOCK_NOFLAGS);
      if (result < 0)
        break;
      sent += result;
    }
  if (message != (jbyte *) internalBuffer)
    {
      jclmem_free_memory (env, message);
    }
#undef INTERNAL_MAX

    /**
	 * We should always throw an exception if all the data cannot be sent because Java methods
	 * assume all the data will be sent or an error occurs.
	 */
  if (result < 0)
    {
      throwJavaNetSocketException (env, result);
      return (jint) 0;
    }
  else
    {
      return (jint) sent;
    }
}

/**
* Create a new socket, for datagrams.  The system socket is created and 'linked' to the
* the java SocketImpl by setting the file descriptor value (which is an integer 
* reference to socket maintained by the system).
*
* @param env    pointer to the JNI library
* @param thisClz   pointer to the class of the receiver (of the java message)
* @param thisObjFD pointer to the file descriptor of the java PlainDatagramSocketImpl
* @param preferIPv4Stack if application preference is to use only IPv4 sockets (default is false)
*/

void JNICALL
Java_java_net_SocketImpl_createDatagramSocketImpl (JNIEnv * env,
                                                   jclass thisClz,
                                                   jobject thisObjFD,
                                                   jboolean preferIPv4Stack)
{
  createSocket (env, thisObjFD, HYSOCK_DGRAM, preferIPv4Stack);
}

void JNICALL
Java_java_net_SocketImpl_oneTimeInitialization (JNIEnv * env, jclass clazz,
                                                jboolean jcl_supports_ipv6)
{
  jfieldID fid;

  netInitializeIDCaches (env, jcl_supports_ipv6);

  fid = (*env)->GetFieldID (env, clazz, "address", "Ljava/net/InetAddress;");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_net_SocketImpl_address, fid);

  fid = (*env)->GetFieldID (env, clazz, "port", "I");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_net_SocketImpl_port, fid);
}

/**
 * Send data on this socket to the nominated host address/port.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java PlainDatagramSocketImpl
 *
 * @exception	SocketException	if an error occurs during the call
 */

void JNICALL
Java_java_net_SocketImpl_shutdownInputImpl (JNIEnv * env, jclass thisClz,
                                            jobject fileDescriptor)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  hysocket_t socketP;

  socketP =
    (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env,
                                                           fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }
  result = hysock_shutdown_input (socketP);
  if (0 != result)
    {
      throwJavaNetSocketException (env, result);
      return;
    }
}

/**
 * Send data on this socket to the nominated host address/port.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java PlainDatagramSocketImpl
 *
 * @exception	SocketException	if an error occurs during the call
 */

void JNICALL
Java_java_net_SocketImpl_shutdownOutputImpl (JNIEnv * env, jclass thisClz,
                                             jobject fileDescriptor)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  hysocket_t socketP;

  socketP =
    (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env,
                                                           fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }
  result = hysock_shutdown_output (socketP);
  if (0 != result)
    {
      throwJavaNetSocketException (env, result);
      return;
    }
}

/**
 * Send the out of band (OOB) byte value on this socket.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java SocketImpl
 * @param	data						byte of data to send
 *
 * @exception	SocketException	if an error occurs during the call
 */

void JNICALL
Java_java_net_SocketImpl_sendUrgentDataImpl (JNIEnv * env, jclass thisClz,
                                             jobject fileDescriptor,
                                             jbyte data)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t socketP;
  I_32 flags = 0;
  I_32 result = 0;

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }
  result = hysock_setflag (HYSOCK_MSG_OOB, &flags);
  if (!result)
    {
      result = hysock_write (socketP, &data, 1, flags);
    }

  /* Always throw an exception if all the data cannot be sent because Java methods
   * assume all the data will be sent or an error occurs.
   */
  if (result < 0)
    {
      throwJavaNetSocketException (env, result);
    }
}

/**
 * Return if out of band (OOB) data is supported on this socket.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java SocketImpl
 *
 * @return true if OOB data is supported, false otherwise
 */

jboolean JNICALL
Java_java_net_SocketImpl_supportsUrgentDataImpl (JNIEnv * env, jclass thisClz,
                                                 jobject fileDescriptor)
{
  PORT_ACCESS_FROM_ENV (env);
  hysocket_t socketP;
  I_32 flags = 0;
  I_32 result;

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      return FALSE;
    }
  result = hysock_setflag (HYSOCK_MSG_OOB, &flags);
  return !result;
}
