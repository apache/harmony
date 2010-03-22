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
 * @author Intel, Pavel Afremov
 */


#include "interpreter.h"
#include "jit_intf_cpp.h"
#include "m2n.h"
#include "stack_iterator.h"
#include "cci.h"

Method_Handle si_get_method(StackIterator* si)
{
    ASSERT_NO_INTERPRETER
    CodeChunkInfo* cci = si_get_code_chunk_info(si);
    if (cci)
        return cci->get_method();
    else
        return m2n_get_method(si_get_m2n(si));
}

U_32 si_get_inline_depth(StackIterator* si)
{
    //
    // Here we assume that JIT data blocks can store only InlineInfo
    // A better idea is to extend JIT_Data_Block with some type information
    // Example:
    //
    // enum JIT_Data_Block_Type { InlineInfo, Empty }
    //
    // struct JIT_Data_Block {
    //     JIT_Data_Block *next;
    //     JIT_Data_Block_Type type;
    //     char bytes[1];
    // };
    //
    // void *Method::allocate_JIT_data_block(size_t size, JIT *jit, JIT_Data_Block_Type)
    //

    ASSERT_NO_INTERPRETER
    CodeChunkInfo* cci = si_get_code_chunk_info(si);
    if ( cci != NULL && cci->has_inline_info()) {
        return cci->get_jit()->get_inline_depth(
                cci->get_inline_info(),
                // FIXME64: no support for large methods
                (U_32)((POINTER_SIZE_INT)si_get_ip(si) - (POINTER_SIZE_INT)cci->get_code_block_addr()));
    }
    return 0;
}
