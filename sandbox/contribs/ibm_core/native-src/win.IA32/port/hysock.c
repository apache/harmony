/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
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

#define CDEV_CURRENT_FUNCTION _comment_
/**
 * @file
 * @ingroup Port
 * @brief Sockets
 */
#undef CDEV_CURRENT_FUNCTION

#include "hysock.h"
#include "portpriv.h"
#include "hyportptb.h"
#include <Iphlpapi.h>

typedef DWORD (WINAPI * GetAdaptersAddressesFunctionAddress) (ULONG, DWORD,
							      PVOID,
							      PIP_ADAPTER_ADDRESSES,
							      PULONG);

#include <limits.h>

#define VALIDATE_ALLOCATIONS 1

#define LOOP_BACK_NAME            "loopback"
#define LOOP_BACK_DISPLAY_NAME    "loopback interface"
#define LOOP_BACK_IPV4_ADDRESS    "127.0.0.1"
#define LOOP_BACK_NUM_ADDRESSES   1

typedef struct selectFDSet_struct
{
  int nfds;
  OSSOCKET sock;
  fd_set writeSet;
  fd_set readSet;
  fd_set exceptionSet;
} selectFDSet_strut;

#define CDEV_CURRENT_FUNCTION _prototypes_private

I_32 platformSocketOption (I_32 portableSocketOption);

static void VMCALL initializeSocketStructure (hysocket_t sockHandle,
					      OSSOCKET sock,
					      BOOL useIPv4Socket);

void VMCALL updateSocketState (hysocket_t socket, BOOL useIPv4Socket);

I_32 platformSocketLevel (I_32 portableSocketLevel);

static I_32 ensureConnected (void);

static I_32 findError (I_32 errorCode);

I_32 map_protocol_family_Hy_to_OS (I_32 addr_family);

BOOL VMCALL isAnyAddress (hysockaddr_t addr);

I_32 map_addr_family_Hy_to_OS (I_32 addr_family);

I_32 map_sockettype_Hy_to_OS (I_32 socket_type);

int VMCALL internalCloseSocket (struct HyPortLibrary *portLibrary,
				hysocket_t sockHandle, BOOL closeIPv4Socket);

BOOL VMCALL useIPv4Socket (hysocket_t sockHandle);

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION ensureConnected
static I_32
ensureConnected (void)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION findError
/**
 * @internal
 * Determines the proper portable error code to return given a native error code
 *
 * @param[in] errorCode The error code reported by the OS
 *
 * @return	the (negative) portable error code
 */
static I_32
findError (I_32 errorCode)
{
  switch (errorCode)
    {
    case WSANOTINITIALISED:
      return HYPORT_ERROR_SOCKET_NOTINITIALIZED;
    case WSAENOPROTOOPT:
      return HYPORT_ERROR_SOCKET_OPTUNSUPP;
    case WSAEINTR:
      return HYPORT_ERROR_SOCKET_INTERRUPTED;
    case WSAENOTCONN:
      return HYPORT_ERROR_SOCKET_NOTCONNECTED;
    case WSAEWOULDBLOCK:
      return HYPORT_ERROR_SOCKET_WOULDBLOCK;
    case WSAECONNABORTED:
      return HYPORT_ERROR_SOCKET_TIMEOUT;
    case WSAECONNRESET:
      return HYPORT_ERROR_SOCKET_CONNRESET;
    case WSAENOBUFS:
      return HYPORT_ERROR_SOCKET_NOBUFFERS;
    case WSAEADDRINUSE:
      return HYPORT_ERROR_SOCKET_ADDRINUSE;
    case WSANO_DATA:
      return HYPORT_ERROR_SOCKET_NODATA;
    case WSAEOPNOTSUPP:
      return HYPORT_ERROR_SOCKET_OPNOTSUPP;
    case WSAEISCONN:
      return HYPORT_ERROR_SOCKET_ISCONNECTED;
    case WSAHOST_NOT_FOUND:
      return HYPORT_ERROR_SOCKET_HOSTNOTFOUND;
    case WSAEADDRNOTAVAIL:
      return HYPORT_ERROR_SOCKET_ADDRNOTAVAIL;
    case WSAEFAULT:
      return HYPORT_ERROR_SOCKET_OPTARGSINVALID;
    case WSAENOTSOCK:
      return HYPORT_ERROR_SOCKET_NOTSOCK;
    case WSAECONNREFUSED:
      return HYPORT_ERROR_SOCKET_CONNECTION_REFUSED;
    case WSAEACCES:
      return HYPORT_ERROR_SOCKET_EACCES;
    default:
      return HYPORT_ERROR_SOCKET_OPFAILED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION internalCloseSocket
/**
 * @internal updates the socketHandle by putting the socket in either the ipv4 or ipv6 slot
 */
int VMCALL
internalCloseSocket (struct HyPortLibrary *portLibrary, hysocket_t sockHandle,
		     BOOL closeIPv4Socket)
{
  U_8 mask1 = SOCKET_USE_IPV4_MASK;
  int rc = 0;

  if (closeIPv4Socket)
    {
      if (sockHandle->flags & SOCKET_IPV4_OPEN_MASK)
	{
	  /* Don't bother to check the error -- not like we can do anything about it. */
	  shutdown (sockHandle->ipv4, 1);

	  if (closesocket (sockHandle->ipv4) == SOCKET_ERROR)
	    {
	      rc = WSAGetLastError ();
	      HYSOCKDEBUG ("<closesocket failed, err=%d>\n", rc);
	      switch (rc)
		{
		case WSAEWOULDBLOCK:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       HYPORT_ERROR_SOCKET_NBWITHLINGER);
		  break;
		default:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       findError (rc));
		}
	    }
	  sockHandle->ipv4 = INVALID_SOCKET;
	}
    }
  else
    {
      if (sockHandle->flags & SOCKET_IPV6_OPEN_MASK)
	{
	  /* Don't bother to check the error -- not like we can do anything about it. */
	  shutdown (sockHandle->ipv6, 1);

	  if (closesocket (SOCKET_CAST (sockHandle->ipv6)) == SOCKET_ERROR)
	    {
	      rc = WSAGetLastError ();
	      HYSOCKDEBUG ("<closesocket failed, err=%d>\n", rc);
	      switch (rc)
		{
		case WSAEWOULDBLOCK:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       HYPORT_ERROR_SOCKET_NBWITHLINGER);
		  break;
		default:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       findError (rc));
		}
	    }
	  sockHandle->ipv6 = INVALID_SOCKET;
	}
    }

  updateSocketState (sockHandle, !closeIPv4Socket);
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION isAnyAddress
/**
 * @internal Returns TRUE if the address is 0.0.0.0 or ::0, FALSE otherwise
 */
