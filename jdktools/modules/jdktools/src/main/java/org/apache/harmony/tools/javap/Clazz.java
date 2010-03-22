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

package org.apache.harmony.tools.javap;

import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.harmony.tools.ClassProvider;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.AccessFlags;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;


/**
 * This class is a wrapper of BCEL's Class class.
 *
 * This class depends on Apache Byte Code Engineering Library (BCEL) 5.0 or
 * later. Please see http://jakarta.apache.org/bcel for more information
 * about this library.
 */
public class Clazz {

    /**
     * A wrapped class.
     */
    private JavaClass wrappedClass;

    /**
     * A constant pool of the wrapped class.
     */
    private ConstantPool constPool;

    /**
     * Verbose output.
     */
    private boolean verbose;

    /**
     * Inner classes of a wrapped class.
     */
    private InnerClasses innerClasses;

    /**
     * Inner class names of a wrapped class.
     */
    private String innerClassNames[];

    /**
     * Whether the disassembled code be printed by
     * the <code>toString</code> method.
     */
    private boolean printCode = false;

    /**
     * Whether line numbers be printed by
     * the <code>toString</code> method.
     */
    private boolean printLineNumbers = false;

    /**
     * Whether local variables be printed by
     * the <code>toString</code> method.
     */
    private boolean printLocalVariables = false;

    /**
     * Whether internal type signatures be printed by
     * the <code>toString</code> method.
     */
    private boolean printTypeSignatures = false;

    /**
     * Whether package private methods be printed by
     * the <code>toString</code> method.
     */
    private boolean printPackagePrivate = true;

    /**
     * Whether public methods be printed by
     * the <code>toString</code> method.
     */
    private boolean printPublic = false;

    /**
     * Whether protected methods be printed by
     * the <code>toString</code> method.
     */
    private boolean printProtected = false;

    /**
     * Whether private methods be printed by
     * the <code>toString</code> method.
     */
    private boolean printPrivate = false;

    /**
     * Platform dependent line separator.
     */
    private static String n = System.getProperty("line.separator");

    /**
     * Constructs a <code>Clazz</code> object.
     * 
     * @param classProvider - a helper that provides the class information.
     * @param className - a fully qualified name of a class.
     * @param verbose - true, if output should be verbose. Otherwise false.
     */
    public Clazz(ClassProvider classProvider, String className, boolean verbose) 
            throws ClassNotFoundException {
        wrappedClass = classProvider.getJavaClass(className);
        constPool = wrappedClass.getConstantPool();
        this.verbose = verbose;

        // Assign an empty array by default.
        Vector foundInners = new Vector();
        // Get the class attributes.
        Attribute attrs[] = wrappedClass.getAttributes();
        for (int i = 0; i < attrs.length; i++) {
            // Find the InnerClasses attribute, if any.
            if (attrs[i] instanceof InnerClasses) {
                // The InnerClasses attribute is found.
                innerClasses = (InnerClasses) attrs[i];

                // Get an array of the inner classes.
                InnerClass inners[] = innerClasses.getInnerClasses();
                for (int j = 0; j < inners.length; j++) {

                    // Get the inner class name from a constant pool.
                    String innerClassName = Utility.compactClassName(
                            innerClasses.getConstantPool().getConstantString(
                                    inners[j].getInnerClassIndex(), 
                                    Constants.CONSTANT_Class),
                            false);

                    // The inner class has the InnerClasses attribute as well
                    // as its outer class. So, we should ignore such an inner 
                    // class.
                    if (!innerClassName.equals(className)) {
                        foundInners.addElement(innerClassName);
                    }
                }
                break;
            }
        }
        // Fill in the inner class array with the found inner classes.
        innerClassNames = new String[foundInners.size()];
        foundInners.toArray(innerClassNames);
    }

    /**
     * Returns an array of inner classes of this class.
     * 
     * @return an array of inner class names.
     */
    public String[] getInnerClassNames() {
        return innerClassNames;
    }

    /**
     * Allows to include a disassembled code into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includeCode(boolean value) {
        printCode = value;
    }

    /**
     * Allows to include line numbers into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includeLineNumbers(boolean value) {
        printLineNumbers = value;
    }

    /**
     * Allows to include local variables into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includeLocalVariables(boolean value) {
        printLocalVariables = value;
    }

    /**
     * Allows to include internal type signatures into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includeTypeSignatures(boolean value) {
        printTypeSignatures = value;
    }

    /**
     * Allows to include package private methods into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includePackagePrivate(boolean value) {
        printPackagePrivate = value;
    }

    /**
     * Allows to include public methods into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includePublic(boolean value) {
        printPublic = value;
    }

    /**
     * Allows to include protected methods into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includeProtected(boolean value) {
        printProtected = value;
    }

    /**
     * Allows to include private methods into the output of
     * the <code>toString</code> method.
     * 
     * @param value - true, if the inclusion is allowed. Otherwise false.
     */
    public void includePrivate(boolean value) {
        printPrivate = value;
    }

