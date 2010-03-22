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
#ifndef _CLASS_INTERFACE_H
#define _CLASS_INTERFACE_H

/**
 * Constant pool tags.
 */
typedef enum {
    _CONSTANT_Unknown               = 0,
    _CONSTANT_Utf8                  = 1,
    _CONSTANT_Integer               = 3,
    _CONSTANT_Float                 = 4,
    _CONSTANT_Long                  = 5,
    _CONSTANT_Double                = 6,
    _CONSTANT_Class                 = 7,
    _CONSTANT_String                = 8,
    _CONSTANT_Fieldref              = 9,
    _CONSTANT_Methodref             = 10,
    _CONSTANT_InterfaceMethodref    = 11,
    _CONSTANT_NameAndType           = 12
} ClassConstantPoolTags;

#endif /* _CLASS_INTERFACE_H */

