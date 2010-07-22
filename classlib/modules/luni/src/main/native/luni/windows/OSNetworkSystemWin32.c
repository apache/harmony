/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string.h>

#include "vmi.h"
#include "helpers.h"
#include "socket.h"
#include "harmonyglob.h"
#include "OSNetworkSystem.h"
#include "nethelp.h"
#include "hysock.h"
#define NOPRIVILEGE -1
#define UNREACHABLE -2
#define REACHABLE 0

unsigned short ip_checksum(unsigned short* buffer, int size);
void set_icmp_packet(struct ICMPHeader * icmp_hdr, int packet_size);

/*
 * Class:     org_apache_harmony_luni_platform_OSNetworkSystem
 * Method:    createServerStreamSocket
 * Signature: (Ljava/io/FileDescriptor;Z)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_luni_platform_OSNetworkSystem_createServerStreamSocket
  (JNIEnv * env, jobject thiz, jobject thisObjFD, jboolean preferIPv4Stack)
{
  createSocket(env, thisObjFD, HYSOCK_STREAM, preferIPv4Stack);
}


/*
 * Class:     org_apache_harmony_luni_platform_OSNetworkSystem
 * Method:    selectImpl
 * Signature: ([Ljava/io/FileDescriptor;[Ljava/io/FileDescriptor;II[IJ)I
 */
JNIEXPORT jint JNICALL
Java_org_apache_harmony_luni_platform_OSNetworkSystem_selectImpl	
  (JNIEnv * env, jobject thiz, jobjectArray readFDArray, jobjectArray writeFDArray,
   jint	countReadC, jint countWriteC, jintArray	outFlags, jlong	timeout)
{
  PORT_ACCESS_FROM_ENV (env);
  hytimeval_struct timeP;	
  I_32 result =	0;		
  jobject gotFD;		
  hyfdset_t fdset_read,fdset_write;
  hysocket_t hysocketP;		
  jboolean isCopy ;
  jint *flagArray;
  int val;
  U_32 time_sec = (U_32)timeout/1000;
  U_32 time_msec = (U_32)(timeout%1000)*1000;

  fdset_read = hymem_allocate_memory(sizeof (hyfdset_struct));
  fdset_write = hymem_allocate_memory(sizeof (hyfdset_struct));

  FD_ZERO (&fdset_read->handle);
  FD_ZERO (&fdset_write->handle);
  for (val = 0; val<countReadC; val++){
	  gotFD	= (*env)->GetObjectArrayElement(env,readFDArray,val);
	  hysocketP = getJavaIoFileDescriptorContentsAsAPointer (env, gotFD);
      (*env)->DeleteLocalRef(env, gotFD);

	  if (!hysock_socketIsValid (hysocketP)){
      		continue;
      }
	 if (hysocketP->flags &	SOCKET_IPV4_OPEN_MASK)
	 {
		FD_SET (hysocketP->ipv4, &fdset_read->handle);
	 }
	 if (hysocketP->flags &	SOCKET_IPV6_OPEN_MASK)
	 {
		FD_SET (hysocketP->ipv6, &fdset_read->handle);
	 }
	}
  for (val = 0; val<countWriteC; val++){
	  gotFD	= (*env)->GetObjectArrayElement(env,writeFDArray,val);
	  hysocketP = getJavaIoFileDescriptorContentsAsAPointer (env, gotFD);
      (*env)->DeleteLocalRef(env, gotFD);

	  if (!hysock_socketIsValid (hysocketP)){
      		continue;
    	  }
	 if (hysocketP->flags &	SOCKET_IPV4_OPEN_MASK)
	 {
		FD_SET (hysocketP->ipv4, &fdset_write->handle);	
	 }
	 if (hysocketP->flags &	SOCKET_IPV6_OPEN_MASK)
	 {
		FD_SET (hysocketP->ipv6, &fdset_write->handle);	
	 }
	}

  /* only set when timeout >= 0 (non-block)*/
  if (0 <= timeout){      	
     hysock_timeval_init ( time_sec, time_msec, &timeP);
     result = hysock_select (0, fdset_read, fdset_write, NULL, &timeP);
  }	
  else{        
     result = hysock_select (0, fdset_read, fdset_write, NULL,NULL);
  }	
    
  if (0	< result){
	  /*output the reslut to a int array*/
	  flagArray = (*env)->GetIntArrayElements(env,outFlags,	&isCopy);
	  for (val=0;val<countReadC;val++){
		gotFD =	(*env)->GetObjectArrayElement(env,readFDArray,val);
		hysocketP = getJavaIoFileDescriptorContentsAsAPointer (env, gotFD);
        (*env)->DeleteLocalRef(env, gotFD);

		if (!hysock_socketIsValid (hysocketP)){
      			continue;
    	  	}
		if (hysocketP->flags & SOCKET_IPV4_OPEN_MASK)
		{
			if (FD_ISSET(hysocketP->ipv4,fdset_read))
				flagArray[val] = SOCKET_OP_READ;
			else
				flagArray[val] = SOCKET_OP_NONE;
		}
		else if (hysocketP->flags &	SOCKET_IPV6_OPEN_MASK)
		{
			if (FD_ISSET(hysocketP->ipv6,fdset_read))
				flagArray[val] = SOCKET_OP_READ;
			else
				flagArray[val] = SOCKET_OP_NONE;
		}
	 }
		
	  for (val=0;val<countWriteC;val++){
		gotFD =	(*env)->GetObjectArrayElement(env,writeFDArray,val);
		hysocketP = getJavaIoFileDescriptorContentsAsAPointer (env, gotFD);
        (*env)->DeleteLocalRef(env, gotFD);

		if (!hysock_socketIsValid (hysocketP)){
      			continue;
    	  	}
		if (hysocketP->flags & SOCKET_IPV4_OPEN_MASK)
		{
			if (FD_ISSET(hysocketP->ipv4,fdset_write))
				flagArray[val+countReadC] = SOCKET_OP_WRITE;
			else
				flagArray[val+countReadC] = SOCKET_OP_NONE;
		}
		else if (hysocketP->flags & SOCKET_IPV6_OPEN_MASK)
		{
			if (FD_ISSET(hysocketP->ipv6,fdset_write))
				flagArray[val+countReadC] = SOCKET_OP_WRITE;
			else
				flagArray[val+countReadC] = SOCKET_OP_NONE;
		}

		}
	(*env)->ReleaseIntArrayElements(env,outFlags, flagArray, 0); 
  }
  hymem_free_memory(fdset_read);
  hymem_free_memory(fdset_write);
  /* return both correct and error result, let java code handle	the exception*/
  return result;
}

