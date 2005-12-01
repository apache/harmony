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
#include <netinet/in.h>         /* for struct in_addr */
#include <sys/ioctl.h>
#include <net/if.h>             /* for struct ifconf */
#endif

#include "portsock.h"

/**
 * Create a new socket, for multicast datagrams.  The system socket is created and 'linked' to the
 * the java PlainMulticastSocketImpl by setting the file descriptor value (which is an integer 
 * reference to socket maintained by the system).  For Multicast sockets, the REUSEADDR is on by
 * default for all platforms tested so far including windows.  In addition on platforms which support REUSEPORT
 * this should also be on by default as well.
 *
 * @param	env				pointer to the JNI library
 * @param	thisClz			pointer to the class of the receiver (of the java message)
 * @param	thisObjFD	pointer to the file descriptor of the java PlainDatagramSocketImpl
 * @param preferIPv4Stack if application preference is to use only IPv4 sockets (default is false)
 */

void JNICALL
Java_java_net_PlainMulticastSocketImpl_createMulticastSocketImpl (JNIEnv *
                                                                  env,
                                                                  jclass
                                                                  thisClz,
                                                                  jobject
                                                                  thisObjFD,
                                                                  jboolean
                                                                  preferIPv4Stack)
{
  PORT_ACCESS_FROM_ENV (env);
  BOOLEAN value = TRUE;
  hysocket_t socketP;
  createSocket (env, thisObjFD, HYSOCK_DGRAM, preferIPv4Stack);
  socketP =
    (hysocket_t) getJavaIoFileDescriptorContentsAsPointer (env, thisObjFD);

  hysock_setopt_bool (socketP, HY_SOL_SOCKET, HY_SO_REUSEPORT, &value);
  hysock_setopt_bool (socketP, HY_SOL_SOCKET, HY_SO_REUSEADDR, &value);
}
