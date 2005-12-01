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
Java_java_net_PlainSocketImpl2_createStreamSocketImpl2 (JNIEnv * env,
                                                        jclass thisClz,
                                                        jobject thisObjFD,
                                                        jboolean
                                                        preferIPv4Stack)
{
  createSocket (env, thisObjFD, HYSOCK_STREAM, preferIPv4Stack);
}

/**
 * Connect the socket to the nominated remote host address/port.  The socket may then be used to send
 * and receive data from the remote host.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the socket file descriptor
 * @param	remotePort			the port on the remote host to connect to
 * @param   inetAddress		    the address to connect to
 *
 * @exception	SocketException	if an error occurs connected to the remote host
 */

void JNICALL
Java_java_net_PlainSocketImpl2_connectStreamSocketImpl2 (JNIEnv * env,
                                                         jclass thisClz,
                                                         jobject
                                                         fileDescriptor,
                                                         jint remotePort,
                                                         jint trafficClass,
                                                         jobject inetAddress)
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
  else
    {
      netGetJavaNetInetAddressValue (env, inetAddress, nAddrBytes, &length);

      nPort = hysock_htons ((U_16) remotePort);
      if (length == HYSOCK_INADDR_LEN)
        {
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, length,
                                 HYADDR_FAMILY_AFINET4, nPort, 0, scope_id,
                                 socketP);
        }
      else
        {
          netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, length,
                                 HYADDR_FAMILY_AFINET6, nPort,
                                 (trafficClass & 0xFF) << 20, scope_id,
                                 socketP);
        }

      result = hysock_connect (socketP, &sockaddrP);
      if (0 != result)
        {
          throwJavaNetConnectException (env, result);
          return;
        }
    }
}

/**
 * Connect the socket to the nominated remote host address/port.  The socket may then be used to send
 * and receive data from the remote host.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the socket file descriptor
 * @param	remotePort			the port on the remote host to connect to
 * @param   timeout				timeout in milliseconds 
 * @param   trafficClass			the traffic class to be used for the connection
 * @param   inetAddress			the address to be used for the connection
 *
 * @exception	SocketException	if an error occurs connected to the remote host
 */

