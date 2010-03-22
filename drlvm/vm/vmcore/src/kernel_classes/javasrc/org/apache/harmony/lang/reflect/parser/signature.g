header {
package org.apache.harmony.lang.reflect.parser;

import java.util.ArrayList;
import java.io.StringBufferInputStream;
import java.io.IOException;
import java.lang.reflect.GenericSignatureFormatError;
import org.apache.harmony.lang.reflect.parser.*;
}

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

class SignatureParser extends Parser;

// SignatureParser class members:
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
};
private PTStack stack;
private PTStack currentStackElem; // points to the current processed level element of the nested parameterized types chain

private String CTPTsignature; // accumulated signature of the current parsered parameterized type, it's used for parameterized types repository
private int  sigInd;
private int  sigLen;

private InterimType/*InterimGenericType*/ highLevelType; // the rolled up reference (InterimParameterizedType or InterimClassType)

private int i;
private InterimType prsrT;
private int len;
private PTStack p1, p2, currentStackElemCopy;
private InterimClassType upper;
private char t;
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
    prntS("      addElemToTypeArgsList");

    // find the previous element for the current stack's element:
     PTStack p1 = stack, p2 = null;

     while (p1 != currentStackElem){
        p2 = p1;
        p1 = p1.nextLevel;
     }
     
    // add the value to the args list of the found stack's element:
     if(p2.args == null) {
        p2.args = new ArrayList();
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
}


pr__DECL
[int kind, Object lex]
{
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
    sigLen = 0;
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
}
       :  {declType==CLASSoDECL}? m1:pr__CLASS_DECL {prntSS("   pr__DECL 1 :", "m1.getText()");}
        | {declType==FIELDoDECL}? m2:pr__FIELD_DECL {                                          
                          // it's time to create InterimFieldGenericDecl and to fill fieldDecl
                          fieldDecl = new InterimFieldGenericDecl();

                          // set fieldType field of InterimFieldGenericDecl object:
                          fieldDecl.fieldType = (InterimGenericType)highLevelType;
                                          
                          highLevelType = null;

                          prntSS("   pr__DECL 2 :", "m2.getText()");
                       }
        | {declType==METHODoDECL}? m3:pr__METHOD_DECL {prntSS("   pr__DECL 3:", "m3.getText()");}
        | {declType==CONSTRUCTORoDECL}? m4:pr__CONSTRUCTOR_DECL {prntSS("   pr__DECL 4 :", "m4.getText()");}
      ;
pr__FIELD_DECL:   m5:pr__FIELD_TYPE_SIGNATURE {prntSS("   pr__FIELD_DECL:", "m5.getText()");}
            ;
