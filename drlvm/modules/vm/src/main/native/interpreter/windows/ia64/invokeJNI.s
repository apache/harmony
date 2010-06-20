//    Licensed to the Apache Software Foundation (ASF) under one or more
//    contributor license agreements.  See the NOTICE file distributed with
//    this work for additional information regarding copyright ownership.
//    The ASF licenses this file to You under the Apache License, Version 2.0
//    (the "License"); you may not use this file except in compliance with
//    the License.  You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
//   Author: Ivan Volosyuk
//
	.file "invokeJNI_Windows_ipf.asm"
	.text

	.align 16
	.type invokeJNI#	,@function
	.global invokeJNI#
	.section	.text
	.proc invokeJNI#
invokeJNI:
//    int64 invokeJNI(uword *args, uword *fpargs, int64 count, int64 frame, GenericFunctionPointer f);
//                    r32          r33            r33          r34          r35
	.prologue
{	.mii
	.save ar.pfs, r38
	alloc r38 = ar.pfs, 5, 4, 8, 0
	add r12 = r35, r12
	mov r16 = r0
	;;
}
{	.mii
	adds r14 = -8, r34
	.save ar.lc, r40
	mov r40 = ar.lc
	mov r39 = r1
	;;
}
{	.mii
	cmp4.lt p6, p7 = 0, r14
	sxt4 r14 = r14
	;;
}
{
	adds r14 = -1, r14
	;;
}
{	.mib
	nop.m 0
	.save rp, r37
	;;
}
{
	mov r37 = b0
	(p7) br.cond.dpnt .L9
	;;
}
{	.mii
	nop.m 0
	mov ar.lc = r14
	nop.i 0
	;;
}
.L10:
{	.mii
	adds r17 = 16, r12
	sxt4 r14 = r16
	adds r16 = 1, r16
	;;
}
{	.mmi
	shladd r14 = r14, 3, r0
	;;
}
{
	add r15 = r14, r32
	add r14 = r14, r17
	;;
}
{	.mmi
	adds r15 = 64, r15
	;;
}
{
	ldfd f6 = [r15]
	nop.i 0
	;;
}
{	.mib
	stfd [r14] = f6
	nop.i 0
	br.cloop.sptk.few .L10
	;;
}
.L9:

// start double copier

{	.mmi
	adds r14 = 8, r33
	adds r16 = 16, r33
	adds r17 = 24, r33
	;;
}
{	.mii
	adds r18 = 32, r33
	adds r19 = 40, r33
	nop.i 0
	;;
}
{	.mii
	ldfd f8 = [r33]
	nop.i 0
	nop.i 0
}
{	.mii
	ldfd f9 = [r14]
	nop.i 0
	nop.i 0
}
{	.mmi
	ldfd f10 = [r16]
	ldfd f11 = [r17]
	nop.i 0
}
{	.mmi
	ldfd f12 = [r18]
	ldfd f13 = [r19]
	;;
}
    
// end  double copier 

// start integer copier
	;;
	adds r17 = 8, r32
{	.mii
	adds r18 = 16, r32
	adds r19 = 24, r32
	adds r20 = 32, r32
}
{	.mmi
	adds r21 = 40, r32
	;;
	adds r22 = 48, r32
	adds r23 = 56, r32
	;;
}
{	.mmi
	ld8 r41 = [r32]
	ld8 r42 = [r17]
	nop.i 0
}
{	.mmi
	ld8 r43 = [r18]
	ld8 r44 = [r19]
	nop.i 0
}
{	.mmi
	ld8 r45 = [r20]
	ld8 r46 = [r21]
	nop.i 0
}
{	.mmi
	ld8 r47 = [r22]
	ld8 r48 = [r23]
	nop.i 0
	;;
}
{	.mmi
	ld8 r14 = [r36], 8
	;;
	ld8 r1 = [r36], -8
	mov b6 = r14
        ;;
}
{	.mib
	nop.m 0
	nop.i 0
	br.call.sptk.many b0 = b6
        ;;
}

// end integer copier

{	.mii
        nop.m 0
	mov r1 = r39
        nop.i 0
}
{	.mii
        nop.m 0
	mov ar.pfs = r38
	mov ar.lc = r40
}
{	.mii
	nop.m 0
	mov b0 = r37
	sub r12 = r12, r35
}
{	.mib
	nop.m 0
	nop.i 0
	br.ret.sptk.many b0
}
	.endp invokeJNI#