    /**
     * Determines if the given access is acceptable.
     * 
     * @param access - the tested flags.
     * @return true, if the given access of a class or a memeber is acceptable.
     */
    private boolean isPrintable(AccessFlags access) {
        if (access.isPrivate() && !printPrivate
                || access.isProtected() && !printProtected
                || access.isPublic() && !printPublic
                || !access.isPublic() && !access.isProtected()
                        && !access.isPrivate() && !printPackagePrivate) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string that does not include the given tokens.
     * 
     * @param s - a source string.
     * @param tokens - a string array of tokens to be skipped.
     * @return the result string.
     */
    private String skipTokens(String s, String tokens[]) {
        StringBuffer result = new StringBuffer();
        // We are going to process the given string as a series of tokens,
        // since we want to modify it a little.
        StringTokenizer st = new StringTokenizer(s);
        boolean empty = true;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            boolean skip = false;
            // Test, if the current token should be skipped.
            for (int i = 0; i < tokens.length; i++) {
                if (token.equals(tokens[i])) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            if (!empty) {
                result.append(' ');
            }
            result.append(token);
            empty = false;
        }
        return result.toString();
    }

    /**
     * Returns a string of spaces whose length is equal to the given argument.
     * 
     * @param indent - a number of space symbols of the result string.
     * @return an indentaion string with the specified length.
     */
    private String indentString(int indent) {
        return indentString(indent, "");
    }

    /**
     * Indents the given string with the given number of spaces.
     * 
     * @param indent - a number of space symbols the result string indentation.
     * @param s - a string to be indent.
     * @return the given string indented by the given number of spaces.
     */
    private String indentString(int indent, String s) {
        StringBuffer result = new StringBuffer();

        // Prepare the indentation prefix.
        char[] chars = new char[indent];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ' ';
        }

        // Append it to the result for the first line.
        result.append(chars);

        // Prepend the prefix to each line of the given string.
        // We also replace the '\n' symbol which is used as EOL by BCEL with
        // the platform specific one.
        int fromIndex = 0;
        int toIndex = 0;
        while ((toIndex = s.indexOf('\n', fromIndex)) != -1) {
            // Append a substring before '\n'.
            result.append(s.substring(fromIndex, toIndex));
            // Append "line.separator".
            result.append(n);
            fromIndex = toIndex + 1;
            // Append the prefix in beginning of the next line,
            // if it is not the latest.
            if (fromIndex < s.length()) {
                result.append(chars);
            }
        }
        // Append the rest of the given string.
        result.append(s.substring(fromIndex));

        return result.toString();
    }

