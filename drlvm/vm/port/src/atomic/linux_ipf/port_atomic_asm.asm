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
// original source is vm\arch\ipf\base\ini_ipf_low_level.asm

	.section .text
	.proc  port_atomic_cas8#
	.global port_atomic_cas8#
	.align 32
port_atomic_cas8:
	// r32 -- Destination (U_8 *)
	// r33 -- Exchange
	// r34 -- Comperand
	// r35 -- for save ar.pfs
	// r36 -- for save ar.ccv

	//alloc		r35 = ar.pfs, 3, 2, 0, 0
	//mov			r36 = ar.ccv
    mov			ar.ccv = r34;;

    cmpxchg1.acq r8 = [r32], r33, ar.ccv
	
	//mov.i	ar.pfs = r35
	//mov		ar.ccv = r36
	br.ret.sptk.many	b0 ;;

	.endp  port_atomic_cas8#



	.section .text
	.proc  port_atomic_cas16#
	.global port_atomic_cas16#
	.align 32
port_atomic_cas16:
	// r32 -- Destination (uint16 *)
	// r33 -- Exchange
	// r34 -- Comperand
	// r35 -- for save ar.pfs
	// r36 -- for save ar.ccv

	//alloc		r35 = ar.pfs, 3, 2, 0, 0
	//mov			r36 = ar.ccv
    mov			ar.ccv = r34;;

    cmpxchg2.acq r8 = [r32], r33, ar.ccv
	
	//mov.i	ar.pfs = r35
	//mov		ar.ccv = r36
	br.ret.sptk.many	b0 ;;

	.endp  port_atomic_cas16#



	.section .text
	.proc  port_atomic_cas32#
	.global port_atomic_cas32#
	.align 32
port_atomic_cas32:
	// r32 -- Destination (U_32 *)
	// r33 -- Exchange
	// r34 -- Comperand
	// r35 -- for save ar.pfs
	// r36 -- for save ar.ccv

	//alloc		r35 = ar.pfs, 3, 2, 0, 0
	//mov			r36 = ar.ccv
    mov			ar.ccv = r34;;

    cmpxchg4.acq r8 = [r32], r33, ar.ccv
	
	//mov.i	ar.pfs = r35
	//mov		ar.ccv = r36
	br.ret.sptk.many	b0 ;;

	.endp  port_atomic_cas32#



	.section .text
	.proc  port_atomic_cas64#
	.global port_atomic_cas64#
	.align 32
port_atomic_cas64:
	// r32 -- Destination (uint64 *)
	// r33 -- Exchange
	// r34 -- Comperand
	// r35 -- for save ar.pfs
	// r36 -- for save ar.ccv

	//alloc		r35 = ar.pfs, 3, 2, 0, 0
	//mov			r36 = ar.ccv
    mov			ar.ccv = r34;;

    cmpxchg8.acq r8 = [r32], r33, ar.ccv
	
	//mov.i	ar.pfs = r35
	//mov		ar.ccv = r36
	br.ret.sptk.many	b0 ;;

	.endp  port_atomic_cas64#
