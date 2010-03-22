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

/**
 * @author Intel, Evgueni Brevnov
 */
// Assembly code needed to access low-level IPF features.
//


	.section .text
	.proc  flush_cache_line#
	.global flush_cache_line#
	.align 32
flush_cache_line:

	fc r32 ;;
	br.ret.sptk.many	b0 ;;

	.endp  flush_cache_line#




	.section .text
	.proc  sync_i_cache#
	.global sync_i_cache#
	.align 32
sync_i_cache:

	sync.i ;;
	srlz.i ;;
	br.ret.sptk.many	b0 ;;

	.endp  sync_i_cache#




	.section .text
	.proc  do_flushrs_asm#
	.global do_flushrs_asm#
	.align 32
do_flushrs_asm:

	flushrs ;;
    mov r8 = ar.bsp
    //mov r8 = sp
	br.ret.sptk.many	b0 ;;

	.endp  do_flushrs_asm#




	.section .text
	.proc get_rnat_and_bsp#
	.global get_rnat_and_bsp#
	.align 32
get_rnat_and_bsp:
	alloc	r14=ar.pfs, 1, 0, 0, 0
	mov		r15 = ar.rsc;;
    dep		r16 = r0, r15, 0, 2;;
	mov		ar.rsc = r16
	flushrs;;
	mov		r17 = ar.rnat
	mov		r18 = ar.bsp;;
	mov		ar.rsc = r15
	st8		[r32] = r17, 8;;
	st8		[r32] = r18, 8
	br.ret.sptk.many	b0

	.endp	get_rnat_and_bsp#


	.section .text
	.proc  do_mf_asm#
	.global do_mf_asm#
	.align 32
do_mf_asm:

	mf ;;
	br.ret.sptk.many	b0 ;;

	.endp  do_mf_asm#




	.section .text
	.proc  do_loadrs_asm#
	.global do_loadrs_asm#
	.align 32
do_loadrs_asm:

    mov r14 = ar.rsc ;;
    dep r15 = r32, r14, 16, 14 ;;
    dep r15 = 0, r15, 0, 2 ;;

    mov ar.rsc = r15 ;;

    flushrs ;;

	loadrs ;;

    mov ar.rsc = r14 ;;

	br.ret.sptk.many	b0 ;;

	.endp  do_loadrs_asm#



	.section .text
	.proc get_rnat_and_bspstore#
	.global get_rnat_and_bspstore#
	.align 32
get_rnat_and_bspstore:
	alloc	r14=ar.pfs, 1, 0, 0, 0
	mov		r15 = ar.rsc;;
    dep		r16 = r0, r15, 0, 2;;
	mov		ar.rsc = r16
	flushrs;;
	mov		r17 = ar.rnat;;
	mov		r18 = ar.bspstore;;
	mov		ar.rsc = r15
	st8		[r32] = r17, 8;;
	st8		[r32] = r18, 8
	br.ret.sptk.many	b0

	.endp	get_rnat_and_bspstore#



