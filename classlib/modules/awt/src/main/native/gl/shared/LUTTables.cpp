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
 * @author Igor V. Stolyarov
 */
 
#include "LUTTables.h"
#include <string.h>

unsigned char mulLUT[256][256];  /* Multiply Luckup table */
unsigned char divLUT[256][256];  /* Divide Luckup table   */
bool mulLUT_inited = false;
bool divLUT_inited = false;

void init_mulLUT(){
    int i, j;
    if(mulLUT_inited) return;
    for(i = 0; i < 256; i++){
        for(j = 0; j < 256; j++){
            mulLUT[i][j] = (int)(((float)i * j) / 255 + 0.5);
        }
    }
    mulLUT_inited = true;
}

void init_divLUT(){
    int i, j;
    if(divLUT_inited) return;
    memset(divLUT[0], 0, 256);
    for(i = 1; i < 256; i++){
        for(j = 0; j <= i; j++){
            divLUT[i][j] = (int)(((float)j) / i * 255 + 0.5);
        }
        for(; j < 256; j++){
            divLUT[i][j] = 0;
        }
    }
    divLUT_inited = true;
}