BOOL VMCALL
isAnyAddress (hysockaddr_t addr)
{
  U_32 length = 0;
  U_8 address[16];
  BOOL truth = TRUE;
  U_32 i;
  U_32 scope_id;

  /* get the address */
  hysock_sockaddr_address6 (NULL, addr, address, &length, &scope_id);

  for (i = 0; i < length; i++)
    {
      if (address[i] != 0)
	{
	  truth = FALSE;
	  break;
	}
    }

  return truth;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION map_addr_family_Hy_to_OS
/**
 * @internal Map the portable address family to the platform address family. 
 *
 * @param[in] addr_family The portable address family to convert
 *
 * @return	the platform family, or OS_AF_UNSPEC if none exists
 */
I_32
map_addr_family_Hy_to_OS (I_32 addr_family)
{
  switch (addr_family)
    {
    case HYADDR_FAMILY_AFINET4:
      return OS_AF_INET4;
    case HYADDR_FAMILY_AFINET6:
      return OS_AF_INET6;
    }
  return OS_AF_UNSPEC;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION map_protocol_family_Hy_to_OS
/**
 * @internal Map the portable address protocol to the platform protocol
 *
 * @param[in] addr_protocol The portable address protocol to convert
 *
 * @return	the platform family, or OS_PF_UNSPEC if none exists
 */
I_32
map_protocol_family_Hy_to_OS (I_32 addr_family)
{
  switch (addr_family)
    {
    case HYPROTOCOL_FAMILY_INET4:
      return OS_PF_INET4;
    case HYPROTOCOL_FAMILY_INET6:
      return OS_PF_INET6;
    }
  return OS_PF_UNSPEC;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION map_sockettype_Hy_to_OS
/**
 * @internal Map the portable socket type to the platform type. 
 *
 * @param[in] addr_family The portable socket type to convert
 *
 * @return	the platform family, or OSSOCK_ANY if none exists
 */
I_32
map_sockettype_Hy_to_OS (I_32 socket_type)
{
  switch (socket_type)
    {
    case HYSOCKET_STREAM:
      return OSSOCK_STREAM;
    case HYSOCKET_DGRAM:
      return OSSOCK_DGRAM;
    case HYSOCKET_RAW:
      return OSSOCK_RAW;
    case HYSOCKET_RDM:
      return OSSOCK_RDM;
    case HYSOCKET_SEQPACKET:
      return OSSOCK_SEQPACKET;
    }
  return OSSOCK_ANY;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION platformSocketLevel
/**
 * @internal Map the portable to the platform socket level. 
 * Used to resolve the arguments of socket option functions.
 * Levels currently in use are:
 * \arg SOL_SOCKET, for most options
 * \arg IPPROTO_TCP, for the TCP noDelay option
 * \arg IPPROTO_IP, for the option operations associated with multicast (join, drop, interface)
 *
 * @param[in] portableSocketLevel The portable socket level to convert.
 *
 * @return the platform socket level or a (negative) error code if no equivalent level exists.
 */
I_32
platformSocketLevel (I_32 portableSocketLevel)
{
  switch (portableSocketLevel)
    {
    case HY_SOL_SOCKET:
      return OS_SOL_SOCKET;
    case HY_IPPROTO_TCP:
      return OS_IPPROTO_TCP;
    case HY_IPPROTO_IP:
      return OS_IPPROTO_IP;
    case HY_IPPROTO_IPV6:
      return OS_IPPROTO_IPV6;
    }
  return HYPORT_ERROR_SOCKET_SOCKLEVELINVALID;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION platformSocketOption
/**
 * @internal
 * Map the portable to the platform socket options.  Used to resolve the arguments of socket option functions.
 * Options currently in supported are:
 * \arg SOL_LINGER, the linger timeout
 * \arg TCP_NODELAY, the buffering scheme implementing Nagle's algorithm
 * \arg IP_MULTICAST_TTL, the packet Time-To-Live
 * \arg IP_ADD_MEMBERSHIP, to join a multicast group
 * \arg  IP_DROP_MEMBERSHIP, to leave a multicast group
 * \arg IP_MULTICAST_IF, the multicast interface
 *
 * @param[in] portableSocketOption The portable socket option to convert.
 *
 * @return	the platform socket option or a (negative) error code if no equivalent option exists.
 */
I_32
platformSocketOption (I_32 portableSocketOption)
{
  switch (portableSocketOption)
    {
    case HY_SO_LINGER:
      return OS_SO_LINGER;
    case HY_SO_KEEPALIVE:
      return OS_SO_KEEPALIVE;
    case HY_TCP_NODELAY:
      return OS_TCP_NODELAY;
    case HY_MCAST_TTL:
      return OS_MCAST_TTL;
    case HY_MCAST_ADD_MEMBERSHIP:
      return OS_MCAST_ADD_MEMBERSHIP;
    case HY_MCAST_DROP_MEMBERSHIP:
      return OS_MCAST_DROP_MEMBERSHIP;
    case HY_MCAST_INTERFACE:
      return OS_MCAST_INTERFACE;
    case HY_MCAST_INTERFACE_2:
      return OS_MCAST_INTERFACE_2;
    case HY_IPV6_ADD_MEMBERSHIP:
      return OS_IPV6_ADD_MEMBERSHIP;
    case HY_IPV6_DROP_MEMBERSHIP:
      return OS_IPV6_DROP_MEMBERSHIP;
    case HY_SO_REUSEADDR:
      return OS_SO_REUSEADDR;
    case HY_SO_SNDBUF:
      return OS_SO_SNDBUF;
    case HY_SO_RCVBUF:
      return OS_SO_RCVBUF;
    case HY_SO_BROADCAST:
      return OS_SO_BROADCAST;
    case HY_SO_OOBINLINE:
      return OS_SO_OOBINLINE;
    case HY_IP_MULTICAST_LOOP:
      return OS_MCAST_LOOP;
    case HY_IP_TOS:
      return OS_IP_TOS;
    }
  return HYPORT_ERROR_SOCKET_OPTUNSUPP;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION updateSocketState

/**
 * @internal updates the socketHandle by putting the socket in either the ipv4 or ipv6 slot
 */
void VMCALL
updateSocketState (hysocket_t socket, BOOL useIPv4Socket)
{

  U_8 mask1 = SOCKET_USE_IPV4_MASK;
  U_8 mask2 = SOCKET_IPV4_OPEN_MASK;
  U_8 mask3 = SOCKET_IPV6_OPEN_MASK;

  if (useIPv4Socket)
    {
      /* Set the flags to "use IPv4" and "IPv6 not open"  */
      socket->flags = (socket->flags | mask1) & ~mask3;
    }
  else
    {
      /* Set the flags to "use IPv6" and "IPv4 not open"  */
      socket->flags = (socket->flags | mask3) & ~(mask1 | mask2);
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION initializeSocketStructure
/**
 * @internal initializes the socketHandle by putting the socket in either the ipv4 or ipv6 slot
 */
static void VMCALL
initializeSocketStructure (hysocket_t sockHandle, OSSOCKET sock,
			   BOOL useIPv4Socket)
{
  if (useIPv4Socket)
    {
      sockHandle->ipv4 = sock;
      sockHandle->ipv6 = INVALID_SOCKET;
      sockHandle->flags = SOCKET_IPV4_OPEN_MASK;
    }
  else
    {
      sockHandle->ipv4 = INVALID_SOCKET;
      sockHandle->ipv6 = sock;
      sockHandle->flags = SOCKET_IPV6_OPEN_MASK;
    }

  updateSocketState (sockHandle, useIPv4Socket);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION useIPv4Socket
/**
 * @internal updates the socketHandle by putting the socket in either the ipv4 or ipv6 slot
 */
BOOL VMCALL
useIPv4Socket (hysocket_t sockHandle)
{
  return (sockHandle->flags & SOCKET_USE_IPV4_MASK) == SOCKET_USE_IPV4_MASK;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_accept
/**
 * The accept function extracts the first connection on the queue of pending connections 
 * on socket sock. It then creates a new socket and returns a handle to the new socket. 
 * The newly created socket is the socket that will handle the actual the connection and 
 * has the same properties as socket sock.  
 *
 * The accept function can block the caller until a connection is present if no pending 
 * connections are present on the queue.
 *
 * @param[in] portLibrary The port library.
 * @param[in] serverSock  A hysocket_t  from which data will be read.
 * @param[in] addrHandle An optional pointer to a buffer that receives the address of the connecting 
 * entity, as known to the communications layer. The exact format of the addr parameter 
 * is determined by the address family established when the socket was created. 
 * @param[in] sockHandle A pointer to a hysocket_t  which will point to the newly created 
 * socket once accept returns succesfully
 *
 * @return 
 * \arg 0 on success
 * \arg HYPORT_ERROR_SOCKET_BADSOCKET, on generic error
 * \arg HYPORT_ERROR_SOCKET_NOTINITIALIZED, if socket library uninitialized
 * \arg HYPORT_ERROR_SOCKET_INTERRUPTED, the call was cancelled
 * \arg HYPORT_ERROR_SOCKET_ADDRNOTAVAIL, the addr parameter is not valid
 * \arg HYPORT_ERROR_SOCKET_SYSTEMBUSY, if system busy handling other requests
 * \arg HYPORT_ERROR_SOCKET_SYSTEMFULL, is too many sockets are active
 * \arg HYPORT_ERROR_SOCKET_WOULDBLOCK, the socket is marked as nonblocking and no connections are present to be accepted., 
 */
/* IPv6 - In the case of IPv6 if the address is the any address, a socket will be created on 
 * both the IPv4 and IPv6 stack.  The Windows IPv6 stack does not implement the full
 * specification and a socket must be created to listen on both stacks for incoming calls.
 */
I_32 VMCALL
hysock_accept (struct HyPortLibrary * portLibrary, hysocket_t serverSock,
	       hysockaddr_t addrHandle, hysocket_t * sockHandle)
{
  I_32 rc = 0;
  hysocket_t sock = 0;
  SOCKET _sc;
  I_32 addrlen = sizeof (addrHandle->addr);

  if (useIPv4Socket (serverSock))
    {
      ((OSSOCKADDR *) & addrHandle->addr)->sin_family = OS_AF_INET4;
      _sc =
	accept (serverSock->ipv4, (struct sockaddr *) &addrHandle->addr,
		&addrlen);
    }
  else
    {
      ((OSSOCKADDR *) & addrHandle->addr)->sin_family = OS_AF_INET6;
      _sc =
	accept (serverSock->ipv6, (struct sockaddr *) &addrHandle->addr,
		&addrlen);
    }

  if (_sc == INVALID_SOCKET)
    {
      *sockHandle = (hysocket_t)INVALID_SOCKET;
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<accept failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTLISTENING);
	case WSAEOPNOTSUPP:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTSTREAMSOCK);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  else
    {
      *sockHandle =
	portLibrary->mem_allocate_memory (portLibrary,
					  sizeof (struct hysocket_struct));
      initializeSocketStructure (*sockHandle, _sc,
				 useIPv4Socket (serverSock));
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_bind
/**
 * The bind function is used on an unconnected socket before subsequent 
 * calls to the connect or listen functions. When a socket is created with a 
 * call to the socket function, it exists in a name space (address family), but 
 * it has no name assigned to it. Use bind to establish the local association 
 * of the socket by assigning a local name to an unnamed socket.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock hysocket_t which will be be associated with the specified name.
 * @param[in] addr Address to bind to socket.
 *
 * @return
 * \arg 0, on success
 * \arg HYPORT_ERROR_SOCKET_BADSOCKET, on generic error
 * \arg HYPORT_ERROR_SOCKET_NOTINITIALIZED, if socket library uninitialized
 * \arg HYPORT_ERROR_SOCKET_ADDRINUSE  A process on the machine is already bound to the same fully-qualified address 
 * and the socket has not been marked to allow address re-use with SO_REUSEADDR. 
 * \arg HYPORT_ERROR_SOCKET_ADDRNOTAVAIL The specified address is not a valid address for this machine 
 * \arg HYPORT_ERROR_SOCKET_SYSTEMBUSY, if system busy handling other requests
 * \arg HYPORT_ERROR_SOCKET_SYSTEMFULL, is too many sockets are active
 * \arg HYPORT_ERROR_SOCKET_BADADDR, the addr parameter is not a valid part of the user address space, 
 */
/* IPv6 - Since we may have 2 sockets open when we are in IPv6 mode we need to
 * close down the second socket.  At the bind stage we now know the IP address' family
 * so we can shut down the other socket.
 */
I_32 VMCALL
hysock_bind (struct HyPortLibrary * portLibrary, hysocket_t sock,
	     hysockaddr_t addr)
{
  I_32 rc = 0;
  BOOL isIPv4 = ((OSSOCKADDR *) & addr->addr)->sin_family == OS_AF_INET4;
  BOOL anyAddress = isAnyAddress (addr);
  OSSOCKET socket;
  struct sockaddr_in temp4Name;
  struct sockaddr_in6 temp6Name;
  I_32 addrlen;
  OSSOCKADDR_STORAGE anyAddr;

  /* use the IPv4 socket, if is an IPv4 address or where  IPv6 socket is null (when preferIPv4Stack=true),
   * it will give a meaningful error msg.
   */
  if (isIPv4 || (sock->ipv6 == -1))
    {
      socket = sock->ipv4;
    }
  else
    {
      socket = sock->ipv6;
    }

  if (SOCKET_ERROR ==
      bind (socket, (const struct sockaddr FAR *) &addr->addr,
	    sizeof (addr->addr)))
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<bind failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_ALREADYBOUND);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }

  /* If we are the any address, then we want to bind on the other IP stack as well, if that socket is still open */
  if (isAnyAddress
      && ((isIPv4 && (sock->flags & SOCKET_IPV6_OPEN_MASK))
	  || (!isIPv4 && (sock->flags & SOCKET_IPV4_OPEN_MASK))))
    {
      memset (&anyAddr, 0, sizeof (OSSOCKADDR_STORAGE));

      if (isIPv4)
	{
	  socket = sock->ipv6;
	  addrlen = sizeof (temp4Name);
	  rc =
	    getsockname (sock->ipv4, (struct sockaddr *) &temp4Name,
			 &addrlen);
	  ((OSSOCKADDR_IN6 *) & anyAddr)->sin6_port = temp4Name.sin_port;
	  ((OSSOCKADDR *) & anyAddr)->sin_family = OS_AF_INET6;
	}
      else
	{
	  socket = sock->ipv4;
	  addrlen = sizeof (temp6Name);
	  rc =
	    getsockname (sock->ipv6, (struct sockaddr *) &temp6Name,
			 &addrlen);
	  /*TODO: put error check here - determine what to do in the case where unable to get port */
	  ((OSSOCKADDR *) & anyAddr)->sin_port = temp6Name.sin6_port;
	  ((OSSOCKADDR *) & anyAddr)->sin_family = OS_AF_INET4;
	}

      if (SOCKET_ERROR ==
	  bind (socket, (const struct sockaddr FAR *) &anyAddr,
		sizeof (addr->addr)))
	{
	  rc = WSAGetLastError ();
	  HYSOCKDEBUG ("<bind failed, err=%d>\n", rc);
	  switch (rc)
	    {
	    case WSAEINVAL:
	      return portLibrary->error_set_last_error (portLibrary, rc,
							HYPORT_ERROR_SOCKET_ALREADYBOUND);
	    default:
	      return portLibrary->error_set_last_error (portLibrary, rc,
							findError (rc));
	    }
	}
    }

  /* close the other half of the socket, if we are not the ANY address ::0 or 0.0.0.0 */
  if (!anyAddress)
    {
      internalCloseSocket (portLibrary, sock, !isIPv4);
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_close
/**
 * The close function closes a socket. Use it to release the socket descriptor socket so 
 * further references to socket will fail.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock hysocket_t  which will be closed.
 *
 * @return
 * \arg 0, on success
 * \arg HYPORT_ERROR_SOCKET_BADSOCKET, on generic error
 * \arg HYPORT_ERROR_SOCKET_SYSTEMBUSY, if system busy handling other requests
 * \arg HYPORT_ERROR_SOCKET_WOULDBLOCK,  the socket is marked as nonblocking and SO_LINGER
 *                                       is set to a nonzero time-out value.
 */
I_32 VMCALL
hysock_close (struct HyPortLibrary * portLibrary, hysocket_t * sock)
{
  I_32 rc1 = 0;
  I_32 rc2 = 0;
  hysocket_t theSocket = *sock;

  rc1 = internalCloseSocket (portLibrary, theSocket, TRUE);
  rc2 = internalCloseSocket (portLibrary, theSocket, FALSE);

  portLibrary->mem_free_memory (portLibrary, theSocket);
  *sock = (hysocket_t) INVALID_SOCKET;

  if (rc1 == 0)
    {
      rc1 = rc2;
    }

  return rc1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_connect
/**
 * Establish a connection to a peer.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock pointer to the unconnected local socket.
 * @param[in] addr	pointer to the sockaddr, specifying remote host/port.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
/* IPv6 - we may have more than one open socket at this point, an IPv6 and an IPv4.  Now
 * that we are connect to a specific address we can determine whether to use the IPv4
 * or IPv6 address and close the other address.
 *
 */
I_32 VMCALL
hysock_connect (struct HyPortLibrary * portLibrary, hysocket_t sock,
		hysockaddr_t addr)
{
  I_32 rc = 0;
  DWORD socketType;
  int socketTypeLen = sizeof (DWORD);
  byte nAddrBytes[HYSOCK_INADDR6_LEN];

  /* get the socket type, it should be the same for both sockets and one of the two should be open */
  if (sock->flags & SOCKET_IPV4_OPEN_MASK)
    {
      if (getsockopt
	  (sock->ipv4, SOL_SOCKET, SO_TYPE, (char *) &socketType,
	   &socketTypeLen) == SOCKET_ERROR)
	{
	  rc = WSAGetLastError ();
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  else
    {
      if (getsockopt
	  (sock->ipv6, SOL_SOCKET, SO_TYPE, (char *) &socketType,
	   &socketTypeLen) == SOCKET_ERROR)
	{
	  rc = WSAGetLastError ();
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }

  /* here we need to do the connect based on the type of addressed passed in as well as the sockets which are open. If 
     a socket with a type that matches the type of the address passed in is open then we use that one.  Otherwise we
     use the socket that is open */
  if (((((OSSOCKADDR *) & addr->addr)->sin_family != OS_AF_UNSPEC) &&
       ((((OSSOCKADDR *) & addr->addr)->sin_family == OS_AF_INET4) ||
	!(sock->flags & SOCKET_IPV6_OPEN_MASK)) &&
       (sock->flags & SOCKET_IPV4_OPEN_MASK)))
    {
      rc =
	connect (sock->ipv4, (const struct sockaddr FAR *) &addr->addr,
		 sizeof (addr->addr));
      if (socketType != SOCK_DGRAM)
	{
	  internalCloseSocket (portLibrary, sock, FALSE);
	}
      else
	{
	  /* we don't acutally want to close the sockets as connect can be called again on a datagram socket  but we 
	     still need to set the flag that tells us which socket to use */
	  sock->flags = sock->flags | SOCKET_USE_IPV4_MASK;
	}
    }
  else if (((OSSOCKADDR *) & addr->addr)->sin_family == OS_AF_INET6)
    {
      rc =
	connect (sock->ipv6, (const struct sockaddr FAR *) &addr->addr,
		 sizeof (addr->addr));
      if (socketType != SOCK_DGRAM)
	{
	  internalCloseSocket (portLibrary, sock, TRUE);
	}
      else
	{
	  /* we don't acutally want to close the sockets as connect can be called again on a datagram socket  but we 
	     still need to set the flag that tells us which socket to use. */
	  sock->flags = sock->flags & ~SOCKET_USE_IPV4_MASK;
	}
    }
  else
    {
      if (socketType != SOCK_DGRAM)
	{
	  /* this should never occur */
	  return HYPORT_ERROR_SOCKET_BADAF;
	}

      /* for windows it seems to want to have it connect with an IN_ADDR any instead of with an 
         UNSPEC familty type so lets be accomodating */

      /* we need to disconnect on both sockets and swallow any expected errors */
      memset (nAddrBytes, 0, HYSOCK_INADDR6_LEN);
      if (sock->flags & SOCKET_IPV4_OPEN_MASK)
	{
	  hysock_sockaddr_init6 (portLibrary, addr, (U_8 *) nAddrBytes,
				 HYSOCK_INADDR_LEN, HYADDR_FAMILY_AFINET4, 0,
				 0, 0, sock);
	  rc =
	    connect (sock->ipv4, (const struct sockaddr FAR *) &addr->addr,
		     sizeof (addr->addr));
	}

      /* filter out acceptable errors */
      if (rc == SOCKET_ERROR)
	{
	  rc = WSAGetLastError ();
	  if (rc == WSAEAFNOSUPPORT || rc == WSAEADDRNOTAVAIL)
	    {
	      rc = 0;
	    }
	}

      if (rc == 0 && sock->flags & SOCKET_IPV6_OPEN_MASK)
	{
	  hysock_sockaddr_init6 (portLibrary, addr, (U_8 *) nAddrBytes,
				 HYSOCK_INADDR_LEN, HYADDR_FAMILY_AFINET6, 0,
				 0, 0, sock);
	  connect (sock->ipv6, (const struct sockaddr FAR *) &addr->addr,
		   sizeof (addr->addr));
	}
    }

  if (rc == SOCKET_ERROR)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<connect failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_ALREADYBOUND);
	case WSAEAFNOSUPPORT:
	  /* if it is a SOCK_DGRAM this is ok as posix says this may be returned when disconnecting */
	  if (socketType == SOCK_DGRAM)
	    {
	      return 0;
	    }
	  /* no break here as default is what we want if we do not return above */
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_error_message
/**
 * Return an error message describing the last OS error that occurred.  The last
 * error returned is not thread safe, it may not be related to the operation that
 * failed for this thread.
 *
 * @param[in] portLibrary The port library
 *
 * @return	error message describing the last OS error, may return NULL.
 *
 * @internal
 * @note  This function gets the last error code from the OS and then returns
 * the corresponding string.  It is here as a helper function for JCL.  Once hyerror
 * is integrated into the port library this function should probably disappear.
 */
const char *VMCALL
hysock_error_message (struct HyPortLibrary *portLibrary)
{
  return portLibrary->error_last_error_message (portLibrary);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_fdset_init
/**
 * Create a file descriptor (FD) set of one element.  The call may not be generally useful,
 * as it currently only supports a single FD and is assumed to be used in conjunction with the 
 * hysock_select function.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP pointer to the socket to be added to the FD set.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_fdset_init (struct HyPortLibrary * portLibrary, hysocket_t socketP)
{
  PortlibPTBuffers_t ptBuffers;
  hyfdset_t fdset;

  ptBuffers = hyport_tls_get (portLibrary);
  if (NULL == ptBuffers)
    {
      return HYPORT_ERROR_SOCKET_SYSTEMFULL;
    }

  if (NULL == ptBuffers->fdset)
    {
      ptBuffers->fdset =
	portLibrary->mem_allocate_memory (portLibrary,
					  sizeof (struct hyfdset_struct));
      if (NULL == ptBuffers->fdset)
	{
	  return HYPORT_ERROR_SOCKET_SYSTEMFULL;
	}
    }
  fdset = ptBuffers->fdset;
  memset (fdset, 0, sizeof (struct hyfdset_struct));

  FD_ZERO (&fdset->handle);
  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      FD_SET (socketP->ipv4, &fdset->handle);
    }
  if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      FD_SET (socketP->ipv6, &fdset->handle);
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_fdset_size
/**
 * Answer the maximum size of the fdset currently declared for the platform.
 * This value is a parameter of the @ref hysock_select call.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle
 *
 * @return	the maximum size of the fdset, otherwise the (negative) error code.
 *
 * @note On Unix, the value was the maximum file descriptor plus one, although
 * on many flavors, the value is ignored in the select function.
 * It is essential on Neutrino 2.0.
 * On Windows, the value is ignored by the select function.
 * On OS/2, the value is the number of file descriptors to be checked.
 */
I_32 VMCALL
hysock_fdset_size (struct HyPortLibrary * portLibrary, hysocket_t handle)
{
  I_32 rc;
  if (handle->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc = handle->ipv4 + 1;
    }
  if (handle->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc = handle->ipv6 + 1;
      if ((handle->ipv4 != -1) && (handle->ipv4 > handle->ipv6))
	{
	  rc = handle->ipv4 + 1;
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_freeaddrinfo
/**
 * Frees the memory created by the call to @ref hysock_getaddrinfo().
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Hints on what results are returned and how the response if formed .
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_freeaddrinfo (struct HyPortLibrary * portLibrary, hyaddrinfo_t handle)
{
  /* If we have the IPv6 functions we free the memory for an addr info, otherwise we just set the pointer to null.
     The hostent structure returned by the IPv4 function is not supposed to be freed  */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      freeaddrinfo ((OSADDRINFO *) handle->addr_info);
    }

  handle->addr_info = NULL;
  handle->length = 0;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getaddrinfo
/**
 * \section hysock_getaddrinfo()
 * Answers a list of addresses as an opaque pointer in "result".
 * 
 * Use the following functions to extract the details:
 * \arg \ref hysock_getaddrinfo_length
 * \arg \ref hysock_getaddrinfo_name
 * \arg \ref hysock_getaddrinfo_address
 * \arg \ref hysock_getaddrinfo_family
 *
 * If the machine type supports IPv6 you can specify how you want the results returned with the following function:
 * \arg \ref hysock_create_getaddrinfo_hints.
 * Passing the structure into a machine with only IPv4 support will have no effect.
 *
 * @param[in] portLibrary The port library.
 * @param[in] name The name of the host in either host name format or in IPv4 or IPv6 accepted notations.
 * @param[in] hints Hints on what results are returned and how the response if formed (can be NULL for default action).
 * @param[out] result An opaque pointer to a list of results (hyaddrinfo_struct must be preallocated).
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note you must free the "result" structure with @ref hysock_freeaddrinfo to free up memory.  This must be done
 * before you make a subsequent call in the same thread to this function. 
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_getaddrinfo (struct HyPortLibrary * portLibrary, char *name,
		    hyaddrinfo_t hints, hyaddrinfo_t result)
{
  struct hyhostent_struct hyhostent;
  U_32 addr = 0;
  I_32 rc = 0;
  OSADDRINFO *ipv6_result;
  OSADDRINFO *addr_info_hints = NULL;

  int count = 0;

  /* If we have the IPv6 functions available we will call them, otherwise we'll call the IPv4 function */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      if (hints != NULL)
	{
	  addr_info_hints = (OSADDRINFO *) hints->addr_info;
	}
      if (0 != getaddrinfo (name, NULL, addr_info_hints, &ipv6_result))
	{
	  I_32 errorCode = WSAGetLastError ();
	  HYSOCKDEBUG ("<getaddrinfo failed, err=%d>\n", errorCode);
	  return portLibrary->error_set_last_error (portLibrary, errorCode,
						    findError (errorCode));
	}
      else
	{
	  memset (result, 0, sizeof (struct hyaddrinfo_struct));
	  result->addr_info = (void *) ipv6_result;
	  while (ipv6_result->ai_next != NULL)
	    {
	      count++;
	      ipv6_result = ipv6_result->ai_next;
	    }
	  result->length = ++count;	/* Have to add an extra, because we didn't count the first entry */
	}
    }
  else
    {
      if (0 != portLibrary->sock_inetaddr (portLibrary, name, &addr))
	{
	  if (0 !=
	      (rc =
	       portLibrary->sock_gethostbyname (portLibrary, name,
						&hyhostent)))
	    {
	      return rc;
	    }
	}
      else
	{
	  if ((0 !=
	       (rc =
		portLibrary->sock_gethostbyaddr (portLibrary, (char *) &addr,
						 sizeof (addr), HYSOCK_AFINET,
						 &hyhostent)))
	      && (0 !=
		  portLibrary->sock_gethostbyname (portLibrary, name,
						   &hyhostent)))
	    {
	      return rc;
	    }
	}

      memset (result, 0, sizeof (struct hyaddrinfo_struct));
      result->addr_info = (void *) hyhostent.entity;

      /* count the host names and the addresses */
      while (hyhostent.entity->h_addr_list[count] != 0)
	{
	  count++;
	}
      result->length = count;
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getaddrinfo_address
/**
 * Answers a U_8 array representing the address at "index" in the structure returned from @ref hysock_getaddrinfo,
 * indexed starting at 0. The address is in network byte order. 
 *
 * The address will either be 4 or 16 bytes depending on whether it is an OS_AF_INET  address or an
 * OS_AF_INET6  address.  You can check this will a call to @ref hysock_getaddrinfo_family.  Therefore you
 * should either check the family type before preallocating the "address" or define it as 16 bytes.
 *
 * @param[in]   portLibrary The port library.
 * @param[in]   handle The result structure returned by @ref hysock_getaddrinfo.
 * @param[out] address The address at "index" in a preallocated buffer.
 * @param[in]   index The address index into the structure returned by @ref hysock_getaddrinfo.
 * @param[out] scope_id  The scope id associated with the address if applicable
 *
 * @return	
 * \arg 0, if no errors occurred, otherwise the (negative) error code
 * \arg HYPORT_ERROR_SOCKET_VALUE_NULL when we have have the old IPv4 gethostbyname call and the address indexed is out
 * of range.  This is because the address list and the host alias list are not the same length.  Just skip this entry.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_getaddrinfo_address (struct HyPortLibrary * portLibrary,
			    hyaddrinfo_t handle, U_8 * address, int index,
			    U_32 * scope_id)
{
  I_32 rc = 0;
  OSADDRINFO *addr;
  void *sock_addr;

  char **addr_list;
  int i;

  /* If we have the IPv6 functions available we cast to an OSADDRINFO structure otherwise a OSHOSTENET structure */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      addr = (OSADDRINFO *) handle->addr_info;
      for (i = 0; i < index; i++)
	{
	  addr = addr->ai_next;
	}
      if (addr->ai_family == OS_AF_INET6)
	{
	  sock_addr = ((OSSOCKADDR_IN6 *) addr->ai_addr)->sin6_addr.s6_addr;
	  memcpy (address, sock_addr, 16);
	  *scope_id = ((OSSOCKADDR_IN6 *) addr->ai_addr)->sin6_scope_id;
	}
      else
	{
	  sock_addr = &((OSSOCKADDR *) addr->ai_addr)->sin_addr.S_un.S_un_b;
	  memcpy (address, sock_addr, 4);
	  *scope_id = 0;
	}
    }
  else
    {
      /* initialize the scope id */
      *scope_id = 0;

      addr_list = ((OSHOSTENT *) handle->addr_info)->h_addr_list;
      for (i = 0; i < index; i++)
	{
	  if (addr_list[i] == NULL)
	    {
	      return HYPORT_ERROR_SOCKET_VALUE_NULL;
	    }
	}
      memcpy (address, addr_list[index], 4);
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getaddrinfo_create_hints
/**
 * Answers a hints structure as an opaque pointer in "result".
 * 
 * This hints structure is used to modify the results returned by a call to @ref hysock_getaddrinfo.  There is one of
 * these structures per thread, so subsequent calls to this function will overwrite the same structure in memory.
 * The structure is cached in ptBuffers and is cleared when a call to @ref hyport_free_ptBuffer is made.
 *
 * This function is only works on IPv6 supported OS's.  If it is called on an OS that does not support IPv6 then
 * it essentially returns a NULL pointer, meaning it will have no effect on the call to @ref hysock_getaddrinfo.
 *
 * See man pages, or MSDN doc on getaddrinfo for information on how the hints structure works.
 *
 * @param[in] portLibrary The port library.
 * @param[out] result The filled in (per thread) hints structure
 * @param[in] family A address family type
 * @param[in] socktype A socket type
 * @param[in] protocol A protocol family
 * @param[in] flags Flags for modifying the result
 *
 * @return
 * \arg 0, if no errors occurred, otherwise the (negative) error code
 * \arg HYPORT_ERROR_SOCKET_SYSTEMFULL -- when we can't allocate memory for the ptBuffers, or the hints structure
 *
 * @note current supported family types are:
 * \arg HYADDR_FAMILY_UNSPEC
 * \arg HYADDR_FAMILY_AFINET4
 * \arg HYADDR_FAMILY_AFINET6
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_getaddrinfo_create_hints (struct HyPortLibrary * portLibrary,
				 hyaddrinfo_t * result, I_16 family,
				 I_32 socktype, I_32 protocol, I_32 flags)
{
  I_32 rc = 0;
  OSADDRINFO *addrinfo;
  PortlibPTBuffers_t ptBuffers;
  *result = NULL;

#define addrinfohints (ptBuffers->addr_info_hints).addr_info
  /* If we have the IPv6 functions available we fill in the structure, otherwise it is null */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      ptBuffers = hyport_tls_get (portLibrary);
      if (NULL == ptBuffers)
	{
	  return HYPORT_ERROR_SOCKET_SYSTEMFULL;
	}
      if (!addrinfohints)
	{
	  addrinfohints =
	    portLibrary->mem_allocate_memory (portLibrary,
					      sizeof (OSADDRINFO));
	  if (!addrinfohints)
	    {
	      return HYPORT_ERROR_SOCKET_SYSTEMFULL;
	    }
	}
      memset (addrinfohints, 0, sizeof (OSADDRINFO));
      addrinfo = (OSADDRINFO *) addrinfohints;
      addrinfo->ai_flags = flags;
      addrinfo->ai_family = map_addr_family_Hy_to_OS (family);
      addrinfo->ai_socktype = map_sockettype_Hy_to_OS (socktype);
      addrinfo->ai_protocol = map_protocol_family_Hy_to_OS (protocol);
      *result = &ptBuffers->addr_info_hints;
    }
#undef addrinfohints

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getaddrinfo_family
/**
 * Answers the family type of the address at "index" in the structure returned from
 * @ref hysock_getaddrinfo, indexed starting at 0.
 *
 * Currently the family types we support are HYSOCK_AFINET and HYSOCK_AFINET6.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle The result structure returned by @ref hysock_getaddrinfo.
 * @param[out] family The family at "index".
 * @param[in] index The address index into the structure returned by @ref hysock_getaddrinfo.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_getaddrinfo_family (struct HyPortLibrary * portLibrary,
			   hyaddrinfo_t handle, I_32 * family, int index)
{
  I_32 rc = 0;
  OSADDRINFO *addr;
  int i;

  /* If we have the IPv6 functions then we'll cast to a OSADDRINFO othewise we have a hostent */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      addr = (OSADDRINFO *) handle->addr_info;
      for (i = 0; i < index; i++)
	{
	  addr = addr->ai_next;
	}
      if (addr->ai_family == OS_AF_INET4)
	{
	  *family = HYADDR_FAMILY_AFINET4;
	}
      else
	{
	  *family = HYADDR_FAMILY_AFINET6;
	}
    }
  else
    {
      *family = HYADDR_FAMILY_AFINET4;
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getaddrinfo_length
/**
 * Answers the number of results returned from @ref hysock_getaddrinfo.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle The result structure returned by @ref hysock_getaddrinfo.
 * @param[out] length The number of results.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_getaddrinfo_length (struct HyPortLibrary * portLibrary,
			   hyaddrinfo_t handle, I_32 * length)
{
  *length = handle->length;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getaddrinfo_name
/**
 * Answers the cannon name of the address at "index" in the structure returned from
 *  @ref hysock_getaddrinfo, indexed starting at 0.
 * 
 * The preallocated buffer for "name" should be the size of the maximum host name: OSNIMAXHOST.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle The result structure returned by @ref hysock_getaddrinfo.
 * @param[out] name The name of the address at "index" in a preallocated buffer.
 * @param[in] index The address index into the structure returned by @ref hysock_getaddrinfo.
 *
 * @return
 * \arg 0, if no errors occurred, otherwise the (negative) error code.
 * \arg HYPORT_ERROR_SOCKET_VALUE_NULL when we have have the old IPv4 gethostbyname call and the name indexed is out
 * of range.  This is because the address list and the host alias list are not the same length.  Just skip this entry.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_getaddrinfo_name (struct HyPortLibrary * portLibrary,
			 hyaddrinfo_t handle, char *name, int index)
{
  I_32 rc = 0;
  char **alias_list;
  int i;
  OSADDRINFO *addr;

  /* If we have the IPv6 functions available we cast to an OSADDRINFO structure otherwise a OSHOSTENET structure */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      addr = (OSADDRINFO *) handle->addr_info;
      for (i = 0; i < index; i++)
	{
	  addr = addr->ai_next;
	}
      if (addr->ai_canonname == NULL)
	{
	  name[0] = 0;
	}
      else
	{
	  strcpy (name, addr->ai_canonname);
	}
    }
  else
    {
      alias_list = ((OSHOSTENT *) handle->addr_info)->h_aliases;
      for (i = 0; i < index; i++)
	{
	  if (alias_list[i] == NULL)
	    {
	      return HYPORT_ERROR_SOCKET_VALUE_NULL;
	    }
	}
      if (alias_list[index] == NULL)
	{
	  name[0] = 0;
	}
      else
	{
	  strcpy (name, alias_list[index]);
	}
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_gethostbyaddr
/**
 * Answer information on the host referred to by the address.  The information includes name, aliases and
 * addresses for the nominated host (the latter being relevant on multi-homed hosts).
 * This call has only been tested for addresses of type AF_INET.
 *
 * @param[in] portLibrary The port library.
 * @param[in] addr Pointer to the binary-format (not null-terminated) address, in network byte order.
 * @param[in] length Length of the addr.
 * @param[in] type The type of the addr.
 * @param[out] handle Pointer to the hyhostent_struct, to be linked to the per thread platform hostent struct.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_gethostbyaddr (struct HyPortLibrary * portLibrary, char *addr,
		      I_32 length, I_32 type, hyhostent_t handle)
{
  OSHOSTENT *result;

  result = gethostbyaddr (addr, length, type);
  if (result == 0)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<gethostbyaddr failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  else
    {
      memset (handle, 0, sizeof (struct hyhostent_struct));
      handle->entity = result;
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_gethostbyname
/**
 * Answer information on the host, specified by name.  The information includes host name,
 * aliases and addresses.
 *
 * @param[in] portLibrary The port library.
 * @param[in] name The host name string.
 * @param[out] handle Pointer to the hyhostent_struct (to be linked to the per thread platform hostent struct).
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_gethostbyname (struct HyPortLibrary * portLibrary, char *name,
		      hyhostent_t handle)
{
  OSHOSTENT *result;

  result = gethostbyname (name);
  if (result == 0)
    {
      I_32 errorCode = WSAGetLastError ();
      HYSOCKDEBUG ("<gethostbyname failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  else
    {
      memset (handle, 0, sizeof (struct hyhostent_struct));
      handle->entity = result;
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_gethostname
/**
 * Answer the name of the local host machine.
 *
 * @param[in] portLibrary The port library.
 * @param[in,out] buffer The string buffer to receive the name
 * @param[in] length The length of the buffer
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code
 */
I_32 VMCALL
hysock_gethostname (struct HyPortLibrary * portLibrary, char *buffer,
		    I_32 length)
{
  if ((gethostname (buffer, length)) != 0)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<gethostname failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getnameinfo
/**
 * Answers the host name of the "in_addr" in a preallocated buffer.
 *
 * The preallocated buffer for "name" should be the size of the maximum host name: OSNIMAXHOST.
 * Currently only AF_INET and AF_INET6 are supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] in_addr The address we want to do a name lookup on
 * @param[in] sockaddr_size The size of "in_addr"
 * @param[out] name The hostname of the passed address in a preallocated buffer.
 * @param[in] name_length The length of the buffer pointed to by name
 * @param[in] flags Flags on how to form the repsonse (see man pages or doc for getnameinfo)
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code
 *
 * @note Added for IPv6 support.
 * @note "flags" do not affect results on OS's that do not support the IPv6 calls.
 */
I_32 VMCALL
hysock_getnameinfo (struct HyPortLibrary * portLibrary, hysockaddr_t in_addr,
		    I_32 sockaddr_size, char *name, I_32 name_length,
		    int flags)
{
  OSHOSTENT *result;
  OSSOCKADDR *addr;
  int size;
  int rc = 0;

  /* If we have the IPv6 functions available we will call them, otherwise we'll call the IPv4 function */
  if (PPG_sock_IPv6_FUNCTION_SUPPORT)
    {
      /* Windows code requires that the sockaddr structure be of the right type, rather than
       * just checking to see if it is large enough */
      addr = (OSSOCKADDR *) & in_addr->addr;
      if (addr->sin_family == OS_AF_INET4)
	{
	  sockaddr_size = sizeof (OSSOCKADDR);
	}
      else
	{
	  sockaddr_size = sizeof (OSSOCKADDR_IN6);
	}
      rc =
	getnameinfo ((OSADDR *) & in_addr->addr, sockaddr_size, name,
		     name_length, NULL, 0, flags);
      if (rc != 0)
	{
	  I_32 errorCode = WSAGetLastError ();
	  HYSOCKDEBUG ("<gethostbyaddr failed, err=%d>\n", errorCode);
	  return portLibrary->error_set_last_error (portLibrary, errorCode,
						    findError (errorCode));
	}
    }
  else
    {				/* IPv4 call */
      addr = (OSSOCKADDR *) & in_addr->addr;
      if (addr->sin_family == OS_AF_INET4)
	{
	  size = 4;
	}
      else
	{
	  size = 16;
	}

      result =
	gethostbyaddr ((char *) &addr->sin_addr, size, addr->sin_family);
      if (result == 0)
	{
	  I_32 errorCode = WSAGetLastError ();

	  HYSOCKDEBUG ("<gethostbyaddr failed, err=%d>\n", errorCode);
	  return portLibrary->error_set_last_error (portLibrary, errorCode,
						    findError (errorCode));
	}
      else
	{
	  strcpy (name, result->h_name);
	}
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getopt_bool
/**
 * Answer the value of the nominated boolean socket option.
 * Refer to the private platformSocketLevel & platformSocketOption functions for details of the options
 * supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to query for the option value.
 * @param[in] optlevel	 The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to retrieve.
 * @param[out] optval Pointer to the boolean to update with the option value.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_getopt_bool (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		    I_32 optlevel, I_32 optname, BOOLEAN * optval)
{
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  BOOL option;
  I_32 optlen = sizeof (option);
  I_32 rc = 0;

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both sockets are open we only need to query the IPv4 option as will both be set to the same value */

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) &option, &optlen);
    }
  else if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      if (platformOption == IP_MULTICAST_LOOP)
	{
	  platformLevel = IPPROTO_IPV6;
	  platformOption = IPV6_MULTICAST_LOOP;
	}
      rc =
	getsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) &option, &optlen);
    }

  if (rc != 0)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<getsockopt (for bool) failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  *optval = (BOOLEAN) option;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getopt_byte
/**
 * Answer the value of the nominated byte socket option.
 * Refer to the private platformSocketLevel & platformSocketOption functions for details of the options
 * supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to query for the option value.
 * @param[in] optlevel	 The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to retrieve.
 * @param[out] optval Pointer to the byte to update with the option value.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_getopt_byte (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		    I_32 optlevel, I_32 optname, U_8 * optval)
{
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  U_32 optTemp = 0;
  I_32 optlen = sizeof (optTemp);
  I_32 rc = 0;

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both sockets are open we only need to query the IPv4 option as will both be set to the same value */

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) &optTemp, &optlen);
    }
  else if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      if (platformOption == IP_MULTICAST_TTL)
	{
	  platformLevel = IPPROTO_IPV6;
	  platformOption = IPV6_MULTICAST_HOPS;
	}
      rc =
	getsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) &optTemp, &optlen);
    }

  if (rc != 0)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<getsockopt (for byte) failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  else
    {
      *optval = (0xFF & optTemp);
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getopt_int
/**
 * Answer the value of the nominated integer socket option.
 * Refer to the private platformSocketLevel & platformSocketOption functions for details of the options
 * supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to query for the option value.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to retrieve.
 * @param[out] optval Pointer to the integer to update with the option value.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_getopt_int (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		   I_32 optlevel, I_32 optname, I_32 * optval)
{
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (*optval);
  I_32 rc = 0;

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both sockets are open we only need to query the IPv4 option as will both be set to the same value */
  /* unless the option is at the IPV6 proto level in which case we have to look at the IPV6 socket */
  if ((socketP->flags & SOCKET_IPV4_OPEN_MASK)
      && (platformLevel != OS_IPPROTO_IPV6))
    {
      rc =
	getsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) optval, &optlen);
    }
  else if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) optval, &optlen);
    }
  if (rc != 0)
    {
      I_32 errorCode = WSAGetLastError ();
      HYSOCKDEBUG ("<getsockopt (for int) failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getopt_linger

/**
 * Answer the value of the socket linger option.
 * See the @ref hysock_linger_init for details of the linger behavior.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to query for the option value
 * @param[in] optlevel The level within the IP stack at which the option is defined
 * @param[in] optname The name of the option to retrieve
 * @param[out] optval Pointer to the linger struct to update with the option value
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code
 */
I_32 VMCALL
hysock_getopt_linger (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		      I_32 optlevel, I_32 optname, hylinger_t optval)
{
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (optval->linger);
  I_32 rc = 0;

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both sockets are open we only need to query the IPv4 option as will both be set to the same value */

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) (&optval->linger), &optlen);
    }
  else if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) (&optval->linger), &optlen);
    }
  if (rc != 0)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<getsockopt (for linger) failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getopt_sockaddr
/**
 * Answer the value of the socket option, an address struct.
 * Currently only used to retrieve the interface of multicast sockets, 
 * but the more general call style has been used.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to query for the option value.
 * @param[in] optlevel	 The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to retrieve.
 * @param[out] optval Pointer to the sockaddr struct to update with the option value.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_getopt_sockaddr (struct HyPortLibrary * portLibrary,
			hysocket_t socketP, I_32 optlevel, I_32 optname,
			hysockaddr_t optval)
{
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (((OSSOCKADDR *) & optval->addr)->sin_addr);
  I_32 rc = 0;

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both sockets are open we only need to query the IPv4 option as will both be set to the same value */
  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) &((OSSOCKADDR *) & optval->addr)->sin_addr,
		    &optlen);
    }
  else if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc =
	getsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) &((OSSOCKADDR *) & optval->addr)->sin_addr,
		    &optlen);
    }

  if (rc != 0)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<getsockopt (for sockaddr) failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getpeername