void JNICALL
Java_java_net_PlainSocketImpl2_connectStreamWithTimeoutSocketImpl2 (JNIEnv *
                                                                    env,
                                                                    jclass
                                                                    thisClz,
                                                                    jobject
                                                                    fileDescriptor,
                                                                    jint
                                                                    remotePort,
                                                                    jint
                                                                    timeout,
                                                                    jint
                                                                    trafficClass,
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
  U_8 *context = NULL;
  I_32 remainingTimeout = timeout;
  I_32 passedTimeout = 0;
  UDATA finishTime = 0;
  BOOLEAN hasTimeout = timeout > 0;
  U_32 scope_id = 0;

  /* if a timeout was specified calculate the finish time value */
  if (hasTimeout)
    {
      finishTime = hytime_msec_clock () + (UDATA) timeout;
    }

  socketP = getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (socketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return;
    }
  else
    {
      netGetJavaNetInetAddressValue (env, inetAddress, nAddrBytes, &length);
      nPort = hysock_htons ((U_16) remotePort);
      if (length == HYSOCK_INADDR_LEN)
        {
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, length,
                                 HYADDR_FAMILY_AFINET4, nPort, 0, scope_id,
                                 socketP);
        }
      else
        {
          netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nAddrBytes, length,
                                 HYADDR_FAMILY_AFINET6, nPort,
                                 (trafficClass & 0xFF) << 20, scope_id,
                                 socketP);
        }

      result =
        hysock_connect_with_timeout (socketP, &sockaddrP, 0,
                                     HY_PORT_SOCKET_STEP_START, &context);
      if (0 == result)
        {
          /* ok we connected right away so we are done */
          hysock_connect_with_timeout (socketP, &sockaddrP, 0,
                                       HY_PORT_SOCKET_STEP_DONE, &context);
          return;
        }
      else if (result != HYPORT_ERROR_SOCKET_NOTCONNECTED)
        {
          /* we got an error other than NOTCONNECTED so we cannot continue */
          if ((HYPORT_ERROR_SOCKET_CONNRESET == result) ||
              (HYPORT_ERROR_SOCKET_CONNECTION_REFUSED == result) ||
              (HYPORT_ERROR_SOCKET_ADDRNOTAVAIL == result) ||
              (HYPORT_ERROR_SOCKET_ADDRINUSE == result) ||
              (HYPORT_ERROR_SOCKET_ENETUNREACH == result) ||
              (HYPORT_ERROR_SOCKET_EACCES == result))
            {
              hysock_connect_with_timeout (socketP, &sockaddrP,
                                           remainingTimeout,
                                           HY_PORT_SOCKET_STEP_DONE,
                                           &context);
              throwJavaNetConnectException (env, result);
              return;
            }
          else
            {
              hysock_connect_with_timeout (socketP, &sockaddrP, 0,
                                           HY_PORT_SOCKET_STEP_DONE,
                                           &context);
              throwJavaNetSocketException (env, result);
              return;
            }
        }

      while (HYPORT_ERROR_SOCKET_NOTCONNECTED == result)
        {

          passedTimeout = remainingTimeout;

          /**
			* ok now try and connect.  Depending on the platform this may sleep for 
            * up to passedTimeout milliseconds 
            */
          result =
            hysock_connect_with_timeout (socketP, &sockaddrP, passedTimeout,
                                         HY_PORT_SOCKET_STEP_CHECK, &context);

          /**
			* now check if the socket is still connected.  
            * Do it here as some platforms seem to think they are connected if the socket 
            * is closed on them 
            */
          socketP =
            getJavaIoFileDescriptorContentsAsPointer (env, fileDescriptor);
          if (!hysock_socketIsValid (socketP))
            {
              hysock_connect_with_timeout (socketP, &sockaddrP, 0,
                                           HY_PORT_SOCKET_STEP_DONE,
                                           &context);
              throwJavaNetSocketException (env,
                                           HYPORT_ERROR_SOCKET_BADSOCKET);
              return;
            }

          /* check if we are now connected, if so we can finish the process and return */
          if (0 == result)
            {
              hysock_connect_with_timeout (socketP, &sockaddrP, 0,
                                           HY_PORT_SOCKET_STEP_DONE,
                                           &context);
              return;
            }

         /**
           * if the error is HYPORT_ERROR_SOCKET_NOTCONNECTED then we have not yet connected 
           * and we may not be done yet 
           */
          if (HYPORT_ERROR_SOCKET_NOTCONNECTED == result)
            {
              /* check if the timeout has expired */
              if (hasTimeout)
                {
                  remainingTimeout = finishTime - hytime_msec_clock ();
                  if (remainingTimeout <= 0)
                    {
                      hysock_connect_with_timeout (socketP, &sockaddrP, 0,
                                                   HY_PORT_SOCKET_STEP_DONE,
                                                   &context);
                      throwJavaNetSocketTimeoutException (env, result);
                      return;
                    }
                }
              else
                {
                  remainingTimeout = 100;
                }

            }
          else
            {
              if ((HYPORT_ERROR_SOCKET_CONNRESET == result) ||
                  (HYPORT_ERROR_SOCKET_CONNECTION_REFUSED == result) ||
                  (HYPORT_ERROR_SOCKET_ADDRNOTAVAIL == result) ||
                  (HYPORT_ERROR_SOCKET_ADDRINUSE == result) ||
                  (HYPORT_ERROR_SOCKET_ENETUNREACH == result) ||
                  (HYPORT_ERROR_SOCKET_EACCES == result))
                {
                  hysock_connect_with_timeout (socketP, &sockaddrP,
                                               remainingTimeout,
                                               HY_PORT_SOCKET_STEP_DONE,
                                               &context);
                  throwJavaNetConnectException (env, result);
                  return;
                }
              else
                {
                  hysock_connect_with_timeout (socketP, &sockaddrP,
                                               remainingTimeout,
                                               HY_PORT_SOCKET_STEP_DONE,
                                               &context);
                  throwJavaNetSocketException (env, result);
                  return;
                }
            }
        }
    }
}

