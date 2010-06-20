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
//   Author: Intel, Evgueni Brevnov
//
	.text

	.align 16
	.global vm_invoke_native_array_stub#
	.global vm_invoke_native_array_stub_ref#
	.global vm_invoke_native_array_stub_float#
	.global vm_invoke_native_array_stub_double#
	.global vm_invoke_native_array_stub_int#

	.proc vm_invoke_native_array_stub#
	.proc vm_invoke_native_array_stub_ref#
	.proc vm_invoke_native_array_stub_float#
	.proc vm_invoke_native_array_stub_double#
	.proc vm_invoke_native_array_stub_int#

vm_invoke_native_array_stub:
vm_invoke_native_array_stub_ref:
vm_invoke_native_array_stub_float:
vm_invoke_native_array_stub_double:
vm_invoke_native_array_stub_int:
//    int64 vm_invoke_native_array_stub(uword *args, uword *fpargs, int64 count, int64 frame, GenericFunctionPointer f);
//                    r32          r33            r34          r35          r36
	.mii
	.save ar.pfs, r38
	alloc r38 = ar.pfs, 5, 4, 8, 0
	add r12 = r35, r12
	mov r16 = r0
	.mii
	adds r14 = -8, r34
	.save ar.lc, r40
	mov r40 = ar.lc
	mov r39 = r1
	;;
	.mii
	cmp4.lt p6, p7 = 0, r14
	sxt4 r14 = r14
	;;
	adds r14 = -1, r14
	;;
	.mib
	nop.m 0
	.save rp, r37
	mov r37 = b0
	(p7) br.cond.dpnt .L9
	.mii
	nop.m 0
	mov ar.lc = r14
	nop.i 0
.L10:
	.mii
	adds r17 = 16, r12
	sxt4 r14 = r16
	adds r16 = 1, r16
	;;
	.mmi
	shladd r14 = r14, 3, r0
	;;
	add r15 = r14, r32
	add r14 = r14, r17
	;;
	.mmi
	adds r15 = 64, r15
	;;
	ldfd f6 = [r15]
	nop.i 0
	;;
	.mib
	stfd [r14] = f6
	nop.i 0
	br.cloop.sptk.few .L10
        ;;
.L9:

// start double copier

	.mii
    adds r14 = 48, r33
	adds r15 = 49, r33
	adds r16 = 50, r33
    ;;
    .mmi
    ld1 r14 = [r14]
    ld1 r15 = [r15]
	adds r17 = 51, r33
    ;;
    .mmi
    ld1 r16 = [r16]
    ld1 r17 = [r17]
	adds r18 = 52, r33
    ;;
    .mii
    ld1 r18 = [r18]
	adds r19 = 53, r33
    cmp4.eq.unc p7,p6=r14,r0
    ;;
    .mii
    ld1 r19 = [r19]
    cmp4.eq.unc p5,p4=r15,r0
    cmp4.eq.unc p3,p2=r16,r0
    ;;
    .mmi
    (p7) ldfs f8 = [r33]
    (p6) ldfd f8 = [r33]
    adds r20 = 8, r33
    ;;
    .mmi
    (p5) ldfs f9 = [r20]
    (p4) ldfd f9 = [r20]
    adds r21 = 16, r33
    ;;
    .mmi
    (p3) ldfs f10 = [r21]
    (p2) ldfd f10 = [r21]
    adds r22 = 24, r33
    ;;
    .mii
    cmp4.eq.unc p7,p6=r17,r0
    cmp4.eq.unc p5,p4=r18,r0
    cmp4.eq.unc p3,p2=r19,r0
    ;;
    .mmi
    (p7) ldfs f11 = [r22]
    (p6) ldfd f11 = [r22]
    adds r23 = 32, r33
    ;;
    .mmi
    (p5) ldfs f12 = [r23]
    (p4) ldfd f12 = [r23]
    adds r14 = 40, r33
    ;;
    .mmi
    (p3) ldfs f13 = [r14]
    (p2) ldfd f13 = [r14]
    nop.i 0
    
// end  double copier

// start integer copier
	;;
	adds r17 = 8, r32
	.mii
	adds r18 = 16, r32
	adds r19 = 24, r32
	adds r20 = 32, r32
	.mmi
	adds r21 = 40, r32
	;;
	adds r22 = 48, r32
	adds r23 = 56, r32
	;;
	.mmi
	ld8 r41 = [r32]
	ld8 r42 = [r17]
	nop.i 0
	.mmi
	ld8 r43 = [r18]
	ld8 r44 = [r19]
	nop.i 0
	.mmi
	ld8 r45 = [r20]
	ld8 r46 = [r21]
	nop.i 0
	.mmi
	ld8 r47 = [r22]
	ld8 r48 = [r23]
	nop.i 0
	;;

	.mmi
	ld8 r14 = [r36], 8
	;;
	ld8 r1 = [r36], -8
	mov b6 = r14
        ;;
	.mib
	nop.m 0
	nop.i 0
	br.call.sptk.many b0 = b6
        ;;

// end integer copier

	.mii
        nop.m 0
	mov r1 = r39
        nop.i 0
        .mii
        nop.m 0
	mov ar.pfs = r38
	mov ar.lc = r40
	.mii
	nop.m 0
	mov b0 = r37
	sub r12 = r12, r35
	.mib
	nop.m 0
	nop.i 0
	br.ret.sptk.many b0
	.endp invokeJNI#

	.ident	"GCC: (GNU) 3.3.3 (SuSE Linux)"