/**
 * Answer the remote name for the socket.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the socket to get the address of.
 * @param[out] addrHandle Pointer to the sockaddr struct to update with the address.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_getpeername (struct HyPortLibrary * portLibrary, hysocket_t handle,
		    hysockaddr_t addrHandle)
{
  I_32 rc = 0;
  I_32 addrlen = sizeof (addrHandle->addr);

  if (handle->flags & SOCKET_IPV4_OPEN_MASK
      || !(handle->flags & SOCKET_IPV6_OPEN_MASK))
    {
      rc =
	getpeername (handle->ipv4, (struct sockaddr *) &addrHandle->addr,
		     &addrlen);
    }
  else
    {
      rc =
	getpeername (handle->ipv6, (struct sockaddr *) &addrHandle->addr,
		     &addrlen);
    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<getpeername failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTBOUND);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_getsockname
/**
 * Answer the local name for the socket.  Note, the stack getsockname function
 * actually answers a sockaddr structure, not a string name as the function name
 * might imply.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the socket to get the address of.
 * @param[out] addrHandle Pointer to the sockaddr struct to update with the address.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_getsockname (struct HyPortLibrary * portLibrary, hysocket_t handle,
		    hysockaddr_t addrHandle)
{
  I_32 rc = 0;
  I_32 addrlen = sizeof (addrHandle->addr);

  if (handle->flags & SOCKET_IPV4_OPEN_MASK
      || !(handle->flags & SOCKET_IPV6_OPEN_MASK))
    {
      rc =
	getsockname (handle->ipv4, (struct sockaddr *) &addrHandle->addr,
		     &addrlen);
    }
  else
    {
      rc =
	getsockname (handle->ipv6, (struct sockaddr *) &addrHandle->addr,
		     &addrlen);
    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<getsockname failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTBOUND);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }

  /* if both sockets are open we cannot retun the address for either one as whichever one we return it is wrong in some 
     cases. Therefore,  we reset the address to the ANY address and leave the port as is as it should be the same
     for both sockets (bind makes sure that when we open the two sockets we use the same port */
  if ((handle->flags & SOCKET_IPV4_OPEN_MASK)
      && (handle->flags & SOCKET_IPV6_OPEN_MASK))
    {
      /* we know the address is any IPv4 as the IPv4 socket was used if both were open */
      ((struct sockaddr_in *) &addrHandle->addr)->sin_addr.S_un.S_addr = 0;
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_hostent_addrlist
/**
 * Answer the nominated element of the address list within the argument hostent struct.
 * The address is in network order.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the hostent struct, in which to access the addr_list.
 * @param[in] index The index of the element within the addr_list to retrieve.
 *
 * @return	the address, in network order.
 */
I_32 VMCALL
hysock_hostent_addrlist (struct HyPortLibrary * portLibrary,
			 hyhostent_t handle, U_32 index)
{
  return *((I_32 *) handle->entity->h_addr_list[index]);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_hostent_aliaslist
/**
 * Answer a reference to the list of alternative names for the host within the argument hostent struct.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the hostent struct, in which to access the addr_list
 * @param[out] aliasList Pointer to the list of alternative names, to be updated 
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code
 */
I_32 VMCALL
hysock_hostent_aliaslist (struct HyPortLibrary * portLibrary,
			  hyhostent_t handle, char ***aliasList)
{
  *aliasList = handle->entity->h_addr_list;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_hostent_hostname
/**
 * Answer the host name (string) within the argument hostent struct.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the hostent struct, in which to access the hostName.
 * @param[out] hostName Host name string.
 *
 * @return	0, the function does not validate the name access.
 */
I_32 VMCALL
hysock_hostent_hostname (struct HyPortLibrary * portLibrary,
			 hyhostent_t handle, char **hostName)
{
  *hostName = handle->entity->h_name;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_htonl
/**
 * Answer the 32 bit host ordered argument, in network byte order.
 *
 * @param[in] portLibrary The port library.
 * @param[in] val The 32 bit host ordered number.
 *
 * @return	the 32 bit network ordered number.
 */
I_32 VMCALL
hysock_htonl (struct HyPortLibrary * portLibrary, I_32 val)
{
  return htonl (val);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_htons
/**
 * Answer the 16 bit host ordered argument, in network byte order.
 *
 * @param[in] portLibrary The port library.
 * @param[in] val The 16 bit host ordered number.
 *
 * @return	the 16 bit network ordered number.
 */
U_16 VMCALL
hysock_htons (struct HyPortLibrary * portLibrary, U_16 val)
{
  return htons (val);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_inetaddr
/**
 * Answer the dotted IP string as an Internet address.
 *
 * @param[in] portLibrary The port library.
 * @param[out] addrStr The dotted IP string.
 * @param[in] addr Pointer to the Internet address.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_inetaddr (struct HyPortLibrary * portLibrary, char *addrStr,
		 U_32 * addr)
{
  I_32 rc = 0;
  U_32 val;

  val = inet_addr (addrStr);
  if (INADDR_NONE == val)
    {
      HYSOCKDEBUGPRINT ("<inet_addr failed>\n");
      rc = HYPORT_ERROR_SOCKET_ADDRNOTAVAIL;
    }
  else
    {
      *addr = val;
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_inetntoa
/**
 * Answer the Internet address as a dotted IP string.
 *
 * @param[in] portLibrary The port library.
 * @param[out] addrStr The dotted IP string.
 * @param[in] nipAddr The Internet address.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_inetntoa (struct HyPortLibrary * portLibrary, char **addrStr,
		 U_32 nipAddr)
{
  I_32 rc = 0;
  char *val;
  struct in_addr addr;
  addr.s_addr = nipAddr;
  val = inet_ntoa (addr);
  if (NULL == val)
    {
      HYSOCKDEBUGPRINT ("<inet_ntoa failed>\n");
      rc = HYPORT_ERROR_SOCKET_ADDRNOTAVAIL;
    }
  else
    {
      *addrStr = val;
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_ipmreq_init
/**
 * Initializes a new multicast membership structure.  The membership structure is used to join & leave
 * multicast groups @see hysock_setopt_ipmreq.  The group may be joined using 0 (HYSOCK_INADDR_ANY)
 * as the local interface, in which case the default local address will be used.
 *
 * @param[in] portLibrary The port library.
 * @param[out] handle Pointer to the multicast membership struct.
 * @param[in] nipmcast The address, in network order, of the multicast group to join.
 * @param[in] nipinterface The address, in network order, of the local machine interface to join on.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_ipmreq_init (struct HyPortLibrary * portLibrary, hyipmreq_t handle,
		    U_32 nipmcast, U_32 nipinterface)
{
  memset (handle, 0, sizeof (struct hyipmreq_struct));
  handle->addrpair.imr_multiaddr.s_addr = nipmcast;
  handle->addrpair.imr_interface.s_addr = nipinterface;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_ipv6_mreq_init
/**
 * Fills in a preallocated hyipv6_mreq_struct
 *
 * @param[in] portLibrary The port library.
 * @param[out] handle A pointer to the hyipv6_mreq_struct to populate.
 * @param[in] ipmcast_addr The ip mulitcast address.
 * @param[in] ipv6mr_interface The ip mulitcast inteface.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_ipv6_mreq_init (struct HyPortLibrary * portLibrary,
		       hyipv6_mreq_t handle, U_8 * ipmcast_addr,
		       U_32 ipv6mr_interface)
{
  memset (handle, 0, sizeof (struct hyipmreq_struct));
  memcpy (handle->mreq.ipv6mr_multiaddr.u.Byte, ipmcast_addr, 16);
  handle->mreq.ipv6mr_interface = ipv6mr_interface;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_linger_enabled
/**
 * Answer true if the linger is enabled in the argument linger struct.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the linger struct to be accessed.
 * @param[out] enabled Pointer to the boolean to be updated with the linger status.
 *
 * @return	0, the function does not validate the access.
 */
I_32 VMCALL
hysock_linger_enabled (struct HyPortLibrary * portLibrary, hylinger_t handle,
		       BOOLEAN * enabled)
{
  *enabled = (BOOLEAN) (handle->linger.l_onoff);
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_linger_init
/**
 * Initializes a new linger structure, enabled or disabled, with the timeout as specified.
 * Linger defines the behavior when unsent messages exist for a socket that has been sent close.
 * If linger is disabled, the default, close returns immediately and the stack attempts to deliver unsent messages.
 * If linger is enabled:
 * \arg if the timeout is 0, the close will block indefinitely until the messages are sent
 * \arg if the timeout is set, the close will return after the messages are sent or the timeout period expired
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the linger struct to be accessed.
 * @param[in] enabled Aero to disable, a non-zero value to enable linger.
 * @param[in] timeout	 0 to linger indefinitely or a positive timeout value (in seconds).
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_linger_init (struct HyPortLibrary * portLibrary, hylinger_t handle,
		    I_32 enabled, U_16 timeout)
{
  memset (handle, 0, sizeof (struct hylinger_struct));
  handle->linger.l_onoff = enabled;
  handle->linger.l_linger = timeout;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_linger_linger
/**
 * Answer the linger timeout value in the argument linger struct.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the linger struct to be accessed.
 * @param[out] linger Pointer to the integer, to be updated with the linger value (in seconds).
 *
 * @return	0, the function does not validate the access.
 */
I_32 VMCALL
hysock_linger_linger (struct HyPortLibrary * portLibrary, hylinger_t handle,
		      U_16 * linger)
{
  *linger = handle->linger.l_linger;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_listen
/**
 * Set the socket to listen for incoming connection requests.  This call is made prior to accepting requests,
 * via the @ref hysock_accept function.  The backlog specifies the maximum length of the queue of pending connections,
 * after which further requests are rejected.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Pointer to the socket to modify.
 * @param[in] backlog The maximum number of queued requests.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
/*IPv6 - If both the IPv4 and IPv6 sockets are still open, then we need to listen on both of them.  
 */
I_32 VMCALL
hysock_listen (struct HyPortLibrary * portLibrary, hysocket_t sock,
	       I_32 backlog)
{
  I_32 rc = 0;

  /* Listen on the IPv4 socket */
  if (sock->flags & SOCKET_IPV4_OPEN_MASK)
    {
      if (listen (sock->ipv4, backlog) == SOCKET_ERROR)
	{
	  rc = WSAGetLastError ();
	  HYSOCKDEBUG ("<listen failed, err=%d>\n", rc);
	  switch (rc)
	    {
	    case WSAEINVAL:
	      rc =
		portLibrary->error_set_last_error (portLibrary, rc,
						   HYPORT_ERROR_SOCKET_BOUNDORCONN);
	      break;
	    default:
	      rc =
		portLibrary->error_set_last_error (portLibrary, rc,
						   findError (rc));
	    }
	}
    }

  /* Listen on the IPv6 socket providing our return code is good */
  if (sock->flags & SOCKET_IPV6_OPEN_MASK && rc == 0)
    {
      if (listen (sock->ipv6, backlog) == SOCKET_ERROR)
	{
	  rc = WSAGetLastError ();
	  HYSOCKDEBUG ("<listen failed, err=%d>\n", rc);
	  switch (rc)
	    {
	    case WSAEINVAL:
	      rc =
		portLibrary->error_set_last_error (portLibrary, rc,
						   HYPORT_ERROR_SOCKET_BOUNDORCONN);
	      break;
	    default:
	      rc =
		portLibrary->error_set_last_error (portLibrary, rc,
						   findError (rc));
	    }
	}
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_ntohl
/**
 * Answer the 32 bit network ordered argument, in host byte order.
 *
 * @param[in] portLibrary The port library.
 * @param[in] val The 32 bit network ordered number.
 *
 * @return	the 32 bit host ordered number.
 */
I_32 VMCALL
hysock_ntohl (struct HyPortLibrary * portLibrary, I_32 val)
{
  return ntohl (val);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_ntohs
/**
 * Answer the 16-bit network ordered argument, in host byte order.
 *
 * @param[in] portLibrary The port library.
 * @param[in] val The 16-bit network ordered number.
 *
 * @return	the 16-bit host ordered number.
 */
U_16 VMCALL
hysock_ntohs (struct HyPortLibrary * portLibrary, U_16 val)
{
  return ntohs (val);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_read
/**
 * The read function receives data from a connected socket.  Calling read will return as much 
 *	information as is currently available up to the size of the buffer supplied. If no incoming 
 * data is available at the socket, the read call blocks and waits for data to arrive.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Pointer to the socket to read on
 * @param[out] buf Pointer to the buffer where input bytes are written
 * @param[in] nbyte The length of buf
 * @param[in] flags The flags, to influence this read (in addition to the socket options)
 *
 * @return
 * \arg If no error occurs, return the number of bytes received.
 * \arg If the connection has been gracefully closed, return 0.
 * \arg Otherwise return the (negative) error code.
 */
I_32 VMCALL
hysock_read (struct HyPortLibrary * portLibrary, hysocket_t sock, U_8 * buf,
	     I_32 nbyte, I_32 flags)
{
  I_32 rc = 0;
  I_32 bytesRec = 0;
  int socketTypeLen = sizeof (DWORD);

  if (sock->flags & SOCKET_USE_IPV4_MASK
      || !(sock->flags & SOCKET_IPV6_OPEN_MASK))
    {
      bytesRec = recv (sock->ipv4, (char *) buf, nbyte, flags);
    }
  else
    {				/* If IPv6 is open */
      bytesRec = recv (sock->ipv6, (char *) buf, nbyte, flags);
    }

  if (SOCKET_ERROR == bytesRec)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<recv failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTBOUND);
	case WSAEMSGSIZE:
	  rc =
	    portLibrary->error_set_last_error (portLibrary, rc, WSAEMSGSIZE);
	  if (flags == MSG_PEEK)
	    {
	      return nbyte;
	    }
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  else
    {
      rc = bytesRec;
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_readfrom
/**
 * The read function receives data from a possibly connected socket.  Calling read will return as much 
 * information as is currently available up to the size of the buffer supplied.  If the information is too large
 * for the buffer, the excess will be discarded.  If no incoming  data is available at the socket, the read call 
 * blocks and waits for data to arrive.  It the address argument is not null, the address will be updated with
 * address of the message sender.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Pointer to the socket to read on.
 * @param[out] buf Pointer to the buffer where input bytes are written.
 * @param[in] nbyte The length of buf.
 * @param[in] flags Tthe flags, to influence this read.
 * @param[out] addrHandle	if provided, the address to be updated with the sender information.
 *
 * @return
 * \arg If no error occurs, return the number of bytes received.
 * \arg If the connection has been gracefully closed, return 0.
 * \arg Otherwise return the (negative) error code.
 */
I_32 VMCALL
hysock_readfrom (struct HyPortLibrary * portLibrary, hysocket_t sock,
		 U_8 * buf, I_32 nbyte, I_32 flags, hysockaddr_t addrHandle)
{
  I_32 rc = 0;
  I_32 bytesRec = 0;
  I_32 addrlen;
  if (NULL == addrHandle)
    {
      addrlen = sizeof (*addrHandle);

      if (sock->flags & SOCKET_USE_IPV4_MASK
	  || !(sock->flags & SOCKET_IPV6_OPEN_MASK))
	{
	  bytesRec =
	    recvfrom (sock->ipv4, (char *) buf, nbyte, flags,
		      (struct sockaddr *) NULL, &addrlen);
	}
      else
	{			/* If IPv6 is open */
	  bytesRec =
	    recvfrom (sock->ipv6, (char *) buf, nbyte, flags,
		      (struct sockaddr *) NULL, &addrlen);
	}
    }
  else
    {
      addrlen = sizeof (addrHandle->addr);
      if (sock->flags & SOCKET_USE_IPV4_MASK
	  || !(sock->flags & SOCKET_IPV6_OPEN_MASK))
	{
	  ((OSSOCKADDR *) & addrHandle->addr)->sin_family = OS_AF_INET4;
	  bytesRec =
	    recvfrom (sock->ipv4, (char *) buf, nbyte, flags,
		      (struct sockaddr *) &addrHandle->addr, &addrlen);
	}
      else
	{			/* If IPv6 is open */
	  ((OSSOCKADDR *) & addrHandle->addr)->sin_family = OS_AF_INET6;
	  bytesRec =
	    recvfrom (sock->ipv6, (char *) buf, nbyte, flags,
		      (struct sockaddr *) &addrHandle->addr, &addrlen);
	}
    }
  if (SOCKET_ERROR == bytesRec)
    {
      rc = WSAGetLastError ();
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTBOUND);
	case WSAEMSGSIZE:
	  rc =
	    portLibrary->error_set_last_error (portLibrary, rc, WSAEMSGSIZE);
	  return nbyte;
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  else
    {
      rc = bytesRec;
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_select
/**
 * The select function allows the state of sockets for read & write operations and exceptional conditions to be tested.
 * The function is used prior to a hysock_read/readfrom, to control the period the operation may block for.
 * Depending upon the timeout specified:
 * \arg 0, return immediately with the status of the descriptors
 * \arg timeout, return when one of the descriptors is ready or after the timeout period has expired
 * \arg null, block indefinitely for a ready descriptor
 *
 * @param[in] portLibrary The port library.
 * @param[in] nfds Maximum number of file descriptors to be tested.
 * @param[in] readfds Tthe set of descriptors to be checked if ready for read operations.
 * @param[in] writefds The set of descriptors to be checked if ready for write operations.
 * @param[in] exceptfds The set of descriptors to be checked for exceptional conditions.
 * @param[in] timeout Pointer to the timeout (a hytimeval struct).
 *
 * @return	0 if no error occurs, otherwise return the (negative) error code.
 */
I_32 VMCALL
hysock_select (struct HyPortLibrary * portLibrary, I_32 nfds,
	       hyfdset_t readfds, hyfdset_t writefds, hyfdset_t exceptfds,
	       hytimeval_t timeout)
{
  I_32 rc = 0;
  I_32 result = 0;

  if (NULL == timeout)
    {
      result =
	select (nfds, &readfds->handle, &writefds->handle, &exceptfds->handle,
		NULL);
    }
  else
    {
      result =
	select (nfds, &readfds->handle, &writefds->handle, &exceptfds->handle,
		&timeout->time);
    }
  if (SOCKET_ERROR == result)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<select failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_INVALIDTIMEOUT);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  else
    {
      if (0 == result)
	{
	  rc = HYPORT_ERROR_SOCKET_TIMEOUT;
	}
      else
	{
	  rc = result;
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_select_read
/**
 * A helper method, to ensure a read operation can be performed without blocking.
 * The portable version of a read operation is a blocking call (will wait indefinitely for data).
 * This function should be called prior to a read operation, to provide a read timeout.
 * If the result is 1, the caller is guaranteed to be able to complete a read on the socket without blocking.
 * The actual contents of the fdset are not available for inspection (as provided in the more general 'select' function).
 * The timeout is specified in seconds and microseconds.
 * If the timeout is 0, skip this function (and thus the caller of a subsequent read operation may block).
 *
 * @param[in] portLibrary The port library.
 * @param[in] hysocketP Pointer to the hysocket to query for available read data.
 * @param[in] secTime The integer component of the timeout periond, in seconds.
 * @param[in] uSecTime The fractional component of the timeout period, in microSeconds.
 * @param[in] accept Set to true when called for an accept(), false when called for a read()
 *
 * @return
 * \arg 1, if there is data available to read at the socket
 * \arg HYPORT_ERROR_SOCKET_TIMEOUT if the call timed out
 * \arg otherwise return the (negative) error code.
 */
/* IPv6 - If both IPv4 and IPv6 sockets are open, the call to this function will alternate selecting between the sockets.
 */
I_32 VMCALL
hysock_select_read (struct HyPortLibrary * portLibrary, hysocket_t hysocketP,
		    I_32 secTime, I_32 uSecTime, BOOLEAN accept)
{
  hytimeval_struct timeP;
  I_32 result = 0;
  I_32 size = 0;
  PortlibPTBuffers_t ptBuffers;

  ptBuffers = hyport_tls_get (portLibrary);
  if (NULL == ptBuffers)
    {
      return HYPORT_ERROR_SOCKET_SYSTEMFULL;
    }

/* The max fdset size per process is always expected to be less than a 32bit integer value.
 * Is this valid on a 64bit platform?
 */

  if (0 == secTime && 0 == uSecTime)
    {
      /* add these checks, so that if only one socket is open return right away and avoid the loop below */
      if (((hysocketP->flags & SOCKET_IPV4_OPEN_MASK) != 0)
	  && ((hysocketP->flags & SOCKET_IPV6_OPEN_MASK) == 0))
	{
	  hysocketP->flags = hysocketP->flags | SOCKET_USE_IPV4_MASK;
	  /* return value of 1 means there is data to be read */
	  return 1;
	}
      else if (((hysocketP->flags & SOCKET_IPV6_OPEN_MASK) != 0)
	       && ((hysocketP->flags & SOCKET_IPV4_OPEN_MASK) == 0))
	{
	  hysocketP->flags = hysocketP->flags & ~SOCKET_USE_IPV4_MASK;
	  /* return value of 1 means there is data to be read */
	  return 1;
	}
      /* poll every 100 ms */
      hysock_timeval_init (portLibrary, 0, 100 * 1000, &timeP);
    }
  else
    {
      hysock_timeval_init (portLibrary, secTime, uSecTime, &timeP);
    }

  while (0 == 0)
    {
      result = hysock_fdset_init (portLibrary, hysocketP);
      if (0 != result)
	{
	  return result;
	}
      size = hysock_fdset_size (portLibrary, hysocketP);
      if (0 > size)
	{
	  result = HYPORT_ERROR_SOCKET_FDSET_SIZEBAD;
	}
      else
	{
	  result =
	    hysock_select (portLibrary, size, ptBuffers->fdset, NULL, NULL,
			   &timeP);
	}
      /* break out of the loop if we should not be looping (timeout is zero) or
       * if an error occured or data ready to be read */
      if ((HYPORT_ERROR_SOCKET_TIMEOUT != result) || (0 != secTime)
	  || (0 != uSecTime))
	{
	  /* check which socket has activity after select call and set the appropriate flags */
	  if (FD_ISSET (hysocketP->ipv6, ptBuffers->fdset))
	    {
	      hysocketP->flags = hysocketP->flags & ~SOCKET_USE_IPV4_MASK;
	    }
	  /* update IPv4 last, so it will be used in the event both sockets had activity */
	  if (FD_ISSET (hysocketP->ipv4, ptBuffers->fdset))
	    {
	      hysocketP->flags = hysocketP->flags | SOCKET_USE_IPV4_MASK;
	    }

	  break;
	}
    }
  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_set_nonblocking
/**
 * Set the nonblocking state of the socket.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to read on
 * @param[in] nonblocking Set true for nonblocking, false for blocking
 *
 * @return	0 if no error occurs, otherwise return the (negative) error code.
 */
I_32 VMCALL
hysock_set_nonblocking (struct HyPortLibrary * portLibrary,
			hysocket_t socketP, BOOLEAN nonblocking)
{
  I_32 rc;
  U_32 param = nonblocking;

  /* If both the IPv4 and IPv6 socket are open then we want to set the option on both.  If only one is open,
     then we set it just on that one.  */

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc = ioctlsocket (socketP->ipv4, FIONBIO, &param);
    }

  if (rc == 0 && socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc = ioctlsocket (socketP->ipv6, FIONBIO, &param);
    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<set_nonblocking (for bool) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setflag
/**
 * Ensure the flag designated is set in the argument.  This is used to construct arguments for the 
 * hysock_read/readfrom/write/writeto calls with optional flags, such as HYSOCK_MSG_PEEK.
 *
 * @param[in] portLibrary The port library.
 * @param[in] flag The operation flag to set in the argument.
 * @param[in] arg Pointer to the argument to set the flag bit in.
 *
 * @return	0 if no error occurs, otherwise return the (negative) error code.
 */
I_32 VMCALL
hysock_setflag (struct HyPortLibrary * portLibrary, I_32 flag, I_32 * arg)
{
  I_32 rc = 0;
  if (flag == HYSOCK_MSG_PEEK)
    {
      *arg |= MSG_PEEK;
    }
  else if (flag == HYSOCK_MSG_OOB)
    {
      *arg |= MSG_OOB;
    }

  else
    {
      rc = HYPORT_ERROR_SOCKET_UNKNOWNFLAG;
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_bool
/**
 * Set the value of the nominated boolean socket option.
 * Refer to the private platformSocketLevel & platformSocketOption functions for details of the options
 * supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the boolean to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_setopt_bool (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		    I_32 optlevel, I_32 optname, BOOLEAN * optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  BOOL option = (BOOL) * optval;
  I_32 optlen = sizeof (option);

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both the IPv4 and IPv6 socket are open then we want to set the option on both.  If only one is open,
     then we set it just on that one.  */

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	setsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) &option, optlen);
    }
  if (rc == 0 && socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      if ((platformOption == IP_MULTICAST_LOOP)
	  && (platformLevel == OS_IPPROTO_IP))
	{
	  platformLevel = IPPROTO_IPV6;
	  platformOption = IPV6_MULTICAST_LOOP;
	}
      rc =
	setsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) &option, optlen);
    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for bool) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_byte
/**
 * Set the value of the nominated byte socket option.
 * Refer to the private platformSocketLevel & platformSocketOption functions for details of the options
 * supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the byte to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_setopt_byte (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		    I_32 optlevel, I_32 optname, U_8 * optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 result = 0;
  U_32 optTemp;
  I_32 optlen;

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both the IPv4 and IPv6 socket are open then we want to set the option on both.  If only one is open,
     then we set it just on that one.  */

  if (platformOption == IP_MULTICAST_TTL)
    {
      optTemp = (U_32) (*optval);
      optlen = sizeof (optTemp);

      if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
	{
	  result =
	    setsockopt (socketP->ipv4, platformLevel, platformOption,
			(char *) &optTemp, optlen);
	}
      if (result == 0 && socketP->flags & SOCKET_IPV6_OPEN_MASK)
	{
	  platformLevel = IPPROTO_IPV6;
	  platformOption = IPV6_MULTICAST_HOPS;
	  result =
	    setsockopt (socketP->ipv6, platformLevel, platformOption,
			(char *) &optTemp, optlen);
	}
    }
  else
    {
      optlen = sizeof (*optval);
      if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
	{
	  result =
	    setsockopt (socketP->ipv4, platformLevel, platformOption,
			(char *) optval, optlen);
	}
      if (result == 0 && socketP->flags & SOCKET_IPV6_OPEN_MASK)
	{
	  result =
	    setsockopt (socketP->ipv6, platformLevel, platformOption,
			(char *) optval, optlen);
	}
    }
  if (0 != result)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for byte) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_int
/**
 * Set the value of the nominated integer socket option.
 * Refer to the private platformSocketLevel & platformSocketOption functions for details of the options
 * supported.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the integer to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_setopt_int (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		   I_32 optlevel, I_32 optname, I_32 * optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (*optval);

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  /* If both the IPv4 and IPv6 socket are open then we want to set the option on both.  If only one is open,
     then we set it just on that one.  Also if the option is at the IPV6 level we only set it if the 
     IPV6 socket is open  */
  if ((socketP->flags & SOCKET_IPV4_OPEN_MASK)
      && (OS_IPPROTO_IPV6 != platformLevel))
    {
      rc =
	setsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) optval, optlen);
    }

  if (rc == 0 && socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      /* set the option on the IPv6 socket unless it is IP_TOS which is not supported on IPv6 sockets */
      if (!
	  ((OS_IPPROTO_IP == platformLevel) && (OS_IP_TOS == platformOption)))
	{

	  rc =
	    setsockopt (socketP->ipv6, platformLevel, platformOption,
			(char *) optval, optlen);
	}

    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for int) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_ipmreq
/**
 * Set the multicast request on this socket. 
 * Currently this is used to join or leave the nominated multicast group on the local interface.
 * 	It may be more generally useful, so a generic 'setop' function has been defined.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the ipmreq struct to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_setopt_ipmreq (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		      I_32 optlevel, I_32 optname, hyipmreq_t optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (optval->addrpair);

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	setsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) (&optval->addrpair), optlen);
    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for ipmreq) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_ipv6_mreq
/**
 * Set the multicast request on this socket for IPv6 sockets. 
 * Currently this is used to join or leave the nominated multicast group on the local interface.
 * 	It may be more generally useful, so a generic 'setop' function has been defined.t.
 *
 * Supported families are OS_AF_INET and OS_AF_INET6 
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the ipmreq struct to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_setopt_ipv6_mreq (struct HyPortLibrary * portLibrary,
			 hysocket_t socketP, I_32 optlevel, I_32 optname,
			 hyipv6_mreq_t optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (optval->mreq);
  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }
  if (socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc =
	setsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) (&optval->mreq), optlen);
    }
  else
    {
      /* this option is not supported on this socket */
      return HYPORT_ERROR_SOCKET_SOCKLEVELINVALID;
    }
  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for ipmreq) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return HYPORT_ERROR_SOCKET_OPTARGSINVALID;
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_linger
/**
 * Set the linger value on the socket. 
 * See the @ref hysock_linger_init for details of the linger behavior.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the linger struct to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_setopt_linger (struct HyPortLibrary * portLibrary, hysocket_t socketP,
		      I_32 optlevel, I_32 optname, hylinger_t optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);
  I_32 optlen = sizeof (optval->linger);

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	setsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) (&optval->linger), optlen);
    }

  if (rc == 0 && socketP->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc =
	setsockopt (socketP->ipv6, platformLevel, platformOption,
		    (char *) (&optval->linger), optlen);
    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for linger) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_setopt_sockaddr
/**
 * Set the sockaddr for the socket.
 * Currently used to set the interface of multicast sockets, but the more general call style is used,
 * in case it is more generally useful.
 *
 * @param[in] portLibrary The port library.
 * @param[in] socketP Pointer to the socket to set the option in.
 * @param[in] optlevel The level within the IP stack at which the option is defined.
 * @param[in] optname The name of the option to set.
 * @param[out] optval Pointer to the hysockaddr struct to update the socket option with.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_setopt_sockaddr (struct HyPortLibrary * portLibrary,
			hysocket_t socketP, I_32 optlevel, I_32 optname,
			hysockaddr_t optval)
{
  I_32 rc = 0;
  I_32 platformLevel = platformSocketLevel (optlevel);
  I_32 platformOption = platformSocketOption (optname);

  /* It is safe to cast to this as this method is only used with IPv4 addresses */
  I_32 optlen = sizeof (((OSSOCKADDR *) & optval->addr)->sin_addr);

  if (0 > platformLevel)
    {
      return platformLevel;
    }
  if (0 > platformOption)
    {
      return platformOption;
    }

  if (socketP->flags & SOCKET_IPV4_OPEN_MASK)
    {
      rc =
	setsockopt (socketP->ipv4, platformLevel, platformOption,
		    (char *) &((OSSOCKADDR *) & optval->addr)->sin_addr,
		    optlen);

    }

  if (rc != 0)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<setsockopt (for sockaddr) failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_OPTARGSINVALID);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_shutdown
/**
 * Terminates use of the socket library.  No sockets should be in use or the results
 * of this operation are unpredictable.  Frees any resources held by the socket library.
 *
 * @param[in] portLibrary The port library.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code
 */
I_32 VMCALL
hysock_shutdown (struct HyPortLibrary * portLibrary)
{
  I_32 result = 0;

  result = WSACleanup ();
  if (SOCKET_ERROR == result)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<WSACleanup() failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_shutdown_input
/**
 * The shutdown_input function disables the input stream on a socket. Any subsequent reads from the socket
 * will fail.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Socket to close input stream on.
 *
 * @return
 * \arg  0, on success
 * \arg HYPORT_ERROR_SOCKET_OPFAILED, on generic error
 * \arg HYPORT_ERROR_SOCKET_NOTINITIALIZED, if the library is not initialized
*/
/* IPv6 - If we still have 2 sockets open, then close the input on both.  May happen with ::0 and 0.0.0.0.
 */
I_32 VMCALL
hysock_shutdown_input (struct HyPortLibrary * portLibrary, hysocket_t sock)
{
  I_32 rc = 0;
  /* If IPv4 is open or IPv6 is not open.  Previously we called it every time, even if the socket was closed. */
  if (sock->flags & SOCKET_USE_IPV4_MASK
      || !(sock->flags & SOCKET_IPV6_OPEN_MASK))
    {
      rc = shutdown (sock->ipv4, 0);
    }

  if (sock->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc = shutdown (sock->ipv6, 0);
    }

  if (rc == SOCKET_ERROR)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<shutdown_input failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_shutdown_output
/**
 * The shutdown_output function disables the output stream on a socket. Any subsequent writes to the socket
 * will fail.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Socket to close output stream on.
 *
 * @return
 * \arg  0, on success
 * \arg HYPORT_ERROR_SOCKET_OPFAILED, on generic error
 * \arg HYPORT_ERROR_SOCKET_NOTINITIALIZED, if the library is not initialized
 */
I_32 VMCALL
hysock_shutdown_output (struct HyPortLibrary * portLibrary, hysocket_t sock)
{
  I_32 rc = 0;

  if (sock->flags & SOCKET_USE_IPV4_MASK
      || !(sock->flags & SOCKET_IPV6_OPEN_MASK))
    {
      rc = shutdown (sock->ipv4, 1);
    }

  if (sock->flags & SOCKET_IPV6_OPEN_MASK)
    {
      rc = shutdown (sock->ipv6, 1);
    }

  if (rc == SOCKET_ERROR)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<shutdown_output failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr
/**
 * Creates a new hysockaddr, refering to the specified port and address.  The only address family currently supported
 * is AF_INET.
 *
 * @param[in] portLibrary The port library.
 * @param[out] handle Pointer to the hysockaddr struct, to be allocated.
 * @param[in] addrStr The target host, as either a name or dotted ip string.
 * @param[in] port The target port, in host order.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_sockaddr (struct HyPortLibrary * portLibrary, hysockaddr_t handle,
		 char *addrStr, U_16 port)
{
  I_32 rc = 0;
  U_32 addr = 0;
  hyhostent_struct host_t;

  if (0 != portLibrary->sock_inetaddr (portLibrary, addrStr, &addr))
    {
      memset (&host_t, 0, sizeof (struct hyhostent_struct));
      if (0 !=
	  (rc =
	   portLibrary->sock_gethostbyname (portLibrary, addrStr, &host_t)))
	{
	  return rc;
	}
      else
	{
	  addr = portLibrary->sock_hostent_addrlist (portLibrary, &host_t, 0);
	}
    }
  rc =
    portLibrary->sock_sockaddr_init (portLibrary, handle, HYSOCK_AFINET, addr,
				     port);
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr_address
/**
 * Answer the address, in network order, of the hysockaddr argument.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the hysockaddr struct to access.
 *
 * @return	the address (there is no validation on the access).
 */
I_32 VMCALL
hysock_sockaddr_address (struct HyPortLibrary * portLibrary,
			 hysockaddr_t handle)
{
  return ((OSSOCKADDR *) & handle->addr)->sin_addr.s_addr;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr_address6
/**
 * Answers the IP address of a structure and its length, in a preallocated buffer.
 *
 * Preallocated buffer "address" should be 16 bytes.  "length" tells you how many bytes were used 4 or 16.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle A populated hysockaddr_t.
 * @param[out] address The IPv4 or IPv6 address in network byte order.
 * @param[out] length The number of bytes of the address (4 or 16).
 * @param[out] scope_id the scope id for the address if appropriate
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_sockaddr_address6 (struct HyPortLibrary * portLibrary,
			  hysockaddr_t handle, U_8 * address, U_32 * length,
			  U_32 * scope_id)
{

  OSSOCKADDR *ipv4;
  OSSOCKADDR_IN6 *ipv6;

  ipv4 = (OSSOCKADDR *) & handle->addr;
  if (ipv4->sin_family == OS_AF_INET4)
    {
      memcpy (address, &ipv4->sin_addr, 4);
      *length = 4;
      *scope_id = 0;
    }
  else
    {
      ipv6 = (OSSOCKADDR_IN6 *) & handle->addr;
      memcpy (address, &ipv6->sin6_addr, 16);
      *length = 16;
      *scope_id = ipv6->sin6_scope_id;
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr_family
/**
 * Answers the family name of a hysockaddr_struct.
 *
 * Supported families are OS_AF_INET and OS_AF_INET6 
 *
 * @param[in] portLibrary The port library.
 * @param[out] family The family name of the address.
 * @param[in] handle A populated hysockaddr_t.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_sockaddr_family (struct HyPortLibrary * portLibrary, I_16 * family,
			hysockaddr_t handle)
{
  OSSOCKADDR *ipv4;

  ipv4 = (OSSOCKADDR *) & handle->addr;
  if (ipv4->sin_family == OS_AF_INET4)
    {
      *family = HYADDR_FAMILY_AFINET4;
    }
  else
    {
      *family = HYADDR_FAMILY_AFINET6;
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr_init
/**
 * Creates a new hysockaddr, refering to the specified port and address.  The only address family currently supported
 * is AF_INET.
 *
 * @param[in] portLibrary The port library.
 * @param[out] handle Pointer pointer to the hysockaddr struct, to be allocated.
 * @param[in] family The address family.
 * @param[in] nipAddr The target host address, in network order.
 * @param[in] nPort The target port, in host order.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_sockaddr_init (struct HyPortLibrary * portLibrary, hysockaddr_t handle,
		      I_16 family, U_32 nipAddr, U_16 nPort)
{
  OSSOCKADDR *sockaddr;
  memset (handle, 0, sizeof (struct hysockaddr_struct));
  sockaddr = (OSSOCKADDR *) & handle->addr;
  sockaddr->sin_family = family;
  sockaddr->sin_addr.s_addr = nipAddr;
  sockaddr->sin_port = nPort;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr_init6
/**
 * Answers an initialized hysockaddr_struct structure.
 *
 * Pass in a hysockaddr_struct with some initial parameters to initialize it appropriately.
 * Currently the only address families supported are OS_AF_INET6 and OS_AF_INET, which
 * will be determined by addrlength.  (4 bytes for IPv4 addresses and 16 bytes for IPv6 addresses).
 *
 * @param[in] portLibrary The port library.
 * @param[out] handle Pointer pointer to the hysockaddr struct, to be allocated.
 * @param[in] addr The IPv4 or IPv6 address in network byte order.
 * @param[in] addrlength The number of bytes of the address (4 or 16).
 * @param[in] family The address family.
 * @param[in] nPort The target port, in network order.
 * @param[in] flowinfo The flowinfo value for IPv6 addresses in HOST order.  Set to 0 for
 *            IPv4 addresses or if no flowinfo needs to be set for IPv6 address
 * @param[in] scope_id The scope id for an IPv6 address in HOST order.  Set to 0 for IPv4
 *            addresses and for non-scoped IPv6 addresses
 * @param[in] sock The socket that this address will be used with.  
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 *
 * @note Added for IPv6 support.
 */
I_32 VMCALL
hysock_sockaddr_init6 (struct HyPortLibrary * portLibrary,
		       hysockaddr_t handle, U_8 * addr, I_32 addrlength,
		       I_16 family, U_16 nPort, U_32 flowinfo, U_32 scope_id,
		       hysocket_t sock)
{
  OSSOCKADDR *sockaddr;
  OSSOCKADDR_IN6 *sockaddr_6;

  memset (handle, 0, sizeof (struct hysockaddr_struct));

  if (family == HYADDR_FAMILY_AFINET4)
    {
      sockaddr = (OSSOCKADDR *) & handle->addr;
      memcpy (&sockaddr->sin_addr.s_addr, addr, addrlength);
      sockaddr->sin_port = nPort;
      sockaddr->sin_family = OS_AF_INET4;
    }
  else if (family == HYADDR_FAMILY_AFINET6)
    {
      sockaddr_6 = (OSSOCKADDR_IN6 *) & handle->addr;
      memcpy (&sockaddr_6->sin6_addr.s6_addr, addr, addrlength);
      sockaddr_6->sin6_port = nPort;
      sockaddr_6->sin6_family = OS_AF_INET6;
      sockaddr_6->sin6_scope_id = scope_id;
      sockaddr_6->sin6_flowinfo = htonl (flowinfo);
    }
  else
    {
      sockaddr = (OSSOCKADDR *) & handle->addr;
      sockaddr->sin_family = map_addr_family_Hy_to_OS (family);
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_sockaddr_port
/**
 * Answer the port, in network order, of the hysockaddr argument.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the hysockaddr struct to access.
 *
 * @return	the port (there is no validation on the access).
 */
U_16 VMCALL
hysock_sockaddr_port (struct HyPortLibrary * portLibrary, hysockaddr_t handle)
{
  if (((OSSOCKADDR *) & handle->addr)->sin_family == OS_AF_INET4)
    {
      return ((OSSOCKADDR *) & handle->addr)->sin_port;
    }
  else
    {
      return ((OSSOCKADDR_IN6 *) & handle->addr)->sin6_port;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_socket
/**
 * Creates a new socket descriptor and any related resources.
 *
 * @param[in] portLibrary The port library.
 * @param[out]	handle Pointer pointer to the hysocket struct, to be allocated
 * @param[in] family The address family (currently, only HYSOCK_AFINET is supported)
 * @param[in] socktype Secifies what type of socket is created
 * \arg HYSOCK_STREAM, for a stream socket
 * \arg HYSOCK_DGRAM, for a datagram socket
 * @param[in] protocol Type/family specific creation parameter (currently, only HYSOCK_DEFPROTOCOL supported).
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_socket (struct HyPortLibrary * portLibrary, hysocket_t * handle,
	       I_32 family, I_32 socktype, I_32 protocol)
{
  I_32 rc = 0;
  OSSOCKET ipv4 = INVALID_SOCKET;
  OSSOCKET ipv6 = INVALID_SOCKET;

  /* Initialize the handle to invalid */
  *handle = (hysocket_t) - 1;

  if (family != HYADDR_FAMILY_AFINET6 && family != HYADDR_FAMILY_AFINET4
      && family != HYADDR_FAMILY_UNSPEC)
    {
      rc = HYPORT_ERROR_SOCKET_BADAF;
    }
  if ((socktype != HYSOCK_STREAM) && (socktype != HYSOCK_DGRAM))
    {
      rc = HYPORT_ERROR_SOCKET_BADTYPE;
    }
  if (protocol != HYSOCK_DEFPROTOCOL)
    {
      rc = HYPORT_ERROR_SOCKET_BADPROTO;
    }
  if (rc == 0)
    {
      if (family == HYADDR_FAMILY_AFINET4 || family == HYADDR_FAMILY_UNSPEC)
	{
	  ipv4 =
	    socket (AF_INET,
		    ((socktype == HYSOCK_STREAM) ? SOCK_STREAM : SOCK_DGRAM),
		    0);
	  if (ipv4 == INVALID_SOCKET)
	    {
	      rc = WSAGetLastError ();
	      HYSOCKDEBUG ("<socket failed, err=%d>\n", rc);
	      switch (rc)
		{
		case WSAENOBUFS:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       HYPORT_ERROR_SOCKET_SYSTEMFULL);
		  break;
		default:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       findError (rc));
		}
	    }
	}
      /* should place a check here to see if IPv6 is actually installed/running, otherwise
       * socket() returns with error WSAAFNOSUPPORT 10047 - address family not supported */
      if (rc == 0
	  && (family == HYADDR_FAMILY_AFINET6
	      || family == HYADDR_FAMILY_UNSPEC))
	{
	  ipv6 =
	    socket (AF_INET6,
		    ((socktype == HYSOCK_STREAM) ? SOCK_STREAM : SOCK_DGRAM),
		    0);
	  if (ipv6 == INVALID_SOCKET)
	    {
	      rc = WSAGetLastError ();
	      HYSOCKDEBUG ("<socket failed, err=%d>\n", rc);
	      switch (rc)
		{
		case WSAENOBUFS:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       HYPORT_ERROR_SOCKET_SYSTEMFULL);
		  break;
		case WSAEAFNOSUPPORT:
		  /* should deal with this earlier by placing a check before the socket( ) call
		   * to see if IPv6 is running or not */
		  rc = 0;
		  break;
		default:
		  rc =
		    portLibrary->error_set_last_error (portLibrary, rc,
						       findError (rc));
		}
	    }
	}
    }

  if (rc == 0)
    {
      *handle =
	portLibrary->mem_allocate_memory (portLibrary,
					  sizeof (struct hysocket_struct));

      /* Initialize the new structure to show that the IPv4 structure is to be used, and the 2 sockets are invalid */
      (*handle)->ipv4 = INVALID_SOCKET;
      (*handle)->ipv6 = INVALID_SOCKET;
      (*handle)->flags = SOCKET_USE_IPV4_MASK;

      if (ipv4 != INVALID_SOCKET)
	{
	  /* adjust flags to show IPv4 socket is open for business */
	  (*handle)->flags = (*handle)->flags | SOCKET_IPV4_OPEN_MASK;
	  (*handle)->ipv4 = ipv4;
	}

      if (ipv6 != INVALID_SOCKET)
	{
	  (*handle)->ipv6 = ipv6;
	  if (family == HYADDR_FAMILY_AFINET6)
	    {
	      /* set flags to show use IPV6 and IPv6 socket open */
	      (*handle)->flags =
		~SOCKET_USE_IPV4_MASK & SOCKET_IPV6_OPEN_MASK;
	    }
	  else
	    {
	      /* adjust flags to show IPv6 is open for business */
	      (*handle)->flags = (*handle)->flags | SOCKET_IPV6_OPEN_MASK;
	    }
	}
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_socketIsValid
/**
 * Determines whether or not the socket is valid.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Pointer to the hysocket struct, to be allocated.
 *
 * @return	0 if invalid, non-zero for valid.
 */
I_32 VMCALL
hysock_socketIsValid (struct HyPortLibrary * portLibrary, hysocket_t handle)
{
  if (handle == (void *) NULL || handle == (void *) INVALID_SOCKET)
    {
      return FALSE;
    }

  /* If either socket is open, then return TRUE, otherwise return FALSE */
  return handle->flags & SOCKET_BOTH_OPEN_MASK;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_startup

/**
 * Initiate the use of sockets by a process.  This function must be called before any other socket calls.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_SOCK
 * \arg HYPORT_ERROR_SOCKET_OPFAILED
 * \arg HYPORT_ERROR_SOCKET_NOTINITIALIZED
 */
I_32 VMCALL
hysock_startup (struct HyPortLibrary * portLibrary)
{
  I_32 rc = 0;
  WSADATA wsaData;

  PPG_sock_IPv6_FUNCTION_SUPPORT = 1;
  /* On windows we need to figure out if we have IPv6 support functions. 
     We set a flag to indicate which set of functions to use.  IPv6 API
     or IPv4 API */
  if (WSAStartup (MAKEWORD (2, 2), &wsaData) != 0)
    {
      if (WSAStartup (MAKEWORD (1, 1), &wsaData) != 0)
	{
	  rc = WSAGetLastError ();
	  HYSOCKDEBUG ("<WSAStartup() failed, err=%d>", rc);
	  rc =
	    portLibrary->error_set_last_error (portLibrary, rc,
					       HYPORT_ERROR_SOCKET_NOTINITIALIZED);
	}
      PPG_sock_IPv6_FUNCTION_SUPPORT = 0;
    }
  else if (LOBYTE (wsaData.wVersion) == 1)
    {
      PPG_sock_IPv6_FUNCTION_SUPPORT = 0;
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_timeval_init
/**
 * Create a time structure, representing the timeout period defined in seconds & microSeconds.
 * Timeval's are used as timeout arguments in the @ref hysock_select function.
 *
 * @param[in] portLibrary The port library.
 * @param[in] secTime The integer component of the timeout value (in seconds).
 * @param[in] uSecTime The fractional component of the timeout value (in microseconds).
 * @param[out] timeP Pointer pointer to the hytimeval_struct to be allocated.
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_timeval_init (struct HyPortLibrary * portLibrary, U_32 secTime,
		     U_32 uSecTime, hytimeval_t timeP)
{
  memset (timeP, 0, sizeof (struct hytimeval_struct));
  timeP->time.tv_sec = secTime;
  timeP->time.tv_usec = uSecTime;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_write
/**
 * The write function writes data to a connected socket.  The successful completion of a write 
 * does not indicate that the data was successfully delivered.  If no buffer space is available 
 * within the transport system to hold the data to be transmitted, send will block.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Pointer to the socket to send on
 * @param[in] buf The bytes to be sent
 * @param[in] nbyte The number of bytes to send
 * @param[in] flags The flags to modify the send behavior
 *
 * @return	If no error occur, return the total number of bytes sent, which can be less than the 
 * 'nbyte' for nonblocking sockets, otherwise the (negative) error code
 */
I_32 VMCALL
hysock_write (struct HyPortLibrary * portLibrary, hysocket_t sock, U_8 * buf,
	      I_32 nbyte, I_32 flags)
{
  I_32 rc = 0;
  I_32 bytesSent = 0;

  if (sock->flags & SOCKET_USE_IPV4_MASK)
    {
      bytesSent = send (sock->ipv4, (char *) buf, nbyte, flags);
    }
  else
    {
      bytesSent = send (sock->ipv6, (char *) buf, nbyte, flags);
    }
  if (SOCKET_ERROR == bytesSent)
    {
      rc = WSAGetLastError ();
      HYSOCKDEBUG ("<send failed, err=%d>\n", rc);
      switch (rc)
	{
	case WSAEINVAL:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    HYPORT_ERROR_SOCKET_NOTBOUND);
	default:
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}
    }
  else
    {
      rc = bytesSent;
    }
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_writeto
/**
 * The writeto function writes data to a datagram socket.  The successful completion of a writeto
 * does not indicate that the data was successfully delivered.  If no buffer space is available 
 * within the transport system to hold the data to be transmitted, writeto will block.
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock Pointer to the socket to send on
 * @param[in] buf The bytes to be sent
 * @param[in] nbyte The number of bytes to send
 * @param[in] flags The flags to modify the send behavior
 * @param [in] addrHandle The network address to send the datagram to
 *
 * @return	If no error occur, return the total number of bytes sent, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_writeto (struct HyPortLibrary * portLibrary, hysocket_t sock,
		U_8 * buf, I_32 nbyte, I_32 flags, hysockaddr_t addrHandle)
{
  I_32 bytesSent = 0;

  if (((((OSSOCKADDR *) & addrHandle->addr)->sin_family == OS_AF_INET4)
       && (sock->flags & SOCKET_IPV4_OPEN_MASK))
      || ((((OSSOCKADDR *) & addrHandle->addr)->sin_family == OS_AF_INET6)
	  && !(sock->flags & SOCKET_IPV6_OPEN_MASK)))
    {
      bytesSent =
	sendto (sock->ipv4, (char *) buf, nbyte, flags,
		(const struct sockaddr *) &(addrHandle->addr),
		sizeof (addrHandle->addr));
    }
  else
    {
      bytesSent =
	sendto (sock->ipv6, (char *) buf, nbyte, flags,
		(const struct sockaddr *) &(addrHandle->addr),
		sizeof (addrHandle->addr));
    }

  if (SOCKET_ERROR == bytesSent)
    {
      I_32 errorCode = WSAGetLastError ();

      HYSOCKDEBUG ("<sendto failed, err=%d>\n", errorCode);
      return portLibrary->error_set_last_error (portLibrary, errorCode,
						findError (errorCode));
    }
  else
    {
      return bytesSent;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_get_network_interfaces
/**
 * Queries and returns the information for the network interfaces that are currently active within the system. 
 * Applications are responsible for freeing the memory returned via the handle.
 *
 * @param[in] portLibrary The port library.
 * @param[in,out] array Pointer to structure with array of network interface entries
 * @param[in] preferIPv4Stack boolean which indicates if we should prefer the IPv4 stack or not
 *
 * @return The number of elements in handle on success, negatvie portable error code on failure.
 *          -WSANO_RECOVERY if system calls required to get the info fail, -WSAENOBUFS if memory allocation fails
 * @note A return value of 0 indicates no interfaces exist
*/
I_32 VMCALL
hysock_get_network_interfaces (struct HyPortLibrary * portLibrary,
			       struct hyNetworkInterfaceArray_struct * array,
			       BOOLEAN preferIPv4Stack)
{
  U_32 numAdapters = 0;
  U_32 numAddresses = 0;
  DWORD returnVal = 0;
  ULONG bufferLength = 0;
  U_32 nameLength = 0;
  U_32 currentAdapterIndex = 0;
  U_32 currentIPAddressIndex = 0;
  U_32 counter = 0;
  U_32 pseudoLoopbackFound = 0;
  U_32 numLoopbackAddresses = LOOP_BACK_NUM_ADDRESSES;
  struct hyNetworkInterface_struct *interfaces = NULL;
  GetAdaptersAddressesFunctionAddress getAdaptersAddresses_functionAddress =
    NULL;
  HINSTANCE libInstance = NULL;

  /* validate that we were passed the required structure to return network interfaces */
  if (NULL == array)
    {
      return HYPORT_ERROR_SOCKET_NORECOVERY;
    }
  else
    {
      /* initialize the structure so that freeing it would not a cause an issue */
      array->length = 0;
      array->elements = NULL;
    }

  /* try to load the function that gives us the IPV6 info.  If it is not available on this platform then 
   * we will default to using the other function which does not give us the IPV6 info but is available on earlier platforms 
   */
  libInstance = LoadLibrary (TEXT ("Iphlpapi"));
  if (NULL != libInstance)
    {
      getAdaptersAddresses_functionAddress =
	(GetAdaptersAddressesFunctionAddress) GetProcAddress (libInstance,
							      TEXT
							      ("GetAdaptersAddresses"));
    }

  /* only use the function that returns the IPV6 info if we could load the function and we
   * have not been told to prefer the IPv4 stack */
  if ((NULL != getAdaptersAddresses_functionAddress) && (!preferIPv4Stack))
    {
      /* For the functions to get network interface information that are supported on the
       * IPV6 platforms, the loopback interface is included in the list */

      IP_ADAPTER_ADDRESSES *adaptersList = NULL;
      IP_ADAPTER_ADDRESSES *currentAdapter = NULL;
      IP_ADAPTER_ADDRESSES *tempAdapter = NULL;
      IP_ADAPTER_UNICAST_ADDRESS *currentIPAddress = NULL;

      /* get the required buffer size and allocate the memory required */
      returnVal =
	(getAdaptersAddresses_functionAddress) (AF_UNSPEC,
						(GAA_FLAG_SKIP_ANYCAST |
						 GAA_FLAG_SKIP_MULTICAST |
						 GAA_FLAG_SKIP_DNS_SERVER),
						NULL, adaptersList,
						&bufferLength);

      /* GetAdaptorsInfo fails if there are no adaptors configured,  
         so just return 0, the structure is allready initialized to 
         indicate that there are no interfaces */
      if (returnVal == ERROR_BUFFER_OVERFLOW)
	{
	  /* this is ok since we did not pass in a buffer */
	}
      else
	{
	  if (libInstance != NULL)
	    {
	      FreeLibrary (libInstance);
	    }
	  return 0;
	}

      adaptersList =
	(IP_ADAPTER_ADDRESSES *) portLibrary->
	mem_allocate_memory (portLibrary, bufferLength);
#if defined(VALIDATE_ALLOCATIONS)
      if (NULL == adaptersList)
	{
	  if (libInstance != NULL)
	    {
	      FreeLibrary (libInstance);
	    }
	  return HYPORT_ERROR_SOCKET_NOBUFFERS;
	}
#endif

      /* now get the actual adaptor information  and the fill in the hyNetworkInterface_struct */
      if ((returnVal =
	   (getAdaptersAddresses_functionAddress) (AF_UNSPEC,
						   (GAA_FLAG_SKIP_ANYCAST |
						    GAA_FLAG_SKIP_MULTICAST |
						    GAA_FLAG_SKIP_DNS_SERVER),
						   NULL, adaptersList,
						   &bufferLength) ==
	   ERROR_SUCCESS))
	{

	  /* first get the number of adaptors */
	  currentAdapter = adaptersList;
	  while (currentAdapter)
	    {
	      /* on windows to be compatible we don't return the Loopback Pseudo Interface we just merge its addresses into
	         the loopback interface with the IPv4 loopback address */
	      if (strcmp (currentAdapter->AdapterName, pseudoLoopbackGUID) !=
		  0)
		{
		  numAdapters = numAdapters + 1;
		}
	      currentAdapter = currentAdapter->Next;
	    }

	  /* now allocate the space for the hyNetworkInterface structs and fill it in */
	  interfaces =
	    portLibrary->mem_allocate_memory (portLibrary,
					      numAdapters *
					      sizeof
					      (hyNetworkInterface_struct));
#if defined(VALIDATE_ALLOCATIONS)
	  if (NULL == interfaces)
	    {
	      portLibrary->mem_free_memory (portLibrary, adaptersList);
	      /* free the dll we don't need anymore */
	      if (libInstance != NULL)
		{
		  FreeLibrary (libInstance);
		}
	      return HYPORT_ERROR_SOCKET_NOBUFFERS;
	    }
	  /* initialize the structure so that we can free allocated if a failure occurs */
	  for (counter = 0; counter < numAdapters; counter++)
	    {
	      interfaces[counter].name = NULL;
	      interfaces[counter].displayName = NULL;
	      interfaces[counter].addresses = NULL;
	    }
#endif

	  /* set up the return stucture */
	  array->elements = interfaces;
	  array->length = numAdapters;

	  currentAdapter = adaptersList;
	  while (currentAdapter)
	    {
	      /* on windows to be compatible we don't return the Loopback Pseudo Interface we just merge
	       * its addresses into the loopback interface with the IPv4 loopback address.  Note the name
	       * should never be null but we do the check to be safe  */
	      if ((currentAdapter->AdapterName != NULL) &&
		  (strcmp (currentAdapter->AdapterName, pseudoLoopbackGUID) !=
		   0))
		{

		  /* set the index for the interface */
		  interfaces[currentAdapterIndex].index =
		    currentAdapter->Ipv6IfIndex;

		  /* get the name and display name for the adapter */
		  nameLength = strlen (currentAdapter->AdapterName);
		  interfaces[currentAdapterIndex].name =
		    portLibrary->mem_allocate_memory (portLibrary,
						      nameLength + 1);
#if defined(VALIDATE_ALLOCATIONS)
		  if (NULL == interfaces[currentAdapterIndex].name)
		    {
		      portLibrary->mem_free_memory (portLibrary,
						    adaptersList);
		      hysock_free_network_interface_struct (portLibrary,
							    array);
		      /* free the dll we don't need anymore */
		      if (libInstance != NULL)
			{
			  FreeLibrary (libInstance);
			}
		      return HYPORT_ERROR_SOCKET_NOBUFFERS;
		    }
#endif

		  strncpy (interfaces[currentAdapterIndex].name,
			   currentAdapter->AdapterName, nameLength);
		  interfaces[currentAdapterIndex].name[nameLength] = 0;
		  nameLength =
		    WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK,
					 currentAdapter->FriendlyName, -1,
					 NULL, 0, NULL, NULL);

		  if (nameLength != 0)
		    {
		      interfaces[currentAdapterIndex].displayName =
			portLibrary->mem_allocate_memory (portLibrary,
							  nameLength + 1);
		    }
		  else
		    {
		      interfaces[currentAdapterIndex].displayName =
			portLibrary->mem_allocate_memory (portLibrary, 1);
		      interfaces[currentAdapterIndex].displayName[0] = 0;
		    }
#if defined(VALIDATE_ALLOCATIONS)
		  if (NULL == interfaces[currentAdapterIndex].displayName)
		    {
		      portLibrary->mem_free_memory (portLibrary,
						    adaptersList);
		      hysock_free_network_interface_struct (portLibrary,
							    array);
		      /* free the dll we don't need anymore */
		      if (libInstance != NULL)
			{
			  FreeLibrary (libInstance);
			}
		      return HYPORT_ERROR_SOCKET_NOBUFFERS;
		    }
#endif

		  if (nameLength != 0)
		    {
		      WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK,
					   currentAdapter->FriendlyName, -1,
					   interfaces[currentAdapterIndex].
					   displayName, nameLength, NULL,
					   NULL);
		      interfaces[currentAdapterIndex].
			displayName[nameLength] = 0;
		    }

		  /* now get the interface information */

		  /* first count the number of IP addreses and allocate the memory required
		   * for the ip address info that will be returned */
		  numAddresses = 0;
		  currentIPAddress = currentAdapter->FirstUnicastAddress;
		  while (currentIPAddress)
		    {
		      numAddresses = numAddresses + 1;
		      currentIPAddress = currentIPAddress->Next;
		    }

		  /* if this is the loopback address then we need to add the addresses from
		   * the Loopback Pseudo-Interface */
		  pseudoLoopbackFound = 0;
		  if ((NULL != currentAdapter->FirstUnicastAddress) &&
		      (AF_INET ==
		       ((struct sockaddr_in *) (currentAdapter->
						FirstUnicastAddress->Address.
						lpSockaddr))->sin_family)
		      && (127 ==
			  ((struct sockaddr_in *) (currentAdapter->
						   FirstUnicastAddress->
						   Address.lpSockaddr))->
			  sin_addr.S_un.S_un_b.s_b1))
		    {
		      /* find the pseudo interface and get the first unicast address */
		      tempAdapter = adaptersList;
		      pseudoLoopbackFound = 0;
		      while ((tempAdapter) && (0 == pseudoLoopbackFound))
			{
			  if (strcmp
			      (tempAdapter->AdapterName,
			       pseudoLoopbackGUID) == 0)
			    {
			      pseudoLoopbackFound = 1;
			    }
			  else
			    {
			      tempAdapter = tempAdapter->Next;
			    }
			}

		      if (1 == pseudoLoopbackFound)
			{
			  /* now if we found the adapter add the count for the addresses on it */
			  currentIPAddress = tempAdapter->FirstUnicastAddress;
			  while (currentIPAddress)
			    {
			      numAddresses = numAddresses + 1;
			      currentIPAddress = currentIPAddress->Next;
			    }

			  /* also if we found the pseudo interface we must have to use the interface
			   * id associated with this interface */
			  interfaces[currentAdapterIndex].index =
			    tempAdapter->Ipv6IfIndex;
			}
		    }

		  interfaces[currentAdapterIndex].addresses =
		    portLibrary->mem_allocate_memory (portLibrary,
						      numAddresses *
						      sizeof
						      (hyipAddress_struct));
#if defined(VALIDATE_ALLOCATIONS)
		  if (NULL == interfaces[currentAdapterIndex].addresses)
		    {
		      portLibrary->mem_free_memory (portLibrary,
						    adaptersList);
		      hysock_free_network_interface_struct (portLibrary,
							    array);
		      /* free the dll we don't need anymore */
		      if (libInstance != NULL)
			{
			  FreeLibrary (libInstance);
			}
		      return HYPORT_ERROR_SOCKET_NOBUFFERS;
		    }
#endif

		  interfaces[currentAdapterIndex].numberAddresses =
		    numAddresses;

		  /* now get the actual ip address info */
		  currentIPAddressIndex = 0;
		  currentIPAddress = currentAdapter->FirstUnicastAddress;
		  while (currentIPAddress)
		    {
		      if (currentIPAddress->Address.iSockaddrLength ==
			  sizeof (struct sockaddr_in6))
			{
			  memcpy (interfaces[currentAdapterIndex].
				  addresses[currentIPAddressIndex].addr.bytes,
				  &(((struct sockaddr_in6 *)
				     currentIPAddress->Address.lpSockaddr)->
				    sin6_addr.u.Byte),
				  sizeof (struct in6_addr));
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].length =
			    sizeof (struct in6_addr);
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].scope =
			    ((struct sockaddr_in6 *) currentIPAddress->
			     Address.lpSockaddr)->sin6_scope_id;
			}
		      else if (currentIPAddress->Address.iSockaddrLength ==
			       sizeof (struct sockaddr_in6_old))
			{
			  memcpy (interfaces[currentAdapterIndex].
				  addresses[currentIPAddressIndex].addr.bytes,
				  &(((struct sockaddr_in6_old *)
				     currentIPAddress->Address.lpSockaddr)->
				    sin6_addr.u.Byte),
				  sizeof (struct in6_addr));
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].length =
			    sizeof (struct in6_addr);
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].scope = 0;
			}
		      else
			{
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].addr.inAddr.S_un.
			    S_addr =
			    ((struct sockaddr_in *) currentIPAddress->Address.
			     lpSockaddr)->sin_addr.S_un.S_addr;
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].length =
			    sizeof (struct in_addr);
			  interfaces[currentAdapterIndex].
			    addresses[currentIPAddressIndex].scope = 0;
			}

		      currentIPAddress = currentIPAddress->Next;
		      currentIPAddressIndex = currentIPAddressIndex + 1;
		    }

		  /* now add in the addresses from the loopback pseudo-interface if appropriate */
		  if (1 == pseudoLoopbackFound)
		    {
		      currentIPAddress = tempAdapter->FirstUnicastAddress;
		      while (currentIPAddress)
			{
			  if (currentIPAddress->Address.iSockaddrLength ==
			      sizeof (struct sockaddr_in6))
			    {
			      memcpy (interfaces[currentAdapterIndex].
				      addresses[currentIPAddressIndex].addr.
				      bytes,
				      &(((struct sockaddr_in6 *)
					 currentIPAddress->Address.
					 lpSockaddr)->sin6_addr.u.Byte),
				      sizeof (struct in6_addr));
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].length =
				sizeof (struct in6_addr);
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].scope =
				((struct sockaddr_in6 *) currentIPAddress->
				 Address.lpSockaddr)->sin6_scope_id;
			    }
			  else if (currentIPAddress->Address.
				   iSockaddrLength ==
				   sizeof (struct sockaddr_in6_old))
			    {
			      memcpy (interfaces[currentAdapterIndex].
				      addresses[currentIPAddressIndex].addr.
				      bytes,
				      &(((struct sockaddr_in6_old *)
					 currentIPAddress->Address.
					 lpSockaddr)->sin6_addr.u.Byte),
				      sizeof (struct in6_addr));
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].length =
				sizeof (struct in6_addr);
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].scope = 0;
			    }
			  else
			    {
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].addr.inAddr.
				S_un.S_addr =
				((struct sockaddr_in *) currentIPAddress->
				 Address.lpSockaddr)->sin_addr.S_un.S_addr;
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].length =
				sizeof (struct in_addr);
			      interfaces[currentAdapterIndex].
				addresses[currentIPAddressIndex].scope = 0;
			    }

			  currentIPAddress = currentIPAddress->Next;
			  currentIPAddressIndex = currentIPAddressIndex + 1;
			}
		    }
		  currentAdapterIndex = currentAdapterIndex + 1;
		}		/* not adaptor to exlude */
	      currentAdapter = currentAdapter->Next;
	    }

	  /* free the memory used for the call to the getAdaptorAddresses info call */
	  portLibrary->mem_free_memory (portLibrary, adaptersList);

	  /* free the dll we don't need anymore */
	  if (libInstance != NULL)
	    {
	      FreeLibrary (libInstance);
	    }

	  /* return OK */
	  return 0;

	}
      else
	{
	  /* GetAdaptorsInfo fails if there are no adaptors configured, so just return 0, the structure is
	   * already initialized to indicate that there are no interfaces */

	  /* free the dll we don't need anymore */
	  if (libInstance != NULL)
	    {
	      FreeLibrary (libInstance);
	    }

	  if (returnVal == ERROR_BUFFER_OVERFLOW)
	    {
	      return HYPORT_ERROR_SOCKET_NOBUFFERS;
	    }
	  else
	    {
	      return 0;
	    }
	}
    }
  else

    {
      /* For the functions to get network interface information that are supported on the pre- IPV6
         platforms, the loopback interface is NOT included in the list.  Therefore, we have to add it ourselves */

      IP_ADAPTER_INFO *adaptersList = NULL;
      IP_ADAPTER_INFO *currentAdapter = NULL;
      IP_ADDR_STRING *currentIPAddress = NULL;

      /* get the required buffer size and allocate the memory required */
      returnVal = GetAdaptersInfo (adaptersList, &bufferLength);

      /* GetAdaptorsInfo fails if there are no adaptors configured, so just return 0, the structure is
       * already initialized to indicate that there are no interfaces */
      if (returnVal != ERROR_SUCCESS)
	{
	  if (returnVal == ERROR_NO_DATA)
	    {
	      numAdapters = 0;
	      bufferLength = 0;
	      adaptersList = NULL;
	    }
	  else if (returnVal == ERROR_BUFFER_OVERFLOW)
	    {
	      /*  this is ok since we did not pass in a buffer */
	    }
	  else
	    {
	      return HYPORT_ERROR_SOCKET_NORECOVERY;
	    }
	}

      if (bufferLength != 0)
	{
	  adaptersList =
	    (IP_ADAPTER_INFO *) portLibrary->mem_allocate_memory (portLibrary,
								  bufferLength);
#if defined(VALIDATE_ALLOCATIONS)
	  if (NULL == adaptersList)
	    {
	      return HYPORT_ERROR_SOCKET_NOBUFFERS;
	    }
#endif

	  /* now get the actual adaptor information  and the fill in the hyNetworkInterface_struct */
	  if ((returnVal =
	       GetAdaptersInfo (adaptersList,
				&bufferLength)) == ERROR_SUCCESS)
	    {

	      /* first get the number of adaptors */
	      currentAdapter = adaptersList;
	      while (currentAdapter)
		{
		  currentAdapter = currentAdapter->Next;
		  numAdapters = numAdapters + 1;
		}
	    }
	  else
	    {
	      numAdapters = 0;
	      if (returnVal == ERROR_NO_DATA)
		{
		  bufferLength = 0;
		  adaptersList = NULL;
		}
	      else if (returnVal == ERROR_BUFFER_OVERFLOW)
		{
		  return HYPORT_ERROR_SOCKET_NOBUFFERS;
		}
	      else
		{
		  return HYPORT_ERROR_SOCKET_NORECOVERY;
		}
	    }
	}

      /* now allocate the space for the hyNetworkInterface structs and fill it in */
      /* allow space for one more than was returned as the system call does not include the loopback interface
         which we must add */
      interfaces =
	portLibrary->mem_allocate_memory (portLibrary,
					  (numAdapters +
					   1) *
					  sizeof (hyNetworkInterface_struct));
#if defined(VALIDATE_ALLOCATIONS)
      if (NULL == interfaces)
	{
	  portLibrary->mem_free_memory (portLibrary, adaptersList);
	  return HYPORT_ERROR_SOCKET_NOBUFFERS;
	}
      /* initialize the structure so that we can free allocated if a failure occurs */
      for (counter = 0; counter < (numAdapters + 1); counter++)
	{
	  interfaces[counter].name = NULL;
	  interfaces[counter].displayName = NULL;
	  interfaces[counter].addresses = NULL;
	}
#endif

      // set up the return stucture
      array->elements = interfaces;
      array->length = numAdapters + 1;
      currentAdapter = adaptersList;
      while (currentAdapter)
	{
	  /* set the index to 0 as for non-IPV6 we don't fill in this value */
	  interfaces[currentAdapterIndex].index = 0;

	  /* get the name and display name for the adapter */
	  nameLength = strlen (currentAdapter->AdapterName);
	  interfaces[currentAdapterIndex].name =
	    portLibrary->mem_allocate_memory (portLibrary, nameLength + 1);
#if defined(VALIDATE_ALLOCATIONS)
	  if (NULL == interfaces[currentAdapterIndex].name)
	    {
	      portLibrary->mem_free_memory (portLibrary, adaptersList);
	      hysock_free_network_interface_struct (portLibrary, array);
	      return HYPORT_ERROR_SOCKET_NOBUFFERS;
	    }
#endif

	  strncpy (interfaces[currentAdapterIndex].name,
		   currentAdapter->AdapterName, nameLength);
	  interfaces[currentAdapterIndex].name[nameLength] = 0;

	  nameLength = strlen (currentAdapter->Description);
	  interfaces[currentAdapterIndex].displayName =
	    portLibrary->mem_allocate_memory (portLibrary, nameLength + 1);
#if defined(VALIDATE_ALLOCATIONS)
	  if (NULL == interfaces[currentAdapterIndex].displayName)
	    {
	      portLibrary->mem_free_memory (portLibrary, adaptersList);
	      hysock_free_network_interface_struct (portLibrary, array);
	      return HYPORT_ERROR_SOCKET_NOBUFFERS;
	    }
#endif

	  strncpy (interfaces[currentAdapterIndex].displayName,
		   currentAdapter->Description, nameLength);
	  interfaces[currentAdapterIndex].displayName[nameLength] = 0;

	  /* now get the interface information */

	  /* first count the number of IP addreses and allocate the memory required for
	   * the ip address info that will be returned */
	  numAddresses = 0;
	  currentIPAddress = &(currentAdapter->IpAddressList);
	  while (currentIPAddress)
	    {
	      /* don't count the any address which seems to be returned as the first address
	       * for interfaces with no addresses */
	      if (inet_addr (currentIPAddress->IpAddress.String) != 0)
		{
		  numAddresses = numAddresses + 1;
		}
	      currentIPAddress = currentIPAddress->Next;
	    }
	  interfaces[currentAdapterIndex].addresses =
	    portLibrary->mem_allocate_memory (portLibrary,
					      numAddresses *
					      sizeof (hyipAddress_struct));
#if defined(VALIDATE_ALLOCATIONS)
	  if (NULL == interfaces[currentAdapterIndex].addresses)
	    {
	      portLibrary->mem_free_memory (portLibrary, adaptersList);
	      hysock_free_network_interface_struct (portLibrary, array);
	      return HYPORT_ERROR_SOCKET_NOBUFFERS;
	    }
#endif

	  interfaces[currentAdapterIndex].numberAddresses = numAddresses;

	  /* now get the actual ip address info */
	  currentIPAddressIndex = 0;
	  currentIPAddress = &(currentAdapter->IpAddressList);
	  while (currentIPAddress)
	    {
	      if (inet_addr (currentIPAddress->IpAddress.String) != 0)
		{
		  interfaces[currentAdapterIndex].
		    addresses[currentIPAddressIndex].addr.inAddr.S_un.S_addr =
		    inet_addr (currentIPAddress->IpAddress.String);
		  interfaces[currentAdapterIndex].
		    addresses[currentIPAddressIndex].length =
		    sizeof (struct in_addr);
		  interfaces[currentAdapterIndex].
		    addresses[currentIPAddressIndex].scope = 0;
		  currentIPAddressIndex = currentIPAddressIndex + 1;
		}
	      currentIPAddress = currentIPAddress->Next;
	    }

	  currentAdapter = currentAdapter->Next;
	  currentAdapterIndex = currentAdapterIndex + 1;
	}

      /* now fill in the loopback adaptor */
      interfaces[currentAdapterIndex].index = 0;
      nameLength = strlen (LOOP_BACK_NAME);

      interfaces[currentAdapterIndex].name =
	portLibrary->mem_allocate_memory (portLibrary, nameLength + 1);
#if defined(VALIDATE_ALLOCATIONS)
      if (NULL == interfaces[currentAdapterIndex].name)
	{
	  portLibrary->mem_free_memory (portLibrary, adaptersList);
	  hysock_free_network_interface_struct (portLibrary, array);
	  return HYPORT_ERROR_SOCKET_NOBUFFERS;
	}
#endif

      strncpy (interfaces[currentAdapterIndex].name, LOOP_BACK_NAME,
	       nameLength);
      interfaces[currentAdapterIndex].name[nameLength] = 0;

      nameLength = strlen (LOOP_BACK_DISPLAY_NAME);
      interfaces[currentAdapterIndex].displayName =
	portLibrary->mem_allocate_memory (portLibrary, nameLength + 1);
#if defined(VALIDATE_ALLOCATIONS)
      if (NULL == interfaces[currentAdapterIndex].displayName)
	{
	  portLibrary->mem_free_memory (portLibrary, adaptersList);
	  hysock_free_network_interface_struct (portLibrary, array);
	  return HYPORT_ERROR_SOCKET_NOBUFFERS;
	}
#endif

      strncpy (interfaces[currentAdapterIndex].displayName,
	       LOOP_BACK_DISPLAY_NAME, nameLength);
      interfaces[currentAdapterIndex].displayName[nameLength] = 0;

      /* now get interface information */

      interfaces[currentAdapterIndex].addresses =
	portLibrary->mem_allocate_memory (portLibrary,
					  numLoopbackAddresses *
					  sizeof (hyipAddress_struct));
#if defined(VALIDATE_ALLOCATIONS)
      if (NULL == interfaces[currentAdapterIndex].addresses)
	{
	  portLibrary->mem_free_memory (portLibrary, adaptersList);
	  hysock_free_network_interface_struct (portLibrary, array);
	  return HYPORT_ERROR_SOCKET_NOBUFFERS;
	}
#endif

      /* now  the actual ip address info */
      interfaces[currentAdapterIndex].numberAddresses = numLoopbackAddresses;
      interfaces[currentAdapterIndex].addresses[0].addr.inAddr.S_un.S_addr =
	inet_addr (LOOP_BACK_IPV4_ADDRESS);
      interfaces[currentAdapterIndex].addresses[0].length =
	sizeof (struct in_addr);
      interfaces[currentAdapterIndex].addresses[0].scope = 0;

      /* free the memory used for the call to the getAdaptors info call */
      if (bufferLength != 0)
	{
	  portLibrary->mem_free_memory (portLibrary, adaptersList);
	}

      /* return ok */
      return 0;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_free_network_interface_struct
/**
 * Frees the memory allocated for the hyNetworkInterface_struct array passed in
 *
 * @param[in] portLibrary The port library.
 * @param[in] array Pointer to array of network interface structures to be freed
 *
 * @return 0 on success
*/
I_32 VMCALL
hysock_free_network_interface_struct (struct HyPortLibrary * portLibrary,
				      struct hyNetworkInterfaceArray_struct * array)
{
  U_32 i = 0;

  if ((array != NULL) && (array->elements != NULL))
    {
      /* free the allocated memory in each of the structures */
      for (i = 0; i < array->length; i++)
	{

	  /* free the name, displayName and addresses */
	  if (array->elements[i].name != NULL)
	    {
	      portLibrary->mem_free_memory (portLibrary,
					    array->elements[i].name);
	    }

	  if (array->elements[i].displayName != NULL)
	    {
	      portLibrary->mem_free_memory (portLibrary,
					    array->elements[i].displayName);
	    }

	  if (array->elements[i].addresses != NULL)
	    {
	      portLibrary->mem_free_memory (portLibrary,
					    array->elements[i].addresses);
	    }
	}

      /* now free the array itself */
      portLibrary->mem_free_memory (portLibrary, array->elements);
    }

  return 0;

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysock_connect_with_timeout
/**
 * Establish a connection to a peer with a timeout.  This function is called repeatedly
 * in order to carry out the connect and to allow other tasks to proceed on certain platforms.
 * The caller must first call with step = HY_SOCK_STEP_START, if the result is HY_ERROR_SOCKET_NOTCONNECTED
 * it will then call it with step = CHECK until either another error or 0 is returned to
 * indicate the connect is complete.  Each time the function should sleep for no more than
 * timeout milliseconds.  If the connect succeeds or an error occurs, the caller must always
 * end the process by calling the function with step = HY_SOCK_STEP_DONE
 *
 * @param[in] portLibrary The port library.
 * @param[in] sock pointer to the unconnected local socket.
 * @param[in] addr	pointer to the sockaddr, specifying remote host/port.
 * @param[in] timeout  timeout in milliseconds 
 * @param[in] step 
 * @param[in,out] context pointer to context pointer.  Filled in on first call and then to be passed into each subsequent call
 *
 * @return	0, if no errors occurred, otherwise the (negative) error code.
 */
I_32 VMCALL
hysock_connect_with_timeout (struct HyPortLibrary * portLibrary,
			     hysocket_t sock, hysockaddr_t addr, U_32 timeout,
			     U_32 step, U_8 ** context)
{
  I_32 rc = 0;
  struct timeval passedTimeout;
  int errorVal;
  int errorValLen = sizeof (int);

  if (HY_PORT_SOCKET_STEP_START == step)
    {

      /* initialize the context to a known state */
      if (NULL != context)
	{
	  *context = NULL;
	}
      else
	{
	  /* this should never happen but just in case */
	  return HYPORT_ERROR_SOCKET_NORECOVERY;
	}

      /* we will be looping checking for when we are connected so allocate the descriptor sets that we will use */
      *context =
	(U_8 *) portLibrary->mem_allocate_memory (portLibrary,
						  sizeof (struct
							  selectFDSet_struct));
#if defined(VALIDATE_ALLOCATIONS)
      if (NULL == *context)
	{
	  return HYPORT_ERROR_SOCKET_NOBUFFERS;
	}
#endif

      /* set the socket to non-blocking */
      rc = hysock_set_nonblocking (portLibrary, sock, TRUE);
      if (0 != rc)
	{
	  return rc;
	}

      /* here we need to do the connect based on the type of addressed passed in as well as the sockets which are open. If 
         a socket with a type that matches the type of the address passed in is open then we use that one.  Otherwise we
         use the socket that is open */
      if (((((OSSOCKADDR *) & addr->addr)->sin_family == OS_AF_INET4) ||
	   !(sock->flags & SOCKET_IPV6_OPEN_MASK)) &&
	  (sock->flags & SOCKET_IPV4_OPEN_MASK))
	{
	  rc =
	    connect (sock->ipv4, (const struct sockaddr FAR *) &addr->addr,
		     sizeof (addr->addr));
	  internalCloseSocket (portLibrary, sock, FALSE);
	  ((struct selectFDSet_struct *) *context)->sock = sock->ipv4;
	  ((struct selectFDSet_struct *) *context)->nfds = sock->ipv4 + 1;
	}
      else
	{
	  rc =
	    connect (sock->ipv6, (const struct sockaddr FAR *) &addr->addr,
		     sizeof (addr->addr));
	  internalCloseSocket (portLibrary, sock, TRUE);
	  ((struct selectFDSet_struct *) *context)->sock = sock->ipv6;
	  ((struct selectFDSet_struct *) *context)->nfds = sock->ipv6 + 1;
	}

      if (rc == SOCKET_ERROR)
	{
	  rc = WSAGetLastError ();
	  switch (rc)
	    {
	    case WSAEINVAL:
	      return HYPORT_ERROR_SOCKET_ALREADYBOUND;
	    case WSAEWOULDBLOCK:
	      return HYPORT_ERROR_SOCKET_NOTCONNECTED;
	    default:
	      return portLibrary->error_set_last_error (portLibrary, rc,
							findError (rc));
	    }
	  return rc;
	}

      /* we connected right off the bat so just return */
      return rc;

    }
  else if (HY_PORT_SOCKET_STEP_CHECK == step)
    {
      /* now check if we have connected yet */

      /* set the timeout value to be used.  Just use the full timeout as windows should return from select 
         error if the socket has been returned  */
      passedTimeout.tv_sec = timeout / 1000;
      passedTimeout.tv_usec = (timeout - passedTimeout.tv_sec * 1000) * 1000;

      /* initialize the FD sets for the select */
      FD_ZERO (&(((struct selectFDSet_struct *) *context)->exceptionSet));
      FD_ZERO (&(((struct selectFDSet_struct *) *context)->writeSet));
      FD_ZERO (&(((struct selectFDSet_struct *) *context)->readSet));
      FD_SET (((struct selectFDSet_struct *) *context)->sock,
	      &(((struct selectFDSet_struct *) *context)->writeSet));
      FD_SET (((struct selectFDSet_struct *) *context)->sock,
	      &(((struct selectFDSet_struct *) *context)->exceptionSet));
      FD_SET (((struct selectFDSet_struct *) *context)->sock,
	      &(((struct selectFDSet_struct *) *context)->readSet));

      /* just use the full timeout as windows should give us the appropriate error if the
         socket is closed by another process */
      rc = select (((struct selectFDSet_struct *) *context)->nfds,
		   &(((struct selectFDSet_struct *) *context)->readSet),
		   &(((struct selectFDSet_struct *) *context)->writeSet),
		   &(((struct selectFDSet_struct *) *context)->exceptionSet),
		   &passedTimeout);

      /* if there is at least one descriptor ready to be checked */
      if (0 < rc)
	{

	  /* if the descriptor is in the exception set then the connect failed */
	  if (FD_ISSET
	      (((struct selectFDSet_struct *) *context)->sock,
	       &(((struct selectFDSet_struct *) *context)->exceptionSet)))
	    {
	      if (getsockopt
		  (((struct selectFDSet_struct *) *context)->sock, SOL_SOCKET,
		   SO_ERROR, (char *) &errorVal,
		   &errorValLen) != SOCKET_ERROR)
		{
		  return findError (errorVal);
		}
	      rc = WSAGetLastError ();
	      return portLibrary->error_set_last_error (portLibrary, rc,
							findError (rc));
	    }

	  /* if the descriptor is in the write set then we have connected so return 0 */
	  if (FD_ISSET
	      (((struct selectFDSet_struct *) *context)->sock,
	       &(((struct selectFDSet_struct *) *context)->writeSet)))
	    {
	      return 0;
	    }
	}
      else if (rc == SOCKET_ERROR)
	{
	  /* something went wrong with the select call */
	  rc = WSAGetLastError ();

	  /* if it was WASEINTR then we can just try again so just return not connected */
	  if (WSAEINTR == rc)
	    {
	      return HYPORT_ERROR_SOCKET_NOTCONNECTED;
	    }

	  /* some other error occured so look it up and return */
	  return portLibrary->error_set_last_error (portLibrary, rc,
						    findError (rc));
	}

      /* if we get here the timeout expired or the connect had not yet completed
         just indicate that the connect is not yet complete */
      return HYPORT_ERROR_SOCKET_NOTCONNECTED;
    }
  else if (HY_PORT_SOCKET_STEP_DONE == step)
    {
      /* we are done the connect or an error occured so clean up */

      /* set the socket back to blocking but only if it is not closed */
      if (sock != (hysocket_t)-1)
	{
	  hysock_set_nonblocking (portLibrary, sock, FALSE);
	}

      /* free the memory for the FD set */
      if ((context != NULL) && (*context != NULL))
	{
	  portLibrary->mem_free_memory (portLibrary, *context);
	  *context = NULL;
	}
    }
    
    return 0;
}

#undef CDEV_CURRENT_FUNCTION
