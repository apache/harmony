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
 
// $ANTLR 2.7.5 (20050128): "signature.g" -> "SignatureParser.java"$

package org.apache.harmony.lang.reflect.parser;

/**
 * @author Serguei S. Zapreyev
 *
 * NOTE 1. This signature.g was created and debugged using 
 * -bash-3.00$ uname -a
 * Linux nstdrlel8.ins.intel.com 2.6.9-11.ELsmp #1 SMP Fri May 20 18:25:30 EDT 2005 x86_64 x86_64 x86_64 GNU/Linux
 * -bash-3.00$ which yacc
 * /usr/bin/yacc
 * -bash-3.00$ which lex
 * /usr/bin/lex
 * -bash-3.00$ lex --version
 * lex version 2.5.4
 * 
 * then it was rewritten for ANTLR 2.7.5
 * 
 * 
 * To generate java code of signature syntax parser (consisting of SignatureParser.java and SignatureParserTokenTypes.java)
 * you should 
 * - enter to ...\tiger-dev\vm\vmcore\src\kernel_classes\javasrc\org\apache\harmony\lang\reflect\parser directory:
 *    cd C:\IJE\tiger-dev\vm\vmcore\src\kernel_classes\javasrc\org\apache\harmony\lang\reflect\parser
 * - set pointer to ANTLR:
 *    set CLASSPATH=C:\Documents and Settings\szapreye\My Documents\ANTLR\antlr-2.7.5.jar;.
 * - start 1.5 java VM:
 *    java antlr.Tool signature.g
 * 
 * It provides the creation of SignatureParser.java and SignatureParserTokenTypes.java which in joining with lexer (SignatureLexer2.java)
 * arrange the generic signature attribute parser.
 */

public interface SignatureParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int TRIANGLEOPEN_SIGN = 4;
	int TRIANGLECLOSE_SIGN = 5;
	int ID_COLON = 6;
	int ID = 7;
	int COLON_SIGN = 8;
	int TVAR = 9;
	int SEMICOLON_SIGN = 10;
	int PACKAGE_SPECIFIER = 11;
	int STAR_SIGN = 12;
	int PLUS_SIGN = 13;
	int MINUS_SIGN = 14;
	int DOT_OR_DOLLAR_SIGN = 15;
	int SQUAREOPEN_SIGN = 16;
	int TBASE = 17;
	int VOIDTYPE = 18;
	int RETURN_BASE_TYPE = 19;
	int RINGOPEN_SIGN = 20;
	int RINGCLOSE_SIGN = 21;
	int CNTRL_SIGN = 22;
}