    /**
     * Returns some extra information.
     * 
     * @param indent - an indentation.
     * @return an indented string with some extra information.
     */
    private String printExtra(int indent) {
        StringBuffer result = new StringBuffer();
        StringBuffer l = new StringBuffer();
        StringBuffer r = new StringBuffer();

        // Create the indentation string.
        String indentStr = indentString(indent);

        // Append a source file name.
        result.append(indentStr);
        result.append("SourceFile: ");
        result.append('\"');
        result.append(wrappedClass.getSourceFileName());
        result.append('\"');
        result.append(n);

        // Append inner classes.
        if (innerClasses != null && innerClasses.getLength() > 0) {
            result.append(indentStr);
            result.append("InnerClass:");
            result.append(n);

            InnerClass inners[] = innerClasses.getInnerClasses();
            for (int i = 0; i < inners.length; i++) {
                InnerClass ic = inners[i];
                int innerAccessFlags = ic.getInnerAccessFlags();
                int innerNameIndex = ic.getInnerNameIndex();
                int innerClassIndex = ic.getInnerClassIndex();
                int outerClassIndex = ic.getOuterClassIndex();

                // Append the indentation.
                result.append(indentStr);
                result.append(indentStr);

                // Append an access string.
                String access = Utility.accessToString(innerAccessFlags, true);
                if (access.length() > 0) {
                    result.append(access);
                    result.append(' ');
                }

                // Construct a string like this:
                //     #?= #? of #?; //?=class ? of class ?
                // Every number or string represented as '?' may be empty.
                // So we try to eliminate strings like this:
                //     #0= #0 of #0; //=class  of class 

                l.setLength(0);
                r.setLength(0);

                if (innerClassIndex > 0) {

                    if (innerNameIndex > 0) {
                        l.append('#');
                        l.append(innerNameIndex);
                        l.append("= ");

                        r.append(constPool.constantToString(
                                constPool.getConstant(innerNameIndex)));
                        r.append("=");
                    }

                    l.append("#");
                    l.append(innerClassIndex);

                    r.append("class ");
                    r.append(constPool.constantToString(
                            constPool.getConstant(innerClassIndex)));

                    if (outerClassIndex > 0) {
                        l.append(" of #");
                        l.append(outerClassIndex);

                        r.append(" of class ");
                        r.append(constPool.constantToString(
                                constPool.getConstant(outerClassIndex)));
                    }

                    if (l.length() > 0) {
                        result.append(l);
                        result.append(";");

                        if (r.length() > 0) {
                            result.append(" //");
                            result.append(r);
                        }
                    }

                    result.append(n);

                }

            }
        }

        // Append a version number.
        result.append(indentStr);
        result.append("minor version: ");
        result.append(wrappedClass.getMinor());
        result.append(n);
        result.append(indentStr);
        result.append("major version: ");
        result.append(wrappedClass.getMajor());
        result.append(n);

        // Append a constant pool.
        if (constPool.getLength() > 0) {
            result.append(indentStr);
            result.append("Constant pool:");
            result.append(n);

            Constant pool[] = constPool.getConstantPool();
            for (int i = 0; i < pool.length; i++) {
                Constant constant = pool[i];
                // pool[i] may be null, so we skip such elements.
                if (constant == null) {
                    continue;
                }
                
                result.append(indentStr);
                result.append(indentStr);
                result.append("const #");
                result.append(i);
                result.append(" = ");

                // Append a constant specific string.
                if (constant instanceof ConstantUtf8) {
                    ConstantUtf8 c = (ConstantUtf8) constant;
                    result.append("Asciz\t");
                    result.append(constPool.constantToString(c));
                    result.append(';');
                } else if (constant instanceof ConstantFieldref) {
                    ConstantFieldref c = (ConstantFieldref) constant;
                    result.append("Field\t#");
                    result.append(c.getClassIndex());
                    result.append(".#");
                    result.append(c.getNameAndTypeIndex());
                    result.append(";\t//  ");
                    result.append(Utility.replace(
                            constPool.constantToString(c), " ", ":"));
                } else if (constant instanceof ConstantNameAndType) {
                    ConstantNameAndType c = (ConstantNameAndType) constant;
                    result.append("NameAndType\t#");
                    result.append(c.getNameIndex());
                    result.append(":#");
                    result.append(c.getSignatureIndex());
                    result.append(";\t//  ");
                    result.append(Utility.replace(
                            constPool.constantToString(c), " ", ":"));
                } else if (constant instanceof ConstantMethodref) {
                    ConstantMethodref c = (ConstantMethodref) constant;
                    result.append("Method\t#");
                    result.append(c.getClassIndex());
                    result.append(".#");
                    result.append(c.getNameAndTypeIndex());
                    result.append(";\t//  ");
                    result.append(Utility.replace(
                            constPool.constantToString(c), " ", ":"));
                } else if (constant instanceof ConstantInterfaceMethodref) {
                    ConstantInterfaceMethodref c = (ConstantInterfaceMethodref) constant;
                    result.append("InterfaceMethod\t#");
                    result.append(c.getClassIndex());
                    result.append(".#");
                    result.append(c.getNameAndTypeIndex());
                    result.append(";\t//  ");
                    result.append(Utility.replace(
                            constPool.constantToString(c), " ", ":"));
                } else if (constant instanceof ConstantDouble) {
                    ConstantDouble c = (ConstantDouble) constant;
                    result.append("double\t");
                    result.append(constPool.constantToString(c));
                    result.append(';');
                } else if (constant instanceof ConstantFloat) {
                    ConstantFloat c = (ConstantFloat) constant;
                    result.append("float\t");
                    result.append(constPool.constantToString(c));
                    result.append(';');
                } else if (constant instanceof ConstantInteger) {
                    ConstantInteger c = (ConstantInteger) constant;
                    result.append("int\t");
                    result.append(constPool.constantToString(c));
                    result.append(';');
                } else if (constant instanceof ConstantLong) {
                    ConstantLong c = (ConstantLong) constant;
                    result.append("long\t");
                    result.append(constPool.constantToString(c));
                    result.append(';');
                } else if (constant instanceof ConstantClass) {
                    ConstantClass c = (ConstantClass) constant;
                    result.append("class\t#");
                    result.append(c.getNameIndex());
                    result.append(";\t//  ");
                    result.append(constPool.constantToString(c));
                } else if (constant instanceof ConstantString) {
                    ConstantString c = (ConstantString) constant;
                    result.append("String\t#");
                    result.append(c.getStringIndex());
                    result.append(";\t//  ");
                    result.append(constPool.constantToString(c));
                }

                result.append(n);
            }
        } 

        result.append(n);

        return result.toString();
    }

