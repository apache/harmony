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

#if !defined(hysock_h)
#define hysock_h

/******************************************************\
       Portable socket library implementation.
\******************************************************/
/* windows.h defined UDATA.  Ignore its definition */
#define UDATA UDATA_win32_
#include <windows.h>
#undef UDATA			/* this is safe because our UDATA is a typedef, not a macro */

/* Undefine the winsockapi because winsock2 defines it.  Removes warnings. */
#if defined(_WINSOCKAPI_) && !defined(_WINSOCK2API_)
#undef _WINSOCKAPI_
#endif
#include <winsock2.h>
#include <ws2tcpip.h>

#include "hysocket.h"
#include "hycomp.h"
#include "hyport.h"
#include "hyportpg.h"

#if defined(DEBUG)
#define HYSOCKDEBUG(x, error) printf(x, error)
#define HYSOCKDEBUGPRINT(x) printf(x)
#else
#define HYSOCKDEBUG(x, error)
#define HYSOCKDEBUGPRINT(x)
#endif

extern HY_CFUNC char *VMCALL os_error_message
PROTOTYPE ((struct HyPortLibrary * portLibrary, I_32 errorNum));

/* os types */
typedef SOCKET OSSOCKET;	/* as returned by socket() */
typedef SOCKADDR OSADDR;
typedef SOCKADDR_IN OSSOCKADDR;	/* as used by bind() and friends */
typedef struct hostent OSHOSTENT;
typedef fd_set OSFDSET;
typedef struct timeval OSTIMEVAL;
typedef struct linger OSLINGER;
typedef struct ip_mreq OSIPMREQ;

#define OSSOMAXCONN SOMAXCONN
#define OS_BADSOCKET INVALID_SOCKET

typedef SOCKADDR_IN6 OSSOCKADDR_IN6;	/* IPv6 */
typedef struct ipv6_mreq OSIPMREQ6;
typedef struct addrinfo OSADDRINFO;	/* IPv6 */
typedef struct sockaddr_storage OSSOCKADDR_STORAGE;	/* IPv6 */
/* constant for identifying the pseudo looback interface */
#define pseudoLoopbackGUID  "{6BD113CC-5EC2-7638-B953-0B889DA72014}"

/* defines for socket levels */
#define OS_SOL_SOCKET SOL_SOCKET
#define OS_IPPROTO_TCP IPPROTO_TCP
#define OS_IPPROTO_IP IPPROTO_IP
#define OS_IPPROTO_IPV6 IPPROTO_IPV6

/* defines for socket options */
#define OS_SO_LINGER SO_LINGER
#define OS_SO_KEEPALIVE SO_KEEPALIVE
#define OS_TCP_NODELAY TCP_NODELAY
#define OS_SO_REUSEADDR SO_REUSEADDR
#define OS_SO_SNDBUF SO_SNDBUF
#define OS_SO_RCVBUF SO_RCVBUF
#define OS_SO_BROADCAST SO_BROADCAST
#define OS_SO_OOBINLINE SO_OOBINLINE
#define OS_IP_TOS IP_TOS

/* defines added for IPv6 */
#define OS_AF_INET4 AF_INET
#define OS_AF_UNSPEC AF_UNSPEC
#define OS_PF_UNSPEC PF_UNSPEC
#define OS_PF_INET4 PF_INET
#define OS_AF_INET6 AF_INET6
#define OS_PF_INET6 PF_INET6
#define OS_INET4_ADDRESS_LENGTH INET_ADDRSTRLEN
#define OS_INET6_ADDRESS_LENGTH INET6_ADDRSTRLEN
#define OSNIMAXHOST NI_MAXHOST
#define OSNIMAXSERV NI_MAXSERV

/* defines for socket options, multicast */
#define OS_MCAST_TTL IP_MULTICAST_TTL
#define OS_MCAST_ADD_MEMBERSHIP IP_ADD_MEMBERSHIP
#define OS_MCAST_DROP_MEMBERSHIP IP_DROP_MEMBERSHIP
#define OS_MCAST_INTERFACE IP_MULTICAST_IF
#define OS_MCAST_INTERFACE_2 IPV6_MULTICAST_IF
#define OS_IPV6_ADD_MEMBERSHIP IPV6_ADD_MEMBERSHIP
#define OS_IPV6_DROP_MEMBERSHIP IPV6_DROP_MEMBERSHIP
#define OS_MCAST_LOOP IP_MULTICAST_LOOP

