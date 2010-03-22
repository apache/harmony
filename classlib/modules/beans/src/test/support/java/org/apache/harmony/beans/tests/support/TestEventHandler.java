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

/*
 * Created on 27.06.2006
 * @author Alexei Y. Zakharov
 */
package org.apache.harmony.beans.tests.support;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class is used by XMLEncoderTest for handling SAX parser events.
 * 
 * @author Alexei Zakharov
 */
public class TestEventHandler extends DefaultHandler {

    public Tag root = null;

    private Tag currentTag = null;

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {
    }

    @Override
    public void startElement(String uri, String name, String qName,
            Attributes atts) {
        Tag theTag = new Tag(name, currentTag);

        theTag.fillAttributes(atts);
        if (currentTag != null) {
            currentTag.innerTags.add(theTag);
        }
        if (root == null) {
            root = theTag;
        }
        currentTag = theTag;
    }

    @Override
    public void endElement(String uri, String name, String qName)
            throws SAXException {
        if (!name.equals(currentTag.name)) {
            throw new SAXException("unexpected closing tag: " + name);
        }
        currentTag = currentTag.parent;
    }

    @Override
    public void characters(char ch[], int start, int length) {
        currentTag.content.append(ch, start, length);
    }

    public static void main(String argv[]) throws Exception {

        XMLReader xmlReader;
        TestEventHandler handler = new TestEventHandler();
        FileReader reader;
        String saxParserClassName = System.getProperty("org.xml.sax.driver");

        if (saxParserClassName == null) {
            saxParserClassName = "org.apache.xerces.parsers.SAXParser";
        }
        xmlReader = XMLReaderFactory.createXMLReader(saxParserClassName);
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);

        if (argv.length < 1) {
            throw new Exception("input file should be specified");
        }
        reader = new FileReader(argv[0]);
        xmlReader.parse(new InputSource(reader));
        System.out.println(handler.root.toString());
    }

    static class Tag {
        String name;

        HashMap<String, String> attributes = new LinkedHashMap<String, String>();

        HashSet<Tag> innerTags = new LinkedHashSet<Tag>();

        Tag parent = null;

        StringBuffer content = new StringBuffer();

        boolean ignoreJavaVersion = true;

        public Tag(String name, Tag parent) {
            this.name = name;
            this.parent = parent;
        }

        public void fillAttributes(Attributes attrs) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String name = attrs.getLocalName(i);
                String value = attrs.getValue(i);

                attributes.put(name, value);
            }
        }

        @Override
        public boolean equals(Object obj) {
            Iterator<Map.Entry<String, String>> it;
            Iterator<Tag> itTag;
            Tag tag;

            if (!(obj instanceof Tag)) {
                return false;
            }
            tag = (Tag) obj;

            // name
            if (!name.equals(tag.name)) {
                return false;
            }

            // attributes
            if (attributes.entrySet().size() != tag.attributes.entrySet()
                    .size()) {
                return false;
            }
            it = attributes.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                Iterator<Map.Entry<String, String>> it2 = tag.attributes
                        .entrySet().iterator();
                boolean found = false;

                while (it2.hasNext()) {
                    Map.Entry<String, String> entry2 = it2.next();

                    if (entry2.getKey().equals(entry.getKey())) {
                        if (ignoreJavaVersion && tag.name.equals("java")
                                && entry.getKey().equals("version")) {
                            // ignore java version
                            found = true;
                            break;
                        } else if (entry2.getValue().equals(entry.getValue())) {
                            // values are the same
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    return false;
                }
            }

            // inner tags
            if (innerTags.size() != tag.innerTags.size()) {
                return false;
            }
            itTag = innerTags.iterator();
            while (itTag.hasNext()) {
                Tag innerTag = itTag.next();
                Iterator<Tag> itTag2 = tag.innerTags.iterator();
                boolean found = false;

                while (itTag2.hasNext()) {
                    Tag innerTag2 = itTag2.next();

                    if (innerTag.equals(innerTag2)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            Iterator<Map.Entry<String, String>> it;

            sb.append('<' + name);
            it = attributes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();

                sb.append(" " + entry.getKey() + "=\"" + entry.getValue()
                        + "\"");
            }
            if (innerTags.isEmpty() && content.length() == 0) {
                sb.append("/>\n");
            } else if (innerTags.isEmpty() && content.length() > 0) {
                sb.append(">");
                sb.append(content);
                sb.append("</" + name + ">\n");
            } else {
                Iterator<Tag> it2 = innerTags.iterator();

                sb.append(">\n");
                while (it2.hasNext()) {
                    Tag child = it2.next();

                    sb.append(child.toString() + "\n");
                }
                sb.append("</" + name + ">\n");
            }
            return sb.toString();
        }
    }

}