/* Alternative Select function */
int 
selectRead (JNIEnv * env, hysocket_t hysocketP, I_32 uSecTime, BOOLEAN accept){
  PORT_ACCESS_FROM_ENV (env);
  hytimeval_struct timeP;
  hyfdset_t fdset_read;
  I_32 result = 0;

  fdset_read = hymem_allocate_memory(sizeof (hyfdset_struct));
  FD_ZERO (&fdset_read->handle);

  if (hysocketP->flags & SOCKET_IPV4_OPEN_MASK) {
    FD_SET (hysocketP->ipv4, &fdset_read->handle);
  }
  if (hysocketP->flags & SOCKET_IPV6_OPEN_MASK) {
    FD_SET (hysocketP->ipv6, &fdset_read->handle);
  }

  if (0 <= uSecTime) {
    hysock_timeval_init (0, uSecTime, &timeP);
    result = hysock_select(0, fdset_read, NULL, NULL, &timeP);
  } else {
    result = hysock_select(0, fdset_read, NULL, NULL, NULL);
  }

  hymem_free_memory(fdset_read);
  return result;
}

/*
 * Class:     org_apache_harmony_luni_platform_OSNetworkSystem
 * Method:    isReachableByICMPImpl
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL
Java_org_apache_harmony_luni_platform_OSNetworkSystem_isReachableByICMPImpl
  (JNIEnv * env, jobject clz, jobject address,jobject localaddr, jint ttl, jint timeout)
{
  PORT_ACCESS_FROM_ENV (env);
  struct sockaddr_in dest,source,local;
  struct ICMPHeader* send_buf = 0;
  struct IPHeader* recv_buf = 0;
  int size = sizeof(struct ICMPHeader);
  int result;
  struct timeval timeP;
  fd_set * fdset_read = 0;
  SOCKET sock;
  unsigned short header_len;
  int sockadd_size = sizeof (source);
  jbyte host[HYSOCK_INADDR6_LEN];
  struct ICMPHeader* icmphdr;
  struct WSAData wsaData;
  int ret = UNREACHABLE;
  U_32 length;
	      
  // start raw socket, return -1 for privilege can not be obtained
  if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
	  if (WSAStartup (MAKEWORD (1, 1), &wsaData) != 0){
	        return NOPRIVILEGE;
	  }
  }  
  sock = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
  if (INVALID_SOCKET == sock){
      return NOPRIVILEGE;
  }
  
  if(0 < ttl){
	  if (SOCKET_ERROR == setsockopt(sock, IPPROTO_IP, IP_TTL, (const char*)&ttl,
	          sizeof(ttl))) {
	      return NOPRIVILEGE;
	  }
  }

  // set address
  netGetJavaNetInetAddressValue (env, address,(U_8 *)host, &length);
  memset(&dest, 0, sizeof(dest));	  
  memcpy (&dest.sin_addr.s_addr,(U_8 *)host, length);
  dest.sin_family = AF_INET;

  if(NULL != localaddr){
    memset(&local, 0, sizeof(local));
    netGetJavaNetInetAddressValue (env, localaddr,(U_8 *)host, &length);
    memcpy (&local.sin_addr.s_addr,(U_8 *)host, length);
    bind(sock, (struct sockaddr *)& local, sizeof(local));
  }
  
  // set basic send and recv buf
  send_buf = (struct ICMPHeader*)hymem_allocate_memory(sizeof(char)*ICMP_SIZE);
  if (NULL == send_buf) {
	  goto cleanup;
  }
  recv_buf = (struct IPHeader*)hymem_allocate_memory(sizeof(char)*PACKET_SIZE);
  if (NULL == recv_buf){
	  goto cleanup;
  }
  set_icmp_packet(send_buf, ICMP_SIZE);

  if(SOCKET_ERROR == sendto(sock, (char*)send_buf, ICMP_SIZE, 0,
            (struct sockaddr*)&dest, sizeof(dest))){
	  goto cleanup;
  }
  fdset_read = (fd_set *)hymem_allocate_memory(sizeof (struct fd_set));
  if (NULL == fdset_read){
  	  goto cleanup;
  }
  FD_ZERO (fdset_read);
  FD_SET (sock, fdset_read);

  //set select timeout, change millisecond to usec
  memset (&timeP, 0, sizeof (struct timeval));    
  timeP.tv_sec = timeout/1000;
  timeP.tv_usec = (timeout%1000)*1000;
  result = select (sock+1, fdset_read, NULL, NULL,&timeP);
  if (SOCKET_ERROR == result || 0 == result){
	  goto cleanup;
  }
  result = recvfrom(sock, (char*)recv_buf,
            PACKET_SIZE, 0,
            (struct sockaddr*)&source, &sockadd_size);
  if (SOCKET_ERROR == result){
	  goto cleanup;
  }  
  // data analysis  
  header_len = recv_buf->h_len << 2;
  icmphdr = (struct ICMPHeader*)((char*)recv_buf + header_len);
  if ((result < header_len + ICMP_SIZE)||
	  (icmphdr->type != ICMP_ECHO_REPLY)||
	  (icmphdr->id != (unsigned short )GetCurrentProcessId())) {
	  goto cleanup;
  }
  ret = REACHABLE;
cleanup:
  hymem_free_memory(fdset_read);
  hymem_free_memory(send_buf);
  hymem_free_memory(recv_buf);
  WSACleanup();	 
  return ret;
}

// typical ip checksum
unsigned short ip_checksum(unsigned short * buffer, int size)
{
    register unsigned short * buf = buffer;
    register int bufleft = size;
    register unsigned long sum = 0;
    
    while (bufleft > 1) {
        sum = sum + (*buf++);
        bufleft = bufleft - sizeof(unsigned short );
    }
    if (bufleft) {
        sum = sum + (*(unsigned char*)buf);
    }
    sum = (sum >> 16) + (sum & 0xffff);
    sum += (sum >> 16);
   
    return (unsigned short )(~sum);
}

//set ICMP packet to send, head only, no data
void set_icmp_packet(struct ICMPHeader* icmp_hdr, int packet_size)
{
    icmp_hdr->type = ICMP_ECHO_REQUEST;
    icmp_hdr->code = 0;
    icmp_hdr->checksum = 0;
    icmp_hdr->id = (unsigned short )GetCurrentProcessId();
    icmp_hdr->seq = 0;

    // Calculate a checksum on the result
    icmp_hdr->checksum = ip_checksum((unsigned short *)icmp_hdr, packet_size);
}

JNIEXPORT jobject JNICALL
Java_org_apache_harmony_luni_platform_OSNetworkSystem_inheritedChannel
  (JNIEnv * env , jobject thiz)
{
  // inheritedChannel is not supported on windows platform. 
  return NULL;
}

/*
 * Utilizes the Winsock2 call to get the available bytes pending on a socket
 * which is similar to, but different to the call on other platforms.
 *
 * Class:     org_apache_harmony_luni_platform_OSNetworkSystem
 * Method:    availableStream
 * Signature: (Ljava/io/FileDescriptor;)I
 */
