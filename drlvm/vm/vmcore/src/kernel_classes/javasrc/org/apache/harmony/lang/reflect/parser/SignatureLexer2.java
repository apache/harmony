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

package org.apache.harmony.lang.reflect.parser;

import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.Token;

/**
 * @author Serguei S. Zapreyev
 *
 * NOTE. Initially this Signature Parser was created and debugged using lex and yacc on Linux:
 * 
 *    -bash-3.00$ uname -a
 *    Linux nstdrlel8.ins.intel.com 2.6.9-11.ELsmp #1 SMP Fri May 20 18:25:30 EDT 2005 x86_64 x86_64 x86_64 GNU/Linux
 *    -bash-3.00$ which yacc
 *    /usr/bin/yacc
 *    -bash-3.00$ which lex
 *    /usr/bin/lex
 *    -bash-3.00$ lex --version
 *    lex version 2.5.4
 * 
 * then it was rewritten for ANTLR 2.7.5 (http://www.antlr.org/) and redebugged:
 * 
 *    // $ANTLR 2.7.5 (20050128): "signature.g" -> "SignatureParser.java"$
 */
public final class SignatureLexer2 extends antlr.CharScanner implements SignatureParserTokenTypes, TokenStream
 {

String ident = null; // to keep previous symbol if it's identifier

int prevLexeme = -1;// to distinguish ID and TVAR

int stackDepth = 0; // the current acheived depth of the parsered nested parameterized types chain (ParameterizedType1<ParameterizedType2<...>;ParameterizedType3<...>>;)
int Lflag2; // to distinguish ID and TBASE
int Lflag3; // to distinguish ID and TBASE
int trnglsCount = 0;

boolean DEBUGGING = false;

String sgntr;
int ind;
int lexlen;

public SignatureLexer2(String sig) {
    sgntr = sig;
    ind = 0;
    lexlen = 0;
}

public Token nextToken() throws TokenStreamException {
    if (ind >= sgntr.length()) {
        return new MToken(MToken.EOF_TYPE, "the end");
    }
    if (DEBUGGING) {
        //System.out.println("nextToken1:"+sgntr);
        //System.out.println("nextToken1:"+sgntr.charAt(ind));
    }
    MToken theRetToken=null;
        try {
                switch ( sgntr.charAt(ind) ) {
                case '*':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:STAR_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    prevLexeme = STAR_SIGN;
                    theRetToken=new MToken(STAR_SIGN, "*");
                    ind++;
                    break;
                }
                case '+':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:PLUS_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    prevLexeme = PLUS_SIGN;
                    theRetToken=new MToken(PLUS_SIGN, "+");
                    ind++;
                    break;
                }
                case '-':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:MINUS_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    prevLexeme = MINUS_SIGN;
                    theRetToken=new MToken(MINUS_SIGN, "-");
                    ind++;
                    break;
                }
                case '[':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:SQUAREOPEN_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    prevLexeme = SQUAREOPEN_SIGN;
                    theRetToken=new MToken(SQUAREOPEN_SIGN, "[");
                    ind++;
                    break;
                }
                case ':':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:COLON_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    prevLexeme = COLON_SIGN;
                    theRetToken=new MToken(COLON_SIGN, ":");
                    ind++;
                    break;
                }
                case '.':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:DOT_OR_DOLLAR_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    prevLexeme = DOT_OR_DOLLAR_SIGN;
                    theRetToken=new MToken(DOT_OR_DOLLAR_SIGN, ".");
                    ind++;
                    break;
                }
                case '<':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:TRIANGLEOPEN_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    if (Lflag2 == 1) trnglsCount++;
                    prevLexeme = TRIANGLEOPEN_SIGN;
                    theRetToken=new MToken(TRIANGLEOPEN_SIGN, "<");
                    ind++;
                    break;
                }
                case '>':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:TRIANGLECLOSE_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    if (Lflag2 == 1) trnglsCount--;
                    prevLexeme = TRIANGLECLOSE_SIGN;
                    theRetToken=new MToken(TRIANGLECLOSE_SIGN, ">");
                    ind++;
                    break;
                }
                case '^':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:CNTRL_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    Lflag2 = 0;
                    prevLexeme = CNTRL_SIGN;
                    theRetToken=new MToken(CNTRL_SIGN, "^");
                    ind++;
                    break;
                }
                case ';':
                {
                    if (DEBUGGING) {
                        System.out.println(".............lex:SEMICOLON_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                    }
                    if (Lflag2 == 1 && trnglsCount == 0) {
                        Lflag3 = 0;
                    }
                    prevLexeme = SEMICOLON_SIGN;
                    theRetToken=new MToken(SEMICOLON_SIGN, ";");
                    ind++;
                    break;
                }
                case '(':
                {
                        if (DEBUGGING) {
                            System.out.println(".............lex:RINGOPEN_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                        }
                    Lflag2 = 1;
                    prevLexeme = RINGOPEN_SIGN;
                    theRetToken=new MToken(RINGOPEN_SIGN, "(");
                    ind++;
                    break;
                }
                case ')':
                {
                        if (DEBUGGING) {
                            System.out.println(".............lex:RINGCLOSE_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                        }
                    prevLexeme = RINGCLOSE_SIGN;
                    theRetToken=new MToken(RINGCLOSE_SIGN, ")");
                    ind++;
                    break;
                }
                case 'V':
                {
                        if (Lflag2 == 1 && trnglsCount == 0 && prevLexeme != PACKAGE_SPECIFIER && prevLexeme != DOT_OR_DOLLAR_SIGN) {
                            if (DEBUGGING) {
                                System.out.println(".............lex:VOIDTYPE" );
                            }
                            prevLexeme = VOIDTYPE;
                            theRetToken=new MToken(VOIDTYPE, "V");
                            ind++;
                        } else if (isID_COLON()){
                            ident = sgntr.substring(ind, ind+lexlen-1); // last ":" should be recovered in stream
                            if (DEBUGGING) {
                                System.out.println(".............lex:ID_COLON:\""+ident+"\"");
                            }
                            prevLexeme = ID_COLON;
                            ind += lexlen - 1;
                            theRetToken=new MToken(ID_COLON, ident);
                        } else if (isIDwoL()){
                            ident = sgntr.substring(ind, ind+lexlen);
                            if (DEBUGGING) {
                                System.out.println(".............lex:ID:\""+ident+"\"");
                            }
                            prevLexeme = ID;
                            ind += lexlen;
                            theRetToken=new MToken(ID, ident);
                        } else {
                            theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                            throw new Exception();
                        }
                        break;
                }
                case 'B':  case 'C':
                case 'D':  case 'F':
                case 'I':  case 'J':
                case 'S':  case 'Z':
                {
                    if (Lflag2 == 1 && Lflag3 != 1 || prevLexeme == SQUAREOPEN_SIGN) {
                        if (DEBUGGING) {
                            System.out.println(".............lex:TBASE: \""+ String.valueOf(sgntr.charAt(ind))+"\"");
                        }
                        if (ind == sgntr.length()-1 || sgntr.charAt(ind+1) == '^'){
                         prevLexeme = RETURN_BASE_TYPE ;
                         theRetToken=new MToken(RETURN_BASE_TYPE , String.valueOf(sgntr.charAt(ind)));
                        } else {
                         prevLexeme = TBASE ;
                         theRetToken=new MToken(TBASE , String.valueOf(sgntr.charAt(ind)));
                        }
                        ind++;
                    } else if (isID_COLON()){
                        ident = sgntr.substring(ind, ind+lexlen-1); // last ":" should be recovered in stream
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID_COLON:\""+ident+"\"");
                        }
                        prevLexeme = ID_COLON;
                         theRetToken=new MToken(ID_COLON, ident);
                        ind += lexlen - 1;
                    } else if (isIDwoL()){
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID:\""+ident+"\"");
                        }
                        prevLexeme = ID;
                        theRetToken=new MToken(ID, ident);
                        ind += lexlen;
                    } else {
                         theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                         throw new Exception();
                    }
                    break;
                }
                case 'T':
                {
                    if (isTV()) {
                      if (prevLexeme == PACKAGE_SPECIFIER || prevLexeme == DOT_OR_DOLLAR_SIGN) {
                        ident = sgntr.substring(ind, ind+lexlen-1);// - ";"
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID:\""+ident+"\"");
                        }
                        prevLexeme = ID ;
                        theRetToken=new MToken(ID , ident);
                        ind += lexlen - 1;
                      } else { 
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:TVAR:\""+ident+"\"");
                        }
                        prevLexeme = TVAR;
                        theRetToken=new MToken(TVAR, ident);
                           ind += lexlen;
                      }
                    } else if (isID_COLON()){
                        ident = sgntr.substring(ind, ind+lexlen-1); // last ":" should be recovered in stream
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID_COLON:\""+ident+"\"");
                        }
                        prevLexeme = ID_COLON;
                         theRetToken=new MToken(ID_COLON, ident);
                        ind += lexlen - 1;
                    } else if (isIDwoL()){
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID:\""+ident+"\"");
                        }
                        prevLexeme = ID;
                         theRetToken=new MToken(ID, ident);
                        ind += lexlen;
                    } else {
                         theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                         throw new Exception();
                    }
                    break;
                }
                case 'L':
                {
                    if (isPACKAGE_SPECIFIER()) {
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:PACKAGE_SPECIFIER:\""+ident+"\"");
                        }
                        if (Lflag2 == 1) {
                            Lflag3 = 1;
                        }
                        prevLexeme = PACKAGE_SPECIFIER ;
                        theRetToken=new MToken(PACKAGE_SPECIFIER , ident);
                        ind += lexlen;
                    } else if (isID_COLON()){
                        ident = sgntr.substring(ind, ind+lexlen-1); // last ":" should be recovered in stream
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID_COLON:\""+ident+"\"");
                        }
                        prevLexeme = ID_COLON;
                         theRetToken=new MToken(ID_COLON, ident);
                        ind += lexlen - 1;
                    } else if (isIDwL()){
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID:\""+ident+"\"");
                        }
                        prevLexeme = ID;
                        theRetToken=new MToken(ID, ident);
                        ind += lexlen;
                    } else {
                         theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                         throw new Exception();
                    }
                    break;
                }
                case '$':
                {
                    if (prevLexeme == TRIANGLECLOSE_SIGN) {
                        if (DEBUGGING) {
                            System.out.println(".............lex:DOT_OR_DOLLAR_SIGN:\""+String.valueOf(sgntr.charAt(ind))+"\"");
                        }
                        prevLexeme = DOT_OR_DOLLAR_SIGN;
                        theRetToken=new MToken(DOT_OR_DOLLAR_SIGN, "$");
                        ind++;
                    } else if (isID_COLON()){
                        ident = sgntr.substring(ind, ind+lexlen-1); // last ":" should be recovered in stream
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID_COLON:\""+ident+"\"");
                        }
                        prevLexeme = ID_COLON;
                         theRetToken=new MToken(ID_COLON, ident);
                        ind += lexlen - 1;
                    } else if (isIDwoL()){
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID:\""+ident+"\"");
                        }
                        prevLexeme = ID;
                         theRetToken=new MToken(ID, ident);
                        ind += lexlen;
                    } else {
                         theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                         throw new Exception();
                    }
                    break;
                }
                case 'A':  case 'E':  case 'G':  case 'H':
                case 'K':  case 'M':  case 'N':  case 'O':
                case 'P':  case 'Q':  case 'R':  case 'U':
                case 'W':  case 'X':  case 'Y':  case '\\':
                case '_':  case 'a':  case 'b':  case 'c':
                case 'd':  case 'e':  case 'f':  case 'g':
                case 'h':  case 'i':  case 'j':  case 'k':
                case 'l':  case 'm':  case 'n':  case 'o':
                case 'p':  case 'q':  case 'r':  case 's':
                case 't':  case 'u':  case 'v':  case 'w':
                case 'x':  case 'y':  case 'z':
                {
                    if (isID_COLON()){
                        ident = sgntr.substring(ind, ind+lexlen-1); // last ":" should be recovered in stream
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID_COLON:\""+ident+"\"");
                        }
                        prevLexeme = ID_COLON;
                         theRetToken=new MToken(ID_COLON, ident);
                        ind += lexlen - 1;
                    } else if (isIDwoL()){
                        ident = sgntr.substring(ind, ind+lexlen);
                        if (DEBUGGING) {
                            System.out.println(".............lex:ID:\""+ident+"\"");
                        }
                        prevLexeme = ID;
                         theRetToken=new MToken(ID, ident);
                        ind += lexlen;
                    } else {
                         theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                         throw new Exception();
                    }
                    break;
                }
                default:
                {
                         theRetToken=new MToken(MToken.INVALID_TYPE, "figvam");
                         throw new Exception();
                }
                }
                if (DEBUGGING) {
                    //new Throwable().printStackTrace();
                    System.out.println("------------nextToken2:\""+theRetToken.getText()+"\"");
                    //System.out.println("nextToken2:"+theRetToken.getType());
                    //System.out.println("nextToken2:"+ind);
                }
                return theRetToken;
        }
        catch (Exception e) {
            /**/e.printStackTrace();
            /**/System.err.println("===nextToken===: "+e.toString());
            throw new TokenStreamException(e.getMessage());
        }
    }

    private boolean isTV() {
        if (DEBUGGING) {
            System.out.println(".............isTV:"+ind);
        }
        String ns = sgntr.substring(ind).replaceFirst("T((\\\\[a-f0-9]{3})|[A-Za-z_$]){1}((\\\\[a-f0-9]{3})|[A-Za-z_$0-9])*;" , "#");
        if (ns.charAt(0) == '#') {
            int i = sgntr.indexOf(ns.substring(1), ind);
            lexlen = (i == ind)? sgntr.length() - ind : i - ind;
            return true;
        }                                                             
        return false;
    }

    private boolean isIDwoL() {
        if (DEBUGGING) {
            System.out.println(".............isIDwoL:"+ind+"|"+sgntr);
        }
        String ns = sgntr.substring(ind).replaceFirst("((\\\\[a-f0-9]{3})|[A-KM-Za-z_$]){1}((\\\\[a-f0-9]{3})|[A-Za-z_$0-9])*" , "#");
        if (ns.charAt(0) == '#') {
            int i = sgntr.indexOf(ns.substring(1), ind);
            lexlen = (i == ind)? sgntr.length() - ind : i - ind;
            return true;
        }                                                             
        return false;
    }
    
    private boolean isID_COLON() {
        if (DEBUGGING) {
            System.out.println(".............isID_COLON:"+ind);
        }
        String ns = sgntr.substring(ind).replaceFirst("((\\\\[a-f0-9]{3})|[A-Za-z_$]){1}((\\\\[a-f0-9]{3})|[A-Za-z_$0-9])*::" , "#");
        if (ns.charAt(0) == '#') {
            int i = sgntr.indexOf(ns.substring(1), ind);
            lexlen = (i == ind)? sgntr.length() - ind : i - ind;
            return true;
        }                                                             
        return false;
    }
    
    private boolean isPACKAGE_SPECIFIER() {
        if (DEBUGGING) {
            System.out.println(".............isPACKAGE_SPECIFIER:"+ind);
        }
        String ns = sgntr.substring(ind).replaceFirst("L((\\\\[a-f0-9]{3})|[A-Za-z_$]){1}((\\\\[a-f0-9]{3})|[A-Za-z_$0-9/])*/" , "#");
        if (ns.charAt(0) == '#') {
            int i = sgntr.indexOf(ns.substring(1), ind);
            lexlen = (i == ind)? sgntr.length() - ind : i - ind;
            return true;
        }                                                             
        return false;
    }

    private boolean isIDwL() {
        if (DEBUGGING) {
            System.out.println(".............isIDwL:"+ind);
        }
        String ns = sgntr.substring(ind).replaceFirst("L((\\\\[a-f0-9]{3})|[A-Za-z_$0-9])*" , "#");
        if (ns.charAt(0) == '#') {
            int i = sgntr.indexOf(ns.substring(1), ind);
            lexlen = (i == ind)? sgntr.length() - ind : i - ind;
            return true;
        }                                                             
        return false;
    }
}

class MToken extends Token {
    String txt;

    public MToken(int t, String txt) {
        super();
        super.type = t;
        setText(txt);
    }

    public String getText() {
        return txt;
    }

    public void setText(String t) {
        txt = t;
    }
}