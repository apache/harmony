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
 * @author Intel, Evgueni Brevnov
 */  


#define LOG_DOMAIN "port.old"
#include "cxxlog.h"
#include "platform_lowlevel.h"

#include <stdlib.h>
#include <io.h>
#include <string.h>
#include <process.h>

#include "init.h"
#include "platform_utils.h"
#include "open/vm_util.h"

//The following is for socket error handling
const char *sock_errstr[] = { 
NULL, /* 10000 WSABASEERR */
NULL,
NULL,
NULL,
/*10004 WSAEINTR*/          "(10004) Interrupted by socket close",     
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
/*10013 WSAEACCES*/         "(10013) Permission denied",       
/*10014 WSAEFAULT*/         "(10014) Bad address",       
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
/*10022 WSAEINVAL*/         "(10022) Invalid argument",       
NULL,
/*10024 WSAEMFILE*/         "(10024) Too many open files",     
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
/*10035 WSAEWOULDBLOCK*/    "(10035) Resource temporarily unavailable",      
/*10036 WSAEINPROGRESS*/    "(10036) Operation now in progress",     
/*10037 WSAEALREADY*/       "(10037) Operation already in progress",     
/*10038 WSAENOTSOCK*/       "(10038) Socket operation on non-socket",     
/*10039 WSAEDESTADDRREQ*/   "(10039) Destination address required",      
/*10040 WSAEMSGSIZE*/       "(10040) Message too long",      
/*10041 WSAEPROTOTYPE*/     "(10041) Protocol wrong type for socket",    
/*10042 WSAENOPROTOOPT*/    "(10042) Bad protocol option",      
/*10043 WSAEPROTONOSUPPORT*/"(10043) Protocol not supported",      
/*10044 WSAESOCKTNOSUPPORT*/"(10044) Socket type not supported",     
/*10045 WSAEOPNOTSUPP*/     "(10045) Operation not supported",      
/*10046 WSAEPFNOSUPPORT*/   "(10046) Protocol family not supported",     
/*10047 WSAEAFNOSUPPORT*/   "(10047) Address family not supported by protocol family",  
/*10048 WSAEADDRINUSE*/     "(10048) Address already in use",     
/*10049 WSAEADDRNOTAVAIL*/  "(10049) Cannot assign requested address",     
/*10050 WSAENETDOWN*/       "(10050) Network is down",      
/*10051 WSAENETUNREACH*/    "(10051) Network is unreachable",      
/*10052 WSAENETRESET*/      "(10052) Network dropped connection on reset",    
/*10053 WSAECONNABORTED*/   "(10053) Software caused connection abort",     
/*10054 WSAECONNRESET*/     "(10054) Connection reset by peer",     
/*10055 WSAENOBUFS*/        "(10055) No buffer space available",     
/*10056 WSAEISCONN*/        "(10056) Socket is already connected",     
/*10057 WSAENOTCONN*/       "(10057) Socket is not connected",     
/*10058 WSAESHUTDOWN*/      "(10058) Cannot send after socket shutdown",    
/*10060 WSAETIMEDOUT*/      "(10060) Connection timed out",      
/*10061 WSAECONNREFUSED*/   "(10061) Connection refused",       
NULL,
NULL,
NULL,
/*10064 WSAEHOSTDOWN*/      "(10064) Host is down",      
/*10065 WSAEHOSTUNREACH*/   "(10065) No route to host",     
NULL,
/*10067 WSAEPROCLIM*/       "(10067) Too many processes",      
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
/*10091 WSASYSNOTREADY*/    "(10091) Network subsystem is unavailable",     
/*10092 WSAVERNOTSUPPORTED*/"(10092) WINSOCK.DLL version out of range",    
/*10093 WSANOTINITIALISED*/ "(10093) Successful WSAStartup not yet performed",    
/*10094 WSAEDISCON*/        "(10094) Graceful shutdown in progress",     
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
NULL,
/*10109 WSATYPE_NOT_FOUND*/ "(10109) Class type not found",
};

const char *sock_errstr1[] = { 
/*11001 WSAHOST_NOT_FOUND*/ "(11001) Host not found",      
/*11002 WSATRY_AGAIN*/      "(11002) Non-authoritative host not found",     
/*11003 WSANO_RECOVERY*/    "(11003) This is a non-recoverable error",    
/*11004 WSANO_DATA*/        "(11004) Valid name, no data record of requested type", 
};

const char *socket_strerror(int errcode){
    if(errcode < 10000)
        return NULL;
    if(errcode < 10110)
        return sock_errstr[errcode - 10000];
    if(errcode > 11000 && errcode < 11005)
        return sock_errstr1[errcode - 11001];
    return NULL;
}