JNIEXPORT jint JNICALL
Java_org_apache_harmony_luni_platform_OSNetworkSystem_availableStream
  (JNIEnv * env, jobject thiz, jobject fileDescriptor)
{
  PORT_ACCESS_FROM_ENV(env);
  hysocket_t hysocketP;
  OSSOCKET socket;
  U_32 nbytes = 0;
  I_32 result;

  hysocketP = getJavaIoFileDescriptorContentsAsAPointer(env, fileDescriptor);
  if (!hysock_socketIsValid(hysocketP)) {
    throwJavaNetSocketException(env, HYPORT_ERROR_SOCKET_BADSOCKET);
    return (jint) 0;
  }

  if ((hysocketP->flags & SOCKET_USE_IPV4_MASK) == SOCKET_USE_IPV4_MASK) {
      socket = hysocketP->ipv4;
  } else {
      socket = hysocketP->ipv6;
  }

  result = ioctlsocket(socket, FIONREAD, &nbytes);
  if (result != 0) {
    throwJavaNetSocketException(env, result);
    return (jint) 0;
  }

  return (jint) nbytes;
}


/**
 * A helper method, call selectRead with a small timeout until read is ready or an error occurs.
 *
 * @param	env						pointer to the JNI library
 * @param	hysocketP				socket pointer
 * @param	timeout				timeout value
 */

