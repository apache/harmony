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
 * @author Pavel Pervov
 */  

#include "bytereader.h"


////////////////////////////////////////////////////////////////////////////
// begin ByteReader

void ByteReader::dump(unsigned num_bytes)
{
    printf("--- begin dump\n");
    int num_columns = 8;
    bool first_time = true;
    int curr_offset = get_offset();
    const U_8* curr_byte = curr;
    for(unsigned i = 0; i < num_bytes; i++, curr_offset++, curr_byte++) {
        if(first_time || !(curr_offset & (num_columns - 1))) {
            if(!first_time) {
                printf("\n");
            }
            printf("%04x: ", curr_offset);
            if(first_time) {
                first_time = false;
                int filler_cols = curr_offset & (num_columns - 1);
                for(int col = 0; col < filler_cols; col++) {
                    printf("        ");
                }
            }
        }
        U_8 b = *curr_byte;
        printf("%02x", b);
        if(b >= 32 && b < 127) {
            printf(" '%c'  ", b);
        } else {
            printf("  .   ");
        }
    }
    printf("\n");
    printf("--- end dump\n");
} //ByteReader::dump


// end ByteReader
////////////////////////////////////////////////////////////////////////////

