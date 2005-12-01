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

#include "nethelp.h"
#include "jclprots.h"
#include "helpers.h"
#include "jclglob.h"
#include "portsock.h"

/**
 * Answer an array of NetworkInterface objects.  One for each network interface within the system
 *
 * @param env pointer to the JNI library
 * @param clazz the class of the object invoking the JNI function
 *
 * @return      an array of NetworkInterface objects of length 0 or more
 */

jobjectArray JNICALL
Java_java_net_NetworkInterface_getNetworkInterfacesImpl (JNIEnv * env,
               jclass clazz)
{
  /* variables to store network interfac edata returned by call to port library */
  struct hyNetworkInterfaceArray_struct networkInterfaceArray;
  I_32 result = 0;

  /* variables for class and method objects needed to create bridge to java */
  jclass networkInterfaceClass = NULL;
  jclass inetAddressClass = NULL;
  jclass utilClass = NULL;
  jmethodID methodID = NULL;
  jmethodID utilMid = NULL;

  /* JNI objects used to return values from native call */
  jstring name = NULL;
  jstring displayName = NULL;
  jobjectArray addresses = NULL;
  jobjectArray networkInterfaces = NULL;
  jbyteArray bytearray = NULL;

  /* jobjects used to build the object arrays returned */
  jobject currentInterface = NULL;
  jobject element = NULL;

  /* misc variables needed for looping and determining inetAddress info */
  U_32 length = 0;
  I_32 family = 0;
  U_32 i = 0;
  U_32 j = 0;
  U_32 nameLength = 0;

  /* required call if we are going to call port library methods */
  PORT_ACCESS_FROM_ENV (env);

  /* get the classes and methods that we need for later calls */
  networkInterfaceClass =
    (*env)->FindClass (env, "java/net/NetworkInterface");
  if (networkInterfaceClass == NULL)
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_NORECOVERY);
      return NULL;
    }

  inetAddressClass = (*env)->FindClass (env, "java/net/InetAddress");
  if (inetAddressClass == NULL)
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_NORECOVERY);
      return NULL;
    }

  methodID =
    (*env)->GetMethodID (env, networkInterfaceClass, "<init>",
       "(Ljava/lang/String;Ljava/lang/String;[Ljava/net/InetAddress;I)V");
  if (methodID == NULL)
    {
      throwJavaNetSocketException (env, HYPORT_ERROR_SOCKET_NORECOVERY);
      return NULL;
    }

  utilClass = (*env)->FindClass (env, "com/ibm/oti/util/Util");
  if (!utilClass)
    {
      return NULL;
    }

  utilMid =
    ((*env)->
     GetStaticMethodID (env, utilClass, "toString",
      "([BII)Ljava/lang/String;"));
  if (!utilMid)
    return NULL;

  result =
    hysock_get_network_interfaces (&networkInterfaceArray,
           preferIPv4Stack (env));

  if (result < 0)
    {
      /* this means an error occured.  The value returned is the socket error that should be returned */
      throwJavaNetSocketException (env, result);
      return NULL;
    }

  /* now loop through the interfaces and extract the information to be returned */
  for (j = 0; j < networkInterfaceArray.length; j++)
    {
      /* set the name and display name and reset the addresses object array */
      addresses = NULL;
      name = NULL;
      displayName = NULL;

      if (networkInterfaceArray.elements[j].name != NULL)
        {
          nameLength = strlen (networkInterfaceArray.elements[j].name);
          bytearray = (*env)->NewByteArray (env, nameLength);
          if (bytearray == NULL)
            {
              /* NewByteArray should have thrown an exception */
              return NULL;
            }
          (*env)->SetByteArrayRegion (env, bytearray, (jint) 0, nameLength,
            networkInterfaceArray.elements[j].name);
          name =
            (*env)->CallStaticObjectMethod (env, utilClass, utilMid,
            bytearray, (jint) 0, nameLength);
          if ((*env)->ExceptionCheck (env))
            {
              return NULL;
            }
        }

      if (networkInterfaceArray.elements[j].displayName != NULL)
        {
          nameLength = strlen (networkInterfaceArray.elements[j].displayName);
          bytearray = (*env)->NewByteArray (env, nameLength);
          if (bytearray == NULL)
            {
              /* NewByteArray should have thrown an exception */
              return NULL;
            }
          (*env)->SetByteArrayRegion (env, bytearray, (jint) 0, nameLength,
            networkInterfaceArray.elements[j].
            displayName);
          displayName =
            (*env)->CallStaticObjectMethod (env, utilClass, utilMid,
            bytearray, (jint) 0, nameLength);
          if ((*env)->ExceptionCheck (env))
            {
              return NULL;
            }
        }

      /* generate the object with the inet addresses for the itnerface       */
      for (i = 0; i < networkInterfaceArray.elements[j].numberAddresses; i++)
        {
          element = newJavaNetInetAddressGenericB (env,
            networkInterfaceArray.
            elements[j].addresses[i].
            addr.bytes,
            networkInterfaceArray.
            elements[j].addresses[i].
            length,
            networkInterfaceArray.
            elements[j].addresses[i].
            scope);
          if (i == 0)
            {
              addresses =
                (*env)->NewObjectArray (env,
                networkInterfaceArray.elements[j].
                numberAddresses, inetAddressClass,
                element);
            }
          else
            {
              (*env)->SetObjectArrayElement (env, addresses, i, element);
            }
        }

      /* now  create the NetworkInterface object for this interface and then add it it ot the arrary that will be returned */
      currentInterface =
        (*env)->NewObject (env, networkInterfaceClass, methodID, name,
        displayName, addresses,
        networkInterfaceArray.elements[j].index);

      if (j == 0)
        {
          networkInterfaces =
            (*env)->NewObjectArray (env, networkInterfaceArray.length,
            networkInterfaceClass, currentInterface);
        }
      else
        {
          (*env)->SetObjectArrayElement (env, networkInterfaces, j,
            currentInterface);
        }
    }

  /* free the memory for the interfaces struct and return the new NetworkInterface List */
  hysock_free_network_interface_struct (&networkInterfaceArray);
  return networkInterfaces;
}
