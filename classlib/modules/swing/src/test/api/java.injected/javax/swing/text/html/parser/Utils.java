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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text.html.parser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.SwingTestCase;
import javax.swing.text.BadLocationException;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML.Tag;

import junit.framework.TestCase;

public class Utils {
    static Field[] dtdConstantsFields;
    static String[] dtdConstantsNames;

    public static String[] getDTDConstantsNames() {
        if (dtdConstantsFields == null) {
            dtdConstantsFields = DTDConstants.class.getDeclaredFields();
        }
        if (dtdConstantsNames == null) {
            dtdConstantsNames = new String[dtdConstantsFields.length];
            for (int i = 0; i < dtdConstantsFields.length; i++) {
                dtdConstantsNames[i] = dtdConstantsFields[i].getName();
            }
        }
        return dtdConstantsNames;
    }

    public static String attrToString(final AttributeList attl) {
        if (attl == null) {
            return null;
        };
        String result = attl + "[" + attl.value + "]:";
        if (attl.values == null) {
            return result;
        }
        for (int i = 0; i < attl.values.size(); i++) {
            result += attl.values.get(i) + ",";
        }
        if (attl.next == null) {
           return result;
        }
        return result + "!" + attrToString(attl.next);
    }

    public static void printElement(final Element e, final String descr) {
        String offset = "   ";
        System.out.println(offset + descr);
        System.out.println(offset + "name: " + e.getName());
        System.out.println(offset + "type: " + e.getType());
        System.out.println(offset + "oStart: " + e.omitStart());
        System.out.println(offset + "oEnd: " + e.omitEnd());
        System.out.println(offset + "content: " + e.content);
        System.out.println(offset + "exclusions: " + e.exclusions);
        System.out.println(offset + "inclusions: " + e.inclusions);
        //System.out.println(offset + "atts: " + e.atts.paramString());
        System.out.println(offset + "data: " + e.data);
        System.out.println(offset + "index: " + e.index);
    }

    public static void printEntity(final Entity e, final String descr) {
        String offset = "   ";
        System.out.println(offset + descr);
        System.out.println(offset + "name: " + e.getName());
        System.out.println(offset + "type: " + (int)e.type + " " + e.getType());
        System.out.println(offset + "data: " + String.valueOf(e.data));
        System.out.println(offset + "isParameter: " + e.isParameter());
        System.out.println(offset + "isGeneral: " + e.isGeneral());
    }

    public static void printContentModel(final ContentModel contentModel,
              final Object content,
              final int type,
              final ContentModel next,
              final boolean isEmpty,
              final Element first,
              final Object objUnderTest,
              final boolean canBeFirst) {
        System.out.println(content + " " + contentModel.content);
        System.out.println(content.hashCode() + " "
                           + contentModel.content.hashCode());
        System.out.println((char)type + " " + (char)contentModel.type);
        System.out.println(next + " " + contentModel.next);
        System.out.println(isEmpty + " " + contentModel.empty());
        System.out.println(canBeFirst + " " + contentModel.first(objUnderTest));
        System.out.println(first + " " + contentModel.first());
    }

    public static void checkElement(final Element elem,
                             final AttributeList atts,
                             final ContentModel model,
                             final Object data,
                             final BitSet inclusions,
                             final BitSet exclusions,
                             final int index,
                             final String name,
                             final boolean oEnd,
                             final boolean oStart,
                             final int type) {
        checkElement(elem, atts, model, data, inclusions, exclusions,
                     index, name, oEnd, oStart, type, true);
    }

    public static Object doSerialization(final Object data) {
        try {
            ByteArrayOutputStream outByte = new ByteArrayOutputStream();
            BufferedOutputStream buffOut = new BufferedOutputStream(outByte);
            ObjectOutputStream output = new ObjectOutputStream(buffOut);
            output.writeObject(data);
            output.flush();
            buffOut.flush();
            ByteArrayInputStream inptByte = new ByteArrayInputStream(outByte
                    .toByteArray());
            ObjectInputStream input = new ObjectInputStream(inptByte);
            return input.readObject();
        } catch (Exception e) {
             e.printStackTrace();
             TestCase.assertFalse("unexpected Exception", true);
        }

        return null;
    }

