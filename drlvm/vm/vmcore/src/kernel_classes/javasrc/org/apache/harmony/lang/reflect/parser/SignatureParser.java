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

import java.util.ArrayList;
import java.lang.reflect.GenericSignatureFormatError;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

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
 *
 */
public class SignatureParser extends antlr.LLkParser       implements SignatureParserTokenTypes
 {

private SignatureLexer2 lexer;
private int declType; // prescribed (predefined in parser's args) type of the parsered declaration
private static final int CLASSoDECL = 1; //Parser.SignatureKind.CLASS_SIGNATURE.value();
private static final int FIELDoDECL = 2; //Parser.SignatureKind.FIELD_SIGNATURE.value();
private static final int METHODoDECL = 3; //Parser.SignatureKind.METHOD_SIGNATURE.value();
private static final int CONSTRUCTORoDECL = 4; //Parser.SignatureKind.CONSTRUCTOR_SIGNATURE.value();

private ArrayList<InterimTypeParameter> genParamList = null; // to accumulate generic parameters
private ArrayList<InterimType> boundList = null; // to accumulate bounds of a generic parameter
private ArrayList<InterimType> methParamList = null; // to accumulate method's or constructor's parameters
private ArrayList<InterimType> thrownList = null; // to accumulate exceptions thrown by a method/constructor
private ArrayList<InterimType> implList = null; // to accumulate implements-clauses of a class decl

private InterimClassGenericDecl classDecl = null;
private InterimFieldGenericDecl fieldDecl = null;
private InterimMethodGenericDecl methodDecl = null;
private InterimConstructorGenericDecl constructorDecl = null;

private InterimTypeParameter currentTypeParameter = null;
private int Yflag1 = 0; // to mark the start of generic type parameters processing
private int Yflag2 = 0; // to mark the start of method's/constructor's throws-part processing
class PTStack {          // stack of parsered nested parameterized types
    String gatheredStr;  // packageSpecifier / packageSpecifier + ID / packageSpecifier + ID {+ "$"ID}
    int gthrdStrLen;     // length of the allocated gatheredStr;
    int wrappInWildcard; // -1 - not to wrapp; 1 - lower; 0 - upper
    int typeKind;        // previous level type kind: 1 - InterimParameterizedType; 0 - InterimClassType
    InterimClassType rawType;     // InterimClassType
    InterimType owner;       // InterimParameterizedType or InterimClassType
    ArrayList<InterimType> args;       // InterimParameterizedType or InterimClassType or InterimTypeVariable or InterimWildcardType
    int  sigBegin;       // start of the signature of the current nested parameterized type within CTPTsignature
    int  sigEnd;         // finish of the signature of the current nested parameterized type within CTPTsignature
    int dim;             // to indicate the number of consequent "[" symbols. ATTENTION: it used for all types: TVAR, TBASE, RETURN_BASE_TYPE, class type, parameterized type
    PTStack nextLevel;
    boolean pendingTypeArg; // crutch to fix HARMONY-5752
};
private PTStack stack;
private PTStack currentStackElem; // points to the current processed level element of the nested parameterized types chain

private String CTPTsignature; // accumulated signature of the current parsered parameterized type, it's used for parameterized types repository
private int  sigInd;

private InterimType/*InterimGenericType*/ highLevelType; // the rolled up reference (InterimParameterizedType or InterimClassType)

private int i;
private InterimType prsrT;
private int len;
private PTStack p1, p2, currentStackElemCopy;
private InterimClassType upper;
//////////////////////////////////////////////////////////////////////////////////////////////////////
private void throwGenericSignatureFormatError() throws GenericSignatureFormatError {
    
    prntS("      throwGenericSignatureFormatError");

    clean();
    throw new GenericSignatureFormatError();
}
private void clean() {
     int i;
     PTStack p1 = null;
     PTStack p2 = null;
    
    prntS("      clean");

     genParamList = null;
     boundList = null;
     methParamList = null;
     thrownList = null;
     for (i = 0; i <= lexer.stackDepth; i++) {
        if (i == 0) {
            p1 = stack;
        } else {
            p1 = p1.nextLevel;
        }
        p1.args = null;
     }
     for (i = 0; i <= lexer.stackDepth; i++) {
        if (i == 0) {
            p1 = stack;
        }
        p2 = p1.nextLevel;
        p1.nextLevel = null;
        p1 = p2;
     }
     stack = null;
     // XXX: How can we clear the memory allocated for currentStackElem.rawType and currentStackElem.owner?
     // Will it be done by VM's gc somehow later or should we invoke some JNI's method
     // or should we just invoke System.gc through JNI?
    CTPTsignature = null;
}
private void addElemToGenParamList(InterimTypeParameter ref) { //XXX: maybe, use genParamList.add(ref) everywhere instead of addElemToGenParamList(ref), remove addElemToGenParamList at all 
    prntS("      addElemToGenParamList");

    if(genParamList == null) {
        genParamList = new ArrayList<InterimTypeParameter>();
    }
    genParamList.add(ref);
}
private void addElemToBoundList(InterimType ref) {
    prntS("      addElemToBoundList");

     if(boundList == null) {
        boundList = new ArrayList<InterimType>();
    }
    boundList.add(ref);
}
private void addElemToMethParamList(Object ref) {
    prntSO("      addElemToMethParamList", ref);

     if(methParamList == null) {
        methParamList = new ArrayList<InterimType>();
    }
    methParamList.add((InterimType)ref);
}
private void addElemToTypeArgsList(InterimType ref) {
    prntS("      addElemToTypeArgsList: " + ref);

    // find the previous element for the current stack's element:
     PTStack p1 = stack, p2 = null;

     while (p1 != currentStackElem){
        p2 = p1;
        p1 = p1.nextLevel;
     }
     
    // add the value to the args list of the found stack's element:
     if(p2.args == null) {
        p2.args = new ArrayList<InterimType>();
    }
    p2.args.add(ref);
                                                                                        
    // clean the current stack's element to be ready for new reference parsering:
    currentStackElem.gatheredStr = null;
    currentStackElem.args = null;
    currentStackElem.wrappInWildcard = -1;
    currentStackElem.typeKind = 0;
    currentStackElem.rawType = null; // value should not be myfreed because a pointer to the later used java object was there
    currentStackElem.owner = null;   // value should not be myfreed because a pointer to the later used java object was there
    currentStackElem.sigBegin = -1;
    currentStackElem.sigEnd = -1;
    currentStackElem.dim = 0;
    currentStackElem.nextLevel = null;
}
private void addElemToThrownList(InterimType ref) {
    prntS("      addElemToThrownList");

     if(thrownList == null) {
        thrownList = new ArrayList<InterimType>();
    }
    thrownList.add(ref);
}
private void addElemToImplList(InterimType ref) {
    prntS("      addElemToImplList");

     if(implList == null) {
        implList = new ArrayList<InterimType>();
    }
    implList.add(ref);
}
private void addToGatheredStr(PTStack stackElem, String part) {
    if(stackElem.gatheredStr != null) {
        prntSSS("      addToGatheredStr", stackElem.gatheredStr, part);
    } else {
        prntSS("      addToGatheredStr", part);
    }

     if(stackElem.gatheredStr == null) {
        stackElem.gatheredStr = "";
     }
    stackElem.gatheredStr = stackElem.gatheredStr + part;
    prntSS(">->->->-> ", stackElem.gatheredStr);
}
private int addToSignature(String part) {
    int res = sigInd;
    
    prntS("      start addToSignature");
     if(CTPTsignature == null) {
        CTPTsignature = "";
     } 
     res = CTPTsignature.length();
    CTPTsignature = CTPTsignature + part;
    sigInd += part.length();
    
    prntS("      end addToSignature");
    return res;
}
private void createTypeParameterName(String name) {
    prntS("      createTypeParameterName");

    currentTypeParameter = new InterimTypeParameter();
    currentTypeParameter.typeParameterName = name;
}
private InterimClassType createInterimClassType(String reference) {
    InterimClassType res;
    
    prntS("      createInterimClassType");

    res = new InterimClassType();
    res.classTypeName = reference;
    return res;
}
private InterimTypeVariable createInterimTypeVariable(String tVariableName) {
    prntSS("      createInterimTypeVariable", tVariableName);

    InterimTypeVariable obj;
    
    obj = new InterimTypeVariable();
    obj.typeVariableName = tVariableName;
    return obj;
}
private InterimParameterizedType createInterimParameterizedType(String signature, PTStack stackElem) {
    InterimParameterizedType obj;

    prntS("      createInterimParameterizedType");

    obj = new InterimParameterizedType();
    obj.signature = signature;
    obj.rawType = stackElem.rawType;
    obj.ownerType = stackElem.owner;
    if (stackElem.args != null) {
        obj.parameters = stackElem.args.toArray(new InterimType[stackElem.args.size()]);
    } else {
        obj.parameters = new InterimType[0];
    }
    
    return obj;
}
private InterimWildcardType createInterimWildcardType(int boundsType, InterimType[] bounds, int boundsInd) {
    InterimWildcardType obj;
    
    prntS("      createInterimWildcardType");
    
    obj = new InterimWildcardType();
    obj.boundsType = boundsType == 0;
    obj.bounds = bounds;

    return obj;
}
private InterimGenericArrayType createInterimGenericArrayType(InterimType nextL) {
    InterimGenericArrayType obj;
    
    prntS("      createInterimGenericArrayType");
    
    obj = new InterimGenericArrayType();
    obj.nextLayer = /*(InterimGenericType)*/nextL;

    return obj;
}
private String getBaseTypeName(char c) {
    switch (c)
    {
    case 'I':
        return "int";  
    case 'F':
        return "float";  
    case 'D':
        return "double";  
    case 'J':
        return "long";  
    case 'S':
        return "short";  
    case 'Z':
        return "boolean";  
    case 'B':
        return "byte";  
    case 'C':
        return "char";  
    }
    throwGenericSignatureFormatError();                             
    return "UNKNOWN";  
}
private void prntS(String str) {
    if (lexer.DEBUGGING){
        System.out.println("|"+str+"|");
    }
}
void prntSS(String str1, String str2) {
    if (lexer.DEBUGGING){
        System.out.println("|"+str1+"|"+str2+"|");
    }
}
void prntSSS(String str1, String str2, String str3) {
    if (lexer.DEBUGGING){
        System.out.println("|"+str1+"|"+str2+"|"+str3+"|");
    }
}
void prntSD(String str1, long num) {
    if (lexer.DEBUGGING){
        System.out.println("|"+str1+"|"+num+"|");
    }
}
void prntSO(String str1, Object o) {
    if (lexer.DEBUGGING){
        System.out.println("|"+str1+"|"+o+"|");
    }
}
//////////////////////////////////////////////////////////////////////////////////////////////////////
public static InterimGenericDeclaration parseSignature(String st, int kind) {
    InterimGenericDeclaration res = null;
  SignatureLexer2 lexer = new SignatureLexer2(st); //?StringReader 
  SignatureParser parser = new SignatureParser(lexer);
  // Parse the input
  try {
   parser.pr__DECL(kind, lexer);
  }
  catch(RecognitionException e) {
    e.printStackTrace();
    System.err.println("signature syntax error: "+e);
    parser.throwGenericSignatureFormatError(); // signature syntax error!
  }
  catch(antlr.TokenStreamException e) {
    e.printStackTrace();
    System.err.println("TokenStreamException: "+e);
    parser.throwGenericSignatureFormatError();
  }
    parser.prntS("zzzzzzzzzzzzzzzzzz3");
    
    switch (kind)
    {
    case CLASSoDECL:
        res = (InterimGenericDeclaration)parser.classDecl;  
        break;
    case FIELDoDECL:
        res = (InterimGenericDeclaration)parser.fieldDecl;  
        break;
    case METHODoDECL:
        res = (InterimGenericDeclaration)parser.methodDecl;  
        break;
    case CONSTRUCTORoDECL:
        res = (InterimGenericDeclaration)parser.constructorDecl;  
        break;
    }
    parser.clean();
    parser.prntSO("zzzzzzzzzzzzzzzzzz4", res);
        
    return res;
}

protected SignatureParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public SignatureParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected SignatureParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public SignatureParser(TokenStream lexer) {
  this(lexer,1);
}

public SignatureParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void pr__DECL(
		int kind, Object lex
	) throws RecognitionException, TokenStreamException {
		
		
		lexer = (SignatureLexer2)lex;
		//lexer.DEBUGGING = true;
		declType = kind;
		stack = new PTStack();
		stack.gatheredStr = null;
		stack.gthrdStrLen = 0;
		stack.typeKind = 0;
		stack.wrappInWildcard = -1;
		stack.rawType = null;
		stack.owner = null;
		stack.args = null;
		stack.sigBegin = -1;
		stack.sigEnd = -1;
		stack.dim = 0;
		stack.nextLevel = null;
		
		// to be reenterable:
		CTPTsignature = null;
		sigInd = 0;
		currentStackElem = stack;
		genParamList = null;
		boundList = null;
		methParamList = null;
		thrownList = null;
		implList = null;
		Yflag2 = 0;
		currentTypeParameter = null;
		
		// Clean lex's environment to provide reenterability:
		lexer.prevLexeme = -1;
		lexer.Lflag2 = 0; 
		lexer.Lflag3 = 0; 
		lexer.stackDepth = 0; 
		lexer.ident = null;
		
		
		try {      // for error handling
			if (((LA(1)==TRIANGLEOPEN_SIGN||LA(1)==ID||LA(1)==PACKAGE_SPECIFIER))&&(declType==CLASSoDECL)) {
				pr__CLASS_DECL();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__DECL 1 :", "m1.getText()");
				}
			}
			else if (((_tokenSet_0.member(LA(1))))&&(declType==FIELDoDECL)) {
				pr__FIELD_DECL();
				if ( inputState.guessing==0 ) {
					
					// it's time to create InterimFieldGenericDecl and to fill fieldDecl
					fieldDecl = new InterimFieldGenericDecl();
					
					// set fieldType field of InterimFieldGenericDecl object:
					fieldDecl.fieldType = (InterimGenericType)highLevelType;
					
					highLevelType = null;
					
					prntSS("   pr__DECL 2 :", "m2.getText()");
					
				}
			}
			else if (((LA(1)==TRIANGLEOPEN_SIGN||LA(1)==RINGOPEN_SIGN))&&(declType==METHODoDECL)) {
				pr__METHOD_DECL();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__DECL 3:", "m3.getText()");
				}
			}
			else if (((LA(1)==TRIANGLEOPEN_SIGN||LA(1)==RINGOPEN_SIGN))&&(declType==CONSTRUCTORoDECL)) {
				pr__CONSTRUCTOR_DECL();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__DECL 4 :", "m4.getText()");
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__CLASS_DECL() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case TRIANGLEOPEN_SIGN:
			{
				pr__FORMAL_TYPE_PARAMETERS_DECL();
				if ( inputState.guessing==0 ) {
					prntSS("   ### 1:", "m6.getText()");
				}
				pr__CLASS_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===1===");  
					// actually, it's pr__SUPERCLASSoSIGNATURE
					
					// it's time to create InterimClassGenericDecl and to fill classDecl
					classDecl = new InterimClassGenericDecl();
					
					// set superClass field of InterimClassGenericDecl object:
					classDecl.superClass = (InterimType)highLevelType;
					
					highLevelType = null;
					
					// set typeParameters field of InterimClassGenericDecl object:
					classDecl.typeParameters = genParamList.toArray(new InterimTypeParameter[genParamList.size()]);
					
					// clean the genParamList:
					genParamList = null;
					
					prntSS("   ### 2:", "m7.getText()");
					
				}
				pr__SUPERINTERFACE_SIGNATURES();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===2===");  
					// set superInterfaces field of InterimClassGenericDecl object:
					classDecl.superInterfaces = (implList == null ? new InterimType[0] : implList.toArray(new InterimType[implList.size()]));
					
					// clean the implList:
					implList = null;
					
					prntSS("   pr__CLASS_DECL 1 :", "m8.getText()");
					
				}
				break;
			}
			case ID:
			case PACKAGE_SPECIFIER:
			{
				pr__CLASS_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					
					if (declType == CLASSoDECL) {
					prntS("      ===3.1===");  
					// actually, it's pr__SUPERCLASSoSIGNATURE
					
					// it's time to create InterimClassGenericDecl and to fill classDecl
					classDecl = new InterimClassGenericDecl();
					
					// set superClass field of InterimClassGenericDecl object:
					classDecl.superClass = (InterimType)highLevelType;
					
					highLevelType = null;
					} else { // it's FIELDoDECL
					prntS("      ===3.2===");  
					// actually, it's field type signature (instead the pr__FIELD_DECL which does not work really)
					
					// it's time to create InterimFieldGenericDecl and to fill fieldDecl
					fieldDecl = new InterimFieldGenericDecl();
					
					// set superClass field of InterimFieldGenericDecl object:
					fieldDecl.fieldType = (InterimGenericType)highLevelType;
					
					highLevelType = null;
					}
					
					prntSS("   ### 3:", "m9.getText()");
					
				}
				pr__SUPERINTERFACE_SIGNATURES();
				if ( inputState.guessing==0 ) {
					
					if (declType == CLASSoDECL) {
					// set superInterfaces field of InterimClassGenericDecl object:
					classDecl.superInterfaces = (implList == null ? new InterimType[0] : implList.toArray(new InterimType[implList.size()]));
					
					// clean the implList:
					implList = null;
					
					prntSS("   pr__CLASS_DECL 2 :", "m10.getText()");
					} else {
					prntSS("   pr__CLASS_DECL 2 (+++ for FIELDoDECL +++) :", "m10.getText()");
					}
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__FIELD_DECL() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__FIELD_TYPE_SIGNATURE();
			if ( inputState.guessing==0 ) {
				prntSS("   pr__FIELD_DECL:", "m5.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__METHOD_DECL() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			boolean synPredMatched48 = false;
			if (((LA(1)==RINGOPEN_SIGN))) {
				int _m48 = mark();
				synPredMatched48 = true;
				inputState.guessing++;
				try {
					{
					pr__M_P_AND_R_T();
					pr__THROWN_SIGNATURE();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched48 = false;
				}
				rewind(_m48);
				inputState.guessing--;
			}
			if ( synPredMatched48 ) {
				pr__M_P_AND_R_T();
				if ( inputState.guessing==0 ) {
					prntSS("   ### 25:", "m59.getText()");
				}
				pr__THROWN_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__METHOD_DECL 2 :prntSS", "m60.getText()");
				}
			}
			else if ((LA(1)==RINGOPEN_SIGN)) {
				pr__M_P_AND_R_T();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__METHOD_DECL 1 :", "m58.getText()");
				}
			}
			else {
				boolean synPredMatched50 = false;
				if (((LA(1)==TRIANGLEOPEN_SIGN))) {
					int _m50 = mark();
					synPredMatched50 = true;
					inputState.guessing++;
					try {
						{
						pr__F_T_P_AND_M_P_AND_R_T();
						pr__THROWN_SIGNATURE();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched50 = false;
					}
					rewind(_m50);
					inputState.guessing--;
				}
				if ( synPredMatched50 ) {
					pr__F_T_P_AND_M_P_AND_R_T();
					if ( inputState.guessing==0 ) {
						prntSS("   ### 26:", "m62.getText()");
					}
					pr__THROWN_SIGNATURE();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__METHOD_DECL 4 :", "m63.getText()");
					}
				}
				else if ((LA(1)==TRIANGLEOPEN_SIGN)) {
					pr__F_T_P_AND_M_P_AND_R_T();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__METHOD_DECL 3 :", "m61.getText()");
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_1);
				} else {
				  throw ex;
				}
			}
		}
		
	public final void pr__CONSTRUCTOR_DECL() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			boolean synPredMatched41 = false;
			if (((LA(1)==RINGOPEN_SIGN))) {
				int _m41 = mark();
				synPredMatched41 = true;
				inputState.guessing++;
				try {
					{
					pr__C_P_AND_R_T();
					pr__THROWN_SIGNATURE();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched41 = false;
				}
				rewind(_m41);
				inputState.guessing--;
			}
			if ( synPredMatched41 ) {
				pr__C_P_AND_R_T();
				if ( inputState.guessing==0 ) {
					prntSS("   ### 23:", "m53.getText()");
				}
				pr__THROWN_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__CONSTRUCTOR_DECL 2 :prntSS", "m54.getText()");
				}
			}
			else if ((LA(1)==RINGOPEN_SIGN)) {
				pr__C_P_AND_R_T();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__CONSTRUCTOR_DECL 1 :", "m52.getText()");
				}
			}
			else {
				boolean synPredMatched43 = false;
				if (((LA(1)==TRIANGLEOPEN_SIGN))) {
					int _m43 = mark();
					synPredMatched43 = true;
					inputState.guessing++;
					try {
						{
						pr__F_T_P_AND_C_P_AND_R_T();
						pr__THROWN_SIGNATURE();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched43 = false;
					}
					rewind(_m43);
					inputState.guessing--;
				}
				if ( synPredMatched43 ) {
					pr__F_T_P_AND_C_P_AND_R_T();
					if ( inputState.guessing==0 ) {
						prntSS("   ### 24:", "m56.getText()");
					}
					pr__THROWN_SIGNATURE();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__CONSTRUCTOR_DECL 4 :", "m57.getText()");
					}
				}
				else if ((LA(1)==TRIANGLEOPEN_SIGN)) {
					pr__F_T_P_AND_C_P_AND_R_T();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__CONSTRUCTOR_DECL 3 :", "m55getText()");
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_1);
				} else {
				  throw ex;
				}
			}
		}
		
	public final void pr__FIELD_TYPE_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		Token  m23 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			case PACKAGE_SPECIFIER:
			{
				pr__CLASS_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__FIELD_TYPE_SIGNATURE 1 :", "m21.getText()");
				}
				break;
			}
			case SQUAREOPEN_SIGN:
			{
				pr__ARRAY_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===9===");  
					if (Yflag1 == 1) {
					throwGenericSignatureFormatError(); // array type is not permissible within a generic params decl
					}
					if (currentStackElem.wrappInWildcard != -1) {
					throwGenericSignatureFormatError(); // array type is not permissible within a wild card
					}
					prntSS("   pr__FIELD_TYPE_SIGNATURE 2 :", "m22.getText()");
					
				}
				break;
			}
			case TVAR:
			{
				m23 = LT(1);
				match(TVAR);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===10===");  
					// grow the signature if it needs:
					if(CTPTsignature != null){
					currentStackElem.sigEnd = addToSignature(m23.getText()) + m23.getText().length();
					}
					
					// to exclude first (official) "T" symbol (and last ";" symbol):
					prsrT = (InterimType)createInterimTypeVariable(m23.getText().substring(1, m23.getText().length()-1));
					
					// if there is wildcard indicator then InterimTypeVariable should be "rolled up" by InterimWildcardType
					if (currentStackElem.wrappInWildcard != -1) {
					prsrT = (InterimType)createInterimWildcardType(currentStackElem.wrappInWildcard, new InterimType[]{(InterimType)prsrT}, 1);
					}
					
					// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
					if (currentStackElem.dim > 0) {
					for (i = 0; i < currentStackElem.dim; i++ ) {
					prsrT = createInterimGenericArrayType(prsrT);
					}
					}
					currentStackElem.dim = 0;
					
					if (Yflag1 == 1) { // within generic params decl (class'/method's/constructor's)
					if(lexer.stackDepth == 0){ // not within parameterized type
					if(boundList == null){
					addElemToBoundList(prsrT); // first (i.e. "extends") bound, consequently, TVAR is permissible here
					} else {
					throwGenericSignatureFormatError(); // non-first (i.e. "implements") bound, consequently, TVAR is not permissible as such bound
					}
					} else { // within parameterized type which appears within gen params decl
					// put the InterimTypeVariable on the layer above of the stack of parsered nested parameterized types:
					addElemToTypeArgsList(prsrT);
					}
					} else {
					// so, for other places of using ...
					if(lexer.stackDepth == 0){ // not within parameterized type
					highLevelType = /*(InterimGenericType)*/(InterimType)prsrT;
					} else { // within parameterized type which appears not within gen params decl
					// put the InterimTypeVariable on the layer above of the stack of parsered nested parameterized types:
					addElemToTypeArgsList(prsrT);
					}
					}
					prntSS("   pr__FIELD_TYPE_SIGNATURE 3 :", m23.getText());
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__FORMAL_TYPE_PARAMETERS_DECL() throws RecognitionException, TokenStreamException {
		
		Token  m11 = null;
		Token  m13 = null;
		
		try {      // for error handling
			m11 = LT(1);
			match(TRIANGLEOPEN_SIGN);
			if ( inputState.guessing==0 ) {
				
				prntS("      ===4===");  
				Yflag1 = 1; // start of generic parameters parsing
				prntSS("   ### 4:", m11.getText());
				
			}
			pr__FORMAL_TYPE_PARAMETERS();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 5:", "m12.getText()");
			}
			m13 = LT(1);
			match(TRIANGLECLOSE_SIGN);
			if ( inputState.guessing==0 ) {
				
				prntS("      ===5===");  
				Yflag1 = 0; // finish of generic parameters parsing
				prntSS("   pr__FORMAL_TYPE_PARAMETERS_DECL:", m13.getText());
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_3);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__CLASS_TYPE_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		Token  m25 = null;
		
		try {      // for error handling
			pr__REFERENCE();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 10:", "m24.getText()");
			}
			m25 = LT(1);
			match(SEMICOLON_SIGN);
			if ( inputState.guessing==0 ) {
				
				prntS("      ===11===");  
				// XXX: seems, the entire code fragment below can be easily simplified
				
				// roll up the reference (InterimClassType or InterimParameterizedType) to put on the layer above or to return as a final result:
				if (lexer.stackDepth == 0) {
				prntS("      ===111===");  
				if(currentStackElem.typeKind == 0) { // InterimClassType
				// return the InterimClassType as a result of a reference rolling up:
				prsrT = (InterimType)createInterimClassType(currentStackElem.gatheredStr);
				
				// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
				if (currentStackElem.dim > 0) {
				for (i = 0; i < currentStackElem.dim; i++ ) {
				prsrT = (InterimType)createInterimGenericArrayType(prsrT);
				}
				}
				currentStackElem.dim = 0;
				
				highLevelType = /*(InterimGenericType)*/(InterimType)prsrT;
				} else { //InterimParameterizedType
				if(Yflag2 == 1){ // within the throws
				throwGenericSignatureFormatError(); // here InterimParameterizedType is prohibited.
				}
				
				// return the InterimParameterizedType as a result of a reference rolling up:
				addToSignature(";"); 
				currentStackElem.sigEnd += 1;
				len = sigInd - currentStackElem.sigBegin - 1; //to eliminate everywhere the last semicolon sign
				prsrT = (InterimType)createInterimParameterizedType(CTPTsignature.substring(currentStackElem.sigBegin, currentStackElem.sigBegin + len), currentStackElem);
				currentStackElem.args = null;
				
				// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
				if (currentStackElem.dim > 0) {
				for (i = 0; i < currentStackElem.dim; i++ ) {
				prsrT = (InterimType)createInterimGenericArrayType(prsrT);
				}
				}
				currentStackElem.dim = 0;
				
				highLevelType = /*(InterimGenericType)*/(InterimType)prsrT;
				}
				
				// it's time to clean entire CTPTsignature for finally rolled up high level reference:
				if (CTPTsignature != null) {
				CTPTsignature = null;
				sigInd = 0;
				}
				
				// clean the current stack's element to be ready for new reference parsering:
				currentStackElem.gatheredStr = null;
				currentStackElem.args = null;
				currentStackElem.wrappInWildcard = -1;
				currentStackElem.typeKind = 0;
				currentStackElem.rawType = null; //value should not be mefreed because a pointer to the later used java object was there
				currentStackElem.owner = null; //value should not be mefreed because a pointer to the later used java object was there
				currentStackElem.sigBegin = -1;
				currentStackElem.sigEnd = -1;
				currentStackElem.dim = 0;
				currentStackElem.nextLevel = null;
				} else {
				prntS("      ===112===");  
				if(currentStackElem.typeKind == 0) { // InterimClassType
				addToSignature(";"); 
				currentStackElem.sigEnd += 1;
				prsrT = createInterimClassType(currentStackElem.gatheredStr);
				
				// if there is wildcard indicator then InterimClassType should be "rolled up" by InterimWildcardType
				if (currentStackElem.wrappInWildcard != -1) {
				prsrT = (InterimType)createInterimWildcardType(currentStackElem.wrappInWildcard, new InterimType[]{(InterimType)prsrT}, 1);
				}
				
				// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
				if (currentStackElem.dim > 0) {
				for (i = 0; i < currentStackElem.dim; i++ ) {
				prsrT = (InterimType)createInterimGenericArrayType(prsrT);
				}
				}
				currentStackElem.dim = 0;
				
				// put the InterimClassType/InterimWildcardType on the layer above of the stack of parsered nested parameterized types:
				addElemToTypeArgsList(prsrT);
				} else { // InterimParameterizedType
				addToSignature(";"); 
				currentStackElem.sigEnd += 1;
				len = sigInd - currentStackElem.sigBegin -1; //to eliminate everywhere the last semicolon sign
				prsrT = (InterimType)createInterimParameterizedType(CTPTsignature.substring(currentStackElem.sigBegin, currentStackElem.sigBegin + len), currentStackElem);
				currentStackElem.args = null;
				
				// if there is wildcard indicator then InterimParameterizedType should be "rolled up" by InterimWildcardType
				if (currentStackElem.wrappInWildcard != -1) {
				prsrT = (InterimType)createInterimWildcardType(currentStackElem.wrappInWildcard, new InterimType[]{(InterimType)prsrT}, 1);
				}
				
				// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
				if (currentStackElem.dim > 0) {
				for (i = 0; i < currentStackElem.dim; i++ ) {
				prsrT = (InterimType)createInterimGenericArrayType(prsrT);
				}
				}
				currentStackElem.dim = 0;
				// put the InterimParameterizedType/InterimWildcardType on the layer above of the stack of parsered nested parameterized types:
				addElemToTypeArgsList(prsrT);
				}
				}
				
				// It's time to clear currentStackElem.gatheredStr:
				currentStackElem.gatheredStr = null;
				// It's time also to clear currentStackElem.rawType:
				currentStackElem.rawType = null;
				
				prntSS("   pr__CLASS_TYPE_SIGNATURE:", m25.getText());
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__SUPERINTERFACE_SIGNATURES() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			_loop37:
			do {
				if ((LA(1)==ID||LA(1)==PACKAGE_SPECIFIER)) {
					pr__SUPERINTERFACE_SIGNATURE();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__SUPERINTERFACE_SIGNATURES 1 :", "m50.getText()");
					}
				}
				else {
					break _loop37;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__FORMAL_TYPE_PARAMETERS() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			int _cnt7=0;
			_loop7:
			do {
				if ((LA(1)==ID_COLON||LA(1)==ID)) {
					pr__FORMAL_TYPE_PARAMETER();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__FORMAL_TYPE_PARAMETERS 1 :", "m14.getText()");
					}
				}
				else {
					if ( _cnt7>=1 ) { break _loop7; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt7++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_4);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__FORMAL_TYPE_PARAMETER() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__ID_WITH_COLON();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 7:", /*lexer.ident*/"m141.getText()");
			}
			pr__CLASS_OR_INTERFACE_BOUNDS();
			if ( inputState.guessing==0 ) {
				
				prntS("      ===6===");  
				// set classBound field of InterimTypeParameter object:
				currentTypeParameter.classBound = (InterimType)boundList.get(0); // the first elem is extends-clause
				boundList.remove(0);
				
				// set interfaceBounds field of InterimTypeParameter object:
				currentTypeParameter.interfaceBounds = boundList.toArray(new InterimType[boundList.size()]);
				
				// clean the boundList before a possible re-using:
				boundList = null;
				
				// add the prepared InterimTypeParameter object to genParamList:
				addElemToGenParamList(currentTypeParameter);
				
				prntSS("   pr__FORMAL_TYPE_PARAMETER:", "m15.getText()");
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_5);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__ID_WITH_COLON() throws RecognitionException, TokenStreamException {
		
		Token  m151 = null;
		Token  m16 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID_COLON:
			{
				m151 = LT(1);
				match(ID_COLON);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===7===");  
					String ts1 = m151.getText();
					String ts2 = ts1.substring(0, ts1.length()-2);
					
					createTypeParameterName(ts2);
					addElemToBoundList(createInterimClassType("Ljava/lang/Object")); // Object is supposed if extends clause is empty (example: PARAMNAME::...;)
					
					prntSS("   pr__ID_WITH_COLON 1 :", ts2);
					
				}
				break;
			}
			case ID:
			{
				m16 = LT(1);
				match(ID);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===8===");  
					createTypeParameterName(m16.getText());
					
					prntSS("   pr__ID_WITH_COLON 2 :", m16.getText());
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_6);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__CLASS_OR_INTERFACE_BOUNDS() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			int _cnt12=0;
			_loop12:
			do {
				if ((LA(1)==COLON_SIGN)) {
					pr__BOUND();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__CLASS_OR_INTERFACE_BOUNDS 1 :", "m18.getText()");
					}
				}
				else {
					if ( _cnt12>=1 ) { break _loop12; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt12++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_5);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__BOUND() throws RecognitionException, TokenStreamException {
		
		Token  m19 = null;
		
		try {      // for error handling
			m19 = LT(1);
			match(COLON_SIGN);
			if ( inputState.guessing==0 ) {
				prntSS("   ### 9:", m19.getText());
			}
			pr__FIELD_TYPE_SIGNATURE();
			if ( inputState.guessing==0 ) {
				
				prntS("      ===8.1===");  
				addElemToBoundList((InterimType)highLevelType); // add the gathered regular type to bounds-list
				
				highLevelType = null;
				
				prntSS("   pr__BOUND:", "m20.getText()");
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_7);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__ARRAY_TYPE_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		Token  m46 = null;
		
		try {      // for error handling
			m46 = LT(1);
			match(SQUAREOPEN_SIGN);
			if ( inputState.guessing==0 ) {
				
				currentStackElem.dim += 1;
				
				prntSS("   ### 21:", m46.getText());
				
			}
			pr__TYPE_SIGNATURE();
			if ( inputState.guessing==0 ) {
				prntSS("   pr__ARRAY_TYPE_SIGNATURE:", "m47.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__REFERENCE() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			boolean synPredMatched18 = false;
			if (((LA(1)==ID))) {
				int _m18 = mark();
				synPredMatched18 = true;
				inputState.guessing++;
				try {
					{
					pr__SIMPLE_CLASS_TYPE_SIGNATURE();
					pr__CLASS_TYPE_SIGNATURE_SUFFIXES();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched18 = false;
				}
				rewind(_m18);
				inputState.guessing--;
			}
			if ( synPredMatched18 ) {
				pr__SIMPLE_CLASS_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   ### 10:", "m27.getText()");
				}
				pr__CLASS_TYPE_SIGNATURE_SUFFIXES();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__REFERENCE 2 :", "m28.getText()");
				}
			}
			else if ((LA(1)==ID)) {
				pr__SIMPLE_CLASS_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__REFERENCE 1 :", "m26.getText()");
				}
			}
			else {
				boolean synPredMatched20 = false;
				if (((LA(1)==PACKAGE_SPECIFIER))) {
					int _m20 = mark();
					synPredMatched20 = true;
					inputState.guessing++;
					try {
						{
						pr__P_S_AND_S_C_T();
						pr__CLASS_TYPE_SIGNATURE_SUFFIXES();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched20 = false;
					}
					rewind(_m20);
					inputState.guessing--;
				}
				if ( synPredMatched20 ) {
					pr__P_S_AND_S_C_T();
					if ( inputState.guessing==0 ) {
						prntSS("   ### 11:", "m29.getText()");
					}
					pr__CLASS_TYPE_SIGNATURE_SUFFIXES();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__REFERENCE 3 :", "m30.getText()");
					}
				}
				else if ((LA(1)==PACKAGE_SPECIFIER)) {
					pr__P_S_AND_S_C_T();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__REFERENCE 4 :", "m31.getText()");
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_8);
				} else {
				  throw ex;
				}
			}
		}
		
	public final void pr__SIMPLE_CLASS_TYPE_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		Token  m34 = null;
		Token  m35 = null;
		Token  m37 = null;
		Token  m371 = null;
		
		try {      // for error handling
			boolean synPredMatched24 = false;
			if (((LA(1)==ID))) {
				int _m24 = mark();
				synPredMatched24 = true;
				inputState.guessing++;
				try {
					{
					match(ID);
					match(TRIANGLEOPEN_SIGN);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched24 = false;
				}
				rewind(_m24);
				inputState.guessing--;
			}
			if ( synPredMatched24 ) {
				m34 = LT(1);
				match(ID);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===13===");  
					String ts1 = m34.getText();
					int tl = ts1.length();
					if (currentStackElem.rawType == null){ // so it's the non-inner class id
					prntS("      ===131===");  
					// create the owner:
					currentStackElem.owner = null; // owner is absent for package level class
					
					// grow the gatheredStr:
					if(currentStackElem.gatheredStr == null){ // so, we should check the existence of "L" at the begin of ID because it is the begin of the reference
					//  (for remembering: any "T..." identifier can not be considered as TVAR within
					//   this being parsered pr__SIMPLE_CLASS_TYPE_SIGNATURE rule, especially, if we are
					//   within generic parameters declaration parsering, where a TVAR using is prohibited)
					if(ts1.charAt(0) != 'L'){
					throwGenericSignatureFormatError();
					}
					addToGatheredStr(currentStackElem, ts1); // so, it's "L"<class ID> here
					} else { //so, it is the reference like <package name>/<non-inner class name> (because rawType == null && gatheredStr == null)
					addToGatheredStr(currentStackElem, ts1); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID> here
					}
					
					// grow the signature (like the gatheredStr growing):
					if(currentStackElem.sigBegin == -1){
					currentStackElem.sigBegin = addToSignature(ts1); // so, it's "L"<class ID> added to CTPTsignature 
					currentStackElem.sigEnd = currentStackElem.sigBegin + tl; 
					} else {
					currentStackElem.sigEnd = addToSignature(ts1) + tl; // so, it's <class ID> added to CTPTsignature, 
					// consequently the "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID> is there regarding the current parsered reference
					}
					
					// create the raw type:
					currentStackElem.rawType = createInterimClassType(currentStackElem.gatheredStr);
					
					// we know "<...>" follows further, so the type becames InterimParameterizedType now:
					currentStackElem.typeKind = 1;
					} else { // so it's the regular inner class id
					prntS("      ===132===");  
					// create the owner:
					if(currentStackElem.typeKind == 0 && currentStackElem.args == null){
					prntS("      ===1321===");  
					//if (currentStackElem.owner != null) { 
					// // so, we have created InterimClassType some times for the previous parts (IDs)
					// // and we should provide that each InterimClassType object should be deleted by GC when the next one is being created
					// // because the only last preceding may to be used as owner of the first InterimParameterizedType object which arises
					// // while a reference is parsered.
					// // So, can/should we do anything special (I don't see such features within JNI) to destroy the previouse InterimClassType 
					// // object which becames superflouos
					// // just when the next one appeares? Or GC will remove such objects because there will not be any references for them
					// // within the java code.
					// ???<<< (*env)->ReleaseObject(..., currentStackElem.owner, ...); >>>???
					//}
					currentStackElem.owner = createInterimClassType(/*currentStackElem.rawType*/currentStackElem.gatheredStr);
					} else {
					//printf("      %s %d %d %d\n", "===1322===", currentStackElem.typeKind, currentStackElem.args);  
					currentStackElem.typeKind = 1; // at least one args was not equal to  null at a previous stage, so we deal with the parameterized type from that time
					len = sigInd - currentStackElem.sigBegin/* + 1*/;
					currentStackElem.owner = createInterimParameterizedType(CTPTsignature.substring(currentStackElem.sigBegin, currentStackElem.sigBegin+len), currentStackElem);
					currentStackElem.args = null;
					}
					
					// grow the gatheredStr:
					addToGatheredStr(currentStackElem, "$"); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$" here
					addToGatheredStr(currentStackElem, ts1); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$"<class ID> here
					
					// grow the signature (like the gatheredStr growing):
					currentStackElem.sigEnd = addToSignature("$") + 1; // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$" gathered in CTPTsignature 
					currentStackElem.sigEnd = addToSignature(ts1) + tl; //so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$"<class ID> gathered in CTPTsignature 
					
					// create the raw type:
					currentStackElem.rawType = createInterimClassType(currentStackElem.gatheredStr);
					
					// we know "<...>" follows further, so the type becames InterimParameterizedType now:
					currentStackElem.typeKind = 1;
					}
					prntSS("   ### 13:", ts1);
					
				}
				m35 = LT(1);
				match(TRIANGLEOPEN_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===14===");  
					// grow the signature:
					currentStackElem.sigEnd = addToSignature("<") + 1;
					
					// It's the time to clean the arguments list of including class:
					if (currentStackElem.args != null) {
					currentStackElem.args = null;
					}
					
					currentStackElemCopy = currentStackElem;
					currentStackElem = new PTStack();
					currentStackElemCopy.nextLevel = currentStackElem;
					lexer.stackDepth++; //to reflect a level of argument nesting
					currentStackElem.gatheredStr = null;
					currentStackElem.gthrdStrLen = 0;
					currentStackElem.wrappInWildcard = -1;
					currentStackElem.typeKind = 0;
					currentStackElem.rawType = null;
					currentStackElem.owner = null;
					currentStackElem.args = null;
					currentStackElem.sigBegin = -1;
					currentStackElem.sigEnd = -1;
					currentStackElem.dim = 0;
					currentStackElem.nextLevel = null;
					
					prntSS("   ### 14:", m35.getText());
					
				}
				pr__TYPE_ARGUMENTS();
				if ( inputState.guessing==0 ) {
					prntSS("   ### 15:", "m36.getText()");
				}
				m37 = LT(1);
				match(TRIANGLECLOSE_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===15===");  
					// grow the signature:
					currentStackElem.sigEnd = addToSignature(">") + 1;
					
					// find the previous element for the current stack's element:
					p1 = stack;
					while (p1 != currentStackElem){
					p2 = p1;
					p1 = p1.nextLevel;
					}
					p2.nextLevel = null;
					lexer.stackDepth--; // to reflect a level of argument nesting
					
					// return to previous stack element:
					currentStackElem = p2;
					p2 = null;
					
					// in any case, the being finished reference is of InterimParametrizedType because it has "<...>"
					currentStackElem.typeKind = 1;
					
					// free memory of the being left stack's element:
					p1.gatheredStr = null;
					p1 = null;
					
					prntSS("   pr__SIMPLE_CLASS_TYPE_SIGNATURE 1 :", m37.getText());
					
				}
			}
			else if ((LA(1)==ID)) {
				m371 = LT(1);
				match(ID);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===16===");  
					String ts1 = m371.getText();
					int tl = ts1.length();
					if (currentStackElem.rawType == null){ // so it's the non-inner class id
					prntSS("      ===160===", ts1);  
					// create the owner:
					currentStackElem.owner = null; // owner is absent for package level class
					
					// grow the gatheredStr:
					if(currentStackElem.gatheredStr == null){ // so, we should check the existence of "L" at the begin of ID because it is the begin of the reference
					// (for remembering: any "T..." identifier can not be considered as TVAR within
					//  this being parsered pr__SIMPLE_CLASS_TYPE_SIGNATURE rule, especially, if we are
					//  within generic parameters declaration parsering, where a TVAR using is prohibited)
					if(ts1.charAt(0) != 'L'){
					throwGenericSignatureFormatError();
					}
					addToGatheredStr(currentStackElem, ts1); // so, it's "L"<class ID> here
					} else {
					addToGatheredStr(currentStackElem, ts1); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID> here
					}
					
					// grow the signature (like the gatheredStr growing):
					if(currentStackElem.sigBegin == -1){
					currentStackElem.sigBegin = addToSignature(ts1); // so, it's "L"<class ID> added to CTPTsignature 
					currentStackElem.sigEnd = currentStackElem.sigBegin + tl; 
					} else {
					currentStackElem.sigEnd = addToSignature(ts1) + tl; // so, it's <class ID> added to CTPTsignature, 
					// consequently the "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID> is there regarding the current parsered reference
					}
					
					// create the raw type:
					currentStackElem.rawType = createInterimClassType(currentStackElem.gatheredStr);
					} else { //so it's the regular inner class id
					prntSS("      ===161===", ts1);  
					// create the owner:
					if(currentStackElem.typeKind == 0 && currentStackElem.args == null){
					//if (currentStackElem.owner != null) { 
					// // so, we have created InterimClassType some times for the previous parts (IDs)
					// // and we should provide that each InterimClassType object should be deleted by GC when the next one is being created
					// // because the only last preceding may to be used as owner of the first InterimParameterizedType object which arises
					// // while a reference is parsered.
					// // So, can/should we do anything special (I don't see such features within JNI) to destroy the previouse InterimClassType 
					// // object which becames superflouos
					// // just when the next one appeares? Or GC will remove such objects because there will not be any references for them
					// // within the java code.
					// ???<<< (*env)->ReleaseObject(..., currentStackElem.owner, ...); >>>???
					//}
					currentStackElem.owner = createInterimClassType(/*currentStackElem.rawType*/currentStackElem.gatheredStr);
					} else {
					currentStackElem.typeKind = 1; // at least one args was not equal to  null at a previous stage
					//len = currentStackElem.sigEnd - currentStackElem.sigBegin + 1;
					len = sigInd - currentStackElem.sigBegin/* + 1*/;
					currentStackElem.owner = createInterimParameterizedType(CTPTsignature.substring(currentStackElem.sigBegin, currentStackElem.sigBegin + len), currentStackElem);
					currentStackElem.args = null;
					}
					
					// grow the gatheredStr:
					addToGatheredStr(currentStackElem, "$"); //so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$" here
					addToGatheredStr(currentStackElem, ts1); //so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$"<class ID> here
					
					// grow the signature (like the gatheredStr growing):
					currentStackElem.sigEnd = addToSignature("$") + 1; //so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$" gathered in CTPTsignature 
					currentStackElem.sigEnd = addToSignature(ts1) + tl; //so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/"<class ID>{"$"<class ID>}"$"<class ID> gathered in CTPTsignature 
					
					// create the raw type:
					currentStackElem.rawType = createInterimClassType(currentStackElem.gatheredStr);
					}
					prntSS("   pr__SIMPLE_CLASS_TYPE_SIGNATURE 2:", ts1);
					
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_9);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__CLASS_TYPE_SIGNATURE_SUFFIXES() throws RecognitionException, TokenStreamException {
		
		Token  m44 = null;
		
		try {      // for error handling
			{
			int _cnt32=0;
			_loop32:
			do {
				if ((LA(1)==DOT_OR_DOLLAR_SIGN)) {
					m44 = LT(1);
					match(DOT_OR_DOLLAR_SIGN);
					if ( inputState.guessing==0 ) {
						
						prntS("      ===21===");  
						//// seems, I have just done it in pr__SIMPLE_CLASS_TYPE_SIGNATURE
						//// grow the signature:
						//currentStackElem.sigEnd = addToSignature("$") + 1;
						
						prntSS("   ### 20:", m44.getText());
						
					}
					pr__SIMPLE_CLASS_TYPE_SIGNATURE();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__CLASS_TYPE_SIGNATURE_SUFFIXES 2 :", "m45.getText()");
					}
				}
				else {
					if ( _cnt32>=1 ) { break _loop32; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt32++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__P_S_AND_S_C_T() throws RecognitionException, TokenStreamException {
		
		Token  m32 = null;
		
		try {      // for error handling
			m32 = LT(1);
			match(PACKAGE_SPECIFIER);
			if ( inputState.guessing==0 ) {
				
				prntS("      ===12===");  
				
				// to start of gathering all info within gatheredStr about the being parsered reference:
				addToGatheredStr(currentStackElem, m32.getText()); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/" here
				
				// to start of gathering all info within CTPTsignature about the being parsered reference:
				currentStackElem.sigBegin = addToSignature(m32.getText()); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/" added to CTPTsignature 
				currentStackElem.sigEnd = currentStackElem.sigBegin + m32.getText().length();
				
				prntSS(">->->->-> ", currentStackElem.gatheredStr);
				prntSS("   ### 12:", m32.getText());
				
			}
			pr__SIMPLE_CLASS_TYPE_SIGNATURE();
			if ( inputState.guessing==0 ) {
				prntSS("   pr__P_S_AND_S_C_T:", "m33.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_9);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__TYPE_ARGUMENTS() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			int _cnt27=0;
			_loop27:
			do {
				if ((_tokenSet_10.member(LA(1)))) {
					pr__TYPE_ARGUMENT();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__TYPE_ARGUMENTS 1 :", "m38.getText()");
					}
				}
				else {
					if ( _cnt27>=1 ) { break _loop27; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt27++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_4);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__TYPE_ARGUMENT() throws RecognitionException, TokenStreamException {
		
		Token  m39 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case STAR_SIGN:
			{
				m39 = LT(1);
				match(STAR_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===17===");  
					// grow the signature inconditionally because a wildcard is always the argument of :
					currentStackElem.sigEnd = addToSignature("*") + 1;
					
					// so, it's "unrestricted" wildcard.
					// add the wildcard to the args list of the stack's element of the previous layer:
					upper = createInterimClassType("Ljava/lang/Object");
					addElemToTypeArgsList(createInterimWildcardType(0 /* i.e. extends Object */, new InterimType[]{(InterimType)upper}, 1));
					
					prntSS("   pr__TYPE_ARGUMENT 1 :", m39.getText());
					
				}
				break;
			}
			case ID:
			case TVAR:
			case PACKAGE_SPECIFIER:
			case SQUAREOPEN_SIGN:
			{
				pr__FIELD_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__TYPE_ARGUMENT 2 :", "m40.getText()");
				}
                if (currentStackElem.pendingTypeArg) {
                    addElemToTypeArgsList(highLevelType);
                    currentStackElem.pendingTypeArg = false;
                }
				break;
			}
			case PLUS_SIGN:
			case MINUS_SIGN:
			{
				pr__WILDCARD_INDICATOR();
				if ( inputState.guessing==0 ) {
					prntSS("   ### 17:", "m41.getText()");
				}
				pr__FIELD_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__TYPE_ARGUMENT 3 :", "m42.getText()");
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_11);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__WILDCARD_INDICATOR() throws RecognitionException, TokenStreamException {
		
		Token  m43 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case PLUS_SIGN:
			{
				match(PLUS_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===18===");  
					// grow the signature inconditionally because a wildcard is always the argument of :
					currentStackElem.sigEnd = addToSignature("+") + 1;
					
					// so, it's "restricted" wildcard :
					currentStackElem.wrappInWildcard = 0; // upper
					
					prntS("   pr__WILDCARD_INDICATOR 1 ");
					
				}
				break;
			}
			case MINUS_SIGN:
			{
				m43 = LT(1);
				match(MINUS_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===19===");  
					// grow the signature inconditionally because a wildcard is always the argument of :
					currentStackElem.sigEnd = addToSignature("-") + 1;
					
					// so, it's "restricted" wildcard:
					currentStackElem.wrappInWildcard = 1; // lower
					
					prntSS("   pr__WILDCARD_INDICATOR 2 :", m43.getText());
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_0);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__TYPE_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		Token  m49 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			case TVAR:
			case PACKAGE_SPECIFIER:
			case SQUAREOPEN_SIGN:
			{
				pr__FIELD_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					prntSS("   pr__TYPE_SIGNATURE 1 :", "m48.getText()");
				}
				break;
			}
			case TBASE:
			{
				m49 = LT(1);
				match(TBASE);
				if ( inputState.guessing==0 ) {
					
					prsrT = createInterimClassType(getBaseTypeName(m49.getText().charAt(0)));
					
					// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
					if (currentStackElem.dim > 0) {
					for (i = 0; i < currentStackElem.dim; i++ ) {
					prsrT = (InterimType)createInterimGenericArrayType(prsrT);
					}
					}
					currentStackElem.dim = 0;
                    currentStackElem.pendingTypeArg = true;
					
					highLevelType = /*(InterimGenericType)*/(InterimType)prsrT;
					
					prntSS("   pr__TYPE_SIGNATURE 2 :", m49.getText());
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__SUPERINTERFACE_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__CLASS_TYPE_SIGNATURE();
			if ( inputState.guessing==0 ) {
				
				prntS("      ===22===");  
				addElemToImplList((InterimType)highLevelType); // add the gathered regular type to implements-list
				
				highLevelType = null;
				
				prntSS("   pr__SUPERINTERFACE_SIGNATURE:", "m51.getText()");
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_12);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__C_P_AND_R_T() throws RecognitionException, TokenStreamException {
		
		try {      // for error handling
			pr__METHOD_PARAMETERS();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 241:", "m556.getText()");
			}
			LT(1);
			match(VOIDTYPE);
			if ( inputState.guessing==0 ) {
				
				prntSS("   pr__C_P_AND_R_T :", "m557.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__THROWN_SIGNATURE() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__THROWNS();
			if ( inputState.guessing==0 ) {
				
				prntS("      ===32===");  
				// set throwns field of InterimMethodGenericDecl/InterimConstructorGenericDecl object:
				//(declType == METHODoDECL ? methodDecl : constructorDecl).throwns = (InterimType[])thrownList.toArray();
				if (declType == METHODoDECL) {
				((InterimMethodGenericDecl)methodDecl).throwns = thrownList.toArray(new InterimType[thrownList.size()]);
				} else {
				((InterimConstructorGenericDecl)constructorDecl).throwns = thrownList.toArray(new InterimType[thrownList.size()]);
				}
				
				// clean the thrownList:
				thrownList = null;
				
				prntSS("   pr__THROWN_SIGNATURE:", "m82.getText()");
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__F_T_P_AND_C_P_AND_R_T() throws RecognitionException, TokenStreamException {
		
		try {      // for error handling
			pr__F_T_P_AND_M_P();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 29:", "m558.getText()");
			}
			LT(1);
			match(VOIDTYPE);
			if ( inputState.guessing==0 ) {
				
				prntSS("   pr__F_T_P_AND_C_P_AND_R_T :", "m559.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__METHOD_PARAMETERS() throws RecognitionException, TokenStreamException {
		
		Token  m76 = null;
		Token  m77 = null;
		Token  m73 = null;
		Token  m75 = null;
		
		try {      // for error handling
			boolean synPredMatched57 = false;
			if (((LA(1)==RINGOPEN_SIGN))) {
				int _m57 = mark();
				synPredMatched57 = true;
				inputState.guessing++;
				try {
					{
					match(RINGOPEN_SIGN);
					match(RINGCLOSE_SIGN);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched57 = false;
				}
				rewind(_m57);
				inputState.guessing--;
			}
			if ( synPredMatched57 ) {
				m76 = LT(1);
				match(RINGOPEN_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===29===");  
					// create InterimMethodGenericDecl or InterimConstructorGenericDecl:
					if (declType == METHODoDECL) {
					methodDecl = new InterimMethodGenericDecl();
					} else {
					constructorDecl = new InterimConstructorGenericDecl();
					}
					
					prntSS("   ### 32:", m76.getText());
					
				}
				m77 = LT(1);
				match(RINGCLOSE_SIGN);
				if ( inputState.guessing==0 ) {
					prntSS("   pr__METHOD_PARAMETERS 2 :", m77.getText());
				}
			}
			else if ((LA(1)==RINGOPEN_SIGN)) {
				m73 = LT(1);
				match(RINGOPEN_SIGN);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===27===");  
					// create InterimMethodGenericDecl or InterimConstructorGenericDecl:
					if (declType == METHODoDECL) {
					methodDecl = new InterimMethodGenericDecl();
					} else {
					constructorDecl = new InterimConstructorGenericDecl();
					}
					
					prntSS("   ### 30:", m73.getText());
					
				}
				pr__PARAMETERS_LIST();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===28===");  
					// set methodParameters field of InterimMethodGenericDecl/InterimConstructorGenericDecl object:
					//(declType == METHODoDECL ? methodDecl : constructorDecl).methodParameters = (InterimType[])methParamList.toArray();
					if (declType == METHODoDECL) {
					((InterimMethodGenericDecl)methodDecl).methodParameters = methParamList.toArray(new InterimType[methParamList.size()]);
					} else {
					((InterimConstructorGenericDecl)constructorDecl).methodParameters = methParamList.toArray(new InterimType[methParamList.size()]);
					}
					
					// clean the methParamList:
					methParamList = null;
					
					prntSS("   ### 31:", "m74.getText()");
					
				}
				m75 = LT(1);
				match(RINGCLOSE_SIGN);
				if ( inputState.guessing==0 ) {
					prntSS("   pr__METHOD_PARAMETERS 1 :", m75.getText());
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_14);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__F_T_P_AND_M_P() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__FORMAL_TYPE_PARAMETERS_DECL();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 28:", "m66.getText()");
			}
			pr__METHOD_PARAMETERS();
			if ( inputState.guessing==0 ) {
				
				prntS("      ===23===");  
				// set typeParameters field of InterimMethodGenericDecl or InterimConstructorGenericDecl object:
				//(declType == METHODoDECL ? methodDecl : constructorDecl).typeParameters = (InterimTypeParameter[])genParamList.toArray();
				if (declType == METHODoDECL) {
				((InterimMethodGenericDecl)methodDecl).typeParameters = genParamList.toArray(new InterimTypeParameter[genParamList.size()]);
				} else {
				((InterimConstructorGenericDecl)constructorDecl).typeParameters = genParamList.toArray(new InterimTypeParameter[genParamList.size()]);
				}
				
				// clean the genParamList:
				genParamList = null;
				
				prntSS("   pr__F_T_P_AND_M_P :", "m67.getText()");
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_14);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__M_P_AND_R_T() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__METHOD_PARAMETERS();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 27:", "m64.getText()");
			}
			pr__RETURN_TYPE();
			if ( inputState.guessing==0 ) {
				prntSS("   pr__M_P_AND_R_T :", "m65.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__F_T_P_AND_M_P_AND_R_T() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			pr__F_T_P_AND_M_P();
			if ( inputState.guessing==0 ) {
				prntSS("   ### 29:", "m68.getText()");
			}
			pr__RETURN_TYPE();
			if ( inputState.guessing==0 ) {
				prntSS("   pr__F_T_P_AND_M_P_AND_R_T :", "m69.getText()");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__RETURN_TYPE() throws RecognitionException, TokenStreamException {
		
		Token  m70 = null;
		Token  m72 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case VOIDTYPE:
			{
				m70 = LT(1);
				match(VOIDTYPE);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===24===");  
					if (declType == METHODoDECL) {
					// put void return type into the method decl:
					methodDecl.returnValue = (InterimType)createInterimClassType("void");
					}
					
					prntSS("   pr__RETURN_TYPE 1 :", m70.getText());
					
				}
				break;
			}
			case ID:
			case TVAR:
			case PACKAGE_SPECIFIER:
			case SQUAREOPEN_SIGN:
			case TBASE:
			{
				pr__TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===25===");  
					// put return type into the method decl:
					methodDecl.returnValue = (InterimType)highLevelType;
					
					highLevelType = null;
					
					prntSS("   pr__RETURN_TYPE 2 :", "m71.getText()");
					
				}
				break;
			}
			case RETURN_BASE_TYPE:
			{
				m72 = LT(1);
				match(RETURN_BASE_TYPE);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===26===");  
					prsrT = createInterimClassType(getBaseTypeName(m72.getText().charAt(0)));
					
					// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
					if (currentStackElem.dim > 0) {
					for (i = 0; i < currentStackElem.dim; i++ ) {
					prsrT = (InterimType)createInterimGenericArrayType(prsrT);
					}
					}
					currentStackElem.dim = 0;
					
					// put base return type into the method decl:
					methodDecl.returnValue = prsrT;
					
					prntSS("   pr__RETURN_TYPE 3 :", m72.getText());
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__PARAMETERS_LIST() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			int _cnt60=0;
			_loop60:
			do {
				if ((_tokenSet_15.member(LA(1)))) {
					pr__PARAMETER();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__PARAMETERS_LIST 2 :", "m78.getText()");
					}
				}
				else {
					if ( _cnt60>=1 ) { break _loop60; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt60++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_16);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__PARAMETER() throws RecognitionException, TokenStreamException {
		
		Token  m80 = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			case TVAR:
			case PACKAGE_SPECIFIER:
			case SQUAREOPEN_SIGN:
			{
				pr__FIELD_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===30===");  
					// put base type into the method params list:
					addElemToMethParamList(highLevelType);
                    currentStackElem.pendingTypeArg = false;
					highLevelType = null;
					
					prntSS("   pr__PARAMETER 1 :", "m79.getText()");
					
				}
				break;
			}
			case TBASE:
			{
				m80 = LT(1);
				match(TBASE);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===31===");  
					prsrT = createInterimClassType(getBaseTypeName(m80.getText().charAt(0)));
					
					// if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
					if (currentStackElem.dim > 0) {
					for (i = 0; i < currentStackElem.dim; i++ ) {
					prsrT = (InterimType)createInterimGenericArrayType(prsrT);
					}
					}
					currentStackElem.dim = 0;
					
					// put base type into the method params list:
					addElemToMethParamList(prsrT);
					
					prntSS("   pr__PARAMETER 2 :", m80.getText());
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_17);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__THROWNS() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			int _cnt65=0;
			_loop65:
			do {
				if ((LA(1)==CNTRL_SIGN)) {
					pr__THROWN();
					if ( inputState.guessing==0 ) {
						prntSS("   pr__THROWNS 2 :", "m83.getText()");
					}
				}
				else {
					if ( _cnt65>=1 ) { break _loop65; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt65++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void pr__THROWN() throws RecognitionException, TokenStreamException {
		
		Token  m84 = null;
		Token  m86 = null;
		Token  m87 = null;
		
		try {      // for error handling
			boolean synPredMatched68 = false;
			if (((LA(1)==CNTRL_SIGN))) {
				int _m68 = mark();
				synPredMatched68 = true;
				inputState.guessing++;
				try {
					{
					match(CNTRL_SIGN);
					pr__CLASS_TYPE_SIGNATURE();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched68 = false;
				}
				rewind(_m68);
				inputState.guessing--;
			}
			if ( synPredMatched68 ) {
				m84 = LT(1);
				match(CNTRL_SIGN);
				if ( inputState.guessing==0 ) {
					
					Yflag2 = 1; // so, we are within throws part
					
					prntSS("   ### 35:", m84.getText());
					
				}
				pr__CLASS_TYPE_SIGNATURE();
				if ( inputState.guessing==0 ) {
					
					prntS("      ===33===");  
					// put the InterimClassType or InterimParameterizedType to the throwns list:
					addElemToThrownList((InterimType)highLevelType);
					
					highLevelType = null;
					
					prntSS("   pr__THROWN 1 :", "m85.getText()");
					
				}
			}
			else if ((LA(1)==CNTRL_SIGN)) {
				m86 = LT(1);
				match(CNTRL_SIGN);
				if ( inputState.guessing==0 ) {
					prntSS("   ### 36:", m86.getText());
				}
				m87 = LT(1);
				match(TVAR);
				if ( inputState.guessing==0 ) {
					
					prntS("      ===34===");  
					assert(currentStackElem.dim == 0);
					//System.out.println(m87.getText()+"|"+m87.getText().length());
					
					// put the InterimTypeVariable to the throwns list:
					// to exclude first (official) "T" symbol (and last ";" symbol):
					addElemToThrownList((InterimType)createInterimTypeVariable(m87.getText().substring(1, m87.getText().length()-1)));
					
					prntSS("   pr__THROWN 2 :", m87.getText());
					
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"TRIANGLEOPEN_SIGN",
		"TRIANGLECLOSE_SIGN",
		"ID_COLON",
		"ID",
		"COLON_SIGN",
		"TVAR",
		"SEMICOLON_SIGN",
		"PACKAGE_SPECIFIER",
		"STAR_SIGN",
		"PLUS_SIGN",
		"MINUS_SIGN",
		"DOT_OR_DOLLAR_SIGN",
		"SQUAREOPEN_SIGN",
		"TBASE",
		"VOIDTYPE",
		"RETURN_BASE_TYPE",
		"RINGOPEN_SIGN",
		"RINGCLOSE_SIGN",
		"CNTRL_SIGN"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 68224L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 6519778L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 1050752L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 32L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 224L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 256L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 480L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 1024L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 33792L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 96896L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 96928L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 2178L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 4194306L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 985728L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 199296L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 2097152L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 2296448L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());

	}