I_32
pollSelectRead (JNIEnv * env, jobject fileDescriptor, jint timeout,
                BOOLEAN poll)
{

  I_32 result;
  hysocket_t hysocketP;
  PORT_ACCESS_FROM_ENV (env);

  hysocketP = getJavaIoFileDescriptorContentsAsAPointer (env, fileDescriptor);
  if (!hysock_socketIsValid (hysocketP))
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_BADSOCKET);
      return (jint) - 1;
    }

  if (0 == timeout)
    {
      result = hysock_select_read (hysocketP, 0, 0, FALSE);
    }
  else
    {
      result =
        hysock_select_read (hysocketP, timeout / 1000,
                            (timeout % 1000) * 1000, FALSE);
    }
  if (HYPORT_ERROR_SOCKET_TIMEOUT == result)
    throwJavaIoInterruptedIOException (env, result);
  else if (0 > result)
    throwJavaNetSocketException (env, result);

  return result;
}

JNIEXPORT jlong JNICALL
Java_org_apache_harmony_luni_platform_OSNetworkSystem_writev
  (JNIEnv *env, jobject thiz, jobject fd, jobjectArray buffers, jintArray offsets, jintArray counts, jint length) {

  PORT_ACCESS_FROM_ENV(env);

  jobject buffer;
  jobject* toBeReleasedBuffers;
  jint *noffset = NULL;
  jboolean isDirectBuffer = JNI_FALSE;
  jlong result = 0;
  LPWSABUF vect;
  int i;
  jint sentBytes = 0;
  jint rc;
  jclass byteBufferClass;

  hysocket_t socketP = getJavaIoFileDescriptorContentsAsAPointer(env, fd);

  if (!hysock_socketIsValid(socketP)) {
    throwJavaNetSocketException(env, HYPORT_ERROR_SOCKET_BADSOCKET);
    return (jint) 0;
  }

  vect = (LPWSABUF) hymem_allocate_memory(sizeof(WSABUF) * length);
  if (vect == NULL) {
    throwNewOutOfMemoryError(env, "");
    return 0;
  }

  toBeReleasedBuffers =
    (jobject*) hymem_allocate_memory(sizeof(jobject) * length);
  if (toBeReleasedBuffers == NULL) {
    throwNewOutOfMemoryError(env, "");
    goto free_resources;
  }
  memset(toBeReleasedBuffers, 0, sizeof(jobject)*length);

  byteBufferClass = HARMONY_CACHE_GET (env, CLS_java_nio_DirectByteBuffer);
  noffset = (*env)->GetIntArrayElements(env, offsets, NULL);
  if (noffset == NULL) {
    throwNewOutOfMemoryError(env, "");
    goto free_resources;
  }

  for (i = 0; i < length; ++i) {
    jint *cts;
    U_8* buf;
    buffer = (*env)->GetObjectArrayElement(env, buffers, i);
    isDirectBuffer = (*env)->IsInstanceOf(env, buffer, byteBufferClass);
    if (isDirectBuffer) {
      buf =
        (U_8 *)(jbyte *)(IDATA) (*env)->GetDirectBufferAddress(env, buffer);
      if (buf == NULL) {
        throwNewOutOfMemoryError(env, "Failed to get direct buffer address");
        goto free_resources;
      }
      toBeReleasedBuffers[i] = NULL;
    } else {
      buf =
        (U_8 *)(jbyte *)(IDATA) (*env)->GetByteArrayElements(env, buffer, NULL);
      if (buf == NULL) {
        throwNewOutOfMemoryError(env, "");
        goto free_resources;
      }
      toBeReleasedBuffers[i] = buffer;
    }
    vect[i].buf = buf  + noffset[i];

    cts = (*env)->GetPrimitiveArrayCritical(env, counts, NULL);
    vect[i].len = cts[i];
    (*env)->ReleasePrimitiveArrayCritical(env, counts, cts, JNI_ABORT);

  }

  if (socketP->flags & SOCKET_USE_IPV4_MASK)
    {
      result = WSASend(socketP->ipv4, vect, length, &sentBytes, HYSOCK_NOFLAGS, NULL, NULL);
    }
  else
    {
      result = WSASend(socketP->ipv6, vect, length, &sentBytes, HYSOCK_NOFLAGS, NULL, NULL);
    }

  if (SOCKET_ERROR == result) {
    rc = WSAGetLastError ();
    if (rc != WSATRY_AGAIN && rc != WSAEWOULDBLOCK) {
        throwJavaNetSocketException(env, rc);
    }
    result = 0;
  }

  
 free_resources:
  
  if (toBeReleasedBuffers != NULL) {
    for (i = 0; i < length; ++i) {
      if (toBeReleasedBuffers[i] != NULL) {
        (*env)->ReleaseByteArrayElements(env, toBeReleasedBuffers[i],
                                         vect[i].buf - noffset[i],
                                         JNI_ABORT);
      }
    }
  }

  if (noffset != NULL) {
    (*env)->ReleaseIntArrayElements(env, offsets, noffset, JNI_ABORT);
  }

  hymem_free_memory(toBeReleasedBuffers);
  hymem_free_memory(vect);

  return sentBytes;
}