    public static void checkElement(final Element elem,
                                    final AttributeList atts,
                                    final ContentModel model,
                                    final Object data,
                                    final BitSet inclusions,
                                    final BitSet exclusions,
                                    final int index,
                                    final String name,
                                    final boolean oEnd,
                                    final boolean oStart,
                                    final int type, boolean isStrongCheck) {
        if (isStrongCheck) {
            TestCase.assertEquals(atts, elem.atts);
            TestCase.assertEquals(model, elem.content);
            TestCase.assertEquals(atts, elem.getAttributes());
            TestCase.assertEquals(model, elem.getContent());
        }

        TestCase.assertEquals(data, elem.data);
        TestCase.assertEquals(inclusions, elem.inclusions);
        TestCase.assertEquals(exclusions, elem.exclusions);
        TestCase.assertEquals(index, elem.index);
        TestCase.assertEquals(name, elem.name);
        TestCase.assertEquals(oEnd, elem.oEnd);
        TestCase.assertEquals(oStart, elem.oStart);
        TestCase.assertEquals(type, elem.type);
        TestCase.assertEquals(index, elem.getIndex());
        TestCase.assertEquals(name, elem.getName());
        TestCase.assertEquals(oEnd, elem.omitEnd());
        TestCase.assertEquals(oStart, elem.omitStart());
        TestCase.assertEquals(type, elem.getType());
    }

    public static void checkDTDDefaultElement(final Element elem,
                                              final String name,
                                              final int index) {

        TestCase.assertNull(elem.atts);
        TestCase.assertNull(elem.content);
        TestCase.assertNull(elem.data);
        TestCase.assertNull(elem.inclusions);
        TestCase.assertNull(elem.exclusions);
        TestCase.assertEquals(index, elem.index);
        TestCase.assertEquals(name, elem.name);
        TestCase.assertFalse(elem.oEnd);
        TestCase.assertFalse(elem.oStart);
        TestCase.assertEquals(19, elem.type);
    }

    public static void checkAttributeList(final AttributeList attl,
                                          final int modifier,
                                          final int type,
                                          final String name,
                                          final AttributeList next,
                                          final Vector values,
                                          final String value,
                                          final boolean isStrongCheck) {
        TestCase.assertEquals(modifier, attl.getModifier());
        TestCase.assertEquals(name, attl.getName());
        TestCase.assertEquals(type, attl.getType());
        TestCase.assertEquals(value, attl.getValue());
        TestCase.assertEquals(values, attl.values);
        if (isStrongCheck || next == null) {
            TestCase.assertEquals(next, attl.getNext());
        } else {
            AttributeList attrList = attl.getNext();
            TestCase.assertEquals(next.modifier, attrList.getModifier());
            TestCase.assertEquals(next.name, attrList.getName());
            TestCase.assertEquals(next.type, attrList.getType());
            TestCase.assertEquals(next.value, attrList.getValue());
            TestCase.assertEquals(next.values, attrList.values);
        }
    }

    public static void checkContentModel(final ContentModel contentModel,
                                         final Object content,
                                         final int type,
                                         final ContentModel next) {
        TestCase.assertEquals("content", content, contentModel.content);
        TestCase.assertEquals("type", type, contentModel.type);
        TestCase.assertEquals("next", next, contentModel.next);
    }

    public static void initElement(final Element elem,
                                   final AttributeList atts,
                                   final ContentModel model,
                                   final Object data,
                                   final BitSet inclusions,
                                   final BitSet exclusions,
                                   final int index,
                                   final String name,
                                   final boolean oEnd,
                                   final boolean oStart,
                                   final int type) {
        elem.atts = atts;
        elem.content = model;
        elem.data = data;
        elem.inclusions = inclusions;
        elem.exclusions = exclusions;
        elem.index = index;
        elem.name = name;
        elem.oEnd = oEnd;
        elem.oStart = oStart;
        elem.type = type;
    }

