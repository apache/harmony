;
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
;
.class public TestVirtual
.super java/lang/Object

.method public <init>()V
   aload_0
   invokenonvirtual java/lang/Object/<init>()V
   return
.end method

.method public static TestForVirtual()V
   .limit stack 300
   .limit locals 3 
   
   new Invoke
   dup
   invokespecial Invoke/<init>()V

   ldc 1  
   ldc 2
   ldc 3
   ldc 4
   ldc 5
   ldc 6
   ldc 7
   ldc 8
   ldc 9
   ldc 10
   ldc 11  
   ldc 12
   ldc 13
   ldc 14
   ldc 15
   ldc 16
   ldc 17
   ldc 18
   ldc 19
   ldc 20
   ldc 21  
   ldc 22
   ldc 23
   ldc 24
   ldc 25
   ldc 26
   ldc 27
   ldc 28
   ldc 29
   ldc 30
   ldc 31  
   ldc 32
   ldc 33
   ldc 34
   ldc 35
   ldc 36
   ldc 37
   ldc 38
   ldc 39
   ldc 40
   ldc 41  
   ldc 42
   ldc 43
   ldc 44
   ldc 45
   ldc 46
   ldc 47
   ldc 48
   ldc 49
   ldc 40
   ldc 51  
   ldc 52
   ldc 53
   ldc 54
   ldc 55
   ldc 56
   ldc 57
   ldc 58
   ldc 59
   ldc 60
   ldc 61  
   ldc 62
   ldc 63
   ldc 64
   ldc 65
   ldc 66
   ldc 67
   ldc 68
   ldc 69
   ldc 70
   ldc 71  
   ldc 72
   ldc 73
   ldc 74
   ldc 75
   ldc 76
   ldc 77
   ldc 78
   ldc 79
   ldc 80
   ldc 81  
   ldc 82
   ldc 83
   ldc 84
   ldc 85
   ldc 86
   ldc 87
   ldc 88
   ldc 89
   ldc 90
   ldc 91  
   ldc 92
   ldc 93
   ldc 94
   ldc 95
   ldc 96
   ldc 97
   ldc 98
   ldc 99
   ldc 100
   ldc 101  
   ldc 102
   ldc 103
   ldc 104
   ldc 105
   ldc 106
   ldc 107
   ldc 108
   ldc 109
   ldc 110
   ldc 111  
   ldc 112
   ldc 113
   ldc 114
   ldc 115
   ldc 116
   ldc 117
   ldc 118
   ldc 119
   ldc 120
   ldc 121  
   ldc 122
   ldc 123
   ldc 124
   ldc 125
   ldc 126
   ldc 127
   ldc 128
   ldc 129
   ldc 130
   ldc 131  
   ldc 132
   ldc 133
   ldc 134
   ldc 135
   ldc 136
   ldc 137
   ldc 138
   ldc 139
   ldc 140
   ldc 141  
   ldc 142
   ldc 143
   ldc 144
   ldc 145
   ldc 146
   ldc 147
   ldc 148
   ldc 149
   ldc 140
   ldc 151  
   ldc 152
   ldc 153
   ldc 154
   ldc 155
   ldc 156
   ldc 157
   ldc 158
   ldc 159
   ldc 160
   ldc 161  
   ldc 162
   ldc 163
   ldc 164
   ldc 165
   ldc 166
   ldc 167
   ldc 168
   ldc 169
   ldc 170
   ldc 171  
   ldc 172
   ldc 173
   ldc 174
   ldc 175
   ldc 176
   ldc 177
   ldc 178
   ldc 179
   ldc 180
   ldc 181  
   ldc 182
   ldc 183
   ldc 184
   ldc 185
   ldc 186
   ldc 187
   ldc 188
   ldc 189
   ldc 190
   ldc 191  
   ldc 192
   ldc 193
   ldc 194
   ldc 195
   ldc 196
   ldc 197
   ldc 198
   ldc 199
   ldc 200
   ldc 201  
   ldc 202
   ldc 203
   ldc 204
   ldc 205
   ldc 206
   ldc 207
   ldc 208
   ldc 209
   ldc 210
   ldc 211  
   ldc 212
   ldc 213
   ldc 214
   ldc 215
   ldc 216
   ldc 217
   ldc 218
   ldc 219
   ldc 220
   ldc 221  
   ldc 222
   ldc 223
   ldc 224
   ldc 225
   ldc 226
   ldc 227
   ldc 228
   ldc 229
   ldc 230
   ldc 231  
   ldc 232
   ldc 233
   ldc 234
   ldc 235
   ldc 236
   ldc 237
   ldc 238
   ldc 239
   ldc 240
   ldc 241  
   ldc 242
   ldc 243
   ldc 244
   ldc 245
   ldc 246
   ldc 247
   ldc 248
   ldc 249
   ldc 240
   ldc 251  
   ldc 252
   ldc 253
   ldc 254
   ldc 255
   ldc 256

   ; must throw java.lang.VerifyError or java.lang.ClassFormatError
   invokevirtual Invoke/InvokeVirtual(IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII)I
   
   return   
.end method


