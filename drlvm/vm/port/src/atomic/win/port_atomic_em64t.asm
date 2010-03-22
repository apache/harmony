;  Licensed to the Apache Software Foundation (ASF) under one or more
;  contributor license agreements.  See the NOTICE file distributed with
;  this work for additional information regarding copyright ownership.
;  The ASF licenses this file to You under the Apache License, Version 2.0
;  (the "License"); you may not use this file except in compliance with
;  the License.  You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
;  Unless required by applicable law or agreed to in writing, software
;  distributed under the License is distributed on an "AS IS" BASIS,
;  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;  See the License for the specific language governing permissions and
;  limitations under the License.

PUBLIC port_atomic_cas8

_TEXT	SEGMENT

port_atomic_cas8 PROC

;U_8 port_atomic_cas8(volatile U_8 * data , U_8 value, U_8 comp)
;
;    rcx - *data - pointer to the byte which shoud be exchanged
;    rdx - value - new value
;    r8  - comp  - previous conditional value
;
;   It's a leaf function so no prolog and epilog are used.

    mov rax, r8
    lock cmpxchg [rcx], dl
    ret

port_atomic_cas8 ENDP

_TEXT	ENDS

END