/**
 * Send data on this socket to the nominated host address/port.
 *
 * @param	env						pointer to the JNI library
 * @param	thisClz					pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor		pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param	data						pointer to the java read buffer
 * @param	msgLength			the length of the read buffer
 * @param	targetPort				target port, in host order
 * @param  inetAddress			the address to send the datagram to
 *
 * @return	the number of bytes sent
 * @exception	SocketException	if an error occurs during the call
 */

jint JNICALL
Java_java_net_PlainSocketImpl2_sendDatagramImpl2 (JNIEnv * env,
                                                  jclass thisClz,
                                                  jobject fileDescriptor,
                                                  jbyteArray data,
                                                  jint offset, jint msgLength,
                                                  jint targetPort,
                                                  jobject inetAddress)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte *message;
  jbyte nhostAddrBytes[HYSOCK_INADDR6_LEN];
  U_16 nPort;
  I_32 result = 0, sent = 0;
  hysocket_t socketP;
  hysockaddr_struct sockaddrP;
  int length;
  U_32 scope_id = 0;

  if (inetAddress != NULL)
    {
      netGetJavaNetInetAddressValue (env, inetAddress, nhostAddrBytes,
                                     &length);

      socketP =
        (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env,
                                                               fileDescriptor);
      nPort = hysock_htons ((U_16) targetPort);
      if (length == HYSOCK_INADDR_LEN)
        {
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nhostAddrBytes, length,
                                 HYPROTOCOL_FAMILY_INET4, nPort, 0, scope_id,
                                 socketP);
        }
      else
        {
          netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
          hysock_sockaddr_init6 (&sockaddrP, (U_8 *) nhostAddrBytes, length,
                                 HYPROTOCOL_FAMILY_INET6, nPort, 0, scope_id,
                                 socketP);
        }
    }

  message = jclmem_allocate_memory (env, msgLength);
  if (message == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  (*env)->GetByteArrayRegion (env, data, offset, msgLength, message);
  while (sent < msgLength)
    {
      socketP =
        (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env,
                                                               fileDescriptor);
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
        hysock_writeto (socketP, (U_8 *) message + sent,
                        (I_32) msgLength - sent, HYSOCK_NOFLAGS, &sockaddrP);
      if (result < 0)
        break;
      sent += result;
    }
  jclmem_free_memory (env, message);
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
 * Bind the socket to the specified local address/port.  This call is made after socket creation
 * and prior to read/write operations.
 *
 * @param	env					pointer to the JNI library
 * @param	thisClz				pointer to the class of the receiver (of the java message)
 * @param	fileDescriptor 	pointer to the file descriptor of the socket to bind
 * @param	localPort			the port, in host order, to bind the socket on
 * @param   inetAddress    address to be used for the bind
 *
 * @exception SocketException	if an error occurs during the call
 */

void JNICALL
Java_java_net_PlainSocketImpl2_socketBindImpl2 (JNIEnv * env, jclass thisClz,
                                                jobject fileDescriptor,
                                                jint localPort,
                                                jobject inetAddress)
{
  PORT_ACCESS_FROM_ENV (env);
  jbyte nlocalAddrBytes[HYSOCK_INADDR6_LEN];
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
  else
    {
      netGetJavaNetInetAddressValue (env, inetAddress, nlocalAddrBytes,
                                     &length);

      nPort = hysock_htons ((U_16) localPort);
      if (length == HYSOCK_INADDR6_LEN)
        {
          netGetJavaNetInetAddressScopeId (env, inetAddress, &scope_id);
          hysock_sockaddr_init6 (&sockaddrP, nlocalAddrBytes, length,
                                 HYADDR_FAMILY_AFINET6, nPort, 0, scope_id,
                                 socketP);
        }
      else
        {
          hysock_sockaddr_init6 (&sockaddrP, nlocalAddrBytes, length,
                                 HYADDR_FAMILY_AFINET4, nPort, 0, scope_id,
                                 socketP);
        }
      result = hysock_bind (socketP, &sockaddrP);
      if (0 != result)
        {
          throwJavaNetBindException (env, result);
          return;
        }
    }
}
