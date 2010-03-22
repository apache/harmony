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
 * @author Pavel Pervov, Pavel Rebriy
 */  

#ifndef _BYTECODES_H_
#define _BYTECODES_H_

/** @file bytecodes.h 
 * The list of byte codes is used by interpreter, jitrino, verifier, class_support and jvmti.
 * */

/** Enumerator of bytecode opcodes.
 * An enumerator sets up a correspondence between instruction mnemonics
 * and instruction <code>opcode</code> values.
 * An enumerator identifier contains instruction mnemonic.
 * The value of the constant is the <code>opcode</code> value.
 */

enum JavaByteCodes {

    OPCODE_NOP = 0,        /** Holds the <code>opcode</code> value of the <code>nop</code> instruction, 0x00.
                  \sa The instruction description <a nop="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc10.html#nop">nop</a>. */
    OPCODE_ACONST_NULL,    /** Holds the <code>opcode</code> value of the <code>const_null</code> instruction, 0x01.
                 \sa The instruction description <a const_null="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aconst_null">aconst_null</a>. */
    OPCODE_ICONST_M1,      /** Holds the <code>opcode</code> value of the <code>iconst_m1</code> instruction, 0x02.
                 \sa The instruction description <a iconst_i="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_i</a>. */
    OPCODE_ICONST_0,       /** Holds the <code>opcode</code> value of the <code>iconst_0</code> instruction, 0x03.
                 \sa The instruction description  <a iconst_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_0</a>. */
    OPCODE_ICONST_1,       /** Holds the <code>opcode</code> value of the <code>iconst_1</code> instruction, 0x04. 
                 \sa The instruction description  <a iconst_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_1</a>. */
    OPCODE_ICONST_2,       /** Holds the <code>opcode</code> value of the <code>iconst_2</code> instruction, 0x05.
                 \sa The instruction description  <a iconst_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_2</a>. */
    OPCODE_ICONST_3,       /** Holds the <code>opcode</code> value of the <code>iconst_3</code> instruction, 0x06.
                 \sa The instruction description  <a iconst_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_3</a>. */
    OPCODE_ICONST_4,       /** Holds the <code>opcode</code> value of the <code>iconst_4</code> instruction, 0x07.
                 \sa The instruction description  <a iconst_4="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_4</a>. */
    OPCODE_ICONST_5,       /** Holds the <code>opcode</code> value of the <code>iconst_5</code> instruction, 0x08.
                 \sa The instruction description  <a iconst_5="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iconst_i">iconst_5</a>. */
    OPCODE_LCONST_0,       /** Holds the <code>opcode</code> value of the <code>lconst_0</code> instruction, 0x09.
                 \sa The instruction description  <a lconst_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lconst_l">lconst_0</a>. */
    OPCODE_LCONST_1,       /** Holds the <code>opcode</code> value of the <code>lconst_1</code> instruction, 0x0a.
                 \sa The instruction description  <a lconst_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lconst_l">lconst_1</a>. */
    OPCODE_FCONST_0,       /** Holds the <code>opcode</code> value of the <code>fconst_0</code> instruction, 0x0b.
                 \sa The instruction description  <a fconst_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fconst_f">fconst_0</a>. */
    OPCODE_FCONST_1,       /** Holds the <code>opcode</code> value of the <code>fconst_1</code> instruction, 0x0c.
                 \sa The instruction description  <a fconst_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fconst_f">fconst_1</a>. */
    OPCODE_FCONST_2,       /** Holds the <code>opcode</code> value of the <code>fconst_2</code> instruction, 0x0d.
                 \sa The instruction description  <a fconst_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fconst_f">fconst_2</a>. */
    OPCODE_DCONST_0,       /** Holds the <code>opcode</code> value of the <code>dconst_0</code> instruction, 0x0e.
                 \sa The instruction description  <a dconst_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dconst_d">dconst_0</a>. */
    OPCODE_DCONST_1,       /** Holds the <code>opcode</code> value of the <code>dconst_1</code> instruction, 0x0f.
                 \sa The instruction description  <a dconst_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dconst_d">dconst_1</a>. */
    OPCODE_BIPUSH,         /** Holds the <code>opcode</code> value of the <code>bipush</code> instruction, 0x10 + s1.
                 \sa The instruction description  <a bipush="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc1.html#bipush">bipush</a>. */
    OPCODE_SIPUSH,         /** Holds the <code>opcode</code> value of the <code>sipush</code> instruction, 0x11 + s2.
                 \sa The instruction description  <a sipush="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc13.html#sipush">sipush</a>. */
    OPCODE_LDC,            /** Holds the <code>opcode</code> value of the <code>ldc</code> instruction, 0x12 + u1.
                 \sa The instruction description  <a ldc="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#ldc">ldc</a>. */
    OPCODE_LDC_W,          /** Holds the <code>opcode</code> value of the <code>ldc_w</code> instruction, 0x13 + u2.
                 \sa The instruction description  <a ldc_w="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#ldc_w">ldc_w</a>. */
    OPCODE_LDC2_W,         /** Holds the <code>opcode</code> value of the <code>ldc2_w</code> instruction, 0x14 + u2.
                 \sa The instruction description  <a ldc2_w="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#ldc2_w">ldc2_w</a>. */
    OPCODE_ILOAD,          /** Holds the <code>opcode</code> value of the <code>iload</code> instruction, 0x15 + u1|u2.
                 \sa The instruction description  <a iload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iload">iload</a>. */
    OPCODE_LLOAD,          /** Holds the <code>opcode</code> value of the <code>lload</code> instruction, 0x16 + u1|u2.
                 \sa The instruction description  <a lload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lload">lload</a>. */
    OPCODE_FLOAD,          /** Holds the <code>opcode</code> value of the <code>fload</code> instruction, 0x17 + u1|u2.
                 \sa The instruction description  <a fload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fload">fload</a>. */
    OPCODE_DLOAD,          /** Holds the <code>opcode</code> value of the <code>dload</code> instruction, 0x18 + u1|u2.
                 \sa The instruction description  <a dload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dload">dload</a>. */
    OPCODE_ALOAD,          /** Holds the <code>opcode</code> value of the <code>aload</code> instruction, 0x19 + u1|u2.
                 \sa The instruction description  <a aload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aload">aload</a>. */
    OPCODE_ILOAD_0,        /** Holds the <code>opcode</code> value of the <code>iload_0</code> instruction, 0x1a.
                 \sa The instruction description  <a iload_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iload_n">iload_0</a>. */
    OPCODE_ILOAD_1,        /** Holds the <code>opcode</code> value of the <code>iload_1</code> instruction, 0x1b.
                 \sa The instruction description  <a iload_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iload_n">iload_1</a>. */
    OPCODE_ILOAD_2,        /** Holds the <code>opcode</code> value of the <code>iload_2</code> instruction, 0x1c.
                 \sa The instruction description  <a iload_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iload_n">iload_2</a>. */
    OPCODE_ILOAD_3,        /** Holds the <code>opcode</code> value of the <code>iload_3</code> instruction, 0x1d.
                 \sa The instruction description  <a iload_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iload_n">iload_3</a>. */
    OPCODE_LLOAD_0,        /** Holds the <code>opcode</code> value of the <code>lload_0</code> instruction, 0x1e.
                 \sa The instruction description  <a lload_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lload_n">lload_0</a>. */
    OPCODE_LLOAD_1,        /** Holds the <code>opcode</code> value of the <code>lload_1</code> instruction, 0x1f.
                 \sa The instruction description  <a lload_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lload_n">lload_1</a>. */
    OPCODE_LLOAD_2,        /** Holds the <code>opcode</code> value of the <code>lload_2</code> instruction, 0x20.
                 \sa The instruction description  <a lload_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lload_n">lload_2</a>. */
    OPCODE_LLOAD_3,        /** Holds the <code>opcode</code> value of the <code>lload_3</code> instruction, 0x21.
                 \sa The instruction description  <a lload_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lload_n">lload_3</a>. */
    OPCODE_FLOAD_0,        /** Holds the <code>opcode</code> value of the <code>fload_0</code> instruction, 0x22.
                 \sa The instruction description  <a fload_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fload_n">fload_0</a>. */
    OPCODE_FLOAD_1,        /** Holds the <code>opcode</code> value of the <code>fload_1</code> instruction, 0x23.
                 \sa The instruction description  <a fload_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fload_n">fload_1</a>. */
    OPCODE_FLOAD_2,        /** Holds the <code>opcode</code> value of the <code>fload_2</code> instruction, 0x24.
                 \sa The instruction description  <a fload_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fload_n">fload_2</a>. */
    OPCODE_FLOAD_3,        /** Holds the <code>opcode</code> value of the <code>fload_3</code> instruction, 0x25.
                 \sa The instruction description  <a fload_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fload_n">fload_3</a>. */
    OPCODE_DLOAD_0,        /** Holds the <code>opcode</code> value of the <code>dload_0</code> instruction, 0x26.
                 \sa The instruction description  <a dload_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dload_n">dload_0</a>. */
    OPCODE_DLOAD_1,        /** Holds the <code>opcode</code> value of the <code>dload_1</code> instruction, 0x27.
                 \sa The instruction description  <a dload_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dload_n">dload_1</a>. */
    OPCODE_DLOAD_2,        /** Holds the <code>opcode</code> value of the <code>dload_2</code> instruction, 0x28.
                 \sa The instruction description  <a dload_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dload_n">dload_2</a>. */
    OPCODE_DLOAD_3,        /** Holds the <code>opcode</code> value of the <code>dload_3</code> instruction, 0x29.
                 \sa The instruction description  <a dload_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dload_n">dload_3</a>. */
    OPCODE_ALOAD_0,        /** Holds the <code>opcode</code> value of the <code>aload_0</code> instruction, 0x2a.
                 \sa The instruction description  <a aload_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aload_n">aload_0</a>. */
    OPCODE_ALOAD_1,        /** Holds the <code>opcode</code> value of the <code>aload_1</code> instruction, 0x2b.
                 \sa The instruction description  <a aload_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aload_n">aload_1</a>. */
    OPCODE_ALOAD_2,        /** Holds the <code>opcode</code> value of the <code>aload_2</code> instruction, 0x2c.
                 \sa The instruction description  <a aload_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aload_n">aload_2</a>. */
    OPCODE_ALOAD_3,        /** Holds the <code>opcode</code> value of the <code>aload_3</code> instruction, 0x2d.
                 \sa The instruction description  <a aload_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aload_n">aload_3</a>. */
    OPCODE_IALOAD,         /** Holds the <code>opcode</code> value of the <code>iaload</code> instruction, 0x2e.
                 \sa The instruction description  <a iaload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iaload">iaload</a>. */
    OPCODE_LALOAD,         /** Holds the <code>opcode</code> value of the <code>laload</code> instruction, 0x2f.
                 \sa The instruction description  <a laload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#laload">laload</a>. */
    OPCODE_FALOAD,         /** Holds the <code>opcode</code> value of the <code>faload</code> instruction, 0x30.
                 \sa The instruction description  <a faload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#faload">faload</a>. */
    OPCODE_DALOAD,         /** Holds the <code>opcode</code> value of the <code>daload</code> instruction, 0x31.
                 \sa The instruction description  <a daload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#daload">daload</a>. */
    OPCODE_AALOAD,         /** Holds the <code>opcode</code> value of the <code>aaload</code> instruction, 0x32.
                 \sa The instruction description  <a aaload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aaload">aaload</a>. */
    OPCODE_BALOAD,         /** Holds the <code>opcode</code> value of the <code>baload</code> instruction, 0x33.
                 \sa The instruction description  <a baload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc1.html#baload">baload</a>. */
    OPCODE_CALOAD,         /** Holds the <code>opcode</code> value of the <code>caload</code> instruction, 0x34.
                 \sa The instruction description  <a caload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc2.html#caload">caload</a>. */
    OPCODE_SALOAD,         /** Holds the <code>opcode</code> value of the <code>saload</code> instruction, 0x35.
                 \sa The instruction description  <a saload="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc13.html#saload">saload</a>. */
    OPCODE_ISTORE,         /** Holds the <code>opcode</code> value of the <code>istore</code> instruction, 0x36 + u1|u2.
                 \sa The instruction description  <a istore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#istore">istore</a>. */
    OPCODE_LSTORE,         /** Holds the <code>opcode</code> value of the <code>lstore</code> instruction, 0x37 + u1|u2.
                 \sa The instruction description  <a lstore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lstore">lstore</a>. */
    OPCODE_FSTORE,         /** Holds the <code>opcode</code> value of the <code>fstore</code> instruction, 0x38 + u1|u2.
                 \sa The instruction description  <a fstore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fstore">fstore</a>. */
    OPCODE_DSTORE,         /** Holds the <code>opcode</code> value of the <code>dstore</code> instruction, 0x39 + u1|u2.
                 \sa The instruction description  <a dstore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dstore">dstore</a>. */
    OPCODE_ASTORE,         /** Holds the <code>opcode</code> value of the <code>astore</code> instruction, 0x3a + u1|u2.
                 \sa The instruction description  <a astore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#astore">astore</a>. */
    OPCODE_ISTORE_0,       /** Holds the <code>opcode</code> value of the <code>istore_0</code> instruction, 0x3b.
                 \sa The instruction description  <a istore_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#istore_n">istore_0</a>. */
    OPCODE_ISTORE_1,       /** Holds the <code>opcode</code> value of the <code>istore_1</code> instruction, 0x3c.
                 \sa The instruction description  <a istore_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#istore_n">istore_1</a>. */
    OPCODE_ISTORE_2,       /** Holds the <code>opcode</code> value of the <code>istore_2</code> instruction, 0x3d.
                 \sa The instruction description  <a istore_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#istore_n">istore_2</a>. */
    OPCODE_ISTORE_3,       /** Holds the <code>opcode</code> value of the <code>istore_3</code> instruction, 0x3e.
                 \sa The instruction description  <a istore_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#istore_n">istore_3</a>. */
    OPCODE_LSTORE_0,       /** Holds the <code>opcode</code> value of the <code>lstore_0</code> instruction, 0x3f.
                 \sa The instruction description  <a lstore_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lstore_n">lstore_0</a>. */
    OPCODE_LSTORE_1,       /** Holds the <code>opcode</code> value of the <code>lstore_1</code> instruction, 0x40.
                 \sa The instruction description  <a lstore_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lstore_n">lstore_1</a>. */
    OPCODE_LSTORE_2,       /** Holds the <code>opcode</code> value of the <code>lstore_2</code> instruction, 0x41.
                 \sa The instruction description  <a lstore_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lstore_n">lstore_2</a>. */
    OPCODE_LSTORE_3,       /** Holds the <code>opcode</code> value of the <code>lstore_3</code> instruction, 0x42.
                 \sa The instruction description  <a lstore_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lstore_n">lstore_3</a>. */
    OPCODE_FSTORE_0,       /** Holds the <code>opcode</code> value of the <code>fstore_0</code> instruction, 0x43.
                 \sa The instruction description  <a fstore_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fstore_n">fstore_0</a>. */
    OPCODE_FSTORE_1,       /** Holds the <code>opcode</code> value of the <code>fstore_1</code> instruction, 0x44.
                 \sa The instruction description  <a fstore_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fstore_n">fstore_1</a>. */
    OPCODE_FSTORE_2,       /** Holds the <code>opcode</code> value of the <code>fstore_2</code> instruction, 0x45.
                 \sa The instruction description  <a fstore_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fstore_n">fstore_2</a>. */
    OPCODE_FSTORE_3,       /** Holds the <code>opcode</code> value of the <code>fstore_3</code> instruction, 0x46.
                 \sa The instruction description  <a fstore_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fstore_n">fstore_3</a>. */
    OPCODE_DSTORE_0,       /** Holds the <code>opcode</code> value of the <code>dstore_0</code> instruction, 0x47.
                 \sa The instruction description  <a dstore_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dstore_n">dstore_0</a>. */
    OPCODE_DSTORE_1,       /** Holds the <code>opcode</code> value of the <code>dstore_1</code> instruction, 0x48.
                 \sa The instruction description  <a dstore_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dstore_n">dstore_1</a>. */
    OPCODE_DSTORE_2,       /** Holds the <code>opcode</code> value of the <code>dstore_1</code> instruction, 0x49.
                 \sa The instruction description  <a dstore_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dstore_n">dstore_2</a>. */
    OPCODE_DSTORE_3,       /** Holds the <code>opcode</code> value of the <code>dstore_3</code> instruction, 0x4a.
                 \sa The instruction description  <a dstore_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dstore_n">dstore_3</a>. */
    OPCODE_ASTORE_0,       /** Holds the <code>opcode</code> value of the <code>astore_0</code> instruction, 0x4b.
                 \sa The instruction description  <a astore_0="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#astore_n">astore_0</a>. */
    OPCODE_ASTORE_1,       /** Holds the <code>opcode</code> value of the <code>astore_1</code> instruction, 0x4c.
                 \sa The instruction description  <a astore_1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#astore_n">astore_1</a>. */
    OPCODE_ASTORE_2,       /** Holds the <code>opcode</code> value of the <code>astore_2</code> instruction, 0x4d.
                 \sa The instruction description  <a astore_2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#astore_n">astore_2</a>. */
    OPCODE_ASTORE_3,       /** Holds the <code>opcode</code> value of the <code>astore_3</code> instruction, 0x4e.
                 \sa The instruction description  <a astore_3="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#astore_n">astore_3</a>. */
    OPCODE_IASTORE,        /** Holds the <code>opcode</code> value of the <code>iastore</code> instruction, 0x4f.
                 \sa The instruction description  <a iastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iastore">iastore</a>. */
    OPCODE_LASTORE,        /** Holds the <code>opcode</code> value of the <code>lastore</code> instruction, 0x50.
                 \sa The instruction description  <a lastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lastore">lastore</a>. */
    OPCODE_FASTORE,        /** Holds the <code>opcode</code> value of the <code>fastore</code> instruction, 0x51.
                 \sa The instruction description  <a fastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fastore">fastore</a>. */
    OPCODE_DASTORE,        /** Holds the <code>opcode</code> value of the <code>dastore</code> instruction, 0x52.
                 \sa The instruction description  <a dastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dastore">dastore</a>. */
    OPCODE_AASTORE,        /** Holds the <code>opcode</code> value of the <code>aastore</code> instruction, 0x53.
                 \sa The instruction description  <a aastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#aastore">aastore</a>. */
    OPCODE_BASTORE,        /** Holds the <code>opcode</code> value of the <code>bastore</code> instruction, 0x54.
                 \sa The instruction description  <a bastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc1.html#bastore">bastore</a>. */
    OPCODE_CASTORE,        /** Holds the <code>opcode</code> value of the <code>castore</code> instruction, 0x55.
                 \sa The instruction description  <a castore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc2.html#castore">castore</a>. */
    OPCODE_SASTORE,        /** Holds the <code>opcode</code> value of the <code>sastore</code> instruction, 0x56.
                 \sa The instruction description  <a sastore="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc13.html#sastore">sastore</a>. */
    OPCODE_POP,            /** Holds the <code>opcode</code> value of the <code>pop</code> instruction, 0x57.
                 \sa The instruction description  <a pop="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc11.html#pop">pop</a>. */
    OPCODE_POP2,           /** Holds the <code>opcode</code> value of the <code>pop2</code> instruction, 0x58.
                 \sa The instruction description  <a pop2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc11.html#pop2">pop2</a>. */
    OPCODE_DUP,            /** Holds the <code>opcode</code> value of the <code>dup</code> instruction, 0x59.
                 \sa The instruction description  <a dup="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dup">dup</a>. */
    OPCODE_DUP_X1,         /** Holds the <code>opcode</code> value of the <code>dup_x1</code> instruction, 0x5a.
                 \sa The instruction description  <a dup_x1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dup_x1">dup_x1</a>. */
    OPCODE_DUP_X2,         /** Holds the <code>opcode</code> value of the <code>dup_x2</code> instruction, 0x5b.
                 \sa The instruction description  <a dup_x2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dup_x2">dup_x2</a>. */
    OPCODE_DUP2,           /** Holds the <code>opcode</code> value of the <code>dup2</code> instruction, 0x5c.
                 \sa The instruction description  <a dup2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dup2">dup2</a>. */
    OPCODE_DUP2_X1,        /** Holds the <code>opcode</code> value of the <code>dup2_x1</code> instruction, 0x5d.
                 \sa The instruction description  <a dup2_x1="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dup2_x1">dup2_x1</a>. */
    OPCODE_DUP2_X2,        /** Holds the <code>opcode</code> value of the <code>dup2_x2</code> instruction, 0x5e.
                 \sa The instruction description  <a dup2_x2="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dup2_x2">dup2_x2</a>. */
    OPCODE_SWAP,           /** Holds the <code>opcode</code> value of the <code>swap</code> instruction, 0x5f.
                 \sa The instruction description  <a swap="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc13.html#swap">swap</a>. */
    OPCODE_IADD,           /** Holds the <code>opcode</code> value of the <code>iadd</code> instruction, 0x60.
                 \sa The instruction description  <a iadd="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iadd">iadd</a>. */
    OPCODE_LADD,           /** Holds the <code>opcode</code> value of the <code>ladd</code> instruction, 0x61.
                 \sa The instruction description  <a ladd="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#ladd">ladd</a>. */
    OPCODE_FADD,           /** Holds the <code>opcode</code> value of the <code>fadd</code> instruction, 0x62.
                 \sa The instruction description  <a fadd="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fadd">fadd</a>. */
    OPCODE_DADD,           /** Holds the <code>opcode</code> value of the <code>dadd</code> instruction, 0x63.
                 \sa The instruction description  <a dadd="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dadd">dadd</a>. */
    OPCODE_ISUB,           /** Holds the <code>opcode</code> value of the <code>isub</code> instruction, 0x64.
                 \sa The instruction description  <a isub="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#isub">isub</a>. */
    OPCODE_LSUB,           /** Holds the <code>opcode</code> value of the <code>lsub</code> instruction, 0x65.
                 \sa The instruction description  <a lsub="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lsub">lsub</a>. */
    OPCODE_FSUB,           /** Holds the <code>opcode</code> value of the <code>fsub</code> instruction, 0x66.
                 \sa The instruction description  <a fsub="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fsub">fsub</a>. */
    OPCODE_DSUB,           /** Holds the <code>opcode</code> value of the <code>dsub</code> instruction, 0x67.
                 \sa The instruction description  <a dsub="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dsub">dsub</a>. */
    OPCODE_IMUL,           /** Holds the <code>opcode</code> value of the <code>imul</code> instruction, 0x68.
                 \sa The instruction description  <a imul="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#imul">imul</a>. */
    OPCODE_LMUL,           /** Holds the <code>opcode</code> value of the <code>lmul</code> instruction, 0x69.
                 \sa The instruction description  <a lmul="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lmul">lmul</a>. */
    OPCODE_FMUL,           /** Holds the <code>opcode</code> value of the <code>fmul</code> instruction, 0x6a.
                 \sa The instruction description  <a fmul="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fmul">fmul</a>. */
    OPCODE_DMUL,           /** Holds the <code>opcode</code> value of the <code>dmul</code> instruction, 0x6b.
                 \sa The instruction description  <a dmul="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dmul">dmul</a>. */
    OPCODE_IDIV,           /** Holds the <code>opcode</code> value of the <code>idiv</code> instruction, 0x6c.
                 \sa The instruction description  <a idiv="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#idiv">idiv</a>. */
    OPCODE_LDIV,           /** Holds the <code>opcode</code> value of the <code>ldiv</code> instruction, 0x6d.
                 \sa The instruction description  <a ldiv="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#ldiv">ldiv</a>. */
    OPCODE_FDIV,           /** Holds the <code>opcode</code> value of the <code>fdiv</code> instruction, 0x6e.
                 \sa The instruction description  <a fdiv="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fdiv">fdiv</a>. */
    OPCODE_DDIV,           /** Holds the <code>opcode</code> value of the <code>ddiv</code> instruction, 0x6f.
                 \sa The instruction description  <a ddiv="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#ddiv">ddiv</a>. */
    OPCODE_IREM,           /** Holds the <code>opcode</code> value of the <code>irem</code> instruction, 0x70.
                 \sa The instruction description  <a irem="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#irem">irem</a>. */
    OPCODE_LREM,           /** Holds the <code>opcode</code> value of the <code>lrem</code> instruction, 0x71.
                 \sa The instruction description  <a lrem="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lrem">lrem</a>. */
    OPCODE_FREM,           /** Holds the <code>opcode</code> value of the <code>frem</code> instruction, 0x72.
                 \sa The instruction description  <a frem="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#frem">frem</a>. */
    OPCODE_DREM,           /** Holds the <code>opcode</code> value of the <code>drem</code> instruction, 0x73.
                 \sa The instruction description  <a drem="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#drem">drem</a>. */
    OPCODE_INEG,           /** Holds the <code>opcode</code> value of the <code>ineg</code> instruction, 0x74.
                 \sa The instruction description  <a ineg="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ineg">ineg</a>. */
    OPCODE_LNEG,           /** Holds the <code>opcode</code> value of the <code>lneg</code> instruction, 0x75.
                 \sa The instruction description  <a lneg="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lneg">lneg</a>. */
    OPCODE_FNEG,           /** Holds the <code>opcode</code> value of the <code>fneg</code> instruction, 0x76.
                 \sa The instruction description  <a fneg="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fneg">fneg</a>. */
    OPCODE_DNEG,           /** Holds the <code>opcode</code> value of the <code>dneg</code> instruction, 0x77.
                 \sa The instruction description  <a dneg="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dneg">dneg</a>. */
    OPCODE_ISHL,           /** Holds the <code>opcode</code> value of the <code>ishl</code> instruction, 0x78.
                 \sa The instruction description  <a ishl="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ishl">ishl</a>. */
    OPCODE_LSHL,           /** Holds the <code>opcode</code> value of the <code>lshl</code> instruction, 0x79.
                 \sa The instruction description  <a lshl="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lshl">lshl</a>. */
    OPCODE_ISHR,           /** Holds the <code>opcode</code> value of the <code>ishr</code> instruction, 0x7a.
                 \sa The instruction description  <a ishr="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ishr">ishr</a>. */
    OPCODE_LSHR,           /** Holds the <code>opcode</code> value of the <code>lshr</code> instruction, 0x7b.
                 \sa The instruction description  <a lshr="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lshr">lshr</a>. */
    OPCODE_IUSHR,          /** Holds the <code>opcode</code> value of the <code>iushr</code> instruction, 0x7c.
                 \sa The instruction description  <a iushr="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iushr">iushr</a>. */
    OPCODE_LUSHR,          /** Holds the <code>opcode</code> value of the <code>lushr</code> instruction, 0x7d.
                 \sa The instruction description  <a lushr="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lushr">lushr</a>. */
    OPCODE_IAND,           /** Holds the <code>opcode</code> value of the <code>iand</code> instruction, 0x7e.
                 \sa The instruction description  <a iand="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iand">iand</a>. */
    OPCODE_LAND,           /** Holds the <code>opcode</code> value of the <code>land</code> instruction, 0x7f.
                 \sa The instruction description  <a land="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#land">land</a>. */
    OPCODE_IOR,            /** Holds the <code>opcode</code> value of the <code>ior</code> instruction, 0x80.
                 \sa The instruction description  <a ior="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ior">ior</a>. */
    OPCODE_LOR,            /** Holds the <code>opcode</code> value of the <code>lor</code> instruction, 0x81.
                 \sa The instruction description  <a lor="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lor">lor</a>. */
    OPCODE_IXOR,           /** Holds the <code>opcode</code> value of the <code>ixor</code> instruction, 0x82.
                 \sa The instruction description  <a ixor="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ixor">ixor</a>. */
    OPCODE_LXOR,           /** Holds the <code>opcode</code> value of the <code>lxor</code> instruction, 0x83.
                 \sa The instruction description  <a lxor="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lxor">lxor</a>. */
    OPCODE_IINC,           /** Holds the <code>opcode</code> value of the <code>iinc</code> instruction, 0x84 + u1|u2 + s1|s2.
                 \sa The instruction description  <a iinc="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#iinc">iinc</a>. */
    OPCODE_I2L,            /** Holds the <code>opcode</code> value of the <code>i2l</code> instruction, 0x85.
                 \sa The instruction description  <a i2l="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#i2l">i2l</a>. */
    OPCODE_I2F,            /** Holds the <code>opcode</code> value of the <code>i2f</code> instruction, 0x86.
                 \sa The instruction description  <a i2f="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#i2f">i2f</a>. */
    OPCODE_I2D,            /** Holds the <code>opcode</code> value of the <code>i2d</code> instruction, 0x87.
                 \sa The instruction description  <a i2d="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#i2d">i2d</a>. */
    OPCODE_L2I,            /** Holds the <code>opcode</code> value of the <code>l2i</code> instruction, 0x88.
                 \sa The instruction description  <a l2i="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#l2i">l2i</a>. */
    OPCODE_L2F,            /** Holds the <code>opcode</code> value of the <code>l2f</code> instruction, 0x89.
                 \sa The instruction description  <a l2f="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#l2f">l2f</a>. */
    OPCODE_L2D,            /** Holds the <code>opcode</code> value of the <code>l2d</code> instruction, 0x8a.
                 \sa The instruction description  <a l2d="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#l2d">l2d</a>. */
    OPCODE_F2I,            /** Holds the <code>opcode</code> value of the <code>f2i</code> instruction, 0x8b.
                 \sa The instruction description  <a f2i="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#f2i">f2i</a>. */
    OPCODE_F2L,            /** Holds the <code>opcode</code> value of the <code>f2l</code> instruction, 0x8c.
                 \sa The instruction description  <a f2l="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#f2l">f2l</a>. */
    OPCODE_F2D,            /** Holds the <code>opcode</code> value of the <code>f2d</code> instruction, 0x8d.
                 \sa The instruction description  <a f2d="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#f2d">f2d</a>. */
    OPCODE_D2I,            /** Holds the <code>opcode</code> value of the <code>d2i</code> instruction, 0x8e.
                 \sa The instruction description  <a d2i="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#d2i">d2i</a>. */
    OPCODE_D2L,            /** Holds the <code>opcode</code> value of the <code>d2l</code> instruction, 0x8f.
                 \sa The instruction description  <a d2l="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#d2l">d2l</a>. */
    OPCODE_D2F,            /** Holds the <code>opcode</code> value of the <code>d2f</code> instruction, 0x90.
                 \sa The instruction description  <a d2f="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#d2f">d2f</a>. */
    OPCODE_I2B,            /** Holds the <code>opcode</code> value of the <code>i2b</code> instruction, 0x91.
                 \sa The instruction description  <a i2b="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#i2b">i2b</a>. */
    OPCODE_I2C,            /** Holds the <code>opcode</code> value of the <code>i2c</code> instruction, 0x92.
                 \sa The instruction description  <a i2c="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#i2c">i2c</a>. */
    OPCODE_I2S,            /** Holds the <code>opcode</code> value of the <code>i2s</code> instruction, 0x93.
                 \sa The instruction description  <a i2s="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#i2s">i2s</a>. */
    OPCODE_LCMP,           /** Holds the <code>opcode</code> value of the <code>lcmp</code> instruction, 0x94.
                 \sa The instruction description  <a lcmp="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lcmp">lcmp</a>. */
    OPCODE_FCMPL,          /** Holds the <code>opcode</code> value of the <code>fcmpl</code> instruction, 0x95.
                 \sa The instruction description  <a fcmpl="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fcmpop">fcmpl</a>. */
    OPCODE_FCMPG,          /** Holds the <code>opcode</code> value of the <code>fcmpg</code> instruction, 0x96.
                 \sa The instruction description  <a fcmpg="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#fcmpop">fcmpg</a>. */
    OPCODE_DCMPL,          /** Holds the <code>opcode</code> value of the <code>dcmpl</code> instruction, 0x97.
                 \sa The instruction description  <a dcmpl="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dcmpop">dcmpl</a>. */
    OPCODE_DCMPG,          /** Holds the <code>opcode</code> value of the <code>dcmpg</code> instruction, 0x98.
                 \sa The instruction description  <a dcmpg="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dcmpop">dcmpg</a>. */
    OPCODE_IFEQ,           /** Holds the <code>opcode</code> value of the <code>ifeq</code> instruction, 0x99 + s2 (c).
                 \sa The instruction description  <a ifeq="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifcond">ifeq</a>. */
    OPCODE_IFNE,           /** Holds the <code>opcode</code> value of the <code>ifne</code> instruction, 0x9a + s2 (c).
                 \sa The instruction description  <a ifne="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifcond">ifne</a>. */
    OPCODE_IFLT,           /** Holds the <code>opcode</code> value of the <code>iflt</code> instruction, 0x9b + s2 (c).
                 \sa The instruction description  <a iflt="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifcond">iflt</a>. */
    OPCODE_IFGE,           /** Holds the <code>opcode</code> value of the <code>ifge</code> instruction, 0x9c + s2 (c).
                 \sa The instruction description  <a ifge="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifcond">ifge</a>. */
    OPCODE_IFGT,           /** Holds the <code>opcode</code> value of the <code>ifgt</code> instruction, 0x9d + s2 (c).
                 \sa The instruction description  <a ifgt="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifcond">ifgt</a>. */
    OPCODE_IFLE,           /** Holds the <code>opcode</code> value of the <code>ifle</code> instruction, 0x9e + s2 (c).
                 \sa The instruction description  <a ifle="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifcond">ifle</a>. */
    OPCODE_IF_ICMPEQ,      /** Holds the <code>opcode</code> value of the <code>if_icmpeq</code> instruction, 0x9f + s2 (c).
                 \sa The instruction description  <a if_icmpeq="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_cmpcond">if_icmpeq</a>. */
    OPCODE_IF_ICMPNE,      /** Holds the <code>opcode</code> value of the <code>if_icmpne</code> instruction, 0xa0 + s2 (c).
                 \sa The instruction description  <a if_icmpne="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_cmpcond">if_icmpne</a>. */
    OPCODE_IF_ICMPLT,      /** Holds the <code>opcode</code> value of the <code>if_icmplt</code> instruction, 0xa1 + s2 (c).
                 \sa The instruction description  <a if_icmplt="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_cmpcond">if_icmplt</a>. */
    OPCODE_IF_ICMPGE,      /** Holds the <code>opcode</code> value of the <code>if_icmpge</code> instruction, 0xa2 + s2 (c).
                 \sa The instruction description  <a if_icmpge="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_cmpcond">if_icmpge</a>. */
    OPCODE_IF_ICMPGT,      /** Holds the <code>opcode</code> value of the <code>if_icmpgt</code> instruction, 0xa3 + s2 (c).
                 \sa The instruction description  <a if_icmpgt="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_cmpcond">if_icmpgt</a>. */
    OPCODE_IF_ICMPLE,      /** Holds the <code>opcode</code> value of the <code>if_icmple</code> instruction, 0xa4 + s2 (c).
                 \sa The instruction description  <a if_icmple="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_cmpcond">if_icmple</a>. */
    OPCODE_IF_ACMPEQ,      /** Holds the <code>opcode</code> value of the <code>if_acmpeq</code> instruction, 0xa5 + s2 (c).
                 \sa The instruction description  <a if_acmpeq="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_acmpcond">if_acmpeq</a>. */
    OPCODE_IF_ACMPNE,      /** Holds the <code>opcode</code> value of the <code>if_acmpne</code> instruction, 0xa6 + s2 (c).
                 \sa The instruction description  <a if_acmpne="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#if_acmpcond">if_acmpne</a>. */
    OPCODE_GOTO,           /** Holds the <code>opcode</code> value of the <code>goto</code> instruction, 0xa7 + s2 (c).
                 \sa The instruction description  <a goto="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc5.html#goto">goto</a>. */
    OPCODE_JSR,            /** Holds the <code>opcode</code> value of the <code>jsr</code> instruction, 0xa8 + s2 (c).
                 \sa The instruction description  <a jsr="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc7.html#jsr">jsr</a>. */
    OPCODE_RET,            /** Holds the <code>opcode</code> value of the <code>ret</code> instruction, 0xa9 + u1|u2 (c).
                 \sa The instruction description  <a ret="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc12.html#ret">ret</a>. */
    OPCODE_TABLESWITCH,    /** Holds the <code>opcode</code> value of the <code>tableswitch</code> instruction, 0xaa + pad + s4 * (3 + N) (c).
                 \sa The instruction description  <a tableswitch="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc14.html#tableswitch">tableswitch</a>. */
    OPCODE_LOOKUPSWITCH,  /** Holds the <code>opcode</code> value of the <code>lookupswitch</code> instruction, 0xab +pad +s4 * 2 * (N + 1) (c).
                 \sa The instruction description  <a lookupswitch="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lookupswitch">lookupswitch</a>. */
    OPCODE_IRETURN,        /** Holds the <code>opcode</code> value of the <code>ireturn</code> instruction, 0xac (c).
                 \sa The instruction description  <a ireturn="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ireturn">ireturn</a>. */
    OPCODE_LRETURN,        /** Holds the <code>opcode</code> value of the <code>lreturn</code> instruction, 0xad (c).
                 \sa The instruction description  <a lreturn="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc8.html#lreturn">lreturn</a>. */
    OPCODE_FRETURN,        /** Holds the <code>opcode</code> value of the <code>freturn</code> instruction, 0xae (c).
                 \sa The instruction description  <a freturn="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc4.html#freturn">freturn</a>. */
    OPCODE_DRETURN,        /** Holds the <code>opcode</code> value of the <code>dreturn</code> instruction, 0xaf (c).
                 \sa The instruction description  <a dreturn="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc3.html#dreturn">dreturn</a>. */
    OPCODE_ARETURN,        /** Holds the <code>opcode</code> value of the <code>areturn</code> instruction, 0xb0 (c).
                 \sa The instruction description  <a dreturn="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#areturn">areturn</a>. */
    OPCODE_RETURN,         /** Holds the <code>opcode</code> value of the <code>return</code> instruction, 0xb1 (c).
                 \sa The instruction description  <a return="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc12.html#return">return</a>. */
    OPCODE_GETSTATIC,      /** Holds the <code>opcode</code> value of the <code>getstatic</code> instruction, 0xb2 + u2.
                 \sa The instruction description  <a getstatic="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc5.html#getstatic">getstatic</a>. */
    OPCODE_PUTSTATIC,      /** Holds the <code>opcode</code> value of the <code>putstatic</code> instruction, 0xb3 + u2.
                 \sa The instruction description  <a putstatic="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc11.html#putstatic">putstatic</a>. */
    OPCODE_GETFIELD,       /** Holds the <code>opcode</code> value of the <code>getfield</code> instruction, 0xb4 + u2.
                 \sa The instruction description  <a getfield="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc5.html#getfield">getfield</a>. */
    OPCODE_PUTFIELD,       /** Holds the <code>opcode</code> value of the <code>putfield</code> instruction, 0xb5 + u2.
                 \sa The instruction description  <a putfield="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc11.html#putfield">putfield</a>. */
    OPCODE_INVOKEVIRTUAL,  /** Holds the <code>opcode</code> value of the <code>invokevirtual</code> instruction, 0xb6 + u2.
                 \sa The instruction description  <a invokevirtual="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#invokevirtual">invokevirtual</a>. */
    OPCODE_INVOKESPECIAL,  /** Holds the <code>opcode</code> value of the <code>invokespecial</code> instruction, 0xb7 + u2.
                 \sa The instruction description  <a invokespecial="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#invokespecial">invokespecial</a>. */
    OPCODE_INVOKESTATIC,   /** Holds the <code>opcode</code> value of the <code>invokestatic</code> instruction, 0xb8 + u2.
                 \sa The instruction description  <a invokestatic="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#invokestatic">invokestatic</a>. */
    OPCODE_INVOKEINTERFACE, /** Holds the <code>opcode</code> value of the <code>invokeinterface</code> instruction, 0xb9 + u2 + u1 + u1.
                 \sa The instruction description  <a invokeinterface="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#invokeinterface">invokeinterface</a>. */
    _OPCODE_UNDEFINED,     /** Holds the <code>opcode</code> value of the <code>unused</code> instruction, 0xba. */
    OPCODE_NEW,            /** Holds the <code>opcode</code> value of the <code>new</code> instruction, 0xbb + u2.
                 \sa The instruction description  <a new="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc10.html#new">new</a>. */
    OPCODE_NEWARRAY,       /** Holds the <code>opcode</code> value of the <code>newarray</code> instruction, 0xbc + u1.
                 \sa The instruction description  <a newarray="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc10.html#newarray">newarray</a>. */
    OPCODE_ANEWARRAY,      /** Holds the <code>opcode</code> value of the <code>anewarray</code> instruction, 0xbd + u1.
                 \sa The instruction description  <a anewarray="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#anewarray">anewarray</a>. */
    OPCODE_ARRAYLENGTH,    /** Holds the <code>opcode</code> value of the <code>arraylength</code> instruction, 0xbe.
                 \sa The instruction description  <a arraylength="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#arraylength">arraylength</a>. */
    OPCODE_ATHROW,         /** Holds the <code>opcode</code> value of the <code>athrow</code> instruction, 0xbf (c).
                 \sa The instruction description  <a athrow="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc.html#athrow">athrow</a>. */
    OPCODE_CHECKCAST,      /** Holds the <code>opcode</code> value of the <code>checkcast</code> instruction, 0xc0 + u2.
                 \sa The instruction description  <a checkcast="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc2.html#checkcast">checkcast</a>. */
    OPCODE_INSTANCEOF,     /** Holds the <code>opcode</code> value of the <code>instanceof</code> instruction, 0xc1 + u2.
                 \sa The instruction description  <a instanceof="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#instanceof">instanceof</a>. */
    OPCODE_MONITORENTER,   /** Holds the <code>opcode</code> value of the <code>monitorenter</code> instruction, 0xc2.
                 \sa The instruction description  <a monitorenter="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc9.html#monitorenter">monitorenter</a>. */
    OPCODE_MONITOREXIT,    /** Holds the <code>opcode</code> value of the <code>monitorexit</code> instruction, 0xc3.
                 \sa The instruction description  <a monitorexit="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc9.html#monitorexit">monitorexit</a>. */
    OPCODE_WIDE,           /** Holds the <code>opcode</code> value of the <code>wide</code> instruction, 0xc4.
                 \sa The instruction description  <a wide="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc15.html#wide">wide</a>. */
    OPCODE_MULTIANEWARRAY, /** Holds the <code>opcode</code> value of the <code>multianewarray</code> instruction, 0xc5 + u2 + u1.
                 \sa The instruction description  <a multianewarray="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc9.html#multianewarray">multianewarray</a>. */
    OPCODE_IFNULL,         /** Holds the <code>opcode</code> value of the <code>ifnull</code> instruction, 0xc6 + s2 (c).
                 \sa The instruction description  <a ifnull="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifnull">ifnull</a>. */
    OPCODE_IFNONNULL,      /** Holds the <code>opcode</code> value of the <code>ifnonnull</code> instruction, 0xc7 + s2 (c).
                 \sa The instruction description  <a ifnonnull="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc6.html#ifnonnull">ifnonnull</a>. */
    OPCODE_GOTO_W,         /** Holds the <code>opcode</code> value of the <code>goto_w</code> instruction, 0xc8 + s4 (c).
                 \sa The instruction description  <a goto_w="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc5.html#goto_w">goto_w</a>. */
    OPCODE_JSR_W,          /** Holds the <code>opcode</code> value of the <code>jsr_w instruction</code>, 0xc9 + s4 (c).
                 \sa The instruction description  <a jsr_w="el" href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Instructions2.doc7.html#jsr_w">jsr_w</a>. */
    OPCODE_COUNT,          /** Holds the number of bytecodes, 0xca. */

    // extended bytecodes
    OPCODE_BREAKPOINT = OPCODE_COUNT    /** Holds the <code>opcode</code> value of the extended instruction, 0xca. */ 

#ifdef FAST_BYTECODES
    ,
    OPCODE_FAST_GETFIELD_REF, /* 0xcb */
    OPCODE_FAST_GETFIELD_INT, /* 0xcc */
#endif /* FAST_BYTECODES */
};
#endif // _BYTECODES_H_


