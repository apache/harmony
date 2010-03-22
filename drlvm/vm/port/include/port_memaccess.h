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

#ifndef _PORT_MEMACCESS_H_
#define _PORT_MEMACCESS_H_

#include <stddef.h>
#include "open/platform_types.h"
#include "port_general.h"


#ifdef __cplusplus
extern "C" {
#endif


/**
* Tries to read specified number of bytes from given address into buffer.
* @param addr   - memory address to read.
* @param size   - size of bytes to read.
* @param buf    - buffer to read to.
* @return <code>0</code> if OK; nonzero if an error occured.
*/
VMEXPORT int port_read_memory(void* addr, size_t size, void* buf);

/**
* Tries to write specified number of bytes from buffer to given address.
* @param addr   - memory address to write.
* @param size   - size of bytes to write.
* @param buf    - buffer to write from.
* @return <code>0</code> if OK; nonzero if an error occured.
*/
VMEXPORT int port_write_memory(void* addr, size_t size, void* buf);


#ifdef __cplusplus
}
#endif

#endif /* _PORT_MEMACCESS_H_ */