    public static void checkEntity(final Entity entity,
                                   final String name,
                                   final int type,
                                   final String data,
                                   final boolean isGeneral,
                                   final boolean isParameter) {
        TestCase.assertEquals(name, entity.name);
        //TestCase.assertEquals(type, entity.type);
        TestCase.assertEquals(data, String.valueOf(entity.data));
        TestCase.assertEquals(name, entity.getName());
        TestCase.assertEquals(type, entity.getType()); //TODO 65536
        TestCase.assertEquals(data, entity.getString());
        TestCase.assertEquals(isGeneral, entity.isGeneral());
        TestCase.assertEquals(isParameter, entity.isParameter());
    }

    public static void checkEntity(final Entity entity,
                                   final String name,
                                   final int type,
                                   final char data,
                                   final boolean isGeneral,
                                   final boolean isParameter) {
        TestCase.assertEquals(name, entity.name);
        TestCase.assertEquals(1, entity.data.length);
        TestCase.assertEquals(data, entity.data[0]);
        TestCase.assertEquals(name, entity.getName());
        TestCase.assertEquals(type, entity.getType()); //TODO 65536
        TestCase.assertEquals(isGeneral, entity.isGeneral());
        TestCase.assertEquals(isParameter, entity.isParameter());
    }


    public static Reader getReader(final String testName) {
        Reader result = null;
        try {
            FileInputStream fis =
                new FileInputStream(System.getProperty("TEST_SRC_DIR")
                                    + "javax/swing/text/html/parser/" + testName
                                    + ".html");
            result = new InputStreamReader(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    static DTD dtd;

    public static DTD getDefaultDTDInstance() {
        DTD dtd = new DTD("test") {
            public Element getElement(final int index) {
                return super.getElement(index);
            }

            public Element getElement(final String name) {
                Element result = super.getElement(name);
                return result;
            }
        };

        return dtd;
    }

    public static DTD getFilledDTD() {
//        if (SwingTestCase.isHarmony())  {
//            DTD dtd = new DTD("test");
//            DTDUtilities.initDTD(dtd);
//            return dtd;
//        }

        DTD dtd = getDefaultDTDInstance();
        fillDTD(dtd);
        return dtd;
    }

    public static void fillDTD(final DTD dtd) {
        String name = SwingTestCase.isHarmony() ? "transitional401.bdtd"
                : "html32.bdtd";
        try {
            dtd.read(new DataInputStream(dtd.getClass()
                                         .getResourceAsStream(name)));
        } catch (IOException e) {
            e.printStackTrace();
            TestCase.assertFalse("Unexpected IOException", true);
        }
    }

    public static DTD getDefaultDTD() {
        if (dtd == null) {
            dtd = getDefaultDTDInstance();
        }
        return dtd;
    }

    public static void printDebugInfo(final String msg,
                                      final boolean condition) {
        if (condition) {
            System.out.println(msg);
        }
    }

    //TODO add checking position for atts && pos &&
    public static class ParserCallback extends HTMLEditorKit.ParserCallback {
        public final boolean debugOut = true;

        public Utils.ExtDocumentParser parserForCheck;

        public boolean checkArguments = false;

        public void checkArguments(final int pos,
                                   final HTML.Tag tag) {
            if (checkArguments) {
                TestCase.assertEquals(parserForCheck.pos_d, pos);
                TestCase.assertEquals(parserForCheck.tag_d, tag);
            }
        }

        public void checkArguments(final int pos) {
            if (checkArguments) {
                TestCase.assertEquals(parserForCheck.pos_d, pos);
            }
        }

        public void checkArguments(final HTML.Tag tag) {
            if (checkArguments) {
                 TestCase.assertEquals(parserForCheck.tag_d, tag);
            }
        }

        public void setParser(final Utils.ExtDocumentParser parser) {
            parserForCheck = parser;
        }

        public void printDebugInfo(final String msg) {
            Utils.printDebugInfo("Utils.ParserCallback:" + msg, debugOut);
        }

        public void flush() throws BadLocationException {
            printDebugInfo("flush");
            super.flush();
        }

        public void handleComment(final char[] data,
                                  final int pos) {
            printDebugInfo("handleComment(data=" + String.valueOf(data)
                           + ", " + "pos= " + pos + ")");
            super.handleComment(data, pos);
        }

        public void handleEndOfLineString(final String eol) {
            printDebugInfo("handleEndOfLineString(eol=" + eol + ")");
            super.handleEndOfLineString(eol);
        }

        public void handleEndTag(final Tag tag,
                                 final int pos) {
            printDebugInfo("handleEndTag(tag=" + tag + ", " + "pos="
                           + pos + ")");
            super.handleEndTag(tag, pos);
        }

        public void handleError(final String msg,
                                final int pos) {
            printDebugInfo("handleError(msg=" + msg + ", " + "pos="
                           + pos + ")");
            super.handleError(msg, pos);
        }
        public void handleSimpleTag(final Tag tag,
                                    final MutableAttributeSet atts,
                                    final int pos) {
            printDebugInfo("handleSimpleTag(tag=" + tag + ", " + "atts="
                           + atts + ", " +  "pos=" + pos + ")");
            super.handleSimpleTag(tag, atts, pos);
        }

        public void handleStartTag(final Tag tag,
                                   final MutableAttributeSet atts,
                                   final int pos) {
            checkArguments(tag);
            printDebugInfo("handleStartTag(tag=" + tag + ", " + "atts="
                           + atts + ", " +  "pos=" + pos + ")");
            super.handleStartTag(tag, atts, pos);
        }
        public void handleText(final char[] text,
                               final int pos) {
            printDebugInfo("handleText(data=" + String.valueOf(text) + ", "
                           + "pos= " + pos + ")");
            super.handleText(text, pos);
        }
    }

    public static class ExtParser extends Parser {
        public boolean debugOut = true;
        public void printDebugInfo(final String msg) {
            Utils.printDebugInfo("Utils.ExtParser:" + msg, debugOut);
        }

        public ExtParser(final DTD dtd) {
            super(dtd);
        }

        protected void endTag(final boolean omitted) {
            printDebugInfo("endTag()");
            super.endTag(omitted);
        }
        protected void error(final String msg) {
            printDebugInfo("2.error: " + msg);
            super.error(msg);
        }
        protected void error(final String msg1, final String msg2) {
            printDebugInfo("3.error: " + msg1 + " " + msg2);
            super.error(msg1, msg2);
        }
        protected void error(final String msg1,
                             final String msg2,
                             final String msg3) {
            printDebugInfo("4.error: " + msg1 + " " + msg2 + " " + msg3);
            super.error(msg1, msg2, msg3);
        }
        protected void error(final String msg1,
                             final String msg2,
                              final String msg3,
                              final String msg4) {
            printDebugInfo("5.error: " + msg1 + " " + msg2 + " " +  msg3
                           + " " + msg4);
            super.error(msg1, msg2, msg3, msg4);
        }
        protected void flushAttributes() {
            printDebugInfo("6.flushAttributes");
            super.flushAttributes();
        }

        protected SimpleAttributeSet getAttributes() {
            printDebugInfo("7.getAttributes");
            return super.getAttributes();
        }

        protected int getCurrentLine() {
            printDebugInfo("8.getCurrentLine");
            return super.getCurrentLine();
        }

        protected int getCurrentPos() {
            printDebugInfo("9.getCurrentLine");
            return super.getCurrentPos();
        }

        protected void handleComment(final char[] text) {
            super.handleComment(text);
            printDebugInfo("10.handleComment: " + new String(text));

        }

        protected void handleEmptyTag(final TagElement elem)
            throws ChangedCharSetException {
            printDebugInfo("11.handleEmptyTag");
            super.handleEmptyTag(elem);
        }

        protected void handleEndTag(final TagElement elem) {
            printDebugInfo("12.handleEndTag");
            super.handleEndTag(elem);
        }

        protected void handleEOFInComment() {
            printDebugInfo("13.handleEOFInComment");
            super.handleEOFInComment();
        }

        protected void handleError(final int pos, final String msg) {
            printDebugInfo("14.handleError: " + pos + " " + msg);
            super.handleError(pos, msg);
        }

        protected void handleStartTag(final TagElement elem) {
            printDebugInfo("15.handleStartTag " + elem.getElement());
            AttributeList attList = elem.getElement().getAttributes();
            if (attList != null) {
                printDebugInfo("..." + attList.getName() + " "
                               + attList.getValue());
            }
            super.handleStartTag(elem);
        }

        protected void handleText(final char[] text) {
            printDebugInfo("16.handleText: " + new String(text));
            super.handleText(text);
        }

        protected void handleTitle(final char[] text) {
            printDebugInfo("17.handleTitle: " + new String(text));
            super.handleTitle(text);
        }

        protected TagElement makeTag(final Element elem) {
            printDebugInfo("18.makeTag(1)" + elem);
            //printElement(elem, "");
            return super.makeTag(elem);
        }

        protected TagElement makeTag(final Element elem,
                                     final boolean functional) {
            printDebugInfo("19.makeTag(2)" + functional + " " + elem);
            //printElement(elem, "");
            return super.makeTag(elem, functional);
        }

        protected void markFirstTime(final Element elem) {
            printDebugInfo("20.markFirstTime");
            super.markFirstTime(elem);
        }

        protected boolean parseMarkupDeclarations(final StringBuffer buf)
            throws IOException {
            printDebugInfo("21.parseMarkupDeclarations");
            return super.parseMarkupDeclarations(buf);
        }

        protected void startTag(final TagElement elem)
            throws ChangedCharSetException {
            printDebugInfo("22.startTag " + elem.getHTMLTag());
            /*System.out.println("__________________________ _");
            Utils.printElement(elem.getElement()," ");
            System.out.println("____________________________"); */
            super.startTag(elem);
        }
    }

    public static class ExtDTD extends DTD {
        public boolean debugOut = true;
        ExtDTD(final String name) {
            super(name);
        }

        public void printDebugInfo(final String msg) {
            Utils.printDebugInfo("Utils:ExtDTD:" + msg, debugOut);

        }

        protected AttributeList defAttributeList(final String arg0,
                                                 final int arg1, final int arg2,
                                                 final String arg3,
                                                 final String arg4,
                                                 final AttributeList arg5) {
            AttributeList result = super.defAttributeList(arg0, arg1, arg2,
                                                          arg3, arg4, arg5);
            printDebugInfo("defAttributeList=" + result);
            return result;
        }
        protected ContentModel defContentModel(final int arg0,
                                               final Object arg1,
                                               final ContentModel arg2) {
            ContentModel result = super.defContentModel(arg0, arg1, arg2);
            printDebugInfo("defContentModel=" + result);
            return result;
        }

        protected Element defElement(final String name,
                                     final int type,
                                     final boolean o1,
                                     final boolean o2,
                                     final ContentModel model,
                                     final String[] excl,
                                     final String[] incl,
                                     final AttributeList atts) {
            Element e = super.defElement(name, type, o1, o2, model, excl,
                                         incl, atts);
            printDebugInfo("defElement(str)=" + e);
            Utils.printElement(e, "(defElement)");
            return e;
        }
        public Entity defEntity(final String arg0, final int arg1,
                                final int arg2) {
            Entity result = super.defEntity(arg0, arg1, arg2);
            printDebugInfo("defEntity=" + result);
            return result;
        }
        protected Entity defEntity(final String arg0, final int arg1,
                                   final String arg2) {
            Entity result = super.defEntity(arg0, arg1, arg2);
            printDebugInfo("defEntity=" + result);
            return result;
        }

        public void defineAttributes(final String arg0,
                                     final AttributeList arg1) {
            printDebugInfo("defineAttributes");
            super.defineAttributes(arg0, arg1);
        }

        public  Element defineElement(final String name,
                                      final int type,
                                      final boolean o1,
                                      final boolean o2,
                                      final ContentModel model,
                                      final BitSet excl,
                                      final BitSet incl,
                                      final AttributeList atts) {

          Element e = super.defineElement(name, type, o1, o2, model,
                                          excl, incl, atts);
          printDebugInfo("defineElement=" + e);
          //Utils.printElement(e, "(defineElement)");
          return e;
        }
        public Entity defineEntity(final String arg0,
                                   final int arg1,
                                   final char[] arg2) {
            Entity result = super.defineEntity(arg0, arg1, arg2);
            printDebugInfo("defineEntity=" + result);
            return result;
        }
        public Element getElement(final int arg0) {
            Element result = super.getElement(arg0);
            printDebugInfo("getElement(int)=" + result);
            return result;
        }
        public Element getElement(final String arg0) {
            Element result = super.getElement(arg0);
            printDebugInfo("getElement(str)=" + result);
            return result;
        }
        public Entity getEntity(final int arg0) {
            Entity result = super.getEntity(arg0);
            printDebugInfo("getEntity(int)=" + result);
            return result;
        }
        public Entity getEntity(final String arg0) {
            Entity result = super.getEntity(arg0);
            printDebugInfo("getEntity(str)=" + result);
            return result;
        }
        public String getName() {
            printDebugInfo("getName " + super.getName());
            return super.getName();
        }
        public void read(final DataInputStream arg0) throws IOException {
            printDebugInfo("read");
            super.read(arg0);
        }
        public String toString() {
            printDebugInfo("toString");
            return super.toString();
        }
    }

    public static class ExtDocumentParser extends DocumentParser {
        public boolean debugOut = true;

        public int pos_d;

        public HTML.Tag tag_d;

        public ExtDocumentParser(final DTD dtd) {
            super(dtd);
        }

        public void printDebugInfo(final String msg) {
            Utils.printDebugInfo("Utils.ExtDocumentParser:" + msg, debugOut);
        }
        protected void handleComment(final char[] text) {
            printDebugInfo("handleComment(text=" +  String.valueOf(text) + ")");
            super.handleComment(text);
        }

        protected void handleEmptyTag(final TagElement tagElement)
            throws ChangedCharSetException {
            printDebugInfo("handleEmptyTag(tag=" + tagElement + ")");
            super.handleEmptyTag(tagElement);
        }

        protected void handleEndTag(final TagElement tagElement) {
            printDebugInfo("handleEndTag(tag=" + tagElement + ")");
            super.handleEndTag(tagElement);
        }

        protected void handleError(final int pos, final String msg) {
            printDebugInfo("handleError(pos=" + pos + ", msg=" + ")");
            super.handleError(pos, msg);
        }

        protected void handleStartTag(final TagElement tagElement) {
            printDebugInfo("handleStartTag(" + tagElement + ")");
            pos_d = getCurrentPos();
            tag_d = tagElement.getHTMLTag();
            super.handleStartTag(tagElement);
        }

        protected void handleText(final char[] text) {
            printDebugInfo("handleText(text=" + String.valueOf(text) + ")");
            super.handleText(text);
        }

        public void parse(final Reader reader,
                          final HTMLEditorKit.ParserCallback cb,
                          final boolean ichs) throws IOException {
            printDebugInfo("parse(reader=" + reader + ", cb=" + cb + ", "
                           + "ichs=" + ichs);
            super.parse(reader, cb, ichs);
        }
    }
}