    /**
     * Returns some information about the class fields.
     * 
     * @param indent - an indentation.
     * @return an indented string with some information about the class fields.
     */
    private String printFields(int indent) {
        StringBuffer result = new StringBuffer();

        Field fields[] = wrappedClass.getFields();
        if (fields.length > 0) {
            // Create the indentation string.
            String indentStr = indentString(indent);

            boolean found = false;
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];

                // Skip a field, if we should not print an information about it.
                if (!isPrintable(f)) {
                    continue;
                }
                found = true;

                result.append(n);
                result.append(indentStr);

                // Append an access string.
                String access = Utility.accessToString(f.getAccessFlags());
                if (access.length() > 0) {
                    result.append(access);
                    result.append(' ');
                }
                // Append a field signature and name.
                result.append(Utility.signatureToString(f.getSignature()));
                result.append(' ');
                result.append(f.getName());
                result.append(';');
                result.append(n);

                // Append a type signature.
                if (printTypeSignatures) {
                    result.append(indentString(indent * 2, "Signature: "));
                    result.append(f.getSignature());
                    result.append(n);
                }

                if (verbose) {
                    // Append a field constant value, if any.
                    ConstantValue cv  = f.getConstantValue();
                    if (cv != null) {
                        result.append(indentString(indent * 2,
                                "Constant value: "));
                        result.append(cv);
                        result.append(n);
                    }

                    Attribute attrs[] = f.getAttributes();
                    for (int j = 0; j < attrs.length; j++) {
                        if (attrs[j].getTag() == Constants.ATTR_SYNTHETIC) {
                            result.append(indentString(indent * 2, 
                                    "Synthetic: true"));
                            result.append(n);
                            break;
                        }
                    }
                }

            }
            if (found) {
                result.append(n);
            }
        }
        return result.toString();
    }

    /**
     * Returns some information about the class methods.
     * 
     * @param indent - an indentation.
     * @return an indented string with some information about the class methods.
     */
    private String printMethods(int indent) {
        StringBuffer result = new StringBuffer();

        // Create the indentation string.
        String indentStr = indentString(indent);

        Method methods[] = wrappedClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];

            // Skip a method, if we should not print an information about it.
            if (!isPrintable(m)) {
                continue;
            }

            result.append(n);
            result.append(indentStr);

            // The default values.
            String methodName = m.getName();
            String signature = m.getSignature();

            if (methodName.equals("<clinit>")) {
                // The "static" block of a class.
                result.append("static {}");
            } else {
                boolean ctor = false;
                if (methodName.equals("<init>")) {
                    // It's a constructor.
                    ctor = true;
                }

                // Create a method string.
                String methodStr = Utility.methodSignatureToString(
                        signature,
                        ctor ? wrappedClass.getClassName() : methodName,
                        Utility.accessToString(m.getAccessFlags()),
                        true,
                        m.getLocalVariableTable());

                if (ctor) {
                    // Remove the "void" word, if this is a constructor.
                    methodStr = skipTokens(methodStr, new String[] {"void"});
                }

                // Append the method string.
                result.append(methodStr);

                // Append an exception table.
                ExceptionTable e = m.getExceptionTable();
                if (e != null) {
                    String exceptions = e.toString();
                    if (exceptions.length() > 0) {
                        result.append(n);
                        result.append(indentString(indent * 3, "throws "));
                        result.append(exceptions);
                    }
                }
            }

            result.append(';');
            result.append(n);

            // Append a signature.
            if (printTypeSignatures) {
                result.append(indentString(indent * 2, "Signature: "));
                result.append(signature);
                result.append(n);
            }

            // Append a code.
            if (printCode) {
                Code code = m.getCode();
                // If the method is not abstract.
                if (code != null) {
                    result.append(indentString(indent * 2, "Code: "));
                    result.append(n);

                    if (verbose) {
                        result.append(indentString(indent * 3, "Max stack="));
                        result.append(code.getMaxStack());
                        result.append(", Max locals=");
                        result.append(code.getMaxLocals());
                        result.append(n);
                    }

                    // Append the code string.
                    result.append(indentString(
                            indent * 3,
                            Utility.codeToString(
                                    code.getCode(), constPool, 0, -1, verbose
                            ))
                    );
                }
            }

            // Append a table of the line numbers.
            if (printLineNumbers) {
                LineNumberTable lt = m.getLineNumberTable();
                if (lt != null) {
                    LineNumber nums[] = lt.getLineNumberTable();
                    if (nums.length > 0) {
                        String indentStr3 = indentString(indent * 3);
                        result.append(indentString(
                                indent * 2, "LineNumberTable:"));
                        result.append(n);
                        for (int j = 0; j < nums.length; j++) {
                            LineNumber ln = nums[j];
                            result.append(indentStr3);
                            result.append("line ");
                            result.append(ln.getLineNumber());
                            result.append(": ");
                            result.append(ln.getStartPC());
                            result.append(n);
                        }
                    }
                }
            }

            // Append the local variables table.
            if (printLocalVariables) {
                LocalVariableTable vt = m.getLocalVariableTable();
                if (vt != null) {
                    LocalVariable vars[] = vt.getLocalVariableTable();
                    if (vars.length > 0) {
                        String indentStr3 = indentString(indent * 3);
                        result.append(indentString(
                                indent * 2, "LocalVariableTable:"));
                        result.append(n);
                        result.append(indentStr3);
                        result.append("Start\tLength\tSlot\tName\tSignature");
                        result.append(n);
                        for (int j = 0; j < vars.length; j++) {
                            LocalVariable lv = vars[j];
                            result.append(indentStr3);
                            result.append(lv.getStartPC());
                            result.append("\t\t");
                            result.append(lv.getLength());
                            result.append("\t");
                            result.append(lv.getIndex());
                            result.append("\t");
                            result.append(lv.getName());
                            result.append("\t");
                            result.append(lv.getSignature());
                            result.append("\t");
                            result.append(n);
                        }
                    }
                }
            }

        }
        return result.toString();
    }

    /**
     * Returns a string that represents a structural information about
     * a wrapped class.
     */
    public String toString() {
        StringBuffer result = new StringBuffer();

        // Append a source file name.
        result.append(n);
        result.append("Compiled from ");
        result.append('\"');
        result.append(wrappedClass.getSourceFileName());
        result.append('\"');
        result.append(n);

        // Construct a class access string.
        int classAccess = wrappedClass.getAccessFlags();
        String classAccessStr = Utility.accessToString(classAccess, true);
        if (wrappedClass.isInterface()) {
            // Remove the "abstract" word, if this is an interface.
            classAccessStr = skipTokens(
                    classAccessStr, new String[] {"abstract"});
        }
        // Append the access string.
        if (classAccessStr.length() > 0) {
            result.append(classAccessStr);
            result.append(' ');
        }

        // Append the word "class" or "interface".
        result.append(Utility.classOrInterface(classAccess));
        result.append(' ');
        result.append(wrappedClass.getClassName());

        // Append the name of an extended class.
        // We skip the sequence "extends java.lang.Object".
        if (!wrappedClass.getSuperclassName().equals("java.lang.Object")) {
            result.append(" extends ");
            result.append(Utility.compactClassName(
                    wrappedClass.getSuperclassName(), false));
        }

        // Append the names of the implemented interfaces.
        String interfaces[] = wrappedClass.getInterfaceNames();
        if (interfaces.length > 0) {
            result.append(" implements");
            for (int i = 0; i < interfaces.length; i++) {
                if (i > 0) {
                    result.append(',');
                }
                result.append(' ');
                result.append(interfaces[i]);
            }
        }

        // Append the open brace.
        result.append(' ');
        result.append('{');
        result.append(n);

        // The default value.
        int indent = 4;

        // Append some extra information, if verbose is true.
        if (verbose) {
            result.append(printExtra(indent));
        }

        // Append the filtered fields and their signatures.
        result.append(printFields(indent));

        // Append the filtered methods and their signatures.
        result.append(printMethods(indent));

        // Append the close brace.
        result.append(n);
        result.append('}');
        result.append(n);

        return result.toString();
    }
}