pr__CLASS_DECL:   m6:pr__FORMAL_TYPE_PARAMETERS_DECL {prntSS("   ### 1:", "m6.getText()");} m7:pr__CLASS_TYPE_SIGNATURE {    
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
                                                                                             } m8:pr__SUPERINTERFACE_SIGNATURES {
                                                                                                                              prntS("      ===2===");  
                                                                                                                              // set superInterfaces field of InterimClassGenericDecl object:
                                                                                                                              classDecl.superInterfaces = (implList == null ? new InterimType[0] : implList.toArray(new InterimType[implList.size()]));
    
                                                                                                                              // clean the implList:
                                                                                                                              implList = null;

                                                                                                                              prntSS("   pr__CLASS_DECL 1 :", "m8.getText()");
                                                                                                                          }
              | m9:pr__CLASS_TYPE_SIGNATURE {
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
                                       } m10:pr__SUPERINTERFACE_SIGNATURES {
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
           ;
pr__FORMAL_TYPE_PARAMETERS_DECL:  m11:TRIANGLEOPEN_SIGN {
                                                      prntS("      ===4===");  
                                                      Yflag1 = 1; // start of generic parameters parsing
                                                      prntSS("   ### 4:", m11.getText());
                                                  }
                                                   m12:pr__FORMAL_TYPE_PARAMETERS {prntSS("   ### 5:", "m12.getText()");} m13:TRIANGLECLOSE_SIGN {
                                                                                                                          prntS("      ===5===");  
                                                                                                                          Yflag1 = 0; // finish of generic parameters parsing
                                                                                                                          prntSS("   pr__FORMAL_TYPE_PARAMETERS_DECL:", m13.getText());
                                                                                                                        }
                             ;
pr__FORMAL_TYPE_PARAMETERS:  ( m14:pr__FORMAL_TYPE_PARAMETER {prntSS("   pr__FORMAL_TYPE_PARAMETERS 1 :", "m14.getText()");} )+
                        ; 
pr__FORMAL_TYPE_PARAMETER:  m141:pr__ID_WITH_COLON {prntSS("   ### 7:", /*lexer.ident*/"m141.getText()");} m15:pr__CLASS_OR_INTERFACE_BOUNDS {
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
                      ;
pr__ID_WITH_COLON:   m151:ID_COLON {
                          prntS("      ===7===");  
                          String ts1 = m151.getText();
                          String ts2 = ts1.substring(0, ts1.length()-2);
                          
                          createTypeParameterName(ts2);
                          addElemToBoundList(createInterimClassType("Ljava/lang/Object")); // Object is supposed if extends clause is empty (example: PARAMNAME::...;)
                               
                          prntSS("   pr__ID_WITH_COLON 1 :", ts2);
                       }
                 | m16:ID {
                         prntS("      ===8===");  
                         createTypeParameterName(m16.getText());
                         
                         prntSS("   pr__ID_WITH_COLON 2 :", m16.getText());
                      }
              ;
pr__CLASS_OR_INTERFACE_BOUNDS:   ( m18:pr__BOUND {prntSS("   pr__CLASS_OR_INTERFACE_BOUNDS 1 :", "m18.getText()");} )+
                           ;
pr__BOUND: m19:COLON_SIGN {prntSS("   ### 9:", m19.getText());} m20:pr__FIELD_TYPE_SIGNATURE {
                                                                      prntS("      ===8.1===");  
                                                                      addElemToBoundList((InterimType)highLevelType); // add the gathered regular type to bounds-list
                                                      
                                                                      highLevelType = null;
                                                                      
                                                                      prntSS("   pr__BOUND:", "m20.getText()");
                                                                   }
      ;
pr__FIELD_TYPE_SIGNATURE:  m21:pr__CLASS_TYPE_SIGNATURE {prntSS("   pr__FIELD_TYPE_SIGNATURE 1 :", "m21.getText()");}
                       | m22:pr__ARRAY_TYPE_SIGNATURE {
                                                   prntS("      ===9===");  
                                                   if (Yflag1 == 1) {
                                                      throwGenericSignatureFormatError(); // array type is not permissible within a generic params decl
                                                   }
                                                   if (currentStackElem.wrappInWildcard != -1) {
                                                      throwGenericSignatureFormatError(); // array type is not permissible within a wild card
                                                   }
                                                   prntSS("   pr__FIELD_TYPE_SIGNATURE 2 :", "m22.getText()");
                                                 }
                       | m23:TVAR {
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
                              } /*SEMICOLON_SIGN {prntS("      ===10.2===");}*/
                      ;
pr__CLASS_TYPE_SIGNATURE:   m24:pr__REFERENCE {prntSS("   ### 10:", "m24.getText()");} m25:SEMICOLON_SIGN {
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
                                                                                        sigLen = 0;
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
                    ;
pr__REFERENCE: (pr__SIMPLE_CLASS_TYPE_SIGNATURE pr__CLASS_TYPE_SIGNATURE_SUFFIXES)=> m27:pr__SIMPLE_CLASS_TYPE_SIGNATURE {prntSS("   ### 10:", "m27.getText()");} m28:pr__CLASS_TYPE_SIGNATURE_SUFFIXES {prntSS("   pr__REFERENCE 2 :", "m28.getText()");}
             | m26:pr__SIMPLE_CLASS_TYPE_SIGNATURE {prntSS("   pr__REFERENCE 1 :", "m26.getText()");}
             | (pr__P_S_AND_S_C_T pr__CLASS_TYPE_SIGNATURE_SUFFIXES)=> m29:pr__P_S_AND_S_C_T {prntSS("   ### 11:", "m29.getText()");} m30:pr__CLASS_TYPE_SIGNATURE_SUFFIXES {prntSS("   pr__REFERENCE 3 :", "m30.getText()");}
             | m31:pr__P_S_AND_S_C_T {prntSS("   pr__REFERENCE 4 :", "m31.getText()");}
           ;  
pr__P_S_AND_S_C_T:   m32:PACKAGE_SPECIFIER {
                                         prntS("      ===12===");  
                                         
                                         // to start of gathering all info within gatheredStr about the being parsered reference:
                                         addToGatheredStr(currentStackElem, m32.getText()); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/" here
                                         
                                         // to start of gathering all info within CTPTsignature about the being parsered reference:
                                         currentStackElem.sigBegin = addToSignature(m32.getText()); // so, it's "L"[A-Za-z][A-Za-z0-9"/"]*"/" added to CTPTsignature 
                                         currentStackElem.sigEnd = currentStackElem.sigBegin + m32.getText().length();
                                         
                                         prntSS(">->->->-> ", currentStackElem.gatheredStr);
                                         prntSS("   ### 12:", m32.getText());
                                      }
                                      m33:pr__SIMPLE_CLASS_TYPE_SIGNATURE {prntSS("   pr__P_S_AND_S_C_T:", "m33.getText()");} 
                ;
pr__SIMPLE_CLASS_TYPE_SIGNATURE:  (ID TRIANGLEOPEN_SIGN)=> m34:ID {
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
                                   } m35:TRIANGLEOPEN_SIGN {                         
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
                                                       } m36:pr__TYPE_ARGUMENTS {prntSS("   ### 15:", "m36.getText()");} m37:TRIANGLECLOSE_SIGN {
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
                              | m371:ID {
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
                         ;
pr__TYPE_ARGUMENTS:  ( m38:pr__TYPE_ARGUMENT {prntSS("   pr__TYPE_ARGUMENTS 1 :", "m38.getText()");} )+
                ;
pr__TYPE_ARGUMENT:  m39:STAR_SIGN {
                               prntS("      ===17===");  
                               // grow the signature inconditionally because a wildcard is always the argument of :
                               currentStackElem.sigEnd = addToSignature("*") + 1;
                                     
                               // so, it's "unrestricted" wildcard.
                               // add the wildcard to the args list of the stack's element of the previous layer:
                               upper = createInterimClassType("Ljava/lang/Object");
                               addElemToTypeArgsList(createInterimWildcardType(0 /* i.e. extends Object */, new InterimType[]{(InterimType)upper}, 1));
                               
                               prntSS("   pr__TYPE_ARGUMENT 1 :", m39.getText());
                            }
                | m40:pr__FIELD_TYPE_SIGNATURE {prntSS("   pr__TYPE_ARGUMENT 2 :", "m40.getText()");}
                | m41:pr__WILDCARD_INDICATOR {prntSS("   ### 17:", "m41.getText()");} m42:pr__FIELD_TYPE_SIGNATURE {prntSS("   pr__TYPE_ARGUMENT 3 :", "m42.getText()");}
              ;  
pr__WILDCARD_INDICATOR:  PLUS_SIGN {
                                    prntS("      ===18===");  
                                    // grow the signature inconditionally because a wildcard is always the argument of :
                                    currentStackElem.sigEnd = addToSignature("+") + 1;
                                     
                                    // so, it's "restricted" wildcard :
                                    currentStackElem.wrappInWildcard = 0; // upper
                                    
                                    prntS("   pr__WILDCARD_INDICATOR 1 ");
                                 }
                     | m43:MINUS_SIGN {
                                     prntS("      ===19===");  
                                     // grow the signature inconditionally because a wildcard is always the argument of :
                                     currentStackElem.sigEnd = addToSignature("-") + 1;
                                     
                                     // so, it's "restricted" wildcard:
                                     currentStackElem.wrappInWildcard = 1; // lower
                                     
                                     prntSS("   pr__WILDCARD_INDICATOR 2 :", m43.getText());
                                  }
                    ;
pr__CLASS_TYPE_SIGNATURE_SUFFIXES:   ( m44:DOT_OR_DOLLAR_SIGN {
                                                         prntS("      ===21===");  
                                                         //// seems, I have just done it in pr__SIMPLE_CLASS_TYPE_SIGNATURE
                                                         //// grow the signature:
                                                         //currentStackElem.sigEnd = addToSignature("$") + 1;

                                                         prntSS("   ### 20:", m44.getText());
                                                      } m45:pr__SIMPLE_CLASS_TYPE_SIGNATURE {prntSS("   pr__CLASS_TYPE_SIGNATURE_SUFFIXES 2 :", "m45.getText()");})+
                               ;                       
pr__ARRAY_TYPE_SIGNATURE: m46:SQUAREOPEN_SIGN {
                                           currentStackElem.dim += 1;
                                           
                                           prntSS("   ### 21:", m46.getText());
                                        } m47:pr__TYPE_SIGNATURE {prntSS("   pr__ARRAY_TYPE_SIGNATURE:", "m47.getText()");}
                      ;                  
pr__TYPE_SIGNATURE:  m48:pr__FIELD_TYPE_SIGNATURE {prntSS("   pr__TYPE_SIGNATURE 1 :", "m48.getText()");}
                 | m49:TBASE {
                            prsrT = createInterimClassType(getBaseTypeName(m49.getText().charAt(0)));

                            // if there is dimention indicator then InterimTypeVariable should be "rolled up" by InterimGenericArrayType
                            if (currentStackElem.dim > 0) {
                                for (i = 0; i < currentStackElem.dim; i++ ) {
                                    prsrT = (InterimType)createInterimGenericArrayType(prsrT);
                                }
                            }
                            currentStackElem.dim = 0;

                            highLevelType = /*(InterimGenericType)*/(InterimType)prsrT;
                            
                            prntSS("   pr__TYPE_SIGNATURE 2 :", m49.getText());
                         }
                 ; 
pr__SUPERINTERFACE_SIGNATURES:  ( m50:pr__SUPERINTERFACE_SIGNATURE {prntSS("   pr__SUPERINTERFACE_SIGNATURES 1 :", "m50.getText()");} )*
                           ; 
pr__SUPERINTERFACE_SIGNATURE: m51:pr__CLASS_TYPE_SIGNATURE {
                                                      prntS("      ===22===");  
                                                      addElemToImplList((InterimType)highLevelType); // add the gathered regular type to implements-list
                                                      
                                                      highLevelType = null;
                                                      
                                                      prntSS("   pr__SUPERINTERFACE_SIGNATURE:", "m51.getText()");
                                                   }
                          ;
//pr__CONSTRUCTOR_DECL: (pr__METHOD_PARAMETERS pr__THROWN_SIGNATURE)=> m53:pr__METHOD_PARAMETERS {prntSS("   ### 23:", "m53.getText()");} m54:pr__THROWN_SIGNATURE {prntSS("   pr__CONSTRUCTOR_DECL 2 :", "m54.getText()");}
//                    | m52:pr__METHOD_PARAMETERS {prntSS("   pr__CONSTRUCTOR_DECL 1 :", "m52.getText()");}
//                    | (pr__F_T_P_AND_M_P pr__THROWN_SIGNATURE)=> m56:pr__F_T_P_AND_M_P {prntSS("   ### 24:", "m56.getText()");} m57:pr__THROWN_SIGNATURE {prntSS("   pr__CONSTRUCTOR_DECL 4 :", "m57.getText()");}
//                    | m55:pr__F_T_P_AND_M_P {prntSS("   pr__CONSTRUCTOR_DECL 3 :", "m55.getText()");}
//                  ;             
//pr__CONSTRUCTOR_DECL: (pr__METHOD_PARAMETERS pr__THROWN_SIGNATURE)=> m53:pr__METHOD_PARAMETERS {prntSS("   ### 23:", "m53.getText()");} VOIDTYPE m54:pr__THROWN_SIGNATURE {prntSS("   pr__CONSTRUCTOR_DECL 2 :", "m54.getText()");}
//                    | m52:pr__METHOD_PARAMETERS {prntSS("   pr__CONSTRUCTOR_DECL 1 :", "m52.getText()");} VOIDTYPE
//                    | (pr__F_T_P_AND_M_P pr__THROWN_SIGNATURE)=> m56:pr__F_T_P_AND_M_P {prntSS("   ### 24:", "m56.getText()");} VOIDTYPE m57:pr__THROWN_SIGNATURE {prntSS("   pr__CONSTRUCTOR_DECL 4 :", "m57.getText()");}
//                    | m55:pr__F_T_P_AND_M_P {prntSS("   pr__CONSTRUCTOR_DECL 3 :", "m55.getText()");} VOIDTYPE
//                  ;             
pr__CONSTRUCTOR_DECL:  (pr__C_P_AND_R_T pr__THROWN_SIGNATURE)=> m53:pr__C_P_AND_R_T {prntSS("   ### 23:", "m53.getText()");} m54:pr__THROWN_SIGNATURE {prntSS("   pr__CONSTRUCTOR_DECL 2 :prntSS", "m54.getText()");}
              | m52:pr__C_P_AND_R_T {prntSS("   pr__CONSTRUCTOR_DECL 1 :", "m52.getText()");}
              | (pr__F_T_P_AND_C_P_AND_R_T pr__THROWN_SIGNATURE)=> m56:pr__F_T_P_AND_C_P_AND_R_T {prntSS("   ### 24:", "m56.getText()");} m57:pr__THROWN_SIGNATURE {prntSS("   pr__CONSTRUCTOR_DECL 4 :", "m57.getText()");}
              | m55:pr__F_T_P_AND_C_P_AND_R_T {prntSS("   pr__CONSTRUCTOR_DECL 3 :", "m55getText()");}
             ;
pr__C_P_AND_R_T:  m555:pr__METHOD_PARAMETERS {prntSS("   ### 241:", "m556.getText()");}    m557:VOIDTYPE {
                             prntSS("   pr__C_P_AND_R_T :", "m557.getText()");}
             ;
pr__F_T_P_AND_C_P_AND_R_T:  m558:pr__F_T_P_AND_M_P {prntSS("   ### 29:", "m558.getText()");}    m559:VOIDTYPE {
                             prntSS("   pr__F_T_P_AND_C_P_AND_R_T :", "m559.getText()");}
                       ;
pr__METHOD_DECL:  (pr__M_P_AND_R_T pr__THROWN_SIGNATURE)=> m59:pr__M_P_AND_R_T {prntSS("   ### 25:", "m59.getText()");} m60:pr__THROWN_SIGNATURE {prntSS("   pr__METHOD_DECL 2 :prntSS", "m60.getText()");}
              | m58:pr__M_P_AND_R_T {prntSS("   pr__METHOD_DECL 1 :", "m58.getText()");}
              | (pr__F_T_P_AND_M_P_AND_R_T pr__THROWN_SIGNATURE)=> m62:pr__F_T_P_AND_M_P_AND_R_T {prntSS("   ### 26:", "m62.getText()");} m63:pr__THROWN_SIGNATURE {prntSS("   pr__METHOD_DECL 4 :", "m63.getText()");}
              | m61:pr__F_T_P_AND_M_P_AND_R_T {prntSS("   pr__METHOD_DECL 3 :", "m61.getText()");}
             ;
pr__M_P_AND_R_T:  m64:pr__METHOD_PARAMETERS {prntSS("   ### 27:", "m64.getText()");} m65:pr__RETURN_TYPE {prntSS("   pr__M_P_AND_R_T :", "m65.getText()");}
             ;
pr__F_T_P_AND_M_P:  m66:pr__FORMAL_TYPE_PARAMETERS_DECL {prntSS("   ### 28:", "m66.getText()");} m67:pr__METHOD_PARAMETERS {
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
               ;
pr__F_T_P_AND_M_P_AND_R_T:  m68:pr__F_T_P_AND_M_P {prntSS("   ### 29:", "m68.getText()");} m69:pr__RETURN_TYPE {prntSS("   pr__F_T_P_AND_M_P_AND_R_T :", "m69.getText()");}
                       ;
pr__RETURN_TYPE:   m70:VOIDTYPE {
                             prntS("      ===24===");  
                             if (declType == METHODoDECL) {
                                 // put void return type into the method decl:
                                 methodDecl.returnValue = (InterimType)createInterimClassType("void");
                             }
                             
                             prntSS("   pr__RETURN_TYPE 1 :", m70.getText());
                          }
               | m71:pr__TYPE_SIGNATURE {
                                     prntS("      ===25===");  
                                     // put return type into the method decl:
                                     methodDecl.returnValue = (InterimType)highLevelType;
                                     
                                     highLevelType = null;
                             
                                     prntSS("   pr__RETURN_TYPE 2 :", "m71.getText()");
                                  }
               | m72:RETURN_BASE_TYPE {
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
              ;                    
pr__METHOD_PARAMETERS:  (RINGOPEN_SIGN RINGCLOSE_SIGN) => m76:RINGOPEN_SIGN {
                                        prntS("      ===29===");  
                                        // create InterimMethodGenericDecl or InterimConstructorGenericDecl:
                                        if (declType == METHODoDECL) {
                                            methodDecl = new InterimMethodGenericDecl();
                                        } else {
                                            constructorDecl = new InterimConstructorGenericDecl();
                                        }

                                        prntSS("   ### 32:", m76.getText());
                                     } m77:RINGCLOSE_SIGN {prntSS("   pr__METHOD_PARAMETERS 2 :", m77.getText());}
                       | m73:RINGOPEN_SIGN {
                                        prntS("      ===27===");  
                                        // create InterimMethodGenericDecl or InterimConstructorGenericDecl:
                                        if (declType == METHODoDECL) {
                                            methodDecl = new InterimMethodGenericDecl();
                                        } else {
                                            constructorDecl = new InterimConstructorGenericDecl();
                                        }

                                        prntSS("   ### 30:", m73.getText());
                                     } m74:pr__PARAMETERS_LIST {
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
                                                         } m75:RINGCLOSE_SIGN {prntSS("   pr__METHOD_PARAMETERS 1 :", m75.getText());}
                   ;
pr__PARAMETERS_LIST:   ( m78:pr__PARAMETER {prntSS("   pr__PARAMETERS_LIST 2 :", "m78.getText()");} )+
                 ;  
pr__PARAMETER:   m79:pr__FIELD_TYPE_SIGNATURE {
                                         prntS("      ===30===");  
                                         // put base type into the method params list:
                                         addElemToMethParamList(highLevelType);
                                         
                                         highLevelType = null;
                      
                                         prntSS("   pr__PARAMETER 1 :", "m79.getText()");
                                      }
             | m80:TBASE {
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
           ;
pr__THROWN_SIGNATURE: m82:pr__THROWNS {
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
                 ;
pr__THROWNS:   ( m83:pr__THROWN {prntSS("   pr__THROWNS 2 :", "m83.getText()");} )+
           ;  
pr__THROWN:   (CNTRL_SIGN pr__CLASS_TYPE_SIGNATURE)=> m84:CNTRL_SIGN {
                          Yflag2 = 1; // so, we are within throws part
                          
                          prntSS("   ### 35:", m84.getText());
                       } m85:pr__CLASS_TYPE_SIGNATURE {
                                                   prntS("      ===33===");  
                                                   // put the InterimClassType or InterimParameterizedType to the throwns list:
                                                   addElemToThrownList((InterimType)highLevelType);
                                          
                                                   highLevelType = null;
                                                                          
                                                   prntSS("   pr__THROWN 1 :", "m85.getText()");
                                                }
          | m86:CNTRL_SIGN {prntSS("   ### 36:", m86.getText());} m87:TVAR {
                                                        prntS("      ===34===");  
                                                        assert(currentStackElem.dim == 0);

                                                        // put the InterimTypeVariable to the throwns list:
                                                        // to exclude first (official) "T" symbol (and last ";" symbol):
                                                        System.out.println(m87.getText()+"|"+m87.getText().length());
                                                        addElemToThrownList((InterimType)createInterimTypeVariable(m87.getText().substring(1, m87.getText().length()-1)));

                                                        prntSS("   pr__THROWN 2 :", m87.getText());
                                                     } /*SEMICOLON_SIGN {prntS("      ===34.2===");} //XXX: should be eliminated again!*/
        ;