/* platform constants */
#define HYSOCK_MAXCONN OSSOMAXCONN
#define HYSOCK_BADSOCKET OS_BADSOCKET

/*
 * Socket Types
 */
#define OSSOCK_ANY        0	/* for getaddrinfo hints */
#define OSSOCK_STREAM     SOCK_STREAM	/* stream socket */
#define OSSOCK_DGRAM      SOCK_DGRAM	/* datagram socket */
#define OSSOCK_RAW        SOCK_RAW	/* raw-protocol interface */
#define OSSOCK_RDM        SOCK_RDM	/* reliably-delivered message */
#define OSSOCK_SEQPACKET  SOCK_SEQPACKET	/* sequenced packet stream */

/* socket structure flags */
#define SOCKET_IPV4_OPEN_MASK '\x1'	/* 00000001 */
#define SOCKET_IPV6_OPEN_MASK '\x2'	/* 00000010 */
#define SOCKET_BOTH_OPEN_MASK '\x3'	/* 00000011 */
#define SOCKET_USE_IPV4_MASK '\x4'	/* 00000100 - this tells which one to pick when doing an operation */

/* The sockets returned as a hysocket_t (hysocket_struct*) are not actual structs, we just
 * pretend that the pointer is a struct and never dereference it.
 */
#if defined(NO_LVALUE_CASTING)
#define SOCKET_CAST(x) (*((OSSOCKET *) &(x)))
#else
#define SOCKET_CAST(x) ((OSSOCKET)x)
#endif
typedef struct hysockaddr_struct
{
  OSSOCKADDR_STORAGE addr;
} hysockaddr_struct;

/*
* Socket structure on windows requires 2 sockets due to the fact that microsoft does not
* handle ipv6-mapped ipv6 addresses.  Therefore we need to listen to sockets on both
* the ipv6 and ipv4 stacks, when in a mode where we support ipv6.
*/
typedef struct hysocket_struct
{
  OSSOCKET ipv4;
  OSSOCKET ipv6;
  U_8 flags;
} hysocket_struct;

typedef struct hyhostent_struct
{
  OSHOSTENT *entity;
} hyhostent_struct;

typedef struct hyfdset_struct
{
  OSFDSET handle;
} hyfdset_struct;

typedef struct hytimeval_struct
{
  OSTIMEVAL time;
} hytimeval_struct;

typedef struct hylinger_struct
{
  OSLINGER linger;
} hylinger_struct;

typedef struct hyipmreq_struct
{
  OSIPMREQ addrpair;
} hyipmreq_struct;

typedef struct hyipv6_mreq_struct
{
  OSIPMREQ6 mreq;
} hyipv6_mreq_struct;

/** structure for IPv6 addrinfo will either point to a hostent or 
	an addr info depending on the IPv6 support for this OS */
typedef struct hyaddrinfo_struct
{
  void *addr_info;
  int length;
} hyaddrinfo_struct;

/* structure for returning either and IPV4 or IPV6 ip address */
typedef struct hyipAddress_struct
{
  union
  {
    U_8 bytes[sizeof (struct in6_addr)];
    struct in_addr inAddr;
    struct in6_addr in6Addr;
  } addr;
  U_32 length;
  U_32 scope;
} hyipAddress_struct;

/* structure for returning network interface information */
typedef struct hyNetworkInterface_struct
{
  char *name;
  char *displayName;
  U_32 numberAddresses;
  U_32 index;
  struct hyipAddress_struct *addresses;
} hyNetworkInterface_struct;

/* array of network interface structures */
typedef struct hyNetworkInterfaceArray_struct
{
  U_32 length;
  struct hyNetworkInterface_struct *elements;
} hyNetworkInterfaceArray_struct;

#endif /* hysock_h */
