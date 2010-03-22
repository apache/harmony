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
 
#ifndef _LUT_TABLES_
#define _LUT_TABLES_


#define MUL(a, b) mulLUT[(a)][(b)]
#define DIV(a, b) divLUT[(a)][(b)]

#define COMPOSE(sa, sr, sg, sb, fs, da, dr, dg, db, fd)     \
    da = MUL(sa, fs) + MUL(da, fd);                         \
    dr = MUL(sr, fs) + MUL(dr, fd);                         \
    dg = MUL(sg, fs) + MUL(dg, fd);                         \
    db = MUL(sb, fs) + MUL(db, fd);

#define COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha)    \
    sa = MUL(alpha, sa);                                          \
    sr = MUL(alpha, sr);                                          \
    sg = MUL(alpha, sg);                                          \
    sb = MUL(alpha, sb);                                          \
    da = MUL(sa, fs) + MUL(da, fd);                               \
    dr = MUL(sr, fs) + MUL(dr, fd);                               \
    dg = MUL(sg, fs) + MUL(dg, fd);                               \
    db = MUL(sb, fs) + MUL(db, fd);

extern unsigned char mulLUT[256][256];  /* Multiply Lookup table */
extern unsigned char divLUT[256][256];  /* Divide Lookup table   */
extern bool mulLUT_inited;
extern bool divLUT_inited;

void init_mulLUT();
void init_divLUT();

#endif
